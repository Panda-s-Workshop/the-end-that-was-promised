package com.panda.promisedend.logic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.promisedend.EndConfig;
import com.panda.promisedend.PromisedEnd;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

/**
 * Dragon Wrath: one number for the world, 0 to 5, raised by each dragon that dies. Each dragon that appears has its
 * health scaled by the Wrath at that moment (200, 250 ... 450), and the Wrath it was fought at picks the hoard left in a
 * chest beside the exit portal once it is gone: {@code chests/dragon_hoard/wrath_<n>}, 3 + Wrath Dragon Scales for now.
 *
 * <p>A kill is a dragon that goes through its dying animation; one removed by {@code /kill} has none and earns nothing.
 * With {@code dragon.wrath} off every fight is Wrath 0 and kills don't raise it; {@code dragon.max_wrath} caps it.
 */
public final class DragonWrath {
    public static final int MAX = 5;
    public static final float BASE_HEALTH = 200.0F;
    /** Each level of Wrath adds a quarter of the dragon's base health. */
    public static final float HEALTH_PER_WRATH = 0.25F;
    public static final int BASE_SCALES = 3;
    /** On a dragon once its health has been scaled (saved with it), so a reload doesn't scale it again. */
    public static final String SCALED_TAG = "promisedend.wrath_scaled";
    /** With the Wrath it was scaled by after it ({@code promisedend.fought_at_3}): the hoard it leaves (saved with it). */
    public static final String FOUGHT_AT_TAG = "promisedend.fought_at_";
    public static final Identifier HEALTH_MODIFIER = PromisedEnd.id("wrath");
    /** How far from the portal's centre the hoard chest is set: just off the podium's rim. */
    private static final int HOARD_DISTANCE = EndPodiumFeature.PODIUM_RADIUS + EndPodiumFeature.RIM_RADIUS + 1;
    /** How far a hoard with no fight to put it by looks down from where the dragon vanished for ground. */
    private static final int HOARD_DROP = 64;

    /** Dying dragons and the Wrath each was fought at, until it vanishes and its hoard is laid out. */
    private static final Map<EnderDragon, Integer> DYING = new HashMap<>();
    /** Where the last hoard was put, for the tests and the harness. */
    private static @Nullable BlockPos lastHoard;

    private DragonWrath() {}

    // ---- the numbers -------------------------------------------------------------------------------

    public static float maxHealth(int wrath) {
        return BASE_HEALTH * (1 + HEALTH_PER_WRATH * clamp(wrath));
    }

    public static int scales(int wrath) {
        return BASE_SCALES + clamp(wrath);
    }

    /** The stored Wrath after a kill: one more, up to the cap. */
    public static int next(int stored, int cap) {
        return Math.min(clamp(stored) + 1, clamp(cap));
    }

    /** The Wrath a fight is at: the stored value under the cap, or 0 with Wrath turned off. */
    public static int effective(int stored, boolean enabled, int cap) {
        return enabled ? Math.min(clamp(stored), clamp(cap)) : 0;
    }

    public static int clamp(int wrath) {
        return Mth.clamp(wrath, 0, MAX);
    }

    public static ResourceKey<LootTable> hoard(int wrath) {
        return ResourceKey.create(Registries.LOOT_TABLE, PromisedEnd.id("chests/dragon_hoard/wrath_" + clamp(wrath)));
    }

    // ---- the world's value -------------------------------------------------------------------------

    /** The stored Wrath, whatever the config says. */
    public static int stored(MinecraftServer server) {
        return Stored.of(server).wrath;
    }

    public static void setStored(MinecraftServer server, int wrath) {
        Stored data = Stored.of(server);
        data.wrath = clamp(wrath);
        data.setDirty();
    }

    /** The Wrath the next fight is at. */
    public static int effective(MinecraftServer server) {
        return effective(stored(server), EndConfig.dragonWrath(), EndConfig.maxWrath());
    }

    // ---- the tick ----------------------------------------------------------------------------------

