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
public class GeneralCategory extends ConfigCategory {

    @Setting(value = "admin-sign-notifications", comment = "Enable sign notifications for admins.")
    public boolean generalAdminSignNotifications = false;
    @Setting(value = "admin-whisper-notifications", comment = "Enable whisper notifications for admins.")
    public boolean generalAdminWhisperNotifications = false;
    @Setting(value = "initial-claim-blocks", comment = "The number of claim blocks a new player starts with.")
    public int claimInitialBlocks = 100;
    @Setting(value = "limit-pistons-to-claims",
            comment = "Whether pistons are limited to only move blocks located within the piston's land claim.")
    public boolean limitPistonsToClaims = false;
    @Setting(value = "limit-sky-trees", comment = "Whether players can build trees on platforms in the sky.")
    public boolean allowSkyTrees = true;
    @Setting(value = "limit-tree-growth", comment = "Whether trees should be prevented from growing into a claim from outside.")
    public boolean limitTreeGrowh = false;
    @Setting(value = "max-players-per-ip", comment = "How many players can share an IP address.")
    public int sharedIpLimit = 3;
    @Setting(value = "smart-ban", comment = "Whether to ban accounts which very likely owned by a banned player.")
    public boolean smartBan = false;
    @Setting(value = "admin-whispers", comment = "Whether whispered messages will broadcast to administrators in game.")
    public boolean broadcastWhisperedMessagesToAdmins = false;
    @Setting(value = "admin-whisper-commands", comment = "List of whisper commands to eavesdrop on.")
    public List<String> whisperCommandList = new ArrayList<>();
    @Setting(value = "protect-item-drops-death-non-pvp", comment = "Whether players' dropped on death items are protected in non-pvp worlds.")
    public boolean protectItemsOnDeathNonPvp = true;
    @Setting(value = "chat-rules-enabled", comment = "Whether chat should be monitored and filtered by rules.")
    public boolean chatProtectionEnabled = false;
}