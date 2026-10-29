package com.panda.promisedend.item;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.registry.ModComponents;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

/**
 * A Star Chart: a sealed chart that names a kind of place (a structure tag in its {@code promisedend:star_chart}
 * component). Unrolled, it looks for the nearest such place from where it is read that no chart has led to yet, and
 * becomes a map marked with it, named for it: as vanilla's treasure maps, each place is charted once, so the next chart
 * leads on rather than back. Where none lies within reach it stays sealed and says so.
 */
public class StarChartItem extends Item {
    /**
     * How far an unrolled chart looks, in rings of its kind's placement grid (not chunks): 32 rings of the Foundry's
     * 20-chunk cells is 10,000 blocks. Vanilla's explorer maps look 100.
     */
    public static final int SEARCH_RINGS = 32;
    public static final byte ZOOM = 3;

    public StarChartItem(Properties properties) {
        super(properties);
    }

    public static ItemStack of(Item chart, Identifier kind) {
        ItemStack stack = new ItemStack(chart);
        stack.set(ModComponents.STAR_CHART.get(), kind);
        return stack;
    }

    public static TagKey<Structure> tag(Identifier kind) {
        return TagKey.create(Registries.STRUCTURE, kind);
    }

    /** The lang key for a chart of this kind: {@code item.promisedend.star_chart.<what>} for {@code on_<what>_charts}. */
    public static String nameKey(Identifier kind) {
        String what = kind.getPath().replaceFirst("^on_", "").replaceFirst("_charts$", "");
        return "item." + kind.getNamespace() + ".star_chart." + what;
    }

    /** A chart's name: its kind's, or, for a kind a pack added without a name, "Star Chart". */
    public static Component name(Identifier kind) {
        return Component.translatableWithFallback(nameKey(kind), Component.translatable("item." + PromisedEnd.MODID + ".star_chart").getString());
    }

    @Override
    public Component getName(ItemStack stack) {
        Identifier kind = stack.get(ModComponents.STAR_CHART.get());
        return kind == null ? super.getName(stack) : name(kind);
    }

    /**
     * Unrolls the chart into a marked map, or null when none of its kind lies within reach. The place found is charted:
     * no later chart leads to it.
     */
    public static ItemStack unroll(ServerLevel level, BlockPos from, Identifier kind) {
        BlockPos found = level.findNearestMapStructure(tag(kind), from, SEARCH_RINGS, true);
        if (found == null) return null;
        // on the mark, as vanilla's explorer maps: the map snaps its centre to a grid as wide as itself, so a map
        // centred anywhere else can leave the mark off it; the reader, off it, shows at its edge on their way. No biome
        // preview, which only draws water and the End has none
        ItemStack map = MapItem.create(level, found.getX(), found.getZ(), ZOOM, true, true);
        MapItemSavedData.addTargetDecoration(map, found, "+", MapDecorationTypes.TARGET_X);
        map.set(DataComponents.ITEM_NAME, name(kind));
        map.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("tooltip.promisedend.star_chart.unrolled").withStyle(ChatFormatting.GRAY))));
        return map;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Identifier kind = stack.get(ModComponents.STAR_CHART.get());
        if (kind == null) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.promisedend.star_chart.blank"));
            return InteractionResult.FAIL;
        }
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        ItemStack map = unroll(server, player.blockPosition(), kind);
        if (map == null) {
            player.sendOverlayMessage(Component.translatable("message.promisedend.star_chart.none"));
            player.getCooldowns().addCooldown(stack, 40);
            return InteractionResult.FAIL;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1, 1);
        stack.consume(1, player);
        // as vanilla's empty map: the last chart becomes the map in hand, one of several goes to the inventory
        if (stack.isEmpty()) return InteractionResult.SUCCESS.heldItemTransformedTo(map);
        player.getInventory().placeItemBackInInventory(map, net.minecraft.util.Prediction.SERVER_ONLY);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Component.translatable("tooltip.promisedend.star_chart.0").withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.promisedend.star_chart.1").withStyle(ChatFormatting.GRAY));
    }
}
