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
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimFlag;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.command.CommandClaimFlag.FlagType;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import me.ryanhamshire.griefprevention.util.ClaimClickData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
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

public class CommandClaimFlagPlayer implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPreventionPlugin.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        User user = ctx.<User>getOne("player").get();
        String name = user.getName();
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

        Tristate value = ctx.<Tristate>getOne("value").orElse(null);
        String context = ctx.<String>getOne("context").orElse(null);
        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAtPlayer(playerData, player.getLocation(), false);
        //Optional<User> targetUser = PlayerUtils.resolvePlayerByName(name);
        /*if (!user.isPresent()) {
            final Text message = GriefPreventionPlugin.instance.messageData.commandPlayerInvalid
                    .apply(ImmutableMap.of(
                    "player", name)).build();
            GriefPreventionPlugin.sendMessage(player, message);
            return CommandResult.empty();
        } else*/

        String reason = ctx.<String>getOne("reason").orElse(null);
        Text reasonText = null;
        if (reason != null) {
            reasonText = TextSerializers.FORMATTING_CODE.deserialize(reason);
        }
        if (flag == null && value == null) {
            Set<Context> contexts = new HashSet<>();
            contexts.add(claim.getContext());

            Subject subj = user;
            Map<String, Text> flagList = new TreeMap<>();
            Map<String, Boolean> permissions = new HashMap<>(subj.getSubjectData().getPermissions(contexts));
            Map<String, ClaimClickData> inheritPermissions = new HashMap<>();
            final List<Claim> inheritParents = claim.getInheritedParents();
            Collections.reverse(inheritParents);
            for (Claim current : inheritParents) {
                GPClaim currentClaim = (GPClaim) current;
                Map<String, Boolean> currentPermissions = new HashMap<>(subj.getSubjectData().getPermissions(ImmutableSet.of(currentClaim.context)));
                for (Map.Entry<String, Boolean> permissionEntry : currentPermissions.entrySet()) {
                    permissions.put(permissionEntry.getKey(), permissionEntry.getValue());
                    inheritPermissions.put(permissionEntry.getKey(), new ClaimClickData(currentClaim, permissionEntry.getValue()));
                }
            }

            for (Map.Entry<String, Boolean> permissionEntry : permissions.entrySet()) {
                String flagPermission = permissionEntry.getKey();
                String baseFlagPerm = flagPermission.replace(GPPermissions.FLAG_BASE + ".",  "");
                Text baseFlagText = Text.builder().append(Text.of(TextColors.GREEN, baseFlagPerm))
                        .onHover(TextActions.showText(CommandHelper.getBaseFlagOverlayText(baseFlagPerm))).build();
                ClaimClickData claimClickData = inheritPermissions.get(flagPermission);
                Text valueText = null;
                if (claimClickData != null) {
                    valueText = Text.of(TextColors.AQUA, CommandHelper.getClickableText(src, subj, name, contexts, claimClickData.claim, flagPermission, Tristate.fromBoolean(claimClickData.value), source, FlagType.INHERIT));
                } else {
                    valueText = Text.of(TextColors.GOLD, CommandHelper.getClickableText(src, subj, name, contexts, claim, flagPermission, Tristate.fromBoolean(permissionEntry.getValue()), source, FlagType.PLAYER));
                }

                Text flagText = Text.of(
                        TextColors.GREEN, baseFlagText, "  ",
                        TextColors.WHITE, "[",
                        valueText,
                        TextColors.WHITE, "]");
                flagList.put(flagPermission, flagText);
            }

            List<Text> textList = new ArrayList<>(flagList.values());
            PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
            PaginationList.Builder paginationBuilder = paginationService.builder()
                    .title(Text.of(TextColors.GOLD, name, TextColors.AQUA, " Flag Permissions")).padding(Text.of(TextStyles.STRIKETHROUGH, "-")).contents(textList);
            paginationBuilder.sendTo(src);
            return CommandResult.success();
        }

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

        if (user.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS) && !src.hasPermission(GPPermissions.SET_ADMIN_FLAGS)) {
            GriefPreventionPlugin.sendMessage(src, GriefPreventionPlugin.instance.messageData.permissionSetAdminFlags.toText());
            return CommandResult.success();
        }

        claim.setPermission(user, name, ClaimFlag.getEnum(flag), source, target, value, claimContext, reasonText, Cause.source(src).build());
        return CommandResult.success();
    }
}
