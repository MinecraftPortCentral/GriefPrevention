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
package me.ryanhamshire.griefprevention.configuration;

import com.flowpowered.math.vector.Vector3i;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.claim.GPClaimManager;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.util.Functional;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.common.SpongeImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

public class ClaimStorageData {

    protected HoconConfigurationLoader loader;
    private CommentedConfigurationNode root = SimpleCommentedConfigurationNode.root(ConfigurationOptions.defaults()
            .setHeader(GriefPreventionPlugin.CONFIG_HEADER));
    protected ObjectMapper<ClaimDataConfig>.BoundInstance configMapper;
    protected ClaimDataConfig configBase;
    public Path filePath;
    public Path folderPath;

    // MAIN
    public static final String MAIN_WORLD_UUID = "world-uuid";
    public static final String MAIN_OWNER_UUID = "owner-uuid";
    public static final String MAIN_CLAIM_NAME = "claim-name";
    public static final String MAIN_CLAIM_GREETING = "claim-greeting";
    public static final String MAIN_CLAIM_FAREWELL = "claim-farewell";
    public static final String MAIN_CLAIM_SPAWN = "claim-spawn";
    public static final String MAIN_CLAIM_TYPE = "claim-type";
    public static final String MAIN_CLAIM_CUBOID = "cuboid";
    public static final String MAIN_CLAIM_RESIZABLE = "resizable";
    public static final String MAIN_CLAIM_PVP = "pvp";
    public static final String MAIN_CLAIM_DATE_CREATED = "date-created";
    public static final String MAIN_CLAIM_DATE_LAST_ACTIVE = "date-last-active";
    public static final String MAIN_CLAIM_MAX_WIDTH = "max-width";
    public static final String MAIN_CLAIM_FOR_SALE = "for-sale";
    public static final String MAIN_CLAIM_SALE_PRICE = "sale-price";
    public static final String MAIN_REQUIRES_CLAIM_BLOCKS = "requires-claim-blocks";
    public static final String MAIN_SUBDIVISION_UUID = "uuid";
    public static final String MAIN_PARENT_CLAIM_UUID = "parent-claim-uuid";
    public static final String MAIN_LESSER_BOUNDARY_CORNER = "lesser-boundary-corner";
    public static final String MAIN_GREATER_BOUNDARY_CORNER = "greater-boundary-corner";
    public static final String MAIN_ACCESSORS = "accessors";
    public static final String MAIN_BUILDERS = "builders";
    public static final String MAIN_CONTAINERS = "containers";
    public static final String MAIN_MANAGERS = "managers";
    public static final String MAIN_ACCESSOR_GROUPS = "accessor-groups";
    public static final String MAIN_BUILDER_GROUPS = "builder-groups";
    public static final String MAIN_CONTAINER_GROUPS = "container-groups";
    public static final String MAIN_MANAGER_GROUPS = "manager-groups";
    public static final String MAIN_ALLOW_DENY_MESSAGES = "deny-messages";
    public static final String MAIN_ALLOW_FLAG_OVERRIDES = "flag-overrides";
    public static final String MAIN_ALLOW_CLAIM_EXPIRATION = "claim-expiration";
    public static final String MAIN_TAX_PAST_DUE_DATE = "tax-past-due-date";
    public static final String MAIN_TAX_BALANCE = "tax-balance";
    // SUB
    public static final String MAIN_INHERIT_PARENT = "inherit-parent";

    // Used for new claims after server startup
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ClaimStorageData(Path path, UUID worldUniqueId, UUID ownerUniqueId, ClaimType type, boolean cuboid) {
        this.filePath = path;
        this.folderPath = path.getParent();
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.createFile(path);
            }

