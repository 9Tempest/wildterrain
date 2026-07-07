# Creature Designs

Wild Terrain creatures should feel like small ecological systems, not isolated collectibles. Every creature needs a reason to exist in terrain, ecology, or ruins.

## Implemented

### Mossquill / 苔针兽

- Status: playable
- Files: `Mossquill.java`, `MossquillModel.java`, `MossquillRenderer.java`, `MossquillGuideScreen.java`, `mossquill.png`
- Habitat: lush caves, swamps, mangrove swamps, jungles
- Role: ruin-grazer and gentle restoration creature
- Food: moss block, moss carpet
- Tempt item: glow berries
- Player interaction: feeding glow berries grants short regeneration and haste
- Ecology loop: slowly turns cobblestone, stone bricks, and cracked stone bricks into mossy variants when mob griefing is enabled
- Field guide: `wildterrain:mossquill_field_guide`
- Animation states: grazing/mossing, glow-berry delight, random sniffing, idle breathing, walking, ear flicks, tail motion
- Visual attachment: `src/main/resources/assets/wildterrain/textures/entity/mossquill.png`

Design notes:

Mossquill is the proof creature. It validates registries, attributes, spawn rules, biome modifier data, creative tab integration, renderer/model registration, loot tables, localization, player-facing field-guide UI, gameplay-triggered animation state, reproducible pixel assets, and real-client launch testing. Future creatures should use it as the simplest reference, then exceed it in behavior richness.

## Designed Next

### Lanternback Nacrelisk / 灯背珠蜥

- Habitat: dark aquifers, abyssal cave pools, flooded ruins
- Shape: low amphibian body, nacre shell, glowing dorsal lanterns
- Temperament: passive unless attacked
- Gameplay: lights a small underwater area and reveals hidden ruin glyph blocks when fed prismarine shards
- Ecology: attracts fish, avoids guardians, startles drowned at low health
- Drops: nacre scale, low chance glow ink sac
- Implementation notes: needs amphibious pathing, custom light-emitting render layer, and a structure/poi search helper

### Silt Skater / 泥纹掠行者

- Habitat: mangrove swamps, mud flats, river deltas
- Shape: wide-legged insectile skimmer
- Temperament: skittish neutral
- Gameplay: leaves temporary firm silt wake over mud and shallow water, letting players cross dangerous swamp patches
- Ecology: feeds near mangrove roots, flees frogs, gathers in rain
- Drops: silt filament for future traversal gear
- Implementation notes: start with ground movement plus block-state replacement cooldowns; avoid permanent terrain grief unless game rule allows it

### Basalt Bellwether / 玄武鸣羊

- Habitat: basalt deltas, volcanic caves, future volcanic terrain
- Shape: compact herd animal with hollow basalt horns
- Temperament: defensive herd creature
- Gameplay: horn calls reveal ore resonance particles and unstable basalt bridges
- Ecology: groups around lava-safe ledges, headbutts magma cubes, panics at ghasts
- Drops: cracked horn shard
- Implementation notes: needs herd memory, sound event registration, and particle feedback before adding combat complexity

### Archive Wisp / 遗卷灵火

- Habitat: underground cabins, libraries, ruins, archaeology sites
- Shape: floating paper-and-flame spirit
- Temperament: curious, non-combat trickster
- Gameplay: guides players to hidden rooms if shown candles, brushes, or written books
- Ecology: dims around sculk, brightens near bookshelves and chiseled blocks
- Drops: none by default; should reward exploration rather than killing
- Implementation notes: pair with ruin structure generation; likely needs custom particles and a simple memory trail system

### Canopy Kite / 林冠风筝兽

- Habitat: jungle canopy, tall birch forests, cliffs
- Shape: gliding mammal with leaf-like membranes
- Temperament: passive but easily startled
- Gameplay: seeds rare canopy plants and can temporarily trust players who feed it fruit
- Ecology: avoids open ground, perches under leaves, spreads saplings or hanging roots
- Drops: down tuft only when naturally shedding, not from combat
- Implementation notes: begin with perch/rest states and short glides; do not overpromise rideability early

## Design Bar

A creature is ready for implementation when it has:

- A habitat and spawn logic.
- A clear player-facing interaction.
- At least one ecological interaction with blocks, mobs, weather, light, or structures.
- A reason not to kill it.
- A localization plan.
- A test plan.
