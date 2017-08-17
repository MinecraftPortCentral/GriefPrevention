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
package me.ryanhamshire.griefprevention;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.api.data.PlayerData;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.command.CommandHelper;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.configuration.PlayerStorageData;
import me.ryanhamshire.griefprevention.permission.GPOptionHandler;
import me.ryanhamshire.griefprevention.permission.GPOptions;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import me.ryanhamshire.griefprevention.util.PlayerUtils;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.common.SpongeImpl;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

//holds all of GriefPrevention's player-tied data
public class GPPlayerData implements PlayerData {

    // the player's ID
    public UUID playerID;
    public WorldProperties worldProperties;
    private WeakReference<Subject> playerSubject;

    // the player's claims
    private List<Claim> claimList;

    private PlayerStorageData playerStorage;

    // where this player was the last time we checked on him for earning claim blocks
    public Location<World> lastAfkCheckLocation;

    // what "mode" the shovel is in determines what it will do when it's used
    public ShovelMode shovelMode = ShovelMode.Basic;

    // radius for restore nature fill mode
    public int fillRadius = 0;

    // last place the player used the shovel, useful in creating and resizing claims,
    // because the player must use the shovel twice in those instances
    public Location<World> lastShovelLocation;
    public Location<World> endShovelLocation;
    public Location<World> lastValidInspectLocation;

    // used for nature restores
    public Chunk fillerChunk;
    public BlockSnapshot[][][] fillerBlocks;

    // the claim this player is currently resizing
    public GPClaim claimResizing;

    // the claim this player is currently subdividing
    public GPClaim claimSubdividing;

    // whether or not the player has a pending /trapped rescue
    public boolean pendingTrapped = false;

    // whether this player was recently warned about building outside land claims
    public boolean warnedAboutBuildingOutsideClaims = false;

    // timestamp of last death, for use in preventing death message spam
    public long lastDeathTimeStamp = 0;

    // whether the player was kicked (set and used during logout)
    public boolean wasKicked = false;

    // spam when the player last logged into the server
    @SuppressWarnings("unused")
    private Date lastLogin;

    // the player's last chat message, or slash command complete with parameters
    public String lastMessage = "";

    // last time the player sent a chat message or used a monitored slash command
    public Date lastMessageTimestamp = new Date();

    // number of consecutive "spams"
    public int spamCount = 0;

    // whether the player recently received a warning
    public boolean spamWarned = false;

    // visualization
    public List<Transaction<BlockSnapshot>> visualBlocks;
    public UUID visualClaimId;
    public Task visualRevertTask;

    // anti-camping pvp protection
    public boolean pvpImmune = false;
    public long lastSpawn = 0;

    // ignore claims mode
    public boolean ignoreClaims = false;

    public boolean debugClaimPermissions = false;
    // the last claim this player was in, that we know of
    public WeakReference<GPClaim> lastClaim = new WeakReference<>(null);

    // pvp
    public long lastPvpTimestamp = 0;
    public String lastPvpPlayer = "";

    // safety confirmation for deleting multi-subdivision claims
    public boolean warnedAboutMajorDeletion = false;

    // town
    public boolean inTown = false;
    public boolean townChat = false;

    public boolean ignoreActiveContexts = false;
    public InetAddress ipAddress;

    // whether or not this player has received a message about unlocking death
    // drops since his last death
    public boolean receivedDropUnlockAdvertisement = false;

    // whether or not this player's dropped items (on death) are unlocked for
    // other players to pick up
    public boolean dropsAreUnlocked = true;

    // message to send to player after he respawns
    public Text messageOnRespawn;

    // player which a pet will be given to when it's right-clicked
    public User petGiveawayRecipient;

    // timestamp for last "you're building outside your land claims" message
    public Long buildWarningTimestamp;

    // spot where a player can't talk, used to mute new players until they've moved a little this is an anti-bot strategy.
    public Location<World> noChatLocation;

    // ignore list true means invisible (admin-forced ignore), false means player-created ignore
    public ConcurrentHashMap<UUID, Boolean> ignoredPlayers = new ConcurrentHashMap<UUID, Boolean>();
    public boolean ignoreListChanged = false;

    // profanity warning, once per play session
    public boolean profanityWarned = false;

    public Tristate lastInteractItemEntityResult = Tristate.UNDEFINED;
    public Tristate lastInteractItemBlockResult = Tristate.UNDEFINED;
    public boolean lastInteractResult = false;
    public int lastTickCounter = 0;
    public UUID lastInteractClaim = GriefPreventionPlugin.PUBLIC_UUID;

