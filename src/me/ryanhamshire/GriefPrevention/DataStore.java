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

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import me.ryanhamshire.GriefPrevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.GriefPrevention.configuration.GriefPreventionConfig.DimensionConfig;
import me.ryanhamshire.GriefPrevention.configuration.GriefPreventionConfig.GlobalConfig;
import me.ryanhamshire.GriefPrevention.configuration.GriefPreventionConfig.WorldConfig;
import me.ryanhamshire.GriefPrevention.events.ClaimDeletedEvent;
import net.minecraft.item.ItemStack;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//singleton class which manages all GriefPrevention data (except for config options)
public abstract class DataStore {

    // in-memory cache for player data
    protected ConcurrentHashMap<UUID, PlayerData> playerUniqueIdToPlayerDataMap = new ConcurrentHashMap<>();

    // in-memory cache for group (permission-based) data
    protected ConcurrentHashMap<String, Integer> permissionToBonusBlocksMap = new ConcurrentHashMap<>();

    // in-memory cache for claim data
    public Map<UUID, List<Claim>> worldClaims = Maps.newHashMap();
    ConcurrentHashMap<String, ArrayList<Claim>> chunksToClaimsMap = new ConcurrentHashMap<>();
    public static Map<UUID, GriefPreventionConfig<DimensionConfig>> dimensionConfigMap = Maps.newHashMap();
    public static Map<UUID, GriefPreventionConfig<WorldConfig>> worldConfigMap = Maps.newHashMap();
    public static GriefPreventionConfig<GlobalConfig> globalConfig;

    // in-memory cache for messages
    protected EnumMap<Messages, CustomizableMessage> messages = new EnumMap<>(Messages.class);

    // pattern for unique user identifiers (UUIDs)
    protected final static Pattern uuidpattern = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    // path information, for where stuff stored on disk is well... stored
    protected final static Path dataLayerFolderPath = Paths.get("config").resolve("GriefPrevention");
    final static Path playerDataFolderPath = dataLayerFolderPath.resolve("PlayerData");
    final static Path configFilePath = dataLayerFolderPath.resolve("config.conf");
    final static Path messagesFilePath = dataLayerFolderPath.resolve("messages.conf");
    final static Path softMuteFilePath = dataLayerFolderPath.resolve("softMute.txt");
    final static Path bannedWordsFilePath = dataLayerFolderPath .resolve("bannedWords.txt");

    // the latest version of the data schema implemented here
    protected static final int latestSchemaVersion = 2;

    // reading and writing the schema version to the data store
    abstract int getSchemaVersionFromStorage();

    abstract void updateSchemaVersionInStorage(int versionToSet);

    // current version of the schema of data in secondary storage
    private int currentSchemaVersion = -1; // -1 means not determined yet

    // video links
    static final String SURVIVAL_VIDEO_URL_RAW = "http://bit.ly/mcgpuser";
    static final String CREATIVE_VIDEO_URL_RAW = "http://bit.ly/mcgpcrea";
    static final String SUBDIVISION_VIDEO_URL_RAW = "http://bit.ly/mcgpsub";

    // list of UUIDs which are soft-muted
    ConcurrentHashMap<UUID, Boolean> softMuteMap = new ConcurrentHashMap<UUID, Boolean>();

    protected int getSchemaVersion() {
        if (this.currentSchemaVersion >= 0) {
            return this.currentSchemaVersion;
        } else {
            this.currentSchemaVersion = this.getSchemaVersionFromStorage();
            return this.currentSchemaVersion;
        }
    }

    protected void setSchemaVersion(int versionToSet) {
        this.currentSchemaVersion = versionToSet;
        this.updateSchemaVersionInStorage(versionToSet);
    }

    // initialization!
    void initialize() throws Exception {
        int count = 0;
        for (List<Claim> claimList : this.worldClaims.values()) {
            count += claimList.size();
        }
        GriefPrevention.AddLogEntry(count + " total claims loaded.");

        // ensure data folders exist
        File playerDataFolder = playerDataFolderPath.toFile();
        if (!playerDataFolder.exists()) {
            playerDataFolder.mkdirs();
        }

        // load up all the messages from messages.hocon
        this.loadMessages();
        GriefPrevention.AddLogEntry("Customizable messages loaded.");

        // load list of soft mutes
        this.loadSoftMutes();

        // make a note of the data store schema version
        this.setSchemaVersion(latestSchemaVersion);
    }

    private void loadSoftMutes() {
        File softMuteFile = softMuteFilePath.toFile();
        if (softMuteFile.exists()) {
            BufferedReader inStream = null;
            try {
                // open the file
                inStream = new BufferedReader(new FileReader(softMuteFile.getAbsolutePath()));

                // while there are lines left
                String nextID = inStream.readLine();
                while (nextID != null) {
                    // parse line into a UUID
                    UUID playerID;
                    try {
                        playerID = UUID.fromString(nextID);
                    } catch (Exception e) {
                        playerID = null;
                        GriefPrevention.AddLogEntry("Failed to parse soft mute entry as a UUID: " + nextID);
                    }

                    // push it into the map
                    if (playerID != null) {
                        this.softMuteMap.put(playerID, true);
                    }

                    // move to the next
                    nextID = inStream.readLine();
                }
            } catch (Exception e) {
                GriefPrevention.AddLogEntry("Failed to read from the soft mute data file: " + e.toString());
                e.printStackTrace();
            }

            try {
                if (inStream != null)
                    inStream.close();
            } catch (IOException exception) {
            }
        }
    }

    public List<String> loadBannedWords() {
        try {
            File bannedWordsFile = bannedWordsFilePath.toFile();
            if (!bannedWordsFile.exists()) {
                Files.touch(bannedWordsFile);
                String defaultWords =
                        "nigger\nniggers\nniger\nnigga\nnigers\nniggas\n" +
                                "fag\nfags\nfaggot\nfaggots\nfeggit\nfeggits\nfaggit\nfaggits\n" +
                                "cunt\ncunts\nwhore\nwhores\nslut\nsluts\n";
                Files.append(defaultWords, bannedWordsFile, Charset.forName("UTF-8"));
            }

            return Files.readLines(bannedWordsFile, Charset.forName("UTF-8"));
        } catch (Exception e) {
            GriefPrevention.AddLogEntry("Failed to read from the banned words data file: " + e.toString());
            e.printStackTrace();
            return new ArrayList<String>();
        }
    }

    // updates soft mute map and data file
    public boolean toggleSoftMute(UUID playerID) {
        boolean newValue = !this.isSoftMuted(playerID);

        this.softMuteMap.put(playerID, newValue);
        this.saveSoftMutes();

        return newValue;
    }

    public boolean isSoftMuted(UUID playerID) {
        Boolean mapEntry = this.softMuteMap.get(playerID);
        if (mapEntry == null || mapEntry == Boolean.FALSE) {
            return false;
        }

        return true;
    }

    private void saveSoftMutes() {
        BufferedWriter outStream = null;

        try {
            // open the file and write the new value
            File softMuteFile = softMuteFilePath.toFile();
            softMuteFile.createNewFile();
            outStream = new BufferedWriter(new FileWriter(softMuteFile));

            for (Map.Entry<UUID, Boolean> entry : softMuteMap.entrySet()) {
                if (entry.getValue().booleanValue()) {
                    outStream.write(entry.getKey().toString());
                    outStream.newLine();
                }
            }

        }

        // if any problem, log it
        catch (Exception e) {
            GriefPrevention.AddLogEntry("Unexpected exception saving soft mute data: " + e.getMessage());
            e.printStackTrace();
        }

        // close the file
        try {
            if (outStream != null)
                outStream.close();
        } catch (IOException exception) {
        }
    }

    // removes cached player data from memory
    public synchronized void clearCachedPlayerData(UUID playerID) {
        this.playerUniqueIdToPlayerDataMap.remove(playerID);
    }

    // gets the number of bonus blocks a player has from his permissions
    // Bukkit doesn't allow for checking permissions of an offline player.
    // this will return 0 when he's offline, and the correct number when online.
    public synchronized int getGroupBonusBlocks(UUID playerID) {
        int bonusBlocks = 0;
        Set<String> keys = permissionToBonusBlocksMap.keySet();
        Iterator<String> iterator = keys.iterator();
        while (iterator.hasNext()) {
            String groupName = iterator.next();
            Optional<Player> player = Sponge.getGame().getServer().getPlayer(playerID);
            if (player.isPresent() && player.get().hasPermission(groupName)) {
                bonusBlocks += this.permissionToBonusBlocksMap.get(groupName);
            }
        }

        return bonusBlocks;
    }

