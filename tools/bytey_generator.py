#!/usr/bin/env python3
"""
Bytey sprite generator (source of truth for all Bytey pixel art).

Every frame is drawn pixel by pixel on a 64x64 grid with a fixed palette:
no anti-aliasing, no resampling, transparent background. Output (nearest-neighbour safe):
  assets/bytey/sprites/tier<N>/<state>.png   horizontal sheets, equal 64x64 frames
  assets/bytey/static/tier<N>/<state>.png    single key frame per state (static fallback)
Tiers = cosmetic progression: 0 basic, 1 glasses (lvl 3), 2 +headphones (lvl 5),
3 +cape (lvl 7), 4 +crown (lvl 10).
Usage: python3 tools/bytey_generator.py [output_assets_dir] [--preview file.png]
"""
import os, random, sys
from PIL import Image

F = 64
OY = 4  # head-room so jumps and the crown never clip

def rgb(h): return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5)) + (255,)
C = {k: rgb(v) for k, v in dict(
    outline="#0B1020", body="#2DD4BF", hi="#6EECD6", shade="#14A096", belly="#D9F7F1", belly2="#A7E3D8",
    spike="#8B5CF6", spike_hi="#A78BFA", white="#FFFFFF", mouth="#7F1D4A", tongue="#F472B6", blush="#FB7185",
    gold="#FBBF24", gold_hi="#FDE68A", gold_dark="#B45309", red="#EF4444", tear="#60A5FA", frame="#1E293B",
    lens="#BFDBFE", hp="#7C3AED", hp_hi="#A78BFA", hp_dark="#4C1D95", cape="#F59E0B", cape2="#D97706",
    cyan="#22D3EE", navy="#1E1B4B", dust="#CBD5E1", gray="#94A3B8", green="#4ADE80", pink="#F472B6",
).items()}

