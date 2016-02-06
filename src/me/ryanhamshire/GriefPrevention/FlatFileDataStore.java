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

import com.flowpowered.math.vector.Vector3i;
import me.ryanhamshire.GriefPrevention.configuration.ClaimStorageData;
import me.ryanhamshire.GriefPrevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.GriefPrevention.configuration.PlayerStorageData;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.common.world.storage.SpongePlayerDataHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

//manages data stored in the file system
public class FlatFileDataStore extends DataStore {

    private final static Path schemaVersionFilePath = dataLayerFolderPath.resolve("_schemaVersion");
    private final static Path worldsConfigFolderPath = dataLayerFolderPath.resolve("worlds");
    protected final static Path claimDataPath = Paths.get("GriefPreventionData", "ClaimData");
    protected final static Path playerDataPath = Paths.get("GriefPreventionData", "PlayerData");

    public FlatFileDataStore() {
    }

    @Override
    void initialize() throws Exception {
        // ensure data folders exist
        File worldsDataFolder = worldsConfigFolderPath.toFile();

        if (!worldsDataFolder.exists()) {
            worldsDataFolder.mkdirs();
        }

        // TODO - use permission groups instead
        // load group data into memory
        /*File[] files = loginDataFolder.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (!file.isFile()) {
                continue; // avoids folders
            }

            // all group data files start with a dollar sign. ignoring the rest,
            // which are player data files.
            if (!file.getName().startsWith("$")) {
                continue;
            }

            String groupName = file.getName().substring(1);
            if (groupName == null || groupName.isEmpty())
                continue; // defensive coding, avoid unlikely cases

            BufferedReader inStream = null;
            try {
                inStream = new BufferedReader(new FileReader(file.getAbsolutePath()));
                String line = inStream.readLine();

                int groupBonusBlocks = Integer.parseInt(line);

                this.permissionToBonusBlocksMap.put(groupName, groupBonusBlocks);
            } catch (Exception e) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                GriefPrevention.AddLogEntry(errors.toString(), CustomLogEntryTypes.Exception);
            }

            try {
                if (inStream != null)
                    inStream.close();
            } catch (IOException exception) {
            }
        }*/

        Path rootPath = Sponge.getGame().getSavesDirectory().resolve(Sponge.getGame().getServer().getDefaultWorld().get().getWorldName());

        for (World world : Sponge.getGame().getServer().getWorlds()) {
            // check if claims are supported
            GriefPreventionConfig<GriefPreventionConfig.WorldConfig> worldConfig = DataStore.worldConfigMap.get(world.getUniqueId());
            if (worldConfig != null && worldConfig.getConfig().configEnabled && !worldConfig.getConfig().claim.allowClaims) {
                GriefPrevention.AddLogEntry("Error - World '" + world.getName() + "' does not allow claims. Skipping...");
                continue;
            }
            // run cleanup task
            CleanupUnusedClaimsTask task = new CleanupUnusedClaimsTask(world.getProperties());
            Sponge.getGame().getScheduler().createTaskBuilder().delay(1, TimeUnit.MINUTES).execute(task).submit(GriefPrevention.instance);
            // check if world has existing data
            Path worldClaimDataPath = Paths.get(world.getName()).resolve(claimDataPath);
            Path worldPlayerDataPath = Paths.get(world.getName()).resolve(playerDataPath);
            if (world.getUniqueId() == Sponge.getGame().getServer().getDefaultWorld().get().getUniqueId()) {
                worldClaimDataPath = claimDataPath;
                worldPlayerDataPath = playerDataPath;
            }
            if (Files.exists(rootPath.resolve(worldClaimDataPath))) {
                File[] files = rootPath.resolve(worldClaimDataPath).toFile().listFiles();
                this.loadClaimData(files);
            } else {
                Files.createDirectories(rootPath.resolve(worldClaimDataPath));
            }

            if (Files.exists(rootPath.resolve(worldPlayerDataPath))) {
                File[] files = rootPath.resolve(worldPlayerDataPath).toFile().listFiles();
                this.loadPlayerData(world.getProperties(), files);
            }
            if (!Files.exists(rootPath.resolve(worldPlayerDataPath))) {
                Files.createDirectories(rootPath.resolve(worldPlayerDataPath));
            }

        }
        super.initialize();
    }

