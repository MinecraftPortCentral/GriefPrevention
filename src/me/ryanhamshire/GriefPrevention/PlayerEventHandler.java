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
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.PoweredMinecart;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

class PlayerEventHandler implements Listener 
{
	private DataStore dataStore;
	
	//list of temporarily banned ip's
	private ArrayList<IpBanInfo> tempBannedIps = new ArrayList<IpBanInfo>();
	
	//number of milliseconds in a day
	private final long MILLISECONDS_IN_DAY = 1000 * 60 * 60 * 24;
	
	//timestamps of login and logout notifications in the last minute
	private ArrayList<Long> recentLoginLogoutNotifications = new ArrayList<Long>();
	
	//regex pattern for the "how do i claim land?" scanner
	private Pattern howToClaimPattern = null;
	
	//typical constructor, yawn
	PlayerEventHandler(DataStore dataStore, GriefPrevention plugin)
	{
		this.dataStore = dataStore;
	}
	
	//when a player chats, monitor for spam
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	synchronized void onPlayerChat (AsyncPlayerChatEvent event)
	{		
		Player player = event.getPlayer();
		if(!player.isOnline())
		{
			event.setCancelled(true);
			return;
		}
		
		String message = event.getMessage();
		
		event.setCancelled(this.handlePlayerChat(player, message, event));
	}
	
	//returns true if the message should be sent, false if it should be muted 
	private boolean handlePlayerChat(Player player, String message, PlayerEvent event)
	{
		//FEATURE: automatically educate players about claiming land
		//watching for message format how*claim*, and will send a link to the basics video
		if(this.howToClaimPattern == null)
		{
			this.howToClaimPattern = Pattern.compile(this.dataStore.getMessage(Messages.HowToClaimRegex), Pattern.CASE_INSENSITIVE);
		}
		
		if(this.howToClaimPattern.matcher(message).matches())
		{
			if(GriefPrevention.instance.creativeRulesApply(player.getLocation()))
			{
				GriefPrevention.sendMessage(player, TextMode.Info, Messages.CreativeBasicsDemoAdvertisement, 10L);
			}
			else
			{
				GriefPrevention.sendMessage(player, TextMode.Info, Messages.SurvivalBasicsDemoAdvertisement, 10L);
			}
		}
		
		//FEATURE: automatically educate players about the /trapped command
		//check for "trapped" or "stuck" to educate players about the /trapped command
		if(!message.contains("/trapped") && (message.contains("trapped") || message.contains("stuck") || message.contains(this.dataStore.getMessage(Messages.TrappedChatKeyword))))
		{
			GriefPrevention.sendMessage(player, TextMode.Info, Messages.TrappedInstructions, 10L);
		}
		
		//FEATURE: monitor for chat and command spam
		
		if(!GriefPrevention.instance.config_spam_enabled) return false;
		
		//if the player has permission to spam, don't bother even examining the message
		if(player.hasPermission("griefprevention.spam")) return false;
		
		boolean spam = false;
		boolean muted = false;
		
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		
		//remedy any CAPS SPAM, exception for very short messages which could be emoticons like =D or XD
		if(message.length() > 4 && this.stringsAreSimilar(message.toUpperCase(), message))
		{
			//exception for strings containing forward slash to avoid changing a case-sensitive URL
			if(event instanceof AsyncPlayerChatEvent && !message.contains("/"))
			{
				((AsyncPlayerChatEvent)event).setMessage(message.toLowerCase());
			}
		}
		
		//where other types of spam are concerned, casing isn't significant
		message = message.toLowerCase();
		
		//check message content and timing		
		long millisecondsSinceLastMessage = (new Date()).getTime() - playerData.lastMessageTimestamp.getTime();
		
		//if the message came too close to the last one
		if(millisecondsSinceLastMessage < 1500)
		{
			//increment the spam counter
			playerData.spamCount++;
			spam = true;
		}
		
		//if it's very similar to the last message
		if(!muted && this.stringsAreSimilar(message, playerData.lastMessage))
		{
			playerData.spamCount++;
			spam = true;
			muted = true;
		}
		
		//filter IP addresses
		if(!muted)
		{
			Pattern ipAddressPattern = Pattern.compile("\\d{1,4}\\D{1,3}\\d{1,4}\\D{1,3}\\d{1,4}\\D{1,3}\\d{1,4}");
			Matcher matcher = ipAddressPattern.matcher(message);
			
			//if it looks like an IP address
			if(matcher.find())
			{
				//and it's not in the list of allowed IP addresses
				if(!GriefPrevention.instance.config_spam_allowedIpAddresses.contains(matcher.group()))
				{
					//log entry
					GriefPrevention.AddLogEntry("Muted IP address from " + player.getName() + ": " + message);
					
					//spam notation
					playerData.spamCount++;
					spam = true;
					
					//block message
					muted = true;
				}
			}
		}
		
		//if the message was mostly non-alpha-numerics or doesn't include much whitespace, consider it a spam (probably ansi art or random text gibberish) 
		if(!muted && message.length() > 5)
		{
			int symbolsCount = 0;
			int whitespaceCount = 0;
			for(int i = 0; i < message.length(); i++)
			{
				char character = message.charAt(i);
				if(!(Character.isLetterOrDigit(character)))
				{
					symbolsCount++;
				}
				
				if(Character.isWhitespace(character))
				{
					whitespaceCount++;
				}
			}
			
			if(symbolsCount > message.length() / 2 || (message.length() > 15 && whitespaceCount < message.length() / 10))
			{
				spam = true;
				if(playerData.spamCount > 0) muted = true;
				playerData.spamCount++;
			}
		}
		
		//very short messages close together are spam
		if(!muted && message.length() < 5 && millisecondsSinceLastMessage < 3000)
		{
			spam = true;
			playerData.spamCount++;
		}
		
		//if the message was determined to be a spam, consider taking action		
		if(spam)
		{		
			//anything above level 8 for a player which has received a warning...  kick or if enabled, ban 
			if(playerData.spamCount > 8 && playerData.spamWarned)
			{
				if(GriefPrevention.instance.config_spam_banOffenders)
				{
					//log entry
					GriefPrevention.AddLogEntry("Banning " + player.getName() + " for spam.");
					
					//kick and ban
					PlayerKickBanTask task = new PlayerKickBanTask(player, GriefPrevention.instance.config_spam_banMessage);
					GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task, 1L);
				}
				else
				{
					//log entry
					GriefPrevention.AddLogEntry("Banning " + player.getName() + " for spam.");
					
					//just kick
					PlayerKickBanTask task = new PlayerKickBanTask(player, null);
					GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task, 1L);					
				}
				
				return true;
			}
			
