/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
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

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.ryanhamshire.griefprevention.DataStore;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GPTimings;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimBlockSystem;
import me.ryanhamshire.griefprevention.api.claim.ClaimManager;
import me.ryanhamshire.griefprevention.api.claim.ClaimResult;
import me.ryanhamshire.griefprevention.api.claim.ClaimResultType;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.configuration.ClaimDataConfig;
import me.ryanhamshire.griefprevention.configuration.ClaimStorageData;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.configuration.PlayerStorageData;
import me.ryanhamshire.griefprevention.event.GPDeleteClaimEvent;
import me.ryanhamshire.griefprevention.util.BlockUtils;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

public class GPClaimManager implements ClaimManager {

    private static final DataStore DATASTORE = GriefPreventionPlugin.instance.dataStore;
    private WorldProperties worldProperties;
    private GriefPreventionConfig<?> activeConfig;

    // Player UUID -> player data
    private Map<UUID, GPPlayerData> playerDataList = Maps.newHashMap();
    // World claim list
    private Set<Claim> worldClaims = new HashSet<>();
    // Claim UUID -> Claim
    private Map<UUID, Claim> claimUniqueIdMap = Maps.newHashMap();
    // String -> Claim
    private Map<Long, Set<Claim>> chunksToClaimsMap = new Long2ObjectOpenHashMap<>(4096);
    private GPClaim theWildernessClaim;

    public GPClaimManager() {
        this.worldProperties = null;
        this.activeConfig = GriefPreventionPlugin.getGlobalConfig();
    }

    public GPClaimManager(WorldProperties worldProperties) {
        this.worldProperties = worldProperties;
        this.activeConfig = GriefPreventionPlugin.getActiveConfig(this.worldProperties);
    }

    public GPPlayerData getOrCreatePlayerData(UUID playerUniqueId) {
        GPPlayerData playerData = this.getPlayerDataMap().get(playerUniqueId);
        if (playerData == null) {
            return createPlayerData(playerUniqueId);
        } else {
            return playerData;
        }
    }

    private GPPlayerData createPlayerData(UUID playerUniqueId) {
        Path playerFilePath = null;
        if (DataStore.USE_GLOBAL_PLAYER_STORAGE) {
            playerFilePath = DataStore.globalPlayerDataPath.resolve(playerUniqueId.toString());
        } else {
            playerFilePath = DataStore.worldConfigMap.get(this.worldProperties.getUniqueId()).getPath().getParent().resolve("PlayerData").resolve(playerUniqueId.toString());
        }

        PlayerStorageData playerStorage = new PlayerStorageData(playerFilePath);
        Set<Claim> claimList = this.createPlayerClaimList(playerUniqueId);
        GPPlayerData playerData = new GPPlayerData(this.worldProperties, playerUniqueId, playerStorage, this.activeConfig, claimList);
        this.getPlayerDataMap().put(playerUniqueId, playerData);
        return playerData;
    }

    private Set<Claim> createPlayerClaimList(UUID playerUniqueId) {
        Set<Claim> claimList = new HashSet<>();
        if (DataStore.USE_GLOBAL_PLAYER_STORAGE) {
            for (World world : Sponge.getServer().getWorlds()) {
                GPClaimManager claimmanager = DATASTORE.getClaimWorldManager(world.getProperties());
                for (Claim claim : claimmanager.worldClaims) {
                    GPClaim gpClaim = (GPClaim) claim;
                    if (gpClaim.isAdminClaim()) {
                        continue;
                    }
                    if (gpClaim.parent != null) {
                       if (gpClaim.parent.getOwnerUniqueId().equals(playerUniqueId)) {
                           claimList.add(claim);
                       }
                    } else {
                        if (gpClaim.getOwnerUniqueId().equals(playerUniqueId)) {
                            claimList.add(claim);
                        }
                    }
                }
            }
        } else {
            for (Claim claim : this.worldClaims) {
                GPClaim gpClaim = (GPClaim) claim;
                if (gpClaim.isAdminClaim()) {
                    continue;
                }
                if (gpClaim.parent != null) {
                   if (gpClaim.parent.getOwnerUniqueId().equals(playerUniqueId)) {
                       claimList.add(claim);
                   }
                } else {
                    if (gpClaim.getOwnerUniqueId().equals(playerUniqueId)) {
                        claimList.add(claim);
                    }
                }
            }
        }

        return claimList;
    }

