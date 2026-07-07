# Progress

## 2026-07-07

Completed:

- Implemented `wildterrain:xingsing`, a friendly 狌狌 / Xingsing mimic companion with spawn egg, renderer, model, texture, field guide, loot table, biome modifier, and dual-language localization.
- Added trust/fear/mischief memory, fruit feeding, player-action mimicry, safe recent-drop fetch/return behavior, hostile warning, and flee/perch option behavior.
- Added Xingsing AI framework classes for observations, action masks, teacher selection, action adapter execution, player action memory, dropped item ownership, local JSONL logging, Java MLP loading, and masked action selection.
- Added `/wt_ai xingsing` scenario, record, debug, and label commands for real-client data collection.
- Added offline behavior-cloning tooling under `tools/ml/xingsing`, including dataset loading, NumPy MLP training, evaluation, Java-weight export, and export validation.
- Added `docs/XINGSING_TRAIN_DEPLOY_PLAN.md` for future AI agents and model rollout gates.
- Added a reproducible Mossquill asset generator at `tools/generate_mossquill_assets.py`.
- Regenerated the Mossquill entity texture as a 64x64 PNG and added a 16x16 Mossquill Field Guide item icon.
- Added `wildterrain:mossquill_field_guide`, creative tab integration, item model, tooltip, and dual-language localization.
- Added `MossquillGuideScreen`, a custom in-game field-guide UI for habitat, behavior, ecology, and animation tells.
- Expanded Mossquill with synced animation states for grazing/mossing, glow-berry delight, and random sniffing.
- Refined the Java model with separate ears, muzzle, moss blanket, tail tuft, paws, and animated quill rows.
- Documented the repeatable creature art/UI/animation workflow in `docs/CREATURE_PIPELINE.md`.

Known gaps:

- Xingsing has a teacher/playable policy, but no trained policy has been collected, trained, exported, or enabled yet.
- Xingsing needs curated no-duplication/no-loss GameTests around carried items.
- Mossquill art is improved pipeline art, but still not final Blockbench/Aseprite production art.
- UI is currently one creature entry; a shared bestiary index will be useful once there are multiple implemented creatures.
- Natural spawning still needs longer in-world validation.
- No dedicated automated GameTests yet.

## 2026-07-06

Completed:

- Created Forge 1.20.1 mod scaffold.
- Added Java 17 toolchain setup.
- Implemented `wildterrain:mossquill`.
- Added Mossquill model, renderer, texture, spawn egg, creative tab, loot table, biome spawn modifier, and localization.
- Added `installClientMod` task.
- Built the jar successfully with `./gradlew build`.
- Installed `wildterrain-0.1.0.jar` into the local Minecraft mods folder.
- Launched `./gradlew runClient`, loaded a local world, and confirmed the mod list includes `wildterrain`.
- Added README creature roster, agent handoff docs, best-practice docs, progress tracking, GitHub workflow, PR template, and issue templates.
- Committed local baseline as `11f8373`.
- Configured local `origin` as `https://github.com/9Tempest/wildterrain.git`.
- Created and pushed the public GitHub repository at `https://github.com/9Tempest/wildterrain`.

Known gaps:

- Mossquill art is a first-pass generated texture, not final Blockbench-quality art.
- Natural spawning needs longer in-world validation.
- No dedicated automated GameTests yet.
- Citadel is optional metadata only; there is no Citadel API usage yet.

Next:

- Add a simple GameTest or command-driven validation for entity registration.
- Implement Lanternback Nacrelisk or Silt Skater as the second creature.
- Add the first ruin structure and link it to creature behavior.
- Run the commands in `docs/GITHUB_SETUP.md` once GitHub authentication is refreshed.
