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

import com.google.common.collect.ImmutableSet;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimFlag;
import me.ryanhamshire.griefprevention.api.event.FlagClaimEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.util.Tristate;

import java.util.Optional;

public class GPFlagClaimEvent extends GPClaimEvent implements FlagClaimEvent {

    private final Subject subject;

    public GPFlagClaimEvent(Claim claim, Subject subject, Cause cause) {
        super(claim, cause);
        this.subject = subject;
    }

    public static class Clear extends GPFlagClaimEvent implements FlagClaimEvent.Clear {

        private final java.util.Set<Context> contexts;

        public Clear(Claim claim, Subject subject, java.util.Set<Context> contexts, Cause cause) {
            super(claim, subject, cause);
            this.contexts = ImmutableSet.copyOf(contexts);
        }

        @Override
        public java.util.Set<Context> getContexts() {
            return this.contexts;
        }
    }

    public static class Set extends GPFlagClaimEvent implements FlagClaimEvent.Set {

        private final ClaimFlag flag;
        private final Tristate value;
        private final String source;
        private final String target;
        private final Context context;

        public Set(Claim claim, Subject subject, ClaimFlag flag, String source, String target, Tristate value, Context context, Cause cause) {
            super(claim, subject, cause);
            this.flag = flag;
            this.source = source;
            this.target = target;
            this.value = value;
            this.context = context;
        }

        @Override
        public ClaimFlag getFlag() {
            return this.flag;
        }

        @Override
        public Optional<String> getSource() {
            return Optional.ofNullable(this.source);
        }

        @Override
        public String getTarget() {
            return this.target;
        }

        @Override
        public Tristate getValue() {
            return this.value;
        }

        @Override
        public Context getPermissionContext() {
            return this.context;
        }
    }

    @Override
    public Subject getSubject() {
        return this.subject;
    }
}
