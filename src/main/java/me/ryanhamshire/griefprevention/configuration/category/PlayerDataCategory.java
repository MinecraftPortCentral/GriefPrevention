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

@ConfigSerializable
public class PlayerDataCategory extends ConfigCategory {

    @Setting(value = "use-global-storage", comment = "Whether player data should be stored globally. False will store all data per world.")
    public boolean useGlobalPlayerDataStorage = true;
    @Setting(value = "wilderness-cuboids", comment = "Whether to allow 3d cuboids to be created in wilderness.\nIf set to true, claim blocks will use the chunk count system to balance 3d claiming."
            + "\nIf false, the standard 2d block count system will be used and 3d claims created in wilderness by admins will be free."
            + "\nNote: Setting this to false will still allow users to create sub 3D claims in their 2D claims.")
    public boolean wildernessCuboids = false;
    @Setting(value = "migration-3d-rate", comment = "The rate to multiply each accrued claim blocks total by."
            + "\nSet to a value greater than -1 to enable. (Default: 256)."
            + "\nNote: This is only run one time to migrate player claimblock data to new system."
            + "\nEach chunk is worth 65,536 blocks in the new system compared to 256 in old."
            + "\nNote: This requires 'wilderness-cuboids' to be enabled.")
    public int migration3dRate = -1;
    @Setting(value = "migration-2d-rate", comment = "The rate to divide each accrued claim blocks total by."
            + "\nSet to a value greater than -1 to enable. (Default: 256)."
            + "\nNote: This should only be used when migrating from the chunk count system back to classic 2d."
            + "\nIn this system, a chunk costs 256 blocks."
            + "\nNote: This requires 'wilderness-cuboids' to be disabled.")
    public int migration2dRate = -1;
    @Setting(value = "reset-migrations", comment = "If enabled, resets all playerdata migration flags to allow for another migration."
            + "\nNote: Use this with caution as it can easily mess up claim block data. It is highly recommended to backup before using.")
    public boolean resetMigrations = false;
    @Setting(value = "reset-claim-block-data", comment = "If enabled, resets all playerdata claimblock data with value specified."
            + "\nExample: If set to 100, every player will start with 100 claim blocks."
            + "\nNote: This will all reset bonus claim blocks to 0. It is highly recommended to backup before using.")
    public int resetClaimBlockData = -1;
}