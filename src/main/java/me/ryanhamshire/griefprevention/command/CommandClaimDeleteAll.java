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
import me.ryanhamshire.griefprevention.event.GPDeleteClaimEvent;
import me.ryanhamshire.griefprevention.logging.CustomLogEntryTypes;
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

public class CommandClaimDeleteAll implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPreventionPlugin.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        // try to find that player
        final User otherPlayer = ctx.<User>getOne("player").get();
        // count claims
        final GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        int originalClaimCount = playerData.getInternalClaims().size();

        // check count
        if (originalClaimCount == 0) {
            src.sendMessage(Text.of(TextColors.RED, "Player " + otherPlayer.getName() + " has no claims to delete."));
            return CommandResult.success();
        }

        GPDeleteClaimEvent event = new GPDeleteClaimEvent(ImmutableList.copyOf(playerData.getInternalClaims()), Cause.of(NamedCause.source(src)));
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            player.sendMessage(Text.of(TextColors.RED, event.getMessage().orElse(Text.of("Could not delete all claims. A plugin has denied it."))));
            return CommandResult.success();
        }

        // delete all that player's claims
        GriefPreventionPlugin.instance.dataStore.deleteClaimsForPlayer(otherPlayer.getUniqueId());
        final Text message = GriefPreventionPlugin.instance.messageData.claimDeleteAllSuccess
                .apply(ImmutableMap.of(
                "owner", otherPlayer.getName())).build();
        GriefPreventionPlugin.sendMessage(player, message);
        if (player != null) {
            GriefPreventionPlugin.addLogEntry(player.getName() + " deleted all claims belonging to " + otherPlayer.getName() + ".",
                    CustomLogEntryTypes.AdminActivity);

            // revert any current visualization
            playerData.revertActiveVisual(player);
        }

        return CommandResult.success();
    }
}
