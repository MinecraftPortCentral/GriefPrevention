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

import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.util.BlockUtils;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.trait.EnumTraits;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.biome.BiomeTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//non-main-thread task which processes world data to repair the unnatural
//after processing is complete, creates a main thread task to make the necessary changes to the world
public class RestoreNatureProcessingTask implements Runnable {

    // world information captured from the main thread
    // will be updated and sent back to main thread to be applied to the world
    private BlockSnapshot[][][] snapshots;

    // other information collected from the main thread.
    // not to be updated, only to be passed back to main thread to provide some
    // context about the operation
    private int miny;
    private DimensionType environment;
    private Location<World> lesserBoundaryCorner;
    private Location<World> greaterBoundaryCorner;
    // absolutely must not be accessed. not thread safe.
    private Player player;
    private BiomeType biome;
    private boolean creativeMode;
    private int seaLevel;
    private boolean aggressiveMode;

    // two lists of materials
    // natural blocks which don't naturally hang in their air
    private ArrayList<BlockType> notAllowedToHang;

    // a "complete" list of player-placed blocks. MUST BE MAINTAINED as patches introduce more
    private ArrayList<BlockType> playerBlocks;

    public RestoreNatureProcessingTask(BlockSnapshot[][][] snapshots, int miny, DimensionType environment, BiomeType biome,
            Location<World> lesserBoundaryCorner, Location<World> greaterBoundaryCorner, int seaLevel, boolean aggressiveMode, boolean creativeMode,
            Player player) {
        this.snapshots = snapshots;
        this.miny = miny;
        if (this.miny < 0) {
            this.miny = 0;
        }
        this.environment = environment;
        this.lesserBoundaryCorner = lesserBoundaryCorner;
        this.greaterBoundaryCorner = greaterBoundaryCorner;
        this.biome = biome;
        this.seaLevel = seaLevel;
        this.aggressiveMode = aggressiveMode;
        this.player = player;
        this.creativeMode = creativeMode;

        this.notAllowedToHang = new ArrayList<BlockType>();
        this.notAllowedToHang.add(BlockTypes.DIRT);
        this.notAllowedToHang.add(BlockTypes.TALLGRASS);
        this.notAllowedToHang.add(BlockTypes.SNOW);
        this.notAllowedToHang.add(BlockTypes.LOG);

        if (this.aggressiveMode) {
            this.notAllowedToHang.add(BlockTypes.GRASS);
            this.notAllowedToHang.add(BlockTypes.STONE);
        }

        this.playerBlocks = new ArrayList<BlockType>();
        this.playerBlocks.addAll(RestoreNatureProcessingTask.getPlayerBlocks(this.environment, this.biome));

        // in aggressive or creative world mode, also treat these blocks as user placed, to be removed
        // this is helpful in the few cases where griefers intentionally use natural blocks to grief,
        // like a single-block tower of iron ore or a giant penis constructed with melons
        if (this.aggressiveMode || this.creativeMode) {
            this.playerBlocks.add(BlockTypes.IRON_ORE);
            this.playerBlocks.add(BlockTypes.GOLD_ORE);
            this.playerBlocks.add(BlockTypes.DIAMOND_ORE);
            this.playerBlocks.add(BlockTypes.MELON_BLOCK);
            this.playerBlocks.add(BlockTypes.MELON_STEM);
            this.playerBlocks.add(BlockTypes.BEDROCK);
            this.playerBlocks.add(BlockTypes.COAL_ORE);
            this.playerBlocks.add(BlockTypes.PUMPKIN);
            this.playerBlocks.add(BlockTypes.PUMPKIN_STEM);
        }

        if (this.aggressiveMode) {
            this.playerBlocks.add(BlockTypes.LEAVES);
            this.playerBlocks.add(BlockTypes.LOG);
            this.playerBlocks.add(BlockTypes.LOG2);
            this.playerBlocks.add(BlockTypes.VINE);
        }
    }