            this.loader = HoconConfigurationLoader.builder().setPath(path).build();
            if (type == ClaimType.TOWN) {
                this.configMapper = (ObjectMapper.BoundInstance) ObjectMapper.forClass(TownDataConfig.class).bindToNew();
            } else {
                this.configMapper = (ObjectMapper.BoundInstance) ObjectMapper.forClass(ClaimDataConfig.class).bindToNew();
            }
            this.configMapper.getInstance().setWorldUniqueId(worldUniqueId);
            this.configMapper.getInstance().setOwnerUniqueId(ownerUniqueId);
            this.configMapper.getInstance().setType(type);
            this.configMapper.getInstance().setCuboid(cuboid);
            this.configMapper.getInstance().setClaimStorageData(this);
            reload();
            ((EconomyDataConfig) this.configMapper.getInstance().getEconomyData()).activeConfig = GriefPreventionPlugin.getActiveConfig(Sponge.getServer().getWorld(worldUniqueId).get().getProperties());
        } catch (Exception e) {
            SpongeImpl.getLogger().error("Failed to initialize configuration", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public ClaimStorageData(Path path, UUID worldUniqueId, ClaimDataConfig claimData) {
        this.filePath = path;
        this.folderPath = path.getParent();
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.createFile(path);
            }

            this.loader = HoconConfigurationLoader.builder().setPath(path).build();
            this.configMapper = (ObjectMapper.BoundInstance) ObjectMapper.forClass(ClaimDataConfig.class).bind(claimData);
            this.configMapper.getInstance().setClaimStorageData(this);
            reload();
            ((EconomyDataConfig) this.configMapper.getInstance().getEconomyData()).activeConfig = GriefPreventionPlugin.getActiveConfig(worldUniqueId);
        } catch (Exception e) {
            SpongeImpl.getLogger().error("Failed to initialize configuration", e);
        }
    }

    // Used during server load
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ClaimStorageData(Path path, UUID worldUniqueId) {
        this.filePath = path;
        this.folderPath = path.getParent();
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.createFile(path);
            }

            this.loader = HoconConfigurationLoader.builder().setPath(path).build();
            if (path.getParent().endsWith("town")) {
                this.configMapper = (ObjectMapper.BoundInstance) ObjectMapper.forClass(TownDataConfig.class).bindToNew();
            } else {
                this.configMapper = (ObjectMapper.BoundInstance) ObjectMapper.forClass(ClaimDataConfig.class).bindToNew();
            }
            this.configMapper.getInstance().setClaimStorageData(this);
            reload();
            ((EconomyDataConfig) this.configMapper.getInstance().getEconomyData()).activeConfig = GriefPreventionPlugin.getActiveConfig(worldUniqueId);
        } catch (Exception e) {
            SpongeImpl.getLogger().error("Failed to initialize configuration", e);
        }
    }

    public ClaimDataConfig getConfig() {
        return this.configBase;
    }

    public void save() {
        try {
            // subdivisions are stored in their own claim files
            this.root.getNode(GriefPreventionPlugin.MOD_ID).removeChild("subdivisions");
            this.configMapper.serialize(this.root.getNode(GriefPreventionPlugin.MOD_ID));
            this.loader.save(this.root);
            this.configBase.setRequiresSave(false);
        } catch (IOException | ObjectMappingException e) {
            SpongeImpl.getLogger().error("Failed to save configuration", e);
        }
    }

    public void reload() {
        try {
            this.root = this.loader.load(ConfigurationOptions.defaults().setHeader(GriefPreventionPlugin.CONFIG_HEADER));
            this.configBase = this.configMapper.populate(this.root.getNode(GriefPreventionPlugin.MOD_ID));
        } catch (Exception e) {
            SpongeImpl.getLogger().error("Failed to load configuration", e);
        }
    }

    public void migrateSubdivision(GPClaim parent) throws Exception {
        try {
            for (Map.Entry<Object, ? extends CommentedConfigurationNode> mapEntry : this.root.getNode(GriefPreventionPlugin.MOD_ID).getChildrenMap().entrySet()) {
                CommentedConfigurationNode node = (CommentedConfigurationNode) mapEntry.getValue();
                String key = "";
                if (node.getKey() instanceof String) {
                    key = (String) node.getKey();
                }
                if (key.equalsIgnoreCase("subdivisions") && node.getValue() != null) {
                    // move subdivision data to children node
                    final Path path = this.filePath.getParent().resolve("subdivision");
                    Map<String, LinkedHashMap<String, Object>> subMap = (Map<String, LinkedHashMap<String, Object>>) node.getValue();
                    if (subMap.isEmpty()) {
                        continue;
                    }

                    for (Map.Entry<String, LinkedHashMap<String, Object>> subEntry : subMap.entrySet()) {
                        ClaimDataConfig claimData = new ClaimDataConfig(subEntry.getValue());
                        final UUID claimId = UUID.fromString(subEntry.getKey());
                        try {
                            Files.createDirectories(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        final File claimFile = new File(path + File.separator + claimId);
                        final ClaimStorageData claimStorage = new ClaimStorageData(claimFile.toPath(), parent.getWorldUniqueId(), claimData);
        
                        // boundaries
                        Vector3i lesserCorner = claimStorage.getConfig().getLesserBoundaryCornerPos();
                        Vector3i greaterCorner = claimStorage.getConfig().getGreaterBoundaryCornerPos();
                        if (lesserCorner == null || greaterCorner == null) {
                            throw new Exception("Claim file '" + claimFile.getName() + "' has corrupted data and cannot be loaded. Skipping...");
                        }
                        Vector3i lesserBoundaryCornerPos = lesserCorner;
                        Vector3i greaterBoundaryCornerPos = greaterCorner;
                        Location<World> lesserBoundaryCorner = new Location<World>(parent.getWorld(), lesserBoundaryCornerPos);
                        Location<World> greaterBoundaryCorner = new Location<World>(parent.getWorld(), greaterBoundaryCornerPos);
        
                        // owner
                        UUID ownerID = parent.getOwnerUniqueId();
                        if (ownerID == null) {
                            GriefPreventionPlugin.addLogEntry("Error - this is not a valid UUID: " + ownerID + ".");
                            continue;
                        }
        
                        // instantiate
                        GPClaim subdivision = new GPClaim(lesserBoundaryCorner, greaterBoundaryCorner, claimId, ClaimType.SUBDIVISION, ownerID, claimData.isCuboid());
                        subdivision.parent = parent;
                        subdivision.setClaimStorage(claimStorage);
                        subdivision.setClaimData(claimData);
                        claimData.setParent(parent.id);
                        GPClaimManager claimManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(parent.getWorld().getProperties());
                        claimManager.addClaim(subdivision, true);
                    }
                    node.getParent().removeChild(key);
                    parent.getInternalClaimData().setRequiresSave(true);
                    parent.getClaimStorage().save();
                    return;
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public CompletableFuture<CommentedConfigurationNode> updateSetting(String key, Object value) {
        return Functional.asyncFailableFuture(() -> {
            CommentedConfigurationNode upd = getSetting(key);
            upd.setValue(value);
            this.configBase = this.configMapper.populate(this.root.getNode(GriefPreventionPlugin.MOD_ID));
            this.loader.save(this.root);
            return upd;
        }, ForkJoinPool.commonPool());
    }

    public CommentedConfigurationNode getRootNode() {
        return this.root.getNode(GriefPreventionPlugin.MOD_ID);
    }

    public CommentedConfigurationNode getSetting(String key) {
        if (!key.contains(".") || key.indexOf('.') == key.length() - 1) {
            return null;
        } else {
            String category = key.substring(0, key.indexOf('.'));
            String prop = key.substring(key.indexOf('.') + 1);
            return getRootNode().getNode(category).getNode(prop);
        }
    }
}
