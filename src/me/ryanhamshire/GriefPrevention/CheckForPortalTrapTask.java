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

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

//players can be "trapped" in a portal frame if they don't have permission to break
//solid blocks blocking them from exiting the frame
//if that happens, we detect the problem and send them back through the portal.
class CheckForPortalTrapTask implements Runnable 
{
	//player who recently teleported via nether portal 
	private Player player;
	
	//where to send the player back to if he hasn't left the portal frame
	private Location returnLocation;
	
	public CheckForPortalTrapTask(Player player, Location location)
	{
		this.player = player;
		this.returnLocation = location;		
	}
	
	@Override
	public void run()
	{
		//if player has logged out, do nothing
	    if(!this.player.isOnline()) return;
	    
	    //otherwise if still standing in a portal frame, teleport him back through
	    if(this.player.getLocation().getBlock().getType() == Material.PORTAL)
	    {
	        this.player.teleport(this.returnLocation);
	    }
	}
}
