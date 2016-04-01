package me.ryanhamshire.griefprevention.command;

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
import org.spongepowered.api.entity.living.player.User;

public class CommandSeparate implements CommandExecutor {

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

        GriefPrevention.instance.setIgnoreStatus(player.getWorld(), targetPlayer, targetPlayer2, IgnoreMode.AdminIgnore);

        GriefPrevention.sendMessage(player, TextMode.Success, Messages.SeparateConfirmation);
        return CommandResult.success();
    }
}
