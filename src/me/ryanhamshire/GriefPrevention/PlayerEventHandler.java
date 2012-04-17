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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.PoweredMinecart;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

class PlayerEventHandler implements Listener 
{
	private DataStore dataStore;
	
	//typical constructor, yawn
	PlayerEventHandler(DataStore dataStore, GriefPrevention plugin)
	{
		this.dataStore = dataStore;
	}
	
	//when a player chats, monitor for spam
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	void onPlayerChat (PlayerChatEvent event)
	{		
		Player player = event.getPlayer();
		String message = event.getMessage();
		
		//FEATURE: automatically educate players about the /trapped command
		
		//check for "trapped" or "stuck" to educate players about the /trapped command
		if(message.contains("trapped") || message.contains("stuck"))
		{
			GriefPrevention.sendMessage(player, TextMode.Info, "Are you trapped in someone's claim?  Consider the /trapped command.");
		}
		
		//FEATURE: monitor for chat and command spam
		
		if(!GriefPrevention.instance.config_spam_enabled) return;
		
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		
		boolean spam = false;
		
		//filter IP addresses
		if(!(event instanceof PlayerCommandPreprocessEvent))
		{
			Pattern ipAddressPattern = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+");
			Matcher matcher = ipAddressPattern.matcher(event.getMessage());
			//if it looks like an IP address
			while(matcher.find())
			{
				//and it's not in the list of allowed IP addresses
				if(!GriefPrevention.instance.config_spam_allowedIpAddresses.contains(matcher.group()))
				{
					//log entry
					GriefPrevention.AddLogEntry("Muted IP address from " + player.getName() + ": " + event.getMessage());
					
					//spam notation
					playerData.spamCount++;
					spam = true;
					
					//block message
					event.setCancelled(true);
				}
			}
		}
		
		//check message content and timing		
		long millisecondsSinceLastMessage = (new Date()).getTime() - playerData.lastMessageTimestamp.getTime();
		
		//if the message came too close to the last one
		if(millisecondsSinceLastMessage < 3000)
		{
			//increment the spam counter
			playerData.spamCount++;
			spam = true;
		}
		
		//if it's the same as the last message
		if(message.equals(playerData.lastMessage))
		{
			playerData.spamCount++;
			spam = true;
		}
		
		//if the message was mostly non-alpha-numerics, consider it a spam (probably ansi art) 
		if(message.length() > 5)
		{
			int symbolsCount = 0;
			for(int i = 0; i < message.length(); i++)
			{
				char character = message.charAt(i);
				if(!(Character.isLetterOrDigit(character) || Character.isWhitespace(character)))
				{
					symbolsCount++;
				}				
			}
			
			if(symbolsCount > message.length() / 2)
			{
				spam = true;
				playerData.spamCount++;
			}
		}
		
		//if the message was determined to be a spam, consider taking action		
		if(!player.hasPermission("griefprevention.spam") && spam)
		{		
			//at the fifth spam level, auto-ban (if enabled)
			if(playerData.spamCount > 4)
			{
				event.setCancelled(true);
				GriefPrevention.AddLogEntry("Muted spam from " + player.getName() + ": " + message);
				
				if(GriefPrevention.instance.config_spam_banOffenders)
				{
					//log entry
					GriefPrevention.AddLogEntry("Banning " + player.getName() + " for spam.");
					
					//ban
					GriefPrevention.instance.getServer().getOfflinePlayer(player.getName()).setBanned(true);
					
					//kick
					player.kickPlayer(GriefPrevention.instance.config_spam_banMessage);
				}				
			}
			
			//cancel any messages while at or above the third spam level and issue warnings
			else if(playerData.spamCount >= 3)
			{
				GriefPrevention.sendMessage(player, TextMode.Warn, GriefPrevention.instance.config_spam_warningMessage);
				event.setCancelled(true);
				GriefPrevention.AddLogEntry("Muted spam from " + player.getName() + ".");
			}
		}
		
		//otherwise if not a spam, reset the spam counter for this player
		else
		{
			playerData.spamCount = 0;
		}
		
		//in any case, record the timestamp of this message and also its content for next time
		playerData.lastMessageTimestamp = new Date();
		playerData.lastMessage = message;	
	}
	
	//when a player uses a slash command, monitor for spam
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	void onPlayerCommandPreprocess (PlayerCommandPreprocessEvent event)
	{
		if(!GriefPrevention.instance.config_spam_enabled) return;
		
		//if the slash command used is in the list of monitored commands, treat it like a chat message (see above)
		String [] args = event.getMessage().split(" ");
		if(GriefPrevention.instance.config_spam_monitorSlashCommands.contains(args[0])) this.onPlayerChat(event);
	}
	
