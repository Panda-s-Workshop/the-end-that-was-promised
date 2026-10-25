package com.panda.promisedend.logic;

import com.panda.promisedend.EndConfig;
import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.worldgen.EndRegions;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * The outer End's advancements by where the player is and, with our terrain off, by what grows there. Outside vanilla's
 * main-island area ({@link EndRegions#mainArea}, the same line the biomes use) is the outer End, which awards
 * {@code outer/enter_outer}. With terrain off our biomes never generate, so each biome visit's second criterion,
 * {@code in_fallback}, is awarded here instead: out there, within {@link #NEAR} blocks of a block in that biome's
 * {@code #promisedend:visit_marker/<biome>} tag (what the terrain-off copies of its features build on vanilla's
 * highlands and midlands). With terrain on the biome criterion does it.
 */
public final class OuterProgress {
    public static final Identifier ENTER_OUTER = PromisedEnd.id("outer/enter_outer");
    /** The outer biomes that have a visit advancement, {@code outer/visit_<biome>}. */
    public static final List<String> BIOMES = List.of("chorus_thicket", "lumen_mire", "resonant_spires");
    public static final int NEAR = 4;
    /** The terrain-off check reads a 9-block cube round the player, so it runs every other distance check. */
    public static final int MARKER_INTERVAL = RimProgress.CHECK_INTERVAL * 2;

    private OuterProgress() {}

    public static Identifier visit(String biome) {
        return PromisedEnd.id("outer/visit_" + biome);
    }

    public static TagKey<Block> marker(String biome) {
        return TagKey.create(Registries.BLOCK, PromisedEnd.id("visit_marker/" + biome));
    }

    /** Whether a block is in the outer End: outside the main-island area, chunk by chunk, as the biomes are. */
    public static boolean outer(BlockPos pos) {
        return !EndRegions.mainArea(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    public static void tick(ServerPlayer player) {
        if (player.tickCount % RimProgress.CHECK_INTERVAL != 0 || player.level().dimension() != Level.END || player.isSpectator()) return;
        BlockPos at = player.blockPosition();
        if (!outer(at)) return;
        var advancements = player.level().getServer().getAdvancements();
        var enter = advancements.get(ENTER_OUTER);
        if (enter != null) player.getAdvancements().award(enter, "far_out");
        if (EndConfig.terrainEnabled() || player.tickCount % MARKER_INTERVAL != 0) return;
        // the visits still to earn, and their markers: one pass over the cube for all of them
        List<AdvancementHolder> pending = new ArrayList<>();
        List<TagKey<Block>> markers = new ArrayList<>();
        for (String biome : BIOMES) {
            var advancement = advancements.get(visit(biome));
            if (advancement == null || player.getAdvancements().getOrStartProgress(advancement).isDone()) continue;
            pending.add(advancement);
            markers.add(marker(biome));
        }
        if (pending.isEmpty() || !loaded(player.level(), at)) return;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-NEAR, -NEAR, -NEAR), at.offset(NEAR, NEAR, NEAR))) {
            var state = player.level().getBlockState(p);
            for (int i = 0; i < pending.size(); i++) {
                if (state.is(markers.get(i))) {
                    player.getAdvancements().award(pending.remove(i), "in_fallback");
                    markers.remove(i);
                    if (pending.isEmpty()) return;
                    break;
                }
            }
        }
    }

    /** Whether every chunk the cube round {@code at} touches is loaded, so the check never loads one itself. */
    private static boolean loaded(ServerLevel level, BlockPos at) {
        for (int cx = SectionPos.blockToSectionCoord(at.getX() - NEAR); cx <= SectionPos.blockToSectionCoord(at.getX() + NEAR); cx++) {
            for (int cz = SectionPos.blockToSectionCoord(at.getZ() - NEAR); cz <= SectionPos.blockToSectionCoord(at.getZ() + NEAR); cz++) {
                if (level.getChunkSource().getChunkNow(cx, cz) == null) return false;
            }
        }
        return true;
    }
}
