/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.ryanhamshire.GriefPrevention;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTransaction;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.carrier.Dispenser;
import org.spongepowered.api.block.tileentity.carrier.Hopper;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.DimensionTypes;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
    @Listener(ignoreCancelled = true, order = Order.DEFAULT)
    public void onBlockBreak(ChangeBlockEvent.Break event) {
        Optional<Player> player = event.getCause().first(Player.class);
        if (player.isPresent()) {
            List<BlockTransaction> transactions = event.getTransactions();
            for (BlockTransaction transaction : transactions) {
                // make sure the player is allowed to break at the location
                String noBuildReason = GriefPrevention.instance.allowBreak(player.get(), transaction.getOriginal());
                if (noBuildReason != null) {
                    GriefPrevention.sendMessage(player.get(), Texts.of(TextMode.Err, noBuildReason));
                    transaction.setIsValid(false);
                }   
            }
        }
    }

    // when a player places a sign...
    @Listener(ignoreCancelled = true)
    public void onSignChanged(ChangeSignEvent event) {
        // send sign content to online administrators
        if (!GriefPrevention.instance.config_signNotifications)
            return;

        Optional<Player> player = event.getCause().first(Player.class);
        if (!player.isPresent()) {
            return;
        }

        StringBuilder lines = new StringBuilder(" placed a sign @ " + GriefPrevention.getfriendlyLocationString(event.getTargetTile().getLocation()));
        boolean notEmpty = false;
        for (int i = 0; i < event.getText().lines().size(); i++) {
            String withoutSpaces = Texts.toPlain(event.getText().lines().get(i)).replace(" ", "");
            if (!withoutSpaces.isEmpty()) {
                notEmpty = true;
                lines.append("\n  " + event.getText().lines().get(i));
            }
        }

        String signMessage = lines.toString();

        // prevent signs with blocked IP addresses
        if (!player.get().hasPermission("griefprevention.spam") && GriefPrevention.instance.containsBlockedIP(signMessage)) {
            event.setCancelled(true);
            return;
        }

        // if not empty and wasn't the same as the last sign, log it and
        // remember it for later
        PlayerData playerData = this.dataStore.getPlayerData(player.get().getUniqueId());
        if (notEmpty && playerData.lastMessage != null && !playerData.lastMessage.equals(signMessage)) {
            GriefPrevention.AddLogEntry(player.get().getName() + lines.toString().replace("\n  ", ";"), null);
            //PlayerEventHandler.makeSocialLogEntry(player.get().getName(), signMessage);
            playerData.lastMessage = signMessage;

            if (!player.get().hasPermission("griefprevention.eavesdropsigns")) {
                Collection<Player> players = (Collection<Player>) GriefPrevention.instance.game.getServer().getOnlinePlayers();
                for (Player otherPlayer : players) {
                    if (otherPlayer.hasPermission("griefprevention.eavesdropsigns")) {
                        otherPlayer.sendMessage(Texts.of(TextColors.GRAY, player.get().getName(), signMessage));
                    }
                }
            }
        }
    }

    // when a player places multiple blocks...
    /*@Listener(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlocksPlace(BlockMultiPlaceEvent placeEvent) {
        Player player = placeEvent.getPlayer();

        // don't track in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(placeEvent.getBlock().getWorld()))
            return;

        // make sure the player is allowed to build at the location
        for (BlockState block : placeEvent.getReplacedBlockStates()) {
            String noBuildReason = GriefPrevention.instance.allowBuild(player, block.getLocation(), block.getType());
            if (noBuildReason != null) {
                GriefPrevention.sendMessage(player, TextMode.Err, noBuildReason);
                placeEvent.setCancelled(true);
                return;
            }
        }
    }*/

    // when a player places a block...
    @Listener(ignoreCancelled = true, order = Order.LAST)
    public void onBlockPlace(ChangeBlockEvent.Place event) {
        Optional<Player> player = event.getCause().first(Player.class);

        if (!player.isPresent()) {
            return;
        }

        for (BlockTransaction transaction : event.getTransactions()) {
            // FEATURE: limit fire placement, to prevent PvP-by-fire
    
            // if placed block is fire and pvp is off, apply rules for proximity to
            // other players
            BlockSnapshot block = transaction.getFinalReplacement();
            if (block.getState().getType() == BlockTypes.FIRE && !GriefPrevention.instance.pvpRulesApply(block.getLocation().get().getExtent())
                    && !player.get().hasPermission("griefprevention.lava")) {
                List<Player> players = ((net.minecraft.world.World)block.getLocation().get().getExtent()).playerEntities;
                for (int i = 0; i < players.size(); i++) {
                    Player otherPlayer = players.get(i);
                    Location<World> location = otherPlayer.getLocation();
                    if (!otherPlayer.equals(player.get()) && location.getBlockPosition().distanceSquared(block.getPosition()) < 9) {
                        GriefPrevention.sendMessage(player.get(), TextMode.Err, Messages.PlayerTooCloseForFire, otherPlayer.getName());
                        transaction.setIsValid(false);
                    }
                }
            }
    
            // don't track in worlds where claims are not enabled
            if (!GriefPrevention.instance.claimsEnabledForWorld(block.getLocation().get().getExtent()))
                return;
    
            // make sure the player is allowed to build at the location
            String noBuildReason = GriefPrevention.instance.allowBuild(player.get(), block.getLocation().get());
            if (noBuildReason != null) {
                GriefPrevention.sendMessage(player.get(), Texts.of(TextMode.Err, noBuildReason));
                transaction.setIsValid(false);
                continue;
            }
    
            // if the block is being placed within or under an existing claim
            PlayerData playerData = this.dataStore.getPlayerData(player.get().getUniqueId());
            Claim claim = this.dataStore.getClaimAt(block.getLocation().get(), true, playerData.lastClaim);
            if (claim != null) {
                playerData.lastClaim = claim;
    
                // warn about TNT not destroying claimed blocks
                if (block.getState().getType() == BlockTypes.TNT && !claim.areExplosivesAllowed && playerData.siegeData == null) {
                    GriefPrevention.sendMessage(player.get(), Texts.of(TextMode.Warn, Messages.NoTNTDamageClaims));
                    GriefPrevention.sendMessage(player.get(), Texts.of(TextMode.Instr, Messages.ClaimExplosivesAdvertisement));
                }
    
                // if the player has permission for the claim and he's placing UNDER
                // the claim
                if (block.getPosition().getY() <= claim.lesserBoundaryCorner.getBlockY() && claim.allowBuild(player.get(), block.getState().getType()) == null) {
                    // extend the claim downward
                    this.dataStore.extendClaim(claim, block.getPosition().getY() - GriefPrevention.instance.config_claims_claimsExtendIntoGroundDistance);
                }
    
                // allow for a build warning in the future
                playerData.warnedAboutBuildingOutsideClaims = false;
            }
    
            // FEATURE: automatically create a claim when a player who has no claims
            // places a chest
    
            // otherwise if there's no claim, the player is placing a chest, and new
            // player automatic claims are enabled
            else if (block.getState().getType() == BlockTypes.CHEST && GriefPrevention.instance.config_claims_automaticClaimsForNewPlayersRadius > -1
                    && GriefPrevention.instance.claimsEnabledForWorld(block.getLocation().get().getExtent())) {
                // if the chest is too deep underground, don't create the claim and
                // explain why
                if (GriefPrevention.instance.config_claims_preventTheft && block.getPosition().getY() < GriefPrevention.instance.config_claims_maxDepth) {
                    GriefPrevention.sendMessage(player.get(), Texts.of(TextMode.Warn, Messages.TooDeepToClaim));
                    return;
                }
    
                int radius = GriefPrevention.instance.config_claims_automaticClaimsForNewPlayersRadius;
    
                // if the player doesn't have any claims yet, automatically create a
                // claim centered at the chest
                if (playerData.getClaims().size() == 0) {
                    // radius == 0 means protect ONLY the chest
                    if (GriefPrevention.instance.config_claims_automaticClaimsForNewPlayersRadius == 0) {
                        this.dataStore.createClaim(block.getLocation().get().getExtent(), block.getPosition().getX(), block.getPosition().getX(), block.getPosition().getY(), block.getPosition().getY(), block.getPosition().getZ(), block.getPosition().getZ(),
                                player.get().getUniqueId(), null, null, player.get());
                        GriefPrevention.sendMessage(player.get(), Texts.of(TextMode.Success, Messages.ChestClaimConfirmation));
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
                                block.getPosition().getY() - GriefPrevention.instance.config_claims_claimsExtendIntoGroundDistance, block.getPosition().getY(),
                                block.getPosition().getZ() - radius, block.getPosition().getZ() + radius,
                                player.get().getUniqueId(),
                                null, null,
                                player.get()).succeeded) {
                            radius--;
                        }
    
                        // notify and explain to player
                        GriefPrevention.sendMessage(player.get(), TextMode.Success, Messages.AutomaticClaimNotification);
    
                        // show the player the protected area
                        Claim newClaim = this.dataStore.getClaimAt(block.getLocation().get(), false, null);
                        Visualization visualization = Visualization.FromClaim(newClaim, block.getPosition().getY(), VisualizationType.Claim, player.get().getLocation());
                        Visualization.Apply(player.get(), visualization);
                    }
    
                    GriefPrevention.sendMessage(player.get(), TextMode.Instr, Messages.SurvivalBasicsVideo2, DataStore.SURVIVAL_VIDEO_URL);
                }
    
                // check to see if this chest is in a claim, and warn when it isn't
                if (GriefPrevention.instance.config_claims_preventTheft
                        && this.dataStore.getClaimAt(block.getLocation().get(), false, playerData.lastClaim) == null) {
                    GriefPrevention.sendMessage(player.get(), TextMode.Warn, Messages.UnprotectedChestWarning);
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
                if (!playerData.warnedAboutBuildingOutsideClaims && !player.get().hasPermission("griefprevention.adminclaims")
                        && ((playerData.lastClaim == null && playerData.getClaims().size() == 0)
                                || (playerData.lastClaim != null && playerData.lastClaim.isNear(player.get().getLocation(), 15)))) {
                    Long now = null;
                    if (playerData.buildWarningTimestamp == null || (now = System.currentTimeMillis()) - playerData.buildWarningTimestamp > 600000) // 10
                                                                                                                                                    // minute
                                                                                                                                                    // cooldown
                    {
                        GriefPrevention.sendMessage(player.get(), TextMode.Warn, Messages.BuildingOutsideClaims);
                        playerData.warnedAboutBuildingOutsideClaims = true;
    
                        if (now == null)
                            now = System.currentTimeMillis();
                        playerData.buildWarningTimestamp = now;
    
                        if (playerData.getClaims().size() < 2) {
                            GriefPrevention.sendMessage(player.get(), TextMode.Instr, Messages.SurvivalBasicsVideo2, DataStore.SURVIVAL_VIDEO_URL);
                        }
    
                        if (playerData.lastClaim != null) {
                            Visualization visualization =
                                    Visualization.FromClaim(playerData.lastClaim, block.getPosition().getY(), VisualizationType.Claim, player.get().getLocation());
                            Visualization.Apply(player.get(), visualization);
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
                GriefPrevention.sendMessage(player.get(), TextMode.Warn, Messages.NoTNTDamageAboveSeaLevel);
            }
    
            // warn players about disabled pistons outside of land claims
            if (GriefPrevention.instance.config_pistonsInClaimsOnly &&
                    (block.getState().getType() == BlockTypes.PISTON || block.getState().getType() == BlockTypes.STICKY_PISTON) &&
                    claim == null) {
                GriefPrevention.sendMessage(player.get(), TextMode.Warn, Messages.NoPistonsOutsideClaims);
            }
        }
    }

    // blocks "pushing" other players' blocks around (pistons)
    /*@EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        // pushing down is ALWAYS safe
        if (event.getDirection() == BlockFace.DOWN)
            return;

        // don't track in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(event.getBlock().getWorld()))
            return;

        Block pistonBlock = event.getBlock();
        List<Block> blocks = event.getBlocks();

        // if no blocks moving, then only check to make sure we're not pushing
        // into a claim from outside
        // this avoids pistons breaking non-solids just inside a claim, like
        // torches, doors, and touchplates
        if (blocks.size() == 0) {
            Block invadedBlock = pistonBlock.getRelative(event.getDirection());

            // pushing "air" is harmless
            if (invadedBlock.getType() == Material.AIR)
                return;

            if (this.dataStore.getClaimAt(pistonBlock.getLocation(), false, null) == null &&
                    this.dataStore.getClaimAt(invadedBlock.getLocation(), false, null) != null) {
                event.setCancelled(true);
            }

            return;
        }

        // who owns the piston, if anyone?
        String pistonClaimOwnerName = "_";
        Claim claim = this.dataStore.getClaimAt(event.getBlock().getLocation(), false, null);
        if (claim != null)
            pistonClaimOwnerName = claim.getOwnerName();

        // if pistons are limited to same-claim block movement
        if (GriefPrevention.instance.config_pistonsInClaimsOnly) {
            // if piston is not in a land claim, cancel event
            if (claim == null) {
                event.setCancelled(true);
                return;
            }

            for (Block pushedBlock : event.getBlocks()) {
                // if pushing blocks located outside the land claim it lives in,
                // cancel the event
                if (!claim.contains(pushedBlock.getLocation(), false, false)) {
                    event.setCancelled(true);
                    return;
                }

                // if pushing a block inside the claim out of the claim, cancel
                // the event
                // reason: could push into another land claim, don't want to
                // spend CPU checking for that
                // reason: push ice out, place torch, get water outside the
                // claim
                if (!claim.contains(pushedBlock.getRelative(event.getDirection()).getLocation(), false, false)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // otherwise, consider ownership of piston and EACH pushed block
        else {
            // which blocks are being pushed?
            Claim cachedClaim = claim;
            for (int i = 0; i < blocks.size(); i++) {
                // if ANY of the pushed blocks are owned by someone other than
                // the piston owner, cancel the event
                Block block = blocks.get(i);
                claim = this.dataStore.getClaimAt(block.getLocation(), false, cachedClaim);
                if (claim != null) {
                    cachedClaim = claim;
                    if (!claim.getOwnerName().equals(pistonClaimOwnerName)) {
                        event.setCancelled(true);
                        event.getBlock().getWorld().createExplosion(event.getBlock().getLocation(), 0);
                        event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(event.getBlock().getType()));
                        event.getBlock().setType(Material.AIR);
                        return;
                    }
                }
            }

            // if any of the blocks are being pushed into a claim from outside,
            // cancel the event
            for (int i = 0; i < blocks.size(); i++) {
                Block block = blocks.get(i);
                Claim originalClaim = this.dataStore.getClaimAt(block.getLocation(), false, cachedClaim);
                String originalOwnerName = "";
                if (originalClaim != null) {
                    cachedClaim = originalClaim;
                    originalOwnerName = originalClaim.getOwnerName();
                }

                Claim newClaim = this.dataStore.getClaimAt(block.getRelative(event.getDirection()).getLocation(), false, cachedClaim);
                String newOwnerName = "";
                if (newClaim != null) {
                    newOwnerName = newClaim.getOwnerName();
                }

                // if pushing this block will change ownership, cancel the event
                // and take away the piston (for performance reasons)
                if (!newOwnerName.equals(originalOwnerName) && !newOwnerName.isEmpty()) {
                    event.setCancelled(true);
                    event.getBlock().getWorld().createExplosion(event.getBlock().getLocation(), 0);
                    event.getBlock().getWorld().dropItem(event.getBlock().getLocation(), new ItemStack(event.getBlock().getType()));
                    event.getBlock().setType(Material.AIR);
                    return;
                }
            }
        }
    }

    // blocks theft by pulling blocks out of a claim (again pistons)
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        // pulling up is always safe
        if (event.getDirection() == BlockFace.UP)
            return;

        try {
            // don't track in worlds where claims are not enabled
            if (!GriefPrevention.instance.claimsEnabledForWorld(event.getBlock().getWorld()))
                return;

            // if pistons limited to only pulling blocks which are in the same
            // claim the piston is in
            if (GriefPrevention.instance.config_pistonsInClaimsOnly) {
                // if piston not in a land claim, cancel event
                Claim pistonClaim = this.dataStore.getClaimAt(event.getBlock().getLocation(), false, null);
                if (pistonClaim == null) {
                    event.setCancelled(true);
                    return;
                }

                for (Block movedBlock : event.getBlocks()) {
                    // if pulled block isn't in the same land claim, cancel the
                    // event
                    if (!pistonClaim.contains(movedBlock.getLocation(), false, false)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }

            // otherwise, consider ownership of both piston and block
            else {
                // who owns the piston, if anyone?
                String pistonOwnerName = "_";
                Location pistonLocation = event.getBlock().getLocation();
                Claim pistonClaim = this.dataStore.getClaimAt(pistonLocation, false, null);
                if (pistonClaim != null)
                    pistonOwnerName = pistonClaim.getOwnerName();

                String movingBlockOwnerName = "_";
                for (Block movedBlock : event.getBlocks()) {
                    // who owns the moving block, if anyone?
                    Claim movingBlockClaim = this.dataStore.getClaimAt(movedBlock.getLocation(), false, pistonClaim);
                    if (movingBlockClaim != null)
                        movingBlockOwnerName = movingBlockClaim.getOwnerName();

                    // if there are owners for the blocks, they must be the same
                    // player
                    // otherwise cancel the event
                    if (!pistonOwnerName.equals(movingBlockOwnerName)) {
                        event.setCancelled(true);
                    }
                }
            }
        } catch (NoSuchMethodError exception) {
            GriefPrevention.AddLogEntry(
                    "Your server is running an outdated version of 1.8 which has a griefing vulnerability.  Update your server (reruns buildtools.jar to get an updated server JAR file) to ensure players can't steal claimed blocks using pistons.");
        }
    }

    // blocks are ignited ONLY by flint and steel (not by being near lava, open
    // flames, etc), unless configured otherwise
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockIgnite(BlockIgniteEvent igniteEvent) {
        // don't track in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(igniteEvent.getBlock().getWorld()))
            return;

        if (!GriefPrevention.instance.config_fireSpreads && igniteEvent.getCause() != IgniteCause.FLINT_AND_STEEL
                && igniteEvent.getCause() != IgniteCause.LIGHTNING) {
            igniteEvent.setCancelled(true);
        }
    }

    // fire doesn't spread unless configured to, but other blocks still do
    // (mushrooms and vines, for example)
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockSpread(BlockSpreadEvent spreadEvent) {
        if (spreadEvent.getSource().getType() != Material.FIRE)
            return;

        // don't track in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(spreadEvent.getBlock().getWorld()))
            return;

        if (!GriefPrevention.instance.config_fireSpreads) {
            spreadEvent.setCancelled(true);

            Block underBlock = spreadEvent.getSource().getRelative(BlockFace.DOWN);
            if (underBlock.getType() != Material.NETHERRACK) {
                spreadEvent.getSource().setType(Material.AIR);
            }

            return;
        }

        // never spread into a claimed area, regardless of settings
        if (this.dataStore.getClaimAt(spreadEvent.getBlock().getLocation(), false, null) != null) {
            spreadEvent.setCancelled(true);

            // if the source of the spread is not fire on netherrack, put out
            // that source fire to save cpu cycles
            Block source = spreadEvent.getSource();
            if (source.getRelative(BlockFace.DOWN).getType() != Material.NETHERRACK) {
                source.setType(Material.AIR);
            }
        }
    }

    // blocks are not destroyed by fire, unless configured to do so
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBurn(BlockBurnEvent burnEvent) {
        // don't track in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(burnEvent.getBlock().getWorld()))
            return;

        if (!GriefPrevention.instance.config_fireDestroys) {
            burnEvent.setCancelled(true);
            Block block = burnEvent.getBlock();
            Block[] adjacentBlocks = new Block[] {
                    block.getRelative(BlockFace.UP),
                    block.getRelative(BlockFace.DOWN),
                    block.getRelative(BlockFace.NORTH),
                    block.getRelative(BlockFace.SOUTH),
                    block.getRelative(BlockFace.EAST),
                    block.getRelative(BlockFace.WEST)
            };

            // pro-actively put out any fires adjacent the burning block, to
            // reduce future processing here
            for (int i = 0; i < adjacentBlocks.length; i++) {
                Block adjacentBlock = adjacentBlocks[i];
                if (adjacentBlock.getType() == Material.FIRE && adjacentBlock.getRelative(BlockFace.DOWN).getType() != Material.NETHERRACK) {
                    adjacentBlock.setType(Material.AIR);
                }
            }

            Block aboveBlock = block.getRelative(BlockFace.UP);
            if (aboveBlock.getType() == Material.FIRE) {
                aboveBlock.setType(Material.AIR);
            }
            return;
        }

        // never burn claimed blocks, regardless of settings
        if (this.dataStore.getClaimAt(burnEvent.getBlock().getLocation(), false, null) != null) {
            burnEvent.setCancelled(true);
        }
    }

    // ensures fluids don't flow into land claims from outside
    private Claim lastSpreadClaim = null;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onBlockFromTo(BlockFromToEvent spreadEvent) {
        // always allow fluids to flow straight down
        if (spreadEvent.getFace() == BlockFace.DOWN)
            return;

        // don't track in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(spreadEvent.getBlock().getWorld()))
            return;

        // where to?
        Block toBlock = spreadEvent.getToBlock();
        Location toLocation = toBlock.getLocation();
        Claim toClaim = this.dataStore.getClaimAt(toLocation, false, lastSpreadClaim);

        // if into a land claim, it must be from the same land claim
        if (toClaim != null) {
            this.lastSpreadClaim = toClaim;
            if (!toClaim.contains(spreadEvent.getBlock().getLocation(), false, true)) {
                // exception: from parent into subdivision
                if (toClaim.parent == null || !toClaim.parent.contains(spreadEvent.getBlock().getLocation(), false, false)) {
                    spreadEvent.setCancelled(true);
                }
            }
        }

        // otherwise if creative mode world, don't flow
        else if (GriefPrevention.instance.creativeRulesApply(toLocation)) {
            spreadEvent.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onForm(BlockFormEvent event) {
        Block block = event.getBlock();
        Location location = block.getLocation();

        if (GriefPrevention.instance.creativeRulesApply(location)) {
            Material type = block.getType();
            if (type == Material.COBBLESTONE || type == Material.OBSIDIAN || type == Material.STATIONARY_LAVA || type == Material.STATIONARY_WATER) {
                Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
                if (claim == null) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // ensures dispensers can't be used to dispense a block(like water or lava)
    // or item across a claim boundary
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onDispense(BlockDispenseEvent dispenseEvent) {
        // don't track in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(dispenseEvent.getBlock().getWorld()))
            return;

        // from where?
        Block fromBlock = dispenseEvent.getBlock();
        Dispenser dispenser = new Dispenser(fromBlock.getType(), fromBlock.getData());

        // to where?
        Block toBlock = fromBlock.getRelative(dispenser.getFacing());
        Claim fromClaim = this.dataStore.getClaimAt(fromBlock.getLocation(), false, null);
        Claim toClaim = this.dataStore.getClaimAt(toBlock.getLocation(), false, fromClaim);

        // into wilderness is NOT OK in creative mode worlds
        Material materialDispensed = dispenseEvent.getItem().getType();
        if ((materialDispensed == Material.WATER_BUCKET || materialDispensed == Material.LAVA_BUCKET)
                && GriefPrevention.instance.creativeRulesApply(dispenseEvent.getBlock().getLocation()) && toClaim == null) {
            dispenseEvent.setCancelled(true);
            return;
        }

        // wilderness to wilderness is OK
        if (fromClaim == null && toClaim == null)
            return;

        // within claim is OK
        if (fromClaim == toClaim)
            return;

        // everything else is NOT OK
        dispenseEvent.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTreeGrow(StructureGrowEvent growEvent) {
        // only take these potentially expensive steps if configured to do so
        if (!GriefPrevention.instance.config_limitTreeGrowth)
            return;

        // don't track in worlds where claims are not enabled
        if (!GriefPrevention.instance.claimsEnabledForWorld(growEvent.getWorld()))
            return;

        Location rootLocation = growEvent.getLocation();
        Claim rootClaim = this.dataStore.getClaimAt(rootLocation, false, null);
        String rootOwnerName = null;

        // who owns the spreading block, if anyone?
        if (rootClaim != null) {
            // tree growth in subdivisions is dependent on who owns the top
            // level claim
            if (rootClaim.parent != null)
                rootClaim = rootClaim.parent;

            // if an administrative claim, just let the tree grow where it wants
            if (rootClaim.isAdminClaim())
                return;

            // otherwise, note the owner of the claim
            rootOwnerName = rootClaim.getOwnerName();
        }

        // for each block growing
        for (int i = 0; i < growEvent.getBlocks().size(); i++) {
            BlockState block = growEvent.getBlocks().get(i);
            Claim blockClaim = this.dataStore.getClaimAt(block.getLocation(), false, rootClaim);

            // if it's growing into a claim
            if (blockClaim != null) {
                // if there's no owner for the new tree, or the owner for the
                // new tree is different from the owner of the claim
                if (rootOwnerName == null || !rootOwnerName.equals(blockClaim.getOwnerName())) {
                    growEvent.getBlocks().remove(i--);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        // prevent hoppers from picking-up items dropped by players on death

        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof HopperMinecart || holder instanceof Hopper) {
            Item item = event.getItem();
            List<MetadataValue> data = item.getMetadata("GP_ITEMOWNER");

            // if this is marked as belonging to a player
            if (data != null && data.size() > 0) {
                // don't allow the pickup
                event.setCancelled(true);
            }
        }
    }*/
}
