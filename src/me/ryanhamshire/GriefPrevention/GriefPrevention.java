/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) Ryan Hamshire
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package me.ryanhamshire.GriefPrevention;

import static org.spongepowered.api.command.CommandMessageFormatting.SPACE_TEXT;
import static org.spongepowered.api.command.args.GenericArguments.firstParsing;
import static org.spongepowered.api.command.args.GenericArguments.integer;
import static org.spongepowered.api.command.args.GenericArguments.literal;
import static org.spongepowered.api.command.args.GenericArguments.onlyOne;
import static org.spongepowered.api.command.args.GenericArguments.optional;
import static org.spongepowered.api.command.args.GenericArguments.player;
import static org.spongepowered.api.command.args.GenericArguments.playerOrSource;
import static org.spongepowered.api.command.args.GenericArguments.string;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import me.ryanhamshire.GriefPrevention.DataStore.NoTransferException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.spongepowered.api.Game;
import org.spongepowered.api.GameRegistry;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandPermissionException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.EventManager;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.TextBuilder;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import net.milkbowl.vault.economy.Economy;

//import net.milkbowl.vault.economy.Economy;

@Plugin(id = "GriefPrevention", name = "GriefPrevention", version = "12.7.1")
public class GriefPrevention {

    // for convenience, a reference to the instance of this plugin
    public static GriefPrevention instance;

    @Inject public Game game;
    @Inject public EventManager eventManager;
    @Inject public GameRegistry gameRegistry;

    // for logging to the console and log file
    private static Logger log = Logger.getLogger("Minecraft");

    // this handles data storage, like player and region data
    public DataStore dataStore;

    // this tracks item stacks expected to drop which will need protection
    ArrayList<PendingItemProtection> pendingItemWatchList = new ArrayList<PendingItemProtection>();

    // log entry manager for GP's custom log files
    CustomLogger customLogger;

    // configuration variables, loaded/saved from a config.yml

    // claim mode for each world
    public ConcurrentHashMap<World, ClaimsMode> config_claims_worldModes;

    // whether containers and crafting blocks are protectable
    public boolean config_claims_preventTheft;

    // whether claimed animals may be injured by players without permission
    public boolean config_claims_protectCreatures;

    // whether open flint+steel flames should be protected - optional because it's expensive
    public boolean config_claims_protectFires;

    // whether horses on a claim should be protected by that claim's rules
    public boolean config_claims_protectHorses;

    // whether buttons and switches are protectable
    public boolean config_claims_preventButtonsSwitches;

    // whether wooden doors should be locked by default (require /accesstrust)
    public boolean config_claims_lockWoodenDoors;

    // whether trap doors should be locked by default (require /accesstrust)
    public boolean config_claims_lockTrapDoors;

    // whether fence gates should be locked by default (require /accesstrust)
    public boolean config_claims_lockFenceGates;

    // whether teleporting into a claim with a pearl requires access trust
    public boolean config_claims_enderPearlsRequireAccessTrust;

    // maximum number of claims per player
    public int config_claims_maxClaimsPerPlayer;

    // whether claim creations requires WG build permission in creation area
    public boolean config_claims_respectWorldGuard;

    // whether nether portals require permission to generate. defaults to off for performance reasons
    public boolean config_claims_portalsRequirePermission;

    // whether trading with a claimed villager requires permission
    public boolean config_claims_villagerTradingRequiresTrust;

    // the number of claim blocks a new player starts with
    public int config_claims_initialBlocks;

    // the portion of claim blocks returned to a player when a claim is abandoned
    public double config_claims_abandonReturnRatio;

    // how many additional blocks players get each hour of play (can be zero)
    public int config_claims_blocksAccruedPerHour;

    // the limit on accrued blocks (over time). doesn't limit purchased or admin-gifted blocks
    public int config_claims_maxAccruedBlocks;

    // limit on how deep claims can go
    public int config_claims_maxDepth;

    // how many days of inactivity before a player loses his claims
    public int config_claims_expirationDays;

    // how big automatic new player claims (when they place a chest) should be. 0 to disable
    public int config_claims_automaticClaimsForNewPlayersRadius;

    // how far below the shoveled block a new claim will reach
    public int config_claims_claimsExtendIntoGroundDistance;

    // minimum width for non-admin claims
    public int config_claims_minWidth;

    // minimum area for non-admin claims
    public int config_claims_minArea;

    // number of days of inactivity before an automatic chest claim will be deleted
    public int config_claims_chestClaimExpirationDays;

    // number of days of inactivity before an unused (nothingbuild) claim will be deleted
    public int config_claims_unusedClaimExpirationDays;

    // whether survival claims will be automatically restored to nature when auto-deleted
    public boolean config_claims_survivalAutoNatureRestoration;

    // which material will beused to investigate claims with a right click
    public ItemType config_claims_investigationTool;

    // which material will be used to create/resize claims with a right click
    public ItemType config_claims_modificationTool;

    // the list of slashcommands requiring access trust when in a claim
    public ArrayList<String> config_claims_commandsRequiringAccessTrust;

    // whether to give new players a book with land claim help in it
    public boolean config_claims_supplyPlayerManual;

    // whether or not /siege is enabled on this server
    public ArrayList<World> config_siege_enabledWorlds;

    // which blocks will be breakable in siege mode
    public ArrayList<BlockType> config_siege_blocks;

    // whether or not to monitor for spam
    public boolean config_spam_enabled;

    // how long players must wait between logins. combats login spam.
    public int config_spam_loginCooldownSeconds;

    // the list of slash commands monitored for spam
    public ArrayList<String> config_spam_monitorSlashCommands;

    // whether or not to ban spammers automatically
    public boolean config_spam_banOffenders;

    // message to show an automatically banned player
    public String config_spam_banMessage;

    // message to show a player who is close to spam level
    public String config_spam_warningMessage;

    // IP addresses which will not be censored
    public String config_spam_allowedIpAddresses;

    // cooldown period for death messages (per player) in seconds
    public int config_spam_deathMessageCooldownSeconds;

    // list of worlds where pvp anti-grief rules apply, according to the config file
    HashMap<World, Boolean> config_pvp_specifiedWorlds;

    // whether to make newly spawned players immune until they pick up an item
    public boolean config_pvp_protectFreshSpawns;

    // whether to kill players who log out during PvP combat
    public boolean config_pvp_punishLogout;

    // how long combat is considered to continue after the most recent damage
    public int config_pvp_combatTimeoutSeconds;

    // whether a player can drop items during combat to hide them
    public boolean config_pvp_allowCombatItemDrop;

    // list of commands which may not be used during pvp combat
    public ArrayList<String> config_pvp_blockedCommands;

    // whether players may fight in player-owned land claims
    public boolean config_pvp_noCombatInPlayerLandClaims;

    // whether players may fight in admin-owned land claims
    public boolean config_pvp_noCombatInAdminLandClaims;

    // whether players may fight insubdivisions of admin-owned land claims
    public boolean config_pvp_noCombatInAdminSubdivisions;

    // whether players' dropped on death items are protected in pvp worlds
    public boolean config_lockDeathDropsInPvpWorlds;

    // whether players' dropped on death items are protected in non-pvp worlds
    public boolean config_lockDeathDropsInNonPvpWorlds;

    // cost to purchase a claim block. set to zero to disable purchase.
    public double config_economy_claimBlocksPurchaseCost;

    // return on a sold claim block. set to zero to disable sale.
    public double config_economy_claimBlocksSellValue;

    // whether explosions may destroy claimed blocks
    public boolean config_blockClaimExplosions;

    // whether creeper explosions near or above the surface destroy blocks
    public boolean config_blockSurfaceCreeperExplosions;

    // whether non-creeper explosions near or above the surface destroy blocks
    public boolean config_blockSurfaceOtherExplosions;

    // whether players can build trees on platforms in the sky
    public boolean config_blockSkyTrees;

    // whether fire spreads outside of claims
    public boolean config_fireSpreads;

    // whether fire destroys blocks outside of claims
    public boolean config_fireDestroys;

    // whether whispered messages will broadcast to administrators in game
    public boolean config_whisperNotifications;

    // whether sign content will broadcast to administrators in game
    public boolean config_signNotifications;

    // list of whisper commands to eavesdrop on
    public ArrayList<String> config_eavesdrop_whisperCommands;

    // whether to ban accounts which very likely owned by a banned player
    public boolean config_smartBan;

    // whether or not endermen may move blocks around
    public boolean config_endermenMoveBlocks;

    // whether silverfish may break blocks
    public boolean config_silverfishBreakBlocks;

    // whether or not non-player entities may trample crops
    public boolean config_creaturesTrampleCrops;

    // whether or not hard-mode zombies may break down wooden doors
    public boolean config_zombiesBreakDoors;

    // how many players can share an IP address
    public int config_ipLimit;

    // list of block IDs which should require /accesstrust for player interaction
    public List<BlockType> config_mods_accessTrustIds;

    // list of block IDs which should require /containertrust for player interaction
    public List<BlockType> config_mods_containerTrustIds;

    // list of player names which ALWAYS ignore claims
    public List<String> config_mods_ignoreClaimsAccounts;

    // list of block IDs which can be destroyed by explosions, even in claimed areas
    public List<BlockType> config_mods_explodableIds;

    // override for sea level, because bukkit doesn't report the right value for all situations
    public HashMap<String, Integer> config_seaLevelOverride;

    // whether trees should be prevented from growing into a claim from outside
    public boolean config_limitTreeGrowth;

    // whether pistons are limited to only move blocks located within the piston's land claim
    public boolean config_pistonsInClaimsOnly;

    // custom log settings
    public int config_logs_daysToKeep;
    public boolean config_logs_socialEnabled;
    public boolean config_logs_suspiciousEnabled;
    public boolean config_logs_adminEnabled;
    public boolean config_logs_debugEnabled;

    private String databaseUrl;
    private String databaseUserName;
    private String databasePassword;

    // reference to the economy plugin, if economy integration is enabled
    // public static Economy economy = null;

    // how far away to search from a tree trunk for its branch blocks
    public static final int TREE_RADIUS = 5;

    // how long to wait before deciding a player is staying online or staying
    // offline, for notication messages
    public static final int NOTIFICATION_SECONDS = 20;

    // adds a server log entry
    public static synchronized void AddLogEntry(String entry, CustomLogEntryTypes customLogType, boolean excludeFromServerLogs) {
        if (customLogType != null && GriefPrevention.instance.customLogger != null) {
            GriefPrevention.instance.customLogger.AddEntry(entry, customLogType);
        }
        if (!excludeFromServerLogs)
            log.info("GriefPrevention: " + entry);
    }

    public static synchronized void AddLogEntry(String entry, CustomLogEntryTypes customLogType) {
        AddLogEntry(entry, customLogType, false);
    }

    public static synchronized void AddLogEntry(String entry) {
        AddLogEntry(entry, CustomLogEntryTypes.Debug);
    }

    // initializes well... everything
    @Listener
    public void onPostInit(GamePostInitializationEvent event) {
        instance = this;

        DataStore.resetTextObjects();

        AddLogEntry("Grief Prevention boot start.");

        this.loadConfig();

        this.customLogger = new CustomLogger();

        AddLogEntry("Finished loading configuration.");

        // when datastore initializes, it loads player and claim data, and posts
        // some stats to the log
        if (this.databaseUrl.length() > 0) {
            try {
                DatabaseDataStore databaseStore = new DatabaseDataStore(this.databaseUrl, this.databaseUserName, this.databasePassword);

                if (FlatFileDataStore.hasData()) {
                    GriefPrevention.AddLogEntry("There appears to be some data on the hard drive.  Migrating those data to the database...");
                    FlatFileDataStore flatFileStore = new FlatFileDataStore();
                    this.dataStore = flatFileStore;
                    flatFileStore.migrateData(databaseStore);
                    GriefPrevention.AddLogEntry("Data migration process complete.  Reloading data from the database...");
                    databaseStore.close();
                    databaseStore = new DatabaseDataStore(this.databaseUrl, this.databaseUserName, this.databasePassword);
                }

                this.dataStore = databaseStore;
            } catch (Exception e) {
                GriefPrevention.AddLogEntry(
                        "Because there was a problem with the database, GriefPrevention will not function properly.  Either update the database config settings resolve the issue, or delete those lines from your config.yml so that GriefPrevention can use the file system to store data.");
                e.printStackTrace();
                return;
            }
        }

        // if not using the database because it's not configured or because
        // there was a problem, use the file system to store data
        // this is the preferred method, as it's simpler than the database
        // scenario
        if (this.dataStore == null) {
            File oldclaimdata = new File(event.getGame().getSavesDirectory().toFile(), "ClaimData");
            if (oldclaimdata.exists()) {
                if (!FlatFileDataStore.hasData()) {
                    File claimdata = new File("plugins" + File.separator + "GriefPreventionData" + File.separator + "ClaimData");
                    oldclaimdata.renameTo(claimdata);
                    File oldplayerdata = new File(event.getGame().getSavesDirectory().toFile(), "PlayerData");
                    File playerdata = new File("plugins" + File.separator + "GriefPreventionData" + File.separator + "PlayerData");
                    oldplayerdata.renameTo(playerdata);
                }
            }
            try {
                this.dataStore = new FlatFileDataStore();
            } catch (Exception e) {
                GriefPrevention.AddLogEntry("Unable to initialize the file system data store.  Details:");
                GriefPrevention.AddLogEntry(e.getMessage());
                e.printStackTrace();
            }
        }

        String dataMode = (this.dataStore instanceof FlatFileDataStore) ? "(File Mode)" : "(Database Mode)";
        AddLogEntry("Finished loading data " + dataMode + ".");

        // unless claim block accrual is disabled, start the recurring per 10
        // minute event to give claim blocks to online players
        if (this.config_claims_blocksAccruedPerHour > 0) {
            DeliverClaimBlocksTask task = new DeliverClaimBlocksTask(null);
            GriefPrevention.instance.game.getScheduler().createTaskBuilder().interval(10, TimeUnit.MINUTES).execute(task)
                    .submit(GriefPrevention.instance);
        }

        // start the recurring cleanup event for entities in creative worlds
        EntityCleanupTask task = new EntityCleanupTask(0);
        GriefPrevention.instance.game.getScheduler().createTaskBuilder().delay(2, TimeUnit.MINUTES).execute(task).submit(GriefPrevention.instance);

        // start recurring cleanup scan for unused claims belonging to inactive
        // players
        CleanupUnusedClaimsTask task2 = new CleanupUnusedClaimsTask();
        GriefPrevention.instance.game.getScheduler().createTaskBuilder().interval(5, TimeUnit.MINUTES).execute(task2).submit(GriefPrevention.instance);

        // register for events
        registerCommands();

        //if economy is enabled
        /*if(this.config_economy_claimBlocksPurchaseCost > 0 || this.config_economy_claimBlocksSellValue > 0) {
            //try to load Vault
            GriefPrevention.AddLogEntry("GriefPrevention requires Vault for economy integration.");
            GriefPrevention.AddLogEntry("Attempting to load Vault...");
            RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            GriefPrevention.AddLogEntry("Vault loaded successfully!");
            
            //ask Vault to hook into an economy plugin
            GriefPrevention.AddLogEntry("Looking for a Vault-compatible economy plugin...");
            if (economyProvider != null)  {
                GriefPrevention.economy = economyProvider.getProvider();
                
                //on success, display success message
                if(GriefPrevention.economy != null) {
                    GriefPrevention.AddLogEntry("Hooked into economy: " + GriefPrevention.economy.getName() + ".");  
                    GriefPrevention.AddLogEntry("Ready to buy/sell claim blocks!");
                }
                
                //otherwise error message
                else {
                    GriefPrevention.AddLogEntry("ERROR: Vault was unable to find a supported economy plugin.  Either install a Vault-compatible economy plugin, or set both of the economy config variables to zero.");
                }               
            }
            
            //another error case
            else {
                GriefPrevention.AddLogEntry("ERROR: Vault was unable to find a supported economy plugin.  Either install a Vault-compatible economy plugin, or set both of the economy config variables to zero.");
            }
        }*/

        // cache offline players
        // User [] offlinePlayers = this.getServer().getOfflinePlayers();
        // CacheOfflinePlayerNamesThread namesThread = new
        // CacheOfflinePlayerNamesThread(offlinePlayers,
        // this.playerNameToIDMap);
        // namesThread.setPriority(Thread.MIN_PRIORITY);
        // namesThread.start();

        // load ignore lists for any already-online players
        Collection<Player> players = (Collection<Player>) GriefPrevention.instance.game.getServer().getOnlinePlayers();
        for (Player player : players) {
            new IgnoreLoaderThread(player.getUniqueId(), this.dataStore.getPlayerData(player.getUniqueId()).ignoredPlayers).start();
        }

        game.getEventManager().registerListeners(this, new BlockEventHandler(dataStore));
        game.getEventManager().registerListeners(this, new PlayerEventHandler(dataStore, this));
        game.getEventManager().registerListeners(this, new EntityEventHandler(dataStore));
        AddLogEntry("Boot finished.");
    }

