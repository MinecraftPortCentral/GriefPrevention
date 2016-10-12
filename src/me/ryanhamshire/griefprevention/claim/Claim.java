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

import com.google.common.collect.ImmutableSet;
import me.ryanhamshire.griefprevention.GPPermissionHandler;
import me.ryanhamshire.griefprevention.GPPermissions;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.SiegeData;
import me.ryanhamshire.griefprevention.Visualization;
import me.ryanhamshire.griefprevention.command.CommandHelper;
import me.ryanhamshire.griefprevention.configuration.ClaimStorageData;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.configuration.IClaimData;
import me.ryanhamshire.griefprevention.configuration.SubDivisionDataConfig;
import me.ryanhamshire.griefprevention.task.RestoreNatureProcessingTask;
import me.ryanhamshire.griefprevention.util.BlockUtils;
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
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextSource;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.BlockChangeFlag;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

//represents a player claim
//creating an instance doesn't make an effective claim
//only claims which have been added to the datastore have any effect
public class Claim implements ContextSource {

    public enum Type {
        ADMIN,
        BASIC,
        SUBDIVISION,
        WILDERNESS
    }

    // two locations, which together define the boundaries of the claim
    // Note: 2D cuboids will ignore the upper Y value while 3D cuboids do not
    public Location<World> lesserBoundaryCorner;
    public Location<World> greaterBoundaryCorner;
    public World world;
    public Type type = Type.BASIC;

    // Permission Context
    public Context context;

    // id number. unique to this claim, never changes.
    public UUID id = null;

    // ownerID. for admin claims, this is NULL
    // use getOwnerName() to get a friendly name (will be "an administrator" for admin claims)
    public UUID ownerID;

    private ClaimStorageData claimStorage;
    private IClaimData claimData;

    // parent claim
    // only used for claim subdivisions. top level claims have null here
    public Claim parent = null;

    // children (subdivisions)
    // note subdivisions themselves never have children
    public ArrayList<Claim> children = new ArrayList<Claim>();

    // information about a siege involving this claim. null means no siege is impacting this claim
    public SiegeData siegeData = null;

    // following a siege, buttons/levers are unlocked temporarily. This represents that state
    public boolean doorsOpen = false;

    public boolean cuboid = false;

    // used by subdivisions to inherit parent permissions
    public boolean inheritParent = true;

    public Visualization visualization;
    public List<UUID> playersWatching = new ArrayList<>();

    public Claim(Location<World> lesserBoundaryCorner, Location<World> greaterBoundaryCorner, Type type) {
        this(lesserBoundaryCorner, greaterBoundaryCorner, UUID.randomUUID(), type);
    }

    // Used at server startup
    public Claim(Location<World> lesserBoundaryCorner, Location<World> greaterBoundaryCorner, UUID claimId, Type type) {
        this(lesserBoundaryCorner, greaterBoundaryCorner, claimId, null, null);
    }

    // main constructor. note that only creating a claim instance does nothing -
    // a claim must be added to the data store to be effective
    public Claim(Location<World> lesserBoundaryCorner, Location<World> greaterBoundaryCorner, UUID claimId, Type type, Player player) {
        // id
        this.id = claimId;

        // store corners
        this.lesserBoundaryCorner = lesserBoundaryCorner;
        this.greaterBoundaryCorner = greaterBoundaryCorner;
        this.world = lesserBoundaryCorner.getExtent();
        if (player != null) {
            this.ownerID = player.getUniqueId();
        }
        this.type = type;
    }

    public Visualization getVisualizer() {
        if (this.visualization == null) {
            this.visualization = new Visualization(this, Visualization.getVisualizationType(this));
        }
        return this.visualization;
    }

    public UUID getOwnerUniqueId() {
        if (this.isSubdivision()) {
            return this.parent.ownerID;
        }

        return this.ownerID;
    }

    // whether or not this is an administrative claim
    // administrative claims are created and maintained by players with the
    // griefprevention.adminclaims permission.
    public boolean isAdminClaim() {
        if (this.parent != null) {
            return this.parent.isAdminClaim();
        }

        return this.type == Type.ADMIN;
    }

    public boolean isBasicClaim() {
        if (this.parent != null) {
            return this.parent.isBasicClaim();
        }

        return this.type == Type.BASIC;
    }

    public boolean isSubdivision() {
        return this.parent != null && this.type == Type.SUBDIVISION;
    }

