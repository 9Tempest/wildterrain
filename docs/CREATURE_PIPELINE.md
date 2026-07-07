# Creature Pipeline

This is the repeatable workflow for adding or polishing a Wild Terrain creature.
It is designed for AI agents and human modders working together.

## 1. Design Brief

Start in `docs/CREATURE_DESIGNS.md`.

Every creature needs:

- Habitat and spawn logic.
- One player-facing interaction.
- One ecological interaction with blocks, mobs, weather, light, or ruins.
- A reason not to kill it.
- English and `zh_cn` localization.
- A concrete real-client test plan.

## 2. Asset Plan

For early playable slices, deterministic pixel assets are acceptable when they are:

- Generated from a committed script.
- Small enough to review.
- Clearly marked as first-party placeholder/pipeline art.
- Wired through the real renderer, item model, and UI paths.

Current example:

```bash
python3 tools/generate_mossquill_assets.py
python3 tools/generate_xingsing_assets.py
```

Outputs:

- `src/main/resources/assets/wildterrain/textures/entity/mossquill.png`
- `src/main/resources/assets/wildterrain/textures/item/mossquill_field_guide.png`
- `src/main/resources/assets/wildterrain/textures/entity/xingsing.png`
- `src/main/resources/assets/wildterrain/textures/item/xingsing_field_guide.png`

For production art, replace or supplement this stage with Blockbench and Aseprite:

- Model source: keep `.bbmodel` under `art/source/<creature>/`.
- Export target: Java model for simple vanilla-style animation, or GeckoLib JSON only after adding the dependency intentionally.
- Texture source: keep layered or palette notes under `art/source/<creature>/`.
- Runtime textures: keep final PNGs under `src/main/resources/assets/wildterrain/textures`.

Do not copy assets, names, or exact mechanics from inspiration mods.

## 3. Implementation

Use the Mossquill slice as the reference pattern:

- Entity logic: `common/entity/<Creature>.java`
- Optional item/UI entry: `common/item` and `client/screen`
- Registration: `core/ModEntities.java`, `core/ModItems.java`, `core/ModCreativeTabs.java`
- Client model/renderer: `client/model`, `client/renderer`
- Runtime assets: `src/main/resources/assets/wildterrain`
- Data-driven spawn/loot: `src/main/resources/data/wildterrain`

Keep client-only classes out of server/common execution paths. If a common item opens a client screen, isolate the client call behind a client-only nested access class or another Dist-safe pattern.

## 4. Animation Pass

Add animation states before adding many behaviors.

For simple creatures, Forge/Minecraft Java models are enough:

- Define synced animation state on the entity with `SynchedEntityData`.
- Trigger short states from real gameplay events.
- In the model, animate independent parts instead of rotating the whole body only.
- Include idle motion, movement motion, and at least one interaction tell.

Current Mossquill states:

- `ANIMATION_GRAZE`: triggered when it mosses old stone.
- `ANIMATION_DELIGHT`: triggered when fed glow berries.
- `ANIMATION_SNIFF`: random idle behavior.

Consider GeckoLib only when the creature needs authored keyframe timelines, complex layered animation, or reusable animation controllers.

## 5. UI Pass

Each creature should eventually have a lightweight player-facing way to understand its ecology.

For early slices:

- Add a field guide item or shared bestiary screen.
- Keep the UI concise: habitat, behavior, ecology, player value, animation tells.
- Localize all visible text in `en_us.json` and `zh_cn.json`.
- Put the entry item in the Wild Terrain creative tab for fast testing.

Current example:

- Item: `wildterrain:mossquill_field_guide`
- Screen: `MossquillGuideScreen`

## 6. AI And Policy Pass

Use option-level policies for creatures with social or learned behavior. Do not train
raw movement controls when vanilla navigation can safely execute a high-level option.

Current example:

- Entity: `wildterrain:xingsing`
- Options: `XingsingOption`
- Observation: `XingsingObservation` with spec version and fixed vector size
- Teacher: `XingsingRuleTeacher`
- Safety layer: `XingsingActionMaskBuilder` and `XingsingActionAdapter`
- Local logs: `run/wildterrain-ai/logs/xingsing`
- Offline tools: `tools/ml/xingsing`

Rules:

- Keep logging disabled by default and local-only.
- Hash player UUIDs and never log usernames, chat, or raw world seeds.
- Keep masks active for teacher and model inference.
- Keep model inference behind config until real scenario gates pass.
- Fall back to teacher behavior when a policy resource is absent, invalid, or disabled.

## 7. Verification

Before calling a creature slice complete:

```bash
GRADLE_USER_HOME="$PWD/.gradle-home" ./gradlew build
./gradlew runClient
```

In the dev client:

- Open the Wild Terrain creative tab.
- Confirm the field guide item and spawn egg render without missing textures.
- Right-click the guide item and check the custom UI.
- Spawn the creature with its egg and with `/summon wildterrain:<id>`.
- Feed or trigger its interaction and watch the animation state.
- Check logs for missing texture, missing lang, or registry errors.
- For Xingsing, run `/wt_ai xingsing scenario fetch_item` and verify no item loss or duplication.
