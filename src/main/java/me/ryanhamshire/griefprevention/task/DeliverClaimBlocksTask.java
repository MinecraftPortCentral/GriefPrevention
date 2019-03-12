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
package me.ryanhamshire.griefprevention.task;

import me.ryanhamshire.griefprevention.DataStore;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.configuration.PlayerStorageData;
import me.ryanhamshire.griefprevention.logging.CustomLogEntryTypes;
import me.ryanhamshire.griefprevention.permission.GPOptions;
import me.ryanhamshire.griefprevention.util.PermissionUtils;
import me.ryanhamshire.griefprevention.util.PlayerUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.manipulator.mutable.entity.VehicleData;
import org.spongepowered.api.data.property.block.MatterProperty;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Optional;

//FEATURE: give players claim blocks for playing, as long as they're not away from their computer

//runs every 5 minutes in the main thread, grants blocks per hour / 12 to each online player who appears to be actively playing
public class DeliverClaimBlocksTask implements Runnable {

    private Player player;

    public DeliverClaimBlocksTask(Player player) {
        this.player = player;
    }

    @Override
    public void run() {
        if (this.player == null) {
            for (World world : Sponge.getServer().getWorlds()) {
                // if no player specified, this task will create a player-specific task
                // for each player, scheduled one tick apart
                int i = 0;
                for (Entity entity : world.getEntities()) {
                    if (!(entity instanceof Player)) {
                        continue;
                    }

                    final Player player = (Player) entity;
                    final GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
                    final int accrualPerHour = PlayerUtils.getOptionIntValue(PermissionUtils.getActiveContexts(player, playerData, null), player, GPOptions.BLOCKS_ACCRUED_PER_HOUR, 120);
                    if (accrualPerHour > 0) {
                        DeliverClaimBlocksTask newTask = new DeliverClaimBlocksTask(player);
                        Sponge.getGame().getScheduler().createTaskBuilder().delayTicks(i++).execute(newTask)
                                .submit(GriefPreventionPlugin.instance);
                    }
                }
            }
        }

        // otherwise, deliver claim blocks to the specified player
        else {
            DataStore dataStore = GriefPreventionPlugin.instance.dataStore;
            GPPlayerData playerData = dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
            Location<World> lastLocation = playerData.lastAfkCheckLocation;
            // if he's not in a vehicle and has moved at least three blocks since the last check and he's not being pushed around by fluids
            Optional<MatterProperty> matterProperty = player.getLocation().getBlock().getProperty(MatterProperty.class);
            if (!player.get(VehicleData.class).isPresent() &&
                    (lastLocation == null || lastLocation.getPosition().distanceSquared(player.getLocation().getPosition()) >= 0) &&
                    matterProperty.isPresent() && matterProperty.get().getValue() != MatterProperty.Matter.LIQUID) {
                // add blocks
                int accruedBlocks = playerData.optionBlocksAccruedPerHour / 12;
                if (accruedBlocks < 0) {
                    accruedBlocks = 1;
                }

                int currentTotal = playerData.getAccruedClaimBlocks();
                if ((currentTotal + accruedBlocks) > playerData.optionMaxAccruedBlocks) {
                    PlayerStorageData playerStorage = playerData.getStorageData();
                    playerStorage.getConfig().setAccruedClaimBlocks(playerData.optionMaxAccruedBlocks);
                    playerData.lastAfkCheckLocation = player.getLocation();
                    return;
                }

                GriefPreventionPlugin.addLogEntry("Delivering " + accruedBlocks + " blocks to " + player.getName(), CustomLogEntryTypes.Debug, false);
                PlayerStorageData playerStorage = playerData.getStorageData();
                playerStorage.getConfig().setAccruedClaimBlocks(playerStorage.getConfig().getAccruedClaimBlocks() + accruedBlocks);
            } else {
                GriefPreventionPlugin.addLogEntry(player.getName() + " isn't active enough.", CustomLogEntryTypes.Debug, false);
            }

            // remember current location for next time
            playerData.lastAfkCheckLocation = player.getLocation();
        }
    }
}
