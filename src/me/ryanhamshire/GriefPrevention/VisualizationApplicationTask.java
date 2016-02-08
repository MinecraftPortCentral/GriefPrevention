/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) Ryan Hamshire
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
package me.ryanhamshire.GriefPrevention;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;

import java.util.concurrent.TimeUnit;

//applies a visualization for a player by sending him block change packets
class VisualizationApplicationTask implements Runnable {

    private Visualization visualization;
    private Player player;
    private PlayerData playerData;

    public VisualizationApplicationTask(Player player, PlayerData playerData, Visualization visualization) {
        this.visualization = visualization;
        this.playerData = playerData;
        this.player = player;
    }

    @Override
    public void run() {
        // for each element (=block) of the visualization
        for (int i = 0; i < visualization.elements.size(); i++) {
            Transaction<BlockSnapshot> element = visualization.elements.get(i);

            // send the player a fake block change event
            if (!element.getFinal().getLocation().get().getExtent().isLoaded()) {
                continue; // cheap distance check
            }
            BlockUtils.sendBlockChange(player, element.getFinal());
        }

        // remember the visualization applied to this player for later (so it
        // can be inexpensively reverted)
        playerData.currentVisualization = visualization;

        // schedule automatic visualization reversion in 60 seconds.
        Sponge.getGame().getScheduler().createTaskBuilder().delay(1, TimeUnit.MINUTES)
                .execute(new VisualizationReversionTask(player, playerData, visualization)).submit(GriefPrevention.instance);
    }
}
