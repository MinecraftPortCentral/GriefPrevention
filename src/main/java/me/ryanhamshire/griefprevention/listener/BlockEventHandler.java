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

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableMap;
import me.ryanhamshire.griefprevention.DataStore;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GPTimings;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.api.claim.TrustType;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.permission.GPPermissionHandler;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import me.ryanhamshire.griefprevention.util.BlockPosCache;
import me.ryanhamshire.griefprevention.util.BlockUtils;
import me.ryanhamshire.griefprevention.visual.Visualization;
import me.ryanhamshire.griefprevention.visual.VisualizationType;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.CollideBlockEvent;
import org.spongepowered.api.event.block.NotifyNeighborBlockEvent;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.LocatableBlock;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.data.util.NbtDataUtil;
import org.spongepowered.common.interfaces.entity.IMixinEntity;
import org.spongepowered.common.interfaces.world.IMixinLocation;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

//event handlers related to blocks
public class BlockEventHandler {

    // convenience reference to singleton datastore
    private final DataStore dataStore;
    private final UserStorageService userStorageService;

    // constructor
    public BlockEventHandler(DataStore dataStore) {
        this.dataStore = dataStore;
        this.userStorageService = Sponge.getServiceManager().provide(UserStorageService.class).get();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onBlockPre(ChangeBlockEvent.Pre event) {
        GPTimings.BLOCK_PRE_EVENT.startTimingIfSync();
        User user = event.getCause().first(User.class).orElse(null);
        if (user != null) {
            if (event.getCause().containsNamed(NamedCause.PLAYER_BREAK) && !event.getCause().containsNamed(NamedCause.FAKE_PLAYER)) {
                GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                return;
            }
            if (event.getCause().containsNamed(NamedCause.PISTON_RETRACT)) {
                GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                return;
            }
        }

        LocatableBlock locatableBlock = event.getCause().first(LocatableBlock.class).orElse(null);
        TileEntity tileEntity = event.getCause().first(TileEntity.class).orElse(null);
        Object rootCause = event.getCause().root();
        Location<World> sourceLocation = locatableBlock != null ? locatableBlock.getLocation() : tileEntity != null ? tileEntity.getLocation() : null;
        boolean hasFakePlayer = event.getCause().containsNamed("FakePlayer");
        final boolean pistonExtend = event.getCause().containsNamed(NamedCause.PISTON_EXTEND);

        if (sourceLocation != null) {
            if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(sourceLocation.getExtent().getProperties())) {
                GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                return;
            }
    
            GPClaim sourceClaim = this.dataStore.getClaimAt(sourceLocation, false, null);
            GPClaim targetClaim = null;
            for (Location<World> location : event.getLocations()) {
                targetClaim = this.dataStore.getClaimAt(location, false, targetClaim);

                if (user != null && targetClaim.isUserTrusted(user, TrustType.BUILDER)) {
                    GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                    return;
                }
                if (sourceClaim.getOwnerUniqueId().equals(targetClaim.getOwnerUniqueId()) && user == null) {
                    GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                    return;
                }
                if (user != null && pistonExtend) {
                    if (targetClaim.isUserTrusted(user, TrustType.ACCESSOR)) {
                        GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                        return;
                    }
                }
                if (event.getCause().containsNamed(NamedCause.FIRE_SPREAD)) {
                    if (GPPermissionHandler.getClaimPermission(event, location, targetClaim, GPPermissions.FIRE_SPREAD, rootCause, location.getBlock(), user, true) == Tristate.FALSE) {
                        event.setCancelled(true);
                        GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                        return;
                    }
                }

                if (GPPermissionHandler.getClaimPermission(event, location, targetClaim, GPPermissions.BLOCK_BREAK, rootCause, location.getBlock(), user) == Tristate.FALSE) {
                    // PRE events can be spammy so we need to avoid sending player messages here.
                    event.setCancelled(true);
                    GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                    return;
                }
            }
        } else if (user != null) {
            GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getPlayerData(event.getLocations().get(0).getExtent(), user.getUniqueId());
            GPClaim targetClaim = null;
            for (Location<World> location : event.getLocations()) {
                targetClaim = this.dataStore.getClaimAt(location, false, targetClaim);

                if (targetClaim.isUserTrusted(user, TrustType.BUILDER)) {
                    GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                    return;
                }
                if (playerData != null && playerData.checkLastInteraction(targetClaim, user)) {
                    GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                    return;
                }

                if (event.getCause().containsNamed(NamedCause.FIRE_SPREAD)) {
                    if (GPPermissionHandler.getClaimPermission(event, location, targetClaim, GPPermissions.FIRE_SPREAD, rootCause, location.getBlock(), user, true) == Tristate.FALSE) {
                        event.setCancelled(true);
                        GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                        return;
                    }
                }

                boolean userAllowed = false;
                boolean canBreak = GPPermissionHandler.getClaimPermission(event, location, targetClaim, GPPermissions.BLOCK_BREAK, rootCause, location.getBlock(), user) == Tristate.TRUE;
                if (hasFakePlayer) {
                    if (!canBreak) {
                        userAllowed = false;
                    }
                } else if (canBreak) {
                    userAllowed = true;
                }

                if (!userAllowed) {
                    event.setCancelled(true);
                    GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                    return;
                }
            }
        }
        GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
    }

