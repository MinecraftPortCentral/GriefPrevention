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
package me.ryanhamshire.griefprevention.command;

import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import org.spongepowered.api.Platform.Component;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class CommandGpVersion implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        if (!src.hasPermission(GPPermissions.COMMAND_VERSION)) {
            return CommandResult.success();
        }

        String version = GriefPreventionPlugin.IMPLEMENTATION_VERSION;
        if (version == null) {
            version = "unknown";
        }
        final String spongePlatform = Sponge.getPlatform().getContainer(Component.IMPLEMENTATION).getName();
        Text gpVersion = Text.of(GriefPreventionPlugin.GP_TEXT, "Running ", TextColors.AQUA, "GriefPrevention ", version);
        Text spongeVersion = Text.of(GriefPreventionPlugin.GP_TEXT, "Running ", TextColors.YELLOW, spongePlatform, " ", GriefPreventionPlugin.SPONGE_VERSION);
        String permissionPlugin = Sponge.getServiceManager().getRegistration(PermissionService.class).get().getPlugin().getId();
        String permissionVersion = Sponge.getServiceManager().getRegistration(PermissionService.class).get().getPlugin().getVersion().orElse("unknown");
        Text permVersion = Text.of(GriefPreventionPlugin.GP_TEXT, "Running ", TextColors.GREEN, permissionPlugin, " ", permissionVersion);
        src.sendMessage(Text.of(gpVersion, "\n", spongeVersion, "\n", permVersion));
        return CommandResult.success();
    }
}
