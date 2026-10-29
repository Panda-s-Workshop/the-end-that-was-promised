package com.panda.promisedend.gametest;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.block.RiftGateBlock;
import com.panda.promisedend.logic.RiftGates;
import com.panda.promisedend.logic.RiftNetwork;
import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The Rift Gates: attuning, the network, the journey and what stops one. */
final class RiftGameTests {
    private RiftGameTests() {}

    private static void build(GameTestHelper helper, BlockPos core, Direction facing) {
        var level = helper.getLevel();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) level.setBlockAndUpdate(core.offset(dx, dy, 0), ModBlocks.RIFT_GATE_FRAME.get().defaultBlockState());
        }
        level.setBlockAndUpdate(core, ModBlocks.RIFT_GATE.get().defaultBlockState().setValue(RiftGateBlock.FACING, facing));
        // a floor on its stepping side, two deep and three wide, under room to stand
        var side = facing.getClockWise();
        for (int out = 1; out <= 2; out++) {
            for (int across = -1; across <= 1; across++) {
                BlockPos step = core.below().relative(facing, out).relative(side, across);
                level.setBlockAndUpdate(step.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(step, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(step.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static void use(GameTestHelper helper, ServerPlayer p, BlockPos core) {
        var hit = new BlockHitResult(Vec3.atCenterOf(core), Direction.SOUTH, core, false);
        helper.getLevel().getBlockState(core).useWithoutItem(helper.getLevel(), p, hit);
    }

    /**
     * A dormant gate used with nothing asks for its price and takes nothing; with a Mire Lily and a Resonite Shard it
     * joins the player's network, lights, and takes one of each. A second gate the same way. From the first the player
     * goes to the second and stands on its stepping side; again at once they wait; a gate not theirs, one they aren't
     * at, and one since broken are refused (the broken one leaves the network). A name tag renames it. The second gate
     * is attuned through the game's own use with the Mire Lily in hand, and no lily is planted. A second player's
     * network, names and wait are their own; their network full, a gate is refused and nothing taken, and a sneaking
     * use lets one go. A far side filled in refuses the journey; one cell beside it cleared, that is where they come
     * out.
     */
    static void riftGateAttuneAndTravel(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos a = helper.absolutePos(new BlockPos(2, 3, 2)), b = a.offset(10, 0, 0), c = a.offset(0, 0, 8);
        build(helper, a, Direction.SOUTH);
        build(helper, b, Direction.NORTH);
        build(helper, c, Direction.SOUTH);
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        RiftGates.forget(p);
        p.getInventory().clearContent();
        p.snapTo(a.getX() + 0.5, a.getY() - 1, a.getZ() + 1.5, 180, 0);
        List<String> bad = new ArrayList<>();
        use(helper, p, a);
        if (RiftGates.attuned(p, a) || level.getBlockState(a).getValue(RiftGateBlock.LIT)) bad.add("attuned with nothing to attune it");
        p.getInventory().add(new ItemStack(ModBlocks.MIRE_LILY.get(), 2));
        p.getInventory().add(new ItemStack(ModItems.RESONITE_SHARD.get(), 3));
        use(helper, p, a);
        if (!RiftGates.attuned(p, a) || !level.getBlockState(a).getValue(RiftGateBlock.LIT)) bad.add("not attuned with a lily and a shard");
        if (p.getInventory().countItem(ModBlocks.MIRE_LILY.get().asItem()) != 1 || p.getInventory().countItem(ModItems.RESONITE_SHARD.get()) != 2)
            bad.add("attuning took " + (2 - p.getInventory().countItem(ModBlocks.MIRE_LILY.get().asItem())) + " lilies, "
                    + (3 - p.getInventory().countItem(ModItems.RESONITE_SHARD.get())) + " shards");
        // the attuned gate used again opens the screen and takes nothing more
        use(helper, p, a);
        if (p.getInventory().countItem(ModItems.RESONITE_SHARD.get()) != 2) bad.add("a second use of an attuned gate took a shard");
        p.snapTo(b.getX() + 0.5, b.getY() - 1, b.getZ() - 1.5, 0, 0);
        // with the lily in hand, through the game's use as a click would: the gate is used, the lily isn't planted
        p.getInventory().setSelectedSlot(p.getInventory().findSlotMatchingItem(new ItemStack(ModBlocks.MIRE_LILY.get())));
        var lilyUse = p.gameMode.useItemOn(p, level, p.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(b), Direction.NORTH, b, false));
        if (!RiftGates.attuned(p, b) || RiftGates.network(p).size() != 2) bad.add("the second gate, lily in hand (" + lilyUse + "): network " + RiftGates.network(p));
        if (level.getBlockState(b.north()).is(ModBlocks.MIRE_LILY.get())) bad.add("the lily was planted on the gate");
        // the journey: from b to a, standing on a's stepping side (south of it, on its floor row)
        var went = RiftGates.travel(p, b, a);
        Vec3 at = p.position();
        if (went != RiftGates.Journey.WENT || Math.abs(at.x - (a.getX() + 0.5)) > 0.01 || Math.abs(at.z - (a.getZ() + 1.5)) > 0.01 || Math.abs(at.y - (a.getY() - 1)) > 0.01)
            bad.add("b to a: " + went + ", at " + at);
        var travelled = p.level().getServer().getAdvancements().get(com.panda.promisedend.logic.CodexBook.GATE_TRAVEL);
        if (travelled == null || !p.getAdvancements().getOrStartProgress(travelled).isDone()) bad.add("a journey didn't keep the Codex's gate goal");
        var again = RiftGates.travel(p, a, b);
        if (again != RiftGates.Journey.WAITING) bad.add("again at once: " + again);
        RiftGates.rest(p);
        // c isn't theirs; from b while standing at a; the same gate
        if (RiftGates.travel(p, a, c) != RiftGates.Journey.NOT_IN_NETWORK) bad.add("a gate not attuned was let through");
        if (RiftGates.travel(p, b, a) != RiftGates.Journey.NOT_AT_A_GATE) bad.add("a journey from a gate the player isn't at");
        if (RiftGates.travel(p, a, a) != RiftGates.Journey.SAME) bad.add("a journey to the same gate");
        // a name tag renames it in this player's network
        ItemStack tag = new ItemStack(Items.NAME_TAG);
        tag.set(DataComponents.CUSTOM_NAME, Component.literal("Home"));
        p.setItemInHand(InteractionHand.MAIN_HAND, tag);
        level.getBlockState(a).useItemOn(tag, level, p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(a), Direction.SOUTH, a, false));
        var named = RiftNetwork.find(RiftGates.network(p), level.dimension().identifier().toString(), a.asLong());
        if (named == null || !named.name().equals("Home") || tag.isEmpty()) bad.add("renamed to " + (named == null ? null : named.name()));
        // a second player: their own network, names and wait
        ServerPlayer q = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        RiftGates.forget(q);
        q.getInventory().clearContent();
        q.getInventory().add(new ItemStack(ModBlocks.MIRE_LILY.get(), 3));
        q.getInventory().add(new ItemStack(ModItems.RESONITE_SHARD.get(), 3));
        q.snapTo(a.getX() + 0.5, a.getY() - 1, a.getZ() + 1.5, 180, 0);
        use(helper, q, a);
        RiftGates.rename(q, a, "Theirs");
        var mine = RiftNetwork.find(RiftGates.network(p), level.dimension().identifier().toString(), a.asLong());
        if (RiftGates.network(q).size() != 1 || mine == null || !mine.name().equals("Home")) bad.add("a second player's network " + RiftGates.network(q) + " touched the first's: " + mine);
        q.snapTo(b.getX() + 0.5, b.getY() - 1, b.getZ() - 1.5, 0, 0);
        use(helper, q, b);
        RiftGates.travel(p, a, b);
        RiftGates.travel(p, b, a);
        var theirs = RiftGates.travel(q, b, a);
        if (theirs != RiftGates.Journey.WENT) bad.add("the first player's wait held the second: " + theirs);
        // the second player's network full: c is refused and costs nothing; a sneaking use lets a go
        for (int i = 0; RiftGates.network(q).size() < RiftNetwork.MOST; i++)
            RiftNetwork.add(RiftGates.network(q), new RiftNetwork.Gate("minecraft:the_end", i, "", "promisedend:lumen_mire"));
        q.snapTo(c.getX() + 0.5, c.getY() - 1, c.getZ() + 1.5, 180, 0);
        use(helper, q, c);
        if (RiftGates.attuned(q, c) || q.getInventory().countItem(ModItems.RESONITE_SHARD.get()) != 1) bad.add("a full network took c, or its price");
        q.snapTo(a.getX() + 0.5, a.getY() - 1, a.getZ() + 1.5, 180, 0);
        q.setShiftKeyDown(true);
        use(helper, q, a);
        q.setShiftKeyDown(false);
        if (RiftGates.attuned(q, a) || RiftGates.network(q).size() != RiftNetwork.MOST - 1) bad.add("a sneaking use didn't let the gate go: " + RiftGates.network(q).size());
        if (!level.getBlockState(a).getValue(RiftGateBlock.LIT)) bad.add("a gate let go went dark for everyone");
        RiftGates.forget(q);
        level.getServer().getPlayerList().remove(q);
        // a's far side filled in: refused; one cell beside the step cleared: out there
        RiftGates.rest(p);
        p.snapTo(b.getX() + 0.5, b.getY() - 1, b.getZ() - 1.5, 0, 0);
        var side = Direction.SOUTH.getClockWise();
        for (int out = 1; out <= 2; out++)
            for (int across = -1; across <= 1; across++) level.setBlockAndUpdate(a.below().relative(Direction.SOUTH, out).relative(side, across), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var blocked = RiftGates.travel(p, b, a);
        if (blocked != RiftGates.Journey.BLOCKED) bad.add("a filled-in far side: " + blocked);
        BlockPos aside = a.below().south().relative(side);
        level.setBlockAndUpdate(aside, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        var sideways = RiftGates.travel(p, b, a);
        if (sideways != RiftGates.Journey.WENT || !p.blockPosition().equals(aside)) bad.add("one cell cleared beside the step: " + sideways + ", at " + p.blockPosition() + " not " + aside);
        RiftGates.rest(p);
        // b broken (as a creative player could): the journey there is refused and b leaves the network
        level.setBlockAndUpdate(b, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        var gone = RiftGates.travel(p, a, b);
        if (gone != RiftGates.Journey.GONE || RiftGates.attuned(p, b)) bad.add("a broken gate: " + gone + ", still in the network " + RiftGates.attuned(p, b));
        PromisedEnd.LOGGER.info("[rift_gate_attune_and_travel] network {}; b to a {}, at {}; again {}; the second player's {}; filled in {}, then {}; to a broken gate {}",
                RiftGates.network(p), went, at, again, theirs, blocked, sideways, gone);
        RiftGates.forget(p);
        level.getServer().getPlayerList().remove(p);
        if (!bad.isEmpty()) helper.fail("rift gates: " + bad);
        helper.succeed();
    }
}
