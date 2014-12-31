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
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
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
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.BlockIterator;

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
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	synchronized void onPlayerChat (AsyncPlayerChatEvent event)
	{		
		Player player = event.getPlayer();
		if(!player.isOnline())
		{
			event.setCancelled(true);
			return;
		}
		
		String message = event.getMessage();
		
		boolean muted = this.handlePlayerChat(player, message, event);
		Set<Player> recipients = event.getRecipients();
		
		//muted messages go out to only the sender
		if(muted)
		{
		    recipients.clear();
		    recipients.add(player);
		}
		
		//soft muted messages go out to all soft muted players
		else if(this.dataStore.isSoftMuted(player.getUniqueId()))
		{
		    Set<Player> recipientsToKeep = new HashSet<Player>();
		    for(Player recipient : recipients)
		    {
		        if(this.dataStore.isSoftMuted(recipient.getUniqueId()))
		        {
		            recipientsToKeep.add(recipient);
		        }
		        else if(recipient.hasPermission("griefprevention.eavesdrop"))
		        {
		            recipient.sendMessage(ChatColor.GRAY + "(Muted " + player.getName() + "): " + message);
		        }
		    }
		    recipients.clear();
		    recipients.addAll(recipientsToKeep);
		}
	}
	
	//last chat message shown, regardless of who sent it
	private String lastChatMessage = "";
	private long lastChatMessageTimestamp = 0;
	
	//number of identical messages in a row
	private int duplicateMessageCount = 0;
	
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
				GriefPrevention.sendMessage(player, TextMode.Info, Messages.CreativeBasicsVideo2, 10L, DataStore.CREATIVE_VIDEO_URL);
			}
			else
			{
				GriefPrevention.sendMessage(player, TextMode.Info, Messages.SurvivalBasicsVideo2, 10L, DataStore.SURVIVAL_VIDEO_URL);
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
		
		PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
		
		//remedy any CAPS SPAM, exception for very short messages which could be emoticons like =D or XD
		if(message.length() > 4 && this.stringsAreSimilar(message.toUpperCase(), message))
		{
			//exception for strings containing forward slash to avoid changing a case-sensitive URL
			if(event instanceof AsyncPlayerChatEvent && !message.contains("/"))
			{
				((AsyncPlayerChatEvent)event).setMessage(message.toLowerCase());
			}
		}
		
		//always mute an exact match to the last chat message
		long now = new Date().getTime();
		if(message.equals(this.lastChatMessage) && now - this.lastChatMessageTimestamp < 750)
		{
		    playerData.spamCount += ++this.duplicateMessageCount;
		    spam = true;
		    muted = true;
		}
		else
		{
		    this.lastChatMessage = message;
		    this.lastChatMessageTimestamp = now;
		    this.duplicateMessageCount = 0;
		}
		
		//where other types of spam are concerned, casing isn't significant
		message = message.toLowerCase();
		
		//check message content and timing		
		long millisecondsSinceLastMessage = now - playerData.lastMessageTimestamp.getTime();
		
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
		int i;
		for(i = 0; i < shorterString.length(); i++)
		{
			if(shorterString.charAt(i) == longerString.charAt(i)) identicalCount++;
			if(identicalCount > maxIdenticalCharacters) return true;
		}
		
		//compare backward
		int j;
		for(j = 0; j < shorterString.length() - i; j++)
		{
			if(shorterString.charAt(shorterString.length() - j - 1) == longerString.charAt(longerString.length() - j - 1)) identicalCount++;
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
		if(GriefPrevention.instance.config_whisperNotifications && GriefPrevention.instance.config_eavesdrop_whisperCommands.contains(command) && !event.getPlayer().hasPermission("griefprevention.eavesdrop") && args.length > 1)
		{			
			StringBuilder logMessageBuilder = new StringBuilder();
			logMessageBuilder.append("[[").append(event.getPlayer().getName()).append("]] ");
			
			for(int i = 1; i < args.length; i++)
			{
				logMessageBuilder.append(args[i]).append(" ");
			}
			
			String logMessage = logMessageBuilder.toString();
			
			Collection<Player> players = (Collection<Player>)GriefPrevention.instance.getServer().getOnlinePlayers();
			for(Player player : players)
			{
				if(player.hasPermission("griefprevention.eavesdrop") && !player.getName().equalsIgnoreCase(args[1]))
				{
					player.sendMessage(ChatColor.GRAY + logMessage);
				}
			}
		}
		
		//if in pvp, block any pvp-banned slash commands
		PlayerData playerData = this.dataStore.getPlayerData(event.getPlayer().getUniqueId());
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
	
	private ConcurrentHashMap<UUID, Date> lastLoginThisServerSessionMap = new ConcurrentHashMap<UUID, Date>();
	
	//when a player attempts to join the server...
	@EventHandler(priority = EventPriority.HIGHEST)
	void onPlayerLogin (PlayerLoginEvent event)
	{
		Player player = event.getPlayer();
		
		//all this is anti-spam code
		if(GriefPrevention.instance.config_spam_enabled)
		{
			//FEATURE: login cooldown to prevent login/logout spam with custom clients
		    long now = Calendar.getInstance().getTimeInMillis();
		    
			//if allowed to join and login cooldown enabled
			if(GriefPrevention.instance.config_spam_loginCooldownSeconds > 0 && event.getResult() == Result.ALLOWED && !player.hasPermission("griefprevention.spam"))
			{
				//determine how long since last login and cooldown remaining
				Date lastLoginThisSession = lastLoginThisServerSessionMap.get(player.getUniqueId());
				if(lastLoginThisSession != null)
				{
    			    long millisecondsSinceLastLogin = now - lastLoginThisSession.getTime();
    				long secondsSinceLastLogin = millisecondsSinceLastLogin / 1000;
    				long cooldownRemaining = GriefPrevention.instance.config_spam_loginCooldownSeconds - secondsSinceLastLogin;
    				
    				//if cooldown remaining
    				if(cooldownRemaining > 0)
    				{
    					//DAS BOOT!
    					event.setResult(Result.KICK_OTHER);				
    					event.setKickMessage("You must wait " + cooldownRemaining + " seconds before logging-in again.");
    					event.disallow(event.getResult(), event.getKickMessage());
    					return;
    				}
				}
			}
			
			//if logging-in account is banned, remember IP address for later
			if(GriefPrevention.instance.config_smartBan && event.getResult() == Result.KICK_BANNED)
			{
				this.tempBannedIps.add(new IpBanInfo(event.getAddress(), now + this.MILLISECONDS_IN_DAY, player.getName()));
			}
		}
		
		//remember the player's ip address
		PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
		playerData.ipAddress = event.getAddress();
	}
	
	//when a player successfully joins the server...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	void onPlayerJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();
		UUID playerID = player.getUniqueId();
		
		//note login time
		Date nowDate = new Date();
        long now = nowDate.getTime();
		PlayerData playerData = this.dataStore.getPlayerData(playerID);
		playerData.lastSpawn = now;
		playerData.setLastLogin(nowDate);
		this.lastLoginThisServerSessionMap.put(playerID, nowDate);
		
		//if player has never played on the server before...
		if(!player.hasPlayedBefore())
		{
			//may need pvp protection
		    GriefPrevention.instance.checkPvpProtectionNeeded(player);
		    
		    //if in survival claims mode, send a message about the claim basics video (except for admins - assumed experts)
		    if(GriefPrevention.instance.config_claims_worldModes.get(player.getWorld()) == ClaimsMode.Survival && !player.hasPermission("griefprevention.adminclaims") && this.dataStore.claims.size() > 10)
		    {
		        GriefPrevention.sendMessage(player, TextMode.Instr, Messages.AvoidGriefClaimLand, 600L);
		        GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SurvivalBasicsVideo2, 601L, DataStore.SURVIVAL_VIDEO_URL);
		    }
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
						Collection<Player> players = (Collection<Player>)GriefPrevention.instance.getServer().getOnlinePlayers();
						for(Player otherPlayer : players)
						{
							if(otherPlayer.isOp())
							{
								GriefPrevention.sendMessage(otherPlayer, TextMode.Success, Messages.AutoBanNotify, player.getName(), info.bannedAccountName);
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
		
		//in case player has changed his name, on successful login, update UUID > Name mapping
		GriefPrevention.cacheUUIDNamePair(player.getUniqueId(), player.getName());
	}
	
	//when a player spawns, conditionally apply temporary pvp protection 
    @EventHandler(ignoreCancelled = true)
    void onPlayerRespawn (PlayerRespawnEvent event)
    {
        Player player = event.getPlayer();
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
        playerData.lastSpawn = Calendar.getInstance().getTimeInMillis();
        
        //also send him any messaged from grief prevention he would have received while dead
        if(playerData.messageOnRespawn != null)
        {
            GriefPrevention.sendMessage(player, ChatColor.RESET /*color is alrady embedded in message in this case*/, playerData.messageOnRespawn, 40L);
            playerData.messageOnRespawn = null;
        }
        
        GriefPrevention.instance.checkPvpProtectionNeeded(player);
    }
	
	//when a player dies...
	@EventHandler(priority = EventPriority.LOWEST)
	void onPlayerDeath(PlayerDeathEvent event)
	{
		//FEATURE: prevent death message spam by implementing a "cooldown period" for death messages
		PlayerData playerData = this.dataStore.getPlayerData(event.getEntity().getUniqueId());
		long now = Calendar.getInstance().getTimeInMillis(); 
		if(now - playerData.lastDeathTimeStamp < GriefPrevention.instance.config_spam_deathMessageCooldownSeconds * 1000)
		{
			event.setDeathMessage("");
		}
		
		playerData.lastDeathTimeStamp = now;
		
		//these are related to locking dropped items on death to prevent theft
		playerData.dropsAreUnlocked = false;
		playerData.receivedDropUnlockAdvertisement = false;
	}
	
	//when a player gets kicked...
	@EventHandler(priority = EventPriority.HIGHEST)
	void onPlayerKicked(PlayerKickEvent event)
    {
	    Player player = event.getPlayer();
	    PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
	    playerData.wasKicked = true;
    }
	
	//when a player quits...
	@EventHandler(priority = EventPriority.HIGHEST)
	void onPlayerQuit(PlayerQuitEvent event)
	{
	    Player player = event.getPlayer();
		UUID playerID = player.getUniqueId();
	    PlayerData playerData = this.dataStore.getPlayerData(playerID);
		boolean isBanned;
		if(playerData.wasKicked)
		{
		    isBanned = player.isBanned();
		}
		else
		{
		    isBanned = false;
		}
		
		//if banned, add IP to the temporary IP ban list
		if(isBanned && playerData.ipAddress != null)
		{
			long now = Calendar.getInstance().getTimeInMillis(); 
			this.tempBannedIps.add(new IpBanInfo(playerData.ipAddress, now + this.MILLISECONDS_IN_DAY, player.getName()));
		}
		
		//silence notifications when they're coming too fast
		if(event.getQuitMessage() != null && this.shouldSilenceNotification())
		{
			event.setQuitMessage(null);
		}
		
		//silence notifications when the player is banned
		if(isBanned)
		{
		    event.setQuitMessage(null);
		}
		
		//make sure his data is all saved - he might have accrued some claim blocks while playing that were not saved immediately
		else
		{
		    this.dataStore.savePlayerData(player.getUniqueId(), playerData);
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
        this.dataStore.clearCachedPlayerData(playerID);
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
		
		PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
		
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
	
	//when a player teleports via a portal
	@EventHandler(priority = EventPriority.HIGHEST)
	void onPlayerPortal(PlayerPortalEvent event) 
	{
	    Player player = event.getPlayer();
        
	    //FEATURE: when players get trapped in a nether portal, send them back through to the other side
        if(event.getCause() == TeleportCause.NETHER_PORTAL)
        {
            CheckForPortalTrapTask task = new CheckForPortalTrapTask(player, event.getFrom());
            GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task, 100L);
        }
        
        //FEATURE: if the player teleporting doesn't have permission to build a nether portal and none already exists at the destination, redirect the portal
        Location existingPortalLocation = event.getPortalTravelAgent().findPortal(event.getTo());
        boolean creatingPortal = event.getPortalTravelAgent().getCanCreatePortal() && (existingPortalLocation == null);
        
        //if creating a new portal
        if(creatingPortal)
        {
            //and it goes to a land claim
            Claim claim = this.dataStore.getClaimAt(event.getTo(), false, null);
            while(claim != null && claim.allowBuild(player, Material.PORTAL) != null)
            {
                //redirect to outside the land claim
                event.setTo(claim.getLesserBoundaryCorner().clone().add(-50, 0, -50));
                claim = this.dataStore.getClaimAt(event.getTo(), false, null);
            }
        }
	}
	
	//when a player teleports
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerTeleport(PlayerTeleportEvent event)
	{
	    Player player = event.getPlayer();
		PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
		
		//FEATURE: prevent players from using ender pearls to gain access to secured claims
		if(event.getCause() == TeleportCause.ENDER_PEARL && GriefPrevention.instance.config_claims_enderPearlsRequireAccessTrust)
		{
			Claim toClaim = this.dataStore.getClaimAt(event.getTo(), false, playerData.lastClaim);
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
		
		//these rules only apply to siege worlds only
		if(!GriefPrevention.instance.config_siege_enabledWorlds.contains(player.getWorld())) return;
		
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
	
	//when a player interacts with a specific part of entity...
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event)
    {
        //treat it the same as interacting with an entity in general
        if(event.getRightClicked().getType() == EntityType.ARMOR_STAND)
        {
            this.onPlayerInteractEntity((PlayerInteractEntityEvent)event);
        }
    }
    
	//when a player interacts with an entity...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event)
	{
		Player player = event.getPlayer();
		Entity entity = event.getRightClicked();
		
		if(!GriefPrevention.instance.claimsEnabledForWorld(entity.getWorld())) return;
		
		//allow horse protection to be overridden to allow management from other plugins
        if (!GriefPrevention.instance.config_claims_protectHorses && entity instanceof Horse) return;
        
        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
        
		//if entity is tameable and has an owner, apply special rules
        if(entity instanceof Tameable && !GriefPrevention.instance.config_pvp_enabledWorlds.contains(entity.getLocation().getWorld()))
        {
            Tameable tameable = (Tameable)entity;
            if(tameable.isTamed() && tameable.getOwner() != null)
            {
               UUID ownerID = tameable.getOwner().getUniqueId();
               
               //if the player interacting is the owner or an admin in ignore claims mode, always allow
               if(player.getUniqueId().equals(ownerID) || playerData.ignoreClaims)
               {
                   //if giving away pet, do that instead
                   if(playerData.petGiveawayRecipient != null)
                   {
                       tameable.setOwner(playerData.petGiveawayRecipient);
                       playerData.petGiveawayRecipient = null;
                       GriefPrevention.sendMessage(player, TextMode.Success, Messages.PetGiveawayConfirmation);
                       event.setCancelled(true);
                   }
                   
                   return;
               }
               
               //otherwise disallow
               OfflinePlayer owner = GriefPrevention.instance.getServer().getOfflinePlayer(ownerID); 
               String ownerName = owner.getName();
               if(ownerName == null) ownerName = "someone";
               String message = GriefPrevention.instance.dataStore.getMessage(Messages.NotYourPet, ownerName);
               if(player.hasPermission("griefprevention.ignoreclaims"))
                   message += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
               GriefPrevention.sendMessage(player, TextMode.Err, message);
               event.setCancelled(true);
               return;
            }
        }
        
        //don't allow interaction with item frames or armor stands in claimed areas without build permission
		if(entity.getType() == EntityType.ARMOR_STAND || entity instanceof Hanging)
		{
			String noBuildReason = GriefPrevention.instance.allowBuild(player, entity.getLocation(), Material.ITEM_FRAME); 
			if(noBuildReason != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
				event.setCancelled(true);
				return;
			}			
		}
		
		//always allow interactions when player is in ignore claims mode
        if(playerData.ignoreClaims) return;
        
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
				//for storage entities, apply container rules (this is a potential theft)
				if(entity instanceof InventoryHolder)
				{					
					String noContainersReason = claim.allowContainers(player);
					if(noContainersReason != null)
					{
						GriefPrevention.sendMessage(player, TextMode.Err, noContainersReason);
						event.setCancelled(true);
						return;
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
						return;
					}
				}
			}
		}
		
		//if the entity is an animal, apply container rules
        if(GriefPrevention.instance.config_claims_preventTheft && entity instanceof Animals)
        {
            //if the entity is in a claim
            Claim claim = this.dataStore.getClaimAt(entity.getLocation(), false, null);
            if(claim != null)
            {
                if(claim.allowContainers(player) != null)
                {
                    String message = GriefPrevention.instance.dataStore.getMessage(Messages.NoDamageClaimedEntity, claim.getOwnerName());
                    if(player.hasPermission("griefprevention.ignoreclaims"))
                        message += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
                    GriefPrevention.sendMessage(player, TextMode.Err, message);
                    event.setCancelled(true);
                    return;
                }
            }
        }
		
		//if preventing theft, prevent leashing claimed creatures
		if(GriefPrevention.instance.config_claims_preventTheft && entity instanceof Creature && player.getItemInHand().getType() == Material.LEASH)
		{
		    Claim claim = this.dataStore.getClaimAt(entity.getLocation(), false, playerData.lastClaim);
            if(claim != null)
            {
                String failureReason = claim.allowContainers(player);
                if(failureReason != null)
                {
                    event.setCancelled(true);
                    GriefPrevention.sendMessage(player, TextMode.Err, failureReason);
                    return;                    
                }
            }
		}
	}
	
	//when a player picks up an item...
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerPickupItem(PlayerPickupItemEvent event)
	{
		Player player = event.getPlayer();
		
		//FEATURE: lock dropped items to player who dropped them
		
		//who owns this stack?
		Item item = event.getItem();
		List<MetadataValue> data = item.getMetadata("GP_ITEMOWNER");
		if(data != null && data.size() > 0)
		{
		    UUID ownerID = (UUID)data.get(0).value();
		    
		    //has that player unlocked his drops?
		    OfflinePlayer owner = GriefPrevention.instance.getServer().getOfflinePlayer(ownerID);
		    String ownerName = GriefPrevention.lookupPlayerName(ownerID);
		    if(owner.isOnline() && !player.equals(owner))
		    {
		        PlayerData playerData = this.dataStore.getPlayerData(ownerID);

                //if locked, don't allow pickup
		        if(!playerData.dropsAreUnlocked)
		        {
		            event.setCancelled(true);
		            
		            //if hasn't been instructed how to unlock, send explanatory messages
		            if(!playerData.receivedDropUnlockAdvertisement)
		            {
		                GriefPrevention.sendMessage(owner.getPlayer(), TextMode.Instr, Messages.DropUnlockAdvertisement);
		                GriefPrevention.sendMessage(player, TextMode.Err, Messages.PickupBlockedExplanation, ownerName);
		                playerData.receivedDropUnlockAdvertisement = true;
		            }
		        }
		    }
		}
		
		//the rest of this code is specific to pvp worlds
		if(!GriefPrevention.instance.config_pvp_enabledWorlds.contains(player.getWorld())) return;
		
		//if we're preventing spawn camping and the player was previously empty handed...
		if(GriefPrevention.instance.config_pvp_protectFreshSpawns && (player.getItemInHand().getType() == Material.AIR))
		{
			//if that player is currently immune to pvp
			PlayerData playerData = this.dataStore.getPlayerData(event.getPlayer().getUniqueId());
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
			PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
		
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
	
	//block use of buckets within other players' claims
	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
	public void onPlayerBucketEmpty (PlayerBucketEmptyEvent bucketEvent)
	{
		if(!GriefPrevention.instance.claimsEnabledForWorld(bucketEvent.getBlockClicked().getWorld())) return;
	    
	    Player player = bucketEvent.getPlayer();
		Block block = bucketEvent.getBlockClicked().getRelative(bucketEvent.getBlockFace());
		int minLavaDistance = 10;
		
		//make sure the player is allowed to build at the location
		String noBuildReason = GriefPrevention.instance.allowBuild(player, block.getLocation(), Material.WATER);
		if(noBuildReason != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
			bucketEvent.setCancelled(true);
			return;
		}
		
		//if the bucket is being used in a claim, allow for dumping lava closer to other players
		PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
		Claim claim = this.dataStore.getClaimAt(block.getLocation(), false, playerData.lastClaim);
		if(claim != null)
		{
			minLavaDistance = 3;
		}
		
		//otherwise no wilderness dumping in creative mode worlds
		else if(GriefPrevention.instance.creativeRulesApply(block.getLocation()))
		{
			if(block.getY() >= GriefPrevention.instance.getSeaLevel(block.getWorld()) - 5 && !player.hasPermission("griefprevention.lava"))
			{
				if(bucketEvent.getBucket() == Material.LAVA_BUCKET)
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
		
		if(!GriefPrevention.instance.claimsEnabledForWorld(block.getWorld())) return;
		
		//make sure the player is allowed to build at the location
		String noBuildReason = GriefPrevention.instance.allowBuild(player, block.getLocation(), Material.AIR);
		if(noBuildReason != null)
		{
		    //exemption for cow milking (permissions will be handled by player interact with entity event instead)
		    Material blockType = block.getType();
		    if(blockType == Material.AIR || blockType.isSolid()) return;
		    
			GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
			bucketEvent.setCancelled(true);
			return;
		}
	}
	
	//when a player interacts with the world
	@EventHandler(priority = EventPriority.LOWEST)
	void onPlayerInteract(PlayerInteractEvent event)
	{
	    //not interested in left-click-on-air actions
	    Action action = event.getAction();
	    if(action == Action.LEFT_CLICK_AIR) return;
        
	    Player player = event.getPlayer();
		Block clickedBlock = event.getClickedBlock(); //null returned here means interacting with air
		
		Material clickedBlockType = null;
		if(clickedBlock != null)
		{
		    clickedBlockType = clickedBlock.getType();
		}
		else
		{
		    clickedBlockType = Material.AIR;
		}
		
		//apply rule for players trampling tilled soil back to dirt (never allow it)
        //NOTE: that this event applies only to players.  monsters and animals can still trample.
        if(action == Action.PHYSICAL)
        {
            if(clickedBlockType == Material.SOIL)
            {
                event.setCancelled(true);
            }

            //not tracking any other "physical" interaction events right now
            return;
        }
        
        //don't care about left-clicking on most blocks, this is probably a break action
        PlayerData playerData = null;
        if(action == Action.LEFT_CLICK_BLOCK && clickedBlock != null)
        {
            //exception for blocks on a specific watch list
            if(!this.onLeftClickWatchList(clickedBlockType) && !GriefPrevention.instance.config_mods_accessTrustIds.Contains(new MaterialInfo(clickedBlock.getTypeId(), clickedBlock.getData(), null)))
            {
                //and an exception for putting our fires
                if(GriefPrevention.instance.config_claims_protectFires && event.getClickedBlock() != null)
                {
                    Block adjacentBlock = event.getClickedBlock().getRelative(event.getBlockFace());
                    if(adjacentBlock.getType() == Material.FIRE)
                    {
                        if(playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
                        Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
                        if(claim != null)
                        {
                            playerData.lastClaim = claim;
                            
                            String noBuildReason = claim.allowBuild(player, Material.AIR);
                            if(noBuildReason != null)
                            {
                                event.setCancelled(true);
                                GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
                                player.sendBlockChange(adjacentBlock.getLocation(), adjacentBlock.getTypeId(), adjacentBlock.getData());
                                return;
                            }
                        }
                    }
                }
                
                return;
            }
        }
        
		//apply rules for containers and crafting blocks
		if(	clickedBlock != null && GriefPrevention.instance.config_claims_preventTheft && (
						event.getAction() == Action.RIGHT_CLICK_BLOCK && (
						this.isInventoryHolder(clickedBlock) ||
						clickedBlockType == Material.ANVIL ||
						GriefPrevention.instance.config_mods_containerTrustIds.Contains(new MaterialInfo(clickedBlock.getTypeId(), clickedBlock.getData(), null)))))
		{			
		    if(playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
		    
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
		
		//otherwise apply rules for doors and beds, if configured that way
		else if( clickedBlock != null && 
		        
		        (GriefPrevention.instance.config_claims_lockWoodenDoors && (
	                        clickedBlockType == Material.WOODEN_DOOR   ||
	                        clickedBlockType == Material.ACACIA_DOOR   || 
	                        clickedBlockType == Material.BIRCH_DOOR    ||
	                        clickedBlockType == Material.JUNGLE_DOOR   ||
                            clickedBlockType == Material.SPRUCE_DOOR   ||
	                        clickedBlockType == Material.DARK_OAK_DOOR)) ||
		        
                (GriefPrevention.instance.config_claims_preventButtonsSwitches && clickedBlockType == Material.BED_BLOCK) ||
		        
                (GriefPrevention.instance.config_claims_lockTrapDoors && (
		                    clickedBlockType == Material.TRAP_DOOR)) ||
				
                (GriefPrevention.instance.config_claims_lockFenceGates && (
    				        clickedBlockType == Material.FENCE_GATE          ||
    				        clickedBlockType == Material.ACACIA_FENCE_GATE   || 
                            clickedBlockType == Material.BIRCH_FENCE_GATE    ||
                            clickedBlockType == Material.JUNGLE_FENCE_GATE   ||
                            clickedBlockType == Material.SPRUCE_FENCE_GATE   ||
                            clickedBlockType == Material.DARK_OAK_FENCE_GATE)))
		{
		    if(playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
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
		else if(clickedBlock != null && GriefPrevention.instance.config_claims_preventButtonsSwitches && (clickedBlockType == null || clickedBlockType == Material.STONE_BUTTON || clickedBlockType == Material.WOOD_BUTTON || clickedBlockType == Material.LEVER || GriefPrevention.instance.config_mods_accessTrustIds.Contains(new MaterialInfo(clickedBlock.getTypeId(), clickedBlock.getData(), null))))
		{
		    if(playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
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
		
		//apply rule for note blocks and repeaters
		else if(clickedBlock != null && clickedBlockType == Material.NOTE_BLOCK || clickedBlockType == Material.DIODE_BLOCK_ON || clickedBlockType == Material.DIODE_BLOCK_OFF)
		{
		    if(playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
		    Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
			if(claim != null)
			{
				String noBuildReason = claim.allowBuild(player, clickedBlockType);
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
			if(action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) return;
			
			//what's the player holding?
			ItemStack itemInHand = player.getItemInHand();
			Material materialInHand = itemInHand.getType();		
			
			//if it's bonemeal or armor stand, check for build permission (ink sac == bone meal, must be a Bukkit bug?)
			if(clickedBlock != null && (materialInHand == Material.INK_SACK || materialInHand == Material.ARMOR_STAND))
			{
				String noBuildReason = GriefPrevention.instance.allowBuild(player, clickedBlock.getLocation(), clickedBlockType);
				if(noBuildReason != null)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
					event.setCancelled(true);
				}
				
				return;
			}
			
			else if(clickedBlock != null && materialInHand ==  Material.BOAT)
			{
			    if(playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
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
			else if(clickedBlock != null && (materialInHand == Material.MONSTER_EGG || materialInHand == Material.MINECART || materialInHand == Material.POWERED_MINECART || materialInHand == Material.STORAGE_MINECART || materialInHand == Material.BOAT) && GriefPrevention.instance.creativeRulesApply(clickedBlock.getLocation()))
			{
				//player needs build permission at this location
				String noBuildReason = GriefPrevention.instance.allowBuild(player, clickedBlock.getLocation(), Material.MINECART);
				if(noBuildReason != null)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
					event.setCancelled(true);
					return;
				}
			
				//enforce limit on total number of entities in this claim
				if(playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
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
		        //if holding shift (sneaking), show all claims in area
			    if(player.isSneaking() && player.hasPermission("griefprevention.visualizenearbyclaims"))
			    {
			        //find nearby claims
			        ArrayList<Claim> claims = this.dataStore.getNearbyClaims(player.getLocation());
			        
			        //visualize boundaries
                    Visualization visualization = Visualization.fromClaims(claims, player.getEyeLocation().getBlockY(), VisualizationType.Claim, player.getLocation());
                    Visualization.Apply(player, visualization);
                    
                    return;
			    }
			    
			    //FEATURE: shovel and stick can be used from a distance away
		        if(action == Action.RIGHT_CLICK_AIR)
		        {
		            //try to find a far away non-air block along line of sight
		            clickedBlock = getTargetBlock(player, 100);
		            clickedBlockType = clickedBlock.getType();
		        }           
		        
		        //if no block, stop here
		        if(clickedBlock == null)
		        {
		            return;
		        }
			    
			    //air indicates too far away
				if(clickedBlockType == Material.AIR)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.TooFarAway);
					Visualization.Revert(player);
					return;
				}
				
				if(playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
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
					Visualization visualization = Visualization.FromClaim(claim, player.getEyeLocation().getBlockY(), VisualizationType.Claim, player.getLocation());
					Visualization.Apply(player, visualization);
					
					//if can resize this claim, tell about the boundaries
					if(claim.allowEdit(player) == null)
					{
						GriefPrevention.sendMessage(player, TextMode.Info, "  " + claim.getWidth() + "x" + claim.getHeight() + "=" + claim.getArea());
					}
					
					//if deleteclaims permission, tell about the player's offline time
					if(!claim.isAdminClaim() && player.hasPermission("griefprevention.deleteclaims"))
					{
						if(claim.parent != null)
						{
						    claim = claim.parent;
						}
					    PlayerData otherPlayerData = this.dataStore.getPlayerData(claim.ownerID);
						Date lastLogin = otherPlayerData.getLastLogin();
						Date now = new Date();
						long daysElapsed = (now.getTime() - lastLogin.getTime()) / (1000 * 60 * 60 * 24); 
						
						GriefPrevention.sendMessage(player, TextMode.Info, Messages.PlayerOfflineTime, String.valueOf(daysElapsed));
						
						//drop the data we just loaded, if the player isn't online
						if(GriefPrevention.instance.getServer().getPlayer(claim.ownerID) == null)
							this.dataStore.clearCachedPlayerData(claim.ownerID);
					}
				}
				
				return;
			}
			
			//if holding a non-vanilla item
			else if(Material.getMaterial(itemInHand.getTypeId()) == null)
            {
                //assume it's a long range tool and project out ahead
                if(action == Action.RIGHT_CLICK_AIR)
                {
                    //try to find a far away non-air block along line of sight
                    clickedBlock = getTargetBlock(player, 100);
                }
                
                //if target is claimed, require build trust permission
                if(playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
                Claim claim = this.dataStore.getClaimAt(clickedBlock.getLocation(), false, playerData.lastClaim);
                if(claim != null)
                {
                    String reason = claim.allowBreak(player, Material.AIR);
                    if(reason != null)
                    {
                        GriefPrevention.sendMessage(player, TextMode.Err, reason);
                        event.setCancelled(true);
                        return;
                    }
                }
                
                return;
            }
			
			//if it's a golden shovel
			else if(materialInHand != GriefPrevention.instance.config_claims_modificationTool) return;
			
			//disable golden shovel while under siege
			if(playerData == null) playerData = this.dataStore.getPlayerData(player.getUniqueId());
			if(playerData.siegeData != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeNoShovel);
				event.setCancelled(true);
				return;
			}
			
			//FEATURE: shovel and stick can be used from a distance away
            if(action == Action.RIGHT_CLICK_AIR)
            {
                //try to find a far away non-air block along line of sight
                clickedBlock = getTargetBlock(player, 100);
                clickedBlockType = clickedBlock.getType();
            }           
            
            //if no block, stop here
            if(clickedBlock == null)
            {
                return;
            }
			
			//can't use the shovel from too far away
			if(clickedBlockType == Material.AIR)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.TooFarAway);
				return;
			}
			
			//if the player is in restore nature mode, do only that
			UUID playerID = player.getUniqueId();
			playerData = this.dataStore.getPlayerData(player.getUniqueId());
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
							this.tryAdvertiseAdminAlternatives(player);
							return;
						}
					}
				}
				
				//special rule for making a top-level claim smaller.  to check this, verifying the old claim's corners are inside the new claim's boundaries.
				//rule: in any mode, shrinking a claim removes any surface fluids
				Claim oldClaim = playerData.claimResizing;
				boolean smaller = false;
				if(oldClaim.parent == null)
				{				
					//temporary claim instance, just for checking contains()
					Claim newClaim = new Claim(
							new Location(oldClaim.getLesserBoundaryCorner().getWorld(), newx1, newy1, newz1), 
							new Location(oldClaim.getLesserBoundaryCorner().getWorld(), newx2, newy2, newz2),
							null, new String[]{}, new String[]{}, new String[]{}, new String[]{}, null);
					
					//if the new claim is smaller
					if(!newClaim.contains(oldClaim.getLesserBoundaryCorner(), true, false) || !newClaim.contains(oldClaim.getGreaterBoundaryCorner(), true, false))
					{
						smaller = true;
						
						//remove surface fluids about to be unclaimed
						oldClaim.removeSurfaceFluids(newClaim);
					}
				}
				
				//ask the datastore to try and resize the claim, this checks for conflicts with other claims
				CreateClaimResult result = GriefPrevention.instance.dataStore.resizeClaim(playerData.claimResizing, newx1, newx2, newy1, newy2, newz1, newz2, player);
				
				if(result.succeeded)
				{
					//inform and show the player
					GriefPrevention.sendMessage(player, TextMode.Success, Messages.ClaimResizeSuccess, String.valueOf(playerData.getRemainingClaimBlocks()));
					Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.Claim, player.getLocation());
					Visualization.Apply(player, visualization);
					
					//if resizing someone else's claim, make a log entry
					if(!playerID.equals(playerData.claimResizing.ownerID))
					{
						GriefPrevention.AddLogEntry(player.getName() + " resized " + playerData.claimResizing.getOwnerName() + "'s claim at " + GriefPrevention.getfriendlyLocationString(playerData.claimResizing.lesserBoundaryCorner) + ".");
					}
					
					//if increased to a sufficiently large size and no subdivisions yet, send subdivision instructions
					if(oldClaim.getArea() < 1000 && result.claim.getArea() >= 1000 && result.claim.children.size() == 0 && !player.hasPermission("griefprevention.adminclaims"))
					{
					  GriefPrevention.sendMessage(player, TextMode.Info, Messages.BecomeMayor, 200L);
	                  GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SubdivisionVideo2, 201L, DataStore.SUBDIVISION_VIDEO_URL);
					}
					
					//if in a creative mode world and shrinking an existing claim, restore any unclaimed area
					if(smaller && GriefPrevention.instance.creativeRulesApply(oldClaim.getLesserBoundaryCorner()))
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
					if(result.claim != null)
					{
    				    //inform player
    					GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeFailOverlap);
    					
    					//show the player the conflicting claim
    					Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.ErrorClaim, player.getLocation());
    					Visualization.Apply(player, visualization);
					}
					else
					{
					    GriefPrevention.sendMessage(player, TextMode.Err, Messages.ResizeFailOverlapRegion);
					}
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
									null,  //owner is not used for subdivisions
									playerData.claimSubdividing,
									null, player);
							
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
				if(!GriefPrevention.instance.claimsEnabledForWorld(player.getWorld()))
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClaimsDisabledWorld);
					return;
				}
				
				//if he's at the claim count per player limit already and doesn't have permission to bypass, display an error message
				if(GriefPrevention.instance.config_claims_maxClaimsPerPlayer > 0 &&
				   !player.hasPermission("griefprevention.overrideclaimcountlimit") &&
				   playerData.getClaims().size() >= GriefPrevention.instance.config_claims_maxClaimsPerPlayer)
				{
				    GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClaimCreationFailedOverClaimCountLimit);
				    return;
				}
				
				//remember it, and start him on the new claim
				playerData.lastShovelLocation = clickedBlock.getLocation();
				GriefPrevention.sendMessage(player, TextMode.Instr, Messages.ClaimStart);
				
				//show him where he's working
				Visualization visualization = Visualization.FromClaim(new Claim(clickedBlock.getLocation(), clickedBlock.getLocation(), null, new String[]{}, new String[]{}, new String[]{}, new String[]{}, null), clickedBlock.getY(), VisualizationType.RestoreNature, player.getLocation());
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
					//this IF block is a workaround for craftbukkit bug which fires two events for one interaction
				    if(newClaimWidth != 1 && newClaimHeight != 1)
				    {
				        GriefPrevention.sendMessage(player, TextMode.Err, Messages.NewClaimTooSmall, String.valueOf(GriefPrevention.instance.config_claims_minSize));
				    }
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
						this.tryAdvertiseAdminAlternatives(player);
						return;
					}
				}					
				else
				{
					playerID = null;
				}
				
				//try to create a new claim
				CreateClaimResult result = this.dataStore.createClaim(
						player.getWorld(), 
						lastShovelLocation.getBlockX(), clickedBlock.getX(), 
						lastShovelLocation.getBlockY() - GriefPrevention.instance.config_claims_claimsExtendIntoGroundDistance, clickedBlock.getY() - GriefPrevention.instance.config_claims_claimsExtendIntoGroundDistance, 
						lastShovelLocation.getBlockZ(), clickedBlock.getZ(), 
						playerID,
						null, null,
						player);
				
				//if it didn't succeed, tell the player why
				if(!result.succeeded)
				{
					if(result.claim != null)
					{
    				    GriefPrevention.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlapShort);
    					
    					Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.ErrorClaim, player.getLocation());
    					Visualization.Apply(player, visualization);
					}
					else
					{
					    GriefPrevention.sendMessage(player, TextMode.Err, Messages.CreateClaimFailOverlapRegion);
					}
    					
					return;
				}
				
				//otherwise, advise him on the /trust command and show him his new claim
				else
				{					
					GriefPrevention.sendMessage(player, TextMode.Success, Messages.CreateClaimSuccess);
					Visualization visualization = Visualization.FromClaim(result.claim, clickedBlock.getY(), VisualizationType.Claim, player.getLocation());
					Visualization.Apply(player, visualization);
					playerData.lastShovelLocation = null;
					
					//if it's a big claim, tell the player about subdivisions
					if(!player.hasPermission("griefprevention.adminclaims") && result.claim.getArea() >= 1000)
		            {
		                GriefPrevention.sendMessage(player, TextMode.Info, Messages.BecomeMayor, 200L);
		                GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SubdivisionVideo2, 201L, DataStore.SUBDIVISION_VIDEO_URL);
		            }
				}
			}
		}
	}
	
	//educates a player about /adminclaims and /acb, if he can use them 
	private void tryAdvertiseAdminAlternatives(Player player)
	{
        if(player.hasPermission("griefprevention.adminclaims") && player.hasPermission("griefprevention.adjustclaimblocks"))
        {
            GriefPrevention.sendMessage(player, TextMode.Info, Messages.AdvertiseACandACB);
        }
        else if(player.hasPermission("griefprevention.adminclaims"))
        {
            GriefPrevention.sendMessage(player, TextMode.Info, Messages.AdvertiseAdminClaims);
        }
        else if(player.hasPermission("griefprevention.adjustclaimblocks"))
        {
            GriefPrevention.sendMessage(player, TextMode.Info, Messages.AdvertiseACB);
        }
    }

    //determines whether a block type is an inventory holder.  uses a caching strategy to save cpu time
	private ConcurrentHashMap<Integer, Boolean> inventoryHolderCache = new ConcurrentHashMap<Integer, Boolean>();
	private boolean isInventoryHolder(Block clickedBlock)
	{
	    Integer cacheKey = clickedBlock.getTypeId();
	    Boolean cachedValue = this.inventoryHolderCache.get(cacheKey);
	    if(cachedValue != null)
	    {
	        return cachedValue.booleanValue();
	        
	    }
	    else
	    {
	        boolean isHolder = clickedBlock.getState() instanceof InventoryHolder;
	        this.inventoryHolderCache.put(cacheKey, isHolder);
	        return isHolder;
	    }
    }

    private boolean onLeftClickWatchList(Material material)
	{
	    switch(material)
        {
            case WOOD_BUTTON:
            case STONE_BUTTON:
            case LEVER:
            case DIODE_BLOCK_ON:  //redstone repeater
            case DIODE_BLOCK_OFF:
                return true;
            default:
                return false;
        }
    }

    static Block getTargetBlock(Player player, int maxDistance) throws IllegalStateException
	{
	    BlockIterator iterator = new BlockIterator(player.getLocation(), player.getEyeHeight(), maxDistance);
	    Block result = player.getLocation().getBlock().getRelative(BlockFace.UP);
	    while (iterator.hasNext())
	    {
	        result = iterator.next();
	        if(result.getType() != Material.AIR && result.getType() != Material.STATIONARY_WATER) return result;
	    }
	    
	    return result;
    }
}
