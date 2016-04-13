package me.ryanhamshire.griefprevention;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import me.ryanhamshire.griefprevention.claim.Claim;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.util.Tristate;

import java.util.Map;
import java.util.Set;

public class GPFlags {

    public static Map<String, String> flagPermissions = Maps.newHashMap();

    // Names
    public static final String BLOCK_BREAK = "block-break";
    public static final String BLOCK_COMMANDS = "block-commands";
    public static final String BLOCK_PLACE = "block-place";
    public static final String EXPLOSIONS = "explosions";
    public static final String FIRE_SPREAD = "fire-spread";
    public static final String FORCE_DENY_ALL = "force-deny-all";
    public static final String IGNITE = "ignite";
    public static final String INTERACT_PRIMARY = "interact-primary";
    public static final String INTERACT_SECONDARY = "interact-secondary";
    public static final String INVENTORY = "inventory-access";
    public static final String ITEM_DROP = "item-drop";
    public static final String ITEM_PICKUP = "item-pickup";
    public static final String ITEM_USE = "item-use";
    public static final String LAVA_FLOW = "lava-flow";
    public static final String MOB_BLOCK_DAMAGE = "mob-block-damage";
    public static final String MOB_PLAYER_DAMAGE = "mob-player-damage";
    public static final String MOB_RIDING = "mob-riding";
    public static final String PORTAL_USE = "portal-use";
    public static final String PROJECTILES_ANY = "projectiles-any";
    public static final String PROJECTILES_MONSTER = "projectiles-monster";
    public static final String PROJECTILES_PLAYER = "projectiles-player";
    public static final String PVP = "pvp";
    public static final String SLEEP = "sleep";
    public static final String SPAWN_AMBIENTS = "spawn-ambient";
    public static final String SPAWN_ANY = "spawn-any";
    public static final String SPAWN_AQUATICS = "spawn-aquatic";
    public static final String SPAWN_MONSTERS = "spawn-monsters";
    public static final String SPAWN_PASSIVES = "spawn-passives";
    public static final String WATER_FLOW = "water-flow";
    public static final String VILLAGER_TRADING = "villager-trading";

    // Comments
    public static final String COMMENT_BLOCK_BREAK = "Allow/deny breaking blocks.";
    public static final String COMMENT_BLOCK_COMMANDS = "Blocked commands.";
    public static final String COMMENT_BLOCK_PLACE = "Allow/deny placing blocks.";
    public static final String COMMENT_EXPLOSIONS = "Allow/deny explosions.";
    public static final String COMMENT_FIRE_SPREAD = "Allow/deny fire spread.";
    public static final String COMMENT_FORCE_DENY_ALL = "Only intended if you want to explicitly ignore all checking for player permissions.";
    public static final String COMMENT_IGNITE = "Allow/deny ignites.";
    public static final String COMMENT_INTERACT_PRIMARY = "Allow/deny left-clicking.";
    public static final String COMMENT_INTERACT_SECONDARY = "Allow/deny right-clicking.";
    public static final String COMMENT_INVENTORY = "Allow/deny blocks with inventories.";
    public static final String COMMENT_ITEM_DROP = "Allow/deny item drops.";
    public static final String COMMENT_ITEM_PICKUP = "Allow/deny picking up items.";
    public static final String COMMENT_ITEM_USE = "Allow/deny item use.";
    public static final String COMMENT_LAVA_FLOW = "Allow/deny lava flow.";
    public static final String COMMENT_MOB_BLOCK_DAMAGE = "Allow/deny mob block damage.";
    public static final String COMMENT_MOB_PLAYER_DAMAGE = "Allow/deny mob player damage.";
    public static final String COMMENT_MOB_RIDING = "Allow/deny mob riding.";
    public static final String COMMENT_PORTAL_USE = "Allow/deny portal use.";
    public static final String COMMENT_PROJECTILES_ANY = "Allow/deny any projectiles.";
    public static final String COMMENT_PROJECTILES_MONSTER = "Allow/deny monster projectiles.";
    public static final String COMMENT_PROJECTILES_PLAYER = "Allow/deny player projectiles.";
    public static final String COMMENT_PVP = "Allow/deny pvp.";
    public static final String COMMENT_SLEEP = "Allow/deny sleep.";
    public static final String COMMENT_SPAWN_AMBIENTS = "Allow/deny the spawning of ambient mobs.";
    public static final String COMMENT_SPAWN_ANY = "Allow/deny the spawning of any mobs.";
    public static final String COMMENT_SPAWN_AQUATICS = "Allow/deny the spawning of aquatic mobs.";
    public static final String COMMENT_SPAWN_MONSTERS = "Allow/deny the spawning of monsters.";
    public static final String COMMENT_SPAWN_PASSIVES = "Allow/deny the spawning of passive mobs.";
    public static final String COMMENT_WATER_FLOW = "Allow/deny water flow.";
    public static final String COMMENT_VILLAGER_TRADING = "Allow/deny villager trading.";