    @Override
    public void run() {
        // order is important!

        // remove sandstone which appears to be unnatural
        this.removeSandstone();

        // remove any blocks which are definitely player placed
        this.removePlayerBlocks();

        // reduce large outcroppings of stone, sandstone
        this.reduceStone();

        // reduce logs, except in jungle biomes
        this.reduceLogs();

        // remove natural blocks which are unnaturally hanging in the air
        this.removeHanging();

        // remove natural blocks which are unnaturally stacked high
        this.removeWallsAndTowers();

        // fill unnatural thin trenches and single-block potholes
        this.fillHolesAndTrenches();

        // fill water depressions and fix unnatural surface ripples
        this.fixWater();

        // remove water/lava above sea level
        this.removeDumpedFluids();

        // cover surface stone and gravel with sand or grass, as the biome requires
        this.coverSurfaceStone();

        // remove any player-placed leaves
        this.removePlayerLeaves();

        // schedule main thread task to apply the result to the world
        RestoreNatureExecutionTask task =
                new RestoreNatureExecutionTask(this.snapshots, this.miny, this.lesserBoundaryCorner, this.greaterBoundaryCorner, this.player);
        Sponge.getGame().getScheduler().createTaskBuilder().execute(task).submit(GriefPreventionPlugin.instance);
    }

    private void removePlayerLeaves() {
        if (this.seaLevel < 1) {
            return;
        }

        for (int x = 1; x < snapshots.length - 1; x++) {
            for (int z = 1; z < snapshots[0][0].length - 1; z++) {
                for (int y = this.seaLevel - 1; y < snapshots[0].length; y++) {
                    // note: see minecraft wiki data values for leaves
                    BlockSnapshot block = snapshots[x][y][z];
                    if (block.getState().getType() == BlockTypes.LEAVES && (BlockUtils.getBlockStateMeta(block.getState()) & 0x4) != 0) {
                        snapshots[x][y][z] = block.withState(BlockTypes.AIR.getDefaultState());
                    }
                }
            }
        }
    }

    // converts sandstone adjacent to sand to sand, and any other sandstone to
    // air
    private void removeSandstone() {
        for (int x = 1; x < snapshots.length - 1; x++) {
            for (int z = 1; z < snapshots[0][0].length - 1; z++) {
                for (int y = snapshots[0].length - 2; y > miny; y--) {
                    if (snapshots[x][y][z].getState().getType() != BlockTypes.SANDSTONE) {
                        continue;
                    }

                    BlockSnapshot leftBlock = this.snapshots[x + 1][y][z];
                    BlockSnapshot rightBlock = this.snapshots[x - 1][y][z];
                    BlockSnapshot upBlock = this.snapshots[x][y][z + 1];
                    BlockSnapshot downBlock = this.snapshots[x][y][z - 1];
                    BlockSnapshot underBlock = this.snapshots[x][y - 1][z];
                    BlockSnapshot aboveBlock = this.snapshots[x][y + 1][z];

                    // skip blocks which may cause a cave-in
                    if (aboveBlock.getState().getType() == BlockTypes.SAND && underBlock.getState().getType() == BlockTypes.AIR) {
                        continue;
                    }

                    // count adjacent non-air/non-leaf blocks
                    if (leftBlock.getState().getType() == BlockTypes.SAND ||
                            rightBlock.getState().getType() == BlockTypes.SAND ||
                            upBlock.getState().getType() == BlockTypes.SAND ||
                            downBlock.getState().getType() == BlockTypes.SAND ||
                            aboveBlock.getState().getType() == BlockTypes.SAND ||
                            underBlock.getState().getType() == BlockTypes.SAND) {
                        snapshots[x][y][z] = snapshots[x][y][z].withState(BlockTypes.SAND.getDefaultState());
                    } else {
                        snapshots[x][y][z] = snapshots[x][y][z].withState(BlockTypes.AIR.getDefaultState());
                    }
                }
            }
        }
    }

