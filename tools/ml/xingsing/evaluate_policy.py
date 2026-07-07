#!/usr/bin/env python3
"""Evaluate a Xingsing MLP against JSONL decision logs."""

from __future__ import annotations

import argparse
from pathlib import Path

from dataset import load_examples
from features import OPTIONS


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", required=True)
    parser.add_argument("--data", nargs="+", required=True)
    args = parser.parse_args()

    import numpy as np
    from model import TinyMlp

    examples = load_examples(args.data)
    if not examples:
        raise SystemExit("No examples found.")
    model = TinyMlp.load(Path(args.model))
    obs = np.asarray([example.obs for example in examples], dtype=np.float32)
    mask = np.asarray([example.mask for example in examples], dtype=bool)
    targets = np.asarray([example.action for example in examples], dtype=np.int64)
    predictions = model.predict(obs, mask)
    accuracy = float((predictions == targets).mean())
    print(f"examples={len(examples)} accuracy={accuracy:.3f}")
    print("confusion rows=target cols=prediction")
    matrix = np.zeros((len(OPTIONS), len(OPTIONS)), dtype=np.int64)
    for target, prediction in zip(targets, predictions, strict=True):
        matrix[target, prediction] += 1
    for index, option in enumerate(OPTIONS):
        row = " ".join(str(value) for value in matrix[index])
        print(f"{option}: {row}")


if __name__ == "__main__":
    main()
