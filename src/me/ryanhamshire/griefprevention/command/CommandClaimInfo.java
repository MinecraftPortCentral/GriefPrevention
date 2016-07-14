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
import me.ryanhamshire.griefprevention.claim.Claim;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.UUID;

public class CommandClaimInfo implements CommandExecutor {

    private static final Text NONE = Text.of(TextColors.GRAY, "none");

    @SuppressWarnings("unused")
    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        Player player;
        try {
            player = GriefPrevention.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        Claim claim = GriefPrevention.instance.dataStore.getClaimAtPlayer(player, false);

        if (claim != null) {
            UUID ownerUniqueId = claim.getClaimData().getOwnerUniqueId();
            if (claim.parent != null) {
                ownerUniqueId = claim.parent.ownerID;
            }
            User owner = null;
            if (!claim.isWildernessClaim()) {
                owner =  GriefPrevention.getOrCreateUser(ownerUniqueId);
            }

            Text name = claim.getClaimData().getClaimName();
            Text greeting = claim.getClaimData().getGreetingMessage();
            Text farewell = claim.getClaimData().getFarewellMessage();
            String accessors = "";
            String builders = "";
            String containers = "";
            String managers = "";
            Date created = null;
            Date lastActive = null;
            try {
                Instant instant = Instant.parse(claim.getClaimData().getDateCreated());
                created = Date.from(instant);
            } catch(DateTimeParseException ex) {
                // ignore
            }

            try {
                Instant instant = Instant.parse(claim.getClaimData().getDateLastActive());
                lastActive = Date.from(instant);
            } catch(DateTimeParseException ex) {
                // ignore
            }

            Text claimName = Text.of(TextColors.YELLOW, "Name", TextColors.WHITE, " : ", TextColors.GRAY, name == null ? NONE : name);
            for (UUID uuid : claim.getClaimData().getAccessors()) {
                User user = GriefPrevention.getOrCreateUser(uuid);
                accessors += user.getName() + " ";
            }
            for (UUID uuid : claim.getClaimData().getBuilders()) {
                User user = GriefPrevention.getOrCreateUser(uuid);
                builders += user.getName() + " ";
            }
            for (UUID uuid : claim.getClaimData().getContainers()) {
                User user = GriefPrevention.getOrCreateUser(uuid);
                containers += user.getName() + " ";
            }
            for (UUID uuid : claim.getClaimData().getManagers()) {
                User user = GriefPrevention.getOrCreateUser(uuid);
                managers += user.getName() + " ";
            }

            TextColor claimTypeColor = TextColors.GREEN;
            if (claim.isAdminClaim()) {
                if (claim.isSubdivision()) {
                    claimTypeColor = TextColors.DARK_AQUA;
                } else {
                    claimTypeColor = TextColors.RED;
                }
            } else if (claim.isSubdivision()) {
                claimTypeColor = TextColors.AQUA;
            }
            Text claimId = Text.join(Text.of(TextColors.YELLOW, "UUID", TextColors.WHITE, " : ",
                    Text.builder()
                            .append(Text.of(TextColors.GRAY, claim.id.toString()))
                            .onShiftClick(TextActions.insertText(claim.id.toString())).build()));
            Text ownerLine = Text.of(TextColors.YELLOW, "Owner", TextColors.WHITE, " : ", TextColors.GOLD, owner != null ? owner.getName() : "administrator");
            Text claimType = Text.of(TextColors.YELLOW, "Type", TextColors.WHITE, " : ", claimTypeColor, claim.type.name());
            Text claimFarewell = Text.of(TextColors.YELLOW, "Farewell", TextColors.WHITE, " : ", TextColors.RESET,
                    farewell == null ? NONE : farewell);
            Text claimGreeting = Text.of(TextColors.YELLOW, "Greeting", TextColors.WHITE, " : ", TextColors.RESET,
                    greeting == null ? NONE : greeting);
            Text pvp = Text.of(TextColors.YELLOW, "PvP", TextColors.WHITE, " : ", TextColors.RESET, claim.isPvpEnabled() ? Text.of(TextColors.GREEN, "ON") : Text.of(TextColors.RED, "OFF"));
            Text lesserCorner = Text.of(TextColors.YELLOW, "LesserCorner", TextColors.WHITE, " : ", TextColors.GRAY,
                    claim.getLesserBoundaryCorner().getBlockPosition());
            Text greaterCorner = Text.of(TextColors.YELLOW, "GreaterCorner", TextColors.WHITE, " : ", TextColors.GRAY,
                    claim.getGreaterBoundaryCorner().getBlockPosition());
            Text claimArea = Text.of(TextColors.YELLOW, "Area", TextColors.WHITE, " : ", TextColors.GRAY, claim.getArea(), " blocks");
            Text claimAccessors = Text.of(TextColors.YELLOW, "Accessors", TextColors.WHITE, " : ", TextColors.BLUE, accessors.equals("") ? NONE : accessors);
            Text claimBuilders = Text.of(TextColors.YELLOW, "Builders", TextColors.WHITE, " : ", TextColors.YELLOW, builders.equals("") ? NONE : builders);
            Text claimContainers = Text.of(TextColors.YELLOW, "Containers", TextColors.WHITE, " : ", TextColors.GREEN, containers.equals("") ? NONE : containers);
            Text claimCoowners = Text.of(TextColors.YELLOW, "Managers", TextColors.WHITE, " : ", TextColors.GOLD, managers.equals("") ? NONE : managers);
            Text dateCreated = Text.of(TextColors.YELLOW, "Created", TextColors.WHITE, " : ", TextColors.GRAY, created != null ? created : "Unknown");
            Text dateLastActive = Text.of(TextColors.YELLOW, "LastActive", TextColors.WHITE, " : ", TextColors.GRAY, lastActive != null ? lastActive : "Unknown");
            Text footer = Text.of(TextColors.WHITE, TextStyles.STRIKETHROUGH, "------------------------------------------");
            if (!claim.isWildernessClaim()) {
                GriefPrevention.sendMessage(src,
                        Text.of("\n",
                                footer, "\n",
                                claimName, "\n",
                                ownerLine, "\n",
                                claimType, "\n",
                                claimArea, "\n",
                                claimAccessors, "\n",
                                claimBuilders, "\n",
                                claimContainers, "\n",
                                claimCoowners, "\n",
                                claimGreeting, "\n",
                                claimFarewell, "\n",
                                pvp, "\n",
                                dateCreated, "\n",
                                dateLastActive, "\n",
                                claimId, "\n",
                                footer));
            } else {
                GriefPrevention.sendMessage(src,
                        Text.of("\n",
                                footer, "\n",
                                claimName, "\n",
                                ownerLine, "\n",
                                claimType, "\n",
                                claimArea, "\n",
                                claimGreeting, "\n",
                                claimFarewell, "\n",
                                pvp, "\n",
                                dateCreated, "\n",
                                claimId, "\n",
                                footer));
            }
        } else {
            GriefPrevention.sendMessage(src, Text.of(TextMode.Err, "No claim in your current location."));
        }

        return CommandResult.success();
    }
}
