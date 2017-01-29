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

import java.util.List;
import java.util.Optional;

import me.ryanhamshire.griefprevention.api.event.CreateClaimEvent;
import org.spongepowered.api.text.Text;

public interface ClaimResult {

    /**
     * Gets the {@link ClaimResultType} of claim action which adds
     * extra context when result is not a success.
     * 
     * @return The result type
     */
    ClaimResultType getResultType();

    /**
     * Gets the result message.
     * 
     * Note: normally this is set during event cancellations.
     * 
     * @return The result message, if available
     */
    Optional<Text> getMessage();

    /**
     * Gets an immutable list of resulting claims.
     * 
     * <p>Examples of list contents:</p>
     *
     * <ul>
     *     <li>Is empty for when an event such as {@link CreateClaimEvent} is cancelled</li>
     *     <li>Contains a single claim if {@link ClaimResultType#SUCCESS} is returned</li>
     *     <li>Contains one or more claims if {@link ClaimResultType#OVERLAPPING_CLAIM} is returned</li>
     * </ul>
     * 
     * Refer to {@link ClaimResultType} for complete list of return results.
     * 
     * @return The immutable list of resulting claims if available, otherwise an empty list
     */
    List<Claim> getClaims();

    /**
     * Gets the first resulting claim.
     * 
     * Note: This is a helper method for results that will usually always
     * contain one claim instead of calling {@link #getClaims}.
     * 
     * @return The result claim, if available
     */
    default Optional<Claim> getClaim() {
        List<Claim> claimList = this.getClaims();
        if (claimList.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(claimList.get(0));
    }

    /**
     * A simple helper method to determine if the {@link ClaimResult}
     * was a success.
     * 
     * @return Whether the claim result was a success
     */
    default boolean successful() {
        return this.getResultType() == ClaimResultType.SUCCESS;
    }
}
