package me.ryanhamshire.GriefPrevention.command;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;

public class CommandDebug implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        if (GriefPrevention.getGlobalConfig().getConfig().logging.loggingDebug) {
            GriefPrevention.sendMessage(player, TextMode.Success, "Debug disabled.");
            GriefPrevention.getGlobalConfig().getConfig().logging.loggingDebug = false;
        } else {
            GriefPrevention.sendMessage(player, TextMode.Success, "Debug enabled.");
            GriefPrevention.getGlobalConfig().getConfig().logging.loggingDebug = true;
        }

        return CommandResult.success();
    }
}
