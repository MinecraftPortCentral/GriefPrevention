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

import org.spongepowered.api.world.World;

/**
 * Represents a type of claim.
 */
public enum ClaimType {

    /**
     * Represents a claim type that is managed by admins.
     */
    ADMIN,
    /**
     * Represents a claim type that is managed by one or more players.
     */
    BASIC,
    /**
     * Represents a sub-claim type that can be one or more blocks.
     * 
     * Note: This type is not supported in {@link #WILDERNESS}
     */
    SUBDIVISION,
    /**
     * Represents a claim type that supports multiple levels of {@link #BASIC}
     * claims.
     */
    TOWN,
    /**
     * Represents all the block space in a single {@link World} not
     * used by any other claim type. There will only be one wilderness
     * claim per world.
     */
    WILDERNESS
}
