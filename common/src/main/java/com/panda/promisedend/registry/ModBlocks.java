package com.panda.promisedend.registry;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.platform.registry.DeferredBlock;
import com.panda.promisedend.platform.registry.DeferredItem;
import com.panda.promisedend.platform.registry.DeferredRegister;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * The mod's blocks. Each one's name, model, loot, recipe and tags live under resources by the same id, which must
 * match the name here.
 */
public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(PromisedEnd.MODID);
    private static final List<DeferredItem<? extends BlockItem>> ITEMS = new ArrayList<>();

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
            p -> p.mapColor(MapColor.COLOR_PURPLE).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.STEM);
    /** The planks' properties, which the rest of the set shares as vanilla's crimson set does. */
    private static final UnaryOperator<BlockBehaviour.Properties> CHORUS_BOARDS =
            p -> p.mapColor(MapColor.COLOR_PURPLE).instrument(NoteBlockInstrument.BASS).strength(2.0F, 3.0F).sound(SoundType.NETHER_WOOD);
    /**
     * Chorus timber's wood type: crimson's sounds (nether wood), its own name for the sign screens' textures
     * ({@code minecraft:textures/gui/signs/promisedend_chorus.png}: the screens put the name in vanilla's namespace).
     */
    public static final WoodType CHORUS_WOOD_TYPE = WoodType.register(new WoodType("promisedend_chorus", BlockSetType.CRIMSON,
            WoodType.CRIMSON.soundType(), WoodType.CRIMSON.hangingSignSoundType(), WoodType.CRIMSON.fenceGateClose(), WoodType.CRIMSON.fenceGateOpen()));
    // in vanilla's creative-tab order for a wood set: each item is registered right after its block
    public static final DeferredBlock<RotatedPillarBlock> CHORUS_LOG = withItem(BLOCKS.registerBlock("chorus_log", RotatedPillarBlock::new, CHORUS_TIMBER));
    public static final DeferredBlock<RotatedPillarBlock> CHORUS_WOOD = withItem(BLOCKS.registerBlock("chorus_wood", RotatedPillarBlock::new, CHORUS_TIMBER));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_CHORUS_LOG = withItem(BLOCKS.registerBlock("stripped_chorus_log", RotatedPillarBlock::new, CHORUS_TIMBER));
    public static final DeferredBlock<RotatedPillarBlock> STRIPPED_CHORUS_WOOD = withItem(BLOCKS.registerBlock("stripped_chorus_wood", RotatedPillarBlock::new, CHORUS_TIMBER));
    public static final DeferredBlock<Block> CHORUS_PLANKS = simple("chorus_planks", CHORUS_BOARDS);
    public static final DeferredBlock<StairBlock> CHORUS_STAIRS = withItem(BLOCKS.registerBlock("chorus_stairs",
            p -> new StairBlock(CHORUS_PLANKS.get().defaultBlockState(), p), CHORUS_BOARDS));
    public static final DeferredBlock<SlabBlock> CHORUS_SLAB = withItem(BLOCKS.registerBlock("chorus_slab", SlabBlock::new, CHORUS_BOARDS));
    public static final DeferredBlock<FenceBlock> CHORUS_FENCE = withItem(BLOCKS.registerBlock("chorus_fence", FenceBlock::new, CHORUS_BOARDS));
    public static final DeferredBlock<FenceGateBlock> CHORUS_FENCE_GATE = withItem(BLOCKS.registerBlock("chorus_fence_gate",
            p -> new FenceGateBlock(CHORUS_WOOD_TYPE, p), p -> CHORUS_BOARDS.apply(p).forceSolidOn()));
    public static final DeferredBlock<DoorBlock> CHORUS_DOOR = withItem(BLOCKS.registerBlock("chorus_door", p -> new DoorBlock(BlockSetType.CRIMSON, p),
            p -> CHORUS_BOARDS.apply(p).strength(3.0F).noOcclusion().pushReaction(PushReaction.POPPED)), DoubleHighBlockItem::new, UnaryOperator.identity());
    public static final DeferredBlock<TrapDoorBlock> CHORUS_TRAPDOOR = withItem(BLOCKS.registerBlock("chorus_trapdoor",
            p -> new TrapDoorBlock(BlockSetType.CRIMSON, p), p -> CHORUS_BOARDS.apply(p).strength(3.0F).noOcclusion().isValidSpawn((s, l, pos, t) -> false)));
    public static final DeferredBlock<PressurePlateBlock> CHORUS_PRESSURE_PLATE = withItem(BLOCKS.registerBlock("chorus_pressure_plate",
            p -> new PressurePlateBlock(BlockSetType.CRIMSON, p),
            p -> CHORUS_BOARDS.apply(p).forceSolidOn().noCollision().strength(0.5F).pushReaction(PushReaction.POPPED)));
    public static final DeferredBlock<ButtonBlock> CHORUS_BUTTON = withItem(BLOCKS.registerBlock("chorus_button",
            p -> new ButtonBlock(BlockSetType.CRIMSON, 30, p), p -> p.noCollision().strength(0.5F).pushReaction(PushReaction.POPPED)));
    private static final UnaryOperator<BlockBehaviour.Properties> CHORUS_SIGN_PROPS =
            p -> p.mapColor(MapColor.COLOR_PURPLE).instrument(NoteBlockInstrument.BASS).forceSolidOn().noCollision().strength(1.0F);
    public static final DeferredBlock<StandingSignBlock> CHORUS_SIGN = withItem(BLOCKS.registerBlock("chorus_sign",
            p -> new StandingSignBlock(CHORUS_WOOD_TYPE, p), CHORUS_SIGN_PROPS),
            (sign, p) -> new StandingAndWallBlockItem(sign, ModBlocks.CHORUS_WALL_SIGN.get(), Direction.DOWN, p), p -> p.stacksTo(16).signText());
    /** Drops and is named as the standing sign, as vanilla's wall signs are. */
    public static final DeferredBlock<WallSignBlock> CHORUS_WALL_SIGN = BLOCKS.registerBlock("chorus_wall_sign",
            p -> new WallSignBlock(CHORUS_WOOD_TYPE, p), p -> wallVariant(CHORUS_SIGN_PROPS.apply(p), "chorus_sign"));
    public static final DeferredBlock<CeilingHangingSignBlock> CHORUS_HANGING_SIGN = withItem(BLOCKS.registerBlock("chorus_hanging_sign",
            p -> new CeilingHangingSignBlock(CHORUS_WOOD_TYPE, p), CHORUS_SIGN_PROPS),
            (sign, p) -> new HangingSignItem(sign, ModBlocks.CHORUS_WALL_HANGING_SIGN.get(), p), p -> p.stacksTo(16).signText());
    public static final DeferredBlock<WallHangingSignBlock> CHORUS_WALL_HANGING_SIGN = BLOCKS.registerBlock("chorus_wall_hanging_sign",
            p -> new WallHangingSignBlock(CHORUS_WOOD_TYPE, p), p -> wallVariant(CHORUS_SIGN_PROPS.apply(p), "chorus_hanging_sign"));
    /** Vanilla's shelves catch from lava, crimson's too; chorus timber's doesn't. */
    public static final DeferredBlock<ShelfBlock> CHORUS_SHELF = withItem(BLOCKS.registerBlock("chorus_shelf", ShelfBlock::new,
            p -> p.mapColor(MapColor.COLOR_PURPLE).instrument(NoteBlockInstrument.BASS).sound(SoundType.SHELF).strength(2.0F, 3.0F)),
            BlockItem::new, p -> p.component(net.minecraft.core.component.DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.EMPTY));
    /** End stone grown over, as nylium is netherrack: it drops end stone unless taken with Silk Touch. */
    public static final DeferredBlock<Block> CHORUS_MOSS = simple("chorus_moss",
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(3.0F, 9.0F).sound(SoundType.NYLIUM).requiresCorrectToolForDrops());

    // ---- the Lumen Mire: void-water pools in Lumen Moss, Mire Lilies on the water
    /** End stone grown over, as Chorus Moss is; it pays Lumen when broken without Silk Touch (its loot table). */
    public static final DeferredBlock<Block> LUMEN_MOSS = simple("lumen_moss",
            p -> p.mapColor(MapColor.COLOR_YELLOW).strength(3.0F, 9.0F).sound(SoundType.NYLIUM).requiresCorrectToolForDrops());
    /** A still pool, not a fluid: no item, nothing drops, a block placed into it replaces it; pistons don't move it. */
    public static final DeferredBlock<com.panda.promisedend.block.VoidWaterBlock> VOID_WATER = BLOCKS.registerBlock("void_water",
            com.panda.promisedend.block.VoidWaterBlock::new, p -> p.mapColor(MapColor.COLOR_BLACK).replaceable().noCollision().noOcclusion()
                    .strength(100.0F).pushReaction(PushReaction.IMMOVEABLE).noLootTable().sound(SoundType.EMPTY));
    public static final DeferredBlock<com.panda.promisedend.block.MireLilyBlock> MIRE_LILY = withItem(BLOCKS.registerBlock("mire_lily",
            com.panda.promisedend.block.MireLilyBlock::new, p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).noCollision().instabreak()
                    .sound(SoundType.LILY_PAD).lightLevel(s -> 7).pushReaction(PushReaction.POPPED)));

    // ---- the Resonant Spires: Resonite, amethyst's cousin from the outer End
    public static final DeferredBlock<Block> RESONITE_BLOCK = simple("resonite_block",
            p -> p.mapColor(MapColor.COLOR_CYAN).strength(1.5F).sound(SoundType.AMETHYST).requiresCorrectToolForDrops());
    /** Grows Resonite Clusters on its open faces; no item, and it drops nothing, as budding amethyst. */
    public static final DeferredBlock<com.panda.promisedend.block.BuddingResoniteBlock> BUDDING_RESONITE = BLOCKS.registerBlock("budding_resonite",
            com.panda.promisedend.block.BuddingResoniteBlock::new, p -> p.mapColor(MapColor.COLOR_CYAN).randomTicks().strength(1.5F)
                    .sound(SoundType.AMETHYST).requiresCorrectToolForDrops().pushReaction(PushReaction.POPPED).noLootTable());
    public static final DeferredBlock<com.panda.promisedend.block.ResoniteClusterBlock> RESONITE_CLUSTER = withItem(BLOCKS.registerBlock("resonite_cluster",
            com.panda.promisedend.block.ResoniteClusterBlock::new, p -> p.mapColor(MapColor.COLOR_CYAN).forceSolidOn().noOcclusion()
                    .sound(SoundType.AMETHYST_CLUSTER).strength(1.5F).lightLevel(s -> 5).pushReaction(PushReaction.POPPED)));

    private ModBlocks() {}

    /** Every block item, in registration order: the creative tab lists them. */
    public static List<DeferredItem<? extends BlockItem>> items() {
        return Collections.unmodifiableList(ITEMS);
    }

    /** A wall sign's properties: it drops, and is named, as the sign it is the wall form of. */
    private static BlockBehaviour.Properties wallVariant(BlockBehaviour.Properties p, String standing) {
        var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, PromisedEnd.id("blocks/" + standing));
        return p.overrideLootTable(java.util.Optional.of(key)).overrideDescription("block." + PromisedEnd.MODID + "." + standing);
    }

    private static <B extends Block> DeferredBlock<B> withItem(DeferredBlock<B> block) {
        ITEMS.add(ModItems.ITEMS.registerSimpleBlockItem(block));
        return block;
    }

    /** A block with an item of its own kind (a door's, a sign's), named as the block. */
    private static <B extends Block> DeferredBlock<B> withItem(DeferredBlock<B> block, java.util.function.BiFunction<B, net.minecraft.world.item.Item.Properties, ? extends BlockItem> item,
            UnaryOperator<net.minecraft.world.item.Item.Properties> props) {
        ITEMS.add(ModItems.ITEMS.registerItem(block.getId().getPath(), p -> item.apply(block.get(), p), p -> props.apply(p.useBlockDescriptionPrefix())));
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
