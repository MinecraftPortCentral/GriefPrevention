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

import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.util.Tristate;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class GPPermissionMigrator {

    private static GPPermissionMigrator instance;

    public Consumer<Subject> migrateSubjectPermissions() {
        return subject -> {
            migrateSubject(subject);
        };
    }

    public void migrateSubject(Subject subject) {
        GriefPreventionPlugin.instance.executor.execute(() -> {
            boolean migrated = false;
            for (Map.Entry<Set<Context>, Map<String, Boolean>> mapEntry : subject.getSubjectData().getAllPermissions().entrySet()) {
                final Set<Context> contextSet = mapEntry.getKey();
                Iterator<Map.Entry<String, Boolean>> iterator = mapEntry.getValue().entrySet().iterator();
                while (iterator.hasNext()) {
                    final Map.Entry<String, Boolean> entry = iterator.next();
                    final String currentPermission = entry.getKey();
                    if (currentPermission.contains(".pixelmon.animal.pixelmon")) {
                        GriefPreventionPlugin.instance.getLogger().info("Detected legacy pixelmon permission '" + currentPermission + "'. Migrating...");
                        final String newPermission = currentPermission.replaceAll("\\.pixelmon\\.animal\\.pixelmon", "\\.pixelmon\\.animal");
                        subject.getSubjectData().setPermission(contextSet, currentPermission, Tristate.UNDEFINED);
                        GriefPreventionPlugin.instance.getLogger().info("Removed legacy pixelmon permission '" + currentPermission + "'.");
                        subject.getSubjectData().setPermission(contextSet, newPermission, Tristate.fromBoolean(entry.getValue()));
                        GriefPreventionPlugin.instance.getLogger().info("Set new permission '" + newPermission);
                        migrated = true;
                    }
                }
            }
            if (migrated) {
                GriefPreventionPlugin.instance.getLogger().info("Finished migration of subject '" + subject.getIdentifier() + "'\n");
            }
        });
    }

    public static GPPermissionMigrator getInstance() {
        return instance;
    }

    static {
        instance = new GPPermissionMigrator();
    }
}
