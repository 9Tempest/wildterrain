#!/usr/bin/env python3
"""Distill the Xingsing rule teacher into a Java-loadable policy JSON.

This is the D0 bootstrap path: it synthesizes observation/mask states, labels
them with the teacher contract, trains a masked linear classifier with a
perceptron update, and exports the same JSON tensor format used by Java.

It deliberately uses only the Python standard library so a fresh agent can run
the first model pass before setting up NumPy/Torch for later behavior cloning.
"""

from __future__ import annotations

import argparse
import base64
import json
import random
import struct
from collections import Counter
from pathlib import Path

from features import OBS_DIM, OBS_SPEC_VERSION, OPTIONS, OPTION_TO_ID


IDX = {
    "health": 0,
    "grounded": 3,
    "carrying": 30,
    "trust": 32,
    "fear": 33,
    "mischief": 34,
    "nearest_player_distance": 37,
    "nearest_player_visible": 38,
    "nearest_player_sneaking": 39,
    "nearest_player_sprinting": 40,
    "nearest_player_jumped": 41,
    "nearest_player_dropped": 42,
    "holding_food": 43,
    "holding_weapon": 44,
    "attacked_recently": 45,
    "favorite_distance": 46,
    "favorite_visible": 47,
    "item_distance": 48,
    "item_visible": 49,
    "item_owner_favorite": 50,
    "item_age": 51,
    "safe_path_item": 52,
    "safe_return": 53,
    "tree_cover": 54,
    "bamboo": 55,
    "vines": 56,
    "perch_distance": 57,
    "perch_reachable": 58,
    "lava": 59,
    "cliff": 60,
    "escape_path": 61,
    "dash_path": 62,
    "hostile_distance": 63,
    "hostile_visible": 64,
    "hostile_targeting_player": 65,
    "hostile_targeting_xingsing": 66,
    "creeper": 67,
    "owner_under_attack": 68,
    "threat_score": 69,
    "imitate_jump": 70,
    "imitate_sneak": 71,
    "imitate_sprint": 72,
    "fetch_interest": 73,
    "return_urgency": 74,
    "play_interest": 75,
    "trust_safety": 76,
    "danger_score": 77,
    "perch_escape": 78,
    "annoyance_risk": 79,
}


def option(name: str) -> int:
    return OPTION_TO_ID[name]


def clamp01(value: float) -> float:
    return max(0.0, min(1.0, value))


def base_observation(rng: random.Random) -> list[float]:
    obs = [0.0] * OBS_DIM
    obs[IDX["health"]] = rng.uniform(0.45, 1.0)
    obs[IDX["grounded"]] = 1.0
    obs[IDX["trust"]] = rng.uniform(0.2, 0.85)
    obs[IDX["fear"]] = rng.uniform(0.0, 0.35)
    obs[IDX["mischief"]] = rng.uniform(0.25, 0.85)
    obs[IDX["nearest_player_distance"]] = rng.uniform(0.08, 0.7)
    obs[IDX["nearest_player_visible"]] = 1.0 if rng.random() < 0.7 else 0.0
    obs[IDX["favorite_distance"]] = obs[IDX["nearest_player_distance"]]
    obs[IDX["favorite_visible"]] = obs[IDX["nearest_player_visible"]]
    obs[IDX["item_distance"]] = 1.0
    obs[IDX["safe_path_item"]] = 0.0
    obs[IDX["safe_return"]] = rng.uniform(0.55, 1.0)
    obs[IDX["tree_cover"]] = rng.uniform(0.0, 0.75)
    obs[IDX["perch_distance"]] = rng.uniform(0.1, 1.0)
    obs[IDX["escape_path"]] = rng.uniform(0.0, 0.8)
    obs[IDX["dash_path"]] = rng.uniform(0.55, 1.0)
    obs[IDX["hostile_distance"]] = 1.0
    obs[IDX["trust_safety"]] = clamp01(obs[IDX["trust"]] - obs[IDX["fear"]] * 0.5)
    return obs


