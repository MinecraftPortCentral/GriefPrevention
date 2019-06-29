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
package me.ryanhamshire.griefprevention.command;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import me.ryanhamshire.griefprevention.GPFlags;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimContexts;
import me.ryanhamshire.griefprevention.api.claim.ClaimFlag;
import me.ryanhamshire.griefprevention.api.claim.ClaimResult;
import me.ryanhamshire.griefprevention.api.claim.FlagResult;
import me.ryanhamshire.griefprevention.api.claim.FlagResultType;
import me.ryanhamshire.griefprevention.api.claim.TrustType;
import me.ryanhamshire.griefprevention.api.economy.BankTransactionType;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.claim.GPFlagResult;
import me.ryanhamshire.griefprevention.command.ClaimFlagBase.FlagType;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.configuration.MessageStorage;
import me.ryanhamshire.griefprevention.economy.GPBankTransaction;
import me.ryanhamshire.griefprevention.event.GPGroupTrustClaimEvent;
import me.ryanhamshire.griefprevention.event.GPUserTrustClaimEvent;
import me.ryanhamshire.griefprevention.permission.GPOptionHandler;
import me.ryanhamshire.griefprevention.permission.GPOptions;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import me.ryanhamshire.griefprevention.util.PermissionUtils;
import me.ryanhamshire.griefprevention.util.TaskUtils;
import me.ryanhamshire.griefprevention.visual.Visualization;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.data.property.entity.EyeLocationProperty;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandHelper {

    public static Player checkPlayer(CommandSource source) throws CommandException {
        if (source instanceof Player) {
            return ((Player) source);
        } else {
            throw new CommandException(Text.of("You must be a player to run this command!"));
        }
    }

    // helper method to resolve a player name from the player's UUID
    public static String lookupPlayerName(UUID playerID) {
        // parameter validation
        if (playerID == null) {
            return "somebody";
        }

        // check the cache
        Optional<User> player = Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(playerID);
        if (player.isPresent()) {
            return player.get().getName();
        } else {
            try {
                return Sponge.getServer().getGameProfileManager().get(playerID).get().getName().get();
            } catch (Exception e) {
                return "someone";
            }
        }
    }

    public static boolean validateFlagTarget(ClaimFlag flag, String target) {
        switch(flag) {
            case BLOCK_BREAK :
            case BLOCK_PLACE :
            case ENTITY_COLLIDE_BLOCK :
                if (validateBlockTarget(target) ||
                    validateItemTarget(target)) {
                    return true;
                }
                return false;
            case ENTER_CLAIM :
            case EXIT_CLAIM :
            case ENTITY_RIDING :
            case ENTITY_DAMAGE :
            case PORTAL_USE :
                if (validateEntityTarget(target) ||
                    validateBlockTarget(target) ||
                    validateItemTarget(target)) {
                    return true;
                }
                return false;
            case INTERACT_INVENTORY :
                if (validateEntityTarget(target) ||
                    validateBlockTarget(target)) {
                    return true;
                }
                return false;
            case INTERACT_BLOCK_PRIMARY :
            case INTERACT_BLOCK_SECONDARY :
            case LIQUID_FLOW :
                return validateBlockTarget(target);
            case ENTITY_CHUNK_SPAWN :
            case ENTITY_SPAWN :
            case INTERACT_ENTITY_PRIMARY :
            case INTERACT_ENTITY_SECONDARY :
                return validateEntityTarget(target);
            case ITEM_DROP :
            case ITEM_PICKUP :
            case ITEM_SPAWN :
            case ITEM_USE :
                return validateItemTarget(target);
            default :
                return true;
        }
    }

    private static boolean validateEntityTarget(String target) {
        Optional<EntityType> entityType = Sponge.getRegistry().getType(EntityType.class, target);
        if (entityType.isPresent()) {
            return true;
        }

        return false;
    }

    private static boolean validateItemTarget(String target) {
        Optional<ItemType> itemType = Sponge.getRegistry().getType(ItemType.class, target);
        if (itemType.isPresent()) {
            return true;
        }
        // target could be an item block, so validate blockstate
        Optional<BlockState> blockState = Sponge.getRegistry().getType(BlockState.class, target);
        if (blockState.isPresent()) {
            return true;
        }

        return false;
    }

    private static boolean validateBlockTarget(String target) {
        Optional<BlockType> blockType = Sponge.getRegistry().getType(BlockType.class, target);
        if (blockType.isPresent()) {
            return true;
        }

        Optional<BlockState> blockState = Sponge.getRegistry().getType(BlockState.class, target);
        if (blockState.isPresent()) {
            return true;
        }
        return false;
    }

    public static Context validateCustomContext(CommandSource src, GPClaim claim, String context) {
        if (context == null) {
            return null;
        }
        if (context.equalsIgnoreCase("default") || context.equalsIgnoreCase("defaults")) {
            return claim.getDefaultContext();
        } else if (context.equalsIgnoreCase("override") || context.equalsIgnoreCase("overrides") || context.equalsIgnoreCase("force") || context.equalsIgnoreCase("forced")) {
            return claim.getOverrideContext();
        } else if (context.equalsIgnoreCase("ban")) {
            return ClaimContexts.WILDERNESS_OVERRIDE_CONTEXT;
        } else {
            final Text message = GriefPreventionPlugin.instance.messageData.claimContextNotFound
                    .apply(ImmutableMap.of(
                    "context", context)).build();
            GriefPreventionPlugin.sendMessage(src, message);
            return null;
        }
    }

    public static FlagResult addFlagPermission(CommandSource src, Subject subject, String subjectName, GPClaim claim, ClaimFlag claimFlag, String source, String target, Tristate value, Context context, Text reason) {
        if (src instanceof Player) {
            Text denyReason = claim.allowEdit((Player) src);
            if (denyReason != null) {
                GriefPreventionPlugin.sendMessage(src, denyReason);
                return new GPFlagResult(FlagResultType.NO_PERMISSION);
            }
        }

        final String baseFlag = claimFlag.toString().toLowerCase();
        String flagPermission = GPPermissions.FLAG_BASE + "." + baseFlag;
        // special handling for commands
        if (baseFlag.equals(ClaimFlag.COMMAND_EXECUTE.name()) || baseFlag.equals(ClaimFlag.COMMAND_EXECUTE_PVP.name())) {
            target = handleCommandFlag(src, target);
            if (target == null) {
                // failed
                return new GPFlagResult(FlagResultType.TARGET_NOT_VALID);
            }
            flagPermission = GPPermissions.FLAG_BASE + "." + baseFlag + "." + target;
        } else {
            if (!target.equalsIgnoreCase("any")) {
                if (!target.contains(":")) {
                    // assume vanilla
                    target = "minecraft:" + target;
                }
    
                String[] parts = target.split(":");
                if (parts[1].equalsIgnoreCase("any")) {
                    target = baseFlag + "." + parts[0];
                } else {
                    // check for meta
                    parts = target.split("\\.");
                    String targetFlag = parts[0];
                    if (parts.length > 1) {
                        try {
                            Integer.parseInt(parts[1]);
                        } catch (NumberFormatException e) {
                            final Text message = GriefPreventionPlugin.instance.messageData.permissionClaimManage
                                    .apply(ImmutableMap.of(
                                    "meta", parts[1],
                                    "flag", baseFlag)).build();
                            GriefPreventionPlugin.sendMessage(src, message);
                            return new GPFlagResult(FlagResultType.TARGET_NOT_VALID);
                        }
                    }
                    String entitySpawnFlag = GPFlags.getEntitySpawnFlag(claimFlag, targetFlag);
                    if (entitySpawnFlag == null && !CommandHelper.validateFlagTarget(claimFlag, targetFlag)) {
                        //TODO
                        /*final Text message = GriefPreventionPlugin.instance.messageData.permissionClaimManage
                                .apply(ImmutableMap.of(
                                "target", targetFlag,
                                "flag", baseFlag)).build();*/
                        GriefPreventionPlugin.sendMessage(src,Text.of(TextColors.RED, "Invalid flag " + targetFlag));
                        return new GPFlagResult(FlagResultType.TARGET_NOT_VALID);
                    }
        
                    if (entitySpawnFlag != null) {
                        target = entitySpawnFlag;
                    } else {
                        target = baseFlag + "." + target.replace(":", ".");//.replace("[", ".[");
                    }
                }

                flagPermission = GPPermissions.FLAG_BASE + "." + target;
            } else {
                if (source != null) {
                    flagPermission+= ".minecraft";
                } else {
                    target = "";
                }
            }
        }

        return applyFlagPermission(src, subject, subjectName, claim, flagPermission, source, target, value, context, null, reason, false);
    }

    public static FlagResult applyFlagPermission(CommandSource src, Subject subject, String subjectName, GPClaim claim, String flagPermission, String source, String target, Tristate value, Context context, FlagType flagType) {
        return applyFlagPermission(src, subject, subjectName, claim, flagPermission, source, target, value, context, flagType, null, false);
    }

    public static FlagResult applyFlagPermission(CommandSource src, Subject subject, String subjectName, GPClaim claim, String flagPermission, String source, String target, Tristate value, Context context, FlagType flagType, Text reason, boolean clicked) {
        // Remove "any" in source
        if (source != null) {
            if (source.equalsIgnoreCase("any")) {
                source = "";
            } else {
                String[] parts = source.split(":");
                if (parts[1].equalsIgnoreCase("any")) {
                    source = parts[0];
                }
            }
        }

        final String basePermission = flagPermission.replace(GPPermissions.FLAG_BASE + ".", "");
        String baseFlagPermission = flagPermission.replace(GPPermissions.FLAG_BASE + ".", "");
        int endIndex = baseFlagPermission.indexOf(".");
        if (endIndex != -1) {
            baseFlagPermission = baseFlagPermission.substring(0, endIndex);
        }

        // Check if player can manage flag
        if (src instanceof Player) {
            Player player = (Player) src;
            GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
            Tristate result = Tristate.UNDEFINED;
            if (playerData.canManageAdminClaims) {
                result = Tristate.fromBoolean(src.hasPermission(GPPermissions.ADMIN_CLAIM_FLAGS + "." + basePermission));
            } else if (GriefPreventionPlugin.getActiveConfig(player.getWorld().getProperties()).getConfig().flags.getUserClaimFlags().contains(baseFlagPermission)) {
                result = Tristate.fromBoolean(src.hasPermission(GPPermissions.USER_CLAIM_FLAGS + "." + basePermission));
            }

            if (result != Tristate.TRUE) {
                GriefPreventionPlugin.sendMessage(src, GriefPreventionPlugin.instance.messageData.permissionFlagUse.toText());
                return new GPFlagResult(FlagResultType.NO_PERMISSION);
            }
        }

        Set<Context> contexts = new HashSet<>();
        if (context != claim.getContext()) {
                // validate perms
            if (context == ClaimContexts.ADMIN_DEFAULT_CONTEXT || 
                    context == ClaimContexts.BASIC_DEFAULT_CONTEXT || 
                    context == ClaimContexts.TOWN_DEFAULT_CONTEXT ||
                    context == ClaimContexts.WILDERNESS_DEFAULT_CONTEXT) {
                if (!src.hasPermission(GPPermissions.MANAGE_FLAG_DEFAULTS)) {
                    GriefPreventionPlugin.sendMessage(src, GriefPreventionPlugin.instance.messageData.permissionFlagDefaults.toText());
                    return new GPFlagResult(FlagResultType.NO_PERMISSION);
                }
                if (flagType == null) {
                    flagType = FlagType.DEFAULT;
                }
            } else if (context == ClaimContexts.ADMIN_OVERRIDE_CONTEXT || 
                    context == ClaimContexts.TOWN_OVERRIDE_CONTEXT ||
                    context == ClaimContexts.BASIC_OVERRIDE_CONTEXT ||
                    context == ClaimContexts.WILDERNESS_OVERRIDE_CONTEXT) {
                if (!src.hasPermission(GPPermissions.MANAGE_FLAG_OVERRIDES)) {
                    GriefPreventionPlugin.sendMessage(src, GriefPreventionPlugin.instance.messageData.permissionFlagOverrides.toText());
                    return new GPFlagResult(FlagResultType.NO_PERMISSION);
                }
                if (flagType == null) {
                    flagType = FlagType.OVERRIDE;
                }
            }
            contexts.add(context);
        } else {
            if (flagType == null) {
                flagType = FlagType.CLAIM;
            }
        }

        Text flagTypeText = Text.of();
        if (flagType == FlagType.OVERRIDE) {
            flagTypeText = Text.of(TextColors.RED, "OVERRIDE");
        } else if (flagType == FlagType.DEFAULT) {
            flagTypeText = Text.of(TextColors.LIGHT_PURPLE, "DEFAULT");
        } else if (flagType == FlagType.CLAIM) {
            flagTypeText = Text.of(TextColors.GOLD, "CLAIM");
        }

        if (source != null) {
            Pattern p = Pattern.compile("\\.[\\d+]*$");
            Matcher m = p.matcher(flagPermission);
            String targetMeta = "";
            if (m.find()) {
                targetMeta = m.group(0);
                flagPermission = flagPermission.replace(targetMeta, "");
            }
            flagPermission += ".source." + source + targetMeta;
            flagPermission = StringUtils.replace(flagPermission, ":", ".");
        }

        if (context == ClaimContexts.WILDERNESS_OVERRIDE_CONTEXT) {
            if (reason != null && !reason.isEmpty()) {
                GriefPreventionPlugin.getGlobalConfig().getConfig().bans.addBan(flagPermission, reason);
                GriefPreventionPlugin.getGlobalConfig().save();
            }
        }

        if (subject == GriefPreventionPlugin.GLOBAL_SUBJECT) {
            if (context == claim.getContext() || !ClaimContexts.CONTEXT_LIST.contains(context)) {
                contexts.add(claim.getContext());
            } else {
                // wilderness overrides affect all worlds
                if (context != ClaimContexts.WILDERNESS_OVERRIDE_CONTEXT) {
                    contexts.add(claim.world.getContext());
                }
            }

            GriefPreventionPlugin.GLOBAL_SUBJECT.getSubjectData().setPermission(contexts, flagPermission, value);
            if (!clicked) {
                src.sendMessage(Text.of(Text.builder()
                    .append(Text.of(TextColors.WHITE, "\n[", TextColors.AQUA, "Return to flags", TextColors.WHITE, "]\n"))
                    .onClick(TextActions.executeCallback(createCommandConsumer(src, "claimflag", ""))).build(),
                        TextColors.GREEN, "Set ", flagTypeText, " permission ", 
                        TextColors.AQUA, flagPermission.replace(GPPermissions.FLAG_BASE + ".", ""), 
                        TextColors.GREEN, "\n to ", 
                        TextColors.LIGHT_PURPLE, getClickableText(src, GriefPreventionPlugin.GLOBAL_SUBJECT, subjectName, contexts, flagPermission, value, flagType), 
                        TextColors.GREEN, " on ", 
                        TextColors.GOLD, "ALL"));
            }
        } else {
            if (context == claim.getContext() || !ClaimContexts.CONTEXT_LIST.contains(context)) {
                contexts.add(claim.getContext());
            } else {
                // wilderness overrides affect all worlds
                if (context != ClaimContexts.WILDERNESS_OVERRIDE_CONTEXT) {
                    contexts.add(claim.world.getContext());
                }
            }

            subject.getSubjectData().setPermission(contexts, flagPermission, value);
            if (!clicked) {
                src.sendMessage(Text.of(Text.builder()
                        .append(Text.of(TextColors.WHITE, "\n[", TextColors.AQUA, "Return to flags", TextColors.WHITE, "]\n"))
                        .onClick(TextActions.executeCallback(createCommandConsumer(src, subject instanceof User ? "claimflagplayer" : "claimflaggroup", subjectName))).build(),
                            TextColors.GREEN, "Set ", flagTypeText, " permission ", 
                            TextColors.AQUA, flagPermission.replace(GPPermissions.FLAG_BASE + ".", ""), 
                            TextColors.GREEN, "\n to ", 
                            TextColors.LIGHT_PURPLE, getClickableText(src, subject, subjectName, contexts, flagPermission, value, flagType), 
                            TextColors.GREEN, " on ", 
                            TextColors.GOLD, subjectName));
            }
        }

        return new GPFlagResult(FlagResultType.SUCCESS);
    }

    public static TextColor getFlagTypeColor(FlagType type) {
        TextColor color = TextColors.LIGHT_PURPLE;
        if (type == FlagType.CLAIM) {
            color = TextColors.GOLD;
        } else if (type == FlagType.OVERRIDE) {
            color = TextColors.RED;
        }

        return color;
    }

    public static Consumer<CommandSource> createFlagConsumer(CommandSource src, Subject subject, String subjectName, Set<Context> contexts, String flagPermission, Tristate flagValue, FlagType flagType) {
        return consumer -> {
            Tristate newValue = Tristate.UNDEFINED;
            if (flagValue == Tristate.TRUE) {
                newValue = Tristate.FALSE;
            } else if (flagValue == Tristate.UNDEFINED) {
                newValue = Tristate.TRUE;
            }

            Text flagTypeText = Text.of();
            if (flagType == FlagType.OVERRIDE) {
                flagTypeText = Text.of(TextColors.RED, "OVERRIDE");
            } else if (flagType == FlagType.DEFAULT) {
                flagTypeText = Text.of(TextColors.LIGHT_PURPLE, "DEFAULT");
            } else if (flagType == FlagType.CLAIM) {
                flagTypeText = Text.of(TextColors.GOLD, "CLAIM");
            }
            String target = flagPermission.replace(GPPermissions.FLAG_BASE + ".",  "");
            Set<Context> newContexts = new HashSet<>(contexts);
            subject.getSubjectData().setPermission(newContexts, flagPermission, newValue);
            src.sendMessage(Text.of(
                    TextColors.GREEN, "Set ", flagTypeText, " permission ", 
                    TextColors.AQUA, target, 
                    TextColors.GREEN, "\n to ", 
                    TextColors.LIGHT_PURPLE, getClickableText(src, subject, subjectName, newContexts, flagPermission, newValue, flagType), 
                    TextColors.GREEN, " for ", 
                    TextColors.GOLD, subjectName));
        };
    }

    public static Consumer<CommandSource> createCommandConsumer(CommandSource src, String command, String arguments) {
        return createCommandConsumer(src, command, arguments, null);
    }

    public static Consumer<CommandSource> createCommandConsumer(CommandSource src, String command, String arguments, Consumer<CommandSource> postConsumerTask) {
        return consumer -> {
            try {
                Sponge.getCommandManager().get(command).get().getCallable().process(src, arguments);
            } catch (CommandException e) {
                src.sendMessage(e.getText());
            }
            if (postConsumerTask != null) {
                postConsumerTask.accept(src);
            }
        };
    }

    public static void executeCommand(CommandSource src, String command, String arguments) {
        try {
            Sponge.getCommandManager().get(command).get().getCallable().process(src, arguments);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
        }
    }

    public static void showClaims(CommandSource src, Set<Claim> claims) {
        if (claims.isEmpty()) {
            // do nothing
            return;
        }
        showClaims(src, claims, 0, false);
    }

    public static void showOverlapClaims(CommandSource src, Set<Claim> claims, int height) {
        showClaims(src, claims, height, true, true);
    }

    public static void showClaims(CommandSource src, Set<Claim> claims, int height, boolean visualizeClaims) {
        showClaims(src, claims, height, visualizeClaims, false);
    }

    public static void showClaims(CommandSource src, Set<Claim> claims, int height, boolean visualizeClaims, boolean overlap) {
        final String worldName = src instanceof Player ? ((Player) src).getWorld().getName() : Sponge.getServer().getDefaultWorldName();
        final boolean canListOthers = src.hasPermission(GPPermissions.LIST_OTHER_CLAIMS);
        List<Text> claimsTextList = generateClaimTextList(new ArrayList<Text>(), claims, worldName, null, src, createShowClaimsConsumer(src, claims, height, visualizeClaims), canListOthers, false, overlap);
        if (visualizeClaims && src instanceof Player) {
            Player player = (Player) src;
            final GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
            if (claims.size() > 1) {
                if (height != 0) {
                    height = playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation.getBlockY() : player.getProperty(EyeLocationProperty.class).get().getValue().getFloorY();
                }
                Visualization visualization = Visualization.fromClaims(claims, playerData.optionClaimCreateMode == 1 ? height : player.getProperty(EyeLocationProperty.class).get().getValue().getFloorY(), player.getLocation(), playerData, null);
                visualization.apply(player);
            } else {
                for (Claim claim : claims) {
                    final GPClaim gpClaim = (GPClaim) claim;
                    gpClaim.getVisualizer().createClaimBlockVisuals(height, player.getLocation(), playerData);
                    gpClaim.getVisualizer().apply(player);
                }
            }
        }

        PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
        PaginationList.Builder paginationBuilder = paginationService.builder()
                .title(Text.of(TextColors.RED,"Claim list")).padding(Text.of(TextStyles.STRIKETHROUGH, "-")).contents(claimsTextList);
        paginationBuilder.sendTo(src);
    }

    private static Consumer<CommandSource> createShowClaimsConsumer(CommandSource src, Set<Claim> claims, int height, boolean visualizeClaims) {
        return consumer -> {
            showClaims(src, claims, height, visualizeClaims);
        };
    }

    public static List<Text> generateClaimTextList(List<Text> claimsTextList, Set<Claim> claimList, String worldName, User user, CommandSource src, Consumer<CommandSource> returnCommand, boolean canListOthers, boolean listChildren) {
        return generateClaimTextList(claimsTextList, claimList, worldName, user, src, returnCommand, canListOthers, listChildren, false);
    }

    public static List<Text> generateClaimTextList(List<Text> claimsTextList, Set<Claim> claimList, String worldName, User user, CommandSource src, Consumer<CommandSource> returnCommand, boolean canListOthers, boolean listChildren, boolean overlap) {
        final User sourceUser = src instanceof User ? (User) src : null;
        if (claimList.size() > 0) {
            for (Claim playerClaim : claimList) {
                GPClaim claim = (GPClaim) playerClaim;
                if (!overlap && !listChildren && claim.isSubdivision() && !claim.getData().getEconomyData().isForSale()) {
                    continue;
                }
                // Only list claims trusted if not an overlap claim
                if (!overlap && sourceUser != null && !claim.isUserTrusted(sourceUser, TrustType.ACCESSOR) && !canListOthers) {
                    continue;
                }

                double teleportHeight = claim.getOwnerPlayerData() == null ? 65.0D : (claim.getOwnerPlayerData().getMinClaimLevel() > 65.0D ? claim.getOwnerPlayerData().getMinClaimLevel() : 65);
                Location<World> a = claim.lesserBoundaryCorner;
                Vector3d center = a.getPosition().add(claim.greaterBoundaryCorner.getPosition()).div(2);
                if (teleportHeight == 65 && claim.getWorld().getDimension().getType() == DimensionTypes.OVERWORLD) {
                    teleportHeight = claim.getWorld().getHighestYAt((int)center.getX(), (int)center.getZ());
                }
                Location<World> southWest = claim.lesserBoundaryCorner.setPosition(new Vector3d(center.getX(), teleportHeight, center.getZ()));

                Text claimName = claim.getData().getName().orElse(Text.of());
                Text teleportName = claim.getData().getName().orElse(claim.getFriendlyNameType());
                Text ownerLine = Text.of(TextColors.YELLOW, "Owner", TextColors.WHITE, " : ", TextColors.GOLD, claim.getOwnerName(), "\n");
                Text claimTypeInfo = Text.of(TextColors.YELLOW, "Type", TextColors.WHITE, " : ", 
                        claim.getFriendlyNameType(), " ", TextColors.GRAY, claim.isCuboid() ? "3D " : "2D ",
                        TextColors.WHITE, " (Area: ", TextColors.GRAY, claim.getClaimBlocks(), " blocks",
                        TextColors.WHITE, ")\n");
                Text clickInfo = Text.of("Click to check more info.");
                Text basicInfo = Text.builder().append(
                        ownerLine,
                        claimTypeInfo,
                        clickInfo).build();

                Text claimInfoCommandClick = Text.builder().append(claim.getFriendlyNameType())
                .onClick(TextActions.executeCallback(CommandHelper.createCommandConsumer(src, "claiminfo", claim.id.toString(), createReturnClaimListConsumer(src, returnCommand))))
                .onHover(TextActions.showText(basicInfo))
                .build();

                Text claimCoordsTPClick = Text.builder().append(Text.of(
                        TextColors.WHITE, "[", TextColors.LIGHT_PURPLE, "TP", TextColors.WHITE, "]"))
                .onClick(TextActions.executeCallback(CommandHelper.createTeleportConsumer(src, southWest, claim)))
                .onHover(TextActions.showText(Text.of("Click here to teleport to ", teleportName, " ", southWest.getBlockPosition(), " in ", TextColors.LIGHT_PURPLE, claim.getWorld().getProperties().getWorldName(), TextColors.WHITE, ".")))
                .build();

                Text claimSpawn = null;
                if (claim.getData().getSpawnPos().isPresent()) {
                    Vector3i spawnPos = claim.getData().getSpawnPos().get();
                    Location<World> spawnLoc = new Location<>(claim.getWorld(), spawnPos);
                    claimSpawn = Text.builder().append(Text.of(TextColors.WHITE, "[", TextColors.LIGHT_PURPLE, "TP", TextColors.WHITE, "]"))
                            .onClick(TextActions.executeCallback(CommandHelper.createTeleportConsumer(src, spawnLoc, claim)))
                            .onHover(TextActions.showText(Text.of("Click here to teleport to ", teleportName, "'s spawn @ ", spawnPos, " in ", TextColors.LIGHT_PURPLE, claim.getWorld().getProperties().getWorldName(), TextColors.WHITE, ".")))
                            .build();
                } else {
                    claimSpawn = claimCoordsTPClick;
                }

                List<Text> childrenTextList = new ArrayList<>();
                if (!listChildren) {
                    childrenTextList = generateClaimTextList(new ArrayList<Text>(), claim.getInternalChildren(true), worldName, user, src, returnCommand, canListOthers, true);
                }
                final Player player = src instanceof Player ? (Player) src : null;
                Text buyClaim = Text.of();
                if (player != null && claim.getEconomyData().isForSale() && claim.getEconomyData().getSalePrice() > -1) {
                    Text buyInfo = Text.of(TextColors.AQUA, "Price ", TextColors.WHITE, ":", TextColors.GOLD, " ", claim.getEconomyData().getSalePrice(), "\nClick here to purchase claim.");
                    buyClaim = Text.builder()
                        .append(claim.getEconomyData().isForSale() ? Text.of(TextColors.WHITE, "[", TextColors.GREEN, "Buy", TextColors.WHITE, "]") : Text.of())
                        .onClick(TextActions.executeCallback(buyClaimConsumerConfirmation(src, claim)))
                        .onHover(TextActions.showText(Text.of(player.getUniqueId().equals(claim.getOwnerUniqueId()) ? "You already own this claim." : buyInfo))).build();
                }
                if (!childrenTextList.isEmpty()) {
                    Text children = Text.builder().append(Text.of(
                            TextColors.WHITE, "[", TextColors.AQUA, "children", TextColors.WHITE, "]"))
                            .onClick(TextActions.executeCallback(showChildrenList(childrenTextList, src, returnCommand, claim)))
                            .onHover(TextActions.showText(Text.of("Click here to view child claim list."))).build();
                    claimsTextList.add(Text.builder()
                            .append(Text.of(
                                    claimSpawn, " ",
                                    claimInfoCommandClick, TextColors.WHITE, " : ", 
                                    TextColors.GOLD, claim.getOwnerName(), " ",
                                    children, " ",
                                    claimName.isEmpty() ? "" : claimName, " ",
                                    buyClaim))
                            .build());
                } else {
                   claimsTextList.add(Text.builder()
                           .append(Text.of(
                                   claimSpawn, " ", 
                                   claimInfoCommandClick, TextColors.WHITE, " : ", 
                                   TextColors.GOLD, claim.getOwnerName(), " ",
                                   claimName.isEmpty() ? "" : claimName, " ",
                                   buyClaim))
                           .build());
                }
            }
            if (claimsTextList.size() == 0) {
                claimsTextList.add(Text.of(TextColors.RED, "No claims found in world."));
            }
        }
        return claimsTextList;
    }

    private static Consumer<CommandSource> buyClaimConsumerConfirmation(CommandSource src, Claim claim) {
        return confirm -> {
            final Player player = (Player) src;
            if (player.getUniqueId().equals(claim.getOwnerUniqueId())) {
                return;
            }
            Account playerAccount = GriefPreventionPlugin.instance.economyService.get().getOrCreateAccount(player.getUniqueId()).orElse(null);
            if (playerAccount == null) {
                Map<String, ?> params = ImmutableMap.of(
                        "user", player.getName());
                GriefPreventionPlugin.sendMessage(player, MessageStorage.ECONOMY_USER_NOT_FOUND, GriefPreventionPlugin.instance.messageData.economyUserNotFound, params);
                return;
            }

            final double balance = playerAccount.getBalance(GriefPreventionPlugin.instance.economyService.get().getDefaultCurrency()).doubleValue();
            if (balance < claim.getEconomyData().getSalePrice()) {
                Map<String, ?> params = ImmutableMap.of(
                        "sale_price", claim.getEconomyData().getSalePrice(),
                        "balance", balance,
                        "amount_needed", claim.getEconomyData().getSalePrice() -  balance);
                GriefPreventionPlugin.sendMessage(player, "economy-claim-buy-not-enough-funds", GriefPreventionPlugin.instance.messageData.economyClaimBuyNotEnoughFunds, params);
                return;
            }
            final Text message = GriefPreventionPlugin.instance.messageData.economyClaimBuyConfirmation
                    .apply(ImmutableMap.of(
                    "sale_price", claim.getEconomyData().getSalePrice())).build();
            GriefPreventionPlugin.sendMessage(src, message);
            final Text buyConfirmationText = Text.builder().append(Text.of(
                    TextColors.WHITE, "\n", TextColors.WHITE, "[", TextColors.GREEN, "Confirm", TextColors.WHITE, "]\n"))
                .onClick(TextActions.executeCallback(createBuyConsumerConfirmed(src, claim))).build();
            GriefPreventionPlugin.sendMessage(player, buyConfirmationText);
        };
    }

    private static Consumer<CommandSource> createBuyConsumerConfirmed(CommandSource src, Claim claim) {
        return confirm -> {
            final Player player = (Player) src;
            final Player ownerPlayer = Sponge.getServer().getPlayer(claim.getOwnerUniqueId()).orElse(null);
            final Account ownerAccount = GriefPreventionPlugin.instance.economyService.get().getOrCreateAccount(claim.getOwnerUniqueId()).orElse(null);
            if (ownerAccount == null) {
                src.sendMessage(Text.of(TextColors.RED, "Buy cancelled! Could not locate an economy account for owner."));
                return;
            }

            final ClaimResult result = claim.transferOwner(player.getUniqueId());
            if (!result.successful()) {
                final Text defaultMessage = Text.of(TextColors.RED, "Buy cancelled! Could not transfer owner. Result was ", result.getResultType().name());
                src.sendMessage(result.getMessage().orElse(defaultMessage));
                return;
            }

            try (CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                final Currency defaultCurrency = GriefPreventionPlugin.instance.economyService.get().getDefaultCurrency();
                final double salePrice = claim.getEconomyData().getSalePrice();
                Sponge.getCauseStackManager().pushCause(src);
                final TransactionResult ownerResult = ownerAccount.deposit(defaultCurrency, BigDecimal.valueOf(salePrice), Sponge.getCauseStackManager().getCurrentCause());
                Account playerAccount = GriefPreventionPlugin.instance.economyService.get().getOrCreateAccount(player.getUniqueId()).orElse(null);
                final TransactionResult
                    transactionResult =
                    playerAccount.withdraw(defaultCurrency, BigDecimal.valueOf(salePrice), Sponge.getCauseStackManager().getCurrentCause());
                final Text message = GriefPreventionPlugin.instance.messageData.economyClaimBuyConfirmed
                    .apply(ImmutableMap.of(
                        "sale_price", salePrice)).build();
                final Text saleMessage = GriefPreventionPlugin.instance.messageData.economyClaimSold
                    .apply(ImmutableMap.of(
                        "amount", salePrice,
                        "balance", ownerAccount.getBalance(defaultCurrency))).build();
                if (ownerPlayer != null) {
                    ownerPlayer.sendMessage(saleMessage);
                }
                claim.getEconomyData().setForSale(false);
                claim.getEconomyData().setSalePrice(0);
                claim.getData().save();
                GriefPreventionPlugin.sendMessage(src, message);
            }
        };
    }

    public static Consumer<CommandSource> showChildrenList(List<Text> childrenTextList, CommandSource src, Consumer<CommandSource> returnCommand, GPClaim parent) {
        return consumer -> {
            Text claimListReturnCommand = Text.builder().append(Text.of(
                    TextColors.WHITE, "\n[", TextColors.AQUA, "Return to claimslist", TextColors.WHITE, "]\n"))
                .onClick(TextActions.executeCallback(returnCommand)).build();
    
            List<Text> textList = new ArrayList<>();
            textList.add(claimListReturnCommand);
            textList.addAll(childrenTextList);
            PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
            PaginationList.Builder paginationBuilder = paginationService.builder()
                    .title(Text.of(parent.getName().orElse(parent.getFriendlyNameType()), " Child Claims")).padding(Text.of(TextStyles.STRIKETHROUGH, "-")).contents(textList);
            paginationBuilder.sendTo(src);
        };
    }

    public static Consumer<CommandSource> createReturnClaimListConsumer(CommandSource src, Consumer<CommandSource> returnCommand) {
        return consumer -> {
            Text claimListReturnCommand = Text.builder().append(Text.of(
                    TextColors.WHITE, "\n[", TextColors.AQUA, "Return to claimslist", TextColors.WHITE, "]\n"))
                .onClick(TextActions.executeCallback(returnCommand)).build();
            src.sendMessage(claimListReturnCommand);
        };
    }

    public static Consumer<CommandSource> createReturnClaimListConsumer(CommandSource src, String arguments) {
        return consumer -> {
            Text claimListReturnCommand = Text.builder().append(Text.of(
                    TextColors.WHITE, "\n[", TextColors.AQUA, "Return to claimslist", TextColors.WHITE, "]\n"))
                .onClick(TextActions.executeCallback(CommandHelper.createCommandConsumer(src, "claimslist", arguments))).build();
            src.sendMessage(claimListReturnCommand);
        };
    }

    public static Consumer<CommandSource> createFlagConsumer(CommandSource src, Subject subject, String subjectName, Set<Context> contexts, GPClaim claim, String flagPermission, Tristate flagValue, String source) {
        return consumer -> {
            String target = flagPermission.replace(GPPermissions.FLAG_BASE + ".", "");
            if (target.isEmpty()) {
                target = "any";
            }
            Tristate newValue = Tristate.UNDEFINED;
            if (flagValue == Tristate.TRUE) {
                newValue = Tristate.FALSE;
            } else if (flagValue == Tristate.UNDEFINED) {
                newValue = Tristate.TRUE;
            }

            CommandHelper.applyFlagPermission(src, subject, subjectName, claim, flagPermission, source, target, newValue, null, FlagType.GROUP);
        };
    }

    public static Text getClickableText(CommandSource src, Subject subject, String subjectName, Set<Context> contexts, String flagPermission, Tristate flagValue, FlagType type) {
        String onClickText = "Click here to toggle " + type.name().toLowerCase() + " value.";
        Text.Builder textBuilder = Text.builder()
                .append(Text.of(flagValue.toString().toLowerCase()))
                .onHover(TextActions.showText(Text.of(onClickText, "\n", getFlagTypeHoverText(type))))
                .onClick(TextActions.executeCallback(createFlagConsumer(src, subject, subjectName, contexts, flagPermission, flagValue, type)));
        return textBuilder.build();
    }

    public static Text getClickableText(CommandSource src, Subject subject, String subjectName, Set<Context> contexts, GPClaim claim, String flagPermission, Tristate flagValue, String source, FlagType type) {
        Text onClickText = Text.of("Click here to toggle flag value.");
        boolean hasPermission = true;
        if (type == FlagType.INHERIT) {
            onClickText = Text.of("This flag is inherited from parent claim ", claim.getName().orElse(claim.getFriendlyNameType()), " and ", TextStyles.UNDERLINE, "cannot", TextStyles.RESET, " be changed.");
            hasPermission = false;
        } else if (src instanceof Player) {
            Text denyReason = claim.allowEdit((Player) src);
            if (denyReason != null) {
                onClickText = denyReason;
                hasPermission = false;
            }
        }

        Text.Builder textBuilder = Text.builder()
        .append(Text.of(flagValue.toString().toLowerCase()))
        .onHover(TextActions.showText(Text.of(onClickText, "\n", getFlagTypeHoverText(type))));
        if (hasPermission) {
            textBuilder.onClick(TextActions.executeCallback(createFlagConsumer(src, subject, subjectName, contexts, claim, flagPermission, flagValue, source)));
        }
        return textBuilder.build();
    }

    public static String handleCommandFlag(CommandSource src, String target) {
        String pluginId = "minecraft";
        String args = "";
        String command = "";
        int argsIndex = target.indexOf("[");
        if (argsIndex != -1) {
            if (argsIndex == 0) {
                // invalid
                src.sendMessage(Text.of(
                        TextColors.RED, "No valid command entered."));
                return null;
            }
            command = target.substring(0, argsIndex);
            String[] parts = command.split(":");
            if (parts.length > 1) {
                pluginId = parts[0];
                command = parts[1];
            }
            if (!validateCommandMapping(src, command, pluginId)) {
                return null;
            }
            if (!pluginId.equals("minecraft")) {
                PluginContainer pluginContainer = Sponge.getPluginManager().getPlugin(pluginId).orElse(null);
                if (pluginContainer == null) {
                    src.sendMessage(Text.of(
                            TextColors.RED, "Could not locate a plugin with id '", 
                            TextColors.AQUA, pluginId, 
                            TextColors.RED, "'."));
                    return null;
                }
            }
            args = target.substring(argsIndex, target.length());
            Pattern p = Pattern.compile("\\[([^\\]]+)\\]");
            Matcher m = p.matcher(args);
            if (!m.find()) {
                // invalid
                src.sendMessage(Text.of(
                        TextColors.RED, "Invalid arguments '", 
                        TextColors.AQUA, args, 
                        TextColors.RED, "' entered. Check syntax matches  'command[arg1:arg2:etc]'"));
                return null;
            }
            args = m.group(1);
            target = pluginId + "." + command + "." + args.replace(":", ".");
        } else {
            String[] parts = target.split(":");
            if (parts.length > 1) {
                pluginId = parts[0];
                command = parts[1];
            } else {
                command = target;
            }
            target = pluginId + "." + command;
        }

        // validate command
        if (!validateCommandMapping(src, command, pluginId)) {
            return null;
        }

        return target;
    }

    private static boolean validateCommandMapping(CommandSource src, String command, String pluginId) {
        CommandMapping commandMapping = Sponge.getCommandManager().get(command).orElse(null);
        if (commandMapping == null) {
            src.sendMessage(Text.of(
                    TextColors.RED, "Could not locate the command '", 
                    TextColors.GREEN, command, 
                    TextColors.RED, "' for mod id '", 
                    TextColors.AQUA, pluginId, 
                    TextColors.RED, "'."));
            return false;
        }
        return true;
    }

    public static void handleUserTrustCommand(Player player, TrustType trustType, User user) {
        if (user == null) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.commandPlayerInvalid.toText());
            return;
        }
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimDisabledWorld.toText());
            return;
        }

        // determine which claim the player is standing in
        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAtPlayer(playerData, player.getLocation());
        if (user != null && user.getUniqueId().equals(player.getUniqueId()) && !playerData.canIgnoreClaim(claim)) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.trustSelf.toText());
            return;
        }

        ArrayList<Claim> targetClaims = new ArrayList<>();
        if (claim == null) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimNotFound.toText());
            return;
        } else if (user != null && claim.getOwnerUniqueId().equals(user.getUniqueId())) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimOwnerAlready.toText());
            return;
        } else {
            //check permission here
            if(claim.allowGrantPermission(player) != null) {
                final Text message = GriefPreventionPlugin.instance.messageData.permissionTrust
                        .apply(ImmutableMap.of(
                        "owner", claim.getOwnerName())).build();
                GriefPreventionPlugin.sendMessage(player, message);
                return;
            }

            if(trustType == TrustType.MANAGER) {
                Text denyReason = claim.allowEdit(player);
                if(denyReason != null) {
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.permissionGrant.toText());
                    return;
                }
            }

            targetClaims.add(claim);
        }

        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            Sponge.getCauseStackManager().pushCause(player);
            GPUserTrustClaimEvent.Add
                event =
                new GPUserTrustClaimEvent.Add(targetClaims, ImmutableList.of(user.getUniqueId()), trustType);
            Sponge.getEventManager().post(event);
            if (event.isCancelled()) {
                player.sendMessage(Text.of(TextColors.RED,
                    event.getMessage().orElse(Text.of("Could not trust user '" + user.getName() + "'. A plugin has denied it."))));
                return;
            }

            for (Claim currentClaim : targetClaims) {
                GPClaim gpClaim = (GPClaim) currentClaim;
                final List<UUID> trustList = gpClaim.getUserTrustList(trustType);
                if (trustList.contains(user.getUniqueId())) {
                    final Text message = GriefPreventionPlugin.instance.messageData.trustAlreadyHas
                        .apply(ImmutableMap.of(
                            "target", user.getName(),
                            "type", trustType.name())).build();
                    GriefPreventionPlugin.sendMessage(player, message);
                    return;
                }

                trustList.add(user.getUniqueId());
                gpClaim.getInternalClaimData().setRequiresSave(true);
                gpClaim.getInternalClaimData().save();
            }

            final Text message = GriefPreventionPlugin.instance.messageData.trustGrant
                .apply(ImmutableMap.of(
                    "target", user.getName(),
                    "type", trustType.name())).build();
            GriefPreventionPlugin.sendMessage(player, message);
        }
    }

    public static void handleGroupTrustCommand(Player player, TrustType trustType, String group) {
        final Text invalidGroup = GriefPreventionPlugin.instance.messageData.commandGroupInvalid
                .apply(ImmutableMap.of(
                "group", group)).build();
        if (group == null) {
            GriefPreventionPlugin.sendMessage(player, invalidGroup);
            return;
        }
        if (!PermissionUtils.hasGroupSubject(group)) {
            GriefPreventionPlugin.sendMessage(player, invalidGroup);
            return;
        }
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(player.getWorld().getProperties())) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimDisabledWorld.toText());
            return;
        }

        Subject subject = PermissionUtils.getGroupSubject(group);
        // determine which claim the player is standing in
        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAtPlayer(playerData, player.getLocation());
        ArrayList<Claim> targetClaims = new ArrayList<>();

        if (claim == null) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimNotFound.toText());
            return;
        } else {
            //check permission here
            if(claim.allowGrantPermission(player) != null) {
                final Text message = GriefPreventionPlugin.instance.messageData.permissionTrust
                        .apply(ImmutableMap.of(
                        "owner", claim.getOwnerName())).build();
                GriefPreventionPlugin.sendMessage(player, message);
                return;
            }

            if(trustType == TrustType.MANAGER) {
                Text denyReason = claim.allowEdit(player);
                if(denyReason != null) {
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.permissionGrant.toText());
                    return;
                }
            }

            targetClaims.add(claim);
        }

        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            Sponge.getCauseStackManager().pushCause(player);
            GPGroupTrustClaimEvent.Add event =
                new GPGroupTrustClaimEvent.Add(targetClaims, ImmutableList.of(group), trustType);
            Sponge.getEventManager().post(event);
            if (event.isCancelled()) {
                player.sendMessage(
                    Text.of(TextColors.RED, event.getMessage().orElse(Text.of("Could not trust group '" + group + "'. A plugin has denied it."))));
                return;
            }
        }

        final String permission = getTrustPermission(trustType);
        for (Claim currentClaim : targetClaims) {
            GPClaim gpClaim = (GPClaim) currentClaim;
            Set<Context> contexts = new HashSet<>(); 
            contexts.add(gpClaim.getContext());
            if (!gpClaim.getGroupTrustList(trustType).contains(group)) {
                gpClaim.getGroupTrustList(trustType).add(group);
            }
            subject.getSubjectData().setPermission(contexts, permission, Tristate.TRUE);
            gpClaim.getInternalClaimData().setRequiresSave(true);
        }

        final Text message = GriefPreventionPlugin.instance.messageData.trustGrant
                .apply(ImmutableMap.of(
                "target", group,
                "type", trustType.name())).build();
        GriefPreventionPlugin.sendMessage(player, message);
    }

    private static String getTrustPermission(TrustType trustType) {
        if (trustType == TrustType.ACCESSOR) {
            return GPPermissions.TRUST_ACCESSOR;
        } else if (trustType == TrustType.CONTAINER) {
            return GPPermissions.TRUST_CONTAINER;
        } else if (trustType == TrustType.BUILDER) {
            return GPPermissions.TRUST_BUILDER;
        } else {
            return GPPermissions.TRUST_MANAGER;
        }
    }

    public static Consumer<CommandSource> createTeleportConsumer(CommandSource src, Location<World> location, Claim claim) {
        return teleport -> {
            if (!(src instanceof Player)) {
                // ignore
                return;
            }
            Player player = (Player) src;
            GPClaim gpClaim = (GPClaim) claim;
            GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
            if (!playerData.canIgnoreClaim(gpClaim) && !playerData.canManageAdminClaims) {
                // if not owner of claim, validate perms
                if (!player.getUniqueId().equals(claim.getOwnerUniqueId())) {
                    if (!player.hasPermission(GPPermissions.COMMAND_CLAIM_INFO_TELEPORT_OTHERS)) {
                        player.sendMessage(Text.of(TextColors.RED, "You do not have permission to use the teleport feature in this claim.")); 
                        return;
                    }
                    if (!gpClaim.isUserTrusted(player, TrustType.ACCESSOR)) {
                        if (GriefPreventionPlugin.instance.economyService.isPresent()) {
                            // Allow non-trusted to TP to claims for sale
                            if (!gpClaim.getEconomyData().isForSale()) {
                                player.sendMessage(Text.of(TextColors.RED, "You are not trusted to use the teleport feature in this claim.")); 
                                return;
                            }
                        } else {
                            player.sendMessage(Text.of(TextColors.RED, "You are not trusted to use the teleport feature in this claim.")); 
                            return;
                        }
                    }
                } else if (!player.hasPermission(GPPermissions.COMMAND_CLAIM_INFO_TELEPORT_BASE)) {
                    player.sendMessage(Text.of(TextColors.RED, "You do not have permission to use the teleport feature in your claim.")); 
                    return;
                }
            }

            Location<World> safeLocation = Sponge.getGame().getTeleportHelper().getSafeLocation(location, 64, 16).orElse(null);
            if (safeLocation == null) {
                player.sendMessage(
                        Text.builder().append(Text.of(TextColors.RED, "Location is not safe. "), 
                        Text.builder().append(Text.of(TextColors.GREEN, "Are you sure you want to teleport here?")).onClick(TextActions.executeCallback(createForceTeleportConsumer(player, location))).style(TextStyles.UNDERLINE).build()).build());
            } else {
                player.setLocation(safeLocation);
            }
        };
    }

    public static Consumer<CommandSource> createForceTeleportConsumer(Player player, Location<World> location) {
        return teleport -> {
            player.setLocation(location);
        };
    }

    public static void handleBankTransaction(CommandSource src, CommandContext args, GPClaim claim) {
        final EconomyService economyService = GriefPreventionPlugin.instance.economyService.orElse(null);
        if (economyService == null) {
            GriefPreventionPlugin.sendMessage(src, GriefPreventionPlugin.instance.messageData.economyNotInstalled.toText());
            return;
        }

        if (claim.isSubdivision() || claim.isAdminClaim()) {
            return;
        }

        Account bankAccount = claim.getEconomyAccount().orElse(null);
        if (bankAccount == null) {
            GriefPreventionPlugin.sendMessage(src, GriefPreventionPlugin.instance.messageData.economyVirtualNotSupported.toText());
            return;
        }

        final String command = args.<String>getOne("command").orElse(null);
        final double amount = args.<Double>getOne("amount").get();

        final UUID playerSource = ((Player) src).getUniqueId();
        final GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getPlayerData(claim.getWorld(), claim.getOwnerUniqueId());
        if (playerData.canIgnoreClaim(claim) || claim.getOwnerUniqueId().equals(playerSource) || claim.getUserTrusts(TrustType.MANAGER).contains(playerData.playerID)) {
            final UniqueAccount playerAccount = economyService.getOrCreateAccount(playerData.playerID).get();
            try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                Sponge.getCauseStackManager().pushCause(src);
                Sponge.getCauseStackManager().addContext(GriefPreventionPlugin.PLUGIN_CONTEXT, GriefPreventionPlugin.instance);
                if (command.equalsIgnoreCase("withdraw")) {
                    TransactionResult
                        result =
                        bankAccount.withdraw(economyService.getDefaultCurrency(), BigDecimal.valueOf(amount), Sponge.getCauseStackManager().getCurrentCause());
                    if (result.getResult() == ResultType.SUCCESS) {
                        final Text message = GriefPreventionPlugin.instance.messageData.claimBankWithdraw
                            .apply(ImmutableMap.of(
                                "amount", amount)).build();
                        GriefPreventionPlugin.sendMessage(src, message);
                        playerAccount.deposit(economyService.getDefaultCurrency(), BigDecimal.valueOf(amount), Sponge.getCauseStackManager().getCurrentCause());
                        claim.getData().getEconomyData().addBankTransaction(
                            new GPBankTransaction(BankTransactionType.WITHDRAW_SUCCESS, playerData.playerID, Instant.now(), amount));
                    } else {
                        final Text message = GriefPreventionPlugin.instance.messageData.claimBankWithdrawNoFunds
                            .apply(ImmutableMap.of(
                                "balance", bankAccount.getBalance(economyService.getDefaultCurrency()),
                                "amount", amount)).build();
                        GriefPreventionPlugin.sendMessage(src, message);
                        claim.getData().getEconomyData()
                            .addBankTransaction(new GPBankTransaction(BankTransactionType.WITHDRAW_FAIL, playerData.playerID, Instant.now(), amount));
                        return;
                    }
                } else if (command.equalsIgnoreCase("deposit")) {
                    TransactionResult
                        result =
                        playerAccount.withdraw(economyService.getDefaultCurrency(), BigDecimal.valueOf(amount), Sponge.getCauseStackManager().getCurrentCause());
                    if (result.getResult() == ResultType.SUCCESS) {
                        double depositAmount = amount;
                        if (claim.getData().isExpired()) {
                            final double taxBalance = claim.getEconomyData().getTaxBalance();
                            depositAmount -= claim.getEconomyData().getTaxBalance();
                            if (depositAmount >= 0) {
                                claim.getEconomyData().addBankTransaction(new GPBankTransaction(BankTransactionType.TAX_SUCCESS, Instant.now(), taxBalance));
                                claim.getEconomyData().setTaxPastDueDate(null);
                                claim.getEconomyData().setTaxBalance(0);
                                claim.getInternalClaimData().setExpired(false);
                                final Text message = GriefPreventionPlugin.instance.messageData.taxClaimPaidBalance
                                        .apply(ImmutableMap.of(
                                            "amount", taxBalance)).build();
                                GriefPreventionPlugin.sendMessage(src, message);
                                if (depositAmount == 0) {
                                    return;
                                }
                            } else {
                                final double newTaxBalance = Math.abs(depositAmount);
                                claim.getEconomyData().setTaxBalance(newTaxBalance);
                                final Text message = GriefPreventionPlugin.instance.messageData.taxClaimPaidPartial
                                        .apply(ImmutableMap.of(
                                            "amount", depositAmount,
                                            "balance", newTaxBalance)).build();
                                GriefPreventionPlugin.sendMessage(src, message);
                                return;
                            }
                        }
                        final Text message = GriefPreventionPlugin.instance.messageData.claimBankDeposit
                            .apply(ImmutableMap.of(
                                "amount", depositAmount)).build();
                        GriefPreventionPlugin.sendMessage(src, message);
                        bankAccount.deposit(economyService.getDefaultCurrency(), BigDecimal.valueOf(depositAmount), Sponge.getCauseStackManager().getCurrentCause());
                        claim.getData().getEconomyData().addBankTransaction(
                            new GPBankTransaction(BankTransactionType.DEPOSIT_SUCCESS, playerData.playerID, Instant.now(), depositAmount));
                    } else {
                        GriefPreventionPlugin.sendMessage(src, GriefPreventionPlugin.instance.messageData.claimBankDepositNoFunds.toText());
                        claim.getData().getEconomyData()
                            .addBankTransaction(new GPBankTransaction(BankTransactionType.DEPOSIT_FAIL, playerData.playerID, Instant.now(), amount));
                        return;
                    }
                }
            }
        } else {
            final Text message = GriefPreventionPlugin.instance.messageData.claimBankNoPermission
                    .apply(ImmutableMap.of(
                            "owner", claim.getOwnerName())).build();
            GriefPreventionPlugin.sendMessage(src, message);
        }
    }

    public static void displayClaimBankInfo(CommandSource src, GPClaim claim) {
        displayClaimBankInfo(src, claim, false, false);
    }

    public static void displayClaimBankInfo(CommandSource src, GPClaim claim, boolean checkTown, boolean returnToClaimInfo) {
        final EconomyService economyService = GriefPreventionPlugin.instance.economyService.orElse(null);
        if (economyService == null) {
            GriefPreventionPlugin.sendMessage(src, GriefPreventionPlugin.instance.messageData.economyNotInstalled.toText());
            return;
        }

        if (checkTown && !claim.isInTown()) {
            GriefPreventionPlugin.sendMessage(src, GriefPreventionPlugin.instance.messageData.townNotIn.toText());
            return;
        }

        if (!checkTown && (claim.isSubdivision() || claim.isAdminClaim())) {
            return;
        }

        final GPClaim town = claim.getTownClaim();
        Account bankAccount = checkTown ? town.getEconomyAccount().orElse(null) : claim.getEconomyAccount().orElse(null);
        if (bankAccount == null) {
            GriefPreventionPlugin.sendMessage(src, GriefPreventionPlugin.instance.messageData.economyVirtualNotSupported.toText());
            return;
        }

        final GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getPlayerData(claim.getWorld(), claim.getOwnerUniqueId());
        final double claimBalance = bankAccount.getBalance(economyService.getDefaultCurrency()).doubleValue();
        double taxOwed = -1;
        final double playerTaxRate = GPOptionHandler.getClaimOptionDouble(playerData.getPlayerSubject(), claim, GPOptions.Type.TAX_RATE, playerData);
        if (checkTown) {
            if (!town.getOwnerUniqueId().equals(playerData.playerID)) {
                for (Claim playerClaim : playerData.getInternalClaims()) {
                    GPClaim playerTown = (GPClaim) playerClaim.getTown().orElse(null);
                    if (!playerClaim.isTown() && playerTown != null && playerTown.getUniqueId().equals(claim.getUniqueId())) {
                        taxOwed += playerTown.getClaimBlocks() * playerTaxRate;
                    }
                }
            } else {
                taxOwed = town.getClaimBlocks() * playerTaxRate;
            }
        } else {
            taxOwed = claim.getClaimBlocks() * playerTaxRate;
        }

        final GriefPreventionConfig<?> activeConfig = GriefPreventionPlugin.getActiveConfig(claim.getWorld().getProperties());
        final ZonedDateTime withdrawDate = TaskUtils.getNextTargetZoneDate(activeConfig.getConfig().claim.taxApplyHour, 0, 0);
        Duration duration = Duration.between(Instant.now().truncatedTo(ChronoUnit.SECONDS), withdrawDate.toInstant()) ;
        final long s = duration.getSeconds();
        final String timeLeft = String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
        final Text message = GriefPreventionPlugin.instance.messageData.claimBankInfo
                .apply(ImmutableMap.of(
                "balance", claimBalance,
                "amount", taxOwed,
                "time_remaining", timeLeft,
                "tax_balance", claim.getData().getEconomyData().getTaxBalance())).build();
        Text transactions = Text.builder()
                .append(Text.of(TextStyles.ITALIC, TextColors.AQUA, "Bank Transactions"))
                .onClick(TextActions.executeCallback(createBankTransactionsConsumer(src, claim, checkTown, returnToClaimInfo)))
                .onHover(TextActions.showText(Text.of("Click here to view bank transactions")))
                .build();
        List<Text> textList = new ArrayList<>();
        if (returnToClaimInfo) {
            textList.add(Text.builder().append(Text.of(
                    TextColors.WHITE, "\n[", TextColors.AQUA, "Return to claim info", TextColors.WHITE, "]\n"))
                .onClick(TextActions.executeCallback(CommandHelper.createCommandConsumer(src, "claiminfo", ""))).build());
        }
        textList.add(message);
        textList.add(transactions);
        PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
        PaginationList.Builder paginationBuilder = paginationService.builder()
                .title(Text.of(TextColors.AQUA, "Bank Info")).padding(Text.of(TextStyles.STRIKETHROUGH, "-")).contents(textList);
        paginationBuilder.sendTo(src);
    }

    public static Consumer<CommandSource> createBankTransactionsConsumer(CommandSource src, GPClaim claim, boolean checkTown, boolean returnToClaimInfo) {
        return settings -> {
            final String name = "Bank Transactions";
            List<String> bankTransactions = new ArrayList<>(claim.getData().getEconomyData().getBankTransactionLog());
            Collections.reverse(bankTransactions);
            List<Text> textList = new ArrayList<>();
            textList.add(Text.builder().append(Text.of(
                    TextColors.WHITE, "\n[", TextColors.AQUA, "Return to bank info", TextColors.WHITE, "]\n"))
                .onClick(TextActions.executeCallback(consumer -> { displayClaimBankInfo(src, claim, checkTown, returnToClaimInfo); })).build());
            Gson gson = new Gson();
            for (String transaction : bankTransactions) {
                GPBankTransaction bankTransaction = gson.fromJson(transaction, GPBankTransaction.class);
                final Duration duration = Duration.between(bankTransaction.timestamp, Instant.now().truncatedTo(ChronoUnit.SECONDS)) ;
                final long s = duration.getSeconds();
                final User user = GriefPreventionPlugin.getOrCreateUser(bankTransaction.source);
                final String timeLeft = String.format("%dh %02dm %02ds", s / 3600, (s % 3600) / 60, (s % 60)) + " ago";
                textList.add(Text.of(getTransactionColor(bankTransaction.type), bankTransaction.type.name(), 
                        TextColors.BLUE, " | ", TextColors.WHITE, bankTransaction.amount, 
                        TextColors.BLUE, " | ", TextColors.GRAY, timeLeft,
                        user == null ? "" : Text.of(TextColors.BLUE, " | ", TextColors.LIGHT_PURPLE, user.getName())));
            }
            textList.add(Text.builder().append(Text.of(
                    TextColors.WHITE, "\n[", TextColors.AQUA, "Return to bank info", TextColors.WHITE, "]\n"))
                .onClick(TextActions.executeCallback(CommandHelper.createCommandConsumer(src, "claimbank", ""))).build());
            PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
            PaginationList.Builder paginationBuilder = paginationService.builder()
                    .title(Text.of(TextColors.AQUA, name)).padding(Text.of(TextStyles.STRIKETHROUGH, "-")).contents(textList);
            paginationBuilder.sendTo(src);
        };
    }

    public static TextColor getTransactionColor(BankTransactionType type) {
        switch (type) {
            case DEPOSIT_SUCCESS :
            case TAX_SUCCESS :
            case WITHDRAW_SUCCESS :
                return TextColors.GREEN;
            case DEPOSIT_FAIL :
            case TAX_FAIL :
            case WITHDRAW_FAIL :
                return TextColors.RED;
            default :
                return TextColors.GREEN;
        }
    }

    private static Comparator<Object[]> rawTextComparator() {
        return (t1, t2) -> Text.of(t1).compareTo(Text.of(t2));
    }

    public static List<Text> stripeText(List<Object[]> texts) {
        Collections.sort(texts, rawTextComparator());

        ImmutableList.Builder<Text> finalTexts = ImmutableList.builder();
        for (int i = 0; i < texts.size(); i++) {
            Object[] text = texts.get(i);
            text[0] = i % 2 == 0 ? TextColors.GREEN : TextColors.AQUA; // Set starting color
            finalTexts.add(Text.of(text));
        }
        return finalTexts.build();
    }

    public static Text getFlagTypeHoverText(FlagType type) {
        Text hoverText = Text.of("");
        if (type == FlagType.DEFAULT) {
            hoverText = Text.of(TextColors.LIGHT_PURPLE, "DEFAULT ", TextColors.WHITE, " : Default is last to be checked. Both claim and override take priority over this.");
        } else if (type == FlagType.CLAIM) {
            hoverText = Text.of(TextColors.GOLD, "CLAIM", TextColors.WHITE, " : Claim is checked before default values. Allows claim owners to specify flag settings in claim only.");
        } else if (type == FlagType.OVERRIDE) {
            hoverText = Text.of(TextColors.RED, "OVERRIDE", TextColors.WHITE, " : Override has highest priority and is checked above both default and claim values. Allows admins to override all basic and admin claims.");
        } else if (type == FlagType.INHERIT) {
            hoverText = Text.of(TextColors.AQUA, "INHERIT", TextColors.WHITE, " : Inherit is an enforced flag set by a parent claim that cannot changed.");
        }
        return hoverText;
    }

    public static Text getBaseFlagOverlayText(String flagPermission) {
        String baseFlag = flagPermission.replace(GPPermissions.FLAG_BASE + ".", "");
        int endIndex = baseFlag.indexOf(".");
        if (endIndex != -1) {
            baseFlag = baseFlag.substring(0, endIndex);
        }
        if (!ClaimFlag.contains(baseFlag)) {
            return Text.of("Not defined.");
        }

        final ClaimFlag claimFlag = ClaimFlag.getEnum(baseFlag);

        switch(claimFlag) {
            case BLOCK_BREAK :
                return Text.of("Controls whether a block can be broken.\n",
                        TextColors.LIGHT_PURPLE, "Example 1", TextColors.WHITE, " : To prevent any source from breaking dirt blocks, enter\n",
                        TextColors.GREEN, "/cf block-break minecraft:dirt false\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : ", "minecraft represents the modid and dirt represents the block id.\n",
                            "Specifying no modid will always default to minecraft.\n",
                        TextColors.LIGHT_PURPLE, "Example 2", TextColors.WHITE, " : To prevent players from breaking dirt blocks, enter\n",
                        TextColors.GREEN, "/cf block-break minecraft:player minecraft:dirt false\n");
            case BLOCK_PLACE :
                return Text.of("Controls whether a block can be placed.\n",
                        TextColors.LIGHT_PURPLE, "Example 1", TextColors.WHITE, " : To prevent any source from placing dirt blocks, enter\n",
                        TextColors.GREEN, "/cf block-place minecraft:dirt false\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : ", "minecraft represents the modid and dirt represents the block id.\n",
                            "Specifying no modid will always default to minecraft.\n",
                        TextColors.LIGHT_PURPLE, "Example 2", TextColors.WHITE, " : To prevent players from placing dirt blocks, enter\n",
                        TextColors.GREEN, "/cf block-place minecraft:player minecraft:dirt false\n");
            case COMMAND_EXECUTE :
                return Text.of("Controls whether a command can be executed.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent pixelmon's command '/shop select' from being run, enter\n",
                        TextColors.GREEN, "/cf command-execute pixelmon:shop[select] false\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : ", 
                        TextStyles.ITALIC, TextColors.GOLD, "pixelmon", TextStyles.RESET, TextColors.RESET, " represents the modid, ", 
                        TextStyles.ITALIC, TextColors.GOLD, "shop", TextStyles.RESET, TextColors.RESET, " represents the base command, and ",
                        TextStyles.ITALIC, TextColors.GOLD, "select", TextStyles.RESET, TextColors.RESET,  " represents the argument.\n",
                            "Specifying no modid will always default to minecraft.\n");
            case COMMAND_EXECUTE_PVP :
                return Text.of("Controls whether a command can be executed while engaged in ", TextColors.RED, "PvP.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent pixelmon's command '/shop select' from being run, enter\n",
                        TextColors.GREEN, "/cf command-execute pixelmon:shop[select] false\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : ", 
                        TextStyles.ITALIC, TextColors.GOLD, "pixelmon", TextStyles.RESET, TextColors.RESET, " represents the modid, ", 
                        TextStyles.ITALIC, TextColors.GOLD, "shop", TextStyles.RESET, TextColors.RESET, " represents the base command, and ",
                        TextStyles.ITALIC, TextColors.GOLD, "select", TextStyles.RESET, TextColors.RESET,  " represents the argument.\n",
                            "Specifying no modid will always default to minecraft.\n");
            case ENTER_CLAIM :
                return Text.of("Controls whether an entity can enter claim.\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : If you want to use this for players, it is recommended to use \n", 
                        "the '/cfg' command with the group the player is in.");
            case ENTITY_CHUNK_SPAWN :
                return Text.of("Controls whether an entity can be spawned during chunk load.\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : This will remove all saved entities within a chunk after it loads.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent horses from spawning in chunks enter\n",
                        TextColors.GREEN, "/cf entity-chunk-spawn minecraft:horse false");
            case ENTITY_COLLIDE_BLOCK :
                return Text.of("Controls whether an entity can collide with a block.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent entity collisions with dirt blocks, enter\n",
                        TextColors.GREEN, "/cf entity-collide-block minecraft:dirt false");
            case ENTITY_COLLIDE_ENTITY :
                return Text.of("Controls whether an entity can collide with an entity.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent entity collisions with item frames, enter\n",
                        TextColors.GREEN, "/cf entity-collide-entity minecraft:itemframe false");
            case ENTITY_DAMAGE :
                return Text.of("Controls whether an entity can be damaged.\n",
                        TextColors.LIGHT_PURPLE, "Example 1", TextColors.WHITE, " : To prevent horses from being damaged, enter\n",
                        TextColors.GREEN, "/cf entity-damage minecraft:horse false\n",
                        TextColors.LIGHT_PURPLE, "Example 2", TextColors.WHITE, " : To prevent all animals from being damaged, enter\n",
                        TextColors.GREEN, "/cf entity-damage minecraft:animals false");
            case ENTITY_RIDING :
                return Text.of("Controls whether an entity can be mounted.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent horses from being mounted enter\n",
                        TextColors.GREEN, "/cf entity-riding minecraft:horse false");
            case ENTITY_SPAWN :
                return Text.of("Controls whether an entity can be spawned into the world.\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : This does not include entity items. See item-spawn flag.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent horses from spawning enter\n",
                        TextColors.GREEN, "/cf entity-spawn minecraft:horse false");
            case ENTITY_TELEPORT_FROM :
                return Text.of("Controls whether an entity can teleport from their current location.\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : If you want to use this for players, it is recommended to use \n", 
                        "the '/cfg' command with the group the player is in.");
            case ENTITY_TELEPORT_TO :
                return Text.of("Controls whether an entity can teleport to a location.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent creepers from traveling and/or teleporting within your claim, enter\n",
                        TextColors.GREEN, "/cf entity-teleport-to minecraft:creeper false\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : If you want to use this for players, it is recommended to use \n", 
                        "the '/cfg' command with the group the player is in.");
            case EXIT_CLAIM :
                return Text.of("Controls whether an entity can exit claim.\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : If you want to use this for players, it is recommended to use \n", 
                        "the '/cfg' command with the group the player is in.");
            case EXPLOSION :
                return Text.of("Controls whether an explosion can occur in the world.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent any explosion, enter\n",
                        TextColors.GREEN, "/cf explosion any false");
            case EXPLOSION_SURFACE :
                return Text.of("Controls whether an explosion can occur above the surface in a world.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent an explosion above surface, enter\n",
                        TextColors.GREEN, "/cf explosion-surface any false");
            case FIRE_SPREAD :
                return Text.of("Controls whether fire can spread in a world.\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : This does not prevent the initial fire being placed, only spread.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent fire from spreading, enter\n",
                        TextColors.GREEN, "/cf fire-spread any false");
            case INTERACT_BLOCK_PRIMARY :
                return Text.of("Controls whether a player can left-click(attack) a block.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent players from left-clicking chests, enter\n",
                        TextColors.GREEN, "/cf interact-block-primary minecraft:chest false");
            case INTERACT_BLOCK_SECONDARY :
                return Text.of("Controls whether a player can right-click a block.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent players from right-clicking(opening) chests, enter\n",
                        TextColors.GREEN, "/cf interact-block-secondary minecraft:chest false");
            case INTERACT_ENTITY_PRIMARY :
                return Text.of("Controls whether a player can left-click(attack) an entity.\n",
                    TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent players from left-clicking horses, enter\n",
                    TextColors.GREEN, "/cf interact-entity-primary minecraft:player minecraft:horse false\n");
            case INTERACT_ENTITY_SECONDARY :
                return Text.of("Controls whether a player can right-click on an entity.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent horses from being mounted, enter\n",
                        TextColors.GREEN, "/cf interact-entity-secondary minecraft:horse false\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : ", "minecraft represents the modid and horse represents the entity id.\n",
                            "Specifying no modid will always default to minecraft.\n");
            case INTERACT_INVENTORY :
                return Text.of("Controls whether a player can right-click with a block that contains inventory such as a chest.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent players from right-clicking any block that contains inventory, enter\n",
                        TextColors.GREEN, "/cf interact-inventory any false");
            case INTERACT_INVENTORY_CLICK :
                return Text.of("Controls whether a player can click on an inventory slot.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent players from clicking an inventory slot that contains diamond, enter\n",
                        TextColors.GREEN, "/cf interact-inventory-click minecraft:diamond false");
            case INTERACT_ITEM_PRIMARY :
                return Text.of("Controls whether a player can left-click(attack) with an item.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent players from left-clicking while holding a diamond sword, enter\n",
                        TextColors.GREEN, "/cf interact-item-primary minecraft:diamond_sword false");
            case INTERACT_ITEM_SECONDARY :
                return Text.of("Controls whether a player can right-click with an item.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent players from right-clicking while holding a flint and steel, enter\n",
                        TextColors.GREEN, "/cf interact-item-secondary minecraft:flint_and_steel false");
            case ITEM_DROP :
                return Text.of("Controls whether an item can be dropped.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent tnt from dropping in the world, enter\n",
                        TextColors.GREEN, "/cf item-drop minecraft:tnt false");
            case ITEM_PICKUP :
                return Text.of("Controls whether an item can be picked up.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent tnt from dropping in the world, enter\n",
                        TextColors.GREEN, "/cf item-drop minecraft:tnt false");
            case ITEM_SPAWN :
                return Text.of("Controls whether an item can be spawned into the world up.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent feather's from dropping in the world, enter\n",
                        TextColors.GREEN, "/cf item-drop minecraft:feather false");
            case ITEM_USE :
                return Text.of("Controls whether an item can be used.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent usage of diamond swords, enter\n",
                        TextColors.GREEN, "/cf item-use minecraft:diamond_sword false");
            case LEAF_DECAY :
                return Text.of("Controls whether leaves can decay in a world.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent leaves from decaying, enter\n",
                        TextColors.GREEN, "/cf leaf-decay any false");
            case LIQUID_FLOW :
                return Text.of("Controls whether liquid is allowed to flow.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent liquid flow, enter\n",
                        TextColors.GREEN, "/cf liquid-flow any false");
            case PORTAL_USE :
                return Text.of("Controls whether a portal can be used.\n",
                        TextColors.LIGHT_PURPLE, "Example 1", TextColors.WHITE, " : To prevent any source from using portals, enter\n",
                        TextColors.GREEN, "/cf portal-use any false\n",
                        TextColors.LIGHT_PURPLE, "Example 2", TextColors.WHITE, " : To prevent only players from using portals, enter\n",
                        TextColors.GREEN, "/cf portal-use minecraft:player any false");
            case PROJECTILE_IMPACT_BLOCK :
                return Text.of("Controls whether a projectile can impact(collide) with a block.\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : This involves things such as potions, arrows, throwables, pixelmon pokeballs, etc.\n",
                        TextColors.LIGHT_PURPLE, "Example 1", TextColors.WHITE, " : To prevent any projectile from impacting a block, enter\n",
                        TextColors.GREEN, "/cf projectile-impact-block any false\n",
                        TextColors.LIGHT_PURPLE, "Example 2", TextColors.WHITE, " : To allow pixelmon pokeball's to impact blocks, enter\n",
                        TextColors.GREEN, "/cf projectile-impact-block pixelmon:occupiedpokeball any true");
            case PROJECTILE_IMPACT_ENTITY :
                return Text.of("Controls whether a projectile can impact(collide) with an entity.\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : This involves things such as potions, arrows, throwables, pixelmon pokeballs, etc.\n",
                        TextColors.LIGHT_PURPLE, "Example 1", TextColors.WHITE, " : To prevent any projectile from impacting an entity, enter\n",
                        TextColors.GREEN, "/cf projectile-impact-entity any false\n",
                        TextColors.LIGHT_PURPLE, "Example 2", TextColors.WHITE, " : To allow arrows to impact entities, enter\n",
                        TextColors.GREEN, "/cf projectile-impact-entity minecraft:arrow any true");
                default :
                    return Text.of("Not defined.");
        }
    }
}
