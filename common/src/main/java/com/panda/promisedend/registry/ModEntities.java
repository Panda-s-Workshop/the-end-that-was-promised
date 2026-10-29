package com.panda.promisedend.registry;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.entity.ChorusWeaver;
import com.panda.promisedend.entity.CrystalWarden;
import com.panda.promisedend.entity.LumenWisp;
import com.panda.promisedend.entity.PurpurSentry;
import com.panda.promisedend.entity.RiftStalker;
import com.panda.promisedend.entity.DiveMark;
import com.panda.promisedend.platform.registry.DeferredRegister;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.levelgen.Heightmap;

/** The Ancients' constructs. Each loader hands {@link #attributes} and the client's {@code ModRenderers} to its own API. */
public final class ModEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(PromisedEnd.MODID);

    // the hitbox fits the model at its 1.5 scale: the ring with its joints is 14 px (1.31 blocks) across, the top cap's
    // top 20.5 px (1.92 blocks) up. Its eyes are the lens, so line of sight and looks run from where the beam leaves
    public static final Supplier<EntityType<CrystalWarden>> CRYSTAL_WARDEN = ENTITIES.registerEntityType("crystal_warden",
            CrystalWarden::new, MobCategory.MONSTER, b -> b.sized(1.3F, 1.9F).eyeHeight(CrystalWarden.LENS_HEIGHT)
                    .clientTrackingRange(10).fireImmune());
    // a chorus plant's height, at the rig's 1.25 scale: its crown 28 px up (2.2 blocks), its trunk 9 px across
    public static final Supplier<EntityType<ChorusWeaver>> CHORUS_WEAVER = ENTITIES.registerEntityType("chorus_weaver",
            ChorusWeaver::new, MobCategory.MONSTER, b -> b.sized(0.8F, 2.2F).eyeHeight(1.85F).clientTrackingRange(8));
    // a lantern's size at the rig's scale: its cage 7 px across, its hook's top 11 px up
    public static final Supplier<EntityType<LumenWisp>> LUMEN_WISP = ENTITIES.registerEntityType("lumen_wisp",
            LumenWisp::new, MobCategory.AMBIENT, b -> b.sized(0.5F, 0.7F).eyeHeight(0.4F).clientTrackingRange(8));
    // hunched at the rig's 1.25 scale: its head's top 28 px up (2.2 blocks), its shoulders 10 px across
    public static final Supplier<EntityType<RiftStalker>> RIFT_STALKER = ENTITIES.registerEntityType("rift_stalker",
            RiftStalker::new, MobCategory.MONSTER, b -> b.sized(0.7F, 2.2F).eyeHeight(1.9F).clientTrackingRange(8));
    // squat at the rig's 1.25 scale: its helm's top 26 px up (2.0 blocks), its shoulders 13 px across; no natural
    // spawn (the shrines and observatories place their own)
    public static final Supplier<EntityType<PurpurSentry>> PURPUR_SENTRY = ENTITIES.registerEntityType("purpur_sentry",
            PurpurSentry::new, MobCategory.MONSTER, b -> b.sized(1.0F, 2.0F).eyeHeight(1.75F).clientTrackingRange(8));
    // the Shulker Foundry's guardian, at the rig's 1.5 scale: its cap's top 32 px up (3 blocks), its plates 16 px
    // across (the halo's shards reach past them); fire can't touch it, and it is seen from across the arena and over
    public static final Supplier<EntityType<com.panda.promisedend.entity.HollowSentinel>> HOLLOW_SENTINEL = ENTITIES.registerEntityType("hollow_sentinel",
            com.panda.promisedend.entity.HollowSentinel::new, MobCategory.MONSTER, b -> b.sized(2.0F, 3.0F).eyeHeight(1.5F)
                    .clientTrackingRange(10).fireImmune());
    /** Its shot: small, and tracked every tick, so a blow aimed at it lands where it is seen. */
    public static final Supplier<EntityType<com.panda.promisedend.entity.ResonantBullet>> RESONANT_BULLET = ENTITIES.registerEntityType("resonant_bullet",
            com.panda.promisedend.entity.ResonantBullet::new, MobCategory.MISC, b -> b.sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(1));
    /** The dragon's dive mark: no body, never saved, seen from as far as the fight. */
    public static final Supplier<EntityType<DiveMark>> DIVE_MARK = ENTITIES.registerEntityType("dive_mark",
            DiveMark::new, MobCategory.MISC, b -> b.sized(0.5F, 0.1F).clientTrackingRange(10).updateInterval(10).noSave().noSummon().fireImmune());
    /**
     * A Drift Islet: 3x3 and a block thick. Clients step it themselves, so the server's position updates are only a
     * check once a second; seen from 160 blocks, across a gap.
     */
    public static final Supplier<EntityType<com.panda.promisedend.entity.DriftIslet>> DRIFT_ISLET = ENTITIES.registerEntityType("drift_islet",
            com.panda.promisedend.entity.DriftIslet::new, MobCategory.MISC, b -> b.sized(3.0F, 1.0F).clientTrackingRange(10).updateInterval(20).fireImmune());

    /** Every construct's attributes, for the loader's attribute registration. */
    public static void attributes(BiConsumer<EntityType<? extends LivingEntity>, AttributeSupplier.Builder> sink) {
        sink.accept(CRYSTAL_WARDEN.get(), CrystalWarden.createAttributes());
        sink.accept(CHORUS_WEAVER.get(), ChorusWeaver.createAttributes());
        sink.accept(LUMEN_WISP.get(), LumenWisp.createAttributes());
        sink.accept(RIFT_STALKER.get(), RiftStalker.createAttributes());
        sink.accept(PURPUR_SENTRY.get(), PurpurSentry.createAttributes());
        sink.accept(HOLLOW_SENTINEL.get(), com.panda.promisedend.entity.HollowSentinel.createAttributes());
    }

    /** A loader's way of registering a spawn placement (NeoForge: the event; Fabric: {@code SpawnPlacements.register}). */
    public interface SpawnRules {
        <T extends Mob> void set(EntityType<T> type, SpawnPlacementType placement, Heightmap.Types heightmap, SpawnPlacements.SpawnPredicate<T> predicate);
    }

    /** Where each construct may spawn naturally; the biomes' spawners say which biome (gen_world). */
    public static void spawnRules(SpawnRules rules) {
        rules.set(CHORUS_WEAVER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ChorusWeaver::canSpawn);
        rules.set(LUMEN_WISP.get(), SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, LumenWisp::canSpawn);
        rules.set(RIFT_STALKER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, RiftStalker::canSpawn);
    }

    private ModEntities() {}
}
