package com.panda.promisedend.registry;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.platform.Services;
import com.panda.promisedend.platform.registry.DeferredRegister;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** The mod's creative tab: every item, blocks first, in registration order. */
public final class ModTabs {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PromisedEnd.MODID);

    /** The Star Charts the tab shows, one of each named kind. */
    private static final java.util.List<String> CHART_KINDS = java.util.List.of("foundry", "observatory", "rift_maw", "citadel");

    public static final Supplier<CreativeModeTab> MAIN = TABS.register("main", () -> Services.PLATFORM.tabBuilder()
            .title(Component.translatable("itemGroup.promisedend"))
            .icon(() -> new ItemStack(ModBlocks.CHISELED_VOIDSTONE.get()))
            .displayItems((params, output) -> {
                // ModItems' own items register before the block items (ModBlocks touches ModItems first), so sort here
                for (var h : ModItems.ITEMS.getEntries()) if (h.get() instanceof BlockItem) output.accept(h.get());
                for (var h : ModItems.ITEMS.getEntries()) {
                    if (h.get() == ModItems.STAR_CHART.get()) {
                        // a blank chart does nothing: one of each kind instead
                        for (String kind : CHART_KINDS) output.accept(com.panda.promisedend.item.StarChartItem.of(ModItems.STAR_CHART.get(), PromisedEnd.id("on_" + kind + "_charts")));
                    } else if (!(h.get() instanceof BlockItem)) output.accept(h.get());
                }
            })
            .build());

    private ModTabs() {}
}
