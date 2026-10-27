package com.panda.promisedend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.promisedend.block.HangingVineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * Vines hung from undersides: {@code tries} columns within {@code spread} blocks of the origin, each looked down
 * from {@code height} above the origin to {@code height} below it, and under every block of {@code anchor} there with
 * air beneath it, one chance in {@code 1 / chance} of a {@code vine} {@code length} blocks long (shorter where it
 * meets something). With {@code open}, only from an underside with air all round the block under it, so a vine hangs in
 * the open under a crown, not in a gap inside it; and {@code reach} of the vines instead hang down to the ground, their
 * tip a block or two above it, where a player can take hold and climb. Nothing in or beside a chunk a structure has
 * claimed. The Chorus Canopy hangs its vines from its leaves, the Hanging Gardens theirs from the islets. Fields are
 * its configuration ({@code worldgen/feature/*_vines.json}).
 */
public record HangingVineFeature(Block vine, TagKey<Block> anchor, int tries, int spread, int height, IntProvider length,
                                 float chance, boolean open, float reach) implements Feature {
    public static final MapCodec<HangingVineFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("vine").forGetter(HangingVineFeature::vine),
            TagKey.codec(Registries.BLOCK).fieldOf("anchor").forGetter(HangingVineFeature::anchor),
            Codec.intRange(1, 256).fieldOf("tries").forGetter(HangingVineFeature::tries),
            Codec.intRange(0, 15).fieldOf("spread").forGetter(HangingVineFeature::spread),
            Codec.intRange(1, 64).fieldOf("height").forGetter(HangingVineFeature::height),
            IntProviders.codec(1, HangingVineBlock.MAX_LENGTH).fieldOf("length").forGetter(HangingVineFeature::length),
            Codec.floatRange(0, 1).fieldOf("chance").forGetter(HangingVineFeature::chance),
            Codec.BOOL.optionalFieldOf("open", false).forGetter(HangingVineFeature::open),
            Codec.floatRange(0, 1).optionalFieldOf("reach", 0f).forGetter(HangingVineFeature::reach)
    ).apply(i, HangingVineFeature::new));

    @Override
    public MapCodec<HangingVineFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (!(vine instanceof HangingVineBlock hanging)) return false;
        if (StructureClaims.claimed(level, java.util.List.of(origin.offset(-spread, 0, -spread), origin.offset(spread, 0, spread),
                origin.offset(-spread, 0, spread), origin.offset(spread, 0, -spread)))) return false;
        int hung = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int t = 0; t < tries; t++) {
            int x = origin.getX() + random.nextInt(2 * spread + 1) - spread, z = origin.getZ() + random.nextInt(2 * spread + 1) - spread;
            int top = Math.min(origin.getY() + height, level.getMaxY()), bottom = Math.max(origin.getY() - height, level.getMinY() + 1);
            for (int y = top; y > bottom; y--) {
                p.set(x, y, z);
                if (!level.getBlockState(p).is(anchor) || !level.getBlockState(p.below()).isAir()) continue;
                if (open && !openBelow(level, p.below())) continue;
                if (random.nextFloat() >= chance) continue;
                int n = length.sample(random);
                if (reach > 0 && random.nextFloat() < reach) {
                    int gap = 1;
                    while (gap <= HangingVineBlock.MAX_LENGTH + 1 && level.getBlockState(p.below(gap)).isAir()) gap++;
                    // the ground gap blocks down: hang to one or two above it, if a vine can be that long
                    if (gap <= HangingVineBlock.MAX_LENGTH + 1) n = Math.max(1, gap - 1 - random.nextInt(2));
                }
                for (int k = 1; k <= n && level.getBlockState(p.below(k)).isAir(); k++) {
                    level.setBlock(p.below(k), hanging.grown(random), Block.UPDATE_CLIENTS);
                    if (k == 1) hung++;
                }
            }
        }
        return hung > 0;
    }

    private static boolean openBelow(WorldGenLevel level, BlockPos under) {
        for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) if (!level.getBlockState(under.relative(d)).isAir()) return false;
        return true;
    }
}