    // grants a group (players with a specific permission) bonus claim blocks as
    // long as they're still members of the group
    synchronized public int adjustGroupBonusBlocks(String groupName, int amount) {
        Integer currentValue = this.permissionToBonusBlocksMap.get(groupName);
        if (currentValue == null)
            currentValue = 0;

        currentValue += amount;
        this.permissionToBonusBlocksMap.put(groupName, currentValue);

        // write changes to storage to ensure they don't get lost
        this.saveGroupBonusBlocks(groupName, currentValue);

        return currentValue;
    }

    abstract void saveGroupBonusBlocks(String groupName, int amount);

    @SuppressWarnings("serial")
    public class NoTransferException extends Exception {

        NoTransferException(String message) {
            super(message);
        }
    }

    synchronized public void changeClaimOwner(Claim claim, UUID newOwnerID) throws NoTransferException {
        // if it's a subdivision, throw an exception
        if (claim.parent != null) {
            throw new NoTransferException("Subdivisions can't be transferred.  Only top-level claims may change owners.");
        }

        // otherwise update information

        // determine current claim owner
        PlayerData ownerData = null;
        if (!claim.isAdminClaim()) {
            ownerData = this.getPlayerData(claim.world.getProperties(), claim.ownerID);
        }

        // determine new owner
        PlayerData newOwnerData = null;

        if (newOwnerID != null) {
            newOwnerData = this.getPlayerData(claim.world.getProperties(), newOwnerID);
        }

        // transfer
        claim.ownerID = newOwnerID;
        this.saveClaim(claim);

        // adjust blocks and other records
        if (ownerData != null) {
            ownerData.playerWorldClaims.get(claim.world.getUniqueId()).remove(claim);
        }

        if (newOwnerData != null) {
            newOwnerData.playerWorldClaims.get(claim.world).add(claim);
        }
    }

    // adds a claim to the datastore, making it an effective claim
    synchronized void addClaim(Claim newClaim, boolean writeToStorage) {
        PlayerData ownerData = this.getPlayerData(newClaim.world, newClaim.ownerID);
        if (ownerData.playerWorldClaims.get(newClaim.world.getUniqueId()) == null) {
            ownerData.playerWorldClaims.put(newClaim.world.getUniqueId(), new ArrayList<Claim>());
        }
        ownerData.playerWorldClaims.get(newClaim.world.getUniqueId()).add(newClaim);

        // subdivisions are added under their parent, not directly to the hash map for direct search
        if (newClaim.parent != null) {
            if (!newClaim.parent.children.contains(newClaim)) {
                newClaim.parent.children.add(newClaim);
            }
            newClaim.inDataStore = true;
            if (writeToStorage) {
                this.saveClaim(newClaim);
            }

            return;
        }

        // add it and mark it as added
        if (this.worldClaims.get(newClaim.world.getProperties().getUniqueId()) == null) {
            List<Claim> newClaims = new ArrayList<>();
            newClaims.add(newClaim);
            this.worldClaims.put(newClaim.world.getProperties().getUniqueId(), newClaims);
        } else {
            this.worldClaims.get(newClaim.world.getProperties().getUniqueId()).add(newClaim);
        }

        ArrayList<String> chunkStrings = newClaim.getChunkStrings();
        for (String chunkString : chunkStrings) {
            ArrayList<Claim> claimsInChunk = this.chunksToClaimsMap.get(chunkString);
            if (claimsInChunk == null) {
                claimsInChunk = new ArrayList<Claim>();
                this.chunksToClaimsMap.put(chunkString, claimsInChunk);
            }

            claimsInChunk.add(newClaim);
        }

        newClaim.inDataStore = true;

        // make sure the claim is saved to disk
        if (writeToStorage) {
            this.saveClaim(newClaim);
        }
    }

    // turns a location into a string, useful in data storage
    private String locationStringDelimiter = ";";

    String positionToString(Location<World> location) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(location.getBlockX());
        stringBuilder.append(locationStringDelimiter);
        stringBuilder.append(location.getBlockY());
        stringBuilder.append(locationStringDelimiter);
        stringBuilder.append(location.getBlockZ());