    private void reduceStone() {
        if (this.seaLevel < 1) {
            return;
        }

        for (int x = 1; x < snapshots.length - 1; x++) {
            for (int z = 1; z < snapshots[0][0].length - 1; z++) {
                int thisy = this.highestY(x, z, true);

                while (thisy > this.seaLevel - 1 && (this.snapshots[x][thisy][z].getState().getType() == BlockTypes.STONE
                        || this.snapshots[x][thisy][z].getState().getType() == BlockTypes.SANDSTONE)) {
                    BlockSnapshot leftBlock = this.snapshots[x + 1][thisy][z];
                    BlockSnapshot rightBlock = this.snapshots[x - 1][thisy][z];
                    BlockSnapshot upBlock = this.snapshots[x][thisy][z + 1];
                    BlockSnapshot downBlock = this.snapshots[x][thisy][z - 1];

                    // count adjacent non-air/non-leaf blocks
                    byte adjacentBlockCount = 0;
                    if (leftBlock.getState().getType() != BlockTypes.AIR && leftBlock.getState().getType() != BlockTypes.LEAVES
                            && leftBlock.getState().getType() != BlockTypes.VINE) {
                        adjacentBlockCount++;
                    }
                    if (rightBlock.getState().getType() != BlockTypes.AIR && rightBlock.getState().getType() != BlockTypes.LEAVES
                            && rightBlock.getState().getType() != BlockTypes.VINE) {
                        adjacentBlockCount++;
                    }
                    if (downBlock.getState().getType() != BlockTypes.AIR && downBlock.getState().getType() != BlockTypes.LEAVES
                            && downBlock.getState().getType() != BlockTypes.VINE) {
                        adjacentBlockCount++;
                    }
                    if (upBlock.getState().getType() != BlockTypes.AIR && upBlock.getState().getType() != BlockTypes.LEAVES
                            && upBlock.getState().getType() != BlockTypes.VINE) {
                        adjacentBlockCount++;
                    }

                    if (adjacentBlockCount < 3) {
                        this.snapshots[x][thisy][z] = this.snapshots[x][thisy][z].withState(BlockTypes.AIR.getDefaultState());
                    }

                    thisy--;
                }
            }
        }
    }

    private void reduceLogs() {
        if (this.seaLevel < 1) {
            return;
        }

        boolean jungleBiome = this.biome == BiomeTypes.JUNGLE || this.biome == BiomeTypes.JUNGLE_HILLS;

        // scan all blocks above sea level
        for (int x = 1; x < snapshots.length - 1; x++) {
            for (int z = 1; z < snapshots[0][0].length - 1; z++) {
                for (int y = this.seaLevel - 1; y < snapshots[0].length; y++) {
                    BlockSnapshot block = snapshots[x][y][z];

                    // skip non-logs
                    if (block.getState().getType() != BlockTypes.LOG) {
                        continue;
                    }
                    if (block.getState().getType() != BlockTypes.LOG2) {
                        continue;
                    }

                    // if in jungle biome, skip jungle logs
                    Optional<? extends Enum<?>> enumProperty = block.getState().getTraitValue(EnumTraits.LOG_VARIANT);
                    if (jungleBiome && enumProperty.isPresent() && enumProperty.get().name().equalsIgnoreCase("jungle")) {
                        continue;
                    }

                    // examine adjacent blocks for logs
                    BlockSnapshot leftBlock = this.snapshots[x + 1][y][z];
                    BlockSnapshot rightBlock = this.snapshots[x - 1][y][z];
                    BlockSnapshot upBlock = this.snapshots[x][y][z + 1];
                    BlockSnapshot downBlock = this.snapshots[x][y][z - 1];

                    // if any, remove the log
                    if (leftBlock.getState().getType() == BlockTypes.LOG || rightBlock.getState().getType() == BlockTypes.LOG
                            || upBlock.getState().getType() == BlockTypes.LOG || downBlock.getState().getType() == BlockTypes.LOG) {
                        this.snapshots[x][y][z] = this.snapshots[x][y][z].withState(BlockTypes.AIR.getDefaultState());
                    }
                }
            }
        }
    }

    private void removePlayerBlocks() {
        int miny = this.miny;
        if (miny < 1) {
            miny = 1;
        }

        // remove all player blocks
        for (int x = 1; x < snapshots.length - 1; x++) {
            for (int z = 1; z < snapshots[0][0].length - 1; z++) {
                for (int y = miny; y < snapshots[0].length - 1; y++) {
                    BlockSnapshot block = snapshots[x][y][z];
                    if (this.playerBlocks.contains(block.getState().getType())) {
                        snapshots[x][y][z] = block.withState(BlockTypes.AIR.getDefaultState());
                    }
                }
            }
        }
    }

