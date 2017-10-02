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
package me.ryanhamshire.griefprevention.api.claim;

import com.flowpowered.math.vector.Vector3i;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.api.data.ClaimData;
import me.ryanhamshire.griefprevention.api.data.EconomyData;
import me.ryanhamshire.griefprevention.api.data.PlayerData;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextSource;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a protected claim.
 */
public interface Claim extends ContextSource {

    /**
     * Gets the {@link UUID} of claim.
     * 
     * @return The UUID of this claim
     */
    UUID getUniqueId();

    /**
     * Gets the claim owner's name.
     * 
     * Note: {@link ClaimType#ADMIN} and {@link ClaimType#WILDERNESS} claims do not have
     * owners. These claims should return a general name such as 'administrator'.
     * 
     * @return The name of claim owner, if available
     */
    Text getOwnerName();

    /**
     * Gets the claim's parent.
     * 
     * @return The parent claim, if available
     */
    Optional<Claim> getParent();

    /**
     * Gets the lesser boundary corner of claim.
     * 
     * @return The lesser boundary corner location.
     */
    Location<World> getLesserBoundaryCorner();

    /**
     * Gets the greater boundary corner of claim.
     * 
     * @return The greater boundary corner location.
     */
    Location<World> getGreaterBoundaryCorner();

    /**
     * Gets whether claim is a cuboid.
     * 
     * @return true if claim is cuboid
     */
    boolean isCuboid();

    /**
     * Checks if claim is parent
     * 
     * @param claim The claim to check
     * @return true if claim is parent
     */
    boolean isParent(Claim claim);

    /**
     * Gets the claim blocks required for this claim.
     * 
     * Note: If cuboids are enabled in wilderness, 2D claims will factor in Y.
     * 
     * @return The claim blocks of claim
     */
    int getClaimBlocks();

    /**
     * Gets the total claim area in blocks.
     * 
     * @return The total area of claim
     */
    int getArea();

    /**
     * Gets the total claim volume in blocks.
     *      
     * @return The total volume of claim
     */
    int getVolume();

    /**
     * Gets the claim's width in blocks.
     * 
     * @return The width of claim
     */
    int getWidth();

    /**
     * Gets the claim's height in blocks.
     * 
     * @return The height of claim
     */
    int getHeight();

    /**
     * Gets the list of chunks used by this claim.
     * 
     * @return The list of claim chunks used.
     */
    List<Chunk> getChunks();

    /**
     * Gets the list of entities currently in claim.
     * 
     * @return The list of entities in claim.
     */
    List<Entity> getEntities();

    /**
     * Gets the list of players currently in claim.
     * 
     * @return The list of players in claim.
     */
    List<Player> getPlayers();

    /** Gets the claim world.
     * 
     * @return The world
     */
    World getWorld();

    /**
     * Transfers claim to new owner.
     * 
     * Note: Both {@link ClaimType#WILDERNESS} and {@link ClaimType#ADMIN} cannot be transferred.
     * This validates if the new owner has enough claim blocks to support this claim.
     * 
     * @param ownerUniqueId
     * @return The claim result
     */
    ClaimResult transferOwner(UUID ownerUniqueId);

    /**
     * Attempts to change claim to another type.
     * 
     * Note: If changing an {@link ClaimType#ADMIN} claim, owner is required.
     * 
     * @param type The new claim type
     * @return The claim result
     */
    default ClaimResult changeType(ClaimType type) {
        return changeType(type, Optional.empty());
    }

    /**
     * Attempts to change claim to another type and owner.
     * 
     * Note: {@link ClaimType#WILDERNESS} cannot be changed.
     * If changing an {@link ClaimType#ADMIN} claim, owner is required.
     * 
     * @param type The new claim type
     * @param owner The owner to set
     * @return The claim result
     */
    ClaimResult changeType(ClaimType type, Optional<UUID> owner);

    /**
     * Resizes a claim.
     * 
     * @param startCornerLoc The start corner location
     * @param endCornerLoc The end corner location
     * @return The claim result
     */
    default ClaimResult resize(Location<World> startCornerLoc, Location<World> endCornerLoc, Cause cause) {
        return this.resize(startCornerLoc.getBlockPosition(), endCornerLoc.getBlockPosition(), cause);
    }