    public void removePlayer(UUID playerUniqueId) {
        this.getPlayerDataMap().remove(playerUniqueId);
    }

    public ClaimResult addClaim(Claim claim) {
        GPClaim newClaim = (GPClaim) claim;
        // ensure this new claim won't overlap any existing claims
        ClaimResult result = newClaim.checkArea(false);
        if (!result.successful()) {
            return result;
        }

        // validate world
        if (!this.worldProperties.getUniqueId().equals(newClaim.getWorld().getProperties().getUniqueId())) {
            World world = Sponge.getServer().getWorld(this.worldProperties.getUniqueId()).get();
            Vector3i lesserPos = newClaim.getLesserBoundaryCorner().getBlockPosition();
            Vector3i greaterPos = newClaim.getGreaterBoundaryCorner().getBlockPosition();
            newClaim.world = world;
            newClaim.lesserBoundaryCorner = new Location<World>(world, lesserPos);
            newClaim.greaterBoundaryCorner = new Location<World>(world, greaterPos);
        }

        // otherwise add this new claim to the data store to make it effective
        this.addClaim(newClaim, true);
        if (result.getClaims().size() > 1) {
            newClaim.migrateClaims(new ArrayList<>(result.getClaims()));
        }
        return result;
    }

    public void addClaim(Claim claimToAdd, boolean writeToStorage) {
        GPClaim claim = (GPClaim) claimToAdd;
        if (claim.parent == null && this.worldClaims.contains(claimToAdd)) {
            return;
        }

        if (writeToStorage) {
            DATASTORE.writeClaimToStorage(claim);
        }

        // We need to keep track of all claims so they can be referenced by children during server startup
        this.claimUniqueIdMap.put(claim.id, claim);

        if (claim.isWilderness()) {
            this.theWildernessClaim = claim;
            return;
        }

        if (claim.parent != null) {
            claim.parent.children.add(claim);
            this.worldClaims.remove(claim);
            this.deleteChunkHashes((GPClaim) claim);
            if (!claim.isAdminClaim() && (!claim.isInTown() || !claim.getTownClaim().getOwnerUniqueId().equals(claim.getOwnerUniqueId()))) {
                final GPPlayerData playerData = this.getPlayerDataMap().get(claim.getOwnerUniqueId());
                Set<Claim> playerClaims = playerData.getInternalClaims();
                if (!playerClaims.contains(claim)) {
                    playerClaims.add(claim);
                }
            }
            return;
        }

        if (!this.worldClaims.contains(claim)) {
            this.worldClaims.add(claim);
        }
        final UUID ownerId = claim.getOwnerUniqueId();
        final GPPlayerData playerData = this.getPlayerDataMap().get(ownerId);
        if (playerData != null) {
            Set<Claim> playerClaims = playerData.getInternalClaims();
            if (!playerClaims.contains(claim)) {
                playerClaims.add(claim);
            }
        } else if (!claim.isAdminClaim()) {
            this.createPlayerData(ownerId);
        }

        this.updateChunkHashes(claim);
        return;
    }

    public void updateChunkHashes(GPClaim claim) {
        this.deleteChunkHashes(claim);
        Set<Long> chunkHashes = claim.getChunkHashes(true);
        for (Long chunkHash : chunkHashes) {
            Set<Claim> claimsInChunk = this.getInternalChunksToClaimsMap().get(chunkHash);
            if (claimsInChunk == null) {
                claimsInChunk = new HashSet<Claim>();
                this.getInternalChunksToClaimsMap().put(chunkHash, claimsInChunk);
            }

            claimsInChunk.add(claim);
        }
    }

    // Used when parent claims becomes children
    public void removeClaimData(Claim claim) {
        this.worldClaims.remove(claim);
        this.deleteChunkHashes((GPClaim) claim);
    }

