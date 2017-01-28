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
package me.ryanhamshire.griefprevention.api.claim;

import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClaimManager {

    /**
     * Adds a new {@link Claim} to the manager in order to be tracked by world.
     * 
     * Note: This is required in order for a claim to be detected
     * in a world as this adds the claim to all required lists.
     * 
     * @param claim The claim to add
     * @param cause The cause of add
     */
    void addClaim(Claim claim, Cause cause);

    /**
     * Gets the wilderness claim of this manager's world.
     * 
     * @return The wilderness claim
     */
    Claim getWildernessClaim();

    /**
     * Gets the {@link Claim} at specified {@link Location}.
     * 
     * Note: If ignoreHeight is true, this means that a location
     * under an existing claim will return the claim. This also
     * assumes that the claim does not extend to bedrock. This should
     * always be set to false for cuboid claims.
     * 
     * @param location The starting location to check
     * @param ignoreHeight Whether to ignore Y-axis while checking under claim.
     * @return The claim if available, otherwise returns the wilderness claim
     * if none were found.
     */
    Claim getClaimAt(Location<World> location, boolean ignoreHeight);

    /**
     * Gets the {@link Claim} with specified {@link UUID}.
     * 
     * @param claimUniqueId The claim UUID to search for
     * @return The claim, if available
     */
    Optional<Claim> getClaimByUUID(UUID claimUniqueId);

    /**
     * Gets an immutable list of player {@link Claim}'s for specified {@link UUID}.
     * 
     * Note: This will return an empty list if player has no claims.
     * 
     * @param playerUniqueId The player UUID
     * @return An immutable list of claims, empty list if none were found
     */
    List<Claim> getPlayerClaims(UUID playerUniqueId);

    /**
     * Gets an immutable list all world {@link Claim}'s for specified {@link UUID}.
     * 
     * Note: This will return an empty list if no world claims are found.
     * 
     * @return An immutable list of world claims, empty list if none were found
     */
    List<Claim> getWorldClaims();

    /**
     * Deletes a {@link Claim} from the managed world.
     * 
     * @param claim The claim to delete
     * @param cause The cause of deletion
     * @return The claim result
     */
    ClaimResult deleteClaim(Claim claim, Cause cause);

    /**
     * Gets the managed {@link WorldProperties}.
     * 
     * @return The managed world properties
     */
    WorldProperties getWorldProperties();
}