    public static Tristate getClaimFlagPermission(User user, Claim claim, String flag) {
        if (GriefPrevention.instance.permPluginInstalled) {
            Set<Context> contextSet = ImmutableSet.of(claim.getContext());
            Tristate value = user.getPermissionValue(contextSet, flagPermissions.get(flag));
            if (value != Tristate.UNDEFINED) {
                return value;
            }
        }

        return getClaimFlagPermission(claim, flag);
    }

    // No user
    public static Tristate getClaimFlagPermission(Claim claim, String flag) {
        Tristate value = null;
        if (claim.isSubDivision) {
            value = (Tristate) claim.subDivisionData.flags.getFlagValue(flag);
        } else {
            value = (Tristate) claim.getClaimData().getConfig().flags.getFlagValue(flag);
        }

        if (value == Tristate.UNDEFINED) {
            Object obj = GriefPrevention.getActiveConfig(claim.world.getProperties()).getConfig().flags.getFlagValue(flag);
            if (obj != null) {
                return Tristate.fromBoolean((boolean) obj);
            }
        }

        if (value != null) {
            return value;
        }

        return Tristate.TRUE;
    }

    static {
        flagPermissions.put(BLOCK_BREAK, GPPermissions.BLOCK_BREAK);
        flagPermissions.put(BLOCK_COMMANDS, GPPermissions.BLOCK_COMMANDS);
        flagPermissions.put(BLOCK_PLACE, GPPermissions.BLOCK_PLACE);
        flagPermissions.put(EXPLOSIONS, GPPermissions.EXPLOSIONS);
        flagPermissions.put(FIRE_SPREAD, GPPermissions.FIRE_SPREAD);
        flagPermissions.put(FORCE_DENY_ALL, GPPermissions.FORCE_DENY_ALL);
        flagPermissions.put(INTERACT_PRIMARY, GPPermissions.INTERACT_PRIMARY);
        flagPermissions.put(INTERACT_SECONDARY, GPPermissions.INTERACT_SECONDARY);
        flagPermissions.put(ITEM_DROP, GPPermissions.ITEM_DROP);
        flagPermissions.put(ITEM_PICKUP, GPPermissions.ITEM_PICKUP);
        flagPermissions.put(ITEM_USE, GPPermissions.ITEM_USE);
        flagPermissions.put(LAVA_FLOW, GPPermissions.LAVA_FLOW);
        flagPermissions.put(MOB_BLOCK_DAMAGE, GPPermissions.MOB_BLOCK_DAMAGE);
        flagPermissions.put(MOB_PLAYER_DAMAGE, GPPermissions.MOB_PLAYER_DAMAGE);
        flagPermissions.put(MOB_RIDING, GPPermissions.MOB_RIDING);
        flagPermissions.put(PORTAL_USE, GPPermissions.PORTAL_USE);
        flagPermissions.put(PROJECTILES_ANY, GPPermissions.PROJECTILES_ANY);
        flagPermissions.put(PROJECTILES_MONSTER, GPPermissions.PROJECTILES_MONSTER);
        flagPermissions.put(PROJECTILES_PLAYER, GPPermissions.PROJECTILES_PLAYER);
        flagPermissions.put(PVP, GPPermissions.PVP);
        flagPermissions.put(SLEEP, GPPermissions.SLEEP);
        flagPermissions.put(SPAWN_AMBIENTS, GPPermissions.SPAWN_AMBIENTS);
        flagPermissions.put(SPAWN_ANY, GPPermissions.SPAWN_ANY);
        flagPermissions.put(SPAWN_AQUATICS, GPPermissions.SPAWN_AQUATICS);
        flagPermissions.put(SPAWN_MONSTERS, GPPermissions.SPAWN_MONSTERS);
        flagPermissions.put(SPAWN_PASSIVES, GPPermissions.SPAWN_PASSIVES);
        flagPermissions.put(WATER_FLOW, GPPermissions.WATER_FLOW);
        flagPermissions.put(VILLAGER_TRADING, GPPermissions.VILLAGER_TRADING);
    }
}
