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

//manages data stored in the file system
public class FlatFileDataStore extends DataStore
{
	private final static String playerDataFolderPath = dataLayerFolderPath + File.separator + "PlayerData";
	private final static String claimDataFolderPath = dataLayerFolderPath + File.separator + "ClaimData";
	private final static String nextClaimIdFilePath = claimDataFolderPath + File.separator + "_nextClaimID";

	static boolean hasData()
	{
		File playerDataFolder = new File(playerDataFolderPath);
		File claimsDataFolder = new File(claimDataFolderPath);
		
		return playerDataFolder.exists() || claimsDataFolder.exists();
	}
	
	//initialization!
	FlatFileDataStore() throws Exception
	{
		this.initialize();
	}
	
	@Override
	void initialize() throws Exception
	{
		//ensure data folders exist
		new File(playerDataFolderPath).mkdirs();
		new File(claimDataFolderPath).mkdirs();
		
		//load group data into memory
		File playerDataFolder = new File(playerDataFolderPath);
		File [] files = playerDataFolder.listFiles();
		for(int i = 0; i < files.length; i++)
		{
			File file = files[i];
			if(!file.isFile()) continue;  //avoids folders
			
			//all group data files start with a dollar sign.  ignoring the rest, which are player data files.			
			if(!file.getName().startsWith("$")) continue;
			
			String groupName = file.getName().substring(1);
			if(groupName == null || groupName.isEmpty()) continue;  //defensive coding, avoid unlikely cases
			
			BufferedReader inStream = null;
			try
			{
				inStream = new BufferedReader(new FileReader(file.getAbsolutePath()));
				String line = inStream.readLine();
				
				int groupBonusBlocks = Integer.parseInt(line);
				
				this.permissionToBonusBlocksMap.put(groupName, groupBonusBlocks);
			}
			catch(Exception e)
			{
				 GriefPrevention.AddLogEntry("Unable to load group bonus block data from file \"" + file.getName() + "\": " + e.getMessage());
			}
			
			try
			{
				if(inStream != null) inStream.close();					
			}
			catch(IOException exception) {}
		}
		
		//load next claim number from file
		File nextClaimIdFile = new File(nextClaimIdFilePath);
		if(nextClaimIdFile.exists())
		{
			BufferedReader inStream = null;
			try
			{
				inStream = new BufferedReader(new FileReader(nextClaimIdFile.getAbsolutePath()));
				
				//read the id
				String line = inStream.readLine();
				
				//try to parse into a long value
				this.nextClaimID = Long.parseLong(line);
			}
			catch(Exception e){ }
			
			try
			{
				if(inStream != null) inStream.close();					
			}
			catch(IOException exception) {}
		}
		
		//load claims data into memory		
		//get a list of all the files in the claims data folder
		File claimDataFolder = new File(claimDataFolderPath);
		files = claimDataFolder.listFiles();
		
		for(int i = 0; i < files.length; i++)
		{			
			if(files[i].isFile())  //avoids folders
			{
				//skip any file starting with an underscore, to avoid the _nextClaimID file.
				if(files[i].getName().startsWith("_")) continue;
				
				//the filename is the claim ID.  try to parse it
				long claimID;
				
				try
				{
					claimID = Long.parseLong(files[i].getName());
				}
				
				//because some older versions used a different file name pattern before claim IDs were introduced,
				//those files need to be "converted" by renaming them to a unique ID
				catch(Exception e)
				{
					claimID = this.nextClaimID;
					this.incrementNextClaimID();
					File newFile = new File(claimDataFolderPath + File.separator + String.valueOf(this.nextClaimID));
					files[i].renameTo(newFile);
					files[i] = newFile;
				}
				
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
							topLevelClaim = new Claim(lesserBoundaryCorner, greaterBoundaryCorner, ownerName, builderNames, containerNames, accessorNames, managerNames, claimID);
							
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
							Claim subdivision = new Claim(lesserBoundaryCorner, greaterBoundaryCorner, "--subdivision--", builderNames, containerNames, accessorNames, managerNames, null);
							
							subdivision.modifiedDate = new Date(files[i].lastModified());
							subdivision.parent = topLevelClaim;
							topLevelClaim.children.add(subdivision);
							subdivision.inDataStore = true;
						}
						
						//move up to the first line in the next subdivision
						line = inStream.readLine();
					}
					
					inStream.close();
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
		
		super.initialize();
	}
	
	@Override
	synchronized void writeClaimToStorage(Claim claim)
	{
		String claimID = String.valueOf(claim.id);
		
		BufferedWriter outStream = null;
		
		try
		{
			//open the claim's file						
			File claimFile = new File(claimDataFolderPath + File.separator + claimID);
			claimFile.createNewFile();
			outStream = new BufferedWriter(new FileWriter(claimFile));
			
			//write top level claim data to the file
			this.writeClaimData(claim, outStream);
			
			//for each subdivision
			for(int i = 0; i < claim.children.size(); i++)
			{
				//write the subdivision's data to the file
				this.writeClaimData(claim.children.get(i), outStream);
			}
		}		
		
		//if any problem, log it
		catch(Exception e)
		{
			GriefPrevention.AddLogEntry("Unexpected exception saving data for claim \"" + claimID + "\": " + e.getMessage());
		}
		
		//close the file
		try
		{
			if(outStream != null) outStream.close();
		}
		catch(IOException exception) {}
	}
	
