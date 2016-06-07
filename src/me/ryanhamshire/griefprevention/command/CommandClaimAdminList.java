package me.ryanhamshire.griefprevention.command;

import me.ryanhamshire.griefprevention.ClaimWorldManager;
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
import org.spongepowered.api.text.Text;

public class CommandClaimAdminList implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        // find admin claims
        ClaimWorldManager claimWorldManager = GriefPrevention.instance.dataStore.getClaimWorldManager(player.getWorld().getProperties());
        for (Claim claim : claimWorldManager.getWorldClaims()) {
            if (claim.isAdminClaim()) {
                GriefPrevention.sendMessage(src, TextMode.Instr, Messages.ClaimsListHeader);;
                GriefPrevention.sendMessage(src, Text.of(TextMode.Instr, GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner())));
            }
        }

        return CommandResult.success();
    }
}
