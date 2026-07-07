# Agent Handoff

This repository is meant to be friendly to future AI agents. Read this file before changing code.

## Project Identity

- Mod name: Wild Terrain
- Mod id: `wildterrain`
- Loader: Forge
- Minecraft: `1.20.1`
- Java bytecode/toolchain: Java 17
- Current playable creature: `Mossquill`
- GitHub owner target: `9Tempest`

Wild Terrain should learn from the design density of Alex's Mobs and Alex's Caves without copying code, assets, text, names, or exact mechanics. Prefer original ecology loops, data-driven placement, and small systems that interact with vanilla Minecraft.

## Build And Test

Use these from the repository root:

```bash
./gradlew build
./gradlew runClient
./gradlew runServer
./gradlew installClientMod
```

In restricted sandboxes, use a workspace-local Gradle home:

```bash
GRADLE_USER_HOME="$PWD/.gradle-home" ./gradlew build
```

Before claiming success, run at least `./gradlew build`. For gameplay changes, launch `./gradlew runClient`, create or load a world, and confirm the client/server mod list includes `wildterrain`.

## Architecture Map

- Main mod bootstrap: `src/main/java/dev/lukez/wildterrain/WildTerrain.java`
- Registries: `src/main/java/dev/lukez/wildterrain/core`
- Entity logic: `src/main/java/dev/lukez/wildterrain/common/entity`
- Client-only model/render code: `src/main/java/dev/lukez/wildterrain/client`
- Runtime assets: `src/main/resources/assets/wildterrain`
- World/data content: `src/main/resources/data/wildterrain`
- Design docs: `docs/`

## Add A Creature

1. Create the entity class under `common/entity`.
2. Register the `EntityType` in `ModEntities`.
3. Register attributes and spawn placement in `CommonModEvents`.
4. Add the spawn egg in `ModItems` and creative tab output in `ModCreativeTabs`.
5. Add client model/renderer under `client`.
6. Add lang keys, item model, texture, loot table, and biome modifier data.
7. Document the creature in `README.md`, `docs/CREATURE_DESIGNS.md`, and `docs/PROGRESS.md`.
8. Build and launch-test.

## Agent Rules

- Keep server/common code free of client-only imports.
- Put data-driven spawn/worldgen in JSON whenever practical.
- Add English and `zh_cn` localization together.
- Do not commit `build/`, `.gradle/`, `.gradle-home/`, or `run/`.
- Do not depend on Citadel unless a feature truly needs it; it is optional metadata for compatibility with the Alex's ecosystem.
- Prefer small, testable slices over giant feature drops.
- Update progress docs in the same commit as behavior changes.