    /** At the end of every server tick (both loaders), before the dragon's phase 3. */
    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (EnderDragon dragon : level.getEntities(EntityTypes.ENDER_DRAGON, d -> !d.isRemoved())) {
                if (!dragon.entityTags().contains(SCALED_TAG) && dragon.isAlive()) scale(dragon, effective(server));
                if (dragon.isDeadOrDying() && !DYING.containsKey(dragon)) DYING.put(dragon, foughtAt(dragon, server));
            }
        }
        for (Iterator<Map.Entry<EnderDragon, Integer>> it = DYING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<EnderDragon, Integer> e = it.next();
            EnderDragon dragon = e.getKey();
            if (dragon.level().getServer() != server) {
                it.remove(); // from a world closed earlier in this game
                continue;
            }
            if (!dragon.isRemoved()) continue;
            it.remove();
            if (dragon.getRemovalReason() != Entity.RemovalReason.KILLED || !(dragon.level() instanceof ServerLevel level)) continue;
            int wrath = e.getValue();
            lastHoard = layHoard(level, dragon, wrath);
            if (EndConfig.dragonWrath()) setStored(server, next(stored(server), EndConfig.maxWrath()));
        }
    }

    /** Scales a dragon's health by Wrath once, keeping its share of health (a fresh dragon stays at full). */
    public static void scale(EnderDragon dragon, int wrath) {
        dragon.addTag(SCALED_TAG);
        dragon.addTag(FOUGHT_AT_TAG + clamp(wrath));
        AttributeInstance max = dragon.getAttribute(Attributes.MAX_HEALTH);
        if (max == null || wrath <= 0) return;
        float share = dragon.getHealth() / dragon.getMaxHealth();
        max.addOrReplacePermanentModifier(new AttributeModifier(HEALTH_MODIFIER, HEALTH_PER_WRATH * clamp(wrath), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        dragon.setHealth(share * dragon.getMaxHealth());
    }

    /** The Wrath the dragon's health was scaled by, or the world's now for one that never was (a test's). */
    private static int foughtAt(EnderDragon dragon, MinecraftServer server) {
        for (String tag : dragon.entityTags()) {
            if (!tag.startsWith(FOUGHT_AT_TAG)) continue;
            try {
                return clamp(Integer.parseInt(tag.substring(FOUGHT_AT_TAG.length())));
            } catch (NumberFormatException e) {
                break;
            }
        }
        return effective(server);
    }

    // ---- the hoard ---------------------------------------------------------------------------------

    /**
     * A chest with the Wrath's hoard: on the island beside the exit portal, at the first of 8 spots round it with
     * ground under it; with no fight, on the ground under where the dragon vanished.
     */
    private static BlockPos layHoard(ServerLevel level, EnderDragon dragon, int wrath) {
        BlockPos at = dragon.getDragonFight() != null ? byPortal(level, dragon.getFightOrigin()) : under(level, dragon.blockPosition());
        level.setBlock(at, Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(at) instanceof ChestBlockEntity chest) chest.setLootTable(hoard(wrath), level.getRandom().nextLong());
        return at;
    }

    private static BlockPos byPortal(ServerLevel level, BlockPos origin) {
        BlockPos fallback = null;
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8;
            BlockPos column = origin.offset(Mth.floor(Math.cos(angle) * HOARD_DISTANCE + 0.5), 0, Mth.floor(Math.sin(angle) * HOARD_DISTANCE + 0.5));
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            if (top.getY() <= level.getMinY()) continue;
            if (fallback == null) fallback = top;
            if (free(level, top)) return top;
        }
        return fallback != null ? fallback : level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin);
    }

    private static BlockPos under(ServerLevel level, BlockPos from) {
        for (int dy = 0; dy < HOARD_DROP && from.getY() - dy > level.getMinY(); dy++) {
            BlockPos at = from.below(dy);
            if (free(level, at)) return at;
        }
        return from;
    }

    private static boolean free(ServerLevel level, BlockPos at) {
        BlockState here = level.getBlockState(at);
        return here.canBeReplaced() && here.getFluidState().isEmpty() && level.getBlockState(at.below()).isFaceSturdy(level, at.below(), Direction.UP);
    }

    /** Where the last hoard chest was set, or null before the first. */
    public static @Nullable BlockPos lastHoard() {
        return lastHoard;
    }

    /** The Wrath a dying dragon was fought at, or -1 if it isn't dying (for tests). */
    public static int dyingAt(EnderDragon dragon) {
        return DYING.getOrDefault(dragon, -1);
    }

    /** The saved value. Its own class, so the numbers above load without the world's storage. */
    private static final class Stored extends SavedData {
        static final Codec<Stored> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("wrath", 0).forGetter(s -> s.wrath)).apply(i, Stored::new));
        // the type wants a data fixer; nothing in vanilla's command-storage fix touches a lone "wrath" int
        static final SavedDataType<Stored> TYPE = new SavedDataType<>(PromisedEnd.id("dragon_wrath"), Stored::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

        int wrath;

        Stored() {}

        Stored(int wrath) {
            this.wrath = clamp(wrath);
        }

        static Stored of(MinecraftServer server) {
            return server.getDataStorage().computeIfAbsent(TYPE);
        }
    }
}
