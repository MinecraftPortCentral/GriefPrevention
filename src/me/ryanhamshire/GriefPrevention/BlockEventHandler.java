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

import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

//event handlers related to blocks
public class BlockEventHandler implements Listener 
{
	//convenience reference to singleton datastore
	private DataStore dataStore;
	
	//boring typical constructor
	public BlockEventHandler(DataStore dataStore)
	{
		this.dataStore = dataStore;
	}
	
	//when a block is damaged...
	@EventHandler(ignoreCancelled = true)
	public void onBlockDamaged(BlockDamageEvent event)
	{
		//if placing items in protected chests isn't enabled, none of this code needs to run
		if(!GriefPrevention.instance.config_addItemsToClaimedChests) return;
		
		Block block = event.getBlock();
		Player player = event.getPlayer(); 
		
		//only care about player-damaged blocks
		if(player == null) return;
		
		//FEATURE: players may add items to a chest they don't have permission for by hitting it
		
		//if it's a chest
		if(block.getType() == Material.CHEST)
		{
			//only care about non-creative mode players, since those would outright break the box in one hit
			if(player.getGameMode() == GameMode.CREATIVE) return;
			
			//only care if the player has an itemstack in hand
			PlayerInventory playerInventory = player.getInventory();
			ItemStack stackInHand = playerInventory.getItemInHand();
			if(stackInHand == null || stackInHand.getType() == Material.AIR) return;
			
			//only care if the chest is in a claim, and the player does not have access to the chest
			Claim claim = this.dataStore.getClaimAt(block.getLocation(), false, null);
			if(claim == null || claim.allowContainers(player) == null) return;
			
			//if the player is under siege, he can't give away items
			PlayerData playerData = this.dataStore.getPlayerData(event.getPlayer().getName());
			if(playerData.siegeData != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You can't give away items while involved in a siege.");
				event.setCancelled(true);
				return;
			}
			
			//NOTE: to eliminate accidental give-aways, first hit on a chest displays a confirmation message
			//subsequent hits donate item to the chest
			
			//if first time damaging this chest, show confirmation message
			if(playerData.lastChestDamageLocation == null || !block.getLocation().equals(playerData.lastChestDamageLocation))
			{
				//remember this location
				playerData.lastChestDamageLocation = block.getLocation();
				
				//give the player instructions
				GriefPrevention.sendMessage(player, TextMode.Instr, "To give away the item(s) in your hand, left-click the chest again.");
			}
			
			//otherwise, try to donate the item stack in hand
			else
			{
				//look for empty slot in chest
				Chest chest = (Chest)block.getState();
				Inventory chestInventory = chest.getInventory();
				int availableSlot = chestInventory.firstEmpty();
				
				//if there isn't one
				if(availableSlot < 0)
				{
					//tell the player and stop here
					GriefPrevention.sendMessage(player, TextMode.Err, "This chest is full.");
					
					return;
				}
				
				//otherwise, transfer item stack from player to chest
				//NOTE: Inventory.addItem() is smart enough to add items to existing stacks, making filling a chest with garbage as a grief very difficult
				chestInventory.addItem(stackInHand);
				playerInventory.setItemInHand(new ItemStack(Material.AIR));
				
				//and confirm for the player
				GriefPrevention.sendMessage(player, TextMode.Success, "Item(s) transferred to chest!");
			}
		}
	}
	
