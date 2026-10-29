package com.panda.promisedend.block;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.item.StarChartItem;
import com.panda.promisedend.registry.ModBlockEntities;
import com.panda.promisedend.registry.ModItems;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import com.panda.promisedend.registry.ModComponents;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A Star Chart Desk's memory: who has taken a chart from it. Each player takes one, drawn from the loot table
 * {@code promisedend:gameplay/star_chart_desk} (a pack sets its kinds and weights there); a chart of a kind with no
 * places in this world (its structure tag empty) is drawn again.
 */
public class StarChartDeskBlockEntity extends BlockEntity {
    public static final ResourceKey<LootTable> TABLE = ResourceKey.create(Registries.LOOT_TABLE, PromisedEnd.id("gameplay/star_chart_desk"));
    /** How many draws a desk makes before it gives up on finding a kind with places. */
    static final int DRAWS = 16;

    private final Set<UUID> taken = new HashSet<>();

    public StarChartDeskBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STAR_CHART_DESK.get(), pos, state);
    }

    public boolean hasTaken(UUID player) {
        return taken.contains(player);
    }

    /** Whether a chart of {@code kind} can find anything in this world: its tag names at least one structure. */
    public static boolean charted(ServerLevel level, Identifier kind) {
        return level.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(StarChartItem.tag(kind)).map(s -> s.size() > 0).orElse(false);
    }

    /** One roll of the desk's table: the sealed chart it holds, or empty. */
    public static ItemStack roll(ServerLevel level, BlockPos at) {
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(at)).create(LootContextParamSets.CHEST);
        for (ItemStack s : level.getServer().reloadableRegistries().getLootTable(TABLE).getRandomItems(params)) {
            if (s.is(ModItems.STAR_CHART.get()) && s.has(ModComponents.STAR_CHART.get())) return s;
        }
        return ItemStack.EMPTY;
    }

    /** The chart this desk gives: drawn until it is of a kind this world can chart, or empty. */
    public ItemStack draw() {
        ServerLevel server = (ServerLevel) level;
        for (int i = 0; i < DRAWS; i++) {
            ItemStack chart = roll(server, worldPosition);
            if (!chart.isEmpty() && charted(server, chart.get(ModComponents.STAR_CHART.get()))) return chart;
        }
        return ItemStack.EMPTY;
    }

    /** Forgets every player (tests). */
    public void reset() {
        taken.clear();
        setChanged();
    }

    /** Gives this player their chart, or tells them they have had it. */
    public boolean take(ServerPlayer player) {
        if (taken.contains(player.getUUID())) {
            player.sendOverlayMessage(Component.translatable("message.promisedend.star_chart_desk.taken"));
            return false;
        }
        ItemStack chart = draw();
        if (chart.isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.promisedend.star_chart_desk.empty"));
            return false;
        }
        player.getInventory().placeItemBackInInventory(chart, net.minecraft.util.Prediction.SERVER_ONLY);
        taken.add(player.getUUID());
        setChanged();
        level.playSound(null, worldPosition, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1, 1);
        player.sendOverlayMessage(Component.translatable("message.promisedend.star_chart_desk.took", chart.getHoverName()));
        return true;
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.store("taken", UUIDUtil.STRING_CODEC.listOf(), List.copyOf(taken));
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        taken.clear();
        in.read("taken", UUIDUtil.STRING_CODEC.listOf()).ifPresent(taken::addAll);
    }
}
