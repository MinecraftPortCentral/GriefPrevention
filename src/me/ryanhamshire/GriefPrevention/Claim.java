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

import me.ryanhamshire.GriefPrevention.command.CommandHelper;
import me.ryanhamshire.GriefPrevention.configuration.ClaimStorageData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextSource;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

//represents a player claim
//creating an instance doesn't make an effective claim
//only claims which have been added to the datastore have any effect
public class Claim implements ContextSource {

    // two locations, which together define the boundaries of the claim
    // note that the upper Y value is always ignored, because claims ALWAYS
    // extend up to the sky
    public Location<World> lesserBoundaryCorner;
    public Location<World> greaterBoundaryCorner;
    public World world;

    // Permission Context
    public Context context;

    // modification date. this comes from the file timestamp during load, and is
    // updated with runtime changes
    public Date modifiedDate;

    // id number. unique to this claim, never changes.
    UUID id = null;

    // ownerID. for admin claims, this is NULL
    // use getOwnerName() to get a friendly name (will be "an administrator" for admin claims)
    public UUID ownerID;

    public ClaimStorageData claimData;

    // list of players who (beyond the claim owner) have permission to grant permissions in this claim
    public ArrayList<String> managers = new ArrayList<String>();

    // permissions for this claim, see ClaimPermission class
    private HashMap<String, ClaimPermission> playerIDToClaimPermissionMap = new HashMap<String, ClaimPermission>();

    // whether or not this claim is in the data store
    // if a claim instance isn't in the data store, it isn't "active" - players can't interract with it
    // why keep this? so that claims which have been removed from the data store can be correctly
    // ignored even though they may have references floating around
    public boolean inDataStore = false;

    public boolean areExplosivesAllowed = false;

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

    // items not allowed to be used in claim
    public Map<ItemType, List<Integer>> bannedItemIds;

    // whether or not this is an administrative claim
    // administrative claims are created and maintained by players with the
    // griefprevention.adminclaims permission.
    public boolean isAdminClaim() {
        if (this.parent != null)
            return this.parent.isAdminClaim();

        return (this.ownerID == null);
    }

    // accessor for ID
    public UUID getID() {
        return this.id;
    }

    // basic constructor, just notes the creation time
    // see above declarations for other defaults
    Claim() {
        this.modifiedDate = Calendar.getInstance().getTime();
    }

    // players may only siege someone when he's not in an admin claim
    // and when he has some level of permission in the claim
    public boolean canSiege(Player defender) {
        if (this.isAdminClaim())
            return false;

        if (this.allowAccess(defender.getWorld(), defender) != null)
            return false;

        return true;
    }

