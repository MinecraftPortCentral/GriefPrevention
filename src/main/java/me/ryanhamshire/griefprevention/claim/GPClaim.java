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
import com.google.common.collect.ImmutableMap;
import me.ryanhamshire.griefprevention.DataStore;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.ShovelMode;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimBlockSystem;
import me.ryanhamshire.griefprevention.api.claim.ClaimContexts;
import me.ryanhamshire.griefprevention.api.claim.ClaimFlag;
import me.ryanhamshire.griefprevention.api.claim.ClaimManager;
import me.ryanhamshire.griefprevention.api.claim.ClaimResult;
import me.ryanhamshire.griefprevention.api.claim.ClaimResultType;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.api.claim.FlagResult;
import me.ryanhamshire.griefprevention.api.claim.FlagResultType;
import me.ryanhamshire.griefprevention.api.claim.TrustType;
import me.ryanhamshire.griefprevention.api.data.ClaimData;
import me.ryanhamshire.griefprevention.command.CommandHelper;
import me.ryanhamshire.griefprevention.configuration.ClaimDataConfig;
import me.ryanhamshire.griefprevention.configuration.ClaimStorageData;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.configuration.IClaimData;
import me.ryanhamshire.griefprevention.configuration.MessageStorage;
import me.ryanhamshire.griefprevention.configuration.TownDataConfig;
import me.ryanhamshire.griefprevention.configuration.TownStorageData;
import me.ryanhamshire.griefprevention.event.GPChangeClaimEvent;
import me.ryanhamshire.griefprevention.event.GPCreateClaimEvent;
import me.ryanhamshire.griefprevention.event.GPDeleteClaimEvent;
import me.ryanhamshire.griefprevention.event.GPFlagClaimEvent;
import me.ryanhamshire.griefprevention.event.GPGroupTrustClaimEvent;
import me.ryanhamshire.griefprevention.event.GPTransferClaimEvent;
import me.ryanhamshire.griefprevention.event.GPUserTrustClaimEvent;
import me.ryanhamshire.griefprevention.permission.GPOptionHandler;
import me.ryanhamshire.griefprevention.permission.GPOptions;
import me.ryanhamshire.griefprevention.permission.GPPermissionHandler;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import me.ryanhamshire.griefprevention.util.BlockUtils;
import me.ryanhamshire.griefprevention.util.PermissionUtils;
import me.ryanhamshire.griefprevention.visual.Visualization;
import me.ryanhamshire.griefprevention.visual.VisualizationType;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.BlockChangeFlags;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.SpongeImpl;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

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
    private ClaimType type = ClaimType.BASIC;
    private Set<Long> chunkHashes;
    private final int hashCode;
    private final GPClaimManager worldClaimManager;
    private final Claim wildernessClaim;

    // Permission Context
    public Context context;

    // id number. unique to this claim, never changes.
    public UUID id = null;

    private UUID ownerUniqueId;

    public boolean cuboid = false;

    private ClaimStorageData claimStorage;
    private IClaimData claimData;

    public GPClaim parent = null;
    public ArrayList<Claim> children = new ArrayList<>();
    public Visualization visualization;
    public List<UUID> playersWatching = new ArrayList<>();

    private GPPlayerData ownerPlayerData;
    private Account economyAccount;
    private static final int MAX_AREA = GriefPreventionPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME ? 2560000 : 10000;

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
        if (ownerUniqueId != null) {
            this.ownerUniqueId = ownerUniqueId;
            this.ownerPlayerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(this.world, this.ownerUniqueId);
        }
        this.type = type;
        this.id = UUID.randomUUID();
        this.context = new Context("gp_claim", this.id.toString());
        this.cuboid = cuboid;
        this.parent = parent;
        this.hashCode = this.id.hashCode();
        this.worldClaimManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(this.world.getProperties());
        if (this.type == ClaimType.WILDERNESS) {
            this.wildernessClaim = this;
        } else {
            this.wildernessClaim = this.worldClaimManager.getWildernessClaim();
        }
    }

    // Used for visualizations
    public GPClaim(Location<World> lesserBoundaryCorner, Location<World> greaterBoundaryCorner, ClaimType type, boolean cuboid) {
        this(lesserBoundaryCorner, greaterBoundaryCorner, UUID.randomUUID(), type, null, cuboid);
    }

    // Used at server startup
    public GPClaim(Location<World> lesserBoundaryCorner, Location<World> greaterBoundaryCorner, UUID claimId, ClaimType type, UUID ownerUniqueId, boolean cuboid) {
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
        this.cuboid = cuboid;
        this.context = new Context("gp_claim", this.id.toString());
        this.hashCode = this.id.hashCode();
        this.worldClaimManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(this.world.getProperties());
        if (this.type == ClaimType.WILDERNESS) {
            this.wildernessClaim = this;
        } else {
            this.wildernessClaim = this.worldClaimManager.getWildernessClaim();
        }
    }

    public void initializeClaimData(GPClaim parent) {
        Path claimDataFolderPath = null;
        // check if main world
        if (parent != null) {
            claimDataFolderPath = parent.getClaimStorage().filePath.getParent().resolve(this.type.name().toLowerCase());
        } else {
            claimDataFolderPath = DataStore.worldConfigMap.get(this.world.getUniqueId()).getPath().getParent().resolve("ClaimData").resolve(this.type.name().toLowerCase());
        }
        try {
            Files.createDirectories(claimDataFolderPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        File claimFile = new File(claimDataFolderPath + File.separator + this.id);
        if (this.isTown()) {
            this.claimStorage = new TownStorageData(claimFile.toPath(), this.world.getUniqueId(), this.ownerUniqueId, this.cuboid);
        } else {
            this.claimStorage = new ClaimStorageData(claimFile.toPath(), this.world.getUniqueId(), this.ownerUniqueId, this.type, this.cuboid);
        }
        this.claimData = this.claimStorage.getConfig();
        this.parent = parent;

        this.updateClaimStorageData();
    }

    public ClaimType getType() {
        return this.type;
    }

    public void setType(ClaimType type) {
        this.type = type;
        this.claimData.setType(type);
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
        if (this.isAdminClaim()) {
            return GriefPreventionPlugin.ADMIN_USER_UUID;
        }
        if (this.ownerUniqueId == null) {
            if (this.parent != null) {
                return this.parent.getOwnerUniqueId();
            }

            return GriefPreventionPlugin.ADMIN_USER_UUID;
        }

        return this.ownerUniqueId;
    }

    public void setOwnerUniqueId(UUID uniqueId) {
        this.ownerUniqueId = uniqueId;
    }

    public boolean isAdminClaim() {
        return this.type == ClaimType.ADMIN;
    }

    @Override
    public boolean isCuboid() {
        if (this.claimData != null) {
            return this.claimData.isCuboid();
        }

        return this.cuboid;
    }

    @Override
    public boolean isInTown() {
        if (this.isTown()) {
            return true;
        }

        GPClaim parent = this.parent;
        while (parent != null) {
            if (parent.isTown()) {
                return true;
            }
            parent = parent.parent;
        }

        return false;
    }

    @Override
    public Optional<Claim> getTown() {
        return Optional.ofNullable(this.getTownClaim());
    }

    @Nullable
    public GPClaim getTownClaim() {
        if (this.isTown()) {
            return this;
        }

        if (this.parent == null) {
            return null;
        }

        GPClaim parent = this.parent;
        while (parent != null) {
            if (parent.isTown()) {
                return parent;
            }
            parent = parent.parent;
        }

        return null;
    }

    @Override
    public UUID getUniqueId() {
        return this.id;
    }

    public Optional<Text> getName() {
        if (this.claimData == null) {
            return Optional.empty();
        }
        return this.claimData.getName();
    }

    public Text getFriendlyNameType() {
        return this.getFriendlyNameType(false);
    }

    public Text getFriendlyNameType(boolean upper) {
        if (this.type == ClaimType.ADMIN) {
            if (upper) {
                return Text.of(TextColors.RED, this.type.name());
            }
            return Text.of(TextColors.RED, "Admin");
        }

        if (this.type == ClaimType.BASIC) {
            if (upper) {
                return Text.of(TextColors.YELLOW, this.type.name());
            }
            return Text.of(TextColors.YELLOW, "Basic");
        }

        if (this.type == ClaimType.SUBDIVISION) {
            if (upper) {
                return Text.of(TextColors.AQUA, this.type.name());
            }
            return Text.of(TextColors.AQUA, "Subdivision");
        }

        if (upper) {
            return Text.of(TextColors.GREEN, this.type.name());
        }
        return Text.of(TextColors.GREEN, "Town");
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
        if (this.getClaimBlocks() > MAX_AREA) {
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
                for (int y = seaLevel - 1; y < lesser.getExtent().getDimension().getBuildHeight(); y++) {
                    // dodge the exclusion claim
                    BlockSnapshot block = lesser.getExtent().createSnapshot(x, y, z);
                    if (exclusionClaim != null && exclusionClaim.contains(block.getLocation().get(), false)) {
                        continue;
                    }

                    if (block.getState().getType() == BlockTypes.LAVA || block.getState().getType() == BlockTypes.FLOWING_WATER
                            || block.getState().getType() == BlockTypes.WATER || block.getState().getType() == BlockTypes.FLOWING_LAVA) {
                        block.withState(BlockTypes.AIR.getDefaultState()).restore(true, BlockChangeFlags.PHYSICS);
                    }
                }
            }
        }
    }

    // determines whether or not a claim has surface lava
    // used to warn players when they abandon their claims about automatic fluid cleanup
    boolean hasSurfaceFluids() {
        Location<World> lesser = this.getLesserBoundaryCorner();
        Location<World> greater = this.getGreaterBoundaryCorner();

        // don't bother for very large claims, too expensive
        if (this.getClaimBlocks() > MAX_AREA) {
            return false;
        }

        int seaLevel = 0; // clean up all fluids in the end

        // respect sea level in normal worlds
        if (lesser.getExtent().getDimension().getType().equals(DimensionTypes.OVERWORLD)) {
            seaLevel = GriefPreventionPlugin.instance.getSeaLevel(lesser.getExtent());
        }

        for (int x = lesser.getBlockX(); x <= greater.getBlockX(); x++) {
            for (int z = lesser.getBlockZ(); z <= greater.getBlockZ(); z++) {
                for (int y = seaLevel - 1; y < lesser.getExtent().getDimension().getBuildHeight(); y++) {
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

    @Override
    public int getClaimBlocks() {
        if (GriefPreventionPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME) {
            return this.getVolume();
        }

        return this.getArea();
    }

    @Override
    public int getArea() {
        final int claimWidth = this.greaterBoundaryCorner.getBlockX() - this.lesserBoundaryCorner.getBlockX() + 1;
        final int claimLength = this.greaterBoundaryCorner.getBlockZ() - this.lesserBoundaryCorner.getBlockZ() + 1;

        return claimWidth * claimLength;
    }

    @Override
    public int getVolume() {
        final int claimWidth = this.greaterBoundaryCorner.getBlockX() - this.lesserBoundaryCorner.getBlockX() + 1;
        final int claimLength = this.greaterBoundaryCorner.getBlockZ() - this.lesserBoundaryCorner.getBlockZ() + 1;
        final int claimHeight = this.greaterBoundaryCorner.getBlockY() - this.lesserBoundaryCorner.getBlockY() + 1;

        return claimWidth * claimLength * claimHeight;
    }

    @Override
    public int getWidth() {
        return this.greaterBoundaryCorner.getBlockX() - this.lesserBoundaryCorner.getBlockX() + 1;
    }

    @Override
    public int getHeight() {
        return this.greaterBoundaryCorner.getBlockZ() - this.lesserBoundaryCorner.getBlockZ() + 1;
    }

    public Text allowEdit(User user) {
        if (this.isUserTrusted(user, TrustType.MANAGER)) {
            return null;
        }

        // anyone with deleteclaims permission can modify non-admin claims at any time
        if (user.hasPermission(GPPermissions.COMMAND_DELETE_CLAIMS)) {
            return null;
        }

        if (this.parent != null && this.getData().doesInheritParent()) {
            return this.parent.allowEdit(user);
        }

        final Text message = GriefPreventionPlugin.instance.messageData.claimOwnerOnly
                .apply(ImmutableMap.of(
                "owner", Text.of(this.getOwnerName())
        )).build();
        return message;
    }

    //grant permission check, relatively simple
    public Text allowGrantPermission(Player player) {
        //anyone who can modify the claim can do this
        if(this.allowEdit(player) == null) {
            return null;
        }

        for(int i = 0; i < this.claimData.getManagers().size(); i++) {
            UUID managerID = this.claimData.getManagers().get(i);
            if(player.getUniqueId().equals(managerID)) {
                return null;
            }
        }

        if(this.parent != null && this.getData().doesInheritParent()) {
            return this.parent.allowGrantPermission(player);
        }

        final Text reason = GriefPreventionPlugin.instance.messageData.permissionTrust
                .apply(ImmutableMap.of(
                "owner", Text.of(this.getOwnerName())
        )).build();
        return reason;
    }

    // returns a copy of the location representing lower x, y, z limits
    @Override
    public Location<World> getLesserBoundaryCorner() {
        return (Location<World>) this.lesserBoundaryCorner.copy();
    }

    // returns a copy of the location representing upper x, y, z limits
    @Override
    public Location<World> getGreaterBoundaryCorner() {
        return (Location<World>) this.greaterBoundaryCorner.copy();
    }

    // returns a friendly owner name (for admin claims, returns "an administrator" as the owner)
    @Override
    public Text getOwnerName() {
        if (this.isAdminClaim() || this.isWilderness()) {
            return GriefPreventionPlugin.instance.messageData.ownerAdmin.toText();
        }

        if (this.getOwnerPlayerData() == null) {
            return Text.of("[unknown]");
        }

        return Text.of(this.getOwnerPlayerData().getPlayerName());
    }

    @Override
    public boolean contains(Location<World> location, boolean excludeChildren) {
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        // main check
        boolean inClaim = (
                y >= this.lesserBoundaryCorner.getBlockY()) &&
                y < this.greaterBoundaryCorner.getBlockY() + 1 &&
                x >= this.lesserBoundaryCorner.getBlockX() &&
                x < this.greaterBoundaryCorner.getBlockX() + 1 &&
                z >= this.lesserBoundaryCorner.getBlockZ() &&
                z < this.greaterBoundaryCorner.getBlockZ() + 1;

        if (!inClaim) {
            return false;
        }

        // additional check for children claims, you're only in a child claim when you're also in its parent claim
        // NOTE: if a player creates children then resizes the parent claim,
        // it's possible that a child can reach outside of its parent's boundaries. so this check is important!
        if (!excludeChildren && this.parent != null && (this.getData() == null || (this.getData() != null && this.getData().doesInheritParent()))) {
            return this.parent.contains(location, false);
        }

        return true;
    }

    public boolean isClaimOnBorder(GPClaim claim) {
        if (claim.cuboid) {
            return false;
        }

        boolean result = claim.lesserBoundaryCorner.getBlockX() == this.lesserBoundaryCorner.getBlockX() ||
               claim.greaterBoundaryCorner.getBlockX() == this.greaterBoundaryCorner.getBlockX() ||
               claim.lesserBoundaryCorner.getBlockZ() == this.lesserBoundaryCorner.getBlockZ() ||
               claim.greaterBoundaryCorner.getBlockZ() == this.greaterBoundaryCorner.getBlockZ();
        if (claim.cuboid) {
            result = claim.lesserBoundaryCorner.getBlockY() == this.lesserBoundaryCorner.getBlockY() ||
                    claim.greaterBoundaryCorner.getBlockY() == this.greaterBoundaryCorner.getBlockY();
        }
        return result;
    }

    @Override
    public boolean overlaps(Claim other) {
        GPClaim otherClaim = (GPClaim) other;
        if (this.id == otherClaim.id) {
            return false;
        }

        // Handle claims entirely within a town
        if (this.isTown() && !otherClaim.isTown() && otherClaim.isInside(this)) {
            return false;
        }

        //verify that no claim's lesser boundary point is inside this new claim, to cover the "existing claim is entirely inside new claim" case
        if(this.contains(otherClaim.getLesserBoundaryCorner(), false)) {
            return true;
        }

        return this.isBandingAcross(otherClaim);
    }

    //Checks if claim bands across another claim, either horizontally or vertically
    public boolean isBandingAcross(GPClaim otherClaim) {
        final boolean isClaimInside = otherClaim.isInside(this);
        if (isClaimInside) {
            return false;
        }

        final int smallX = otherClaim.getLesserBoundaryCorner().getBlockX();
        final int smallY = otherClaim.getLesserBoundaryCorner().getBlockY();
        final int smallZ = otherClaim.getLesserBoundaryCorner().getBlockZ();
        final int bigX = otherClaim.getGreaterBoundaryCorner().getBlockX();
        final int bigY = otherClaim.getGreaterBoundaryCorner().getBlockY();
        final int bigZ = otherClaim.getGreaterBoundaryCorner().getBlockZ();

        if(this.contains(otherClaim.lesserBoundaryCorner, false)) {
            return true;
        }
        if(this.contains(otherClaim.greaterBoundaryCorner, false)) {
            return true;
        }
        if(this.contains(new Location<World>(this.world, smallX, 0, bigZ), false)) {
            return true;
        }
        if(this.contains(new Location<World>(this.world, bigX, 0, smallZ), false)) {
            return true;
        }

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
            // check height
            if ((this.lesserBoundaryCorner.getBlockY() >= smallY &&
                 this.lesserBoundaryCorner.getBlockY() <= bigY) ||
                (this.greaterBoundaryCorner.getBlockY() <= smallY &&
                 this.greaterBoundaryCorner.getBlockY() >= smallY)) {
                return true;
            }

            return false;
        }

        return false;
    }

    @Override
    public boolean isInside(Claim claim) {
        final GPClaim otherClaim = (GPClaim) claim;
        if(!otherClaim.contains(this.lesserBoundaryCorner)) {
            return false;
        }
        if(!otherClaim.contains(this.greaterBoundaryCorner)) {
            return false;
        }

        if(!otherClaim.contains(new Location<World>(this.world, this.lesserBoundaryCorner.getBlockX(), this.lesserBoundaryCorner.getBlockY(), this.greaterBoundaryCorner.getBlockZ()))) {
            return false;
        }
        if(!otherClaim.contains(new Location<World>(this.world, this.greaterBoundaryCorner.getBlockX(), this.greaterBoundaryCorner.getBlockY(), this.lesserBoundaryCorner.getBlockZ()))) {
            return false;
        }

        return true;
    }

    @Override
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

    public boolean canIgnoreHeight() {
        if (this.isCuboid()) {
            return false;
        }
        if (this.ownerPlayerData != null && (this.ownerPlayerData.getMinClaimLevel() > 0 || this.ownerPlayerData.getMaxClaimLevel() < 255)) {
            return false;
        }

        return true;
    }

    @Override
    public Set<Long> getChunkHashes() {
        return this.getChunkHashes(true);
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
                    this.chunkHashes.add(ChunkPos.asLong(x, z));
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

    @Nullable
    public TownDataConfig getTownData() {
        if (!(this.claimData instanceof TownDataConfig)) {
            return null;
        }

        return (TownDataConfig) this.claimData;
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
        if (!this.isAdminClaim()) {
            this.claimStorage.getConfig().setOwnerUniqueId(this.getOwnerUniqueId());
        }
        this.claimStorage.getConfig().setWorldUniqueId(this.world.getUniqueId());
        this.claimData.setCuboid(this.cuboid);
        this.claimData.setType(this.type);
        this.claimData.setLesserBoundaryCorner(BlockUtils.positionToString(this.lesserBoundaryCorner));
        this.claimData.setGreaterBoundaryCorner(BlockUtils.positionToString(this.greaterBoundaryCorner));
        // Will save next world save
        this.claimData.setRequiresSave(true);
    }

    public void save() {
        for (Claim child : this.children) {
            GPClaim childClaim = (GPClaim) child;
            if (childClaim.getInternalClaimData().requiresSave()) {
                childClaim.save();
            }
        }
        if (this.getInternalClaimData().requiresSave()) {
            this.updateClaimStorageData();
            this.getClaimStorage().save();
            this.getInternalClaimData().setRequiresSave(false);
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
        Tristate value = this.claimData.getPvpOverride();
        if (value != Tristate.UNDEFINED) {
            return value.asBoolean();
        }

        return this.world.getProperties().isPVPEnabled();
    }

    public void setPvpOverride(Tristate value) {
        this.claimData.setPvpOverride(value);
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

        if (this.isBasicClaim() && this.claimData.requiresClaimBlocks()) {
            int remainingClaimBlocks = newOwnerData.getRemainingClaimBlocks();
            if (remainingClaimBlocks < 0 || (this.getClaimBlocks() > remainingClaimBlocks)) {
                return new GPClaimResult(ClaimResultType.INSUFFICIENT_CLAIM_BLOCKS);
            }
        }

        // Check limits
        final Player currentOwner = ownerData.getPlayerSubject() instanceof Player ? (Player) ownerData.getPlayerSubject() : null;
        final Double createClaimLimit = GPOptionHandler.getClaimOptionDouble(newOwnerData.getPlayerSubject(), this, GPOptions.Type.CLAIM_LIMIT, newOwnerData);
        if (createClaimLimit != null && createClaimLimit > 0 && (newOwnerData.getInternalClaims().size() + 1) > createClaimLimit.intValue()) {
            if (currentOwner != null) {
                GriefPreventionPlugin.sendMessage(currentOwner, GriefPreventionPlugin.instance.messageData.claimTransferExceedsLimit.toText());
            }
            return new GPClaimResult(this, ClaimResultType.EXCEEDS_MAX_CLAIM_LIMIT, GriefPreventionPlugin.instance.messageData.claimTransferExceedsLimit.toText());
        }

        // transfer
        GPTransferClaimEvent event = new GPTransferClaimEvent(this, this.getOwnerUniqueId(), newOwnerID);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            return new GPClaimResult(this, ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        if (this.isAdminClaim()) {
            // convert to basic
            this.type = ClaimType.BASIC;
            this.getVisualizer().setType(VisualizationType.CLAIM);
            this.claimData.setType(ClaimType.BASIC);
        }

        this.ownerUniqueId = event.getNewOwner();
        if (!this.getOwnerUniqueId().equals(newOwnerID)) {
            newOwnerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(this.world, this.getOwnerUniqueId());
        }

        this.claimData.setOwnerUniqueId(newOwnerID);
        if (this.isBasicClaim()) {
            ownerData.getInternalClaims().remove(this);
            newOwnerData.getInternalClaims().add(this);
        }

        this.ownerPlayerData = newOwnerData;
        this.getClaimStorage().save();
        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    public ClaimResult doesClaimOverlap() {
        if (this.parent != null) {
            final GPClaim parentClaim = (GPClaim) this.parent;
            // 1 - Make sure new claim is inside parent
            if (!this.isInside(parentClaim)) {
                return new GPClaimResult(parentClaim, ClaimResultType.OVERLAPPING_CLAIM);
            }

            // 2 - Check parent children
            for (Claim child : parentClaim.children) {
                final GPClaim childClaim = (GPClaim) child;
                if (this.isBandingAcross(childClaim) || childClaim.isBandingAcross(this)) {
                    return new GPClaimResult(childClaim, ClaimResultType.OVERLAPPING_CLAIM);
                }
            }
            return new GPClaimResult(this, ClaimResultType.SUCCESS);
        }

        final GPClaimManager claimWorldManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(this.world.getProperties());
        final Set<Long> chunkHashes = this.getChunkHashes(true);

        // Since there is no parent we need to check all claims stored in chunk hashes
        for (Long chunkHash : chunkHashes) {
            Set<Claim> claimsInChunk = claimWorldManager.getInternalChunksToClaimsMap().get(chunkHash);
            if (claimsInChunk == null || claimsInChunk.size() == 0) {
                continue;
            }
            for (Claim child : claimsInChunk) {
                final GPClaim gpChild = (GPClaim) child;
                // First check if newly resized claim is crossing another
                if (this.isBandingAcross(gpChild) || gpChild.isBandingAcross(this)) {
                    return new GPClaimResult(child, ClaimResultType.OVERLAPPING_CLAIM);
                }
            }
        }

        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    // Scans area for any overlaps and migrates children to a newly created or resized claim
    public ClaimResult checkArea(boolean resize) {
        final List<Claim> claimsInArea = new ArrayList<>();
        claimsInArea.add(this);

        if (this.parent != null) {
            if (this.isClaimOnBorder(this.parent)) {
                return new GPClaimResult(this.parent, ClaimResultType.OVERLAPPING_CLAIM);
            }
            final GPClaim parentClaim = (GPClaim) this.parent;
            // 1 - Make sure new claim is inside parent
            if (!this.isInside(parentClaim)) {
                return new GPClaimResult(parentClaim, ClaimResultType.OVERLAPPING_CLAIM);
            }

            // 2 - Check parent children
            for (Claim child : parentClaim.children) {
                final GPClaim childClaim = (GPClaim) child;
                if (this.equals(child)) {
                    continue;
                }
                if (this.isBandingAcross(childClaim) || childClaim.isBandingAcross(this)) {
                    return new GPClaimResult(childClaim, ClaimResultType.OVERLAPPING_CLAIM);
                }
                if (childClaim.isInside(this)) {
                    if (this.type.equals(childClaim.type)) {
                        return new GPClaimResult(childClaim, ClaimResultType.OVERLAPPING_CLAIM);
                    }
                    if (!this.isSubdivision()) {
                        claimsInArea.add(childClaim);
                    }
                }
                // ignore claims not inside
            }

            if (resize) {
                // Make sure children are still within their parent
                final List<Claim> claimsToMigrate = new ArrayList<>();
                for (Claim child : this.children) {
                    GPClaim childClaim = (GPClaim) child;
                    if (this.isBandingAcross(childClaim) || childClaim.isBandingAcross(this)) {
                        return new GPClaimResult(childClaim, ClaimResultType.OVERLAPPING_CLAIM);
                    }
                    if (!childClaim.isInside(this)) {
                        if (this.parent != null) {
                            claimsToMigrate.add(childClaim);
                        } else {
                            childClaim.parent = null;
                            this.children.remove(childClaim);
                            final GPClaimManager claimWorldManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(this.world.getProperties());
                            claimWorldManager.addClaim(childClaim, true);
                        }
                    }
                }
                if (!claimsToMigrate.isEmpty()) {
                    this.parent.migrateClaims(claimsToMigrate);
                }
            }
            return new GPClaimResult(claimsInArea, ClaimResultType.SUCCESS);
        }

        final List<Claim> claimsToMigrate = new ArrayList<>();
        // First check children
        for (Claim child : this.children) {
            final GPClaim childClaim = (GPClaim) child;
            if (this.isBandingAcross(childClaim) || childClaim.isBandingAcross(this)) {
                return new GPClaimResult(childClaim, ClaimResultType.OVERLAPPING_CLAIM);
            }
            if (childClaim.isInside(this)) {
                if (this.type.equals(childClaim.type)) {
                    return new GPClaimResult(childClaim, ClaimResultType.OVERLAPPING_CLAIM);
                }
            } else {
                // child is no longer within parent
                // if resizing, migrate the child claim out
                if (resize) {
                    claimsToMigrate.add(childClaim);
                } else {
                    return new GPClaimResult(childClaim, ClaimResultType.OVERLAPPING_CLAIM);
                }
            }
        }

        if (!claimsToMigrate.isEmpty()) {
            ((GPClaim) this.wildernessClaim).migrateClaims(claimsToMigrate);
        }

        final GPClaimManager claimWorldManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(this.world.getProperties());
        final Set<Long> chunkHashes = this.getChunkHashes(true);

        // Since there is no parent we need to check all claims stored in chunk hashes
        for (Long chunkHash : chunkHashes) {
            Set<Claim> claimsInChunk = claimWorldManager.getInternalChunksToClaimsMap().get(chunkHash);
            if (claimsInChunk == null || claimsInChunk.size() == 0) {
                continue;
            }
            for (Claim chunkClaim : claimsInChunk) {
                final GPClaim gpChunkClaim = (GPClaim) chunkClaim;
                if (gpChunkClaim.equals(this)) {
                    continue;
                }

                // First check if newly resized claim is crossing another
                if (this.isBandingAcross(gpChunkClaim) || gpChunkClaim.isBandingAcross(this)) {
                    return new GPClaimResult(gpChunkClaim, ClaimResultType.OVERLAPPING_CLAIM);
                }
                if (gpChunkClaim.isInside(this)) {
                    if (this.type.equals(gpChunkClaim.type)) {
                        return new GPClaimResult(gpChunkClaim, ClaimResultType.OVERLAPPING_CLAIM);
                    }
                    if (!this.canEnclose(gpChunkClaim)) {
                        return new GPClaimResult(gpChunkClaim, ClaimResultType.OVERLAPPING_CLAIM);
                    }
                    if (!this.isSubdivision()) {
                        claimsInArea.add(gpChunkClaim);
                    }
                }
            }
        }

        return new GPClaimResult(claimsInArea, ClaimResultType.SUCCESS);
    }

    public boolean canEnclose(Claim claim) {
        if (claim.isWilderness()) {
            return false;
        }
        if (this.isSubdivision()) {
            return false;
        }
        if (this.isBasicClaim()) {
            if (!this.isSubdivision()) {
                return false;
            }
            return true;
        }
        if (this.isTown()) {
            if (claim.isAdminClaim()) {
                return false;
            }
            return true;
        }
        if (this.isAdminClaim()) {
            if (claim.isAdminClaim()) {
                return false;
            }
        }
        return true;
    }

    // Checks to see if the passed in claim is a parent of this claim
    @Override
    public boolean isParent(Claim claim) {
        if (this.parent == null) {
            return false;
        }

        GPClaim parent = this.parent;
        while (parent != null) {
            if (parent.getUniqueId().equals(claim.getUniqueId())) {
                return true;
            }
            parent = parent.parent;
        }

        return false;
    }

    @Override
    public ClaimResult resize(int x1, int x2, int y1, int y2, int z1, int z2) {
        int smallx, bigx, smally, bigy, smallz, bigz;

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

        return this.resizeInternal(smallx, smally, smallz, bigx, bigy, bigz);
    }

    public ClaimResult resizeInternal(int smallX, int smallY, int smallZ, int bigX, int bigY, int bigZ) {
        if (this.cuboid) {
            return resizeCuboid(smallX, smallY, smallZ, bigX, bigY, bigZ);
        }

        Location<World> startCorner = null;
        Location<World> endCorner = null;
        GPPlayerData playerData = null;
        final Object root = Sponge.getCauseStackManager().getCurrentCause().root();
        final Player player = root instanceof Player ? (Player) root : null;
        if (player != null) {
            playerData = GriefPreventionPlugin.instance.dataStore.getPlayerData(this.world, player.getUniqueId());
        } else if (!this.isAdminClaim() && this.ownerUniqueId != null) {
            playerData = GriefPreventionPlugin.instance.dataStore.getPlayerData(this.world, this.ownerUniqueId);
        }

        if (playerData == null) {
            startCorner = new Location<World>(this.world, smallX, smallY, smallZ);
            endCorner = new Location<World>(this.world, bigX, bigY, bigZ);
        } else {
            startCorner = playerData.lastShovelLocation;
            endCorner = playerData.endShovelLocation;
        }

        // creative mode claims always go to bedrock
        if (GriefPreventionPlugin.instance.claimModeIsActive(this.world.getProperties(), ClaimsMode.Creative)) {
            smallY = 2;
        }

        // Auto-adjust Y levels for 2D claims
        if (playerData != null) {
            smallY = playerData.getMinClaimLevel();
        }
        if (playerData != null) {
            bigY = playerData.getMaxClaimLevel();
        }
        Location<World> currentLesserCorner = this.getLesserBoundaryCorner();
        Location<World> currentGreaterCorner = this.getGreaterBoundaryCorner();
        Location<World> newLesserCorner = new Location<World>(this.world, smallX, smallY, smallZ);
        Location<World> newGreaterCorner = new Location<World>(this.world, bigX, bigY, bigZ);

        // check player has enough claim blocks
        if ((this.isBasicClaim() || this.isTown()) && this.claimData.requiresClaimBlocks()) {
            final int newCost = BlockUtils.getClaimBlockCost(this.world, newLesserCorner.getBlockPosition(), newGreaterCorner.getBlockPosition(), this.cuboid);
            final int currentCost = BlockUtils.getClaimBlockCost(this.world, currentLesserCorner.getBlockPosition(), currentGreaterCorner.getBlockPosition(), this.cuboid);
            if (newCost > currentCost) {
                final int remainingClaimBlocks = this.ownerPlayerData.getRemainingClaimBlocks() - (newCost - currentCost);
                if (remainingClaimBlocks < 0) {
                    if (player != null) {
                        if (GriefPreventionPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME) {
                            final double claimableChunks = Math.abs(remainingClaimBlocks / 65536.0);
                            final Map<String, ?> params = ImmutableMap.of(
                                    "chunks", Math.round(claimableChunks * 100.0)/100.0,
                                    "blocks", Math.abs(remainingClaimBlocks));
                            GriefPreventionPlugin.sendMessage(player, MessageStorage.CLAIM_SIZE_NEED_BLOCKS_3D, GriefPreventionPlugin.instance.messageData.claimSizeNeedBlocks3d, params);
                        } else {
                            final Map<String, ?> params = ImmutableMap.of(
                                    "blocks", Math.abs(remainingClaimBlocks));
                            GriefPreventionPlugin.sendMessage(player, MessageStorage.CLAIM_SIZE_NEED_BLOCKS_2D, GriefPreventionPlugin.instance.messageData.claimSizeNeedBlocks2d, params);
                        }
                    }
                    playerData.lastShovelLocation = null;
                    playerData.claimResizing = null;
                    this.lesserBoundaryCorner = currentLesserCorner;
                    this.greaterBoundaryCorner = currentGreaterCorner;
                    return new GPClaimResult(ClaimResultType.INSUFFICIENT_CLAIM_BLOCKS);
                }
            }
        }

        this.lesserBoundaryCorner = newLesserCorner;
        this.greaterBoundaryCorner = newGreaterCorner;

        // checkArea refreshes the current chunk hashes so it is important
        // to make a copy before making the call
        final Set<Long> currentChunkHashes = new HashSet<>(this.chunkHashes);

        final ClaimResult result = this.checkArea(true);
        if (!result.successful()) {
            this.lesserBoundaryCorner = currentLesserCorner;
            this.greaterBoundaryCorner = currentGreaterCorner;
            return result;
        }

        GPChangeClaimEvent.Resize event = new GPChangeClaimEvent.Resize(this, startCorner, endCorner, this);
        SpongeImpl.postEvent(event);
        if (event.isCancelled()) {
            this.lesserBoundaryCorner = currentLesserCorner;
            this.greaterBoundaryCorner = currentGreaterCorner;
            return new GPClaimResult(this, ClaimResultType.CLAIM_EVENT_CANCELLED);
        }

        ClaimResult claimResult = checkSizeLimits(player, playerData, newLesserCorner.getBlockPosition(), newGreaterCorner.getBlockPosition());
        if (!claimResult.successful()) {
            this.lesserBoundaryCorner = currentLesserCorner;
            this.greaterBoundaryCorner = currentGreaterCorner;
            return claimResult;
        }

        // This needs to be adjusted before we check for overlaps
        this.lesserBoundaryCorner = newLesserCorner;
        this.greaterBoundaryCorner = newGreaterCorner;
        GPClaimManager claimWorldManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(this.world.getProperties());

        // resize validated, remove invalid chunkHashes
        if (this.parent == null) {
            for (Long chunkHash : currentChunkHashes) {
                Set<Claim> claimsInChunk = claimWorldManager.getInternalChunksToClaimsMap().get(chunkHash);
                if (claimsInChunk != null && claimsInChunk.size() > 0) {
                    claimsInChunk.remove(this);
                }
            }

            final Set<Long> newChunkHashes = this.getChunkHashes(true);
            // add new chunk hashes
            for (Long chunkHash : newChunkHashes) {
                Set<Claim> claimsInChunk = claimWorldManager.getInternalChunksToClaimsMap().get(chunkHash);
                if (claimsInChunk == null) {
                    claimsInChunk = new HashSet<>();
                    claimWorldManager.getInternalChunksToClaimsMap().put(chunkHash, claimsInChunk);
                }

                claimsInChunk.add(this);
            }
        }

        this.claimData.setLesserBoundaryCorner(BlockUtils.positionToString(this.lesserBoundaryCorner));
        this.claimData.setGreaterBoundaryCorner(BlockUtils.positionToString(this.greaterBoundaryCorner));
        this.claimData.setRequiresSave(true);
        this.getClaimStorage().save();

        if (result.getClaims().size() > 1) {
            this.migrateClaims(new ArrayList<>(result.getClaims()));
        }
        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    public ClaimResult resizeCuboid(int smallX, int smallY, int smallZ, int bigX, int bigY, int bigZ) {
        Location<World> startCorner = null;
        Location<World> endCorner = null;
        GPPlayerData playerData = null;
        final Object root = Sponge.getCauseStackManager().getCurrentCause().root();
        final Player player = root instanceof Player ? (Player) root : null;
        if (player != null) {
            playerData = GriefPreventionPlugin.instance.dataStore.getPlayerData(this.world, player.getUniqueId());
        } else if (!this.isAdminClaim() && this.ownerUniqueId != null) {
            playerData = GriefPreventionPlugin.instance.dataStore.getPlayerData(this.world, this.ownerUniqueId);
        }

        if (playerData == null) {
            startCorner = new Location<World>(this.world, smallX, smallY, smallZ);
            endCorner = new Location<World>(this.world, bigX, bigY, bigZ);
        } else {
            startCorner = playerData.lastShovelLocation;
            endCorner = playerData.endShovelLocation;
        }

        // make sure resize doesn't cross paths
        if (smallX >= bigX || smallY >= bigY || smallZ >= bigZ) {
            return new GPClaimResult(this, ClaimResultType.OVERLAPPING_CLAIM);
        }
        if (playerData != null && playerData.shovelMode != ShovelMode.Admin && smallY < playerData.getMinClaimLevel()) {
            final Text message = GriefPreventionPlugin.instance.messageData.claimBelowLevel
                    .apply(ImmutableMap.of(
                    "claim-level", playerData.getMinClaimLevel())).build();
            GriefPreventionPlugin.sendMessage(player, message);
            return new GPClaimResult(ClaimResultType.BELOW_MIN_LEVEL);
        }
        if (playerData != null && playerData.shovelMode != ShovelMode.Admin && bigY > playerData.getMaxClaimLevel()) {
            final Text message = GriefPreventionPlugin.instance.messageData.claimAboveLevel
                    .apply(ImmutableMap.of(
                    "claim-level", playerData.getMaxClaimLevel())).build();
            GriefPreventionPlugin.sendMessage(player, message);
            return new GPClaimResult(ClaimResultType.ABOVE_MAX_LEVEL);
        }
        // check if child extends past parent limits
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

        Location<World> currentLesserCorner = this.lesserBoundaryCorner;
        Location<World> currentGreaterCorner = this.greaterBoundaryCorner;
        Location<World> newLesserCorner = new Location<World>(this.world, smallX, smallY, smallZ);
        Location<World> newGreaterCorner = new Location<World>(this.world, bigX, bigY, bigZ);
        this.lesserBoundaryCorner = newLesserCorner;
        this.greaterBoundaryCorner = newGreaterCorner;

        // checkArea refreshes the current chunk hashes so it is important
        // to make a copy before making the call
        final Set<Long> currentChunkHashes = new HashSet<>(this.chunkHashes);

        final ClaimResult result = this.checkArea(true);
        if (!result.successful()) {
            this.lesserBoundaryCorner = currentLesserCorner;
            this.greaterBoundaryCorner = currentGreaterCorner;
            return result;
        }

        GPChangeClaimEvent.Resize event = new GPChangeClaimEvent.Resize(this, startCorner, endCorner, 
                new GPClaim(newLesserCorner.copy(), newGreaterCorner.copy(), this.type, this.cuboid));
        SpongeImpl.postEvent(event);
        if (event.isCancelled()) {
            this.lesserBoundaryCorner = currentLesserCorner;
            this.greaterBoundaryCorner = currentGreaterCorner;
            return new GPClaimResult(this, ClaimResultType.CLAIM_EVENT_CANCELLED);
        }

        ClaimResult claimResult = checkSizeLimits(player, playerData, newLesserCorner.getBlockPosition(), newGreaterCorner.getBlockPosition());
        if (!claimResult.successful()) {
            this.lesserBoundaryCorner = currentLesserCorner;
            this.greaterBoundaryCorner = currentGreaterCorner;
            return claimResult;
        }

        this.lesserBoundaryCorner = newLesserCorner;
        this.greaterBoundaryCorner = newGreaterCorner;
        // resize validated, remove invalid chunkHashes
        final GPClaimManager claimWorldManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(this.world.getProperties());
        if (this.parent == null) {
            for (Long chunkHash : currentChunkHashes) {
                Set<Claim> claimsInChunk = claimWorldManager.getInternalChunksToClaimsMap().get(chunkHash);
                if (claimsInChunk != null && claimsInChunk.size() > 0) {
                    claimsInChunk.remove(this);
                }
            }

            final Set<Long> newChunkHashes = this.getChunkHashes(true);
            // add new chunk hashes
            for (Long chunkHash : newChunkHashes) {
                Set<Claim> claimsInChunk = claimWorldManager.getInternalChunksToClaimsMap().get(chunkHash);
                if (claimsInChunk == null) {
                    claimsInChunk = new HashSet<>();
                    claimWorldManager.getInternalChunksToClaimsMap().put(chunkHash, claimsInChunk);
                }

                claimsInChunk.add(this);
            }
        }

        this.claimData.setLesserBoundaryCorner(BlockUtils.positionToString(this.lesserBoundaryCorner));
        this.claimData.setGreaterBoundaryCorner(BlockUtils.positionToString(this.greaterBoundaryCorner));
        this.claimData.setRequiresSave(true);
        this.getClaimStorage().save();
        if (result.getClaims().size() > 1) {
            this.migrateClaims(new ArrayList<>(result.getClaims()));
        }
        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    private ClaimResult checkSizeLimits(Player player, GPPlayerData playerData, Vector3i lesserCorner, Vector3i greaterCorner) {
        if (playerData == null) {
            return new GPClaimResult(ClaimResultType.SUCCESS);
        }

        final Subject subject = playerData.getPlayerSubject();
        final int minClaimX = GPOptionHandler.getClaimOptionDouble(subject, this, GPOptions.Type.MIN_CLAIM_SIZE_X, playerData).intValue();
        final int minClaimY = GPOptionHandler.getClaimOptionDouble(subject, this, GPOptions.Type.MIN_CLAIM_SIZE_Y, playerData).intValue();
        final int minClaimZ = GPOptionHandler.getClaimOptionDouble(subject, this, GPOptions.Type.MIN_CLAIM_SIZE_Z, playerData).intValue();
        final int maxClaimX = GPOptionHandler.getClaimOptionDouble(subject, this, GPOptions.Type.MAX_CLAIM_SIZE_X, playerData).intValue();
        final int maxClaimY = GPOptionHandler.getClaimOptionDouble(subject, this, GPOptions.Type.MAX_CLAIM_SIZE_Y, playerData).intValue();
        final int maxClaimZ = GPOptionHandler.getClaimOptionDouble(subject, this, GPOptions.Type.MAX_CLAIM_SIZE_Z, playerData).intValue();

        // Handle single block selection
        if ((this.isCuboid() && greaterCorner.equals(lesserCorner)) || (!this.isCuboid() && greaterCorner.getX() == lesserCorner.getX() && greaterCorner.getZ() == lesserCorner.getZ())) {
            if (playerData.claimResizing != null) {
                final Text message = GriefPreventionPlugin.instance.messageData.claimResizeSameLocation.toText();
                GriefPreventionPlugin.sendMessage(player, message);
                playerData.lastShovelLocation = null;
                playerData.claimResizing = null;
                // TODO: Add new result type for this
                return new GPClaimResult(ClaimResultType.BELOW_MIN_SIZE_X, message);
            }
            if (playerData.claimSubdividing == null) {
                final Text message = GriefPreventionPlugin.instance.messageData.claimCreateOnlySubdivision.toText();
                GriefPreventionPlugin.sendMessage(player, message);
                playerData.lastShovelLocation = null;
                // TODO: Add new result type for this
                return new GPClaimResult(ClaimResultType.BELOW_MIN_SIZE_X, message);
            }
        }
        Text message = null;
        if (maxClaimX > 0) {
            int size = Math.abs(greaterCorner.getX() - lesserCorner.getX()) + 1;
            if (size > maxClaimX) {
                if (player != null) {
                    if (this.isCuboid()) {
                        message = GriefPreventionPlugin.instance.messageData.claimSizeMaxX
                                .apply(ImmutableMap.of(
                                        "size", size,
                                        "max-size", maxClaimX,
                                        "min-area", minClaimX + "x" + minClaimY + "x" + minClaimZ,
                                        "max-area", maxClaimX + "x" + maxClaimY + "x" + minClaimZ)).build();
                    } else {
                        message = GriefPreventionPlugin.instance.messageData.claimSizeMaxX
                                .apply(ImmutableMap.of(
                                        "size", size,
                                        "max-size", maxClaimX,
                                        "min-area", minClaimX + "x" + minClaimZ,
                                        "max-area", maxClaimX + "x" + minClaimZ)).build();
                    }
                    GriefPreventionPlugin.sendMessage(player, message);
                }
                return new GPClaimResult(ClaimResultType.EXCEEDS_MAX_SIZE_X, message);
            }
        }
        if (this.cuboid && maxClaimY > 0) {
            int size = Math.abs(greaterCorner.getY() - lesserCorner.getY()) + 1;
            if (size > maxClaimY) {
                if (player != null) {
                    if (this.isCuboid()) {
                        message = GriefPreventionPlugin.instance.messageData.claimSizeMaxY
                                .apply(ImmutableMap.of(
                                        "size", size,
                                        "max-size", maxClaimY,
                                        "min-area", minClaimX + "x" + minClaimY + "x" + minClaimZ,
                                        "max-area", maxClaimX + "x" + maxClaimY + "x" + minClaimZ)).build();
                    } else {
                        message = GriefPreventionPlugin.instance.messageData.claimSizeMaxY
                                .apply(ImmutableMap.of(
                                        "size", size,
                                        "max-size", maxClaimY,
                                        "min-area", minClaimX + "x" + minClaimZ,
                                        "max-area", maxClaimX + "x" + minClaimZ)).build();
                    }
                    GriefPreventionPlugin.sendMessage(player, message);
                }
                return new GPClaimResult(ClaimResultType.EXCEEDS_MAX_SIZE_Y, message);
            }
        }
        if (maxClaimZ > 0) {
            int size = Math.abs(greaterCorner.getZ() - lesserCorner.getZ()) + 1;
            if (size > maxClaimZ) {
                if (player != null) {
                    if (this.isCuboid()) {
                        message = GriefPreventionPlugin.instance.messageData.claimSizeMaxZ
                            .apply(ImmutableMap.of(
                                    "size", size,
                                    "max-size", maxClaimZ,
                                    "min-area", minClaimX + "x" + minClaimY + "x" + minClaimZ,
                                    "max-area", maxClaimX + "x" + maxClaimY + "x" + minClaimZ)).build();
                    } else {
                        message = GriefPreventionPlugin.instance.messageData.claimSizeMaxZ
                            .apply(ImmutableMap.of(
                                    "size", size,
                                    "max-size", maxClaimZ,
                                    "min-area", minClaimX + "x" + minClaimZ,
                                    "max-area", maxClaimX + "x" + minClaimZ)).build();
                    }
                    GriefPreventionPlugin.sendMessage(player, message);
                }
                return new GPClaimResult(ClaimResultType.EXCEEDS_MAX_SIZE_Z, message);
            }
        }
        if (minClaimX > 0) {
            int size = Math.abs(greaterCorner.getX() - lesserCorner.getX()) + 1;
            if (size < minClaimX) {
                if (player != null) {
                    if (this.isCuboid()) {
                        message = GriefPreventionPlugin.instance.messageData.claimSizeMinX
                                .apply(ImmutableMap.of(
                                        "size", size,
                                        "min-size", minClaimX,
                                        "min-area", minClaimX + "x" + minClaimY + "x" + minClaimZ,
                                        "max-area", maxClaimX + "x" + maxClaimY + "x" + maxClaimZ)).build();
                    } else {
                        message = GriefPreventionPlugin.instance.messageData.claimSizeMinX
                                .apply(ImmutableMap.of(
                                        "size", size,
                                        "min-size", minClaimX,
                                        "min-area", minClaimX + "x" + minClaimZ,
                                        "max-area", maxClaimX + "x" + maxClaimZ)).build();
                    }
                    GriefPreventionPlugin.sendMessage(player, message);
                }
                return new GPClaimResult(ClaimResultType.BELOW_MIN_SIZE_X, message);
            }
        }
        if (this.cuboid && minClaimY > 0) {
            int size = Math.abs(greaterCorner.getY() - lesserCorner.getY()) + 1;
            if (size < minClaimY) {
                if (player != null) {
                    if (this.isCuboid()) {
                        message = GriefPreventionPlugin.instance.messageData.claimSizeMinY
                                .apply(ImmutableMap.of(
                                        "size", size,
                                        "min-size", minClaimY,
                                        "min-area", minClaimX + "x" + minClaimY + "x" + minClaimZ,
                                        "max-area", maxClaimX + "x" + maxClaimY + "x" + maxClaimZ)).build();
                    } else {
                        message = GriefPreventionPlugin.instance.messageData.claimSizeMinY
                                .apply(ImmutableMap.of(
                                        "size", size,
                                        "min-size", minClaimY,
                                        "min-area", minClaimX + "x" + minClaimZ,
                                        "max-area", maxClaimX + "x" + maxClaimZ)).build();
                    }
                    GriefPreventionPlugin.sendMessage(player, message);
                }
                return new GPClaimResult(ClaimResultType.BELOW_MIN_SIZE_Y, message);
            }
        }
        if (minClaimZ > 0) {
            int size = Math.abs(greaterCorner.getZ() - lesserCorner.getZ()) + 1;
            if (size < minClaimZ) {
                if (player != null) {
                    if (this.isCuboid()) {
                        message = GriefPreventionPlugin.instance.messageData.claimSizeMinZ
                                .apply(ImmutableMap.of(
                                        "size", size,
                                        "min-size", minClaimZ,
                                        "min-area", minClaimX + "x" + minClaimY + "x" + minClaimZ,
                                        "max-area", maxClaimX + "x" + maxClaimY + "x" + maxClaimZ)).build();
                    } else {
                        message = GriefPreventionPlugin.instance.messageData.claimSizeMinZ
                                .apply(ImmutableMap.of(
                                        "size", size,
                                        "min-size", minClaimZ,
                                        "min-area", minClaimX + "x" + minClaimZ,
                                        "max-area", maxClaimX + "x" + maxClaimZ)).build();
                    }
                    GriefPreventionPlugin.sendMessage(player, message);
                }
                return new GPClaimResult(ClaimResultType.BELOW_MIN_SIZE_Z, message);
            }
        }

        return new GPClaimResult(ClaimResultType.SUCCESS);
    }

    public void unload() {
        // clear any references
        this.world = null;
        if (this.ownerPlayerData != null) {
            this.ownerPlayerData.getInternalClaims().remove(this);
        }
    }

    @Override
    public Claim getWilderness() {
        return this.wildernessClaim;
    }

    @Override
    public ClaimManager getClaimManager() {
        return (ClaimManager) this.worldClaimManager;
    }

    @Override
    public Context getContext() {
        return this.context;
    }
 
    public Context getInheritContext() {
        if (this.parent == null || !this.getData().doesInheritParent()) {
            return this.context;
        }

        return this.parent.getInheritContext();
    }

    public boolean hasAdminParent() {
        if (this.parent == null || this.isAdminClaim()) {
            return false;
        }

        if (this.parent.isAdminClaim()) {
            return true;
        }

        return this.parent.hasAdminParent();
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
    public List<Entity> getEntities() {
        Collection<Entity> worldEntityList = Sponge.getServer().getWorld(this.world.getUniqueId()).get().getEntities();
        List<Entity> entityList = new ArrayList<>();
        for (Entity entity : worldEntityList) {
            if (!((net.minecraft.entity.Entity) entity).isDead && this.contains(entity.getLocation())) {
                entityList.add(entity);
            }
        }

        return entityList;
    }

    @Override
    public List<Player> getPlayers() {
        Collection<Player> worldPlayerList = Sponge.getServer().getWorld(this.world.getUniqueId()).get().getPlayers();
        List<Player> playerList = new ArrayList<>();
        for (Player player : worldPlayerList) {
            if (!((net.minecraft.entity.Entity) player).isDead && this.contains(player.getLocation())) {
                playerList.add(player);
            }
        }

        return playerList;
    }

    @Override
    public List<Claim> getChildren(boolean recursive) {
        if (recursive) {
            List<Claim> claimList = new ArrayList<>(this.children);
            List<Claim> subChildren = new ArrayList<>();
            for (Claim child : claimList) {
                GPClaim childClaim = (GPClaim) child;
                if (!childClaim.children.isEmpty()) {
                    subChildren.addAll(childClaim.getChildren(true));
                }
            }
            claimList.addAll(subChildren);
            return claimList;
        }
        return ImmutableList.copyOf(this.children);
    }

    @Override
    public List<Claim> getParents(boolean recursive) {
        List<Claim> parents = new ArrayList<>();
        GPClaim currentClaim = this;
        while (currentClaim.parent != null) {
            parents.add(currentClaim.parent);
            currentClaim = currentClaim.parent;
        }

        // Index 0 is highest parent while last index represents direct
        Collections.reverse(parents);
        return ImmutableList.copyOf(parents);
    }

    public List<Claim> getInheritedParents() {
        List<Claim> parents = new ArrayList<>();
        GPClaim currentClaim = this;
        while (currentClaim.parent != null && currentClaim.getData().doesInheritParent()) {
            parents.add(currentClaim.parent);
            currentClaim = currentClaim.parent;
        }

        // Index 0 is highest parent while last index represents direct
        Collections.reverse(parents);
        return parents;
    }

    @Override
    public ClaimResult deleteChild(Claim child) {
        boolean found = false;
        for (Claim childClaim : this.children) {
            if (childClaim.getUniqueId().equals(child.getUniqueId())) {
                found = true;
            }
        }

        if (!found) {
            return new GPClaimResult(ClaimResultType.CLAIM_NOT_FOUND);
        }

        final GPClaimManager claimManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(this.world.getProperties());
        return claimManager.deleteClaim(child, true);
    }

    @Override
    public ClaimResult deleteChildren() {
        return this.deleteChildren(null);
    }

    @Override
    public ClaimResult deleteChildren(ClaimType claimType) {
        List<Claim> claimList = new ArrayList<>();
        for (Claim child : this.children) {
            if (claimType == null || child.getType() == claimType) {
                claimList.add(child);
            }
        }

        if (claimList.isEmpty()) {
            return new GPClaimResult(ClaimResultType.CLAIM_NOT_FOUND);
        }

        GPDeleteClaimEvent event = new GPDeleteClaimEvent(claimList);
        SpongeImpl.postEvent(event);
        if (event.isCancelled()) {
            return new GPClaimResult(claimList, ClaimResultType.CLAIM_EVENT_CANCELLED);
        }

        final GPClaimManager claimManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(this.world.getProperties());
        for (Claim child : claimList) {
            claimManager.deleteClaimInternal(child, true);
        }

        return new GPClaimResult(event.getClaims(), ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult changeType(ClaimType type, Optional<UUID> ownerUniqueId) {
        return changeType(type, ownerUniqueId, null);
    }

    public ClaimResult changeType(ClaimType type, Optional<UUID> ownerUniqueId, CommandSource src) {
        if (type == this.type) {
            return new GPClaimResult(ClaimResultType.SUCCESS);
        }

        GPChangeClaimEvent.Type event = new GPChangeClaimEvent.Type(this, type);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            return new GPClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        final GPClaimManager claimWorldManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(this.world.getProperties());
        final GPPlayerData sourcePlayerData = src != null && src instanceof Player ? claimWorldManager.getOrCreatePlayerData(((Player) src).getUniqueId()) : null;
        UUID newOwnerUUID = ownerUniqueId.orElse(this.ownerUniqueId);
        final ClaimResult result = this.validateClaimType(type, newOwnerUUID, sourcePlayerData);
        if (!result.successful()) {
            return result;
        }

        if (type == ClaimType.ADMIN) {
            newOwnerUUID = GriefPreventionPlugin.ADMIN_USER_UUID;
        }

        final String fileName = this.getClaimStorage().filePath.getFileName().toString();
        final Path newPath = this.getClaimStorage().folderPath.getParent().resolve(type.name().toLowerCase()).resolve(fileName);
        try {
            Files.createDirectories(newPath.getParent());
            Files.move(this.getClaimStorage().filePath, newPath);
            if (type == ClaimType.TOWN) {
                this.setClaimStorage(new TownStorageData(newPath, this.getWorldUniqueId(), newOwnerUUID, this.cuboid));
            } else {
                this.setClaimStorage(new ClaimStorageData(newPath, this.getWorldUniqueId(), (ClaimDataConfig) this.getInternalClaimData()));
            }
            this.claimData = this.claimStorage.getConfig();
            this.getClaimStorage().save();
        } catch (IOException e) {
            e.printStackTrace();
            return new GPClaimResult(ClaimResultType.CLAIM_NOT_FOUND, Text.of(e.getMessage()));
        }

        // If switched to admin or new owner, remove from player claim list
        if (type == ClaimType.ADMIN || !this.ownerUniqueId.equals(newOwnerUUID)) {
            final List<Claim> currentPlayerClaims = claimWorldManager.getInternalPlayerClaims(this.ownerUniqueId);
            if (currentPlayerClaims != null) {
                currentPlayerClaims.remove(this);
            }
        }
        if (type != ClaimType.ADMIN) {
            final List<Claim> newPlayerClaims = claimWorldManager.getInternalPlayerClaims(newOwnerUUID);
            if (newPlayerClaims != null && !newPlayerClaims.contains(this)) {
                newPlayerClaims.add(this);
            }
        }

        if (!this.isAdminClaim() && this.ownerPlayerData != null) {
            final Player player = Sponge.getServer().getPlayer(this.ownerUniqueId).orElse(null);
            if (player != null) {
                this.ownerPlayerData.revertActiveVisual(player);
            }
        }

        // revert visuals for all players watching this claim
        List<UUID> playersWatching = new ArrayList<>(this.playersWatching);
        for (UUID playerUniqueId : playersWatching) {
            final Player spongePlayer = Sponge.getServer().getPlayer(playerUniqueId).orElse(null);
            final GPPlayerData playerData = claimWorldManager.getOrCreatePlayerData(playerUniqueId);
            if (spongePlayer != null) {
                playerData.revertActiveVisual(spongePlayer);
            }
        }

        if (!newOwnerUUID.equals(GriefPreventionPlugin.ADMIN_USER_UUID)) {
            this.setOwnerUniqueId(newOwnerUUID);
        }
        this.setType(type);
        this.visualization = null;
        this.getInternalClaimData().setRequiresSave(true);
        this.getClaimStorage().save();
        return new GPClaimResult(ClaimResultType.SUCCESS);
    }

    public ClaimResult validateClaimType(ClaimType type, UUID newOwnerUUID, GPPlayerData playerData) {
        boolean isAdmin = false;
        if (playerData != null && (playerData.canManageAdminClaims || playerData.canIgnoreClaim(this))) {
            isAdmin = true;
        }

        switch (type) {
            case ADMIN : 
                if (!isAdmin) {
                    final Text message = Text.of(TextColors.RED, "You do not have administrative permissions to change type to ADMIN.");
                    return new GPClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, message);
                }
                if (this.parent != null && this.parent.isAdminClaim()) {
                    final Text message = Text.of(TextColors.RED, "Admin claims cannot have direct admin children claims.");
                    return new GPClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, message);
                }
                break;
            case BASIC :
                if (this.isAdminClaim() && newOwnerUUID == null) {
                    return new GPClaimResult(ClaimResultType.REQUIRES_OWNER, Text.of(TextColors.RED, "Could not convert admin claim to basic. Owner is required."));
                }
                if (this.parent != null && this.parent.isBasicClaim()) {
                    final Text message = Text.of(TextColors.RED, "Basic claims cannot have direct basic children claims.");
                    return new GPClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, message);
                }
                for (Claim child : this.children) {
                    if (!child.isSubdivision()) {
                        final Text message = Text.of(TextColors.RED, "Basic claims can only contain subdivisions.");
                        return new GPClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, message);
                    }
                }
                break;
            case SUBDIVISION :
                if (!this.children.isEmpty()) {
                    final Text message = Text.of(TextColors.RED, "Subdivisions cannot contain children claims.");
                    return new GPClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, message);
                }
                if (this.parent == null) {
                    final Text message = Text.of(TextColors.RED, "Subdivisions cannot be created in the wilderness.");
                    return new GPClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, message);
                }
                if (this.isAdminClaim() && newOwnerUUID == null) {
                    return new GPClaimResult(ClaimResultType.REQUIRES_OWNER, Text.of(TextColors.RED, "Could not convert admin claim to subdivision. Owner is required."));
                }
                break;
            case TOWN :
                if (this.parent != null && this.parent.isTown()) {
                    final Text message = Text.of(TextColors.RED, "Towns cannot contain children towns.");
                    return new GPClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, message);
                }
                break;
            case WILDERNESS :
                final Text message = Text.of(TextColors.RED, "You cannot change a claim to WILDERNESS.");
                return new GPClaimResult(ClaimResultType.WRONG_CLAIM_TYPE, message);
        }

        return new GPClaimResult(ClaimResultType.SUCCESS);
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
    public List<UUID> getUserTrusts() {
        List<UUID> trustList = new ArrayList<>();
        trustList.addAll(this.claimData.getAccessors());
        trustList.addAll(this.claimData.getContainers());
        trustList.addAll(this.claimData.getBuilders());
        trustList.addAll(this.claimData.getManagers());
        return ImmutableList.copyOf(trustList);
    }

    @Override
    public List<UUID> getUserTrusts(TrustType type) {
        return ImmutableList.copyOf(this.getUserTrustList(type));
    }

    @Override
    public boolean isUserTrusted(User user, TrustType type) {
        return isUserTrusted(user, type, null);
    }

    @Override
    public boolean isUserTrusted(UUID uuid, TrustType type) {
        final User user = GriefPreventionPlugin.getOrCreateUser(uuid);
        return isUserTrusted(user, type, null);
    }

    public boolean isUserTrusted(User user, TrustType type, Set<Context> contexts) {
        if (user == null) {
            return false;
        }

        final GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(world, user.getUniqueId());
        if (!playerData.canIgnoreClaim(this) && this.getInternalClaimData().isExpired()) {
            return false;
        }
        if (!playerData.executingClaimDebug && !playerData.debugClaimPermissions) {
            if (user.getUniqueId().equals(this.getOwnerUniqueId())) {
                return true;
            }
            if (this.isAdminClaim() && playerData.canManageAdminClaims) {
                return true;
            }
            if (this.isWilderness() && playerData.canManageWilderness) {
                return true;
            }
            if (playerData.canIgnoreClaim(this)) {
                return true;
            }
        }

        if (type == null) {
            return true;
        }
        if (this.isPublicTrusted(type)) {
            return true;
        }

        if (type == TrustType.ACCESSOR) {
            if (this.claimData.getAccessors().contains(user.getUniqueId())) {
                return true;
            }
            if (this.claimData.getBuilders().contains(user.getUniqueId())) {
                return true;
            }
            if (this.claimData.getContainers().contains(user.getUniqueId())) {
                return true;
            }
            if (this.claimData.getManagers().contains(user.getUniqueId())) {
                return true;
            }
        } else if (type == TrustType.BUILDER) {
            if (this.claimData.getBuilders().contains(user.getUniqueId())) {
                return true;
            }
            if (this.claimData.getManagers().contains(user.getUniqueId())) {
                return true;
            }
        } else if (type == TrustType.CONTAINER) {
            if (this.claimData.getContainers().contains(user.getUniqueId())) {
                return true;
            }
            if (this.claimData.getBuilders().contains(user.getUniqueId())) {
                return true;
            }
            if (this.claimData.getManagers().contains(user.getUniqueId())) {
                return true;
            }
        } else if (type == TrustType.MANAGER) {
            if (this.claimData.getManagers().contains(user.getUniqueId())) {
                return true;
            }
        }

        if (contexts == null) {
            contexts = new HashSet<>();
            contexts.add(this.getContext());
        }

        if (user.hasPermission(contexts, GPPermissions.getTrustPermission(type))) {
            return true;
        }

        // Only check parent if this claim inherits
        if (this.parent != null && this.getData().doesInheritParent()) {
            return this.parent.isUserTrusted(user, type, contexts);
        }

        return false;
    }

    private boolean isPublicTrusted(TrustType type) {
        if (type == TrustType.ACCESSOR) {
            if (this.claimData.getAccessors().contains(GriefPreventionPlugin.PUBLIC_UUID)) {
                return true;
            }
            if (this.claimData.getBuilders().contains(GriefPreventionPlugin.PUBLIC_UUID)) {
                return true;
            }
            if (this.claimData.getContainers().contains(GriefPreventionPlugin.PUBLIC_UUID)) {
                return true;
            }
            if (this.claimData.getManagers().contains(GriefPreventionPlugin.PUBLIC_UUID)) {
                return true;
            }
        } else if (type == TrustType.BUILDER) {
            if (this.claimData.getBuilders().contains(GriefPreventionPlugin.PUBLIC_UUID)) {
                return true;
            }
            if (this.claimData.getManagers().contains(GriefPreventionPlugin.PUBLIC_UUID)) {
                return true;
            }
        } else if (type == TrustType.CONTAINER) {
            if (this.claimData.getContainers().contains(GriefPreventionPlugin.PUBLIC_UUID)) {
                return true;
            }
            if (this.claimData.getBuilders().contains(GriefPreventionPlugin.PUBLIC_UUID)) {
                return true;
            }
            if (this.claimData.getManagers().contains(GriefPreventionPlugin.PUBLIC_UUID)) {
                return true;
            }
        } else if (type == TrustType.MANAGER) {
            if (this.claimData.getManagers().contains(GriefPreventionPlugin.PUBLIC_UUID)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isGroupTrusted(String group, TrustType type) {
        if (group == null) {
            return false;
        }

        if (!PermissionUtils.hasGroupSubject(group)) {
            return false;
        }

        final Subject subj = PermissionUtils.getGroupSubject(group);
        Set<Context> contexts = new HashSet<>();
        contexts.add(this.getContext());

        return subj.hasPermission(contexts, GPPermissions.getTrustPermission(type));
    }

    @Override
    public ClaimResult addUserTrust(UUID uuid, TrustType type) {
        GPUserTrustClaimEvent.Add event = new GPUserTrustClaimEvent.Add(this, ImmutableList.of(uuid), type);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            return new GPClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        List<UUID> userList = this.getUserTrustList(type);
        if (!userList.contains(uuid)) {
            userList.add(uuid);
        }

        this.claimData.setRequiresSave(true);
        this.claimData.save();
        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult addUserTrusts(List<UUID> uuids, TrustType type) {
        GPUserTrustClaimEvent.Add event = new GPUserTrustClaimEvent.Add(this, uuids, type);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            return new GPClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        for (UUID uuid : uuids) {
            List<UUID> userList = this.getUserTrustList(type);
            if (!userList.contains(uuid)) {
                userList.add(uuid);
            }
        }

        this.claimData.setRequiresSave(true);
        this.claimData.save();
        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult removeUserTrust(UUID uuid, TrustType type) {
        GPUserTrustClaimEvent.Remove event = new GPUserTrustClaimEvent.Remove(this, ImmutableList.of(uuid), type);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            return new GPClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        if (type == TrustType.NONE) {
            final ClaimResult result = this.removeAllTrustsFromUser(uuid);
            this.claimData.setRequiresSave(true);
            this.claimData.save();
            return result;
        }

        this.getUserTrustList(type).remove(uuid);
        this.claimData.setRequiresSave(true);
        this.claimData.save();
        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult removeUserTrusts(List<UUID> uuids, TrustType type) {
        GPUserTrustClaimEvent.Remove event = new GPUserTrustClaimEvent.Remove(this, uuids, type);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            return new GPClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        if (type == TrustType.NONE) {
            for (UUID uuid : uuids) {
                this.removeAllTrustsFromUser(uuid);
            }

            this.claimData.setRequiresSave(true);
            this.claimData.save();
            return new GPClaimResult(this, ClaimResultType.SUCCESS);
        }

        List<UUID> userList = this.getUserTrustList(type);
        for (UUID uuid : uuids) {
            if (userList.contains(uuid)) {
                userList.remove(uuid);
            }
        }

        this.claimData.setRequiresSave(true);
        this.claimData.save();
        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult addGroupTrust(String group, TrustType type) {
        GPGroupTrustClaimEvent.Add event = new GPGroupTrustClaimEvent.Add(this, ImmutableList.of(group), type);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            return new GPClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        List<String> groupList = this.getGroupTrustList(type);
        if (!groupList.contains(group)) {
            groupList.add(group);
        }

        this.claimData.setRequiresSave(true);
        this.claimData.save();
        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult addGroupTrusts(List<String> groups, TrustType type) {
        GPGroupTrustClaimEvent.Add event = new GPGroupTrustClaimEvent.Add(this, groups, type);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            return new GPClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        for (String group : groups) {
            List<String> groupList = this.getGroupTrustList(type);
            if (!groupList.contains(group)) {
                groupList.add(group);
            }
        }

        this.claimData.setRequiresSave(true);
        this.claimData.save();
        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult removeGroupTrust(String group, TrustType type) {
        GPGroupTrustClaimEvent.Remove event = new GPGroupTrustClaimEvent.Remove(this, ImmutableList.of(group), type);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            return new GPClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        if (type == TrustType.NONE) {
            final ClaimResult result = this.removeAllTrustsFromGroup(group);
            this.claimData.setRequiresSave(true);
            this.claimData.save();
            return result;
        }

        this.getGroupTrustList(type).remove(group);
        this.claimData.setRequiresSave(true);
        this.claimData.save();
        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult removeGroupTrusts(List<String> groups, TrustType type) {
        GPGroupTrustClaimEvent.Remove event = new GPGroupTrustClaimEvent.Remove(this, groups, type);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            return new GPClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        if (type == TrustType.NONE) {
            for (String group : groups) {
                this.removeAllTrustsFromGroup(group);
            }

            this.claimData.setRequiresSave(true);
            this.claimData.save();
            return new GPClaimResult(this, ClaimResultType.SUCCESS);
        }

        List<String> groupList = this.getGroupTrustList(type);
        for (String group : groups) {
            if (groupList.contains(group)) {
                groupList.remove(group);
            }
        }

        this.claimData.setRequiresSave(true);
        this.claimData.save();
        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult removeAllTrusts() {
        List<UUID> userTrustList = this.getUserTrusts();
        GPUserTrustClaimEvent.Remove userEvent = new GPUserTrustClaimEvent.Remove(this, userTrustList, TrustType.NONE);
        Sponge.getEventManager().post(userEvent);
        if (userEvent.isCancelled()) {
            return new GPClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, userEvent.getMessage().orElse(null));
        }

        List<String> groupTrustList = this.getGroupTrusts();
        GPGroupTrustClaimEvent.Remove event = new GPGroupTrustClaimEvent.Remove(this, groupTrustList, TrustType.NONE);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            return new GPClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        for (TrustType type : TrustType.values()) {
            this.getUserTrustList(type).clear();
        }

        for (TrustType type : TrustType.values()) {
            this.getGroupTrustList(type).clear();
        }

        this.claimData.setRequiresSave(true);
        this.claimData.save();
        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult removeAllUserTrusts() {
        List<UUID> trustList = this.getUserTrusts();
        GPUserTrustClaimEvent.Remove event = new GPUserTrustClaimEvent.Remove(this, trustList, TrustType.NONE);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            return new GPClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        for (TrustType type : TrustType.values()) {
            this.getUserTrustList(type).clear();
        }

        this.claimData.setRequiresSave(true);
        this.claimData.save();
        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    @Override
    public ClaimResult removeAllGroupTrusts() {
        List<String> trustList = this.getGroupTrusts();
        GPGroupTrustClaimEvent.Remove event = new GPGroupTrustClaimEvent.Remove(this, trustList, TrustType.NONE);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            return new GPClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        for (TrustType type : TrustType.values()) {
            this.getGroupTrustList(type).clear();
        }

        this.claimData.setRequiresSave(true);
        this.claimData.save();
        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    public ClaimResult removeAllTrustsFromUser(UUID userUniqueId) {
        for (TrustType type : TrustType.values()) {
            this.getUserTrustList(type).remove(userUniqueId);
        }

        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    public ClaimResult removeAllTrustsFromGroup(String group) {
        for (TrustType type : TrustType.values()) {
            this.getGroupTrustList(type).remove(group);
        }

        return new GPClaimResult(this, ClaimResultType.SUCCESS);
    }

    public List<UUID> getUserTrustList(TrustType type) {
        if (type == TrustType.NONE) {
            return new ArrayList<>();
        }
        if (type == TrustType.ACCESSOR) {
            return this.claimData.getAccessors();
        }
        if (type == TrustType.CONTAINER) {
            return this.claimData.getContainers();
        }
        if (type == TrustType.BUILDER) {
            return this.claimData.getBuilders();
        }
        return this.claimData.getManagers();
    }

    public List<UUID> getParentUserTrustList(TrustType type) {
        List<UUID> userList = new ArrayList<>();
        for (Claim claim : this.getInheritedParents()) {
            GPClaim parentClaim = (GPClaim) claim;
            userList.addAll(parentClaim.getUserTrusts(type));
        }
        return userList;
    }

    public List<String> getParentGroupTrustList(TrustType type) {
        List<String> trustList = new ArrayList<>();
        for (Claim claim : this.getInheritedParents()) {
            GPClaim parentClaim = (GPClaim) claim;
            trustList.addAll(parentClaim.getGroupTrusts(type));
        }
        return trustList;
    }

    public List<UUID> getUserTrustList(TrustType type, boolean includeParents) {
        List<UUID> trustList = new ArrayList<>();
        if (type == TrustType.ACCESSOR) {
            trustList.addAll(this.claimData.getAccessors());
        } else if (type == TrustType.CONTAINER) {
            trustList.addAll(this.claimData.getContainers());
        } else if (type == TrustType.BUILDER) {
            trustList.addAll(this.claimData.getBuilders());
        } else {
            trustList.addAll(this.claimData.getManagers());
        }

        if (includeParents) {
            List<UUID> parentList = getParentUserTrustList(type);
            for (UUID uuid : parentList) {
                if (!trustList.contains(uuid)) {
                    trustList.add(uuid);
                }
            }
        }

        return trustList;
    }

    public List<String> getGroupTrustList(TrustType type) {
        return this.getGroupTrustList(type, false);
    }

    public List<String> getGroupTrustList(TrustType type, boolean includeParents) {
        List<String> trustList = new ArrayList<>();
        if (type == TrustType.ACCESSOR) {
            trustList.addAll(this.claimData.getAccessorGroups());
        } else if (type == TrustType.CONTAINER) {
            trustList.addAll(this.claimData.getContainerGroups());
        } else if (type == TrustType.BUILDER) {
            trustList.addAll(this.claimData.getBuilderGroups());
        } else {
            trustList.addAll(this.claimData.getManagerGroups());
        }

        if (includeParents) {
            List<String> parentList = getParentGroupTrustList(type);
            for (String groupId : parentList) {
                if (!trustList.contains(groupId)) {
                    trustList.add(groupId);
                }
            }
        }

        return trustList;
    }

    @Override
    public List<String> getGroupTrusts() {
        List<String> groups = new ArrayList<>();
        groups.addAll(this.getInternalClaimData().getAccessorGroups());
        groups.addAll(this.getInternalClaimData().getBuilderGroups());
        groups.addAll(this.getInternalClaimData().getContainerGroups());
        groups.addAll(this.getInternalClaimData().getManagerGroups());
        return ImmutableList.copyOf(groups);
    }

    @Override
    public List<String> getGroupTrusts(TrustType type) {
        return ImmutableList.copyOf(this.getGroupTrustList(type));
    }

    @Override
    public Optional<Account> getEconomyAccount() {
        if (this.isAdminClaim() || this.isSubdivision() || !GriefPreventionPlugin.getGlobalConfig().getConfig().claim.bankTaxSystem) {
            return Optional.empty();
        }

        if (this.economyAccount != null) {
            return Optional.of(this.economyAccount);
        }
        EconomyService economyService = GriefPreventionPlugin.instance.economyService.orElse(null);
        if (economyService != null) {
            this.economyAccount = economyService.getOrCreateAccount(this.claimStorage.filePath.getFileName().toString()).orElse(null);
            return Optional.ofNullable(this.economyAccount);
        }
        return Optional.empty();
    }

    public static class ClaimBuilder implements Builder {

        private UUID ownerUniqueId;
        private ClaimType type = ClaimType.BASIC;
        private boolean cuboid = false;
        private boolean requiresClaimBlocks = true;
        private boolean denyMessages = true;
        private boolean expire = true;
        private boolean resizable = true;
        private boolean inherit = true;
        private boolean overrides = true;
        private boolean createLimitRestrictions = true;
        private boolean levelRestrictions = true;
        private boolean sizeRestrictions = true;
        private World world;
        private Vector3i point1;
        private Vector3i point2;
        private Vector3i spawnPos;
        private Text greeting;
        private Text farewell;
        private Claim parent;

        public ClaimBuilder() {
            
        }

        @Override
        public Builder bounds(Vector3i point1, Vector3i point2) {
            this.point1 = point1;
            this.point2 = point2;
            return this;
        }

        @Override
        public Builder cuboid(boolean cuboid) {
            this.cuboid = cuboid;
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
        public Builder createLimitRestrictions(boolean checkCreateLimit) {
            this.createLimitRestrictions = checkCreateLimit;
            return this;
        }

        @Override
        public Builder levelRestrictions(boolean checkLevel) {
            this.levelRestrictions = checkLevel;
            return this;
        }

        @Override
        public Builder sizeRestrictions(boolean checkSize) {
            this.sizeRestrictions = checkSize;
            return this;
        }

        @Override
        public Builder requireClaimBlocks(boolean requiresClaimBlocks) {
            this.requiresClaimBlocks = requiresClaimBlocks;
            return this;
        }

        @Override
        public Builder denyMessages(boolean allowDenyMessages) {
            this.denyMessages = allowDenyMessages;
            return this;
        }

        @Override
        public Builder expire(boolean allowExpire) {
            this.expire = allowExpire;
            return this;
        }

        @Override
        public Builder inherit(boolean inherit) {
            this.inherit = inherit;
            return this;
        }

        @Override
        public Builder resizable(boolean allowResize) {
            this.resizable = allowResize;
            return this;
        }

        @Override
        public Builder overrides(boolean allowOverrides) {
            this.overrides = allowOverrides;
            return this;
        }

        @Override
        public Builder farewell(Text farewell) {
            this.farewell = farewell;
            return this;
        }

        @Override
        public Builder greeting(Text greeting) {
            this.greeting = greeting;
            return this;
        }

        @Override
        public Builder spawnPos(Vector3i spawnPos) {
            this.spawnPos = spawnPos;
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
            if (this.type == ClaimType.SUBDIVISION) {
                checkNotNull(this.parent);
            }
            if (this.type == ClaimType.ADMIN || this.type == ClaimType.WILDERNESS) {
                this.sizeRestrictions = false;
                this.levelRestrictions = false;
            }

            GPClaim claim = new GPClaim(this.world, this.point1, this.point2, this.type, this.ownerUniqueId, this.cuboid);
            claim.parent = (GPClaim) this.parent;
            Player player = null;
            final Cause cause = Sponge.getCauseStackManager().getCurrentCause();
            if (cause.root() instanceof Player) {
                player = (Player) cause.root();
            }
            GPPlayerData playerData = null;

            if (this.ownerUniqueId != null) {
                if (playerData == null) {
                    playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(this.world, this.ownerUniqueId);
                }

                if (this.levelRestrictions) {
                    if (claim.getLesserBoundaryCorner().getBlockY() < playerData.getMinClaimLevel()) {
                        Text message = null;
                        if (player != null) {
                            message = GriefPreventionPlugin.instance.messageData.claimBelowLevel
                                    .apply(ImmutableMap.of(
                                    "claim-level", playerData.getMinClaimLevel())).build();
                            GriefPreventionPlugin.sendMessage(player, message);
                        }
                        return new GPClaimResult(claim, ClaimResultType.BELOW_MIN_LEVEL, message);
                    }
                    if (claim.getGreaterBoundaryCorner().getBlockY() > playerData.getMaxClaimLevel()) {
                        Text message = null;
                        if (player != null) {
                            message = GriefPreventionPlugin.instance.messageData.claimAboveLevel
                                    .apply(ImmutableMap.of(
                                    "claim-level", playerData.getMaxClaimLevel())).build();
                            GriefPreventionPlugin.sendMessage(player, message);
                        }
                        return new GPClaimResult(claim, ClaimResultType.ABOVE_MAX_LEVEL, message);
                    }
                }

                if (this.sizeRestrictions) {
                    ClaimResult claimResult = claim.checkSizeLimits(player, playerData, this.point1, this.point2);
                    if (!claimResult.successful()) {
                        return claimResult;
                    }
                }

                final User user = GriefPreventionPlugin.getOrCreateUser(this.ownerUniqueId);
                if (this.createLimitRestrictions && !user.hasPermission(GPPermissions.OVERRIDE_CLAIM_LIMIT)) {
                    final Double createClaimLimit = GPOptionHandler.getClaimOptionDouble(user, claim, GPOptions.Type.CLAIM_LIMIT, playerData);
                    if (createClaimLimit != null && createClaimLimit > 0 && (playerData.getInternalClaims().size() + 1) > createClaimLimit.intValue()) {
                        if (player != null) {
                            GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimCreateFailedLimit.toText());
                        }
                        return new GPClaimResult(claim, ClaimResultType.EXCEEDS_MAX_CLAIM_LIMIT, GriefPreventionPlugin.instance.messageData.claimCreateFailedLimit.toText());
                    }
                }

                // check player has enough claim blocks
                if ((claim.isBasicClaim() || claim.isTown()) && this.requiresClaimBlocks) {
                    final int claimCost = BlockUtils.getClaimBlockCost(this.world, claim.lesserBoundaryCorner.getBlockPosition(), claim.greaterBoundaryCorner.getBlockPosition(), claim.cuboid);
                    final int remainingClaimBlocks = playerData.getRemainingClaimBlocks() - claimCost;
                    if (remainingClaimBlocks < 0) {
                        Text message = null;
                        if (player != null) {
                            if (GriefPreventionPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME) {
                                final double claimableChunks = Math.abs(remainingClaimBlocks / 65536.0);
                                message = GriefPreventionPlugin.instance.messageData.claimSizeNeedBlocks3d
                                        .apply(ImmutableMap.of(
                                        "chunks", Math.round(claimableChunks * 100.0)/100.0,
                                        "blocks", Math.abs(remainingClaimBlocks))).build();
                            } else {
                                message = GriefPreventionPlugin.instance.messageData.claimSizeNeedBlocks2d
                                        .apply(ImmutableMap.of("blocks", Math.abs(remainingClaimBlocks))).build();
                            }
                            GriefPreventionPlugin.sendMessage(player, message);
                        }
                        playerData.lastShovelLocation = null;
                        playerData.claimResizing = null;
                        return new GPClaimResult(ClaimResultType.INSUFFICIENT_CLAIM_BLOCKS, message);
                    }
                }

                if (claim.isTown() && GriefPreventionPlugin.instance.economyService != null && player != null) {
                    final double townCost = GriefPreventionPlugin.getGlobalConfig().getConfig().town.cost;
                    if (townCost > 0) {
                        Account playerAccount = GriefPreventionPlugin.instance.economyService.get().getOrCreateAccount(player.getUniqueId()).orElse(null);
                        if (playerAccount == null) {
                            final Text message = GriefPreventionPlugin.instance.messageData.economyUserNotFound
                                    .apply(ImmutableMap.of(
                                    "user", claim.getOwnerName())).build();
                            GriefPreventionPlugin.sendMessage(player, message);
                            return new GPClaimResult(claim, ClaimResultType.NOT_ENOUGH_FUNDS, message);
                        }
                        final double balance = playerAccount.getBalance(GriefPreventionPlugin.instance.economyService.get().getDefaultCurrency()).doubleValue();
                        if (balance < townCost) {
                            final Text message = GriefPreventionPlugin.instance.messageData.townCreateNotEnoughFunds
                                    .apply(ImmutableMap.of(
                                    "create_cost", townCost,
                                    "balance", balance,
                                    "amount_needed", townCost - balance)).build();
                            GriefPreventionPlugin.sendMessage(player, message);
                            return new GPClaimResult(claim, ClaimResultType.NOT_ENOUGH_FUNDS, message);
                        }
                        final Currency defaultCurrency = GriefPreventionPlugin.instance.economyService.get().getDefaultCurrency();
                        playerAccount.withdraw(defaultCurrency, BigDecimal.valueOf(townCost), cause);
                    }
                }
            }

            final ClaimResult result = claim.checkArea(false);
            if (!result.successful()) {
                return result;
            }

            GPCreateClaimEvent.Pre event = new GPCreateClaimEvent.Pre(claim);
            Sponge.getEventManager().post(event);
            if (event.isCancelled()) {
                final Text message = event.getMessage().orElse(null);
                if (message != null && player != null) {
                    GriefPreventionPlugin.sendMessage(player, message);
                }
                return new GPClaimResult(claim, ClaimResultType.CLAIM_EVENT_CANCELLED, message);
            }

            claim.initializeClaimData((GPClaim) this.parent);
            if (this.parent != null) {
                if (this.parent.isTown()) {
                    claim.getData().setInheritParent(true);
                }
                claim.getData().setParent(this.parent.getUniqueId());
            }

            claim.getData().setExpiration(this.expire);
            claim.getData().setDenyMessages(this.denyMessages);
            claim.getData().setFlagOverrides(this.overrides);
            claim.getData().setInheritParent(this.inherit);
            claim.getData().setResizable(this.resizable);
            claim.getData().setRequiresClaimBlocks(this.requiresClaimBlocks);
            claim.getData().setFarewell(this.farewell);
            claim.getData().setGreeting(this.greeting);
            claim.getData().setSpawnPos(this.spawnPos);
            claim.getData().setSizeRestrictions(this.sizeRestrictions);
            final GPClaimManager claimManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(this.world.getProperties());
            claimManager.addClaim(claim, true);

            if (result.getClaims().size() > 1) {
                claim.migrateClaims(new ArrayList<>(result.getClaims()));
            }

            GPCreateClaimEvent.Post postEvent = new GPCreateClaimEvent.Post(claim);
            Sponge.getEventManager().post(postEvent);
            if (postEvent.isCancelled()) {
                final Text message = postEvent.getMessage().orElse(null);
                if (message != null && player != null) {
                    GriefPreventionPlugin.sendMessage(player, message);
                }
                claimManager.deleteClaimInternal(claim, true);
                return new GPClaimResult(claim, ClaimResultType.CLAIM_EVENT_CANCELLED, message);
            }
            return new GPClaimResult(claim, ClaimResultType.SUCCESS);
        }
    }

    public boolean migrateClaims(List<Claim> claims) {
        for (Claim child : claims) {
            if (child.equals(this)) {
                continue;
            }

            GPClaim childClaim = (GPClaim) child;
            final String fileName = childClaim.getClaimStorage().filePath.getFileName().toString();
            if (childClaim.parent != null) {
                childClaim.parent.children.remove(child);
            }
            Path newPath = null;
            if (this.isWilderness()) {
                childClaim.parent = null;
                childClaim.getClaimStorage().getConfig().setParent(null);
                newPath = this.getClaimStorage().filePath.getParent().getParent().resolve(child.getType().name().toLowerCase()).resolve(fileName);
            } else {
                childClaim.parent = this;
                childClaim.getClaimStorage().getConfig().setParent(this.getUniqueId());
                this.children.add(child);
                newPath = this.getClaimStorage().filePath.getParent().resolve(child.getType().name().toLowerCase()).resolve(fileName);
            }

            try {
                Files.createDirectories(newPath.getParent());
                Files.move(childClaim.getClaimStorage().filePath, newPath);
                childClaim.setClaimStorage(new ClaimStorageData(newPath, this.getWorldUniqueId(), (ClaimDataConfig) childClaim.getInternalClaimData()));
                childClaim.getClaimStorage().save();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            final GPClaimManager claimWorldManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(this.world.getProperties());
            if (!this.isWilderness()) {
                claimWorldManager.removeClaimData(childClaim);
            } else {
                claimWorldManager.updateChunkHashes(childClaim);
            }
            // migrate admin children
            if (!childClaim.children.isEmpty()) {
                childClaim.migrateClaims(new ArrayList<>(childClaim.children));
            }
        }

        return true;
    }

    @Override
    public CompletableFuture<FlagResult> clearPermissions(Subject subject) {
        CompletableFuture<FlagResult> result = new CompletableFuture<>();
        Set<Context> contexts = new HashSet<>();
        for (Context context : ClaimContexts.CONTEXT_LIST) {
            contexts.add(context);
        }

        GPFlagClaimEvent.Clear event = new GPFlagClaimEvent.Clear(this, subject, contexts);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            result.complete(new GPFlagResult(FlagResultType.EVENT_CANCELLED, event.getMessage().orElse(null)));
            return result;
        }

        for (Context context : ClaimContexts.CONTEXT_LIST) {
            contexts = new HashSet<>();
            contexts.add(context);
            contexts.add(this.world.getContext());
            subject.getSubjectData().clearPermissions(contexts);
        }

        contexts = new HashSet<>();
        contexts.add(this.getContext());
        contexts.add(this.world.getContext());
        subject.getSubjectData().clearPermissions(contexts);
        result.complete(new GPFlagResult(FlagResultType.SUCCESS));
        return result;
    }

    @Override
    public CompletableFuture<FlagResult> clearPermissions(Context context) {
        return clearPermissions(GriefPreventionPlugin.GLOBAL_SUBJECT, context);
    }

    @Override
    public CompletableFuture<FlagResult> clearPermissions(Subject subject, Context context) {
        CompletableFuture<FlagResult> result = new CompletableFuture<>();
        Set<Context> contexts = new HashSet<>();
        contexts.add(context);
        GPFlagClaimEvent.Clear event = new GPFlagClaimEvent.Clear(this, subject, contexts);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            result.complete(new GPFlagResult(FlagResultType.EVENT_CANCELLED, event.getMessage().orElse(null)));
            return result;
        }

        contexts.add(this.world.getContext());
        subject.getSubjectData().clearPermissions(contexts);
        result.complete(new GPFlagResult(FlagResultType.SUCCESS));
        return result;
    }

    @Override
    public CompletableFuture<FlagResult> setPermission(ClaimFlag flag, Tristate value, Context context) {
       return setPermission(GriefPreventionPlugin.GLOBAL_SUBJECT, flag, value, context);
    }

    @Override
    public CompletableFuture<FlagResult> setPermission(Subject subject, ClaimFlag flag, Tristate value, Context context) {
        return setPermission(subject, flag, "any", value, context);
    }

    @Override
    public CompletableFuture<FlagResult> setPermission(ClaimFlag flag, String target, Tristate value, Context context) {
        return setPermission(GriefPreventionPlugin.GLOBAL_SUBJECT, flag, target, value, context);
    }

    @Override
    public CompletableFuture<FlagResult> setPermission(Subject subject, ClaimFlag flag, String target, Tristate value, Context context) {
        if (target.equalsIgnoreCase("any:any")) {
            target = "any";
        }
        CompletableFuture<FlagResult> result = new CompletableFuture<>();
        if (target != null && !GriefPreventionPlugin.ID_MAP.containsKey(target)) {
            result.complete(new GPFlagResult(FlagResultType.TARGET_NOT_VALID));
            return result;
        }
        if (!ClaimContexts.CONTEXT_LIST.contains(context) && context != this.getContext()) {
            result.complete(new GPFlagResult(FlagResultType.CONTEXT_NOT_VALID));
            return result;
        }

        GPFlagClaimEvent.Set event = new GPFlagClaimEvent.Set(this, subject, flag, null, target, value, context);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            result.complete(new GPFlagResult(FlagResultType.EVENT_CANCELLED, event.getMessage().orElse(null)));
            return result;
        }

        CommandSource commandSource = Sponge.getServer().getConsole();
        final Object root = Sponge.getCauseStackManager().getCurrentCause().root();
        if (root instanceof CommandSource) {
            commandSource = (CommandSource) root;
        }
        String subjectName = subject.getIdentifier();
        if (subject instanceof User) {
            subjectName = ((User) subject).getName();
        } else if (subject == GriefPreventionPlugin.GLOBAL_SUBJECT) {
            subjectName = "ALL";
        }
        result.complete(CommandHelper.addFlagPermission(commandSource, subject, subjectName, this, flag, null, target, value, context, null));
        return result;
    }

    @Override
    public CompletableFuture<FlagResult> setPermission(ClaimFlag flag, String source, String target, Tristate value, Context context) {
       return setPermission(GriefPreventionPlugin.GLOBAL_SUBJECT, flag, source, target, value, context);
    }

    @Override
    public CompletableFuture<FlagResult> setPermission(Subject subject, ClaimFlag flag, String source, String target, Tristate value, Context context) {
        return setPermission(subject, subject.getIdentifier(), flag, source, target, value, context, null);
    }

    public CompletableFuture<FlagResult> setPermission(Subject subject, String friendlyName, ClaimFlag flag, String source, String target, Tristate value, Context context, Text reason) {
        if (source != null && (source.equalsIgnoreCase("any:any") || source.equalsIgnoreCase("any"))) {
            source = null;
        }
        if (target.equalsIgnoreCase("any:any")) {
            target = "any";
        }

        CompletableFuture<FlagResult> result = new CompletableFuture<>();
        if (flag != ClaimFlag.COMMAND_EXECUTE && flag != ClaimFlag.COMMAND_EXECUTE_PVP) {
            if (source != null && !source.contains("pixelmon") && !GriefPreventionPlugin.ID_MAP.containsKey(GPPermissionHandler.getIdentifierWithoutMeta(source))) {
                result.complete(new GPFlagResult(FlagResultType.SOURCE_NOT_VALID));
                return result;
            }
            if (target != null && !target.contains("pixelmon") && !GriefPreventionPlugin.ID_MAP.containsKey(GPPermissionHandler.getIdentifierWithoutMeta(target))) {
                result.complete(new GPFlagResult(FlagResultType.TARGET_NOT_VALID));
                return result;
            }
        }

        GPFlagClaimEvent.Set event = new GPFlagClaimEvent.Set(this, subject, flag, source, target, value, context);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            result.complete(new GPFlagResult(FlagResultType.EVENT_CANCELLED, event.getMessage().orElse(null)));
            return result;
        }

        CommandSource commandSource = Sponge.getServer().getConsole();
        final Object root = Sponge.getCauseStackManager().getCurrentCause().root();
        if (root instanceof CommandSource) {
            commandSource = (CommandSource) root;
        }
        result.complete(CommandHelper.addFlagPermission(commandSource, subject, friendlyName, this, flag, source, target, value, context, reason));
        return result;
    }

    @Override
    public Tristate getPermissionValue(ClaimFlag flag, String source, String target, Context context) {
        return getPermissionValue(GriefPreventionPlugin.GLOBAL_SUBJECT, flag, source, target, context);
    }

    @Override
    public Tristate getPermissionValue(Subject subject, ClaimFlag flag, String source, String target, Context context) {
        if (source.equalsIgnoreCase("any:any") || source.equalsIgnoreCase("any")) {
            source = null;
        }
        if (target.equalsIgnoreCase("any:any") || target.equalsIgnoreCase("any")) {
            target = null;
        }
        if (subject != GriefPreventionPlugin.GLOBAL_SUBJECT && (context == this.getDefaultContext() || context == this.getOverrideContext())) {
            return Tristate.UNDEFINED;
        }
        return GPPermissionHandler.getClaimPermission(this, flag, subject, source, target, context);
    }

    @Override
    public Tristate getPermissionValue(ClaimFlag flag, String target, Context context) {
        return getPermissionValue(GriefPreventionPlugin.GLOBAL_SUBJECT, flag, target, context);
    }

    @Override
    public Tristate getPermissionValue(Subject subject, ClaimFlag flag, String target, Context context) {
        if (target.equalsIgnoreCase("any:any") || target.equalsIgnoreCase("any")) {
            target = null;
        }
        if (subject != GriefPreventionPlugin.GLOBAL_SUBJECT && (context == this.getDefaultContext() || context == this.getOverrideContext())) {
            return Tristate.UNDEFINED;
        }
        return GPPermissionHandler.getClaimPermission(this, flag, subject, null, target, context);
    }

    @Override
    public Map<String, Boolean> getPermissions(Context context) {
        return getPermissions(GriefPreventionPlugin.GLOBAL_SUBJECT, context);
    }

    @Override
    public Map<String, Boolean> getPermissions(Subject subject, Context context) {
        Set<Context> contexts = new HashSet<>();
        contexts.add(context);
        return subject.getSubjectData().getPermissions(contexts);
    }

    @Override
    public Context getDefaultContext() {
        if (this.isAdminClaim()) {
            return ClaimContexts.ADMIN_DEFAULT_CONTEXT;
        }
        if (this.isBasicClaim() || this.isSubdivision()) {
            return ClaimContexts.BASIC_DEFAULT_CONTEXT;
        }
        if (this.isTown()) {
            return ClaimContexts.TOWN_DEFAULT_CONTEXT;
        }

        return ClaimContexts.WILDERNESS_DEFAULT_CONTEXT;
    }

    @Override
    public Context getOverrideContext() {
        if (this.isAdminClaim()) {
            return ClaimContexts.ADMIN_OVERRIDE_CONTEXT;
        }
        if (this.isBasicClaim() || this.isSubdivision()) {
            return ClaimContexts.BASIC_OVERRIDE_CONTEXT;
        }
        if (this.isTown()) {
            return ClaimContexts.TOWN_OVERRIDE_CONTEXT;
        }

        return ClaimContexts.WILDERNESS_OVERRIDE_CONTEXT;
    }
}