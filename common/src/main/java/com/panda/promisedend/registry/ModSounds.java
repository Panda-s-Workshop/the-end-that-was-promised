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

    public static final Supplier<SoundEvent> VOID_WATER_ENTER = sound("block.void_water.enter");
    // Resonite: a cluster's hum when you're near, and the chime when one breaks
    public static final Supplier<SoundEvent> RESONITE_HUM = sound("block.resonite_cluster.hum");
    public static final Supplier<SoundEvent> RESONITE_CHIME = sound("block.resonite_cluster.chime");
    // the Updraft Vent's wind
    public static final Supplier<SoundEvent> UPDRAFT_WIND = sound("block.updraft_vent.wind");
    // Crumbling Voidstone cracking under a step
    public static final Supplier<SoundEvent> CRUMBLING_VOIDSTONE_CRACK = sound("block.crumbling_voidstone.crack");

    private static Supplier<SoundEvent> sound(String name) {
        return SOUNDS.register(name, SoundEvent::createVariableRangeEvent);
    }

    private ModSounds() {}
}
