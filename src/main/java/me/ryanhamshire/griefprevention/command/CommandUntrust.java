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
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.ClaimResult;
import me.ryanhamshire.griefprevention.api.claim.TrustType;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.event.GPTrustClaimEvent;
import me.ryanhamshire.griefprevention.message.Messages;
import me.ryanhamshire.griefprevention.message.TextMode;
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
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

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
        if (user == null) {
            String group = ctx.<String>getOne("group").orElse(null);
            if (group.equalsIgnoreCase("public") || group.equalsIgnoreCase("all")) {
                user = GriefPreventionPlugin.PUBLIC_USER;
            }
        }

        if (user == null) {
            GriefPreventionPlugin.sendMessage(src, Text.of(TextMode.Err, "Not a valid player."));
            return CommandResult.success();
        }
        if (user.getUniqueId().equals(player.getUniqueId())) {
            GriefPreventionPlugin.sendMessage(player, Text.of(TextMode.Err, "You cannot not untrust yourself."));
            return CommandResult.success();
        }

        // determine which claim the player is standing in
        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAtPlayer(playerData, player.getLocation(), true);
        if (claim == null) {
            GriefPreventionPlugin.sendMessage(player, Text.of(TextMode.Err, "No claim found at location. If you want to untrust all claims, use /untrustall instead."));
            return CommandResult.success();
        }

        // verify player has full perms
        UUID ownerID = claim.getOwnerUniqueId();
        if (ownerID == null && claim.parent != null) {
            ownerID = claim.parent.getOwnerUniqueId();
        }
        if (user.getUniqueId().equals(ownerID)) {
            GriefPreventionPlugin.sendMessage(player, Text.of(TextMode.Err, user.getName() + " is owner of claim and cannot be untrusted."));
            return CommandResult.success();
        }

        if (claim.allowGrantPermission(player) != null && !(playerData != null && playerData.canIgnoreClaim(claim))) {
            GriefPreventionPlugin.sendMessage(player, Text.of(TextMode.Err, "You do not have permission to edit this claim."));
            return CommandResult.success();
        }

        GPTrustClaimEvent.Remove event = new GPTrustClaimEvent.Remove(claim, Cause.of(NamedCause.source(player)), ImmutableList.of(user.getUniqueId()), TrustType.NONE);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            player.sendMessage(Text.of(TextColors.RED, event.getMessage().orElse(Text.of("Could not remove trust from user '" + user.getName() + "'. A plugin has denied it."))));
            return CommandResult.success();
        }

        for (TrustType trustType : TrustType.values()) {
            claim.getTrustList(trustType).remove(user.getUniqueId());
        }

        claim.getInternalClaimData().setRequiresSave(true);
        GriefPreventionPlugin.sendMessage(player, TextMode.Success, Messages.UntrustIndividualSingleClaim, user.getName());
        return CommandResult.success();
    }
}
