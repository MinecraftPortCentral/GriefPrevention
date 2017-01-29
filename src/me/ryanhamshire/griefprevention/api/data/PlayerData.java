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

import me.ryanhamshire.griefprevention.api.claim.ClaimType;

public interface PlayerData {

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
     * @param The claim type to check
     * @return The maximum size of x for claim creation
     */
    int getMaxClaimX(ClaimType type);

    /**
     * Gets the max size limit of y, in blocks, for claim
     * creation.
     * 
     * Note: This option is ignored for 2D claims (non-cuboids).
     * 
     * @param The claim type to check
     * @return The maximum size of y for claim creation
     */
    int getMaxClaimY(ClaimType type);

    /**
     * Gets the max size limit of z, in blocks, for claim
     * creation.
     * 
     * @param The claim type to check
     * @return The maximum size of z for claim creation
     */
    int getMaxClaimZ(ClaimType type);

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
     * Gets whether this player is currently in cuboid mode.
     * If set to true, all newly created claims will be 3D.
     * 
     * @return If player is in cuboid mode
     */
    boolean getCuboidMode();

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
     * Toggles whether player is in cuboid creation mode.
     * 
     * @param cuboidMode
     */
    void setCuboidMode(boolean cuboidMode);
}
