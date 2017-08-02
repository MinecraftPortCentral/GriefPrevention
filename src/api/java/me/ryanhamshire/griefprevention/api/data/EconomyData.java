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
package me.ryanhamshire.griefprevention.api.data;

import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.economy.BankTransaction;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Contains all economy data of a {@link Claim}.
 */
public interface EconomyData {

    /**
     * Gets the bank transaction log.
     * 
     * @return The bank transaction log
     */
    List<String> getBankTransactionLog();

    /**
     * Gets the first instant tax was not paid.
     * 
     * @return The instant, if available
     */
    Optional<Instant> getTaxPastDueDate();

    /**
     * Gets the sale price of {@link Claim}.
     * 
     * @return The sale price, or -1 if not set.
     */
    double getSalePrice();

    /**
     * Gets whether claim is for sale.
     * 
     * @return whether claim is for sale
     */
    boolean isForSale();

    /**
     * Gets the current owed tax balance.
     * 
     * @return The tax balance
     */
    double getTaxBalance();

    /**
     * Clears the bank transaction log.
     */
    void clearBankTransactionLog();

    /**
     * Sets a sale price for {@link Claim}.
     * 
     * @param price
     */
    void setSalePrice(double price);

    /**
     * Enables or disables the sale of {@link Claim}.
     * 
     * @param forSale true to enable, false to disable
     */
    void setForSale(boolean forSale);

    /**
     * Sets the tax balance of {@link Claim}.
     * 
     * @param balance The new tax balance
     */
    void setTaxBalance(double balance);

    /**
     * Sets the new tax past due date.
     * 
     * @param date The new date
     */
    void setTaxPastDueDate(Instant date);

    /**
     * Adds a bank transaction to the {@link Claim} log.
     * 
     * @param transaction The bank transaction
     */
    void addBankTransaction(BankTransaction transaction);
}
