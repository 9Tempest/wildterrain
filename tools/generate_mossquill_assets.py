#!/usr/bin/env python3
"""Generate first-party pixel assets for the Mossquill playable slice.

The output is intentionally deterministic so other agents can refine the art,
rerun the script, and review the exact asset delta in git.
"""

from __future__ import annotations

import random
import struct
import zlib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ENTITY_TEXTURE = ROOT / "src/main/resources/assets/wildterrain/textures/entity/mossquill.png"
GUIDE_TEXTURE = ROOT / "src/main/resources/assets/wildterrain/textures/item/mossquill_field_guide.png"


Color = tuple[int, int, int, int]


def write_png(path: Path, width: int, height: int, pixels: list[Color]) -> None:
    def chunk(kind: bytes, data: bytes) -> bytes:
        return (
            struct.pack(">I", len(data))
            + kind
            + data
            + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
        )

    rows = []
    for y in range(height):
        row = bytearray([0])
        for pixel in pixels[y * width : (y + 1) * width]:
            row.extend(pixel)
        rows.append(bytes(row))

    path.parent.mkdir(parents=True, exist_ok=True)
    payload = zlib.compress(b"".join(rows), level=9)
    path.write_bytes(
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
        + chunk(b"IDAT", payload)
        + chunk(b"IEND", b"")
    )


def canvas(width: int, height: int) -> list[Color]:
    return [(0, 0, 0, 0) for _ in range(width * height)]


def put(pixels: list[Color], width: int, height: int, x: int, y: int, color: Color) -> None:
    if 0 <= x < width and 0 <= y < height:
        pixels[y * width + x] = color


def rect(pixels: list[Color], width: int, height: int, x: int, y: int, w: int, h: int, color: Color) -> None:
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            put(pixels, width, height, xx, yy, color)


def frame(pixels: list[Color], width: int, height: int, x: int, y: int, w: int, h: int, color: Color) -> None:
    rect(pixels, width, height, x, y, w, 1, color)
    rect(pixels, width, height, x, y + h - 1, w, 1, color)
    rect(pixels, width, height, x, y, 1, h, color)
    rect(pixels, width, height, x + w - 1, y, 1, h, color)


def speckle(
    pixels: list[Color],
    width: int,
    height: int,
    x: int,
    y: int,
    w: int,
    h: int,
    colors: tuple[Color, ...],
    amount: int,
    seed: int,
) -> None:
    rng = random.Random(seed)
    for _ in range(amount):
        put(pixels, width, height, x + rng.randrange(w), y + rng.randrange(h), rng.choice(colors))


def shaded_patch(
    pixels: list[Color],
    width: int,
    height: int,
    x: int,
    y: int,
    w: int,
    h: int,
    base: Color,
    edge: Color,
    highlight: Color,
    seed: int,
) -> None:
    rect(pixels, width, height, x, y, w, h, base)
    frame(pixels, width, height, x, y, w, h, edge)
    rect(pixels, width, height, x + 2, y + 1, max(1, w - 4), 1, highlight)
    speckle(pixels, width, height, x + 1, y + 1, max(1, w - 2), max(1, h - 2),
            (highlight, (135, 164, 90, 255), (62, 82, 52, 255)), max(3, (w * h) // 12), seed)


def generate_entity_texture() -> None:
    width = 64
    height = 64
    pixels = canvas(width, height)

    bark = (86, 79, 50, 255)
    bark_dark = (49, 55, 38, 255)
    moss = (83, 116, 68, 255)
    moss_dark = (45, 74, 49, 255)
    moss_light = (189, 219, 127, 255)
    cream = (211, 210, 152, 255)
    rose = (168, 104, 92, 255)

    shaded_patch(pixels, width, height, 0, 0, 30, 17, moss, moss_dark, moss_light, 11)
    shaded_patch(pixels, width, height, 38, 24, 24, 12, bark, bark_dark, cream, 12)
    shaded_patch(pixels, width, height, 0, 18, 30, 12, moss, moss_dark, moss_light, 13)
    shaded_patch(pixels, width, height, 18, 18, 10, 6, cream, bark_dark, (235, 229, 169, 255), 14)
    shaded_patch(pixels, width, height, 28, 0, 16, 12, moss, moss_dark, moss_light, 15)
    shaded_patch(pixels, width, height, 0, 34, 38, 12, (72, 127, 71, 255), moss_dark, moss_light, 16)

    for offset_x in (46, 54):
        shaded_patch(pixels, width, height, offset_x, 0, 8, 24, (59, 94, 61, 255), moss_dark, moss_light, offset_x)
        for yy in range(2, 23, 4):
            rect(pixels, width, height, offset_x + 3, yy, 2, 2, cream)

    shaded_patch(pixels, width, height, 0, 48, 16, 8, bark, bark_dark, cream, 17)
    shaded_patch(pixels, width, height, 18, 48, 16, 8, bark, bark_dark, cream, 18)
    shaded_patch(pixels, width, height, 28, 18, 8, 8, rose, bark_dark, cream, 19)

    for xx in range(3, 27, 6):
        rect(pixels, width, height, xx, 6, 3, 1, moss_light)
    for xx in range(5, 35, 7):
        rect(pixels, width, height, xx, 39, 3, 2, moss_light)

    write_png(ENTITY_TEXTURE, width, height, pixels)


def generate_guide_icon() -> None:
    width = 16
    height = 16
    pixels = canvas(width, height)
    leather = (70, 91, 56, 255)
    leather_dark = (34, 45, 33, 255)
    page = (219, 214, 158, 255)
    gold = (218, 198, 94, 255)
    moss_light = (189, 219, 127, 255)

    rect(pixels, width, height, 3, 1, 10, 14, leather)
    frame(pixels, width, height, 3, 1, 10, 14, leather_dark)
    rect(pixels, width, height, 5, 3, 6, 10, page)
    rect(pixels, width, height, 4, 2, 1, 12, gold)
    rect(pixels, width, height, 7, 5, 5, 1, leather_dark)
    rect(pixels, width, height, 7, 8, 4, 1, leather_dark)
    rect(pixels, width, height, 6, 11, 3, 1, leather_dark)
    put(pixels, width, height, 10, 10, moss_light)
    put(pixels, width, height, 11, 9, moss_light)
    put(pixels, width, height, 11, 11, moss_light)
    put(pixels, width, height, 12, 10, moss_light)

    write_png(GUIDE_TEXTURE, width, height, pixels)


def main() -> None:
    generate_entity_texture()
    generate_guide_icon()
    print(f"wrote {ENTITY_TEXTURE.relative_to(ROOT)}")
    print(f"wrote {GUIDE_TEXTURE.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
