package com.panda.promisedend.block;

import com.panda.promisedend.logic.Updraft;
import com.panda.promisedend.registry.ModBlockEntities;
import com.panda.promisedend.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * An Updraft Vent: a column of air rises over it, {@link Updraft#HEIGHT_ANCIENT} blocks over an ancient one (found on
 * the islands) and {@link Updraft#HEIGHT_CRAFTED} over one a player made, stopped by the first full block in it. Its
 * block entity does the lifting ({@link UpdraftVentBlockEntity}); the wind is heard within about 16 blocks.
 */
public class UpdraftVentBlock extends Block implements EntityBlock {
    public static final BooleanProperty ANCIENT = BooleanProperty.create("ancient");
    /**
     * One animateTick call in this many plays the wind. The client asks blocks within 16 about 0.16 times a tick 2
     * blocks off and 0.05 at 12 (see ResoniteClusterBlock), so at one in 8 the wind sounds about every 5 s beside a
     * vent and every 16 s at 12 blocks: a soft, uneven loop.
     */
    public static final int WIND_ONE_IN = 8;

    public UpdraftVentBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ANCIENT, false));
    }

    public static int height(BlockState state) {
        return state.getValue(ANCIENT) ? Updraft.HEIGHT_ANCIENT : Updraft.HEIGHT_CRAFTED;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ANCIENT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UpdraftVentBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.UPDRAFT_VENT.get() ? (l, p, s, be) -> UpdraftVentBlockEntity.tick(l, p, s, (UpdraftVentBlockEntity) be) : null;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(WIND_ONE_IN) != 0) return;
        // a full block on the vent shuts its column: no air, no wind
        if (!(level.getBlockEntity(pos) instanceof UpdraftVentBlockEntity vent) || vent.height() == 0) return;
        level.playLocalSound(pos, ModSounds.UPDRAFT_WIND.get(), SoundSource.BLOCKS, 0.35F, 0.8F + random.nextFloat() * 0.3F, false);
    }
}
