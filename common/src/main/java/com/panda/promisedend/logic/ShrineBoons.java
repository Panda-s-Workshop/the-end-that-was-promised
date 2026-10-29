package com.panda.promisedend.logic;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.block.UpdraftVentBlockEntity;
import com.panda.promisedend.registry.ModEffects;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * What the Void Shrines' boons do, each server tick for each player who has one. Resonant Edge is its effect's
 * attribute modifier, Shulker Ward the check in {@link #wards}, and Rift Step the Void Anchor's rescue.
 */
public final class ShrineBoons {
    /** How long a boon lasts: ten minutes, Void Sight five; Rift Step until it catches a fall, or ten minutes. */
    public static final int DURATION = 12000, VOID_SIGHT_DURATION = 6000;
    /** The most wear a worn glider takes in a tick that Gliding Grace counts as flight (a swap reads as more). */
    static final int GLIDE_WEAR_MOST = 2;
    /** Void Sight: how often its outlines are drawn, how far round the player vents and structures are shown. */
    public static final int SIGHT_EVERY = 20, VENT_REACH = 32, STRUCTURE_CHUNKS = 6;
    private static final DustParticleOptions OUTLINE = new DustParticleOptions(0x9A7CFF, 1.6F);

    /** Players whose pearl cooldown running now was already halved. */
    private static final Set<UUID> HALVED = ConcurrentHashMap.newKeySet();
    /** Gliding Grace, per player: the worn glider's damage last tick, and the points of wear it has taken since. */
    private static final Map<UUID, int[]> WEAR = new ConcurrentHashMap<>();

    private ShrineBoons() {}

    /** Gives {@code player} the boon at {@code index} ({@link Offerings#BOONS}), its full time. */
    public static void grant(ServerPlayer player, int index) {
        int ticks = Offerings.BOONS[index].equals("void_sight") ? VOID_SIGHT_DURATION : DURATION;
        player.addEffect(new MobEffectInstance(ModEffects.boon(index), ticks, 0, false, true, true));
        var advancements = player.level().getServer().getAdvancements();
        var boon = advancements.get(CodexBook.TAKE_BOON);
        if (boon != null) player.getAdvancements().award(boon, "boon");
        var every = advancements.get(CodexBook.ALL_BOONS);
        if (every != null) player.getAdvancements().award(every, Offerings.BOONS[index]);
    }

    /** Shulker Ward: Levitation doesn't take hold on a warded entity (a Sentinel's bullet, a shulker's). */
    public static boolean wards(LivingEntity entity, MobEffectInstance effect) {
        return effect.is(MobEffects.LEVITATION) && ModEffects.SHULKER_WARD.isBound() && entity.hasEffect(ModEffects.SHULKER_WARD.holder());
    }

    public static void tick(ServerPlayer player) {
        if (player.isSpectator()) return;
        if (player.hasEffect(ModEffects.GLIDING_GRACE.holder())) mendGlider(player);
        else WEAR.remove(player.getUUID());
        // a ward taken while already lifted lets go at once
        if (player.hasEffect(MobEffects.LEVITATION) && player.hasEffect(ModEffects.SHULKER_WARD.holder())) player.removeEffect(MobEffects.LEVITATION);
        if (player.hasEffect(ModEffects.PEARL_SENSE.holder())) halvePearl(player);
        else HALVED.remove(player.getUUID());
        if (player.hasEffect(ModEffects.VOID_SIGHT.holder()) && player.tickCount % SIGHT_EVERY == 0) sight(player);
    }

    public static void forget(ServerPlayer player) {
        HALVED.remove(player.getUUID());
        WEAR.remove(player.getUUID());
    }

    /**
     * Gliding Grace: every second point of wear the worn glider takes is given back, so it wears half as fast. It
     * reads the wear itself, so Unbreaking still counts, and only the glider worn on the chest.
     */
    private static void mendGlider(ServerPlayer player) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!stack.has(DataComponents.GLIDER) || !stack.isDamageableItem()) {
            WEAR.remove(player.getUUID());
            return;
        }
        int now = stack.getDamageValue();
        int[] wear = WEAR.computeIfAbsent(player.getUUID(), u -> new int[] {now, 0});
        int worn = now - wear[0];
        if (worn > 0 && worn <= GLIDE_WEAR_MOST) {
            int back = (wear[1] + worn) / 2 - wear[1] / 2;
            wear[1] += worn;
            if (back > 0) stack.setDamageValue(now - back);
        }
        wear[0] = stack.getDamageValue();
    }

    /** Pearl Sense: a pearl cooldown, as it starts, is set to half its length. */
    private static void halvePearl(ServerPlayer player) {
        ItemStack pearl = new ItemStack(Items.ENDER_PEARL);
        var cooldowns = player.getCooldowns();
        if (!cooldowns.isOnCooldown(pearl)) {
            HALVED.remove(player.getUUID());
            return;
        }
        if (!HALVED.add(player.getUUID())) return;
        var use = pearl.get(DataComponents.USE_COOLDOWN);
        int ticks = use == null ? 20 : use.ticks();
        cooldowns.addCooldown(pearl, Math.max(1, ticks / 2));
    }

    /**
     * Void Sight: the Updraft Vents within {@link #VENT_REACH} as a column of motes over each, and every structure of
     * ours that starts within {@link #STRUCTURE_CHUNKS} chunks outlined at its top, shown to this player alone. Only
     * loaded chunks are read.
     */
    private static void sight(ServerPlayer player) {
        ServerLevel level = player.level();
        List<BoundingBox> boxes = new ArrayList<>();
        for (UpdraftVentBlockEntity vent : seen(player, boxes)) {
            var p = vent.getBlockPos();
            for (int y = 1; y <= Math.min(vent.height(), 24); y += 2) {
                level.sendParticles(player, ParticleTypes.END_ROD, true, false, p.getX() + 0.5, p.getY() + y, p.getZ() + 0.5, 1, 0.05, 0.3, 0.05, 0.0);
            }
        }
        for (BoundingBox box : boxes) outline(level, player, box);
    }

    /** What Void Sight shows {@code player} now: the vents in reach, and (into {@code boxes}) our structures near. */
    public static List<UpdraftVentBlockEntity> seen(ServerPlayer player, List<BoundingBox> boxes) {
        ServerLevel level = player.level();
        BlockPos at = player.blockPosition();
        int pcx = at.getX() >> 4, pcz = at.getZ() >> 4;
        var structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        int ventChunks = (VENT_REACH >> 4) + 1;
        List<UpdraftVentBlockEntity> vents = new ArrayList<>();
        for (int cx = pcx - STRUCTURE_CHUNKS; cx <= pcx + STRUCTURE_CHUNKS; cx++) {
            for (int cz = pcz - STRUCTURE_CHUNKS; cz <= pcz + STRUCTURE_CHUNKS; cz++) {
                var chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (var start : chunk.getAllStarts().entrySet()) {
                    var key = structures.getKey(start.getKey());
                    if (key != null && key.getNamespace().equals(PromisedEnd.MODID) && start.getValue().isValid()) boxes.add(start.getValue().getBoundingBox());
                }
                if (Math.abs(cx - pcx) > ventChunks || Math.abs(cz - pcz) > ventChunks) continue;
                for (var be : chunk.getBlockEntities().values()) {
                    if (be instanceof UpdraftVentBlockEntity vent && be.getBlockPos().distSqr(at) <= VENT_REACH * VENT_REACH) vents.add(vent);
                }
            }
        }
        return vents;
    }

    /** The top rectangle of {@code box} in still motes, one every two blocks, and its corners down to its foot. */
    private static void outline(ServerLevel level, ServerPlayer player, BoundingBox box) {
        double top = box.maxY() + 1;
        for (int x = box.minX(); x <= box.maxX() + 1; x += 2) {
            mote(level, player, x, top, box.minZ());
            mote(level, player, x, top, box.maxZ() + 1);
        }
        for (int z = box.minZ(); z <= box.maxZ() + 1; z += 2) {
            mote(level, player, box.minX(), top, z);
            mote(level, player, box.maxX() + 1, top, z);
        }
        for (int y = box.minY(); y < top; y += 3) {
            mote(level, player, box.minX(), y, box.minZ());
            mote(level, player, box.maxX() + 1, y, box.minZ());
            mote(level, player, box.minX(), y, box.maxZ() + 1);
            mote(level, player, box.maxX() + 1, y, box.maxZ() + 1);
        }
    }

    private static void mote(ServerLevel level, ServerPlayer player, double x, double y, double z) {
        level.sendParticles(player, OUTLINE, true, false, x, y, z, 1, 0, 0, 0, 0);
    }
}
