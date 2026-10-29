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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Phantom Lattice: a lattice block torn out for a while. It is the block's ghost, faintly lit, with nothing to stand
 * on, nothing a pick can break and nothing to drop; when its time is up (a scheduled tick, saved with its chunk) it
 * re-forms as the block it was ({@link #FORM}), pushing up onto its top anyone standing in it. Only the blocks in
 * {@code #promisedend:lattice_tearable} are torn, so nothing else a player built is ever touched.
 *
 * <p>The Architect also raises it where there was nothing ({@link Form#RAISED}): a wall of lattice that stands solid
 * for a while and goes back to air.
 */
public class PhantomLatticeBlock extends Block {
    /** What it was, and re-forms as. */
    public enum Form implements StringRepresentable {
        RAW_LATTICE("raw_lattice"), LATTICE_SCAFFOLD("lattice_scaffold"), LATTICE_TILE("lattice_tile"),
        /** Raised out of the air: solid while it stands, and air again after. */
        RAISED("raised");

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
                case RAISED -> Blocks.AIR;
            };
        }

        /** The form for a lattice block, or null for anything else. */
        public static @Nullable Form of(BlockState state) {
            for (Form f : values()) {
                if (f != RAISED && state.is(f.block())) return f;
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

    /** A torn block is nothing to stand on; a raised one is a wall. */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FORM) == Form.RAISED ? Shapes.block() : Shapes.empty();
    }

    /**
     * Raises a wall block at {@code pos} for {@code ticks}, pushing up what stands there. False, and nothing changed,
     * where there is anything but air: it never takes a player's block, or the arena's.
     */
    public static boolean raise(ServerLevel level, BlockPos pos, int ticks) {
        BlockState was = level.getBlockState(pos);
        if (!was.isAir()) return false;
        BlockState raised = ModBlocks.PHANTOM_LATTICE.get().defaultBlockState().setValue(FORM, Form.RAISED);
        level.setBlock(pos, Block.pushEntitiesUp(was, raised, level, pos), Block.UPDATE_ALL);
        level.scheduleTick(pos, ModBlocks.PHANTOM_LATTICE.get(), ticks);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.01);
        return true;
    }

    /**
     * Puts it back at {@code pos} now, as its tick would (the Architect's arena, reset): a torn block re-formed, pushing
     * up what stands in it; a raised one gone. False where there is no Phantom Lattice.
     */
    public static boolean restore(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.PHANTOM_LATTICE.get())) return false;
        Form form = state.getValue(FORM);
        BlockState back = form.block().defaultBlockState();
        level.setBlock(pos, form == Form.RAISED ? back : Block.pushEntitiesUp(state, back, level, pos), Block.UPDATE_ALL);
        return true;
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
        if (state.getValue(FORM) == Form.RAISED) {
            // a wall comes down: it crumbles back into the air it was raised from
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ModBlocks.RAW_LATTICE.get().defaultBlockState()),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.05);
            if (random.nextInt(4) == 0) level.playSound(null, pos, ModSounds.LATTICE_TEAR.get(), SoundSource.BLOCKS, 0.6F, 1.2F);
            return;
        }
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