	//when a player attempts to join the server...
	@EventHandler(ignoreCancelled = true)
	void onPlayerLogin (PlayerLoginEvent event)
	{
		if(!GriefPrevention.instance.config_spam_enabled) return;
		
		Player player = event.getPlayer();
		
		//FEATURE: login cooldown to prevent login/logout spam with custom clients
		
		//if allowed to join and login cooldown enabled
		if(GriefPrevention.instance.config_spam_loginCooldownMinutes > 0 && event.getResult() == Result.ALLOWED)
		{
			//determine how long since last login and cooldown remaining
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			long millisecondsSinceLastLogin = (new Date()).getTime() - playerData.lastLogin.getTime();
			long minutesSinceLastLogin = millisecondsSinceLastLogin / 1000 / 60;
			long cooldownRemaining = GriefPrevention.instance.config_spam_loginCooldownMinutes - minutesSinceLastLogin;
			
			//if cooldown remaining and player doesn't have permission to spam
			if(cooldownRemaining > 0 && !player.hasPermission("griefprevention.spam"))
			{
				//DAS BOOT!
				event.setResult(Result.KICK_OTHER);				
				event.setKickMessage("You must wait " + cooldownRemaining + " more minutes before logging-in again.");
				event.disallow(event.getResult(), event.getKickMessage());
			}
		}
	}
	
	//when a player spawns, conditionally apply temporary pvp protection 
	@EventHandler(ignoreCancelled = true)
	void onPlayerRespawn (PlayerRespawnEvent event)
	{
		PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(event.getPlayer().getName());
		playerData.lastSpawn = Calendar.getInstance().getTimeInMillis();
		GriefPrevention.instance.checkPvpProtectionNeeded(event.getPlayer());
	}
	
	//when a player successfully joins the server...
	@EventHandler(ignoreCancelled = true)
	void onPlayerJoin(PlayerJoinEvent event)
	{
		String playerName = event.getPlayer().getName();
		
		//note login time
		PlayerData playerData = this.dataStore.getPlayerData(playerName);
		playerData.lastSpawn = Calendar.getInstance().getTimeInMillis();
		playerData.lastLogin = new Date();
		this.dataStore.savePlayerData(playerName, playerData);
		
		//check inventory, may need pvp protection
		GriefPrevention.instance.checkPvpProtectionNeeded(event.getPlayer());
	}
	
	//when a player quits...
	@EventHandler
	void onPlayerQuit(PlayerQuitEvent event)
	{
		this.onPlayerDisconnect(event.getPlayer());
	}
	
	//when a player gets kicked...
	@EventHandler(ignoreCancelled = true)
	void onPlayerKicked(PlayerKickEvent event)
	{
		this.onPlayerDisconnect(event.getPlayer());
	}
	
	//helper for above
	private void onPlayerDisconnect(Player player)
	{
		String playerName = player.getName();
		PlayerData playerData = this.dataStore.getPlayerData(playerName);
		
		//FEATURE: players in pvp combat when they log out will die
		if(GriefPrevention.instance.config_pvp_punishLogout && playerData.inPvpCombat())
		{
			player.setHealth(0);
		}
		
		//FEATURE: on logout or kick, give player the any claim blocks he may have earned for this play session
		//NOTE: not all kicks are bad, for example an AFK kick or a kick to make room for an admin to log in
		//that's why even kicked players get their claim blocks
		
		//FEATURE: during a siege, any player who logs out dies and forfeits the siege
		
		//if player was involved in a siege, he forfeits
		if(playerData.siegeData != null)
		{
			player.setHealth(0);
			this.dataStore.endSiege(playerData.siegeData, null, player.getName());
		}
		
		//disable ignore claims mode
		playerData.ignoreClaims = false;
	}

