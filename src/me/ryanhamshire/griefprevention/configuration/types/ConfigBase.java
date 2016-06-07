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
package me.ryanhamshire.griefprevention.configuration.types;

import me.ryanhamshire.griefprevention.configuration.category.ClaimCategory;
import me.ryanhamshire.griefprevention.configuration.category.EconomyCategory;
import me.ryanhamshire.griefprevention.configuration.category.FlagDefaultsCategory;
import me.ryanhamshire.griefprevention.configuration.category.GeneralCategory;
import me.ryanhamshire.griefprevention.configuration.category.PvpCategory;
import me.ryanhamshire.griefprevention.configuration.category.SiegeCategory;
import ninja.leaping.configurate.objectmapping.Setting;

public abstract class ConfigBase {

    @Setting
    public ClaimCategory claim = new ClaimCategory();
    @Setting
    public EconomyCategory economy = new EconomyCategory();
    @Setting
    public FlagDefaultsCategory flags = new FlagDefaultsCategory();
    @Setting
    public GeneralCategory general = new GeneralCategory();
    @Setting
    public PvpCategory pvp = new PvpCategory();
    @Setting
    public SiegeCategory siege = new SiegeCategory();
}