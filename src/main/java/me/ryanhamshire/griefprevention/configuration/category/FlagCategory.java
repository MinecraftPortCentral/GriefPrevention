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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ConfigSerializable
public class FlagCategory extends ConfigCategory {

    @Setting(value = "default-admin", comment = "The default flag settings used when an admin claim is created.")
    private Map<String, Boolean> defaultAdminFlags = Maps.newHashMap();

    @Setting(value = "default-basic", comment = "The default flag settings used when a basic claim is created.")
    private Map<String, Boolean> defaultBasicFlags = Maps.newHashMap();

    @Setting(value = "default-town", comment = "The default flag settings used for towns.")
    private Map<String, Boolean> defaultTownFlags = Maps.newHashMap();

    @Setting(value = "default-wilderness", comment = "The default flag settings used for wilderness.")
    private Map<String, Boolean> defaultWildernessFlags = Maps.newHashMap();

    @Setting(value = "user-claim-flags", comment = "A list of flags standard users can manage in their claims with the /cf, /cfg, and /cfp commands.\n"
            + "Note: All flags in this list will be used to determine which user permission to check such as 'griefprevention.user.flag.block-break'.")
    private List<String> userClaimFlags = new ArrayList<>();

    public FlagCategory() {
        defaultAdminFlags.put(ClaimFlag.BLOCK_BREAK.toString(), false);
        defaultAdminFlags.put(ClaimFlag.BLOCK_PLACE.toString(), false);
        defaultAdminFlags.put(ClaimFlag.COMMAND_EXECUTE.toString(), true);
        defaultAdminFlags.put(ClaimFlag.COMMAND_EXECUTE_PVP.toString(), true);
        defaultAdminFlags.put(ClaimFlag.ENTER_CLAIM.toString(), true);
        defaultAdminFlags.put(ClaimFlag.ENTITY_CHUNK_SPAWN.toString(), true);
        defaultAdminFlags.put(ClaimFlag.ENTITY_COLLIDE_BLOCK.toString(), false);
        defaultAdminFlags.put(ClaimFlag.ENTITY_COLLIDE_ENTITY.toString(), false);
        defaultAdminFlags.put(ClaimFlag.ENTITY_DAMAGE.toString(), true);
        defaultAdminFlags.put(ClaimFlag.ENTITY_RIDING.toString(), true);
        defaultAdminFlags.put(ClaimFlag.ENTITY_SPAWN.toString(), true);
        defaultAdminFlags.put(ClaimFlag.ENTITY_TELEPORT_FROM.toString(), true);
        defaultAdminFlags.put(ClaimFlag.ENTITY_TELEPORT_TO.toString(), true);
        defaultAdminFlags.put(ClaimFlag.EXIT_CLAIM.toString(), true);
        defaultAdminFlags.put(ClaimFlag.EXPLOSION.toString(), false);
        defaultAdminFlags.put(ClaimFlag.EXPLOSION_SURFACE.toString(), false);
        defaultAdminFlags.put(ClaimFlag.FIRE_SPREAD.toString(), false);
        defaultAdminFlags.put(ClaimFlag.INTERACT_BLOCK_PRIMARY.toString(), false);
        defaultAdminFlags.put(ClaimFlag.INTERACT_BLOCK_SECONDARY.toString(), false);
        defaultAdminFlags.put(ClaimFlag.INTERACT_ENTITY_PRIMARY.toString(), false);
        defaultAdminFlags.put(ClaimFlag.INTERACT_ENTITY_SECONDARY.toString(), true);
        defaultAdminFlags.put(ClaimFlag.INTERACT_INVENTORY.toString(), false);
        defaultAdminFlags.put(ClaimFlag.INTERACT_ITEM_PRIMARY.toString(), true);
        defaultAdminFlags.put(ClaimFlag.INTERACT_ITEM_SECONDARY.toString(), true);
        defaultAdminFlags.put(ClaimFlag.INTERACT_INVENTORY_CLICK.toString(), true);
        defaultAdminFlags.put(ClaimFlag.ITEM_DROP.toString(), true);
        defaultAdminFlags.put(ClaimFlag.ITEM_PICKUP.toString(), true);
        defaultAdminFlags.put(ClaimFlag.ITEM_SPAWN.toString(), false);
        defaultAdminFlags.put(ClaimFlag.ITEM_USE.toString(), true);
        defaultAdminFlags.put(ClaimFlag.LEAF_DECAY.toString(), true);
        defaultAdminFlags.put(ClaimFlag.LIQUID_FLOW.toString(), false);
        defaultAdminFlags.put(ClaimFlag.PORTAL_USE.toString(), true);
        defaultAdminFlags.put(ClaimFlag.PROJECTILE_IMPACT_BLOCK.toString(), false);
        defaultAdminFlags.put(ClaimFlag.PROJECTILE_IMPACT_ENTITY.toString(), false);

        defaultBasicFlags.put(ClaimFlag.BLOCK_BREAK.toString(), false);
        defaultBasicFlags.put(ClaimFlag.BLOCK_PLACE.toString(), false);
        defaultBasicFlags.put(ClaimFlag.COMMAND_EXECUTE.toString(), true);
        defaultBasicFlags.put(ClaimFlag.COMMAND_EXECUTE_PVP.toString(), true);
        defaultBasicFlags.put(ClaimFlag.ENTER_CLAIM.toString(), true);
        defaultBasicFlags.put(ClaimFlag.ENTITY_CHUNK_SPAWN.toString(), true);
        defaultBasicFlags.put(ClaimFlag.ENTITY_COLLIDE_BLOCK.toString(), false);
        defaultBasicFlags.put(ClaimFlag.ENTITY_COLLIDE_ENTITY.toString(), false);
        defaultBasicFlags.put(ClaimFlag.ENTITY_DAMAGE.toString(), true);
        defaultBasicFlags.put(ClaimFlag.ENTITY_RIDING.toString(), true);
        defaultBasicFlags.put(ClaimFlag.ENTITY_SPAWN.toString(), true);
        defaultBasicFlags.put(ClaimFlag.ENTITY_TELEPORT_FROM.toString(), true);
        defaultBasicFlags.put(ClaimFlag.ENTITY_TELEPORT_TO.toString(), true);
        defaultBasicFlags.put(ClaimFlag.EXIT_CLAIM.toString(), true);
        defaultBasicFlags.put(ClaimFlag.EXPLOSION.toString(), false);
        defaultBasicFlags.put(ClaimFlag.EXPLOSION_SURFACE.toString(), false);
        defaultBasicFlags.put(ClaimFlag.FIRE_SPREAD.toString(), false);
        defaultBasicFlags.put(ClaimFlag.INTERACT_BLOCK_PRIMARY.toString(), false);
        defaultBasicFlags.put(ClaimFlag.INTERACT_BLOCK_SECONDARY.toString(), false);
        defaultBasicFlags.put(ClaimFlag.INTERACT_ENTITY_PRIMARY.toString(), false);
        defaultBasicFlags.put(ClaimFlag.INTERACT_ENTITY_SECONDARY.toString(), true);
        defaultBasicFlags.put(ClaimFlag.INTERACT_INVENTORY.toString(), false);
        defaultBasicFlags.put(ClaimFlag.INTERACT_ITEM_PRIMARY.toString(), false);
        defaultBasicFlags.put(ClaimFlag.INTERACT_ITEM_SECONDARY.toString(), false);
        defaultBasicFlags.put(ClaimFlag.INTERACT_INVENTORY_CLICK.toString(), true);
        defaultBasicFlags.put(ClaimFlag.ITEM_DROP.toString(), true);
        defaultBasicFlags.put(ClaimFlag.ITEM_PICKUP.toString(), true);
        defaultBasicFlags.put(ClaimFlag.ITEM_SPAWN.toString(), false);
        defaultBasicFlags.put(ClaimFlag.ITEM_USE.toString(), false);
        defaultBasicFlags.put(ClaimFlag.LEAF_DECAY.toString(), true);
        defaultBasicFlags.put(ClaimFlag.LIQUID_FLOW.toString(), false);
        defaultBasicFlags.put(ClaimFlag.PORTAL_USE.toString(), true);
        defaultBasicFlags.put(ClaimFlag.PROJECTILE_IMPACT_BLOCK.toString(), false);
        defaultBasicFlags.put(ClaimFlag.PROJECTILE_IMPACT_ENTITY.toString(), false);

        defaultTownFlags.put(ClaimFlag.BLOCK_BREAK.toString(), false);
        defaultTownFlags.put(ClaimFlag.BLOCK_PLACE.toString(), false);
        defaultTownFlags.put(ClaimFlag.COMMAND_EXECUTE.toString(), true);
        defaultTownFlags.put(ClaimFlag.COMMAND_EXECUTE_PVP.toString(), true);
        defaultTownFlags.put(ClaimFlag.ENTER_CLAIM.toString(), true);
        defaultTownFlags.put(ClaimFlag.ENTITY_CHUNK_SPAWN.toString(), true);
        defaultTownFlags.put(ClaimFlag.ENTITY_COLLIDE_BLOCK.toString(), false);
        defaultTownFlags.put(ClaimFlag.ENTITY_COLLIDE_ENTITY.toString(), false);
        defaultTownFlags.put(ClaimFlag.ENTITY_DAMAGE.toString(), true);
        defaultTownFlags.put(ClaimFlag.ENTITY_RIDING.toString(), true);
        defaultTownFlags.put(ClaimFlag.ENTITY_SPAWN.toString(), true);
        defaultTownFlags.put(ClaimFlag.ENTITY_TELEPORT_FROM.toString(), true);
        defaultTownFlags.put(ClaimFlag.ENTITY_TELEPORT_TO.toString(), true);
        defaultTownFlags.put(ClaimFlag.EXIT_CLAIM.toString(), true);
        defaultTownFlags.put(ClaimFlag.EXPLOSION.toString(), false);
        defaultTownFlags.put(ClaimFlag.EXPLOSION_SURFACE.toString(), false);
        defaultTownFlags.put(ClaimFlag.FIRE_SPREAD.toString(), false);
        defaultTownFlags.put(ClaimFlag.INTERACT_BLOCK_PRIMARY.toString(), false);
        defaultTownFlags.put(ClaimFlag.INTERACT_BLOCK_SECONDARY.toString(), false);
        defaultTownFlags.put(ClaimFlag.INTERACT_ENTITY_PRIMARY.toString(), false);
        defaultTownFlags.put(ClaimFlag.INTERACT_ENTITY_SECONDARY.toString(), true);
        defaultTownFlags.put(ClaimFlag.INTERACT_INVENTORY.toString(), false);
        defaultTownFlags.put(ClaimFlag.INTERACT_ITEM_PRIMARY.toString(), false);
        defaultTownFlags.put(ClaimFlag.INTERACT_ITEM_SECONDARY.toString(), false);
        defaultTownFlags.put(ClaimFlag.INTERACT_INVENTORY_CLICK.toString(), true);
        defaultTownFlags.put(ClaimFlag.ITEM_DROP.toString(), true);
        defaultTownFlags.put(ClaimFlag.ITEM_PICKUP.toString(), true);
        defaultTownFlags.put(ClaimFlag.ITEM_SPAWN.toString(), false);
        defaultTownFlags.put(ClaimFlag.ITEM_USE.toString(), false);
        defaultTownFlags.put(ClaimFlag.LEAF_DECAY.toString(), true);
        defaultTownFlags.put(ClaimFlag.LIQUID_FLOW.toString(), false);
        defaultTownFlags.put(ClaimFlag.PORTAL_USE.toString(), true);
        defaultTownFlags.put(ClaimFlag.PROJECTILE_IMPACT_BLOCK.toString(), false);
        defaultTownFlags.put(ClaimFlag.PROJECTILE_IMPACT_ENTITY.toString(), false);

        defaultWildernessFlags.put(ClaimFlag.BLOCK_BREAK.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.BLOCK_PLACE.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.COMMAND_EXECUTE.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.COMMAND_EXECUTE_PVP.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.ENTER_CLAIM.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.ENTITY_CHUNK_SPAWN.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.ENTITY_COLLIDE_BLOCK.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.ENTITY_COLLIDE_ENTITY.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.ENTITY_DAMAGE.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.ENTITY_RIDING.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.ENTITY_SPAWN.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.ENTITY_TELEPORT_FROM.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.ENTITY_TELEPORT_TO.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.EXIT_CLAIM.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.EXPLOSION.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.EXPLOSION_SURFACE.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.FIRE_SPREAD.toString(), false);
        defaultWildernessFlags.put(ClaimFlag.INTERACT_BLOCK_PRIMARY.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.INTERACT_BLOCK_SECONDARY.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.INTERACT_ENTITY_PRIMARY.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.INTERACT_ENTITY_SECONDARY.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.INTERACT_INVENTORY.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.INTERACT_ITEM_PRIMARY.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.INTERACT_ITEM_SECONDARY.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.INTERACT_INVENTORY_CLICK.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.ITEM_DROP.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.ITEM_PICKUP.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.ITEM_SPAWN.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.ITEM_USE.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.LEAF_DECAY.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.LIQUID_FLOW.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.PORTAL_USE.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.PROJECTILE_IMPACT_BLOCK.toString(), true);
        defaultWildernessFlags.put(ClaimFlag.PROJECTILE_IMPACT_ENTITY.toString(), true);

        userClaimFlags.add(ClaimFlag.BLOCK_BREAK.toString());
        userClaimFlags.add(ClaimFlag.BLOCK_PLACE.toString());
        userClaimFlags.add(ClaimFlag.ENTITY_COLLIDE_BLOCK.toString());
        userClaimFlags.add(ClaimFlag.ENTITY_RIDING.toString());
        userClaimFlags.add(ClaimFlag.EXPLOSION.toString());
        userClaimFlags.add(ClaimFlag.EXPLOSION_SURFACE.toString());
        userClaimFlags.add(ClaimFlag.FIRE_SPREAD.toString());
        userClaimFlags.add(ClaimFlag.INTERACT_BLOCK_PRIMARY.toString());
        userClaimFlags.add(ClaimFlag.INTERACT_BLOCK_SECONDARY.toString());
        userClaimFlags.add(ClaimFlag.INTERACT_ENTITY_PRIMARY.toString());
        userClaimFlags.add(ClaimFlag.INTERACT_ENTITY_SECONDARY.toString());
        userClaimFlags.add(ClaimFlag.INTERACT_ITEM_PRIMARY.toString());
        userClaimFlags.add(ClaimFlag.INTERACT_ITEM_SECONDARY.toString());
        userClaimFlags.add(ClaimFlag.INTERACT_INVENTORY_CLICK.toString());
        userClaimFlags.add(ClaimFlag.INTERACT_INVENTORY.toString());
        userClaimFlags.add(ClaimFlag.ITEM_DROP.toString());
        userClaimFlags.add(ClaimFlag.ITEM_PICKUP.toString());
        userClaimFlags.add(ClaimFlag.ITEM_USE.toString());
        userClaimFlags.add(ClaimFlag.LEAF_DECAY.toString());
        userClaimFlags.add(ClaimFlag.PORTAL_USE.toString());
        userClaimFlags.add(ClaimFlag.PROJECTILE_IMPACT_BLOCK.toString());
        userClaimFlags.add(ClaimFlag.PROJECTILE_IMPACT_ENTITY.toString());
    }

