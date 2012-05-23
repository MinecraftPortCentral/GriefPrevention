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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

//singleton class which manages all GriefPrevention data (except for config options)
public class DataStore 
{
	//in-memory cache for player data
	private HashMap<String, PlayerData> playerNameToPlayerDataMap = new HashMap<String, PlayerData>();
	
	//in-memory cache for claim data
	private ArrayList<Claim> claims = new ArrayList<Claim>();
	
	//path information, for where stuff stored on disk is well...  stored
	private final static String dataLayerFolderPath = "plugins" + File.separator + "GriefPreventionData";
	private final static String playerDataFolderPath = dataLayerFolderPath + File.separator + "PlayerData";
	private final static String claimDataFolderPath = dataLayerFolderPath + File.separator + "ClaimData";
	final static String configFilePath = dataLayerFolderPath + File.separator + "config.yml";
	
	//initialization!
	DataStore()
	{
		//ensure data folders exist
		new File(playerDataFolderPath).mkdirs();
		new File(claimDataFolderPath).mkdirs();
		
		//load claims data into memory		
		File claimDataFolder = new File(claimDataFolderPath);
		File [] files = claimDataFolder.listFiles();
		
		int loadedClaimCount = 0;
		
		for(int i = 0; i < files.length; i++)
		{			
			if(files[i].isFile())  //avoids folders
			{
				BufferedReader inStream = null;
				try
				{					
					Claim topLevelClaim = null;
					
					inStream = new BufferedReader(new FileReader(files[i].getAbsolutePath()));
					String line = inStream.readLine();
					
					while(line != null)
					{					
						//first line is lesser boundary corner location
						Location lesserBoundaryCorner = this.locationFromString(line);
						
						//second line is greater boundary corner location
						line = inStream.readLine();
						Location greaterBoundaryCorner = this.locationFromString(line);
						
						//third line is owner name
						line = inStream.readLine();						
						String ownerName = line;
						
						//fourth line is list of builders
						line = inStream.readLine();
						String [] builderNames = line.split(";");
						
						//fifth line is list of players who can access containers
						line = inStream.readLine();
						String [] containerNames = line.split(";");
						
						//sixth line is list of players who can use buttons and switches
						line = inStream.readLine();
						String [] accessorNames = line.split(";");
						
						//seventh line is list of players who can grant permissions
						line = inStream.readLine();
						if(line == null) line = "";
						String [] managerNames = line.split(";");
						
						//skip any remaining extra lines, until the "===" string, indicating the end of this claim or subdivision
						line = inStream.readLine();
						while(line != null && !line.contains("=========="))
							line = inStream.readLine();
						
						//build a claim instance from those data
						//if this is the first claim loaded from this file, it's the top level claim
						if(topLevelClaim == null)
						{
							//instantiate
							topLevelClaim = new Claim(lesserBoundaryCorner, greaterBoundaryCorner, ownerName, builderNames, containerNames, accessorNames, managerNames);
							
							//search for another claim overlapping this one
							Claim conflictClaim = this.getClaimAt(topLevelClaim.lesserBoundaryCorner, true, null);
							
							//if there is such a claim, delete this file and move on to the next
							if(conflictClaim != null)
							{
								inStream.close();
								files[i].delete();
								line = null;
								continue;
							}
							
							//otherwise, add this claim to the claims collection
							else
							{
								topLevelClaim.modifiedDate = new Date(files[i].lastModified());
								int j = 0;
								while(j < this.claims.size() && !this.claims.get(j).greaterThan(topLevelClaim)) j++;
								if(j < this.claims.size())
									this.claims.add(j, topLevelClaim);
								else
									this.claims.add(this.claims.size(), topLevelClaim);
								topLevelClaim.inDataStore = true;								
							}
						}
						
						//otherwise there's already a top level claim, so this must be a subdivision of that top level claim
						else
						{
							Claim subdivision = new Claim(lesserBoundaryCorner, greaterBoundaryCorner, "--subdivision--", builderNames, containerNames, accessorNames, managerNames);
							
							//make sure there are no other subdivisions overlapping this one
							
							subdivision.modifiedDate = new Date(files[i].lastModified());
							subdivision.parent = topLevelClaim;
							topLevelClaim.children.add(subdivision);
							subdivision.inDataStore = true;
						}
						
						//move up to the first line in the next subdivision
						line = inStream.readLine();
					}
					
					inStream.close();
					  
					loadedClaimCount++;
				}
				
				//if there's any problem with the file's content, log an error message and skip it
				catch(Exception e)
				{
					 GriefPrevention.AddLogEntry("Unable to load data for claim \"" + files[i].getName() + "\": " + e.getMessage());
				}
				
				try
				{
					if(inStream != null) inStream.close();					
				}
				catch(IOException exception) {}
			}
		}
		
		GriefPrevention.AddLogEntry(loadedClaimCount + " total claims loaded.");
		
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
		
		//load each of these players and determine whether his claims should be cleaned up
		for(int i = 0; i < playerNames.size(); i++)
		{
			String playerName = playerNames.get(i);
			
			PlayerData playerData = this.getPlayerData(playerName);
			
			int areaOfDefaultClaim = 0;
			
			//determine area of the default chest claim
			if(GriefPrevention.instance.config_claims_automaticClaimsForNewPlayersRadius >= 0)
			{
				areaOfDefaultClaim = (int)Math.pow(GriefPrevention.instance.config_claims_automaticClaimsForNewPlayersRadius * 2 + 1, 2);  
			}
			
			//figure out how long the player has been away
			Calendar sevenDaysAgo = Calendar.getInstance();
			sevenDaysAgo.add(Calendar.DATE, -7);
			boolean claimsExpired = sevenDaysAgo.getTime().after(playerData.lastLogin);
			
			//if only one claim, and the player hasn't played in a week
			if(claimsExpired && playerData.claims.size() == 1)
			{
				Claim claim = playerData.claims.get(0);
				
				//if that's a chest claim, delete it
				if(claim.getArea() <= areaOfDefaultClaim)
				{
					this.deleteClaim(claim);
					GriefPrevention.AddLogEntry(" " + playerName + "'s new player claim expired.");
				}
			}
			
			//toss that player data out of the cache, it's not needed in memory right now
			this.clearCachedPlayerData(playerName);
		}
		
		//collect garbage, since lots of stuff was loaded into memory and then tossed out
		System.gc();
	}
	
