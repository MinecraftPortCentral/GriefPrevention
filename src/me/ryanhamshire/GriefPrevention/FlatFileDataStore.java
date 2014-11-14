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
import java.util.regex.Matcher;
import org.bukkit.*;

//manages data stored in the file system
public class FlatFileDataStore extends DataStore
{
	private final static String playerDataFolderPath = dataLayerFolderPath + File.separator + "PlayerData";
	private final static String claimDataFolderPath = dataLayerFolderPath + File.separator + "ClaimData";
	private final static String nextClaimIdFilePath = claimDataFolderPath + File.separator + "_nextClaimID";
	private final static String schemaVersionFilePath = dataLayerFolderPath + File.separator + "_schemaVersion";
	
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
		boolean newDataStore = false;
	    File playerDataFolder = new File(playerDataFolderPath);
		File claimDataFolder = new File(claimDataFolderPath);
		if(!playerDataFolder.exists() || !claimDataFolder.exists())
		{
		    newDataStore = true;
		    playerDataFolder.mkdirs();
		    claimDataFolder.mkdirs();
		}
		
		//if there's no data yet, then anything written will use the schema implemented by this code
		if(newDataStore)
		{
		    this.setSchemaVersion(DataStore.latestSchemaVersion);
		}
		
		//load group data into memory
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
		
		//if converting up from schema version 0, rename player data files using UUIDs instead of player names
        //get a list of all the files in the claims data folder
        if(this.getSchemaVersion() == 0)
        {
            files = playerDataFolder.listFiles();
            ArrayList<String> namesToConvert = new ArrayList<String>();
            for(File playerFile : files)
            {
                namesToConvert.add(playerFile.getName());
            }
            
            //resolve and cache as many as possible through various means
            try
            {
                UUIDFetcher fetcher = new UUIDFetcher(namesToConvert);
                fetcher.call();
            }
            catch(Exception e)
            {
                GriefPrevention.AddLogEntry("Failed to resolve a batch of names to UUIDs.  Details:" + e.getMessage());
                e.printStackTrace();
            }
            
            //rename files
            for(File playerFile : files)
            {
                String currentFilename = playerFile.getName();
                
                //try to convert player name to UUID
                UUID playerID = null;
                try
                {
                    playerID = UUIDFetcher.getUUIDOf(currentFilename);
                    
                    //if successful, rename the file using the UUID
                    if(playerID != null)
                    {
                        playerFile.renameTo(new File(playerDataFolder, playerID.toString()));
                    }
                }
                catch(Exception ex){ }
            }
        }
		
		//load claims data into memory		
		//get a list of all the files in the claims data folder
		files = claimDataFolder.listFiles();
		
