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

import me.ryanhamshire.griefprevention.configuration.category.BanCategory;
import me.ryanhamshire.griefprevention.configuration.category.LoggingCategory;
import me.ryanhamshire.griefprevention.configuration.category.MessageCategory;
import me.ryanhamshire.griefprevention.configuration.category.MigratorCategory;
import me.ryanhamshire.griefprevention.configuration.category.ModuleCategory;
import me.ryanhamshire.griefprevention.configuration.category.PlayerDataCategory;
import me.ryanhamshire.griefprevention.configuration.category.SpamCategory;
import me.ryanhamshire.griefprevention.configuration.category.ThreadCategory;
import ninja.leaping.configurate.objectmapping.Setting;

public class GlobalConfig extends ConfigBase {

    @Setting
    public BanCategory bans = new BanCategory();

    //@Setting
    //public DatabaseCategory database = new DatabaseCategory();

    @Setting
    public LoggingCategory logging = new LoggingCategory();

    @Setting
    public PlayerDataCategory playerdata = new PlayerDataCategory();

    @Setting
    public SpamCategory spam = new SpamCategory();

    @Setting
    public MessageCategory message = new MessageCategory();

    @Setting(comment = 
            "List of migrators that convert old or other protection data into the current GP claim data format." + 
            "\nNote: It is recommended to backup data before using.")
    public MigratorCategory migrator = new MigratorCategory();

    @Setting(value = "modules")
    public ModuleCategory modules = new ModuleCategory();

    @Setting
    public ThreadCategory thread = new ThreadCategory();
}
