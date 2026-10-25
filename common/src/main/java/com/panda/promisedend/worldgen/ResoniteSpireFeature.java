package com.panda.promisedend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.panda.promisedend.EndConfig;
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
import net.minecraft.world.level.Level;
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
 * and here and there elsewhere. Round its foot lie a few fallen pieces, {@link #FALLEN_FROM}-{@link #FALLEN_TO} blocks
 * out on its own ground: plain crystal, each either a broken-off column lying along x or z, two or three long, or a
 * shard standing half-buried, two blocks over the ground and its root sunk into it (no budding block and no cluster, so the
 * formation's yield is the same). A piece that would meet anything else is left out on its own; the formation stands
 * or not by its columns alone. Fields are its configuration ({@code worldgen/feature/resonite_spire.json}).
 *
 * <p>Each column's footing must be {@code #promisedend:resonite_spire_ground} at the surface, within a block of the
 * first's, so a formation never stands in the air, hangs off a slope or stands on another's crystal. A formation that
 * would meet anything but air, its ground, a plant or another formation's Resonite, or any block of it under a roof, or
 * touch a chunk that any structure reaches (End Cities' pieces go in later, chunk by chunk), isn't made: it keeps out of
 * structures, the return gateway and a grove across a border. Sideways it stays within {@link #REACH} blocks of
 * the origin (2.5 off, then a lean of up to 0.45 a block over at most 0.75 of the codec's tallest 32, and a cluster),
 * inside the chunks a feature may write to.
 *
 * <p>{@code fallback} marks the sparser copy on {@code #promisedend:spire_fallback} (vanilla's highlands), which builds
 * only in the End with our terrain off: there the Spires never generate, and it is the only Resonite.
 */
public record ResoniteSpireFeature(BlockStateProvider crystal, BlockStateProvider budding, BlockStateProvider cluster,
                                   IntProvider height, IntProvider columns, float buddingChance, float clusterChance, boolean fallback) implements Feature {
    public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, PromisedEnd.id("resonite_spire_ground"));
    /** The chance of a cluster on each open side of a budding block at generation. */
    public static final float BUDDING_CLUSTER_CHANCE = 0.25F;
    /** The farthest a formation writes from its origin, sideways: the codec's tallest (32) leaning column and a cluster. */
    public static final int REACH = 14;
    /** How far from the origin the fallen pieces lie: past the columns' widest foot (2.5 off and 2.6 across). */
    public static final float FALLEN_FROM = 6.0F, FALLEN_TO = 8.0F;
    public static final MapCodec<ResoniteSpireFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockStateProvider.DIRECT_CODEC.fieldOf("crystal").forGetter(ResoniteSpireFeature::crystal),
            BlockStateProvider.DIRECT_CODEC.fieldOf("budding").forGetter(ResoniteSpireFeature::budding),
            BlockStateProvider.DIRECT_CODEC.fieldOf("cluster").forGetter(ResoniteSpireFeature::cluster),
            IntProviders.codec(4, 32).fieldOf("height").forGetter(ResoniteSpireFeature::height),
            IntProviders.codec(1, 8).fieldOf("columns").forGetter(ResoniteSpireFeature::columns),
            Codec.floatRange(0, 1).fieldOf("budding_chance").forGetter(ResoniteSpireFeature::buddingChance),
            Codec.floatRange(0, 1).fieldOf("cluster_chance").forGetter(ResoniteSpireFeature::clusterChance),
            Codec.BOOL.optionalFieldOf("fallback", false).forGetter(ResoniteSpireFeature::fallback)
    ).apply(i, ResoniteSpireFeature::new));

    @Override
    public MapCodec<ResoniteSpireFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        // the copy on vanilla's highlands is for packs with our terrain off, where the Spires never generate
        if (fallback && (EndConfig.terrainEnabled() || level.getLevel().dimension() != Level.END)) return false;
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
        // another formation's crystal, budding blocks and clusters are open too: neighbouring spires grow into one clump
        Set<Block> ours = Set.of(crystal.getState(level, random, foot).getBlock(), budding.getState(level, random, foot).getBlock(),
                cluster.getState(level, random, foot).getBlock());
        java.util.function.Predicate<BlockState> open = here -> here.isAir() || here.is(GROUND) || here.canBeReplaced() || ours.contains(here.getBlock());
        java.util.function.Predicate<Set<BlockPos>> clear = blocks -> {
            if (StructureClaims.claimed(level, blocks)) return false;
            for (BlockPos p : blocks) {
                if (!open.test(level.getBlockState(p))) return false;
                // and nothing over it but more of the same (a hillside's end stone is fine, an overhang of anything else isn't)
                int surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, p).getY();
                for (int up = p.getY() + 1; up < surface; up++) {
                    if (!open.test(level.getBlockState(p.atY(up)))) return false;
                }
            }
            return true;
        };
        if (!clear.test(body)) return false;
        // fallen pieces round the foot, each on ground within two blocks of the foot's height, each kept only if clear
        Set<BlockPos> fallen = new LinkedHashSet<>();
        int pieces = 2 + random.nextInt(3);
        for (int i = 0; i < pieces; i++) {
            float a = random.nextFloat() * Mth.TWO_PI, d = Mth.nextFloat(random, FALLEN_FROM, FALLEN_TO);
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, foot.offset(Math.round(Mth.cos(a) * d), 0, Math.round(Mth.sin(a) * d)));
            if (Math.abs(top.getY() - foot.getY()) > 2 || !level.getBlockState(top.below()).is(GROUND)) continue;
            Set<BlockPos> piece = new LinkedHashSet<>();
            if (random.nextInt(3) == 0) {
                // a shard standing half-buried: two blocks over the ground, its root sunk into the ground's top
                piece.add(top.below());
                piece.add(top);
                piece.add(top.above());
            } else {
                // a broken-off column lying along the axis it fell nearest to, on level ground all its length
                Direction along = Math.abs(Mth.cos(a)) >= Math.abs(Mth.sin(a))
                        ? (Mth.cos(a) > 0 ? Direction.EAST : Direction.WEST) : (Mth.sin(a) > 0 ? Direction.SOUTH : Direction.NORTH);
                int length = 2 + random.nextInt(2);
                for (int k = 0; k < length; k++) {
                    BlockPos at = top.relative(along, k);
                    if (level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, at).getY() != top.getY() || !level.getBlockState(at.below()).is(GROUND)) break;
                    piece.add(at);
                }
                if (piece.size() < 2) continue;
            }
            piece.removeAll(body);
            if (!piece.isEmpty() && clear.test(piece)) fallen.addAll(piece);
        }
        Set<BlockPos> all = new LinkedHashSet<>(body);
        all.addAll(fallen);
        for (BlockPos p : all) level.setBlock(p, crystal.getState(level, random, p), Block.UPDATE_CLIENTS);
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
