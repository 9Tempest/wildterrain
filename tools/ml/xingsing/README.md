# Xingsing Option-Policy Training

This folder trains and exports a small option-level policy for `wildterrain:xingsing`.
Minecraft never imports Python; these tools only read local JSONL logs written under
`run/wildterrain-ai/logs/xingsing` when recording is enabled.

Quick flow:

Bootstrap a first model from synthetic teacher data with no third-party Python
dependencies:

```bash
PYTHONDONTWRITEBYTECODE=1 python3 tools/ml/xingsing/distill_teacher_policy.py \
  --out src/main/resources/data/wildterrain/policies/xingsing_policy_v1.json
```

The current committed D0 policy was distilled from 60k synthetic teacher states
and reached 99.23% held-out teacher-match accuracy. It is a bootstrap model, not
a replacement for real playtest data.

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
  --out src/main/resources/data/wildterrain/policies/xingsing_policy_v1.json

python tools/ml/xingsing/validate_export.py \
  --model artifacts/policies/xingsing_bc_v1/model.npz \
  --policy-json src/main/resources/data/wildterrain/policies/xingsing_policy_v1.json
```

Logs are privacy-safe by design: no usernames, no chat, and no raw world seed.
Do not commit raw logs or generated checkpoints.
