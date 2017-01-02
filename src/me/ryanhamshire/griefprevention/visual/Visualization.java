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
package me.ryanhamshire.griefprevention.visual;

import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.task.VisualizationApplicationTask;
import net.minecraft.block.state.IBlockState;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.property.block.MatterProperty;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//represents a visualization sent to a player
//FEATURE: to show players visually where claim boundaries are, we send them fake block change packets
//the result is that those players see new blocks, but the world hasn't been changed.  other players can't see the new blocks, either.
public class Visualization {

    public ArrayList<Transaction<BlockSnapshot>> elements;
    private ArrayList<Transaction<BlockSnapshot>> newElements;
    private VisualizationType type;
    private GPClaim claim;
    private Location<World> lesserBoundaryCorner;
    private Location<World> greaterBoundaryCorner;
    private boolean cuboidVisual = false;
    private int smallx;
    private int smally;
    private int smallz;
    private int bigx;
    private int bigy;
    private int bigz;
    private int minx;
    private int maxx;
    private int minz;
    private int maxz;
    private BlockType cornerMaterial;
    private BlockType accentMaterial;
    private BlockType fillerMaterial = BlockTypes.DIAMOND_BLOCK; // used for 3d cuboids
    private BlockSnapshot.Builder snapshotBuilder;
    public boolean displaySubdivisions = false;
    private static int STEP = 10;

    public Visualization(VisualizationType type) {
        initBlockVisualTypes(type);
        this.type = type;
        this.snapshotBuilder = Sponge.getGame().getRegistry().createBuilder(BlockSnapshot.Builder.class);
        this.elements = new ArrayList<Transaction<BlockSnapshot>>();
        this.newElements = new ArrayList<>();
    }

    public Visualization(GPClaim claim, VisualizationType type) {
        this(claim.lesserBoundaryCorner, claim.greaterBoundaryCorner, type);
        this.claim = claim;
    }

    public Visualization(Location<World> lesserBoundaryCorner, Location<World> greaterBoundaryCorner, VisualizationType type) {
        initBlockVisualTypes(type);
        this.lesserBoundaryCorner = lesserBoundaryCorner;
        this.greaterBoundaryCorner = greaterBoundaryCorner;
        this.type = type;
        this.snapshotBuilder = Sponge.getGame().getRegistry().createBuilder(BlockSnapshot.Builder.class);
        this.elements = new ArrayList<Transaction<BlockSnapshot>>();
        this.newElements = new ArrayList<>();
    }

    public void initBlockVisualTypes(VisualizationType type) {
        if (type == VisualizationType.Claim) {
            cornerMaterial = BlockTypes.GLOWSTONE;
            accentMaterial = BlockTypes.GOLD_BLOCK;
            //fillerMaterial = BlockTypes.STAINED_GLASS;
        } else if (type == VisualizationType.AdminClaim) {
            cornerMaterial = BlockTypes.GLOWSTONE;
            accentMaterial = BlockTypes.PUMPKIN;
           // fillerMaterial = BlockTypes.DIAMOND_BLOCK;
        } else if (type == VisualizationType.Subdivision) {
            cornerMaterial = BlockTypes.IRON_BLOCK;
            accentMaterial = BlockTypes.WOOL;
           // fillerMaterial = BlockTypes.DIAMOND_BLOCK;
        } else if (type == VisualizationType.RestoreNature) {
            cornerMaterial = BlockTypes.DIAMOND_BLOCK;
            accentMaterial = BlockTypes.DIAMOND_BLOCK;
           // fillerMaterial = BlockTypes.DIAMOND_BLOCK;
        } else {
            cornerMaterial = BlockTypes.REDSTONE_ORE;
            accentMaterial = BlockTypes.NETHERRACK;
           // fillerMaterial = BlockTypes.DIAMOND_BLOCK;
        }
    }

    public static VisualizationType getVisualizationType(GPClaim claim) {
        ClaimType type = claim.type;
        if (type != null) {
            if (type == ClaimType.ADMIN) {
                return VisualizationType.AdminClaim;
            } else if (type == ClaimType.SUBDIVISION) {
                return VisualizationType.Subdivision;
            }
        }

        return VisualizationType.Claim;
    }

    public GPClaim getClaim() {
        return this.claim;
    }

