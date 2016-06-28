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

import me.ryanhamshire.griefprevention.GPPermissions;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.claim.Claim;
import org.spongepowered.api.command.CommandPermissionException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.Optional;

public class CommandClaimList implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {

        // player whose claims will be listed if any
        Optional<User> otherPlayer = ctx.<User>getOne("player");

        // otherwise if no permission to delve into another player's claims data or self
        if ((otherPlayer.isPresent() && otherPlayer.get() != src && !src.hasPermission(GPPermissions.CLAIM_LIST_OTHERS)) ||
                (!otherPlayer.isPresent() && !src.hasPermission(GPPermissions.COMMAND_LIST_CLAIMS))) {
            try {
                throw new CommandPermissionException();
            } catch (CommandPermissionException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        Player player = (Player) src;
        User targetUser = otherPlayer.isPresent() ? otherPlayer.get() : (User) src;
        // load the target player's data
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), targetUser.getUniqueId());
        List<Claim> claimList = playerData.getClaims();
        GriefPrevention.sendMessage(src, TextMode.Instr, Messages.StartBlockMath,
                String.valueOf(playerData.getAccruedClaimBlocks()),
                String.valueOf((playerData.getBonusClaimBlocks() + GriefPrevention.instance.dataStore
                        .getGroupBonusBlocks(targetUser.getUniqueId()))),
                String.valueOf((playerData.getAccruedClaimBlocks() + playerData.getBonusClaimBlocks()
                        + GriefPrevention.instance.dataStore.getGroupBonusBlocks(targetUser.getUniqueId()))));
        if (claimList.size() > 0) {
            GriefPrevention.sendMessage(src, TextMode.Instr, Messages.ClaimsListHeader);
            for (Claim claim : claimList) {
                GriefPrevention.sendMessage(src, Text.of(TextMode.Instr, GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner())
                        + GriefPrevention.instance.dataStore.getMessage(Messages.ContinueBlockMath, String.valueOf(claim.getArea()))));
            }

            GriefPrevention
                    .sendMessage(src, TextMode.Instr, Messages.EndBlockMath, String.valueOf(playerData.getRemainingClaimBlocks()));
        }

        // drop the data we just loaded, if the player isn't online
        if (!targetUser.isOnline()) {
            GriefPrevention.instance.dataStore.clearCachedPlayerData(player.getWorld().getProperties(), targetUser.getUniqueId());
        }

        return CommandResult.success();
    }
}
