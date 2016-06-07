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
package me.ryanhamshire.griefprevention;

import com.flowpowered.math.vector.Vector3i;
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.configuration.ClaimStorageData;
import me.ryanhamshire.griefprevention.configuration.ClaimTemplateStorage;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig.Type;
import me.ryanhamshire.griefprevention.configuration.SubDivisionDataConfig;
import me.ryanhamshire.griefprevention.configuration.types.DimensionConfig;
import me.ryanhamshire.griefprevention.configuration.types.WorldConfig;
import me.ryanhamshire.griefprevention.task.CleanupUnusedClaimsTask;
import me.ryanhamshire.griefprevention.util.BlockUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

//manages data stored in the file system
public class FlatFileDataStore extends DataStore {

    private final static Path schemaVersionFilePath = dataLayerFolderPath.resolve("_schemaVersion");
    private final static Path worldsConfigFolderPath = dataLayerFolderPath.resolve("worlds");
    public final static Path claimDataPath = Paths.get("GriefPreventionData", "ClaimData");
    public final static Path claimTemplatePath = claimDataPath.resolve("Templates");
    public final static Path playerDataPath = Paths.get("GriefPreventionData", "PlayerData");
    private final Path rootConfigPath = Sponge.getGame().getSavesDirectory().resolve("config").resolve("GriefPrevention").resolve("worlds");
    public static Path rootWorldSavePath;

    public FlatFileDataStore() {
    }

    @Override
    void initialize() throws Exception {
        // ensure data folders exist
        File worldsDataFolder = worldsConfigFolderPath.toFile();

        if (!worldsDataFolder.exists()) {
            worldsDataFolder.mkdirs();
        }

        rootWorldSavePath = Sponge.getGame().getSavesDirectory().resolve(Sponge.getServer().getDefaultWorldName());

        super.initialize();
    }

