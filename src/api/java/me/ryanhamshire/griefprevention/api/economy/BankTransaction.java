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
package me.ryanhamshire.griefprevention.api.economy;

import me.ryanhamshire.griefprevention.GriefPrevention;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Contains information of a bank transaction in a claim.
 */
public interface BankTransaction {

    /**
     * Gets the {@link BankTransactionType}.
     * 
     * @return The type
     */
    BankTransactionType getType();

    /**
     * Gets the source of this transaction.
     * 
     * @return The source, if available
     */
    Optional<UUID> getSource();

    /**
     * Gets the timestamp of this transaction.
     * 
     * @return The timestamp
     */
    Instant getTimestamp();

    /**
     * Gets the amount of the this transaction.
     * 
     * @return The amount
     */
    Double getAmount();

    /**
     * Gets a new claim builder instance for {@link Builder}.
     * 
     * @return A new claim builder instance
     */
    public static BankTransaction.Builder builder() {
        return GriefPrevention.getApi().createBankTransactionBuilder();
    }

    public interface Builder {

        Builder type(BankTransactionType type);

        Builder source(UUID source);

        Builder amount(double amount);

        Builder reset();

        BankTransaction build();
    }
}
