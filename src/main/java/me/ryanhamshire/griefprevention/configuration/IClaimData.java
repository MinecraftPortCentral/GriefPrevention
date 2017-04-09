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
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IClaimData {

    boolean requiresSave();

    boolean allowClaimExpiration();

    boolean allowDenyMessages();

    boolean allowFlagOverrides();

    boolean isCuboid();

    boolean isResizable();

    boolean doesInheritParent();

    boolean requiresClaimBlocks();

    Tristate getPvpOverride();

    UUID getWorldUniqueId();

    UUID getOwnerUniqueId();

    ClaimType getType();

    Instant getDateCreated();

    Instant getDateLastActive();

    Optional<Text> getName();

    Optional<Text> getGreeting();

    Optional<Text> getFarewell();

    Vector3i getLesserBoundaryCornerPos();

    Vector3i getGreaterBoundaryCornerPos();

    Optional<Vector3i> getSpawnPos();

    List<UUID> getAccessors();

    List<UUID> getBuilders();

    List<UUID> getContainers();

    List<UUID> getManagers();

    void setOwnerUniqueId(UUID newClaimOwner);

    void setWorldUniqueId(UUID uuid);

    void setType(ClaimType type);

    void setCuboid(boolean cuboid);

    void setResizable(boolean resizable);

    void setDateLastActive(Instant date);

    void setName(Text name);

    void setGreeting(Text message);

    void setFarewell(Text message);

    void setLesserBoundaryCorner(String location);

    void setGreaterBoundaryCorner(String location);

    void setAccessors(List<UUID> accessors);

    void setBuilders(List<UUID> builders);

    void setContainers(List<UUID> containers);

    void setManagers(List<UUID> coowners);

    void setRequiresSave(boolean flag);

    void setInheritParent(boolean flag);

    void setClaimExpiration(boolean flag);

    void setFlagOverrides(boolean flag);

    void setDenyMessages(boolean flag);

    void setPvpOverride(Tristate value);

    void setSpawnPos(Vector3i spawnPos);

    void setRequiresClaimBlocks(boolean requiresClaimBlocks);
}