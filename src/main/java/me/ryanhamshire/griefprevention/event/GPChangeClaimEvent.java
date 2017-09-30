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
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.api.event.ChangeClaimEvent;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class GPChangeClaimEvent extends GPClaimEvent implements ChangeClaimEvent {

    public GPChangeClaimEvent(Claim claim) {
        super(claim);
    }

    public static class Type extends GPChangeClaimEvent implements ChangeClaimEvent.Type {
        private final ClaimType originalType;
        private final ClaimType newType;
    
        public Type(Claim claim, ClaimType newType) {
            super(claim);
            this.originalType = claim.getType();
            this.newType = newType;
        }
    
        @Override
        public ClaimType getOriginalType() {
            return originalType;
        }
    
        @Override
        public ClaimType getType() {
            return newType;
        }
    }

    public static class Resize extends GPChangeClaimEvent implements ChangeClaimEvent.Resize {
        private Claim resizedClaim;
        private Location<World> startCorner;
        private Location<World> endCorner;

        public Resize(Claim claim, Location<World> startCorner, Location<World> endCorner, Claim resizedClaim) {
            super(claim);
            this.resizedClaim = resizedClaim;
        }

        @Override
        public Location<World> getStartCorner() {
            return this.startCorner;
        }

        @Override
        public Location<World> getEndCorner() {
            return this.endCorner;
        }

        @Override
        public Claim getResizedClaim() {
            return this.resizedClaim;
        }
    }
}