    /**
     * Resizes a claim.
     * 
     * @param startCornerPos The start corner block position
     * @param endCornerPos The end corner block position
     * @return The claim result
     */
    default ClaimResult resize(Vector3i startCornerPos, Vector3i endCornerPos, Cause cause) {
        return this.resize(startCornerPos.getX(), endCornerPos.getX(), startCornerPos.getY(), endCornerPos.getY(), startCornerPos.getZ(), endCornerPos.getZ(), cause);
    }

    /**
     * Resizes the claim.
     * 
     * @param x1 The start X-axis position
     * @param x2 The end X-axis position
     * @param y1 The start Y-axis position
     * @param y2 The end Y-axis position
     * @param z1 The start Z-axis position
     * @param z2 The end Z-axis position
     * @param cause The cause of resize
     * @return The claim result
     */
    ClaimResult resize(int x1, int x2, int y1, int y2, int z1, int z2, Cause cause);

    /**
     * Gets an immutable list of child claims.
     * 
     * Note: This will return an empty list if no child claims
     * are found.
     * 
     * @param recursive Whether to recursively scan for children
     * @return The immutable list of child claims
     */
    List<Claim> getChildren(boolean recursive);

    /**
     * Gets an immutable list of parent claims.
     * 
     * Note: This will return an empty list if no parent claims
     * are found.
     * 
     * @param recursive Whether to recursively scan for parents
     * @return The immutable list of parent claims
     */
    List<Claim> getParents(boolean recursive);

    /**
     * Gets an immutable list of all trusted users.
     * 
     * @return An immutable list of all trusted users
     */
    @Deprecated
    default List<UUID> getAllTrusts() {
        return this.getUserTrusts();
    }

    /**
     * Gets an immutable list of trusted users for {@link TrustType}.
     * 
     * @return An immutable list of trusted users
     */
    @Deprecated
    default List<UUID> getTrusts(TrustType type) {
        return this.getUserTrusts(type);
    }

    /**
     * Gets an immutable list of all trusted users.
     * 
     * @return An immutable list of all trusted users
     */
    List<UUID> getUserTrusts();

    /**
     * Gets an immutable list of trusted users for {@link TrustType}.
     * 
     * @return An immutable list of trusted users
     */
    List<UUID> getUserTrusts(TrustType type);

    /**
     * Gets an immutable list of all trusted groups.
     * 
     * @return An immutable list of all trusted groups
     */
    List<String> getGroupTrusts();

    /**
     * Gets an immutable list of trusted groups for {@link TrustType}.
     * 
     * @return An immutable list of trusted groups
     */
    List<String> getGroupTrusts(TrustType type);

    /**
     * Grants claim trust to the UUID for given {@link TrustType}.
     * 
     * @param uuid The UUID of user
     * @param type The trust type
     * @param cause The plugin cause
     * @return The claim result
     */
    @Deprecated
    default ClaimResult addTrust(UUID uuid, TrustType type, Cause cause) {
        return this.addUserTrust(uuid, type, cause);
    }

    /**
     * Grants claim trust to the list of UUID's for given {@link TrustType}.
     * 
     * @param uuid The list of user UUID's
     * @param type The trust type
     * @param cause The plugin cause
     * @return The claim result
     */
    @Deprecated
    default ClaimResult addTrusts(List<UUID> uuid, TrustType type, Cause cause) {
        return this.addUserTrusts(uuid, type, cause);
    }

    /**
     * Removes UUID from claim trust for given {@link TrustType}.
     * 
     * @param uuid The UUID of user
     * @param type The trust type
     * @param cause The plugin cause
     * @return The claim result
     */
    @Deprecated
    default ClaimResult removeTrust(UUID uuid, TrustType type, Cause cause) {
        return this.removeUserTrust(uuid, type, cause);
    }

    /**
     * Removes the list of UUID's from claim trust for given {@link TrustType}.
     * 
     * @param uuid The list of user UUID's
     * @param type The trust type
     * @param cause The plugin cause
     * @return The claim result
     */
    @Deprecated
    default ClaimResult removeTrusts(List<UUID> uuid, TrustType type, Cause cause) {
        return this.removeUserTrusts(uuid, type, cause);
    }

    /**
     * Clears all trusts for claim.
     * 
     * @param cause The plugin cause
     * @return The claim result
     */
    ClaimResult removeAllTrusts(Cause cause);

    /**
     * Clears all user trusts for claim.
     * 
     * @param cause The plugin cause
     * @return The claim result
     */
    ClaimResult removeAllUserTrusts(Cause cause);

