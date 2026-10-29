package com.panda.promisedend.block;

import com.panda.promisedend.logic.RiftGates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Rift Gate's core, in the middle of a 3x3 frame standing upright; it faces the side a traveller steps out on. A
 * player attunes it to their network with a Mire Lily and a Resonite Shard; once they have, using it opens the screen
 * of their gates, and a named name tag renames it in their network. It lights for good once anyone has attuned it.
 */
public class RiftGateBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public RiftGateBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!RiftGates.isNameTag(stack)) return super.useItemOn(stack, state, level, pos, player, hand, hit);
        if (player instanceof ServerPlayer sp) {
            if (RiftGates.attuned(sp, pos)) RiftGates.rename(sp, pos, stack.get(DataComponents.CUSTOM_NAME).getString());
            else sp.sendOverlayMessage(Component.translatable("message.promisedend.rift_gate.dormant"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        // sneaking with an empty hand lets an attuned gate go, so a full network can make room
        if (RiftGates.attuned(sp, pos)) {
            if (sp.isShiftKeyDown()) RiftGates.release(sp, pos);
            else RiftGates.open(sp, pos);
        } else RiftGates.attune(sp, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            if (random.nextInt(6) == 0) level.addParticle(ParticleTypes.PORTAL, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0, 0, 0);
            return;
        }
        // a lit gate breathes motes out on both faces
        Direction f = state.getValue(FACING);
        for (int i = 0; i < 3; i++) {
            double side = random.nextBoolean() ? 0.55 : -0.55;
            level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5 + f.getStepX() * side + (random.nextDouble() - 0.5) * (f.getStepX() == 0 ? 2.6 : 0.1),
                    pos.getY() + 0.5 + (random.nextDouble() - 0.5) * 2.6, pos.getZ() + 0.5 + f.getStepZ() * side + (random.nextDouble() - 0.5) * (f.getStepZ() == 0 ? 2.6 : 0.1),
                    f.getStepX() * side * 0.05, 0.01, f.getStepZ() * side * 0.05);
        }
    }
}
