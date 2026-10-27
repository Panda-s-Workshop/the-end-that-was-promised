package com.panda.promisedend.block;

import com.panda.promisedend.registry.ModParticles;
import com.panda.promisedend.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

/**
 * An Echo Amber Cluster: amber grown on a cavern's walls in the Hollow Highlands, the caves' only light ({@link #DIM}).
 * Hit, used, stood on or struck by a projectile, it answers as redstone ore does: it flares to {@link #BRIGHT} with a
 * soft light-coming-on sound and a burst of motes, and dims again {@link #FLARE_TICKS} ticks after the last touch.
 * Flared, it holds the caves' dark off round it ({@code logic/CaveDark}); dim, it doesn't. It breaks as amethyst's
 * cluster does (its loot table): Echo Amber, more with a pickaxe and Fortune; Silk Touch keeps the cluster.
 */
public class EchoAmberClusterBlock extends AmethystClusterBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** Its light, dim and flared. */
    public static final int DIM = 5, BRIGHT = 11;
    /** How long a flare lasts. */
    public static final int FLARE_TICKS = 100;
    /** One client tick in this many a dim cluster lets a mote go; a flared one, every few. */
    public static final int MOTE_ONE_IN = 40, FLARE_MOTE_ONE_IN = 3;
    /**
     * When each lit cluster was last touched (server only). The tick scheduler keeps the first tick asked for a block, so
     * a touch while lit can't put the dimming off; the dimming tick reads this and waits out the rest. A restart forgets
     * it, and the cluster dims at its next tick.
     */
    private static final java.util.Map<net.minecraft.core.GlobalPos, Long> TOUCHED = new java.util.concurrent.ConcurrentHashMap<>();

    public EchoAmberClusterBlock(Properties properties) {
        super(7.0F, 3.0F, properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    /** The light a state gives: for the block's properties. */
    public static int light(BlockState state) {
        return state.getValue(LIT) ? BRIGHT : DIM;
    }

    /** Forgets every touch: a new server's game time is another clock. */
    public static void forget() {
        TOUCHED.clear();
    }

    /** Lights it (or keeps it lit) for another {@link #FLARE_TICKS}; the sound only when it was dim. */
    public static void flare(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide() || !(state.getBlock() instanceof EchoAmberClusterBlock block)) return;
        TOUCHED.put(net.minecraft.core.GlobalPos.of(level.dimension(), pos.immutable()), level.getGameTime());
        if (!state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
            level.playSound(null, pos, ModSounds.ECHO_AMBER_FLARE.get(), SoundSource.BLOCKS, 0.7F, 0.8F + level.getRandom().nextFloat() * 0.3F);
        }
        level.scheduleTick(pos, block, FLARE_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        var at = net.minecraft.core.GlobalPos.of(level.dimension(), pos.immutable());
        Long touched = TOUCHED.get(at);
        long left = touched == null ? 0 : touched + FLARE_TICKS - level.getGameTime();
        if (left > 0) {
            level.scheduleTick(pos, this, (int) left);
            return;
        }
        TOUCHED.remove(at);
        if (state.getValue(LIT)) level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        TOUCHED.remove(net.minecraft.core.GlobalPos.of(level.dimension(), pos.immutable()));
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        flare(level, pos, state);
        super.attack(state, level, pos, player);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!entity.isSteppingCarefully()) flare(level, pos, state);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        flare(level, pos, state);
        // an empty hand's touch is the use, with its swing; a held block still places past it, as on redstone ore
        return stack.isEmpty() ? InteractionResult.SUCCESS : super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        flare(level, hit.getBlockPos(), state);
        super.onProjectileHit(level, state, hit, projectile);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(state.getValue(LIT) ? FLARE_MOTE_ONE_IN : MOTE_ONE_IN) != 0) return;
        // off the cluster's tip, the way it points
        Direction facing = state.getValue(FACING);
        double x = pos.getX() + 0.5 + facing.getStepX() * 0.3 + (random.nextDouble() - 0.5) * 0.5;
        double y = pos.getY() + 0.5 + facing.getStepY() * 0.3 + (random.nextDouble() - 0.5) * 0.5;
        double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.3 + (random.nextDouble() - 0.5) * 0.5;
        level.addParticle(ModParticles.AMBER_MOTE.get(), x, y, z, 0, 0, 0);
    }
}
