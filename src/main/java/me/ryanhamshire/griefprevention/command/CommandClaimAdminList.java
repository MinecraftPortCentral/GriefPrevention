/*
 * This file is part of GriefPrevention, licensed under the MIT License (MIT).
 *
 * Copyright (c) Ryan Hamshire
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

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Lists;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.claim.GPClaimManager;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandPermissionException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandExecutor;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationList;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.List;
import java.util.function.Consumer;

public class CommandClaimAdminList implements CommandExecutor {

    @Override
    public CommandResult execute(CommandSource src, CommandContext ctx) {
        WorldProperties worldProperties = ctx.<WorldProperties>getOne("world").orElse(null);
        if (worldProperties == null) {
            if (src instanceof Player) {
                worldProperties = ((Player) src).getWorld().getProperties();
            } else {
                worldProperties = Sponge.getServer().getDefaultWorld().get();
            }
        }

        if (!src.hasPermission(GPPermissions.COMMAND_LIST_ADMIN_CLAIMS)) {
            try {
                throw new CommandPermissionException();
            } catch (CommandPermissionException e) {
                src.sendMessage(e.getText());
                return CommandResult.success();
            }
        }

        GPClaimManager claimWorldManager =  GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(worldProperties);
        List<Claim> claimList = claimWorldManager.getWorldClaims();
        List<Text> claimsTextList = Lists.newArrayList();
        if (claimList.size() > 0) {
            for (Claim worldClaim : claimList) {
                GPClaim claim = (GPClaim) worldClaim;
                if (!claim.isAdminClaim()) {
                    continue;
                }
                Location<World> southWest = claim.lesserBoundaryCorner.setPosition(new Vector3d(claim.lesserBoundaryCorner.getPosition().getX(), 65.0D, claim.greaterBoundaryCorner.getPosition().getZ()));
                Text claimName = claim.getData().getName().orElse(null);
                if (claimName == null) {
                    claimName = Text.of(TextColors.GREEN, "Claim");
                }

                Text claimInfoCommandClick = Text.builder().append(Text.of(
                        TextColors.GREEN, claimName))
                .onClick(TextActions.executeCallback(CommandHelper.createCommandConsumer(src, "claiminfo", claim.id.toString(), createReturnClaimListConsumer(src, worldProperties.getWorldName()))))
                .onHover(TextActions.showText(Text.of("Click here to check claim info.")))
                .build();

                Text claimCoordsTPClick = Text.builder().append(Text.of(
                        TextColors.GRAY, southWest.getBlockPosition()))
                .onClick(TextActions.executeCallback(CommandHelper.createTeleportConsumer(src, southWest, claim)))
                .onHover(TextActions.showText(Text.of("Click here to teleport to ", claimName, ".")))
                .build();

                claimsTextList.add(Text.builder()
                        .append(Text.of(
                                claimInfoCommandClick, TextColors.WHITE, " : ", 
                                claimCoordsTPClick, " ", 
                                TextColors.YELLOW, "(Area : " + claim.getArea() + " blocks)"))
                        .build());
            }
            if (claimsTextList.size() == 0) {
                claimsTextList.add(Text.of(TextColors.RED, "No admin claims found in world."));
            }
        }

        PaginationService paginationService = Sponge.getServiceManager().provide(PaginationService.class).get();
        PaginationList.Builder paginationBuilder = paginationService.builder()
                .title(Text.of(TextColors.AQUA, "Admin Claims")).padding(Text.of("-")).contents(claimsTextList);
        paginationBuilder.sendTo(src);

        return CommandResult.success();
    }

    private Consumer<CommandSource> createReturnClaimListConsumer(CommandSource src, String arguments) {
        return consumer -> {
            Text claimListReturnCommand = Text.builder().append(Text.of(
                    TextColors.WHITE, "\n[", TextColors.AQUA, "Return to claimslist", TextColors.WHITE, "]\n"))
                .onClick(TextActions.executeCallback(CommandHelper.createCommandConsumer(src, "adminclaimslist", arguments))).build();
            src.sendMessage(claimListReturnCommand);
        };
    }
}