	//when a player drops an item
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDropItem(PlayerDropItemEvent event)
	{
		Player player = event.getPlayer();
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		
		//FEATURE: players under siege or in PvP combat, can't throw items on the ground to hide 
		//them or give them away to other players before they are defeated
		
		//if in combat, don't let him drop it
		if(playerData.inPvpCombat())
		{
			GriefPrevention.sendMessage(player, TextMode.Err, "You can't drop items while in PvP combat.");
			event.setCancelled(true);			
		}
		
		//if he's under siege, don't let him drop it
		else if(playerData.siegeData != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, "You can't drop items while involved in a siege.");
			event.setCancelled(true);
		}
	}
	
	//when a player teleports
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerTeleport(PlayerTeleportEvent event)
	{
		//FEATURE: prevent teleport abuse to win sieges
		
		//these rules only apply to non-ender-pearl teleportation
		if(event.getCause() == TeleportCause.ENDER_PEARL) return;
		
		Player player = event.getPlayer();
		
		Location source = event.getFrom();
		Claim sourceClaim = this.dataStore.getClaimAt(source, false, null);
		if(sourceClaim != null && sourceClaim.siegeData != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, "You can't teleport out of a besieged area.");
			event.setCancelled(true);
			return;
		}
		
		Location destination = event.getTo();
		Claim destinationClaim = this.dataStore.getClaimAt(destination, false, null);
		if(destinationClaim != null && destinationClaim.siegeData != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, "You can't teleport into a besieged area.");
			event.setCancelled(true);
			return;
		}
	}
	
	//when a player interacts with an entity...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event)
	{
		Player player = event.getPlayer();
		Entity entity = event.getRightClicked();
		
		//don't allow container access during pvp combat
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		if((entity instanceof StorageMinecart || entity instanceof PoweredMinecart))
		{
			if(playerData.siegeData != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You can't access containers while under siege.");
				event.setCancelled(true);
				return;
			}
			
			if(playerData.inPvpCombat())
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You can't access containers during PvP combat.");
				event.setCancelled(true);
				return;
			}			
		}
		
		//if the entity is a vehicle and we're preventing theft in claims		
		if(GriefPrevention.instance.config_claims_preventTheft && entity instanceof Vehicle)
		{
			//if the entity is in a claim
			Claim claim = this.dataStore.getClaimAt(entity.getLocation(), false, null);
			if(claim != null)
			{
				//for storage and powered minecarts, apply container rules (this is a potential theft)
				if(entity instanceof StorageMinecart || entity instanceof PoweredMinecart)
				{					
					String noContainersReason = claim.allowContainers(player);
					if(noContainersReason != null)
					{
						GriefPrevention.sendMessage(player, TextMode.Err, noContainersReason);
						event.setCancelled(true);
					}
				}
				
				//for boats, apply access rules
				else if(entity instanceof Boat)
				{
					String noAccessReason = claim.allowAccess(player);
					if(noAccessReason != null)
					{
						player.sendMessage(noAccessReason);
						event.setCancelled(true);
					}
				}
				
				//if the entity is an animal, apply container rules
				else if(entity instanceof Animals)
				{
					if(claim.allowContainers(player) != null)
					{
						GriefPrevention.sendMessage(player, TextMode.Err, "That animal belongs to " + claim.getOwnerName() + ".");
						event.setCancelled(true);
					}
				}
			}
		}
	}
	
	//when a player picks up an item...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPlayerPickupItem(PlayerPickupItemEvent event)
	{
		Player player = event.getPlayer();
		
		if(!event.getPlayer().getWorld().getPVP()) return;
		
		//if we're preventing spawn camping and the player was previously empty handed...
		if(GriefPrevention.instance.config_pvp_protectFreshSpawns && (player.getItemInHand().getType() == Material.AIR))
		{
			//if that player is currently immune to pvp
			PlayerData playerData = this.dataStore.getPlayerData(event.getPlayer().getName());
			if(playerData.pvpImmune)
			{
				//if it's been less than 10 seconds since the last time he spawned, don't pick up the item
				long now = Calendar.getInstance().getTimeInMillis();
				long elapsedSinceLastSpawn = now - playerData.lastSpawn;
				if(elapsedSinceLastSpawn < 10000)
				{
					event.setCancelled(true);
					return;
				}
				
				//otherwise take away his immunity. he may be armed now.  at least, he's worth killing for some loot
				playerData.pvpImmune = false;
				GriefPrevention.sendMessage(player, TextMode.Warn, "Now you can fight with other players.");
			}			
		}
	}
	
	//when a player switches in-hand items
	@EventHandler(ignoreCancelled = true)
	public void onItemHeldChange(PlayerItemHeldEvent event)
	{
		Player player = event.getPlayer();
		
		//if he's switching to the golden shovel
		ItemStack newItemStack = player.getInventory().getItem(event.getNewSlot());
		if(newItemStack != null && newItemStack.getType() == Material.GOLD_SPADE)
		{
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			
			//reset any work he might have been doing
			playerData.lastShovelLocation = null;
			playerData.claimResizing = null;
			
			//always reset to basic claims mode
			if(playerData.shovelMode != ShovelMode.Basic)
			{			
				playerData.shovelMode = ShovelMode.Basic;
				GriefPrevention.sendMessage(player, TextMode.Info, "Shovel returned to basic claims mode.");
			}
			
			int remainingBlocks = playerData.getRemainingClaimBlocks();
			
			//if he doesn't have enough blocks to create a new claim, tell him so and offer advice
			if(remainingBlocks < GriefPrevention.instance.config_claims_minSize * GriefPrevention.instance.config_claims_minSize)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You don't have enough available claim blocks to create a new claim (each new claim must be at least " + GriefPrevention.instance.config_claims_minSize + " x " + GriefPrevention.instance.config_claims_minSize + ").  Consider /AbandonClaim to delete an existing claim.");
				return;
			}
			
			//otherwise instruct him in the steps to create a claim
			else
			{
				GriefPrevention.sendMessage(player, TextMode.Instr, "To start creating a claim, right-click at one corner of the claim area.  You may claim up to " + String.valueOf(remainingBlocks) + " more blocks.");
				GriefPrevention.sendMessage(player, TextMode.Instr, "Need a demonstration?  Watch the \"Grief Prevention Basics\" YouTube video.");
			}
		}
	}
	
	//block players from entering beds they don't have permission for
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPlayerBedEnter (PlayerBedEnterEvent bedEvent)
	{
		if(!GriefPrevention.instance.config_claims_preventButtonsSwitches) return;
		
		Player player = bedEvent.getPlayer();
		Block block = bedEvent.getBed();
		
		//if the bed is in a claim 
		Claim claim = this.dataStore.getClaimAt(block.getLocation(), false, null);
		if(claim != null)
		{
			//if the player doesn't have access in that claim, tell him so and prevent him from sleeping in the bed
			if(claim.allowAccess(player) != null)
			{
				bedEvent.setCancelled(true);
				GriefPrevention.sendMessage(player, TextMode.Err, claim.getOwnerName() + " hasn't given you permission to sleep here.");
			}
		}		
	}
	
	//block use of buckets within other players' claims
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPlayerBucketEmpty (PlayerBucketEmptyEvent bucketEvent)
	{
		Player player = bucketEvent.getPlayer();
		Block block = bucketEvent.getBlockClicked().getRelative(bucketEvent.getBlockFace());
		int minLavaDistance = 10;
		
		//if the bucket is being used in a claim
		Claim claim = this.dataStore.getClaimAt(block.getLocation(), false, null);
		if(claim != null)
		{
			//the player must have build permission to use it
			if(claim.allowBuild(player) != null)
			{
				bucketEvent.setCancelled(true);
				GriefPrevention.sendMessage(player, TextMode.Err, "You don't have " + claim.getOwnerName() + "'s permission to use your bucket here.");
				return;
			}
			
			//the claim must be at least an hour old
			long now = Calendar.getInstance().getTimeInMillis();
			long lastModified = claim.modifiedDate.getTime();
			long elapsed = now - lastModified;
			if(bucketEvent.getBucket() == Material.LAVA_BUCKET && !player.hasPermission("griefprevention.lava") && elapsed < 1000 * 60 * 60)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You can't dump lava here because this claim was recently modified.  Try again later.");
				bucketEvent.setCancelled(true);
			}
			
			minLavaDistance = 3;			
		}
		
		//otherwise it must be underground
		else
		{
			if(bucketEvent.getBucket() == Material.LAVA_BUCKET && block.getY() >= block.getWorld().getSeaLevel() - 5 && !player.hasPermission("griefprevention.lava"))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You may only dump lava inside your claim(s) or underground.");
				bucketEvent.setCancelled(true);
			}			
		}
		
		//lava buckets can't be dumped near other players unless pvp is on
		if(!block.getWorld().getPVP() && !player.hasPermission("griefprevention.lava"))
		{
			if(bucketEvent.getBucket() == Material.LAVA_BUCKET)
			{
				List<Player> players = block.getWorld().getPlayers();
				for(int i = 0; i < players.size(); i++)
				{
					Player otherPlayer = players.get(i);
					Location location = otherPlayer.getLocation();
					if(!otherPlayer.equals(player) && block.getY() >= location.getBlockY() - 1 && location.distanceSquared(block.getLocation()) < minLavaDistance * minLavaDistance)
					{
						GriefPrevention.sendMessage(player, TextMode.Err, "You can't place lava this close to " + otherPlayer.getName() + ".");
						bucketEvent.setCancelled(true);
						return;
					}					
				}
			}
		}
	}
	
	//see above
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onPlayerBucketFill (PlayerBucketFillEvent bucketEvent)
	{
		Player player = bucketEvent.getPlayer();
		Block block = bucketEvent.getBlockClicked();
		
		Claim claim = this.dataStore.getClaimAt(block.getLocation(), false, null);
		if(claim != null)
		{
			if(claim.allowBuild(player) != null)
			{
				bucketEvent.setCancelled(true);
				GriefPrevention.sendMessage(player, TextMode.Err, "You don't have permission to use your bucket here.");
			}
		}		
	}
	
	//when a player interacts with the world
	@EventHandler(priority = EventPriority.HIGHEST)
	void onPlayerInteract(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		
		//determine target block.  FEATURE: shovel and string can be used from a distance away
		Block clickedBlock = null;
		
		try
		{
			clickedBlock = event.getClickedBlock();  //null returned here means interacting with air			
			if(clickedBlock == null)
			{
				//try to find a far away non-air block along line of sight
				clickedBlock = player.getTargetBlock(null, 250);
			}			
		}
		catch(Exception e)  //an exception intermittently comes from getTargetBlock().  when it does, just ignore the event
		{
			return;
		}
		
		//if no block, stop here
		if(clickedBlock == null)
		{
			return;
		}
		
		Material clickedBlockType = clickedBlock.getType();
		
		//apply rules for buttons and switches
		if(GriefPrevention.instance.config_claims_preventButtonsSwitches && (clickedBlockType == Material.STONE_BUTTON || clickedBlockType == Material.LEVER))
		{
			Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, null);
			if(claim != null)
			{
				String noAccessReason = claim.allowAccess(player);
				if(noAccessReason != null)
				{
					event.setCancelled(true);
					GriefPrevention.sendMessage(player, TextMode.Err, noAccessReason);
				}
			}			
		}
		
		//otherwise apply rules for containers and crafting blocks
		else if(	GriefPrevention.instance.config_claims_preventTheft && (
						event.getAction() == Action.RIGHT_CLICK_BLOCK && (
						clickedBlock.getState() instanceof InventoryHolder || 
						clickedBlockType == Material.BREWING_STAND || 
						clickedBlockType == Material.WORKBENCH || 
						clickedBlockType == Material.JUKEBOX || 
						clickedBlockType == Material.ENCHANTMENT_TABLE)))
		{			
			//block container use while under siege, so players can't hide items from attackers
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			if(playerData.siegeData != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You can't access containers while involved in a siege.");
				event.setCancelled(true);
				return;
			}
			
			//block container use during pvp combat, same reason
			if(playerData.inPvpCombat())
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You can't access containers during PvP combat.");
				event.setCancelled(true);
				return;
			}
			
			//otherwise check permissions for the claim the player is in
			Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, null);
			if(claim != null)
			{
				String noContainersReason = claim.allowContainers(player);
				if(noContainersReason != null)
				{
					event.setCancelled(true);
					GriefPrevention.sendMessage(player, TextMode.Err, noContainersReason);
				}
			}
			
			//if the event hasn't been cancelled, then the player is allowed to use the container
			//so drop any pvp protection
			if(playerData.pvpImmune)
			{
				playerData.pvpImmune = false;
				GriefPrevention.sendMessage(player, TextMode.Warn, "Now you can fight with other players.");
			}
		}
		
		//apply rule for players trampling tilled soil back to dirt (never allow it)
		//NOTE: that this event applies only to players.  monsters and animals can still trample.
		else if(event.getAction() == Action.PHYSICAL && clickedBlockType == Material.SOIL)
		{
			event.setCancelled(true);
		}
		
		//otherwise handle right click (shovel, string, bonemeal)
		else
		{
			//ignore all actions except right-click on a block or in the air
			Action action = event.getAction();
			if(action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) return;
			
			//what's the player holding?
			Material materialInHand = player.getItemInHand().getType();		
			
			//if it's bonemeal, check for build permission (ink sac == bone meal, must be a Bukkit bug?)
			if(materialInHand == Material.INK_SACK)
			{
				Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, null);
				if(claim == null) return;
				
				String noBuildReason = claim.allowBuild(player); 
				if(claim != null && noBuildReason != null)
				{
					player.sendMessage(noBuildReason);
					event.setCancelled(true);					
				}
				
				return;
			}
			
			//if it's a string, he's investigating a claim			
			else if(materialInHand == Material.STRING)
			{
				//air indicates too far away
				if(clickedBlockType == Material.AIR)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, "That's too far away.");
					return;
				}
				
				Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false /*ignore height*/, null);
				
				//no claim case
				if(claim == null)
				{
					GriefPrevention.sendMessage(player, TextMode.Info, "No one has claimed this block.");
					Visualization.Revert(player);
				}
				
				//claim case
				else
				{
					GriefPrevention.sendMessage(player, TextMode.Info, "This block has been claimed by " + claim.getOwnerName() + ".");
					
					//visualize boundary
					Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.Claim);
					Visualization.Apply(player, visualization);
				}
				
				return;
			}
			
			//if it's a golden shovel
			else if(materialInHand != Material.GOLD_SPADE) return;
			
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			
			//disable golden shovel while under siege
			if(playerData.siegeData != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You can't use your shovel tool while involved in a siege.");
				event.setCancelled(true);
				return;
			}
			
			//can't use the shovel from too far away
			if(clickedBlockType == Material.AIR)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "That's too far away!");
				return;
			}
			
			//if the player is in restore nature mode, do only that
			String playerName = player.getName();
			playerData = this.dataStore.getPlayerData(player.getName());
			if(playerData.shovelMode == ShovelMode.RestoreNature)
			{
				//if the clicked block is in a claim, visualize that claim and deliver an error message
				Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), true, playerData.lastClaim);
				if(claim != null)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, claim.getOwnerName() + " claimed that block.");
					Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.ErrorClaim);
					Visualization.Apply(player, visualization);
					
					return;
				}
				
				//figure out which chunk to regen
				Chunk chunk = player.getWorld().getChunkAt(clickedBlock.getLocation());
				
				//check it for players, and cancel if there are any
				Entity [] entities = chunk.getEntities();
				for(int i = 0; i < entities.length; i++)
				{
					if(entities[i] instanceof Player)
					{
						Player otherPlayer = (Player)entities[i];
						GriefPrevention.sendMessage(player, TextMode.Err, "Unable to restore.  " + otherPlayer.getName() + " is in that chunk.");
						return;
					}
				}
				
				//build a snapshot of this chunk, including 1 block boundary outside of the chunk all the way around
				int maxHeight = chunk.getWorld().getMaxHeight();
				BlockSnapshot[][][] snapshots = new BlockSnapshot[18][maxHeight][18];
				Block startBlock = chunk.getBlock(0, 0, 0);
				Location startLocation = new Location(chunk.getWorld(), startBlock.getX() - 1, 0, startBlock.getZ() - 1);
				for(int x = 0; x < snapshots.length; x++)
				{
					for(int z = 0; z < snapshots[0][0].length; z++)
					{
						for(int y = 0; y < snapshots[0].length; y++)
						{
							Block block = chunk.getWorld().getBlockAt(startLocation.getBlockX() + x, startLocation.getBlockY() + y, startLocation.getBlockZ() + z);
							snapshots[x][y][z] = new BlockSnapshot(block.getLocation(), block.getTypeId(), block.getData());
						}
					}
				}
				
				//create task to process those data in another thread
				
				//set boundaries for processing
				int miny = clickedBlock.getY();
				if(miny > chunk.getWorld().getSeaLevel() - 10)
				{
					miny = chunk.getWorld().getSeaLevel() - 10;
				}
				
				Location lesserBoundaryCorner = chunk.getBlock(0,  0, 0).getLocation();
				Location greaterBoundaryCorner = chunk.getBlock(15, 0, 15).getLocation();
				
				//create task
				//when done processing, this task will create a main thread task to actually update the world with processing results
				RestoreNatureProcessingTask task = new RestoreNatureProcessingTask(snapshots, miny, chunk.getWorld().getEnvironment(), chunk.getWorld().getBiome(lesserBoundaryCorner.getBlockX(), lesserBoundaryCorner.getBlockZ()), lesserBoundaryCorner, greaterBoundaryCorner, chunk.getWorld().getSeaLevel(), player);
				GriefPrevention.instance.getServer().getScheduler().scheduleAsyncDelayedTask(GriefPrevention.instance, task);
				
				return;
			}
			
			//if the player doesn't have claims permission, don't do anything
			if(GriefPrevention.instance.config_claims_creationRequiresPermission && !player.hasPermission("griefprevention.createclaims"))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You don't have permission to claim land.");
				return;
			}
			
			//if he's resizing a claim and that claim hasn't been deleted since he started resizing it
			if(playerData.claimResizing != null && playerData.claimResizing.inDataStore)
			{
				if(clickedBlock.getLocation().equals(playerData.lastShovelLocation)) return;

				//figure out what the coords of his new claim would be
				int newx1, newx2, newz1, newz2, newy1, newy2;
				if(playerData.lastShovelLocation.getBlockX() == playerData.claimResizing.getLesserBoundaryCorner().getBlockX())
				{
					newx1 = clickedBlock.getX();
				}
				else
				{
					newx1 = playerData.claimResizing.getLesserBoundaryCorner().getBlockX();
				}
				
				if(playerData.lastShovelLocation.getBlockX() == playerData.claimResizing.getGreaterBoundaryCorner().getBlockX())
				{
					newx2 = clickedBlock.getX();
				}
				else
				{
					newx2 = playerData.claimResizing.getGreaterBoundaryCorner().getBlockX();
				}
				
				if(playerData.lastShovelLocation.getBlockZ() == playerData.claimResizing.getLesserBoundaryCorner().getBlockZ())
				{
					newz1 = clickedBlock.getZ();
				}
				else
				{
					newz1 = playerData.claimResizing.getLesserBoundaryCorner().getBlockZ();
				}
				
				if(playerData.lastShovelLocation.getBlockZ() == playerData.claimResizing.getGreaterBoundaryCorner().getBlockZ())
				{
					newz2 = clickedBlock.getZ();
				}
				else
				{
					newz2 = playerData.claimResizing.getGreaterBoundaryCorner().getBlockZ();
				}
				
				newy1 = playerData.claimResizing.getLesserBoundaryCorner().getBlockY();
				newy2 = clickedBlock.getY() - GriefPrevention.instance.config_claims_claimsExtendIntoGroundDistance;
				
				//for top level claims, apply size rules and claim blocks requirement
				if(playerData.claimResizing.parent == null)
				{				
					//measure new claim, apply size rules
					int newWidth = (Math.abs(newx1 - newx2) + 1);
					int newHeight = (Math.abs(newz1 - newz2) + 1);
							
					if(!playerData.claimResizing.isAdminClaim() && (newWidth < GriefPrevention.instance.config_claims_minSize || newHeight < GriefPrevention.instance.config_claims_minSize))
					{
						GriefPrevention.sendMessage(player, TextMode.Err, "This new size would be too small.  Claims must be at least " + GriefPrevention.instance.config_claims_minSize + " x " + GriefPrevention.instance.config_claims_minSize + ".");
						return;
					}
					
					//make sure player has enough blocks to make up the difference
					if(!playerData.claimResizing.isAdminClaim())
					{
						int newArea =  newWidth * newHeight;
						int blocksRemainingAfter = playerData.getRemainingClaimBlocks() + playerData.claimResizing.getArea() - newArea;
					
						if(blocksRemainingAfter < 0)
						{
							GriefPrevention.sendMessage(player, TextMode.Err, "You don't have enough blocks for this size.  You need " + Math.abs(blocksRemainingAfter) + " more.");
							return;
						}
					}
				}
				
				//ask the datastore to try and resize the claim
				CreateClaimResult result = GriefPrevention.instance.dataStore.resizeClaim(playerData.claimResizing, newx1, newx2, newy1, newy2, newz1, newz2);
				
				if(result.succeeded)
				{
					//inform and show the player
					GriefPrevention.sendMessage(player, TextMode.Success, "Claim resized.  You now have " + playerData.getRemainingClaimBlocks() + " available claim blocks.");
					Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.Claim);
					Visualization.Apply(player, visualization);						
					
					//clean up
					playerData.claimResizing = null;
					playerData.lastShovelLocation = null;
				}
				else
				{
					//inform player
					GriefPrevention.sendMessage(player, TextMode.Err, "Can't resize here because it would overlap another nearby claim.");
					
					//show the player the conflicting claim
					Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.ErrorClaim);
					Visualization.Apply(player, visualization);
				}
				
				return;
			}
			
			//otherwise, since not currently resizing a claim, must be starting a resize, creating a new claim, or creating a subdivision
			Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), true /*ignore height*/, playerData.lastClaim);			
			
			//if within an existing claim, he's not creating a new one
			if(claim != null)
			{
				//if the player has permission to edit the claim or subdivision
				String noEditReason = claim.allowEdit(player);
				if(noEditReason == null)
				{
					//if he clicked on a corner, start resizing it
					if((clickedBlock.getX() == claim.getLesserBoundaryCorner().getBlockX() || clickedBlock.getX() == claim.getGreaterBoundaryCorner().getBlockX()) && (clickedBlock.getZ() == claim.getLesserBoundaryCorner().getBlockZ() || clickedBlock.getZ() == claim.getGreaterBoundaryCorner().getBlockZ()))
					{
						playerData.claimResizing = claim;
						playerData.lastShovelLocation = clickedBlock.getLocation();
						player.sendMessage("Resizing claim.  Use your shovel again at the new location for this corner.");
					}
					
					//if he didn't click on a corner and is in subdivision mode, he's creating a new subdivision
					else if(playerData.shovelMode == ShovelMode.Subdivide)
					{
						//if it's the first click, he's trying to start a new subdivision
						if(playerData.lastShovelLocation == null)
						{						
							//if the clicked claim was a subdivision, tell him he can't start a new subdivision here
							if(claim.parent != null)
							{
								GriefPrevention.sendMessage(player, TextMode.Err, "You can't create a subdivision here because it would overlap another subdivision.  Consider /abandonclaim to delete it, or use your shovel at a corner to resize it.");							
							}
						
							//otherwise start a new subdivision
							else
							{
								GriefPrevention.sendMessage(player, TextMode.Instr, "Subdivision corner set!  Use your shovel at the location for the opposite corner of this new subdivision.");
								playerData.lastShovelLocation = clickedBlock.getLocation();
								playerData.claimSubdividing = claim;
							}
						}
						
						//otherwise, he's trying to finish creating a subdivision by setting the other boundary corner
						else
						{
							//if last shovel location was in a different world, assume the player is starting the create-claim workflow over
							if(!playerData.lastShovelLocation.getWorld().equals(clickedBlock.getWorld()))
							{
								playerData.lastShovelLocation = null;
								this.onPlayerInteract(event);
								return;
							}
							
							//try to create a new claim (will return null if this subdivision overlaps another)
							CreateClaimResult result = this.dataStore.createClaim(
									player.getWorld(), 
									playerData.lastShovelLocation.getBlockX(), clickedBlock.getX(), 
									playerData.lastShovelLocation.getBlockY() - GriefPrevention.instance.config_claims_claimsExtendIntoGroundDistance, clickedBlock.getY() - GriefPrevention.instance.config_claims_claimsExtendIntoGroundDistance, 
									playerData.lastShovelLocation.getBlockZ(), clickedBlock.getZ(), 
									"--subdivision--",  //owner name is not used for subdivisions
									playerData.claimSubdividing);
							
							//if it didn't succeed, tell the player why
							if(!result.succeeded)
							{
								GriefPrevention.sendMessage(player, TextMode.Err, "Your selected area overlaps another subdivision.");
																				
								Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.ErrorClaim);
								Visualization.Apply(player, visualization);
								
								return;
							}
							
							//otherwise, advise him on the /trust command and show him his new subdivision
							else
							{					
								GriefPrevention.sendMessage(player, TextMode.Success, "Subdivision created!  Use /trust to share it with friends.");
								Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.Claim);
								Visualization.Apply(player, visualization);
								playerData.lastShovelLocation = null;
								playerData.claimSubdividing = null;
							}
						}
					}
					
					//otherwise tell him he can't create a claim here, and show him the existing claim
					//also advise him to consider /abandonclaim or resizing the existing claim
					else
					{						
						GriefPrevention.sendMessage(player, TextMode.Err, "You can't create a claim here because it would overlap your other claim.  Use /abandonclaim to delete it, or use your shovel at a corner to resize it.");
						Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.Claim);
						Visualization.Apply(player, visualization);
					}
				}
				
				//otherwise tell the player he can't claim here because it's someone else's claim, and show him the claim
				else
				{
					GriefPrevention.sendMessage(player, TextMode.Err, "You can't create a claim here because it would overlap " + claim.getOwnerName() + "'s claim.");
					Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.ErrorClaim);
					Visualization.Apply(player, visualization);						
				}
				
				return;
			}
			
			//otherwise, the player isn't in an existing claim!
			
			//if he hasn't already start a claim with a previous shovel action
			Location lastShovelLocation = playerData.lastShovelLocation;
			if(lastShovelLocation == null)
			{
				//if claims are not enabled in this world and it's not an administrative claim, display an error message and stop
				if(!GriefPrevention.instance.claimsEnabledForWorld(player.getWorld()) && playerData.shovelMode != ShovelMode.Admin)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, "Land claims are disabled in this world.");
					return;
				}
				
				//remember it, and start him on the new claim
				playerData.lastShovelLocation = clickedBlock.getLocation();
				GriefPrevention.sendMessage(player, TextMode.Instr, "Claim corner set!  Use the shovel again at the opposite corner to claim a rectangle of land.  To cancel, put your shovel away.");
			}
			
			//otherwise, he's trying to finish creating a claim by setting the other boundary corner
			else
			{
				//if last shovel location was in a different world, assume the player is starting the create-claim workflow over
				if(!lastShovelLocation.getWorld().equals(clickedBlock.getWorld()))
				{
					playerData.lastShovelLocation = null;
					this.onPlayerInteract(event);
					return;
				}
				
				//apply minimum claim dimensions rule
				int newClaimWidth = Math.abs(playerData.lastShovelLocation.getBlockX() - clickedBlock.getX()) + 1;
				int newClaimHeight = Math.abs(playerData.lastShovelLocation.getBlockZ() - clickedBlock.getZ()) + 1;
				
				if(playerData.shovelMode != ShovelMode.Admin && (newClaimWidth < GriefPrevention.instance.config_claims_minSize || newClaimHeight < GriefPrevention.instance.config_claims_minSize))
				{
					GriefPrevention.sendMessage(player, TextMode.Err, "Stopping your claim here would create a too-small claim.  A claim must be at least " + GriefPrevention.instance.config_claims_minSize + " x " + GriefPrevention.instance.config_claims_minSize + ".");
					return;
				}
				
				//if not an administrative claim, verify the player has enough claim blocks for this new claim
				if(playerData.shovelMode != ShovelMode.Admin)
				{					
					int newClaimArea = newClaimWidth * newClaimHeight; 
					int remainingBlocks = playerData.getRemainingClaimBlocks();
					if(newClaimArea > remainingBlocks)
					{
						GriefPrevention.sendMessage(player, TextMode.Err, "You don't have enough blocks to claim that entire area.  You need " + (newClaimArea - remainingBlocks) + " more blocks.");
						GriefPrevention.sendMessage(player, TextMode.Instr, "To delete another claim and free up some blocks, use /abandonclaim.");
						return;
					}
				}					
				else
				{
					playerName = "";
				}
				
				//try to create a new claim (will return null if this claim overlaps another)
				CreateClaimResult result = this.dataStore.createClaim(
						player.getWorld(), 
						lastShovelLocation.getBlockX(), clickedBlock.getX(), 
						lastShovelLocation.getBlockY() - GriefPrevention.instance.config_claims_claimsExtendIntoGroundDistance, clickedBlock.getY() - GriefPrevention.instance.config_claims_claimsExtendIntoGroundDistance, 
						lastShovelLocation.getBlockZ(), clickedBlock.getZ(), 
						playerName,
						null);
				
				//if it didn't succeed, tell the player why
				if(!result.succeeded)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, "Your selected area overlaps an existing claim.");
					
					Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.ErrorClaim);
					Visualization.Apply(player, visualization);
					
					return;
				}
				
				//otherwise, advise him on the /trust command and show him his new claim
				else
				{					
					GriefPrevention.sendMessage(player, TextMode.Success, "Claim created!  Use /trust to share it with friends.");
					Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.Claim);
					Visualization.Apply(player, visualization);
					playerData.lastShovelLocation = null;
				}
			}
		}
	}	
}