    /**
     * Clears all group trusts for claim.
     * 
     * @param cause The plugin cause
     * @return The claim result
     */
    ClaimResult removeAllGroupTrusts(Cause cause);

    /**
     * Grants claim trust to the UUID for given {@link TrustType}.
     * 
     * @param uuid The UUID of user
     * @param type The trust type
     * @param cause The plugin cause
     * @return The claim result
     */
    ClaimResult addUserTrust(UUID uuid, TrustType type, Cause cause);

    /**
     * Grants claim trust to the list of UUID's for given {@link TrustType}.
     * 
     * @param uuid The list of user UUID's
     * @param type The trust type
     * @param cause The plugin cause
     * @return The claim result
     */
    ClaimResult addUserTrusts(List<UUID> uuid, TrustType type, Cause cause);

    /**
     * Removes UUID from claim trust for given {@link TrustType}.
     * 
     * @param uuid The UUID of user
     * @param type The trust type
     * @param cause The plugin cause
     * @return The claim result
     */
    ClaimResult removeUserTrust(UUID uuid, TrustType type, Cause cause);

    /**
     * Removes the list of UUID's from claim trust for given {@link TrustType}.
     * 
     * @param uuid The list of user UUID's
     * @param type The trust type
     * @param cause The plugin cause
     * @return The claim result
     */
    ClaimResult removeUserTrusts(List<UUID> uuid, TrustType type, Cause cause);

    /**
     * Grants claim trust to the group for given {@link TrustType}.
     * 
     * @param group The group
     * @param type The trust type
     * @param cause The plugin cause
     * @return The claim result
     */
    ClaimResult addGroupTrust(String group, TrustType type, Cause cause);

    /**
     * Grants claim trust to the list of groups for given {@link TrustType}.
     * 
     * @param groups The list of groups
     * @param type The trust type
     * @param cause The plugin cause
     * @return The claim result
     */
    ClaimResult addGroupTrusts(List<String> groups, TrustType type, Cause cause);

    /**
     * Removes a group from claim trust for given {@link TrustType}.
     * 
     * @param group The group
     * @param type The trust type
     * @param cause The plugin cause
     * @return The claim result
     */
    ClaimResult removeGroupTrust(String group, TrustType type, Cause cause);

    /**
     * Removes the list of UUID's from claim trust for given {@link TrustType}.
     * 
     * @param groups The list of groups
     * @param type The trust type
     * @param cause The plugin cause
     * @return The claim result
     */
    ClaimResult removeGroupTrusts(List<String> groups, TrustType type, Cause cause);

    /**
     * Checks if the {@link UUID} is able to build in claim.
     * 
     * @param uuid The uuid to check
     * @return Whether the uuid is trusted
     */
    default boolean isTrusted(UUID uuid) {
        return this.isUserTrusted(uuid, TrustType.BUILDER);
    }

    /**
     * Checks if the {@link UUID} is trusted with given {@link TrustType}.
     * 
     * @param uuid The uuid to check
     * @param type The minimum trust required
     * @return Whether the uuid is trusted
     */
    boolean isUserTrusted(UUID uuid, TrustType type);

    /**
     * Checks if the user is trusted with given {@link TrustType}.
     * 
     * @param user The user to check
     * @param type The minimum trust required
     * @return Whether the user is trusted
     */
    boolean isUserTrusted(User user, TrustType type);

    /**
     * Checks if the group is trusted in claim.
     * 
     * @param group The group to check
     * @return Whether the group is trusted
     */
    default boolean isGroupTrusted(String group) {
        return isGroupTrusted(group, TrustType.BUILDER);
    }

    /**
     * Checks if the group is trusted with given {@link TrustType}.
     * 
     * @param group The group to check
     * @param type The minimum trust required
     * @return Whether the group is trusted
     */
    boolean isGroupTrusted(String group, TrustType type);

    /**
     * Checks if this type is {@link ClaimType#ADMIN}.
     * 
     * @return true if admin claim
     */
    default boolean isAdminClaim() {
        return this.getType() == ClaimType.ADMIN;
    }

    /**
     * Checks if this type is {@link ClaimType#BASIC}.
     * 
     * @return true if basic claim
     */
    default boolean isBasicClaim() {
        return this.getType() == ClaimType.BASIC;
    }

