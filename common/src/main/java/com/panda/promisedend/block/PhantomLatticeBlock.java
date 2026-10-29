package com.panda.promisedend.block;

import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModSounds;
import com.panda.promisedend.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jspecify.annotations.Nullable;

/**
 * Phantom Lattice: a lattice block torn out for a while. It is the block's ghost, faintly lit, with nothing to stand
 * on, nothing a pick can break and nothing to drop; when its time is up (a scheduled tick, saved with its chunk) it
 * re-forms as the block it was ({@link #FORM}), pushing up onto its top anyone standing in it. Only the blocks in
 * {@code #promisedend:lattice_tearable} are torn, so nothing else a player built is ever touched.
 */
public class PhantomLatticeBlock extends Block {
    /** What it was, and re-forms as. */
    public enum Form implements StringRepresentable {
        RAW_LATTICE("raw_lattice"), LATTICE_SCAFFOLD("lattice_scaffold"), LATTICE_TILE("lattice_tile");

        private final String name;

        Form(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public Block block() {
            return switch (this) {
                case RAW_LATTICE -> ModBlocks.RAW_LATTICE.get();
                case LATTICE_SCAFFOLD -> ModBlocks.LATTICE_SCAFFOLD.get();
                case LATTICE_TILE -> ModBlocks.LATTICE_TILE.get();
            };
        }

        /** The form for a lattice block, or null for anything else. */
        public static @Nullable Form of(BlockState state) {
            for (Form f : values()) {
                if (state.is(f.block())) return f;
            }
            return null;
        }
    }

    public static final EnumProperty<Form> FORM = EnumProperty.create("form", Form.class);
    /** Ticks a torn block stays torn: 8 s. */
    public static final int REFORM = 160;

    public PhantomLatticeBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORM, Form.RAW_LATTICE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORM);
    }

    /**
     * Tears out the lattice block at {@code pos} for {@code ticks}: it becomes its phantom, with a crack and a puff of
     * its grit. False, and nothing changed, when the block there isn't one that tears.
     */
    public static boolean tear(ServerLevel level, BlockPos pos, int ticks) {
        BlockState state = level.getBlockState(pos);
        Form form = state.is(ModTags.LATTICE_TEARABLE) ? Form.of(state) : null;
        if (form == null) return false;
        level.setBlock(pos, ModBlocks.PHANTOM_LATTICE.get().defaultBlockState().setValue(FORM, form), Block.UPDATE_ALL);
        level.scheduleTick(pos, ModBlocks.PHANTOM_LATTICE.get(), ticks);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                24, 0.3, 0.3, 0.3, 0.1);
        level.playSound(null, pos, ModSounds.LATTICE_TEAR.get(), SoundSource.BLOCKS, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
        return true;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState back = state.getValue(FORM).block().defaultBlockState();
        level.setBlock(pos, Block.pushEntitiesUp(state, back, level, pos), Block.UPDATE_ALL);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.01);
        level.playSound(null, pos, ModSounds.LATTICE_REFORM.get(), SoundSource.BLOCKS, 0.8F, 0.9F + random.nextFloat() * 0.2F);
    }

    /** One left without its tick (set by a command, or its tick lost) re-forms at a random tick, so none stays a ghost. */
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) tick(state, level, pos, random);
    }
}
