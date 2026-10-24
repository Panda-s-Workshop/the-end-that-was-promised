package com.panda.promisedend.item;

import com.panda.promisedend.PromisedEnd;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Opens the Promise Codex on the client. The book reads the player's advancements; nothing is sent. */
public class PromiseCodexItem extends Item {
    public PromiseCodexItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) PromisedEnd.openCodex.run();
        return InteractionResult.SUCCESS;
    }
}
