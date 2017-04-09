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
import me.ryanhamshire.griefprevention.GPFlags;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ConfigSerializable
public class FlagCategory extends ConfigCategory {

    @Setting(value = "default-admin", comment = "The default flag settings used when an admin claim is created.")
    private Map<String, Boolean> defaultAdminFlags = Maps.newHashMap();

    @Setting(value = "default-basic", comment = "The default flag settings used when a basic claim is created.")
    private Map<String, Boolean> defaultBasicFlags = Maps.newHashMap();

    @Setting(value = "default-wilderness", comment = "The default flag settings used for wilderness.")
    private Map<String, Boolean> defaultWildernessFlags = Maps.newHashMap();

    @Setting(value = "user-claim-flags", comment = "A list of flags standard users can manage in their claims with the /cf, /cfg, and /cfp commands.\n"
            + "Note: All flags in this list will be used to determine which user permission to check such as 'griefprevention.user.flag.block-break'.")
    private List<String> userClaimFlags = new ArrayList<>();

    public FlagCategory() {
        defaultAdminFlags.put(GPFlags.BLOCK_BREAK, false);
        defaultAdminFlags.put(GPFlags.BLOCK_PLACE, false);
        defaultAdminFlags.put(GPFlags.COMMAND_EXECUTE, true);
        defaultAdminFlags.put(GPFlags.COMMAND_EXECUTE_PVP, true);
        defaultAdminFlags.put(GPFlags.ENTER_CLAIM, true);
        defaultAdminFlags.put(GPFlags.ENTITY_COLLIDE_BLOCK, false);
        defaultAdminFlags.put(GPFlags.ENTITY_COLLIDE_ENTITY, false);
        defaultAdminFlags.put(GPFlags.ENTITY_DAMAGE, true);
        defaultAdminFlags.put(GPFlags.ENTITY_FALL, true);
        defaultAdminFlags.put(GPFlags.ENTITY_RIDING, true);
        defaultAdminFlags.put(GPFlags.ENTITY_SPAWN, true);
        defaultAdminFlags.put(GPFlags.ENTITY_TELEPORT_FROM, true);
        defaultAdminFlags.put(GPFlags.ENTITY_TELEPORT_TO, true);
        defaultAdminFlags.put(GPFlags.EXIT_CLAIM, true);
        defaultAdminFlags.put(GPFlags.EXPLOSION, false);
        defaultAdminFlags.put(GPFlags.EXPLOSION_SURFACE, false);
        defaultAdminFlags.put(GPFlags.FIRE_SPREAD, false);
        defaultAdminFlags.put(GPFlags.INTERACT_BLOCK_PRIMARY, false);
        defaultAdminFlags.put(GPFlags.INTERACT_BLOCK_SECONDARY, false);
        defaultAdminFlags.put(GPFlags.INTERACT_ENTITY_PRIMARY, false);
        defaultAdminFlags.put(GPFlags.INTERACT_ENTITY_SECONDARY, true);
        defaultAdminFlags.put(GPFlags.INTERACT_INVENTORY, false);
        defaultAdminFlags.put(GPFlags.INTERACT_ITEM_PRIMARY, true);
        defaultAdminFlags.put(GPFlags.INTERACT_ITEM_SECONDARY, true);
        defaultAdminFlags.put(GPFlags.ITEM_DROP, true);
        defaultAdminFlags.put(GPFlags.ITEM_PICKUP, true);
        defaultAdminFlags.put(GPFlags.ITEM_SPAWN, false);
        defaultAdminFlags.put(GPFlags.ITEM_USE, true);
        defaultAdminFlags.put(GPFlags.LIQUID_FLOW, false);
        defaultAdminFlags.put(GPFlags.PORTAL_USE, true);
        defaultAdminFlags.put(GPFlags.PROJECTILE_IMPACT_BLOCK, false);
        defaultAdminFlags.put(GPFlags.PROJECTILE_IMPACT_ENTITY, false);

        defaultBasicFlags.put(GPFlags.BLOCK_BREAK, false);
        defaultBasicFlags.put(GPFlags.BLOCK_PLACE, false);
        defaultBasicFlags.put(GPFlags.COMMAND_EXECUTE, true);
        defaultBasicFlags.put(GPFlags.COMMAND_EXECUTE_PVP, true);
        defaultBasicFlags.put(GPFlags.ENTER_CLAIM, true);
        defaultBasicFlags.put(GPFlags.ENTITY_COLLIDE_BLOCK, false);
        defaultBasicFlags.put(GPFlags.ENTITY_COLLIDE_ENTITY, false);
        defaultBasicFlags.put(GPFlags.ENTITY_DAMAGE, true);
        defaultBasicFlags.put(GPFlags.ENTITY_FALL, true);
        defaultBasicFlags.put(GPFlags.ENTITY_RIDING, true);
        defaultBasicFlags.put(GPFlags.ENTITY_SPAWN, true);
        defaultBasicFlags.put(GPFlags.ENTITY_TELEPORT_FROM, true);
        defaultBasicFlags.put(GPFlags.ENTITY_TELEPORT_TO, true);
        defaultBasicFlags.put(GPFlags.EXIT_CLAIM, true);
        defaultBasicFlags.put(GPFlags.EXPLOSION, false);
        defaultBasicFlags.put(GPFlags.EXPLOSION_SURFACE, false);
        defaultBasicFlags.put(GPFlags.FIRE_SPREAD, false);
        defaultBasicFlags.put(GPFlags.INTERACT_BLOCK_PRIMARY, false);
        defaultBasicFlags.put(GPFlags.INTERACT_BLOCK_SECONDARY, false);
        defaultBasicFlags.put(GPFlags.INTERACT_ENTITY_PRIMARY, false);
        defaultBasicFlags.put(GPFlags.INTERACT_ENTITY_SECONDARY, true);
        defaultBasicFlags.put(GPFlags.INTERACT_INVENTORY, false);
        defaultBasicFlags.put(GPFlags.INTERACT_ITEM_PRIMARY, true);
        defaultBasicFlags.put(GPFlags.INTERACT_ITEM_SECONDARY, true);
        defaultBasicFlags.put(GPFlags.ITEM_DROP, true);
        defaultBasicFlags.put(GPFlags.ITEM_PICKUP, true);
        defaultBasicFlags.put(GPFlags.ITEM_SPAWN, false);
        defaultBasicFlags.put(GPFlags.ITEM_USE, true);
        defaultBasicFlags.put(GPFlags.LIQUID_FLOW, false);
        defaultBasicFlags.put(GPFlags.PORTAL_USE, true);
        defaultBasicFlags.put(GPFlags.PROJECTILE_IMPACT_BLOCK, false);
        defaultBasicFlags.put(GPFlags.PROJECTILE_IMPACT_ENTITY, false);

        defaultWildernessFlags.put(GPFlags.BLOCK_BREAK, true);
        defaultWildernessFlags.put(GPFlags.BLOCK_PLACE, true);;
        defaultWildernessFlags.put(GPFlags.COMMAND_EXECUTE, true);
        defaultWildernessFlags.put(GPFlags.COMMAND_EXECUTE_PVP, true);
        defaultWildernessFlags.put(GPFlags.ENTER_CLAIM, true);
        defaultWildernessFlags.put(GPFlags.ENTITY_COLLIDE_BLOCK, true);
        defaultWildernessFlags.put(GPFlags.ENTITY_COLLIDE_ENTITY, true);
        defaultWildernessFlags.put(GPFlags.ENTITY_DAMAGE, true);
        defaultWildernessFlags.put(GPFlags.ENTITY_FALL, true);
        defaultWildernessFlags.put(GPFlags.ENTITY_RIDING, true);
        defaultWildernessFlags.put(GPFlags.ENTITY_SPAWN, true);
        defaultWildernessFlags.put(GPFlags.ENTITY_TELEPORT_FROM, true);
        defaultWildernessFlags.put(GPFlags.ENTITY_TELEPORT_TO, true);
        defaultWildernessFlags.put(GPFlags.EXIT_CLAIM, true);
        defaultWildernessFlags.put(GPFlags.EXPLOSION, true);
        defaultWildernessFlags.put(GPFlags.EXPLOSION_SURFACE, true);
        defaultWildernessFlags.put(GPFlags.FIRE_SPREAD, false);
        defaultWildernessFlags.put(GPFlags.INTERACT_BLOCK_PRIMARY, true);
        defaultWildernessFlags.put(GPFlags.INTERACT_BLOCK_SECONDARY, true);
        defaultWildernessFlags.put(GPFlags.INTERACT_ENTITY_PRIMARY, true);
        defaultWildernessFlags.put(GPFlags.INTERACT_ENTITY_SECONDARY, true);
        defaultWildernessFlags.put(GPFlags.INTERACT_INVENTORY, true);
        defaultWildernessFlags.put(GPFlags.INTERACT_ITEM_PRIMARY, true);
        defaultWildernessFlags.put(GPFlags.INTERACT_ITEM_SECONDARY, true);
        defaultWildernessFlags.put(GPFlags.ITEM_DROP, true);
        defaultWildernessFlags.put(GPFlags.ITEM_PICKUP, true);
        defaultWildernessFlags.put(GPFlags.ITEM_SPAWN, true);
        defaultWildernessFlags.put(GPFlags.ITEM_USE, true);
        defaultWildernessFlags.put(GPFlags.LIQUID_FLOW, true);
        defaultWildernessFlags.put(GPFlags.PORTAL_USE, true);
        defaultWildernessFlags.put(GPFlags.PROJECTILE_IMPACT_BLOCK, true);
        defaultWildernessFlags.put(GPFlags.PROJECTILE_IMPACT_ENTITY, true);

        userClaimFlags.add(GPFlags.BLOCK_BREAK);
        userClaimFlags.add(GPFlags.BLOCK_PLACE);
        userClaimFlags.add(GPFlags.ENTITY_COLLIDE_BLOCK);
        userClaimFlags.add(GPFlags.ENTITY_RIDING);
        userClaimFlags.add(GPFlags.EXPLOSION);
        userClaimFlags.add(GPFlags.EXPLOSION_SURFACE);
        userClaimFlags.add(GPFlags.FIRE_SPREAD);
        userClaimFlags.add(GPFlags.INTERACT_BLOCK_PRIMARY);
        userClaimFlags.add(GPFlags.INTERACT_BLOCK_SECONDARY);
        userClaimFlags.add(GPFlags.INTERACT_ENTITY_PRIMARY);
        userClaimFlags.add(GPFlags.INTERACT_ENTITY_SECONDARY);
        userClaimFlags.add(GPFlags.INTERACT_ITEM_PRIMARY);
        userClaimFlags.add(GPFlags.INTERACT_ITEM_SECONDARY);
        userClaimFlags.add(GPFlags.ITEM_DROP);
        userClaimFlags.add(GPFlags.ITEM_PICKUP);
        userClaimFlags.add(GPFlags.ITEM_USE);
        userClaimFlags.add(GPFlags.PORTAL_USE);
        userClaimFlags.add(GPFlags.PROJECTILE_IMPACT_BLOCK);
        userClaimFlags.add(GPFlags.PROJECTILE_IMPACT_ENTITY);
    }

    public Map<String, Boolean> getAdminDefaults() {
        return this.defaultAdminFlags;
    }

    public Map<String, Boolean> getBasicDefaults() {
        return this.defaultBasicFlags;
    }

    public Map<String, Boolean> getWildernessDefaults() {
        return this.defaultWildernessFlags;
    }

    public List<String> getUserClaimFlags() {
        return this.userClaimFlags;
    }
}
