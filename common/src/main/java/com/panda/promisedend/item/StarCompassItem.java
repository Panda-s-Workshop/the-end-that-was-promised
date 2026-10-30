package com.panda.promisedend.item;

import com.panda.promisedend.logic.Starfall;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;

/**
 * The Star Compass: a Starfall Core set in a compass. Carried, it points as a lodestone compass does at where a star is
 * falling (its beam), or else at the last crater; with neither, or outside the End, its needle spins. The target is a
 * lodestone tracker that isn't tracked, so vanilla's compass model reads it and nothing clears it for want of a lodestone.
 */
public class StarCompassItem extends HintItem {
    /** It looks again this often. */
    public static final int LOOK_EVERY = 20;

    public StarCompassItem(Properties properties, String name, int lines) {
        super(properties, name, lines);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (level.getGameTime() % LOOK_EVERY == 0) aim(stack, level);
    }

    /** Points {@code stack} where the Starfall says, or at nothing (public for GameTests). */
    public static void aim(ItemStack stack, ServerLevel level) {
        Optional<GlobalPos> at = Starfall.aim(level.getServer()).map(p -> GlobalPos.of(Level.END, p));
        LodestoneTracker now = stack.get(DataComponents.LODESTONE_TRACKER);
        if (at.isEmpty()) {
            if (now != null) stack.remove(DataComponents.LODESTONE_TRACKER);
        } else if (now == null || !now.target().equals(at)) {
            stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(at, false));
        }
    }
}
