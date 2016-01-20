package me.ryanhamshire.GriefPrevention.command;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;

public class CommandGpBlockInfo implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;

        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();

        }
        try {
            throw new CommandException(Text.of("Information about block-in-hand must be redesigned for Sponge"));
            // TODO: Handle sponge's w
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        // worldview
        /*
         * ItemStack inHand = player.getItemInHand().orElse(null);
         * player.sendMessage("In Hand: " + String.format("%s(%d:%d)",
         * inHand.getType().name(), inHand.getTypeId(),
         * inHand.getData().getData())); Block inWorld =
         * GriefPrevention.getTargetNonAirBlock(player, 300);
         * player.sendMessage("In World: " + String.format("%s(%d:%d)",
         * inWorld.getType().name(), inWorld.getTypeId(), inWorld.getData()));
         * return CommandResult.success();
         */
    }
}
