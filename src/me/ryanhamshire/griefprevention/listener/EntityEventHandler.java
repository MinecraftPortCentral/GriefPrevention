/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) Ryan Hamshire
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
package me.ryanhamshire.griefprevention.listener;

import me.ryanhamshire.griefprevention.DataStore;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GPTimings;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.claim.ClaimsMode;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.claim.GPClaimManager;
import me.ryanhamshire.griefprevention.event.GPAttackPlayerEvent;
import me.ryanhamshire.griefprevention.message.Messages;
import me.ryanhamshire.griefprevention.message.TextMode;
import me.ryanhamshire.griefprevention.permission.GPPermissionHandler;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.explosive.Explosive;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.cause.entity.damage.DamageTypes;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.IndirectEntityDamageSource;
import org.spongepowered.api.event.cause.entity.spawn.SpawnCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
import org.spongepowered.api.event.cause.entity.teleport.EntityTeleportCause;
import org.spongepowered.api.event.cause.entity.teleport.TeleportCause;
import org.spongepowered.api.event.cause.entity.teleport.TeleportType;
import org.spongepowered.api.event.cause.entity.teleport.TeleportTypes;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.api.event.entity.CollideEntityEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.explosion.Explosion;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.common.SpongeImplHooks;
import org.spongepowered.common.data.util.NbtDataUtil;
import org.spongepowered.common.interfaces.entity.IMixinEntity;

import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

//handles events related to entities
public class EntityEventHandler {

    // convenience reference for the singleton datastore
    private DataStore dataStore;

