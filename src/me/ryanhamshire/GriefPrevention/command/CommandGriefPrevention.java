/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) Ryan Hamshire
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
package me.ryanhamshire.GriefPrevention.command;

import com.google.common.collect.ImmutableMap;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.Messages;
import me.ryanhamshire.GriefPrevention.PlayerData;
import me.ryanhamshire.GriefPrevention.TextMode;
import me.ryanhamshire.GriefPrevention.Visualization;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.ChildCommandElementExecutor;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.util.Map;

@NonnullByDefault
public class CommandGriefPrevention {

    private static final String INDENT = "    ";
    private static final String LONG_INDENT = INDENT + INDENT;

    private static final Map<String, Boolean> FLAG_BOOLEANS = ImmutableMap.<String, Boolean>builder()
            .put("allow", true)
            .put("deny", false)
            .build();

    /**
     * Create a new instance of the GriefPrevention command structure.
     *
     * @return The newly created command
     */
    public static CommandSpec getCommand() {
        final ChildCommandElementExecutor commandExecutor = new ChildCommandElementExecutor(null);
        commandExecutor.register(getAbandonAllClaimsCommand(), "abandonallclaims");
        commandExecutor.register(getAbandonClaimCommand(), "abandonclaim", "unclaim", "declaim", "removeclaim", "disclaim");
        commandExecutor.register(getAbandonTopLevelClaimCommand(), "abandontoplevelclaim");
        commandExecutor.register(getIgnoreClaimsCommand(), "ignoreclaims", "ic");
        commandExecutor.register(getFlagsCommand(), "flag");
        return CommandSpec.builder()
                .description(Texts.of("Text description"))
                .extendedDescription(Texts.of("commands:\n",
                        INDENT, title("abandonallclaims"), LONG_INDENT, "Deletes ALL your claims\n",
                        INDENT, title("abandonclaim"), LONG_INDENT, "Deletes a claim\n",
                        INDENT, title("abandontoplevelclaim"), LONG_INDENT, "Deletes a claim and all its subdivisions\n",
                        INDENT, title("flag"), LONG_INDENT, "Toggles various flags in claims\n",
                        INDENT, title("ignoreclaims"), LONG_INDENT, "Toggles ignore claims mode\n"))
                .arguments(commandExecutor)
                .executor(commandExecutor)
                .build();
    }

    private static Text title(String title) {
        return Texts.of(TextColors.GREEN, title);
    }

    private static CommandCallable getAbandonAllClaimsCommand() {
        return CommandSpec.builder()
                .description(Texts.of("Deletes ALL your claims"))
                .permission("griefprevention.claims")
                .executor((src, args) -> {
                    final Player player = CommandHelper.checkPlayer(src);
                    // count claims
                    PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
                    int originalClaimCount = playerData.getClaims().size();

                    // check count
                    if (originalClaimCount == 0) {
                        throw new CommandException(GriefPrevention.getMessage(Messages.YouHaveNoClaims));
                    }

                    // adjust claim blocks
                    for (Claim claim : playerData.getClaims()) {
                        playerData.setAccruedClaimBlocks(
                                playerData.getAccruedClaimBlocks()
                                        - (int) Math.ceil((claim.getArea() * (1 - GriefPrevention.instance.config_claims_abandonReturnRatio))));
                    }

                    // delete them
                    GriefPrevention.instance.dataStore.deleteClaimsForPlayer(player.getUniqueId(), false);

                    // inform the player
                    int remainingBlocks = playerData.getRemainingClaimBlocks();
                    GriefPrevention.sendMessage(player, TextMode.Success, Messages.SuccessfulAbandon, String.valueOf(remainingBlocks));

                    // revert any current visualization
                    Visualization.Revert(player);

                    return CommandResult.success();
                })
                .build();
    }

    private static CommandCallable getAbandonClaimCommand() {
        return CommandSpec.builder()
                .description(Texts.of("Deletes a claim"))
                .permission("griefprevention.claims")
                .executor((src, args) -> {
                    final Player player = CommandHelper.checkPlayer(src);
                    return CommandHelper.abandonClaimHandler(player, false);
                }).build();
    }

    private static CommandCallable getAbandonTopLevelClaimCommand() {
        return CommandSpec.builder()
                .description(Texts.of("Deletes a claim and all its subdivisions"))
                .permission("griefprevention.claims")
                .executor((src, args) -> {
                    final Player player = CommandHelper.checkPlayer(src);
                    return CommandHelper.abandonClaimHandler(player, true);
                }).build();
    }

    private static CommandCallable getFlagsCommand() {
        return CommandSpec.builder()
                .description(Texts.of("Gets/Sets various claim flags in the claim you are standing in"))
                .permission("griefprevention.commands.flag")
                .child(CommandSpec.builder()
                        .arguments(GenericArguments.choices(Texts.of("state"), FLAG_BOOLEANS))
                        .executor((src, args) -> {
                            boolean state = args.<Boolean>getOne("state").get();
                            return CommandResult.success();
                        })
                        .build(), "spawn-mobs")
                .build();
    }

    private static CommandCallable getIgnoreClaimsCommand() {
        return CommandSpec.builder()
                .description(Texts.of("Toggles ignore claims mode"))
                .permission("griefprevention.commands.ignoreclaims")
                .executor((src, args) -> {
                    final Player player = CommandHelper.checkPlayer(src);
                    PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());

                    playerData.ignoreClaims = !playerData.ignoreClaims;

                    // toggle ignore claims mode on or off
                    if (!playerData.ignoreClaims) {
                        GriefPrevention.sendMessage(player, TextMode.Success, Messages.RespectingClaims);
                    } else {
                        GriefPrevention.sendMessage(player, TextMode.Success, Messages.IgnoringClaims);
                    }

                    return CommandResult.success();
                }).build();
    }
}
