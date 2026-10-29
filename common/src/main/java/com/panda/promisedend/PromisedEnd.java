package com.panda.promisedend;

import com.mojang.logging.LogUtils;
import com.panda.promisedend.registry.ModBlockEntities;
import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModEntities;
import com.panda.promisedend.registry.ModItems;
import com.panda.promisedend.registry.ModParticles;
import com.panda.promisedend.registry.ModSounds;
import com.panda.promisedend.registry.ModTabs;
import com.panda.promisedend.registry.ModWorldgen;
import java.util.List;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

/** The mod's common side. Each loader module has its own entry point, which calls {@link #init()} first. */
public final class PromisedEnd {
    public static final String MODID = "promisedend";
    public static final Logger LOGGER = LogUtils.getLogger();

    /** The classes whose static DeferredRegisters hold the mod's content. */
    private static final List<Class<?>> CONTENT = List.of(ModSounds.class, com.panda.promisedend.registry.ModComponents.class, ModEntities.class, ModBlocks.class, ModBlockEntities.class, ModItems.class, ModTabs.class,
            ModWorldgen.class, ModParticles.class, com.panda.promisedend.registry.ModEffects.class,
            com.panda.promisedend.registry.ModRecipes.class);

    private static boolean initialised;

    /** Opens the Codex screen; each loader's client entry sets it (common code can't name client classes). */
    public static Runnable openCodex = () -> {};
    /** Sends a payload to the server, if it knows it; each loader's client entry sets it. */
    public static java.util.function.Consumer<net.minecraft.network.protocol.common.custom.CustomPacketPayload> toServer = payload -> {};

    private PromisedEnd() {}

    /** Loads the content classes so every registration is queued before the loader registers them. */
    public static void init() {
        for (Class<?> c : CONTENT) {
            try {
                Class.forName(c.getName(), true, c.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        initialised = true;
        LOGGER.info("The End That Was Promised booting on {}", com.panda.promisedend.platform.Services.PLATFORM.loaderName());
    }

    /** Whether {@link #init()} ran. */
    public static boolean initialised() {
        return initialised;
    }

    /** Before the server loads its levels: the End's region picker needs this world's seed. */
    public static void onServerStarting(net.minecraft.server.MinecraftServer server) {
        com.panda.promisedend.worldgen.EndBiomes.setSeed(server.getWorldGenSettings().options().seed());
        com.panda.promisedend.block.EchoAmberClusterBlock.forget();
    }

    /** At the end of every server tick (both loaders). */
    public static void onServerTick(net.minecraft.server.MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(PromisedEnd::onPlayerTick);
        com.panda.promisedend.logic.DragonWrath.tick(server);
        com.panda.promisedend.logic.DragonPhase3.tick(server);
        com.panda.promisedend.logic.VoidCurrents.tick(server);
        com.panda.promisedend.logic.AncientWard.tick(server);
    }

    /** A player joined the server: their client is told what it can't read itself. */
    public static void onPlayerJoin(net.minecraft.server.level.ServerPlayer player) {
        com.panda.promisedend.platform.Services.PLATFORM.sendToPlayer(player, new com.panda.promisedend.network.TerrainPayload(EndConfig.terrainEnabled()));
    }

    /** For each server player at the end of every server tick. */
    public static void onPlayerTick(net.minecraft.server.level.ServerPlayer player) {
        com.panda.promisedend.logic.VoidAnchor.tick(player);
        com.panda.promisedend.item.VoidTouched.tick(player);
        com.panda.promisedend.logic.RimProgress.tick(player);
        com.panda.promisedend.logic.OuterProgress.tick(player);
        com.panda.promisedend.logic.CaveDark.tick(player);
        com.panda.promisedend.logic.AncientWard.tick(player);
        com.panda.promisedend.logic.PearlGuide.tick(player);
        com.panda.promisedend.logic.ShrineBoons.tick(player);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
