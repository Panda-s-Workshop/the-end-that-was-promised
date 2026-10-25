package com.panda.promisedend.registry;

import com.mojang.serialization.MapCodec;
import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.platform.registry.DeferredRegister;
import com.panda.promisedend.worldgen.ChorusGroveFeature;
import com.panda.promisedend.worldgen.MirePoolFeature;
import com.panda.promisedend.worldgen.ResoniteSpireFeature;
import com.panda.promisedend.worldgen.RimDebrisFeature;
import com.panda.promisedend.worldgen.RingSpreadPlacement;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/** Worldgen types. On 26.3 a feature type (and a structure placement type) is its codec. */
public final class ModWorldgen {
    private static final DeferredRegister<MapCodec<? extends Feature>> FEATURE_TYPES = DeferredRegister.create(Registries.FEATURE_TYPE, PromisedEnd.MODID);

    private static final DeferredRegister<MapCodec<? extends StructurePlacement>> PLACEMENTS = DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, PromisedEnd.MODID);

    public static final Supplier<MapCodec<RimDebrisFeature>> RIM_DEBRIS = FEATURE_TYPES.register("rim_debris", () -> RimDebrisFeature.CODEC);
    public static final Supplier<MapCodec<ChorusGroveFeature>> CHORUS_GROVE = FEATURE_TYPES.register("chorus_grove", () -> ChorusGroveFeature.CODEC);
    public static final Supplier<MapCodec<MirePoolFeature>> MIRE_POOL = FEATURE_TYPES.register("mire_pool", () -> MirePoolFeature.CODEC);
    public static final Supplier<MapCodec<ResoniteSpireFeature>> RESONITE_SPIRE = FEATURE_TYPES.register("resonite_spire", () -> ResoniteSpireFeature.CODEC);
    public static final Supplier<MapCodec<RingSpreadPlacement>> RING_SPREAD = PLACEMENTS.register("ring_spread", () -> RingSpreadPlacement.CODEC);

    private ModWorldgen() {}
}
