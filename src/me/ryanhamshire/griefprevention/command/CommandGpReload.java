package me.ryanhamshire.griefprevention.command;

import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.TextMode;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.text.Text;

public class CommandGpReload implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        GriefPrevention.instance.loadConfig();
        GriefPrevention.sendMessage(src, Text.of(TextMode.Success,
                "Configuration updated. If you have updated your Grief Prevention JAR, you still need to restart your server."));

        return CommandResult.success();
    }
}