	//removes cached player data from memory
	void clearCachedPlayerData(String playerName)
	{
		this.playerNameToPlayerDataMap.remove(playerName);
	}
	
	public void changeClaimOwner(Claim claim, String newOwnerName) throws Exception
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
	void addClaim(Claim newClaim)
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
	
	//turns a location into a string, useful in data files and data file names
	private String locationStringDelimiter = ";";	
	private String locationToString(Location location)
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
	private Location locationFromString(String string) throws Exception
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

	//does the work of actually writing a claim to file
	public void saveClaim(Claim claim)
	{
		//subdivisions don't save to their own files, but instead live in their parent claim's file
		//so any attempt to save a subdivision will save its parent (and thus the subdivision)
		if(claim.parent != null)
		{
			this.saveClaim(claim.parent);
			return;
		}
		
		//otherwise get a unique identifier for the claim which will be used to name the file on disk
		String claimID = this.getClaimID(claim);
		
		BufferedWriter outStream = null;
		
		try
		{
			//open the claim's file						
			File claimFile = new File(claimDataFolderPath + File.separator + claimID);
			claimFile.createNewFile();
			outStream = new BufferedWriter(new FileWriter(claimFile));
			
			this.writeClaimData(claim, outStream);
			
			for(int i = 0; i < claim.children.size(); i++)
			{
				//see below for details of writing data to file
				this.writeClaimData(claim.children.get(i), outStream);
			}
		}		
		
		//if any problem, log it
		catch(Exception e)
		{
			GriefPrevention.AddLogEntry("PopulationDensity: Unexpected exception saving data for claim \"" + claimID + "\": " + e.getMessage());
		}
		
		//close the file
		try
		{
			if(outStream != null) outStream.close();
		}
		catch(IOException exception) {}
	}
	
	//actually writes claim data to an output stream
	private void writeClaimData(Claim claim, BufferedWriter outStream) throws IOException
	{
		String claimID = this.getClaimID(claim);
		
		//first line is lesser boundary corner location
		outStream.write(claimID);
		outStream.newLine();
		
		//second line is greater boundary corner location
		outStream.write(this.locationToString(claim.getGreaterBoundaryCorner()));
		outStream.newLine();
		
		//third line is owner name
		outStream.write(claim.ownerName);
		outStream.newLine();
		
		ArrayList<String> builders = new ArrayList<String>();
		ArrayList<String> containers = new ArrayList<String>();
		ArrayList<String> accessors = new ArrayList<String>();
		ArrayList<String> managers = new ArrayList<String>();
		
		claim.getPermissions(builders, containers, accessors, managers);
		
		//fourth line is list of players with build permission
		for(int i = 0; i < builders.size(); i++)
		{
			outStream.write(builders.get(i) + ";");
		}
		outStream.newLine();
		
		//fifth line is list of players with container permission
		for(int i = 0; i < containers.size(); i++)
		{
			outStream.write(containers.get(i) + ";");
		}
		outStream.newLine();
		
		//sixth line is list of players with access permission
		for(int i = 0; i < accessors.size(); i++)
		{
			outStream.write(accessors.get(i) + ";");
		}
		outStream.newLine();
		
		//seventh line is list of players who may grant permissions for others
		for(int i = 0; i < managers.size(); i++)
		{
			outStream.write(managers.get(i) + ";");
		}
		outStream.newLine();
		
		//cap each claim with "=========="
		outStream.write("==========");
		outStream.newLine();
	}
	
