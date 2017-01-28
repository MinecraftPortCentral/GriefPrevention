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
package me.ryanhamshire.griefprevention.claim;

import static com.google.common.base.Preconditions.checkNotNull;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import me.ryanhamshire.griefprevention.DataStore;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.SiegeData;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimResult;
import me.ryanhamshire.griefprevention.api.claim.ClaimResultType;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.api.claim.TrustType;
import me.ryanhamshire.griefprevention.api.data.ClaimData;
import me.ryanhamshire.griefprevention.configuration.ClaimStorageData;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.configuration.IClaimData;
import me.ryanhamshire.griefprevention.configuration.SubDivisionDataConfig;
import me.ryanhamshire.griefprevention.event.GPCreateClaimEvent;
import me.ryanhamshire.griefprevention.event.GPDeleteClaimEvent;
import me.ryanhamshire.griefprevention.event.GPResizeClaimEvent;
import me.ryanhamshire.griefprevention.event.GPTransferClaimEvent;
import me.ryanhamshire.griefprevention.event.GPTrustClaimEvent;
import me.ryanhamshire.griefprevention.message.Messages;
import me.ryanhamshire.griefprevention.permission.GPPermissionHandler;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import me.ryanhamshire.griefprevention.util.BlockUtils;
import me.ryanhamshire.griefprevention.visual.Visualization;
import me.ryanhamshire.griefprevention.visual.VisualizationType;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.property.block.MatterProperty;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.interfaces.world.IMixinWorldServer;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

//represents a player claim
//creating an instance doesn't make an effective claim
//only claims which have been added to the datastore have any effect
public class GPClaim implements Claim {

    public static final DataStore DATASTORE = GriefPreventionPlugin.instance.dataStore;
    // two locations, which together define the boundaries of the claim
    // Note: 2D cuboids will ignore the upper Y value while 3D cuboids do not
    public Location<World> lesserBoundaryCorner;
    public Location<World> greaterBoundaryCorner;
    public World world;
    public ClaimType type = ClaimType.BASIC;
    private Set<Long> chunkHashes;
    private final int hashCode;

    // Permission Context
    public Context context;

    // id number. unique to this claim, never changes.
    public UUID id = null;

    private UUID ownerUniqueId;

    public boolean cuboid = false;

    private ClaimStorageData claimStorage;
    private IClaimData claimData;

    // parent claim
    // only used for claim subdivisions. top level claims have null here
    public GPClaim parent = null;

    // children (subdivisions)
    // note subdivisions themselves never have children
    public ArrayList<Claim> children = new ArrayList<>();

    // information about a siege involving this claim. null means no siege is impacting this claim
    public SiegeData siegeData = null;

    // following a siege, buttons/levers are unlocked temporarily. This represents that state
    public boolean doorsOpen = false;

    public Visualization visualization;
    public List<UUID> playersWatching = new ArrayList<>();

    private GPPlayerData ownerPlayerData;

    public GPClaim(World world, Vector3i point1, Vector3i point2, ClaimType type, UUID ownerUniqueId, boolean cuboid) {
        this(world, point1, point2, type, ownerUniqueId, cuboid, null);
    }

    public GPClaim(World world, Vector3i point1, Vector3i point2, ClaimType type, UUID ownerUniqueId, boolean cuboid, GPClaim parent) {
        int smallx, bigx, smally, bigy, smallz, bigz;
        int x1 = point1.getX();
        int x2 = point2.getX();
        int y1 = point1.getY();
        int y2 = point2.getY();
        int z1 = point1.getZ();
        int z2 = point2.getZ();
        // determine small versus big inputs
        if (x1 < x2) {
            smallx = x1;
            bigx = x2;
        } else {
            smallx = x2;
            bigx = x1;
        }

        if (y1 < y2) {
            smally = y1;
            bigy = y2;
        } else {
            smally = y2;
            bigy = y1;
        }

        if (z1 < z2) {
            smallz = z1;
            bigz = z2;
        } else {
            smallz = z2;
            bigz = z1;
        }

        // creative mode claims always go to bedrock
        if (GriefPreventionPlugin.instance.claimModeIsActive(world.getProperties(), ClaimsMode.Creative)) {
            smally = 2;
        }

        this.world = world;
        this.lesserBoundaryCorner = new Location<World>(world, smallx, smally, smallz);
        this.greaterBoundaryCorner = new Location<World>(world, bigx, bigy, bigz);
        this.ownerUniqueId = ownerUniqueId;
        this.type = type;
        this.id = UUID.randomUUID();
        this.context = new Context("gp_claim", this.id.toString());
        this.cuboid = cuboid;
        this.parent = parent;
        this.hashCode = this.id.hashCode();
    }

    // Used for visualizations
    public GPClaim(Location<World> lesserBoundaryCorner, Location<World> greaterBoundaryCorner, ClaimType type, boolean cuboid) {
        this(lesserBoundaryCorner, greaterBoundaryCorner, UUID.randomUUID(), type, null);
        this.cuboid = cuboid;
    }

    // Used at server startup
    public GPClaim(Location<World> lesserBoundaryCorner, Location<World> greaterBoundaryCorner, UUID claimId, ClaimType type, UUID ownerUniqueId) {
        // id
        this.id = claimId;

        // store corners
        this.lesserBoundaryCorner = lesserBoundaryCorner;
        this.greaterBoundaryCorner = greaterBoundaryCorner;
        this.world = lesserBoundaryCorner.getExtent();
        if (ownerUniqueId != null) {
            this.ownerUniqueId = ownerUniqueId;
            this.ownerPlayerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(this.world, this.ownerUniqueId);
        }
        this.type = type;
        this.context = new Context("gp_claim", this.id.toString());
        this.hashCode = this.id.hashCode();
    }

    public void initializeClaimData(GPClaim parent) {
        if (parent != null) {
            this.parent = parent;
            this.claimData = new SubDivisionDataConfig(this);
            this.claimStorage = this.parent.getClaimStorage();
            this.claimStorage.getConfig().getSubdivisions().put(this.id, (SubDivisionDataConfig) this.claimData);
        } else {
            Path claimDataFolderPath = null;
            // check if main world
            claimDataFolderPath = DataStore.worldConfigMap.get(this.world.getUniqueId()).getPath().getParent().resolve("ClaimData");
            File claimFile = new File(claimDataFolderPath + File.separator + this.id);
            this.claimStorage = new ClaimStorageData(claimFile.toPath(), this.id, this.ownerUniqueId, this.type, cuboid);
            this.claimData = this.claimStorage.getConfig();
        }

        this.updateClaimStorageData();
    }

    public ClaimType getType() {
        return this.type;
    }

    public Visualization getVisualizer() {
        if (this.visualization == null) {
            this.visualization = new Visualization(this, Visualization.getVisualizationType(this));
        }
        return this.visualization;
    }

    public GPPlayerData getOwnerPlayerData() {
        if (this.ownerPlayerData == null && this.ownerUniqueId != null) {
            this.ownerPlayerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(this.world, this.ownerUniqueId);
        }

        return this.ownerPlayerData;
    }

    public UUID getOwnerUniqueId() {
        if (this.isSubdivision()) {
            return this.parent.getOwnerUniqueId();
        }
        if (this.isAdminClaim()) {
            return GriefPreventionPlugin.ADMIN_USER_UUID;
        }

        return this.ownerUniqueId;
    }

    public void setOwnerUniqueId(UUID uniqueId) {
        this.ownerUniqueId = uniqueId;
    }

    // whether or not this is an administrative claim
    // administrative claims are created and maintained by players with the
    // griefprevention.adminclaims permission.
    public boolean isAdminClaim() {
        if (this.parent != null) {
            return this.parent.isAdminClaim();
        }

        return this.type == ClaimType.ADMIN;
    }

    public boolean isBasicClaim() {
        return this.type == ClaimType.BASIC;
    }

    public boolean isSubdivision() {
        return this.parent != null;
    }

    public boolean isWildernessClaim() {
        return this.type == ClaimType.WILDERNESS;
    }

    public boolean isCuboid() {
        if (this.claimData != null) {
            return this.claimData.isCuboid();
        }

        return this.cuboid;
    }

    // accessor for ID
    public UUID getUniqueId() {
        return this.id;
    }

    public Optional<Text> getName() {
        return this.getInternalClaimData().getName();
    }

    // players may only siege someone when he's not in an admin claim
    // and when he has some level of permission in the claim
    public boolean canSiege(Player defender) {
        if (this.isAdminClaim()) {
            return false;
        }

        if (this.allowAccess(defender) != null) {
            return false;
        }

        return true;
    }

