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
import org.spongepowered.api.entity.living.player.User;

public class CommandGivePet implements CommandExecutor {

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

        // special case: cancellation
        if (args.getOne("player").orElse(false).equals(true)) {
            playerData.petGiveawayRecipient = null;
            GriefPrevention.sendMessage(player, TextMode.Success, Messages.PetTransferCancellation);
            return CommandResult.success();
        }

        // find the specified player
        User targetPlayer = args.<User>getOne("player").get();

        // remember the player's ID for later pet transfer
        playerData.petGiveawayRecipient = targetPlayer;

        // send instructions
        GriefPrevention.sendMessage(player, TextMode.Instr, Messages.ReadyToTransferPet);

        return CommandResult.success();
    }
}
