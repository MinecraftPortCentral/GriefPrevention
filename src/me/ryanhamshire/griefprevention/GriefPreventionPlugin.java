/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) Ryan Hamshire
 * Copyright (c) bloodmc
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
package me.ryanhamshire.griefprevention;

import static org.spongepowered.api.command.args.GenericArguments.bool;
import static org.spongepowered.api.command.args.GenericArguments.catalogedElement;
import static org.spongepowered.api.command.args.GenericArguments.choices;
import static org.spongepowered.api.command.args.GenericArguments.integer;
import static org.spongepowered.api.command.args.GenericArguments.onlyOne;
import static org.spongepowered.api.command.args.GenericArguments.optional;
import static org.spongepowered.api.command.args.GenericArguments.player;
import static org.spongepowered.api.command.args.GenericArguments.playerOrSource;
import static org.spongepowered.api.command.args.GenericArguments.string;
import static org.spongepowered.api.command.args.GenericArguments.user;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import me.ryanhamshire.griefprevention.api.GriefPreventionApi;
import me.ryanhamshire.griefprevention.claim.ClaimContextCalculator;
import me.ryanhamshire.griefprevention.claim.ClaimsMode;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.claim.GPClaimManager;
import me.ryanhamshire.griefprevention.command.CommandAccessTrust;
import me.ryanhamshire.griefprevention.command.CommandAdjustBonusClaimBlocks;
import me.ryanhamshire.griefprevention.command.CommandClaimAbandon;
import me.ryanhamshire.griefprevention.command.CommandClaimAbandonAll;
import me.ryanhamshire.griefprevention.command.CommandClaimAdmin;
import me.ryanhamshire.griefprevention.command.CommandClaimAdminList;
import me.ryanhamshire.griefprevention.command.CommandClaimBanItem;
import me.ryanhamshire.griefprevention.command.CommandClaimBasic;
import me.ryanhamshire.griefprevention.command.CommandClaimBook;
import me.ryanhamshire.griefprevention.command.CommandClaimBuy;
import me.ryanhamshire.griefprevention.command.CommandClaimClear;
import me.ryanhamshire.griefprevention.command.CommandClaimCuboid;
import me.ryanhamshire.griefprevention.command.CommandClaimDelete;
import me.ryanhamshire.griefprevention.command.CommandClaimDeleteAll;
import me.ryanhamshire.griefprevention.command.CommandClaimDeleteAllAdmin;
import me.ryanhamshire.griefprevention.command.CommandClaimFarewell;
import me.ryanhamshire.griefprevention.command.CommandClaimFlag;
import me.ryanhamshire.griefprevention.command.CommandClaimFlagDebug;
import me.ryanhamshire.griefprevention.command.CommandClaimFlagGroup;
import me.ryanhamshire.griefprevention.command.CommandClaimFlagPlayer;
import me.ryanhamshire.griefprevention.command.CommandClaimFlagReset;
import me.ryanhamshire.griefprevention.command.CommandClaimGreeting;
import me.ryanhamshire.griefprevention.command.CommandClaimIgnore;
import me.ryanhamshire.griefprevention.command.CommandClaimInfo;
import me.ryanhamshire.griefprevention.command.CommandClaimInherit;
import me.ryanhamshire.griefprevention.command.CommandClaimList;
import me.ryanhamshire.griefprevention.command.CommandClaimName;
import me.ryanhamshire.griefprevention.command.CommandClaimPermissionGroup;
import me.ryanhamshire.griefprevention.command.CommandClaimPermissionPlayer;
import me.ryanhamshire.griefprevention.command.CommandClaimSell;
import me.ryanhamshire.griefprevention.command.CommandClaimSetSpawn;
import me.ryanhamshire.griefprevention.command.CommandClaimSpawn;
import me.ryanhamshire.griefprevention.command.CommandClaimSubdivide;
import me.ryanhamshire.griefprevention.command.CommandClaimTransfer;
import me.ryanhamshire.griefprevention.command.CommandClaimUnbanItem;
import me.ryanhamshire.griefprevention.command.CommandContainerTrust;
import me.ryanhamshire.griefprevention.command.CommandDebug;
import me.ryanhamshire.griefprevention.command.CommandGivePet;
import me.ryanhamshire.griefprevention.command.CommandGpReload;
import me.ryanhamshire.griefprevention.command.CommandIgnorePlayer;
import me.ryanhamshire.griefprevention.command.CommandIgnoredPlayerList;
import me.ryanhamshire.griefprevention.command.CommandPermissionTrust;
import me.ryanhamshire.griefprevention.command.CommandPlayerInfo;
import me.ryanhamshire.griefprevention.command.CommandRestoreNature;
import me.ryanhamshire.griefprevention.command.CommandRestoreNatureAggressive;
import me.ryanhamshire.griefprevention.command.CommandRestoreNatureFill;
import me.ryanhamshire.griefprevention.command.CommandSeparate;
import me.ryanhamshire.griefprevention.command.CommandSetAccruedClaimBlocks;
import me.ryanhamshire.griefprevention.command.CommandSiege;
import me.ryanhamshire.griefprevention.command.CommandSoftMute;
import me.ryanhamshire.griefprevention.command.CommandTrust;
import me.ryanhamshire.griefprevention.command.CommandTrustAll;
import me.ryanhamshire.griefprevention.command.CommandTrustList;
import me.ryanhamshire.griefprevention.command.CommandUnignorePlayer;
import me.ryanhamshire.griefprevention.command.CommandUnlockDrops;
import me.ryanhamshire.griefprevention.command.CommandUnseparate;
import me.ryanhamshire.griefprevention.command.CommandUntrust;
import me.ryanhamshire.griefprevention.command.CommandUntrustAll;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig.Type;
import me.ryanhamshire.griefprevention.configuration.type.DimensionConfig;
import me.ryanhamshire.griefprevention.configuration.type.GlobalConfig;
import me.ryanhamshire.griefprevention.configuration.type.WorldConfig;
import me.ryanhamshire.griefprevention.listener.BlockEventHandler;
import me.ryanhamshire.griefprevention.listener.EntityEventHandler;
import me.ryanhamshire.griefprevention.listener.PlayerEventHandler;
import me.ryanhamshire.griefprevention.listener.WorldEventHandler;
import me.ryanhamshire.griefprevention.logging.CustomLogEntryTypes;
import me.ryanhamshire.griefprevention.logging.CustomLogger;
import me.ryanhamshire.griefprevention.message.Messages;
import me.ryanhamshire.griefprevention.message.TextMode;
import me.ryanhamshire.griefprevention.permission.GPPermissionHandler;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import me.ryanhamshire.griefprevention.task.CleanupUnusedClaimsTask;
import me.ryanhamshire.griefprevention.task.DeliverClaimBlocksTask;
import me.ryanhamshire.griefprevention.task.IgnoreLoaderThread;
import me.ryanhamshire.griefprevention.task.PvPImmunityValidationTask;
import me.ryanhamshire.griefprevention.task.RestoreNatureProcessingTask;
import me.ryanhamshire.griefprevention.task.SendPlayerMessageTask;
import me.ryanhamshire.griefprevention.util.PlayerUtils;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.spongepowered.api.Platform.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.living.player.gamemode.GameModes;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.game.state.GameAboutToStartServerEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.interfaces.world.IMixinDimensionType;
import org.spongepowered.common.registry.RegistryHelper;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Plugin(id = "griefprevention", name = "GriefPrevention", version = "2.3.1", description = "This plugin is designed to prevent all forms of grief.")
public class GriefPreventionPlugin {

    // for convenience, a reference to the instance of this plugin
    public static GriefPreventionPlugin instance;
    public static final String MOD_ID = "GriefPrevention";
    public static Cause pluginCause;
    @Inject public PluginContainer pluginContainer;
    @Inject private Logger logger;
    @Inject @ConfigDir(sharedRoot = false)
    private Path configPath;
    //java.util.concurrent.ScheduledExecutorService executor = Executors.newScheduledThreadPool(

    public static final String CONFIG_HEADER = "2.3.1\n"
            + "# If you need help with the configuration or have any questions related to GriefPrevention,\n"
            + "# join us at the IRC or drop by our forums and leave a post.\n"
            + "# IRC: #griefprevention @ irc.esper.net ( http://webchat.esper.net/?channel=griefprevention )\n"
            + "# Forums: https://forums.spongepowered.org/t/griefprevention-official-thread-1-8-9-1-10-2/1123/\n";

    // GP Public user info
    public static final UUID PUBLIC_UUID = UUID.fromString("41C82C87-7AfB-4024-BA57-13D2C99CAE77");
    public static final UUID WORLD_USER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    public static final UUID ADMIN_USER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static User PUBLIC_USER;
    public static User WORLD_USER;
    public static final String PUBLIC_NAME = "[GPPublic]";
    public static final String WORLD_USER_NAME = "[GPWorld]";

    // GP Custom Subjects
    public static Subject GLOBAL_SUBJECT;

    // GP Global contexts
    public static final Context ADMIN_CLAIM_FLAG_DEFAULT_CONTEXT = new Context("gp_claim_defaults", "ADMIN");
    public static final Context BASIC_CLAIM_FLAG_DEFAULT_CONTEXT = new Context("gp_claim_defaults", "BASIC");
    public static final Context WILDERNESS_CLAIM_FLAG_DEFAULT_CONTEXT = new Context("gp_claim_defaults", "WILDERNESS");
    public static final Context ADMIN_CLAIM_FLAG_OVERRIDE_CONTEXT = new Context("gp_claim_overrides", "ADMIN");
    public static final Context BASIC_CLAIM_FLAG_OVERRIDE_CONTEXT = new Context("gp_claim_overrides", "BASIC");

