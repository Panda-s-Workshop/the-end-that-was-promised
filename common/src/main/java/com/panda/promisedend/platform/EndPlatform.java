package com.panda.promisedend.platform;

/** What common code needs from the mod loader. Each loader module provides one, found with {@link Services}. */
public interface EndPlatform {
    /** "neoforge" or "fabric". */
    String loaderName();

    /** The display name of the mod that owns a namespace, or the namespace itself. */
    String modName(String namespace);

    /** A builder for a creative tab of the mod's own, which the loader places among its pages. */
    net.minecraft.world.item.CreativeModeTab.Builder tabBuilder();
}