    private void removeHanging() {
        int miny = this.miny;
        if (miny < 1) {
            miny = 1;
        }

        for (int x = 1; x < snapshots.length - 1; x++) {
            for (int z = 1; z < snapshots[0][0].length - 1; z++) {
                for (int y = miny; y < snapshots[0].length - 1; y++) {
                    BlockSnapshot block = snapshots[x][y][z];
                    BlockSnapshot underBlock = snapshots[x][y - 1][z];

                    if (underBlock.getState().getType() == BlockTypes.AIR || underBlock.getState().getType() == BlockTypes.WATER
                            || underBlock.getState().getType() == BlockTypes.LAVA || underBlock.getState().getType() == BlockTypes.LEAVES) {
                        if (this.notAllowedToHang.contains(block.getState().getType())) {
                            snapshots[x][y][z] = block.withState(BlockTypes.AIR.getDefaultState());
                        }
                    }
                }
            }
        }
    }

    private void removeWallsAndTowers() {
        List<BlockType> excludedBlocksArray = new ArrayList<BlockType>();
        excludedBlocksArray.add(BlockTypes.CACTUS);
        excludedBlocksArray.add(BlockTypes.TALLGRASS);
        excludedBlocksArray.add(BlockTypes.RED_MUSHROOM);
        excludedBlocksArray.add(BlockTypes.BROWN_MUSHROOM);
        excludedBlocksArray.add(BlockTypes.DEADBUSH);
        excludedBlocksArray.add(BlockTypes.SAPLING);
        excludedBlocksArray.add(BlockTypes.YELLOW_FLOWER);
        excludedBlocksArray.add(BlockTypes.RED_FLOWER);
        excludedBlocksArray.add(BlockTypes.REEDS);
        excludedBlocksArray.add(BlockTypes.VINE);
        excludedBlocksArray.add(BlockTypes.PUMPKIN);
        excludedBlocksArray.add(BlockTypes.WATERLILY);
        excludedBlocksArray.add(BlockTypes.LEAVES);

        boolean changed;
        do {
            changed = false;
            for (int x = 1; x < snapshots.length - 1; x++) {
                for (int z = 1; z < snapshots[0][0].length - 1; z++) {
                    int thisy = this.highestY(x, z, false);
                    if (excludedBlocksArray.contains(this.snapshots[x][thisy][z].getState().getType())) {
                        continue;
                    }

                    int righty = this.highestY(x + 1, z, false);
                    int lefty = this.highestY(x - 1, z, false);
                    while (lefty < thisy && righty < thisy) {
                        this.snapshots[x][thisy--][z] = this.snapshots[x][thisy--][z].withState(BlockTypes.AIR.getDefaultState());
                        changed = true;
                    }

                    int upy = this.highestY(x, z + 1, false);
                    int downy = this.highestY(x, z - 1, false);
                    while (upy < thisy && downy < thisy) {
                        this.snapshots[x][thisy--][z] = this.snapshots[x][thisy--][z].withState(BlockTypes.AIR.getDefaultState());
                        changed = true;
                    }
                }
            }
        } while (changed);
    }

    private void coverSurfaceStone() {
        for (int x = 1; x < snapshots.length - 1; x++) {
            for (int z = 1; z < snapshots[0][0].length - 1; z++) {
                int y = this.highestY(x, z, true);
                BlockSnapshot block = snapshots[x][y][z];

                if (block.getState().getType() == BlockTypes.STONE || block.getState().getType() == BlockTypes.GRAVEL
                        || block.getState().getType() == BlockTypes.FARMLAND
                        || block.getState().getType() == BlockTypes.DIRT || block.getState().getType() == BlockTypes.SANDSTONE) {
                    if (this.biome == BiomeTypes.DESERT || this.biome == BiomeTypes.DESERT_HILLS || this.biome == BiomeTypes.BEACH) {
                        this.snapshots[x][y][z] = this.snapshots[x][y][z].withState(BlockTypes.SAND.getDefaultState());
                    } else {
                        this.snapshots[x][y][z] = this.snapshots[x][y][z].withState(BlockTypes.GRASS.getDefaultState());
                    }
                }
            }
        }
    }