    // Handle fluids flowing into claims
    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onBlockNotify(NotifyNeighborBlockEvent event, @First User user) {
        GPTimings.BLOCK_NOTIFY_EVENT.startTimingIfSync();

        LocatableBlock locatableBlock = event.getCause().first(LocatableBlock.class).orElse(null);
        TileEntity tileEntity = event.getCause().first(TileEntity.class).orElse(null);
        Location<World> sourceLocation = locatableBlock != null ? locatableBlock.getLocation() : tileEntity != null ? tileEntity.getLocation() : null;
        GPClaim sourceClaim = null;
        GPPlayerData playerData = null;
        if (sourceLocation == null) {
            Player player = event.getCause().first(Player.class).orElse(null);
            if (player == null) {
                GPTimings.BLOCK_NOTIFY_EVENT.stopTimingIfSync();
                return;
            }

            sourceLocation = player.getLocation();
            playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
            sourceClaim = this.dataStore.getClaimAtPlayer(playerData, player.getLocation(), false);
        } else {
            playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(sourceLocation.getExtent(), user.getUniqueId());
            sourceClaim = this.dataStore.getClaimAt(sourceLocation, false, playerData.lastClaim.get());
        }

        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(sourceLocation.getExtent().getProperties())) {
            GPTimings.BLOCK_NOTIFY_EVENT.stopTimingIfSync();
            return;
        }

        Iterator<Direction> iterator = event.getNeighbors().keySet().iterator();
        GPClaim targetClaim = null;
        while (iterator.hasNext()) {
            Direction direction = iterator.next();
            Location<World> location = sourceLocation.getBlockRelative(direction);
            Vector3i pos = location.getBlockPosition();
            targetClaim = this.dataStore.getClaimAt(location, false, targetClaim);
            if (sourceClaim.isWilderness() && targetClaim.isWilderness()) {
                if (playerData != null) {
                    playerData.setLastInteractData(targetClaim);
                }
                continue;
            } else if (!sourceClaim.isWilderness() && targetClaim.isWilderness()) {
                if (playerData != null) {
                    playerData.setLastInteractData(targetClaim);
                }

                UUID creator = location.getExtent().getCreator(pos).orElse(null);
                if (creator == null) {
                    // check notifier
                    creator = location.getExtent().getNotifier(pos).orElse(null);
                }

                if (creator != null) {
                    User creatorUser = this.userStorageService.get(creator).orElse(null);
                    if (!sourceClaim.isUserTrusted(creatorUser, TrustType.ACCESSOR)) {
                        iterator.remove();
                    }
                }
                continue;
            } else if (sourceClaim.id.equals(targetClaim.id)) {
                if (playerData != null) {
                    playerData.setLastInteractData(targetClaim);
                }
                continue;
            } else {
                if (playerData.checkLastInteraction(targetClaim, user)) {
                    continue;
                }
                // Needed to handle levers notifying doors to open etc.
                if (targetClaim.isUserTrusted(user, TrustType.ACCESSOR)) {
                    if (playerData != null) {
                        playerData.setLastInteractData(targetClaim);
                    }
                    continue;
                }
            }

            // no claim crossing unless trusted
            iterator.remove();
        }
        GPTimings.BLOCK_NOTIFY_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onBlockCollide(CollideBlockEvent event, @Root Entity source, @First User user) {
        // ignore falling blocks
        if (source instanceof EntityFallingBlock) {
            return;
        }
        GPTimings.BLOCK_COLLIDE_EVENT.startTimingIfSync();
        final BlockType blockType = event.getTargetBlock().getType();
        if (event.getTargetSide().equals(Direction.UP) || blockType.equals(BlockTypes.AIR) 
                || !GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getTargetLocation().getExtent().getProperties())) {
            GPTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
            return;
        }