        return stringBuilder.toString();
    }

    // turns a location string back into a location
    Vector3i positionFromString(String string) throws Exception {
        // split the input string on the space
        String[] elements = string.split(locationStringDelimiter);

        // expect three elements - X, Y, and Z, respectively
        if (elements.length < 3) {
            throw new Exception("Expected four distinct parts to the location string: \"" + string + "\"");
        }

        String xString = elements[0];
        String yString = elements[1];
        String zString = elements[2];

        // convert those numerical strings to integer values
        int x = Integer.parseInt(xString);
        int y = Integer.parseInt(yString);
        int z = Integer.parseInt(zString);

        return new Vector3i(x, y, z);
    }

    // saves any changes to a claim to secondary storage
    synchronized public void saveClaim(Claim claim) {
        // ensure a unique identifier for the claim which will be used to name
        // the file on disk
        if (claim.id == null) {
            claim.id = UUID.randomUUID();
        }

        this.writeClaimToStorage(claim);
    }

    abstract void writeClaimToStorage(Claim claim);

    // increments the claim ID and updates secondary storage to be sure it's
    // saved
    abstract void incrementNextClaimID();

    // retrieves player data from memory or secondary storage, as necessary
    // if the player has never been on the server before, this will return a
    // fresh player data with default values
    public PlayerData getPlayerData(World world, UUID playerID) {
        return getPlayerData(world.getProperties(), playerID);
    }

    public abstract PlayerData getPlayerData(WorldProperties worldProperties, UUID playerID);

    abstract PlayerData getPlayerDataFromStorage(UUID playerID);

    // deletes a claim or subdivision
    synchronized public void deleteClaim(Claim claim) {
        this.deleteClaim(claim, true);
    }

    public synchronized void deleteClaim(Claim claim, boolean fireEvent) {
        // delete any children
        for (int j = 0; j < claim.children.size(); j++) {
            this.deleteClaim(claim.children.get(j--), true);
        }

        // subdivisions must also be removed from the parent claim child list
        if (claim.parent != null) {
            Claim parentClaim = claim.parent;
            parentClaim.children.remove(claim);
        }

        // mark as deleted so any references elsewhere can be ignored
        claim.inDataStore = false;

        // remove from memory
        Iterator<Claim> iterator = this.worldClaims.get(claim.world.getProperties().getUniqueId()).iterator();
        while (iterator.hasNext()) {
            Claim worldClaim = iterator.next();
            if (worldClaim.id == claim.id) {
                iterator.remove();
                break;
            }
        }

        ArrayList<String> chunkStrings = claim.getChunkStrings();
        for (String chunkString : chunkStrings) {
            ArrayList<Claim> claimsInChunk = this.chunksToClaimsMap.get(chunkString);
            for (int j = 0; j < claimsInChunk.size(); j++) {
                if (claimsInChunk.get(j).id.equals(claim.id)) {
                    claimsInChunk.remove(j);
                    break;
                }
            }
        }

        // remove from secondary storage
        this.deleteClaimFromSecondaryStorage(claim);

        // update player data
        if (claim.ownerID != null) {
            PlayerData ownerData = this.getPlayerData(claim.world, claim.ownerID);
            ownerData.playerWorldClaims.get(claim.world.getUniqueId()).remove(claim);
        }

        if (fireEvent) {
            ClaimDeletedEvent ev = new ClaimDeletedEvent(claim);
            Sponge.getGame().getEventManager().post(ev);
        }
    }

    abstract void deleteClaimFromSecondaryStorage(Claim claim);

    // gets the claim at a specific location
    // ignoreHeight = TRUE means that a location UNDER an existing claim will return the claim
    // cachedClaim can be NULL, but will help performance if you have a
    // reasonable guess about which claim the location is in
    synchronized public Claim getClaimAt(Location<World> location, boolean ignoreHeight, Claim cachedClaim) {
        // check cachedClaim guess first. if it's in the datastore and the
        // location is inside it, we're done
        if (cachedClaim != null && cachedClaim.inDataStore && cachedClaim.contains(location, ignoreHeight, true))
            return cachedClaim;

        // find a top level claim
        String chunkID = this.getChunkString(location);
        ArrayList<Claim> claimsInChunk = this.chunksToClaimsMap.get(chunkID);
        if (claimsInChunk == null)
            return null;

        for (Claim claim : claimsInChunk) {
            if (claim.inDataStore && claim.contains(location, ignoreHeight, false)) {
                // when we find a top level claim, if the location is in one of
                // its subdivisions,
                // return the SUBDIVISION, not the top level claim
                for (int j = 0; j < claim.children.size(); j++) {
                    Claim subdivision = claim.children.get(j);
                    if (subdivision.inDataStore && subdivision.contains(location, ignoreHeight, false))
                        return subdivision;
                }

                return claim;
            }
        }

        // if no claim found, return null
        return null;
    }

    // finds a claim by ID
    public synchronized Claim getClaim(World world, UUID id) {
        List<Claim> claimList = this.worldClaims.get(world.getProperties().getUniqueId());
        for (Claim claim : claimList) {
            if (claim.inDataStore && claim.getID() == id) {
                return claim;
            }
        }

        return null;
    }

    // gets an almost-unique, persistent identifier string for a chunk
    String getChunkString(Location<World> location) {
        return String.valueOf(location.getBlockX() >> 4) + (location.getBlockZ() >> 4);
    }

    // creates a claim.
    // if the new claim would overlap an existing claim, returns a failure along
    // with a reference to the existing claim
    // if the new claim would overlap a WorldGuard region where the player
    // doesn't have permission to build, returns a failure with NULL for claim
    // otherwise, returns a success along with a reference to the new claim
    // use ownerName == "" for administrative claims
    // for top level claims, pass parent == NULL
    // DOES adjust claim blocks available on success (players can go into
    // negative quantity available)
    // DOES check for world guard regions where the player doesn't have
    // permission
    // does NOT check a player has permission to create a claim, or enough claim blocks.
    // does NOT check minimum claim size constraints
    // does NOT visualize the new claim for any players
    synchronized public CreateClaimResult createClaim(World world, int x1, int x2, int y1, int y2, int z1, int z2, UUID ownerID, Claim parent,
            UUID id, Player creatingPlayer) {
        CreateClaimResult result = new CreateClaimResult();

        int smallx, bigx, smally, bigy, smallz, bigz;

        // determine small versus big inputs
        if (x1 < x2) {
            smallx = x1;
            bigx = x2;
        } else {
            smallx = x2;
            bigx = x1;
        }

        if (y1 < y2) {
            smally = y1;
            bigy = y2;
        } else {
            smally = y2;
            bigy = y1;
        }

        if (z1 < z2) {
            smallz = z1;
            bigz = z2;
        } else {
            smallz = z2;
            bigz = z1;
        }

        // creative mode claims always go to bedrock
        if (GriefPrevention.instance.claimModeIsActive(world.getProperties(), ClaimsMode.Creative)) {
            smally = 2;
        }

        // create a new claim instance (but don't save it, yet)
        Claim newClaim = new Claim(
                new Location<World>(world, smallx, smally, smallz),
                new Location<World>(world, bigx, bigy, bigz),
                ownerID,
                new ArrayList<String>(),
                new ArrayList<String>(),
                new ArrayList<String>(),
                new ArrayList<String>(),
                id == null ? UUID.randomUUID() : id);

        newClaim.parent = parent;

        // ensure this new claim won't overlap any existing claims
        ArrayList<Claim> claimsToCheck;
        if (newClaim.parent != null) {
            claimsToCheck = newClaim.parent.children;
        } else {
            claimsToCheck = (ArrayList<Claim>) this.worldClaims.get(world.getProperties().getUniqueId());
        }

        if (claimsToCheck != null) {
            for (int i = 0; i < claimsToCheck.size(); i++) {
                Claim otherClaim = claimsToCheck.get(i);
    
                // if we find an existing claim which will be overlapped
                if (otherClaim.id != newClaim.id && otherClaim.inDataStore && otherClaim.overlaps(newClaim)) {
                    // result = fail, return conflicting claim
                    result.succeeded = false;
                    result.claim = otherClaim;
                    return result;
                }
            }
        }

        newClaim.context = new Context("claim", newClaim.id.toString());
        // Assign owner full flag permissions in claim context
        creatingPlayer.getSubjectData().setPermission(ImmutableSet.of(newClaim.getContext()), "griefprevention.command.claim.flag", Tristate.TRUE);
        // otherwise add this new claim to the data store to make it effective
        this.addClaim(newClaim, true);

        // then return success along with reference to new claim
        result.succeeded = true;
        result.claim = newClaim;
        return result;
    }

    public void asyncSavePlayerData(UUID playerID, PlayerData playerData) {
        // save everything except the ignore list
        this.overrideSavePlayerData(playerID, playerData);

        // save the ignore list
        if (playerData.ignoreListChanged) {
            StringBuilder fileContent = new StringBuilder();
            try {
                for (UUID uuidKey : playerData.ignoredPlayers.keySet()) {
                    Boolean value = playerData.ignoredPlayers.get(uuidKey);
                    if (value == null)
                        continue;

                    // admin-enforced ignores begin with an asterisk
                    if (value) {
                        fileContent.append("*");
                    }

                    fileContent.append(uuidKey);
                    fileContent.append("\n");
                }

                // write data to file
                File playerDataFile = playerDataFolderPath.resolve(playerID.toString() + ".ignore").toFile();
                Files.write(fileContent.toString().trim().getBytes("UTF-8"), playerDataFile);
            }

            // if any problem, log it
            catch (Exception e) {
                GriefPrevention.AddLogEntry(
                        "GriefPrevention: Unexpected exception saving data for player \"" + playerID.toString() + "\": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    abstract void overrideSavePlayerData(UUID playerID, PlayerData playerData);

    // extends a claim to a new depth
    // respects the max depth config variable
    synchronized public void extendClaim(Claim claim, int newDepth) {
        if (newDepth < GriefPrevention.getActiveConfig(claim.world.getProperties()).getConfig().claim.maxClaimDepth) {
            newDepth = GriefPrevention.getActiveConfig(claim.world.getProperties()).getConfig().claim.maxClaimDepth;
        }

        if (claim.parent != null) {
            claim = claim.parent;
        }

        // adjust to new depth
        Vector3d newLesserPosition = new Vector3d(claim.lesserBoundaryCorner.getX(), newDepth, claim.lesserBoundaryCorner.getZ());
        claim.lesserBoundaryCorner = claim.lesserBoundaryCorner.setPosition(newLesserPosition);
        Vector3d newGreaterPosition = new Vector3d(claim.greaterBoundaryCorner.getX(), newDepth, claim.greaterBoundaryCorner.getZ());
        claim.greaterBoundaryCorner = claim.greaterBoundaryCorner.setPosition(newGreaterPosition);

        for (Claim subdivision : claim.children) {
            newLesserPosition = new Vector3d(subdivision.lesserBoundaryCorner.getX(), newDepth, subdivision.lesserBoundaryCorner.getZ());
            subdivision.lesserBoundaryCorner = subdivision.lesserBoundaryCorner.setPosition(newLesserPosition);
            newGreaterPosition = new Vector3d(subdivision.greaterBoundaryCorner.getX(), newDepth, subdivision.greaterBoundaryCorner.getZ());
            subdivision.greaterBoundaryCorner = subdivision.greaterBoundaryCorner.setPosition(newGreaterPosition);
            this.saveClaim(subdivision);
        }

        // save changes
        this.saveClaim(claim);
    }

    // starts a siege on a claim
    // does NOT check siege cooldowns, see onCooldown() below
    synchronized public void startSiege(Player attacker, Player defender, Claim defenderClaim) {
        // fill-in the necessary SiegeData instance
        SiegeData siegeData = new SiegeData(attacker, defender, defenderClaim);
        PlayerData attackerData = this.getPlayerData(attacker.getWorld(), attacker.getUniqueId());
        PlayerData defenderData = this.getPlayerData(defender.getWorld(), defender.getUniqueId());
        attackerData.siegeData = siegeData;
        defenderData.siegeData = siegeData;
        defenderClaim.siegeData = siegeData;

        // start a task to monitor the siege
        // why isn't this a "repeating" task?
        // because depending on the status of the siege at the time the task
        // runs, there may or may not be a reason to run the task again
        SiegeCheckupTask siegeTask = new SiegeCheckupTask(siegeData);
        Task task = Sponge.getGame().getScheduler().createTaskBuilder().delay(30, TimeUnit.SECONDS).execute(siegeTask).submit(GriefPrevention.instance);
        siegeData.checkupTaskID = task.getUniqueId();
    }

    // ends a siege
    // either winnerName or loserName can be null, but not both
    synchronized public void endSiege(SiegeData siegeData, String winnerName, String loserName, boolean death) {
        boolean grantAccess = false;

        // determine winner and loser
        if (winnerName == null && loserName != null) {
            if (siegeData.attacker.getName().equals(loserName)) {
                winnerName = siegeData.defender.getName();
            } else {
                winnerName = siegeData.attacker.getName();
            }
        } else if (winnerName != null && loserName == null) {
            if (siegeData.attacker.getName().equals(winnerName)) {
                loserName = siegeData.defender.getName();
            } else {
                loserName = siegeData.attacker.getName();
            }
        }

        // if the attacker won, plan to open the doors for looting
        if (siegeData.attacker.getName().equals(winnerName)) {
            grantAccess = true;
        }

        PlayerData attackerData = this.getPlayerData(siegeData.attacker.getWorld(), siegeData.attacker.getUniqueId());
        attackerData.siegeData = null;

        PlayerData defenderData = this.getPlayerData(siegeData.defender.getWorld(), siegeData.defender.getUniqueId());
        defenderData.siegeData = null;
        defenderData.lastSiegeEndTimeStamp = System.currentTimeMillis();

        // start a cooldown for this attacker/defender pair
        Long now = Calendar.getInstance().getTimeInMillis();
        Long cooldownEnd = now + 1000 * 60 * 60; // one hour from now
        this.siegeCooldownRemaining.put(siegeData.attacker.getName() + "_" + siegeData.defender.getName(), cooldownEnd);

        // start cooldowns for every attacker/involved claim pair
        for (int i = 0; i < siegeData.claims.size(); i++) {
            Claim claim = siegeData.claims.get(i);
            claim.siegeData = null;
            this.siegeCooldownRemaining.put(siegeData.attacker.getName() + "_" + claim.getOwnerName(), cooldownEnd);

            // if doors should be opened for looting, do that now
            if (grantAccess) {
                claim.doorsOpen = true;
            }
        }

        // cancel the siege checkup task
        Sponge.getGame().getScheduler().getTaskById(siegeData.checkupTaskID).get().cancel();

        // notify everyone who won and lost
        if (winnerName != null && loserName != null) {
            Sponge.getGame().getServer().getBroadcastChannel().send(Text.of(winnerName + " defeated " + loserName + " in siege warfare!"));
        }

        // if the claim should be opened to looting
        if (grantAccess) {
            Optional<Player> winner = Sponge.getGame().getServer().getPlayer(winnerName);
            if (winner.isPresent()) {
                // notify the winner
                GriefPrevention.sendMessage(winner.get(), TextMode.Success, Messages.SiegeWinDoorsOpen);

                // schedule a task to secure the claims in about 5 minutes
                SecureClaimTask task = new SecureClaimTask(siegeData);
                Sponge.getGame().getScheduler().createTaskBuilder().delay(5, TimeUnit.MINUTES).execute(task).submit(GriefPrevention.instance);
            }
        }

        // if the siege ended due to death, transfer inventory to winner
        if (death) {
            Optional<Player> winner = Sponge.getGame().getServer().getPlayer(winnerName);
            Optional<Player> loser = Sponge.getGame().getServer().getPlayer(loserName);
            if (winner.isPresent() && loser.isPresent()) {
                // get loser's inventory, then clear it
                net.minecraft.entity.player.EntityPlayerMP loserPlayer = (net.minecraft.entity.player.EntityPlayerMP) loser.get();
                net.minecraft.entity.player.EntityPlayerMP winnerPlayer = (net.minecraft.entity.player.EntityPlayerMP) winner.get();
                List<ItemStack> loserItems = new ArrayList<ItemStack>();
                for (int i = 0; i < loserPlayer.inventory.mainInventory.length; i++) {
                    if (loserPlayer.inventory.mainInventory[i] != null) {
                        loserItems.add(loserPlayer.inventory.mainInventory[i]);
                    }
                }

                loserPlayer.inventory.clear();

                // try to add it to the winner's inventory
                Iterator<ItemStack> iterator = loserItems.iterator();
                while (iterator.hasNext()) {
                    ItemStack loserItem = iterator.next();
                    boolean added = winnerPlayer.inventory.addItemStackToInventory(loserItem);
                    if (added) {
                        iterator.remove();
                    }
                }

                // drop any remainder on the ground at his feet
                Location<World> winnerLocation = winner.get().getLocation();
                for (int i = 0; i < loserItems.size(); i++) {
                    net.minecraft.entity.item.EntityItem entity = new net.minecraft.entity.item.EntityItem((net.minecraft.world.World) winnerLocation.getExtent(), winnerLocation.getX(), winnerLocation.getY(), winnerLocation.getZ(), loserItems.get(i));
                    entity.setPickupDelay(10);
                    ((net.minecraft.world.World)winnerLocation.getExtent()).spawnEntityInWorld(entity);
                }
            }
        }
    }

    // timestamp for each siege cooldown to end
    public HashMap<String, Long> siegeCooldownRemaining = new HashMap<String, Long>();

    // whether or not a sieger can siege a particular victim or claim,
    // considering only cooldowns
    synchronized public boolean onCooldown(Player attacker, Player defender, Claim defenderClaim) {
        Long cooldownEnd = null;

        // look for an attacker/defender cooldown
        if (this.siegeCooldownRemaining.get(attacker.getName() + "_" + defender.getName()) != null) {
            cooldownEnd = this.siegeCooldownRemaining.get(attacker.getName() + "_" + defender.getName());

            if (Calendar.getInstance().getTimeInMillis() < cooldownEnd) {
                return true;
            }

            // if found but expired, remove it
            this.siegeCooldownRemaining.remove(attacker.getName() + "_" + defender.getName());
        }

        // look for genderal defender cooldown
        PlayerData defenderData = this.getPlayerData(defender.getWorld(), defender.getUniqueId());
        if (defenderData.lastSiegeEndTimeStamp > 0) {
            long now = System.currentTimeMillis();
            if (now - defenderData.lastSiegeEndTimeStamp > 1000 * 60 * 15) // 15 minutes in milliseconds
            {
                return true;
            }
        }

        // look for an attacker/claim cooldown
        if (cooldownEnd == null && this.siegeCooldownRemaining.get(attacker.getName() + "_" + defenderClaim.getOwnerName()) != null) {
            cooldownEnd = this.siegeCooldownRemaining.get(attacker.getName() + "_" + defenderClaim.getOwnerName());

            if (Calendar.getInstance().getTimeInMillis() < cooldownEnd) {
                return true;
            }

            // if found but expired, remove it
            this.siegeCooldownRemaining.remove(attacker.getName() + "_" + defenderClaim.getOwnerName());
        }

        return false;
    }

    // extend a siege, if it's possible to do so
    synchronized void tryExtendSiege(Player player, Claim claim) {
        PlayerData playerData = this.getPlayerData(player.getWorld(), player.getUniqueId());

        // player must be sieged
        if (playerData.siegeData == null)
            return;

        // claim isn't already under the same siege
        if (playerData.siegeData.claims.contains(claim))
            return;

        // admin claims can't be sieged
        if (claim.isAdminClaim())
            return;

        // player must have some level of permission to be sieged in a claim
        if (claim.allowAccess(player.getWorld(), player) != null)
            return;

        // otherwise extend the siege
        playerData.siegeData.claims.add(claim);
        claim.siegeData = playerData.siegeData;
    }

    // deletes all claims owned by a player
    synchronized public void deleteClaimsForPlayer(UUID playerID, boolean deleteCreativeClaims) {
        // make a list of the player's claims
        ArrayList<Claim> claimsToDelete = new ArrayList<Claim>();
        for (Map.Entry<UUID, List<Claim>> mapEntry : this.worldClaims.entrySet()) {
            List<Claim> claimList = mapEntry.getValue();
            for (Claim claim : claimList) {
                if ((playerID == claim.ownerID || (playerID != null && playerID.equals(claim.ownerID)))
                        && (deleteCreativeClaims || !GriefPrevention.instance.claimModeIsActive(claim.getLesserBoundaryCorner().getExtent().getProperties(), ClaimsMode.Creative)))
                    claimsToDelete.add(claim);
            }
        }

        // delete them one by one
        for (int i = 0; i < claimsToDelete.size(); i++) {
            Claim claim = claimsToDelete.get(i);
            claim.removeSurfaceFluids(null);

            this.deleteClaim(claim, true);

            // if in a creative mode world, delete the claim
            if (GriefPrevention.instance.claimModeIsActive(claim.getLesserBoundaryCorner().getExtent().getProperties(), ClaimsMode.Creative)) {
                GriefPrevention.instance.restoreClaim(claim, 0);
            }
        }
    }

    // tries to resize a claim
    // see CreateClaim() for details on return value
    synchronized public CreateClaimResult resizeClaim(Claim claim, int newx1, int newx2, int newy1, int newy2, int newz1, int newz2,
            Player resizingPlayer) {
        // try to create this new claim, ignoring the original when checking for overlap
        CreateClaimResult result = this.createClaim(claim.getLesserBoundaryCorner().getExtent(), newx1, newx2, newy1, newy2, newz1, newz2,
                claim.ownerID, claim.parent, claim.id, resizingPlayer);

        // if succeeded
        if (result.succeeded) {
            // copy permissions from old claim
            ArrayList<String> builders = new ArrayList<String>();
            ArrayList<String> containers = new ArrayList<String>();
            ArrayList<String> accessors = new ArrayList<String>();
            ArrayList<String> managers = new ArrayList<String>();
            claim.getPermissions(builders, containers, accessors, managers);

            for (int i = 0; i < builders.size(); i++)
                result.claim.setPermission(builders.get(i), ClaimPermission.Build);

            for (int i = 0; i < containers.size(); i++)
                result.claim.setPermission(containers.get(i), ClaimPermission.Inventory);

            for (int i = 0; i < accessors.size(); i++)
                result.claim.setPermission(accessors.get(i), ClaimPermission.Access);

            for (int i = 0; i < managers.size(); i++) {
                result.claim.managers.add(managers.get(i));
            }

            // restore subdivisions
            for (Claim subdivision : claim.children) {
                subdivision.parent = result.claim;
                result.claim.children.add(subdivision);
            }

            // save those changes
            this.saveClaim(result.claim);

            // make original claim ineffective (it's still in the hash map, so let's make it ignored)
            claim.inDataStore = false;
            this.deleteClaim(claim);
        }

        return result;
    }

    private void loadMessages() {
        // initialize defaults
        this.addDefault(Messages.RespectingClaims, "Now respecting claims.");
        this.addDefault(Messages.IgnoringClaims, "Now ignoring claims.");
        this.addDefault(Messages.NoCreativeUnClaim,
                "You can't unclaim this land.  You can only make this claim larger or create additional claims.");
        this.addDefault(Messages.SuccessfulAbandon, "Claims abandoned.  You now have {0} available claim blocks.", "0: remaining blocks");
        this.addDefault(Messages.RestoreNatureActivate,
                "Ready to restore some nature!  Right click to restore nature, and use /BasicClaims to stop.");
        this.addDefault(Messages.RestoreNatureAggressiveActivate,
                "Aggressive mode activated.  Do NOT use this underneath anything you want to keep!  Right click to aggressively restore nature, and use /BasicClaims to stop.",
                null);
        this.addDefault(Messages.FillModeActive, "Fill mode activated with radius {0}.  Right click an area to fill.", "0: fill radius");
        this.addDefault(Messages.TransferClaimPermission, "That command requires the administrative claims permission.");
        this.addDefault(Messages.TransferClaimMissing, "There's no claim here.  Stand in the administrative claim you want to transfer.",
                null);
        this.addDefault(Messages.TransferClaimAdminOnly, "Only administrative claims may be transferred to a player.");
        this.addDefault(Messages.PlayerNotFound2, "No player by that name has logged in recently.");
        this.addDefault(Messages.TransferTopLevel,
                "Only top level claims (not subdivisions) may be transferred.  Stand outside of the subdivision and try again.");
        this.addDefault(Messages.TransferSuccess, "Claim transferred.");
        this.addDefault(Messages.TrustListNoClaim, "Stand inside the claim you're curious about.");
        this.addDefault(Messages.ClearPermsOwnerOnly, "Only the claim owner can clear all permissions.");
        this.addDefault(Messages.UntrustIndividualAllClaims,
                "Revoked {0}'s access to ALL your claims.  To set permissions for a single claim, stand inside it.", "0: untrusted player");
        this.addDefault(Messages.UntrustEveryoneAllClaims,
                "Cleared permissions in ALL your claims.  To set permissions for a single claim, stand inside it.");
        this.addDefault(Messages.NoPermissionTrust, "You don't have {0}'s permission to manage permissions here.", "0: claim owner's name");
        this.addDefault(Messages.ClearPermissionsOneClaim,
                "Cleared permissions in this claim.  To set permission for ALL your claims, stand outside them.");
        this.addDefault(Messages.UntrustIndividualSingleClaim,
                "Revoked {0}'s access to this claim.  To set permissions for a ALL your claims, stand outside them.", "0: untrusted player");
        this.addDefault(Messages.OnlySellBlocks, "Claim blocks may only be sold, not purchased.");
        this.addDefault(Messages.BlockPurchaseCost, "Each claim block costs {0}.  Your balance is {1}.",
                "0: cost of one block; 1: player's account balance");
        this.addDefault(Messages.ClaimBlockLimit, "You've reached your claim block limit.  You can't purchase more.");
        this.addDefault(Messages.InsufficientFunds, "You don't have enough money.  You need {0}, but you only have {1}.",
                "0: total cost; 1: player's account balance");
        this.addDefault(Messages.PurchaseConfirmation, "Withdrew {0} from your account.  You now have {1} available claim blocks.",
                "0: total cost; 1: remaining blocks");
        this.addDefault(Messages.OnlyPurchaseBlocks, "Claim blocks may only be purchased, not sold.");
        this.addDefault(Messages.BlockSaleValue, "Each claim block is worth {0}.  You have {1} available for sale.",
                "0: block value; 1: available blocks");
        this.addDefault(Messages.NotEnoughBlocksForSale, "You don't have that many claim blocks available for sale.");
        this.addDefault(Messages.BlockSaleConfirmation, "Deposited {0} in your account.  You now have {1} available claim blocks.",
                "0: amount deposited; 1: remaining blocks");
        this.addDefault(Messages.AdminClaimsMode,
                "Administrative claims mode active.  Any claims created will be free and editable by other administrators.");
        this.addDefault(Messages.BasicClaimsMode, "Returned to basic claim creation mode.");
        this.addDefault(Messages.SubdivisionMode,
                "Subdivision mode.  Use your shovel to create subdivisions in your existing claims.  Use /basicclaims to exit.");
        this.addDefault(Messages.SubdivisionVideo2, "Click for Subdivision Help: " + SUBDIVISION_VIDEO_URL_RAW);
        this.addDefault(Messages.DeleteClaimMissing, "There's no claim here.");
        this.addDefault(Messages.DeletionSubdivisionWarning,
                "This claim includes subdivisions.  If you're sure you want to delete it, use /DeleteClaim again.");
        this.addDefault(Messages.DeleteSuccess, "Claim deleted.");
        this.addDefault(Messages.CantDeleteAdminClaim, "You don't have permission to delete administrative claims.");
        this.addDefault(Messages.DeleteAllSuccess, "Deleted all of {0}'s claims.", "0: owner's name");
        this.addDefault(Messages.NoDeletePermission, "You don't have permission to delete claims.");
        this.addDefault(Messages.AllAdminDeleted, "Deleted all administrative claims.");
        this.addDefault(Messages.AdjustBlocksSuccess, "Adjusted {0}'s bonus claim blocks by {1}.  New total bonus blocks: {2}.",
                "0: player; 1: adjustment; 2: new total");
        this.addDefault(Messages.NotTrappedHere, "You can build here.  Save yourself.");
        this.addDefault(Messages.RescuePending, "If you stay put for 10 seconds, you'll be teleported out.  Please wait.");
        this.addDefault(Messages.NonSiegeWorld, "Siege is disabled here.");
        this.addDefault(Messages.AlreadySieging, "You're already involved in a siege.");
        this.addDefault(Messages.AlreadyUnderSiegePlayer, "{0} is already under siege.  Join the party!", "0: defending player");
        this.addDefault(Messages.NotSiegableThere, "{0} isn't protected there.", "0: defending player");
        this.addDefault(Messages.SiegeTooFarAway, "You're too far away to siege.");
        this.addDefault(Messages.NoSiegeDefenseless, "That player is defenseless.  Go pick on somebody else.");
        this.addDefault(Messages.AlreadyUnderSiegeArea, "That area is already under siege.  Join the party!");
        this.addDefault(Messages.NoSiegeAdminClaim, "Siege is disabled in this area.");
        this.addDefault(Messages.SiegeOnCooldown, "You're still on siege cooldown for this defender or claim.  Find another victim.");
        this.addDefault(Messages.SiegeAlert,
                "You're under siege!  If you log out now, you will die.  You must defeat {0}, wait for him to give up, or escape.",
                "0: attacker name");
        this.addDefault(Messages.SiegeConfirmed,
                "The siege has begun!  If you log out now, you will die.  You must defeat {0}, chase him away, or admit defeat and walk away.",
                "0: defender name");
        this.addDefault(Messages.AbandonClaimMissing, "Stand in the claim you want to delete, or consider /AbandonAllClaims.");
        this.addDefault(Messages.NotYourClaim, "This isn't your claim.");
        this.addDefault(Messages.DeleteTopLevelClaim,
                "To delete a subdivision, stand inside it.  Otherwise, use /AbandonTopLevelClaim to delete this claim and all subdivisions.");
        this.addDefault(Messages.AbandonSuccess, "Claim abandoned.  You now have {0} available claim blocks.", "0: remaining claim blocks");
        this.addDefault(Messages.CantGrantThatPermission, "You can't grant a permission you don't have yourself.");
        this.addDefault(Messages.GrantPermissionNoClaim, "Stand inside the claim where you want to grant permission.");
        this.addDefault(Messages.GrantPermissionConfirmation, "Granted {0} permission to {1} {2}.",
                "0: target player; 1: permission description; 2: scope (changed claims)");
        this.addDefault(Messages.ManageUniversalPermissionsInstruction, "To manage permissions for ALL your claims, stand outside them.");
        this.addDefault(Messages.ManageOneClaimPermissionsInstruction, "To manage permissions for a specific claim, stand inside it.");
        this.addDefault(Messages.CollectivePublic, "the public", "as in 'granted the public permission to...'");
        this.addDefault(Messages.BuildPermission, "build");
        this.addDefault(Messages.ContainersPermission, "access containers and animals");
        this.addDefault(Messages.AccessPermission, "use buttons and levers");
        this.addDefault(Messages.PermissionsPermission, "manage permissions");
        this.addDefault(Messages.LocationCurrentClaim, "in this claim");
        this.addDefault(Messages.LocationAllClaims, "in all your claims");
        this.addDefault(Messages.PvPImmunityStart, "You're protected from attack by other players as long as your inventory is empty.");
        this.addDefault(Messages.SiegeNoDrop, "You can't give away items while involved in a siege.");
        this.addDefault(Messages.DonateItemsInstruction, "To give away the item(s) in your hand, left-click the chest again.");
        this.addDefault(Messages.ChestFull, "This chest is full.");
        this.addDefault(Messages.DonationSuccess, "Item(s) transferred to chest!");
        this.addDefault(Messages.PlayerTooCloseForFire, "You can't start a fire this close to {0}.", "0: other player's name");
        this.addDefault(Messages.TooDeepToClaim, "This chest can't be protected because it's too deep underground.  Consider moving it.");
        this.addDefault(Messages.ChestClaimConfirmation, "This chest is protected.");
        this.addDefault(Messages.AutomaticClaimNotification, "This chest and nearby blocks are protected from breakage and theft.");
        this.addDefault(Messages.UnprotectedChestWarning,
                "This chest is NOT protected.  Consider using a golden shovel to expand an existing claim or to create a new one.");
        this.addDefault(Messages.ThatPlayerPvPImmune, "You can't injure defenseless players.");
        this.addDefault(Messages.CantFightWhileImmune, "You can't fight someone while you're protected from PvP.");
        this.addDefault(Messages.NoDamageClaimedEntity, "That belongs to {0}.", "0: owner name");
        this.addDefault(Messages.ShovelBasicClaimMode, "Shovel returned to basic claims mode.");
        this.addDefault(Messages.RemainingBlocks, "You may claim up to {0} more blocks.", "0: remaining blocks");
        this.addDefault(Messages.CreativeBasicsVideo2, "Click for Land Claim Help: " + CREATIVE_VIDEO_URL_RAW);
        this.addDefault(Messages.SurvivalBasicsVideo2, "Click for Land Claim Help: " + SURVIVAL_VIDEO_URL_RAW);
        this.addDefault(Messages.TrappedChatKeyword, "trapped",
                "When mentioned in chat, players get information about the /trapped command.");
        this.addDefault(Messages.TrappedInstructions, "Are you trapped in someone's land claim?  Try the /trapped command.");
        this.addDefault(Messages.PvPNoDrop, "You can't drop items while in PvP combat.");
        this.addDefault(Messages.SiegeNoTeleport, "You can't teleport out of a besieged area.");
        this.addDefault(Messages.BesiegedNoTeleport, "You can't teleport into a besieged area.");
        this.addDefault(Messages.SiegeNoContainers, "You can't access containers while involved in a siege.");
        this.addDefault(Messages.PvPNoContainers, "You can't access containers during PvP combat.");
        this.addDefault(Messages.PvPImmunityEnd, "Now you can fight with other players.");
        this.addDefault(Messages.NoBedPermission, "{0} hasn't given you permission to sleep here.", "0: claim owner");
        this.addDefault(Messages.NoWildernessBuckets, "You may only dump buckets inside your claim(s) or underground.");
        this.addDefault(Messages.NoLavaNearOtherPlayer, "You can't place lava this close to {0}.", "0: nearby player");
        this.addDefault(Messages.TooFarAway, "That's too far away.");
        this.addDefault(Messages.BlockNotClaimed, "No one has claimed this block.");
        this.addDefault(Messages.BlockClaimed, "That block has been claimed by {0}.", "0: claim owner");
        this.addDefault(Messages.SiegeNoShovel, "You can't use your shovel tool while involved in a siege.");
        this.addDefault(Messages.RestoreNaturePlayerInChunk, "Unable to restore.  {0} is in that chunk.", "0: nearby player");
        this.addDefault(Messages.NoCreateClaimPermission, "You don't have permission to claim land.");
        this.addDefault(Messages.ResizeClaimTooNarrow, "This new size would be too small.  Claims must be at least {0} blocks wide.",
                "0: minimum claim width");
        this.addDefault(Messages.ResizeNeedMoreBlocks, "You don't have enough blocks for this size.  You need {0} more.",
                "0: how many needed");
        this.addDefault(Messages.ClaimResizeSuccess, "Claim resized.  {0} available claim blocks remaining.", "0: remaining blocks");
        this.addDefault(Messages.ResizeFailOverlap, "Can't resize here because it would overlap another nearby claim.");
        this.addDefault(Messages.ResizeStart, "Resizing claim.  Use your shovel again at the new location for this corner.");
        this.addDefault(Messages.ResizeFailOverlapSubdivision,
                "You can't create a subdivision here because it would overlap another subdivision.  Consider /abandonclaim to delete it, or use your shovel at a corner to resize it.");
        this.addDefault(Messages.SubdivisionStart,
                "Subdivision corner set!  Use your shovel at the location for the opposite corner of this new subdivision.");
        this.addDefault(Messages.CreateSubdivisionOverlap, "Your selected area overlaps another subdivision.");
        this.addDefault(Messages.SubdivisionSuccess, "Subdivision created!  Use /trust to share it with friends.");
        this.addDefault(Messages.CreateClaimFailOverlap,
                "You can't create a claim here because it would overlap your other claim.  Use /abandonclaim to delete it, or use your shovel at a corner to resize it.");
        this.addDefault(Messages.CreateClaimFailOverlapOtherPlayer, "You can't create a claim here because it would overlap {0}'s claim.",
                "0: other claim owner");
        this.addDefault(Messages.ClaimsDisabledWorld, "Land claims are disabled in this world.");
        this.addDefault(Messages.ClaimStart,
                "Claim corner set!  Use the shovel again at the opposite corner to claim a rectangle of land.  To cancel, put your shovel away.");
        this.addDefault(Messages.NewClaimTooNarrow, "This claim would be too small.  Any claim must be at least {0} blocks wide.",
                "0: minimum claim width");
        this.addDefault(Messages.ResizeClaimInsufficientArea,
                "The selected claim size of {0} blocks({1}x{2}) would be too small. A claim must use at least {3} total claim blocks.");
        this.addDefault(Messages.CreateClaimInsufficientBlocks,
                "You don't have enough blocks to claim that entire area.  You need {0} more blocks.", "0: additional blocks needed");
        this.addDefault(Messages.AbandonClaimAdvertisement, "To delete another claim and free up some blocks, use /AbandonClaim.");
        this.addDefault(Messages.CreateClaimFailOverlapShort, "Your selected area overlaps an existing claim.");
        this.addDefault(Messages.CreateClaimSuccess, "Claim created!  Use /trust to share it with friends.");
        this.addDefault(Messages.SiegeWinDoorsOpen, "Congratulations!  Buttons and levers are temporarily unlocked (five minutes).");
        this.addDefault(Messages.RescueAbortedMoved, "You moved!  Rescue cancelled.");
        this.addDefault(Messages.SiegeDoorsLockedEjection, "Looting time is up!  Ejected from the claim.");
        this.addDefault(Messages.NoModifyDuringSiege, "Claims can't be modified while under siege.");
        this.addDefault(Messages.OnlyOwnersModifyClaims, "Only {0} can modify this claim.", "0: owner name");
        this.addDefault(Messages.NoBuildUnderSiege, "This claim is under siege by {0}.  No one can build here.", "0: attacker name");
        this.addDefault(Messages.NoBuildPvP, "You can't build in claims during PvP combat.");
        this.addDefault(Messages.NoBuildPermission, "You don't have {0}'s permission to build here.", "0: owner name");
        this.addDefault(Messages.NonSiegeMaterial, "That material is too tough to break.");
        this.addDefault(Messages.NoOwnerBuildUnderSiege, "You can't make changes while under siege.");
        this.addDefault(Messages.NoAccessPermission, "You don't have {0}'s permission to use that.",
                "0: owner name.  access permission controls buttons, levers, and beds");
        this.addDefault(Messages.NoContainersSiege, "This claim is under siege by {0}.  No one can access containers here right now.",
                "0: attacker name");
        this.addDefault(Messages.NoContainersPermission, "You don't have {0}'s permission to use that.",
                "0: owner's name.  containers also include crafting blocks");
        this.addDefault(Messages.OwnerNameForAdminClaims, "an administrator",
                "as in 'You don't have an administrator's permission to build here.'");
        this.addDefault(Messages.ClaimTooSmallForEntities, "This claim isn't big enough for that.  Try enlarging it.");
        this.addDefault(Messages.TooManyEntitiesInClaim,
                "This claim has too many entities already.  Try enlarging the claim or removing some animals, monsters, paintings, or minecarts.");
        this.addDefault(Messages.YouHaveNoClaims, "You don't have any land claims.");
        this.addDefault(Messages.ConfirmFluidRemoval,
                "Abandoning this claim will remove lava inside the claim.  If you're sure, use /AbandonClaim again.");
        this.addDefault(Messages.AutoBanNotify, "Auto-banned {0}({1}).  See logs for details.");
        this.addDefault(Messages.AdjustGroupBlocksSuccess,
                "Adjusted bonus claim blocks for players with the {0} permission by {1}.  New total: {2}.",
                "0: permission; 1: adjustment amount; 2: new total bonus");
        this.addDefault(Messages.InvalidPermissionID, "Please specify a player name, or a permission in [brackets].");
        this.addDefault(Messages.HowToClaimRegex, "(^|.*\\W)how\\W.*\\W(claim|protect|lock)(\\W.*|$)",
                "This is a Java Regular Expression.  Look it up before editing!  It's used to tell players about the demo video when they ask how to claim land.");
        this.addDefault(Messages.NoBuildOutsideClaims, "You can't build here unless you claim some land first.");
        this.addDefault(Messages.PlayerOfflineTime, "  Last login: {0} days ago.", "0: number of full days since last login");
        this.addDefault(Messages.BuildingOutsideClaims,
                "Other players can build here, too.  Consider creating a land claim to protect your work!");
        this.addDefault(Messages.TrappedWontWorkHere,
                "Sorry, unable to find a safe location to teleport you to.  Contact an admin, or consider /kill if you don't want to wait.");
        this.addDefault(Messages.CommandBannedInPvP, "You can't use that command while in PvP combat.");
        this.addDefault(Messages.UnclaimCleanupWarning,
                "The land you've unclaimed may be changed by other players or cleaned up by administrators.  If you've built something there you want to keep, you should reclaim it.");
        this.addDefault(Messages.BuySellNotConfigured, "Sorry, buying anhd selling claim blocks is disabled.");
        this.addDefault(Messages.NoTeleportPvPCombat, "You can't teleport while fighting another player.");
        this.addDefault(Messages.NoTNTDamageAboveSeaLevel, "Warning: TNT will not destroy blocks above sea level.");
        this.addDefault(Messages.NoTNTDamageClaims, "Warning: TNT will not destroy claimed blocks.");
        this.addDefault(Messages.IgnoreClaimsAdvertisement, "To override, use /IgnoreClaims.");
        this.addDefault(Messages.NoPermissionForCommand, "You don't have permission to do that.");
        this.addDefault(Messages.ClaimsListNoPermission, "You don't have permission to get information about another player's land claims.");
        this.addDefault(Messages.ExplosivesDisabled, "This claim is now protected from explosions.  Use /ClaimExplosions again to disable.");
        this.addDefault(Messages.ExplosivesEnabled,
                "This claim is now vulnerable to explosions.  Use /ClaimExplosions again to re-enable protections.");
        this.addDefault(Messages.ClaimExplosivesAdvertisement,
                "To allow explosives to destroy blocks in this land claim, use /ClaimExplosions.");
        this.addDefault(Messages.PlayerInPvPSafeZone, "That player is in a PvP safe zone.");
        this.addDefault(Messages.NoPistonsOutsideClaims, "Warning: Pistons won't move blocks outside land claims.");
        this.addDefault(Messages.SoftMuted, "Soft-muted {0}.", "0: The changed player's name.");
        this.addDefault(Messages.UnSoftMuted, "Un-soft-muted {0}.", "0: The changed player's name.");
        this.addDefault(Messages.DropUnlockAdvertisement, "Other players can't pick up your dropped items unless you /UnlockDrops first.");
        this.addDefault(Messages.PickupBlockedExplanation, "You can't pick this up unless {0} uses /UnlockDrops.",
                "0: The item stack's owner.");
        this.addDefault(Messages.DropUnlockConfirmation, "Unlocked your drops.  Other players may now pick them up (until you die again).");
        this.addDefault(Messages.AdvertiseACandACB,
                "You may use /ACB to give yourself more claim blocks, or /AdminClaims to create a free administrative claim.");
        this.addDefault(Messages.AdvertiseAdminClaims,
                "You could create an administrative land claim instead using /AdminClaims, which you'd share with other administrators.");
        this.addDefault(Messages.AdvertiseACB, "You may use /ACB to give yourself more claim blocks.");
        this.addDefault(Messages.NotYourPet, "That belongs to {0} until it's given to you with /GivePet.", "0: owner name");
        this.addDefault(Messages.PetGiveawayConfirmation, "Pet transferred.");
        this.addDefault(Messages.PetTransferCancellation, "Pet giveaway cancelled.");
        this.addDefault(Messages.ReadyToTransferPet,
                "Ready to transfer!  Right-click the pet you'd like to give away, or cancel with /GivePet cancel.");
        this.addDefault(Messages.AvoidGriefClaimLand, "Prevent grief!  If you claim your land, you will be grief-proof.");
        this.addDefault(Messages.BecomeMayor, "Subdivide your land claim and become a mayor!");
        this.addDefault(Messages.ClaimCreationFailedOverClaimCountLimit,
                "You've reached your limit on land claims.  Use /AbandonClaim to remove one before creating another.");
        this.addDefault(Messages.CreateClaimFailOverlapRegion, "You can't claim all of this because you're not allowed to build here.");
        this.addDefault(Messages.ResizeFailOverlapRegion, "You don't have permission to build there, so you can't claim that area.");
        this.addDefault(Messages.NoBuildPortalPermission,
                "You can't use this portal because you don't have {0}'s permission to build an exit portal in the destination land claim.",
                "0: Destination land claim owner's name.");
        this.addDefault(Messages.ShowNearbyClaims, "Found {0} land claims.", "0: Number of claims found.");
        this.addDefault(Messages.NoChatUntilMove,
                "Sorry, but you have to move a little more before you can chat.  We get lots of spam bots here.  :)");
        this.addDefault(Messages.SiegeImmune, "That player is immune to /siege.");
        this.addDefault(Messages.SetClaimBlocksSuccess, "Updated accrued claim blocks.");
        this.addDefault(Messages.IgnoreConfirmation, "You're now ignoring chat messages from that player.");
        this.addDefault(Messages.UnIgnoreConfirmation, "You're no longer ignoring chat messages from that player.");
        this.addDefault(Messages.NotIgnoringPlayer, "You're not ignoring that player.");
        this.addDefault(Messages.SeparateConfirmation, "Those players will now ignore each other in chat.");
        this.addDefault(Messages.UnSeparateConfirmation, "Those players will no longer ignore each other in chat.");
        this.addDefault(Messages.NotIgnoringAnyone, "You're not ignoring anyone.");
        this.addDefault(Messages.TrustListHeader, "Explicit permissions here:");
        this.addDefault(Messages.Manage, "Manage");
        this.addDefault(Messages.Build, "Build");
        this.addDefault(Messages.Containers, "Containers");
        this.addDefault(Messages.Access, "Access");
        this.addDefault(Messages.StartBlockMath, "{0} blocks from play + {1} bonus = {2} total.");
        this.addDefault(Messages.ClaimsListHeader, "Claims:");
        this.addDefault(Messages.ContinueBlockMath, " (-{0} blocks)");
        this.addDefault(Messages.EndBlockMath, " = {0} blocks left to spend");
        this.addDefault(Messages.NoClaimDuringPvP, "You can't claim lands during PvP combat.");
        this.addDefault(Messages.UntrustAllOwnerOnly, "Only the claim owner can clear all its permissions.");
        this.addDefault(Messages.ManagersDontUntrustManagers, "Only the claim owner can demote a manager.");
        this.addDefault(Messages.PlayerNotIgnorable, "You can't ignore that player.");

        this.addDefault(Messages.BookAuthor, "BigScary");
        this.addDefault(Messages.BookTitle, "How to Claim Land");
        this.addDefault(Messages.BookLink, "Click: " + SURVIVAL_VIDEO_URL_RAW);
        this.addDefault(Messages.BookIntro,
                "Claim land to protect your stuff!  Click the link above to learn land claims in 3 minutes or less.  :)");
        this.addDefault(Messages.BookTools, "Our claim tools are {0} and {1}.",
                "0: claim modification tool name; 1:claim information tool name");
        this.addDefault(Messages.BookDisabledChestClaims, "  On this server, placing a chest will NOT claim land for you.");
        this.addDefault(Messages.BookUsefulCommands, "Useful Commands:");
        this.addDefault(Messages.NoProfanity, "Please moderate your language.");
        this.addDefault(Messages.NoDropsAllowed, "You can't drop items in this claim.");

        // load the config file
        try
        {
            HoconConfigurationLoader configurationLoader = HoconConfigurationLoader.builder().setPath(messagesFilePath).build();
            CommentedConfigurationNode mainNode = configurationLoader.load();

            // for each message ID
            for (CustomizableMessage messageData : this.messages.values()) 
            {
                // if default is missing, log an error and use some fake data for
                // now so that the plugin can run
                if(messageData == null)
                {
                    GriefPrevention.AddLogEntry("Missing message for " + messageData.id + ".  Please contact the developer.");
                    messageData =
                            new CustomizableMessage(messageData.id, "Missing message!  ID: " + messageData.id + ".  Please contact a server admin.", null);
                }

                // read the message from the file, use default if necessary
                if (mainNode.getNode("Messages", messageData.id.name(), "Text").isVirtual()) {
                    mainNode.getNode("Messages", messageData.id.name(), "Text").setValue(messageData.text);
                } else {
                    messageData.text = mainNode.getNode("Messages", messageData.id.name(), "Text").getString();
                }

                if(messageData.notes != null)
                {
                    if (mainNode.getNode("Messages", messageData.id.name(), "Notes").isVirtual()) {
                        mainNode.getNode("Messages", messageData.id.name(), "Notes").setValue(messageData.notes);
                    } else {
                        messageData.notes = mainNode.getNode("Messages", messageData.id.name(), "Notes").getString();
                    }
                }
            }

            // save any changes
            configurationLoader.save(mainNode);

        } catch (Exception e) {
            e.printStackTrace();
            GriefPrevention.AddLogEntry("Unable to write to the configuration file at \"" + DataStore.messagesFilePath + "\"");
        }
    }

    private void addDefault(Messages id, String text) {
        this.addDefault(id, text, null);
    }

    private void addDefault(Messages id, String text, String notes) {
        CustomizableMessage message = new CustomizableMessage(id, text, notes);
        this.messages.put(id, message);
    }

    public String getMessage(Messages messageID, String... args) {
        if (this.messages.get(messageID) == null) {
            return null;
        }

        String message = this.messages.get(messageID).text;

        for (int i = 0; i < args.length; i++) {
            String param = args[i];
            message = message.replace("{" + i + "}", param);
        }

        return message;
    }

    public Text parseMessage(Messages messageID, TextColor color, String...args) {
        String message = GriefPrevention.instance.dataStore.getMessage(messageID, args);
        Text textMessage = Text.of(color, message);
        List<String> urls = extractUrls(message);
        if (urls.isEmpty()) {
            return textMessage;
        }

        Iterator<String> iterator = urls.iterator();
        while (iterator.hasNext()) {
            String url = iterator.next();
            String msgPart = StringUtils.substringBefore(message, url);

            if (msgPart != null && !msgPart.equals("")) {
                try {
                    textMessage = Text.of(color, msgPart, TextColors.GREEN, TextActions.openUrl(new URL(url)), url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    return Text.of(message);
                }
            }

            iterator.remove();
            message = StringUtils.substringAfter(message, url);
        }

        if (message != null && !message.equals("")) {
            textMessage = Text.of(textMessage, " ", color, message);
        }

        return textMessage;
    }

    /**
     * Returns a list with all links contained in the input
     */
    public static List<String> extractUrls(String text) {
        List<String> containedUrls = new ArrayList<String>();
        String urlRegex = "((https?|ftp|gopher|telnet|file):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)";
        Pattern pattern = Pattern.compile(urlRegex, Pattern.CASE_INSENSITIVE);
        Matcher urlMatcher = pattern.matcher(text);

        while (urlMatcher.find())
        {
            containedUrls.add(text.substring(urlMatcher.start(0),
                    urlMatcher.end(0)));
        }

        return containedUrls;
    }

    // used in updating the data schema from 0 to 1.
    // converts player names in a list to uuids
    protected List<String> convertNameListToUUIDList(List<String> names) {
        // doesn't apply after schema has been updated to version 1
        if (this.getSchemaVersion() >= 1)
            return names;

        // list to build results
        List<String> resultNames = new ArrayList<String>();

        for (String name : names) {
            // skip non-player-names (groups and "public"), leave them as-is
            if (name.startsWith("[") || name.equals("public")) {
                resultNames.add(name);
                continue;
            }

            // otherwise try to convert to a UUID
            Optional<User> player = Optional.empty();
            try {
                player = Sponge.getGame().getServiceManager().provide(UserStorageService.class).get().get(name);
            } catch (Exception ex) {
            }

            // if successful, replace player name with corresponding UUID
            if (player.isPresent()) {
                resultNames.add(player.get().getUniqueId().toString());
            }
        }

        return resultNames;
    }

    // gets all the claims "near" a location
    public Set<Claim> getNearbyClaims(Location<World> location) {
        Set<Claim> claims = new HashSet<Claim>();

        Optional<Chunk> lesserChunk = location.getExtent().getChunk(location.sub(150, 0, 150).getBlockPosition());
        Optional<Chunk> greaterChunk = location.getExtent().getChunk(location.add(300, 0, 300).getBlockPosition());

        if (lesserChunk.isPresent() && greaterChunk.isPresent()) {
            for (int chunk_x = lesserChunk.get().getPosition().getX(); chunk_x <= greaterChunk.get().getPosition().getX(); chunk_x++) {
                for (int chunk_z = lesserChunk.get().getPosition().getZ(); chunk_z <= greaterChunk.get().getPosition().getZ(); chunk_z++) {
                    Optional<Chunk> chunk = location.getExtent().getChunk(chunk_x, 0, chunk_z);
                    if (chunk.isPresent()) {
                        String chunkID = this.getChunkString(chunk.get().getWorld().getLocation(0, 0, 0));
                        ArrayList<Claim> claimsInChunk = this.chunksToClaimsMap.get(chunkID);
                        if (claimsInChunk != null) {
                            claims.addAll(claimsInChunk);
                        }
                    }
                }
            }
        }

        return claims;
    }

    public abstract PlayerData createPlayerWorldStorageData(WorldProperties worldProperties, UUID playerUUID);
}