    public EntityEventHandler(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Listener(order = Order.FIRST)
    public void onEntityExplosionPre(ExplosionEvent.Pre event) {
        GPTimings.ENTITY_EXPLOSION_PRE_EVENT.startTimingIfSync();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
            GPTimings.ENTITY_EXPLOSION_PRE_EVENT.stopTimingIfSync();
            return;
        }

        Location<World> location = event.getExplosion().getLocation();
        GPClaim claim =  GriefPreventionPlugin.instance.dataStore.getClaimAt(location, false, null);

        User user = event.getCause().first(User.class).orElse(null);
        //Object source = event.getCause().root();
        Optional<Explosive> explosive = Optional.empty();
        if (event.getExplosion() instanceof Explosion) {
            explosive = ((Explosion) event.getExplosion()).getSourceExplosive();
        }

        if (explosive.isPresent()) {
            Entity entity = (Entity) explosive.get();

            if (user == null) {
                Optional<UUID> uuid = entity.getCreator();
                if (uuid.isPresent()) {
                    user = Sponge.getServiceManager().provide(UserStorageService.class).get().get(uuid.get()).orElse(null);
                }
            }
            // check overrides
            Tristate override = GPPermissionHandler.getFlagOverride(claim, GPPermissions.ENTITY_EXPLOSION, null);
            if (override != Tristate.UNDEFINED) {
                if (override == Tristate.TRUE) {
                    GPTimings.ENTITY_EXPLOSION_PRE_EVENT.stopTimingIfSync();
                    return;
                }

                event.setCancelled(true);
                GriefPreventionPlugin.addEventLogEntry(event, claim, location, user, GPPermissions.ENTITY_EXPLOSION, null);
                GPTimings.ENTITY_EXPLOSION_PRE_EVENT.stopTimingIfSync();
                return;
            }

            if(GPPermissionHandler.getClaimPermission(claim, GPPermissions.ENTITY_EXPLOSION, null, null, user) == Tristate.FALSE) {
                GriefPreventionPlugin.addEventLogEntry(event, claim, location, user, GPPermissions.ENTITY_EXPLOSION, null);
                event.setCancelled(true);
                GPTimings.ENTITY_EXPLOSION_PRE_EVENT.stopTimingIfSync();
                return;
            }
        }

        if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.EXPLOSION, null, null, user) == Tristate.FALSE) {
            GriefPreventionPlugin.addEventLogEntry(event, claim, location, user, GPPermissions.ENTITY_EXPLOSION, null);
            event.setCancelled(true);
        }
        GPTimings.ENTITY_EXPLOSION_PRE_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST)
    public void onEntityExplosionDetonate(ExplosionEvent.Detonate event) {
        GPTimings.ENTITY_EXPLOSION_DETONATE_EVENT.startTimingIfSync();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
            GPTimings.ENTITY_EXPLOSION_DETONATE_EVENT.stopTimingIfSync();
            return;
        }

        User user = event.getCause().first(User.class).orElse(null);
        Iterator<Entity> iterator = event.getEntities().iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            GPClaim claim =  GriefPreventionPlugin.instance.dataStore.getClaimAt(entity.getLocation(), false, null);

            if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.ENTITY_DAMAGE, event.getCause().root(), entity, user) == Tristate.FALSE) {
                iterator.remove();
            }
        }
        GPTimings.ENTITY_EXPLOSION_DETONATE_EVENT.stopTimingIfSync();
    }

    // when a creature spawns...
    @Listener(order = Order.FIRST)
    public void onEntitySpawn(SpawnEntityEvent event, @First SpawnCause spawnCause) {
        if (event instanceof DropItemEvent) {
            return;
        }

        GPTimings.ENTITY_SPAWN_EVENT.startTimingIfSync();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
            GPTimings.ENTITY_SPAWN_EVENT.stopTimingIfSync();
            return;
        }

        User user = event.getCause().first(User.class).orElse(null);
        event.filterEntities(new Predicate<Entity>() {
            @Override
            public boolean test(Entity entity) {
                GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAt(entity.getLocation(), false, null);
                if (claim != null) {
                    String permission = GPPermissions.ENTITY_SPAWN;
                    if (entity instanceof EntityItem || entity instanceof EntityXPOrb) {
                        if (user == null || claim.allowItemDrop(user, entity.getLocation()) == null) {
                            return true;
                        }
                        // If we reached this point, XP orbs shouldn't be allowed to spawn
                        if (entity instanceof EntityXPOrb) {
                            return false;
                        }
                        permission = GPPermissions.ITEM_SPAWN;
                    }
                    // Remove when pixelmon stops sending client packets to spawn on server
                    if (entity.getType().getId().equals("pixelmon:pixelmon") && spawnCause.getType() == SpawnTypes.CUSTOM) {
                        User owner = ((IMixinEntity) entity).getTrackedPlayer(NbtDataUtil.SPONGE_ENTITY_CREATOR).orElse(null);
                        if (owner != null) {
                            return true;
                        }
                    }
                    String entityType = entity.getType() == null ? "unknown" : entity.getType().getId();
                    if (GPPermissionHandler.getClaimPermission(claim, permission, spawnCause, entity, user, true) == Tristate.FALSE) {
                        GriefPreventionPlugin.addEventLogEntry(event, claim, entity.getLocation(), permission, spawnCause, entity, user, "Not allowed to spawn " + entityType + " within claim.");
                        return false;
                    }
                }
                return true;
            }
        });

        GPTimings.ENTITY_SPAWN_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST)
    public void onEntityAttack(AttackEntityEvent event, @First DamageSource damageSource) {
        GPTimings.ENTITY_ATTACK_EVENT.startTimingIfSync();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getTargetEntity().getWorld().getProperties())) {
            GPTimings.ENTITY_ATTACK_EVENT.stopTimingIfSync();
            return;
        }

        if (protectEntity(event, event.getTargetEntity(), event.getCause(), damageSource)) {
            event.setCancelled(true);
        }
        GPTimings.ENTITY_ATTACK_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST)
    public void onEntityDamage(DamageEntityEvent event, @First DamageSource damageSource) {
        GPTimings.ENTITY_DAMAGE_EVENT.startTimingIfSync();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getTargetEntity().getWorld().getProperties())) {
            GPTimings.ENTITY_DAMAGE_EVENT.stopTimingIfSync();
            return;
        }

        if (protectEntity(event, event.getTargetEntity(), event.getCause(), damageSource)) {
            event.setCancelled(true);
        }
        GPTimings.ENTITY_DAMAGE_EVENT.stopTimingIfSync();
    }

    public boolean protectEntity(Event event, Entity targetEntity, Cause cause, DamageSource damageSource) {
        User user = cause.first(User.class).orElse(null);
        Player player = cause.first(Player.class).orElse(null);
        Entity sourceEntity = null;
        if (damageSource instanceof EntityDamageSource) {
            EntityDamageSource entityDamageSource = (EntityDamageSource) damageSource;
            sourceEntity = entityDamageSource.getSource();
            if (entityDamageSource instanceof IndirectEntityDamageSource) {
                sourceEntity = ((IndirectEntityDamageSource) entityDamageSource).getIndirectSource();
            }
            if (sourceEntity instanceof Player) {
                if (user == null) {
                    user = (User) sourceEntity;
                }
                if (player == null) {
                    player = (Player) sourceEntity;
                }
            }
        }

        if (player != null) {
            GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(targetEntity.getWorld(), player.getUniqueId());
            if (playerData.canIgnoreClaim(GriefPreventionPlugin.instance.dataStore.getClaimAtPlayer(playerData, player.getLocation(), false))) {
                return false;
            }
        }
        if (!GriefPreventionPlugin.isEntityProtected(targetEntity)) {
            return false;
        }

        GPClaim claim = this.dataStore.getClaimAt(targetEntity.getLocation(), false, null);
        String denyMessage = claim.allowAccess(player);
        // allow trusted users to attack entities within claim
        if (!(targetEntity instanceof Player) && denyMessage == null) {
            return false;
        }
        // check fall
        if (damageSource.getType() == DamageTypes.FALL) {
            if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.ENTITY_FALL, cause.root(), targetEntity, user) == Tristate.FALSE) {
                return true;
            }
        }
        // Protect owned entities anywhere in world
        if (sourceEntity != null && !(SpongeImplHooks.isCreatureOfType((net.minecraft.entity.Entity) targetEntity, EnumCreatureType.MONSTER))) {
            Tristate perm = Tristate.UNDEFINED;
            // Ignore PvP checks for owned entities
            if (!(sourceEntity instanceof Player) && !(targetEntity instanceof Player)) {
                if (sourceEntity instanceof User) {
                    User sourceUser = (User) sourceEntity;
                    if (sourceUser instanceof Player) {
                        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(targetEntity.getWorld(), sourceUser.getUniqueId());
                        if (playerData.canIgnoreClaim(claim)) {
                            return false;
                        }
                    }
                    perm = GPPermissionHandler.getClaimPermission(claim, GPPermissions.ENTITY_DAMAGE, sourceEntity, targetEntity, sourceUser);
                    if (targetEntity instanceof EntityLivingBase && perm == Tristate.TRUE) {
                        return false;
                    }
                    Optional<UUID> creatorUuid = targetEntity.getCreator();
                    if (creatorUuid.isPresent()) {
                        Optional<User> creator = Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(creatorUuid.get());
                        if (creator.isPresent() && !creator.get().getUniqueId().equals(sourceUser.getUniqueId())) {
                            return true;
                        }
                    } else if (sourceUser.getUniqueId().equals(claim.getOwnerUniqueId())) {
                        return true;
                    }
    
                    return false;
                } else {
                    if (targetEntity instanceof Player) {
                        if (SpongeImplHooks.isCreatureOfType((net.minecraft.entity.Entity) sourceEntity, EnumCreatureType.MONSTER)) {
                            if (user == null) {
                                user = ((IMixinEntity) sourceEntity).getTrackedPlayer(NbtDataUtil.SPONGE_ENTITY_CREATOR).orElse(null);
                            }
                            if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.ENTITY_DAMAGE, sourceEntity, targetEntity, user) != Tristate.TRUE) {
                                GriefPreventionPlugin.addEventLogEntry(event, claim, targetEntity.getLocation(), GPPermissions.ENTITY_DAMAGE, sourceEntity, targetEntity, user, "Monsters not allowed to attack players within claim.");
                                return true;
                            }
                        }
                    } else if (targetEntity instanceof EntityLivingBase && !SpongeImplHooks.isCreatureOfType((net.minecraft.entity.Entity) targetEntity, EnumCreatureType.MONSTER)) {
                        if (user != null && !user.getUniqueId().equals(claim.getOwnerUniqueId()) && perm != Tristate.TRUE) {
                            GriefPreventionPlugin.addEventLogEntry(event, claim, targetEntity.getLocation(), sourceEntity, targetEntity, user, "Untrusted player attempting to attack entity in claim.");
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

        // if the attacker is a player and defender is a player (pvp combat)
        if (attacker != null && targetEntity instanceof Player && claim.pvpRulesApply()) {
            // FEATURE: prevent pvp in the first minute after spawn, and prevent pvp when one or both players have no inventory
            Player defender = (Player) (targetEntity);

            if (attacker != defender) {
                GPPlayerData defenderData = this.dataStore.getOrCreatePlayerData(defender.getWorld().getProperties(), defender.getUniqueId());
                GPPlayerData attackerData = this.dataStore.getOrCreatePlayerData(attacker.getWorld().getProperties(), attacker.getUniqueId());

                // otherwise if protecting spawning players
                if (defenderData.pvpImmune) {
                    GriefPreventionPlugin.addEventLogEntry(event, claim, attacker.getLocation(), attacker, targetEntity, user, "Defender PVP Immune.");
                    GriefPreventionPlugin.sendMessage(attacker, TextMode.Err, Messages.ThatPlayerPvPImmune);
                    return true;
                }

                if (attackerData.pvpImmune) {
                    GriefPreventionPlugin.addEventLogEntry(event, claim, attacker.getLocation(), attacker, targetEntity, user, "Attacker PVP Immune.");
                    GriefPreventionPlugin.sendMessage(attacker, TextMode.Err, Messages.CantFightWhileImmune);
                    return true;
                }

                // FEATURE: prevent players from engaging in PvP combat inside land claims (when it's disabled)
                GPClaim attackerClaim = this.dataStore.getClaimAt(attacker.getLocation(), false, attackerData.lastClaim);
                if (!attackerData.canIgnoreClaim(attackerClaim)) {
                    // ignore claims mode allows for pvp inside land claims
                    if (attackerClaim != null && !attackerData.inPvpCombat(defender.getWorld()) && attackerClaim.protectPlayersInClaim()) {
                        attackerData.lastClaim = new WeakReference<>(attackerClaim);
                        GPAttackPlayerEvent pvpEvent = new GPAttackPlayerEvent(attackerClaim, Cause.of(NamedCause.source(entityDamageSource)), defender);
                        Sponge.getEventManager().post(pvpEvent);
                        if (!pvpEvent.isCancelled()) {
                            GriefPreventionPlugin.addEventLogEntry(pvpEvent, attackerClaim, attacker.getLocation(), attacker, targetEntity, user, this.dataStore.getMessage(Messages.PlayerInPvPSafeZone));
                            pvpEvent.setCancelled(true);
                            GriefPreventionPlugin.sendMessage(attacker, TextMode.Err, Messages.PlayerInPvPSafeZone);
                            return true;
                        }
                    }

                    GPClaim defenderClaim = this.dataStore.getClaimAt(defender.getLocation(), false, defenderData.lastClaim);
                    if (defenderClaim != null && !defenderData.inPvpCombat(defender.getWorld()) && defenderClaim.protectPlayersInClaim()) {
                        defenderData.lastClaim = new WeakReference<>(defenderClaim);
                        GPAttackPlayerEvent pvpEvent = new GPAttackPlayerEvent(defenderClaim, Cause.of(NamedCause.source(entityDamageSource)), defender);
                        Sponge.getEventManager().post(pvpEvent);
                        if (!pvpEvent.isCancelled()) {
                            GriefPreventionPlugin.addEventLogEntry(pvpEvent, attackerClaim, attacker.getLocation(), attacker, targetEntity, user, this.dataStore.getMessage(Messages.PlayerInPvPSafeZone));
                            pvpEvent.setCancelled(true);
                            GriefPreventionPlugin.sendMessage(attacker, TextMode.Err, Messages.PlayerInPvPSafeZone);
                            return true;
                        }
                    }
                }
            }
        } else {
            if (attacker instanceof Player && targetEntity instanceof Player) {
                GPPlayerData defenderData = this.dataStore.getOrCreatePlayerData(attacker.getWorld().getProperties(), targetEntity.getUniqueId());
                // don't protect players already in combat
                if (defenderData.inPvpCombat(claim.world)) {
                    return false;
                }
                if (!claim.isPvpEnabled()) {
                    GriefPreventionPlugin.sendMessage(attacker, TextMode.Err, Messages.PlayerInPvPSafeZone);
                    return true;
                }
            }

            // check perms
            User sourceUser = null;
            if (player != null) {
                sourceUser = player;
            } else if (attacker instanceof User) {
                sourceUser = attacker;
            }

            // only players can interact
            if (attacker instanceof Player) {
                if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.INTERACT_ENTITY_PRIMARY, attacker, targetEntity, sourceUser) == Tristate.FALSE) {
                    return true;
                }
            }

            if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.ENTITY_DAMAGE, attacker, targetEntity, sourceUser) == Tristate.FALSE) {
                return true;
            }
        }

        return false;
    }

    @Listener(order = Order.POST)
    public void onEntityDamageMonitor(DamageEntityEvent event) {
        GPTimings.ENTITY_DAMAGE_MONITOR_EVENT.startTimingIfSync();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getTargetEntity().getWorld().getProperties())) {
            GPTimings.ENTITY_DAMAGE_MONITOR_EVENT.stopTimingIfSync();
            return;
        }

        //FEATURE: prevent players who very recently participated in pvp combat from hiding inventory to protect it from looting
        //FEATURE: prevent players who are in pvp combat from logging out to avoid being defeated

        if (event.getTargetEntity().getType() != EntityTypes.PLAYER || !GriefPreventionPlugin.isEntityProtected(event.getTargetEntity())) {
            GPTimings.ENTITY_DAMAGE_MONITOR_EVENT.stopTimingIfSync();
            return;
        }

        Player defender = (Player) event.getTargetEntity();

        //only interested in entities damaging entities (ignoring environmental damage)
        // the rest is only interested in entities damaging entities (ignoring environmental damage)
        if (!(event.getCause().root() instanceof EntityDamageSource)) {
            GPTimings.ENTITY_DAMAGE_MONITOR_EVENT.stopTimingIfSync();
            return;
        }

        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(defender.getWorld(), defender.getUniqueId());
        GPClaim claim = this.dataStore.getClaimAtPlayer(playerData, defender.getLocation(), false);
        EntityDamageSource entityDamageSource = (EntityDamageSource) event.getCause().root();

        //if not in a pvp rules world, do nothing
        if (!claim.isPvpEnabled()) {
            GPTimings.ENTITY_DAMAGE_MONITOR_EVENT.stopTimingIfSync();
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
            GPTimings.ENTITY_DAMAGE_MONITOR_EVENT.stopTimingIfSync();
            return;
        }

        GPPlayerData defenderData = this.dataStore.getOrCreatePlayerData(defender.getWorld().getProperties(), defender.getUniqueId());
        GPPlayerData attackerData = this.dataStore.getOrCreatePlayerData(attacker.getWorld().getProperties(), attacker.getUniqueId());
        GPClaim attackerClaim = this.dataStore.getClaimAtPlayer(attackerData, attacker.getLocation(), false);
        GPClaim defenderClaim = this.dataStore.getClaimAtPlayer(defenderData, defender.getLocation(), false);

        if (attacker != defender) {
            long now = Calendar.getInstance().getTimeInMillis();
            if (defenderClaim != null) {
                if (GriefPreventionPlugin.getActiveConfig(defender.getWorld().getProperties()).getConfig().pvp.protectPlayersInClaims) {
                    GPTimings.ENTITY_DAMAGE_MONITOR_EVENT.stopTimingIfSync();
                    return;
                }
            } else if (attackerClaim != null) {
                if (GriefPreventionPlugin.getActiveConfig(attacker.getWorld().getProperties()).getConfig().pvp.protectPlayersInClaims) {
                    GPTimings.ENTITY_DAMAGE_MONITOR_EVENT.stopTimingIfSync();
                    return;
                }
            }

            defenderData.lastPvpTimestamp = now;
            defenderData.lastPvpPlayer = attacker.getName();
            attackerData.lastPvpTimestamp = now;
            attackerData.lastPvpPlayer = defender.getName();
        }
        GPTimings.ENTITY_DAMAGE_MONITOR_EVENT.stopTimingIfSync();
    }

    // when an entity drops items on death
    @Listener(order = Order.FIRST)
    public void onEntityDropItemDeath(DropItemEvent.Destruct event, @Root Living livingEntity) {
        GPTimings.ENTITY_DROP_ITEM_DEATH_EVENT.startTimingIfSync();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
            GPTimings.ENTITY_DROP_ITEM_DEATH_EVENT.stopTimingIfSync();
            return;
        }

        // special rule for creative worlds: killed entities don't drop items or experience orbs
        if (GriefPreventionPlugin.instance.claimModeIsActive(livingEntity.getLocation().getExtent().getProperties(), ClaimsMode.Creative)) {
            GriefPreventionPlugin.addEventLogEntry(event, null, null, null, "Drops not allowed in creative worlds.");
            event.setCancelled(true);
            GPTimings.ENTITY_DROP_ITEM_DEATH_EVENT.stopTimingIfSync();
            return;
        }

        if (livingEntity instanceof Player) {
            Player player = (Player) livingEntity;
            GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld().getProperties(), player.getUniqueId());

            // if involved in a siege
            if (playerData.siegeData != null) {
                // end it, with the dieing player being the loser
                this.dataStore.endSiege(playerData.siegeData,
                        event.getCause().first(Player.class).isPresent() ? event.getCause().first(Player.class).get().getName() : null,
                        player.getName(), true);
                // don't drop items as usual, they will be sent to the siege winner
                GriefPreventionPlugin.addEventLogEntry(event, null, player.getLocation(), player, "Siege in progress.");
                event.setCancelled(true);
            }
        }
        GPTimings.ENTITY_DROP_ITEM_DEATH_EVENT.stopTimingIfSync();
    }

    // when an entity dies...
    @Listener(order = Order.LAST)
    public void onEntityDeath(DestructEntityEvent.Death event) {
        GPTimings.ENTITY_DEATH_EVENT.startTimingIfSync();
        Living entity = event.getTargetEntity();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getTargetEntity().getWorld().getProperties())) {
            GPTimings.ENTITY_DEATH_EVENT.stopTimingIfSync();
            return;
        }

        if (!(entity instanceof Player) || !event.getCause().first(EntityDamageSource.class).isPresent()) {
            GPTimings.ENTITY_DEATH_EVENT.stopTimingIfSync();
            return;
        }
        // don't do the rest in worlds where claims are not enabled
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(entity.getWorld().getProperties())) {
            GPTimings.ENTITY_DEATH_EVENT.stopTimingIfSync();
            return;
        }

        Player player = (Player) entity;
        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld().getProperties(), player.getUniqueId());
        EntityDamageSource damageSource = event.getCause().first(EntityDamageSource.class).get();

        // if involved in a siege
        if (playerData.siegeData != null) {
            // end it, with the dying player being the loser
            this.dataStore.endSiege(playerData.siegeData,
                    damageSource.getSource() != null ? ((net.minecraft.entity.Entity) damageSource.getSource()).getName() : null, player.getName(),
                    true);
        }
        GPTimings.ENTITY_DEATH_EVENT.stopTimingIfSync();
    }

    @Listener
    public void onEntityMove(MoveEntityEvent event){
        GPTimings.ENTITY_MOVE_EVENT.startTimingIfSync();
        Entity entity = event.getTargetEntity();
        if (entity instanceof IProjectile) {
            GPTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
            return;
        }
        if (event.getFromTransform().getLocation().getBlockPosition().equals(event.getToTransform().getLocation().getBlockPosition())) {
            GPTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
            return;
        }

        World world = event.getTargetEntity().getWorld();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(world.getProperties())) {
            GPTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
            return;
        }

        Player player = null;
        GPPlayerData playerData = null;
        User owner = null;
        if (entity instanceof Player) {
            player = (Player) entity;
            playerData = this.dataStore.getOrCreatePlayerData(world, player.getUniqueId());
        } else {
            if (((net.minecraft.entity.Entity) entity).getControllingPassenger() instanceof Player) {
                player = (Player) ((net.minecraft.entity.Entity) entity).getControllingPassenger();
                playerData = this.dataStore.getOrCreatePlayerData(world, player.getUniqueId());
            }
            owner = ((IMixinEntity) entity).getTrackedPlayer(NbtDataUtil.SPONGE_ENTITY_CREATOR).orElse(null);
        }

        if (player == null && owner == null) {
            GPTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
            return;
        }

        Location<World> fromLocation = event.getFromTransform().getLocation();
        Location<World> toLocation = event.getToTransform().getLocation();
        GPClaim fromClaim = null;
        if (playerData != null) {
            fromClaim = this.dataStore.getClaimAtPlayer(playerData, fromLocation);
        }
        GPClaim toClaim = this.dataStore.getClaimAt(toLocation, false, null);

        User user = player != null ? player : owner;
        if (user != null && toClaim.allowAccess(user) == null) {
            GPTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
            return;
        }

        // enter
        if (fromClaim != toClaim && toClaim != null) {
            Tristate value = GPPermissionHandler.getClaimPermission(toClaim, GPPermissions.ENTER_CLAIM, entity, entity, user);
            if (value == Tristate.FALSE) {
                if (player != null) {
                    GriefPreventionPlugin.sendClaimDenyMessage(toClaim, player, TextMode.Err, Messages.NoEnterClaim);
                }

                GriefPreventionPlugin.addEventLogEntry(event, toClaim, toLocation, GPPermissions.ENTER_CLAIM, null, entity, user, this.dataStore.getMessage(Messages.NoEnterClaim));
                event.setCancelled(true);
                GPTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
                return;
            }

            if (playerData != null) {
                playerData.lastClaim = new WeakReference<>(toClaim);
                Text welcomeMessage = toClaim.getInternalClaimData().getGreeting().orElse(null);
                if (welcomeMessage != null && !welcomeMessage.equals(Text.of())) {
                    player.sendMessage(Text.of(GriefPreventionPlugin.GP_TEXT, welcomeMessage));
                }
            }
        }

        // exit
        if (fromClaim != toClaim) {
            if (GPPermissionHandler.getClaimPermission(fromClaim, GPPermissions.EXIT_CLAIM, entity, entity, user) == Tristate.FALSE) {
                if (player != null) {
                    GriefPreventionPlugin.sendClaimDenyMessage(fromClaim, player, TextMode.Err, Messages.NoExitClaim);
                }

                GriefPreventionPlugin.addEventLogEntry(event, fromClaim, fromLocation, GPPermissions.EXIT_CLAIM, null, entity, user, this.dataStore.getMessage(Messages.NoExitClaim));
                event.setCancelled(true);
                GPTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
                return;
            }

            if (playerData != null) {
                playerData.lastClaim = new WeakReference<>(toClaim);
                Text farewellMessage = fromClaim.getInternalClaimData().getFarewell().orElse(null);
                if (farewellMessage != null && !farewellMessage.equals(Text.of())) {
                    player.sendMessage(Text.of(GriefPreventionPlugin.GP_TEXT, farewellMessage));
                }
            }
        }
        GPTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
    }

    // when a player teleports
    @Listener(order = Order.FIRST)
    public void onEntityTeleport(MoveEntityEvent.Teleport event) {
        GPTimings.ENTITY_TELEPORT_EVENT.startTimingIfSync();
        Entity entity = event.getTargetEntity();
        Player player = null;
        User user = null;
        if (entity instanceof Player) {
            player = (Player) entity;
            user = player;
        } else {
            user = ((IMixinEntity) entity).getTrackedPlayer(NbtDataUtil.SPONGE_ENTITY_CREATOR).orElse(null);
        }

        if (user == null || !GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getFromTransform().getExtent().getProperties())) {
            GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
            return;
        }

        TeleportType type = event.getCause().first(TeleportCause.class).get().getTeleportType();
        EntityTeleportCause entityTeleportCause = null;
        if (type == TeleportTypes.ENTITY_TELEPORT) {
            entityTeleportCause = (EntityTeleportCause) event.getCause().first(EntityTeleportCause.class).get();
        }

        // FEATURE: prevent teleport abuse to win sieges

        Location<World> sourceLocation = event.getFromTransform().getLocation();
        GPClaim sourceClaim = null;
        GPPlayerData playerData = null;
        if (player != null) {
            playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
            sourceClaim = this.dataStore.getClaimAtPlayer(playerData, player.getLocation(), false);
        } else {
            sourceClaim = this.dataStore.getClaimAt(sourceLocation, false, null);
        }

        if (sourceClaim != null) {
            Tristate override = GPPermissionHandler.getFlagOverride(sourceClaim, GPPermissions.ENTITY_TELEPORT_FROM, entityTeleportCause == null ? null : entityTeleportCause.getTeleportType().getId(), entity);
            if (override != Tristate.UNDEFINED) {
                if (override == Tristate.TRUE) {
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                }

                event.setCancelled(true);
                GriefPreventionPlugin.addEventLogEntry(event, sourceClaim, sourceLocation, GPPermissions.ENTITY_TELEPORT_FROM, entityTeleportCause == null ? null : entityTeleportCause.getTeleportType().getId(), entity, user, this.dataStore.getMessage(Messages.NoTeleportFromProtectedClaim));
                GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                return;
            }

            if (player != null && GriefPreventionPlugin.getActiveConfig(sourceLocation.getExtent().getProperties()).getConfig().siege.siegeEnabled && sourceClaim.siegeData != null) {
                GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.SiegeNoTeleport);
                GriefPreventionPlugin.addEventLogEntry(event, sourceClaim, sourceLocation, user, this.dataStore.getMessage(Messages.SiegeNoTeleport));
                event.setCancelled(true);
                GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                return;
            }
            if (entityTeleportCause != null) {
                Tristate result = GPPermissionHandler.getClaimPermission(sourceClaim, GPPermissions.ENTITY_TELEPORT_FROM, entityTeleportCause.getTeleportType().getId(), entity, user);
                if (result == Tristate.TRUE) {
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                } else if (result == Tristate.FALSE) {
                    if (player != null) {
                        GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.NoTeleportFromProtectedClaim, sourceClaim.getOwnerName());
                    }

                    GriefPreventionPlugin.addEventLogEntry(event, sourceClaim, sourceLocation, GPPermissions.ENTITY_TELEPORT_FROM, entityTeleportCause.getTeleportType().getId(), entity, user, this.dataStore.getMessage(Messages.NoTeleportFromProtectedClaim));
                    event.setCancelled(true);
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                }
            } else if (type.equals(TeleportTypes.PORTAL)) {
                Tristate result = GPPermissionHandler.getClaimPermission(sourceClaim, GPPermissions.PORTAL_USE, type.getId(), entity, user);
                if (result == Tristate.TRUE) {
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                } else if (result == Tristate.FALSE) {
                    if (player != null) {
                        GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.NoPortalToProtectedClaim, sourceClaim.getOwnerName());
                    }

                    GriefPreventionPlugin.addEventLogEntry(event, sourceClaim, sourceLocation, GPPermissions.PORTAL_USE, type.getId(), entity, user, this.dataStore.getMessage(Messages.NoPortalToProtectedClaim));
                    event.setCancelled(true);
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                }/* else if (GPPermissionHandler.getClaimPermission(sourceClaim, GPPermissions.BLOCK_PLACE, entity, null, user) == Tristate.FALSE) {
                    if (player != null) {
                        GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.NoBuildPortalPermission, sourceClaim.getOwnerName());
                    }

                    GriefPreventionPlugin.addEventLogEntry(event, sourceClaim, sourceLocation, GPPermissions.BLOCK_PLACE, entity, null, user, this.dataStore.getMessage(Messages.NoBuildPortalPermission));
                    event.setCancelled(true);
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                }*/
            }
        }

        // check if destination world is enabled
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getToTransform().getExtent().getProperties())) {
            GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
            return;
        }

        Location<World> destination = event.getToTransform().getLocation();
        GPClaim toClaim = this.dataStore.getClaimAt(destination, false, null);
        if (toClaim != null) {
            Tristate override = GPPermissionHandler.getFlagOverride(toClaim, GPPermissions.ENTITY_TELEPORT_TO, entityTeleportCause == null ? null : entityTeleportCause.getTeleportType().getId(), entity);
            if (override != Tristate.UNDEFINED) {
                if (override == Tristate.TRUE) {
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                }

                event.setCancelled(true);
                GriefPreventionPlugin.addEventLogEntry(event, toClaim, destination, GPPermissions.ENTITY_TELEPORT_TO, entityTeleportCause == null ? null : entityTeleportCause.getTeleportType().getId(), entity, user, this.dataStore.getMessage(Messages.NoTeleportToProtectedClaim));
                GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                return;
            }

            if (player != null && GriefPreventionPlugin.getActiveConfig(destination.getExtent().getProperties()).getConfig().siege.siegeEnabled && toClaim.siegeData != null) {
                GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.BesiegedNoTeleport);
                GriefPreventionPlugin.addEventLogEntry(event, toClaim, destination, user, this.dataStore.getMessage(Messages.BesiegedNoTeleport));
                event.setCancelled(true);
                GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                return;
            }

            // FEATURE: prevent players from using entities to gain access to secured claims
            if (entityTeleportCause != null) {
                Tristate result = GPPermissionHandler.getClaimPermission(toClaim, GPPermissions.ENTITY_TELEPORT_TO, entityTeleportCause.getTeleportType().getId(), entity, user);
                if (result == Tristate.TRUE) {
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                } else if (result == Tristate.FALSE) {
                    if (player != null) {
                        GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.NoTeleportToProtectedClaim, toClaim.getOwnerName());
                    }

                    GriefPreventionPlugin.addEventLogEntry(event, toClaim, destination, GPPermissions.ENTITY_TELEPORT_TO, entityTeleportCause.getTeleportType().getId(), entity, user, this.dataStore.getMessage(Messages.NoTeleportToProtectedClaim));
                    event.setCancelled(true);
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                }
                String denyReason = toClaim.allowAccess(user);
                if (denyReason != null) {
                    if (player != null) {
                        GriefPreventionPlugin.sendClaimDenyMessage(toClaim, player, Text.of(TextMode.Err, denyReason));
                    }

                    GriefPreventionPlugin.addEventLogEntry(event, toClaim, destination, user, denyReason);
                    event.setCancelled(true);
                    if (entityTeleportCause != null && entityTeleportCause.getTeleporter().getType().equals(EntityTypes.ENDER_PEARL)) {
                        ((EntityPlayer) player).inventory.addItemStackToInventory(new net.minecraft.item.ItemStack(Items.ENDER_PEARL));
                    }
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                }
            } else if (type.equals(TeleportTypes.PORTAL)) {
                Tristate result = GPPermissionHandler.getClaimPermission(toClaim, GPPermissions.PORTAL_USE, null, entity, user);
                if (result == Tristate.TRUE) {
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                } else if (result == Tristate.FALSE) {
                    if (player != null) {
                        GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.NoPortalToProtectedClaim, toClaim.getOwnerName());
                    }

                    GriefPreventionPlugin.addEventLogEntry(event, toClaim, destination, GPPermissions.PORTAL_USE, null, entity, user, this.dataStore.getMessage(Messages.NoPortalToProtectedClaim));
                    event.setCancelled(true);
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                } /*else if (GPPermissionHandler.getClaimPermission(toClaim, GPPermissions.BLOCK_PLACE, entity, null, user) == Tristate.FALSE) {
                    if (player != null) {
                        GriefPreventionPlugin.sendMessage(player, TextMode.Err, Messages.NoBuildPortalPermission, toClaim.getOwnerName());
                    }

                    GriefPreventionPlugin.addEventLogEntry(event, toClaim, destination, GPPermissions.BLOCK_PLACE, user, this.dataStore.getMessage(Messages.NoBuildPortalPermission));
                    event.setCancelled(true);
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                }*/
            } else if (type.equals(TeleportTypes.COMMAND)) {
                // TODO
            }
        }

        if (player != null && !sourceLocation.getExtent().getUniqueId().equals(destination.getExtent().getUniqueId())) {
            // new world, check if player has world storage for it
            GPClaimManager claimWorldManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(destination.getExtent().getProperties());

            // update lastActive timestamps for claims this player owns
            WorldProperties worldProperties = destination.getExtent().getProperties();
            UUID playerUniqueId = player.getUniqueId();
            for (Claim claim : this.dataStore.getClaimWorldManager(worldProperties).getWorldClaims()) {
                if (claim.getOwnerUniqueId().equals(playerUniqueId)) {
                    // update lastActive timestamp for claim
                    claim.getData().setDateLastActive(Instant.now());
                    claimWorldManager.addClaim(claim, GriefPreventionPlugin.pluginCause);
                } else if (claim.getParent().isPresent() && claim.getParent().get().getOwnerUniqueId().equals(playerUniqueId)) {
                    // update lastActive timestamp for subdivisions if parent owner logs on
                    claim.getData().setDateLastActive(Instant.now());
                    claimWorldManager.addClaim(claim, GriefPreventionPlugin.pluginCause);
                }
            }
        }

        // TODO
        /*if (event.getCause().first(PortalTeleportCause.class).isPresent()) {
            // FEATURE: when players get trapped in a nether portal, send them back through to the other side
            CheckForPortalTrapTask task = new CheckForPortalTrapTask(player, event.getFromTransform().getLocation());
            Sponge.getGame().getScheduler().createTaskBuilder().delayTicks(200).execute(task).submit(GriefPrevention.instance);
        }*/
        GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST)
    public void onEntityCollideEntity(CollideEntityEvent event, @First User user) {
        GPTimings.ENTITY_COLLIDE_EVENT.startTimingIfSync();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
            GPTimings.ENTITY_COLLIDE_EVENT.stopTimingIfSync();
            return;
        }

        Object rootCause = event.getCause().root();
        event.filterEntities(new Predicate<Entity>() {
            @Override
            public boolean test(Entity entity) {
                if (rootCause == entity) {
                    return true;
                }

                // Avoid living entities breaking itemframes
                if (rootCause instanceof EntityItemFrame && entity instanceof EntityLiving) {
                    return false;
                }

                /* Is this still needed ?
                // always allow collisions with players
                if (entity instanceof Player || entity instanceof EntityItem) {
                    return true;
                }

                User owner = null;
                int entityId = ((net.minecraft.entity.Entity) entity).getEntityId();
                GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(entity.getWorld(), user.getUniqueId());
                GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAt(entity.getLocation(), false);
                if (playerData.lastCollideEntityId == entityId && playerData.lastClaim != null && playerData.lastClaim.get() != null) {
                    if (claim.id.equals(playerData.lastClaim.get().getUniqueId())) {
                        return playerData.lastCollideEntityResult;
                    }
                }
                if (claim != null) {
                    // check owner
                    owner = ((IMixinEntity) entity).getTrackedPlayer(NbtDataUtil.SPONGE_ENTITY_CREATOR).orElse(null);
                    if (owner == null) {
                        playerData.setLastCollideEntityData(entityId, true);
                        return true;
                    }

                    // check if user owns entity
                    if (owner.getUniqueId().equals(user.getUniqueId())) {
                        playerData.setLastCollideEntityData(entityId, true);
                        return true;
                    }

                    if (claim.allowAccess(user) != null) {
                        if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.ENTITY_COLLIDE_ENTITY, rootCause, entity, user) == Tristate.FALSE) {
                            playerData.setLastCollideEntityData(entityId, false);
                            return false;
                        }
                    } else if (claim.allowAccess(owner) != null) {
                        if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.ENTITY_COLLIDE_ENTITY, rootCause, entity, owner) == Tristate.FALSE) {
                            playerData.setLastCollideEntityData(entityId, false);
                            return false;
                        }
                    }
                }
                playerData.setLastCollideEntityData(entityId, true); */
                return true;
            }
        });
        GPTimings.ENTITY_COLLIDE_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST)
    public void onProjectileImpactEntity(CollideEntityEvent.Impact event, @First User user) {
        GPTimings.PROJECTILE_IMPACT_ENTITY_EVENT.startTimingIfSync();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getImpactPoint().getExtent().getProperties())) {
            GPTimings.PROJECTILE_IMPACT_ENTITY_EVENT.stopTimingIfSync();
            return;
        }

        Object source = event.getCause().root();
        Location<World> impactPoint = event.getImpactPoint();
        for (Entity entity : event.getEntities()) {
            GPClaim targetClaim = null;
            GPPlayerData playerData = null;
            if (user instanceof Player) {
                playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(impactPoint.getExtent(), user.getUniqueId());
                targetClaim = this.dataStore.getClaimAtPlayer(playerData, impactPoint, false);
            } else {
                targetClaim = this.dataStore.getClaimAt(impactPoint, false, null);
            }

            Tristate override = GPPermissionHandler.getFlagOverride(targetClaim, GPPermissions.PROJECTILE_IMPACT_ENTITY, source, entity);
            if (override != Tristate.UNDEFINED) {
                if (override == Tristate.TRUE) {
                    GPTimings.PROJECTILE_IMPACT_ENTITY_EVENT.stopTimingIfSync();
                    return;
                }

                event.setCancelled(true);
                GriefPreventionPlugin.addEventLogEntry(event, targetClaim, impactPoint, GPPermissions.PROJECTILE_IMPACT_ENTITY, source, entity, user, "");
                GPTimings.PROJECTILE_IMPACT_ENTITY_EVENT.stopTimingIfSync();
                return;
            }

            String denyReason = targetClaim.allowAccess(user, impactPoint);
            if (denyReason != null) {
                if (GPPermissionHandler.getClaimPermission(targetClaim, GPPermissions.PROJECTILE_IMPACT_ENTITY, source, entity, user) == Tristate.TRUE) {
                    GPTimings.PROJECTILE_IMPACT_ENTITY_EVENT.stopTimingIfSync();
                    return;
                }

                GriefPreventionPlugin.addEventLogEntry(event, targetClaim, impactPoint, source, entity, user, denyReason);
                event.setCancelled(true);
            } else {
                if (GPPermissionHandler.getClaimPermission(targetClaim, GPPermissions.PROJECTILE_IMPACT_ENTITY, source, entity, user) == Tristate.FALSE) {
                    GriefPreventionPlugin.addEventLogEntry(event, targetClaim, impactPoint, GPPermissions.PROJECTILE_IMPACT_ENTITY, source, entity, user, denyReason);
                    event.setCancelled(true);
                    GPTimings.PROJECTILE_IMPACT_ENTITY_EVENT.stopTimingIfSync();
                    return;
                }
            }
        }
        GPTimings.PROJECTILE_IMPACT_ENTITY_EVENT.stopTimingIfSync();
    }
}
