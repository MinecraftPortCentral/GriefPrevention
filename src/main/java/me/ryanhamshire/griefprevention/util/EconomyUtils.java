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

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableMap;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimResult;
import me.ryanhamshire.griefprevention.api.claim.ClaimResultType;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.command.CommandHelper;
import me.ryanhamshire.griefprevention.permission.GPPermissions;
import me.ryanhamshire.griefprevention.provider.WorldEditApiProvider;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.cause.EventContextKeys;
import org.spongepowered.api.service.economy.Currency;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class EconomyUtils {

    public static void economyCreateClaimConfirmation(Player player, GPPlayerData playerData, int height, Vector3i point1, Vector3i point2, ClaimType claimType, boolean cuboid, Claim parent) {
        GPClaim claim = new GPClaim(player.getWorld(), point1, point2, claimType, player.getUniqueId(), cuboid);
        claim.parent = (GPClaim) parent;
        final int claimCost = BlockUtils.getClaimBlockCost(player.getWorld(), claim.lesserBoundaryCorner.getBlockPosition(), claim.greaterBoundaryCorner.getBlockPosition(), claim.cuboid);
        final double requiredFunds = claimCost * playerData.optionEconomyClaimBlockCost;
        final Text message = GriefPreventionPlugin.instance.messageData.economyClaimBuyConfirmation
                .apply(ImmutableMap.of(
                "sale_price", requiredFunds)).build();
        GriefPreventionPlugin.sendMessage(player, message);
        final Text buyConfirmationText = Text.builder().append(Text.of(
                TextColors.WHITE, "\n", TextColors.WHITE, "[", TextColors.GREEN, "Confirm", TextColors.WHITE, "]\n"))
            .onClick(TextActions.executeCallback(economyClaimBuyConfirmed(player, playerData, height, requiredFunds, point1, point2, claimType, cuboid, parent))).build();
        GriefPreventionPlugin.sendMessage(player, buyConfirmationText);
    }

    private static Consumer<CommandSource> economyClaimBuyConfirmed(Player player, GPPlayerData playerData, int height, double requiredFunds, Vector3i lesserBoundaryCorner, Vector3i greaterBoundaryCorner, ClaimType claimType, boolean cuboid, Claim parent) {
        return confirm -> {
            // try to create a new claim
            ClaimResult result = null;
            try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
                Sponge.getCauseStackManager().pushCause(player);
                Sponge.getCauseStackManager().addContext(EventContextKeys.PLUGIN, GriefPreventionPlugin.instance.pluginContainer);
                result = GriefPreventionPlugin.instance.dataStore.createClaim(
                        player.getWorld(),
                        lesserBoundaryCorner,
                        greaterBoundaryCorner,
                        claimType, player.getUniqueId(), cuboid);
            }

            GPClaim gpClaim = (GPClaim) result.getClaim().orElse(null);
            // if it didn't succeed, tell the player why
            if (!result.successful()) {
                if (result.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                    GPClaim overlapClaim = (GPClaim) result.getClaim().get();
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.claimCreateOverlapShort.toText());
                    List<Claim> claims = new ArrayList<>();
                    claims.add(overlapClaim);
                    CommandHelper.showClaims(player, claims, height, true);
                }
                return;
            }

            // otherwise, advise him on the /trust command and show him his new claim
            else {
                Text message = GriefPreventionPlugin.instance.messageData.economyClaimBuyConfirmed
                        .apply(ImmutableMap.of(
                            "sale_price", requiredFunds)).build();
                GriefPreventionPlugin.sendMessage(player, message);
                playerData.lastShovelLocation = null;
                message = GriefPreventionPlugin.instance.messageData.claimCreateSuccess
                        .apply(ImmutableMap.of(
                        "type", gpClaim.getType().name())).build();
                GriefPreventionPlugin.sendMessage(player, message);
                final WorldEditApiProvider worldEditProvider = GriefPreventionPlugin.instance.worldEditProvider;
                if (worldEditProvider != null) {
                    worldEditProvider.stopVisualDrag(player);
                    worldEditProvider.visualizeClaim(gpClaim, player, playerData, false);
                }
                gpClaim.getVisualizer().createClaimBlockVisuals(height, player.getLocation(), playerData);
                gpClaim.getVisualizer().apply(player, false);
                // if it's a big claim, tell the player about subdivisions
                if (!player.hasPermission(GPPermissions.COMMAND_ADMIN_CLAIMS) && gpClaim.getClaimBlocks() >= 1000) {
                    GriefPreventionPlugin.sendMessage(player, GriefPreventionPlugin.instance.messageData.urlSubdivisionBasics.toText(), 201L);
                }
            }
        };
    }

    public static TransactionResult depositFunds(UUID uuid, double amount) {
        final Account playerAccount = GriefPreventionPlugin.instance.economyService.get().getOrCreateAccount(uuid).orElse(null);
        final Currency defaultCurrency = GriefPreventionPlugin.instance.economyService.get().getDefaultCurrency();
        return playerAccount.deposit(defaultCurrency, BigDecimal.valueOf(amount), Sponge.getCauseStackManager().getCurrentCause());
    }

    public static TransactionResult withdrawFunds(UUID uuid, double amount) {
        final Account playerAccount = GriefPreventionPlugin.instance.economyService.get().getOrCreateAccount(uuid).orElse(null);
        final Currency defaultCurrency = GriefPreventionPlugin.instance.economyService.get().getDefaultCurrency();
        return playerAccount.withdraw(defaultCurrency, BigDecimal.valueOf(amount), Sponge.getCauseStackManager().getCurrentCause());
    }
}