    @Override
    public void loadClaimTemplates() {
        try {
            if (Files.exists(rootWorldSavePath.resolve(claimTemplatePath))) {
                File[] files = rootWorldSavePath.resolve(claimTemplatePath).toFile().listFiles();
                int count = 0;
                for (File file : files) {
                    ClaimTemplateStorage templateStorage = new ClaimTemplateStorage(file.toPath());
                    String templateName = templateStorage.getConfig().getTemplateName();
                    if (!templateName.isEmpty()) {
                        globalTemplates.put(templateName, templateStorage);
                        count++;
                    }
                }
                GriefPrevention.addLogEntry(count + " total claim templates loaded.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadWorldData(WorldProperties worldProperties) {
        DimensionType dimType = worldProperties.getDimensionType();
        if (!Files.exists(rootConfigPath.resolve(dimType.getId()).resolve(worldProperties.getWorldName()))) {
            try {
                Files.createDirectories(rootConfigPath.resolve(dimType.getId()).resolve(worldProperties.getWorldName()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // create/load configs
        // create dimension config
        DataStore.dimensionConfigMap.put(worldProperties.getUniqueId(),
                new GriefPreventionConfig<DimensionConfig>(Type.DIMENSION, rootConfigPath.resolve(dimType.getId()).resolve("dimension.conf")));
        // create world config
        DataStore.worldConfigMap.put(worldProperties.getUniqueId(), new GriefPreventionConfig<>(Type.WORLD,
                rootConfigPath.resolve(dimType.getId()).resolve(worldProperties.getWorldName()).resolve("world.conf")));

        // check if claims are supported
        GriefPreventionConfig<WorldConfig> worldConfig = DataStore.worldConfigMap.get(worldProperties.getUniqueId());
        if (worldConfig != null && worldConfig.getConfig().configEnabled && worldConfig.getConfig().claim.claimMode == 0) {
            GriefPrevention.addLogEntry("Error - World '" + worldProperties.getWorldName() + "' does not allow claims. Skipping...");
            return;
        }

        if (!GriefPrevention.getGlobalConfig().getConfig().playerdata.useGlobalPlayerDataStorage) {
            this.claimWorldManagers.put(worldProperties.getUniqueId(), new ClaimWorldManager(worldProperties));
            // TODO - disable for now
            // run cleanup task
            //CleanupUnusedClaimsTask cleanupTask = new CleanupUnusedClaimsTask(worldProperties);
            //Sponge.getGame().getScheduler().createTaskBuilder().delay(1, TimeUnit.MINUTES).execute(cleanupTask).submit(GriefPrevention.instance);
        }

        // check if world has existing data
        Path worldClaimDataPath = Paths.get(worldProperties.getWorldName()).resolve(claimDataPath);
        Path worldPlayerDataPath = Paths.get(worldProperties.getWorldName()).resolve(playerDataPath);
        if (worldProperties.getUniqueId() == Sponge.getGame().getServer().getDefaultWorld().get().getUniqueId()) {
            worldClaimDataPath = claimDataPath;
            worldPlayerDataPath = playerDataPath;
        }

        try {
            if (Files.exists(rootWorldSavePath.resolve(worldClaimDataPath))) {
                File[] files = rootWorldSavePath.resolve(worldClaimDataPath).toFile().listFiles();
                this.loadClaimData(files, worldProperties);
                GriefPrevention.addLogEntry("[" + worldProperties.getWorldName() + "]" + files.length + " total claims loaded.");
            } else {
                Files.createDirectories(rootWorldSavePath.resolve(worldClaimDataPath));
            }
    
            if (Files.exists(rootWorldSavePath.resolve(worldPlayerDataPath))) {
                File[] files = rootWorldSavePath.resolve(worldPlayerDataPath).toFile().listFiles();
                this.loadPlayerData(worldProperties, files);
            }
            if (!Files.exists(rootWorldSavePath.resolve(worldPlayerDataPath))) {
                Files.createDirectories(rootWorldSavePath.resolve(worldPlayerDataPath));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unloadWorldData(WorldProperties worldProperties) {
        if (GriefPrevention.getGlobalConfig().getConfig().playerdata.useGlobalPlayerDataStorage) {
            this.claimWorldManagers.remove(worldProperties);
        }
    }

    void loadClaimData(File[] files, WorldProperties worldProperties) throws Exception {
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) // avoids folders
            {
                // the filename is the claim ID. try to parse it
                UUID claimId;

                try {
                    claimId = UUID.fromString(files[i].getName());
                } catch (Exception e) {
                    GriefPrevention.addLogEntry("ERROR!! could not read claim file " + files[i].getAbsolutePath());
                    continue;
                }

                try {
                   this.loadClaim(files[i], worldProperties, claimId);
                }

                // if there's any problem with the file's content, log an error message and skip it
                catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("World not found")) {
                        files[i].delete();
                    } else {
                        StringWriter errors = new StringWriter();
                        e.printStackTrace(new PrintWriter(errors));
                        GriefPrevention.addLogEntry(files[i].getName() + " " + errors.toString(), CustomLogEntryTypes.Exception);
                    }
                }
            }
        }
    }

    void loadPlayerData(WorldProperties worldProperties, File[] files) throws Exception {
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) // avoids folders
            {
                // the filename is the claim ID. try to parse it
                UUID playerUUID;

                try {
                    playerUUID = UUID.fromString(files[i].getName());
                } catch (Exception e) {
                    GriefPrevention.addLogEntry("ERROR!! could not read player file " + files[i].getAbsolutePath());
                    continue;
                }

                if (!Sponge.getServer().getPlayer(playerUUID).isPresent()) {
                    return;
                }

                try {
                    this.createPlayerData(worldProperties, playerUUID);
                }

                // if there's any problem with the file's content, log an error message and skip it
                catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("World not found")) {
                        files[i].delete();
                    } else {
                        StringWriter errors = new StringWriter();
                        e.printStackTrace(new PrintWriter(errors));
                        GriefPrevention.addLogEntry(files[i].getName() + " " + errors.toString(), CustomLogEntryTypes.Exception);
                    }
                }
            }
        }
    }

    Claim loadClaim(File claimFile, WorldProperties worldProperties, UUID claimId)
            throws Exception {
        Claim claim;

        ClaimStorageData claimStorage = new ClaimStorageData(claimFile.toPath());

        // identify world the claim is in
        UUID worldUniqueId = claimStorage.getConfig().getWorldUniqueId();
        if (!worldProperties.getUniqueId().equals(worldUniqueId)) {
            GriefPrevention.addLogEntry("Found mismatch world UUID in claim file " + claimFile + ". Expected " + worldProperties.getUniqueId() + ", found " + worldUniqueId + ". Updating file with correct UUID...", CustomLogEntryTypes.Exception);
            claimStorage.getConfig().setWorldUniqueId(worldProperties.getUniqueId());
        }

        World world = Sponge.getServer().loadWorld(worldProperties).orElse(null);

        if (world == null) {
            throw new Exception("World [Name: " + worldProperties.getWorldName() + "][UUID: " + worldProperties.getUniqueId().toString() + "] is not loaded.");
        }

        // boundaries
        Vector3i lesserBoundaryCornerPos = BlockUtils.positionFromString(claimStorage.getConfig().getLesserBoundaryCorner());
        Vector3i greaterBoundaryCornerPos = BlockUtils.positionFromString(claimStorage.getConfig().getGreaterBoundaryCorner());
        Location<World> lesserBoundaryCorner = new Location<World>(world, lesserBoundaryCornerPos);
        Location<World> greaterBoundaryCorner = new Location<World>(world, greaterBoundaryCornerPos);

        // owner
        UUID ownerID = claimStorage.getConfig().getOwnerUniqueId();
        if (ownerID == null) {
            GriefPrevention.addLogEntry("Error - this is not a valid UUID: " + ownerID + ".");
            GriefPrevention.addLogEntry("  Converted land claim to administrative @ " + lesserBoundaryCorner.toString());
        }

        // instantiate
        claim = new Claim(lesserBoundaryCorner, greaterBoundaryCorner, claimId);
        claim.ownerID = ownerID;
        claim.world = lesserBoundaryCorner.getExtent();
        claim.type = claimStorage.getConfig().getClaimType();
        claim.setClaimStorage(claimStorage);
        claim.setClaimData(claimStorage.getConfig());
        claim.context = new Context("claim", claim.id.toString());

        // add parent claim first
        this.addClaim(claim, false);
        // check for subdivisions
        for(Map.Entry<UUID, SubDivisionDataConfig> mapEntry : claimStorage.getConfig().getSubdivisions().entrySet()) {
            SubDivisionDataConfig subDivisionData = mapEntry.getValue();
            Vector3i subLesserBoundaryCornerPos = BlockUtils.positionFromString(subDivisionData.getLesserBoundaryCorner());
            Vector3i subGreaterBoundaryCornerPos = BlockUtils.positionFromString(subDivisionData.getGreaterBoundaryCorner());
            Location<World> subLesserBoundaryCorner = new Location<World>(world, subLesserBoundaryCornerPos);
            Location<World> subGreaterBoundaryCorner = new Location<World>(world, subGreaterBoundaryCornerPos);

            Claim subDivision = new Claim(subLesserBoundaryCorner, subGreaterBoundaryCorner, mapEntry.getKey());
            subDivision.id = mapEntry.getKey();
            subDivision.world = subLesserBoundaryCorner.getExtent();
            subDivision.setClaimStorage(claimStorage);
            subDivision.context = new Context("claim", subDivision.id.toString());
            subDivision.parent = claim;
            subDivision.type = Claim.Type.SUBDIVISION;
            subDivision.setClaimData(subDivisionData);
            // add subdivision to parent
            claim.children.add(subDivision);
        }
        return claim;
    }

    public void updateClaimData(Claim claim, File claimFile) {
        if (claim.id == null) {
            claim.id = UUID.randomUUID();
        }

        ClaimStorageData claimStorage = claim.getClaimStorage();
        if (claimStorage == null) {
            claimStorage = new ClaimStorageData(claim, claimFile.toPath());
            claim.setClaimStorage(claimStorage);
            claim.setClaimData(claimStorage.getConfig());
        }

        // owner
        if (!claim.isSubdivision()) {
            claimStorage.getConfig().setClaimOwnerUniqueId(claim.ownerID);
            claimStorage.getConfig().setWorldUniqueId(claim.world.getUniqueId());
            claimStorage.getConfig().setClaimType(claim.type);
            claim.getClaimData().setLesserBoundaryCorner(BlockUtils.positionToString(claim.lesserBoundaryCorner));
            claim.getClaimData().setGreaterBoundaryCorner(BlockUtils.positionToString(claim.greaterBoundaryCorner));
        } else {
            if (claim.getClaimData() == null) {
                claim.setClaimData(new SubDivisionDataConfig(claim));
            }
            claimStorage.getConfig().getSubdivisions().put(claim.id, (SubDivisionDataConfig) claim.getClaimData());
        }

        // Will save next world save
        claim.getClaimData().setRequiresSave(true);
    }

    @Override
    void writeClaimToStorage(Claim claim) {
        try {
            // open the claim's file
            Path claimDataFolderPath = null;
            // check if main world
            if (claim.world.getUniqueId() == Sponge.getGame().getServer().getDefaultWorld().get().getUniqueId()) {
                claimDataFolderPath = rootWorldSavePath.resolve(claimDataPath);
            } else {
                claimDataFolderPath = rootWorldSavePath.resolve(claim.world.getName()).resolve(claimDataPath);
            }

            UUID claimId = claim.parent != null ? claim.parent.id : claim.id;
            File claimFile = new File(claimDataFolderPath + File.separator + claimId);
            if (!claimFile.exists()) {
                claimFile.createNewFile();
            }

            updateClaimData(claim, claimFile);
        }

        // if any problem, log it
        catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            GriefPrevention.addLogEntry(claim.id + " " + errors.toString(), CustomLogEntryTypes.Exception);
        }
    }

    // deletes a claim from the file system
    @Override
    void deleteClaimFromSecondaryStorage(Claim claim) {
        try {
            Files.delete(claim.getClaimStorage().filePath);
        } catch (IOException e) {
            e.printStackTrace();
            GriefPrevention.addLogEntry("Error: Unable to delete claim file \"" + claim.getClaimStorage().filePath + "\".");
        }
    }

    // grants a group (players with a specific permission) bonus claim blocks as
    // long as they're still members of the group
    // TODO - hook into permissions
    @Override
    void saveGroupBonusBlocks(String groupName, int currentValue) {
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
                if (inStream != null) {
                    inStream.close();
                }
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
            GriefPrevention.addLogEntry("Unexpected exception saving schema version: " + e.getMessage());
        }

        // close the file
        try {
            if (outStream != null) {
                outStream.close();
            }
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
