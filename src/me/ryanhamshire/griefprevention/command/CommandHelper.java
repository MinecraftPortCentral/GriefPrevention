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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import me.ryanhamshire.griefprevention.GPFlags;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimResultType;
import me.ryanhamshire.griefprevention.api.claim.TrustType;
import me.ryanhamshire.griefprevention.claim.ClaimsMode;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.claim.GPClaimResult;
import me.ryanhamshire.griefprevention.command.CommandClaimFlag.FlagType;
import me.ryanhamshire.griefprevention.event.GPTrustClaimEvent;
import me.ryanhamshire.griefprevention.message.Messages;
import me.ryanhamshire.griefprevention.message.TextMode;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.context.Context;
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
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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

    public static CommandResult abandonClaimHandler(Player player, boolean deleteTopLevelClaim) {
        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());

        // which claim is being abandoned?
        GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAt(player.getLocation(), true, null);
        if (claim.isWildernessClaim()) {
            GriefPreventionPlugin.sendMessage(player, TextMode.Instr, Messages.AbandonClaimMissing);
        }

        // verify ownership
        else if (claim.allowEdit(player) != null) {
            GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.NotYourClaim);
        }

        // warn if has children and we're not explicitly deleting a top level claim
        else if (claim.children.size() > 0 && !deleteTopLevelClaim) {
            GriefPreventionPlugin.sendMessage(player, TextMode.Instr, Messages.DeleteTopLevelClaim);
            return CommandResult.empty();
        } else {
            // delete it
            claim.removeSurfaceFluids(null);
            GriefPreventionPlugin.instance.dataStore.deleteClaim(claim, Cause.of(NamedCause.source(player)));

            // if in a creative mode world, restore the claim area
            if (GriefPreventionPlugin.instance.claimModeIsActive(claim.getLesserBoundaryCorner().getExtent().getProperties(), ClaimsMode.Creative)) {
                GriefPreventionPlugin.addLogEntry(
                        player.getName() + " abandoned a claim @ " + GriefPreventionPlugin.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
                GriefPreventionPlugin.sendMessage(player, TextMode.Warn, Messages.UnclaimCleanupWarning);
                GriefPreventionPlugin.instance.restoreClaim(claim, 20L * 60 * 2);
            }

            // adjust claim blocks when abandoning a top level claim
            if (!claim.isSubdivision() && !claim.isAdminClaim()) {
                playerData.setAccruedClaimBlocks(
                        playerData.getAccruedClaimBlocks() - (int) Math
                                .ceil((claim.getArea() * (1 - playerData.optionAbandonReturnRatio))));

                // tell the player how many claim blocks he has left
                int remainingBlocks = playerData.getRemainingClaimBlocks();
                GriefPreventionPlugin.sendMessage(player, TextMode.Success, Messages.AbandonSuccess, String.valueOf(remainingBlocks));
            }

            // revert any current visualization
            playerData.revertActiveVisual(player);
            playerData.warnedAboutMajorDeletion = false;
        }

        return CommandResult.success();

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

    public static boolean validateFlagTarget(String flag, String target) {
        switch(flag) {
            case GPFlags.BLOCK_BREAK :
            case GPFlags.BLOCK_PLACE :
            case GPFlags.ENTITY_COLLIDE_BLOCK :
            case GPFlags.PORTAL_USE :
                if (validateBlockTarget(target) ||
                    validateItemTarget(target)) {
                    return true;
                }

                return false;
            case GPFlags.ENTER_CLAIM :
            case GPFlags.EXIT_CLAIM :
            case GPFlags.ENTITY_RIDING :
            case GPFlags.ENTITY_FALL :
                return validateEntityTarget(target);
            case GPFlags.ENTITY_DAMAGE :
                if (validateEntityTarget(target) ||
                    validateBlockTarget(target) ||
                    validateItemTarget(target)) {
                    return true;
                }

                return false;
            case GPFlags.INTERACT_INVENTORY :
            case GPFlags.LIQUID_FLOW :
                return validateBlockTarget(target);
            case GPFlags.INTERACT_BLOCK_PRIMARY :
            case GPFlags.INTERACT_BLOCK_SECONDARY :
                return validateBlockTarget(target);
            case GPFlags.INTERACT_ENTITY_PRIMARY :
            case GPFlags.INTERACT_ENTITY_SECONDARY :
                return validateEntityTarget(target);
            case GPFlags.ENTITY_SPAWN :
                return validateEntityTarget(target);
            case GPFlags.ITEM_DROP :
            case GPFlags.ITEM_PICKUP :
            case GPFlags.ITEM_SPAWN :
            case GPFlags.ITEM_USE :
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
        if (context.equalsIgnoreCase("default") || context.equalsIgnoreCase("defaults")) {
            if (claim.isAdminClaim()) {
                return GriefPreventionPlugin.ADMIN_CLAIM_FLAG_DEFAULT_CONTEXT;
            } else if (claim.isBasicClaim() || claim.isSubdivision()) {
                return GriefPreventionPlugin.BASIC_CLAIM_FLAG_DEFAULT_CONTEXT;
            } else {
                return GriefPreventionPlugin.WILDERNESS_CLAIM_FLAG_DEFAULT_CONTEXT;
            }
        } else if (context.equalsIgnoreCase("override") || context.equalsIgnoreCase("overrides") || context.equalsIgnoreCase("force") || context.equalsIgnoreCase("forced")) {
            if (claim.isAdminClaim()) {
                return GriefPreventionPlugin.ADMIN_CLAIM_FLAG_OVERRIDE_CONTEXT;
            } else if (claim.isBasicClaim() || claim.isSubdivision()) {
                return GriefPreventionPlugin.BASIC_CLAIM_FLAG_OVERRIDE_CONTEXT;
            } else {
                src.sendMessage(Text.of(TextMode.Err, "Claim type " + claim.type.name() + " does not support flag overrides."));
                return null;
            }
        } else {
            GriefPreventionPlugin.sendMessage(src, Text.of(TextMode.Err, "Context '" + context + "' was not found."));
            return null;
        }
    }

    public static CommandResult addFlagPermission(CommandSource src, Subject subject, String subjectName, GPClaim claim, String baseFlag, String source, String target, Tristate value, String context) {
        if (src instanceof Player) {
            String denyReason = claim.allowEdit((Player) src);
            if (denyReason != null) {
                GriefPreventionPlugin.sendMessage(src, Text.of(TextMode.Err, denyReason));
                return CommandResult.success();
            }
        }

        if (!GPFlags.FLAG_LIST.contains(baseFlag)) {
            src.sendMessage(Text.of(TextColors.RED, "Flag not found."));
            return CommandResult.success();
        }

        String flagPermission = GPPermissions.FLAG_BASE + "." + baseFlag;
        // special handling for commands
        if (baseFlag.equals(GPFlags.COMMAND_EXECUTE) || baseFlag.equals(GPFlags.COMMAND_EXECUTE_PVP)) {
            target = handleCommandFlag(src, target);
            if (target == null) {
                // failed
                return CommandResult.success();
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
                            GriefPreventionPlugin.sendMessage(src, Text.of(TextMode.Err, "Invalid target meta '" + parts[1] + "' entered for flag " + baseFlag + "."));
                            return CommandResult.success();
                        }
                    }
                    String entitySpawnFlag = GPFlags.getEntitySpawnFlag(baseFlag, targetFlag);
                    if (entitySpawnFlag == null && !CommandHelper.validateFlagTarget(baseFlag, targetFlag)) {
                        GriefPreventionPlugin.sendMessage(src, Text.of(TextMode.Err, "Invalid target id '" + targetFlag + "' entered for flag " + baseFlag + "."));
                        return CommandResult.success();
                    }
        
                    if (entitySpawnFlag != null) {
                        target = entitySpawnFlag;
                    } else {
                        target = baseFlag + "." + target.replace(":", ".");//.replace("[", ".[");
                    }
                }

                flagPermission = GPPermissions.FLAG_BASE + "." + target;
            } else {
                target = "";
            }
        }

        return applyFlagPermission(src, subject, subjectName, claim, flagPermission, source, target, value, context, null);
    }

    public static CommandResult applyFlagPermission(CommandSource src, Subject subject, String subjectName, GPClaim claim, String flagPermission, String source, String target, Tristate value, String context, FlagType flagType) {
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

        String basePermission = flagPermission.replace(GPPermissions.FLAG_BASE + ".", "");
        int endIndex = basePermission.indexOf(".");
        if (endIndex != -1) {
            basePermission = basePermission.substring(0, endIndex);
        }

        // Check if player can manage flag
        if (src instanceof Player) {
            Player player = (Player) src;
            GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
            Tristate result = Tristate.UNDEFINED;
            if (!playerData.canManageAdminClaims && GriefPreventionPlugin.getActiveConfig(player.getWorld().getProperties()).getConfig().flags.getUserClaimFlags().contains(basePermission)) {
                result = Tristate.fromBoolean(src.hasPermission(GPPermissions.USER_CLAIM_FLAGS + "." + basePermission));
            } else if (result != Tristate.TRUE && playerData.canManageAdminClaims) {
                result = Tristate.fromBoolean(src.hasPermission(GPPermissions.ADMIN_CLAIM_FLAGS + "." + basePermission));
            }
            if (result != Tristate.TRUE) {
                GriefPreventionPlugin.sendMessage(src, Text.of(TextMode.Err, "No permission to use this flag."));
                return CommandResult.success();
            }
        }

        Set<Context> contexts = new HashSet<>();
        Context customContext = null;
        if (context != null) {
            String targetContext = context;
            customContext = CommandHelper.validateCustomContext(src, claim, targetContext);
            if (customContext == null) {
                GriefPreventionPlugin.sendMessage(src, Text.of(TextMode.Err, "Context '" + targetContext + "' is invalid."));
                return CommandResult.success();
            } else {
                // validate perms
                if (customContext == GriefPreventionPlugin.ADMIN_CLAIM_FLAG_DEFAULT_CONTEXT || 
                        customContext == GriefPreventionPlugin.BASIC_CLAIM_FLAG_DEFAULT_CONTEXT || 
                        customContext == GriefPreventionPlugin.WILDERNESS_CLAIM_FLAG_DEFAULT_CONTEXT) {
                    if (!src.hasPermission(GPPermissions.MANAGE_FLAG_DEFAULTS)) {
                        GriefPreventionPlugin.sendMessage(src, Text.of(TextMode.Err, "No permission to manage flag defaults."));
                        return CommandResult.success();
                    }
                } else if (customContext == GriefPreventionPlugin.ADMIN_CLAIM_FLAG_OVERRIDE_CONTEXT || 
                        customContext == GriefPreventionPlugin.BASIC_CLAIM_FLAG_OVERRIDE_CONTEXT) {
                    if (!src.hasPermission(GPPermissions.MANAGE_FLAG_OVERRIDES)) {
                        GriefPreventionPlugin.sendMessage(src, Text.of(TextMode.Err, "No permission to manage flag overrides."));
                        return CommandResult.success();
                    }
                }
            }
            contexts.add(customContext);
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

        if (subject == GriefPreventionPlugin.GLOBAL_SUBJECT) {
            if (customContext == null || (customContext != GriefPreventionPlugin.ADMIN_CLAIM_FLAG_DEFAULT_CONTEXT && customContext != GriefPreventionPlugin.ADMIN_CLAIM_FLAG_OVERRIDE_CONTEXT
                    && customContext != GriefPreventionPlugin.BASIC_CLAIM_FLAG_DEFAULT_CONTEXT && customContext != GriefPreventionPlugin.BASIC_CLAIM_FLAG_OVERRIDE_CONTEXT && customContext != GriefPreventionPlugin.WILDERNESS_CLAIM_FLAG_DEFAULT_CONTEXT)) {
                contexts.add(claim.getContext());
            } else {
                contexts.add(claim.world.getContext());
            }

            GriefPreventionPlugin.GLOBAL_SUBJECT.getSubjectData().setPermission(contexts, flagPermission, value);
            src.sendMessage(Text.of(
                    Text.builder().append(Text.of(
                            TextColors.WHITE, "\n[", TextColors.AQUA, "Return to flags", TextColors.WHITE, "]\n"))
                        .onClick(TextActions.executeCallback(createCommandConsumer(src, "claimflag", ""))).build(),
                    TextColors.GREEN, "Set permission of ", 
                    TextColors.AQUA, flagPermission.replace(GPPermissions.FLAG_BASE + ".", ""), 
                    TextColors.GREEN, " to ", 
                    flagType == null ? Text.of(TextColors.LIGHT_PURPLE, value) : Text.of(getFlagTypeColor(flagType), getClickableText(src,  GriefPreventionPlugin.GLOBAL_SUBJECT, subjectName, contexts, flagPermission, value, flagType)), 
                    TextColors.GREEN, " for ", 
                    TextColors.GOLD, "ALL."));
        } else {
            if (!contexts.contains(claim.getContext())) {
                contexts.add(claim.getContext());
            }

            subject.getSubjectData().setPermission(contexts, flagPermission, value);
            src.sendMessage(Text.of(
                    Text.builder().append(Text.of(
                            TextColors.WHITE, "\n[", TextColors.AQUA, "Return to flags", TextColors.WHITE, "]\n"))
                        .onClick(TextActions.executeCallback(createCommandConsumer(src, "claimflaggroup", subjectName))).build(),
                    TextColors.GREEN, "Set permission of ", 
                    TextColors.AQUA, flagPermission.replace(GPPermissions.FLAG_BASE + ".", ""), 
                    TextColors.GREEN, " to ", 
                    flagType == null ? Text.of(TextColors.LIGHT_PURPLE, value) : Text.of(getFlagTypeColor(flagType), getClickableText(src,  subject, subjectName, contexts, flagPermission, value, flagType)), 
                    TextColors.GREEN, " for ", 
                    TextColors.GOLD, subjectName));
        }

        return CommandResult.success();
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

    public static Consumer<CommandSource> createFlagConsumer(CommandSource src, Subject subject, String subjectName, Set<Context> contexts, String flagPermission, Tristate flagValue, FlagType type) {
        return consumer -> {
            Tristate newValue = Tristate.UNDEFINED;
            if (flagValue == Tristate.TRUE) {
                newValue = Tristate.FALSE;
            } else if (flagValue == Tristate.UNDEFINED) {
                newValue = Tristate.TRUE;
            }

            String target = flagPermission.replace(GPPermissions.FLAG_BASE + ".",  "");
            Set<Context> newContexts = new HashSet<>(contexts);
            subject.getSubjectData().setPermission(newContexts, flagPermission, newValue);
            src.sendMessage(Text.of(
                    TextColors.GREEN, "Set permission of ", 
                    TextColors.AQUA, target, 
                    TextColors.GREEN, " to ", 
                    getFlagTypeColor(type), getClickableText(src, subject, subjectName, newContexts, flagPermission, newValue, type), 
                    TextColors.GREEN, " for ", 
                    TextColors.GOLD, subjectName, "."));
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

    public static void showOverlapClaims(CommandSource src, List<Claim> overlappingClaims) {
        List<Text> claimsTextList = Lists.newArrayList();
        for (Claim claim : overlappingClaims) {
            Location<World> southWest = claim.getLesserBoundaryCorner().setPosition(new Vector3d(claim.getLesserBoundaryCorner().getPosition().getX(), 65.0D, claim.getGreaterBoundaryCorner().getPosition().getZ()));
            // inform player
            Text claimInfoCommandClick = Text.builder().append(Text.of(
                    TextColors.GREEN, claim.getName().orElse(Text.of("Claim"))))
            .onClick(TextActions.executeCallback(CommandHelper.createCommandConsumer(src, "claiminfo", claim.getUniqueId().toString(), CommandHelper.createReturnOverlapConsumer(src, overlappingClaims))))
            .onHover(TextActions.showText(Text.of("Click here to check claim info.")))
            .build();

            Text claimCoordsTPClick = Text.builder().append(Text.of(
                    TextColors.GRAY, southWest.getBlockPosition()))
            .onClick(TextActions.executeCallback(CommandHelper.createTeleportConsumer(src, southWest, claim)))
            .onHover(TextActions.showText(Text.of("Click here to teleport to ", claim.getName().orElse(Text.of("none")), ".")))
            .build();

            claimsTextList.add(Text.builder()
                    .append(Text.of(
                            claimInfoCommandClick, TextColors.WHITE, " : ", 
                            claimCoordsTPClick, " ", 
                            TextColors.YELLOW, "(Area : " + claim.getArea() + " blocks)"))
                    .build());
        }

        PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
        PaginationList.Builder paginationBuilder = paginationService.builder()
                .title(Text.of(TextColors.RED,"Overlapping Claims")).padding(Text.of("-")).contents(claimsTextList);
        paginationBuilder.sendTo(src);
    }

    private static Consumer<CommandSource> createReturnOverlapConsumer(CommandSource src, List<Claim> overlappingClaims) {
        return consumer -> {
            Text overlapClaimsReturnCommand = Text.builder().append(Text.of(
                    TextColors.WHITE, "\n[", TextColors.AQUA, "Return to overlapping claims", TextColors.WHITE, "]\n"))
                .onClick(TextActions.executeCallback(CommandHelper.createOverlapConsumer(src, overlappingClaims))).build();
            src.sendMessage(overlapClaimsReturnCommand);
        };
    }

    private static Consumer<CommandSource> createOverlapConsumer(CommandSource src, List<Claim> overlappingClaims) {
        return consumer -> {
            showOverlapClaims(src, overlappingClaims);
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
        String onClickText = "Click here to toggle flag value.";
        boolean hasPermission = true;
        if (src instanceof Player) {
            String denyReason = claim.allowEdit((Player) src);
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

    public static void handleTrustCommand(Player player, TrustType trustType, User user) {
        if (user == null) {
            GriefPreventionPlugin.sendMessage(player, Text.of(TextMode.Err, "Not a valid player."));
            return;
        }

        // determine which claim the player is standing in
        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAtPlayer(playerData, player.getLocation(), true);
        if (user.getUniqueId().equals(player.getUniqueId()) && !playerData.canIgnoreClaim(claim)) {
            GriefPreventionPlugin.sendMessage(player, Text.of(TextMode.Err, "You cannot trust yourself."));
            return;
        }

        ArrayList<Claim> targetClaims = new ArrayList<>();
        if (claim == null) {
            GriefPreventionPlugin.sendMessage(player, Text.of(TextMode.Err, "No claim found at location. If you want to trust all claims, use /trustall instead."));
            return;
        } else if (claim.getOwnerUniqueId().equals(user.getUniqueId())) {
            GriefPreventionPlugin.sendMessage(player, Text.of(TextMode.Err, "You are already the claim owner."));
            return;
        } else {
            //check permission here
            if(claim.allowGrantPermission(player) != null) {
                GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.NoPermissionTrust, claim.getOwnerName());
                return;
            }
            
            //see if the player has the level of permission he's trying to grant
            String errorMessage = null;
            //permission level null indicates granting permission trust
            if(trustType == TrustType.MANAGER) {
                errorMessage = claim.allowEdit(player);
                if(errorMessage != null) {
                    //error message for trying to grant a permission the player doesn't have
                    errorMessage = "Only " + claim.getOwnerName() + " can grant /PermissionTrust here.";
                    GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.CantGrantThatPermission);
                    return;
                }
            }

            targetClaims.add(claim);
        }

        GPTrustClaimEvent.Add event = new GPTrustClaimEvent.Add(targetClaims, Cause.of(NamedCause.source(player)), ImmutableList.of(user.getUniqueId()), trustType);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            player.sendMessage(Text.of(TextColors.RED, event.getMessage().orElse(Text.of("Could not trust user '" + user.getName() + "'. A plugin has denied it."))));
            return;
        }

        String location = GriefPreventionPlugin.instance.dataStore.getMessage(Messages.LocationCurrentClaim);
        for (Claim currentClaim : targetClaims) {
            ArrayList<UUID> memberList = null;
            GPClaim gpClaim = (GPClaim) currentClaim;
            if (trustType == TrustType.ACCESSOR) {
                memberList = (ArrayList<UUID>) gpClaim.getInternalClaimData().getAccessors();
            } else if (trustType == TrustType.CONTAINER) {
                memberList = (ArrayList<UUID>) gpClaim.getInternalClaimData().getContainers();
            } else if (trustType == TrustType.BUILDER) {
                memberList = (ArrayList<UUID>) gpClaim.getInternalClaimData().getBuilders();
            } else if (trustType == TrustType.MANAGER) {
                memberList = (ArrayList<UUID>) gpClaim.getInternalClaimData().getManagers();
            }

            if (memberList.contains(user.getUniqueId())) {
                String message = "Player " + user.getName() + " already has " + trustType.name() + " permission.";
                if (user == GriefPreventionPlugin.PUBLIC_USER) {
                    message = "Public already has " + trustType.name() + " permission.";
                }
                GriefPreventionPlugin.sendMessage(player, Text.of(TextMode.Info, message));
                return;
            } else {
                memberList.add(user.getUniqueId());
            }

            gpClaim.getInternalClaimData().setRequiresSave(true);
        }

        //notify player
        String recipientName = user.getName();
        if(user.getName().equalsIgnoreCase("public") || user.getName().equalsIgnoreCase("all")) {
            recipientName = GriefPreventionPlugin.instance.dataStore.getMessage(Messages.CollectivePublic);
        }
        String permissionDescription;
        if(trustType == TrustType.MANAGER) {
            permissionDescription = GriefPreventionPlugin.instance.dataStore.getMessage(Messages.PermissionsPermission);
        } else if(trustType == TrustType.BUILDER) {
            permissionDescription = GriefPreventionPlugin.instance.dataStore.getMessage(Messages.BuildPermission);
        } else if(trustType == TrustType.ACCESSOR) {
            permissionDescription = GriefPreventionPlugin.instance.dataStore.getMessage(Messages.AccessPermission);
        } else {
            permissionDescription = GriefPreventionPlugin.instance.dataStore.getMessage(Messages.ContainersPermission);
        }

        GriefPreventionPlugin.sendMessage(player, TextMode.Success, Messages.GrantPermissionConfirmation, recipientName, permissionDescription, location);
    }

    public static Consumer<CommandSource> createTeleportConsumer(CommandSource src, Location<World> location, Claim claim) {
        return teleport -> {
            if (!(src instanceof Player)) {
                // ignore
                return;
            }
            Player player = (Player) src;
            GPClaim gpClaim = (GPClaim) claim;
            // if not owner of claim, validate perms
            if (!player.getUniqueId().equals(claim.getOwnerUniqueId())) {
                if (!gpClaim.getInternalClaimData().getContainers().contains(player.getUniqueId()) 
                        && !gpClaim.getInternalClaimData().getBuilders().contains(player.getUniqueId())
                        && !gpClaim.getInternalClaimData().getManagers().contains(player.getUniqueId())
                        && !player.hasPermission(GPPermissions.COMMAND_CLAIM_INFO_TELEPORT_OTHERS)) {
                    player.sendMessage(Text.of(TextColors.RED, "You do not have permission to use the teleport feature in this claim.")); 
                    return;
                }
            } else if (!player.hasPermission(GPPermissions.COMMAND_CLAIM_INFO_TELEPORT_BASE)) {
                player.sendMessage(Text.of(TextColors.RED, "You do not have permission to use the teleport feature in your claim.")); 
                return;
            }

            Location<World> safeLocation = Sponge.getGame().getTeleportHelper().getSafeLocation(location, 9, 9).orElse(null);
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
        }
        return hoverText;
    }

    public static Text getBaseFlagOverlayText(String flagPermission) {
        String baseFlag = flagPermission.replace(GPPermissions.FLAG_BASE + ".", "");
        int endIndex = baseFlag.indexOf(".");
        if (endIndex != -1) {
            baseFlag = baseFlag.substring(0, endIndex);
        }

        switch(baseFlag) {
            case GPFlags.BLOCK_BREAK :
                return Text.of("Controls whether a block can be broken.\n",
                        TextColors.LIGHT_PURPLE, "Example 1", TextColors.WHITE, " : To prevent any source from breaking dirt blocks, enter\n",
                        TextColors.GREEN, "/cf block-break minecraft:dirt false\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : ", "minecraft represents the modid and dirt represents the block id.\n",
                            "Specifying no modid will always default to minecraft.\n",
                        TextColors.LIGHT_PURPLE, "Example 2", TextColors.WHITE, " : To prevent players from breaking dirt blocks, enter\n",
                        TextColors.GREEN, "/cf block-break minecraft:player minecraft:dirt false\n");
            case GPFlags.BLOCK_PLACE :
                return Text.of("Controls whether a block can be placed.\n",
                        TextColors.LIGHT_PURPLE, "Example 1", TextColors.WHITE, " : To prevent any source from placing dirt blocks, enter\n",
                        TextColors.GREEN, "/cf block-place minecraft:dirt false\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : ", "minecraft represents the modid and dirt represents the block id.\n",
                            "Specifying no modid will always default to minecraft.\n",
                        TextColors.LIGHT_PURPLE, "Example 2", TextColors.WHITE, " : To prevent players from placing dirt blocks, enter\n",
                        TextColors.GREEN, "/cf block-place minecraft:player minecraft:dirt false\n");
            case GPFlags.COMMAND_EXECUTE :
                return Text.of("Controls whether a command can be executed.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent pixelmon's command '/shop select' from being run, enter\n",
                        TextColors.GREEN, "/cf command-execute pixelmon:shop[select] false\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : ", 
                        TextStyles.ITALIC, TextColors.GOLD, "pixelmon", TextStyles.RESET, TextColors.RESET, " represents the modid, ", 
                        TextStyles.ITALIC, TextColors.GOLD, "shop", TextStyles.RESET, TextColors.RESET, " represents the base command, and ",
                        TextStyles.ITALIC, TextColors.GOLD, "select", TextStyles.RESET, TextColors.RESET,  " represents the argument.\n",
                            "Specifying no modid will always default to minecraft.\n");
            case GPFlags.COMMAND_EXECUTE_PVP :
                return Text.of("Controls whether a command can be executed while engaged in ", TextColors.RED, "PvP.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent pixelmon's command '/shop select' from being run, enter\n",
                        TextColors.GREEN, "/cf command-execute pixelmon:shop[select] false\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : ", 
                        TextStyles.ITALIC, TextColors.GOLD, "pixelmon", TextStyles.RESET, TextColors.RESET, " represents the modid, ", 
                        TextStyles.ITALIC, TextColors.GOLD, "shop", TextStyles.RESET, TextColors.RESET, " represents the base command, and ",
                        TextStyles.ITALIC, TextColors.GOLD, "select", TextStyles.RESET, TextColors.RESET,  " represents the argument.\n",
                            "Specifying no modid will always default to minecraft.\n");
            case GPFlags.ENTER_CLAIM :
                return Text.of("Controls whether an entity can enter claim.\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : If you want to use this for players, it is recommended to use \n", 
                        "the '/cfg' command with the group the player is in.");
            case GPFlags.ENTITY_COLLIDE_BLOCK :
                return Text.of("Controls whether an entity can collide with a block.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent entity collisions with dirt blocks, enter\n",
                        TextColors.GREEN, "/cf entity-collide-block minecraft:dirt false");
            case GPFlags.ENTITY_COLLIDE_ENTITY :
                return Text.of("Controls whether an entity can collide with an entity.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent entity collisions with item frames, enter\n",
                        TextColors.GREEN, "/cf entity-collide-entity minecraft:itemframe false");
            case GPFlags.ENTITY_DAMAGE :
                return Text.of("Controls whether an entity can be damaged.\n",
                        TextColors.LIGHT_PURPLE, "Example 1", TextColors.WHITE, " : To prevent horses from being damaged, enter\n",
                        TextColors.GREEN, "/cf entity-damage minecraft:horse false\n",
                        TextColors.LIGHT_PURPLE, "Example 2", TextColors.WHITE, " : To prevent all animals from being damaged, enter\n",
                        TextColors.GREEN, "/cf entity-damage minecraft:animals false");
            case GPFlags.ENTITY_FALL :
                return Text.of("Controls whether an entity can receive fall damage.\n",
                        TextColors.LIGHT_PURPLE, "Example 1", TextColors.WHITE, " : To prevent players from taking fall damage, enter\n",
                        TextColors.GREEN, "/cf entity-fall minecraft:player false\n",
                        TextColors.LIGHT_PURPLE, "Example 2", TextColors.WHITE, " : To prevent horses from taking fall damage, enter\n",
                        TextColors.GREEN, "/cf entity-fall minecraft:horse false");
            case GPFlags.ENTITY_RIDING :
                return Text.of("Controls whether an entity can be mounted.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent horses from being mounted enter\n",
                        TextColors.GREEN, "/cf entity-riding minecraft:horse false");
            case GPFlags.ENTITY_SPAWN :
                return Text.of("Controls whether an entity can be spawned into the world.\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : This does not include entity items. See item-spawn flag.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent horses from spawning enter\n",
                        TextColors.GREEN, "/cf entity-spawn minecraft:horse false");
            case GPFlags.ENTITY_TELEPORT_FROM :
                return Text.of("Controls whether an entity can teleport from their current location.\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : If you want to use this for players, it is recommended to use \n", 
                        "the '/cfg' command with the group the player is in.");
            case GPFlags.ENTITY_TELEPORT_TO :
                return Text.of("Controls whether an entity can teleport to a location.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent creepers from traveling and/or teleporting within your claim, enter\n",
                        TextColors.GREEN, "/cf entity-teleport-to minecraft:creeper false\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : If you want to use this for players, it is recommended to use \n", 
                        "the '/cfg' command with the group the player is in.");
            case GPFlags.EXIT_CLAIM :
                return Text.of("Controls whether an entity can exit claim.\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : If you want to use this for players, it is recommended to use \n", 
                        "the '/cfg' command with the group the player is in.");
            case GPFlags.EXPLOSION :
                return Text.of("Controls whether an explosion can occur in the world.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent any explosion, enter\n",
                        TextColors.GREEN, "/cf explosion any false");
            case GPFlags.EXPLOSION_SURFACE :
                return Text.of("Controls whether an explosion can occur above the surface in a world.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent an explosion above surface, enter\n",
                        TextColors.GREEN, "/cf explosion-surface any false");
            case GPFlags.FIRE_SPREAD :
                return Text.of("Controls whether fire can spread in a world.\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : This does not prevent the initial fire being placed, only spread.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent fire from spreading, enter\n",
                        TextColors.GREEN, "/cf fire-spread any false");
            case GPFlags.INTERACT_BLOCK_PRIMARY :
                return Text.of("Controls whether a player can left-click(attack) a block.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent players from left-clicking chests, enter\n",
                        TextColors.GREEN, "/cf interact-block-primary minecraft:chest false");
            case GPFlags.INTERACT_BLOCK_SECONDARY :
                return Text.of("Controls whether a player can right-click a block.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent players from right-clicking(opening) chests, enter\n",
                        TextColors.GREEN, "/cf interact-block-secondary minecraft:chest false");
            case GPFlags.INTERACT_ENTITY_PRIMARY :
                return Text.of("Controls whether a player can left-click(attack) an entity.\n",
                    TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent players from left-clicking horses, enter\n",
                    TextColors.GREEN, "/cf interact-entity-primary minecraft:player minecraft:horse false\n");
            case GPFlags.INTERACT_ENTITY_SECONDARY :
                return Text.of("Controls whether a player can right-click on an entity.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent horses from being mounted, enter\n",
                        TextColors.GREEN, "/cf interact-entity-secondary minecraft:horse false\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : ", "minecraft represents the modid and horse represents the entity id.\n",
                            "Specifying no modid will always default to minecraft.\n");
            case GPFlags.INTERACT_INVENTORY :
                return Text.of("Controls whether a player can interact with a block that contains inventory such as a chest.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent players from interacting with any block that contains inventory, enter\n",
                        TextColors.GREEN, "/cf interact-inventory any false");
            case GPFlags.INTERACT_ITEM_PRIMARY :
                return Text.of("Controls whether a player can left-click(attack) with an item.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent players from left-clicking while holding a diamond sword, enter\n",
                        TextColors.GREEN, "/cf interact-item-primary minecraft:diamond_sword false");
            case GPFlags.INTERACT_ITEM_SECONDARY :
                return Text.of("Controls whether a player can right-click with an item.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent players from right-clicking while holding a flint and steel, enter\n",
                        TextColors.GREEN, "/cf interact-item-secondary minecraft:flint_and_steel false");
            case GPFlags.ITEM_DROP :
                return Text.of("Controls whether an item can be dropped.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent tnt from dropping in the world, enter\n",
                        TextColors.GREEN, "/cf item-drop minecraft:tnt false");
            case GPFlags.ITEM_PICKUP :
                return Text.of("Controls whether an item can be picked up.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent tnt from dropping in the world, enter\n",
                        TextColors.GREEN, "/cf item-drop minecraft:tnt false");
            case GPFlags.ITEM_SPAWN :
                return Text.of("Controls whether an item can be spawned into the world up.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent feather's from dropping in the world, enter\n",
                        TextColors.GREEN, "/cf item-drop minecraft:feather false");
            case GPFlags.ITEM_USE :
                return Text.of("Controls whether an item can be used.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent usage of diamond swords, enter\n",
                        TextColors.GREEN, "/cf item-use minecraft:diamond_sword false");
            case GPFlags.LIQUID_FLOW :
                return Text.of("Controls whether liquid is allowed to flow.\n",
                        TextColors.LIGHT_PURPLE, "Example", TextColors.WHITE, " : To prevent liquid flow, enter\n",
                        TextColors.GREEN, "/cf liquid-flow any false");
            case GPFlags.PORTAL_USE :
                return Text.of("Controls whether a portal can be used.\n",
                        TextColors.LIGHT_PURPLE, "Example 1", TextColors.WHITE, " : To prevent any source from using portals, enter\n",
                        TextColors.GREEN, "/cf portal-use any false\n",
                        TextColors.LIGHT_PURPLE, "Example 2", TextColors.WHITE, " : To prevent only players from using portals, enter\n",
                        TextColors.GREEN, "/cf portal-use minecraft:player any false");
            case GPFlags.PROJECTILE_IMPACT_BLOCK :
                return Text.of("Controls whether a projectile can impact(collide) with a block.\n",
                        TextColors.AQUA, "Note", TextColors.WHITE, " : This involves things such as potions, arrows, throwables, pixelmon pokeballs, etc.\n",
                        TextColors.LIGHT_PURPLE, "Example 1", TextColors.WHITE, " : To prevent any projectile from impacting a block, enter\n",
                        TextColors.GREEN, "/cf projectile-impact-block any false\n",
                        TextColors.LIGHT_PURPLE, "Example 2", TextColors.WHITE, " : To allow pixelmon pokeball's to impact blocks, enter\n",
                        TextColors.GREEN, "/cf projectile-impact-block pixelmon:occupiedpokeball any true");
            case GPFlags.PROJECTILE_IMPACT_ENTITY :
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
