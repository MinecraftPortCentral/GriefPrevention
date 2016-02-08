package me.ryanhamshire.GriefPrevention.command;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimsMode;
import me.ryanhamshire.GriefPrevention.CustomLogEntryTypes;
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
            if (!claim.isAdminClaim() || player.hasPermission("griefprevention.adminclaims")) {
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
                    GriefPrevention.AddLogEntry(
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
