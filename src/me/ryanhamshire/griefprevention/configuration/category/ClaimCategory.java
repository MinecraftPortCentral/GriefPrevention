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
package me.ryanhamshire.griefprevention.configuration.category;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public class ClaimCategory extends ConfigCategory {

    @Setting(value = "abandon-return-ratio", comment = "The portion of claim blocks returned to a player when a claim is abandoned.")
    public double abandonReturnRatio = 1.0;
    @Setting(value = "accesstrust-commands", comment = "The list of slashcommands requiring access trust when in a claim.")
    public List<String> accessTrustCommands = new ArrayList<>();
    @Setting(value = "auto-claim-radius", comment = "Radius used for auto-created claims")
    public int claimRadius = 4;
    @Setting(value = "banned-item-ids", comment = "Contains list of banned item ids on server.")
    public List<String> bannedItemIds = new ArrayList<>();
    @Setting(value = "blocks-accrued-per-hour", comment = "Blocks earned per hour.")
    public int claimBlocksEarned = 100;
    @Setting(value = "deliver-manuals", comment = "Send players manuals on claim creation.")
    public boolean deliverManuals = false;
    @Setting(value = "expiration-all-claim-days", comment = "How many days of inactivity before a player loses his claims.")
    public int daysInactiveClaimExpiration = 0;
    @Setting(value = "chest-claim-days", comment = "Number of days of inactivity before an automatic chest claim will be deleted.")
    public int daysInactiveChestClaimExpiration = 7;
    @Setting(value = "unused-claim-days", comment = "Number of days of inactivity before an unused claim will be deleted.")
    public int daysInactiveUnusedClaimExpiration = 14;
    @Setting(value = "auto-nature-restore", comment = "Whether survival claims will be automatically restored to nature when auto-deleted.")
    public boolean claimAutoNatureRestore = false;
    @Setting(value = "extend-into-ground-distance", comment = "How far below the shoveled block a new claim will reach.")
    public int extendIntoGroundDistance = 5;
    @Setting(value = "claim-max-depth", comment = "Limit on how deep claims can go.")
    public int maxClaimDepth = 0;
    @Setting(value = "investigation-tool", comment = "The item used to investigate claims with a right-click.")
    public String investigationTool = "minecraft:stick";
    @Setting(value = "max-accrued-blocks",
            comment = "The limit on accrued blocks (over time). doesn't limit purchased or admin-gifted blocks.")
    public int maxAccruedBlocks = 80000;
    @Setting(value = "max-claims-per-player", comment = "Maximum number of claims per player.")
    public int maxClaimsPerPlayer = 0;
    @Setting(value = "minimum-area", comment = "Minimum area for non-admin claims.")
    public int claimMinimumArea = 100;
    @Setting(value = "minimum-width", comment = "Minimum width for non-admin claims.")
    public int claimMinimumWidth = 5;
    @Setting(value = "modification-tool", comment = "The item used to create/resize claims with a right click.")
    public String modificationTool = "minecraft:golden_shovel";
    @Setting(value = "claims-mode",
            comment = "The mode used when creating claims. (0 = Disabled, 1 = Survival, 2 = SurvivalRequiringClaims, 3 = Creative)")
    public int claimMode = 1;
    @Setting(value = "fire-spreads-outside", comment = "Whether fire spreads outside of claims.")
    public boolean fireSpreadOutsideClaim = false;
    @Setting(value = "always-ignore-claims", comment = "List of player uuid's which ALWAYS ignore claims.")
    public List<String> alwaysIgnoreClaimsList = new ArrayList<>();
    @Setting(value = "ignored-entity-ids", comment = "List of entity id's that ignore protection. Format should be modid:name. For example, if you want creepers to ignore protection you would add minecraft:creeper.")
    public List<String> ignoredEntityIds = new ArrayList<>();
}