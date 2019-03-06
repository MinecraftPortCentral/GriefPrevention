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

import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.configuration.type.ConfigBase;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.ValueType;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.util.ConfigurationNodeWalker;
import org.spongepowered.common.SpongeImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Objects;

public class GriefPreventionConfig<T extends ConfigBase> {

    private static final ConfigurationOptions LOADER_OPTIONS = ConfigurationOptions.defaults()
            .setHeader(GriefPreventionPlugin.CONFIG_HEADER);

    private final Path path;

    /**
     * The parent configuration - values are inherited from this
     */
    private final GriefPreventionConfig parent;

    /**
     * The loader (mapped to a file) used to read/write the config to disk
     */
    private HoconConfigurationLoader loader;

    /**
     * A node representation of "whats actually in the file".
     */
    private CommentedConfigurationNode fileData = SimpleCommentedConfigurationNode.root(LOADER_OPTIONS);

    /**
     * A node representation of {@link #fileData}, merged with the data of {@link #parent}.
     */
    private CommentedConfigurationNode data = SimpleCommentedConfigurationNode.root(LOADER_OPTIONS);

    /**
     * The mapper instance used to populate the config instance
     */
    private ObjectMapper<T>.BoundInstance configMapper;

    public GriefPreventionConfig(Class<T> clazz, Path path, GriefPreventionConfig parent) {
        this.parent = parent;
        this.path = path;

        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.createFile(path);
            }

            this.loader = HoconConfigurationLoader.builder().setPath(path).build();
            this.configMapper = ObjectMapper.forClass(clazz).bindToNew();

            reload();
            save();
        } catch (Exception e) {
            SpongeImpl.getLogger().error("Failed to initialize configuration", e);
        }
    }

    public T getConfig() {
        return this.configMapper.getInstance();
    }

    public boolean save() {
        try {
            // save from the mapped object --> node
            CommentedConfigurationNode saveNode = SimpleCommentedConfigurationNode.root(LOADER_OPTIONS);
            this.configMapper.serialize(saveNode.getNode(GriefPreventionPlugin.MOD_ID));

            // before saving this config, remove any values already declared with the same value on the parent
            if (this.parent != null) {
                removeDuplicates(saveNode);
            }

            // merge the values we need to write with the ones already declared in the file
            saveNode.mergeValuesFrom(this.fileData);

            // save the data to disk
            this.loader.save(saveNode);
            return true;
        } catch (IOException | ObjectMappingException e) {
            GriefPreventionPlugin.instance.getLogger().error("Failed to save configuration", e);
            return false;
        }
    }

    public void reload() {
        try {
            // load settings from file
            CommentedConfigurationNode loadedNode = this.loader.load();

            // attempt to strip duplicate settings from the file
            // (this only happens on the first pass of the file - see javadocs below)
            if (cleanupConfig(loadedNode)) {
                this.loader.save(loadedNode);
            }

            // store "what's in the file" separately in memory
            this.fileData = loadedNode;

            // make a copy of the file data
            this.data = this.fileData.copy();

            // merge with settings from parent
            if (this.parent != null) {
                this.parent.reload();
                this.data.mergeValuesFrom(this.parent.data);
            }

            // populate the config object
            populateInstance();
        } catch (Exception e) {
            GriefPreventionPlugin.instance.getLogger().error("Failed to load configuration", e);
        }
    }

    private void populateInstance() throws ObjectMappingException {
        this.configMapper.populate(this.data.getNode(GriefPreventionPlugin.MOD_ID));
    }

    /**
     * Performs a cleanup operation on the given configuration node, taking into
     * account the legacy 'config-enabled' setting.
     *
     * @param root The node to cleanup
     * @return If the cleanup was able to occur, depending on the state of the 'config-enabled' setting
     */
    private boolean cleanupConfig(CommentedConfigurationNode root) {
        // we can't strip values from the global config, there won't be any duplicates there
        if (this.parent == null) {
            return false;
        }

        ConfigurationNode configEnabled = root.getNode(GriefPreventionPlugin.MOD_ID, "config-enabled");

        // if the node is missing, don't strip anything from the file
        // (this ensures the migration only happens on the first pass)
        if (configEnabled.isVirtual()) {
            return false;
        }

        boolean enabled = configEnabled.getBoolean(true);

        // remove the enabled property so migration doesn't happen again
        configEnabled.setValue(null);

        if (!enabled) {
            // config wasn't enabled, just clear it
            // (we don't wish to take account for any of the settings defined here)
            root.getNode(GriefPreventionPlugin.MOD_ID).setValue(null);
        } else {
            // config was enabled, but we want to strip out duplicated values
            // but keep any overrides
            removeDuplicates(root);
        }

        return true;
    }

    /**
     * Traverses the given {@code root} config node, removing any values which
     * are also present and set to the same value on this configs "parent".
     *
     * @param root The node to process
     */
    private void removeDuplicates(CommentedConfigurationNode root) {
        if (this.parent == null) {
            throw new IllegalStateException("parent is null");
        }

        Iterator<ConfigurationNodeWalker.VisitedNode<CommentedConfigurationNode>> it = ConfigurationNodeWalker.DEPTH_FIRST_POST_ORDER.walkWithPath(root);
        while (it.hasNext()) {
            ConfigurationNodeWalker.VisitedNode<CommentedConfigurationNode> next = it.next();
            CommentedConfigurationNode node = next.getNode();

            // remove empty maps
            if (node.hasMapChildren()) {
                if (node.getChildrenMap().isEmpty()) {
                    node.setValue(null);
                }
                continue;
            }

            // ignore list values
            if (node.getParent() != null && node.getParent().getValueType() == ValueType.LIST) {
                continue;
            }

            // if the node already exists in the parent config, remove it
            CommentedConfigurationNode parentValue = this.parent.data.getNode(next.getPath().getArray());
            if (Objects.equals(node.getValue(), parentValue.getValue())) {
                node.setValue(null);
            }
        }
    }

    public CommentedConfigurationNode getRootNode() {
        return this.data.getNode(GriefPreventionPlugin.MOD_ID);
    }

    public Path getPath() {
        return this.path;
    }
}