			//cancel any messages while at or above the third spam level and issue warnings
			//anything above level 2, mute and warn
			if(playerData.spamCount >= 4)
			{
				muted = true;
				if(!playerData.spamWarned)
				{
					GriefPrevention.sendMessage(player, TextMode.Warn, GriefPrevention.instance.config_spam_warningMessage, 10L);
					GriefPrevention.AddLogEntry("Warned " + player.getName() + " about spam penalties.");
					playerData.spamWarned = true;
				}
			}
			
			if(muted)
			{
				//make a log entry
				GriefPrevention.AddLogEntry("Muted spam from " + player.getName() + ": " + message);
				
				//send a fake message so the player doesn't realize he's muted
				//less information for spammers = less effective spam filter dodging
				player.sendMessage("<" + player.getName() + "> " + message);
				
				//cancelling the event guarantees other players don't receive the message
				return true;
			}		
		}
		
		//otherwise if not a spam, reset the spam counter for this player
		else
		{
			playerData.spamCount = 0;
			playerData.spamWarned = false;
		}
		
		//in any case, record the timestamp of this message and also its content for next time
		playerData.lastMessageTimestamp = new Date();
		playerData.lastMessage = message;
		
		return false;
	}
	
	//if two strings are 75% identical, they're too close to follow each other in the chat
	private boolean stringsAreSimilar(String message, String lastMessage)
	{
		//determine which is shorter
		String shorterString, longerString;
		if(lastMessage.length() < message.length())
		{
			shorterString = lastMessage;
			longerString = message;
		}
		else
		{
			shorterString = message;
			longerString = lastMessage;
		}
		
		if(shorterString.length() <= 5) return shorterString.equals(longerString);
		
		//set similarity tolerance
		int maxIdenticalCharacters = longerString.length() - longerString.length() / 4;
		
		//trivial check on length
		if(shorterString.length() < maxIdenticalCharacters) return false;
		
		//compare forward
		int identicalCount = 0;
		for(int i = 0; i < shorterString.length(); i++)
		{
			if(shorterString.charAt(i) == longerString.charAt(i)) identicalCount++;
			if(identicalCount > maxIdenticalCharacters) return true;
		}
		
		//compare backward
		for(int i = 0; i < shorterString.length(); i++)
		{
			if(shorterString.charAt(shorterString.length() - i - 1) == longerString.charAt(longerString.length() - i - 1)) identicalCount++;
			if(identicalCount > maxIdenticalCharacters) return true;
		}
		
		return false;
	}

	//when a player uses a slash command...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	synchronized void onPlayerCommandPreprocess (PlayerCommandPreprocessEvent event)
	{
		String [] args = event.getMessage().split(" ");
		
		//if eavesdrop enabled, eavesdrop
		String command = args[0].toLowerCase();
		if(GriefPrevention.instance.config_eavesdrop && GriefPrevention.instance.config_eavesdrop_whisperCommands.contains(command) && !event.getPlayer().hasPermission("griefprevention.eavesdrop") && args.length > 1)
		{			
			StringBuilder logMessageBuilder = new StringBuilder();
			logMessageBuilder.append("[[").append(event.getPlayer().getName()).append("]] ");
			
			for(int i = 1; i < args.length; i++)
			{
				logMessageBuilder.append(args[i]).append(" ");
			}
			
			String logMessage = logMessageBuilder.toString();
			
			Player [] players = GriefPrevention.instance.getServer().getOnlinePlayers();
			for(int i = 0; i < players.length; i++)
			{
				Player player = players[i];
				if(player.hasPermission("griefprevention.eavesdrop") && !player.getName().equalsIgnoreCase(args[1]))
				{
					player.sendMessage(ChatColor.GRAY + logMessage);
				}
			}
		}
		
		//if in pvp, block any pvp-banned slash commands
		PlayerData playerData = this.dataStore.getPlayerData(event.getPlayer().getName());
		if((playerData.inPvpCombat() || playerData.siegeData != null) && GriefPrevention.instance.config_pvp_blockedCommands.contains(command))
		{
			event.setCancelled(true);
			GriefPrevention.sendMessage(event.getPlayer(), TextMode.Err, Messages.CommandBannedInPvP);
			return;
		}
		
		//if anti spam enabled, check for spam
		if(!GriefPrevention.instance.config_spam_enabled) return;
		
		//if the slash command used is in the list of monitored commands, treat it like a chat message (see above)
		boolean isMonitoredCommand = false;
		for(String monitoredCommand : GriefPrevention.instance.config_spam_monitorSlashCommands)
		{
			if(args[0].equalsIgnoreCase(monitoredCommand))
			{
				isMonitoredCommand = true;
				break;
			}
		}
		
		if(isMonitoredCommand)
		{
			event.setCancelled(this.handlePlayerChat(event.getPlayer(), event.getMessage(), event));		
		}
	}
	
	//when a player attempts to join the server...
	@EventHandler(priority = EventPriority.HIGHEST)
	void onPlayerLogin (PlayerLoginEvent event)
	{
		Player player = event.getPlayer();
		
		//all this is anti-spam code
		if(GriefPrevention.instance.config_spam_enabled)
		{
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
					return;
				}
			}
			
			//if logging-in account is banned, remember IP address for later
			long now = Calendar.getInstance().getTimeInMillis();
			if(GriefPrevention.instance.config_smartBan && event.getResult() == Result.KICK_BANNED)
			{
				this.tempBannedIps.add(new IpBanInfo(event.getAddress(), now + this.MILLISECONDS_IN_DAY, player.getName()));
			}
		}
		
		//remember the player's ip address
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		playerData.ipAddress = event.getAddress();
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
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	void onPlayerJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		String playerName = player.getName();
		
		//note login time
		long now = Calendar.getInstance().getTimeInMillis();
		PlayerData playerData = this.dataStore.getPlayerData(playerName);
		playerData.lastSpawn = now;
		playerData.lastLogin = new Date();
		this.dataStore.savePlayerData(playerName, playerData);
		
		//if player has never played on the server before, may need pvp protection
		if(!player.hasPlayedBefore())
		{
			GriefPrevention.instance.checkPvpProtectionNeeded(player);
		}
		
		//silence notifications when they're coming too fast
		if(event.getJoinMessage() != null && this.shouldSilenceNotification())
		{
			event.setJoinMessage(null);
		}
		
		//FEATURE: auto-ban accounts who use an IP address which was very recently used by another banned account
		if(GriefPrevention.instance.config_smartBan && !player.hasPlayedBefore())
		{		
			//search temporarily banned IP addresses for this one
			for(int i = 0; i < this.tempBannedIps.size(); i++)
			{
				IpBanInfo info = this.tempBannedIps.get(i);
				String address = info.address.toString();
				
				//eliminate any expired entries
				if(now > info.expirationTimestamp)
				{
					this.tempBannedIps.remove(i--);
				}
				
				//if we find a match				
				else if(address.equals(playerData.ipAddress.toString()))
				{
					//if the account associated with the IP ban has been pardoned, remove all ip bans for that ip and we're done
					OfflinePlayer bannedPlayer = GriefPrevention.instance.getServer().getOfflinePlayer(info.bannedAccountName);
					if(!bannedPlayer.isBanned())
					{
						for(int j = 0; j < this.tempBannedIps.size(); j++)
						{
							IpBanInfo info2 = this.tempBannedIps.get(j);
							if(info2.address.toString().equals(address))
							{
								OfflinePlayer bannedAccount = GriefPrevention.instance.getServer().getOfflinePlayer(info2.bannedAccountName);
								bannedAccount.setBanned(false);
								this.tempBannedIps.remove(j--);
							}
						}
						
						break;
					}
					
					//otherwise if that account is still banned, ban this account, too
					else
					{
						GriefPrevention.AddLogEntry("Auto-banned " + player.getName() + " because that account is using an IP address very recently used by banned player " + info.bannedAccountName + " (" + info.address.toString() + ").");
						
						//notify any online ops
						Player [] players = GriefPrevention.instance.getServer().getOnlinePlayers();
						for(int k = 0; k < players.length; k++)
						{
							if(players[k].isOp())
							{
								GriefPrevention.sendMessage(players[k], TextMode.Success, Messages.AutoBanNotify, player.getName(), info.bannedAccountName);
							}
						}
						
						//ban player
						PlayerKickBanTask task = new PlayerKickBanTask(player, "");
						GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task, 10L);
						
						//silence join message
						event.setJoinMessage("");
						
						break;
					}
				}
			}
		}
	}
	
	//when a player dies...
	@EventHandler(priority = EventPriority.LOWEST)
	void onPlayerDeath(PlayerDeathEvent event)
	{
		//FEATURE: prevent death message spam by implementing a "cooldown period" for death messages
		PlayerData playerData = this.dataStore.getPlayerData(event.getEntity().getName());
		long now = Calendar.getInstance().getTimeInMillis(); 
		if(now - playerData.lastDeathTimeStamp < GriefPrevention.instance.config_spam_deathMessageCooldownSeconds * 1000)
		{
			event.setDeathMessage("");
		}
		
		playerData.lastDeathTimeStamp = now;
	}
	
	//when a player quits...
	@EventHandler(priority = EventPriority.HIGHEST)
	void onPlayerQuit(PlayerQuitEvent event)
	{
		Player player = event.getPlayer();
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		
		//if banned, add IP to the temporary IP ban list
		if(player.isBanned() && playerData.ipAddress != null)
		{
			long now = Calendar.getInstance().getTimeInMillis(); 
			this.tempBannedIps.add(new IpBanInfo(playerData.ipAddress, now + this.MILLISECONDS_IN_DAY, player.getName()));
		}
		
		//silence notifications when they're coming too fast
		if(event.getQuitMessage() != null && this.shouldSilenceNotification())
		{
			event.setQuitMessage(null);
		}
		
		//make sure his data is all saved - he might have accrued some claim blocks while playing that were not saved immediately
		this.dataStore.savePlayerData(player.getName(), playerData);
		
		this.onPlayerDisconnect(event.getPlayer(), event.getQuitMessage());
	}
	
	//helper for above
	private void onPlayerDisconnect(Player player, String notificationMessage)
	{
		String playerName = player.getName();
		PlayerData playerData = this.dataStore.getPlayerData(playerName);
		
		//FEATURE: claims where players have allowed explosions will revert back to not allowing them when the owner logs out
		for(Claim claim : playerData.claims)
		{
			claim.areExplosivesAllowed = false;
		}
		
		//FEATURE: players in pvp combat when they log out will die
		if(GriefPrevention.instance.config_pvp_punishLogout && playerData.inPvpCombat())
		{
			player.setHealth(0);
		}
		
		//FEATURE: during a siege, any player who logs out dies and forfeits the siege
		
		//if player was involved in a siege, he forfeits
		if(playerData.siegeData != null)
		{
			if(player.getHealth() > 0) player.setHealth(0);  //might already be zero from above, this avoids a double death message
		}
		
		//drop data about this player
		this.dataStore.clearCachedPlayerData(player.getName());
	}
	
	//determines whether or not a login or logout notification should be silenced, depending on how many there have been in the last minute
	private boolean shouldSilenceNotification()
	{
		final long ONE_MINUTE = 60000;
		final int MAX_ALLOWED = 20;
		Long now = Calendar.getInstance().getTimeInMillis();
		
		//eliminate any expired entries (longer than a minute ago)
		for(int i = 0; i < this.recentLoginLogoutNotifications.size(); i++)
		{
			Long notificationTimestamp = this.recentLoginLogoutNotifications.get(i);
			if(now - notificationTimestamp > ONE_MINUTE)
			{
				this.recentLoginLogoutNotifications.remove(i--);
			}
			else
			{
				break;
			}
		}
		
		//add the new entry
		this.recentLoginLogoutNotifications.add(now);
		
		return this.recentLoginLogoutNotifications.size() > MAX_ALLOWED;
	}

	//when a player drops an item
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerDropItem(PlayerDropItemEvent event)
	{
		Player player = event.getPlayer();
		
		//in creative worlds, dropping items is blocked
		if(GriefPrevention.instance.creativeRulesApply(player.getLocation()))
		{
			event.setCancelled(true);
			return;
		}
		
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		
		//FEATURE: players under siege or in PvP combat, can't throw items on the ground to hide 
		//them or give them away to other players before they are defeated
		
		//if in combat, don't let him drop it
		if(!GriefPrevention.instance.config_pvp_allowCombatItemDrop && playerData.inPvpCombat())
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.PvPNoDrop);
			event.setCancelled(true);			
		}
		
		//if he's under siege, don't let him drop it
		else if(playerData.siegeData != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeNoDrop);
			event.setCancelled(true);
		}
	}
	
	//when a player teleports
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerTeleport(PlayerTeleportEvent event)
	{
		Player player = event.getPlayer();
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		
		//FEATURE: prevent players from using ender pearls to gain access to secured claims
		if(event.getCause() == TeleportCause.ENDER_PEARL && GriefPrevention.instance.config_claims_enderPearlsRequireAccessTrust)
		{
			Claim toClaim = this.dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);
			if(toClaim != null)
			{
				playerData.lastClaim = toClaim;
				String noAccessReason = toClaim.allowAccess(player);
				if(noAccessReason != null)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, noAccessReason);
					event.setCancelled(true);
				}
			}
		}
		
		//FEATURE: prevent teleport abuse to win sieges
		
		//these rules only apply to non-ender-pearl teleportation
		if(event.getCause() == TeleportCause.ENDER_PEARL) return;
		
		Location source = event.getFrom();
		Claim sourceClaim = this.dataStore.getClaimAt(source, false, playerData.lastClaim);
		if(sourceClaim != null && sourceClaim.siegeData != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeNoTeleport);
			event.setCancelled(true);
			return;
		}
		
		Location destination = event.getTo();
		Claim destinationClaim = this.dataStore.getClaimAt(destination, false, null);
		if(destinationClaim != null && destinationClaim.siegeData != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.BesiegedNoTeleport);
			event.setCancelled(true);
			return;
		}
	}
	
	//when a player interacts with an entity...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event)
	{
		Player player = event.getPlayer();
		Entity entity = event.getRightClicked();
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		
		//don't allow interaction with item frames in claimed areas without build permission
		if(entity instanceof Hanging)
		{
			String noBuildReason = GriefPrevention.instance.allowBuild(player, entity.getLocation()); 
			if(noBuildReason != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
				event.setCancelled(true);
				return;
			}			
		}
		
		//don't allow container access during pvp combat
		if((entity instanceof StorageMinecart || entity instanceof PoweredMinecart))
		{
			if(playerData.siegeData != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeNoContainers);
				event.setCancelled(true);
				return;
			}
			
			if(playerData.inPvpCombat())
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.PvPNoContainers);
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
						GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoDamageClaimedEntity);
						event.setCancelled(true);
					}
				}
			}
		}
	}
	
	//when a player picks up an item...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
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
				GriefPrevention.sendMessage(player, TextMode.Warn, Messages.PvPImmunityEnd);
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
		if(newItemStack != null && newItemStack.getType() == GriefPrevention.instance.config_claims_modificationTool)
		{
			PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getName());
		
			//always reset to basic claims mode
			if(playerData.shovelMode != ShovelMode.Basic)
			{			
				playerData.shovelMode = ShovelMode.Basic;
				GriefPrevention.sendMessage(player, TextMode.Info, Messages.ShovelBasicClaimMode);
			}
		
			//reset any work he might have been doing
			playerData.lastShovelLocation = null;
			playerData.claimResizing = null;
			
			//give the player his available claim blocks count and claiming instructions, but only if he keeps the shovel equipped for a minimum time, to avoid mouse wheel spam
			if(GriefPrevention.instance.claimsEnabledForWorld(player.getWorld()))
			{
				EquipShovelProcessingTask task = new EquipShovelProcessingTask(player);
				GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task, 15L);  //15L is approx. 3/4 of a second
			}
		}
	}
	
	//block players from entering beds they don't have permission for
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
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
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoBedPermission, claim.getOwnerName());
			}
		}		
	}
	
	//block use of buckets within other players' claims
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerBucketEmpty (PlayerBucketEmptyEvent bucketEvent)
	{
		Player player = bucketEvent.getPlayer();
		Block block = bucketEvent.getBlockClicked().getRelative(bucketEvent.getBlockFace());
		int minLavaDistance = 10;
		
		//make sure the player is allowed to build at the location
		String noBuildReason = GriefPrevention.instance.allowBuild(player, block.getLocation());
		if(noBuildReason != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
			bucketEvent.setCancelled(true);
			return;
		}
		
		//if the bucket is being used in a claim, allow for dumping lava closer to other players
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		Claim claim = this.dataStore.getClaimAt(block.getLocation(), false, playerData.lastClaim);
		if(claim != null)
		{
			minLavaDistance = 3;
		}
		
		//otherwise no wilderness dumping (unless underground) in worlds where claims are enabled
		else if(GriefPrevention.instance.config_claims_enabledWorlds.contains(block.getWorld()))
		{
			if(block.getY() >= GriefPrevention.instance.getSeaLevel(block.getWorld()) - 5 && !player.hasPermission("griefprevention.lava"))
			{
				if(bucketEvent.getBucket() == Material.LAVA_BUCKET || GriefPrevention.instance.config_blockWildernessWaterBuckets)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoWildernessBuckets);
					bucketEvent.setCancelled(true);
					return;
				}
			}
		}
		
		//lava buckets can't be dumped near other players unless pvp is on
		if(!GriefPrevention.instance.config_pvp_enabledWorlds.contains(block.getWorld()) && !player.hasPermission("griefprevention.lava"))
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
						GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoLavaNearOtherPlayer, otherPlayer.getName());
						bucketEvent.setCancelled(true);
						return;
					}					
				}
			}
		}
	}
	
	//see above
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerBucketFill (PlayerBucketFillEvent bucketEvent)
	{
		Player player = bucketEvent.getPlayer();
		Block block = bucketEvent.getBlockClicked();
		
		//make sure the player is allowed to build at the location
		String noBuildReason = GriefPrevention.instance.allowBuild(player, block.getLocation());
		if(noBuildReason != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
			bucketEvent.setCancelled(true);
			return;
		}
	}
	
	//when a player interacts with the world
	@EventHandler(priority = EventPriority.LOWEST)
	void onPlayerInteract(PlayerInteractEvent event)
	{
		Player player = event.getPlayer();
		
		//determine target block.  FEATURE: shovel and string can be used from a distance away
		Block clickedBlock = null;
		
		try
		{
			clickedBlock = event.getClickedBlock();  //null returned here means interacting with air			
			if(clickedBlock == null || clickedBlock.getType() == Material.SNOW)
			{
				//try to find a far away non-air block along line of sight
				HashSet<Byte> transparentMaterials = new HashSet<Byte>();
				transparentMaterials.add(Byte.valueOf((byte)Material.AIR.getId()));
				transparentMaterials.add(Byte.valueOf((byte)Material.SNOW.getId()));
				transparentMaterials.add(Byte.valueOf((byte)Material.LONG_GRASS.getId()));
				clickedBlock = player.getTargetBlock(transparentMaterials, 250);
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
		
		//apply rules for putting out fires (requires build permission)
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		if(event.getClickedBlock() != null && event.getClickedBlock().getRelative(event.getBlockFace()).getType() == Material.FIRE)
		{
			Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
			if(claim != null)
			{
				playerData.lastClaim = claim;
				
				String noBuildReason = claim.allowBuild(player);
				if(noBuildReason != null)
				{
					event.setCancelled(true);
					GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
					return;
				}
			}
		}
		
		//apply rules for containers and crafting blocks
		if(	GriefPrevention.instance.config_claims_preventTheft && (
						event.getAction() == Action.RIGHT_CLICK_BLOCK && (
						clickedBlock.getState() instanceof InventoryHolder ||
						clickedBlockType == Material.WORKBENCH || 
						clickedBlockType == Material.ENDER_CHEST ||
						clickedBlockType == Material.DISPENSER ||
						clickedBlockType == Material.ANVIL ||
						clickedBlockType == Material.BREWING_STAND || 
						clickedBlockType == Material.JUKEBOX || 
						clickedBlockType == Material.ENCHANTMENT_TABLE ||
						GriefPrevention.instance.config_mods_containerTrustIds.Contains(new MaterialInfo(clickedBlock.getTypeId(), clickedBlock.getData(), null)))))
		{			
			//block container use while under siege, so players can't hide items from attackers
			if(playerData.siegeData != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeNoContainers);
				event.setCancelled(true);
				return;
			}
			
			//block container use during pvp combat, same reason
			if(playerData.inPvpCombat())
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.PvPNoContainers);
				event.setCancelled(true);
				return;
			}
			
			//otherwise check permissions for the claim the player is in
			Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
			if(claim != null)
			{
				playerData.lastClaim = claim;
				
				String noContainersReason = claim.allowContainers(player);
				if(noContainersReason != null)
				{
					event.setCancelled(true);
					GriefPrevention.sendMessage(player, TextMode.Err, noContainersReason);
					return;
				}
			}
			
			//if the event hasn't been cancelled, then the player is allowed to use the container
			//so drop any pvp protection
			if(playerData.pvpImmune)
			{
				playerData.pvpImmune = false;
				GriefPrevention.sendMessage(player, TextMode.Warn, Messages.PvPImmunityEnd);
			}
		}
		
		//otherwise apply rules for doors, if configured that way
		else if((GriefPrevention.instance.config_claims_lockWoodenDoors && clickedBlockType == Material.WOODEN_DOOR) ||
				(GriefPrevention.instance.config_claims_lockTrapDoors && clickedBlockType == Material.TRAP_DOOR) ||
				(GriefPrevention.instance.config_claims_lockFenceGates && clickedBlockType == Material.FENCE_GATE))
		{
			Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
			if(claim != null)
			{
				playerData.lastClaim = claim;
				
				String noAccessReason = claim.allowAccess(player);
				if(noAccessReason != null)
				{
					event.setCancelled(true);
					GriefPrevention.sendMessage(player, TextMode.Err, noAccessReason);
					return;
				}
			}	
		}
		
		//otherwise apply rules for buttons and switches
		else if(GriefPrevention.instance.config_claims_preventButtonsSwitches && (clickedBlockType == null || clickedBlockType == Material.STONE_BUTTON || clickedBlockType == Material.WOOD_BUTTON || clickedBlockType == Material.LEVER || GriefPrevention.instance.config_mods_accessTrustIds.Contains(new MaterialInfo(clickedBlock.getTypeId(), clickedBlock.getData(), null))))
		{
			Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
			if(claim != null)
			{
				playerData.lastClaim = claim;
				
				String noAccessReason = claim.allowAccess(player);
				if(noAccessReason != null)
				{
					event.setCancelled(true);
					GriefPrevention.sendMessage(player, TextMode.Err, noAccessReason);
					return;
				}
			}			
		}
		
		//apply rule for players trampling tilled soil back to dirt (never allow it)
		//NOTE: that this event applies only to players.  monsters and animals can still trample.
		else if(event.getAction() == Action.PHYSICAL && clickedBlockType == Material.SOIL)
		{
			event.setCancelled(true);
			return;
		}
		
		//apply rule for note blocks and repeaters
		else if(clickedBlockType == Material.NOTE_BLOCK || clickedBlockType == Material.DIODE_BLOCK_ON || clickedBlockType == Material.DIODE_BLOCK_OFF)
		{
			Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
			if(claim != null)
			{
				String noBuildReason = claim.allowBuild(player);
				if(noBuildReason != null)
				{
					event.setCancelled(true);
					GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
					return;
				}
			}
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
				String noBuildReason = GriefPrevention.instance.allowBuild(player, clickedBlock.getLocation());
				if(noBuildReason != null)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
					event.setCancelled(true);
				}
				
				return;
			}
			
			else if(materialInHand ==  Material.BOAT)
			{
				Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
				if(claim != null)
				{
					String noAccessReason = claim.allowAccess(player);
					if(noAccessReason != null)
					{
						GriefPrevention.sendMessage(player, TextMode.Err, noAccessReason);
						event.setCancelled(true);
					}
				}
				
				return;
			}
			
			//if it's a spawn egg, minecart, or boat, and this is a creative world, apply special rules
			else if((materialInHand == Material.MONSTER_EGG || materialInHand == Material.MINECART || materialInHand == Material.POWERED_MINECART || materialInHand == Material.STORAGE_MINECART || materialInHand == Material.BOAT) && GriefPrevention.instance.creativeRulesApply(clickedBlock.getLocation()))
			{
				//player needs build permission at this location
				String noBuildReason = GriefPrevention.instance.allowBuild(player, clickedBlock.getLocation());
				if(noBuildReason != null)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
					event.setCancelled(true);
					return;
				}
			
				//enforce limit on total number of entities in this claim
				Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
				if(claim == null) return;
				
				String noEntitiesReason = claim.allowMoreEntities();
				if(noEntitiesReason != null)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, noEntitiesReason);
					event.setCancelled(true);
					return;
				}
				
				return;
			}
			
			//if he's investigating a claim			
			else if(materialInHand == GriefPrevention.instance.config_claims_investigationTool)
			{
				//air indicates too far away
				if(clickedBlockType == Material.AIR)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.TooFarAway);
					return;
				}
				
				Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false /*ignore height*/, playerData.lastClaim);
				
				//no claim case
				if(claim == null)
				{
					GriefPrevention.sendMessage(player, TextMode.Info, Messages.BlockNotClaimed);
					Visualization.Revert(player);
				}
				
				//claim case
				else
				{
					playerData.lastClaim = claim;
					GriefPrevention.sendMessage(player, TextMode.Info, Messages.BlockClaimed, claim.getOwnerName());
					
					//visualize boundary
					Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.Claim, player.getLocation());
					Visualization.Apply(player, visualization);
					
					//if can resize this claim, tell about the boundaries
					if(claim.allowEdit(player) == null)
					{
						GriefPrevention.sendMessage(player, TextMode.Info, "  " + claim.getWidth() + "x" + claim.getHeight() + "=" + claim.getArea());
					}
					
					//if deleteclaims permission, tell about the player's offline time
					if(!claim.isAdminClaim() && player.hasPermission("griefprevention.deleteclaims"))
					{
						PlayerData otherPlayerData = this.dataStore.getPlayerData(claim.getOwnerName());
						Date lastLogin = otherPlayerData.lastLogin;
						Date now = new Date();
						long daysElapsed = (now.getTime() - lastLogin.getTime()) / (1000 * 60 * 60 * 24); 
						
						GriefPrevention.sendMessage(player, TextMode.Info, Messages.PlayerOfflineTime, String.valueOf(daysElapsed));
						
						//drop the data we just loaded, if the player isn't online
						if(GriefPrevention.instance.getServer().getPlayerExact(claim.getOwnerName()) == null)
							this.dataStore.clearCachedPlayerData(claim.getOwnerName());
					}
				}
				
				return;
			}
			
			//if it's a golden shovel
			else if(materialInHand != GriefPrevention.instance.config_claims_modificationTool) return;
			
			//disable golden shovel while under siege
			if(playerData.siegeData != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeNoShovel);
				event.setCancelled(true);
				return;
			}
			
			//can't use the shovel from too far away
			if(clickedBlockType == Material.AIR)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.TooFarAway);
				return;
			}
			
			//if the player is in restore nature mode, do only that
			String playerName = player.getName();
			playerData = this.dataStore.getPlayerData(player.getName());
			if(playerData.shovelMode == ShovelMode.RestoreNature || playerData.shovelMode == ShovelMode.RestoreNatureAggressive)
			{
				//if the clicked block is in a claim, visualize that claim and deliver an error message
				Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
				if(claim != null)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.BlockClaimed, claim.getOwnerName());
					Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.ErrorClaim, player.getLocation());
					Visualization.Apply(player, visualization);
					
					return;
				}
				
				//figure out which chunk to repair
				Chunk chunk = player.getWorld().getChunkAt(clickedBlock.getLocation());
				
				//start the repair process
				
				//set boundaries for processing
				int miny = clickedBlock.getY();
				
				//if not in aggressive mode, extend the selection down to a little below sea level
				if(!(playerData.shovelMode == ShovelMode.RestoreNatureAggressive))
				{
					if(miny > GriefPrevention.instance.getSeaLevel(chunk.getWorld()) - 10)
					{
						miny = GriefPrevention.instance.getSeaLevel(chunk.getWorld()) - 10;
					}
				}
				
				GriefPrevention.instance.restoreChunk(chunk, miny, playerData.shovelMode == ShovelMode.RestoreNatureAggressive, 0, player);
				
				return;
			}
			
			//if in restore nature fill mode
			if(playerData.shovelMode == ShovelMode.RestoreNatureFill)
			{
				ArrayList<Material> allowedFillBlocks = new ArrayList<Material>();				
				Environment environment = clickedBlock.getWorld().getEnvironment();
				if(environment == Environment.NETHER)
				{
					allowedFillBlocks.add(Material.NETHERRACK);
				}
				else if(environment == Environment.THE_END)
				{
					allowedFillBlocks.add(Material.ENDER_STONE);
				}			
				else
				{
					allowedFillBlocks.add(Material.GRASS);
					allowedFillBlocks.add(Material.DIRT);
					allowedFillBlocks.add(Material.STONE);
					allowedFillBlocks.add(Material.SAND);
					allowedFillBlocks.add(Material.SANDSTONE);
					allowedFillBlocks.add(Material.ICE);
				}
				
				Block centerBlock = clickedBlock;
				
				int maxHeight = centerBlock.getY();
				int minx = centerBlock.getX() - playerData.fillRadius;
				int maxx = centerBlock.getX() + playerData.fillRadius;
				int minz = centerBlock.getZ() - playerData.fillRadius;
				int maxz = centerBlock.getZ() + playerData.fillRadius;				
				int minHeight = maxHeight - 10;
				if(minHeight < 0) minHeight = 0;
				
				Claim cachedClaim = null;
				for(int x = minx; x <= maxx; x++)
				{
					for(int z = minz; z <= maxz; z++)
					{
						//circular brush
						Location location = new Location(centerBlock.getWorld(), x, centerBlock.getY(), z);
						if(location.distance(centerBlock.getLocation()) > playerData.fillRadius) continue;
						
						//default fill block is initially the first from the allowed fill blocks list above
						Material defaultFiller = allowedFillBlocks.get(0);
						
						//prefer to use the block the player clicked on, if it's an acceptable fill block
						if(allowedFillBlocks.contains(centerBlock.getType()))
						{
							defaultFiller = centerBlock.getType();
						}
						
						//if the player clicks on water, try to sink through the water to find something underneath that's useful for a filler
						else if(centerBlock.getType() == Material.WATER || centerBlock.getType() == Material.STATIONARY_WATER)
						{
							Block block = centerBlock.getWorld().getBlockAt(centerBlock.getLocation());
							while(!allowedFillBlocks.contains(block.getType()) && block.getY() > centerBlock.getY() - 10)
							{
								block = block.getRelative(BlockFace.DOWN);
							}
							if(allowedFillBlocks.contains(block.getType()))
							{
								defaultFiller = block.getType();
							}
						}
						
						//fill bottom to top
						for(int y = minHeight; y <= maxHeight; y++)
						{
							Block block = centerBlock.getWorld().getBlockAt(x, y, z);
							
							//respect claims
							Claim claim = this.dataStore.getClaimAt(block.getLocation(), false, cachedClaim);
							if(claim != null)
							{
								cachedClaim = claim;
								break;
							}
							
							//only replace air, spilling water, snow, long grass
							if(block.getType() == Material.AIR || block.getType() == Material.SNOW || (block.getType() == Material.STATIONARY_WATER && block.getData() != 0) || block.getType() == Material.LONG_GRASS)
							{							
								//if the top level, always use the default filler picked above
								if(y == maxHeight)
								{
									block.setType(defaultFiller);
								}
								
								//otherwise look to neighbors for an appropriate fill block
								else
								{
									Block eastBlock = block.getRelative(BlockFace.EAST);
									Block westBlock = block.getRelative(BlockFace.WEST);
									Block northBlock = block.getRelative(BlockFace.NORTH);
									Block southBlock = block.getRelative(BlockFace.SOUTH);
									
									//first, check lateral neighbors (ideally, want to keep natural layers)
									if(allowedFillBlocks.contains(eastBlock.getType()))
									{
										block.setType(eastBlock.getType());
									}
									else if(allowedFillBlocks.contains(westBlock.getType()))
									{
										block.setType(westBlock.getType());
									}
									else if(allowedFillBlocks.contains(northBlock.getType()))
									{
										block.setType(northBlock.getType());
									}
									else if(allowedFillBlocks.contains(southBlock.getType()))
									{
										block.setType(southBlock.getType());
									}
									
									//if all else fails, use the default filler selected above
									else
									{
										block.setType(defaultFiller);
									}
								}
							}
						}
					}
				}
				
				return;
			}
			
			//if the player doesn't have claims permission, don't do anything
			if(GriefPrevention.instance.config_claims_creationRequiresPermission && !player.hasPermission("griefprevention.createclaims"))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoCreateClaimPermission);
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
						GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeClaimTooSmall, String.valueOf(GriefPrevention.instance.config_claims_minSize));
						return;
					}
					
					//make sure player has enough blocks to make up the difference
					if(!playerData.claimResizing.isAdminClaim() && player.getName().equals(playerData.claimResizing.getOwnerName()))
					{
						int newArea =  newWidth * newHeight;
						int blocksRemainingAfter = playerData.getRemainingClaimBlocks() + playerData.claimResizing.getArea() - newArea;
						
						if(blocksRemainingAfter < 0)
						{
							GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeNeedMoreBlocks, String.valueOf(Math.abs(blocksRemainingAfter)));
							return;
						}
					}
				}
				
				//special rules for making a top-level claim smaller.  to check this, verifying the old claim's corners are inside the new claim's boundaries.
				//rule1: in creative mode, top-level claims can't be moved or resized smaller.
				//rule2: in any mode, shrinking a claim removes any surface fluids
				Claim oldClaim = playerData.claimResizing;
				boolean smaller = false;
				if(oldClaim.parent == null)
				{				
					//temporary claim instance, just for checking contains()
					Claim newClaim = new Claim(
							new Location(oldClaim.getLesserBoundaryCorner().getWorld(), newx1, newy1, newz1), 
							new Location(oldClaim.getLesserBoundaryCorner().getWorld(), newx2, newy2, newz2),
							"", new String[]{}, new String[]{}, new String[]{}, new String[]{}, null);
					
					//if the new claim is smaller
					if(!newClaim.contains(oldClaim.getLesserBoundaryCorner(), true, false) || !newClaim.contains(oldClaim.getGreaterBoundaryCorner(), true, false))
					{
						smaller = true;
						
						//enforce creative mode rule
						if(!GriefPrevention.instance.config_claims_allowUnclaimInCreative && !player.hasPermission("griefprevention.deleteclaims") && GriefPrevention.instance.creativeRulesApply(player.getLocation()))
						{
							GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoCreativeUnClaim);
							return;
						}
						
						//remove surface fluids about to be unclaimed
						oldClaim.removeSurfaceFluids(newClaim);
					}
				}
				
				//ask the datastore to try and resize the claim, this checks for conflicts with other claims
				CreateClaimResult result = GriefPrevention.instance.dataStore.resizeClaim(playerData.claimResizing, newx1, newx2, newy1, newy2, newz1, newz2);
				
				if(result.succeeded)
				{
					//inform and show the player
					GriefPrevention.sendMessage(player, TextMode.Success, Messages.ClaimResizeSuccess, String.valueOf(playerData.getRemainingClaimBlocks()));
					Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.Claim, player.getLocation());
					Visualization.Apply(player, visualization);
					
					//if resizing someone else's claim, make a log entry
					if(!playerData.claimResizing.ownerName.equals(playerName))
					{
						GriefPrevention.AddLogEntry(playerName + " resized " + playerData.claimResizing.getOwnerName() + "'s claim at " + GriefPrevention.getfriendlyLocationString(playerData.claimResizing.lesserBoundaryCorner) + ".");
					}
					
					//if in a creative mode world and shrinking an existing claim, restore any unclaimed area
					if(smaller && GriefPrevention.instance.config_claims_autoRestoreUnclaimedCreativeLand && GriefPrevention.instance.creativeRulesApply(oldClaim.getLesserBoundaryCorner()))
					{
						GriefPrevention.sendMessage(player, TextMode.Warn, Messages.UnclaimCleanupWarning);
						GriefPrevention.instance.restoreClaim(oldClaim, 20L * 60 * 2);  //2 minutes
						GriefPrevention.AddLogEntry(player.getName() + " shrank a claim @ " + GriefPrevention.getfriendlyLocationString(playerData.claimResizing.getLesserBoundaryCorner()));
					}
					
					//clean up
					playerData.claimResizing = null;
					playerData.lastShovelLocation = null;
				}
				else
				{
					//inform player
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeFailOverlap);
					
					//show the player the conflicting claim
					Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.ErrorClaim, player.getLocation());
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
						GriefPrevention.sendMessage(player, TextMode.Instr, Messages.ResizeStart);
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
								GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeFailOverlapSubdivision);							
							}
						
							//otherwise start a new subdivision
							else
							{
								GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SubdivisionStart);
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
									playerData.claimSubdividing,
									null);
							
							//if it didn't succeed, tell the player why
							if(!result.succeeded)
							{
								GriefPrevention.sendMessage(player, TextMode.Err, Messages.CreateSubdivisionOverlap);
																				
								Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.ErrorClaim, player.getLocation());
								Visualization.Apply(player, visualization);
								
								return;
							}
							
							//otherwise, advise him on the /trust command and show him his new subdivision
							else
							{					
								GriefPrevention.sendMessage(player, TextMode.Success, Messages.SubdivisionSuccess);
								Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.Claim, player.getLocation());
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
						GriefPrevention.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlap);
						Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.Claim, player.getLocation());
						Visualization.Apply(player, visualization);
					}
				}
				
				//otherwise tell the player he can't claim here because it's someone else's claim, and show him the claim
				else
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlapOtherPlayer, claim.getOwnerName());
					Visualization visualization = Visualization.FromClaim(claim, clickedBlock.getY(), VisualizationType.ErrorClaim, player.getLocation());
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
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClaimsDisabledWorld);
					return;
				}
				
				//remember it, and start him on the new claim
				playerData.lastShovelLocation = clickedBlock.getLocation();
				GriefPrevention.sendMessage(player, TextMode.Instr, Messages.ClaimStart);
				
				//show him where he's working
				Visualization visualization = Visualization.FromClaim(new Claim(clickedBlock.getLocation(), clickedBlock.getLocation(), "", new String[]{}, new String[]{}, new String[]{}, new String[]{}, null), clickedBlock.getY(), VisualizationType.RestoreNature, player.getLocation());
				Visualization.Apply(player, visualization);
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
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.NewClaimTooSmall, String.valueOf(GriefPrevention.instance.config_claims_minSize));
					return;
				}
				
				//if not an administrative claim, verify the player has enough claim blocks for this new claim
				if(playerData.shovelMode != ShovelMode.Admin)
				{					
					int newClaimArea = newClaimWidth * newClaimHeight; 
					int remainingBlocks = playerData.getRemainingClaimBlocks();
					if(newClaimArea > remainingBlocks)
					{
						GriefPrevention.sendMessage(player, TextMode.Err, Messages.CreateClaimInsufficientBlocks, String.valueOf(newClaimArea - remainingBlocks));
						GriefPrevention.sendMessage(player, TextMode.Instr, Messages.AbandonClaimAdvertisement);
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
						null, null);
				
				//if it didn't succeed, tell the player why
				if(!result.succeeded)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlapShort);
					
					Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.ErrorClaim, player.getLocation());
					Visualization.Apply(player, visualization);
					
					return;
				}
				
				//otherwise, advise him on the /trust command and show him his new claim
				else
				{					
					GriefPrevention.sendMessage(player, TextMode.Success, Messages.CreateClaimSuccess);
					Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.Claim, player.getLocation());
					Visualization.Apply(player, visualization);
					playerData.lastShovelLocation = null;
				}
			}
		}
	}	
}
