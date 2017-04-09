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
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.ClaimResult;
import me.ryanhamshire.griefprevention.claim.ClaimsMode;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.logging.CustomLogEntryTypes;
import me.ryanhamshire.griefprevention.message.Messages;
import me.ryanhamshire.griefprevention.message.TextMode;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class CommandClaimDelete implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPreventionPlugin.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }
        // determine which claim the player is standing in
        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAt(player.getLocation(), true);

        if (claim.isWildernessClaim()) {
            GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.DeleteClaimMissing);
        } else {
            if (claim.isAdminClaim() && !player.hasPermission(GPPermissions.DELETE_CLAIM_ADMIN)) {
                GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.CantDeleteAdminClaim);
                return CommandResult.success();
            }
            if (claim.isBasicClaim() && !player.hasPermission(GPPermissions.DELETE_CLAIM_BASIC)) {
                GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.CantDeleteBasicClaim);
                return CommandResult.success();
            }

            if (claim.children.size() > 0 && !playerData.warnedAboutMajorDeletion) {
                GriefPreventionPlugin.sendMessage(player, TextMode.Warn, Messages.DeletionSubdivisionWarning);
                playerData.warnedAboutMajorDeletion = true;
            } else {

                ClaimResult claimResult = GriefPreventionPlugin.instance.dataStore.deleteClaim(claim, Cause.of(NamedCause.source(src)));
                if (!claimResult.successful()) {
                    player.sendMessage(Text.of(TextColors.RED, claimResult.getMessage().orElse(Text.of("Could not delete claim. A plugin has denied it."))));
                    return CommandResult.success();
                }

                claim.removeSurfaceFluids(null);
                // clear permissions
                GriefPreventionPlugin.GLOBAL_SUBJECT.getSubjectData().clearPermissions(ImmutableSet.of(claim.getContext()));
                // if in a creative mode world, /restorenature the claim
                if (GriefPreventionPlugin.instance
                        .claimModeIsActive(claim.getLesserBoundaryCorner().getExtent().getProperties(), ClaimsMode.Creative)) {
                    GriefPreventionPlugin.instance.restoreClaim(claim, 0);
                }

                GriefPreventionPlugin.sendMessage(player, TextMode.Success, Messages.DeleteSuccess);
                GriefPreventionPlugin.addLogEntry(
                        player.getName() + " deleted " + claim.getOwnerName() + "'s claim at "
                                + GriefPreventionPlugin.getfriendlyLocationString(claim.getLesserBoundaryCorner()),
                        CustomLogEntryTypes.AdminActivity);

                // revert any current visualization
                playerData.revertActiveVisual(player);

                playerData.warnedAboutMajorDeletion = false;
            }
        }

        return CommandResult.success();
    }
}
