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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.TrustType;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.event.GPGroupTrustClaimEvent;
import me.ryanhamshire.griefprevention.event.GPUserTrustClaimEvent;
import me.ryanhamshire.griefprevention.util.PermissionUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CommandTrustAll implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPreventionPlugin.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(Text.of("An error occurred while executing this command."));
            return CommandResult.success();
        }

        User user = ctx.<User>getOne("user").orElse(null);
        String group = null;
        if (user == null) {
            group = ctx.<String>getOne("group").orElse(null);
            if (group.equalsIgnoreCase("public") || group.equalsIgnoreCase("all")) {
                user = GriefPreventionPlugin.PUBLIC_USER;
                group = null;
            }
        }

        // validate player argument
        if (user == null && group == null) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.commandPlayerGroupInvalid.toText());
            return CommandResult.success();
        }

        if (user != null && user.getUniqueId().equals(player.getUniqueId())) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.trustSelf.toText());
            return CommandResult.success();
        }

        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        Set<Claim> claimList = null;
        if (playerData != null) {
            claimList = playerData.getInternalClaims();
        }

        if (playerData == null || claimList == null || claimList.size() == 0) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.trustNoClaims.toText());
            return CommandResult.success();
        }

        if (user != null) {
            try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                Sponge.getCauseStackManager().pushCause(player);
                GPUserTrustClaimEvent.Add
                    event = new GPUserTrustClaimEvent.Add(new ArrayList<>(claimList), ImmutableList.of(user.getUniqueId()), TrustType.NONE);
                Sponge.getEventManager().post(event);
                if (event.isCancelled()) {
                    player.sendMessage(Text.of(TextColors.RED,
                        event.getMessage().orElse(Text.of("Could not add trust for user '" + user.getName() + "'. A plugin has denied it."))));
                    return CommandResult.success();
                }

                for (Claim claim : claimList) {
                    this.addAllUserTrust(claim, user);
                }
            }
        } else {
            if (!PermissionUtils.hasGroupSubject(group)) {
                GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.commandGroupInvalid.toText());
                return CommandResult.success();
            }

            try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                final Subject subject = PermissionUtils.getGroupSubject(group);
                Sponge.getCauseStackManager().pushCause(player);
                GPGroupTrustClaimEvent.Add
                    event = new GPGroupTrustClaimEvent.Add(new ArrayList<>(claimList), ImmutableList.of(group), TrustType.NONE);
                Sponge.getEventManager().post(event);
                if (event.isCancelled()) {
                    player.sendMessage(Text.of(TextColors.RED,
                        event.getMessage().orElse(Text.of("Could not add trust for group '" + group + "'. A plugin has denied it."))));
                    return CommandResult.success();
                }

                for (Claim claim : claimList) {
                    this.addAllGroupTrust(claim, subject);
                }
            }
        }

        final Text message = GriefPreventionPlugin.instance.messageData.trustIndividualAllClaims
                .apply(ImmutableMap.of(
                "player", user.getName())).build();
        GriefPreventionPlugin.sendMessage(player, message);
        return CommandResult.success();
    }

    private void addAllUserTrust(Claim claim, User user) {
        GPClaim gpClaim = (GPClaim) claim;
        List<UUID> trustList = gpClaim.getUserTrustList(TrustType.BUILDER);
        if (!trustList.contains(user.getUniqueId())) {
            trustList.add(user.getUniqueId());
        }

        gpClaim.getInternalClaimData().setRequiresSave(true);
        for (Claim child : gpClaim.children) {
            this.addAllGroupTrust(child, user);
        }
    }

    private void addAllGroupTrust(Claim claim, Subject group) {
        GPClaim gpClaim = (GPClaim) claim;
        List<String> trustList = gpClaim.getGroupTrustList(TrustType.BUILDER);
        if (!trustList.contains(group.getIdentifier())) {
            trustList.add(group.getIdentifier());
        }

        gpClaim.getInternalClaimData().setRequiresSave(true);
        for (Claim child : gpClaim.children) {
            this.addAllGroupTrust(child, group);
        }
    }
}
