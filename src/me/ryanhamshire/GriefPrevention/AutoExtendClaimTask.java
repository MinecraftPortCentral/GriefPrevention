package me.ryanhamshire.GriefPrevention;

import org.spongepowered.api.world.Chunk;
import org.spongepowered.api.world.DimensionType;
import org.spongepowered.api.world.biome.BiomeType;

import java.util.ArrayList;

//automatically extends a claim downward based on block types detected
class AutoExtendClaimTask implements Runnable {

    private Claim claim;
    private ArrayList<Chunk> chunks;
    private DimensionType worldType;

    public AutoExtendClaimTask(Claim claim, ArrayList<Chunk> chunks, DimensionType worldType) {
        this.claim = claim;
        this.chunks = chunks;
        this.worldType = worldType;
    }

    @Override
    public void run() {
        int newY = this.getLowestBuiltY();
        if (newY < this.claim.getLesserBoundaryCorner().getBlockY()) {
            GriefPrevention.instance.getGame().getScheduler().createTaskBuilder().execute(new ExecuteExtendClaimTask(claim, newY))
                    .submit(GriefPrevention.instance);
        }
    }

    private int getLowestBuiltY() {
        int y = this.claim.getLesserBoundaryCorner().getBlockY();

        if (this.yTooSmall(y))
            return y;

        for (Chunk chunk : this.chunks) {
            BiomeType biome = chunk.getBiome(0, 0);
            ArrayList<Integer> playerBlockIDs = RestoreNatureProcessingTask.getPlayerBlocks(this.worldType, biome);

            boolean ychanged = true;
            while (!this.yTooSmall(y) && ychanged) {
                ychanged = false;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int blockType = chunk.getBlockTypeId(x, y, z);
                        while (!this.yTooSmall(y) && playerBlockIDs.contains(blockType)) {
                            ychanged = true;
                            blockType = chunk.getBlockTypeId(x, --y, z);
                        }

                        if (this.yTooSmall(y))
                            return y;
                    }
                }
            }

            if (this.yTooSmall(y))
                return y;
        }

        return y;
    }

    private boolean yTooSmall(int y) {
        return y == 0 || y <= GriefPrevention.instance.config_claims_maxDepth;
    }

    // runs in the main execution thread, where it can safely change claims and
    // save those changes
    private class ExecuteExtendClaimTask implements Runnable {

        private Claim claim;
        private int newY;

        public ExecuteExtendClaimTask(Claim claim, int newY) {
            this.claim = claim;
            this.newY = newY;
        }

        @Override
        public void run() {
            GriefPrevention.instance.dataStore.extendClaim(claim, newY);
        }
    }

}
