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

import com.google.common.collect.ImmutableSet;
import me.ryanhamshire.griefprevention.GPFlags;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.message.TextMode;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandMessageFormatting;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

public class CommandClaimFlag implements CommandExecutor {

    public enum FlagType {
        DEFAULT,
        CLAIM,
        OVERRIDE,
        GROUP,
        PLAYER
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        String flag = ctx.<String>getOne("flag").orElse(null);
        String source = ctx.<String>getOne("source").orElse(null);
        String target = ctx.<String>getOne("target").orElse(null);
        if (source != null && source.equalsIgnoreCase(target)) {
            source = null;
        }

        Tristate value = ctx.<Tristate>getOne("value").orElse(null);
        String context = ctx.<String>getOne("context").orElse(null);
        Player player;

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
                Map<String, Text> flagList = new TreeMap<>();
                Set<Context> contexts = new HashSet<>();
                Set<Context> overrideContexts = new HashSet<>();
                if (claim.isAdminClaim()) {
                    contexts.add(GriefPreventionPlugin.ADMIN_CLAIM_FLAG_DEFAULT_CONTEXT);
                    overrideContexts.add(GriefPreventionPlugin.ADMIN_CLAIM_FLAG_OVERRIDE_CONTEXT);
                } else if (claim.isBasicClaim() || claim.isSubdivision()) {
                    contexts.add(GriefPreventionPlugin.BASIC_CLAIM_FLAG_DEFAULT_CONTEXT);
                    overrideContexts.add(GriefPreventionPlugin.BASIC_CLAIM_FLAG_OVERRIDE_CONTEXT);
                } else {
                    contexts.add(GriefPreventionPlugin.WILDERNESS_CLAIM_FLAG_DEFAULT_CONTEXT);
                }
                contexts.add(claim.world.getContext());
                if (!overrideContexts.isEmpty()) {
                    overrideContexts.add(claim.world.getContext());
                }

                Map<String, Boolean> defaultTransientPermissions = GriefPreventionPlugin.GLOBAL_SUBJECT.getTransientSubjectData().getPermissions(contexts);
                Map<String, Boolean> defaultTransientOverridePermissions = GriefPreventionPlugin.GLOBAL_SUBJECT.getSubjectData().getPermissions(contexts);
                Map<String, Boolean> overridePermissions = GriefPreventionPlugin.GLOBAL_SUBJECT.getSubjectData().getPermissions(overrideContexts);
                Map<String, Boolean> claimPermissions = GriefPreventionPlugin.GLOBAL_SUBJECT.getSubjectData().getPermissions(ImmutableSet.of(claim.context));
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
                    if (GPFlags.FLAG_LIST.contains(baseFlagPerm)) {
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
                    Text flagText = Text.of(TextColors.GOLD, getClickableText(src, claim, flagPermission, Tristate.fromBoolean(flagValue), source, FlagType.CLAIM));
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

                List<Text> textList = new ArrayList<>(flagList.values());
                PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
                PaginationList.Builder paginationBuilder = paginationService.builder()
                        .title(Text.of(TextColors.AQUA, "Claim Flag Permissions")).padding(Text.of("-")).contents(textList);
                paginationBuilder.sendTo(src);
                return CommandResult.success();
            } else if (flag != null && value != null) {
                if (GPFlags.FLAG_LIST.contains(flag)) {
                    CommandHelper.addFlagPermission(src, GriefPreventionPlugin.GLOBAL_SUBJECT, "ALL", claim, flag, source, target, value, context);
                } else {
                    GriefPreventionPlugin.sendMessage(src, Text.of(TextMode.Err, "Invalid flag entered."));
                }
                return CommandResult.success();
            }

            GriefPreventionPlugin.sendMessage(src, CommandMessageFormatting.error(Text.of("Usage: /cf [<flag> <target> <value> [subject|context]]")));
        } else {
            GriefPreventionPlugin.sendMessage(src, Text.of(TextMode.Err, "No claim found."));
        }
        return CommandResult.success();
    }

    public static Consumer<CommandSource> createFlagConsumer(CommandSource src, GPClaim claim, String flagPermission, Tristate flagValue, String source, FlagType type) {
        return consumer -> {
            // Toggle DEFAULT type
            if (type == FlagType.DEFAULT) {
                Tristate newValue = Tristate.UNDEFINED;
                if (flagValue == Tristate.TRUE) {
                    newValue = Tristate.FALSE;
                } else if (flagValue == Tristate.UNDEFINED) {
                    newValue = Tristate.TRUE;
                }
                CommandHelper.applyFlagPermission(src, GriefPreventionPlugin.GLOBAL_SUBJECT, "ALL", claim, flagPermission, source, "any", newValue, "default", type);
            // Toggle CLAIM type
            } else if (type == FlagType.CLAIM) {
                Tristate newValue = Tristate.UNDEFINED;
                if (flagValue == Tristate.TRUE) {
                    newValue = Tristate.FALSE;
                } else if (flagValue == Tristate.UNDEFINED) {
                    newValue = Tristate.TRUE;
                }
                CommandHelper.applyFlagPermission(src, GriefPreventionPlugin.GLOBAL_SUBJECT, "ALL", claim, flagPermission, source, "any", newValue, null, type);
            // Toggle OVERRIDE type
            } else if (type == FlagType.OVERRIDE) {
                Tristate newValue = Tristate.UNDEFINED;
                if (flagValue == Tristate.TRUE) {
                    newValue = Tristate.FALSE;
                } else if (flagValue == Tristate.UNDEFINED) {
                    newValue = Tristate.TRUE;
                }
                CommandHelper.applyFlagPermission(src, GriefPreventionPlugin.GLOBAL_SUBJECT, "ALL", claim, flagPermission, source, "any", newValue, "override", type);
            }
        };
    }

    public static Text getClickableText(CommandSource src, GPClaim claim, String flagPermission, Tristate flagValue, String source, FlagType type) {
        String onClickText = "Click here to toggle " + type.name().toLowerCase() + " value.";
        boolean hasPermission = true;
        if (type == FlagType.DEFAULT && !src.hasPermission(GPPermissions.MANAGE_FLAG_DEFAULTS)) {
            onClickText = "You do not have permission to change flag defaults.";
            hasPermission = false;
        }
        if (type == FlagType.OVERRIDE && !src.hasPermission(GPPermissions.MANAGE_FLAG_OVERRIDES)) {
            onClickText = "This flag has been forced by an admin and cannot be changed.";
            hasPermission = false;
        } else if (src instanceof Player) {
            String denyReason = claim.allowEdit((Player) src);
            if (denyReason != null) {
                onClickText = denyReason;
                hasPermission = false;
            }
        }

        Text.Builder textBuilder = Text.builder()
        .append(Text.of(flagValue.toString().toLowerCase()))
        .onHover(TextActions.showText(Text.of(onClickText, "\n", CommandHelper.getFlagTypeHoverText(type))));
        if (hasPermission) {
            textBuilder.onClick(TextActions.executeCallback(createFlagConsumer(src, claim, flagPermission, flagValue, source, type)));
        }
        return textBuilder.build();
    }
}
