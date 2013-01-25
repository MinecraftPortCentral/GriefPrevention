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
	private boolean creativeMode;
	private int seaLevel;
	private boolean aggressiveMode;
	
	//two lists of materials
	private ArrayList<Integer> notAllowedToHang;    //natural blocks which don't naturally hang in their air
	private ArrayList<Integer> playerBlocks;		//a "complete" list of player-placed blocks.  MUST BE MAINTAINED as patches introduce more
	
	public RestoreNatureProcessingTask(BlockSnapshot[][][] snapshots, int miny, Environment environment, Biome biome, Location lesserBoundaryCorner, Location greaterBoundaryCorner, int seaLevel, boolean aggressiveMode, boolean creativeMode, Player player)
	{
		this.snapshots = snapshots;
		this.miny = miny;
		if(this.miny < 0) this.miny = 0;
		this.environment = environment;
		this.lesserBoundaryCorner = lesserBoundaryCorner;
		this.greaterBoundaryCorner = greaterBoundaryCorner;
		this.biome = biome;
		this.seaLevel = seaLevel;
		this.aggressiveMode = aggressiveMode;
		this.player = player;
		this.creativeMode = creativeMode;
		
		this.notAllowedToHang = new ArrayList<Integer>();
		this.notAllowedToHang.add(Material.DIRT.getId());
		this.notAllowedToHang.add(Material.LONG_GRASS.getId());
		this.notAllowedToHang.add(Material.SNOW.getId());
		this.notAllowedToHang.add(Material.LOG.getId());
		
		if(this.aggressiveMode)
		{
			this.notAllowedToHang.add(Material.GRASS.getId());			
			this.notAllowedToHang.add(Material.STONE.getId());
		}
		
		this.playerBlocks = new ArrayList<Integer>();
		this.playerBlocks.addAll(RestoreNatureProcessingTask.getPlayerBlocks(this.environment, this.biome));
		
		//in aggressive or creative world mode, also treat these blocks as user placed, to be removed
		//this is helpful in the few cases where griefers intentionally use natural blocks to grief,
		//like a single-block tower of iron ore or a giant penis constructed with melons
		if(this.aggressiveMode || this.creativeMode)
		{
			this.playerBlocks.add(Material.IRON_ORE.getId());
			this.playerBlocks.add(Material.GOLD_ORE.getId());
			this.playerBlocks.add(Material.DIAMOND_ORE.getId());
			this.playerBlocks.add(Material.MELON_BLOCK.getId());
			this.playerBlocks.add(Material.MELON_STEM.getId());
			this.playerBlocks.add(Material.BEDROCK.getId());
			this.playerBlocks.add(Material.COAL_ORE.getId());
			this.playerBlocks.add(Material.PUMPKIN.getId());
			this.playerBlocks.add(Material.PUMPKIN_STEM.getId());
			this.playerBlocks.add(Material.MELON.getId());
		}
		
		if(this.aggressiveMode)
		{
			this.playerBlocks.add(Material.LEAVES.getId());
			this.playerBlocks.add(Material.LOG.getId());
			this.playerBlocks.add(Material.VINE.getId());
		}
	}
	
	@Override
	public void run()
	{
		//order is important!
		
		//remove sandstone which appears to be unnatural
		this.removeSandstone();
		
		//remove any blocks which are definitely player placed
		this.removePlayerBlocks();
		
		//reduce large outcroppings of stone, sandstone
		this.reduceStone();
		
		//reduce logs, except in jungle biomes
		this.reduceLogs();
		
		//remove natural blocks which are unnaturally hanging in the air
		this.removeHanging();
		
		//remove natural blocks which are unnaturally stacked high
		this.removeWallsAndTowers();
		
		//fill unnatural thin trenches and single-block potholes
		this.fillHolesAndTrenches();
		
		//fill water depressions and fix unnatural surface ripples
		this.fixWater();
		
		//remove water/lava above sea level
		this.removeDumpedFluids();
		
		//cover over any gaping holes in creative mode worlds
		if(this.creativeMode && this.environment == Environment.NORMAL)
		{
			this.fillBigHoles();
		}
		
		//cover surface stone and gravel with sand or grass, as the biome requires
		this.coverSurfaceStone();
		
		//remove any player-placed leaves
		this.removePlayerLeaves();
		
		//schedule main thread task to apply the result to the world
		RestoreNatureExecutionTask task = new RestoreNatureExecutionTask(this.snapshots, this.miny, this.lesserBoundaryCorner, this.greaterBoundaryCorner, this.player);
		GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task);
	}
	
	private void removePlayerLeaves()
	{
		for(int x = 1; x < snapshots.length - 1; x++)
		{
			for(int z = 1; z < snapshots[0][0].length - 1; z++)
			{
				for(int y = this.seaLevel - 1; y < snapshots[0].length; y++)
				{
					//note: see minecraft wiki data values for leaves
					BlockSnapshot block = snapshots[x][y][z];
					if(block.typeId == Material.LEAVES.getId() && (block.data & 0x4) != 0)
					{
						block.typeId = Material.AIR.getId();
					}
				}
			}
		}		
	}

	private void fillBigHoles()
	{
		for(int x = 1; x < snapshots.length - 1; x++)
		{
			for(int z = 1; z < snapshots[0][0].length - 1; z++)
			{
				//replace air, lava, or running water at sea level with stone
				if(this.snapshots[x][this.seaLevel - 2][z].typeId == Material.AIR.getId() || this.snapshots[x][this.seaLevel - 2][z].typeId == Material.LAVA.getId() || (this.snapshots[x][this.seaLevel - 2][z].typeId == Material.WATER.getId() || this.snapshots[x][this.seaLevel - 2][z].data != 0))
				{
					this.snapshots[x][this.seaLevel - 2][z].typeId = Material.STONE.getId();
				}
				
				//do the same for one layer beneath that (because a future restoration step may convert surface stone to sand, which falls down)
				if(this.snapshots[x][this.seaLevel - 3][z].typeId == Material.AIR.getId() || this.snapshots[x][this.seaLevel - 3][z].typeId == Material.LAVA.getId() || (this.snapshots[x][this.seaLevel - 3][z].typeId == Material.WATER.getId() || this.snapshots[x][this.seaLevel - 3][z].data != 0))
				{
					this.snapshots[x][this.seaLevel - 3][z].typeId = Material.STONE.getId();
				}
			}
		}
	}
	
	//converts sandstone adjacent to sand to sand, and any other sandstone to air
	private void removeSandstone()
	{
		for(int x = 1; x < snapshots.length - 1; x++)
		{
			for(int z = 1; z < snapshots[0][0].length - 1; z++)
			{
				for(int y = snapshots[0].length - 2; y > miny; y--)
				{
					if(snapshots[x][y][z].typeId != Material.SANDSTONE.getId()) continue;
					
					BlockSnapshot leftBlock = this.snapshots[x + 1][y][z];
					BlockSnapshot rightBlock = this.snapshots[x - 1][y][z];
					BlockSnapshot upBlock = this.snapshots[x][y][z + 1];
					BlockSnapshot downBlock = this.snapshots[x][y][z - 1];
					BlockSnapshot underBlock = this.snapshots[x][y - 1][z];
					BlockSnapshot aboveBlock = this.snapshots[x][y + 1][z];
					
					//skip blocks which may cause a cave-in
					if(aboveBlock.typeId == Material.SAND.getId() && underBlock.typeId == Material.AIR.getId()) continue;
					
					//count adjacent non-air/non-leaf blocks
					if(	leftBlock.typeId == Material.SAND.getId() || 
						rightBlock.typeId == Material.SAND.getId() ||
						upBlock.typeId == Material.SAND.getId() ||
						downBlock.typeId == Material.SAND.getId() ||
						aboveBlock.typeId == Material.SAND.getId() ||
						underBlock.typeId == Material.SAND.getId())
					{
						snapshots[x][y][z].typeId = Material.SAND.getId();
					}
					else
					{
						snapshots[x][y][z].typeId = Material.AIR.getId();
					}					
				}				
			}
		}
	}
	
	private void reduceStone()
	{
		for(int x = 1; x < snapshots.length - 1; x++)
		{
			for(int z = 1; z < snapshots[0][0].length - 1; z++)
			{
				int thisy = this.highestY(x, z, true);
				
				while(thisy > this.seaLevel - 1 && (this.snapshots[x][thisy][z].typeId == Material.STONE.getId() || this.snapshots[x][thisy][z].typeId == Material.SANDSTONE.getId()))
				{
					BlockSnapshot leftBlock = this.snapshots[x + 1][thisy][z];
					BlockSnapshot rightBlock = this.snapshots[x - 1][thisy][z];
					BlockSnapshot upBlock = this.snapshots[x][thisy][z + 1];
					BlockSnapshot downBlock = this.snapshots[x][thisy][z - 1];
					
					//count adjacent non-air/non-leaf blocks
					byte adjacentBlockCount = 0;
					if(leftBlock.typeId != Material.AIR.getId() && leftBlock.typeId != Material.LEAVES.getId() && leftBlock.typeId != Material.VINE.getId())
					{
						adjacentBlockCount++;
					}
					if(rightBlock.typeId != Material.AIR.getId() && rightBlock.typeId != Material.LEAVES.getId() && rightBlock.typeId != Material.VINE.getId())
					{
						adjacentBlockCount++;
					}
					if(downBlock.typeId != Material.AIR.getId() && downBlock.typeId != Material.LEAVES.getId() && downBlock.typeId != Material.VINE.getId())
					{
						adjacentBlockCount++;
					}
					if(upBlock.typeId != Material.AIR.getId() && upBlock.typeId != Material.LEAVES.getId() && upBlock.typeId != Material.VINE.getId())
					{
						adjacentBlockCount++;
					}
					
					if(adjacentBlockCount < 3)
					{
						this.snapshots[x][thisy][z].typeId = Material.AIR.getId();
					}

					thisy--;
				}				
			}
		}
	}
	
	private void reduceLogs()
	{
		boolean jungleBiome = this.biome == Biome.JUNGLE || this.biome == Biome.JUNGLE_HILLS;
		
		//scan all blocks above sea level
		for(int x = 1; x < snapshots.length - 1; x++)
		{
			for(int z = 1; z < snapshots[0][0].length - 1; z++)
			{
				for(int y = this.seaLevel - 1; y < snapshots[0].length; y++)
				{
					BlockSnapshot block = snapshots[x][y][z];
					
					//skip non-logs
					if(block.typeId != Material.LOG.getId()) continue;
					
					//if in jungle biome, skip jungle logs
					if(jungleBiome && block.data == 3) continue;
				
					//examine adjacent blocks for logs
					BlockSnapshot leftBlock = this.snapshots[x + 1][y][z];
					BlockSnapshot rightBlock = this.snapshots[x - 1][y][z];
					BlockSnapshot upBlock = this.snapshots[x][y][z + 1];
					BlockSnapshot downBlock = this.snapshots[x][y][z - 1];
					
					//if any, remove the log
					if(leftBlock.typeId == Material.LOG.getId() || rightBlock.typeId == Material.LOG.getId() || upBlock.typeId == Material.LOG.getId() || downBlock.typeId == Material.LOG.getId())
					{
						this.snapshots[x][y][z].typeId = Material.AIR.getId();
					}
				}				
			}
		}
	}
	
	private void removePlayerBlocks()
	{
		int miny = this.miny;
		if(miny < 1) miny = 1;
		
		//remove all player blocks
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
					
					if(underBlock.typeId == Material.AIR.getId() || underBlock.typeId == Material.STATIONARY_WATER.getId() || underBlock.typeId == Material.STATIONARY_LAVA.getId() || underBlock.typeId == Material.LEAVES.getId())
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
					int thisy = this.highestY(x, z, false);
					if(excludedBlocks.contains(this.snapshots[x][thisy][z].typeId)) continue;
						
					int righty = this.highestY(x + 1, z, false);
					int lefty = this.highestY(x - 1, z, false);
					while(lefty < thisy && righty < thisy)
					{
						this.snapshots[x][thisy--][z].typeId = Material.AIR.getId();
						changed = true;
					}
					
					int upy = this.highestY(x, z + 1, false);
					int downy = this.highestY(x, z - 1, false);
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
				int y = this.highestY(x, z, true);
				BlockSnapshot block = snapshots[x][y][z];
				
				if(block.typeId == Material.STONE.getId() || block.typeId == Material.GRAVEL.getId() || block.typeId == Material.SOIL.getId() || block.typeId == Material.DIRT.getId() || block.typeId == Material.SANDSTONE.getId())
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
		fillableBlocks.add(Material.LONG_GRASS.getId());
		
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
	
	private void removeDumpedFluids()
	{
		//remove any surface water or lava above sea level, presumed to be placed by players
		//sometimes, this is naturally generated.  but replacing it is very easy with a bucket, so overall this is a good plan
		if(this.environment == Environment.NETHER) return;
		for(int x = 1; x < snapshots.length - 1; x++)
		{
			for(int z = 1; z < snapshots[0][0].length - 1; z++)
			{
				for(int y = this.seaLevel - 1; y < snapshots[0].length - 1; y++)
				{
					BlockSnapshot block = snapshots[x][y][z];
					if(block.typeId == Material.STATIONARY_WATER.getId() || block.typeId == Material.STATIONARY_LAVA.getId() ||
					   block.typeId == Material.WATER.getId() || block.typeId == Material.LAVA.getId())
					{
						block.typeId = Material.AIR.getId();
					}
				}
			}
		}
	}
	
	private int highestY(int x, int z, boolean ignoreLeaves)
	{
		int y;
		for(y = snapshots[0].length - 1; y > 0; y--)
		{
			BlockSnapshot block = this.snapshots[x][y][z];
			if(block.typeId != Material.AIR.getId() &&
			!(ignoreLeaves && block.typeId == Material.SNOW.getId()) &&
			!(ignoreLeaves && block.typeId == Material.LEAVES.getId()) &&
			!(block.typeId == Material.STATIONARY_WATER.getId()) &&
			!(block.typeId == Material.WATER.getId()) &&
			!(block.typeId == Material.LAVA.getId()) &&
			!(block.typeId == Material.STATIONARY_LAVA.getId()))
			{
				return y;
			}
		}
		
		return y;
	}
	
	static ArrayList<Integer> getPlayerBlocks(Environment environment, Biome biome) 
	{
		//NOTE on this list.  why not make a list of natural blocks?
		//answer: better to leave a few player blocks than to remove too many natural blocks.  remember we're "restoring nature"
		//a few extra player blocks can be manually removed, but it will be impossible to guess exactly which natural materials to use in manual repair of an overzealous block removal
		ArrayList<Integer> playerBlocks = new ArrayList<Integer>();
		playerBlocks.add(Material.FIRE.getId());
		playerBlocks.add(Material.BED_BLOCK.getId());
		playerBlocks.add(Material.WOOD.getId());
		playerBlocks.add(Material.BOOKSHELF.getId());
		playerBlocks.add(Material.BREWING_STAND.getId());
		playerBlocks.add(Material.BRICK.getId());
		playerBlocks.add(Material.COBBLESTONE.getId());
		playerBlocks.add(Material.GLASS.getId());
		playerBlocks.add(Material.LAPIS_BLOCK.getId());
		playerBlocks.add(Material.DISPENSER.getId());
		playerBlocks.add(Material.NOTE_BLOCK.getId());
		playerBlocks.add(Material.POWERED_RAIL.getId());
		playerBlocks.add(Material.DETECTOR_RAIL.getId());
		playerBlocks.add(Material.PISTON_STICKY_BASE.getId());
		playerBlocks.add(Material.PISTON_BASE.getId());
		playerBlocks.add(Material.PISTON_EXTENSION.getId());
		playerBlocks.add(Material.WOOL.getId());
		playerBlocks.add(Material.PISTON_MOVING_PIECE.getId());
		playerBlocks.add(Material.GOLD_BLOCK.getId());
		playerBlocks.add(Material.IRON_BLOCK.getId());
		playerBlocks.add(Material.DOUBLE_STEP.getId());
		playerBlocks.add(Material.STEP.getId());
		playerBlocks.add(Material.CROPS.getId());
		playerBlocks.add(Material.TNT.getId());
		playerBlocks.add(Material.MOSSY_COBBLESTONE.getId());
		playerBlocks.add(Material.TORCH.getId());
		playerBlocks.add(Material.FIRE.getId());
		playerBlocks.add(Material.WOOD_STAIRS.getId());
		playerBlocks.add(Material.CHEST.getId());
		playerBlocks.add(Material.REDSTONE_WIRE.getId());
		playerBlocks.add(Material.DIAMOND_BLOCK.getId());
		playerBlocks.add(Material.WORKBENCH.getId());
		playerBlocks.add(Material.FURNACE.getId());
		playerBlocks.add(Material.BURNING_FURNACE.getId());
		playerBlocks.add(Material.WOODEN_DOOR.getId());
		playerBlocks.add(Material.SIGN_POST.getId());
		playerBlocks.add(Material.LADDER.getId());
		playerBlocks.add(Material.RAILS.getId());
		playerBlocks.add(Material.COBBLESTONE_STAIRS.getId());
		playerBlocks.add(Material.WALL_SIGN.getId());
		playerBlocks.add(Material.STONE_PLATE.getId());
		playerBlocks.add(Material.LEVER.getId());
		playerBlocks.add(Material.IRON_DOOR_BLOCK.getId());
		playerBlocks.add(Material.WOOD_PLATE.getId());
		playerBlocks.add(Material.REDSTONE_TORCH_ON.getId());
		playerBlocks.add(Material.REDSTONE_TORCH_OFF.getId());
		playerBlocks.add(Material.STONE_BUTTON.getId());
		playerBlocks.add(Material.SNOW_BLOCK.getId());
		playerBlocks.add(Material.JUKEBOX.getId());
		playerBlocks.add(Material.FENCE.getId());
		playerBlocks.add(Material.PORTAL.getId());
		playerBlocks.add(Material.JACK_O_LANTERN.getId());
		playerBlocks.add(Material.CAKE_BLOCK.getId());
		playerBlocks.add(Material.DIODE_BLOCK_ON.getId());
		playerBlocks.add(Material.DIODE_BLOCK_OFF.getId());
		playerBlocks.add(Material.TRAP_DOOR.getId());
		playerBlocks.add(Material.SMOOTH_BRICK.getId());
		playerBlocks.add(Material.HUGE_MUSHROOM_1.getId());
		playerBlocks.add(Material.HUGE_MUSHROOM_2.getId());
		playerBlocks.add(Material.IRON_FENCE.getId());
		playerBlocks.add(Material.THIN_GLASS.getId());
		playerBlocks.add(Material.MELON_STEM.getId());
		playerBlocks.add(Material.FENCE_GATE.getId());
		playerBlocks.add(Material.BRICK_STAIRS.getId());
		playerBlocks.add(Material.SMOOTH_STAIRS.getId());
		playerBlocks.add(Material.ENCHANTMENT_TABLE.getId());
		playerBlocks.add(Material.BREWING_STAND.getId());
		playerBlocks.add(Material.CAULDRON.getId());
		playerBlocks.add(Material.DIODE_BLOCK_ON.getId());
		playerBlocks.add(Material.DIODE_BLOCK_ON.getId());		
		playerBlocks.add(Material.WEB.getId());
		playerBlocks.add(Material.SPONGE.getId());
		playerBlocks.add(Material.GRAVEL.getId());
		playerBlocks.add(Material.EMERALD_BLOCK.getId());
		playerBlocks.add(Material.SANDSTONE.getId());
		playerBlocks.add(Material.WOOD_STEP.getId());
		playerBlocks.add(Material.WOOD_DOUBLE_STEP.getId());
		playerBlocks.add(Material.ENDER_CHEST.getId());
		playerBlocks.add(Material.SANDSTONE_STAIRS.getId());
		playerBlocks.add(Material.SPRUCE_WOOD_STAIRS.getId());
		playerBlocks.add(Material.JUNGLE_WOOD_STAIRS.getId());
		playerBlocks.add(Material.COMMAND.getId());
		playerBlocks.add(Material.BEACON.getId());
		playerBlocks.add(Material.COBBLE_WALL.getId());
		playerBlocks.add(Material.FLOWER_POT.getId());
		playerBlocks.add(Material.CARROT.getId());
		playerBlocks.add(Material.POTATO.getId());
		playerBlocks.add(Material.WOOD_BUTTON.getId());
		playerBlocks.add(Material.SKULL.getId());
		playerBlocks.add(Material.ANVIL.getId());
		
		
		//these are unnatural in the standard world, but not in the nether
		if(environment != Environment.NETHER)
		{
			playerBlocks.add(Material.NETHERRACK.getId());
			playerBlocks.add(Material.SOUL_SAND.getId());
			playerBlocks.add(Material.GLOWSTONE.getId());
			playerBlocks.add(Material.NETHER_BRICK.getId());
			playerBlocks.add(Material.NETHER_FENCE.getId());
			playerBlocks.add(Material.NETHER_BRICK_STAIRS.getId());
		}
		
		//these are unnatural in the standard and nether worlds, but not in the end
		if(environment != Environment.THE_END)
		{
			playerBlocks.add(Material.OBSIDIAN.getId());
			playerBlocks.add(Material.ENDER_STONE.getId());
			playerBlocks.add(Material.ENDER_PORTAL_FRAME.getId());
		}
		
		//these are unnatural in sandy biomes, but not elsewhere
		if(biome == Biome.DESERT || biome == Biome.DESERT_HILLS || biome == Biome.BEACH || environment != Environment.NORMAL)
		{
			playerBlocks.add(Material.LEAVES.getId());
			playerBlocks.add(Material.LOG.getId());
		}
		
		return playerBlocks;
	}
}
