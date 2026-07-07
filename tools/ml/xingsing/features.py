"""Shared Xingsing feature and action spec."""

from __future__ import annotations

from dataclasses import dataclass


OBS_SPEC_VERSION = 1
OBS_DIM = 110
OPTIONS = [
    "IDLE_GROOM",
    "OBSERVE_PLAYER",
    "APPROACH_PLAYER",
    "KEEP_PLAY_DISTANCE",
    "MIRROR_JUMP",
    "MIRROR_SNEAK",
    "MIRROR_SPRINT",
    "PICKUP_ITEM",
    "RETURN_ITEM",
    "PLAY_CHASE",
    "CLIMB_TO_PERCH",
    "WARN_HOSTILE",
    "FLEE_TO_TREE",
    "LEAD_TO_FRUIT",
]
OPTION_TO_ID = {name: index for index, name in enumerate(OPTIONS)}


@dataclass(frozen=True)
class DecisionExample:
    obs: list[float]
    mask: list[bool]
    action: int
    source: str


def option_id(name: str) -> int:
    normalized = name.strip().upper()
    if normalized not in OPTION_TO_ID:
        raise ValueError(f"unknown Xingsing option {name!r}")
    return OPTION_TO_ID[normalized]


def normalize_obs(obs: list[float]) -> list[float]:
    if len(obs) > OBS_DIM:
        return obs[:OBS_DIM]
    if len(obs) < OBS_DIM:
        return obs + [0.0] * (OBS_DIM - len(obs))
    return obs


def normalize_mask(mask: list[bool]) -> list[bool]:
    if len(mask) > len(OPTIONS):
        return mask[: len(OPTIONS)]
    if len(mask) < len(OPTIONS):
        return mask + [False] * (len(OPTIONS) - len(mask))
    return mask
