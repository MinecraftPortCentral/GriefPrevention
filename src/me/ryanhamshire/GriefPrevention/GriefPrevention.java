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
import com.google.inject.Inject;
import me.ryanhamshire.GriefPrevention.command.CommandAccessTrust;
import me.ryanhamshire.GriefPrevention.command.CommandAdjustBonusClaimBlocks;
import me.ryanhamshire.GriefPrevention.command.CommandContainerTrust;
import me.ryanhamshire.GriefPrevention.command.CommandGivePet;
import me.ryanhamshire.GriefPrevention.command.CommandGpBlockInfo;
import me.ryanhamshire.GriefPrevention.command.CommandGpReload;
import me.ryanhamshire.GriefPrevention.command.CommandGriefPrevention;
import me.ryanhamshire.GriefPrevention.command.CommandIgnorePlayer;
import me.ryanhamshire.GriefPrevention.command.CommandIgnoredPlayerList;
import me.ryanhamshire.GriefPrevention.command.CommandPermissionTrust;
import me.ryanhamshire.GriefPrevention.command.CommandRestoreNature;
import me.ryanhamshire.GriefPrevention.command.CommandRestoreNatureAggressive;
import me.ryanhamshire.GriefPrevention.command.CommandRestoreNatureFill;
import me.ryanhamshire.GriefPrevention.command.CommandSeparate;
import me.ryanhamshire.GriefPrevention.command.CommandSetAccruedClaimBlocks;
import me.ryanhamshire.GriefPrevention.command.CommandSiege;
import me.ryanhamshire.GriefPrevention.command.CommandSoftMute;
import me.ryanhamshire.GriefPrevention.command.CommandTrapped;
import me.ryanhamshire.GriefPrevention.command.CommandTrust;
import me.ryanhamshire.GriefPrevention.command.CommandTrustList;
import me.ryanhamshire.GriefPrevention.command.CommandUnignorePlayer;
import me.ryanhamshire.GriefPrevention.command.CommandUnlockDrops;
import me.ryanhamshire.GriefPrevention.command.CommandUnseparate;
import me.ryanhamshire.GriefPrevention.command.CommandUntrust;
import me.ryanhamshire.GriefPrevention.command.claim.CommandClaimAbandon;
import me.ryanhamshire.GriefPrevention.command.claim.CommandClaimAbandonAll;
import me.ryanhamshire.GriefPrevention.command.claim.CommandClaimBasic;
import me.ryanhamshire.GriefPrevention.command.claim.CommandClaimAdmin;
import me.ryanhamshire.GriefPrevention.command.claim.CommandClaimAdminList;
import me.ryanhamshire.GriefPrevention.command.claim.CommandClaim;
import me.ryanhamshire.GriefPrevention.command.claim.CommandClaimBook;
import me.ryanhamshire.GriefPrevention.command.claim.CommandClaimBuy;
import me.ryanhamshire.GriefPrevention.command.claim.CommandClaimExplosions;
import me.ryanhamshire.GriefPrevention.command.claim.CommandClaimFlag;
import me.ryanhamshire.GriefPrevention.command.claim.CommandClaimIgnore;
import me.ryanhamshire.GriefPrevention.command.claim.CommandClaimList;
import me.ryanhamshire.GriefPrevention.command.claim.CommandClaimTransfer;
import me.ryanhamshire.GriefPrevention.command.claim.CommandClaimSell;
import me.ryanhamshire.GriefPrevention.command.claim.CommandClaimSubdivide;
import me.ryanhamshire.GriefPrevention.command.claim.CommandClaimDelete;
import me.ryanhamshire.GriefPrevention.command.claim.CommandClaimDeleteAll;
import me.ryanhamshire.GriefPrevention.command.claim.CommandClaimDeleteAllAdmin;
import me.ryanhamshire.GriefPrevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.GriefPrevention.configuration.GriefPreventionConfig.DimensionConfig;
import me.ryanhamshire.GriefPrevention.configuration.GriefPreventionConfig.GlobalConfig;
import me.ryanhamshire.GriefPrevention.configuration.GriefPreventionConfig.Type;
import me.ryanhamshire.GriefPrevention.configuration.GriefPreventionConfig.WorldConfig;
import me.ryanhamshire.GriefPrevention.events.BlockEventHandler;
import me.ryanhamshire.GriefPrevention.events.EntityEventHandler;
import me.ryanhamshire.GriefPrevention.events.PlayerEventHandler;
import me.ryanhamshire.GriefPrevention.events.WorldEventHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
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
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Plugin(id = "GriefPrevention", name = "GriefPrevention", version = "12.7.1")
public class GriefPrevention {

