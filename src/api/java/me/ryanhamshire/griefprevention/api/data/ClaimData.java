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
package me.ryanhamshire.griefprevention.api.data;

import com.flowpowered.math.vector.Vector3i;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents the persisted data of a claim.
 */
public interface ClaimData {

    /**
     * Gets the claim's name.
     * 
     * @return The name of claim, if available
     */
    Optional<Text> getName();

    /**
     * Gets the {@link ClaimType} of claim.
     * 
     * @return The claim type
     */
    ClaimType getType();

    /**
     * Gets the lesser boundary corner of claim.
     * 
     * @return The lesser boundary corner position.
     */
    Vector3i getLesserBoundaryCornerPos();

    /**
     * Gets the greater boundary corner of claim.
     * 
     * @return The greater boundary corner position.
     */
    Vector3i getGreaterBoundaryCornerPos();

    /**
     * Gets the spawn position of claim.
     * 
     * @return The spawn position, if available
     */
    Optional<Vector3i> getSpawnPos();

    Optional<UUID> getParent();

    /** Gets the claim's world {@link UUID}.
     * 
     * @return The world UUID
     */
    UUID getWorldUniqueId();

    /**
     * Gets the claim owner's {@link UUID}.
     * 
     * Note: {@link ClaimType#ADMIN} and {@link ClaimType#WILDERNESS} claims do not have
     * owners.
     * 
     * @return The UUID of this claim
     */
    UUID getOwnerUniqueId();

    /**
     * Gets the claim's greeting message.
     * 
     * @return The greeting message, if available
     */
    Optional<Text> getGreeting();

    /**
     * Gets the claim's farewell message.
     * 
     * @return The farewell message, if available
     */
    Optional<Text> getFarewell();

    /**
     * Gets the creation date of claim.
     * 
     * @return The date created
     */
    Instant getDateCreated();

    /**
     * Gets the last active date of claim.
     * 
     * @return The last active date
     */
    Instant getDateLastActive();

    Tristate getPvpOverride();

    /**
     * Gets whether the claim allows sending deny messages
     * to players.
     * 
     * @return Whether deny messages are being sent to players
     */
    boolean allowDenyMessages();

    /**
     * Gets whether the claim allows flag overrides.
     * 
     * @return Whether flag overrides are allowed
     */
    boolean allowFlagOverrides();

    /**
     * Gets whether claim can expire.
     * 
     * @return If claim can expire
     */
    boolean allowExpiration();

    /**
     * Gets whether claim is a 3D cuboid.
     * 
     * @return True if 3D cuboid, false if not
     */
    boolean isCuboid();

    /**
     * Gets whether this claim is inheriting from parent claim.
     * 
     * @return If claim inherits from parent
     */
    boolean doesInheritParent();

    /**
     * Gets whether this claim can be resized.
     * 
     * @return If resizeable
     */
    boolean isResizable();

    boolean isExpired();
    /**
     * Gets whether this claim requires claim blocks.
     * 
     * @return Whether claim requires claim blocks
     */
    boolean requiresClaimBlocks();

    /**
     * Gets whether this claim has min/max size restrictions.
     * 
     * @return Whether claim has size restrictions
     */
    boolean hasSizeRestrictions();

    /**
     * Sets the name of claim.
     * 
     * @param name The claim name
     */
    void setName(Text name);

    /**
     * Sets the farewell message when a player exits the claim.
     * 
     * @param farewell The farewell message
     */
    void setFarewell(Text farewell);

    /**
     * Sets the greeting message when a player enters the claim.
     * 
     * @param greeting The greeting message
     */
    void setGreeting(Text greeting);

    /**
     * Sets last active date of claim.
     * 
     * Note: The date is used to determine whether the claim expires
     * based on expiration settings.
     * 
     * @param date The last active date
     */
    void setDateLastActive(Instant date);

    /**
     * Toggles whether this claim is inheriting from parent claim.
     * 
     * @param inherit Whether claim inherits from parent
     */
    void setInheritParent(boolean inherit);

    /**
     * Toggles whether this claim is resizable.
     * 
     * @param allowResize Whether claim can be resized.
     */
    void setResizable(boolean allowResize);

    /**
     * Toggles whether this claim allows deny messages to be sent to
     * players. If false, no deny messages will be sent.
     * 
     * @param allowDenyMessages Whether to allow sending deny messages to players
     */
    void setDenyMessages(boolean allowDenyMessages);

    /**
     * Toggles whether this claim can expire due to no player activity.
     * 
     * @param allowExpire Whether this claim allows expirations
     */
    void setExpiration(boolean allowExpire);

    /**
     * Toggles whether this claim should allow flag overrides.
     * 
     * @param allowOverrides Whether this claim allows flag overrides
     */
    void setFlagOverrides(boolean allowOverrides);

    void setParent(UUID uniqueId);

    /**
     * Overrides the world PvP setting.
     * 
     * Possible values are :
     * <ul>
     *     <li>{@link Tristate#UNDEFINED} will use world PvP setting</li>
     *     <li>{@link Tristate#TRUE} will force PvP in claim</li>
     *     <li>{@link Tristate#FALSE} will force disable PvP in claim</li>
     * </ul>
     * 
     * @param value The new 
     */
    void setPvpOverride(Tristate value);

    /**
     * Sets if this claim requires claim blocks from players.
     * 
     * Note: This is true by default.
     * 
     * @param requiresClaimBlocks Whether this claim requires claim blocks
     */
    void setRequiresClaimBlocks(boolean requiresClaimBlocks);

    /**
     * Sets if this claim checks for min/max size restrictions.
     * 
     * Note: {@link ClaimType#ADMIN} and {@link ClaimType#WILDERNESS} can not have restrictions.
     * 
     * @param sizeRestrictions Whether this claim checks size restrictions.
     */
    void setSizeRestrictions(boolean sizeRestrictions);

    /**
     * Sets the spawn position of claim.
     * 
     * @param location The new spawn position
     */
    default void setSpawnPos(Location<World> location) {
        setSpawnPos(location.getBlockPosition());
    }

    /**
     * Sets the spawn position of claim.
     * 
     * @param pos The new spawn position
     */
    void setSpawnPos(Vector3i pos);

    /**
     * Saves all changes to storage.
     */
    void save();

    EconomyData getEconomyData();
}
