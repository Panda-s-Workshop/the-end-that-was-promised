package com.panda.promisedend.logic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.promisedend.EndConfig;
import com.panda.promisedend.PromisedEnd;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.panda.promisedend.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
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
    /**
     * Where the Ancients' Ward keeps the dragon for last, it is the journey's finale, fought in Voidsteel after all three
     * guardians: its health is this many times what Wrath gives it (450 at Wrath 0, vanilla's at Wrath 5; 1012.5 at
     * Wrath 5, under vanilla's most of 1024). Without the Ward it may be the first fight, and stays vanilla's.
     */
    public static final float FINALE = 2.25F;
    public static final Identifier FINALE_MODIFIER = PromisedEnd.id("finale");
    /** How far from the portal's centre the hoard chest is set: just off the podium's rim. */
    private static final int HOARD_DISTANCE = EndPodiumFeature.PODIUM_RADIUS + EndPodiumFeature.RIM_RADIUS + 1;
    /** How far a hoard with no fight to put it by looks down from where the dragon vanished for ground. */
    private static final int HOARD_DROP = 64;

    /** Dying dragons and the Wrath each was fought at, until it vanishes and its hoard is laid out. */
    private static final Map<EnderDragon, Integer> DYING = new HashMap<>();
    /** How far from the hoard players hear that Wrath rose. */
    private static final double ANNOUNCE_RANGE = 192.0;
    /** How many blocks out from the portal's centre the hoard looks for a free spot, in rings of 8. */
    private static final int HOARD_RINGS = 4;

    /** Where the last hoard was put, the last Wrath line and how many players it went to, for the tests and the harness. */
    private static @Nullable BlockPos lastHoard;
    /** The level of the hoard still glimmering, null once it was opened, broken or its time ran out. */
    private static @Nullable ServerLevel glimmerLevel;
    private static int glimmerTicks;
    /** How long an unopened hoard glimmers at most: 5 minutes. */
    private static final int GLIMMER_TICKS = 6000;
    private static String lastAnnouncement = "";
    private static int told;

    private DragonWrath() {}

    // ---- the numbers -------------------------------------------------------------------------------

    public static float maxHealth(int wrath) {
        return BASE_HEALTH * (1 + HEALTH_PER_WRATH * clamp(wrath));
    }

    /** Its health at {@code wrath}, as the finale ({@link #FINALE}) or not. */
    public static float maxHealth(int wrath, boolean finale) {
        return maxHealth(wrath) * (finale ? FINALE : 1.0F);
    }

    /** Whether the dragon is fought last: the Ward stands between it and the outer End, with a seal still to break. */
    public static boolean finale(MinecraftServer server) {
        return EndConfig.arrivalByCoordinates() && AncientWard.guarded(server) != 0;
    }

    public static int scales(int wrath) {
        return BASE_SCALES + clamp(wrath);
    }

    /** The stored Wrath after a kill: one more, up to the cap; a value stored above a lowered cap is kept (it counts as the cap). */
    public static int next(int stored, int cap) {
        return Math.max(clamp(stored), Math.min(clamp(stored) + 1, clamp(cap)));
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
        if (glimmerLevel != null && glimmerLevel.getServer() != server) glimmerLevel = null;
        glimmer();
        for (ServerLevel level : server.getAllLevels()) {
            for (EnderDragon dragon : level.getEntities(EntityTypes.ENDER_DRAGON, d -> !d.isRemoved())) {
                if (!dragon.entityTags().contains(SCALED_TAG) && dragon.isAlive()) scale(dragon, effective(server), finale(server));
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
            if (!EndConfig.dragonWrath()) continue;
            int was = stored(server), now = next(was, EndConfig.maxWrath());
            setStored(server, now);
            // the players at the fight are told, so a rematch is a choice they know about; with the cap at 0 there is
            // no Wrath to tell of
            int cap = clamp(EndConfig.maxWrath());
            if (cap == 0) continue;
            Component line = now > was ? Component.translatable("message.promisedend.wrath.rises", now, cap)
                    : Component.translatable("message.promisedend.wrath.height", Math.min(now, cap));
            lastAnnouncement = line.getString();
            told = 0;
            for (var player : level.players()) {
                if (player.distanceToSqr(Vec3.atCenterOf(lastHoard == null ? dragon.blockPosition() : lastHoard)) < ANNOUNCE_RANGE * ANNOUNCE_RANGE)
                {
                    player.sendSystemMessage(line.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
                    told++;
                }
            }
        }
    }

    /**
     * Scales a dragon's health by Wrath once, and by {@link #FINALE} when it is fought last, keeping its share of health
     * (a fresh dragon stays at full), and names it for its Wrath ("Ender Dragon · Wrath 2"), which its boss bar shows; a
     * dragon already named keeps its name.
     */
    public static void scale(EnderDragon dragon, int wrath, boolean finale) {
        dragon.addTag(SCALED_TAG);
        dragon.addTag(FOUGHT_AT_TAG + clamp(wrath));
        AttributeInstance max = dragon.getAttribute(Attributes.MAX_HEALTH);
        if (max == null || wrath <= 0 && !finale) return;
        if (wrath > 0 && !dragon.hasCustomName()) dragon.setCustomName(Component.translatable("entity.promisedend.ender_dragon.wrath", dragon.getType().getDescription(), clamp(wrath)));
        float share = dragon.getHealth() / dragon.getMaxHealth();
        if (wrath > 0) max.addOrReplacePermanentModifier(new AttributeModifier(HEALTH_MODIFIER, HEALTH_PER_WRATH * clamp(wrath), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        if (finale) max.addOrReplacePermanentModifier(new AttributeModifier(FINALE_MODIFIER, FINALE - 1.0F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        dragon.setHealth(share * dragon.getMaxHealth());
    }

    /** The Wrath the dragon's health was scaled by, or the world's now for one that never was (a test's). */
    public static int foughtAt(EnderDragon dragon, MinecraftServer server) {
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
     * The Wrath's hoard in a chest named "Dragon Hoard": on the island beside the exit portal, at the first spot round it
     * that is empty and on solid ground (rings of 8, from just off the podium's rim outwards); with no fight, on the
     * ground under where the dragon vanished. Where there is no such spot, nothing is replaced: the hoard drops as items
     * (returns null).
     */
    private static @Nullable BlockPos layHoard(ServerLevel level, EnderDragon dragon, int wrath) {
        BlockPos at = dragon.getDragonFight() != null ? byPortal(level, dragon.getFightOrigin()) : under(level, dragon.blockPosition());
        if (at == null) {
            // on the island off the podium's rim, not over the portal, and with no speed, so nothing rolls into it
            Vec3 drop = dragon.getDragonFight() != null ? Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    dragon.getFightOrigin().offset(HOARD_DISTANCE, 0, 0))).add(0, 0.25, 0) : dragon.position();
            for (ItemStack stack : roll(level, wrath, drop)) level.addFreshEntity(new ItemEntity(level, drop.x, drop.y, drop.z, stack, 0, 0, 0));
            return null;
        }
        level.setBlock(at, Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(at) instanceof ChestBlockEntity chest) {
            // named first: applying components also applies the (empty) contents, which would clear the items
            Component name = wrath > 0 ? Component.translatable("container.promisedend.dragon_hoard.wrath", clamp(wrath)) : Component.translatable("container.promisedend.dragon_hoard");
            chest.applyComponents(DataComponentMap.builder().set(DataComponents.CUSTOM_NAME, name).build(), DataComponentPatch.EMPTY);
            // laid out, not scattered: the stacks side by side along the middle row
            List<ItemStack> items = roll(level, wrath, Vec3.atCenterOf(at));
            int row = chest.getContainerSize() / 3, first = row + Math.max(0, (row - items.size()) / 2);
            for (int i = 0; i < items.size() && first + i < chest.getContainerSize(); i++) chest.setItem(first + i, items.get(i));
            chest.setChanged();
        }
        level.playSound(null, at, ModSounds.DRAGON_HOARD_APPEARS.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
        level.sendParticles(ParticleTypes.END_ROD, at.getX() + 0.5, at.getY() + 0.8, at.getZ() + 0.5, 30, 0.4, 0.5, 0.4, 0.05);
        glimmerLevel = level;
        glimmerTicks = 0;
        return at;
    }

    /** The Wrath's hoard table rolled once, with like items merged into full stacks. */
    private static List<ItemStack> roll(ServerLevel level, int wrath, Vec3 origin) {
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin).create(LootContextParamSets.CHEST);
        List<ItemStack> merged = new ArrayList<>();
        level.getServer().reloadableRegistries().getLootTable(hoard(wrath)).getRandomItems(params, stack -> {
            for (ItemStack into : merged) {
                if (stack.isEmpty()) break;
                if (ItemStack.isSameItemSameComponents(into, stack) && into.getCount() < into.getMaxStackSize()) {
                    int moved = Math.min(stack.getCount(), into.getMaxStackSize() - into.getCount());
                    into.grow(moved);
                    stack.shrink(moved);
                }
            }
            if (!stack.isEmpty()) merged.add(stack.copy());
        });
        return merged;
    }

    /**
     * A slow glimmer over the last hoard until a player first opens it (or for {@link #GLIMMER_TICKS}): its sparks at
     * placement come during the dying, when no one is looking at the ground.
     */
    private static void glimmer() {
        if (glimmerLevel == null || lastHoard == null) return;
        ServerLevel level = glimmerLevel;
        if (++glimmerTicks > GLIMMER_TICKS || !(level.getBlockEntity(lastHoard) instanceof ChestBlockEntity) || ChestBlockEntity.getOpenCount(level, lastHoard) > 0) {
            glimmerLevel = null;
            return;
        }
        if (glimmerTicks % 10 == 0) level.sendParticles(ParticleTypes.END_ROD, lastHoard.getX() + 0.5, lastHoard.getY() + 1.2, lastHoard.getZ() + 0.5, 1, 0.25, 0.3, 0.25, 0.01);
    }

    private static @Nullable BlockPos byPortal(ServerLevel level, BlockPos origin) {
        for (int ring = 0; ring < HOARD_RINGS; ring++) {
            int distance = HOARD_DISTANCE + ring;
            for (int i = 0; i < 8; i++) {
                double angle = Math.PI * 2 * (i + ring * 0.5) / 8;
                BlockPos column = origin.offset(Mth.floor(Math.cos(angle) * distance + 0.5), 0, Mth.floor(Math.sin(angle) * distance + 0.5));
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
                if (top.getY() > level.getMinY() && free(level, top)) return top;
            }
        }
        return null;
    }

    private static @Nullable BlockPos under(ServerLevel level, BlockPos from) {
        for (int dy = 0; dy < HOARD_DROP && from.getY() - dy > level.getMinY(); dy++) {
            BlockPos at = from.below(dy);
            if (free(level, at)) return at;
        }
        return null;
    }

    /**
     * Air or what a placed block replaces anyway (snow, short grass), never a torch, a carpet or a flower, on a solid
     * face, with no one and nothing standing in it.
     */
    private static boolean free(ServerLevel level, BlockPos at) {
        var here = level.getBlockState(at);
        return (here.isAir() || here.canBeReplaced() && here.getFluidState().isEmpty() && !here.is(net.minecraft.tags.BlockTags.FLOWERS))
                && level.getBlockState(at.below()).isFaceSturdy(level, at.below(), Direction.UP)
                && level.getEntities((Entity) null, new net.minecraft.world.phys.AABB(at)).isEmpty();
    }

    /** Where the last hoard chest was set, or null before the first (and after a hoard that dropped as items). */
    public static @Nullable BlockPos lastHoard() {
        return lastHoard;
    }

    /** The last line that told players Wrath rose (or is at its height), and to how many players it went. */
    public static String lastAnnouncement() {
        return lastAnnouncement + " (to " + told + ")";
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