    // collide event cache
    public int lastCollideEntityId = 0;
    public boolean lastCollideEntityResult = false;

    private String playerName;

    // cached option values
    public double optionAbandonReturnRatioBasic = GPOptions.DEFAULT_ABANDON_RETURN_RATIO_BASIC;
    public double optionAbandonReturnRatioTown = GPOptions.DEFAULT_ABANDON_RETURN_RATIO_TOWN;
    public int optionBlocksAccruedPerHour = GPOptions.DEFAULT_BLOCKS_ACCRUED_PER_HOUR;
    public int optionCreateClaimLimitBasic = GPOptions.DEFAULT_CREATE_CLAIM_LIMIT_BASIC;
    public int optionCreateClaimLimitSubdivision = GPOptions.DEFAULT_CREATE_CLAIM_LIMIT_SUBDIVISION;
    public int optionCreateClaimLimitTown = GPOptions.DEFAULT_CREATE_CLAIM_LIMIT_TOWN;
    public int optionInitialClaimBlocks = GPOptions.DEFAULT_INITIAL_CLAIM_BLOCKS;
    public int optionMaxAccruedBlocks = GPOptions.DEFAULT_MAX_ACCRUED_BLOCKS;
    public int optionMaxClaimSizeBasicX = GPOptions.DEFAULT_MAX_CLAIM_SIZE_BASIC_X;
    public int optionMaxClaimSizeBasicY = GPOptions.DEFAULT_MAX_CLAIM_SIZE_BASIC_Y;
    public int optionMaxClaimSizeBasicZ = GPOptions.DEFAULT_MAX_CLAIM_SIZE_BASIC_Z;
    public int optionMaxClaimSizeTownX = GPOptions.DEFAULT_MAX_CLAIM_SIZE_TOWN_X;
    public int optionMaxClaimSizeTownY = GPOptions.DEFAULT_MAX_CLAIM_SIZE_TOWN_Y;
    public int optionMaxClaimSizeTownZ = GPOptions.DEFAULT_MAX_CLAIM_SIZE_TOWN_Z;
    public int optionMaxClaimSizeSubX = GPOptions.DEFAULT_MAX_CLAIM_SIZE_SUBDIVISION_X;
    public int optionMaxClaimSizeSubY = GPOptions.DEFAULT_MAX_CLAIM_SIZE_SUBDIVISION_Y;
    public int optionMaxClaimSizeSubZ = GPOptions.DEFAULT_MAX_CLAIM_SIZE_SUBDIVISION_Z;
    public int optionMinClaimSizeBasicX = GPOptions.DEFAULT_MIN_CLAIM_SIZE_BASIC_X;
    public int optionMinClaimSizeBasicY = GPOptions.DEFAULT_MIN_CLAIM_SIZE_BASIC_Y;
    public int optionMinClaimSizeBasicZ = GPOptions.DEFAULT_MIN_CLAIM_SIZE_BASIC_Z;
    public int optionMinClaimSizeTownX = GPOptions.DEFAULT_MIN_CLAIM_SIZE_TOWN_X;
    public int optionMinClaimSizeTownY = GPOptions.DEFAULT_MIN_CLAIM_SIZE_TOWN_Y;
    public int optionMinClaimSizeTownZ = GPOptions.DEFAULT_MIN_CLAIM_SIZE_TOWN_Z;
    public int optionClaimExpirationChest = GPOptions.DEFAULT_CLAIM_EXPIRATION_CHEST;
    public int optionClaimExpirationBasic = GPOptions.DEFAULT_CLAIM_EXPIRATION_BASIC;
    public int optionClaimExpirationSubdivision = GPOptions.DEFAULT_CLAIM_EXPIRATION_SUBDIVISION;
    public int optionClaimExpirationTown = GPOptions.DEFAULT_CLAIM_EXPIRATION_TOWN;
    public int optionTaxExpirationBasic = GPOptions.DEFAULT_TAX_EXPIRATION_BASIC;
    public int optionTaxExpirationSubdivision = GPOptions.DEFAULT_TAX_EXPIRATION_SUBDIVISION;
    public int optionTaxExpirationTown = GPOptions.DEFAULT_TAX_EXPIRATION_TOWN;
    public double optionTaxRateBasic = GPOptions.DEFAULT_TAX_RATE_BASIC;
    public double optionTaxRateSubdivision = GPOptions.DEFAULT_TAX_RATE_SUBDIVISION;
    public double optionTaxRateTown = GPOptions.DEFAULT_TAX_RATE_TOWN;
    public double optionTaxRateTownBasic = GPOptions.DEFAULT_TAX_RATE_TOWN_BASIC;
    public double optionTaxRateTownSubdivision = GPOptions.DEFAULT_TAX_RATE_TOWN_SUBDIVISION;
    public Map<String, Double> optionMap = Maps.newHashMap();

