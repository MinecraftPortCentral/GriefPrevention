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
import me.ryanhamshire.griefprevention.api.claim.TrustType;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.event.GPGroupTrustClaimEvent;
import me.ryanhamshire.griefprevention.event.GPUserTrustClaimEvent;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CommandUntrust implements CommandExecutor {

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

        if (user == null && group == null) {
            GriefPreventionPlugin.sendMessage(src, GriefPreventionPlugin.instance.messageData.commandPlayerGroupInvalid.toText());
            return CommandResult.success();
        }
        if (user != null && user.getUniqueId().equals(player.getUniqueId())) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.untrustSelf.toText());
            return CommandResult.success();
        }

        // determine which claim the player is standing in
        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAtPlayer(playerData, player.getLocation());
        if (claim == null) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimNotFound.toText());
            return CommandResult.success();
        }

        // verify player has full perms
        UUID ownerID = claim.getOwnerUniqueId();
        if (ownerID == null && claim.isSubdivision()) {
            ownerID = claim.parent.getOwnerUniqueId();
        }
        if (user != null && user.getUniqueId().equals(ownerID)) {
            final Text message = GriefPreventionPlugin.instance.messageData.untrustOwner
                    .apply(ImmutableMap.of(
                    "owner", user.getName())).build();
            GriefPreventionPlugin.sendMessage(player, message);
            return CommandResult.success();
        }

        if (claim.allowGrantPermission(player) != null && !(playerData != null && playerData.canIgnoreClaim(claim))) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.permissionEditClaim.toText());
            return CommandResult.success();
        }

        if (user != null) {
            GPUserTrustClaimEvent.Remove event = new GPUserTrustClaimEvent.Remove(claim, Cause.of(NamedCause.source(player)), ImmutableList.of(user.getUniqueId()), TrustType.NONE);
            Sponge.getEventManager().post(event);
            if (event.isCancelled()) {
                player.sendMessage(Text.of(TextColors.RED, event.getMessage().orElse(Text.of("Could not remove trust from user '" + user.getName() + "'. A plugin has denied it."))));
                return CommandResult.success();
            }

            for (TrustType trustType : TrustType.values()) {
                claim.getUserTrustList(trustType).remove(user.getUniqueId());
            }
        } else {
            Set<Context> contexts = new HashSet<>();
            contexts.add(claim.getContext());
            if (!GriefPreventionPlugin.instance.permissionService.getGroupSubjects().hasRegistered(group)) {
                for (TrustType trustType : TrustType.values()) {
                    claim.getGroupTrustList(trustType).remove(group);
                    claim.getInternalClaimData().setRequiresSave(true);
                }

                GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.commandGroupInvalid.toText());
                return CommandResult.success();
            }

            final Subject subject = GriefPreventionPlugin.instance.permissionService.getGroupSubjects().get(group);
            GPGroupTrustClaimEvent.Remove event = new GPGroupTrustClaimEvent.Remove(claim, Cause.of(NamedCause.source(player)), ImmutableList.of(group), TrustType.NONE);
            Sponge.getEventManager().post(event);
            if (event.isCancelled()) {
                player.sendMessage(Text.of(TextColors.RED, event.getMessage().orElse(Text.of("Could not remove trust from group '" + group + "'. A plugin has denied it."))));
                return CommandResult.success();
            }

            for (TrustType trustType : TrustType.values()) {
                subject.getSubjectData().setPermission(contexts, GPPermissions.getTrustPermission(trustType), Tristate.UNDEFINED);
                claim.getGroupTrustList(trustType).remove(group);
            }
        }

        claim.getInternalClaimData().setRequiresSave(true);
        final Text message = GriefPreventionPlugin.instance.messageData.untrustIndividualSingleClaim
                .apply(ImmutableMap.of(
                "target", user != null ? user.getName() : group)).build();
        GriefPreventionPlugin.sendMessage(player, message);
        return CommandResult.success();
    }
}