    /**
     * Checks if this type is {@link ClaimType#SUBDIVISION}.
     * 
     * @return true if subdivision
     */
    default boolean isSubdivision() {
        return this.getType() == ClaimType.SUBDIVISION;
    }

    /**
     * Checks if this type is {@link ClaimType#TOWN}.
     * 
     * @return true if town
     */
    default boolean isTown() {
        return this.getType() == ClaimType.TOWN;
    }

    /**
     * Checks if this type is {@link ClaimType#WILDERNESS}.
     * 
     * @return true if wilderness
     */
    default boolean isWilderness() {
        return this.getType() == ClaimType.WILDERNESS;
    }

    /**
     * Checks if this claim is within a town.
     * 
     * @return true if this claim is within a town
     */
    boolean isInTown();

    /**
     * Gets the town this claim is in.
     * 
     * @return the town this claim is in, if any
     */
    Optional<Claim> getTown();

    /**
     * Gets the {@link ClaimManager} of this claim's world.
     * 
     * @return The claim manager
     */
    ClaimManager getClaimManager();

    /**
     * Gets the wilderness claim of this claim's world.
     * 
     * @return The wilderness claim
     */
    Claim getWilderness();

    /**
     * Checks if the location is within this claim.
     * 
     * Note: This always includes children.
     * 
     * @param location
     * @return Whether this claim contains the passed location
     */
    default boolean contains(Location<World> location) {
        return this.contains(location, false);
    }

    /**
     * Checks if the location is within this claim.
     * 
     * @param location
     * @param excludeChildren
     * @return Whether this claim contains the passed location
     */
    boolean contains(Location<World> location, boolean excludeChildren);

    /**
     * Checks if this claim overlaps another.
     * 
     * @param otherClaim The other claim to check
     * 
     * @return Whether this claim overlaps the other claim
     */
    boolean overlaps(Claim otherClaim);

    /**
     * Extends a cuboid claim downward.
     * 
     * @param newDepth The new depth
     * @return Whether the extension was successful
     */
    boolean extend(int newDepth);

    /**
     * Checks if this claim is within another claim.
     * 
     * @param otherClaim The other claim
     * @return Whether this claim is inside other claim
     */
    boolean isInside(Claim otherClaim);

    /**
     * Gets the chunk hashes this claim contains.
     * 
     * @return The set of chunk hashes
     */
    Set<Long> getChunkHashes();

    /**
     * Deletes all children claims.
     * 
     * @param cause The cause for deletion
     * @return The result of deletion
     */
    ClaimResult deleteChildren(Cause cause);

    /**
     * Deletes all children claims of a specific {@link ClaimType}.
     * 
     * @param cause The cause for deletion
     * @param type The type of claims to delete
     * @return The result of deletion
     */
    ClaimResult deleteChildren(ClaimType type, Cause cause);

    /**
     * Deletes a child claim.
     * 
     * @param child The child claim to be deleted
     * @param cause The cause for deletion
     * @return The result of deletion
     */
    ClaimResult deleteChild(Claim child, Cause cause);

    /**
     * Deletes the subdivision.
     * 
     * @param subdivision The subdivision to be deleted
     * @param cause The cause for deletion
     * @return The result of deletion
     */
    @Deprecated
    ClaimResult deleteSubdivision(Claim subdivision, Cause cause);

    /**
     * Gets the persisted data of claim.
     * 
     * @return The claim's persisted data
     */
    ClaimData getData();

    /**
     * Gets the {@link ClaimType} of claim.
     * 
     * @return The claim type
     */
    default ClaimType getType() {
        return this.getData().getType();
    }

    /** Gets the claim's world {@link UUID}.
     * 
     * @return The world UUID
     */
    default UUID getWorldUniqueId() {
        return this.getData().getWorldUniqueId();
    }

    /**
     * Gets the claim owner's {@link UUID}.
     * 
     * Note: {@link ClaimType#ADMIN} and {@link ClaimType#WILDERNESS} claims do not have
     * owners.
     * 
     * @return The UUID of this claim
     */
    default UUID getOwnerUniqueId() {
        return this.getData().getOwnerUniqueId();
    }

    /**
     * Gets the claim's name.
     * 
     * @return The name of claim, if available
     */
    default Optional<Text> getName() {
        return this.getData().getName();
    }

    default EconomyData getEconomyData() {
        return this.getData().getEconomyData();
    }

