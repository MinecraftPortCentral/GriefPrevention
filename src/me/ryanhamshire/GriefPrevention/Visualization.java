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
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

//represents a visualization sent to a player
//FEATURE: to show players visually where claim boundaries are, we send them fake block change packets
//the result is that those players see new blocks, but the world hasn't been changed.  other players can't see the new blocks, either.
public class Visualization 
{
	public ArrayList<VisualizationElement> elements = new ArrayList<VisualizationElement>();
	
	//sends a visualization to a player
	public static void Apply(Player player, Visualization visualization)
	{
		PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getName());
		
		//if he has any current visualization, clear it first
		if(playerData.currentVisualization != null)
		{
			Visualization.Revert(player);
		}
		
		//if he's online, create a task to send him the visualization in about half a second
		if(player.isOnline())
		{
			GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, new VisualizationApplicationTask(player, playerData, visualization), 10L);
		}
	}
	
	//reverts a visualization by sending another block change list, this time with the real world block values
	public static void Revert(Player player)
	{
		PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getName());
		
		Visualization visualization = playerData.currentVisualization;
		
		if(playerData.currentVisualization != null)
		{
			if(player.isOnline())
			{			
				for(int i = 0; i < visualization.elements.size(); i++)
				{
					VisualizationElement element = visualization.elements.get(i);
					Block block = element.location.getBlock();
					player.sendBlockChange(element.location, block.getType(), block.getData());
				}
			}
			
			playerData.currentVisualization = null;
		}
	}
	
	//convenience method to build a visualization from a claim
	//visualizationType determines the style (gold blocks, silver, red, diamond, etc)
	public static Visualization FromClaim(Claim claim, int height, VisualizationType visualizationType, Location locality)
	{
		//visualize only top level claims
		if(claim.parent != null)
		{
			return FromClaim(claim.parent, height, visualizationType, locality);
		}
		
		Visualization visualization = new Visualization();
		
		//add subdivisions first
		for(int i = 0; i < claim.children.size(); i++)
		{
			visualization.addClaimElements(claim.children.get(i), height, VisualizationType.Subdivision, locality);
		}
		
		//add top level last so that it takes precedence (it shows on top when the child claim boundaries overlap with its boundaries)
		visualization.addClaimElements(claim, height, visualizationType, locality);
		
		return visualization;
	}
	
	//adds a claim's visualization to the current visualization
	//handy for combining several visualizations together, as when visualization a top level claim with several subdivisions inside
	//locality is a performance consideration.  only create visualization blocks for around 100 blocks of the locality
	private void addClaimElements(Claim claim, int height, VisualizationType visualizationType, Location locality)
	{
		Location smallXsmallZ = claim.getLesserBoundaryCorner();
		Location bigXbigZ = claim.getGreaterBoundaryCorner();
		World world = smallXsmallZ.getWorld();
		
		int smallx = smallXsmallZ.getBlockX();
		int smallz = smallXsmallZ.getBlockZ();
		int bigx = bigXbigZ.getBlockX();
		int bigz = bigXbigZ.getBlockZ();
		
		Material cornerMaterial;
		Material accentMaterial;
		
		if(visualizationType == VisualizationType.Claim)
		{
			cornerMaterial = Material.GLOWSTONE;
			accentMaterial = Material.GOLD_BLOCK;
		}
		
		else if(visualizationType == VisualizationType.Subdivision)
		{
			cornerMaterial = Material.IRON_BLOCK;
			accentMaterial = Material.WOOL;
		}
		
		else if(visualizationType == VisualizationType.RestoreNature)
		{
			cornerMaterial = Material.DIAMOND_BLOCK;
			accentMaterial = Material.DIAMOND_BLOCK;
		}
		
		else
		{
			cornerMaterial = Material.GLOWING_REDSTONE_ORE;
			accentMaterial = Material.NETHERRACK;
		}
		
		//bottom left corner
		this.elements.add(new VisualizationElement(getVisibleLocation(world, smallx, height, smallz), cornerMaterial, (byte)0));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, smallx + 1, height, smallz), accentMaterial, (byte)0));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, smallx, height, smallz + 1), accentMaterial, (byte)0));
		
		//bottom right corner
		this.elements.add(new VisualizationElement(getVisibleLocation(world, bigx, height, smallz), cornerMaterial, (byte)0));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, bigx - 1, height, smallz), accentMaterial, (byte)0));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, bigx, height, smallz + 1), accentMaterial, (byte)0));
		
		//top right corner
		this.elements.add(new VisualizationElement(getVisibleLocation(world, bigx, height, bigz), cornerMaterial, (byte)0));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, bigx - 1, height, bigz), accentMaterial, (byte)0));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, bigx, height, bigz - 1), accentMaterial, (byte)0));
		
		//top left corner
		this.elements.add(new VisualizationElement(getVisibleLocation(world, smallx, height, bigz), cornerMaterial, (byte)0));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, smallx + 1, height, bigz), accentMaterial, (byte)0));
		this.elements.add(new VisualizationElement(getVisibleLocation(world, smallx, height, bigz - 1), accentMaterial, (byte)0));
		
		//locality
		int minx = locality.getBlockX() - 100;
		int minz = locality.getBlockZ() - 100;
		int maxx = locality.getBlockX() + 100;
		int maxz = locality.getBlockZ() + 100;
		
		//top line		
		for(int x = smallx + 10; x < bigx - 10; x += 10)
		{
			if(x > minx && x < maxx)
				this.elements.add(new VisualizationElement(getVisibleLocation(world, x, height, bigz), accentMaterial, (byte)0));
		}
		
		//bottom line
		for(int x = smallx + 10; x < bigx - 10; x += 10)
		{
			if(x > minx && x < maxx)
				this.elements.add(new VisualizationElement(getVisibleLocation(world, x, height, smallz), accentMaterial, (byte)0));
		}
		
		//left line
		for(int z = smallz + 10; z < bigz - 10; z += 10)
		{
			if(z > minz && z < maxz)
				this.elements.add(new VisualizationElement(getVisibleLocation(world, smallx, height, z), accentMaterial, (byte)0));
		}
		
		//right line
		for(int z = smallz + 10; z < bigz - 10; z += 10)
		{
			if(z > minz && z < maxz)
				this.elements.add(new VisualizationElement(getVisibleLocation(world, bigx, height, z), accentMaterial, (byte)0));
		}
	}
	
	//finds a block the player can probably see.  this is how visualizations "cling" to the ground or ceiling
	private static Location getVisibleLocation(World world, int x, int y, int z)
	{
		Block block = world.getBlockAt(x,  y, z);
		BlockFace direction = (isTransparent(block)) ? BlockFace.DOWN : BlockFace.UP;
				
		while(	block.getY() >= 1 && 
				block.getY() < world.getMaxHeight() - 1 &&
				(!isTransparent(block.getRelative(BlockFace.UP)) || isTransparent(block)))
		{
			block = block.getRelative(direction);
		}
		
		return block.getLocation();
	}
	
	//helper method for above.  allows visualization blocks to sit underneath partly transparent blocks like grass and fence
	private static boolean isTransparent(Block block)
	{
		return (	block.getType() == Material.AIR ||
					block.getType() == Material.LONG_GRASS ||
					block.getType() == Material.FENCE ||
					block.getType() == Material.LEAVES ||
					block.getType() == Material.RED_ROSE ||
					block.getType() == Material.CHEST ||
					block.getType() == Material.TORCH ||
					block.getType() == Material.VINE ||
					block.getType() == Material.YELLOW_FLOWER );
	}
}
