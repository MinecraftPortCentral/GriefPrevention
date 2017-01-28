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

import com.flowpowered.math.vector.Vector3i;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.api.data.ClaimData;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.configuration.category.ConfigCategory;
import me.ryanhamshire.griefprevention.util.BlockUtils;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ConfigSerializable
public class SubDivisionDataConfig extends ConfigCategory implements ClaimData, IClaimData {

    private IClaimData parent;
    private ClaimStorageData parentStorage;
    private Vector3i lesserPos;
    private Vector3i greaterPos;

    @Setting(value = ClaimStorageData.MAIN_OWNER_UUID)//, comment = "The owner uuid assocated with claim.")
    private UUID ownerUniqueId;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_NAME)//, comment = "The name associated with subdivision.")
    public Text claimName;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_TYPE)//, comment = "The type of claim.")
    public ClaimType claimType = ClaimType.SUBDIVISION;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_CUBOID)
    private boolean isCuboid = false;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_RESIZABLE)
    private boolean isResizable = true;
    @Setting(value = ClaimStorageData.MAIN_ALLOW_DENY_MESSAGES)
    private boolean allowDenyMessages = true;
    @Setting(value = ClaimStorageData.MAIN_ALLOW_CLAIM_EXPIRATION)
    private boolean allowClaimExpiration = false;
    @Setting(value = ClaimStorageData.MAIN_ALLOW_FLAG_OVERRIDES)
    private boolean allowFlagOverrides = false;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_PVP)
    private Tristate pvpOverride = Tristate.UNDEFINED;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_DATE_CREATED)//, comment = "The date and time this subdivision was created.")
    public String dateCreated = Instant.now().toString();
    @Setting(value = ClaimStorageData.MAIN_CLAIM_DATE_LAST_ACTIVE)//, comment = "The last date and time this subdivision was used.")
    public String dateLastActive = Instant.now().toString();
    @Setting(value = ClaimStorageData.MAIN_CLAIM_GREETING)//, comment = "The greeting message players will receive when entering subdivision.")
    public Text claimGreetingMessage;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_FAREWELL)//, comment = "The farewell message players will receive when leaving subdivision.")
    public Text claimFarewellMessage;
    @Setting(value = ClaimStorageData.MAIN_LESSER_BOUNDARY_CORNER)//, comment = "The lesser boundary corner location of subdivision.")
    public String lesserBoundaryCornerPos;
    @Setting(value = ClaimStorageData.MAIN_GREATER_BOUNDARY_CORNER)//, comment = "The greater boundary corner location of subdivision.")
    public String greaterBoundaryCornerPos;
    @Setting(value = ClaimStorageData.MAIN_ACCESSORS)//, comment = "The accessors associated with subdivision.")
    public List<UUID> accessors = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_BUILDERS)//, comment = "The builders associated with subdivision.")
    public List<UUID> builders = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_CONTAINERS)//, comment = "The containers associated with subdivision.")
    public List<UUID> containers = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_MANAGERS)//, comment = "The managers associated with subdivision.")
    public List<UUID> managers = new ArrayList<>();
    @Setting(value = ClaimStorageData.SUB_INHERIT_PARENT)
    public boolean inheritParent = true;

    public SubDivisionDataConfig() {}

    public SubDivisionDataConfig(GPClaim claim) {
        this.lesserBoundaryCornerPos = BlockUtils.positionToString(claim.lesserBoundaryCorner);
        this.greaterBoundaryCornerPos = BlockUtils.positionToString(claim.greaterBoundaryCorner);
        this.isCuboid = claim.cuboid;
        this.parent = claim.parent.getInternalClaimData();
        this.parentStorage = claim.parent.getClaimStorage();
    }

    @Override
    public boolean doesInheritParent() {
        return this.inheritParent;
    }

    @Override
    public UUID getWorldUniqueId() {
        return GriefPreventionPlugin.PUBLIC_UUID; // return dummy uuid
    }

    @Override
    public UUID getOwnerUniqueId() {
        return GriefPreventionPlugin.PUBLIC_UUID; // return dummy uuid
    }

    @Override
    public boolean allowClaimExpiration() {
        return this.allowClaimExpiration;
    }

    @Override
    public boolean allowDenyMessages() {
        return this.allowDenyMessages;
    }

    @Override
    public boolean allowFlagOverrides() {
        return this.allowFlagOverrides;
    }

    @Override
    public boolean isCuboid() {
        return this.isCuboid;
    }

    @Override
    public Tristate getPvpOverride() {
        return this.pvpOverride;
    }

    @Override
    public boolean isResizable() {
        return this.isResizable;
    }

    @Override
    public ClaimType getType() {
        return this.claimType;
    }

    @Override
    public Instant getDateCreated() {
        return Instant.parse(this.dateCreated);
    }

    @Override
    public Instant getDateLastActive() {
        return Instant.parse(this.dateLastActive);
    }

    @Override
    public Optional<Text> getName() {
        return Optional.ofNullable(this.claimName);
    }

    @Override
    public Optional<Text> getGreeting() {
        return Optional.ofNullable(this.claimGreetingMessage);
    }

    @Override
    public Optional<Text> getFarewell() {
        return Optional.ofNullable(this.claimFarewellMessage);
    }

    @Override
    public Vector3i getLesserBoundaryCornerPos() {
        if (this.lesserPos == null) {
            try {
                this.lesserPos = BlockUtils.positionFromString(this.lesserBoundaryCornerPos);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return this.lesserPos;
    }

    @Override
    public Vector3i getGreaterBoundaryCornerPos() {
        if (this.greaterPos == null) {
            try {
                this.greaterPos = BlockUtils.positionFromString(this.greaterBoundaryCornerPos);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return this.greaterPos;
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
    public List<UUID> getManagers() {
        return this.managers;
    }

    @Override
    public void setWorldUniqueId(UUID uuid) {
    }

    @Override
    public void setOwnerUniqueId(UUID newClaimOwner) {
        //this.ownerUniqueId = newClaimOwner;
    }

    @Override
    public void setDenyMessages(boolean flag) {
        this.parent.setRequiresSave(true);
        this.allowDenyMessages = flag;
    }

    @Override
    public void setClaimExpiration(boolean flag) {
        this.parent.setRequiresSave(true);
        this.allowClaimExpiration = flag;
    }

    @Override
    public void setFlagOverrides(boolean flag) {
        this.parent.setRequiresSave(true);
        this.allowFlagOverrides = flag;
    }

    @Override
    public void setCuboid(boolean cuboid) {
        this.parent.setRequiresSave(true);
        this.isCuboid = cuboid;
    }

    @Override
    public void setPvpOverride(Tristate pvp) {
        this.parent.setRequiresSave(true);
        this.pvpOverride = pvp;
    }

    @Override
    public void setResizable(boolean resizable) {
        this.parent.setRequiresSave(true);
        this.isResizable = resizable;
    }

    @Override
    public void setType(ClaimType type) {
        this.parent.setRequiresSave(true);
        this.claimType = type;
    }

    @Override
    public void setDateLastActive(Instant date) {
        this.parent.setRequiresSave(true);
        this.dateLastActive = date.toString();
    }

    @Override
    public void setName(Text name) {
        this.parent.setRequiresSave(true);
        this.claimName = name;
    }

    @Override
    public void setGreeting(Text message) {
        this.parent.setRequiresSave(true);
        this.claimGreetingMessage = message;
    }

    @Override
    public void setFarewell(Text message) {
        this.parent.setRequiresSave(true);
        this.claimFarewellMessage = message;
    }

    @Override
    public void setLesserBoundaryCorner(String location) {
        this.parent.setRequiresSave(true);
        this.lesserBoundaryCornerPos = location;
    }

    @Override
    public void setGreaterBoundaryCorner(String location) {
        this.parent.setRequiresSave(true);
        this.greaterBoundaryCornerPos = location;
    }

    @Override
    public void setAccessors(List<UUID> accessors) {
        this.parent.setRequiresSave(true);
        this.accessors = accessors;
    }

    @Override
    public void setBuilders(List<UUID> builders) {
        this.parent.setRequiresSave(true);
        this.builders = builders;
    }

    @Override
    public void setContainers(List<UUID> containers) {
        this.parent.setRequiresSave(true);
        this.containers = containers;
    }

    @Override
    public void setManagers(List<UUID> coowners) {
        this.parent.setRequiresSave(true);
        this.managers = coowners;
    }

    @Override
    public boolean requiresSave() {
        return this.parent.requiresSave();
    }

    @Override
    public void setRequiresSave(boolean flag) {
        if (flag) {
            this.parent.setRequiresSave(true);
        }
    }

    public void setParentData(IClaimData parentData) {
        this.parent = parentData;
    }

    @Override
    public void setInheritParent(boolean flag) {
        this.parent.setRequiresSave(true);
        this.inheritParent = flag;
    }

    @Override
    public void save() {
        this.parentStorage.save();
    }

    @Override
    public Optional<Vector3i> getSpawnPos() {
        return Optional.empty();
    }

    @Override
    public void setSpawnPos(Vector3i spawnPos) {
    }

    @Override
    public boolean requiresClaimBlocks() {
        return false;
    }

    @Override
    public void setRequiresClaimBlocks(boolean requiresClaimBlocks) {
    }
}