        if (source instanceof EntityItem && (blockType != BlockTypes.PORTAL && !(blockType instanceof BlockBasePressurePlate))) {
            GPTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
            return;
        }

        BlockPos collidePos = ((IMixinLocation)(Object) event.getTargetLocation()).getBlockPos();
        short shortPos = BlockUtils.blockPosToShort(collidePos);
        int entityId = ((net.minecraft.entity.Entity) source).getEntityId();
        BlockPosCache entityBlockCache = BlockUtils.ENTITY_BLOCK_CACHE.get(entityId);
        if (entityBlockCache == null) {
            entityBlockCache = new BlockPosCache(shortPos);
            BlockUtils.ENTITY_BLOCK_CACHE.put(entityId, entityBlockCache);
        } else {
            Tristate result = entityBlockCache.getCacheResult(shortPos);
            if (result != Tristate.UNDEFINED) {
                if (result == Tristate.FALSE) {
                    event.setCancelled(true);
                }

                GPTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
                return;
            }
        }

        GPPlayerData playerData = null;
        GPClaim targetClaim = null;
        if (user instanceof Player) {
            playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(event.getTargetLocation().getExtent(), user.getUniqueId());
            targetClaim = this.dataStore.getClaimAtPlayer(playerData, event.getTargetLocation(), false);
        } else {
            targetClaim = this.dataStore.getClaimAt(event.getTargetLocation(), false, null);
        }

        Tristate result = GPPermissionHandler.getFlagOverride(event, event.getTargetLocation(), targetClaim, GPPermissions.ENTITY_COLLIDE_BLOCK, source, event.getTargetBlock(), true);
        if (result != Tristate.UNDEFINED) {
            if (result == Tristate.TRUE) {
                if (playerData != null) {
                    playerData.setLastInteractData(targetClaim);
                }
                entityBlockCache.setLastResult(Tristate.TRUE);
                GPTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
                return;
            }

            entityBlockCache.setLastResult(Tristate.FALSE);
            event.setCancelled(true);
            GPTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
            return;
        }

        if (!targetClaim.isUserTrusted(user, TrustType.ACCESSOR)) {
            if (GPPermissionHandler.getClaimPermission(event, event.getTargetLocation(), targetClaim, GPPermissions.ENTITY_COLLIDE_BLOCK, source, event.getTargetBlock(), user) == Tristate.TRUE) {
                if (playerData != null) {
                    playerData.setLastInteractData(targetClaim);
                }
                entityBlockCache.setLastResult(Tristate.TRUE);
                GPTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
                return;
            }
            if (event.getTargetBlock().getType() == BlockTypes.PORTAL) {
                if (GPPermissionHandler.getClaimPermission(event, event.getTargetLocation(), targetClaim, GPPermissions.PORTAL_USE, source, event.getTargetBlock(), user) == Tristate.TRUE) {
                    GPTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
                    return;
                } else if (event.getCause().root() instanceof Player){
                    if (event.getTargetLocation().getExtent().getProperties().getTotalTime() % 20 == 0L) { // log once a second to avoid spam
                        // Disable message temporarily
                        //GriefPrevention.sendMessage((Player) user, TextMode.Err, Messages.NoPortalFromProtectedClaim, claim.getOwnerName());
                        /*final Text message = GriefPreventionPlugin.instance.messageData.permissionProtectedPortal
                                .apply(ImmutableMap.of(
                                "owner", targetClaim.getOwnerName())).build();*/
                        event.setCancelled(true);
                        entityBlockCache.setLastResult(Tristate.FALSE);
                        GPTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
                        return;
                    }
                }
            }

            event.setCancelled(true);
            entityBlockCache.setLastResult(Tristate.FALSE);
            GPTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
            return;
        }

