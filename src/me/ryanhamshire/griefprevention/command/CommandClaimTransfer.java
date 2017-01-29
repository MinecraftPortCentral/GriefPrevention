package me.ryanhamshire.griefprevention.command;

import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.ClaimResult;
import me.ryanhamshire.griefprevention.api.claim.ClaimResultType;
import me.ryanhamshire.griefprevention.api.data.PlayerData;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.logging.CustomLogEntryTypes;
import me.ryanhamshire.griefprevention.message.Messages;
import me.ryanhamshire.griefprevention.message.TextMode;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class CommandClaimTransfer implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        Player player;
        try {
            player = GriefPreventionPlugin.checkPlayer(src);
        } catch (CommandException e1) {
            src.sendMessage(e1.getText());
            return CommandResult.success();
        }

        User targetPlayer = args.<User>getOne("user").orElse(null);
        if (targetPlayer == null) {
            GriefPreventionPlugin.sendMessage(player, TextMode.Err, "No user found.");
            return CommandResult.success();
        }

        // which claim is the user in?
        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAtPlayer(playerData, player.getLocation(), true);
        if (claim == null || claim.isWildernessClaim()) {
            GriefPreventionPlugin.sendMessage(player, TextMode.Instr, Messages.TransferClaimMissing);
            return CommandResult.empty();
        }

        // check additional permission for admin claims
        if (claim.isAdminClaim()) {
            if (!player.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS)) {
                try {
                    throw new CommandException(GriefPreventionPlugin.getMessage(Messages.TransferClaimPermission));
                } catch (CommandException e1) {
                    src.sendMessage(e1.getText());
                    return CommandResult.success();
                }
            }
        }

        // change ownership
        ClaimResult claimResult = claim.transferOwner(targetPlayer.getUniqueId());
        if (!claimResult.successful()) {
            PlayerData targetPlayerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), targetPlayer.getUniqueId());
            if (claimResult.getResultType() == ClaimResultType.INSUFFICIENT_CLAIM_BLOCKS) {
                src.sendMessage(Text.of(TextColors.RED, "Could not transfer claim to player with UUID " + targetPlayer.getUniqueId() + "."
                    + " Player only has " + targetPlayerData.getRemainingClaimBlocks() + " claim blocks remaining." 
                    + " The claim requires a total of " + claim.getArea() + " claim blocks to own."));
            } else if (claimResult.getResultType() == ClaimResultType.WRONG_CLAIM_TYPE) {
                src.sendMessage(Text.of(TextColors.RED, "The wilderness claim cannot be transferred."));
            } else if (claimResult.getResultType() == ClaimResultType.CLAIM_EVENT_CANCELLED) {
                src.sendMessage(Text.of(TextColors.RED, "Could not transfer the claim. A plugin has cancelled the TransferClaimEvent."));
            }
            return CommandResult.success();
        }

        // confirm
        GriefPreventionPlugin.sendMessage(player, TextMode.Success, Messages.TransferSuccess);
        GriefPreventionPlugin.addLogEntry(player.getName() + " transferred a claim at "
                        + GriefPreventionPlugin.getfriendlyLocationString(claim.getLesserBoundaryCorner()) + " to " + targetPlayer.getName() + ".",
                CustomLogEntryTypes.AdminActivity);

        return CommandResult.success();

    }
}
