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
package me.ryanhamshire.griefprevention.migrator;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.Lists;
import me.ryanhamshire.griefprevention.GriefPreventionPlugin;
import me.ryanhamshire.griefprevention.api.claim.Claim;
import me.ryanhamshire.griefprevention.api.claim.ClaimResult;
import me.ryanhamshire.griefprevention.api.claim.ClaimType;
import me.ryanhamshire.griefprevention.api.claim.TrustType;
import me.ryanhamshire.griefprevention.claim.GPClaimManager;
import me.ryanhamshire.griefprevention.command.CommandHelper;
import net.minecraft.util.math.ChunkPos;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.spongepowered.api.service.economy.EconomyService;
import org.spongepowered.api.service.economy.account.UniqueAccount;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.World;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

public class PolisMigrator {

    private static final String padding = "--------------------------------------------------";
    private static int skippedCount = 0;
    private static int successfulCount = 0;
    private static final List<UUID> ECONOMY_TRANSFERS = new ArrayList<>();
    private static final Direction[] CARDINAL_DIRECTIONS = new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    public static void migrate(World world, Path claimsFilePath, Path teamsFilePath, Path gpClaimDataPath) throws FileNotFoundException, ClassNotFoundException {
        if (!GriefPreventionPlugin.getGlobalConfig().getConfig().migrator.polisMigrator) {
            return;
        }

        skippedCount = 0;
        successfulCount = 0;
        ECONOMY_TRANSFERS.clear();

        try {
            GriefPreventionPlugin.instance.getLogger().info("Starting Polis data migration for world " + world.getProperties().getWorldName() + "...");
            ConfigurationLoader<CommentedConfigurationNode> claimConfig = HoconConfigurationLoader.builder().setPath(claimsFilePath).build();
            ConfigurationLoader<CommentedConfigurationNode> teamConfig = HoconConfigurationLoader.builder().setPath(teamsFilePath).build();
            CommentedConfigurationNode claimsRoot = claimConfig.load();
            CommentedConfigurationNode teamsRoot = teamConfig.load();
            GriefPreventionPlugin.instance.getLogger().info("Scanning Polis claims in file '" + claimsFilePath + "'...");
            CommentedConfigurationNode claimList = claimsRoot.getNode("claims");
            // Begin configuration inception
            for (Entry<Object, ? extends CommentedConfigurationNode> mapEntry : claimList.getChildrenMap().entrySet()) {
                String townName = (String) mapEntry.getKey();
                Text townTextName = TextSerializers.FORMATTING_CODE.deserialize(townName.replace("\\.", ""));
                GriefPreventionPlugin.instance.getLogger().info(padding);
                GriefPreventionPlugin.instance.getLogger().info("Scanning town " + townTextName.toPlain() + "....");
                UUID worldUniqueId = null;
                TreeMap<Integer, List<Integer>> chunkMap = new TreeMap<>();
                for (Entry<Object, ? extends CommentedConfigurationNode> worldEntry : mapEntry.getValue().getChildrenMap().entrySet()) {
                    worldUniqueId = UUID.fromString((String) worldEntry.getKey());
                    if (!worldUniqueId.equals(world.getProperties().getUniqueId())) {
                        continue;
                    }
                    for (Entry<Object, ? extends CommentedConfigurationNode> chunkEntry : worldEntry.getValue().getChildrenMap().entrySet()) {
                        int chunkX = Integer.parseInt((String) chunkEntry.getKey());
                        for (Entry<Object, ? extends CommentedConfigurationNode> chunkZEntry : chunkEntry.getValue().getChildrenMap().entrySet()) {
                            boolean activeChunk = chunkZEntry.getValue().getBoolean();
                            if (!activeChunk) {
                                continue;
                            }
                            int chunkZ = Integer.parseInt((String) chunkZEntry.getKey());
                            List<Integer> chunkZList = chunkMap.get(chunkX);
                            if (chunkZList == null) {
                                chunkZList = new ArrayList<>();
                                chunkZList.add(chunkZ);
                                chunkMap.put(chunkX, chunkZList);
                            } else {
                                chunkZList.add(chunkZ);
                            }
                        }
                    }
                }

                if (chunkMap.isEmpty()) {
                    GriefPreventionPlugin.instance.getLogger().info("Skipping abandoned town " + townTextName.toPlain() + ". No active claims found.");
                    skippedCount++;
                    GriefPreventionPlugin.instance.getLogger().info(padding);
                    continue;
                }

                Vector3i lesserBoundaryCorner = null;
                Vector3i greaterBoundaryCorner = null;
                List<ChunkPos> chunkPositions = new ArrayList<>();
                for (Entry<Integer, List<Integer>> chunkList : chunkMap.entrySet()) {
                    int chunkX = chunkList.getKey();
                    Collections.sort(chunkList.getValue());
                    for (Integer chunkZ : chunkList.getValue()) {
                        chunkPositions.add(new ChunkPos(chunkX, chunkZ));
                    }
                }

                Iterator<ChunkPos> iterator = chunkPositions.iterator();
                List<ChunkPos> currentChunkGroup = new ArrayList<>();
                while (iterator.hasNext()) {
                    ChunkPos chunkPos = iterator.next();
                    if (!currentChunkGroup.isEmpty()) {
                        boolean isNeighbor = false;
                        Vector3i chunkVec = new Vector3i(chunkPos.chunkXPos, 0, chunkPos.chunkZPos);
                        for (ChunkPos currentChunk : currentChunkGroup) {
                            for (Direction direction : CARDINAL_DIRECTIONS) {
                                Vector3i currentChunkVec = new Vector3i(currentChunk.chunkXPos, 0, currentChunk.chunkZPos);
                                Vector3i neighborVec = currentChunkVec.add(direction.asBlockOffset());
                                if (neighborVec.equals(chunkVec)) {
                                    isNeighbor = true;
                                    break;
                                }
                            }
                            if (isNeighbor) {
                                break;
                            }
                        }
                        if (!isNeighbor) {
                            createClaim(teamsRoot, world, townName, lesserBoundaryCorner, greaterBoundaryCorner, currentChunkGroup);
                            currentChunkGroup.clear();
                            lesserBoundaryCorner = null;
                            greaterBoundaryCorner = null;
                        }
                    }
                    int blockXStart = chunkPos.getXStart();
                    int blockZStart = chunkPos.getZStart();
                    int blockXEnd = chunkPos.getXEnd();
                    int blockZEnd = chunkPos.getZEnd();
                    if (lesserBoundaryCorner == null) {
                        lesserBoundaryCorner = new Vector3i(blockXStart, 0, blockZStart);
                        greaterBoundaryCorner = new Vector3i(blockXEnd, 255, blockZEnd);
                        currentChunkGroup.add(chunkPos);
                        iterator.remove();
                        continue;
                    }

                    int smallX, smallZ, bigX, bigZ;
                    if (blockXStart < lesserBoundaryCorner.getX()) {
                        smallX = blockXStart;
                    } else {
                        smallX = lesserBoundaryCorner.getX();
                    }
                    if (blockZStart < lesserBoundaryCorner.getZ()) {
                        smallZ = blockZStart;
                    } else {
                        smallZ = lesserBoundaryCorner.getZ();
                    }
                    if (blockXEnd > greaterBoundaryCorner.getX()) {
                        bigX = blockXEnd;
                    } else {
                        bigX = greaterBoundaryCorner.getX();
                    }
                    if (blockZEnd > greaterBoundaryCorner.getZ()) {
                        bigZ = blockZEnd;
                    } else {
                        bigZ = greaterBoundaryCorner.getZ();
                    }

                    lesserBoundaryCorner = new Vector3i(smallX, 0, smallZ);
                    greaterBoundaryCorner = new Vector3i(bigX, 255, bigZ);
                    currentChunkGroup.add(chunkPos);
                    iterator.remove();
                    if (!iterator.hasNext()) {
                        createClaim(teamsRoot, world, townName, lesserBoundaryCorner, greaterBoundaryCorner, currentChunkGroup);
                    }
                }
            }
            GriefPreventionPlugin.instance.getLogger().info("Finished Polis data migration for world '" + world.getProperties().getWorldName() + "'.");
            GriefPreventionPlugin.instance.getLogger().info("Skipped a total of " + skippedCount + " claims.");
            GriefPreventionPlugin.instance.getLogger().info("Migrated a total of " + successfulCount + " claims.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createClaim(CommentedConfigurationNode teamsRoot, World world, String townName, Vector3i lesserBoundaryCorner, Vector3i greaterBoundaryCorner, List<ChunkPos> currentChunkGroup) {
        UUID townLeader = null;
        Text townTextName = TextSerializers.FORMATTING_CODE.deserialize(townName.replace("\\.", ""));
        GriefPreventionPlugin.instance.getLogger().info("Migrating town " + townTextName.toPlain() + "'s chunk group " + currentChunkGroup + " to GriefPrevention...");
        try {
            townLeader = UUID.fromString(getLeader(teamsRoot, townName));
        } catch (IllegalArgumentException e) {
            GriefPreventionPlugin.instance.getLogger().error("Could not parse leader UUID for town " + townTextName.toPlain());
            e.printStackTrace();
        }

        GriefPreventionPlugin.instance.getLogger().info("Found leader " + townLeader);
        GriefPreventionPlugin.instance.getLogger().info("Found members " + getMembers(teamsRoot, townName));
        GriefPreventionPlugin.instance.getLogger().info("Found executives " + getExecutives(teamsRoot, townName));
        GriefPreventionPlugin.instance.getLogger().info("lesser = " + lesserBoundaryCorner + ", greater = " + greaterBoundaryCorner);
        ClaimResult claimResult = Claim.builder()
                .bounds(lesserBoundaryCorner, greaterBoundaryCorner)
                .owner(townLeader).type(ClaimType.BASIC)
                .world(world)
                .cause(GriefPreventionPlugin.pluginCause)
                .build();

        Claim claim = claimResult.getClaim().get();
        // convert town name to proper text
        if (!claimResult.successful()) {
            skippedCount++;
            GriefPreventionPlugin.instance.getLogger().info("Skipping town creation " + townTextName.toPlain() + ". Found overlap town " + claim.getName().orElse(Text.of("unknown")) + " with UUID " + claim.getUniqueId());
            GriefPreventionPlugin.instance.getLogger().info(padding);
            return;
        }
        claim.getData().setName(townTextName);
        GriefPreventionPlugin.instance.getLogger().info("Created claim " + claim.getUniqueId() + " with name " + townTextName.toPlain());

        // migrate members and executives
        List<String> members = getMembers(teamsRoot, townName);
        for (String member : members) {
            if (member.isEmpty()) {
                continue;
            }
            UUID memberUniqueId = UUID.fromString(member);
            claim.addTrust(memberUniqueId, TrustType.BUILDER, GriefPreventionPlugin.pluginCause);
        }
        List<String> executives = getExecutives(teamsRoot, townName);
        for (String executive : executives) {
            if (executive.isEmpty()) {
                continue;
            }
            UUID executiveUniqueId = UUID.fromString(executive);
            claim.addTrust(executiveUniqueId, TrustType.BUILDER, GriefPreventionPlugin.pluginCause);
        }

        // migrate balances
        EconomyService economyService = GriefPreventionPlugin.instance.economyService.orElse(null);
        if (economyService != null && !ECONOMY_TRANSFERS.contains(townLeader)) {
            UniqueAccount account = economyService.getOrCreateAccount(townLeader).orElse(null);
            if (account != null) {
                BigDecimal amount = getBalance(teamsRoot, townName);
                if (amount.intValue() != 0) {
                    GriefPreventionPlugin.instance.getLogger().info("Transferring $" + amount + " town balance into leader " + CommandHelper.lookupPlayerName(townLeader) + "'s account.");
                    account.deposit(economyService.getDefaultCurrency(), getBalance(teamsRoot, townName), GriefPreventionPlugin.pluginCause);
                }
            }
            ECONOMY_TRANSFERS.add(townLeader);
        }
        claim.getData().setDateLastActive(Instant.now());
        GPClaimManager claimManager = GriefPreventionPlugin.instance.dataStore.getClaimWorldManager(world.getProperties());
        claimManager.addClaim(claim, true);
        GriefPreventionPlugin.instance.getLogger().info("Successfully migrated " + townTextName.toPlain() + "'s chunk group " + currentChunkGroup);
        GriefPreventionPlugin.instance.getLogger().info(padding);
        successfulCount++;
    }

    // Below copied from Polis ConfigManager to grab data we need
    // https://github.com/hsyyid/Polis/blob/1.10/src/main/java/io/github/hsyyid/polis/utils/ConfigManager.java
    private static ArrayList<String> getMembers(CommentedConfigurationNode config, String teamName)
    {
        CommentedConfigurationNode valueNode = config.getNode("teams").getNode(teamName).getNode("members");

        if (valueNode.getValue() == null)
            return Lists.newArrayList();

        String list = valueNode.getString();
        ArrayList<String> membersList = Lists.newArrayList();
        boolean finished = false;

        if (finished != true)
        {
            int endIndex = list.indexOf(",");
            if (endIndex != -1)
            {
                String substring = list.substring(0, endIndex);
                membersList.add(substring);

                // If they Have More than 1
                while (finished != true)
                {
                    int startIndex = endIndex;
                    endIndex = list.indexOf(",", startIndex + 1);
                    if (endIndex != -1)
                    {
                        String substrings = list.substring(startIndex + 1, endIndex);
                        membersList.add(substrings);
                    }
                    else
                    {
                        finished = true;
                    }
                }
            }
            else
            {
                membersList.add(list);
                finished = true;
            }
        }

        return membersList;
    }

    public static ArrayList<String> getExecutives(CommentedConfigurationNode config, String teamName)
    {
        ConfigurationNode valueNode = config.getNode("teams").getNode(teamName).getNode("executives");

        if (valueNode.getValue() == null)
            return Lists.newArrayList();

        String list = valueNode.getString();
        ArrayList<String> executivesList = Lists.newArrayList();
        boolean finished = false;

        if (finished != true)
        {
            int endIndex = list.indexOf(",");

            if (endIndex != -1)
            {
                String substring = list.substring(0, endIndex);
                executivesList.add(substring);

                // If they Have More than 1
                while (finished != true)
                {
                    int startIndex = endIndex;
                    endIndex = list.indexOf(",", startIndex + 1);
                    if (endIndex != -1)
                    {
                        String substrings = list.substring(startIndex + 1, endIndex);
                        executivesList.add(substrings);
                    }
                    else
                    {
                        finished = true;
                    }
                }
            }
            else
            {
                executivesList.add(list);
                finished = true;
            }
        }

        return executivesList;
    }

    public static String getLeader(CommentedConfigurationNode config, String teamName)
    {
        ConfigurationNode valueNode = config.getNode("teams").getNode(teamName).getNode("leader");

        if (valueNode.getValue() != null)
            return valueNode.getString();
        else
            return "";
    }

    public static BigDecimal getBalance(CommentedConfigurationNode config, String teamName)
    {
        ConfigurationNode valueNode = config.getNode("teams").getNode(teamName).getNode("balance");

        if (valueNode.getValue() != null)
            return new BigDecimal(valueNode.getDouble());
        else
            return new BigDecimal(0);
    }
}