def circle(cx, cy, r):
    return [(x, y) for y in range(cy - r, cy + r + 1) for x in range(cx - r, cx + r + 1)
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r + r // 2]

def pattern(img, x, y, rows, colors):
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch in colors: img[(x + i, y + j)] = colors[ch]

QMARK = [".XXX.", "X...X", "....X", "..XX.", "..X..", ".....", "..X.."]
HEART = [".X.X.", "XXXXX", "XXXXX", ".XXX.", "..X.."]

DEFAULT = dict(dx=0, dy=0, hx=0, hy=0, eyes="open", mouth="smile", arm="down", legs=((0, 0), (0, 0)),
               wag=0, fx=(), blush=True, cape=0)

def render(pose, tier):
    P = dict(DEFAULT); P.update(pose)
    dx, dy = P["dx"], P["dy"] + OY
    hx, hy = dx + P["hx"], dy + P["hy"]
    base = {}

    def fill(x0, y0, x1, y1, name, ox, oy, rnd=True):
        for y in range(y0 + oy, y1 + oy + 1):
            for x in range(x0 + ox, x1 + ox + 1):
                if rnd and x in (x0 + ox, x1 + ox) and y in (y0 + oy, y1 + oy): continue
                if 0 <= x < F and 0 <= y < F: base[(x, y)] = name

    # cape (behind everything)
    if tier >= 3:
        w = P["cape"]
        fill(9, 29, 22, 50, "cape", dx, dy, True)
        for i, yy in enumerate(range(47, 52)):
            for x in range(9 + dx, 23 + dx):
                if (x + i + w) % 4 in (0, 1) and yy > 49 + dy - 3: base[(x, yy + dy)] = "cape"
        for x in range(9 + dx, 23 + dx):  # wavy hem
            if (x + w) % 4 in (2, 3): base.pop((x, 50 + dy), None)
    # tail
    wg = P["wag"]
    fill(11, 33, 18, 42, "body", dx, dy)
    fill(6, 36 + wg, 11, 42 + wg, "body", dx, dy)
    fill(2, 38 + 2 * wg, 6, 42 + 2 * wg, "body", dx, dy, False)
    # legs
    for (lx, (xo, lift)) in zip((20, 30), P["legs"]):
        bottom = 56 - lift
        fill(lx + xo, 46, lx + xo + 5, bottom, "body", dx, dy, False)
        fill(lx + xo, bottom - 2, lx + xo + 8, bottom, "body", dx, dy)
    # torso + head
    fill(17, 28, 38, 46, "body", dx, dy)
    fill(24, 8, 48, 30, "body", hx - dx + dx, hy - dy + dy) if False else None
    fill(24, 8, 48, 30, "body", hx, hy)
    fill(48, 17, 52, 28, "body", hx, hy)
    # spikes
    def spike(x, y, ox, oy):
        base[(x + ox, y + oy)] = "spike_hi"
        for xx in range(x - 1, x + 2):
            for yy in (y + 1, y + 2): base[(xx + ox, yy + oy)] = "spike"
    spike(14, 30, dx, dy); spike(9, 33, dx, dy); spike(19, 25, dx, dy)
    spike(27, 5, hx, hy); spike(33, 3, hx, hy); spike(39, 4, hx, hy)

    img = {}
    for (x, y), n in base.items():
        if n == "body":
            if (x, y - 1) not in base: n = "hi"
            elif (x, y + 1) not in base: n = "shade"
        elif n == "cape":
            n = "cape" if (x + y) % 5 else "cape2"
        img[(x, y)] = C[n]
    for (x, y) in list(base):  # outline
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if (nx, ny) not in base and 0 <= nx < F and 0 <= ny < F: img[(nx, ny)] = C["outline"]

    # belly
    for y in range(34 + dy, 46 + dy):
        for x in range(25 + dx, 37 + dx):
            if base.get((x, y)) == "body" and not (x in (25 + dx, 36 + dx) and y in (34 + dy, 45 + dy)):
                img[(x, y)] = C["belly2"] if (y - dy) % 3 == 0 else C["belly"]

    # face (relative to head offset)
    def eye(ex, kind):
        ey = 14
        X, Y = ex + hx, ey + hy
        def r(x0, y0, x1, y1, c, rnd=False):
            for yy in range(y0, y1 + 1):
                for xx in range(x0, x1 + 1):
                    if rnd and xx in (x0, x1) and yy in (y0, y1): continue
                    img[(xx, yy)] = c
        if kind in ("open", "think", "sad", "star"):
            r(X, Y, X + 3, Y + 6, C["outline"], True)
            hxo = 1 if kind == "think" else 0
            r(X + hxo, Y + 1, X + hxo + 1, Y + 2, C["white"])
            img[(X + 3 - hxo, Y + 5)] = C["white"]
            if kind == "star": r(X + 2, Y + 4, X + 2, Y + 4, C["white"]); img[(X + 1, Y + 5)] = C["white"]
            if kind == "sad": r(X - 1, Y - 2, X + 4, Y - 2, C["outline"]); img[(X - 1, Y - 1)] = C["outline"] if ex < 38 else C["body"]; img[(X + 4, Y - 1)] = C["outline"] if ex > 38 else C["body"]
            if kind == "think": r(X - 1, Y - 3, X + 4, Y - 3, C["outline"])
        elif kind == "blink":
            r(X, Y + 3, X + 3, Y + 3, C["outline"])
        elif kind == "happy":
            r(X + 1, Y + 2, X + 2, Y + 2, C["outline"]); img[(X, Y + 3)] = C["outline"]; img[(X + 3, Y + 3)] = C["outline"]
        elif kind == "squint":
            if ex < 38: pts = [(0, 1), (1, 2), (2, 3), (1, 4), (0, 5)]
            else: pts = [(3, 1), (2, 2), (1, 3), (2, 4), (3, 5)]
            for a, b in pts: img[(X + a, Y + b)] = C["outline"]
        elif kind == "wide":
            r(X - 1, Y - 1, X + 4, Y + 7, C["white"], True)
            for xx in range(X - 2, X + 6):
                for yy in range(Y - 2, Y + 9):
                    if (xx, yy) not in img or img[(xx, yy)] == C["body"] or img[(xx, yy)] == C["hi"]: pass
            r(X, Y + 1, X + 3, Y + 6, C["white"], True)
            r(X + 1, Y + 2, X + 2, Y + 4, C["outline"])
            for xx in range(X - 1, X + 5):
                for yy in (Y - 2, Y + 7):
                    img.setdefault((xx, yy), C["outline"])
    for ex in (31, 42): eye(ex, P["eyes"])
    if P["blush"] and P["eyes"] not in ("wide",):
        for (bx, by) in ((29, 22), (44, 22)):
            for yy in range(by + hy, by + hy + 2):
                for xx in range(bx + hx, bx + hx + 3): img[(xx, yy)] = C["blush"]
    img[(50 + hx, 21 + hy)] = C["outline"]
    # mouth
    m = P["mouth"]; mx, my = 34 + hx, 24 + hy
    O, M, T, W = C["outline"], C["mouth"], C["tongue"], C["white"]
    def mr(x0, y0, x1, y1, c):
        for yy in range(y0, y1 + 1):
            for xx in range(x0, x1 + 1): img[(mx + xx, my + yy)] = c
    if m == "smile":
        mr(1, 1, 6, 1, O); mr(0, 0, 0, 0, O); mr(7, 0, 7, 0, O)
    elif m in ("open", "big", "laugh"):
        h = 3 if m == "open" else 5
        mr(0, 0, 7, h, O); mr(1, 1, 6, h - 1, M); mr(2, h - 1, 5, h - 1, T); mr(1, 1, 2, 1, W); mr(5, 1, 6, 1, W)
    elif m == "frown":
        mr(1, 1, 6, 1, O); mr(0, 2, 0, 2, O); mr(7, 2, 7, 2, O)
    elif m == "o":
        mr(3, 0, 5, 3, O); mr(4, 1, 4, 2, M)
    elif m == "flat":
        mr(2, 1, 6, 1, O)

    # arm (front layer with own outline)
    arm = {}
    a = P["arm"]
    def ar(x0, y0, x1, y1):
        for yy in range(y0, y1 + 1):
            for xx in range(x0, x1 + 1): arm[(xx + dx, yy + dy)] = C["body"]
    if a == "down": ar(37, 34, 41, 36)
    elif a == "up": ar(38, 34, 46, 36); ar(44, 26, 46, 34); ar(43, 24, 47, 26)
    elif a == "wave": ar(38, 34, 45, 36); ar(44, 27, 46, 34); ar(46, 25, 48, 28)
    elif a == "point": ar(38, 34, 50, 36); ar(50, 33, 53, 37)
    elif a == "chin": ar(38, 34, 44, 36); ar(43, 27, 45, 34); ar(42, 26, 46, 28)
    elif a == "cheer": ar(38, 34, 50, 36); ar(50, 28, 52, 36); ar(49, 27, 53, 29)
    for (x, y) in list(arm):
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if (nx, ny) not in arm: img[(nx, ny)] = C["outline"]
    for k, v in arm.items(): img[k] = v
    for (x, y) in arm:
        if (x, y - 1) not in arm: img[(x, y)] = C["hi"]

    # accessories
    acc = {}
    if tier >= 1:  # programmer glasses
        for ex in (31, 42):
            x0, y0, x1, y1 = ex - 2 + hx, 12 + hy, ex + 5 + hx, 22 + hy
            for yy in range(y0, y1 + 1):
                for xx in range(x0, x1 + 1):
                    if (xx in (x0, x1) or yy in (y0, y1)) and not (xx in (x0, x1) and yy in (y0, y1)):
                        img[(xx, yy)] = C["frame"]
            img[(x0 + 1, y0 + 1)] = C["lens"]; img[(x0 + 2, y0 + 1)] = C["lens"]
        for xx in range(37 + hx, 40 + hx): img[(xx, 15 + hy)] = C["frame"]
    if tier >= 2:  # purple headphones
        for (x, y) in [(23, 12), (23, 10), (24, 8), (25, 6), (27, 4), (30, 3), (36, 2), (42, 3), (45, 5), (47, 7), (48, 9)]:
            for ox in (0, 1):
                acc[(x + hx + ox, y + hy)] = C["hp"]; acc[(x + hx + ox, y + hy + 1)] = C["hp_hi"] if ox == 0 else C["hp"]
        for yy in range(12, 23):
            for xx in range(20, 25):
                if not (xx in (20, 24) and yy in (12, 22)):
                    acc[(xx + hx, yy + hy)] = C["hp_dark"] if xx in (20, 24) or yy in (12, 22) else C["hp"]
        for yy in range(15, 20): acc[(22 + hx, yy + hy)] = C["cyan"]
        for yy in range(8, 15):
            for xx in range(47, 51):
                acc[(xx + hx, yy + hy)] = C["hp_dark"] if xx in (47, 50) else C["hp"]
    if tier >= 4:  # legendary crown
        for xx in range(29, 40):
            for yy in (7, 8, 9): acc[(xx + hx, yy + hy)] = C["gold"] if yy < 9 else C["gold_dark"]
        for (x0, x1, y0) in ((29, 30, 4), (33, 35, 2), (38, 39, 4)):
            for xx in range(x0, x1 + 1):
                for yy in range(y0, 7): acc[(xx + hx, yy + hy)] = C["gold_hi"] if yy == y0 else C["gold"]
        acc[(34 + hx, 8 + hy)] = C["red"]; acc[(30 + hx, 8 + hy)] = C["cyan"]; acc[(38 + hx, 8 + hy)] = C["cyan"]
    if tier >= 2 and tier >= 3:  # cape clasp
        for xx in range(19 + dx, 22 + dx):
            for yy in range(28 + dy, 31 + dy): acc[(xx, yy)] = C["gold"]
        acc[(20 + dx, 29 + dy)] = C["red"]
    if tier >= 2:
        for (x, y) in list(acc):
            for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                if (nx, ny) not in acc and 0 <= nx < F and 0 <= ny < F: img[(nx, ny)] = C["outline"]
    elif tier >= 4:
        pass
    img.update(acc)

    # effects
    for fx in P["fx"]: fx(img)
    return {k: v for k, v in img.items() if 0 <= k[0] < F and 0 <= k[1] < F}

# ---- effects ----
def sparkle(x, y, big=False):
    def f(img):
        img[(x, y)] = C["white"]
        for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)): img[(x + a, y + b)] = C["gold_hi"]
        if big:
            for a, b in ((2, 0), (-2, 0), (0, 2), (0, -2)): img[(x + a, y + b)] = C["gold"]
    return f
