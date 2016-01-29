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
package me.ryanhamshire.GriefPrevention;

import com.google.common.collect.Maps;
import me.ryanhamshire.GriefPrevention.configuration.PlayerStorageData;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

//holds all of GriefPrevention's player-tied data
public class PlayerData {

    // the player's ID
    public UUID playerID;

    // the player's claims
    public Map<UUID, List<Claim>> playerWorldClaims = Maps.newHashMap();

    // world uuid -> storage
    public Map<UUID, PlayerStorageData> worldStorageData = Maps.newHashMap();

    // where this player was the last time we checked on him for earning claim blocks
    public Location<World> lastAfkCheckLocation = null;

    // what "mode" the shovel is in determines what it will do when it's used
    public ShovelMode shovelMode = ShovelMode.Basic;

    // radius for restore nature fill mode
    public int fillRadius = 0;

    // last place the player used the shovel, useful in creating and resizing claims,
    // because the player must use the shovel twice in those instances
    public Location<World> lastShovelLocation = null;

    // the claim this player is currently resizing
    public Claim claimResizing = null;

    // the claim this player is currently subdividing
    public Claim claimSubdividing = null;

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
    private Date lastLogin = null;

    // the player's last chat message, or slash command complete with parameters
    public String lastMessage = "";

    // last time the player sent a chat message or used a monitored slash command
    public Date lastMessageTimestamp = new Date();

    // number of consecutive "spams"
    public int spamCount = 0;

    // whether the player recently received a warning
    public boolean spamWarned = false;

    // visualization
    public Visualization currentVisualization = null;

    // anti-camping pvp protection
    public boolean pvpImmune = false;
    public long lastSpawn = 0;

    // ignore claims mode
    public boolean ignoreClaims = false;

    // the last claim this player was in, that we know of
    public Claim lastClaim = null;

    // siege
    public SiegeData siegeData = null;

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
    public boolean dropsAreUnlocked = false;

    // message to send to player after he respawns
    public Text messageOnRespawn = null;

    // player which a pet will be given to when it's right-clicked
    public User petGiveawayRecipient = null;

    // timestamp for last "you're building outside your land claims" message
    public Long buildWarningTimestamp = null;

    // spot where a player can't talk, used to mute new players until they've moved a little this is an anti-bot strategy.
    public Location<World> noChatLocation = null;

    // ignore list true means invisible (admin-forced ignore), false means player-created ignore
    public ConcurrentHashMap<UUID, Boolean> ignoredPlayers = new ConcurrentHashMap<UUID, Boolean>();
    public boolean ignoreListChanged = false;

    // profanity warning, once per play session
    public boolean profanityWarned = false;

    // whether or not this player is "in" pvp combat
    public boolean inPvpCombat(World world) {
        if (this.lastPvpTimestamp == 0) {
            return false;
        }

        long now = Calendar.getInstance().getTimeInMillis();

        long elapsed = now - this.lastPvpTimestamp;

        if (elapsed > GriefPrevention.getActiveConfig(world.getProperties()).getConfig().pvp.combatTimeout * 1000) // X seconds
        {
            this.lastPvpTimestamp = 0;
            return false;
        }

        return true;
    }

    // the number of claim blocks a player has available for claiming land
    public int getRemainingClaimBlocks(World world) {
        int remainingBlocks = this.getAccruedClaimBlocks(world) + this.getBonusClaimBlocks(world);
        List<Claim> claimList = this.playerWorldClaims.get(world.getUniqueId());
        for (Claim claim : claimList) {
            remainingBlocks -= claim.getArea();
        }

        // add any blocks this player might have based on group membership (permissions)
        remainingBlocks += GriefPrevention.instance.dataStore.getGroupBonusBlocks(this.playerID);

        return remainingBlocks;
    }

    // don't load data from secondary storage until it's needed
    public int getAccruedClaimBlocks(World world) {
        // if player is over accrued limit, accrued limit was probably reduced
        // in config file AFTER he accrued
        // in that case, leave his blocks where they are
        int currentTotal = this.worldStorageData.get(world.getUniqueId()).getConfig().accruedClaimBlocks;
        if (currentTotal >= GriefPrevention.getActiveConfig(world.getProperties()).getConfig().claim.maxAccruedBlocks) {
            return currentTotal;
        }

        return this.worldStorageData.get(world.getUniqueId()).getConfig().accruedClaimBlocks;
    }

    public void setAccruedClaimBlocks(World world, int accruedClaimBlocks) {
        this.worldStorageData.get(world.getUniqueId()).getConfig().accruedClaimBlocks = accruedClaimBlocks;
    }

    public int getBonusClaimBlocks(World world) {
        return this.worldStorageData.get(world.getUniqueId()).getConfig().bonusClaimBlocks;
    }

    public void setBonusClaimBlocks(World world, int bonusClaimBlocks) {
        this.worldStorageData.get(world.getUniqueId()).getConfig().bonusClaimBlocks = bonusClaimBlocks;
    }

    public void initializePlayerWorldClaims(WorldProperties worldProperties) {
        if (this.playerWorldClaims.get(worldProperties.getUniqueId()) != null) {
            return;
        }

        this.playerWorldClaims.put(worldProperties.getUniqueId(), new ArrayList<Claim>());

        // find all the claims for world belonging to this player
        DataStore dataStore = GriefPrevention.instance.dataStore;

        if (dataStore.worldClaims.get(worldProperties.getUniqueId()) == null) {
            return; // no claims
        }

        List<Claim> claimList = dataStore.worldClaims.get(worldProperties.getUniqueId());
        for (Claim claim : claimList) {
            System.out.println("Found claim " + claim);
            if (!claim.inDataStore) {
                dataStore.worldClaims.get(worldProperties.getUniqueId()).remove(claim);
                continue;
            }

            if (playerID.equals(claim.ownerID)) {
                this.playerWorldClaims.get(worldProperties.getUniqueId()).add(claim);
            }
        }
    }

    public void saveAllData() {
        new SavePlayerDataThread(this);
    }

    private class SavePlayerDataThread extends Thread {

        private PlayerData playerData;

        SavePlayerDataThread(PlayerData playerData) {
            this.playerData = playerData;
        }

        @Override
        public void run() {
            for (PlayerStorageData storageData : playerData.worldStorageData.values()) {
                storageData.save();
            }
        }
    }
}
