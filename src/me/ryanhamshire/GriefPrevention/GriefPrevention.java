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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.World.Environment;
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
	public ArrayList<World> config_claims_enabledCreativeWorlds;	//list of worlds where additional creative mode anti-grief rules apply
	
	public boolean config_claims_preventTheft;						//whether containers and crafting blocks are protectable
	public boolean config_claims_protectCreatures;					//whether claimed animals may be injured by players without permission
	public boolean config_claims_preventButtonsSwitches;			//whether buttons and switches are protectable
	public boolean config_claims_lockWoodenDoors;					//whether wooden doors should be locked by default (require /accesstrust)
	public boolean config_claims_lockTrapDoors;						//whether trap doors should be locked by default (require /accesstrust)
	public boolean config_claims_lockFenceGates;					//whether fence gates should be locked by default (require /accesstrust)
	public boolean config_claims_enderPearlsRequireAccessTrust;		//whether teleporting into a claim with a pearl requires access trust
	
	public int config_claims_initialBlocks;							//the number of claim blocks a new player starts with
	public int config_claims_blocksAccruedPerHour;					//how many additional blocks players get each hour of play (can be zero)
	public int config_claims_maxAccruedBlocks;						//the limit on accrued blocks (over time).  doesn't limit purchased or admin-gifted blocks 
	public int config_claims_maxDepth;								//limit on how deep claims can go
	public int config_claims_expirationDays;						//how many days of inactivity before a player loses his claims
	
	public int config_claims_automaticClaimsForNewPlayersRadius;	//how big automatic new player claims (when they place a chest) should be.  0 to disable
	public boolean config_claims_creationRequiresPermission;		//whether creating claims with the shovel requires a permission
	public int config_claims_claimsExtendIntoGroundDistance;		//how far below the shoveled block a new claim will reach
	public int config_claims_minSize;								//minimum width and height for non-admin claims
	public boolean config_claims_allowUnclaimInCreative;			//whether players may unclaim land (resize or abandon) in creative mode
	public boolean config_claims_autoRestoreUnclaimedCreativeLand; 	//whether unclaimed land in creative worlds is automatically /restorenature-d
	
	public boolean config_claims_noBuildOutsideClaims;				//whether players can build in survival worlds outside their claimed areas
	
	public int config_claims_chestClaimExpirationDays;				//number of days of inactivity before an automatic chest claim will be deleted
	public int config_claims_unusedClaimExpirationDays;				//number of days of inactivity before an unused (nothing build) claim will be deleted
	public boolean config_claims_survivalAutoNatureRestoration;		//whether survival claims will be automatically restored to nature when auto-deleted
	public boolean config_claims_creativeAutoNatureRestoration;		//whether creative claims will be automatically restored to nature when auto-deleted
	
	public int config_claims_trappedCooldownHours;					//number of hours between uses of the /trapped command
	
	public Material config_claims_investigationTool;				//which material will be used to investigate claims with a right click
	public Material config_claims_modificationTool;	  				//which material will be used to create/resize claims with a right click
	
	public ArrayList<World> config_siege_enabledWorlds;				//whether or not /siege is enabled on this server
	public ArrayList<Material> config_siege_blocks;					//which blocks will be breakable in siege mode
		
	public boolean config_spam_enabled;								//whether or not to monitor for spam
	public int config_spam_loginCooldownMinutes;					//how long players must wait between logins.  combats login spam.
	public ArrayList<String> config_spam_monitorSlashCommands;  	//the list of slash commands monitored for spam
	public boolean config_spam_banOffenders;						//whether or not to ban spammers automatically
	public String config_spam_banMessage;							//message to show an automatically banned player
	public String config_spam_warningMessage;						//message to show a player who is close to spam level
	public String config_spam_allowedIpAddresses;					//IP addresses which will not be censored
	public int config_spam_deathMessageCooldownSeconds;				//cooldown period for death messages (per player) in seconds
	
	public ArrayList<World> config_pvp_enabledWorlds;				//list of worlds where pvp anti-grief rules apply
	public boolean config_pvp_protectFreshSpawns;					//whether to make newly spawned players immune until they pick up an item
	public boolean config_pvp_punishLogout;						    //whether to kill players who log out during PvP combat
	public int config_pvp_combatTimeoutSeconds;						//how long combat is considered to continue after the most recent damage
	public boolean config_pvp_allowCombatItemDrop;					//whether a player can drop items during combat to hide them
	public ArrayList<String> config_pvp_blockedCommands;			//list of commands which may not be used during pvp combat
	public boolean config_pvp_noCombatInPlayerLandClaims;			//whether players may fight in player-owned land claims
	public boolean config_pvp_noCombatInAdminLandClaims;			//whether players may fight in admin-owned land claims
	
	public boolean config_trees_removeFloatingTreetops;				//whether to automatically remove partially cut trees
	public boolean config_trees_regrowGriefedTrees;					//whether to automatically replant partially cut trees
	
	public double config_economy_claimBlocksPurchaseCost;			//cost to purchase a claim block.  set to zero to disable purchase.
	public double config_economy_claimBlocksSellValue;				//return on a sold claim block.  set to zero to disable sale.
	
	public boolean config_blockSurfaceCreeperExplosions;			//whether creeper explosions near or above the surface destroy blocks
	public boolean config_blockSurfaceOtherExplosions;				//whether non-creeper explosions near or above the surface destroy blocks
	public boolean config_blockWildernessWaterBuckets;				//whether players can dump water buckets outside their claims
	public boolean config_blockSkyTrees;							//whether players can build trees on platforms in the sky
	
	public boolean config_fireSpreads;								//whether fire spreads outside of claims
	public boolean config_fireDestroys;								//whether fire destroys blocks outside of claims
	
	public boolean config_addItemsToClaimedChests;					//whether players may add items to claimed chests by left-clicking them
	public boolean config_eavesdrop; 								//whether whispered messages will be visible to administrators
	public ArrayList<String> config_eavesdrop_whisperCommands;		//list of whisper commands to eavesdrop on
	
	public boolean config_smartBan;									//whether to ban accounts which very likely owned by a banned player
	
	public boolean config_endermenMoveBlocks;						//whether or not endermen may move blocks around
	public boolean config_silverfishBreakBlocks;					//whether silverfish may break blocks
	public boolean config_creaturesTrampleCrops;					//whether or not non-player entities may trample crops
	public boolean config_zombiesBreakDoors;						//whether or not hard-mode zombies may break down wooden doors
	
	public MaterialCollection config_mods_accessTrustIds;			//list of block IDs which should require /accesstrust for player interaction
	public MaterialCollection config_mods_containerTrustIds;		//list of block IDs which should require /containertrust for player interaction
	public List<String> config_mods_ignoreClaimsAccounts;			//list of player names which ALWAYS ignore claims
	public MaterialCollection config_mods_explodableIds;			//list of block IDs which can be destroyed by explosions, even in claimed areas

	public boolean config_claims_warnOnBuildOutside;				//whether players should be warned when they're building in an unclaimed area
	
	public HashMap<String, Integer> config_seaLevelOverride;		//override for sea level, because bukkit doesn't report the right value for all situations
	
	//reference to the economy plugin, if economy integration is enabled
	public static Economy economy = null;					
	
	//how far away to search from a tree trunk for its branch blocks
	public static final int TREE_RADIUS = 5;
	
	//how long to wait before deciding a player is staying online or staying offline, for notication messages
	public static final int NOTIFICATION_SECONDS = 20;
	
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
		FileConfiguration outConfig = new YamlConfiguration();
		
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
		
		//default creative claim world names
		List<String> defaultCreativeWorldNames = new ArrayList<String>();
		
		//if default game mode for the server is creative, creative rules will apply to all worlds unless the config specifies otherwise
		if(this.getServer().getDefaultGameMode() == GameMode.CREATIVE)
		{
			for(int i = 0; i < defaultClaimsWorldNames.size(); i++)
			{
				defaultCreativeWorldNames.add(defaultClaimsWorldNames.get(i));
			}			
		}
		
		//get creative world names from the config file
		List<String> creativeClaimsEnabledWorldNames = config.getStringList("GriefPrevention.Claims.CreativeRulesWorlds");
		if(creativeClaimsEnabledWorldNames == null || creativeClaimsEnabledWorldNames.size() == 0)
		{			
			creativeClaimsEnabledWorldNames = defaultCreativeWorldNames;
		}
		
		//validate that list
		this.config_claims_enabledCreativeWorlds = new ArrayList<World>();
		for(int i = 0; i < creativeClaimsEnabledWorldNames.size(); i++)
		{
			String worldName = creativeClaimsEnabledWorldNames.get(i);
			World world = this.getServer().getWorld(worldName);
			if(world == null)
			{
				AddLogEntry("Error: Claims Configuration: There's no world named \"" + worldName + "\".  Please update your config.yml.");
			}
			else
			{
				this.config_claims_enabledCreativeWorlds.add(world);
			}
		}
		
		//default for pvp worlds list
		ArrayList<String> defaultPvpWorldNames = new ArrayList<String>();
		for(int i = 0; i < worlds.size(); i++)			
		{
			World world = worlds.get(i); 
			if(world.getPVP())
			{
				defaultPvpWorldNames.add(world.getName());
			}
		}
		
		//get pvp world names from the config file
		List<String> pvpEnabledWorldNames = config.getStringList("GriefPrevention.PvP.Worlds");
		if(pvpEnabledWorldNames == null || pvpEnabledWorldNames.size() == 0)
		{			
			pvpEnabledWorldNames = defaultPvpWorldNames;
		}
		
		//validate that list
		this.config_pvp_enabledWorlds = new ArrayList<World>();
		for(int i = 0; i < pvpEnabledWorldNames.size(); i++)
		{
			String worldName = pvpEnabledWorldNames.get(i);
			World world = this.getServer().getWorld(worldName);
			if(world == null)
			{
				AddLogEntry("Error: PvP Configuration: There's no world named \"" + worldName + "\".  Please update your config.yml.");
			}
			else
			{
				this.config_pvp_enabledWorlds.add(world);
			}
		}
		
		//sea level
		this.config_seaLevelOverride = new HashMap<String, Integer>();
		for(int i = 0; i < worlds.size(); i++)
		{
			int seaLevelOverride = config.getInt("GriefPrevention.SeaLevelOverrides." + worlds.get(i).getName(), -1);
			outConfig.set("GriefPrevention.SeaLevelOverrides." + worlds.get(i).getName(), seaLevelOverride);
			this.config_seaLevelOverride.put(worlds.get(i).getName(), seaLevelOverride);
		}
		
		this.config_claims_preventTheft = config.getBoolean("GriefPrevention.Claims.PreventTheft", true);
		this.config_claims_protectCreatures = config.getBoolean("GriefPrevention.Claims.ProtectCreatures", true);
		this.config_claims_preventButtonsSwitches = config.getBoolean("GriefPrevention.Claims.PreventButtonsSwitches", true);
		this.config_claims_lockWoodenDoors = config.getBoolean("GriefPrevention.Claims.LockWoodenDoors", false);
		this.config_claims_lockTrapDoors = config.getBoolean("GriefPrevention.Claims.LockTrapDoors", false);
		this.config_claims_lockFenceGates = config.getBoolean("GriefPrevention.Claims.LockFenceGates", true);
		this.config_claims_enderPearlsRequireAccessTrust = config.getBoolean("GriefPrevention.Claims.EnderPearlsRequireAccessTrust", true);
		this.config_claims_initialBlocks = config.getInt("GriefPrevention.Claims.InitialBlocks", 100);
		this.config_claims_blocksAccruedPerHour = config.getInt("GriefPrevention.Claims.BlocksAccruedPerHour", 100);
		this.config_claims_maxAccruedBlocks = config.getInt("GriefPrevention.Claims.MaxAccruedBlocks", 80000);
		this.config_claims_automaticClaimsForNewPlayersRadius = config.getInt("GriefPrevention.Claims.AutomaticNewPlayerClaimsRadius", 4);
		this.config_claims_claimsExtendIntoGroundDistance = config.getInt("GriefPrevention.Claims.ExtendIntoGroundDistance", 5);
		this.config_claims_creationRequiresPermission = config.getBoolean("GriefPrevention.Claims.CreationRequiresPermission", false);
		this.config_claims_minSize = config.getInt("GriefPrevention.Claims.MinimumSize", 10);
		this.config_claims_maxDepth = config.getInt("GriefPrevention.Claims.MaximumDepth", 0);
		this.config_claims_trappedCooldownHours = config.getInt("GriefPrevention.Claims.TrappedCommandCooldownHours", 8);
		this.config_claims_noBuildOutsideClaims = config.getBoolean("GriefPrevention.Claims.NoSurvivalBuildingOutsideClaims", false);
		this.config_claims_warnOnBuildOutside = config.getBoolean("GriefPrevention.Claims.WarnWhenBuildingOutsideClaims", true);
		this.config_claims_allowUnclaimInCreative = config.getBoolean("GriefPrevention.Claims.AllowUnclaimingCreativeModeLand", true);
		this.config_claims_autoRestoreUnclaimedCreativeLand = config.getBoolean("GriefPrevention.Claims.AutoRestoreUnclaimedCreativeLand", true);		

		this.config_claims_chestClaimExpirationDays = config.getInt("GriefPrevention.Claims.Expiration.ChestClaimDays", 7);
		outConfig.set("GriefPrevention.Claims.Expiration.ChestClaimDays", this.config_claims_chestClaimExpirationDays);
		
		this.config_claims_unusedClaimExpirationDays = config.getInt("GriefPrevention.Claims.Expiration.UnusedClaimDays", 14);
		outConfig.set("GriefPrevention.Claims.Expiration.UnusedClaimDays", this.config_claims_unusedClaimExpirationDays);		
		
		this.config_claims_expirationDays = config.getInt("GriefPrevention.Claims.Expiration.AllClaimDays", 0);
		outConfig.set("GriefPrevention.Claims.Expiration.AllClaimDays", this.config_claims_expirationDays);
		
		this.config_claims_survivalAutoNatureRestoration = config.getBoolean("GriefPrevention.Claims.Expiration.AutomaticNatureRestoration.SurvivalWorlds", false);
		outConfig.set("GriefPrevention.Claims.Expiration.AutomaticNatureRestoration.SurvivalWorlds", this.config_claims_survivalAutoNatureRestoration);		
		
		this.config_claims_creativeAutoNatureRestoration = config.getBoolean("GriefPrevention.Claims.Expiration.AutomaticNatureRestoration.CreativeWorlds", true);
		outConfig.set("GriefPrevention.Claims.Expiration.AutomaticNatureRestoration.CreativeWorlds", this.config_claims_creativeAutoNatureRestoration);		
		
		this.config_spam_enabled = config.getBoolean("GriefPrevention.Spam.Enabled", true);
		this.config_spam_loginCooldownMinutes = config.getInt("GriefPrevention.Spam.LoginCooldownMinutes", 2);
		this.config_spam_warningMessage = config.getString("GriefPrevention.Spam.WarningMessage", "Please reduce your noise level.  Spammers will be banned.");
		this.config_spam_allowedIpAddresses = config.getString("GriefPrevention.Spam.AllowedIpAddresses", "1.2.3.4; 5.6.7.8");
		this.config_spam_banOffenders = config.getBoolean("GriefPrevention.Spam.BanOffenders", true);		
		this.config_spam_banMessage = config.getString("GriefPrevention.Spam.BanMessage", "Banned for spam.");
		String slashCommandsToMonitor = config.getString("GriefPrevention.Spam.MonitorSlashCommands", "/me;/tell;/global;/local");
		this.config_spam_deathMessageCooldownSeconds = config.getInt("GriefPrevention.Spam.DeathMessageCooldownSeconds", 60);		
		
		this.config_pvp_protectFreshSpawns = config.getBoolean("GriefPrevention.PvP.ProtectFreshSpawns", true);
		this.config_pvp_punishLogout = config.getBoolean("GriefPrevention.PvP.PunishLogout", true);
		this.config_pvp_combatTimeoutSeconds = config.getInt("GriefPrevention.PvP.CombatTimeoutSeconds", 15);
		this.config_pvp_allowCombatItemDrop = config.getBoolean("GriefPrevention.PvP.AllowCombatItemDrop", false);
		String bannedPvPCommandsList = config.getString("GriefPrevention.PvP.BlockedSlashCommands", "/home;/vanish;/spawn;/tpa");
		
		this.config_trees_removeFloatingTreetops = config.getBoolean("GriefPrevention.Trees.RemoveFloatingTreetops", true);
		this.config_trees_regrowGriefedTrees = config.getBoolean("GriefPrevention.Trees.RegrowGriefedTrees", true);
		
		this.config_economy_claimBlocksPurchaseCost = config.getDouble("GriefPrevention.Economy.ClaimBlocksPurchaseCost", 0);
		this.config_economy_claimBlocksSellValue = config.getDouble("GriefPrevention.Economy.ClaimBlocksSellValue", 0);
		
		this.config_blockSurfaceCreeperExplosions = config.getBoolean("GriefPrevention.BlockSurfaceCreeperExplosions", true);
		this.config_blockSurfaceOtherExplosions = config.getBoolean("GriefPrevention.BlockSurfaceOtherExplosions", true);
		this.config_blockWildernessWaterBuckets = config.getBoolean("GriefPrevention.LimitSurfaceWaterBuckets", true);
		this.config_blockSkyTrees = config.getBoolean("GriefPrevention.LimitSkyTrees", true);
				
		this.config_fireSpreads = config.getBoolean("GriefPrevention.FireSpreads", false);
		this.config_fireDestroys = config.getBoolean("GriefPrevention.FireDestroys", false);
		
		this.config_addItemsToClaimedChests = config.getBoolean("GriefPrevention.AddItemsToClaimedChests", true);
		this.config_eavesdrop = config.getBoolean("GriefPrevention.EavesdropEnabled", false);
		String whisperCommandsToMonitor = config.getString("GriefPrevention.WhisperCommands", "/tell;/pm;/r");
		
		this.config_smartBan = config.getBoolean("GriefPrevention.SmartBan", true);
		
		this.config_endermenMoveBlocks = config.getBoolean("GriefPrevention.EndermenMoveBlocks", false);
		this.config_silverfishBreakBlocks = config.getBoolean("GriefPrevention.SilverfishBreakBlocks", false);
		this.config_creaturesTrampleCrops = config.getBoolean("GriefPrevention.CreaturesTrampleCrops", false);
		this.config_zombiesBreakDoors = config.getBoolean("GriefPrevention.HardModeZombiesBreakDoors", false);
		
		this.config_mods_ignoreClaimsAccounts = config.getStringList("GriefPrevention.Mods.PlayersIgnoringAllClaims");
		
		if(this.config_mods_ignoreClaimsAccounts == null) this.config_mods_ignoreClaimsAccounts = new ArrayList<String>();
		
		this.config_mods_accessTrustIds = new MaterialCollection();
		List<String> accessTrustStrings = config.getStringList("GriefPrevention.Mods.BlockIdsRequiringAccessTrust");
		
		//default values for access trust mod blocks
		if(accessTrustStrings == null || accessTrustStrings.size() == 0)
		{
			//none by default
		}
		
		this.parseMaterialListFromConfig(accessTrustStrings, this.config_mods_accessTrustIds);
		
		this.config_mods_containerTrustIds = new MaterialCollection();
		List<String> containerTrustStrings = config.getStringList("GriefPrevention.Mods.BlockIdsRequiringContainerTrust");
		
		//default values for container trust mod blocks
		if(containerTrustStrings == null || containerTrustStrings.size() == 0)
		{
			containerTrustStrings.add(new MaterialInfo(227, "Battery Box").toString());
			containerTrustStrings.add(new MaterialInfo(130, "Transmutation Tablet").toString());
			containerTrustStrings.add(new MaterialInfo(128, "Alchemical Chest and Energy Condenser").toString());
			containerTrustStrings.add(new MaterialInfo(181, "Various Chests").toString());
			containerTrustStrings.add(new MaterialInfo(178, "Ender Chest").toString());
			containerTrustStrings.add(new MaterialInfo(150, "Various BuildCraft Gadgets").toString());
			containerTrustStrings.add(new MaterialInfo(155, "Filler").toString());
			containerTrustStrings.add(new MaterialInfo(157, "Builder").toString());
			containerTrustStrings.add(new MaterialInfo(158, "Template Drawing Table").toString());
			containerTrustStrings.add(new MaterialInfo(126, "Various EE Gadgets").toString());
			containerTrustStrings.add(new MaterialInfo(138, "Various RedPower Gadgets").toString());
			containerTrustStrings.add(new MaterialInfo(137, "BuildCraft Project Table and Furnaces").toString());
			containerTrustStrings.add(new MaterialInfo(250, "Various IC2 Machines").toString());
			containerTrustStrings.add(new MaterialInfo(161, "BuildCraft Engines").toString());
			containerTrustStrings.add(new MaterialInfo(169, "Automatic Crafting Table").toString());
			containerTrustStrings.add(new MaterialInfo(177, "Wireless Components").toString());
			containerTrustStrings.add(new MaterialInfo(183, "Solar Arrays").toString());
			containerTrustStrings.add(new MaterialInfo(187, "Charging Benches").toString());
			containerTrustStrings.add(new MaterialInfo(188, "More IC2 Machines").toString());
			containerTrustStrings.add(new MaterialInfo(190, "Generators, Fabricators, Strainers").toString());
			containerTrustStrings.add(new MaterialInfo(194, "More Gadgets").toString());
			containerTrustStrings.add(new MaterialInfo(207, "Computer").toString());
			containerTrustStrings.add(new MaterialInfo(208, "Computer Peripherals").toString());
			containerTrustStrings.add(new MaterialInfo(246, "IC2 Generators").toString());
			containerTrustStrings.add(new MaterialInfo(24303, "Teleport Pipe").toString());
			containerTrustStrings.add(new MaterialInfo(24304, "Waterproof Teleport Pipe").toString());
			containerTrustStrings.add(new MaterialInfo(24305, "Power Teleport Pipe").toString());
			containerTrustStrings.add(new MaterialInfo(4311, "Diamond Sorting Pipe").toString());
			containerTrustStrings.add(new MaterialInfo(216, "Turtle").toString());
			
		}
		
		//parse the strings from the config file
		this.parseMaterialListFromConfig(containerTrustStrings, this.config_mods_containerTrustIds);
		
		this.config_mods_explodableIds = new MaterialCollection();
		List<String> explodableStrings = config.getStringList("GriefPrevention.Mods.BlockIdsExplodable");
		
		//default values for explodable mod blocks
		if(explodableStrings == null || explodableStrings.size() == 0)
		{
			explodableStrings.add(new MaterialInfo(161, "BuildCraft Engines").toString());			
			explodableStrings.add(new MaterialInfo(246, (byte)5 ,"Nuclear Reactor").toString());
		}
		
		//parse the strings from the config file
		this.parseMaterialListFromConfig(explodableStrings, this.config_mods_explodableIds);
		
		//default for claim investigation tool
		String investigationToolMaterialName = Material.STICK.name();
		
		//get investigation tool from config
		investigationToolMaterialName = config.getString("GriefPrevention.Claims.InvestigationTool", investigationToolMaterialName);
		
		//validate investigation tool
		this.config_claims_investigationTool = Material.getMaterial(investigationToolMaterialName);
		if(this.config_claims_investigationTool == null)
		{
			GriefPrevention.AddLogEntry("ERROR: Material " + investigationToolMaterialName + " not found.  Defaulting to the stick.  Please update your config.yml.");
			this.config_claims_investigationTool = Material.STICK;
		}
		
		//default for claim creation/modification tool
		String modificationToolMaterialName = Material.GOLD_SPADE.name();
		
		//get modification tool from config
		modificationToolMaterialName = config.getString("GriefPrevention.Claims.ModificationTool", modificationToolMaterialName);
		
		//validate modification tool
		this.config_claims_modificationTool = Material.getMaterial(modificationToolMaterialName);
		if(this.config_claims_modificationTool == null)
		{
			GriefPrevention.AddLogEntry("ERROR: Material " + modificationToolMaterialName + " not found.  Defaulting to the golden shovel.  Please update your config.yml.");
			this.config_claims_modificationTool = Material.GOLD_SPADE;
		}
		
		//default for siege worlds list
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
		
		this.config_pvp_noCombatInPlayerLandClaims = config.getBoolean("GriefPrevention.PvP.ProtectPlayersInLandClaims.PlayerOwnedClaims", this.config_siege_enabledWorlds.size() == 0);
		this.config_pvp_noCombatInAdminLandClaims = config.getBoolean("GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeClaims", this.config_siege_enabledWorlds.size() == 0);
		
		//optional database settings
		String databaseUrl = config.getString("GriefPrevention.Database.URL", "");
		String databaseUserName = config.getString("GriefPrevention.Database.UserName", "");
		String databasePassword = config.getString("GriefPrevention.Database.Password", "");
		
		outConfig.set("GriefPrevention.Claims.Worlds", claimsEnabledWorldNames);
		outConfig.set("GriefPrevention.Claims.CreativeRulesWorlds", creativeClaimsEnabledWorldNames);
		outConfig.set("GriefPrevention.Claims.PreventTheft", this.config_claims_preventTheft);
		outConfig.set("GriefPrevention.Claims.ProtectCreatures", this.config_claims_protectCreatures);
		outConfig.set("GriefPrevention.Claims.PreventButtonsSwitches", this.config_claims_preventButtonsSwitches);
		outConfig.set("GriefPrevention.Claims.LockWoodenDoors", this.config_claims_lockWoodenDoors);
		outConfig.set("GriefPrevention.Claims.LockTrapDoors", this.config_claims_lockTrapDoors);
		outConfig.set("GriefPrevention.Claims.LockFenceGates", this.config_claims_lockFenceGates);
		outConfig.set("GriefPrevention.Claims.EnderPearlsRequireAccessTrust", this.config_claims_enderPearlsRequireAccessTrust);
		outConfig.set("GriefPrevention.Claims.InitialBlocks", this.config_claims_initialBlocks);
		outConfig.set("GriefPrevention.Claims.BlocksAccruedPerHour", this.config_claims_blocksAccruedPerHour);
		outConfig.set("GriefPrevention.Claims.MaxAccruedBlocks", this.config_claims_maxAccruedBlocks);
		outConfig.set("GriefPrevention.Claims.AutomaticNewPlayerClaimsRadius", this.config_claims_automaticClaimsForNewPlayersRadius);
		outConfig.set("GriefPrevention.Claims.ExtendIntoGroundDistance", this.config_claims_claimsExtendIntoGroundDistance);
		outConfig.set("GriefPrevention.Claims.CreationRequiresPermission", this.config_claims_creationRequiresPermission);
		outConfig.set("GriefPrevention.Claims.MinimumSize", this.config_claims_minSize);
		outConfig.set("GriefPrevention.Claims.MaximumDepth", this.config_claims_maxDepth);
		outConfig.set("GriefPrevention.Claims.IdleLimitDays", this.config_claims_expirationDays);
		outConfig.set("GriefPrevention.Claims.TrappedCommandCooldownHours", this.config_claims_trappedCooldownHours);
		outConfig.set("GriefPrevention.Claims.InvestigationTool", this.config_claims_investigationTool.name());
		outConfig.set("GriefPrevention.Claims.ModificationTool", this.config_claims_modificationTool.name());
		outConfig.set("GriefPrevention.Claims.NoSurvivalBuildingOutsideClaims", this.config_claims_noBuildOutsideClaims);
		outConfig.set("GriefPrevention.Claims.WarnWhenBuildingOutsideClaims", this.config_claims_warnOnBuildOutside);
		outConfig.set("GriefPrevention.Claims.AllowUnclaimingCreativeModeLand", this.config_claims_allowUnclaimInCreative);
		outConfig.set("GriefPrevention.Claims.AutoRestoreUnclaimedCreativeLand", this.config_claims_autoRestoreUnclaimedCreativeLand);
		
		outConfig.set("GriefPrevention.Spam.Enabled", this.config_spam_enabled);
		outConfig.set("GriefPrevention.Spam.LoginCooldownMinutes", this.config_spam_loginCooldownMinutes);
		outConfig.set("GriefPrevention.Spam.MonitorSlashCommands", slashCommandsToMonitor);
		outConfig.set("GriefPrevention.Spam.WarningMessage", this.config_spam_warningMessage);
		outConfig.set("GriefPrevention.Spam.BanOffenders", this.config_spam_banOffenders);		
		outConfig.set("GriefPrevention.Spam.BanMessage", this.config_spam_banMessage);
		outConfig.set("GriefPrevention.Spam.AllowedIpAddresses", this.config_spam_allowedIpAddresses);
		outConfig.set("GriefPrevention.Spam.DeathMessageCooldownSeconds", this.config_spam_deathMessageCooldownSeconds);
		
		outConfig.set("GriefPrevention.PvP.Worlds", pvpEnabledWorldNames);
		outConfig.set("GriefPrevention.PvP.ProtectFreshSpawns", this.config_pvp_protectFreshSpawns);
		outConfig.set("GriefPrevention.PvP.PunishLogout", this.config_pvp_punishLogout);
		outConfig.set("GriefPrevention.PvP.CombatTimeoutSeconds", this.config_pvp_combatTimeoutSeconds);
		outConfig.set("GriefPrevention.PvP.AllowCombatItemDrop", this.config_pvp_allowCombatItemDrop);
		outConfig.set("GriefPrevention.PvP.BlockedSlashCommands", bannedPvPCommandsList);
		outConfig.set("GriefPrevention.PvP.ProtectPlayersInLandClaims.PlayerOwnedClaims", this.config_pvp_noCombatInPlayerLandClaims);
		outConfig.set("GriefPrevention.PvP.ProtectPlayersInLandClaims.AdministrativeClaims", this.config_pvp_noCombatInAdminLandClaims);
		
		outConfig.set("GriefPrevention.Trees.RemoveFloatingTreetops", this.config_trees_removeFloatingTreetops);
		outConfig.set("GriefPrevention.Trees.RegrowGriefedTrees", this.config_trees_regrowGriefedTrees);
		
		outConfig.set("GriefPrevention.Economy.ClaimBlocksPurchaseCost", this.config_economy_claimBlocksPurchaseCost);
		outConfig.set("GriefPrevention.Economy.ClaimBlocksSellValue", this.config_economy_claimBlocksSellValue);
		
		outConfig.set("GriefPrevention.BlockSurfaceCreeperExplosions", this.config_blockSurfaceCreeperExplosions);
		outConfig.set("GriefPrevention.BlockSurfaceOtherExplosions", this.config_blockSurfaceOtherExplosions);
		outConfig.set("GriefPrevention.LimitSurfaceWaterBuckets", this.config_blockWildernessWaterBuckets);
		outConfig.set("GriefPrevention.LimitSkyTrees", this.config_blockSkyTrees);
		
		outConfig.set("GriefPrevention.FireSpreads", this.config_fireSpreads);
		outConfig.set("GriefPrevention.FireDestroys", this.config_fireDestroys);
		
		outConfig.set("GriefPrevention.AddItemsToClaimedChests", this.config_addItemsToClaimedChests);
		
		outConfig.set("GriefPrevention.EavesdropEnabled", this.config_eavesdrop);		
		outConfig.set("GriefPrevention.WhisperCommands", whisperCommandsToMonitor);		
		outConfig.set("GriefPrevention.SmartBan", this.config_smartBan);
		
		outConfig.set("GriefPrevention.Siege.Worlds", siegeEnabledWorldNames);
		outConfig.set("GriefPrevention.Siege.BreakableBlocks", breakableBlocksList);
		
		outConfig.set("GriefPrevention.EndermenMoveBlocks", this.config_endermenMoveBlocks);
		outConfig.set("GriefPrevention.SilverfishBreakBlocks", this.config_silverfishBreakBlocks);		
		outConfig.set("GriefPrevention.CreaturesTrampleCrops", this.config_creaturesTrampleCrops);
		outConfig.set("GriefPrevention.HardModeZombiesBreakDoors", this.config_zombiesBreakDoors);		
		
		outConfig.set("GriefPrevention.Database.URL", databaseUrl);
		outConfig.set("GriefPrevention.Database.UserName", databaseUserName);
		outConfig.set("GriefPrevention.Database.Password", databasePassword);		
		
		outConfig.set("GriefPrevention.Mods.BlockIdsRequiringAccessTrust", this.config_mods_accessTrustIds);
		outConfig.set("GriefPrevention.Mods.BlockIdsRequiringContainerTrust", this.config_mods_containerTrustIds);
		outConfig.set("GriefPrevention.Mods.BlockIdsExplodable", this.config_mods_explodableIds);
		outConfig.set("GriefPrevention.Mods.PlayersIgnoringAllClaims", this.config_mods_ignoreClaimsAccounts);
		outConfig.set("GriefPrevention.Mods.BlockIdsRequiringAccessTrust", accessTrustStrings);
		outConfig.set("GriefPrevention.Mods.BlockIdsRequiringContainerTrust", containerTrustStrings);
		outConfig.set("GriefPrevention.Mods.BlockIdsExplodable", explodableStrings);
		
		try
		{
			outConfig.save(DataStore.configFilePath);
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
		
		//try to parse the list of commands which should be included in eavesdropping
		this.config_eavesdrop_whisperCommands  = new ArrayList<String>();
		commands = whisperCommandsToMonitor.split(";");
		for(int i = 0; i < commands.length; i++)
		{
			this.config_eavesdrop_whisperCommands.add(commands[i].trim());
		}		
		
		//try to parse the list of commands which should be banned during pvp combat
		this.config_pvp_blockedCommands = new ArrayList<String>();
		commands = bannedPvPCommandsList.split(";");
		for(int i = 0; i < commands.length; i++)
		{
			this.config_pvp_blockedCommands.add(commands[i].trim());
		}
		
		//when datastore initializes, it loads player and claim data, and posts some stats to the log
		if(databaseUrl.length() > 0)
		{
			try
			{
				DatabaseDataStore databaseStore = new DatabaseDataStore(databaseUrl, databaseUserName, databasePassword);
			
				if(FlatFileDataStore.hasData())
				{
					GriefPrevention.AddLogEntry("There appears to be some data on the hard drive.  Migrating those data to the database...");
					FlatFileDataStore flatFileStore = new FlatFileDataStore();
					flatFileStore.migrateData(databaseStore);
					GriefPrevention.AddLogEntry("Data migration process complete.  Reloading data from the database...");
					databaseStore.close();
					databaseStore = new DatabaseDataStore(databaseUrl, databaseUserName, databasePassword);
				}
				
				this.dataStore = databaseStore;
			}
			catch(Exception e)
			{
				GriefPrevention.AddLogEntry("Because there was a problem with the database, GriefPrevention will not function properly.  Either update the database config settings resolve the issue, or delete those lines from your config.yml so that GriefPrevention can use the file system to store data.");
				return;
			}			
		}
		
		//if not using the database because it's not configured or because there was a problem, use the file system to store data
		//this is the preferred method, as it's simpler than the database scenario
		if(this.dataStore == null)
		{
			try
			{
				this.dataStore = new FlatFileDataStore();
			}
			catch(Exception e)
			{
				GriefPrevention.AddLogEntry("Unable to initialize the file system data store.  Details:");
				GriefPrevention.AddLogEntry(e.getMessage());
			}
		}
		
		//unless claim block accrual is disabled, start the recurring per 5 minute event to give claim blocks to online players
		//20L ~ 1 second
		if(this.config_claims_blocksAccruedPerHour > 0)
		{
			DeliverClaimBlocksTask task = new DeliverClaimBlocksTask();
			this.getServer().getScheduler().scheduleSyncRepeatingTask(this, task, 20L * 60 * 5, 20L * 60 * 5);
		}
		
		//start the recurring cleanup event for entities in creative worlds
		EntityCleanupTask task = new EntityCleanupTask(0);
		this.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, task, 20L);
		
		//start recurring cleanup scan for unused claims belonging to inactive players
		CleanupUnusedClaimsTask task2 = new CleanupUnusedClaimsTask();
		this.getServer().getScheduler().scheduleSyncRepeatingTask(this, task2, 20L * 60 * 2, 20L * 60 * 5);
		
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
			return this.abandonClaimHandler(player, false);
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
				GriefPrevention.sendMessage(player, TextMode.Success, Messages.RespectingClaims);
			}
			else
			{
				GriefPrevention.sendMessage(player, TextMode.Success, Messages.IgnoringClaims);
			}
			
			return true;
		}
		
		//abandonallclaims
		else if(cmd.getName().equalsIgnoreCase("abandonallclaims") && player != null)
		{
			if(args.length != 0) return false;
			
			if(!GriefPrevention.instance.config_claims_allowUnclaimInCreative && creativeRulesApply(player.getLocation()))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoCreativeUnClaim);
				return true;
			}
			
			//count claims
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			int originalClaimCount = playerData.claims.size();
			
			//check count
			if(originalClaimCount == 0)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.YouHaveNoClaims);
				return true;
			}
			
			//delete them
			this.dataStore.deleteClaimsForPlayer(player.getName(), false);
			
			//inform the player
			int remainingBlocks = playerData.getRemainingClaimBlocks();
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.SuccessfulAbandon, String.valueOf(remainingBlocks));
			
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
			GriefPrevention.sendMessage(player, TextMode.Instr, Messages.RestoreNatureActivate);
			return true;
		}
		
		//restore nature aggressive mode
		else if(cmd.getName().equalsIgnoreCase("restorenatureaggressive") && player != null)
		{
			//change shovel mode
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			playerData.shovelMode = ShovelMode.RestoreNatureAggressive;
			GriefPrevention.sendMessage(player, TextMode.Warn, Messages.RestoreNatureAggressiveActivate);
			return true;
		}
		
		//restore nature fill mode
		else if(cmd.getName().equalsIgnoreCase("restorenaturefill") && player != null)
		{
			//change shovel mode
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			playerData.shovelMode = ShovelMode.RestoreNatureFill;
			
			//set radius based on arguments
			playerData.fillRadius = 2;
			if(args.length > 0)
			{
				try
				{
					playerData.fillRadius = Integer.parseInt(args[0]);
				}
				catch(Exception exception){ }
			}
			
			if(playerData.fillRadius < 0) playerData.fillRadius = 2;
			
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.FillModeActive, String.valueOf(playerData.fillRadius));
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
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.TransferClaimPermission);
				return true;
			}
			
			//which claim is the user in?
			Claim claim = this.dataStore.getClaimAt(player.getLocation(), true, null);
			if(claim == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Instr, Messages.TransferClaimMissing);
				return true;
			}
			
			OfflinePlayer targetPlayer = this.resolvePlayer(args[0]);
			if(targetPlayer == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
				return true;
			}
			
			//change ownerhsip
			try
			{
				this.dataStore.changeClaimOwner(claim, targetPlayer.getName());
			}
			catch(Exception e)
			{
				GriefPrevention.sendMessage(player, TextMode.Instr, Messages.TransferTopLevel);
				return true;
			}
			
			//confirm
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.TransferSuccess);
			GriefPrevention.AddLogEntry(player.getName() + " transferred a claim at " + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()) + " to " + targetPlayer.getName() + ".");
			
			return true;
		}
		
		//trustlist
		else if(cmd.getName().equalsIgnoreCase("trustlist") && player != null)
		{
			Claim claim = this.dataStore.getClaimAt(player.getLocation(), true, null);
			
			//if no claim here, error message
			if(claim == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrustListNoClaim);
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
		
		//untrust <player> or untrust [<group>]
		else if(cmd.getName().equalsIgnoreCase("untrust") && player != null)
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;
			
			//determine which claim the player is standing in
			Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
			
			//bracket any permissions
			if(args[0].contains("."))
			{
				args[0] = "[" + args[0] + "]";
			}
			
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
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClearPermsOwnerOnly);
					return true;
				}
			}
			
			else
			{
				//validate player argument or group argument
				if(!args[0].startsWith("[") || !args[0].endsWith("]"))
				{
					otherPlayer = this.resolvePlayer(args[0]);
					if(!clearPermissions && otherPlayer == null && !args[0].equals("public"))
					{
						GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
						return true;
					}
					
					//correct to proper casing
					if(otherPlayer != null)
						args[0] = otherPlayer.getName();
				}
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
					GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustIndividualAllClaims, args[0]);
				}
				else
				{
					GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustEveryoneAllClaims);
				}
			}			
			
			//otherwise, apply changes to only this claim
			else if(claim.allowGrantPermission(player) != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoPermissionTrust, claim.getOwnerName());
			}
			else
			{
				//if clearing all
				if(clearPermissions)
				{
					claim.clearPermissions();
					GriefPrevention.sendMessage(player, TextMode.Success, Messages.ClearPermissionsOneClaim);
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
						
						GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustIndividualSingleClaim, args[0]);
					}
					else
					{
						GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustOwnerOnly, claim.getOwnerName());
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
			if(GriefPrevention.economy == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.BuySellNotConfigured);
				return true;
			}
			
			if(!player.hasPermission("griefprevention.buysellclaimblocks"))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoPermissionForCommand);
				return true;
			}
			
			//if purchase disabled, send error message
			if(GriefPrevention.instance.config_economy_claimBlocksPurchaseCost == 0)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.OnlySellBlocks);
				return true;
			}
			
			//if no parameter, just tell player cost per block and balance
			if(args.length != 1)
			{
				GriefPrevention.sendMessage(player, TextMode.Info, Messages.BlockPurchaseCost, String.valueOf(GriefPrevention.instance.config_economy_claimBlocksPurchaseCost), String.valueOf(GriefPrevention.economy.getBalance(player.getName())));
				return false;
			}
			
			else
			{
				//determine max purchasable blocks
				PlayerData playerData = this.dataStore.getPlayerData(player.getName());
				int maxPurchasable = GriefPrevention.instance.config_claims_maxAccruedBlocks - playerData.accruedClaimBlocks;
				
				//if the player is at his max, tell him so
				if(maxPurchasable <= 0)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClaimBlockLimit);
					return true;
				}
				
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
				
				if(blockCount <= 0)
				{
					return false;
				}
				
				//correct block count to max allowed
				if(blockCount > maxPurchasable)
				{
					blockCount = maxPurchasable;
				}
				
				//if the player can't afford his purchase, send error message
				double balance = economy.getBalance(player.getName());				
				double totalCost = blockCount * GriefPrevention.instance.config_economy_claimBlocksPurchaseCost;				
				if(totalCost > balance)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.InsufficientFunds, String.valueOf(totalCost),  String.valueOf(balance));
				}
				
				//otherwise carry out transaction
				else
				{
					//withdraw cost
					economy.withdrawPlayer(player.getName(), totalCost);
					
					//add blocks
					playerData.accruedClaimBlocks += blockCount;
					this.dataStore.savePlayerData(player.getName(), playerData);
					
					//inform player
					GriefPrevention.sendMessage(player, TextMode.Success, Messages.PurchaseConfirmation, String.valueOf(totalCost), String.valueOf(playerData.getRemainingClaimBlocks()));
				}
				
				return true;
			}
		}
		
		//sellclaimblocks <amount> 
		else if(cmd.getName().equalsIgnoreCase("sellclaimblocks") && player != null)
		{
			//if economy is disabled, don't do anything
			if(GriefPrevention.economy == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.BuySellNotConfigured);
				return true;
			}
			
			if(!player.hasPermission("griefprevention.buysellclaimblocks"))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoPermissionForCommand);
				return true;
			}
			
			//if disabled, error message
			if(GriefPrevention.instance.config_economy_claimBlocksSellValue == 0)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.OnlyPurchaseBlocks);
				return true;
			}
			
			//load player data
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			int availableBlocks = playerData.getRemainingClaimBlocks();
			
			//if no amount provided, just tell player value per block sold, and how many he can sell
			if(args.length != 1)
			{
				GriefPrevention.sendMessage(player, TextMode.Info, Messages.BlockSaleValue, String.valueOf(GriefPrevention.instance.config_economy_claimBlocksSellValue), String.valueOf(availableBlocks));
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
			
			if(blockCount <= 0)
			{
				return false;
			}
			
			//if he doesn't have enough blocks, tell him so
			if(blockCount > availableBlocks)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotEnoughBlocksForSale);
			}
			
			//otherwise carry out the transaction
			else
			{					
				//compute value and deposit it
				double totalValue = blockCount * GriefPrevention.instance.config_economy_claimBlocksSellValue;					
				economy.depositPlayer(player.getName(), totalValue);
				
				//subtract blocks
				playerData.accruedClaimBlocks -= blockCount;
				this.dataStore.savePlayerData(player.getName(), playerData);
				
				//inform player
				GriefPrevention.sendMessage(player, TextMode.Success, Messages.BlockSaleConfirmation, String.valueOf(totalValue), String.valueOf(playerData.getRemainingClaimBlocks()));
			}
			
			return true;
		}		
		
		//adminclaims
		else if(cmd.getName().equalsIgnoreCase("adminclaims") && player != null)
		{
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			playerData.shovelMode = ShovelMode.Admin;
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.AdminClaimsMode);
			
			return true;
		}
		
		//basicclaims
		else if(cmd.getName().equalsIgnoreCase("basicclaims") && player != null)
		{
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			playerData.shovelMode = ShovelMode.Basic;
			playerData.claimSubdividing = null;
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.BasicClaimsMode);
			
			return true;
		}
		
		//subdivideclaims
		else if(cmd.getName().equalsIgnoreCase("subdivideclaims") && player != null)
		{
			PlayerData playerData = this.dataStore.getPlayerData(player.getName());
			playerData.shovelMode = ShovelMode.Subdivide;
			playerData.claimSubdividing = null;
			GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SubdivisionMode);
			GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SubdivisionDemo);
			
			return true;
		}
		
		//deleteclaim
		else if(cmd.getName().equalsIgnoreCase("deleteclaim") && player != null)
		{
			//determine which claim the player is standing in
			Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
			
			if(claim == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.DeleteClaimMissing);
			}
			
			else 
			{
				//deleting an admin claim additionally requires the adminclaims permission
				if(!claim.isAdminClaim() || player.hasPermission("griefprevention.adminclaims"))
				{
					PlayerData playerData = this.dataStore.getPlayerData(player.getName());
					if(claim.children.size() > 0 && !playerData.warnedAboutMajorDeletion)
					{
						GriefPrevention.sendMessage(player, TextMode.Warn, Messages.DeletionSubdivisionWarning);
						playerData.warnedAboutMajorDeletion = true;
					}
					else
					{
						claim.removeSurfaceFluids(null);
						this.dataStore.deleteClaim(claim);
						
						//if in a creative mode world, /restorenature the claim
						if(GriefPrevention.instance.config_claims_autoRestoreUnclaimedCreativeLand && GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner()))
						{
							GriefPrevention.instance.restoreClaim(claim, 0);
						}
						
						GriefPrevention.sendMessage(player, TextMode.Success, Messages.DeleteSuccess);
						GriefPrevention.AddLogEntry(player.getName() + " deleted " + claim.getOwnerName() + "'s claim at " + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
						
						//revert any current visualization
						Visualization.Revert(player);
						
						playerData.warnedAboutMajorDeletion = false;
					}
				}
				else
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.CantDeleteAdminClaim);
				}
			}

			return true;
		}
		
		else if(cmd.getName().equalsIgnoreCase("claimexplosions") && player != null)
		{
			//determine which claim the player is standing in
			Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
			
			if(claim == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.DeleteClaimMissing);
			}
			
			else
			{
				String noBuildReason = claim.allowBuild(player);
				if(noBuildReason != null)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
					return true;
				}
				
				if(claim.areExplosivesAllowed)
				{
					claim.areExplosivesAllowed = false;
					GriefPrevention.sendMessage(player, TextMode.Success, Messages.ExplosivesDisabled);
				}
				else
				{
					claim.areExplosivesAllowed = true;
					GriefPrevention.sendMessage(player, TextMode.Success, Messages.ExplosivesEnabled);
				}
			}

			return true;
		}
		
		//deleteallclaims <player>
		else if(cmd.getName().equalsIgnoreCase("deleteallclaims"))
		{
			//requires exactly one parameter, the other player's name
			if(args.length != 1) return false;
			
			//try to find that player
			OfflinePlayer otherPlayer = this.resolvePlayer(args[0]);
			if(otherPlayer == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
				return true;
			}
			
			//delete all that player's claims
			this.dataStore.deleteClaimsForPlayer(otherPlayer.getName(), true);
			
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.DeleteAllSuccess, otherPlayer.getName());
			if(player != null)
			{
				GriefPrevention.AddLogEntry(player.getName() + " deleted all claims belonging to " + otherPlayer.getName() + ".");
			
				//revert any current visualization
				Visualization.Revert(player);
			}
			
			return true;
		}
		
		//claimslist or claimslist <player>
		else if(cmd.getName().equalsIgnoreCase("claimslist"))
		{
			//at most one parameter
			if(args.length > 1) return false;
			
			//player whose claims will be listed
			OfflinePlayer otherPlayer;
			
			//if another player isn't specified, assume current player
			if(args.length < 1)
			{
				if(player != null)
					otherPlayer = player;
				else
					return false;
			}
			
			//otherwise if no permission to delve into another player's claims data
			else if(player != null && !player.hasPermission("griefprevention.deleteclaims"))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.ClaimsListNoPermission);
				return true;
			}
						
			//otherwise try to find the specified player
			else
			{
				otherPlayer = this.resolvePlayer(args[0]);
				if(otherPlayer == null)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
					return true;
				}
			}
			
			//load the target player's data
			PlayerData playerData = this.dataStore.getPlayerData(otherPlayer.getName());
			GriefPrevention.sendMessage(player, TextMode.Instr, " " + playerData.accruedClaimBlocks + "(+" + (playerData.bonusClaimBlocks + this.dataStore.getGroupBonusBlocks(otherPlayer.getName())) + ")=" + (playerData.accruedClaimBlocks + playerData.bonusClaimBlocks + this.dataStore.getGroupBonusBlocks(otherPlayer.getName())));
			for(int i = 0; i < playerData.claims.size(); i++)
			{
				Claim claim = playerData.claims.get(i);
				GriefPrevention.sendMessage(player, TextMode.Instr, "  (-" + claim.getArea() + ") " + getfriendlyLocationString(claim.getLesserBoundaryCorner()));
			}
			
			if(playerData.claims.size() > 0)
				GriefPrevention.sendMessage(player, TextMode.Instr, "   =" + playerData.getRemainingClaimBlocks());
			
			//drop the data we just loaded, if the player isn't online
			if(!otherPlayer.isOnline())
				this.dataStore.clearCachedPlayerData(otherPlayer.getName());
			
			return true;
		}
		
		//deathblow <player> [recipientPlayer]
		else if(cmd.getName().equalsIgnoreCase("deathblow"))
		{
			//requires at least one parameter, the target player's name
			if(args.length < 1) return false;
			
			//try to find that player
			Player targetPlayer = this.getServer().getPlayer(args[0]);
			if(targetPlayer == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
				return true;
			}
			
			//try to find the recipient player, if specified
			Player recipientPlayer = null;
			if(args.length > 1)
			{
				recipientPlayer = this.getServer().getPlayer(args[1]);
				if(recipientPlayer == null)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
					return true;
				}
			}
			
			//if giving inventory to another player, teleport the target player to that receiving player
			if(recipientPlayer != null)
			{
				targetPlayer.teleport(recipientPlayer);
			}
			
			//otherwise, plan to "pop" the player in place
			else
			{
				//if in a normal world, shoot him up to the sky first, so his items will fall on the surface.
				if(targetPlayer.getWorld().getEnvironment() == Environment.NORMAL)
				{
					Location location = targetPlayer.getLocation();
					location.setY(location.getWorld().getMaxHeight());
					targetPlayer.teleport(location);
				}
			}
			 
			//kill target player
			targetPlayer.setHealth(0);
			
			//log entry
			if(player != null)
			{
				GriefPrevention.AddLogEntry(player.getName() + " used /DeathBlow to kill " + targetPlayer.getName() + ".");
			}
			else
			{
				GriefPrevention.AddLogEntry("Killed " + targetPlayer.getName() + ".");
			}
			
			return true;
		}
		
		//deletealladminclaims
		else if(cmd.getName().equalsIgnoreCase("deletealladminclaims"))
		{
			if(!player.hasPermission("griefprevention.deleteclaims"))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoDeletePermission);
				return true;
			}
			
			//delete all admin claims
			this.dataStore.deleteClaimsForPlayer("", true);  //empty string for owner name indicates an administrative claim
			
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.AllAdminDeleted);
			if(player != null)
			{
				GriefPrevention.AddLogEntry(player.getName() + " deleted all administrative claims.");
			
				//revert any current visualization
				Visualization.Revert(player);
			}
			
			return true;
		}
		
		//adjustbonusclaimblocks <player> <amount> or [<permission>] amount
		else if(cmd.getName().equalsIgnoreCase("adjustbonusclaimblocks"))
		{
			//requires exactly two parameters, the other player or group's name and the adjustment
			if(args.length != 2) return false;
			
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
			
			//if granting blocks to all players with a specific permission
			if(args[0].startsWith("[") && args[0].endsWith("]"))
			{
				String permissionIdentifier = args[0].substring(1, args[0].length() - 1);
				int newTotal = this.dataStore.adjustGroupBonusBlocks(permissionIdentifier, adjustment);
				
				GriefPrevention.sendMessage(player, TextMode.Success, Messages.AdjustGroupBlocksSuccess, permissionIdentifier, String.valueOf(adjustment), String.valueOf(newTotal));
				if(player != null) GriefPrevention.AddLogEntry(player.getName() + " adjusted " + permissionIdentifier + "'s bonus claim blocks by " + adjustment + ".");
				
				return true;
			}
			
			//otherwise, find the specified player
			OfflinePlayer targetPlayer = this.resolvePlayer(args[0]);
			if(targetPlayer == null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
				return true;
			}
			
			//give blocks to player
			PlayerData playerData = this.dataStore.getPlayerData(targetPlayer.getName());
			playerData.bonusClaimBlocks += adjustment;
			this.dataStore.savePlayerData(targetPlayer.getName(), playerData);
			
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.AdjustBlocksSuccess, targetPlayer.getName(), String.valueOf(adjustment), String.valueOf(playerData.bonusClaimBlocks));
			if(player != null) GriefPrevention.AddLogEntry(player.getName() + " adjusted " + targetPlayer.getName() + "'s bonus claim blocks by " + adjustment + ".");
			
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
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotTrappedHere);				
				return true;
			}
			
			//if the player is in the nether or end, he's screwed (there's no way to programmatically find a safe place for him)
			if(player.getWorld().getEnvironment() != Environment.NORMAL)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrappedWontWorkHere);				
				return true;
			}
			
			//if the player is in an administrative claim, he should contact an admin
			if(claim.isAdminClaim())
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrappedWontWorkHere);
				return true;
			}
			
			//check cooldown
			long lastTrappedUsage = playerData.lastTrappedUsage.getTime();
			long nextTrappedUsage = lastTrappedUsage + 1000 * 60 * 60 * this.config_claims_trappedCooldownHours; 
			long now = Calendar.getInstance().getTimeInMillis();
			if(now < nextTrappedUsage)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrappedOnCooldown, String.valueOf(this.config_claims_trappedCooldownHours), String.valueOf((nextTrappedUsage - now) / (1000 * 60) + 1));
				return true;
			}
			
			//send instructions
			GriefPrevention.sendMessage(player, TextMode.Instr, Messages.RescuePending);
			
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
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NonSiegeWorld);
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
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.AlreadySieging);
				return true;
			}
			
			//can't start a siege when you're protected from pvp combat
			if(attackerData.pvpImmune)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.CantFightWhileImmune);
				return true;
			}
			
			//if a player name was specified, use that
			Player defender = null;
			if(args.length >= 1)
			{
				defender = this.getServer().getPlayer(args[0]);
				if(defender == null)
				{
					GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
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
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.AlreadyUnderSiegePlayer);
				return true;
			}
			
			//victim must not be pvp immune
			if(defenderData.pvpImmune)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoSiegeDefenseless);
				return true;
			}
			
			Claim defenderClaim = this.dataStore.getClaimAt(defender.getLocation(), false, null);
			
			//defender must have some level of permission there to be protected
			if(defenderClaim == null || defenderClaim.allowAccess(defender) != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotSiegableThere);
				return true;
			}									
			
			//attacker must be close to the claim he wants to siege
			if(!defenderClaim.isNear(attacker.getLocation(), 25))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeTooFarAway);
				return true;
			}
			
			//claim can't be under siege already
			if(defenderClaim.siegeData != null)
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.AlreadyUnderSiegeArea);
				return true;
			}
			
			//can't siege admin claims
			if(defenderClaim.isAdminClaim())
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoSiegeAdminClaim);
				return true;
			}
			
			//can't be on cooldown
			if(dataStore.onCooldown(attacker, defender, defenderClaim))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeOnCooldown);
				return true;
			}
			
			//start the siege
			dataStore.startSiege(attacker, defender, defenderClaim);			

			//confirmation message for attacker, warning message for defender
			GriefPrevention.sendMessage(defender, TextMode.Warn, Messages.SiegeAlert, attacker.getName());
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.SiegeConfirmed, defender.getName());			
		}
		
		return false; 
	}
	
	public static String getfriendlyLocationString(Location location) 
	{
		return location.getWorld().getName() + "(" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + ")";
	}

	private boolean abandonClaimHandler(Player player, boolean deleteTopLevelClaim) 
	{
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		
		//which claim is being abandoned?
		Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
		if(claim == null)
		{
			GriefPrevention.sendMessage(player, TextMode.Instr, Messages.AbandonClaimMissing);
		}
		
		//verify ownership
		else if(claim.allowEdit(player) != null)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotYourClaim);
		}
		
		//don't allow abandon of creative mode claims
		else if(!GriefPrevention.instance.config_claims_allowUnclaimInCreative && this.creativeRulesApply(player.getLocation()))
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoCreativeUnClaim);
		}
		
		//warn if has children and we're not explicitly deleting a top level claim
		else if(claim.children.size() > 0 && !deleteTopLevelClaim)
		{
			GriefPrevention.sendMessage(player, TextMode.Instr, Messages.DeleteTopLevelClaim);
			return true;
		}
		
		//if the claim has lots of surface water or some surface lava, warn the player it will be cleaned up
		else if(!playerData.warnedAboutMajorDeletion && claim.hasSurfaceFluids())
		{			
			GriefPrevention.sendMessage(player, TextMode.Warn, Messages.ConfirmFluidRemoval);
			playerData.warnedAboutMajorDeletion = true;
		}
		
		else
		{
			//delete it
			claim.removeSurfaceFluids(null);
			this.dataStore.deleteClaim(claim);
			
			//if in a creative mode world, restore the claim area
			if(GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner()))
			{
				GriefPrevention.AddLogEntry(player.getName() + " abandoned a claim @ " + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
				GriefPrevention.sendMessage(player, TextMode.Warn, Messages.UnclaimCleanupWarning);
				GriefPrevention.instance.restoreClaim(claim, 20L * 60 * 2);
			}
			
			//tell the player how many claim blocks he has left
			int remainingBlocks = playerData.getRemainingClaimBlocks();
			GriefPrevention.sendMessage(player, TextMode.Success, Messages.AbandonSuccess, String.valueOf(remainingBlocks));
			
			//revert any current visualization
			Visualization.Revert(player);
			
			playerData.warnedAboutMajorDeletion = false;
		}
		
		return true;
		
	}

	//helper method keeps the trust commands consistent and eliminates duplicate code
	private void handleTrustCommand(Player player, ClaimPermission permissionLevel, String recipientName) 
	{
		//determine which claim the player is standing in
		Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*ignore height*/, null);
		
		//validate player or group argument
		String permission = null;
		OfflinePlayer otherPlayer = null;
		if(recipientName.startsWith("[") && recipientName.endsWith("]"))
		{
			permission = recipientName.substring(1, recipientName.length() - 1);
			if(permission == null || permission.isEmpty())
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.InvalidPermissionID);
				return;
			}
		}
		
		else if(recipientName.contains("."))
		{
			permission = recipientName;
		}
		
		else
		{		
			otherPlayer = this.resolvePlayer(recipientName);
			if(otherPlayer == null && !recipientName.equals("public") && !recipientName.equals("all"))
			{
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerNotFound);
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
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoPermissionTrust, claim.getOwnerName());
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
				GriefPrevention.sendMessage(player, TextMode.Err, Messages.CantGrantThatPermission);
				return;
			}
			
			targetClaims.add(claim);
		}
		
		//if we didn't determine which claims to modify, tell the player to be specific
		if(targetClaims.size() == 0)
		{
			GriefPrevention.sendMessage(player, TextMode.Err, Messages.GrantPermissionNoClaim);
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
		if(recipientName.equals("public")) recipientName = this.dataStore.getMessage(Messages.CollectivePublic);
		String permissionDescription;
		if(permissionLevel == null)
		{
			permissionDescription = this.dataStore.getMessage(Messages.PermissionsPermission);
		}
		else if(permissionLevel == ClaimPermission.Build)
		{
			permissionDescription = this.dataStore.getMessage(Messages.BuildPermission);
		}		
		else if(permissionLevel == ClaimPermission.Access)
		{
			permissionDescription = this.dataStore.getMessage(Messages.AccessPermission);
		}
		else //ClaimPermission.Inventory
		{
			permissionDescription = this.dataStore.getMessage(Messages.ContainersPermission);
		}
		
		String location;
		if(claim == null)
		{
			location = this.dataStore.getMessage(Messages.LocationAllClaims);
		}
		else
		{
			location = this.dataStore.getMessage(Messages.LocationCurrentClaim);
		}
		
		GriefPrevention.sendMessage(player, TextMode.Success, Messages.GrantPermissionConfirmation, recipientName, permissionDescription, location);
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
		//save data for any online players
		Player [] players = this.getServer().getOnlinePlayers();
		for(int i = 0; i < players.length; i++)
		{
			Player player = players[i];
			String playerName = player.getName();
			PlayerData playerData = this.dataStore.getPlayerData(playerName);
			this.dataStore.savePlayerData(playerName, playerData);
		}
		
		this.dataStore.close();
		
		AddLogEntry("GriefPrevention disabled.");
	}
	
	//called when a player spawns, applies protection for that player if necessary
	public void checkPvpProtectionNeeded(Player player)
	{
		//if pvp is disabled, do nothing
		if(!this.config_pvp_enabledWorlds.contains(player.getWorld())) return;
		
		//if player is in creative mode, do nothing
		if(player.getGameMode() == GameMode.CREATIVE) return;
		
		//if anti spawn camping feature is not enabled, do nothing
		if(!this.config_pvp_protectFreshSpawns) return;
		
		//if the player has the damage any player permission enabled, do nothing
		if(player.hasPermission("griefprevention.nopvpimmunity")) return;
		
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
		GriefPrevention.sendMessage(player, TextMode.Success, Messages.PvPImmunityStart);
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
			TreeCleanupTask cleanupTask = new TreeCleanupTask(block, rootBlock, treeBlocks, rootBlock.getData());

			//20L ~ 1 second, so 2 mins = 120 seconds ~ 2400L 
			GriefPrevention.instance.getServer().getScheduler().scheduleSyncDelayedTask(GriefPrevention.instance, cleanupTask, 2400L);
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
	static void sendMessage(Player player, ChatColor color, Messages messageID, String... args)
	{
		sendMessage(player, color, messageID, 0, args);
	}
	
	//sends a color-coded message to a player
	static void sendMessage(Player player, ChatColor color, Messages messageID, long delayInTicks, String... args)
	{
		String message = GriefPrevention.instance.dataStore.getMessage(messageID, args);
		sendMessage(player, color, message, delayInTicks);
	}
	
	//sends a color-coded message to a player
	static void sendMessage(Player player, ChatColor color, String message)
	{
		if(player == null)
		{
			GriefPrevention.AddLogEntry(color + message);
		}
		else
		{
			player.sendMessage(color + message);
		}
	}
	
	static void sendMessage(Player player, ChatColor color, String message, long delayInTicks)
	{
		SendPlayerMessageTask task = new SendPlayerMessageTask(player, color, message);
		if(delayInTicks > 0)
		{
			GriefPrevention.instance.getServer().getScheduler().runTaskLater(GriefPrevention.instance, task, delayInTicks);
		}
		else
		{
			task.run();
		}
	}
	
	//determines whether creative anti-grief rules apply at a location
	boolean creativeRulesApply(Location location)
	{
		return this.config_claims_enabledCreativeWorlds.contains(location.getWorld());
	}
	
	public String allowBuild(Player player, Location location)
	{
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		Claim claim = this.dataStore.getClaimAt(location, false, playerData.lastClaim);
		
		//exception: administrators in ignore claims mode and special player accounts created by server mods
		if(playerData.ignoreClaims || GriefPrevention.instance.config_mods_ignoreClaimsAccounts.contains(player.getName())) return null;
		
		//wilderness rules
		if(claim == null)
		{
			//no building in the wilderness in creative mode
			if(this.creativeRulesApply(location))
			{
				String reason = this.dataStore.getMessage(Messages.NoBuildOutsideClaims) + "  " + this.dataStore.getMessage(Messages.CreativeBasicsDemoAdvertisement);
				if(player.hasPermission("griefprevention.ignoreclaims"))
					reason += "  " + this.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
				return reason;
			}
			
			//no building in survival wilderness when that is configured
			else if(this.config_claims_noBuildOutsideClaims && this.config_claims_enabledWorlds.contains(location.getWorld()))
			{
				return this.dataStore.getMessage(Messages.NoBuildOutsideClaims) + "  " + this.dataStore.getMessage(Messages.SurvivalBasicsDemoAdvertisement);
			}
			
			else
			{
				//but it's fine in survival mode
				return null;
			}			
		}
		
		//if not in the wilderness, then apply claim rules (permissions, etc)
		else
		{
			//cache the claim for later reference
			playerData.lastClaim = claim;
			return claim.allowBuild(player);
		}
	}
	
	public String allowBreak(Player player, Location location)
	{
		PlayerData playerData = this.dataStore.getPlayerData(player.getName());
		Claim claim = this.dataStore.getClaimAt(location, false, playerData.lastClaim);
		
		//exception: administrators in ignore claims mode, and special player accounts created by server mods
		if(playerData.ignoreClaims || GriefPrevention.instance.config_mods_ignoreClaimsAccounts.contains(player.getName())) return null;
		
		//wilderness rules
		if(claim == null)
		{
			//no building in the wilderness in creative mode
			if(this.creativeRulesApply(location))
			{
				String reason = this.dataStore.getMessage(Messages.NoBuildOutsideClaims) + "  " + this.dataStore.getMessage(Messages.CreativeBasicsDemoAdvertisement);
				if(player.hasPermission("griefprevention.ignoreclaims"))
					reason += "  " + this.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
				return reason;
			}
			
			else if(this.config_claims_noBuildOutsideClaims && this.config_claims_enabledWorlds.contains(location.getWorld()))
			{
				return this.dataStore.getMessage(Messages.NoBuildOutsideClaims) + "  " + this.dataStore.getMessage(Messages.SurvivalBasicsDemoAdvertisement);
			}
			
			//but it's fine in survival mode
			else
			{
				return null;
			}
		}
		else
		{
			//cache the claim for later reference
			playerData.lastClaim = claim;
		
			//if not in the wilderness, then apply claim rules (permissions, etc)
			return claim.allowBreak(player, location.getBlock().getType());
		}
	}

	//restores nature in multiple chunks, as described by a claim instance
	//this restores all chunks which have ANY number of claim blocks from this claim in them
	//if the claim is still active (in the data store), then the claimed blocks will not be changed (only the area bordering the claim)
	public void restoreClaim(Claim claim, long delayInTicks)
	{
		//admin claims aren't automatically cleaned up when deleted or abandoned
		if(claim.isAdminClaim()) return;
		
		//it's too expensive to do this for huge claims
		if(claim.getArea() > 10000) return;
		
		Chunk lesserChunk = claim.getLesserBoundaryCorner().getChunk();
		Chunk greaterChunk = claim.getGreaterBoundaryCorner().getChunk();
		
		for(int x = lesserChunk.getX(); x <= greaterChunk.getX(); x++)
			for(int z = lesserChunk.getZ(); z <= greaterChunk.getZ(); z++)
			{
				Chunk chunk = lesserChunk.getWorld().getChunkAt(x, z);
				this.restoreChunk(chunk, this.getSeaLevel(chunk.getWorld()) - 15, false, delayInTicks, null);
			}
	}
	
	public void restoreChunk(Chunk chunk, int miny, boolean aggressiveMode, long delayInTicks, Player playerReceivingVisualization)
	{
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
		Location lesserBoundaryCorner = chunk.getBlock(0,  0, 0).getLocation();
		Location greaterBoundaryCorner = chunk.getBlock(15, 0, 15).getLocation();
		
		//create task
		//when done processing, this task will create a main thread task to actually update the world with processing results
		RestoreNatureProcessingTask task = new RestoreNatureProcessingTask(snapshots, miny, chunk.getWorld().getEnvironment(), lesserBoundaryCorner.getBlock().getBiome(), lesserBoundaryCorner, greaterBoundaryCorner, this.getSeaLevel(chunk.getWorld()), aggressiveMode, GriefPrevention.instance.creativeRulesApply(lesserBoundaryCorner), playerReceivingVisualization);
		GriefPrevention.instance.getServer().getScheduler().runTaskLaterAsynchronously(GriefPrevention.instance, task, delayInTicks);
	}
	
	private void parseMaterialListFromConfig(List<String> stringsToParse, MaterialCollection materialCollection)
	{
		materialCollection.clear();
		
		//for each string in the list
		for(int i = 0; i < stringsToParse.size(); i++)
		{
			//try to parse the string value into a material info
			MaterialInfo materialInfo = MaterialInfo.fromString(stringsToParse.get(i));
			
			//null value returned indicates an error parsing the string from the config file
			if(materialInfo == null)
			{
				//show error in log
				GriefPrevention.AddLogEntry("ERROR: Unable to read a material entry from the config file.  Please update your config.yml.");
				
				//update string, which will go out to config file to help user find the error entry
				if(!stringsToParse.get(i).contains("can't"))
				{
					stringsToParse.set(i, stringsToParse.get(i) + "     <-- can't understand this entry, see BukkitDev documentation");
				}
			}
			
			//otherwise store the valid entry in config data
			else
			{
				materialCollection.Add(materialInfo);
			}
		}		
	}
	
	public int getSeaLevel(World world)
	{
		Integer overrideValue = this.config_seaLevelOverride.get(world.getName());
		if(overrideValue == null || overrideValue == -1)
		{
			return world.getSeaLevel();
		}
		else
		{
			return overrideValue;
		}		
	}
}