package me.ryanhamshire.GriefPrevention.command;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

public class CommandClaim implements CommandExecutor {

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
        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, playerData.lastClaim);

        if (claim != null) {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Instr, GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner())
                    + GriefPrevention.instance.dataStore.getMessage(Messages.ContinueBlockMath, String.valueOf(claim.getArea()))));
        } else {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "No claim in your current location."));
        }

        return CommandResult.success();
    }
}