    // removes any lava above sea level in a claim
    // exclusionClaim is another claim indicating an sub-area to be excluded
    // from this operation
    // it may be null
    public void removeSurfaceFluids(GPClaim exclusionClaim) {
        // don't do this for administrative claims
        if (this.isAdminClaim()) {
            return;
        }

        // don't do it for very large claims
        if (this.getArea() > 10000) {
            return;
        }

        // only in creative mode worlds
        if (!GriefPreventionPlugin.instance.claimModeIsActive(this.lesserBoundaryCorner.getExtent().getProperties(), ClaimsMode.Creative)) {
            return;
        }

        Location<World> lesser = this.getLesserBoundaryCorner();
        Location<World> greater = this.getGreaterBoundaryCorner();

        if (lesser.getExtent().getDimension().getType().equals(DimensionTypes.NETHER)) {
            return; // don't clean up lava in the nether
        }

        int seaLevel = 0; // clean up all fluids in the end

        // respect sea level in normal worlds
        if (lesser.getExtent().getDimension().getType().equals(DimensionTypes.OVERWORLD)) {
            seaLevel = GriefPreventionPlugin.instance.getSeaLevel(lesser.getExtent());
        }

        for (int x = lesser.getBlockX(); x <= greater.getBlockX(); x++) {
            for (int z = lesser.getBlockZ(); z <= greater.getBlockZ(); z++) {
                for (int y = seaLevel - 1; y <= lesser.getExtent().getDimension().getBuildHeight(); y++) {
                    // dodge the exclusion claim
                    BlockSnapshot block = lesser.getExtent().createSnapshot(x, y, z);
                    if (exclusionClaim != null && exclusionClaim.contains(block.getLocation().get(), true, false)) {
                        continue;
                    }

                    if (block.getState().getType() == BlockTypes.LAVA || block.getState().getType() == BlockTypes.FLOWING_WATER
                            || block.getState().getType() == BlockTypes.WATER || block.getState().getType() == BlockTypes.FLOWING_LAVA) {
                        block.withState(BlockTypes.AIR.getDefaultState()).restore(true, BlockChangeFlag.PHYSICS);
                    }
                }
            }
        }
    }

