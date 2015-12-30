package me.ryanhamshire.GriefPrevention.command;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.GriefPrevention.IgnoreMode;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;

public class CommandUnignorePlayer implements CommandExecutor {

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
        User targetPlayer = args.<User>getOne("player").get();

        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        Boolean ignoreStatus = playerData.ignoredPlayers.get(targetPlayer.getUniqueId());
        if (ignoreStatus == null || ignoreStatus == true) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.NotIgnoringPlayer));
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        GriefPrevention.instance.setIgnoreStatus(player.getWorld(), player, targetPlayer, IgnoreMode.None);

        GriefPrevention.sendMessage(player, TextMode.Success, Messages.UnIgnoreConfirmation);

        return CommandResult.success();
    }
}
