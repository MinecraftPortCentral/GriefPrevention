package me.ryanhamshire.griefprevention.command;

import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.claim.Claim;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public class CommandClaimBanItem implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        String itemToBan = "";
        Optional<String> itemid = ctx.<String>getOne("itemid");
        Optional<ItemStack> itemInHand = player.getItemInHand();
        if (!itemid.isPresent() && !itemInHand.isPresent()) {
            GriefPrevention.sendMessage(player, TextMode.Err, "No item id was specified and player is not holding an item.");
            return CommandResult.success();
        } else if (!itemid.isPresent()) {
            itemToBan = itemInHand.get().getItem().getId();
        } else {
            itemToBan = itemid.get();
        }

        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), false, null);
        if (claim != null) {
            List<String> bannedList = claim.getClaimData().getBannedItemList();
            if (!bannedList.contains(itemToBan)) {
                bannedList.add(itemToBan);
                GriefPrevention.getActiveConfig(player.getWorld().getProperties()).save();
                GriefPrevention.sendMessage(player, TextMode.Success, "Item " + itemToBan + " was successfully banned in claim.");
            } else {
                GriefPrevention.sendMessage(player, TextMode.Success, "Item " + itemToBan + " is already banned in claim.");
            }
        }

        return CommandResult.success();
    }
}
