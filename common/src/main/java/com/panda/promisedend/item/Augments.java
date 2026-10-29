package com.panda.promisedend.item;

import com.mojang.serialization.Codec;
import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.registry.ModComponents;
import com.panda.promisedend.registry.ModTags;
import io.netty.buffer.ByteBuf;
import com.panda.promisedend.logic.LatticeBoost;
import com.panda.promisedend.registry.ModSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.TooltipProvider;

/**
 * An elytra's augments, the {@code promisedend:augments} component: up to {@link #MOST}, each once, smithed on
 * (ElytraAugmentRecipe). The item stays the elytra it was, so its enchantments and any other mod's handling keep
 * working. Drift Weave takes {@link #WEAVE_WEAR} off a glide's wear; the Leviathan Fin lifts a glider in an updraft
 * {@link #FIN_LIFT} more and speeds them along a current {@link #FIN_CURRENT} more; the Architect's Lattice gives a
 * glider a rocket's boost on the jump key, once every {@link LatticeBoost#COOLDOWN} ticks ({@link #boost}).
 */
public record Augments(List<Identifier> list) implements TooltipProvider {
    public static final int MOST = 3;
    public static final Identifier DRIFT_WEAVE = PromisedEnd.id("drift_weave");
    public static final Identifier LEVIATHAN_FIN = PromisedEnd.id("leviathan_fin");
    public static final Identifier ARCHITECTS_LATTICE = PromisedEnd.id("architects_lattice");
    /** The share of a glide's wear Drift Weave takes off. */
    public static final double WEAVE_WEAR = 0.4;
    /** What the Fin adds to an updraft's lift, as a share of the lift (a Void-touched set adds its own). */
    public static final double FIN_LIFT = 0.5;
    /** What the Fin adds to a current's push on a glider, as a share of the glider's speed (the current gives 0.3). */
    public static final double FIN_CURRENT = 0.3;

    public static final Augments NONE = new Augments(List.of());
    /** Each player's last boost, by game time; kept for the server's run. */
    private static final Map<UUID, Long> BOOSTED = new HashMap<>();
    public static final Codec<Augments> CODEC = Identifier.CODEC.sizeLimitedListOf(MOST).xmap(Augments::new, Augments::list);
    public static final StreamCodec<ByteBuf, Augments> STREAM_CODEC =
            Identifier.STREAM_CODEC.apply(ByteBufCodecs.list(MOST)).map(Augments::new, Augments::list);

    public Augments {
        list = List.copyOf(list);
    }

    public static Augments of(ItemStack stack) {
        return stack.getOrDefault(ModComponents.AUGMENTS.get(), NONE);
    }

    public boolean has(Identifier augment) {
        return list.contains(augment);
    }

    /** The elytra with {@code augment} added, or empty if it can't take it: not an augmentable glider, already on, or full. */
    public static ItemStack apply(ItemStack elytra, Identifier augment) {
        if (elytra.isEmpty() || !elytra.is(ModTags.AUGMENTABLE_ELYTRA)) return ItemStack.EMPTY;
        Augments now = of(elytra);
        if (now.has(augment) || now.list.size() >= MOST) return ItemStack.EMPTY;
        List<Identifier> more = new ArrayList<>(now.list);
        more.add(augment);
        ItemStack out = elytra.copyWithCount(1);
        out.set(ModComponents.AUGMENTS.get(), new Augments(more));
        return out;
    }

    /** Whether {@code e} is gliding on a glider worn on the chest with {@code augment}. */
    public static boolean gliding(Entity e, Identifier augment) {
        return e instanceof Player p && p.isFallFlying() && of(p.getItemBySlot(EquipmentSlot.CHEST)).has(augment);
    }

    /** What the Fin adds to an updraft's lift for {@code e}. */
    public static double lift(Entity e) {
        return gliding(e, LEVIATHAN_FIN) ? FIN_LIFT : 0.0;
    }

    /**
     * A glider pressed the jump key ({@code BoostPayload}): gliding on the Architect's Lattice and its lattice set, it
     * is boosted as by a flight-1 rocket (one is fired from it, as vanilla's); still setting, it is told how long.
     * Returns whether it boosted.
     */
    public static boolean boost(ServerPlayer player) {
        if (!gliding(player, ARCHITECTS_LATTICE) || !(player.level() instanceof ServerLevel level)) return false;
        long now = level.getGameTime();
        long left = LatticeBoost.left(BOOSTED.getOrDefault(player.getUUID(), LatticeBoost.NEVER), now);
        if (left > 0) {
            player.sendOverlayMessage(Component.translatable("message.promisedend.lattice_setting", LatticeBoost.seconds(left)));
            level.playSound(null, player.blockPosition(), ModSounds.LATTICE_BOOST_SETTING.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
            return false;
        }
        BOOSTED.put(player.getUUID(), now);
        ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
        rocket.set(DataComponents.FIREWORKS, new Fireworks(1, List.of()));
        Projectile.spawnProjectile(new FireworkRocketEntity(level, rocket, player), level, rocket);
        level.playSound(null, player.blockPosition(), ModSounds.LATTICE_BOOST.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.5, player.getZ(), 12, 0.4, 0.4, 0.4, 0.05);
        return true;
    }

    /** Forgets the boosts (a new server; tests). */
    public static void forgetBoosts() {
        BOOSTED.clear();
    }

    /** The share of a glide's wear the worn glider keeps (Drift Weave's 0.6, or all of it). */
    public static double wearKept(ItemStack glider) {
        return of(glider).has(DRIFT_WEAVE) ? 1.0 - WEAVE_WEAR : 1.0;
    }

    @Override
    public void addToTooltip(Item.TooltipContext context, Consumer<Component> out, TooltipFlag flag, DataComponentGetter components) {
        if (list.isEmpty()) return;
        out.accept(Component.translatable("tooltip.promisedend.augments", list.size(), MOST).withStyle(ChatFormatting.GRAY));
        for (Identifier augment : list) {
            out.accept(Component.literal(" ").append(Component.translatable("augment." + augment.getNamespace() + "." + augment.getPath()))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
