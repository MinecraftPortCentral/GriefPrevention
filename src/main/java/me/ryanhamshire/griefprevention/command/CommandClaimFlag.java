/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import me.ryanhamshire.griefprevention.DataStore;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimContexts;
import me.ryanhamshire.griefprevention.api.claim.ClaimFlag;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.api.claim.FlagResult;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.event.GPFlagClaimEvent;
import me.ryanhamshire.griefprevention.permission.GPPermissionHandler;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import me.ryanhamshire.griefprevention.util.ClaimClickData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandMessageFormatting;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.Tristate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

public class CommandClaimFlag implements CommandExecutor {

    public enum FlagType {
        ALL,
        DEFAULT,
        CLAIM,
        OVERRIDE,
        INHERIT,
        GROUP,
        PLAYER
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        String flag = ctx.<String>getOne("flag").orElse(null);
        String source = ctx.<String>getOne("source").orElse(null);
        String target = null;
        // Workaround command API issue not handling onlyOne arguments with sequences properly
        List<String> targetValues = new ArrayList<>(ctx.<String>getAll("target"));
        if (targetValues.size() > 0) {
            if (targetValues.size() > 1) {
                target = targetValues.get(1);
            } else {
                target = targetValues.get(0);
            }
        }

        if (source != null && source.equalsIgnoreCase("any")) {
            source = null;
        }
        if (source != null && source.equalsIgnoreCase("hand")) {
            if (!(src instanceof Player)) {
                src.sendMessage(Text.of(TextColors.RED, "The variable hand can only be used with a Player!."));
                return CommandResult.success();
            }
            final Player player = (Player) src;
            ItemStack stack = player.getItemInHand(HandTypes.MAIN_HAND).orElse(null);
            if (stack != null) {
                source = GPPermissionHandler.getPermissionIdentifier(stack);
                source = filterMeta(stack, source);
            }
        }
        if (target != null && target.equalsIgnoreCase("hand")) {
            if (!(src instanceof Player)) {
                src.sendMessage(Text.of(TextColors.RED, "The variable hand can only be used with a Player!."));
                return CommandResult.success();
            }
            final Player player = (Player) src;
            ItemStack stack = player.getItemInHand(HandTypes.MAIN_HAND).orElse(null);
            if (stack != null) {
                target = GPPermissionHandler.getPermissionIdentifier(stack);
                target = filterMeta(stack, target);
            }
        }

        Tristate value = ctx.<Tristate>getOne("value").orElse(null);
        String context = ctx.<String>getOne("context").orElse(null);
        String reason = ctx.<String>getOne("reason").orElse(null);
        Text reasonText = null;
        Player player;
        if (reason != null) {
            reasonText = TextSerializers.FORMATTING_CODE.deserialize(reason);
        }

        try {
            player = GriefPreventionPlugin.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAtPlayer(playerData, player.getLocation(), false);
        if (claim != null) {
            if (flag == null && value == null && src.hasPermission(GPPermissions.COMMAND_LIST_CLAIM_FLAGS)) {
                showFlagPermissions(src, claim, FlagType.ALL, source);
                return CommandResult.success();
            } else if (flag != null && value != null) {
                if (!ClaimFlag.contains(flag)) {
                    src.sendMessage(Text.of(TextColors.RED, "Flag not found."));
                    return CommandResult.success();
                }
                Context claimContext = claim.getContext();
                if (context != null) {
                    claimContext = CommandHelper.validateCustomContext(src, claim, context);
                    if (claimContext == null) {
                        final Text message = GriefPreventionPlugin.instance.messageData.flagInvalidContext
                                .apply(ImmutableMap.of(
                                "context", context,
                                "flag", flag)).build();
                        GriefPreventionPlugin.sendMessage(src, message);
                        return CommandResult.success();
                    }
                }

                claim.setPermission(GriefPreventionPlugin.GLOBAL_SUBJECT, "ALL", ClaimFlag.getEnum(flag), source, target, value, claimContext, reasonText, Cause.source(src).build());
                //CommandHelper.addFlagPermission(src, GriefPreventionPlugin.GLOBAL_SUBJECT, "ALL", claim, ClaimFlag.valueOf(flag), source, target, value, claimContext);
                return CommandResult.success();
            }

            GriefPreventionPlugin.sendMessage(src, CommandMessageFormatting.error(Text.of("Usage: /cf [<flag> <target> <value> [subject|context]]")));
        } else {
            GriefPreventionPlugin.sendMessage(src, GriefPreventionPlugin.instance.messageData.claimNotFound.toText());
        }
        return CommandResult.success();
    }

    private String filterMeta(ItemStack stack, String id) {
        if (stack != null && stack.getItem() != null) {
            final int meta = ((net.minecraft.item.ItemStack)(Object) stack).getItemDamage();
            if (meta == 0) {
                return GPPermissionHandler.getIdentifierWithoutMeta(id);
            }
        }
        return id;
    }

