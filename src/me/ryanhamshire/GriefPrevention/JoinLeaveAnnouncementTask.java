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

import org.bukkit.entity.Player;

//this main thread task takes the output from the RestoreNatureProcessingTask\
//and updates the world accordingly
class JoinLeaveAnnouncementTask implements Runnable 
{
	//player joining or leaving the server
	private Player player;
	
	//message to be displayed
	private String message;
	
	//whether joining or leaving
	private boolean joining;

	public JoinLeaveAnnouncementTask(Player player, String message, boolean joining)
	{
		this.player = player;
		this.message = message;
		this.joining = joining;
	}
	
	@Override
	public void run()
	{
		//verify the player still has the same online/offline status
		if((this.joining && this.player.isOnline() || (!this.joining && !this.player.isOnline())))
		{
			Player players [] = GriefPrevention.instance.getServer().getOnlinePlayers();
			for(int i = 0; i < players.length; i++)
			{
				if(!players[i].equals(this.player))
				{
					players[i].sendMessage(this.message);
				}
			}
			
			//if left
			if(!joining)
			{
				//drop player data from memory
				GriefPrevention.instance.dataStore.clearCachedPlayerData(this.player.getName());			
			}
		}		
	}	
}