    // this handles data storage, like player and region data
    public DataStore dataStore;

    public PermissionService permissionService;
    public PermissionDescription.Builder permissionDescriptionBuilder;
    private GriefPreventionApi api;

    public Optional<EconomyService> economyService;

    public boolean permPluginInstalled = false;

    public ItemType modificationTool = ItemTypes.GOLDEN_SHOVEL;
    public ItemType investigationTool = ItemTypes.STICK;
    public int maxInspectionDistance = 100;

    // log entry manager for GP's custom log files
    CustomLogger customLogger;
    public static boolean debugLogging = false;
    public static boolean debugVerbose = false;
    public static User debugUser = null;
    public static CommandSource debugSource = null;

    // how far away to search from a tree trunk for its branch blocks
    public static final int TREE_RADIUS = 5;

    // how long to wait before deciding a player is staying online or staying offline, for notication messages
    public static final int NOTIFICATION_SECONDS = 20;

    public static final Text GP_TEXT = Text.of(TextColors.RESET, "[", TextColors.AQUA, "GP", TextColors.WHITE, "] ");

    public Path getConfigPath() {
        return this.configPath;
    }

    public Logger getLogger() {
        return this.logger;
    }

    // adds a server log entry
    public static void addLogEntry(String entry, CustomLogEntryTypes customLogType, boolean verbose) {
        if (customLogType == CustomLogEntryTypes.Debug && !GriefPreventionPlugin.debugLogging) {
            return;
        }

        GriefPreventionPlugin.instance.customLogger.addEntry(entry, customLogType);

        if (verbose) {
            GriefPreventionPlugin.instance.logger.info("[GriefPrevention DEBUG]: " + entry);
        }
    }

    public static void addEventLogEntry(Event event, GPClaim claim, Location<World> location, User user, String reason) {
        addEventLogEntry(event, claim, location, null, null, user, reason);
    }

    public static void addEventLogEntry(Event event, GPClaim claim, Location<World> location, User user, String permission, String reason) {
        addEventLogEntry(event, claim, location, user, null, null, permission, reason);
    }

    public static void addEventLogEntry(Event event, GPClaim claim, Location<World> location, Object source, Object target, User user, String reason) {
        addEventLogEntry(event, claim, location, user, source, target, null, reason);
    }

    public static void addEventLogEntry(Event event, GPClaim claim, Location<World> location, String flagPermission, Object source, Object target, User user, String reason) {
        addEventLogEntry(event, claim, location, user, source, target, GPPermissionHandler.getPermission(source, target, flagPermission), reason);
    }

