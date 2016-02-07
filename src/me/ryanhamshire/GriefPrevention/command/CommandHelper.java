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
package me.ryanhamshire.GriefPrevention.command;

import com.google.common.collect.ImmutableSet;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.ClaimsMode;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import me.ryanhamshire.GriefPrevention.Visualization;
import me.ryanhamshire.GriefPrevention.configuration.ClaimStorageData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
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
        }
        else {
            // delete it
            claim.removeSurfaceFluids(null);
            GriefPrevention.instance.dataStore.deleteClaim(claim, true);

            // if in a creative mode world, restore the claim area
            if (GriefPrevention.instance.claimModeIsActive(claim.getLesserBoundaryCorner().getExtent().getProperties(), ClaimsMode.Creative)) {
                GriefPrevention.AddLogEntry(
                        player.getName() + " abandoned a claim @ " + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
                GriefPrevention.sendMessage(player, TextMode.Warn, Messages.UnclaimCleanupWarning);
                GriefPrevention.instance.restoreClaim(claim, 20L * 60 * 2);
            }

            // adjust claim blocks when abandoning a top level claim
            if (claim.parent == null) {
                playerData.setAccruedClaimBlocks(player.getWorld(),
                        playerData.getAccruedClaimBlocks(player.getWorld()) - (int) Math.ceil((claim.getArea() * (1 - GriefPrevention.getActiveConfig(player.getWorld().getProperties()).getConfig().claim.abandonReturnRatio))));
            }

            // tell the player how many claim blocks he has left
            int remainingBlocks = playerData.getRemainingClaimBlocks(player.getWorld());
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
        if (playerID == null)
            return "somebody";

        // check the cache
        Optional<User> player = Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(playerID);
        if (player.isPresent() || player.get().isOnline()) {
            return player.get().getName();
        } else {
            return "someone";
        }
    }

    // helper method keeps the trust commands consistent and eliminates duplicate code
    public static void handleTrustCommand(Player player, ClaimPermission permissionLevel, String recipientName) throws CommandException {
        // determine which claim the player is standing in
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true /* ignore height */, null);

        // validate player or group argument
        String permission = null;
        User otherPlayer = null;
        UUID recipientID = null;
        if (recipientName.startsWith("[") && recipientName.endsWith("]")) {
            permission = recipientName.substring(1, recipientName.length() - 1);
            if (permission == null || permission.isEmpty()) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.InvalidPermissionID);
                return;
            }
        }
        else if (recipientName.contains(".")) {
            permission = recipientName;
        }
        else {
            otherPlayer = GriefPrevention.instance.resolvePlayerByName(recipientName).orElse(null);
            if (otherPlayer == null && !recipientName.equals("public") && !recipientName.equals("all")) {
                throw new CommandException(GriefPrevention.getMessage(Messages.PlayerNotFound2));
            }

            if (otherPlayer != null) {
                recipientName = otherPlayer.getName();
                recipientID = otherPlayer.getUniqueId();
            } else {
                recipientName = "public";
            }
        }

        // determine which claims should be modified
        ArrayList<Claim> targetClaims = new ArrayList<Claim>();
        if (claim == null) {
            PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
            targetClaims.addAll(playerData.playerWorldClaims.get(player.getWorld().getUniqueId()));
        } else {
            // check permission here
            if (claim.allowGrantPermission(player) != null) {
                throw new CommandException(GriefPrevention.getMessage(Messages.NoPermissionTrust, claim.getOwnerName()));
            }

            // see if the player has the level of permission he's trying to grant
            String errorMessage = null;

            // permission level null indicates granting permission trust
            if (permissionLevel == null) {
                errorMessage = claim.allowEdit(player);
                if (errorMessage != null) {
                    errorMessage = "Only " + claim.getOwnerName() + " can grant /PermissionTrust here.";
                }
            }

            // otherwise just use the ClaimPermission enum values
            else {
                switch (permissionLevel) {
                    case Access:
                        errorMessage = claim.allowAccess(player.getWorld(), player);
                        break;
                    case Inventory:
                        errorMessage = claim.allowContainers(player);
                        break;
                    default:
                        errorMessage = claim.allowBuild(player, BlockTypes.AIR);
                }
            }

            // error message for trying to grant a permission the player doesn't have
            if (errorMessage != null) {
                throw new CommandException(GriefPrevention.getMessage(Messages.CantGrantThatPermission));
            }

            targetClaims.add(claim);
        }

        // if we didn't determine which claims to modify, tell the player to be specific
        if (targetClaims.size() == 0) {
            throw new CommandException(GriefPrevention.getMessage(Messages.GrantPermissionNoClaim));
        }

        // apply changes
        for (int i = 0; i < targetClaims.size(); i++) {
            Claim currentClaim = targetClaims.get(i);
            String identifierToAdd = recipientName;
            if (permission != null) {
                identifierToAdd = "[" + permission + "]";
            } else if (recipientID != null) {
                identifierToAdd = recipientID.toString();
            }

            if (permissionLevel == null) {
                if (!currentClaim.managers.contains(identifierToAdd)) {
                    currentClaim.managers.add(identifierToAdd);
                }
            } else {
                currentClaim.setPermission(identifierToAdd, permissionLevel);
            }
            GriefPrevention.instance.dataStore.saveClaim(currentClaim);
        }

        // notify player
        if (recipientName.equals("public"))
            recipientName = GriefPrevention.instance.dataStore.getMessage(Messages.CollectivePublic);
        String permissionDescription;
        if (permissionLevel == null) {
            permissionDescription = GriefPrevention.instance.dataStore.getMessage(Messages.PermissionsPermission);
        } else if (permissionLevel == ClaimPermission.Build) {
            permissionDescription = GriefPrevention.instance.dataStore.getMessage(Messages.BuildPermission);
        } else if (permissionLevel == ClaimPermission.Access) {
            permissionDescription = GriefPrevention.instance.dataStore.getMessage(Messages.AccessPermission);
        } else // ClaimPermission.Inventory
        {
            permissionDescription = GriefPrevention.instance.dataStore.getMessage(Messages.ContainersPermission);
        }

        String location;
        if (claim == null) {
            location = GriefPrevention.instance.dataStore.getMessage(Messages.LocationAllClaims);
        } else {
            location = GriefPrevention.instance.dataStore.getMessage(Messages.LocationCurrentClaim);
        }

        GriefPrevention.sendMessage(player, TextMode.Success, Messages.GrantPermissionConfirmation, recipientName, permissionDescription, location);
    }

    public static CommandResult handleFlagPermission(Player player, CommandContext ctx, String permission) {
        String target = ctx.<String>getOne("target").get();
        String name = ctx.<String>getOne("name").get();
        String flag = ctx.<String>getOne("flag").get();
        String value = ctx.<String>getOne("value").get();

        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);

        if (claim != null) {
            if (claim.getClaimData().getConfig().flags.getFlagValue(flag) != null) {
                if (target.equalsIgnoreCase("player")) {
                    Optional<Player> targetPlayer = Sponge.getServer().getPlayer(name);
                    if (targetPlayer.isPresent()) {
                        Subject subj = targetPlayer.get().getContainingCollection().get(targetPlayer.get().getIdentifier());
                        subj.getSubjectData().setPermission(ImmutableSet.of(claim.getContext()), permission + flag,
                                Tristate.fromBoolean(Boolean.valueOf(value)));
                        player.sendMessage(Text.of(TextColors.GREEN, "Set permission of ", flag, " to ", value, " for ", target, " ", name, "."));
                    } else {
                        GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "Not a valid player."));
                    }
                } else if (target.equalsIgnoreCase("group")) {
                    PermissionService service = Sponge.getServiceManager().provide(PermissionService.class).get();
                    Subject subj = service.getGroupSubjects().get(name);
                    if (subj != null) {
                        subj.getSubjectData().setPermission(ImmutableSet.of(claim.getContext()), permission + flag,
                                Tristate.fromBoolean(Boolean.valueOf(value)));
                        player.sendMessage(Text.of(TextColors.GREEN, "Set permission of ", flag, " to ", value, " for ", target, " ", name, "."));
                    } else {
                        GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "Not a valid group."));
                    }
                }
            } else {
                GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "Not a valid flag."));
            }
        } else {
            GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "No claim found."));
        }

        return CommandResult.success();
    }
}
