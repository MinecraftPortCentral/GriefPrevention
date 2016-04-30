package me.ryanhamshire.griefprevention.command;

import me.ryanhamshire.griefprevention.CustomLogEntryTypes;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.TextMode;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;

public class CommandSetAccruedClaimBlocks implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        // parse the adjustment amount
        int newAmount = args.<Integer>getOne("amount").get();

        // find the specified player
        User targetPlayer;
        try {
            targetPlayer = args.<String>getOne("player").flatMap(GriefPrevention.instance::resolvePlayerByName)
                    .orElseThrow(() -> new CommandException(GriefPrevention.getMessage(Messages.PlayerNotFound2)));
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        // set player's blocks
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), targetPlayer.getUniqueId());
        playerData.setAccruedClaimBlocks(newAmount);
        playerData.getStorageData().save();

        GriefPrevention.sendMessage(player, TextMode.Success, Messages.SetClaimBlocksSuccess);
        if (player != null) {
            GriefPrevention.addLogEntry(player.getName() + " set " + targetPlayer.getName() + "'s accrued claim blocks to " + newAmount + ".",
                    CustomLogEntryTypes.AdminActivity);
        }

        return CommandResult.success();
    }
}
