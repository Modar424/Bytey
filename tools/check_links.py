#!/usr/bin/env python3
"""Checks every resource URL in assets/concepts/concepts_en.json. Needs internet.
Exit 1 if any URL is definitely broken (404/410/DNS/connection error).
403/429/5xx are reported as 'uncertain' (many sites block bots) and do not fail the run."""
import json, pathlib, sys, urllib.request, urllib.error, concurrent.futures as cf

ROOT = pathlib.Path(__file__).resolve().parent.parent
data = json.loads((ROOT / "app/src/main/assets/concepts/concepts_en.json").read_text(encoding="utf-8"))
urls = sorted({r["url"] for c in data for r in c["resources"]})
UA = {"User-Agent": "Mozilla/5.0 (compatible; BytewiseLinkCheck/1.0)"}

def check(u):
    for method in ("HEAD", "GET"):
        try:
            with urllib.request.urlopen(urllib.request.Request(u, headers=UA, method=method), timeout=25) as r:
                return u, "OK", r.status
        except urllib.error.HTTPError as e:
            if method == "HEAD" and e.code in (400, 403, 404, 405, 501):
                continue
            return u, ("BROKEN" if e.code in (404, 410) else "UNCERTAIN"), e.code
        except Exception as e:  # DNS, TLS, timeout
            if method == "HEAD":
                continue
            return u, "BROKEN", str(e)[:80]
    return u, "UNCERTAIN", "?"

with cf.ThreadPoolExecutor(8) as ex:
    results = list(ex.map(check, urls))
for u, s, c in results:
    print(f"{s:9} {c!s:>4}  {u}")
bad = [r for r in results if r[1] == "BROKEN"]
print(f"\n{len(urls)} urls: {sum(r[1]=='OK' for r in results)} ok, {sum(r[1]=='UNCERTAIN' for r in results)} uncertain, {len(bad)} broken")
sys.exit(1 if bad else 0)
