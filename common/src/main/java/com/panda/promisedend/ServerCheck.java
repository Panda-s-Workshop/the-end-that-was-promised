package com.panda.promisedend;

import com.panda.promisedend.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;

/**
 * A development check of the End on a dedicated server: with {@code -Dpromisedend.servercheck=true} (Gradle
 * {@code -PendServerCheck}), once the server has started it generates the End around (300, 0) and logs
 * {@code [promisedend servercheck] PASS} or {@code FAIL}. Booting to "Done" alone never generates an End chunk, so it
 * can't show that the worldgen runs without a client.
 */
public final class ServerCheck {
    public static final String PROPERTY = "promisedend.servercheck";
    private static final int RADIUS = 64;

    private ServerCheck() {}

    public static void onServerStarted(MinecraftServer server) {
        if (!Boolean.getBoolean(PROPERTY) || !server.isDedicatedServer()) return;
        String failure = null;
        try {
            failure = check(server);
        } catch (RuntimeException e) {
            PromisedEnd.LOGGER.error("[promisedend servercheck] threw", e);
            failure = e.toString();
        }
        if (failure == null) PromisedEnd.LOGGER.info("[promisedend servercheck] PASS");
        else PromisedEnd.LOGGER.error("[promisedend servercheck] FAIL: {}", failure);
    }

    private static String check(MinecraftServer server) {
        var end = server.getLevel(Level.END);
        if (end == null) return "no End level";
        BlockPos centre = new BlockPos(300, 64, 0);
        for (int cx = (centre.getX() - RADIUS) >> 4; cx <= (centre.getX() + RADIUS) >> 4; cx++) {
            for (int cz = (centre.getZ() - RADIUS) >> 4; cz <= (centre.getZ() + RADIUS) >> 4; cz++) end.getChunk(cx, cz);
        }
        var biome = end.getBiome(centre);
        String id = biome.unwrapKey().map(k -> k.identifier().toString()).orElse("?");
        PromisedEnd.LOGGER.info("[promisedend servercheck] biome at {}: {}", centre, id);
        if (!id.equals("promisedend:shattered_rim")) return "expected the Shattered Rim at x=300, got " + id;
        if (!biome.is(TagKey.create(Registries.BIOME, Identifier.parse("c:is_end")))) return "the Shattered Rim isn't in #c:is_end";
        int voidstone = 0;
        var block = ModBlocks.VOIDSTONE_SET.base().get();
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                for (int y = 40; y <= 90; y++) {
                    if (end.getBlockState(new BlockPos(centre.getX() + x, y, centre.getZ() + z)).is(block)) voidstone++;
                }
            }
        }
        PromisedEnd.LOGGER.info("[promisedend servercheck] voidstone within {} of {}: {}", RADIUS, centre, voidstone);
        return voidstone < 200 ? "too little Voidstone around x=300: " + voidstone : null;
    }
}