    // determines whether or not a claim has surface lava
    // used to warn players when they abandon their claims about automatic fluid
    // cleanup
    boolean hasSurfaceFluids() {
        Location<World> lesser = this.getLesserBoundaryCorner();
        Location<World> greater = this.getGreaterBoundaryCorner();

        // don't bother for very large claims, too expensive
        if (this.getArea() > 10000) {
            return false;
        }

        int seaLevel = 0; // clean up all fluids in the end

        // respect sea level in normal worlds
        if (lesser.getExtent().getDimension().getType().equals(DimensionTypes.OVERWORLD)) {
            seaLevel = GriefPreventionPlugin.instance.getSeaLevel(lesser.getExtent());
        }

        for (int x = lesser.getBlockX(); x <= greater.getBlockX(); x++) {
            for (int z = lesser.getBlockZ(); z <= greater.getBlockZ(); z++) {
                for (int y = seaLevel - 1; y <= lesser.getExtent().getDimension().getBuildHeight(); y++) {
                    // dodge the exclusion claim
                    BlockState block = lesser.getExtent().getBlock(x, y, z);

                    if (block.getType() == BlockTypes.WATER || block.getType() == BlockTypes.FLOWING_WATER
                            || block.getType() == BlockTypes.LAVA || block.getType() == BlockTypes.FLOWING_LAVA) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    // measurements. all measurements are in blocks
    @Override
    public int getArea() {
        int claimWidth = this.greaterBoundaryCorner.getBlockX() - this.lesserBoundaryCorner.getBlockX() + 1;
        int claimHeight = this.greaterBoundaryCorner.getBlockZ() - this.lesserBoundaryCorner.getBlockZ() + 1;

        return claimWidth * claimHeight;
    }

    @Override
    public int getWidth() {
        return this.greaterBoundaryCorner.getBlockX() - this.lesserBoundaryCorner.getBlockX() + 1;
    }

    @Override
    public int getHeight() {
        return this.greaterBoundaryCorner.getBlockZ() - this.lesserBoundaryCorner.getBlockZ() + 1;
    }

    public boolean hasFullAccess(User user) {
        return this.hasFullAccess(user, null);
    }

    public boolean hasFullAccess(User user, GPPlayerData playerData) {
        if (playerData == null) {
            playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(this.world, user.getUniqueId());
        }
        if (playerData != null && playerData.canIgnoreClaim(this)) {
            return true;
        }

        if (this.isAdminClaim() && playerData.canManageAdminClaims) {
            if (playerData.debugClaimPermissions) {
                return false;
            }

            return true;
        }

        // owner
        if (!this.isAdminClaim() && user.getUniqueId().equals(this.getOwnerUniqueId())) {
            // only check debug claim permissions if owner
            if (playerData.debugClaimPermissions) {
                return false;
            }

            return true;
        }

        if (this.isWildernessClaim() && playerData.canManageWilderness) {
            if (playerData.debugClaimPermissions) {
                return false;
            }

            return true;
        }

        // if subdivision
        if (this.parent != null) {
            return this.parent.hasFullAccess(user);
        }

        return false;
    }

    // similar to hasFullAccess except it doesn't get checked by delete/abandon claims
    public boolean hasFullTrust(User user) {
        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(this.world, user.getUniqueId());
        if (playerData != null && playerData.canIgnoreClaim(this)) {
            return true;
        }

        if (this.isAdminClaim() && playerData.canManageAdminClaims) {
            if (playerData.debugClaimPermissions) {
                return false;
            }

            return true;
        }

        // owner
        if (!this.isAdminClaim() && user.getUniqueId().equals(this.getOwnerUniqueId())) {
            // only check debug claim permissions if owner
            if (playerData.debugClaimPermissions) {
                return false;
            }

            return true;
        }

        if (this.isWildernessClaim() && playerData.canManageWilderness) {
            if (playerData.debugClaimPermissions) {
                return false;
            }

            return true;
        }

        // Builders can place blocks in claims
        if (this.claimData.getBuilders().contains(GriefPreventionPlugin.PUBLIC_UUID) || this.claimData.getBuilders().contains(user.getUniqueId())) {
            return true;
        }

        // if subdivision
        if (this.parent != null) {
            if (!this.getData().doesInheritParent()) {
                // check if parent owner
                return this.parent.hasFullAccess(user);
            }
            return this.parent.hasFullTrust(user);
        }

        return false;
    }

    // permissions. note administrative "public" claims have different rules than other claims
    // all of these return NULL when a player has permission, or a String error
    // message when the player doesn't have permission
    public String allowEdit(Player player) {
        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(this.world, player.getUniqueId());
        if (this.hasFullAccess(player, playerData)) {
            return null;
        }

        // anyone with deleteclaims permission can modify non-admin claims at any time
        else {
            if (player.hasPermission(GPPermissions.COMMAND_DELETE_CLAIMS)) {
                return null;
            }
        }

        // no resizing, deleting, and so forth while under siege
        // don't use isManager here as only owners may edit claims
        if (player.getUniqueId().equals(this.getOwnerUniqueId())) {
            if (this.siegeData != null) {
                return GriefPreventionPlugin.instance.dataStore.getMessage(Messages.NoModifyDuringSiege);
            }

            // otherwise, owners can do whatever
            return null;
        }

        if (this.isWildernessClaim() && playerData.canManageWilderness) {
            return null;
        }

        // permission inheritance for subdivisions
        if (this.parent != null && this.getData().doesInheritParent()) {
            return this.parent.allowEdit(player);
        }

        // error message if all else fails
        return GriefPreventionPlugin.instance.dataStore.getMessage(Messages.OnlyOwnersModifyClaims, this.getOwnerName());
    }

    // build permission check
    public String allowBuild(Object source, Location<World> location, User user) {
        if (user == null) {
            return null;
        }

        // when a player tries to build in a claim, if he's under siege, the
        // siege may extend to include the new claim
        if (user instanceof Player) {
            GriefPreventionPlugin.instance.dataStore.tryExtendSiege((Player) user, this);
        }

        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(location.getExtent(), user.getUniqueId());
        // admin claims can always be modified by admins, no exceptions
        if (this.isAdminClaim()) {
            if (playerData.canManageAdminClaims) {
                return null;
            }
        }

        // no building while under siege
        if (this.siegeData != null) {
            return GriefPreventionPlugin.instance.dataStore.getMessage(Messages.NoBuildUnderSiege, this.siegeData.attacker.getName());
        }

        // no building while in pvp combat
        if (playerData.inPvpCombat(location.getExtent())) {
            return GriefPreventionPlugin.instance.dataStore.getMessage(Messages.NoBuildPvP);
        }

        // owners can make changes, or admins with ignore claims mode enabled
        if (hasFullAccess(user)) {
            return null;
        }

        if (!(source instanceof Player)) {
            if (location.getBlock().getType() == BlockTypes.FIRE) {
                Tristate value = GPPermissionHandler.getClaimPermission(this, GPPermissions.FIRE_SPREAD, source, location.getBlock(), user);
                if (value == Tristate.TRUE) {
                    return null;
                } else if (value == Tristate.FALSE) {
                    return "You do not have permission to spread fire in this claim.";
                }
            }
        }

        // Builders can place blocks in claims
        if (this.claimData.getBuilders().contains(GriefPreventionPlugin.PUBLIC_UUID) || this.claimData.getBuilders().contains(user.getUniqueId())) {
            return null;
        }

        if (!(source instanceof Player)) {
            Optional<MatterProperty> matterProperty = location.getProperty(MatterProperty.class);
            if (matterProperty.isPresent() && matterProperty.get().getValue() == MatterProperty.Matter.LIQUID) {
                Tristate value = GPPermissionHandler.getClaimPermission(this, GPPermissions.LIQUID_FLOW, source, location.getBlock(), user);
                if (value == Tristate.TRUE) {
                    return null;
                } else if (value == Tristate.FALSE) {
                    return "You do not have permission to flow liquid in this claim.";
                }
            }
        }

        // anyone with explicit build permission can make changes
        if (GPPermissionHandler.getClaimPermission(this, GPPermissions.BLOCK_PLACE, source, location.getBlock(),user) == Tristate.TRUE) {
            return null;
        }
        // subdivision permission inheritance
        if (this.parent != null && this.getData().doesInheritParent()) {
            return this.parent.allowBuild(source, location, user);
        }

        // failure message for all other cases
        String reason = "";
        if (location.getBlock().getType() != BlockTypes.FLOWING_WATER && location.getBlock().getType() != BlockTypes.FLOWING_LAVA) {
            reason = GriefPreventionPlugin.instance.dataStore.getMessage(Messages.NoBuildPermission, this.getOwnerName());
        }

        return reason;
    }

    // A blocksnapshot must be passed here instead of location as the block in world represents the "final" block
    public String allowBreak(Object source, BlockSnapshot blockSnapshot, User user) {
        Location<World> location = blockSnapshot.getLocation().orElse(null);
        if (location == null) {
            return null;
        }

        // if under siege, some blocks will be breakable
        if (this.siegeData != null || this.doorsOpen) {
            boolean breakable = false;

            // search for block type in list of breakable blocks
            for (int i = 0; i < GriefPreventionPlugin.getActiveConfig(location.getExtent().getProperties()).getConfig().siege.breakableSiegeBlocks.size();
                 i++) {
                String blockTypeId =
                        GriefPreventionPlugin.getActiveConfig(location.getExtent().getProperties()).getConfig().siege.breakableSiegeBlocks.get(i);
                Optional<BlockType> breakableBlockType = Sponge.getGame().getRegistry().getType(BlockType.class, blockTypeId);
                if (breakableBlockType.isPresent() && breakableBlockType.get() == location.getBlockType()) {
                    breakable = true;
                    break;
                }
            }

            // custom error messages for siege mode
            if (!breakable) {
                return GriefPreventionPlugin.instance.dataStore.getMessage(Messages.NonSiegeMaterial);
            } else if (user != null && hasFullAccess(user)) {
                return GriefPreventionPlugin.instance.dataStore.getMessage(Messages.NoOwnerBuildUnderSiege);
            } else {
                return null;
            }
        }

        String reason = GriefPreventionPlugin.instance.dataStore.getMessage(Messages.NoBuildPermission, this.getOwnerName());
        if (user != null) {
            if (hasFullAccess(user)) {
                return null;
            }

            // Builders can break blocks
            if (this.getInternalClaimData().getBuilders().contains(GriefPreventionPlugin.PUBLIC_UUID) || this.getInternalClaimData().getBuilders().contains(user.getUniqueId())) {
                return null;
            }

            // Flag order matters
            // interact should always be checked before break
            // pass the blocksnapshot here as the live location represents the final transaction which would be AIR at this point
            if (GPPermissionHandler.getClaimPermission(this, GPPermissions.BLOCK_BREAK, source, blockSnapshot, user) == Tristate.TRUE) {
                return null;
            }
        }

        return reason;
    }

    public String allowAccess(User user) {
        return allowAccess(user, null);
    }

    public String allowAccess(User user, Location<World> location) {
        return allowAccess(user, location, false);
    }

    // access permission check
    public String allowAccess(User user, Location<World> location, boolean interact) {
        if (user == null) {
            return "";
        }
        // following a siege where the defender lost, the claim will allow everyone access for a time
        if (this.doorsOpen) {
            return null;
        }

        // claim owner and admins in ignoreclaims mode have access
        if (hasFullAccess(user)) {
            return null;
        }

        if (this.getInternalClaimData().getAccessors().contains(GriefPreventionPlugin.PUBLIC_UUID)
                || this.getInternalClaimData().getBuilders().contains(GriefPreventionPlugin.PUBLIC_UUID) 
                || this.getInternalClaimData().getContainers().contains(GriefPreventionPlugin.PUBLIC_UUID) 
                || this.getInternalClaimData().getBuilders().contains(user.getUniqueId()) 
                || this.getInternalClaimData().getContainers().contains(user.getUniqueId())
                || this.getInternalClaimData().getAccessors().contains(user.getUniqueId())) {
            return null;
        }

        if (interact) {
            if (GPPermissionHandler.getClaimPermission(this, GPPermissions.INTERACT_BLOCK_SECONDARY, user, location.getBlock(), user) == Tristate.TRUE) {
                return null;
            }
        }

        // permission inheritance for subdivisions
        if (this.parent != null && this.getData().doesInheritParent()) {
            return this.parent.allowAccess(user, location);
        }

        //catch-all error message for all other cases
        String reason = GriefPreventionPlugin.instance.dataStore.getMessage(Messages.NoAccessPermission, this.getOwnerName());
        return reason;
    }

    public String allowItemDrop(User user, Location<World> location) {
        // claim owner and admins in ignoreclaims mode have access
        if (hasFullAccess(user)) {
            return null;
        }

        if (this.getInternalClaimData().getBuilders().contains(GriefPreventionPlugin.PUBLIC_UUID) 
                || this.getInternalClaimData().getBuilders().contains(user.getUniqueId())) {
            return null;
        }

        if (GPPermissionHandler.getClaimPermission(this, GPPermissions.BLOCK_BREAK, user, location.getBlock(), user) == Tristate.TRUE) {
            return null;
        }

        // permission inheritance for subdivisions
        if (this.parent != null && this.getData().doesInheritParent()) {
            return this.parent.allowAccess(user, location);
        }

        //catch-all error message for all other cases
        String reason = GriefPreventionPlugin.instance.dataStore.getMessage(Messages.NoDropsAllowed, this.getOwnerName());
        return reason;
    }

    public String allowContainers(User user, Location<World> location) {
        //trying to access inventory in a claim may extend an existing siege to include this claim
        if (user instanceof Player) {
            GriefPreventionPlugin.instance.dataStore.tryExtendSiege((Player) user, this);
        }
        
        //if under siege, nobody accesses containers
        if(this.siegeData != null) {
            return GriefPreventionPlugin.instance.dataStore.getMessage(Messages.NoContainersSiege, siegeData.attacker.getName());
        }
        
        // claim owner and admins in ignoreclaims mode have access
        if (hasFullAccess(user)) {
            return null;
        }

        if (this.getInternalClaimData().getBuilders().contains(GriefPreventionPlugin.PUBLIC_UUID) 
                || this.getInternalClaimData().getContainers().contains(GriefPreventionPlugin.PUBLIC_UUID) 
                || this.getInternalClaimData().getBuilders().contains(user.getUniqueId()) 
                || this.getInternalClaimData().getContainers().contains(user.getUniqueId())) {
            return null;
        }

        if (GPPermissionHandler.getClaimPermission(this, GPPermissions.INTERACT_BLOCK_SECONDARY, user, location.getBlock(), user) == Tristate.TRUE) {
            return null;
        }

        //permission inheritance for subdivisions
        if(this.parent != null && this.getData().doesInheritParent()) {
            return this.parent.allowContainers(user, location);
        }

        //error message for all other cases
        String reason = GriefPreventionPlugin.instance.dataStore.getMessage(Messages.NoContainersPermission, this.getOwnerName());
        return reason;
    }

    //grant permission check, relatively simple
    public String allowGrantPermission(Player player) {
        //anyone who can modify the claim can do this
        if(this.allowEdit(player) == null) {
            return null;
        }
        
        //anyone who's in the managers (/PermissionTrust) list can do this
        for(int i = 0; i < this.getInternalClaimData().getManagers().size(); i++) {
            UUID managerID = this.getInternalClaimData().getManagers().get(i);
            if(player.getUniqueId().equals(managerID)) {
                return null;
            }
        }
        
        //permission inheritance for subdivisions
        if(this.parent != null && this.getData().doesInheritParent()) {
            return this.parent.allowGrantPermission(player);
        }
        
        //generic error message
        String reason = GriefPreventionPlugin.instance.dataStore.getMessage(Messages.NoPermissionTrust, this.getOwnerName());

        return reason;
    }

    //clears all permissions (except owner of course)
    public void clearPermissions() {
        this.getInternalClaimData().getManagers().clear();
        
        for(Claim child : this.children) {
            ((GPClaim) child).clearPermissions();
        }
    }

    // returns a copy of the location representing lower x, y, z limits
    public Location<World> getLesserBoundaryCorner() {
        return (Location<World>) this.lesserBoundaryCorner.copy();
    }

    // returns a copy of the location representing upper x, y, z limits
    // NOTE: remember upper Y will always be ignored, all claims always extend to the sky
    public Location<World> getGreaterBoundaryCorner() {
        return (Location<World>) this.greaterBoundaryCorner.copy();
    }

    // returns a friendly owner name (for admin claims, returns "an
    // administrator" as the owner)
    public String getOwnerName() {
        if (this.isWildernessClaim()) {
            return GriefPreventionPlugin.instance.dataStore.getMessage(Messages.OwnerNameForWildernessClaims);
        }

        if (this.isAdminClaim()) {
            return GriefPreventionPlugin.instance.dataStore.getMessage(Messages.OwnerNameForAdminClaims);
        }

        if (this.parent != null) {
            return this.parent.getOwnerName();
        }

        if (this.getOwnerPlayerData() == null) {
            return "[unknown]";
        }

        return this.getOwnerPlayerData().getPlayerName();
    }

    // whether or not a location is in a claim
    // ignoreHeight = true means location UNDER the claim will return TRUE
    // excludeSubdivisions = true means that locations inside subdivisions of the claim will return FALSE
    public boolean contains(Location<World> location, boolean ignoreHeight, boolean excludeSubdivisions) {
        if (this.isCuboid()) {
            return this.contains(location);
        }

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        // main check
        boolean inClaim = (ignoreHeight || y >= this.lesserBoundaryCorner.getY()) &&
                x >= this.lesserBoundaryCorner.getBlockX() &&
                x < this.greaterBoundaryCorner.getBlockX() + 1 &&
                z >= this.lesserBoundaryCorner.getBlockZ() &&
                z < this.greaterBoundaryCorner.getBlockZ() + 1;

        if (!inClaim) {
            return false;
        }

        // additional check for subdivisions
        // you're only in a subdivision when you're also in its parent claim
        // NOTE: if a player creates subdivions then resizes the parent claim,
        // it's possible that
        // a subdivision can reach outside of its parent's boundaries. so this
        // check is important!
        if (this.parent != null && this.getData().doesInheritParent()) {
            return this.parent.contains(location, ignoreHeight, false);
        }

        // code to exclude subdivisions in this check
        else if (excludeSubdivisions) {
            // search all subdivisions to see if the location is in any of them
            for (int i = 0; i < this.children.size(); i++) {
                // if we find such a subdivision, return false
                if (this.children.get(i).contains(location, ignoreHeight, true)) {
                    return false;
                }
            }
        }

        // otherwise yes
        return true;
    }

    // 3d cuboid check
    public boolean contains(Location<World> location) {
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        boolean inClaim = (
                x >= this.lesserBoundaryCorner.getBlockX() &&
                x <= this.greaterBoundaryCorner.getBlockX() &&
                y >= this.lesserBoundaryCorner.getBlockY() &&
                y <= this.greaterBoundaryCorner.getBlockY() &&
                z >= this.lesserBoundaryCorner.getBlockZ() &&
                z <= this.greaterBoundaryCorner.getBlockZ());

        if (!inClaim) {
            return false;
        }

        return true;
    }

    @Override
    public boolean overlaps(Claim other) {
        GPClaim otherClaim = (GPClaim) other;
        if (this.id == otherClaim.id) {
            return false;
        }

        if (this.parent != null && otherClaim.parent != null && this.parent.id != otherClaim.parent.id) {
            return true;
        }

        int smallX = otherClaim.getLesserBoundaryCorner().getBlockX();
        int smallY = otherClaim.getLesserBoundaryCorner().getBlockY();
        int smallZ = otherClaim.getLesserBoundaryCorner().getBlockZ();
        int bigX = otherClaim.getGreaterBoundaryCorner().getBlockX();
        int bigY = otherClaim.getGreaterBoundaryCorner().getBlockY();
        int bigZ = otherClaim.getGreaterBoundaryCorner().getBlockZ();

        //verify this claim doesn't band across an existing claim, either horizontally or vertically
        boolean inArea = false;
        if(this.getLesserBoundaryCorner().getBlockZ() <= bigZ &&
           this.getLesserBoundaryCorner().getBlockZ() >= smallZ &&
           this.getLesserBoundaryCorner().getBlockX() < smallX &&
           this.getGreaterBoundaryCorner().getBlockX() > bigX)
           inArea = true;

        if( this.getGreaterBoundaryCorner().getBlockZ() <= bigZ && 
            this.getGreaterBoundaryCorner().getBlockZ() >= smallZ && 
            this.getLesserBoundaryCorner().getBlockX() < smallX &&
            this.getGreaterBoundaryCorner().getBlockX() > bigX )
            inArea = true;
        
        if( this.getLesserBoundaryCorner().getBlockX() <= bigX && 
            this.getLesserBoundaryCorner().getBlockX() >= smallX && 
            this.getLesserBoundaryCorner().getBlockZ() < smallZ &&
            this.getGreaterBoundaryCorner().getBlockZ() > bigZ )
            inArea = true;
            
        if( this.getGreaterBoundaryCorner().getBlockX() <= bigX && 
            this.getGreaterBoundaryCorner().getBlockX() >= smallX && 
            this.getLesserBoundaryCorner().getBlockZ() < smallZ &&
            this.getGreaterBoundaryCorner().getBlockZ() > bigZ )
            inArea = true;

        if (inArea) {
            if (this.cuboid && otherClaim.cuboid) {
                // check height
                if ((this.lesserBoundaryCorner.getBlockY() >= smallY &&
                     this.lesserBoundaryCorner.getBlockY() <= bigY) ||
                    (this.greaterBoundaryCorner.getBlockY() <= smallY &&
                     this.greaterBoundaryCorner.getBlockY() >= smallY)) {
                    return true;
                }

                return false;
            }
            return true;
        }

        return false;
    }

    // whether more entities may be added to a claim
    public String allowMoreEntities() {
        if (this.parent != null) {
            return this.parent.allowMoreEntities();
        }

        // this rule only applies to creative mode worlds
        if (!GriefPreventionPlugin.instance.claimModeIsActive(this.getLesserBoundaryCorner().getExtent().getProperties(), ClaimsMode.Creative)) {
            return null;
        }

        // admin claims aren't restricted
        if (this.isAdminClaim()) {
            return null;
        }

        // don't apply this rule to very large claims
        if (this.getArea() > 10000) {
            return null;
        }

        // determine maximum allowable entity count, based on claim size
        int maxEntities = this.getArea() / 50;
        if (maxEntities == 0) {
            return GriefPreventionPlugin.instance.dataStore.getMessage(Messages.ClaimTooSmallForEntities);
        }

        // count current entities (ignoring players)
        int totalEntities = 0;
        ArrayList<Chunk> chunks = this.getChunks();
        for (Chunk chunk : chunks) {
            ArrayList<Entity> entities = (ArrayList<Entity>) chunk.getEntities();
            for (int i = 0; i < entities.size(); i++) {
                Entity entity = entities.get(i);
                if (!(entity instanceof Player) && this.contains(entity.getLocation(), false, false)) {
                    totalEntities++;
                    if (totalEntities > maxEntities) {
                        entity.remove();
                    }
                }
            }
        }

        if (totalEntities > maxEntities) {
            return GriefPreventionPlugin.instance.dataStore.getMessage(Messages.TooManyEntitiesInClaim);
        }

        return null;
    }

    // implements a strict ordering of claims, used to keep the claims
    // collection sorted for faster searching
    boolean greaterThan(GPClaim otherClaim) {
        Location<World> thisCorner = this.getLesserBoundaryCorner();
        Location<World> otherCorner = otherClaim.getLesserBoundaryCorner();

        if (thisCorner.getBlockX() > otherCorner.getBlockX()) {
            return true;
        }

        if (thisCorner.getBlockX() < otherCorner.getBlockX()) {
            return false;
        }

        if (thisCorner.getBlockZ() > otherCorner.getBlockZ()) {
            return true;
        }

        if (thisCorner.getBlockZ() < otherCorner.getBlockZ()) {
            return false;
        }

        return thisCorner.getExtent().getUniqueId().compareTo(otherCorner.getExtent().getUniqueId()) < 0;
    }

    public ArrayList<Chunk> getChunks() {
        ArrayList<Chunk> chunks = new ArrayList<Chunk>();

        World world = this.getLesserBoundaryCorner().getExtent();
        Chunk lesserChunk = this.getLesserBoundaryCorner().getExtent()
                .getChunk(this.getLesserBoundaryCorner().getBlockX() >> 4, 0, this.getLesserBoundaryCorner().getBlockZ() >> 4).orElse(null);
        Chunk greaterChunk = this.getGreaterBoundaryCorner().getExtent()
                .getChunk(this.getGreaterBoundaryCorner().getBlockX() >> 4, 0, this.getGreaterBoundaryCorner().getBlockZ() >> 4).orElse(null);

        if (lesserChunk != null && greaterChunk != null) {
            for (int x = lesserChunk.getPosition().getX(); x <= greaterChunk.getPosition().getX(); x++) {
                for (int z = lesserChunk.getPosition().getZ(); z <= greaterChunk.getPosition().getZ(); z++) {
                    Chunk chunk = world.loadChunk(x, 0, z, true).orElse(null);
                    if (chunk != null) {
                        chunks.add(chunk);
                    }
                }
            }
        }

        return chunks;
    }

    public Set<Long> getChunkHashes(boolean refresh) {
        if (this.chunkHashes == null || refresh) {
            this.chunkHashes = new HashSet<Long>();
            int smallX = this.lesserBoundaryCorner.getBlockX() >> 4;
            int smallZ = this.lesserBoundaryCorner.getBlockZ() >> 4;
            int largeX = this.greaterBoundaryCorner.getBlockX() >> 4;
            int largeZ = this.greaterBoundaryCorner.getBlockZ() >> 4;
    
            for (int x = smallX; x <= largeX; x++) {
                for (int z = smallZ; z <= largeZ; z++) {
                    this.chunkHashes.add(ChunkPos.chunkXZ2Int(x, z));
                }
            }
        }

        return this.chunkHashes;
    }

    @Override
    public ClaimData getData() {
        return (ClaimData) this.claimData;
    }

    public IClaimData getInternalClaimData() {
        return this.claimData;
    }

    public ClaimStorageData getClaimStorage() {
        return this.claimStorage;
    }

    public void setClaimData(IClaimData data) {
        this.claimData = data;
    }

    public void setClaimStorage(ClaimStorageData storage) {
        this.claimStorage = storage;
    }

    public void updateClaimStorageData() {
        // owner
        if (!this.isSubdivision()) {
            this.claimStorage.getConfig().setWorldUniqueId(this.world.getUniqueId());
        } else {
            if (this.getInternalClaimData() == null) {
                this.setClaimData(new SubDivisionDataConfig(this));
            }

            this.claimStorage.getConfig().getSubdivisions().put(this.id, (SubDivisionDataConfig) this.getInternalClaimData());
        }

        if (this.isBasicClaim()) {
            this.claimStorage.getConfig().setOwnerUniqueId(this.getOwnerUniqueId());
        }

        this.claimData.setCuboid(this.cuboid);
        this.claimData.setType(this.type);
        this.claimData.setLesserBoundaryCorner(BlockUtils.positionToString(this.lesserBoundaryCorner));
        this.claimData.setGreaterBoundaryCorner(BlockUtils.positionToString(this.greaterBoundaryCorner));
        // Will save next world save
        this.getInternalClaimData().setRequiresSave(true);
        // Update SubdivisionData
        for (Claim subdivision : this.children) {
            ((GPClaim) subdivision).updateClaimStorageData();
        }
    }

    public boolean protectPlayersInClaim() {
        GriefPreventionConfig<?> activeConfig = GriefPreventionPlugin.getActiveConfig(this.world.getProperties());
        if (this.isBasicClaim() || this.isSubdivision()) {
            if (activeConfig.getConfig().pvp.protectPlayersInClaims) {
                return true;
            }

            return false;
        } else if (this.isAdminClaim()) {
            if (activeConfig.getConfig().pvp.protectPlayersInAdminClaims) {
                return true;
            }

            return false;
        } else if (this.isSubdivision() && this.parent.isAdminClaim()) {
            if (activeConfig.getConfig().pvp.protectPlayersInAdminSubDivisions) {
                return true;
            }

            return false;
        } else {
            if (activeConfig.getConfig().pvp.protectPlayersInWilderness) {
                return true;
            }

            return false;
        }
    }

    public boolean isPvpEnabled() {
        Tristate value = this.getInternalClaimData().getPvpOverride();
        if (value != Tristate.UNDEFINED) {
            return value.asBoolean();
        }

        return ((IMixinWorldServer) this.world).getActiveConfig().getConfig().getWorld().getPVPEnabled();
    }

    public void setPvpOverride(Tristate value) {
        this.getInternalClaimData().setPvpOverride(value);
        this.getClaimStorage().save();
    }

    public boolean pvpRulesApply() {
        if (!this.isPvpEnabled()) {
            return false;
        }

        GriefPreventionConfig<?> activeConfig = GriefPreventionPlugin.getActiveConfig(this.world.getProperties());
        if (activeConfig != null) {
            return activeConfig.getConfig().pvp.rulesEnabled;
        }

        return false;
    }

    @Override
    public ClaimResult transferOwner(UUID newOwnerID) {
        if (this.isWilderness()) {
            return new GPClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, Text.of(TextColors.RED, "The wilderness cannot be transferred."));
        }

        if (this.isAdminClaim()) {
            return new GPClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, Text.of(TextColors.RED, "Admin claims cannot be transferred."));
        }

        GPPlayerData ownerData = DATASTORE.getOrCreatePlayerData(this.world, this.getOwnerUniqueId());
        // determine new owner
        GPPlayerData newOwnerData = DATASTORE.getOrCreatePlayerData(this.world, newOwnerID);

        if (this.isBasicClaim()) {
            int remainingClaimBlocks = newOwnerData.getRemainingClaimBlocks();
            if (remainingClaimBlocks < 0 || (this.getArea() > remainingClaimBlocks)) {
                return new GPClaimResult(ClaimResultType.INSUFFICIENT_CLAIM_BLOCKS);
            }
        }

        // transfer
        GPTransferClaimEvent event = new GPTransferClaimEvent(this, GriefPreventionPlugin.pluginCause, this.getOwnerUniqueId(), newOwnerID);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            return new GPClaimResult(this, ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        if (this.isAdminClaim()) {
            // convert to basic
            this.type = ClaimType.BASIC;
            this.getVisualizer().setType(VisualizationType.Claim);
            this.getInternalClaimData().setType(ClaimType.BASIC);
        }

        this.ownerUniqueId = event.getNewOwner();
        if (!this.getOwnerUniqueId().equals(newOwnerID)) {
            newOwnerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(this.world, this.getOwnerUniqueId());
        }

        this.getInternalClaimData().setOwnerUniqueId(newOwnerID);
        if (this.isBasicClaim()) {
            ownerData.getClaims().remove(this);
            newOwnerData.getClaims().add(this);
        }

        this.ownerPlayerData = newOwnerData;
        this.getClaimStorage().save();
        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult createSubdivision(Vector3i lesserBoundary, Vector3i greaterBoundary, UUID ownerUniqueId, boolean cuboid, Cause cause) {
        GPClaim subdivision = new GPClaim(this.world, lesserBoundary, greaterBoundary, ClaimType.SUBDIVISION, ownerUniqueId, cuboid, this);
        GPCreateClaimEvent event = new GPCreateClaimEvent(subdivision, cause);
        SpongeImpl.postEvent(event);
        if (event.isCancelled()) {
            return new GPClaimResult(subdivision, ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        Claim overlapClaim = subdivision.doesClaimOverlap();
        if (overlapClaim != null) {
            return new GPClaimResult(overlapClaim, ClaimResultType.OVERLAPPING_CLAIM);
        }

        subdivision.initializeClaimData(this);
        this.children.add(subdivision);
        this.getClaimStorage().save();

        return new GPClaimResult(subdivision, ClaimResultType.SUCCESS);
    }

    public Claim doesClaimOverlap() {
        if (this.isSubdivision() && !this.isCuboid()) {
            return null;
        }

        Claim parent = this.getParent().orElse(null);
        GPClaimManager claimWorldManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(this.world.getProperties());
        Set<Long> chunkHashes = ((GPClaim) this).getChunkHashes(false);
        for (Long chunkHash : chunkHashes) {
            Set<GPClaim> claimsInChunk = claimWorldManager.getChunksToClaimsMap().get(chunkHash);
            if (claimsInChunk == null || claimsInChunk.size() == 0) {
                continue;
            }

            for (GPClaim otherClaim : claimsInChunk) {
                if (otherClaim.getUniqueId().equals(this.getUniqueId())) {
                    continue;
                }
                // if we find an existing claim which will be overlapped
                if (parent != null && otherClaim.getUniqueId() == parent.getUniqueId()) {
                    // check children
                    for (Claim subdivision : otherClaim.children) {
                        if (this.overlaps(subdivision) || subdivision.overlaps(this)) {
                            // result = fail, return conflicting claim
                            return subdivision;
                        }
                    }
                }

                if ((parent == null || (parent != null && otherClaim.id != parent.getUniqueId())) && (this.overlaps(otherClaim) || otherClaim.overlaps(this))) {
                    // result = fail, return conflicting claim
                    return otherClaim;
                }
            }
        }

        return null;
    }

    @Override
    public ClaimResult resize(int newx1, int newx2, int newy1, int newy2, int newz1, int newz2, Cause cause) {
        if (this.cuboid) {
            return resizeCuboid(newx1, newx2, newy1, newy2, newz1, newz2, cause);
        }

        Location<World> startCorner = null;
        Location<World> endCorner = null;
        GPPlayerData playerData = null;
        if (!(cause.root() instanceof Player)) {
            startCorner = new Location<World>(this.world, newx1, newy1, newz1);
            endCorner = new Location<World>(this.world, newx2, newy2, newz2);
        } else {
            Player player = (Player) cause.root();
            playerData = GriefPreventionPlugin.instance.dataStore.getPlayerData(this.world, player.getUniqueId());
            startCorner = playerData.lastShovelLocation;
            endCorner = playerData.endShovelLocation;
        }

        int smallx, bigx, smally, bigy, smallz, bigz;

        // determine small versus big inputs
        if (newx1 < newx2) {
            smallx = newx1;
            bigx = newx2;
        } else {
            smallx = newx2;
            bigx = newx1;
        }

        if (newy1 < newy2) {
            smally = newy1;
            bigy = newy2;
        } else {
            smally = newy2;
            bigy = newy1;
        }

        if (newz1 < newz2) {
            smallz = newz1;
            bigz = newz2;
        } else {
            smallz = newz2;
            bigz = newz1;
        }

        // creative mode claims always go to bedrock
        if (GriefPreventionPlugin.instance.claimModeIsActive(this.world.getProperties(), ClaimsMode.Creative)) {
            smally = 2;
        }

        Location<World> currentLesserCorner = this.getLesserBoundaryCorner();
        Location<World> currentGreaterCorner = this.getGreaterBoundaryCorner();
        Location<World> newLesserCorner = new Location<World>(this.world, smallx, smally, smallz);
        Location<World> newGreaterCorner = new Location<World>(this.world, bigx, bigy, bigz);
        GPResizeClaimEvent event = new GPResizeClaimEvent(this, cause, startCorner, endCorner, 
                new GPClaim(newLesserCorner.copy(), newGreaterCorner.copy(), this.type, this.cuboid));
        SpongeImpl.postEvent(event);
        if (event.isCancelled()) {
            return new GPClaimResult(this, ClaimResultType.CLAIM_EVENT_CANCELLED);
        }

        ClaimResult claimResult = checkSizeLimits(playerData, newLesserCorner.getBlockPosition(), newGreaterCorner.getBlockPosition());
        if (!claimResult.successful()) {
            return claimResult;
        }

        Set<Long> currentChunkHashes = this.getChunkHashes(false);
        // This needs to be adjusted before we check for overlaps
        this.lesserBoundaryCorner = newLesserCorner;
        this.greaterBoundaryCorner = newGreaterCorner;
        ArrayList<Claim> claimsToCheck = null;
        GPClaimManager claimWorldManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(this.world.getProperties());
        if (this.parent == null) {
            claimsToCheck = (ArrayList<Claim>) claimWorldManager.getWorldClaims();
        }
        if (claimsToCheck != null) {
            for (int i = 0; i < claimsToCheck.size(); i++) {
                Claim otherClaim = claimsToCheck.get(i);

                // if we find an existing claim which will be overlapped
                if (otherClaim.getUniqueId() != this.id && otherClaim.overlaps(this)) {
                    // revert boundary locations
                    this.lesserBoundaryCorner = currentLesserCorner;
                    this.greaterBoundaryCorner = currentGreaterCorner;
                    // result = fail, return conflicting claim
                    return new GPClaimResult(otherClaim, ClaimResultType.OVERLAPPING_CLAIM);
                }
            }
        }

        // resize validated, remove invalid chunkHashes
        Set<Long> newChunkHashes = this.getChunkHashes(true);
        if (this.parent == null) {
            currentChunkHashes.removeAll(newChunkHashes);
            for (Long chunkHash : currentChunkHashes) {
                Set<GPClaim> claimsInChunk = claimWorldManager.getChunksToClaimsMap().get(chunkHash);
                if (claimsInChunk != null && claimsInChunk.size() > 0) {
                    claimsInChunk.remove(this);
                }
            }
            // add new chunk hashes
            for (Long chunkHash : newChunkHashes) {
                Set<GPClaim> claimsInChunk = claimWorldManager.getChunksToClaimsMap().get(chunkHash);
                if (claimsInChunk == null) {
                    claimsInChunk = new HashSet<>();
                    claimWorldManager.getChunksToClaimsMap().put(chunkHash, claimsInChunk);
                }
    
                claimsInChunk.add(this);
            }
        }

        this.getInternalClaimData().setLesserBoundaryCorner(BlockUtils.positionToString(this.lesserBoundaryCorner));
        this.getInternalClaimData().setGreaterBoundaryCorner(BlockUtils.positionToString(this.greaterBoundaryCorner));
        this.getInternalClaimData().setRequiresSave(true);
        this.getClaimStorage().save();

        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    public ClaimResult resizeCuboid(int newx1, int newy1, int newz1, int newx2, int newy2, int newz2, Cause cause) {
        int smallX = this.lesserBoundaryCorner.getBlockX();
        int smallY = this.lesserBoundaryCorner.getBlockY();
        int smallZ = this.lesserBoundaryCorner.getBlockZ();
        int bigX = this.greaterBoundaryCorner.getBlockX();
        int bigY = this.greaterBoundaryCorner.getBlockY();
        int bigZ = this.greaterBoundaryCorner.getBlockZ();

        if (newx1 == smallX) {
            smallX = newx2;
        } else {
            bigX = newx2;
        }

        if (newy1 == smallY) {
            smallY = newy2;
        } else {
            bigY = newy2;
        }

        if (newz1 == smallZ) {
            smallZ = newz2;
        } else {
            bigZ = newz2;
        }
        newx1 = smallX;
        newy1 = smallY;
        newz1 = smallZ;
        newx2 = bigX;
        newy2 = bigY;
        newz2 = bigZ;

        Location<World> startCorner = null;
        Location<World> endCorner = null;
        GPPlayerData playerData = null;
        if (!(cause.root() instanceof Player)) {
            startCorner = new Location<World>(this.world, smallX, smallY, smallZ);
            endCorner = new Location<World>(this.world, bigX, bigY, bigZ);
        } else {
            Player player = (Player) cause.root();
            playerData = GriefPreventionPlugin.instance.dataStore.getPlayerData(this.world, player.getUniqueId());
            startCorner = playerData.lastShovelLocation;
            endCorner = playerData.endShovelLocation;
        }

        // make sure resize doesn't cross paths
        if (smallX >= bigX || smallY >= bigY || smallZ >= bigZ) {
            return null;
        }
        // check if subdivision extends past parent limits
        if (this.parent != null) {
            if (smallX < this.parent.getLesserBoundaryCorner().getBlockX() ||
                smallY < this.parent.getLesserBoundaryCorner().getBlockY() ||
                smallZ < this.parent.getLesserBoundaryCorner().getBlockZ()) {
                return new GPClaimResult(this.parent, ClaimResultType.OVERLAPPING_CLAIM);
            }
            if (bigX > this.parent.getGreaterBoundaryCorner().getBlockX() ||
                (this.parent.isCuboid() && bigY > this.parent.getGreaterBoundaryCorner().getBlockY()) ||
                bigZ > this.parent.getGreaterBoundaryCorner().getBlockZ()) {
                return new GPClaimResult(this.parent, ClaimResultType.OVERLAPPING_CLAIM);
            }
        }

        Set<Long> currentChunkHashes = this.getChunkHashes(false);
        Location<World> newLesserCorner = new Location<World>(this.world, smallX, smallY, smallZ);
        Location<World> newGreaterCorner = new Location<World>(this.world, bigX, bigY, bigZ);
        Claim overlapClaim = this.doesClaimOverlap();
        if (overlapClaim != null) {
            return new GPClaimResult(overlapClaim, ClaimResultType.OVERLAPPING_CLAIM);
        }

        GPResizeClaimEvent event = new GPResizeClaimEvent(this, cause, startCorner, endCorner, 
                new GPClaim(newLesserCorner.copy(), newGreaterCorner.copy(), this.type, this.cuboid));
        SpongeImpl.postEvent(event);
        if (event.isCancelled()) {
            return new GPClaimResult(this, ClaimResultType.CLAIM_EVENT_CANCELLED);
        }

        ClaimResult claimResult = checkSizeLimits(playerData, newLesserCorner.getBlockPosition(), newGreaterCorner.getBlockPosition());
        if (!claimResult.successful()) {
            return claimResult;
        }

        this.lesserBoundaryCorner = newLesserCorner;
        this.greaterBoundaryCorner = newGreaterCorner;
        // resize validated, remove invalid chunkHashes
        Set<Long> newChunkHashes = this.getChunkHashes(true);
        GPClaimManager claimWorldManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(this.world.getProperties());
        if (this.parent == null) {
            currentChunkHashes.removeAll(newChunkHashes);
            for (Long chunkHash : currentChunkHashes) {
                Set<GPClaim> claimsInChunk = claimWorldManager.getChunksToClaimsMap().get(chunkHash);
                if (claimsInChunk != null && claimsInChunk.size() > 0) {
                    claimsInChunk.remove(this);
                }
            }
            // add new chunk hashes
            for (Long chunkHash : newChunkHashes) {
                Set<GPClaim> claimsInChunk = claimWorldManager.getChunksToClaimsMap().get(chunkHash);
                if (claimsInChunk == null) {
                    claimsInChunk = new HashSet<>();
                    claimWorldManager.getChunksToClaimsMap().put(chunkHash, claimsInChunk);
                }

                claimsInChunk.add(this);
            }
        }

        this.getInternalClaimData().setLesserBoundaryCorner(BlockUtils.positionToString(this.lesserBoundaryCorner));
        this.getInternalClaimData().setGreaterBoundaryCorner(BlockUtils.positionToString(this.greaterBoundaryCorner));
        this.getInternalClaimData().setRequiresSave(true);
        this.getClaimStorage().save();

        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    private ClaimResult checkSizeLimits(GPPlayerData playerData, Vector3i lesserCorner, Vector3i greaterCorner) {
        if (playerData != null && playerData.optionMaxClaimSizeX > 0) {
            int claimWidth = greaterCorner.getX() - lesserCorner.getX() + 1;
            if (claimWidth > playerData.optionMaxClaimSizeX) {
                return new GPClaimResult(ClaimResultType.EXCEEDS_MAX_SIZE_X);
            }
        }
        if (this.cuboid && playerData != null && playerData.optionMaxClaimSizeY > 0) {
            int claimWidth = greaterCorner.getY() - lesserCorner.getY() + 1;
            if (claimWidth > playerData.optionMaxClaimSizeY) {
                return new GPClaimResult(ClaimResultType.EXCEEDS_MAX_SIZE_Y);
            }
        }
        if (playerData != null && playerData.optionMaxClaimSizeZ > 0) {
            int claimWidth = greaterCorner.getZ() - lesserCorner.getZ() + 1;
            if (claimWidth > playerData.optionMaxClaimSizeZ) {
                return new GPClaimResult(ClaimResultType.EXCEEDS_MAX_SIZE_Z);
            }
        }

        return new GPClaimResult(ClaimResultType.SUCCESS);
    }

    public void unload() {
        // clear any references
        this.world = null;
    }

    @Override
    public Context getContext() {
        return this.context;
    }

    @Override
    public boolean extend(int newDepth) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Optional<Claim> getParent() {
        return Optional.ofNullable(this.parent);
    }

    @Override
    public World getWorld() {
        return this.world;
    }

    @Override
    public List<Claim> getSubdivisions() {
        return ImmutableList.copyOf(this.children);
    }

    @Override
    public ClaimResult deleteSubdivision(Claim subdivision, Cause cause) {
        GPDeleteClaimEvent event = new GPDeleteClaimEvent(subdivision, cause);
        SpongeImpl.postEvent(event);
        if (event.isCancelled()) {
            return new GPClaimResult(subdivision, ClaimResultType.CLAIM_EVENT_CANCELLED);
        }

        Claim parentClaim = subdivision.getParent().orElse(null);
        if (parentClaim == null) {
            return new GPClaimResult(subdivision, ClaimResultType.WRONG_CLAIM_TYPE);
        }

        if (parentClaim.getUniqueId() != this.id) {
            return new GPClaimResult(parentClaim, ClaimResultType.PARENT_CLAIM_MISMATCH);
        }

        this.deleteSubdivision(subdivision, true);
        return new GPClaimResult(subdivision, ClaimResultType.SUCCESS);
    }

    public void deleteSubdivision(Claim subdivision, boolean save) {

        Iterator<Claim> iterator = this.children.iterator();
        while (iterator.hasNext()) {
            Claim claim = iterator.next();
            if (claim.getUniqueId().equals(subdivision.getUniqueId())) {
                iterator.remove();
                this.getClaimStorage().getConfig().getSubdivisions().remove(claim.getUniqueId());
                if (save) {
                    this.getClaimStorage().save();
                }
                return;
            }
        }

        return;
    }

    @Override
    public ClaimResult convertToType(ClaimType type, Optional<UUID> owner) {
        if (this.isWilderness()) {
            return new GPClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, Text.of(TextColors.RED, "The wilderness cannot be converted."));
        }
        if (this.isSubdivision()) {
            return new GPClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, Text.of(TextColors.RED, "Subdivisions cannot be converted."));
        }
        if (type == ClaimType.WILDERNESS) {
            return new GPClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, Text.of(TextColors.RED, "Claims cannot be converted to wilderness."));
        }
        if (type == ClaimType.SUBDIVISION) {
            return new GPClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, Text.of(TextColors.RED, "Claims cannot be converted to subdivisions."));
        }
        if (type == ClaimType.TOWN) {
            return new GPClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, Text.of(TextColors.RED, "The town type is currently unsupported."));
        }

        if (this.isAdminClaim()) {
            if (type == ClaimType.ADMIN) {
                return new GPClaimResult(ClaimResultType.CLAIM_ALREADY_EXISTS, Text.of(TextColors.RED, "Could not convert, claim is already an admin claim."));
            }
            if (!owner.isPresent()) {
                return new GPClaimResult(ClaimResultType.REQUIRES_OWNER, Text.of(TextColors.RED, "Could not convert admin claim to basic. Owner is required."));
            }

            UUID newOwnerUniqueId = owner.get();
            GPClaimManager claimWorldManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(this.world.getProperties());
            List<Claim> playerClaims = claimWorldManager.getInternalPlayerClaims(newOwnerUniqueId);
            if (playerClaims != null && !playerClaims.contains(this)) {
                playerClaims.add(this);
            }

            this.type = ClaimType.BASIC;
            this.setOwnerUniqueId(newOwnerUniqueId);
            this.visualization = null;
            this.getInternalClaimData().setOwnerUniqueId(newOwnerUniqueId);
            this.getInternalClaimData().setType(ClaimType.BASIC);
        }
        if (isBasicClaim()) {
            if (type == ClaimType.BASIC) {
                return new GPClaimResult(ClaimResultType.CLAIM_ALREADY_EXISTS, Text.of(TextColors.RED, "Could not convert, claim is already a basic claim."));
            }

            GPClaimManager claimWorldManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(this.world.getProperties());
            List<Claim> playerClaims = claimWorldManager.getInternalPlayerClaims(this.getOwnerUniqueId());
            if (playerClaims != null) {
                playerClaims.remove(this);
            }

            this.visualization = null;
            this.setOwnerUniqueId(null);
            this.type = ClaimType.ADMIN;
            this.getInternalClaimData().setType(ClaimType.ADMIN);
        }
        // revert visuals for all players watching this claim
        List<UUID> playersWatching = new ArrayList<>(this.playersWatching);
        for (UUID playerUniqueId : playersWatching) {
            Player player = Sponge.getServer().getPlayer(playerUniqueId).orElse(null);
            if (player != null) {
                GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getPlayerData(this.world, playerUniqueId);
                playerData.revertActiveVisual(player);
            }
        }
        this.getClaimStorage().save();
        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(o instanceof GPClaim)) {
            return false;
        }
        GPClaim that = (GPClaim) o;
        return this.type == that.type &&
               Objects.equal(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public List<UUID> getAllTrusts() {
        List<UUID> trustList = new ArrayList<>();
        trustList.addAll(this.getInternalClaimData().getAccessors());
        trustList.addAll(this.getInternalClaimData().getContainers());
        trustList.addAll(this.getInternalClaimData().getBuilders());
        trustList.addAll(this.getInternalClaimData().getManagers());
        return ImmutableList.copyOf(trustList);
    }

    @Override
    public List<UUID> getTrusts(TrustType type) {
        if (type == TrustType.ACCESSOR) {
            return ImmutableList.copyOf(this.getInternalClaimData().getAccessors());
        }
        if (type == TrustType.CONTAINER) {
            return ImmutableList.copyOf(this.getInternalClaimData().getContainers());
        }
        if (type == TrustType.BUILDER) {
            return ImmutableList.copyOf(this.getInternalClaimData().getBuilders());
        }

        return ImmutableList.copyOf(this.getInternalClaimData().getManagers());
    }

    @Override
    public ClaimResult addTrust(UUID uuid, TrustType type, Cause cause) {
        GPTrustClaimEvent.Add event = new GPTrustClaimEvent.Add(this, cause, ImmutableList.of(uuid), type);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            return new GPClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        List<UUID> userList = this.getTrustList(type);
        if (!userList.contains(uuid)) {
            userList.add(uuid);
        }

        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult addTrusts(List<UUID> uuids, TrustType type, Cause cause) {
        GPTrustClaimEvent.Add event = new GPTrustClaimEvent.Add(this, cause, uuids, type);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            return new GPClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        for (UUID uuid : uuids) {
            List<UUID> userList = this.getTrustList(type);
            if (!userList.contains(uuid)) {
                userList.add(uuid);
            }
        }

        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult removeTrust(UUID uuid, TrustType type, Cause cause) {
        GPTrustClaimEvent.Remove event = new GPTrustClaimEvent.Remove(this, cause, ImmutableList.of(uuid), type);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            return new GPClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        this.getTrustList(type).remove(uuid);
        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult removeAllTrusts(Cause cause) {
        List<UUID> trustList = this.getAllTrusts();
        GPTrustClaimEvent.Remove event = new GPTrustClaimEvent.Remove(this, cause, trustList, TrustType.NONE);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            return new GPClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        for (TrustType type : TrustType.values()) {
            this.getTrustList(type).clear();
        }

        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult removeTrusts(List<UUID> uuids, TrustType type, Cause cause) {
        GPTrustClaimEvent.Remove event = new GPTrustClaimEvent.Remove(this, cause, uuids, type);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            return new GPClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        List<UUID> userList = this.getTrustList(type);
        for (UUID uuid : uuids) {
            if (userList.contains(uuid)) {
                userList.remove(uuid);
            }
        }

        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    public List<UUID> getTrustList(TrustType type) {
        if (type == TrustType.ACCESSOR) {
            return this.getInternalClaimData().getAccessors();
        }
        if (type == TrustType.CONTAINER) {
            return this.getInternalClaimData().getContainers();
        }
        if (type == TrustType.BUILDER) {
            return this.getInternalClaimData().getBuilders();
        }
        return this.getInternalClaimData().getManagers();
    }

    public static class ClaimBuilder implements Builder {

        private UUID ownerUniqueId;
        private ClaimType type = ClaimType.BASIC;
        private boolean cuboid = false;
        private boolean requiresClaimBlocks = true;
        private boolean sizeRestrictions = true;
        private World world;
        private Vector3i point1;
        private Vector3i point2;
        private Claim parent;
        private Cause cause;

        public ClaimBuilder() {
            
        }

        @Override
        public Builder cuboid(boolean cuboid) {
            this.cuboid = cuboid;
            return this;
        }

        @Override
        public Builder bounds(Vector3i point1, Vector3i point2) {
            this.point1 = point1;
            this.point2 = point2;
            return this;
        }

        @Override
        public Builder owner(UUID ownerUniqueId) {
            this.ownerUniqueId = ownerUniqueId;
            return this;
        }

        @Override
        public Builder parent(Claim parentClaim) {
            this.parent = parentClaim;
            return this;
        }

        @Override
        public Builder type(ClaimType type) {
            this.type = type;
            return this;
        }

        @Override
        public Builder world(World world) {
            this.world = world;
            return this;
        }

        @Override
        public Builder sizeRestrictions(boolean checkSizeRestrictions) {
            this.sizeRestrictions = checkSizeRestrictions;
            return this;
        }

        @Override
        public Builder requiresClaimBlocks(boolean requiresClaimBlocks) {
            this.requiresClaimBlocks = requiresClaimBlocks;
            return this;
        }

        public Builder cause(Cause cause) {
            this.cause = cause;
            return this;
        }

        @Override
        public Builder reset() {
            this.ownerUniqueId = null;
            this.type = ClaimType.BASIC;
            this.cuboid = false;
            this.world = null;
            this.point1 = null;
            this.point2 = null;
            this.parent = null;
            return this;
        }

        @Override
        public ClaimResult build() {
            checkNotNull(this.type);
            checkNotNull(this.world);
            checkNotNull(this.point1);
            checkNotNull(this.point2);
            checkNotNull(this.cause);
            if (this.type == ClaimType.SUBDIVISION) {
                checkNotNull(this.parent);
            }

            GPClaim claim = new GPClaim(this.world, this.point1, this.point2, this.type, this.ownerUniqueId, this.cuboid);
            // ensure this new claim won't overlap any existing claims
            GPClaim overlapClaim = (GPClaim) claim.doesClaimOverlap();
            claim.parent = (GPClaim) this.parent;
            if (overlapClaim != null) {
                return new GPClaimResult(overlapClaim, ClaimResultType.OVERLAPPING_CLAIM);
            }

            if (this.sizeRestrictions && this.ownerUniqueId != null) {
                GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(this.world, this.ownerUniqueId);
                ClaimResult claimResult = claim.checkSizeLimits(playerData, this.point1, this.point2);
                if (!claimResult.successful()) {
                    return claimResult;
                }
            }

            GPCreateClaimEvent event = new GPCreateClaimEvent(claim, this.cause);
            Sponge.getEventManager().post(event);
            if (event.isCancelled()) {
                return new GPClaimResult(claim, ClaimResultType.CLAIM_EVENT_CANCELLED);
            }

            claim.initializeClaimData((GPClaim) this.parent);
            claim.getData().setRequiresClaimBlocks(this.requiresClaimBlocks);
            return new GPClaimResult(claim, ClaimResultType.SUCCESS);
        }
    }
}
