"""Load Xingsing JSONL decision logs."""

from __future__ import annotations

import json
from collections import Counter
from pathlib import Path
from typing import Iterable, TypedDict

from features import DecisionExample, normalize_mask, normalize_obs, option_id, OPTIONS


class _PendingExample(TypedDict):
    obs: list[float]
    mask: list[bool]
    action: int
    tick: int
    player_hash: str
    source: str


def iter_jsonl_files(paths: Iterable[Path]) -> Iterable[Path]:
    for path in paths:
        if path.is_dir():
            yield from sorted(path.rglob("*.jsonl"))
        elif path.suffix == ".jsonl":
            yield path


def load_examples(
    paths: Iterable[str | Path],
    action_field: str = "teacher_action",
    correction_window_ticks: int = 80,
) -> list[DecisionExample]:
    examples: list[DecisionExample] = []
    for file_path in iter_jsonl_files(Path(path) for path in paths):
        pending: list[_PendingExample] = []
        with file_path.open("r", encoding="utf-8") as handle:
            for line_number, line in enumerate(handle, 1):
                line = line.strip()
                if not line:
                    continue
                record = json.loads(line)
                if record.get("type") == "human_correction":
                    apply_human_correction(record, pending, file_path, line_number, correction_window_ticks)
                    continue
                if "obs" not in record or action_field not in record:
                    continue
                obs = normalize_obs([float(value) for value in record["obs"]])
                mask = normalize_mask([bool(value) for value in record.get("action_mask", [])])
                action = option_id(str(record[action_field]))
                if not mask[action]:
                    mask[action] = True
                metadata = record.get("metadata", {})
                player_hash = ""
                if isinstance(metadata, dict):
                    player_hash = str(metadata.get("favorite_player_hash", ""))
                pending.append({
                    "obs": obs,
                    "mask": mask,
                    "action": action,
                    "tick": int(record.get("tick", -1)),
                    "player_hash": player_hash,
                    "source": f"{file_path}:{line_number}",
                })
        examples.extend(
            DecisionExample(obs=example["obs"], mask=example["mask"], action=example["action"], source=example["source"])
            for example in pending
        )
    return examples


def apply_human_correction(
    record: dict,
    pending: list[_PendingExample],
    file_path: Path,
    line_number: int,
    correction_window_ticks: int,
) -> None:
    raw_label = record.get("label")
    if raw_label is None:
        return
    try:
        action = option_id(str(raw_label))
    except ValueError:
        return

    correction_tick = int(record.get("tick", -1))
    player_hash = str(record.get("player_hash", ""))
    for example in reversed(pending):
        if correction_tick >= 0 and example["tick"] >= 0:
            age = correction_tick - example["tick"]
            if age < 0 or age > correction_window_ticks:
                continue
        if player_hash and example["player_hash"] and player_hash != example["player_hash"]:
            continue
        example["action"] = action
        if not example["mask"][action]:
            example["mask"][action] = True
        example["source"] = f"{example['source']} corrected_by={file_path}:{line_number}"
        return


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
