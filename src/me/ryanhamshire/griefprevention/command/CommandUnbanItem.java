package me.ryanhamshire.griefprevention.command;

import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.TextMode;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Optional;

public class CommandUnbanItem implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        String itemToUnban = "";
        Optional<String> itemid = ctx.<String>getOne("itemid");
        Optional<ItemStack> itemInHand = player.getItemInHand();
        if (!itemid.isPresent() && !itemInHand.isPresent()) {
            GriefPrevention.sendMessage(player, TextMode.Err, "No item id was specified and player is not holding an item.");
            return CommandResult.success();
        } else if (!itemid.isPresent()) {
            itemToUnban = itemInHand.get().getItem().getId();
        } else {
            itemToUnban = itemid.get();
        }

        ArrayList<String> bannedList = GriefPrevention.getActiveConfig(player.getWorld().getProperties()).getConfig().general.bannedItemList;
        if (bannedList.contains(itemToUnban)) {
            bannedList.remove(itemToUnban);
            GriefPrevention.getActiveConfig(player.getWorld().getProperties()).save();
            GriefPrevention.sendMessage(player, TextMode.Success, "Item " + itemToUnban + " was successfully unbanned.");
        } else {
            GriefPrevention.sendMessage(player, TextMode.Success, "Item " + itemToUnban + " is not banned.");
        }

        return CommandResult.success();
    }
}
