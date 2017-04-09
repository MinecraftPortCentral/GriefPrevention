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
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.text.Text;

import java.util.List;
import java.util.Optional;

public interface ClaimEvent extends Cancellable, Event {

    /**
     * Gets the immutable list of claims.
     * 
     * @return The immutable list of claims
     */
    List<Claim> getClaims();

    /**
     * Gets the first claim.
     * 
     * Note: This is a helper method for events that will usually always
     * contain one claim instead of calling {@link #getClaims}.
     * 
     * @return The first claim
     */
    default Claim getClaim() {
        return this.getClaims().get(0);
    }

    /**
     * Sets the claim message to be presented to {@link CommandSource}
     * if applicable.
     * 
     * @param message The message
     * @return The claim message to send to {@link CommandSource}
     */
    void setMessage(Text message);

    /**
     * Gets the claim message.
     * 
     * Note: This is only available if event was cancelled.
     * 
     * @return The claim message, if available
     */
    Optional<Text> getMessage();
}