        if (playerData != null) {
            playerData.setLastInteractData(targetClaim);
        }
        entityBlockCache.setLastResult(Tristate.TRUE);
        GPTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onProjectileImpactBlock(CollideBlockEvent.Impact event, @First User user) {
        GPTimings.PROJECTILE_IMPACT_BLOCK_EVENT.startTimingIfSync();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getImpactPoint().getExtent().getProperties())) {
            GPTimings.PROJECTILE_IMPACT_BLOCK_EVENT.stopTimingIfSync();
            return;
        }

        Object source = event.getCause().root();
        Location<World> impactPoint = event.getImpactPoint();
        GPClaim targetClaim = null;
        GPPlayerData playerData = null;
        if (user instanceof Player) {
            playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(event.getTargetLocation().getExtent(), user.getUniqueId());
            targetClaim = this.dataStore.getClaimAtPlayer(playerData, impactPoint, false);
        } else {
            targetClaim = this.dataStore.getClaimAt(impactPoint, false, null);
        }

        Tristate result = GPPermissionHandler.getClaimPermission(event, impactPoint, targetClaim, GPPermissions.PROJECTILE_IMPACT_BLOCK, source, event.getTargetBlock(), user, TrustType.ACCESSOR, true);
        if (result == Tristate.FALSE) {
            event.setCancelled(true);
            GPTimings.PROJECTILE_IMPACT_BLOCK_EVENT.stopTimingIfSync();
            return;
        }

        GPTimings.PROJECTILE_IMPACT_BLOCK_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onExplosion(ExplosionEvent.Post event) {
        GPTimings.EXPLOSION_EVENT.startTimingIfSync();
        final World world = event.getExplosion().getWorld();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(world.getProperties())) {
            GPTimings.EXPLOSION_EVENT.stopTimingIfSync();
            return;
        }

        Object source = event.getCause().root();
        User creator = null;
        if (source instanceof Entity) {
            Entity entity = (Entity) source;
            creator = ((IMixinEntity) entity).getTrackedPlayer(NbtDataUtil.SPONGE_ENTITY_CREATOR).orElse(null);
        }

        GPClaim targetClaim = null;
        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
            BlockSnapshot blockSnapshot = transaction.getOriginal();
            Location<World> location = blockSnapshot.getLocation().orElse(null);
            if (location == null) {
                continue;
            }

            targetClaim =  GriefPreventionPlugin.instance.dataStore.getClaimAt(blockSnapshot.getLocation().get(), false, targetClaim);
            if (location.getPosition().getY() > ((net.minecraft.world.World) world).getSeaLevel() && GPPermissionHandler.getClaimPermission(event, location, targetClaim, GPPermissions.EXPLOSION_SURFACE, source, blockSnapshot, creator, true) == Tristate.FALSE) {
                event.setCancelled(true);
                GPTimings.EXPLOSION_EVENT.stopTimingIfSync();
                return;
            }

            if (GPPermissionHandler.getClaimPermission(event, location, targetClaim, GPPermissions.EXPLOSION, source, blockSnapshot, creator, true) == Tristate.FALSE) {
                // Avoid lagging server from large explosions.
                if (event.getTransactions().size() > 100) {
                    event.setCancelled(true);
                    GPTimings.EXPLOSION_EVENT.stopTimingIfSync();
                    return;
                }
                transaction.setValid(false);
                GPTimings.EXPLOSION_EVENT.stopTimingIfSync();
                return;
            }
        }
        GPTimings.EXPLOSION_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onBlockBreak(ChangeBlockEvent.Break event) {
        GPTimings.BLOCK_BREAK_EVENT.startTimingIfSync();
        if (event instanceof ExplosionEvent) {
            GPTimings.BLOCK_BREAK_EVENT.stopTimingIfSync();
            return;
        }

        final World world = event.getTransactions().get(0).getFinal().getLocation().get().getExtent();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(world.getProperties())) {
            GPTimings.BLOCK_BREAK_EVENT.stopTimingIfSync();
            return;
        }

        Object source = event.getCause().root();
        User user = event.getCause().first(User.class).orElse(null);
        GPClaim sourceClaim = null;
        LocatableBlock locatable = null;
        if (source instanceof LocatableBlock) {
            locatable = (LocatableBlock) source;
            if (user != null && user instanceof Player) {
                final GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(locatable.getWorld(), user.getUniqueId());
                sourceClaim = this.dataStore.getClaimAt(locatable.getLocation(), false, playerData.lastClaim.get());
            } else {
                sourceClaim = this.dataStore.getClaimAt(locatable.getLocation(), false, null);
            }
        } else {
            sourceClaim = this.getSourceClaim(event.getCause());
        }
        if (sourceClaim == null) {
            GPTimings.BLOCK_BREAK_EVENT.stopTimingIfSync();
            return;
        }

        List<Transaction<BlockSnapshot>> transactions = event.getTransactions();
        GPClaim targetClaim = null;
        for (Transaction<BlockSnapshot> transaction : transactions) {
            Location<World> location = transaction.getOriginal().getLocation().orElse(null);
            targetClaim = this.dataStore.getClaimAt(location, false, targetClaim);
            if (locatable != null && targetClaim.isWilderness()) {
                continue;
            }

            // check overrides
            Tristate value = GPPermissionHandler.getClaimPermission(event, location, targetClaim, GPPermissions.BLOCK_BREAK, source, transaction.getOriginal(), user, TrustType.BUILDER, true);
            if (value == Tristate.FALSE) {
                if (source instanceof Player) {
                    final Text message = GriefPreventionPlugin.instance.messageData.permissionBuild
                            .apply(ImmutableMap.of(
                            "player", Text.of(targetClaim.getOwnerName())
                    )).build();
                    GriefPreventionPlugin.sendClaimDenyMessage(targetClaim, (Player) event.getCause().root(), message);
                }

                event.setCancelled(true);
                GPTimings.BLOCK_BREAK_EVENT.stopTimingIfSync();
                return;
            }
        }
        GPTimings.BLOCK_BREAK_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onBlockPlace(ChangeBlockEvent.Place event) {
        GPTimings.BLOCK_PLACE_EVENT.startTimingIfSync();
        final World world = event.getTransactions().get(0).getFinal().getLocation().get().getExtent();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(world.getProperties())) {
            GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
            return;
        }

        Object source = event.getCause().root();
        GPClaim sourceClaim = null;
        LocatableBlock locatable = null;
        User user = event.getCause().first(User.class).orElse(null);
        if (source instanceof LocatableBlock) {
            locatable = (LocatableBlock) source;
            if (user != null && user instanceof Player) {
                final GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(locatable.getWorld(), user.getUniqueId());
                sourceClaim = this.dataStore.getClaimAt(locatable.getLocation(), false, playerData.lastClaim.get());
            } else {
                sourceClaim = this.dataStore.getClaimAt(locatable.getLocation(), false, null);
            }
        } else {
            sourceClaim = this.getSourceClaim(event.getCause());
        }
        if (sourceClaim == null) {
            GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
            return;
        }

        Player player = user != null && user instanceof Player ? (Player) user : null;
        GPPlayerData playerData = null;
        if (user != null) {
            playerData = this.dataStore.getOrCreatePlayerData(world, user.getUniqueId());
        }

        GriefPreventionConfig<?> activeConfig = GriefPreventionPlugin.getActiveConfig(world.getProperties());
        if (sourceClaim != null && !(source instanceof User) && playerData != null && playerData.checkLastInteraction(sourceClaim, user)) {
            GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
            return;
        }

        GPClaim targetClaim = null;
        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
            BlockSnapshot block = transaction.getFinal();
            Location<World> location = block.getLocation().orElse(null);
            if (location == null) {
                continue;
            }

            targetClaim = this.dataStore.getClaimAt(location, false, targetClaim);
            if (locatable != null && targetClaim.isWilderness()) {
                continue;
            }

            // check overrides
            Tristate result = GPPermissionHandler.getClaimPermission(event, location, targetClaim, GPPermissions.BLOCK_PLACE, source, block, user, TrustType.BUILDER, true);
            if (result != Tristate.UNDEFINED) {
                if (result == Tristate.TRUE) {
                    GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                    return;
                }

                event.setCancelled(true);
                GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                return;
            }

            // warn players when they place TNT above sea level, since it doesn't destroy blocks there
            if (player != null && GPPermissionHandler.getClaimPermission(event, location, targetClaim, GPPermissions.EXPLOSION_SURFACE, event.getCause().root(), block.getState(), user) == Tristate.FALSE && block.getState().getType() == BlockTypes.TNT &&
                    !block.getLocation().get().getExtent().getDimension().getType().equals(DimensionTypes.NETHER) &&
                    block.getPosition().getY() > GriefPreventionPlugin.instance.getSeaLevel(block.getLocation().get().getExtent()) - 5 &&
                    targetClaim.isWilderness()) {
                GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.warningTntAboveSeaLevel.toText());
            }

            // warn players about disabled pistons outside of land claims
            if (player != null && !playerData.canIgnoreClaim(targetClaim) && activeConfig.getConfig().general.limitPistonsToClaims &&
                    (block.getState().getType() == BlockTypes.PISTON || block.getState().getType() == BlockTypes.STICKY_PISTON) &&
                    targetClaim.isWilderness()) {
                GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.warningPistonsOutsideClaims.toText());
            }

            // Don't run logic below if a player didn't directly cause this event. Prevents issues such as claims getting autocreated while
            // a player is indirectly placing blocks.
            if (!(source instanceof Player)) {
                continue;
            }

            if (targetClaim.isWilderness() && activeConfig.getConfig().claim.claimRadius > -1) {
                net.minecraft.tileentity.TileEntity tileEntity = (net.minecraft.tileentity.TileEntity) block.getLocation().get().getTileEntity().orElse(null);
                if (tileEntity == null || !(tileEntity instanceof IInventory) || !(tileEntity instanceof TileEntityChest)) {
                    GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                    return;
                }

                // FEATURE: automatically create a claim when a player who has no claims
                // places a chest otherwise if there's no claim, the player is placing a chest, and new player automatic claims are enabled
                // if the chest is too deep underground, don't create the claim and explain why
                if (block.getPosition().getY() < activeConfig.getConfig().claim.maxClaimDepth) {
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimChestTooDeep.toText());
                    GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                    return;
                }

                int radius = activeConfig.getConfig().claim.claimRadius;

                // if the player doesn't have any claims yet, automatically create a claim centered at the chest
                if (playerData.getInternalClaims().size() == 0) {
                    // radius == 0 means protect ONLY the chest
                    if (activeConfig.getConfig().claim.claimRadius == 0) {
                        this.dataStore.createClaim(
                                block.getLocation().get().getExtent(), 
                                block.getPosition(), block.getPosition(), ClaimType.BASIC, player.getUniqueId(), false, Cause.of(NamedCause.source(player)));
                        GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimChestConfirmation.toText());
                    }

                    // otherwise, create a claim in the area around the chest
                    else {
                        Vector3i lesserBoundary = new Vector3i(
                                block.getPosition().getX() - radius, 
                                0,
                                block.getPosition().getZ() - radius);
                        Vector3i greaterBoundary = new Vector3i(
                                block.getPosition().getX() + radius,
                                player.getWorld().getDimension().getBuildHeight() - 1,
                                block.getPosition().getZ() + radius);
                        // as long as the automatic claim overlaps another existing
                        // claim, shrink it note that since the player had permission to place the
                        // chest, at the very least, the automatic claim will include the chest
                        while (radius >= 0 && !this.dataStore.createClaim(block.getLocation().get().getExtent(),
                                lesserBoundary,
                                greaterBoundary,
                                ClaimType.BASIC,
                                player.getUniqueId(), false, Cause.of(NamedCause.source(player))).successful()) {
                            radius--;
                        }

                        // notify and explain to player
                        GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimAutomaticNotification.toText());

                        // show the player the protected area
                        GPClaim newClaim = this.dataStore.getClaimAt(block.getLocation().get(), false, null);
                        Visualization visualization = new Visualization(newClaim, VisualizationType.CLAIM);
                        visualization.createClaimBlockVisuals(block.getPosition().getY(), player.getLocation(), playerData);
                        visualization.apply(player);
                    }

                    if (player.hasPermission(GPPermissions.CLAIM_SHOW_TUTORIAL)) {
                        GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.urlSurvivalBasics.toText());
                    }
                }

                // check to see if this chest is in a claim, and warn when it isn't
                if (targetClaim.isWilderness() && player.hasPermission(GPPermissions.CLAIM_SHOW_TUTORIAL)) {
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.warningChestUnprotected.toText());
                }
            }
        }

        GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST, beforeModifications = true)
    public void onSignChanged(ChangeSignEvent event, @First User user) {
        GPTimings.SIGN_CHANGE_EVENT.startTimingIfSync();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getTargetTile().getLocation().getExtent().getProperties())) {
            GPTimings.SIGN_CHANGE_EVENT.stopTimingIfSync();
            return;
        }

        Location<World> location = event.getTargetTile().getLocation();
        // Prevent users exploiting signs
        GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAt(location, false, null);
        if (GPPermissionHandler.getClaimPermission(event, location, claim, GPPermissions.INTERACT_BLOCK_SECONDARY, user, location.getBlock(), user, TrustType.ACCESSOR, true) == Tristate.FALSE) {
            if (user instanceof Player) {
                event.setCancelled(true);
                final Text message = GriefPreventionPlugin.instance.messageData.permissionAccess
                        .apply(ImmutableMap.of(
                        "player", Text.of(claim.getOwnerName()))).build();
                GriefPreventionPlugin.sendClaimDenyMessage(claim, (Player) user, message);
                return;
            }
        }

        // send sign content to online administrators
        if (!GriefPreventionPlugin.getActiveConfig(location.getExtent().getProperties())
                .getConfig().general.generalAdminSignNotifications) {
            GPTimings.SIGN_CHANGE_EVENT.stopTimingIfSync();
            return;
        }

        World world = location.getExtent();
        StringBuilder lines = new StringBuilder(" placed a sign @ " + GriefPreventionPlugin.getfriendlyLocationString(event.getTargetTile().getLocation()));
        boolean notEmpty = false;
        for (int i = 0; i < event.getText().lines().size(); i++) {
            String withoutSpaces = Text.of(event.getText().lines().get(i)).toPlain().replace(" ", "");
            if (!withoutSpaces.isEmpty()) {
                notEmpty = true;
                lines.append("\n  " + event.getText().lines().get(i));
            }
        }

        String signMessage = lines.toString();
        // prevent signs with blocked IP addresses
        if (!user.hasPermission(GPPermissions.SPAM) && GriefPreventionPlugin.instance.containsBlockedIP(signMessage)) {
            event.setCancelled(true);
            GPTimings.SIGN_CHANGE_EVENT.stopTimingIfSync();
            return;
        }

        // if not empty and wasn't the same as the last sign, log it and remember it for later
        GPPlayerData playerData = this.dataStore.getOrCreatePlayerData(world, user.getUniqueId());
        if (notEmpty && playerData.lastMessage != null && !playerData.lastMessage.equals(signMessage)) {
            GriefPreventionPlugin.addLogEntry(user.getName() + lines.toString().replace("\n  ", ";"), null);
            //PlayerEventHandler.makeSocialLogEntry(player.get().getName(), signMessage);
            playerData.lastMessage = signMessage;

            if (!user.hasPermission(GPPermissions.EAVES_DROP_SIGNS)) {
                Collection<Player> players = (Collection<Player>) Sponge.getGame().getServer().getOnlinePlayers();
                for (Player otherPlayer : players) {
                    if (otherPlayer.hasPermission(GPPermissions.EAVES_DROP_SIGNS)) {
                        otherPlayer.sendMessage(Text.of(TextColors.GRAY, user.getName(), signMessage));
                    }
                }
            }
        }
        GPTimings.SIGN_CHANGE_EVENT.stopTimingIfSync();
    }

    public GPClaim getSourceClaim(Cause cause) {
        BlockSnapshot blockSource = cause.first(BlockSnapshot.class).orElse(null);
        LocatableBlock locatableBlock = null;
        Entity entitySource = null;
        if (blockSource == null) {
            locatableBlock = cause.first(LocatableBlock.class).orElse(null);
            if (locatableBlock == null) {
                entitySource = cause.first(Entity.class).orElse(null);
            }
        }

        GPClaim sourceClaim = null;
        if (blockSource != null) {
            sourceClaim = this.dataStore.getClaimAt(blockSource.getLocation().get(), false, null);
        } else if (entitySource != null) {
            Entity entity = entitySource;
            if (entity instanceof Player) {
                Player player = (Player) entity;
                GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
                sourceClaim = this.dataStore.getClaimAtPlayer(playerData, player.getLocation(), false);
            } else {
                sourceClaim = this.dataStore.getClaimAt(entity.getLocation(), false, null);
            }
        } else if (locatableBlock != null) {
            sourceClaim = this.dataStore.getClaimAt(locatableBlock.getLocation(), false, null);
        }

        return sourceClaim;
    }
}
