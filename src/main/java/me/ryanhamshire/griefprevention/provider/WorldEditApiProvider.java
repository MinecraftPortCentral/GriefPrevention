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

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Maps;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import com.sk89q.worldedit.world.World;
import me.ryanhamshire.griefprevention.GPPlayerData;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.claim.GPClaim;
import me.ryanhamshire.griefprevention.provider.worldedit.GPActor;
import me.ryanhamshire.griefprevention.provider.worldedit.cui.MultiSelectionColors;
import me.ryanhamshire.griefprevention.provider.worldedit.cui.event.MultiSelectionClearEvent;
import me.ryanhamshire.griefprevention.provider.worldedit.cui.event.MultiSelectionColorEvent;
import me.ryanhamshire.griefprevention.provider.worldedit.cui.event.MultiSelectionCuboidEvent;
import me.ryanhamshire.griefprevention.provider.worldedit.cui.event.MultiSelectionGridEvent;
import me.ryanhamshire.griefprevention.provider.worldedit.cui.event.MultiSelectionPointEvent;
import me.ryanhamshire.griefprevention.util.BlockUtils;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WorldEditApiProvider {

    private final WorldEdit worldEditService;
    private Map<UUID, GPActor> worldEditPlayers = Maps.newHashMap();

    public WorldEditApiProvider() {
        this.worldEditService = WorldEdit.getInstance();
    }

    public WorldEdit getWorldEditService() {
        return this.worldEditService;
    }

    public LocalSession getLocalSession(String playerName) {
        return WorldEdit.getInstance().getSessionManager().findByName(playerName);
    }

    public World getWorld(String playerName) {
        final LocalSession session = getLocalSession(playerName);
        if (session == null) {
            return null;
        }

        return session.getSelectionWorld();
    }

    public World getWorld(org.spongepowered.api.world.World spongeWorld) {
        for (World world : this.worldEditService.getServer().getWorlds()) {
            if (world.getName().equals(spongeWorld.getName())) {
                return world;
            }
        }

        return null;
    }

    public RegionSelector getRegionSelector(Player player) {
        final LocalSession session = getLocalSession(player.getName());
        if (session == null) {
            return null;
        }

        World world = session.getSelectionWorld();
        if (world == null) {
            world = this.getWorld(player.getWorld());
        }
        return session.getRegionSelector(world);
    }

    public Vector createVector(Vector3i point) {
        return new Vector(point.getX(), point.getY(), point.getZ());
    }

    public GPActor getOrCreateActor(Player player) {
        if (this.worldEditPlayers.containsKey(player.getUniqueId())) {
            return this.worldEditPlayers.get(player.getUniqueId());
        }

        final GPActor actor = new GPActor(player);
        this.worldEditPlayers.put(player.getUniqueId(), actor);
        return actor;
    }

    public void removePlayer(Player player) {
        this.worldEditPlayers.remove(player.getUniqueId());
    }

    public void visualizeClaim(Claim claim, Player player, GPPlayerData playerData, boolean investigating) {
        this.visualizeClaim(claim, claim.getLesserBoundaryCorner().getBlockPosition(), claim.getGreaterBoundaryCorner().getBlockPosition(), player, playerData, investigating);
    }

    public void visualizeClaim(Claim claim, Vector3i pos1, Vector3i pos2, Player player, GPPlayerData playerData, boolean investigating) {
        // revert any current visuals if investigating
        if (investigating) {
            this.revertVisuals(player, playerData, null);
        }
        final LocalSession session = this.getLocalSession(player.getName());
        if (session == null || !session.hasCUISupport()) {
            return;
        }
        final Vector point1 = this.createVector(pos1);
        final Vector point2 = this.createVector(pos2);
        final CuboidRegionSelector regionSelector = new CuboidRegionSelector(session.getSelectionWorld(), point1, point2);
        final GPActor actor = this.getOrCreateActor(player);
        session.setRegionSelector(this.getWorld(player.getWorld()), regionSelector);
        actor.dispatchCUIEvent(new MultiSelectionCuboidEvent(claim.getUniqueId()));
        actor.dispatchCUIEvent(new MultiSelectionPointEvent(0, point1, regionSelector.getArea()));
        if (playerData.claimResizing != null) {
            actor.dispatchCUIEvent(new MultiSelectionPointEvent(1));
        } else {
            actor.dispatchCUIEvent(new MultiSelectionPointEvent(1, point2, regionSelector.getArea()));
        }

        if (investigating || playerData.lastShovelLocation == null) {
            actor.dispatchCUIEvent(new MultiSelectionColorEvent(MultiSelectionColors.RED, MultiSelectionColors.getClaimColor(claim), "", ""));
        }
        actor.dispatchCUIEvent(new MultiSelectionGridEvent(10));
    }

    public void visualizeClaims(Set<Claim> claims, Player player, GPPlayerData playerData, boolean investigating) {
        for (Claim claim : claims) {
            if (((GPClaim) claim).children.size() > 0) {
                visualizeClaims(((GPClaim) claim).getInternalChildren(true), player, playerData, investigating);
            }
            final LocalSession session = this.getLocalSession(player.getName());
            if (session == null || !session.hasCUISupport()) {
                return;
            }
            final Vector point1 = this.createVector(claim.getLesserBoundaryCorner().getBlockPosition());
            final Vector point2 = this.createVector(claim.getGreaterBoundaryCorner().getBlockPosition());
            final CuboidRegionSelector regionSelector = new CuboidRegionSelector(session.getSelectionWorld(), point1, point2);
            final GPActor actor = this.getOrCreateActor(player);
            session.setRegionSelector(this.getWorld(player.getWorld()), regionSelector);
            actor.dispatchCUIEvent(new MultiSelectionCuboidEvent(claim.getUniqueId()));
            actor.dispatchCUIEvent(new MultiSelectionPointEvent(0, point1, regionSelector.getArea()));
            if (playerData.claimResizing != null) {
                actor.dispatchCUIEvent(new MultiSelectionPointEvent(1));
            } else {
                actor.dispatchCUIEvent(new MultiSelectionPointEvent(1, point2, regionSelector.getArea()));
            }
            if (investigating) {
                actor.dispatchCUIEvent(new MultiSelectionColorEvent(MultiSelectionColors.RED, MultiSelectionColors.getClaimColor(claim), "", ""));
            }
            actor.dispatchCUIEvent(new MultiSelectionGridEvent(10));
        }
    }

    public void revertVisuals(Player player, GPPlayerData playerData, UUID claimUniqueId) {
        final LocalSession session = this.getLocalSession(player.getName());
        if (session == null || !session.hasCUISupport()) {
            return;
        }
        final World world = this.getWorld(player.getWorld());
        final RegionSelector region = session.getRegionSelector(world);
        final GPActor actor = this.getOrCreateActor(player);
        region.clear();
        session.dispatchCUISelection(actor);
        if (claimUniqueId != null) {
            actor.dispatchCUIEvent(new MultiSelectionClearEvent(claimUniqueId));
        } else {
            actor.dispatchCUIEvent(new MultiSelectionClearEvent());
        }
    }

    public void stopVisualDrag(Player player) {
        final GPActor actor = this.getOrCreateActor(player);
        actor.dispatchCUIEvent(new MultiSelectionClearEvent(player.getUniqueId()));
    }

    public void sendVisualDrag(Player player, GPPlayerData playerData, Vector3i pos) {
        final LocalSession session = this.getLocalSession(player.getName());
        if (session == null || !session.hasCUISupport()) {
            return;
        }

        final Location<org.spongepowered.api.world.World> location = BlockUtils.getTargetBlock(player, playerData, 60, true).orElse(null);
        Vector point1 = null;
        if (playerData.claimResizing != null) {
            // get opposite corner
            final GPClaim claim = playerData.claimResizing;
            final int x = playerData.lastShovelLocation.getBlockX() == claim.lesserBoundaryCorner.getBlockX() ? claim.greaterBoundaryCorner.getBlockX() : claim.lesserBoundaryCorner.getBlockX();
            final int y = playerData.lastShovelLocation.getBlockY() == claim.lesserBoundaryCorner.getBlockY() ? claim.greaterBoundaryCorner.getBlockY() : claim.lesserBoundaryCorner.getBlockY();
            final int z = playerData.lastShovelLocation.getBlockZ() == claim.lesserBoundaryCorner.getBlockZ() ? claim.greaterBoundaryCorner.getBlockZ() : claim.lesserBoundaryCorner.getBlockZ();
            point1 = new Vector(x, y, z);
        } else {
            point1 = this.createVector(pos);
        }
        Vector point2 = null;
        if (location == null) {
            point2 = new Vector(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ());
        } else {
            point2 = this.createVector(location.getBlockPosition());
        }

        final CuboidRegionSelector regionSelector = new CuboidRegionSelector(session.getSelectionWorld(), point1, point2);
        final GPActor actor = this.getOrCreateActor(player);
        actor.dispatchCUIEvent(new MultiSelectionCuboidEvent(player.getUniqueId()));
        actor.dispatchCUIEvent(new MultiSelectionPointEvent(0, point1, regionSelector.getArea()));
        actor.dispatchCUIEvent(new MultiSelectionPointEvent(1));
    }

    public boolean hasCUISupport(Player player) {
        return hasCUISupport(player.getName());
    }

    public boolean hasCUISupport(String name) {
        final LocalSession session = this.getLocalSession(name);
        if (session == null || !session.hasCUISupport()) {
            return false;
        }

        return true;
    }
}
