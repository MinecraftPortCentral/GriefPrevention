/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2011 Ryan Hamshire

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
		
		//get data for the player, especially last login timestamp
		PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(claim.ownerName);
		
		//determine area of the default chest claim
		int areaOfDefaultClaim = 0;
		if(GriefPrevention.instance.config_claims_automaticClaimsForNewPlayersRadius >= 0)
		{
			areaOfDefaultClaim = (int)Math.pow(GriefPrevention.instance.config_claims_automaticClaimsForNewPlayersRadius * 2 + 1, 2);  
		}
		
		//if he's been gone at least a week, if he has ONLY the new player claim, it will be removed
		Calendar sevenDaysAgo = Calendar.getInstance();
		sevenDaysAgo.add(Calendar.DATE, -7);
		boolean newPlayerClaimsExpired = sevenDaysAgo.getTime().after(playerData.lastLogin);
		
		//if only one claim, and the player hasn't played in a week
		if(newPlayerClaimsExpired && playerData.claims.size() == 1)
		{
			//if that's a chest claim, delete it
			if(claim.getArea() <= areaOfDefaultClaim)
			{
				claim.removeSurfaceFluids(null);
				GriefPrevention.instance.dataStore.deleteClaim(claim);
				
				//if in a creative mode world, delete the claim
				if(GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner()))
				{
					GriefPrevention.instance.restoreClaim(claim, 0);
				}
				
				GriefPrevention.AddLogEntry(" " + claim.getOwnerName() + "'s new player claim expired.");
			}
		}
		
		//if configured to always remove claims after some inactivity period without exceptions...
		else if(GriefPrevention.instance.config_claims_expirationDays > 0)
		{
			Calendar earliestPermissibleLastLogin = Calendar.getInstance();
			earliestPermissibleLastLogin.add(Calendar.DATE, -GriefPrevention.instance.config_claims_expirationDays);
			
			if(earliestPermissibleLastLogin.getTime().after(playerData.lastLogin))
			{
				GriefPrevention.instance.dataStore.deleteClaimsForPlayer(claim.getOwnerName(), true);
				GriefPrevention.AddLogEntry(" All of " + claim.getOwnerName() + "'s claims have expired.");
			}
		}
		
		else
		{
		
			//if the player has been gone two weeks, scan claim content to assess player investment
			Calendar fourteenDaysAgo = Calendar.getInstance();
			fourteenDaysAgo.add(Calendar.DATE, -14);
			boolean needsInvestmentScan = fourteenDaysAgo.getTime().after(playerData.lastLogin);
			
			//avoid scanning large claims and administrative claims
			if(claim.isAdminClaim() || claim.getWidth() > 25 || claim.getHeight() > 25) return;
			
			//if creative mode or the claim owner has been away a long enough time, scan the claim content
			if(needsInvestmentScan || GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner()))
			{
				int minInvestment;
				if(GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner()))
				{
					minInvestment = 400;
				}
				else
				{
					minInvestment = 200;
				}
				
				long investmentScore = claim.getPlayerInvestmentScore();
				boolean removeClaim = false;
				
				//in creative mode, a build which is almost entirely lava above sea level will be automatically removed, even if the owner is an active player
				//lava above the surface deducts 10 points per block from the investment score
				//so 500 blocks of lava without anything built to offset all that potential mess would be cleaned up automatically
				if(GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner()) && investmentScore < -5000)
				{
					removeClaim = true;
				}			
				
				//otherwise, the only way to get a claim automatically removed based on build investment is to be away for two weeks AND not build much of anything
				else if(needsInvestmentScan && investmentScore < minInvestment)
				{
					removeClaim = true;
				}
				
				if(removeClaim)
				{
					GriefPrevention.instance.dataStore.deleteClaim(claim);
					GriefPrevention.AddLogEntry("Removed " + claim.getOwnerName() + "'s unused claim @ " + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
					
					//if in a creative mode world, restore the claim area
					if(GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner()))
					{
						GriefPrevention.instance.restoreClaim(claim, 0);
					}
				}
			}
		}
		
		//toss that player data out of the cache, it's probably not needed in memory right now
		GriefPrevention.instance.dataStore.clearCachedPlayerData(claim.ownerName);
		
		//since we're potentially loading a lot of chunks to scan parts of the world where there are no players currently playing, be mindful of memory usage
		//unfortunately, java/minecraft don't do a good job of clearing unused memory, leading to out of memory errors from this type of world scanning
		if(this.nextClaimIndex % 20 == 0)
		{
			World world = claim.getLesserBoundaryCorner().getWorld();
			Chunk [] chunks = world.getLoadedChunks();
			for(int i = 0; i < chunks.length; i++)
			{
				Chunk chunk = chunks[i];
				chunk.unload(true, true);
			}
			
			System.gc();
		}
	}
}
