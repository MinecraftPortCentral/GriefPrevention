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
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.biome.BiomeType;

import java.util.ArrayList;

//automatically extends a claim downward based on block types detected
public class AutoExtendClaimTask implements Runnable {

    private Claim claim;
    private ArrayList<Location<Chunk>> chunks;
    private DimensionType worldType;

    public AutoExtendClaimTask(Claim claim, ArrayList<Location<Chunk>> chunks, DimensionType worldType) {
        this.claim = claim;
        this.chunks = chunks;
        this.worldType = worldType;
    }

    @Override
    public void run() {
        int newY = this.getLowestBuiltY();
        if (newY < this.claim.getLesserBoundaryCorner().getBlockY()) {
            Sponge.getGame().getScheduler().createTaskBuilder().execute(new ExecuteExtendClaimTask(claim, newY))
                    .submit(GriefPrevention.instance);
        }
    }

    private int getLowestBuiltY() {
        int y = this.claim.getLesserBoundaryCorner().getBlockY();

        if (this.yTooSmall(y))
            return y;

        for (Location<Chunk> chunk : this.chunks) {
            BiomeType biome = chunk.getBiome();
            ArrayList<BlockType> playerBlocks = RestoreNatureProcessingTask.getPlayerBlocks(this.worldType, biome);

            boolean ychanged = true;
            while (!this.yTooSmall(y) && ychanged) {
                ychanged = false;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        BlockType blockType = chunk.add(0, y, 0).getBlockType();
                        while (!this.yTooSmall(y) && playerBlocks.contains(blockType)) {
                            ychanged = true;
                            blockType = chunk.add(0, --y, 0).getBlockType();
                        }

                        if (this.yTooSmall(y))
                            return y;
                    }
                }
            }

            if (this.yTooSmall(y))
                return y;
        }

        return y;
    }

    private boolean yTooSmall(int y) {
        return y == 0 || y <= GriefPrevention.getActiveConfig(this.claim.world.getProperties()).getConfig().claim.maxClaimDepth;
    }

    // runs in the main execution thread, where it can safely change claims and
    // save those changes
    private class ExecuteExtendClaimTask implements Runnable {

        private Claim claim;
        private int newY;

        public ExecuteExtendClaimTask(Claim claim, int newY) {
            this.claim = claim;
            this.newY = newY;
        }

        @Override
        public void run() {
            GriefPrevention.instance.dataStore.extendClaim(claim, newY);
        }
    }

}
