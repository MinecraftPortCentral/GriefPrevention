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

import com.flowpowered.math.vector.Vector3i;
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
    private ArrayList<Vector3i> corners;
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
    private BlockType fillerMaterial; // used for 3d cuboids
    private BlockSnapshot.Builder snapshotBuilder;
    public boolean displaySubdivisions = false;
    private int STEP = 10;

    public Visualization(VisualizationType type) {
        initBlockVisualTypes(type);
        this.type = type;
        this.snapshotBuilder = Sponge.getGame().getRegistry().createBuilder(BlockSnapshot.Builder.class);
        this.elements = new ArrayList<Transaction<BlockSnapshot>>();
        this.newElements = new ArrayList<>();
        this.corners = new ArrayList<>();
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
        this.corners = new ArrayList<>();
    }

    public void initBlockVisualTypes(VisualizationType type) {
        if (type == VisualizationType.CLAIM) {
            cornerMaterial = BlockTypes.GLOWSTONE;
            accentMaterial = BlockTypes.GOLD_BLOCK;
            fillerMaterial = BlockTypes.GOLD_BLOCK;
            //fillerMaterial = BlockTypes.STAINED_GLASS;
        } else if (type == VisualizationType.ADMINCLAIM) {
            cornerMaterial = BlockTypes.GLOWSTONE;
            accentMaterial = BlockTypes.PUMPKIN;
            fillerMaterial = BlockTypes.PUMPKIN;
           // fillerMaterial = BlockTypes.DIAMOND_BLOCK;
        } else if (type == VisualizationType.SUBDIVISION) {
            cornerMaterial = BlockTypes.IRON_BLOCK;
            accentMaterial = BlockTypes.WOOL;
            fillerMaterial = BlockTypes.WOOL;
           // fillerMaterial = BlockTypes.DIAMOND_BLOCK;
        } else if (type == VisualizationType.RESTORENATURE) {
            cornerMaterial = BlockTypes.DIAMOND_BLOCK;
            accentMaterial = BlockTypes.DIAMOND_BLOCK;
           // fillerMaterial = BlockTypes.DIAMOND_BLOCK;
        } else if (type == VisualizationType.TOWN) {
            cornerMaterial = BlockTypes.GLOWSTONE;
            accentMaterial = BlockTypes.EMERALD_BLOCK;
            fillerMaterial = BlockTypes.EMERALD_BLOCK;
        } else {
            cornerMaterial = BlockTypes.REDSTONE_ORE;
            accentMaterial = BlockTypes.NETHERRACK;
            fillerMaterial = BlockTypes.DIAMOND_BLOCK;
        }
    }

    public static VisualizationType getVisualizationType(GPClaim claim) {
        ClaimType type = claim.getType();
        if (type != null) {
            if (type == ClaimType.ADMIN) {
                return VisualizationType.ADMINCLAIM;
            } else if (type == ClaimType.TOWN) {
                return VisualizationType.TOWN;
            } else if (type == ClaimType.SUBDIVISION) {
                return VisualizationType.SUBDIVISION;
            }
        }

        return VisualizationType.CLAIM;
    }

    public GPClaim getClaim() {
        return this.claim;
    }

    public void apply(Player player) {
        this.apply(player, true);
    }

    // sends a visualization to a player
    public void apply(Player player, boolean resetActive) {
        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());

        // if he has any current visualization, clear it first
        //playerData.revertActiveVisual(player);
        
        boolean hideBorders = GriefPreventionPlugin.instance.worldEditProvider != null &&
                GriefPreventionPlugin.instance.worldEditProvider.hasCUISupport(player) &&
                GriefPreventionPlugin.getActiveConfig(player.getWorld().getProperties()).getConfig().claim.hideBorders;
        if (!hideBorders) {
            // if he's online, create a task to send him the visualization
            if (player.isOnline() && this.elements.size() > 0
                    && this.elements.get(0).getOriginal().getLocation().get().getExtent().equals(player.getWorld())) {
                Sponge.getGame().getScheduler().createTaskBuilder().delayTicks(1L)
                        .execute(new VisualizationApplicationTask(player, playerData, this, resetActive)).submit(GriefPreventionPlugin.instance);
                //GriefPreventionPlugin.instance.executor.execute(new VisualizationApplicationTask(player, playerData, this, resetActive));
            }
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

    public static Visualization fromClick(Location<World> location, int height, VisualizationType visualizationType, Player player, GPPlayerData playerData) {
        Visualization visualization = new Visualization(visualizationType);
        BlockSnapshot blockClicked =
                visualization.snapshotBuilder.from(location).blockState(visualization.cornerMaterial.getDefaultState()).build();
        visualization.elements.add(new Transaction<BlockSnapshot>(blockClicked.getLocation().get().createSnapshot(), blockClicked));
        if (GriefPreventionPlugin.instance.worldEditProvider != null) {
            GriefPreventionPlugin.instance.worldEditProvider.sendVisualDrag(player, playerData, location.getBlockPosition());
        }
        return visualization;
    }

    public void resetVisuals() {
        this.elements.clear();
        this.newElements.clear();
    }

    public void createClaimBlockVisualWithType(GPClaim claim, int height, Location<World> locality, GPPlayerData playerData, VisualizationType visualType) {
        this.type = visualType;
        this.claim = claim;
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
        this.initBlockVisualTypes(type);
        Location<World> lesser = this.claim.getLesserBoundaryCorner();
        Location<World> greater = this.claim.getGreaterBoundaryCorner();
        World world = lesser.getExtent();
        boolean liquidTransparent = locality.getBlock().getType().getProperty(MatterProperty.class).isPresent() ? false : true;

        this.smallx = lesser.getBlockX();
        this.smally = this.useCuboidVisual() ? lesser.getBlockY() : 0;
        this.smallz = lesser.getBlockZ();
        this.bigx = greater.getBlockX();
        this.bigy = this.useCuboidVisual() ? greater.getBlockY() : 0;
        this.bigz = greater.getBlockZ();
        this.minx = this.claim.cuboid ? this.smallx : locality.getBlockX() - 75;
        this.minz = this.claim.cuboid ? this.smallz : locality.getBlockZ() - 75;
        this.maxx = this.claim.cuboid ? this.bigx : locality.getBlockX() + 75;
        this.maxz = this.claim.cuboid ? this.bigz : locality.getBlockZ() + 75;

        // initialize visualization elements without Y values and real data
        // that will be added later for only the visualization elements within
        // visualization range

        if (this.smallx == this.bigx && this.smally == this.bigy && this.smallz == this.bigz) {
            BlockSnapshot blockClicked =
                    snapshotBuilder.from(new Location<World>(world, this.smallx, this.smally, this.smallz)).blockState(this.cornerMaterial.getDefaultState()).build();
            elements.add(new Transaction<BlockSnapshot>(blockClicked.getLocation().get().createSnapshot(), blockClicked));
            return;
        }

        // check CUI support
        if (GriefPreventionPlugin.instance.worldEditProvider != null && playerData != null
                && GriefPreventionPlugin.instance.worldEditProvider.hasCUISupport(playerData.getPlayerName())) {
            playerData.showVisualFillers = false;
            STEP = 0;
        }

        if (this.useCuboidVisual()) {
            this.addVisuals3D(claim, playerData);
        } else {
            this.addVisuals2D(claim, height, liquidTransparent);
        }
    }

    public void addVisuals3D(GPClaim claim, GPPlayerData playerData) {
        World world = claim.world;

        this.addTopLine(world, this.smally, this.cornerMaterial, this.accentMaterial);
        this.addTopLine(world, this.bigy, this.cornerMaterial, this.accentMaterial);
        this.addBottomLine(world, this.smally, this.cornerMaterial, this.accentMaterial);
        this.addBottomLine(world, this.bigy, this.cornerMaterial, this.accentMaterial);
        this.addLeftLine(world, this.smally, this.cornerMaterial, this.accentMaterial);
        this.addLeftLine(world, this.bigy, this.cornerMaterial, this.accentMaterial);
        this.addRightLine(world, this.smally, this.cornerMaterial, this.accentMaterial);
        this.addRightLine(world, this.bigy, this.cornerMaterial, this.accentMaterial);
        // don't show corners while subdividing
        if (playerData == null || (playerData.claimSubdividing == null)) {
            // top corners
            this.addCorners(world, this.bigy - 1, this.accentMaterial);
            // bottom corners
            this.addCorners(world, this.smally + 1, this.accentMaterial);
        }

        if (STEP != 0 && (playerData == null || playerData.showVisualFillers)) {
            for (int y = this.smally + STEP; y < this.bigy - STEP / 2; y += STEP) {
                this.addTopLine(world, y, fillerMaterial, fillerMaterial);
            }
            for (int y = this.smally + STEP; y < this.bigy - STEP / 2; y += STEP) {
                this.addBottomLine(world, y, fillerMaterial, fillerMaterial);
            }
            for (int y = this.smally + STEP; y < this.bigy - STEP / 2; y += STEP) {
                this.addLeftLine(world, y, fillerMaterial, fillerMaterial);
            }
            for (int y = this.smally + STEP; y < this.bigy - STEP / 2; y += STEP) {
                this.addRightLine(world, y, fillerMaterial, fillerMaterial);
            }
        }
        this.elements.addAll(newElements);
    }

    public void addVisuals2D(GPClaim claim, int height, boolean liquidTransparent) {
        World world = claim.world;
        this.addTopLine(world, 0, this.cornerMaterial, this.accentMaterial);
        this.addBottomLine(world, 0, this.cornerMaterial, this.accentMaterial);
        this.addLeftLine(world, 0, this.cornerMaterial, this.accentMaterial);
        this.addRightLine(world, 0, this.cornerMaterial, this.accentMaterial);

        // remove any out of range elements
        this.removeElementsOutOfRange(this.newElements, this.minx, this.minz, this.maxx, this.maxz);

        for (int i = 0; i < this.newElements.size(); i++) {
            BlockSnapshot element = this.newElements.get(i).getFinal();
            if (!claim.contains(element.getLocation().get())) {
                this.newElements.remove(i);
            }
        }

        // set Y values and real block information for any remaining visualization blocks
        ArrayList<Transaction<BlockSnapshot>> actualElements = new ArrayList<Transaction<BlockSnapshot>>();
        for (Transaction<BlockSnapshot> element : this.newElements) {
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
                snapshotBuilder.from(new Location<World>(world, this.smallx, y, this.bigz)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(corner1.getLocation().get().createSnapshot(), corner1));
        BlockSnapshot corner2 =
                snapshotBuilder.from(new Location<World>(world, this.bigx, y, this.bigz)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(corner2.getLocation().get().createSnapshot(), corner2));
        BlockSnapshot corner3 =
                snapshotBuilder.from(new Location<World>(world, this.bigx, y, this.smallz)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(corner3.getLocation().get().createSnapshot(), corner3));
        BlockSnapshot corner4 =
                snapshotBuilder.from(new Location<World>(world, this.smallx, y, this.smallz)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(corner4.getLocation().get().createSnapshot(), corner4));
    }

    public void addTopLine(World world, int y, BlockType cornerMaterial, BlockType accentMaterial) {
        BlockSnapshot topVisualBlock1 =
                snapshotBuilder.from(new Location<World>(world, this.smallx, y, this.bigz)).blockState(cornerMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(topVisualBlock1.getLocation().get().createSnapshot(), topVisualBlock1));
        this.corners.add(topVisualBlock1.getPosition());
        BlockSnapshot topVisualBlock2 =
                snapshotBuilder.from(new Location<World>(world, this.smallx + 1, y, this.bigz)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(topVisualBlock2.getLocation().get().createSnapshot(), topVisualBlock2));
        BlockSnapshot topVisualBlock3 =
                snapshotBuilder.from(new Location<World>(world, this.bigx - 1, y, this.bigz)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(topVisualBlock3.getLocation().get().createSnapshot(), topVisualBlock3));

        if (STEP != 0) {
            for (int x = this.smallx + STEP; x < this.bigx - STEP / 2; x += STEP) {
                if ((y != 0 && x >= this.smallx && x <= this.bigx) || (x > this.minx && x < this.maxx)) {
                    BlockSnapshot visualBlock =
                            snapshotBuilder.from(new Location<World>(world, x, y, this.bigz)).blockState(accentMaterial.getDefaultState()).build();
                    newElements.add(new Transaction<BlockSnapshot>(visualBlock.getLocation().get().createSnapshot(), visualBlock));
                }
            }
        }
    }

    public void addBottomLine(World world, int y, BlockType cornerMaterial, BlockType accentMaterial) {
        BlockSnapshot bottomVisualBlock1 =
                this.snapshotBuilder.from(new Location<World>(world, this.smallx + 1, y, this.smallz)).blockState(accentMaterial.getDefaultState()).build();
        this.newElements.add(new Transaction<BlockSnapshot>(bottomVisualBlock1.getLocation().get().createSnapshot(), bottomVisualBlock1));
        this.corners.add(bottomVisualBlock1.getPosition());
        BlockSnapshot bottomVisualBlock2 =
                this.snapshotBuilder.from(new Location<World>(world, this.bigx - 1, y, this.smallz)).blockState(accentMaterial.getDefaultState()).build();
        this.newElements.add(new Transaction<BlockSnapshot>(bottomVisualBlock2.getLocation().get().createSnapshot(), bottomVisualBlock2));

        if (STEP != 0) {
            for (int x = this.smallx + STEP; x < this.bigx - STEP / 2; x += STEP) {
                if ((y != 0 && x >= this.smallx && x <= this.bigx) || (x > this.minx && x < this.maxx)) {
                    BlockSnapshot visualBlock =
                            this.snapshotBuilder.from(new Location<World>(world, x, y, this.smallz)).blockState(accentMaterial.getDefaultState()).build();
                    newElements.add(new Transaction<BlockSnapshot>(visualBlock.getLocation().get().createSnapshot(), visualBlock));
                }
            }
        }
    }

    public void addLeftLine(World world, int y, BlockType cornerMaterial, BlockType accentMaterial) {
        BlockSnapshot leftVisualBlock1 =
                snapshotBuilder.from(new Location<World>(world, this.smallx, y, this.smallz)).blockState(cornerMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(leftVisualBlock1.getLocation().get().createSnapshot(), leftVisualBlock1));
        this.corners.add(leftVisualBlock1.getPosition());
        BlockSnapshot leftVisualBlock2 =
                snapshotBuilder.from(new Location<World>(world, this.smallx, y, this.smallz + 1)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(leftVisualBlock2.getLocation().get().createSnapshot(), leftVisualBlock2));
        BlockSnapshot leftVisualBlock3 =
                snapshotBuilder.from(new Location<World>(world, this.smallx, y, this.bigz - 1)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(leftVisualBlock3.getLocation().get().createSnapshot(), leftVisualBlock3));

        if (STEP != 0) {
            for (int z = this.smallz + STEP; z < this.bigz - STEP / 2; z += STEP) {
                if ((y != 0 && z >= this.smallz && z <= this.bigz) || (z > this.minz && z < this.maxz)) {
                    BlockSnapshot visualBlock =
                            snapshotBuilder.from(new Location<World>(world, this.smallx, y, z)).blockState(accentMaterial.getDefaultState()).build();
                    newElements.add(new Transaction<BlockSnapshot>(visualBlock.getLocation().get().createSnapshot(), visualBlock));
               }
            }
        }
    }

    public void addRightLine(World world, int y, BlockType cornerMaterial, BlockType accentMaterial) {
        BlockSnapshot rightVisualBlock1 =
                snapshotBuilder.from(new Location<World>(world, this.bigx, y, this.smallz)).blockState(cornerMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(rightVisualBlock1.getLocation().get().createSnapshot(), rightVisualBlock1));
        this.corners.add(rightVisualBlock1.getPosition());
        BlockSnapshot rightVisualBlock2 =
                snapshotBuilder.from(new Location<World>(world, this.bigx, y, this.smallz + 1)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(rightVisualBlock2.getLocation().get().createSnapshot(), rightVisualBlock2));
        if (STEP != 0) {
            for (int z = this.smallz + STEP; z < this.bigz - STEP / 2; z += STEP) {
                if ((y != 0 && z >= this.smallz && z <= this.bigz) || (z > this.minz && z < this.maxz)) {
                    BlockSnapshot visualBlock =
                            snapshotBuilder.from(new Location<World>(world, this.bigx, y, z)).blockState(accentMaterial.getDefaultState()).build();
                    newElements.add(new Transaction<BlockSnapshot>(visualBlock.getLocation().get().createSnapshot(), visualBlock));
                }
            }
        }
        BlockSnapshot rightVisualBlock3 =
                snapshotBuilder.from(new Location<World>(world, this.bigx, y, this.bigz - 1)).blockState(accentMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(rightVisualBlock3.getLocation().get().createSnapshot(), rightVisualBlock3));
        BlockSnapshot rightVisualBlock4 =
                snapshotBuilder.from(new Location<World>(world, this.bigx, y, this.bigz)).blockState(cornerMaterial.getDefaultState()).build();
        newElements.add(new Transaction<BlockSnapshot>(rightVisualBlock4.getLocation().get().createSnapshot(), rightVisualBlock4));
        this.corners.add(rightVisualBlock4.getPosition());
    }

    public List<Transaction<BlockSnapshot>> getVisualElements() {
        return this.elements;
    }

    public List<Vector3i> getVisualCorners() {
        return this.corners;
    }

    // removes any elements which are out of visualization range
    private void removeElementsOutOfRange(ArrayList<Transaction<BlockSnapshot>> elements, int minx, int minz, int maxx, int maxz) {
        for (int i = 0; i < elements.size(); i++) {
            Location<World> location = elements.get(i).getFinal().getLocation().get();
            if (location.getX() < minx || location.getX() > maxx || location.getZ() < minz || location.getZ() > maxz) {
                elements.remove(i);
            }
        }
    }

    // finds a block the player can probably see. this is how visualizations "cling" to the ground or ceiling
    private static Location<World> getVisibleLocation(World world, int x, int y, int z, boolean waterIsTransparent) {
        Location<World> location = world.getLocation(x, y, z);
        Direction direction = (isTransparent(location.getBlock(), waterIsTransparent)) ? Direction.DOWN : Direction.UP;

        while (location.getPosition().getY() >= 1 &&
                location.getPosition().getY() < world.getDimension().getBuildHeight() - 1 &&
                (!isTransparent(location.getRelative(Direction.UP).getBlock(), waterIsTransparent)
                        || isTransparent(location.getBlock(), waterIsTransparent))) {
            location = location.getRelative(direction);
        }

        return location;
    }

    // helper method for above. allows visualization blocks to sit underneath partly transparent blocks like grass and fence
    private static boolean isTransparent(BlockState blockstate, boolean waterIsTransparent) {
        if (blockstate.getType() == BlockTypes.SNOW_LAYER) {
            return false;
        }

        IBlockState iblockstate = (IBlockState)(Object) blockstate;
        Optional<MatterProperty> matterProperty = blockstate.getProperty(MatterProperty.class);
        if (!waterIsTransparent && matterProperty.isPresent() && matterProperty.get().getValue() == MatterProperty.Matter.LIQUID) {
            return false;
        }
        return !iblockstate.isOpaqueCube();
    }

    public static Visualization fromClaims(List<Claim> claims, int height, Location<World> locality, GPPlayerData playerData, Visualization visualization) {
        if (visualization == null) {
            visualization = new Visualization(VisualizationType.CLAIM);
        }

        for (Claim claim : claims) {
            GPClaim gpClaim = (GPClaim) claim;
            if (!gpClaim.children.isEmpty()) {
                fromClaims(gpClaim.children, height, locality, playerData, visualization);
            }
            if (gpClaim.visualization != null) {
                visualization.elements.addAll(gpClaim.getVisualizer().elements);
            } else {
                visualization.createClaimBlockVisualWithType(gpClaim, height, locality, playerData, Visualization.getVisualizationType(gpClaim));
            }
        }

        return visualization;
    }

    private boolean useCuboidVisual() {
        if (this.claim.cuboid) {
            return true;
        }

        final GPPlayerData ownerData = this.claim.getOwnerPlayerData();
        if (ownerData != null && (ownerData.getMinClaimLevel() > 0 || ownerData.getMaxClaimLevel() < 255)) {
            return true;
        }
        // Claim could of been created with different min/max levels, so check Y values
        if (this.claim.getLesserBoundaryCorner().getBlockY() > 0 || this.claim.getGreaterBoundaryCorner().getBlockY() < 255) {
            return true;
        }

        return false;
    }
}
