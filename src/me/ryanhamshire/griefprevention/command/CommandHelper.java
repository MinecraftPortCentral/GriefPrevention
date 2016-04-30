/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) Ryan Hamshire
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
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.Visualization;
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.claim.ClaimPermission;
import me.ryanhamshire.griefprevention.claim.ClaimsMode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

public class CommandHelper {

    public static Player checkPlayer(CommandSource source) throws CommandException {
        if (source instanceof Player) {
            return ((Player) source);
        } else {
            throw new CommandException(Text.of("You must be a player to run this command!"));
        }
    }

    public static CommandResult abandonClaimHandler(Player player, boolean deleteTopLevelClaim) {
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());

        // which claim is being abandoned?
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);
        if (claim == null) {
            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.AbandonClaimMissing);
        }

        // verify ownership
        else if (claim.allowEdit(player) != null) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotYourClaim);
        }

        // warn if has children and we're not explicitly deleting a top level claim
        else if (claim.children.size() > 0 && !deleteTopLevelClaim) {
            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.DeleteTopLevelClaim);
            return CommandResult.empty();
        } else {
            // delete it
            claim.removeSurfaceFluids(null);
            GriefPrevention.instance.dataStore.deleteClaim(claim, true);

            // if in a creative mode world, restore the claim area
            if (GriefPrevention.instance.claimModeIsActive(claim.getLesserBoundaryCorner().getExtent().getProperties(), ClaimsMode.Creative)) {
                GriefPrevention.addLogEntry(
                        player.getName() + " abandoned a claim @ " + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
                GriefPrevention.sendMessage(player, TextMode.Warn, Messages.UnclaimCleanupWarning);
                GriefPrevention.instance.restoreClaim(claim, 20L * 60 * 2);
            }

            // adjust claim blocks when abandoning a top level claim
            if (claim.parent == null) {
                playerData.setAccruedClaimBlocks(
                        playerData.getAccruedClaimBlocks() - (int) Math
                                .ceil((claim.getArea() * (1 - GriefPrevention.getActiveConfig(player.getWorld().getProperties())
                                        .getConfig().claim.abandonReturnRatio))));
            }

            // tell the player how many claim blocks he has left
            int remainingBlocks = playerData.getRemainingClaimBlocks();
            GriefPrevention.sendMessage(player, TextMode.Success, Messages.AbandonSuccess, String.valueOf(remainingBlocks));

            // revert any current visualization
            Visualization.Revert(player);

            playerData.warnedAboutMajorDeletion = false;
        }

        return CommandResult.success();

    }

    // helper method to resolve a player name from the player's UUID
    public static String lookupPlayerName(UUID playerID) {
        // parameter validation
        if (playerID == null) {
            return "somebody";
        }

        // check the cache
        Optional<User> player = Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(playerID);
        if (player.isPresent() || player.get().isOnline()) {
            return player.get().getName();
        } else {
            return "someone";
        }
    }

    public static CommandResult handleFlagPermission(Player player, CommandContext ctx, String permission) {
        String target = ctx.<String>getOne("target").get();
        String name = ctx.<String>getOne("name").get();
        String flag = ctx.<String>getOne("flag").get();
        String value = ctx.<String>getOne("value").get();

        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, null);
        Optional<User> targetPlayer = GriefPrevention.instance.resolvePlayerByName(name);
        if (!targetPlayer.isPresent() && target.equalsIgnoreCase("player")) {
            GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "Not a valid player."));
            return CommandResult.success();
        }

        return addPermission(claim, player, targetPlayer, name, flag, permission, value);
    }

    public static CommandResult addPermission(Claim claim, Player sourcePlayer, Optional<User> targetPlayer, String group, String flag, String permission, String value) {
        if (claim == null) {
            GriefPrevention.sendMessage(sourcePlayer, Text.of(TextMode.Err, "No claim found."));
            return CommandResult.success();
        }

        if (claim.getClaimData().getFlags().getFlagValue(flag) != null) {
            if (targetPlayer.isPresent()) {
                Subject subj = targetPlayer.get().getContainingCollection().get(targetPlayer.get().getIdentifier());
                subj.getSubjectData().setPermission(ImmutableSet.of(claim.getContext()), permission + flag,
                        Tristate.fromBoolean(Boolean.valueOf(value)));
                sourcePlayer.sendMessage(Text.of(TextColors.GREEN, "Set permission of ", flag, " to ", value, " for ", targetPlayer.get().getName(), "."));
            } else if (group == null) {
                GriefPrevention.sendMessage(sourcePlayer, Text.of(TextMode.Err, "Not a valid player."));
            } else { // group
                PermissionService service = Sponge.getServiceManager().provide(PermissionService.class).get();
                Subject subj = service.getGroupSubjects().get(group);
                if (subj != null) {
                    subj.getSubjectData().setPermission(ImmutableSet.of(claim.getContext()), permission + flag,
                            Tristate.fromBoolean(Boolean.valueOf(value)));
                    sourcePlayer.sendMessage(Text.of(TextColors.GREEN, "Set permission of ", flag, " to ", value, " for group ", group, "."));
                } else {
                    GriefPrevention.sendMessage(sourcePlayer, Text.of(TextMode.Err, "Not a valid group."));
                }
            }
        } else {
            GriefPrevention.sendMessage(sourcePlayer, Text.of(TextMode.Err, "Not a valid flag."));
        }

        return CommandResult.success();
    }

    public static void handleTrustCommand(Player player, ClaimPermission claimPermission, String target) {

        Optional<User> targetPlayer = Optional.empty();
        if (!target.equalsIgnoreCase("all") && !target.equalsIgnoreCase("public")) {
            targetPlayer = GriefPrevention.instance.resolvePlayerByName(target);
        } else {
            targetPlayer = Optional.of(GriefPrevention.PUBLIC_USER);
        }

        if (!targetPlayer.isPresent()) {
            GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "Not a valid player."));
            return;
        }

        // determine which claim the player is standing in
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);
        ArrayList<Claim> targetClaims = new ArrayList<>();
        if (claim == null) {
            GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "No claim found at location. If you want to trust all claims, use /trustall instead."));
            return;
        } else {
            // verify claim belongs to player
            UUID ownerID = claim.ownerID;
            if (ownerID == null && claim.parent != null) {
                ownerID = claim.parent.ownerID;
            }
            if (targetPlayer.get().getUniqueId().equals(claim.ownerID)) {
                GriefPrevention.sendMessage(player, Text.of(TextMode.Err, targetPlayer.get().getName() + " is already the owner of claim."));
                return;
            }

            if (claim.hasFullAccess(player)) {
                targetClaims.add(claim);
            } else {
                GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "You do not own this claim."));
                return;
            }
        }

        String location = GriefPrevention.instance.dataStore.getMessage(Messages.LocationCurrentClaim);

        for (Claim currentClaim : targetClaims) {
            ArrayList<UUID> memberList = null;
            if (claimPermission == ClaimPermission.ACCESS) {
                memberList = (ArrayList<UUID>) currentClaim.getClaimData().getAccessors();
            } else if (claimPermission == ClaimPermission.BUILD) {
                memberList = (ArrayList<UUID>) currentClaim.getClaimData().getBuilders();
            } else if (claimPermission == ClaimPermission.INVENTORY) {
                memberList = (ArrayList<UUID>) currentClaim.getClaimData().getContainers();
            } else if (claimPermission == ClaimPermission.FULL) {
                memberList = (ArrayList<UUID>) currentClaim.getClaimData().getCoowners();
            }

            if (memberList.contains(targetPlayer.get().getUniqueId())) {
                String message = "Player " + target + " already has " + claimPermission + " permission.";
                if (targetPlayer.get() == GriefPrevention.PUBLIC_USER) {
                    message = "Public already has " + claimPermission + " permission.";
                }
                GriefPrevention.sendMessage(player, Text.of(TextMode.Info, message));
                return;
            } else {
                memberList.add(targetPlayer.get().getUniqueId());
            }

            currentClaim.getClaimStorage().save();
        }

        GriefPrevention.sendMessage(player, TextMode.Success, Messages.GrantPermissionConfirmation, claimPermission.name(), targetPlayer.get().getName(), location);
    }
}
