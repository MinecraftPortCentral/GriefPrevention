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

import me.ryanhamshire.griefprevention.CustomLogEntryTypes;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.TextMode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;

import java.util.UUID;

public class CommandAdjustBonusClaimBlocks implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e1) {
            src.sendMessage(e1.getText());
            return CommandResult.success();
        }

        // parse the adjustment amount
        int adjustment = args.<Integer>getOne("amount").get();
        String target = args.<String>getOne("player").get();

        // if granting blocks to all players with a specific permission
        if (target.startsWith("[") && target.endsWith("]")) {
            String permissionIdentifier = target.substring(1, target.length() - 1);
            int newTotal = GriefPrevention.instance.dataStore.adjustGroupBonusBlocks(permissionIdentifier, adjustment);

            GriefPrevention.sendMessage(player, TextMode.Success, Messages.AdjustGroupBlocksSuccess, permissionIdentifier,
                    String.valueOf(adjustment), String.valueOf(newTotal));
            if (player != null) {
                GriefPrevention
                        .addLogEntry(
                                player.getName() + " adjusted " + permissionIdentifier + "'s bonus claim blocks by " + adjustment + ".");
            }

            return CommandResult.success();
        }

        // otherwise, find the specified player
        User targetPlayer;
        try {
            UUID playerID = UUID.fromString(target);
            targetPlayer = Sponge.getGame().getServiceManager().provideUnchecked(UserStorageService.class).get(playerID).orElse(null);

        } catch (IllegalArgumentException e) {
            targetPlayer = GriefPrevention.instance.resolvePlayerByName(target).orElse(null);
        }

        if (targetPlayer == null) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.PlayerNotFound2));
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        // give blocks to player
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), targetPlayer.getUniqueId());
        playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + adjustment);
        playerData.getStorageData().save();

        GriefPrevention
                .sendMessage(player, TextMode.Success, Messages.AdjustBlocksSuccess, targetPlayer.getName(), String.valueOf(adjustment),
                        String.valueOf(playerData.getBonusClaimBlocks()));
        if (player != null) {
            GriefPrevention.addLogEntry(
                    player.getName() + " adjusted " + targetPlayer.getName() + "'s bonus claim blocks by " + adjustment + ".",
                    CustomLogEntryTypes.AdminActivity);
        }


        return CommandResult.success();
    }
}
