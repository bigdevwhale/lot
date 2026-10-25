"""Looks for lines and fork outcomes whose words assume something about the life that their `need` or effects don't say.

    python tools/audit.py            the counts per check
    python tools/audit.py spouse     every suspect line of one check

Each check is a pattern in the Russian text and what the line should carry: a need (married, kids, lit, f, town...) or a
mark it leaves (+town, +widowed, +abroad). A suspect is a line that matches the pattern and lacks it.
"""
import glob
import re
import sys
from pathlib import Path

A = Path(__file__).resolve().parent.parent / "app/src/main/assets"

# name: (pattern, the token the line should have, kinds it applies to)
CHECKS = {
    "spouse": (r"\b(жена|жены|жене|женой|муж|мужа|мужу|мужем|супруг|свёкр|свекров|тёщ|тест[ьяю]|невестк|золовк)\b|\{жен[ауы]?\|муж", "married", "ajox"),
    "widowed": (r"овдовел|овдовела|\{Овдовел|похоронил[аи]? (жену|мужа)|\{Похоронил\|Похоронила\} \{жену\|мужа\}|\{Жена умерла\|Муж умер\}|вдов[аеуы]?\b|вдовец", "+widowed", "ajox"),
    "kids": (r"\b(сын|сына|сыну|сыном|дочь|дочери|дочка|дочку|детей|дети|детям|ребёнок|ребёнка|внук|внуки|внуков|внучк|правнук)", "kids", "ajo"),
    "lit": (r"\b(читаешь|прочёл|прочла|прочитал|пишешь письмо|написал[аи]? письмо|по книге|в книге|грамот[ау]|расписал|расписываешься|газету читаешь)", "lit", "yajo"),
    "female": (r"(беремен|родила|рожаешь|роды|кормишь грудью|грудью|повитух|выкидыш|сватают тебя за|выдали замуж|выдают замуж)", "f", "yajox"),
    "town": (r"(завод|фабрик|цех|гудок|трамва|метро|автобус до работы|офис|контор|смен[уаы] на|общежит|квартир|коммунал|подъезд|лифт|этаж)", "+town", "yajo"),
    "abroad": (r"(эмигр|за океан|в Америк[уе]|в Аргентин|на корабле в|переехал[аи]? в (другую|чужую) стран|в Германию на заработки|в Залив|в Дубай)", "+abroad", "yajo"),
}
# A line that carries any of these is fine for that check.
OK = {
    "spouse": {"married", "+widowed"},
    "widowed": {"+widowed"},
    "kids": {"kids"},
    "lit": {"lit"},
    "female": {"f"},
    "town": {"town", "+town", "!town"},
    "abroad": {"+abroad"},
}


def lines():
    for f in sorted(glob.glob(str(A / "lines" / "*.tsv"))):
        for n, r in enumerate(open(f, encoding="utf-8").read().split("\n"), 1):
            if not r or r.startswith("#"):
                continue
            c = r.split("\t") + [""] * 6
            yield Path(f).name, n, c[1], c[3], set(c[5].split())


def outcomes():
    for f in sorted(glob.glob(str(A / "dilemmas*.txt"))):
        did = None
        need = set()
        for n, r in enumerate(open(f, encoding="utf-8").read().split("\n"), 1):
            if r.startswith("id:"):
                did = r[3:].strip()
            if r.startswith("need:"):
                need = set(r[5:].split())
            if r.startswith("out:"):
                p = [x.strip() for x in re.split(r"\|(?![^{]*\})", r[4:])]
                yield Path(f).name, n, did, p[1], set((p[3] if len(p) > 3 else "").split()), need


def suspects(check):
    pat, want, kinds = CHECKS[check]
    rx = re.compile(pat, re.I)
    for f, n, k, ru, need in lines():
        if k in kinds and rx.search(ru) and not (need & OK[check]):
            yield f"{f}:{n} [{k}] {' '.join(sorted(need))} | {ru}"


# Fork outcomes whose words change the life but whose effects don't.
FX = {
    "fork-town": (r"(в город|в столиц|на завод|на фабрик|в Шэньчжэнь|в Найроби|в Лагос|в Мумбаи)", "town"),
    "fork-married": (r"(свадьб|женился|вышла замуж|\{Женился\|Вышла замуж\}|обвенчал)", "married"),
    "fork-kids": (r"(ребёнок умер|сын умер|дочь умерла|похоронил[аи]? (сына|дочь|ребёнка)|не стало (сына|дочери|ребёнка))", "kids-1"),
    "fork-lit": (r"(научил[аи]?сь читать|\{научился\|научилась\} читать|выучил[аи]?сь грамоте|читаешь по слогам)", "lit"),
    "fork-sea": (r"(увидел[аи]? море|\{увидел\|увидела\} море|впервые море)", "sea"),
    "fork-abroad": (r"(за океан|в Америк|в Аргентин|в чужую страну|в Залив)", "moved"),
}


def fork_suspects(check):
    pat, fx = FX[check]
    rx = re.compile(pat, re.I)
    for f, n, did, ru, effects, need in outcomes():
        if rx.search(ru) and fx not in effects:
            yield f"{f}:{n} [{did}] {' '.join(sorted(effects))} | {ru}"


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")
    if len(sys.argv) > 1:
        c = sys.argv[1]
        for s in (suspects(c) if c in CHECKS else fork_suspects(c)):
            print(s)
    else:
        for c in CHECKS:
            print(f"line  {c:8} {sum(1 for _ in suspects(c))}")
        for c in FX:
            print(f"fork  {c:8} {sum(1 for _ in fork_suspects(c))}")
