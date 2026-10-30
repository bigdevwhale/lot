"""Builds the Google Play graphics in docs/google-play/graphics from the README screenshots.

python tools/play.py

Takes docs/screenshots/<lang>/*.jpg (1080 px wide renders of the demo, see tools/shots.py) and writes
the 512 px icon, the 1024x500 feature graphics and seven 1080x1920 captioned screenshots per language.
Play wants screenshots no longer than twice their width, so each screen is cut short under its caption.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs/google-play/graphics"
SHOTS = ROOT / "docs/screenshots"
FONT = ROOT / "app/src/main/res/font"

PAPER = (0xEF, 0xE0, 0xBF)
PAGE = (0xF7, 0xEC, 0xD2)
RULE = (0xD9, 0xC4, 0x9B)
MARGIN = (0xE3, 0xB8, 0xA0)
INK = (0x2A, 0x1A, 0x10)
RED = (0x8F, 0x2F, 0x22)

# The order they appear in the store, with the caption over each: an ink line and a red one.
PAGES = [
    ("1-draw", ("One of 117 billion.", "The lot decides which"), ("Один из 117 миллиардов.", "Кем — решит жребий")),
    ("2-book", ("Every door is shown,", "and why it is shut"), ("Видны все двери —", "и почему закрыты")),
    ("3-end", ("A life written", "into a parish register"), ("Жизнь в метрической", "книге, год за годом")),
    ("6-atlas", ("Your lots next to", "everyone ever born"), ("Твои жребии рядом", "со всеми, кто жил")),
    ("4-chest", ("Each life leaves", "one thing behind"), ("Каждая жизнь", "оставляет вещь")),
    ("5-thing", ("Handed down", "to great-grandchildren"), ("Вещь переходит", "к правнукам")),
    ("0-welcome", ("Mostly a peasant,", "long before 1800"), ("Чаще всего крестьянин", "задолго до 1800 года")),
]


def font(name, size):
    return ImageFont.truetype(str(FONT / name), size)


def register(w, h):
    """A page of the parish register: ruled lines and a red margin rule, as in the book."""
    g = Image.new("RGB", (w, h), PAGE)
    d = ImageDraw.Draw(g)
    for y in range(36, h, 36):
        d.line((0, y, w, y), fill=RULE, width=2)
    d.line((80, 0, 80, h), fill=MARGIN, width=3)
    return g.convert("RGBA")


def lay(g, card, x, y):
    """A screen on the page: a soft shadow and a thin ink rule round it."""
    sh = Image.new("RGBA", (card.width + 80, card.height + 80), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rectangle((40, 50, 40 + card.width, 50 + card.height), fill=(60, 40, 20, 110))
    g.alpha_composite(sh.filter(ImageFilter.GaussianBlur(18)), (x - 40, y - 40))
    g.alpha_composite(card.convert("RGBA"), (x, y))
    ImageDraw.Draw(g).rectangle((x - 3, y - 3, x + card.width + 2, y + card.height + 2), outline=INK, width=3)


def fit(name, text, size, width):
    while font(name, size).getlength(text) > width:
        size -= 2
    return font(name, size)


def screenshot(src, caption):
    W, H = 1080, 1920
    g = register(W, H)
    d = ImageDraw.Draw(g)
    d.rectangle((0, 0, W, 330), fill=PAPER)
    d.rectangle((60, 40, W - 60, 46), fill=INK)
    size = min(fit("pt_serif_bold.ttf", line, 80, W - 140).size for line in caption)
    title = font("pt_serif_bold.ttf", size)
    d.text((W / 2, 135), caption[0], font=title, fill=INK, anchor="mm")
    d.text((W / 2, 235), caption[1], font=title, fill=RED, anchor="mm")
    d.rectangle((60, 318, W - 60, 321), fill=INK)
    im = Image.open(src).convert("RGB")
    w = 820
    im = im.resize((w, round(im.height * w / im.width)), Image.LANCZOS)
    top = 380
    # The screen runs off the bottom edge, like a page that goes on.
    lay(g, im.crop((0, 0, w, H - top + 20)), (W - w) // 2, top)
    return g.convert("RGB")


def feature(lang):
    W, H = 1024, 500
    g = register(W, H)
    ImageDraw.Draw(g).rectangle((0, 0, 520, H), fill=PAPER)
    draw = Image.open(SHOTS / lang / "1-draw.jpg").convert("RGB")
    # The tag rising out of the sack and the card of who you are.
    draw = draw.crop((0, 260, 1080, 1560))
    draw = draw.resize((round(draw.width * 420 / draw.height), 420), Image.LANCZOS)
    lay(g, draw, W - draw.width - 50, 40)
    d = ImageDraw.Draw(g)
    name, lines = ("Lot", ["You are one of", "117 billion.", "The lot decides which."]) if lang == "en" else \
        ("Жребий", ["Ты — один из", "117 миллиардов.", "Кем — решит жребий."])
    d.rectangle((48, 100, 490, 106), fill=INK)
    d.text((48, 118), name, font=fit("pt_serif_bold.ttf", name, 96, 440), fill=INK)
    d.rectangle((48, 240, 490, 242), fill=INK)
    sub = font("pt_serif_bold.ttf", 34)
    for i, l in enumerate(lines):
        d.text((48, 266 + i * 46), l, font=sub, fill=RED if i == 2 else INK)
    g.convert("RGB").save(OUT / f"feature-graphic-{lang}.png", optimize=True)


def icon():
    """The README logo is already a full square, so Play's mask finds no corners."""
    Image.open(ROOT / "docs/logo.png").convert("RGB").resize((512, 512), Image.LANCZOS).save(OUT / "icon-512.png", optimize=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    icon()
    for lang in ("en", "ru"):
        feature(lang)
        folder = OUT / f"screenshots-{lang}"
        folder.mkdir(exist_ok=True)
        for i, (name, en, ru) in enumerate(PAGES, 1):
            screenshot(SHOTS / lang / f"{name}.jpg", en if lang == "en" else ru).save(folder / f"{i}.png", optimize=True)
    print("done:", OUT)


if __name__ == "__main__":
    main()
