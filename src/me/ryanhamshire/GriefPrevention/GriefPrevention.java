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

import static org.spongepowered.api.command.args.GenericArguments.integer;
import static org.spongepowered.api.command.args.GenericArguments.onlyOne;
import static org.spongepowered.api.command.args.GenericArguments.optional;
import static org.spongepowered.api.command.args.GenericArguments.player;
import static org.spongepowered.api.command.args.GenericArguments.playerOrSource;
import static org.spongepowered.api.command.args.GenericArguments.string;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;
import me.ryanhamshire.GriefPrevention.command.CommandAbandonAllClaims;
import me.ryanhamshire.GriefPrevention.command.CommandAbandonClaim;
import me.ryanhamshire.GriefPrevention.command.CommandAccessTrust;
import me.ryanhamshire.GriefPrevention.command.CommandAdjustBonusClaimBlocks;
import me.ryanhamshire.GriefPrevention.command.CommandAdminClaims;
import me.ryanhamshire.GriefPrevention.command.CommandAdminClaimsList;
import me.ryanhamshire.GriefPrevention.command.CommandBasicClaims;
import me.ryanhamshire.GriefPrevention.command.CommandBuyClaimBlocks;
import me.ryanhamshire.GriefPrevention.command.CommandClaimBook;
import me.ryanhamshire.GriefPrevention.command.CommandClaimExplosions;
import me.ryanhamshire.GriefPrevention.command.CommandClaimsList;
import me.ryanhamshire.GriefPrevention.command.CommandContainerTrust;
import me.ryanhamshire.GriefPrevention.command.CommandDeleteAllAdminClaims;
import me.ryanhamshire.GriefPrevention.command.CommandDeleteAllClaims;
import me.ryanhamshire.GriefPrevention.command.CommandDeleteClaim;
import me.ryanhamshire.GriefPrevention.command.CommandGivePet;
import me.ryanhamshire.GriefPrevention.command.CommandGpBlockInfo;
import me.ryanhamshire.GriefPrevention.command.CommandGpReload;
import me.ryanhamshire.GriefPrevention.command.CommandGriefPrevention;
import me.ryanhamshire.GriefPrevention.command.CommandIgnoreClaim;
import me.ryanhamshire.GriefPrevention.command.CommandIgnorePlayer;
import me.ryanhamshire.GriefPrevention.command.CommandIgnoredPlayerList;
import me.ryanhamshire.GriefPrevention.command.CommandPermissionTrust;
import me.ryanhamshire.GriefPrevention.command.CommandRestoreNature;
import me.ryanhamshire.GriefPrevention.command.CommandRestoreNatureAggressive;
import me.ryanhamshire.GriefPrevention.command.CommandRestoreNatureFill;
import me.ryanhamshire.GriefPrevention.command.CommandSellClaimBlocks;
import me.ryanhamshire.GriefPrevention.command.CommandSeparate;
import me.ryanhamshire.GriefPrevention.command.CommandSetAccruedClaimBlocks;
import me.ryanhamshire.GriefPrevention.command.CommandSiege;
import me.ryanhamshire.GriefPrevention.command.CommandSoftMute;
import me.ryanhamshire.GriefPrevention.command.CommandSubdivideClaims;
import me.ryanhamshire.GriefPrevention.command.CommandTransferClaim;
import me.ryanhamshire.GriefPrevention.command.CommandTrapped;
import me.ryanhamshire.GriefPrevention.command.CommandTrust;
import me.ryanhamshire.GriefPrevention.command.CommandTrustList;
import me.ryanhamshire.GriefPrevention.command.CommandUnignorePlayer;
import me.ryanhamshire.GriefPrevention.command.CommandUnlockDrops;
import me.ryanhamshire.GriefPrevention.command.CommandUnseparate;
import me.ryanhamshire.GriefPrevention.command.CommandUntrust;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.util.IpSet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Plugin(id = "GriefPrevention", name = "GriefPrevention", version = "12.7.1")
public class GriefPrevention {

    // for convenience, a reference to the instance of this plugin
    public static GriefPrevention instance;
    @Inject public PluginContainer pluginContainer;

    // for logging to the console and log file
    private static Logger log = Logger.getLogger("Minecraft");

    // this handles data storage, like player and region data
    public DataStore dataStore;

    private static final String HEADER = "12.1.7\n"
            + "# If you need help with the configuration or have any questions related to GriefPrevention,\n"
            + "# join us at the IRC or drop by our forums and leave a post.\n"
            + "# IRC: #sponge @ irc.esper.net ( http://webchat.esper.net/?channel=sponge )\n"
            + "# Forums: https://forums.spongepowered.org/\n";

    private HoconConfigurationLoader loader;
    private CommentedConfigurationNode rootNode = SimpleCommentedConfigurationNode.root(ConfigurationOptions.defaults()
            .setHeader(HEADER));

    // log entry manager for GP's custom log files
    CustomLogger customLogger;

    // configuration variables, loaded/saved from a config.hocon

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
    public List<ItemInfo> config_mods_accessTrustIds;

    // list of block IDs which should require /containertrust for player interaction
    public List<ItemInfo> config_mods_containerTrustIds;

    // list of player names which ALWAYS ignore claims
    public List<String> config_mods_ignoreClaimsAccounts;

    // list of block IDs which can be destroyed by explosions, even in claimed areas
    public List<ItemInfo> config_mods_explodableIds;

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

