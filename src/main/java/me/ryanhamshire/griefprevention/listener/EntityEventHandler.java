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
import me.ryanhamshire.griefprevention.GPFlags;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GPTimings;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimFlag;
import me.ryanhamshire.griefprevention.api.claim.TrustType;
import me.ryanhamshire.griefprevention.claim.ClaimsMode;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.claim.GPClaimManager;
import me.ryanhamshire.griefprevention.event.GPAttackPlayerEvent;
import me.ryanhamshire.griefprevention.event.GPBorderClaimEvent;
import me.ryanhamshire.griefprevention.permission.GPBlacklists;
import me.ryanhamshire.griefprevention.permission.GPPermissionHandler;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import me.ryanhamshire.griefprevention.provider.MCClansApiProvider;
import me.ryanhamshire.griefprevention.util.CauseContextHelper;
import me.ryanhamshire.griefprevention.util.EntityUtils;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.ContainerPlayer;
import nl.riebie.mcclans.api.ClanPlayer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.ExperienceOrb;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.entity.projectile.Projectile;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.Event;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.EventContext;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.event.cause.entity.damage.source.DamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.EntityDamageSource;
import org.spongepowered.api.event.cause.entity.damage.source.IndirectEntityDamageSource;
import org.spongepowered.api.event.cause.entity.teleport.TeleportType;
import org.spongepowered.api.event.cause.entity.teleport.TeleportTypes;
import org.spongepowered.api.event.entity.AttackEntityEvent;
import org.spongepowered.api.event.entity.CollideEntityEvent;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.entity.SpawnEntityEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.chat.ChatType;
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
        if (!GPFlags.EXPLOSION || !GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
            return;
        }
        if (GriefPreventionPlugin.isSourceIdBlacklisted(ClaimFlag.EXPLOSION.toString(), event.getSource(), event.getTargetWorld().getProperties())) {
            return;
        }

        GPTimings.ENTITY_EXPLOSION_PRE_EVENT.startTimingIfSync();
        Location<World> location = event.getExplosion().getLocation();
        GPClaim claim =  GriefPreventionPlugin.instance.dataStore.getClaimAt(location);
        User user = CauseContextHelper.getEventUser(event);
        Object source = event.getSource();
        if (source instanceof Explosion) {
            final Explosion explosion = (Explosion) source;
            if (explosion.getSourceExplosive().isPresent()) {
                source = explosion.getSourceExplosive().get();
            } else {
                Entity exploder = event.getCause().first(Entity.class).orElse(null);
                if (exploder != null) {
                    source = exploder;
                }
            }
        }

        Tristate result = Tristate.UNDEFINED;
        if (GPFlags.EXPLOSION_SURFACE && location.getPosition().getY() > ((net.minecraft.world.World) location.getExtent()).getSeaLevel()) {
            result = GPPermissionHandler.getClaimPermission(event, location, claim, GPPermissions.EXPLOSION_SURFACE, source, location.getBlock(), user, true);
        } else {
            result = GPPermissionHandler.getClaimPermission(event, location, claim, GPPermissions.EXPLOSION, source, location.getBlock(), user, true);
        }

        if(result == Tristate.FALSE) {
            event.setCancelled(true);
            GPTimings.ENTITY_EXPLOSION_PRE_EVENT.stopTimingIfSync();
            return;
        }

        GPTimings.ENTITY_EXPLOSION_PRE_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntityExplosionDetonate(ExplosionEvent.Detonate event) {
        if (!GPFlags.EXPLOSION || !GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
            return;
        }
        if (GriefPreventionPlugin.isSourceIdBlacklisted(ClaimFlag.EXPLOSION.toString(), event.getSource(), event.getTargetWorld().getProperties())) {
            return;
        }

        GPTimings.ENTITY_EXPLOSION_DETONATE_EVENT.startTimingIfSync();
        final User user = CauseContextHelper.getEventUser(event);
        Iterator<Entity> iterator = event.getEntities().iterator();
        GPClaim targetClaim = null;
        Object source = event.getSource();
        if (source instanceof Explosion) {
            final Explosion explosion = (Explosion) source;
            if (explosion.getSourceExplosive().isPresent()) {
                source = explosion.getSourceExplosive().get();
            } else {
                Entity exploder = event.getCause().first(Entity.class).orElse(null);
                if (exploder != null) {
                    source = exploder;
                }
            }
        }

        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            targetClaim =  GriefPreventionPlugin.instance.dataStore.getClaimAt(entity.getLocation(), targetClaim);
            if (GPPermissionHandler.getClaimPermission(event, entity.getLocation(), targetClaim, GPPermissions.ENTITY_DAMAGE, source, entity, user) == Tristate.FALSE) {
                iterator.remove();
            }
        }
        GPTimings.ENTITY_EXPLOSION_DETONATE_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntitySpawn(SpawnEntityEvent event) {
        Object source = event.getSource();
        if (source instanceof ConsoleSource || !GPFlags.ENTITY_SPAWN || event.getEntities().isEmpty()) {
            return;
        }

        // If root cause is damage source, look for target as that should be passed instead
        // Ex. Entity dies and drops an item would be after EntityDamageSource
        if (source instanceof DamageSource) {
            final Object target = event.getCause().after(DamageSource.class).orElse(null);
            if (target != null) {
                source = target;
            }
        }

        final boolean isChunkSpawn = event instanceof SpawnEntityEvent.ChunkLoad;
        if (isChunkSpawn && !GPFlags.ENTITY_CHUNK_SPAWN) {
            return;
        }
        if (event instanceof DropItemEvent) {
            if (!GPFlags.ITEM_DROP) {
                return;
            }
            // only handle item spawns from non-living
            if (source instanceof Living || event.getCause().containsType(ContainerPlayer.class)) {
                return;
            }
        }

        final World world = event.getEntities().get(0).getWorld();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(world.getProperties())) {
            return;
        }
        if (GriefPreventionPlugin.isSourceIdBlacklisted(ClaimFlag.ENTITY_SPAWN.toString(), source, world.getProperties())) {
            return;
        }
        if (isChunkSpawn && GriefPreventionPlugin.isSourceIdBlacklisted(ClaimFlag.ENTITY_CHUNK_SPAWN.toString(), source, world.getProperties())) {
            return;
        }

        GPTimings.ENTITY_SPAWN_EVENT.startTimingIfSync();
        final User user = CauseContextHelper.getEventUser(event);
        final Object actualSource = source;
        event.filterEntities(new Predicate<Entity>() {
            GPClaim targetClaim = null;

            @Override
            public boolean test(Entity entity) {
                if (entity instanceof ExperienceOrb) {
                    return true;
                }

                if (GriefPreventionPlugin.isTargetIdBlacklisted(ClaimFlag.ENTITY_SPAWN.toString(), entity, world.getProperties())) {
                    return true;
                }

                targetClaim = GriefPreventionPlugin.instance.dataStore.getClaimAt(entity.getLocation(), targetClaim);
                if (targetClaim == null) {
                    return true;
                }

                String permission = GPPermissions.ENTITY_SPAWN;
                if (isChunkSpawn) {
                    if (GriefPreventionPlugin.isTargetIdBlacklisted(ClaimFlag.ENTITY_CHUNK_SPAWN.toString(), entity, world.getProperties())) {
                        return true;
                    }
                    permission = GPPermissions.ENTITY_CHUNK_SPAWN;
                }

                if (!isChunkSpawn && entity instanceof Item) {
                    if (user == null) {
                        return true;
                    }
                    if (!GPFlags.ITEM_SPAWN) {
                        return true;
                    }
                    if (GriefPreventionPlugin.isTargetIdBlacklisted(ClaimFlag.ITEM_SPAWN.toString(), entity, world.getProperties())) {
                        return true;
                    }
                    permission = GPPermissions.ITEM_SPAWN;
                    if (actualSource instanceof BlockSnapshot) {
                        final BlockSnapshot block = (BlockSnapshot) actualSource;
                        final Location<World> location = block.getLocation().orElse(null);
                        if (location != null) {
                            if (GriefPreventionPlugin.isTargetIdBlacklisted(ClaimFlag.BLOCK_BREAK.toString(), block, world.getProperties())) {
                                return true;
                            }
                            final Tristate result = GPPermissionHandler.getClaimPermission(event, location, targetClaim, GPPermissions.BLOCK_BREAK, actualSource, block, user, TrustType.ACCESSOR, true);
                            if (result != Tristate.UNDEFINED) {
                                if (result == Tristate.TRUE) {
                                    // Check if item drop is allowed
                                    if (GPPermissionHandler.getClaimPermission(event, location, targetClaim, permission, actualSource, entity, user, TrustType.ACCESSOR, true) == Tristate.FALSE) {
                                        return false;
                                    }
                                    return true;
                                }
                                return false;
                            }
                        }
                    }
                }

                if (GPPermissionHandler.getClaimPermission(event, entity.getLocation(), targetClaim, permission, actualSource, entity, user, TrustType.ACCESSOR, true) == Tristate.FALSE) {
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
        if (protectEntity(event, event.getTargetEntity(), event.getCause(), damageSource)) {
            event.setCancelled(true);
        }
        GPTimings.ENTITY_ATTACK_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntityDamage(DamageEntityEvent event, @First DamageSource damageSource) {
        GPTimings.ENTITY_DAMAGE_EVENT.startTimingIfSync();
        if (protectEntity(event, event.getTargetEntity(), event.getCause(), damageSource)) {
            event.setCancelled(true);
        }
        GPTimings.ENTITY_DAMAGE_EVENT.stopTimingIfSync();
    }

    public boolean protectEntity(Event event, Entity targetEntity, Cause cause, DamageSource damageSource) {
        if (!GPFlags.ENTITY_DAMAGE || !GriefPreventionPlugin.instance.claimsEnabledForWorld(targetEntity.getWorld().getProperties())) {
            return false;
        }
        if (GriefPreventionPlugin.isTargetIdBlacklisted(ClaimFlag.ENTITY_DAMAGE.toString(), targetEntity, targetEntity.getWorld().getProperties())) {
            return false;
        }

        User user = CauseContextHelper.getEventUser(event);
        Player player = cause.first(Player.class).orElse(null);
        Object source = damageSource;
        EntityDamageSource entityDamageSource = null;
        final TileEntity tileEntity = cause.first(TileEntity.class).orElse(null);
        // TE takes priority over entity damage sources
        if (tileEntity != null) {
            source = tileEntity;
        } else if (damageSource instanceof EntityDamageSource) {
            entityDamageSource = (EntityDamageSource) damageSource;
            source = entityDamageSource.getSource();
            if (entityDamageSource instanceof IndirectEntityDamageSource) {
                final Entity indirectSource = ((IndirectEntityDamageSource) entityDamageSource).getIndirectSource();
                if (indirectSource != null) {
                    source = indirectSource;
                }
            }
            if (source instanceof Player) {
                if (user == null) {
                    user = (User) source;
                }
                if (player == null) {
                    player = (Player) source;
                }
            }
        }

        if (GriefPreventionPlugin.isSourceIdBlacklisted(ClaimFlag.ENTITY_DAMAGE.toString(), source, targetEntity.getWorld().getProperties())) {
            return false;
        }

        GPPlayerData playerData = null;
        if (player != null) {
            playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(targetEntity.getWorld(), player.getUniqueId());
        }
        if (!GriefPreventionPlugin.isEntityProtected(targetEntity)) {
            return false;
        }

        final GPClaim claim = this.dataStore.getClaimAt(targetEntity.getLocation(), playerData != null ? playerData.lastClaim.get() : null);
        final TrustType trustType = TrustType.BUILDER;
        if (GPPermissionHandler.getClaimPermission(event, targetEntity.getLocation(), claim, GPPermissions.ENTITY_DAMAGE, source, targetEntity, user, trustType, true) == Tristate.FALSE) {
            return true;
        }

        // allow trusted users to attack entities within claim
        if (!(targetEntity instanceof Player) && claim.isUserTrusted(user, TrustType.ACCESSOR)) {
            return false;
        }

        // Protect owned entities anywhere in world
        if (entityDamageSource != null && !(SpongeImplHooks.isCreatureOfType((net.minecraft.entity.Entity) targetEntity, EnumCreatureType.MONSTER))) {
            Tristate perm = Tristate.UNDEFINED;
            // Ignore PvP checks for owned entities
            if (!(source instanceof Player) && !(targetEntity instanceof Player)) {
                if (source instanceof User) {
                    User sourceUser = (User) source;
                    perm = GPPermissionHandler.getClaimPermission(event, targetEntity.getLocation(), claim, GPPermissions.ENTITY_DAMAGE, source, targetEntity, sourceUser, trustType, true);
                    if (targetEntity instanceof Living && perm == Tristate.TRUE) {
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
                        if (SpongeImplHooks.isCreatureOfType((net.minecraft.entity.Entity) source, EnumCreatureType.MONSTER)) {
                            if (GPPermissionHandler.getClaimPermission(event, targetEntity.getLocation(), claim, GPPermissions.ENTITY_DAMAGE, source, targetEntity, user, trustType, true) != Tristate.TRUE) {
                                return true;
                            }
                        }
                    } else if (targetEntity instanceof Living && !SpongeImplHooks.isCreatureOfType((net.minecraft.entity.Entity) targetEntity, EnumCreatureType.MONSTER)) {
                        if (user != null && !user.getUniqueId().equals(claim.getOwnerUniqueId()) && perm != Tristate.TRUE) {
                            return true;
                        }
                    }
                }
            }
        }

        // the rest is only interested in entities damaging entities (ignoring environmental damage)
        if (entityDamageSource == null || tileEntity != null) {
            return false;
        }

        // determine which player is attacking, if any
        Player attacker = null;
        Projectile arrow = null;

        if (source != null) {
            if (source instanceof Player) {
                attacker = (Player) source;
            } else if (source instanceof Projectile) {
                arrow = (Projectile) source;
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
                GPClaim attackerClaim = this.dataStore.getClaimAt(attacker.getLocation(), attackerData.lastClaim.get());
                if (!attackerData.canIgnoreClaim(attackerClaim)) {
                    try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                        Sponge.getCauseStackManager().pushCause(entityDamageSource);
                        // ignore claims mode allows for pvp inside land claims
                        if (attackerClaim != null && !attackerData.inPvpCombat(defender.getWorld()) && attackerClaim.protectPlayersInClaim()) {
                            attackerData.lastClaim = new WeakReference<>(attackerClaim);
                            GPAttackPlayerEvent
                                pvpEvent =
                                new GPAttackPlayerEvent(attackerClaim, defender);
                            Sponge.getEventManager().post(pvpEvent);
                            if (!pvpEvent.isCancelled()) {
                                pvpEvent.setCancelled(true);
                                GriefPreventionPlugin.sendMessage(attacker, GriefPreventionPlugin.instance.messageData.pvpPlayerSafeZone.toText());
                                return true;
                            }
                        }

                        GPClaim defenderClaim = this.dataStore.getClaimAt(defender.getLocation(), defenderData.lastClaim.get());
                        if (defenderClaim != null && !defenderData.inPvpCombat(defender.getWorld()) && defenderClaim.protectPlayersInClaim()) {
                            defenderData.lastClaim = new WeakReference<>(defenderClaim);
                            GPAttackPlayerEvent
                                pvpEvent =
                                new GPAttackPlayerEvent(defenderClaim, defender);
                            Sponge.getEventManager().post(pvpEvent);
                            if (!pvpEvent.isCancelled()) {
                                pvpEvent.setCancelled(true);
                                GriefPreventionPlugin.sendMessage(attacker, GriefPreventionPlugin.instance.messageData.pvpPlayerSafeZone.toText());
                                return true;
                            }
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

        if (GPPermissionHandler.getClaimPermission(event, targetEntity.getLocation(), claim, GPPermissions.ENTITY_DAMAGE, attacker, targetEntity, user, trustType, true) == Tristate.FALSE) {
            return true;
        }

        return false;
    }

    @Listener(order = Order.POST)
    public void onEntityDamageMonitor(DamageEntityEvent event) {
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getTargetEntity().getWorld().getProperties())) {
            return;
        }

        GPTimings.ENTITY_DAMAGE_MONITOR_EVENT.startTimingIfSync();
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
        GPClaim claim = this.dataStore.getClaimAtPlayer(playerData, defender.getLocation());
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
        GPClaim attackerClaim = this.dataStore.getClaimAtPlayer(attackerData, attacker.getLocation());
        GPClaim defenderClaim = this.dataStore.getClaimAtPlayer(defenderData, defender.getLocation());

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
    public void onEntityDropItemDeath(DropItemEvent.Destruct event) {
        if (!GPFlags.ITEM_DROP || event.getEntities().isEmpty()) {
            return;
        }

        final World world = event.getEntities().get(0).getWorld();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(world.getProperties())) {
            return;
        }

        Object source = event.getSource();
        // If root cause is damage source, look for target as that should be passed instead
        // Ex. Entity dies and drops an item would be after EntityDamageSource
        if (source instanceof DamageSource) {
            final Object target = event.getCause().after(DamageSource.class).orElse(null);
            if (target != null) {
                source = target;
            }
        }
        if (!(source instanceof Entity)) {
            return;
        }

        final Entity entity = (Entity) source;
        if (GriefPreventionPlugin.isSourceIdBlacklisted(ClaimFlag.ITEM_DROP.toString(), entity, world.getProperties())) {
            return;
        }

        GPTimings.ENTITY_DROP_ITEM_DEATH_EVENT.startTimingIfSync();
        // special rule for creative worlds: killed entities don't drop items or experience orbs
        if (GriefPreventionPlugin.instance.claimModeIsActive(entity.getLocation().getExtent().getProperties(), ClaimsMode.Creative)) {
            event.setCancelled(true);
            GPTimings.ENTITY_DROP_ITEM_DEATH_EVENT.stopTimingIfSync();
            return;
        }

        final User user = CauseContextHelper.getEventUser(event);
        event.filterEntities(new Predicate<Entity>() {
            GPClaim targetClaim = null;

            @Override
            public boolean test(Entity item) {
                if (GriefPreventionPlugin.isTargetIdBlacklisted(ClaimFlag.ITEM_DROP.toString(), item, world.getProperties())) {
                    return true;
                }

                targetClaim = GriefPreventionPlugin.instance.dataStore.getClaimAt(item.getLocation(), targetClaim);
                if (targetClaim == null) {
                    return true;
                }

                if (user == null) {
                    return true;
                }
                if (GriefPreventionPlugin.isTargetIdBlacklisted(ClaimFlag.ITEM_DROP.toString(), item, world.getProperties())) {
                    return true;
                }

                if (GPPermissionHandler.getClaimPermission(event, item.getLocation(), targetClaim, GPPermissions.ITEM_DROP, entity, item, user, TrustType.ACCESSOR, true) == Tristate.FALSE) {
                    return false;
                }
                return true;
            }
        });

        GPTimings.ENTITY_DROP_ITEM_DEATH_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntityMove(MoveEntityEvent event){
        if ((!GPFlags.ENTER_CLAIM && !GPFlags.EXIT_CLAIM) || event.getFromTransform().getLocation().getBlockPosition().equals(event.getToTransform().getLocation().getBlockPosition())) {
            return;
        }

        final Entity entity = event.getTargetEntity();
        World world = event.getTargetEntity().getWorld();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(world.getProperties())) {
            return;
        }
        final boolean enterBlacklisted = GriefPreventionPlugin.isSourceIdBlacklisted(ClaimFlag.ENTER_CLAIM.toString(), entity, world.getProperties());
        final boolean exitBlacklisted = GriefPreventionPlugin.isSourceIdBlacklisted(ClaimFlag.EXIT_CLAIM.toString(), entity, world.getProperties());
        if (enterBlacklisted && exitBlacklisted) {
            return;
        }

        GPTimings.ENTITY_MOVE_EVENT.startTimingIfSync();
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
            GPClaim fromClaim = this.dataStore.getClaimAt(fromLocation);
            GPClaim toClaim = this.dataStore.getClaimAt(toLocation);
            if (fromClaim != toClaim) {
                GPBorderClaimEvent gpEvent = new GPBorderClaimEvent(entity, fromClaim, toClaim);
                // enter
                if (GPFlags.ENTER_CLAIM && !enterBlacklisted && GPPermissionHandler.getClaimPermission(event, toLocation, toClaim, GPPermissions.ENTER_CLAIM, entity, entity, null) == Tristate.FALSE) {
                    gpEvent.setCancelled(true);
                }

                // exit
                if (GPFlags.EXIT_CLAIM && !exitBlacklisted && GPPermissionHandler.getClaimPermission(event, fromLocation, fromClaim, GPPermissions.EXIT_CLAIM, entity, entity, null) == Tristate.FALSE) {
                    gpEvent.setCancelled(true);
                }

                Sponge.getEventManager().post(gpEvent);
                if (gpEvent.isCancelled()) {
                    event.setCancelled(true);
                    if (!(entity instanceof Player) && EntityUtils.getOwnerUniqueId(entity) == null) {
                        ((net.minecraft.entity.Entity) entity).setDead();
                    }
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

        if (GPFlags.ENTER_CLAIM && !enterBlacklisted && playerData != null && playerData.lastClaim != null) {
            final GPClaim lastClaim = (GPClaim) playerData.lastClaim.get();
            if (lastClaim != null && lastClaim != fromClaim) {
                if (GPPermissionHandler.getClaimPermission(event, toLocation, toClaim, GPPermissions.ENTER_CLAIM, entity, entity, player, TrustType.ACCESSOR, false) == Tristate.FALSE) {
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
        GPBorderClaimEvent gpEvent = new GPBorderClaimEvent(entity, fromClaim, toClaim);
        if (user != null && toClaim.isUserTrusted(user, TrustType.ACCESSOR)) {
            Sponge.getEventManager().post(gpEvent);
            if (gpEvent.isCancelled()) {
                event.setCancelled(true);
                if (!(entity instanceof Player) && EntityUtils.getOwnerUniqueId(entity) == null) {
                    ((net.minecraft.entity.Entity) entity).setDead();
                }
                final Text cancelMessage = gpEvent.getMessage().orElse(null);
                if (player != null && cancelMessage != null) {
                    player.sendMessage(cancelMessage);
                }
            } else {
                if (playerData != null) {
                    final boolean showGpPrefix = GriefPreventionPlugin.getGlobalConfig().getConfig().message.showGpPrefixGreetingFarewell;
                    playerData.lastClaim = new WeakReference<>(toClaim);
                    Text welcomeMessage = gpEvent.getEnterMessage().orElse(null);
                    if (welcomeMessage != null && !welcomeMessage.equals(Text.of())) {
                        ChatType chatType = gpEvent.getEnterMessageChatType();
                        if (showGpPrefix) {
                            player.sendMessage(chatType, Text.of(enterClanTag != null ? enterClanTag : GriefPreventionPlugin.GP_TEXT, welcomeMessage));
                        } else {
                            player.sendMessage(chatType, Text.of(enterClanTag != null ? enterClanTag : welcomeMessage));
                        }
                    }

                    Text farewellMessage = gpEvent.getExitMessage().orElse(null);
                    if (farewellMessage != null && !farewellMessage.equals(Text.of())) {
                        ChatType chatType = gpEvent.getExitMessageChatType();
                        if (showGpPrefix) {
                            player.sendMessage(chatType, Text.of(exitClanTag != null ? exitClanTag : GriefPreventionPlugin.GP_TEXT, farewellMessage));
                        } else {
                            player.sendMessage(chatType, Text.of(exitClanTag != null ? exitClanTag : farewellMessage));
                        }
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
            if (GPFlags.ENTER_CLAIM && !enterBlacklisted && GPPermissionHandler.getClaimPermission(event, toLocation, toClaim, GPPermissions.ENTER_CLAIM, entity, entity, user) == Tristate.FALSE) {
                enterCancelled = true;
                gpEvent.setCancelled(true);
            }

            // exit
            if (GPFlags.EXIT_CLAIM && !exitBlacklisted && GPPermissionHandler.getClaimPermission(event, fromLocation, fromClaim, GPPermissions.EXIT_CLAIM, entity, entity, user) == Tristate.FALSE) {
                exitCancelled = true;
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
                if (!(entity instanceof Player) && EntityUtils.getOwnerUniqueId(entity) == null) {
                    ((net.minecraft.entity.Entity) entity).setDead();
                }
                GPTimings.ENTITY_MOVE_EVENT.stopTimingIfSync();
                return;
            }

            if (playerData != null) {
                final boolean showGpPrefix = GriefPreventionPlugin.getGlobalConfig().getConfig().message.showGpPrefixGreetingFarewell;
                playerData.lastClaim = new WeakReference<>(toClaim);
                Text welcomeMessage = gpEvent.getEnterMessage().orElse(null);
                if (welcomeMessage != null && !welcomeMessage.equals(Text.of())) {
                    ChatType chatType = gpEvent.getEnterMessageChatType();
                    if (showGpPrefix) {
                        player.sendMessage(chatType, Text.of(enterClanTag != null ? enterClanTag : GriefPreventionPlugin.GP_TEXT, welcomeMessage));
                    } else {
                        player.sendMessage(chatType, Text.of(enterClanTag != null ? enterClanTag : welcomeMessage));
                    }
                }

                Text farewellMessage = gpEvent.getExitMessage().orElse(null);
                if (farewellMessage != null && !farewellMessage.equals(Text.of())) {
                    ChatType chatType = gpEvent.getExitMessageChatType();
                    if (showGpPrefix) {
                        player.sendMessage(chatType, Text.of(exitClanTag != null ? exitClanTag : GriefPreventionPlugin.GP_TEXT, farewellMessage));
                    } else {
                        player.sendMessage(chatType, Text.of(exitClanTag != null ? exitClanTag : farewellMessage));
                    }
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
    public void onEntityTeleport(MoveEntityEvent.Teleport event) {
        if (!GPFlags.ENTITY_TELEPORT_FROM && !GPFlags.ENTITY_TELEPORT_TO) {
            return;
        }

        final Entity entity = event.getTargetEntity();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(entity.getWorld().getProperties())) {
            return;
        }
        final boolean teleportFromBlacklisted = GriefPreventionPlugin.isSourceIdBlacklisted(ClaimFlag.ENTITY_TELEPORT_FROM.toString(), entity, entity.getWorld().getProperties());
        final boolean teleportToBlacklisted = GriefPreventionPlugin.isSourceIdBlacklisted(ClaimFlag.ENTITY_TELEPORT_TO.toString(), entity, entity.getWorld().getProperties());
        final boolean portalUseBlacklisted = GriefPreventionPlugin.isSourceIdBlacklisted(ClaimFlag.PORTAL_USE.toString(), entity, entity.getWorld().getProperties());
        if (teleportFromBlacklisted && teleportToBlacklisted && portalUseBlacklisted) {
            return;
        }

        GPTimings.ENTITY_TELEPORT_EVENT.startTimingIfSync();
        Player player = null;
        User user = null;
        if (entity instanceof Player) {
            player = (Player) entity;
            user = player;
        } else {
            user = ((IMixinEntity) entity).getTrackedPlayer(NbtDataUtil.SPONGE_ENTITY_CREATOR).orElse(null);
        }

        if (user == null) {
            GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
            return;
        }

        final Cause cause = event.getCause();
        final EventContext context = cause.getContext();

        final TeleportType type = context.get(EventContextKeys.TELEPORT_TYPE).orElse(TeleportTypes.ENTITY_TELEPORT);
        final Location<World> sourceLocation = event.getFromTransform().getLocation();
        GPClaim sourceClaim = null;
        GPPlayerData playerData = null;
        if (player != null) {
            playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
            sourceClaim = this.dataStore.getClaimAtPlayer(playerData, player.getLocation());
        } else {
            sourceClaim = this.dataStore.getClaimAt(sourceLocation);
        }

        if (sourceClaim != null) {
            if (GPFlags.ENTITY_TELEPORT_FROM && !teleportFromBlacklisted && GPPermissionHandler.getClaimPermission(event, sourceLocation, sourceClaim, GPPermissions.ENTITY_TELEPORT_FROM, type, entity, user, TrustType.ACCESSOR, true) == Tristate.FALSE) {
                boolean cancelled = true;
                if (GPFlags.PORTAL_USE && type.equals(TeleportTypes.PORTAL)) {
                    if (portalUseBlacklisted || GPPermissionHandler.getClaimPermission(event, sourceLocation, sourceClaim, GPPermissions.PORTAL_USE, type, entity, user) == Tristate.TRUE) {
                        cancelled = false;
                    }
                }
                if (cancelled) {
                    final Text message = GriefPreventionPlugin.instance.messageData.permissionPortalExit
                            .apply(ImmutableMap.of(
                            "owner", sourceClaim.getOwnerName())).build();
                    if (player != null) {
                        GriefPreventionPlugin.sendMessage(player, message);
                    }

                    event.setCancelled(true);
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                }
            }
        }

        // check if destination world is enabled
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getToTransform().getExtent().getProperties())) {
            GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
            return;
        }

        final Location<World> destination = event.getToTransform().getLocation();
        final GPClaim toClaim = this.dataStore.getClaimAt(destination);
        if (toClaim != null) {
            if (GPFlags.ENTITY_TELEPORT_TO && !teleportToBlacklisted && GPPermissionHandler.getClaimPermission(event, destination, toClaim, GPPermissions.ENTITY_TELEPORT_TO, type, entity, user, TrustType.ACCESSOR, true) == Tristate.FALSE) {
                boolean cancelled = true;
                if (GPFlags.PORTAL_USE && type.equals(TeleportTypes.PORTAL)) {
                    if (portalUseBlacklisted || GPPermissionHandler.getClaimPermission(event, destination, toClaim, GPPermissions.PORTAL_USE, type, entity, user, TrustType.ACCESSOR, true) == Tristate.TRUE) {
                        cancelled = false;
                    }
                }
                if (cancelled) {
                    final Text message = GriefPreventionPlugin.instance.messageData.permissionPortalEnter
                            .apply(ImmutableMap.of(
                            "owner", toClaim.getOwnerName())).build();
                    if (player != null) {
                        GriefPreventionPlugin.sendMessage(player, message);
                    }
    
                    if (type.equals(EntityTypes.ENDER_PEARL)) {
                        ((EntityPlayer) player).inventory.addItemStackToInventory(new net.minecraft.item.ItemStack(Items.ENDER_PEARL));
                    }
                    event.setCancelled(true);
                    GPTimings.ENTITY_TELEPORT_EVENT.stopTimingIfSync();
                    return;
                }
            }
        }

        if (player != null && !sourceLocation.getExtent().getUniqueId().equals(destination.getExtent().getUniqueId())) {
            // new world, check if player has world storage for it
            GPClaimManager claimWorldManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(destination.getExtent().getProperties());

            try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                // update lastActive timestamps for claims this player owns
                WorldProperties worldProperties = destination.getExtent().getProperties();
                UUID playerUniqueId = player.getUniqueId();
                Sponge.getCauseStackManager().pushCause(GriefPreventionPlugin.instance);
                for (Claim claim : this.dataStore.getClaimWorldManager(worldProperties).getWorldClaims()) {
                    if (claim.getOwnerUniqueId().equals(playerUniqueId)) {
                        // update lastActive timestamp for claim
                        claim.getData().setDateLastActive(Instant.now());
                        claimWorldManager.addClaim(claim);
                    } else if (claim.getParent().isPresent() && claim.getParent().get().getOwnerUniqueId().equals(playerUniqueId)) {
                        // update lastActive timestamp for subdivisions if parent owner logs on
                        claim.getData().setDateLastActive(Instant.now());
                        claimWorldManager.addClaim(claim);
                    }
                }
            }
        }

        if (playerData != null) {
            if (toClaim.isTown()) {
                playerData.inTown = true;
            } else {
                playerData.inTown = false;
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

    // Protects Item Frames
    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onEntityCollideEntity(CollideEntityEvent event) {
        if (!GPFlags.ENTITY_COLLIDE_ENTITY || event instanceof CollideEntityEvent.Impact) {
            return;
        }
        //if (GriefPreventionPlugin.isSourceIdBlacklisted(ClaimFlag.ENTITY_COLLIDE_ENTITY.toString(), event.getSource(), event.getEntities().get(0).getWorld().getProperties())) {
        //    return;
        //}

        Object rootCause = event.getCause().root();
        final boolean isRootEntityItemFrame = rootCause instanceof EntityItemFrame;
        if (!isRootEntityItemFrame) {
            return;
        }

        GPTimings.ENTITY_COLLIDE_EVENT.startTimingIfSync();
        event.filterEntities(new Predicate<Entity>() {
            @Override
            public boolean test(Entity entity) {
                // Avoid living entities breaking itemframes
                if (isRootEntityItemFrame && entity instanceof EntityLiving) {
                    return false;
                }

                return true;
            }
        });
        GPTimings.ENTITY_COLLIDE_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onProjectileImpactEntity(CollideEntityEvent.Impact event) {
        if (!GPFlags.PROJECTILE_IMPACT_ENTITY) {
            return;
        }
        if (GriefPreventionPlugin.isSourceIdBlacklisted(ClaimFlag.PROJECTILE_IMPACT_ENTITY.toString(), event.getSource(), event.getImpactPoint().getExtent().getProperties())) {
            return;
        }

        final User user = CauseContextHelper.getEventUser(event);
        if (user == null || !GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getImpactPoint().getExtent().getProperties())) {
            return;
        }

        GPTimings.PROJECTILE_IMPACT_ENTITY_EVENT.startTimingIfSync();
        Object source = event.getCause().root();
        Location<World> impactPoint = event.getImpactPoint();
        GPClaim targetClaim = null;
        for (Entity entity : event.getEntities()) {
            if (GriefPreventionPlugin.isTargetIdBlacklisted(ClaimFlag.PROJECTILE_IMPACT_ENTITY.toString(), entity, event.getImpactPoint().getExtent().getProperties())) {
                return;
            }
            targetClaim = this.dataStore.getClaimAt(impactPoint, targetClaim);
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
