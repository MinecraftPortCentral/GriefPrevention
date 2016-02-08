package me.ryanhamshire.GriefPrevention.command;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

import java.util.Map.Entry;
import java.util.UUID;

public class CommandIgnoredPlayerList implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        StringBuilder builder = new StringBuilder();
        for (Entry<UUID, Boolean> entry : playerData.ignoredPlayers.entrySet()) {
            if (entry.getValue() != null) {
                // if not an admin ignore, add it to the list
                if (!entry.getValue()) {
                    builder.append(CommandHelper.lookupPlayerName(entry.getKey()));
                    builder.append(" ");
                }
            }
        }

        String list = builder.toString().trim();
        if (list.isEmpty()) {
            GriefPrevention.sendMessage(player, TextMode.Info, Messages.NotIgnoringAnyone);
        } else {
            GriefPrevention.sendMessage(player, Text.of(TextMode.Info, list));
        }

        return CommandResult.success();
    }
}
