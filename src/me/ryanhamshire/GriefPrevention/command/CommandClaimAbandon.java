package me.ryanhamshire.GriefPrevention.command;

import com.google.common.collect.ImmutableSet;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimsMode;
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
import org.spongepowered.api.service.context.Context;

public class CommandClaimAbandon implements CommandExecutor {

    private boolean deleteTopLevelClaim;

    public CommandClaimAbandon(boolean deleteTopLevelClaim) {
        this.deleteTopLevelClaim = deleteTopLevelClaim;
    }

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }
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

        // warn if has children and we're not explicitly deleting a top level
        // claim
        else if (claim.children.size() > 0 && !deleteTopLevelClaim) {
            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.DeleteTopLevelClaim);
            return CommandResult.empty();
        }

        else {
            // delete it
            claim.removeSurfaceFluids(null);
            // remove all context permissions
            player.getSubjectData().clearPermissions(ImmutableSet.of(claim.getContext()));
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
}
