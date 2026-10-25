package com.panda.promisedend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.promisedend.EndConfig;
import com.panda.promisedend.PromisedEnd;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * A Chorus Thicket grove: a few chorus timber trees standing close, each a trunk with roots splayed at its foot and
 * limbs that climb out and up from its upper half, and vanilla's own chorus plant grown from the trunk's top and every
 * limb's tip (chorus timber is in {@code #minecraft:supports_chorus_plant}), so the canopies meet overhead. Fields are
 * its configuration ({@code worldgen/feature/chorus_grove.json}).
 *
 * <p>Everything stays within {@code radius + 3 + canopy_spread} blocks of the origin, inside the chunks a feature may
 * write to. A trunk needs {@code #promisedend:chorus_grove_ground} under it (not timber, so no tree stands on another's
 * root) and clear air for its height and two more: never a trunk through an overhang. {@code fallback} marks the copy
 * on {@code #promisedend:grove_fallback} (vanilla's highlands), which builds only in the End with our terrain off.
 * Where a structure has claimed a chunk the grove could reach (an End City in the Thicket), it builds nothing, so a
 * city put down later never cuts through a tree.
 */
public record ChorusGroveFeature(BlockStateProvider trunk, IntProvider trees, int radius, IntProvider height, IntProvider limbs,
                                 int canopySpread, boolean fallback) implements Feature {
    public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, PromisedEnd.id("chorus_grove_ground"));
    public static final MapCodec<ChorusGroveFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockStateProvider.DIRECT_CODEC.fieldOf("trunk").forGetter(ChorusGroveFeature::trunk),
            IntProviders.codec(1, 8).fieldOf("trees").forGetter(ChorusGroveFeature::trees),
            Codec.intRange(0, 6).fieldOf("radius").forGetter(ChorusGroveFeature::radius),
            IntProviders.codec(3, 16).fieldOf("height").forGetter(ChorusGroveFeature::height),
            IntProviders.codec(0, 4).fieldOf("limbs").forGetter(ChorusGroveFeature::limbs),
            Codec.intRange(1, 6).fieldOf("canopy_spread").forGetter(ChorusGroveFeature::canopySpread),
            Codec.BOOL.optionalFieldOf("fallback", false).forGetter(ChorusGroveFeature::fallback)
    ).apply(i, ChorusGroveFeature::new));

    @Override
    public MapCodec<ChorusGroveFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        // the copy on vanilla's highlands is for packs with our terrain off, where the Thicket never generates
        if (fallback && (EndConfig.terrainEnabled() || level.getLevel().dimension() != Level.END)) return false;
        // every chunk the grove can reach, sampled closer than a chunk apart
        int reach = radius + 3 + canopySpread;
        List<BlockPos> span = new ArrayList<>();
        for (int dx = -reach; dx <= reach; dx += Math.max(1, Math.min(8, reach))) {
            for (int dz = -reach; dz <= reach; dz += Math.max(1, Math.min(8, reach))) span.add(origin.offset(dx, 0, dz));
        }
        for (int d : new int[] {-reach, reach}) {
            span.add(origin.offset(d, 0, reach));
            span.add(origin.offset(reach, 0, d));
        }
        if (StructureClaims.claimed(level, span)) return false;
        List<BlockPos> trunks = new ArrayList<>();
        int wanted = trees.sample(random);
        for (int attempt = 0; attempt < wanted * 3 && trunks.size() < wanted; attempt++) {
            int dx = radius == 0 ? 0 : Mth.randomBetweenInclusive(random, -radius, radius);
            int dz = radius == 0 ? 0 : Mth.randomBetweenInclusive(random, -radius, radius);
            BlockPos foot = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, origin.offset(dx, 0, dz));
            // two blocks between trunks, so each has room for its roots and limbs
            if (trunks.stream().anyMatch(t -> Math.abs(t.getX() - foot.getX()) <= 2 && Math.abs(t.getZ() - foot.getZ()) <= 2)) continue;
            if (tree(level, random, foot)) trunks.add(foot);
        }
        return !trunks.isEmpty();
    }

    /** One tree at {@code foot}, or nothing if the ground or the air above it won't do. */
    private boolean tree(WorldGenLevel level, RandomSource random, BlockPos foot) {
        if (!level.getBlockState(foot.below()).is(GROUND)) return false;
        int h = height.sample(random);
        for (int dy = 0; dy < h + 2; dy++) {
            if (!level.getBlockState(foot.above(dy)).isAir()) return false;
        }
        for (int dy = 0; dy < h; dy++) setBlock(level, foot.above(dy), log(level, random, foot.above(dy), Direction.Axis.Y));
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos root = foot.relative(side);
            if (random.nextInt(3) == 0 && level.getBlockState(root).isAir() && level.getBlockState(root.below()).isFaceSturdy(level, root.below(), Direction.UP)) {
                setBlock(level, root, log(level, random, root, side.getAxis()));
            }
        }
        // limbs: each from a different side of the upper half, a stair of timber out and up, two or three blocks out
        List<Direction> sides = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
        net.minecraft.util.Util.shuffle(sides, random);
        int n = Math.min(limbs.sample(random), sides.size());
        for (int l = 0; l < n; l++) {
            Direction side = sides.get(l);
            BlockPos at = foot.above(Mth.randomBetweenInclusive(random, h / 2, h - 2));
            int length = 2 + random.nextInt(2);
            boolean whole = true;
            for (int step = 1; step <= length && whole; step++) {
                at = at.relative(side);
                if (!level.getBlockState(at).isAir()) whole = false;
                else setBlock(level, at, log(level, random, at, side.getAxis()));
                if (whole && step < length) {
                    at = at.above();
                    if (!level.getBlockState(at).isAir()) whole = false;
                    else setBlock(level, at, log(level, random, at, Direction.Axis.Y));
                }
            }
            if (whole && level.getBlockState(at.above()).isAir()) ChorusFlowerBlock.generatePlant(level, at.above(), random, canopySpread);
        }
        ChorusFlowerBlock.generatePlant(level, foot.above(h), random, canopySpread);
        return true;
    }

    private BlockState log(WorldGenLevel level, RandomSource random, BlockPos at, Direction.Axis axis) {
        BlockState state = trunk.getState(level, random, at);
        return state.hasProperty(RotatedPillarBlock.AXIS) ? state.setValue(RotatedPillarBlock.AXIS, axis) : state;
    }
}
