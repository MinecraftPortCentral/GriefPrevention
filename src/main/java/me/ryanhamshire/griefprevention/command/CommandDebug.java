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

import me.ryanhamshire.griefprevention.GPDebugData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

public class CommandDebug implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        String target = ctx.<String>getOne("target").orElse(null);
        User user = ctx.<User>getOne("user").orElse(null);
        GPDebugData debugData = null;
        boolean paste = false;
        if (target.equalsIgnoreCase("on")) {
            debugData = getOrCreateDebugUser(src, user, true);
        } else if (target.equalsIgnoreCase("record")) {
            debugData = getOrCreateDebugUser(src, user, false);
        } else if (target.equalsIgnoreCase("paste")) {
            paste = true;
        } else if (target.equalsIgnoreCase("off")) {
            GriefPreventionPlugin.instance.getDebugUserMap().remove(src.getIdentifier());
            GriefPreventionPlugin.debugLogging = false;
        }

        final Text GP_TEXT = Text.of(TextColors.RESET, "[", TextColors.AQUA, "GP", TextColors.WHITE, "] ");
        if (debugData == null) {
            if (paste) {
                debugData = GriefPreventionPlugin.instance.getDebugUserMap().get(src.getIdentifier());
                if (debugData == null) {
                    src.sendMessage(Text.of(TextColors.RED, "Nothing to paste!"));
                    return CommandResult.success();
                }
                debugData.pasteRecords();
            }
            src.sendMessage(Text.of(GP_TEXT, TextColors.GRAY, "Debug ", TextColors.RED, "OFF"));
            GriefPreventionPlugin.instance.getDebugUserMap().remove(src.getIdentifier());
        } else {
            src.sendMessage(Text.of(
                    GP_TEXT, TextColors.GRAY, "Debug: ", TextColors.GREEN, "ON", TextColors.WHITE, " | ", 
                    TextColors.GRAY, "Verbose: ", !debugData.isRecording() ? Text.of(TextColors.GREEN, "ON") : Text.of(TextColors.RED, "OFF"), " | ",
                    TextColors.GRAY, "Record: ", debugData.isRecording() ? Text.of(TextColors.GREEN, "ON") : Text.of(TextColors.RED, "OFF"), " | ",
                    TextColors.GRAY, "User: ", TextColors.GOLD, user == null ? "ALL" : user.getName()));
            GriefPreventionPlugin.instance.getDebugUserMap().put(src.getIdentifier(), debugData);
        }

        return CommandResult.success();
    }

    private GPDebugData getOrCreateDebugUser(CommandSource src, User user, boolean verbose) {
        GPDebugData debugData = GriefPreventionPlugin.instance.getDebugUserMap().get(src.getIdentifier());
        if (debugData == null) {
            debugData = new GPDebugData(src, user, verbose);
            GriefPreventionPlugin.instance.getDebugUserMap().put(src.getIdentifier(), debugData);
        } else {
            debugData.setTarget(user);
            debugData.setVerbose(verbose);
        }
        return debugData;
    }
}
