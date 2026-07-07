#!/usr/bin/env python3
"""Train a small behavior-cloning MLP from Xingsing JSONL logs."""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from dataset import describe, load_examples
from features import OBS_DIM, OPTIONS


def read_config(path: Path) -> dict[str, float | int]:
    values: dict[str, float | int] = {
        "hidden_size": 128,
        "epochs": 12,
        "batch_size": 128,
        "learning_rate": 0.003,
        "seed": 7,
    }
    if not path.exists():
        return values
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.split("#", 1)[0].strip()
        if not line or ":" not in line:
            continue
        key, raw_value = [part.strip() for part in line.split(":", 1)]
        if key in values:
            values[key] = float(raw_value) if "." in raw_value else int(raw_value)
    return values


def train(args: argparse.Namespace) -> None:
    import numpy as np
    from model import TinyMlp, masked_softmax

    config = read_config(Path(args.config))
    examples = load_examples(args.data)
    if not examples:
        raise SystemExit("No Xingsing examples found. Enable /wt_ai xingsing record start and collect JSONL logs first.")

    rng = np.random.default_rng(int(config["seed"]))
    obs = np.asarray([example.obs for example in examples], dtype=np.float32)
    mask = np.asarray([example.mask for example in examples], dtype=bool)
    targets = np.asarray([example.action for example in examples], dtype=np.int64)
    model = TinyMlp.init(hidden=int(config["hidden_size"]), seed=int(config["seed"]))

    counts = np.bincount(targets, minlength=len(OPTIONS)).astype(np.float32)
    class_weights = counts.sum() / np.maximum(counts, 1.0)
    class_weights = class_weights / class_weights.mean()

    batch_size = int(config["batch_size"])
    lr = float(config["learning_rate"])
    for epoch in range(1, int(config["epochs"]) + 1):
        order = rng.permutation(len(examples))
        total_loss = 0.0
        correct = 0
        for start in range(0, len(order), batch_size):
            index = order[start : start + batch_size]
            x = obs[index]
            m = mask[index]
            y = targets[index]
            logits, (h0, h1) = model.forward(x)
            probs = masked_softmax(logits, m)
            weights = class_weights[y].reshape(-1, 1)
            loss = -np.log(np.maximum(probs[np.arange(len(y)), y], 1.0e-8)) * class_weights[y]
            total_loss += float(loss.sum())
            correct += int((probs.argmax(axis=1) == y).sum())

            grad_logits = probs
            grad_logits[np.arange(len(y)), y] -= 1.0
            grad_logits *= weights / len(y)
            grad_w2 = grad_logits.T @ h1
            grad_b2 = grad_logits.sum(axis=0)
            grad_h1 = grad_logits @ model.w2
            grad_z1 = grad_h1 * (h1 > 0.0)
            grad_w1 = grad_z1.T @ h0
            grad_b1 = grad_z1.sum(axis=0)
            grad_h0 = grad_z1 @ model.w1
            grad_z0 = grad_h0 * (h0 > 0.0)
            grad_w0 = grad_z0.T @ x
            grad_b0 = grad_z0.sum(axis=0)

            model.w2 -= lr * grad_w2.astype(np.float32)
            model.b2 -= lr * grad_b2.astype(np.float32)
            model.w1 -= lr * grad_w1.astype(np.float32)
            model.b1 -= lr * grad_b1.astype(np.float32)
            model.w0 -= lr * grad_w0.astype(np.float32)
            model.b0 -= lr * grad_b0.astype(np.float32)

        print(f"epoch {epoch:02d} loss={total_loss / len(examples):.4f} acc={correct / len(examples):.3f}")

    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    model.save(out / "model.npz")
    (out / "metadata.json").write_text(json.dumps({
        "obs_dim": OBS_DIM,
        "num_actions": len(OPTIONS),
        "examples": len(examples),
        "options": OPTIONS,
        "config": config,
    }, indent=2), encoding="utf-8")
    (out / "dataset_summary.txt").write_text(describe(examples), encoding="utf-8")
    print(f"saved {out / 'model.npz'}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--config", default="tools/ml/xingsing/configs/xingsing_bc_v1.yaml")
    parser.add_argument("--data", nargs="+", required=True)
    parser.add_argument("--out", required=True)
    train(parser.parse_args())


if __name__ == "__main__":
    main()