def synthesize(rng: random.Random, scenario: str) -> tuple[list[float], list[bool], int]:
    obs = base_observation(rng)

    if scenario == "flee_low_health":
        obs[IDX["health"]] = rng.uniform(0.08, 0.32)
        obs[IDX["escape_path"]] = rng.uniform(0.35, 1.0)
        obs[IDX["fear"]] = rng.uniform(0.45, 0.9)
        obs[IDX["perch_reachable"]] = 1.0
        obs[IDX["perch_escape"]] = rng.uniform(0.45, 0.95)
    elif scenario == "warn_hostile":
        obs[IDX["health"]] = rng.uniform(0.55, 1.0)
        obs[IDX["hostile_visible"]] = 1.0
        obs[IDX["hostile_distance"]] = rng.uniform(0.02, 0.28)
        obs[IDX["hostile_targeting_player"]] = 1.0 if rng.random() < 0.6 else 0.0
        obs[IDX["creeper"]] = 1.0 if rng.random() < 0.25 else 0.0
        obs[IDX["threat_score"]] = rng.uniform(0.68, 0.98)
        obs[IDX["danger_score"]] = obs[IDX["threat_score"]]
        obs[IDX["escape_path"]] = rng.uniform(0.3, 0.8)
    elif scenario == "return_item":
        obs[IDX["carrying"]] = 1.0
        obs[IDX["return_urgency"]] = rng.uniform(0.42, 0.98)
        obs[IDX["safe_return"]] = rng.uniform(0.55, 1.0)
        obs[IDX["nearest_player_visible"]] = 1.0
        obs[IDX["favorite_visible"]] = 1.0
    elif scenario == "pickup_item":
        obs[IDX["item_visible"]] = 1.0
        obs[IDX["item_distance"]] = rng.uniform(0.04, 0.3)
        obs[IDX["item_owner_favorite"]] = 1.0 if rng.random() < 0.7 else 0.0
        obs[IDX["item_age"]] = rng.uniform(0.0, 0.45)
        obs[IDX["safe_path_item"]] = rng.uniform(0.55, 1.0)
        obs[IDX["fetch_interest"]] = rng.uniform(0.72, 0.98)
    elif scenario == "mirror_jump":
        obs[IDX["nearest_player_visible"]] = 1.0
        obs[IDX["nearest_player_jumped"]] = 1.0
        obs[IDX["nearest_player_distance"]] = rng.uniform(0.04, 0.22)
        obs[IDX["imitate_jump"]] = rng.uniform(0.65, 0.96)
    elif scenario == "mirror_sneak":
        obs[IDX["nearest_player_visible"]] = 1.0
        obs[IDX["nearest_player_sneaking"]] = 1.0
        obs[IDX["nearest_player_distance"]] = rng.uniform(0.05, 0.25)
        obs[IDX["trust"]] = rng.uniform(0.35, 0.9)
        obs[IDX["imitate_sneak"]] = rng.uniform(0.65, 0.95)
    elif scenario == "mirror_sprint":
        obs[IDX["nearest_player_visible"]] = 1.0
        obs[IDX["nearest_player_sprinting"]] = 1.0
        obs[IDX["nearest_player_distance"]] = rng.uniform(0.1, 0.42)
        obs[IDX["trust"]] = rng.uniform(0.42, 0.95)
        obs[IDX["dash_path"]] = rng.uniform(0.55, 1.0)
        obs[IDX["imitate_sprint"]] = rng.uniform(0.58, 0.95)
    elif scenario == "play_chase":
        obs[IDX["nearest_player_visible"]] = 1.0
        obs[IDX["trust"]] = rng.uniform(0.48, 0.95)
        obs[IDX["mischief"]] = rng.uniform(0.45, 0.95)
        obs[IDX["holding_weapon"]] = 0.0
        obs[IDX["dash_path"]] = rng.uniform(0.55, 1.0)
        obs[IDX["play_interest"]] = rng.uniform(0.68, 0.98)
    elif scenario == "climb_to_perch":
        obs[IDX["perch_reachable"]] = 1.0
        obs[IDX["perch_distance"]] = rng.uniform(0.08, 0.5)
        obs[IDX["mischief"]] = rng.uniform(0.58, 0.95)
        obs[IDX["escape_path"]] = rng.uniform(0.35, 0.8)
        obs[IDX["perch_escape"]] = rng.uniform(0.58, 0.96)
    elif scenario == "observe_player":
        obs[IDX["nearest_player_visible"]] = 1.0
        obs[IDX["trust"]] = rng.uniform(0.28, 0.75)
        obs[IDX["play_interest"]] = rng.uniform(0.0, 0.55)
    elif scenario == "idle":
        obs[IDX["nearest_player_visible"]] = 0.0
        obs[IDX["favorite_visible"]] = 0.0
        obs[IDX["trust"]] = rng.uniform(0.0, 0.24)
    elif scenario == "random":
        for index in range(OBS_DIM):
            if rng.random() < 0.22:
                obs[index] = rng.random()
        obs[IDX["health"]] = max(0.05, obs[IDX["health"]])
        obs[IDX["grounded"]] = 1.0 if rng.random() < 0.85 else 0.0

    obs[IDX["trust_safety"]] = clamp01(obs[IDX["trust"]] - obs[IDX["fear"]] * 0.5)
    obs[IDX["danger_score"]] = clamp01(obs[IDX["threat_score"]] + obs[IDX["lava"]] * 0.25 + obs[IDX["cliff"]] * 0.2)
    obs[IDX["annoyance_risk"]] = clamp01((0.45 if obs[IDX["nearest_player_distance"]] < 0.08 else 0.0)
                                         + obs[IDX["mischief"]] * 0.3 + obs[IDX["fear"]] * 0.2)
    mask = build_mask(obs)
    label = teacher(obs, mask)
    return obs, mask, label