    public boolean isWildernessClaim() {
        return this.type == Type.WILDERNESS;
    }

    // accessor for ID
    public UUID getID() {
        return this.id;
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
    public void removeSurfaceFluids(Claim exclusionClaim) {
        // don't do this for administrative claims
        if (this.isAdminClaim()) {
            return;
        }

        // don't do it for very large claims
        if (this.getArea() > 10000) {
            return;
        }

        // only in creative mode worlds
        if (!GriefPrevention.instance.claimModeIsActive(this.lesserBoundaryCorner.getExtent().getProperties(), ClaimsMode.Creative)) {
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
            seaLevel = GriefPrevention.instance.getSeaLevel(lesser.getExtent());
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
            seaLevel = GriefPrevention.instance.getSeaLevel(lesser.getExtent());
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
    public int getArea() {
        int claimWidth = this.greaterBoundaryCorner.getBlockX() - this.lesserBoundaryCorner.getBlockX() + 1;
        int claimHeight = this.greaterBoundaryCorner.getBlockZ() - this.lesserBoundaryCorner.getBlockZ() + 1;

        return claimWidth * claimHeight;
    }

    public int getWidth() {
        return this.greaterBoundaryCorner.getBlockX() - this.lesserBoundaryCorner.getBlockX() + 1;
    }

    public int getHeight() {
        return this.greaterBoundaryCorner.getBlockZ() - this.lesserBoundaryCorner.getBlockZ() + 1;
    }

    public boolean hasFullAccess(User user) {
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(this.world, user.getUniqueId());
        if (playerData != null && playerData.ignoreClaims) {
            return true;
        }

        // owner
        if (user.getUniqueId().equals(this.ownerID)) {
            // only check debug claim permissions if owner
            if (playerData.debugClaimPermissions) {
                return false;
            }

            return true;
        }

        if (this.isWildernessClaim() && user.hasPermission(GPPermissions.MANAGE_WILDERNESS)) {
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
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(this.world, user.getUniqueId());
        if (playerData != null && playerData.ignoreClaims) {
            return true;
        }

        // owner
        if (user.getUniqueId().equals(this.ownerID)) {
            // only check debug claim permissions if owner
            if (playerData.debugClaimPermissions) {
                return false;
            }

            return true;
        }

        if (this.isWildernessClaim() && user.hasPermission(GPPermissions.MANAGE_WILDERNESS)) {
            if (playerData.debugClaimPermissions) {
                return false;
            }

            return true;
        }

        // Builders can place blocks in claims
        if (this.getClaimData().getBuilders().contains(GriefPrevention.PUBLIC_UUID) || this.getClaimData().getBuilders().contains(user.getUniqueId())) {
            return true;
        }

        // if subdivision
        if (this.parent != null) {
            if (!this.inheritParent) {
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
        if (this.hasFullAccess(player)) {
            return null;
        }

        // special cases...
        // admin claims need adminclaims permission only.
        if (this.isAdminClaim()) {
            if (player.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS)) {
                return null;
            }
        }

        // anyone with deleteclaims permission can modify non-admin claims at any time
        else {
            if (player.hasPermission(GPPermissions.COMMAND_DELETE_CLAIMS)) {
                return null;
            }
        }

        // no resizing, deleting, and so forth while under siege
        // don't use isManager here as only owners may edit claims
        if (player.getUniqueId().equals(this.ownerID)) {
            if (this.siegeData != null) {
                return GriefPrevention.instance.dataStore.getMessage(Messages.NoModifyDuringSiege);
            }

            // otherwise, owners can do whatever
            return null;
        }

        if (this.isWildernessClaim() && player.hasPermission(GPPermissions.MANAGE_WILDERNESS)) {
            return null;
        }

        // permission inheritance for subdivisions
        if (this.parent != null && this.inheritParent) {
            return this.parent.allowEdit(player);
        }

        // error message if all else fails
        return GriefPrevention.instance.dataStore.getMessage(Messages.OnlyOwnersModifyClaims, this.getOwnerName());
    }

    // build permission check
    public String allowBuild(Object source, Location<World> location, User user) {
        if (user == null) {
            return null;
        }

        // when a player tries to build in a claim, if he's under siege, the
        // siege may extend to include the new claim
        if (user instanceof Player) {
            GriefPrevention.instance.dataStore.tryExtendSiege((Player) user, this);
        }

        // admin claims can always be modified by admins, no exceptions
        if (this.isAdminClaim()) {
            if (user.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS)) {
                return null;
            }
        }

        // no building while under siege
        if (this.siegeData != null) {
            return GriefPrevention.instance.dataStore.getMessage(Messages.NoBuildUnderSiege, this.siegeData.attacker.getName());
        }

        // no building while in pvp combat
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(location.getExtent(), user.getUniqueId());
        if (playerData.inPvpCombat(location.getExtent())) {
            return GriefPrevention.instance.dataStore.getMessage(Messages.NoBuildPvP);
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
        if (this.getClaimData().getBuilders().contains(GriefPrevention.PUBLIC_UUID) || this.getClaimData().getBuilders().contains(user.getUniqueId())) {
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
        if (this.parent != null && this.inheritParent) {
            return this.parent.allowBuild(source, location, user);
        }

        // failure message for all other cases
        String reason = "";
        if (location.getBlock().getType() != BlockTypes.FLOWING_WATER && location.getBlock().getType() != BlockTypes.FLOWING_LAVA) {
            reason = GriefPrevention.instance.dataStore.getMessage(Messages.NoBuildPermission, this.getOwnerName());
        }
        if (user.hasPermission(GPPermissions.COMMAND_IGNORE_CLAIMS)) {
            if (!reason.equals("")) {
                reason += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
            } else {
                reason = GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
            }
        }

        return reason;
    }

    public String allowBreak(Object source, BlockSnapshot blockSnapshot, User user) {
        Location<World> location = blockSnapshot.getLocation().orElse(null);
        if (location == null) {
            return null;
        }
        return this.allowBreak(source, location, user);
    }

    public String allowBreak(Object source, Location<World> location, User user) {
        // if under siege, some blocks will be breakable
        if (this.siegeData != null || this.doorsOpen) {
            boolean breakable = false;

            // search for block type in list of breakable blocks
            for (int i = 0; i < GriefPrevention.getActiveConfig(location.getExtent().getProperties()).getConfig().siege.breakableSiegeBlocks.size();
                 i++) {
                String blockTypeId =
                        GriefPrevention.getActiveConfig(location.getExtent().getProperties()).getConfig().siege.breakableSiegeBlocks.get(i);
                Optional<BlockType> breakableBlockType = Sponge.getGame().getRegistry().getType(BlockType.class, blockTypeId);
                if (breakableBlockType.isPresent() && breakableBlockType.get() == location.getBlockType()) {
                    breakable = true;
                    break;
                }
            }

            // custom error messages for siege mode
            if (!breakable) {
                return GriefPrevention.instance.dataStore.getMessage(Messages.NonSiegeMaterial);
            } else if (user != null && hasFullAccess(user)) {
                return GriefPrevention.instance.dataStore.getMessage(Messages.NoOwnerBuildUnderSiege);
            } else {
                return null;
            }
        }

        String reason = GriefPrevention.instance.dataStore.getMessage(Messages.NoBuildPermission, this.getOwnerName());
        if (user != null) {
            if (hasFullAccess(user)) {
                return null;
            }
    
            //failure message for all other cases
            if(user.hasPermission(GPPermissions.COMMAND_IGNORE_CLAIMS)) {
                reason += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
            }

            // Builders can break blocks
            if (this.getClaimData().getBuilders().contains(GriefPrevention.PUBLIC_UUID) || this.getClaimData().getBuilders().contains(user.getUniqueId())) {
                return null;
            }

            // Flag order matters
            // interact should always be checked before break
            if (GPPermissionHandler.getClaimPermission(this, GPPermissions.BLOCK_BREAK, source, location.getBlock(), user) == Tristate.TRUE) {
                return null;
            }
        }

        return reason;
    }

    public String allowAccess(User user) {
        return allowAccess(user, null);
    }

    // access permission check
    public String allowAccess(User user, Location<World> location) {
        // admin claims need adminclaims permission only.
        if (this.isAdminClaim()) {
            if (user.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS)) {
                return null;
            }
        }

        // following a siege where the defender lost, the claim will allow everyone access for a time
        if (this.doorsOpen) {
            return null;
        }

        // claim owner and admins in ignoreclaims mode have access
        if (hasFullAccess(user)) {
            return null;
        }

        if (this.getClaimData().getAccessors().contains(GriefPrevention.PUBLIC_UUID)
                || this.getClaimData().getBuilders().contains(GriefPrevention.PUBLIC_UUID) 
                || this.getClaimData().getContainers().contains(GriefPrevention.PUBLIC_UUID) 
                || this.getClaimData().getBuilders().contains(user.getUniqueId()) 
                || this.getClaimData().getContainers().contains(user.getUniqueId())
                || this.getClaimData().getAccessors().contains(user.getUniqueId())) {
            return null;
        }

        // permission inheritance for subdivisions
        if (this.parent != null && this.inheritParent) {
            return this.parent.allowAccess(user, location);
        }

        //catch-all error message for all other cases
        String reason = GriefPrevention.instance.dataStore.getMessage(Messages.NoAccessPermission, this.getOwnerName());
        if(user.hasPermission(GPPermissions.COMMAND_IGNORE_CLAIMS)) {
            reason += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
        }
        return reason;
    }

    public String allowContainers(User user, Location<World> location) {
        //trying to access inventory in a claim may extend an existing siege to include this claim
        if (user instanceof Player) {
            GriefPrevention.instance.dataStore.tryExtendSiege((Player) user, this);
        }
        
        //if under siege, nobody accesses containers
        if(this.siegeData != null) {
            return GriefPrevention.instance.dataStore.getMessage(Messages.NoContainersSiege, siegeData.attacker.getName());
        }
        
        // claim owner and admins in ignoreclaims mode have access
        if (hasFullAccess(user)) {
            return null;
        }

        // admin claims need adminclaims permission only.
        if (this.isAdminClaim()) {
            if (user.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS)) {
                return null;
            }
        }

        if (this.getClaimData().getBuilders().contains(GriefPrevention.PUBLIC_UUID) 
                || this.getClaimData().getContainers().contains(GriefPrevention.PUBLIC_UUID) 
                || this.getClaimData().getBuilders().contains(user.getUniqueId()) 
                || this.getClaimData().getContainers().contains(user.getUniqueId())) {
            return null;
        }

        //permission inheritance for subdivisions
        if(this.parent != null && this.inheritParent) {
            return this.parent.allowContainers(user, location);
        }
        
        //error message for all other cases
        String reason = GriefPrevention.instance.dataStore.getMessage(Messages.NoContainersPermission, this.getOwnerName());
        if(user.hasPermission(GPPermissions.COMMAND_IGNORE_CLAIMS)) {
            reason += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
        }
        return reason;
    }

    //grant permission check, relatively simple
    public String allowGrantPermission(Player player) {
        //anyone who can modify the claim can do this
        if(this.allowEdit(player) == null) {
            return null;
        }
        
        //anyone who's in the managers (/PermissionTrust) list can do this
        for(int i = 0; i < this.getClaimData().getManagers().size(); i++) {
            UUID managerID = this.getClaimData().getManagers().get(i);
            if(player.getUniqueId().equals(managerID)) {
                return null;
            }
        }
        
        //permission inheritance for subdivisions
        if(this.parent != null && this.inheritParent) {
            return this.parent.allowGrantPermission(player);
        }
        
        //generic error message
        String reason = GriefPrevention.instance.dataStore.getMessage(Messages.NoPermissionTrust, this.getOwnerName());
        if(player.hasPermission("griefprevention.ignoreclaims")) {
            reason += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
        }

        return reason;
    }

    //clears all permissions (except owner of course)
    public void clearPermissions() {
        this.getClaimData().getManagers().clear();
        
        for(Claim child : this.children) {
            child.clearPermissions();
        }
    }

    // returns a copy of the location representing lower x, y, z limits
    @SuppressWarnings("unchecked")
    public Location<World> getLesserBoundaryCorner() {
        return (Location<World>) this.lesserBoundaryCorner.copy();
    }

    // returns a copy of the location representing upper x, y, z limits
    // NOTE: remember upper Y will always be ignored, all claims always extend to the sky
    @SuppressWarnings("unchecked")
    public Location<World> getGreaterBoundaryCorner() {
        return (Location<World>) this.greaterBoundaryCorner.copy();
    }

    // returns a friendly owner name (for admin claims, returns "an
    // administrator" as the owner)
    public String getOwnerName() {
        if (this.isWildernessClaim()) {
            return GriefPrevention.instance.dataStore.getMessage(Messages.OwnerNameForWildernessClaims);
        }

        if (this.isAdminClaim()) {
            return GriefPrevention.instance.dataStore.getMessage(Messages.OwnerNameForAdminClaims);
        }

        if (this.parent != null) {
            return this.parent.getOwnerName();
        }

        String name = CommandHelper.lookupPlayerName(this.ownerID);
        if (name == null) {
            return "unknown";
        }

        return name;
    }

    // whether or not a location is in a claim
    // ignoreHeight = true means location UNDER the claim will return TRUE
    // excludeSubdivisions = true means that locations inside subdivisions of the claim will return FALSE
    public boolean contains(Location<World> location, boolean ignoreHeight, boolean excludeSubdivisions) {
        if (this.cuboid) {
            return this.contains(location);
        }

        // not in the same world implies false
        if (!location.getExtent().equals(this.lesserBoundaryCorner.getExtent())) {
            return false;
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
        if (this.parent != null && this.inheritParent) {
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
        if (!location.getExtent().equals(this.lesserBoundaryCorner.getExtent())) {
            return false;
        }

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

    public boolean overlaps(Claim otherClaim) {
        if (this.id == otherClaim.id) {
            return false;
        }

        if (!this.lesserBoundaryCorner.getExtent().equals(otherClaim.getLesserBoundaryCorner().getExtent())) {
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

        boolean inArea = 
                ((this.lesserBoundaryCorner.getBlockX() >= smallX &&
                 this.lesserBoundaryCorner.getBlockX() <= bigX) ||
                (this.greaterBoundaryCorner.getBlockX() >= smallX &&
                 this.greaterBoundaryCorner.getBlockX() <= bigX)) &&
                ((this.lesserBoundaryCorner.getBlockZ() >= smallZ &&
                  this.lesserBoundaryCorner.getBlockZ() <= bigZ) ||
                 (this.greaterBoundaryCorner.getBlockZ() >= smallZ &&
                  this.greaterBoundaryCorner.getBlockZ() <= bigZ));

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
        if (!GriefPrevention.instance.claimModeIsActive(this.getLesserBoundaryCorner().getExtent().getProperties(), ClaimsMode.Creative)) {
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
            return GriefPrevention.instance.dataStore.getMessage(Messages.ClaimTooSmallForEntities);
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
            return GriefPrevention.instance.dataStore.getMessage(Messages.TooManyEntitiesInClaim);
        }

        return null;
    }

    // implements a strict ordering of claims, used to keep the claims
    // collection sorted for faster searching
    boolean greaterThan(Claim otherClaim) {
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

    public long getPlayerInvestmentScore() {
        // decide which blocks will be considered player placed
        Location<World> lesserBoundaryCorner = this.getLesserBoundaryCorner();
        ArrayList<BlockType> playerBlocks = RestoreNatureProcessingTask.getPlayerBlocks(lesserBoundaryCorner.getExtent().getDimension().getType(),
                lesserBoundaryCorner.getBiome());

        // scan the claim for player placed blocks
        double score = 0;

        boolean creativeMode = GriefPrevention.instance.claimModeIsActive(lesserBoundaryCorner.getExtent().getProperties(), ClaimsMode.Creative);

        for (int x = this.lesserBoundaryCorner.getBlockX(); x <= this.greaterBoundaryCorner.getBlockX(); x++) {
            for (int z = this.lesserBoundaryCorner.getBlockZ(); z <= this.greaterBoundaryCorner.getBlockZ(); z++) {
                int y = this.lesserBoundaryCorner.getBlockY();
                for (; y < GriefPrevention.instance.getSeaLevel(this.lesserBoundaryCorner.getExtent()) - 5; y++) {
                    BlockState block = this.lesserBoundaryCorner.getExtent().getBlock(x, y, z);
                    if (playerBlocks.contains(block.getType())) {
                        if (block.getType() == BlockTypes.CHEST && !creativeMode) {
                            score += 10;
                        } else {
                            score += .5;
                        }
                    }
                }

                for (; y < this.lesserBoundaryCorner.getExtent().getDimension().getBuildHeight(); y++) {
                    BlockState block = this.lesserBoundaryCorner.getExtent().getBlock(x, y, z);
                    if (playerBlocks.contains(block.getType())) {
                        if (block.getType() == BlockTypes.CHEST && !creativeMode) {
                            score += 10;
                        } else if (creativeMode && (block.getType() == BlockTypes.LAVA || block.getType() == BlockTypes.FLOWING_LAVA)) {
                            score -= 10;
                        } else {
                            score += 1;
                        }
                    }
                }
            }
        }

        return (long) score;
    }

    public ArrayList<Chunk> getChunks() {
        ArrayList<Chunk> chunks = new ArrayList<Chunk>();

        World world = this.getLesserBoundaryCorner().getExtent();
        Optional<Chunk> lesserChunk = this.getLesserBoundaryCorner().getExtent()
                .getChunk(this.getLesserBoundaryCorner().getBlockX() >> 4, 0, this.getLesserBoundaryCorner().getBlockZ() >> 4);
        Optional<Chunk> greaterChunk = this.getGreaterBoundaryCorner().getExtent()
                .getChunk(this.getGreaterBoundaryCorner().getBlockX() >> 4, 0, this.getGreaterBoundaryCorner().getBlockZ() >> 4);

        if (lesserChunk.isPresent() && greaterChunk.isPresent()) {
            for (int x = lesserChunk.get().getPosition().getX(); x <= greaterChunk.get().getPosition().getX(); x++) {
                for (int z = lesserChunk.get().getPosition().getZ(); z <= greaterChunk.get().getPosition().getZ(); z++) {
                    Optional<Chunk> chunk = world.loadChunk(x, 0, z, true);
                    if (chunk.isPresent()) {
                        chunks.add(chunk.get());
                    }
                }
            }
        }

        return chunks;
    }

    public Set<Long> getChunkHashes() {
        Set<Long> chunkHashes = new HashSet<Long>();
        int smallX = this.getLesserBoundaryCorner().getBlockX() >> 4;
        int smallZ = this.getLesserBoundaryCorner().getBlockZ() >> 4;
        int largeX = this.getGreaterBoundaryCorner().getBlockX() >> 4;
        int largeZ = this.getGreaterBoundaryCorner().getBlockZ() >> 4;

        for (int x = smallX; x <= largeX; x++) {
            for (int z = smallZ; z <= largeZ; z++) {
                chunkHashes.add(ChunkPos.chunkXZ2Int(x, z));
            }
        }

        return chunkHashes;
    }

    public IClaimData getClaimData() {
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
        if (this.isWildernessClaim()) {
            this.claimStorage.getConfig().setWorldUniqueId(this.world.getUniqueId());
        } else if (this.isBasicClaim() || this.isAdminClaim()) {
            this.claimStorage.getConfig().setClaimOwnerUniqueId(this.ownerID);
            this.claimStorage.getConfig().setWorldUniqueId(this.world.getUniqueId());
        } else if (this.isSubdivision()){
            if (this.getClaimData() == null) {
                this.setClaimData(new SubDivisionDataConfig(this));
            }
            this.claimStorage.getConfig().getSubdivisions().put(this.id, (SubDivisionDataConfig) this.getClaimData());
        }

        this.claimStorage.getConfig().setClaimType(this.type);
        this.claimData.setLesserBoundaryCorner(BlockUtils.positionToString(this.lesserBoundaryCorner));
        this.claimData.setGreaterBoundaryCorner(BlockUtils.positionToString(this.greaterBoundaryCorner));
        this.claimData.setCuboid(this.cuboid);
        // Will save next world save
        this.getClaimData().setRequiresSave(true);
    }

    public boolean protectPlayersInClaim() {
        GriefPreventionConfig<?> activeConfig = GriefPrevention.getActiveConfig(this.world.getProperties());
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
        return GPPermissionHandler.getClaimBooleanFlagPermission(this, GPPermissions.PVP) == Tristate.TRUE;
    }

    public void setPvpEnabled(Tristate value) {
        GriefPrevention.GLOBAL_SUBJECT.getSubjectData().setPermission(ImmutableSet.of(this.context), GPPermissions.PVP, value);
    }

    public boolean pvpRulesApply() {
        if (!this.isPvpEnabled()) {
            return false;
        }

        GriefPreventionConfig<?> activeConfig = GriefPrevention.getActiveConfig(this.world.getProperties());
        if (activeConfig != null) {
            return activeConfig.getConfig().pvp.rulesEnabled;
        }

        return false;
    }

    public void unload() {
        // clear any references
        this.world = null;
    }

    @Override
    public Context getContext() {
        return this.context;
    }
}
