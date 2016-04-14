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

public class CommandTrust implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(Text.of("An error occurred while executing this command."));
            return CommandResult.success();
        }

        String subject = args.<String>getOne("subject").get();
        Optional<User> targetPlayer = GriefPrevention.instance.resolvePlayerByName(subject);
        if (!targetPlayer.isPresent()) {
            GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "Not a valid player."));
            return CommandResult.success();
        }

        // determine which claim the player is standing in
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);
        ArrayList<Claim> targetClaims = new ArrayList<>();
        if (claim == null) {
            PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
            if (playerData == null || playerData.playerWorldClaims.get(player.getWorld().getUniqueId()) == null) {
                GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "No claims found."));
                return CommandResult.success();
            }

            for (Claim currentClaim : playerData.playerWorldClaims.get(player.getWorld().getUniqueId())) {
                targetClaims.add(currentClaim);
            }
        } else {
            targetClaims.add(claim);
        }

        String location;
        if (claim == null) {
            location = GriefPrevention.instance.dataStore.getMessage(Messages.LocationAllClaims);
        } else {
            location = GriefPrevention.instance.dataStore.getMessage(Messages.LocationCurrentClaim);
        }

        for (Claim currentClaim : targetClaims) {
            ArrayList<UUID> managers = currentClaim.getClaimData().getConfig().managers;
            if (currentClaim.isSubDivision) {
                managers = currentClaim.getClaimData().getConfig().subDivisions.get(currentClaim.id).managers;
            }
            if (managers.contains(targetPlayer.get().getUniqueId())) {
                GriefPrevention.sendMessage(player, Text.of(TextMode.Info, "Player " + subject + " is already trusted."));
                return CommandResult.success();
            } else {
                managers.add(targetPlayer.get().getUniqueId());
            }

            currentClaim.claimData.save();
        }

        GriefPrevention.sendMessage(player, TextMode.Success, Messages.GrantPermissionConfirmation, targetPlayer.get().getName(), "Full trust permission", location);
        return CommandResult.success();
    }
}
