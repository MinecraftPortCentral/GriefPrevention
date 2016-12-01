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

import static org.spongepowered.api.command.CommandMessageFormatting.SPACE_TEXT;

import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.claim.Claim;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Text.Builder;
import org.spongepowered.api.text.format.TextColors;

import java.util.UUID;

public class CommandTrustList implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        PlayerData playerData = GriefPrevention.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        Claim claim = GriefPrevention.instance.dataStore.getClaimAtPlayer(playerData, player.getLocation(), true);

        // if no claim here, error message
        if (claim == null) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.TrustListNoClaim));
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        GriefPrevention.sendMessage(player, TextMode.Info, Messages.TrustListHeader);
        Builder permissions = Text.builder(">").color(TextColors.GOLD);

        for (UUID uuid : claim.getClaimData().getManagers()) {
            User user = GriefPrevention.getOrCreateUser(uuid);
            permissions.append(SPACE_TEXT, Text.of(user.getName()));
        }

        player.sendMessage(permissions.build());
        permissions = Text.builder(">").color(TextColors.YELLOW);

        for (UUID uuid : claim.getClaimData().getBuilders()) {
            User user = GriefPrevention.getOrCreateUser(uuid);
            permissions.append(SPACE_TEXT, Text.of(user.getName()));
        }

        player.sendMessage(permissions.build());
        permissions = Text.builder(">").color(TextColors.GREEN);

        for (UUID uuid : claim.getClaimData().getContainers()) {
            User user = GriefPrevention.getOrCreateUser(uuid);
            permissions.append(SPACE_TEXT, Text.of(user.getName()));
        }

        player.sendMessage(permissions.build());
        permissions = Text.builder(">").color(TextColors.BLUE);

        for (UUID uuid : claim.getClaimData().getAccessors()) {
            User user = GriefPrevention.getOrCreateUser(uuid);
            permissions.append(SPACE_TEXT, Text.of(user.getName()));
        }

        player.sendMessage(permissions.build());
        player.sendMessage(Text.of(
                Text.of(TextColors.BLUE, GriefPrevention.instance.dataStore.getMessage(Messages.Access)), SPACE_TEXT,
                Text.of(TextColors.YELLOW, GriefPrevention.instance.dataStore.getMessage(Messages.Build)), SPACE_TEXT,
                Text.of(TextColors.GREEN, GriefPrevention.instance.dataStore.getMessage(Messages.Containers)), SPACE_TEXT,
                Text.of(TextColors.GOLD, GriefPrevention.instance.dataStore.getMessage(Messages.Manage))));
        return CommandResult.success();

    }
}