    @SuppressWarnings("serial")
    private void loadConfig() {
        try {
            new File(DataStore.configFilePath).getParentFile().mkdirs();
            // load the config if it existss=
            // FileConfiguration config =
            // YamlConfiguration.loadConfiguration(new
            // File(DataStore.configFilePath));
            // FileConfiguration outConfig = new YamlConfiguration();

            HoconConfigurationLoader configurationLoader = HoconConfigurationLoader.builder().setFile(new File(DataStore.configFilePath)).build();
            CommentedConfigurationNode mainNode = configurationLoader.load();//ConfigurationOptions.defaults().setShouldCopyDefaults(true));

            Collection<World> worlds = game.getServer().getWorlds();

            // decide claim mode for each world
            this.config_claims_worldModes = new ConcurrentHashMap<World, ClaimsMode>();
            for (World world : worlds) {
                // is it specified in the config file?
                String configSetting = mainNode.getNode("GriefPrevention", "Claims", "Mode", world.getUniqueId().toString()).getString();
                mainNode.getNode("GriefPrevention", "Claims", "Mode", world.getUniqueId().toString()).setValue(configSetting);
                if (configSetting != null) {
                    ClaimsMode claimsMode = this.configStringToClaimsMode(configSetting);
                    if (claimsMode != null) {
                        this.config_claims_worldModes.put(world, claimsMode);
                        continue;
                    } else {
                        GriefPrevention
                                .AddLogEntry("Error: Invalid claim mode \"" + configSetting + "\".  Options are Survival, Creative, and Disabled.");
                        this.config_claims_worldModes.put(world, ClaimsMode.Creative);
                    }
                }

                // does the world's name indicate its purpose?
                else if (world.getName().toLowerCase().contains("survival")) {
                    this.config_claims_worldModes.put(world, ClaimsMode.Survival);
                } else if (world.getName().toLowerCase().contains("creative")) {
                    this.config_claims_worldModes.put(world, ClaimsMode.Creative);
                }

                // decide a default based on server type and world type
                else if (GriefPrevention.instance.game.getServer().getDefaultWorld().get().getGameMode() == GameModes.CREATIVE) {
                    this.config_claims_worldModes.put(world, ClaimsMode.Creative);
                } else if (world.getDimension().getType().equals(DimensionTypes.OVERWORLD)) {
                    this.config_claims_worldModes.put(world, ClaimsMode.Survival);
                } else {
                    this.config_claims_worldModes.put(world, ClaimsMode.Disabled);
                }

            }

            // pvp worlds list
            this.config_pvp_specifiedWorlds = new HashMap<World, Boolean>();
            for (World world : worlds) {
                boolean pvpWorld = mainNode.getNode("GriefPrevention", "PvP", "RulesEnabledInWorld", world.getUniqueId().toString()).getBoolean();
                mainNode.getNode("GriefPrevention", "PvP", "RulesEnabledInWorld", world.getUniqueId().toString()).setValue(pvpWorld);
                // TODO
//                boolean pvpWorld = config.getBoolean("GriefPrevention.PvP.RulesEnabledInWorld." + world.getName(), world.getPVP());
                this.config_pvp_specifiedWorlds.put(world, pvpWorld);
            }

            // sea level
            this.config_seaLevelOverride = new HashMap<String, Integer>();
            for (World world : worlds) {
                int seaLevelOverride = mainNode.getNode("GriefPrevention", "SeaLevelOverrides", world.getUniqueId().toString()).getInt(-1);
                mainNode.getNode("GriefPrevention", "SeaLevelOverrides", world.getUniqueId().toString()).setValue(seaLevelOverride);
                this.config_seaLevelOverride.put(world.getName(), seaLevelOverride);
            }

            this.config_claims_preventTheft = mainNode.getNode("GriefPrevention", "Claims", "PreventTheft").getBoolean(true);
            mainNode.getNode("GriefPrevention", "Claims", "PreventTheft").setValue(config_claims_preventTheft);
            this.config_claims_protectCreatures = mainNode.getNode("GriefPrevention", "Claims", "ProtectCreatures").getBoolean(true);
            mainNode.getNode("GriefPrevention", "Claims", "ProtectCreatures").setValue(config_claims_protectCreatures);
            this.config_claims_protectFires = mainNode.getNode("GriefPrevention", "Claims", "ProtectFires").getBoolean(false);
            mainNode.getNode("GriefPrevention", "Claims", "ProtectFires").setValue(config_claims_protectFires);
            this.config_claims_protectHorses = mainNode.getNode("GriefPrevention", "Claims", "ProtectHorses").getBoolean(true);
            mainNode.getNode("GriefPrevention", "Claims", "ProtectHorses").setValue(config_claims_protectHorses);
            this.config_claims_preventButtonsSwitches = mainNode.getNode("GriefPrevention", "Claims", "PreventButtonsSwitches").getBoolean(true);
            mainNode.getNode("GriefPrevention", "Claims", "PreventButtonsSwitches").setValue(config_claims_preventButtonsSwitches);
            this.config_claims_lockWoodenDoors = mainNode.getNode("GriefPrevention", "Claims", "LockWoodenDoors").getBoolean(false);
            mainNode.getNode("GriefPrevention", "Claims", "LockWoodenDoors").setValue(config_claims_lockWoodenDoors);
            this.config_claims_lockTrapDoors = mainNode.getNode("GriefPrevention", "Claims", "LockTrapDoors").getBoolean(false);
            mainNode.getNode("GriefPrevention", "Claims", "LockTrapDoors").setValue(config_claims_lockTrapDoors);
            this.config_claims_lockFenceGates = mainNode.getNode("GriefPrevention", "Claims", "LockFenceGates").getBoolean(true);
            mainNode.getNode("GriefPrevention", "Claims", "LockFenceGates").setValue(config_claims_lockFenceGates);
            this.config_claims_enderPearlsRequireAccessTrust = mainNode.getNode("GriefPrevention", "Claims", "EnderPearlsRequireAccessTrust").getBoolean(true);
            mainNode.getNode("GriefPrevention", "Claims", "EnderPearlsRequireAccessTrust").setValue(config_claims_enderPearlsRequireAccessTrust);
            this.config_claims_initialBlocks = mainNode.getNode("GriefPrevention", "Claims", "InitialBlocks").getInt(100);
            mainNode.getNode("GriefPrevention", "Claims", "InitialBlocks").setValue(config_claims_initialBlocks);
            this.config_claims_blocksAccruedPerHour = mainNode.getNode("GriefPrevention", "Claims", "BlocksAccruedPerHour").getInt(100);
            mainNode.getNode("GriefPrevention", "Claims", "BlocksAccruedPerHour").setValue(config_claims_blocksAccruedPerHour);
            this.config_claims_maxAccruedBlocks = mainNode.getNode("GriefPrevention", "Claims", "MaxAccruedBlocks").getInt(80000);
            mainNode.getNode("GriefPrevention", "Claims", "MaxAccruedBlocks").setValue(config_claims_maxAccruedBlocks);
            this.config_claims_abandonReturnRatio = mainNode.getNode("GriefPrevention", "Claims", "AbandonReturnRatio").getDouble(1);
            mainNode.getNode("GriefPrevention", "Claims", "AbandonReturnRatio").setValue(config_claims_abandonReturnRatio);
            this.config_claims_automaticClaimsForNewPlayersRadius = mainNode.getNode("GriefPrevention", "Claims", "AutomaticNewPlayerClaimsRadius").getInt(4);
            mainNode.getNode("GriefPrevention", "Claims", "AutomaticNewPlayerClaimsRadius").setValue(config_claims_automaticClaimsForNewPlayersRadius);
            this.config_claims_claimsExtendIntoGroundDistance = Math.abs(mainNode.getNode("GriefPrevention", "Claims", "ExtendIntoGroundDistance").getInt(5));
            mainNode.getNode("GriefPrevention", "Claims", "ExtendIntoGroundDistance").setValue(config_claims_claimsExtendIntoGroundDistance);
            this.config_claims_minWidth = mainNode.getNode("GriefPrevention", "Claims", "MinimumWidth").getInt(5);
            mainNode.getNode("GriefPrevention", "Claims", "MinimumWidth").setValue(config_claims_minWidth);
            this.config_claims_minArea = mainNode.getNode("GriefPrevention", "Claims", "MinimumArea").getInt(100);
            mainNode.getNode("GriefPrevention", "Claims", "MinimumArea").setValue(config_claims_minArea);
            this.config_claims_maxDepth = mainNode.getNode("GriefPrevention", "Claims", "MaximumDepth").getInt(0);
            mainNode.getNode("GriefPrevention", "Claims", "MaximumDepth").setValue(config_claims_maxDepth);
            this.config_claims_chestClaimExpirationDays = mainNode.getNode("GriefPrevention", "Claims", "Expiration", "ChestClaimDays").getInt(7);
            mainNode.getNode("GriefPrevention", "Claims", "Expiration", "ChestClaimDays").setValue(config_claims_chestClaimExpirationDays);
            this.config_claims_unusedClaimExpirationDays = mainNode.getNode("GriefPrevention", "Claims", "Expiration", "UnusedClaimDays").getInt(14);
            mainNode.getNode("GriefPrevention", "Claims", "Expiration", "UnusedClaimDays").setValue(config_claims_unusedClaimExpirationDays);
            this.config_claims_expirationDays = mainNode.getNode("GriefPrevention", "Claims", "Expiration", "AllClaimDays").getInt(0);
            mainNode.getNode("GriefPrevention", "Claims", "Expiration", "AllClaimDays").setValue(config_claims_expirationDays);
            this.config_claims_survivalAutoNatureRestoration = mainNode.getNode("GriefPrevention", "Claims", "Expiration", "AutomaticNatureRestoration", "SurvivalWorlds").getBoolean(false);
            mainNode.getNode("GriefPrevention", "Claims", "Expiration", "AutomaticNatureRestoration", "SurvivalWorlds").setValue(config_claims_survivalAutoNatureRestoration);
            this.config_claims_maxClaimsPerPlayer = mainNode.getNode("GriefPrevention", "Claims", "MaximumNumberOfClaimsPerPlayer").getInt(0);
            mainNode.getNode("GriefPrevention", "Claims", "MaximumNumberOfClaimsPerPlayer").setValue(config_claims_maxClaimsPerPlayer);
            this.config_claims_respectWorldGuard = mainNode.getNode("GriefPrevention", "Claims", "CreationRequiresWorldGuardBuildPermission").getBoolean(true);
            mainNode.getNode("GriefPrevention", "Claims", "CreationRequiresWorldGuardBuildPermission").setValue(config_claims_respectWorldGuard);
            this.config_claims_portalsRequirePermission = mainNode.getNode("GriefPrevention", "Claims", "PortalGenerationRequiresPermission").getBoolean(false);
            mainNode.getNode("GriefPrevention", "Claims", "PortalGenerationRequiresPermission").setValue(config_claims_portalsRequirePermission);
            this.config_claims_villagerTradingRequiresTrust = mainNode.getNode("GriefPrevention", "Claims", "VillagerTradingRequiresPermission").getBoolean(true);
            mainNode.getNode("GriefPrevention", "Claims", "VillagerTradingRequiresPermission").setValue(config_claims_villagerTradingRequiresTrust);
            String accessTrustSlashCommands = mainNode.getNode("GriefPrevention", "Claims", "CommandsRequiringAccessTrust").getString("/sethome");
            mainNode.getNode("GriefPrevention", "Claims", "CommandsRequiringAccessTrust").setValue(accessTrustSlashCommands);
            this.config_claims_supplyPlayerManual = mainNode.getNode("GriefPrevention", "Claims", "DeliverManuals").getBoolean(true);
            mainNode.getNode("GriefPrevention", "Claims", "DeliverManuals").setValue(config_claims_supplyPlayerManual);

            this.config_spam_enabled = mainNode.getNode("GriefPrevention", "Spam", "Enabled").getBoolean(true);
            mainNode.getNode("GriefPrevention", "Spam", "Enabled").setValue(config_spam_enabled);
            this.config_spam_loginCooldownSeconds = mainNode.getNode("GriefPrevention", "Spam", "LoginCooldownSeconds").getInt(60);
            mainNode.getNode("GriefPrevention", "Spam", "LoginCooldownSeconds").setValue(config_spam_loginCooldownSeconds);
            this.config_spam_warningMessage = mainNode.getNode("GriefPrevention", "Spam", "WarningMessage").getString("Please reduce your noise level.  Spammers will be banned.");
            mainNode.getNode("GriefPrevention", "Spam", "WarningMessage").setValue(config_spam_warningMessage);
            this.config_spam_allowedIpAddresses = mainNode.getNode("GriefPrevention", "Spam", "AllowedIpAddresses").getString("1.2.3.4; 5.6.7.8");
            mainNode.getNode("GriefPrevention", "Spam", "AllowedIpAddresses").setValue(config_spam_allowedIpAddresses);
            this.config_spam_banOffenders = mainNode.getNode("GriefPrevention", "Spam", "BanOffenders").getBoolean(true);
            mainNode.getNode("GriefPrevention", "Spam", "BanOffenders").setValue(config_spam_banOffenders);
            this.config_spam_banMessage = mainNode.getNode("GriefPrevention", "Spam", "BanMessage").getString("Banned for spam.");
            mainNode.getNode("GriefPrevention", "Spam", "BanMessage").setValue(config_spam_banMessage);
            String slashCommandsToMonitor = mainNode.getNode("GriefPrevention", "Spam", "MonitorSlashCommands").getString("/me;/tell;/global;/local;/w;/msg;/r;/t");
            mainNode.getNode("GriefPrevention", "Spam", "MonitorSlashCommands").setValue(slashCommandsToMonitor);
            this.config_spam_deathMessageCooldownSeconds = mainNode.getNode("GriefPrevention", "Spam", "DeathMessageCooldownSeconds").getInt(60);
            mainNode.getNode("GriefPrevention", "Spam", "DeathMessageCooldownSeconds").setValue(config_spam_deathMessageCooldownSeconds);

            this.config_pvp_protectFreshSpawns = mainNode.getNode("GriefPrevention", "PvP", "ProtectFreshSpawns").getBoolean(true);
            mainNode.getNode("GriefPrevention", "PvP", "ProtectFreshSpawns").setValue(config_pvp_protectFreshSpawns);
            this.config_pvp_punishLogout = mainNode.getNode("GriefPrevention", "PvP", "PunishLogout").getBoolean(true);
            mainNode.getNode("GriefPrevention", "PvP", "PunishLogout").setValue(config_pvp_punishLogout);
            this.config_pvp_combatTimeoutSeconds = mainNode.getNode("GriefPrevention", "PvP", "CombatTimeoutSeconds").getInt(15);
            mainNode.getNode("GriefPrevention", "PvP", "CombatTimeoutSeconds").setValue(config_pvp_combatTimeoutSeconds);
            this.config_pvp_allowCombatItemDrop = mainNode.getNode("GriefPrevention", "PvP", "AllowCombatItemDrop").getBoolean(false);
            mainNode.getNode("GriefPrevention", "PvP", "AllowCombatItemDrop").setValue(config_pvp_allowCombatItemDrop);
            String bannedPvPCommandsList = mainNode.getNode("GriefPrevention", "PvP", "BlockedSlashCommands").getString("/home;/vanish;/spawn;/tpa");
            mainNode.getNode("GriefPrevention", "PvP", "BlockedSlashCommands").setValue(bannedPvPCommandsList);

            this.config_economy_claimBlocksPurchaseCost = mainNode.getNode("GriefPrevention", "Economy", "ClaimBlocksPurchaseCost").getDouble(0);
            mainNode.getNode("GriefPrevention", "Economy", "ClaimBlocksPurchaseCost").setValue(config_economy_claimBlocksPurchaseCost);
            this.config_economy_claimBlocksSellValue = mainNode.getNode("GriefPrevention", "Economy", "ClaimBlocksSellValue").getDouble(0);
            mainNode.getNode("GriefPrevention", "Economy", "ClaimBlocksSellValue").setValue(config_economy_claimBlocksSellValue);

            this.config_lockDeathDropsInPvpWorlds = mainNode.getNode("GriefPrevention", "ProtectItemsDroppedOnDeath", "PvPWorlds").getBoolean(false);
            mainNode.getNode("GriefPrevention", "ProtectItemsDroppedOnDeath", "PvPWorlds").setValue(config_lockDeathDropsInPvpWorlds);
            this.config_lockDeathDropsInNonPvpWorlds = mainNode.getNode("GriefPrevention", "ProtectItemsDroppedOnDeath", "NonPvPWorlds").getBoolean(true);
            mainNode.getNode("GriefPrevention", "ProtectItemsDroppedOnDeath", "NonPvPWorlds").setValue(config_lockDeathDropsInNonPvpWorlds);

            this.config_blockClaimExplosions = mainNode.getNode("GriefPrevention", "BlockLandClaimExplosions").getBoolean(true);
            mainNode.getNode("GriefPrevention", "BlockLandClaimExplosions").setValue(config_blockClaimExplosions);
            this.config_blockSurfaceCreeperExplosions = mainNode.getNode("GriefPrevention", "BlockSurfaceCreeperExplosions").getBoolean(true);
            mainNode.getNode("GriefPrevention", "BlockSurfaceCreeperExplosions").setValue(config_blockSurfaceCreeperExplosions);
            this.config_blockSurfaceOtherExplosions = mainNode.getNode("GriefPrevention", "BlockSurfaceOtherExplosions").getBoolean(true);
            mainNode.getNode("GriefPrevention", "BlockSurfaceOtherExplosions").setValue(config_blockSurfaceOtherExplosions);
            this.config_blockSkyTrees = mainNode.getNode("GriefPrevention", "LimitSkyTrees").getBoolean(true);
            mainNode.getNode("GriefPrevention", "LimitSkyTrees").setValue(config_blockSkyTrees);
            this.config_limitTreeGrowth = mainNode.getNode("GriefPrevention", "LimitTreeGrowth").getBoolean(false);
            mainNode.getNode("GriefPrevention", "LimitTreeGrowth").setValue(config_limitTreeGrowth);
            this.config_pistonsInClaimsOnly = mainNode.getNode("GriefPrevention", "LimitPistonsToLandClaims").getBoolean(true);
            mainNode.getNode("GriefPrevention", "LimitPistonsToLandClaims").setValue(config_pistonsInClaimsOnly);

            this.config_fireSpreads = mainNode.getNode("GriefPrevention", "FireSpreads").getBoolean(false);
            mainNode.getNode("GriefPrevention", "FireSpreads").setValue(config_fireSpreads);
            this.config_fireDestroys = mainNode.getNode("GriefPrevention", "FireDestroys").getBoolean(false);
            mainNode.getNode("GriefPrevention", "FireDestroys").setValue(config_fireDestroys);

            this.config_whisperNotifications = mainNode.getNode("GriefPrevention", "AdminsGetWhispers").getBoolean(true);
            mainNode.getNode("GriefPrevention", "AdminsGetWhispers").setValue(config_whisperNotifications);
            this.config_signNotifications = mainNode.getNode("GriefPrevention", "AdminsGetSignNotifications").getBoolean(true);
            mainNode.getNode("GriefPrevention", "AdminsGetSignNotifications").setValue(config_signNotifications);
            String whisperCommandsToMonitor = mainNode.getNode("GriefPrevention", "WhisperCommands").getString("/tell;/pm;/r;/w;/whisper;/t;/msg");
            mainNode.getNode("GriefPrevention", "WhisperCommands").setValue(whisperCommandsToMonitor);

            this.config_smartBan = mainNode.getNode("GriefPrevention", "SmartBan").getBoolean(true);
            mainNode.getNode("GriefPrevention", "SmartBan").setValue(config_smartBan);
            this.config_ipLimit = mainNode.getNode("GriefPrevention", "MaxPlayersPerIpAddress").getInt(3);
            mainNode.getNode("GriefPrevention", "MaxPlayersPerIpAddress").setValue(config_ipLimit);

            this.config_endermenMoveBlocks = mainNode.getNode("GriefPrevention", "EndermenMoveBlocks").getBoolean(false);
            mainNode.getNode("GriefPrevention", "EndermenMoveBlocks").setValue(config_endermenMoveBlocks);
            this.config_silverfishBreakBlocks = mainNode.getNode("GriefPrevention", "SilverfishBreakBlocks").getBoolean(false);
            mainNode.getNode("GriefPrevention", "SilverfishBreakBlocks").setValue(config_silverfishBreakBlocks);
            this.config_creaturesTrampleCrops = mainNode.getNode("GriefPrevention", "creaturesTrampleCrops").getBoolean(false);
            mainNode.getNode("GriefPrevention", "creaturesTrampleCrops").setValue(config_creaturesTrampleCrops);
            this.config_zombiesBreakDoors = mainNode.getNode("GriefPrevention", "HardModeZombiesBreakDoors").getBoolean(false);
            mainNode.getNode("GriefPrevention", "HardModeZombiesBreakDoors").setValue(config_zombiesBreakDoors);

            this.config_mods_ignoreClaimsAccounts =
                    mainNode.getNode("GriefPrevention", "Mods", "PlayersIgnoringAllClaims").getList(new TypeToken<String>() {});
            mainNode.getNode("GriefPrevention", "Mods", "PlayersIgnoringAllClaims").setValue(config_mods_ignoreClaimsAccounts);

            if (this.config_mods_ignoreClaimsAccounts == null)
                this.config_mods_ignoreClaimsAccounts = new ArrayList<>();

            this.config_mods_accessTrustIds = new ArrayList<BlockType>();
            List<String> accessTrustStrings =
                    mainNode.getNode("GriefPrevention", "Mods", "BlockIdsRequiringAccessTrust").getList(new TypeToken<String>() {
                    });
            mainNode.getNode("GriefPrevention", "Mods", "BlockIdsRequiringAccessTrust").setValue(accessTrustStrings);

            this.parseBlockIdListFromConfig(accessTrustStrings, this.config_mods_accessTrustIds);

            this.config_mods_containerTrustIds = new ArrayList<BlockType>();
            List<String> containerTrustStrings =
                    mainNode.getNode("GriefPrevention", "Mods", "BlockIdsRequiringContainerTrust").getList(new TypeToken<String>() {
                    });
            mainNode.getNode("GriefPrevention", "Mods", "BlockIdsRequiringContainerTrust").setValue(containerTrustStrings);

            // default values for container trust mod blocks
            // TODO
            /*if (containerTrustStrings == null || containerTrustStrings.size() == 0) {
                containerTrustStrings = Lists.newArrayList();
                containerTrustStrings.add(new MaterialInfo(99999, "Example - ID 99999, all data values.").toString());
            }*/

            // parse the strings from the config file
            this.parseBlockIdListFromConfig(containerTrustStrings, this.config_mods_containerTrustIds);

            this.config_mods_explodableIds = new ArrayList<BlockType>();
            List<String> explodableStrings = mainNode.getNode("GriefPrevention", "Mods", "BlockIdsExplodable").getList(new TypeToken<String>() {
            });
            mainNode.getNode("GriefPrevention", "Mods", "BlockIdsExplodable").setValue(explodableStrings);

            // parse the strings from the config file
            this.parseBlockIdListFromConfig(explodableStrings, this.config_mods_explodableIds);

            // default for claim investigation tool
            String investigationToolMaterialName = ItemTypes.STICK.getName();

            // get investigation tool from config
            investigationToolMaterialName =
                    mainNode.getNode("GriefPrevention", "Claims", "InvestigationTool").getString(investigationToolMaterialName);
            mainNode.getNode("GriefPrevention", "Claims", "InvestigationTool").setValue(investigationToolMaterialName);

            // validate investigation tool
            Optional<ItemType> investigationTool = game.getRegistry().getType(ItemType.class, investigationToolMaterialName);
            if (!investigationTool.isPresent()) {
                GriefPrevention.AddLogEntry(
                        "ERROR: Material " + investigationToolMaterialName + " not found.  Defaulting to the stick.  Please update your config.yml.");
                this.config_claims_investigationTool = ItemTypes.STICK;
            } else {
                this.config_claims_investigationTool = investigationTool.get();
            }

            // default for claim creation/modification tool
            String modificationToolMaterialName = ItemTypes.GOLDEN_SHOVEL.getId();

            // get modification tool from config
            modificationToolMaterialName = mainNode.getNode("GriefPrevention", "Claims", "ModificationTool").getString(modificationToolMaterialName);
            mainNode.getNode("GriefPrevention", "Claims", "ModificationTool").setValue(modificationToolMaterialName);

            // validate modification tool
            this.config_claims_modificationTool = null;
            Optional<ItemType> modificationTool = game.getRegistry().getType(ItemType.class, modificationToolMaterialName);
            if (!modificationTool.isPresent()) {
                GriefPrevention.AddLogEntry("ERROR: Material " + modificationToolMaterialName
                        + " not found.  Defaulting to the golden shovel.  Please update your config.yml.");
                this.config_claims_modificationTool = ItemTypes.GOLDEN_SHOVEL;
            } else {
                this.config_claims_modificationTool = modificationTool.get();
            }

            // default for siege worlds list
            ArrayList<String> defaultSiegeWorldNames = new ArrayList<>();

            // get siege world names from the config file
            List<String> siegeEnabledWorldNames = mainNode.getNode("GriefPrevention", "Siege", "Worlds").getList(new TypeToken<String>() {
            });
            if (siegeEnabledWorldNames == null || siegeEnabledWorldNames.size() == 0) {
                siegeEnabledWorldNames = defaultSiegeWorldNames;
            }
            mainNode.getNode("GriefPrevention", "Siege", "Worlds").setValue(siegeEnabledWorldNames);

            // validate that list
            this.config_siege_enabledWorlds = new ArrayList<World>();
            for (int i = 0; i < siegeEnabledWorldNames.size(); i++) {
                String worldName = siegeEnabledWorldNames.get(i);
                Optional<World> world = this.game.getServer().getWorld(UUID.fromString(worldName));
                if (!world.isPresent()) {
                    AddLogEntry("Error: Siege Configuration: There's no world uuid \"" + worldName + "\".  Please update your config.yml.");
                } else {
                    this.config_siege_enabledWorlds.add(world.get());
                }
            }

            // default siege blocks
            this.config_siege_blocks = new ArrayList<BlockType>();
            this.config_siege_blocks.add(BlockTypes.DIRT);
            this.config_siege_blocks.add(BlockTypes.GRASS);
            this.config_siege_blocks.add(BlockTypes.TALLGRASS);
            this.config_siege_blocks.add(BlockTypes.COBBLESTONE);
            this.config_siege_blocks.add(BlockTypes.GRAVEL);
            this.config_siege_blocks.add(BlockTypes.SAND);
            this.config_siege_blocks.add(BlockTypes.GLASS);
            this.config_siege_blocks.add(BlockTypes.GLASS_PANE);
            this.config_siege_blocks.add(BlockTypes.PLANKS);
            this.config_siege_blocks.add(BlockTypes.WOOL);
            this.config_siege_blocks.add(BlockTypes.SNOW);

            // build a default config entry
            ArrayList<String> defaultBreakableBlocksList = new ArrayList<String>();
            for (int i = 0; i < this.config_siege_blocks.size(); i++) {
                defaultBreakableBlocksList.add(this.config_siege_blocks.get(i).getId());
            }

            // try to load the list from the config file
            List<String> breakableBlocksList = mainNode.getNode("GriefPrevention", "Siege", "BreakableBlocks").getList(new TypeToken<String>() {
            });
            // if it fails, use default list instead
            if (breakableBlocksList == null || breakableBlocksList.size() == 0) {
                breakableBlocksList = defaultBreakableBlocksList;
            }
            mainNode.getNode("GriefPrevention", "Siege", "BreakableBlocks").setValue(breakableBlocksList);


            // parse the list of siege-breakable blocks
            this.config_siege_blocks = new ArrayList<BlockType>();
            for (int i = 0; i < breakableBlocksList.size(); i++) {
                String blockName = breakableBlocksList.get(i);
                Optional<BlockType> material = game.getRegistry().getType(BlockType.class, blockName);
                if (!material.isPresent()) {
                    GriefPrevention.AddLogEntry("Siege Configuration: Material not found: " + blockName + ".");
                } else {
                    this.config_siege_blocks.add(material.get());
                }
            }

            this.config_pvp_noCombatInPlayerLandClaims = mainNode.getNode("GriefPrevention", "PvP", "ProtectPlayersInLandClaims", "PlayerOwnedClaims")
                    .getBoolean(this.config_siege_enabledWorlds.size() == 0);
            mainNode.getNode("GriefPrevention", "PvP", "ProtectPlayersInLandClaims", "PlayerOwnedClaims").setValue(config_pvp_noCombatInPlayerLandClaims);
            this.config_pvp_noCombatInAdminLandClaims =
                    mainNode.getNode("GriefPrevention", "PvP", "ProtectPlayersInLandClaims", "AdministrativeClaims")
                            .getBoolean(this.config_siege_enabledWorlds.size() == 0);
            mainNode.getNode("GriefPrevention", "PvP", "ProtectPlayersInLandClaims", "AdministrativeClaims").setValue(config_pvp_noCombatInAdminLandClaims);
            this.config_pvp_noCombatInAdminSubdivisions =
                    mainNode.getNode("GriefPrevention", "PvP", "ProtectPlayersInLandClaims", "AdministrativeSubdivisions")
                            .getBoolean(this.config_siege_enabledWorlds.size() == 0);
            mainNode.getNode("GriefPrevention", "PvP", "ProtectPlayersInLandClaims", "AdministrativeSubdivisions").setValue(config_pvp_noCombatInAdminSubdivisions);

            // optional database settings
            this.databaseUrl = mainNode.getNode("GriefPrevention", "Database", "URL").getString("");
            mainNode.getNode("GriefPrevention", "Database", "URL").setValue(databaseUrl);
            this.databaseUserName = mainNode.getNode("GriefPrevention", "Database", "UserName").getString("");
            mainNode.getNode("GriefPrevention", "Database", "UserName").setValue(databaseUserName);
            this.databasePassword = mainNode.getNode("GriefPrevention", "Database", "Password").getString("");
            mainNode.getNode("GriefPrevention", "Database", "Password").setValue(databasePassword);

            // custom logger settings
            this.config_logs_daysToKeep = mainNode.getNode("GriefPrevention", "Abridged Logs", "Days To Keep").getInt(7);
            mainNode.getNode("GriefPrevention", "Abridged Logs", "Days To Keep").setValue(config_logs_daysToKeep);
            this.config_logs_socialEnabled =
                    mainNode.getNode("GriefPrevention", "Abridged Logs", "Included Entry Types", "Social Activity").getBoolean(true);
            mainNode.getNode("GriefPrevention", "Abridged Logs", "Included Entry Types", "Social Activity").setValue(config_logs_socialEnabled);
            this.config_logs_suspiciousEnabled =
                    mainNode.getNode("GriefPrevention", "Abridged Logs", "Included Entry Types", "Suspicious Activity").getBoolean(true);
            mainNode.getNode("GriefPrevention", "Abridged Logs", "Included Entry Types", "Suspicious Activity").setValue(config_logs_suspiciousEnabled);
            this.config_logs_adminEnabled =
                    mainNode.getNode("GriefPrevention", "Abridged Logs", "Included Entry Types", "Administrative Activity").getBoolean(false);
            mainNode.getNode("GriefPrevention", "Abridged Logs", "Included Entry Types", "Administrative Activity").setValue(config_logs_adminEnabled);
            this.config_logs_debugEnabled = mainNode.getNode("GriefPrevention", "Abridged Logs", "Included Entry Types", "Debug").getBoolean(false);
            mainNode.getNode("GriefPrevention", "Abridged Logs", "Included Entry Types", "Debug").setValue(config_logs_debugEnabled);

            // claims mode by world
            for (World world : this.config_claims_worldModes.keySet()) {
                mainNode.getNode("GriefPrevention", "Claims", "Mode", world.getUniqueId()).setValue(this.config_claims_worldModes.get(world).name());
            }

            try {
                configurationLoader.save(mainNode);
            } catch (IOException exception) {
                exception.printStackTrace();
                AddLogEntry("Unable to write to the configuration file at \"" + DataStore.configFilePath + "\"");
            }

            // try to parse the list of commands requiring access trust in land
            // claims
            this.config_claims_commandsRequiringAccessTrust = new ArrayList<String>();
            String[] commands = accessTrustSlashCommands.split(";");
            for (int i = 0; i < commands.length; i++) {
                if (!commands[i].isEmpty()) {
                    this.config_claims_commandsRequiringAccessTrust.add(commands[i].trim().toLowerCase());
                }
            }

            // try to parse the list of commands which should be monitored for
            // spam
            this.config_spam_monitorSlashCommands = new ArrayList<String>();
            commands = slashCommandsToMonitor.split(";");
            for (int i = 0; i < commands.length; i++) {
                this.config_spam_monitorSlashCommands.add(commands[i].trim());
            }

            // try to parse the list of commands which should be included in
            // eavesdropping
            this.config_eavesdrop_whisperCommands = new ArrayList<String>();
            commands = whisperCommandsToMonitor.split(";");
            for (int i = 0; i < commands.length; i++) {
                this.config_eavesdrop_whisperCommands.add(commands[i].trim());
            }

            // try to parse the list of commands which should be banned during
            // pvp
            // combat
            this.config_pvp_blockedCommands = new ArrayList<String>();
            commands = bannedPvPCommandsList.split(";");
            for (int i = 0; i < commands.length; i++) {
                this.config_pvp_blockedCommands.add(commands[i].trim());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ClaimsMode configStringToClaimsMode(String configSetting) {
        if (configSetting.equalsIgnoreCase("Survival")) {
            return ClaimsMode.Survival;
        } else if (configSetting.equalsIgnoreCase("Creative")) {
            return ClaimsMode.Creative;
        } else if (configSetting.equalsIgnoreCase("Disabled")) {
            return ClaimsMode.Disabled;
        } else if (configSetting.equalsIgnoreCase("SurvivalRequiringClaims")) {
            return ClaimsMode.SurvivalRequiringClaims;
        } else {
            return null;
        }
    }

    private Player checkPlayer(CommandSource source) throws CommandException {
        if (source instanceof Player) {
            return ((Player) source);
        } else {
            throw new CommandException(Texts.of("You must be a player to run this command!"));
        }
    }

    // handles slash commands
    @SuppressWarnings("unused")
    private void registerCommands() {
        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Deletes a claim"))
                .permission("griefprevention.claims")
                .executor((src, args) -> {
                    final Player player = checkPlayer(src);
                    return this.abandonClaimHandler(player, false);
                })
                .build(), "abandonclaim", "unclaim", "declaim", "removeclaim", "disclaim");
        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Deletes a claim and all its subdivisions"))
                .permission("griefprevention.claims")
                .executor((src, args) -> {
                    final Player player = checkPlayer(src);
                    return this.abandonClaimHandler(player, true);
                })
                .build(), "abandontoplevelclaim");
        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Toggles ignore claims mode"))
                .permission("griefprevention.ignoreclaims")
                .executor((src, args) -> {
                    final Player player = checkPlayer(src);
                    PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());

                    playerData.ignoreClaims = !playerData.ignoreClaims;

                    // toggle ignore claims mode on or off
                    if (!playerData.ignoreClaims) {
                        GriefPrevention.sendMessage(player, TextMode.Success, Messages.RespectingClaims);
                    } else {
                        GriefPrevention.sendMessage(player, TextMode.Success, Messages.IgnoringClaims);
                    }

                    return CommandResult.success();
                }).build(), "ignoreclaims", "ic");
        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Deletes ALL your claims"))
                .permission("griefprevention.claims")
                .executor((src, args) -> {
                    final Player player = checkPlayer(src);
                    // count claims
                    PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
                    int originalClaimCount = playerData.getClaims().size();

                    // check count
                    if (originalClaimCount == 0) {
                        throw new CommandException(getMessage(Messages.YouHaveNoClaims));
                    }

                    // adjust claim blocks
                    for (Claim claim : playerData.getClaims()) {
                        playerData.setAccruedClaimBlocks(
                                playerData.getAccruedClaimBlocks() - (int) Math.ceil((claim.getArea() * (1 - this.config_claims_abandonReturnRatio))));
                    }

                    // delete them
                    this.dataStore.deleteClaimsForPlayer(player.getUniqueId(), false);

                    // inform the player
                    int remainingBlocks = playerData.getRemainingClaimBlocks();
                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.SuccessfulAbandon, String.valueOf(remainingBlocks));

                    // revert any current visualization
                    Visualization.Revert(player);

                    return CommandResult.success();
                })
        .build(), "abandonallclaims");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Switches the shovel tool to restoration mode"))
                .permission("griefprevention.restorenature")
                .executor(((src, args) -> {
                    final Player player = checkPlayer(src);
                    // change shovel mode
                    PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
                    playerData.shovelMode = ShovelMode.RestoreNature;
                    GriefPrevention.sendMessage(player, TextMode.Instr, Messages.RestoreNatureActivate);
                    return CommandResult.success();
                }))
                .build(), "restorenature", "rn");
        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Switches the shovel tool to aggressive restoration mode"))
                .permission("griefprevention.restorenatureaggressive")
                .executor(((src, args) -> {
                    final Player player = checkPlayer(src);
                    // change shovel mode
                    PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
                    playerData.shovelMode = ShovelMode.RestoreNatureAggressive;
                    GriefPrevention.sendMessage(player, TextMode.Warn, Messages.RestoreNatureAggressiveActivate);
                    return CommandResult.success();
                }))
                .build(), "restorenatureaggressive", "rna");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Switches the shovel tool to fill mode"))
                .permission("griefprotection.restorenaturefill")
                .arguments(optional(integer(Texts.of("radius")), 2))
                .executor((src, args) -> {
                    final Player player = checkPlayer(src);
                    // change shovel mode
                    PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
                    playerData.shovelMode = ShovelMode.RestoreNatureFill;

                    // set radius based on arguments
                    playerData.fillRadius = args.<Integer>getOne("radius").get();
                    if (playerData.fillRadius < 0)
                        playerData.fillRadius = 2;

                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.FillModeActive, String.valueOf(playerData.fillRadius));
                    return CommandResult.success();
                })
                .build(), "restorenaturefill", "rnf");
        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Grants a player full access to your claim(s)"))
                .extendedDescription(Texts.of("Grants a player full access to your claim(s).\n"
                        + "See also /untrust, /containertrust, /accesstrust, and /permissiontrust."))
                .permission("griefprevention.claims")
                .arguments(string(Texts.of("subject")))
                .executor((src, args) -> {

                    // most trust commands use this helper method, it keeps them
                    // consistent
                    this.handleTrustCommand(checkPlayer(src), ClaimPermission.Build, args.<String>getOne("subject").get());
                    return CommandResult.success();
                })
                .build(), "trust", "tr");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Converts an administrative claim to a private claim"))
                .arguments(optional(player(Texts.of("target"), game)))
                .permission("griefprevention.transferclaim")
                .executor((src, args) -> {
                    final Player player = checkPlayer(src);
                    // which claim is the user in?
                    Claim claim = this.dataStore.getClaimAt(player.getLocation(), true, null);
                    if (claim == null) {
                        GriefPrevention.sendMessage(player, TextMode.Instr, Messages.TransferClaimMissing);
                        return CommandResult.empty();
                    }

                    // check additional permission for admin claims
                    if (claim.isAdminClaim() && !player.hasPermission("griefprevention.adminclaims")) {
                        throw new CommandException(getMessage(Messages.TransferClaimPermission));
                    }

                    UUID newOwnerID = null; // no argument = make an admin claim
                    String ownerName = "admin";

                    Optional<User> targetOpt = args.<User>getOne("target");
                    if (targetOpt.isPresent()) {
                        User targetPlayer = targetOpt.get();
                        newOwnerID = targetPlayer.getUniqueId();
                        ownerName = targetPlayer.getName();
                    }

                    // change ownerhsip
                    try {
                        this.dataStore.changeClaimOwner(claim, newOwnerID);
                    } catch (NoTransferException e) {
                        GriefPrevention.sendMessage(player, TextMode.Instr, Messages.TransferTopLevel);
                        return CommandResult.empty();
                    }

                    // confirm
                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.TransferSuccess);
                    GriefPrevention.AddLogEntry(player.getName() + " transferred a claim at "
                                    + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()) + " to " + ownerName + ".",
                            CustomLogEntryTypes.AdminActivity);

                    return CommandResult.success();

                })
        .build(), "transferclaim", "giveclaim");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Lists permissions for the claim you're standing in"))
                .permission("griefprevention.claimls")
                .executor((src, args) -> {
                    final Player player = checkPlayer(src);
                    Claim claim = this.dataStore.getClaimAt(player.getLocation(), true, null);

                    // if no claim here, error message
                    if (claim == null) {
                        throw new CommandException(getMessage(Messages.TrustListNoClaim));
                    }

                    // if no permission to manage permissions, error message
                    String errorMessage = claim.allowGrantPermission(player);
                    if (errorMessage != null) {
                        throw new CommandException(Texts.of(errorMessage));
                    }

                    // otherwise build a list of explicit permissions by permission
                    // level
                    // and send that to the player
                    ArrayList<String> builders = new ArrayList<>();
                    ArrayList<String> containers = new ArrayList<>();
                    ArrayList<String> accessors = new ArrayList<>();
                    ArrayList<String> managers = new ArrayList<>();
                    claim.getPermissions(builders, containers, accessors, managers);

                    GriefPrevention.sendMessage(player, TextMode.Info, Messages.TrustListHeader);

                    TextBuilder permissions = Texts.builder(">").color(TextColors.GOLD);

                    if (managers.size() > 0) {
                        for (int i = 0; i < managers.size(); i++)
                            permissions.append(SPACE_TEXT, Texts.of(this.trustEntryToPlayerName(managers.get(i))));
                    }

                    player.sendMessage(permissions.build());
                    permissions = Texts.builder(">").color(TextColors.YELLOW);

                    if (builders.size() > 0) {
                        for (int i = 0; i < builders.size(); i++)
                            permissions.append(SPACE_TEXT, Texts.of(this.trustEntryToPlayerName(builders.get(i))));
                    }

                    player.sendMessage(permissions.build());
                    permissions = Texts.builder(">").color(TextColors.GREEN);

                    if (containers.size() > 0) {
                        for (int i = 0; i < containers.size(); i++)
                            permissions.append(SPACE_TEXT, Texts.of(this.trustEntryToPlayerName(containers.get(i))));
                    }

                    player.sendMessage(permissions.build());
                    permissions = Texts.builder(">").color(TextColors.BLUE);

                    if (accessors.size() > 0) {
                        for (int i = 0; i < accessors.size(); i++)
                            permissions.append(SPACE_TEXT, Texts.of(this.trustEntryToPlayerName(accessors.get(i))));
                    }

                    player.sendMessage(permissions.build());

                    player.sendMessage(Texts.of(
                                    Texts.of(TextColors.GOLD, this.dataStore.getMessage(Messages.Manage)), SPACE_TEXT,
                                    Texts.of(TextColors.YELLOW, this.dataStore.getMessage(Messages.Build)), SPACE_TEXT,
                            Texts.of(TextColors.GREEN, this.dataStore.getMessage(Messages.Containers)), SPACE_TEXT,
                            Texts.of(TextColors.BLUE, this.dataStore.getMessage(Messages.Access))));

                    return CommandResult.success();

                })
                .build(), "trustlist");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Revokes a player's access to your claim(s)"))
                .permission("griefprevention.claims")
                .arguments(player(Texts.of("player"), game))
                .executor((src, args) -> {
                    final Player player = checkPlayer(src);
                    // determine which claim the player is standing in
                    Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*
                                                                                * ignore
                                                                                * height
                                                                                */, null);

                    String target = args.<String>getOne("player").get();

                    // bracket any permissions
                    if (target.contains(".") && !target.startsWith("[") && !target.endsWith("]")) {
                        target = "[" + target + "]";
                    }

                    // determine whether a single player or clearing permissions
                    // entirely
                    boolean clearPermissions = false;
                    User otherPlayer = null;
                    if (target.equals("all")) {
                        if (claim == null || claim.allowEdit(player) == null) {
                            clearPermissions = true;
                        } else {
                            throw new CommandException(getMessage(Messages.ClearPermsOwnerOnly));
                        }
                    }

                    else {
                        // validate player argument or group argument
                        if (!target.startsWith("[") || !target.endsWith("]")) {
                            otherPlayer = this.resolvePlayerByName(target).orElse(null);
                            if (!clearPermissions && otherPlayer == null && !target.equals("public")) {
                                throw new CommandException(getMessage(Messages.PlayerNotFound2));
                            }

                            // correct to proper casing
                            if (otherPlayer != null)
                                target = otherPlayer.getName();
                        }
                    }

                    // if no claim here, apply changes to all his claims
                    if (claim == null) {
                        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
                        for (int i = 0; i < playerData.getClaims().size(); i++) {
                            claim = playerData.getClaims().get(i);

                            // if untrusting "all" drop all permissions
                            if (clearPermissions) {
                                claim.clearPermissions();
                            }

                            // otherwise drop individual permissions
                            else {
                                String idToDrop = target;
                                if (otherPlayer != null) {
                                    idToDrop = otherPlayer.getUniqueId().toString();
                                }
                                claim.dropPermission(idToDrop);
                                claim.managers.remove(idToDrop);
                            }

                            // save changes
                            this.dataStore.saveClaim(claim);
                        }

                        // beautify for output
                        if (target.equals("public")) {
                            target = "the public";
                        }

                        // confirmation message
                        if (!clearPermissions) {
                            GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustIndividualAllClaims, target);
                        } else {
                            GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustEveryoneAllClaims);
                        }
                    }

                    // otherwise, apply changes to only this claim
                    else if (claim.allowGrantPermission(player) != null) {
                        throw new CommandException(getMessage(Messages.NoPermissionTrust, claim.getOwnerName()));
                    } else {
                        // if clearing all
                        if (clearPermissions) {
                            // requires owner
                            if (claim.allowEdit(player) != null) {
                                throw new CommandException(getMessage(Messages.UntrustAllOwnerOnly));
                            }

                            claim.clearPermissions();
                            GriefPrevention.sendMessage(player, TextMode.Success, Messages.ClearPermissionsOneClaim);
                        }

                        // otherwise individual permission drop
                        else {
                            String idToDrop = target;
                            if (otherPlayer != null) {
                                idToDrop = otherPlayer.getUniqueId().toString();
                            }
                            boolean targetIsManager = claim.managers.contains(idToDrop);
                            if (targetIsManager && claim.allowEdit(player) != null) // only
                            // claim
                            // owners
                            // can
                            // untrust
                            // managers
                            {
                                throw new CommandException(getMessage(Messages.ManagersDontUntrustManagers, claim.getOwnerName()));
                            } else {
                                claim.dropPermission(idToDrop);
                                claim.managers.remove(idToDrop);

                                // beautify for output
                                if (target.equals("public")) {
                                    target = "the public";
                                }

                                GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustIndividualSingleClaim, target);
                            }
                        }

                        // save changes
                        this.dataStore.saveClaim(claim);
                    }
                    return CommandResult.success();

                })
                .build(), "untrust", "ut");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Grants a player entry to your claim(s) and use of your bed"))
                .permission("griefprevention.claims")
                .arguments(string(Texts.of("target")))
                .executor((src, args) -> {
                    this.handleTrustCommand(checkPlayer(src), ClaimPermission.Access, args.<String>getOne("target").get());
                    return CommandResult.success();
                })
                .build(), "accesstrust", "at");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Grants a player access to your claim's containers, crops, animals, bed, buttons, and levers"))
                .permission("griefprevention.claims")
                .arguments(string(Texts.of("target")))
                .executor((src, args) -> {
                    this.handleTrustCommand(checkPlayer(src), ClaimPermission.Inventory, args.<String>getOne("target").get());
                    return CommandResult.success();
                })
                .build(), "containertrust", "ct");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Grants a player permission to grant their level of permission to others"))
                .permission("griefprevention.claims")
                .arguments(string(Texts.of("target")))
                .executor((src, args) -> {
                    this.handleTrustCommand(checkPlayer(src), null, args.<String>getOne("target").get());
                    return CommandResult.success();
                })
                .build(), "permissiontrust", "pt");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Purchases additional claim blocks with server money. Doesn't work on servers without a vault-compatible "
                        + "economy plugin"))
                .permission("griefprevention.buysellclaimblocks")
                .arguments(optional(integer(Texts.of("numberOfBlocks"))))
                .executor((src, args) -> {
                    // TODO: Implement economy
                    /*final Player player = checkPlayer(src);
                    // if economy is disabled, don't do anything
                    if (GriefPrevention.economy == null) {
                        GriefPrevention.sendMessage(player, TextMode.Err, Messages.BuySellNotConfigured);
                        return true;
                    }

                    // if purchase disabled, send error message
                    if (GriefPrevention.instance.config_economy_claimBlocksPurchaseCost == 0) {
                        GriefPrevention.sendMessage(player, TextMode.Err, Messages.OnlySellBlocks);
                        return true;
                    }

                    Optional<Integer> blockCountOpt = args.getOne("numberOfBlocks");

                    // if no parameter, just tell player cost per block and balance
                    if (!blockCountOpt.isPresent()) {
                        GriefPrevention.sendMessage(player, TextMode.Info, Messages.BlockPurchaseCost,
                                String.valueOf(GriefPrevention.instance.config_economy_claimBlocksPurchaseCost),
                                String.valueOf(GriefPrevention.economy.getBalance(player)));
                        return false;
                    } else {
                        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());

                        // try to parse number of blocks
                        int blockCount = blockCountOpt.get();

                        if (blockCount <= 0) {
                            throw new CommandException(Texts.of("Invalid block count of lte 0"));
                        }

                        // if the player can't afford his purchase, send error message
                        double balance = economy.getBalance(player);
                        double totalCost = blockCount * GriefPrevention.instance.config_economy_claimBlocksPurchaseCost;
                        if (totalCost > balance) {
                            GriefPrevention.sendMessage(player, TextMode.Err, Messages.InsufficientFunds, String.valueOf(totalCost),
                                    String.valueOf(balance));
                        }

                        // otherwise carry out transaction
                        else {
                            // withdraw cost
                            economy.withdrawPlayer(player, totalCost);

                            // add blocks
                            playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + blockCount);
                            this.dataStore.savePlayerData(player.getUniqueId(), playerData);

                            // inform player
                            GriefPrevention.sendMessage(player, TextMode.Success, Messages.PurchaseConfirmation, String.valueOf(totalCost),
                                    String.valueOf(playerData.getRemainingClaimBlocks()));
                        }

                    }*/
                    return CommandResult.success();
                })
                .build(), "buyclaimblocks", "buyclaim");
        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Sell your claim blocks for server money. Doesn't work on servers without a vault-compatible "
                        + "economy plugin"))
                .permission("griefprevention.buysellclaimblocks")
                .arguments(optional(integer(Texts.of("numberOfBlocks"))))
                .executor((src, args) -> {
                    // TODO: Implement economy
                    /*final Player player = checkPlayer(src);
                    // if economy is disabled, don't do anything
                    if (GriefPrevention.economy == null) {
                        GriefPrevention.sendMessage(player, TextMode.Err, Messages.BuySellNotConfigured);
                        return true;
                    }

                    // if disabled, error message
                    if (GriefPrevention.instance.config_economy_claimBlocksSellValue == 0) {
                        GriefPrevention.sendMessage(player, TextMode.Err, Messages.OnlyPurchaseBlocks);
                        return true;
                    }

                    // load player data
                    PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
                    int availableBlocks = playerData.getRemainingClaimBlocks();

                    Optional<Integer> blockCountOpt = args.getOne("numberOfBlocks");
                    // if no amount provided, just tell player value per block sold, and
                    // how many he can sell
                    if (!blockCountOpt.isPresent()) {
                        GriefPrevention.sendMessage(player, TextMode.Info, Messages.BlockSaleValue,
                                String.valueOf(GriefPrevention.instance.config_economy_claimBlocksSellValue), String.valueOf(availableBlocks));
                        return false;
                    }

                    // parse number of blocks
                    int blockCount = blockCountOpt.get();

                    if (blockCount <= 0) {
                        throw new CommandException(Texts.of("Invalid block count of lte 0"));
                    }

                    // if he doesn't have enough blocks, tell him so
                    if (blockCount > availableBlocks) {
                        GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotEnoughBlocksForSale);
                    }

                    // otherwise carry out the transaction
                    else {
                        // compute value and deposit it
                        double totalValue = blockCount * GriefPrevention.instance.config_economy_claimBlocksSellValue;
                        economy.depositPlayer(player, totalValue);

                        // subtract blocks
                        playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() - blockCount);
                        this.dataStore.savePlayerData(player.getUniqueId(), playerData);

                        // inform player
                        GriefPrevention.sendMessage(player, TextMode.Success, Messages.BlockSaleConfirmation, String.valueOf(totalValue),
                                String.valueOf(playerData.getRemainingClaimBlocks()));
                    }*/
                    return CommandResult.success();
                })
                .build(), "sellclaimblocks", "sellclaim");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Switches the shovel tool to administrative claims mode"))
                .permission("griefprevention.adminclaims")
                .executor((src, args) -> {
                    final Player player = checkPlayer(src);
                    PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
                    playerData.shovelMode = ShovelMode.Admin;
                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.AdminClaimsMode);
                    return CommandResult.success();
                })
                .build(), "adminclaims", "ac");
        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Switches the shovel tool back to basic claims mode"))
                .permission("griefprevention.claims")
                .executor((src, args) -> {
                    final Player player = checkPlayer(src);
                    PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
                    playerData.shovelMode = ShovelMode.Basic;
                    playerData.claimSubdividing = null;
                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.BasicClaimsMode);

                    return CommandResult.success();
                })
                .build(), "basicclaims", "bc");
        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Switches the shovel tool to subdivision mode, used to subdivide your claims"))
                .permission("griefprevention.claims")
                .executor((src, args) -> {
                    final Player player = checkPlayer(src);
                    PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
                    playerData.shovelMode = ShovelMode.Subdivide;
                    playerData.claimSubdividing = null;
                    GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SubdivisionMode);
                    GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SubdivisionVideo2, DataStore.SUBDIVISION_VIDEO_URL);

                    return CommandResult.success();
                })
                .build(), "subdivideclaims", "sc", "subdivideclaim");
        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Deletes the claim you're standing in, even if it's not your claim"))
                .permission("griefprevention.deleteclaims")
                .executor((src, args) -> {
                    final Player player = checkPlayer(src);
                    // determine which claim the player is standing in
                    Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*
                                                                                * ignore
                                                                                * height
                                                                                */, null);

                    if (claim == null) {
                        GriefPrevention.sendMessage(player, TextMode.Err, Messages.DeleteClaimMissing);
                    }

                    else {
                        // deleting an admin claim additionally requires the adminclaims
                        // permission
                        if (!claim.isAdminClaim() || player.hasPermission("griefprevention.adminclaims")) {
                            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
                            if (claim.children.size() > 0 && !playerData.warnedAboutMajorDeletion) {
                                GriefPrevention.sendMessage(player, TextMode.Warn, Messages.DeletionSubdivisionWarning);
                                playerData.warnedAboutMajorDeletion = true;
                            } else {
                                claim.removeSurfaceFluids(null);
                                this.dataStore.deleteClaim(claim, true);

                                // if in a creative mode world, /restorenature the claim
                                if (GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner())) {
                                    GriefPrevention.instance.restoreClaim(claim, 0);
                                }

                                GriefPrevention.sendMessage(player, TextMode.Success, Messages.DeleteSuccess);
                                GriefPrevention.AddLogEntry(
                                        player.getName() + " deleted " + claim.getOwnerName() + "'s claim at "
                                                + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()),
                                        CustomLogEntryTypes.AdminActivity);

                                // revert any current visualization
                                Visualization.Revert(player);

                                playerData.warnedAboutMajorDeletion = false;
                            }
                        } else {
                            GriefPrevention.sendMessage(player, TextMode.Err, Messages.CantDeleteAdminClaim);
                        }
                    }

                    return CommandResult.success();
                })
                .build(), "deleteclaim", "dc");
        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Toggles whether explosives may be used in a specific land claim"))
                .permission("griefprevention.claims")
                .executor((src, args) -> {
                    final Player player = checkPlayer(src);
                    // determine which claim the player is standing in
                    Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*
                                                                                * ignore
                                                                                * height
                                                                                */, null);

                    if (claim == null) {
                        GriefPrevention.sendMessage(player, TextMode.Err, Messages.DeleteClaimMissing);
                    }

                    else {
                        String noBuildReason = claim.allowBuild(player, BlockTypes.STONE);
                        if (noBuildReason != null) {
                            throw new CommandException(Texts.of(noBuildReason));
                        }

                        if (claim.areExplosivesAllowed) {
                            claim.areExplosivesAllowed = false;
                            GriefPrevention.sendMessage(player, TextMode.Success, Messages.ExplosivesDisabled);
                        } else {
                            claim.areExplosivesAllowed = true;
                            GriefPrevention.sendMessage(player, TextMode.Success, Messages.ExplosivesEnabled);
                        }
                    }

                    return CommandResult.success();
                })
                .build(), "claimexplosions");
        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Delete all of another player's claims"))
                .permission("griefprevention.deleteclaims")
                .arguments(player(Texts.of("player"), game)) // TODO: Use user commandelement when added
                .executor((src, args) -> {
                    final Player player = checkPlayer(src);

                    // try to find that player
                    User otherPlayer = args.<User>getOne("player").get();

                    // delete all that player's claims
                    this.dataStore.deleteClaimsForPlayer(otherPlayer.getUniqueId(), true);

                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.DeleteAllSuccess, otherPlayer.getName());
                    if (player != null) {
                        GriefPrevention.AddLogEntry(player.getName() + " deleted all claims belonging to " + otherPlayer.getName() + ".",
                                CustomLogEntryTypes.AdminActivity);

                        // revert any current visualization
                        Visualization.Revert(player);
                    }

                    return CommandResult.success();
                })
                .build(), "deleteallclaims");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Gives a player a manual about claiming land"))
                .permission("griefprevention.claimbook")
                .arguments(playerOrSource(Texts.of("player"), game))
                .executor((src, args) -> {
                    for (Player otherPlayer : args.<Player>getAll("player")) {
                        WelcomeTask task = new WelcomeTask(otherPlayer);
                        task.run();
                    }

                    return CommandResult.success();
                })
                .build(), "claimbook");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("List information about a player's claim blocks and claims"))
                .arguments(onlyOne(playerOrSource(Texts.of("player"), game)))
                .executor((src, args) -> {

                    // player whose claims will be listed
                    User otherPlayer = args.<User>getOne("player").get();

                    // otherwise if no permission to delve into another player's claims
                    // data
                    if (otherPlayer != src && !src.hasPermission("griefprevention.claimslistother")) {
                        throw new CommandPermissionException();
                    }

                    // load the target player's data
                    PlayerData playerData = this.dataStore.getPlayerData(otherPlayer.getUniqueId());
                    Vector<Claim> claims = playerData.getClaims();
                    GriefPrevention.sendMessage(src, TextMode.Instr, Messages.StartBlockMath,
                            String.valueOf(playerData.getAccruedClaimBlocks()),
                            String.valueOf((playerData.getBonusClaimBlocks() + this.dataStore.getGroupBonusBlocks(otherPlayer.getUniqueId()))),
                            String.valueOf((playerData.getAccruedClaimBlocks() + playerData.getBonusClaimBlocks()
                                    + this.dataStore.getGroupBonusBlocks(otherPlayer.getUniqueId()))));
                    if (claims.size() > 0) {
                        GriefPrevention.sendMessage(src, TextMode.Instr, Messages.ClaimsListHeader);
                        for (int i = 0; i < playerData.getClaims().size(); i++) {
                            Claim claim = playerData.getClaims().get(i);
                            GriefPrevention.sendMessage(src, Texts.of(TextMode.Instr, getfriendlyLocationString(claim.getLesserBoundaryCorner())
                                    + this.dataStore.getMessage(Messages.ContinueBlockMath, String.valueOf(claim.getArea()))));
                        }

                        GriefPrevention.sendMessage(src, TextMode.Instr, Messages.EndBlockMath, String.valueOf(playerData.getRemainingClaimBlocks()));
                    }

                    // drop the data we just loaded, if the player isn't online
                    if (!otherPlayer.isOnline())
                        this.dataStore.clearCachedPlayerData(otherPlayer.getUniqueId());


                    return CommandResult.success();
                })
                .build(), "claimslist", "claimlist", "listclaims");
        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("List all administrative claims"))
                .permission("griefprevention.adminclaims")
                .executor((src, args) -> {
                    // find admin claims
                    List<Claim> claims = new ArrayList<>();
                    for (Claim claim : this.dataStore.claims) {
                        if (claim.ownerID == null) { // admin claim
                            claims.add(claim);
                        }
                    }

                    if (claims.size() > 0) {
                        GriefPrevention.sendMessage(src, TextMode.Instr, Messages.ClaimsListHeader);
                        for (int i = 0; i < claims.size(); i++) {
                            Claim claim = claims.get(i);
                            GriefPrevention.sendMessage(src, Texts.of(TextMode.Instr, getfriendlyLocationString(claim.getLesserBoundaryCorner())));
                        }
                    }

                    return CommandResult.success();
                })
                .build(), "adminclaimslist");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Allows other players to pick up the items you dropped when you died"))
                .executor((src, args) -> {
                    Player player = checkPlayer(src);

                    PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
                    playerData.dropsAreUnlocked = true;
                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.DropUnlockConfirmation);

                    return CommandResult.success();
                })
                .build(), "unlockdrops");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Deletes all administrative claims"))
                .permission("griefprevention.adminclaims")
                .executor((src, args) -> {
                    args.checkPermission(src, "griefprevention.deleteclaims");
                    Player player = checkPlayer(src);

                    // delete all admin claims
                    this.dataStore.deleteClaimsForPlayer(null, true); // null for owner
                    // id indicates an administrative claim

                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.AllAdminDeleted);
                    GriefPrevention.AddLogEntry(player.getName() + " deleted all administrative claims.", CustomLogEntryTypes.AdminActivity);

                    // revert any current visualization
                    Visualization.Revert(player);

                    return CommandResult.success();
                })
                .build(), "deletealladminclaims");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Adds or subtracts bonus claim blocks for a player"))
                .permission("griefprevention.adjustclaimblocks")
                .arguments(string(Texts.of("player")), integer(Texts.of("amount")))
                .executor((src, args) -> {
                    Player player = checkPlayer(src);

                    // parse the adjustment amount
                    int adjustment = args.<Integer>getOne("amount").get();
                    String target = args.<String>getOne("player").get();

                    // if granting blocks to all players with a specific permission
                    if (target.startsWith("[") && target.endsWith("]")) {
                        String permissionIdentifier = target.substring(1, target.length() - 1);
                        int newTotal = this.dataStore.adjustGroupBonusBlocks(permissionIdentifier, adjustment);

                        GriefPrevention.sendMessage(player, TextMode.Success, Messages.AdjustGroupBlocksSuccess, permissionIdentifier,
                                String.valueOf(adjustment), String.valueOf(newTotal));
                        if (player != null)
                            GriefPrevention
                                    .AddLogEntry(
                                            player.getName() + " adjusted " + permissionIdentifier + "'s bonus claim blocks by " + adjustment + ".");

                        return CommandResult.success();
                    }

                    // otherwise, find the specified player
                    User targetPlayer;
                    try {
                        UUID playerID = UUID.fromString(target);
                        targetPlayer = game.getServiceManager().provideUnchecked(UserStorageService.class).get(playerID).orElse(null);

                    } catch (IllegalArgumentException e) {
                        targetPlayer = this.resolvePlayerByName(target).orElse(null);
                    }

                    if (targetPlayer == null) {
                        throw new CommandException(getMessage(Messages.PlayerNotFound2));
                    }

                    // give blocks to player
                    PlayerData playerData = this.dataStore.getPlayerData(targetPlayer.getUniqueId());
                    playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + adjustment);
                    this.dataStore.savePlayerData(targetPlayer.getUniqueId(), playerData);

                    GriefPrevention
                            .sendMessage(player, TextMode.Success, Messages.AdjustBlocksSuccess, targetPlayer.getName(), String.valueOf(adjustment),
                                    String.valueOf(playerData.getBonusClaimBlocks()));
                    if (player != null)
                        GriefPrevention.AddLogEntry(
                                player.getName() + " adjusted " + targetPlayer.getName() + "'s bonus claim blocks by " + adjustment + ".",
                                CustomLogEntryTypes.AdminActivity);


                    return CommandResult.success();
                })
                .build(), "adjustbonusclaimblocks", "acb");
        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Updates a player's accrued claim block total"))
                .permission("griefprevention.adjustclaimblocks")
                .arguments(string(Texts.of("player")), integer(Texts.of("amount")))
                .executor((src, args) -> {
                    Player player = checkPlayer(src);

                    // parse the adjustment amount
                    int newAmount = args.<Integer>getOne("amount").get();

                    // find the specified player
                    User targetPlayer = args.<String>getOne("player")
                            .flatMap(this::resolvePlayerByName)
                            .orElseThrow(() -> new CommandException(getMessage(Messages.PlayerNotFound2)));

                    // set player's blocks
                    PlayerData playerData = this.dataStore.getPlayerData(targetPlayer.getUniqueId());
                    playerData.setAccruedClaimBlocks(newAmount);
                    this.dataStore.savePlayerData(targetPlayer.getUniqueId(), playerData);

                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.SetClaimBlocksSuccess);
                    if (player != null)
                        GriefPrevention.AddLogEntry(player.getName() + " set " + targetPlayer.getName() + "'s accrued claim blocks to " + newAmount + ".",
                                CustomLogEntryTypes.AdminActivity);



                    return CommandResult.success();
                })
                .build(), "setaccruedclaimblocks", "scb");
        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Ejects you to nearby unclaimed land. Has a substantial cooldown period"))
                .executor((src, args) -> {
                    Player player = checkPlayer(src);

                    // FEATURE: empower players who get "stuck" in an area where they
                    // don't have permission to build to save themselves

                    PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
                    Claim claim = this.dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);

                    // if another /trapped is pending, ignore this slash command
                    if (playerData.pendingTrapped) {
                        return CommandResult.empty();
                    }

                    // if the player isn't in a claim or has permission to build, tell
                    // him to man up
                    if (claim == null || claim.allowBuild(player, BlockTypes.AIR) == null) {
                        throw new CommandException(getMessage(Messages.NotTrappedHere));
                    }

                    // if the player is in the nether or end, he's screwed (there's no
                    // way to programmatically find a safe place for him)
                    if (player.getWorld().getDimension().getType() != DimensionTypes.OVERWORLD) {
                        throw new CommandException(getMessage(Messages.TrappedWontWorkHere));
                    }

                    // if the player is in an administrative claim, he should contact an
                    // admin
                    if (claim.isAdminClaim()) {
                        throw new CommandException(getMessage(Messages.TrappedWontWorkHere));
                    }

                    // send instructions
                    GriefPrevention.sendMessage(player, TextMode.Instr, Messages.RescuePending);

                    // create a task to rescue this player in a little while
                    PlayerRescueTask task = new PlayerRescueTask(player, player.getLocation());
                    game.getScheduler().createTaskBuilder()
                            .delay(1, TimeUnit.SECONDS)
                            .execute(task)
                            .submit(this);

                    return CommandResult.success();
                })
                .build(), "trapped");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Initiates a siege versus another player"))
                .arguments(optional(onlyOne(player(Texts.of("playerName"), game))))
                .executor((src, args) -> {
                    Player player = checkPlayer(src);

                    // error message for when siege mode is disabled
                    if (!this.siegeEnabledForWorld(player.getWorld())) {
                        throw new CommandException(getMessage(Messages.NonSiegeWorld));
                    }

                    // can't start a siege when you're already involved in one
                    Player attacker = player;
                    PlayerData attackerData = this.dataStore.getPlayerData(attacker.getUniqueId());
                    if (attackerData.siegeData != null) {
                        throw new CommandException(getMessage(Messages.AlreadySieging));
                    }

                    // can't start a siege when you're protected from pvp combat
                    if (attackerData.pvpImmune) {
                        throw new CommandException(getMessage(Messages.CantFightWhileImmune));
                    }

                    // if a player name was specified, use that
                    Optional<Player> defenderOpt = args.<Player>getOne("playerName");
                    if (!defenderOpt.isPresent() && attackerData.lastPvpPlayer.length() > 0) {
                        defenderOpt = game.getServer().getPlayer(attackerData.lastPvpPlayer);
                    }
                    Player defender = defenderOpt.orElseThrow(() -> new CommandException(Texts.of("No player was matched")));

                    // victim must not have the permission which makes him immune to
                    // siege
                    if (defender.hasPermission("griefprevention.siegeimmune")) {
                        throw new CommandException(getMessage(Messages.SiegeImmune));
                    }

                    // victim must not be under siege already
                    PlayerData defenderData = this.dataStore.getPlayerData(defender.getUniqueId());
                    if (defenderData.siegeData != null) {
                        throw new CommandException(getMessage(Messages.AlreadyUnderSiegePlayer));
                    }

                    // victim must not be pvp immune
                    if (defenderData.pvpImmune) {
                        throw new CommandException(getMessage(Messages.NoSiegeDefenseless));
                    }

                    Claim defenderClaim = this.dataStore.getClaimAt(defender.getLocation(), false, null);

                    // defender must have some level of permission there to be protected
                    if (defenderClaim == null || defenderClaim.allowAccess(defender) != null) {
                        throw new CommandException(getMessage(Messages.NotSiegableThere));
                    }

                    // attacker must be close to the claim he wants to siege
                    if (!defenderClaim.isNear(attacker.getLocation(), 25)) {
                        throw new CommandException(getMessage(Messages.SiegeTooFarAway));
                    }

                    // claim can't be under siege already
                    if (defenderClaim.siegeData != null) {
                        throw new CommandException(getMessage(Messages.AlreadyUnderSiegeArea));
                    }

                    // can't siege admin claims
                    if (defenderClaim.isAdminClaim()) {
                        throw new CommandException(getMessage(Messages.NoSiegeAdminClaim));
                    }

                    // can't be on cooldown
                    if (dataStore.onCooldown(attacker, defender, defenderClaim)) {
                        throw new CommandException(getMessage(Messages.SiegeOnCooldown));
                    }

                    // start the siege
                    dataStore.startSiege(attacker, defender, defenderClaim);

                    // confirmation message for attacker, warning message for defender
                    GriefPrevention.sendMessage(defender, TextMode.Warn, Messages.SiegeAlert, attacker.getName());
                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.SiegeConfirmed, defender.getName());

                    return CommandResult.success();
                })
                .build(), "siege");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Toggles whether a player's messages will only reach other soft-muted players"))
                .permission("griefprevention.softmute")
                .arguments(onlyOne(player(Texts.of("player"), game)))
                .executor((src, args) -> {
                    Player player = checkPlayer(src);

                    // find the specified player
                    User targetPlayer = args.<User>getOne("player").get();

                    // toggle mute for player
                    boolean isMuted = this.dataStore.toggleSoftMute(targetPlayer.getUniqueId());
                    if (isMuted) {
                        GriefPrevention.sendMessage(player, TextMode.Success, Messages.SoftMuted, targetPlayer.getName());
                    } else {
                        GriefPrevention.sendMessage(player, TextMode.Success, Messages.UnSoftMuted, targetPlayer.getName());
                    }

                    return CommandResult.success();
                })
                .build(), "softmute");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Reloads Grief Prevention's configuration settings"))
                .permission("griefprevention.reload")
                .executor((src, args) -> {
                    this.loadConfig();
                        GriefPrevention.sendMessage(src, Texts.of(TextMode.Success,
                                "Configuration updated. If you have updated your Grief Prevention JAR, you still need to restart your server."));

                    return CommandResult.success();
                })
                .build(), "gpreload");
        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Allows a player to give away a pet they tamed"))
                .permission("griefprevention.givepet")
                .arguments(firstParsing(literal(Texts.of("player"), "cancel"), player(Texts.of("player"), game)))
                .executor((src, args) -> {
                    Player player = checkPlayer(src);

                    PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());

                    // special case: cancellation
                    if (args.getOne("player").orElse(false).equals(true)) {
                        playerData.petGiveawayRecipient = null;
                        GriefPrevention.sendMessage(player, TextMode.Success, Messages.PetTransferCancellation);
                        return CommandResult.success();
                    }

                    // find the specified player
                    User targetPlayer = args.<User>getOne("player").get();

                    // remember the player's ID for later pet transfer
                    playerData.petGiveawayRecipient = targetPlayer;

                    // send instructions
                    GriefPrevention.sendMessage(player, TextMode.Instr, Messages.ReadyToTransferPet);

                    return CommandResult.success();
                })
                .build(), "givepet");
        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Allows an administrator to get technical information about blocks in the world and items in hand"))
                .permission("griefprevention.gpblockinfo")
                .executor((src, args) -> {
                    Player player = checkPlayer(src);
                    throw new CommandException(Texts.of("Information about block-in-hand must be redesigned for Sponge")); // TODO: Handle sponge's w
                    // worldview
                    /*
                    ItemStack inHand = player.getItemInHand().orElse(null);

                    player.sendMessage("In Hand: " + String.format("%s(%d:%d)", inHand.getType().name(), inHand.getTypeId(), inHand.getData().getData()));

                    Block inWorld = GriefPrevention.getTargetNonAirBlock(player, 300);
                    player.sendMessage("In World: " + String.format("%s(%d:%d)", inWorld.getType().name(), inWorld.getTypeId(), inWorld.getData()));

                    return CommandResult.success();*/
                })
                .build(), "gpblockinfo");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Ignores another player's chat messages"))
                .permission("griefprevention.ignore")
                .arguments(onlyOne(player(Texts.of("player"), game)))
                .executor((src, args) -> {
                    Player player = checkPlayer(src);

                    // validate target player
                    Player targetPlayer = args.<Player>getOne("player").get();
                    if (targetPlayer.hasPermission("griefprevention.notignorable")) {
                        throw new CommandException(getMessage(Messages.PlayerNotIgnorable));
                    }

                    this.setIgnoreStatus(player, targetPlayer, IgnoreMode.StandardIgnore);

                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.IgnoreConfirmation);

                    return CommandResult.success();
                })
                .build(), "ignoreplayer", "ignore");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Unignores another player's chat messages"))
                .permission("griefprevention.ignore")
                .arguments(onlyOne(player(Texts.of("player"), game)))
                .executor((src, args) -> {
                    Player player = checkPlayer(src);

                    // validate target player
                    User targetPlayer = args.<User>getOne("player").get();

                    PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
                    Boolean ignoreStatus = playerData.ignoredPlayers.get(targetPlayer.getUniqueId());
                    if (ignoreStatus == null || ignoreStatus == true) {
                        throw new CommandException(getMessage(Messages.NotIgnoringPlayer));
                    }

                    this.setIgnoreStatus(player, targetPlayer, IgnoreMode.None);

                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.UnIgnoreConfirmation);


                    return CommandResult.success();
                })
                .build(), "unignoreplayer", "unignore");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Lists the players you're ignoring in chat"))
                .permission("griefprevention.ignore")
                .executor((src, args) -> {
                    Player player = checkPlayer(src);

                    PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
                    StringBuilder builder = new StringBuilder();
                    for (Entry<UUID, Boolean> entry : playerData.ignoredPlayers.entrySet()) {
                        if (entry.getValue() != null) {
                            // if not an admin ignore, add it to the list
                            if (!entry.getValue()) {
                                builder.append(GriefPrevention.lookupPlayerName(entry.getKey()));
                                builder.append(" ");
                            }
                        }
                    }

                    String list = builder.toString().trim();
                    if (list.isEmpty()) {
                        GriefPrevention.sendMessage(player, TextMode.Info, Messages.NotIgnoringAnyone);
                    } else {
                        GriefPrevention.sendMessage(player, Texts.of(TextMode.Info, list));
                    }



                    return CommandResult.success();
                })
                .build(), "ignoredplayerlist", "ignores", "ignored", "ignoredlist", "listignores", "listignored", "ignoring");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Forces two players to ignore each other in chat"))
                .permission("griefprevention.separate")
                .arguments(onlyOne(player(Texts.of("player1"), game)), onlyOne(player(Texts.of("player2"), game)))
                .executor((src, args) -> {
                    Player player = checkPlayer(src);
                    // validate target players
                    User targetPlayer = args.<User>getOne("player1").get();

                    User targetPlayer2 = args.<User>getOne("player2").get();

                    this.setIgnoreStatus(targetPlayer, targetPlayer2, IgnoreMode.AdminIgnore);

                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.SeparateConfirmation);
                    return  CommandResult.success();
                })
                .build(), "separate");

        game.getCommandManager().register(this, CommandSpec.builder()
                .description(Texts.of("Reverses /separate"))
                .permission("griefprevention.separate")
                .arguments(onlyOne(player(Texts.of("player1"), game)), onlyOne(player(Texts.of("player2"), game)))
                .executor((src, args) -> {
                    Player player = checkPlayer(src);
                    // validate target players
                    User targetPlayer = args.<User>getOne("player1").get();
                    User targetPlayer2 = args.<User>getOne("player2").get();

                    this.setIgnoreStatus(targetPlayer, targetPlayer2, IgnoreMode.None);
                    this.setIgnoreStatus(targetPlayer2, targetPlayer, IgnoreMode.None);

                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.UnSeparateConfirmation);
                    return  CommandResult.success();
                })
                .build(), "unseparate");
    }

    void setIgnoreStatus(User ignorer, User ignoree, IgnoreMode mode) {
        PlayerData playerData = this.dataStore.getPlayerData(ignorer.getUniqueId());
        if (mode == IgnoreMode.None) {
            playerData.ignoredPlayers.remove(ignoree.getUniqueId());
        } else {
            playerData.ignoredPlayers.put(ignoree.getUniqueId(), mode == IgnoreMode.StandardIgnore ? false : true);
        }

        playerData.ignoreListChanged = true;
        if (!ignorer.isOnline()) {
            this.dataStore.savePlayerData(ignorer.getUniqueId(), playerData);
            this.dataStore.clearCachedPlayerData(ignorer.getUniqueId());
        }
    }

    enum IgnoreMode {
        None, StandardIgnore, AdminIgnore
    }

    private String trustEntryToPlayerName(String entry) {
        if (entry.startsWith("[") || entry.equals("public")) {
            return entry;
        } else {
            return GriefPrevention.lookupPlayerName(entry);
        }
    }

    public static String getfriendlyLocationString(Location<World> location) {
        return location.getExtent().getName() + ": x" + location.getBlockX() + ", z" + location.getBlockZ();
    }

    private CommandResult abandonClaimHandler(Player player, boolean deleteTopLevelClaim) {
        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());

        // which claim is being abandoned?
        Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*
                                                                            * ignore
                                                                            * height
                                                                            */, null);
        if (claim == null) {
            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.AbandonClaimMissing);
        }

        // verify ownership
        else if (claim.allowEdit(player) != null) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotYourClaim);
        }

        // warn if has children and we're not explicitly deleting a top level
        // claim
        else if (claim.children.size() > 0 && !deleteTopLevelClaim) {
            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.DeleteTopLevelClaim);
            return CommandResult.empty();
        }

        else {
            // delete it
            claim.removeSurfaceFluids(null);
            this.dataStore.deleteClaim(claim, true);

            // if in a creative mode world, restore the claim area
            if (GriefPrevention.instance.creativeRulesApply(claim.getLesserBoundaryCorner())) {
                GriefPrevention.AddLogEntry(
                        player.getName() + " abandoned a claim @ " + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
                GriefPrevention.sendMessage(player, TextMode.Warn, Messages.UnclaimCleanupWarning);
                GriefPrevention.instance.restoreClaim(claim, 20L * 60 * 2);
            }

            // adjust claim blocks when abandoning a top level claim
            if (claim.parent == null) {
                playerData.setAccruedClaimBlocks(
                        playerData.getAccruedClaimBlocks() - (int) Math.ceil((claim.getArea() * (1 - this.config_claims_abandonReturnRatio))));
            }

            // tell the player how many claim blocks he has left
            int remainingBlocks = playerData.getRemainingClaimBlocks();
            GriefPrevention.sendMessage(player, TextMode.Success, Messages.AbandonSuccess, String.valueOf(remainingBlocks));

            // revert any current visualization
            Visualization.Revert(player);

            playerData.warnedAboutMajorDeletion = false;
        }

        return CommandResult.success();

    }

    // helper method keeps the trust commands consistent and eliminates
    // duplicate code
    private void handleTrustCommand(Player player, ClaimPermission permissionLevel, String recipientName) throws CommandException {
        // determine which claim the player is standing in
        Claim claim = this.dataStore.getClaimAt(player.getLocation(), true /*
                                                                            * ignore
                                                                            * height
                                                                            */, null);

        // validate player or group argument
        String permission = null;
        User otherPlayer = null;
        UUID recipientID = null;
        if (recipientName.startsWith("[") && recipientName.endsWith("]")) {
            permission = recipientName.substring(1, recipientName.length() - 1);
            if (permission == null || permission.isEmpty()) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.InvalidPermissionID);
                return;
            }
        }

        else if (recipientName.contains(".")) {
            permission = recipientName;
        }

        else {
            otherPlayer = this.resolvePlayerByName(recipientName).orElse(null);
            if (otherPlayer == null && !recipientName.equals("public") && !recipientName.equals("all")) {
                throw new CommandException(getMessage(Messages.PlayerNotFound2));
            }

            if (otherPlayer != null) {
                recipientName = otherPlayer.getName();
                recipientID = otherPlayer.getUniqueId();
            } else {
                recipientName = "public";
            }
        }

        // determine which claims should be modified
        ArrayList<Claim> targetClaims = new ArrayList<Claim>();
        if (claim == null) {
            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
            for (int i = 0; i < playerData.getClaims().size(); i++) {
                targetClaims.add(playerData.getClaims().get(i));
            }
        } else {
            // check permission here
            if (claim.allowGrantPermission(player) != null) {
                throw new CommandException(getMessage(Messages.NoPermissionTrust, claim.getOwnerName()));
            }

            // see if the player has the level of permission he's trying to
            // grant
            String errorMessage = null;

            // permission level null indicates granting permission trust
            if (permissionLevel == null) {
                errorMessage = claim.allowEdit(player);
                if (errorMessage != null) {
                    errorMessage = "Only " + claim.getOwnerName() + " can grant /PermissionTrust here.";
                }
            }

            // otherwise just use the ClaimPermission enum values
            else {
                switch (permissionLevel) {
                    case Access:
                        errorMessage = claim.allowAccess(player);
                        break;
                    case Inventory:
                        errorMessage = claim.allowContainers(player);
                        break;
                    default:
                        errorMessage = claim.allowBuild(player, BlockTypes.AIR);
                }
            }

            // error message for trying to grant a permission the player doesn't
            // have
            if (errorMessage != null) {
                throw new CommandException(getMessage(Messages.CantGrantThatPermission));
            }

            targetClaims.add(claim);
        }

        // if we didn't determine which claims to modify, tell the player to be
        // specific
        if (targetClaims.size() == 0) {
            throw new CommandException(getMessage(Messages.GrantPermissionNoClaim));
        }

        // apply changes
        for (int i = 0; i < targetClaims.size(); i++) {
            Claim currentClaim = targetClaims.get(i);
            String identifierToAdd = recipientName;
            if (permission != null) {
                identifierToAdd = "[" + permission + "]";
            } else if (recipientID != null) {
                identifierToAdd = recipientID.toString();
            }

            if (permissionLevel == null) {
                if (!currentClaim.managers.contains(identifierToAdd)) {
                    currentClaim.managers.add(identifierToAdd);
                }
            } else {
                currentClaim.setPermission(identifierToAdd, permissionLevel);
            }
            this.dataStore.saveClaim(currentClaim);
        }

        // notify player
        if (recipientName.equals("public"))
            recipientName = this.dataStore.getMessage(Messages.CollectivePublic);
        String permissionDescription;
        if (permissionLevel == null) {
            permissionDescription = this.dataStore.getMessage(Messages.PermissionsPermission);
        } else if (permissionLevel == ClaimPermission.Build) {
            permissionDescription = this.dataStore.getMessage(Messages.BuildPermission);
        } else if (permissionLevel == ClaimPermission.Access) {
            permissionDescription = this.dataStore.getMessage(Messages.AccessPermission);
        } else // ClaimPermission.Inventory
        {
            permissionDescription = this.dataStore.getMessage(Messages.ContainersPermission);
        }

        String location;
        if (claim == null) {
            location = this.dataStore.getMessage(Messages.LocationAllClaims);
        } else {
            location = this.dataStore.getMessage(Messages.LocationCurrentClaim);
        }

        GriefPrevention.sendMessage(player, TextMode.Success, Messages.GrantPermissionConfirmation, recipientName, permissionDescription, location);
    }

    public Optional<User> resolvePlayerByName(String name) {
        // try online players first
        Optional<Player> targetPlayer = GriefPrevention.instance.game.getServer().getPlayer(name);
        if (targetPlayer.isPresent())
            return Optional.of((User) targetPlayer.get());

        Optional<User> user = GriefPrevention.instance.game.getServiceManager().provide(UserStorageService.class).get().get(name);
        if (user.isPresent())
            return user;

        return Optional.empty();
    }

    // helper method to resolve a player name from the player's UUID
    static String lookupPlayerName(UUID playerID) {
        // parameter validation
        if (playerID == null)
            return "somebody";

        // check the cache
        Optional<User> player = GriefPrevention.instance.game.getServiceManager().provide(UserStorageService.class).get().get(playerID);
        if (player.isPresent() || player.get().isOnline()) {
            return player.get().getName();
        } else {
            return "someone";
        }
    }

    // string overload for above helper
    static String lookupPlayerName(String playerID) {
        Optional<User> user = GriefPrevention.instance.game.getServiceManager().provide(UserStorageService.class).get().get(playerID);
        if (!user.isPresent()) {
            GriefPrevention.AddLogEntry("Error: Tried to look up a local player name for invalid UUID: " + playerID);
            return "someone";
        }

        return user.get().getName();
    }

    public void onDisable() {
        // save data for any online players
        Collection<Player> players = (Collection<Player>) GriefPrevention.instance.game.getServer().getOnlinePlayers();
        for (Player player : players) {
            UUID playerID = player.getUniqueId();
            PlayerData playerData = this.dataStore.getPlayerData(playerID);
            this.dataStore.savePlayerDataSync(playerID, playerData);
        }

        this.dataStore.close();

        // dump any remaining unwritten log entries
        this.customLogger.WriteEntries();

        AddLogEntry("GriefPrevention disabled.");
    }

    // called when a player spawns, applies protection for that player if
    // necessary
    public void checkPvpProtectionNeeded(Player player) {
        // if anti spawn camping feature is not enabled, do nothing
        if (!this.config_pvp_protectFreshSpawns)
            return;

        // if pvp is disabled, do nothing
        if (!pvpRulesApply(player.getWorld()))
            return;

        // if player is in creative mode, do nothing
        if (player.get(Keys.GAME_MODE).get() == GameModes.CREATIVE)
            return;

        // if the player has the damage any player permission enabled, do
        // nothing
        if (player.hasPermission("griefprevention.nopvpimmunity"))
            return;

        // check inventory for well, anything
        if (GriefPrevention.isInventoryEmpty(player)) {
            // if empty, apply immunity
            PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
            playerData.pvpImmune = true;

            // inform the player after he finishes respawning
            GriefPrevention.sendMessage(player, TextMode.Success, Messages.PvPImmunityStart, 5L);

            // start a task to re-check this player's inventory every minute
            // until his immunity is gone
            PvPImmunityValidationTask task = new PvPImmunityValidationTask(player);
            this.game.getScheduler().createTaskBuilder().delay(1, TimeUnit.MINUTES).execute(task).submit(this);
        }
    }

    static boolean isInventoryEmpty(Player player) {
        InventoryPlayer inventory = ((EntityPlayerMP) player).inventory;
        return inventory.mainInventory.length == 0 && inventory.armorInventory.length == 0;
    }

    // checks whether players siege in a world
    public boolean siegeEnabledForWorld(World world) {
        return this.config_siege_enabledWorlds.contains(world);
    }

    // moves a player from the claim they're in to a nearby wilderness location
    public Location<World> ejectPlayer(Player player) {
        // look for a suitable location
        Location<World> candidateLocation = player.getLocation();
        while (true) {
            Claim claim = null;
            claim = GriefPrevention.instance.dataStore.getClaimAt(candidateLocation, false, null);

            // if there's a claim here, keep looking
            if (claim != null) {
                candidateLocation = new Location<World>(claim.lesserBoundaryCorner.getExtent(), claim.lesserBoundaryCorner.getBlockX() - 1,
                        claim.lesserBoundaryCorner.getBlockY(), claim.lesserBoundaryCorner.getBlockZ() - 1);
                continue;
            }

            // otherwise find a safe place to teleport the player
            else {
                // find a safe height, a couple of blocks above the surface
                GuaranteeChunkLoaded(candidateLocation);
                Location<World> destination = candidateLocation.add(candidateLocation.getExtent().getBlockMax().add(0, 2, 0).toDouble());
                player.setLocation(destination);
                return destination;
            }
        }
    }

    // ensures a piece of the managed world is loaded into server memory
    // (generates the chunk if necessary)
    private static void GuaranteeChunkLoaded(Location<World> location) {
        location.getExtent().loadChunk(location.getBlockPosition(), true);
    }

    static Text getMessage(Messages messageID, String... args) {
        return Texts.of(GriefPrevention.instance.dataStore.getMessage(messageID, args));
    }

    // sends a color-coded message to a player
    static void sendMessage(CommandSource player, TextColor color, Messages messageID, String... args) {
        sendMessage(player, color, messageID, Texts.of(), args);
    }

    // sends a color-coded message to a player
    static void sendMessage(CommandSource player, TextColor color, Messages messageID, Text text, String... args) {
        sendMessage(player, color, messageID, 0, text, args);
    }

    // sends a color-coded message to a player
    static void sendMessage(CommandSource player, TextColor color, Messages messageID, long delayInTicks, String... args) {
        sendMessage(player, color, messageID, delayInTicks, Texts.of(), args);
    }

    // sends a color-coded message to a player
    static void sendMessage(CommandSource player, TextColor color, Messages messageID, long delayInTicks, Text text, String... args) {
        String message = GriefPrevention.instance.dataStore.getMessage(messageID, args);
        if (message.isEmpty() || message.equals("{0}")) {
            sendMessage(player, Texts.of(text), delayInTicks);
        } else {
            sendMessage(player, Texts.of(color, message, text), delayInTicks);
        }
    }

    // sends a color-coded message to a player
    static void sendMessage(CommandSource player, Text message) {
        if (message == Texts.of() || message == null)
            return;

        if (player == null) {
            GriefPrevention.AddLogEntry(Texts.toPlain(message));
        } else {
            player.sendMessage(message);
        }
    }

    static void sendMessage(CommandSource player, Text message, long delayInTicks) {
        SendPlayerMessageTask task = new SendPlayerMessageTask((Player) player, message);
        if (delayInTicks > 0) {
            GriefPrevention.instance.game.getScheduler().createTaskBuilder().delayTicks(delayInTicks).execute(task).submit(GriefPrevention.instance);
        } else {
            task.run();
        }
    }

    // checks whether players can create claims in a world
    public boolean claimsEnabledForWorld(World world) {
        return this.config_claims_worldModes.get(world) != ClaimsMode.Disabled;
    }

    // determines whether creative anti-grief rules apply at a location
    boolean creativeRulesApply(Location<World> location) {
        return this.config_claims_worldModes.get((location.getExtent())) == ClaimsMode.Creative;
    }

    public String allowBuild(Player player, Location<World> location) {
        return this.allowBuild(player, location, location.getBlock().getType());
    }

    public String allowBuild(Player player, Location<World> location, BlockType material) {
        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = this.dataStore.getClaimAt(location, false, playerData.lastClaim);

        // exception: administrators in ignore claims mode and special player
        // accounts created by server mods
        if (playerData.ignoreClaims || GriefPrevention.instance.config_mods_ignoreClaimsAccounts.contains(player.getName()))
            return null;

        // wilderness rules
        if (claim == null) {
            // no building in the wilderness in creative mode
            if (this.creativeRulesApply(location) || this.config_claims_worldModes.get(location.getExtent()) == ClaimsMode.SurvivalRequiringClaims) {
                // exception: when chest claims are enabled, players who have
                // zero land claims and are placing a chest
                if (material != BlockTypes.CHEST || playerData.getClaims().size() > 0
                        || GriefPrevention.instance.config_claims_automaticClaimsForNewPlayersRadius == -1) {
                    String reason = this.dataStore.getMessage(Messages.NoBuildOutsideClaims);
                    if (player.hasPermission("griefprevention.ignoreclaims"))
                        reason += "  " + this.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
                    reason += "  " + this.dataStore.getMessage(Messages.CreativeBasicsVideo2, DataStore.CREATIVE_VIDEO_URL_RAW);
                    return reason;
                } else {
                    return null;
                }
            }

            // but it's fine in survival mode
            else {
                return null;
            }
        }

        // if not in the wilderness, then apply claim rules (permissions, etc)
        else {
            // cache the claim for later reference
            playerData.lastClaim = claim;
            return claim.allowBuild(player, material);
        }
    }

    public String allowBreak(Player player, BlockSnapshot snapshot) {
        PlayerData playerData = this.dataStore.getPlayerData(player.getUniqueId());
        Claim claim = this.dataStore.getClaimAt(snapshot.getLocation().get(), false, playerData.lastClaim);

        // exception: administrators in ignore claims mode, and special player
        // accounts created by server mods
        if (playerData.ignoreClaims || GriefPrevention.instance.config_mods_ignoreClaimsAccounts.contains(player.getName()))
            return null;

        // wilderness rules
        if (claim == null) {
            // no building in the wilderness in creative mode
            if (this.creativeRulesApply(snapshot.getLocation().get()) || this.config_claims_worldModes.get(snapshot.getLocation().get().getExtent()) == ClaimsMode.SurvivalRequiringClaims) {
                String reason = this.dataStore.getMessage(Messages.NoBuildOutsideClaims);
                if (player.hasPermission("griefprevention.ignoreclaims"))
                    reason += "  " + this.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
                reason += "  " + this.dataStore.getMessage(Messages.CreativeBasicsVideo2, DataStore.CREATIVE_VIDEO_URL_RAW);
                return reason;
            }

            // but it's fine in survival mode
            else {
                return null;
            }
        } else {
            // cache the claim for later reference
            playerData.lastClaim = claim;

            // if not in the wilderness, then apply claim rules (permissions,
            // etc)
            return claim.allowBreak(player, snapshot.getState().getType());
        }
    }

    // restores nature in multiple chunks, as described by a claim instance
    // this restores all chunks which have ANY number of claim blocks from this
    // claim in them
    // if the claim is still active (in the data store), then the claimed blocks
    // will not be changed (only the area bordering the claim)
    public void restoreClaim(Claim claim, long delayInTicks) {
        // admin claims aren't automatically cleaned up when deleted or
        // abandoned
        if (claim.isAdminClaim())
            return;

        // it's too expensive to do this for huge claims
        if (claim.getArea() > 10000)
            return;

        ArrayList<Chunk> chunks = claim.getChunks();
        for (Chunk chunk : chunks) {
            this.restoreChunk(chunk, this.getSeaLevel(chunk.getWorld()) - 15, false, delayInTicks, null);
        }
    }

    public void restoreChunk(Chunk chunk, int miny, boolean aggressiveMode, long delayInTicks, Player playerReceivingVisualization) {
        // build a snapshot of this chunk, including 1 block boundary outside of
        // the chunk all the way around
        int maxHeight = chunk.getWorld().getDimension().getBuildHeight();
        BlockSnapshot[][][] snapshots = new BlockSnapshot[18][maxHeight][18];
        BlockSnapshot startBlock = chunk.createSnapshot(0, 0, 0);
        Location<World> startLocation = new Location<World>(chunk.getWorld(), startBlock.getPosition().getX() - 1, 0, startBlock.getPosition().getZ() - 1);
        for (int x = 0; x < snapshots.length; x++) {
            for (int z = 0; z < snapshots[0][0].length; z++) {
                for (int y = 0; y < snapshots[0].length; y++) {
                    snapshots[x][y][z] = chunk.getWorld().createSnapshot(startLocation.getBlockX() + x, startLocation.getBlockY() + y, startLocation.getBlockZ() + z);
                }
            }
        }

        // create task to process those data in another thread
        Location<World> lesserBoundaryCorner = chunk.createSnapshot(0, 0, 0).getLocation().get();
        Location<World> greaterBoundaryCorner = chunk.createSnapshot(15, 0, 15).getLocation().get();

        // create task
        // when done processing, this task will create a main thread task to
        // actually update the world with processing results
        RestoreNatureProcessingTask task = new RestoreNatureProcessingTask(snapshots, miny, chunk.getWorld().getDimension().getType(),
                lesserBoundaryCorner.getBiome(), lesserBoundaryCorner, greaterBoundaryCorner, this.getSeaLevel(chunk.getWorld()),
                aggressiveMode, GriefPrevention.instance.creativeRulesApply(lesserBoundaryCorner), playerReceivingVisualization);
        GriefPrevention.instance.game.getScheduler().createTaskBuilder().async().delayTicks(delayInTicks).execute(task).submit(this);
    }

    private void parseBlockIdListFromConfig(List<String> stringsToParse, List<BlockType> blockTypes) {
        // for each string in the list
        for (int i = 0; i < stringsToParse.size(); i++) {
            // try to parse the string value into a material info
            Optional<BlockType> blockType = game.getRegistry().getType(BlockType.class, stringsToParse.get(i));

            // null value returned indicates an error parsing the string from
            // the config file
            if (!blockType.isPresent()) {
                // show error in log
                GriefPrevention.AddLogEntry("ERROR: Unable to read a block entry from the config file.  Please update your config.hocon.");

                // update string, which will go out to config file to help user
                // find the error entry
                if (!stringsToParse.get(i).contains("can't")) {
                    stringsToParse.set(i, stringsToParse.get(i) + "     <-- can't understand this entry, see Sponge documentation");
                }
            }

            // otherwise store the valid entry in config data
            else {
                blockTypes.add(blockType.get());
            }
        }
    }

    public int getSeaLevel(World world) {
        Integer overrideValue = this.config_seaLevelOverride.get(world.getName());
        if (overrideValue == null || overrideValue == -1) {
            return world.getDimension().getMinimumSpawnHeight();
        } else {
            return overrideValue;
        }
    }

    /*private static BlockType getTargetNonAirBlock(Player player, int maxDistance) throws IllegalStateException {
        BlockIterator iterator = new BlockIterator(player.getLocation(), player.getEyeHeight(), maxDistance);
        BlockType result = player.getLocation().getRelative(Direction.UP).getBlockType();
        while (iterator.hasNext()) {
            result = iterator.next();
            if (result.getType() != Material.AIR)
                return result;
        }

        return result;
    }*/

    public boolean containsBlockedIP(String message) {
        message = message.replace("\r\n", "");
        Pattern ipAddressPattern = Pattern.compile("([0-9]{1,3}\\.){3}[0-9]{1,3}");
        Matcher matcher = ipAddressPattern.matcher(message);

        // if it looks like an IP address
        if (matcher.find()) {
            // and it's not in the list of allowed IP addresses
            if (!GriefPrevention.instance.config_spam_allowedIpAddresses.contains(matcher.group())) {
                return true;
            }
        }

        return false;
    }

    public boolean pvpRulesApply(World world) {
        return false;
        /* TODO
        Boolean configSetting = this.config_pvp_specifiedWorlds.get(world);
        if (configSetting != null)
            return configSetting;
        return world.getPVP(); */
    }
}