    void loadClaimData(File[] files) throws Exception {
        ConcurrentHashMap<Claim, UUID> orphans = new ConcurrentHashMap<>();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) // avoids folders
            {
                // the filename is the claim ID. try to parse it
                UUID claimID;

                try {
                    claimID = UUID.fromString(files[i].getName());
                    System.out.println("claimID = " + claimID);
                } catch (Exception e) {
                    System.out.println("ERROR!! could not read claim file " + files[i].getAbsolutePath());
                    continue;
                }

                try {
                    ArrayList<UUID> out_parentID = new ArrayList<UUID>(); // hacky output parameter
                    Claim claim = this.loadClaim(files[i], out_parentID, claimID);
                    if (out_parentID.size() == 0) {
                        this.addClaim(claim, false);
                    } else {
                        orphans.put(claim, out_parentID.get(0));
                    }
                }

                // if there's any problem with the file's content, log an error message and skip it
                catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("World not found")) {
                        files[i].delete();
                    } else {
                        StringWriter errors = new StringWriter();
                        e.printStackTrace(new PrintWriter(errors));
                        GriefPrevention.AddLogEntry(files[i].getName() + " " + errors.toString(), CustomLogEntryTypes.Exception);
                    }
                }
            }
        }

        // link children to parents
        for (Claim child : orphans.keySet()) {
            Claim parent = this.getClaim(child.world, orphans.get(child));
            if (parent != null) {
                child.parent = parent;
                this.addClaim(child, false);
            }
        }
    }

    void loadPlayerData(WorldProperties worldProperties, File[] files) throws Exception {
        ConcurrentHashMap<Claim, UUID> orphans = new ConcurrentHashMap<>();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) // avoids folders
            {
                // the filename is the claim ID. try to parse it
                UUID playerUUID;

                try {
                    playerUUID = UUID.fromString(files[i].getName());
                } catch (Exception e) {
                    GriefPrevention.AddLogEntry("ERROR!! could not read player file " + files[i].getAbsolutePath());
                    continue;
                }

                try {
                    this.createPlayerWorldStorageData(worldProperties, playerUUID);
                }

                // if there's any problem with the file's content, log an error message and skip it
                catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("World not found")) {
                        files[i].delete();
                    } else {
                        StringWriter errors = new StringWriter();
                        e.printStackTrace(new PrintWriter(errors));
                        GriefPrevention.AddLogEntry(files[i].getName() + " " + errors.toString(), CustomLogEntryTypes.Exception);
                    }
                }
            }
        }
    }

    Claim loadClaim(File file, ArrayList<UUID> out_parentID, UUID claimID) throws IOException, Exception {
        return this.loadClaim(file, out_parentID, file.lastModified(), claimID);
    }

    Claim loadClaim(File claimFile, ArrayList<UUID> out_parentID, long lastModifiedDate, UUID claimID)
            throws Exception {
        Claim claim;

        ClaimStorageData claimData = new ClaimStorageData(claimFile.toPath());

        // identify world the claim is in
        String worldUniqueId = claimData.getConfig().worldUniqueId;
        World world = null;
        for (World w : Sponge.getGame().getServer().getWorlds()) {
            if (w.getUniqueId().equals(UUID.fromString(worldUniqueId))) {
                world = w;
                break;
            }
        }

        if (world == null) {
            throw new Exception("World UUID not found: \"" + worldUniqueId + "\"");
        }

        // boundaries
        Vector3i lesserBoundaryCornerPos = positionFromString(claimData.getConfig().lesserBoundaryCornerPos);
        Vector3i greaterBoundaryCornerPos = positionFromString(claimData.getConfig().greaterBoundaryCornerPos);
        Location<World> lesserBoundaryCorner = new Location<World>(world, lesserBoundaryCornerPos);
        Location<World> greaterBoundaryCorner = new Location<World>(world, greaterBoundaryCornerPos);

        // owner
        String ownerIdentifier = claimData.getConfig().ownerUniqueId;
        UUID ownerID = null;
        if (!ownerIdentifier.isEmpty()) {
            try {
                ownerID = UUID.fromString(ownerIdentifier);
            } catch (Exception ex) {
                GriefPrevention.AddLogEntry("Error - this is not a valid UUID: " + ownerIdentifier + ".");
                GriefPrevention.AddLogEntry("  Converted land claim to administrative @ " + lesserBoundaryCorner.toString());
            }
        }

        List<String> builders = claimData.getConfig().builders;
        List<String> containers = claimData.getConfig().containers;
        List<String> accessors = claimData.getConfig().accessors;
        List<String> managers = claimData.getConfig().managers;
        if (claimData.getConfig().parentUniqueId != null && !claimData.getConfig().parentUniqueId.equals("")) {
            out_parentID.add(UUID.fromString(claimData.getConfig().parentUniqueId));
        }

        // instantiate
        claim = new Claim(lesserBoundaryCorner, greaterBoundaryCorner, ownerID, builders, containers, accessors, managers, claimID);
        claim.modifiedDate = new Date(lastModifiedDate);
        claim.id = claimID;
        claim.world = lesserBoundaryCorner.getExtent();
        claim.claimData = claimData;
        claim.context = new Context("claim", claim.id.toString());

        if (this.worldClaims.get(world.getProperties().getUniqueId()) == null) {
            this.worldClaims.put(world.getProperties().getUniqueId(), new ArrayList<Claim>());
        } else {
            this.worldClaims.get(world.getProperties().getUniqueId()).add(claim);
        }
        return claim;
    }

    public void updateClaimData(Claim claim, File claimFile) {

        ClaimStorageData claimData = claim.claimData;

        if (claimData == null) {
            claimData = new ClaimStorageData(claimFile.toPath());
            claim.claimData = claimData;
        }

        // owner
        if (claim.ownerID != null) {
            claimData.getConfig().ownerUniqueId = claim.ownerID.toString();
        }
        if (claim.parent != null) {
            claimData.getConfig().parentUniqueId = claim.parent.id.toString();
        }

        claimData.getConfig().worldUniqueId = claim.world.getUniqueId().toString();
        claimData.getConfig().lesserBoundaryCornerPos = positionToString(claim.lesserBoundaryCorner);
        claimData.getConfig().greaterBoundaryCornerPos = positionToString(claim.greaterBoundaryCorner);
        claim.getPermissions(claimData.getConfig().builders, claimData.getConfig().containers, claimData.getConfig().accessors, claimData.getConfig().managers);

        claimData.save();
    }

    @Override
    synchronized void writeClaimToStorage(Claim claim) {
        Path rootPath = Sponge.getGame().getSavesDirectory().resolve(Sponge.getGame().getServer().getDefaultWorld().get().getWorldName());

        try {
            // open the claim's file
            Path claimDataFolderPath = null;
            // check if main world
            if (claim.world.getUniqueId() == Sponge.getGame().getServer().getDefaultWorld().get().getUniqueId()) {
                claimDataFolderPath = rootPath.resolve(claimDataPath);
            } else {
                claimDataFolderPath = rootPath.resolve(claim.world.getName()).resolve(claimDataPath);
            }

            File claimFile = new File(claimDataFolderPath + File.separator + claim.id);
            if (!claimFile.exists()) {
                claimFile.createNewFile();
            }

            updateClaimData(claim, claimFile);
        }

        // if any problem, log it
        catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            GriefPrevention.AddLogEntry(claim.id + " " + errors.toString(), CustomLogEntryTypes.Exception);
        }
    }

    // deletes a claim from the file system
    @Override
    synchronized void deleteClaimFromSecondaryStorage(Claim claim) {
        try {
            Files.delete(claim.claimData.filePath);
        } catch (IOException e) {
            e.printStackTrace();
            GriefPrevention.AddLogEntry("Error: Unable to delete claim file \"" + claim.claimData.filePath + "\".");
        }
    }

    @Override
    public PlayerData getPlayerData(WorldProperties worldProperties, UUID playerUniqueId) {
        PlayerData playerData = this.playerUniqueIdToPlayerDataMap.get(playerUniqueId);
        if (playerData != null) {
            return playerData;
        }


        return createPlayerWorldStorageData(worldProperties, playerUniqueId);
    }

    @Override
    public PlayerData createPlayerWorldStorageData(WorldProperties worldProperties, UUID playerUniqueId) {
        PlayerData playerData = this.playerUniqueIdToPlayerDataMap.get(playerUniqueId);
        if (playerData != null) {
            return playerData;
        }
        Path rootPath = Sponge.getGame().getSavesDirectory().resolve(Sponge.getGame().getServer().getDefaultWorld().get().getWorldName());
        Path playerFilePath = null;
        if (worldProperties.getUniqueId() == Sponge.getGame().getServer().getDefaultWorld().get().getUniqueId()) {
            playerFilePath = rootPath.resolve(playerDataPath).resolve(playerUniqueId.toString());
        } else {
            playerFilePath = rootPath.resolve(worldProperties.getWorldName()).resolve(playerDataPath).resolve(playerUniqueId.toString());
        }

        PlayerStorageData storageData = new PlayerStorageData(playerFilePath, playerUniqueId, GriefPrevention.getActiveConfig(worldProperties).getConfig().general.claimInitialBlocks);
        playerData = new PlayerData();
        playerData.playerID = playerUniqueId;
        playerData.worldStorageData.put(worldProperties.getUniqueId(), storageData);

        // shove that new player data into the hash map cache
        this.playerUniqueIdToPlayerDataMap.put(playerUniqueId, playerData);
        playerData.initializePlayerWorldClaims(worldProperties);
        return playerData;
    }

    // grants a group (players with a specific permission) bonus claim blocks as
    // long as they're still members of the group
    // TODO - hook into permissions
    @Override
    synchronized void saveGroupBonusBlocks(String groupName, int currentValue) {
        /*
        // write changes to file to ensure they don't get lost
        BufferedWriter outStream = null;
        try {
            // open the group's file
            File groupDataFile = new File(playerDataFolderPath + File.separator + "$" + groupName);
            groupDataFile.createNewFile();
            outStream = new BufferedWriter(new FileWriter(groupDataFile));

            // first line is number of bonus blocks
            outStream.write(String.valueOf(currentValue));
            outStream.newLine();
        }

        // if any problem, log it
        catch (Exception e) {
            GriefPrevention.AddLogEntry("Unexpected exception saving data for group \"" + groupName + "\": " + e.getMessage());
        }

        try {
            // close the file
            if (outStream != null) {
                outStream.close();
            }
        } catch (IOException exception) {
        }
        */
    }

    @Override
    int getSchemaVersionFromStorage() {
        File schemaVersionFile = schemaVersionFilePath.toFile();
        if (schemaVersionFile.exists()) {
            BufferedReader inStream = null;
            int schemaVersion = 0;
            try {
                inStream = new BufferedReader(new FileReader(schemaVersionFile.getAbsolutePath()));

                // read the version number
                String line = inStream.readLine();

                // try to parse into an int value
                schemaVersion = Integer.parseInt(line);
            } catch (Exception e) {
            }

            try {
                if (inStream != null)
                    inStream.close();
            } catch (IOException exception) {
            }

            return schemaVersion;
        } else {
            this.updateSchemaVersionInStorage(0);
            return 0;
        }
    }

    @Override
            void updateSchemaVersionInStorage(int versionToSet) {
        BufferedWriter outStream = null;

        try {
            // open the file and write the new value
            File schemaVersionFile = schemaVersionFilePath.toFile();
            schemaVersionFile.createNewFile();
            outStream = new BufferedWriter(new FileWriter(schemaVersionFile));

            outStream.write(String.valueOf(versionToSet));
        }

        // if any problem, log it
        catch (Exception e) {
            GriefPrevention.AddLogEntry("Unexpected exception saving schema version: " + e.getMessage());
        }

        // close the file
        try {
            if (outStream != null)
                outStream.close();
        } catch (IOException exception) {
        }

    }

    @Override
            void incrementNextClaimID() {
        // TODO Auto-generated method stub
        
    }

    @Override
            PlayerData getPlayerDataFromStorage(UUID playerID) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
            void overrideSavePlayerData(UUID playerID, PlayerData playerData) {
        // TODO Auto-generated method stub
        
    }

}
