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
        if (playermp.playerNetServerHandler == null)
            return;

        S23PacketBlockChange packet = new S23PacketBlockChange((net.minecraft.world.World) snapshot.getLocation().get().getExtent(),
                new BlockPos(snapshot.getPosition().getX(), snapshot.getPosition().getY(), snapshot.getPosition().getZ()));

        packet.blockState = (IBlockState) snapshot.getState();
        playermp.playerNetServerHandler.sendPacket(packet);
    }
}
