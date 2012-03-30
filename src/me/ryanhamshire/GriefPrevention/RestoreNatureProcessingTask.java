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

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;

//non-main-thread task which processes world data to repair the unnatural
//after processing is complete, creates a main thread task to make the necessary changes to the world
class RestoreNatureProcessingTask implements Runnable 
{
	//world information captured from the main thread
	//will be updated and sent back to main thread to be applied to the world
	private BlockSnapshot[][][] snapshots;
	
	//other information collected from the main thread.
	//not to be updated, only to be passed back to main thread to provide some context about the operation
	private int miny;
	private Environment environment;
	private Location lesserBoundaryCorner;
	private Location greaterBoundaryCorner;
	private Player player;			//absolutely must not be accessed.  not thread safe.
	private Biome biome;
	private int seaLevel;
	
	//two lists of materials
	private ArrayList<Integer> notAllowedToHang;    //natural blocks which don't naturally hang in their air
	private ArrayList<Integer> playerBlocks;		//a "complete" list of player-placed blocks.  MUST BE MAINTAINED as patches introduce more
	
	public RestoreNatureProcessingTask(BlockSnapshot[][][] snapshots, int miny, Environment environment, Biome biome, Location lesserBoundaryCorner, Location greaterBoundaryCorner, int seaLevel, Player player)
	{
		this.snapshots = snapshots;
		this.miny = miny;
		this.environment = environment;
		this.lesserBoundaryCorner = lesserBoundaryCorner;
		this.greaterBoundaryCorner = greaterBoundaryCorner;
		this.biome = biome;
		this.seaLevel = seaLevel;
		this.player = player;
		
		this.notAllowedToHang = new ArrayList<Integer>();
		this.notAllowedToHang.add(Material.DIRT.getId());
		this.notAllowedToHang.add(Material.GRASS.getId());
		this.notAllowedToHang.add(Material.SNOW.getId());
		this.notAllowedToHang.add(Material.LOG.getId());
		
		//NOTE on this list.  why not make a list of natural blocks?
		//answer: better to leave a few player blocks than to remove too many natural blocks.  remember we're "restoring nature"
		//a few extra player blocks can be manually removed, but it will be impossible to guess exactly which natural materials to use in replacements
		this.playerBlocks = new ArrayList<Integer>();
		this.playerBlocks.add(Material.BED_BLOCK.getId());
		this.playerBlocks.add(Material.WOOD.getId());
		this.playerBlocks.add(Material.BOOKSHELF.getId());
		this.playerBlocks.add(Material.BREWING_STAND.getId());
		this.playerBlocks.add(Material.BRICK.getId());
		this.playerBlocks.add(Material.COBBLESTONE.getId());
		this.playerBlocks.add(Material.OBSIDIAN.getId());
		this.playerBlocks.add(Material.GLASS.getId());
		this.playerBlocks.add(Material.LAPIS_BLOCK.getId());
		this.playerBlocks.add(Material.DISPENSER.getId());
		this.playerBlocks.add(Material.NOTE_BLOCK.getId());
		this.playerBlocks.add(Material.POWERED_RAIL.getId());
		this.playerBlocks.add(Material.DETECTOR_RAIL.getId());
		this.playerBlocks.add(Material.PISTON_STICKY_BASE.getId());
		this.playerBlocks.add(Material.PISTON_BASE.getId());
		this.playerBlocks.add(Material.PISTON_EXTENSION.getId());
		this.playerBlocks.add(Material.WOOL.getId());
		this.playerBlocks.add(Material.PISTON_MOVING_PIECE.getId());
		this.playerBlocks.add(Material.GOLD_BLOCK.getId());
		this.playerBlocks.add(Material.IRON_BLOCK.getId());
		this.playerBlocks.add(Material.DOUBLE_STEP.getId());
		this.playerBlocks.add(Material.STEP.getId());
		this.playerBlocks.add(Material.CROPS.getId());
		this.playerBlocks.add(Material.TNT.getId());
		this.playerBlocks.add(Material.MOSSY_COBBLESTONE.getId());
		this.playerBlocks.add(Material.TORCH.getId());
		this.playerBlocks.add(Material.FIRE.getId());
		this.playerBlocks.add(Material.WOOD_STAIRS.getId());
		this.playerBlocks.add(Material.CHEST.getId());
		this.playerBlocks.add(Material.REDSTONE_WIRE.getId());
		this.playerBlocks.add(Material.DIAMOND_BLOCK.getId());
		this.playerBlocks.add(Material.WORKBENCH.getId());
		this.playerBlocks.add(Material.SOIL.getId());
		this.playerBlocks.add(Material.FURNACE.getId());
		this.playerBlocks.add(Material.BURNING_FURNACE.getId());
		this.playerBlocks.add(Material.WOODEN_DOOR.getId());
		this.playerBlocks.add(Material.SIGN_POST.getId());
		this.playerBlocks.add(Material.LADDER.getId());
		this.playerBlocks.add(Material.RAILS.getId());
		this.playerBlocks.add(Material.COBBLESTONE_STAIRS.getId());
		this.playerBlocks.add(Material.WALL_SIGN.getId());
		this.playerBlocks.add(Material.STONE_PLATE.getId());
		this.playerBlocks.add(Material.LEVER.getId());
		this.playerBlocks.add(Material.IRON_DOOR_BLOCK.getId());
		this.playerBlocks.add(Material.WOOD_PLATE.getId());
		this.playerBlocks.add(Material.REDSTONE_TORCH_ON.getId());
		this.playerBlocks.add(Material.REDSTONE_TORCH_OFF.getId());
		this.playerBlocks.add(Material.STONE_BUTTON.getId());
		this.playerBlocks.add(Material.SNOW_BLOCK.getId());
		this.playerBlocks.add(Material.JUKEBOX.getId());
		this.playerBlocks.add(Material.FENCE.getId());
		this.playerBlocks.add(Material.PORTAL.getId());
		this.playerBlocks.add(Material.JACK_O_LANTERN.getId());
		this.playerBlocks.add(Material.CAKE_BLOCK.getId());
		this.playerBlocks.add(Material.DIODE_BLOCK_ON.getId());
		this.playerBlocks.add(Material.DIODE_BLOCK_OFF.getId());
		this.playerBlocks.add(Material.TRAP_DOOR.getId());
		this.playerBlocks.add(Material.SMOOTH_BRICK.getId());
		this.playerBlocks.add(Material.HUGE_MUSHROOM_1.getId());
		this.playerBlocks.add(Material.HUGE_MUSHROOM_2.getId());
		this.playerBlocks.add(Material.IRON_FENCE.getId());
		this.playerBlocks.add(Material.THIN_GLASS.getId());
		this.playerBlocks.add(Material.MELON_STEM.getId());
		this.playerBlocks.add(Material.FENCE_GATE.getId());
		this.playerBlocks.add(Material.BRICK_STAIRS.getId());
		this.playerBlocks.add(Material.SMOOTH_STAIRS.getId());
		this.playerBlocks.add(Material.ENCHANTMENT_TABLE.getId());
		this.playerBlocks.add(Material.BREWING_STAND.getId());
		this.playerBlocks.add(Material.CAULDRON.getId());
		this.playerBlocks.add(Material.DIODE_BLOCK_ON.getId());
		this.playerBlocks.add(Material.DIODE_BLOCK_ON.getId());
		
		//these are unnatural in the standard world, but not in the nether
		if(this.environment != Environment.NETHER)
		{
			this.playerBlocks.add(Material.NETHERRACK.getId());
			this.playerBlocks.add(Material.SOUL_SAND.getId());
			this.playerBlocks.add(Material.GLOWSTONE.getId());
			this.playerBlocks.add(Material.NETHER_BRICK.getId());
			this.playerBlocks.add(Material.NETHER_FENCE.getId());
			this.playerBlocks.add(Material.NETHER_BRICK_STAIRS.getId());
		}
		
		//these are unnatural in sandy biomes, but not elsewhere
		if(this.biome == Biome.DESERT || this.biome == Biome.DESERT_HILLS || this.biome == Biome.BEACH)
		{
			this.playerBlocks.add(Material.LEAVES.getId());
			this.playerBlocks.add(Material.LOG.getId());
		}
	}
	
