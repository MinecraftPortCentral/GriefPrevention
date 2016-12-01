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

import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.util.PlayerUtils;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.Optional;

public class CommandUntrustAll implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(Text.of("An error occurred while executing this command."));
            return CommandResult.success();
        }

        String subject = ctx.<String>getOne("subject").get();
        Optional<User> targetPlayer = Optional.empty();
        if (subject.equalsIgnoreCase("public") || subject.equalsIgnoreCase("all")) {
            targetPlayer = Optional.of(GriefPrevention.PUBLIC_USER);
        } else {
            targetPlayer = PlayerUtils.resolvePlayerByName(subject);
        }

        // validate player argument
        if (!targetPlayer.isPresent()) {
            GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "Not a valid player."));
            return CommandResult.success();
        } else if (targetPlayer.isPresent() && targetPlayer.get().getUniqueId().equals(player.getUniqueId())) {
            GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "You cannot not untrust yourself."));
            return CommandResult.success();
        }

        PlayerData playerData = GriefPrevention.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        List<Claim> claimList = null;
        if (playerData != null) {
            claimList = playerData.getClaims();
        }

        if (playerData == null || claimList == null || claimList.size() == 0) {
            GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "You have no claims to untrust."));
            return CommandResult.success();
        }

        for (Claim claim : claimList) {
            claim.getClaimData().getAccessors().remove(targetPlayer.get().getUniqueId());
            claim.getClaimData().getBuilders().remove(targetPlayer.get().getUniqueId());
            claim.getClaimData().getContainers().remove(targetPlayer.get().getUniqueId());
            claim.getClaimData().getManagers().remove(targetPlayer.get().getUniqueId());
            claim.getClaimData().setRequiresSave(true);
        }

        GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustIndividualAllClaims, targetPlayer.get().getName());
        return CommandResult.success();
    }
}
