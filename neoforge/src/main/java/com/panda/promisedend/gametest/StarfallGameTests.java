package com.panda.promisedend.gametest;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.logic.Starfall;
import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** The Starfall. */
final class StarfallGameTests {
    private StarfallGameTests() {}

    /**
     * A star lands only where nothing was built: a bare island of end stone is safe, and not with planks or a block
     * entity (an Updraft Vent, a natural block) 2 off. Its crater, radius 4, takes the end stone and the grass in its
     * bowl and leaves obsidian (outside {@code #promisedend:starfall_replaceable}) and the ground past its rim (its top
     * maybe burnt to Voidstone); its
     * walls hold 3-6 Starmetal Ore, each facing into it, and one Starfall Core lies in it, glowing, never to despawn.
     */
    static void craterRespectsTag(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        for (int x = 1; x <= 11; x++) {
            for (int z = 1; z <= 11; z++) {
                for (int y = 1; y <= 7; y++) helper.setBlock(new BlockPos(x, y, z), y <= 4 ? Blocks.END_STONE : Blocks.AIR);
            }
        }
        // its safety looked at 3 round, so all it sees is inside the arena, which the test filled
        BlockPos middle = new BlockPos(6, 4, 6);
        BlockPos at = helper.absolutePos(middle);
        if (!Starfall.safe(level, at, 3)) bad.add("a bare island of end stone isn't safe");
        helper.setBlock(new BlockPos(6, 5, 8), Blocks.OAK_PLANKS);
        if (Starfall.safe(level, at, 3)) bad.add("safe with planks 2 off");
        helper.setBlock(new BlockPos(6, 5, 8), Blocks.AIR);
        helper.setBlock(new BlockPos(8, 5, 6), ModBlocks.UPDRAFT_VENT.get());
        if (Starfall.safe(level, at, 3)) bad.add("safe with a vent, a block entity, 2 off");
        helper.setBlock(new BlockPos(8, 5, 6), Blocks.AIR);
        helper.setBlock(new BlockPos(6, 2, 6), Blocks.OBSIDIAN);
        helper.setBlock(new BlockPos(4, 5, 6), ModBlocks.VOID_GRASS.get());
        Starfall.impact(level, at, 4);
        if (!helper.getBlockState(new BlockPos(6, 2, 6)).is(Blocks.OBSIDIAN)) bad.add("the crater took the obsidian");
        if (!helper.getBlockState(middle).isAir() || !helper.getBlockState(middle.below()).isAir()) bad.add("the bowl's middle wasn't hollowed");
        if (!helper.getBlockState(new BlockPos(4, 5, 6)).isAir()) bad.add("the grass in the bowl stood");
        // past the rim the ground stands, burnt to Voidstone or not
        var past = helper.getBlockState(new BlockPos(11, 4, 6));
        if (!past.is(Blocks.END_STONE) && !past.is(ModBlocks.VOIDSTONE_SET.base().get())) bad.add("the ground past the rim went: " + past);
        if (!helper.getBlockState(new BlockPos(11, 3, 6)).is(Blocks.END_STONE)) bad.add("the end stone under the rim went");
        int ore = 0;
        for (int x = 0; x <= 11; x++) {
            for (int z = 0; z <= 11; z++) {
                for (int y = 0; y <= 7; y++) {
                    BlockPos b = new BlockPos(x, y, z);
                    if (!helper.getBlockState(b).is(ModBlocks.STARMETAL_ORE.get())) continue;
                    ore++;
                    boolean faces = false;
                    for (var d : net.minecraft.core.Direction.values()) faces |= helper.getBlockState(b.relative(d)).isAir();
                    if (!faces) bad.add("ore at " + b + " faces nothing");
                }
            }
        }
        if (ore < 3 || ore > 6) bad.add(ore + " Starmetal Ore, not 3-6");
        var cores = level.getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).expandTowards(12, 8, 12),
                i -> i.getItem().is(ModItems.STARFALL_CORE.get()));
        if (cores.size() != 1) bad.add(cores.size() + " Starfall Cores, not 1");
        else if (cores.getFirst().getAge() != -32768 || !cores.getFirst().hasGlowingTag()) bad.add("the core would despawn or doesn't glow");
        PromisedEnd.LOGGER.info("[starfall_crater_respects_tag] {} Starmetal Ore, {} core(s)", ore, cores.size());
        if (!bad.isEmpty()) helper.fail("starfall: " + bad);
        helper.succeed();
    }
}