def build_mask(obs: list[float]) -> list[bool]:
    mask = [False] * len(OPTIONS)
    mask[option("IDLE_GROOM")] = True
    mask[option("OBSERVE_PLAYER")] = obs[IDX["nearest_player_visible"]] > 0.5
    mask[option("APPROACH_PLAYER")] = mask[option("OBSERVE_PLAYER")] and obs[IDX["nearest_player_distance"]] > 0.12
    mask[option("KEEP_PLAY_DISTANCE")] = mask[option("OBSERVE_PLAYER")] and obs[IDX["nearest_player_distance"]] < 0.28
    mask[option("MIRROR_JUMP")] = (
        obs[IDX["nearest_player_jumped"]] > 0.5 and obs[IDX["grounded"]] > 0.5 and obs[IDX["lava"]] <= 0.5
    )
    mask[option("MIRROR_SNEAK")] = obs[IDX["nearest_player_sneaking"]] > 0.5 and obs[IDX["trust"]] > 0.2
    mask[option("MIRROR_SPRINT")] = obs[IDX["nearest_player_sprinting"]] > 0.5 and obs[IDX["dash_path"]] > 0.45
    mask[option("PICKUP_ITEM")] = (
        obs[IDX["carrying"]] <= 0.5 and obs[IDX["item_visible"]] > 0.5 and obs[IDX["safe_path_item"]] > 0.4
    )
    mask[option("RETURN_ITEM")] = obs[IDX["carrying"]] > 0.5 and obs[IDX["safe_return"]] > 0.3
    mask[option("PLAY_CHASE")] = (
        obs[IDX["trust"]] > 0.35 and obs[IDX["play_interest"]] > 0.35 and obs[IDX["dash_path"]] > 0.45
    )
    mask[option("CLIMB_TO_PERCH")] = obs[IDX["perch_reachable"]] > 0.5 and obs[IDX["perch_escape"]] > 0.25
    mask[option("WARN_HOSTILE")] = obs[IDX["hostile_visible"]] > 0.5 and obs[IDX["threat_score"]] > 0.25
    mask[option("FLEE_TO_TREE")] = (
        obs[IDX["escape_path"]] > 0.25
        and (obs[IDX["threat_score"]] > 0.4 or obs[IDX["fear"]] > 0.65 or obs[IDX["health"]] < 0.35)
    )
    mask[option("LEAD_TO_FRUIT")] = False
    return mask


def teacher(obs: list[float], mask: list[bool]) -> int:
    if obs[IDX["health"]] < 0.35 and mask[option("FLEE_TO_TREE")]:
        return option("FLEE_TO_TREE")
    if obs[IDX["threat_score"]] > 0.65 and mask[option("WARN_HOSTILE")]:
        return option("WARN_HOSTILE")
    if obs[IDX["carrying"]] > 0.5 and obs[IDX["return_urgency"]] > 0.35 and mask[option("RETURN_ITEM")]:
        return option("RETURN_ITEM")
    if obs[IDX["fetch_interest"]] > 0.70 and mask[option("PICKUP_ITEM")]:
        return option("PICKUP_ITEM")
    if obs[IDX["imitate_jump"]] > 0.60 and mask[option("MIRROR_JUMP")]:
        return option("MIRROR_JUMP")
    if obs[IDX["imitate_sneak"]] > 0.60 and mask[option("MIRROR_SNEAK")]:
        return option("MIRROR_SNEAK")
    if obs[IDX["imitate_sprint"]] > 0.55 and obs[IDX["trust"]] > 0.35 and mask[option("MIRROR_SPRINT")]:
        return option("MIRROR_SPRINT")
    if obs[IDX["play_interest"]] > 0.65 and obs[IDX["trust"]] > 0.45 and mask[option("PLAY_CHASE")]:
        return option("PLAY_CHASE")
    if obs[IDX["perch_escape"]] > 0.55 and obs[IDX["mischief"]] > 0.55 and mask[option("CLIMB_TO_PERCH")]:
        return option("CLIMB_TO_PERCH")
    if obs[IDX["nearest_player_visible"]] > 0.5 and obs[IDX["trust"]] > 0.25 and mask[option("OBSERVE_PLAYER")]:
        return option("OBSERVE_PLAYER")
    return option("IDLE_GROOM")


def nonzero(obs: list[float]) -> list[tuple[int, float]]:
    return [(index, value) for index, value in enumerate(obs) if abs(value) > 1.0e-8]


