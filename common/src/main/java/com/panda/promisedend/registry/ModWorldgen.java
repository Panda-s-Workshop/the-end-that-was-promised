package com.panda.promisedend.registry;

import com.mojang.serialization.MapCodec;
import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.platform.registry.DeferredRegister;
import com.panda.promisedend.worldgen.ChorusGroveFeature;
import com.panda.promisedend.worldgen.CrumblingEdgeFeature;
import com.panda.promisedend.worldgen.MirePoolFeature;
import com.panda.promisedend.worldgen.PurpurReefFeature;
import com.panda.promisedend.worldgen.ResoniteSpireFeature;
import com.panda.promisedend.worldgen.RiftScarFeature;
import com.panda.promisedend.worldgen.RimDebrisFeature;
import com.panda.promisedend.worldgen.RingSpreadPlacement;
import com.panda.promisedend.worldgen.TerrainOffFilter;
import com.panda.promisedend.worldgen.UpdraftVentFeature;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

/** Worldgen types. On 26.3 a feature type (and a structure placement type) is its codec. */
public final class ModWorldgen {
    private static final DeferredRegister<MapCodec<? extends Feature>> FEATURE_TYPES = DeferredRegister.create(Registries.FEATURE_TYPE, PromisedEnd.MODID);

    private static final DeferredRegister<MapCodec<? extends PlacementModifier>> MODIFIERS = DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, PromisedEnd.MODID);

    private static final DeferredRegister<MapCodec<? extends StructurePlacement>> PLACEMENTS = DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, PromisedEnd.MODID);

    private static final DeferredRegister<net.minecraft.world.level.levelgen.structure.StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, PromisedEnd.MODID);

    private static final DeferredRegister<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, PromisedEnd.MODID);

    public static final Supplier<MapCodec<RimDebrisFeature>> RIM_DEBRIS = FEATURE_TYPES.register("rim_debris", () -> RimDebrisFeature.CODEC);
    public static final Supplier<MapCodec<com.panda.promisedend.worldgen.CausewayFeature>> CAUSEWAY = FEATURE_TYPES.register("causeway",
            () -> com.panda.promisedend.worldgen.CausewayFeature.CODEC);
    public static final Supplier<MapCodec<ChorusGroveFeature>> CHORUS_GROVE = FEATURE_TYPES.register("chorus_grove", () -> ChorusGroveFeature.CODEC);
    public static final Supplier<MapCodec<MirePoolFeature>> MIRE_POOL = FEATURE_TYPES.register("mire_pool", () -> MirePoolFeature.CODEC);
    public static final Supplier<MapCodec<ResoniteSpireFeature>> RESONITE_SPIRE = FEATURE_TYPES.register("resonite_spire", () -> ResoniteSpireFeature.CODEC);
    public static final Supplier<MapCodec<UpdraftVentFeature>> UPDRAFT_VENT = FEATURE_TYPES.register("updraft_vent", () -> UpdraftVentFeature.CODEC);
    public static final Supplier<MapCodec<RiftScarFeature>> RIFT_SCAR = FEATURE_TYPES.register("rift_scar", () -> RiftScarFeature.CODEC);
    public static final Supplier<MapCodec<CrumblingEdgeFeature>> CRUMBLING_EDGE = FEATURE_TYPES.register("crumbling_edge", () -> CrumblingEdgeFeature.CODEC);
    public static final Supplier<MapCodec<PurpurReefFeature>> PURPUR_REEF = FEATURE_TYPES.register("purpur_reef", () -> PurpurReefFeature.CODEC);
    public static final Supplier<MapCodec<com.panda.promisedend.worldgen.HollowCavernFeature>> HOLLOW_CAVERN = FEATURE_TYPES.register("hollow_cavern",
            () -> com.panda.promisedend.worldgen.HollowCavernFeature.CODEC);
    public static final Supplier<MapCodec<com.panda.promisedend.worldgen.HangingVineFeature>> HANGING_VINE = FEATURE_TYPES.register("hanging_vine",
            () -> com.panda.promisedend.worldgen.HangingVineFeature.CODEC);
    public static final Supplier<MapCodec<com.panda.promisedend.worldgen.EdgeGroundFeature>> EDGE_GROUND = FEATURE_TYPES.register("edge_ground",
            () -> com.panda.promisedend.worldgen.EdgeGroundFeature.CODEC);
    public static final Supplier<MapCodec<com.panda.promisedend.worldgen.EdgeScaffoldFeature>> EDGE_SCAFFOLD = FEATURE_TYPES.register("edge_scaffold",
            () -> com.panda.promisedend.worldgen.EdgeScaffoldFeature.CODEC);
    public static final Supplier<MapCodec<TerrainOffFilter>> TERRAIN_OFF = MODIFIERS.register("terrain_off", () -> TerrainOffFilter.CODEC);
    public static final Supplier<MapCodec<com.panda.promisedend.worldgen.ChunkChanceFilter>> CHUNK_CHANCE = MODIFIERS.register("chunk_chance",
            () -> com.panda.promisedend.worldgen.ChunkChanceFilter.CODEC);
    public static final Supplier<MapCodec<com.panda.promisedend.worldgen.StableSeed>> STABLE_SEED = MODIFIERS.register("stable_seed",
            () -> com.panda.promisedend.worldgen.StableSeed.CODEC);
    public static final Supplier<MapCodec<com.panda.promisedend.worldgen.UnclaimedFilter>> UNCLAIMED = MODIFIERS.register("unclaimed",
            () -> com.panda.promisedend.worldgen.UnclaimedFilter.CODEC);
    public static final Supplier<MapCodec<RingSpreadPlacement>> RING_SPREAD = PLACEMENTS.register("ring_spread", () -> RingSpreadPlacement.CODEC);
    public static final Supplier<net.minecraft.world.level.levelgen.structure.StructureType<com.panda.promisedend.worldgen.DriftRouteStructure>> DRIFT_ROUTE =
            STRUCTURE_TYPES.register("drift_route", () -> () -> com.panda.promisedend.worldgen.DriftRouteStructure.CODEC);
    public static final Supplier<net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType> DRIFT_ROUTE_PIECE =
            STRUCTURE_PIECES.register("drift_route", () -> com.panda.promisedend.worldgen.DriftRouteStructure.Piece::new);

    private ModWorldgen() {}
}
