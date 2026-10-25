package com.panda.promisedend.block;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.registry.ModTags;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Stripped chorus timber: put in place of a block in {@code #promisedend:sheds_chorus_resin} (the chorus log and wood:
 * an axe's strip, any mod's), it drops the loot table {@code promisedend:strip/chorus_timber} (one Chorus Resin half
 * the time), popped out of a free face: the top, or in a standing trunk the side towards the nearest player. Placed
 * any other way (a player, worldgen, a command over air) it sheds none, so the Thicket's groves pay resin to the work
 * of stripping them.
 */
public class StrippedChorusBlock extends RotatedPillarBlock {
    public static final ResourceKey<LootTable> SHED = ResourceKey.create(Registries.LOOT_TABLE, PromisedEnd.id("strip/chorus_timber"));

    public StrippedChorusBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!(level instanceof ServerLevel server) || moved || !oldState.is(ModTags.SHEDS_CHORUS_RESIN)) return;
        Direction face = freeFace(server, pos);
        boolean[] shed = {false};
        Block.dropFromBlockInteractLootTable(server, SHED, pos, oldState, null, null, null, (l, stack) -> {
            Block.popResourceFromFace(l, pos, face, stack);
            shed[0] = true;
        });
        if (shed[0]) server.playSound(null, pos, SoundEvents.RESIN_BREAK, SoundSource.BLOCKS, 0.8F, 1.1F);
    }

    /** The top if nothing solid is on it; else the open side nearest the nearest player within 8; else the top. */
    static Direction freeFace(ServerLevel level, BlockPos pos) {
        if (open(level, pos.above())) return Direction.UP;
        List<Direction> sides = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
        Player near = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, false);
        if (near != null) {
            double dx = near.getX() - (pos.getX() + 0.5), dz = near.getZ() - (pos.getZ() + 0.5);
            sides.sort(Comparator.comparingDouble(d -> -(d.getStepX() * dx + d.getStepZ() * dz)));
        }
        for (Direction side : sides) {
            if (open(level, pos.relative(side))) return side;
        }
        return Direction.UP;
    }

    private static boolean open(ServerLevel level, BlockPos at) {
        return level.getBlockState(at).getCollisionShape(level, at).isEmpty();
    }
}
