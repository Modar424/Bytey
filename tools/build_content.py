#!/usr/bin/env python3
"""Builds assets/questions/questions_{en,ar}.json and assets/concepts/concepts_{en,ar}.json
from tools/content_src/, then validates them with the same rules as ContentValidator.kt.

  python3 tools/build_content.py            # build + validate (exit 1 on any problem)
  python3 tools/build_content.py --partial  # tolerate an incomplete bank (work in progress)
"""
import importlib, json, pathlib, re, sys, collections

ROOT = pathlib.Path(__file__).resolve().parent.parent
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent / "content_src"))
import common  # noqa: E402

DIFF = [(1, 2), (2, 3), (3, 3), (3, 4), (4, 4), (4, 5), (5, 5), (5, 6), (6, 7), (7, 8)]
ARABIC = re.compile(r"[\u0600-\u06FF]")
partial = "--partial" in sys.argv

for n in range(1, 11):
    try:
        importlib.import_module(f"level{n:02d}")
    except ModuleNotFoundError as e:
        if e.name != f"level{n:02d}":
            raise
import concepts as cmod  # noqa: E402

problems = []
recs = common.RECORDS
recs.sort(key=lambda r: (r["level"], list(common.CATS.values()).index(r["category"]), r["id"]))
concept_ids = {c["id"] for c in cmod.CONCEPTS}

# ---- balance the position of the correct answer (deterministic; swaps the same two
# options in BOTH languages, so content and translation parity are untouched).
import random
_rng = random.Random(20261105)
_targets = [i % 4 for i in range(len(recs))]
_rng.shuffle(_targets)
for r, t in zip(recs, _targets):
    c = r["correctIndex"]
    if c != t:
        for lang in ("en", "ar"):
            o = r[lang]["options"]
            o[c], o[t] = o[t], o[c]
        r["correctIndex"] = t

# ---- questions
seen = set()
for r in recs:
    i = r["id"]
    if i in seen: problems.append(f"{i}: duplicate id")
    seen.add(i)
    for lang in ("en", "ar"):
        o = r[lang]["options"]
        if len(o) != 4: problems.append(f"{i}/{lang}: needs exactly 4 options")
        if len(set(o)) != len(o): problems.append(f"{i}/{lang}: duplicate options")
        if any(not s.strip() for s in o + [r[lang]["text"], r[lang]["explanation"]]):
            problems.append(f"{i}/{lang}: blank field")
    if r["correctIndex"] not in range(4): problems.append(f"{i}: correctIndex out of range")
    lo, hi = DIFF[r["level"] - 1]
    if not lo <= r["difficulty"] <= hi: problems.append(f"{i}: difficulty {r['difficulty']} not in {lo}-{hi}")
    if r["conceptId"] not in concept_ids: problems.append(f"{i}: unknown concept {r['conceptId']}")
    if ARABIC.search(r["en"]["text"] + "".join(r["en"]["options"]) + r["en"]["explanation"]):
        problems.append(f"{i}: Arabic letters inside the English record")
    if not ARABIC.search(r["ar"]["text"]) or not ARABIC.search(r["ar"]["explanation"]):
        problems.append(f"{i}: Arabic record has no Arabic text")
    key = r["en"]["text"].strip().lower()
    if key in seen: problems.append(f"{i}: duplicate English text")
    seen.add(key)

by_lc = collections.Counter((r["level"], r["category"]) for r in recs)
if not partial:
    if len(recs) != 250: problems.append(f"expected 250 questions, found {len(recs)}")
    for lv in range(1, 11):
        for c in common.CATS.values():
            if by_lc[(lv, c)] != 5: problems.append(f"level {lv} {c}: {by_lc[(lv, c)]} questions (expected 5)")

# ---- concepts
for c in cmod.CONCEPTS:
    n = len(c["res"])
    if not 3 <= n <= 5: problems.append(f"{c['id']}: {n} resources (need 3-5)")
    for res in c["res"]:
        if not res["url"].startswith("https://"): problems.append(f"{c['id']}: non-https url {res['url']}")
        if res["type"] not in ("ARTICLE", "VIDEO", "DOCUMENTATION", "PRACTICE", "COURSE"): problems.append(f"{c['id']}: bad type {res['type']}")
        if res["language"] not in ("AR", "EN"): problems.append(f"{c['id']}: bad language")
    if c["minutes"] <= 0: problems.append(f"{c['id']}: minutes")
    for lang in ("en", "ar"):
        for k in ("name", "description", "practice"):
            if not c[lang][k].strip(): problems.append(f"{c['id']}/{lang}: blank {k}")
    if ARABIC.search(c["en"]["name"] + c["en"]["description"] + c["en"]["practice"]):
        problems.append(f"{c['id']}: Arabic letters in English concept")
    if not ARABIC.search(c["ar"]["name"] + c["ar"]["description"]):
        problems.append(f"{c['id']}: Arabic concept has no Arabic text")
if len(cmod.CONCEPTS) < 25: problems.append(f"only {len(cmod.CONCEPTS)} concepts (need >= 25)")
if len({c['id'] for c in cmod.CONCEPTS}) != len(cmod.CONCEPTS): problems.append("duplicate concept ids")

# ---- write
def q_json(lang):
    return [{"id": r["id"], "level": r["level"], "category": r["category"], "conceptId": r["conceptId"],
             "text": r[lang]["text"], "options": r[lang]["options"], "correctIndex": r["correctIndex"],
             "explanation": r[lang]["explanation"], "difficulty": r["difficulty"]} for r in recs]

def c_json(lang):
    out = []
    for c in cmod.CONCEPTS:
        d = {"id": c["id"], "name": c[lang]["name"], "description": c[lang]["description"],
             "resources": [{"url": x["url"], "type": x["type"], "language": x["language"],
                            "title": x[lang][0], "description": x[lang][1]} for x in c["res"]],
             "practiceChallenge": c[lang]["practice"], "estimatedMinutes": c["minutes"]}
        if c[lang].get("advanced"): d["advancedChallenge"] = c[lang]["advanced"]
        out.append(d)
    return out

assets = ROOT / "app/src/main/assets"
(assets / "questions").mkdir(parents=True, exist_ok=True)
(assets / "concepts").mkdir(parents=True, exist_ok=True)
for lang in ("en", "ar"):
    (assets / f"questions/questions_{lang}.json").write_text(json.dumps(q_json(lang), ensure_ascii=False, indent=1), encoding="utf-8")
    (assets / f"concepts/concepts_{lang}.json").write_text(json.dumps(c_json(lang), ensure_ascii=False, indent=1), encoding="utf-8")

# ---- report
print(f"questions: {len(recs)}  concepts: {len(cmod.CONCEPTS)}")
print("correctIndex distribution:", dict(sorted(collections.Counter(r['correctIndex'] for r in recs).items())))
used = collections.Counter(r["conceptId"] for r in recs)
print("questions per concept:", dict(sorted(used.items())))
print("concepts with no question:", sorted(concept_ids - set(used)))
print("PROBLEMS:" if problems else "OK - no problems")
for p in problems: print("  -", p)
sys.exit(1 if problems and not partial else 0)
