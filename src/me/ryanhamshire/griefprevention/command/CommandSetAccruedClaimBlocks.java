/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) Ryan Hamshire
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

import me.ryanhamshire.griefprevention.CustomLogEntryTypes;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.TextMode;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.storage.WorldProperties;

public class CommandSetAccruedClaimBlocks implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        WorldProperties worldProperties = args.<WorldProperties> getOne("world").orElse(null);

        if (worldProperties == null) {
            if (src instanceof Player) {
                worldProperties = ((Player) src).getWorld().getProperties();
            } else {
                src.sendMessage(Text.of(TextColors.DARK_RED, "Error! ", TextColors.RED, "No valid world could be found!"));
                return CommandResult.success();
            }
        }

        // parse the adjustment amount
        int newAmount = args.<Integer>getOne("amount").get();
        User user = args.<User>getOne("user").get();

        // set player's blocks
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(worldProperties, user.getUniqueId());
        playerData.setAccruedClaimBlocks(newAmount);
        playerData.getStorageData().save();

        GriefPrevention.sendMessage(src, TextMode.Success, Messages.SetClaimBlocksSuccess);
        GriefPrevention.addLogEntry(src.getName() + " set " + user.getName() + "'s accrued claim blocks to " + newAmount + ".",
                CustomLogEntryTypes.AdminActivity);

        return CommandResult.success();
    }
}
