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
package me.ryanhamshire.griefprevention.event;

import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.TrustType;
import me.ryanhamshire.griefprevention.api.event.TrustClaimEvent;
import org.spongepowered.api.event.cause.Cause;

import java.util.List;
import java.util.UUID;

public class GPTrustClaimEvent extends GPClaimEvent implements TrustClaimEvent {

    private List<UUID> users;
    private TrustType trustType;

    public GPTrustClaimEvent(Claim claim, Cause cause, List<UUID> users, TrustType trustType) {
        super(claim, cause);
        this.users = users;
        this.trustType = trustType;
    }

    public GPTrustClaimEvent(List<Claim> claims, Cause cause, List<UUID> users, TrustType trustType) {
        super(claims, cause);
        this.users = users;
        this.trustType = trustType;
    }

    @Override
    public List<UUID> getUsers() {
        return this.users;
    }

    @Override
    public TrustType getTrustType() {
        return this.trustType;
    }

    public static class Add extends GPTrustClaimEvent implements TrustClaimEvent.Add {
        public Add(List<Claim> claims, Cause cause, List<UUID> users, TrustType trustType) {
            super(claims, cause, users, trustType);
        }

        public Add(Claim claim, Cause cause, List<UUID> users, TrustType trustType) {
            super(claim, cause, users, trustType);
        }
    }

    public static class Remove extends GPTrustClaimEvent implements TrustClaimEvent.Remove {
        public Remove(List<Claim> claims, Cause cause, List<UUID> users, TrustType trustType) {
            super(claims, cause, users, trustType);
        }

        public Remove(Claim claim, Cause cause, List<UUID> users, TrustType trustType) {
            super(claim, cause, users, trustType);
        }
    }
}
