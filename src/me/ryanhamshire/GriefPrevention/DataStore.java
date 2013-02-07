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

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

//singleton class which manages all GriefPrevention data (except for config options)
public abstract class DataStore 
{
	//in-memory cache for player data
	protected ConcurrentHashMap<String, PlayerData> playerNameToPlayerDataMap = new ConcurrentHashMap<String, PlayerData>();
	
	//in-memory cache for group (permission-based) data
	protected ConcurrentHashMap<String, Integer> permissionToBonusBlocksMap = new ConcurrentHashMap<String, Integer>();
	
	//in-memory cache for claim data
	ArrayList<Claim> claims = new ArrayList<Claim>();
	
	//in-memory cache for messages
	private String [] messages;
	
	//next claim ID
	Long nextClaimID = (long)0;
	
	//path information, for where stuff stored on disk is well...  stored
	protected final static String dataLayerFolderPath = "plugins" + File.separator + "GriefPreventionData";
	final static String configFilePath = dataLayerFolderPath + File.separator + "config.yml";
	final static String messagesFilePath = dataLayerFolderPath + File.separator + "messages.yml";
	
	//initialization!
	void initialize() throws Exception
	{
		GriefPrevention.AddLogEntry(this.claims.size() + " total claims loaded.");
		
		//make a list of players who own claims
		Vector<String> playerNames = new Vector<String>();
		for(int i = 0; i < this.claims.size(); i++)
		{
			Claim claim = this.claims.get(i);
			
			//ignore admin claims
			if(claim.isAdminClaim()) continue;
			
			if(!playerNames.contains(claim.ownerName))
				playerNames.add(claim.ownerName);
		}
		
		GriefPrevention.AddLogEntry(playerNames.size() + " players have staked claims.");
		
		//load up all the messages from messages.yml
		this.loadMessages();
		
		//collect garbage, since lots of stuff was loaded into memory and then tossed out
		System.gc();
	}
	
	//removes cached player data from memory
	synchronized void clearCachedPlayerData(String playerName)
	{
		this.playerNameToPlayerDataMap.remove(playerName);
	}
	
	//gets the number of bonus blocks a player has from his permissions
	synchronized int getGroupBonusBlocks(String playerName)
	{
		int bonusBlocks = 0;
		Set<String> keys = permissionToBonusBlocksMap.keySet();
		Iterator<String> iterator = keys.iterator();
		while(iterator.hasNext())
		{
			String groupName = iterator.next();
			Player player = GriefPrevention.instance.getServer().getPlayer(playerName);
			if(player != null && player.hasPermission(groupName))
			{
				bonusBlocks += this.permissionToBonusBlocksMap.get(groupName);
			}
		}
		
		return bonusBlocks;
	}
	
	//grants a group (players with a specific permission) bonus claim blocks as long as they're still members of the group
	synchronized public int adjustGroupBonusBlocks(String groupName, int amount)
	{
		Integer currentValue = this.permissionToBonusBlocksMap.get(groupName);
		if(currentValue == null) currentValue = 0;
		
		currentValue += amount;
		this.permissionToBonusBlocksMap.put(groupName, currentValue);
		
		//write changes to storage to ensure they don't get lost
		this.saveGroupBonusBlocks(groupName, currentValue);
		
		return currentValue;		
	}
	
	abstract void saveGroupBonusBlocks(String groupName, int amount);
	
	synchronized public void changeClaimOwner(Claim claim, String newOwnerName) throws Exception
	{
		//if it's a subdivision, throw an exception
		if(claim.parent != null)
		{
			throw new Exception("Subdivisions can't be transferred.  Only top-level claims may change owners.");
		}
		
		//otherwise update information
		
		//determine current claim owner
		PlayerData ownerData = null;
		if(!claim.isAdminClaim())
		{
			ownerData = this.getPlayerData(claim.ownerName);
		}
		
		//determine new owner
		PlayerData newOwnerData = this.getPlayerData(newOwnerName);
		
		//transfer
		claim.ownerName = newOwnerName;
		this.saveClaim(claim);
		
		//adjust blocks and other records
		if(ownerData != null)
		{
			ownerData.claims.remove(claim);
			ownerData.bonusClaimBlocks -= claim.getArea();
			this.savePlayerData(claim.ownerName, ownerData);
		}
		
		newOwnerData.claims.add(claim);
		newOwnerData.bonusClaimBlocks += claim.getArea();
		this.savePlayerData(newOwnerName, newOwnerData);
	}

	//adds a claim to the datastore, making it an effective claim
	synchronized void addClaim(Claim newClaim)
	{
		//subdivisions are easy
		if(newClaim.parent != null)
		{
			newClaim.parent.children.add(newClaim);
			newClaim.inDataStore = true;
			this.saveClaim(newClaim);
			return;
		}
		
		//add it and mark it as added
		int j = 0;
		while(j < this.claims.size() && !this.claims.get(j).greaterThan(newClaim)) j++;
		if(j < this.claims.size())
			this.claims.add(j, newClaim);
		else
			this.claims.add(this.claims.size(), newClaim);
		newClaim.inDataStore = true;
		
		//except for administrative claims (which have no owner), update the owner's playerData with the new claim
		if(!newClaim.isAdminClaim())
		{
			PlayerData ownerData = this.getPlayerData(newClaim.getOwnerName());
			ownerData.claims.add(newClaim);
			this.savePlayerData(newClaim.getOwnerName(), ownerData);
		}
		
		//make sure the claim is saved to disk
		this.saveClaim(newClaim);
	}
	
	//turns a location into a string, useful in data storage
	private String locationStringDelimiter = ";";	
	String locationToString(Location location)
	{
		StringBuilder stringBuilder = new StringBuilder(location.getWorld().getName());
		stringBuilder.append(locationStringDelimiter);
		stringBuilder.append(location.getBlockX());
		stringBuilder.append(locationStringDelimiter);
		stringBuilder.append(location.getBlockY());
		stringBuilder.append(locationStringDelimiter);
		stringBuilder.append(location.getBlockZ());
		
		return stringBuilder.toString();
	}
	
