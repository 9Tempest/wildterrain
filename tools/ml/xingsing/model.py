"""Tiny NumPy MLP used for behavior cloning and Java export."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

import numpy as np

from features import OBS_DIM, OPTIONS


@dataclass
class TinyMlp:
    w0: np.ndarray
    b0: np.ndarray
    w1: np.ndarray
    b1: np.ndarray
    w2: np.ndarray
    b2: np.ndarray

    @classmethod
    def init(cls, hidden: int = 128, seed: int = 7) -> "TinyMlp":
        rng = np.random.default_rng(seed)
        return cls(
            w0=(rng.normal(0.0, 0.03, size=(hidden, OBS_DIM))).astype(np.float32),
            b0=np.zeros(hidden, dtype=np.float32),
            w1=(rng.normal(0.0, 0.03, size=(hidden, hidden))).astype(np.float32),
            b1=np.zeros(hidden, dtype=np.float32),
            w2=(rng.normal(0.0, 0.03, size=(len(OPTIONS), hidden))).astype(np.float32),
            b2=np.zeros(len(OPTIONS), dtype=np.float32),
        )

    def forward(self, obs: np.ndarray) -> tuple[np.ndarray, tuple[np.ndarray, np.ndarray]]:
        z0 = obs @ self.w0.T + self.b0
        h0 = np.maximum(z0, 0.0)
        z1 = h0 @ self.w1.T + self.b1
        h1 = np.maximum(z1, 0.0)
        logits = h1 @ self.w2.T + self.b2
        return logits, (h0, h1)

    def predict(self, obs: np.ndarray, mask: np.ndarray) -> np.ndarray:
        logits, _ = self.forward(obs)
        masked = np.where(mask, logits, -1.0e9)
        return masked.argmax(axis=1)

    def save(self, path: Path) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        np.savez(path, w0=self.w0, b0=self.b0, w1=self.w1, b1=self.b1, w2=self.w2, b2=self.b2)

    @classmethod
    def load(cls, path: Path) -> "TinyMlp":
        data = np.load(path)
        return cls(*(data[name].astype(np.float32) for name in ("w0", "b0", "w1", "b1", "w2", "b2")))


def masked_softmax(logits: np.ndarray, mask: np.ndarray) -> np.ndarray:
    masked = np.where(mask, logits, -1.0e9)
    shifted = masked - masked.max(axis=1, keepdims=True)
    exp = np.exp(shifted) * mask
    denom = np.maximum(exp.sum(axis=1, keepdims=True), 1.0e-8)
    return exp / denom
