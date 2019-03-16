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
import me.ryanhamshire.griefprevention.permission.GPPermissions;
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
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CommandUntrustAll implements CommandExecutor {

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
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.untrustSelf.toText());
            return CommandResult.success();
        }

        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        Set<Claim> claimList = null;
        if (playerData != null) {
            claimList = playerData.getInternalClaims();
        }

        if (playerData == null || claimList == null || claimList.size() == 0) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.untrustNoClaims.toText());
            return CommandResult.success();
        }

        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            Sponge.getCauseStackManager().pushCause(player);
            if (user != null) {
                GPUserTrustClaimEvent.Remove
                    event = new GPUserTrustClaimEvent.Remove(new ArrayList<>(claimList), ImmutableList.of(user.getUniqueId()), TrustType.NONE);
                Sponge.getEventManager().post(event);
                if (event.isCancelled()) {
                    player.sendMessage(Text.of(TextColors.RED,
                        event.getMessage().orElse(Text.of("Could not remove trust from user '" + user.getName() + "'. A plugin has denied it."))));
                    return CommandResult.success();
                }

                for (Claim claim : claimList) {
                    this.removeAllUserTrust(claim, user);
                }
            } else {
                if (!PermissionUtils.hasGroupSubject(group)) {
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.commandGroupInvalid.toText());
                    return CommandResult.success();
                }

                final Subject subject = PermissionUtils.getGroupSubject(group);
                GPGroupTrustClaimEvent.Remove
                    event = new GPGroupTrustClaimEvent.Remove(new ArrayList<>(claimList), ImmutableList.of(group), TrustType.NONE);
                Sponge.getEventManager().post(event);
                if (event.isCancelled()) {
                    player.sendMessage(Text.of(TextColors.RED,
                        event.getMessage().orElse(Text.of("Could not remove trust from group '" + group + "'. A plugin has denied it."))));
                    return CommandResult.success();
                }

                for (Claim claim : claimList) {
                    this.removeAllGroupTrust(claim, subject);
                }
            }
        }

        final Text message = GriefPreventionPlugin.instance.messageData.untrustIndividualAllClaims
                .apply(ImmutableMap.of(
                "target", user.getName())).build();
        GriefPreventionPlugin.sendMessage(player, message);
        return CommandResult.success();
    }

    private void removeAllUserTrust(Claim claim, User user) {
        GPClaim gpClaim = (GPClaim) claim;
        Set<Context> contexts = new HashSet<>(); 
        contexts.add(gpClaim.getContext());
        for (TrustType type : TrustType.values()) {
            user.getSubjectData().setPermission(contexts, GPPermissions.getTrustPermission(type), Tristate.UNDEFINED);
            gpClaim.getUserTrustList(type).remove(user.getUniqueId());
            gpClaim.getInternalClaimData().setRequiresSave(true);
            for (Claim child : gpClaim.children) {
                this.removeAllUserTrust(child, user);
            }
        }
    }

    private void removeAllGroupTrust(Claim claim, Subject group) {
        GPClaim gpClaim = (GPClaim) claim;
        Set<Context> contexts = new HashSet<>(); 
        contexts.add(gpClaim.getContext());
        for (TrustType type : TrustType.values()) {
            group.getSubjectData().setPermission(contexts, GPPermissions.getTrustPermission(type), Tristate.UNDEFINED);
            gpClaim.getGroupTrustList(type).remove(group);
            gpClaim.getInternalClaimData().setRequiresSave(true);
            for (Claim child : gpClaim.children) {
                this.removeAllGroupTrust(child, group);
            }
        }
    }
}
