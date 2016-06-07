package me.ryanhamshire.griefprevention.command;

import me.ryanhamshire.griefprevention.ClaimWorldManager;
import me.ryanhamshire.griefprevention.ClaimWorldManager.NoTransferException;
import me.ryanhamshire.griefprevention.CustomLogEntryTypes;
import me.ryanhamshire.griefprevention.GPPermissions;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.claim.Claim;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;

import java.util.UUID;

public class CommandClaimTransfer implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e1) {
            src.sendMessage(e1.getText());
            return CommandResult.success();
        }
        // which claim is the user in?
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);
        if (claim == null) {
            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.TransferClaimMissing);
            return CommandResult.empty();
        }

        // check additional permission for admin claims
        if (claim.isAdminClaim() && !player.hasPermission(GPPermissions.CLAIMS_ADMIN)) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.TransferClaimPermission));
            } catch (CommandException e1) {
                src.sendMessage(e1.getText());
                return CommandResult.success();
            }
        }

        UUID newOwnerID = null; // no argument = make an admin claim
        String ownerName = "admin";

        User targetPlayer = args.<User>getOne("player").get();
        newOwnerID = targetPlayer.getUniqueId();
        ownerName = targetPlayer.getName();

        // change ownerhsip
        try {
            ClaimWorldManager claimWorldManager = GriefPrevention.instance.dataStore.getClaimWorldManager(claim.world.getProperties());
            claimWorldManager.changeClaimOwner(claim, newOwnerID);
        } catch (NoTransferException e) {
            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.TransferTopLevel);
            return CommandResult.empty();
        }

        // confirm
        GriefPrevention.sendMessage(player, TextMode.Success, Messages.TransferSuccess);
        GriefPrevention.addLogEntry(player.getName() + " transferred a claim at "
                        + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()) + " to " + ownerName + ".",
                CustomLogEntryTypes.AdminActivity);

        return CommandResult.success();

    }
}
