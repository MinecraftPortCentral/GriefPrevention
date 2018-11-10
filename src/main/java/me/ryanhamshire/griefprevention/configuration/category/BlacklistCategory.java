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

import me.ryanhamshire.griefprevention.api.claim.ClaimFlag;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

@ConfigSerializable
public class BlacklistCategory extends ConfigCategory {

    @Setting(value = "global-source", comment = "A global list of source id's that are ignored by events. \nNote: This only affects events where the id specified is the source.")
    public List<String> globalSourceBlacklist = new ArrayList<>();

    @Setting(value = "global-target", comment = "A global list of target id's that are ignored by events. \nNote: This only affects events where the id specified is the target.")
    public List<String> globalTargetBlacklist = new ArrayList<>();

    @Setting(value = "block-pre")
    public List<String> blacklistBlockPre = new ArrayList<>();

    @Setting(value = "block-break")
    public List<String> blacklistBlockBreak = new ArrayList<>();

    @Setting(value = "block-notify")
    public List<String> blacklistBlockNotify = new ArrayList<>();

    @Setting(value = "block-place")
    public List<String> blacklistBlockPlace = new ArrayList<>();

    @Setting(value = "command-execute")
    public List<String> blacklistCommandExecute = new ArrayList<>();

    @Setting(value = "command-execute-pvp")
    public List<String> blacklistCommandExecutePvp = new ArrayList<>();

    @Setting(value = "enter-claim")
    public List<String> blacklistEnterClaim = new ArrayList<>();

    @Setting(value = "entity-chunk-spawn")
    public List<String> blacklistEntityChunkSpawn = new ArrayList<>();

    @Setting(value = "entity-collide-block")
    public List<String> blacklistEntityCollideBlock = new ArrayList<>();

    @Setting(value = "entity-collide-entity")
    public List<String> blacklistEntityCollideEntity = new ArrayList<>();

    @Setting(value = "entity-damage")
    public List<String> blacklistEntityDamage = new ArrayList<>();

    @Setting(value = "entity-riding")
    public List<String> blacklistEntityRiding = new ArrayList<>();

    @Setting(value = "entity-spawn")
    public List<String> blacklistEntitySpawn = new ArrayList<>();

    @Setting(value = "entity-teleport-from")
    public List<String> blacklistEntityTeleportFrom = new ArrayList<>();

    @Setting(value = "entity-teleport-to")
    public List<String> blacklistEntityTeleportTo = new ArrayList<>();

    @Setting(value = "exit-claim")
    public List<String> blacklistExitClaim = new ArrayList<>();

    @Setting(value = "explosion")
    public List<String> blacklistExplosion = new ArrayList<>();

    @Setting(value = "explosion-surface")
    public List<String> blacklistExplosionSurface = new ArrayList<>();

    @Setting(value = "fire-spread")
    public List<String> blacklistFireSpread = new ArrayList<>();

    @Setting(value = "interact-block-primary")
    public List<String> blacklistInteractBlockPrimary = new ArrayList<>();

    @Setting(value = "interact-block-secondary")
    public List<String> blacklistInteractBlockSecondary = new ArrayList<>();

    @Setting(value = "interact-entity-primary")
    public List<String> blacklistInteractEntityPrimary = new ArrayList<>();

    @Setting(value = "interact-entity-secondary")
    public List<String> blacklistInteractEntitySecondary = new ArrayList<>();

    @Setting(value = "interact-inventory")
    public List<String> blacklistInteractInventory = new ArrayList<>();

    @Setting(value = "interact-item-primary")
    public List<String> blacklistInteractItemPrimary = new ArrayList<>();

    @Setting(value = "interact-item-secondary")
    public List<String> blacklistInteractItemSecondary = new ArrayList<>();

    @Setting(value = "interact-inventory-click")
    public List<String> blacklistInteractInventoryClick = new ArrayList<>();

    @Setting(value = "item-drop")
    public List<String> blacklistItemDrop = new ArrayList<>();

    @Setting(value = "item-pickup")
    public List<String> blacklistItemPickup = new ArrayList<>();

    @Setting(value = "item-spawn")
    public List<String> blacklistItemSpawn = new ArrayList<>();

    @Setting(value = "item-use")
    public List<String> blacklistItemUse = new ArrayList<>();

    @Setting(value = "leaf-decay")
    public List<String> blacklistLeafDecay = new ArrayList<>();