    private void fillHolesAndTrenches() {
        ArrayList<BlockType> fillableBlocks = new ArrayList<BlockType>();
        fillableBlocks.add(BlockTypes.AIR);
        fillableBlocks.add(BlockTypes.WATER);
        fillableBlocks.add(BlockTypes.LAVA);
        fillableBlocks.add(BlockTypes.TALLGRASS);

        ArrayList<BlockType> notSuitableForFillBlocks = new ArrayList<BlockType>();
        notSuitableForFillBlocks.add(BlockTypes.TALLGRASS);
        notSuitableForFillBlocks.add(BlockTypes.CACTUS);
        notSuitableForFillBlocks.add(BlockTypes.WATER);
        notSuitableForFillBlocks.add(BlockTypes.LAVA);
        notSuitableForFillBlocks.add(BlockTypes.LOG);
        notSuitableForFillBlocks.add(BlockTypes.LOG2);

        boolean changed;
        do {
            changed = false;
            for (int x = 1; x < snapshots.length - 1; x++) {
                for (int z = 1; z < snapshots[0][0].length - 1; z++) {
                    for (int y = 0; y < snapshots[0].length - 1; y++) {
                        BlockSnapshot block = this.snapshots[x][y][z];
                        if (!fillableBlocks.contains(block.getState().getType())) {
                            continue;
                        }

                        BlockSnapshot leftBlock = this.snapshots[x + 1][y][z];
                        BlockSnapshot rightBlock = this.snapshots[x - 1][y][z];

                        if (!fillableBlocks.contains(leftBlock.getState().getType()) && !fillableBlocks.contains(rightBlock.getState().getType())) {
                            if (!notSuitableForFillBlocks.contains(rightBlock.getState().getType())) {
                                this.snapshots[x][y][z] = block.withState(rightBlock.getState().getType().getDefaultState());
                                changed = true;
                            }
                        }

                        BlockSnapshot upBlock = this.snapshots[x][y][z + 1];
                        BlockSnapshot downBlock = this.snapshots[x][y][z - 1];

                        if (!fillableBlocks.contains(upBlock.getState().getType()) && !fillableBlocks.contains(downBlock.getState().getType())) {
                            if (!notSuitableForFillBlocks.contains(downBlock.getState().getType())) {
                                this.snapshots[x][y][z] = block.withState(downBlock.getState().getType().getDefaultState());
                                changed = true;
                            }
                        }
                    }
                }
            }
        } while (changed);
    }

