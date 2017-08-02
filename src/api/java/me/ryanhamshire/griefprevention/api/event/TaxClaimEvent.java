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

/**
 * An event that is fired before a claim is taxed.
 * 
 * Note: Canceling this event will prevent claim being taxed.
 */
public interface TaxClaimEvent extends ClaimEvent {

    /**
     * Gets the original tax rate for claim.
     * 
     * @return The original tax rate
     */
    double getOriginalTaxRate();

    /**
     * Gets the original tax amount for claim.
     * 
     * @return The original tax amount
     */
    double getOriginalTaxAmount();

    /**
     * Gets the current tax rate to be applied to claim.
     * 
     * @return The current tax rate
     */
    double getTaxRate();

    /**
     * Gets the current total tax amount to be applied to claim.
     * 
     * Note: To adjust the tax amount, use {{@link #setTaxRate(double)}
     * 
     * @return The current total tax amount
     */
    double getTaxAmount();

    /**
     * Sets a new tax rate for claim.
     * 
     * @param newTaxRate The new tax rate.
     */
    void setTaxRate(double newTaxRate);
}
