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

import com.google.common.collect.Maps;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.api.data.PlayerData;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.command.CommandHelper;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.configuration.PlayerStorageData;
import me.ryanhamshire.griefprevention.permission.GPOptions;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import me.ryanhamshire.griefprevention.util.PlayerUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.common.SpongeImpl;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

//holds all of GriefPrevention's player-tied data
public class GPPlayerData implements PlayerData {

    // the player's ID
    public UUID playerID;
    public WorldProperties worldProperties;
    private WeakReference<Subject> playerSubject;
    private GriefPreventionConfig<?> activeConfig;

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

    // timestamp when last siege ended (where this player was the defender)
    long lastSiegeEndTimeStamp = 0;

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
    public WeakReference<Claim> lastClaim;

    // siege
    public SiegeData siegeData;

    // pvp
    public long lastPvpTimestamp = 0;
    public String lastPvpPlayer = "";

    // safety confirmation for deleting multi-subdivision claims
    public boolean warnedAboutMajorDeletion = false;

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

    public boolean lastInteractResult = false;
    public int lastTickCounter = 0;
    public UUID lastInteractClaim = GriefPreventionPlugin.PUBLIC_UUID;

    // collide event cache
    public int lastCollideEntityId = 0;
    public boolean lastCollideEntityResult = false;

    private String playerName;

    // cached option values
    public double optionAbandonReturnRatio = 1.0;
    public int optionBlocksAccruedPerHour = 120;
    public int optionCreateClaimLimit = 0;
    public int optionInitialClaimBlocks = 100;
    public int optionMaxAccruedBlocks = 80000;
    public int optionMaxClaimSizeX = 0;
    public int optionMaxClaimSizeY = 0;
    public int optionMaxClaimSizeZ = 0;
    public int optionChestClaimExpiration = 7;
    public int optionPlayerClaimExpiration = 14;

    // cached permission values
    public boolean canManageAdminClaims = false;
    public boolean canManageWilderness = false;
    public boolean ignoreAdminClaims = false;
    public boolean ignoreWilderness = false;
    public boolean ignoreBasicClaims = false;

    public boolean dataInitialized = false;

    public GPPlayerData(WorldProperties worldProperties, UUID playerUniqueId, PlayerStorageData playerStorage, GriefPreventionConfig<?> activeConfig, List<Claim> claims) {
        this.worldProperties = worldProperties;
        this.playerID = playerUniqueId;
        this.playerStorage = playerStorage;
        this.claimList = claims;
        this.activeConfig = activeConfig;
        this.refreshPlayerOptions();
    }

    // Run async
    public void refreshPlayerOptions() {
        Sponge.getScheduler().createAsyncExecutor(GriefPreventionPlugin.instance.pluginContainer).execute(() -> {
            if (this.playerSubject == null || this.playerSubject.get() == null) {
                Subject subject = GriefPreventionPlugin.instance.permissionService.getUserSubjects().get(this.playerID.toString());
                this.playerSubject = new WeakReference<>(subject);
            }
            // options
            this.optionAbandonReturnRatio = PlayerUtils.getOptionDoubleValue(this.playerSubject.get(), GPOptions.ABANDON_RETURN_RATIO, 1.0);
            this.optionBlocksAccruedPerHour = PlayerUtils.getOptionIntValue(this.playerSubject.get(), GPOptions.BLOCKS_ACCRUED_PER_HOUR, 120);
            this.optionChestClaimExpiration = PlayerUtils.getOptionIntValue(this.playerSubject.get(), GPOptions.CHEST_CLAIM_EXPIRATION, 7);
            this.optionCreateClaimLimit = PlayerUtils.getOptionIntValue(this.playerSubject.get(), GPOptions.CREATE_CLAIM_LIMIT, 0);
            this.optionInitialClaimBlocks = PlayerUtils.getOptionIntValue(this.playerSubject.get(), GPOptions.INITIAL_CLAIM_BLOCKS, 100);
            this.optionMaxAccruedBlocks = PlayerUtils.getOptionIntValue(this.playerSubject.get(), GPOptions.MAX_ACCRUED_BLOCKS, 80000);
            this.optionMaxClaimSizeX= PlayerUtils.getOptionIntValue(this.playerSubject.get(), GPOptions.MAX_CLAIM_SIZE_X, 0);
            this.optionMaxClaimSizeY= PlayerUtils.getOptionIntValue(this.playerSubject.get(), GPOptions.MAX_CLAIM_SIZE_Y, 0);
            this.optionMaxClaimSizeZ = PlayerUtils.getOptionIntValue(this.playerSubject.get(), GPOptions.MAX_CLAIM_SIZE_Z, 0);
            this.optionPlayerClaimExpiration = PlayerUtils.getOptionIntValue(this.playerSubject.get(), GPOptions.PLAYER_CLAIM_EXPIRATION, 14);
            // permissions
            this.ignoreAdminClaims = this.playerSubject.get().hasPermission(GPPermissions.IGNORE_CLAIMS_ADMIN);
            this.ignoreWilderness = this.playerSubject.get().hasPermission(GPPermissions.IGNORE_CLAIMS_WILDERNESS);
            this.ignoreBasicClaims = this.playerSubject.get().hasPermission(GPPermissions.IGNORE_CLAIMS_BASIC);
            this.canManageAdminClaims = this.playerSubject.get().hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS);
            this.canManageWilderness = this.playerSubject.get().hasPermission(GPPermissions.MANAGE_WILDERNESS);
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
        return this.optionChestClaimExpiration;
    }

    @Override
    public int getCreateClaimLimit() {
        return this.optionCreateClaimLimit;
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
            if (!claim.isAdminClaim() && !claim.isSubdivision() && claim.getData().requiresClaimBlocks()) {
                remainingBlocks -= claim.getArea();
            }
        }

        return remainingBlocks;
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
        return this.optionAbandonReturnRatio;
    }

    public boolean getCuboidMode() {
        return this.playerStorage.getConfig().getCuboidMode();
    }

    public void setCuboidMode(boolean cuboidMode) {
        this.playerStorage.getConfig().setCuboidMode(cuboidMode);
    }

    public void saveAllData() {
        this.playerStorage.save();
    }

    public PlayerStorageData getStorageData() {
        return this.playerStorage;
    }

    public List<Claim> getClaims() {
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
            if (claim.getUniqueId().equals(this.lastInteractClaim) || claim.isWildernessClaim()) {
                return true;
            }
        }

        return false;
    }

    public void setIgnoreClaims(boolean flag) {
        this.ignoreClaims = flag;
    }

    public boolean canIgnoreClaim(GPClaim claim) {
        if (claim == null || this.ignoreClaims == false) {
            return false;
        }

        if (claim.isAdminClaim()) {
            return this.ignoreAdminClaims;
        } else if (claim.isWildernessClaim()) {
            return this.ignoreWilderness;
        }
        return this.ignoreBasicClaims;
    }

    @Override
    public int getMaxAccruedClaimBlocks() {
        return this.optionMaxAccruedBlocks;
    }

    @Override
    public int getMaxClaimX(ClaimType type) {
        return this.optionMaxClaimSizeX;
    }

    @Override
    public int getMaxClaimY(ClaimType type) {
        return this.optionMaxClaimSizeY;
    }

    @Override
    public int getMaxClaimZ(ClaimType type) {
        return this.optionMaxClaimSizeZ;
    }
}
