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

import com.flowpowered.math.vector.Vector3i;
import me.ryanhamshire.GriefPrevention.configuration.GriefPreventionConfig;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.common.world.storage.SpongePlayerDataHandler;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import java.util.Vector;

//FEATURE: automatically remove claims owned by inactive players which:
//...aren't protecting much OR
//...are a free new player claim (and the player has no other claims) OR
//...because the player has been gone a REALLY long time, and that expiration has been configured in config.hocon

//runs every 1 minute in the main thread
public class CleanupUnusedClaimsTask implements Runnable {

    private WorldProperties worldProperties;

    public CleanupUnusedClaimsTask(WorldProperties worldProperties) {
        this.worldProperties = worldProperties;
    }

    @Override
    public void run() {
        // don't do anything when there are no claims
        ArrayList<Claim> claimList = (ArrayList<Claim>) GriefPrevention.instance.dataStore.worldClaims.get(this.worldProperties.getUniqueId());
        if (claimList == null || claimList.size() == 0) {
            return;
        }

        Iterator<Claim> iterator = ((ArrayList)claimList.clone()).iterator();
        while (iterator.hasNext()) {
            Claim claim = iterator.next();
            // skip administrative claims
            if (claim.isAdminClaim()) {
                continue;
            }

            // track whether we do any important work which would require cleanup afterward
            boolean cleanupChunks = false;

            // get data for the player, especially last login timestamp
            PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(this.worldProperties, claim.ownerID);

            // determine area of the default chest claim
            int areaOfDefaultClaim = 0;
            GriefPreventionConfig<?> activeConfig = GriefPrevention.getActiveConfig(this.worldProperties);
            if (activeConfig.getConfig().claim.claimRadius >= 0) {
                areaOfDefaultClaim = (int) Math.pow(activeConfig.getConfig().claim.claimRadius * 2 + 1, 2);
            }

            // if this claim is a chest claim and those are set to expire
            if (claim.getArea() <= areaOfDefaultClaim && activeConfig.getConfig().claim.daysInactiveChestClaimExpiration > 0) {
                // if the owner has been gone at least a week, and if he has ONLY
                // the new player claim, it will be removed
                boolean newPlayerClaimsExpired = false;
                Optional<Instant> lastPlayed = SpongePlayerDataHandler.getLastPlayed(claim.ownerID);
                if (lastPlayed.isPresent() && lastPlayed.get().plus(Duration.ofDays(activeConfig.getConfig().claim.daysInactiveChestClaimExpiration))
                        .isBefore(Instant.now())) {
                    newPlayerClaimsExpired = true;
                }
                if (newPlayerClaimsExpired && claimList.size() == 1) {
                    claim.removeSurfaceFluids(null);
                    GriefPrevention.instance.dataStore.deleteClaim(claim, true);
                    cleanupChunks = true;

                    // if configured to do so, restore the land to natural
                    if (GriefPrevention.instance.claimModeIsActive(this.worldProperties, ClaimsMode.Creative) || activeConfig
                            .getConfig().claim.claimAutoNatureRestore) {
                        GriefPrevention.instance.restoreClaim(claim, 0);
                    }

                    GriefPrevention.AddLogEntry(" " + claim.getOwnerName() + "'s new player claim expired.", CustomLogEntryTypes.AdminActivity);
                }
            }

            // if configured to always remove claims after some inactivity period without exceptions...
            else if (activeConfig.getConfig().claim.daysInactiveClaimExpiration > 0) {
                Optional<Instant> lastPlayed = SpongePlayerDataHandler.getLastPlayed(claim.ownerID);
                if (lastPlayed.isPresent() && lastPlayed.get().plus(Duration.ofDays(activeConfig.getConfig().claim.daysInactiveChestClaimExpiration))
                        .isBefore(Instant.now())) {
                    // make a copy of this player's claim list
                    Vector<Claim> claims = new Vector<Claim>();
                    for (int i = 0; i < claimList.size(); i++) {
                        claims.add(claimList.get(i));
                    }

                    // delete them
                    GriefPrevention.instance.dataStore.deleteClaimsForPlayer(claim.ownerID, true);
                    GriefPrevention.AddLogEntry(" All of " + claim.getOwnerName() + "'s claims have expired.", CustomLogEntryTypes.AdminActivity);

                    for (int i = 0; i < claims.size(); i++) {
                        // if configured to do so, restore the land to natural
                        if (GriefPrevention.instance.claimModeIsActive(this.worldProperties, ClaimsMode.Creative)
                                || activeConfig.getConfig().claim.claimAutoNatureRestore) {
                            GriefPrevention.instance.restoreClaim(claims.get(i), 0);
                            cleanupChunks = true;
                        }
                    }
                }
            } else if (activeConfig.getConfig().claim.daysInactiveUnusedClaimExpiration > 0
                    && GriefPrevention.instance.claimModeIsActive(this.worldProperties, ClaimsMode.Creative)) {
                // avoid scanning large claims and administrative claims
                if (claim.isAdminClaim() || claim.getWidth() > 25 || claim.getHeight() > 25) {
                    continue;
                }

                // otherwise scan the claim content
                int minInvestment = 400;

                long investmentScore = claim.getPlayerInvestmentScore();
                cleanupChunks = true;

                if (investmentScore < minInvestment) {
                    playerData = GriefPrevention.instance.dataStore.getPlayerData(this.worldProperties, claim.ownerID);

                    // if the owner has been gone at least a week, and if he has
                    // ONLY the new player claim, it will be removed
                    Optional<Instant> lastPlayed = SpongePlayerDataHandler.getLastPlayed(claim.ownerID);
                    if (lastPlayed.isPresent() && lastPlayed.get().plus(Duration.ofDays(activeConfig.getConfig().claim.daysInactiveUnusedClaimExpiration))
                            .isBefore(Instant.now())) {
                        GriefPrevention.instance.dataStore.deleteClaim(claim, true);
                        GriefPrevention.AddLogEntry("Removed " + claim.getOwnerName() + "'s unused claim @ "
                                + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()), CustomLogEntryTypes.AdminActivity);

                        // restore the claim area to natural state
                        GriefPrevention.instance.restoreClaim(claim, 0);
                    }
                }
            }

            if (playerData != null) {
                GriefPrevention.instance.dataStore.clearCachedPlayerData(claim.ownerID);
            }

            // since we're potentially loading a lot of chunks to scan parts of the
            // world where there are no players currently playing, be mindful of
            // memory usage
            if (cleanupChunks) {
                World world = claim.getLesserBoundaryCorner().getExtent();
                final Optional<Chunk> optLesserChunk = world.getChunk(claim.getLesserBoundaryCorner().getBlockPosition());
                final Optional<Chunk> optGreaterChunk = world.getChunk(claim.getGreaterBoundaryCorner().getBlockPosition());

                if (optLesserChunk.isPresent() && optGreaterChunk.isPresent()) {
                    final Vector3i lesserChunkPos = optLesserChunk.get().getPosition();
                    final Vector3i greaterChunkPos = optGreaterChunk.get().getPosition();

                    for (int x = lesserChunkPos.getX(); x <= greaterChunkPos.getX(); x++) {
                        for (int z = lesserChunkPos.getZ(); z <= greaterChunkPos.getZ(); z++) {
                            Optional<Chunk> chunk = world.getChunk(x, 0, z);
                            if (chunk.isPresent() && chunk.get().isLoaded()) {
                                chunk.get().unloadChunk();
                            }
                        }
                    }
                }
            }
        }
    }
}
