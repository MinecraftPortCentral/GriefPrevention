package me.ryanhamshire.GriefPrevention;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S23PacketBlockChange;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.common.util.VecHelper;

public class BlockUtils {

    public static void sendBlockChange(Player player, BlockSnapshot snapshot) {
        EntityPlayerMP playermp = (EntityPlayerMP) player;
        if (playermp.playerNetServerHandler == null)
            return;

        S23PacketBlockChange packet = new S23PacketBlockChange((net.minecraft.world.World) snapshot.getLocation().get().getExtent(),
                VecHelper.toBlockPos(snapshot.getPosition()));

        packet.blockState = (IBlockState) snapshot.getState();
        playermp.playerNetServerHandler.sendPacket(packet);
    }
}
