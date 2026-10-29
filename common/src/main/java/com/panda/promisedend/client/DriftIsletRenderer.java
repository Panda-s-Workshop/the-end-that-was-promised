package com.panda.promisedend.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.panda.promisedend.entity.DriftIslet;
import com.panda.promisedend.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * A Drift Islet drawn as blocks, the way vanilla draws a falling block: a 3x3 of end stone with Ancient purpur at its
 * corners, and an end-stone keel under its middle so it reads as a small island, not a slab. Each block is lit by the
 * light where it is.
 */
public final class DriftIsletRenderer extends EntityRenderer<DriftIslet, DriftIsletRenderer.State> {
    /** Each block: its offset from the islet's bottom middle (x, y, z), and corner or not. */
    private static final int[][] CELLS;

    static {
        List<int[]> cells = new ArrayList<>();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) cells.add(new int[] {x, 0, z, Math.abs(x) + Math.abs(z) == 2 ? 1 : 0});
        }
        cells.add(new int[] {0, -1, 0, 0});
        CELLS = cells.toArray(new int[0][]);
    }

    public static final class State extends EntityRenderState {
        final MovingBlockRenderState[] blocks = new MovingBlockRenderState[CELLS.length];

        State() {
            for (int i = 0; i < blocks.length; i++) blocks[i] = new MovingBlockRenderState();
        }
    }

    public DriftIsletRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    protected AABB getBoundingBoxForCulling(DriftIslet islet, float partialTick) {
        return islet.getBoundingBox().expandTowards(0, -1, 0);
    }

    @Override
    public void extractRenderState(DriftIslet islet, State state, float partialTick) {
        super.extractRenderState(islet, state, partialTick);
        BlockState stone = Blocks.END_STONE.defaultBlockState(), corner = ModBlocks.ANCIENT_PURPUR_SET.base().get().defaultBlockState();
        // a chiseled Voidstone heart, so a docked islet reads as a vessel and not more of the island
        BlockState heart = ModBlocks.CHISELED_VOIDSTONE.get().defaultBlockState();
        var level = (ClientLevel) islet.level();
        double x = state.x, y = state.y, z = state.z;
        for (int i = 0; i < CELLS.length; i++) {
            int[] c = CELLS[i];
            MovingBlockRenderState b = state.blocks[i];
            BlockPos at = BlockPos.containing(x + c[0], y + c[1] + 0.5, z + c[2]);
            b.blockPos = at;
            b.randomSeedPos = at;
            b.blockState = c[3] == 1 ? corner : c[0] == 0 && c[2] == 0 ? heart : stone;
            b.biome = level.getBiome(at);
            b.cardinalLighting = level.cardinalLighting();
            b.lightEngine = level.getLightEngine();
        }
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int i = 0; i < CELLS.length; i++) {
            int[] c = CELLS[i];
            pose.pushPose();
            pose.translate(c[0] - 0.5, c[1], c[2] - 0.5);
            collector.submitMovingBlock(pose, state.blocks[i], state.outlineColor);
            pose.popPose();
        }
        super.submit(state, pose, collector, camera);
    }
}