	//turns a location string back into a location
	Location locationFromString(String string) throws Exception
	{
		//split the input string on the space
		String [] elements = string.split(locationStringDelimiter);
	    
		//expect four elements - world name, X, Y, and Z, respectively
		if(elements.length != 4)
		{
			throw new Exception("Expected four distinct parts to the location string.");
		}
		
		String worldName = elements[0];
		String xString = elements[1];
		String yString = elements[2];
		String zString = elements[3];
	    
		//identify world the claim is in
		World world = GriefPrevention.instance.getServer().getWorld(worldName);
		if(world == null)
		{
			throw new Exception("World not found: \"" + worldName + "\"");
		}
		
		//convert those numerical strings to integer values
	    int x = Integer.parseInt(xString);
	    int y = Integer.parseInt(yString);
	    int z = Integer.parseInt(zString);
	    
	    return new Location(world, x, y, z);
	}	

	//saves any changes to a claim to secondary storage
	synchronized public void saveClaim(Claim claim)
	{
		//subdivisions don't save to their own files, but instead live in their parent claim's file
		//so any attempt to save a subdivision will save its parent (and thus the subdivision)
		if(claim.parent != null)
		{
			this.saveClaim(claim.parent);
			return;
		}
		
		//otherwise get a unique identifier for the claim which will be used to name the file on disk
		if(claim.id == null)
		{
			claim.id = this.nextClaimID;
			this.incrementNextClaimID();
		}
		
		this.writeClaimToStorage(claim);
	}
	
	abstract void writeClaimToStorage(Claim claim);
	
	//increments the claim ID and updates secondary storage to be sure it's saved
	abstract void incrementNextClaimID();
	
	//retrieves player data from memory or secondary storage, as necessary
	//if the player has never been on the server before, this will return a fresh player data with default values
	synchronized public PlayerData getPlayerData(String playerName)
	{
		//first, look in memory
		PlayerData playerData = this.playerNameToPlayerDataMap.get(playerName);
		
		//if not there, look in secondary storage
		if(playerData == null)
		{
			playerData = this.getPlayerDataFromStorage(playerName);
			playerData.playerName = playerName;
			
			//find all the claims belonging to this player and note them for future reference
			for(int i = 0; i < this.claims.size(); i++)
			{
				Claim claim = this.claims.get(i);
				if(claim.ownerName.equals(playerName))
				{
					playerData.claims.add(claim);
				}
			}
			
			//shove that new player data into the hash map cache
			this.playerNameToPlayerDataMap.put(playerName, playerData);
		}
		
		//try the hash map again.  if it's STILL not there, we have a bug to fix
		return this.playerNameToPlayerDataMap.get(playerName);
	}
	
	abstract PlayerData getPlayerDataFromStorage(String playerName);
	
	//deletes a claim or subdivision
	synchronized public void deleteClaim(Claim claim)
	{
		//subdivisions are simple - just remove them from their parent claim and save that claim
		if(claim.parent != null)
		{
			Claim parentClaim = claim.parent;
			parentClaim.children.remove(claim);
			this.saveClaim(parentClaim);
			return;
		}
		
		//remove from memory
		for(int i = 0; i < this.claims.size(); i++)
		{
			if(claims.get(i).id.equals(claim.id))
			{
				this.claims.remove(i);
				claim.inDataStore = false;
				for(int j = 0; j < claim.children.size(); j++)
				{
					claim.children.get(j).inDataStore = false;
				}
				break;
			}
		}
		
		//remove from secondary storage
		this.deleteClaimFromSecondaryStorage(claim);
		
		//update player data, except for administrative claims, which have no owner
		if(!claim.isAdminClaim())
		{
			PlayerData ownerData = this.getPlayerData(claim.getOwnerName());
			for(int i = 0; i < ownerData.claims.size(); i++)
			{
				if(ownerData.claims.get(i).id.equals(claim.id))
				{
					ownerData.claims.remove(i);
					break;
				}
			}
			this.savePlayerData(claim.getOwnerName(), ownerData);
		}
	}
	
	abstract void deleteClaimFromSecondaryStorage(Claim claim);
	
	//gets the claim at a specific location
	//ignoreHeight = TRUE means that a location UNDER an existing claim will return the claim
	//cachedClaim can be NULL, but will help performance if you have a reasonable guess about which claim the location is in
	synchronized public Claim getClaimAt(Location location, boolean ignoreHeight, Claim cachedClaim)
	{
		//check cachedClaim guess first.  if it's in the datastore and the location is inside it, we're done
		if(cachedClaim != null && cachedClaim.inDataStore && cachedClaim.contains(location, ignoreHeight, true)) return cachedClaim;
		
		//the claims list is ordered by greater boundary corner
		//create a temporary "fake" claim in memory for comparison purposes		
		Claim tempClaim = new Claim();
		tempClaim.lesserBoundaryCorner = location;
		
		//otherwise, search all existing claims until we find the right claim
		for(int i = 0; i < this.claims.size(); i++)
		{
			Claim claim = this.claims.get(i);
			
			//if we reach a claim which is greater than the temp claim created above, there's definitely no claim
			//in the collection which includes our location
			if(claim.greaterThan(tempClaim)) return null;
			
			//find a top level claim
			if(claim.contains(location, ignoreHeight, false))
			{
				//when we find a top level claim, if the location is in one of its subdivisions,
				//return the SUBDIVISION, not the top level claim
				for(int j = 0; j < claim.children.size(); j++)
				{
					Claim subdivision = claim.children.get(j);
					if(subdivision.contains(location, ignoreHeight, false)) return subdivision;
				}						
					
				return claim;
			}
		}
		
		//if no claim found, return null
		return null;
	}
	