    // removes any lava above sea level in a claim
    // exclusionClaim is another claim indicating an sub-area to be excluded
    // from this operation
    // it may be null
    public void removeSurfaceFluids(Claim exclusionClaim) {
        // don't do this for administrative claims
        if (this.isAdminClaim())
            return;

        // don't do it for very large claims
        if (this.getArea() > 10000)
            return;

        // only in creative mode worlds
        if (!GriefPrevention.instance.claimModeIsActive(this.lesserBoundaryCorner.getExtent().getProperties(), ClaimsMode.Creative))
            return;

        Location<World> lesser = this.getLesserBoundaryCorner();
        Location<World> greater = this.getGreaterBoundaryCorner();

        if (lesser.getExtent().getDimension().getType().equals(DimensionTypes.NETHER))
            return; // don't clean up lava in the nether

        int seaLevel = 0; // clean up all fluids in the end

        // respect sea level in normal worlds
        if (lesser.getExtent().getDimension().getType().equals(DimensionTypes.OVERWORLD))
            seaLevel = GriefPrevention.instance.getSeaLevel(lesser.getExtent());

        for (int x = lesser.getBlockX(); x <= greater.getBlockX(); x++) {
            for (int z = lesser.getBlockZ(); z <= greater.getBlockZ(); z++) {
                for (int y = seaLevel - 1; y <= lesser.getExtent().getDimension().getBuildHeight(); y++) {
                    // dodge the exclusion claim
                    BlockSnapshot block = lesser.getExtent().createSnapshot(x, y, z);
                    if (exclusionClaim != null && exclusionClaim.contains(block.getLocation().get(), true, false))
                        continue;

                    if (block.getState().getType() == BlockTypes.LAVA || block.getState().getType() == BlockTypes.FLOWING_WATER
                            || block.getState().getType() == BlockTypes.WATER || block.getState().getType() == BlockTypes.FLOWING_LAVA) {
                        block.withState(BlockTypes.AIR.getDefaultState()).restore(true, false);
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
        if (this.getArea() > 10000)
            return false;

        int seaLevel = 0; // clean up all fluids in the end

        // respect sea level in normal worlds
        if (lesser.getExtent().getDimension().getType().equals(DimensionTypes.OVERWORLD))
            seaLevel = GriefPrevention.instance.getSeaLevel(lesser.getExtent());

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

    // main constructor. note that only creating a claim instance does nothing -
    // a claim must be added to the data store to be effective
    public Claim(Location<World> lesserBoundaryCorner, Location<World> greaterBoundaryCorner, UUID ownerID, List<String> builderIDs, List<String> containerIDs,
            List<String> accessorIDs, List<String> managerIDs, UUID id) {
        // modification date
        this.modifiedDate = Calendar.getInstance().getTime();

        // id
        this.id = id;

        // store corners
        this.lesserBoundaryCorner = lesserBoundaryCorner;
        this.greaterBoundaryCorner = greaterBoundaryCorner;
        this.world = lesserBoundaryCorner.getExtent();

        // owner
        this.ownerID = ownerID;

        // other permissions
        for (String builderID : builderIDs) {
            if (builderID != null && !builderID.isEmpty()) {
                this.playerIDToClaimPermissionMap.put(builderID, ClaimPermission.Build);
            }
        }

        for (String containerID : containerIDs) {
            if (containerID != null && !containerID.isEmpty()) {
                this.playerIDToClaimPermissionMap.put(containerID, ClaimPermission.Inventory);
            }
        }

        for (String accessorID : accessorIDs) {
            if (accessorID != null && !accessorID.isEmpty()) {
                this.playerIDToClaimPermissionMap.put(accessorID, ClaimPermission.Access);
            }
        }

        for (String managerID : managerIDs) {
            if (managerID != null && !managerID.isEmpty()) {
                this.managers.add(managerID);
            }
        }
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

    // distance check for claims, distance in this case is a band around the
    // outside of the claim rather then euclidean distance
    public boolean isNear(Location<World> location, int howNear) {
        Claim claim = new Claim(new Location<World>(this.lesserBoundaryCorner.getExtent(), this.lesserBoundaryCorner.getBlockX() - howNear,
                this.lesserBoundaryCorner.getBlockY(), this.lesserBoundaryCorner.getBlockZ() - howNear),
                new Location<World>(this.greaterBoundaryCorner.getExtent(), this.greaterBoundaryCorner.getBlockX() + howNear,
                        this.greaterBoundaryCorner.getBlockY(), this.greaterBoundaryCorner.getBlockZ() + howNear),
                null, new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), null);

        return claim.contains(location, false, true);
    }

    // permissions. note administrative "public" claims have different rules
    // than other claims
    // all of these return NULL when a player has permission, or a String error
    // message when the player doesn't have permission
    public String allowEdit(Player player) {
        // if we don't know who's asking, always say no (i've been told some
        // mods can make this happen somehow)
        if (player == null)
            return "";

        // special cases...

        // admin claims need adminclaims permission only.
        if (this.isAdminClaim()) {
            if (player.hasPermission("griefprevention.adminclaims"))
                return null;
        }

        // anyone with deleteclaims permission can modify non-admin claims at
        // any time
        else {
            if (player.hasPermission("griefprevention.deleteclaims"))
                return null;
        }

        // no resizing, deleting, and so forth while under siege
        if (player.getUniqueId().equals(this.ownerID)) {
            if (this.siegeData != null) {
                return GriefPrevention.instance.dataStore.getMessage(Messages.NoModifyDuringSiege);
            }

            // otherwise, owners can do whatever
            return null;
        }

        // permission inheritance for subdivisions
        if (this.parent != null)
            return this.parent.allowEdit(player);

        // error message if all else fails
        return GriefPrevention.instance.dataStore.getMessage(Messages.OnlyOwnersModifyClaims, this.getOwnerName());
    }

    private List<BlockType> placeableFarmingBlocksList = Arrays.asList(
            BlockTypes.PUMPKIN_STEM,
            BlockTypes.WHEAT,
            BlockTypes.MELON_STEM,
            BlockTypes.CARROTS,
            BlockTypes.POTATOES,
            BlockTypes.NETHER_WART);

    private boolean placeableForFarming(BlockType material) {
        return this.placeableFarmingBlocksList.contains(material);
    }

    // build permission check
    public String allowBuild(Player player, BlockType blockType) {
        // if we don't know who's asking, always say no (i've been told some
        // mods can make this happen somehow)
        if (player == null) {
            return "";
        }

        // when a player tries to build in a claim, if he's under siege, the
        // siege may extend to include the new claim
        GriefPrevention.instance.dataStore.tryExtendSiege(player, this);

        // admin claims can always be modified by admins, no exceptions
        if (this.isAdminClaim()) {
            if (player.hasPermission("griefprevention.adminclaims")) {
                return null;
        }
        }

        // no building while under siege
        if (this.siegeData != null) {
            return GriefPrevention.instance.dataStore.getMessage(Messages.NoBuildUnderSiege, this.siegeData.attacker.getName());
        }

        // no building while in pvp combat
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        if (playerData.inPvpCombat(player.getWorld())) {
            return GriefPrevention.instance.dataStore.getMessage(Messages.NoBuildPvP);
        }

        // owners can make changes, or admins with ignore claims mode enabled
        if (player.getUniqueId().equals(this.ownerID) || GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId()).ignoreClaims) {
            return null;
        }

        // anyone with explicit build permission can make changes
        if (this.hasExplicitPermission(player, ClaimPermission.Build)) {
            return null;
        }

        // also everyone is a member of the "public", so check for public permission
        ClaimPermission permissionLevel = this.playerIDToClaimPermissionMap.get("public");
        if (ClaimPermission.Build == permissionLevel) {
            return null;
        }

        // allow for farming with /containertrust permission
        if (this.allowContainers(player) == null) {
            // do allow for farming, if player has /containertrust permission
            if (this.placeableForFarming(blockType)) {
                return null;
            }
        }

        // subdivision permission inheritance
        if (this.parent != null) {
            return this.parent.allowBuild(player, blockType);
        }

        // failure message for all other cases
        String reason = "";
        if (blockType != BlockTypes.FLOWING_WATER && blockType != BlockTypes.FLOWING_LAVA) {
            reason = GriefPrevention.instance.dataStore.getMessage(Messages.NoBuildPermission, this.getOwnerName());
        }
        if (player.hasPermission("griefprevention.ignoreclaims")) {
            reason += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
        }

        return reason;
    }

    private boolean hasExplicitPermission(User player, ClaimPermission level) {
        String playerID = player.getUniqueId().toString();
        Set<String> keys = this.playerIDToClaimPermissionMap.keySet();
        Iterator<String> iterator = keys.iterator();
        while (iterator.hasNext()) {
            String identifier = iterator.next();
            if (playerID.equalsIgnoreCase(identifier) && this.playerIDToClaimPermissionMap.get(identifier) == level)
                return true;

            else if (identifier.startsWith("[") && identifier.endsWith("]")) {
                // drop the brackets
                String permissionIdentifier = identifier.substring(1, identifier.length() - 1);

                // defensive coding
                if (permissionIdentifier == null || permissionIdentifier.isEmpty())
                    continue;

                // check permission
                if (player.hasPermission(permissionIdentifier) && this.playerIDToClaimPermissionMap.get(identifier) == level)
                    return true;
            }
        }

        return false;
    }

    // break permission check
    public String allowBreak(Player player, BlockType blockType) {
        // if under siege, some blocks will be breakable
        if (this.siegeData != null || this.doorsOpen) {
            boolean breakable = false;

            // search for block type in list of breakable blocks
            for (int i = 0; i < GriefPrevention.getActiveConfig(player.getWorld().getProperties()).getConfig().siege.breakableSiegeBlocks.size(); i++) {
                String blockTypeId = GriefPrevention.getActiveConfig(player.getWorld().getProperties()).getConfig().siege.breakableSiegeBlocks.get(i);
                Optional<BlockType> breakableBlockType = Sponge.getGame().getRegistry().getType(BlockType.class, blockTypeId);
                if (breakableBlockType.isPresent() && breakableBlockType.get() == blockType) {
                    breakable = true;
                    break;
                }
            }

            // custom error messages for siege mode
            if (!breakable) {
                return GriefPrevention.instance.dataStore.getMessage(Messages.NonSiegeMaterial);
            } else if (player.getUniqueId().equals(this.ownerID)) {
                return GriefPrevention.instance.dataStore.getMessage(Messages.NoOwnerBuildUnderSiege);
            } else {
                return null;
            }
        }

        // if not under siege, build rules apply
        return this.allowBuild(player, blockType);
    }

    // access permission check
    public String allowAccess(World world, User player) {
        // following a siege where the defender lost, the claim will allow everyone access for a time
        if (this.doorsOpen) {
            return null;
        }

        // admin claims need adminclaims permission only.
        if (this.isAdminClaim()) {
            if (player.hasPermission("griefprevention.adminclaims")) {
                return null;
            }
        }

        // claim owner and admins in ignoreclaims mode have access
        if (player.getUniqueId().equals(this.ownerID) || GriefPrevention.instance.dataStore.getPlayerData(world, player.getUniqueId()).ignoreClaims) {
            return null;
        }

        // look for explicit individual access, inventory, or build permission
        if (this.hasExplicitPermission(player, ClaimPermission.Access)) {
            return null;
        }
        if (this.hasExplicitPermission(player, ClaimPermission.Inventory)) {
            return null;
        }
        if (this.hasExplicitPermission(player, ClaimPermission.Build)) {
            return null;
        }

        // also check for public permission
        ClaimPermission permissionLevel = this.playerIDToClaimPermissionMap.get("public");
        if (ClaimPermission.Build == permissionLevel || ClaimPermission.Inventory == permissionLevel || ClaimPermission.Access == permissionLevel) {
            return null;
        }

        // permission inheritance for subdivisions
        if (this.parent != null) {
            return this.parent.allowAccess(world, player);
        }

        // catch-all error message for all other cases
        String reason = GriefPrevention.instance.dataStore.getMessage(Messages.NoAccessPermission, this.getOwnerName());
        if (player.hasPermission("griefprevention.ignoreclaims")) {
            reason += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
        }
        return reason;
    }

    // inventory permission check
    public String allowContainers(Player player) {
        // if we don't know who's asking, always say no (i've been told some mods can make this happen somehow)
        if (player == null)
            return "";

        // trying to access inventory in a claim may extend an existing siege to include this claim
        GriefPrevention.instance.dataStore.tryExtendSiege(player, this);

        // if under siege, nobody accesses containers
        if (this.siegeData != null) {
            return GriefPrevention.instance.dataStore.getMessage(Messages.NoContainersSiege, siegeData.attacker.getName());
        }

        // owner and administrators in ignoreclaims mode have access
        if (player.getUniqueId().equals(this.ownerID) || GriefPrevention.instance.dataStore.getPlayerData(player.getWorld(), player.getUniqueId()).ignoreClaims)
            return null;

        // admin claims need adminclaims permission only.
        if (this.isAdminClaim()) {
            if (player.hasPermission("griefprevention.adminclaims"))
                return null;
        }

        // check for explicit individual container or build permission
        if (this.hasExplicitPermission(player, ClaimPermission.Inventory))
            return null;
        if (this.hasExplicitPermission(player, ClaimPermission.Build))
            return null;

        // check for public container or build permission
        ClaimPermission permissionLevel = this.playerIDToClaimPermissionMap.get("public");
        if (ClaimPermission.Build == permissionLevel || ClaimPermission.Inventory == permissionLevel)
            return null;

        // permission inheritance for subdivisions
        if (this.parent != null)
            return this.parent.allowContainers(player);

        // error message for all other cases
        String reason = GriefPrevention.instance.dataStore.getMessage(Messages.NoContainersPermission, this.getOwnerName());
        if (player.hasPermission("griefprevention.ignoreclaims"))
            reason += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
        return reason;
    }

    // grant permission check, relatively simple
    public String allowGrantPermission(Player player) {
        // if we don't know who's asking, always say no (i've been told some mods can make this happen somehow)
        if (player == null)
            return "";

        // anyone who can modify the claim can do this
        if (this.allowEdit(player) == null)
            return null;

        // anyone who's in the managers (/PermissionTrust) list can do this
        for (int i = 0; i < this.managers.size(); i++) {
            String managerID = this.managers.get(i);
            if (player.getUniqueId().toString().equals(managerID))
                return null;

            else if (managerID.startsWith("[") && managerID.endsWith("]")) {
                managerID = managerID.substring(1, managerID.length() - 1);
                if (managerID == null || managerID.isEmpty())
                    continue;
                if (player.hasPermission(managerID))
                    return null;
            }
        }

        // permission inheritance for subdivisions
        if (this.parent != null)
            return this.parent.allowGrantPermission(player);

        // generic error message
        String reason = GriefPrevention.instance.dataStore.getMessage(Messages.NoPermissionTrust, this.getOwnerName());
        if (player.hasPermission("griefprevention.ignoreclaims"))
            reason += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
        return reason;
    }

    // grants a permission for a player or the public
    public void setPermission(String playerID, ClaimPermission permissionLevel) {
        this.playerIDToClaimPermissionMap.put(playerID.toLowerCase(), permissionLevel);
    }

    // revokes a permission for a player or the public
    public void dropPermission(String playerID) {
        this.playerIDToClaimPermissionMap.remove(playerID.toLowerCase());

        for (Claim child : this.children) {
            child.dropPermission(playerID);
        }
    }

    // clears all permissions (except owner of course)
    public void clearPermissions() {
        this.playerIDToClaimPermissionMap.clear();
        this.managers.clear();

        for (Claim child : this.children) {
            child.clearPermissions();
        }
    }

    // gets ALL permissions
    // useful for making copies of permissions during a claim resize and listing all permissions in a claim
    public void getPermissions(ArrayList<String> builders, ArrayList<String> containers, ArrayList<String> accessors, ArrayList<String> managers) {
        // loop through all the entries in the hash map
        Iterator<Map.Entry<String, ClaimPermission>> mappingsIterator = this.playerIDToClaimPermissionMap.entrySet().iterator();
        while (mappingsIterator.hasNext()) {
            Map.Entry<String, ClaimPermission> entry = mappingsIterator.next();

            // build up a list for each permission level
            if (entry.getValue() == ClaimPermission.Build) {
                builders.add(entry.getKey());
            } else if (entry.getValue() == ClaimPermission.Inventory) {
                containers.add(entry.getKey());
            } else {
                accessors.add(entry.getKey());
            }
        }

        // managers are handled a little differently
        for (int i = 0; i < this.managers.size(); i++) {
            managers.add(this.managers.get(i));
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
        if (this.parent != null)
            return this.parent.getOwnerName();

        if (this.ownerID == null)
            return GriefPrevention.instance.dataStore.getMessage(Messages.OwnerNameForAdminClaims);

        return CommandHelper.lookupPlayerName(this.ownerID);
    }

    // whether or not a location is in a claim
    // ignoreHeight = true means location UNDER the claim will return TRUE
    // excludeSubdivisions = true means that locations inside subdivisions of the claim will return FALSE
    public boolean contains(Location<World> location, boolean ignoreHeight, boolean excludeSubdivisions) {
        // not in the same world implies false
        if (!location.getExtent().equals(this.lesserBoundaryCorner.getExtent()))
            return false;

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        // main check
        boolean inClaim = (ignoreHeight || y >= this.lesserBoundaryCorner.getY()) &&
                x >= this.lesserBoundaryCorner.getX() &&
                x < this.greaterBoundaryCorner.getX() + 1 &&
                z >= this.lesserBoundaryCorner.getZ() &&
                z < this.greaterBoundaryCorner.getZ() + 1;

        if (!inClaim)
            return false;

        // additional check for subdivisions
        // you're only in a subdivision when you're also in its parent claim
        // NOTE: if a player creates subdivions then resizes the parent claim,
        // it's possible that
        // a subdivision can reach outside of its parent's boundaries. so this
        // check is important!
        if (this.parent != null) {
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

    // whether or not two claims overlap
    // used internally to prevent overlaps when creating claims
    boolean overlaps(Claim otherClaim) {
        // NOTE: if trying to understand this makes your head hurt, don't feel
        // bad - it hurts mine too.
        // try drawing pictures to visualize test cases.

        if (!this.lesserBoundaryCorner.getExtent().equals(otherClaim.getLesserBoundaryCorner().getExtent()))
            return false;

        // first, check the corners of this claim aren't inside any existing
        // claims
        if (otherClaim.contains(this.lesserBoundaryCorner, true, false))
            return true;
        if (otherClaim.contains(this.greaterBoundaryCorner, true, false))
            return true;
        if (otherClaim.contains(
                new Location<World>(this.lesserBoundaryCorner.getExtent(), this.lesserBoundaryCorner.getBlockX(), 0,
                        this.greaterBoundaryCorner.getBlockZ()),
                true, false))
            return true;
        if (otherClaim.contains(
                new Location<World>(this.lesserBoundaryCorner.getExtent(), this.greaterBoundaryCorner.getBlockX(), 0,
                        this.lesserBoundaryCorner.getBlockZ()),
                true, false))
            return true;

        // verify that no claim's lesser boundary point is inside this new
        // claim, to cover the "existing claim is entirely inside new claim"
        // case
        if (this.contains(otherClaim.getLesserBoundaryCorner(), true, false))
            return true;

        // verify this claim doesn't band across an existing claim, either
        // horizontally or vertically
        if (this.getLesserBoundaryCorner().getBlockZ() <= otherClaim.getGreaterBoundaryCorner().getBlockZ() &&
                this.getLesserBoundaryCorner().getBlockZ() >= otherClaim.getLesserBoundaryCorner().getBlockZ() &&
                this.getLesserBoundaryCorner().getBlockX() < otherClaim.getLesserBoundaryCorner().getBlockX() &&
                this.getGreaterBoundaryCorner().getBlockX() > otherClaim.getGreaterBoundaryCorner().getBlockX())
            return true;

        if (this.getGreaterBoundaryCorner().getBlockZ() <= otherClaim.getGreaterBoundaryCorner().getBlockZ() &&
                this.getGreaterBoundaryCorner().getBlockZ() >= otherClaim.getLesserBoundaryCorner().getBlockZ() &&
                this.getLesserBoundaryCorner().getBlockX() < otherClaim.getLesserBoundaryCorner().getBlockX() &&
                this.getGreaterBoundaryCorner().getBlockX() > otherClaim.getGreaterBoundaryCorner().getBlockX())
            return true;

        if (this.getLesserBoundaryCorner().getBlockX() <= otherClaim.getGreaterBoundaryCorner().getBlockX() &&
                this.getLesserBoundaryCorner().getBlockX() >= otherClaim.getLesserBoundaryCorner().getBlockX() &&
                this.getLesserBoundaryCorner().getBlockZ() < otherClaim.getLesserBoundaryCorner().getBlockZ() &&
                this.getGreaterBoundaryCorner().getBlockZ() > otherClaim.getGreaterBoundaryCorner().getBlockZ())
            return true;

        if (this.getGreaterBoundaryCorner().getBlockX() <= otherClaim.getGreaterBoundaryCorner().getBlockX() &&
                this.getGreaterBoundaryCorner().getBlockX() >= otherClaim.getLesserBoundaryCorner().getBlockX() &&
                this.getLesserBoundaryCorner().getBlockZ() < otherClaim.getLesserBoundaryCorner().getBlockZ() &&
                this.getGreaterBoundaryCorner().getBlockZ() > otherClaim.getGreaterBoundaryCorner().getBlockZ())
            return true;

        return false;
    }

    // whether more entities may be added to a claim
    public String allowMoreEntities() {
        if (this.parent != null)
            return this.parent.allowMoreEntities();

        // this rule only applies to creative mode worlds
        if (!GriefPrevention.instance.claimModeIsActive(this.getLesserBoundaryCorner().getExtent().getProperties(), ClaimsMode.Creative))
            return null;

        // admin claims aren't restricted
        if (this.isAdminClaim())
            return null;

        // don't apply this rule to very large claims
        if (this.getArea() > 10000)
            return null;

        // determine maximum allowable entity count, based on claim size
        int maxEntities = this.getArea() / 50;
        if (maxEntities == 0)
            return GriefPrevention.instance.dataStore.getMessage(Messages.ClaimTooSmallForEntities);

        // count current entities (ignoring players)
        int totalEntities = 0;
        ArrayList<Chunk> chunks = this.getChunks();
        for (Chunk chunk : chunks) {
            ArrayList<Entity> entities = (ArrayList<Entity>) chunk.getEntities();
            for (int i = 0; i < entities.size(); i++) {
                Entity entity = entities.get(i);
                if (!(entity instanceof Player) && this.contains(entity.getLocation(), false, false)) {
                    totalEntities++;
                    if (totalEntities > maxEntities)
                        entity.remove();
                }
            }
        }

        if (totalEntities > maxEntities)
            return GriefPrevention.instance.dataStore.getMessage(Messages.TooManyEntitiesInClaim);

        return null;
    }

    // implements a strict ordering of claims, used to keep the claims
    // collection sorted for faster searching
    boolean greaterThan(Claim otherClaim) {
        Location<World> thisCorner = this.getLesserBoundaryCorner();
        Location<World> otherCorner = otherClaim.getLesserBoundaryCorner();

        if (thisCorner.getBlockX() > otherCorner.getBlockX())
            return true;

        if (thisCorner.getBlockX() < otherCorner.getBlockX())
            return false;

        if (thisCorner.getBlockZ() > otherCorner.getBlockZ())
            return true;

        if (thisCorner.getBlockZ() < otherCorner.getBlockZ())
            return false;

        return thisCorner.getExtent().getUniqueId().compareTo(otherCorner.getExtent().getUniqueId()) < 0;
    }

    long getPlayerInvestmentScore() {
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
        Optional<Chunk> lesserChunk = this.getLesserBoundaryCorner().getExtent().getChunk(this.getLesserBoundaryCorner().getBlockX() >> 4, 0, this.getLesserBoundaryCorner().getBlockZ() >> 4);
        Optional<Chunk> greaterChunk = this.getGreaterBoundaryCorner().getExtent().getChunk(this.getGreaterBoundaryCorner().getBlockX() >> 4, 0, this.getGreaterBoundaryCorner().getBlockZ() >> 4);

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

    public ArrayList<String> getChunkStrings() {
        ArrayList<String> chunkStrings = new ArrayList<String>();
        int smallX = this.getLesserBoundaryCorner().getBlockX() >> 4;
        int smallZ = this.getLesserBoundaryCorner().getBlockZ() >> 4;
        int largeX = this.getGreaterBoundaryCorner().getBlockX() >> 4;
        int largeZ = this.getGreaterBoundaryCorner().getBlockZ() >> 4;

        for (int x = smallX; x <= largeX; x++) {
            for (int z = smallZ; z <= largeZ; z++) {
                chunkStrings.add(String.valueOf(x) + z);
            }
        }

        return chunkStrings;
    }

    public ClaimStorageData getClaimData() {
        return this.claimData;
    }

    public boolean isItemBlacklisted(ItemType type, int meta) {
        String nonMetaItemString = type.getId();
        String metaItemString = type.getId() + ":" + meta;
        // TODO: fix possible NPE here
        if (this.claimData.getConfig().protectionBlacklist.contains(nonMetaItemString)) {
            return true;
        } else if (this.claimData.getConfig().protectionBlacklist.contains(metaItemString)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Context getContext() {
        return this.context;
    }
}
