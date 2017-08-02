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
package me.ryanhamshire.griefprevention.economy;

import static com.google.common.base.Preconditions.checkNotNull;

import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.economy.BankTransaction;
import me.ryanhamshire.griefprevention.api.economy.BankTransactionType;
import org.spongepowered.api.event.cause.Cause;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class GPBankTransaction implements BankTransaction {

    public final BankTransactionType type;
    public final UUID source;
    public final Instant timestamp;
    public final double amount;

    public GPBankTransaction(Claim claim, BankTransactionType type, Instant timestamp, double amount) {
        this(type, timestamp, amount);
    }

    public GPBankTransaction(BankTransactionType type, UUID source, Instant timestamp, double amount) {
        this.type = type;
        this.timestamp = timestamp;
        this.amount = amount;
        this.source = source;
    }

    public GPBankTransaction(BankTransactionType type, Instant timestamp, double amount) {
        this.type = type;
        this.timestamp = timestamp;
        this.amount = amount;
        this.source = null;
    }

    @Override
    public BankTransactionType getType() {
        return this.type;
    }

    @Override
    public Optional<UUID> getSource() {
        return Optional.ofNullable(this.source);
    }

    @Override
    public Instant getTimestamp() {
        return this.timestamp;
    }

    @Override
    public Double getAmount() {
        return this.amount;
    }

    public static class BankTransactionBuilder implements Builder {

        private Cause cause;
        private Claim claim;
        private UUID source;
        private Instant timestamp;
        private double amount;
        private BankTransactionType type;

        @Override
        public Builder cause(Cause cause) {
            this.cause = cause;
            return this;
        }

        @Override
        public Builder type(BankTransactionType type) {
            this.type = type;
            return this;
        }

        @Override
        public Builder source(UUID source) {
            this.source = source;
            return this;
        }

        @Override
        public Builder amount(double amount) {
            this.amount = amount;
            return this;
        }

        @Override
        public Builder reset() {
            this.cause = null;
            this.source = null;
            this.amount = 0;
            this.type = null;
            return this;
        }

        @Override
        public BankTransaction build() {
            checkNotNull(this.type);
            checkNotNull(this.cause);
            checkNotNull(this.amount);
            if (this.source != null) {
                return new GPBankTransaction(this.type, this.source, Instant.now(), this.amount); 
            }
            return new GPBankTransaction(this.type, Instant.now(), this.amount); 
        }
        
    }
}
