/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package me.ryanhamshire.griefprevention.command;

import me.ryanhamshire.griefprevention.GPFlags;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.claim.Claim;
import net.minecraft.item.ItemBlock;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.data.type.HandTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.util.Tristate;

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
        Optional<ItemType> item = ctx.<ItemType>getOne("item");
        Optional<ItemStack> itemInHand = player.getItemInHand(HandTypes.MAIN_HAND);
        if (!item.isPresent() && !itemInHand.isPresent()) {
            GriefPrevention.sendMessage(player, TextMode.Err, "No item id was specified and player is not holding an item.");
            return CommandResult.success();
        } else if (!item.isPresent()) {
            ItemStack itemstack = itemInHand.get();
            if (itemstack.getItem() instanceof ItemBlock) {
                ItemBlock itemBlock = (ItemBlock) itemstack.getItem();
                net.minecraft.item.ItemStack nmsStack = (net.minecraft.item.ItemStack)(Object) itemstack;
                BlockState blockState = ((BlockState) itemBlock.getBlock().getStateFromMeta(nmsStack.getItemDamage()));
                itemToBan = blockState.getId();
            } else {
                itemToBan = itemstack.getItem().getId();
            }
        } else {
            itemToBan = item.get().getId();
        }

        Claim claim = GriefPrevention.instance.dataStore.getClaimAtPlayer(player, false);
        CommandHelper.addFlagPermission(player, GriefPrevention.GLOBAL_SUBJECT, "ALL", claim, GPFlags.INTERACT_ITEM_PRIMARY, "any", itemToBan, Tristate.FALSE, null);
        CommandHelper.addFlagPermission(player, GriefPrevention.GLOBAL_SUBJECT, "ALL", claim, GPFlags.INTERACT_ITEM_SECONDARY, "any", itemToBan, Tristate.FALSE, null);

        return CommandResult.success();
    }
}