    // for convenience, a reference to the instance of this plugin
    public static GriefPrevention instance;
    public static final String MOD_ID = "GriefPrevention";
    @Inject public PluginContainer pluginContainer;

    // for logging to the console and log file
    private static Logger log = Logger.getLogger("Minecraft");

    // this handles data storage, like player and region data
    public DataStore dataStore;

    // log entry manager for GP's custom log files
    CustomLogger customLogger;

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

    // whether nether portals require permission to generate. defaults to off for performance reasons
    public boolean config_claims_portalsRequirePermission;

    // whether trading with a claimed villager requires permission
    public boolean config_claims_villagerTradingRequiresTrust;

    // how many additional blocks players get each hour of play (can be zero)
    public int config_claims_blocksAccruedPerHour;

    // whether creeper explosions near or above the surface destroy blocks
    public boolean config_blockSurfaceCreeperExplosions;

    // whether non-creeper explosions near or above the surface destroy blocks
    public boolean config_blockSurfaceOtherExplosions;

    // whether or not endermen may move blocks around
    public boolean config_endermenMoveBlocks;

    // whether silverfish may break blocks
    public boolean config_silverfishBreakBlocks;

    // whether or not non-player entities may trample crops
    public boolean config_creaturesTrampleCrops;

    // whether or not hard-mode zombies may break down wooden doors
    public boolean config_zombiesBreakDoors;

    // list of block IDs which can be destroyed by explosions, even in claimed areas
    public List<ItemInfo> config_mods_explodableIds;

    // override for sea level, because bukkit doesn't report the right value for all situations
    public HashMap<String, Integer> config_seaLevelOverride;

    // how far away to search from a tree trunk for its branch blocks
    public static final int TREE_RADIUS = 5;

    // how long to wait before deciding a player is staying online or staying offline, for notication messages
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
        // TODO - add proper DB support
        /*if (this.databaseUrl.length() > 0) {
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
                        "Because there was a problem with the database, GriefPrevention will not function properly.  Either update the database config settings resolve the issue, or delete those lines from your config so that GriefPrevention can use the file system to store data.");
                e.printStackTrace();
                return;
            }
        }*/

        // if not using the database because it's not configured or because
        // there was a problem, use the file system to store data
        // this is the preferred method, as it's simpler than the database scenario
        if (this.dataStore == null) {
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
        //EntityCleanupTask task = new EntityCleanupTask(0);
        //Sponge.getGame().getScheduler().createTaskBuilder().delay(2, TimeUnit.MINUTES).execute(task).submit(GriefPrevention.instance);

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

        // load ignore lists for any already-online players
        Collection<Player> players = (Collection<Player>) Sponge.getGame().getServer().getOnlinePlayers();
        for (Player player : players) {
            new IgnoreLoaderThread(player.getUniqueId(), this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId()).ignoredPlayers).start();
        }

