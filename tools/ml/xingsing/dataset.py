"""Load Xingsing JSONL decision logs."""

from __future__ import annotations

import json
from collections import Counter
from pathlib import Path
from typing import Iterable

from features import DecisionExample, normalize_mask, normalize_obs, option_id, OPTIONS


def iter_jsonl_files(paths: Iterable[Path]) -> Iterable[Path]:
    for path in paths:
        if path.is_dir():
            yield from sorted(path.rglob("*.jsonl"))
        elif path.suffix == ".jsonl":
            yield path


def load_examples(paths: Iterable[str | Path], action_field: str = "teacher_action") -> list[DecisionExample]:
    examples: list[DecisionExample] = []
    for file_path in iter_jsonl_files(Path(path) for path in paths):
        with file_path.open("r", encoding="utf-8") as handle:
            for line_number, line in enumerate(handle, 1):
                line = line.strip()
                if not line:
                    continue
                record = json.loads(line)
                if record.get("type") == "human_correction":
                    continue
                if "obs" not in record or action_field not in record:
                    continue
                obs = normalize_obs([float(value) for value in record["obs"]])
                mask = normalize_mask([bool(value) for value in record.get("action_mask", [])])
                action = option_id(str(record[action_field]))
                if not mask[action]:
                    mask[action] = True
                examples.append(DecisionExample(obs=obs, mask=mask, action=action,
                                                source=f"{file_path}:{line_number}"))
    return examples


def class_counts(examples: Iterable[DecisionExample]) -> Counter[str]:
    counts: Counter[str] = Counter()
    for example in examples:
        counts[OPTIONS[example.action]] += 1
    return counts


def describe(examples: list[DecisionExample]) -> str:
    counts = class_counts(examples)
    lines = [f"examples: {len(examples)}"]
    for option in OPTIONS:
        lines.append(f"{option}: {counts.get(option, 0)}")
    return "\n".join(lines)
