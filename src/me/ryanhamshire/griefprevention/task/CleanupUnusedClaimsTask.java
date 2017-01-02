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
package me.ryanhamshire.griefprevention.task;

import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.event.GPNamedCause;
import me.ryanhamshire.griefprevention.claim.ClaimsMode;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.claim.GPClaimManager;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.logging.CustomLogEntryTypes;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.world.storage.WorldProperties;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;

//FEATURE: automatically remove inactive claims
//runs every 5 minutes on the main thread
public class CleanupUnusedClaimsTask implements Runnable {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void run() {
        for (WorldProperties worldProperties : Sponge.getServer().getAllWorldProperties()) {
            // don't do anything when there are no claims
            GPClaimManager claimManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(worldProperties);
            ArrayList<Claim> claimList = (ArrayList<Claim>) claimManager.getWorldClaims();
            if (claimList.size() == 0) {
                continue;
            }

            Iterator<GPClaim> iterator = ((ArrayList) claimList.clone()).iterator();
            while (iterator.hasNext()) {
                GPClaim claim = iterator.next();
                // skip administrative claims
                if (claim.isAdminClaim() || claim.getInternalClaimData().allowClaimExpiration() || claim.ownerPlayerData == null) {
                    continue;
                }

                if (!claim.ownerPlayerData.dataInitialized) {
                    continue;
                }

                GriefPreventionConfig<?> activeConfig = GriefPreventionPlugin.getActiveConfig(worldProperties);
                // determine area of the default chest claim
                int areaOfDefaultClaim = 0;
                if (activeConfig.getConfig().claim.claimRadius >= 0) {
                    areaOfDefaultClaim = (int) Math.pow(activeConfig.getConfig().claim.claimRadius * 2 + 1, 2);
                }
    
                Instant claimLastActive = claim.getInternalClaimData().getDateLastActive();
    
                // if this claim is a chest claim and those are set to expire
                if (claim.getArea() <= areaOfDefaultClaim && claim.ownerPlayerData.optionChestClaimExpiration > 0) {
                    if (claimLastActive.plus(Duration.ofDays(claim.ownerPlayerData.optionChestClaimExpiration))
                            .isBefore(Instant.now())) {
                        claim.removeSurfaceFluids(null);
                        claimManager.deleteClaim(claim, Cause.of(NamedCause.of(GPNamedCause.CHEST_CLAIM_EXPIRED, GriefPreventionPlugin.instance.pluginContainer)));
    
                        // if configured to do so, restore the land to natural
                        if (GriefPreventionPlugin.instance.claimModeIsActive(worldProperties, ClaimsMode.Creative) || activeConfig
                                .getConfig().claim.claimAutoNatureRestore) {
                            GriefPreventionPlugin.instance.restoreClaim(claim, 0);
                        }
    
                        GriefPreventionPlugin.addLogEntry(" " + claim.getOwnerName() + "'s new player claim " + "'" + claim.id + "' expired.", CustomLogEntryTypes.AdminActivity);
                    }
                }
    
                if (claim.ownerPlayerData.optionPlayerClaimExpiration > 0) {
                    if (claimLastActive.plus(Duration.ofDays(claim.ownerPlayerData.optionPlayerClaimExpiration))
                            .isBefore(Instant.now())) {
                        claimManager.deleteClaim(claim, Cause.of(NamedCause.of(GPNamedCause.PLAYER_CLAIM_EXPIRED, GriefPreventionPlugin.instance.pluginContainer)));
                        GriefPreventionPlugin.addLogEntry("Removed " + claim.getOwnerName() + "'s unused claim @ "
                                + GriefPreventionPlugin.getfriendlyLocationString(claim.getLesserBoundaryCorner()), CustomLogEntryTypes.AdminActivity);

                        // if configured to do so, restore the land to natural
                        if (GriefPreventionPlugin.instance.claimModeIsActive(worldProperties, ClaimsMode.Creative)
                                || activeConfig.getConfig().claim.claimAutoNatureRestore) {
                            // restore the claim area to natural state
                            GriefPreventionPlugin.instance.restoreClaim(claim, 0);
                        }
                    }
                }
            }
        }
    }
}
