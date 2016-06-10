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
        Optional<User> targetPlayer = Optional.empty();
        if (subject.equalsIgnoreCase("public") || subject.equalsIgnoreCase("all")) {
            targetPlayer = Optional.of(GriefPrevention.PUBLIC_USER);
        } else {
            targetPlayer = GriefPrevention.instance.resolvePlayerByName(subject);
        }

        if (!targetPlayer.isPresent()) {
            GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "Not a valid player."));
            return CommandResult.success();
        }

        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);
        if (claim == null) {
            GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "No claim found at location. If you want to untrust all claims, use /untrustall instead."));
            return CommandResult.success();
        }

        // determine which claim the player is standing in
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        // verify player has full perms
        UUID ownerID = claim.ownerID;
        if (ownerID == null && claim.parent != null) {
            ownerID = claim.parent.ownerID;
        }
        if (targetPlayer.get().getUniqueId().equals(ownerID)) {
            GriefPrevention.sendMessage(player, Text.of(TextMode.Err, targetPlayer.get().getName() + " is owner of claim and cannot be untrusted."));
            return CommandResult.success();
        }

        if (!claim.hasFullAccess(player) && !(playerData != null && playerData.ignoreClaims)) {
            GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "You do not have permission to edit this claim."));
            return CommandResult.success();
        }

        claim.getClaimData().getAccessors().remove(targetPlayer.get().getUniqueId());
        claim.getClaimData().getBuilders().remove(targetPlayer.get().getUniqueId());
        claim.getClaimData().getContainers().remove(targetPlayer.get().getUniqueId());
        claim.getClaimData().getCoowners().remove(targetPlayer.get().getUniqueId());
        claim.getClaimData().setRequiresSave(true);
        claim.getClaimStorage().save();

        GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustIndividualSingleClaim, subject);
        return CommandResult.success();
    }
}
