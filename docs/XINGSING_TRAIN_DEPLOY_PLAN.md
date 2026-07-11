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
- Real-log behavior-cloned student policy exported at `data/wildterrain/policies/xingsing_policy_v1.json`
- D0 synthetic teacher-distilled policy retained as a reproducible bootstrap path

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
/wt_ai xingsing collect start
/wt_ai xingsing collect start <scenario> <episodes> <ticks> <teacher|model>
/wt_ai xingsing collect status
/wt_ai xingsing collect stop
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

## Automated Real-MC Collection

Manual recording is useful for taste checks, but it does not scale. The D2 path
is server-side collection driven by Minecraft ticks:

```text
/wt_ai xingsing collect start
/wt_ai xingsing collect start coverage 120 240 teacher
/wt_ai xingsing collect start fetch_item 40 240 teacher
/wt_ai xingsing collect start hostile_warning 30 220 model
/wt_ai xingsing collect status
/wt_ai xingsing collect stop
```

Current scenarios:

- `coverage`
- `settle_near_player`
- `mimic_jump`
- `mimic_sneak`
- `mimic_sprint`
- `fetch_item`
- `return_item`
- `hostile_warning`
- `flee_to_tree`
- `climb_to_perch`
- `play_chase`

The collector requires a player to start the run in-world, then scripts stimuli
server-side. It spawns and cleans up Xingsing, items, hostiles, and temporary
canopy blocks per episode. This avoids Computer Use and captures real Forge AI,
navigation, entity, item, and world-state ticks. A future dedicated-server bot
or Forge `FakePlayer` bridge should replace the human anchor for fully headless
RL farms.

Automated logs are written to:

```text
<active Minecraft gameDir>/wildterrain-ai/runs/xingsing/<run-id>/
```

Each run has:

- `manifest.json`: scenario, mode, episode count, tick budget, vector spec, and
  privacy-safe player hash.
- `episodes/*.jsonl`: schema v2 transition records.

Each transition includes `run_id`, `episode_id`, `scenario_id`, `seed`, `step`,
`episode_tick`, `obs`, `action_mask`, `teacher_action`, `policy_action`,
`reward`, `done`, `next_obs`, `next_action_mask`, and compact metadata. The
existing Python loader accepts these files for behavior cloning because
`teacher_action` remains the default supervised label. RL code should use
`policy_action`, `reward`, `done`, `next_obs`, and the masks.

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
  --out src/main/resources/data/wildterrain/policies/xingsing_policy_v1.json \
  --policy-version xingsing_real_bc_YYYYMMDD_v1

python tools/ml/xingsing/validate_export.py \
  --model artifacts/policies/xingsing_bc_v1/model.npz \
  --policy-json src/main/resources/data/wildterrain/policies/xingsing_policy_v1.json
```

The original D0 policy was distilled from 60k synthetic teacher states and reached
99.23% held-out teacher-match accuracy. The current student policy,
`xingsing_real_bc_20260710_v1`, was trained from 1,200 real Minecraft decision
records and reached 90.2% teacher-match accuracy on that local training set.

The 2026-07-10 dataset covered `IDLE_GROOM`, `OBSERVE_PLAYER`, mimic
jump/sneak/sprint, item pickup/return, and `CLIMB_TO_PERCH`. It did not cover
`APPROACH_PLAYER`, `KEEP_PLAY_DISTANCE`, `PLAY_CHASE`, `WARN_HOSTILE`,
`FLEE_TO_TREE`, or `LEAD_TO_FRUIT`, so collect those scenarios before treating
the policy as broad.

`train_bc.py` uses balanced class sampling by default. `dataset.py` also merges
human correction records from `/wt_ai xingsing label <EXPECTED_OPTION>` into the
nearest recent decision sample within `correction_window_ticks`.

Release gate before model inference can be treated as production-learned behavior:

- Item loss rate is zero in curated fetch/return tests.
- Item duplication rate is zero.
- Wrong-owner returns stay below 1%.
- False warning rate stays low in safe scenarios.
- Missing or rejected model resources fall back to teacher behavior.
- Dedicated server launch works with no client-class leak.

## Next Milestones

- Add a dedicated-server bot/FakePlayer bridge so automated collection can run
  without a human player anchoring the scenario.
- Add a Python Gym-style wrapper over the v2 transition schema and an online
  command bridge for RL resets/steps.
- Add a debug overlay or compact in-world debug readout.
- Add curated replay fixtures for fetch, warning, and imitation.
- Add GameTests for carried-item death drop and no-duplication behavior.
- Collect model-mode rollouts plus human corrections for missing option classes.
- Consider GRU/ONNX only after the MLP path has real data and scenario metrics.
