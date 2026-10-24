package com.panda.promisedend.fabric;

import com.panda.promisedend.platform.EndPlatform;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;

/** {@link EndPlatform} on Fabric. */
public final class FabricPlatform implements EndPlatform {
    @Override
    public String loaderName() {
        return "fabric";
    }

    @Override
    public String modName(String namespace) {
        return FabricLoader.getInstance().getModContainer(namespace).map(c -> c.getMetadata().getName()).orElse(namespace);
    }

    @Override
    public CreativeModeTab.Builder tabBuilder() {
        return FabricCreativeModeTab.builder();
    }
}
