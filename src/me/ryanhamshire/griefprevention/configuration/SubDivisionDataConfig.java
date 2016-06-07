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

import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.claim.Claim.Type;
import me.ryanhamshire.griefprevention.configuration.category.ClaimDataFlagsCategory;
import me.ryanhamshire.griefprevention.configuration.category.ConfigCategory;
import me.ryanhamshire.griefprevention.util.BlockUtils;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.text.Text;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ConfigSerializable
public class SubDivisionDataConfig extends ConfigCategory implements IClaimData {

    private boolean requiresSave = true;

    @Setting(value = ClaimStorageData.MAIN_CLAIM_NAME, comment = "The name associated with subdivision.")
    public Text claimName = Text.of("");
    @Setting(value = ClaimStorageData.MAIN_CLAIM_TYPE, comment = "The type of claim.")
    public Claim.Type claimType = Claim.Type.SUBDIVISION;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_DATE_CREATED, comment = "The date and time this subdivision was created.")
    public String dateCreated = Instant.now().toString();
    @Setting(value = ClaimStorageData.MAIN_CLAIM_DATE_LAST_ACTIVE, comment = "The last date and time this subdivision was used.")
    public String dateLastActive = Instant.now().toString();
    @Setting(value = ClaimStorageData.MAIN_CLAIM_GREETING, comment = "The greeting message players will receive when entering subdivision.")
    public Text claimGreetingMessage = Text.of("");
    @Setting(value = ClaimStorageData.MAIN_CLAIM_FAREWELL, comment = "The farewell message players will receive when leaving subdivision.")
    public Text claimFarewellMessage = Text.of("");
    @Setting(value = ClaimStorageData.MAIN_LESSER_BOUNDARY_CORNER, comment = "The lesser boundary corner location of subdivision.")
    public String lesserBoundaryCornerPos;
    @Setting(value = ClaimStorageData.MAIN_GREATER_BOUNDARY_CORNER, comment = "The greater boundary corner location of subdivision.")
    public String greaterBoundaryCornerPos;
    @Setting(value = ClaimStorageData.MAIN_ACCESSORS, comment = "The accessors associated with subdivision.")
    public List<UUID> accessors = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_BUILDERS, comment = "The builders associated with subdivision.")
    public List<UUID> builders = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_CONTAINERS, comment = "The containers associated with subdivision.")
    public List<UUID> containers = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_COOWNERS, comment = "The coowners associated with subdivision.")
    public List<UUID> coowners = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_BANNED_ITEM_LIST, comment = "Item id's that are banned from use in claim.")
    private List<String> bannedItemList = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_PROTECTION_BLACKLIST, comment = "Item id's that are not protected within subdivision.")
    public ArrayList<String> protectionBlacklist = new ArrayList<>();
    @Setting
    public ClaimDataFlagsCategory flags = new ClaimDataFlagsCategory();

    public SubDivisionDataConfig(Claim claim) {
        this.lesserBoundaryCornerPos = BlockUtils.positionToString(claim.lesserBoundaryCorner);
        this.greaterBoundaryCornerPos = BlockUtils.positionToString(claim.greaterBoundaryCorner);
    }

    @Override
    public UUID getWorldUniqueId() {
        return GriefPrevention.PUBLIC_UUID; // return dummy uuid
    }
    @Override
    public UUID getOwnerUniqueId() {
        return GriefPrevention.PUBLIC_UUID; // return dummy uuid
    }
    @Override
    public void setClaimOwnerUniqueId(UUID newClaimOwner) {
    }
    @Override
    public Type getClaimType() {
        return this.claimType;
    }
    @Override
    public String getDateCreated() {
        return this.dateCreated;
    }
    @Override
    public String getDateLastActive() {
        return this.dateLastActive;
    }
    @Override
    public Text getClaimName() {
        return this.claimName;
    }
    @Override
    public Text getGreetingMessage() {
        return this.claimGreetingMessage;
    }
    @Override
    public Text getFarewellMessage() {
        return this.claimFarewellMessage;
    }
    @Override
    public String getLesserBoundaryCorner() {
        return this.lesserBoundaryCornerPos;
    }
    @Override
    public String getGreaterBoundaryCorner() {
        return this.greaterBoundaryCornerPos;
    }
    @Override
    public List<UUID> getAccessors() {
        return this.accessors;
    }
    @Override
    public List<UUID> getBuilders() {
        return this.builders;
    }
    @Override
    public List<UUID> getContainers() {
        return this.containers;
    }
    @Override
    public List<UUID> getCoowners() {
        return this.coowners;
    }
    @Override
    public List<String> getProtectionBlackList() {
        return this.protectionBlacklist;
    }
    @Override
    public ClaimDataFlagsCategory getFlags() {
        return this.flags;
    }

    public List<String> getBannedItemList() {
        return this.bannedItemList;
    }

    @Override
    public void setClaimType(Type type) {
        this.claimType = type;
    }

    @Override
    public void setDateLastActive(String date) {
        this.dateLastActive = date;
    }

    @Override
    public void setClaimName(Text name) {
        this.claimName = name;
    }

    @Override
    public void setGreetingMessage(Text message) {
        this.claimGreetingMessage = message;
    }

    @Override
    public void setFarewellMessage(Text message) {
        this.claimFarewellMessage = message;
    }

    @Override
    public void setLesserBoundaryCorner(String location) {
        this.lesserBoundaryCornerPos = location;
    }

    @Override
    public void setGreaterBoundaryCorner(String location) {
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

    @Override
    public void setFlags(ClaimDataFlagsCategory copyFlags) {
        this.flags = copyFlags;
    }

    public void setCoowners(List<UUID> coowners) {
        this.requiresSave = true;
        this.coowners = coowners;
    }

    public boolean requiresSave() {
        return this.requiresSave;
    }

    public void setRequiresSave(boolean flag) {
        this.requiresSave = flag;
    }
}