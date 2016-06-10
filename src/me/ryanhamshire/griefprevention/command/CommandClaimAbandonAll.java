package me.ryanhamshire.griefprevention.command;

import com.google.common.collect.ImmutableSet;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.Visualization;
import me.ryanhamshire.griefprevention.claim.Claim;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;

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
            // remove all context permissions
            player.getSubjectData().clearPermissions(ImmutableSet.of(claim.getContext()));
            if (claim.isSubdivision() || claim.isAdminClaim()) {
                continue;
            }
            playerData.setAccruedClaimBlocks(playerData.getAccruedClaimBlocks() - (int) Math.ceil((claim.getArea() * (1 - GriefPrevention.getActiveConfig(player.getWorld().getProperties()).getConfig().claim.abandonReturnRatio))));
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
