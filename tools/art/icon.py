"""The launcher icon from Codex's painted layers (tools/art/raw/app): the adaptive icon's mipmaps, the Play icon and the
README logo.

    python tools/art/icon.py

The monochrome layer for themed icons stays the vector tag in res/drawable/ic_launcher_foreground.xml.
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
RAW = Path(__file__).parent / "raw" / "app"
RES = ROOT / "app/src/main/res"

# Of the 108dp adaptive canvas, the motif keeps inside the 66dp circle every launcher mask leaves whole.
MOTIF = 0.62
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}


def layers(n: int):
    bg = Image.open(RAW / "background.png").convert("RGB")
    side = min(bg.size)
    bg = bg.crop(((bg.width - side) // 2, (bg.height - side) // 2, (bg.width + side) // 2, (bg.height + side) // 2))
    bg = bg.resize((n, n), Image.LANCZOS).convert("RGBA")
    fg = Image.open(RAW / "foreground.png").convert("RGBA")
    fg = fg.crop(fg.getbbox())
    s = int(n * MOTIF)
    fg.thumbnail((s, s), Image.LANCZOS)
    canvas = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    canvas.alpha_composite(fg, ((n - fg.width) // 2, (n - fg.height) // 2))
    return bg, canvas


def main():
    for d, k in DENSITIES.items():
        n = round(108 * k)
        bg, fg = layers(n)
        out = RES / f"mipmap-{d}"
        out.mkdir(exist_ok=True)
        bg.convert("RGB").save(out / "ic_launcher_background.png", optimize=True)
        fg.save(out / "ic_launcher_foreground.png", optimize=True)

    # Play draws its own mask over a full square; the motif a little larger there.
    bg, fg = layers(1024)
    play = bg.copy()
    play.alpha_composite(fg.resize((int(1024 * 1.1),) * 2, Image.LANCZOS), (-int(1024 * .05),) * 2)
    play.convert("RGB").resize((512, 512), Image.LANCZOS).save(ROOT / "docs/google-play/graphics/icon-512.png", optimize=True)

    # The README logo: the icon as a launcher shows it, on a transparent page.
    full = bg.copy()
    full.alpha_composite(fg)
    m = Image.new("L", full.size, 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, 1023, 1023), radius=230, fill=255)
    logo = Image.new("RGBA", full.size, (0, 0, 0, 0))
    logo.paste(full, (0, 0), m)
    logo.resize((256, 256), Image.LANCZOS).save(ROOT / "docs/logo.png", optimize=True)
    print("icon done")


if __name__ == "__main__":
    main()
