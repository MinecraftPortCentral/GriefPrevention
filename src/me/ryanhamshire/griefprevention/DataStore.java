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
import me.ryanhamshire.griefprevention.message.CustomizableMessage;
import me.ryanhamshire.griefprevention.message.Messages;
import me.ryanhamshire.griefprevention.message.TextMode;
import me.ryanhamshire.griefprevention.permission.GPOptions;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import me.ryanhamshire.griefprevention.task.SecureClaimTask;
import me.ryanhamshire.griefprevention.task.SiegeCheckupTask;
import me.ryanhamshire.griefprevention.util.WordFinder;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.ChunkPos;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.SubjectData;
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
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
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

    // World UUID -> PlayerDataWorldManager
    protected final Map<UUID, GPClaimManager> claimWorldManagers = Maps.newHashMap();

    // in-memory cache for claim data
    public static Map<UUID, GriefPreventionConfig<DimensionConfig>> dimensionConfigMap = Maps.newHashMap();
    public static Map<UUID, GriefPreventionConfig<WorldConfig>> worldConfigMap = Maps.newHashMap();
    public static Map<String, ClaimTemplateStorage> globalTemplates = new HashMap<>();
    public static GriefPreventionConfig<GlobalConfig> globalConfig;
    public static Map<UUID, GPPlayerData> GLOBAL_PLAYER_DATA = Maps.newHashMap();
    public static boolean USE_GLOBAL_PLAYER_STORAGE = true;

    // in-memory cache for messages
    protected EnumMap<Messages, CustomizableMessage> messages = new EnumMap<>(Messages.class);

    // pattern for unique user identifiers (UUIDs)
    protected final static Pattern uuidpattern = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    // path information, for where stuff stored on disk is well... stored
    public final static Path dataLayerFolderPath = GriefPreventionPlugin.instance.getConfigPath();
    public final static Path globalPlayerDataPath = dataLayerFolderPath.resolve("GlobalPlayerData");
    final static Path messagesFilePath = dataLayerFolderPath.resolve("messages.conf");
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
        this.loadMessages();
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

        if (claim.parent != null) {
            claim = claim.parent;
        }

        // adjust to new depth
        Vector3d newLesserPosition = new Vector3d(claim.lesserBoundaryCorner.getX(), newDepth, claim.lesserBoundaryCorner.getZ());
        claim.lesserBoundaryCorner = claim.lesserBoundaryCorner.setPosition(newLesserPosition);
        Vector3d newGreaterPosition = new Vector3d(claim.greaterBoundaryCorner.getX(), newDepth, claim.greaterBoundaryCorner.getZ());
        claim.greaterBoundaryCorner = claim.greaterBoundaryCorner.setPosition(newGreaterPosition);

        for (Claim subClaim : claim.children) {
            GPClaim subdivision = (GPClaim) subClaim;
            newLesserPosition = new Vector3d(subdivision.lesserBoundaryCorner.getX(), newDepth, subdivision.lesserBoundaryCorner.getZ());
            subdivision.lesserBoundaryCorner = subdivision.lesserBoundaryCorner.setPosition(newLesserPosition);
            newGreaterPosition = new Vector3d(subdivision.greaterBoundaryCorner.getX(), newDepth, subdivision.greaterBoundaryCorner.getZ());
            subdivision.greaterBoundaryCorner = subdivision.greaterBoundaryCorner.setPosition(newGreaterPosition);
        }

        claim.updateClaimStorageData();
    }

    // starts a siege on a claim
    // does NOT check siege cooldowns, see onCooldown() below
    public void startSiege(Player attacker, Player defender, GPClaim defenderClaim) {
        // fill-in the necessary SiegeData instance
        SiegeData siegeData = new SiegeData(attacker, defender, defenderClaim);
        GPPlayerData attackerData = this.getPlayerData(attacker.getWorld(), attacker.getUniqueId());
        GPPlayerData defenderData = this.getPlayerData(defender.getWorld(), defender.getUniqueId());
        attackerData.siegeData = siegeData;
        defenderData.siegeData = siegeData;
        defenderClaim.siegeData = siegeData;

        // start a task to monitor the siege
        // why isn't this a "repeating" task?
        // because depending on the status of the siege at the time the task
        // runs, there may or may not be a reason to run the task again
        SiegeCheckupTask siegeTask = new SiegeCheckupTask(siegeData);
        Task task =
                Sponge.getGame().getScheduler().createTaskBuilder().delay(30, TimeUnit.SECONDS).execute(siegeTask).submit(GriefPreventionPlugin.instance);
        siegeData.checkupTaskID = task.getUniqueId();
    }

    // ends a siege
    // either winnerName or loserName can be null, but not both
    public void endSiege(SiegeData siegeData, String winnerName, String loserName, boolean death) {
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

        GPPlayerData attackerData = this.getPlayerData(siegeData.attacker.getWorld(), siegeData.attacker.getUniqueId());
        attackerData.siegeData = null;

        GPPlayerData defenderData = this.getPlayerData(siegeData.defender.getWorld(), siegeData.defender.getUniqueId());
        defenderData.siegeData = null;
        defenderData.lastSiegeEndTimeStamp = System.currentTimeMillis();

        // start a cooldown for this attacker/defender pair
        Long now = Calendar.getInstance().getTimeInMillis();
        Long cooldownEnd = now + 1000 * 60 * 60; // one hour from now
        this.siegeCooldownRemaining.put(siegeData.attacker.getName() + "_" + siegeData.defender.getName(), cooldownEnd);

        // start cooldowns for every attacker/involved claim pair
        for (int i = 0; i < siegeData.claims.size(); i++) {
            GPClaim claim = siegeData.claims.get(i);
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
                GriefPreventionPlugin.sendMessage(winner.get(), TextMode.Success, Messages.SiegeWinDoorsOpen);

                // schedule a task to secure the claims in about 5 minutes
                SecureClaimTask task = new SecureClaimTask(siegeData);
                Sponge.getGame().getScheduler().createTaskBuilder().delay(5, TimeUnit.MINUTES).execute(task).submit(GriefPreventionPlugin.instance);
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
                for (int i = 0; i < loserPlayer.inventory.mainInventory.size(); i++) {
                    if (loserPlayer.inventory.mainInventory.get(i)!= null) {
                        loserItems.add(loserPlayer.inventory.mainInventory.get(i));
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
                    net.minecraft.entity.item.EntityItem entity =
                            new net.minecraft.entity.item.EntityItem((net.minecraft.world.World) winnerLocation.getExtent(), winnerLocation.getX(),
                                    winnerLocation.getY(), winnerLocation.getZ(), loserItems.get(i));
                    entity.setPickupDelay(10);
                    ((net.minecraft.world.World) winnerLocation.getExtent()).spawnEntity(entity);
                }
            }
        }
    }

    // timestamp for each siege cooldown to end
    public HashMap<String, Long> siegeCooldownRemaining = new HashMap<String, Long>();

    // whether or not a sieger can siege a particular victim or claim,
    // considering only cooldowns
    public boolean onCooldown(Player attacker, Player defender, GPClaim defenderClaim) {
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
        GPPlayerData defenderData = this.getPlayerData(defender.getWorld(), defender.getUniqueId());
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
    public void tryExtendSiege(Player player, GPClaim claim) {
        GPPlayerData playerData = this.getPlayerData(player.getWorld(), player.getUniqueId());

        // player must be sieged
        if (playerData.siegeData == null) {
            return;
        }

        // claim isn't already under the same siege
        if (playerData.siegeData.claims.contains(claim)) {
            return;
        }

        // admin claims can't be sieged
        if (claim.isAdminClaim()) {
            return;
        }

        // player must have some level of permission to be sieged in a claim
        if (claim.allowAccess(player) != null) {
            return;
        }

        // otherwise extend the siege
        playerData.siegeData.claims.add(claim);
        claim.siegeData = playerData.siegeData;
    }

    public ClaimResult createClaim(World world, Vector3i point1, Vector3i point2, ClaimType claimType, UUID ownerUniqueId, boolean cuboid) {
        GPClaimManager claimManager = this.getClaimWorldManager(world.getProperties());
        ClaimResult claimResult = Claim.builder()
                .bounds(point1, point2)
                .world(world)
                .type(claimType)
                .owner(ownerUniqueId)
                .cuboid(cuboid)
                .cause(GriefPreventionPlugin.pluginCause)
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
            claimWorldManager.deleteClaim(claim);

            // if in a creative mode world, delete the claim
            if (GriefPreventionPlugin.instance.claimModeIsActive(claim.getLesserBoundaryCorner().getExtent().getProperties(), ClaimsMode.Creative)) {
                GriefPreventionPlugin.instance.restoreClaim((GPClaim) claim, 0);
            }
        }

        return new GPClaimResult(claimsToDelete, ClaimResultType.SUCCESS);
    }

    public ClaimResult deleteClaim(Claim claim, Cause cause) {
        GPClaimManager claimManager = this.getClaimWorldManager(claim.getWorld().getProperties());
        return claimManager.deleteClaim(claim, cause);
    }

    // deletes all claims owned by a player
    public void deleteClaimsForPlayer(UUID playerID) {
        if (DataStore.USE_GLOBAL_PLAYER_STORAGE && playerID != null) {
            List<Claim> claimsToDelete = new ArrayList<>(DataStore.GLOBAL_PLAYER_DATA.get(playerID).getClaims());
            for (Claim claim : claimsToDelete) {
                ((GPClaim) claim).removeSurfaceFluids(null);
                GriefPreventionPlugin.GLOBAL_SUBJECT.getSubjectData().clearPermissions(ImmutableSet.of(claim.getContext()));
                GPClaimManager claimWorldManager = this.claimWorldManagers.get(claim.getWorld().getProperties().getUniqueId());
                claimWorldManager.deleteClaim(claim);

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
                claimWorldManager.deleteClaim(claim);

                // if in a creative mode world, delete the claim
                if (GriefPreventionPlugin.instance.claimModeIsActive(claim.getLesserBoundaryCorner().getExtent().getProperties(), ClaimsMode.Creative)) {
                    GriefPreventionPlugin.instance.restoreClaim((GPClaim) claim, 0);
                }
            }
        }
    }

    public void loadMessages() {
        this.messages.clear();
        // initialize defaults
        this.addDefault(Messages.AbandonClaimAdvertisement, "To delete another claim and free up some blocks, use /AbandonClaim.");
        this.addDefault(Messages.AbandonClaimMissing, "Stand in the claim you want to delete, or consider /AbandonAllClaims.");
        this.addDefault(Messages.AbandonSuccess, "Claim abandoned.  You now have {0} available claim blocks.", "0: remaining claim blocks");
        this.addDefault(Messages.AbandonOtherSuccess, "{0}'s claim has been abandoned. {0} now has {1} available claim blocks.", "0: player; 1: remaining claim blocks");
        this.addDefault(Messages.Access, "Access");
        this.addDefault(Messages.AccessPermission, "interact with everything except inventory containers.");
        this.addDefault(Messages.AdjustBlocksSuccess, "Adjusted {0}'s bonus claim blocks by {1}.  New total bonus blocks: {2}.", "0: player; 1: adjustment; 2: new total");
        this.addDefault(Messages.AdjustGroupBlocksSuccess, "Adjusted bonus claim blocks for players with the {0} permission by {1}.  New total: {2}.", "0: permission; 1: adjustment amount; 2: new total bonus");
        this.addDefault(Messages.AdminClaimsMode, "Administrative claims mode active.  Any claims created will be free and editable by other administrators.");
        this.addDefault(Messages.AdvertiseACB, "You may use /ACB to give yourself more claim blocks.");
        this.addDefault(Messages.AdvertiseACandACB, "You may use /ACB to give yourself more claim blocks, or /AdminClaims to create a free administrative claim.");
        this.addDefault(Messages.AdvertiseAdminClaims, "You could create an administrative land claim instead using /AdminClaims, which you'd share with other administrators.");
        this.addDefault(Messages.AllAdminDeleted, "Deleted all administrative claims.");
        this.addDefault(Messages.AlreadySieging, "You're already involved in a siege.");
        this.addDefault(Messages.AlreadyUnderSiegeArea, "That area is already under siege.  Join the party!");
        this.addDefault(Messages.AlreadyUnderSiegePlayer, "{0} is already under siege.  Join the party!", "0: defending player");
        this.addDefault(Messages.AutoBanNotify, "Auto-banned {0}({1}).  See logs for details.");
        this.addDefault(Messages.AutomaticClaimNotification, "This chest and nearby blocks are protected from breakage and theft.");
        this.addDefault(Messages.AvoidGriefClaimLand, "Prevent grief!  If you claim your land, you will be grief-proof.");
        this.addDefault(Messages.BasicClaimsMode, "Returned to basic claim creation mode.");
        this.addDefault(Messages.BecomeMayor, "Subdivide your land claim and become a mayor!");
        this.addDefault(Messages.BesiegedNoTeleport, "You can't teleport into a besieged area.");
        this.addDefault(Messages.BlockChangeFromWilderness, "Claim blocks are not allowed to be changed from wilderness.");
        this.addDefault(Messages.BlockClaimed, "That block has been claimed by {0}.", "0: claim owner");
        this.addDefault(Messages.BlockedCommand, "The command {0} has been blocked by claim owner {1}.");
        this.addDefault(Messages.BlockNotClaimed, "No one has claimed this block.");
        this.addDefault(Messages.BlockPurchaseCost, "Each claim block costs {0}.  Your balance is {1}.", "0: cost of one block; 1: player's account balance");
        this.addDefault(Messages.BlockSaleConfirmation, "Deposited {0} in your account.  You now have {1} available claim blocks.", "0: amount deposited; 1: remaining blocks");
        this.addDefault(Messages.BlockSaleValue, "Each claim block is worth {0}.  You have {1} available for sale.", "0: block value; 1: available blocks");
        this.addDefault(Messages.BookAuthor, "BigScary");
        this.addDefault(Messages.BookDisabledChestClaims, "  On this server, placing a chest will NOT claim land for you.");
        this.addDefault(Messages.BookIntro, "Claim land to protect your stuff!  Click the link above to learn land claims in 3 minutes or less.  :)");
        this.addDefault(Messages.BookLink, "Click: " + SURVIVAL_VIDEO_URL_RAW);
        this.addDefault(Messages.BookTitle, "How to Claim Land");
        this.addDefault(Messages.BookTools, "Our claim tools are {0} and {1}.", "0: claim modification tool name; 1:claim information tool name");
        this.addDefault(Messages.BookUsefulCommands, "Useful Commands:");
        this.addDefault(Messages.Build, "Build");
        this.addDefault(Messages.BuildPermission, "build");
        this.addDefault(Messages.BuildingOutsideClaims, "Other players can build here, too.  Consider creating a land claim to protect your work!");
        this.addDefault(Messages.BuySellNotConfigured, "Sorry, buying and selling claim blocks is disabled.");
        this.addDefault(Messages.CantDeleteAdminClaim, "You don't have permission to delete administrative claims.");
        this.addDefault(Messages.CantDeleteBasicClaim, "You don't have permission to delete basic claims.");
        this.addDefault(Messages.CantFightWhileImmune, "You can't fight someone while you're protected from PvP.");
        this.addDefault(Messages.CantGrantThatPermission, "You can't grant a permission you don't have yourself.");
        this.addDefault(Messages.ChestClaimConfirmation, "This chest is protected.");
        this.addDefault(Messages.ChestFull, "This chest is full.");
        this.addDefault(Messages.ClaimBlockLimit, "You've reached your claim block limit.  You can't purchase more.");
        this.addDefault(Messages.ClaimCreationFailedOverClaimCountLimit, "You've reached your limit on land claims.  Use /AbandonClaim to remove one before creating another.");
        this.addDefault(Messages.ClaimExplosivesAdvertisement, "To allow explosives to destroy blocks in this land claim, use /ClaimExplosions.");
        this.addDefault(Messages.ClaimFlagOverridden, "Failed to set claim flag. The flag '{0}' has been overridden by an admin.", "0: The claim flag that has been overridden.");
        this.addDefault(Messages.ClaimLastActive, "Claim last active {0}.", "0: The date and time when this claim was last active");
        this.addDefault(Messages.ClaimResizeSuccess, "Claim resized.  {0} available claim blocks remaining.", "0: remaining blocks");
        this.addDefault(Messages.ClaimStart, "Claim corner set!  Use the shovel again at the opposite corner to claim a rectangle of land.  To cancel, put your shovel away.");
        this.addDefault(Messages.ClaimTooSmallForEntities, "This claim isn't big enough for that.  Try enlarging it.");
        this.addDefault(Messages.ClaimsDisabledWorld, "Land claims are disabled in this world.");
        this.addDefault(Messages.ClaimsListHeader, "Claims:");
        this.addDefault(Messages.ClaimsListNoPermission, "You don't have permission to get information about another player's land claims.");
        this.addDefault(Messages.ClearPermissionsOneClaim, "Cleared permissions in this claim.  To set permission for ALL your claims, stand outside them.");
        this.addDefault(Messages.ClearPermsOwnerOnly, "Only the claim owner can clear all permissions.");
        this.addDefault(Messages.CollectivePublic, "the public", "as in 'granted the public permission to...'");
        this.addDefault(Messages.CommandBannedInPvP, "You can't use that command while in PvP combat.");
        this.addDefault(Messages.ConfirmFluidRemoval, "Abandoning this claim will remove lava inside the claim.  If you're sure, use /AbandonClaim again.");
        this.addDefault(Messages.Containers, "Containers");
        this.addDefault(Messages.ContainersPermission, "access all containers including inventory.");
        this.addDefault(Messages.ContinueBlockMath, " (-{0} blocks)");
        this.addDefault(Messages.CreateClaimFailOverlap, "You can't create a claim here because it would overlap your other claim.  Use /abandonclaim to delete it, or use your shovel at a " + "corner to resize it.");
        this.addDefault(Messages.CreateClaimFailOverlapOtherPlayer, "You can't create a claim here because it would overlap {0}'s claim.", "0: other claim owner");
        this.addDefault(Messages.CreateClaimFailOverlapRegion, "You can't claim all of this because you're not allowed to build here.");
        this.addDefault(Messages.CreateClaimFailOverlapShort, "Your selected area overlaps an existing claim.");
        this.addDefault(Messages.CreateClaimInsufficientBlocks, "You don't have enough blocks to claim that entire area.  You need {0} more blocks.", "0: additional blocks needed");
        this.addDefault(Messages.CreateClaimSuccess, "Claim created!  Use /trust to share it with friends.");
        this.addDefault(Messages.CreateSubdivisionOverlap, "Your selected area overlaps another subdivision.");
        this.addDefault(Messages.CreativeBasicsVideo2, "Click for Land Claim Help: " + CREATIVE_VIDEO_URL_RAW);
        this.addDefault(Messages.CuboidClaimDisabled, "Now claiming in 2D mode.");
        this.addDefault(Messages.CuboidClaimEnabled, "Now claiming in 3D mode.");
        this.addDefault(Messages.DeleteAllSuccess, "Deleted all of {0}'s claims.", "0: owner's name");
        this.addDefault(Messages.DeleteClaimMissing, "There's no claim here.");
        this.addDefault(Messages.DeleteSuccess, "Claim deleted.");
        this.addDefault(Messages.DeleteTopLevelClaim, "To delete a subdivision, stand inside it.  Otherwise, use /AbandonTopLevelClaim to delete this claim and all subdivisions.");
        this.addDefault(Messages.DeletionSubdivisionWarning, "This claim includes subdivisions.  If you're sure you want to delete it, use /DeleteClaim again.");
        this.addDefault(Messages.DonateItemsInstruction, "To give away the item(s) in your hand, left-click the chest again.");
        this.addDefault(Messages.DonationSuccess, "Item(s) transferred to chest!");
        this.addDefault(Messages.DropUnlockAdvertisement, "Other players can't pick up your dropped items unless you /UnlockDrops first.");
        this.addDefault(Messages.DropUnlockConfirmation, "Unlocked your drops.  Other players may now pick them up (until you die again).");
        this.addDefault(Messages.EndBlockMath, " = {0} blocks left to spend");
        this.addDefault(Messages.FillModeActive, "Fill mode activated with radius {0}.  Right click an area to fill.", "0: fill radius");
        this.addDefault(Messages.FireSpreadOutsideClaim, "Fire attempting to spread outside of claim.");
        this.addDefault(Messages.GrantPermissionConfirmation, "Granted {0} permission to {1} {2}.", "0: target player; 1: permission description; 2: scope (changed claims)");
        this.addDefault(Messages.GrantPermissionNoClaim, "Stand inside the claim where you want to grant permission.");
        this.addDefault(Messages.HowToClaimRegex, "(^|.*\\W)how\\W.*\\W(claim|protect|lock)(\\W.*|$)", "This is a Java Regular Expression.  Look it up before editing!  It's used to tell players about the demo video when they ask how " + "to claim land.");
        this.addDefault(Messages.IgnoreClaimsAdvertisement, "To override, use /IgnoreClaims.");
        this.addDefault(Messages.IgnoreConfirmation, "You're now ignoring chat messages from that player.");
        this.addDefault(Messages.IgnoringClaims, "Now ignoring claims.");
        this.addDefault(Messages.InsufficientFunds, "You don't have enough money.  You need {0}, but you only have {1}.", "0: total cost; 1: player's account balance");
        this.addDefault(Messages.InvalidPermissionID, "Please specify a player name, or a permission in [brackets].");
        this.addDefault(Messages.ItemNotAuthorized, "You have not been authorized to use the item {0} in this claim.");
        this.addDefault(Messages.LocationAllClaims, "in all your claims");
        this.addDefault(Messages.LocationCurrentClaim, "in this claim");
        this.addDefault(Messages.Manage, "Manage");
        this.addDefault(Messages.ManageOneClaimPermissionsInstruction, "To manage permissions for a specific claim, stand inside it.");
        this.addDefault(Messages.ManageUniversalPermissionsInstruction, "To manage permissions for ALL your claims, stand outside them.");
        this.addDefault(Messages.ManagersDontUntrustManagers, "Only the claim owner can demote a manager.");
        this.addDefault(Messages.NewClaimTooNarrow, "This claim would be too small.  Any claim must be at least {0} blocks wide.", "0: minimum claim width");
        this.addDefault(Messages.NoAccessPermission, "You don't have {0}'s permission to use that.", "0: owner name.  access permission controls buttons, levers, and beds");
        this.addDefault(Messages.NoBedPermission, "{0} hasn't given you permission to sleep here.", "0: claim owner");
        this.addDefault(Messages.NoBuildOutsideClaims, "You can't build here unless you claim some land first.");
        this.addDefault(Messages.NoBuildPermission, "You don't have {0}'s permission to build here.", "0: owner name");
        this.addDefault(Messages.NoBuildPortalPermission, "You can't use this portal because you don't have {0}'s permission to build an exit portal in the destination land claim.","0: Destination land claim owner's name.");
        this.addDefault(Messages.NoBuildPvP, "You can't build in claims during PvP combat.");
        this.addDefault(Messages.NoBuildUnderSiege, "This claim is under siege by {0}.  No one can build here.", "0: attacker name");
        this.addDefault(Messages.NoChatUntilMove, "Sorry, but you have to move a little more before you can chat.  We get lots of spam bots here.  :)");
        this.addDefault(Messages.NoClaimDuringPvP, "You can't claim lands during PvP combat.");
        this.addDefault(Messages.NoContainersPermission, "You don't have {0}'s permission to use that.", "0: owner's name.  containers also include crafting blocks");
        this.addDefault(Messages.NoContainersSiege, "This claim is under siege by {0}.  No one can access containers here right now.", "0: attacker name");
        this.addDefault(Messages.NoCreateClaimPermission, "You don't have permission to claim land.");
        this.addDefault(Messages.NoCreativeUnClaim, "You can't unclaim this land.  You can only make this claim larger or create additional claims.");
        this.addDefault(Messages.NoDamageClaimedEntity, "That belongs to {0}.", "0: owner name");
        this.addDefault(Messages.NoDeletePermission, "You don't have permission to delete claims.");
        this.addDefault(Messages.NoDropsAllowed, "You can't drop items in this claim.");
        this.addDefault(Messages.NoEditPermission, "You don't have permission to edit this claim.");
        this.addDefault(Messages.NoEnterClaim, "You don't have permission to enter this claim.");
        this.addDefault(Messages.NoExitClaim, "You don't have permission to exit this claim.");
        this.addDefault(Messages.NoInteractBlockPermission, "You don't have {0}'s permission to interact with the block {1}.", "0: owner name; 1: block id");
        this.addDefault(Messages.NoInteractEntityPermission, "You don't have {0}'s permission to interact with the entity {1}.", "0: owner name; 1: entity id");
        this.addDefault(Messages.NoInteractItemPermission, "You don't have {0}'s permission to interact with the item {1}.", "0: owner name; 1: item id");
        this.addDefault(Messages.NoInteractItemPermissionSelf, "You don't have permission to interact with the item {0}.", "0: item id");
        this.addDefault(Messages.NoLavaNearOtherPlayer, "You can't place lava this close to {0}.", "0: nearby player");
        this.addDefault(Messages.NoModifyDuringSiege, "Claims can't be modified while under siege.");
        this.addDefault(Messages.NoOwnerBuildUnderSiege, "You can't make changes while under siege.");
        this.addDefault(Messages.NoPermissionForCommand, "You don't have permission to do that.");
        this.addDefault(Messages.NoPermissionTrust, "You don't have {0}'s permission to manage permissions here.", "0: claim owner's name");
        this.addDefault(Messages.NoPistonsOutsideClaims, "Warning: Pistons won't move blocks outside land claims.");
        this.addDefault(Messages.NoPortalFromProtectedClaim, "You do not have permission to use portals in this claim owned by {0}.", "0: claim owner's name");
        this.addDefault(Messages.NoPortalToProtectedClaim, "You do not have permission to travel through this portal into the protected claim owned by {0}.", "0: claim owner's name");
        this.addDefault(Messages.NoProfanity, "Please moderate your language.");
        this.addDefault(Messages.NoSiegeAdminClaim, "Siege is disabled in this area.");
        this.addDefault(Messages.NoSiegeDefenseless, "That player is defenseless.  Go pick on somebody else.");
        this.addDefault(Messages.NoTNTDamageAboveSeaLevel, "Warning: TNT will not destroy blocks above sea level.");
        this.addDefault(Messages.NoTNTDamageClaims, "Warning: TNT will not destroy claimed blocks.");
        this.addDefault(Messages.NoTeleportFromProtectedClaim, "You do not have permission to teleport from the protected claim owned by {0}.", "0: owner of claim");
        this.addDefault(Messages.NoTeleportToProtectedClaim, "You do not have permission to teleport into a protected claim owned by {0}.", "0: owner of claim");
        this.addDefault(Messages.NoTeleportPvPCombat, "You can't teleport while fighting another player.");
        this.addDefault(Messages.NoWildernessBuckets, "You may only dump buckets inside your claim(s) or underground.");
        this.addDefault(Messages.NonSiegeMaterial, "That material is too tough to break.");
        this.addDefault(Messages.NonSiegeWorld, "Siege is disabled here.");
        this.addDefault(Messages.NotEnoughBlocksForSale, "You don't have that many claim blocks available for sale.");
        this.addDefault(Messages.NotIgnoringAnyone, "You're not ignoring anyone.");
        this.addDefault(Messages.NotIgnoringPlayer, "You're not ignoring that player.");
        this.addDefault(Messages.NotSiegableThere, "{0} isn't protected there.", "0: defending player");
        this.addDefault(Messages.NotTrappedHere, "You can build here.  Save yourself.");
        this.addDefault(Messages.NotYourClaim, "This isn't your claim.");
        this.addDefault(Messages.NotYourPet, "That belongs to {0} until it's given to you with /GivePet.", "0: owner name");
        this.addDefault(Messages.OnlyOwnersModifyClaims, "Only {0} can modify this claim.", "0: owner name");
        this.addDefault(Messages.OnlyPurchaseBlocks, "Claim blocks may only be purchased, not sold.");
        this.addDefault(Messages.OnlySellBlocks, "Claim blocks may only be sold, not purchased.");
        this.addDefault(Messages.OwnerNameForAdminClaims, "an administrator", "as in 'You don't have an administrator's permission to build here.'");
        this.addDefault(Messages.OwnerNameForWildernessClaims, "a wilderness administrator", "as in 'You don't have a wilderness administrator's permission to build here.'");
        this.addDefault(Messages.PermissionsPermission, "manage permissions");
        this.addDefault(Messages.PetGiveawayInvalid, "Pet type {0} is invalid, only vanilla entities are supported.", "0: The entity type.");
        this.addDefault(Messages.PetGiveawayConfirmation, "Pet transferred.");
        this.addDefault(Messages.PetTransferCancellation, "Pet giveaway cancelled.");
        this.addDefault(Messages.PickupBlockedExplanation, "You can't pick this up unless {0} uses /UnlockDrops.", "0: The item stack's owner.");
        this.addDefault(Messages.PlayerInPvPSafeZone, "That player is in a PvP safe zone.");
        this.addDefault(Messages.PlayerNotFound2, "No player by that name has logged in recently.");
        this.addDefault(Messages.PlayerNotIgnorable, "You can't ignore that player.");
        this.addDefault(Messages.PlayerOfflineTime, "  Last login: {0} days ago.", "0: number of full days since last login");
        this.addDefault(Messages.PlayerTooCloseForFire, "You can't start a fire this close to {0}.", "0: other player's name");
        this.addDefault(Messages.PurchaseConfirmation, "Withdrew {0} from your account.  You now have {1} available claim blocks.", "0: total cost; 1: remaining blocks");
        this.addDefault(Messages.PvPImmunityEnd, "Now you can fight with other players.");
        this.addDefault(Messages.PvPImmunityStart, "You're protected from attack by other players as long as your inventory is empty.");
        this.addDefault(Messages.PvPNoContainers, "You can't access containers during PvP combat.");
        this.addDefault(Messages.PvPNoDrop, "You can't drop items while in PvP combat.");
        this.addDefault(Messages.ReadyToTransferPet,"Ready to transfer!  Right-click the pet you'd like to give away, or cancel with /GivePet cancel.");
        this.addDefault(Messages.RemainingBlocks, "You may claim up to {0} more blocks.", "0: remaining blocks");
        this.addDefault(Messages.RescueAbortedMoved, "You moved!  Rescue cancelled.");
        this.addDefault(Messages.RescuePending, "If you stay put for 10 seconds, you'll be teleported out.  Please wait.");
        this.addDefault(Messages.ResizeClaimInsufficientArea, "The selected claim size of {0} blocks({1}x{2}) would be too small. A claim must use at least {3} total claim blocks.");
        this.addDefault(Messages.ResizeClaimTooNarrow, "This new claim size of {0} blocks({1}x{2}) would be too small.  Claims must be at least {3} blocks wide.", "0: minimum claim width");
        this.addDefault(Messages.ResizeFailOverlap, "Can't resize here because it would overlap another nearby claim.");
        this.addDefault(Messages.ResizeFailOverlapRegion, "You don't have permission to build there, so you can't claim that area.");
        this.addDefault(Messages.ResizeFailOverlapSubdivision, "You can't create a subdivision here because it would overlap another subdivision.  Consider /abandonclaim to delete it, or use " + "your shovel at a corner to resize it.");
        this.addDefault(Messages.ResizeNeedMoreBlocks, "You don't have enough blocks for this size.  You need {0} more.", "0: how many needed");
        this.addDefault(Messages.ResizeStart, "Resizing claim.  Use your shovel again at the new location for this corner.");
        this.addDefault(Messages.RespectingClaims, "Now respecting claims.");
        this.addDefault(Messages.RestoreNatureActivate, "Ready to restore some nature!  Right click to restore nature, and use /BasicClaims to stop.");
        this.addDefault(Messages.RestoreNatureAggressiveActivate, "Aggressive mode activated.  Do NOT use this underneath anything you want to keep!  Right click to aggressively restore nature, and" + " use /BasicClaims to stop.", null);
        this.addDefault(Messages.RestoreNaturePlayerInChunk, "Unable to restore.  {0} is in that chunk.", "0: nearby player");
        this.addDefault(Messages.SeparateConfirmation, "Those players will now ignore each other in chat.");
        this.addDefault(Messages.SetClaimBlocksSuccess, "Updated accrued claim blocks.");
        this.addDefault(Messages.ShovelBasicClaimMode, "Shovel returned to basic claims mode.");
        this.addDefault(Messages.ShowNearbyClaims, "Found {0} land claims.", "0: Number of claims found.");
        this.addDefault(Messages.SiegeAlert, "You're under siege!  If you log out now, you will die.  You must defeat {0}, wait for him to give up, or escape.", "0: attacker name");
        this.addDefault(Messages.SiegeConfirmed, "The siege has begun!  If you log out now, you will die.  You must defeat {0}, chase him away, or admit defeat and walk away.", "0: defender name");
        this.addDefault(Messages.SiegeDoorsLockedEjection, "Looting time is up!  Ejected from the claim.");
        this.addDefault(Messages.SiegeImmune, "That player is immune to /siege.");
        this.addDefault(Messages.SiegeNoContainers, "You can't access containers while involved in a siege.");
        this.addDefault(Messages.SiegeNoDrop, "You can't give away items while involved in a siege.");
        this.addDefault(Messages.SiegeNoShovel, "You can't use your shovel tool while involved in a siege.");
        this.addDefault(Messages.SiegeNoTeleport, "You can't teleport out of a besieged area.");
        this.addDefault(Messages.SiegeOnCooldown, "You're still on siege cooldown for this defender or claim.  Find another victim.");
        this.addDefault(Messages.SiegeTooFarAway, "You're too far away to siege.");
        this.addDefault(Messages.SiegeWinDoorsOpen, "Congratulations!  Buttons and levers are temporarily unlocked (five minutes).");
        this.addDefault(Messages.SoftMuted, "Soft-muted {0}.", "0: The changed player's name.");
        this.addDefault(Messages.StartBlockMath, "{0} blocks from play + {1} bonus + {2} initial = {3} total.");
        this.addDefault(Messages.SubdivisionNoClaimFound, "No claim exists at selected corner. Please click a valid opposite corner within parent claim in order to create your subdivision.");
        this.addDefault(Messages.SubdivisionMode, "Subdivision mode.  Use your shovel to create subdivisions in your existing claims.  Use /basicclaims to exit.");
        this.addDefault(Messages.SubdivisionStart, "Subdivision corner set!  Use your shovel at the location for the opposite corner of this new subdivision.");
        this.addDefault(Messages.SubdivisionSuccess, "Subdivision created!  Use /trust to share it with friends.");
        this.addDefault(Messages.SubdivisionVideo2, "Click for Subdivision Help: " + SUBDIVISION_VIDEO_URL_RAW);
        this.addDefault(Messages.SuccessfulAbandon, "Claims abandoned.  You now have {0} available claim blocks.", "0: remaining blocks");
        this.addDefault(Messages.SurvivalBasicsVideo2, "Click for Land Claim Help: " + SURVIVAL_VIDEO_URL_RAW);
        this.addDefault(Messages.ThatPlayerPvPImmune, "You can't injure defenseless players.");
        this.addDefault(Messages.TooDeepToClaim, "This chest can't be protected because it's too deep underground.  Consider moving it.");
        this.addDefault(Messages.TooFarAway, "That's too far away.");
        this.addDefault(Messages.TooManyEntitiesInClaim, "This claim has too many entities already.  Try enlarging the claim or removing some animals, monsters, paintings, or minecarts.");
        this.addDefault(Messages.TransferClaimAdminOnly, "Only administrative claims may be transferred to a player.");
        this.addDefault(Messages.TransferClaimMissing, "There's no claim here.  Stand in the administrative claim you want to transfer.", null);
        this.addDefault(Messages.TransferClaimPermission, "That command requires the administrative claims permission.");
        this.addDefault(Messages.TransferSuccess, "Claim transferred.");
        this.addDefault(Messages.TransferTopLevel, "Only top level claims (not subdivisions) may be transferred.  Stand outside of the subdivision and try again.");
        this.addDefault(Messages.TrappedWontWorkHere, "Sorry, unable to find a safe location to teleport you to.  Contact an admin, or consider /kill if you don't want to wait.");
        this.addDefault(Messages.TrustIndividualAllClaims, "Granted {0}'s full trust to all your claims.  To unset permissions for ALL your claims, use /untrustall.", "0: untrusted player");
        this.addDefault(Messages.TrustListHeader, "Explicit permissions here:");
        this.addDefault(Messages.TrustListNoClaim, "Stand inside the claim you're curious about.");
        this.addDefault(Messages.UnIgnoreConfirmation, "You're no longer ignoring chat messages from that player.");
        this.addDefault(Messages.UnSeparateConfirmation, "Those players will no longer ignore each other in chat.");
        this.addDefault(Messages.UnSoftMuted, "Un-soft-muted {0}.", "0: The changed player's name.");
        this.addDefault(Messages.UnclaimCleanupWarning, "The land you've unclaimed may be changed by other players or cleaned up by administrators.  If you've built something there you " + "want to keep, you should reclaim it.");
        this.addDefault(Messages.UnprotectedChestWarning, "This chest is NOT protected.  Consider using a golden shovel to expand an existing claim or to create a new one.");
        this.addDefault(Messages.UntrustAllOwnerOnly, "Only the claim owner can clear all its permissions.");
        this.addDefault(Messages.UntrustEveryoneAllClaims, "Cleared permissions in ALL your claims.  To set permissions for a single claim, stand inside it.");
        this.addDefault(Messages.UntrustIndividualAllClaims, "Revoked {0}'s access to ALL your claims.  To set permissions for a single claim, stand inside it and use /untrust.", "0: untrusted player");
        this.addDefault(Messages.UntrustIndividualSingleClaim, "Revoked {0}'s access to this claim.  To unset permissions for ALL your claims, use /untrustall.", "0: untrusted player");
        this.addDefault(Messages.YouHaveNoClaims, "You don't have any land claims.");

        // load the config file
        try {
            HoconConfigurationLoader configurationLoader = HoconConfigurationLoader.builder().setPath(messagesFilePath).build();
            CommentedConfigurationNode mainNode = configurationLoader.load();

            // for each message ID
            for (CustomizableMessage messageData : this.messages.values()) {
                // read the message from the file, use default if necessary
                if (mainNode.getNode("Messages", messageData.id.name(), "Text").isVirtual()) {
                    mainNode.getNode("Messages", messageData.id.name(), "Text").setValue(messageData.text);
                } else {
                    messageData.text = mainNode.getNode("Messages", messageData.id.name(), "Text").getString();
                }

                if (messageData.notes != null) {
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
            GriefPreventionPlugin.addLogEntry("Unable to write to the configuration file at \"" + DataStore.messagesFilePath + "\"");
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
        if (!generateMessages) {
            return "";
        }

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

    public Text parseMessage(Messages messageID, TextColor color, String... args) {
        String message = GriefPreventionPlugin.instance.dataStore.getMessage(messageID, args);
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
        Matcher urlMatcher = null;
        try {
            urlMatcher = pattern.matcher(text);
        } catch (Throwable t) {
            return containedUrls;
        }

        while (urlMatcher.find()) {
            containedUrls.add(text.substring(urlMatcher.start(0),
                    urlMatcher.end(0)));
        }

        return containedUrls;
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
    public Set<GPClaim> getNearbyClaims(Location<World> location) {
        Set<GPClaim> claims = new HashSet<>();
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
                        Set<GPClaim> claimsInChunk = claimWorldManager.getChunksToClaimsMap().get(ChunkPos.asLong(chunkX, chunkZ));
                        if (claimsInChunk != null) {
                            claims.addAll(claimsInChunk);
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

    public GPClaim getClaimAt(Location<World> location, boolean ignoreHeight, WeakReference<Claim> cachedClaim) {
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
        contexts.add(GriefPreventionPlugin.ADMIN_CLAIM_FLAG_DEFAULT_CONTEXT);
        contexts.add(world.getContext());
        this.setFlagDefaultPermissions(contexts, GriefPreventionPlugin.getActiveConfig(world.getProperties()).getConfig().flags.getAdminDefaults());
        this.setOptionDefaultPermissions(contexts);
        contexts = new HashSet<>();
        contexts.add(GriefPreventionPlugin.BASIC_CLAIM_FLAG_DEFAULT_CONTEXT);
        contexts.add(world.getContext());
        this.setFlagDefaultPermissions(contexts, GriefPreventionPlugin.getActiveConfig(world.getProperties()).getConfig().flags.getBasicDefaults());
        this.setOptionDefaultPermissions(contexts);
        contexts = new HashSet<>();
        contexts.add(GriefPreventionPlugin.WILDERNESS_CLAIM_FLAG_DEFAULT_CONTEXT);
        contexts.add(world.getContext());
        this.setFlagDefaultPermissions(contexts, GriefPreventionPlugin.getActiveConfig(world.getProperties()).getConfig().flags.getWildernessDefaults());
        this.setOptionDefaultPermissions(contexts);
    }

    private void setFlagDefaultPermissions(Set<Context> contexts, Map<String, Boolean> defaultFlags) {
        Sponge.getScheduler().createAsyncExecutor(GriefPreventionPlugin.instance.pluginContainer).execute(() -> {
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
        Sponge.getScheduler().createAsyncExecutor(GriefPreventionPlugin.instance.pluginContainer).execute(() -> {
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
