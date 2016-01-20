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
package me.ryanhamshire.GriefPrevention.events;

import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import me.ryanhamshire.GriefPrevention.Visualization;
import me.ryanhamshire.GriefPrevention.VisualizationType;
import net.minecraft.entity.player.EntityPlayer;
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

    // when a player breaks a block...
    @IsCancelled(Tristate.UNDEFINED)
    @Listener(order = Order.DEFAULT)
    public void onBlockBreak(ChangeBlockEvent.Break event) {
        Optional<Player> player = event.getCause().first(Player.class);
        if (player.isPresent()) {
            List<Transaction<BlockSnapshot>> transactions = event.getTransactions();
            for (Transaction<BlockSnapshot> transaction : transactions) {
                // make sure the player is allowed to break at the location
                String noBuildReason = GriefPrevention.instance.allowBreak(player.get(), transaction.getOriginal());
                if (noBuildReason != null) {
                    if (event.getCause().root() instanceof Player) {
                        GriefPrevention.sendMessage(player.get(), Text.of(TextMode.Err, noBuildReason));
                    }
                    transaction.setValid(false);
                }   
            }
        }
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

        Iterator<Direction> iterator = event.getNeighbors().keySet().iterator();
        while (iterator.hasNext()) {
            Direction direction = iterator.next();
            Location<World> location = blockSource.get().getLocation().get().getRelative(direction);
            if ((blockSource.get().getState().getType() == BlockTypes.WATER || blockSource.get().getState().getType() == BlockTypes.FLOWING_WATER) && (location.getBlock().getType() == BlockTypes.FLOWING_LAVA || location.getBlock().getType() == BlockTypes.LAVA)) {
                iterator.remove();
            }
            Claim claim = this.dataStore.getClaimAt(location, false, null);
            if (claim != null) {
                String reason = claim.allowAccess(claim.world, user.get());
                if (reason != null) {
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
        if (claim !=null && claim.allowAccess(claim.world, user.get()) != null) {
            event.setCancelled(true);
        }
    }

    @IsCancelled(Tristate.UNDEFINED)
    @Listener
    public void onBlockChange(ChangeBlockEvent event) {
        Optional<User> user = event.getCause().first(User.class);

        if (!user.isPresent()) {
            return;
        }

        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
            Claim claim = this.dataStore.getClaimAt(transaction.getFinal().getLocation().get(), false, null);
            if (claim !=null && claim.allowAccess(claim.world, user.get()) != null) {
                transaction.setValid(false);
            }
        }
    }

    // when a player places a block...
    @SuppressWarnings({"unchecked", "null"})
    @IsCancelled(Tristate.UNDEFINED)
    @Listener(order = Order.LAST)
    public void onBlockPlace(ChangeBlockEvent.Place event) {
        Optional<Player> playerOpt = event.getCause().first(Player.class);

        if (!playerOpt.isPresent()) {
            return;
        }

        Player player = playerOpt.get();
        // don't track in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getTargetWorld())) {
            return;
        }

        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {

            BlockSnapshot block = transaction.getFinal();

            // FEATURE: limit fire placement, to prevent PvP-by-fire
            if (block.getState().getType() == BlockTypes.FIRE) {
                if (!GriefPrevention.getActiveConfig(player.getWorld()).getConfig().claim.fireSpreadOutsideClaim) {
                    transaction.setValid(false);
                    continue;
                }
            }

            // if placed block is fire and pvp is off, apply rules for proximity to other players
            if (block.getState().getType() == BlockTypes.FIRE && !GriefPrevention.instance.pvpRulesApply(block.getLocation().get().getExtent())
                    && !player.hasPermission("griefprevention.lava")) {
                List<EntityPlayer> players = ((net.minecraft.world.World)block.getLocation().get().getExtent()).playerEntities;
                for (int i = 0; i < players.size(); i++) {
                    Player otherPlayer = (Player) players.get(i);
                    Location<World> location = otherPlayer.getLocation();
                    if (!otherPlayer.equals(player) && location.getBlockPosition().distanceSquared(block.getPosition()) < 9) {
                        if (event.getCause().root() instanceof Player) {
                            GriefPrevention.sendMessage(player, TextMode.Err, Messages.PlayerTooCloseForFire, otherPlayer.getName());
                        }
                        transaction.setValid(false);
                        continue;
                    }
                }
            }
   
    
            // make sure the player is allowed to build at the location
            String noBuildReason = GriefPrevention.instance.allowBuild(player, block.getLocation().get());
            if (noBuildReason != null) {
                if (event.getCause().root() instanceof Player) {
                    GriefPrevention.sendMessage(player, Text.of(TextMode.Err, noBuildReason));
                }
                transaction.setValid(false);
                continue;
            }
    
            // if the block is being placed within or under an existing claim
            PlayerData playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
            Claim claim = this.dataStore.getClaimAt(block.getLocation().get(), true, playerData.lastClaim);

            if (claim != null) {
                playerData.lastClaim = claim;
    
                // warn about TNT not destroying claimed blocks
                if (block.getState().getType().equals(BlockTypes.TNT) && !claim.areExplosivesAllowed && playerData.siegeData == null) {
                    GriefPrevention.sendMessage(player, Text.of(TextMode.Warn, Messages.NoTNTDamageClaims));
                    GriefPrevention.sendMessage(player, Text.of(TextMode.Instr, Messages.ClaimExplosivesAdvertisement));
                }
    
                // if the player has permission for the claim and he's placing UNDER the claim
                if (block.getPosition().getY() <= claim.lesserBoundaryCorner.getBlockY() && claim.allowBuild(player, block.getState().getType()) == null) {
                    // extend the claim downward
                    this.dataStore.extendClaim(claim, block.getPosition().getY() - GriefPrevention.getActiveConfig(player.getWorld()).getConfig().claim.extendIntoGroundDistance);
                }
    
                // allow for a build warning in the future
                playerData.warnedAboutBuildingOutsideClaims = false;
            } else if (block.getState().getType().equals(BlockTypes.CHEST) && GriefPrevention.getActiveConfig(player.getWorld()).getConfig().claim.claimRadius > -1
                    && GriefPrevention.instance.claimsEnabledForWorld(block.getLocation().get().getExtent())) {
                // FEATURE: automatically create a claim when a player who has no claims
                // places a chest otherwise if there's no claim, the player is placing a chest, and new player automatic claims are enabled
                // if the chest is too deep underground, don't create the claim and explain why
                if (GriefPrevention.instance.config_claims_preventTheft && block.getPosition().getY() < GriefPrevention.getActiveConfig(claim.world).getConfig().claim.maxClaimDepth) {
                    GriefPrevention.sendMessage(player, Text.of(TextMode.Warn, Messages.TooDeepToClaim));
                    return;
                }
    
                int radius = GriefPrevention.getActiveConfig(player.getWorld()).getConfig().claim.claimRadius;
    
                // if the player doesn't have any claims yet, automatically create a claim centered at the chest
                if (playerData.playerWorldClaims.get(player.getWorld().getUniqueId()).size() == 0) {
                    // radius == 0 means protect ONLY the chest
                    if (GriefPrevention.getActiveConfig(player.getWorld()).getConfig().claim.claimRadius == 0) {
                        this.dataStore.createClaim(block.getLocation().get().getExtent(), block.getPosition().getX(), block.getPosition().getX(), block.getPosition().getY(), block.getPosition().getY(), block.getPosition().getZ(), block.getPosition().getZ(),
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
                                block.getPosition().getY() - GriefPrevention.getActiveConfig(player.getWorld()).getConfig().claim.extendIntoGroundDistance, block.getPosition().getY(),
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
                        Visualization visualization = Visualization.FromClaim(newClaim, block.getPosition().getY(), VisualizationType.Claim, player.getLocation());
                        Visualization.Apply(player, visualization);
                    }
    
                    GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SurvivalBasicsVideo2);
                }
    
                // check to see if this chest is in a claim, and warn when it isn't
                if (GriefPrevention.instance.config_claims_preventTheft
                        && this.dataStore.getClaimAt(block.getLocation().get(), false, playerData.lastClaim) == null) {
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
            else if (!this.trashBlocks.contains(block.getState().getType()) && GriefPrevention.instance.claimsEnabledForWorld(block.getLocation().get().getExtent())) {
                if (!playerData.warnedAboutBuildingOutsideClaims && !player.hasPermission("griefprevention.adminclaims")
                        && ((playerData.lastClaim == null && playerData.playerWorldClaims.get(player.getWorld().getUniqueId()).size() == 0)
                                || (playerData.lastClaim != null && playerData.lastClaim.isNear(player.getLocation(), 15)))) {
                    Long now = null;
                    if (playerData.buildWarningTimestamp == null || (now = System.currentTimeMillis()) - playerData.buildWarningTimestamp > 600000) { // 10 minute cooldown
                        GriefPrevention.sendMessage(player, TextMode.Warn, Messages.BuildingOutsideClaims);
                        playerData.warnedAboutBuildingOutsideClaims = true;
    
                        if (now == null)
                            now = System.currentTimeMillis();
                        playerData.buildWarningTimestamp = now;
    
                        if (playerData.playerWorldClaims.get(player.getWorld().getUniqueId()).size() < 2) {
                            GriefPrevention.sendMessage(player, TextMode.Instr, Messages.SurvivalBasicsVideo2);
                        }
    
                        if (playerData.lastClaim != null) {
                            Visualization visualization =
                                    Visualization.FromClaim(playerData.lastClaim, block.getPosition().getY(), VisualizationType.Claim, player.getLocation());
                            Visualization.Apply(player, visualization);
                        }
                    }
                }
            }
    
            // warn players when they place TNT above sea level, since it doesn't
            // destroy blocks there
            if (GriefPrevention.instance.config_blockSurfaceOtherExplosions && block.getState().getType() == BlockTypes.TNT &&
                    !block.getLocation().get().getExtent().getDimension().getType().equals(DimensionTypes.NETHER) &&
                    block.getPosition().getY() > GriefPrevention.instance.getSeaLevel(block.getLocation().get().getExtent()) - 5 &&
                    claim == null &&
                    playerData.siegeData == null) {
                GriefPrevention.sendMessage(player, TextMode.Warn, Messages.NoTNTDamageAboveSeaLevel);
            }
    
            // warn players about disabled pistons outside of land claims
            if (GriefPrevention.getActiveConfig(player.getWorld()).getConfig().general.limitPistonsToClaims &&
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
        if (!GriefPrevention.getActiveConfig(event.getTargetTile().getLocation().getExtent()).getConfig().general.generalAdminSignNotifications)
            return;

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
        if (!player.hasPermission("griefprevention.spam") && GriefPrevention.instance.containsBlockedIP(signMessage)) {
            event.setCancelled(true);
            return;
        }

        // if not empty and wasn't the same as the last sign, log it and
        // remember it for later
        PlayerData playerData = this.dataStore.getPlayerData(player.getWorld(), player.getUniqueId());
        if (notEmpty && playerData.lastMessage != null && !playerData.lastMessage.equals(signMessage)) {
            GriefPrevention.AddLogEntry(player.getName() + lines.toString().replace("\n  ", ";"), null);
            //PlayerEventHandler.makeSocialLogEntry(player.get().getName(), signMessage);
            playerData.lastMessage = signMessage;

            if (!player.hasPermission("griefprevention.eavesdropsigns")) {
                Collection<Player> players = (Collection<Player>) Sponge.getGame().getServer().getOnlinePlayers();
                for (Player otherPlayer : players) {
                    if (otherPlayer.hasPermission("griefprevention.eavesdropsigns")) {
                        otherPlayer.sendMessage(Text.of(TextColors.GRAY, player.getName(), signMessage));
                    }
                }
            }
        }
    }
}
