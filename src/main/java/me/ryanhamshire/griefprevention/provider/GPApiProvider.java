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
package me.ryanhamshire.griefprevention.provider;

import com.google.common.collect.ImmutableList;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.GriefPreventionApi;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.Claim.Builder;
import me.ryanhamshire.griefprevention.api.claim.ClaimBlockSystem;
import me.ryanhamshire.griefprevention.api.claim.ClaimFlag;
import me.ryanhamshire.griefprevention.api.claim.ClaimManager;
import me.ryanhamshire.griefprevention.api.data.PlayerData;
import me.ryanhamshire.griefprevention.api.economy.BankTransaction;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.economy.GPBankTransaction;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class GPApiProvider implements GriefPreventionApi {

    public static final double API_VERSION = 0.91;

    public GPApiProvider() {
    }

    @Override
    public double getApiVersion() {
        return API_VERSION;
    }

    @Override
    public String getImplementationVersion() {
        return GriefPreventionPlugin.IMPLEMENTATION_VERSION;
    }

    @Override
    public boolean isEnabled(WorldProperties worldProperties) {
        return GriefPreventionPlugin.instance.claimsEnabledForWorld(worldProperties);
    }

    @Override
    public ClaimBlockSystem getClaimBlockSystem() {
        return GriefPreventionPlugin.getGlobalConfig().getConfig().playerdata.claimBlockSystem;
    }

    @Override
    public boolean isProtectionModuleEnabled(ClaimFlag flag) {
        return GriefPreventionPlugin.getGlobalConfig().getConfig().modules.isProtectionModuleEnabled(flag.toString());
    }

    @Override
    public ClaimManager getClaimManager(WorldProperties worldProperties) {
        return GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(worldProperties);
    }

    @Override
    public Builder createClaimBuilder() {
        return new GPClaim.ClaimBuilder();
    }

    @Override
    public BankTransaction.Builder createBankTransactionBuilder() {
        return new GPBankTransaction.BankTransactionBuilder();
    }

    @Override
    public Optional<PlayerData> getGlobalPlayerData(UUID playerUniqueId) {
        final WorldProperties worldProperties = Sponge.getServer().getDefaultWorld().orElse(null);
        if (worldProperties == null) {
            return Optional.empty();
        }

        return this.getWorldPlayerData(worldProperties, playerUniqueId);
    }

    @Override
    public Optional<PlayerData> getWorldPlayerData(WorldProperties worldProperties, UUID playerUniqueId) {
        return Optional.ofNullable(GriefPreventionPlugin.instance.dataStore.getOrCreatePlayerData(worldProperties, playerUniqueId));
    }

    @Override
    public List<Claim> getAllPlayerClaims(UUID playerUniqueId) {
        List<Claim> claimList = new ArrayList<>();
        for (World world : Sponge.getServer().getWorlds()) {
            claimList.addAll(this.getClaimManager(world).getPlayerClaims(playerUniqueId));
        }

        return ImmutableList.copyOf(claimList);
    }
}
