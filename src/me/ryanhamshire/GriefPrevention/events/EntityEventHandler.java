/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) Ryan Hamshire
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
package me.ryanhamshire.GriefPrevention.events;

import com.google.common.collect.ImmutableSet;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimsMode;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GPPermissions;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import me.ryanhamshire.GriefPrevention.configuration.ClaimStorageData;
import me.ryanhamshire.GriefPrevention.configuration.GriefPreventionConfig;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.world.Explosion;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.effect.potion.PotionEffectType;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntitySnapshot;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.explosive.Explosive;
import org.spongepowered.api.entity.living.Ambient;
import org.spongepowered.api.entity.living.Aquatic;
import org.spongepowered.api.entity.living.Creature;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.Villager;
import org.spongepowered.api.entity.living.monster.Monster;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

//handles events related to entities
public class EntityEventHandler {

    // convenience reference for the singleton datastore
    private DataStore dataStore;

    public EntityEventHandler(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Listener(order = Order.EARLY)
    public void onChangeBlockBreak(ChangeBlockEvent.Break event) {
        final Cause cause = event.getCause();
        final Entity entity = cause.first(Entity.class).orElse(null);
        if (entity != null) {
            final EntityType entityType = entity.getType();
            for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
                final BlockType originalType = transaction.getOriginal().getState().getType();
                final BlockType finalType = transaction.getFinal().getState().getType();

                if (entityType.equals(EntityTypes.ENDERMAN)) {
                    if (!GriefPrevention.instance.config_endermenMoveBlocks) {
                        transaction.setValid(false);
                    } else {
                        // and the block is claimed
                        if (this.dataStore.getClaimAt(transaction.getFinal().getLocation().get(), false, null) != null) {
                            // he doesn't get to steal it
                            transaction.setValid(false);
                        }
                    }
                } else if (!GriefPrevention.instance.config_silverfishBreakBlocks && entityType.equals(EntityTypes.SILVERFISH)) {
                    transaction.setValid(false);
                } else if (!GriefPrevention.instance.claimModeIsActive(event.getTargetWorld().getProperties(), ClaimsMode.Disabled) && entityType
                        .equals(EntityTypes.WITHER)) {
                    transaction.setValid(false);
                } else if (!GriefPrevention.instance.config_zombiesBreakDoors && transaction.getOriginal().get(Keys.HINGE_POSITION).isPresent() &&
                        entityType.equals(EntityTypes.ZOMBIE)) {
                    transaction.setValid(false);
                } else if (finalType.equals(BlockTypes.DIRT) && originalType.equals(BlockTypes.FARMLAND)) {
                    if (!GriefPrevention.instance.config_creaturesTrampleCrops) {
                        transaction.setValid(false);
                    } else {
                        final Optional<EntitySnapshot> optPassenger = entity.get(Keys.PASSENGER);
                        if (optPassenger.isPresent() && optPassenger.get().getType().equals(EntityTypes.PLAYER)) {
                            transaction.setValid(false);
                        }
                    }
                }
            }
        }
    }

    @Listener
    public void onExplosion(ExplosionEvent.Pre event) {
        Claim claim =  GriefPrevention.instance.dataStore.getClaimAt(event.getTargetWorld().getLocation(event.getExplosion().getOrigin()), false, null);

        if (claim == null) {
            return;
        }

        net.minecraft.entity.Entity mcEntity = ((Explosion) event.getExplosion()).exploder;
        boolean checked = false;

        if (mcEntity != null) {
            Entity entity = (Entity) mcEntity;

            Optional<UUID> uuid = entity.getCreator();
            if (uuid.isPresent()) {
                Optional<User> user = Sponge.getServiceManager().provide(UserStorageService.class).get().get(uuid.get());
                if(user.isPresent()) {
                    Tristate value = user.get().getPermissionValue(ImmutableSet.of(claim.getContext()), GPPermissions.EXPLOSIONS);
                    if (value != Tristate.UNDEFINED) {
                        event.setCancelled(!value.asBoolean());
                        return;
                    }
                };
            }
        }

        event.setCancelled(!claim.getClaimData().getConfig().flags.explosions);
    }

