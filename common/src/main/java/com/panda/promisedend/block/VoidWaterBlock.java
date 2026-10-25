package com.panda.promisedend.block;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Void-water, the Lumen Mire's still pools. Not a fluid: a block drawn as one, that never flows and
 * has no bucket or item. You wade through it (no collision), and while you're in it you have Slowness I. As with
 * water, a block placed into it takes its place and the cursor goes through it (flowing fluids don't: it isn't in
 * {@code #minecraft:washed_away_by_fluids}), except for a player holding what grows
 * on it ({@code #promisedend:plants_on_void_water}, the Mire Lily), who sees its surface to plant on.
 */
public class VoidWaterBlock extends Block {
    /** Its surface, two pixels down as vanilla water's is: what a player holding a Mire Lily clicks. */
    private static final VoxelShape SURFACE = Block.box(0, 0, 0, 16, 14, 16);
    /** Slowness I for this long, given again once half of it has run out while the entity is still in. */
    public static final int SLOW_TICKS = 40;

    public VoidWaterBlock(Properties properties) {
        super(properties);
    }

    /** What a player plants on the surface, in either hand. */
    public static final TagKey<Item> PLANTS_ON = TagKey.create(Registries.ITEM, PromisedEnd.id("plants_on_void_water"));

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // a tag, not the lily itself: Fabric builds every block's shape cache while blocks are still registering
        return context instanceof EntityCollisionContext e && e.getEntity() instanceof LivingEntity holder
                && (holder.getMainHandItem().is(PLANTS_ON) || holder.getOffhandItem().is(PLANTS_ON)) ? SURFACE : Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        // a lily goes on the surface, not into the pool
        return !context.getItemInHand().is(PLANTS_ON);
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, direction);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean intersects) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living)) return;
        MobEffectInstance slow = living.getEffect(MobEffects.SLOWNESS);
        // stepping in (no Slowness yet, since it's topped up while you stay): a dull splash and the void's motes
        if (slow == null && level instanceof ServerLevel server) {
            server.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ModSounds.VOID_WATER_ENTER.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            server.sendParticles(ParticleTypes.REVERSE_PORTAL, entity.getX(), pos.getY() + 0.85, entity.getZ(), 8, 0.3, 0.05, 0.3, 0.02);
        }
        if (slow == null || slow.getAmplifier() == 0 && slow.getDuration() < SLOW_TICKS / 2) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOW_TICKS, 0, true, true));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // now and then a mote of the void rises off the surface
        if (random.nextInt(14) != 0 || !level.getBlockState(pos.above()).isAir()) return;
        level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + random.nextDouble(), pos.getY() + 0.9, pos.getZ() + random.nextDouble(),
                0.0, 0.02, 0.0);
    }
}
