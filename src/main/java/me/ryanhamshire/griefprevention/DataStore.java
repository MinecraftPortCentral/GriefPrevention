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

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimContexts;
import me.ryanhamshire.griefprevention.api.claim.ClaimResult;
import me.ryanhamshire.griefprevention.api.claim.ClaimResultType;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.claim.ClaimsMode;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.claim.GPClaimManager;
import me.ryanhamshire.griefprevention.claim.GPClaimResult;
import me.ryanhamshire.griefprevention.configuration.ClaimTemplateStorage;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.configuration.type.DimensionConfig;
import me.ryanhamshire.griefprevention.configuration.type.GlobalConfig;
import me.ryanhamshire.griefprevention.configuration.type.WorldConfig;
import me.ryanhamshire.griefprevention.event.GPDeleteClaimEvent;
import me.ryanhamshire.griefprevention.permission.GPOptions;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import me.ryanhamshire.griefprevention.util.WordFinder;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.SubjectData;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
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
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

//singleton class which manages all GriefPrevention data (except for config options)
public abstract class DataStore {

    // World UUID -> PlayerDataWorldManager
    protected final Map<UUID, GPClaimManager> claimWorldManagers = Maps.newHashMap();

    // in-memory cache for claim data
    public static Map<UUID, GriefPreventionConfig<DimensionConfig>> dimensionConfigMap = Maps.newHashMap();
    public static Map<UUID, GriefPreventionConfig<WorldConfig>> worldConfigMap = Maps.newHashMap();
    public static Map<String, ClaimTemplateStorage> globalTemplates = new HashMap<>();
    public static GriefPreventionConfig<GlobalConfig> globalConfig;
    public static Map<UUID, GPPlayerData> GLOBAL_PLAYER_DATA = Maps.newHashMap();
    public static boolean USE_GLOBAL_PLAYER_STORAGE = true;
    public static Map<ClaimType, Map<String, Boolean>> CLAIM_FLAG_DEFAULTS = Maps.newHashMap();

    // pattern for unique user identifiers (UUIDs)
    protected final static Pattern uuidpattern = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    // path information, for where stuff stored on disk is well... stored
    public final static Path dataLayerFolderPath = GriefPreventionPlugin.instance.getConfigPath();
    public final static Path globalPlayerDataPath = dataLayerFolderPath.resolve("GlobalPlayerData");
    final static Path messagesFilePath = dataLayerFolderPath.resolve("message.conf");
    final static Path softMuteFilePath = dataLayerFolderPath.resolve("softMute.txt");
    final static Path bannedWordsFilePath = dataLayerFolderPath.resolve("bannedWords.txt");

    // the latest version of the data schema implemented here
    protected static final int latestSchemaVersion = 2;

    // reading and writing the schema version to the data store
    abstract int getSchemaVersionFromStorage();

    abstract void updateSchemaVersionInStorage(int versionToSet);

    // current version of the schema of data in secondary storage
    private int currentSchemaVersion = -1; // -1 means not determined yet

    // video links
    public static final String SURVIVAL_VIDEO_URL_RAW = "http://bit.ly/mcgpuser";
    static final String CREATIVE_VIDEO_URL_RAW = "http://bit.ly/mcgpcrea";
    static final String SUBDIVISION_VIDEO_URL_RAW = "http://bit.ly/mcgpsub";

    public static boolean generateMessages = true;
    // matcher for banned words
    public WordFinder bannedWordFinder;

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
        // ensure global player data folder exists
        USE_GLOBAL_PLAYER_STORAGE = GriefPreventionPlugin.getGlobalConfig().getConfig().playerdata.useGlobalPlayerDataStorage;
        if (USE_GLOBAL_PLAYER_STORAGE) {
            File globalPlayerDataFolder = globalPlayerDataPath.toFile();
            if (!globalPlayerDataFolder.exists()) {
                globalPlayerDataFolder.mkdirs();
            }
        }

