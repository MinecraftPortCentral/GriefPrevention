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
package me.ryanhamshire.griefprevention.permission;

import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;

import java.util.HashSet;
import java.util.Set;

public class GPOptionHandler {

    public static Double getClaimOptionDouble(Subject subject, Claim claim, GPOptions.Type type, GPPlayerData playerData) {
        if (claim == null) {
            return 0.0;
        }

        final String claimOption = checkClaimOption(type, claim);
        if (claimOption.equals(GPOptions.INVALID_OPTION)) {
            return 0.0;
        }
        return getClaimOptionDouble(subject, claim, claimOption, playerData);
    }

    public static Double getClaimOptionDouble(Subject subject, Claim claim, String option, GPPlayerData playerData) {
        if (claim == null) {
            if (playerData.lastShovelLocation != null) {
                GPClaim parentClaim = GriefPreventionPlugin.instance.dataStore.getClaimAt(playerData.lastShovelLocation);
                claim = parentClaim;
            } else {
                return null;
            }
        }

        if (!option.startsWith("griefprevention.")) {
            option = "griefprevention." + option;
        }

        final double adminValue = playerData.optionMap.get(option);
        Set<Context> contexts = new HashSet<>();
        contexts.add(claim.getContext());
        String optionValueStr = subject.getOption(contexts, option).orElse(null);

        if (optionValueStr == null) {
            if (subject != GriefPreventionPlugin.GLOBAL_SUBJECT) {
                optionValueStr = GriefPreventionPlugin.GLOBAL_SUBJECT.getOption(contexts, option).orElse(null);
                if (optionValueStr == null) {
                    return adminValue;
                }
            } else {
                return adminValue;
            }
        }

        Double optionValue = null;
        try {
            optionValue = Double.parseDouble(optionValueStr);
        } catch (NumberFormatException e) {

        }
        if (optionValue != null) {
            if (optionValue > adminValue) {
                return adminValue;
            }
        } else if (subject != GriefPreventionPlugin.GLOBAL_SUBJECT) {
            Double defaultValue = getClaimOptionDouble(GriefPreventionPlugin.GLOBAL_SUBJECT, claim, option, playerData);
            return defaultValue;
        }

        return optionValue;
    }

    private static String checkClaimOption(GPOptions.Type type, Claim claim) {
        if (claim.isAdminClaim()) {
            return GPOptions.INVALID_OPTION;
        }

        switch (type) {
            case MAX_CLAIM_SIZE_X :
                if (claim.isTown()) {
                    return GPOptions.MAX_CLAIM_SIZE_TOWN_X;
                }
                if (claim.isSubdivision()) {
                    return GPOptions.MAX_CLAIM_SIZE_SUBDIVISION_X;
                }
                return GPOptions.MAX_CLAIM_SIZE_BASIC_X;
            case MAX_CLAIM_SIZE_Y :
                if (claim.isTown()) {
                    return GPOptions.MAX_CLAIM_SIZE_TOWN_Y;
                }
                if (claim.isSubdivision()) {
                    return GPOptions.MAX_CLAIM_SIZE_SUBDIVISION_Y;
                }
                return GPOptions.MAX_CLAIM_SIZE_BASIC_Y;
            case MAX_CLAIM_SIZE_Z :
                if (claim.isTown()) {
                    return GPOptions.MAX_CLAIM_SIZE_TOWN_Z;
                }
                if (claim.isSubdivision()) {
                    return GPOptions.MAX_CLAIM_SIZE_SUBDIVISION_Z;
                }
                return GPOptions.MAX_CLAIM_SIZE_BASIC_Z;
            case MIN_CLAIM_SIZE_X :
                if (claim.isTown()) {
                    return GPOptions.MIN_CLAIM_SIZE_TOWN_X;
                }
                if (claim.isSubdivision()) {
                    return GPOptions.INVALID_OPTION;
                }
                return GPOptions.MIN_CLAIM_SIZE_BASIC_X;
            case MIN_CLAIM_SIZE_Y :
                if (claim.isTown()) {
                    return GPOptions.MIN_CLAIM_SIZE_TOWN_Y;
                }
                if (claim.isSubdivision()) {
                    return GPOptions.INVALID_OPTION;
                }
                return GPOptions.MIN_CLAIM_SIZE_BASIC_Y;
            case MIN_CLAIM_SIZE_Z :
                if (claim.isTown()) {
                    return GPOptions.MIN_CLAIM_SIZE_TOWN_Z;
                }
                if (claim.isSubdivision()) {
                    return GPOptions.INVALID_OPTION;
                }
                return GPOptions.MIN_CLAIM_SIZE_BASIC_Z;
            case CLAIM_LIMIT :
                if (claim.isTown()) {
                    return GPOptions.CREATE_CLAIM_LIMIT_TOWN;
                }
                if (claim.isSubdivision()) {
                    return GPOptions.CREATE_CLAIM_LIMIT_SUBDIVISION;
                }
                return GPOptions.CREATE_CLAIM_LIMIT_BASIC;
            case EXPIRATION_DAYS_KEEP :
                if (claim.isTown()) {
                    return GPOptions.TAX_EXPIRATION_TOWN_DAYS_KEEP;
                }
                if (claim.isSubdivision()) {
                    return GPOptions.TAX_EXPIRATION_SUBDIVISION_DAYS_KEEP;
                }
                return GPOptions.TAX_EXPIRATION_BASIC_DAYS_KEEP;
            case TAX_EXPIRATION :
                if (claim.isTown()) {
                    return GPOptions.TAX_EXPIRATION_TOWN;
                }
                if (claim.isSubdivision()) {
                    return GPOptions.TAX_EXPIRATION_SUBDIVISION;
                }
                return GPOptions.TAX_EXPIRATION_BASIC;
            case TAX_RATE :
                if (claim.isTown()) {
                    return GPOptions.TAX_RATE_TOWN;
                }
                if (claim.isInTown()) {
                    if (claim.isSubdivision()) {
                        return GPOptions.TAX_RATE_TOWN_SUBDIVISION;
                    }
                    return GPOptions.TAX_RATE_TOWN_BASIC;
                }
                if (claim.isSubdivision()) {
                    return GPOptions.TAX_RATE_SUBDIVISION;
                }
                return GPOptions.TAX_RATE_BASIC;

            default :
                return null;
        }
    }
}
