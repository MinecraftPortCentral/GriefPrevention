/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
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
package me.ryanhamshire.griefprevention.migrator;

import com.google.common.reflect.TypeToken;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.configuration.ClaimDataConfig;
import me.ryanhamshire.griefprevention.configuration.ClaimStorageData;
import me.ryanhamshire.griefprevention.util.BlockUtils;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RedProtectMigrator {

    public static void migrate(World world, Path redProtectFilePath, Path gpClaimDataPath) throws FileNotFoundException, ClassNotFoundException {
        if (!GriefPreventionPlugin.getGlobalConfig().getConfig().migrator.redProtectMigrator) {
            return;
        }

        int count = 0;
        try {
            GriefPreventionPlugin.instance.getLogger().info("Starting RedProtect region data migration for world " + world.getProperties().getWorldName() + "...");
            ConfigurationLoader<CommentedConfigurationNode> regionManager = HoconConfigurationLoader.builder().setPath(redProtectFilePath).build();
            CommentedConfigurationNode region = regionManager.load();
            GriefPreventionPlugin.instance.getLogger().info("Scanning RedProtect regions in world data file '" + redProtectFilePath + "'...");
            for (Object key:region.getChildrenMap().keySet()){
                String rname = key.toString();
                if (!region.getNode(rname).hasMapChildren()){
                    continue;
                }
                int maxX = region.getNode(rname,"maxX").getInt();
                int maxY = region.getNode(rname,"maxY").getInt(255);
                int maxZ = region.getNode(rname,"maxZ").getInt();
                int minX = region.getNode(rname,"minX").getInt();
                int minY = region.getNode(rname,"minY").getInt(0);
                int minZ = region.getNode(rname,"minZ").getInt();
                List<String> owners = new ArrayList<String>();
                owners.addAll(region.getNode(rname,"owners").getList(TypeToken.of(String.class)));
                
                List<String> members = new ArrayList<String>();
                members.addAll(region.getNode(rname,"members").getList(TypeToken.of(String.class)));
                
                String creator = region.getNode(rname,"creator").getString();             
                String welcome = region.getNode(rname,"welcome").getString();                 

                // create GP claim data file
                GriefPreventionPlugin.instance.getLogger().info("Migrating RedProtect region data '" + rname + "'...");
                UUID ownerUniqueId = null;
                try {
                    ownerUniqueId = UUID.fromString(creator);
                } catch (IllegalArgumentException e) {
                    GriefPreventionPlugin.instance.getLogger().error("Could not migrate RedProtect region data '" + rname + 
                            "', creator UUID '" + creator + "' is invalid. Skipping...");
                    continue;
                }

                UUID claimUniqueId = UUID.randomUUID();
                Location<World> lesserBoundaryCorner = new Location<>(world, minX, minY, minZ);
                Location<World> greaterBoundaryCorner = new Location<>(world, maxX, maxY, maxZ);
                Path claimFilePath = gpClaimDataPath.resolve(claimUniqueId.toString());
                if (!Files.exists(claimFilePath)) {
                    Files.createFile(claimFilePath);
                }

                ClaimStorageData claimStorage = new ClaimStorageData(claimFilePath);
                ClaimDataConfig claimDataConfig = claimStorage.getConfig();
                claimDataConfig.setName(Text.of(rname));
                claimDataConfig.setWorldUniqueId(world.getUniqueId());
                claimDataConfig.setOwnerUniqueId(ownerUniqueId);
                claimDataConfig.setLesserBoundaryCorner(BlockUtils.positionToString(lesserBoundaryCorner));
                claimDataConfig.setGreaterBoundaryCorner(BlockUtils.positionToString(greaterBoundaryCorner));
                claimDataConfig.setDateLastActive(Instant.now());
                claimDataConfig.setType(ClaimType.BASIC);
                if (!welcome.equals("")) {
                    claimDataConfig.setGreeting(Text.of(welcome));
                }
                List<String> rpUsers = new ArrayList<>(owners);
                rpUsers.addAll(members);
                List<UUID> builders = claimDataConfig.getBuilders();
                for (String builder : rpUsers) {
                    UUID builderUniqueId = null;
                    try {
                        builderUniqueId = UUID.fromString(builder);
                    } catch (IllegalArgumentException e) {
                        // ignore
                        continue;
                    }
                    if (!builders.contains(builderUniqueId) && !builderUniqueId.equals(ownerUniqueId)) {
                        builders.add(builderUniqueId);
                    }
                }

                claimDataConfig.setRequiresSave(true);
                claimStorage.save();
                GriefPreventionPlugin.instance.getLogger().info("Successfully migrated RedProtect region data '" + rname + "' to '" + claimFilePath + "'");
                count++;
            }
            GriefPreventionPlugin.instance.getLogger().info("Finished RedProtect region data migration for world '" + world.getProperties().getWorldName() + "'."
                    + " Migrated a total of " + count + " regions.");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ObjectMappingException e) {
            e.printStackTrace();
        } 
    }
}
