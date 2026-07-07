#!/usr/bin/env python3
"""Generate deterministic first-party pixel assets for Xingsing.

The art is intentionally compact but source-controlled through this script:
white ears, pale face, teal shadow accents, and warm jungle browns.
"""

from __future__ import annotations

import random
import struct
import zlib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ENTITY_TEXTURE = ROOT / "src/main/resources/assets/wildterrain/textures/entity/xingsing.png"
GUIDE_TEXTURE = ROOT / "src/main/resources/assets/wildterrain/textures/item/xingsing_field_guide.png"

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
            (highlight, base, edge), max(3, (w * h) // 11), seed)


def generate_entity_texture() -> None:
    width = 64
    height = 64
    pixels = canvas(width, height)

    fur = (104, 92, 67, 255)
    fur_dark = (55, 54, 43, 255)
    fur_light = (153, 130, 86, 255)
    cream = (242, 238, 226, 255)
    cream_shadow = (204, 198, 181, 255)
    teal = (142, 199, 216, 255)
    teal_dark = (70, 119, 136, 255)
    blush = (182, 111, 101, 255)

    shaded_patch(pixels, width, height, 0, 0, 24, 14, fur, fur_dark, fur_light, 31)
    shaded_patch(pixels, width, height, 0, 14, 26, 12, fur, fur_dark, fur_light, 32)
    shaded_patch(pixels, width, height, 0, 26, 22, 10, cream, cream_shadow, (255, 250, 238, 255), 33)
    shaded_patch(pixels, width, height, 22, 26, 10, 10, cream, fur_dark, (255, 250, 238, 255), 34)
    shaded_patch(pixels, width, height, 30, 18, 24, 7, cream, cream_shadow, (255, 250, 238, 255), 35)

    shaded_patch(pixels, width, height, 28, 0, 8, 12, fur, fur_dark, fur_light, 36)
    shaded_patch(pixels, width, height, 36, 0, 10, 5, fur, fur_dark, fur_light, 37)
    shaded_patch(pixels, width, height, 0, 38, 22, 11, fur, fur_dark, fur_light, 38)
    shaded_patch(pixels, width, height, 42, 5, 16, 10, fur, fur_dark, fur_light, 39)

    for x in (36, 42):
        shaded_patch(pixels, width, height, x, 25, 5, 8, cream, cream_shadow, (255, 250, 238, 255), x)
        rect(pixels, width, height, x + 2, 27, 1, 5, teal)

    rect(pixels, width, height, 6, 28, 3, 2, fur_dark)
    rect(pixels, width, height, 14, 28, 3, 2, fur_dark)
    rect(pixels, width, height, 9, 32, 5, 1, blush)
    rect(pixels, width, height, 3, 17, 5, 2, teal)
    rect(pixels, width, height, 17, 17, 5, 2, teal)

    for y in range(7, 13, 2):
        rect(pixels, width, height, 44, y, 6, 1, teal_dark)
    for x in range(5, 22, 5):
        rect(pixels, width, height, x, 5, 2, 1, fur_light)
    for x in range(4, 20, 6):
        rect(pixels, width, height, x, 43, 3, 1, cream_shadow)

    write_png(ENTITY_TEXTURE, width, height, pixels)


def generate_guide_icon() -> None:
    width = 16
    height = 16
    pixels = canvas(width, height)
    cover = (78, 95, 72, 255)
    dark = (33, 42, 36, 255)
    page = (242, 238, 226, 255)
    teal = (142, 199, 216, 255)
    gold = (218, 184, 92, 255)

    rect(pixels, width, height, 3, 1, 10, 14, cover)
    frame(pixels, width, height, 3, 1, 10, 14, dark)
    rect(pixels, width, height, 5, 3, 6, 10, page)
    rect(pixels, width, height, 4, 2, 1, 12, gold)
    rect(pixels, width, height, 6, 5, 5, 1, dark)
    rect(pixels, width, height, 6, 8, 4, 1, dark)
    rect(pixels, width, height, 7, 11, 3, 1, dark)
    rect(pixels, width, height, 10, 4, 1, 3, teal)
    rect(pixels, width, height, 11, 5, 1, 2, teal)
    put(pixels, width, height, 12, 8, teal)
    put(pixels, width, height, 9, 10, teal)

    write_png(GUIDE_TEXTURE, width, height, pixels)


def main() -> None:
    generate_entity_texture()
    generate_guide_icon()
    print(f"wrote {ENTITY_TEXTURE.relative_to(ROOT)}")
    print(f"wrote {GUIDE_TEXTURE.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