		for(int i = 0; i < files.length; i++)
		{			
			if(files[i].isFile())  //avoids folders
			{
				//skip any file starting with an underscore, to avoid special files not representing land claims
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
						//skip any SUB:### lines from previous versions
					    if(line.startsWith("SUB:"))
					    {
					        line = inStream.readLine();
					    }
					    
					    //skip any UUID lines from previous versions
						Matcher match = uuidpattern.matcher(line.trim());
						if(match.find())
						{
							line = inStream.readLine();
						}
						
						//first line is lesser boundary corner location
						Location lesserBoundaryCorner = this.locationFromString(line);
						
						//second line is greater boundary corner location
						line = inStream.readLine();
						Location greaterBoundaryCorner = this.locationFromString(line);
						
						//third line is owner name
						line = inStream.readLine();						
						String ownerName = line;
						UUID ownerID = null;
						if(ownerName.isEmpty() || ownerName.startsWith("--"))
						{
						    ownerID = null;  //administrative land claim or subdivision
						}
						else if(this.getSchemaVersion() == 0)
						{
						    try
						    {
						        ownerID = UUIDFetcher.getUUIDOf(ownerName);
						    }
						    catch(Exception ex)
						    {
						        GriefPrevention.AddLogEntry("Couldn't resolve this name to a UUID: " + ownerName + ".");
                                GriefPrevention.AddLogEntry("  Converted land claim to administrative @ " + lesserBoundaryCorner.toString());
						    }
						}
						else
						{
						    try
						    {
						        ownerID = UUID.fromString(ownerName);
						    }
						    catch(Exception ex)
						    {
						        GriefPrevention.AddLogEntry("Error - this is not a valid UUID: " + ownerName + ".");
						        GriefPrevention.AddLogEntry("  Converted land claim to administrative @ " + lesserBoundaryCorner.toString());
						    }
						}
						
						//fourth line is list of builders
						line = inStream.readLine();
						String [] builderNames = line.split(";");
						builderNames = this.convertNameListToUUIDList(builderNames);
						
						//fifth line is list of players who can access containers
						line = inStream.readLine();
						String [] containerNames = line.split(";");
						containerNames = this.convertNameListToUUIDList(containerNames);
						
						//sixth line is list of players who can use buttons and switches
						line = inStream.readLine();
						String [] accessorNames = line.split(";");
						accessorNames = this.convertNameListToUUIDList(accessorNames);
						
						//seventh line is list of players who can grant permissions
						line = inStream.readLine();
						if(line == null) line = "";
						String [] managerNames = line.split(";");
						managerNames = this.convertNameListToUUIDList(managerNames);
						
						//skip any remaining extra lines, until the "===" string, indicating the end of this claim or subdivision
						line = inStream.readLine();
						while(line != null && !line.contains("=========="))
							line = inStream.readLine();
						
						//build a claim instance from those data
						//if this is the first claim loaded from this file, it's the top level claim
						if(topLevelClaim == null)
						{
							//instantiate
							topLevelClaim = new Claim(lesserBoundaryCorner, greaterBoundaryCorner, ownerID, builderNames, containerNames, accessorNames, managerNames, claimID);
							
							topLevelClaim.modifiedDate = new Date(files[i].lastModified());
							this.addClaim(topLevelClaim, false);
						}
						
						//otherwise there's already a top level claim, so this must be a subdivision of that top level claim
						else
						{
							Claim subdivision = new Claim(lesserBoundaryCorner, greaterBoundaryCorner, null, builderNames, containerNames, accessorNames, managerNames, null);
							
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
					if(e.getMessage().contains("World not found"))
					{
					    files[i].delete();
					}
					else
					{
					    GriefPrevention.AddLogEntry("Unable to load data for claim \"" + files[i].getName() + "\": " + e.toString());
					    e.printStackTrace();
					}
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
			GriefPrevention.AddLogEntry("Unexpected exception saving data for claim \"" + claimID + "\": " + e.toString());
			e.printStackTrace();
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
		String lineToWrite = "";
		if(claim.ownerID != null) lineToWrite = claim.ownerID.toString();
		outStream.write(lineToWrite);
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
	synchronized PlayerData getPlayerDataFromStorage(UUID playerID)
	{
		File playerFile = new File(playerDataFolderPath + File.separator + playerID.toString());
					
		PlayerData playerData = new PlayerData();
		playerData.playerID = playerID;
		
		//if it exists as a file, read the file
		if(playerFile.exists())
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
					playerData.setLastLogin(dateFormat.parse(lastLoginTimestampString));
				}
				catch(ParseException parseException)
				{
					GriefPrevention.AddLogEntry("Unable to load last login for \"" + playerFile.getName() + "\".");
					playerData.setLastLogin(null);
				}
				
				//second line is accrued claim blocks
				String accruedBlocksString = inStream.readLine();
				
				//convert that to a number and store it
				playerData.setAccruedClaimBlocks(Integer.parseInt(accruedBlocksString));
				
				//third line is any bonus claim blocks granted by administrators
				String bonusBlocksString = inStream.readLine();					
				
				//convert that to a number and store it										
				playerData.setBonusClaimBlocks(Integer.parseInt(bonusBlocksString));
				
				//fourth line is a double-semicolon-delimited list of claims, which is currently ignored
				//String claimsString = inStream.readLine();
				inStream.readLine();
				
				inStream.close();
			}
				
			//if there's any problem with the file's content, log an error message
			catch(Exception e)
			{
				 GriefPrevention.AddLogEntry("Unable to load data for player \"" + playerID.toString() + "\": " + e.toString());
				 e.printStackTrace();
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
	public void asyncSavePlayerData(UUID playerID, PlayerData playerData)
	{
		//never save data for the "administrative" account.  null for claim owner ID indicates administrative account
		if(playerID == null) return;
		
		BufferedWriter outStream = null;
		try
		{
			//open the player's file
			File playerDataFile = new File(playerDataFolderPath + File.separator + playerID.toString());
			playerDataFile.createNewFile();
			outStream = new BufferedWriter(new FileWriter(playerDataFile));
			
			//first line is last login timestamp
			if(playerData.getLastLogin() == null) playerData.setLastLogin(new Date());
			DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
			outStream.write(dateFormat.format(playerData.getLastLogin()));
			outStream.newLine();
			
			//second line is accrued claim blocks
			outStream.write(String.valueOf(playerData.getAccruedClaimBlocks()));
			outStream.newLine();			
			
			//third line is bonus claim blocks
			outStream.write(String.valueOf(playerData.getBonusClaimBlocks()));
			outStream.newLine();						
			
			//fourth line is a double-semicolon-delimited list of claims
			if(playerData.getClaims().size() > 0)
			{
				outStream.write(this.locationToString(playerData.getClaims().get(0).getLesserBoundaryCorner()));
				for(int i = 1; i < playerData.getClaims().size(); i++)
				{
					outStream.write(";;" + this.locationToString(playerData.getClaims().get(i).getLesserBoundaryCorner()));
				}
			}
			outStream.newLine();
		}		
		
		//if any problem, log it
		catch(Exception e)
		{
			GriefPrevention.AddLogEntry("GriefPrevention: Unexpected exception saving data for player \"" + playerID.toString() + "\": " + e.getMessage());
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
			e.printStackTrace();
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
			databaseStore.addClaim(claim, true);
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
			
			//ignore special files (claimID)
			if(file.getName().startsWith("_")) continue;
			
			UUID playerID = UUID.fromString(file.getName());
			databaseStore.savePlayerData(playerID, this.getPlayerData(playerID));
			this.clearCachedPlayerData(playerID);
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

    @Override
    int getSchemaVersionFromStorage()
    {
        File schemaVersionFile = new File(schemaVersionFilePath);
        if(schemaVersionFile.exists())
        {
            BufferedReader inStream = null;
            int schemaVersion = 0;
            try
            {
                inStream = new BufferedReader(new FileReader(schemaVersionFile.getAbsolutePath()));
                
                //read the version number
                String line = inStream.readLine();
                
                //try to parse into an int value
                schemaVersion = Integer.parseInt(line);
            }
            catch(Exception e){ }
            
            try
            {
                if(inStream != null) inStream.close();                  
            }
            catch(IOException exception) {}
            
            return schemaVersion;
        }
        else
        {
            this.updateSchemaVersionInStorage(0);
            return 0;
        }
    }

    @Override
    void updateSchemaVersionInStorage(int versionToSet)
    {
        BufferedWriter outStream = null;
        
        try
        {
            //open the file and write the new value
            File schemaVersionFile = new File(schemaVersionFilePath);
            schemaVersionFile.createNewFile();
            outStream = new BufferedWriter(new FileWriter(schemaVersionFile));
            
            outStream.write(String.valueOf(versionToSet));
        }       
        
        //if any problem, log it
        catch(Exception e)
        {
            GriefPrevention.AddLogEntry("Unexpected exception saving schema version: " + e.getMessage());
        }
        
        //close the file
        try
        {
            if(outStream != null) outStream.close();
        }
        catch(IOException exception) {}
        
    }
}
