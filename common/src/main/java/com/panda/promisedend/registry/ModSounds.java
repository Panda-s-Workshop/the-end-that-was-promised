package com.panda.promisedend.registry;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.platform.registry.DeferredRegister;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

/** The mod's sound events. Each one plays a vanilla sound for now (assets/promisedend/sounds.json) under our subtitle. */
public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, PromisedEnd.MODID);

    public static final Supplier<SoundEvent> CRYSTAL_WARDEN_AMBIENT = sound("entity.crystal_warden.ambient");
    public static final Supplier<SoundEvent> CRYSTAL_WARDEN_HURT = sound("entity.crystal_warden.hurt");
    public static final Supplier<SoundEvent> CRYSTAL_WARDEN_DEATH = sound("entity.crystal_warden.death");
    public static final Supplier<SoundEvent> CRYSTAL_WARDEN_LOCK_ON = sound("entity.crystal_warden.lock_on");
    public static final Supplier<SoundEvent> CRYSTAL_WARDEN_BEAM = sound("entity.crystal_warden.beam");
    public static final Supplier<SoundEvent> CRYSTAL_WARDEN_POWER_DOWN = sound("entity.crystal_warden.power_down");
    public static final Supplier<SoundEvent> CRYSTAL_WARDEN_ARRIVE = sound("entity.crystal_warden.arrive");
    public static final Supplier<SoundEvent> DRAGON_CALL_WARDENS = sound("entity.ender_dragon.call_wardens");
    public static final Supplier<SoundEvent> DRAGON_HOARD_APPEARS = sound("block.dragon_hoard.appear");
    public static final Supplier<SoundEvent> DRAGON_VOID_BREATH_GATHER = sound("entity.ender_dragon.void_breath_gather");
    public static final Supplier<SoundEvent> DRAGON_VOID_BREATH = sound("entity.ender_dragon.void_breath");

    // biome ambience (the biomes' minecraft:audio/ambient_sounds)
    public static final Supplier<SoundEvent> AMBIENT_CHORUS_THICKET = sound("ambient.chorus_thicket.additions");
    public static final Supplier<SoundEvent> AMBIENT_LUMEN_MIRE = sound("ambient.lumen_mire.additions");

    private static Supplier<SoundEvent> sound(String name) {
        return SOUNDS.register(name, SoundEvent::createVariableRangeEvent);
    }

    private ModSounds() {}
}
