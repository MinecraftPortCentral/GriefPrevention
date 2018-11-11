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

import com.google.common.collect.Maps;
import me.ryanhamshire.griefprevention.api.claim.ClaimFlag;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.Map;

@ConfigSerializable
public class ModuleCategory {

    @Setting(value = "protection", comment = "Controls which protection modules are enabled." 
            + "\nNote: If you want full protection, it is recommended to keep everything enabled.")
    private Map<String, Boolean> protection = Maps.newHashMap();

    public ModuleCategory() {
        protection.put(ClaimFlag.BLOCK_BREAK.toString(), true);
        protection.put(ClaimFlag.BLOCK_PLACE.toString(), true);
        protection.put(ClaimFlag.COMMAND_EXECUTE.toString(), true);
        protection.put(ClaimFlag.COMMAND_EXECUTE_PVP.toString(), true);
        protection.put(ClaimFlag.ENTER_CLAIM.toString(), true);
        protection.put(ClaimFlag.ENTITY_CHUNK_SPAWN.toString(), true);
        protection.put(ClaimFlag.ENTITY_COLLIDE_BLOCK.toString(), true);
        protection.put(ClaimFlag.ENTITY_COLLIDE_ENTITY.toString(), true);
        protection.put(ClaimFlag.ENTITY_DAMAGE.toString(), true);
        protection.put(ClaimFlag.ENTITY_RIDING.toString(), true);
        protection.put(ClaimFlag.ENTITY_SPAWN.toString(), true);
        protection.put(ClaimFlag.ENTITY_TELEPORT_FROM.toString(), true);
        protection.put(ClaimFlag.ENTITY_TELEPORT_TO.toString(), true);
        protection.put(ClaimFlag.EXIT_CLAIM.toString(), true);
        protection.put(ClaimFlag.EXPLOSION.toString(), true);
        protection.put(ClaimFlag.EXPLOSION_SURFACE.toString(), true);
        protection.put(ClaimFlag.FIRE_SPREAD.toString(), true);
        protection.put(ClaimFlag.INTERACT_BLOCK_PRIMARY.toString(), true);
        protection.put(ClaimFlag.INTERACT_BLOCK_SECONDARY.toString(), true);
        protection.put(ClaimFlag.INTERACT_ENTITY_PRIMARY.toString(), true);
        protection.put(ClaimFlag.INTERACT_ENTITY_SECONDARY.toString(), true);
        protection.put(ClaimFlag.INTERACT_INVENTORY.toString(), true);
        protection.put(ClaimFlag.INTERACT_ITEM_PRIMARY.toString(), true);
        protection.put(ClaimFlag.INTERACT_ITEM_SECONDARY.toString(), true);
        protection.put(ClaimFlag.INTERACT_INVENTORY_CLICK.toString(), true);
        protection.put(ClaimFlag.ITEM_DROP.toString(), true);
        protection.put(ClaimFlag.ITEM_PICKUP.toString(), true);
        protection.put(ClaimFlag.ITEM_SPAWN.toString(), true);
        protection.put(ClaimFlag.ITEM_USE.toString(), true);
        protection.put(ClaimFlag.LEAF_DECAY.toString(), true);
        protection.put(ClaimFlag.LIQUID_FLOW.toString(), true);
        protection.put(ClaimFlag.PORTAL_USE.toString(), true);
        protection.put(ClaimFlag.PROJECTILE_IMPACT_BLOCK.toString(), true);
        protection.put(ClaimFlag.PROJECTILE_IMPACT_ENTITY.toString(), true);
    }

    public boolean isProtectionModuleEnabled(String flag) {
        final Boolean result = this.protection.get(flag);
        if (result == null) {
            if (flag.equals(ClaimFlag.LEAF_DECAY.toString())) {
                protection.put(ClaimFlag.LEAF_DECAY.toString(), true);
                return true;
            }
            return false;
        }

        return result;
    }
}
