package com.panda.promisedend.block;

import com.panda.promisedend.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Hanging Gardens' vine: a {@link HangingVineBlock} that bears Void Berries and glows while it does (light
 * {@link #LIT}), as vanilla's cave vines do. A use picks the berries; they come back, one random tick in
 * {@link #RIPEN_CHANCE}, or at once with bone meal (which lengthens a vine already bearing). One block of new vine in
 * {@link #BERRY_CHANCE} bears at once.
 */
public class VoidBerryVineBlock extends HangingVineBlock {
    public static final BooleanProperty BERRIES = BooleanProperty.create("berries");
    public static final int LIT = 10, RIPEN_CHANCE = 6, BERRY_CHANCE = 3;

    public VoidBerryVineBlock(TagKey<Block> anchor, Properties properties) {
        super(anchor, properties);
        registerDefaultState(stateDefinition.any().setValue(BERRIES, false));
    }

    public static int light(BlockState state) {
        return state.getValue(BERRIES) ? LIT : 0;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BERRIES);
    }

    @Override
    public BlockState grown(RandomSource random) {
        return defaultBlockState().setValue(BERRIES, random.nextInt(BERRY_CHANCE) == 0);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(BERRIES)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            Block.popResource(level, pos, new ItemStack(ModItems.VOID_BERRIES.get()));
            level.playSound(null, pos, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + level.getRandom().nextFloat() * 0.4F);
            level.setBlock(pos, state.setValue(BERRIES, false), Block.UPDATE_ALL);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isValidBonemealTarget(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state, net.minecraft.world.level.block.BonemealSource source) {
        return !state.getValue(BERRIES) || super.isValidBonemealTarget(level, pos, state, source);
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, net.minecraft.world.level.block.BonemealSource source) {
        if (!state.getValue(BERRIES)) level.setBlock(pos, state.setValue(BERRIES, true), Block.UPDATE_ALL);
        else super.performBonemeal(level, random, pos, state, source);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(BERRIES) && random.nextInt(RIPEN_CHANCE) == 0) {
            level.setBlock(pos, state.setValue(BERRIES, true), Block.UPDATE_ALL);
            return;
        }
        super.randomTick(state, level, pos, random);
    }
}