    private void fixWater() {
        int miny = this.miny;
        if (miny < 1) {
            miny = 1;
        }

        boolean changed;

        // remove hanging water or lava
        for (int x = 1; x < snapshots.length - 1; x++) {
            for (int z = 1; z < snapshots[0][0].length - 1; z++) {
                for (int y = miny; y < snapshots[0].length - 1; y++) {
                    BlockSnapshot block = this.snapshots[x][y][z];
                    BlockSnapshot underBlock = this.snapshots[x][y - 1][z];
                    if (block.getState().getType() == BlockTypes.WATER || block.getState().getType() == BlockTypes.LAVA) {
                        if (underBlock.getState().getType() == BlockTypes.AIR || (((IBlockState) underBlock.getState()).getBlock().getMetaFromState((IBlockState) underBlock.getState()) != 0)) {
                            this.snapshots[x][y][z] = block.withState(BlockTypes.AIR.getDefaultState());
                        }
                    }
                }
            }
        }

        // fill water depressions
        do {
            changed = false;
            for (int y = Math.max(this.seaLevel - 10, 0); y <= this.seaLevel; y++) {
                for (int x = 1; x < snapshots.length - 1; x++) {
                    for (int z = 1; z < snapshots[0][0].length - 1; z++) {
                        BlockSnapshot block = snapshots[x][y][z];

                        // only consider air blocks and flowing water blocks for upgrade to water source blocks
                        if (block.getState().getType() == BlockTypes.AIR || (block.getState().getType() == BlockTypes.WATER
                                && (((IBlockState) block.getState()).getBlock().getMetaFromState((IBlockState) block.getState())) != 0)) {
                            BlockSnapshot leftBlock = this.snapshots[x + 1][y][z];
                            BlockSnapshot rightBlock = this.snapshots[x - 1][y][z];
                            BlockSnapshot upBlock = this.snapshots[x][y][z + 1];
                            BlockSnapshot downBlock = this.snapshots[x][y][z - 1];
                            BlockSnapshot underBlock = this.snapshots[x][y - 1][z];

                            // block underneath MUST be source water
                            if (underBlock.getState().getType() != BlockTypes.WATER
                                    || (((IBlockState) underBlock.getState()).getBlock().getMetaFromState((IBlockState) underBlock.getState())) != 0) {
                                continue;
                            }

                            // count adjacent source water blocks
                            byte adjacentSourceWaterCount = 0;
                            if (leftBlock.getState().getType() == BlockTypes.WATER
                                    && (((IBlockState) leftBlock.getState()).getBlock().getMetaFromState((IBlockState) leftBlock.getState())) == 0) {
                                adjacentSourceWaterCount++;
                            }
                            if (rightBlock.getState().getType() == BlockTypes.WATER
                                    && (((IBlockState) rightBlock.getState()).getBlock().getMetaFromState((IBlockState) rightBlock.getState())) == 0) {
                                adjacentSourceWaterCount++;
                            }
                            if (upBlock.getState().getType() == BlockTypes.WATER && (((IBlockState) upBlock.getState()).getBlock().getMetaFromState((IBlockState) upBlock.getState())) == 0) {
                                adjacentSourceWaterCount++;
                            }
                            if (downBlock.getState().getType() == BlockTypes.WATER
                                    && (((IBlockState) downBlock.getState()).getBlock().getMetaFromState((IBlockState) downBlock.getState())) == 0) {
                                adjacentSourceWaterCount++;
                            }

                            // at least two adjacent blocks must be source water
                            if (adjacentSourceWaterCount >= 2) {
                                snapshots[x][y][z] = block.withState(BlockTypes.WATER.getDefaultState());
                                changed = true;
                            }
                        }
                    }
                }
            }
        } while (changed);
    }

    private void removeDumpedFluids() {
        if (this.seaLevel < 1) {
            return;
        }

        // remove any surface water or lava above sea level, presumed to be
        // placed by players
        // sometimes, this is naturally generated. but replacing it is very easy
        // with a bucket, so overall this is a good plan
        if (this.environment.equals(DimensionTypes.NETHER)) {
            return;
        }
        for (int x = 1; x < snapshots.length - 1; x++) {
            for (int z = 1; z < snapshots[0][0].length - 1; z++) {
                for (int y = this.seaLevel - 1; y < snapshots[0].length - 1; y++) {
                    BlockSnapshot block = snapshots[x][y][z];
                    if (block.getState().getType() == BlockTypes.WATER || block.getState().getType() == BlockTypes.LAVA ||
                            block.getState().getType() == BlockTypes.WATER || block.getState().getType() == BlockTypes.LAVA) {
                        snapshots[x][y][z] = block.withState(BlockTypes.AIR.getDefaultState());
                    }
                }
            }
        }
    }

    private int highestY(int x, int z, boolean ignoreLeaves) {
        int y;
        for (y = snapshots[0].length - 1; y > 0; y--) {
            BlockSnapshot block = this.snapshots[x][y][z];
            if (block.getState().getType() != BlockTypes.AIR &&
                    !(ignoreLeaves && block.getState().getType() == BlockTypes.SNOW) &&
                    !(ignoreLeaves && block.getState().getType() == BlockTypes.LEAVES) &&
                    !(block.getState().getType() == BlockTypes.WATER) &&
                    !(block.getState().getType() == BlockTypes.FLOWING_WATER) &&
                    !(block.getState().getType() == BlockTypes.LAVA) &&
                    !(block.getState().getType() == BlockTypes.FLOWING_LAVA)) {
                return y;
            }
        }

        return y;
    }

