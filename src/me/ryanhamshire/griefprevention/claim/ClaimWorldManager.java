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

import com.google.common.collect.Maps;
import me.ryanhamshire.griefprevention.DataStore;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.configuration.PlayerStorageData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

public class ClaimWorldManager {

    public final static Path playerDataPath = Paths.get("GriefPreventionData", "PlayerData");
    private WorldProperties worldProperties;
    private GriefPreventionConfig<?> activeConfig;

    // World UUID -> player data
    private Map<UUID, PlayerData> playerDataList = Maps.newHashMap();
    // Player UUID -> storage
    private Map<UUID, PlayerStorageData> playerStorageList = Maps.newHashMap();
    // Player UUID -> claims
    private Map<UUID, List<Claim>> playerClaimList = Maps.newHashMap();
    // World claim list
    private List<Claim> worldClaims = new ArrayList<>();
    // Claim UUID -> Claim
    private Map<UUID, Claim> claimUniqueIdMap = Maps.newHashMap();
    // String -> Claim
    private ConcurrentHashMap<Long, Set<Claim>> chunksToClaimsMap = new ConcurrentHashMap<>();
    private Claim theWildernessClaim;

    public ClaimWorldManager() {
        this.worldProperties = null;
        this.activeConfig = GriefPrevention.getGlobalConfig();
    }

    public ClaimWorldManager(WorldProperties worldProperties) {
        this.worldProperties = worldProperties;
        this.activeConfig = GriefPrevention.getActiveConfig(this.worldProperties);
    }

    public PlayerData getPlayerData(UUID playerUniqueId) {
        PlayerData playerData = this.playerDataList.get(playerUniqueId);
        if (playerData == null) {
            return createPlayerData(playerUniqueId);
        } else {
            return playerData;
        }
    }

    public PlayerData createPlayerData(UUID playerUniqueId) {
        PlayerData playerData = this.playerDataList.get(playerUniqueId);
        if (playerData != null) {
            return playerData;
        }

        Path playerFilePath = null;
        if (DataStore.USE_GLOBAL_PLAYER_STORAGE) {
            playerFilePath = DataStore.globalPlayerDataPath.resolve(playerUniqueId.toString());
        } else {
            playerFilePath = DataStore.worldConfigMap.get(this.worldProperties.getUniqueId()).getPath().getParent().resolve("PlayerData").resolve(playerUniqueId.toString());
        }

        PlayerStorageData playerStorage = new PlayerStorageData(playerFilePath, playerUniqueId, this.activeConfig.getConfig().general.claimInitialBlocks);
        List<Claim> claimList = new ArrayList<>();
        for (Claim claim : this.worldClaims) {
            if (claim.parent != null) {
               if (claim.parent.ownerID.equals(playerUniqueId)) {
                   claimList.add(claim);
               }
            } else {
                if (claim.ownerID.equals(playerUniqueId)) {
                    claimList.add(claim);
                }
            }
        }
        playerData = new PlayerData(this.worldProperties, playerUniqueId, playerStorage, this.activeConfig, claimList);
        this.playerStorageList.put(playerUniqueId, playerStorage);
        this.playerDataList.put(playerUniqueId, playerData);
        this.playerClaimList.put(playerUniqueId, claimList);
        return playerData;
    }

    public void removePlayer(UUID playerUniqueId) {
        this.playerClaimList.remove(playerUniqueId);
        this.playerStorageList.remove(playerUniqueId);
        this.playerDataList.remove(playerUniqueId);
    }

    public void addWorldClaim(Claim claim) {
        if (!claim.isWildernessClaim()) {
            if (claim.parent != null) {
                return;
            }
    
            UUID ownerId = claim.ownerID;
            if (!this.worldClaims.contains(claim)) {
                this.worldClaims.add(claim);
            }
            if (!this.claimUniqueIdMap.containsKey(claim.id)) {
                this.claimUniqueIdMap.put(claim.id, claim);
            }
    
            PlayerData playerData = this.playerDataList.get(ownerId);
            if (claim.parent == null && playerData != null) {
                List<Claim> playerClaims = playerData.getClaims();
                if (!playerClaims.contains(claim)) {
                    playerClaims.add(claim);
                }
            } else {
                createPlayerData(ownerId);
            }
    
            Set<Long> chunkHashes = claim.getChunkHashes();
            for (Long chunkHash : chunkHashes) {
                Set<Claim> claimsInChunk = this.getChunksToClaimsMap().get(chunkHash);
                if (claimsInChunk == null) {
                    claimsInChunk = new HashSet<Claim>();
                    this.getChunksToClaimsMap().put(chunkHash, claimsInChunk);
                }

                claimsInChunk.add(claim);
            }
        } else {
            this.theWildernessClaim = claim;
        }
    }