def heart(x, y): return lambda img: pattern(img, x, y, HEART, {"X": C["red"]})
def qmark(x, y): return lambda img: pattern(img, x, y, QMARK, {"X": C["gold"]})
def excl(x, y):
    def f(img):
        for yy in range(y, y + 5):
            for xx in (x, x + 1): img[(xx, yy)] = C["red"]
        for xx in (x, x + 1):
            for yy in (y + 6, y + 7): img[(xx, yy)] = C["red"]
    return f
def drop(x, y, c="tear"):
    def f(img):
        img[(x, y)] = C[c]; img[(x - 1, y + 1)] = C[c]; img[(x, y + 1)] = C[c]; img[(x + 1, y + 1)] = C[c]; img[(x, y + 2)] = C[c]
    return f
def dots(x, y, n):
    def f(img):
        for i in range(n):
            for a in (0, 1):
                for b in (0, 1): img[(x + i * 4 + a, y + b)] = C["gold"]
    return f
def bulb(x, y):
    def f(img):
        for p in circle(x, y, 3): img[p] = C["gold"]
        for p in ((x - 1, y - 1), (x, y - 2)): img[p] = C["gold_hi"]
        for yy in (y + 4, y + 5):
            for xx in range(x - 1, x + 2): img[(xx, yy)] = C["gray"]
        for (a, b) in ((0, -6), (-5, -4), (5, -4), (-6, 0), (6, 0)): img[(x + a, y + b)] = C["gold_hi"]
    return f
