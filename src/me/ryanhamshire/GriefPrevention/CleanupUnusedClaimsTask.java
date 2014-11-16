/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
 
 package me.ryanhamshire.GriefPrevention;

import java.util.Calendar;
import java.util.Random;
import java.util.Vector;

import org.bukkit.Chunk;
import org.bukkit.World;

//FEATURE: automatically remove claims owned by inactive players which:
//...aren't protecting much OR
//...are a free new player claim (and the player has no other claims) OR
//...because the player has been gone a REALLY long time, and that expiration has been configured in config.yml

//runs every 1 minute in the main thread
class CleanupUnusedClaimsTask implements Runnable 
{	
	int nextClaimIndex;
	
	CleanupUnusedClaimsTask()
	{
		//start scanning in a random spot
		if(GriefPrevention.instance.dataStore.claims.size() == 0)
		{
			this.nextClaimIndex = 0;
		}
		else
		{
			Random randomNumberGenerator = new Random();
			this.nextClaimIndex = randomNumberGenerator.nextInt(GriefPrevention.instance.dataStore.claims.size());
		}
	}
	
	@Override
	public void run()
	{
		//don't do anything when there are no claims
		if(GriefPrevention.instance.dataStore.claims.size() == 0) return;
		
		//wrap search around to beginning
		if(this.nextClaimIndex >= GriefPrevention.instance.dataStore.claims.size()) this.nextClaimIndex = 0;
		
		//decide which claim to check next
		Claim claim = GriefPrevention.instance.dataStore.claims.get(this.nextClaimIndex++);
		
		//skip administrative claims
		if(claim.isAdminClaim()) return;
		
		//track whether we do any important work which would require cleanup afterward
		boolean cleanupChunks = false;
		
		//get data for the player, especially last login timestamp
		PlayerData playerData = null;
		
		//determine area of the default chest claim
		int areaOfDefaultClaim = 0;
		if(GriefPrevention.instance.config_claims_automaticClaimsForNewPlayersRadius >= 0)
		{
			areaOfDefaultClaim = (int)Math.pow(GriefPrevention.instance.config_claims_automaticClaimsForNewPlayersRadius * 2 + 1, 2);  
		}
		
		//if this claim is a chest claim and those are set to expire
        if(claim.getArea() <= areaOfDefaultClaim && GriefPrevention.instance.config_claims_chestClaimExpirationDays > 0)
		{
            playerData = GriefPrevention.instance.dataStore.getPlayerData(claim.ownerID);
            
            //if the owner has been gone at least a week, and if he has ONLY the new player claim, it will be removed
	        Calendar sevenDaysAgo = Calendar.getInstance();
	        sevenDaysAgo.add(Calendar.DATE, -GriefPrevention.instance.config_claims_chestClaimExpirationDays);
	        boolean newPlayerClaimsExpired = sevenDaysAgo.getTime().after(playerData.getLastLogin());
			if(newPlayerClaimsExpired && playerData.getClaims().size() == 1)
			{
				claim.removeSurfaceFluids(null);
				GriefPrevention.instance.dataStore.deleteClaim(claim);
				cleanupChunks = true;
				
				//if configured to do so, restore the land to natural
				if((GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner()) && GriefPrevention.instance.config_claims_creativeAutoNatureRestoration) || GriefPrevention.instance.config_claims_survivalAutoNatureRestoration)
				{
					GriefPrevention.instance.restoreClaim(claim, 0);
				}
				
				GriefPrevention.AddLogEntry(" " + claim.getOwnerName() + "'s new player claim expired.");
			}
		}
		
		//if configured to always remove claims after some inactivity period without exceptions...
		else if(GriefPrevention.instance.config_claims_expirationDays > 0)
		{
			if(playerData == null) playerData = GriefPrevention.instance.dataStore.getPlayerData(claim.ownerID);
		    Calendar earliestPermissibleLastLogin = Calendar.getInstance();
			earliestPermissibleLastLogin.add(Calendar.DATE, -GriefPrevention.instance.config_claims_expirationDays);
			
			if(earliestPermissibleLastLogin.getTime().after(playerData.getLastLogin()))
			{
				//make a copy of this player's claim list
				Vector<Claim> claims = new Vector<Claim>();
				for(int i = 0; i < playerData.getClaims().size(); i++)
				{
					claims.add(playerData.getClaims().get(i));
				}
				
				//delete them
				GriefPrevention.instance.dataStore.deleteClaimsForPlayer(claim.ownerID, true);
				GriefPrevention.AddLogEntry(" All of " + claim.getOwnerName() + "'s claims have expired.");
				
				for(int i = 0; i < claims.size(); i++)
				{
					//if configured to do so, restore the land to natural
					if((GriefPrevention.instance.creativeRulesApply(claims.get(i).getLesserBoundaryCorner()) && GriefPrevention.instance.config_claims_creativeAutoNatureRestoration) || GriefPrevention.instance.config_claims_survivalAutoNatureRestoration)
					{
						GriefPrevention.instance.restoreClaim(claims.get(i), 0);
						cleanupChunks = true;				
					}
				}
			}
		}
		
		else if(GriefPrevention.instance.config_claims_unusedClaimExpirationDays > 0 && GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner()))
		{		
			//avoid scanning large claims and administrative claims
			if(claim.isAdminClaim() || claim.getWidth() > 25 || claim.getHeight() > 25) return;
			
			//otherwise scan the claim content
			int minInvestment = 400;
			
			long investmentScore = claim.getPlayerInvestmentScore();
			cleanupChunks = true;
			
			if(investmentScore < minInvestment)
			{
				GriefPrevention.instance.dataStore.deleteClaim(claim);
				GriefPrevention.AddLogEntry("Removed " + claim.getOwnerName() + "'s unused claim @ " + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
				
				//if configured to do so, restore the claim area to natural state
				if((GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner()) && GriefPrevention.instance.config_claims_creativeAutoNatureRestoration) || GriefPrevention.instance.config_claims_survivalAutoNatureRestoration)
				{
					GriefPrevention.instance.restoreClaim(claim, 0);
				}
			}
		}
		
		if(playerData != null) GriefPrevention.instance.dataStore.clearCachedPlayerData(claim.ownerID);
		
		//since we're potentially loading a lot of chunks to scan parts of the world where there are no players currently playing, be mindful of memory usage
		if(cleanupChunks)
		{
			World world = claim.getLesserBoundaryCorner().getWorld();
			Chunk lesserChunk = world.getChunkAt(claim.getLesserBoundaryCorner());
			Chunk greaterChunk = world.getChunkAt(claim.getGreaterBoundaryCorner());
			for(int x = lesserChunk.getX(); x <= greaterChunk.getX(); x++)
			{
			    for(int z = lesserChunk.getZ(); z <= greaterChunk.getZ(); z++)
			    {
			        Chunk chunk = world.getChunkAt(x, z);
			        if(chunk.isLoaded())
			        {
			            chunk.unload(true, true);
			        }
			    }
			}
		}
	}
}
