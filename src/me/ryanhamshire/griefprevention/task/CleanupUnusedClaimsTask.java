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

import me.ryanhamshire.griefprevention.CustomLogEntryTypes;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.claim.ClaimWorldManager;
import me.ryanhamshire.griefprevention.claim.ClaimsMode;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import org.spongepowered.api.world.storage.WorldProperties;

import java.lang.ref.WeakReference;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;

//FEATURE: automatically remove inactive claims
//runs every 5 minutes on the main thread
public class CleanupUnusedClaimsTask implements Runnable {

    private WeakReference<WorldProperties> worldPropertiesRef;

    public CleanupUnusedClaimsTask(WorldProperties worldProperties) {
        this.worldPropertiesRef = new WeakReference<>(worldProperties);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void run() {
        if (this.worldPropertiesRef.isEnqueued()) {
            return;
        }

        WorldProperties worldProperties = this.worldPropertiesRef.get();
        // don't do anything when there are no claims
        ClaimWorldManager claimWorldManager = GriefPrevention.instance.dataStore.getClaimWorldManager(worldProperties);
        ArrayList<Claim> claimList = (ArrayList<Claim>) claimWorldManager.getWorldClaims();
        if (claimList.size() == 0) {
            return;
        }

        Iterator<Claim> iterator = ((ArrayList) claimList.clone()).iterator();
        while (iterator.hasNext()) {
            Claim claim = iterator.next();
            // skip administrative claims
            if (claim.isAdminClaim()) {
                continue;
            }

            // determine area of the default chest claim
            int areaOfDefaultClaim = 0;
            GriefPreventionConfig<?> activeConfig = GriefPrevention.getActiveConfig(worldProperties);
            if (activeConfig.getConfig().claim.claimRadius >= 0) {
                areaOfDefaultClaim = (int) Math.pow(activeConfig.getConfig().claim.claimRadius * 2 + 1, 2);
            }

            Instant claimLastActive = null;
            try {
                claimLastActive = Instant.parse(claim.getClaimData().getDateLastActive());
            } catch (DateTimeParseException e) {
                return;
            }

            // if this claim is a chest claim and those are set to expire
            if (claim.getArea() <= areaOfDefaultClaim && activeConfig.getConfig().claim.daysInactiveChestClaimExpiration > 0) {
                if (claimLastActive.plus(Duration.ofDays(activeConfig.getConfig().claim.daysInactiveChestClaimExpiration))
                        .isBefore(Instant.now())) {
                    claim.removeSurfaceFluids(null);
                    GriefPrevention.instance.dataStore.deleteClaim(claim, true);

                    // if configured to do so, restore the land to natural
                    if (GriefPrevention.instance.claimModeIsActive(worldProperties, ClaimsMode.Creative) || activeConfig
                            .getConfig().claim.claimAutoNatureRestore) {
                        GriefPrevention.instance.restoreClaim(claim, 0);
                    }

                    GriefPrevention.addLogEntry(" " + claim.getOwnerName() + "'s new player claim expired.", CustomLogEntryTypes.AdminActivity);
                }
            }

            // if configured to always remove claims after some inactivity period without exceptions...
            else if (activeConfig.getConfig().claim.daysInactiveClaimExpiration > 0) {
                if (claimLastActive.plus(Duration.ofDays(activeConfig.getConfig().claim.daysInactiveClaimExpiration))
                        .isBefore(Instant.now())) {
                    GriefPrevention.instance.dataStore.deleteClaim(claim);
                    GriefPrevention.addLogEntry(" All of " + claim.getOwnerName() + "'s claims have expired.", CustomLogEntryTypes.AdminActivity);

                    // if configured to do so, restore the land to natural
                    if (GriefPrevention.instance.claimModeIsActive(worldProperties, ClaimsMode.Creative)
                            || activeConfig.getConfig().claim.claimAutoNatureRestore) {
                        GriefPrevention.instance.restoreClaim(claim, 0);
                    }
                }
            } else if (activeConfig.getConfig().claim.daysInactiveUnusedClaimExpiration > 0
                    && GriefPrevention.instance.claimModeIsActive(worldProperties, ClaimsMode.Creative)) {
                // avoid scanning large claims and administrative claims
                if (claim.isAdminClaim() || claim.getWidth() > 25 || claim.getHeight() > 25) {
                    continue;
                }

                // otherwise scan the claim content
                int minInvestment = 400;

                long investmentScore = claim.getPlayerInvestmentScore();
                if (investmentScore < minInvestment) {
                    if (claimLastActive.plus(Duration.ofDays(activeConfig.getConfig().claim.daysInactiveUnusedClaimExpiration))
                            .isBefore(Instant.now())) {
                        GriefPrevention.instance.dataStore.deleteClaim(claim, true);
                        GriefPrevention.addLogEntry("Removed " + claim.getOwnerName() + "'s unused claim @ "
                                + GriefPrevention.getfriendlyLocationString(claim.getLesserBoundaryCorner()), CustomLogEntryTypes.AdminActivity);

                        // restore the claim area to natural state
                        GriefPrevention.instance.restoreClaim(claim, 0);
                    }
                }
            }
        }
    }
}
