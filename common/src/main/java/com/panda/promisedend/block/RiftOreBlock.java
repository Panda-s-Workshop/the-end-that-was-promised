package com.panda.promisedend.block;

import com.panda.promisedend.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Voidsteel Scrap Ore, in the walls of the Voidscar's rift scars: it glows rift magenta (its model's emissive layer and
 * a little light), and now and then a rift mote comes off an open face and drifts up out of the scar. Its loot table
 * gives one Voidsteel Scrap; Silk Touch keeps the ore.
 */
public class RiftOreBlock extends Block {
    /** One tick in this many, a mote comes off it (client only). */
    public static final int MOTE_CHANCE = 6;

    public RiftOreBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(MOTE_CHANCE) != 0) return;
        Direction face = Direction.getRandom(random);
        if (!level.getBlockState(pos.relative(face)).isAir()) return;
        double x = pos.getX() + 0.5 + face.getStepX() * 0.55 + (face.getStepX() == 0 ? random.nextDouble() - 0.5 : 0);
        double y = pos.getY() + 0.5 + face.getStepY() * 0.55 + (face.getStepY() == 0 ? random.nextDouble() - 0.5 : 0);
        double z = pos.getZ() + 0.5 + face.getStepZ() * 0.55 + (face.getStepZ() == 0 ? random.nextDouble() - 0.5 : 0);
        level.addParticle(ModParticles.RIFT_MOTE.get(), x, y, z, 0, 0, 0);
    }
}