def speed(y, n, length=9):
    def f(img):
        for i in range(n):
            yy = y + i * 5
            for xx in range(0, length - i % 3 * 2): img[(xx + 1 + i % 2 * 2, yy)] = C["cyan"] if i % 2 else C["white"]
    return f
def bolt(x, y):
    def f(img):
        for (a, b) in ((2, 0), (1, 1), (0, 2), (1, 2), (2, 2), (1, 3), (0, 4)): img[(x + a, y + b)] = C["gold"]
        img[(x + 1, y + 2)] = C["gold_hi"]
    return f
def dust(x, y, s=1):
    def f(img):
        for (a, b) in ((0, 0), (1, 0), (0, 1), (1, 1), (2, 1)): img[(x + a, y - b * s)] = C["dust"]
    return f
def confetti(seed, n=16):
    def f(img):
        rnd = random.Random(seed)
        cols = [C["gold"], C["pink"], C["cyan"], C["green"], C["hp_hi"], C["red"]]
        for _ in range(n):
            x, y = rnd.randrange(3, 60), rnd.randrange(0, 32)
            c = rnd.choice(cols)
            for a, b in ((0, 0), (1, 0), (0, 1), (1, 1)): img.setdefault((x + a, y + b), c)
    return f
def trophy(x, y):
    def f(img):
        for yy in range(y, y + 6):
            for xx in range(x, x + 7):
                if not (xx in (x, x + 6) and yy in (y, y + 5)) and not (yy == y + 5 and xx in (x + 1, x + 5)): img[(xx, yy)] = C["gold"]
        for yy in range(y + 1, y + 4): img[(x - 1, yy)] = C["gold_dark"]; img[(x + 7, yy)] = C["gold_dark"]
        img[(x + 1, y + 1)] = C["gold_hi"]; img[(x + 1, y + 2)] = C["gold_hi"]
        for yy in (y + 6, y + 7): img[(x + 3, yy)] = C["gold_dark"]
        for xx in range(x + 1, x + 6): img[(xx, y + 8)] = C["gold_dark"]
    return f
