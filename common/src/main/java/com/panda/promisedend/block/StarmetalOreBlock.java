package com.panda.promisedend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Starmetal Ore, in a fallen star's crater: end stone with the star's metal in it, its flecks lit (its model's emissive
 * layer and a little light), and now and then a spark comes off an open face. Its loot table gives Starmetal, as raw
 * gold's ore does (Fortune counts); Silk Touch keeps the ore.
 */
public class StarmetalOreBlock extends Block {
    /** One tick in this many, a spark comes off it (client only). */
    public static final int SPARK_CHANCE = 10;

    public StarmetalOreBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(SPARK_CHANCE) != 0) return;
        Direction face = Direction.getRandom(random);
        if (!level.getBlockState(pos.relative(face)).isAir()) return;
        double x = pos.getX() + 0.5 + face.getStepX() * 0.55 + (face.getStepX() == 0 ? random.nextDouble() - 0.5 : 0);
        double y = pos.getY() + 0.5 + face.getStepY() * 0.55 + (face.getStepY() == 0 ? random.nextDouble() - 0.5 : 0);
        double z = pos.getZ() + 0.5 + face.getStepZ() * 0.55 + (face.getStepZ() == 0 ? random.nextDouble() - 0.5 : 0);
        level.addParticle(ParticleTypes.END_ROD, x, y, z, 0, 0.02, 0);
    }
}