    // cached permission values
    public boolean canManageAdminClaims = false;
    public boolean canManageWilderness = false;
    public boolean ignoreAdminClaims = false;
    public boolean ignoreBasicClaims = false;
    public boolean ignoreTowns = false;
    public boolean ignoreWilderness = false;

    public boolean dataInitialized = false;
    public boolean showVisualFillers = true;

    public GPPlayerData(WorldProperties worldProperties, UUID playerUniqueId, PlayerStorageData playerStorage, GriefPreventionConfig<?> activeConfig, List<Claim> claims) {
        this.worldProperties = worldProperties;
        this.playerID = playerUniqueId;
        this.playerStorage = playerStorage;
        this.claimList = claims;
        this.refreshPlayerOptions();
    }

    // Run async
    public void refreshPlayerOptions() {
        GriefPreventionPlugin.instance.executor.execute(() -> {
            if (this.playerSubject == null || this.playerSubject.get() == null) {
                Subject subject = GriefPreventionPlugin.instance.permissionService.getUserSubjects().get(this.playerID.toString());
                this.playerSubject = new WeakReference<>(subject);
            }
            final Subject subject = this.playerSubject.get();
            // options
            this.optionAbandonReturnRatioTown = PlayerUtils.getOptionDoubleValue(subject, GPOptions.ABANDON_RETURN_RATIO_TOWN, this.optionAbandonReturnRatioTown);
            this.optionAbandonReturnRatioBasic = PlayerUtils.getOptionDoubleValue(subject, GPOptions.ABANDON_RETURN_RATIO_BASIC, this.optionAbandonReturnRatioBasic);
            this.optionBlocksAccruedPerHour = PlayerUtils.getOptionIntValue(subject, GPOptions.BLOCKS_ACCRUED_PER_HOUR, this.optionBlocksAccruedPerHour);
            this.optionClaimExpirationChest = PlayerUtils.getOptionIntValue(subject, GPOptions.CLAIM_EXPIRATION_CHEST, this.optionClaimExpirationChest);
            this.optionCreateClaimLimitBasic = PlayerUtils.getOptionIntValue(subject, GPOptions.CREATE_CLAIM_LIMIT_BASIC, this.optionCreateClaimLimitBasic);
            this.optionCreateClaimLimitSubdivision = PlayerUtils.getOptionIntValue(subject, GPOptions.CREATE_CLAIM_LIMIT_SUBDIVISION, this.optionCreateClaimLimitSubdivision);
            this.optionCreateClaimLimitTown = PlayerUtils.getOptionIntValue(subject, GPOptions.CREATE_CLAIM_LIMIT_TOWN, this.optionCreateClaimLimitTown);
            this.optionInitialClaimBlocks = PlayerUtils.getOptionIntValue(subject, GPOptions.INITIAL_CLAIM_BLOCKS, this.optionInitialClaimBlocks);
            this.optionMaxAccruedBlocks = PlayerUtils.getOptionIntValue(subject, GPOptions.MAX_ACCRUED_BLOCKS, this.optionMaxAccruedBlocks);
            this.optionMaxClaimSizeBasicX = PlayerUtils.getOptionIntValue(subject, GPOptions.MAX_CLAIM_SIZE_BASIC_X, this.optionMaxClaimSizeBasicX);
            this.optionMaxClaimSizeBasicY = PlayerUtils.getOptionIntValue(subject, GPOptions.MAX_CLAIM_SIZE_BASIC_Y, this.optionMaxClaimSizeBasicY);
            this.optionMaxClaimSizeBasicZ = PlayerUtils.getOptionIntValue(subject, GPOptions.MAX_CLAIM_SIZE_BASIC_Z, this.optionMaxClaimSizeBasicZ);
            this.optionMaxClaimSizeTownX = PlayerUtils.getOptionIntValue(subject, GPOptions.MAX_CLAIM_SIZE_TOWN_X, this.optionMaxClaimSizeTownX);
            this.optionMaxClaimSizeTownY = PlayerUtils.getOptionIntValue(subject, GPOptions.MAX_CLAIM_SIZE_TOWN_Y, this.optionMaxClaimSizeTownY);
            this.optionMaxClaimSizeTownZ = PlayerUtils.getOptionIntValue(subject, GPOptions.MAX_CLAIM_SIZE_TOWN_Z, this.optionMaxClaimSizeTownZ);
            this.optionMaxClaimSizeSubX = PlayerUtils.getOptionIntValue(subject, GPOptions.MAX_CLAIM_SIZE_SUBDIVISION_X, this.optionMaxClaimSizeSubX);
            this.optionMaxClaimSizeSubY = PlayerUtils.getOptionIntValue(subject, GPOptions.MAX_CLAIM_SIZE_SUBDIVISION_Y, this.optionMaxClaimSizeSubY);
            this.optionMaxClaimSizeSubZ = PlayerUtils.getOptionIntValue(subject, GPOptions.MAX_CLAIM_SIZE_SUBDIVISION_Z, this.optionMaxClaimSizeSubZ);
            this.optionMinClaimSizeBasicX = PlayerUtils.getOptionIntValue(subject, GPOptions.MIN_CLAIM_SIZE_BASIC_X, this.optionMinClaimSizeBasicX);
            this.optionMinClaimSizeBasicY = PlayerUtils.getOptionIntValue(subject, GPOptions.MIN_CLAIM_SIZE_BASIC_Y, this.optionMinClaimSizeBasicY);
            this.optionMinClaimSizeBasicZ = PlayerUtils.getOptionIntValue(subject, GPOptions.MIN_CLAIM_SIZE_BASIC_Z, this.optionMinClaimSizeBasicZ);
            this.optionMinClaimSizeTownX = PlayerUtils.getOptionIntValue(subject, GPOptions.MIN_CLAIM_SIZE_TOWN_X, this.optionMinClaimSizeTownX);
            this.optionMinClaimSizeTownY = PlayerUtils.getOptionIntValue(subject, GPOptions.MIN_CLAIM_SIZE_TOWN_Y, this.optionMinClaimSizeTownY);
            this.optionMinClaimSizeTownZ = PlayerUtils.getOptionIntValue(subject, GPOptions.MIN_CLAIM_SIZE_TOWN_Z, this.optionMinClaimSizeTownZ);
            this.optionClaimExpirationChest = PlayerUtils.getOptionIntValue(subject, GPOptions.CLAIM_EXPIRATION_CHEST, this.optionClaimExpirationChest);
            this.optionClaimExpirationBasic = PlayerUtils.getOptionIntValue(subject, GPOptions.CLAIM_EXPIRATION_BASIC, this.optionClaimExpirationBasic);
            this.optionClaimExpirationTown = PlayerUtils.getOptionIntValue(subject, GPOptions.TAX_EXPIRATION_TOWN, this.optionClaimExpirationTown);
            this.optionTaxExpirationBasic = PlayerUtils.getOptionIntValue(subject, GPOptions.TAX_EXPIRATION_BASIC, this.optionTaxExpirationBasic);
            this.optionTaxExpirationSubdivision = PlayerUtils.getOptionIntValue(subject, GPOptions.TAX_EXPIRATION_BASIC, this.optionTaxExpirationSubdivision);
            this.optionTaxExpirationTown = PlayerUtils.getOptionIntValue(subject, GPOptions.TAX_EXPIRATION_BASIC, this.optionTaxExpirationTown);
            this.optionTaxRateBasic = PlayerUtils.getOptionDoubleValue(subject, GPOptions.TAX_RATE_BASIC, this.optionTaxRateBasic);
            this.optionTaxRateSubdivision = PlayerUtils.getOptionDoubleValue(subject, GPOptions.TAX_RATE_BASIC, this.optionTaxRateSubdivision);
            this.optionTaxRateTown = PlayerUtils.getOptionDoubleValue(subject, GPOptions.TAX_RATE_TOWN, this.optionTaxRateTown);
            this.optionTaxRateTownBasic = PlayerUtils.getOptionDoubleValue(subject, GPOptions.TAX_RATE_TOWN_BASIC, this.optionTaxRateTownBasic);
            this.optionTaxRateTownSubdivision = PlayerUtils.getOptionDoubleValue(subject, GPOptions.TAX_RATE_TOWN_BASIC, this.optionTaxRateTownSubdivision);
            this.optionMap.put(GPOptions.ABANDON_RETURN_RATIO_TOWN, this.optionAbandonReturnRatioTown);
            this.optionMap.put(GPOptions.ABANDON_RETURN_RATIO_BASIC, this.optionAbandonReturnRatioBasic);
            this.optionMap.put(GPOptions.BLOCKS_ACCRUED_PER_HOUR, (double) this.optionBlocksAccruedPerHour);
            this.optionMap.put(GPOptions.CLAIM_EXPIRATION_CHEST, (double) this.optionClaimExpirationChest);
            this.optionMap.put(GPOptions.CREATE_CLAIM_LIMIT_BASIC, (double) this.optionCreateClaimLimitBasic);
            this.optionMap.put(GPOptions.CREATE_CLAIM_LIMIT_SUBDIVISION, (double) this.optionCreateClaimLimitSubdivision);
            this.optionMap.put(GPOptions.CREATE_CLAIM_LIMIT_TOWN, (double) this.optionCreateClaimLimitTown);
            this.optionMap.put(GPOptions.INITIAL_CLAIM_BLOCKS, (double) this.optionInitialClaimBlocks);
            this.optionMap.put(GPOptions.MAX_ACCRUED_BLOCKS, (double) this.optionMaxAccruedBlocks);
            this.optionMap.put(GPOptions.MAX_CLAIM_SIZE_BASIC_X, (double) this.optionMaxClaimSizeBasicX);
            this.optionMap.put(GPOptions.MAX_CLAIM_SIZE_BASIC_Y, (double) this.optionMaxClaimSizeBasicY);
            this.optionMap.put(GPOptions.MAX_CLAIM_SIZE_BASIC_Z, (double) this.optionMaxClaimSizeBasicZ);
            this.optionMap.put(GPOptions.MAX_CLAIM_SIZE_SUBDIVISION_X, (double) this.optionMaxClaimSizeSubX);
            this.optionMap.put(GPOptions.MAX_CLAIM_SIZE_SUBDIVISION_Y, (double) this.optionMaxClaimSizeSubY);
            this.optionMap.put(GPOptions.MAX_CLAIM_SIZE_SUBDIVISION_Z, (double) this.optionMaxClaimSizeSubZ);
            this.optionMap.put(GPOptions.MAX_CLAIM_SIZE_TOWN_X, (double) this.optionMaxClaimSizeTownX);
            this.optionMap.put(GPOptions.MAX_CLAIM_SIZE_TOWN_Y, (double) this.optionMaxClaimSizeTownY);
            this.optionMap.put(GPOptions.MAX_CLAIM_SIZE_TOWN_Z, (double) this.optionMaxClaimSizeTownZ);
            this.optionMap.put(GPOptions.MIN_CLAIM_SIZE_BASIC_X, (double) this.optionMinClaimSizeBasicX);
            this.optionMap.put(GPOptions.MIN_CLAIM_SIZE_BASIC_Y, (double) this.optionMinClaimSizeBasicY);
            this.optionMap.put(GPOptions.MIN_CLAIM_SIZE_BASIC_Z, (double) this.optionMinClaimSizeBasicZ);
            this.optionMap.put(GPOptions.MIN_CLAIM_SIZE_TOWN_X, (double) this.optionMinClaimSizeTownX);
            this.optionMap.put(GPOptions.MIN_CLAIM_SIZE_TOWN_Y, (double) this.optionMinClaimSizeTownY);
            this.optionMap.put(GPOptions.MIN_CLAIM_SIZE_TOWN_Z, (double) this.optionMinClaimSizeTownZ);
            this.optionMap.put(GPOptions.CLAIM_EXPIRATION_CHEST, (double) this.optionClaimExpirationChest);
            this.optionMap.put(GPOptions.CLAIM_EXPIRATION_BASIC, (double) this.optionClaimExpirationBasic);
            this.optionMap.put(GPOptions.CLAIM_EXPIRATION_SUBDIVISION, (double) this.optionClaimExpirationSubdivision);
            this.optionMap.put(GPOptions.CLAIM_EXPIRATION_TOWN, (double) this.optionClaimExpirationTown);
            this.optionMap.put(GPOptions.TAX_EXPIRATION_BASIC, (double) this.optionTaxExpirationBasic);
            this.optionMap.put(GPOptions.TAX_EXPIRATION_SUBDIVISION, (double) this.optionTaxExpirationSubdivision);
            this.optionMap.put(GPOptions.TAX_EXPIRATION_TOWN, (double) this.optionTaxExpirationTown);
            this.optionMap.put(GPOptions.TAX_RATE_BASIC, (double) this.optionTaxRateBasic);
            this.optionMap.put(GPOptions.TAX_RATE_SUBDIVISION, (double) this.optionTaxRateSubdivision);
            this.optionMap.put(GPOptions.TAX_RATE_TOWN, (double) this.optionTaxRateTown);
            this.optionMap.put(GPOptions.TAX_RATE_TOWN_BASIC, (double) this.optionTaxRateTownBasic);
            this.optionMap.put(GPOptions.TAX_RATE_TOWN_SUBDIVISION, (double) this.optionTaxRateTownSubdivision);
            // permissions
            this.ignoreAdminClaims = subject.hasPermission(GPPermissions.IGNORE_CLAIMS_ADMIN);
            this.ignoreTowns = subject.hasPermission(GPPermissions.IGNORE_CLAIMS_TOWN);
            this.ignoreWilderness = subject.hasPermission(GPPermissions.IGNORE_CLAIMS_WILDERNESS);
            this.ignoreBasicClaims = subject.hasPermission(GPPermissions.IGNORE_CLAIMS_BASIC);
            this.canManageAdminClaims = subject.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS);
            this.canManageWilderness = subject.hasPermission(GPPermissions.MANAGE_WILDERNESS);
            this.playerName = CommandHelper.lookupPlayerName(this.playerID);
            this.dataInitialized = true;
        });
    }

    public String getPlayerName() {
        if (this.playerName == null) {
            return "[unknown]";
        }

        return this.playerName;
    }

    public void revertActiveVisual(Player player) {
        if (this.visualRevertTask != null) {
            this.visualRevertTask.cancel();
        }

        if (this.visualClaimId != null) {
            GPClaim claim = (GPClaim) GriefPreventionPlugin.instance.dataStore.getClaim(this.worldProperties, this.visualClaimId);
            if (claim != null) {
                claim.playersWatching.remove(this.playerID);
            }
        }
        this.visualClaimId = null;
        if (this.visualBlocks == null || !player.getWorld().equals(this.visualBlocks.get(0).getFinal().getLocation().get().getExtent())) {
            return;
        }

        for (int i = 0; i < this.visualBlocks.size(); i++) {
            BlockSnapshot snapshot = this.visualBlocks.get(i).getOriginal();
            player.sendBlockChange(snapshot.getPosition(), snapshot.getState());
        }
    }

    // whether or not this player is "in" pvp combat
    public boolean inPvpCombat(World world) {
        if (this.lastPvpTimestamp == 0) {
            return false;
        }

        long now = Calendar.getInstance().getTimeInMillis();

        long elapsed = now - this.lastPvpTimestamp;

        if (elapsed > GriefPreventionPlugin.getActiveConfig(world.getProperties()).getConfig().pvp.combatTimeout * 1000) // X seconds
        {
            this.lastPvpTimestamp = 0;
            return false;
        }

        return true;
    }

    @Override
    public int getBlocksAccruedPerHour() {
        return this.optionBlocksAccruedPerHour;
    }

    @Override
    public int getChestClaimExpiration() {
        return this.optionClaimExpirationChest;
    }

    @Override
    public int getCreateClaimLimit() {
        return this.optionCreateClaimLimitBasic;
    }

    @Override
    public int getInitialClaimBlocks() {
        return this.optionInitialClaimBlocks;
    }

    // the number of claim blocks a player has available for claiming land
    @Override
    public int getRemainingClaimBlocks() {
        int remainingBlocks = this.optionInitialClaimBlocks + this.getAccruedClaimBlocks() + this.getBonusClaimBlocks();
        for (Claim claim : this.claimList) {
            if (claim.isSubdivision()) {
                continue;
            }

            GPClaim gpClaim = (GPClaim) claim;
            if ((gpClaim.parent == null || gpClaim.parent.isAdminClaim()) && claim.getData().requiresClaimBlocks()) {
                remainingBlocks -= claim.getArea();
            }
        }

        return remainingBlocks;
    }

    public double getRemainingChunks() {
        final double remainingChunks = this.getRemainingClaimBlocks() / 65536.0;
        return Math.round(remainingChunks * 100.0)/100.0;
    }

    public int getAccruedClaimBlocks() {
        return this.playerStorage.getConfig().getAccruedClaimBlocks();
    }

    public boolean addAccruedClaimBlocks(int newAccruedClaimBlocks) {
        int currentTotal = this.getAccruedClaimBlocks();
        if ((currentTotal + newAccruedClaimBlocks) >= this.optionMaxAccruedBlocks) {
            // player has exceeded limit, set nothing
            return false;
        }

        this.playerStorage.getConfig().setAccruedClaimBlocks(currentTotal + newAccruedClaimBlocks);
        return true;
    }

    public boolean setAccruedClaimBlocks(int newAccruedClaimBlocks) {
        if (newAccruedClaimBlocks >= this.optionMaxAccruedBlocks) {
            // player has exceeded limit, set nothing
            return false;
        }

        this.playerStorage.getConfig().setAccruedClaimBlocks(newAccruedClaimBlocks);
        return true;
    }

    public int getBonusClaimBlocks() {
        return this.playerStorage.getConfig().getBonusClaimBlocks();
    }

    public void setBonusClaimBlocks(int bonusClaimBlocks) {
        this.playerStorage.getConfig().setBonusClaimBlocks(bonusClaimBlocks);
    }

    @Override
    public double getAbandonedReturnRatio() {
        return this.optionAbandonReturnRatioBasic;
    }

    public boolean getCuboidMode() {
        return this.playerStorage.getConfig().getCuboidMode();
    }

    public void setCuboidMode(boolean cuboidMode) {
        this.playerStorage.getConfig().setCuboidMode(cuboidMode);
    }

    public boolean canCreateClaim(Player player) {
        return canCreateClaim(player, false);
    }

    public boolean canCreateClaim(Player player, boolean sendMessage) {
        if (this.shovelMode == ShovelMode.Basic) {
            if (!player.hasPermission(GPPermissions.CLAIM_CREATE_BASIC)) {
                if (sendMessage) {
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.permissionClaimCreate.toText());
                }
                return false;
            }
            if (this.getCuboidMode() && !player.hasPermission(GPPermissions.CLAIM_CUBOID_BASIC)) {
                this.setCuboidMode(false);
                if (sendMessage) {
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.permissionCuboid.toText());
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimCuboidDisabled.toText());
                }
                return false;
            }
        } else if (this.shovelMode == ShovelMode.Subdivide) {
            if (!player.hasPermission(GPPermissions.CLAIM_CREATE_SUBDIVISION)) {
                if (sendMessage) {
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.permissionClaimCreate.toText());
                }
                return false;
            }
            if (this.getCuboidMode() && !player.hasPermission(GPPermissions.CLAIM_CUBOID_SUBDIVISION)) {
                this.setCuboidMode(false);
                if (sendMessage) {
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.permissionCuboid.toText());
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimCuboidDisabled.toText());
                }
                return false;
            }
        } else if (this.shovelMode == ShovelMode.Admin) {
            if (!player.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS)) {
                return false;
            }
            if (this.getCuboidMode() && !player.hasPermission(GPPermissions.CLAIM_CUBOID_ADMIN)) {
                
                return false;
            }
        } else if (this.shovelMode == ShovelMode.Town && !player.hasPermission(GPPermissions.CLAIM_CREATE_TOWN)) {
            return false;
        }

        return true;
    }

    public void saveAllData() {
        this.playerStorage.save();
    }

    public PlayerStorageData getStorageData() {
        return this.playerStorage;
    }

    public List<Claim> getClaims() {
        return ImmutableList.copyOf(this.claimList);
    }

    public List<Claim> getInternalClaims() {
        return this.claimList;
    }

    public void setLastCollideEntityData(int entityId, boolean result) {
        this.lastCollideEntityId = entityId;
        this.lastCollideEntityResult = result;
    }

    public void setLastInteractData(GPClaim claim) {
        this.lastInteractResult = true;
        this.lastInteractClaim = claim.getUniqueId();
        this.lastTickCounter = SpongeImpl.getServer().getTickCounter();
    }

    public boolean checkLastInteraction(GPClaim claim, User user) {
        if (this.lastInteractResult && user != null && ((SpongeImpl.getServer().getTickCounter() - this.lastTickCounter) <= 2)) {
            if (claim.getUniqueId().equals(this.lastInteractClaim) || claim.isWilderness()) {
                return true;
            }
        }

        return false;
    }

    public void setIgnoreClaims(boolean flag) {
        this.ignoreClaims = flag;
    }

    @Override
    public boolean canIgnoreClaim(Claim claim) {
        if (claim == null || this.ignoreClaims == false) {
            return false;
        }

        if (claim.isAdminClaim()) {
            return this.ignoreAdminClaims;
        } else if (claim.isWilderness()) {
            return this.ignoreWilderness;
        }
        return this.ignoreBasicClaims;
    }

    public boolean canManageOption(Player player, GPClaim claim, boolean isGroup) {
        if (claim.allowEdit(player) != null) {
            return false;
        }

        if (isGroup) {
            if (claim.isTown() && player.hasPermission(GPPermissions.COMMAND_OPTIONS_GROUP_TOWN)) {
                return true;
            }
            if (claim.isAdminClaim() && player.hasPermission(GPPermissions.COMMAND_OPTIONS_GROUP_ADMIN)) {
                return true;
            }
            if (claim.isBasicClaim() && player.hasPermission(GPPermissions.COMMAND_OPTIONS_GROUP_BASIC)) {
                return true;
            }
            if (claim.isSubdivision() && player.hasPermission(GPPermissions.COMMAND_OPTIONS_GROUP_SUBDIVISION)) {
                return true;
            }
        } else {
            if (claim.isTown() && player.hasPermission(GPPermissions.COMMAND_OPTIONS_PLAYER_TOWN)) {
                return true;
            }
            if (claim.isAdminClaim() && player.hasPermission(GPPermissions.COMMAND_OPTIONS_PLAYER_ADMIN)) {
                return true;
            }
            if (claim.isBasicClaim() && player.hasPermission(GPPermissions.COMMAND_OPTIONS_PLAYER_BASIC)) {
                return true;
            }
            if (claim.isSubdivision() && player.hasPermission(GPPermissions.COMMAND_OPTIONS_PLAYER_SUBDIVISION)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int getMaxAccruedClaimBlocks() {
        return this.optionMaxAccruedBlocks;
    }

    @Override
    public int getMaxClaimX(ClaimType type) {
        switch(type) {
            case BASIC:
                return this.optionMaxClaimSizeBasicX;
            case SUBDIVISION:
                return this.optionMaxClaimSizeSubX;
            case TOWN:
                return this.optionMaxClaimSizeTownX;
            default:
                break;
        }

        return 0;
    }

    @Override
    public int getMaxClaimY(ClaimType type) {
        switch(type) {
            case BASIC:
                return this.optionMaxClaimSizeBasicY;
            case SUBDIVISION:
                return this.optionMaxClaimSizeSubY;
            case TOWN:
                return this.optionMaxClaimSizeTownY;
            default:
                break;
        }

        return 0;
    }

    @Override
    public int getMaxClaimZ(ClaimType type) {
        switch(type) {
            case BASIC:
                return this.optionMaxClaimSizeBasicZ;
            case SUBDIVISION:
                return this.optionMaxClaimSizeSubZ;
            case TOWN:
                return this.optionMaxClaimSizeTownZ;
            default:
                break;
        }

        return 0;
    }

    @Override
    public int getMinClaimX(ClaimType type) {
        switch(type) {
            case BASIC:
                return this.optionMinClaimSizeBasicX;
            case TOWN:
                return this.optionMinClaimSizeTownX;
            default:
                break;
        }

        return 0;
    }

    @Override
    public int getMinClaimY(ClaimType type) {
        switch(type) {
            case BASIC:
                return this.optionMinClaimSizeBasicY;
            case TOWN:
                return this.optionMinClaimSizeTownY;
            default:
                break;
        }

        return 0;
    }

    @Override
    public int getMinClaimZ(ClaimType type) {
        switch(type) {
            case BASIC:
                return this.optionMinClaimSizeBasicZ;
            case TOWN:
                return this.optionMinClaimSizeTownZ;
            default:
                break;
        }

        return 0;
    }

    public Subject getPlayerSubject() {
        this.playerSubject = null;
        if (this.playerSubject == null || this.playerSubject.get() == null) {
            User user = GriefPreventionPlugin.getOrCreateUser(this.playerID);
            if (user.isOnline()) {
                user = user.getPlayer().get();
            }

            this.playerSubject = new WeakReference<>(user);
        }

        return this.playerSubject.get();
    }

    public double getTotalTax() {
        double totalTax = 0;
        final Subject subject = this.getPlayerSubject();
        for (Claim claim : this.getInternalClaims()) {
            double playerTaxRate = GPOptionHandler.getClaimOptionDouble(subject, claim, GPOptions.Type.TAX_RATE, this);
            totalTax += (claim.getArea() / 256) * playerTaxRate;
        }

        return totalTax;
    }
}
