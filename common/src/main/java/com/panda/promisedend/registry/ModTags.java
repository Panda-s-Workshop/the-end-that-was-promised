package com.panda.promisedend.registry;

import com.panda.promisedend.PromisedEnd;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Tags the mod defines (the files are under data/promisedend/tags). */
public final class ModTags {
    /** What repairs Voidstone tools in an anvil. */
    public static final TagKey<Item> VOIDSTONE_TOOL_MATERIALS = TagKey.create(Registries.ITEM, PromisedEnd.id("voidstone_tool_materials"));
    public static final TagKey<Item> RESONITE_TOOL_MATERIALS = TagKey.create(Registries.ITEM, PromisedEnd.id("resonite_tool_materials"));
    /** The armour the Resonance bonus counts: four pieces, one a slot. */
    public static final TagKey<Item> RESONANCE_SET = TagKey.create(Registries.ITEM, PromisedEnd.id("resonance_set"));
    /** What repairs Resonite armour (the ingot), apart from the tools' material. */
    public static final TagKey<Item> REPAIRS_RESONITE_ARMOR = TagKey.create(Registries.ITEM, PromisedEnd.id("repairs_resonite_armor"));
    /** What stripping into stripped chorus timber sheds Chorus Resin from: the chorus log and wood. */
    public static final TagKey<Block> SHEDS_CHORUS_RESIN = TagKey.create(Registries.BLOCK, PromisedEnd.id("sheds_chorus_resin"));
    /** What a Dragon Scale repairs in an anvil, half its durability a scale (the elytra). */
    public static final TagKey<Item> REPAIRED_BY_DRAGON_SCALE = TagKey.create(Registries.ITEM, PromisedEnd.id("repaired_by_dragon_scale"));

    /**
     * Ground the Void Anchor remembers as safe footing on top of every block with a sturdy top face: blocks
     * players build bridges and paths from whose tops aren't full faces.
     */
    public static final TagKey<Block> VOID_SAFE_GROUND = TagKey.create(Registries.BLOCK, PromisedEnd.id("void_safe_ground"));
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
    /** The Ancients' constructs: they never turn on each other or on the dragon they guard. */
    public static final TagKey<EntityType<?>> CONSTRUCTS = TagKey.create(Registries.ENTITY_TYPE, PromisedEnd.id("constructs"));

    private ModTags() {}
}
