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
import me.ryanhamshire.griefprevention.claim.Claim;
import me.ryanhamshire.griefprevention.claim.ClaimWorldManager;
import me.ryanhamshire.griefprevention.configuration.ClaimStorageData;
import me.ryanhamshire.griefprevention.configuration.ClaimTemplateStorage;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig.Type;
import me.ryanhamshire.griefprevention.configuration.SubDivisionDataConfig;
import me.ryanhamshire.griefprevention.configuration.types.DimensionConfig;
import me.ryanhamshire.griefprevention.configuration.types.WorldConfig;
import me.ryanhamshire.griefprevention.task.CleanupUnusedClaimsTask;
import me.ryanhamshire.griefprevention.util.BlockUtils;
import me.ryanhamshire.griefprevention.util.RedProtectMigrator;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.util.Tristate;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

//manages data stored in the file system
public class FlatFileDataStore extends DataStore {

    private final static Path schemaVersionFilePath = dataLayerFolderPath.resolve("_schemaVersion");
    private final static Path worldsConfigFolderPath = dataLayerFolderPath.resolve("worlds");
    public final static Path claimDataPath = Paths.get("GriefPreventionData", "ClaimData");
    public final static Path claimTemplatePath = claimDataPath.resolve("Templates");
    public final static Path worldClaimDataPath = Paths.get("GriefPreventionData", "WorldClaim");
    public final static Path playerDataPath = Paths.get("GriefPreventionData", "PlayerData");
    public final static Path redProtectDataPath = Paths.get("config", "RedProtect", "data");
    public final static Map<UUID, Task> cleanupClaimTasks = Maps.newHashMap();
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
    public void loadWorldData(World world) {
        WorldProperties worldProperties = world.getProperties();
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
            // run cleanup task
            int cleanupTaskInterval = GriefPrevention.getActiveConfig(worldProperties).getConfig().claim.cleanupTaskInterval;
            if (cleanupTaskInterval > 0) {
                CleanupUnusedClaimsTask cleanupTask = new CleanupUnusedClaimsTask(worldProperties);
                cleanupClaimTasks.put(worldProperties.getUniqueId(), Sponge.getGame().getScheduler().createTaskBuilder().delay(cleanupTaskInterval, TimeUnit.MINUTES).execute(cleanupTask).submit(GriefPrevention.instance));
            }
        }

        // check if world has existing data
        Path claimData = Paths.get(worldProperties.getWorldName()).resolve(claimDataPath);
        Path worldPlayerDataPath = Paths.get(worldProperties.getWorldName()).resolve(playerDataPath);
        if (worldProperties.getUniqueId() == Sponge.getGame().getServer().getDefaultWorld().get().getUniqueId()) {
            claimData = claimDataPath;
            worldPlayerDataPath = playerDataPath;
        }