    public Map<String, Boolean> getAdminDefaults() {
        if (!this.defaultAdminFlags.containsKey(ClaimFlag.LEAF_DECAY.toString())) {
            this.defaultAdminFlags.put(ClaimFlag.LEAF_DECAY.toString(), true);
        }
        return this.defaultAdminFlags;
    }

    public Map<String, Boolean> getBasicDefaults() {
        if (!this.defaultBasicFlags.containsKey(ClaimFlag.LEAF_DECAY.toString())) {
            this.defaultBasicFlags.put(ClaimFlag.LEAF_DECAY.toString(), true);
        }
        return this.defaultBasicFlags;
    }

    public Map<String, Boolean> getTownDefaults() {
        if (!this.defaultTownFlags.containsKey(ClaimFlag.LEAF_DECAY.toString())) {
            this.defaultTownFlags.put(ClaimFlag.LEAF_DECAY.toString(), true);
        }
        return this.defaultTownFlags;
    }

    public Map<String, Boolean> getWildernessDefaults() {
        if (!this.defaultWildernessFlags.containsKey(ClaimFlag.LEAF_DECAY.toString())) {
            this.defaultWildernessFlags.put(ClaimFlag.LEAF_DECAY.toString(), true);
        }
        return this.defaultWildernessFlags;
    }

    public List<String> getUserClaimFlags() {
        return this.userClaimFlags;
    }
}
