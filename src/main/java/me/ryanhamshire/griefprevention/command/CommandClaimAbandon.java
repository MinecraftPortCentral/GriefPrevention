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
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.TrustType;
import me.ryanhamshire.griefprevention.claim.ClaimsMode;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.claim.GPClaimManager;
import me.ryanhamshire.griefprevention.event.GPDeleteClaimEvent;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class CommandClaimAbandon implements CommandExecutor {

    private boolean abandonTopClaim;

    public CommandClaimAbandon(boolean abandonTopClaim) {
        this.abandonTopClaim = abandonTopClaim;
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

        // which claim is being abandoned?
        final GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        final GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAt(player.getLocation());
        final UUID ownerId = claim.getOwnerUniqueId();

        final boolean isAdmin = playerData.canIgnoreClaim(claim);
        final boolean isTown = claim.isTown();
        if (claim.isWilderness()) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.commandAbandonClaimMissing.toText());
            return CommandResult.success();
        } else if (!isAdmin && !player.getUniqueId().equals(ownerId) && claim.isUserTrusted(player, TrustType.MANAGER)) {
            if (claim.parent == null) {
                // Managers can only abandon child claims
                GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimNotYours.toText());
                return CommandResult.success();
            }
        } else if (!isAdmin && (claim.allowEdit(player) != null || (!claim.isAdminClaim() && !player.getUniqueId().equals(ownerId)))) {
            // verify ownership
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimNotYours.toText());
            return CommandResult.success();
        }

        if (!claim.isTown() && !claim.isAdminClaim() && claim.children.size() > 0 && !this.abandonTopClaim) {
            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.commandAbandonTopLevel.toText());
            return CommandResult.empty();
        } else {
            if (this.abandonTopClaim && (claim.isTown() || claim.isAdminClaim()) && claim.children.size() > 0) {
                Set<Claim> invalidClaims = new HashSet<>();
                for (Claim child : claim.getChildren(true)) {
                    if (child.getOwnerUniqueId() == null || !child.getOwnerUniqueId().equals(ownerId)) {
                        //return CommandResult.empty();
                        invalidClaims.add(child);
                    }
                }

                if (!invalidClaims.isEmpty()) {
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.commandAbandonTownChildren.toText());
                    CommandHelper.showClaims(player, invalidClaims, 0, true);
                    return CommandResult.success();
                }
            }

            try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                Sponge.getCauseStackManager().pushCause(src);
                GPDeleteClaimEvent.Abandon event = new GPDeleteClaimEvent.Abandon(claim);
                Sponge.getEventManager().post(event);
                if (event.isCancelled()) {
                    player
                        .sendMessage(Text.of(TextColors.RED, event.getMessage().orElse(Text.of("Could not abandon claim. A plugin has denied it."))));
                    return CommandResult.success();
                }
            }

            // delete it
            GPClaimManager claimManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(player.getWorld().getProperties());
            claimManager.deleteClaimInternal(claim, this.abandonTopClaim);
            claim.removeSurfaceFluids(null);
            // remove all context permissions
            player.getSubjectData().clearPermissions(ImmutableSet.of(claim.getContext()));
            GriefPreventionPlugin.GLOBAL_SUBJECT.getSubjectData().clearPermissions(ImmutableSet.of(claim.getContext()));

            // if in a creative mode world, restore the claim area
            if (GriefPreventionPlugin.instance.claimModeIsActive(claim.getLesserBoundaryCorner().getExtent().getProperties(), ClaimsMode.Creative)) {
                GriefPreventionPlugin.addLogEntry(
                        player.getName() + " abandoned a " + claim.getType() + " @ " + GriefPreventionPlugin.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
                GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimCleanupWarning.toText());
                GriefPreventionPlugin.instance.restoreClaim(claim, 20L * 60 * 2);
            }

            // this prevents blocks being gained without spending adjust claim blocks when abandoning a top level claim
            if (!claim.isSubdivision() && !claim.isAdminClaim()) {
                int newAccruedClaimCount = playerData.getAccruedClaimBlocks() - ((int) Math.ceil(claim.getClaimBlocks() * (1 - playerData.optionAbandonReturnRatioBasic)));
                playerData.setAccruedClaimBlocks(newAccruedClaimCount);
            }

            // tell the player how many claim blocks he has left
            int remainingBlocks = playerData.getRemainingClaimBlocks();
            final Text message = GriefPreventionPlugin.instance.messageData.claimAbandonSuccess
                    .apply(ImmutableMap.of(
                    "remaining-blocks", Text.of(remainingBlocks)
            )).build();
            GriefPreventionPlugin.sendMessage(player, message);
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
