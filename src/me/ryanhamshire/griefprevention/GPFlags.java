package me.ryanhamshire.griefprevention;

import com.google.common.collect.ImmutableSet;
import me.ryanhamshire.griefprevention.claim.Claim;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.util.Tristate;

import java.util.Set;

public class GPFlags {

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
    public static final String INVENTORY_ACCESS = "inventory-access";
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
    public static final String COMMENT_INVENTORY_ACCESS = "Allow/deny blocks with inventories.";
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
            Tristate value = user.getPermissionValue(contextSet, flag);
            if (value != Tristate.UNDEFINED) {
                return value;
            }
        }

        return getClaimFlagPermission(claim, flag);
    }

    // No user
    public static Tristate getClaimFlagPermission(Claim claim, String flag) {
        flag = flag.replace("griefprevention.flag.", "");
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
}
