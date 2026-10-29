package com.panda.promisedend.registry;

import com.panda.promisedend.PromisedEnd;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Tags the mod defines (the files are under data/promisedend/tags). */
public final class ModTags {
    /** What the Waystation's first Star Chart and a held pearl lead to: the Shulker Foundries. */
    public static final TagKey<net.minecraft.world.level.levelgen.structure.Structure> ON_FOUNDRY_CHARTS =
            TagKey.create(Registries.STRUCTURE, PromisedEnd.id("on_foundry_charts"));
    /** What the Sentinel's chart and, after its seal, a held pearl lead to: the Rift Maws. */
    public static final TagKey<net.minecraft.world.level.levelgen.structure.Structure> ON_RIFT_MAW_CHARTS =
            TagKey.create(Registries.STRUCTURE, PromisedEnd.id("on_rift_maw_charts"));
    /** What a Void Shrine's altar takes as an offering; each is also in one {@link #favour} tag, its worth. */
    public static final TagKey<Item> OFFERINGS = TagKey.create(Registries.ITEM, PromisedEnd.id("offerings"));
    private static final java.util.List<TagKey<Item>> FAVOUR = java.util.stream.IntStream.rangeClosed(1, com.panda.promisedend.logic.Offerings.MOST)
            .mapToObj(n -> TagKey.create(Registries.ITEM, PromisedEnd.id("offerings/favour_" + n))).toList();
    /** What repairs Voidstone tools in an anvil. */
    public static final TagKey<Item> VOIDSTONE_TOOL_MATERIALS = TagKey.create(Registries.ITEM, PromisedEnd.id("voidstone_tool_materials"));
    public static final TagKey<Item> RESONITE_TOOL_MATERIALS = TagKey.create(Registries.ITEM, PromisedEnd.id("resonite_tool_materials"));
    /** The armour the Resonance bonus counts: four pieces, one a slot. */
    public static final TagKey<Item> RESONANCE_SET = TagKey.create(Registries.ITEM, PromisedEnd.id("resonance_set"));
    /** What repairs Resonite armour (the ingot), apart from the tools' material. */
    public static final TagKey<Item> REPAIRS_RESONITE_ARMOR = TagKey.create(Registries.ITEM, PromisedEnd.id("repairs_resonite_armor"));
    /** What repairs Voidsteel tools (the ingot). */
    public static final TagKey<Item> VOIDSTEEL_TOOL_MATERIALS = TagKey.create(Registries.ITEM, PromisedEnd.id("voidsteel_tool_materials"));
    /** The gliders an augment can be smithed onto (the elytra). */
    public static final TagKey<Item> AUGMENTABLE_ELYTRA = TagKey.create(Registries.ITEM, PromisedEnd.id("augmentable_elytra"));
    /** The armour the Void-touched bonus counts: four pieces, one a slot. */
    public static final TagKey<Item> VOID_TOUCHED_SET = TagKey.create(Registries.ITEM, PromisedEnd.id("void_touched_set"));
    /** What repairs Voidsteel armour (the ingot), apart from the tools' material. */
    public static final TagKey<Item> REPAIRS_VOIDSTEEL_ARMOR = TagKey.create(Registries.ITEM, PromisedEnd.id("repairs_voidsteel_armor"));
    /** What stripping into stripped chorus timber sheds Chorus Resin from: the chorus log and wood. */
    public static final TagKey<Block> SHEDS_CHORUS_RESIN = TagKey.create(Registries.BLOCK, PromisedEnd.id("sheds_chorus_resin"));
    /** What a Dragon Scale repairs in an anvil, half its durability a scale (the elytra). */
    public static final TagKey<Item> REPAIRED_BY_DRAGON_SCALE = TagKey.create(Registries.ITEM, PromisedEnd.id("repaired_by_dragon_scale"));

