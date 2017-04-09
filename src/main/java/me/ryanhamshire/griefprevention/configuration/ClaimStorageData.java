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

import com.google.common.reflect.TypeToken;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import org.spongepowered.api.util.Functional;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.util.IpSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

public class ClaimStorageData {

    private HoconConfigurationLoader loader;
    private CommentedConfigurationNode root = SimpleCommentedConfigurationNode.root(ConfigurationOptions.defaults()
            .setHeader(GriefPreventionPlugin.CONFIG_HEADER));
    private ObjectMapper<ClaimDataConfig>.BoundInstance configMapper;
    private ClaimDataConfig configBase;
    public Path filePath;

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
    public static final String MAIN_REQUIRES_CLAIM_BLOCKS = "requires-claim-blocks";
    public static final String MAIN_SUBDIVISION_UUID = "uuid";
    public static final String MAIN_PARENT_CLAIM_UUID = "parent-claim-uuid";
    public static final String MAIN_LESSER_BOUNDARY_CORNER = "lesser-boundary-corner";
    public static final String MAIN_GREATER_BOUNDARY_CORNER = "greater-boundary-corner";
    public static final String MAIN_ACCESSORS = "accessors";
    public static final String MAIN_BUILDERS = "builders";
    public static final String MAIN_CONTAINERS = "managers";
    public static final String MAIN_MANAGERS = "coowners";
    public static final String MAIN_SUBDIVISIONS = "sub-divisions";
    public static final String MAIN_ALLOW_DENY_MESSAGES = "deny-messages";
    public static final String MAIN_ALLOW_FLAG_OVERRIDES = "flag-overrides";
    public static final String MAIN_ALLOW_CLAIM_EXPIRATION = "claim-expiration";
    // SUB
    public static final String SUB_INHERIT_PARENT = "inherit-parent";

    // Used for new claims after server startup
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ClaimStorageData(Path path, UUID claimUniqueId, UUID ownerUniqueId, ClaimType type, boolean cuboid) {
        this.filePath = path;
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.createFile(path);
            }

            this.loader = HoconConfigurationLoader.builder().setPath(path).build();
            this.configMapper = (ObjectMapper.BoundInstance) ObjectMapper.forClass(ClaimDataConfig.class).bindToNew();
            this.configMapper.getInstance().setWorldUniqueId(claimUniqueId);
            this.configMapper.getInstance().setOwnerUniqueId(ownerUniqueId);
            this.configMapper.getInstance().setType(type);
            this.configMapper.getInstance().setCuboid(cuboid);
            this.configMapper.getInstance().setClaimStorageData(this);
            reload();
        } catch (Exception e) {
            SpongeImpl.getLogger().error("Failed to initialize configuration", e);
        }
    }

    // Used during server load
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ClaimStorageData(Path path) {
        this.filePath = path;
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.createFile(path);
            }

            this.loader = HoconConfigurationLoader.builder().setPath(path).build();
            this.configMapper = (ObjectMapper.BoundInstance) ObjectMapper.forClass(ClaimDataConfig.class).bindToNew();
            this.configMapper.getInstance().setClaimStorageData(this);
            reload();
        } catch (Exception e) {
            SpongeImpl.getLogger().error("Failed to initialize configuration", e);
        }
    }

    public ClaimDataConfig getConfig() {
        return this.configBase;
    }

    public void save() {
        try {
            this.configMapper.serialize(this.root.getNode(GriefPreventionPlugin.MOD_ID));
            this.loader.save(this.root);
            this.configBase.setRequiresSave(false);
        } catch (IOException | ObjectMappingException e) {
            SpongeImpl.getLogger().error("Failed to save configuration", e);
        }
    }

    public void reload() {
        try {
            this.root = this.loader.load(ConfigurationOptions.defaults()
                    .setSerializers(
                            TypeSerializers.getDefaultSerializers().newChild().registerType(TypeToken.of(IpSet.class), new IpSet.IpSetSerializer()))
                    .setHeader(GriefPreventionPlugin.CONFIG_HEADER));
            // Remove empty strings as they are no longer serializable in 1.9+
            boolean requiresSave = false;
            for (Map.Entry<Object, ? extends CommentedConfigurationNode> mapEntry : this.root.getNode(GriefPreventionPlugin.MOD_ID).getChildrenMap().entrySet()) {
                CommentedConfigurationNode node = (CommentedConfigurationNode) mapEntry.getValue();
                if (node.getValue() instanceof String) {
                    String value = (String) node.getValue();
                    if (value.isEmpty()) {
                        this.root.getNode(GriefPreventionPlugin.MOD_ID).removeChild(mapEntry.getKey());
                        requiresSave = true;
                    }
                }
            }
            this.configBase = this.configMapper.populate(this.root.getNode(GriefPreventionPlugin.MOD_ID));
            if (requiresSave) {
                this.save();
            }
        } catch (Exception e) {
            SpongeImpl.getLogger().error("Failed to load configuration", e);
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
