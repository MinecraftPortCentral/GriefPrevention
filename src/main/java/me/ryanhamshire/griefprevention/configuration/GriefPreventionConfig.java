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
import me.ryanhamshire.griefprevention.configuration.type.ConfigBase;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.ValueType;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMapper;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;
import ninja.leaping.configurate.util.ConfigurationNodeWalker;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.util.IpSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

public class GriefPreventionConfig<T extends ConfigBase> {

    private static final ConfigurationOptions LOADER_OPTIONS = ConfigurationOptions.defaults()
            .setHeader(GriefPreventionPlugin.CONFIG_HEADER)
            .setSerializers(TypeSerializers.getDefaultSerializers().newChild()
                    .registerType(TypeToken.of(IpSet.class), new IpSet.IpSetSerializer())
            );

    private CommentedConfigurationNode data = SimpleCommentedConfigurationNode.root(LOADER_OPTIONS);

    private final GriefPreventionConfig parent;
    private final Path path;

    private HoconConfigurationLoader loader;
    private ObjectMapper<T>.BoundInstance configMapper;
    private T configObject;

    public GriefPreventionConfig(Class<T> clazz, Path path, GriefPreventionConfig parent) {
        this.parent = parent;
        this.path = path;

        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.createFile(path);
            }

            this.loader = HoconConfigurationLoader.builder().setPath(path).setDefaultOptions(LOADER_OPTIONS).build();
            this.configMapper = ObjectMapper.forClass(clazz).bindToNew();

            reload();
            save();
        } catch (Exception e) {
            SpongeImpl.getLogger().error("Failed to initialize configuration", e);
        }
    }

    public T getConfig() {
        return this.configObject;
    }

    public void save() {
        try {
            // save from the mapped object --> node
            this.configMapper.serialize(this.data.getNode(GriefPreventionPlugin.MOD_ID));

            CommentedConfigurationNode saveNode = this.data.copy();

            // before saving this config, remove any values already declared with the same value on the parent
            if (this.parent != null) {
                Iterator<ConfigurationNodeWalker.VisitedNode<CommentedConfigurationNode>> it = ConfigurationNodeWalker.DEPTH_FIRST_POST_ORDER.walkWithPath(saveNode);
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
                    if (node.equals(parentValue)) {
                        node.setValue(null);
                    }
                }
            }

            this.loader.save(saveNode);
        } catch (IOException | ObjectMappingException e) {
            SpongeImpl.getLogger().error("Failed to save configuration", e);
        }
    }

    public void reload() {
        try {
            // load data from the file
            this.data = this.loader.load();

            // merge with parent
            if (this.parent != null) {
                this.data.mergeValuesFrom(this.parent.data);
            }

            // populate the config object
            this.configObject = this.configMapper.populate(this.data.getNode(GriefPreventionPlugin.MOD_ID));
        } catch (Exception e) {
            SpongeImpl.getLogger().error("Failed to load configuration", e);
        }
    }

    public CommentedConfigurationNode getRootNode() {
        return this.data.getNode(GriefPreventionPlugin.MOD_ID);
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

    public Path getPath() {
        return this.path;
    }
}
