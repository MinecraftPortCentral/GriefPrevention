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
import me.ryanhamshire.griefprevention.Visualization;
import me.ryanhamshire.griefprevention.VisualizationType;
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import net.minecraft.inventory.IInventory;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
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
import org.spongepowered.api.event.cause.entity.teleport.PortalTeleportCause;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.data.util.NbtDataUtil;
import org.spongepowered.common.interfaces.entity.IMixinEntity;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

//event handlers related to blocks
public class BlockEventHandler {

    // convenience reference to singleton datastore
    private DataStore dataStore;

    // constructor
    public BlockEventHandler(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    @Listener(order = Order.FIRST)
    public void onBlockPre(ChangeBlockEvent.Pre event) {
        GPTimings.BLOCK_PRE_EVENT.startTimingIfSync();
        User user = event.getCause().first(User.class).orElse(null);
        Optional<BlockSnapshot> blockSourceOpt = event.getCause().first(BlockSnapshot.class);
        Optional<TileEntity> tileEntityOpt = event.getCause().first(TileEntity.class);
        Object rootCause = event.getCause().root();
        Location<World> sourceLocation = blockSourceOpt.isPresent() ? blockSourceOpt.get().getLocation().orElse(null) : tileEntityOpt.isPresent() ? tileEntityOpt.get().getLocation() : null;
        if (sourceLocation != null) {
            if (!GriefPrevention.instance.claimsEnabledForWorld(sourceLocation.getExtent().getProperties())) {
                GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                return;
            }
    
            Claim sourceClaim = this.dataStore.getClaimAt(sourceLocation, true, null);
            for (Location<World> location : event.getLocations()) {
                Claim targetClaim = this.dataStore.getClaimAt(location, true, null);
                if (user != null && targetClaim.hasFullTrust(user)) {
                    GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                    return;
                } else if (sourceClaim.getOwnerUniqueId().equals(targetClaim.getOwnerUniqueId()) && user == null) {
                    GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                    return;
                }

                String denyReason= GriefPrevention.instance.allowBuild(rootCause, location, user);
                boolean canBreak = true;
                if (denyReason == null) {
                    canBreak = GPPermissionHandler.getClaimPermission(targetClaim, GPPermissions.BLOCK_BREAK, rootCause, location.getBlock(), user) == Tristate.TRUE;
                }
                if (denyReason != null || !canBreak) {
                    GriefPrevention.addEventLogEntry(event, targetClaim, location, user, denyReason);
                    // PRE events can be spammy so we need to avoid sending player messages here.
                    event.setCancelled(true);
                    GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                    return;
                }
            }
        } else if (user != null) {
            boolean rootPlayer = rootCause instanceof User;
            boolean hasFakePlayer = event.getCause().containsNamed("FakePlayer");
            for (Location<World> location : event.getLocations()) {
                Claim targetClaim = this.dataStore.getClaimAt(location, true, null);
                if (targetClaim.hasFullTrust(user)) {
                    GPTimings.BLOCK_PRE_EVENT.stopTimingIfSync();
                    return;
                }

                String denyReason= GriefPrevention.instance.allowBuild(rootCause, location, user);
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
                    GriefPrevention.addEventLogEntry(event, targetClaim, location, user, denyReason);
                    if (!hasFakePlayer && denyReason != null && rootPlayer) {
                        GriefPrevention.sendMessage((Player) rootCause, Text.of(TextMode.Err, denyReason));
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
    public void onBlockNotify(NotifyNeighborBlockEvent event, @Root BlockSnapshot blockSource) {
        GPTimings.BLOCK_NOTIFY_EVENT.startTimingIfSync();
        User user = event.getCause().first(User.class).orElse(null);
        if  (!blockSource.getLocation().isPresent()) {
            GPTimings.BLOCK_NOTIFY_EVENT.stopTimingIfSync();
            return;
        }

        Location<World> sourceLocation = blockSource.getLocation().get();
        if (!GriefPrevention.instance.claimsEnabledForWorld(sourceLocation.getExtent().getProperties())) {
            GPTimings.BLOCK_NOTIFY_EVENT.stopTimingIfSync();
            return;
        }

        Claim sourceClaim = this.getSourceClaim(event.getCause());
        if (sourceClaim == null) {
            GPTimings.BLOCK_NOTIFY_EVENT.stopTimingIfSync();
            return;
        }

        if (user != null) {
            PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(sourceClaim.world, user.getUniqueId());
            if (playerData != null && playerData.checkLastInteraction(sourceClaim, user)) {
                GPTimings.BLOCK_NOTIFY_EVENT.stopTimingIfSync();
                return;
            }
        }

        Iterator<Direction> iterator = event.getNeighbors().keySet().iterator();
        while (iterator.hasNext()) {
            Direction direction = iterator.next();
            Location<World> location = sourceLocation.getRelative(direction);
            Claim targetClaim = this.dataStore.getClaimAt(location, false, null);

            if (sourceClaim.isWildernessClaim() && targetClaim.isWildernessClaim()) {
                continue;
            } else if (!sourceClaim.isWildernessClaim() && targetClaim.isWildernessClaim()) {
                continue;
            } else if (sourceClaim.getOwnerUniqueId().equals(targetClaim.getOwnerUniqueId()) && user == null) {
                continue;
            } else if (user != null) {
                // Needed to handle levers notifying doors to open etc.
                String denyReason = targetClaim.allowAccess(user, location);
                if (denyReason == null) {
                    continue;
                }
            }

            // no claim crossing unless trusted
            GriefPrevention.addEventLogEntry(event, targetClaim, location, user, "Removed direction.");
            iterator.remove();
        }
        GPTimings.BLOCK_NOTIFY_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST)
    public void onBlockCollide(CollideBlockEvent event, @Root Entity source, @First User user) {
        GPTimings.BLOCK_COLLIDE_EVENT.startTimingIfSync();
        if (event.getTargetSide().equals(Direction.UP) || event.getTargetBlock().getType().equals(BlockTypes.AIR) || !GriefPrevention.instance.claimsEnabledForWorld(event.getTargetLocation().getExtent().getProperties())) {
            GPTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
            return;
        }

        Claim targetClaim = null;
        PlayerData playerData = null;
        if (user instanceof Player) {
            Player player = (Player) user;
            playerData = GriefPrevention.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
            targetClaim = this.dataStore.getClaimAtPlayer(playerData, event.getTargetLocation(), false);
        } else {
            targetClaim = this.dataStore.getClaimAt(event.getTargetLocation(), false, null);
        }

        if (user instanceof Player) {
            Player player = (Player) user;
            if (targetClaim.doorsOpen && GriefPrevention.getActiveConfig(player.getWorld().getProperties()).getConfig().siege.winnerAccessibleBlocks
                    .contains(event
                            .getTargetBlock().getType().getId())) {
                GPTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
                return; // allow siege mode
            }
            if (GPPermissionHandler.getClaimPermission(targetClaim, GPPermissions.ENTITY_COLLIDE_BLOCK, source, event.getTargetBlock(), user) == Tristate.TRUE) {
                GPTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
                return;
            }
        }

        if (event.getTargetBlock().getType() == BlockTypes.PORTAL) {
            if (GPPermissionHandler.getClaimPermission(targetClaim, GPPermissions.PORTAL_USE, source, event.getTargetBlock(), user) == Tristate.TRUE) {
                GPTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
                return;
            } else if (event.getCause().root() instanceof Player){
                if (event.getTargetLocation().getExtent().getProperties().getTotalTime() % 20 == 0L) { // log once a second to avoid spam
                    // Disable message temporarily
                    //GriefPrevention.sendMessage((Player) user, TextMode.Err, Messages.NoPortalFromProtectedClaim, claim.getOwnerName());
                    GriefPrevention.addEventLogEntry(event, targetClaim, event.getTargetLocation(), user, GriefPrevention.getMessage(Messages.NoPortalFromProtectedClaim, user.getName()).toPlain());
                    event.setCancelled(true);
                    GPTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
                    return;
                }
            }
        }
        String denyReason = targetClaim.allowAccess(user, event.getTargetLocation());
        //DataStore.generateMessages = true;
        if (denyReason != null) {
            if (GriefPrevention.debugLogging && event.getTargetLocation().getExtent().getProperties().getTotalTime() % 100 == 0L) { // log once a second to avoid spam
               GriefPrevention.addEventLogEntry(event, targetClaim, event.getTargetLocation(), user, denyReason);
            }
            event.setCancelled(true);
        }
        if (playerData != null) {
            playerData.setLastInteractData(targetClaim);
        }
        GPTimings.BLOCK_COLLIDE_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST)
    public void onProjectileImpactBlock(CollideBlockEvent.Impact event, @First User user) {
        GPTimings.PROJECTILE_IMPACT_BLOCK_EVENT.startTimingIfSync();
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getImpactPoint().getExtent().getProperties())) {
            GPTimings.PROJECTILE_IMPACT_BLOCK_EVENT.stopTimingIfSync();
            return;
        }

        Location<World> impactPoint = event.getImpactPoint();
        Claim targetClaim = null;
        PlayerData playerData = null;
        if (user instanceof Player) {
            playerData = GriefPrevention.instance.dataStore.getOrCreatePlayerData(impactPoint.getExtent(), user.getUniqueId());
            targetClaim = this.dataStore.getClaimAtPlayer(playerData, impactPoint, false);
        } else {
            targetClaim = this.dataStore.getClaimAt(impactPoint, false, null);
        }

        String denyReason = targetClaim.allowAccess(user, impactPoint);
        Tristate result = GPPermissionHandler.getClaimPermission(targetClaim, GPPermissions.PROJECTILE_IMPACT_BLOCK, event.getCause().root(), event.getTargetBlock(), user);
        if (result == Tristate.TRUE) {
            GPTimings.PROJECTILE_IMPACT_BLOCK_EVENT.stopTimingIfSync();
            return;
        }
        if (denyReason != null || result == Tristate.FALSE) {
            GriefPrevention.addEventLogEntry(event, targetClaim, impactPoint, user, denyReason);
            event.setCancelled(true);
        }
        GPTimings.PROJECTILE_IMPACT_BLOCK_EVENT.stopTimingIfSync();
    }

    @Listener(order = Order.FIRST)
    public void onExplosion(ExplosionEvent.Detonate event) {
        GPTimings.EXPLOSION_EVENT.startTimingIfSync();
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
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

            Claim claim =  GriefPrevention.instance.dataStore.getClaimAt(blockSnapshot.getLocation().get(), false, null);
            if (GPPermissionHandler.getClaimPermission(claim, GPPermissions.EXPLOSION_SURFACE, source, blockSnapshot.getLocation(), creator) == Tristate.FALSE && location.getPosition().getY() > ((net.minecraft.world.World) event.getTargetWorld()).getSeaLevel()) {
                transaction.setValid(false);
                continue;
            }

            String denyReason = claim.allowBreak(source, location, creator);
            if (denyReason != null) {
                // Avoid lagging server from large explosions.
                if (event.getTransactions().size() > 100) {
                    GriefPrevention.addEventLogEntry(event, claim, location, creator, denyReason);
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

        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
            GPTimings.BLOCK_BREAK_EVENT.stopTimingIfSync();
            return;
        }

        Object source = event.getCause().root();
        User user = event.getCause().first(User.class).orElse(null);
        Claim sourceClaim = this.getSourceClaim(event.getCause());
        if (sourceClaim == null) {
            GPTimings.BLOCK_BREAK_EVENT.stopTimingIfSync();
            return;
        }

        List<Transaction<BlockSnapshot>> transactions = event.getTransactions();
        for (Transaction<BlockSnapshot> transaction : transactions) {
            Location<World> location = transaction.getOriginal().getLocation().orElse(null);
            Claim targetClaim = this.dataStore.getClaimAt(location, false, null);
            if (user != null && targetClaim.hasFullTrust(user)) {
                GPTimings.BLOCK_BREAK_EVENT.stopTimingIfSync();
                return;
            } else if (user == null && sourceClaim.getOwnerUniqueId().equals(targetClaim.getOwnerUniqueId())) {
                GPTimings.BLOCK_BREAK_EVENT.stopTimingIfSync();
                return;
            }

            // make sure the player is allowed to break at the location
            String denyReason = GriefPrevention.instance.allowBreak(source, location, user);
            if (denyReason != null) {
                if (event.getCause().root() instanceof Player) {
                    GriefPrevention.sendMessage((Player) event.getCause().root(), Text.of(TextMode.Err, denyReason));
                }

                GriefPrevention.addEventLogEntry(event, targetClaim, location, user, denyReason);
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
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
            GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
            return;
        }

        World world = event.getTargetWorld();
        Object source = event.getCause().root();
        User user = event.getCause().first(User.class).orElse(null);
        Player player = user != null && user instanceof Player ? (Player) user : null;
        PlayerData playerData = null;
        if (user != null) {
            playerData = this.dataStore.getOrCreatePlayerData(world, user.getUniqueId());
        }

        GriefPreventionConfig<?> activeConfig = GriefPrevention.getActiveConfig(world.getProperties());
        Claim sourceClaim = this.getSourceClaim(event.getCause());
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

            Claim targetClaim = this.dataStore.getClaimAt(location, true, null);
            if (user == null && sourceClaim.getOwnerUniqueId().equals(targetClaim.getOwnerUniqueId())) {
                GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                return;
            }

            String denyReason = GriefPrevention.instance.allowBuild(source, location, user);
            if (denyReason != null) {
                if (source instanceof PortalTeleportCause) {
                    if (targetClaim != null && player != null) {
                        // cancel and inform about the reason
                        GriefPrevention.addEventLogEntry(event, targetClaim, location, user, denyReason);
                        event.setCancelled(true);
                        GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoBuildPortalPermission, targetClaim.getOwnerName());
                        GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                        return;
                    }
                }

                if (source instanceof Player) {
                    GriefPrevention.sendMessage(player, TextMode.Err, denyReason);
                }

                GriefPrevention.addEventLogEntry(event, targetClaim, location, user, denyReason);
                event.setCancelled(true);
                GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                return;
            }

            // warn players when they place TNT above sea level, since it doesn't destroy blocks there
            if (player != null && GPPermissionHandler.getClaimPermission(targetClaim, GPPermissions.EXPLOSION_SURFACE, event.getCause().root(), block.getState(), user) == Tristate.FALSE && block.getState().getType() == BlockTypes.TNT &&
                    !block.getLocation().get().getExtent().getDimension().getType().equals(DimensionTypes.NETHER) &&
                    block.getPosition().getY() > GriefPrevention.instance.getSeaLevel(block.getLocation().get().getExtent()) - 5 &&
                    targetClaim.isWildernessClaim() &&
                    playerData.siegeData == null) {
                GriefPrevention.sendMessage(player, TextMode.Warn, Messages.NoTNTDamageAboveSeaLevel);
            }

            // warn players about disabled pistons outside of land claims
            if (player != null && !playerData.ignoreClaims && activeConfig.getConfig().general.limitPistonsToClaims &&
                    (block.getState().getType() == BlockTypes.PISTON || block.getState().getType() == BlockTypes.STICKY_PISTON) &&
                    targetClaim.isWildernessClaim()) {
                GriefPrevention.sendMessage(player, TextMode.Warn, Messages.NoPistonsOutsideClaims);
            }

            // Don't run logic below if a player didn't directly cause this event. Prevents issues such as claims getting autocreated while
            // a player is indirectly placing blocks.
            if (!(source instanceof Player)) {
                continue;
            }

            // if the block is being placed within or under an existing claim
            if (!targetClaim.isWildernessClaim() && !targetClaim.cuboid) {
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
                if (tileEntity == null || !(tileEntity instanceof IInventory)) {
                    GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                    return;
                }

                // FEATURE: automatically create a claim when a player who has no claims
                // places a chest otherwise if there's no claim, the player is placing a chest, and new player automatic claims are enabled
                // if the chest is too deep underground, don't create the claim and explain why
                if (block.getPosition().getY() < activeConfig.getConfig().claim.maxClaimDepth) {
                    GriefPrevention.sendMessage(player, Text.of(TextMode.Warn, Messages.TooDeepToClaim));
                    GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
                    return;
                }

                int radius = activeConfig.getConfig().claim.claimRadius;

                // if the player doesn't have any claims yet, automatically create a claim centered at the chest
                if (playerData.getClaims().size() == 0) {
                    // radius == 0 means protect ONLY the chest
                    if (activeConfig.getConfig().claim.claimRadius == 0) {
                        this.dataStore.createClaim(block.getLocation().get().getExtent(), block.getPosition().getX(), block.getPosition().getX(),
                                block.getPosition().getY(), block.getPosition().getY(), block.getPosition().getZ(), block.getPosition().getZ(), UUID.randomUUID(), null, Claim.Type.BASIC, false, player);
                        GriefPrevention.sendMessage(player, Text.of(TextMode.Success, Messages.ChestClaimConfirmation));
                    }

                    // otherwise, create a claim in the area around the chest
                    else {
                        // as long as the automatic claim overlaps another existing
                        // claim, shrink it note that since the player had permission to place the
                        // chest, at the very least, the automatic claim will include the chest
                        while (radius >= 0 && !this.dataStore.createClaim(block.getLocation().get().getExtent(),
                                block.getPosition().getX() - radius, block.getPosition().getX() + radius,
                                block.getPosition().getY() - activeConfig.getConfig().claim.extendIntoGroundDistance, block.getPosition().getY(),
                                block.getPosition().getZ() - radius, block.getPosition().getZ() + radius,
                                UUID.randomUUID(),
                                null,
                                Claim.Type.BASIC, false,
                                player).succeeded) {
                            radius--;
                        }

                        // notify and explain to player
                        GriefPrevention.sendMessage(player, TextMode.Success, Messages.AutomaticClaimNotification);

                        // show the player the protected area
                        Claim newClaim = this.dataStore.getClaimAt(block.getLocation().get(), false, null);
                        Visualization visualization = new Visualization(newClaim, VisualizationType.Claim);
                        visualization.createClaimBlockVisuals(block.getPosition().getY(), player.getLocation(), playerData);
                        visualization.apply(player);
                    }

                    GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SurvivalBasicsVideo2);
                }

                // check to see if this chest is in a claim, and warn when it isn't
                if (this.dataStore.getClaimAt(block.getLocation().get(), false, playerData.lastClaim) == null) {
                    GriefPrevention.sendMessage(player, TextMode.Warn, Messages.UnprotectedChestWarning);
                }
            }
        }

        GPTimings.BLOCK_PLACE_EVENT.stopTimingIfSync();
    }

    // when a player places a sign...
    @Listener(order = Order.FIRST)
    public void onSignChanged(ChangeSignEvent event, @First User user) {
        GPTimings.SIGN_CHANGE_EVENT.startTimingIfSync();
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getTargetTile().getLocation().getExtent().getProperties())) {
            GPTimings.SIGN_CHANGE_EVENT.stopTimingIfSync();
            return;
        }

        Location<World> location = event.getTargetTile().getLocation();
        // send sign content to online administrators
        if (!GriefPrevention.getActiveConfig(location.getExtent().getProperties())
                .getConfig().general.generalAdminSignNotifications) {
            GPTimings.SIGN_CHANGE_EVENT.stopTimingIfSync();
            return;
        }

        World world = location.getExtent();
        StringBuilder lines = new StringBuilder(" placed a sign @ " + GriefPrevention.getfriendlyLocationString(event.getTargetTile().getLocation()));
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
        if (!user.hasPermission(GPPermissions.SPAM) && GriefPrevention.instance.containsBlockedIP(signMessage)) {
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
            GriefPrevention.addEventLogEntry(event, claim, location, user, "contains blocked IP address " + signMessage + ".");
            event.setCancelled(true);
            GPTimings.SIGN_CHANGE_EVENT.stopTimingIfSync();
            return;
        }

        // if not empty and wasn't the same as the last sign, log it and remember it for later
        PlayerData playerData = this.dataStore.getOrCreatePlayerData(world, user.getUniqueId());
        if (notEmpty && playerData.lastMessage != null && !playerData.lastMessage.equals(signMessage)) {
            GriefPrevention.addLogEntry(user.getName() + lines.toString().replace("\n  ", ";"), null);
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

    public Claim getSourceClaim(Cause cause) {
        Optional<Entity> entitySource = cause.first(Entity.class);
        Optional<BlockSnapshot> blockSource = cause.first(BlockSnapshot.class);

        Claim sourceClaim = null;
        if (blockSource.isPresent()) {
            sourceClaim = this.dataStore.getClaimAt(blockSource.get().getLocation().get(), false, null);
        } else if (entitySource.isPresent()) {
            Entity entity = entitySource.get();
            if (entity instanceof Player) {
                Player player = (Player) entity;
                PlayerData playerData = GriefPrevention.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
                sourceClaim = this.dataStore.getClaimAtPlayer(playerData, player.getLocation(), false);
            } else {
                sourceClaim = this.dataStore.getClaimAt(entity.getLocation(), false, null);
            }
        }

        return sourceClaim;
    }
}
