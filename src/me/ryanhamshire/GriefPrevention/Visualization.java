/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.ryanhamshire.GriefPrevention;

import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockSnapshotBuilder;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;

//represents a visualization sent to a player
//FEATURE: to show players visually where claim boundaries are, we send them fake block change packets
//the result is that those players see new blocks, but the world hasn't been changed.  other players can't see the new blocks, either.
public class Visualization {

    public ArrayList<BlockSnapshot> elements = new ArrayList<BlockSnapshot>();

    // sends a visualization to a player
    public static void Apply(Player player, Visualization visualization) {
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());

        // if he has any current visualization, clear it first
        if (playerData.currentVisualization != null) {
            Visualization.Revert(player);
        }

        // if he's online, create a task to send him the visualization
        if (player.isOnline() && visualization.elements.size() > 0
                && visualization.elements.get(0).getLocation().get().getExtent().equals(player.getWorld())) {
            GriefPrevention.instance.game.getScheduler().createTaskBuilder().delay(1L)
                    .execute(new VisualizationApplicationTask(player, playerData, visualization)).submit(GriefPrevention.instance);
        }
    }

    // reverts a visualization by sending another block change list, this time
    // with the real world block values
    public static void Revert(Player player) {
        if (!player.isOnline())
            return;

        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());

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
                BlockSnapshot element = visualization.elements.get(i);

                // check player still in world where visualization exists
                if (i == 0) {
                    if (!player.getWorld().equals(element.getLocation().get().getExtent()))
                        return;
                }

                BlockUtils.sendBlockChange(player, element);
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
            if (!child.inDataStore)
                continue;
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

        ArrayList<BlockSnapshot> newElements = new ArrayList<BlockSnapshot>();

        if (visualizationType == VisualizationType.Claim) {
            cornerMaterial = BlockTypes.GLOWSTONE;
            accentMaterial = BlockTypes.GOLD_BLOCK;
        }

        else if (visualizationType == VisualizationType.AdminClaim) {
            cornerMaterial = BlockTypes.GLOWSTONE;
            accentMaterial = BlockTypes.PUMPKIN;
        }

        else if (visualizationType == VisualizationType.Subdivision) {
            cornerMaterial = BlockTypes.IRON_BLOCK;
            accentMaterial = BlockTypes.WOOL;
        }

        else if (visualizationType == VisualizationType.RestoreNature) {
            cornerMaterial = BlockTypes.DIAMOND_BLOCK;
            accentMaterial = BlockTypes.DIAMOND_BLOCK;
        }

        else {
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
        BlockSnapshotBuilder snapshotBuilder = GriefPrevention.instance.gameRegistry.createBlockSnapshotBuilder();
        newElements.add(snapshotBuilder.from(new Location<World>(world, smallx, height, bigz)).blockState(cornerMaterial.getDefaultState()).build());
        newElements.add(snapshotBuilder.from(new Location<World>(world, smallx + 1, height, bigz)).blockState(accentMaterial.getDefaultState()).build());
        for (int x = smallx + STEP; x < bigx - STEP / 2; x += STEP) {
            if (x > minx && x < maxx)
                newElements.add(snapshotBuilder.from(new Location<World>(world, x, height, bigz)).blockState(accentMaterial.getDefaultState()).build());
        }
        newElements.add(snapshotBuilder.from(new Location<World>(world, bigx - 1, height, bigz)).blockState(accentMaterial.getDefaultState()).build());

        // bottom line
        newElements.add(snapshotBuilder.from(new Location<World>(world, smallx + 1, height, smallz)).blockState(accentMaterial.getDefaultState()).build());
        for (int x = smallx + STEP; x < bigx - STEP / 2; x += STEP) {
            if (x > minx && x < maxx)
                newElements.add(snapshotBuilder.from(new Location<World>(world, x, height, smallz)).blockState(accentMaterial.getDefaultState()).build());
        }
        newElements.add(snapshotBuilder.from(new Location<World>(world, bigx - 1, height, smallz)).blockState(accentMaterial.getDefaultState()).build());

        // left line
        newElements.add(snapshotBuilder.from(new Location<World>(world, smallx, height, smallz)).blockState(cornerMaterial.getDefaultState()).build());
        newElements.add(snapshotBuilder.from(new Location<World>(world, smallx, height, smallz + 1)).blockState(accentMaterial.getDefaultState()).build());
        for (int z = smallz + STEP; z < bigz - STEP / 2; z += STEP) {
            if (z > minz && z < maxz)
                newElements.add(snapshotBuilder.from(new Location<World>(world, smallx, height, z)).blockState(accentMaterial.getDefaultState()).build());
        }
        newElements.add(snapshotBuilder.from(new Location<World>(world, smallx, height, bigz - 1)).blockState(accentMaterial.getDefaultState()).build());

        // right line
        newElements.add(snapshotBuilder.from(new Location<World>(world, bigx, height, smallz)).blockState(cornerMaterial.getDefaultState()).build());
        newElements.add(snapshotBuilder.from(new Location<World>(world, bigx, height, smallz + 1)).blockState(accentMaterial.getDefaultState()).build());
        for (int z = smallz + STEP; z < bigz - STEP / 2; z += STEP) {
            if (z > minz && z < maxz)
                newElements.add(snapshotBuilder.from(new Location<World>(world, bigx, height, z)).blockState(accentMaterial.getDefaultState()).build());
        }
        newElements.add(snapshotBuilder.from(new Location<World>(world, bigx, height, bigz - 1)).blockState(accentMaterial.getDefaultState()).build());
        newElements.add(snapshotBuilder.from(new Location<World>(world, bigx, height, bigz)).blockState(cornerMaterial.getDefaultState()).build());

        // remove any out of range elements
        this.removeElementsOutOfRange(newElements, minx, minz, maxx, maxz);

        // remove any elements outside the claim
        for (int i = 0; i < newElements.size(); i++) {
            BlockSnapshot element = newElements.get(i);
            if (!claim.contains(element.getLocation().get(), true, false)) {
                newElements.remove(i--);
            }
        }

        // set Y values and real block information for any remaining
        // visualization blocks
        for (BlockSnapshot element : newElements) {
            Location<World> tempLocation = element.getLocation().get();
            element = element.withLocation(
                    getVisibleLocation(tempLocation.getExtent(), tempLocation.getBlockX(), height, tempLocation.getBlockZ(), waterIsTransparent));
        }

        this.elements.addAll(newElements);
    }

    // removes any elements which are out of visualization range
    private void removeElementsOutOfRange(ArrayList<BlockSnapshot> elements, int minx, int minz, int maxx, int maxz) {
        for (int i = 0; i < elements.size(); i++) {
            Location<World> location = elements.get(i).getLocation().get();
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
            block = block.withLocation(block.getLocation().get().getRelative(direction));
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
                (waterIsTransparent && block == BlockTypes.WATER)));// ||
        // block.isTransparent()));
    }

    public static Visualization fromClaims(Iterable<Claim> claims, int height, VisualizationType type, Location<World> locality) {
        Visualization visualization = new Visualization();

        for (Claim claim : claims) {
            visualization.addClaimElements(claim, height, type, locality);
        }

        return visualization;
    }
}