    private static final Map<String, Boolean> FLAG_BOOLEANS = ImmutableMap.<String, Boolean>builder()
            .put("allow", true)
            .put("deny", false)
            .build();
    
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
    public void onServerStarted(GameStartedServerEvent event) {
        instance = this;

        AddLogEntry("Grief Prevention boot start.");

        this.loadConfig();

        this.customLogger = new CustomLogger();

        AddLogEntry("Finished loading configuration.");

        // when datastore initializes, it loads player and claim data, and posts some stats to the log
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
                        "Because there was a problem with the database, GriefPrevention will not function properly.  Either update the database config settings resolve the issue, or delete those lines from your config.hocon so that GriefPrevention can use the file system to store data.");
                e.printStackTrace();
                return;
            }
        }

        // if not using the database because it's not configured or because
        // there was a problem, use the file system to store data
        // this is the preferred method, as it's simpler than the database
        // scenario
        if (this.dataStore == null) {
            File oldclaimdata = new File(Sponge.getGame().getSavesDirectory().toFile(), "ClaimData");
            if (oldclaimdata.exists()) {
                if (!FlatFileDataStore.hasData()) {
                    File claimdata = new File("plugins" + File.separator + "GriefPreventionData" + File.separator + "ClaimData");
                    oldclaimdata.renameTo(claimdata);
                    File oldplayerdata = new File(Sponge.getGame().getSavesDirectory().toFile(), "PlayerData");
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
            Sponge.getGame().getScheduler().createTaskBuilder().interval(10, TimeUnit.MINUTES).execute(task)
                    .submit(GriefPrevention.instance);
        }

        // start the recurring cleanup event for entities in creative worlds
        EntityCleanupTask task = new EntityCleanupTask(0);
        Sponge.getGame().getScheduler().createTaskBuilder().delay(2, TimeUnit.MINUTES).execute(task).submit(GriefPrevention.instance);

        // start recurring cleanup scan for unused claims belonging to inactive
        // players
        CleanupUnusedClaimsTask task2 = new CleanupUnusedClaimsTask();
        Sponge.getGame().getScheduler().createTaskBuilder().interval(5, TimeUnit.MINUTES).execute(task2).submit(GriefPrevention.instance);

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
        Collection<Player> players = (Collection<Player>) Sponge.getGame().getServer().getOnlinePlayers();
        for (Player player : players) {
            new IgnoreLoaderThread(player.getUniqueId(), this.dataStore.getPlayerData(player.getUniqueId()).ignoredPlayers).start();
        }

        Sponge.getGame().getEventManager().registerListeners(this, new BlockEventHandler(dataStore));
        Sponge.getGame().getEventManager().registerListeners(this, new PlayerEventHandler(dataStore, this));
        Sponge.getGame().getEventManager().registerListeners(this, new EntityEventHandler(dataStore));
        Sponge.getGame().getCommandManager().register(this, CommandGriefPrevention.getCommand(), "griefprevention", "gp");
        AddLogEntry("Boot finished.");
    }

    @SuppressWarnings("serial")
    public void loadConfig() {
        try {
            Files.createDirectories(DataStore.configFilePath.getParent());
            if (Files.notExists(DataStore.configFilePath)) {
                Files.createFile(DataStore.configFilePath);
            }
            if (Files.notExists(DataStore.messagesFilePath)) {
                Files.createFile(DataStore.messagesFilePath);
            }
            if (Files.notExists(DataStore.bannedWordsFilePath)) {
                Files.createFile(DataStore.bannedWordsFilePath);
            }
            if (Files.notExists(DataStore.softMuteFilePath)) {
                Files.createFile(DataStore.softMuteFilePath);
            }

            this.loader = HoconConfigurationLoader.builder().setPath(DataStore.configFilePath).build();
            this.rootNode = this.loader.load(ConfigurationOptions.defaults()
                    .setSerializers(
                            TypeSerializers.getDefaultSerializers().newChild().registerType(TypeToken.of(IpSet.class), new IpSet.IpSetSerializer()))
                    .setHeader(HEADER));

            Collection<World> worlds = Sponge.getGame().getServer().getWorlds();

            // decide claim mode for each world
            this.config_claims_worldModes = new ConcurrentHashMap<World, ClaimsMode>();
            for (World world : worlds) {
                // is it specified in the config file?
                String configSetting = this.rootNode.getNode("GriefPrevention", "Claims", "Mode", world.getProperties().getWorldName()).getString();
                this.rootNode.getNode("GriefPrevention", "Claims", "Mode", world.getProperties().getWorldName()).setValue(configSetting);
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
                else if (Sponge.getGame().getServer().getDefaultWorld().get().getGameMode() == GameModes.CREATIVE) {
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
                CommentedConfigurationNode configNode = this.rootNode.getNode("GriefPrevention", "PvP", "RulesEnabledInWorld", world.getProperties().getWorldName());
                boolean pvpWorld = true;
                if (!configNode.isVirtual()) {
                    pvpWorld = world.getProperties().isPVPEnabled();
                }
                this.rootNode.getNode("GriefPrevention", "PvP", "RulesEnabledInWorld", world.getProperties().getWorldName()).setValue(pvpWorld);
                this.config_pvp_specifiedWorlds.put(world, pvpWorld);
            }

            // sea level
            this.config_seaLevelOverride = new HashMap<String, Integer>();
            for (World world : worlds) {
                int seaLevelOverride = this.rootNode.getNode("GriefPrevention", "SeaLevelOverrides", world.getProperties().getWorldName()).getInt(-1);
                this.rootNode.getNode("GriefPrevention", "SeaLevelOverrides", world.getProperties().getWorldName()).setValue(seaLevelOverride);
                this.config_seaLevelOverride.put(world.getName(), seaLevelOverride);
            }

            this.config_claims_preventTheft = this.rootNode.getNode("GriefPrevention", "Claims", "PreventTheft").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "Claims", "PreventTheft").setValue(config_claims_preventTheft);
            this.config_claims_protectCreatures = this.rootNode.getNode("GriefPrevention", "Claims", "ProtectCreatures").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "Claims", "ProtectCreatures").setValue(config_claims_protectCreatures);
            this.config_claims_protectFires = this.rootNode.getNode("GriefPrevention", "Claims", "ProtectFires").getBoolean(false);
            this.rootNode.getNode("GriefPrevention", "Claims", "ProtectFires").setValue(config_claims_protectFires);
            this.config_claims_protectHorses = this.rootNode.getNode("GriefPrevention", "Claims", "ProtectHorses").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "Claims", "ProtectHorses").setValue(config_claims_protectHorses);
            this.config_claims_preventButtonsSwitches = this.rootNode.getNode("GriefPrevention", "Claims", "PreventButtonsSwitches").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "Claims", "PreventButtonsSwitches").setValue(config_claims_preventButtonsSwitches);
            this.config_claims_lockWoodenDoors = this.rootNode.getNode("GriefPrevention", "Claims", "LockWoodenDoors").getBoolean(false);
            this.rootNode.getNode("GriefPrevention", "Claims", "LockWoodenDoors").setValue(config_claims_lockWoodenDoors);
            this.config_claims_lockTrapDoors = this.rootNode.getNode("GriefPrevention", "Claims", "LockTrapDoors").getBoolean(false);
            this.rootNode.getNode("GriefPrevention", "Claims", "LockTrapDoors").setValue(config_claims_lockTrapDoors);
            this.config_claims_lockFenceGates = this.rootNode.getNode("GriefPrevention", "Claims", "LockFenceGates").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "Claims", "LockFenceGates").setValue(config_claims_lockFenceGates);
            this.config_claims_enderPearlsRequireAccessTrust = this.rootNode.getNode("GriefPrevention", "Claims", "EnderPearlsRequireAccessTrust").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "Claims", "EnderPearlsRequireAccessTrust").setValue(config_claims_enderPearlsRequireAccessTrust);
            this.config_claims_initialBlocks = this.rootNode.getNode("GriefPrevention", "Claims", "InitialBlocks").getInt(100);
            this.rootNode.getNode("GriefPrevention", "Claims", "InitialBlocks").setValue(config_claims_initialBlocks);
            this.config_claims_blocksAccruedPerHour = this.rootNode.getNode("GriefPrevention", "Claims", "BlocksAccruedPerHour").getInt(100);
            this.rootNode.getNode("GriefPrevention", "Claims", "BlocksAccruedPerHour").setValue(config_claims_blocksAccruedPerHour);
            this.config_claims_maxAccruedBlocks = this.rootNode.getNode("GriefPrevention", "Claims", "MaxAccruedBlocks").getInt(80000);
            this.rootNode.getNode("GriefPrevention", "Claims", "MaxAccruedBlocks").setValue(config_claims_maxAccruedBlocks);
            this.config_claims_abandonReturnRatio = this.rootNode.getNode("GriefPrevention", "Claims", "AbandonReturnRatio").getDouble(1);
            this.rootNode.getNode("GriefPrevention", "Claims", "AbandonReturnRatio").setValue(config_claims_abandonReturnRatio);
            this.config_claims_automaticClaimsForNewPlayersRadius = this.rootNode.getNode("GriefPrevention", "Claims", "AutomaticNewPlayerClaimsRadius").getInt(4);
            this.rootNode.getNode("GriefPrevention", "Claims", "AutomaticNewPlayerClaimsRadius").setValue(config_claims_automaticClaimsForNewPlayersRadius);
            this.config_claims_claimsExtendIntoGroundDistance = Math.abs(this.rootNode.getNode("GriefPrevention", "Claims", "ExtendIntoGroundDistance").getInt(5));
            this.rootNode.getNode("GriefPrevention", "Claims", "ExtendIntoGroundDistance").setValue(config_claims_claimsExtendIntoGroundDistance);
            this.config_claims_minWidth = this.rootNode.getNode("GriefPrevention", "Claims", "MinimumWidth").getInt(5);
            this.rootNode.getNode("GriefPrevention", "Claims", "MinimumWidth").setValue(config_claims_minWidth);
            this.config_claims_minArea = this.rootNode.getNode("GriefPrevention", "Claims", "MinimumArea").getInt(100);
            this.rootNode.getNode("GriefPrevention", "Claims", "MinimumArea").setValue(config_claims_minArea);
            this.config_claims_maxDepth = this.rootNode.getNode("GriefPrevention", "Claims", "MaximumDepth").getInt(0);
            this.rootNode.getNode("GriefPrevention", "Claims", "MaximumDepth").setValue(config_claims_maxDepth);
            this.config_claims_chestClaimExpirationDays = this.rootNode.getNode("GriefPrevention", "Claims", "Expiration", "ChestClaimDays").getInt(7);
            this.rootNode.getNode("GriefPrevention", "Claims", "Expiration", "ChestClaimDays").setValue(config_claims_chestClaimExpirationDays);
            this.config_claims_unusedClaimExpirationDays = this.rootNode.getNode("GriefPrevention", "Claims", "Expiration", "UnusedClaimDays").getInt(14);
            this.rootNode.getNode("GriefPrevention", "Claims", "Expiration", "UnusedClaimDays").setValue(config_claims_unusedClaimExpirationDays);
            this.config_claims_expirationDays = this.rootNode.getNode("GriefPrevention", "Claims", "Expiration", "AllClaimDays").getInt(0);
            this.rootNode.getNode("GriefPrevention", "Claims", "Expiration", "AllClaimDays").setValue(config_claims_expirationDays);
            this.config_claims_survivalAutoNatureRestoration = this.rootNode.getNode("GriefPrevention", "Claims", "Expiration", "AutomaticNatureRestoration", "SurvivalWorlds").getBoolean(false);
            this.rootNode.getNode("GriefPrevention", "Claims", "Expiration", "AutomaticNatureRestoration", "SurvivalWorlds").setValue(config_claims_survivalAutoNatureRestoration);
            this.config_claims_maxClaimsPerPlayer = this.rootNode.getNode("GriefPrevention", "Claims", "MaximumNumberOfClaimsPerPlayer").getInt(0);
            this.rootNode.getNode("GriefPrevention", "Claims", "MaximumNumberOfClaimsPerPlayer").setValue(config_claims_maxClaimsPerPlayer);
            this.config_claims_respectWorldGuard = this.rootNode.getNode("GriefPrevention", "Claims", "CreationRequiresWorldGuardBuildPermission").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "Claims", "CreationRequiresWorldGuardBuildPermission").setValue(config_claims_respectWorldGuard);
            this.config_claims_portalsRequirePermission = this.rootNode.getNode("GriefPrevention", "Claims", "PortalGenerationRequiresPermission").getBoolean(false);
            this.rootNode.getNode("GriefPrevention", "Claims", "PortalGenerationRequiresPermission").setValue(config_claims_portalsRequirePermission);
            this.config_claims_villagerTradingRequiresTrust = this.rootNode.getNode("GriefPrevention", "Claims", "VillagerTradingRequiresPermission").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "Claims", "VillagerTradingRequiresPermission").setValue(config_claims_villagerTradingRequiresTrust);
            String accessTrustSlashCommands = this.rootNode.getNode("GriefPrevention", "Claims", "CommandsRequiringAccessTrust").getString("/sethome");
            this.rootNode.getNode("GriefPrevention", "Claims", "CommandsRequiringAccessTrust").setValue(accessTrustSlashCommands);
            this.config_claims_supplyPlayerManual = this.rootNode.getNode("GriefPrevention", "Claims", "DeliverManuals").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "Claims", "DeliverManuals").setValue(config_claims_supplyPlayerManual);

            this.config_spam_enabled = this.rootNode.getNode("GriefPrevention", "Spam", "Enabled").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "Spam", "Enabled").setValue(config_spam_enabled);
            this.config_spam_loginCooldownSeconds = this.rootNode.getNode("GriefPrevention", "Spam", "LoginCooldownSeconds").getInt(60);
            this.rootNode.getNode("GriefPrevention", "Spam", "LoginCooldownSeconds").setValue(config_spam_loginCooldownSeconds);
            this.config_spam_warningMessage = this.rootNode.getNode("GriefPrevention", "Spam", "WarningMessage").getString("Please reduce your noise level.  Spammers will be banned.");
            this.rootNode.getNode("GriefPrevention", "Spam", "WarningMessage").setValue(config_spam_warningMessage);
            this.config_spam_allowedIpAddresses = this.rootNode.getNode("GriefPrevention", "Spam", "AllowedIpAddresses").getString("1.2.3.4; 5.6.7.8");
            this.rootNode.getNode("GriefPrevention", "Spam", "AllowedIpAddresses").setValue(config_spam_allowedIpAddresses);
            this.config_spam_banOffenders = this.rootNode.getNode("GriefPrevention", "Spam", "BanOffenders").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "Spam", "BanOffenders").setValue(config_spam_banOffenders);
            this.config_spam_banMessage = this.rootNode.getNode("GriefPrevention", "Spam", "BanMessage").getString("Banned for spam.");
            this.rootNode.getNode("GriefPrevention", "Spam", "BanMessage").setValue(config_spam_banMessage);
            String slashCommandsToMonitor = this.rootNode.getNode("GriefPrevention", "Spam", "MonitorSlashCommands").getString("/me;/tell;/global;/local;/w;/msg;/r;/t");
            this.rootNode.getNode("GriefPrevention", "Spam", "MonitorSlashCommands").setValue(slashCommandsToMonitor);
            this.config_spam_deathMessageCooldownSeconds = this.rootNode.getNode("GriefPrevention", "Spam", "DeathMessageCooldownSeconds").getInt(60);
            this.rootNode.getNode("GriefPrevention", "Spam", "DeathMessageCooldownSeconds").setValue(config_spam_deathMessageCooldownSeconds);

            this.config_pvp_protectFreshSpawns = this.rootNode.getNode("GriefPrevention", "PvP", "ProtectFreshSpawns").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "PvP", "ProtectFreshSpawns").setValue(config_pvp_protectFreshSpawns);
            this.config_pvp_punishLogout = this.rootNode.getNode("GriefPrevention", "PvP", "PunishLogout").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "PvP", "PunishLogout").setValue(config_pvp_punishLogout);
            this.config_pvp_combatTimeoutSeconds = this.rootNode.getNode("GriefPrevention", "PvP", "CombatTimeoutSeconds").getInt(15);
            this.rootNode.getNode("GriefPrevention", "PvP", "CombatTimeoutSeconds").setValue(config_pvp_combatTimeoutSeconds);
            this.config_pvp_allowCombatItemDrop = this.rootNode.getNode("GriefPrevention", "PvP", "AllowCombatItemDrop").getBoolean(false);
            this.rootNode.getNode("GriefPrevention", "PvP", "AllowCombatItemDrop").setValue(config_pvp_allowCombatItemDrop);
            String bannedPvPCommandsList = this.rootNode.getNode("GriefPrevention", "PvP", "BlockedSlashCommands").getString("/home;/vanish;/spawn;/tpa");
            this.rootNode.getNode("GriefPrevention", "PvP", "BlockedSlashCommands").setValue(bannedPvPCommandsList);

            this.config_economy_claimBlocksPurchaseCost = this.rootNode.getNode("GriefPrevention", "Economy", "ClaimBlocksPurchaseCost").getDouble(0);
            this.rootNode.getNode("GriefPrevention", "Economy", "ClaimBlocksPurchaseCost").setValue(config_economy_claimBlocksPurchaseCost);
            this.config_economy_claimBlocksSellValue = this.rootNode.getNode("GriefPrevention", "Economy", "ClaimBlocksSellValue").getDouble(0);
            this.rootNode.getNode("GriefPrevention", "Economy", "ClaimBlocksSellValue").setValue(config_economy_claimBlocksSellValue);

            this.config_lockDeathDropsInPvpWorlds = this.rootNode.getNode("GriefPrevention", "ProtectItemsDroppedOnDeath", "PvPWorlds").getBoolean(false);
            this.rootNode.getNode("GriefPrevention", "ProtectItemsDroppedOnDeath", "PvPWorlds").setValue(config_lockDeathDropsInPvpWorlds);
            this.config_lockDeathDropsInNonPvpWorlds = this.rootNode.getNode("GriefPrevention", "ProtectItemsDroppedOnDeath", "NonPvPWorlds").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "ProtectItemsDroppedOnDeath", "NonPvPWorlds").setValue(config_lockDeathDropsInNonPvpWorlds);

            this.config_blockClaimExplosions = this.rootNode.getNode("GriefPrevention", "BlockLandClaimExplosions").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "BlockLandClaimExplosions").setValue(config_blockClaimExplosions);
            this.config_blockSurfaceCreeperExplosions = this.rootNode.getNode("GriefPrevention", "BlockSurfaceCreeperExplosions").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "BlockSurfaceCreeperExplosions").setValue(config_blockSurfaceCreeperExplosions);
            this.config_blockSurfaceOtherExplosions = this.rootNode.getNode("GriefPrevention", "BlockSurfaceOtherExplosions").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "BlockSurfaceOtherExplosions").setValue(config_blockSurfaceOtherExplosions);
            this.config_blockSkyTrees = this.rootNode.getNode("GriefPrevention", "LimitSkyTrees").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "LimitSkyTrees").setValue(config_blockSkyTrees);
            this.config_limitTreeGrowth = this.rootNode.getNode("GriefPrevention", "LimitTreeGrowth").getBoolean(false);
            this.rootNode.getNode("GriefPrevention", "LimitTreeGrowth").setValue(config_limitTreeGrowth);
            this.config_pistonsInClaimsOnly = this.rootNode.getNode("GriefPrevention", "LimitPistonsToLandClaims").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "LimitPistonsToLandClaims").setValue(config_pistonsInClaimsOnly);

            this.config_fireSpreads = this.rootNode.getNode("GriefPrevention", "FireSpreads").getBoolean(false);
            this.rootNode.getNode("GriefPrevention", "FireSpreads").setValue(config_fireSpreads);
            this.config_fireDestroys = this.rootNode.getNode("GriefPrevention", "FireDestroys").getBoolean(false);
            this.rootNode.getNode("GriefPrevention", "FireDestroys").setValue(config_fireDestroys);

            this.config_whisperNotifications = this.rootNode.getNode("GriefPrevention", "AdminsGetWhispers").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "AdminsGetWhispers").setValue(config_whisperNotifications);
            this.config_signNotifications = this.rootNode.getNode("GriefPrevention", "AdminsGetSignNotifications").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "AdminsGetSignNotifications").setValue(config_signNotifications);
            String whisperCommandsToMonitor = this.rootNode.getNode("GriefPrevention", "WhisperCommands").getString("/tell;/pm;/r;/w;/whisper;/t;/msg");
            this.rootNode.getNode("GriefPrevention", "WhisperCommands").setValue(whisperCommandsToMonitor);

            this.config_smartBan = this.rootNode.getNode("GriefPrevention", "SmartBan").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "SmartBan").setValue(config_smartBan);
            this.config_ipLimit = this.rootNode.getNode("GriefPrevention", "MaxPlayersPerIpAddress").getInt(3);
            this.rootNode.getNode("GriefPrevention", "MaxPlayersPerIpAddress").setValue(config_ipLimit);

            this.config_endermenMoveBlocks = this.rootNode.getNode("GriefPrevention", "EndermenMoveBlocks").getBoolean(false);
            this.rootNode.getNode("GriefPrevention", "EndermenMoveBlocks").setValue(config_endermenMoveBlocks);
            this.config_silverfishBreakBlocks = this.rootNode.getNode("GriefPrevention", "SilverfishBreakBlocks").getBoolean(false);
            this.rootNode.getNode("GriefPrevention", "SilverfishBreakBlocks").setValue(config_silverfishBreakBlocks);
            this.config_creaturesTrampleCrops = this.rootNode.getNode("GriefPrevention", "creaturesTrampleCrops").getBoolean(false);
            this.rootNode.getNode("GriefPrevention", "creaturesTrampleCrops").setValue(config_creaturesTrampleCrops);
            this.config_zombiesBreakDoors = this.rootNode.getNode("GriefPrevention", "HardModeZombiesBreakDoors").getBoolean(false);
            this.rootNode.getNode("GriefPrevention", "HardModeZombiesBreakDoors").setValue(config_zombiesBreakDoors);

            this.config_mods_ignoreClaimsAccounts =
                    this.rootNode.getNode("GriefPrevention", "Mods", "PlayersIgnoringAllClaims").getList(new TypeToken<String>() {});
            this.rootNode.getNode("GriefPrevention", "Mods", "PlayersIgnoringAllClaims").setValue(config_mods_ignoreClaimsAccounts);

            if (this.config_mods_ignoreClaimsAccounts == null)
                this.config_mods_ignoreClaimsAccounts = new ArrayList<>();

            this.config_mods_accessTrustIds = new ArrayList<ItemInfo>();
            List<String> accessTrustStrings =
                    this.rootNode.getNode("GriefPrevention", "Mods", "BlockIdsRequiringAccessTrust").getList(new TypeToken<String>() {
                    });
            this.rootNode.getNode("GriefPrevention", "Mods", "BlockIdsRequiringAccessTrust").setValue(accessTrustStrings);

            this.parseBlockIdListFromConfig(accessTrustStrings, this.config_mods_accessTrustIds);

            this.config_mods_containerTrustIds = new ArrayList<ItemInfo>();
            List<String> containerTrustStrings =
                    this.rootNode.getNode("GriefPrevention", "Mods", "BlockIdsRequiringContainerTrust").getList(new TypeToken<String>() {
                    });
            this.rootNode.getNode("GriefPrevention", "Mods", "BlockIdsRequiringContainerTrust").setValue(containerTrustStrings);

            // default values for container trust mod blocks
            // TODO
            /*if (containerTrustStrings == null || containerTrustStrings.size() == 0) {
                containerTrustStrings = Lists.newArrayList();
                containerTrustStrings.add(new MaterialInfo(99999, "Example - ID 99999, all data values.").toString());
            }*/

            // parse the strings from the config file
            this.parseBlockIdListFromConfig(containerTrustStrings, this.config_mods_containerTrustIds);

            this.config_mods_explodableIds = new ArrayList<ItemInfo>();
            List<String> explodableStrings = this.rootNode.getNode("GriefPrevention", "Mods", "BlockIdsExplodable").getList(new TypeToken<String>() {
            });
            this.rootNode.getNode("GriefPrevention", "Mods", "BlockIdsExplodable").setValue(explodableStrings);

            // parse the strings from the config file
            this.parseBlockIdListFromConfig(explodableStrings, this.config_mods_explodableIds);

            // default for claim investigation tool
            String investigationToolMaterialName = ItemTypes.STICK.getName();

            // get investigation tool from config
            investigationToolMaterialName =
                    this.rootNode.getNode("GriefPrevention", "Claims", "InvestigationTool").getString(investigationToolMaterialName);
            this.rootNode.getNode("GriefPrevention", "Claims", "InvestigationTool").setValue(investigationToolMaterialName);

            // validate investigation tool
            Optional<ItemType> investigationTool = Sponge.getGame().getRegistry().getType(ItemType.class, investigationToolMaterialName);
            if (!investigationTool.isPresent()) {
                GriefPrevention.AddLogEntry(
                        "ERROR: Material " + investigationToolMaterialName + " not found.  Defaulting to the stick.  Please update your config.hocon.");
                this.config_claims_investigationTool = ItemTypes.STICK;
            } else {
                this.config_claims_investigationTool = investigationTool.get();
            }

            // default for claim creation/modification tool
            String modificationToolMaterialName = ItemTypes.GOLDEN_SHOVEL.getId();

            // get modification tool from config
            modificationToolMaterialName = this.rootNode.getNode("GriefPrevention", "Claims", "ModificationTool").getString(modificationToolMaterialName);
            this.rootNode.getNode("GriefPrevention", "Claims", "ModificationTool").setValue(modificationToolMaterialName);

            // validate modification tool
            this.config_claims_modificationTool = null;
            Optional<ItemType> modificationTool = Sponge.getGame().getRegistry().getType(ItemType.class, modificationToolMaterialName);
            if (!modificationTool.isPresent()) {
                GriefPrevention.AddLogEntry("ERROR: Material " + modificationToolMaterialName
                        + " not found.  Defaulting to the golden shovel.  Please update your config.hocon.");
                this.config_claims_modificationTool = ItemTypes.GOLDEN_SHOVEL;
            } else {
                this.config_claims_modificationTool = modificationTool.get();
            }

            // default for siege worlds list
            ArrayList<String> defaultSiegeWorldNames = new ArrayList<>();

            // get siege world names from the config file
            List<String> siegeEnabledWorldNames = this.rootNode.getNode("GriefPrevention", "Siege", "Worlds").getList(new TypeToken<String>() {
            });
            if (siegeEnabledWorldNames == null || siegeEnabledWorldNames.size() == 0) {
                siegeEnabledWorldNames = defaultSiegeWorldNames;
            }
            this.rootNode.getNode("GriefPrevention", "Siege", "Worlds").setValue(siegeEnabledWorldNames);

            // validate that list
            this.config_siege_enabledWorlds = new ArrayList<World>();
            for (int i = 0; i < siegeEnabledWorldNames.size(); i++) {
                String worldName = siegeEnabledWorldNames.get(i);
                Optional<World> world = Sponge.getGame().getServer().getWorld(worldName);
                if (!world.isPresent()) {
                    AddLogEntry("Error: Siege Configuration: There's no world \"" + worldName + "\".  Please update your config.hocon.");
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
            List<String> breakableBlocksList = this.rootNode.getNode("GriefPrevention", "Siege", "BreakableBlocks").getList(new TypeToken<String>() {
            });
            // if it fails, use default list instead
            if (breakableBlocksList == null || breakableBlocksList.size() == 0) {
                breakableBlocksList = defaultBreakableBlocksList;
            }
            this.rootNode.getNode("GriefPrevention", "Siege", "BreakableBlocks").setValue(breakableBlocksList);


            // parse the list of siege-breakable blocks
            this.config_siege_blocks = new ArrayList<BlockType>();
            for (int i = 0; i < breakableBlocksList.size(); i++) {
                String blockName = breakableBlocksList.get(i);
                Optional<BlockType> material = Sponge.getGame().getRegistry().getType(BlockType.class, blockName);
                if (!material.isPresent()) {
                    GriefPrevention.AddLogEntry("Siege Configuration: Material not found: " + blockName + ".");
                } else {
                    this.config_siege_blocks.add(material.get());
                }
            }

            this.config_pvp_noCombatInPlayerLandClaims = this.rootNode.getNode("GriefPrevention", "PvP", "ProtectPlayersInLandClaims", "PlayerOwnedClaims")
                    .getBoolean(this.config_siege_enabledWorlds.size() == 0);
            this.rootNode.getNode("GriefPrevention", "PvP", "ProtectPlayersInLandClaims", "PlayerOwnedClaims").setValue(config_pvp_noCombatInPlayerLandClaims);
            this.config_pvp_noCombatInAdminLandClaims =
                    this.rootNode.getNode("GriefPrevention", "PvP", "ProtectPlayersInLandClaims", "AdministrativeClaims")
                            .getBoolean(this.config_siege_enabledWorlds.size() == 0);
            this.rootNode.getNode("GriefPrevention", "PvP", "ProtectPlayersInLandClaims", "AdministrativeClaims").setValue(config_pvp_noCombatInAdminLandClaims);
            this.config_pvp_noCombatInAdminSubdivisions =
                    this.rootNode.getNode("GriefPrevention", "PvP", "ProtectPlayersInLandClaims", "AdministrativeSubdivisions")
                            .getBoolean(this.config_siege_enabledWorlds.size() == 0);
            this.rootNode.getNode("GriefPrevention", "PvP", "ProtectPlayersInLandClaims", "AdministrativeSubdivisions").setValue(config_pvp_noCombatInAdminSubdivisions);

            // optional database settings
            this.databaseUrl = this.rootNode.getNode("GriefPrevention", "Database", "URL").getString("");
            this.rootNode.getNode("GriefPrevention", "Database", "URL").setValue(databaseUrl);
            this.databaseUserName = this.rootNode.getNode("GriefPrevention", "Database", "UserName").getString("");
            this.rootNode.getNode("GriefPrevention", "Database", "UserName").setValue(databaseUserName);
            this.databasePassword = this.rootNode.getNode("GriefPrevention", "Database", "Password").getString("");
            this.rootNode.getNode("GriefPrevention", "Database", "Password").setValue(databasePassword);

            // custom logger settings
            this.config_logs_daysToKeep = this.rootNode.getNode("GriefPrevention", "Abridged Logs", "Days To Keep").getInt(7);
            this.rootNode.getNode("GriefPrevention", "Abridged Logs", "Days To Keep").setValue(config_logs_daysToKeep);
            this.config_logs_socialEnabled =
                    this.rootNode.getNode("GriefPrevention", "Abridged Logs", "Included Entry Types", "Social Activity").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "Abridged Logs", "Included Entry Types", "Social Activity").setValue(config_logs_socialEnabled);
            this.config_logs_suspiciousEnabled =
                    this.rootNode.getNode("GriefPrevention", "Abridged Logs", "Included Entry Types", "Suspicious Activity").getBoolean(true);
            this.rootNode.getNode("GriefPrevention", "Abridged Logs", "Included Entry Types", "Suspicious Activity").setValue(config_logs_suspiciousEnabled);
            this.config_logs_adminEnabled =
                    this.rootNode.getNode("GriefPrevention", "Abridged Logs", "Included Entry Types", "Administrative Activity").getBoolean(false);
            this.rootNode.getNode("GriefPrevention", "Abridged Logs", "Included Entry Types", "Administrative Activity").setValue(config_logs_adminEnabled);
            this.config_logs_debugEnabled = this.rootNode.getNode("GriefPrevention", "Abridged Logs", "Included Entry Types", "Debug").getBoolean(false);
            this.rootNode.getNode("GriefPrevention", "Abridged Logs", "Included Entry Types", "Debug").setValue(config_logs_debugEnabled);

            // claims mode by world
            for (World world : this.config_claims_worldModes.keySet()) {
                this.rootNode.getNode("GriefPrevention", "Claims", "Mode", world.getProperties().getWorldName()).setValue(this.config_claims_worldModes.get(world).name());
            }

            try {
                this.loader.save(this.rootNode);
            } catch (IOException exception) {
                exception.printStackTrace();
                AddLogEntry("Unable to write to the configuration file at \"" + DataStore.configFilePath + "\"");
            }

            // try to parse the list of commands requiring access trust in land claims
            this.config_claims_commandsRequiringAccessTrust = new ArrayList<String>();
            String[] commands = accessTrustSlashCommands.split(";");
            for (int i = 0; i < commands.length; i++) {
                if (!commands[i].isEmpty()) {
                    this.config_claims_commandsRequiringAccessTrust.add(commands[i].trim().toLowerCase());
                }
            }

            // try to parse the list of commands which should be monitored for spam
            this.config_spam_monitorSlashCommands = new ArrayList<String>();
            commands = slashCommandsToMonitor.split(";");
            for (int i = 0; i < commands.length; i++) {
                this.config_spam_monitorSlashCommands.add(commands[i].trim());
            }

            // try to parse the list of commands which should be included in eavesdropping
            this.config_eavesdrop_whisperCommands = new ArrayList<String>();
            commands = whisperCommandsToMonitor.split(";");
            for (int i = 0; i < commands.length; i++) {
                this.config_eavesdrop_whisperCommands.add(commands[i].trim());
            }

            // try to parse the list of commands which should be banned during pvp combat
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

    public static Player checkPlayer(CommandSource source) throws CommandException {
        if (source instanceof Player) {
            return ((Player) source);
        } else {
            throw new CommandException(Texts.of("You must be a player to run this command!"));
        }
    }

    // handles sub commands
    public HashMap<List<String>, CommandSpec> registerSubCommands() {
        HashMap<List<String>, CommandSpec> subcommands = new HashMap<List<String>, CommandSpec>();

        subcommands.put(Arrays.asList("abandonclaim", "unclaim", "declaim", "removeclaim", "disclaim"), CommandSpec.builder()
                .description(Texts.of("Deletes a claim"))
                .permission("griefprevention.claims")
                .executor(new CommandAbandonClaim(false))
                .build());
        
        subcommands.put(Arrays.asList("abandontoplevelclaim"), CommandSpec.builder()
                .description(Texts.of("Deletes a claim and all its subdivisions"))
                .permission("griefprevention.claims")
                .executor(new CommandAbandonClaim(true))
                .build());
        
        subcommands.put(Arrays.asList("ignoreclaims", "ic"), CommandSpec.builder()
                .description(Texts.of("Toggles ignore claims mode"))
                .permission("griefprevention.ignoreclaims")
                .executor(new CommandIgnoreClaim())
                .build());
        
        subcommands.put(Arrays.asList("flag"), CommandSpec.builder()
                .description(Texts.of("Gets/Sets various claim flags in the claim you are standing in"))
                .permission("griefprevention.commands.flag")
                .child(CommandSpec.builder()
                        .arguments(GenericArguments.choices(Texts.of("state"), FLAG_BOOLEANS))
                        .executor((src, args) -> {
                            boolean state = args.<Boolean>getOne("state").get();
                            return CommandResult.success();
                        })
                        .build(), "spawn-mobs")
                .build());
        
        subcommands.put(Arrays.asList("abandonallclaims"), CommandSpec.builder()
                .description(Texts.of("Deletes ALL your claims"))
                .permission("griefprevention.claims")
                .executor(new CommandAbandonAllClaims())
                .build());

        subcommands.put(Arrays.asList("restorenature", "rn"), CommandSpec.builder()
                .description(Texts.of("Switches the shovel tool to restoration mode"))
                .permission("griefprevention.restorenature")
                .executor(new CommandRestoreNature())
                .build());
        
        subcommands.put(Arrays.asList("restorenatureaggressive", "rna"), CommandSpec.builder()
                .description(Texts.of("Switches the shovel tool to aggressive restoration mode"))
                .permission("griefprevention.restorenatureaggressive")
                .executor(new CommandRestoreNatureAggressive())
                .build());

        subcommands.put(Arrays.asList("restorenaturefill", "rnf"), CommandSpec.builder()
                .description(Texts.of("Switches the shovel tool to fill mode"))
                .permission("griefprotection.restorenaturefill")
                .arguments(optional(integer(Texts.of("radius")), 2))
                .executor(new CommandRestoreNatureFill())
                .build());
        
        subcommands.put(Arrays.asList("trust", "tr"), CommandSpec.builder()
                .description(Texts.of("Grants a player full access to your claim(s)"))
                .extendedDescription(Texts.of("Grants a player full access to your claim(s).\n"
                        + "See also /untrust, /containertrust, /accesstrust, and /permissiontrust."))
                .permission("griefprevention.claims")
                .arguments(string(Texts.of("subject")))
                .executor(new CommandTrust())
                .build());

        subcommands.put(Arrays.asList("transferclaim", "giveclaim"), CommandSpec.builder()
                .description(Texts.of("Converts an administrative claim to a private claim"))
                .arguments(optional(player(Texts.of("target"))))
                .permission("griefprevention.transferclaim")
                .executor(new CommandTransferClaim())
                .build());

        subcommands.put(Arrays.asList("trustlist"), CommandSpec.builder()
                .description(Texts.of("Lists permissions for the claim you're standing in"))
                .permission("griefprevention.claimls")
                .executor(new CommandTrustList())
                .build());

        subcommands.put(Arrays.asList("untrust", "ut"), CommandSpec.builder()
                .description(Texts.of("Revokes a player's access to your claim(s)"))
                .permission("griefprevention.claims")
                .arguments(string(Texts.of("subject")))
                .executor(new CommandUntrust())
                .build());

        subcommands.put(Arrays.asList("accesstrust", "at"), CommandSpec.builder()
                .description(Texts.of("Grants a player entry to your claim(s) and use of your bed"))
                .permission("griefprevention.claims")
                .arguments(string(Texts.of("target")))
                .executor(new CommandAccessTrust())
                .build());

        subcommands.put(Arrays.asList("containertrust", "ct"), CommandSpec.builder()
                .description(Texts.of("Grants a player access to your claim's containers, crops, animals, bed, buttons, and levers"))
                .permission("griefprevention.claims")
                .arguments(string(Texts.of("target")))
                .executor(new CommandContainerTrust())
                .build());

        subcommands.put(Arrays.asList("permissiontrust", "pt"), CommandSpec.builder()
                .description(Texts.of("Grants a player permission to grant their level of permission to others"))
                .permission("griefprevention.claims")
                .arguments(string(Texts.of("target")))
                .executor(new CommandPermissionTrust())
                .build());

        subcommands.put(Arrays.asList("buyclaimblocks", "buyclaim"), CommandSpec.builder()
                .description(Texts.of("Purchases additional claim blocks with server money. Doesn't work on servers without a vault-compatible "
                        + "economy plugin"))
                .permission("griefprevention.buysellclaimblocks")
                .arguments(optional(integer(Texts.of("numberOfBlocks"))))
                .executor(new CommandBuyClaimBlocks())
                .build());
        
        subcommands.put(Arrays.asList("sellclaimblocks", "sellclaim"), CommandSpec.builder()
                .description(Texts.of("Sell your claim blocks for server money. Doesn't work on servers without a vault-compatible "
                        + "economy plugin"))
                .permission("griefprevention.buysellclaimblocks")
                .arguments(optional(integer(Texts.of("numberOfBlocks"))))
                .executor(new CommandSellClaimBlocks())
                .build());

        subcommands.put(Arrays.asList("adminclaims", "ac"), CommandSpec.builder()
                .description(Texts.of("Switches the shovel tool to administrative claims mode"))
                .permission("griefprevention.adminclaims")
                .executor(new CommandAdminClaims())
                .build());
        
        subcommands.put(Arrays.asList("basicclaims", "bc"), CommandSpec.builder()
                .description(Texts.of("Switches the shovel tool back to basic claims mode"))
                .permission("griefprevention.claims")
                .executor(new CommandBasicClaims())
                .build());
        
        subcommands.put(Arrays.asList("subdivideclaims", "sc", "subdivideclaim"), CommandSpec.builder()
                .description(Texts.of("Switches the shovel tool to subdivision mode, used to subdivide your claims"))
                .permission("griefprevention.claims")
                .executor(new CommandSubdivideClaims())
                .build());
        
        subcommands.put(Arrays.asList("deleteclaim", "dc"), CommandSpec.builder()
                .description(Texts.of("Deletes the claim you're standing in, even if it's not your claim"))
                .permission("griefprevention.deleteclaims")
                .executor(new CommandDeleteClaim())
                .build());
        
        subcommands.put(Arrays.asList("claimexplosions"), CommandSpec.builder()
                .description(Texts.of("Toggles whether explosives may be used in a specific land claim"))
                .permission("griefprevention.claims")
                .executor(new CommandClaimExplosions())
                .build());
        
        subcommands.put(Arrays.asList("deleteallclaims"), CommandSpec.builder()
                .description(Texts.of("Delete all of another player's claims"))
                .permission("griefprevention.deleteclaims")
                .arguments(player(Texts.of("player"))) // TODO: Use user commandelement when added
                .executor(new CommandDeleteAllClaims())
                .build());

        subcommands.put(Arrays.asList("claimbook"), CommandSpec.builder()
                .description(Texts.of("Gives a player a manual about claiming land"))
                .permission("griefprevention.claimbook")
                .arguments(playerOrSource(Texts.of("player")))
                .executor(new CommandClaimBook())
                .build());

        subcommands.put(Arrays.asList("claimslist", "claimlist", "listclaims"), CommandSpec.builder()
                .description(Texts.of("List information about a player's claim blocks and claims"))
                .arguments(onlyOne(playerOrSource(Texts.of("player"))))
                .executor(new CommandClaimsList())
                .build());
        
        subcommands.put(Arrays.asList("adminclaimslist"), CommandSpec.builder()
                .description(Texts.of("List all administrative claims"))
                .permission("griefprevention.adminclaims")
                .executor(new CommandAdminClaimsList())
                .build());

        subcommands.put(Arrays.asList("unlockdrops"), CommandSpec.builder()
                .description(Texts.of("Allows other players to pick up the items you dropped when you died"))
                .executor(new CommandUnlockDrops())
                .build());

        subcommands.put(Arrays.asList("deletealladminclaims"), CommandSpec.builder()
                .description(Texts.of("Deletes all administrative claims"))
                .permission("griefprevention.adminclaims")
                .executor(new CommandDeleteAllAdminClaims())
                .build());

        subcommands.put(Arrays.asList("adjustbonusclaimblocks", "acb"), CommandSpec.builder()
                .description(Texts.of("Adds or subtracts bonus claim blocks for a player"))
                .permission("griefprevention.adjustclaimblocks")
                .arguments(string(Texts.of("player")), integer(Texts.of("amount")))
                .executor(new CommandAdjustBonusClaimBlocks())
                .build());
        
        subcommands.put(Arrays.asList("setaccruedclaimblocks", "scb"), CommandSpec.builder()
                .description(Texts.of("Updates a player's accrued claim block total"))
                .permission("griefprevention.adjustclaimblocks")
                .arguments(string(Texts.of("player")), integer(Texts.of("amount")))
                .executor(new CommandSetAccruedClaimBlocks())
                .build());
        
        subcommands.put(Arrays.asList("trapped"), CommandSpec.builder()
                .description(Texts.of("Ejects you to nearby unclaimed land. Has a substantial cooldown period"))
                .executor(new CommandTrapped())
                .build());

        subcommands.put(Arrays.asList("siege"), CommandSpec.builder()
                .description(Texts.of("Initiates a siege versus another player"))
                .arguments(optional(onlyOne(player(Texts.of("playerName")))))
                .executor(new CommandSiege())
                .build());

        subcommands.put(Arrays.asList("softmute"), CommandSpec.builder()
                .description(Texts.of("Toggles whether a player's messages will only reach other soft-muted players"))
                .permission("griefprevention.softmute")
                .arguments(onlyOne(player(Texts.of("player"))))
                .executor(new CommandSoftMute())
                .build());

        subcommands.put(Arrays.asList("gpreload"), CommandSpec.builder()
                .description(Texts.of("Reloads Grief Prevention's configuration settings"))
                .permission("griefprevention.reload")
                .executor(new CommandGpReload())
                .build());
        
        subcommands.put(Arrays.asList("givepet"), CommandSpec.builder()
                .description(Texts.of("Allows a player to give away a pet they tamed"))
                .permission("griefprevention.givepet")
                .arguments(GenericArguments.firstParsing(GenericArguments.literal(Texts.of("player"), "cancel"), player(Texts.of("player"))))
                .executor(new CommandGivePet())
                .build());
        
        subcommands.put(Arrays.asList("gpblockinfo"), CommandSpec.builder()
                .description(Texts.of("Allows an administrator to get technical information about blocks in the world and items in hand"))
                .permission("griefprevention.gpblockinfo")
                .executor(new CommandGpBlockInfo())
                .build());

        subcommands.put(Arrays.asList("ignoreplayer", "ignore"), CommandSpec.builder()
                .description(Texts.of("Ignores another player's chat messages"))
                .permission("griefprevention.ignore")
                .arguments(onlyOne(player(Texts.of("player"))))
                .executor(new CommandIgnorePlayer())
                .build());

        subcommands.put(Arrays.asList("unignoreplayer", "unignore"), CommandSpec.builder()
                .description(Texts.of("Unignores another player's chat messages"))
                .permission("griefprevention.ignore")
                .arguments(onlyOne(player(Texts.of("player"))))
                .executor(new CommandUnignorePlayer())
                .build());

        subcommands.put(Arrays.asList("ignoredplayerlist", "ignores", "ignored", "ignoredlist", "listignores", "listignored", "ignoring"), 
                CommandSpec.builder()
                    .description(Texts.of("Lists the players you're ignoring in chat"))
                    .permission("griefprevention.ignore")
                    .executor(new CommandIgnoredPlayerList())
                    .build());

        subcommands.put(Arrays.asList("separate"), CommandSpec.builder()
                .description(Texts.of("Forces two players to ignore each other in chat"))
                .permission("griefprevention.separate")
                .arguments(onlyOne(player(Texts.of("player1"))), onlyOne(player(Texts.of("player2"))))
                .executor(new CommandSeparate())
                .build());

        subcommands.put(Arrays.asList("unseparate"), CommandSpec.builder()
                .description(Texts.of("Reverses /separate"))
                .permission("griefprevention.separate")
                .arguments(onlyOne(player(Texts.of("player1"))), onlyOne(player(Texts.of("player2"))))
                .executor(new CommandUnseparate())
                .build());
        
        return subcommands;
    }

    public void setIgnoreStatus(User ignorer, User ignoree, IgnoreMode mode) {
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

    public enum IgnoreMode {
        None, StandardIgnore, AdminIgnore
    }

    public String trustEntryToPlayerName(String entry) {
        if (entry.startsWith("[") || entry.equals("public")) {
            return entry;
        } else {
            return GriefPrevention.lookupPlayerName(entry);
        }
    }

    public static String getfriendlyLocationString(Location<World> location) {
        return location.getExtent().getName() + ": x" + location.getBlockX() + ", z" + location.getBlockZ();
    }

    public Optional<User> resolvePlayerByName(String name) {
        // try online players first
        Optional<Player> targetPlayer = Sponge.getGame().getServer().getPlayer(name);
        if (targetPlayer.isPresent())
            return Optional.of((User) targetPlayer.get());

        Optional<User> user = Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(name);
        if (user.isPresent())
            return user;

        return Optional.empty();
    }

    // string overload for above helper
    static String lookupPlayerName(String uuid) {
        Optional<User> user = Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(UUID.fromString(uuid));
        if (!user.isPresent()) {
            GriefPrevention.AddLogEntry("Error: Tried to look up a local player name for invalid UUID: " + uuid);
            return "someone";
        }

        return user.get().getName();
    }

    public void onDisable() {
        // save data for any online players
        Collection<Player> players = (Collection<Player>) Sponge.getGame().getServer().getOnlinePlayers();
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

    // called when a player spawns, applies protection for that player if necessary
    public void checkPvpProtectionNeeded(Player player) {
        // if anti spawn camping feature is not enabled, do nothing
        if (!this.config_pvp_protectFreshSpawns) {
            return;
        }

        // if pvp is disabled, do nothing
        if (!pvpRulesApply(player.getWorld())) {
            return;
        }

        // if player is in creative mode, do nothing
        if (player.get(Keys.GAME_MODE).get() == GameModes.CREATIVE) {
            return;
        }

        // if the player has the damage any player permission enabled, do nothing
        if (player.hasPermission("griefprevention.nopvpimmunity")) {
            return;
        }

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
            Sponge.getGame().getScheduler().createTaskBuilder().delay(1, TimeUnit.MINUTES).execute(task).submit(this);
        }
    }

    static boolean isInventoryEmpty(Player player) {
        InventoryPlayer inventory = ((EntityPlayerMP) player).inventory;
        for (ItemStack stack : inventory.mainInventory) {
            if (stack != null) {
                return false;
            }
        }
        for (ItemStack stack : inventory.armorInventory) {
            if (stack != null) {
                return false;
            }
        }
        return true;
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

    // ensures a piece of the managed world is loaded into server memory (generates the chunk if necessary)
    private static void GuaranteeChunkLoaded(Location<World> location) {
        location.getExtent().loadChunk(location.getBlockPosition(), true);
    }

    public static Text getMessage(Messages messageID, String... args) {
        return Texts.of(GriefPrevention.instance.dataStore.getMessage(messageID, args));
    }

    // sends a color-coded message to a player
    public static void sendMessage(CommandSource player, TextColor color, Messages messageID, String... args) {
        sendMessage(player, color, messageID, 0, args);
    }

    // sends a color-coded message to a player
    public static void sendMessage(CommandSource player, TextColor color, Messages messageID, long delayInTicks, String... args) {
        sendMessage(player, GriefPrevention.instance.dataStore.parseMessage(messageID, color, args), delayInTicks);
    }

    public static void sendMessage(CommandSource player, TextColor color, String message) {
        sendMessage(player, Texts.of(color, message));
    }

    // sends a color-coded message to a player
    public static void sendMessage(CommandSource player, Text message) {
        if (message == Texts.of() || message == null)
            return;

        if (player == null) {
            GriefPrevention.AddLogEntry(Texts.toPlain(message));
        } else {
            player.sendMessage(message);
        }
    }

    public static void sendMessage(CommandSource player, Text message, long delayInTicks) {
        SendPlayerMessageTask task = new SendPlayerMessageTask((Player) player, message);
        if (delayInTicks > 0) {
            Sponge.getGame().getScheduler().createTaskBuilder().delayTicks(delayInTicks).execute(task).submit(GriefPrevention.instance);
        } else {
            task.run();
        }
    }

    // checks whether players can create claims in a world
    public boolean claimsEnabledForWorld(World world) {
        return this.config_claims_worldModes.get(world) != ClaimsMode.Disabled;
    }

    // determines whether creative anti-grief rules apply at a location
    public boolean creativeRulesApply(Location<World> location) {
        return this.config_claims_worldModes.get((location.getExtent())) == ClaimsMode.Creative;
    }

    public String allowBuild(Player player, Location<World> location) {
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
                // exception: when chest claims are enabled, players who have zero land claims and are placing a chest
                if (!player.getItemInHand().isPresent() || player.getItemInHand().get().getItem() != ItemTypes.CHEST || playerData.getClaims().size() > 0
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
            return claim.allowBuild(player, location.getBlockType());
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
            return claim.allowBreak(player, snapshot.getLocation().get().getBlockType());
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
        Sponge.getGame().getScheduler().createTaskBuilder().async().delayTicks(delayInTicks).execute(task).submit(this);
    }

    private void parseBlockIdListFromConfig(List<String> stringsToParse, List<ItemInfo> blockTypes) {
        // for each string in the list
        for (int i = 0; i < stringsToParse.size(); i++) {
            // try to parse the string value into a material info
            String blockInfo = stringsToParse.get(i);
            // validate block info
            int count = StringUtils.countMatches(blockInfo, ":");
            int meta = -1;
            if (count == 2) {
                // grab meta
                int lastIndex = blockInfo.lastIndexOf(":");
                try {
                    if (blockInfo.length() >= lastIndex + 1) {
                        meta = Integer.parseInt(blockInfo.substring(lastIndex+1, blockInfo.length()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                blockInfo = blockInfo.substring(0, lastIndex);
            } else if (count > 2) {
                GriefPrevention.AddLogEntry("ERROR: Invalid block entry " + blockInfo + " found in config. Skipping...");
                continue;
            }

            Optional<BlockType> blockType = Sponge.getGame().getRegistry().getType(BlockType.class, blockInfo);

            // null value returned indicates an error parsing the string from
            // the config file
            if (!blockType.isPresent() || !blockType.get().getItem().isPresent()) {
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
                blockTypes.add(new ItemInfo(blockType.get().getItem().get(), meta));
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
        Boolean configSetting = this.config_pvp_specifiedWorlds.get(world);
        if (configSetting != null) {
            return configSetting;
        }

        return world.getProperties().isPVPEnabled();
    }
}
