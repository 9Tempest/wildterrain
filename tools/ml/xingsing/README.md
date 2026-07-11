# Xingsing Option-Policy Training

This folder trains and exports a small option-level policy for `wildterrain:xingsing`.
Minecraft never imports Python; these tools only read local JSONL logs written under
`<gameDir>/wildterrain-ai/logs/xingsing` when recording is enabled. For
`./gradlew runClient` the game directory is usually `run/`; for the normal Launcher
profile it is usually `.minecraft`.

Quick flow:

Bootstrap a first model from synthetic teacher data with no third-party Python
dependencies:

```bash
PYTHONDONTWRITEBYTECODE=1 python3 tools/ml/xingsing/distill_teacher_policy.py \
  --out src/main/resources/data/wildterrain/policies/xingsing_policy_v1.json
```

The original D0 policy was distilled from 60k synthetic teacher states and reached
99.23% held-out teacher-match accuracy. The current committed policy is
`xingsing_real_bc_20260710_v1`, behavior-cloned from 1,200 real Minecraft
decision records collected in the normal Launcher profile.

Collect real JSONL playtest logs inside Minecraft:

```text
/wt_ai xingsing mode teacher
/wt_ai xingsing record start
/wt_ai xingsing record status
/wt_ai xingsing debug on
/summon wildterrain:xingsing ~2 ~ ~2
/wt_ai xingsing scenario fetch_item
/wt_ai xingsing scenario hostile_warning
/wt_ai xingsing label OBSERVE_PLAYER
/wt_ai xingsing record stop
```

For scalable collection, stand in a safe test world and let the server script
episodes around you:

```text
/wt_ai xingsing collect start
/wt_ai xingsing collect status
/wt_ai xingsing collect stop
```

The default command runs coverage episodes in teacher mode. Target one scenario
with optional episode count, ticks, and mode:

```text
/wt_ai xingsing collect start fetch_item 40 240 teacher
/wt_ai xingsing collect start hostile_warning 20 220 model
```

Automated runs write v2 transition logs under:

```text
<gameDir>/wildterrain-ai/runs/xingsing/<run-id>/
```

Each run has `manifest.json` plus `episodes/*.jsonl`. The current dataset loader
can train from either manual v1 logs or automated v2 runs because both include
`obs`, `action_mask`, and `teacher_action`.

Behavior-clone real JSONL playtest logs once NumPy is installed:

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
  --out src/main/resources/data/wildterrain/policies/xingsing_policy_v1.json \
  --policy-version xingsing_real_bc_YYYYMMDD_v1

python tools/ml/xingsing/validate_export.py \
  --model artifacts/policies/xingsing_bc_v1/model.npz \
  --policy-json src/main/resources/data/wildterrain/policies/xingsing_policy_v1.json
```

Logs are privacy-safe by design: no usernames, no chat, and no raw world seed.
Do not commit raw logs or generated checkpoints.

Training notes for future agents:

- `train_bc.py` uses balanced class sampling by default so small real datasets
  do not collapse into only `IDLE_GROOM` and `OBSERVE_PLAYER`.
- `dataset.py` merges `/wt_ai xingsing label <option>` human corrections into
  the nearest recent decision sample, within `correction_window_ticks`.
- Missing classes are still missing data, not a modeling success. Collect more
  scenario logs before expecting `WARN_HOSTILE`, `FLEE_TO_TREE`, `PLAY_CHASE`,
  or other uncovered options to generalize.
- Prefer `/wt_ai xingsing collect start coverage ...` for D2 datasets; keep
  manual `/record` sessions for spot checks and human corrections.
