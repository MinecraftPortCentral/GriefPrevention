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
package me.ryanhamshire.griefprevention.task;

import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.economy.BankTransactionType;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.claim.GPClaimManager;
import me.ryanhamshire.griefprevention.configuration.GriefPreventionConfig;
import me.ryanhamshire.griefprevention.economy.GPBankTransaction;
import me.ryanhamshire.griefprevention.event.GPTaxClaimEvent;
import me.ryanhamshire.griefprevention.permission.GPOptionHandler;
import me.ryanhamshire.griefprevention.permission.GPOptions;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.Account;
import org.spongepowered.api.service.economy.transaction.ResultType;
import org.spongepowered.api.service.economy.transaction.TransactionResult;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.world.storage.WorldProperties;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TaxApplyTask implements Runnable {

    final WorldProperties worldProperties;
    final EconomyService economyService;
    final GriefPreventionConfig<?> activeConfig;
    private int bankTransactionLogLimit = 60;

    public TaxApplyTask(WorldProperties worldProperties) {
        this.worldProperties = worldProperties;
        this.economyService = GriefPreventionPlugin.instance.economyService.get();
        this.activeConfig = GriefPreventionPlugin.getActiveConfig(this.worldProperties);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void run() {
        // don't do anything when there are no claims
        GPClaimManager claimManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(this.worldProperties);
        ArrayList<Claim> claimList = (ArrayList<Claim>) claimManager.getWorldClaims();
        if (claimList.size() == 0) {
            return;
        }

        this.bankTransactionLogLimit = this.activeConfig.getConfig().claim.bankTransactionLogLimit;
        Iterator<GPClaim> iterator = ((ArrayList) claimList.clone()).iterator();
        while (iterator.hasNext()) {
            GPClaim claim = iterator.next();
            final GPPlayerData playerData = claim.getOwnerPlayerData();
            if (claim.isWilderness()) {
                continue;
            }
            if (playerData == null) {
                continue;
            }

            if (!playerData.dataInitialized) {
                continue;
            }

            if (claim.isAdminClaim()) {
                // search for town
                final List<Claim> children = claim.getChildren(false);
                for (Claim child : children) {
                    if (child.isTown()) {
                        handleTownTax((GPClaim) child, playerData);
                    } else if (child.isBasicClaim()) {
                        handleClaimTax((GPClaim) child, playerData, false);
                    }
                }
            } else {
                if (claim.isTown()) {
                    handleTownTax(claim, playerData);
                } else if (claim.isBasicClaim()){
                    handleClaimTax(claim, playerData, false);
                }
            }
        }
    }

    private void handleClaimTax(GPClaim claim, GPPlayerData playerData, boolean inTown) {
        final Subject subject = playerData.getPlayerSubject();
        final Account claimAccount = claim.getEconomyAccount().orElse(null);
        double taxRate = GPOptionHandler.getClaimOptionDouble(subject, claim, GPOptions.Type.TAX_RATE, playerData);
        double taxOwed = claim.getEconomyData().getTaxBalance() + (claim.getClaimBlocks() * taxRate);
        try (final CauseStackManager.StackFrame frame = Sponge.getCauseStackManager().pushCauseFrame()) {
            Sponge.getCauseStackManager().pushCause(GriefPreventionPlugin.instance);
            GPTaxClaimEvent event = new GPTaxClaimEvent(claim, taxRate, taxOwed);
            Sponge.getEventManager().post(event);
            if (event.isCancelled()) {
                return;
            }
            final double taxBalance = claim.getEconomyData().getTaxBalance();
            taxRate = event.getTaxRate();
            taxOwed = taxBalance + (claim.getClaimBlocks() * taxRate);

            TransactionResult result = claimAccount.withdraw(this.economyService.getDefaultCurrency(), BigDecimal.valueOf(taxOwed), Sponge.getCauseStackManager().getCurrentCause());
            if (result.getResult() != ResultType.SUCCESS) {
                final Instant localNow = Instant.now();
                Instant taxPastDueDate = claim.getEconomyData().getTaxPastDueDate().orElse(null);
                if (taxPastDueDate == null) {
                    claim.getEconomyData().setTaxPastDueDate(localNow);
                } else {
                    final int taxExpirationDays = GPOptionHandler.getClaimOptionDouble(subject, claim, GPOptions.Type.TAX_EXPIRATION, playerData).intValue();
                    if (taxExpirationDays <= 0) {
                        claim.getInternalClaimData().setExpired(true);
                        claim.getData().save();
                    } else if (taxPastDueDate.plus(Duration.ofDays(taxExpirationDays)).isBefore(localNow)) {
                        claim.getInternalClaimData().setExpired(true);
                        claim.getData().save();
                    }
                }
                final double totalTaxOwed = taxBalance + taxOwed;
                claim.getEconomyData().setTaxBalance(totalTaxOwed);
                claim.getEconomyData().addBankTransaction(new GPBankTransaction(BankTransactionType.TAX_FAIL, Instant.now(), taxOwed));
            } else {
                claim.getEconomyData().addBankTransaction(new GPBankTransaction(BankTransactionType.TAX_SUCCESS, Instant.now(), taxOwed));
                claim.getEconomyData().setTaxPastDueDate(null);
                claim.getEconomyData().setTaxBalance(0);
                claim.getInternalClaimData().setExpired(false);
                if (inTown) {
                    final GPClaim town = claim.getTownClaim();
                    town.getData()
                        .getEconomyData()
                        .addBankTransaction(new GPBankTransaction(BankTransactionType.TAX_SUCCESS, Instant.now(), taxOwed));
                    town.getEconomyAccount()
                        .get()
                        .deposit(this.economyService.getDefaultCurrency(), BigDecimal.valueOf(taxOwed), Sponge.getCauseStackManager().getCurrentCause());
                }
                claim.getData().save();
            }
        }
    }

    private void handleTownTax(GPClaim town, GPPlayerData playerData) {
        Account townAccount = town.getEconomyAccount().orElse(null);
        if (townAccount == null) {
            // Virtual Accounts not supported by Economy Plugin so ignore
            return;
        }
        List<Claim> children = town.getChildren(true);
        for (Claim child : children) {
            // resident tax
            if (child.isBasicClaim()) {
                handleClaimTax((GPClaim) child, playerData, true);
            }
        }
        if (town.getOwnerUniqueId().equals(playerData.playerID)) {
            handleClaimTax(town, playerData, false);
        }
    }
}
