# Xingsing Option-Policy Training

This folder trains and exports a small option-level policy for `wildterrain:xingsing`.
Minecraft never imports Python; these tools only read local JSONL logs written under
`run/wildterrain-ai/logs/xingsing` when recording is enabled.

Quick flow:

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

Logs are privacy-safe by design: no usernames, no chat, and no raw world seed.
Do not commit raw logs or generated checkpoints.
