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
package me.ryanhamshire.griefprevention.configuration.type;

import me.ryanhamshire.griefprevention.configuration.category.DatabaseCategory;
import me.ryanhamshire.griefprevention.configuration.category.LoggingCategory;
import me.ryanhamshire.griefprevention.configuration.category.MigratorCategory;
import me.ryanhamshire.griefprevention.configuration.category.PlayerDataCategory;
import me.ryanhamshire.griefprevention.configuration.category.SpamCategory;
import ninja.leaping.configurate.objectmapping.Setting;

public class GlobalConfig extends ConfigBase {

    @Setting
    public DatabaseCategory database = new DatabaseCategory();
    @Setting
    public LoggingCategory logging = new LoggingCategory();
    @Setting
    public PlayerDataCategory playerdata = new PlayerDataCategory();
    @Setting
    public SpamCategory spam = new SpamCategory();
    @Setting(comment = 
            "List of migrators that convert other protection data into GP claim data." + 
            "\nNote: These migrators will NOT change or delete your data. It simply reads and creates new data for GriefPrevention.")
    public MigratorCategory migrator = new MigratorCategory();
}