    // sends a visualization to a player
    public void apply(Player player) {
        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());

        // if he has any current visualization, clear it first
        playerData.revertActiveVisual(player);

        // if he's online, create a task to send him the visualization
        if (player.isOnline() && this.elements.size() > 0
                && this.elements.get(0).getOriginal().getLocation().get().getExtent().equals(player.getWorld())) {
            Sponge.getGame().getScheduler().createTaskBuilder().delayTicks(1L)
                    .execute(new VisualizationApplicationTask(player, playerData, this)).submit(GriefPreventionPlugin.instance);
        }
    }

    // reverts a visualization by sending another block change list, this time
    // with the real world block values
    public void revert(Player player) {
        if (!player.isOnline()) {
            return;
        }

        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());

        int minx = player.getLocation().getBlockX() - 100;
        int minz = player.getLocation().getBlockZ() - 100;
        int maxx = player.getLocation().getBlockX() + 100;
        int maxz = player.getLocation().getBlockZ() + 100;

        // remove any elements which are too far away
        if (!this.cuboidVisual) {
            this.removeElementsOutOfRange(this.elements, minx, minz, maxx, maxz);
        }

        // send real block information for any remaining elements
        for (int i = 0; i < this.elements.size(); i++) {
            BlockSnapshot snapshot = this.elements.get(i).getOriginal();

            // check player still in world where visualization exists
            if (i == 0) {
                if (!player.getWorld().equals(snapshot.getLocation().get().getExtent())) {
                    return;
                }
            }

            player.sendBlockChange(snapshot.getPosition(), snapshot.getState());
        }

        playerData.visualBlocks = null;
        if (playerData.visualRevertTask != null) {
            playerData.visualRevertTask.cancel();
        }
    }

    public static Visualization fromClick(Location<World> location, int height, VisualizationType visualizationType, GPPlayerData playerData) {
        Visualization visualization = new Visualization(visualizationType);
        BlockSnapshot blockClicked =
                visualization.snapshotBuilder.from(location).blockState(visualization.cornerMaterial.getDefaultState()).build();
        visualization.elements.add(new Transaction<BlockSnapshot>(blockClicked.getLocation().get().createSnapshot(), blockClicked));
        return visualization;
    }

    public void resetVisuals() {
        this.elements.clear();
        this.newElements.clear();
    }

    public void createClaimBlockVisualWithType(int height, Location<World> locality, GPPlayerData playerData, VisualizationType visualType) {
        VisualizationType currentType = this.type;
        this.type = visualType;
        this.addClaimElements(height, locality, playerData);
    }

    public void createClaimBlockVisuals(int height, Location<World> locality, GPPlayerData playerData) {
        if (this.elements.size() != 0) {
            return;
        }

        // add top level last so that it takes precedence (it shows on top when
        // the child claim boundaries overlap with its boundaries)
        this.addClaimElements(height, locality, playerData);
    }

    public void displaySubdivisionVisuals(int height, Location<World> locality, GPPlayerData playerData) {
        // add subdivisions first
        for (int i = 0; i < this.claim.children.size(); i++) {
            GPClaim subdivision = (GPClaim) this.claim.children.get(i);
            subdivision.getVisualizer().createClaimBlockVisuals(height, locality, playerData);
        }
    }

    public VisualizationType getType() {
        return this.type;
    }

    public void setType(VisualizationType type) {
        this.type = type;
    }

    // adds a claim's visualization to the current visualization
    // handy for combining several visualizations together, as when
    // visualization a top level claim with several subdivisions inside
    // locality is a performance consideration. only create visualization blocks
    // for around 100 blocks of the locality
    private void addClaimElements(int height, Location<World> locality, GPPlayerData playerData) {
        Location<World> smallXsmallZ = this.claim.getLesserBoundaryCorner();
        Location<World> bigXbigZ = this.claim.getGreaterBoundaryCorner();
        World world = smallXsmallZ.getExtent();
        boolean liquidTransparent = locality.getBlock().getType().getProperty(MatterProperty.class).isPresent() ? false : true;

        this.smallx = smallXsmallZ.getBlockX();
        this.smally = this.claim.cuboid ? smallXsmallZ.getBlockY() : 0;
        this.smallz = smallXsmallZ.getBlockZ();
        this.bigx = bigXbigZ.getBlockX();
        this.bigy = this.claim.cuboid ? bigXbigZ.getBlockY() : 0;
        this.bigz = bigXbigZ.getBlockZ();
        this.minx = this.claim.cuboid ? smallx : locality.getBlockX() - 75;
        this.minz = this.claim.cuboid ? smallz : locality.getBlockZ() - 75;
        this.maxx = this.claim.cuboid ? bigx : locality.getBlockX() + 75;
        this.maxz = this.claim.cuboid ? bigz : locality.getBlockZ() + 75;

        // initialize visualization elements without Y values and real data
        // that will be added later for only the visualization elements within
        // visualization range

        if (this.smallx == this.bigx && this.smally == this.bigy && this.smallz == this.bigz) {
            BlockSnapshot blockClicked =
                    snapshotBuilder.from(new Location<World>(world, smallx, smally, smallz)).blockState(cornerMaterial.getDefaultState()).build();
            elements.add(new Transaction<BlockSnapshot>(blockClicked.getLocation().get().createSnapshot(), blockClicked));
            return;
        }

        if (this.claim.cuboid) {
            this.addVisuals3D(claim, playerData);
        } else {
            this.addVisuals2D(claim, height, liquidTransparent);
        }
    }

    public void addVisuals3D(GPClaim claim, GPPlayerData playerData) {
        World world = claim.world;

        this.addTopLine(world, smally, cornerMaterial, accentMaterial);
        for (int y = smally + STEP; y < bigy - STEP / 2; y += STEP) {
            this.addTopLine(world, y, fillerMaterial, fillerMaterial);
        }
        this.addTopLine(world, bigy, cornerMaterial, accentMaterial);
        this.addBottomLine(world, smally, cornerMaterial, accentMaterial);
        for (int y = smally + STEP; y < bigy - STEP / 2; y += STEP) {
            this.addBottomLine(world, y, fillerMaterial, fillerMaterial);
        }
        this.addBottomLine(world, bigy, cornerMaterial, accentMaterial);
        this.addLeftLine(world, smally, cornerMaterial, accentMaterial);
        for (int y = smally + STEP; y < bigy - STEP / 2; y += STEP) {
            this.addLeftLine(world, y, fillerMaterial, fillerMaterial);
        }
        this.addLeftLine(world, bigy, cornerMaterial, accentMaterial);
        this.addRightLine(world, smally, cornerMaterial, accentMaterial);
        for (int y = smally + STEP; y < bigy - STEP / 2; y += STEP) {
            this.addRightLine(world, y, fillerMaterial, fillerMaterial);
        }
        this.addRightLine(world, bigy, cornerMaterial, accentMaterial);
        // don't show corners while subdividing
        if (playerData == null || (playerData.claimSubdividing == null)) {
            // top corners
            this.addCorners(world, bigy - 1, accentMaterial);
            // bottom corners
            this.addCorners(world, smally + 1, accentMaterial);
        }

        this.elements.addAll(newElements);
    }

    public void addVisuals2D(GPClaim claim, int height, boolean liquidTransparent) {
        World world = claim.world;
        this.addTopLine(world, 0, cornerMaterial, accentMaterial);
        this.addBottomLine(world, 0, cornerMaterial, accentMaterial);
        this.addLeftLine(world, 0, cornerMaterial, accentMaterial);
        this.addRightLine(world, 0, cornerMaterial, accentMaterial);

        // remove any out of range elements
        this.removeElementsOutOfRange(this.newElements, minx, minz, maxx, maxz);

        for (int i = 0; i < newElements.size(); i++) {
            BlockSnapshot element = newElements.get(i).getFinal();
            if (!claim.contains(element.getLocation().get(), true, false)) {
                newElements.remove(i--);
            }
        }
        // set Y values and real block information for any remaining visualization blocks
        ArrayList<Transaction<BlockSnapshot>> actualElements = new ArrayList<Transaction<BlockSnapshot>>();
        for (Transaction<BlockSnapshot> element : newElements) {
            Location<World> tempLocation = element.getFinal().getLocation().get();
            Location<World> visibleLocation =
                    getVisibleLocation(tempLocation.getExtent(), tempLocation.getBlockX(), height, tempLocation.getBlockZ(), liquidTransparent);
            element = new Transaction<BlockSnapshot>(element.getOriginal().withLocation(visibleLocation).withState(visibleLocation.getBlock()),
                    element.getFinal().withLocation(visibleLocation));
            height = element.getFinal().getPosition().getY();
            actualElements.add(element);
        }

        this.elements.addAll(actualElements);
    }

    public void addCorners(World world, int y, BlockType accentMaterial) {
        BlockSnapshot corner1 =
                snapshotBuilder.from(new Location<World>(world, smallx, y, bigz)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(corner1.getLocation().get().createSnapshot(), corner1));
        BlockSnapshot corner2 =
                snapshotBuilder.from(new Location<World>(world, bigx, y, bigz)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(corner2.getLocation().get().createSnapshot(), corner2));
        BlockSnapshot corner3 =
                snapshotBuilder.from(new Location<World>(world, bigx, y, smallz)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(corner3.getLocation().get().createSnapshot(), corner3));
        BlockSnapshot corner4 =
                snapshotBuilder.from(new Location<World>(world, smallx, y, smallz)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(corner4.getLocation().get().createSnapshot(), corner4));
    }

    public void addTopLine(World world, int y, BlockType cornerMaterial, BlockType accentMaterial) {
        BlockSnapshot topVisualBlock1 =
                snapshotBuilder.from(new Location<World>(world, smallx, y, bigz)).blockState(cornerMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(topVisualBlock1.getLocation().get().createSnapshot(), topVisualBlock1));
        BlockSnapshot topVisualBlock2 =
                snapshotBuilder.from(new Location<World>(world, smallx + 1, y, bigz)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(topVisualBlock2.getLocation().get().createSnapshot(), topVisualBlock2));
        BlockSnapshot topVisualBlock3 =
                snapshotBuilder.from(new Location<World>(world, bigx - 1, y, bigz)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(topVisualBlock3.getLocation().get().createSnapshot(), topVisualBlock3));

        for (int x = smallx + STEP; x < bigx - STEP / 2; x += STEP) {
            if ((y != 0 && x >= smallx && x <= bigx) || (x > minx && x < maxx)) {
                BlockSnapshot visualBlock =
                        snapshotBuilder.from(new Location<World>(world, x, y, bigz)).blockState(accentMaterial.getDefaultState()).build();
                newElements.add(new Transaction<BlockSnapshot>(visualBlock.getLocation().get().createSnapshot(), visualBlock));
            }
        }
    }

    public void addBottomLine(World world, int y, BlockType cornerMaterial, BlockType accentMaterial) {
        BlockSnapshot bottomVisualBlock1 =
                this.snapshotBuilder.from(new Location<World>(world, smallx + 1, y, smallz)).blockState(accentMaterial.getDefaultState()).build();
        this.newElements.add(new Transaction<BlockSnapshot>(bottomVisualBlock1.getLocation().get().createSnapshot(), bottomVisualBlock1));
        BlockSnapshot bottomVisualBlock2 =
                this.snapshotBuilder.from(new Location<World>(world, bigx - 1, y, smallz)).blockState(accentMaterial.getDefaultState()).build();
        this.newElements.add(new Transaction<BlockSnapshot>(bottomVisualBlock2.getLocation().get().createSnapshot(), bottomVisualBlock2));

        for (int x = smallx + STEP; x < bigx - STEP / 2; x += STEP) {
            if ((y != 0 && x >= smallx && x <= bigx) || (x > minx && x < maxx)) {
                BlockSnapshot visualBlock =
                        this.snapshotBuilder.from(new Location<World>(world, x, y, smallz)).blockState(accentMaterial.getDefaultState()).build();
                newElements.add(new Transaction<BlockSnapshot>(visualBlock.getLocation().get().createSnapshot(), visualBlock));
            }
        }
    }

    public void addLeftLine(World world, int y, BlockType cornerMaterial, BlockType accentMaterial) {
        BlockSnapshot leftVisualBlock1 =
                snapshotBuilder.from(new Location<World>(world, smallx, y, smallz)).blockState(cornerMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(leftVisualBlock1.getLocation().get().createSnapshot(), leftVisualBlock1));
        BlockSnapshot leftVisualBlock2 =
                snapshotBuilder.from(new Location<World>(world, smallx, y, smallz + 1)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(leftVisualBlock2.getLocation().get().createSnapshot(), leftVisualBlock2));
        BlockSnapshot leftVisualBlock3 =
                snapshotBuilder.from(new Location<World>(world, smallx, y, bigz - 1)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(leftVisualBlock3.getLocation().get().createSnapshot(), leftVisualBlock3));

        for (int z = smallz + STEP; z < bigz - STEP / 2; z += STEP) {
            if ((y != 0 && z >= smallz && z <= bigz) || (z > minz && z < maxz)) {
                BlockSnapshot visualBlock =
                        snapshotBuilder.from(new Location<World>(world, smallx, y, z)).blockState(accentMaterial.getDefaultState()).build();
                newElements.add(new Transaction<BlockSnapshot>(visualBlock.getLocation().get().createSnapshot(), visualBlock));
           }
        }
    }

    public void addRightLine(World world, int y, BlockType cornerMaterial, BlockType accentMaterial) {
        BlockSnapshot rightVisualBlock1 =
                snapshotBuilder.from(new Location<World>(world, bigx, y, smallz)).blockState(cornerMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(rightVisualBlock1.getLocation().get().createSnapshot(), rightVisualBlock1));
        BlockSnapshot rightVisualBlock2 =
                snapshotBuilder.from(new Location<World>(world, bigx, y, smallz + 1)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(rightVisualBlock2.getLocation().get().createSnapshot(), rightVisualBlock2));
        for (int z = smallz + STEP; z < bigz - STEP / 2; z += STEP) {
            if ((y != 0 && z >= smallz && z <= bigz) || (z > minz && z < maxz)) {
                BlockSnapshot visualBlock =
                        snapshotBuilder.from(new Location<World>(world, bigx, y, z)).blockState(accentMaterial.getDefaultState()).build();
                newElements.add(new Transaction<BlockSnapshot>(visualBlock.getLocation().get().createSnapshot(), visualBlock));
            }
        }
        BlockSnapshot rightVisualBlock3 =
                snapshotBuilder.from(new Location<World>(world, bigx, y, bigz - 1)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(rightVisualBlock3.getLocation().get().createSnapshot(), rightVisualBlock3));
        BlockSnapshot rightVisualBlock4 =
                snapshotBuilder.from(new Location<World>(world, bigx, y, bigz)).blockState(cornerMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(rightVisualBlock4.getLocation().get().createSnapshot(), rightVisualBlock4));
    }

    public List<Transaction<BlockSnapshot>> getVisualElements() {
        return this.elements;
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
        Direction direction = (isTransparent(block.getState(), waterIsTransparent)) ? Direction.DOWN : Direction.UP;

        while (block.getPosition().getY() >= 1 &&
                block.getPosition().getY() < world.getDimension().getBuildHeight() - 1 &&
                (!isTransparent(block.getLocation().get().getRelative(Direction.UP).getBlock(), waterIsTransparent)
                        || isTransparent(block.getState(), waterIsTransparent))) {
            block = block.getLocation().get().getRelative(direction).createSnapshot();
        }

        return block.getLocation().get();
    }

    // helper method for above. allows visualization blocks to sit underneath
    // partly transparent blocks like grass and fence
    private static boolean isTransparent(BlockState blockstate, boolean waterIsTransparent) {
        IBlockState iblockstate = (IBlockState)(Object) blockstate;
        Optional<MatterProperty> matterProperty = blockstate.getProperty(MatterProperty.class);
        if (!waterIsTransparent && matterProperty.isPresent() && matterProperty.get().getValue() == MatterProperty.Matter.LIQUID) {
            return false;
        }
        return !iblockstate.isOpaqueCube();
    }

    public static Visualization fromClaims(Iterable<GPClaim> claims) {
        Visualization visualization = new Visualization(VisualizationType.Claim);

        for (GPClaim claim : claims) {
            if (claim.visualization != null) {
                visualization.elements.addAll(claim.visualization.elements);
            }
            for (Claim child : claim.children) {
                GPClaim subdivision = (GPClaim) child;
                if (subdivision.visualization != null) {
                    visualization.elements.addAll(subdivision.visualization.elements);
                }
            }
        }

        return visualization;
    }
}