    private static void showFlagPermissions(CommandSource src, GPClaim claim, FlagType type, String source) {
            final Text whiteOpenBracket = Text.of(TextColors.AQUA, "[");
            final Text whiteCloseBracket = Text.of(TextColors.AQUA, "]");
            final Text showAllText = Text.of("Click here to show all flag permissions for claim.");
            final Text showDefaultText = Text.of("Click here to filter by ", TextColors.LIGHT_PURPLE, "DEFAULT ", TextColors.RESET, "permissions.");
            final Text showClaimText = Text.of("Click here to filter by ", TextColors.GOLD, "CLAIM ", TextColors.RESET, "permissions.");
            final Text showInheritText = Text.of("Click here to filter by ", TextColors.AQUA, "INHERIT ", TextColors.RESET, "permissions.");
            final Text showOverrideText = Text.of("Click here to filter by ", TextColors.RED, "OVERRIDE ", TextColors.RESET, "permissions.");
            final Text allTypeText = Text.builder()
                    .append(Text.of(type == FlagType.ALL ? Text.of(whiteOpenBracket, TextColors.GOLD, "ALL", whiteCloseBracket) : Text.of(TextColors.GRAY, "ALL")))
                    .onClick(TextActions.executeCallback(createClaimFlagConsumer(src, claim, FlagType.ALL, source)))
                    .onHover(TextActions.showText(showAllText)).build();
            final Text defaultFlagText = Text.builder()
                    .append(Text.of(type == FlagType.DEFAULT ? Text.of(whiteOpenBracket, TextColors.LIGHT_PURPLE, "DEFAULT", whiteCloseBracket) : Text.of(TextColors.GRAY, "DEFAULT")))
                    .onClick(TextActions.executeCallback(createClaimFlagConsumer(src, claim, FlagType.DEFAULT, source)))
                    .onHover(TextActions.showText(showDefaultText)).build();
            final Text claimFlagText = Text.builder()
                    .append(Text.of(type == FlagType.CLAIM ? Text.of(whiteOpenBracket, TextColors.YELLOW, "CLAIM", whiteCloseBracket) : Text.of(TextColors.GRAY, "CLAIM")))
                    .onClick(TextActions.executeCallback(createClaimFlagConsumer(src, claim, FlagType.CLAIM, source)))
                    .onHover(TextActions.showText(showClaimText)).build();
            final Text inheritFlagText = Text.builder()
                    .append(Text.of(type == FlagType.INHERIT ? Text.of(whiteOpenBracket, TextColors.AQUA, "INHERIT", whiteCloseBracket) : Text.of(TextColors.GRAY, "INHERIT")))
                    .onClick(TextActions.executeCallback(createClaimFlagConsumer(src, claim, FlagType.INHERIT, source)))
                    .onHover(TextActions.showText(showInheritText)).build();
            final Text overrideFlagText = Text.builder()
                    .append(Text.of(type == FlagType.OVERRIDE ? Text.of(whiteOpenBracket, TextColors.RED, "OVERRIDE", whiteCloseBracket) : Text.of(TextColors.GRAY, "OVERRIDE")))
                    .onClick(TextActions.executeCallback(createClaimFlagConsumer(src, claim, FlagType.OVERRIDE, source)))
                    .onHover(TextActions.showText(showOverrideText)).build();
            final Text claimFlagHead = Text.builder().append(Text.of(
                    TextColors.AQUA," Displaying : ", allTypeText, "  ", defaultFlagText, "  ", claimFlagText, "  ", inheritFlagText, "  ", overrideFlagText)).build();
            Map<String, Text> flagList = new TreeMap<>();
            Set<Context> contexts = new HashSet<>();
            Set<Context> overrideContexts = new HashSet<>();
            if (claim.isAdminClaim()) {
                contexts.add(ClaimContexts.ADMIN_DEFAULT_CONTEXT);
                overrideContexts.add(ClaimContexts.ADMIN_OVERRIDE_CONTEXT);
            } else if (claim.isBasicClaim() || claim.isSubdivision()) {
                contexts.add(ClaimContexts.BASIC_DEFAULT_CONTEXT);
                overrideContexts.add(ClaimContexts.BASIC_OVERRIDE_CONTEXT);
            } else if (claim.isTown()) {
                contexts.add(ClaimContexts.TOWN_DEFAULT_CONTEXT);
                overrideContexts.add(ClaimContexts.TOWN_OVERRIDE_CONTEXT);
            } else {
                contexts.add(ClaimContexts.WILDERNESS_DEFAULT_CONTEXT);
                overrideContexts.add(ClaimContexts.WILDERNESS_OVERRIDE_CONTEXT);
            }
            contexts.add(claim.world.getContext());
            if (!overrideContexts.isEmpty()) {
                overrideContexts.add(claim.world.getContext());
            }

            Map<String, Boolean> defaultTransientPermissions = GriefPreventionPlugin.GLOBAL_SUBJECT.getTransientSubjectData().getPermissions(contexts);
            Map<String, Boolean> defaultTransientOverridePermissions = GriefPreventionPlugin.GLOBAL_SUBJECT.getSubjectData().getPermissions(contexts);
            Map<String, Boolean> overridePermissions = GriefPreventionPlugin.GLOBAL_SUBJECT.getSubjectData().getPermissions(overrideContexts);
            Map<String, Boolean> claimPermissions = new HashMap<>(GriefPreventionPlugin.GLOBAL_SUBJECT.getSubjectData().getPermissions(ImmutableSet.of(claim.context)));
            Map<String, ClaimClickData> inheritPermissions = new HashMap<>();
            final List<Claim> inheritParents = claim.getInheritedParents();
            Collections.reverse(inheritParents);
            for (Claim current : inheritParents) {
                GPClaim currentClaim = (GPClaim) current;
                Map<String, Boolean> currentPermissions = new HashMap<>(GriefPreventionPlugin.GLOBAL_SUBJECT.getSubjectData().getPermissions(ImmutableSet.of(currentClaim.context)));
                for (Map.Entry<String, Boolean> permissionEntry : currentPermissions.entrySet()) {
                    claimPermissions.put(permissionEntry.getKey(), permissionEntry.getValue());
                    inheritPermissions.put(permissionEntry.getKey(), new ClaimClickData(currentClaim, permissionEntry.getValue()));
                }
            }
            final Text denyText = claim.allowEdit((Player) src);
            final boolean hasPermission = denyText == null;

            if (type == FlagType.ALL) {
                for (Map.Entry<String, Boolean> permissionEntry : defaultTransientPermissions.entrySet()) {
                    Text flagText = null;
                    String flagPermission = permissionEntry.getKey();
                    String baseFlagPerm = flagPermission.replace(GPPermissions.FLAG_BASE + ".",  "");
                    // check if transient default has been overridden and if so display that value instead
                    Boolean defaultTransientOverrideValue = defaultTransientOverridePermissions.get(flagPermission);
                    if (defaultTransientOverrideValue != null) {
                        continue;
                    } else {
                        Text baseFlagText = Text.builder().append(Text.of(TextColors.GREEN, baseFlagPerm))
                                .onHover(TextActions.showText(CommandHelper.getBaseFlagOverlayText(baseFlagPerm))).build();
                        flagText = Text.of(
                                baseFlagText, "  ",
                                TextColors.WHITE, "[",
                                TextColors.LIGHT_PURPLE, getClickableText(src, claim, flagPermission, Tristate.fromBoolean(permissionEntry.getValue()), source, FlagType.DEFAULT));
                    }
                    if (claimPermissions.get(permissionEntry.getKey()) == null) {
                        flagText = Text.join(flagText, 
                                Text.of(
                                TextColors.WHITE, ", ",
                                TextColors.GOLD, getClickableText(src, claim, flagPermission, Tristate.UNDEFINED, source, FlagType.CLAIM)));
                        if (overridePermissions.get(flagPermission) == null) {
                            flagText = Text.join(flagText, Text.of(TextColors.WHITE, "]"));
                        }
                    }
                    flagList.put(flagPermission, flagText);
                }
    
                for (Map.Entry<String, Boolean> permissionEntry : defaultTransientOverridePermissions.entrySet()) {
                    Text flagText = null;
                    String flagPermission = permissionEntry.getKey();
                    String baseFlagPerm = flagPermission.replace(GPPermissions.FLAG_BASE + ".",  "");
                    Text baseFlagText = null;
                    if (ClaimFlag.contains(baseFlagPerm)) {
                        baseFlagText = Text.builder().append(Text.of(TextColors.GREEN, baseFlagPerm))
                                .onHover(TextActions.showText(CommandHelper.getBaseFlagOverlayText(baseFlagPerm))).build();
                    }
    
                    baseFlagPerm = flagPermission.replace(GPPermissions.FLAG_BASE + ".",  "");
                    flagText = Text.of(
                            TextColors.GREEN, baseFlagText != null ? baseFlagText : baseFlagPerm, "  ",
                            TextColors.WHITE, "[",
                            TextColors.LIGHT_PURPLE, getClickableText(src, claim, flagPermission, Tristate.fromBoolean(permissionEntry.getValue()), source, FlagType.DEFAULT));
    
                    if (claimPermissions.get(permissionEntry.getKey()) == null) {
                        flagText = Text.join(flagText, 
                                Text.of(
                                TextColors.WHITE, ", ",
                                TextColors.GOLD, getClickableText(src, claim, flagPermission, Tristate.UNDEFINED, source, FlagType.CLAIM)));
                        if (overridePermissions.get(flagPermission) == null) {
                            flagText = Text.join(flagText, Text.of(TextColors.WHITE, "]"));
                        }
                    }
                    flagList.put(flagPermission, flagText);
                }
    
                for (Map.Entry<String, Boolean> permissionEntry : claimPermissions.entrySet()) {
                    String flagPermission = permissionEntry.getKey();
                    Boolean flagValue = permissionEntry.getValue();
                    Text flagText = null;
                    ClaimClickData claimClickData = inheritPermissions.get(flagPermission);
                    if (claimClickData != null) {
                        flagText = Text.of(TextColors.AQUA, getClickableText(src, claimClickData.claim, flagPermission, Tristate.fromBoolean(claimClickData.value), source, FlagType.INHERIT));
                    } else {
                        flagText = Text.of(TextColors.GOLD, getClickableText(src, claim, flagPermission, Tristate.fromBoolean(flagValue), source, FlagType.CLAIM));
                    }
    
                    Text currentText = flagList.get(flagPermission);
                    boolean customFlag = false;
                    if (currentText == null) {
                        customFlag = true;
                        // custom flag
                        String baseFlagPerm = flagPermission.replace(GPPermissions.FLAG_BASE + ".",  "");
                        currentText = Text.builder().append(Text.of(
                                TextColors.GREEN, baseFlagPerm, "  ",
                                TextColors.WHITE, "["))
                                .onHover(TextActions.showText(CommandHelper.getBaseFlagOverlayText(baseFlagPerm))).build();
                    }
    
                    if (overridePermissions.get(flagPermission) == null) {
                        flagList.put(flagPermission, Text.join(currentText, Text.of(customFlag ? "" : ", ", flagText, TextColors.WHITE, "]")));
                    } else {
                        flagList.put(flagPermission, Text.join(currentText, Text.of(customFlag ? "" : ", ", flagText)));
                    }
                }
    
                for (Map.Entry<String, Boolean> permissionEntry : overridePermissions.entrySet()) {
                    String flagPermission = permissionEntry.getKey();
                    Boolean flagValue = permissionEntry.getValue();
                    Text flagText = Text.of(TextColors.RED, getClickableText(src, claim, flagPermission, Tristate.fromBoolean(flagValue), source, FlagType.OVERRIDE));
                    Text currentText = flagList.get(flagPermission);
                    boolean customFlag = false;
                    if (currentText == null) {
                        customFlag = true;
                        // custom flag
                        String baseFlagPerm = flagPermission.replace(GPPermissions.FLAG_BASE + ".",  "");
                        currentText = Text.builder().append(Text.of(
                                TextColors.GREEN, baseFlagPerm, "  ",
                                TextColors.WHITE, "["))
                                .onHover(TextActions.showText(CommandHelper.getBaseFlagOverlayText(baseFlagPerm))).build();
                    }
    
                    flagList.put(flagPermission, Text.join(currentText, Text.of(customFlag ? "" : ", ", flagText, TextColors.WHITE, "]")));
                }
            } else if (type == FlagType.CLAIM) {
                for (Map.Entry<String, Boolean> permissionEntry : claimPermissions.entrySet()) {
                    String flagPermission = permissionEntry.getKey();
                    final String baseFlagPerm = flagPermission.replace(GPPermissions.FLAG_BASE + ".",  "");
                    Boolean flagValue = permissionEntry.getValue();
                    Text flagText = null;
                    final ClaimFlag baseFlag = GPPermissionHandler.getFlagFromPermission(flagPermission);
                    if (baseFlag == null) {
                        // invalid flag
                        continue;
                    }

                    boolean hasOverride = false;
                    for (Map.Entry<String, Boolean> mapEntry : overridePermissions.entrySet()) {
                        if (flagPermission.contains(mapEntry.getKey())) {
                            hasOverride = true;
                            Text undefinedText = null;
                            if (hasPermission) {
                                undefinedText = Text.builder().append(
                                    Text.of(TextColors.GRAY, "undefined"))
                                    .onHover(TextActions.showText(Text.of(TextColors.GREEN, baseFlagPerm, TextColors.WHITE, " is currently being ", TextColors.RED, "overridden", TextColors.WHITE, " by an administrator", TextColors.WHITE, ".\nClick here to remove this flag.")))
                                    .onClick(TextActions.executeCallback(createFlagConsumer(src, claim, flagPermission, Tristate.UNDEFINED, source, type, FlagType.CLAIM, false))).build();
                            } else {
                                undefinedText = Text.builder().append(
                                        Text.of(TextColors.GRAY, "undefined"))
                                        .onHover(TextActions.showText(denyText)).build();
                            }
                            flagText = Text.builder().append(
                                    Text.of(undefinedText, "  ", TextColors.AQUA, "[", TextColors.RED, mapEntry.getValue(), TextStyles.RESET, TextColors.AQUA, "]"))
                                    .onHover(TextActions.showText(Text.of(TextColors.WHITE, "This flag has been overridden by an administrator and can ", TextColors.RED, TextStyles.UNDERLINE, "NOT", TextStyles.RESET, TextColors.WHITE, " be changed.")))
                                    .build();
                            break;
                        }
                    }
                    if (!hasOverride) {
                        ClaimClickData claimClickData = inheritPermissions.get(flagPermission);
                        if (claimClickData != null) {
                            final Text undefinedText = getClickableText(src, claimClickData.claim, flagPermission, Tristate.fromBoolean(flagValue), Tristate.UNDEFINED, source, type, FlagType.INHERIT, false);
                            final Text trueText = getClickableText(src, claimClickData.claim, flagPermission, Tristate.fromBoolean(flagValue), Tristate.TRUE, source, type, FlagType.INHERIT, false);
                            final Text falseText = getClickableText(src, claimClickData.claim, flagPermission, Tristate.fromBoolean(flagValue), Tristate.FALSE, source, type, FlagType.INHERIT, false);
                            flagText = Text.of(undefinedText, "  ", trueText, "  ", falseText);
                        } else {
                            final Text undefinedText = getClickableText(src, claim, flagPermission, Tristate.fromBoolean(flagValue), Tristate.UNDEFINED, source, type, FlagType.CLAIM, false);
                            final Text trueText = getClickableText(src, claim, flagPermission, Tristate.fromBoolean(flagValue), Tristate.TRUE, source, type, FlagType.CLAIM, false);
                            final Text falseText = getClickableText(src, claim, flagPermission, Tristate.fromBoolean(flagValue), Tristate.FALSE, source, type, FlagType.CLAIM, false);
                            flagText = Text.of(undefinedText, "  ", trueText, "  ", falseText);
                        }
                    }
    
                    Text currentText = flagList.get(flagPermission);
                    if (currentText == null) {
                        currentText = Text.builder().append(Text.of(
                                TextColors.GREEN, baseFlagPerm, "  "))
                                //TextColors.WHITE, "["))
                                .onHover(TextActions.showText(CommandHelper.getBaseFlagOverlayText(baseFlagPerm))).build();
                    }
                    Text baseFlagText = getFlagText(flagPermission, baseFlag.toString()); 
                    flagText = Text.of(
                            baseFlagText, "  ",
                            flagText);
                    flagList.put(flagPermission, flagText);//Text.join(currentText, Text.of(customFlag ? "" : ", ", flagText)));
                }
                for (Map.Entry<String, Boolean> permissionEntry : defaultTransientPermissions.entrySet()) {
                    Text flagText = null;
                    String flagPermission = permissionEntry.getKey();
                    if (flagList.containsKey(flagPermission)) {
                        // only display flags not overridden
                        continue;
                    }

                    Boolean flagValue = permissionEntry.getValue();
                    String baseFlagPerm = flagPermission.replace(GPPermissions.FLAG_BASE + ".",  "");
                    final ClaimFlag baseFlag = GPPermissionHandler.getFlagFromPermission(baseFlagPerm);
                    if (baseFlag == null) {
                        // invalid flag
                        continue;
                    }

                    boolean hasOverride = false;
                    for (Map.Entry<String, Boolean> mapEntry : overridePermissions.entrySet()) {
                        if (flagPermission.contains(mapEntry.getKey())) {
                            hasOverride = true;
                            flagText = Text.builder().append(
                                    Text.of(TextColors.RED, mapEntry.getValue()))
                                    .onHover(TextActions.showText(Text.of(TextColors.GREEN, baseFlagPerm, TextColors.WHITE, " is currently being ", TextColors.RED, "overridden", TextColors.WHITE, " by an administrator and can ", TextColors.RED, TextStyles.UNDERLINE, "NOT", TextStyles.RESET, TextColors.WHITE, " be changed.")))
                                    .build();
                            break;
                        }
                    }
                    if (!hasOverride) {
                        // check if transient default has been overridden and if so display that value instead
                        Boolean defaultTransientOverrideValue = defaultTransientOverridePermissions.get(flagPermission);
                        if (defaultTransientOverrideValue != null) {
                            flagValue = defaultTransientOverrideValue;
                        }
    
                        Text undefinedText = null;
                        if (hasPermission) {
                            undefinedText = Text.builder().append(
                                Text.of(TextColors.AQUA, "[", TextColors.GOLD, "undefined", TextStyles.RESET, TextColors.AQUA, "]"))
                                .onHover(TextActions.showText(Text.of(TextColors.GREEN, baseFlagPerm, TextColors.WHITE, " is currently not set.\nThe default claim value of ", TextColors.LIGHT_PURPLE, flagValue, TextColors.WHITE, " will be active until set.")))
                                .onClick(TextActions.executeCallback(createFlagConsumer(src, claim, flagPermission, Tristate.fromBoolean(flagValue), source, type, FlagType.CLAIM, false))).build();
                        } else {
                            undefinedText = Text.builder().append(
                                    Text.of(TextColors.AQUA, "[", TextColors.GOLD, "undefined", TextStyles.RESET, TextColors.AQUA, "]"))
                                    .onHover(TextActions.showText(denyText)).build();
                        }
                        final Text trueText = Text.of(TextColors.GRAY, getClickableText(src, claim, flagPermission, Tristate.UNDEFINED, Tristate.TRUE, source, type, FlagType.CLAIM, false));
                        final Text falseText = Text.of(TextColors.GRAY, getClickableText(src, claim, flagPermission, Tristate.UNDEFINED, Tristate.FALSE, source, type, FlagType.CLAIM, false));
                        flagText = Text.of(undefinedText, "  ", trueText, "  ", falseText);
                    }
                    Text baseFlagText = getFlagText(flagPermission, baseFlag.toString()); 
                    flagText = Text.of(
                            baseFlagText, "  ",
                            flagText);
                    flagList.put(flagPermission, flagText);
                }
            } else if (type == FlagType.OVERRIDE) {
                for (Map.Entry<String, Boolean> permissionEntry : overridePermissions.entrySet()) {
                    String flagPermission = permissionEntry.getKey();
                    Boolean flagValue = permissionEntry.getValue();
                    Text flagText = Text.of(TextColors.RED, getClickableText(src, claim, flagPermission, Tristate.fromBoolean(flagValue), source, FlagType.OVERRIDE));
                    Text currentText = flagList.get(flagPermission);
                    String baseFlagPerm = flagPermission.replace(GPPermissions.FLAG_BASE + ".",  "");
                    boolean customFlag = false;
                    Text hover = CommandHelper.getBaseFlagOverlayText(baseFlagPerm);
                    if (claim.isWilderness()) {
                        Text reason = GriefPreventionPlugin.getGlobalConfig().getConfig().bans.getReason(baseFlagPerm);
                        if (reason != null && !reason.isEmpty()) {
                            hover = Text.of(TextColors.GREEN, "Ban Reason", TextColors.WHITE, " : ", reason);
                        }
                    }
                    if (currentText == null) {
                        customFlag = true;
                        // custom flag
                        currentText = Text.builder().append(Text.of(
                                TextColors.GREEN, baseFlagPerm, "  ",
                                TextColors.WHITE, "["))
                                .onHover(TextActions.showText(hover)).build();
                    }
    
                    flagList.put(flagPermission, Text.join(currentText, Text.of(customFlag ? "" : ", ", flagText, TextColors.WHITE, "]")));
                }
            } else if (type == FlagType.INHERIT) {
                for (Map.Entry<String, ClaimClickData> permissionEntry : inheritPermissions.entrySet()) {
                    String flagPermission = permissionEntry.getKey();
                    final String baseFlagPerm = flagPermission.replace(GPPermissions.FLAG_BASE + ".",  "");
                    final ClaimClickData claimClickData = permissionEntry.getValue();
                    final boolean flagValue = claimClickData.value;
                    Text flagText = null;
                    final ClaimFlag baseFlag = GPPermissionHandler.getFlagFromPermission(flagPermission);
                    if (baseFlag == null) {
                        // invalid flag
                        continue;
                    }

                    boolean hasOverride = false;
                    for (Map.Entry<String, Boolean> mapEntry : overridePermissions.entrySet()) {
                        if (flagPermission.contains(mapEntry.getKey())) {
                            hasOverride = true;
                            final Text undefinedText = Text.builder().append(
                                    Text.of(TextColors.GRAY, "undefined"))
                                    .onHover(TextActions.showText(Text.of(TextColors.GREEN, baseFlagPerm, TextColors.WHITE, " is currently being ", TextColors.RED, "overridden", TextColors.WHITE, " by an administrator", TextColors.WHITE, ".\nClick here to remove this flag.")))
                                    .onClick(TextActions.executeCallback(createFlagConsumer(src, claim, flagPermission, Tristate.UNDEFINED, source, type, FlagType.CLAIM, false))).build();
                            flagText = Text.builder().append(
                                    Text.of(undefinedText, "  ", TextColors.AQUA, "[", TextColors.RED, mapEntry.getValue(), TextStyles.RESET, TextColors.AQUA, "]"))
                                    .onHover(TextActions.showText(Text.of(TextColors.WHITE, "This flag has been overridden by an administrator and can ", TextColors.RED, TextStyles.UNDERLINE, "NOT", TextStyles.RESET, TextColors.WHITE, " be changed.")))
                                    .build();
                            break;
                        }
                    }

                    if (!hasOverride) {
                        flagText = Text.of(TextColors.AQUA, getClickableText(src, claimClickData.claim, flagPermission, Tristate.fromBoolean(flagValue), source, FlagType.INHERIT));
                    }

                    Text currentText = flagList.get(flagPermission);
                    if (currentText == null) {
                        currentText = Text.builder().append(Text.of(
                                TextColors.GREEN, baseFlagPerm, "  "))
                                .onHover(TextActions.showText(CommandHelper.getBaseFlagOverlayText(baseFlagPerm))).build();
                    }
                    Text baseFlagText = getFlagText(flagPermission, baseFlag.toString()); 
                    flagText = Text.of(
                            baseFlagText, "  ",
                            flagText);
                    flagList.put(flagPermission, flagText);
                }
            } else if (type == FlagType.DEFAULT) {
                for (Map.Entry<String, Boolean> permissionEntry : defaultTransientPermissions.entrySet()) {
                    Text flagText = null;
                    String flagPermission = permissionEntry.getKey();
                    Boolean flagValue = permissionEntry.getValue();
                    String baseFlagPerm = flagPermission.replace(GPPermissions.FLAG_BASE + ".",  "");
                    if (!ClaimFlag.contains(baseFlagPerm)) {
                        continue;
                    }
                    final ClaimFlag baseFlag = ClaimFlag.getEnum(baseFlagPerm);

                    // check if transient default has been overridden and if so display that value instead
                    Boolean defaultTransientOverrideValue = defaultTransientOverridePermissions.get(flagPermission);
                    if (defaultTransientOverrideValue != null) {
                        flagValue = defaultTransientOverrideValue;
                    }

                    final Text trueText = Text.of(TextColors.GRAY, getClickableText(src, claim, flagPermission, Tristate.fromBoolean(flagValue), Tristate.TRUE, source, type, FlagType.DEFAULT, false));
                    final Text falseText = Text.of(TextColors.GRAY, getClickableText(src, claim, flagPermission, Tristate.fromBoolean(flagValue), Tristate.FALSE, source, type, FlagType.DEFAULT, false));
                    flagText = Text.of(trueText, "  ", falseText);
                    Text baseFlagText = getFlagText(flagPermission, baseFlag.toString()); 
                    flagText = Text.of(
                            baseFlagText, "  ",
                            flagText);
                    flagList.put(flagPermission, flagText);
                }
            }

            List<Text> textList = new ArrayList<>(flagList.values());
            int fillSize = 20 - (textList.size() + 2);
            for (int i = 0; i < fillSize; i++) {
                textList.add(Text.of(" "));
            }
            PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
            PaginationList.Builder paginationBuilder = paginationService.builder()
                    .title(claimFlagHead).padding(Text.of(TextStyles.STRIKETHROUGH,"-")).contents(textList);
            paginationBuilder.sendTo(src);
    }

