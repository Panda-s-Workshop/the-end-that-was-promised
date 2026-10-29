package com.panda.promisedend.gametest;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.block.StarChartDeskBlockEntity;
import com.panda.promisedend.item.StarChartItem;
import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModComponents;
import com.panda.promisedend.registry.ModItems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Star Charts and the Fallen Observatory's desk. Where a chart's search finds a place is the server check's. */
final class ChartGameTests {
    private ChartGameTests() {}

    /**
     * A chart names its kind (a kind a pack adds without a name reads "Star Chart"). Used where nothing of its kind
     * lies within reach (this world makes no structures), it stays sealed, in hand, and says so; a blank chart
     * does nothing.
     */
    static void starChartLocates(GameTestHelper helper) {
        var level = helper.getLevel();
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        List<String> bad = new ArrayList<>();
        Map<String, String> names = new HashMap<>();
        for (String kind : List.of("foundry", "observatory", "rift_maw", "citadel", "nowhere")) {
            ItemStack chart = StarChartItem.of(ModItems.STAR_CHART.get(), PromisedEnd.id("on_" + kind + "_charts"));
            var name = chart.getHoverName();
            names.put(kind, name.getString());
            String key = name.getContents() instanceof TranslatableContents t ? t.getKey() : "";
            if (!kind.equals("nowhere") && !key.equals("item.promisedend.star_chart." + kind)) bad.add(kind + " named " + key);
        }
        if (!names.get("nowhere").equals(new ItemStack(ModItems.STAR_CHART.get()).getHoverName().getString())) bad.add("a pack's kind named " + names.get("nowhere"));
        ItemStack chart = StarChartItem.of(ModItems.STAR_CHART.get(), PromisedEnd.id("on_foundry_charts"));
        chart.setCount(2);
        p.setItemInHand(InteractionHand.MAIN_HAND, chart);
        var result = chart.use(level, p, InteractionHand.MAIN_HAND);
        ItemStack held = p.getMainHandItem();
        if (!held.is(ModItems.STAR_CHART.get()) || held.getCount() != 2) bad.add("a chart that found nothing became " + held + " (" + result + ")");
        if (!p.getCooldowns().isOnCooldown(held)) bad.add("no pause after a search that found nothing");
        ItemStack blank = new ItemStack(ModItems.STAR_CHART.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, blank);
        blank.use(level, p, InteractionHand.MAIN_HAND);
        if (!p.getMainHandItem().is(ModItems.STAR_CHART.get())) bad.add("a blank chart became " + p.getMainHandItem());
        PromisedEnd.LOGGER.info("[star_chart_locates] names {}; a Foundry chart with none in reach: {}, still {}", names, result, held);
        level.getServer().getPlayerList().remove(p);
        if (!bad.isEmpty()) helper.fail("star charts: " + bad);
        helper.succeed();
    }

    /**
     * A Star Chart Desk gives each player one sealed chart, and a second use nothing; it draws only kinds this world
     * can chart (the Rift Maw's and the Citadel's tags are empty), by weight; it keeps who has taken through a save
     * and load.
     */
    static void deskGivesOneChart(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(at, ModBlocks.STAR_CHART_DESK.get().defaultBlockState());
        if (!(level.getBlockEntity(at) instanceof StarChartDeskBlockEntity desk)) {
            helper.fail("no desk block entity");
            return;
        }
        List<String> bad = new ArrayList<>();
        // what the desk gives in this world: only kinds with places (the Rift Maw's and the Citadel's tags are empty)
        java.util.Set<String> kinds = new java.util.TreeSet<>();
        for (int i = 0; i < 40; i++) {
            ItemStack drawn = desk.draw();
            kinds.add(drawn.isEmpty() ? "nothing" : drawn.get(ModComponents.STAR_CHART.get()).getPath());
        }
        if (!kinds.equals(java.util.Set.of("on_foundry_charts"))) bad.add("40 draws gave " + kinds);
        // the table's own weights, every kind as it will be once the later places exist: 3:3:2
        Map<String, Integer> drawn = new HashMap<>();
        for (int i = 0; i < 800; i++) {
            ItemStack roll = StarChartDeskBlockEntity.roll(level, at);
            drawn.merge(roll.isEmpty() ? "nothing" : roll.get(ModComponents.STAR_CHART.get()).getPath(), 1, Integer::sum);
        }
        Map<String, Integer> want = Map.of("on_foundry_charts", 300, "on_rift_maw_charts", 300, "on_citadel_charts", 200);
        for (var w : want.entrySet()) if (Math.abs(drawn.getOrDefault(w.getKey(), 0) - w.getValue()) > 60) bad.add("800 rolls " + drawn + " (by weight 3:3:2)");
        if (drawn.size() != 3) bad.add("800 rolls " + drawn);
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        ServerPlayer q = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        p.getInventory().clearContent();
        q.getInventory().clearContent();
        boolean first = desk.take(p), second = desk.take(p);
        int pCharts = p.getInventory().countItem(ModItems.STAR_CHART.get());
        net.minecraft.resources.Identifier pKind = null;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) if (s.is(ModItems.STAR_CHART.get())) pKind = s.get(ModComponents.STAR_CHART.get());
        if (pKind == null || !kinds.contains(pKind.getPath())) bad.add("the chart taken is of " + pKind);
        if (!first || second || pCharts != 1) bad.add("one player: took " + first + ", again " + second + ", " + pCharts + " charts");
        if (!desk.take(q) || q.getInventory().countItem(ModItems.STAR_CHART.get()) != 1) bad.add("a second player got no chart");
        var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
        desk.saveWithoutMetadata(out);
        var copy = new StarChartDeskBlockEntity(at, desk.getBlockState());
        copy.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
        if (!copy.hasTaken(p.getUUID()) || !copy.hasTaken(q.getUUID())) bad.add("the takers weren't kept through a save and load");
        PromisedEnd.LOGGER.info("[desk_gives_one_chart] kinds {}; 800 draws {}; a player took {} ({}), again {}", kinds, drawn, first, pKind, second);
        level.getServer().getPlayerList().remove(p);
        level.getServer().getPlayerList().remove(q);
        if (!bad.isEmpty()) helper.fail("desk: " + bad);
        helper.succeed();
    }
}