	//creates a claim.
	//if the new claim would overlap an existing claim, returns a failure along with a reference to the existing claim
	//otherwise, returns a success along with a reference to the new claim
	//use ownerName == "" for administrative claims
	//for top level claims, pass parent == NULL
	//DOES adjust claim blocks available on success (players can go into negative quantity available)
	//does NOT check a player has permission to create a claim, or enough claim blocks.
	//does NOT check minimum claim size constraints
	//does NOT visualize the new claim for any players	
	synchronized public CreateClaimResult createClaim(World world, int x1, int x2, int y1, int y2, int z1, int z2, String ownerName, Claim parent, Long id)
	{
		CreateClaimResult result = new CreateClaimResult();
		
		int smallx, bigx, smally, bigy, smallz, bigz;

		//determine small versus big inputs
		if(x1 < x2)
		{
			smallx = x1;
			bigx = x2;
		}
		else
		{
			smallx = x2;
			bigx = x1;
		}
		
		if(y1 < y2)
		{
			smally = y1;
			bigy = y2;
		}
		else
		{
			smally = y2;
			bigy = y1;
		}
		
		if(z1 < z2)
		{
			smallz = z1;
			bigz = z2;
		}
		else
		{
			smallz = z2;
			bigz = z1;
		}
		
		//creative mode claims always go to bedrock
		if(GriefPrevention.instance.config_claims_enabledCreativeWorlds.contains(world))
		{
			smally = 2;
		}
		
		//create a new claim instance (but don't save it, yet)
		Claim newClaim = new Claim(
			new Location(world, smallx, smally, smallz),
			new Location(world, bigx, bigy, bigz),
			ownerName,
			new String [] {}, 
			new String [] {},
			new String [] {},
			new String [] {},
			id);
		
		newClaim.parent = parent;
		
		//ensure this new claim won't overlap any existing claims
		ArrayList<Claim> claimsToCheck;
		if(newClaim.parent != null)
		{
			claimsToCheck = newClaim.parent.children;			
		}
		else
		{
			claimsToCheck = this.claims;
		}

		for(int i = 0; i < claimsToCheck.size(); i++)
		{
			Claim otherClaim = claimsToCheck.get(i);
			
			//if we find an existing claim which will be overlapped
			if(otherClaim.overlaps(newClaim))
			{
				//result = fail, return conflicting claim
				result.succeeded = false;
				result.claim = otherClaim;
				return result;
			}
		}
		
		//otherwise add this new claim to the data store to make it effective
		this.addClaim(newClaim);
		
		//then return success along with reference to new claim
		result.succeeded = true;
		result.claim = newClaim;
		return result;
	}
	
	//saves changes to player data to secondary storage.  MUST be called after you're done making changes, otherwise a reload will lose them
	public abstract void savePlayerData(String playerName, PlayerData playerData);
	
	//extends a claim to a new depth
	//respects the max depth config variable
	synchronized public void extendClaim(Claim claim, int newDepth) 
	{
		if(newDepth < GriefPrevention.instance.config_claims_maxDepth) newDepth = GriefPrevention.instance.config_claims_maxDepth;
		
		if(claim.parent != null) claim = claim.parent;
		
		//delete the claim
		this.deleteClaim(claim);
		
		//re-create it at the new depth
		claim.lesserBoundaryCorner.setY(newDepth);
		claim.greaterBoundaryCorner.setY(newDepth);
		
		//make all subdivisions reach to the same depth
		for(int i = 0; i < claim.children.size(); i++)
		{
			claim.children.get(i).lesserBoundaryCorner.setY(newDepth);
			claim.children.get(i).greaterBoundaryCorner.setY(newDepth);
		}
		
		//save changes
		this.addClaim(claim);
	}

