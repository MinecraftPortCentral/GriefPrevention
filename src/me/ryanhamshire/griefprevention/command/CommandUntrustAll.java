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

import java.util.List;
import java.util.Optional;

public class CommandUntrustAll implements CommandExecutor {

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

        // validate player argument
        if (!targetPlayer.isPresent()) {
            GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "Not a valid player."));
            return CommandResult.success();
        } else if (targetPlayer.isPresent() && targetPlayer.get().getUniqueId().equals(player.getUniqueId())) {
            GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "You cannot not untrust yourself."));
            return CommandResult.success();
        }

        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        List<Claim> claimList = null;
        if (playerData != null) {
            claimList = playerData.getClaims();
        }

        if (playerData == null || claimList == null || claimList.size() == 0) {
            GriefPrevention.sendMessage(player, Text.of(TextMode.Err, "You have no claims to untrust."));
            return CommandResult.success();
        }

        for (Claim claim : claimList) {
            claim.getClaimData().getAccessors().remove(targetPlayer.get().getUniqueId());
            claim.getClaimData().getBuilders().remove(targetPlayer.get().getUniqueId());
            claim.getClaimData().getContainers().remove(targetPlayer.get().getUniqueId());
            claim.getClaimData().getCoowners().remove(targetPlayer.get().getUniqueId());
            claim.getClaimStorage().save();
        }

        GriefPrevention.sendMessage(player, TextMode.Success, Messages.UntrustIndividualAllClaims, targetPlayer.get().getName());
        return CommandResult.success();
    }
}
