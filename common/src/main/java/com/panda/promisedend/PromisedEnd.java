package com.panda.promisedend;

import com.mojang.logging.LogUtils;
import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModItems;
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
    private static final List<Class<?>> CONTENT = List.of(ModBlocks.class, ModItems.class, ModTabs.class, ModWorldgen.class);

    private static boolean initialised;

    /** Opens the Codex screen; each loader's client entry sets it (common code can't name client classes). */
    public static Runnable openCodex = () -> {};

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
    }

    /** For each server player at the end of every server tick (both loaders). */
    public static void onPlayerTick(net.minecraft.server.level.ServerPlayer player) {
        com.panda.promisedend.logic.VoidAnchor.tick(player);
        com.panda.promisedend.logic.RimProgress.tick(player);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
