package me.ryanhamshire.GriefPrevention.command;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;

import java.util.List;

public class CommandUntrust implements CommandExecutor {

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
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);

        String target = ctx.<String>getOne("subject").get();

        // bracket any permissions
        if (target.contains(".") && !target.startsWith("[") && !target.endsWith("]")) {
            target = "[" + target + "]";
        }

        // determine whether a single player or clearing permissions entirely
        boolean clearPermissions = false;
        User otherPlayer = null;
        if (target.equals("all")) {
            if (claim == null || claim.allowEdit(player) == null) {
                clearPermissions = true;
            } else {
                try {
                    throw new CommandException(GriefPrevention.getMessage(Messages.ClearPermsOwnerOnly));
                } catch (CommandException e) {
                    src.sendMessage(e.getText());
                    return CommandResult.success();
                }
            }
        }

        else {
            // validate player argument or group argument
            if (!target.startsWith("[") || !target.endsWith("]")) {
                otherPlayer = GriefPrevention.instance.resolvePlayerByName(target).orElse(null);
                if (!clearPermissions && otherPlayer == null && !target.equals("public")) {
                    try {
                        throw new CommandException(GriefPrevention.getMessage(Messages.PlayerNotFound2));
                    } catch (CommandException e) {
                        src.sendMessage(e.getText());
                        return CommandResult.success();
                    }
                }

                // correct to proper casing
                if (otherPlayer != null)
                    target = otherPlayer.getName();
            }
        }

        // if no claim here, apply changes to all his claims
        if (claim == null) {
            PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
            List<Claim> claimList = playerData.playerWorldClaims.get(player.getWorld().getUniqueId());
            for (Claim playerClaim : claimList) {
                // if untrusting "all" drop all permissions
                if (clearPermissions) {
                    playerClaim.clearPermissions();
                }

                // otherwise drop individual permissions
                else {
                    String idToDrop = target;
                    if (otherPlayer != null) {
                        idToDrop = otherPlayer.getUniqueId().toString();
                    }
                    playerClaim.dropPermission(idToDrop);
                    playerClaim.managers.remove(idToDrop);
                }

                // save changes
                GriefPrevention.instance.dataStore.saveClaim(playerClaim);
            }

            // beautify for output
            if (target.equals("public")) {
                target = "the public";
            }

            // confirmation message
            if (!clearPermissions) {
                GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustIndividualAllClaims, target);
            } else {
                GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustEveryoneAllClaims);
            }
        } else if (claim.allowGrantPermission(player) != null) {
            // otherwise, apply changes to only this claim
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.NoPermissionTrust, claim.getOwnerName()));
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        } else {
            // if clearing all
            if (clearPermissions) {
                // requires owner
                if (claim.allowEdit(player) != null) {
                    try {
                        throw new CommandException(GriefPrevention.getMessage(Messages.UntrustAllOwnerOnly));
                    } catch (CommandException e) {
                        src.sendMessage(e.getText());
                        return CommandResult.success();
                    }
                }

                claim.clearPermissions();
                GriefPrevention.sendMessage(player, TextMode.Success, Messages.ClearPermissionsOneClaim);
            }

            // otherwise individual permission drop
            else {
                String idToDrop = target;
                if (otherPlayer != null) {
                    idToDrop = otherPlayer.getUniqueId().toString();
                }
                boolean targetIsManager = claim.managers.contains(idToDrop);
                if (targetIsManager && claim.allowEdit(player) != null) // only
                // claim owners can untrust managers
                {
                    try {
                        throw new CommandException(GriefPrevention.getMessage(Messages.ManagersDontUntrustManagers, claim.getOwnerName()));
                    } catch (CommandException e) {
                        src.sendMessage(e.getText());
                        return CommandResult.success();
                    }
                } else {
                    claim.dropPermission(idToDrop);
                    claim.managers.remove(idToDrop);

                    // beautify for output
                    if (target.equals("public")) {
                        target = "the public";
                    }

                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustIndividualSingleClaim, target);
                }
            }

            // save changes
            GriefPrevention.instance.dataStore.saveClaim(claim);
        }

        return CommandResult.success();
    }
}
