package com.panda.promisedend.block;

import com.mojang.serialization.Codec;
import com.panda.promisedend.EndConfig;
import com.panda.promisedend.logic.Offerings;
import com.panda.promisedend.logic.ShrineBoons;
import com.panda.promisedend.network.ShrinePayloads;
import com.panda.promisedend.platform.Services;
import com.panda.promisedend.registry.ModBlockEntities;
import com.panda.promisedend.registry.ModSounds;
import com.panda.promisedend.registry.ModTags;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A Shrine Altar's memory, per player: the favour offered so far, the boons it is offering them, and when they last
 * took one here. Offerings are tagged {@code #promisedend:offerings/favour_<n>}; at {@link Offerings#ASK} favour the
 * altar offers three boons and the player's screen opens.
 */
public class ShrineAltarBlockEntity extends BlockEntity {
    /** How near the altar a player must be to pick (the screen can be left open while they walk off). */
    public static final double REACH = 6.0;

    private final Map<UUID, Integer> favour = new HashMap<>();
    private final Map<UUID, List<Integer>> offered = new HashMap<>();
    private final Map<UUID, Long> taken = new HashMap<>();

    public ShrineAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHRINE_ALTAR.get(), pos, state);
    }

    /** The favour {@code stack} is worth as an offering, 0 if it isn't one. */
    public static int worth(ItemStack stack) {
        for (int n = Offerings.MOST; n >= 1; n--) if (stack.is(ModTags.favour(n))) return n;
        return 0;
    }

    public int favour(UUID player) {
        return favour.getOrDefault(player, 0);
    }

    public List<Integer> offered(UUID player) {
        return offered.getOrDefault(player, List.of());
    }

    /** Ticks until {@code player} may take another boon here. */
    public long waitLeft(ServerPlayer player) {
        return Offerings.waitLeft(taken.getOrDefault(player.getUUID(), -1L), player.level().getGameTime(), EndConfig.shrineCooldownTicks());
    }

    /**
     * {@code player} lays {@code stack} on the altar: one is taken and its favour counted, and at enough favour the
     * three boons are drawn and their screen opens. Returns false (nothing taken) while the altar is resting for them.
     */
    public boolean offer(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.level();
        int worth = worth(stack);
        if (worth == 0) return false;
        long wait = waitLeft(player);
        if (wait > 0) {
            player.sendOverlayMessage(Component.translatable("message.promisedend.shrine.resting", Math.max(1, (wait + 1199) / 1200)));
            return false;
        }
        if (!offered(player.getUUID()).isEmpty()) {
            // a choice is already waiting: show it again, take nothing
            send(player);
            return false;
        }
        stack.consume(1, player);
        int now = Offerings.add(favour(player.getUUID()), worth);
        favour.put(player.getUUID(), now);
        BlockPos p = getBlockPos();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 12 * worth, 0.25, 0.2, 0.25, 0.04);
        level.playSound(null, p, ModSounds.SHRINE_OFFER.get(), SoundSource.BLOCKS, 1.0F, 0.8F + 0.1F * Math.min(now, Offerings.ASK));
        if (Offerings.ready(now)) {
            int[] boons = Offerings.choices(Offerings.seed(p.asLong(), player.getUUID().getMostSignificantBits() ^ player.getUUID().getLeastSignificantBits(), level.getGameTime()));
            offered.put(player.getUUID(), List.of(boons[0], boons[1], boons[2]));
            level.playSound(null, p, ModSounds.SHRINE_READY.get(), SoundSource.BLOCKS, 1.2F, 1.0F);
            send(player);
        } else {
            player.sendOverlayMessage(Component.translatable("message.promisedend.shrine.favour", now, Offerings.ASK));
        }
        setChanged();
        return true;
    }

    /** Opens the player's screen on what this altar offers them now. */
    public void send(ServerPlayer player) {
        Services.PLATFORM.sendToPlayer(player, new ShrinePayloads.Offer(getBlockPos(), offered(player.getUUID())));
    }

    /**
     * {@code player} picks {@code boon}: if this altar offered it to them and they are at it, it is theirs, the favour
     * spent and the altar rests for them. Returns whether it was granted.
     */
    public boolean choose(ServerPlayer player, int boon) {
        UUID id = player.getUUID();
        if (!offered(id).contains(boon)) return false;
        if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(getBlockPos())) > REACH * REACH) {
            // the offer stands: back at the altar, an empty hand opens it again
            player.sendOverlayMessage(Component.translatable("message.promisedend.shrine.too_far"));
            return false;
        }
        ShrineBoons.grant(player, boon);
        offered.remove(id);
        favour.put(id, favour(id) - Offerings.ASK);
        if (favour(id) <= 0) favour.remove(id);
        taken.put(id, player.level().getGameTime());
        BlockPos p = getBlockPos();
        player.level().sendParticles(ParticleTypes.END_ROD, p.getX() + 0.5, p.getY() + 1.4, p.getZ() + 0.5, 30, 0.3, 0.6, 0.3, 0.06);
        player.level().playSound(null, p, ModSounds.SHRINE_GRANT.get(), SoundSource.BLOCKS, 1.2F, 1.0F);
        setChanged();
        return true;
    }

    /** The payload's way in: the altar at {@code pos} in the player's level, if it is one. */
    public static void chosen(ServerPlayer player, ShrinePayloads.Choose choice) {
        if (!player.level().isLoaded(choice.altar())) return;
        if (player.level().getBlockEntity(choice.altar()) instanceof ShrineAltarBlockEntity altar) altar.choose(player, choice.boon());
    }

    /** Forgets every player (the command and tests). */
    public void reset() {
        favour.clear();
        offered.clear();
        taken.clear();
        setChanged();
    }

    private static final Codec<Map<UUID, Integer>> FAVOUR = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT);
    private static final Codec<Map<UUID, List<Integer>>> OFFERED = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT.listOf());
    private static final Codec<Map<UUID, Long>> TAKEN = Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG);

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("favour", FAVOUR, favour);
        output.store("offered", OFFERED, offered);
        output.store("taken", TAKEN, taken);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        favour.clear();
        favour.putAll(input.read("favour", FAVOUR).orElse(Map.of()));
        offered.clear();
        offered.putAll(input.read("offered", OFFERED).orElse(Map.of()));
        taken.clear();
        taken.putAll(input.read("taken", TAKEN).orElse(Map.of()));
    }
}