    public static void addEventLogEntry(Event event, GPClaim claim, Location<World> location, User user, Object source, Object target, String permission, String reason) {
        if (GriefPreventionPlugin.debugLogging) {
            String message = "[Claim: " + claim == null ? "none" : claim.getUniqueId().toString() +
                            "][Event: " + event.getClass().getSimpleName().replace('$', '.').replace(".Impl", "") +
                            "][Cause: " + GPPermissionHandler.getPermissionIdentifier(event.getCause().root())  +
                            "][Location: " + (location == null ? "none" : location.getBlockPosition()) + 
                            "][User: " + (user == null ? "none" : user.getName());
                            // TODO: Add more reasons                
                            //"][Reason: " + (reason == null ? "none" : reason + "]");

            if (GriefPreventionPlugin.debugUser == null) {
                addLogEntry(message);
            } else if (user != null && GriefPreventionPlugin.debugUser.getUniqueId().equals(user.getUniqueId())) {
                if (GriefPreventionPlugin.debugVerbose) {
                    CommandSource src = GriefPreventionPlugin.debugSource;
                    if (src instanceof Player) {
                        final Text textEvent = Text.of(GP_TEXT, TextColors.GRAY, "Event: ", TextColors.GREEN, event.getClass().getSimpleName().replace('$', '.').replace(".Impl", ""), "\n");
                        final Text textCause = Text.of(GP_TEXT, TextColors.GRAY, "Cause: ", TextColors.LIGHT_PURPLE, GPPermissionHandler.getPermissionIdentifier(event.getCause().root()), "\n");
                        final Text textLocation = Text.of(GP_TEXT, TextColors.GRAY, "Location: ", TextColors.WHITE, location == null ? "NONE" : location.getBlockPosition());
                        final Text textUser = Text.of(TextColors.GRAY, "User: ", TextColors.GOLD, user.getName(), "\n");
                        final Text textLocationAndUser = Text.of(textLocation, " ", textUser);
                        Text textContext = null;
                        Text textPermission = null;
                        if (source != null) {
                            textContext = Text.of(GP_TEXT, TextColors.GRAY, "Source: ", TextColors.YELLOW, GPPermissionHandler.getPermissionIdentifier(source));
                            if (target != null) {
                                textContext = Text.of(textContext, " ", TextColors.GRAY, "Target: ", TextColors.YELLOW, GPPermissionHandler.getPermissionIdentifier(target), "\n");
                            } else {
                                textContext = Text.of(textContext, "\n");
                            }
                        } else if (target != null) {
                            textContext = Text.of(GP_TEXT, TextColors.GRAY, "Target: ", TextColors.YELLOW, GPPermissionHandler.getPermissionIdentifier(target), "\n");
                        }
                        if (permission != null) {
                            textPermission = Text.of(GP_TEXT, TextColors.GRAY, "Permission: ", TextColors.RED, permission, "\n");
                        }
                        Text.Builder textBuilder = Text.builder().append(textEvent);
                        if (textContext != null) {
                            textBuilder.append(textContext);
                        } else {
                            textBuilder.append(textCause);
                        }
                        if (textPermission != null) {
                            textBuilder.append(textPermission);
                        } else {
                            for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                                System.out.println("FOund trace " + element);
                            }
                        }
                        textBuilder.append(textLocationAndUser);
                        GriefPreventionPlugin.debugSource.sendMessage(textBuilder.build());
                                // TODO: Add more reasons
                                //GP_TEXT, TextColors.GRAY, "Reason: ", TextColors.RED, reason == null ? "NONE" : reason, "\n"));
                    } else {
                        src.sendMessage(Text.of(message));
                    }
                }
                addLogEntry(message);
            }
        }
    }

    public static void addLogEntry(String entry, CustomLogEntryTypes customLogType) {
        addLogEntry(entry, customLogType, false);
    }

    public static void addLogEntry(String entry) {
        addLogEntry(entry, CustomLogEntryTypes.Debug, GriefPreventionPlugin.debugVerbose);
    }

    @Listener
    public void onChangeServiceProvider(ChangeServiceProviderEvent event) {
        if (event.getNewProvider() instanceof PermissionService && this.validateSpongeVersion()) {
            ((PermissionService) event.getNewProvider()).registerContextCalculator(new ClaimContextCalculator());
        }
    }

    private boolean validateSpongeVersion() {
        if (Sponge.getPlatform().getContainer(Component.IMPLEMENTATION).getName().equals("SpongeForge")) {
            if (Sponge.getPlatform().getContainer(Component.IMPLEMENTATION).getVersion().isPresent()) {
                int spongeVersion = 0;
                try {
                    String version = Sponge.getPlatform().getContainer(Component.IMPLEMENTATION).getVersion().get();
                    version = version.substring(Math.max(version.length() - 4, 0));
                    spongeVersion = Integer.parseInt(version);
                    if (spongeVersion < 2096) {
                        this.logger.error("Unable to initialize plugin. Detected SpongeForge build " + spongeVersion + " but GriefPrevention requires build 2096+.");
                        return false;
                    }
                } catch (NumberFormatException e) {

                }
            }
        }

        return true;
    }

    @Listener(order = Order.LAST)
    public void onPreInit(GamePreInitializationEvent event) {
        if (!validateSpongeVersion()) {
            return;
        }

        this.permissionService = Sponge.getServiceManager().provide(PermissionService.class).get();
        if (Sponge.getServiceManager().getRegistration(PermissionService.class).get().getPlugin().getId().equalsIgnoreCase("sponge")) {
            this.logger.error("Unable to initialize plugin. GriefPrevention requires one of the following permission plugins : LuckPerms, PEX, or PermissionsManager.");
            return;
        }

        this.permissionDescriptionBuilder = this.permissionService.newDescriptionBuilder(this.pluginContainer).orElse(null);
        if (this.permissionDescriptionBuilder != null) {
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_ABANDON_ALL_CLAIMS)
                .description(Text.of("Allows a user to run the command /abandonallclaims"))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_ABANDON_CLAIM)
                .description(Text.of("Allows a user to run the command /abandonclaim"))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_ABANDON_TOP_LEVEL_CLAIM)
                .description(Text.of("Allows a user to run the command /abandontoplevelclaim"))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_ADJUST_CLAIM_BLOCKS)
                .description(Text.of("Allows a user to run the command /acb"))
                .assign(PermissionDescription.ROLE_ADMIN, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_ADMIN_CLAIMS)
                .description(Text.of("Allows a user to run the command /adminclaims"))
                .assign(PermissionDescription.ROLE_ADMIN, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_BAN_ITEM)
                .description(Text.of("Allows a user to run the command /banitem"))
                .assign(PermissionDescription.ROLE_ADMIN, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_BASIC_MODE)
                .description(Text.of("Allows a user to run the command /basicclaims"))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_BUY_CLAIM_BLOCKS)
                .description(Text.of("Allows a user to run the command /buyclaimblocks"))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_CLAIM_CLEAR)
                .description(Text.of("Allows a user to run the command /claimclear"))
                .assign(PermissionDescription.ROLE_ADMIN, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_CLAIM_INFO_BASE)
                .description(Text.of("Allows a user to run the command /claiminfo"))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_CLAIM_INFO_OTHERS)
                .description(Text.of("Allows a user to run the command /claiminfo on other claims."))
                .assign(PermissionDescription.ROLE_STAFF, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_CLAIM_INFO_TELEPORT_BASE)
                .description(Text.of("Allows a user to use the teleport feature in /claiminfo"))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_CLAIM_INFO_TELEPORT_OTHERS)
                .description(Text.of("Allows a user to use the teleport feature in /claiminfo on other claims."))
                .assign(PermissionDescription.ROLE_STAFF, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_CLAIM_PERMISSION_GROUP)
                .description(Text.of("Sets a permission on a group for claim you are standing in."))
                .assign(PermissionDescription.ROLE_ADMIN, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_CLAIM_PERMISSION_PLAYER)
                .description(Text.of("Sets a permission on a player for claim you are standing in."))
                .assign(PermissionDescription.ROLE_ADMIN, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_CUBOID_CLAIMS)
                .description(Text.of("Toggles 3D cuboid claims mode."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_DEBUG)
                .description(Text.of("Allows an admin to debug events being cancelled by GP."))
                .assign(PermissionDescription.ROLE_ADMIN, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_DELETE_ADMIN_CLAIMS)
                .description(Text.of("Deletes all administrative claims."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_DELETE_CLAIMS)
                .description(Text.of("Delete all of another player's claims."))
                .assign(PermissionDescription.ROLE_ADMIN, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_FLAGS_CLAIM)
                .description(Text.of("Gets/Sets claim flags in the claim you are standing in."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_FLAGS_DEBUG)
                .description(Text.of("Toggles claim flag debug mode."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_FLAGS_GROUP)
                .description(Text.of("Gets/Sets claim flags for a group in claim you are standing in."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_FLAGS_PLAYER)
                .description(Text.of("Gets/Sets claim flags for a player in claim you are standing in."))
                .assign(PermissionDescription.ROLE_ADMIN, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_FLAGS_RESET)
                .description(Text.of("Resets claim you are standing in to flag defaults."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_GIVE_ACCESS_TRUST)
                .description(Text.of("Grants a player entry to your claim(s) and use of your bed(s)"))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_GIVE_BOOK)
                .description(Text.of("Gives a player a manual about claiming land."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_GIVE_BUILDER_TRUST)
                .description(Text.of("Grants a player edit access to your claim(s)."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_GIVE_CONTAINER_TRUST)
                .description(Text.of("Grants a player access to your claim's containers, crops, animals, bed, buttons, and levers."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_GIVE_PERMISSION_TRUST)
                .description(Text.of("Grants a player permission to grant their level of permission to others."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_GIVE_PET)
                .description(Text.of("Allows a player to give away a pet they tamed."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_IGNORE_CLAIMS)
                .description(Text.of("Toggles ignore claims mode."))
                .assign(PermissionDescription.ROLE_ADMIN, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_IGNORE_PLAYER)
                .description(Text.of("Ignores another player's chat messages."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_LIST_ADMIN_CLAIMS)
                .description(Text.of("List all administrative claims"))
                .assign(PermissionDescription.ROLE_ADMIN, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_LIST_CLAIMS)
                .description(Text.of("List all claims owned by a player."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_LIST_IGNORED_PLAYERS)
                .description(Text.of("Lists the players you're ignoring in chat."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_LIST_TRUST)
                .description(Text.of("Lists permissions for the claim you're standing i"))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_PLAYER_INFO_BASE)
                .description(Text.of("Gets information about a player such as claim blocks and options."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_RELOAD)
                .description(Text.of("Reloads GriefPrevention's configuration, messages, and player options."))
                .assign(PermissionDescription.ROLE_ADMIN, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_REMOVE_TRUST)
                .description(Text.of("Revokes a player's access to your claim."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_RESTORE_NATURE)
                .description(Text.of("Switches the shovel tool to restoration mode."))
                .assign(PermissionDescription.ROLE_ADMIN, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_RESTORE_NATURE_AGGRESSIVE)
                .description(Text.of("Switches the shovel tool to aggressive restoration mode."))
                .assign(PermissionDescription.ROLE_ADMIN, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_RESTORE_NATURE_FILL)
                .description(Text.of("Switches the shovel tool to fill mode."))
                .assign(PermissionDescription.ROLE_ADMIN, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_SELL_CLAIM_BLOCKS)
                .description(Text.of("Sell your claim blocks for server money. Doesn't work on servers without a compatible "
                        + "economy plugin."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_SEPARATE_PLAYERS)
                .description(Text.of("Forces two players to ignore each other in chat."))
                .assign(PermissionDescription.ROLE_ADMIN, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_SET_ACCRUED_CLAIM_BLOCKS)
                .description(Text.of("Updates a player's accrued claim block total."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_SET_CLAIM_FAREWELL)
                .description(Text.of("Sets the farewell message of your claim."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_SET_CLAIM_GREETING)
                .description(Text.of("Sets the greeting message of your claim."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_SET_CLAIM_NAME)
                .description(Text.of("Sets the name of your claim."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_SIEGE)
                .description(Text.of("Initiates a siege versus another player."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_SOFT_MUTE_PLAYER)
                .description(Text.of("Toggles whether a player's messages will only reach other soft-muted players."))
                .assign(PermissionDescription.ROLE_ADMIN, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_SUBDIVIDE_CLAIMS)
                .description(Text.of("Switches the shovel tool to subdivision mode, used to subdivide your claims."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_SUBDIVISION_INHERIT)
                .description(Text.of("Toggles inheritance from parent claim."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_UNBAN_ITEM)
                .description(Text.of("Unbans the specified item id or item in hand if no id is specified."))
                .assign(PermissionDescription.ROLE_ADMIN, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_UNIGNORE_PLAYER)
                .description(Text.of("Unignores another player's chat messages."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_UNLOCK_DROPS)
                .description(Text.of("Allows other players to pick up the items you dropped when you died."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_UNSEPARATE_PLAYERS)
                .description(Text.of("Reverses /separate."))
                .assign(PermissionDescription.ROLE_ADMIN, true)
                .register();
            this.permissionDescriptionBuilder
                .id(GPPermissions.COMMAND_TRANSFER_CLAIM)
                .description(Text.of("Transfers a basic or admin claim to another player."))
                .assign(PermissionDescription.ROLE_USER, true)
                .register();
        }
        instance = this;
        pluginCause = Cause.of(NamedCause.source(this.pluginContainer));
        this.getLogger().info("Grief Prevention boot start.");
        this.getLogger().info("Finished loading configuration.");

        GLOBAL_SUBJECT = GriefPreventionPlugin.instance.permissionService.getDefaults();
        // register with the LP API
        this.getLogger().info("Registering GriefPrevention API...");
        this.api = new GPApiProvider();
        RegistryHelper.setFinalStatic(GriefPrevention.class, "api", this.api);
        Sponge.getGame().getServiceManager().setProvider(this.pluginContainer, GriefPreventionApi.class, this.api);
    }

    @Listener
    public void onServerAboutToStart(GameAboutToStartServerEvent event) {
        if (!validateSpongeVersion()) {
            return;
        }

        this.loadConfig();
        this.customLogger = new CustomLogger();
        this.economyService = Sponge.getServiceManager().provide(EconomyService.class);

        if (this.dataStore == null) {
            try {
                this.dataStore = new FlatFileDataStore();
                this.dataStore.initialize();
            } catch (Exception e) {
                this.getLogger().info("Unable to initialize the file system data store.  Details:");
                this.getLogger().info(e.getMessage());
                e.printStackTrace();
                return;
            }
        }

        String dataMode = (this.dataStore instanceof FlatFileDataStore) ? "(File Mode)" : "(Database Mode)";
        Sponge.getGame().getEventManager().registerListeners(this, new BlockEventHandler(dataStore));
        Sponge.getGame().getEventManager().registerListeners(this, new PlayerEventHandler(dataStore, this));
        Sponge.getGame().getEventManager().registerListeners(this, new EntityEventHandler(dataStore));
        Sponge.getGame().getEventManager().registerListeners(this, new WorldEventHandler());
        addLogEntry("Finished loading data " + dataMode + ".");
        if (Sponge.getServiceManager().getRegistration(PermissionService.class).get().getPlugin().getId().equalsIgnoreCase("sponge")) {
            this.logger.error("Unable to initialize plugin. GriefPrevention requires one of the following permission plugins : LuckPerms, PEX, or PermissionsManager.");
            return;
        }

        PUBLIC_USER = Sponge.getServiceManager().provide(UserStorageService.class).get()
                .getOrCreate(GameProfile.of(GriefPreventionPlugin.PUBLIC_UUID, GriefPreventionPlugin.PUBLIC_NAME));
        WORLD_USER = Sponge.getServiceManager().provide(UserStorageService.class).get()
                .getOrCreate(GameProfile.of(GriefPreventionPlugin.WORLD_USER_UUID, GriefPreventionPlugin.WORLD_USER_NAME));
        // unless claim block accrual is disabled, start the recurring per 10
        // minute event to give claim blocks to online players
        DeliverClaimBlocksTask task = new DeliverClaimBlocksTask(null);
        Sponge.getGame().getScheduler().createTaskBuilder().interval(5, TimeUnit.MINUTES).execute(task)
                .submit(GriefPreventionPlugin.instance);

        // run cleanup task
        int cleanupTaskInterval = GriefPreventionPlugin.getGlobalConfig().getConfig().claim.cleanupTaskInterval;
        if (cleanupTaskInterval > 0) {
            CleanupUnusedClaimsTask cleanupTask = new CleanupUnusedClaimsTask();
            Sponge.getGame().getScheduler().createTaskBuilder().delay(cleanupTaskInterval, TimeUnit.MINUTES).execute(cleanupTask)
                    .submit(GriefPreventionPlugin.instance);
        }

        // if economy is enabled
        if (this.economyService.isPresent()) {
            GriefPreventionPlugin.addLogEntry("GriefPrevention economy integration enabled.");
            GriefPreventionPlugin.addLogEntry(
                    "Hooked into economy: " + Sponge.getServiceManager().getRegistration(EconomyService.class).get().getPlugin().getId() + ".");
            GriefPreventionPlugin.addLogEntry("Ready to buy/sell claim blocks!");
        }

        // load ignore lists for any already-online players
        Collection<Player> players = Sponge.getGame().getServer().getOnlinePlayers();
        for (Player player : players) {
            new IgnoreLoaderThread(player.getUniqueId(), this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId()).ignoredPlayers)
                    .start();
        }

        // TODO - rewrite /gp command
        //Sponge.getGame().getCommandManager().register(this, CommandGriefPrevention.getCommand().getCommandSpec(),
               //CommandGriefPrevention.getCommand().getAliases());
        registerBaseCommands();
        this.dataStore.loadClaimTemplates();
        addLogEntry("Boot finished.");
        this.logger.info("Loaded successfully.");
    }

    // handles sub commands
    public void registerBaseCommands() {

        ImmutableMap.Builder<String, String> flagChoicesBuilder = ImmutableMap.builder();
        for (String flag: GPFlags.FLAG_LIST) {
            flagChoicesBuilder.put(flag, flag);
        }

        ImmutableMap<String, String> flagChoices = flagChoicesBuilder.build();

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Grants a player entry to your claim(s) and use of your bed"))
                .permission(GPPermissions.COMMAND_GIVE_ACCESS_TRUST)
                .arguments(GenericArguments.firstParsing(
                        user(Text.of("user")),
                        string(Text.of("group"))))
                .executor(new CommandAccessTrust())
                .build(), "accesstrust", "at");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Adds or subtracts bonus claim blocks for a player"))
                .permission(GPPermissions.COMMAND_ADJUST_CLAIM_BLOCKS)
                .arguments(user(Text.of("user")), integer(Text.of("amount")), optional(GenericArguments.world(Text.of("world"))))
                .executor(new CommandAdjustBonusClaimBlocks())
                .build(), "adjustbonusclaimblocks", "acb");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Abandons a claim"))
                .permission(GPPermissions.COMMAND_ABANDON_CLAIM)
                .executor(new CommandClaimAbandon(false))
                .build(), "abandonclaim");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Abandons ALL your claims"))
                .permission(GPPermissions.COMMAND_ABANDON_ALL_CLAIMS)
                .executor(new CommandClaimAbandonAll())
                .build(), "abandonallclaims");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Abandons a claim and all its subdivisions"))
                .permission(GPPermissions.COMMAND_ABANDON_TOP_LEVEL_CLAIM)
                .executor(new CommandClaimAbandon(true))
                .build(), "abandontoplevelclaim");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Switches the shovel tool to administrative claims mode"))
                .permission(GPPermissions.COMMAND_ADMIN_CLAIMS)
                .executor(new CommandClaimAdmin())
                .build(), "adminclaims", "ac");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("List all administrative claims"))
                .permission(GPPermissions.COMMAND_LIST_ADMIN_CLAIMS)
                .arguments(optional(GenericArguments.world(Text.of("world"))))
                .executor(new CommandClaimAdminList())
                .build(), "adminclaimlist", "adminclaimslist", "claimadminlist");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Bans the specified item id or item in hand if no id is specified."))
                .permission(GPPermissions.COMMAND_BAN_ITEM)
                .arguments(optional(catalogedElement(Text.of("item"), ItemType.class)))
                .executor(new CommandClaimBanItem())
                .build(), "banitem");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Switches the shovel tool back to basic claims mode"))
                .permission(GPPermissions.COMMAND_BASIC_MODE)
                .executor(new CommandClaimBasic())
                .build(), "basicclaims", "bc");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Gives a player a manual about claiming land"))
                .permission(GPPermissions.COMMAND_GIVE_BOOK)
                .arguments(playerOrSource(Text.of("player")))
                .executor(new CommandClaimBook())
                .build(), "claimbook");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Sets the farewell message of your claim"))
                .permission(GPPermissions.COMMAND_SET_CLAIM_FAREWELL)
                .arguments(string(Text.of("message")))
                .executor(new CommandClaimFarewell())
                .build(), "claimfarewell");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Sets the greeting message of your claim"))
                .permission(GPPermissions.COMMAND_SET_CLAIM_GREETING)
                .arguments(string(Text.of("message")))
                .executor(new CommandClaimGreeting())
                .build(), "claimgreeting");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Teleports you to claim spawn if available."))
                .permission(GPPermissions.COMMAND_CLAIM_SPAWN)
                .executor(new CommandClaimSpawn())
                .build(), "claimspawn");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Sets the spawn of claim."))
                .permission(GPPermissions.COMMAND_CLAIM_SET_SPAWN)
                .executor(new CommandClaimSetSpawn())
                .build(), "claimsetspawn");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Purchases additional claim blocks with server money. Doesn't work on servers without a vault-compatible "
                        + "economy plugin"))
                .permission(GPPermissions.COMMAND_BUY_CLAIM_BLOCKS)
                .arguments(optional(integer(Text.of("numberOfBlocks"))))
                .executor(new CommandClaimBuy())
                .build(), "buyclaimblocks", "buyclaim");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Toggles cuboid claims mode"))
                .permission(GPPermissions.COMMAND_CUBOID_CLAIMS)
                .executor(new CommandClaimCuboid())
                .build(), "cuboidclaims", "cuboid");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Deletes the claim you're standing in, even if it's not your claim"))
                .permission(GPPermissions.COMMAND_DELETE_CLAIM_BASE)
                .executor(new CommandClaimDelete())
                .build(), "deleteclaim", "dc");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Delete all of another player's claims"))
                .permission(GPPermissions.COMMAND_DELETE_CLAIMS)
                .arguments(user(Text.of("player")))
                .executor(new CommandClaimDeleteAll())
                .build(), "deleteallclaims", "dac");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Deletes all administrative claims"))
                .permission(GPPermissions.COMMAND_DELETE_ADMIN_CLAIMS)
                .executor(new CommandClaimDeleteAllAdmin())
                .build(), "deletealladminclaims");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Toggles claim flag debug mode"))
                .permission(GPPermissions.COMMAND_FLAGS_DEBUG)
                .executor(new CommandClaimFlagDebug())
                .build(), "claimflagdebug", "cfd");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Resets a claim to flag defaults"))
                .permission(GPPermissions.COMMAND_FLAGS_RESET)
                .executor(new CommandClaimFlagReset())
                .build(), "claimflagreset", "cfr");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Toggles ignore claims mode"))
                .permission(GPPermissions.COMMAND_IGNORE_CLAIMS)
                .executor(new CommandClaimIgnore())
                .build(), "ignoreclaims", "ic");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Toggles subdivision inherit mode"))
                .permission(GPPermissions.COMMAND_SUBDIVISION_INHERIT)
                .executor(new CommandClaimInherit())
                .build(), "inheritpermissions", "inherit");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("List information about a player's claim blocks and claims"))
                .permission(GPPermissions.COMMAND_LIST_CLAIMS)
                .arguments(GenericArguments.firstParsing(
                        GenericArguments.seq(
                                GenericArguments.user(Text.of("user")),
                                onlyOne(GenericArguments.world(Text.of("world")))),
                        GenericArguments.user(Text.of("user")),
                        optional(onlyOne(GenericArguments.world(Text.of("world"))))))
                .executor(new CommandClaimList())
                .build(), "claimlist", "claimslist");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Allows clearing of entities within one or more claims."))
                .permission(GPPermissions.COMMAND_CLAIM_CLEAR)
                .arguments(onlyOne(string(Text.of("target"))),
                        optional(//GenericArguments.firstParsing(
                            GenericArguments.seq(
                                    onlyOne(string(Text.of("claim"))),
                                    optional(onlyOne(GenericArguments.world(Text.of("world")))))))
                            //string(Text.of("claim")))))
                .executor(new CommandClaimClear())
                .build(), "claimclear", "claimclear");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Gets information about a claim"))
                .permission(GPPermissions.COMMAND_CLAIM_INFO_BASE)
                .arguments(optional(string(Text.of("id"))))
                .executor(new CommandClaimInfo())
                .build(), "claiminfo", "claimsinfo");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Sell your claim blocks for server money. Doesn't work on servers without a vault-compatible "
                        + "economy plugin"))
                .permission(GPPermissions.COMMAND_SELL_CLAIM_BLOCKS)
                .arguments(optional(integer(Text.of("numberOfBlocks"))))
                .executor(new CommandClaimSell())
                .build(), "sellclaimblocks", "sellclaim");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Switches the shovel tool to subdivision mode, used to subdivide your claims"))
                .permission(GPPermissions.COMMAND_SUBDIVIDE_CLAIMS)
                .executor(new CommandClaimSubdivide())
                .build(), "claimsubdivide", "subdivideclaims", "sc");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Converts an administrative claim to a private claim"))
                .arguments(user(Text.of("user")))
                .permission(GPPermissions.COMMAND_TRANSFER_CLAIM)
                .executor(new CommandClaimTransfer())
                .build(), "claimtransfer", "transferclaim");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Grants a player access to your claim's containers, crops, animals, bed, buttons, and levers"))
                .permission(GPPermissions.COMMAND_GIVE_CONTAINER_TRUST)
                .arguments(GenericArguments.firstParsing(
                        user(Text.of("user")),
                        string(Text.of("group"))))
                .executor(new CommandContainerTrust())
                .build(), "containertrust", "ct");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Allows a player to give away a pet they tamed"))
                .permission(GPPermissions.COMMAND_GIVE_PET)
                .arguments(GenericArguments
                        .firstParsing(GenericArguments.literal(Text.of("player"), "cancel"), player(Text.of("player"))))
                .executor(new CommandGivePet())
                .build(), "givepet");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Toggles debug"))
                .permission(GPPermissions.COMMAND_DEBUG)
                .arguments(GenericArguments.firstParsing(
                        GenericArguments.seq(user(Text.of("user")),
                                bool(Text.of("verbose"))),
                        optional(bool(Text.of("verbose")))))
                .executor(new CommandDebug())
                .build(), "gpdebug");

        // TODO - rewrite help command to list all commands with nice overlays showing help
        /*Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Lists detailed information on each command."))
                .permission(GPPermissions.COMMAND_HELP)
                .executor(new CommandHelp())
                .build(), "gphelp");*/

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Lists the players you're ignoring in chat"))
                .permission(GPPermissions.COMMAND_LIST_IGNORED_PLAYERS)
                .executor(new CommandIgnoredPlayerList())
                .build(), "ignoredplayerlist", "ignoredlist");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Ignores another player's chat messages"))
                .permission(GPPermissions.COMMAND_IGNORE_PLAYER)
                .arguments(onlyOne(player(Text.of("player"))))
                .executor(new CommandIgnorePlayer())
                .build(), "ignoreplayer", "ignore");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Sets a permission on a group with a claim context"))
                .permission(GPPermissions.COMMAND_CLAIM_PERMISSION_GROUP)
                .arguments(string(Text.of("group")), 
                        optional(GenericArguments.seq(string(Text.of("permission")), string(Text.of("value")))))
                .executor(new CommandClaimPermissionGroup())
                .build(), "claimpermissiongroup", "cpg");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Sets a permission on a player with a claim context"))
                .permission(GPPermissions.COMMAND_CLAIM_PERMISSION_PLAYER)
                .arguments(user(Text.of("user")),
                        optional(GenericArguments.seq(string(Text.of("permission")), string(Text.of("value")))))
                .executor(new CommandClaimPermissionPlayer())
                .build(), "claimpermissionplayer", "cpp");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Sets the name of your claim"))
                .permission(GPPermissions.COMMAND_SET_CLAIM_NAME)
                .arguments(string(Text.of("name")))
                .executor(new CommandClaimName())
                .build(), "claimname");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Reloads Grief Prevention's configuration settings"))
                .permission(GPPermissions.COMMAND_RELOAD)
                .executor(new CommandGpReload())
                .build(), "gpreload");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Grants a player permission to grant their level of permission to others"))
                .permission(GPPermissions.COMMAND_GIVE_PERMISSION_TRUST)
                .arguments(GenericArguments.firstParsing(
                        user(Text.of("user")),
                        string(Text.of("group"))))
                .executor(new CommandPermissionTrust())
                .build(), "permissiontrust", "pt");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Gets information about a player"))
                .permission(GPPermissions.COMMAND_PLAYER_INFO_BASE)
                .arguments(GenericArguments.firstParsing(
                        GenericArguments.seq(
                                GenericArguments.user(Text.of("user")),
                                onlyOne(GenericArguments.world(Text.of("world")))),
                        GenericArguments.user(Text.of("user")),
                        optional(onlyOne(GenericArguments.world(Text.of("world"))))))
                .executor(new CommandPlayerInfo())
                .build(), "playerinfo");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Switches the shovel tool to restoration mode"))
                .permission(GPPermissions.COMMAND_RESTORE_NATURE)
                .executor(new CommandRestoreNature())
                .build(), "restorenature", "rn");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Switches the shovel tool to aggressive restoration mode"))
                .permission(GPPermissions.COMMAND_RESTORE_NATURE_AGGRESSIVE)
                .executor(new CommandRestoreNatureAggressive())
                .build(), "restorenatureaggressive", "rna");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Switches the shovel tool to fill mode"))
                .permission(GPPermissions.COMMAND_RESTORE_NATURE_FILL)
                .arguments(optional(integer(Text.of("radius")), 2))
                .executor(new CommandRestoreNatureFill())
                .build(), "restorenaturefill", "rnf");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Forces two players to ignore each other in chat"))
                .permission(GPPermissions.COMMAND_SEPARATE_PLAYERS)
                .arguments(onlyOne(player(Text.of("player1"))), onlyOne(player(Text.of("player2"))))
                .executor(new CommandSeparate())
                .build(), "separate");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Updates a player's accrued claim block total"))
                .permission(GPPermissions.COMMAND_SET_ACCRUED_CLAIM_BLOCKS)
                .arguments(user(Text.of("user")), integer(Text.of("amount")), optional(GenericArguments.world(Text.of("world"))))
                .executor(new CommandSetAccruedClaimBlocks())
                .build(), "setaccruedclaimblocks", "scb");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Initiates a siege versus another player"))
                .arguments(optional(onlyOne(player(Text.of("playerName")))))
                .permission(GPPermissions.COMMAND_SIEGE)
                .executor(new CommandSiege())
                .build(), "siege");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Toggles whether a player's messages will only reach other soft-muted players"))
                .permission(GPPermissions.COMMAND_SOFT_MUTE_PLAYER)
                .arguments(onlyOne(user(Text.of("player"))))
                .executor(new CommandSoftMute())
                .build(), "softmute");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Grants a player edit access to your claim(s)"))
                .extendedDescription(Text.of("Grants a player edit access to your claim(s).\n"
                        + "See also /untrust, /containertrust, /accesstrust, and /permissiontrust."))
                .permission(GPPermissions.COMMAND_GIVE_BUILDER_TRUST)
                .arguments(GenericArguments.firstParsing(
                        user(Text.of("user")),
                        string(Text.of("group"))))
                .executor(new CommandTrust())
                .build(), "trust", "t");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Gives a player or group access to all your claims"))
                .permission(GPPermissions.COMMAND_TRUST_ALL)
                .arguments(GenericArguments.firstParsing(
                        user(Text.of("user")),
                        string(Text.of("group"))))
                .executor(new CommandTrustAll()).build(), Arrays.asList("trustall", "ta"));

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Lists permissions for the claim you're standing in"))
                .permission(GPPermissions.COMMAND_LIST_TRUST)
                .executor(new CommandTrustList())
                .build(), "trustlist");

        /*Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Exports/Imports claim data."))
                .permission(GPPermissions.CLAIM_TEMPLATES_ADMIN)
                .arguments(string(Text.of("action")), optional(string(Text.of("name"))), optional(string(Text.of("description"))))
                .executor(new CommandClaimTemplate())
                .build(), "claimtemplate");*/

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Unbans the specified item id or item in hand if no id is specified."))
                .permission(GPPermissions.COMMAND_UNBAN_ITEM)
                .arguments(optional(catalogedElement(Text.of("item"), ItemType.class)))
                .executor(new CommandClaimUnbanItem())
                .build(), "unbanitem");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Unignores another player's chat messages"))
                .permission(GPPermissions.COMMAND_UNIGNORE_PLAYER)
                .arguments(onlyOne(user(Text.of("player"))))
                .executor(new CommandUnignorePlayer())
                .build(), "unignoreplayer", "unignore");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Allows other players to pick up the items you dropped when you died"))
                .permission(GPPermissions.COMMAND_UNLOCK_DROPS)
                .executor(new CommandUnlockDrops())
                .build(), "unlockdrops");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Reverses /separate"))
                .permission(GPPermissions.COMMAND_UNSEPARATE_PLAYERS)
                .arguments(onlyOne(player(Text.of("player1"))), onlyOne(player(Text.of("player2"))))
                .executor(new CommandUnseparate())
                .build(), "unseparate");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Revokes a player's access to your claim"))
                .permission(GPPermissions.COMMAND_REMOVE_TRUST)
                .arguments(GenericArguments.firstParsing(
                        user(Text.of("user")),
                        string(Text.of("group"))))
                .executor(new CommandUntrust()).build(), Arrays.asList("untrust", "ut"));

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Revokes a player's access to all your claims"))
                .permission(GPPermissions.COMMAND_REMOVE_TRUST)
                .arguments(GenericArguments.firstParsing(
                        user(Text.of("user")),
                        string(Text.of("group"))))
                .executor(new CommandUntrustAll()).build(), Arrays.asList("untrustall", "uta"));

        Map<String, String> catalogMap = Maps.newHashMap();
        for (BlockType blockType : Sponge.getRegistry().getAllOf(BlockType.class)) {
            String modId = blockType.getId().split(":")[0].toLowerCase();
            if (modId.equals("none")) {
                continue;
            }
            if (!catalogMap.containsKey(modId + ":any")) {
                catalogMap.put(modId + ":any", modId + ":any");
            }
            catalogMap.put(blockType.getId(), blockType.getId());
        }

        for (EntityType entityType : Sponge.getRegistry().getAllOf(EntityType.class)) {
            String modId = entityType.getId().split(":")[0].toLowerCase();
            if (modId.equals("none")) {
                continue;
            }
            if (!catalogMap.containsKey(modId + ":any")) {
                catalogMap.put(modId + ":any", modId + ":any");
            }
            if (!catalogMap.containsKey(entityType.getId())) {
                catalogMap.put(entityType.getId(), entityType.getId());
            }
            if (!catalogMap.containsKey(modId + ":animal") && Living.class.isAssignableFrom(entityType.getEntityClass())) {
                catalogMap.put(modId + ":ambient", modId + ":ambient");
                catalogMap.put(modId + ":animal", modId + ":animal");
                catalogMap.put(modId + ":aquatic", modId + ":aquatic");
                catalogMap.put(modId + ":monster", modId + ":monster");
            }
        }

        for (ItemType itemType : Sponge.getRegistry().getAllOf(ItemType.class)) {
            String modId = itemType.getId().split(":")[0].toLowerCase();
            if (modId.equals("none")) {
                continue;
            }
            if (!catalogMap.containsKey(modId + ":any")) {
                catalogMap.put(modId + ":any", modId + ":any");
            }
            if (!catalogMap.containsKey(itemType.getId())) {
                catalogMap.put(itemType.getId(), itemType.getId());
            }
        }
        // commands
        Set<? extends CommandMapping> commandList = Sponge.getCommandManager().getCommands();
        for (CommandMapping command : commandList) {
            PluginContainer pluginContainer = Sponge.getCommandManager().getOwner(command).orElse(null);
            if (pluginContainer != null) {
                for (String alias : command.getAllAliases()) {
                    String[] parts = alias.split(":");
                    if (parts.length > 1) {
                        catalogMap.put(alias, alias);
                    }
                }
            }
        }
        catalogMap.put("griefprevention:cf", "griefprevention:cf");
        catalogMap.put("griefprevention:cfg", "griefprevention:cfg");
        catalogMap.put("griefprevention:cfp", "griefprevention:cfp");
        catalogMap.put("griefprevention:cpg", "griefprevention:cpg");
        catalogMap.put("griefprevention:cpp", "griefprevention:cpp");
        catalogMap.put("griefprevention:claimflag", "griefprevention:claimflag");
        catalogMap.put("griefprevention:claimflaggroup", "griefprevention:claimflaggroup");
        catalogMap.put("griefprevention:claimflagplayer", "griefprevention:claimflagplayer");
        catalogMap.put("any", "any");
        ImmutableMap<String, String> catalogChoices = ImmutableMap.copyOf(catalogMap);

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Gets/Sets claim flags in the claim you are standing in"))
                .permission(GPPermissions.COMMAND_FLAGS_CLAIM)
                .arguments(GenericArguments.firstParsing(
                        optional(GenericArguments.seq(
                            choices(Text.of("flag"), flagChoices),
                            GenericArguments.firstParsing(
                                GenericArguments.seq(
                                    choices(Text.of("target"), catalogChoices),
                                    onlyOne(GenericArguments.choices(Text.of("value"), ImmutableMap.<String, Tristate>builder()
                                            .put("-1", Tristate.FALSE)
                                            .put("0", Tristate.UNDEFINED)
                                            .put("1", Tristate.TRUE)
                                            .put("false", Tristate.FALSE)
                                            .put("undefined", Tristate.UNDEFINED)
                                            .put("true", Tristate.TRUE)
                                            .build())),
                                    optional(GenericArguments.onlyOne(GenericArguments.string(Text.of("context"))))),
                                GenericArguments.seq(
                                    onlyOne(choices(Text.of("source"), catalogChoices)),
                                    choices(Text.of("target"), catalogChoices),
                                    onlyOne(GenericArguments.choices(Text.of("value"), ImmutableMap.<String, Tristate>builder()
                                            .put("-1", Tristate.FALSE)
                                            .put("0", Tristate.UNDEFINED)
                                            .put("1", Tristate.TRUE)
                                            .put("false", Tristate.FALSE)
                                            .put("undefined", Tristate.UNDEFINED)
                                            .put("true", Tristate.TRUE)
                                            .build())),
                                    optional(GenericArguments.onlyOne(GenericArguments.string(Text.of("context"))))),
                                GenericArguments.seq(
                                        string(Text.of("target")),
                                        onlyOne(GenericArguments.choices(Text.of("value"), ImmutableMap.<String, Tristate>builder()
                                                .put("-1", Tristate.FALSE)
                                                .put("0", Tristate.UNDEFINED)
                                                .put("1", Tristate.TRUE)
                                                .put("false", Tristate.FALSE)
                                                .put("undefined", Tristate.UNDEFINED)
                                                .put("true", Tristate.TRUE)
                                                .build())),
                                        optional(GenericArguments.onlyOne(GenericArguments.string(Text.of("context"))))),
                                GenericArguments.seq(
                                        onlyOne(choices(Text.of("source"), catalogChoices)),
                                        string(Text.of("target")),
                                        onlyOne(GenericArguments.choices(Text.of("value"), ImmutableMap.<String, Tristate>builder()
                                                .put("-1", Tristate.FALSE)
                                                .put("0", Tristate.UNDEFINED)
                                                .put("1", Tristate.TRUE)
                                                .put("false", Tristate.FALSE)
                                                .put("undefined", Tristate.UNDEFINED)
                                                .put("true", Tristate.TRUE)
                                                .build())),
                                        optional(GenericArguments.onlyOne(GenericArguments.string(Text.of("context"))))))))))
                .executor(new CommandClaimFlag())
                .build(), "claimflag", "cf");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Gets/Sets flag permission for a group in claim you are standing in."))
                .permission(GPPermissions.COMMAND_FLAGS_GROUP)
                .arguments(GenericArguments.firstParsing(
                        GenericArguments.seq(
                            onlyOne(string(Text.of("group"))),
                            GenericArguments.firstParsing(
                                GenericArguments.seq(
                                    choices(Text.of("flag"), flagChoices),
                                    GenericArguments.firstParsing(
                                            GenericArguments.seq(
                                                choices(Text.of("target"), catalogChoices),
                                                onlyOne(GenericArguments.choices(Text.of("value"), ImmutableMap.<String, Tristate>builder()
                                                        .put("-1", Tristate.FALSE)
                                                        .put("0", Tristate.UNDEFINED)
                                                        .put("1", Tristate.TRUE)
                                                        .put("false", Tristate.FALSE)
                                                        .put("undefined", Tristate.UNDEFINED)
                                                        .put("true", Tristate.TRUE)
                                                        .build()))),
                                            GenericArguments.seq(
                                                onlyOne(choices(Text.of("source"), catalogChoices)),
                                                choices(Text.of("target"), catalogChoices),
                                                onlyOne(GenericArguments.choices(Text.of("value"), ImmutableMap.<String, Tristate>builder()
                                                        .put("-1", Tristate.FALSE)
                                                        .put("0", Tristate.UNDEFINED)
                                                        .put("1", Tristate.TRUE)
                                                        .put("false", Tristate.FALSE)
                                                        .put("undefined", Tristate.UNDEFINED)
                                                        .put("true", Tristate.TRUE)
                                                        .build()))),
                                            GenericArguments.seq(
                                                    string(Text.of("target")),
                                                    onlyOne(GenericArguments.choices(Text.of("value"), ImmutableMap.<String, Tristate>builder()
                                                            .put("-1", Tristate.FALSE)
                                                            .put("0", Tristate.UNDEFINED)
                                                            .put("1", Tristate.TRUE)
                                                            .put("false", Tristate.FALSE)
                                                            .put("undefined", Tristate.UNDEFINED)
                                                            .put("true", Tristate.TRUE)
                                                            .build()))),
                                            GenericArguments.seq(
                                                    onlyOne(choices(Text.of("source"), catalogChoices)),
                                                    string(Text.of("target")),
                                                    onlyOne(GenericArguments.choices(Text.of("value"), ImmutableMap.<String, Tristate>builder()
                                                            .put("-1", Tristate.FALSE)
                                                            .put("0", Tristate.UNDEFINED)
                                                            .put("1", Tristate.TRUE)
                                                            .put("false", Tristate.FALSE)
                                                            .put("undefined", Tristate.UNDEFINED)
                                                            .put("true", Tristate.TRUE)
                                                            .build())))))))))
                .executor(new CommandClaimFlagGroup())
                .build(), "claimflaggroup", "cfg");

        Sponge.getCommandManager().register(this, CommandSpec.builder()
                .description(Text.of("Adds flag permission to player."))
                .permission(GPPermissions.COMMAND_FLAGS_PLAYER)
                .arguments(GenericArguments.firstParsing(
                        GenericArguments.seq(
                            onlyOne(string(Text.of("player"))),
                            GenericArguments.firstParsing(
                                GenericArguments.seq(
                                    choices(Text.of("flag"), flagChoices),
                                    GenericArguments.firstParsing(
                                            GenericArguments.seq(
                                                choices(Text.of("target"), catalogChoices),
                                                onlyOne(GenericArguments.choices(Text.of("value"), ImmutableMap.<String, Tristate>builder()
                                                        .put("-1", Tristate.FALSE)
                                                        .put("0", Tristate.UNDEFINED)
                                                        .put("1", Tristate.TRUE)
                                                        .put("false", Tristate.FALSE)
                                                        .put("undefined", Tristate.UNDEFINED)
                                                        .put("true", Tristate.TRUE)
                                                        .build()))),
                                            GenericArguments.seq(
                                                    onlyOne(choices(Text.of("source"), catalogChoices)),
                                                    choices(Text.of("target"), catalogChoices),
                                                    onlyOne(GenericArguments.choices(Text.of("value"), ImmutableMap.<String, Tristate>builder()
                                                            .put("-1", Tristate.FALSE)
                                                            .put("0", Tristate.UNDEFINED)
                                                            .put("1", Tristate.TRUE)
                                                            .put("false", Tristate.FALSE)
                                                            .put("undefined", Tristate.UNDEFINED)
                                                            .put("true", Tristate.TRUE)
                                                            .build()))),
                                            GenericArguments.seq(
                                                    string(Text.of("target")),
                                                    onlyOne(GenericArguments.choices(Text.of("value"), ImmutableMap.<String, Tristate>builder()
                                                            .put("-1", Tristate.FALSE)
                                                            .put("0", Tristate.UNDEFINED)
                                                            .put("1", Tristate.TRUE)
                                                            .put("false", Tristate.FALSE)
                                                            .put("undefined", Tristate.UNDEFINED)
                                                            .put("true", Tristate.TRUE)
                                                            .build()))),
                                            GenericArguments.seq(
                                                    onlyOne(choices(Text.of("source"), catalogChoices)),
                                                    string(Text.of("target")),
                                                    onlyOne(GenericArguments.choices(Text.of("value"), ImmutableMap.<String, Tristate>builder()
                                                            .put("-1", Tristate.FALSE)
                                                            .put("0", Tristate.UNDEFINED)
                                                            .put("1", Tristate.TRUE)
                                                            .put("false", Tristate.FALSE)
                                                            .put("undefined", Tristate.UNDEFINED)
                                                            .put("true", Tristate.TRUE)
                                                            .build())))))))))
                .executor(new CommandClaimFlagPlayer())
                .build(), "claimflagplayer", "cfp");
    }

    public void loadConfig() {
        try {
            if (Files.notExists(DataStore.dataLayerFolderPath)) {
                Files.createDirectories(DataStore.dataLayerFolderPath);
            }
            if (Files.notExists(DataStore.messagesFilePath)) {
                Files.createFile(DataStore.messagesFilePath);
            }
            if (this.dataStore != null) {
                this.dataStore.loadMessages();
            }
            if (Files.notExists(DataStore.bannedWordsFilePath)) {
                if (this.dataStore != null) {
                    this.dataStore.loadBannedWords();
                }
            }
            if (Files.notExists(DataStore.softMuteFilePath)) {
                Files.createFile(DataStore.softMuteFilePath);
            }

            Path rootConfigPath = this.getConfigPath().resolve("worlds");
            DataStore.globalConfig = new GriefPreventionConfig<GlobalConfig>(Type.GLOBAL, rootConfigPath.resolve("global.conf"));
            DataStore.USE_GLOBAL_PLAYER_STORAGE = DataStore.globalConfig.getConfig().playerdata.useGlobalPlayerDataStorage;
            this.modificationTool = Sponge.getRegistry().getType(ItemType.class, DataStore.globalConfig.getConfig().claim.modificationTool).orElse(ItemTypes.NONE);
            this.investigationTool = Sponge.getRegistry().getType(ItemType.class, DataStore.globalConfig.getConfig().claim.investigationTool).orElse(ItemTypes.NONE);
            this.maxInspectionDistance = DataStore.globalConfig.getConfig().general.maxClaimInspectionDistance;
            for (World world : Sponge.getGame().getServer().getWorlds()) {
                DimensionType dimType = world.getProperties().getDimensionType();
                Path dimPath = rootConfigPath.resolve(((IMixinDimensionType) dimType).getModId()).resolve(((IMixinDimensionType) dimType).getEnumName());
                if (!Files.exists(dimPath.resolve(world.getProperties().getWorldName()))) {
                    try {
                        Files.createDirectories(rootConfigPath.resolve(dimType.getId()).resolve(world.getName()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                DataStore.dimensionConfigMap.put(world.getProperties().getUniqueId(),
                        new GriefPreventionConfig<DimensionConfig>(Type.DIMENSION, dimPath.resolve("dimension.conf")));
                DataStore.worldConfigMap.put(world.getProperties().getUniqueId(), new GriefPreventionConfig<>(Type.WORLD,
                        dimPath.resolve(world.getProperties().getWorldName()).resolve("world.conf")));

                // refresh player data
                for (GPClaimManager claimWorldManager : GriefPreventionPlugin.instance.dataStore.claimWorldManagers.values()) {
                    for (GPPlayerData playerData : claimWorldManager.getPlayerDataList().values()) {
                        playerData.refreshPlayerOptions();
                    }
                }
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

    public void setIgnoreStatus(World world, User ignorer, User ignoree, IgnoreMode mode) {
        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(world, ignorer.getUniqueId());
        if (mode == IgnoreMode.None) {
            playerData.ignoredPlayers.remove(ignoree.getUniqueId());
        } else {
            playerData.ignoredPlayers.put(ignoree.getUniqueId(), mode == IgnoreMode.StandardIgnore ? false : true);
        }

        playerData.ignoreListChanged = true;
        if (!ignorer.isOnline()) {
            this.dataStore.asyncSaveGlobalPlayerData(ignorer.getUniqueId(), playerData);
            this.dataStore.clearCachedPlayerData(world.getProperties(), ignorer.getUniqueId());
        }
    }

    public enum IgnoreMode {
        None, StandardIgnore, AdminIgnore
    }

    public String trustEntryToPlayerName(String entry) {
        if (entry.startsWith("[") || entry.equals("public")) {
            return entry;
        } else {
            return PlayerUtils.lookupPlayerName(entry);
        }
    }

    public static String getfriendlyLocationString(Location<World> location) {
        return location.getExtent().getName() + ": x" + location.getBlockX() + ", z" + location.getBlockZ();
    }

    // called when a player spawns, applies protection for that player if necessary
    public void checkPvpProtectionNeeded(Player player) {
        // if anti spawn camping feature is not enabled, do nothing
        if (!GriefPreventionPlugin.getActiveConfig(player.getWorld().getProperties()).getConfig().pvp.protectFreshSpawns) {
            return;
        }

        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GPClaim claim = this.dataStore.getClaimAtPlayer(playerData, player.getLocation(), false);
        // if pvp rules are disabled in claim, do nothing
        if (!claim.pvpRulesApply()) {
            return;
        }

        // if player is in creative mode, do nothing
        if (player.get(Keys.GAME_MODE).get() == GameModes.CREATIVE) {
            return;
        }

        // if the player has the damage any player permission enabled, do nothing
        if (player.hasPermission(GPPermissions.NO_PVP_IMMUNITY)) {
            return;
        }

        // check inventory for well, anything
        if (GriefPreventionPlugin.isInventoryEmpty(player)) {
            // if empty, apply immunity
            playerData.pvpImmune = true;

            // inform the player after he finishes respawning
            GriefPreventionPlugin.sendMessage(player, TextMode.Success, Messages.PvPImmunityStart, 5L);

            // start a task to re-check this player's inventory every minute
            // until his immunity is gone
            PvPImmunityValidationTask task = new PvPImmunityValidationTask(player);
            Sponge.getGame().getScheduler().createTaskBuilder().delay(1, TimeUnit.MINUTES).execute(task).submit(this);
        }
    }

    public static boolean isInventoryEmpty(Player player) {
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
    public boolean ejectPlayer(Player player) {
        // look for a suitable location
        Location<World> candidateLocation = player.getLocation();
        while (true) {
            GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
            GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAtPlayer(playerData, player.getLocation(), false);

            // if there's a claim here, keep looking
            if (!claim.isWildernessClaim()) {
                candidateLocation = new Location<World>(claim.lesserBoundaryCorner.getExtent(), claim.lesserBoundaryCorner.getBlockX() - 1,
                        claim.lesserBoundaryCorner.getBlockY(), claim.lesserBoundaryCorner.getBlockZ() - 1);
                continue;
            }

            // otherwise find a safe place to teleport the player
            else {
                // find a safe height, a couple of blocks above the surface
                GuaranteeChunkLoaded(candidateLocation);
                return player.setLocationSafely(player.getLocation().add(0, 2, 0));
            }
        }
    }

    // ensures a piece of the managed world is loaded into server memory (generates the chunk if necessary)
    private static void GuaranteeChunkLoaded(Location<World> location) {
        location.getExtent().loadChunk(location.getBlockPosition(), true);
    }

    public static Text getMessage(Messages messageID, String... args) {
        return Text.of(GriefPreventionPlugin.instance.dataStore.getMessage(messageID, args));
    }

    public static void sendClaimDenyMessage(GPClaim claim, CommandSource player, TextColor color, Messages messageID, String... args) {
        if (claim.getData() != null && !claim.getData().allowDenyMessages()) {
            return;
        }

        sendMessage(player, color, messageID, args);
    }

    public static void sendClaimDenyMessage(GPClaim claim, CommandSource player, TextColor color, String message) {
        if (claim.getData() != null && !claim.getData().allowDenyMessages()) {
            return;
        }

        sendMessage(player, color, message);
    }

    public static void sendClaimDenyMessage(GPClaim claim, CommandSource source, Text message) {
        if (claim.getData() != null && !claim.getData().allowDenyMessages()) {
            return;
        }

        sendMessage(source, message);
    }

    // sends a color-coded message to a player
    public static void sendMessage(CommandSource player, TextColor color, Messages messageID, String... args) {
        sendMessage(player, color, messageID, 0, args);
    }

    // sends a color-coded message to a player
    public static void sendMessage(CommandSource player, TextColor color, Messages messageID, long delayInTicks, String... args) {
        sendMessage(player, GriefPreventionPlugin.instance.dataStore.parseMessage(messageID, color, args), delayInTicks);
    }

    public static void sendMessage(CommandSource player, TextColor color, String message) {
        sendMessage(player, Text.of(color, message));
    }

    // sends a color-coded message to a player
    public static void sendMessage(CommandSource source, Text message) {
        if (source instanceof Player && SpongeImplHooks.isFakePlayer((net.minecraft.entity.Entity) source)) {
            return;
        }
        if (message == Text.of() || message == null) {
            return;
        }

        if (source == null) {
            GriefPreventionPlugin.addLogEntry(Text.of(message).toPlain());
        } else {
            source.sendMessage(message);
        }
    }

    public static void sendMessage(CommandSource source, Text message, long delayInTicks) {
        if (source instanceof Player && SpongeImplHooks.isFakePlayer((net.minecraft.entity.Entity) source)) {
            return;
        }

        if (source instanceof Player) {
            SendPlayerMessageTask task = new SendPlayerMessageTask((Player) source, message);
            if (delayInTicks > 0) {
                Sponge.getGame().getScheduler().createTaskBuilder().delayTicks(delayInTicks).execute(task).submit(GriefPreventionPlugin.instance);
            } else {
                task.run();
            }
        } else {
            source.sendMessage(message);
        }
    }

    public static GriefPreventionConfig<?> getActiveConfig(WorldProperties worldProperties) {
        GriefPreventionConfig<WorldConfig> worldConfig = DataStore.worldConfigMap.get(worldProperties.getUniqueId());
        GriefPreventionConfig<DimensionConfig> dimConfig = DataStore.dimensionConfigMap.get(worldProperties.getUniqueId());
        if (worldConfig == null || worldConfig.getConfig() == null) {
            return DataStore.globalConfig;
        }
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
    public boolean claimsEnabledForWorld(WorldProperties worldProperties) {
        return GriefPreventionPlugin.getActiveConfig(worldProperties).getConfig().claim.claimMode != 0;
    }

    public boolean claimModeIsActive(WorldProperties worldProperties, ClaimsMode mode) {
        return GriefPreventionPlugin.getActiveConfig(worldProperties).getConfig().claim.claimMode == mode.ordinal();
    }

    public String allowBuild(Object source, Location<World> targetLocation, User user) {
        GPPlayerData playerData = null;
        if (user != null) {
            playerData = this.dataStore.getOrCreatePlayerData(targetLocation.getExtent(), user.getUniqueId());
        } else {
            GPClaim claim = this.dataStore.getClaimAt(targetLocation, false);
            if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.BLOCK_PLACE, source, targetLocation, user) == Tristate.FALSE) {
                return this.dataStore.getMessage(Messages.NoBuildPermission);
            }

            return null;
        }

        GPClaim claim = null;
        if (playerData != null) {
            claim = this.dataStore.getClaimAtPlayer(playerData, targetLocation);
        } else {
            claim = this.dataStore.getClaimAt(targetLocation);
        }

        // exception: administrators in ignore claims mode and special player accounts created by server mods
        if (playerData != null && playerData.canIgnoreClaim(claim)) {
            return null;
        }

        // cache the claim for later reference
        if (playerData != null) {
            playerData.lastClaim = new WeakReference<>(claim);
        }
        return claim.allowBuild(source, targetLocation, user);
    }

    // A blocksnapshot must be passed here instead of location as the block in world represents the "final" block
    public String allowBreak(Object source, BlockSnapshot blockSnapshot, User user) {
        Location<World> location = blockSnapshot.getLocation().orElse(null);
        if (location == null) {
            return null;
        }

        GPPlayerData playerData = user != null ? this.dataStore.getOrCreatePlayerData(location.getExtent(), user.getUniqueId()) : null;
        GPClaim claim = this.dataStore.getClaimAt(location);

        // exception: administrators in ignore claims mode
        if (user != null && (playerData.canIgnoreClaim(claim))) {
            return null;
        }

        // cache the claim for later reference
        if (playerData != null) {
            playerData.lastClaim = new WeakReference<>(claim);
        }

        // if not in the wilderness, then apply claim rules (permissions, etc)
        return claim.allowBreak(source, blockSnapshot, user);
    }

    // restores nature in multiple chunks, as described by a claim instance
    // this restores all chunks which have ANY number of claim blocks from this claim in them
    // if the claim is still active (in the data store), then the claimed blocks
    // will not be changed (only the area bordering the claim)
    public void restoreClaim(GPClaim claim, long delayInTicks) {
        // admin claims aren't automatically cleaned up when deleted or abandoned
        if (claim.isAdminClaim()) {
            return;
        }

        // it's too expensive to do this for huge claims
        if (claim.getArea() > 10000) {
            return;
        }

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
        Location<World> startLocation =
                new Location<World>(chunk.getWorld(), startBlock.getPosition().getX() - 1, 0, startBlock.getPosition().getZ() - 1);
        for (int x = 0; x < snapshots.length; x++) {
            for (int z = 0; z < snapshots[0][0].length; z++) {
                for (int y = 0; y < snapshots[0].length; y++) {
                    snapshots[x][y][z] = chunk.getWorld()
                            .createSnapshot(startLocation.getBlockX() + x, startLocation.getBlockY() + y, startLocation.getBlockZ() + z);
                }
            }
        }

        // create task to process those data in another thread
        Location<World> lesserBoundaryCorner = startBlock.getLocation().get();
        Location<World> greaterBoundaryCorner = chunk.createSnapshot(15, 0, 15).getLocation().get();

        // create task when done processing, this task will create a main thread task to actually update the world with processing results
        RestoreNatureProcessingTask task = new RestoreNatureProcessingTask(snapshots, miny, chunk.getWorld().getDimension().getType(),
                lesserBoundaryCorner.getBiome(), lesserBoundaryCorner, greaterBoundaryCorner, this.getSeaLevel(chunk.getWorld()),
                aggressiveMode, claimModeIsActive(lesserBoundaryCorner.getExtent().getProperties(), ClaimsMode.Creative),
                playerReceivingVisualization);
        Sponge.getGame().getScheduler().createTaskBuilder().async().delayTicks(delayInTicks).execute(task).submit(this);
    }

    @SuppressWarnings("unused")
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
                        meta = Integer.parseInt(blockInfo.substring(lastIndex + 1, blockInfo.length()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                blockInfo = blockInfo.substring(0, lastIndex);
            } else if (count > 2) {
                GriefPreventionPlugin.addLogEntry("ERROR: Invalid block entry " + blockInfo + " found in config. Skipping...");
                continue;
            }

            Optional<BlockType> blockType = Sponge.getGame().getRegistry().getType(BlockType.class, blockInfo);

            // null value returned indicates an error parsing the string from the config file
            if (!blockType.isPresent() || !blockType.get().getItem().isPresent()) {
                // show error in log
                GriefPreventionPlugin.addLogEntry("ERROR: Unable to read a block entry from the config file.  Please update your config.");

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
        return world.getDimension().getMinimumSpawnHeight();
    }

    public boolean containsBlockedIP(String message) {
        message = message.replace("\r\n", "");
        Pattern ipAddressPattern = Pattern.compile("([0-9]{1,3}\\.){3}[0-9]{1,3}");
        Matcher matcher = ipAddressPattern.matcher(message);

        // if it looks like an IP address
        if (matcher.find()) {
            // and it's not in the list of allowed IP addresses
            if (!GriefPreventionPlugin.getGlobalConfig().getConfig().spam.allowedIpAddresses.contains(matcher.group())) {
                return true;
            }
        }

        return false;
    }

    public static boolean isEntityProtected(Entity entity) {
        // ignore monsters
        if (SpongeImplHooks.isCreatureOfType((net.minecraft.entity.Entity) entity, EnumCreatureType.MONSTER)) {
            return false;
        }

        return true;
    }

    public static User getOrCreateUser(UUID uuid) {
        if (uuid == null) {
            return null;
        }

        if (uuid == WORLD_USER_UUID) {
            return WORLD_USER;
        }

        // check the cache
        Optional<User> player = Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(uuid);
        if (player.isPresent()) {
            return player.get();
        } else {
            try {
                GameProfile gameProfile = Sponge.getServer().getGameProfileManager().get(uuid).get();
                return Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().getOrCreate(gameProfile);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
