package com.panda.promisedend.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.panda.promisedend.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla puts an outer gateway 10 blocks over the tallest full block within 16 of its landing spot, and a player
 * coming through it on the tallest within 5: in the Resonant Spires that is a needle up to 30 blocks over the ground.
 * When the tallest is in {@code #promisedend:not_a_gateway_exit} (Resonite), the search goes again over the columns
 * whose top block isn't in the tag. The exit then stands beside a spire, never on one. The gateway comes down to 10
 * over the ground beside the spires too, so a pearl thrown from the exit still reaches it, unless a spire reaches into
 * the gateway's frame there: then it keeps vanilla's place, over everything, and its frame never bites into a spire.
 * With no such column, vanilla's answer stands.
 */
@Mixin(TheEndGatewayBlockEntity.class)
abstract class TheEndGatewayBlockEntityMixin {
    /** How far over the block found vanilla builds the gateway (getPortalPosition's {@code above(10)}). */
    @Unique
    private static final int PERCH = 10;

    @ModifyReturnValue(method = "findTallestBlock", at = @At("RETURN"))
    private static BlockPos promisedend$notOnASpire(BlockPos tallest, BlockGetter level, BlockPos centre, int radius, boolean allowBedrock) {
        if (tallest == null || !level.getBlockState(tallest).is(ModTags.NOT_A_GATEWAY_EXIT)) return tallest;
        BlockPos best = null;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // vanilla skips the centre column unless bedrock is allowed: the exit search skips the gateway's own
                if (dx == 0 && dz == 0 && !allowBedrock) continue;
                BlockPos top = promisedend$top(level, centre.offset(dx, 0, dz), allowBedrock);
                if (top == null || level.getBlockState(top).is(ModTags.NOT_A_GATEWAY_EXIT)) continue;
                if (best == null || top.getY() > best.getY()) best = top;
            }
        }
        if (best == null) return tallest;
        // the gateway's search (the one that takes bedrock): its frame, 3 across and 5 high round the perch, clear of spires
        if (allowBedrock) {
            BlockPos perch = best.above(PERCH);
            for (BlockPos q : BlockPos.betweenClosed(perch.offset(-1, -2, -1), perch.offset(1, 2, 1))) {
                if (level.getBlockState(q).is(ModTags.NOT_A_GATEWAY_EXIT)) return tallest;
            }
        }
        return best;
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
