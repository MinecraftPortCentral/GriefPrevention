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
public class SpamCategory extends ConfigCategory {

    @Setting(value = "enable-spam-monitor", comment = "Whether or not to monitor for spam.")
    public boolean monitorEnabled = true;
    @Setting(value = "login-cooldown", comment = "How long players must wait between logins. combats login spam.")
    public int loginCooldown = 60;
    @Setting(value = "autoban-offenders", comment = "Whether or not to ban spammers automatically.")
    public boolean autoBanOffenders = false;
    @Setting(value = "monitor-commands", comment = "the list of slash commands monitored for spam,")
    public List<String> monitoredCommandList = new ArrayList<>();
    @Setting(value = "allowed-ips", comment = "IP addresses which will not be censored.")
    public List<String> allowedIpAddresses = new ArrayList<>();
    @Setting(value = "death-message-cooldown", comment = "Cooldown period for death messages (per player) in seconds.")
    public int deathMessageCooldown = 60;
}