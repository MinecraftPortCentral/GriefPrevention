package me.ryanhamshire.griefprevention.command;

import com.google.common.collect.ImmutableSet;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.Visualization;
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.claim.ClaimsMode;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;

import java.util.UUID;

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
        } else if (claim.allowEdit(player) != null) {
            // verify ownership
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.NotYourClaim);
        }

        // warn if has children and we're not explicitly deleting a top level claim
        else if (claim.children.size() > 0 && !deleteTopLevelClaim) {
            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.DeleteTopLevelClaim);
            return CommandResult.empty();
        } else {
            // delete it
            claim.removeSurfaceFluids(null);
            // remove all context permissions
            player.getSubjectData().clearPermissions(ImmutableSet.of(claim.getContext()));
            GriefPrevention.instance.dataStore.deleteClaim(claim, true);

            // if in a creative mode world, restore the claim area
            if (GriefPrevention.instance.claimModeIsActive(claim.getLesserBoundaryCorner().getExtent().getProperties(), ClaimsMode.Creative)) {
                GriefPrevention.addLogEntry(
                        player.getName() + " abandoned a claim @ " + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()));
                GriefPrevention.sendMessage(player, TextMode.Warn, Messages.UnclaimCleanupWarning);
                GriefPrevention.instance.restoreClaim(claim, 20L * 60 * 2);
            }

            // only owners can abandon non-admin claims
            // this prevents blocks being gained without spending
            if (!claim.isAdminClaim() && player.getUniqueId().equals(claim.ownerID)) {
                // adjust claim blocks when abandoning a top level claim
                if (claim.parent == null) {
                    int newAccruedClaimCount = playerData.getAccruedClaimBlocks(player.getWorld()) - (int) Math
                            .ceil((claim.getArea() * (1 - GriefPrevention.getActiveConfig(player.getWorld().getProperties())
                                    .getConfig().claim.abandonReturnRatio)));
                    playerData.setAccruedClaimBlocks(player.getWorld(), newAccruedClaimCount);
                }
    
                // tell the player how many claim blocks he has left
                int remainingBlocks = playerData.getRemainingClaimBlocks(player.getWorld());
                GriefPrevention.sendMessage(player, TextMode.Success, Messages.AbandonSuccess, String.valueOf(remainingBlocks));
            } else if (!player.getUniqueId().equals(claim.ownerID)) {
                UUID ownerId = claim.ownerID;
                if (claim.isSubDivision) {
                    ownerId = claim.parent.ownerID;
                }

                User user = GriefPrevention.getOrCreateUser(ownerId);
                playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), user.getUniqueId());
                int remainingBlocks = playerData.getRemainingClaimBlocks(player.getWorld());
                GriefPrevention.sendMessage(player, TextMode.Success, Messages.AbandonOtherSuccess, user.getName(), String.valueOf(remainingBlocks));
            }

            // revert any current visualization
            Visualization.Revert(player);

            playerData.warnedAboutMajorDeletion = false;
        }

        return CommandResult.success();
    }
}
