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
package me.ryanhamshire.GriefPrevention;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.BlockPos;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.entity.living.player.Player;

public class BlockUtils {

    public static void sendBlockChange(Player player, BlockSnapshot snapshot) {
        EntityPlayerMP playermp = (EntityPlayerMP) player;
        if (playermp.playerNetServerHandler == null) {
            return;
        }

        S23PacketBlockChange packet = new S23PacketBlockChange((net.minecraft.world.World) snapshot.getLocation().get().getExtent(),
                new BlockPos(snapshot.getPosition().getX(), snapshot.getPosition().getY(), snapshot.getPosition().getZ()));

        packet.blockState = (IBlockState) snapshot.getState();
        playermp.playerNetServerHandler.sendPacket(packet);
    }
}
