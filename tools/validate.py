"""Checks Lot's content files (see tools/CONTENT.md). Prints counts and OK, or the problems found.

    python tools/validate.py            all files
    python tools/validate.py anc        one group's lines plus history and dilemmas
"""
import collections
import re
import sys
from pathlib import Path

A = Path(__file__).resolve().parent.parent / "app/src/main/assets"
GROUPS = ["stone", "anc", "med", "early", "ind", "mod"]
KINDS = set("cyajox")
TAGS = {"af", "me", "eg", "med", "eu", "fr", "de", "it", "en", "ru", "in", "cn", "jp", "sea", "meso", "andes", "am", "latam", "us", "au"}
NEED = {"m", "f", "r0", "r1", "r2", "r3", "lit", "illit", "married", "single", "kids", "monk", "town", "widowed", "abroad", "sea"}
SHUT = {"m", "f", "r0", "r1", "r2", "r3", "lit", "!lit", "poor", "rich", "married", "!married", "kids", "!kids", "monk", "town"}
FX = re.compile(r"^(wealth[+-][12]|risk([5-9]|[1-5][0-9]|60)|lit|sea|kind|love|town|moved|free|faith|brave|kids[+-]1|married)$")
MARK = re.compile(r"\{[^{}|]*\|[^{}|]*\}")
BAR = re.compile(r"\|(?![^{]*\})")
errors = []


def err(where, msg):
    errors.append(f"{where}: {msg}")


def text(where, s, lang):
    if not s.strip():
        err(where, f"empty {lang}")
    if "\t" in s:
        err(where, "tab inside text")
    rest = MARK.sub("", s)
    if "{" in rest or "}" in rest or "|" in rest:
        err(where, f"broken {{m|f}} mark in {lang}: {s}")
    if len(MARK.sub("x", s)) > 220:
        err(where, f"{lang} longer than 220 chars")


def lines(group):
    p = A / "lines" / f"{group}.tsv"
    count = collections.Counter()
    seen = set()
    for n, row in enumerate(p.read_text(encoding="utf-8").split("\n"), 1):
        if not row.strip() or row.startswith("#"):
            continue
        w = f"lines/{group}.tsv:{n}"
        c = row.split("\t")
        if len(c) == 5:
            c.append("")
        if len(c) != 6:
            err(w, f"{len(c)} columns, want 6"); continue
        g, k, tags, ru, en, need = c
        if g != group:
            err(w, f"group {g!r} in the {group} file")
        if k not in KINDS:
            err(w, f"kind {k!r}")
        for t in tags.split():
            if t not in TAGS:
                err(w, f"tag {t!r}")
        for t in need.split():
            if t.lstrip("!+") not in NEED and not re.match(r"^(after|before):-?\d+$", t):
                err(w, f"need {t!r}")
        text(w, ru, "ru"); text(w, en, "en")
        if ru in seen:
            err(w, "duplicate line")
        seen.add(ru)
        count[k] += 1
    return count


def history():
    p = A / "history.tsv"
    n_rows = 0
    meets = 0
    for n, row in enumerate(p.read_text(encoding="utf-8").split("\n"), 1):
        if not row.strip() or row.startswith("#"):
            continue
        w = f"history.tsv:{n}"
        c = row.split("\t")
        if len(c) != 6:
            err(w, f"{len(c)} columns, want 6"); continue
        tags, a, b, meet, ru, en = c
        for t in tags.split():
            if t != "*" and t not in TAGS:
                err(w, f"tag {t!r}")
        try:
            if int(a) > int(b):
                err(w, "from > to")
        except ValueError:
            err(w, "years must be integers")
        if meet not in ("0", "1"):
            err(w, "meet must be 0 or 1")
        meets += meet == "1"
        text(w, ru, "ru"); text(w, en, "en")
        n_rows += 1
    return n_rows, meets


def pair(w, s, parts=2):
    c = [x.strip() for x in BAR.split(s)]
    if len(c) != parts:
        err(w, f"want {parts} parts separated by |, got {len(c)}: {s}")
        return None
    return c


