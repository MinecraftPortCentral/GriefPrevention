package me.ryanhamshire.GriefPrevention.command;

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

public class CommandAbandonAllClaims implements CommandExecutor {

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
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
        int originalClaimCount = playerData.getClaims().size();

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
        for (Claim claim : playerData.getClaims()) {
            playerData.setAccruedClaimBlocks(
                    playerData.getAccruedClaimBlocks() - (int) Math.ceil((claim.getArea() * (1 - GriefPrevention.instance.config_claims_abandonReturnRatio))));
        }

        // delete them
        GriefPrevention.instance.dataStore.deleteClaimsForPlayer(player.getUniqueId(), false);

        // inform the player
        int remainingBlocks = playerData.getRemainingClaimBlocks();
        GriefPrevention.sendMessage(player, TextMode.Success, Messages.SuccessfulAbandon, String.valueOf(remainingBlocks));

        // revert any current visualization
        Visualization.Revert(player);

        return CommandResult.success();
    }
}
