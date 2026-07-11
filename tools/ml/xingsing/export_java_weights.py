#!/usr/bin/env python3
"""Export a NumPy Xingsing MLP to the Java JSON resource format."""

from __future__ import annotations

import argparse
import base64
import json
from pathlib import Path

from features import OBS_SPEC_VERSION, OPTIONS


def tensor(values) -> str:
    import numpy as np

    return base64.b64encode(np.asarray(values, dtype="<f4").tobytes()).decode("ascii")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--policy-version", default="xingsing_bc_v1")
    args = parser.parse_args()

    from model import TinyMlp

    model_path = Path(args.model)
    model = TinyMlp.load(model_path)
    hidden = int(model.b0.shape[0])
    policy = {
        "schema_version": 1,
        "entity": "wildterrain:xingsing",
        "policy_version": args.policy_version,
        "obs_spec_version": OBS_SPEC_VERSION,
        "num_actions": len(OPTIONS),
        "layers": [
            {"type": "linear", "in": int(model.w0.shape[1]), "out": hidden,
             "weights": tensor(model.w0), "bias": tensor(model.b0)},
            {"type": "relu"},
            {"type": "linear", "in": int(model.w1.shape[1]), "out": int(model.w1.shape[0]),
             "weights": tensor(model.w1), "bias": tensor(model.b1)},
            {"type": "relu"},
            {"type": "linear", "in": int(model.w2.shape[1]), "out": int(model.w2.shape[0]),
             "weights": tensor(model.w2), "bias": tensor(model.b2)},
        ],
    }
    metadata_path = model_path.parent / "metadata.json"
    if metadata_path.exists():
        metadata = json.loads(metadata_path.read_text(encoding="utf-8"))
        policy["training"] = {
            "source": metadata.get("source", "jsonl_behavior_cloning"),
            "examples": metadata.get("examples"),
            "action_counts": metadata.get("action_counts", {}),
            "config": metadata.get("config", {}),
            "options": metadata.get("options", OPTIONS),
        }
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(policy, separators=(",", ":")), encoding="utf-8")
    print(f"wrote {out}")


if __name__ == "__main__":
    main()
