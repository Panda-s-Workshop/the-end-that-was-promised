package com.panda.promisedend.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.panda.promisedend.EndConfig;
import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.registry.ModTags;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla puts an outer gateway 10 blocks over the tallest full block within 16 of its landing spot, and a player
 * coming through it on the tallest within 5: in the Resonant Spires that is a needle up to 30 blocks over the ground.
 * When the tallest is in {@code #promisedend:not_a_gateway_exit} (Resonite), the search goes again over the columns
 * whose top block isn't in the tag; for the gateway's own search, also when the tallest is other ground but a spire
 * (one inside the search, or just past its edge) reaches into the gateway's frame over it. The exit then stands beside a spire. The gateway comes down to 10 over the ground
 * beside the spires too, so a pearl thrown from the exit still reaches it: over the tallest such column whose gateway
 * frame (3 across, 5 high) holds nothing in {@code #promisedend:gateway_frame_keep_out} (crystal or cluster), so the
 * frame never bites into a spire or breaks a cluster. With no such column (no ground beside the spires in reach, or every frame crowded), vanilla's
 * answer stands, over everything.
 *
 * <p>Where an outer gateway throws a player: vanilla's search walks its line out from the origin to the first land past
 * the void, which can be the edge of an island still in the main island's biome (inside 1024) or vanilla's small
 * islands. With our terrain on, the search goes on out along the same line, a chunk at a time, to the first land whose
 * landing spot is in one of our biomes, so the first step into the outer End is into one of our lands. Past
 * {@link #FURTHER} chunks with none, vanilla's answer stands.
 */
@Mixin(TheEndGatewayBlockEntity.class)
abstract class TheEndGatewayBlockEntityMixin {
    /** How far over the block found vanilla builds the gateway (getPortalPosition's {@code above(10)}). */
    @Unique
    private static final int PERCH = 10;
    /** How many chunks (16 blocks each) past vanilla's landing the search for one of our lands goes. */
    @Unique
    private static final int FURTHER = 32;

    @Shadow
    private static boolean isChunkEmpty(ServerLevel level, Vec3 pos) {
        throw new AssertionError();
    }

    @Shadow
    private static LevelChunk getChunk(Level level, Vec3 pos) {
        throw new AssertionError();
    }

    @Shadow
    private static BlockPos findValidSpawnInChunk(LevelChunk chunk) {
        throw new AssertionError();
    }

    @ModifyReturnValue(method = "findExitPortalXZPosTentative", at = @At("RETURN"))
    private static Vec3 promisedend$intoOurLands(Vec3 landing, ServerLevel level, BlockPos gateway) {
        if (!EndConfig.terrainEnabled() || promisedend$ours(level, landing)) return landing;
        Vec3 step = new Vec3(gateway.getX(), 0, gateway.getZ()).normalize().scale(16);
        Vec3 at = landing;
        for (int i = 0; i < FURTHER; i++) {
            at = at.add(step);
            if (promisedend$ours(level, at)) return at;
        }
        return landing;
    }

    /** Whether the chunk at {@code at} has land, and vanilla's landing spot in it is in one of our biomes. */
    @Unique
    private static boolean promisedend$ours(ServerLevel level, Vec3 at) {
        if (isChunkEmpty(level, at)) return false;
        BlockPos spot = findValidSpawnInChunk(getChunk(level, at));
        return spot != null && level.getBiome(spot).unwrapKey().map(key -> key.identifier().getNamespace().equals(PromisedEnd.MODID)).orElse(false);
    }

    @ModifyReturnValue(method = "findTallestBlock", at = @At("RETURN"))
    private static BlockPos promisedend$notOnASpire(BlockPos tallest, BlockGetter level, BlockPos centre, int radius, boolean allowBedrock) {
        if (tallest == null) return null;
        // ground off the spires stands, unless a spire beside it (inside the search or just past its edge) reaches into
        // the gateway's frame over it
        if (!level.getBlockState(tallest).is(ModTags.NOT_A_GATEWAY_EXIT)
                && (!allowBedrock || promisedend$clearFrame(level, tallest.above(PERCH)))) return tallest;
        List<BlockPos> tops = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // vanilla skips the centre column unless bedrock is allowed: the exit search skips the gateway's own
                if (dx == 0 && dz == 0 && !allowBedrock) continue;
                BlockPos top = promisedend$top(level, centre.offset(dx, 0, dz), allowBedrock);
                if (top != null && !level.getBlockState(top).is(ModTags.NOT_A_GATEWAY_EXIT)) tops.add(top);
            }
        }
        // the tallest first; on a tie, the first in vanilla's scan order (the sort is stable)
        tops.sort(Comparator.comparingInt((BlockPos p) -> p.getY()).reversed());
        for (BlockPos top : tops) {
            if (!allowBedrock || promisedend$clearFrame(level, top.above(PERCH))) return top;
        }
        return tallest;
    }

    /** Nothing in {@code #promisedend:gateway_frame_keep_out} in the gateway's frame round {@code perch}: EndGatewayFeature's box. */
    @Unique
    private static boolean promisedend$clearFrame(BlockGetter level, BlockPos perch) {
        for (BlockPos q : BlockPos.betweenClosed(perch.offset(-1, -2, -1), perch.offset(1, 2, 1))) {
            if (level.getBlockState(q).is(ModTags.GATEWAY_FRAME_KEEP_OUT)) return false;
        }
        return true;
    }

    /** The column's highest full block, as vanilla reckons it (bedrock only when allowed), or null. */
    @Unique
    private static BlockPos promisedend$top(BlockGetter level, BlockPos column, boolean allowBedrock) {
        for (int y = level.getMaxY(); y > level.getMinY(); y--) {
            BlockPos at = new BlockPos(column.getX(), y, column.getZ());
            BlockState state = level.getBlockState(at);
            if (state.isCollisionShapeFullBlock(level, at) && (allowBedrock || !state.is(Blocks.BEDROCK))) return at;
        }
        return null;
    }
}