def predict(weights: list[list[float]], bias: list[float], obs: list[float], mask: list[bool]) -> int:
    nz = nonzero(obs)
    best = option("IDLE_GROOM")
    best_score = -1.0e30
    for action, allowed in enumerate(mask):
        if not allowed:
            continue
        score = bias[action]
        row = weights[action]
        for index, value in nz:
            score += row[index] * value
        if score > best_score:
            best = action
            best_score = score
    return best


def train(examples: list[tuple[list[float], list[bool], int]], epochs: int, learning_rate: float, seed: int):
    rng = random.Random(seed)
    weights = [[0.0] * OBS_DIM for _ in OPTIONS]
    bias = [0.0] * len(OPTIONS)

    for epoch in range(1, epochs + 1):
        rng.shuffle(examples)
        errors = 0
        for obs, mask, target in examples:
            pred = predict(weights, bias, obs, mask)
            if pred == target:
                continue
            errors += 1
            nz = nonzero(obs)
            bias[target] += learning_rate
            bias[pred] -= learning_rate
            for index, value in nz:
                weights[target][index] += learning_rate * value
                weights[pred][index] -= learning_rate * value
        print(f"epoch {epoch:02d} errors={errors} acc={1.0 - errors / len(examples):.4f}")
    return weights, bias


def evaluate(weights, bias, examples):
    correct = 0
    confusion = [[0] * len(OPTIONS) for _ in OPTIONS]
    for obs, mask, target in examples:
        pred = predict(weights, bias, obs, mask)
        correct += int(pred == target)
        confusion[target][pred] += 1
    return correct / len(examples), confusion


def f32_base64(values: list[float]) -> str:
    payload = b"".join(struct.pack("<f", float(value)) for value in values)
    return base64.b64encode(payload).decode("ascii")


def export_policy(weights, bias, out: Path, metadata: dict) -> None:
    out.parent.mkdir(parents=True, exist_ok=True)
    flat_weights = [value for row in weights for value in row]
    policy = {
        "schema_version": 1,
        "entity": "wildterrain:xingsing",
        "policy_version": "xingsing_teacher_distilled_v1",
        "obs_spec_version": OBS_SPEC_VERSION,
        "num_actions": len(OPTIONS),
        "training": metadata,
        "layers": [
            {
                "type": "linear",
                "in": OBS_DIM,
                "out": len(OPTIONS),
                "weights": f32_base64(flat_weights),
                "bias": f32_base64(bias),
            }
        ],
    }
    out.write_text(json.dumps(policy, separators=(",", ":")), encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--examples", type=int, default=50000)
    parser.add_argument("--test-examples", type=int, default=10000)
    parser.add_argument("--epochs", type=int, default=18)
    parser.add_argument("--learning-rate", type=float, default=0.35)
    parser.add_argument("--seed", type=int, default=23)
    parser.add_argument("--out", default="src/main/resources/data/wildterrain/policies/xingsing_policy_v1.json")
    parser.add_argument("--report", default="artifacts/policies/xingsing_teacher_distilled_v1/report.json")
    args = parser.parse_args()

    scenarios = [
        "flee_low_health",
        "warn_hostile",
        "return_item",
        "pickup_item",
        "mirror_jump",
        "mirror_sneak",
        "mirror_sprint",
        "play_chase",
        "climb_to_perch",
        "observe_player",
        "idle",
        "random",
    ]
    rng = random.Random(args.seed)
    train_examples = [synthesize(rng, scenarios[i % len(scenarios)]) for i in range(args.examples)]
    test_rng = random.Random(args.seed + 1)
    test_examples = [synthesize(test_rng, scenarios[i % len(scenarios)]) for i in range(args.test_examples)]

    counts = Counter(OPTIONS[label] for _, _, label in train_examples)
    print("train label counts:")
    for name in OPTIONS:
        if counts[name]:
            print(f"  {name}: {counts[name]}")

    weights, bias = train(train_examples, args.epochs, args.learning_rate, args.seed + 2)
    train_accuracy, _ = evaluate(weights, bias, train_examples)
    test_accuracy, confusion = evaluate(weights, bias, test_examples)
    print(f"train_accuracy={train_accuracy:.4f}")
    print(f"test_accuracy={test_accuracy:.4f}")

    metadata = {
        "source": "synthetic_teacher_v1",
        "train_examples": args.examples,
        "test_examples": args.test_examples,
        "epochs": args.epochs,
        "learning_rate": args.learning_rate,
        "seed": args.seed,
        "train_accuracy": round(train_accuracy, 6),
        "test_accuracy": round(test_accuracy, 6),
        "options": OPTIONS,
    }
    export_policy(weights, bias, Path(args.out), metadata)
    print(f"wrote {args.out}")

    report = Path(args.report)
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps({
        **metadata,
        "label_counts": dict(counts),
        "confusion": confusion,
    }, indent=2), encoding="utf-8")
    print(f"wrote {report}")


if __name__ == "__main__":
    main()