        try {
            if (!Files.exists(rootWorldSavePath.resolve(claimData))) {
                Files.createDirectories(rootWorldSavePath.resolve(claimData));
            }

            // check for RedProtectData
            if (GriefPrevention.getGlobalConfig().getConfig().migrator.redProtectMigrator) {
                Path redProtectFilePath = redProtectDataPath.resolve("data_" + worldProperties.getWorldName() + ".conf");
                Path gpMigratedPath = redProtectDataPath.resolve("gp_migrated_" + worldProperties.getWorldName());
                if (Files.exists(redProtectFilePath) && !Files.exists(gpMigratedPath)) {
                    RedProtectMigrator.migrate(world, redProtectFilePath, rootWorldSavePath.resolve(claimData));
                    Files.createFile(gpMigratedPath);
                }
            }

                File[] files = rootWorldSavePath.resolve(claimData).toFile().listFiles();
            if (files.length > 0) {
                this.loadClaimData(files, worldProperties);
                GriefPrevention.addLogEntry("[" + worldProperties.getWorldName() + "]" + files.length + " total claims loaded.");
            }
    
            if (Files.exists(rootWorldSavePath.resolve(worldPlayerDataPath))) {
                files = rootWorldSavePath.resolve(worldPlayerDataPath).toFile().listFiles();
                this.loadPlayerData(worldProperties, files);
            }
            if (!Files.exists(rootWorldSavePath.resolve(worldPlayerDataPath))) {
                Files.createDirectories(rootWorldSavePath.resolve(worldPlayerDataPath));
            }

            ClaimWorldManager claimWorldManager = this.claimWorldManagers.get(worldProperties.getUniqueId());
            if (claimWorldManager != null && claimWorldManager.getWildernessClaim() == null) {
                claimWorldManager.createWildernessClaim(worldProperties);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // handle default flag permissions
        Set<Context> contexts = new HashSet<>();
        contexts.add(GriefPrevention.ADMIN_CLAIM_FLAG_DEFAULT_CONTEXT);
        contexts.add(world.getContext());
        validateFlagDefaults(contexts, GPFlags.DEFAULT_FLAGS);
        contexts = new HashSet<>();
        contexts.add(GriefPrevention.BASIC_CLAIM_FLAG_DEFAULT_CONTEXT);
        contexts.add(world.getContext());
        validateFlagDefaults(contexts, GPFlags.DEFAULT_FLAGS);
        contexts = new HashSet<>();
        contexts.add(GriefPrevention.WILDERNESS_CLAIM_FLAG_DEFAULT_CONTEXT);
        contexts.add(world.getContext());
        validateFlagDefaults(contexts, GPFlags.DEFAULT_WILDERNESS_FLAGS);
    }

    public void validateFlagDefaults(Set<Context> contexts, Map<String, Tristate> defaultFlags) {
        Map<String, Boolean> defaultPermissions = GriefPrevention.GLOBAL_SUBJECT.getTransientSubjectData().getPermissions(contexts);
        if (defaultPermissions.isEmpty()) {
            for (Map.Entry<String, Tristate> mapEntry : defaultFlags.entrySet()) {
                GriefPrevention.GLOBAL_SUBJECT.getTransientSubjectData().setPermission(contexts, GPPermissions.FLAG_BASE + "." + mapEntry.getKey(), mapEntry.getValue());
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            }
        } else {
            // remove invalid flag entries
            for (String flagPermission : defaultPermissions.keySet()) {
                String flag = flagPermission.replace(GPPermissions.FLAG_BASE + ".", "");
                if (!defaultFlags.containsKey(flag)) {
                    GriefPrevention.GLOBAL_SUBJECT.getTransientSubjectData().setPermission(contexts, flagPermission, Tristate.UNDEFINED);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                    }
                }
            }

            // make sure all defaults are available
            for (Map.Entry<String, Tristate> mapEntry : defaultFlags.entrySet()) {
                String flagPermission = GPPermissions.FLAG_BASE + "." + mapEntry.getKey();
                if (!defaultPermissions.keySet().contains(flagPermission)) {
                    GriefPrevention.GLOBAL_SUBJECT.getTransientSubjectData().setPermission(contexts, flagPermission, mapEntry.getValue());
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }

    public void unloadWorldData(WorldProperties worldProperties) {
        ClaimWorldManager claimWorldManager = this.claimWorldManagers.get(worldProperties.getUniqueId());
        for (Claim claim : claimWorldManager.getWorldClaims()) {
            claim.unload();
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
            claimStorage.getConfig().setRequiresSave(true);
        }

        World world = Sponge.getServer().loadWorld(worldProperties).orElse(null);

        if (world == null) {
            throw new Exception("World [Name: " + worldProperties.getWorldName() + "][UUID: " + worldProperties.getUniqueId().toString() + "] is not loaded.");
        }

        // boundaries
        String lesserCorner = claimStorage.getConfig().getLesserBoundaryCorner();
        String greaterCorner = claimStorage.getConfig().getGreaterBoundaryCorner();
        if (lesserCorner == null || greaterCorner == null) {
            throw new Exception("Claim file '" + claimFile.getName() + "' has corrupted data and cannot be loaded. Skipping...");
        }
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
        claim = new Claim(lesserBoundaryCorner, greaterBoundaryCorner, claimId, claimStorage.getConfig().getClaimType());
        claim.ownerID = ownerID;
        claim.world = lesserBoundaryCorner.getExtent();
        claim.type = claimStorage.getConfig().getClaimType();
        claim.cuboid = claimStorage.getConfig().isCuboid();
        claim.setClaimStorage(claimStorage);
        claim.setClaimData(claimStorage.getConfig());
        claim.context = new Context("gp_claim", claim.id.toString());

        // add parent claim first
        this.addClaim(claim, false);
        if (!claim.isWildernessClaim()) {
            // check for subdivisions
            for(Map.Entry<UUID, SubDivisionDataConfig> mapEntry : claimStorage.getConfig().getSubdivisions().entrySet()) {
                SubDivisionDataConfig subDivisionData = mapEntry.getValue();
                subDivisionData.setParentData(claim.getClaimData());
                Vector3i subLesserBoundaryCornerPos = BlockUtils.positionFromString(subDivisionData.getLesserBoundaryCorner());
                Vector3i subGreaterBoundaryCornerPos = BlockUtils.positionFromString(subDivisionData.getGreaterBoundaryCorner());
                Location<World> subLesserBoundaryCorner = new Location<World>(world, subLesserBoundaryCornerPos);
                Location<World> subGreaterBoundaryCorner = new Location<World>(world, subGreaterBoundaryCornerPos);
    
                Claim subDivision = new Claim(subLesserBoundaryCorner, subGreaterBoundaryCorner, mapEntry.getKey(), Claim.Type.SUBDIVISION);
                subDivision.id = mapEntry.getKey();
                subDivision.world = subLesserBoundaryCorner.getExtent();
                subDivision.setClaimStorage(claimStorage);
                subDivision.context = new Context("claim", subDivision.id.toString());
                subDivision.parent = claim;
                subDivision.type = Claim.Type.SUBDIVISION;
                subDivision.cuboid = subDivisionData.isCuboid();
                subDivision.setClaimData(subDivisionData);
                // add subdivision to parent
                claim.children.add(subDivision);
            }
        }
        return claim;
    }

    @Override
    public void writeClaimToStorage(Claim claim) {
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

            if (claim.id == null) {
                claim.id = UUID.randomUUID();
            }

            ClaimStorageData claimStorage = claim.getClaimStorage();
            if (claimStorage == null) {
                if (claim.isSubdivision()) {
                    claim.setClaimStorage(claim.parent.getClaimStorage());
                    claim.setClaimData(new SubDivisionDataConfig());
                } else {
                    claimStorage = new ClaimStorageData(claim, claimFile.toPath());
                    claim.setClaimStorage(claimStorage);
                    claim.setClaimData(claimStorage.getConfig());
                }
            }

            claim.updateClaimStorageData();
            claimStorage.save();
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
    PlayerData getPlayerDataFromStorage(UUID playerID) {
        return null;
    }

    @Override
    void overrideSavePlayerData(UUID playerID, PlayerData playerData) {
    }

}
