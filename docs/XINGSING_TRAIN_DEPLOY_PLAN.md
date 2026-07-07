# Xingsing Train And Deploy Plan

`wildterrain:xingsing` is a friendly, fast, white-eared mimic companion inspired by
Shan Hai Jing descriptions of 狌狌. Its value comes from social behavior, not combat
or loot.

## Current Slice

Implemented now:

- Playable Forge entity: `common/entity/Xingsing.java`
- Spawn egg and field guide: `wildterrain:xingsing_spawn_egg`, `wildterrain:xingsing_field_guide`
- Habitat data: jungle tag, bamboo jungle, and conservative lush cave spawns
- Trust/fear/mischief memory with NBT persistence
- Teacher-compatible option loop with 14 high-level actions
- Observation vector spec v1 with 110 float features
- Action masks for unsafe or impossible options
- Safe fetch/return path for recently player-dropped items only
- Hostile warning, mimic jump/sneak/sprint, playful chase, flee/perch options
- JSONL decision logger behind config or `/wt_ai xingsing record start`
- Scenario/debug/label commands under `/wt_ai xingsing`
- Offline NumPy training/export tools under `tools/ml/xingsing`
- Java MLP loader and masked action selector for future model deployment

Not enabled by default:

- Model inference. `allowModelInference=false` and `aiMode=teacher` remain the safe defaults.
- Raw log collection. Training logs are local-only and disabled unless explicitly enabled.
- ONNX/GRU runtime. Keep this optional until packaging is proven across client and dedicated server.

## Gameplay Contract

Xingsing should feel curious, social, and a little mischievous:

- Watches nearby friendly players.
- May copy jumps, sneaks, and sprints.
- Fetches recently tossed player items and returns them to the right owner.
- Warns when hostile mobs approach.
- Fears players that attack it and recovers trust when fed fruit.
- Uses jungle geometry by fleeing or perching near leaves/logs where possible.

Food and trust items:

- Sweet berries
- Glow berries
- Cocoa beans
- Melon slices

It has an empty loot table. Do not add desirable player-kill drops unless the design changes.

## Option Policy

The policy chooses what to do every configurable decision interval, default 10 ticks.
Minecraft navigation and safety stay in `XingsingActionAdapter`.

| ID | Option |
| ---: | --- |
| 0 | `IDLE_GROOM` |
| 1 | `OBSERVE_PLAYER` |
| 2 | `APPROACH_PLAYER` |
| 3 | `KEEP_PLAY_DISTANCE` |
| 4 | `MIRROR_JUMP` |
| 5 | `MIRROR_SNEAK` |
| 6 | `MIRROR_SPRINT` |
| 7 | `PICKUP_ITEM` |
| 8 | `RETURN_ITEM` |
| 9 | `PLAY_CHASE` |
| 10 | `CLIMB_TO_PERCH` |
| 11 | `WARN_HOSTILE` |
| 12 | `FLEE_TO_TREE` |
| 13 | `LEAD_TO_FRUIT` |

Safety rule: masks are mandatory in teacher and model mode. Invalid logits must be treated as
effectively impossible before selection.

## Dev Commands

```text
/wt_ai xingsing scenario mimic_jump
/wt_ai xingsing scenario mimic_sneak
/wt_ai xingsing scenario fetch_item
/wt_ai xingsing scenario hostile_warning
/wt_ai xingsing scenario trust_recovery
/wt_ai xingsing scenario jungle_pathing
/wt_ai xingsing record start
/wt_ai xingsing record stop
/wt_ai xingsing label <option>
/wt_ai xingsing debug on|off
```

Logs are written to:

```text
run/wildterrain-ai/logs/xingsing/YYYY-MM-DD/session-<timestamp>.jsonl
```

Do not commit raw logs, replay files, or generated training checkpoints.

## Training Flow

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install numpy

python tools/ml/xingsing/train_bc.py \
  --config tools/ml/xingsing/configs/xingsing_bc_v1.yaml \
  --data run/wildterrain-ai/logs/xingsing \
  --out artifacts/policies/xingsing_bc_v1

python tools/ml/xingsing/evaluate_policy.py \
  --model artifacts/policies/xingsing_bc_v1/model.npz \
  --data run/wildterrain-ai/logs/xingsing

python tools/ml/xingsing/export_java_weights.py \
  --model artifacts/policies/xingsing_bc_v1/model.npz \
  --out src/main/resources/assets/wildterrain/policies/xingsing_policy_v1.json

python tools/ml/xingsing/validate_export.py \
  --model artifacts/policies/xingsing_bc_v1/model.npz \
  --policy-json src/main/resources/assets/wildterrain/policies/xingsing_policy_v1.json
```

Release gate before model inference becomes default:

- Item loss rate is zero in curated fetch/return tests.
- Item duplication rate is zero.
- Wrong-owner returns stay below 1%.
- False warning rate stays low in safe scenarios.
- Missing or rejected model resources fall back to teacher behavior.
- Dedicated server launch works with no client-class leak.

## Next Milestones

- Add a debug overlay or compact in-world debug readout.
- Add curated replay fixtures for fetch, warning, and imitation.
- Add GameTests for carried-item death drop and no-duplication behavior.
- Implement DAgger relabel merge once model rollouts produce failure states.
- Consider GRU/ONNX only after the MLP path has real data and scenario metrics.
