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
import me.ryanhamshire.griefprevention.api.data.ClaimData;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.event.entity.TargetEntityEvent;
import org.spongepowered.api.text.Text;

import java.util.Optional;

import javax.annotation.Nullable;

public interface BorderClaimEvent extends ClaimEvent, TargetEntityEvent {

    /**
     * Gets the claim the {@link Entity} is attempting to exit.
     * 
     * @return The source claim
     */
    Claim getExitClaim();

    /**
     * Gets the claim the {@link Entity} is attempting to enter.
     * 
     * @return The destination claim
     */
    default Claim getEnterClaim() {
        return this.getClaim();
    }

    /**
     * Gets the event enter message if available.
     * 
     * Note: If no message is set, the {@link ClaimData#getGreeting()}
     * will be used.
     */
    Optional<Text> getEnterMessage();

    /**
     * Gets the event exit message if available.
     * 
     * Note: If no message is set, the {@link ClaimData#getFarewell()}
     * will be used.
     */
    Optional<Text> getExitMessage();

    /**
     * Sets the enter message for this event only.
     * 
     * Note: Setting message to {@code null} will hide the message.
     * If no message is set, the {@link ClaimData#getGreeting()} will be used.
     * 
     * @param message The message to set
     */
    void setEnterMessage(@Nullable Text message);

    /**
     * Sets the exit message for this event only.
     * 
     * Note: Setting message to {@code null} will hide the message.
     * If no message is set, the {@link ClaimData#getFarewell()} will be used.
     * 
     * @param message The message to set
     */
    void setExitMessage(@Nullable Text message);
}