    // when a creature spawns...
    @Listener(order = Order.EARLY)
    public void onSpawnEntity(SpawnEntityEvent event) {
        Optional<User> user = event.getCause().first(User.class);
        event.filterEntities(new Predicate<Entity>() {
            @Override
            public boolean test(Entity entity) {
                Claim claim = GriefPrevention.instance.dataStore.getClaimAt(entity.getLocation(), false, null);
                if (claim != null) {
                    if (user.isPresent() && GriefPrevention.instance.permPluginInstalled) {
                        net.minecraft.entity.Entity nmsEntity = (net.minecraft.entity.Entity) entity;
                        User spongeUser = user.get();
                        Set contextSet = ImmutableSet.of(claim.getContext());

                        if (spongeUser.getPermissionValue(contextSet, GPPermissions.SPAWN_ANY) != Tristate.UNDEFINED) {
                            return spongeUser.getPermissionValue(contextSet, GPPermissions.SPAWN_ANY) == Tristate.TRUE;
                        } else if (nmsEntity.isCreatureType(EnumCreatureType.AMBIENT, false)
                                && spongeUser.getPermissionValue(contextSet, GPPermissions
                                .SPAWN_AMBIENTS) != Tristate.UNDEFINED) {
                            return spongeUser.getPermissionValue(contextSet, GPPermissions.SPAWN_AMBIENTS) == Tristate.TRUE;
                        } else if (nmsEntity.isCreatureType(EnumCreatureType.WATER_CREATURE, false) && spongeUser.getPermissionValue(contextSet,
                                GPPermissions.SPAWN_AQUATICS) != Tristate.UNDEFINED) {
                            return spongeUser.getPermissionValue(contextSet, GPPermissions.SPAWN_AQUATICS) == Tristate.TRUE;
                        } else if (nmsEntity.isCreatureType(EnumCreatureType.MONSTER, false)
                                && spongeUser.getPermissionValue(contextSet, GPPermissions
                                .SPAWN_MONSTERS) != Tristate.UNDEFINED) {
                            return spongeUser.getPermissionValue(contextSet, GPPermissions.SPAWN_MONSTERS) == Tristate.TRUE;
                        } else if (nmsEntity.isCreatureType(EnumCreatureType.CREATURE, false)
                                && spongeUser.getPermissionValue(contextSet, GPPermissions
                                .SPAWN_PASSIVES) != Tristate.UNDEFINED) {
                            return spongeUser.getPermissionValue(contextSet, GPPermissions.SPAWN_PASSIVES) == Tristate.TRUE;
                        }
                    }

                    ClaimStorageData.ClaimDataNode claimStorageData = claim.getClaimData().getConfig();
                    if (!claimStorageData.flags.spawnAny) {
                        return false;
                    } else if (!claimStorageData.flags.spawnAmbient && entity instanceof Ambient) {
                        return false;
                    } else if (!claimStorageData.flags.spawnAquatic && entity instanceof Aquatic) {
                        return false;
                    }
                }
                return true;
            }
        });

        for (Entity entity : event.getEntities()) {
            final Location<World> location = entity.getLocation();
            // these rules apply only to creative worlds
            if (!GriefPrevention.instance.claimModeIsActive(location.getExtent().getProperties(), ClaimsMode.Creative)) {
                return;
            }

            final Cause cause = event.getCause();
            final Player player = cause.first(Player.class).orElse(null);
            final ItemStack stack = cause.first(ItemStack.class).orElse(null);
            final EntityType entityType = entity.getType();
            if (player != null) {
                if (stack != null && !stack.getItem().equals(ItemTypes.SPAWN_EGG)) {
                    event.setCancelled(true);
                    return;
                }
                if (!entityType.equals(EntityTypes.IRON_GOLEM) && !entityType.equals(EntityTypes.SNOWMAN) && !entityType.equals(EntityTypes
                        .ARMOR_STAND)) {
                    event.setCancelled(true);
                    return;
                }
            }

            // otherwise, just apply the limit on total entities per claim (and no spawning in the wilderness!)
            Claim claim = this.dataStore.getClaimAt(location, false, null);
            if (claim == null || claim.allowMoreEntities() != null) {
                event.setCancelled(true);
            }
        }
    }

