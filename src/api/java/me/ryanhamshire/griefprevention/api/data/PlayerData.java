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
package me.ryanhamshire.griefprevention.api.data;

import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;

import java.util.List;

public interface PlayerData {

    /**
     * Gets an immutable list of claims owned by this player.
     * 
     * @return The immutable list of owned claims, empty if none
     */
    List<Claim> getClaims();

    /**
     * Gets the abandon return ratio used when abandoning a claim.
     * 
     * @return The abandon return ratio
     */
    double getAbandonedReturnRatio();

    /**
     * Gets the total accrued claim blocks.
     * 
     * Note: This does not include initial claim blocks.
     * 
     * @return The total accrued claim blocks
     */
    int getAccruedClaimBlocks();

    /**
     * Gets the blocks accrued per hour.
     * 
     * @return The blocks accrued per hour
     */
    int getBlocksAccruedPerHour();

    /**
     * Gets the maximum amount of claim blocks a player
     * can hold.
     * 
     * @return The maximum amount of claim blocks
     */
    int getMaxAccruedClaimBlocks();

    /**
     * Gets the max size limit of x, in blocks, for claim
     * creation.
     * 
     * @param type The claim type to check
     * @return The maximum size of x for claim creation
     */
    int getMaxClaimX(ClaimType type);

    /**
     * Gets the max size limit of y, in blocks, for claim
     * creation.
     * 
     * Note: This option is ignored for 2D claims.
     * 
     * @param type The claim type to check
     * @return The maximum size of y for claim creation
     */
    int getMaxClaimY(ClaimType type);

    /**
     * Gets the max size limit of z, in blocks, for claim
     * creation.
     * 
     * @param type The claim type to check
     * @return The maximum size of z for claim creation
     */
    int getMaxClaimZ(ClaimType type);

    /**
     * Gets the min size limit of x, in blocks, for claim
     * creation.
     * 
     * @param type The claim type to check
     * @return The minimum size of x for claim creation
     */
    int getMinClaimX(ClaimType type);

    /**
     * Gets the min size limit of y, in blocks, for claim
     * creation.
     * 
     * Note: This option is ignored for 2D claims.
     * 
     * @param type The claim type to check
     * @return The minimum size of y for claim creation
     */
    int getMinClaimY(ClaimType type);

    /**
     * Gets the min size limit of z, in blocks, for claim
     * creation.
     * 
     * @param type The claim type to check
     * @return The minimum size of z for claim creation
     */
    int getMinClaimZ(ClaimType type);

    /**
     * Gets the total bonus claim blocks.
     * 
     * @return The total bonus claim blocks
     */
    int getBonusClaimBlocks();

    /**
     * Gets the amount of days for this player's
     * auto-created chest claim's to expire.
     * 
     * @return The amount of days for chest claims to expire
     */
    int getChestClaimExpiration();

    /**
     * Gets the claim creation mode for player on login.
     * 
     * Note: 0 is for 2D cuboids, 1 is for 3D cuboids
     * 
     * @return The claim creation mode
     */
    int getClaimCreateMode();

    /**
     * Gets the max amount of claims this player can create.
     * 
     * @return The max create claim limit
     */
    int getCreateClaimLimit();

    /**
     * Gets the initial claim blocks.
     * 
     * @return The initial claim blocks
     */
    int getInitialClaimBlocks();

    /**
     * Gets the remaining claim blocks.
     * 
     * @return The remaining claim blocks
     */
    int getRemainingClaimBlocks();

    /**
     * Checks if this player can ignore a claim.
     * 
     * @param claim The claim to check
     * @return Whether this player can ignore the claim
     */
    boolean canIgnoreClaim(Claim claim);

    /**
     * Gets whether this player is currently in cuboid mode.
     * If set to true, all newly created claims will be 3D.
     * 
     * @return If player is in cuboid mode
     */
    @Deprecated
    default boolean getCuboidMode() {
        if (this.getClaimCreateMode() == 1) {
            return true;
        }

        return false;
    }

    /**
     * Sets the total amount of accrued claim blocks.
     * 
     * @param blocks The new total of accrued claim blocks
     */
    boolean setAccruedClaimBlocks(int blocks);

    /**
     * Sets the total amount of bonus claim blocks.
     * 
     * @param blocks The new total of bonus claim blocks
     */
    void setBonusClaimBlocks(int blocks);

    /**
     * Sets the claim create mode for player on login.
     * 
     * Note: 0 is for 2D cuboids, 1 is for 3D cuboids
     * 
     * @param mode
     */
    void setClaimCreateMode(int mode);

    /**
     * Toggles whether player is in cuboid creation mode.
     * 
     * @param cuboidMode
     */
    @Deprecated
    default void setCuboidMode(boolean cuboidMode) {
        if (cuboidMode) {
            this.setClaimCreateMode(1);
        } else {
            this.setClaimCreateMode(0);
        }
    }
}
