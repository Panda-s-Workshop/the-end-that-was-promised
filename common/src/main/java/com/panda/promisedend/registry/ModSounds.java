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
    public static final Supplier<SoundEvent> CHORUS_WEAVER_AMBIENT = sound("entity.chorus_weaver.ambient");
    public static final Supplier<SoundEvent> CHORUS_WEAVER_HURT = sound("entity.chorus_weaver.hurt");
    public static final Supplier<SoundEvent> CHORUS_WEAVER_DEATH = sound("entity.chorus_weaver.death");
    public static final Supplier<SoundEvent> CHORUS_WEAVER_REVEAL = sound("entity.chorus_weaver.reveal");
    public static final Supplier<SoundEvent> CHORUS_WEAVER_LASH = sound("entity.chorus_weaver.lash");
    public static final Supplier<SoundEvent> CHORUS_WEAVER_VINE_HIT = sound("entity.chorus_weaver.vine_hit");
    public static final Supplier<SoundEvent> LUMEN_WISP_AMBIENT = sound("entity.lumen_wisp.ambient");
    public static final Supplier<SoundEvent> LUMEN_WISP_HURT = sound("entity.lumen_wisp.hurt");
    public static final Supplier<SoundEvent> LUMEN_WISP_DEATH = sound("entity.lumen_wisp.death");
    public static final Supplier<SoundEvent> RIFT_STALKER_AMBIENT = sound("entity.rift_stalker.ambient");
    public static final Supplier<SoundEvent> RIFT_STALKER_HURT = sound("entity.rift_stalker.hurt");
    public static final Supplier<SoundEvent> RIFT_STALKER_DEATH = sound("entity.rift_stalker.death");
    public static final Supplier<SoundEvent> RIFT_STALKER_GATHER = sound("entity.rift_stalker.gather");
    public static final Supplier<SoundEvent> RIFT_STALKER_BLINK = sound("entity.rift_stalker.blink");
    public static final Supplier<SoundEvent> RIFT_STALKER_STRIKE = sound("entity.rift_stalker.strike");
    public static final Supplier<SoundEvent> RIFT_STALKER_FREEZE = sound("entity.rift_stalker.freeze");
    public static final Supplier<SoundEvent> PURPUR_SENTRY_AMBIENT = sound("entity.purpur_sentry.ambient");
    public static final Supplier<SoundEvent> PURPUR_SENTRY_HURT = sound("entity.purpur_sentry.hurt");
    public static final Supplier<SoundEvent> PURPUR_SENTRY_DEATH = sound("entity.purpur_sentry.death");
    public static final Supplier<SoundEvent> PURPUR_SENTRY_SHIELD = sound("entity.purpur_sentry.shield");
    public static final Supplier<SoundEvent> PURPUR_SENTRY_BLOCK = sound("entity.purpur_sentry.block");
    public static final Supplier<SoundEvent> PURPUR_SENTRY_LUNGE = sound("entity.purpur_sentry.lunge");
    public static final Supplier<SoundEvent> HOLLOW_SENTINEL_AMBIENT = sound("entity.hollow_sentinel.ambient");
    public static final Supplier<SoundEvent> HOLLOW_SENTINEL_HURT = sound("entity.hollow_sentinel.hurt");
    public static final Supplier<SoundEvent> HOLLOW_SENTINEL_HURT_CLOSED = sound("entity.hollow_sentinel.hurt_closed");
    public static final Supplier<SoundEvent> HOLLOW_SENTINEL_DEATH = sound("entity.hollow_sentinel.death");
    public static final Supplier<SoundEvent> HOLLOW_SENTINEL_WAKE = sound("entity.hollow_sentinel.wake");
    public static final Supplier<SoundEvent> HOLLOW_SENTINEL_VOLLEY = sound("entity.hollow_sentinel.volley");
    public static final Supplier<SoundEvent> HOLLOW_SENTINEL_SHOOT = sound("entity.hollow_sentinel.shoot");
    public static final Supplier<SoundEvent> HOLLOW_SENTINEL_OPEN = sound("entity.hollow_sentinel.open");
    public static final Supplier<SoundEvent> HOLLOW_SENTINEL_CLOSE = sound("entity.hollow_sentinel.close");
    public static final Supplier<SoundEvent> HOLLOW_SENTINEL_PHASE = sound("entity.hollow_sentinel.phase");
    public static final Supplier<SoundEvent> HOLLOW_SENTINEL_ENRAGE = sound("entity.hollow_sentinel.enrage");
    public static final Supplier<SoundEvent> VOID_LEVIATHAN_AMBIENT = sound("entity.void_leviathan.ambient");
    public static final Supplier<SoundEvent> VOID_LEVIATHAN_HURT = sound("entity.void_leviathan.hurt");
    public static final Supplier<SoundEvent> VOID_LEVIATHAN_HURT_DEEP = sound("entity.void_leviathan.hurt_deep");
    public static final Supplier<SoundEvent> VOID_LEVIATHAN_DEATH = sound("entity.void_leviathan.death");
    public static final Supplier<SoundEvent> VOID_LEVIATHAN_WAKE = sound("entity.void_leviathan.wake");
    public static final Supplier<SoundEvent> VOID_LEVIATHAN_TELEGRAPH = sound("entity.void_leviathan.telegraph");
    public static final Supplier<SoundEvent> VOID_LEVIATHAN_BREACH = sound("entity.void_leviathan.breach");
    public static final Supplier<SoundEvent> VOID_LEVIATHAN_DIVE = sound("entity.void_leviathan.dive");
    public static final Supplier<SoundEvent> VOID_LEVIATHAN_PULL = sound("entity.void_leviathan.pull");
    public static final Supplier<SoundEvent> RESONANT_BULLET_RETURN = sound("entity.resonant_bullet.return");
    public static final Supplier<SoundEvent> RESONANT_BULLET_HIT = sound("entity.resonant_bullet.hit");
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
    public static final Supplier<SoundEvent> ECHO_AMBER_FLARE = sound("block.echo_amber_cluster.flare");

    private static Supplier<SoundEvent> sound(String name) {
        return SOUNDS.register(name, SoundEvent::createVariableRangeEvent);
    }

    /** A Void Shrine's altar: an offering laid on it, the choice ready, a boon given. */
    public static final Supplier<SoundEvent> SHRINE_OFFER = sound("block.shrine_altar.offer");
    public static final Supplier<SoundEvent> SHRINE_READY = sound("block.shrine_altar.ready");
    public static final Supplier<SoundEvent> SHRINE_GRANT = sound("block.shrine_altar.grant");
    public static final Supplier<SoundEvent> RIFT_GATE_ATTUNE = sound("block.rift_gate.attune");
    public static final Supplier<SoundEvent> RIFT_GATE_TRAVEL = sound("block.rift_gate.travel");
    /** The Lattice Crawler, and the lattice it tears: the groan while it goes, the tear, the block re-forming. */
    public static final Supplier<SoundEvent> LATTICE_CRAWLER_AMBIENT = sound("entity.lattice_crawler.ambient");
    public static final Supplier<SoundEvent> LATTICE_CRAWLER_HURT = sound("entity.lattice_crawler.hurt");
    public static final Supplier<SoundEvent> LATTICE_CRAWLER_DEATH = sound("entity.lattice_crawler.death");
    public static final Supplier<SoundEvent> LATTICE_CRAWLER_STEP = sound("entity.lattice_crawler.step");
    public static final Supplier<SoundEvent> LATTICE_CRAWLER_TEAR = sound("entity.lattice_crawler.tear");
    public static final Supplier<SoundEvent> VOIDSTEEL_HOLD = sound("item.voidsteel.hold");
    public static final Supplier<SoundEvent> VOIDSTEEL_RELEASE = sound("item.voidsteel.release");
    public static final Supplier<SoundEvent> AMBER_GATE_OPEN = sound("block.amber_gate.open");
    public static final Supplier<SoundEvent> AMBER_GATE_CLOSE = sound("block.amber_gate.close");
    public static final Supplier<SoundEvent> LATTICE_GROAN = sound("block.lattice.groan");
    public static final Supplier<SoundEvent> ARCHITECT_AMBIENT = sound("entity.the_architect.ambient");
    public static final Supplier<SoundEvent> ARCHITECT_HURT = sound("entity.the_architect.hurt");
    public static final Supplier<SoundEvent> ARCHITECT_DEATH = sound("entity.the_architect.death");
    public static final Supplier<SoundEvent> ARCHITECT_WAKE = sound("entity.the_architect.wake");
    public static final Supplier<SoundEvent> ARCHITECT_RAISE = sound("entity.the_architect.raise");
    public static final Supplier<SoundEvent> ARCHITECT_THROW = sound("entity.the_architect.throw");
    public static final Supplier<SoundEvent> ARCHITECT_UNMAKE = sound("entity.the_architect.unmake");
    public static final Supplier<SoundEvent> ARCHITECT_KEYSTONE = sound("entity.the_architect.keystone");
    public static final Supplier<SoundEvent> ARCHITECT_PHASE = sound("entity.the_architect.phase");
    public static final Supplier<SoundEvent> ARCHITECT_OPEN = sound("entity.the_architect.open");
    public static final Supplier<SoundEvent> ARCHITECT_SHUT = sound("entity.the_architect.shut");
    public static final Supplier<SoundEvent> ARCHITECT_SWEEP = sound("entity.the_architect.sweep");
    public static final Supplier<SoundEvent> LATTICE_SHARD_HIT = sound("entity.lattice_shard.hit");
    /** The Architect's Lattice: a glider's boost, and its press while the lattice is still setting. */
    public static final Supplier<SoundEvent> LATTICE_BOOST = sound("item.architects_lattice.boost");
    public static final Supplier<SoundEvent> LATTICE_BOOST_SETTING = sound("item.architects_lattice.setting");
    public static final Supplier<SoundEvent> LATTICE_TEAR = sound("block.lattice.tear");
    public static final Supplier<SoundEvent> LATTICE_REFORM = sound("block.lattice.reform");
    /** The Starfall: its far rumble as it is seen coming, and its landing. */
    public static final Supplier<SoundEvent> STARFALL_WARNING = sound("event.starfall.warning");
    public static final Supplier<SoundEvent> STARFALL_IMPACT = sound("event.starfall.impact");

    private ModSounds() {}
}
