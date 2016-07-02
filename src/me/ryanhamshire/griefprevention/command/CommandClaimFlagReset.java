/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
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

import com.google.common.collect.ImmutableSet;
import me.ryanhamshire.griefprevention.GPPermissions;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.claim.Claim;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

public class CommandClaimFlagReset implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        Claim claim = GriefPrevention.instance.dataStore.getClaimAtPlayer(player, false);
        if (claim.isWildernessClaim()) {
            if (!src.hasPermission(GPPermissions.CLAIM_WILDERNESS_ADMIN)) {
                GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "You do not have permission to reset the Wilderness Claim to flag defaults."));
                return CommandResult.success();
            }
        } else if (claim.isAdminClaim()) {
            if (!player.getUniqueId().equals(claim.getOwnerUniqueId()) && !src.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS)) {
                GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "You do not have permission to reset an Admin Claim to flag defaults."));
                return CommandResult.success();
            }
        } else if ((claim.isBasicClaim() || claim.isSubdivision()) && !player.getUniqueId().equals(claim.getOwnerUniqueId())) {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "You do not have permission to reset your claim to flag defaults."));
            return CommandResult.success();
        }

        GriefPrevention.GLOBAL_SUBJECT.getSubjectData().clearPermissions(ImmutableSet.of(claim.getContext()));
        GriefPrevention.sendMessage(src, Text.of(TextMode.Success, "Claim flags reset to defaults successfully."));
        return CommandResult.success();
    }
}
