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
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.ryanhamshire.griefprevention.DataStore;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GPTimings;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimManager;
import me.ryanhamshire.griefprevention.api.claim.ClaimResult;
import me.ryanhamshire.griefprevention.api.claim.ClaimResultType;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.configuration.PlayerStorageData;
import me.ryanhamshire.griefprevention.event.GPDeleteClaimEvent;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.lang.ref.WeakReference;
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
    // Player UUID -> storage
    private Map<UUID, PlayerStorageData> playerStorageList = Maps.newHashMap();
    // Player UUID -> claims
    private Map<UUID, List<Claim>> playerClaimList = Maps.newHashMap();
    // World claim list
    private List<Claim> worldClaims = new ArrayList<>();
    // Claim UUID -> Claim
    private Map<UUID, Claim> claimUniqueIdMap = Maps.newHashMap();
    // String -> Claim
    private Map<Long, Set<GPClaim>> chunksToClaimsMap = new Long2ObjectOpenHashMap<>(4096);
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
        GPPlayerData playerData = null;
        if (DataStore.USE_GLOBAL_PLAYER_STORAGE) {
            playerData = DataStore.GLOBAL_PLAYER_DATA.get(playerUniqueId);
        } else {
            playerData = this.playerDataList.get(playerUniqueId);
        }
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
        List<Claim> claimList = new ArrayList<>();
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

        GPPlayerData playerData = new GPPlayerData(this.worldProperties, playerUniqueId, playerStorage, this.activeConfig, claimList);
        this.playerClaimList.put(playerUniqueId, claimList);
        this.playerStorageList.put(playerUniqueId, playerStorage);
        if (DataStore.USE_GLOBAL_PLAYER_STORAGE) {
            DataStore.GLOBAL_PLAYER_DATA.put(playerUniqueId, playerData);
        } else {
            this.playerDataList.put(playerUniqueId, playerData);
        }
        return playerData;
    }

    public void removePlayer(UUID playerUniqueId) {
        this.playerClaimList.remove(playerUniqueId);
        this.playerStorageList.remove(playerUniqueId);
        if (DataStore.USE_GLOBAL_PLAYER_STORAGE) {
            DataStore.GLOBAL_PLAYER_DATA.remove(playerUniqueId);
        } else {
            this.playerDataList.remove(playerUniqueId);
        }
    }

    @Override
    public void addClaim(Claim claim, Cause cause) {
        GPClaim newClaim = (GPClaim) claim;
        // ensure this new claim won't overlap any existing claims
        GPClaim overlapClaim = (GPClaim) newClaim.doesClaimOverlap();
        if (overlapClaim != null) {
            return;
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
    }

    /*public void addClaim(Claim claimToAdd) {
        this.addClaim(claimToAdd, false);
    }*/

    public void addClaim(Claim claimToAdd, boolean writeToStorage) {
        GPClaim claim = (GPClaim) claimToAdd;
        if (this.worldClaims.contains(claimToAdd)) {
            return;
        }

        if (claim.isWildernessClaim()) {
            this.theWildernessClaim = claim;
            if (writeToStorage) {
                DATASTORE.writeClaimToStorage(claim);
            }
            return;
        }
 
        if (claim.parent != null) {
            return;
        }

        UUID ownerId = claim.getOwnerUniqueId();
        if (!this.worldClaims.contains(claim)) {
            this.worldClaims.add(claim);
        }

        this.claimUniqueIdMap.put(claim.id, claim);

        GPPlayerData playerData = null;
        if (DataStore.USE_GLOBAL_PLAYER_STORAGE) {
            playerData = DataStore.GLOBAL_PLAYER_DATA.get(ownerId);
        } else {
            playerData = this.playerDataList.get(ownerId);
        }
        if (claim.parent == null && playerData != null) {
            List<Claim> playerClaims = playerData.getClaims();
            if (!playerClaims.contains(claim)) {
                playerClaims.add(claim);
            }
        } else if (!claim.isAdminClaim()) {
            this.createPlayerData(ownerId);
        }

        Set<Long> chunkHashes = claim.getChunkHashes(true);
        for (Long chunkHash : chunkHashes) {
            Set<GPClaim> claimsInChunk = this.getChunksToClaimsMap().get(chunkHash);
            if (claimsInChunk == null) {
                claimsInChunk = new HashSet<GPClaim>();
                this.getChunksToClaimsMap().put(chunkHash, claimsInChunk);
            }

            claimsInChunk.add(claim);
        }

        if (writeToStorage) {
            DATASTORE.writeClaimToStorage(claim);
        }
        return;
    }

    public ClaimResult deleteClaim(Claim claim, Cause cause) {
        if (cause != null) {
            GPDeleteClaimEvent event = new GPDeleteClaimEvent(claim, cause);
            Sponge.getEventManager().post(event);
            if (event.isCancelled()) {
                return new GPClaimResult(claim, ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(null));
            }
        }

        this.deleteClaim(claim);
        return new GPClaimResult(claim, ClaimResultType.SUCCESS);
    }

    public void deleteClaim(Claim claim) {
        if (claim.isSubdivision()) {
            GPClaim parent = (GPClaim) claim.getParent().get();
            parent.deleteSubdivision(claim, true);
            return;
        }

        // delete any subdivisions
        List<Claim> subClaims = claim.getSubdivisions();
        for (Claim subdivision : subClaims) {
            GPClaim gpClaim = (GPClaim) subdivision;
            gpClaim.deleteSubdivision(subdivision, false);
        }
        ((GPClaim) claim).getClaimStorage().save();

        // player may be offline so check is needed
        if (this.playerClaimList.get(claim.getOwnerUniqueId()) != null) {
            this.playerClaimList.get(claim.getOwnerUniqueId()).remove(claim);
        }
        this.worldClaims.remove(claim);
        this.claimUniqueIdMap.remove(claim.getUniqueId());
        this.updateChunkHashes((GPClaim) claim);
        // revert visuals for all players watching this claim
        List<UUID> playersWatching = new ArrayList<>(((GPClaim) claim).playersWatching);
        for (UUID playerUniqueId : playersWatching) {
            Player player = Sponge.getServer().getPlayer(playerUniqueId).orElse(null);
            if (player != null) {
                GPPlayerData playerData = this.getOrCreatePlayerData(playerUniqueId);
                playerData.revertActiveVisual(player);
            }
        }

        DATASTORE.deleteClaimFromSecondaryStorage((GPClaim) claim);
    }

    private void updateChunkHashes(GPClaim claim) {
        Set<Long> chunkHashes = claim.getChunkHashes(false);
        for (Long chunkHash : chunkHashes) {
            Set<GPClaim> claimsInChunk = this.getChunksToClaimsMap().get(chunkHash);
            if (claimsInChunk != null) {
                claimsInChunk.remove(claim);
            }
        }
    }

    @Nullable
    public Optional<Claim> getClaimByUUID(UUID claimUniqueId) {
        return Optional.ofNullable(this.claimUniqueIdMap.get(claimUniqueId));
    }

    public List<Claim> getInternalPlayerClaims(UUID playerUniqueId) {
        return this.playerClaimList.get(playerUniqueId);
    }

    @Nullable
    public List<Claim> getPlayerClaims(UUID playerUniqueId) {
        if (this.playerClaimList.get(playerUniqueId) == null) {
            return ImmutableList.of();
        }
        return ImmutableList.copyOf(this.playerClaimList.get(playerUniqueId));
    }

    public void createWildernessClaim(WorldProperties worldProperties) {
        World world = Sponge.getServer().getWorld(worldProperties.getUniqueId()).get();
        Location<World> lesserCorner = new Location<World>(world, -30000000, 0, -30000000);
        Location<World> greaterCorner = new Location<World>(world, 29999999, 255, 29999999);
        GPClaim worldClaim = new GPClaim(lesserCorner, greaterCorner, UUID.randomUUID(), ClaimType.WILDERNESS, null);
        worldClaim.setOwnerUniqueId(GriefPreventionPlugin.WORLD_USER_UUID);
        worldClaim.initializeClaimData(null);
        DATASTORE.writeClaimToStorage(worldClaim);
        this.theWildernessClaim = worldClaim;
    }

    @Override
    public GPClaim getWildernessClaim() {
        return this.theWildernessClaim;
    }

    @Override
    public List<Claim> getWorldClaims() {
        return this.worldClaims;
    }

    public Map<UUID, GPPlayerData> getPlayerDataList() {
        if (DataStore.USE_GLOBAL_PLAYER_STORAGE) {
            return DataStore.GLOBAL_PLAYER_DATA;
        }
        return this.playerDataList;
    }

    public Map<Long, Set<GPClaim>> getChunksToClaimsMap() {
        return this.chunksToClaimsMap;
    }

    public void save() {
        for (Claim claim : this.worldClaims) {
            GPClaim gpClaim = (GPClaim) claim;
            if (gpClaim.getInternalClaimData().requiresSave()) {
                gpClaim.updateClaimStorageData();
                gpClaim.getClaimStorage().save();
                gpClaim.getInternalClaimData().setRequiresSave(false);
            }
        }

        for (PlayerStorageData storageData : this.playerStorageList.values()) {
            if (storageData != null) {
                storageData.save();
            }
        }
    }

    public void unload() {
        this.playerClaimList.clear();
        this.playerDataList.clear();
        this.playerStorageList.clear();
        this.worldClaims.clear();
        this.claimUniqueIdMap.clear();
        this.chunksToClaimsMap.clear();
        if (this.theWildernessClaim != null) {
            this.theWildernessClaim.unload();
            this.theWildernessClaim = null;
        }
        this.worldProperties = null;
    }

    public Claim getClaimAtPlayer(GPPlayerData playerData, Location<World> location, boolean ignoreHeight) {
        return this.getClaimAt(location, ignoreHeight, playerData.lastClaim);
    }

    @Override
    public Claim getClaimAt(Location<World> location, boolean ignoreHeight) {
        return this.getClaimAt(location, ignoreHeight, null);
    }

    // gets the claim at a specific location
    // ignoreHeight = TRUE means that a location UNDER an existing claim will return the claim
    public Claim getClaimAt(Location<World> location, boolean ignoreHeight,  WeakReference<Claim> cachedClaimRef) {
        GPTimings.CLAIM_GETCLAIM.startTimingIfSync();
        Claim cachedClaim = null;
        if (cachedClaimRef != null) {
            cachedClaim = cachedClaimRef.get();
        }
        // check cachedClaim guess first. if the location is inside it, we're done
        if (cachedClaim != null && !cachedClaim.isWilderness() && cachedClaim.contains(location, ignoreHeight, true)) {
            GPTimings.CLAIM_GETCLAIM.stopTimingIfSync();
            return cachedClaim;
        }

        Set<GPClaim> claimsInChunk = this.getChunksToClaimsMap().get(ChunkPos.chunkXZ2Int(location.getBlockX() >> 4, location.getBlockZ() >> 4));
        if (claimsInChunk == null) {
            GPTimings.CLAIM_GETCLAIM.stopTimingIfSync();
            return this.getWildernessClaim();
        }

        for (GPClaim claim : claimsInChunk) {
            if (claim.contains(location, claim.isCuboid() ? false : ignoreHeight, false)) {
                // when we find a top level claim, if the location is in one of its subdivisions,
                // return the SUBDIVISION, not the top level claim
                for (int j = 0; j < claim.children.size(); j++) {
                    Claim subdivision = claim.children.get(j);
                    if (subdivision.contains(location, subdivision.isCuboid() ? false : ignoreHeight, false)) {
                        GPTimings.CLAIM_GETCLAIM.stopTimingIfSync();
                        return subdivision;
                    }
                }

                GPTimings.CLAIM_GETCLAIM.stopTimingIfSync();
                return claim;
            }
        }

        GPTimings.CLAIM_GETCLAIM.stopTimingIfSync();
        // if no claim found, return the world claim
        return this.getWildernessClaim();
    }

    @Override
    public WorldProperties getWorldProperties() {
        return this.worldProperties;
    }
}