    /**
     * Gets the economy account used for claim bank.
     * 
     * @return the economy account, if available
     */
    Optional<Account> getEconomyAccount();

    /**
     * Sets {@link ClaimFlag} permission on the {@link Subject}.
     * 
     * @param subject The subject
     * @param flag The claim flag
     * @param value The new value
     * @param cause The cause of set
     * @return The result of set
     */
    default CompletableFuture<FlagResult> setPermission(Subject subject, ClaimFlag flag, String source, String target, Tristate value, Cause cause) {
        return setPermission(subject, flag, source, target, value, this.getContext(), cause);
    }

    /**
     * Sets {@link ClaimFlag} permission for target on the {@link Subject}.
     * 
     * @param subject The subject
     * @param flag The claim flag
     * @param value The new value
     * @param cause The cause of set
     * @return The result of set
     */
    default CompletableFuture<FlagResult> setPermission(Subject subject, ClaimFlag flag, String target, Tristate value, Cause cause) {
        return setPermission(subject, flag, target, value, this.getContext(), cause);
    }

    /**
     * Gets the {@link ClaimFlag} permission value of {@link Subject} for source and target.
     * 
     * @param subject The subject
     * @param flag The claim flag
     * @param target The target id
     * @return The permission value, or {@link Tristate#UNDEFINED} if none
     */
    default Tristate getPermissionValue(Subject subject, ClaimFlag flag, String source, String target) {
        return getPermissionValue(subject, flag, source, target, this.getContext());
    }

    /**
     * Gets the {@link ClaimFlag} permission value of {@link Subject} for target.
     * 
     * @param subject The subject
     * @param flag The claim flag
     * @param target The target id
     * @return The permission value, or {@link Tristate#UNDEFINED} if none
     */
    default Tristate getPermissionValue(Subject subject, ClaimFlag flag, String target) {
        return getPermissionValue(subject, flag, target, this.getContext());
    }

    /**
     * Clears claim permissions on the {@link Subject}.
     * 
     * Note: All permissions will be cleared from all claim contexts. If you require
     * a specific context, use {@link #clearPermissions(Subject, Context, Cause)}.
     * 
     * @param subject The subject
     * @param cause The cause
     * @return The result of clear
     */
    CompletableFuture<FlagResult> clearPermissions(Subject subject, Cause cause);

    /**
     * Clears claim permissions from specified {@link Context}.
     * 
     * Note: This uses the default subject which applies to all users in claim.
     * 
     * @param context The context holding the permissions
     * @param cause The cause
     * @return The result of clear
     */
    CompletableFuture<FlagResult> clearPermissions(Context context, Cause cause);

    /**
     * Clears claim permissions on the {@link Subject} from specified {@link Context}.
     * 
     * @param subject The subject
     * @param context The context holding the permissions
     * @param cause The cause
     * @return The result of clear
     */
    CompletableFuture<FlagResult> clearPermissions(Subject subject, Context context, Cause cause);

    /**
     * Sets {@link ClaimFlag} permission with {@link Context}.
     * 
     * Note: This uses the default subject which applies to all users in claim.
     * 
     * @param flag The claim flag
     * @param value The new value
     * @param context The claim context
     * @param cause The cause of set
     * @return The result of set
     */
    CompletableFuture<FlagResult> setPermission(ClaimFlag flag, Tristate value, Context context, Cause cause);

    /**
     * Sets {@link ClaimFlag} permission on {@link Subject} with {@link Context}.
     * 
     * @param subject The subject
     * @param flag The claim flag
     * @param value The new value
     * @param context The claim context
     * @param cause The cause of set
     * @return The result of set
     */
    CompletableFuture<FlagResult> setPermission(Subject subject, ClaimFlag flag, Tristate value, Context context, Cause cause);

    /**
     * Sets {@link ClaimFlag} permission for target with {@link Context}.
     * 
     * Note: This uses the default subject which applies to all users in claim.
     * 
     * @param flag The claim flag
     * @param target The target id
     * @param value The new value
     * @param context The claim context
     * @param cause The cause of set
     * @return The result of set
     */
    CompletableFuture<FlagResult> setPermission(ClaimFlag flag, String target, Tristate value, Context context, Cause cause);