    @Setting(value = "liquid-flow")
    public List<String> blacklistLiquidFlow = new ArrayList<>();

    @Setting(value = "portal-use")
    public List<String> blacklistPortalUse = new ArrayList<>();

    @Setting(value = "projectile-impact-block")
    public List<String> blacklistProjectileImpactBlock = new ArrayList<>();

    @Setting(value = "projectile-impact-entity")
    public List<String> blacklistProjectileImpactEntity = new ArrayList<>();

    public List<String> getGlobalSourceBlacklist() {
        return this.globalSourceBlacklist;
    }

    public List<String> getGlobalTargetBlacklist() {
        return this.globalTargetBlacklist;
    }

    // Used by API
    @Nullable
    public List<String> getFlagBlacklist(String flag) {
        if (flag.equalsIgnoreCase("block-pre")) {
            return this.blacklistBlockPre;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.BLOCK_BREAK.toString())) {
            return this.blacklistBlockBreak;
        }
        if (flag.equalsIgnoreCase("block-notify")) {
            return this.blacklistBlockNotify;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.BLOCK_PLACE.toString())) {
            return this.blacklistBlockPlace;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.COMMAND_EXECUTE.toString())) {
            return this.blacklistCommandExecute;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.COMMAND_EXECUTE_PVP.toString())) {
            return this.blacklistCommandExecutePvp;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.ENTER_CLAIM.toString())) {
            return this.blacklistEnterClaim;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.ENTITY_CHUNK_SPAWN.toString())) {
            return this.blacklistEntityChunkSpawn;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.ENTITY_COLLIDE_BLOCK.toString())) {
            return this.blacklistEntityCollideBlock;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.ENTITY_COLLIDE_ENTITY.toString())) {
            return this.blacklistEntityCollideEntity;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.ENTITY_DAMAGE.toString())) {
            return this.blacklistEntityDamage;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.ENTITY_RIDING.toString())) {
            return this.blacklistEntityRiding;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.ENTITY_SPAWN.toString())) {
            return this.blacklistEntitySpawn;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.ENTITY_TELEPORT_FROM.toString())) {
            return this.blacklistEntityTeleportFrom;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.ENTITY_TELEPORT_TO.toString())) {
            return this.blacklistEntityTeleportTo;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.EXIT_CLAIM.toString())) {
            return this.blacklistExitClaim;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.EXPLOSION.toString())) {
            return this.blacklistExplosion;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.EXPLOSION_SURFACE.toString())) {
            return this.blacklistExplosionSurface;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.FIRE_SPREAD.toString())) {
            return this.blacklistFireSpread;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.INTERACT_BLOCK_PRIMARY.toString())) {
            return this.blacklistInteractBlockPrimary;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.INTERACT_BLOCK_SECONDARY.toString())) {
            return this.blacklistInteractBlockSecondary;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.INTERACT_ENTITY_PRIMARY.toString())) {
            return this.blacklistInteractEntityPrimary;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.INTERACT_ENTITY_SECONDARY.toString())) {
            return this.blacklistInteractEntitySecondary;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.INTERACT_INVENTORY.toString())) {
            return this.blacklistInteractInventory;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.INTERACT_ITEM_PRIMARY.toString())) {
            return this.blacklistInteractItemPrimary;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.INTERACT_ITEM_SECONDARY.toString())) {
            return this.blacklistInteractItemSecondary;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.INTERACT_INVENTORY_CLICK.toString())) {
            return this.blacklistInteractInventoryClick;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.ITEM_DROP.toString())) {
            return this.blacklistItemDrop;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.ITEM_PICKUP.toString())) {
            return this.blacklistItemPickup;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.ITEM_SPAWN.toString())) {
            return this.blacklistItemSpawn;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.ITEM_USE.toString())) {
            return this.blacklistItemUse;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.LEAF_DECAY.toString())) {
            return this.blacklistLeafDecay;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.LIQUID_FLOW.toString())) {
            return this.blacklistLiquidFlow;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.PORTAL_USE.toString())) {
            return this.blacklistPortalUse;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.PROJECTILE_IMPACT_BLOCK.toString())) {
            return this.blacklistProjectileImpactBlock;
        }
        if (flag.equalsIgnoreCase(ClaimFlag.PROJECTILE_IMPACT_ENTITY.toString())) {
            return this.blacklistProjectileImpactEntity;
        }

        return null;
    }
}
