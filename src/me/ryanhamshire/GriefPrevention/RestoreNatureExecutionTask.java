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

import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.hanging.Hanging;
import org.spongepowered.api.entity.living.animal.Animal;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Optional;

//this main thread task takes the output from the RestoreNatureProcessingTask\
//and updates the world accordingly
class RestoreNatureExecutionTask implements Runnable {

    // results from processing thread
    // will be applied to the world
    private BlockSnapshot[][][] snapshots;

    // boundaries for changes
    private int miny;
    private Location<World> lesserCorner;
    private Location<World> greaterCorner;

    // player who should be notified about the result (will see a visualization
    // when the restoration is complete)
    private Player player;

    public RestoreNatureExecutionTask(BlockSnapshot[][][] snapshots, int miny, Location<World> lesserCorner, Location<World> greaterCorner,
            Player player) {
        this.snapshots = snapshots;
        this.miny = miny;
        this.lesserCorner = lesserCorner;
        this.greaterCorner = greaterCorner;
        this.player = player;
    }

    @Override
    public void run() {
        // apply changes to the world, but ONLY to unclaimed blocks
        // note that the edge of the results is not applied (the 1-block-wide
        // band around the outside of the chunk)
        // those data were sent to the processing thread for referernce
        // purposes, but aren't part of the area selected for restoration
        Claim cachedClaim = null;
        for (int x = 1; x < this.snapshots.length - 1; x++) {
            for (int z = 1; z < this.snapshots[0][0].length - 1; z++) {
                for (int y = this.miny; y < this.snapshots[0].length; y++) {
                    BlockSnapshot blockUpdate = this.snapshots[x][y][z];
                    BlockState currentBlock = blockUpdate.getLocation().get().getBlock();
                    if (blockUpdate.getState() != currentBlock) {
                        Claim claim = GriefPrevention.instance.dataStore.getClaimAt(blockUpdate.getLocation().get(), false, cachedClaim);
                        if (claim != null) {
                            cachedClaim = claim;
                            break;
                        }

                        blockUpdate.restore(true, false);
                    }
                }
            }
        }

        // clean up any entities in the chunk, ensure no players are suffocated
        Optional<Chunk> chunk = this.lesserCorner.getExtent().getChunk(this.lesserCorner.getBlockX() >> 4, 0, this.lesserCorner.getBlockZ() >> 4);
        if (chunk.isPresent()) {
            for (Entity entity : chunk.get().getEntities()) {
                if (!(entity instanceof Player || entity instanceof Animal)) {
                    // hanging entities (paintings, item frames) are protected when they're in land claims
                    if (!(entity instanceof Hanging) || GriefPrevention.instance.dataStore.getClaimAt(entity.getLocation(), false, null) == null) {
                        // everything else is removed
                        entity.remove();
                    }
                }
    
                // for players, always ensure there's air where the player is standing
                else {
                    entity.getLocation().setBlock(BlockTypes.AIR.getDefaultState());
                    entity.getLocation().getRelative(Direction.UP).setBlock(BlockTypes.AIR.getDefaultState());
                }
            }
        }

        // show visualization to player who started the restoration
        if (player != null) {
            Claim claim = new Claim(lesserCorner, greaterCorner, null, new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(),
                    new ArrayList<String>(), null);
            Visualization visualization =
                    Visualization.FromClaim(claim, player.getLocation().getBlockY(), VisualizationType.RestoreNature, player.getLocation());
            Visualization.Apply(player, visualization);
        }
    }
}