def magnifier(cx, cy):
    def f(img):
        ring = circle(cx, cy, 5); inner = set(circle(cx, cy, 3))
        for p in ring:
            img[p] = C["lens"] if p in inner else C["frame"]
        img[(cx - 2, cy - 2)] = C["white"]; img[(cx - 1, cy - 2)] = C["white"]
        for i in range(5): img[(cx - 4 - i, cy + 4 + i)] = C["gold_dark"]; img[(cx - 3 - i, cy + 4 + i)] = C["gold"]
    return f
def portal(cx, cy, rx, ry, seed=0):
    def f(img):
        for y in range(cy - ry - 1, cy + ry + 2):
            for x in range(cx - rx - 1, cx + rx + 2):
                d = ((x - cx) / (rx + 1)) ** 2 + ((y - cy) / (ry + 1)) ** 2
                if d <= 1.0: img[(x, y)] = C["navy"] if d < 0.45 else (C["cyan"] if d < 0.72 else C["hp"])
        rnd = random.Random(seed)
        for _ in range(4):
            x, y = cx + rnd.randint(-rx, rx - 1), cy + rnd.randint(-ry + 2, ry - 2)
            img[(x, y)] = C["white"]
    return f

R = lambda a, b: ((a[0], a[1]), (b[0], b[1]))
STAND = ((0, 0), (0, 0)); TUCK = ((1, 4), (1, 4))

