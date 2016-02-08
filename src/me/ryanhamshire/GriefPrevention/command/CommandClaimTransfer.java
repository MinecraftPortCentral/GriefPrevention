package me.ryanhamshire.GriefPrevention.command;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.CustomLogEntryTypes;
import me.ryanhamshire.GriefPrevention.DataStore.NoTransferException;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;

import java.util.Optional;
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
        if (claim.isAdminClaim() && !player.hasPermission("griefprevention.adminclaims")) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.TransferClaimPermission));
            } catch (CommandException e1) {
                src.sendMessage(e1.getText());
                return CommandResult.success();
            }
        }

        UUID newOwnerID = null; // no argument = make an admin claim
        String ownerName = "admin";

        Optional<User> targetOpt = args.<User>getOne("target");
        if (targetOpt.isPresent()) {
            User targetPlayer = targetOpt.get();
            newOwnerID = targetPlayer.getUniqueId();
            ownerName = targetPlayer.getName();
        }

        // change ownerhsip
        try {
            GriefPrevention.instance.dataStore.changeClaimOwner(claim, newOwnerID);
        } catch (NoTransferException e) {
            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.TransferTopLevel);
            return CommandResult.empty();
        }

        // confirm
        GriefPrevention.sendMessage(player, TextMode.Success, Messages.TransferSuccess);
        GriefPrevention.AddLogEntry(player.getName() + " transferred a claim at "
                        + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()) + " to " + ownerName + ".",
                CustomLogEntryTypes.AdminActivity);

        return CommandResult.success();

    }
}
