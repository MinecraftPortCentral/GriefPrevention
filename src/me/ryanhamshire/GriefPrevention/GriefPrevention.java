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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class GriefPrevention extends JavaPlugin
{
	//for convenience, a reference to the instance of this plugin
	public static GriefPrevention instance;
	
	//for logging to the console and log file
	private static Logger log = Logger.getLogger("Minecraft");
		
	//this handles data storage, like player and region data
	public DataStore dataStore;
	
	//configuration variables, loaded/saved from a config.yml
	public ArrayList<World> config_claims_enabledWorlds;			//list of worlds where players can create GriefPrevention claims
	
	public boolean config_claims_preventTheft;						//whether containers and crafting blocks are protectable
	public boolean config_claims_preventButtonsSwitches;			//whether buttons and switches are protectable
	
	public int config_claims_initialBlocks;							//the number of claim blocks a new player starts with
	public int config_claims_blocksAccruedPerHour;					//how many additional blocks players get each hour of play (can be zero)
	public int config_claims_maxAccruedBlocks;						//the limit on accrued blocks (over time).  doesn't limit purchased or admin-gifted blocks 
	public int config_claims_maxDepth;								//limit on how deep claims can go
	
	public int config_claims_automaticClaimsForNewPlayersRadius;	//how big automatic new player claims (when they place a chest) should be.  0 to disable
	public boolean config_claims_creationRequiresPermission;		//whether creating claims with the shovel requires a permission
	public int config_claims_claimsExtendIntoGroundDistance;		//how far below the shoveled block a new claim will reach
	public int config_claims_minSize;								//minimum width and height for non-admin claims
	
	public int config_claims_trappedCooldownHours;					//number of hours between uses of the /trapped command
	
	public ArrayList<World> config_siege_enabledWorlds;				//whether or not /siege is enabled on this server
	public ArrayList<Material> config_siege_blocks;					//which blocks will be breakable in siege mode
		
	public boolean config_spam_enabled;								//whether or not to monitor for spam
	public int config_spam_loginCooldownMinutes;					//how long players must wait between logins.  combats login spam.
	public ArrayList<String> config_spam_monitorSlashCommands;  	//the list of slash commands monitored for spam
	public boolean config_spam_banOffenders;						//whether or not to ban spammers automatically
	public String config_spam_banMessage;							//message to show an automatically banned player
	public String config_spam_warningMessage;						//message to show a player who is close to spam level
	public String config_spam_allowedIpAddresses;					//IP addresses which will not be censored
	
	public boolean config_pvp_protectFreshSpawns;					//whether to make newly spawned players immune until they pick up an item
	public boolean config_pvp_punishLogout;						    //whether to kill players who log out during PvP combat
	
	public boolean config_trees_removeFloatingTreetops;				//whether to automatically remove partially cut trees
	public boolean config_trees_regrowGriefedTrees;					//whether to automatically replant partially cut trees
	
	public double config_economy_claimBlocksPurchaseCost;			//cost to purchase a claim block.  set to zero to disable purchase.
	public double config_economy_claimBlocksSellValue;				//return on a sold claim block.  set to zero to disable sale.
	
	public boolean config_creepersDontDestroySurface;				//whether creeper explosions near or above the surface destroy blocks
	
	public boolean config_fireSpreads;								//whether fire spreads outside of claims
	public boolean config_fireDestroys;								//whether fire destroys blocks outside of claims
	
	//reference to the economy plugin, if economy integration is enabled
	public static Economy economy = null;					
	
	//how far away to search from a tree trunk for its branch blocks
	public static final int TREE_RADIUS = 5;
	
	//adds a server log entry
	public static void AddLogEntry(String entry)
	{
		log.info("GriefPrevention: " + entry);
	}
	
	//initializes well...   everything
	public void onEnable()
	{ 		
		AddLogEntry("Grief Prevention enabled.");
		
		instance = this;
		
		//load the config if it exists
		FileConfiguration config = YamlConfiguration.loadConfiguration(new File(DataStore.configFilePath));
		
		//read configuration settings (note defaults)
		
		//default for claims worlds list
		ArrayList<String> defaultClaimsWorldNames = new ArrayList<String>();
		List<World> worlds = this.getServer().getWorlds(); 
		for(int i = 0; i < worlds.size(); i++)
		{
			defaultClaimsWorldNames.add(worlds.get(i).getName());
		}
		
		//get claims world names from the config file
		List<String> claimsEnabledWorldNames = config.getStringList("GriefPrevention.Claims.Worlds");
		if(claimsEnabledWorldNames == null || claimsEnabledWorldNames.size() == 0)
		{			
			claimsEnabledWorldNames = defaultClaimsWorldNames;
		}
		
		//validate that list
		this.config_claims_enabledWorlds = new ArrayList<World>();
		for(int i = 0; i < claimsEnabledWorldNames.size(); i++)
		{
			String worldName = claimsEnabledWorldNames.get(i);
			World world = this.getServer().getWorld(worldName);
			if(world == null)
			{
				AddLogEntry("Error: Claims Configuration: There's no world named \"" + worldName + "\".  Please update your config.yml.");
			}
			else
			{
				this.config_claims_enabledWorlds.add(world);
			}
		}
		
		this.config_claims_preventTheft = config.getBoolean("GriefPrevention.Claims.PreventTheft", true);
		this.config_claims_preventButtonsSwitches = config.getBoolean("GriefPrevention.Claims.PreventButtonsSwitches", true);		
		this.config_claims_initialBlocks = config.getInt("GriefPrevention.Claims.InitialBlocks", 100);
		this.config_claims_blocksAccruedPerHour = config.getInt("GriefPrevention.Claims.BlocksAccruedPerHour", 100);
		this.config_claims_maxAccruedBlocks = config.getInt("GriefPrevention.Claims.MaxAccruedBlocks", 80000);
		this.config_claims_automaticClaimsForNewPlayersRadius = config.getInt("GriefPrevention.Claims.AutomaticNewPlayerClaimsRadius", 4);
		this.config_claims_claimsExtendIntoGroundDistance = config.getInt("GriefPrevention.Claims.ExtendIntoGroundDistance", 5);
		this.config_claims_creationRequiresPermission = config.getBoolean("GriefPrevention.Claims.CreationRequiresPermission", false);
		this.config_claims_minSize = config.getInt("GriefPrevention.Claims.MinimumSize", 10);
		this.config_claims_maxDepth = config.getInt("GriefPrevention.Claims.MaximumDepth", 0);
		this.config_claims_trappedCooldownHours = config.getInt("GriefPrevention.Claims.TrappedCommandCooldownHours", 8);
		
		this.config_spam_enabled = config.getBoolean("GriefPrevention.Spam.Enabled", true);
		this.config_spam_loginCooldownMinutes = config.getInt("GriefPrevention.Spam.LoginCooldownMinutes", 5);
		this.config_spam_warningMessage = config.getString("GriefPrevention.Spam.WarningMessage", "Please reduce your message speed.  Spammers will be banned.");
		this.config_spam_allowedIpAddresses = config.getString("GriefPrevention.Spam.AllowedIpAddresses", "1.2.3.4; 5.6.7.8");
		this.config_spam_banOffenders = config.getBoolean("GriefPrevention.Spam.BanOffenders", true);		
		this.config_spam_banMessage = config.getString("GriefPrevention.Spam.BanMessage", "Banned for spam.");
		String slashCommandsToMonitor = config.getString("GriefPrevention.Spam.MonitorSlashCommands", "/me;/tell;/global;/local");
		
		this.config_pvp_protectFreshSpawns = config.getBoolean("GriefPrevention.PvP.ProtectFreshSpawns", true);
		this.config_pvp_punishLogout = config.getBoolean("GriefPrevention.PvP.PunishLogout", true);
		
		this.config_trees_removeFloatingTreetops = config.getBoolean("GriefPrevention.Trees.RemoveFloatingTreetops", true);
		this.config_trees_regrowGriefedTrees = config.getBoolean("GriefPrevention.Trees.RegrowGriefedTrees", true);
		
		this.config_economy_claimBlocksPurchaseCost = config.getDouble("GriefPrevention.Economy.ClaimBlocksPurchaseCost", 0);
		this.config_economy_claimBlocksSellValue = config.getDouble("GriefPrevention.Economy.ClaimBlocksSellValue", 0);
		
		this.config_creepersDontDestroySurface = config.getBoolean("GriefPrevention.CreepersDontDestroySurface", true);
		
		this.config_fireSpreads = config.getBoolean("GriefPrevention.FireSpreads", false);
		this.config_fireDestroys = config.getBoolean("GriefPrevention.FireDestroys", false);
		
		//default for claims worlds list
		ArrayList<String> defaultSiegeWorldNames = new ArrayList<String>();
		
		//get siege world names from the config file
		List<String> siegeEnabledWorldNames = config.getStringList("GriefPrevention.Siege.Worlds");
		if(siegeEnabledWorldNames == null)
		{			
			siegeEnabledWorldNames = defaultSiegeWorldNames;
		}
		
		//validate that list
		this.config_siege_enabledWorlds = new ArrayList<World>();
		for(int i = 0; i < siegeEnabledWorldNames.size(); i++)
		{
			String worldName = siegeEnabledWorldNames.get(i);
			World world = this.getServer().getWorld(worldName);
			if(world == null)
			{
				AddLogEntry("Error: Siege Configuration: There's no world named \"" + worldName + "\".  Please update your config.yml.");
			}
			else
			{
				this.config_siege_enabledWorlds.add(world);
			}
		}
		
		//default siege blocks
		this.config_siege_blocks = new ArrayList<Material>();
		this.config_siege_blocks.add(Material.DIRT);
		this.config_siege_blocks.add(Material.GRASS);
		this.config_siege_blocks.add(Material.LONG_GRASS);
		this.config_siege_blocks.add(Material.COBBLESTONE);
		this.config_siege_blocks.add(Material.GRAVEL);
		this.config_siege_blocks.add(Material.SAND);
		this.config_siege_blocks.add(Material.GLASS);
		this.config_siege_blocks.add(Material.THIN_GLASS);
		this.config_siege_blocks.add(Material.WOOD);
		this.config_siege_blocks.add(Material.WOOL);
		this.config_siege_blocks.add(Material.SNOW);
		
		//build a default config entry
		ArrayList<String> defaultBreakableBlocksList = new ArrayList<String>();
		for(int i = 0; i < this.config_siege_blocks.size(); i++)
		{
			defaultBreakableBlocksList.add(this.config_siege_blocks.get(i).name());
		}
		
		//try to load the list from the config file
		List<String> breakableBlocksList = config.getStringList("GriefPrevention.Siege.BreakableBlocks");
		
		//if it fails, use default list instead
		if(breakableBlocksList == null || breakableBlocksList.size() == 0)
		{
			breakableBlocksList = defaultBreakableBlocksList;
		}
		
		//parse the list of siege-breakable blocks
		this.config_siege_blocks = new ArrayList<Material>();
		for(int i = 0; i < breakableBlocksList.size(); i++)
		{
			String blockName = breakableBlocksList.get(i);
			Material material = Material.getMaterial(blockName);
			if(material == null)
			{
				GriefPrevention.AddLogEntry("Siege Configuration: Material not found: " + blockName + ".");
			}
			else
			{
				this.config_siege_blocks.add(material);
			}
		}
		
		config.set("GriefPrevention.Claims.Worlds", claimsEnabledWorldNames);
		config.set("GriefPrevention.Claims.PreventTheft", this.config_claims_preventTheft);
		config.set("GriefPrevention.Claims.PreventButtonsSwitches", this.config_claims_preventButtonsSwitches);
		config.set("GriefPrevention.Claims.InitialBlocks", this.config_claims_initialBlocks);
		config.set("GriefPrevention.Claims.BlocksAccruedPerHour", this.config_claims_blocksAccruedPerHour);
		config.set("GriefPrevention.Claims.MaxAccruedBlocks", this.config_claims_maxAccruedBlocks);
		config.set("GriefPrevention.Claims.AutomaticNewPlayerClaimsRadius", this.config_claims_automaticClaimsForNewPlayersRadius);
		config.set("GriefPrevention.Claims.ExtendIntoGroundDistance", this.config_claims_claimsExtendIntoGroundDistance);
		config.set("GriefPrevention.Claims.CreationRequiresPermission", this.config_claims_creationRequiresPermission);
		config.set("GriefPrevention.Claims.MinimumSize", this.config_claims_minSize);
		config.set("GriefPrevention.Claims.MaximumDepth", this.config_claims_maxDepth);
		config.set("GriefPrevention.Claims.TrappedCommandCooldownHours", this.config_claims_trappedCooldownHours);
		
		config.set("GriefPrevention.Spam.Enabled", this.config_spam_enabled);
		config.set("GriefPrevention.Spam.LoginCooldownMinutes", this.config_spam_loginCooldownMinutes);
		config.set("GriefPrevention.Spam.MonitorSlashCommands", slashCommandsToMonitor);
		config.set("GriefPrevention.Spam.WarningMessage", this.config_spam_warningMessage);
		config.set("GriefPrevention.Spam.BanOffenders", this.config_spam_banOffenders);		
		config.set("GriefPrevention.Spam.BanMessage", this.config_spam_banMessage);
		config.set("GriefPrevention.Spam.AllowedIpAddresses", this.config_spam_allowedIpAddresses);
		
		config.set("GriefPrevention.PvP.ProtectFreshSpawns", this.config_pvp_protectFreshSpawns);
		config.set("GriefPrevention.PvP.PunishLogout", this.config_pvp_punishLogout);
		
		config.set("GriefPrevention.Trees.RemoveFloatingTreetops", this.config_trees_removeFloatingTreetops);
		config.set("GriefPrevention.Trees.RegrowGriefedTrees", this.config_trees_regrowGriefedTrees);
		
		config.set("GriefPrevention.Economy.ClaimBlocksPurchaseCost", this.config_economy_claimBlocksPurchaseCost);
		config.set("GriefPrevention.Economy.ClaimBlocksSellValue", this.config_economy_claimBlocksSellValue);
		
		config.set("GriefPrevention.CreepersDontDestroySurface", this.config_creepersDontDestroySurface);
		
		config.set("GriefPrevention.FireSpreads", this.config_fireSpreads);
		config.set("GriefPrevention.FireDestroys", this.config_fireDestroys);
		
		config.set("GriefPrevention.Siege.Worlds", siegeEnabledWorldNames);
		config.set("GriefPrevention.Siege.BreakableBlocks", breakableBlocksList);

		try
		{
			config.save(DataStore.configFilePath);
		}
		catch(IOException exception)
		{
			AddLogEntry("Unable to write to the configuration file at \"" + DataStore.configFilePath + "\"");
		}
		
		//try to parse the list of commands which should be monitored for spam
		this.config_spam_monitorSlashCommands = new ArrayList<String>();
		String [] commands = slashCommandsToMonitor.split(";");
		for(int i = 0; i < commands.length; i++)
		{
			this.config_spam_monitorSlashCommands.add(commands[i].trim());
		}
		
		//when datastore initializes, it loads player and claim data, and posts some stats to the log
		this.dataStore = new DataStore();
		
		//unless claim block accrual is disabled, start the recurring per 5 minute event to give claim blocks to online players
		//20L ~ 1 second
		if(this.config_claims_blocksAccruedPerHour > 0)
		{
			DeliverClaimBlocksTask task = new DeliverClaimBlocksTask();
			this.getServer().getScheduler().scheduleSyncRepeatingTask(this, task, 20L * 60 * 5, 20L * 60 * 5);
		}
		
		//register for events
		PluginManager pluginManager = this.getServer().getPluginManager();
		
		//player events
		PlayerEventHandler playerEventHandler = new PlayerEventHandler(this.dataStore, this);
		pluginManager.registerEvents(playerEventHandler, this);
		
		//block events
		BlockEventHandler blockEventHandler = new BlockEventHandler(this.dataStore);
		pluginManager.registerEvents(blockEventHandler, this);
				
		//entity events
		EntityEventHandler entityEventHandler = new EntityEventHandler(this.dataStore);
		pluginManager.registerEvents(entityEventHandler, this);
		
		//if economy is enabled
		if(this.config_economy_claimBlocksPurchaseCost > 0 || this.config_economy_claimBlocksSellValue > 0)
		{
			//try to load Vault
			GriefPrevention.AddLogEntry("GriefPrevention requires Vault for economy integration.");
			GriefPrevention.AddLogEntry("Attempting to load Vault...");
			RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
			GriefPrevention.AddLogEntry("Vault loaded successfully!");
			
			//ask Vault to hook into an economy plugin
			GriefPrevention.AddLogEntry("Looking for a Vault-compatible economy plugin...");
			if (economyProvider != null) 
	        {
	        	GriefPrevention.economy = economyProvider.getProvider();
	            
	            //on success, display success message
				if(GriefPrevention.economy != null)
		        {
	            	GriefPrevention.AddLogEntry("Hooked into economy: " + GriefPrevention.economy.getName() + ".");  
	            	GriefPrevention.AddLogEntry("Ready to buy/sell claim blocks!");
		        }
		        
				//otherwise error message
				else
		        {
		        	GriefPrevention.AddLogEntry("ERROR: Vault was unable to find a supported economy plugin.  Either install a Vault-compatible economy plugin, or set both of the economy config variables to zero.");
		        }	            
	        }
			
			//another error case
			else
			{
				GriefPrevention.AddLogEntry("ERROR: Vault was unable to find a supported economy plugin.  Either install a Vault-compatible economy plugin, or set both of the economy config variables to zero.");
			}
		}		
	}
	
	//handles slash commands
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args){
		
		Player player = null;
		if (sender instanceof Player) 
		{
			player = (Player) sender;
		}
		
		//abandonclaim
		if(cmd.getName().equalsIgnoreCase("abandonclaim") && player != null)
		{
			this.abandonClaimHandler(player, false);
		}		
		
		//abandontoplevelclaim
		if(cmd.getName().equalsIgnoreCase("abandontoplevelclaim") && player != null)
		{
			return this.abandonClaimHandler(player, true);
		}
		
		//ignoreclaims
		if(cmd.getName().equalsIgnoreCase("ignoreclaims") && player != null)
		{
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			
			playerData.ignoreClaims = !playerData.ignoreClaims;
			
			//toggle ignore claims mode on or off
			if(!playerData.ignoreClaims)
			{
				GriefPrevention.sendMessage(player, TextMode.Success, "Now respecting claims.");
			}
			else
			{
				GriefPrevention.sendMessage(player, TextMode.Success, "Now ignoring claims.");
			}
			
			return true;
		}
		
		//abandonallclaims
		else if(cmd.getName().equalsIgnoreCase("abandonallclaims") && player != null)
		{
			//count claims
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			int originalClaimCount = playerData.claims.size();
			
			//check count
			if(originalClaimCount == 0)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You haven't claimed any land.");
				return true;
			}
			
			//delete them
			this.dataStore.deleteClaimsForPlayer(player.getName());
			
			//inform the player
			int remainingBlocks = playerData.getRemainingClaimBlocks();
			GriefPrevention.sendMessage(player, TextMode.Success, originalClaimCount + " claims abandoned.  You now have " + String.valueOf(remainingBlocks) + " available claim blocks.");
			
			//revert any current visualization
			Visualization.Revert(player);
			
			return true;
		}
		
		//restore nature
		else if(cmd.getName().equalsIgnoreCase("restorenature") && player != null)
		{
			//change shovel mode
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			playerData.shovelMode = ShovelMode.RestoreNature;
			GriefPrevention.sendMessage(player, TextMode.Instr, "Ready to restore some nature!  Right click to restore nature, and use /BasicClaims to stop.");
			return true;
		}
		
		//trust <player>
		else if(cmd.getName().equalsIgnoreCase("trust") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;
			
			//most trust commands use this helper method, it keeps them consistent
			this.handleTrustCommand(player, ClaimPermission.Build, args[0]);
			
			return true;
		}
		
		//transferclaim <player>
		else if(cmd.getName().equalsIgnoreCase("transferclaim") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;
			
			//check additional permission
			if(!player.hasPermission("griefprevention.adminclaims"))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "That command requires the administrative claims permission.");
				return true;
			}
			
			//which claim is the user in?
			Claim claim = this.dataStore.getClaimAt(player.getLocation(), true, null);
			if(claim == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Instr, "There's no claim here.  Stand in the administrative claim you want to transfer.");
				return true;
			}
			else if(!claim.isAdminClaim())
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "Only administrative claims may be transferred to a player.");
				return true;
			}
			
			OfflinePlayer targetPlayer = this.resolvePlayer(args[0]);
			if(targetPlayer == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "Player not found.");
				return true;
			}
			
			//change ownerhsip
			try
			{
				this.dataStore.changeClaimOwner(claim, targetPlayer.getName());
			}
			catch(Exception e)
			{
				GriefPrevention.sendMessage(player, TextMode.Instr, "Only top level claims (not subdivisions) may be transferred.  Stand outside of the subdivision and try again.");
				return true;
			}
			
			//confirm
			GriefPrevention.sendMessage(player, TextMode.Success, "Claim transferred.");
			
			return true;
		}
		
		//trustlist
		else if(cmd.getName().equalsIgnoreCase("trustlist") && player != null)
		{
			Claim claim = this.dataStore.getClaimAt(player.getLocation(), true, null);
			
			//if no claim here, error message
			if(claim == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "Stand inside the claim you're curious about.");
				return true;
			}
			
			//if no permission to manage permissions, error message
			String errorMessage = claim.allowGrantPermission(player);
			if(errorMessage != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, errorMessage);
				return true;
			}
			
			//otherwise build a list of explicit permissions by permission level
			//and send that to the player
			ArrayList<String> builders = new ArrayList<String>();
			ArrayList<String> containers = new ArrayList<String>();
			ArrayList<String> accessors = new ArrayList<String>();
			ArrayList<String> managers = new ArrayList<String>();
			claim.getPermissions(builders, containers, accessors, managers);
			
			player.sendMessage("Explicit permissions here:");
			
			StringBuilder permissions = new StringBuilder();
			permissions.append(ChatColor.GOLD + "M: ");
			
			if(managers.size() > 0)
			{
				for(int i = 0; i < managers.size(); i++)
					permissions.append(managers.get(i) + " ");
			}
			
			player.sendMessage(permissions.toString());
			permissions = new StringBuilder();
			permissions.append(ChatColor.YELLOW + "B: ");
			
			if(builders.size() > 0)
			{				
				for(int i = 0; i < builders.size(); i++)
					permissions.append(builders.get(i) + " ");		
			}
			
			player.sendMessage(permissions.toString());
			permissions = new StringBuilder();
			permissions.append(ChatColor.GREEN + "C: ");				
			
			if(containers.size() > 0)
			{
				for(int i = 0; i < containers.size(); i++)
					permissions.append(containers.get(i) + " ");		
			}
			
			player.sendMessage(permissions.toString());
			permissions = new StringBuilder();
			permissions.append(ChatColor.BLUE + "A :");
				
			if(accessors.size() > 0)
			{
				for(int i = 0; i < accessors.size(); i++)
					permissions.append(accessors.get(i) + " ");			
			}
			
			player.sendMessage(permissions.toString());
			
			player.sendMessage("(M-anager, B-builder, C-ontainers, A-ccess)");
			
			return true;
		}
		
		//untrust <player>
		else if(cmd.getName().equalsIgnoreCase("untrust") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;
			
			//determine which claim the player is standing in
			Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
			
			//determine whether a single player or clearing permissions entirely
			boolean clearPermissions = false;
			OfflinePlayer otherPlayer = null;
			if(args[0].equals("all"))				
			{
				if(claim == null || claim.allowEdit(player) == null)
				{
					clearPermissions = true;
				}
				else
				{
					GriefPrevention.sendMessage(player, TextMode.Err, "Only the claim owner can clear all permissions.");
					return true;
				}
			}
			
			else
			{
				//validate player argument
				otherPlayer = this.resolvePlayer(args[0]);
				if(!clearPermissions && otherPlayer == null && !args[0].equals("public"))
				{
					GriefPrevention.sendMessage(player, TextMode.Err, "Player not found.");
					return true;
				}
				
				//correct to proper casing
				if(otherPlayer != null)
					args[0] = otherPlayer.getName();
			}
			
			//if no claim here, apply changes to all his claims
			if(claim == null)
			{
				PlayerData playerData = this.dataStore.getPlayerData(player.getName());
				for(int i = 0; i < playerData.claims.size(); i++)
				{
					claim = playerData.claims.get(i);
					
					//if untrusting "all" drop all permissions
					if(clearPermissions)
					{	
						claim.clearPermissions();
					}
					
					//otherwise drop individual permissions
					else
					{
						claim.dropPermission(args[0]);
						claim.managers.remove(args[0]);
					}
					
					//save changes
					this.dataStore.saveClaim(claim);
				}
				
				//beautify for output
				if(args[0].equals("public"))
				{
					args[0] = "the public";
				}
				
				//confirmation message
				if(!clearPermissions)
				{
					GriefPrevention.sendMessage(player, TextMode.Success, "Revoked " + args[0] + "'s access to ALL your claims.  To set permissions for a single claim, stand inside it.");
				}
				else
				{
					GriefPrevention.sendMessage(player, TextMode.Success, "Cleared permissions in ALL your claims.  To set permissions for a single claim, stand inside it.");
				}
			}			
			
			//otherwise, apply changes to only this claim
			else if(claim.allowGrantPermission(player) != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You don't have " + claim.getOwnerName() + "'s permission to manage permissions here.");
			}
			else
			{
				//if clearing all
				if(clearPermissions)
				{
					claim.clearPermissions();
					GriefPrevention.sendMessage(player, TextMode.Success, "Cleared permissions in this claim.  To set permission for ALL your claims, stand outside them.");
				}
				
				//otherwise individual permission drop
				else
				{
					claim.dropPermission(args[0]);
					if(claim.allowEdit(player) == null)
					{
						claim.managers.remove(args[0]);
						
						//beautify for output
						if(args[0].equals("public"))
						{
							args[0] = "the public";
						}
						
						GriefPrevention.sendMessage(player, TextMode.Success, "Revoked " + args[0] + "'s access to this claim.  To set permissions for a ALL your claims, stand outside them.");
					}
				}
				
				//save changes
				this.dataStore.saveClaim(claim);										
			}
			
			return true;
		}
		
		//accesstrust <player>
		else if(cmd.getName().equalsIgnoreCase("accesstrust") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;
			
			this.handleTrustCommand(player, ClaimPermission.Access, args[0]);
			
			return true;
		}
		
		//containertrust <player>
		else if(cmd.getName().equalsIgnoreCase("containertrust") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;
			
			this.handleTrustCommand(player, ClaimPermission.Inventory, args[0]);
			
			return true;
		}
		
		//permissiontrust <player>
		else if(cmd.getName().equalsIgnoreCase("permissiontrust") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;
			
			this.handleTrustCommand(player, null, args[0]);  //null indicates permissiontrust to the helper method
			
			return true;
		}
		
		//buyclaimblocks
		else if(cmd.getName().equalsIgnoreCase("buyclaimblocks") && player != null)
		{
			//if economy is disabled, don't do anything
			if(GriefPrevention.economy == null) return true;
			
			//if purchase disabled, send error message
			if(GriefPrevention.instance.config_economy_claimBlocksPurchaseCost == 0)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "Claim blocks may only be sold, not purchased.");
				return true;
			}
			
			//if no parameter, just tell player cost per block and balance
			if(args.length != 1)
			{
				GriefPrevention.sendMessage(player, TextMode.Info, "Each claim block costs " + GriefPrevention.instance.config_economy_claimBlocksPurchaseCost + ".  Your balance is " + GriefPrevention.economy.getBalance(player.getName()) + ".");
				return false;
			}
			
			else
			{
				//try to parse number of blocks
				int blockCount;
				try
				{
					blockCount = Integer.parseInt(args[0]);
				}
				catch(NumberFormatException numberFormatException)
				{
					return false;  //causes usage to be displayed
				}
				
				//if the player can't afford his purchase, send error message
				double balance = economy.getBalance(player.getName());				
				double totalCost = blockCount * GriefPrevention.instance.config_economy_claimBlocksPurchaseCost;				
				if(totalCost > balance)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, "You don't have enough money.  You need " + totalCost + ", but you only have " + balance + ".");
				}
				
				//otherwise carry out transaction
				else
				{
					//withdraw cost
					economy.withdrawPlayer(player.getName(), totalCost);
					
					//add blocks
					PlayerData playerData = this.dataStore.getPlayerData(player.getName());
					playerData.bonusClaimBlocks += blockCount;
					this.dataStore.savePlayerData(player.getName(), playerData);
					
					//inform player
					GriefPrevention.sendMessage(player, TextMode.Success, "Withdrew " + totalCost + " from your account.  You now have " + playerData.getRemainingClaimBlocks() + " available claim blocks.");
				}
				
				return true;
			}
		}
		
		//sellclaimblocks <amount> 
		else if(cmd.getName().equalsIgnoreCase("sellclaimblocks") && player != null)
		{
			//if economy is disabled, don't do anything
			if(GriefPrevention.economy == null) return true;
			
			//if disabled, error message
			if(GriefPrevention.instance.config_economy_claimBlocksSellValue == 0)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "Claim blocks may only be purchased, not sold.");
				return true;
			}
			
			//load player data
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			int availableBlocks = playerData.getRemainingClaimBlocks();
			
			//if no amount provided, just tell player value per block sold, and how many he can sell
			if(args.length != 1)
			{
				GriefPrevention.sendMessage(player, TextMode.Info, "Each claim block is worth " + GriefPrevention.instance.config_economy_claimBlocksSellValue + ".  You have " + availableBlocks + " available for sale.");
				return false;
			}
						
			//parse number of blocks
			int blockCount;
			try
			{
				blockCount = Integer.parseInt(args[0]);
			}
			catch(NumberFormatException numberFormatException)
			{
				return false;  //causes usage to be displayed
			}
			
			//if he doesn't have enough blocks, tell him so
			if(blockCount > availableBlocks)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You don't have that many claim blocks available for sale.");
			}
			
			//otherwise carry out the transaction
			else
			{					
				//compute value and deposit it
				double totalValue = blockCount * GriefPrevention.instance.config_economy_claimBlocksSellValue;					
				economy.depositPlayer(player.getName(), totalValue);
				
				//subtract blocks
				playerData.bonusClaimBlocks -= blockCount;
				this.dataStore.savePlayerData(player.getName(), playerData);
				
				//inform player
				GriefPrevention.sendMessage(player, TextMode.Success, "Deposited " + totalValue + " in your account.  You now have " + playerData.getRemainingClaimBlocks() + " available claim blocks.");
			}
			
			return true;
		}		
		
		//adminclaims
		else if(cmd.getName().equalsIgnoreCase("adminclaims") && player != null)
		{
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			playerData.shovelMode = ShovelMode.Admin;
			GriefPrevention.sendMessage(player, TextMode.Success, "Administrative claims mode active.  Any claims created will be free and editable by other administrators.");
			
			return true;
		}
		
		//basicclaims
		else if(cmd.getName().equalsIgnoreCase("basicclaims") && player != null)
		{
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			playerData.shovelMode = ShovelMode.Basic;
			playerData.claimSubdividing = null;
			GriefPrevention.sendMessage(player, TextMode.Success, "Returned to basic claim creation mode.");
			
			return true;
		}
		
		//subdivideclaims
		else if(cmd.getName().equalsIgnoreCase("subdivideclaims") && player != null)
		{
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			playerData.shovelMode = ShovelMode.Subdivide;
			playerData.claimSubdividing = null;
			GriefPrevention.sendMessage(player, TextMode.Instr, "Subdivision mode.  Use your shovel to create subdivisions in your existing claims.  Use /basicclaims to exit.");
			
			return true;
		}
		
		//deleteclaim
		else if(cmd.getName().equalsIgnoreCase("deleteclaim") && player != null)
		{
			//determine which claim the player is standing in
			Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
			
			if(claim == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "There's no claim here.");
			}
			
			else 
			{
				//deleting an admin claim additionally requires the adminclaims permission
				if(!claim.isAdminClaim() || player.hasPermission("griefprevention.adminclaims"))
				{
					this.dataStore.deleteClaim(claim);
					GriefPrevention.sendMessage(player, TextMode.Success, "Claim deleted.");
					
					//revert any current visualization
					Visualization.Revert(player);
				}
				else
				{
					GriefPrevention.sendMessage(player, TextMode.Err, "You don't have permission to delete administrative claims.");
				}
			}

			return true;
		}
		
		//deleteallclaims <player>
		else if(cmd.getName().equalsIgnoreCase("deleteallclaims") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;
			
			//try to find that player
			OfflinePlayer otherPlayer = this.resolvePlayer(args[0]);
			if(otherPlayer == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "Player not found.");
				return true;
			}
			
			//delete all that player's claims
			this.dataStore.deleteClaimsForPlayer(otherPlayer.getName());
			
			GriefPrevention.sendMessage(player, TextMode.Success, "Deleted all of " + otherPlayer.getName() + "'s claims.");
			
			//revert any current visualization
			Visualization.Revert(player);
			
			return true;
		}
		
		//deletealladminclaims
		else if(cmd.getName().equalsIgnoreCase("deletealladminclaims") && player != null)
		{
			if(!player.hasPermission("griefprevention.deleteclaims"))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You don't have permission to delete claims.");
				return true;
			}
			
			//delete all admin claims
			this.dataStore.deleteClaimsForPlayer("");  //empty string for owner name indicates an administrative claim
			
			GriefPrevention.sendMessage(player, TextMode.Success, "Deleted all administrative claims.");
			
			//revert any current visualization
			Visualization.Revert(player);
			
			return true;
		}
		
		//adjustbonusclaimblocks <player> <amount>
		else if(cmd.getName().equalsIgnoreCase("adjustbonusclaimblocks") && player != null)
		{
			//requires exactly two parameters, the other player's name and the adjustment
			if(args.length != 2) return false;
			
			//find the specified player
			OfflinePlayer targetPlayer = this.resolvePlayer(args[0]);
			if(targetPlayer == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "Player \"" + args[0] + "\" not found.");
				return true;
			}
			
			//parse the adjustment amount
			int adjustment;			
			try
			{
				adjustment = Integer.parseInt(args[1]);
			}
			catch(NumberFormatException numberFormatException)
			{
				return false;  //causes usage to be displayed
			}
			
			//give blocks to player
			PlayerData playerData = this.dataStore.getPlayerData(targetPlayer.getName());
			playerData.bonusClaimBlocks += adjustment;
			this.dataStore.savePlayerData(targetPlayer.getName(), playerData);
			
			GriefPrevention.sendMessage(player, TextMode.Success, "Adjusted " + targetPlayer.getName() + "'s bonus claim blocks by " + adjustment + ".  New total bonus blocks: " + playerData.bonusClaimBlocks + ".");
			
			return true;			
		}
		
		//trapped
		else if(cmd.getName().equalsIgnoreCase("trapped") && player != null)
		{
			//FEATURE: empower players who get "stuck" in an area where they don't have permission to build to save themselves
			
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			Claim claim = this.dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);
			
			//if another /trapped is pending, ignore this slash command
			if(playerData.pendingTrapped)
			{
				return true;
			}
			
			//if the player isn't in a claim or has permission to build, tell him to man up
			if(claim == null || claim.allowBuild(player) == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You can build here.  Save yourself.");				
				return true;
			}
			
			//check cooldown
			long lastTrappedUsage = playerData.lastTrappedUsage.getTime();
			long nextTrappedUsage = lastTrappedUsage + 1000 * 60 * 60 * this.config_claims_trappedCooldownHours; 
			long now = Calendar.getInstance().getTimeInMillis();
			if(now < nextTrappedUsage)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You used /trapped within the last " + this.config_claims_trappedCooldownHours + " hours.  You have to wait about " + ((nextTrappedUsage - now) / (1000 * 60) + 1) + " more minutes before using it again.");
				return true;
			}
			
			//send instructions
			GriefPrevention.sendMessage(player, TextMode.Instr, "If you stay put for 10 seconds, you'll be teleported out.  Please wait.");
			
			//create a task to rescue this player in a little while
			PlayerRescueTask task = new PlayerRescueTask(player, player.getLocation());
			this.getServer().getScheduler().scheduleSyncDelayedTask(this, task, 200L);  //20L ~ 1 second
			
			return true;
		}
		
		//siege
		else if(cmd.getName().equalsIgnoreCase("siege") && player != null)
		{
			//error message for when siege mode is disabled
			if(!this.siegeEnabledForWorld(player.getWorld()))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "Siege is disabled here.");
				return true;
			}
			
			//requires one argument
			if(args.length > 1)
			{
				return false;
			}
			
			//can't start a siege when you're already involved in one
			Player attacker = player;
			PlayerData attackerData = this.dataStore.getPlayerData(attacker.getName());
			if(attackerData.siegeData != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You're already involved in a siege.");
				return true;
			}
			
			//if a player name was specified, use that
			Player defender = null;
			if(args.length >= 1)
			{
				defender = this.getServer().getPlayer(args[0]);
				if(defender == null)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, "Player not found.");
					return true;
				}
			}
			
			//otherwise use the last player this player was in pvp combat with 
			else if(attackerData.lastPvpPlayer.length() > 0)
			{
				defender = this.getServer().getPlayer(attackerData.lastPvpPlayer);
				if(defender == null)
				{
					return false;
				}
			}
			
			else
			{
				return false;
			}
			
			//victim must not be under siege already
			PlayerData defenderData = this.dataStore.getPlayerData(defender.getName());
			if(defenderData.siegeData != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, defender.getName() + " is already under siege.  Join the party!");
				return true;
			}
			
			//victim must not be pvp immune
			if(defenderData.pvpImmune)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, defender.getName() + " is defenseless.  Go pick on somebody else.");
				return true;
			}
			
			Claim defenderClaim = this.dataStore.getClaimAt(defender.getLocation(), false, null);
			
			//defender must have some level of permission there to be protected
			if(defenderClaim == null || defenderClaim.allowAccess(defender) != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, defender.getName() + " isn't protected there.");
				return true;
			}									
			
			//attacker must be close to the claim he wants to siege
			if(!defenderClaim.isNear(attacker.getLocation(), 25))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You're too far away from " + defender.getName() + " to siege.");
				return true;
			}
			
			//claim can't be under siege already
			if(defenderClaim.siegeData != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "That area is already under siege.  Join the party!");
				return true;
			}
			
			//can't siege admin claims
			if(defenderClaim.isAdminClaim())
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "Siege is disabled in this area.");
				return true;
			}
			
			//can't be on cooldown
			if(dataStore.onCooldown(attacker, defender, defenderClaim))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You're still on siege cooldown for this defender or claim.  Find another victim.");
				return true;
			}
			
			//start the siege
			dataStore.startSiege(attacker, defender, defenderClaim);			

			//confirmation message for attacker, warning message for defender
			GriefPrevention.sendMessage(defender, TextMode.Warn, "You're under siege!  If you log out now, you will die.  You must defeat " + attacker.getName() + ", wait for him to give up, or escape.");
			GriefPrevention.sendMessage(player, TextMode.Success, "The siege has begun!  If you log out now, you will die.  You must defeat " + defender.getName() + ", chase him away, or admit defeat and walk away.");			
		}
		
		return false; 
	}
	
	private boolean abandonClaimHandler(Player player, boolean deleteTopLevelClaim) 
	{
		//which claim is being abandoned?
		Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
		if(claim == null)
		{
			GriefPrevention.sendMessage(player, TextMode.Instr, "Stand in the claim you want to delete, or consider /AbandonAllClaims.");
		}			
		
		//verify ownership
		else if(claim.allowEdit(player) != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, "This isn't your claim.");
		}
		
		//warn if has children and we're not explicitly deleting a top level claim
		else if(claim.children.size() > 0 && !deleteTopLevelClaim)
		{
			GriefPrevention.sendMessage(player, TextMode.Instr, "To delete a subdivision, stand inside it.  Otherwise, use /AbandonTopLevelClaim to delete this claim and all subdivisions.");
			return true;
		}
		
		else
		{
			//delete it
			this.dataStore.deleteClaim(claim);
			
			//tell the player how many claim blocks he has left
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			int remainingBlocks = playerData.getRemainingClaimBlocks();
			GriefPrevention.sendMessage(player, TextMode.Success, "Claim abandoned.  You now have " + String.valueOf(remainingBlocks) + " available claim blocks.");
			
			//revert any current visualization
			Visualization.Revert(player);
		}
		
		return true;
		
	}

	//helper method keeps the trust commands consistent and eliminates duplicate code
	private void handleTrustCommand(Player player, ClaimPermission permissionLevel, String recipientName) 
	{
		//determine which claim the player is standing in
		Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
		
		//validate player argument
		OfflinePlayer otherPlayer = this.resolvePlayer(recipientName);
		if(otherPlayer == null && !recipientName.equals("public"))
		{
			GriefPrevention.sendMessage(player, TextMode.Err, "Player not found.");
			return;
		}
		
		if(otherPlayer != null)
		{
			recipientName = otherPlayer.getName();
		}
		else
		{
			recipientName = "public";
		}
		
		//determine which claims should be modified
		ArrayList<Claim> targetClaims = new ArrayList<Claim>();
		if(claim == null)
		{
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			for(int i = 0; i < playerData.claims.size(); i++)
			{
				targetClaims.add(playerData.claims.get(i));
			}
		}
		else
		{
			//check permission here
			if(claim.allowGrantPermission(player) != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, "You don't have " + claim.getOwnerName() + "'s permission to grant permissions here.");
				return;
			}
			
			//see if the player has the level of permission he's trying to grant
			String errorMessage = null;
			
			//permission level null indicates granting permission trust
			if(permissionLevel == null)
			{
				errorMessage = claim.allowEdit(player);
				if(errorMessage != null)
				{
					errorMessage = "Only " + claim.getOwnerName() + " can grant /PermissionTrust here."; 
				}
			}
			
			//otherwise just use the ClaimPermission enum values
			else
			{
				switch(permissionLevel)
				{
					case Access:
						errorMessage = claim.allowAccess(player);
						break;
					case Inventory:
						errorMessage = claim.allowContainers(player);
						break;
					default:
						errorMessage = claim.allowBuild(player);					
				}
			}
			
			//error message for trying to grant a permission the player doesn't have
			if(errorMessage != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, errorMessage + "  You can't grant a permission you don't have yourself.");
				return;
			}
			
			targetClaims.add(claim);
		}
		
		//if we didn't determine which claims to modify, tell the player to be specific
		if(targetClaims.size() == 0)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, "Stand inside the claim where you want to grant permission.");
			return;
		}
		
		//apply changes
		for(int i = 0; i < targetClaims.size(); i++)
		{
			Claim currentClaim = targetClaims.get(i);
			if(permissionLevel == null)
			{
				if(!currentClaim.managers.contains(recipientName))
				{
					currentClaim.managers.add(recipientName);
				}
			}
			else
			{				
				currentClaim.setPermission(recipientName, permissionLevel);
			}
			this.dataStore.saveClaim(currentClaim);
		}
		
		//notify player
		if(recipientName.equals("public")) recipientName = "the public";
		StringBuilder resultString = new StringBuilder();
		resultString.append("Granted " + recipientName + " ");
		if(permissionLevel == null)
		{
			resultString.append("manager status");
		}
		else if(permissionLevel == ClaimPermission.Build)
		{
			resultString.append("permission to build in");
		}		
		else if(permissionLevel == ClaimPermission.Access)
		{
			resultString.append("permission to use buttons and levers in");
		}
		else if(permissionLevel == ClaimPermission.Inventory)
		{
			resultString.append("permission to access containers in");
		}
		
		if(claim == null)
		{
			resultString.append(" ALL your claims.  To modify only one claim, stand inside it.");
		}
		else
		{
			resultString.append(" this claim.  To modify ALL your claims, stand outside them.");
		}
		
		GriefPrevention.sendMessage(player, TextMode.Success, resultString.toString());
	}

	//helper method to resolve a player by name
	private OfflinePlayer resolvePlayer(String name) 
	{
		//try online players first
		Player player = this.getServer().getPlayer(name);
		if(player != null) return player;
		
		//then search offline players
		OfflinePlayer [] offlinePlayers = this.getServer().getOfflinePlayers();
		for(int i = 0; i < offlinePlayers.length; i++)
		{
			if(offlinePlayers[i].getName().equalsIgnoreCase(name))
			{
				return offlinePlayers[i];
			}
		}
		
		//if none found, return null
		return null;
	}

	public void onDisable()
	{ 
		AddLogEntry("GriefPrevention disabled.");
	}
	
	//called when a player spawns, applies protection for that player if necessary
	public void checkPvpProtectionNeeded(Player player)
	{
		//if pvp is disabled, do nothing
		if(!player.getWorld().getPVP()) return;
		
		//if anti spawn camping feature is not enabled, do nothing
		if(!this.config_pvp_protectFreshSpawns) return;
		
		//check inventory for well, anything
		PlayerInventory inventory = player.getInventory();
		ItemStack [] armorStacks = inventory.getArmorContents();
		
		//check armor slots, stop if any items are found
		for(int i = 0; i < armorStacks.length; i++)
		{
			if(!(armorStacks[i] == null || armorStacks[i].getType() == Material.AIR)) return;
		}
		
		//check other slots, stop if any items are found
		ItemStack [] generalStacks = inventory.getContents();
		for(int i = 0; i < generalStacks.length; i++)
		{
			if(!(generalStacks[i] == null || generalStacks[i].getType() == Material.AIR)) return;
		}
			
		//otherwise, apply immunity
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		playerData.pvpImmune = true;
		
		//inform the player
		GriefPrevention.sendMessage(player, TextMode.Success, "You're protected from attack by other players as long as your inventory is empty.");
	}
	
	//checks whether players can create claims in a world
	public boolean claimsEnabledForWorld(World world)
	{
		return this.config_claims_enabledWorlds.contains(world);
	}
	
	//checks whether players siege in a world
	public boolean siegeEnabledForWorld(World world)
	{
		return this.config_siege_enabledWorlds.contains(world);
	}

	//processes broken log blocks to automatically remove floating treetops
	void handleLogBroken(Block block) 
	{
		//find the lowest log in the tree trunk including this log
		Block rootBlock = this.getRootBlock(block); 
		
		//null indicates this block isn't part of a tree trunk
		if(rootBlock == null) return;
		
		//next step: scan for other log blocks and leaves in this tree
		
		//set boundaries for the scan 
		int min_x = rootBlock.getX() - GriefPrevention.TREE_RADIUS;
		int max_x = rootBlock.getX() + GriefPrevention.TREE_RADIUS;
		int min_z = rootBlock.getZ() - GriefPrevention.TREE_RADIUS;
		int max_z = rootBlock.getZ() + GriefPrevention.TREE_RADIUS;
		int max_y = rootBlock.getWorld().getMaxHeight() - 1;
		
		//keep track of all the examined blocks, and all the log blocks found
		ArrayList<Block> examinedBlocks = new ArrayList<Block>();
		ArrayList<Block> treeBlocks = new ArrayList<Block>();
		
		//queue the first block, which is the block immediately above the player-chopped block
		ConcurrentLinkedQueue<Block> blocksToExamine = new ConcurrentLinkedQueue<Block>();
		blocksToExamine.add(rootBlock);
		examinedBlocks.add(rootBlock);
		
		boolean hasLeaves = false;
		
		while(!blocksToExamine.isEmpty())
		{
			//pop a block from the queue
			Block currentBlock = blocksToExamine.remove();
			
			//if this is a log block, determine whether it should be chopped
			if(currentBlock.getType() == Material.LOG)
			{
				boolean partOfTree = false;
				
				//if it's stacked with the original chopped block, the answer is always yes
				if(currentBlock.getX() == block.getX() && currentBlock.getZ() == block.getZ())
				{
					partOfTree = true;
				}
				
				//otherwise find the block underneath this stack of logs
				else
				{
					Block downBlock = currentBlock.getRelative(BlockFace.DOWN);
					while(downBlock.getType() == Material.LOG)
					{
						downBlock = downBlock.getRelative(BlockFace.DOWN);
					}
					
					//if it's air or leaves, it's okay to chop this block
					//this avoids accidentally chopping neighboring trees which are close enough to touch their leaves to ours
					if(downBlock.getType() == Material.AIR || downBlock.getType() == Material.LEAVES)
					{
						partOfTree = true;
					}
					
					//otherwise this is a stack of logs which touches a solid surface
					//if it's close to the original block's stack, don't clean up this tree (just stop here)
					else
					{
						if(Math.abs(downBlock.getX() - block.getX()) <= 1 && Math.abs(downBlock.getZ() - block.getZ()) <= 1) return;
					}
				}
				
				if(partOfTree)
				{
					treeBlocks.add(currentBlock);
				}
			}
			
			//if this block is a log OR a leaf block, also check its neighbors
			if(currentBlock.getType() == Material.LOG || currentBlock.getType() == Material.LEAVES)
			{
				if(currentBlock.getType() == Material.LEAVES)
				{
					hasLeaves = true;
				}
				
				Block [] neighboringBlocks = new Block [] 
				{
					currentBlock.getRelative(BlockFace.EAST),
					currentBlock.getRelative(BlockFace.WEST),
					currentBlock.getRelative(BlockFace.NORTH),
					currentBlock.getRelative(BlockFace.SOUTH),
					currentBlock.getRelative(BlockFace.UP),						
					currentBlock.getRelative(BlockFace.DOWN)
				};
				
				for(int i = 0; i < neighboringBlocks.length; i++)
				{
					Block neighboringBlock = neighboringBlocks[i];
											
					//if the neighboringBlock is out of bounds, skip it
					if(neighboringBlock.getX() < min_x || neighboringBlock.getX() > max_x || neighboringBlock.getZ() < min_z || neighboringBlock.getZ() > max_z || neighboringBlock.getY() > max_y) continue;						
					
					//if we already saw this block, skip it
					if(examinedBlocks.contains(neighboringBlock)) continue;
					
					//mark the block as examined
					examinedBlocks.add(neighboringBlock);
					
					//if the neighboringBlock is a leaf or log, put it in the queue to be examined later
					if(neighboringBlock.getType() == Material.LOG || neighboringBlock.getType() == Material.LEAVES)
					{
						blocksToExamine.add(neighboringBlock);
					}
					
					//if we encounter any player-placed block type, bail out (don't automatically remove parts of this tree, it might support a treehouse!)
					else if(this.isPlayerBlock(neighboringBlock)) 
					{
						return;						
					}
				}					
			}				
		}
		
		//if it doesn't have leaves, it's not a tree, so don't clean it up
		if(hasLeaves)
		{		
			//schedule a cleanup task for later, in case the player leaves part of this tree hanging in the air		
			TreeCleanupTask cleanupTask = new TreeCleanupTask(block, rootBlock, treeBlocks);
			
			//20L ~ 1 second, so 5 mins = 300 seconds ~ 6000L 
			GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, cleanupTask, 6000L);
		}
	}
	
	//helper for above, finds the "root" of a stack of logs
	//will return null if the stack is determined to not be a natural tree
	private Block getRootBlock(Block logBlock)
	{
		if(logBlock.getType() != Material.LOG) return null;
		
		//run down through log blocks until finding a non-log block
		Block underBlock = logBlock.getRelative(BlockFace.DOWN);
		while(underBlock.getType() == Material.LOG)
		{
			underBlock = underBlock.getRelative(BlockFace.DOWN);
		}
		
		//if this is a standard tree, that block MUST be dirt
		if(underBlock.getType() != Material.DIRT) return null;
		
		//run up through log blocks until finding a non-log block
		Block aboveBlock = logBlock.getRelative(BlockFace.UP);
		while(aboveBlock.getType() == Material.LOG)
		{
			aboveBlock = aboveBlock.getRelative(BlockFace.UP);
		}
		
		//if this is a standard tree, that block MUST be air or leaves
		if(aboveBlock.getType() != Material.AIR && aboveBlock.getType() != Material.LEAVES) return null;
		
		return underBlock.getRelative(BlockFace.UP);
	}
	
	//for sake of identifying trees ONLY, a cheap but not 100% reliable method for identifying player-placed blocks
	private boolean isPlayerBlock(Block block)
	{
		Material material = block.getType();
		
		//list of natural blocks which are OK to have next to a log block in a natural tree setting
		if(	material == Material.AIR || 
			material == Material.LEAVES || 
			material == Material.LOG || 
			material == Material.DIRT ||
			material == Material.GRASS ||			
			material == Material.STATIONARY_WATER ||
			material == Material.BROWN_MUSHROOM || 
			material == Material.RED_MUSHROOM ||
			material == Material.RED_ROSE ||
			material == Material.LONG_GRASS ||
			material == Material.SNOW ||
			material == Material.STONE ||
			material == Material.VINE ||
			material == Material.WATER_LILY ||
			material == Material.YELLOW_FLOWER ||
			material == Material.CLAY)
		{
			return false;
		}
		else
		{
			return true;
		}
	}
	
	//moves a player from the claim he's in to a nearby wilderness location
	public Location ejectPlayer(Player player)
	{
		//look for a suitable location
		Location candidateLocation = player.getLocation();
		while(true)
		{
			Claim claim = null;
			claim = GriefPrevention.instance.dataStore.getClaimAt(candidateLocation, false, null);
			
			//if there's a claim here, keep looking
			if(claim != null)
			{
				candidateLocation = new Location(claim.lesserBoundaryCorner.getWorld(), claim.lesserBoundaryCorner.getBlockX() - 1, claim.lesserBoundaryCorner.getBlockY(), claim.lesserBoundaryCorner.getBlockZ() - 1);
				continue;
			}
			
			//otherwise find a safe place to teleport the player
			else
			{
				//find a safe height, a couple of blocks above the surface
				GuaranteeChunkLoaded(candidateLocation);
				Block highestBlock = candidateLocation.getWorld().getHighestBlockAt(candidateLocation.getBlockX(), candidateLocation.getBlockZ());
				Location destination = new Location(highestBlock.getWorld(), highestBlock.getX(), highestBlock.getY() + 2, highestBlock.getZ());
				player.teleport(destination);			
				return destination;
			}			
		}
	}
	
	//ensures a piece of the managed world is loaded into server memory
	//(generates the chunk if necessary)
	private static void GuaranteeChunkLoaded(Location location)
	{
		Chunk chunk = location.getChunk();
		while(!chunk.isLoaded() || !chunk.load(true));
	}
	
	//sends a color-coded message to a player
	static void sendMessage(Player player, ChatColor color, String message)
	{
		player.sendMessage(color + message);
	}
}