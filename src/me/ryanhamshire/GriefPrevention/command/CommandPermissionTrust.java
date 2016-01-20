package me.ryanhamshire.GriefPrevention.command;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;

public class CommandPermissionTrust implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        try {
            CommandHelper.handleTrustCommand(GriefPrevention.checkPlayer(src), null, ctx.<String>getOne("target").get());
        } catch (CommandException e) {
            src.sendMessage(Text.of("An error occurred while executing that command."));
        }
        return CommandResult.success();
    }
}
