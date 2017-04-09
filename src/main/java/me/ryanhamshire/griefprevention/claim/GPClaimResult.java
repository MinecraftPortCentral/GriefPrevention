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
package me.ryanhamshire.griefprevention.claim;

import com.google.common.collect.ImmutableList;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimResult;
import me.ryanhamshire.griefprevention.api.claim.ClaimResultType;
import org.spongepowered.api.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GPClaimResult implements ClaimResult {

    private final Text eventMessage;
    private final List<Claim> claims;
    private final ClaimResultType resultType;

    public GPClaimResult(ClaimResultType type) {
        this(type, null);
    }

    public GPClaimResult(ClaimResultType type, Text message) {
        this.claims = ImmutableList.of();
        this.resultType = type;
        this.eventMessage = message;
    }

    public GPClaimResult(Claim claim, ClaimResultType type) {
        this(claim, type, null);
    }

    public GPClaimResult(Claim claim, ClaimResultType type, Text message) {
        List<Claim> claimList = new ArrayList<>();
        claimList.add(claim);
        this.claims = ImmutableList.copyOf(claimList);
        this.resultType = type;
        this.eventMessage = message;
    }

    public GPClaimResult(List<Claim> claims, ClaimResultType type) {
        this(claims, type, null);
    }

    public GPClaimResult(List<Claim> claims, ClaimResultType type, Text message) {
        this.claims = ImmutableList.copyOf(claims);
        this.resultType = type;
        this.eventMessage = message;
    }

    @Override
    public ClaimResultType getResultType() {
        return this.resultType;
    }

    @Override
    public Optional<Text> getMessage() {
        return Optional.ofNullable(this.eventMessage);
    }

    @Override
    public List<Claim> getClaims() {
        return this.claims;
    }
}
