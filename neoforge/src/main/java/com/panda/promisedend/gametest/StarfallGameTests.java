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
     * The ore gives Starmetal to an iron pickaxe; the core is a shrine's fourth-favour offering. A Star Compass then points
     * at this crater, the last (no star on its way), in the End; a compass, four Starmetal and the core make one.
     */
    /**
     * What stands where a star lands is thrown clear and comes down unhurt: a pig at the middle of a radius-4 fall,
     * thrown up over the bowl, lands with its health whole and out of the bowl's middle.
     */
    static void thrownUnhurt(GameTestHelper helper) {
        var level = helper.getLevel();
        for (int x = 1; x <= 11; x++) {
            for (int z = 1; z <= 11; z++) {
                for (int y = 1; y <= 4; y++) helper.setBlock(new BlockPos(x, y, z), Blocks.END_STONE);
            }
        }
        BlockPos at = helper.absolutePos(new BlockPos(6, 4, 6));
        var pig = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(6, 5, 6));
        float whole = pig.getHealth();
        double[] top = {pig.getY()};
        helper.runAtTickTime(5, () -> Starfall.impact(level, at, 4));
        helper.onEachTick(() -> top[0] = Math.max(top[0], pig.getY()));
        helper.runAtTickTime(120, () -> {
            List<String> bad = new ArrayList<>();
            PromisedEnd.LOGGER.info("[starfall_thrown_unhurt] rose to {} from {}, health {} of {}, on the ground {} at {}", top[0], at.getY() + 1,
                    pig.getHealth(), whole, pig.onGround(), pig.position());
            if (top[0] < at.getY() + 3) bad.add("it wasn't thrown up: it rose to " + top[0]);
            if (!pig.isAlive() || pig.getHealth() < whole) bad.add("it came down hurt: " + pig.getHealth() + " of " + whole);
            if (!pig.onGround()) bad.add("it hasn't landed");
            pig.discard();
            if (!bad.isEmpty()) helper.fail("thrown by a star: " + bad);
            else helper.succeed();
        });
    }

    /**
     * A star through a thin island keeps its core: a radius-4 fall on an island one block thick goes through it, and the
     * core, two seconds on, is still by the crater (on the ground under it, or hung in the hole), not lost to the void.
     */
    static void coreKeptThroughThinIsland(GameTestHelper helper) {
        var level = helper.getLevel();
        for (int x = 1; x <= 11; x++) for (int z = 1; z <= 11; z++) helper.setBlock(new BlockPos(x, 8, z), Blocks.END_STONE);
        BlockPos at = helper.absolutePos(new BlockPos(6, 8, 6));
        helper.runAtTickTime(2, () -> Starfall.impact(level, at, 4));
        helper.runAtTickTime(42, () -> {
            var cores = level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(12, 14, 12), i -> i.getItem().is(ModItems.STARFALL_CORE.get()));
            PromisedEnd.LOGGER.info("[starfall_core_kept_through_thin_island] cores {} at {}", cores.size(), cores.stream().map(i -> i.position().toString()).toList());
            if (cores.size() != 1) helper.fail(cores.size() + " cores by the crater through a thin island, not 1");
            else if (cores.getFirst().getY() < at.getY() - 13) helper.fail("the core fell to " + cores.getFirst().getY());
            else helper.succeed();
            cores.forEach(net.minecraft.world.entity.Entity::discard);
        });
    }

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
        // a survival player a block off the middle, falling, is thrown clear on their own client, the fall forgotten
        var player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var stood = helper.absoluteVec(new net.minecraft.world.phys.Vec3(7.5, 5, 6.5));
        player.snapTo(stood.x, stood.y, stood.z, 0, 0);
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        player.fallDistance = 4;
        Starfall.impact(level, at, 4);
        var compass = new net.minecraft.world.item.ItemStack(ModItems.STAR_COMPASS.get());
        com.panda.promisedend.item.StarCompassItem.aim(compass, level);
        var aim = compass.get(net.minecraft.core.component.DataComponents.LODESTONE_TRACKER);
        if (Starfall.pending(level.getServer()).isEmpty()
                && (aim == null || !aim.target().equals(java.util.Optional.of(net.minecraft.core.GlobalPos.of(net.minecraft.world.level.Level.END, at))) || aim.tracked()))
            bad.add("the Star Compass points at " + aim + ", not the last crater " + at.toShortString() + " in the End, untracked");
        var grid = new ArrayList<net.minecraft.world.item.ItemStack>();
        for (char c : " M MKM C ".toCharArray()) grid.add(switch (c) {
            case 'M' -> new net.minecraft.world.item.ItemStack(ModItems.STARMETAL.get());
            case 'K' -> new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COMPASS);
            case 'C' -> new net.minecraft.world.item.ItemStack(ModItems.STARFALL_CORE.get());
            default -> net.minecraft.world.item.ItemStack.EMPTY;
        });
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, grid);
        var made = level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level)
                .map(r -> r.value().assemble(input)).orElse(net.minecraft.world.item.ItemStack.EMPTY);
        if (!made.is(ModItems.STAR_COMPASS.get())) bad.add("the Star Compass's recipe makes " + made);
        var thrown = player.getDeltaMovement();
        if (thrown.x < 0.5 || thrown.y < 0.3) bad.add("the player wasn't thrown clear, away and up: " + thrown);
        if (!player.syncVelocity) bad.add("the throw isn't sent to the player's own client (syncVelocity)");
        if (player.fallDistance != 0) bad.add("the thrown player's fall wasn't forgotten: " + player.fallDistance);
        player.discard();
        if (!helper.getBlockState(new BlockPos(6, 2, 6)).is(Blocks.OBSIDIAN)) bad.add("the crater took the obsidian");
        if (!helper.getBlockState(middle).isAir() || !helper.getBlockState(middle.below()).isAir()) bad.add("the bowl's middle wasn't hollowed");
        if (!helper.getBlockState(new BlockPos(4, 5, 6)).isAir()) bad.add("the grass in the bowl stood");
        // past the rim the ground stands, burnt to Voidstone or not, or holding an ore of the crater's wall
        var past = helper.getBlockState(new BlockPos(11, 4, 6));
        if (!past.is(Blocks.END_STONE) && !past.is(ModBlocks.VOIDSTONE_SET.base().get()) && !past.is(ModBlocks.STARMETAL_ORE.get()))
            bad.add("the ground past the rim went: " + past);
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
        // the ore gives Starmetal to an iron pickaxe, itself to Silk Touch; the core is a fourth-favour offering
        var oreState = ModBlocks.STARMETAL_ORE.get().defaultBlockState();
        var drops = net.minecraft.world.level.block.Block.getDrops(oreState, level, at, null, null, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
        if (drops.size() != 1 || !drops.getFirst().is(ModItems.STARMETAL.get())) bad.add("the ore gives " + drops + " to an iron pickaxe");
        if (!oreState.is(net.minecraft.tags.BlockTags.NEEDS_IRON_TOOL)) bad.add("the ore doesn't need an iron pickaxe");
        var favour4 = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, PromisedEnd.id("offerings/favour_4"));
        if (!new net.minecraft.world.item.ItemStack(ModItems.STARFALL_CORE.get()).is(favour4)) bad.add("the core isn't a fourth-favour offering");
        PromisedEnd.LOGGER.info("[starfall_crater_respects_tag] {} Starmetal Ore, {} core(s)", ore, cores.size());
        if (!bad.isEmpty()) helper.fail("starfall: " + bad);
        helper.succeed();
    }
}
