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
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.service.context.ContextSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
     * Note: {@link Type#ADMIN} and {@link Type#WILDERNESS} claims do not have
     * owners. These claims should return a general name such as 'administrator'.
     * 
     * @return The name of claim owner, if available
     */
    String getOwnerName();

    /**
     * Gets the claim's parent.
     * 
     * Note: Only {@link ClaimType#SUBDIVISION}'s have parent's.
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
     * Gets the claim's area in blocks.
     * 
     * @return The area of claim
     */
    int getArea();

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

    default ClaimResult convertToType(ClaimType type) {
        return convertToType(type, Optional.empty());
    }

    /**
     * Attempts to convert claim to {@link ClaimType#ADMIN} type.
     * 
     * Note: Both {@link ClaimType#WILDERNESS} and {@link ClaimType#SUBDIVISION} cannot be converted
     * to {@link ClaimType#ADMIN}.
     * If changing a {@link ClaimType#ADMIN} to {@link ClaimType#BASIC}, owner is required.
     * 
     * @param type The new claim type
     * @param owner The owner to set
     * @return The claim result
     */
    ClaimResult convertToType(ClaimType type, Optional<UUID> owner);

    /**
     * Resizes a claim.
     * 
     * @param startCornerLoc The start corner location
     * @param endCornerLoc The end corner location
     * @return
     */
    default ClaimResult resize(Location<World> startCornerLoc, Location<World> endCornerLoc, Cause cause) {
        return this.resize(startCornerLoc.getBlockPosition(), endCornerLoc.getBlockPosition(), cause);
    }

    /**
     * Resizes a claim.
     * 
     * @param startCornerPos The start corner block position
     * @param endCornerPos The end corner block position
     * @return
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
     * Creates a subdivision.
     * 
     * @param point1 The first point
     * @param point2 The second point
     * @param owner The owner of claim
     * @param cuboid Whether claim is 3D
     * @return The claim result
     */
    ClaimResult createSubdivision(Vector3i point1, Vector3i point2, UUID owner, boolean cuboid, Cause cause);

    /**
     * Gets an immutable list of subdivisions.
     * 
     * Note: This will return an empty list if no subdivisions
     * are found.
     * 
     * @return The immutable list of subdivisions
     */
    List<Claim> getSubdivisions();

    /**
     * Gets an immutable list of all trusted users.
     * 
     * @return An immutable list of all trusted users
     */
    List<UUID> getAllTrusts();

    /**
     * Gets an immutable list of trusted users for {@link TrustType}.
     * 
     * @return An immutable list of trusted users
     */
    List<UUID> getTrusts(TrustType type);

    /**
     * Grants claim trust to the UUID for given {@link TrustType}.
     * 
     * @param uuid The UUID of user
     * @param type The trust type
     * @param cause The plugin cause
     * @return The claim result
     */
    ClaimResult addTrust(UUID uuid, TrustType type, Cause cause);

    /**
     * Grants claim trust to the list of UUID's for given {@link TrustType}.
     * 
     * @param uuid The list of user UUID's
     * @param type The trust type
     * @param cause The plugin cause
     * @return The claim result
     */
    ClaimResult addTrusts(List<UUID> uuid, TrustType type, Cause cause);

    /**
     * Removes UUID from claim trust for given {@link TrustType}.
     * 
     * @param uuid The UUID of user
     * @param type The trust type
     * @param cause The plugin cause
     * @return The claim result
     */
    ClaimResult removeTrust(UUID uuid, TrustType type, Cause cause);

    /**
     * Removes the list of UUID's from claim trust for given {@link TrustType}.
     * 
     * @param uuid The list of user UUID's
     * @param type The trust type
     * @param cause The plugin cause
     * @return The claim result
     */
    ClaimResult removeTrusts(List<UUID> uuid, TrustType type, Cause cause);

    /**
     * Clears all trusts for claim.
     * 
     * @param cause The plugin cause
     * @return The claim result
     */
    ClaimResult removeAllTrusts(Cause cause);

    default boolean isAdminClaim() {
        return this.getType() == ClaimType.ADMIN;
    }

    default boolean isBasicClaim() {
        return this.getType() == ClaimType.BASIC;
    }

    default boolean isSubdivision() {
        return this.getType() == ClaimType.SUBDIVISION;
    }

    default boolean isWilderness() {
        return this.getType() == ClaimType.WILDERNESS;
    }

    /**
     * Checks if the location is within this claim.
     * 
     * Note: If ignoreHeight is true, this will only check
     * x and z coordinates of location. This is usually done
     * for 2D claims.
     * 
     * @param location
     * @param ignoreHeight
     * @param excludeSubdivisions
     * @return Whether this claim contains the passed location
     */
    boolean contains(Location<World> location, boolean ignoreHeight, boolean excludeSubdivisions);

    /**
     * Checks if this claim overlaps another.
     * 
     * @param otherClaim The other claim to check
     * 
     * @return Whether this claim overlaps the other claim
     */
    boolean overlaps(Claim otherClaim);

    /**
     * Extends a claim downward.
     * 
     * Note: By default, 2D claims do not extend to bedrock.
     * 
     * @param newDepth The new depth
     * @return Whether the extension was successful
     */
    boolean extend(int newDepth);

    /**
     * Deletes the subdivision.
     * 
     * @param subdivision The subdivision to be deleted
     * @return The result of deletion
     */
    ClaimResult deleteSubdivision(Claim subdivision, Cause cause);

    /**
     * Gets the persisted data of claim.
     * 
     * @return The claim's persisted data
     */
    ClaimData getData();

    /**
     * Gets the {@link Type} of claim.
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
     * Note: {@link Type#ADMIN} and {@link Type#WILDERNESS} claims do not have
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

    /**
     * Gets a new claim builder instance for {@link Builder}.
     * 
     * @return A new claim builder instance
     */
    public static Claim.Builder builder() {
        return GriefPrevention.getApi().createClaimBuilder();
    }

    public interface Builder {

        Builder cause(Cause cause);

        Builder cuboid(boolean cuboid);

        default Builder bounds(Location<World> loc1, Location<World> loc2) {
            return this.bounds(loc1.getBlockPosition(), loc2.getBlockPosition());
        }

        Builder bounds(Vector3i point1, Vector3i point2);

        Builder owner(UUID ownerUniqueId);

        Builder parent(Claim parentClaim);

        Builder type(ClaimType type);

        Builder world(World world);

        Builder sizeRestrictions(boolean sizeRestrictions);

        Builder requiresClaimBlocks(boolean requiresClaimBlocks);

        Builder reset();

        ClaimResult build();
    }
}
