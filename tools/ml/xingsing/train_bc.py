#!/usr/bin/env python3
"""Train a small behavior-cloning MLP from Xingsing JSONL logs."""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from dataset import class_counts, describe, load_examples
from features import OBS_DIM, OPTIONS


def read_config(path: Path) -> dict[str, float | int]:
    values: dict[str, float | int] = {
        "hidden_size": 128,
        "epochs": 12,
        "batch_size": 128,
        "learning_rate": 0.003,
        "class_weight_power": 0.25,
        "balanced_class_sampling": 1,
        "correction_window_ticks": 80,
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
    examples = load_examples(args.data, correction_window_ticks=int(config["correction_window_ticks"]))
    if not examples:
        raise SystemExit("No Xingsing examples found. Enable /wt_ai xingsing record start and collect JSONL logs first.")

    rng = np.random.default_rng(int(config["seed"]))
    obs = np.asarray([example.obs for example in examples], dtype=np.float32)
    mask = np.asarray([example.mask for example in examples], dtype=bool)
    targets = np.asarray([example.action for example in examples], dtype=np.int64)
    model = TinyMlp.init(hidden=int(config["hidden_size"]), seed=int(config["seed"]))

    counts = np.bincount(targets, minlength=len(OPTIONS)).astype(np.float32)
    class_weights = np.ones(len(OPTIONS), dtype=np.float32)
    class_weight_power = float(config["class_weight_power"])
    present = counts > 0.0
    if class_weight_power > 0.0 and present.any():
        class_weights[present] = (counts[present].sum() / counts[present]) ** class_weight_power
        class_weights[present] = class_weights[present] / class_weights[present].mean()
        class_weights[~present] = 0.0
    indices_by_class = [np.flatnonzero(targets == action) for action in range(len(OPTIONS))]
    present_class_indices = [indices for indices in indices_by_class if len(indices) > 0]
    samples_per_class = max((len(indices) for indices in present_class_indices), default=len(examples))

    def epoch_order() -> np.ndarray:
        if int(config["balanced_class_sampling"]) <= 0 or not present_class_indices:
            return rng.permutation(len(examples))
        sampled = [
            rng.choice(indices, size=samples_per_class, replace=len(indices) < samples_per_class)
            for indices in present_class_indices
        ]
        return rng.permutation(np.concatenate(sampled))

    batch_size = int(config["batch_size"])
    lr = float(config["learning_rate"])
    for epoch in range(1, int(config["epochs"]) + 1):
        order = epoch_order()
        total_loss = 0.0
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

        predictions = model.predict(obs, mask)
        accuracy = float((predictions == targets).mean())
        print(f"epoch {epoch:02d} loss={total_loss / max(len(order), 1):.4f} acc={accuracy:.3f}")

    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    model.save(out / "model.npz")
    (out / "metadata.json").write_text(json.dumps({
        "source": "jsonl_behavior_cloning",
        "obs_dim": OBS_DIM,
        "num_actions": len(OPTIONS),
        "examples": len(examples),
        "action_counts": class_counts(examples),
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