    public void removePlayerClaim(Claim claim) {
        // player may be offline so check is needed
        if (this.playerClaimList.get(claim.ownerID) != null) {
            this.playerClaimList.get(claim.ownerID).remove(claim);
        }
        this.worldClaims.remove(claim);
        this.claimUniqueIdMap.remove(claim.id);
    }

    @Nullable
    public Claim getClaimByUUID(UUID claimUniqueId) {
        return this.claimUniqueIdMap.get(claimUniqueId);
    }

    @Nullable
    public List<Claim> getPlayerClaims(UUID playerUniqueId) {
        if (this.playerClaimList.get(playerUniqueId) == null) {
            this.createPlayerData(playerUniqueId);
        }
        return this.playerClaimList.get(playerUniqueId);
    }

    public void createWildernessClaim(WorldProperties worldProperties) {
        World world = Sponge.getServer().getWorld(worldProperties.getUniqueId()).get();
        Location<World> lesserCorner = new Location<World>(world, -30000000, 0, -30000000);
        Location<World> greaterCorner = new Location<World>(world, 29999999, 255, 29999999);
        Claim worldClaim = new Claim(lesserCorner, greaterCorner, UUID.randomUUID(), Claim.Type.WILDERNESS, null);
        worldClaim.ownerID = GriefPrevention.WORLD_USER_UUID;
        worldClaim.type = Claim.Type.WILDERNESS;
        worldClaim.context = new Context("gp_claim", worldClaim.id.toString());
        GriefPrevention.instance.dataStore.writeClaimToStorage(worldClaim);
        this.theWildernessClaim = worldClaim;
    }

    public Claim getWildernessClaim() {
        return this.theWildernessClaim;
    }

    public void setWildernessClaim(Claim claim) {
        this.theWildernessClaim = claim;
    }

    public List<Claim> getWorldClaims() {
        return this.worldClaims;
    }

    public ConcurrentHashMap<Long, Set<Claim>> getChunksToClaimsMap() {
        return this.chunksToClaimsMap;
    }

    public void transferClaimOwner(Claim claim, UUID newOwnerID) throws NoTransferException {
        // if it's a subdivision, throw an exception
        if (claim.parent != null) {
            throw new NoTransferException("Subdivisions can't be transferred.  Only top-level claims may change owners.");
        }

        // determine current claim owner
        if (claim.isAdminClaim() || claim.isWildernessClaim()) {
            return;
        }

        PlayerData ownerData = this.getPlayerData(claim.ownerID);
        // determine new owner
        PlayerData newOwnerData = this.getPlayerData(newOwnerID);

        if (newOwnerData == null) {
            throw new NoTransferException("Could not locate PlayerData for new owner with UUID " + newOwnerID + ".");
        }
        // transfer
        claim.ownerID = newOwnerID;
        claim.getClaimData().setClaimOwnerUniqueId(newOwnerID);

        // adjust blocks and other records
        if (ownerData != null) {
              ownerData.getClaims().remove(claim);
        }
        newOwnerData.getClaims().add(claim);
        claim.getClaimStorage().save();
    }

    public void save() {
        for (List<Claim> claimList : this.playerClaimList.values()) {
            for (Claim claim : claimList) {
                if (claim.getClaimData().requiresSave()) {
                    claim.updateClaimStorageData();
                    claim.getClaimStorage().save();
                    claim.getClaimData().setRequiresSave(false);
                }
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

    @SuppressWarnings("serial")
    public class NoTransferException extends Exception {

        NoTransferException(String message) {
            super(message);
        }
    }
}
