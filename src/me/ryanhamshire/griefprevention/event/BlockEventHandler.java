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

import com.flowpowered.math.vector.Vector3i;
import me.ryanhamshire.griefprevention.CustomLogEntryTypes;
import me.ryanhamshire.griefprevention.DataStore;
import me.ryanhamshire.griefprevention.GPPermissions;
import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.Messages;
import me.ryanhamshire.griefprevention.PlayerData;
import me.ryanhamshire.griefprevention.TextMode;
import me.ryanhamshire.griefprevention.Visualization;
import me.ryanhamshire.griefprevention.VisualizationType;
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.CollideBlockEvent;
import org.spongepowered.api.event.block.NotifyNeighborBlockEvent;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

//event handlers related to blocks
public class BlockEventHandler {

    // convenience reference to singleton datastore
    private DataStore dataStore;

    private ArrayList<BlockType> trashBlocks;

    // constructor
    public BlockEventHandler(DataStore dataStore) {
        this.dataStore = dataStore;

        // create the list of blocks which will not trigger a warning when
        // they're placed outside of land claims
        this.trashBlocks = new ArrayList<BlockType>();
        this.trashBlocks.add(BlockTypes.COBBLESTONE);
        this.trashBlocks.add(BlockTypes.TORCH);
        this.trashBlocks.add(BlockTypes.DIRT);
        this.trashBlocks.add(BlockTypes.SAPLING);
        this.trashBlocks.add(BlockTypes.GRAVEL);
        this.trashBlocks.add(BlockTypes.SAND);
        this.trashBlocks.add(BlockTypes.TNT);
        this.trashBlocks.add(BlockTypes.CRAFTING_TABLE);
    }

    // Handle items being dropped into claims
    @IsCancelled(Tristate.UNDEFINED)
    @Listener
    public void onDropItem(DropItemEvent.Dispense event) {
        Optional<Player> optPlayer = event.getCause().first(Player.class);

        if (!optPlayer.isPresent()) {
            return;
        }

        Player player = optPlayer.get();
        Location<World> location = player.getLocation();
        Claim claim = this.dataStore.getClaimAt(location, false, null);
        if (claim != null) {
            String reason = claim.allowAccess(player.getWorld(), player);
            if (reason != null) {
                if (event.getCause().root() instanceof Player) {
                    GriefPrevention.sendMessage(player, Text.of(TextMode.Warn, Messages.NoDropsAllowed));
                }

                GriefPrevention.addLogEntry("[Event: DropItemEvent][RootCause: " + event.getCause().root() + "][CancelReason: " + reason + "]", CustomLogEntryTypes.Debug);
                event.setCancelled(true);
            }
        }
    }