        // load up all the messages from messages.hocon
       // this.loadMessages();
        this.loadBannedWords();
        GriefPreventionPlugin.addLogEntry("Customizable messages loaded.");

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
                        GriefPreventionPlugin.addLogEntry("Failed to parse soft mute entry as a UUID: " + nextID);
                    }

                    // push it into the map
                    if (playerID != null) {
                        this.softMuteMap.put(playerID, true);
                    }

                    // move to the next
                    nextID = inStream.readLine();
                }
            } catch (Exception e) {
                GriefPreventionPlugin.addLogEntry("Failed to read from the soft mute data file: " + e.toString());
                e.printStackTrace();
            }

            try {
                if (inStream != null) {
                    inStream.close();
                }
            } catch (IOException exception) {
            }
        }
    }

    public void loadBannedWords() {
        try {
            File bannedWordsFile = bannedWordsFilePath.toFile();
            boolean regenerateDefaults = false;
            if (!bannedWordsFile.exists()) {
                Files.touch(bannedWordsFile);
                regenerateDefaults = true;
            }

            List<String> bannedWords = Files.readLines(bannedWordsFile, Charset.forName("UTF-8"));
            if (regenerateDefaults || bannedWords.isEmpty()) {
                String defaultWords =
                        "nigger\nniggers\nniger\nnigga\nnigers\nniggas\n" +
                                "fag\nfags\nfaggot\nfaggots\nfeggit\nfeggits\nfaggit\nfaggits\n" +
                                "cunt\ncunts\nwhore\nwhores\nslut\nsluts\n";
                Files.write(defaultWords, bannedWordsFile, Charset.forName("UTF-8"));
            }
            this.bannedWordFinder = new WordFinder(Files.readLines(bannedWordsFile, Charset.forName("UTF-8")));
        } catch (Exception e) {
            GriefPreventionPlugin.addLogEntry("Failed to read from the banned words data file: " + e.toString());
            e.printStackTrace();
            this.bannedWordFinder = new WordFinder(new ArrayList<String>());
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
            GriefPreventionPlugin.addLogEntry("Unexpected exception saving soft mute data: " + e.getMessage());
            e.printStackTrace();
        }

        // close the file
        try {
            if (outStream != null) {
                outStream.close();
            }
        } catch (IOException exception) {
        }
    }

    // removes cached player data from memory
    public void clearCachedPlayerData(WorldProperties worldProperties, UUID playerUniqueId) {
        this.getClaimWorldManager(worldProperties).removePlayer(playerUniqueId);
    }

    public abstract void writeClaimToStorage(GPClaim claim);

    public abstract void deleteClaimFromSecondaryStorage(GPClaim claim);

    // finds a claim by ID
    public Claim getClaim(WorldProperties worldProperties, UUID id) {
        return this.getClaimWorldManager(worldProperties).getClaimByUUID(id).orElse(null);
    }

    public void asyncSaveGlobalPlayerData(UUID playerID, GPPlayerData playerData) {
        // save everything except the ignore list
        this.overrideSavePlayerData(playerID, playerData);

        // save the ignore list
        if (playerData.ignoreListChanged) {
            StringBuilder fileContent = new StringBuilder();
            try {
                for (UUID uuidKey : playerData.ignoredPlayers.keySet()) {
                    Boolean value = playerData.ignoredPlayers.get(uuidKey);
                    if (value == null) {
                        continue;
                    }

                    // admin-enforced ignores begin with an asterisk
                    if (value) {
                        fileContent.append("*");
                    }

                    fileContent.append(uuidKey);
                    fileContent.append("\n");
                }

                // write data to file
                File playerDataFile = globalPlayerDataPath.resolve(playerID.toString() + ".ignore").toFile();
                Files.write(fileContent.toString().trim().getBytes("UTF-8"), playerDataFile);
            }

            // if any problem, log it
            catch (Exception e) {
                GriefPreventionPlugin.addLogEntry(
                        "GriefPrevention: Unexpected exception saving data for player \"" + playerID.toString() + "\": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    abstract void overrideSavePlayerData(UUID playerID, GPPlayerData playerData);

    // extends a claim to a new depth
    // respects the max depth config variable
    public void extendClaim(GPClaim claim, int newDepth) {
        if (newDepth < GriefPreventionPlugin.getActiveConfig(claim.world.getProperties()).getConfig().claim.maxClaimDepth) {
            newDepth = GriefPreventionPlugin.getActiveConfig(claim.world.getProperties()).getConfig().claim.maxClaimDepth;
        }

        if (claim.isSubdivision()) {
            claim = claim.parent;
        }

        // adjust to new depth
        Vector3d newLesserPosition = new Vector3d(claim.lesserBoundaryCorner.getX(), newDepth, claim.lesserBoundaryCorner.getZ());
        claim.lesserBoundaryCorner = claim.lesserBoundaryCorner.setPosition(newLesserPosition);
        Vector3d newGreaterPosition = new Vector3d(claim.greaterBoundaryCorner.getX(), newDepth, claim.greaterBoundaryCorner.getZ());
        claim.greaterBoundaryCorner = claim.greaterBoundaryCorner.setPosition(newGreaterPosition);
        if (claim.parent == null) {
            
        }

        for (Claim subClaim : claim.children) {
            GPClaim subdivision = (GPClaim) subClaim;
            newLesserPosition = new Vector3d(subdivision.lesserBoundaryCorner.getX(), newDepth, subdivision.lesserBoundaryCorner.getZ());
            subdivision.lesserBoundaryCorner = subdivision.lesserBoundaryCorner.setPosition(newLesserPosition);
            newGreaterPosition = new Vector3d(subdivision.greaterBoundaryCorner.getX(), newDepth, subdivision.greaterBoundaryCorner.getZ());
            subdivision.greaterBoundaryCorner = subdivision.greaterBoundaryCorner.setPosition(newGreaterPosition);
        }

        claim.updateClaimStorageData();
    }

    public ClaimResult createAdminClaim(Claim parent, World world, Vector3i point1, Vector3i point2, ClaimType claimType, UUID ownerUniqueId, boolean cuboid, Cause cause) {
        GPClaimManager claimManager = this.getClaimWorldManager(world.getProperties());
        ClaimResult claimResult = Claim.builder()
                .bounds(point1, point2)
                .world(world)
                .type(claimType)
                .owner(ownerUniqueId)
                .parent(parent)
                .cause(cause)
                .build();
        if(claimResult.successful()) {
            claimManager.addClaim(claimResult.getClaim().get(), true);
        }
        return claimResult;
    }

    public ClaimResult createClaim(World world, Vector3i point1, Vector3i point2, ClaimType claimType, UUID ownerUniqueId, boolean cuboid, Cause cause) {
        return createClaim(world, point1, point2, claimType, ownerUniqueId, cuboid, null, cause);
    }

    public ClaimResult createClaim(World world, Vector3i point1, Vector3i point2, ClaimType claimType, UUID ownerUniqueId, boolean cuboid, Claim parent, Cause cause) {
        GPClaimManager claimManager = this.getClaimWorldManager(world.getProperties());
        ClaimResult claimResult = Claim.builder()
                .bounds(point1, point2)
                .world(world)
                .type(claimType)
                .owner(ownerUniqueId)
                .parent(parent)
                .cause(cause)
                .build();
        if(claimResult.successful()) {
            claimManager.addClaim(claimResult.getClaim().get(), true);
        }
        return claimResult;
    }

    public ClaimResult deleteAllAdminClaims(CommandSource src, World world) {
        GPClaimManager claimWorldManager = this.claimWorldManagers.get(world.getProperties().getUniqueId());
        if (claimWorldManager == null) {
            return new GPClaimResult(ClaimResultType.CLAIMS_DISABLED);
        }

        List<Claim> claimsToDelete = new ArrayList<Claim>();
        boolean adminClaimFound = false;
        for (Claim claim : claimWorldManager.getWorldClaims()) {
            if (claim.isAdminClaim()) {
                claimsToDelete.add(claim);
                adminClaimFound = true;
            }
        }

        if (!adminClaimFound) {
            return new GPClaimResult(ClaimResultType.CLAIM_NOT_FOUND);
        }

        GPDeleteClaimEvent event = new GPDeleteClaimEvent(ImmutableList.copyOf(claimsToDelete), Cause.of(NamedCause.source(src)));
        Sponge.getEventManager().post(event);
        if (event.isCancelled()) {
            return new GPClaimResult(ClaimResultType.CLAIM_EVENT_CANCELLED, event.getMessage().orElse(Text.of("Could not delete all admin claims. A plugin has denied it.")));
        }

        for (Claim claim : claimsToDelete) {
            GPClaim gpClaim = (GPClaim) claim;
            gpClaim.removeSurfaceFluids(null);

            GriefPreventionPlugin.GLOBAL_SUBJECT.getSubjectData().clearPermissions(ImmutableSet.of(claim.getContext()));
            claimWorldManager.deleteClaim(claim, true);

            // if in a creative mode world, delete the claim
            if (GriefPreventionPlugin.instance.claimModeIsActive(claim.getLesserBoundaryCorner().getExtent().getProperties(), ClaimsMode.Creative)) {
                GriefPreventionPlugin.instance.restoreClaim((GPClaim) claim, 0);
            }
        }

        return new GPClaimResult(claimsToDelete, ClaimResultType.SUCCESS);
    }

    public ClaimResult deleteClaim(Claim claim, Cause cause, boolean deleteChildren) {
        GPClaimManager claimManager = this.getClaimWorldManager(claim.getWorld().getProperties());
        return claimManager.deleteClaim(claim, cause, deleteChildren);
    }

    // deletes all claims owned by a player
    public void deleteClaimsForPlayer(UUID playerID) {
        if (DataStore.USE_GLOBAL_PLAYER_STORAGE && playerID != null) {
            List<Claim> claimsToDelete = new ArrayList<>(DataStore.GLOBAL_PLAYER_DATA.get(playerID).getInternalClaims());
            for (Claim claim : claimsToDelete) {
                ((GPClaim) claim).removeSurfaceFluids(null);
                GriefPreventionPlugin.GLOBAL_SUBJECT.getSubjectData().clearPermissions(ImmutableSet.of(claim.getContext()));
                GPClaimManager claimWorldManager = this.claimWorldManagers.get(claim.getWorld().getProperties().getUniqueId());
                claimWorldManager.deleteClaim(claim, true);

                // if in a creative mode world, delete the claim
                if (GriefPreventionPlugin.instance.claimModeIsActive(claim.getLesserBoundaryCorner().getExtent().getProperties(), ClaimsMode.Creative)) {
                    GriefPreventionPlugin.instance.restoreClaim((GPClaim) claim, 0);
                }
            }
            return;
        }

        for (GPClaimManager claimWorldManager : this.claimWorldManagers.values()) {
            List<Claim> claims = claimWorldManager.getInternalPlayerClaims(playerID);
            if (playerID == null) {
                claims = claimWorldManager.getWorldClaims();
            }
            if (claims == null) {
                continue;
            }

            List<Claim> claimsToDelete = new ArrayList<Claim>();
            for (Claim claim : claims) {
                if (!claim.isAdminClaim()) {
                    claimsToDelete.add(claim);
                }
            }
 
            for (Claim claim : claimsToDelete) {
                ((GPClaim) claim).removeSurfaceFluids(null);
                GriefPreventionPlugin.GLOBAL_SUBJECT.getSubjectData().clearPermissions(ImmutableSet.of(claim.getContext()));
                claimWorldManager.deleteClaim(claim, true);

                // if in a creative mode world, delete the claim
                if (GriefPreventionPlugin.instance.claimModeIsActive(claim.getLesserBoundaryCorner().getExtent().getProperties(), ClaimsMode.Creative)) {
                    GriefPreventionPlugin.instance.restoreClaim((GPClaim) claim, 0);
                }
            }
        }
    }

    // used in updating the data schema from 0 to 1.
    // converts player names in a list to uuids
    protected List<String> convertNameListToUUIDList(List<String> names) {
        // doesn't apply after schema has been updated to version 1
        if (this.getSchemaVersion() >= 1) {
            return names;
        }

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
    public List<Claim> getNearbyClaims(Location<World> location) {
        List<Claim> claims = new ArrayList<>();
        GPClaimManager claimWorldManager = this.getClaimWorldManager(location.getExtent().getProperties());
        if (claimWorldManager == null) {
            return claims;
        }

        Optional<Chunk> lesserChunk = location.getExtent().getChunkAtBlock(location.sub(50, 0, 50).getBlockPosition());
        Optional<Chunk> greaterChunk = location.getExtent().getChunkAtBlock(location.add(50, 0, 50).getBlockPosition());

        if (lesserChunk.isPresent() && greaterChunk.isPresent()) {
            for (int chunkX = lesserChunk.get().getPosition().getX(); chunkX <= greaterChunk.get().getPosition().getX(); chunkX++) {
                for (int chunkZ = lesserChunk.get().getPosition().getZ(); chunkZ <= greaterChunk.get().getPosition().getZ(); chunkZ++) {
                    Optional<Chunk> chunk = location.getExtent().getChunk(chunkX, 0, chunkZ);
                    if (chunk.isPresent()) {
                        Set<Claim> claimsInChunk = claimWorldManager.getInternalChunksToClaimsMap().get(ChunkPos.asLong(chunkX, chunkZ));
                        if (claimsInChunk != null) {
                            for (Claim claim : claimsInChunk) {
                                final GPClaim gpClaim = (GPClaim) claim;
                                if (gpClaim.parent == null && !claims.contains(claim)) {
                                    claims.add(claim);
                                }
                            }
                        }
                    }
                }
            }
        }

        return claims;
    }

    public GPClaim getClaimAtPlayer(GPPlayerData playerData, Location<World> location) {
        return this.getClaimAtPlayer(playerData, location, false);
    }

    public GPClaim getClaimAtPlayer(GPPlayerData playerData, Location<World> location, boolean ignoreHeight) {
        GPClaimManager claimManager = this.getClaimWorldManager(location.getExtent().getProperties());
        return (GPClaim) claimManager.getClaimAtPlayer(playerData, location, ignoreHeight);
    }

    public GPClaim getClaimAt(Location<World> location) {
        return this.getClaimAt(location, false);
    }

    public GPClaim getClaimAt(Location<World> location, boolean ignoreHeight) {
        GPClaimManager claimManager = this.getClaimWorldManager(location.getExtent().getProperties());
        return (GPClaim) claimManager.getClaimAt(location, ignoreHeight);
    }

    public GPClaim getClaimAt(Location<World> location, boolean ignoreHeight, GPClaim cachedClaim) {
        GPClaimManager claimManager = this.getClaimWorldManager(location.getExtent().getProperties());
        return (GPClaim) claimManager.getClaimAt(location, ignoreHeight, cachedClaim);
    }

    public GPPlayerData getPlayerData(World world, UUID playerUniqueId) {
        return this.getPlayerData(world.getProperties(), playerUniqueId);
    }

    public GPPlayerData getPlayerData(WorldProperties worldProperties, UUID playerUniqueId) {
        GPPlayerData playerData = null;
        GPClaimManager claimWorldManager = this.getClaimWorldManager(worldProperties);
        playerData = claimWorldManager.getPlayerDataMap().get(playerUniqueId);
        return playerData;
    }

    // retrieves player data from memory or secondary storage, as necessary
    // if the player has never been on the server before, this will return a
    // fresh player data with default values
    public GPPlayerData getOrCreatePlayerData(World world, UUID playerUniqueId) {
        return getOrCreatePlayerData(world.getProperties(), playerUniqueId);
    }

    public GPPlayerData getOrCreatePlayerData(WorldProperties worldProperties, UUID playerUniqueId) {
        GPClaimManager claimWorldManager = this.getClaimWorldManager(worldProperties);
        return claimWorldManager.getOrCreatePlayerData(playerUniqueId);
    }

    public void removePlayerData(WorldProperties worldProperties, UUID playerUniqueId) {
        GPClaimManager claimWorldManager = this.getClaimWorldManager(worldProperties);
        claimWorldManager.removePlayer(playerUniqueId);
    }

    public GPClaimManager getClaimWorldManager(WorldProperties worldProperties) {
        GPClaimManager claimWorldManager = null;
        if (worldProperties == null) {
            claimWorldManager = this.claimWorldManagers.get(Sponge.getServer().getDefaultWorld().get().getUniqueId());
        } else {
            claimWorldManager = this.claimWorldManagers.get(worldProperties.getUniqueId());
        }

        if (claimWorldManager == null) {
            claimWorldManager = new GPClaimManager(worldProperties);
            this.claimWorldManagers.put(worldProperties.getUniqueId(), claimWorldManager);
        }
        return claimWorldManager;
    }

    public void removeClaimWorldManager(WorldProperties worldProperties) {
        if (DataStore.USE_GLOBAL_PLAYER_STORAGE) {
            return;
        }
        this.claimWorldManagers.remove(worldProperties.getUniqueId());
    }

    public void setupDefaultPermissions(World world) {
        Set<Context> contexts = new HashSet<>();
        contexts.add(ClaimContexts.ADMIN_DEFAULT_CONTEXT);
        contexts.add(world.getContext());
        final Map<String, Boolean> adminDefaults = GriefPreventionPlugin.getActiveConfig(world.getProperties()).getConfig().flags.getAdminDefaults();
        CLAIM_FLAG_DEFAULTS.put(ClaimType.ADMIN, adminDefaults);
        this.setFlagDefaultPermissions(contexts, adminDefaults);
        this.setOptionDefaultPermissions(contexts);
        contexts = new HashSet<>();
        contexts.add(ClaimContexts.BASIC_DEFAULT_CONTEXT);
        contexts.add(world.getContext());
        final Map<String, Boolean> basicDefaults = GriefPreventionPlugin.getActiveConfig(world.getProperties()).getConfig().flags.getBasicDefaults();
        CLAIM_FLAG_DEFAULTS.put(ClaimType.BASIC, basicDefaults);
        this.setFlagDefaultPermissions(contexts, basicDefaults);
        this.setOptionDefaultPermissions(contexts);
        contexts = new HashSet<>();
        contexts.add(ClaimContexts.TOWN_DEFAULT_CONTEXT);
        contexts.add(world.getContext());
        final Map<String, Boolean> townDefaults = GriefPreventionPlugin.getActiveConfig(world.getProperties()).getConfig().flags.getTownDefaults();
        CLAIM_FLAG_DEFAULTS.put(ClaimType.TOWN, townDefaults);
        this.setFlagDefaultPermissions(contexts, townDefaults);
        this.setOptionDefaultPermissions(contexts);
        contexts = new HashSet<>();
        contexts.add(ClaimContexts.WILDERNESS_DEFAULT_CONTEXT);
        contexts.add(world.getContext());
        final Map<String, Boolean> wildernessDefaults = GriefPreventionPlugin.getActiveConfig(world.getProperties()).getConfig().flags.getWildernessDefaults();
        CLAIM_FLAG_DEFAULTS.put(ClaimType.WILDERNESS, wildernessDefaults);
        this.setFlagDefaultPermissions(contexts, wildernessDefaults);
        this.setOptionDefaultPermissions(contexts);
    }

    private void setFlagDefaultPermissions(Set<Context> contexts, Map<String, Boolean> defaultFlags) {
        GriefPreventionPlugin.instance.executor.execute(() -> {
            Map<String, Boolean> defaultPermissions = GriefPreventionPlugin.GLOBAL_SUBJECT.getTransientSubjectData().getPermissions(contexts);
            if (defaultPermissions.isEmpty()) {
                for (Map.Entry<String, Boolean> mapEntry : defaultFlags.entrySet()) {
                    GriefPreventionPlugin.GLOBAL_SUBJECT.getTransientSubjectData().setPermission(contexts, GPPermissions.FLAG_BASE + "." + mapEntry.getKey(), Tristate.fromBoolean(mapEntry.getValue()));
                }
            } else {
                // remove invalid flag entries
                for (String flagPermission : defaultPermissions.keySet()) {
                    String flag = flagPermission.replace(GPPermissions.FLAG_BASE + ".", "");
                    if (!defaultFlags.containsKey(flag)) {
                        GriefPreventionPlugin.GLOBAL_SUBJECT.getTransientSubjectData().setPermission(contexts, flagPermission, Tristate.UNDEFINED);
                    }
                }

                // make sure all defaults are available
                for (Map.Entry<String, Boolean> mapEntry : defaultFlags.entrySet()) {
                    String flagPermission = GPPermissions.FLAG_BASE + "." + mapEntry.getKey();
                    if (!defaultPermissions.keySet().contains(flagPermission)) {
                        GriefPreventionPlugin.GLOBAL_SUBJECT.getTransientSubjectData().setPermission(contexts, flagPermission, Tristate.fromBoolean(mapEntry.getValue()));
                    }
                }
            }
        });
    }

    private void setOptionDefaultPermissions(Set<Context> contexts) {
        GriefPreventionPlugin.instance.executor.execute(() -> {
            final SubjectData globalSubjectData = GriefPreventionPlugin.GLOBAL_SUBJECT.getTransientSubjectData();
            for (Map.Entry<String, String> optionEntry : GPOptions.DEFAULT_OPTIONS.entrySet()) {
                globalSubjectData.setOption(contexts, optionEntry.getKey(), optionEntry.getValue());
            }
        });
    }

    abstract GPPlayerData getPlayerDataFromStorage(UUID playerID);

    public abstract void loadWorldData(World world);

    public abstract void unloadWorldData(WorldProperties worldProperties);

    abstract void loadClaimTemplates();
}
