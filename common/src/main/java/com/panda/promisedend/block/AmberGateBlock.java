package com.panda.promisedend.block;

import com.panda.promisedend.entity.TheArchitect;
import com.panda.promisedend.registry.ModItems;
import com.panda.promisedend.registry.ModSounds;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Unfinished Citadel's Amber Gate: a wall of amber set in lattice that nothing breaks. An Amber Key used on it is
 * spent, and the whole gate (every gate block joined to that one, {@link #MOST} at most) opens for {@link #OPEN_TICKS}:
 * clear amber light you walk through. Then it closes, pushing up and out anything left standing in it. Opened, it also
 * unbinds the Architect inside, for good ({@link TheArchitect#unbind}): until then it can't be woken, however a player
 * came in. A gate opened already takes no second key. Once its Architect is unbound, the shut gate opens again to an
 * empty hand while it lives: a fight lost or run past the ten minutes needs no second key.
 */
public class AmberGateBlock extends Block {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    /** 10 minutes. */
    public static final int OPEN_TICKS = 12000;
    /** The most gate blocks one key opens. */
    public static final int MOST = 64;
    /** How far from the gate the Architect it unbinds may be (the Citadel's is 30.5 in). */
    public static final double UNBINDS = 48.0;

    public AmberGateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : super.getCollisionShape(state, level, pos, context);
    }

    /** One pane: the faces between two gate blocks aren't drawn, as between two of glass. */
    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacent, Direction direction) {
        return adjacent.is(this) || super.skipRendering(state, adjacent, direction);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.AMBER_KEY.get())) return super.useItemOn(stack, state, level, pos, player, hand, hit);
        if (state.getValue(OPEN)) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            open(server, pos);
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(OPEN) && level instanceof ServerLevel server) {
            if (awake(server, pos)) {
                open(server, pos);
                player.sendOverlayMessage(Component.translatable("message.promisedend.amber_gate.gives_way"));
            } else {
                player.sendOverlayMessage(Component.translatable("message.promisedend.amber_gate.locked"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Whether an Architect the gate at {@code pos} unbound still lives within its reach. */
    public static boolean awake(ServerLevel level, BlockPos pos) {
        return !level.getEntitiesOfClass(TheArchitect.class, new AABB(pos).inflate(UNBINDS), a -> a.isAlive() && !a.bound()).isEmpty();
    }

    /** Every gate block joined to {@code pos}, face to face, {@link #MOST} at most. */
    public static List<BlockPos> gate(Level level, BlockPos pos) {
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(pos.immutable());
        seen.add(pos.immutable());
        while (!queue.isEmpty() && out.size() < MOST) {
            BlockPos p = queue.poll();
            if (!(level.getBlockState(p).getBlock() instanceof AmberGateBlock)) continue;
            out.add(p);
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (seen.add(n)) queue.add(n);
            }
        }
        return out;
    }

    /** Opens the gate {@code pos} is part of for {@link #OPEN_TICKS} (public for GameTests). */
    public static void open(ServerLevel level, BlockPos pos) {
        for (BlockPos p : gate(level, pos)) {
            BlockState state = level.getBlockState(p);
            level.setBlock(p, state.setValue(OPEN, true), Block.UPDATE_ALL);
            level.scheduleTick(p, state.getBlock(), OPEN_TICKS);
            level.sendParticles(ParticleTypes.WAX_OFF, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.02);
        }
        level.playSound(null, pos, ModSounds.AMBER_GATE_OPEN.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
        for (TheArchitect a : level.getEntitiesOfClass(TheArchitect.class, new AABB(pos).inflate(UNBINDS))) a.unbind();
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(OPEN)) return;
        BlockState shut = state.setValue(OPEN, false);
        level.setBlock(pos, Block.pushEntitiesUp(state, shut, level, pos), Block.UPDATE_ALL);
        if (random.nextInt(8) == 0) level.playSound(null, pos, ModSounds.AMBER_GATE_CLOSE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }
}
