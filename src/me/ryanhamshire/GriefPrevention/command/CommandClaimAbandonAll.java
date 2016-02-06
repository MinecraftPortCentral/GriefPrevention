package me.ryanhamshire.GriefPrevention.command;

import com.google.common.collect.ImmutableSet;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import me.ryanhamshire.GriefPrevention.Visualization;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;

import java.util.List;

public class CommandClaimAbandonAll implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }
        // count claims
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        int originalClaimCount = playerData.playerWorldClaims.get(player.getWorld().getUniqueId()).size();

        // check count
        if (originalClaimCount == 0) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.YouHaveNoClaims));
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        // adjust claim blocks
        List<Claim> claimList = playerData.playerWorldClaims.get(player.getWorld().getUniqueId());
        for (Claim claim : claimList) {
            // remove all context permissions
            player.getSubjectData().clearPermissions(ImmutableSet.of(claim.getContext()));
            playerData.setAccruedClaimBlocks(player.getWorld(),
                    playerData.getAccruedClaimBlocks(player.getWorld()) - (int) Math.ceil((claim.getArea() * (1 - GriefPrevention.getActiveConfig(player.getWorld().getProperties()).getConfig().claim.abandonReturnRatio))));
        }

        // delete them
        GriefPrevention.instance.dataStore.deleteClaimsForPlayer(player.getUniqueId(), false);

        // inform the player
        int remainingBlocks = playerData.getRemainingClaimBlocks(player.getWorld());
        GriefPrevention.sendMessage(player, TextMode.Success, Messages.SuccessfulAbandon, String.valueOf(remainingBlocks));

        // revert any current visualization
        Visualization.Revert(player);

        return CommandResult.success();
    }
}
