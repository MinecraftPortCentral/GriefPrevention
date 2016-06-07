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
package me.ryanhamshire.griefprevention.configuration;

import com.google.common.collect.Maps;
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.configuration.category.ClaimDataFlagsCategory;
import me.ryanhamshire.griefprevention.configuration.category.ConfigCategory;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.text.Text;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ConfigSerializable
public class ClaimDataConfig extends ConfigCategory implements IClaimData {

    private boolean requiresSave = true;

    @Setting(value = ClaimStorageData.MAIN_WORLD_UUID, comment = "The world uuid associated with claim.")
    private UUID worldUniqueId;
    @Setting(value = ClaimStorageData.MAIN_OWNER_UUID, comment = "The owner uuid assocated with claim.")
    private UUID ownerUniqueId;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_TYPE, comment = "The type of claim.")
    private Claim.Type claimType = Claim.Type.BASIC;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_DATE_CREATED, comment = "The date and time this claim was created.")
    private String dateCreated = Instant.now().toString();
    @Setting(value = ClaimStorageData.MAIN_CLAIM_DATE_LAST_ACTIVE, comment = "The last date and time this claim was active.")
    private String dateLastActive = Instant.now().toString();
    @Setting(value = ClaimStorageData.MAIN_CLAIM_NAME, comment = "The name associated with claim.")
    private Text claimName = Text.of("");
    @Setting(value = ClaimStorageData.MAIN_CLAIM_GREETING, comment = "The greeting message players will receive when entering claim area.")
    private Text claimGreetingMessage = Text.of("");
    @Setting(value = ClaimStorageData.MAIN_CLAIM_FAREWELL, comment = "The farewell message players will receive when leaving claim area.")
    private Text claimFarewellMessage = Text.of("");
    @Setting(value = ClaimStorageData.MAIN_LESSER_BOUNDARY_CORNER, comment = "The lesser boundary corner location of claim.")
    private String lesserBoundaryCornerPos;
    @Setting(value = ClaimStorageData.MAIN_GREATER_BOUNDARY_CORNER, comment = "The greater boundary corner location of claim.")
    private String greaterBoundaryCornerPos;
    @Setting(value = ClaimStorageData.MAIN_ACCESSORS, comment = "The accessors associated with claim. Note: Accessors can interact with all blocks except inventory containers like chests.")
    private List<UUID> accessors = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_BUILDERS, comment = "The builders associated with claim. Note: Builders can do everything accessors and containers do with the addition of placing and breaking blocks.")
    private List<UUID> builders = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_CONTAINERS, comment = "The containers associated with claim. Note: Containers can do everything accessors with the addition of inventory access.")
    private List<UUID> containers = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_COOWNERS, comment = "The coowners associated with claim. Note: Use this type with caution as it provides full access in claim.")
    private List<UUID> coowners = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_PROTECTION_BLACKLIST, comment = "Item id's that are not protected within claim.")
    private List<String> protectionBlacklist = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_BANNED_ITEM_LIST, comment = "Item id's that are banned from use in claim.")
    private List<String> bannedItemList = new ArrayList<>();
    @Setting
    private ClaimDataFlagsCategory flags = new ClaimDataFlagsCategory();
    @Setting
    private Map<UUID, SubDivisionDataConfig> subdivisions = Maps.newHashMap();

    public ClaimDataConfig() {

    }

    public UUID getWorldUniqueId() {
        return this.worldUniqueId;
    }

    public UUID getOwnerUniqueId() {
        return this.ownerUniqueId;
    }

    public void setClaimOwnerUniqueId(UUID newClaimOwner) {
        this.ownerUniqueId = newClaimOwner;
    }

    public void setWorldUniqueId(UUID uuid) {
        this.worldUniqueId = uuid;
    }

    public Claim.Type getClaimType() {
        return this.claimType;
    }

    public String getDateCreated() {
        return this.dateCreated;
    }

    public String getDateLastActive() {
        return this.dateLastActive;
    }

    public Text getClaimName() {
        return this.claimName;
    }

    public Text getGreetingMessage() {
        return this.claimGreetingMessage;
    }

    public Text getFarewellMessage() {
        return this.claimFarewellMessage;
    }

    public String getLesserBoundaryCorner() {
        return this.lesserBoundaryCornerPos;
    }

    public String getGreaterBoundaryCorner() {
        return this.greaterBoundaryCornerPos;
    }

    public List<UUID> getAccessors() {
        return this.accessors;
    }

    public List<UUID> getBuilders() {
        return this.builders;
    }

    public List<UUID> getContainers() {
        return this.containers;
    }

    public List<UUID> getCoowners() {
        return this.coowners;
    }

    public List<String> getProtectionBlackList() {
        return this.protectionBlacklist;
    }

    public List<String> getBannedItemList() {
        return this.bannedItemList;
    }

    public ClaimDataFlagsCategory getFlags() {
        return this.flags;
    }

    public void setClaimType(Claim.Type type) {
        this.requiresSave = true;
        this.claimType = type;
    }

    public void setDateLastActive(String date) {
        this.requiresSave = true;
        this.dateLastActive = date;
    }

    public void setClaimName(Text name) {
        this.requiresSave = true;
        this.claimName = name;
    }

    public void setGreetingMessage(Text message) {
        this.requiresSave = true;
        this.claimGreetingMessage = message;
    }

    public void setFarewellMessage(Text message) {
        this.requiresSave = true;
        this.claimFarewellMessage = message;
    }

    public void setLesserBoundaryCorner(String location) {
        this.requiresSave = true;
        this.lesserBoundaryCornerPos = location;
    }

    public void setGreaterBoundaryCorner(String location) {
        this.requiresSave = true;
        this.greaterBoundaryCornerPos = location;
    }

    public void setAccessors(List<UUID> accessors) {
        this.requiresSave = true;
        this.accessors = accessors;
    }

    public void setBuilders(List<UUID> builders) {
        this.requiresSave = true;
        this.builders = builders;
    }

    public void setContainers(List<UUID> containers) {
        this.requiresSave = true;
        this.containers = containers;
    }

    public void setCoowners(List<UUID> coowners) {
        this.requiresSave = true;
        this.coowners = coowners;
    }

    public void setFlags(ClaimDataFlagsCategory flags) {
        this.requiresSave = true;
        this.flags = flags;
    }

    public Map<UUID, SubDivisionDataConfig> getSubdivisions() {
        return this.subdivisions;
    }

    public boolean requiresSave() {
        return this.requiresSave;
    }

    public void setRequiresSave(boolean flag) {
        this.requiresSave = flag;
    }
}