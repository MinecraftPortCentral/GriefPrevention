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

/**
 * Contains all flags used in GriefPrevention.
 */
public enum ClaimFlag {

    /**
     * Used to allow or deny a block break.
     */
    BLOCK_BREAK("block-break"),
    /**
     * Used to allow or deny a block place.
     */
    BLOCK_PLACE("block-place"),
    /**
     * Used to allow or deny a user on a command execution.
     */
    COMMAND_EXECUTE("command-execute"),
    /**
     * Used to allow or deny a user on a command execution during PvP.
     */
    COMMAND_EXECUTE_PVP("command-execute-pvp"),
    /**
     * Used to allow or deny entering a {@link Claim}.
     */
    ENTER_CLAIM("enter-claim"),
    /**
     * Used to allow or deny an entity spawn during chunk load.
     */
    ENTITY_CHUNK_SPAWN("entity-chunk-spawn"),
    /**
     * Used to allow or deny a collision with a block.
     */
    ENTITY_COLLIDE_BLOCK("collide-block"),
    /**
     * Used to allow or deny a collision with an entity.
     */
    ENTITY_COLLIDE_ENTITY("collide-entity"),
    /**
     * Used to allow or deny damage to an entity.
     */
    ENTITY_DAMAGE("entity-damage"),
    /**
     * Used to allow or deny mounting an entity.
     */
    ENTITY_RIDING("entity-riding"),
    /**
     * Used to allow or deny an entity spawn.
     * 
     * Note: This not include items, see {@link #ITEM_SPAWN}
     */
    ENTITY_SPAWN("entity-spawn"),
    /**
     * Used to allow or deny a teleport from a {@link Claim}.
     */
    ENTITY_TELEPORT_FROM("entity-teleport-from"),
    /**
     * Used to allow or deny a teleport to a {@link Claim}.
     */
    ENTITY_TELEPORT_TO("entity-teleport-to"),
    /**
     * Used to allow or deny exiting a {@link Claim}.
     */
    EXIT_CLAIM("exit-claim"),
    /**
     * Used to allow or deny an explosion from breaking one or more block(s).
     */
    EXPLOSION("explosion"),
    /**
     * Used to allow or deny an explosion, above the surface, from breaking one or more block(s).
     */
    EXPLOSION_SURFACE("explosion-surface"),
    /**
     * Used to allow or deny fire spreading.
     */
    FIRE_SPREAD("fire-spread"),
    /**
     * Used to allow or deny a player from left-clicking a block.
     */
    INTERACT_BLOCK_PRIMARY("interact-block-primary"),
    /**
     * Used to allow or deny a player from right-clicking a block.
     */
    INTERACT_BLOCK_SECONDARY("interact-block-secondary"),
    /**
     * Used to allow or deny a player from left-clicking an entity.
     */
    INTERACT_ENTITY_PRIMARY("interact-entity-primary"),
    /**
     * Used to allow or deny a player from right-clicking an entity.
     */
    INTERACT_ENTITY_SECONDARY("interact-entity-secondary"),
    /**
     * Used to allow or deny a user from opening an inventory container.
     */
    INTERACT_INVENTORY("interact-inventory"),
    /**
     * Used to allow or deny a user from clicking an item in an inventory slot.
     */
    INTERACT_INVENTORY_CLICK("interact-inventory-click"),
    /**
     * Used to allow or deny a player from left-clicking with an item in hand.
     */
    INTERACT_ITEM_PRIMARY("interact-item-primary"),
    /**
     * Used to allow or deny a player from right-clicking with an item in hand.
     */
    INTERACT_ITEM_SECONDARY("interact-item-secondary"),
    /**
     * Used to allow or deny an item from dropping into the world.
     */
    ITEM_DROP("item-drop"),
    /**
     * Used to allow or deny an item from being picked up.
     */
    ITEM_PICKUP("item-pickup"),
    /**
     * Used to allow or deny an item spawn.
     */
    ITEM_SPAWN("item-spawn"),
    /**
     * Used to allow or deny an item from being used.
     * 
     * Note: This only affects items that have durations such as food.
     */
    ITEM_USE("item-use"),
    /**
     * Used to allow or deny leaves from decaying.
     */
    LEAF_DECAY("leaf-decay"),
    /**
     * Used to allow or deny liquid from flowing.
     */
    LIQUID_FLOW("liquid-flow"),
    /**
     * Used to allow or deny a portal from being used.
     * 
     * Note: This may also require using {@link #ENTITY_COLLIDE_BLOCK}.
     */
    PORTAL_USE("portal-use"),
    /**
     * Used to allow or deny a projectile from hitting a block.
     */
    PROJECTILE_IMPACT_BLOCK("projectile-impact-block"),
    /**
     * Used to allow or deny a projectile from hitting an entity.
     */
    PROJECTILE_IMPACT_ENTITY("projectile-impact-entity");

    private final String flag;

    private ClaimFlag(final String flag) {
        this.flag = flag;
    }

    public static boolean contains(String value) {
        value = value.replace("griefprevention.flag.", "");
        String[] parts = value.split("\\.");
        if (parts.length > 0) {
            value = parts[0];
        }
        for (ClaimFlag claimFlag : values()) {
            if (claimFlag.name().equalsIgnoreCase(value) || claimFlag.flag.equalsIgnoreCase(value)) {
                return true;
            }
        }

        return false;
    }

    public static ClaimFlag getEnum(String value) {
        value = value.replace("griefprevention.flag.", "");
        String[] parts = value.split("\\.");
        if (parts.length > 0) {
            value = parts[0];
        }
        for (ClaimFlag claimFlag : values()) {
            if (claimFlag.name().equalsIgnoreCase(value) || claimFlag.flag.equalsIgnoreCase(value)) {
                return claimFlag;
            }
        }
        throw new IllegalArgumentException();
    }

    @Override
    public String toString() {
        return this.flag;
    }
}
