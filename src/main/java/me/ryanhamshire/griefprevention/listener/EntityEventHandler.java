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

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.ImmutableMap;
import me.ryanhamshire.griefprevention.DataStore;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GPTimings;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.TrustType;
import me.ryanhamshire.griefprevention.claim.ClaimsMode;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.claim.GPClaimManager;
import me.ryanhamshire.griefprevention.event.GPAttackPlayerEvent;
import me.ryanhamshire.griefprevention.event.GPBorderClaimEvent;
import me.ryanhamshire.griefprevention.permission.GPPermissionHandler;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import me.ryanhamshire.griefprevention.provider.MCClansApiProvider;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import nl.riebie.mcclans.api.ClanPlayer;
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
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.IndirectEntityDamageSource;
import org.spongepowered.api.event.cause.entity.spawn.EntitySpawnCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnCause;
import org.spongepowered.api.event.cause.entity.spawn.SpawnTypes;
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
import org.spongepowered.api.text.chat.ChatType;
import org.spongepowered.api.text.chat.ChatTypes;
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
    private final DataStore dataStore;

    public EntityEventHandler(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntityExplosionPre(ExplosionEvent.Pre event) {
        GPTimings.ENTITY_EXPLOSION_PRE_EVENT.startTimingIfSync();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
            GPTimings.ENTITY_EXPLOSION_PRE_EVENT.stopTimingIfSync();
            return;
        }

        Location<World> location = event.getExplosion().getLocation();
        GPClaim claim =  GriefPreventionPlugin.instance.dataStore.getClaimAt(location, false, null);

        User user = event.getCause().first(User.class).orElse(null);
        Explosive explosive = null;
        if (event.getExplosion() instanceof Explosion) {
            explosive = ((Explosion) event.getExplosion()).getSourceExplosive().orElse(null);
        }

        if (explosive != null) {
            Entity entity = (Entity) explosive;

            if (user == null) {
                UUID uuid = entity.getCreator().orElse(null);
                if (uuid != null) {
                    user = GriefPreventionPlugin.getOrCreateUser(uuid);
                }
            }

            if(GPPermissionHandler.getClaimPermission(event, location, claim, GPPermissions.EXPLOSION, entity, location, user, true) == Tristate.FALSE) {
                event.setCancelled(true);
                GPTimings.ENTITY_EXPLOSION_PRE_EVENT.stopTimingIfSync();
                return;
            }
        }

        GPTimings.ENTITY_EXPLOSION_PRE_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntityExplosionDetonate(ExplosionEvent.Detonate event) {
        GPTimings.ENTITY_EXPLOSION_DETONATE_EVENT.startTimingIfSync();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
            GPTimings.ENTITY_EXPLOSION_DETONATE_EVENT.stopTimingIfSync();
            return;
        }

        User user = event.getCause().first(User.class).orElse(null);
        Iterator<Entity> iterator = event.getEntities().iterator();
        GPClaim targetClaim = null;
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            targetClaim =  GriefPreventionPlugin.instance.dataStore.getClaimAt(entity.getLocation(), false, targetClaim);

            if (GPPermissionHandler.getClaimPermission(event, entity.getLocation(), targetClaim, GPPermissions.ENTITY_DAMAGE, event.getCause().root(), entity, user) == Tristate.FALSE) {
                iterator.remove();
            }
        }
        GPTimings.ENTITY_EXPLOSION_DETONATE_EVENT.stopTimingIfSync();
    }

    // when a creature spawns...
    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntitySpawn(SpawnEntityEvent event, @First SpawnCause spawnCause) {
        if (event instanceof DropItemEvent || event.getEntities().isEmpty()) {
            return;
        }

        GPTimings.ENTITY_SPAWN_EVENT.startTimingIfSync();
        final World world = event.getEntities().get(0).getWorld();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(world.getProperties())) {
            GPTimings.ENTITY_SPAWN_EVENT.stopTimingIfSync();
            return;
        }

        User user = event.getCause().first(User.class).orElse(null);
        event.filterEntities(new Predicate<Entity>() {
            GPClaim targetClaim = null;

            @Override
            public boolean test(Entity entity) {
                targetClaim = GriefPreventionPlugin.instance.dataStore.getClaimAt(entity.getLocation(), false, targetClaim);
                if (targetClaim == null) {
                    return true;
                }

                final Location<World> location = entity.getLocation();
                String permission = GPPermissions.ENTITY_SPAWN;
                if (entity instanceof EntityItem || entity instanceof EntityXPOrb) {
                    if (user == null) {
                        return true;
                    }
                    if (targetClaim.isUserTrusted(user, TrustType.BUILDER)) {
                        return true;
                    }
                    if (GPPermissionHandler.getClaimPermission(event, location, targetClaim, GPPermissions.BLOCK_BREAK, user, location, user) == Tristate.TRUE) {
                        return true;
                    }
                    // If we reached this point, XP orbs shouldn't be allowed to spawn
                    if (entity instanceof EntityXPOrb) {
                        return false;
                    }
                    permission = GPPermissions.ITEM_SPAWN;
                }
                // Always allow pixelmon spawns from pokeballs
                if (spawnCause instanceof EntitySpawnCause) {
                    final EntitySpawnCause entitySpawnCause = (EntitySpawnCause) spawnCause;
                    final String entityId = entitySpawnCause.getEntity().getType().getId();
                    if (entityId.equals("pixelmon:occupiedpokeball") || entityId.equals("pixelmon:pokeball")) {
                        User owner = ((IMixinEntity) entity).getTrackedPlayer(NbtDataUtil.SPONGE_ENTITY_CREATOR).orElse(null);
                        if (owner != null) {
                            return true;
                        }
                    }
                } else if (entity.getType().getId().equals("pixelmon:occupiedpokeball") || entity.getType().getId().equals("pixelmon:pokeball") || 
                        (spawnCause.getType() == SpawnTypes.CUSTOM && entity.getType().getId().equals("pixelmon:pixelmon"))) {
                    User owner = ((IMixinEntity) entity).getTrackedPlayer(NbtDataUtil.SPONGE_ENTITY_CREATOR).orElse(null);
                    if (owner != null) {
                        return true;
                    }
                }
                if (GPPermissionHandler.getClaimPermission(event, entity.getLocation(), targetClaim, permission, spawnCause, entity, user, true) == Tristate.FALSE) {
                    return false;
                }
                return true;
            }
        });

        GPTimings.ENTITY_SPAWN_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
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

    @Listener(order = Order.FIRST, beforeModifications = true)
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
        EntityDamageSource entityDamageSource = null;
        if (damageSource instanceof EntityDamageSource) {
            entityDamageSource = (EntityDamageSource) damageSource;
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

        GPPlayerData playerData = null;
        if (player != null) {
            playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(targetEntity.getWorld(), player.getUniqueId());
            if (playerData.canIgnoreClaim(GriefPreventionPlugin.instance.dataStore.getClaimAtPlayer(playerData, player.getLocation(), false))) {
                return false;
            }
        }
        if (!GriefPreventionPlugin.isEntityProtected(targetEntity)) {
            return false;
        }

        GPClaim claim = this.dataStore.getClaimAt(targetEntity.getLocation(), false, playerData != null ? playerData.lastClaim.get() : null);
        Tristate result = GPPermissionHandler.getClaimPermission(event, targetEntity.getLocation(), claim, GPPermissions.ENTITY_DAMAGE, sourceEntity, targetEntity, user, true);
        if (result == Tristate.FALSE) {
            return true;
        }
        result = GPPermissionHandler.getClaimPermission(event, targetEntity.getLocation(), claim, GPPermissions.ENTITY_DAMAGE, damageSource, targetEntity, user, true);
        if (result == Tristate.FALSE) {
            return true;
        }

        // allow trusted users to attack entities within claim
        if (!(targetEntity instanceof Player) && claim.isUserTrusted(user, TrustType.ACCESSOR)) {
            return false;
        }

        // Protect owned entities anywhere in world
        if (sourceEntity != null && !(SpongeImplHooks.isCreatureOfType((net.minecraft.entity.Entity) targetEntity, EnumCreatureType.MONSTER))) {
            Tristate perm = Tristate.UNDEFINED;
            // Ignore PvP checks for owned entities
            if (!(sourceEntity instanceof Player) && !(targetEntity instanceof Player)) {
                if (sourceEntity instanceof User) {
                    User sourceUser = (User) sourceEntity;
                    if (sourceUser instanceof Player) {
                        playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(targetEntity.getWorld(), sourceUser.getUniqueId());
                        if (playerData.canIgnoreClaim(claim)) {
                            return false;
                        }
                    }
                    perm = GPPermissionHandler.getClaimPermission(event, targetEntity.getLocation(), claim, GPPermissions.ENTITY_DAMAGE, sourceEntity, targetEntity, sourceUser);
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
                            if (GPPermissionHandler.getClaimPermission(event, targetEntity.getLocation(), claim, GPPermissions.ENTITY_DAMAGE, sourceEntity, targetEntity, user) != Tristate.TRUE) {
                                return true;
                            }
                        }
                    } else if (targetEntity instanceof EntityLivingBase && !SpongeImplHooks.isCreatureOfType((net.minecraft.entity.Entity) targetEntity, EnumCreatureType.MONSTER)) {
                        if (user != null && !user.getUniqueId().equals(claim.getOwnerUniqueId()) && perm != Tristate.TRUE) {
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
                    GriefPreventionPlugin.sendMessage(attacker, GriefPreventionPlugin.instance.messageData.pvpDefenseless.toText());
                    return true;
                }

                if (attackerData.pvpImmune) {
                    GriefPreventionPlugin.sendMessage(attacker, GriefPreventionPlugin.instance.messageData.pvpFightImmune.toText());
                    return true;
                }

                // FEATURE: prevent players from engaging in PvP combat inside land claims (when it's disabled)
                GPClaim attackerClaim = this.dataStore.getClaimAt(attacker.getLocation(), false, attackerData.lastClaim.get());
                if (!attackerData.canIgnoreClaim(attackerClaim)) {
                    // ignore claims mode allows for pvp inside land claims
                    if (attackerClaim != null && !attackerData.inPvpCombat(defender.getWorld()) && attackerClaim.protectPlayersInClaim()) {
                        attackerData.lastClaim = new WeakReference<>(attackerClaim);
                        GPAttackPlayerEvent pvpEvent = new GPAttackPlayerEvent(attackerClaim, Cause.of(NamedCause.source(entityDamageSource)), defender);
                        Sponge.getEventManager().post(pvpEvent);
                        if (!pvpEvent.isCancelled()) {
                            pvpEvent.setCancelled(true);
                            GriefPreventionPlugin.sendMessage(attacker, GriefPreventionPlugin.instance.messageData.pvpPlayerSafeZone.toText());
                            return true;
                        }
                    }

                    GPClaim defenderClaim = this.dataStore.getClaimAt(defender.getLocation(), false, defenderData.lastClaim.get());
                    if (defenderClaim != null && !defenderData.inPvpCombat(defender.getWorld()) && defenderClaim.protectPlayersInClaim()) {
                        defenderData.lastClaim = new WeakReference<>(defenderClaim);
                        GPAttackPlayerEvent pvpEvent = new GPAttackPlayerEvent(defenderClaim, Cause.of(NamedCause.source(entityDamageSource)), defender);
                        Sponge.getEventManager().post(pvpEvent);
                        if (!pvpEvent.isCancelled()) {
                            pvpEvent.setCancelled(true);
                            GriefPreventionPlugin.sendMessage(attacker, GriefPreventionPlugin.instance.messageData.pvpPlayerSafeZone.toText());
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
                    GriefPreventionPlugin.sendMessage(attacker, GriefPreventionPlugin.instance.messageData.pvpPlayerSafeZone.toText());
                    return true;
                }
            }
        }

        if (GPPermissionHandler.getClaimPermission(event, targetEntity.getLocation(), claim, GPPermissions.ENTITY_DAMAGE, attacker, targetEntity, user) == Tristate.FALSE) {
            return true;
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
    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntityDropItemDeath(DropItemEvent.Destruct event, @Root Living livingEntity) {
        if (event.getEntities().isEmpty()) {
            return;
        }

        GPTimings.ENTITY_DROP_ITEM_DEATH_EVENT.startTimingIfSync();
        final World world = event.getEntities().get(0).getWorld();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(world.getProperties())) {
            GPTimings.ENTITY_DROP_ITEM_DEATH_EVENT.stopTimingIfSync();
            return;
        }

        // special rule for creative worlds: killed entities don't drop items or experience orbs
        if (GriefPreventionPlugin.instance.claimModeIsActive(livingEntity.getLocation().getExtent().getProperties(), ClaimsMode.Creative)) {
            event.setCancelled(true);
            GPTimings.ENTITY_DROP_ITEM_DEATH_EVENT.stopTimingIfSync();
            return;
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

        GPTimings.ENTITY_DEATH_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntityMove(MoveEntityEvent event){
        GPTimings.ENTITY_MOVE_EVENT.startTimingIfSync();
        Entity entity = event.getTargetEntity();
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

        final Location<World> fromLocation = event.getFromTransform().getLocation();
        final Location<World> toLocation = event.getToTransform().getLocation();

        if (player == null && owner == null) {
            // Handle border event without player
            GPClaim fromClaim = this.dataStore.getClaimAt(fromLocation, false, null);
            GPClaim toClaim = this.dataStore.getClaimAt(toLocation, false, null);
            if (fromClaim != toClaim) {
                GPBorderClaimEvent gpEvent = new GPBorderClaimEvent(entity, fromClaim, toClaim, event.getCause());
                Sponge.getEventManager().post(gpEvent);
                if (gpEvent.isCancelled()) {
                    event.setCancelled(true);
                }
            }
            GPTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
            return;
        }

        GPClaim fromClaim = null;
        GPClaim toClaim = this.dataStore.getClaimAt(toLocation);
        if (playerData != null) {
            fromClaim = this.dataStore.getClaimAtPlayer(playerData, fromLocation);
        } else {
            fromClaim = this.dataStore.getClaimAt(fromLocation);
        }

        if (playerData != null && playerData.lastClaim != null) {
            final GPClaim lastClaim = (GPClaim) playerData.lastClaim.get();
            if (lastClaim != null && lastClaim != fromClaim) {
                if (GPPermissionHandler.getClaimPermission(event, toLocation, toClaim, GPPermissions.ENTER_CLAIM, entity, entity, player) == Tristate.FALSE) {
                    Location<World> claimCorner = lastClaim.lesserBoundaryCorner.setPosition(new Vector3d(toClaim.lesserBoundaryCorner.getX(), player.getLocation().getY(), toClaim.greaterBoundaryCorner.getZ()));
                    Location<World> safeLocation = Sponge.getGame().getTeleportHelper().getSafeLocation(claimCorner, 9, 9).orElse(player.getWorld().getSpawnLocation());
                    event.setToTransform(player.getTransform().setLocation(safeLocation));
                }
            }
        }
        if (fromClaim == toClaim) {
            GPTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
            return;
        }
        // MCClans tag support
        Text enterClanTag = null;
        Text exitClanTag = null;
        MCClansApiProvider clanApiProvider = GriefPreventionPlugin.instance.clanApiProvider;
        if (clanApiProvider != null) {
            if ((fromClaim.isBasicClaim() || (fromClaim.isSubdivision() && !fromClaim.isAdminClaim()))) {
                ClanPlayer clanPlayer = clanApiProvider.getClanService().getClanPlayer(fromClaim.getOwnerUniqueId());
                if (clanPlayer != null && clanPlayer.getClan() != null) {
                    exitClanTag = Text.of(clanPlayer.getClan().getTagColored(), " ");
                }
            }
            if ((toClaim.isBasicClaim() || (toClaim.isSubdivision() && !toClaim.isAdminClaim()))) {
                ClanPlayer clanPlayer = clanApiProvider.getClanService().getClanPlayer(toClaim.getOwnerUniqueId());
                if (clanPlayer != null && clanPlayer.getClan() != null) {
                    enterClanTag = Text.of(clanPlayer.getClan().getTagColored(), " ");
                }
            }
        }

        User user = player != null ? player : owner;
        GPBorderClaimEvent gpEvent = new GPBorderClaimEvent(entity, fromClaim, toClaim, event.getCause());
        if (user != null && toClaim.isUserTrusted(user, TrustType.ACCESSOR)) {
            Sponge.getEventManager().post(gpEvent);
            if (gpEvent.isCancelled()) {
                event.setCancelled(true);
                final Text cancelMessage = gpEvent.getMessage().orElse(null);
                if (player != null && cancelMessage != null) {
                    player.sendMessage(cancelMessage);
                }
            } else {
                if (playerData != null) {
                    playerData.lastClaim = new WeakReference<>(toClaim);
                    Text welcomeMessage = gpEvent.getEnterMessage().orElse(null);
                    if (welcomeMessage != null && !welcomeMessage.equals(Text.of())) {
                        ChatType chatType = gpEvent.getEnterMessageChatType().orElse(ChatTypes.CHAT);
                        player.sendMessage(chatType, Text.of(enterClanTag != null ? enterClanTag : GriefPreventionPlugin.GP_TEXT, welcomeMessage));
                    }

                    Text farewellMessage = gpEvent.getExitMessage().orElse(null);
                    if (farewellMessage != null && !farewellMessage.equals(Text.of())) {
                        ChatType chatType = gpEvent.getExitMessageChatType().orElse(ChatTypes.CHAT);
                        player.sendMessage(chatType, Text.of(exitClanTag != null ? exitClanTag : GriefPreventionPlugin.GP_TEXT, farewellMessage));
                    }

                    if (toClaim.isInTown()) {
                        playerData.inTown = true;
                    } else {
                        playerData.inTown = false;
                    }
                }
            }

            GPTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
            return;
        }

        if (fromClaim != toClaim) {
            boolean enterCancelled = false;
            boolean exitCancelled = false;
            // enter
            if (GPPermissionHandler.getClaimPermission(event, toLocation, toClaim, GPPermissions.ENTER_CLAIM, entity, entity, user) == Tristate.FALSE) {
                event.setCancelled(true);
                enterCancelled = true;
            }

            // exit
            if (GPPermissionHandler.getClaimPermission(event, fromLocation, fromClaim, GPPermissions.EXIT_CLAIM, entity, entity, user) == Tristate.FALSE) {
                event.setCancelled(true);
                exitCancelled = true;
            }

            if (enterCancelled || exitCancelled) {
                gpEvent.setCancelled(true);
            }
            Sponge.getEventManager().post(gpEvent);
            if (gpEvent.isCancelled()) {
                final Text cancelMessage = gpEvent.getMessage().orElse(null);
                if (exitCancelled) {
                    if (player != null && cancelMessage != null) {
                        GriefPreventionPlugin.sendClaimDenyMessage(fromClaim, player, GriefPreventionPlugin.instance.messageData.permissionClaimExit.toText());
                    }
                } else if (enterCancelled) {
                    if (player != null && cancelMessage != null) {
                        GriefPreventionPlugin.sendClaimDenyMessage(toClaim, player, GriefPreventionPlugin.instance.messageData.permissionClaimEnter.toText());
                    }
                }

                if (player != null && cancelMessage != null) {
                    player.sendMessage(cancelMessage);
                }

                event.setCancelled(true);
                GPTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
                return;
            }

            if (playerData != null) {
                playerData.lastClaim = new WeakReference<>(toClaim);
                Text welcomeMessage = gpEvent.getEnterMessage().orElse(null);
                if (welcomeMessage != null && !welcomeMessage.equals(Text.of())) {
                    ChatType chatType = gpEvent.getEnterMessageChatType().orElse(ChatTypes.CHAT);
                    player.sendMessage(chatType, Text.of(enterClanTag != null ? enterClanTag : GriefPreventionPlugin.GP_TEXT, welcomeMessage));
                }

                Text farewellMessage = gpEvent.getExitMessage().orElse(null);
                if (farewellMessage != null && !farewellMessage.equals(Text.of())) {
                    ChatType chatType = gpEvent.getExitMessageChatType().orElse(ChatTypes.CHAT);
                    player.sendMessage(chatType, Text.of(exitClanTag != null ? exitClanTag : GriefPreventionPlugin.GP_TEXT, farewellMessage));
                }

                if (toClaim.isInTown()) {
                    playerData.inTown = true;
                } else {
                    playerData.inTown = false;
                }
            }
        }

        GPTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
    }

    // when a player teleports
    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntityTeleport(MoveEntityEvent.Teleport event, @First TeleportCause teleportCause) {
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

        TeleportType type = teleportCause.getTeleportType();
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
            Tristate override = GPPermissionHandler.getFlagOverride(event, sourceLocation, sourceClaim, GPPermissions.ENTITY_TELEPORT_FROM, type, entity);
            if (override != Tristate.UNDEFINED) {
                if (override == Tristate.TRUE) {
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                }

                event.setCancelled(true);
                /*final Text message = GriefPreventionPlugin.instance.messageData.permissionProtectedPortal
                        .apply(ImmutableMap.of(
                        "owner", sourceClaim.getOwnerName())).build();*/
                GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                return;
            }

            if (GPPermissionHandler.getClaimPermission(event, sourceLocation, sourceClaim, GPPermissions.ENTITY_TELEPORT_FROM, type, entity, user) == Tristate.FALSE) {
                final Text message = GriefPreventionPlugin.instance.messageData.permissionPortalExit
                        .apply(ImmutableMap.of(
                        "owner", sourceClaim.getOwnerName())).build();
                if (player != null) {
                    GriefPreventionPlugin.sendMessage(player, message);
                }

                event.setCancelled(true);
                GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                return;
            } else if (type.equals(TeleportTypes.PORTAL)) {
                Tristate result = GPPermissionHandler.getClaimPermission(event, sourceLocation, sourceClaim, GPPermissions.PORTAL_USE, type, entity, user);
                if (result == Tristate.TRUE) {
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                } else if (result == Tristate.FALSE) {
                    final Text message = GriefPreventionPlugin.instance.messageData.permissionPortalEnter
                            .apply(ImmutableMap.of(
                            "owner", sourceClaim.getOwnerName())).build();
                    if (player != null) {
                        GriefPreventionPlugin.sendMessage(player, message);
                    }

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
            Tristate override = GPPermissionHandler.getFlagOverride(event, event.getToTransform().getLocation(), toClaim, GPPermissions.ENTITY_TELEPORT_TO, type, entity);
            if (override != Tristate.UNDEFINED) {
                if (override == Tristate.TRUE) {
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                }

                event.setCancelled(true);
                final Text message = GriefPreventionPlugin.instance.messageData.permissionPortalEnter
                        .apply(ImmutableMap.of(
                        "owner", toClaim.getOwnerName())).build();
                GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                return;
            }

            // FEATURE: prevent players from using entities to gain access to secured claims
            Tristate result = GPPermissionHandler.getClaimPermission(event, destination, toClaim, GPPermissions.ENTITY_TELEPORT_TO, type, entity, user);
            if (result == Tristate.TRUE) {
                GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                return;
            }

            if (result == Tristate.FALSE) {
                final Text message = GriefPreventionPlugin.instance.messageData.permissionPortalEnter
                        .apply(ImmutableMap.of(
                        "owner", toClaim.getOwnerName())).build();
                if (player != null) {
                    GriefPreventionPlugin.sendMessage(player, message);
                }

                event.setCancelled(true);
                GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                return;
            }

            if (!toClaim.isUserTrusted(player, TrustType.ACCESSOR)) {
                if (player != null) {
                    //GriefPreventionPlugin.sendClaimDenyMessage(toClaim, player, denyReason);
                }

                GriefPreventionPlugin.addEventLogEntry(event, toClaim, destination, user, null);
                event.setCancelled(true);
                if (type.equals(EntityTypes.ENDER_PEARL)) {
                    ((EntityPlayer) player).inventory.addItemStackToInventory(new net.minecraft.item.ItemStack(Items.ENDER_PEARL));
                }
                GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                return;
            } else if (type.equals(TeleportTypes.PORTAL)) {
                result = GPPermissionHandler.getClaimPermission(event, destination, toClaim, GPPermissions.PORTAL_USE, type, entity, user);
                if (result == Tristate.TRUE) {
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                } else if (result == Tristate.FALSE) {
                    final Text message = GriefPreventionPlugin.instance.messageData.permissionPortalEnter
                            .apply(ImmutableMap.of(
                            "owner", toClaim.getOwnerName())).build();
                    if (player != null) {
                        GriefPreventionPlugin.sendMessage(player, message);
                    }

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

        if (toClaim.isTown()) {
            playerData.inTown = true;
        } else {
            playerData.inTown = false;
        }
        // TODO
        /*if (event.getCause().first(PortalTeleportCause.class).isPresent()) {
            // FEATURE: when players get trapped in a nether portal, send them back through to the other side
            CheckForPortalTrapTask task = new CheckForPortalTrapTask(player, event.getFromTransform().getLocation());
            Sponge.getGame().getScheduler().createTaskBuilder().delayTicks(200).execute(task).submit(GriefPrevention.instance);
        }*/
        GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntityCollideEntity(CollideEntityEvent event, @First User user) {
        if (event.getEntities().isEmpty()) {
            return;
        }

        GPTimings.ENTITY_COLLIDE_EVENT.startTimingIfSync();
        final World world = event.getEntities().get(0).getWorld();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(world.getProperties())) {
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

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onProjectileImpactEntity(CollideEntityEvent.Impact event, @First User user) {
        GPTimings.PROJECTILE_IMPACT_ENTITY_EVENT.startTimingIfSync();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getImpactPoint().getExtent().getProperties())) {
            GPTimings.PROJECTILE_IMPACT_ENTITY_EVENT.stopTimingIfSync();
            return;
        }

        Object source = event.getCause().root();
        Location<World> impactPoint = event.getImpactPoint();
        GPClaim targetClaim = null;
        for (Entity entity : event.getEntities()) {
            targetClaim = this.dataStore.getClaimAt(impactPoint, false, targetClaim);
            final Tristate result = GPPermissionHandler.getClaimPermission(event, impactPoint, targetClaim, GPPermissions.PROJECTILE_IMPACT_ENTITY, source, entity, user, TrustType.ACCESSOR, true);
            if (result == Tristate.FALSE) {
                if (GPPermissionHandler.getClaimPermission(event, impactPoint, targetClaim, GPPermissions.PROJECTILE_IMPACT_ENTITY, source, entity, user) == Tristate.TRUE) {
                    GPTimings.PROJECTILE_IMPACT_ENTITY_EVENT.stopTimingIfSync();
                    return;
                }

                event.setCancelled(true);
            }
        }
        GPTimings.PROJECTILE_IMPACT_ENTITY_EVENT.stopTimingIfSync();
    }
}
