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
package me.ryanhamshire.griefprevention.event;

import com.google.common.collect.ImmutableSet;
import me.ryanhamshire.griefprevention.CustomLogEntryTypes;
import me.ryanhamshire.griefprevention.DataStore;
import me.ryanhamshire.griefprevention.GPPermissions;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.claim.ClaimsMode;
import me.ryanhamshire.griefprevention.configuration.ClaimStorageData;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import net.minecraft.entity.EnumCreatureType;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.effect.potion.PotionEffectType;
import org.spongepowered.api.effect.potion.PotionEffectTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.explosive.Explosive;
import org.spongepowered.api.entity.living.Ambient;
import org.spongepowered.api.entity.living.Aquatic;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.monster.Monster;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.IndirectEntityDamageSource;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.explosion.Explosion;

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

    @Listener
    public void onExplosion(ExplosionEvent.Pre event) {
        Claim claim =  GriefPrevention.instance.dataStore.getClaimAt(event.getTargetWorld().getLocation(event.getExplosion().getOrigin()), false, null);

        if (claim == null) {
            return;
        }

        Optional<Explosive> explosive = ((Explosion) event.getExplosion()).getSourceExplosive();

        if (GriefPrevention.instance.permPluginInstalled && explosive.isPresent()) {
            Entity entity = (Entity) explosive.get();

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
                        Set<Context> contextSet = ImmutableSet.of(claim.getContext());

                        if (spongeUser.getPermissionValue(contextSet, GPPermissions.SPAWN_ANY) != Tristate.UNDEFINED) {
                            Tristate result = spongeUser.getPermissionValue(contextSet, GPPermissions.SPAWN_ANY);
                            if (result != Tristate.TRUE) {
                                GriefPrevention.addLogEntry("[Event: SpawnEntityEvent][RootCause: " + event.getCause().root() + "][Entity: " + entity + "][FilterReason: Not allowed to spawn entities within claim.]", CustomLogEntryTypes.Debug);
                            }
                            return true;
                        } else if (nmsEntity.isCreatureType(EnumCreatureType.AMBIENT, false)
                                && spongeUser.getPermissionValue(contextSet, GPPermissions
                                .SPAWN_AMBIENTS) != Tristate.UNDEFINED) {
                            Tristate result = spongeUser.getPermissionValue(contextSet, GPPermissions.SPAWN_AMBIENTS);
                            if (result != Tristate.TRUE) {
                                GriefPrevention.addLogEntry("[Event: SpawnEntityEvent][RootCause: " + event.getCause().root() + "][Entity: " + entity + "][FilterReason: Not allowed to spawn ambients within claim.]", CustomLogEntryTypes.Debug);
                            }
                            return true;
                        } else if (nmsEntity.isCreatureType(EnumCreatureType.WATER_CREATURE, false) && spongeUser.getPermissionValue(contextSet,
                                GPPermissions.SPAWN_AQUATICS) != Tristate.UNDEFINED) {
                            Tristate result = spongeUser.getPermissionValue(contextSet, GPPermissions.SPAWN_AQUATICS);
                            if (result != Tristate.TRUE) {
                                GriefPrevention.addLogEntry("[Event: SpawnEntityEvent][RootCause: " + event.getCause().root() + "][Entity: " + entity + "][FilterReason: Not allowed to spawn aquatics within claim.]", CustomLogEntryTypes.Debug);
                            }
                            return true;
                        } else if (nmsEntity.isCreatureType(EnumCreatureType.MONSTER, false)
                                && spongeUser.getPermissionValue(contextSet, GPPermissions
                                .SPAWN_MONSTERS) != Tristate.UNDEFINED) {
                            Tristate result = spongeUser.getPermissionValue(contextSet, GPPermissions.SPAWN_MONSTERS);
                            if (result != Tristate.TRUE) {
                                GriefPrevention.addLogEntry("[Event: SpawnEntityEvent][RootCause: " + event.getCause().root() + "][Entity: " + entity + "][FilterReason: Not allowed to spawn monsters within claim.]", CustomLogEntryTypes.Debug);
                            }
                            return true;
                        } else if (nmsEntity.isCreatureType(EnumCreatureType.CREATURE, false)
                                && spongeUser.getPermissionValue(contextSet, GPPermissions
                                .SPAWN_PASSIVES) != Tristate.UNDEFINED) {
                            Tristate result = spongeUser.getPermissionValue(contextSet, GPPermissions.SPAWN_PASSIVES);
                            if (result != Tristate.TRUE) {
                                GriefPrevention.addLogEntry("[Event: SpawnEntityEvent][RootCause: " + event.getCause().root() + "][Entity: " + entity + "][FilterReason: Not allowed to spawn passives within claim.]", CustomLogEntryTypes.Debug);
                            }
                            return true;
                        }
                    }

                    ClaimStorageData.ClaimDataNode claimStorageData = claim.getClaimData().getConfig();
                    if (!claimStorageData.flags.spawnAny) {
                        GriefPrevention.addLogEntry("[Event: SpawnEntityEvent][RootCause: " + event.getCause().root() + "][Entity: " + entity + "][FilterReason: Not allowed to spawn entities within claim.]", CustomLogEntryTypes.Debug);
                        return false;
                    } else if (!claimStorageData.flags.spawnAmbient && entity instanceof Ambient) {
                        GriefPrevention.addLogEntry("[Event: SpawnEntityEvent][RootCause: " + event.getCause().root() + "][Entity: " + entity + "][FilterReason: Not allowed to spawn ambients within claim.]", CustomLogEntryTypes.Debug);
                        return false;
                    } else if (!claimStorageData.flags.spawnAquatic && entity instanceof Aquatic) {
                        GriefPrevention.addLogEntry("[Event: SpawnEntityEvent][RootCause: " + event.getCause().root() + "][Entity: " + entity + "][FilterReason: Not allowed to spawn aquatics within claim.]", CustomLogEntryTypes.Debug);
                        return false;
                    } else if (!claimStorageData.flags.spawnMonsters && entity instanceof Monster) {
                        GriefPrevention.addLogEntry("[Event: SpawnEntityEvent][RootCause: " + event.getCause().root() + "][Entity: " + entity + "][FilterReason: Not allowed to spawn monsters within claim.]", CustomLogEntryTypes.Debug);
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

            if (player != null) {
                if (stack != null && !stack.getItem().equals(ItemTypes.SPAWN_EGG)) {
                    GriefPrevention.addLogEntry("[Event: SpawnEntityEvent][RootCause: " + event.getCause().root() + "][Entity: " + entity + "][FilterReason: Cannot spawn entities in creative worlds.]", CustomLogEntryTypes.Debug);
                    event.setCancelled(true);
                    return;
                }
            }

            // otherwise, just apply the limit on total entities per claim (and no spawning in the wilderness!)
            Claim claim = this.dataStore.getClaimAt(location, false, null);
            if (claim == null) {
                continue;
            }

            String denyReason = claim.allowMoreEntities();
            if (denyReason != null) {
                GriefPrevention.addLogEntry("[Event: SpawnEntityEvent][RootCause: " + event.getCause().root() + "][Entity: " + entity + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                event.setCancelled(true);
            }
        }
    }

    @IsCancelled(Tristate.UNDEFINED)
    @Listener
    public void onEntityAttack(AttackEntityEvent event) {
        if (protectEntity(event.getTargetEntity(), event.getCause())) {
            event.setCancelled(true);
        }
    }

    @IsCancelled(Tristate.UNDEFINED)
    @Listener
    public void onEntityDamage(DamageEntityEvent event) {
        if (protectEntity(event.getTargetEntity(), event.getCause())) {
            event.setCancelled(true);
        }
    }

    public boolean protectEntity(Entity entity, Cause cause) {
        // monsters are never protected
        if (entity instanceof Monster || !GriefPrevention.isEntityProtected(entity)) {
            return false;
        }

        Optional<DamageSource> damageSourceOpt = cause.first(DamageSource.class);
        if (!damageSourceOpt.isPresent()) {
            return false;
        }

        DamageSource damageSource = damageSourceOpt.get();
        Claim claim = this.dataStore.getClaimAt(entity.getLocation(), false, null);

        // Protect owned entities anywhere in world
        if (damageSource instanceof EntityDamageSource && !((net.minecraft.entity.Entity) entity).isCreatureType(EnumCreatureType.MONSTER, false)) {
            EntityDamageSource entityDamageSource = (EntityDamageSource) damageSource;
            Entity sourceEntity = entityDamageSource.getSource();
            if (entityDamageSource instanceof IndirectEntityDamageSource) {
                sourceEntity = ((IndirectEntityDamageSource) entityDamageSource).getIndirectSource();
            }

            if (sourceEntity instanceof User) {
                User sourceUser = (User) sourceEntity;
                Optional<UUID> creatorUuid = entity.getCreator();
                if (creatorUuid.isPresent()) {
                    Optional<User> user = Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(creatorUuid.get());
                    if (user.isPresent() && !user.get().getUniqueId().equals(sourceUser.getUniqueId())) {
                        return true;
                    }
                } else if (claim != null && sourceUser.getUniqueId().equals(claim.ownerID)) {
                    return true;
                }
                return false;
            } else if (claim != null) {
                if (entity instanceof Player) {
                    if (entityDamageSource.getSource() instanceof Monster) {
                        if (!claim.getClaimData().getConfig().flags.mobPlayerDamage) {
                            GriefPrevention.addLogEntry("[Event: DamageEntityEvent][RootCause: " + cause.root() + "][Entity: " + entity + "][CancelReason: Monsters not allowed to attack players within claim.]", CustomLogEntryTypes.Debug);
                            return true;
                        }
                    }
                }
            }
        }

        // the rest is only interested in entities damaging entities (ignoring environmental damage)
        if (!(damageSource instanceof EntityDamageSource)) {
            return false;
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

        GriefPreventionConfig<?> activeConfig = GriefPrevention.getActiveConfig(entity.getWorld().getProperties());
        // if the attacker is a player and defender is a player (pvp combat)
        if (attacker != null && entity instanceof Player && GriefPrevention.instance.pvpRulesApply(attacker.getWorld())) {
            // FEATURE: prevent pvp in the first minute after spawn, and prevent pvp when one or both players have no inventory
            Player defender = (Player) (entity);

            if (attacker != defender) {
                PlayerData defenderData = this.dataStore.getPlayerData(defender.getWorld().getProperties(), defender.getUniqueId());
                PlayerData attackerData = this.dataStore.getPlayerData(attacker.getWorld().getProperties(), attacker.getUniqueId());

                // otherwise if protecting spawning players
                if (activeConfig.getConfig().pvp.protectFreshSpawns) {
                    if (defenderData.pvpImmune) {
                        GriefPrevention.addLogEntry("[Event: DamageEntityEvent][RootCause: " + cause.root() + "][Entity: " + entity + "][CancelReason: Defender PVP Immune.]", CustomLogEntryTypes.Debug);
                        GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.ThatPlayerPvPImmune);
                        return true;
                    }

                    if (attackerData.pvpImmune) {
                        GriefPrevention.addLogEntry("[Event: DamageEntityEvent][RootCause: " + cause.root() + "][Entity: " + entity + "][CancelReason: Attacker PVP Immune.]", CustomLogEntryTypes.Debug);
                        GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.CantFightWhileImmune);
                        return true;
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
                                GriefPrevention.addLogEntry("[Event: DamageEntityEvent][RootCause: " + cause.root() + "][Entity: " + entity + "][CancelReason: Cannot fight while PVP Immune.]", CustomLogEntryTypes.Debug);
                                GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.CantFightWhileImmune);
                                return true;
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
                                GriefPrevention.addLogEntry("[Event: DamageEntityEvent][RootCause: " + cause.root() + "][Entity: " + entity + "][CancelReason: Player in PVP Safe Zone.]", CustomLogEntryTypes.Debug);
                                GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.PlayerInPvPSafeZone);
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    @Listener(order = Order.POST)
    public void onEntityDamageMonitor(DamageEntityEvent event) {
        //FEATURE: prevent players who very recently participated in pvp combat from hiding inventory to protect it from looting
        //FEATURE: prevent players who are in pvp combat from logging out to avoid being defeated

        if (event.getTargetEntity().getType() != EntityTypes.PLAYER || !GriefPrevention.isEntityProtected(event.getTargetEntity())) {
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
            GriefPrevention.addLogEntry("[Event: DamageEntityEvent][RootCause: " + event.getCause().root() + "][CancelReason: Drops not allowed in creative worlds.]", CustomLogEntryTypes.Debug);
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
                GriefPrevention.addLogEntry("[Event: DropItemEvent.Destruct][RootCause: " + event.getCause().root() + "][CancelReason: Siege in progress.]", CustomLogEntryTypes.Debug);
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
