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

import java.util.ArrayList;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

//FEATURE: treetops left unnaturally hanging will be automatically cleaned up

//this main thread task revisits the location of a partially chopped tree from several minutes ago
//if any part of the tree is still there and nothing else has been built in its place, remove the remaining parts
class TreeCleanupTask implements Runnable 
{
	private Block originalChoppedBlock;          //first block chopped in the tree
	private Block originalRootBlock;             //where the root of the tree used to be
	private byte originalRootBlockData;			 //data value of that root block (TYPE of log)
	private ArrayList<Block> originalTreeBlocks; //a list of other log blocks determined to be part of this tree
	
	public TreeCleanupTask(Block originalChoppedBlock, Block originalRootBlock, ArrayList<Block> originalTreeBlocks, byte originalRootBlockData)
	{
		this.originalChoppedBlock = originalChoppedBlock;
		this.originalRootBlock = originalRootBlock;
		this.originalTreeBlocks = originalTreeBlocks;
		this.originalRootBlockData = originalRootBlockData;
	}
	
	@Override
	public void run() 
	{
		//if this chunk is no longer loaded, load it and come back in a few seconds
		Chunk chunk = this.originalChoppedBlock.getWorld().getChunkAt(this.originalChoppedBlock); 
		if(!chunk.isLoaded())
		{
			chunk.load();
			GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, this, 100L);
			return;
		}
		
		//if the block originally chopped has been replaced with anything but air, something has been built (or has grown here)
		//in that case, don't do any cleanup
		if(this.originalChoppedBlock.getWorld().getBlockAt(this.originalChoppedBlock.getLocation()).getType() != Material.AIR) return;
		
		//scan the original tree block locations to see if any of them have been replaced		
		for(int i = 0; i < this.originalTreeBlocks.size(); i++)
		{
			Location location = this.originalTreeBlocks.get(i).getLocation();
			Block currentBlock = location.getBlock();
			
			//if the block has been replaced, stop here, we won't do any cleanup
			if(currentBlock.getType() != Material.LOG && currentBlock.getType() != Material.AIR)
			{
				return;
			}
		}
		
		//otherwise scan again, this time removing any remaining log blocks
		boolean logsRemaining = false;
		for(int i = 0; i < this.originalTreeBlocks.size(); i++)
		{
			Location location = this.originalTreeBlocks.get(i).getLocation();
			Block currentBlock = location.getBlock();
			if(currentBlock.getType() == Material.LOG)
			{
				logsRemaining = true;
				currentBlock.setType(Material.AIR);
			}
		}
		
		//if any were actually removed and we're set to automatically replant griefed trees, place a sapling where the root block was previously
		if(logsRemaining && GriefPrevention.instance.config_trees_regrowGriefedTrees)
		{
			Block currentBlock = this.originalRootBlock.getLocation().getBlock();
			//make sure there's grass or dirt underneath
			if(currentBlock.getType() == Material.AIR && (currentBlock.getRelative(BlockFace.DOWN).getType() == Material.DIRT || currentBlock.getRelative(BlockFace.DOWN).getType() == Material.GRASS))
			{
				currentBlock.setType(Material.SAPLING);
				currentBlock.setData(this.originalRootBlockData);  //makes the sapling type match the original tree type
			}
		}
	}
}