def states():
    S = {}
    S["idle"] = ([dict(wag=0), dict(wag=1, hy=0), dict(wag=0, hy=1), dict(wag=-1, eyes="blink", hy=0)], 4, True, 0)
    S["happy"] = ([dict(eyes="happy", mouth="open", fx=[sparkle(9, 12)]),
                   dict(dy=-3, legs=TUCK, arm="up", eyes="happy", mouth="big", fx=[sparkle(8, 10), heart(54, 6)]),
                   dict(dy=-4, legs=TUCK, arm="up", eyes="happy", mouth="big", wag=1, fx=[sparkle(12, 6, True), heart(56, 4)]),
                   dict(dy=-1, eyes="happy", mouth="open", fx=[sparkle(6, 14)])], 8, True, 2)
    S["excited"] = ([dict(dy=-2, arm="wave", eyes="star", mouth="big", wag=1, fx=[sparkle(8, 10), sparkle(56, 8)]),
                     dict(dy=0, arm="up", eyes="star", mouth="big", wag=-1, fx=[sparkle(10, 6, True), sparkle(58, 12)]),
                     dict(dy=-2, arm="wave", eyes="star", mouth="big", wag=1, fx=[sparkle(6, 12), sparkle(55, 5, True)]),
                     dict(dy=0, arm="up", eyes="star", mouth="big", wag=-1, fx=[sparkle(12, 8), sparkle(59, 10)])], 8, True, 1)
    run = ((-4, 0), (4, 4)); run2 = ((3, 4), (-3, 0))
    S["super_speed"] = ([dict(dx=2, hx=2, legs=run, eyes="squint", mouth="open", wag=2, fx=[speed(18, 5), bolt(56, 8)]),
                         dict(dx=2, hx=2, dy=-1, legs=((0, 2), (0, 2)), eyes="squint", mouth="open", wag=2, fx=[speed(20, 5), bolt(54, 10)]),
                         dict(dx=2, hx=2, legs=run2, eyes="squint", mouth="open", wag=2, fx=[speed(18, 5), bolt(56, 8)]),
                         dict(dx=2, hx=2, dy=-1, legs=((0, 2), (0, 2)), eyes="squint", mouth="open", wag=2, fx=[speed(20, 5), bolt(54, 10)])], 12, True, 0)
    S["sad"] = ([dict(eyes="sad", mouth="frown", hy=1, wag=-2, blush=False, fx=[drop(32, 25)]),
                 dict(eyes="sad", mouth="frown", hy=2, wag=-2, blush=False, fx=[drop(32, 29)]),
                 dict(eyes="sad", mouth="frown", hy=2, dy=1, wag=-2, blush=False, fx=[drop(32, 33), drop(46, 26)]),
                 dict(eyes="sad", mouth="frown", hy=1, wag=-2, blush=False, fx=[drop(46, 30)])], 5, True, 1)
    S["surprised"] = ([dict(dy=-4, legs=TUCK, eyes="wide", mouth="o", arm="up", blush=False, fx=[excl(54, 6)]),
                       dict(dy=-3, legs=TUCK, eyes="wide", mouth="o", arm="up", blush=False, fx=[excl(54, 6), drop(24, 8)]),
                       dict(dy=-1, eyes="wide", mouth="o", arm="up", blush=False, fx=[excl(54, 8), drop(24, 10)])], 6, False, 0)
    S["thinking"] = ([dict(arm="chin", eyes="think", mouth="flat", fx=[dots(52, 8, 1)]),
                      dict(arm="chin", eyes="think", mouth="flat", fx=[dots(52, 8, 2)]),
                      dict(arm="chin", eyes="think", mouth="flat", fx=[qmark(54, 2)]),
                      dict(arm="up", eyes="open", mouth="open", fx=[bulb(55, 6)])], 3, True, 2)
    S["laughing"] = ([dict(eyes="squint", mouth="laugh", hx=0, dy=0, fx=[drop(28, 22, "tear"), drop(48, 22, "tear")]),
                      dict(eyes="squint", mouth="laugh", hx=1, dy=-1, fx=[drop(27, 24), drop(49, 24)]),
                      dict(eyes="squint", mouth="laugh", hx=0, dy=0, fx=[drop(26, 26), drop(50, 26)]),
                      dict(eyes="squint", mouth="laugh", hx=-1, dy=-1, fx=[drop(27, 22), drop(49, 22)])], 10, True, 1)
    rA = ((-4, 0), (4, 4)); rB = ((3, 4), (-3, 0)); rM = ((0, 2), (0, 2))
    S["running"] = ([dict(dx=1, legs=rA, mouth="open", dy=0, fx=[dust(6, 59)]), dict(dx=1, legs=rM, mouth="open", dy=-1),
                     dict(dx=1, legs=rB, mouth="open", dy=0, fx=[dust(3, 59, 1)]), dict(dx=1, legs=rB, mouth="open", dy=0),
                     dict(dx=1, legs=rM, mouth="open", dy=-1, fx=[dust(8, 59)]), dict(dx=1, legs=rA, mouth="open", dy=0)], 10, True, 0)
    S["celebrating"] = ([dict(arm="cheer", eyes="happy", mouth="big", fx=[trophy(48, 16), confetti(1)]),
                         dict(dy=-3, legs=TUCK, arm="cheer", eyes="happy", mouth="big", wag=1, fx=[trophy(48, 14), confetti(2), sparkle(10, 20)]),
                         dict(dy=-4, legs=TUCK, arm="cheer", eyes="happy", mouth="big", wag=-1, fx=[trophy(48, 13), confetti(3), sparkle(56, 8, True)]),
                         dict(dy=-3, legs=TUCK, arm="cheer", eyes="happy", mouth="big", wag=1, fx=[trophy(48, 14), confetti(4)]),
                         dict(arm="cheer", eyes="happy", mouth="big", fx=[trophy(48, 16), confetti(5), sparkle(8, 14)]),
                         dict(dy=-2, arm="cheer", eyes="happy", mouth="big", fx=[trophy(48, 15), confetti(6)])], 8, True, 2)
    S["level_unlocked"] = ([dict(dx=-8, arm="point", mouth="big", fx=[portal(57, 36, 2, 5, 1)]),
                            dict(dx=-8, arm="point", mouth="big", fx=[portal(57, 36, 4, 10, 2)]),
                            dict(dx=-8, arm="point", mouth="big", eyes="star", fx=[portal(57, 36, 5, 15, 3), sparkle(40, 8)]),
                            dict(dx=-8, dy=-4, legs=TUCK, arm="point", mouth="big", eyes="star", fx=[portal(57, 36, 5, 15, 4), sparkle(44, 6, True)]),
                            dict(dx=-8, dy=-1, arm="point", mouth="big", eyes="star", fx=[portal(57, 36, 5, 15, 5), sparkle(38, 10)]),
                            dict(dx=-8, arm="point", mouth="big", eyes="star", fx=[portal(57, 36, 5, 15, 6), sparkle(46, 12, True)])], 7, True, 3)
    S["learning_mode"] = ([dict(arm="point", eyes="think", mouth="smile", fx=[magnifier(56, 28)]),
                           dict(arm="point", eyes="think", mouth="smile", fx=[magnifier(56, 27), sparkle(54, 26)]),
                           dict(arm="point", eyes="open", mouth="open", fx=[magnifier(56, 28), sparkle(58, 24)]),
                           dict(arm="point", eyes="blink", mouth="smile", fx=[magnifier(56, 29)])], 4, True, 2)
    return S

