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
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.claim.GPClaimManager;
import me.ryanhamshire.griefprevention.configuration.ClaimStorageData;
import me.ryanhamshire.griefprevention.configuration.ClaimTemplateStorage;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig.Type;
import me.ryanhamshire.griefprevention.configuration.PlayerStorageData;
import me.ryanhamshire.griefprevention.configuration.TownStorageData;
import me.ryanhamshire.griefprevention.configuration.type.DimensionConfig;
import me.ryanhamshire.griefprevention.logging.CustomLogEntryTypes;
import me.ryanhamshire.griefprevention.migrator.RedProtectMigrator;
import me.ryanhamshire.griefprevention.util.BlockUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;
import org.spongepowered.common.interfaces.world.IMixinDimensionType;

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

    private final static Path schemaVersionFilePath = dataLayerFolderPath.resolve("_schemaVersion");
    private final static Path worldsConfigFolderPath = dataLayerFolderPath.resolve("worlds");
    public final static Path claimDataPath = Paths.get("GriefPreventionData", "ClaimData");
    public final static Path claimTemplatePath = claimDataPath.resolve("Templates");
    public final static Path worldClaimDataPath = Paths.get("GriefPreventionData", "WorldClaim");
    public final static Path playerDataPath = Paths.get("GriefPreventionData", "PlayerData");
    public final static Path polisDataPath = GriefPreventionPlugin.instance.getConfigPath().getParent().resolve("polis").resolve("data");
    public final static Path redProtectDataPath = GriefPreventionPlugin.instance.getConfigPath().getParent().resolve("RedProtect").resolve("data");
    public final static Map<UUID, Task> cleanupClaimTasks = Maps.newHashMap();
    private final Path rootConfigPath = GriefPreventionPlugin.instance.getConfigPath().resolve("worlds");
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
                GriefPreventionPlugin.addLogEntry(count + " total claim templates loaded.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerWorld(WorldProperties worldProperties) {
        DimensionType dimType = worldProperties.getDimensionType();
        Path dimPath = rootConfigPath.resolve(((IMixinDimensionType) dimType).getModId()).resolve(((IMixinDimensionType) dimType).getEnumName());
        if (!Files.exists(dimPath.resolve(worldProperties.getWorldName()))) {
            try {
                Files.createDirectories(dimPath.resolve(worldProperties.getWorldName()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // create/load configs
        // create dimension config
        DataStore.dimensionConfigMap.put(worldProperties.getUniqueId(),
                new GriefPreventionConfig<DimensionConfig>(Type.DIMENSION, dimPath.resolve("dimension.conf")));
        // create world config
        DataStore.worldConfigMap.put(worldProperties.getUniqueId(), new GriefPreventionConfig<>(Type.WORLD,
                dimPath.resolve(worldProperties.getWorldName()).resolve("world.conf")));

        GPClaimManager claimWorldManager = new GPClaimManager(worldProperties);
        this.claimWorldManagers.put(worldProperties.getUniqueId(), claimWorldManager);

        Path newWorldDataPath = dimPath.resolve(worldProperties.getWorldName());

        try {
            // Create data folders if they do not exist
            if (!Files.exists(newWorldDataPath.resolve("ClaimData"))) {
                Files.createDirectories(newWorldDataPath.resolve("ClaimData"));
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
        final Path dimPath = rootConfigPath.resolve(((IMixinDimensionType) dimType).getModId()).resolve(((IMixinDimensionType) dimType).getEnumName());
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

        // Load Claim Data
        try {
            File[] files = newWorldDataPath.resolve("ClaimData").toFile().listFiles();
            if (files != null && files.length > 0) {
                this.loadClaimData(files, worldProperties);
                GriefPreventionPlugin.instance.getLogger().info("[" + worldProperties.getWorldName() + "] " + files.length + " total claims loaded.");
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
        // migrate playerdata to new claim block system
        final int migration3dRate = GriefPreventionPlugin.getGlobalConfig().getConfig().playerdata.migration3dRate;
        final int migration2dRate = GriefPreventionPlugin.getGlobalConfig().getConfig().playerdata.migration2dRate;
        final int resetClaimBlockData = GriefPreventionPlugin.getGlobalConfig().getConfig().playerdata.resetClaimBlockData;
        if (migration3dRate <= -1 && migration2dRate <= -1 && resetClaimBlockData <= -1) {
            return;
        }
        if (GriefPreventionPlugin.wildernessCuboids && migration2dRate >= 0) {
            return;
        }
        if (!GriefPreventionPlugin.wildernessCuboids && migration3dRate >= 0) {
            return;
        }

        for (GPPlayerData playerData : claimWorldManager.getPlayerDataMap().values()) {
            final PlayerStorageData playerStorage = playerData.getStorageData();
            final int accruedBlocks = playerStorage.getConfig().getAccruedClaimBlocks();
            int newAccruedBlocks = accruedBlocks;
            // first check reset
            if (resetClaimBlockData > -1) {
                newAccruedBlocks = resetClaimBlockData;
                playerStorage.getConfig().setBonusClaimBlocks(0);
            } else if (migration3dRate > -1 && !playerStorage.getConfig().hasMigratedBlocks()) {
                newAccruedBlocks = accruedBlocks * migration3dRate;
                playerStorage.getConfig().setMigratedBlocks(true);
            } else if (migration2dRate > -1 && !playerStorage.getConfig().hasMigratedBlocks()) {
                newAccruedBlocks = accruedBlocks / migration2dRate;
                playerStorage.getConfig().setMigratedBlocks(true);
            }
            if (newAccruedBlocks < 0) {
                newAccruedBlocks = 0;
            }
            if (newAccruedBlocks > playerData.optionMaxAccruedBlocks) {
                newAccruedBlocks = playerData.optionMaxAccruedBlocks;
            }
            playerStorage.getConfig().setAccruedClaimBlocks(newAccruedBlocks);
            playerStorage.save();
        }
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
            }

            // if there's any problem with the file's content, log an error message and skip it
            catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("World not found")) {
                    file.delete();
                } else {
                    StringWriter errors = new StringWriter();
                    e.printStackTrace(new PrintWriter(errors));
                    GriefPreventionPlugin.addLogEntry(file.getName() + " " + errors.toString(), CustomLogEntryTypes.Exception);
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
                    GriefPreventionPlugin.instance.getLogger().error("Could not read player file " + files[i].getAbsolutePath());
                    continue;
                }

                if (!Sponge.getServer().getPlayer(playerUUID).isPresent()) {
                    return;
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

        boolean isTown = claimFile.toPath().getParent().endsWith("town");
        //boolean isTownChild = claimFile.toPath().getParent().getParent().endsWith("town");
        ClaimStorageData claimStorage = null;
        if (isTown) {
            claimStorage = new TownStorageData(claimFile.toPath(), worldProperties.getUniqueId());
        } else {
            claimStorage = new ClaimStorageData(claimFile.toPath(), worldProperties.getUniqueId());
        }

        final ClaimType type = claimStorage.getConfig().getType();
        final UUID parent = claimStorage.getConfig().getParent().orElse(null);
        final String fileName = claimFile.getName();
        final GriefPreventionConfig<?> activeConfig = GriefPreventionPlugin.getActiveConfig(worldProperties);
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
            // Validate 2D Y space and if between bedrock and sky, convert to 3D
            if (!claimStorage.getConfig().isCuboid()) {
                int adjustedY = claimStorage.getConfig().getLesserBoundaryCornerPos().getY() - activeConfig.getConfig().claim.extendIntoGroundDistance;
                if (adjustedY > 0) {
                    claimStorage.getConfig().setCuboid(true);
                    final Vector3i greaterBounderCorner = claimStorage.getConfig().getGreaterBoundaryCornerPos();
                    final Vector3i greaterCorner = new Vector3i(greaterBounderCorner.getX(), world.getDimension().getBuildHeight() - 1, greaterBounderCorner.getZ());
                    claimStorage.getConfig().setGreaterBoundaryCorner(BlockUtils.positionToString(greaterCorner));
                    claimStorage.save();
                }
            }
        }

        // identify world the claim is in
        UUID worldUniqueId = claimStorage.getConfig().getWorldUniqueId();
        if (!worldProperties.getUniqueId().equals(worldUniqueId)) {
            GriefPreventionPlugin.addLogEntry("Found mismatch world UUID in " + type.name().toLowerCase() + " claim file " + claimFile + ". Expected " + worldProperties.getUniqueId() + ", found " + worldUniqueId + ". Updating file with correct UUID...", CustomLogEntryTypes.Exception);
            claimStorage.getConfig().setWorldUniqueId(worldProperties.getUniqueId());
            claimStorage.getConfig().setRequiresSave(true);
            claimStorage.save();
        }

        // boundaries
        Vector3i lesserCorner = claimStorage.getConfig().getLesserBoundaryCornerPos();
        Vector3i greaterCorner = claimStorage.getConfig().getGreaterBoundaryCornerPos();
        if (lesserCorner == null || greaterCorner == null) {
            throw new Exception("Claim file '" + claimFile.getName() + "' has corrupted data and cannot be loaded. Skipping...");
        }

        if (!claimStorage.getConfig().isCuboid()) {
            final int extendIntoGround = activeConfig.getConfig().claim.extendIntoGroundDistance;
            if (extendIntoGround == 255 || ((lesserCorner.getY() - extendIntoGround) <= 0)) {
                lesserCorner = new Vector3i(lesserCorner.getX(), 0, lesserCorner.getZ());
                greaterCorner = new Vector3i(greaterCorner.getX(), world.getDimension().getBuildHeight() - 1, greaterCorner.getZ());
            }
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
        claim = new GPClaim(lesserBoundaryCorner, greaterBoundaryCorner, claimId, claimStorage.getConfig().getType(), ownerID);
        claim.world = lesserBoundaryCorner.getExtent();
        claim.cuboid = claimStorage.getConfig().isCuboid();
        claim.setClaimStorage(claimStorage);
        claim.setClaimData(claimStorage.getConfig());
        claim.context = new Context("gp_claim", claim.id.toString());
        final GPClaimManager claimManager = this.getClaimWorldManager(worldProperties);

        // add parent claim first
        if (parent != null) {
            GPClaim parentClaim = null;
            try {
                parentClaim = (GPClaim) claimManager.getClaimByUUID(parent).orElse(null);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            claim.parent = parentClaim;
        }

        claimManager.addClaim(claim, false);
        if (!claim.isWilderness()) {
            claimStorage.migrateSubdivision(claim);
        }
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
    GPPlayerData getPlayerDataFromStorage(UUID playerID) {
        return null;
    }

    @Override
    void overrideSavePlayerData(UUID playerID, GPPlayerData playerData) {
    }

}