	//retrieves player data from memory or file, as necessary
	//if the player has never been on the server before, this will return a fresh player data with default values
	public PlayerData getPlayerData(String playerName)
	{
		//first, look in memory
		PlayerData playerData = this.playerNameToPlayerDataMap.get(playerName);
		
		//if not there, look on disk
		if(playerData == null)
		{
			File playerFile = new File(playerDataFolderPath + File.separator + playerName);
					
			playerData = new PlayerData();
			
			//if it doesn't exist as a file
			if(!playerFile.exists())
			{
				//create a file with defaults
				this.savePlayerData(playerName, playerData);
			}
			
			//otherwise, read the file
			else
			{			
				BufferedReader inStream = null;
				try
				{					
					inStream = new BufferedReader(new FileReader(playerFile.getAbsolutePath()));
					
					//first line is last login timestamp
					String lastLoginTimestampString = inStream.readLine();
					
					//convert that to a date and store it
				    DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");					
					try
					{
						playerData.lastLogin = dateFormat.parse(lastLoginTimestampString);
					}
					catch(ParseException parseException)
					{
						GriefPrevention.AddLogEntry("Unable to load last login for \"" + playerFile.getName() + "\".");
						playerData.lastLogin = null;
					}
					
					//second line is accrued claim blocks
					String accruedBlocksString = inStream.readLine();
					
					//convert that to a number and store it
					playerData.accruedClaimBlocks = Integer.parseInt(accruedBlocksString);
					
					//third line is any bonus claim blocks granted by administrators
					String bonusBlocksString = inStream.readLine();					
					
					//convert that to a number and store it										
					playerData.bonusClaimBlocks = Integer.parseInt(bonusBlocksString);
					
					//fourth line is a double-semicolon-delimited list of claims, which is currently ignored
					//String claimsString = inStream.readLine();
					inStream.readLine();
					/*
					if(claimsString != null && claimsString.length() > 0)
					{
						String [] claimsStrings = claimsString.split(";;");
						boolean missingClaim = false;
						
						//search for each claim mentioned in the file
						for(int i = 0; i < claimsStrings.length; i++)
						{
							String claimID = claimsStrings[i];
							if(claimID != null)
							{
								Claim claim = this.getClaimAt(this.locationFromString(claimID), true, null);
								
								//if the referenced claim exists, add it to the player data instance for later reference
								if(claim != null)
								{
									playerData.claims.add(claim);
								}
								
								//if the claim doesn't seem to exist anymore, plan to drop the reference from the file
								else
								{
									missingClaim = true;
								}
							}
						}
						
						//if any referenced claims no longer exist, write the player data back to file to eliminate those references
						if(missingClaim)
						{
							this.savePlayerData(playerName, playerData);
						}
					}
					*/
					
					//find all the claims belonging to this player and note them for future reference
					for(int i = 0; i < this.claims.size(); i++)
					{
						Claim claim = this.claims.get(i);
						if(claim.ownerName.equals(playerName))
						{
							playerData.claims.add(claim);
						}
					}
					
					inStream.close();
				}
					
				//if there's any problem with the file's content, log an error message
				catch(Exception e)
				{
					 GriefPrevention.AddLogEntry("Unable to load data for player \"" + playerName + "\": " + e.getMessage());			 
				}
				
				try
				{
					if(inStream != null) inStream.close();
				}
				catch(IOException exception) {}
			}
			
			//shove that new player data into the hash map cache
			this.playerNameToPlayerDataMap.put(playerName, playerData);
		}
		
		//try the hash map again.  if it's STILL not there, we have a bug to fix
		return this.playerNameToPlayerDataMap.get(playerName);
	}
	
