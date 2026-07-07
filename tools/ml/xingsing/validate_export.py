#!/usr/bin/env python3
"""Check exported Java policy JSON against the NumPy checkpoint."""

from __future__ import annotations

import argparse
import base64
import json
from pathlib import Path

from features import OBS_DIM


def decode(values: str, shape: tuple[int, ...], np):
    data = base64.b64decode(values)
    return np.frombuffer(data, dtype="<f4").reshape(shape).astype(np.float32)


def json_forward(path: Path, obs, np):
    policy = json.loads(path.read_text(encoding="utf-8"))
    x = obs
    for layer in policy["layers"]:
        if layer["type"] == "linear":
            w = decode(layer["weights"], (layer["out"], layer["in"]), np)
            b = decode(layer["bias"], (layer["out"],), np)
            x = x @ w.T + b
        elif layer["type"] == "relu":
            x = np.maximum(x, 0.0)
        else:
            raise ValueError(layer["type"])
    return x


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", required=True)
    parser.add_argument("--policy-json", required=True)
    parser.add_argument("--samples", type=int, default=16)
    args = parser.parse_args()

    import numpy as np
    from model import TinyMlp

    model = TinyMlp.load(Path(args.model))
    rng = np.random.default_rng(19)
    obs = rng.random((args.samples, OBS_DIM), dtype=np.float32)
    logits_np, _ = model.forward(obs)
    logits_json = json_forward(Path(args.policy_json), obs, np)
    max_abs = float(np.max(np.abs(logits_np - logits_json)))
    print(f"max_abs_diff={max_abs:.8f}")
    if max_abs > 1.0e-5:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
