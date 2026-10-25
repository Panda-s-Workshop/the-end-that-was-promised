package com.panda.promisedend.neoforge;

import com.panda.promisedend.platform.EndPlatform;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.fml.ModList;

/** {@link EndPlatform} on NeoForge. */
public final class NeoForgePlatform implements EndPlatform {
    @Override
    public String loaderName() {
        return "neoforge";
    }

    @Override
    public String modName(String namespace) {
        return ModList.get().getModContainerById(namespace).map(c -> c.getModInfo().getDisplayName()).orElse(namespace);
    }

    @Override
    public CreativeModeTab.Builder tabBuilder() {
        return CreativeModeTab.builder();
    }

    @Override
    public boolean isFakePlayer(net.minecraft.world.entity.player.Player player) {
        return player.isFakePlayer();
    }
}
