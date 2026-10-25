package com.panda.promisedend.registry;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.block.UpdraftVentBlockEntity;
import com.panda.promisedend.platform.registry.DeferredRegister;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** The mod's block entities. */
public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, PromisedEnd.MODID);

    public static final Supplier<BlockEntityType<UpdraftVentBlockEntity>> UPDRAFT_VENT = TYPES.register("updraft_vent",
            () -> new BlockEntityType<>(UpdraftVentBlockEntity::new, Set.of(ModBlocks.UPDRAFT_VENT.get())));

    private ModBlockEntities() {}
}
