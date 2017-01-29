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

import me.ryanhamshire.griefprevention.api.claim.TrustType;

import java.util.List;
import java.util.UUID;

public interface TrustClaimEvent extends ClaimEvent {

    /**
     * Gets the list of {@link User}'s requesting trust.
     * 
     * @return The list of users requesting trust
     */
    List<UUID> getUsers();

    /**
     * The {@link TrustType} to grant user.
     * 
     * @return The trust type
     */
    TrustType getTrustType();

    /**
     * Fired when a user is added to claim trust.
     */
    interface Add extends TrustClaimEvent {};

    /**
     * Fired when a user is removed from claim trust.
     */
    interface Remove extends TrustClaimEvent {};
}
