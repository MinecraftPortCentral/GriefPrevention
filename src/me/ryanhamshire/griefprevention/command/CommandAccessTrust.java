package me.ryanhamshire.griefprevention.command;

import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.claim.ClaimPermission;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;

public class CommandAccessTrust implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        try {
            CommandHelper.handleTrustCommand(GriefPrevention.checkPlayer(src), ClaimPermission.Access, ctx.<String>getOne("target").get());
        } catch (CommandException e) {
            src.sendMessage(e.getText());
        }
        return CommandResult.success();
    }
}
