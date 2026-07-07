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
- D0 distilled teacher policy exported at `data/wildterrain/policies/xingsing_policy_v1.json`

Not enabled by default:

- Raw log collection. Training logs are local-only and disabled unless explicitly enabled.
- ONNX/GRU runtime. Keep this optional until packaging is proven across client and dedicated server.

Enabled by default:

- Model inference runs in `aiMode=model` with `allowModelInference=true`.
- The model is still bounded by runtime action masks and `XingsingActionAdapter`.
- If the policy resource is absent, invalid, or rejects the observation spec, Xingsing falls back to the teacher.

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
/wt_ai xingsing mode teacher
/wt_ai xingsing mode model
/wt_ai xingsing record start
/wt_ai xingsing record status
/wt_ai xingsing record stop
/wt_ai xingsing label <option>
/wt_ai xingsing debug on|off
```

Logs are written to:

```text
<active Minecraft gameDir>/wildterrain-ai/logs/xingsing/YYYY-MM-DD/session-<timestamp>.jsonl
```

In `./gradlew runClient`, `<active Minecraft gameDir>` is usually `run/`.
In the normal Launcher profile, it is usually the `.minecraft` directory.

Do not commit raw logs, replay files, or generated training checkpoints.

## Manual Real-MC Collection Pass

Use this when collecting real data without Computer Use:

```text
/wt_ai xingsing mode teacher
/wt_ai xingsing record start
/wt_ai xingsing record status
/wt_ai xingsing debug on
/summon wildterrain:xingsing ~2 ~ ~2
```

Then run a few short clips:

- Stand still near a fresh Xingsing; it should settle, look, or approach instead of immediately fleeing.
- Jump, sneak, and sprint near it.
- Use `/wt_ai xingsing scenario fetch_item`, then watch the fetch/return loop.
- Use `/wt_ai xingsing scenario hostile_warning`, then watch warning and evasive behavior.
- Feed sweet berries, glow berries, cocoa beans, or melon slices after a fear/trust test.
- If behavior is wrong, run `/wt_ai xingsing label <EXPECTED_OPTION>` close to the mistake.
- Finish with `/wt_ai xingsing record stop`.

For the first real dataset, prefer collecting broad teacher-supervised behavior
with `/wt_ai xingsing mode teacher`. The training loader uses `teacher_action` by
default, while also logging `policy_action` so later agents can compare model drift.

## Training Flow

```bash
PYTHONDONTWRITEBYTECODE=1 python3 tools/ml/xingsing/distill_teacher_policy.py \
  --out src/main/resources/data/wildterrain/policies/xingsing_policy_v1.json

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
  --out src/main/resources/data/wildterrain/policies/xingsing_policy_v1.json

python tools/ml/xingsing/validate_export.py \
  --model artifacts/policies/xingsing_bc_v1/model.npz \
  --policy-json src/main/resources/data/wildterrain/policies/xingsing_policy_v1.json
```

The committed D0 policy was distilled from 60k synthetic teacher states and reached
99.23% held-out teacher-match accuracy. Before using real playtest behavior as the
source of truth, replace or fine-tune this policy with recorded JSONL data.

Release gate before model inference can be treated as production-learned behavior:

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