    private static Text getFlagText(String flagPermission, String baseFlag) {
        final String flagSource = GPPermissionHandler.getSourcePermission(flagPermission);
        final String flagTarget = GPPermissionHandler.getTargetPermission(flagPermission);
        Text sourceText = flagSource == null ? null : Text.of(TextColors.WHITE, "source=",TextColors.GREEN, flagSource);
        Text targetText = flagTarget == null ? null : Text.of(TextColors.WHITE, "target=",TextColors.GREEN, flagTarget);
        if (sourceText != null) {
           /* if (targetText != null) {
                sourceText = Text.of(sourceText, "  ");
            } else {*/
                sourceText = Text.of(sourceText, "\n");
            //}
        } else {
            sourceText = Text.of();
        }
        if (targetText != null) {
            targetText = Text.of(targetText);
        } else {
            targetText = Text.of();
        }
        Text baseFlagText = Text.of();
        if (flagSource == null && flagTarget == null) {
            baseFlagText = Text.builder().append(Text.of(TextColors.GREEN, baseFlag.toString(), " "))
                .onHover(TextActions.showText(Text.of(sourceText, targetText))).build();
        } else {
            baseFlagText = Text.builder().append(Text.of(TextStyles.ITALIC, TextColors.YELLOW, baseFlag.toString(), " ", TextStyles.RESET))
                    .onHover(TextActions.showText(Text.of(sourceText, targetText))).build();
        }
        final Text baseText = Text.builder().append(Text.of(
                baseFlagText)).build();
                //sourceText,
                //targetText)).build();
        return baseText;
    }

