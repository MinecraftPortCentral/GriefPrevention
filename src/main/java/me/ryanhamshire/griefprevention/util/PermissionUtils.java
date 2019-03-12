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
package me.ryanhamshire.griefprevention.util;

import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class PermissionUtils {

    public static boolean hasGroupSubject(String identifier) {
        try {
            return GriefPreventionPlugin.instance.permissionService.getGroupSubjects().hasSubject(identifier).get();
        } catch (InterruptedException e) {
            return false;
        } catch (ExecutionException e) {
            return false;
        }
    }

    public static boolean hasUserSubject(String identifier) {
        try {
            return GriefPreventionPlugin.instance.permissionService.getUserSubjects().hasSubject(identifier).get();
        } catch (InterruptedException e) {
            return false;
        } catch (ExecutionException e) {
            return false;
        }
    }

    public static Subject getGroupSubject(String identifier) {
        try {
            return GriefPreventionPlugin.instance.permissionService.getGroupSubjects().loadSubject(identifier).get();
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            return null;
        }
    }

    public static Subject getUserSubject(String identifier) {
        try {
            return GriefPreventionPlugin.instance.permissionService.getUserSubjects().loadSubject(identifier).get();
        } catch (InterruptedException e) {
            return null;
        } catch (ExecutionException e) {
            return null;
        }
    }

    public static Set<Context> getActiveContexts(Subject subject) {
        return getActiveContexts(subject, null, null);
    }

    public static Set<Context> getActiveContexts(Subject subject, GPPlayerData playerData, String flag) {
        if (flag != null && flag.startsWith(GPPermissions.COMMAND_EXECUTE)) {
            return new HashSet<>(subject.getActiveContexts());
        }
        if (playerData != null) {
            playerData.ignoreActiveContexts = true;
        }
        Set<Context> activeContexts = new HashSet<>(subject.getActiveContexts());
        if (playerData != null) {
            // If we are ignoring active contexts then we must remove all
            // contexts with gp_claim to avoid permissions returning wrong result.
            // For ex. a player using nucleus's tpa command will end up
            // with the current claim context being added to all active contexts
            // causing our permission checks to not be accurate.
            final Iterator<Context> iterator = activeContexts.iterator();
            while (iterator.hasNext()) {
                final Context context = iterator.next();
                if (context.getKey().contains("gp_claim")) {
                    iterator.remove();
                }
            }
        }
        return activeContexts;
    }
}
