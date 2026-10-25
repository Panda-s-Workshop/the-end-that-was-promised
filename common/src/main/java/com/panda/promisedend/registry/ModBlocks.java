package com.panda.promisedend.registry;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.platform.registry.DeferredBlock;
import com.panda.promisedend.platform.registry.DeferredItem;
import com.panda.promisedend.platform.registry.DeferredRegister;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/**
 * The mod's blocks. Each one's name, model, loot, recipe and tags live under resources by the same id, which must
 * match the name here.
 */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PromisedEnd.MODID);
    private static final List<DeferredItem<BlockItem>> ITEMS = new ArrayList<>();

    /** A stone with its stairs, slab and wall, registered and named together ({@code <base>_stairs} and so on). */
    public record StoneSet(DeferredBlock<Block> base, DeferredBlock<StairBlock> stairs, DeferredBlock<SlabBlock> slab,
                           DeferredBlock<WallBlock> wall) {}

    // ---- the Shattered Rim: Voidstone, end stone darkened by the void
    private static final UnaryOperator<BlockBehaviour.Properties> VOIDSTONE =
            p -> p.mapColor(MapColor.TERRACOTTA_PURPLE).strength(3.0F, 9.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();
    private static final UnaryOperator<BlockBehaviour.Properties> POLISHED_VOIDSTONE =
            p -> p.mapColor(MapColor.TERRACOTTA_PURPLE).strength(3.0F, 9.0F).sound(SoundType.POLISHED_DEEPSLATE).requiresCorrectToolForDrops();
    private static final UnaryOperator<BlockBehaviour.Properties> VOIDSTONE_BRICKS =
            p -> p.mapColor(MapColor.TERRACOTTA_PURPLE).strength(3.0F, 9.0F).sound(SoundType.DEEPSLATE_BRICKS).requiresCorrectToolForDrops();

    public static final StoneSet VOIDSTONE_SET = stoneSet("voidstone", VOIDSTONE);
    public static final StoneSet POLISHED_VOIDSTONE_SET = stoneSet("polished_voidstone", POLISHED_VOIDSTONE);
    public static final StoneSet VOIDSTONE_BRICK_SET = stoneSet("voidstone_brick", "voidstone_bricks", VOIDSTONE_BRICKS);
    public static final DeferredBlock<Block> CHISELED_VOIDSTONE = simple("chiseled_voidstone", VOIDSTONE_BRICKS);

    // ---- the Chorus Thicket: chorus timber, the End's only wood, and the moss under its groves. Like the Nether's
    // stems it doesn't burn
    private static final UnaryOperator<BlockBehaviour.Properties> CHORUS_TIMBER =
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(2.0F).sound(SoundType.STEM);
    public static final DeferredBlock<RotatedPillarBlock> CHORUS_LOG = withItem(BLOCKS.registerBlock("chorus_log", RotatedPillarBlock::new, CHORUS_TIMBER));
    public static final DeferredBlock<Block> CHORUS_PLANKS = simple("chorus_planks",
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(2.0F, 3.0F).sound(SoundType.NETHER_WOOD));
    /** End stone grown over, as nylium is netherrack: it drops end stone unless taken with Silk Touch. */
    public static final DeferredBlock<Block> CHORUS_MOSS = simple("chorus_moss",
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(3.0F, 9.0F).sound(SoundType.NYLIUM).requiresCorrectToolForDrops());

    private ModBlocks() {}

    /** Every block item, in registration order: the creative tab lists them. */
    public static List<DeferredItem<BlockItem>> items() {
        return Collections.unmodifiableList(ITEMS);
    }

    private static <B extends Block> DeferredBlock<B> withItem(DeferredBlock<B> block) {
        ITEMS.add(ModItems.ITEMS.registerSimpleBlockItem(block));
        return block;
    }

    private static DeferredBlock<Block> simple(String name, UnaryOperator<BlockBehaviour.Properties> properties) {
        DeferredBlock<Block> block = BLOCKS.registerSimpleBlock(name, properties);
        ITEMS.add(ModItems.ITEMS.registerSimpleBlockItem(block));
        return block;
    }

    private static StoneSet stoneSet(String name, UnaryOperator<BlockBehaviour.Properties> properties) {
        return stoneSet(name, name, properties);
    }

    /** {@code prefix} names the variants ("voidstone_brick_stairs"), {@code baseName} the full block ("voidstone_bricks"). */
    private static StoneSet stoneSet(String prefix, String baseName, UnaryOperator<BlockBehaviour.Properties> properties) {
        DeferredBlock<Block> base = simple(baseName, properties);
        DeferredBlock<StairBlock> stairs = BLOCKS.registerBlock(prefix + "_stairs", p -> new StairBlock(base.get().defaultBlockState(), p), properties);
        DeferredBlock<SlabBlock> slab = BLOCKS.registerBlock(prefix + "_slab", SlabBlock::new, properties);
        DeferredBlock<WallBlock> wall = BLOCKS.registerBlock(prefix + "_wall", WallBlock::new, p -> properties.apply(p).forceSolidOn());
        for (DeferredBlock<? extends Block> b : List.of(stairs, slab, wall)) ITEMS.add(ModItems.ITEMS.registerSimpleBlockItem(b));
        return new StoneSet(base, stairs, slab, wall);
    }
}
