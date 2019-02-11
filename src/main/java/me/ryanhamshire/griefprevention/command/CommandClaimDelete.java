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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.ClaimResult;
import me.ryanhamshire.griefprevention.claim.ClaimsMode;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.logging.CustomLogEntryTypes;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class CommandClaimDelete implements CommandExecutor {

    private boolean deleteTopLevelClaim;

    public CommandClaimDelete(boolean deleteTopLevelClaim) {
        this.deleteTopLevelClaim = deleteTopLevelClaim;
    }

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
        final GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAt(player.getLocation());
        final boolean isTown = claim.isTown();

        if (claim.isWilderness()) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimNotFound.toText());
            return CommandResult.success();
        }

        final Text message = GriefPreventionPlugin.instance.messageData.permissionClaimDelete
                .apply(ImmutableMap.of(
                "type", claim.getType().name())).build();

        if (claim.isAdminClaim() && !player.hasPermission(GPPermissions.DELETE_CLAIM_ADMIN)) {
            GriefPreventionPlugin.sendMessage(player, message);
            return CommandResult.success();
        }
        if (claim.isBasicClaim() && !player.hasPermission(GPPermissions.DELETE_CLAIM_BASIC)) {
            GriefPreventionPlugin.sendMessage(player, message);
            return CommandResult.success();
        }

        if (!this.deleteTopLevelClaim && !claim.isTown() && claim.children.size() > 0 && !playerData.warnedAboutMajorDeletion) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimChildrenWarning.toText());
            playerData.warnedAboutMajorDeletion = true;
            return CommandResult.success();
        }

        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            Sponge.getCauseStackManager().pushCause(src);
            ClaimResult
                claimResult =
                GriefPreventionPlugin.instance.dataStore.deleteClaim(claim, !this.deleteTopLevelClaim);
            if (!claimResult.successful()) {
                player.sendMessage(
                    Text.of(TextColors.RED, claimResult.getMessage().orElse(Text.of("Could not delete claim. A plugin has denied it."))));
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

            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimDeleted.toText());
            GriefPreventionPlugin.addLogEntry(
                player.getName() + " deleted " + claim.getOwnerName() + "'s claim at "
                + GriefPreventionPlugin.getfriendlyLocationString(claim.getLesserBoundaryCorner()),
                CustomLogEntryTypes.AdminActivity);

            // revert any current visualization
            playerData.revertActiveVisual(player);

            playerData.warnedAboutMajorDeletion = false;
            if (isTown) {
                playerData.inTown = false;
                playerData.townChat = false;
            }
        }

        return CommandResult.success();
    }
}
