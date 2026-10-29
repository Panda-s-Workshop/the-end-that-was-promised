package com.panda.promisedend.registry;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.platform.registry.DeferredRegister;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

/** The item data components the mod defines. */
public final class ModComponents {
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, PromisedEnd.MODID);

    /** What a sealed Star Chart leads to: a structure tag, as {@code promisedend:on_foundry_charts}. */
    public static final Supplier<DataComponentType<Identifier>> STAR_CHART = COMPONENTS.registerComponentType("star_chart",
            b -> b.persistent(Identifier.CODEC).networkSynchronized(Identifier.STREAM_CODEC));

    private ModComponents() {}
}
