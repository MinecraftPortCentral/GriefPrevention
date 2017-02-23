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
import me.ryanhamshire.griefprevention.DataStore;
import me.ryanhamshire.griefprevention.BlockPosCache;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GPTimings;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.message.Messages;
import me.ryanhamshire.griefprevention.message.TextMode;
import me.ryanhamshire.griefprevention.permission.GPPermissionHandler;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
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
import org.spongepowered.api.event.cause.entity.teleport.PortalTeleportCause;
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

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
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

    @Listener(order = Order.FIRST)
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
        boolean rootPlayer = rootCause instanceof User;
        boolean hasFakePlayer = event.getCause().containsNamed("FakePlayer");

        if (sourceLocation != null) {
            if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(sourceLocation.getExtent().getProperties())) {
                GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                return;
            }
    
            GPClaim sourceClaim = this.dataStore.getClaimAt(sourceLocation, true, null);
            for (Location<World> location : event.getLocations()) {
                GPClaim targetClaim = this.dataStore.getClaimAt(location, true, null);

                if (user != null && targetClaim.hasFullTrust(user)) {
                    GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                    return;
                } else if (sourceClaim.getOwnerUniqueId().equals(targetClaim.getOwnerUniqueId()) && user == null) {
                    GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                    return;
                }

                if (event.getCause().containsNamed(NamedCause.FIRE_SPREAD)) {
                    if (GPPermissionHandler.getClaimPermission(targetClaim, GPPermissions.FIRE_SPREAD, rootCause, location.getBlock(), user, true) == Tristate.FALSE) {
                        event.setCancelled(true);
                        GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                        return;
                    }
                }

                String denyReason= GriefPreventionPlugin.instance.allowBuild(rootCause, location, user);
                boolean canBreak = true;
                if (denyReason == null) {
                    canBreak = GPPermissionHandler.getClaimPermission(targetClaim, GPPermissions.BLOCK_BREAK, rootCause, location.getBlock(), user) == Tristate.TRUE;
                }
                if (denyReason != null || !canBreak) {
                    GriefPreventionPlugin.addEventLogEntry(event, targetClaim, location, GPPermissions.BLOCK_BREAK, rootCause, location.getBlock(), user, denyReason);
                    // PRE events can be spammy so we need to avoid sending player messages here.
                    event.setCancelled(true);
                    GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                    return;
                }
            }
        } else if (user != null) {
            GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getPlayerData(event.getTargetWorld(), user.getUniqueId());
            for (Location<World> location : event.getLocations()) {
                GPClaim targetClaim = this.dataStore.getClaimAt(location, true, null);

                if (targetClaim.hasFullTrust(user)) {
                    GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                    return;
                }

                if (playerData != null && playerData.checkLastInteraction(targetClaim, user)) {
                    GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                    return;
                }

                if (event.getCause().containsNamed(NamedCause.FIRE_SPREAD)) {
                    if (GPPermissionHandler.getClaimPermission(targetClaim, GPPermissions.FIRE_SPREAD, rootCause, location.getBlock(), user, true) == Tristate.FALSE) {
                        event.setCancelled(true);
                        GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                        return;
                    }
                }

                String denyReason= GriefPreventionPlugin.instance.allowBuild(rootCause, location, user);
                boolean userAllowed = denyReason == null;
                boolean canBreak = GPPermissionHandler.getClaimPermission(targetClaim, GPPermissions.BLOCK_BREAK, rootCause, location.getBlock(), user) == Tristate.TRUE;
                if (hasFakePlayer) {
                    if (!canBreak) {
                        userAllowed = false;
                    }
                } else if (canBreak) {
                    userAllowed = true;
                }

                if (!userAllowed) {
                    if (!canBreak) {
                        GriefPreventionPlugin.addEventLogEntry(event, targetClaim, location, GPPermissions.BLOCK_BREAK, rootCause, location.getBlock(), user, denyReason);
                    } else {
                    GriefPreventionPlugin.addEventLogEntry(event, targetClaim, location, user, denyReason);
                    }
                    if (!hasFakePlayer && denyReason != null && rootPlayer) {
                        GriefPreventionPlugin.sendClaimDenyMessage(targetClaim, (Player) rootCause, Text.of(TextMode.Err, denyReason));
                    }
                    event.setCancelled(true);
                    GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                    return;
                }
            }
        }
        GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
    }

    // Handle fluids flowing into claims
    @Listener(order = Order.FIRST)
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
            sourceClaim = this.dataStore.getClaimAt(sourceLocation, false, null);
        }

        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(sourceLocation.getExtent().getProperties())) {
            GPTimings.BLOCK_NOTIFY_EVENT.stopTimingIfSync();
            return;
        }

        Iterator<Direction> iterator = event.getNeighbors().keySet().iterator();
        while (iterator.hasNext()) {
            Direction direction = iterator.next();
            Location<World> location = sourceLocation.getBlockRelative(direction);
            Vector3i pos = location.getBlockPosition();
            GPClaim targetClaim = this.dataStore.getClaimAt(location, false, null);
            if (sourceClaim.isWildernessClaim() && targetClaim.isWildernessClaim()) {
                if (playerData != null) {
                    playerData.setLastInteractData(targetClaim);
                }
                continue;
            } else if (!sourceClaim.isWildernessClaim() && targetClaim.isWildernessClaim()) {
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
                    if (sourceClaim.allowAccess(creatorUser, location) != null) {
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
                String denyReason = targetClaim.allowAccess(user, location);
                if (denyReason == null) {
                    if (playerData != null) {
                        playerData.setLastInteractData(targetClaim);
                    }
                    continue;
                }
            }

            // no claim crossing unless trusted
            GriefPreventionPlugin.addEventLogEntry(event, targetClaim, location, user, "Removed direction.");
            iterator.remove();
        }
        GPTimings.BLOCK_NOTIFY_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST)
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

        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(event.getTargetLocation().getExtent(), user.getUniqueId());
        GPClaim targetClaim = null;
        if (user instanceof Player) {
            targetClaim = this.dataStore.getClaimAtPlayer(playerData, event.getTargetLocation(), false);
        } else {
            targetClaim = this.dataStore.getClaimAt(event.getTargetLocation(), false, null);
        }

        // check overrides
        Tristate override = GPPermissionHandler.getFlagOverride(targetClaim, GPPermissions.ENTITY_COLLIDE_BLOCK, source, event.getTargetBlock());
        if (override != Tristate.UNDEFINED) {
            if (override == Tristate.TRUE) {
                if (playerData != null) {
                    playerData.setLastInteractData(targetClaim);
                }
                entityBlockCache.setLastResult(Tristate.TRUE);
                GPTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
                return;
            }

            entityBlockCache.setLastResult(Tristate.FALSE);
            event.setCancelled(true);
            // log once a second to avoid spam
            if (GriefPreventionPlugin.debugLogging && event.getTargetLocation().getExtent().getProperties().getTotalTime() % 100 == 0L) {
                GriefPreventionPlugin.addEventLogEntry(event, targetClaim, event.getTargetLocation(), source, event.getTargetBlock(), user, "");
            }
            GPTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
            return;
        }

        if (user instanceof Player) {
            Player player = (Player) user;
            if (targetClaim.doorsOpen && GriefPreventionPlugin.getActiveConfig(player.getWorld().getProperties()).getConfig().siege.winnerAccessibleBlocks
                    .contains(blockType.getId())) {
                GPTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
                if (playerData != null) {
                    playerData.setLastInteractData(targetClaim);
                }
                entityBlockCache.setLastResult(Tristate.TRUE);
                return; // allow siege mode
            }
        }

        String denyReason = targetClaim.allowAccess(user, event.getTargetLocation());
        //DataStore.generateMessages = true;
        if (denyReason != null) {
            if (GPPermissionHandler.getClaimPermission(targetClaim, GPPermissions.ENTITY_COLLIDE_BLOCK, source, event.getTargetBlock(), user) == Tristate.TRUE) {
                if (playerData != null) {
                    playerData.setLastInteractData(targetClaim);
                }
                entityBlockCache.setLastResult(Tristate.TRUE);
                GPTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
                return;
            }
            if (event.getTargetBlock().getType() == BlockTypes.PORTAL) {
                if (GPPermissionHandler.getClaimPermission(targetClaim, GPPermissions.PORTAL_USE, source, event.getTargetBlock(), user) == Tristate.TRUE) {
                    GPTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
                    return;
                } else if (event.getCause().root() instanceof Player){
                    if (event.getTargetLocation().getExtent().getProperties().getTotalTime() % 20 == 0L) { // log once a second to avoid spam
                        // Disable message temporarily
                        //GriefPrevention.sendMessage((Player) user, TextMode.Err, Messages.NoPortalFromProtectedClaim, claim.getOwnerName());
                        GriefPreventionPlugin.addEventLogEntry(event, targetClaim, event.getTargetLocation(), GPPermissions.PORTAL_USE, source, event.getTargetBlock(), user, GriefPreventionPlugin.getMessage(Messages.NoPortalFromProtectedClaim, user.getName()).toPlain());
                        event.setCancelled(true);
                        entityBlockCache.setLastResult(Tristate.FALSE);
                        GPTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
                        return;
                    }
                }
            }

            if (GriefPreventionPlugin.debugLogging && event.getTargetLocation().getExtent().getProperties().getTotalTime() % 100 == 0L) { // log once a second to avoid spam
               GriefPreventionPlugin.addEventLogEntry(event, targetClaim, event.getTargetLocation(), source, event.getTargetBlock(), user, denyReason);
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

    @Listener(order = Order.FIRST)
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
            playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(impactPoint.getExtent(), user.getUniqueId());
            targetClaim = this.dataStore.getClaimAtPlayer(playerData, impactPoint, false);
        } else {
            targetClaim = this.dataStore.getClaimAt(impactPoint, false, null);
        }

        // check overrides
        Tristate override = GPPermissionHandler.getFlagOverride(targetClaim, GPPermissions.PROJECTILE_IMPACT_BLOCK, source, event.getTargetBlock());
        if (override != Tristate.UNDEFINED) {
            if (override == Tristate.TRUE) {
                GPTimings.PROJECTILE_IMPACT_BLOCK_EVENT.stopTimingIfSync();
                return;
            }

            event.setCancelled(true);
            GriefPreventionPlugin.addEventLogEntry(event, targetClaim, impactPoint, source, event.getTargetBlock(), user, null);
            GPTimings.PROJECTILE_IMPACT_BLOCK_EVENT.stopTimingIfSync();
            return;
        }

        String denyReason = targetClaim.allowAccess(user, impactPoint);
        Tristate result = GPPermissionHandler.getClaimPermission(targetClaim, GPPermissions.PROJECTILE_IMPACT_BLOCK, source, event.getTargetBlock(), user);
        if (result == Tristate.TRUE) {
            GPTimings.PROJECTILE_IMPACT_BLOCK_EVENT.stopTimingIfSync();
            return;
        }
        if (denyReason != null || result == Tristate.FALSE) {
            GriefPreventionPlugin.addEventLogEntry(event, targetClaim, impactPoint, source, event.getTargetBlock(), user, denyReason);
            event.setCancelled(true);
        }
        GPTimings.PROJECTILE_IMPACT_BLOCK_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST)
    public void onExplosion(ExplosionEvent.Post event) {
        GPTimings.EXPLOSION_EVENT.startTimingIfSync();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
            GPTimings.EXPLOSION_EVENT.stopTimingIfSync();
            return;
        }

        Object source = event.getCause().root();
        User creator = null;
        if (source instanceof Entity) {
            Entity entity = (Entity) source;
            creator = ((IMixinEntity) entity).getTrackedPlayer(NbtDataUtil.SPONGE_ENTITY_CREATOR).orElse(null);
        }

        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
            BlockSnapshot blockSnapshot = transaction.getOriginal();
            Location<World> location = blockSnapshot.getLocation().orElse(null);
            if (location == null) {
                continue;
            }

            GPClaim claim =  GriefPreventionPlugin.instance.dataStore.getClaimAt(blockSnapshot.getLocation().get(), false, null);
            if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.EXPLOSION_SURFACE, source, blockSnapshot.getLocation(), creator, true) == Tristate.FALSE && location.getPosition().getY() > ((net.minecraft.world.World) event.getTargetWorld()).getSeaLevel()) {
                event.setCancelled(true);
                GriefPreventionPlugin.addEventLogEntry(event, claim, location, source, blockSnapshot.getLocation(), creator, null);
                GPTimings.EXPLOSION_EVENT.stopTimingIfSync();
                return;
            }

            String denyReason = claim.allowBreak(source, blockSnapshot, creator);
            if (denyReason != null) {
                // Avoid lagging server from large explosions.
                if (event.getTransactions().size() > 100) {
                    GriefPreventionPlugin.addEventLogEntry(event, claim, location, source, blockSnapshot, creator, denyReason);
                    event.setCancelled(true);
                    GPTimings.EXPLOSION_EVENT.stopTimingIfSync();
                    return;
                }
                transaction.setValid(false);
            }
        }
        GPTimings.EXPLOSION_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST)
    public void onBlockBreak(ChangeBlockEvent.Break event) {
        GPTimings.BLOCK_BREAK_EVENT.startTimingIfSync();
        if (event instanceof ExplosionEvent) {
            GPTimings.BLOCK_BREAK_EVENT.stopTimingIfSync();
            return;
        }

        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
            GPTimings.BLOCK_BREAK_EVENT.stopTimingIfSync();
            return;
        }

        Object source = event.getCause().root();
        User user = event.getCause().first(User.class).orElse(null);
        LocatableBlock locatable = null;
        if (source instanceof LocatableBlock) {
            locatable = (LocatableBlock) source;
        }
        GPClaim sourceClaim = this.getSourceClaim(event.getCause());
        if (sourceClaim == null) {
            GPTimings.BLOCK_BREAK_EVENT.stopTimingIfSync();
            return;
        }

        List<Transaction<BlockSnapshot>> transactions = event.getTransactions();
        for (Transaction<BlockSnapshot> transaction : transactions) {
            Location<World> location = transaction.getOriginal().getLocation().orElse(null);
            GPClaim targetClaim = this.dataStore.getClaimAt(location, false, null);
            if (locatable != null /*&& !((IMixinBlock) locatable.getBlockState().getType()).requiresBlockCapture()*/ && targetClaim.isWildernessClaim()) {
                continue;
            }

            // check overrides
            Tristate override = GPPermissionHandler.getFlagOverride(targetClaim, GPPermissions.BLOCK_BREAK, source, transaction.getOriginal());
            if (override != Tristate.UNDEFINED) {
                if (override == Tristate.TRUE) {
                    GPTimings.BLOCK_BREAK_EVENT.stopTimingIfSync();
                    return;
                }

                event.setCancelled(true);
                GriefPreventionPlugin.addEventLogEntry(event, targetClaim, location, source, transaction.getOriginal(), user, null);
                GPTimings.BLOCK_BREAK_EVENT.stopTimingIfSync();
                return;
            }

            if (user != null && targetClaim.hasFullTrust(user)) {
                GPTimings.BLOCK_BREAK_EVENT.stopTimingIfSync();
                return;
            } else if (user == null && sourceClaim.getOwnerUniqueId().equals(targetClaim.getOwnerUniqueId())) {
                GPTimings.BLOCK_BREAK_EVENT.stopTimingIfSync();
                return;
            }

            // Pass original snapshot as it represents the block that was broken
            String denyReason = GriefPreventionPlugin.instance.allowBreak(source, transaction.getOriginal(), user);
            if (denyReason != null) {
                if (event.getCause().root() instanceof Player) {
                    GriefPreventionPlugin.sendClaimDenyMessage(targetClaim, (Player) event.getCause().root(), Text.of(TextMode.Err, denyReason));
                }

                GriefPreventionPlugin.addEventLogEntry(event, targetClaim, location, source, transaction.getOriginal(), user, denyReason);
                event.setCancelled(true);
                GPTimings.BLOCK_BREAK_EVENT.stopTimingIfSync();
                return;
            }
        }
        GPTimings.BLOCK_BREAK_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST)
    public void onBlockPlace(ChangeBlockEvent.Place event) {
        GPTimings.BLOCK_PLACE_EVENT.startTimingIfSync();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
            GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
            return;
        }

        World world = event.getTargetWorld();
        Object source = event.getCause().root();
        LocatableBlock locatable = null;
        if (source instanceof LocatableBlock) {
            locatable = (LocatableBlock) source;
        }
        User user = event.getCause().first(User.class).orElse(null);
        Player player = user != null && user instanceof Player ? (Player) user : null;
        GPPlayerData playerData = null;
        if (user != null) {
            playerData = this.dataStore.getOrCreatePlayerData(world, user.getUniqueId());
        }

        GriefPreventionConfig<?> activeConfig = GriefPreventionPlugin.getActiveConfig(world.getProperties());
        GPClaim sourceClaim = this.getSourceClaim(event.getCause());
        if (sourceClaim == null) {
            GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
            return;
        }

        if (!(source instanceof User) && playerData != null && playerData.checkLastInteraction(sourceClaim, user)) {
            GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
            return;
        }

        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
            BlockSnapshot block = transaction.getFinal();
            Location<World> location = block.getLocation().orElse(null);
            if (location == null) {
                continue;
            }

            GPClaim targetClaim = this.dataStore.getClaimAt(location, true, null);
            if (locatable != null /*&& !((IMixinBlock) locatable.getBlockState().getType()).requiresBlockCapture()*/ && targetClaim.isWildernessClaim()) {
                continue;
            }

            // check overrides
            Tristate override = GPPermissionHandler.getFlagOverride(targetClaim, GPPermissions.BLOCK_PLACE, source, block);
            if (override != Tristate.UNDEFINED) {
                if (override == Tristate.TRUE) {
                    GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                    return;
                }

                event.setCancelled(true);
                GriefPreventionPlugin.addEventLogEntry(event, targetClaim, location, source, block, user, null);
                GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                return;
            }

            if (user == null && sourceClaim.getOwnerUniqueId().equals(targetClaim.getOwnerUniqueId())) {
                GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                return;
            }

            String denyReason = GriefPreventionPlugin.instance.allowBuild(source, location, user);
            if (denyReason != null) {
                if (source instanceof PortalTeleportCause) {
                    if (targetClaim != null && player != null) {
                        // cancel and inform about the reason
                        GriefPreventionPlugin.addEventLogEntry(event, targetClaim, location, source, block, user, denyReason);
                        event.setCancelled(true);
                        GriefPreventionPlugin.sendClaimDenyMessage(targetClaim, player, TextMode.Err, Messages.NoBuildPortalPermission, targetClaim.getOwnerName());
                        GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                        return;
                    }
                }

                if (source instanceof Player) {
                    GriefPreventionPlugin.sendClaimDenyMessage(targetClaim, player, TextMode.Err, denyReason);
                }

                GriefPreventionPlugin.addEventLogEntry(event, targetClaim, location, source, block, user, denyReason);
                event.setCancelled(true);
                GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                return;
            }

            // warn players when they place TNT above sea level, since it doesn't destroy blocks there
            if (player != null && GPPermissionHandler.getClaimPermission(targetClaim, GPPermissions.EXPLOSION_SURFACE, event.getCause().root(), block.getState(), user) == Tristate.FALSE && block.getState().getType() == BlockTypes.TNT &&
                    !block.getLocation().get().getExtent().getDimension().getType().equals(DimensionTypes.NETHER) &&
                    block.getPosition().getY() > GriefPreventionPlugin.instance.getSeaLevel(block.getLocation().get().getExtent()) - 5 &&
                    targetClaim.isWildernessClaim() &&
                    playerData.siegeData == null) {
                GriefPreventionPlugin.sendMessage(player, TextMode.Warn, Messages.NoTNTDamageAboveSeaLevel);
            }

            // warn players about disabled pistons outside of land claims
            if (player != null && !playerData.canIgnoreClaim(targetClaim) && activeConfig.getConfig().general.limitPistonsToClaims &&
                    (block.getState().getType() == BlockTypes.PISTON || block.getState().getType() == BlockTypes.STICKY_PISTON) &&
                    targetClaim.isWildernessClaim()) {
                GriefPreventionPlugin.sendMessage(player, TextMode.Warn, Messages.NoPistonsOutsideClaims);
            }

            // Don't run logic below if a player didn't directly cause this event. Prevents issues such as claims getting autocreated while
            // a player is indirectly placing blocks.
            if (!(source instanceof Player)) {
                continue;
            }

            // if the block is being placed within or under an existing claim
            if (!targetClaim.isWildernessClaim() && !targetClaim.isCuboid()) {
                playerData.lastClaim = new WeakReference<>(targetClaim);

                // if the player has permission for the claim and he's placing UNDER the claim
                if (block.getPosition().getY() <= targetClaim.lesserBoundaryCorner.getBlockY()) {
                    denyReason = targetClaim.allowBuild(source, block.getLocation().get(), player);
                    if (denyReason == null) {
                        // extend the claim downward
                        this.dataStore.extendClaim(targetClaim, block.getPosition().getY() - activeConfig.getConfig().claim.extendIntoGroundDistance);
                    }
                }

                // allow for a build warning in the future
                playerData.warnedAboutBuildingOutsideClaims = false;
            }

            if (targetClaim.isWildernessClaim() && activeConfig.getConfig().claim.claimRadius > -1) {
                net.minecraft.tileentity.TileEntity tileEntity = (net.minecraft.tileentity.TileEntity) block.getLocation().get().getTileEntity().orElse(null);
                if (tileEntity == null || !(tileEntity instanceof IInventory) || !(tileEntity instanceof TileEntityChest)) {
                    GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                    return;
                }

                // FEATURE: automatically create a claim when a player who has no claims
                // places a chest otherwise if there's no claim, the player is placing a chest, and new player automatic claims are enabled
                // if the chest is too deep underground, don't create the claim and explain why
                if (block.getPosition().getY() < activeConfig.getConfig().claim.maxClaimDepth) {
                    GriefPreventionPlugin.sendMessage(player, Text.of(TextMode.Warn, Messages.TooDeepToClaim));
                    GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                    return;
                }

                int radius = activeConfig.getConfig().claim.claimRadius;

                // if the player doesn't have any claims yet, automatically create a claim centered at the chest
                if (playerData.getClaims().size() == 0) {
                    // radius == 0 means protect ONLY the chest
                    if (activeConfig.getConfig().claim.claimRadius == 0) {
                        this.dataStore.createClaim(
                                block.getLocation().get().getExtent(), 
                                block.getPosition(), block.getPosition(), ClaimType.BASIC, player.getUniqueId(), false);
                        GriefPreventionPlugin.sendMessage(player, Text.of(TextMode.Success, Messages.ChestClaimConfirmation));
                    }

                    // otherwise, create a claim in the area around the chest
                    else {
                        Vector3i lesserBoundary = new Vector3i(
                                block.getPosition().getX() - radius, 
                                block.getPosition().getY() - activeConfig.getConfig().claim.extendIntoGroundDistance,
                                block.getPosition().getZ() - radius);
                        Vector3i greaterBoundary = new Vector3i(
                                block.getPosition().getX() + radius,
                                block.getPosition().getY(),
                                block.getPosition().getZ() + radius);
                        // as long as the automatic claim overlaps another existing
                        // claim, shrink it note that since the player had permission to place the
                        // chest, at the very least, the automatic claim will include the chest
                        while (radius >= 0 && !this.dataStore.createClaim(block.getLocation().get().getExtent(),
                                lesserBoundary,
                                greaterBoundary,
                                ClaimType.BASIC,
                                player.getUniqueId(), false).successful()) {
                            radius--;
                        }

                        // notify and explain to player
                        GriefPreventionPlugin.sendMessage(player, TextMode.Success, Messages.AutomaticClaimNotification);

                        // show the player the protected area
                        GPClaim newClaim = this.dataStore.getClaimAt(block.getLocation().get(), false, null);
                        Visualization visualization = new Visualization(newClaim, VisualizationType.Claim);
                        visualization.createClaimBlockVisuals(block.getPosition().getY(), player.getLocation(), playerData);
                        visualization.apply(player);
                    }

                    GriefPreventionPlugin.sendMessage(player, TextMode.Instr, Messages.SurvivalBasicsVideo2);
                }

                // check to see if this chest is in a claim, and warn when it isn't
                if (this.dataStore.getClaimAt(block.getLocation().get(), false, playerData.lastClaim) == null) {
                    GriefPreventionPlugin.sendMessage(player, TextMode.Warn, Messages.UnprotectedChestWarning);
                }
            }
        }

        GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST)
    public void onSignChanged(ChangeSignEvent event, @First User user) {
        GPTimings.SIGN_CHANGE_EVENT.startTimingIfSync();
        if (!GriefPreventionPlugin.instance.claimsEnabledForWorld(event.getTargetTile().getLocation().getExtent().getProperties())) {
            GPTimings.SIGN_CHANGE_EVENT.stopTimingIfSync();
            return;
        }

        Location<World> location = event.getTargetTile().getLocation();
        // Prevent users exploiting signs
        GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAt(location, false, null);
        String denyReason = claim.allowAccess(user, location, true);
        if (denyReason != null) {
            if (user instanceof Player) {
                event.setCancelled(true);
                GriefPreventionPlugin.sendClaimDenyMessage(claim, (Player) user, Text.of(TextMode.Err, denyReason));
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
            GriefPreventionPlugin.addEventLogEntry(event, claim, location, user, "contains blocked IP address " + signMessage + ".");
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
        Optional<Entity> entitySource = cause.first(Entity.class);
        Optional<BlockSnapshot> blockSource = cause.first(BlockSnapshot.class);

        GPClaim sourceClaim = null;
        if (blockSource.isPresent()) {
            sourceClaim = this.dataStore.getClaimAt(blockSource.get().getLocation().get(), false, null);
        } else if (entitySource.isPresent()) {
            Entity entity = entitySource.get();
            if (entity instanceof Player) {
                Player player = (Player) entity;
                GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
                sourceClaim = this.dataStore.getClaimAtPlayer(playerData, player.getLocation(), false);
            } else {
                sourceClaim = this.dataStore.getClaimAt(entity.getLocation(), false, null);
            }
        }

        return sourceClaim;
    }
}
