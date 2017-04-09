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
import com.google.common.collect.Maps;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.api.data.ClaimData;
import me.ryanhamshire.griefprevention.configuration.category.ConfigCategory;
import me.ryanhamshire.griefprevention.util.BlockUtils;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ConfigSerializable
public class ClaimDataConfig extends ConfigCategory implements ClaimData, IClaimData {

    private boolean requiresSave = false;
    private Vector3i lesserPos;
    private Vector3i greaterPos;
    private Vector3i spawnPos;
    private ClaimStorageData claimStorage;

    @Setting(value = ClaimStorageData.MAIN_WORLD_UUID)//, comment = "The world uuid associated with claim.")
    private UUID worldUniqueId;
    @Setting(value = ClaimStorageData.MAIN_OWNER_UUID)//, comment = "The owner uuid assocated with claim.")
    private UUID ownerUniqueId;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_TYPE)//, comment = "The type of claim.")
    private ClaimType claimType = ClaimType.BASIC;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_CUBOID)
    private boolean isCuboid = false;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_RESIZABLE)
    private boolean isResizable = true;
    @Setting(value = ClaimStorageData.MAIN_ALLOW_DENY_MESSAGES)
    private boolean allowDenyMessages = true;
    @Setting(value = ClaimStorageData.MAIN_ALLOW_CLAIM_EXPIRATION)
    private boolean allowClaimExpiration = true;
    @Setting(value = ClaimStorageData.MAIN_ALLOW_FLAG_OVERRIDES)
    private boolean allowFlagOverrides = true;
    @Setting(value = ClaimStorageData.MAIN_REQUIRES_CLAIM_BLOCKS)
    private boolean requiresClaimBlocks = true;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_PVP)
    private Tristate pvpOverride = Tristate.UNDEFINED;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_DATE_CREATED)//, comment = "The date and time this claim was created.")
    private String dateCreated = Instant.now().toString();
    @Setting(value = ClaimStorageData.MAIN_CLAIM_DATE_LAST_ACTIVE)//, comment = "The last date and time this claim was active.")
    private String dateLastActive = Instant.now().toString();
    @Setting(value = ClaimStorageData.MAIN_CLAIM_NAME)//, comment = "The name associated with claim.")
    private Text claimName;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_GREETING)//, comment = "The greeting message players will receive when entering claim area.")
    private Text claimGreetingMessage;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_FAREWELL)//, comment = "The farewell message players will receive when leaving claim area.")
    private Text claimFarewellMessage;
    @Setting(value = ClaimStorageData.MAIN_CLAIM_SPAWN)
    private String claimSpawn;
    @Setting(value = ClaimStorageData.MAIN_LESSER_BOUNDARY_CORNER)//, comment = "The lesser boundary corner location of claim.")
    private String lesserBoundaryCornerPos;
    @Setting(value = ClaimStorageData.MAIN_GREATER_BOUNDARY_CORNER)//, comment = "The greater boundary corner location of claim.")
    private String greaterBoundaryCornerPos;
    @Setting(value = ClaimStorageData.MAIN_ACCESSORS)//, comment = "The accessors associated with claim. Note: Accessors can interact with all blocks except inventory containers like chests.")
    private List<UUID> accessors = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_BUILDERS)//, comment = "The builders associated with claim. Note: Builders can do everything accessors and containers do with the addition of placing and breaking blocks.")
    private List<UUID> builders = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_CONTAINERS)//, comment = "The containers associated with claim. Note: Containers can do everything accessors with the addition of inventory access.")
    private List<UUID> containers = new ArrayList<>();
    @Setting(value = ClaimStorageData.MAIN_MANAGERS)//, comment = "The managers associated with claim. Note: Managers have permission to grant trust and flag permissions within claim.")
    private List<UUID> managers = new ArrayList<>();
    @Setting
    private Map<UUID, SubDivisionDataConfig> subdivisions = Maps.newHashMap();

    public ClaimDataConfig() {

    }

    @Override
    public UUID getWorldUniqueId() {
        return this.worldUniqueId;
    }

    @Override
    public UUID getOwnerUniqueId() {
        return this.ownerUniqueId;
    }

    @Override
    public boolean allowClaimExpiration() {
        return this.allowClaimExpiration;
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
    public boolean allowDenyMessages() {
        return this.allowDenyMessages;
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
    public Optional<Vector3i> getSpawnPos() {
        if (this.spawnPos == null && this.claimSpawn != null) {
            try {
                this.spawnPos = BlockUtils.positionFromString(this.claimSpawn);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return Optional.ofNullable(this.spawnPos);
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

    public List<UUID> getAccessors() {
        return this.accessors;
    }

    public List<UUID> getBuilders() {
        return this.builders;
    }

    public List<UUID> getContainers() {
        return this.containers;
    }

    public List<UUID> getManagers() {
        return this.managers;
    }

    @Override
    public void setDenyMessages(boolean flag) {
        this.requiresSave = true;
        this.allowDenyMessages = flag;
    }

    @Override
    public void setClaimExpiration(boolean flag) {
        this.requiresSave = true;
        this.allowClaimExpiration = flag;
    }

    @Override
    public void setFlagOverrides(boolean flag) {
        this.allowFlagOverrides = flag;
    }

    @Override
    public void setCuboid(boolean cuboid) {
        this.isCuboid = cuboid;
    }

    @Override
    public void setPvpOverride(Tristate pvp) {
        this.requiresSave = true;
        this.pvpOverride = pvp;
    }

    @Override
    public void setResizable(boolean resizable) {
        this.requiresSave = true;
        this.isResizable = resizable;
    }

    @Override
    public void setType(ClaimType type) {
        this.requiresSave = true;
        this.claimType = type;
    }

    @Override
    public void setDateLastActive(Instant date) {
        this.requiresSave = true;
        this.dateLastActive = date.toString();
    }

    @Override
    public void setName(Text name) {
        this.requiresSave = true;
        this.claimName = name;
    }

    @Override
    public void setGreeting(Text message) {
        this.requiresSave = true;
        this.claimGreetingMessage = message;
    }

    @Override
    public void setFarewell(Text message) {
        this.requiresSave = true;
        this.claimFarewellMessage = message;
    }

    @Override
    public void setLesserBoundaryCorner(String location) {
        this.requiresSave = true;
        this.lesserBoundaryCornerPos = location;
    }

    @Override
    public void setGreaterBoundaryCorner(String location) {
        this.requiresSave = true;
        this.greaterBoundaryCornerPos = location;
    }

    @Override
    public void setAccessors(List<UUID> accessors) {
        this.requiresSave = true;
        this.accessors = accessors;
    }

    @Override
    public void setBuilders(List<UUID> builders) {
        this.requiresSave = true;
        this.builders = builders;
    }

    @Override
    public void setContainers(List<UUID> containers) {
        this.requiresSave = true;
        this.containers = containers;
    }

    @Override
    public void setManagers(List<UUID> coowners) {
        this.requiresSave = true;
        this.managers = coowners;
    }

    public Map<UUID, SubDivisionDataConfig> getSubdivisions() {
        return this.subdivisions;
    }

    public boolean requiresSave() {
        return this.requiresSave;
    }

    @Override
    public void setRequiresSave(boolean flag) {
        this.requiresSave = flag;
    }

    @Override
    public boolean doesInheritParent() {
        return false;
    }

    @Override
    public void setInheritParent(boolean flag) {
        // do nothing
    }

    @Override
    public void setOwnerUniqueId(UUID newClaimOwner) {
        this.ownerUniqueId = newClaimOwner;
    }

    @Override
    public void setWorldUniqueId(UUID uuid) {
        this.worldUniqueId = uuid;
    }

    public void setClaimStorageData(ClaimStorageData claimStorage) {
        this.claimStorage = claimStorage;
    }

    @Override
    public void save() {
        this.claimStorage.save();
    }

    @Override
    public void setSpawnPos(Vector3i spawnPos) {
        this.spawnPos = spawnPos;
        this.claimSpawn = BlockUtils.positionToString(spawnPos);
    }

    @Override
    public boolean requiresClaimBlocks() {
        return this.requiresClaimBlocks;
    }

    @Override
    public void setRequiresClaimBlocks(boolean requiresClaimBlocks) {
        this.requiresClaimBlocks = requiresClaimBlocks;
    }
}