    @Override
    public ClaimResult deleteClaim(Claim claim, boolean deleteChildren) {
        GPDeleteClaimEvent event = new GPDeleteClaimEvent(claim);
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            return new GPClaimResult(claim, ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
        }

        this.deleteClaimInternal(claim, deleteChildren);
        return new GPClaimResult(claim, ClaimResultType.SUCCESS);
    }

    public void deleteClaimInternal(Claim claim, boolean deleteChildren) {
        final GPClaim gpClaim = (GPClaim) claim;
        List<Claim> subClaims = claim.getChildren(false);
        for (Claim child : subClaims) {
            if (deleteChildren || (gpClaim.parent == null && child.isSubdivision())) {
                this.deleteClaimInternal(child, true);
                continue;
            }

            final GPClaim parentClaim = (GPClaim) claim;
            final GPClaim childClaim = (GPClaim) child;
            if (parentClaim.parent != null) {
                migrateChildToNewParent(parentClaim.parent, childClaim);
            } else {
                // move child to parent folder
                migrateChildToNewParent(null, childClaim);
            }
        }

        resetPlayerClaimVisuals(claim);
        // transfer bank balance to owner
        final Account bankAccount = claim.getEconomyAccount().orElse(null);
        if (bankAccount != null) {
            try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                Sponge.getCauseStackManager().pushCause(GriefPreventionPlugin.instance);
                final EconomyService economyService = GriefPreventionPlugin.instance.economyService.get();
                final UniqueAccount ownerAccount = economyService.getOrCreateAccount(claim.getOwnerUniqueId()).orElse(null);
                if (ownerAccount != null) {
                    ownerAccount.deposit(economyService.getDefaultCurrency(), bankAccount.getBalance(economyService.getDefaultCurrency()),
                        Sponge.getCauseStackManager().getCurrentCause());
                }
                bankAccount.resetBalance(economyService.getDefaultCurrency(), Sponge.getCauseStackManager().getCurrentCause());
            }
        }
        this.worldClaims.remove(claim);
        this.claimUniqueIdMap.remove(claim.getUniqueId());
        this.deleteChunkHashes((GPClaim) claim);
        if (gpClaim.parent != null) {
            gpClaim.parent.children.remove(claim);
        }

