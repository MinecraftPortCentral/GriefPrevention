package me.ryanhamshire.griefprevention.command;

import me.ryanhamshire.griefprevention.GPPermissions;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.GriefPrevention.IgnoreMode;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.TextMode;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;

public class CommandIgnorePlayer implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        // validate target player
        Player targetPlayer = args.<Player>getOne("player").get();
        if (targetPlayer.hasPermission(GPPermissions.NOT_IGNORABLE)) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.PlayerNotIgnorable));
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        GriefPrevention.instance.setIgnoreStatus(player.getWorld(), player, targetPlayer, IgnoreMode.StandardIgnore);

        GriefPrevention.sendMessage(player, TextMode.Success, Messages.IgnoreConfirmation);

        return CommandResult.success();
    }
}
