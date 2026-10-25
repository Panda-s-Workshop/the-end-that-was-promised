package com.panda.promisedend.registry;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.entity.CrystalWarden;
import com.panda.promisedend.entity.DiveMark;
import com.panda.promisedend.platform.registry.DeferredRegister;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

/** The Ancients' constructs. Each loader hands {@link #attributes} and the client's {@code ModRenderers} to its own API. */
public final class ModEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(PromisedEnd.MODID);

    // the hitbox fits the model at its 1.5 scale: the ring with its joints is 14 px (1.31 blocks) across, the top cap's
    // top 20.5 px (1.92 blocks) up. Its eyes are the lens, so line of sight and looks run from where the beam leaves
    public static final Supplier<EntityType<CrystalWarden>> CRYSTAL_WARDEN = ENTITIES.registerEntityType("crystal_warden",
            CrystalWarden::new, MobCategory.MONSTER, b -> b.sized(1.3F, 1.9F).eyeHeight(CrystalWarden.LENS_HEIGHT)
                    .clientTrackingRange(10).fireImmune());
    /** The dragon's dive mark: no body, never saved, seen from as far as the fight. */
    public static final Supplier<EntityType<DiveMark>> DIVE_MARK = ENTITIES.registerEntityType("dive_mark",
            DiveMark::new, MobCategory.MISC, b -> b.sized(0.5F, 0.1F).clientTrackingRange(10).updateInterval(10).noSave().noSummon().fireImmune());

    /** Every construct's attributes, for the loader's attribute registration. */
    public static void attributes(BiConsumer<EntityType<? extends LivingEntity>, AttributeSupplier.Builder> sink) {
        sink.accept(CRYSTAL_WARDEN.get(), CrystalWarden.createAttributes());
    }

    private ModEntities() {}
}
