package com.panda.promisedend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.promisedend.EndConfig;
import com.panda.promisedend.PromisedEnd;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * A Voidscar rift scar: a jagged crack across an island's top, {@code length} blocks long, {@link #WIDEST} blocks
 * wide and {@code depth} deep at its middle, closing to a block at each end, its line wandering a little either side
 * as it goes; on an island too small for it (a quarter of its line off the ground), it is tried shorter, down to
 * {@link #SHORTEST}. It is cut down into each column's own surface, and through the island where the island is
 * thinner, so the void shows at the bottom. Its walls and floor are lined with {@code wall} (Voidstone),
 * {@code oreChance} of the lining {@code ore} (Voidsteel Scrap Ore, never with the void under it), so the scar glows
 * where the scrap is.
 *
 * <p>It starts at its spot, or over the void on the middle of its chunk's ground, and cuts only
 * {@code #promisedend:rift_scar_ground} (end stone, Voidstone) columns, and a scar that would cut into anything else
 * but air or a plant, or touch a chunk a structure has claimed, isn't made at all. Sideways it stays within
 * {@link #REACH} blocks of where it starts (half the longest scar, the wander, its half width and the lining), and it
 * starts in its spot's chunk, so it stays inside the chunks a feature may write to. {@code fallback} marks the
 * sparser copy on vanilla's barrens ({@code #promisedend:voidscar_fallback}), which builds only in the End with our
 * terrain off.
 */
public record RiftScarFeature(BlockStateProvider wall, BlockStateProvider ore, IntProvider length, IntProvider depth, float oreChance,
                              boolean fallback) implements Feature {
    public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, PromisedEnd.id("rift_scar_ground"));
    public static final int WIDEST = 3;
    /** The shortest a scar is tried at on a small island (the codec's least). */
    public static final int SHORTEST = 6;
    /** How far either side of its straight line the scar's middle wanders. */
    public static final float WANDER = 1.5F;
    /** The farthest it writes sideways from its origin: 11 (half the codec's longest, 22), the wander, 1.5 and the lining. */
    public static final int REACH = 15;
    public static final MapCodec<RiftScarFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockStateProvider.DIRECT_CODEC.fieldOf("wall").forGetter(RiftScarFeature::wall),
            BlockStateProvider.DIRECT_CODEC.fieldOf("ore").forGetter(RiftScarFeature::ore),
            IntProviders.codec(6, 22).fieldOf("length").forGetter(RiftScarFeature::length),
            IntProviders.codec(2, 12).fieldOf("depth").forGetter(RiftScarFeature::depth),
            Codec.floatRange(0, 1).fieldOf("ore_chance").forGetter(RiftScarFeature::oreChance),
            Codec.BOOL.optionalFieldOf("fallback", false).forGetter(RiftScarFeature::fallback)
    ).apply(i, RiftScarFeature::new));

    @Override
    public MapCodec<RiftScarFeature> codec() {
        return CODEC;
    }

    /**
     * The blocks a scar {@code len} long and {@code deep} deep along {@code angle} cuts from {@code start}: each column
     * down from its own surface, and none where the column's top isn't the scar's ground. {@code onGround} gets how many
     * of the line's {@code len + 1} steps have that ground under their middle.
     */
    private static Set<BlockPos> carve(WorldGenLevel level, BlockPos start, int len, int deep, float angle, RandomSource random, int[] onGround) {
        float dx = Mth.cos(angle), dz = Mth.sin(angle), wander = 0;
        Set<BlockPos> cut = new LinkedHashSet<>();
        onGround[0] = 0;
        for (int i = 0; i <= len; i++) {
            float t = (float) i / len, middle = Mth.sin(t * Mth.PI);
            wander = Mth.clamp(wander + (random.nextFloat() - 0.5F) * 0.8F, -WANDER, WANDER);
            float along = i - len / 2.0F;
            float cx = dx * along - dz * wander, cz = dz * along + dx * wander;
            float half = 0.5F + middle * (WIDEST - 1) / 2.0F;
            int down = Math.max(1, Math.round(deep * middle));
            int reach = Mth.ceil(half);
            if (level.getBlockState(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, start.offset(Mth.floor(cx), 0, Mth.floor(cz))).below()).is(GROUND)) onGround[0]++;
            for (int ox = -reach; ox <= reach; ox++) {
                for (int oz = -reach; oz <= reach; oz++) {
                    int x = Mth.floor(cx) + ox, z = Mth.floor(cz) + oz;
                    if (Mth.square(x + 0.5F - cx) + Mth.square(z + 0.5F - cz) > half * half + 0.25F) continue;
                    // down from this column's own surface: a scar that runs off the island's edge simply ends there
                    BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, start.offset(x, 0, z)).below();
                    if (!level.getBlockState(top).is(GROUND)) continue;
                    for (int d = 0; d < down; d++) cut.add(top.below(d));
                }
            }
        }
        return cut;
    }

    /**
     * Where the scar starts: over the ground at {@code origin}, or else the ground column of its chunk nearest the middle
     * of the chunk's ground (over the barrens most spots are over the void, and the ground nearest such a spot is an
     * island's rim, where the scar would be half off the island). Null when the chunk has none.
     */
    private static BlockPos land(WorldGenLevel level, BlockPos origin) {
        BlockPos here = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, origin);
        if (ground(level, here)) return here;
        int x0 = Math.floorDiv(origin.getX(), 16) * 16, z0 = Math.floorDiv(origin.getZ(), 16) * 16;
        java.util.List<BlockPos> tops = new java.util.ArrayList<>();
        long sx = 0, sz = 0;
        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
                if (!ground(level, top)) continue;
                tops.add(top);
                sx += x;
                sz += z;
            }
        }
        if (tops.isEmpty()) return null;
        double mx = (double) sx / tops.size(), mz = (double) sz / tops.size();
        return tops.stream().min(java.util.Comparator.comparingDouble(t -> Mth.square(t.getX() - mx) + Mth.square(t.getZ() - mz))).orElseThrow();
    }

    private static boolean ground(WorldGenLevel level, BlockPos top) {
        return top.getY() > level.getMinY() + 1 && level.getBlockState(top.below()).is(GROUND);
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        // the copy on vanilla's barrens is for packs with our terrain off, where the Voidscar never generates
        if (fallback && (EndConfig.terrainEnabled() || level.getLevel().dimension() != Level.END)) return false;
        BlockPos start = land(level, origin);
        if (start == null) return false;
        int len = length.sample(random), deep = depth.sample(random);
        float angle = random.nextFloat() * Mth.TWO_PI;
        // on an island too small for it (a quarter of its line off the ground) the scar is tried again two thirds as
        // long, down to SHORTEST
        int[] onGround = {0};
        Set<BlockPos> cut = carve(level, start, len, deep, angle, random, onGround);
        while (onGround[0] * 4 < (len + 1) * 3 && len > SHORTEST) {
            len = Math.max(SHORTEST, len * 2 / 3);
            cut = carve(level, start, len, deep, angle, random, onGround);
        }
        if (onGround[0] * 2 < len + 1) return false;
        // the lining: the ground round the cut, below it and to its sides
        Set<BlockPos> lining = new LinkedHashSet<>();
        for (BlockPos p : cut) {
            for (Direction d : Direction.values()) {
                if (d == Direction.UP) continue;
                BlockPos side = p.relative(d);
                if (!cut.contains(side) && level.getBlockState(side).is(GROUND)) lining.add(side);
            }
        }
        // keep out: nothing but the ground, air and plants in the cut, and no chunk a structure has claimed
        for (BlockPos p : cut) {
            BlockState here = level.getBlockState(p);
            if (!(here.isAir() || here.is(GROUND) || here.canBeReplaced())) return false;
        }
        Set<BlockPos> all = new LinkedHashSet<>(cut);
        all.addAll(lining);
        if (StructureClaims.claimed(level, all)) return false;
        for (BlockPos p : cut) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        for (BlockPos p : lining) {
            // no ore with the void under it: mined, its scrap would fall away
            boolean overAir = cut.contains(p.below()) || level.getBlockState(p.below()).isAir();
            BlockState lined = !overAir && random.nextFloat() < oreChance ? ore.getState(level, random, p) : wall.getState(level, random, p);
            level.setBlock(p, lined, Block.UPDATE_CLIENTS);
        }
        return true;
    }
}