    public static Consumer<CommandSource> createFlagConsumer(CommandSource src, GPClaim claim, String flagPermission, Tristate flagValue, String source, FlagType displayType, FlagType type, boolean toggleType) {
        return consumer -> {
            // Toggle DEFAULT type
            final String sourceId = GPPermissionHandler.getSourcePermission(flagPermission);
            final String targetId = GPPermissionHandler.getTargetPermission(flagPermission);
            final ClaimFlag claimFlag = GPPermissionHandler.getFlagFromPermission(flagPermission);
            if (claimFlag == null) {
                return;
            }
            Context claimContext = claim.getContext();
            Tristate newValue = Tristate.UNDEFINED;
            if (type == FlagType.DEFAULT) {
                if (toggleType) {
                    if (flagValue == Tristate.TRUE) {
                        newValue = Tristate.FALSE;
                    } else {
                        newValue = Tristate.TRUE;
                    }
                    ClaimType claimType = claim.getType();
                    if (claimType == ClaimType.SUBDIVISION) {
                        claimType = ClaimType.BASIC;
                    }
                    final Boolean defaultValue = DataStore.CLAIM_FLAG_DEFAULTS.get(claimType).get(claimFlag.toString());
                    if (defaultValue != null && defaultValue == newValue.asBoolean()) {
                        newValue = Tristate.UNDEFINED;
                    }
                }
                claimContext = CommandHelper.validateCustomContext(src, claim, "default");
            // Toggle CLAIM type
            } else if (type == FlagType.CLAIM) {
                if (flagValue == Tristate.TRUE) {
                    newValue = Tristate.FALSE;
                } else if (flagValue == Tristate.UNDEFINED) {
                    newValue = Tristate.TRUE;
                }
            // Toggle OVERRIDE type
            } else if (type == FlagType.OVERRIDE) {
                if (flagValue == Tristate.TRUE) {
                    newValue = Tristate.FALSE;
                } else if (flagValue == Tristate.UNDEFINED) {
                    newValue = Tristate.TRUE;
                }
                claimContext = CommandHelper.validateCustomContext(src, claim, "override");
            }
            GPFlagClaimEvent.Set event = new GPFlagClaimEvent.Set(claim, GriefPreventionPlugin.GLOBAL_SUBJECT, claimFlag, sourceId, targetId, toggleType ? newValue : flagValue, claimContext, Cause.source(src).build());
            Sponge.getEventManager().post(event);
            if (event.isCancelled()) {
                return;
            }
            FlagResult result = CommandHelper.applyFlagPermission(src, GriefPreventionPlugin.GLOBAL_SUBJECT, "ALL", claim, flagPermission, source, "any", toggleType ? newValue : flagValue, claimContext, type, null, true);
            if (result.successful()) {
                showFlagPermissions(src, claim, displayType, source);
            }
        };
    }

