package me.ryanhamshire.GriefPrevention.command;

import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;

public class CommandTrust implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        // most trust commands use this helper method, it keeps them
        // consistent
        try {
            CommandHelper.handleTrustCommand(GriefPrevention.checkPlayer(src), ClaimPermission.Build,
                    args.<String>getOne("subject").get());
        } catch (CommandException e) {
            src.sendMessage(Text.of("An error occurred while executing this command."));
        }
        return CommandResult.success();
    }
}