        DATASTORE.deleteClaimFromSecondaryStorage((GPClaim) claim);
    }

    // Migrates children to new parent
    private void migrateChildToNewParent(GPClaim parentClaim, GPClaim childClaim) {
        childClaim.parent = parentClaim;
        String fileName = childClaim.getClaimStorage().filePath.getFileName().toString();
        Path newPath = null;
        if (parentClaim == null) {
            newPath = childClaim.getClaimStorage().folderPath.getParent().getParent().resolve(childClaim.getType().name().toLowerCase()).resolve(fileName);
        } else {
            // Only store in same claim type folder if not admin.
            // Admin claims are currently the only type that can hold children of same type within
            if (childClaim.getType().equals(parentClaim.getType()) && (!parentClaim.isAdminClaim())) {
                newPath = parentClaim.getClaimStorage().folderPath.resolve(fileName);
            } else {
                newPath = parentClaim.getClaimStorage().folderPath.resolve(childClaim.getType().name().toLowerCase()).resolve(fileName);
            }
        }

        try {
            if (Files.notExists(newPath.getParent())) {
                Files.createDirectories(newPath.getParent());
            }
            Files.move(childClaim.getClaimStorage().filePath, newPath);
            if (childClaim.getClaimStorage().folderPath.toFile().listFiles().length == 0) {
                Files.delete(childClaim.getClaimStorage().folderPath);
            }
            childClaim.setClaimStorage(new ClaimStorageData(newPath, this.getWorldProperties().getUniqueId(), (ClaimDataConfig) childClaim.getInternalClaimData()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Make sure to update new parent in storage
        final UUID parentUniqueId = parentClaim == null ? null : parentClaim.getUniqueId();
        childClaim.getInternalClaimData().setParent(parentUniqueId);
        this.addClaim(childClaim, true);
        for (Claim child : childClaim.children) {
            migrateChildToNewParent(childClaim, (GPClaim) child);
        }
    }

    private void resetPlayerClaimVisuals(Claim claim) {
        // player may be offline so check is needed
        GPPlayerData playerData = this.getPlayerDataMap().get(claim.getOwnerUniqueId());
        if (playerData != null) {
            playerData.getInternalClaims().remove(claim);
            if (playerData.lastClaim != null) {
                playerData.lastClaim.clear();
            }
        }

        // reset visuals for all players watching this claim
        List<UUID> playersWatching = new ArrayList<>(((GPClaim) claim).playersWatching);
        for (UUID playerUniqueId : playersWatching) {
            Player player = Sponge.getServer().getPlayer(playerUniqueId).orElse(null);
            if (player != null) {
                playerData = this.getOrCreatePlayerData(playerUniqueId);
                playerData.revertActiveVisual(player);
                if (playerData.lastClaim != null) {
                    playerData.lastClaim.clear();
                }
                if (GriefPreventionPlugin.instance.worldEditProvider != null) {
                    GriefPreventionPlugin.instance.worldEditProvider.revertVisuals(player, playerData, claim.getUniqueId());
                }
            }
        }
    }

    private void deleteChunkHashes(GPClaim claim) {
        Set<Long> chunkHashes = claim.getChunkHashes(false);
        if (chunkHashes == null) {
            return;
        }

        for (Long chunkHash : chunkHashes) {
            Set<Claim> claimsInChunk = this.getInternalChunksToClaimsMap().get(chunkHash);
            if (claimsInChunk != null) {
                claimsInChunk.remove(claim);
            }
        }
    }

    @Nullable
    public Optional<Claim> getClaimByUUID(UUID claimUniqueId) {
        return Optional.ofNullable(this.claimUniqueIdMap.get(claimUniqueId));
    }

    public Set<Claim> getInternalPlayerClaims(UUID playerUniqueId) {
        final GPPlayerData playerData = this.getPlayerDataMap().get(playerUniqueId);
        if (playerData == null) {
            return new HashSet<>();
        }
        return playerData.getInternalClaims();
    }

    @Nullable
    public List<Claim> getPlayerClaims(UUID playerUniqueId) {
        final GPPlayerData playerData = this.getPlayerDataMap().get(playerUniqueId);
        if (playerData == null) {
            return ImmutableList.of();
        }
        return ImmutableList.copyOf(this.getPlayerDataMap().get(playerUniqueId).getInternalClaims());
    }

    public void createWildernessClaim(WorldProperties worldProperties) {
        World world = Sponge.getServer().getWorld(worldProperties.getUniqueId()).get();
        Location<World> lesserCorner = new Location<World>(world, -30000000, 0, -30000000);
        Location<World> greaterCorner = new Location<World>(world, 29999999, 255, 29999999);
        // Use world UUID as wilderness claim ID
        GPClaim wilderness = new GPClaim(lesserCorner, greaterCorner, worldProperties.getUniqueId(), ClaimType.WILDERNESS, null, false);
        wilderness.setOwnerUniqueId(GriefPreventionPlugin.WORLD_USER_UUID);
        wilderness.initializeClaimData(null);
        DATASTORE.writeClaimToStorage(wilderness);
        this.theWildernessClaim = wilderness;
        this.claimUniqueIdMap.put(wilderness.getUniqueId(), wilderness);
    }

    @Override
    public GPClaim getWildernessClaim() {
        if (this.theWildernessClaim == null) {
            this.createWildernessClaim(worldProperties);
        }
        return this.theWildernessClaim;
    }

    @Override
    public List<Claim> getWorldClaims() {
        List<Claim> claims = new ArrayList<>();
        claims.addAll(this.worldClaims);
        return claims;
    }

    public Set<Claim> getInternalWorldClaims() {
        return this.worldClaims;
    }

    public Map<UUID, GPPlayerData> getPlayerDataMap() {
        if (DataStore.USE_GLOBAL_PLAYER_STORAGE) {
            return DataStore.GLOBAL_PLAYER_DATA;
        }
        return this.playerDataList;
    }

    @Override
    public Map<Long, Set<Claim>> getChunksToClaimsMap() {
        return ImmutableMap.copyOf(this.chunksToClaimsMap);
    }

    public Map<Long, Set<Claim>> getInternalChunksToClaimsMap() {
        return this.chunksToClaimsMap;
    }

    public void save() {
        for (Claim claim : this.worldClaims) {
            GPClaim gpClaim = (GPClaim) claim;
            gpClaim.save();
        }
        this.theWildernessClaim.save();

        for (GPPlayerData playerData : this.getPlayerDataMap().values()) {
            playerData.getStorageData().save();
        }
    }

    public void unload() {
        this.playerDataList.clear();
        this.worldClaims.clear();
        this.claimUniqueIdMap.clear();
        this.chunksToClaimsMap.clear();
        if (this.theWildernessClaim != null) {
            this.theWildernessClaim.unload();
            this.theWildernessClaim = null;
        }
        this.worldProperties = null;
    }

    @Override
    public Claim getClaimAt(Location<World> location) {
        return this.getClaimAt(location, false);
    }

    public Claim getClaimAt(Location<World> location, boolean useBorderBlockRadius) {
        return this.getClaimAt(location, null, null, useBorderBlockRadius);
    }

    public Claim getClaimAtPlayer(Location<World> location, GPPlayerData playerData) {
        return this.getClaimAt(location, (GPClaim) playerData.lastClaim.get(), playerData, false);
    }

    public Claim getClaimAtPlayer(Location<World> location, GPPlayerData playerData, boolean useBorderBlockRadius) {
        return this.getClaimAt(location, (GPClaim) playerData.lastClaim.get(), playerData, useBorderBlockRadius);
    }

    // gets the claim at a specific location
    // ignoreHeight = TRUE means that a location UNDER an existing claim will return the claim
    public Claim getClaimAt(Location<World> location, GPClaim cachedClaim, GPPlayerData playerData, boolean useBorderBlockRadius) {
        //GPTimings.CLAIM_GETCLAIM.startTimingIfSync();
        // check cachedClaim guess first. if the location is inside it, we're done
        if (cachedClaim != null && !cachedClaim.isWilderness() && cachedClaim.contains(location, true)) {
           // GPTimings.CLAIM_GETCLAIM.stopTimingIfSync();
            return cachedClaim;
        }

        Set<Claim> claimsInChunk = this.getInternalChunksToClaimsMap().get(ChunkPos.asLong(location.getBlockX() >> 4, location.getBlockZ() >> 4));
        if (useBorderBlockRadius && (playerData != null && !playerData.ignoreBorderCheck)) {
            final int borderBlockRadius = GriefPreventionPlugin.getActiveConfig(location.getExtent().getUniqueId()).getConfig().claim.borderBlockRadius;
            // if borderBlockRadius > 0, check surrounding chunks
            if (borderBlockRadius > 0) {
                for (Direction direction : BlockUtils.ORDINAL_SET) {
                    Location<World> currentLocation = location;
                    for (int i = 0; i < borderBlockRadius; i++) { // Handle depth
                        currentLocation = currentLocation.getBlockRelative(direction); 
                        Set<Claim> relativeClaims = this.getInternalChunksToClaimsMap().get(ChunkPos.asLong(currentLocation.getBlockX() >> 4, currentLocation.getBlockZ() >> 4));
                        if (relativeClaims != null) {
                            if (claimsInChunk == null) {
                                claimsInChunk = new HashSet<>();
                            }
                            claimsInChunk.addAll(relativeClaims);
                        }
                    }
                }
            }
        }
        if (claimsInChunk == null) {
            //GPTimings.CLAIM_GETCLAIM.stopTimingIfSync();
            return this.getWildernessClaim();
        }

        // TODO change to check deepest level and work its way up to root
        for (Claim claim : claimsInChunk) {
            GPClaim foundClaim = findClaim((GPClaim) claim, location, playerData, useBorderBlockRadius);
            if (foundClaim != null) {
                return foundClaim;
            }
        }

        //GPTimings.CLAIM_GETCLAIM.stopTimingIfSync();
        // if no claim found, return the world claim
        return this.getWildernessClaim();
    }

    private GPClaim findClaim(GPClaim claim, Location<World> location, GPPlayerData playerData, boolean useBorderBlockRadius) {
        if (claim.contains(location, playerData, useBorderBlockRadius)) {
            // when we find a top level claim, if the location is in one of its children,
            // return the child claim, not the top level claim
            for (Claim childClaim : claim.children) {
                GPClaim child = (GPClaim) childClaim;
                if (!child.children.isEmpty()) {
                    GPClaim innerChild = findClaim(child, location, playerData, useBorderBlockRadius);
                    if (innerChild != null) {
                        return innerChild;
                    }
                }
                // check if child has children (Town -> Basic -> Subdivision)
                if (child.contains(location, playerData, useBorderBlockRadius)) {
                    //GPTimings.CLAIM_GETCLAIM.stopTimingIfSync();
                    return child;
                }
            }
            return claim;
        }
        return null;
    }

    @Override
    public List<Claim> getClaimsByName(String name) {
        List<Claim> claimList = new ArrayList<>();
        for (Claim worldClaim : this.getWorldClaims()) {
            Text claimName = worldClaim.getName().orElse(null);
            if (claimName != null && !claimName.isEmpty()) {
                if (claimName.toPlain().equalsIgnoreCase(name)) {
                    claimList.add(worldClaim);
                }
            }
            // check children
            for (Claim child : ((GPClaim) worldClaim).getChildren(true)) {
                if (child.getUniqueId().toString().equals(name)) {
                    claimList.add(child);
                }
            }
        }
        return claimList;
    }

    public void resetPlayerData() {
        // check migration reset
        if (GriefPreventionPlugin.getGlobalConfig().getConfig().playerdata.resetMigrations) {
            for (GPPlayerData playerData : this.getPlayerDataMap().values()) {
                final PlayerStorageData playerStorage = playerData.getStorageData();
                playerStorage.getConfig().setMigratedBlocks(false);
                playerStorage.save();
            }
        }
        // migrate playerdata to new claim block system
        final int migration3dRate = GriefPreventionPlugin.getGlobalConfig().getConfig().playerdata.migrateVolumeRate;
        final int migration2dRate = GriefPreventionPlugin.getGlobalConfig().getConfig().playerdata.migrateAreaRate;
        final boolean resetClaimBlockData = GriefPreventionPlugin.getGlobalConfig().getConfig().playerdata.resetAccruedClaimBlocks;

        if (migration3dRate <= -1 && migration2dRate <= -1 && !resetClaimBlockData) {
            return;
        }
        if (GriefPreventionPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME && migration2dRate >= 0) {
            return;
        }
        if (GriefPreventionPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.AREA && migration3dRate >= 0) {
            return;
        }

        for (GPPlayerData playerData : this.getPlayerDataMap().values()) {
            final PlayerStorageData playerStorage = playerData.getStorageData();
            final int accruedBlocks = playerStorage.getConfig().getAccruedClaimBlocks();
            int newAccruedBlocks = accruedBlocks;
            // first check reset
            if (resetClaimBlockData) {
                newAccruedBlocks = playerData.getTotalClaimsCost();
                playerStorage.getConfig().setBonusClaimBlocks(0);
            } else if (migration3dRate > -1 && !playerStorage.getConfig().hasMigratedBlocks()) {
                newAccruedBlocks = accruedBlocks * migration3dRate;
                playerStorage.getConfig().setMigratedBlocks(true);
            } else if (migration2dRate > -1 && !playerStorage.getConfig().hasMigratedBlocks()) {
                newAccruedBlocks = accruedBlocks / migration2dRate;
                playerStorage.getConfig().setMigratedBlocks(true);
            }
            if (newAccruedBlocks < 0) {
                newAccruedBlocks = 0;
            }
            if (newAccruedBlocks > playerData.optionMaxAccruedBlocks) {
                newAccruedBlocks = playerData.optionMaxAccruedBlocks;
            }
            playerStorage.getConfig().setAccruedClaimBlocks(newAccruedBlocks);
            playerStorage.save();
        }
    }

    @Override
    public WorldProperties getWorldProperties() {
        return this.worldProperties;
    }
}
