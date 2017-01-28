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
import me.ryanhamshire.griefprevention.api.claim.Claim;
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

import java.util.List;
import java.util.UUID;

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
        if (user == null) {
            String group = ctx.<String>getOne("group").orElse(null);
            if (group.equalsIgnoreCase("public") || group.equalsIgnoreCase("all")) {
                user = GriefPreventionPlugin.PUBLIC_USER;
            }
        }

        // validate player argument
        if (user == null) {
            GriefPreventionPlugin.sendMessage(player, Text.of(TextMode.Err, "Not a valid player."));
            return CommandResult.success();
        }
        if (user.getUniqueId().equals(player.getUniqueId())) {
            GriefPreventionPlugin.sendMessage(player, Text.of(TextMode.Err, "You cannot not untrust yourself."));
            return CommandResult.success();
        }

        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        List<Claim> claimList = null;
        if (playerData != null) {
            claimList = playerData.getClaims();
        }

        if (playerData == null || claimList == null || claimList.size() == 0) {
            GriefPreventionPlugin.sendMessage(player, Text.of(TextMode.Err, "You have no claims to untrust."));
            return CommandResult.success();
        }

        GPTrustClaimEvent.Remove event = new GPTrustClaimEvent.Remove(claimList, Cause.of(NamedCause.source(player)), ImmutableList.of(user.getUniqueId()), TrustType.NONE);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            player.sendMessage(Text.of(TextColors.RED, event.getMessage().orElse(Text.of("Could not remove trust from user '" + user.getName() + "'. A plugin has denied it."))));
            return CommandResult.success();
        }

        for (Claim claim : claimList) {
            this.removeAllTrust(claim, user.getUniqueId());
        }

        GriefPreventionPlugin.sendMessage(player, TextMode.Success, Messages.UntrustIndividualAllClaims, user.getName());
        return CommandResult.success();
    }

    private void removeAllTrust(Claim claim, UUID playerUniqueId) {
        GPClaim gpClaim = (GPClaim) claim;
        for (TrustType type : TrustType.values()) {
            gpClaim.getTrustList(type).remove(playerUniqueId);
        }

        for (Claim subdivision : gpClaim.children) {
            this.removeAllTrust(subdivision, playerUniqueId);
        }
    }
}
