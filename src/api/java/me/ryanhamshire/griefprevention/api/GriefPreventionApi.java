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
package me.ryanhamshire.griefprevention.api;

import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimManager;
import me.ryanhamshire.griefprevention.api.data.PlayerData;
import me.ryanhamshire.griefprevention.api.economy.BankTransaction;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GriefPreventionApi {

    /**
     * Gets the API version.
     * 
     * @return The API version
     */
    double getApiVersion();

    /**
     * Gets the implementation version.
     * 
     * @return The implementation version
     */
    String getImplementationVersion();

    /**
     * Gets the claim manager of world.
     * 
     * @param world
     * @return The claim manager of world
     */
    default ClaimManager getClaimManager(World world) {
        return this.getClaimManager(world.getProperties());
    }

    /**
     * Gets the global {@link PlayerData} for specified {@link UUID}.
     * 
     * @param playerUniqueId The player UUID
     * @return The player data associated with uuid, if available
     */
    Optional<PlayerData> getGlobalPlayerData(UUID playerUniqueId);

    /**
     * Gets the world {@link PlayerData} for specified {@link UUID}.
     * 
     * @param worldProperties The world properties
     * @param playerUniqueId The player UUID
     * @return The player data associated with uuid, if available
     */
    Optional<PlayerData> getWorldPlayerData(WorldProperties worldProperties, UUID playerUniqueId);

    /**
     * Gets an immutable list of all player {@link Claim}'s.
     * 
     * Note: This will return an empty list if player has no claims.
     * 
     * @param playerUniqueId The player UUID
     * @return An immutable list of all claims, empty list if none were found
     */
    List<Claim> getAllPlayerClaims(UUID playerUniqueId);

    /**
     * Gets the claim manager of world.
     * 
     * @param worldProperties
     * @return The claim manager of world
     */
    ClaimManager getClaimManager(WorldProperties worldProperties);

    /**
     * Creates a claim builder.
     * 
     * @return The claim builder
     */
    Claim.Builder createClaimBuilder();

    /**
     * Creates a bank transaction builder.
     * 
     * @return The bank transaction builder
     */
    BankTransaction.Builder createBankTransactionBuilder();
}
