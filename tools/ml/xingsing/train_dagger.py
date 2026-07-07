#!/usr/bin/env python3
"""Create an aggregate DAgger dataset manifest from rollout and correction logs."""

from __future__ import annotations

import argparse
import json
from pathlib import Path


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--logs", nargs="+", required=True)
    parser.add_argument("--out", required=True)
    args = parser.parse_args()

    files: list[str] = []
    for raw in args.logs:
        path = Path(raw)
        if path.is_dir():
            files.extend(str(item) for item in sorted(path.rglob("*.jsonl")))
        elif path.suffix == ".jsonl":
            files.append(str(path))
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps({"schema_version": 1, "files": files}, indent=2), encoding="utf-8")
    print(f"wrote {out} with {len(files)} files")


if __name__ == "__main__":
    main()