    // Handle fluids flowing into claims
    @Listener
    public void onBlockNotify(NotifyNeighborBlockEvent event) {
        Optional<User> user = event.getCause().first(User.class);
        Optional<BlockSnapshot> blockSource = event.getCause().first(BlockSnapshot.class);

        if (!user.isPresent() || !blockSource.isPresent()) {
            return;
        }

        Claim sourceClaim = this.dataStore.getClaimAt(blockSource.get().getLocation().get(), false, null);
        Iterator<Direction> iterator = event.getNeighbors().keySet().iterator();
        while (iterator.hasNext()) {
            Direction direction = iterator.next();
            Location<World> location = blockSource.get().getLocation().get().getRelative(direction);
            Claim targetClaim = this.dataStore.getClaimAt(location, false, null);
            if (sourceClaim == null && targetClaim != null) {
                GriefPrevention.addLogEntry("[Event: NotifyNeighborBlockEvent][RootCause: " + event.getCause().root() + "][Removed: " + direction + "][Location: " + location + "]", CustomLogEntryTypes.Debug);
                iterator.remove();
            } else if (sourceClaim != null && targetClaim != null) {
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

    @IsCancelled(Tristate.UNDEFINED)
    @Listener
    public void onBlockCollide(CollideBlockEvent event) {
        Optional<User> user = event.getCause().first(User.class);

        if (!user.isPresent()) {
            return;
        }

        Claim claim = this.dataStore.getClaimAt(event.getTargetLocation(), false, null);
        if (claim != null) {
            if (user.isPresent() && user.get() instanceof Player) {
                Player player = (Player) user.get();
                if (claim.doorsOpen && GriefPrevention.getActiveConfig(player.getWorld().getProperties()).getConfig().siege.winnerAccessibleBlocks
                        .contains(event
                                .getTargetBlock().getType().getId())) {
                    return; // allow siege mode
                }
            }
            String denyReason = claim.allowAccess(claim.world, user.get());
            if (denyReason != null) {
                if (event.getTargetLocation().getExtent().getProperties().getTotalTime() % 20 == 0L) { // log once a second to avoid spam
                    GriefPrevention.addLogEntry("[Event: CollideBlockEvent][RootCause: " + event.getCause().root() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                 }
                event.setCancelled(true);
            }
        }
    }

    @Listener(order = Order.EARLY)
    public void onImpactEvent(CollideBlockEvent.Impact event) {
        Optional<User> user = event.getCause().first(User.class);
        if (!user.isPresent()) {
            return;
        }

        Claim targetClaim = this.dataStore.getClaimAt(event.getImpactPoint(), false, null);
        if (targetClaim == null) {
            return;
        }

        String denyReason = targetClaim.allowAccess(targetClaim.world, user.get());
        if (denyReason != null) {
            GriefPrevention.addLogEntry("[Event: CollideBlockEvent.Impact][RootCause: " + event.getCause().root() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
            event.setCancelled(true);
        }
    }

    @IsCancelled(Tristate.UNDEFINED)
    @Listener(order = Order.DEFAULT)
    public void onBlockBreak(ChangeBlockEvent.Break event) {
        Optional<User> user = event.getCause().first(User.class);
        if (user.isPresent()) {
            List<Transaction<BlockSnapshot>> transactions = event.getTransactions();
            for (Transaction<BlockSnapshot> transaction : transactions) {
                // make sure the player is allowed to break at the location
                String denyReason = GriefPrevention.instance.allowBreak(user.get(), transaction.getOriginal().getLocation().get());
                if (denyReason != null) {
                    if (event.getCause().root() instanceof Player) {
                        GriefPrevention.sendMessage((Player) event.getCause().root(), Text.of(TextMode.Err, denyReason));
                    }

                    GriefPrevention.addLogEntry("[Event: BlockBreakEvent][RootCause: " + event.getCause().root() + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @IsCancelled(Tristate.UNDEFINED)
    @Listener
    public void onBlockChangePost(ChangeBlockEvent.Post event) {
        Optional<User> user = event.getCause().first(User.class);
        Optional<BlockSnapshot> blockSource = event.getCause().first(BlockSnapshot.class);

        if (!user.isPresent() && !blockSource.isPresent()) {
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
                    GriefPrevention.addLogEntry("[Event: ChangeBlockEvent.Post][RootCause: " + event.getCause().root() + "][Pos: " + pos + "][CancelReason: Source came from outside a claimed area.]", CustomLogEntryTypes.Debug);
                    event.setCancelled(true);
                    return;
                } else if (sourceClaim != null && targetClaim != null) {
                    Claim sourceTopLevelClaim = sourceClaim.parent != null ? sourceClaim.parent : sourceClaim;
                    Claim targetTopLevelClaim = targetClaim.parent != null ? targetClaim.parent : targetClaim;
                    if (sourceTopLevelClaim != targetTopLevelClaim) {
                        GriefPrevention.addLogEntry("[Event: ChangeBlockEvent.Post][RootCause: " + event.getCause().root() + "][Pos: " + pos + "][CancelReason: Two different parent claims.]", CustomLogEntryTypes.Debug);
                        event.setCancelled(true);
                        return;
                    }
                }
            } else if (user.isPresent()) {
                Claim claim = this.dataStore.getClaimAt(transaction.getFinal().getLocation().get(), false, null);
                if (claim == null) {
                    return;
                }

                String denyReason = claim.allowAccess(claim.world, user.get());
                if (denyReason != null) {
                    GriefPrevention.addLogEntry("[Event: ChangeBlockEvent.Post][RootCause: " + event.getCause().root() + "][Pos: " + pos + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @SuppressWarnings({"null"})
    @IsCancelled(Tristate.UNDEFINED)
    @Listener(order = Order.LAST)
    public void onBlockPlace(ChangeBlockEvent.Place event) {
        Optional<Player> playerOpt = event.getCause().first(Player.class);

        if (!playerOpt.isPresent()) {
            return;
        }

        Player player = playerOpt.get();
        PlayerData playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        GriefPreventionConfig<?> activeConfig = GriefPrevention.getActiveConfig(player.getWorld().getProperties());
        // don't track in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getTargetWorld().getProperties())) {
            return;
        }

        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {

            BlockSnapshot block = transaction.getFinal();

            Claim claim = this.dataStore.getClaimAt(block.getLocation().get(), true, playerData.lastClaim);
            if (claim != null) {
                String denyReason = claim.allowBuild(player, block.getLocation().get());
                if (denyReason != null) {
                    GriefPrevention.addLogEntry("[Event: ChangeBlockEvent.Place][RootCause: " + event.getCause().root() + "][BlockSnapshot: " + block + "][CancelReason: " + denyReason + "]", CustomLogEntryTypes.Debug);
                    event.setCancelled(true);
                    return;
                }
            }
            // FEATURE: limit fire placement, to prevent PvP-by-fire
            if (block.getState().getType() == BlockTypes.FIRE) {
                if (!activeConfig.getConfig().claim.fireSpreadOutsideClaim) {
                    GriefPrevention.addLogEntry("[Event: ChangeBlockEvent.Place][RootCause: " + event.getCause().root() + "][BlockSnapshot: " + block + "][CancelReason: " + Messages.FireSpreadOutsideClaim + "]", CustomLogEntryTypes.Debug);
                    event.setCancelled(true);
                    return;
                }
            }

            // if the block is being placed within or under an existing claim
            if (claim != null) {
                playerData.lastClaim = claim;

                // if the player has permission for the claim and he's placing UNDER the claim
                if (block.getPosition().getY() <= claim.lesserBoundaryCorner.getBlockY() && claim.allowBuild(player, block.getLocation().get()) ==
                        null) {
                    // extend the claim downward
                    this.dataStore.extendClaim(claim, block.getPosition().getY() - activeConfig.getConfig().claim.extendIntoGroundDistance);
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
                if (playerData.playerWorldClaims.get(player.getWorld().getUniqueId()).size() == 0) {
                    // radius == 0 means protect ONLY the chest
                    if (activeConfig.getConfig().claim.claimRadius == 0) {
                        this.dataStore.createClaim(block.getLocation().get().getExtent(), block.getPosition().getX(), block.getPosition().getX(),
                                block.getPosition().getY(), block.getPosition().getY(), block.getPosition().getZ(), block.getPosition().getZ(),
                                player.getUniqueId(), null, UUID.randomUUID(), player);
                        GriefPrevention.sendMessage(player, Text.of(TextMode.Success, Messages.ChestClaimConfirmation));
                    }

                    // otherwise, create a claim in the area around the chest
                    else {
                        // as long as the automatic claim overlaps another existing
                        // claim, shrink it
                        // note that since the player had permission to place the
                        // chest, at the very least, the automatic claim will
                        // include the chest
                        while (radius >= 0 && !this.dataStore.createClaim(block.getLocation().get().getExtent(),
                                block.getPosition().getX() - radius, block.getPosition().getX() + radius,
                                block.getPosition().getY() - activeConfig.getConfig().claim.extendIntoGroundDistance, block.getPosition().getY(),
                                block.getPosition().getZ() - radius, block.getPosition().getZ() + radius,
                                player.getUniqueId(),
                                null, null,
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

            // FEATURE: limit wilderness tree planting to grass, or dirt with more
            // blocks beneath it
            // TODO
            /*else if (block.getState().getType() == BlockTypes.SAPLING && GriefPrevention.instance.config_blockSkyTrees
                    && GriefPrevention.instance.claimsEnabledForWorld(player.get().getWorld())) {
                Block earthBlock = event.getBlockAgainst();
                if (earthBlock.getType() != Material.GRASS) {
                    if (earthBlock.getRelative(BlockFace.DOWN).getType() == Material.AIR ||
                            earthBlock.getRelative(BlockFace.DOWN).getRelative(BlockFace.DOWN).getType() == Material.AIR) {
                        placeEvent.setCancelled(true);
                    }
                }
            }*/

            // FEATURE: warn players when they're placing non-trash blocks outside
            // of their claimed areas
            else if (!this.trashBlocks.contains(block.getState().getType()) && GriefPrevention.instance
                    .claimsEnabledForWorld(block.getLocation().get().getExtent().getProperties())) {
                if (!playerData.warnedAboutBuildingOutsideClaims && !player.hasPermission(GPPermissions.ADMIN_CLAIMS)
                        && ((playerData.lastClaim == null && playerData.playerWorldClaims.get(player.getWorld().getUniqueId()).size() == 0)
                        || (playerData.lastClaim != null && playerData.lastClaim.isNear(player.getLocation(), 15)))) {
                    Long now = null;
                    if (playerData.buildWarningTimestamp == null
                            || (now = System.currentTimeMillis()) - playerData.buildWarningTimestamp > 600000) { // 10 minute cooldown
                        GriefPrevention.sendMessage(player, TextMode.Warn, Messages.BuildingOutsideClaims);
                        playerData.warnedAboutBuildingOutsideClaims = true;

                        if (now == null) {
                            now = System.currentTimeMillis();
                        }
                        playerData.buildWarningTimestamp = now;

                        if (playerData.playerWorldClaims.get(player.getWorld().getUniqueId()).size() < 2) {
                            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SurvivalBasicsVideo2);
                        }

                        if (playerData.lastClaim != null) {
                            Visualization visualization =
                                    Visualization.FromClaim(playerData.lastClaim, block.getPosition().getY(), VisualizationType.Claim,
                                            player.getLocation());
                            Visualization.Apply(player, visualization);
                        }
                    }
                }
            }

            // warn players when they place TNT above sea level, since it doesn't
            // destroy blocks there
            if (!activeConfig.getConfig().general.surfaceExplosions && block.getState().getType() == BlockTypes.TNT &&
                    !block.getLocation().get().getExtent().getDimension().getType().equals(DimensionTypes.NETHER) &&
                    block.getPosition().getY() > GriefPrevention.instance.getSeaLevel(block.getLocation().get().getExtent()) - 5 &&
                    claim == null &&
                    playerData.siegeData == null) {
                GriefPrevention.sendMessage(player, TextMode.Warn, Messages.NoTNTDamageAboveSeaLevel);
            }

            // warn players about disabled pistons outside of land claims
            if (activeConfig.getConfig().general.limitPistonsToClaims &&
                    (block.getState().getType() == BlockTypes.PISTON || block.getState().getType() == BlockTypes.STICKY_PISTON) &&
                    claim == null) {
                GriefPrevention.sendMessage(player, TextMode.Warn, Messages.NoPistonsOutsideClaims);
            }
        }
    }

    // when a player places a sign...
    @IsCancelled(Tristate.UNDEFINED)
    @Listener
    public void onSignChanged(ChangeSignEvent event) {
        // send sign content to online administrators
        if (!GriefPrevention.getActiveConfig(event.getTargetTile().getLocation().getExtent().getProperties())
                .getConfig().general.generalAdminSignNotifications) {
            return;
        }

        Optional<Player> optPlayer = event.getCause().first(Player.class);
        if (!optPlayer.isPresent()) {
            return;
        }

        Player player = optPlayer.get();
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
        if (!player.hasPermission(GPPermissions.SPAM) && GriefPrevention.instance.containsBlockedIP(signMessage)) {
            GriefPrevention.addLogEntry("[Event: ChangeBlockEvent.Post][RootCause: " + event.getCause().root() + "][Sign: " + event.getTargetTile() + "][CancelReason: contains blocked IP address " + signMessage + "]", CustomLogEntryTypes.Debug);
            event.setCancelled(true);
            return;
        }

        // if not empty and wasn't the same as the last sign, log it and
        // remember it for later
        PlayerData playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        if (notEmpty && playerData.lastMessage != null && !playerData.lastMessage.equals(signMessage)) {
            GriefPrevention.addLogEntry(player.getName() + lines.toString().replace("\n  ", ";"), null);
            //PlayerEventHandler.makeSocialLogEntry(player.get().getName(), signMessage);
            playerData.lastMessage = signMessage;

            if (!player.hasPermission(GPPermissions.EAVES_DROP_SIGNS)) {
                Collection<Player> players = (Collection<Player>) Sponge.getGame().getServer().getOnlinePlayers();
                for (Player otherPlayer : players) {
                    if (otherPlayer.hasPermission(GPPermissions.EAVES_DROP_SIGNS)) {
                        otherPlayer.sendMessage(Text.of(TextColors.GRAY, player.getName(), signMessage));
                    }
                }
            }
        }
    }
}