	@Override
	public void run()
	{
		//order is important!
		
		//remove any blocks which are definitely player placed
		this.removePlayerBlocks();
		
		//remove natural blocks which are unnaturally hanging in the air
		this.removeHanging();
		
		//remove natural blocks which are unnaturally stacked high
		this.removeWallsAndTowers();
		
		//cover surface stone and gravel with sand or grass, as the biome requires
		this.coverSurfaceStone();
		
		//fill unnatural thin trenches and single-block potholes
		this.fillHolesAndTrenches();
		
		//fill water depressions and fix unnatural surface ripples
		this.fixWater();
		
		//schedule main thread task to apply the result to the world
		RestoreNatureExecutionTask task = new RestoreNatureExecutionTask(this.snapshots, this.miny, this.lesserBoundaryCorner, this.greaterBoundaryCorner, this.player);
		GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task);
	}
	
	private void removePlayerBlocks()
	{
		int miny = this.miny;
		if(miny < 1) miny = 1;
		
		for(int x = 1; x < snapshots.length - 1; x++)
		{
			for(int z = 1; z < snapshots[0][0].length - 1; z++)
			{
				for(int y = miny; y < snapshots[0].length - 1; y++)
				{
					BlockSnapshot block = snapshots[x][y][z];
					if(this.playerBlocks.contains(block.typeId))
					{
						block.typeId = Material.AIR.getId();
					}
				}
			}
		}
	}
	
	private void removeHanging()
	{
		int miny = this.miny;
		if(miny < 1) miny = 1;
		
		for(int x = 1; x < snapshots.length - 1; x++)
		{
			for(int z = 1; z < snapshots[0][0].length - 1; z++)
			{
				for(int y = miny; y < snapshots[0].length - 1; y++)
				{
					BlockSnapshot block = snapshots[x][y][z];
					BlockSnapshot underBlock = snapshots[x][y - 1][z];
					
					if(underBlock.typeId == Material.AIR.getId() || underBlock.typeId == Material.WATER.getId())
					{
						if(this.notAllowedToHang.contains(block.typeId))
						{
							block.typeId = Material.AIR.getId();
						}
					}
				}
			}
		}
	}
	
	private void removeWallsAndTowers()
	{
		int [] excludedBlocksArray = new int []
		{
			Material.CACTUS.getId(),
			Material.LONG_GRASS.getId(),
			Material.RED_MUSHROOM.getId(),
			Material.BROWN_MUSHROOM.getId(),
			Material.DEAD_BUSH.getId(),
			Material.SAPLING.getId(),
			Material.YELLOW_FLOWER.getId(),
			Material.RED_ROSE.getId(),
			Material.SUGAR_CANE_BLOCK.getId(),
			Material.VINE.getId(),
			Material.PUMPKIN.getId(),
			Material.WATER_LILY.getId(),
			Material.LEAVES.getId()
		};
		
		ArrayList<Integer> excludedBlocks = new ArrayList<Integer>();
		for(int i = 0; i < excludedBlocksArray.length; i++) excludedBlocks.add(excludedBlocksArray[i]);
		
		boolean changed;
		do
		{
			changed = false;
			for(int x = 1; x < snapshots.length - 1; x++)
			{
				for(int z = 1; z < snapshots[0][0].length - 1; z++)
				{
					int thisy = this.highestY(x, z);
					if(excludedBlocks.contains(this.snapshots[x][thisy][z].typeId)) continue;
						
					int righty = this.highestY(x + 1, z);
					int lefty = this.highestY(x - 1, z);
					while(lefty < thisy && righty < thisy)
					{
						this.snapshots[x][thisy--][z].typeId = Material.AIR.getId();
						changed = true;
					}
					
					int upy = this.highestY(x, z + 1);
					int downy = this.highestY(x, z - 1);
					while(upy < thisy && downy < thisy)
					{
						this.snapshots[x][thisy--][z].typeId = Material.AIR.getId();
						changed = true;
					}
				}
			}
		}while(changed);
	}
	
	private void coverSurfaceStone()
	{
		for(int x = 1; x < snapshots.length - 1; x++)
		{
			for(int z = 1; z < snapshots[0][0].length - 1; z++)
			{
				int y = this.highestY(x, z);
				BlockSnapshot block = snapshots[x][y][z];
				
				if(block.typeId == Material.STONE.getId() || block.typeId == Material.GRAVEL.getId() || block.typeId == Material.DIRT.getId())
				{
					if(this.biome == Biome.DESERT || this.biome == Biome.DESERT_HILLS || this.biome == Biome.BEACH)
					{
						this.snapshots[x][y][z].typeId = Material.SAND.getId();
					}
					else
					{
						this.snapshots[x][y][z].typeId = Material.GRASS.getId();
					}
				}
			}
		}
	}
	
	private void fillHolesAndTrenches()
	{
		ArrayList<Integer> fillableBlocks = new ArrayList<Integer>();
		fillableBlocks.add(Material.AIR.getId());
		fillableBlocks.add(Material.STATIONARY_WATER.getId());
		fillableBlocks.add(Material.STATIONARY_LAVA.getId());
		
		ArrayList<Integer> notSuitableForFillBlocks = new ArrayList<Integer>();
		notSuitableForFillBlocks.add(Material.LONG_GRASS.getId());
		notSuitableForFillBlocks.add(Material.CACTUS.getId());
		notSuitableForFillBlocks.add(Material.STATIONARY_WATER.getId());
		notSuitableForFillBlocks.add(Material.STATIONARY_LAVA.getId());
		
		boolean changed;
		do
		{
			changed = false;
			for(int x = 1; x < snapshots.length - 1; x++)
			{
				for(int z = 1; z < snapshots[0][0].length - 1; z++)
				{
					for(int y = 0; y < snapshots[0].length - 1; y++)
					{
						BlockSnapshot block = this.snapshots[x][y][z];
						if(!fillableBlocks.contains(block.typeId)) continue;
							
						BlockSnapshot leftBlock = this.snapshots[x + 1][y][z];
						BlockSnapshot rightBlock = this.snapshots[x - 1][y][z];
						
						if(!fillableBlocks.contains(leftBlock.typeId) && !fillableBlocks.contains(rightBlock.typeId))
						{
							if(!notSuitableForFillBlocks.contains(rightBlock.typeId))
							{
								block.typeId = rightBlock.typeId;
								changed = true;
							}
						}
					
						BlockSnapshot upBlock = this.snapshots[x][y][z + 1];
						BlockSnapshot downBlock = this.snapshots[x][y][z - 1];
						
						if(!fillableBlocks.contains(upBlock.typeId) && !fillableBlocks.contains(downBlock.typeId))
						{	
							if(!notSuitableForFillBlocks.contains(downBlock.typeId))
							{
								block.typeId = downBlock.typeId;
								changed = true;
							}
						}
					}
				}
			}
		}while(changed);
	}	
	
	private void fixWater()
	{
		int miny = this.miny;
		if(miny < 1) miny = 1;
		
		boolean changed;
		
		//remove hanging water or lava
		for(int x = 1; x < snapshots.length - 1; x++)
		{
			for(int z = 1; z < snapshots[0][0].length - 1; z++)
			{
				for(int y = miny; y < snapshots[0].length - 1; y++)
				{
					BlockSnapshot block = this.snapshots[x][y][z];
					BlockSnapshot underBlock = this.snapshots[x][y][z];
					if(block.typeId == Material.STATIONARY_WATER.getId() || block.typeId == Material.STATIONARY_LAVA.getId())
					{
						if(underBlock.typeId == Material.AIR.getId() || (underBlock.data != 0))
						{
							block.typeId = Material.AIR.getId();
						}
					}
				}
			}
		}
		
		//fill water depressions
		do
		{
			changed = false;		
			for(int y = this.seaLevel - 10; y <= this.seaLevel; y++)			
			{
				for(int x = 1; x < snapshots.length - 1; x++)				
				{
					for(int z = 1; z < snapshots[0][0].length - 1; z++)
					{
						BlockSnapshot block = snapshots[x][y][z];
						
						//only consider air blocks and flowing water blocks for upgrade to water source blocks
						if(block.typeId == Material.AIR.getId() || (block.typeId == Material.STATIONARY_WATER.getId() && block.data != 0))
						{
							BlockSnapshot leftBlock = this.snapshots[x + 1][y][z];
							BlockSnapshot rightBlock = this.snapshots[x - 1][y][z];
							BlockSnapshot upBlock = this.snapshots[x][y][z + 1];
							BlockSnapshot downBlock = this.snapshots[x][y][z - 1];
							BlockSnapshot underBlock = this.snapshots[x][y - 1][z];
							
							//block underneath MUST be source water
							if(underBlock.typeId != Material.STATIONARY_WATER.getId() || underBlock.data != 0) continue;
							
							//count adjacent source water blocks
							byte adjacentSourceWaterCount = 0;
							if(leftBlock.typeId == Material.STATIONARY_WATER.getId() && leftBlock.data == 0)
							{
								adjacentSourceWaterCount++;
							}
							if(rightBlock.typeId == Material.STATIONARY_WATER.getId() && rightBlock.data == 0)
							{
								adjacentSourceWaterCount++;
							}
							if(upBlock.typeId == Material.STATIONARY_WATER.getId() && upBlock.data == 0)
							{
								adjacentSourceWaterCount++;
							}
							if(downBlock.typeId == Material.STATIONARY_WATER.getId() && downBlock.data == 0)
							{
								adjacentSourceWaterCount++;
							}
							
							//at least two adjacent blocks must be source water
							if(adjacentSourceWaterCount >= 2)
							{
								block.typeId = Material.STATIONARY_WATER.getId();
								block.data = 0;
								changed = true;
							}
						}
					}
				}
			}
		}while(changed);
	}
	
	private int highestY(int x, int z)
	{
		int y;
		for(y = snapshots[0].length - 1; y >= 0; y--)
		{
			BlockSnapshot block = this.snapshots[x][y][z];
			if(block.typeId != Material.AIR.getId() && 
			!(block.typeId == Material.STATIONARY_WATER.getId() && block.data != 0) &&
			!(block.typeId == Material.STATIONARY_LAVA.getId() && block.data != 0))
			{
				return y;
			}
		}
		
		return y;
	}
}