	//starts a siege on a claim
	//does NOT check siege cooldowns, see onCooldown() below
	synchronized public void startSiege(Player attacker, Player defender, Claim defenderClaim)
	{
		//fill-in the necessary SiegeData instance
		SiegeData siegeData = new SiegeData(attacker, defender, defenderClaim);
		PlayerData attackerData = this.getPlayerData(attacker.getName());
		PlayerData defenderData = this.getPlayerData(defender.getName());
		attackerData.siegeData = siegeData;
		defenderData.siegeData = siegeData;
		defenderClaim.siegeData = siegeData;
		
		//start a task to monitor the siege
		//why isn't this a "repeating" task?
		//because depending on the status of the siege at the time the task runs, there may or may not be a reason to run the task again
		SiegeCheckupTask task = new SiegeCheckupTask(siegeData);
		siegeData.checkupTaskID = GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task, 20L * 30);
	}
	
	//ends a siege
	//either winnerName or loserName can be null, but not both
	synchronized public void endSiege(SiegeData siegeData, String winnerName, String loserName, boolean death)
	{
		boolean grantAccess = false;
		
		//determine winner and loser
		if(winnerName == null && loserName != null)
		{
			if(siegeData.attacker.getName().equals(loserName))
			{
				winnerName = siegeData.defender.getName();
			}
			else
			{
				winnerName = siegeData.attacker.getName();
			}
		}
		else if(winnerName != null && loserName == null)
		{
			if(siegeData.attacker.getName().equals(winnerName))
			{
				loserName = siegeData.defender.getName();
			}
			else
			{
				loserName = siegeData.attacker.getName();
			}
		}
		
		//if the attacker won, plan to open the doors for looting
		if(siegeData.attacker.getName().equals(winnerName))
		{
			grantAccess = true;
		}
		
		PlayerData attackerData = this.getPlayerData(siegeData.attacker.getName());
		attackerData.siegeData = null;
		
		PlayerData defenderData = this.getPlayerData(siegeData.defender.getName());	
		defenderData.siegeData = null;

		//start a cooldown for this attacker/defender pair
		Long now = Calendar.getInstance().getTimeInMillis();
		Long cooldownEnd = now + 1000 * 60 * 60;  //one hour from now
		this.siegeCooldownRemaining.put(siegeData.attacker.getName() + "_" + siegeData.defender.getName(), cooldownEnd);
		
		//start cooldowns for every attacker/involved claim pair
		for(int i = 0; i < siegeData.claims.size(); i++)
		{
			Claim claim = siegeData.claims.get(i);
			claim.siegeData = null;
			this.siegeCooldownRemaining.put(siegeData.attacker.getName() + "_" + claim.ownerName, cooldownEnd);
			
			//if doors should be opened for looting, do that now
			if(grantAccess)
			{
				claim.doorsOpen = true;
			}
		}

		//cancel the siege checkup task
		GriefPrevention.instance.getServer().getScheduler().cancelTask(siegeData.checkupTaskID);
		
		//notify everyone who won and lost
		if(winnerName != null && loserName != null)
		{
			GriefPrevention.instance.getServer().broadcastMessage(winnerName + " defeated " + loserName + " in siege warfare!");
		}
		
		//if the claim should be opened to looting
		if(grantAccess)
		{
			Player winner = GriefPrevention.instance.getServer().getPlayer(winnerName);
			if(winner != null)
			{
				//notify the winner
				GriefPrevention.sendMessage(winner, TextMode.Success, Messages.SiegeWinDoorsOpen);
				
				//schedule a task to secure the claims in about 5 minutes
				SecureClaimTask task = new SecureClaimTask(siegeData);
				GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task, 20L * 60 * 5);
			}
		}
		
		//if the siege ended due to death, transfer inventory to winner
		if(death)
		{
			Player winner = GriefPrevention.instance.getServer().getPlayer(winnerName);
			Player loser = GriefPrevention.instance.getServer().getPlayer(loserName);
			if(winner != null && loser != null)
			{
				//get loser's inventory, then clear it
				ItemStack [] loserItems = loser.getInventory().getContents();
				loser.getInventory().clear();
				
				//try to add it to the winner's inventory
				for(int j = 0; j < loserItems.length; j++)
				{
					if(loserItems[j] == null || loserItems[j].getType() == Material.AIR || loserItems[j].getAmount() == 0) continue;
					
					HashMap<Integer, ItemStack> wontFitItems = winner.getInventory().addItem(loserItems[j]);
					
					//drop any remainder on the ground at his feet
					Object [] keys = wontFitItems.keySet().toArray();
					Location winnerLocation = winner.getLocation(); 
					for(int i = 0; i < keys.length; i++)
					{
						Integer key = (Integer)keys[i];
						winnerLocation.getWorld().dropItemNaturally(winnerLocation, wontFitItems.get(key));
					}
				}
			}
		}
	}
	
	//timestamp for each siege cooldown to end
	private HashMap<String, Long> siegeCooldownRemaining = new HashMap<String, Long>();

	//whether or not a sieger can siege a particular victim or claim, considering only cooldowns
	synchronized public boolean onCooldown(Player attacker, Player defender, Claim defenderClaim)
	{
		Long cooldownEnd = null;
		
		//look for an attacker/defender cooldown
		if(this.siegeCooldownRemaining.get(attacker.getName() + "_" + defender.getName()) != null)
		{
			cooldownEnd = this.siegeCooldownRemaining.get(attacker.getName() + "_" + defender.getName());
			
			if(Calendar.getInstance().getTimeInMillis() < cooldownEnd)
			{
				return true;
			}
			
			//if found but expired, remove it
			this.siegeCooldownRemaining.remove(attacker.getName() + "_" + defender.getName());
		}
		
		//look for an attacker/claim cooldown
		if(cooldownEnd == null && this.siegeCooldownRemaining.get(attacker.getName() + "_" + defenderClaim.ownerName) != null)
		{
			cooldownEnd = this.siegeCooldownRemaining.get(attacker.getName() + "_" + defenderClaim.ownerName);
			
			if(Calendar.getInstance().getTimeInMillis() < cooldownEnd)
			{
				return true;
			}
			
			//if found but expired, remove it
			this.siegeCooldownRemaining.remove(attacker.getName() + "_" + defenderClaim.ownerName);			
		}
		
		return false;
	}

	//extend a siege, if it's possible to do so
	synchronized void tryExtendSiege(Player player, Claim claim)
	{
		PlayerData playerData = this.getPlayerData(player.getName());
		
		//player must be sieged
		if(playerData.siegeData == null) return;
		
		//claim isn't already under the same siege
		if(playerData.siegeData.claims.contains(claim)) return;
		
		//admin claims can't be sieged
		if(claim.isAdminClaim()) return;
		
		//player must have some level of permission to be sieged in a claim
		if(claim.allowAccess(player) != null) return;
		
		//otherwise extend the siege
		playerData.siegeData.claims.add(claim);
		claim.siegeData = playerData.siegeData;
	}		
	
	//deletes all claims owned by a player
	synchronized public void deleteClaimsForPlayer(String playerName, boolean deleteCreativeClaims)
	{
		//make a list of the player's claims
		ArrayList<Claim> claimsToDelete = new ArrayList<Claim>();
		for(int i = 0; i < this.claims.size(); i++)
		{
			Claim claim = this.claims.get(i);
			if(claim.ownerName.equals(playerName) && (deleteCreativeClaims || !GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner())))
				claimsToDelete.add(claim);
		}
		
		//delete them one by one
		for(int i = 0; i < claimsToDelete.size(); i++)
		{
			Claim claim = claimsToDelete.get(i); 
			claim.removeSurfaceFluids(null);
			
			this.deleteClaim(claim);
			
			//if in a creative mode world, delete the claim
			if(GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner()))
			{
				GriefPrevention.instance.restoreClaim(claim, 0);
			}
		}					
	}

	//tries to resize a claim
	//see CreateClaim() for details on return value
	synchronized public CreateClaimResult resizeClaim(Claim claim, int newx1, int newx2, int newy1, int newy2, int newz1, int newz2)
	{
		//remove old claim
		this.deleteClaim(claim);					
		
		//try to create this new claim, ignoring the original when checking for overlap
		CreateClaimResult result = this.createClaim(claim.getLesserBoundaryCorner().getWorld(), newx1, newx2, newy1, newy2, newz1, newz2, claim.ownerName, claim.parent, claim.id);
		
		//if succeeded
		if(result.succeeded)
		{
			//copy permissions from old claim
			ArrayList<String> builders = new ArrayList<String>();
			ArrayList<String> containers = new ArrayList<String>();
			ArrayList<String> accessors = new ArrayList<String>();
			ArrayList<String> managers = new ArrayList<String>();
			claim.getPermissions(builders, containers, accessors, managers);
			
			for(int i = 0; i < builders.size(); i++)
				result.claim.setPermission(builders.get(i), ClaimPermission.Build);
			
			for(int i = 0; i < containers.size(); i++)
				result.claim.setPermission(containers.get(i), ClaimPermission.Inventory);
			
			for(int i = 0; i < accessors.size(); i++)
				result.claim.setPermission(accessors.get(i), ClaimPermission.Access);
			
			for(int i = 0; i < managers.size(); i++)
			{
				result.claim.managers.add(managers.get(i));
			}
			
			//copy subdivisions from old claim
			for(int i = 0; i < claim.children.size(); i++)
			{
				Claim subdivision = claim.children.get(i);
				subdivision.parent = result.claim;
				result.claim.children.add(subdivision);
			}
			
			//save those changes
			this.saveClaim(result.claim);
		}
		
		else
		{
			//put original claim back
			this.addClaim(claim);
		}
		
		return result;
	}
	
	private void loadMessages() 
	{
		Messages [] messageIDs = Messages.values();
		this.messages = new String[Messages.values().length];
		
		HashMap<String, CustomizableMessage> defaults = new HashMap<String, CustomizableMessage>();
		
		//initialize defaults
		this.addDefault(defaults, Messages.RespectingClaims, "Now respecting claims.", null);
		this.addDefault(defaults, Messages.IgnoringClaims, "Now ignoring claims.", null);
		this.addDefault(defaults, Messages.NoCreativeUnClaim, "You can't unclaim this land.  You can only make this claim larger or create additional claims.", null);
		this.addDefault(defaults, Messages.SuccessfulAbandon, "Claims abandoned.  You now have {0} available claim blocks.", "0: remaining blocks");
		this.addDefault(defaults, Messages.RestoreNatureActivate, "Ready to restore some nature!  Right click to restore nature, and use /BasicClaims to stop.", null);
		this.addDefault(defaults, Messages.RestoreNatureAggressiveActivate, "Aggressive mode activated.  Do NOT use this underneath anything you want to keep!  Right click to aggressively restore nature, and use /BasicClaims to stop.", null);
		this.addDefault(defaults, Messages.FillModeActive, "Fill mode activated with radius {0}.  Right click an area to fill.", "0: fill radius");
		this.addDefault(defaults, Messages.TransferClaimPermission, "That command requires the administrative claims permission.", null);
		this.addDefault(defaults, Messages.TransferClaimMissing, "There's no claim here.  Stand in the administrative claim you want to transfer.", null);
		this.addDefault(defaults, Messages.TransferClaimAdminOnly, "Only administrative claims may be transferred to a player.", null);
		this.addDefault(defaults, Messages.PlayerNotFound, "Player not found.", null);
		this.addDefault(defaults, Messages.TransferTopLevel, "Only top level claims (not subdivisions) may be transferred.  Stand outside of the subdivision and try again.", null);
		this.addDefault(defaults, Messages.TransferSuccess, "Claim transferred.", null);
		this.addDefault(defaults, Messages.TrustListNoClaim, "Stand inside the claim you're curious about.", null);
		this.addDefault(defaults, Messages.ClearPermsOwnerOnly, "Only the claim owner can clear all permissions.", null);
		this.addDefault(defaults, Messages.UntrustIndividualAllClaims, "Revoked {0}'s access to ALL your claims.  To set permissions for a single claim, stand inside it.", "0: untrusted player");
		this.addDefault(defaults, Messages.UntrustEveryoneAllClaims, "Cleared permissions in ALL your claims.  To set permissions for a single claim, stand inside it.", null);
		this.addDefault(defaults, Messages.NoPermissionTrust, "You don't have {0}'s permission to manage permissions here.", "0: claim owner's name");
		this.addDefault(defaults, Messages.ClearPermissionsOneClaim, "Cleared permissions in this claim.  To set permission for ALL your claims, stand outside them.", null);
		this.addDefault(defaults, Messages.UntrustIndividualSingleClaim, "Revoked {0}'s access to this claim.  To set permissions for a ALL your claims, stand outside them.", "0: untrusted player");
		this.addDefault(defaults, Messages.OnlySellBlocks, "Claim blocks may only be sold, not purchased.", null);
		this.addDefault(defaults, Messages.BlockPurchaseCost, "Each claim block costs {0}.  Your balance is {1}.", "0: cost of one block; 1: player's account balance");
		this.addDefault(defaults, Messages.ClaimBlockLimit, "You've reached your claim block limit.  You can't purchase more.", null);
		this.addDefault(defaults, Messages.InsufficientFunds, "You don't have enough money.  You need {0}, but you only have {1}.", "0: total cost; 1: player's account balance");
		this.addDefault(defaults, Messages.PurchaseConfirmation, "Withdrew {0} from your account.  You now have {1} available claim blocks.", "0: total cost; 1: remaining blocks");
		this.addDefault(defaults, Messages.OnlyPurchaseBlocks, "Claim blocks may only be purchased, not sold.", null);
		this.addDefault(defaults, Messages.BlockSaleValue, "Each claim block is worth {0}.  You have {1} available for sale.", "0: block value; 1: available blocks");
		this.addDefault(defaults, Messages.NotEnoughBlocksForSale, "You don't have that many claim blocks available for sale.", null);
		this.addDefault(defaults, Messages.BlockSaleConfirmation, "Deposited {0} in your account.  You now have {1} available claim blocks.", "0: amount deposited; 1: remaining blocks");
		this.addDefault(defaults, Messages.AdminClaimsMode, "Administrative claims mode active.  Any claims created will be free and editable by other administrators.", null);
		this.addDefault(defaults, Messages.BasicClaimsMode, "Returned to basic claim creation mode.", null);
		this.addDefault(defaults, Messages.SubdivisionMode, "Subdivision mode.  Use your shovel to create subdivisions in your existing claims.  Use /basicclaims to exit.", null);
		this.addDefault(defaults, Messages.SubdivisionDemo, "Land Claim Help:  http://tinyurl.com/7urdtue", null);
		this.addDefault(defaults, Messages.DeleteClaimMissing, "There's no claim here.", null);
		this.addDefault(defaults, Messages.DeletionSubdivisionWarning, "This claim includes subdivisions.  If you're sure you want to delete it, use /DeleteClaim again.", null);
		this.addDefault(defaults, Messages.DeleteSuccess, "Claim deleted.", null);
		this.addDefault(defaults, Messages.CantDeleteAdminClaim, "You don't have permission to delete administrative claims.", null);
		this.addDefault(defaults, Messages.DeleteAllSuccess, "Deleted all of {0}'s claims.", "0: owner's name");
		this.addDefault(defaults, Messages.NoDeletePermission, "You don't have permission to delete claims.", null);
		this.addDefault(defaults, Messages.AllAdminDeleted, "Deleted all administrative claims.", null);
		this.addDefault(defaults, Messages.AdjustBlocksSuccess, "Adjusted {0}'s bonus claim blocks by {1}.  New total bonus blocks: {2}.", "0: player; 1: adjustment; 2: new total");
		this.addDefault(defaults, Messages.NotTrappedHere, "You can build here.  Save yourself.", null);
		this.addDefault(defaults, Messages.TrappedOnCooldown, "You used /trapped within the last {0} hours.  You have to wait about {1} more minutes before using it again.", "0: default cooldown hours; 1: remaining minutes");
		this.addDefault(defaults, Messages.RescuePending, "If you stay put for 10 seconds, you'll be teleported out.  Please wait.", null);
		this.addDefault(defaults, Messages.NonSiegeWorld, "Siege is disabled here.", null);
		this.addDefault(defaults, Messages.AlreadySieging, "You're already involved in a siege.", null);
		this.addDefault(defaults, Messages.AlreadyUnderSiegePlayer, "{0} is already under siege.  Join the party!", "0: defending player");
		this.addDefault(defaults, Messages.NotSiegableThere, "{0} isn't protected there.", "0: defending player");
		this.addDefault(defaults, Messages.SiegeTooFarAway, "You're too far away to siege.", null);
		this.addDefault(defaults, Messages.NoSiegeDefenseless, "That player is defenseless.  Go pick on somebody else.", null);
		this.addDefault(defaults, Messages.AlreadyUnderSiegeArea, "That area is already under siege.  Join the party!", null);
		this.addDefault(defaults, Messages.NoSiegeAdminClaim, "Siege is disabled in this area.", null);
		this.addDefault(defaults, Messages.SiegeOnCooldown, "You're still on siege cooldown for this defender or claim.  Find another victim.", null);
		this.addDefault(defaults, Messages.SiegeAlert, "You're under siege!  If you log out now, you will die.  You must defeat {0}, wait for him to give up, or escape.", "0: attacker name");
		this.addDefault(defaults, Messages.SiegeConfirmed, "The siege has begun!  If you log out now, you will die.  You must defeat {0}, chase him away, or admit defeat and walk away.", "0: defender name");
		this.addDefault(defaults, Messages.AbandonClaimMissing, "Stand in the claim you want to delete, or consider /AbandonAllClaims.", null);
		this.addDefault(defaults, Messages.NotYourClaim, "This isn't your claim.", null);
		this.addDefault(defaults, Messages.DeleteTopLevelClaim, "To delete a subdivision, stand inside it.  Otherwise, use /AbandonTopLevelClaim to delete this claim and all subdivisions.", null);		
		this.addDefault(defaults, Messages.AbandonSuccess, "Claim abandoned.  You now have {0} available claim blocks.", "0: remaining claim blocks");
		this.addDefault(defaults, Messages.CantGrantThatPermission, "You can't grant a permission you don't have yourself.", null);
		this.addDefault(defaults, Messages.GrantPermissionNoClaim, "Stand inside the claim where you want to grant permission.", null);
		this.addDefault(defaults, Messages.GrantPermissionConfirmation, "Granted {0} permission to {1} {2}.", "0: target player; 1: permission description; 2: scope (changed claims)");
		this.addDefault(defaults, Messages.ManageUniversalPermissionsInstruction, "To manage permissions for ALL your claims, stand outside them.", null);
		this.addDefault(defaults, Messages.ManageOneClaimPermissionsInstruction, "To manage permissions for a specific claim, stand inside it.", null);
		this.addDefault(defaults, Messages.CollectivePublic, "the public", "as in 'granted the public permission to...'");
		this.addDefault(defaults, Messages.BuildPermission, "build", null);
		this.addDefault(defaults, Messages.ContainersPermission, "access containers and animals", null);
		this.addDefault(defaults, Messages.AccessPermission, "use buttons and levers", null);
		this.addDefault(defaults, Messages.PermissionsPermission, "manage permissions", null);
		this.addDefault(defaults, Messages.LocationCurrentClaim, "in this claim", null);
		this.addDefault(defaults, Messages.LocationAllClaims, "in all your claims", null);
		this.addDefault(defaults, Messages.PvPImmunityStart, "You're protected from attack by other players as long as your inventory is empty.", null);
		this.addDefault(defaults, Messages.SiegeNoDrop, "You can't give away items while involved in a siege.", null);
		this.addDefault(defaults, Messages.DonateItemsInstruction, "To give away the item(s) in your hand, left-click the chest again.", null);
		this.addDefault(defaults, Messages.ChestFull, "This chest is full.", null);
		this.addDefault(defaults, Messages.DonationSuccess, "Item(s) transferred to chest!", null);
		this.addDefault(defaults, Messages.PlayerTooCloseForFire, "You can't start a fire this close to {0}.", "0: other player's name");
		this.addDefault(defaults, Messages.TooDeepToClaim, "This chest can't be protected because it's too deep underground.  Consider moving it.", null);
		this.addDefault(defaults, Messages.ChestClaimConfirmation, "This chest is protected.", null);
		this.addDefault(defaults, Messages.AutomaticClaimNotification, "This chest and nearby blocks are protected from breakage and theft.  The temporary gold and glowstone blocks mark the protected area.  To toggle them on and off, right-click with a stick.", null);
		this.addDefault(defaults, Messages.TrustCommandAdvertisement, "Use the /trust command to grant other players access.", null);
		this.addDefault(defaults, Messages.GoldenShovelAdvertisement, "To claim more land, you need a golden shovel.  When you equip one, you'll get more information.", null);
		this.addDefault(defaults, Messages.UnprotectedChestWarning, "This chest is NOT protected.  Consider using a golden shovel to expand an existing claim or to create a new one.", null);
		this.addDefault(defaults, Messages.ThatPlayerPvPImmune, "You can't injure defenseless players.", null);
		this.addDefault(defaults, Messages.CantFightWhileImmune, "You can't fight someone while you're protected from PvP.", null);
		this.addDefault(defaults, Messages.NoDamageClaimedEntity, "That belongs to {0}.", "0: owner name");
		this.addDefault(defaults, Messages.ShovelBasicClaimMode, "Shovel returned to basic claims mode.", null);
		this.addDefault(defaults, Messages.RemainingBlocks, "You may claim up to {0} more blocks.", "0: remaining blocks");
		this.addDefault(defaults, Messages.CreativeBasicsDemoAdvertisement, "Land Claim Help:  http://tinyurl.com/c7bajb8", null);
		this.addDefault(defaults, Messages.SurvivalBasicsDemoAdvertisement, "Land Claim Help:  http://tinyurl.com/6nkwegj", null);
		this.addDefault(defaults, Messages.TrappedChatKeyword, "trapped", "When mentioned in chat, players get information about the /trapped command.");
		this.addDefault(defaults, Messages.TrappedInstructions, "Are you trapped in someone's land claim?  Try the /trapped command.", null);
		this.addDefault(defaults, Messages.PvPNoDrop, "You can't drop items while in PvP combat.", null);
		this.addDefault(defaults, Messages.SiegeNoTeleport, "You can't teleport out of a besieged area.", null);
		this.addDefault(defaults, Messages.BesiegedNoTeleport, "You can't teleport into a besieged area.", null);
		this.addDefault(defaults, Messages.SiegeNoContainers, "You can't access containers while involved in a siege.", null);
		this.addDefault(defaults, Messages.PvPNoContainers, "You can't access containers during PvP combat.", null);
		this.addDefault(defaults, Messages.PvPImmunityEnd, "Now you can fight with other players.", null);
		this.addDefault(defaults, Messages.NoBedPermission, "{0} hasn't given you permission to sleep here.", "0: claim owner");
		this.addDefault(defaults, Messages.NoWildernessBuckets, "You may only dump buckets inside your claim(s) or underground.", null);
		this.addDefault(defaults, Messages.NoLavaNearOtherPlayer, "You can't place lava this close to {0}.", "0: nearby player");
		this.addDefault(defaults, Messages.TooFarAway, "That's too far away.", null);
		this.addDefault(defaults, Messages.BlockNotClaimed, "No one has claimed this block.", null);
		this.addDefault(defaults, Messages.BlockClaimed, "That block has been claimed by {0}.", "0: claim owner");
		this.addDefault(defaults, Messages.SiegeNoShovel, "You can't use your shovel tool while involved in a siege.", null);
		this.addDefault(defaults, Messages.RestoreNaturePlayerInChunk, "Unable to restore.  {0} is in that chunk.", "0: nearby player");
		this.addDefault(defaults, Messages.NoCreateClaimPermission, "You don't have permission to claim land.", null);
		this.addDefault(defaults, Messages.ResizeClaimTooSmall, "This new size would be too small.  Claims must be at least {0} x {0}.", "0: minimum claim size");
		this.addDefault(defaults, Messages.ResizeNeedMoreBlocks, "You don't have enough blocks for this size.  You need {0} more.", "0: how many needed");
		this.addDefault(defaults, Messages.ClaimResizeSuccess, "Claim resized.  You now have {0} available claim blocks.", "0: remaining blocks");
		this.addDefault(defaults, Messages.ResizeFailOverlap, "Can't resize here because it would overlap another nearby claim.", null);
		this.addDefault(defaults, Messages.ResizeStart, "Resizing claim.  Use your shovel again at the new location for this corner.", null);
		this.addDefault(defaults, Messages.ResizeFailOverlapSubdivision, "You can't create a subdivision here because it would overlap another subdivision.  Consider /abandonclaim to delete it, or use your shovel at a corner to resize it.", null);
		this.addDefault(defaults, Messages.SubdivisionStart, "Subdivision corner set!  Use your shovel at the location for the opposite corner of this new subdivision.", null);
		this.addDefault(defaults, Messages.CreateSubdivisionOverlap, "Your selected area overlaps another subdivision.", null);
		this.addDefault(defaults, Messages.SubdivisionSuccess, "Subdivision created!  Use /trust to share it with friends.", null);
		this.addDefault(defaults, Messages.CreateClaimFailOverlap, "You can't create a claim here because it would overlap your other claim.  Use /abandonclaim to delete it, or use your shovel at a corner to resize it.", null);
		this.addDefault(defaults, Messages.CreateClaimFailOverlapOtherPlayer, "You can't create a claim here because it would overlap {0}'s claim.", "0: other claim owner");
		this.addDefault(defaults, Messages.ClaimsDisabledWorld, "Land claims are disabled in this world.", null);
		this.addDefault(defaults, Messages.ClaimStart, "Claim corner set!  Use the shovel again at the opposite corner to claim a rectangle of land.  To cancel, put your shovel away.", null);
		this.addDefault(defaults, Messages.NewClaimTooSmall, "This claim would be too small.  Any claim must be at least {0} x {0}.", "0: minimum claim size");
		this.addDefault(defaults, Messages.CreateClaimInsufficientBlocks, "You don't have enough blocks to claim that entire area.  You need {0} more blocks.", "0: additional blocks needed");
		this.addDefault(defaults, Messages.AbandonClaimAdvertisement, "To delete another claim and free up some blocks, use /AbandonClaim.", null);
		this.addDefault(defaults, Messages.CreateClaimFailOverlapShort, "Your selected area overlaps an existing claim.", null);
		this.addDefault(defaults, Messages.CreateClaimSuccess, "Claim created!  Use /trust to share it with friends.", null);
		this.addDefault(defaults, Messages.SiegeWinDoorsOpen, "Congratulations!  Buttons and levers are temporarily unlocked (five minutes).", null);
		this.addDefault(defaults, Messages.RescueAbortedMoved, "You moved!  Rescue cancelled.", null);
		this.addDefault(defaults, Messages.SiegeDoorsLockedEjection, "Looting time is up!  Ejected from the claim.", null);
		this.addDefault(defaults, Messages.NoModifyDuringSiege, "Claims can't be modified while under siege.", null);
		this.addDefault(defaults, Messages.OnlyOwnersModifyClaims, "Only {0} can modify this claim.", "0: owner name");
		this.addDefault(defaults, Messages.NoBuildUnderSiege, "This claim is under siege by {0}.  No one can build here.", "0: attacker name");
		this.addDefault(defaults, Messages.NoBuildPvP, "You can't build in claims during PvP combat.", null);
		this.addDefault(defaults, Messages.NoBuildPermission, "You don't have {0}'s permission to build here.", "0: owner name");
		this.addDefault(defaults, Messages.NonSiegeMaterial, "That material is too tough to break.", null);
		this.addDefault(defaults, Messages.NoOwnerBuildUnderSiege, "You can't make changes while under siege.", null);
		this.addDefault(defaults, Messages.NoAccessPermission, "You don't have {0}'s permission to use that.", "0: owner name.  access permission controls buttons, levers, and beds");
		this.addDefault(defaults, Messages.NoContainersSiege, "This claim is under siege by {0}.  No one can access containers here right now.", "0: attacker name");
		this.addDefault(defaults, Messages.NoContainersPermission, "You don't have {0}'s permission to use that.", "0: owner's name.  containers also include crafting blocks");
		this.addDefault(defaults, Messages.OwnerNameForAdminClaims, "an administrator", "as in 'You don't have an administrator's permission to build here.'");
		this.addDefault(defaults, Messages.ClaimTooSmallForEntities, "This claim isn't big enough for that.  Try enlarging it.", null);
		this.addDefault(defaults, Messages.TooManyEntitiesInClaim, "This claim has too many entities already.  Try enlarging the claim or removing some animals, monsters, paintings, or minecarts.", null);
		this.addDefault(defaults, Messages.YouHaveNoClaims, "You don't have any land claims.", null);
		this.addDefault(defaults, Messages.ConfirmFluidRemoval, "Abandoning this claim will remove all your lava and water.  If you're sure, use /AbandonClaim again.", null);
		this.addDefault(defaults, Messages.AutoBanNotify, "Auto-banned {0}({1}).  See logs for details.", null);
		this.addDefault(defaults, Messages.AdjustGroupBlocksSuccess, "Adjusted bonus claim blocks for players with the {0} permission by {1}.  New total: {2}.", "0: permission; 1: adjustment amount; 2: new total bonus");
		this.addDefault(defaults, Messages.InvalidPermissionID, "Please specify a player name, or a permission in [brackets].", null);
		this.addDefault(defaults, Messages.UntrustOwnerOnly, "Only {0} can revoke permissions here.", "0: claim owner's name");
		this.addDefault(defaults, Messages.HowToClaimRegex, "(^|.*\\W)how\\W.*\\W(claim|protect|lock)(\\W.*|$)", "This is a Java Regular Expression.  Look it up before editing!  It's used to tell players about the demo video when they ask how to claim land.");
		this.addDefault(defaults, Messages.NoBuildOutsideClaims, "You can't build here unless you claim some land first.", null);
		this.addDefault(defaults, Messages.PlayerOfflineTime, "  Last login: {0} days ago.", "0: number of full days since last login");
		this.addDefault(defaults, Messages.BuildingOutsideClaims, "Other players can undo your work here!  Consider using a golden shovel to claim this area so that your work will be protected.", null);
		this.addDefault(defaults, Messages.TrappedWontWorkHere, "Sorry, unable to find a safe location to teleport you to.  Contact an admin, or consider /kill if you don't want to wait.", null);
		this.addDefault(defaults, Messages.CommandBannedInPvP, "You can't use that command while in PvP combat.", null);
		this.addDefault(defaults, Messages.UnclaimCleanupWarning, "The land you've unclaimed may be changed by other players or cleaned up by administrators.  If you've built something there you want to keep, you should reclaim it.", null);
		this.addDefault(defaults, Messages.BuySellNotConfigured, "Sorry, buying anhd selling claim blocks is disabled.", null);
		this.addDefault(defaults, Messages.NoTeleportPvPCombat, "You can't teleport while fighting another player.", null);
		this.addDefault(defaults, Messages.NoTNTDamageAboveSeaLevel, "Warning: TNT will not destroy blocks above sea level.", null);
		this.addDefault(defaults, Messages.NoTNTDamageClaims, "Warning: TNT will not destroy claimed blocks.", null);
		this.addDefault(defaults, Messages.IgnoreClaimsAdvertisement, "To override, use /IgnoreClaims.", null);		
		this.addDefault(defaults, Messages.NoPermissionForCommand, "You don't have permission to do that.", null);
		this.addDefault(defaults, Messages.ClaimsListNoPermission, "You don't have permission to get information about another player's land claims.", null);
		this.addDefault(defaults, Messages.ExplosivesDisabled, "This claim is now protected from explosions.  Use /ClaimExplosions again to disable.", null);
		this.addDefault(defaults, Messages.ExplosivesEnabled, "This claim is now vulnerable to explosions.  Use /ClaimExplosions again to re-enable protections.", null);
		this.addDefault(defaults, Messages.ClaimExplosivesAdvertisement, "To allow explosives to destroy blocks in this land claim, use /ClaimExplosions.", null);
		this.addDefault(defaults, Messages.PlayerInPvPSafeZone, "That player is in a PvP safe zone.", null);		
		
		//load the config file
		FileConfiguration config = YamlConfiguration.loadConfiguration(new File(messagesFilePath));
		
		//for each message ID
		for(int i = 0; i < messageIDs.length; i++)
		{
			//get default for this message
			Messages messageID = messageIDs[i];
			CustomizableMessage messageData = defaults.get(messageID.name());
			
			//if default is missing, log an error and use some fake data for now so that the plugin can run
			if(messageData == null)
			{
				GriefPrevention.AddLogEntry("Missing message for " + messageID.name() + ".  Please contact the developer.");
				messageData = new CustomizableMessage(messageID, "Missing message!  ID: " + messageID.name() + ".  Please contact a server admin.", null);
			}
			
			//read the message from the file, use default if necessary
			this.messages[messageID.ordinal()] = config.getString("Messages." + messageID.name() + ".Text", messageData.text);
			config.set("Messages." + messageID.name() + ".Text", this.messages[messageID.ordinal()]);
			
			if(messageData.notes != null)
			{
				messageData.notes = config.getString("Messages." + messageID.name() + ".Notes", messageData.notes);
				config.set("Messages." + messageID.name() + ".Notes", messageData.notes);
			}
		}
		
		//save any changes
		try
		{
			config.save(DataStore.messagesFilePath);
		}
		catch(IOException exception)
		{
			GriefPrevention.AddLogEntry("Unable to write to the configuration file at \"" + DataStore.messagesFilePath + "\"");
		}
		
		defaults.clear();
		System.gc();				
	}

	private void addDefault(HashMap<String, CustomizableMessage> defaults,
			Messages id, String text, String notes)
	{
		CustomizableMessage message = new CustomizableMessage(id, text, notes);
		defaults.put(id.name(), message);		
	}

	synchronized public String getMessage(Messages messageID, String... args)
	{
		String message = messages[messageID.ordinal()];
		
		for(int i = 0; i < args.length; i++)
		{
			String param = args[i];
			message = message.replace("{" + i + "}", param);
		}
		
		return message;		
	}
	
	abstract void close();	
}
