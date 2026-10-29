package com.panda.promisedend.block;

import com.panda.promisedend.logic.Offerings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A Shrine Altar, found in the Void Shrines: lay an offering on it ({@code #promisedend:offerings}) and at three favour
 * it offers three of the six boons. It can't be taken away (it drops nothing), so the boons are a shrine's, not a
 * base's. Its block entity keeps each player's favour, offer and wait.
 */
public class ShrineAltarBlock extends Block implements EntityBlock {
    public ShrineAltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShrineAltarBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (ShrineAltarBlockEntity.worth(stack) == 0) return super.useItemOn(stack, state, level, pos, player, hand, hit);
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof ShrineAltarBlockEntity altar) altar.offer(sp, stack);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(player instanceof ServerPlayer sp) || !(level.getBlockEntity(pos) instanceof ShrineAltarBlockEntity altar)) return InteractionResult.SUCCESS;
        long wait = altar.waitLeft(sp);
        if (!altar.offered(sp.getUUID()).isEmpty()) altar.send(sp);
        else if (wait > 0) sp.sendOverlayMessage(Component.translatable("message.promisedend.shrine.resting", Math.max(1, (wait + 1199) / 1200)));
        else {
            sp.sendOverlayMessage(Component.translatable("message.promisedend.shrine.asks", altar.favour(sp.getUUID()), Offerings.ASK));
            sp.sendSystemMessage(offerings());
        }
        return InteractionResult.SUCCESS;
    }

    /** What the altar takes, read from the favour tags (a pack's own offerings among them), most favour first. */
    public static Component offerings() {
        var line = Component.translatable("message.promisedend.shrine.offerings").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);
        boolean first = true;
        for (int n = Offerings.MOST; n >= 1; n--) {
            for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM.getTagOrEmpty(com.panda.promisedend.registry.ModTags.favour(n))) {
                line.append(Component.literal(first ? " " : ", ").withStyle(net.minecraft.ChatFormatting.GRAY))
                        .append(new ItemStack(item).getHoverName().copy().withStyle(net.minecraft.ChatFormatting.GRAY))
                        .append(Component.literal(" " + n).withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
                first = false;
            }
        }
        return line;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) return;
        level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 1.05, pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.03, 0);
    }
}
