package me.ryanhamshire.griefprevention.command;

import me.ryanhamshire.griefprevention.task.WelcomeTask;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;

public class CommandClaimBook implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        for (Player otherPlayer : ctx.<Player>getAll("player")) {
            WelcomeTask task = new WelcomeTask(otherPlayer);
            task.run();
        }

        return CommandResult.success();
    }
}