def sheet(frames, tier):
    img = Image.new("RGBA", (F * len(frames), F), (0, 0, 0, 0))
    for i, pose in enumerate(frames):
        for (x, y), c in render(pose, tier).items(): img.putpixel((i * F + x, y), c)
    return img

def main():
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    out = args[0] if args else os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "bytey")
    preview = sys.argv[sys.argv.index("--preview") + 1] if "--preview" in sys.argv else None
    S = states()
    for tier in range(5):
        for d in ("sprites", "static"): os.makedirs(os.path.join(out, d, f"tier{tier}"), exist_ok=True)
        for name, (frames, fps, loop, key) in S.items():
            sh = sheet(frames, tier)
            sh.save(os.path.join(out, "sprites", f"tier{tier}", f"{name}.png"))
            sh.crop((key * F, 0, key * F + F, F)).save(os.path.join(out, "static", f"tier{tier}", f"{name}.png"))
    if preview:
        names = list(S)
        W = max(len(S[n][0]) for n in names) * F
        sheetall = Image.new("RGBA", (W, F * len(names)), (40, 90, 180, 255))
        for r, n in enumerate(names):
            sheetall.alpha_composite(Image.open(os.path.join(out, "sprites", "tier0", f"{n}.png")), (0, r * F))
        sheetall.resize((sheetall.width * 3, sheetall.height * 3), Image.NEAREST).save(preview)
        top = Image.new("RGBA", (F * 5 * 2, F * 2), (40, 90, 180, 255))
        for t in range(5):
            top.alpha_composite(Image.open(os.path.join(out, "static", f"tier{t}", "idle.png")), (t * F, 0))
            top.alpha_composite(Image.open(os.path.join(out, "static", f"tier{t}", "celebrating.png")), (t * F, F))
        top.resize((top.width * 3, top.height * 3), Image.NEAREST).save(preview.replace(".png", "_tiers.png"))
    print("ok", len(S), "states x 5 tiers")

if __name__ == "__main__":
    main()
