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

import com.google.common.collect.ImmutableMap;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.logging.CustomLogEntryTypes;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.storage.WorldProperties;

public class CommandAdjustBonusClaimBlocks implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) {
        WorldProperties worldProperties = args.<WorldProperties> getOne("world").orElse(Sponge.getServer().getDefaultWorld().get());

        if (worldProperties == null) {
            if (src instanceof Player) {
                worldProperties = ((Player) src).getWorld().getProperties();
            } else {
                worldProperties = Sponge.getServer().getDefaultWorld().get();
            }
        }
        if (worldProperties == null || !GriefPreventionPlugin.instance.claimsEnabledForWorld(worldProperties)) {
            GriefPreventionPlugin.sendMessage(src, GriefPreventionPlugin.instance.messageData.claimDisabledWorld.toText());
            return CommandResult.success();
        }

        // parse the adjustment amount
        int adjustment = args.<Integer>getOne("amount").get();
        User user = args.<User>getOne("user").get();

        // give blocks to player
        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(worldProperties, user.getUniqueId());
        playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + adjustment);
        playerData.getStorageData().save();
        final Text message = GriefPreventionPlugin.instance.messageData.adjustBlocksSuccess
                .apply(ImmutableMap.of(
                "player", Text.of(user.getName()),
                "adjustment", Text.of(adjustment),
                "total", Text.of(playerData.getBonusClaimBlocks()))).build();
        GriefPreventionPlugin
                .sendMessage(src, message);
        GriefPreventionPlugin.addLogEntry(
                src.getName() + " adjusted " + user.getName() + "'s bonus claim blocks by " + adjustment + ".",
                CustomLogEntryTypes.AdminActivity);

        return CommandResult.success();
    }
}
