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
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;

//represents a visualization sent to a player
//FEATURE: to show players visually where claim boundaries are, we send them fake block change packets
//the result is that those players see new blocks, but the world hasn't been changed.  other players can't see the new blocks, either.
public class Visualization {

    public ArrayList<Transaction<BlockSnapshot>> elements = new ArrayList<Transaction<BlockSnapshot>>();

    // sends a visualization to a player
    public static void Apply(Player player, Visualization visualization) {
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());

        // if he has any current visualization, clear it first
        if (playerData.currentVisualization != null) {
            Visualization.Revert(player);
        }

        // if he's online, create a task to send him the visualization
        if (player.isOnline() && visualization.elements.size() > 0
                && visualization.elements.get(0).getOriginal().getLocation().get().getExtent().equals(player.getWorld())) {
            Sponge.getGame().getScheduler().createTaskBuilder().delayTicks(1L)
                    .execute(new VisualizationApplicationTask(player, playerData, visualization)).submit(GriefPrevention.instance);
        }
    }

    // reverts a visualization by sending another block change list, this time
    // with the real world block values
    public static void Revert(Player player) {
        if (!player.isOnline()) {
            return;
        }

        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());

        Visualization visualization = playerData.currentVisualization;

        if (playerData.currentVisualization != null) {
            // locality
            int minx = player.getLocation().getBlockX() - 100;
            int minz = player.getLocation().getBlockZ() - 100;
            int maxx = player.getLocation().getBlockX() + 100;
            int maxz = player.getLocation().getBlockZ() + 100;

            // remove any elements which are too far away
            visualization.removeElementsOutOfRange(visualization.elements, minx, minz, maxx, maxz);

            // send real block information for any remaining elements
            for (int i = 0; i < visualization.elements.size(); i++) {
                Transaction<BlockSnapshot> element = visualization.elements.get(i);

                // check player still in world where visualization exists
                if (i == 0) {
                    if (!player.getWorld().equals(element.getOriginal().getLocation().get().getExtent())) {
                        return;
                    }
                }

                BlockUtils.sendBlockChange(player, element.getOriginal());
            }

            playerData.currentVisualization = null;
        }
    }

    // convenience method to build a visualization from a claim
    // visualizationType determines the style (gold blocks, silver, red,
    // diamond, etc)
    public static Visualization FromClaim(Claim claim, int height, VisualizationType visualizationType, Location<World> locality) {
        // visualize only top level claims
        if (claim.parent != null) {
            return FromClaim(claim.parent, height, visualizationType, locality);
        }

        Visualization visualization = new Visualization();

        // add subdivisions first
        for (int i = 0; i < claim.children.size(); i++) {
            Claim child = claim.children.get(i);
            if (!child.inDataStore) {
                continue;
            }
            visualization.addClaimElements(child, height, VisualizationType.Subdivision, locality);
        }

        // special visualization for administrative land claims
        if (claim.isAdminClaim() && visualizationType == VisualizationType.Claim) {
            visualizationType = VisualizationType.AdminClaim;
        }

        // add top level last so that it takes precedence (it shows on top when
        // the child claim boundaries overlap with its boundaries)
        visualization.addClaimElements(claim, height, visualizationType, locality);

        return visualization;
    }

    // adds a claim's visualization to the current visualization
    // handy for combining several visualizations together, as when
    // visualization a top level claim with several subdivisions inside
    // locality is a performance consideration. only create visualization blocks
    // for around 100 blocks of the locality
    private void addClaimElements(Claim claim, int height, VisualizationType visualizationType, Location<World> locality) {
        Location<World> smallXsmallZ = claim.getLesserBoundaryCorner();
        Location<World> bigXbigZ = claim.getGreaterBoundaryCorner();
        World world = smallXsmallZ.getExtent();
        boolean waterIsTransparent = locality.getBlock().getType() == BlockTypes.WATER;

        int smallx = smallXsmallZ.getBlockX();
        int smallz = smallXsmallZ.getBlockZ();
        int bigx = bigXbigZ.getBlockX();
        int bigz = bigXbigZ.getBlockZ();

        BlockType cornerMaterial;
        BlockType accentMaterial;

        ArrayList<Transaction<BlockSnapshot>> newElements = new ArrayList<Transaction<BlockSnapshot>>();

        if (visualizationType == VisualizationType.Claim) {
            cornerMaterial = BlockTypes.GLOWSTONE;
            accentMaterial = BlockTypes.GOLD_BLOCK;
        } else if (visualizationType == VisualizationType.AdminClaim) {
            cornerMaterial = BlockTypes.GLOWSTONE;
            accentMaterial = BlockTypes.PUMPKIN;
        } else if (visualizationType == VisualizationType.Subdivision) {
            cornerMaterial = BlockTypes.IRON_BLOCK;
            accentMaterial = BlockTypes.WOOL;
        } else if (visualizationType == VisualizationType.RestoreNature) {
            cornerMaterial = BlockTypes.DIAMOND_BLOCK;
            accentMaterial = BlockTypes.DIAMOND_BLOCK;
        } else {
            cornerMaterial = BlockTypes.REDSTONE_ORE;
            accentMaterial = BlockTypes.NETHERRACK;
        }

        // initialize visualization elements without Y values and real data
        // that will be added later for only the visualization elements within
        // visualization range

        // locality
        int minx = locality.getBlockX() - 75;
        int minz = locality.getBlockZ() - 75;
        int maxx = locality.getBlockX() + 75;
        int maxz = locality.getBlockZ() + 75;

        final int STEP = 10;

        // top line
        BlockSnapshot.Builder snapshotBuilder = Sponge.getGame().getRegistry().createBuilder(BlockSnapshot.Builder.class);
        BlockSnapshot topVisualBlock1 =
                snapshotBuilder.from(new Location<World>(world, smallx, 0, bigz)).blockState(cornerMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(topVisualBlock1.getLocation().get().createSnapshot(), topVisualBlock1));
        BlockSnapshot topVisualBlock2 =
                snapshotBuilder.from(new Location<World>(world, smallx + 1, 0, bigz)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(topVisualBlock2.getLocation().get().createSnapshot(), topVisualBlock2));
        for (int x = smallx + STEP; x < bigx - STEP / 2; x += STEP) {
            if (x > minx && x < maxx) {
                BlockSnapshot visualBlock =
                        snapshotBuilder.from(new Location<World>(world, x, 0, bigz)).blockState(accentMaterial.getDefaultState()).build();
                newElements.add(new Transaction<BlockSnapshot>(visualBlock.getLocation().get().createSnapshot(), visualBlock));
            }
        }
        BlockSnapshot topVisualBlock3 =
                snapshotBuilder.from(new Location<World>(world, bigx - 1, 0, bigz)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(topVisualBlock3.getLocation().get().createSnapshot(), topVisualBlock3));

        // bottom line
        BlockSnapshot bottomVisualBlock1 =
                snapshotBuilder.from(new Location<World>(world, smallx + 1, 0, smallz)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(bottomVisualBlock1.getLocation().get().createSnapshot(), bottomVisualBlock1));
        for (int x = smallx + STEP; x < bigx - STEP / 2; x += STEP) {
            if (x > minx && x < maxx) {
                BlockSnapshot visualBlock =
                        snapshotBuilder.from(new Location<World>(world, x, 0, smallz)).blockState(accentMaterial.getDefaultState()).build();
                newElements.add(new Transaction<BlockSnapshot>(visualBlock.getLocation().get().createSnapshot(), visualBlock));
            }
        }
        BlockSnapshot bottomVisualBlock2 =
                snapshotBuilder.from(new Location<World>(world, bigx - 1, 0, smallz)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(bottomVisualBlock2.getLocation().get().createSnapshot(), bottomVisualBlock2));

        // left line
        BlockSnapshot leftVisualBlock1 =
                snapshotBuilder.from(new Location<World>(world, smallx, 0, smallz)).blockState(cornerMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(leftVisualBlock1.getLocation().get().createSnapshot(), leftVisualBlock1));
        BlockSnapshot leftVisualBlock2 =
                snapshotBuilder.from(new Location<World>(world, smallx, 0, smallz + 1)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(leftVisualBlock2.getLocation().get().createSnapshot(), leftVisualBlock2));
        for (int z = smallz + STEP; z < bigz - STEP / 2; z += STEP) {
            if (z > minz && z < maxz) {
                BlockSnapshot visualBlock =
                        snapshotBuilder.from(new Location<World>(world, smallx, 0, z)).blockState(accentMaterial.getDefaultState()).build();
                newElements.add(new Transaction<BlockSnapshot>(visualBlock.getLocation().get().createSnapshot(), visualBlock));
            }
        }
        BlockSnapshot leftVisualBlock3 =
                snapshotBuilder.from(new Location<World>(world, smallx, 0, bigz - 1)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(leftVisualBlock3.getLocation().get().createSnapshot(), leftVisualBlock3));

        // right line
        BlockSnapshot rightVisualBlock1 =
                snapshotBuilder.from(new Location<World>(world, bigx, 0, smallz)).blockState(cornerMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(rightVisualBlock1.getLocation().get().createSnapshot(), rightVisualBlock1));
        BlockSnapshot rightVisualBlock2 =
                snapshotBuilder.from(new Location<World>(world, bigx, 0, smallz + 1)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(rightVisualBlock2.getLocation().get().createSnapshot(), rightVisualBlock2));
        for (int z = smallz + STEP; z < bigz - STEP / 2; z += STEP) {
            if (z > minz && z < maxz) {
                BlockSnapshot visualBlock =
                        snapshotBuilder.from(new Location<World>(world, bigx, 0, z)).blockState(accentMaterial.getDefaultState()).build();
                newElements.add(new Transaction<BlockSnapshot>(visualBlock.getLocation().get().createSnapshot(), visualBlock));
            }
        }
        BlockSnapshot rightVisualBlock3 =
                snapshotBuilder.from(new Location<World>(world, bigx, 0, bigz - 1)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(rightVisualBlock3.getLocation().get().createSnapshot(), rightVisualBlock3));
        BlockSnapshot rightVisualBlock4 =
                snapshotBuilder.from(new Location<World>(world, bigx, 0, bigz)).blockState(cornerMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(rightVisualBlock4.getLocation().get().createSnapshot(), rightVisualBlock4));

        // remove any out of range elements
        this.removeElementsOutOfRange(newElements, minx, minz, maxx, maxz);

        // remove any elements outside the claim
        for (int i = 0; i < newElements.size(); i++) {
            BlockSnapshot element = newElements.get(i).getFinal();
            if (!claim.contains(element.getLocation().get(), true, false)) {
                newElements.remove(i--);
            }
        }

        // set Y values and real block information for any remaining
        // visualization blocks
        ArrayList<Transaction<BlockSnapshot>> actualElements = new ArrayList<Transaction<BlockSnapshot>>();
        for (Transaction<BlockSnapshot> element : newElements) {
            Location<World> tempLocation = element.getFinal().getLocation().get();
            Location<World> visibleLocation =
                    getVisibleLocation(tempLocation.getExtent(), tempLocation.getBlockX(), height, tempLocation.getBlockZ(), waterIsTransparent);
            element = new Transaction<BlockSnapshot>(element.getOriginal().withLocation(visibleLocation).withState(visibleLocation.getBlock()),
                    element.getFinal().withLocation(visibleLocation));
            height = element.getFinal().getPosition().getY();
            actualElements.add(element);
        }

        this.elements.addAll(actualElements);
    }

    // removes any elements which are out of visualization range
    private void removeElementsOutOfRange(ArrayList<Transaction<BlockSnapshot>> elements, int minx, int minz, int maxx, int maxz) {
        for (int i = 0; i < elements.size(); i++) {
            Location<World> location = elements.get(i).getFinal().getLocation().get();
            if (location.getX() < minx || location.getX() > maxx || location.getZ() < minz || location.getZ() > maxz) {
                elements.remove(i--);
            }
        }
    }

    // finds a block the player can probably see. this is how visualizations
    // "cling" to the ground or ceiling
    private static Location<World> getVisibleLocation(World world, int x, int y, int z, boolean waterIsTransparent) {
        BlockSnapshot block = world.createSnapshot(x, y, z);
        Direction direction = (isTransparent(block.getState().getType(), waterIsTransparent)) ? Direction.DOWN : Direction.UP;

        while (block.getPosition().getY() >= 1 &&
                block.getPosition().getY() < world.getDimension().getBuildHeight() - 1 &&
                (!isTransparent(block.getLocation().get().getRelative(Direction.UP).getBlockType(), waterIsTransparent)
                        || isTransparent(block.getState().getType(), waterIsTransparent))) {
            block = block.getLocation().get().getRelative(direction).createSnapshot();
        }

        return block.getLocation().get();
    }

    // helper method for above. allows visualization blocks to sit underneath
    // partly transparent blocks like grass and fence
    private static boolean isTransparent(BlockType block, boolean waterIsTransparent) {
        return (block != BlockTypes.SNOW && (block == BlockTypes.AIR ||
                block == BlockTypes.FENCE ||
                block == BlockTypes.STANDING_SIGN ||
                block == BlockTypes.WALL_SIGN ||
                (waterIsTransparent && block == BlockTypes.WATER) ||
                !((net.minecraft.block.Block) block).getMaterial().blocksLight()));
    }

    public static Visualization fromClaims(Iterable<Claim> claims, int height, VisualizationType type, Location<World> locality) {
        Visualization visualization = new Visualization();

        for (Claim claim : claims) {
            visualization.addClaimElements(claim, height, type, locality);
        }

        return visualization;
    }
}