	//when a player breaks a block...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockBreak(BlockBreakEvent breakEvent)
	{
		Player player = breakEvent.getPlayer();
		Block block = breakEvent.getBlock();		
		
		//make sure the player is allowed to break at the location
		String noBuildReason = GriefPrevention.instance.allowBreak(player, block.getLocation());
		if(noBuildReason != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
			breakEvent.setCancelled(true);
			return;
		}
		
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		Claim claim = this.dataStore.getClaimAt(block.getLocation(), true, playerData.lastClaim);
		
		//if there's a claim here
		if(claim != null)
		{
			//if breaking UNDER the claim
			if(block.getY() < claim.lesserBoundaryCorner.getBlockY())
			{
				//extend the claim downward beyond the breakage point
				this.dataStore.extendClaim(claim, claim.getLesserBoundaryCorner().getBlockY() - GriefPrevention.instance.config_claims_claimsExtendIntoGroundDistance);
			}
		}
		
		//FEATURE: automatically clean up hanging treetops
		//if it's a log
		if(block.getType() == Material.LOG && GriefPrevention.instance.config_trees_removeFloatingTreetops)
		{
			//run the specialized code for treetop removal (see below)
			GriefPrevention.instance.handleLogBroken(block);
		}
	}
	
	//when a player places a block...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockPlace(BlockPlaceEvent placeEvent)
	{
		Player player = placeEvent.getPlayer();
		Block block = placeEvent.getBlock();
		
		//FEATURE: limit fire placement, to prevent PvP-by-fire
		
		//if placed block is fire and pvp is off, apply rules for proximity to other players 
		if(block.getType() == Material.FIRE && !block.getWorld().getPVP() && !player.hasPermission("griefprevention.lava"))
		{
			List<Player> players = block.getWorld().getPlayers();
			for(int i = 0; i < players.size(); i++)
			{
				Player otherPlayer = players.get(i);
				Location location = otherPlayer.getLocation();
				if(!otherPlayer.equals(player) && location.distanceSquared(block.getLocation()) < 9)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, "You can't start a fire this close to " + otherPlayer.getName() + ".");
					placeEvent.setCancelled(true);
					return;
				}					
			}
		}
		
		//make sure the player is allowed to build at the location
		String noBuildReason = GriefPrevention.instance.allowBuild(player, block.getLocation());
		if(noBuildReason != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
			placeEvent.setCancelled(true);
			return;
		}
		
		//if the block is being placed within an existing claim
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		Claim claim = this.dataStore.getClaimAt(block.getLocation(), true, playerData.lastClaim);
		if(claim != null)
		{
			//if the player has permission for the claim and he's placing UNDER the claim
			if(block.getY() < claim.lesserBoundaryCorner.getBlockY())
			{
				//extend the claim downward
				this.dataStore.extendClaim(claim, claim.getLesserBoundaryCorner().getBlockY() - GriefPrevention.instance.config_claims_claimsExtendIntoGroundDistance);
			}
		}
		
		//FEATURE: automatically create a claim when a player who has no claims places a chest
		
		//otherwise if there's no claim, the player is placing a chest, and new player automatic claims are enabled
		else if(block.getType() == Material.CHEST && GriefPrevention.instance.config_claims_automaticClaimsForNewPlayersRadius > -1 && GriefPrevention.instance.claimsEnabledForWorld(block.getWorld()))
		{			
			//if the chest is too deep underground, don't create the claim and explain why
			if(GriefPrevention.instance.config_claims_preventTheft && block.getY() < GriefPrevention.instance.config_claims_maxDepth)
			{
				GriefPrevention.sendMessage(player, TextMode.Warn, "This chest can't be protected because it's too deep underground.  Consider moving it.");
				return;
			}
			
			int radius = GriefPrevention.instance.config_claims_automaticClaimsForNewPlayersRadius;
			
			//if the player doesn't have any claims yet, automatically create a claim centered at the chest
			if(playerData.claims.size() == 0)
			{
				//radius == 0 means protect ONLY the chest
				if(GriefPrevention.instance.config_claims_automaticClaimsForNewPlayersRadius == 0)
				{					
					this.dataStore.createClaim(block.getWorld(), block.getX(), block.getX(), block.getY(), block.getY(), block.getZ(), block.getZ(), player.getName(), null);
					GriefPrevention.sendMessage(player, TextMode.Success, "This chest is protected.");						
				}
				
				//otherwise, create a claim in the area around the chest
				else
				{
					//as long as the automatic claim overlaps another existing claim, shrink it
					//note that since the player had permission to place the chest, at the very least, the automatic claim will include the chest
					while(radius >= 0 && !this.dataStore.createClaim(block.getWorld(), 
							block.getX() - radius, block.getX() + radius, 
							block.getY() - GriefPrevention.instance.config_claims_claimsExtendIntoGroundDistance, block.getY(), 
							block.getZ() - radius, block.getZ() + radius, 
							player.getName(), 
							null).succeeded)
					{
						radius--;
					}
					
					//notify and explain to player
					GriefPrevention.sendMessage(player, TextMode.Success, "This chest and nearby blocks are protected from breakage and theft.  The gold and glowstone blocks mark the protected area.");
					
					//show the player the protected area
					Claim newClaim = this.dataStore.getClaimAt(block.getLocation(), false, null);
					Visualization visualization = Visualization.FromClaim(newClaim, block.getY(), VisualizationType.Claim);
					Visualization.Apply(player, visualization);
				}
				
				//instructions for using /trust
				GriefPrevention.sendMessage(player, TextMode.Instr, "Use the /trust command to grant other players access.");
				
				//unless special permission is required to create a claim with the shovel, educate the player about the shovel
				if(!GriefPrevention.instance.config_claims_creationRequiresPermission)
				{
					GriefPrevention.sendMessage(player, TextMode.Instr, "To claim more land, use a golden shovel.");
				}
			}
			
			//check to see if this chest is in a claim, and warn when it isn't
			if(GriefPrevention.instance.config_claims_preventTheft && this.dataStore.getClaimAt(block.getLocation(), false, playerData.lastClaim) == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Warn, "This chest is NOT protected.  Consider expanding an existing claim or creating a new one.");				
			}
		}
	}
	
	//blocks "pushing" other players' blocks around (pistons)
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockPistonExtend (BlockPistonExtendEvent event)
	{		
		//who owns the piston, if anyone?
		String pistonClaimOwnerName = "_";
		Claim claim = this.dataStore.getClaimAt(event.getBlock().getLocation(), false, null);
		if(claim != null) pistonClaimOwnerName = claim.getOwnerName();
		
		//which blocks are being pushed?
		List<Block> blocks = event.getBlocks();
		for(int i = 0; i < blocks.size(); i++)
		{
			//if ANY of the pushed blocks are owned by someone other than the piston owner, cancel the event
			Block block = blocks.get(i);
			claim = this.dataStore.getClaimAt(block.getLocation(), false, null);
			if(claim != null && !claim.getOwnerName().equals(pistonClaimOwnerName))
			{
				event.setCancelled(true);
				return;
			}
		}
	}
	
	//blocks theft by pulling blocks out of a claim (again pistons)
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockPistonRetract (BlockPistonRetractEvent event)
	{
		//we only care about sticky pistons
		if(!event.isSticky()) return;
				
		//who owns the moving block, if anyone?
		String movingBlockOwnerName = "_";
		Claim movingBlockClaim = this.dataStore.getClaimAt(event.getRetractLocation(), false, null);
		if(movingBlockClaim != null) movingBlockOwnerName = movingBlockClaim.getOwnerName();
		
		//who owns the piston, if anyone?
		String pistonOwnerName = "_";
		Location pistonLocation = event.getBlock().getLocation();		
		Claim pistonClaim = this.dataStore.getClaimAt(pistonLocation, false, null);
		if(pistonClaim != null) pistonOwnerName = pistonClaim.getOwnerName();
		
		//if there are owners for the blocks, they must be the same player
		//otherwise cancel the event
		if(!pistonOwnerName.equals(movingBlockOwnerName))
		{
			event.setCancelled(true);
		}		
	}
	
	//blocks are ignited ONLY by flint and steel (not by being near lava, open flames, etc), unless configured otherwise
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockIgnite (BlockIgniteEvent igniteEvent)
	{
		if(igniteEvent.getCause() != IgniteCause.FLINT_AND_STEEL  && !GriefPrevention.instance.config_fireSpreads) igniteEvent.setCancelled(true);
	}
	
	//fire doesn't spread unless configured to, but other blocks still do (mushrooms and vines, for example)
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockSpread (BlockSpreadEvent spreadEvent)
	{
		if(spreadEvent.getSource().getType() == Material.FIRE && !GriefPrevention.instance.config_fireSpreads) spreadEvent.setCancelled(true);
	}
	
	//blocks are not destroyed by fire, unless configured to do so
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onBlockBurn (BlockBurnEvent burnEvent)
	{
		if(!GriefPrevention.instance.config_fireDestroys)
		{
			burnEvent.setCancelled(true);
		}
		
		//never burn claimed blocks, regardless of settings
		if(this.dataStore.getClaimAt(burnEvent.getBlock().getLocation(), false, null) != null)
		{
			burnEvent.setCancelled(true);
		}
	}
	
	//ensures fluids don't flow into claims, unless out of another claim where the owner is trusted to build in the receiving claim
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onBlockFromTo (BlockFromToEvent spreadEvent)
	{
		//where to?
		Block toBlock = spreadEvent.getToBlock();
		Claim toClaim = this.dataStore.getClaimAt(toBlock.getLocation(), false, null);
		
		//if in a creative world, block any spread into the wilderness
		if(GriefPrevention.instance.creativeRulesApply(toBlock.getLocation()) && toClaim == null)
		{
			spreadEvent.setCancelled(true);
			return;
		}
		
		//if spreading into a claim
		if(toClaim != null)
		{		
			//from where?
			Block fromBlock = spreadEvent.getBlock();
			Claim fromClaim = this.dataStore.getClaimAt(fromBlock.getLocation(), false, null);
						
			//who owns the spreading block, if anyone?
			OfflinePlayer fromOwner = null;			
			if(fromClaim != null)
			{
				//if it's within the same claim, allow it
				if(fromClaim == toClaim) return;				
				
				fromOwner = GriefPrevention.instance.getServer().getOfflinePlayer(fromClaim.ownerName);
			}
			
			//cancel unless the owner of the spreading block is allowed to build in the receiving claim
			if(fromOwner == null || fromOwner.getPlayer() == null || toClaim.allowBuild(fromOwner.getPlayer()) != null)
			{
				spreadEvent.setCancelled(true);
			}
		}
	}
	
	@EventHandler(ignoreCancelled = true)
	public void onTreeGrow (StructureGrowEvent growEvent)
	{
		Location rootLocation = growEvent.getLocation();
		Claim rootClaim = this.dataStore.getClaimAt(rootLocation, false, null);
		
		//who owns the root, if anyone?
		//who owns the spreading block, if anyone?
		OfflinePlayer fromOwner = null;			
		if(rootClaim != null)
		{
			//if an administrative claim, just let the tree grow where it wants
			if(rootClaim.isAdminClaim()) return;
			
			//otherwise, note the owner of the claim
			fromOwner = GriefPrevention.instance.getServer().getOfflinePlayer(rootClaim.ownerName);
		}
		
		//for each block growing
		for(int i = 0; i < growEvent.getBlocks().size(); i++)
		{
			BlockState block = growEvent.getBlocks().get(i);
			Claim blockClaim = this.dataStore.getClaimAt(block.getLocation(), false, rootClaim);
			
			//if it's growing into a claim
			if(blockClaim != null)
			{
				//if there's no owner for the new tree, or the owner doesn't have permission to build in the claim, don't grow this block
				if(fromOwner == null  || fromOwner.getPlayer() == null || blockClaim.allowBuild(fromOwner.getPlayer()) != null)
				{
					growEvent.getBlocks().remove(i--);
				}
			}
		}
	}
}
