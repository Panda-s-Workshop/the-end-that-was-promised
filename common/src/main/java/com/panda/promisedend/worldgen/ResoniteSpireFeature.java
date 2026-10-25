package com.panda.promisedend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.promisedend.PromisedEnd;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * A Resonant Spires formation: a few crystal columns of Resonite rising from one footing and leaning out
 * from it, the tallest {@code height} blocks (10-30), the others shorter. Each column is a six-sided prism that
 * narrows to a point. Some of its face blocks are Budding Resonite, and clusters grow on the open faces beside them
 * and here and there elsewhere. Fields are its configuration ({@code worldgen/feature/resonite_spire.json}).
 *
 * <p>Each column's footing must be {@code #promisedend:resonite_spire_ground} at the surface, within a block of the
 * first's, so a formation never stands in the air, hangs off a slope or stands on another's crystal. A formation that
 * would meet anything but air, its ground, a plant or another formation's Resonite, or any block of it under a roof, or
 * touch a chunk that any structure reaches (End Cities' pieces go in later, chunk by chunk), isn't made: it keeps out of
 * structures, the return gateway and a grove across a border. Sideways it stays within {@link #REACH} blocks of
 * the origin (2.5 off, then a lean of up to 0.45 a block over at most 0.75 of the codec's tallest 32, and a cluster),
 * inside the chunks a feature may write to.
 */
public record ResoniteSpireFeature(BlockStateProvider crystal, BlockStateProvider budding, BlockStateProvider cluster,
                                   IntProvider height, IntProvider columns, float buddingChance, float clusterChance) implements Feature {
    public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, PromisedEnd.id("resonite_spire_ground"));
    /** The chance of a cluster on each open side of a budding block at generation. */
    public static final float BUDDING_CLUSTER_CHANCE = 0.25F;
    /** The farthest a formation writes from its origin, sideways: the codec's tallest (32) leaning column and a cluster. */
    public static final int REACH = 14;
    public static final MapCodec<ResoniteSpireFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockStateProvider.DIRECT_CODEC.fieldOf("crystal").forGetter(ResoniteSpireFeature::crystal),
            BlockStateProvider.DIRECT_CODEC.fieldOf("budding").forGetter(ResoniteSpireFeature::budding),
            BlockStateProvider.DIRECT_CODEC.fieldOf("cluster").forGetter(ResoniteSpireFeature::cluster),
            IntProviders.codec(4, 32).fieldOf("height").forGetter(ResoniteSpireFeature::height),
            IntProviders.codec(1, 8).fieldOf("columns").forGetter(ResoniteSpireFeature::columns),
            Codec.floatRange(0, 1).fieldOf("budding_chance").forGetter(ResoniteSpireFeature::buddingChance),
            Codec.floatRange(0, 1).fieldOf("cluster_chance").forGetter(ResoniteSpireFeature::clusterChance)
    ).apply(i, ResoniteSpireFeature::new));

    @Override
    public MapCodec<ResoniteSpireFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockPos foot = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, origin);
        if (!level.getBlockState(foot.below()).is(GROUND)) return false;
        int tallest = height.sample(random);
        int count = columns.sample(random);
        Set<BlockPos> body = new LinkedHashSet<>();
        for (int c = 0; c < count; c++) {
            // the first stands near upright; the others lean out round it, shorter
            int h = c == 0 ? tallest : Math.max(4, Math.round(tallest * Mth.nextFloat(random, 0.35F, 0.75F)));
            float angle = random.nextFloat() * Mth.TWO_PI;
            float lean = c == 0 ? Mth.nextFloat(random, 0.0F, 0.12F) : Mth.nextFloat(random, 0.18F, 0.45F);
            float radius = Mth.clamp(h / 9.0F, 1.0F, 2.6F);
            float bx = c == 0 ? 0 : Mth.cos(angle) * Mth.nextFloat(random, 1.0F, 2.5F);
            float bz = c == 0 ? 0 : Mth.sin(angle) * Mth.nextFloat(random, 1.0F, 2.5F);
            // each column stands on its own ground, at most a block off the first's: none hangs off a slope or an edge
            BlockPos base = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, foot.offset(Mth.floor(bx), 0, Mth.floor(bz)));
            if (Math.abs(base.getY() - foot.getY()) > 1 || !level.getBlockState(base.below()).is(GROUND)) {
                if (c == 0) return false;
                continue;
            }
            int lastX = 0, lastZ = 0;
            // the column goes a block into the ground, so it never shows a gap at its foot
            // it narrows to a needle a block wide, and its tip is its h-th block, so the codec's height is the height seen
            for (int dy = -1; dy < h; dy++) {
                float t = Math.max(0, dy) / (float) h;
                float r = Math.max(0.5F, radius * (1.0F - t * t));
                float cx = bx + Mth.cos(angle) * lean * dy, cz = bz + Mth.sin(angle) * lean * dy;
                int reach = Mth.ceil(r);
                for (int dx = -reach; dx <= reach; dx++) {
                    for (int dz = -reach; dz <= reach; dz++) {
                        // a hexagon, not a circle: crystal faces
                        float px = dx - (cx - Mth.floor(cx)), pz = dz - (cz - Mth.floor(cz));
                        if (Math.max(Math.abs(px) * 0.866F + Math.abs(pz) * 0.5F, Math.abs(pz)) > r + 0.25F) continue;
                        body.add(base.offset(Mth.floor(cx) + dx - Mth.floor(bx), dy, Mth.floor(cz) + dz - Mth.floor(bz)));
                    }
                }
                // a thin top that steps sideways keeps a block under its new place, so it never hangs by an edge
                if (r < 1.5F && dy > 0 && (Mth.floor(cx) != lastX || Mth.floor(cz) != lastZ)) {
                    body.add(base.offset(Mth.floor(cx) - Mth.floor(bx), dy - 1, Mth.floor(cz) - Mth.floor(bz)));
                }
                lastX = Mth.floor(cx);
                lastZ = Mth.floor(cz);
                if (dy == h - 1) body.add(base.offset(Mth.floor(cx) - Mth.floor(bx), dy, Mth.floor(cz) - Mth.floor(bz)));
            }
        }
        // keep out: a formation that would meet anything but air, its ground, a plant or Resonite, or stand under a roof
        // (an End City, the return gateway, a grove across a border), isn't made at all
        // nor in a chunk a structure has claimed, whose pieces may not be placed yet
        if (StructureClaims.claimed(level, body)) return false;
        // another formation's crystal, budding blocks and clusters are open too: neighbouring spires grow into one clump
        Set<Block> ours = Set.of(crystal.getState(level, random, foot).getBlock(), budding.getState(level, random, foot).getBlock(),
                cluster.getState(level, random, foot).getBlock());
        java.util.function.Predicate<BlockState> open = here -> here.isAir() || here.is(GROUND) || here.canBeReplaced() || ours.contains(here.getBlock());
        for (BlockPos p : body) {
            if (!open.test(level.getBlockState(p))) return false;
            // and nothing over it but more of the same (a hillside's end stone is fine, an overhang of anything else isn't)
            int surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, p).getY();
            for (int up = p.getY() + 1; up < surface; up++) {
                if (!open.test(level.getBlockState(p.atY(up)))) return false;
            }
        }
        for (BlockPos p : body) level.setBlock(p, crystal.getState(level, random, p), Block.UPDATE_CLIENTS);
        // budding blocks on the faces, clusters on the open sides next to them and now and then anywhere
        List<BlockPos> faces = new ArrayList<>();
        for (BlockPos p : body) {
            for (Direction d : Direction.values()) {
                if (!body.contains(p.relative(d)) && level.getBlockState(p.relative(d)).isAir()) {
                    faces.add(p);
                    break;
                }
            }
        }
        for (BlockPos p : faces) {
            boolean bud = random.nextFloat() < buddingChance;
            if (bud) level.setBlock(p, budding.getState(level, random, p), Block.UPDATE_CLIENTS);
            for (Direction d : Direction.values()) {
                BlockPos out = p.relative(d);
                if (!level.getBlockState(out).isAir() || random.nextFloat() >= (bud ? BUDDING_CLUSTER_CHANCE : clusterChance)) continue;
                BlockState grown = cluster.getState(level, random, out);
                if (grown.hasProperty(AmethystClusterBlock.FACING)) grown = grown.setValue(AmethystClusterBlock.FACING, d);
                level.setBlock(out, grown, Block.UPDATE_CLIENTS);
            }
        }
        return !body.isEmpty();
    }
}