    /**
     * Ground the Void Anchor remembers as safe footing on top of every block with a sturdy top face: blocks
     * players build bridges and paths from whose tops aren't full faces.
     */
    public static final TagKey<Block> VOID_SAFE_GROUND = TagKey.create(Registries.BLOCK, PromisedEnd.id("void_safe_ground"));
    /** What a fallen star's crater takes, and what may stand within 8 of where it lands (anything else is a player's). */
    public static final TagKey<Block> STARFALL_REPLACEABLE = TagKey.create(Registries.BLOCK, PromisedEnd.id("starfall_replaceable"));
    public static final TagKey<Block> STARFALL_NATURAL = TagKey.create(Registries.BLOCK, PromisedEnd.id("starfall_natural"));
    /** The lattice that tears and re-forms (block.PhantomLatticeBlock): only ours, so nothing else is ever torn. */
    public static final TagKey<Block> LATTICE_TEARABLE = TagKey.create(Registries.BLOCK, PromisedEnd.id("lattice_tearable"));
    /** What a Lattice Crawler climbs. */
    public static final TagKey<Block> LATTICE_CLIMBABLE = TagKey.create(Registries.BLOCK, PromisedEnd.id("lattice_climbable"));
    /** What a Drift Blossom grows on: end stone. */
    public static final TagKey<Block> DRIFT_BLOSSOM_GROUND = TagKey.create(Registries.BLOCK, PromisedEnd.id("drift_blossom_ground"));
    /** What Void Grass grows on: end stone and the End's mosses (VoidGrassBlock). */
    public static final TagKey<Block> VOID_GRASS_GROUND = TagKey.create(Registries.BLOCK, PromisedEnd.id("void_grass_ground"));
    /** What a Void Mushroom grows on: Void Mycelium. */
    public static final TagKey<Block> VOID_MUSHROOM_GROUND = TagKey.create(Registries.BLOCK, PromisedEnd.id("void_mushroom_ground"));
    /** What a Chorus Vine hangs from: the chorus leaves and timber. */
    public static final TagKey<Block> CHORUS_VINE_ANCHOR = TagKey.create(Registries.BLOCK, PromisedEnd.id("chorus_vine_anchor"));
    /** What a Void Berry Vine hangs from: end stone and the End's mosses, the islets' undersides. */
    public static final TagKey<Block> VOID_BERRY_VINE_ANCHOR = TagKey.create(Registries.BLOCK, PromisedEnd.id("void_berry_vine_anchor"));
    /** The ground an ancient Updraft Vent is sunk into on an island's edge: UpdraftVentFeature. */
    public static final TagKey<Block> UPDRAFT_VENT_GROUND = TagKey.create(Registries.BLOCK, PromisedEnd.id("updraft_vent_ground"));
    /** Blocks an outer gateway and its exit keep off while other ground is in reach (the spires' Resonite): TheEndGatewayBlockEntityMixin. */
    public static final TagKey<Block> NOT_A_GATEWAY_EXIT = TagKey.create(Registries.BLOCK, PromisedEnd.id("not_a_gateway_exit"));
    /** What an outer gateway's frame keeps clear of: the spires' blocks and their clusters. */
    public static final TagKey<Block> GATEWAY_FRAME_KEEP_OUT = TagKey.create(Registries.BLOCK, PromisedEnd.id("gateway_frame_keep_out"));

    /** Entities an Updraft Vent's column leaves alone (the dragon, the wither, shulkers): UpdraftVentBlockEntity. */
    public static final TagKey<EntityType<?>> UPDRAFT_IGNORES = TagKey.create(Registries.ENTITY_TYPE, PromisedEnd.id("updraft_ignores"));
    /** And the void currents (the same ones): logic/VoidCurrents. */
    public static final TagKey<EntityType<?>> VOID_CURRENT_IGNORES = TagKey.create(Registries.ENTITY_TYPE, PromisedEnd.id("void_current_ignores"));
    /** The biomes a void current runs under, and those it runs under only while our terrain is off. */
    public static final TagKey<net.minecraft.world.level.biome.Biome> VOID_CURRENTS = TagKey.create(Registries.BIOME, PromisedEnd.id("void_currents"));
    public static final TagKey<net.minecraft.world.level.biome.Biome> VOID_CURRENTS_FALLBACK = TagKey.create(Registries.BIOME, PromisedEnd.id("void_currents_fallback"));
    /**
     * Vanilla's biomes a biome's features (and constructs) go to while our terrain is off: the Thicket's on the
     * highlands, the Mire's on the midlands, the Voidscar's on the barrens.
     */
    public static final TagKey<net.minecraft.world.level.biome.Biome> GROVE_FALLBACK = TagKey.create(Registries.BIOME, PromisedEnd.id("grove_fallback"));
    public static final TagKey<net.minecraft.world.level.biome.Biome> MIRE_FALLBACK = TagKey.create(Registries.BIOME, PromisedEnd.id("mire_fallback"));
    public static final TagKey<net.minecraft.world.level.biome.Biome> VOIDSCAR_FALLBACK = TagKey.create(Registries.BIOME, PromisedEnd.id("voidscar_fallback"));
    /** The Ancients' constructs: they never turn on each other or on the dragon they guard. */
    public static final TagKey<EntityType<?>> CONSTRUCTS = TagKey.create(Registries.ENTITY_TYPE, PromisedEnd.id("constructs"));

    private ModTags() {}

    /** The offerings worth {@code n} favour ({@code #promisedend:offerings/favour_<n>}, 1 to 5). */
    public static TagKey<Item> favour(int n) {
        return FAVOUR.get(n - 1);
    }
}
