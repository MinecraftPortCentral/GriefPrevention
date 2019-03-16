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

import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.visual.Visualization;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.entity.living.player.Player;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

//applies a visualization for a player by sending him block change packets
public class VisualizationApplicationTask implements Runnable {

    private Visualization visualization;
    private Player player;
    private GPPlayerData playerData;
    private boolean resetActive;

    public VisualizationApplicationTask(Player player, GPPlayerData playerData, Visualization visualization) {
        this(player, playerData, visualization, true);
    }

    public VisualizationApplicationTask(Player player, GPPlayerData playerData, Visualization visualization, boolean resetActive) {
        this.visualization = visualization;
        this.playerData = playerData;
        this.player = player;
        this.resetActive = resetActive;
    }

    @Override
    public void run() {
        if (this.playerData.visualBlocks != null) {
            if (this.resetActive) {
                this.playerData.revertActiveVisual(this.player);
            }
        }

        for (int i = 0; i < this.visualization.elements.size(); i++) {
            BlockSnapshot snapshot = this.visualization.elements.get(i).getFinal();
            this.player.sendBlockChange(snapshot.getPosition(), snapshot.getState());
        }

        // remember the visualization applied to this player for later (so it can be inexpensively reverted)
        if (this.visualization.getClaim() != null) {
            this.playerData.visualClaimId = this.visualization.getClaim().id;
            this.visualization.getClaim().playersWatching.add(this.player.getUniqueId());
        }
        if (this.playerData.visualBlocks == null) {
            this.playerData.visualBlocks = new ArrayList<>(this.visualization.elements);
        } else {
            this.playerData.visualBlocks.addAll(this.visualization.elements);
        }

        if (this.playerData.visualRevertTask != null) {
            this.playerData.visualRevertTask.cancel();
            this.playerData.visualRevertTask = Sponge.getGame().getScheduler().createTaskBuilder().async().delay(1, TimeUnit.MINUTES)
                    .execute(new VisualizationReversionTask(this.player, this.playerData)).submit(GriefPreventionPlugin.instance);
        } else {
            // schedule automatic visualization reversion in 60 seconds.
            // only create revert task if not resizing/starting a claim
            if (playerData.lastShovelLocation == null) {
                this.playerData.visualRevertTask = Sponge.getGame().getScheduler().createTaskBuilder().async().delay(1, TimeUnit.MINUTES)
                        .execute(new VisualizationReversionTask(this.player, this.playerData)).submit(GriefPreventionPlugin.instance);
            }
        }
    }
}
