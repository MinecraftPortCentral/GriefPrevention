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

import org.bukkit.Location;
import org.bukkit.entity.Player;

//FEATURE: give players claim blocks for playing, as long as they're not away from their computer

//runs every 5 minutes in the main thread, grants blocks per hour / 12 to each online player who appears to be actively playing
class DeliverClaimBlocksTask implements Runnable 
{	
	@Override
	public void run()
	{
		Player [] players = GriefPrevention.instance.getServer().getOnlinePlayers();
		
		//for each online player
		for(int i = 0; i < players.length; i++)
		{
			Player player = players[i];
			DataStore dataStore = GriefPrevention.instance.dataStore;
			PlayerData playerData = dataStore.getPlayerData(player.getName());
			
			Location lastLocation = playerData.lastAfkCheckLocation;
			try  //distance squared will throw an exception if the player has changed worlds
			{
				//if he's not in a vehicle and has moved at least three blocks since the last check
				if(!player.isInsideVehicle() && (lastLocation == null || lastLocation.distanceSquared(player.getLocation()) >= 9))
				{
					playerData.accruedClaimBlocks += GriefPrevention.instance.config_claims_blocksAccruedPerHour / 12;
					
					//respect limits
					if(playerData.accruedClaimBlocks > GriefPrevention.instance.config_claims_maxAccruedBlocks)
					{
						playerData.accruedClaimBlocks = GriefPrevention.instance.config_claims_maxAccruedBlocks; 
					}
					
					dataStore.savePlayerData(player.getName(), playerData);
				}
			}
			catch(Exception e) { }
			
			//remember current location for next time
			playerData.lastAfkCheckLocation = player.getLocation();
		}
	}
}