    /**
     * Sets {@link ClaimFlag} permission for target on {@link Subject} with {@link Context}.
     * 
     * @param subject The subject
     * @param flag The claim flag
     * @param target The target id
     * @param value The new value
     * @param context The claim context
     * @param cause The cause of set
     * @return The result of set
     */
    CompletableFuture<FlagResult> setPermission(Subject subject, ClaimFlag flag, String target, Tristate value, Context context, Cause cause);

    /**
     * Sets {@link ClaimFlag} permission for source and target on default subject with {@link Context}.
     * 
     * @param flag The claim flag
     * @param source The source id
     * @param target The target id
     * @param value The new value
     * @param context The claim context
     * @param cause The cause of set
     * @return The result of set
     */
    CompletableFuture<FlagResult> setPermission(ClaimFlag flag, String source, String target, Tristate value, Context context, Cause cause);

    /**
     * Sets {@link ClaimFlag} permission for source and target on {@link Subject} with {@link Context}.
     * 
     * @param subject The subject
     * @param flag The claim flag
     * @param source The source id
     * @param target The target id
     * @param value The new value
     * @param context The claim context
     * @param cause The cause of set
     * @return The result of set
     */
    CompletableFuture<FlagResult> setPermission(Subject subject, ClaimFlag flag, String source, String target, Tristate value, Context context, Cause cause);

    /**
     * Gets the {@link ClaimFlag} permission value for target with {@link Context}.
     * 
     * Note: This uses the default subject which applies to all users in claim.
     * 
     * @param flag The claim flag
     * @param target The target id
     * @param context The claim context
     * @return The permission value, or {@link Tristate#UNDEFINED} if none
     */
    Tristate getPermissionValue(ClaimFlag flag, String target, Context context);

    /**
     * Gets the {@link ClaimFlag} permission value of {@link Subject} for target with {@link Context}.
     * 
     * Note: Only the default subject supports default and override context. Attempting to pass another subject 
     * with these specific contexts will always return {@link Tristate#UNDEFINED}.
     * 
     * @param subject The subject
     * @param flag The claim flag
     * @param target The target id
     * @param context The claim context
     * @return The permission value, or {@link Tristate#UNDEFINED} if none
     */
    Tristate getPermissionValue(Subject subject, ClaimFlag flag, String target, Context context);

    /**
     * Gets the {@link ClaimFlag} permission value for source and target with {@link Context}.
     * 
     * Note: This uses the default subject which applies to all users in claim.
     * 
     * @param flag The claim flag
     * @param target The target id
     * @param context The claim context
     * @return The permission value, or {@link Tristate#UNDEFINED} if none
     */
    Tristate getPermissionValue(ClaimFlag flag, String source, String target, Context context);

    /**
     * Gets the {@link ClaimFlag} permission value of {@link Subject} for source and target with {@link Context}.
     * 
     * Note: Only the default subject supports default and override context. Attempting to pass another subject 
     * with these specific contexts will always return {@link Tristate#UNDEFINED}.
     * 
     * @param subject The subject
     * @param flag The claim flag
     * @param target The target id
     * @param context The claim context
     * @return The permission value, or {@link Tristate#UNDEFINED} if none
     */
    Tristate getPermissionValue(Subject subject, ClaimFlag flag, String source, String target, Context context);

    /**
     * Gets all flag permissions with {@link Context}.
     * 
     * @param context The claim context
     * @return A map containing all permissions, empty if none
     */
    Map<String, Boolean> getPermissions(Context context);

    /**
     * Gets the {@link Subject}'s flag permissions with {@link Context}.
     * 
     * @param subject The subject
     * @param context The claim context
     * @return A map containing all permissions, empty if none
     */
    Map<String, Boolean> getPermissions(Subject subject, Context context);

    /**
     * Gets the default context which is used for storing flag defaults.
     * 
     * @return The default context
     */
    Context getDefaultContext();

    /**
     * Gets the override context which is used for overriding claim flags.
     * 
     * @return The override context
     */
    Context getOverrideContext();

    /**
     * Gets a new claim builder instance for {@link Builder}.
     * 
     * @return A new claim builder instance
     */
    public static Claim.Builder builder() {
        return GriefPrevention.getApi().createClaimBuilder();
    }

    public interface Builder {

        /**
         * The cause of creation.
         * 
         * Note: This is usually a player or plugin.
         * 
         * @param cause The cause
         * @return The builder
         */
        Builder cause(Cause cause);

