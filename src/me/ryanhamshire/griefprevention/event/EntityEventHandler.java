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
package me.ryanhamshire.griefprevention.event;

import me.ryanhamshire.griefprevention.DataStore;
import me.ryanhamshire.griefprevention.GPPermissionHandler;
import me.ryanhamshire.griefprevention.GPPermissions;
import me.ryanhamshire.griefprevention.GPTimings;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.claim.ClaimWorldManager;
import me.ryanhamshire.griefprevention.claim.ClaimsMode;
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
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.IndirectEntityDamageSource;
import org.spongepowered.api.event.cause.entity.spawn.SpawnCause;
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
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
            GPTimings.ENTITY_EXPLOSION_PRE_EVENT.stopTimingIfSync();
            return;
        }

        Location<World> location = event.getExplosion().getLocation();
        Claim claim =  GriefPrevention.instance.dataStore.getClaimAt(location, false, null);

        User user = event.getCause().first(User.class).orElse(null);
        Object source = event.getCause().root();
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
            if(GPPermissionHandler.getClaimPermission(claim, GPPermissions.ENTITY_EXPLOSION, entity, null, user) == Tristate.FALSE) {
                GriefPrevention.addEventLogEntry(event, claim, location, user, null);
                event.setCancelled(true);
                GPTimings.ENTITY_EXPLOSION_PRE_EVENT.stopTimingIfSync();
                return;
            }
        }

        if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.EXPLOSION, source, null, user) == Tristate.FALSE) {
            GriefPrevention.addEventLogEntry(event, claim, location, user, null);
            event.setCancelled(true);
        }
        GPTimings.ENTITY_EXPLOSION_PRE_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST)
    public void onEntityExplosionDetonate(ExplosionEvent.Detonate event) {
        GPTimings.ENTITY_EXPLOSION_DETONATE_EVENT.startTimingIfSync();
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
            GPTimings.ENTITY_EXPLOSION_DETONATE_EVENT.stopTimingIfSync();
            return;
        }

        User user = event.getCause().first(User.class).orElse(null);
        Iterator<Entity> iterator = event.getEntities().iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            Claim claim =  GriefPrevention.instance.dataStore.getClaimAt(entity.getLocation(), false, null);

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
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
            GPTimings.ENTITY_SPAWN_EVENT.stopTimingIfSync();
            return;
        }

        User user = event.getCause().first(User.class).orElse(null);
        event.filterEntities(new Predicate<Entity>() {
            @Override
            public boolean test(Entity entity) {
                Claim claim = GriefPrevention.instance.dataStore.getClaimAt(entity.getLocation(), false, null);
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
                    String entityType = entity.getType() == null ? "unknown" : entity.getType().getId();
                    if (GPPermissionHandler.getClaimPermission(claim, permission, spawnCause, entity, user) == Tristate.FALSE) {
                        GriefPrevention.addEventLogEntry(event, claim, entity.getLocation(), user, "Not allowed to spawn " + entityType + " within claim.");
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
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getTargetEntity().getWorld().getProperties())) {
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
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getTargetEntity().getWorld().getProperties())) {
            GPTimings.ENTITY_DAMAGE_EVENT.stopTimingIfSync();
            return;
        }

        if (protectEntity(event, event.getTargetEntity(), event.getCause(), damageSource)) {
            event.setCancelled(true);
        }
        GPTimings.ENTITY_DAMAGE_EVENT.stopTimingIfSync();
    }

    public boolean protectEntity(Event event, Entity targetEntity, Cause cause, DamageSource damageSource) {
        // monsters are never protected
        User user = cause.first(User.class).orElse(null);
        Optional<Player> player = cause.first(Player.class);
        if (player.isPresent()) {
            PlayerData playerData = GriefPrevention.instance.dataStore.getOrCreatePlayerData(targetEntity.getWorld(), player.get().getUniqueId());
            if (playerData.ignoreClaims) {
                return false;
            }
        }
        if (!GriefPrevention.isEntityProtected(targetEntity)) {
            return false;
        }

        Claim claim = this.dataStore.getClaimAt(targetEntity.getLocation(), false, null);
        // Protect owned entities anywhere in world
        if (damageSource instanceof EntityDamageSource && !(SpongeImplHooks.isCreatureOfType((net.minecraft.entity.Entity) targetEntity, EnumCreatureType.MONSTER))) {
            EntityDamageSource entityDamageSource = (EntityDamageSource) damageSource;
            Entity sourceEntity = entityDamageSource.getSource();
            if (entityDamageSource instanceof IndirectEntityDamageSource) {
                sourceEntity = ((IndirectEntityDamageSource) entityDamageSource).getIndirectSource();
            }

            Tristate perm = Tristate.UNDEFINED;
            // Ignore PvP checks for owned entities
            if (!(sourceEntity instanceof Player) && !(targetEntity instanceof Player)) {
                if (sourceEntity instanceof User) {
                    User sourceUser = (User) sourceEntity;
                    if (sourceUser instanceof Player) {
                        PlayerData playerData = GriefPrevention.instance.dataStore.getOrCreatePlayerData(targetEntity.getWorld(), sourceUser.getUniqueId());
                        if (playerData.ignoreClaims) {
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
                    } else if (sourceUser.getUniqueId().equals(claim.ownerID)) {
                        return true;
                    }
    
                    return false;
                } else {
                    if (targetEntity instanceof Player) {
                        if (SpongeImplHooks.isCreatureOfType((net.minecraft.entity.Entity) entityDamageSource.getSource(), EnumCreatureType.MONSTER)) {
                            if (user == null) {
                                user = ((IMixinEntity) entityDamageSource.getSource()).getTrackedPlayer(NbtDataUtil.SPONGE_ENTITY_CREATOR).orElse(null);
                            }
                            if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.ENTITY_DAMAGE, entityDamageSource.getSource(), targetEntity, user) != Tristate.TRUE) {
                                GriefPrevention.addEventLogEntry(event, claim, targetEntity.getLocation(), user, "Monsters not allowed to attack players within claim.");
                                return true;
                            }
                        }
                    } else if (targetEntity instanceof EntityLivingBase && !SpongeImplHooks.isCreatureOfType((net.minecraft.entity.Entity) targetEntity, EnumCreatureType.MONSTER)) {
                        if (user != null && !user.getUniqueId().equals(claim.ownerID) && perm != Tristate.TRUE) {
                            GriefPrevention.addEventLogEntry(event, claim, targetEntity.getLocation(), user, "Untrusted player attempting to attack entity in claim.");
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

        // if the attacker is a player and defender is a player (pvp combat)
        if (attacker != null && targetEntity instanceof Player && claim.pvpRulesApply()) {
            // FEATURE: prevent pvp in the first minute after spawn, and prevent pvp when one or both players have no inventory
            Player defender = (Player) (targetEntity);

            if (attacker != defender) {
                PlayerData defenderData = this.dataStore.getOrCreatePlayerData(defender.getWorld().getProperties(), defender.getUniqueId());
                PlayerData attackerData = this.dataStore.getOrCreatePlayerData(attacker.getWorld().getProperties(), attacker.getUniqueId());

                // otherwise if protecting spawning players
                if (defenderData.pvpImmune) {
                    GriefPrevention.addEventLogEntry(event, claim, attacker.getLocation(), attacker, "Defender PVP Immune.");
                    GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.ThatPlayerPvPImmune);
                    return true;
                }

                if (attackerData.pvpImmune) {
                    GriefPrevention.addEventLogEntry(event, claim, attacker.getLocation(), attacker, "Attacker PVP Immune.");
                    GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.CantFightWhileImmune);
                    return true;
                }

                // FEATURE: prevent players from engaging in PvP combat inside land claims (when it's disabled)
                Claim attackerClaim = this.dataStore.getClaimAt(attacker.getLocation(), false, attackerData.lastClaim);
                if (!attackerData.ignoreClaims) {
                    // ignore claims mode allows for pvp inside land claims
                    if (attackerClaim != null && !attackerData.inPvpCombat(defender.getWorld()) && attackerClaim.protectPlayersInClaim()) {
                        attackerData.lastClaim = new WeakReference<>(attackerClaim);
                        PreventPvPEvent pvpEvent = new PreventPvPEvent(attackerClaim);
                        Sponge.getGame().getEventManager().post(pvpEvent);
                        if (!pvpEvent.isCancelled()) {
                            GriefPrevention.addEventLogEntry(event, attackerClaim, attacker.getLocation(), attacker, this.dataStore.getMessage(Messages.PlayerInPvPSafeZone));
                            pvpEvent.setCancelled(true);
                            GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.PlayerInPvPSafeZone);
                            return true;
                        }
                    }

                    Claim defenderClaim = this.dataStore.getClaimAt(defender.getLocation(), false, defenderData.lastClaim);
                    if (defenderClaim != null && !defenderData.inPvpCombat(defender.getWorld()) && defenderClaim.protectPlayersInClaim()) {
                        defenderData.lastClaim = new WeakReference<>(defenderClaim);
                        PreventPvPEvent pvpEvent = new PreventPvPEvent(defenderClaim);
                        Sponge.getGame().getEventManager().post(pvpEvent);
                        if (!pvpEvent.isCancelled()) {
                            GriefPrevention.addEventLogEntry(event, attackerClaim, attacker.getLocation(), attacker, this.dataStore.getMessage(Messages.PlayerInPvPSafeZone));
                            pvpEvent.setCancelled(true);
                            GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.PlayerInPvPSafeZone);
                            return true;
                        }
                    }
                }
            }
        } else {
            if (attacker instanceof Player && targetEntity instanceof Player) {
                PlayerData defenderData = this.dataStore.getOrCreatePlayerData(attacker.getWorld().getProperties(), targetEntity.getUniqueId());
                // don't protect players already in combat
                if (defenderData.inPvpCombat(claim.world)) {
                    return false;
                }
                if (!claim.isPvpEnabled()) {
                    GriefPrevention.sendMessage(attacker, TextMode.Err, Messages.PlayerInPvPSafeZone);
                    return true;
                }
            }

            // check perms
            User sourceUser = null;
            if (player.isPresent()) {
                sourceUser = player.get();
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
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getTargetEntity().getWorld().getProperties())) {
            GPTimings.ENTITY_DAMAGE_MONITOR_EVENT.stopTimingIfSync();
            return;
        }

        //FEATURE: prevent players who very recently participated in pvp combat from hiding inventory to protect it from looting
        //FEATURE: prevent players who are in pvp combat from logging out to avoid being defeated

        if (event.getTargetEntity().getType() != EntityTypes.PLAYER || !GriefPrevention.isEntityProtected(event.getTargetEntity())) {
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

        PlayerData playerData = GriefPrevention.instance.dataStore.getOrCreatePlayerData(defender.getWorld(), defender.getUniqueId());
        Claim claim = this.dataStore.getClaimAtPlayer(playerData, defender.getLocation(), false);
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

        PlayerData defenderData = this.dataStore.getOrCreatePlayerData(defender.getWorld().getProperties(), defender.getUniqueId());
        PlayerData attackerData = this.dataStore.getOrCreatePlayerData(attacker.getWorld().getProperties(), attacker.getUniqueId());
        Claim attackerClaim = this.dataStore.getClaimAtPlayer(attackerData, attacker.getLocation(), false);
        Claim defenderClaim = this.dataStore.getClaimAtPlayer(defenderData, defender.getLocation(), false);

        if (attacker != defender) {
            long now = Calendar.getInstance().getTimeInMillis();
            if (defenderClaim != null) {
                if (GriefPrevention.getActiveConfig(defender.getWorld().getProperties()).getConfig().pvp.protectPlayersInClaims) {
                    GPTimings.ENTITY_DAMAGE_MONITOR_EVENT.stopTimingIfSync();
                    return;
                }
            } else if (attackerClaim != null) {
                if (GriefPrevention.getActiveConfig(attacker.getWorld().getProperties()).getConfig().pvp.protectPlayersInClaims) {
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
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
            GPTimings.ENTITY_DROP_ITEM_DEATH_EVENT.stopTimingIfSync();
            return;
        }

        // special rule for creative worlds: killed entities don't drop items or experience orbs
        if (GriefPrevention.instance.claimModeIsActive(livingEntity.getLocation().getExtent().getProperties(), ClaimsMode.Creative)) {
            GriefPrevention.addEventLogEntry(event, null, null, null, "Drops not allowed in creative worlds.");
            event.setCancelled(true);
            GPTimings.ENTITY_DROP_ITEM_DEATH_EVENT.stopTimingIfSync();
            return;
        }

        if (livingEntity instanceof Player) {
            Player player = (Player) livingEntity;
            PlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld().getProperties(), player.getUniqueId());

            // if involved in a siege
            if (playerData.siegeData != null) {
                // end it, with the dieing player being the loser
                this.dataStore.endSiege(playerData.siegeData,
                        event.getCause().first(Player.class).isPresent() ? event.getCause().first(Player.class).get().getName() : null,
                        player.getName(), true);
                // don't drop items as usual, they will be sent to the siege winner
                GriefPrevention.addEventLogEntry(event, null, player.getLocation(), player, "Siege in progress.");
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
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getTargetEntity().getWorld().getProperties())) {
            GPTimings.ENTITY_DEATH_EVENT.stopTimingIfSync();
            return;
        }

        if (!(entity instanceof Player) || !event.getCause().first(EntityDamageSource.class).isPresent()) {
            GPTimings.ENTITY_DEATH_EVENT.stopTimingIfSync();
            return;
        }
        // don't do the rest in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(entity.getWorld().getProperties())) {
            GPTimings.ENTITY_DEATH_EVENT.stopTimingIfSync();
            return;
        }

        Player player = (Player) entity;
        PlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld().getProperties(), player.getUniqueId());
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
        if (entity instanceof IProjectile || entity instanceof EntityItem) {
            GPTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
            return;
        }
        if (event.getFromTransform().getLocation().getBlockPosition().equals(event.getToTransform().getLocation().getBlockPosition())) {
            GPTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
            return;
        }

        World world = event.getTargetEntity().getWorld();
        if (!GriefPrevention.instance.claimsEnabledForWorld(world.getProperties())) {
            GPTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
            return;
        }

        Player player = null;
        PlayerData playerData = null;
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
        Claim fromClaim = this.dataStore.getClaimAt(fromLocation, false, null);
        Claim toClaim = this.dataStore.getClaimAt(toLocation, false, null);

        User user = player != null ? player : owner;
        // enter
        if (fromClaim != toClaim && toClaim != null) {
            if (GPPermissionHandler.getClaimPermission(toClaim, GPPermissions.ENTER_CLAIM, user, entity, user) == Tristate.FALSE) {
                if (player != null) {
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoEnterClaim);
                }

                GriefPrevention.addEventLogEntry(event, toClaim, toLocation, user, this.dataStore.getMessage(Messages.NoEnterClaim));
                event.setCancelled(true);
                GPTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
                return;
            }

            if (playerData != null) {
                playerData.lastClaim = new WeakReference<>(toClaim);
                Text welcomeMessage = toClaim.getClaimData().getGreetingMessage();
                if (welcomeMessage != null && !welcomeMessage.equals(Text.of())) {
                    player.sendMessage(welcomeMessage);
                }
            }
        }

        // exit
        if (fromClaim != toClaim) {
            if (GPPermissionHandler.getClaimPermission(fromClaim, GPPermissions.EXIT_CLAIM, user, entity, user) == Tristate.FALSE) {
                if (player != null) {
                    GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoExitClaim);
                }

                GriefPrevention.addEventLogEntry(event, fromClaim, fromLocation, user, this.dataStore.getMessage(Messages.NoExitClaim));
                event.setCancelled(true);
                GPTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
                return;
            }

            if (playerData != null) {
                playerData.lastClaim = new WeakReference<>(toClaim);
                Text farewellMessage = fromClaim.getClaimData().getFarewellMessage();
                if (farewellMessage != null && !farewellMessage.equals(Text.of())) {
                    player.sendMessage(farewellMessage);
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

        if (user == null || !GriefPrevention.instance.claimsEnabledForWorld(event.getFromTransform().getExtent().getProperties())) {
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
        Claim sourceClaim = null;
        PlayerData playerData = null;
        if (player != null) {
            playerData = GriefPrevention.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
            sourceClaim = this.dataStore.getClaimAtPlayer(playerData, player.getLocation(), false);
        } else {
            sourceClaim = this.dataStore.getClaimAt(sourceLocation, false, null);
        }

        if (sourceClaim != null) {
            if (player != null && GriefPrevention.getActiveConfig(sourceLocation.getExtent().getProperties()).getConfig().siege.siegeEnabled && sourceClaim.siegeData != null) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.SiegeNoTeleport);
                GriefPrevention.addEventLogEntry(event, sourceClaim, sourceLocation, user, this.dataStore.getMessage(Messages.SiegeNoTeleport));
                event.setCancelled(true);
                GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                return;
            }
            if (entityTeleportCause != null) {
                if (GPPermissionHandler.getClaimPermission(sourceClaim, GPPermissions.ENTITY_TELEPORT_FROM, entityTeleportCause.getTeleportType().getId(), entity, user) == Tristate.TRUE) {
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                } else if (GPPermissionHandler.getClaimPermission(sourceClaim, GPPermissions.ENTITY_TELEPORT_FROM, entityTeleportCause.getTeleportType().getId(), entity, user) == Tristate.FALSE) {
                    if (player != null) {
                        GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoTeleportFromProtectedClaim, sourceClaim.getOwnerName());
                    }

                    GriefPrevention.addEventLogEntry(event, sourceClaim, sourceLocation, user, this.dataStore.getMessage(Messages.NoTeleportFromProtectedClaim));
                    event.setCancelled(true);
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                }
            } else if (type.equals(TeleportTypes.PORTAL)) {
                if (GPPermissionHandler.getClaimPermission(sourceClaim, GPPermissions.PORTAL_USE, type.getId(), entity, user) == Tristate.TRUE) {
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                } else if (GPPermissionHandler.getClaimPermission(sourceClaim, GPPermissions.PORTAL_USE, type.getId(), entity, user) == Tristate.FALSE) {
                    if (player != null) {
                        GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoPortalToProtectedClaim, sourceClaim.getOwnerName());
                    }

                    GriefPrevention.addEventLogEntry(event, sourceClaim, sourceLocation, user, this.dataStore.getMessage(Messages.NoPortalToProtectedClaim));
                    event.setCancelled(true);
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                } else if (GPPermissionHandler.getClaimPermission(sourceClaim, GPPermissions.BLOCK_PLACE, entity, null, user) == Tristate.FALSE) {
                    if (player != null) {
                        GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoBuildPortalPermission, sourceClaim.getOwnerName());
                    }

                    GriefPrevention.addEventLogEntry(event, sourceClaim, sourceLocation, user, this.dataStore.getMessage(Messages.NoBuildPortalPermission));
                    event.setCancelled(true);
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                }
            }
        }

        // check if destination world is enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getToTransform().getExtent().getProperties())) {
            GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
            return;
        }

        Location<World> destination = event.getToTransform().getLocation();
        Claim toClaim = this.dataStore.getClaimAt(destination, false, null);
        if (toClaim != null) {
            if (player != null && GriefPrevention.getActiveConfig(destination.getExtent().getProperties()).getConfig().siege.siegeEnabled && toClaim.siegeData != null) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.BesiegedNoTeleport);
                GriefPrevention.addEventLogEntry(event, toClaim, destination, user, this.dataStore.getMessage(Messages.BesiegedNoTeleport));
                event.setCancelled(true);
                GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                return;
            }

            // FEATURE: prevent players from using entities to gain access to secured claims
            if (entityTeleportCause != null) {
                if (GPPermissionHandler.getClaimPermission(toClaim, GPPermissions.ENTITY_TELEPORT_TO, entityTeleportCause.getTeleportType().getId(), entity, user) == Tristate.TRUE) {
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                } else if (GPPermissionHandler.getClaimPermission(toClaim, GPPermissions.ENTITY_TELEPORT_TO, entityTeleportCause.getTeleportType().getId(), entity, user) == Tristate.FALSE) {
                    if (player != null) {
                        GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoTeleportToProtectedClaim, toClaim.getOwnerName());
                    }

                    GriefPrevention.addEventLogEntry(event, toClaim, destination, user, this.dataStore.getMessage(Messages.NoTeleportToProtectedClaim));
                    event.setCancelled(true);
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                }
                String denyReason = toClaim.allowAccess(user);
                if (denyReason != null) {
                    if (player != null) {
                        GriefPrevention.sendMessage(player, Text.of(TextMode.Err, denyReason));
                    }

                    GriefPrevention.addEventLogEntry(event, toClaim, destination, user, denyReason);
                    event.setCancelled(true);
                    if (entityTeleportCause != null && entityTeleportCause.getTeleporter().getType().equals(EntityTypes.ENDER_PEARL)) {
                        ((EntityPlayer) player).inventory.addItemStackToInventory(new net.minecraft.item.ItemStack(Items.ENDER_PEARL));
                    }
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                }
            } else if (type.equals(TeleportTypes.PORTAL)) {
                if (GPPermissionHandler.getClaimPermission(toClaim, GPPermissions.PORTAL_USE, entity, null, user) == Tristate.TRUE) {
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                } else if (GPPermissionHandler.getClaimPermission(toClaim, GPPermissions.PORTAL_USE, entity, null, user) == Tristate.FALSE) {
                    if (player != null) {
                        GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoPortalToProtectedClaim, toClaim.getOwnerName());
                    }

                    GriefPrevention.addEventLogEntry(event, toClaim, destination, user, this.dataStore.getMessage(Messages.NoPortalToProtectedClaim));
                    event.setCancelled(true);
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                } else if (GPPermissionHandler.getClaimPermission(toClaim, GPPermissions.BLOCK_PLACE, entity, null, user) == Tristate.FALSE) {
                    if (player != null) {
                        GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoBuildPortalPermission, toClaim.getOwnerName());
                    }

                    GriefPrevention.addEventLogEntry(event, toClaim, destination, user, this.dataStore.getMessage(Messages.NoBuildPortalPermission));
                    event.setCancelled(true);
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                }
            } else if (type.equals(TeleportTypes.COMMAND)) {
                // TODO
            }
        }

        if (player != null && !sourceLocation.getExtent().getUniqueId().equals(destination.getExtent().getUniqueId())) {
            // new world, check if player has world storage for it
            ClaimWorldManager claimWorldManager = GriefPrevention.instance.dataStore.getClaimWorldManager(destination.getExtent().getProperties());

            // update lastActive timestamps for claims this player owns
            WorldProperties worldProperties = destination.getExtent().getProperties();
            UUID playerUniqueId = player.getUniqueId();
            for (Claim claim : this.dataStore.getClaimWorldManager(worldProperties).getWorldClaims()) {
                if (claim.ownerID.equals(playerUniqueId)) {
                    // update lastActive timestamp for claim
                    claim.getClaimData().setDateLastActive(Instant.now().toString());
                    claimWorldManager.addWorldClaim(claim);
                } else if (claim.parent != null && claim.parent.ownerID.equals(playerUniqueId)) {
                    // update lastActive timestamp for subdivisions if parent owner logs on
                    claim.getClaimData().setDateLastActive(Instant.now().toString());
                    claimWorldManager.addWorldClaim(claim);
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
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
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

                // always allow collisions with players
                if (entity instanceof Player || entity instanceof EntityItem) {
                    return true;
                }

                User owner = null;
                int entityId = ((net.minecraft.entity.Entity) entity).getEntityId();
                PlayerData playerData = GriefPrevention.instance.dataStore.getOrCreatePlayerData(entity.getWorld(), user.getUniqueId());
                Claim claim = GriefPrevention.instance.dataStore.getClaimAt(entity.getLocation(), false, null);
                if (playerData.lastCollideEntityId == entityId && playerData.lastClaim != null && playerData.lastClaim.get() != null) {
                    if (claim.id.equals(playerData.lastClaim.get().id)) {
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
                playerData.setLastCollideEntityData(entityId, true);
                return true;
            }
        });
        GPTimings.ENTITY_COLLIDE_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST)
    public void onProjectileImpactEntity(CollideEntityEvent.Impact event, @First User user) {
        GPTimings.PROJECTILE_IMPACT_ENTITY_EVENT.startTimingIfSync();
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getImpactPoint().getExtent().getProperties())) {
            GPTimings.PROJECTILE_IMPACT_ENTITY_EVENT.stopTimingIfSync();
            return;
        }

        Location<World> impactPoint = event.getImpactPoint();
        for (Entity entity : event.getEntities()) {
            Claim targetClaim = null;
            PlayerData playerData = null;
            if (user instanceof Player) {
                playerData = GriefPrevention.instance.dataStore.getOrCreatePlayerData(impactPoint.getExtent(), user.getUniqueId());
                targetClaim = this.dataStore.getClaimAtPlayer(playerData, impactPoint, false);
            } else {
                targetClaim = this.dataStore.getClaimAt(impactPoint, false, null);
            }
    
            String denyReason = targetClaim.allowAccess(user, impactPoint);
            if (denyReason != null) {
                if (GPPermissionHandler.getClaimPermission(targetClaim, GPPermissions.PROJECTILE_IMPACT_ENTITY, event.getCause().root(), entity, user) == Tristate.TRUE) {
                    GPTimings.PROJECTILE_IMPACT_ENTITY_EVENT.stopTimingIfSync();
                    return;
                }

                GriefPrevention.addEventLogEntry(event, targetClaim, impactPoint, user, denyReason);
                event.setCancelled(true);
            } else {
                if (GPPermissionHandler.getClaimPermission(targetClaim, GPPermissions.PROJECTILE_IMPACT_ENTITY, event.getCause().root(), entity, user) == Tristate.FALSE) {
                    GriefPrevention.addEventLogEntry(event, targetClaim, impactPoint, user, denyReason);
                    event.setCancelled(true);
                    GPTimings.PROJECTILE_IMPACT_ENTITY_EVENT.stopTimingIfSync();
                    return;
                }
            }
        }
        GPTimings.PROJECTILE_IMPACT_ENTITY_EVENT.stopTimingIfSync();
    }
}
