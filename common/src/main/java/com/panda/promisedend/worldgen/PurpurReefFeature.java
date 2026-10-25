package com.panda.promisedend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.promisedend.EndConfig;
import com.panda.promisedend.PromisedEnd;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * A Shulker Reef: {@code growths} growths of {@code coral} (Ancient Purpur) tried round its spot, each one of three
 * forms as vanilla's corals are: a {@link Form#TREE} (a trunk {@code height} tall with 2-4 branches climbing out of
 * its upper half), a {@link Form#FAN} (a lacy fan in an upright plane on a short stem) or a {@link Form#DOME} (a cap
 * on a short stalk). A branch tip, a trunk's top, a fan's crown or a dome's top carries a {@code polyp} (an end rod),
 * as often as {@code polypChance} says. A share {@code colonyChance} of the reefs are colonies: {@code colony} of
 * {@code colonist} (wild shulkers) sit on their tops, with room over them to open, once all its growths stand. They
 * are the Ancients' shulkers gone wild, and vanilla's duplication (a shulker hit by a shulker's bullet may split)
 * makes their shells renewable.
 *
 * <p>Each growth stands on {@code #promisedend:purpur_reef_ground} (end stone) within {@link #SPREAD} of the spot,
 * and is left out if any of its blocks would go where there is anything but air or a plant (not a fluid), or into a
 * chunk a structure has claimed, so a reef never cuts into a structure, a pool or its neighbour. It stays within
 * {@link #REACH} blocks of its spot sideways. {@code fallback} marks the sparser copy on vanilla's midlands ({@code
 * #promisedend:reef_fallback}), which builds only in the End with our terrain off.
 */
public record PurpurReefFeature(BlockStateProvider coral, BlockStateProvider polyp, IntProvider growths, IntProvider height,
                                float polypChance, float colonyChance, IntProvider colony, EntityType<?> colonist, boolean fallback) implements Feature {
    public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, PromisedEnd.id("purpur_reef_ground"));
    /** How far from the spot a growth's foot may be. */
    public static final int SPREAD = 6;
    /** The farthest it writes sideways: the spread, a branch's or a fan's or a dome's 3, and one. */
    public static final int REACH = 10;
    public static final MapCodec<PurpurReefFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockStateProvider.DIRECT_CODEC.fieldOf("coral").forGetter(PurpurReefFeature::coral),
            BlockStateProvider.DIRECT_CODEC.fieldOf("polyp").forGetter(PurpurReefFeature::polyp),
            IntProviders.codec(1, 10).fieldOf("growths").forGetter(PurpurReefFeature::growths),
            IntProviders.codec(3, 9).fieldOf("height").forGetter(PurpurReefFeature::height),
            Codec.floatRange(0, 1).fieldOf("polyp_chance").forGetter(PurpurReefFeature::polypChance),
            Codec.floatRange(0, 1).fieldOf("colony_chance").forGetter(PurpurReefFeature::colonyChance),
            IntProviders.codec(1, 4).fieldOf("colony").forGetter(PurpurReefFeature::colony),
            EntityType.CODEC.optionalFieldOf("colonist", EntityTypes.SHULKER).forGetter(PurpurReefFeature::colonist),
            Codec.BOOL.optionalFieldOf("fallback", false).forGetter(PurpurReefFeature::fallback)
    ).apply(i, PurpurReefFeature::new));

    /** A growth's form, as vanilla's three corals. */
    public enum Form { TREE, FAN, DOME }

    @Override
    public MapCodec<PurpurReefFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        // the copy on vanilla's midlands is for packs with our terrain off, where the Reefs never generate
        if (fallback && (EndConfig.terrainEnabled() || level.getLevel().dimension() != Level.END)) return false;
        int n = growths.sample(random), made = 0;
        List<BlockPos> coralMade = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            BlockPos at = origin.offset(random.nextInt(SPREAD * 2 + 1) - SPREAD, 0, random.nextInt(SPREAD * 2 + 1) - SPREAD);
            BlockPos foot = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, at);
            if (foot.getY() <= level.getMinY() + 1 || !level.getBlockState(foot.below()).is(GROUND)) continue;
            Map<BlockPos, BlockState> plan = new LinkedHashMap<>();
            java.util.Set<BlockPos> polyps = new java.util.HashSet<>();
            Form form = Form.values()[random.nextInt(Form.values().length)];
            grow(level, random, foot, form, height.sample(random), plan, polyps);
            // keep out: a growth that would go into anything but air or a plant, or into a claimed chunk, isn't made
            if (plan.keySet().stream().anyMatch(p -> !open(level, p)) || StructureClaims.claimed(level, plan.keySet())) continue;
            plan.forEach((p, state) -> level.setBlock(p, state, Block.UPDATE_CLIENTS));
            plan.keySet().stream().filter(p -> !polyps.contains(p)).forEach(coralMade::add);
            made++;
        }
        if (made == 0) return false;
        // the colony perches once every growth stands, so none grows over a shell: over a coral block (not a polyp),
        // with two blocks of air, room for a shulker to open
        List<BlockPos> tops = new ArrayList<>();
        for (BlockPos p : coralMade) {
            if (level.getBlockState(p.above()).isAir() && level.getBlockState(p.above(2)).isAir()) tops.add(p.above());
        }
        if (!tops.isEmpty() && random.nextFloat() < colonyChance) {
            int count = Math.min(colony.sample(random), tops.size());
            for (int i = 0; i < count; i++) {
                BlockPos perch = tops.remove(random.nextInt(tops.size()));
                // and no two in one column, one over the other
                tops.removeIf(q -> q.getX() == perch.getX() && q.getZ() == perch.getZ());
                var colonist = this.colonist.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
                if (colonist == null) continue;
                colonist.setPos(perch.getX() + 0.5, perch.getY(), perch.getZ() + 0.5);
                level.addFreshEntity(colonist);
                if (tops.isEmpty()) break;
            }
        }
        return true;
    }

    /** Air, or a plant: not a fluid, and not Void-water, which a block placed into it would replace. */
    private static boolean open(WorldGenLevel level, BlockPos p) {
        BlockState here = level.getBlockState(p);
        return here.isAir() || (here.canBeReplaced() && here.getFluidState().isEmpty() && !here.is(com.panda.promisedend.registry.ModBlocks.VOID_WATER.get()));
    }

    /** One growth from {@code foot} (the air over its ground), its blocks into {@code plan}. */
    private void grow(WorldGenLevel level, RandomSource random, BlockPos foot, Form form, int h, Map<BlockPos, BlockState> plan, java.util.Set<BlockPos> polyps) {
        switch (form) {
            case TREE -> {
                for (int y = 0; y < h; y++) put(level, random, foot.above(y), plan);
                crown(level, random, foot.above(h), plan, polyps);
                int branches = 2 + random.nextInt(3);
                for (int b = 0; b < branches; b++) {
                    Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                    BlockPos p = foot.above(h / 2 + random.nextInt(Math.max(1, h - h / 2 - 1)));
                    int length = 2 + random.nextInt(2);
                    for (int s = 0; s < length; s++) {
                        p = p.relative(d);
                        put(level, random, p, plan);
                        // climbing: up a block after each step but the first
                        if (s > 0) {
                            p = p.above();
                            put(level, random, p, plan);
                        }
                    }
                    crown(level, random, p.above(), plan, polyps);
                }
            }
            case FAN -> {
                Direction.Axis axis = random.nextBoolean() ? Direction.Axis.X : Direction.Axis.Z;
                int stem = 1 + random.nextInt(2);
                for (int y = 0; y < stem; y++) put(level, random, foot.above(y), plan);
                int rows = Math.max(2, h - stem);
                for (int r = 0; r < rows; r++) {
                    // it widens a block a side every two rows, so a rim stands on the rim under it
                    int half = Math.min(3, 1 + r / 2);
                    boolean widened = r > 0 && half > Math.min(3, 1 + (r - 1) / 2);
                    for (int o = -half; o <= half; o++) {
                        // lacy: the middle and the rim always, and the block inside a rim that just widened (else
                        // the rim would meet the fan at a corner); the rest one in two
                        int a = Math.abs(o);
                        if (o != 0 && a != half && !(widened && a == half - 1) && random.nextBoolean()) continue;
                        BlockPos p = foot.above(stem + r).relative(axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH, o);
                        put(level, random, p, plan);
                    }
                }
                crown(level, random, foot.above(stem + rows), plan, polyps);
            }
            case DOME -> {
                int stalk = 1 + random.nextInt(2), r = h > 5 ? 3 : 2;
                for (int y = 0; y < stalk; y++) put(level, random, foot.above(y), plan);
                BlockPos c = foot.above(stalk);
                for (int dx = -r; dx <= r; dx++) {
                    for (int dy = 0; dy <= r; dy++) {
                        for (int dz = -r; dz <= r; dz++) {
                            if (dx * dx + dy * dy + dz * dz <= r * r + 1) put(level, random, c.offset(dx, dy, dz), plan);
                        }
                    }
                }
                crown(level, random, c.above(r + 1), plan, polyps);
            }
        }
    }

    private void put(WorldGenLevel level, RandomSource random, BlockPos p, Map<BlockPos, BlockState> plan) {
        plan.put(p.immutable(), coral.getState(level, random, p));
    }

    /** A polyp on a tip, {@code polypChance} of the time. */
    private void crown(WorldGenLevel level, RandomSource random, BlockPos p, Map<BlockPos, BlockState> plan, java.util.Set<BlockPos> polyps) {
        if (random.nextFloat() < polypChance && !plan.containsKey(p)) {
            plan.put(p.immutable(), polyp.getState(level, random, p));
            polyps.add(p.immutable());
        }
    }

}
