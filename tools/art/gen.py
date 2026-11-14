"""Asks Codex (its image generation tool) to paint every picture in manifest.py that tools/art/raw doesn't have yet.

    python tools/art/gen.py [--parallel 8] [--batch 8] [--only thing/anc.lamp,era/3]

Each Codex session paints a batch and prints `@@ <id> <path>` per picture; the PNGs are copied to tools/art/raw/<id>.png.
"""
import argparse
import os
import re
import shutil
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from manifest import jobs  # noqa: E402

RAW = Path(__file__).parent / "raw"
GEN = Path.home() / ".codex" / "generated_images"


def prompt(batch):
    lines = [
        "Use your image generation tool to create the following images, one at a time, in this order.",
        "Generate each exactly once, using the prompt verbatim. Do not run shell commands, do not edit or save files.",
        "Images whose id starts with era/ are landscape 1536x1024; all others are square 1024x1024 with a transparent background, unless the prompt itself says otherwise.",
        "Right after each image is generated, print one line exactly like: @@ <id> <absolute path of the generated png>",
        "",
    ]
    for i, (id_, p) in enumerate(batch, 1):
        lines.append(f"{i}. id: {id_}\n   prompt: {p}\n")
    return "\n".join(lines)


def run(batch):
    before = {p for p in GEN.rglob("*.png")} if GEN.exists() else set()
    exe = shutil.which("codex.cmd") or shutil.which("codex") or "codex"
    r = subprocess.run([exe, "exec", "--skip-git-repo-check", "-s", "read-only", "-"], input=prompt(batch),
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    out = r.stdout + r.stderr
    got = {}
    for m in re.finditer(r"@@\s+(\S+)\s+(.+?\.png)", out):
        p = Path(m.group(2).strip().strip("`"))
        if p.exists():
            got[m.group(1)] = p
    # Fallback: the session's new files, in the order they were made.
    sid = re.search(r"session id:\s*(\S+)", out)
    if sid and len(got) < len(batch):
        made = sorted((GEN / sid.group(1)).glob("*.png"), key=os.path.getmtime) if (GEN / sid.group(1)).exists() else []
        made = [p for p in made if p not in before and p not in got.values()]
        missing = [i for i, _ in batch if i not in got]
        if len(made) == len(missing):
            got.update(zip(missing, made))
    for id_, p in got.items():
        dst = RAW / f"{id_}.png"
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy(p, dst)
    lost = [i for i, _ in batch if i not in got]
    print(f"batch {batch[0][0]}..: {len(got)}/{len(batch)}" + (f" missing {lost}" if lost else ""), flush=True)
    if lost:
        (RAW / "logs").mkdir(parents=True, exist_ok=True)
        (RAW / "logs" / (batch[0][0].replace("/", "_") + ".log")).write_text(out, encoding="utf-8")
    return lost


def main():
    a = argparse.ArgumentParser()
    a.add_argument("--parallel", type=int, default=8)
    a.add_argument("--batch", type=int, default=8)
    a.add_argument("--only", default="")
    a.add_argument("--force", action="store_true")
    args = a.parse_args()
    only = set(filter(None, args.only.split(",")))
    todo = [(i, p) for i, p in jobs() if (not only or i in only) and (args.force or not (RAW / f"{i}.png").exists())]
    # The most visible first, so a usage limit hit halfway leaves the plates missing, not the icons.
    order = {"icon": 0, "pic": 1, "thing": 2, "era": 3}
    todo.sort(key=lambda j: order.get(j[0].split("/")[0], 9))
    print(f"{len(todo)} to paint", flush=True)
    batches = [todo[i:i + args.batch] for i in range(0, len(todo), args.batch)]
    with ThreadPoolExecutor(args.parallel) as ex:
        lost = sum(ex.map(run, batches), [])
    print(f"done, missing: {lost}" if lost else "done", flush=True)


if __name__ == "__main__":
    main()
