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

/**
 * 
 */
public enum ClaimResultType {

    /**
     * Returns no claim in result.
     */
    CLAIM_ALREADY_EXISTS,
    CLAIM_NOT_FOUND,
    CLAIMS_DISABLED,
    EXCEEDS_MAX_SIZE_X,
    EXCEEDS_MAX_SIZE_Y,
    EXCEEDS_MAX_SIZE_Z,
    INSUFFICIENT_CLAIM_BLOCKS,
    REQUIRES_OWNER,
    WRONG_CLAIM_TYPE,

    /**
     * Returns a list of overlapping claims.
     * 
     * See {@link ClaimResult#getClaims()}
     */
    OVERLAPPING_CLAIM,

    /**
     * Returns parent claim in result.
     */
    PARENT_CLAIM_MISMATCH,

    /**
     * Returns one or more claims that were cancelled.
     * 
     * Note: If deleting a claim with subdivisions, this may
     * return a list of one or more subdivisions in result that
     * could not be deleted due to event cancellations.
     */
    CLAIM_EVENT_CANCELLED,

    /**
     * Returns successful claim in result.
     * 
     * <p>Examples of results:</p>
     *
     * <ul>
     *     <li>Created claim</li>
     *     <li>Deleted claim</li>
     *     <li>Resized claim</li>
     *     <li>Transferred claim</li>
     * </ul>
     */
    SUCCESS
}