    // when an entity is damaged
    @IsCancelled(Tristate.UNDEFINED)
    @Listener
    public void onEntityDamage(DamageEntityEvent event) {
        // monsters are never protected
        if (event.getTargetEntity() instanceof Monster) {
            return;
        }

        Optional<DamageSource> damageSourceOpt = event.getCause().first(DamageSource.class);
        if (!damageSourceOpt.isPresent()) {
            return;
        }

        DamageSource damageSource = damageSourceOpt.get();
        Claim claim = this.dataStore.getClaimAt(event.getTargetEntity().getLocation(), false, null);
        if (claim != null) {
            // check mob-player-damage flag
            if (damageSource instanceof EntityDamageSource && event.getTargetEntity() instanceof Player) {
                EntityDamageSource entityDamageSource = (EntityDamageSource) damageSource;
                if (entityDamageSource.getSource() instanceof Monster) {
                    if (!claim.getClaimData().getConfig().flags.mobPlayerDamage) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        // protect pets from environmental damage types which could be easily caused by griefers
        if (event.getTargetEntity() instanceof EntityTameable && !GriefPrevention.instance.pvpRulesApply(event.getTargetEntity().getWorld())) {
            EntityTameable tameable = (EntityTameable) event.getTargetEntity();
            if (tameable.isTamed()) {
                if (damageSource.getType() == DamageTypes.EXPLOSIVE ||
                        damageSource.getType() == DamageTypes.CONTACT ||
                        damageSource.getType() == DamageTypes.FIRE ||
                        damageSource.getType() == DamageTypes.SUFFOCATE) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // the rest is only interested in entities damaging entities (ignoring environmental damage)
        if (!(damageSource instanceof EntityDamageSource)) {
            return;
        }

        EntityDamageSource entityDamageSource = (EntityDamageSource) damageSource;
        // determine which player is attacking, if any
        Player attacker = null;
        Projectile arrow = null;
        Entity sourceEntity = entityDamageSource.getSource();

        if (sourceEntity != null) {
            if (sourceEntity instanceof Player) {
                attacker = (Player) sourceEntity;
            } else if (sourceEntity instanceof Projectile) {
                arrow = (Projectile) sourceEntity;
                if (arrow.getShooter() instanceof Player) {
                    attacker = (Player) arrow.getShooter();
                }
            }
        }

        GriefPreventionConfig<?> activeConfig = GriefPrevention.getActiveConfig(event.getTargetEntity().getWorld().getProperties());
        // if the attacker is a player and defender is a player (pvp combat)
        if (attacker != null && event.getTargetEntity() instanceof Player && GriefPrevention.instance.pvpRulesApply(attacker.getWorld())) {
            // FEATURE: prevent pvp in the first minute after spawn, and prevent pvp when one or both players have no inventory
            Player defender = (Player) (event.getTargetEntity());

            if (attacker != defender) {
                PlayerData defenderData = this.dataStore.getPlayerData(defender.getWorld().getProperties(), defender.getUniqueId());
                PlayerData attackerData = this.dataStore.getPlayerData(attacker.getWorld().getProperties(), attacker.getUniqueId());

                // otherwise if protecting spawning players
                if (activeConfig.getConfig().pvp.protectFreshSpawns) {
                    if (defenderData.pvpImmune) {
                        event.setCancelled(true);
                        GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.ThatPlayerPvPImmune);
                        return;
                    }

                    if (attackerData.pvpImmune) {
                        event.setCancelled(true);
                        GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.CantFightWhileImmune);
                        return;
                    }
                }

                // FEATURE: prevent players from engaging in PvP combat inside land claims (when it's disabled)
                if (!activeConfig.getConfig().pvp.protectPlayersInClaims || !activeConfig.getConfig().pvp.protectPlayersInAdminClaims) {
                    Claim attackerClaim = this.dataStore.getClaimAt(attacker.getLocation(), false, attackerData.lastClaim);
                    if (!attackerData.ignoreClaims) {
                        // ignore claims mode allows for pvp inside land claims
                        if (attackerClaim != null && !attackerData.inPvpCombat(defender.getWorld()) &&
                                (attackerClaim.isAdminClaim() && attackerClaim.parent == null
                                        && activeConfig.getConfig().pvp.protectPlayersInAdminClaims ||
                                        attackerClaim.isAdminClaim() && attackerClaim.parent != null
                                                && activeConfig.getConfig().pvp.protectPlayersInAdminSubClaims
                                        || !attackerClaim.isAdminClaim() && activeConfig.getConfig().pvp.protectPlayersInClaims)) {
                            attackerData.lastClaim = attackerClaim;
                            PreventPvPEvent pvpEvent = new PreventPvPEvent(attackerClaim);
                            Sponge.getGame().getEventManager().post(pvpEvent);
                            if (!pvpEvent.isCancelled()) {
                                event.setCancelled(true);
                                GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.CantFightWhileImmune);
                                return;
                            }
                        }

                        Claim defenderClaim = this.dataStore.getClaimAt(defender.getLocation(), false, defenderData.lastClaim);
                        if (defenderClaim != null &&
                                !defenderData.inPvpCombat(defender.getWorld()) &&
                                (defenderClaim.isAdminClaim() && defenderClaim.parent == null
                                        && activeConfig.getConfig().pvp.protectPlayersInAdminClaims ||
                                        defenderClaim.isAdminClaim() && defenderClaim.parent != null
                                                && activeConfig.getConfig().pvp.protectPlayersInAdminSubClaims
                                        ||
                                        !defenderClaim.isAdminClaim() && activeConfig.getConfig().pvp.protectPlayersInClaims)) {
                            defenderData.lastClaim = defenderClaim;
                            PreventPvPEvent pvpEvent = new PreventPvPEvent(defenderClaim);
                            Sponge.getGame().getEventManager().post(pvpEvent);
                            if (!pvpEvent.isCancelled()) {
                                event.setCancelled(true);
                                GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.PlayerInPvPSafeZone);
                                return;
                            }
                        }
                    }
                }
            }
        }

        // FEATURE: protect claimed animals, boats, minecarts, and items inside item frames
        // NOTE: animals can be lead with wheat, vehicles can be pushed around.
        // so unless precautions are taken by the owner, a resourceful thief
        // might find ways to steal anyway if theft protection is enabled

        // don't track in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getTargetEntity().getWorld().getProperties())) {
            return;
        }

        // if the damaged entity is a claimed item frame or armor stand, the
        // damager needs to be a player with container trust in the claim
        if (event.getTargetEntity().getType() == EntityTypes.ITEM_FRAME
                || event.getTargetEntity().getType() == EntityTypes.ARMOR_STAND
                || event.getTargetEntity().getType() == EntityTypes.VILLAGER) {
            // TODO - add support with claim flags
            if (event.getTargetEntity().getType() == EntityTypes.VILLAGER) {
                return;
            }

            // decide whether it's claimed
            Claim cachedClaim = null;
            PlayerData playerData = null;
            if (attacker != null) {
                playerData = this.dataStore.getPlayerData(attacker.getWorld().getProperties(), attacker.getUniqueId());
                cachedClaim = playerData.lastClaim;
            }

            claim = this.dataStore.getClaimAt(event.getTargetEntity().getLocation(), false, cachedClaim);

            // if it's claimed
            if (claim != null) {
                // if attacker isn't a player, cancel
                if (attacker == null) {
                    // exception case
                    if (event.getTargetEntity() instanceof Villager && sourceEntity != null && sourceEntity instanceof Monster) {
                        return;
                    }

                    event.setCancelled(true);
                    return;
                }

                // otherwise player must have container trust in the claim
                String failureReason = claim.allowBuild(attacker, event.getTargetEntity().getLocation());
                if (failureReason != null) {
                    event.setCancelled(true);
                    GriefPrevention.sendMessage(attacker, TextMode.Err, failureReason);
                    return;
                }
            }
        }

        // if the entity is an non-monster creature (remember monsters disqualified above), or a vehicle
        if (((event.getTargetEntity() instanceof Creature || event.getTargetEntity() instanceof Aquatic))) {
            // if entity is tameable and has an owner, apply special rules
            if (event.getTargetEntity() instanceof EntityTameable) {
                EntityTameable tameable = (EntityTameable) event.getTargetEntity();
                if (tameable.isTamed() && tameable.getOwner() != null) {
                    // limit attacks by players to owners and admins in
                    // ignore claims mode
                    if (attacker != null) {
                        UUID ownerID = tameable.getOwner().getUniqueID();

                        // if the player interacting is the owner, always
                        // allow
                        if (attacker.getUniqueId().equals(ownerID)) {
                            return;
                        }

                        // allow for admin override
                        PlayerData attackerData = this.dataStore.getPlayerData(attacker.getWorld().getProperties(), attacker.getUniqueId());
                        if (attackerData.ignoreClaims) {
                            return;
                        }

                        // otherwise disallow in non-pvp worlds
                        if (!GriefPrevention.instance.pvpRulesApply(event.getTargetEntity().getLocation().getExtent())) {
                            Optional<User> owner = Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(ownerID);
                            String ownerName = "someone";
                            if (owner.isPresent()) {
                                ownerName = owner.get().getName();
                            }
                            String message = GriefPrevention.instance.dataStore.getMessage(Messages.NoDamageClaimedEntity, ownerName);
                            if (attacker.hasPermission(GPPermissions.IGNORE_CLAIMS)) {
                                message += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
                            }
                            GriefPrevention.sendMessage(attacker, TextMode.Err, message);
                            event.setCancelled(true);
                            return;
                        }
                        // and disallow if attacker is pvp immune
                        else if (attackerData.pvpImmune) {
                            event.setCancelled(true);
                            GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.CantFightWhileImmune);
                            return;
                        }
                    }
                }
            }

            Claim cachedClaim = null;
            PlayerData playerData = null;

            // if not a player or an explosive, allow
            if (attacker == null && sourceEntity != null && !(sourceEntity instanceof Projectile) && sourceEntity.getType() != EntityTypes.CREEPER
                    && !(sourceEntity instanceof Explosive)) {
                return;
            }

            if (attacker != null) {
                playerData = this.dataStore.getPlayerData(attacker.getWorld().getProperties(), attacker.getUniqueId());
                cachedClaim = playerData.lastClaim;
            }

            claim = this.dataStore.getClaimAt(event.getTargetEntity().getLocation(), false, cachedClaim);

            // if it's claimed
            if (claim != null) {
                // if damaged by anything other than a player (exception
                // villagers injured by zombies in admin claims), cancel the event
                // why exception? so admins can set up a village which can't
                // be CHANGED by players, but must be "protected" by players.
                if (attacker == null) {
                    // exception case
                    if (event.getTargetEntity() instanceof Villager && sourceEntity != null && sourceEntity instanceof Monster) {
                        return;
                    }

                    // all other cases
                    else {
                        event.setCancelled(true);
                        if (sourceEntity != null && sourceEntity instanceof Projectile) {
                            sourceEntity.remove();
                        }
                    }
                }

                // otherwise the player damaging the entity must have permission, unless it's a dog in a pvp world
                else if (!event.getTargetEntity().getWorld().getProperties().isPVPEnabled() && !(event.getTargetEntity().getType()
                        == EntityTypes.WOLF)) {
                    String noContainersReason = claim.allowContainers(attacker, event.getTargetEntity().getLocation());
                    if (noContainersReason != null) {

                        // kill the arrow to avoid infinite bounce between crowded together animals
                        if (arrow != null) {
                            arrow.remove();
                        }

                        String message = GriefPrevention.instance.dataStore.getMessage(Messages.NoDamageClaimedEntity, claim.getOwnerName());
                        if (attacker.hasPermission(GPPermissions.IGNORE_CLAIMS)) {
                            message += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
                        }
                        GriefPrevention.sendMessage(attacker, TextMode.Err, message);
                        event.setCancelled(true);
                    }

                    // cache claim for later
                    if (playerData != null) {
                        playerData.lastClaim = claim;
                    }
                }
            }
        }
    }

    @Listener(order = Order.POST)
    public void onEntityDamageMonitor(DamageEntityEvent event) {
        //FEATURE: prevent players who very recently participated in pvp combat from hiding inventory to protect it from looting
        //FEATURE: prevent players who are in pvp combat from logging out to avoid being defeated

        if (event.getTargetEntity().getType() != EntityTypes.PLAYER) {
            return;
        }

        Player defender = (Player) event.getTargetEntity();

        //only interested in entities damaging entities (ignoring environmental damage)
        // the rest is only interested in entities damaging entities (ignoring environmental damage)
        if (!(event.getCause().root() instanceof EntityDamageSource)) {
            return;
        }

        EntityDamageSource entityDamageSource = (EntityDamageSource) event.getCause().root();

        //if not in a pvp rules world, do nothing
        if (!GriefPrevention.instance.pvpRulesApply(defender.getWorld())) {
            return;
        }

        //determine which player is attacking, if any
        Player attacker = null;
        Projectile arrow = null;
        Entity damageSource = entityDamageSource.getSource();

        if (damageSource != null) {
            if (damageSource instanceof Player) {
                attacker = (Player) damageSource;
            } else if (damageSource instanceof Projectile) {
                arrow = (Projectile) damageSource;
                if (arrow.getShooter() instanceof Player) {
                    attacker = (Player) arrow.getShooter();
                }
            }
        }

        //if attacker not a player, do nothing
        if (attacker == null) {
            return;
        }

        PlayerData defenderData = this.dataStore.getPlayerData(defender.getWorld().getProperties(), defender.getUniqueId());
        PlayerData attackerData = this.dataStore.getPlayerData(attacker.getWorld().getProperties(), attacker.getUniqueId());
        Claim attackerClaim = this.dataStore.getClaimAt(attacker.getLocation(), false, null);
        Claim defenderClaim = this.dataStore.getClaimAt(defender.getLocation(), false, null);

        if (attacker != defender) {
            long now = Calendar.getInstance().getTimeInMillis();
            if (defenderClaim != null) {
                if (GriefPrevention.getActiveConfig(defender.getWorld().getProperties()).getConfig().pvp.protectPlayersInClaims) {
                    return;
                }
            } else if (attackerClaim != null) {
                if (GriefPrevention.getActiveConfig(attacker.getWorld().getProperties()).getConfig().pvp.protectPlayersInClaims) {
                    return;
                }
            }

            defenderData.lastPvpTimestamp = now;
            defenderData.lastPvpPlayer = attacker.getName();
            attackerData.lastPvpTimestamp = now;
            attackerData.lastPvpPlayer = defender.getName();
        }
    }

    // when an entity drops items on death
    @Listener(order = Order.LAST)
    public void onEntityDropItemDeath(DropItemEvent.Destruct event) {
        if (!(event.getCause().root() instanceof Living)) {
            return;
        }

        Living livingEntity = (Living) event.getCause().root();
        // special rule for creative worlds: killed entities don't drop items or experience orbs
        if (GriefPrevention.instance.claimModeIsActive(livingEntity.getLocation().getExtent().getProperties(), ClaimsMode.Creative)) {
            event.setCancelled(true);
            return;
        }

        if (livingEntity instanceof Player) {
            Player player = (Player) livingEntity;
            PlayerData playerData = this.dataStore.getPlayerData(player.getWorld().getProperties(), player.getUniqueId());

            // if involved in a siege
            if (playerData.siegeData != null) {
                // end it, with the dieing player being the loser
                this.dataStore.endSiege(playerData.siegeData,
                        event.getCause().first(Player.class).isPresent() ? event.getCause().first(Player.class).get().getName() : null,
                        player.getName(), true);
                // don't drop items as usual, they will be sent to the siege winner
                event.setCancelled(true);
            }
        }
    }

    // when an entity dies...
    @Listener(order = Order.LAST)
    public void onEntityDeath(DestructEntityEvent.Death event) {
        Living entity = event.getTargetEntity();

        if (!(entity instanceof Player) || !event.getCause().first(EntityDamageSource.class).isPresent()) {
            return;
        }
        // don't do the rest in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(entity.getWorld().getProperties())) {
            return;
        }

        Player player = (Player) entity;
        PlayerData playerData = this.dataStore.getPlayerData(player.getWorld().getProperties(), player.getUniqueId());
        EntityDamageSource damageSource = event.getCause().first(EntityDamageSource.class).get();

        // if involved in a siege
        if (playerData.siegeData != null) {
            // end it, with the dying player being the loser
            this.dataStore.endSiege(playerData.siegeData,
                    damageSource.getSource() != null ? ((net.minecraft.entity.Entity) damageSource.getSource()).getName() : null, player.getName(),
                    true);
        }
    }

    // when an item spawns...
    @Listener(order = Order.LAST)
    public void onItemSpawn(SpawnEntityEvent event) {
        if (event.getEntities().size() > 0) {
            if (event.getEntities().get(0) instanceof Item) {
                // if in a creative world, cancel the event (don't drop items on the ground)
                if (GriefPrevention.instance
                        .claimModeIsActive(event.getEntities().get(0).getLocation().getExtent().getProperties(), ClaimsMode.Creative)) {
                    event.setCancelled(true);
                    return;
                }
            } else {
                return;
            }
        }
    }

    public static final HashSet<PotionEffectType> positiveEffects = new HashSet<PotionEffectType>(Arrays.asList(
            PotionEffectTypes.ABSORPTION,
            //PotionEffectTypes.DAMAGE_RESISTANCE,
            // PotionEffectTypes.FAST_DIGGING,
            PotionEffectTypes.FIRE_RESISTANCE,
            //PotionEffectTypes.HEAL,
            PotionEffectTypes.HEALTH_BOOST,
            //PotionEffectTypes.INCREASE_DAMAGE,
            PotionEffectTypes.INVISIBILITY,
            //PotionEffectTypes.JUMP,
            PotionEffectTypes.NIGHT_VISION,
            PotionEffectTypes.REGENERATION,
            PotionEffectTypes.SATURATION,
            PotionEffectTypes.SPEED,
            PotionEffectTypes.WATER_BREATHING));
}
