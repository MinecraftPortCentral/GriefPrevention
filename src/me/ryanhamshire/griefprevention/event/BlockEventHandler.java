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

import com.flowpowered.math.vector.Vector3i;
import me.ryanhamshire.griefprevention.CustomLogEntryTypes;
import me.ryanhamshire.griefprevention.DataStore;
import me.ryanhamshire.griefprevention.GPFlags;
import me.ryanhamshire.griefprevention.GPPermissions;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.Visualization;
import me.ryanhamshire.griefprevention.VisualizationType;
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import net.minecraft.block.BlockLiquid;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.action.CollideEvent;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.CollideBlockEvent;
import org.spongepowered.api.event.block.NotifyNeighborBlockEvent;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.event.cause.entity.teleport.PortalTeleportCause;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

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

    // Handle fluids flowing into claims
    @Listener(order = Order.FIRST)
    public void onBlockNotify(NotifyNeighborBlockEvent event) {
        Optional<User> user = event.getCause().first(User.class);
        Optional<BlockSnapshot> blockSource = event.getCause().first(BlockSnapshot.class);

        if (!user.isPresent() || !blockSource.isPresent() || !blockSource.get().getLocation().isPresent()) {
            return;
        }

        Location<World> sourceLocation = blockSource.get().getLocation().get();
        if (!GriefPrevention.instance.claimsEnabledForWorld(sourceLocation.getExtent().getProperties())) {
            return;
        }

        Claim sourceClaim = this.dataStore.getClaimAt(sourceLocation, false, null);
        Iterator<Direction> iterator = event.getNeighbors().keySet().iterator();
        while (iterator.hasNext()) {
            Direction direction = iterator.next();
            Location<World> location = sourceLocation.getRelative(direction);
            Claim targetClaim = this.dataStore.getClaimAt(location, false, null);
            if (targetClaim == null) {
                if (blockSource.get().getState().getType() == BlockTypes.FIRE) {
                    if (!GriefPrevention.getActiveConfig(sourceLocation.getExtent().getProperties()).getConfig().claim.fireSpreadOutsideClaim) {
                        GriefPrevention.addLogEntry("[Event: NotifyNeighborBlockEvent][RootCause: " + event.getCause().root() + "][BlockSnapshot: " + blockSource.get() + "][CancelReason: " + Messages.FireSpreadOutsideClaim + "]", CustomLogEntryTypes.Debug);
                        iterator.remove();
                    }
                }
            } else if (sourceClaim == null) {
                GriefPrevention.addLogEntry("[Event: NotifyNeighborBlockEvent][RootCause: " + event.getCause().root() + "][Removed: " + direction + "][Location: " + location + "][CancelReason: " + Messages.BlockChangeFromWilderness + "]", CustomLogEntryTypes.Debug);
                iterator.remove();
            } else if (sourceClaim != null) {
                if (user.isPresent() && user.get() instanceof Player) {
                    Player player = (Player) user.get();
                    if (targetClaim.doorsOpen && GriefPrevention.getActiveConfig(player.getWorld().getProperties())
                            .getConfig().siege.winnerAccessibleBlocks
                            .contains(location.getBlock().getType().getId())) {
                        continue; // allow siege mode
                    }
                }
                Claim sourceTopLevelClaim = sourceClaim.parent != null ? sourceClaim.parent : sourceClaim;
                Claim targetTopLevelClaim = targetClaim.parent != null ? targetClaim.parent : targetClaim;
                if (sourceTopLevelClaim != targetTopLevelClaim) {
                    GriefPrevention.addLogEntry("[Event: NotifyNeighborBlockEvent][RootCause: " + event.getCause().root() + "][Removed: " + direction + "][Location: " + location + "]", CustomLogEntryTypes.Debug);
                    iterator.remove();
                }
            }
        }
    }

    @Listener(order = Order.FIRST)
    public void onBlockCollide(CollideBlockEvent event, @First User user) {
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getTargetLocation().getExtent().getProperties())) {
            return;
        }

        Claim claim = this.dataStore.getClaimAt(event.getTargetLocation(), false, null);
        if (claim != null) {
            if (user instanceof Player) {
                Player player = (Player) user;
                if (claim.doorsOpen && GriefPrevention.getActiveConfig(player.getWorld().getProperties()).getConfig().siege.winnerAccessibleBlocks
                        .contains(event
                                .getTargetBlock().getType().getId())) {
                    return; // allow siege mode
                }
            }
            DataStore.generateMessages = false;
            String denyReason = claim.allowAccess(user);
            DataStore.generateMessages = true;
            if (denyReason != null) {
                if (event.getTargetLocation().getExtent().getProperties().getTotalTime() % 20 == 0L) { // log once a second to avoid spam
                    GriefPrevention.addLogEntry("[Event: CollideBlockEvent][RootCause: " + event.getCause().root() + "][TargetBlock: " + event.getTargetBlock() + "][CancelReason: No permission.]", CustomLogEntryTypes.Debug);
                 }
                event.setCancelled(true);
            }
        }
    }

    @Listener(order = Order.FIRST)
    public void onImpactEvent(CollideEvent.Impact event, @First User user) {
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getImpactPoint().getExtent().getProperties())) {
            return;
        }

        Claim targetClaim = this.dataStore.getClaimAt(event.getImpactPoint(), false, null);
        if (targetClaim == null) {
            return;
        }

        String denyReason = targetClaim.allowAccess(user);
        if (denyReason != null) {
            GriefPrevention.addLogEntry("[Event: CollideBlockEvent.Impact][RootCause: " + event.getCause().root() + "][ImpactPoint: " + event.getImpactPoint() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
            event.setCancelled(true);
        }
    }

    @Listener(order = Order.FIRST)
    public void onBlockBreak(ChangeBlockEvent.Break event, @First User user) {
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
            return;
        }

        List<Transaction<BlockSnapshot>> transactions = event.getTransactions();
        for (Transaction<BlockSnapshot> transaction : transactions) {
            // make sure the player is allowed to break at the location
            String denyReason = GriefPrevention.instance.allowBreak(user, transaction.getOriginal());
            if (denyReason != null) {
                if (event.getCause().root() instanceof Player) {
                    GriefPrevention.sendMessage((Player) event.getCause().root(), Text.of(TextMode.Err, denyReason));
                }

                GriefPrevention.addLogEntry("[Event: BlockBreakEvent][RootCause: " + event.getCause().root() + "][FirstTransaction: " + event.getTransactions().get(0) + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                event.setCancelled(true);
                return;
            }
        }
    }

    @Listener(order = Order.FIRST)
    public void onBlockChangePost(ChangeBlockEvent.Post event) {
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
            return;
        }

        Optional<User> user = event.getCause().first(User.class);
        Optional<BlockSnapshot> blockSource = event.getCause().first(BlockSnapshot.class);

        if (!user.isPresent() && (!blockSource.isPresent() || !blockSource.get().getLocation().isPresent())) {
            return;
        }

        Claim sourceClaim = null;
        if (blockSource.isPresent()) {
            sourceClaim = this.dataStore.getClaimAt(blockSource.get().getLocation().get(), false, null);
        }

        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
            Vector3i pos = transaction.getFinal().getPosition();
            if (blockSource.isPresent()) {
                Claim targetClaim = this.dataStore.getClaimAt(transaction.getFinal().getLocation().get(), false, null);
                if (sourceClaim == null && targetClaim != null) {
                    GriefPrevention.addLogEntry("[Event: ChangeBlockEvent.Post][RootCause: " + event.getCause().root() + "][FirstTransaction: " + event.getTransactions().get(0) + "][Pos: " + pos + "][CancelReason: " + Messages.BlockChangeFromWilderness + ".]", CustomLogEntryTypes.Debug);
                    event.setCancelled(true);
                    return;
                } else if (sourceClaim != null && targetClaim != null) {
                    Claim sourceTopLevelClaim = sourceClaim.parent != null ? sourceClaim.parent : sourceClaim;
                    Claim targetTopLevelClaim = targetClaim.parent != null ? targetClaim.parent : targetClaim;
                    if (sourceTopLevelClaim != targetTopLevelClaim) {
                        GriefPrevention.addLogEntry("[Event: ChangeBlockEvent.Post][RootCause: " + event.getCause().root() + "][Pos: " + pos + "][FirstTransaction: " + event.getTransactions().get(0) + "][CancelReason: Two different parent claims.]", CustomLogEntryTypes.Debug);
                        event.setCancelled(true);
                        return;
                    }
                }
            } else if (user.isPresent()) {
                String denyReason = GriefPrevention.instance.allowBuild(user.get(), transaction.getFinal().getLocation().get());
                if (denyReason != null) {
                    GriefPrevention.addLogEntry("[Event: ChangeBlockEvent.Post][RootCause: " + event.getCause().root() + "][Pos: " + pos + "][FirstTransaction: " + event.getTransactions().get(0) + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @Listener(order = Order.FIRST)
    public void onBlockPlace(ChangeBlockEvent.Place event, @First User user) {
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
            return;
        }

        World world = event.getTargetWorld();
        Player player = user instanceof Player ? (Player) user : null;
        PlayerData playerData = this.dataStore.getPlayerData(world, user.getUniqueId());
        GriefPreventionConfig<?> activeConfig = GriefPrevention.getActiveConfig(world.getProperties());
        Object rootCause = event.getCause().root();

        Claim sourceClaim = null;
        Optional<BlockSnapshot> sourceBlock = event.getCause().first(BlockSnapshot.class);
        if (sourceBlock.isPresent() && sourceBlock.get().getLocation().isPresent()) {
            sourceClaim = this.dataStore.getClaimAt(sourceBlock.get().getLocation().get(), true, null);
        }

        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
            BlockSnapshot block = transaction.getFinal();
            if (!block.getLocation().isPresent()) {
                continue;
            }

            Claim targetClaim = this.dataStore.getClaimAt(block.getLocation().get(), true, null);
            if (rootCause instanceof PortalTeleportCause && targetClaim == null) {
                // if survival requires claims and user has admin perm to create portal, allow
                if (GriefPrevention.getActiveConfig(block.getLocation().get().getExtent().getProperties()).getConfig().claim.claimMode == 2) {
                    if (user.hasPermission(GPPermissions.PORTAL_CREATE_SURVIVAL)) {
                        return;
                    }
                }
            }

            if (sourceBlock.isPresent() && sourceClaim == null && targetClaim != null) {
                GriefPrevention.addLogEntry("[Event: ChangeBlockEvent.Place][RootCause: " + event.getCause().root() + "][BlockSnapshot: " + block + "][CancelReason: " + Messages.BlockChangeFromWilderness + "]", CustomLogEntryTypes.Debug);
                if (sourceBlock.isPresent() && sourceBlock.get().getState().getType() instanceof BlockLiquid) {
                    transaction.setValid(false);
                    continue;
                } else {
                    event.setCancelled(true);
                    return;
                }
            }

            String denyReason = GriefPrevention.instance.allowBuild(user, block.getLocation().get());
            if (denyReason != null) {
                GriefPrevention.addLogEntry("[Event: ChangeBlockEvent.Place][RootCause: " + event.getCause().root() + "][BlockSnapshot: " + block + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                if (rootCause instanceof PortalTeleportCause) {
                    if (targetClaim != null && player != null) {
                        // cancel and inform about the reason
                        GriefPrevention.addLogEntry("[Event: DisplaceEntityEvent.Portal][RootCause: " + event.getCause().root() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                        event.setCancelled(true);
                        GriefPrevention.sendMessage(player, TextMode.Err, Messages.NoBuildPortalPermission, targetClaim.getOwnerName());
                        return;
                    }
                }
                if (sourceBlock.isPresent() && sourceBlock.get().getState().getType() instanceof BlockLiquid) {
                    transaction.setValid(false);
                    continue;
                } else {
                    event.setCancelled(true);
                    return;
                }
            }

            if (targetClaim == null && !(event.getCause().root() instanceof User) && block.getState().getType() == BlockTypes.FIRE) {
                if (GPFlags.getClaimFlagPermission(targetClaim, GPFlags.FIRE_SPREAD) != Tristate.TRUE) {
                    GriefPrevention.addLogEntry("[Event: ChangeBlockEvent.Place][RootCause: " + event.getCause().root() + "][BlockSnapshot: " + block + "][CancelReason: " + Messages.FireSpreadOutsideClaim + "]", CustomLogEntryTypes.Debug);
                    event.setCancelled(true);
                    return;
                }
            }

            // warn players when they place TNT above sea level, since it doesn't destroy blocks there
            if (player != null && !activeConfig.getConfig().general.surfaceExplosions && block.getState().getType() == BlockTypes.TNT &&
                    !block.getLocation().get().getExtent().getDimension().getType().equals(DimensionTypes.NETHER) &&
                    block.getPosition().getY() > GriefPrevention.instance.getSeaLevel(block.getLocation().get().getExtent()) - 5 &&
                    targetClaim == null &&
                    playerData.siegeData == null) {
                GriefPrevention.sendMessage(player, TextMode.Warn, Messages.NoTNTDamageAboveSeaLevel);
            }

            // warn players about disabled pistons outside of land claims
            if (player != null && activeConfig.getConfig().general.limitPistonsToClaims &&
                    (block.getState().getType() == BlockTypes.PISTON || block.getState().getType() == BlockTypes.STICKY_PISTON) &&
                    targetClaim == null) {
                GriefPrevention.sendMessage(player, TextMode.Warn, Messages.NoPistonsOutsideClaims);
            }

            // Don't run logic below if a player didn't directly cause this event. Prevents issues such as claims getting autocreated while
            // a player is indirectly placing blocks.
            if (!(event.getCause().root() instanceof Player)) {
                continue;
            }

            // if the block is being placed within or under an existing claim
            if (targetClaim != null) {
                playerData.lastClaim = targetClaim;

                // if the player has permission for the claim and he's placing UNDER the claim
                if (block.getPosition().getY() <= targetClaim.lesserBoundaryCorner.getBlockY() && targetClaim.allowBuild(player, block.getLocation().get()) == null) {
                    // extend the claim downward
                    this.dataStore.extendClaim(targetClaim, block.getPosition().getY() - activeConfig.getConfig().claim.extendIntoGroundDistance);
                }

                // allow for a build warning in the future
                playerData.warnedAboutBuildingOutsideClaims = false;
            } else if (block.getState().getType().equals(BlockTypes.CHEST) && activeConfig.getConfig().claim.claimRadius > -1
                    && GriefPrevention.instance.claimsEnabledForWorld(block.getLocation().get().getExtent().getProperties())) {
                // FEATURE: automatically create a claim when a player who has no claims
                // places a chest otherwise if there's no claim, the player is placing a chest, and new player automatic claims are enabled
                // if the chest is too deep underground, don't create the claim and explain why
                if (block.getPosition().getY() < activeConfig.getConfig().claim.maxClaimDepth) {
                    GriefPrevention.sendMessage(player, Text.of(TextMode.Warn, Messages.TooDeepToClaim));
                    return;
                }

                int radius = activeConfig.getConfig().claim.claimRadius;

                // if the player doesn't have any claims yet, automatically create a claim centered at the chest
                if (playerData.getClaims().size() == 0) {
                    // radius == 0 means protect ONLY the chest
                    if (activeConfig.getConfig().claim.claimRadius == 0) {
                        this.dataStore.createClaim(block.getLocation().get().getExtent(), block.getPosition().getX(), block.getPosition().getX(),
                                block.getPosition().getY(), block.getPosition().getY(), block.getPosition().getZ(), block.getPosition().getZ(), UUID.randomUUID(), null, player);
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
                                player).succeeded) {
                            radius--;
                        }

                        // notify and explain to player
                        GriefPrevention.sendMessage(player, TextMode.Success, Messages.AutomaticClaimNotification);

                        // show the player the protected area
                        Claim newClaim = this.dataStore.getClaimAt(block.getLocation().get(), false, null);
                        Visualization visualization =
                                Visualization.FromClaim(newClaim, block.getPosition().getY(), VisualizationType.Claim, player.getLocation());
                        Visualization.Apply(player, visualization);
                    }

                    GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SurvivalBasicsVideo2);
                }

                // check to see if this chest is in a claim, and warn when it isn't
                if (this.dataStore.getClaimAt(block.getLocation().get(), false, playerData.lastClaim) == null) {
                    GriefPrevention.sendMessage(player, TextMode.Warn, Messages.UnprotectedChestWarning);
                }
            }
        }
    }

    // when a player places a sign...
    @Listener(order = Order.FIRST)
    public void onSignChanged(ChangeSignEvent event, @First User user) {
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getTargetTile().getLocation().getExtent().getProperties())) {
            return;
        }

        // send sign content to online administrators
        if (!GriefPrevention.getActiveConfig(event.getTargetTile().getLocation().getExtent().getProperties())
                .getConfig().general.generalAdminSignNotifications) {
            return;
        }

        World world = event.getTargetTile().getLocation().getExtent();
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
            GriefPrevention.addLogEntry("[Event: ChangeBlockEvent.Post][RootCause: " + event.getCause().root() + "][Sign: " + event.getTargetTile() + "][CancelReason: contains blocked IP address " + signMessage + "]", CustomLogEntryTypes.Debug);
            event.setCancelled(true);
            return;
        }

        // if not empty and wasn't the same as the last sign, log it and remember it for later
        PlayerData playerData = this.dataStore.getPlayerData(world, user.getUniqueId());
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
    }
}