def dilemmas():
    src = "\n\n".join(f.read_text(encoding="utf-8") for f in sorted(A.glob("dilemmas*.txt")))
    blocks = [b for b in re.split(r"\n\s*\n", src) if b.strip() and not all(l.startswith("#") for l in b.strip().split("\n"))]
    ids = set()
    per = collections.Counter()
    for b in blocks:
        rows = [l.rstrip() for l in b.strip().split("\n") if not l.startswith("#")]
        head = {}
        doors = []
        for r in rows:
            key, _, val = r.partition(":")
            key, val = key.strip(), val.strip()
            if key == "door":
                doors.append({"text": val, "shut": [], "out": [], "ep": None, "ask": None})
            elif key in ("shut", "out", "ep", "ask") and doors:
                if key in ("ep", "ask"):
                    doors[-1][key] = val
                else:
                    doors[-1][key].append(val)
            else:
                head[key] = val
        w = f"dilemmas.txt [{head.get('id', '?')}]"
        for k in ("id", "groups", "tags", "ages", "q"):
            if k not in head:
                err(w, f"missing {k}")
        if head.get("id") in ids:
            err(w, "duplicate id")
        ids.add(head.get("id"))
        gs = head.get("groups", "").split()
        for g in gs:
            if g not in GROUPS:
                err(w, f"group {g!r}")
            per[g] += 1
        for t in head.get("tags", "").split():
            if t != "*" and t not in TAGS:
                err(w, f"tag {t!r}")
        for t in head.get("need", "").split():
            if t.lstrip("!") not in NEED and not re.match(r"^(after|before):-?\d+$", t):
                err(w, f"need {t!r}")
        if not re.match(r"^\d+-\d+$", head.get("ages", "")):
            err(w, "ages must look like 20-55")
        try:
            float(head.get("weight", "1"))
        except ValueError:
            err(w, "weight must be a number")
        q = pair(w, head.get("q", ""))
        if q:
            text(w, q[0], "ru"); text(w, q[1], "en")
        if not 2 <= len(doors) <= 3:
            err(w, f"{len(doors)} doors, want 2 or 3")
        if not any(not d["shut"] for d in doors):
            err(w, "every door can be shut; leave at least one door with no shut line")
        for d in doors:
            t = pair(w, d["text"])
            if t:
                text(w, t[0], "ru"); text(w, t[1], "en")
            for s in d["shut"]:
                c = pair(w, s, 3)
                if c:
                    for cond in c[0].split():
                        if cond not in SHUT:
                            err(w, f"shut cond {cond!r}")
                    text(w, c[1], "ru"); text(w, c[2], "en")
            if not d["out"]:
                err(w, f"door without out: {d['text']}")
            for o in d["out"]:
                c = [x.strip() for x in BAR.split(o)]
                if len(c) == 3:
                    c.append("")
                if len(c) != 4:
                    err(w, f"out wants weight | ru | en | effects: {o}"); continue
                try:
                    float(c[0])
                except ValueError:
                    err(w, f"out weight {c[0]!r}")
                text(w, c[1], "ru"); text(w, c[2], "en")
                for fx in c[3].split():
                    if not FX.match(fx):
                        err(w, f"effect {fx!r}")
            for k in ("ep", "ask"):
                if d[k]:
                    c = pair(w, d[k])
                    if c:
                        text(w, c[0], "ru"); text(w, c[1], "en")
    return len(blocks), per


only = sys.argv[1:] or GROUPS
for g in only:
    if (A / "lines" / f"{g}.tsv").exists():
        c = lines(g)
        print(f"{g:6} " + " ".join(f"{k}={c[k]}" for k in "cyajox") + f"  total={sum(c.values())}")
h, m = history()
print(f"history {h} rows, {m} encounters")
d, per = dilemmas()
print(f"dilemmas {d}: " + " ".join(f"{g}={per[g]}" for g in GROUPS))
if errors:
    print(f"\n{len(errors)} problems:")
    for e in errors[:200]:
        print(" -", e)
    sys.exit(1)
print("OK")
