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

import com.google.common.collect.ImmutableSet;
import me.ryanhamshire.griefprevention.CustomLogEntryTypes;
import me.ryanhamshire.griefprevention.GPPermissions;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.Visualization;
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.claim.ClaimsMode;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;

public class CommandClaimDelete implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }
        // determine which claim the player is standing in
        Claim claim = GriefPrevention.instance.dataStore.getClaimAtPlayer(player, true);

        if (claim.isWildernessClaim()) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.DeleteClaimMissing);
        } else {
            // deleting an admin claim additionally requires the adminclaims permission
            if (!claim.isAdminClaim() || (player.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS) || player.hasPermission(GPPermissions.COMMAND_DELETE_ADMIN_CLAIMS))) {
                PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
                if (claim.children.size() > 0 && !playerData.warnedAboutMajorDeletion) {
                    GriefPrevention.sendMessage(player, TextMode.Warn, Messages.DeletionSubdivisionWarning);
                    playerData.warnedAboutMajorDeletion = true;
                } else {
                    claim.removeSurfaceFluids(null);
                    // clear permissions
                    GriefPrevention.GLOBAL_SUBJECT.getSubjectData().clearPermissions(ImmutableSet.of(claim.getContext()));
                    GriefPrevention.instance.dataStore.deleteClaim(claim, true);

                    // if in a creative mode world, /restorenature the claim
                    if (GriefPrevention.instance
                            .claimModeIsActive(claim.getLesserBoundaryCorner().getExtent().getProperties(), ClaimsMode.Creative)) {
                        GriefPrevention.instance.restoreClaim(claim, 0);
                    }

                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.DeleteSuccess);
                    GriefPrevention.addLogEntry(
                            player.getName() + " deleted " + claim.getOwnerName() + "'s claim at "
                                    + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()),
                            CustomLogEntryTypes.AdminActivity);

                    // revert any current visualization
                    Visualization.Revert(player);

                    playerData.warnedAboutMajorDeletion = false;
                }
            } else {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.CantDeleteAdminClaim);
            }
        }

        return CommandResult.success();
    }
}
