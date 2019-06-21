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

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Maps;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimBlockSystem;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.claim.GPClaimManager;
import me.ryanhamshire.griefprevention.configuration.ClaimStorageData;
import me.ryanhamshire.griefprevention.configuration.ClaimTemplateStorage;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.configuration.TownStorageData;
import me.ryanhamshire.griefprevention.configuration.type.ConfigBase;
import me.ryanhamshire.griefprevention.logging.CustomLogEntryTypes;
import me.ryanhamshire.griefprevention.migrator.RedProtectMigrator;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.common.bridge.world.DimensionTypeBridge;

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

//manages data stored in the file system
public class FlatFileDataStore extends DataStore {

    private final static Path migrationVersionFilePath = dataLayerFolderPath.resolve("_migrationVersion");
    private final static Path schemaVersionFilePath = dataLayerFolderPath.resolve("_schemaVersion");
    private final static Path worldsConfigFolderPath = dataLayerFolderPath.resolve("worlds");
    public final static Path claimDataPath = Paths.get("GriefPreventionData", "ClaimData");
    public final static Path claimTemplatePath = claimDataPath.resolve("Templates");
    public final static Path worldClaimDataPath = Paths.get("GriefPreventionData", "WorldClaim");
    public final static Path playerDataPath = Paths.get("GriefPreventionData", "PlayerData");
    public final static Path redProtectDataPath = GriefPreventionPlugin.instance.getConfigPath().getParent().resolve("RedProtect").resolve("data");
    public final static Map<UUID, Task> cleanupClaimTasks = Maps.newHashMap();
    private final Path rootConfigPath = GriefPreventionPlugin.instance.getConfigPath().resolve("worlds");
    public static Path rootWorldSavePath;
    private int claimLoadCount = 0;

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
                GriefPreventionPlugin.addLogEntry(count + " total claim templates loaded.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerWorld(WorldProperties worldProperties) {
        DimensionType dimType = worldProperties.getDimensionType();
        Path dimPath = rootConfigPath.resolve(((DimensionTypeBridge) dimType).getModId()).resolve(((DimensionTypeBridge) dimType).getEnumName());
        if (!Files.exists(dimPath.resolve(worldProperties.getWorldName()))) {
            try {
                Files.createDirectories(dimPath.resolve(worldProperties.getWorldName()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // create/load configs
        GriefPreventionConfig<ConfigBase> dimConfig = new GriefPreventionConfig<>(ConfigBase.class, dimPath.resolve("dimension.conf"), DataStore.globalConfig);
        GriefPreventionConfig<ConfigBase> worldConfig = new GriefPreventionConfig<>(ConfigBase.class, dimPath.resolve(worldProperties.getWorldName()).resolve("world.conf"), dimConfig);
        DataStore.dimensionConfigMap.put(worldProperties.getUniqueId(), dimConfig);
        DataStore.worldConfigMap.put(worldProperties.getUniqueId(), worldConfig);

        GPClaimManager claimWorldManager = new GPClaimManager(worldProperties);
        this.claimWorldManagers.put(worldProperties.getUniqueId(), claimWorldManager);

        Path newWorldDataPath = dimPath.resolve(worldProperties.getWorldName());

        try {
            // Create data folders if they do not exist
            if (!Files.exists(newWorldDataPath.resolve("ClaimData"))) {
                Files.createDirectories(newWorldDataPath.resolve("ClaimData"));
            }
            if (!Files.exists(newWorldDataPath.resolve("ClaimData").resolve("wilderness"))) {
                Files.createDirectories(newWorldDataPath.resolve("ClaimData").resolve("wilderness"));
            }
            if (DataStore.USE_GLOBAL_PLAYER_STORAGE) {
                if (!globalPlayerDataPath.toFile().exists()) {
                    Files.createDirectories(globalPlayerDataPath);
                }
            } else if (!Files.exists(newWorldDataPath.resolve("PlayerData"))) {
                Files.createDirectories(newWorldDataPath.resolve("PlayerData"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadWorldData(World world) {
        final WorldProperties worldProperties = world.getProperties();
        final DimensionType dimType = worldProperties.getDimensionType();
        final Path dimPath = rootConfigPath.resolve(((DimensionTypeBridge) dimType).getModId()).resolve(((DimensionTypeBridge) dimType).getEnumName());
        final Path newWorldDataPath = dimPath.resolve(worldProperties.getWorldName());
        GPClaimManager claimWorldManager = this.claimWorldManagers.get(worldProperties.getUniqueId());
        if (claimWorldManager == null) {
            this.registerWorld(worldProperties);
            claimWorldManager = this.claimWorldManagers.get(worldProperties.getUniqueId());
        }

        try {
            // Migrate RedProtectData if enabled
            if (GriefPreventionPlugin.getGlobalConfig().getConfig().migrator.redProtectMigrator) {
                Path redProtectFilePath = redProtectDataPath.resolve("data_" + worldProperties.getWorldName() + ".conf");
                Path gpMigratedPath = redProtectDataPath.resolve("gp_migrated_" + worldProperties.getWorldName());
                if (Files.exists(redProtectFilePath) && !Files.exists(gpMigratedPath)) {
                    RedProtectMigrator.migrate(world, redProtectFilePath, newWorldDataPath.resolve("ClaimData"));
                    Files.createFile(gpMigratedPath);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Load wilderness claim first
        final Path wildernessFilePath = newWorldDataPath.resolve("ClaimData").resolve("wilderness").resolve(worldProperties.getUniqueId().toString());
        if (Files.exists(wildernessFilePath)) {
            try {
                this.loadClaim(wildernessFilePath.toFile(), worldProperties, worldProperties.getUniqueId());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            claimWorldManager.createWildernessClaim(worldProperties);
        }

        // Load Claim Data
        try {
            File[] files = newWorldDataPath.resolve("ClaimData").toFile().listFiles();
            if (files != null && files.length > 0) {
                this.loadClaimData(files, worldProperties);
                GriefPreventionPlugin.instance.getLogger().info("[" + worldProperties.getWorldName() + "] " + this.claimLoadCount + " total claims loaded.");
            }

            if (GriefPreventionPlugin.getGlobalConfig().getConfig().playerdata.useGlobalPlayerDataStorage) {
                files = globalPlayerDataPath.toFile().listFiles();
            } else {
                files = newWorldDataPath.resolve("PlayerData").toFile().listFiles();
            }
            if (files != null && files.length > 0) {
                this.loadPlayerData(worldProperties, files);
            }

            // If a wilderness claim was not loaded, create a new one
            if (claimWorldManager.getWildernessClaim() == null) {
                claimWorldManager.createWildernessClaim(worldProperties);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // handle default flag permissions
        this.setupDefaultPermissions(world);
        this.claimLoadCount = 0;
    }

    public void unloadWorldData(WorldProperties worldProperties) {
        GPClaimManager claimWorldManager = this.getClaimWorldManager(worldProperties);
        for (Claim claim : claimWorldManager.getWorldClaims()) {
            ((GPClaim) claim).unload();
        }
        // Task must be cancelled before removing the claimWorldManager reference to avoid a memory leak
        Task cleanupTask = cleanupClaimTasks.get(worldProperties.getUniqueId());
        if (cleanupTask != null) {
           cleanupTask.cancel();
           cleanupClaimTasks.remove(worldProperties.getUniqueId());
        }

        claimWorldManager.unload();
        this.claimWorldManagers.remove(worldProperties.getUniqueId());
        DataStore.dimensionConfigMap.remove(worldProperties.getUniqueId());
        DataStore.worldConfigMap.remove(worldProperties.getUniqueId());
    }

    void loadClaimData(File[] files, WorldProperties worldProperties) throws Exception {
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isFile()) {
                this.loadClaimFile(file, worldProperties);
            }
        }
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                this.loadClaimData(file.listFiles(), worldProperties);
            }
        }
    }

    void loadClaimFile(File file, WorldProperties worldProperties) {
        if (file.isFile()) // avoids folders
        {
            // the filename is the claim ID. try to parse it
            UUID claimId;

            try {
                final String fileName = file.getName();
                // UUID's should always be 36 in length
                if (fileName.length() != 36) {
                    return;
                }

                claimId = UUID.fromString(fileName);
            } catch (Exception e) {
                GriefPreventionPlugin.instance.getLogger().error("Could not read claim file " + file.getAbsolutePath());
                return;
            }

            try {
               this.loadClaim(file, worldProperties, claimId);
            } catch (Exception e) {
                GriefPreventionPlugin.instance.getLogger().error(file.getAbsolutePath() + " failed to load.");
                e.printStackTrace();
            }
        }
    }

    void loadPlayerData(WorldProperties worldProperties, File[] files) throws Exception {
        final boolean resetMigration = GriefPreventionPlugin.getGlobalConfig().getConfig().playerdata.resetMigrations;
        final boolean resetClaimData = GriefPreventionPlugin.getGlobalConfig().getConfig().playerdata.resetAccruedClaimBlocks;
        final int migration2dRate = GriefPreventionPlugin.getGlobalConfig().getConfig().playerdata.migrateAreaRate;
        final int migration3dRate = GriefPreventionPlugin.getGlobalConfig().getConfig().playerdata.migrateVolumeRate;
        boolean migrate = false;
        if (resetMigration || resetClaimData || (migration2dRate > -1 && GriefPreventionPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.AREA) 
                || (migration3dRate > -1 && GriefPreventionPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME)) {
            // load all player data if migrating
            migrate = true;
        }
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) // avoids folders
            {
                // the filename is the claim ID. try to parse it
                UUID playerUUID;

                try {
                    playerUUID = UUID.fromString(files[i].getName());
                } catch (Exception e) {
                    GriefPreventionPlugin.instance.getLogger().error("Could not read player file " + files[i].getAbsolutePath());
                    continue;
                }

                if (!migrate && !Sponge.getServer().getPlayer(playerUUID).isPresent()) {
                    continue;
                }

                try {
                    this.getOrCreatePlayerData(worldProperties, playerUUID);
                }

                // if there's any problem with the file's content, log an error message and skip it
                catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().contains("World not found")) {
                        files[i].delete();
                    } else {
                        StringWriter errors = new StringWriter();
                        e.printStackTrace(new PrintWriter(errors));
                        GriefPreventionPlugin.addLogEntry(files[i].getName() + " " + errors.toString(), CustomLogEntryTypes.Exception);
                    }
                }
            }
        }
    }

    public GPClaim loadClaim(File claimFile, WorldProperties worldProperties, UUID claimId)
            throws Exception {
        GPClaim claim;

        final GPClaimManager claimManager = this.getClaimWorldManager(worldProperties);
        if (claimManager.getWildernessClaim() != null && claimManager.getWildernessClaim().getUniqueId().equals(claimId)) {
            return null;
        }
        boolean isTown = claimFile.toPath().getParent().endsWith("town");
        boolean writeToStorage = false;
        ClaimStorageData claimStorage = null;
        if (isTown) {
            claimStorage = new TownStorageData(claimFile.toPath(), worldProperties.getUniqueId());
        } else {
            claimStorage = new ClaimStorageData(claimFile.toPath(), worldProperties.getUniqueId());
        }

        final ClaimType type = claimStorage.getConfig().getType();
        final UUID parent = claimStorage.getConfig().getParent().orElse(null);
        final String fileName = claimFile.getName();
        final World world = Sponge.getServer().loadWorld(worldProperties).orElse(null);
        if (world == null) {
            throw new Exception("World [Name: " + worldProperties.getWorldName() + "][UUID: " + worldProperties.getUniqueId().toString() + "] is not loaded.");
        }

        if (claimFile.getParentFile().getName().equalsIgnoreCase("claimdata")) {
            final Path newPath = claimStorage.filePath.getParent().resolve(type.name().toLowerCase());
            Files.createDirectories(newPath);
            Files.move(claimStorage.filePath, newPath.resolve(fileName));
            claimStorage.filePath = newPath.resolve(fileName);
            claimStorage = new ClaimStorageData(claimStorage.filePath, worldProperties.getUniqueId());
        }

        // identify world the claim is in
        UUID worldUniqueId = claimStorage.getConfig().getWorldUniqueId();
        if (!worldProperties.getUniqueId().equals(worldUniqueId)) {
            GriefPreventionPlugin.addLogEntry("Found mismatch world UUID in " + type.name().toLowerCase() + " claim file " + claimFile + ". Expected " + worldProperties.getUniqueId() + ", found " + worldUniqueId + ". Updating file with correct UUID...", CustomLogEntryTypes.Exception);
            claimStorage.getConfig().setWorldUniqueId(worldProperties.getUniqueId());
            writeToStorage = true;
        }

        // boundaries
        final boolean cuboid = claimStorage.getConfig().isCuboid();
        Vector3i lesserCorner = claimStorage.getConfig().getLesserBoundaryCornerPos();
        Vector3i greaterCorner = claimStorage.getConfig().getGreaterBoundaryCornerPos();
        if (lesserCorner == null || greaterCorner == null) {
            throw new Exception("Claim file '" + claimFile.getName() + "' has corrupted data and cannot be loaded. Skipping...");
        }

        Location<World> lesserBoundaryCorner = new Location<World>(world, lesserCorner);
        Location<World> greaterBoundaryCorner = new Location<World>(world, greaterCorner);

        // owner
        UUID ownerID = claimStorage.getConfig().getOwnerUniqueId();
        if (ownerID == null) {
            GriefPreventionPlugin.addLogEntry("Error - this is not a valid UUID: " + ownerID + ".");
            GriefPreventionPlugin.addLogEntry("  Converted land claim to administrative @ " + lesserBoundaryCorner.toString());
        }

        // instantiate
        claim = new GPClaim(lesserBoundaryCorner, greaterBoundaryCorner, claimId, claimStorage.getConfig().getType(), ownerID, cuboid);
        claim.setClaimStorage(claimStorage);
        claim.setClaimData(claimStorage.getConfig());

        // add parent claim first
        if (parent != null) {
            GPClaim parentClaim = null;
            try {
                parentClaim = (GPClaim) claimManager.getClaimByUUID(parent).orElse(null);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            if (parentClaim == null) {
                throw new Exception("Required parent claim '" + parent + " no longer exists. Skipping...");
            }
            claim.parent = parentClaim;
        }

        claimManager.addClaim(claim, writeToStorage);
        this.claimLoadCount++;
        return claim;
    }

    @Override
    public void writeClaimToStorage(GPClaim claim) {
        try {
            ClaimStorageData claimStorage = claim.getClaimStorage();
            claim.updateClaimStorageData();
            claimStorage.save();
        }

        // if any problem, log it
        catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            GriefPreventionPlugin.addLogEntry(claim.id + " " + errors.toString(), CustomLogEntryTypes.Exception);
        }
    }

    // deletes a claim from the file system
    @Override
    public void deleteClaimFromSecondaryStorage(GPClaim claim) {
        try {
            Files.delete(claim.getClaimStorage().filePath);
            if (claim.getClaimStorage().folderPath.toFile().listFiles().length == 0) {
                Files.delete(claim.getClaimStorage().folderPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
            GriefPreventionPlugin.addLogEntry("Error: Unable to delete claim file \"" + claim.getClaimStorage().filePath + "\".");
        }
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
            GriefPreventionPlugin.addLogEntry("Unexpected exception saving schema version: " + e.getMessage());
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
    int getMigrationVersionFromStorage() {
        File migrationVersionFile = migrationVersionFilePath.toFile();
        if (migrationVersionFile.exists()) {
            BufferedReader inStream = null;
            int migrationVersion = 0;
            try {
                inStream = new BufferedReader(new FileReader(migrationVersionFile.getAbsolutePath()));

                // read the version number
                String line = inStream.readLine();

                // try to parse into an int value
                migrationVersion = Integer.parseInt(line);
            } catch (Exception e) {
            }

            try {
                if (inStream != null) {
                    inStream.close();
                }
            } catch (IOException exception) {
            }

            return migrationVersion;
        } else {
            this.updateMigrationVersionInStorage(0);
            return 0;
        }
    }

    @Override
    void updateMigrationVersionInStorage(int versionToSet) {
        BufferedWriter outStream = null;

        try {
            // open the file and write the new value
            File migrationVersionFile = migrationVersionFilePath.toFile();
            migrationVersionFile.createNewFile();
            outStream = new BufferedWriter(new FileWriter(migrationVersionFile));
            outStream.write(String.valueOf(versionToSet));
        }

        // if any problem, log it
        catch (Exception e) {
            GriefPreventionPlugin.addLogEntry("Unexpected exception saving migration version: " + e.getMessage());
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
    GPPlayerData getPlayerDataFromStorage(UUID playerID) {
        return null;
    }

    @Override
    void overrideSavePlayerData(UUID playerID, GPPlayerData playerData) {
    }

}
