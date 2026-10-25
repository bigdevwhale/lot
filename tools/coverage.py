"""Shows how many lines each region gets per kind in one group, counting only lines anyone there can draw (no `need`).
The thinnest cells are where a new line adds the most variety.

    python tools/coverage.py med
"""
import sys
from pathlib import Path

A = Path(__file__).resolve().parent.parent / "app/src/main/assets"
# The regions of each group and the tags they carry (as in World.kt).
REGIONS = {
    "stone": {"eaf": "af", "me": "me", "eur": "eu med", "sas": "in", "eas": "cn", "am": "am meso", "au": "au"},
    "anc": {"cn": "cn", "in": "in", "mes": "me", "eg": "eg me", "med": "med it eu", "waf": "af", "meso": "meso am", "andes": "andes am", "au": "au"},
    "med": {"cn": "cn", "in": "in", "me": "me eg", "fr": "eu fr", "it": "eu it med", "de": "eu de", "en": "eu en", "ru": "ru", "waf": "af",
            "jp": "jp", "sea": "sea", "meso": "meso am", "andes": "andes am", "au": "au"},
    "early": {"cn": "cn", "in": "in", "sea": "sea", "jp": "jp", "me": "me eg", "fr": "eu fr", "de": "eu de", "en": "eu en", "it": "eu it med",
              "ru": "ru", "waf": "af", "latam": "latam meso andes am", "us": "us", "au": "au"},
    "ind": {"cn": "cn", "in": "in", "sea": "sea", "jp": "jp", "me": "me eg", "fr": "eu fr", "de": "eu de", "en": "eu en", "it": "eu it med",
            "ru": "ru", "af": "af", "latam": "latam meso andes am", "us": "us", "au": "au"},
    "mod": {"cn": "cn", "in": "in", "sea": "sea", "jp": "jp", "me": "me eg", "eu": "eu de fr it en", "ru": "ru", "af": "af",
            "latam": "latam meso andes am", "us": "us", "au": "au"},
}
g = sys.argv[1]
rows = [l.split("\t") for l in (A / "lines" / f"{g}.tsv").read_text(encoding="utf-8").split("\n") if l.strip() and not l.startswith("#")]
print(f"{'region':8}" + "".join(f"{k:>6}" for k in "cyajox") + "   (lines with no need that fit the region)")
for key, tags in REGIONS[g].items():
    t = set(tags.split())
    cells = []
    for k in "cyajox":
        n = sum(1 for c in rows if c[1] == k and (len(c) < 6 or not c[5].strip()) and (not c[2].strip() or set(c[2].split()) & t))
        cells.append(n)
    print(f"{key:8}" + "".join(f"{n:>6}" for n in cells))
