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

import com.google.common.collect.ImmutableMap;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.configuration.MessageStorage;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.world.schematic.Schematic;

import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CommandClaimSchematic implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        Player player;
        try {
            player = GriefPreventionPlugin.checkPlayer(src);
        } catch (CommandException e) {
            src.sendMessage(e.getText());
            return CommandResult.success();
        }

        String action = args.<String>getOne("action").orElse(null);
        String name = args.<String>getOne("name").orElse(null);
        GPPlayerData playerData = GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GPClaim claim = GriefPreventionPlugin.instance.dataStore.getClaimAtPlayer(playerData, player.getLocation(), false);
        final Text denyMessage = claim.allowEdit(player);
        if (denyMessage != null) {
            GriefPreventionPlugin.sendMessage(player, denyMessage);
            return CommandResult.success();
        }

        if (action == null) {
            if (claim.schematicBackups.isEmpty()) {
                GriefPreventionPlugin.sendMessage(player, Text.of(TextColors.RED, "There are no schematic backups for this claim."));
                return CommandResult.success();
            }

            List<Text> schematicTextList = new ArrayList<>();
            for (Schematic schematic : claim.schematicBackups) {
                final String schematicName = schematic.getMetadata().getString(DataQuery.of(".", Schematic.METADATA_NAME)).orElse("unknown");
                final String schematicDate = schematic.getMetadata().getString(DataQuery.of(".", Schematic.METADATA_DATE)).orElse("unknown");
                Instant schematicInstant;
                try {
                    schematicInstant = Instant.parse(schematicDate);
                } catch (Exception e) {
                    continue;
                }
                schematicTextList.add(
                        Text.builder().append(Text.of(schematicName))
                        .onClick(TextActions.executeCallback(displayConfirmationConsumer(src, claim, schematic)))
                        .onHover(TextActions.showText(
                                Text.of("Click here to restore schematic.",
                                        "\n", TextColors.YELLOW, "Name", TextColors.WHITE, ": ", TextColors.GREEN, schematicName, 
                                        "\n", TextColors.YELLOW, "Created", TextColors.WHITE, ": ", TextColors.AQUA, Date.from(schematicInstant))))
                        .build());
            }
            PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
            PaginationList.Builder paginationBuilder = paginationService.builder()
                    .title(Text.of(TextColors.AQUA,"Schematics")).padding(Text.of(TextStyles.STRIKETHROUGH, "-")).contents(schematicTextList);
            paginationBuilder.sendTo(src);
        } else if (action.equalsIgnoreCase("backup")) {
            GriefPreventionPlugin.sendMessage(player, Text.of(TextColors.GREEN, "Creating schematic backup..."));
            Schematic newSchematic = claim.createBackupSchematic(name).orElse(null);
            GriefPreventionPlugin.sendMessage(player, Text.of(TextColors.GREEN, "Schematic backup complete."));
        }
        return CommandResult.success();
    }

    private static Consumer<CommandSource> displayConfirmationConsumer(CommandSource src, Claim claim, Schematic schematic) {
        return confirm -> {
            final Text schematicConfirmationText = Text.builder().append(Text.of(
                    TextColors.WHITE, "\n", TextColors.WHITE, "[", TextColors.GREEN, "Confirm", TextColors.WHITE, "]\n"))
                .onClick(TextActions.executeCallback(createConfirmationConsumer(src, claim, schematic)))
                .onHover(TextActions.showText(Text.of("Clicking confirm will restore ALL claim data with schematic. Use cautiously!"))).build();
            GriefPreventionPlugin.sendMessage(src, schematicConfirmationText);
        };
    }

    private static Consumer<CommandSource> createConfirmationConsumer(CommandSource src, Claim claim, Schematic schematic) {
        return confirm -> {
            claim.applySchematic(schematic);
            final Text message = GriefPreventionPlugin.instance.messageData.schematicRestoreConfirmed
                    .apply(ImmutableMap.of(
                    "schematic_name", schematic.getMetadata().get(DataQuery.of(".", Schematic.METADATA_NAME)).orElse("unknown"))).build();
            GriefPreventionPlugin.sendMessage(src, message);
        };
    }
}