	//actually writes claim data to an output stream
	synchronized private void writeClaimData(Claim claim, BufferedWriter outStream) throws IOException
	{
		//first line is lesser boundary corner location
		outStream.write(this.locationToString(claim.getLesserBoundaryCorner()));
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
	
	//deletes a top level claim from the file system
	@Override
	synchronized void deleteClaimFromSecondaryStorage(Claim claim)
	{
		String claimID = String.valueOf(claim.id);
		
		//remove from disk
		File claimFile = new File(claimDataFolderPath + File.separator + claimID);
		if(claimFile.exists() && !claimFile.delete())
		{
			GriefPrevention.AddLogEntry("Error: Unable to delete claim file \"" + claimFile.getAbsolutePath() + "\".");
		}		
	}
	
	@Override
	synchronized PlayerData getPlayerDataFromStorage(String playerName)
	{
		File playerFile = new File(playerDataFolderPath + File.separator + playerName);
					
		PlayerData playerData = new PlayerData();
		playerData.playerName = playerName;
		
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
			
		return playerData;
	}
	
	//saves changes to player data.  MUST be called after you're done making changes, otherwise a reload will lose them
	@Override
	synchronized public void savePlayerData(String playerName, PlayerData playerData)
	{
		//never save data for the "administrative" account.  an empty string for claim owner indicates administrative account
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
			GriefPrevention.AddLogEntry("GriefPrevention: Unexpected exception saving data for player \"" + playerName + "\": " + e.getMessage());
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
	
	@Override
	synchronized void incrementNextClaimID()
	{
		//increment in memory
		this.nextClaimID++;
		
		BufferedWriter outStream = null;
		
		try
		{
			//open the file and write the new value
			File nextClaimIdFile = new File(nextClaimIdFilePath);
			nextClaimIdFile.createNewFile();
			outStream = new BufferedWriter(new FileWriter(nextClaimIdFile));
			
			outStream.write(String.valueOf(this.nextClaimID));
		}		
		
		//if any problem, log it
		catch(Exception e)
		{
			GriefPrevention.AddLogEntry("Unexpected exception saving next claim ID: " + e.getMessage());
		}
		
		//close the file
		try
		{
			if(outStream != null) outStream.close();
		}
		catch(IOException exception) {} 
	}
	
	//grants a group (players with a specific permission) bonus claim blocks as long as they're still members of the group
	@Override
	synchronized void saveGroupBonusBlocks(String groupName, int currentValue)
	{
		//write changes to file to ensure they don't get lost
		BufferedWriter outStream = null;
		try
		{
			//open the group's file
			File groupDataFile = new File(playerDataFolderPath + File.separator + "$" + groupName);
			groupDataFile.createNewFile();
			outStream = new BufferedWriter(new FileWriter(groupDataFile));
			
			//first line is number of bonus blocks
			outStream.write(String.valueOf(currentValue));
			outStream.newLine();			
		}		
		
		//if any problem, log it
		catch(Exception e)
		{
			GriefPrevention.AddLogEntry("Unexpected exception saving data for group \"" + groupName + "\": " + e.getMessage());
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
	
	synchronized void migrateData(DatabaseDataStore databaseStore)
	{
		//migrate claims
		for(int i = 0; i < this.claims.size(); i++)
		{
			Claim claim = this.claims.get(i);
			databaseStore.addClaim(claim);
		}
		
		//migrate groups
		Iterator<String> groupNamesEnumerator = this.permissionToBonusBlocksMap.keySet().iterator();
		while(groupNamesEnumerator.hasNext())
		{
			String groupName = groupNamesEnumerator.next();
			databaseStore.saveGroupBonusBlocks(groupName, this.permissionToBonusBlocksMap.get(groupName));
		}
		
		//migrate players
		File playerDataFolder = new File(playerDataFolderPath);
		File [] files = playerDataFolder.listFiles();
		for(int i = 0; i < files.length; i++)
		{
			File file = files[i];
			if(!file.isFile()) continue;  //avoids folders
			
			//all group data files start with a dollar sign.  ignoring those, already handled above
			if(file.getName().startsWith("$")) continue;
			
			String playerName = file.getName();
			databaseStore.savePlayerData(playerName, this.getPlayerData(playerName));
			this.clearCachedPlayerData(playerName);
		}
		
		//migrate next claim ID
		if(this.nextClaimID > databaseStore.nextClaimID)
		{
			databaseStore.setNextClaimID(this.nextClaimID);
		}
		
		//rename player and claim data folders so the migration won't run again
		int i = 0;
		File claimsBackupFolder;
		File playersBackupFolder;
		do
		{
			String claimsFolderBackupPath = claimDataFolderPath;
			if(i > 0) claimsFolderBackupPath += String.valueOf(i);
			claimsBackupFolder = new File(claimsFolderBackupPath);
			
			String playersFolderBackupPath = playerDataFolderPath;
			if(i > 0) playersFolderBackupPath += String.valueOf(i);
			playersBackupFolder = new File(playersFolderBackupPath);
			i++;
		} while(claimsBackupFolder.exists() || playersBackupFolder.exists());
		
		File claimsFolder = new File(claimDataFolderPath);
		File playersFolder = new File(playerDataFolderPath);
		
		claimsFolder.renameTo(claimsBackupFolder);
		playersFolder.renameTo(playersBackupFolder);			
		
		GriefPrevention.AddLogEntry("Backed your file system data up to " + claimsBackupFolder.getName() + " and " + playersBackupFolder.getName() + ".");
		GriefPrevention.AddLogEntry("If your migration encountered any problems, you can restore those data with a quick copy/paste.");
		GriefPrevention.AddLogEntry("When you're satisfied that all your data have been safely migrated, consider deleting those folders.");
	}

	@Override
	synchronized void close() { }
}