	//deletes a claim or subdivision
	public void deleteClaim(Claim claim)
	{
		//subdivisions are simple - just remove them from their parent claim and save that claim
		if(claim.parent != null)
		{
			Claim parentClaim = claim.parent;
			parentClaim.children.remove(claim);
			this.saveClaim(parentClaim);
			return;
		}
		
		//otherwise, need to update the data store and ensure the claim's file is deleted		
		String claimID = this.getClaimID(claim);
		
		//remove from memory
		for(int i = 0; i < this.claims.size(); i++)
		{
			if(this.getClaimID(this.claims.get(i)).equals(claimID))
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
		
		//remove from disk
		File claimFile = new File(claimDataFolderPath + File.separator + claimID);
		if(claimFile.exists() && !claimFile.delete())
		{
			GriefPrevention.AddLogEntry("Error: Unable to delete claim file \"" + claimFile.getAbsolutePath() + "\".");
		}
		
		//update player data, except for administrative claims, which have no owner
		if(!claim.isAdminClaim())
		{
			PlayerData ownerData = this.getPlayerData(claim.getOwnerName());
			for(int i = 0; i < ownerData.claims.size(); i++)
			{
				if(this.getClaimID(ownerData.claims.get(i)).equals(claimID))
				{
					ownerData.claims.remove(i);
					break;
				}
			}
			this.savePlayerData(claim.getOwnerName(), ownerData);
		}
	}
	
	//gets the claim at a specific location
	//ignoreHeight = TRUE means that a location UNDER an existing claim will return the claim
	//cachedClaim can be NULL, but will help performance if you have a reasonable guess about which claim the location is in
	public Claim getClaimAt(Location location, boolean ignoreHeight, Claim cachedClaim)
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
	public CreateClaimResult createClaim(World world, int x1, int x2, int y1, int y2, int z1, int z2, String ownerName, Claim parent)
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
		
		//create a new claim instance (but don't save it, yet)
		Claim newClaim = new Claim(
			new Location(world, smallx, smally, smallz),
			new Location(world, bigx, bigy, bigz),
			ownerName,
			new String [] {}, 
			new String [] {},
			new String [] {},
			new String [] {});
		
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
	
	//saves changes to player data.  MUST be called after you're done making changes, otherwise a reload will lose them
	public void savePlayerData(String playerName, PlayerData playerData)
	{
		//never save data for the "administrative" account.  an empty string for claim owner indicates an administrative claim
		if(playerName.length() == 0) return;
		
		BufferedWriter outStream = null;
		try
		{
			//open the player's file
			File playerDataFile = new File(playerDataFolderPath + File.separator + playerName);
			playerDataFile.createNewFile();
			outStream = new BufferedWriter(new FileWriter(playerDataFile));
			
			//first line is last login timestamp
			if(playerData.lastLogin == null)playerData.lastLogin = new Date();
			DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
			outStream.write(dateFormat.format(playerData.lastLogin));
			outStream.newLine();
			
			//second line is accrued claim blocks
			outStream.write(String.valueOf(playerData.accruedClaimBlocks));
			outStream.newLine();			
			
			//third line is bonus claim blocks
			outStream.write(String.valueOf(playerData.bonusClaimBlocks));
			outStream.newLine();						
			
			//fourth line is a double-semicolon-delimited list of claims
			if(playerData.claims.size() > 0)
			{
				outStream.write(this.locationToString(playerData.claims.get(0).getLesserBoundaryCorner()));
				for(int i = 1; i < playerData.claims.size(); i++)
				{
					outStream.write(";;" + this.locationToString(playerData.claims.get(i).getLesserBoundaryCorner()));
				}
			}
			outStream.newLine();
		}		
		
		//if any problem, log it
		catch(Exception e)
		{
			GriefPrevention.AddLogEntry("PopulationDensity: Unexpected exception saving data for player \"" + playerName + "\": " + e.getMessage());
		}
		
		try
		{
			//close the file
			if(outStream != null)
			{
				outStream.close();
			}
		}
		catch(IOException exception){}
	}
	
	//gets a unique identifier for a claim
	private String getClaimID(Claim claim)
	{
		return this.locationToString(claim.getLesserBoundaryCorner());
	}

	//extends a claim to a new depth
	//respects the max depth config variable
	public void extendClaim(Claim claim, int newDepth) 
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
	public void startSiege(Player attacker, Player defender, Claim defenderClaim)
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
	public void endSiege(SiegeData siegeData, String winnerName, String loserName, boolean death)
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
				GriefPrevention.sendMessage(winner, TextMode.Success, "Congratulations!  Buttons and levers are temporarily unlocked (five minutes).");
				
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
	public boolean onCooldown(Player attacker, Player defender, Claim defenderClaim)
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
	void tryExtendSiege(Player player, Claim claim)
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
	public void deleteClaimsForPlayer(String playerName, boolean deleteCreativeClaims)
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
			this.deleteClaim(claimsToDelete.get(i));
		}					
	}

	//tries to resize a claim
	//see CreateClaim() for details on return value
	public CreateClaimResult resizeClaim(Claim claim, int newx1, int newx2, int newy1, int newy2, int newz1, int newz2)
	{
		//remove old claim
		this.deleteClaim(claim);					
		
		//try to create this new claim, ignoring the original when checking for overlap
		CreateClaimResult result = this.createClaim(claim.getLesserBoundaryCorner().getWorld(), newx1, newx2, newy1, newy2, newz1, newz2, claim.ownerName, claim.parent);
		
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
}
