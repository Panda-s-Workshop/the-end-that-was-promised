# The End That Was Promised

[![Build](https://github.com/Panda-s-Workshop/the-end-that-was-promised/actions/workflows/build.yml/badge.svg)](https://github.com/Panda-s-Workshop/the-end-that-was-promised/actions/workflows/build.yml)

A Minecraft 26.3 mod for NeoForge (26.3.0.97) and Fabric (Fabric API 0.165.0), Java 25, with GeckoLib 5.5.7 on both.
**In development.** The store page link will go here at release.

The dragon was only the gatekeeper. Beyond it lie the ruins of the **Ancients**, who built the End Cities and left the
dimension half finished. Cross a living outer End of eight new biomes, wake their constructs, defeat three guardians,
and reach the edge where the world was never completed.

## Build and run

JDK 25 is pinned in `gradle.properties` (`org.gradle.java.home`). CI overrides it.

```bash
./gradlew build
```

```bash
./gradlew :neoforge:runGameTestServer
```

- `build` writes `neoforge/build/libs/promisedend-<version>-neoforge.jar` and
  `fabric/build/libs/promisedend-<version>-fabric.jar`. Both need GeckoLib; the Fabric jar also needs Fabric API.
- `./gradlew :<loader>:runServer -PendServerCheck` starts a dedicated server that generates the End round x=300 and
  logs `[promisedend servercheck] PASS` or `FAIL` for the Shattered Rim there.
- The dev servers listen on port 25612.

License: All Rights Reserved.
