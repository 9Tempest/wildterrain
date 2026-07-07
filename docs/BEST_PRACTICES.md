# Best Practices

## Forge 1.20.1

- Use `DeferredRegister` and `RegistryObject` for blocks, items, entities, tabs, and future registries.
- Register attributes in `EntityAttributeCreationEvent`.
- Register spawn placement in `SpawnPlacementRegisterEvent`.
- Keep client-only renderer/model code in a client package and subscribe with `value = Dist.CLIENT`.
- Use Java 17 bytecode. The Gradle toolchain handles this even if the local launcher JDK is newer.

## Data-Driven Content

- Prefer biome modifier JSON for spawn additions.
- Prefer tags over hard-coded biome lists when the concept maps to a biome family.
- Keep loot tables in `data/wildterrain/loot_tables`.
- Keep language keys in both `en_us.json` and `zh_cn.json`.
- Add generated resources only when the generator is part of the workflow; otherwise keep JSON human-authored.

## Creature Behavior

- Start with vanilla goals, then add one custom behavior at a time.
- Respect `ForgeEventFactory.getMobGriefingEvent` before changing blocks.
- Avoid frequent block scans in `aiStep`; throttle with `tickCount` and randomness.
- Use particles/sounds to telegraph ecological effects.
- Prefer non-combat value so new creatures are not just loot containers.

## Worldgen And Ruins

- Build ruins as data packs first: configured structures, template pools, processors, and biome tags.
- Let creatures point players toward ruins through behavior instead of relying only on map items.
- Keep rare destinations rare, but add discoverability hooks.
- Never make irreversible terrain edits without checking game rules or player intent.

## Verification Checklist

- `./gradlew build`
- JSON validation for resources.
- `./gradlew runClient`
- Creative inventory contains expected spawn eggs/items.
- Entity spawns with `/summon wildterrain:<id>`.
- Entity can be spawned with its egg.
- Integrated server handshake includes `wildterrain`.
- No missing texture or missing lang key is visible during basic play.
