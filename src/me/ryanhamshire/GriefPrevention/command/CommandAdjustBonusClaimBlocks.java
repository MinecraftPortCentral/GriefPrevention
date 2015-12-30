package me.ryanhamshire.GriefPrevention.command;

import me.ryanhamshire.GriefPrevention.CustomLogEntryTypes;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.user.UserStorageService;

import java.util.UUID;

public class CommandAdjustBonusClaimBlocks implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e1) {
            src.sendMessage(e1.getText());
            return CommandResult.success();
        }

        // parse the adjustment amount
        int adjustment = args.<Integer>getOne("amount").get();
        String target = args.<String>getOne("player").get();

        // if granting blocks to all players with a specific permission
        if (target.startsWith("[") && target.endsWith("]")) {
            String permissionIdentifier = target.substring(1, target.length() - 1);
            int newTotal = GriefPrevention.instance.dataStore.adjustGroupBonusBlocks(permissionIdentifier, adjustment);

            GriefPrevention.sendMessage(player, TextMode.Success, Messages.AdjustGroupBlocksSuccess, permissionIdentifier,
                    String.valueOf(adjustment), String.valueOf(newTotal));
            if (player != null)
                GriefPrevention
                        .AddLogEntry(
                                player.getName() + " adjusted " + permissionIdentifier + "'s bonus claim blocks by " + adjustment + ".");

            return CommandResult.success();
        }

        // otherwise, find the specified player
        User targetPlayer;
        try {
            UUID playerID = UUID.fromString(target);
            targetPlayer = Sponge.getGame().getServiceManager().provideUnchecked(UserStorageService.class).get(playerID).orElse(null);

        } catch (IllegalArgumentException e) {
            targetPlayer = GriefPrevention.instance.resolvePlayerByName(target).orElse(null);
        }

        if (targetPlayer == null) {
            try {
                throw new CommandException(GriefPrevention.getMessage(Messages.PlayerNotFound2));
            } catch (CommandException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        // give blocks to player
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), targetPlayer.getUniqueId());
        playerData.setBonusClaimBlocks(player.getWorld(), playerData.getBonusClaimBlocks(player.getWorld()) + adjustment);
        playerData.worldStorageData.get(player.getWorld().getUniqueId()).save();

        GriefPrevention
                .sendMessage(player, TextMode.Success, Messages.AdjustBlocksSuccess, targetPlayer.getName(), String.valueOf(adjustment),
                        String.valueOf(playerData.getBonusClaimBlocks(player.getWorld())));
        if (player != null)
            GriefPrevention.AddLogEntry(
                    player.getName() + " adjusted " + targetPlayer.getName() + "'s bonus claim blocks by " + adjustment + ".",
                    CustomLogEntryTypes.AdminActivity);


        return CommandResult.success();
    }
}