        /**
         * The location bounds of claim.
         * 
         * @param loc1 The lesser boundary location
         * @param loc2 The greater boundary location
         * @return The builder
         */
        default Builder bounds(Location<World> loc1, Location<World> loc2) {
            return this.bounds(loc1.getBlockPosition(), loc2.getBlockPosition());
        }

        /**
         * The position bounds of claim.
         * 
         * @param point1 The lesser boundary position
         * @param point2 The greater boundary position
         * @return The builder
         */
        Builder bounds(Vector3i point1, Vector3i point2);

        /**
         * Toggles whether this claim is cuboid
         * 
         * @param cuboid Whether claim is cuboid
         * @return The builder
         */
        Builder cuboid(boolean cuboid);

        /**
         * The owner of claim.
         * 
         * Note: {@link ClaimType#ADMIN} does not use owners.
         * 
         * @param ownerUniqueId The claim owner UUID, if available
         * @return The builder
         */
        Builder owner(UUID ownerUniqueId);

        /**
         * The claim type.
         * 
         * @param type The claim type
         * @return The builder
         */
        Builder type(ClaimType type);

        /**
         * The world to add claim into.
         * 
         * @param world The world
         * @return The builder
         */
        Builder world(World world);

        /**
         * The parent claim.
         * 
         * Note: This is required when adding a child claim
         * to an existing parent.
         * 
         * @param parent The parent, if available
         * @return The builder
         */
        Builder parent(Claim parent);

        /**
         * Toggles whether this claim allows deny messages to be sent to
         * players. If false, no deny messages will be sent.
         * 
         * @param allowDeny Whether to allow sending deny messages to players
         * @return The builder
         */
        Builder denyMessages(boolean allowDeny);

        /**
         * Toggles whether this claim can expire due to no activity.
         * 
         * @param allowExpire Whether this claim can expire
         * @return The builder
         */
        Builder expire(boolean allowExpire);

        /**
         * Sets the farewell message when a player exits the claim.
         * 
         * @param farewell The farewell message
         * @return The builder
         */
        Builder farewell(Text farewell);

        /**
         * Sets the greeting message when a player exits the claim.
         * 
         * @param greeting The greeting message
         * @return The builder
         */
        Builder greeting(Text greeting);

        /**
         * Toggles whether this claim is inheriting from parent claim.
         * 
         * @param inherit Whether claim inherits from parent
         * @return The builder
         */
        Builder inherit(boolean inherit);

        /**
         * Toggles whether this claim should allow flag overrides.
         * 
         * @param allowOverrides Whether this claim allows flag overrides
         * @return The builder
         */
        Builder overrides(boolean allowOverrides);

        /**
         * Sets if this claim requires claim blocks from players.
         * 
         * Note: This is true by default.
         * 
         * @param requireClaimBlocks Whether this claim requires claim blocks
         * @return The builder
         */
        Builder requireClaimBlocks(boolean requireClaimBlocks);

        /**
         * Toggles whether this claim is resizable.
         * 
         * @param allowResize Whether claim can be resized.
         */
        Builder resizable(boolean allowResize);

        /**
         * Whether to check {@link PlayerData#getCreateClaimLimit()}.
         * 
         * @param checkCreate Whether to check for claim creation restrictions.
         * @return The builder
         */
        Builder createLimitRestrictions(boolean checkCreateLimit);

        /**
         * Whether to check {@link PlayerData#getMinClaimLevel()} and 
         * {@link PlayerData#getMaxClaimLevel()}.
         * 
         * @param checkLevel Whether to check for level restrictions.
         * @return The builder
         */
        Builder levelRestrictions(boolean checkLevel);

        /**
         * Whether to check for min/max size restrictions.
         * 
         * @param checkSize Whether to check for size restrictions.
         * @return The builder
         */
        Builder sizeRestrictions(boolean checkSize);

        /**
         * The spawn location of this claim.
         * 
         * @param location The spawn location
         * @return The builder
         */
        default Builder spawnPos(Location<World> location) {
            return this.spawnPos(location.getBlockPosition());
        }

        /**
         * The spawn position of this claim.
         * 
         * @param spawnPos The spawn position
         * @return The builder
         */
        Builder spawnPos(Vector3i spawnPos);

        /**
         * Resets the builder to default settings.
         * 
         * @return The builder
         */
        Builder reset();

        /**
         * Returns the {@link ClaimResult}.
         * 
         * @return The claim result
         */
        ClaimResult build();
    }
}
