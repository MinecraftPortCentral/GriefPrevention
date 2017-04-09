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
package me.ryanhamshire.griefprevention.api.event;

import me.ryanhamshire.griefprevention.api.claim.Claim;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public interface ResizeClaimEvent extends ClaimEvent {

    /**
     * The start corner location for resize.
     * 
     * @return The start location
     */
    Location<World> getStartCorner();

    /**
     * The end corner location to resize to.
     * 
     * @return The end corner location
     */
    Location<World> getEndCorner();

    /**
     * The attempted resized claim.
     * 
     * Note: The original claim is only resized if event isn't cancelled.
     * This claim just represents a temporary one before final checks.
     * 
     * @return The resized claim
     */
    Claim getResizedClaim();
}
