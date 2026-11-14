"""Turns Codex's raw PNGs (tools/art/raw) into the small WebPs the app ships (app/src/main/assets/art).

    python tools/art/pack.py

Things and pictures are cropped to the object and padded to a square; eras are cropped to the vignette;
icons become a white mask (the app tints them), so one file serves every colour.
"""
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
RAW = Path(__file__).parent / "raw"
OUT = ROOT / "app/src/main/assets/art"

SIZE = {"thing": 320, "pic": 480, "icon": 128}
ERA_W = 560
STAGE_W = 720


def clean(im: Image.Image) -> Image.Image:
    """Drops the faint haze and fringe a generated cut-out leaves around its edges."""
    a = np.asarray(im.convert("RGBA")).astype(np.float32)
    al = a[..., 3]
    al = np.clip((al - 16) * (255 / 239), 0, 255)
    a[..., 3] = al
    a[al == 0, :3] = 0
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def square(im: Image.Image, margin: float) -> Image.Image:
    box = im.getbbox()
    if box:
        im = im.crop(box)
    s = int(max(im.size) * (1 + 2 * margin))
    c = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    c.alpha_composite(im, ((s - im.width) // 2, (s - im.height) // 2))
    return c


def mask(im: Image.Image) -> Image.Image:
    """White, with the ink's darkness as alpha: tinted in the app."""
    a = np.asarray(im.convert("RGBA")).astype(np.float32)
    lum = (0.3 * a[..., 0] + 0.59 * a[..., 1] + 0.11 * a[..., 2]) / 255
    dark = np.clip((1 - lum) * 1.6, 0, 1)
    out = np.zeros_like(a)
    out[..., :3] = 255
    out[..., 3] = a[..., 3] * dark
    return Image.fromarray(out.astype(np.uint8), "RGBA")


def main():
    n = 0
    for src in sorted(RAW.rglob("*.png")):
        kind = src.parent.name
        if kind not in ("thing", "era", "pic", "icon", "stage", "map"):
            continue
        dst = OUT / kind / (src.stem + ".webp")
        # Only what changed since it was packed (or since this script changed).
        if dst.exists() and dst.stat().st_mtime > max(src.stat().st_mtime, Path(__file__).stat().st_mtime):
            continue
        im = clean(Image.open(src))
        if kind == "era":
            box = im.getbbox()
            im = im.crop(box) if box else im
            im = im.resize((ERA_W, round(im.height * ERA_W / im.width)), Image.LANCZOS)
        elif kind == "map" and src.stem in ("sea", "land"):
            # Textures stay whole and opaque: the app cuts them by the continents.
            im = Image.open(src).convert("RGB").resize((1024, 512), Image.LANCZOS)
            # Toned down to the app's paper: the land C.land, the sea C.sea.
            tone, k = ((212, 189, 140), .45) if src.stem == "land" else ((233, 215, 174), .25)
            im = Image.blend(im, Image.new("RGB", im.size, tone), k)
        elif kind == "map":
            im = square(im, 0.02).resize((256, 256), Image.LANCZOS)
        elif kind == "stage":
            # Cropped tight to the object: the app maps it onto the stage's own geometry.
            im = im.crop(im.getbbox())
            im = im.resize((STAGE_W, round(im.height * STAGE_W / im.width)), Image.LANCZOS)
        elif kind == "icon":
            im = mask(square(im, 0.04)).resize((SIZE[kind],) * 2, Image.LANCZOS)
        else:
            im = square(im, 0.05 if kind == "thing" else 0.03).resize((SIZE[kind],) * 2, Image.LANCZOS)
        dst.parent.mkdir(parents=True, exist_ok=True)
        im.save(dst, "WEBP", quality={"icon": 92, "era": 70, "map": 78}.get(kind, 84), method=6)
        n += 1
    total = sum(p.stat().st_size for p in OUT.rglob("*.webp"))
    print(f"{n} pictures, {total / 1024:.0f} KB")


if __name__ == "__main__":
    main()
