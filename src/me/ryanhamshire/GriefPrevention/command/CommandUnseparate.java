package me.ryanhamshire.GriefPrevention.command;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.GriefPrevention.IgnoreMode;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;

public class CommandUnseparate implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }
        // validate target players
        User targetPlayer = args.<User>getOne("player1").get();
        User targetPlayer2 = args.<User>getOne("player2").get();

        GriefPrevention.instance.setIgnoreStatus(player.getWorld(), targetPlayer, targetPlayer2, IgnoreMode.None);
        GriefPrevention.instance.setIgnoreStatus(player.getWorld(), targetPlayer2, targetPlayer, IgnoreMode.None);

        GriefPrevention.sendMessage(player, TextMode.Success, Messages.UnSeparateConfirmation);
        return CommandResult.success();
    }
}
