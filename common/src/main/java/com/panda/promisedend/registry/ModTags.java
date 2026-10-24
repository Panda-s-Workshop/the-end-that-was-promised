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

    /**
     * Ground the Void Anchor remembers as safe footing on top of every block with a sturdy top face: blocks
     * players build bridges and paths from whose tops aren't full faces.
     */
    public static final TagKey<Block> VOID_SAFE_GROUND = TagKey.create(Registries.BLOCK, PromisedEnd.id("void_safe_ground"));

    /** The Ancients' constructs: they never turn on each other or on the dragon they guard. */
    public static final TagKey<EntityType<?>> CONSTRUCTS = TagKey.create(Registries.ENTITY_TYPE, PromisedEnd.id("constructs"));

    private ModTags() {}
}
