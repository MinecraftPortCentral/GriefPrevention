package me.ryanhamshire.griefprevention.command;

import me.ryanhamshire.griefprevention.CustomLogEntryTypes;
import me.ryanhamshire.griefprevention.GPPermissions;
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

public class CommandClaimDelete implements CommandExecutor {

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
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true /* ignore height */, null);

        if (claim == null) {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.DeleteClaimMissing);
        } else {
            // deleting an admin claim additionally requires the adminclaims permission
            if (!claim.isAdminClaim() || player.hasPermission(GPPermissions.CLAIMS_ADMIN)) {
                PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
                if (claim.children.size() > 0 && !playerData.warnedAboutMajorDeletion) {
                    GriefPrevention.sendMessage(player, TextMode.Warn, Messages.DeletionSubdivisionWarning);
                    playerData.warnedAboutMajorDeletion = true;
                } else {
                    claim.removeSurfaceFluids(null);
                    GriefPrevention.instance.dataStore.deleteClaim(claim, true);

                    // if in a creative mode world, /restorenature the claim
                    if (GriefPrevention.instance
                            .claimModeIsActive(claim.getLesserBoundaryCorner().getExtent().getProperties(), ClaimsMode.Creative)) {
                        GriefPrevention.instance.restoreClaim(claim, 0);
                    }

                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.DeleteSuccess);
                    GriefPrevention.addLogEntry(
                            player.getName() + " deleted " + claim.getOwnerName() + "'s claim at "
                                    + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()),
                            CustomLogEntryTypes.AdminActivity);

                    // revert any current visualization
                    Visualization.Revert(player);

                    playerData.warnedAboutMajorDeletion = false;
                }
            } else {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.CantDeleteAdminClaim);
            }
        }

        return CommandResult.success();
    }
}
