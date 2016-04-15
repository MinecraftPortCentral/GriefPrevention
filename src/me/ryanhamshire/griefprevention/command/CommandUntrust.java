package me.ryanhamshire.griefprevention.command;

import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.claim.Claim;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

public class CommandUntrust implements CommandExecutor {

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
        Optional<User> targetPlayer = GriefPrevention.instance.resolvePlayerByName(subject);
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);
        System.out.println("claim = " + claim);
        // determine whether a single player or clearing permissions entirely
        boolean clearPermissions = false;
        if (subject.equals("all")) {
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
        } else {
            // validate player argument
            if (!targetPlayer.isPresent()) {
                GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "Not a valid player."));
                return CommandResult.success();
            }
        }

        // determine which claim the player is standing in
        ArrayList<Claim> targetClaims = new ArrayList<>();
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        if (claim == null) {
            if (playerData == null || playerData.playerWorldClaims.get(player.getWorld().getUniqueId()) == null) {
                GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "No claim found."));
                return CommandResult.success();
            }

            for (Claim currentClaim : playerData.playerWorldClaims.get(player.getWorld().getUniqueId())) {
                targetClaims.add(currentClaim);
            }
        } else {
            // verify claim belongs to player
            UUID ownerID = claim.ownerID;
            if (ownerID == null && claim.parent != null) {
                ownerID = claim.parent.ownerID;
            }
            if (targetPlayer.get().getUniqueId().equals(claim.ownerID)) {
                GriefPrevention.sendMessage(player, Text.of(TextMode.Err, targetPlayer.get().getName() + " is owner of claim and cannot be untrusted."));
                return CommandResult.success();
            }

            if (player.getUniqueId().equals(ownerID) || (playerData != null && playerData.ignoreClaims)) {
                targetClaims.add(claim);
            } else {
                GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "You do not own this claim."));
                return CommandResult.success();
            }
        }

        for (Claim currentClaim : targetClaims) {
            ArrayList<UUID> managers = currentClaim.getClaimData().getConfig().managers;
            if (currentClaim.isSubDivision) {
                managers = currentClaim.getClaimData().getConfig().subDivisions.get(currentClaim.id).managers;
            }
            // if untrusting "all" drop all permissions
            if (clearPermissions) {
                managers.clear();
            } else {
                if (!managers.contains(targetPlayer.get().getUniqueId())) {
                    GriefPrevention.sendMessage(player, Text.of(TextMode.Info, "Player " + subject + " is not trusted."));
                    return CommandResult.success();
                } else {
                    managers.remove(targetPlayer.get().getUniqueId());
                }
            }

            currentClaim.claimData.save();
        }

        if (!clearPermissions) {
            GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustIndividualSingleClaim, subject);
        } else {
            GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustIndividualAllClaims);
        }

        return CommandResult.success();
    }
}
