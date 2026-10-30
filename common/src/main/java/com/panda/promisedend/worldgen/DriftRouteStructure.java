package com.panda.promisedend.worldgen;

import com.mojang.serialization.MapCodec;
import com.panda.promisedend.entity.DriftIslet;
import com.panda.promisedend.logic.DriftGap;
import com.panda.promisedend.logic.DriftRoute;
import com.panda.promisedend.registry.ModEntities;
import com.panda.promisedend.registry.ModWorldgen;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.core.Direction;

/**
 * Drift Islets' routes: from the middle of a chunk, each way, the ground's edge and an
 * island across 12-96 blocks of void at the same height ({@link DriftGap}, on the generator's own
 * heights, so islands built by features, as the Skyfall's islets, aren't seen: their updrafts carry). A piece for each
 * route spans both its ends: it clears what features left in the corridor, sets a mooring (a purpur dock and two lit posts) at each
 * edge, and puts the islet down at the first, waiting.
 */
public final class DriftRouteStructure extends Structure {
    public static final MapCodec<DriftRouteStructure> CODEC = simpleCodec(DriftRouteStructure::new);
    /** No route this near the centre: the main island's own. */
    public static final int CENTRE_CLEAR = 200;
    /**
     * How far past its start chunk's corner each structure a route keeps clear of may reach: its template's width and a
     * block, turned any way about that corner; an End City's towers and ships, and anything else, {@link #REACH_ELSE}.
     */
    private static final java.util.Map<String, Integer> REACH = java.util.Map.of(
            "promisedend:rim_outpost", 22, "promisedend:shulker_foundry", 42, "promisedend:rift_maw", 64,
            "promisedend:void_shrine", 18, "promisedend:fallen_observatory", 24, "promisedend:unfinished_citadel", 81,
            "promisedend:shulker_foundry_fallback", 42, "promisedend:rift_maw_fallback", 64, "promisedend:unfinished_citadel_fallback", 81);
    private static final int REACH_ELSE = 96;
    /** This structure, which its own clearing passes and every other's keeps out of. */
    public static final net.minecraft.resources.ResourceKey<Structure> KEY = net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.STRUCTURE, com.panda.promisedend.PromisedEnd.id("drift_route"));

    public DriftRouteStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
        if ((long) x * x + (long) z * z < (long) CENTRE_CLEAR * CENTRE_CLEAR) return Optional.empty();
        ChunkGenerator generator = context.chunkGenerator();
        int floor = context.heightAccessor().getMinY() + 1;
        // none that might run into another structure, whose blocks its islet won't clear
        var gaps = DriftGap.all((gx, gz) -> generator.getBaseHeight(gx, gz, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState()),
                floor, x, z).stream().filter(gap -> !nearOther(context.registryAccess(), context.seed(), context.biomeResolver(), gap)).toList();
        if (gaps.isEmpty()) return Optional.empty();
        DriftGap.Gap first = gaps.getFirst();
        return Optional.of(new GenerationStub(new BlockPos(first.ex(), first.he(), first.ez()), builder -> gaps.forEach(gap -> builder.addPiece(new Piece(gap)))));
    }

    /**
     * Whether {@code gap}'s sweep comes within reach ({@link #REACH}) of a chunk where a structure it keeps clear of
     * ({@code #keep_clear_of_drift_routes}) may start. Each such set's spread placement gives each of its cells one
     * possible start, worked out from the seed alone, so every chunk that finds this gap comes to the same answer. A
     * start counts when one of the structure's biomes lies at its chunk's middle or a corner, or where its piece's middle
     * falls turned any way about the chunk's corner (a jigsaw checks its biome there): more than the structure's own
     * check lets through, so fewer routes, never one into another structure.
     */
    public static boolean nearOther(net.minecraft.core.RegistryAccess access, long seed, net.minecraft.world.level.biome.BiomeResolver biomes, DriftGap.Gap gap) {
        double[] s = gap.sweep();
        for (var set : access.lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET).listElements().toList()) {
            if (!(set.value().placement() instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement spread)) continue;
            var kept = set.value().structures().stream().map(e -> e.structure()).filter(h -> h.is(com.panda.promisedend.registry.ModTags.KEEP_CLEAR_OF_DRIFT_ROUTES)).toList();
            if (kept.isEmpty()) continue;
            int reach = kept.stream().mapToInt(h -> REACH.getOrDefault(h.getRegisteredName(), REACH_ELSE)).max().getAsInt();
            int minX = (int) Math.floor(s[0]) - reach, maxX = (int) Math.ceil(s[1]) + reach;
            int minZ = (int) Math.floor(s[2]) - reach, maxZ = (int) Math.ceil(s[3]) + reach;
            int spacing = spread.spacing();
            for (int gx = Math.floorDiv(minX >> 4, spacing); gx <= Math.floorDiv(maxX >> 4, spacing); gx++) {
                for (int gz = Math.floorDiv(minZ >> 4, spacing); gz <= Math.floorDiv(maxZ >> 4, spacing); gz++) {
                    ChunkPos start = spread.getPotentialStructureChunk(seed, gx * spacing, gz * spacing);
                    if (start.getMaxBlockX() < minX || start.getMinBlockX() > maxX || start.getMaxBlockZ() < minZ || start.getMinBlockZ() > maxZ) continue;
                    int half = reach / 2;
                    for (int[] at : new int[][] {{8, 8}, {0, 0}, {15, 0}, {0, 15}, {15, 15}, {half, half}, {-half, half}, {half, -half}, {-half, -half}}) {
                        var biome = biomes.getNoiseBiome((start.getMinBlockX() + at[0]) >> 2, 64 >> 2, (start.getMinBlockZ() + at[1]) >> 2);
                        if (kept.stream().anyMatch(h -> h.value().biomes().contains(biome))) return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public StructureType<?> type() {
        return ModWorldgen.DRIFT_ROUTE.get();
    }

    /** The whole route: its corridor, its two moorings and its islet. */
    public static final class Piece extends StructurePiece {
        private final DriftGap.Gap gap;

        Piece(DriftGap.Gap gap) {
            super(ModWorldgen.DRIFT_ROUTE_PIECE.get(), 0, box(gap));
            this.gap = gap;
        }

        public Piece(StructurePieceSerializationContext context, CompoundTag tag) {
            super(ModWorldgen.DRIFT_ROUTE_PIECE.get(), tag);
            this.gap = new DriftGap.Gap(tag.getIntOr("ex", 0), tag.getIntOr("ez", 0), tag.getIntOr("fx", 0), tag.getIntOr("fz", 0),
                    tag.getIntOr("dx", 0), tag.getIntOr("dz", 0), tag.getIntOr("he", 0), tag.getIntOr("hf", 0));
        }

        /**
         * The moorings: a dock of Ancient purpur at each edge, flush with that edge's ground, 5 wide (the islet's 3 and a
         * post's block each side; an edge that crumbled still has ground to step onto), and on its two ends a lit post:
         * two purpur pillars and an end rod. Only where {@code may} allows (worldgen: the chunk being written).
         */
        public static void moor(net.minecraft.world.level.LevelAccessor level, DriftGap.Gap gap, java.util.function.Predicate<BlockPos> may) {
            int lx = gap.dz() != 0 ? 1 : 0, lz = gap.dx() != 0 ? 1 : 0;
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            var dock = com.panda.promisedend.registry.ModBlocks.ANCIENT_PURPUR_SET.base().get().defaultBlockState();
            var pillar = Blocks.PURPUR_PILLAR.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
            var rod = Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING, Direction.UP);
            for (int[] end : new int[][] {{gap.ex(), gap.ez(), gap.he()}, {gap.fx(), gap.fz(), gap.hf()}}) {
                for (int l = -DriftGap.CORRIDOR; l <= DriftGap.CORRIDOR; l++) {
                    at.set(end[0] + lx * l, end[2] - 1, end[1] + lz * l);
                    if (!may.test(at)) continue;
                    level.setBlock(at, dock, 2);
                    if (Math.abs(l) != DriftGap.CORRIDOR) continue;
                    level.setBlock(at.setY(end[2]), pillar, 2);
                    level.setBlock(at.setY(end[2] + 1), pillar, 2);
                    level.setBlock(at.setY(end[2] + 2), rod, 2);
                }
            }
        }

        /** The route this piece makes (the server check). */
        public DriftGap.Gap gap() {
            return gap;
        }

        private static BoundingBox box(DriftGap.Gap g) {
            int c = DriftGap.CORRIDOR;
            return new BoundingBox(Math.min(g.ex(), g.fx()) - c, g.top() - 2, Math.min(g.ez(), g.fz()) - c,
                    Math.max(g.ex(), g.fx()) + c, g.high() + 3, Math.max(g.ez(), g.fz()) + c);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putInt("ex", gap.ex());
            tag.putInt("ez", gap.ez());
            tag.putInt("fx", gap.fx());
            tag.putInt("fz", gap.fz());
            tag.putInt("dx", gap.dx());
            tag.putInt("dz", gap.dz());
            tag.putInt("he", gap.he());
            tag.putInt("hf", gap.hf());
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox chunkBox,
                                ChunkPos chunk, BlockPos pivot) {
            int dx = gap.dx(), dz = gap.dz(), lx = dz != 0 ? 1 : 0, lz = dx != 0 ? 1 : 0;
            int far = Math.abs(gap.fx() - gap.ex()) + Math.abs(gap.fz() - gap.ez());
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            // the corridor: what features hung or set in it, from the islet's bottom to a rider's head, column by column
            // at the islet's height there; never inside another structure (an End City's ship or bridge)
            for (int j = 1; j < far; j++) {
                for (int l = -DriftGap.CORRIDOR; l <= DriftGap.CORRIDOR; l++) {
                    at.set(gap.ex() + dx * j + lx * l, gap.lowAt(j), gap.ez() + dz * j + lz * l);
                    if (!chunkBox.isInside(at) || structures.getStructureWithPieceAt(at, h -> !h.is(KEY)).isValid()) continue;
                    for (int y = gap.lowAt(j) - 1; y <= gap.highAt(j) + 2; y++) {
                        at.setY(y);
                        if (!level.getBlockState(at).isAir()) level.setBlock(at, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
            moor(level, gap, chunkBox::isInside);
            // the islet, once: in the chunk that holds its first end
            DriftRoute route = gap.route();
            at.set(Math.floor(route.ax()), Math.floor(route.ay()), Math.floor(route.az()));
            if (chunkBox.isInside(at)) {
                DriftIslet islet = ModEntities.DRIFT_ISLET.get().create(level.getLevel(), EntitySpawnReason.STRUCTURE);
                if (islet != null) {
                    islet.setRoute(route, DriftRoute.State.start());
                    level.addFreshEntity(islet);
                }
            }
        }
    }
}