    public static Text getClickableText(CommandSource src, GPClaim claim, String flagPermission, Tristate flagValue, String source, FlagType type) {
        return getClickableText(src, claim, flagPermission, Tristate.UNDEFINED, flagValue, source, FlagType.ALL, type, true);
    }

    public static Text getClickableText(CommandSource src, GPClaim claim, String flagPermission, Tristate currentValue, Tristate flagValue, String source, FlagType displayType, FlagType flagType, boolean toggleType) {
        Text onHoverText = Text.of("Click here to toggle " + flagType.name().toLowerCase() + " value.");
        if (!toggleType) {
            if (flagValue == Tristate.TRUE) {
                onHoverText = Text.of("Click here to allow this flag.");
            } else if (flagValue == Tristate.FALSE) {
                onHoverText = Text.of("Click here to deny this flag.");
            } else {
                onHoverText = Text.of("Click here to remove this flag.");
            }
        }
        TextColor flagColor = TextColors.GOLD;
        boolean hasPermission = true;
        if (flagType == FlagType.DEFAULT) {
            flagColor = TextColors.LIGHT_PURPLE;
            if (!src.hasPermission(GPPermissions.MANAGE_FLAG_DEFAULTS)) {
                onHoverText = Text.of("You do not have permission to change flag defaults.");
                hasPermission = false;
            }
        }
        if (flagType == FlagType.OVERRIDE) {
            flagColor = TextColors.RED;
            if (!src.hasPermission(GPPermissions.MANAGE_FLAG_OVERRIDES)) {
                onHoverText = Text.of("This flag has been forced by an admin and cannot be changed.");
                hasPermission = false;
            }
        } else if (flagType == FlagType.INHERIT) {
            flagColor = TextColors.AQUA;
            onHoverText = Text.of("This flag is inherited from parent claim ", claim.getName().orElse(claim.getFriendlyNameType()), " and ", TextStyles.UNDERLINE, "cannot", TextStyles.RESET, " be changed.");
            hasPermission = false;
        } else if (src instanceof Player) {
            Text denyReason = claim.allowEdit((Player) src);
            if (denyReason != null) {
                onHoverText = denyReason;
                hasPermission = false;
            }
        }

        if (toggleType) {
            Text.Builder textBuilder = Text.builder()
                    .append(Text.of(flagValue.toString().toLowerCase()))
                    .onHover(TextActions.showText(Text.of(onHoverText, "\n", CommandHelper.getFlagTypeHoverText(flagType))));
            if (hasPermission) {
                textBuilder.onClick(TextActions.executeCallback(createFlagConsumer(src, claim, flagPermission, flagValue, source, displayType, flagType, true)));
            }
            return textBuilder.build();
        }

        Text.Builder textBuilder = Text.builder()
                .append(Text.of(flagValue.toString().toLowerCase()))
                .onHover(TextActions.showText(Text.of(onHoverText, "\n", CommandHelper.getFlagTypeHoverText(flagType))));
        if (hasPermission) {
            textBuilder.onClick(TextActions.executeCallback(createFlagConsumer(src, claim, flagPermission, flagValue, source, displayType, flagType, false)));
        }
        Text result = textBuilder.build();
        if (currentValue == flagValue) {
            final Text whiteOpenBracket = Text.of(TextColors.AQUA, "[");
            final Text whiteCloseBracket = Text.of(TextColors.AQUA, "]");
            result = Text.of(whiteOpenBracket, flagColor, result, whiteCloseBracket);
        } else {
            result = Text.of(TextColors.GRAY, result, TextColors.RESET);
        }
        return result;
    }

    private static Consumer<CommandSource> createClaimFlagConsumer(CommandSource src, GPClaim claim, FlagType type, String source) {
        return consumer -> {
            showFlagPermissions(src, claim, type, source);
        };
    }
}
