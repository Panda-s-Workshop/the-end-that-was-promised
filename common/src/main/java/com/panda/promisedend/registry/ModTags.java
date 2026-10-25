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
    /** Blocks an outer gateway and its exit keep off while other ground is in reach (the spires' Resonite): TheEndGatewayBlockEntityMixin. */
    public static final TagKey<Block> NOT_A_GATEWAY_EXIT = TagKey.create(Registries.BLOCK, PromisedEnd.id("not_a_gateway_exit"));
    /** What an outer gateway's frame keeps clear of: the spires' blocks and their clusters. */
    public static final TagKey<Block> GATEWAY_FRAME_KEEP_OUT = TagKey.create(Registries.BLOCK, PromisedEnd.id("gateway_frame_keep_out"));

    /** The Ancients' constructs: they never turn on each other or on the dragon they guard. */
    public static final TagKey<EntityType<?>> CONSTRUCTS = TagKey.create(Registries.ENTITY_TYPE, PromisedEnd.id("constructs"));

    private ModTags() {}
}
