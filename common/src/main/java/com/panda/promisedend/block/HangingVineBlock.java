package com.panda.promisedend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A vine that hangs: from a block of its {@code anchor} tag, or from more of itself, down into the air. It is climbable
 * ({@code #minecraft:climbable}), breaks when what holds it goes, and now and then grows a block longer (one random tick
 * in {@link #GROW_CHANCE}), or at once with bone meal, to {@link #MAX_LENGTH} at most. The Chorus Vine hangs under the
 * Chorus Canopy's leaves, the Void Berry Vine under the Hanging Gardens' islets.
 */
public class HangingVineBlock extends Block implements BonemealableBlock {
    private static final VoxelShape SHAPE = Block.column(10, 0, 16);
    public static final int MAX_LENGTH = 12, GROW_CHANCE = 12;
    private final TagKey<Block> anchor;

    public HangingVineBlock(TagKey<Block> anchor, Properties properties) {
        super(properties);
        this.anchor = anchor;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return above.is(this) || above.is(anchor);
    }

    /** What holds it gone, it breaks on the next tick and drops what it drops, as vanilla's vines do. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == Direction.UP && !canSurvive(state, level, pos)) ticks.scheduleTick(pos, this, 1);
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) level.destroyBlock(pos, true);
    }

    /** How many blocks of vine hang from the anchor down to {@code pos}, counting it. */
    public int length(BlockGetter level, BlockPos pos) {
        int n = 1;
        while (n <= MAX_LENGTH && level.getBlockState(pos.above(n)).is(this)) n++;
        return n;
    }

    /** The bottom block of the vine {@code pos} is in. */
    private BlockPos tip(BlockGetter level, BlockPos pos) {
        BlockPos p = pos;
        while (level.getBlockState(p.below()).is(this)) p = p.below();
        return p;
    }

    /** The state a new block of vine takes: this block's own. */
    public BlockState grown(RandomSource random) {
        return defaultBlockState();
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(GROW_CHANCE) == 0 && level.getBlockState(pos.below()).isAir() && length(level, pos) < MAX_LENGTH) {
            level.setBlock(pos.below(), grown(random), Block.UPDATE_ALL);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        BlockPos tip = tip(level, pos);
        return level.getBlockState(tip.below()).isAir() && length(level, tip) < MAX_LENGTH;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        BlockPos tip = tip(level, pos);
        if (level.getBlockState(tip.below()).isAir()) level.setBlock(tip.below(), grown(random), Block.UPDATE_ALL);
    }
}
