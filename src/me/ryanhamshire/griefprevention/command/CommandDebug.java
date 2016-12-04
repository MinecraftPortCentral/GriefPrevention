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

import me.ryanhamshire.griefprevention.GriefPrevention;
import me.ryanhamshire.griefprevention.TextMode;
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
        User user = ctx.<User>getOne("user").orElse(null);
        boolean verbose = ctx.<Boolean>getOne("verbose").orElse(false);

        final Text GP_TEXT = Text.of(TextColors.RESET, "[", TextColors.AQUA, "GP", TextColors.WHITE, "] ");
        if (GriefPrevention.debugLogging) {
            src.sendMessage(Text.of(
                    GP_TEXT, TextColors.GRAY, "Debug ", TextColors.RED, "OFF", TextColors.WHITE, " | ", 
                    TextColors.GRAY, "Verbose ", TextColors.RED, "OFF"));
            GriefPrevention.debugLogging = false;
            GriefPrevention.debugVerbose = false;
            GriefPrevention.debugUser = null;
            GriefPrevention.debugSource = null;
        } else {
            
            src.sendMessage(Text.of(
                    GP_TEXT, TextColors.GRAY, "Debug ", TextColors.GREEN, "ON", TextColors.WHITE, " | ", 
                    TextColors.GRAY, "Verbose ", verbose ? Text.of(TextColors.GREEN, "ON") : Text.of(TextColors.RED, "OFF")));
            GriefPrevention.debugLogging = true;
            GriefPrevention.debugVerbose = verbose;
            GriefPrevention.debugUser = user;
            GriefPrevention.debugSource = src;
        }

        return CommandResult.success();
    }
}