        Sponge.getGame().getEventManager().registerListeners(this, new BlockEventHandler(dataStore));
        Sponge.getGame().getEventManager().registerListeners(this, new PlayerEventHandler(dataStore, this));
        Sponge.getGame().getEventManager().registerListeners(this, new EntityEventHandler(dataStore));
        Sponge.getGame().getEventManager().registerListeners(this, new WorldEventHandler());
        Sponge.getGame().getCommandManager().register(this, CommandGriefPrevention.getCommand(), "griefprevention", "gp");
        AddLogEntry("Boot finished.");
    }

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

            Path rootConfigPath = Sponge.getGame().getSavesDirectory().resolve("config").resolve("GriefPrevention").resolve("worlds");
            DataStore.globalConfig = new GriefPreventionConfig<GlobalConfig>(Type.GLOBAL, rootConfigPath.resolve("global.conf"));
            for (World world : Sponge.getGame().getServer().getWorlds()) {
                DimensionType dimType = world.getProperties().getDimensionType();
                if (!Files.exists(rootConfigPath.resolve(dimType.getId()).resolve(world.getProperties().getWorldName()))) {
                    try {
                        Files.createDirectories(rootConfigPath.resolve(dimType.getId()).resolve(world.getProperties().getWorldName()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                DataStore.dimensionConfigMap.put(world.getProperties().getUniqueId(), new GriefPreventionConfig<DimensionConfig>(Type.DIMENSION, rootConfigPath.resolve(dimType.getId()).resolve("dimension.conf")));
                DataStore.worldConfigMap.put(world.getProperties().getUniqueId(), new GriefPreventionConfig<>(Type.WORLD, rootConfigPath.resolve(dimType.getId()).resolve(world.getProperties().getWorldName()).resolve("world.conf")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Player checkPlayer(CommandSource source) throws CommandException {
        if (source instanceof Player) {
            return ((Player) source);
        } else {
            throw new CommandException(Text.of("You must be a player to run this command!"));
        }
    }

    // handles sub commands
    public HashMap<List<String>, CommandSpec> registerSubCommands() {
        HashMap<List<String>, CommandSpec> subcommands = new HashMap<List<String>, CommandSpec>();

        subcommands.put(Arrays.asList("restorenature", "rn"), CommandSpec.builder()
                .description(Text.of("Switches the shovel tool to restoration mode"))
                .permission("griefprevention.command.restorenature")
                .executor(new CommandRestoreNature())
                .build());
        
        subcommands.put(Arrays.asList("restorenatureaggressive", "rna"), CommandSpec.builder()
                .description(Text.of("Switches the shovel tool to aggressive restoration mode"))
                .permission("griefprevention.command.restorenatureaggressive")
                .executor(new CommandRestoreNatureAggressive())
                .build());

        subcommands.put(Arrays.asList("restorenaturefill", "rnf"), CommandSpec.builder()
                .description(Text.of("Switches the shovel tool to fill mode"))
                .permission("griefprotection.command.restorenaturefill")
                .arguments(optional(integer(Text.of("radius")), 2))
                .executor(new CommandRestoreNatureFill())
                .build());
        
        subcommands.put(Arrays.asList("trust", "tr"), CommandSpec.builder()
                .description(Text.of("Grants a player full access to your claim(s)"))
                .extendedDescription(Text.of("Grants a player full access to your claim(s).\n"
                        + "See also /untrust, /containertrust, /accesstrust, and /permissiontrust."))
                .permission("griefprevention.command.trust")
                .arguments(string(Text.of("subject")))
                .executor(new CommandTrust())
                .build());

        subcommands.put(Arrays.asList("trustlist"), CommandSpec.builder()
                .description(Text.of("Lists permissions for the claim you're standing in"))
                .permission("griefprevention.command.trustlist")
                .executor(new CommandTrustList())
                .build());

        subcommands.put(Arrays.asList("untrust", "ut"), CommandSpec.builder()
                .description(Text.of("Revokes a player's access to your claim(s)"))
                .permission("griefprevention.command.untrust")
                .arguments(string(Text.of("subject")))
                .executor(new CommandUntrust())
                .build());

        subcommands.put(Arrays.asList("accesstrust", "at"), CommandSpec.builder()
                .description(Text.of("Grants a player entry to your claim(s) and use of your bed"))
                .permission("griefprevention.command.accesstrust")
                .arguments(string(Text.of("target")))
                .executor(new CommandAccessTrust())
                .build());

        subcommands.put(Arrays.asList("containertrust", "ct"), CommandSpec.builder()
                .description(Text.of("Grants a player access to your claim's containers, crops, animals, bed, buttons, and levers"))
                .permission("griefprevention.command.containertrust")
                .arguments(string(Text.of("target")))
                .executor(new CommandContainerTrust())
                .build());

        subcommands.put(Arrays.asList("permissiontrust", "pt"), CommandSpec.builder()
                .description(Text.of("Grants a player permission to grant their level of permission to others"))
                .permission("griefprevention.command.permissiontrust")
                .arguments(string(Text.of("target")))
                .executor(new CommandPermissionTrust())
                .build());
        
        HashMap<List<String>, CommandSpec> claimSubcommands = new HashMap<List<String>, CommandSpec>();

        claimSubcommands.put(Arrays.asList("abandontoplevel"), CommandSpec.builder()
                .description(Text.of("Deletes a claim and all its subdivisions"))
                .permission("griefprevention.command.claim.abandontoplevel")
                .executor(new CommandClaimAbandon(true))
                .build());
        
        claimSubcommands.put(Arrays.asList("ignore", "i"), CommandSpec.builder()
                .description(Text.of("Toggles ignore claims mode"))
                .permission("griefprevention.command.claim.ignore")
                .executor(new CommandClaimIgnore())
                .build());
        
        claimSubcommands.put(Arrays.asList("abandonall"), CommandSpec.builder()
                .description(Text.of("Deletes ALL your claims"))
                .permission("griefprevention.command.claim.abandonall")
                .executor(new CommandClaimAbandonAll())
                .build());
        
        claimSubcommands.put(Arrays.asList("buyblocks", "buy"), CommandSpec.builder()
                .description(Text.of("Purchases additional claim blocks with server money. Doesn't work on servers without a vault-compatible "
                        + "economy plugin"))
                .permission("griefprevention.command.claim.buy")
                .arguments(optional(integer(Text.of("numberOfBlocks"))))
                .executor(new CommandClaimBuy())
                .build());
        
        claimSubcommands.put(Arrays.asList("sellblocks", "sell"), CommandSpec.builder()
                .description(Text.of("Sell your claim blocks for server money. Doesn't work on servers without a vault-compatible "
                        + "economy plugin"))
                .permission("griefprevention.command.claim.sell")
                .arguments(optional(integer(Text.of("numberOfBlocks"))))
                .executor(new CommandClaimSell())
                .build());

        claimSubcommands.put(Arrays.asList("admin", "a"), CommandSpec.builder()
                .description(Text.of("Switches the shovel tool to administrative claims mode"))
                .permission("griefprevention.command.claim.admin")
                .executor(new CommandClaimAdmin())
                .build());
        
        claimSubcommands.put(Arrays.asList("basic", "b"), CommandSpec.builder()
                .description(Text.of("Switches the shovel tool back to basic claims mode"))
                .permission("griefprevention.command.claim.basic")
                .executor(new CommandClaimBasic())
                .build());
        
        claimSubcommands.put(Arrays.asList("subdivide", "s"), CommandSpec.builder()
                .description(Text.of("Switches the shovel tool to subdivision mode, used to subdivide your claims"))
                .permission("griefprevention.command.claim.subdivide")
                .executor(new CommandClaimSubdivide())
                .build());
        
        claimSubcommands.put(Arrays.asList("delete", "d"), CommandSpec.builder()
                .description(Text.of("Deletes the claim you're standing in, even if it's not your claim"))
                .permission("griefprevention.dcommand.claim.delete")
                .executor(new CommandClaimDelete())
                .build());
        
        claimSubcommands.put(Arrays.asList("transfer", "give"), CommandSpec.builder()
                .description(Text.of("Converts an administrative claim to a private claim"))
                .arguments(optional(player(Text.of("target"))))
                .permission("griefprevention.command.claim.transfer")
                .executor(new CommandClaimTransfer())
                .build());
        
        claimSubcommands.put(Arrays.asList("explosions"), CommandSpec.builder()
                .description(Text.of("Toggles whether explosives may be used in a specific land claim"))
                .permission("griefprevention.command.claim.explosions")
                .executor(new CommandClaimExplosions())
                .build());
        
        claimSubcommands.put(Arrays.asList("deleteall"), CommandSpec.builder()
                .description(Text.of("Delete all of another player's claims"))
                .permission("griefprevention.command.claim.deleteall")
                .arguments(player(Text.of("player"))) // TODO: Use user commandelement when added
                .executor(new CommandClaimDeleteAll())
                .build());

        claimSubcommands.put(Arrays.asList("book"), CommandSpec.builder()
                .description(Text.of("Gives a player a manual about claiming land"))
                .permission("griefprevention.command.claim.book")
                .arguments(playerOrSource(Text.of("player")))
                .executor(new CommandClaimBook())
                .build());

        claimSubcommands.put(Arrays.asList("list"), CommandSpec.builder()
                .description(Text.of("List information about a player's claim blocks and claims"))
                .permission("griefprevention.command.claim.list")
                .arguments(onlyOne(playerOrSource(Text.of("player"))))
                .executor(new CommandClaimList())
                .build());
        
        claimSubcommands.put(Arrays.asList("flag"), CommandSpec.builder()
                .description(Text.of("Gets/Sets various claim flags in the claim you are standing in"))
                .permission("griefprevention.command.claim.flag")
                .arguments(GenericArguments.seq(
                        GenericArguments.onlyOne(GenericArguments.string(Text.of("flag"))),
                        GenericArguments.onlyOne(GenericArguments.bool(Text.of("value")))))
                .executor(new CommandClaimFlag())
                .build());
        
        claimSubcommands.put(Arrays.asList("abandon", "remove"), CommandSpec.builder()
                .description(Text.of("Deletes a claim"))
                .permission("griefprevention.command.claim.abandon")
                .executor(new CommandClaimAbandon(false))
                .build());
        
        claimSubcommands.put(Arrays.asList("adminlist"), CommandSpec.builder()
                .description(Text.of("List all administrative claims"))
                .permission("griefprevention.command.claim.adminlist")
                .executor(new CommandClaimAdminList())
                .build());
        
        claimSubcommands.put(Arrays.asList("deletealladmin"), CommandSpec.builder()
                .description(Text.of("Deletes all administrative claims"))
                .permission("griefprevention.command.claim.deletealladmin")
                .executor(new CommandClaimDeleteAllAdmin())
                .build());
        
        subcommands.put(Arrays.asList("claim"), CommandSpec.builder()
                .description(Text.of("Claims land"))
                .permission("griefprevention.command.claim")
                .children(claimSubcommands)
                .executor(new CommandClaim())
                .build());

        subcommands.put(Arrays.asList("unlockdrops"), CommandSpec.builder()
                .description(Text.of("Allows other players to pick up the items you dropped when you died"))
                .permission("griefprevention.command.unlockdrops")
                .executor(new CommandUnlockDrops())
                .build());

        subcommands.put(Arrays.asList("adjustbonusclaimblocks", "acb"), CommandSpec.builder()
                .description(Text.of("Adds or subtracts bonus claim blocks for a player"))
                .permission("griefprevention.command.adjustclaimblocks")
                .arguments(string(Text.of("player")), integer(Text.of("amount")))
                .executor(new CommandAdjustBonusClaimBlocks())
                .build());
        
        subcommands.put(Arrays.asList("setaccruedclaimblocks", "scb"), CommandSpec.builder()
                .description(Text.of("Updates a player's accrued claim block total"))
                .permission("griefprevention.command.setaccruedclaimblocks")
                .arguments(string(Text.of("player")), integer(Text.of("amount")))
                .executor(new CommandSetAccruedClaimBlocks())
                .build());
        
        subcommands.put(Arrays.asList("trapped"), CommandSpec.builder()
                .description(Text.of("Ejects you to nearby unclaimed land. Has a substantial cooldown period"))
                .permission("griefprevention.command.trapped")
                .executor(new CommandTrapped())
                .build());

        subcommands.put(Arrays.asList("siege"), CommandSpec.builder()
                .description(Text.of("Initiates a siege versus another player"))
                .arguments(optional(onlyOne(player(Text.of("playerName")))))
                .permission("griefprevention.command.siege")
                .executor(new CommandSiege())
                .build());

        subcommands.put(Arrays.asList("softmute"), CommandSpec.builder()
                .description(Text.of("Toggles whether a player's messages will only reach other soft-muted players"))
                .permission("griefprevention.comand.softmute")
                .arguments(onlyOne(player(Text.of("player"))))
                .executor(new CommandSoftMute())
                .build());

        subcommands.put(Arrays.asList("reload"), CommandSpec.builder()
                .description(Text.of("Reloads Grief Prevention's configuration settings"))
                .permission("griefprevention.command.reload")
                .executor(new CommandGpReload())
                .build());
        
        subcommands.put(Arrays.asList("givepet"), CommandSpec.builder()
                .description(Text.of("Allows a player to give away a pet they tamed"))
                .permission("griefprevention.command.givepet")
                .arguments(GenericArguments.firstParsing(GenericArguments.literal(Text.of("player"), "cancel"), player(Text.of("player"))))
                .executor(new CommandGivePet())
                .build());
        
        subcommands.put(Arrays.asList("gpblockinfo"), CommandSpec.builder()
                .description(Text.of("Allows an administrator to get technical information about blocks in the world and items in hand"))
                .permission("griefprevention.command.gpblockinfo")
                .executor(new CommandGpBlockInfo())
                .build());

        subcommands.put(Arrays.asList("ignoreplayer", "ignore"), CommandSpec.builder()
                .description(Text.of("Ignores another player's chat messages"))
                .permission("griefprevention.command.ignore")
                .arguments(onlyOne(player(Text.of("player"))))
                .executor(new CommandIgnorePlayer())
                .build());

        subcommands.put(Arrays.asList("unignoreplayer", "unignore"), CommandSpec.builder()
                .description(Text.of("Unignores another player's chat messages"))
                .permission("griefprevention.command.unignore")
                .arguments(onlyOne(player(Text.of("player"))))
                .executor(new CommandUnignorePlayer())
                .build());

        subcommands.put(Arrays.asList("ignoredplayerlist", "ignores", "ignored", "ignoredlist", "listignores", "listignored", "ignoring"), 
                CommandSpec.builder()
                    .description(Text.of("Lists the players you're ignoring in chat"))
                    .permission("griefprevention.command.ignores")
                    .executor(new CommandIgnoredPlayerList())
                    .build());

        subcommands.put(Arrays.asList("separate"), CommandSpec.builder()
                .description(Text.of("Forces two players to ignore each other in chat"))
                .permission("griefprevention.command.separate")
                .arguments(onlyOne(player(Text.of("player1"))), onlyOne(player(Text.of("player2"))))
                .executor(new CommandSeparate())
                .build());

        subcommands.put(Arrays.asList("unseparate"), CommandSpec.builder()
                .description(Text.of("Reverses /separate"))
                .permission("griefprevention.command.unseparate")
                .arguments(onlyOne(player(Text.of("player1"))), onlyOne(player(Text.of("player2"))))
                .executor(new CommandUnseparate())
                .build());
        
        return subcommands;
    }

    public void setIgnoreStatus(World world, User ignorer, User ignoree, IgnoreMode mode) {
        PlayerData playerData = this.dataStore.getPlayerData(world, ignorer.getUniqueId());
        if (mode == IgnoreMode.None) {
            playerData.ignoredPlayers.remove(ignoree.getUniqueId());
        } else {
            playerData.ignoredPlayers.put(ignoree.getUniqueId(), mode == IgnoreMode.StandardIgnore ? false : true);
        }

        playerData.ignoreListChanged = true;
        if (!ignorer.isOnline()) {
            // TODO
            // this.dataStore.savePlayerData(ignorer.getUniqueId(), playerData);
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

   /* public void onDisable() {
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
    }*/

    // called when a player spawns, applies protection for that player if necessary
    public void checkPvpProtectionNeeded(Player player) {
        // if anti spawn camping feature is not enabled, do nothing
        if (!GriefPrevention.getActiveConfig(player.getWorld()).getConfig().pvp.protectFreshSpawns) {
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
            PlayerData playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
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
        return Text.of(GriefPrevention.instance.dataStore.getMessage(messageID, args));
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
        sendMessage(player, Text.of(color, message));
    }

    // sends a color-coded message to a player
    public static void sendMessage(CommandSource player, Text message) {
        if (message == Text.of() || message == null)
            return;

        if (player == null) {
            GriefPrevention.AddLogEntry(Text.of(message).toPlain());
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

    public static GriefPreventionConfig<?> getActiveConfig(World world) {
        GriefPreventionConfig<WorldConfig> worldConfig = DataStore.worldConfigMap.get(world.getUniqueId());
        GriefPreventionConfig<DimensionConfig> dimConfig = DataStore.dimensionConfigMap.get(world.getUniqueId());
        if (worldConfig.getConfig().configEnabled) {
            return worldConfig;
        } else if (dimConfig.getConfig().configEnabled) {
            return dimConfig;
        } else {
            return DataStore.globalConfig;
        }
    }

    public static GriefPreventionConfig<GlobalConfig> getGlobalConfig() {
        return DataStore.globalConfig;
    }

    // checks whether players can create claims in a world
    public boolean claimsEnabledForWorld(World world) {
        return GriefPrevention.getActiveConfig(world).getConfig().claim.allowClaims;
    }

    public boolean claimModeIsActive(World world, ClaimsMode mode) {
        return GriefPrevention.getActiveConfig(world).getConfig().claim.claimMode == mode.ordinal();
    }

    public String allowBuild(Player player, Location<World> location) {
        PlayerData playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        Claim claim = this.dataStore.getClaimAt(location, false, playerData.lastClaim);

        // exception: administrators in ignore claims mode and special player accounts created by server mods
        if (playerData.ignoreClaims || GriefPrevention.getActiveConfig(location.getExtent()).getConfig().claim.alwaysIgnoreClaimsList.contains(player.getUniqueId().toString()))
            return null;

        // wilderness rules
        if (claim == null) {
            // no building in the wilderness in creative mode
            if (claimModeIsActive(location.getExtent(), ClaimsMode.Creative) || claimModeIsActive(location.getExtent(), ClaimsMode.SurvivalRequiringClaims)) {
                // exception: when chest claims are enabled, players who have zero land claims and are placing a chest
                if (!player.getItemInHand().isPresent() || player.getItemInHand().get().getItem() != ItemTypes.CHEST || playerData.playerWorldClaims.get(location.getExtent().getUniqueId()).size() > 0
                        || GriefPrevention.getActiveConfig(player.getWorld()).getConfig().claim.claimRadius == -1) {
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
        PlayerData playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        Claim claim = this.dataStore.getClaimAt(snapshot.getLocation().get(), false, playerData.lastClaim);

        // exception: administrators in ignore claims mode, and special player
        // accounts created by server mods
        if (playerData.ignoreClaims || GriefPrevention.getActiveConfig(player.getWorld()).getConfig().claim.alwaysIgnoreClaimsList.contains(player.getUniqueId().toString()))
            return null;

        // wilderness rules
        if (claim == null) {
            // no building in the wilderness in creative mode
            if (claimModeIsActive(snapshot.getLocation().get().getExtent(), ClaimsMode.Creative) || claimModeIsActive(snapshot.getLocation().get().getExtent(), ClaimsMode.SurvivalRequiringClaims)) {
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

            // if not in the wilderness, then apply claim rules (permissions, etc)
            return claim.allowBreak(player, snapshot.getLocation().get().getBlockType());
        }
    }

    // restores nature in multiple chunks, as described by a claim instance
    // this restores all chunks which have ANY number of claim blocks from this claim in them
    // if the claim is still active (in the data store), then the claimed blocks
    // will not be changed (only the area bordering the claim)
    public void restoreClaim(Claim claim, long delayInTicks) {
        // admin claims aren't automatically cleaned up when deleted or abandoned
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
                aggressiveMode, claimModeIsActive(lesserBoundaryCorner.getExtent(), ClaimsMode.Creative), playerReceivingVisualization);
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

            // null value returned indicates an error parsing the string from the config file
            if (!blockType.isPresent() || !blockType.get().getItem().isPresent()) {
                // show error in log
                GriefPrevention.AddLogEntry("ERROR: Unable to read a block entry from the config file.  Please update your config.");

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

    public boolean containsBlockedIP(String message) {
        message = message.replace("\r\n", "");
        Pattern ipAddressPattern = Pattern.compile("([0-9]{1,3}\\.){3}[0-9]{1,3}");
        Matcher matcher = ipAddressPattern.matcher(message);

        // if it looks like an IP address
        if (matcher.find()) {
            // and it's not in the list of allowed IP addresses
            if (!GriefPrevention.getGlobalConfig().getConfig().spam.allowedIpAddresses.contains(matcher.group())) {
                return true;
            }
        }

        return false;
    }

    public boolean pvpRulesApply(World world) {
        Boolean configSetting = GriefPrevention.getActiveConfig(world).getConfig().pvp.rulesEnabled;
        if (configSetting != null) {
            return configSetting;
        }

        return world.getProperties().isPVPEnabled();
    }

    public static boolean isItemBanned(World world, ItemType type, int meta) {
        String nonMetaItemString = type.getId();
        String metaItemString = type.getId() + ":" + meta;
        if (GriefPrevention.getActiveConfig(world).getConfig().general.bannedItemList.contains(nonMetaItemString)) {
            return true;
        } else if (GriefPrevention.getActiveConfig(world).getConfig().general.bannedItemList.contains(metaItemString)) {
            return true;
        } else {
            return false;
        }
    }
}