    public static ArrayList<BlockType> getPlayerBlocks(DimensionType environment, BiomeType biome) {
        // NOTE on this list. why not make a list of natural blocks?
        // answer: better to leave a few player blocks than to remove too many
        // natural blocks. remember we're "restoring nature"
        // a few extra player blocks can be manually removed, but it will be
        // impossible to guess exactly which natural materials to use in manual
        // repair of an overzealous block removal

        // TODO : add mod support
        ArrayList<BlockType> playerBlocks = new ArrayList<BlockType>();

        playerBlocks.add(BlockTypes.FIRE);
        playerBlocks.add(BlockTypes.BED);
        playerBlocks.add(BlockTypes.PLANKS);
        playerBlocks.add(BlockTypes.BOOKSHELF);
        playerBlocks.add(BlockTypes.BREWING_STAND);
        playerBlocks.add(BlockTypes.BRICK_BLOCK);
        playerBlocks.add(BlockTypes.COBBLESTONE);
        playerBlocks.add(BlockTypes.GLASS);
        playerBlocks.add(BlockTypes.LAPIS_BLOCK);
        playerBlocks.add(BlockTypes.DISPENSER);
        playerBlocks.add(BlockTypes.NOTEBLOCK);
        playerBlocks.add(BlockTypes.RAIL);
        playerBlocks.add(BlockTypes.DETECTOR_RAIL);
        playerBlocks.add(BlockTypes.STICKY_PISTON);
        playerBlocks.add(BlockTypes.PISTON);
        playerBlocks.add(BlockTypes.PISTON_EXTENSION);
        playerBlocks.add(BlockTypes.WOOL);
        playerBlocks.add(BlockTypes.PISTON_HEAD);
        playerBlocks.add(BlockTypes.GOLD_BLOCK);
        playerBlocks.add(BlockTypes.IRON_BLOCK);
        playerBlocks.add(BlockTypes.DOUBLE_STONE_SLAB);
        playerBlocks.add(BlockTypes.STONE_SLAB);
        playerBlocks.add(BlockTypes.WHEAT);
        playerBlocks.add(BlockTypes.TNT);
        playerBlocks.add(BlockTypes.MOSSY_COBBLESTONE);
        playerBlocks.add(BlockTypes.TORCH);
        playerBlocks.add(BlockTypes.FIRE);
        playerBlocks.add(BlockTypes.OAK_STAIRS);
        playerBlocks.add(BlockTypes.CHEST);
        playerBlocks.add(BlockTypes.REDSTONE_WIRE);
        playerBlocks.add(BlockTypes.DIAMOND_BLOCK);
        playerBlocks.add(BlockTypes.CRAFTING_TABLE);
        playerBlocks.add(BlockTypes.FURNACE);
        playerBlocks.add(BlockTypes.LIT_FURNACE);
        playerBlocks.add(BlockTypes.WOODEN_DOOR);
        playerBlocks.add(BlockTypes.STANDING_SIGN);
        playerBlocks.add(BlockTypes.LADDER);
        playerBlocks.add(BlockTypes.RAIL);
        playerBlocks.add(BlockTypes.STONE_STAIRS);
        playerBlocks.add(BlockTypes.WALL_SIGN);
        playerBlocks.add(BlockTypes.STONE_PRESSURE_PLATE);
        playerBlocks.add(BlockTypes.LEVER);
        playerBlocks.add(BlockTypes.IRON_DOOR);
        playerBlocks.add(BlockTypes.WOODEN_PRESSURE_PLATE);
        playerBlocks.add(BlockTypes.REDSTONE_TORCH);
        playerBlocks.add(BlockTypes.UNLIT_REDSTONE_TORCH);
        playerBlocks.add(BlockTypes.STONE_BUTTON);
        playerBlocks.add(BlockTypes.SNOW);
        playerBlocks.add(BlockTypes.JUKEBOX);
        playerBlocks.add(BlockTypes.FENCE);
        playerBlocks.add(BlockTypes.PORTAL);
        playerBlocks.add(BlockTypes.LIT_PUMPKIN);
        playerBlocks.add(BlockTypes.CAKE);
        playerBlocks.add(BlockTypes.UNPOWERED_REPEATER);
        playerBlocks.add(BlockTypes.POWERED_REPEATER);
        playerBlocks.add(BlockTypes.TRAPDOOR);
        playerBlocks.add(BlockTypes.STONEBRICK);
        playerBlocks.add(BlockTypes.BROWN_MUSHROOM_BLOCK);
        playerBlocks.add(BlockTypes.RED_MUSHROOM_BLOCK);
        playerBlocks.add(BlockTypes.IRON_BARS);
        playerBlocks.add(BlockTypes.GLASS_PANE);
        playerBlocks.add(BlockTypes.MELON_STEM);
        playerBlocks.add(BlockTypes.FENCE_GATE);
        playerBlocks.add(BlockTypes.BRICK_STAIRS);
        playerBlocks.add(BlockTypes.STONE_BRICK_STAIRS);
        playerBlocks.add(BlockTypes.ENCHANTING_TABLE);
        playerBlocks.add(BlockTypes.BREWING_STAND);
        playerBlocks.add(BlockTypes.CAULDRON);
        playerBlocks.add(BlockTypes.WEB);
        playerBlocks.add(BlockTypes.SPONGE);
        playerBlocks.add(BlockTypes.GRAVEL);
        playerBlocks.add(BlockTypes.EMERALD_BLOCK);
        playerBlocks.add(BlockTypes.SANDSTONE);
        playerBlocks.add(BlockTypes.WOODEN_SLAB);
        playerBlocks.add(BlockTypes.DOUBLE_WOODEN_SLAB);
        playerBlocks.add(BlockTypes.ENDER_CHEST);
        playerBlocks.add(BlockTypes.SANDSTONE_STAIRS);
        playerBlocks.add(BlockTypes.SPRUCE_STAIRS);
        playerBlocks.add(BlockTypes.JUNGLE_STAIRS);
        playerBlocks.add(BlockTypes.COMMAND_BLOCK);
        playerBlocks.add(BlockTypes.BEACON);
        playerBlocks.add(BlockTypes.COBBLESTONE_WALL);
        playerBlocks.add(BlockTypes.FLOWER_POT);
        playerBlocks.add(BlockTypes.CARROTS);
        playerBlocks.add(BlockTypes.POTATOES);
        playerBlocks.add(BlockTypes.WOODEN_BUTTON);
        playerBlocks.add(BlockTypes.SKULL);
        playerBlocks.add(BlockTypes.ANVIL);

        // these are unnatural in the standard world, but not in the nether
        if (environment.equals(DimensionTypes.NETHER)) {
            playerBlocks.add(BlockTypes.NETHERRACK);
            playerBlocks.add(BlockTypes.SOUL_SAND);
            playerBlocks.add(BlockTypes.GLOWSTONE);
            playerBlocks.add(BlockTypes.NETHER_BRICK);
            playerBlocks.add(BlockTypes.NETHER_BRICK_FENCE);
            playerBlocks.add(BlockTypes.NETHER_BRICK_STAIRS);
        }

        // these are unnatural in the standard and nether worlds, but not in the end
        if (environment.equals(DimensionTypes.THE_END)) {
            playerBlocks.add(BlockTypes.OBSIDIAN);
            playerBlocks.add(BlockTypes.END_STONE);
            playerBlocks.add(BlockTypes.END_PORTAL_FRAME);
        }

        //these are unnatural in sandy biomes, but not elsewhere 
        if (biome == BiomeTypes.DESERT || biome == BiomeTypes.DESERT_HILLS || biome == BiomeTypes.BEACH ||
                !environment.equals(DimensionTypes.OVERWORLD)) {
            playerBlocks.add(BlockTypes.LEAVES);
            playerBlocks.add(BlockTypes.LOG);
            playerBlocks.add(BlockTypes.LOG2);
        }

        return playerBlocks;
    }
}
