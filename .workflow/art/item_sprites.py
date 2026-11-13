"""Draws the mod's own 16x16 item textures in code, so no one has to paint them.

Run from the repository root:
    python3 .workflow/art/item_sprites.py            # writes the textures and points the item models at them
    python3 .workflow/art/item_sprites.py --sheet out.png   # also writes the pilot before-and-after sheet
    python3 .workflow/art/item_sprites.py --grid out.png    # also writes a review grid of every sprite

Each sprite is one or more shapes filled from a colour ramp. Shading comes from the shape: the light falls from the
top left, so pixels facing it take the lighter tones and pixels facing away the darker ones, and the edge takes the
darkest tone as an outline in the sprite's own hue. Details such as stems, seeds, and eyes are placed by hand on top.
Ramps shift shadows toward blue and highlights toward yellow, as pixel artists do, so colours stay lively.
"""
import colorsys
import json
import math
import os
import random
import sys

from PIL import Image

ROOT = "src/main/resources/assets/portable_pocket_cabin"
SIZE = 16
OUTLINE, SHADOW, BASE, LIGHT, HIGHLIGHT = range(5)


def hex_rgb(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def ramp(base, spread=1.0):
    """Five tones from outline to highlight around a base colour, with hue-shifted shadows and highlights."""
    r, g, b = (c / 255 for c in hex_rgb(base))
    h, l, s = colorsys.rgb_to_hls(r, g, b)
    tones = []
    for step, (dl, dh, ds) in enumerate([(-0.30, 0.04, 0.05), (-0.14, 0.02, 0.03), (0, 0, 0), (0.12, -0.02, -0.02), (0.24, -0.04, -0.08)]):
        # Shadows lean toward blue (hue 0.66), highlights toward yellow (hue 0.16).
        target = 0.66 if dl < 0 else 0.16
        # Take the short way round the colour wheel, so red shadows lean crimson rather than through orange.
        turn = (target - h + 0.5) % 1.0 - 0.5
        hh = h + turn * abs(dh) * 2 if dl else h
        ll = min(0.95, max(0.04, l + dl * spread))
        ss = min(1, max(0, s + ds))
        tones.append(tuple(round(c * 255) for c in colorsys.hls_to_rgb(hh % 1, ll, ss)))
    return tones


class Sprite:
    def __init__(self):
        self.pixels = [[None] * SIZE for _ in range(SIZE)]

    def put(self, x, y, colour):
        if 0 <= x < SIZE and 0 <= y < SIZE:
            self.pixels[y][x] = colour

    def shape(self, inside, tones, centre, radius, light=(-0.6, -0.8), outline=True, texture=None):
        """Fills the pixels where inside(x, y) holds, shaded by where each faces relative to the centre."""
        mask = {(x, y) for y in range(SIZE) for x in range(SIZE) if inside(x + 0.5, y + 0.5)}
        cx, cy = centre
        rx, ry = radius
        for x, y in mask:
            edge = any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if edge and outline:
                tone = OUTLINE
            else:
                facing = ((x + 0.5 - cx) / rx * light[0] + (y + 0.5 - cy) / ry * light[1])
                tone = HIGHLIGHT if facing > 0.62 else LIGHT if facing > 0.2 else BASE if facing > -0.35 else SHADOW
                if texture:
                    tone = max(SHADOW, min(HIGHLIGHT, tone + texture(x, y)))
            # Tones may depend on the pixel, for fruit that changes colour across its skin.
            self.put(x, y, (tones(x, y) if callable(tones) else tones)[tone])
        return mask

    def ellipse(self, tones, cx, cy, rx, ry, angle=0.0, **kw):
        cos, sin = math.cos(math.radians(angle)), math.sin(math.radians(angle))

        def inside(x, y):
            dx, dy = x - cx, y - cy
            u, v = dx * cos + dy * sin, -dx * sin + dy * cos
            return (u / rx) ** 2 + (v / ry) ** 2 <= 1

        return self.shape(inside, tones, (cx, cy), (rx, ry), **kw)

    def capsule(self, tones, points, radii, **kw):
        """Fills a tube along a polyline whose radius runs from radii[0] to radii[1], for peppers, roots, and pods."""
        segments = list(zip(points, points[1:]))
        lengths = [math.dist(p, q) for p, q in segments]
        total = sum(lengths)

        def inside(x, y):
            done = 0.0
            for (p, q), length in zip(segments, lengths):
                t = max(0.0, min(1.0, ((x - p[0]) * (q[0] - p[0]) + (y - p[1]) * (q[1] - p[1])) / (length * length)))
                cx, cy = p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t
                r = radii[0] + (radii[1] - radii[0]) * (done + t * length) / total
                if math.dist((x, y), (cx, cy)) <= r:
                    return True
                done += length
            return False
        xs, ys = [p[0] for p in points], [p[1] for p in points]
        centre = ((min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2)
        reach = max(max(xs) - min(xs), max(ys) - min(ys)) / 2 + max(radii)
        return self.shape(inside, tones, centre, (reach, reach), **kw)

    def pixels_at(self, colour, points):
        for x, y in points:
            self.put(x, y, colour)

    def line(self, colour, x0, y0, x1, y1):
        steps = max(abs(x1 - x0), abs(y1 - y0))
        points = [(round(x0 + (x1 - x0) * t / steps), round(y0 + (y1 - y0) * t / steps)) for t in range(steps + 1)]
        self.pixels_at(colour, points)
        return points

    def image(self):
        img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
        for y in range(SIZE):
            for x in range(SIZE):
                if self.pixels[y][x]:
                    img.putpixel((x, y), (*self.pixels[y][x], 255))
        return img


def stable_hash(text):
    """A hash that is the same on every run, unlike Python's hash(), so sprites come out identical each time."""
    import zlib
    return zlib.crc32(text.encode())


def speckle(seed, chance, up=1, down=-1):
    """A texture that nudges some pixels a tone up or down, the same way every run."""
    rng = random.Random(seed)
    cache = {}

    def texture(x, y):
        if (x, y) not in cache:
            roll = rng.random()
            cache[(x, y)] = up if roll < chance / 2 else down if roll < chance else 0
        return cache[(x, y)]

    return texture


LEAF = ramp("#4f8a32")
STEM = ramp("#5a6e2a")


def tomato():
    s = Sprite()
    s.ellipse(ramp("#d8322a"), 7.5, 9.2, 6.4, 5.6)
    green = LEAF
    s.pixels_at(green[BASE], [(5, 4), (6, 4), (9, 4), (10, 4), (7, 5), (8, 5), (6, 5), (9, 5)])
    s.pixels_at(green[SHADOW], [(4, 5), (11, 5), (7, 4), (8, 4)])
    s.pixels_at(STEM[OUTLINE], [(8, 2), (8, 3)])
    s.pixels_at(ramp("#d8322a")[HIGHLIGHT], [(4, 7), (5, 6)])
    return s


def aubergine():
    s = Sprite()
    purple = ramp("#5a2e78")

    def inside(x, y):
        # A bulb at the bottom left narrowing toward the stem at the top right.
        return (x - 6.2) ** 2 + (y - 10.2) ** 2 <= 4.6 ** 2 or (x - 9.6) ** 2 + (y - 6.6) ** 2 <= 3.1 ** 2 \
            or abs((x - 6.2) + (y - 10.2) - ((9.6 - 6.2) + (6.6 - 10.2)) * max(0, min(1, ((x - 6.2) * 3.4 - (y - 10.2) * 3.6) / 24.5))) < 3.3 \
            and 0 <= ((x - 6.2) * 3.4 - (y - 10.2) * 3.6) / 24.5 <= 1
    s.shape(inside, purple, (7.6, 8.6), (5.4, 5.4))
    s.pixels_at(LEAF[BASE], [(10, 4), (11, 4), (11, 5), (12, 5), (10, 3), (9, 4), (12, 6), (11, 3)])
    s.pixels_at(LEAF[SHADOW], [(9, 5), (12, 4), (13, 6)])
    s.pixels_at(STEM[SHADOW], [(12, 3), (13, 2)])
    s.pixels_at(purple[HIGHLIGHT], [(4, 9), (4, 10), (5, 8)])
    return s


def pineapple():
    s = Sprite()
    gold = ramp("#d49a32")

    def scales(x, y):
        return -1 if (x + y) % 3 == 0 or (x - y) % 3 == 0 else 0
    s.ellipse(gold, 7.5, 10.6, 4.6, 5.0, texture=scales)
    crown = ramp("#3f8a3a")
    for points, tone in [([(7, 1), (7, 2), (7, 3), (7, 4), (7, 5)], LIGHT), ([(8, 2), (8, 3), (8, 4), (8, 5)], BASE),
                         ([(5, 2), (5, 3), (6, 4), (6, 5)], BASE), ([(10, 2), (10, 3), (9, 4), (9, 5)], SHADOW),
                         ([(3, 4), (4, 5), (5, 5)], SHADOW), ([(12, 4), (11, 5), (10, 5)], OUTLINE), ([(6, 3)], LIGHT), ([(9, 3)], BASE)]:
        s.pixels_at(crown[tone], points)
    return s


def coconut():
    s = Sprite()
    shell = ramp("#6e4a2a")
    s.ellipse(shell, 7.5, 9.6, 6.6, 5.4, texture=speckle(3, 0.35))
    flesh = ramp("#efe9d8", spread=0.6)
    s.ellipse(flesh, 7.5, 7.4, 5.5, 2.6, light=(-0.2, 0.9))
    s.ellipse(ramp("#d8d2bf", spread=0.5), 7.5, 7.5, 3.2, 1.3, outline=False, light=(0.3, 0.9))
    s.pixels_at(shell[OUTLINE], [(2, 7), (13, 7), (2, 8), (13, 8)])
    return s


def rosemary():
    s = Sprite()
    needle = ramp("#3f7048")
    stem = s.line(ramp("#6b5a3a")[SHADOW], 3, 14, 11, 3)
    # Needles on both sides at every step, angled toward the tip, so the sprig reads as dense and leafy.
    for i, (x, y) in enumerate(stem[:-1]):
        length = 1 if i < 2 else 2
        right = [(x + 1 + k, y - k // 2) for k in range(length)]
        left = [(x - 1 - k // 2, y - 1 - k) for k in range(length)]
        s.pixels_at(needle[LIGHT if i % 2 else BASE], right)
        s.pixels_at(needle[SHADOW if i % 2 else BASE], left)
    s.pixels_at(needle[LIGHT], [(11, 2), (12, 2), (11, 1), (12, 1)])
    s.pixels_at(needle[HIGHLIGHT], [(12, 0), (10, 3), (8, 6), (6, 9)])
    s.pixels_at(ramp("#8d8ae0")[LIGHT], [(13, 3), (9, 2)])
    return s


def blackcurrant():
    s = Sprite()
    berry = ramp("#3a2050", spread=0.9)
    s.line(STEM[SHADOW], 9, 1, 8, 4)
    s.line(STEM[SHADOW], 8, 4, 5, 7)
    s.line(STEM[SHADOW], 8, 4, 11, 7)
    for cx, cy in [(5.0, 9.0), (10.5, 9.0), (7.8, 12.0), (4.6, 13.2), (11.0, 13.4)]:
        s.ellipse(berry, cx, cy, 2.6, 2.6)
        s.put(int(cx - 1), int(cy - 1), berry[HIGHLIGHT])
    s.pixels_at(LEAF[BASE], [(10, 1), (11, 1), (11, 2)])
    return s


def lemon():
    s = Sprite()
    yellow = ramp("#f0d838")
    angle = -30
    s.ellipse(yellow, 7.5, 8.5, 5.8, 4.3, angle=angle, texture=speckle(5, 0.12, up=0))
    # Pointed tips at both ends of the long axis.
    ax, ay = math.cos(math.radians(angle)), math.sin(math.radians(angle))
    for sign in (1, -1):
        tip = (round(7.5 + sign * 6.4 * ax - 0.5), round(8.5 + sign * 6.4 * ay - 0.5))
        s.put(*tip, yellow[OUTLINE])
    s.pixels_at(LEAF[BASE], [(12, 3), (13, 2), (12, 2)])
    s.pixels_at(yellow[HIGHLIGHT], [(5, 7), (6, 6)])
    return s


def garlic():
    s = Sprite()
    white = ramp("#e8e0d2", spread=0.7)

    def inside(x, y):
        bulb = ((x - 7.5) / 5.6) ** 2 + ((y - 10.2) / 4.6) ** 2 <= 1
        neck = 3 <= y <= 7 and abs(x - 7.5) <= 0.8 + (y - 3) * 0.75
        return bulb or neck
    s.shape(inside, white, (7.5, 9.4), (5.6, 5.6))
    # Clove seams, and the faint purple streaks of the papery skin.
    s.pixels_at(white[SHADOW], [(5, 8), (5, 9), (5, 10), (5, 11), (10, 8), (10, 9), (10, 10), (10, 11), (7, 9), (7, 10)])
    s.pixels_at(ramp("#b08aa0", spread=0.5)[LIGHT], [(4, 11), (4, 12), (11, 11), (11, 12), (8, 12), (8, 13)])
    s.pixels_at(ramp("#c8b48a")[BASE], [(6, 15), (7, 15), (9, 15)])
    s.pixels_at(white[OUTLINE], [(7, 2), (8, 2)])
    return s


def mussels():
    s = Sprite()
    shell = ramp("#2c3050", spread=0.9)

    def bands(x, y):
        return 1 if (x + 2 * y) % 5 == 0 else 0
    # Two closed shells, one behind the other, narrow at the hinge.
    s.ellipse(ramp("#262a46", spread=0.9), 9.4, 7.0, 5.0, 2.4, angle=-40, texture=bands)
    s.ellipse(shell, 6.6, 9.6, 5.8, 2.8, angle=-40, texture=bands)
    s.pixels_at(shell[HIGHLIGHT], [(7, 7), (8, 6)])
    return s


def compost():
    s = Sprite()
    soil = ramp("#4e3626", spread=0.8)

    def inside(x, y):
        return y >= 6 and ((x - 7.5) / 7.2) ** 2 + ((y - 15.0) / 9.0) ** 2 <= 1
    s.shape(inside, soil, (7.5, 11.0), (7.0, 5.0), texture=speckle(11, 0.5))
    rng = random.Random(7)
    bits = [ramp("#6a9a3a")[BASE], ramp("#d0802a")[BASE], ramp("#e8d8b0")[BASE], ramp("#6a9a3a")[LIGHT]]
    for _ in range(9):
        x, y = rng.randint(3, 12), rng.randint(9, 14)
        if s.pixels[y][x] and s.pixels[y][x] != soil[OUTLINE]:
            s.put(x, y, rng.choice(bits))
    return s




# Templates for families of produce. Each takes the item's colour, so one shape serves many items.

def round_fruit(colour, r=5.4, cy=9.0, crease=False, pores=False, leaf=True, stem=True, blush=None, nub=False):
    s = Sprite()
    tones = ramp(colour)
    s.ellipse(tones, 7.5, cy, r, r * 0.95, texture=speckle(stable_hash(colour) % 97, 0.18, up=0) if pores else None)
    top = round(cy - r)
    if crease:
        s.pixels_at(tones[SHADOW], [(8, top + 1), (8, top + 2), (9, top + 3), (9, top + 4)])
    if blush:
        s.pixels_at(ramp(blush)[BASE], [(5, round(cy) - 1), (4, round(cy)), (5, round(cy)), (6, round(cy) - 2), (4, round(cy) + 1)])
    if stem:
        s.pixels_at(STEM[OUTLINE], [(8, top - 1), (8, top)])
    if leaf:
        s.pixels_at(LEAF[BASE], [(9, top - 1), (10, top - 1), (10, top - 2)])
        s.pixels_at(LEAF[LIGHT], [(9, top - 2)])
    if nub:
        s.pixels_at(tones[OUTLINE], [(round(7.5 + r * 0.8), round(cy + r * 0.6))])
    s.pixels_at(tones[HIGHLIGHT], [(5, round(cy) - 3), (4, round(cy) - 2)])
    return s


def cherries(colour):
    s = Sprite()
    tones = ramp(colour)
    s.line(STEM[SHADOW], 9, 2, 5, 9)
    s.line(STEM[SHADOW], 9, 2, 10, 9)
    for cx, cy in [(5.0, 11.2), (10.5, 11.6)]:
        s.ellipse(tones, cx, cy, 3.2, 3.0)
        s.put(int(cx) - 1, int(cy) - 1, tones[HIGHLIGHT])
    s.pixels_at(LEAF[BASE], [(10, 1), (11, 1), (11, 2), (12, 2)])
    return s


def bunch(colour, r=2.3):
    """A bunch of grapes: round berries in rows narrowing to a point, under a stem and leaf."""
    s = Sprite()
    tones = ramp(colour)
    rows = [(5.0, [4.2, 8.2, 12.2]), (8.6, [6.2, 10.2, 2.6]), (12.0, [4.4, 8.4]), (14.4, [6.6])]
    for cy, xs in rows:
        for cx in xs:
            s.ellipse(tones, cx, cy, r, r)
            s.pixels_at(tones[HIGHLIGHT], [(int(cx) - 1, int(cy) - 1)])
    s.pixels_at(STEM[SHADOW], [(8, 1), (8, 2)])
    s.pixels_at(LEAF[BASE], [(9, 1), (10, 1), (10, 0), (11, 1)])
    return s


def berry_cluster(colour, spray=False):
    """Small berries on fine stems: currants on a strig, or an elderberry spray."""
    s = Sprite()
    tones = ramp(colour)
    if spray:
        for x0, y0 in [(3, 8), (7, 6), (11, 7), (5, 11), (10, 11)]:
            s.line(STEM[SHADOW], 8, 15, x0, y0)
        centres = [(3, 7), (5, 6), (7, 5), (9, 5), (11, 6), (13, 7), (4, 10), (6, 9), (9, 9), (11, 10), (5, 12), (10, 12), (8, 7)]
        for cx, cy in centres:
            s.pixels_at(tones[BASE], [(cx, cy), (cx + 1, cy)])
            s.pixels_at(tones[OUTLINE], [(cx, cy + 1), (cx + 1, cy + 1)])
            s.put(cx, cy, tones[LIGHT])
        return s
    s.line(STEM[SHADOW], 4, 1, 9, 6)
    s.line(STEM[SHADOW], 9, 6, 11, 13)
    for cx, cy in [(6.0, 5.0), (9.5, 7.5), (7.6, 10.0), (11.4, 10.6), (9.4, 13.0), (12.4, 13.8)]:
        s.ellipse(tones, cx, cy, 2.0, 2.0)
        s.put(int(cx) - 1, int(cy) - 1, tones[HIGHLIGHT])
    return s


def strawberry(colour):
    s = Sprite()
    tones = ramp(colour)

    def inside(x, y):
        if y < 4 or y > 15:
            return False
        half = 5.6 * (1 - ((y - 5.5) / 10.0) ** 2) if y >= 5.5 else 5.6 * math.sqrt(max(0, 1 - ((5.5 - y) / 1.8) ** 2))
        return abs(x - 7.5) <= half
    s.shape(inside, tones, (7.5, 8.5), (5.6, 6.0))
    seed = ramp("#f0d860")[LIGHT]
    for y in range(6, 14):
        for x in range(3, 13):
            if (x + 2 * y) % 4 == 0 and s.pixels[y][x] not in (None, tones[OUTLINE]):
                s.put(x, y, seed)
    s.pixels_at(LEAF[BASE], [(4, 4), (5, 3), (6, 4), (7, 3), (8, 3), (9, 4), (10, 3), (11, 4)])
    s.pixels_at(LEAF[SHADOW], [(5, 4), (8, 4), (10, 4)])
    s.pixels_at(STEM[OUTLINE], [(8, 1), (8, 2)])
    return s


def raspberry(colour):
    s = Sprite()
    tones = ramp(colour)

    def drupelets(x, y):
        return 1 if x % 2 == 0 and y % 2 == 1 else 0
    s.ellipse(tones, 7.5, 9.5, 4.8, 5.4, texture=drupelets)
    s.pixels_at(LEAF[BASE], [(5, 3), (6, 3), (7, 3), (8, 3), (9, 3), (10, 3)])
    s.pixels_at(LEAF[SHADOW], [(6, 4), (9, 4)])
    s.pixels_at(STEM[OUTLINE], [(7, 1), (7, 2)])
    return s


def pepper(colour, kind):
    s = Sprite()
    tones = ramp(colour)
    if kind == "bell":
        s.ellipse(tones, 7.5, 9.6, 5.4, 5.4)
        s.pixels_at(tones[SHADOW], [(5, 7), (5, 8), (5, 9), (5, 10), (5, 11), (10, 7), (10, 8), (10, 9), (10, 10), (10, 11)])
        s.pixels_at(tones[OUTLINE], [(7, 14), (8, 14)])
        s.pixels_at(LEAF[SHADOW], [(6, 4), (7, 4), (8, 4), (9, 4)])
        s.pixels_at(LEAF[BASE], [(7, 3), (8, 3), (8, 2), (9, 1)])
        s.pixels_at(tones[HIGHLIGHT], [(3, 8), (3, 9), (4, 7)])
        return s
    shapes = {
        "paprika": ([(6, 4), (8, 9), (9, 14)], (3.2, 0.9)),
        "chili": ([(5, 3), (7, 7), (9, 11), (12, 14)], (2.0, 0.6)),
        "small": ([(6, 4), (8, 8), (10, 12)], (1.5, 0.5)),
        "poblano": ([(5, 4), (8, 9), (10, 14)], (3.6, 1.2)),
        "aji": ([(5, 3), (8, 8), (10, 13)], (2.4, 0.8)),
    }
    points, radii = shapes[kind]
    s.capsule(tones, points, radii)
    x0, y0 = points[0]
    s.pixels_at(LEAF[SHADOW], [(x0 - 1, y0 - 2), (x0, y0 - 2), (x0 + 1, y0 - 2)])
    s.pixels_at(LEAF[BASE], [(x0, y0 - 3), (x0 - 1, y0 - 4)])
    return s


def leafy_head(colour, frilly=False):
    s = Sprite()
    tones = ramp(colour)
    s.ellipse(tones, 7.5, 9.0, 6.6, 6.0, texture=speckle(stable_hash(colour) % 89, 0.25) if frilly else None)
    # Overlapping leaves: curved seams, and a paler heart.
    s.pixels_at(tones[SHADOW], [(4, 6), (3, 8), (3, 10), (4, 12), (11, 6), (12, 8), (12, 10), (11, 12), (6, 4), (9, 4)])
    s.pixels_at(tones[HIGHLIGHT], [(7, 7), (8, 7), (7, 8), (8, 9), (7, 10)])
    s.pixels_at(tones[LIGHT], [(6, 9), (9, 8), (8, 11), (6, 11)])
    return s


def long_veg(colour, bumpy=False, stripes=False):
    s = Sprite()
    tones = ramp(colour)

    def skin(x, y):
        if bumpy and (x * 3 + y * 5) % 7 == 0:
            return 1
        if stripes and (x + y) % 3 == 0:
            return -1
        return 0
    s.capsule(tones, [(3, 12), (12, 3)], (2.9, 2.6), texture=skin)
    s.pixels_at(STEM[SHADOW], [(13, 2), (14, 1)])
    s.pixels_at(tones[HIGHLIGHT], [(5, 8), (6, 7), (7, 6)])
    return s


def husked(colour):
    """A tomatillo: a pale green fruit in a papery husk."""
    s = Sprite()
    s.ellipse(ramp(colour), 7.5, 9.6, 5.0, 4.8)
    husk = ramp("#c8b878", spread=0.7)
    s.pixels_at(husk[BASE], [(3, 7), (3, 8), (4, 6), (12, 7), (12, 8), (11, 6), (5, 4), (6, 4), (9, 4), (10, 4), (7, 3), (8, 3)])
    s.pixels_at(husk[SHADOW], [(2, 9), (13, 9), (4, 5), (11, 5)])
    s.pixels_at(STEM[OUTLINE], [(8, 1), (8, 2)])
    return s


def small_round(colour, stripes):
    """A Thai aubergine: a small round fruit with pale stripes under a green cap."""
    s = Sprite()
    tones = ramp(colour)
    s.ellipse(tones, 7.5, 9.5, 5.2, 5.0, texture=(lambda x, y: 1 if x % 3 == 0 else 0) if stripes else None)
    s.pixels_at(LEAF[SHADOW], [(5, 4), (6, 4), (7, 4), (8, 4), (9, 4), (10, 4), (6, 5), (9, 5)])
    s.pixels_at(LEAF[BASE], [(7, 3), (8, 3), (8, 2)])
    return s


def radish(colour):
    s = Sprite()
    tones = ramp(colour)
    s.ellipse(tones, 7.5, 10.0, 4.4, 4.2)
    s.pixels_at(ramp("#f0ece0")[BASE], [(7, 14), (8, 14), (8, 15)])
    s.pixels_at(LEAF[BASE], [(5, 3), (6, 4), (7, 5), (8, 5), (9, 4), (10, 3), (11, 2), (4, 2), (7, 4), (8, 3), (8, 2)])
    s.pixels_at(LEAF[LIGHT], [(5, 2), (10, 2), (8, 1)])
    s.pixels_at(tones[HIGHLIGHT], [(5, 8), (5, 9)])
    return s


def bulb(colour, small=False):
    s = Sprite()
    tones = ramp(colour)
    rx, ry, cy = (4.2, 3.6, 11.0) if small else (5.6, 4.6, 10.2)

    def inside(x, y):
        body = ((x - 7.5) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1
        neck = 3 <= y <= cy - ry + 2 and abs(x - 7.5) <= 0.8 + (y - 3) * 0.7
        return body or neck
    s.shape(inside, tones, (7.5, cy - 1), (rx, rx))
    s.pixels_at(tones[SHADOW], [(6, round(cy) - 2), (6, round(cy) - 1), (6, round(cy)), (9, round(cy) - 2), (9, round(cy) - 1), (9, round(cy))])
    s.pixels_at(tones[HIGHLIGHT], [(4, round(cy) - 1), (5, round(cy) - 2)])
    s.pixels_at(ramp("#c8b48a")[BASE], [(6, 15), (7, 15), (9, 15)])
    s.pixels_at(tones[OUTLINE], [(7, 2), (8, 1)])
    return s


def root(colour, kind):
    s = Sprite()
    tones = ramp(colour)
    if kind == "tuber":
        s.capsule(tones, [(3, 11), (8, 8), (12, 6)], (2.6, 1.6), texture=speckle(stable_hash(colour) % 83, 0.2, up=0))
        s.pixels_at(tones[OUTLINE], [(13, 5), (14, 4), (2, 12)])
    elif kind == "cassava":
        def rings(x, y):
            return -1 if (x - y) % 4 == 0 else 0
        s.capsule(tones, [(2, 13), (13, 3)], (2.4, 1.8), texture=rings)
        s.ellipse(ramp("#efe6cf", spread=0.5), 2.6, 12.6, 1.7, 1.7, outline=False)
    else:
        # A knobbly rhizome: a body with a few fingers.
        def rings(x, y):
            return -1 if (x + y * 2) % 5 == 0 else 0
        for points, radii in [([(3, 10), (11, 9)], (2.6, 2.2)), ([(6, 9), (5, 4)], (1.8, 1.4)), ([(10, 9), (12, 5)], (1.7, 1.3)),
                              ([(9, 10), (10, 14)], (1.6, 1.2))]:
            s.capsule(tones, points, radii, texture=rings)
        if kind == "turmeric":
            s.ellipse(ramp("#f09018"), 12.0, 5.0, 1.2, 1.2, outline=False)
    return s


def herb(colour, kind):
    s = Sprite()
    tones = ramp(colour)
    if kind == "bay":
        # A single glossy leaf, pointed at both ends, with a pale midrib.
        s.capsule(ramp("#5a8a44"), [(3, 13), (7, 9), (13, 2)], (0.6, 0.6), outline=False)
        leaf = ramp("#5a8a44")

        def inside(x, y):
            u, v = ((x - 8) - (y - 7.5)) / 1.414, ((x - 8) + (y - 7.5)) / 1.414
            return abs(v) <= 2.9 * max(0, 1 - (u / 7.2) ** 2)
        s.shape(inside, leaf, (8, 7.5), (5, 5))
        s.line(leaf[HIGHLIGHT], 4, 12, 12, 3)
        s.pixels_at(STEM[SHADOW], [(3, 13), (2, 14)])
        return s
    stem = s.line(STEM[SHADOW], 7, 15, 8, 3)
    if kind in ("mint", "basil"):
        size = (2.6, 1.6) if kind == "mint" else (3.0, 2.0)
        for i, (y, side) in enumerate([(12, -1), (10, 1), (8, -1), (6, 1)]):
            s.ellipse(tones, 7.5 + side * (size[0] + 0.2), y, size[0], size[1], angle=-25 * side,
                      texture=(lambda x, yy: -1 if (x + yy) % 3 == 0 else 0) if kind == "mint" else None)
        s.ellipse(tones, 7.8, 3.4, 1.8, 2.4)
        return s
    if kind == "thyme":
        for x0, y0, x1, y1 in [(7, 12, 3, 6), (8, 10, 12, 4), (8, 7, 9, 1)]:
            for x, y in s.line(STEM[SHADOW], x0, y0, x1, y1)[1:]:
                s.pixels_at(tones[LIGHT if (x + y) % 2 else BASE], [(x - 1, y), (x + 1, y)])
        return s
    if kind == "parsley":
        for cx, cy in [(5.0, 5.0), (10.0, 4.5), (7.5, 8.0), (4.5, 9.5), (11.0, 9.0)]:
            s.ellipse(tones, cx, cy, 2.4, 2.2, texture=speckle(int(cx * 10 + cy), 0.5))
        for x, y in [(5, 7), (10, 7), (8, 10), (5, 11), (10, 11)]:
            s.line(STEM[LIGHT], 7, 15, x, y)
        return s
    if kind == "dill":
        for x0, y0, x1, y1 in [(8, 12, 3, 7), (8, 10, 13, 5), (8, 7, 5, 2), (8, 6, 11, 1)]:
            for i, (x, y) in enumerate(s.line(tones[SHADOW], x0, y0, x1, y1)[1:]):
                if i % 2 == 0:
                    s.pixels_at(tones[LIGHT], [(x, y - 1), (x + 1, y - 1)])
        return s
    return s


def grain(colour, kind):
    s = Sprite()
    tones = ramp(colour)
    if kind == "corn":
        def kernels(x, y):
            return 1 if x % 2 == 0 and y % 2 == 0 else -1 if (x + y) % 2 else 0
        s.capsule(tones, [(7.5, 3), (7.5, 11)], (3.2, 2.6), texture=kernels)
        husk = ramp("#6aa040")
        s.pixels_at(husk[BASE], [(3, 10), (3, 11), (4, 12), (4, 13), (5, 14), (12, 10), (12, 11), (11, 12), (11, 13), (10, 14)])
        s.pixels_at(husk[SHADOW], [(6, 14), (7, 15), (8, 15), (9, 14)])
        return s
    if kind == "rice":
        def grains(x, y):
            return 1 if (x + 2 * y) % 3 == 0 else 0
        s.shape(lambda x, y: y >= 6 and ((x - 7.5) / 7.0) ** 2 + ((y - 15.5) / 9.5) ** 2 <= 1, ramp(colour, spread=0.6),
                (7.5, 11.0), (7.0, 5.0), texture=grains)
        return s
    # An ear of barley: a stalk, plump kernels in two rows, and long awns sweeping up.
    s.line(STEM[LIGHT], 3, 15, 6, 10)
    for x, y in [(6, 9), (7, 7), (8, 5), (9, 3)]:
        s.pixels_at(tones[SHADOW], [(x - 1, y + 1), (x + 1, y + 1)])
        s.pixels_at(tones[BASE], [(x - 1, y), (x + 1, y), (x, y + 1)])
        s.pixels_at(tones[LIGHT], [(x, y)])
        s.line(tones[LIGHT], x + 1, y - 1, min(15, x + 5), max(0, y - 5))
    s.line(tones[LIGHT], 9, 2, 10, 0)
    return s
    if kind == "rice":
        def grains(x, y):
            return 1 if (x + 2 * y) % 3 == 0 else 0
        s.shape(lambda x, y: y >= 6 and ((x - 7.5) / 7.0) ** 2 + ((y - 15.5) / 9.5) ** 2 <= 1, ramp(colour, spread=0.6),
                (7.5, 11.0), (7.0, 5.0), texture=grains)
        return s
    # An ear of barley: paired kernels up a stalk, with long awns.
    s.line(STEM[LIGHT], 4, 15, 7, 9)
    for i, (x, y) in enumerate([(7, 8), (8, 7), (8, 5), (9, 4), (9, 2)]):
        s.ellipse(tones, x - 0.6, y + 0.5, 1.4, 1.0, angle=-60)
        s.ellipse(tones, x + 1.6, y + 0.8, 1.4, 1.0, angle=-30)
        s.line(tones[LIGHT], x + 2, y, min(15, x + 6), max(0, y - 4))
    return s


def almond(colour):
    s = Sprite()
    tones = ramp(colour)

    def inside(x, y):
        # A teardrop: round at the base, pointed at the tip, lying on a diagonal.
        u, v = ((x - 7.5) - (y - 8.5)) / 1.414, ((x - 7.5) + (y - 8.5)) / 1.414
        half = 3.6 * math.sqrt(max(0, 1 - ((u + 1.5) / 6.5) ** 2)) * (0.45 + 0.55 * (1 - (u + 6) / 12))
        return abs(v) <= half and -6.5 <= u <= 6.5
    s.shape(inside, tones, (7.5, 8.5), (5, 5), texture=lambda x, y: -1 if (x + 2 * y) % 5 == 0 else 0)
    return s


def mango(colour):
    s = Sprite()
    tones = ramp(colour)
    s.ellipse(tones, 7.5, 8.8, 6.0, 4.6, angle=-35)
    s.pixels_at(ramp("#d84a2a")[LIGHT], [(5, 6), (6, 5), (7, 5), (5, 7), (4, 8)])
    s.pixels_at(ramp("#8ab83a")[BASE], [(11, 12), (12, 11), (10, 13)])
    s.pixels_at(STEM[OUTLINE], [(12, 4), (13, 3)])
    return s


def papaya(colour):
    s = Sprite()
    tones = ramp(colour)
    s.capsule(tones, [(6, 11), (10, 5)], (4.4, 2.6), texture=speckle(31, 0.2, up=1, down=0))
    s.pixels_at(STEM[OUTLINE], [(11, 2), (12, 1)])
    return s


def asparagus(colour):
    s = Sprite()
    tones = ramp(colour)
    tip = ramp("#7a5a9a")
    for x0, x1 in ((4, 5), (8, 8), (12, 11)):
        s.line(tones[SHADOW], x0, 15, x1, 4)
        s.line(tones[LIGHT], x0 + 1, 15, x1 + 1, 4)
        s.pixels_at(tip[BASE], [(x1, 3), (x1 + 1, 3), (x1, 2)])
        s.pixels_at(tip[LIGHT], [(x1 + 1, 2)])
        s.pixels_at(tones[OUTLINE], [(x1, 7), (x1 + 1, 10)])
    band = ramp("#c84030")
    s.pixels_at(band[BASE], [(x, 11) for x in range(3, 14)])
    s.pixels_at(band[SHADOW], [(x, 12) for x in range(4, 13)])
    return s


def rhubarb(colour):
    s = Sprite()
    tones = ramp(colour)
    s.capsule(tones, [(4, 14), (8, 5)], (1.6, 1.4))
    s.capsule(tones, [(8, 15), (11, 6)], (1.6, 1.4))
    leaf = ramp("#4f9a3a")
    s.ellipse(leaf, 8.0, 3.6, 5.0, 2.8, angle=-10, texture=lambda x, y: -1 if (x - y) % 3 == 0 else 0)
    return s


TEMPLATED = {
    "plum": lambda c: round_fruit(c, crease=True, leaf=False), "apricot": lambda c: round_fruit(c, crease=True, blush="#e06a30"),
    "orange": lambda c: round_fruit(c, pores=True), "lime": lambda c: round_fruit(c, r=4.6, cy=9.5, pores=True, leaf=False, nub=True),
    "ume_plum": lambda c: round_fruit(c, r=4.6, cy=9.5, crease=True, leaf=False, blush="#d86a3a"), "mango": mango,
    "green_papaya": papaya, "cherry": cherries, "almond": almond,
    "grape": bunch, "white_grape": bunch, "red_grape": bunch, "strawberry": strawberry, "raspberry": raspberry,
    "redcurrant": berry_cluster, "elderberry": lambda c: berry_cluster(c, spray=True),
    "bell_pepper": lambda c: pepper(c, "bell"), "paprika_pepper": lambda c: pepper(c, "paprika"),
    "green_chili": lambda c: pepper(c, "chili"), "red_chili": lambda c: pepper(c, "chili"),
    "poblano_chili": lambda c: pepper(c, "poblano"), "birds_eye_chili": lambda c: pepper(c, "small"), "aji_amarillo": lambda c: pepper(c, "aji"),
    "cabbage": leafy_head, "red_cabbage": leafy_head, "lettuce": lambda c: leafy_head(c, frilly=True),
    "courgette": lambda c: long_veg(c, stripes=True), "cucumber": lambda c: long_veg(c, bumpy=True),
    "tomatillo": husked, "thai_aubergine": lambda c: small_round(c, True), "radish": radish,
    "onion": bulb, "red_onion": bulb, "shallot": lambda c: bulb(c, small=True), "asparagus": asparagus, "rhubarb": rhubarb,
    "sweet_potato": lambda c: root(c, "tuber"), "cassava": lambda c: root(c, "cassava"), "ginger": lambda c: root(c, "rhizome"),
    "turmeric": lambda c: root(c, "turmeric"), "galangal": lambda c: root(c, "rhizome"),
    "thyme": lambda c: herb(c, "thyme"), "mint": lambda c: herb(c, "mint"), "basil": lambda c: herb(c, "basil"),
    "parsley": lambda c: herb(c, "parsley"), "dill": lambda c: herb(c, "dill"), "bay_laurel_leaf": lambda c: herb(c, "bay"),
    "barley": lambda c: grain(c, "ear"), "corn": lambda c: grain(c, "corn"), "rice": lambda c: grain(c, "rice"),
}
# Each templated item's colour: the tint it had, so items keep their familiar colours.
COLOURS = {"plum": "#6a2a6a", "apricot": "#f0a040", "orange": "#f09020", "lime": "#6ac02a", "mango": "#f0a030",
           "green_papaya": "#7aa03a", "cherry": "#b0102a", "ume_plum": "#c8c040", "almond": "#c8a070", "grape": "#5aa03a",
           "white_grape": "#c8d86a", "red_grape": "#6a1a4a", "strawberry": "#e0303a", "raspberry": "#c8284a", "redcurrant": "#d0202a",
           "elderberry": "#3a2050", "bell_pepper": "#d8382a", "paprika_pepper": "#c8302a", "green_chili": "#4a9a2a",
           "red_chili": "#d0201a", "poblano_chili": "#2a5a1a", "birds_eye_chili": "#e0201a", "aji_amarillo": "#f0b020",
           "cabbage": "#8cc468", "red_cabbage": "#8a2a6a", "lettuce": "#9ad86a", "courgette": "#3f7a2e", "cucumber": "#4e8a2e",
           "tomatillo": "#8ac85a", "thai_aubergine": "#8ab86a", "radish": "#d8405a", "onion": "#c89a5a", "red_onion": "#9a3a6a",
           "shallot": "#b0705a", "asparagus": "#6fa84a", "rhubarb": "#c83a4a", "sweet_potato": "#a8504a", "cassava": "#8a5a3a",
           "ginger": "#d8b070", "turmeric": "#c87a2a", "galangal": "#e0c8a0", "thyme": "#6a8a4a", "mint": "#4ab84a",
           "basil": "#3a9a2a", "parsley": "#3e8a2e", "dill": "#8ab85a", "bay_laurel_leaf": "#4a7a3a", "barley": "#d8c27a",
           "corn": "#f2d23a", "rice": "#e8e0c0"}

SPRITES = {"pineapple": pineapple, "coconut": coconut, "rosemary": rosemary, "blackcurrant": blackcurrant, "tomato": tomato,
           "aubergine": aubergine, "lemon": lemon, "garlic": garlic, "mussels": mussels, "compost": compost}
SPRITES.update({name: (lambda draw, colour: lambda: draw(colour))(draw, COLOURS[name]) for name, draw in TEMPLATED.items()})
def blend(value, edge, x, y, width=0.14):
    """Whether a pixel falls past an edge, dithered in a checkerboard across a narrow band so colours mix softly."""
    if abs(value - edge) < width:
        return (x + y) % 2 == 0
    return value > edge


# Hand-drawn produce, reviewed five at a time. Each is drawn on its own, as the first ten were.

def plum_hand():
    s = Sprite()
    skin = ramp("#4a2468", spread=0.9)
    s.ellipse(skin, 7.5, 9.2, 5.8, 6.0, angle=12)
    # The seam down the front, and the dusty bloom where the light falls.
    s.pixels_at(skin[SHADOW], [(9, 4), (10, 5), (10, 6), (11, 7), (11, 8), (11, 9)])
    bloom = ramp("#9a88c0", spread=0.5)
    s.pixels_at(bloom[BASE], [(4, 7), (5, 6), (6, 5), (4, 9), (6, 7)])
    s.pixels_at(bloom[LIGHT], [(5, 7), (5, 5)])
    s.pixels_at(ramp("#6b4a2a")[SHADOW], [(8, 2), (8, 3), (9, 1)])
    s.pixels_at(LEAF[BASE], [(10, 1), (11, 1), (10, 2), (12, 2)])
    s.pixels_at(LEAF[LIGHT], [(11, 0)])
    return s


def apricot_hand():
    s = Sprite()
    orange, blush = ramp("#f2a43c"), ramp("#e2582a")

    def skin(x, y):
        # A blush on the sunny shoulder, fading into the orange through a dithered edge.
        return blush if blend(-math.dist((x + 0.5, y + 0.5), (10.8, 6.6)) / 2.9, -1.0, x, y, width=0.25) else orange
    s.ellipse(skin, 7.5, 9.3, 6.0, 5.7, texture=speckle(17, 0.12, up=0))
    s.pixels_at(orange[SHADOW], [(7, 5), (7, 6), (6, 7), (6, 8), (6, 9)])
    s.pixels_at(orange[HIGHLIGHT], [(4, 7), (5, 6)])
    s.pixels_at(ramp("#6b4a2a")[SHADOW], [(8, 3), (8, 2)])
    s.pixels_at(LEAF[BASE], [(9, 2), (10, 2), (10, 1), (11, 1)])
    s.pixels_at(LEAF[LIGHT], [(9, 1)])
    return s


def orange_hand():
    s = Sprite()
    peel = ramp("#f0861a")

    def dimples(x, y):
        return -1 if (x * 3 + y * 5) % 7 == 0 else 0
    s.ellipse(peel, 7.5, 9.0, 6.4, 6.2, texture=dimples)
    s.pixels_at(peel[HIGHLIGHT], [(4, 6), (5, 5), (4, 7)])
    s.pixels_at(peel[LIGHT], [(5, 6), (6, 5), (3, 8)])
    s.pixels_at(peel[OUTLINE], [(8, 3)])
    s.pixels_at(ramp("#6b4a2a")[SHADOW], [(8, 2)])
    s.pixels_at(LEAF[BASE], [(9, 1), (10, 1), (11, 2), (10, 2)])
    s.pixels_at(LEAF[SHADOW], [(12, 2), (11, 3)])
    s.pixels_at(LEAF[LIGHT], [(9, 2)])
    return s


def lime_hand():
    s = Sprite()
    rind = ramp("#4fa02a")
    angle = -25
    mask = s.ellipse(rind, 7.5, 8.8, 5.8, 4.9, angle=angle, texture=speckle(23, 0.14, up=0))
    # A nub at each end, touching the fruit.
    ax, ay = math.cos(math.radians(angle)), math.sin(math.radians(angle))
    for sign in (1, -1):
        for reach in (6.4, 6.0, 5.6):
            nub = (round(7.5 + sign * reach * ax - 0.5), round(8.8 + sign * reach * ay - 0.5))
            if nub not in mask and any((nub[0] + dx, nub[1] + dy) in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                s.put(*nub, rind[OUTLINE])
                break
    s.pixels_at(rind[HIGHLIGHT], [(4, 8), (5, 7), (6, 6), (7, 6)])
    s.pixels_at(rind[LIGHT], [(4, 9), (5, 8), (8, 5)])
    return s


def mango_hand():
    s = Sprite()
    red, orange, green = ramp("#c83a2a"), ramp("#e8962a"), ramp("#a8b832")
    angle = -35
    cos, sin = math.cos(math.radians(angle)), math.sin(math.radians(angle))

    def skin(x, y):
        # Red at the shoulder by the stem, orange across the middle, and green-gold at the tip, blended where they meet.
        u = ((x + 0.5 - 7.5) * cos + (y + 0.5 - 8.8) * sin) / 6.4
        return red if blend(u, 0.4, x, y) else orange if blend(u, -0.4, x, y) else green
    s.ellipse(skin, 7.5, 8.8, 6.4, 4.7, angle=angle)
    s.pixels_at(orange[HIGHLIGHT], [(5, 7), (6, 6), (7, 6)])
    s.pixels_at(ramp("#6b4a2a")[SHADOW], [(13, 3), (14, 2)])
    s.pixels_at(LEAF[BASE], [(12, 2), (12, 1), (11, 1)])
    return s


def cherry_hand():
    s = Sprite()
    red = ramp("#a80e26", spread=0.9)
    stem = ramp("#6a7a2a")
    # Two stems from one twig, curving down to the fruit.
    s.pixels_at(stem[SHADOW], [(9, 1), (8, 2), (7, 3), (6, 4), (6, 5), (5, 6), (9, 2), (10, 3), (10, 4), (11, 5), (11, 6)])
    s.ellipse(red, 11.0, 10.4, 3.5, 3.4)
    s.ellipse(red, 5.0, 11.2, 3.6, 3.5)
    for x, y in [(3, 9), (4, 9), (9, 8), (10, 8)]:
        s.put(x, y, red[HIGHLIGHT])
    s.pixels_at(red[LIGHT], [(3, 10), (9, 9)])
    leaf = ramp("#4f9a3a")
    s.pixels_at(leaf[BASE], [(10, 1), (11, 1), (12, 1), (11, 0), (12, 2), (13, 2)])
    s.pixels_at(leaf[LIGHT], [(10, 0), (11, 2)])
    s.pixels_at(leaf[SHADOW], [(13, 1), (14, 2)])
    return s


ALMOND = [  # One almond, pixel by pixel: o outline, s shadow, b base, l light, h highlight, r a ridge.
    "...o..",
    "..olo.",
    "..lbbo",
    ".olrbo",
    "olhrso",
    "olbrso",
    "olbrso",
    ".obso.",
    "..oo..",
]


def stamp(s, art, tones, x0, y0, mirror=False):
    """Draws a hand-placed pixel pattern with a ramp, at a position, optionally mirrored left to right."""
    key = {"o": OUTLINE, "s": SHADOW, "b": BASE, "l": LIGHT, "h": HIGHLIGHT, "r": SHADOW, "d": SHADOW}
    width = max(len(row) for row in art)
    for y, row in enumerate(art):
        for x, ch in enumerate(row):
            if ch in key:
                s.put(x0 + (width - 1 - x if mirror else x), y0 + y, tones[key[ch]])


def almond_hand():
    s = Sprite()
    shell = ramp("#b88a5a")
    # Three almonds in a little pile, the front one drawn last.
    stamp(s, ALMOND, shell, 1, 1)
    stamp(s, ALMOND, shell, 9, 2, mirror=True)
    stamp(s, ALMOND, shell, 5, 6)
    return s


def green_papaya_hand():
    s = Sprite()
    skin = ramp("#6a9e36")
    angle = 25
    cos, sin = math.cos(math.radians(angle)), math.sin(math.radians(angle))

    def inside(x, y):
        # An egg: rounder and fuller at the bottom, gently narrower at the stem.
        u, v = -(x - 7.8) * sin + (y - 8.6) * cos, (x - 7.8) * cos + (y - 8.6) * sin
        length = 7.3
        if abs(u) > length:
            return False
        width = 3.8 * math.sqrt(1 - (u / length) ** 2) * (1 + 0.22 * u / length)
        return abs(v) <= width

    def ribs(x, y):
        v = (x + 0.5 - 7.8) * cos + (y + 0.5 - 8.6) * sin
        return -1 if abs(abs(v) - 2.2) < 0.45 else 0
    s.shape(inside, skin, (7.0, 8.0), (4.4, 6.9), texture=ribs)
    s.pixels_at(skin[HIGHLIGHT], [(6, 5), (6, 6), (5, 7)])
    s.pixels_at(ramp("#c8d050")[BASE], [(8, 11), (6, 9), (9, 8)])
    # The stem sits on the fruit's axis, at the narrow top.
    tip = (7.8 + 7.4 * sin, 8.6 - 7.4 * cos)
    s.pixels_at(ramp("#6b4a2a")[SHADOW], [(round(tip[0] - 0.5), round(tip[1] - 0.5)), (round(tip[0] + sin - 0.5), round(tip[1] - cos - 0.5))])
    return s


def ume_plum_hand():
    s = Sprite()
    skin, blush = ramp("#b8bc40"), ramp("#d86a3a")

    def colour(x, y):
        return blush if math.dist((x + 0.5, y + 0.5), (4.4, 8.4)) < 2.1 else skin
    # Two small ume, one behind the other, both round.
    s.ellipse(skin, 10.4, 6.4, 4.2, 4.2, texture=speckle(41, 0.12, up=0))
    s.ellipse(colour, 6.0, 10.4, 4.7, 4.6, texture=speckle(43, 0.12, up=0))
    s.pixels_at(skin[SHADOW], [(7, 6), (7, 7), (8, 8), (8, 9)])
    s.pixels_at(skin[HIGHLIGHT], [(8, 4), (9, 3)])
    s.pixels_at(blush[HIGHLIGHT], [(3, 8)])
    s.pixels_at(ramp("#6b4a2a")[SHADOW], [(11, 1), (12, 0)])
    s.pixels_at(LEAF[BASE], [(13, 0), (14, 0), (13, 1)])
    return s


def grape_hand():
    s = Sprite()
    berry = ramp("#86c044")
    # Berries of different sizes placed by hand, the bunch leaning and tapering off to one side, back rows first.
    berries = [(5.0, 3.8, 2.1), (8.6, 3.4, 2.3), (12.0, 4.6, 1.9), (3.0, 7.0, 1.9), (6.6, 6.9, 2.3), (10.3, 7.6, 2.2),
               (13.4, 8.6, 1.7), (4.8, 10.4, 2.1), (8.4, 10.8, 2.3), (11.8, 11.4, 1.8), (6.9, 13.9, 1.9), (10.0, 14.4, 1.6)]
    for cx, cy, r in berries:
        s.ellipse(berry, cx, cy, r, r, light=(-0.7, -0.7))
        if r > 2.0:
            s.put(int(cx) - 1, int(cy) - 1, berry[HIGHLIGHT])
    s.pixels_at(ramp("#6b5a2a")[SHADOW], [(8, 0), (7, 1)])
    s.pixels_at(LEAF[BASE], [(9, 0), (10, 0), (10, 1), (11, 1)])
    # A curling tendril.
    s.pixels_at(LEAF[LIGHT], [(5, 0), (4, 0), (3, 1), (4, 2)])
    return s

def natural_bunch(colour, berries, bloom=None, spread=1.0):
    """A grape bunch from hand-placed berries of varied size, back rows first, with a stem, leaf, and tendril."""
    s = Sprite()
    berry = ramp(colour, spread=spread)
    for i, (cx, cy, r) in enumerate(berries):
        s.ellipse(berry, cx, cy, r, r, light=(-0.7, -0.7))
        if r > 2.0:
            s.put(int(cx) - 1, int(cy) - 1, berry[HIGHLIGHT])
        if bloom and i % 3 == 0:
            s.put(int(cx), int(cy), ramp(bloom, spread=0.5)[BASE])
    s.pixels_at(ramp("#6b5a2a")[SHADOW], [(8, 0), (7, 1)])
    s.pixels_at(LEAF[BASE], [(9, 0), (10, 0), (10, 1), (11, 1)])
    s.pixels_at(LEAF[LIGHT], [(5, 0), (4, 0), (3, 1), (4, 2)])
    return s


def white_grape_hand():
    return natural_bunch("#cfdc74", [(4.4, 3.8, 2.2), (8.2, 3.6, 2.2), (11.8, 4.2, 2.0), (2.8, 7.4, 1.8), (6.4, 7.2, 2.3),
                                    (10.2, 7.4, 2.3), (13.2, 7.8, 1.6), (4.6, 10.8, 2.2), (8.4, 10.6, 2.3), (11.6, 10.6, 1.9),
                                    (6.6, 13.8, 2.0), (9.6, 13.6, 1.7)])


def red_grape_hand():
    return natural_bunch("#9c2c62", [(5.6, 3.6, 2.3), (9.4, 3.8, 2.2), (12.6, 5.0, 1.8), (3.4, 6.6, 2.0), (7.4, 7.2, 2.3),
                                    (11.0, 7.8, 2.2), (4.2, 10.2, 2.1), (8.0, 10.8, 2.3), (11.4, 11.2, 1.8), (5.8, 13.6, 1.9),
                                    (9.0, 14.0, 1.8)], bloom="#b08ac0", spread=0.75)


def strawberry_hand():
    s = Sprite()
    red = ramp("#e0303a")

    def inside(x, y):
        # Broad shoulders under the leaves, tapering to a rounded point.
        if not 3.6 <= y <= 15.4:
            return False
        t = (y - 3.6) / 11.8
        # A full, rounded heart: it stays broad through the middle and rounds off at the tip.
        half = 6.4 * (1 - t ** 2.6) ** 0.6
        return abs(x - 7.6) <= max(0.6, half)
    s.shape(inside, red, (7.2, 8.6), (5.6, 6.0))
    seed, pit = ramp("#ecc22a")[BASE], red[SHADOW]
    for x, y in [(4, 6), (7, 6), (10, 6), (5, 8), (8, 8), (11, 8), (3, 9), (6, 10), (9, 10), (4, 12), (7, 12), (10, 11), (6, 14), (8, 13)]:
        if s.pixels[y][x] not in (None, red[OUTLINE]):
            s.put(x, y, seed)
            if s.pixels[y + 1][x] not in (None, red[OUTLINE]):
                s.put(x, y + 1, pit)
    s.pixels_at(red[HIGHLIGHT], [(3, 6), (4, 5)])
    leaf = ramp("#3f9a3a")
    s.pixels_at(leaf[BASE], [(3, 3), (4, 4), (5, 3), (6, 4), (7, 3), (9, 3), (10, 4), (11, 3), (12, 4), (6, 2), (9, 2)])
    s.pixels_at(leaf[SHADOW], [(5, 4), (8, 4), (11, 4), (2, 4), (13, 4)])
    s.pixels_at(leaf[LIGHT], [(4, 3), (10, 3), (7, 2)])
    s.pixels_at(ramp("#5a7a2a")[SHADOW], [(8, 1), (8, 2), (9, 0)])
    return s


def raspberry_hand():
    s = Sprite()
    red = ramp("#c42a4c")

    def inside(x, y):
        # A rounded cone, a little wider at the top.
        if not 4.0 <= y <= 15.2:
            return False
        t = (y - 4.0) / 11.2
        return abs(x - 7.5) <= 5.2 * math.sqrt(max(0, 1 - (t - 0.3) ** 2 / 0.7 ** 2)) * (1.05 - 0.25 * t)
    mask = s.shape(inside, red, (7.0, 9.0), (5.2, 5.6))
    # Drupelets: a lattice of little beads, each lit on top and shaded below.
    for y in range(5, 15):
        for x in range(2, 14):
            if (x + (y // 2) % 2 * 1) % 2 == 0 and y % 2 == 1 and (x, y) in mask and s.pixels[y][x] != red[OUTLINE]:
                s.put(x, y, red[LIGHT if x < 8 else BASE])
                if (x, y + 1) in mask and s.pixels[y + 1][x] != red[OUTLINE]:
                    s.put(x, y + 1, red[SHADOW])
    s.pixels_at(red[HIGHLIGHT], [(4, 7), (5, 6)])
    leaf = ramp("#4f8a3a")
    s.pixels_at(leaf[BASE], [(5, 3), (6, 3), (7, 2), (8, 3), (9, 3), (10, 3), (4, 4), (11, 4)])
    s.pixels_at(leaf[SHADOW], [(7, 3), (6, 4), (9, 4)])
    s.pixels_at(ramp("#5a7a2a")[SHADOW], [(7, 1), (7, 0)])
    return s


def redcurrant_hand():
    s = Sprite()
    red = ramp("#e23a24", spread=0.8)
    stem = ramp("#a8a032")
    # The stem sweeps in from the bottom left and runs on up through the cluster, so the berries lie along it.
    s.pixels_at(stem[BASE], [(0, 15), (1, 14), (2, 14), (3, 13), (4, 12), (5, 11), (6, 10), (7, 9), (8, 8), (9, 7), (10, 6),
                             (11, 5), (12, 4)])
    s.pixels_at(stem[SHADOW], [(1, 15), (3, 14)])
    s.pixels_at(ramp("#6b4226")[SHADOW], [(0, 15)])
    # Glossy berries in two rows either side of the stem, the upper row behind. Each has a white shine, some a dark tip.
    berries = [(6.0, 7.0, 2.6, (4, 6)), (9.6, 4.6, 2.9, (9, 2)), (13.2, 3.0, 2.4, (14, 2)),
               (7.6, 11.2, 2.7, (8, 13)), (11.2, 8.6, 2.9, (13, 9)), (14.0, 6.6, 2.0, None)]
    shine = ramp("#fff2dc")[BASE]
    for cx, cy, r, tip in berries:
        s.ellipse(red, cx, cy, r, r, light=(-0.6, -0.8))
        s.put(int(cx) - 1, int(cy), shine)
        s.put(int(cx) - 1, int(cy) - 2, red[HIGHLIGHT])
        if tip:
            s.put(*tip, ramp("#5a2a1a")[BASE])
    return s

def elderberry_hand():
    s = Sprite()
    stem = ramp("#8a2a4a")
    # One red-purple stalk that forks into thin stems under a broad, dense head of berries.
    s.line(stem[SHADOW], 8, 15, 8, 11)
    for x1, y1 in [(3, 8), (6, 7), (10, 7), (13, 8)]:
        s.line(stem[BASE], 8, 11, x1, y1)
    berry, shine, deep = ramp("#241c34", spread=0.5), ramp("#6a78a0")[BASE], ramp("#100a16")[BASE]
    rng = random.Random(9)

    def head(x, y):
        return ((x - 8.0) / 7.2) ** 2 + ((y - 5.6) / 4.6) ** 2 <= 1 and y <= 9.0
    for y in range(SIZE):
        for x in range(SIZE):
            if not head(x + 0.5, y + 0.5):
                continue
            edge = not all(head(x + 0.5 + dx, y + 0.5 + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if edge and rng.random() < 0.4:
                continue  # A bumpy outline where berries stick out or leave gaps.
            # Tiny black berries, each two pixels across: a glint on some, deep shade in the gaps between.
            cell = (x // 2 + (y // 2) % 2) % 2
            if x % 2 == 0 and y % 2 == 0 and rng.random() < 0.55:
                s.put(x, y, shine)
            elif (x % 2 == 1 and y % 2 == 1) or cell:
                s.put(x, y, deep if rng.random() < 0.5 else berry[SHADOW])
            else:
                s.put(x, y, berry[BASE])
    leaf = ramp("#4f8a3a")
    s.pixels_at(leaf[BASE], [(1, 12), (2, 12), (2, 13), (3, 13), (3, 14), (4, 14), (2, 11)])
    s.pixels_at(leaf[LIGHT], [(1, 11), (3, 12)])
    s.pixels_at(leaf[SHADOW], [(4, 13), (5, 14)])
    return s


def bell_pepper_hand():
    s = Sprite()
    red = ramp("#d8241e")
    lobes = [(4.8, 9.8, 3.0, 5.4), (7.8, 10.4, 2.8, 5.4), (10.6, 9.8, 3.0, 5.4)]

    def inside(x, y):
        return any(((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1 for cx, cy, rx, ry in lobes)
    s.shape(inside, red, (7.6, 9.4), (6.2, 5.6))
    # Creases between the lobes, and glossy streaks down the front.
    s.pixels_at(red[SHADOW], [(6, 6), (6, 7), (6, 8), (6, 9), (6, 10), (6, 11), (9, 6), (9, 7), (9, 8), (9, 9), (9, 10), (9, 11)])
    s.pixels_at(red[OUTLINE], [(6, 14), (9, 14)])
    s.pixels_at(red[HIGHLIGHT], [(3, 7), (3, 8), (3, 9), (7, 7), (7, 8)])
    s.pixels_at(red[LIGHT], [(4, 6), (11, 7), (11, 8)])
    green = ramp("#3f8a2a")
    s.pixels_at(green[SHADOW], [(5, 4), (6, 4), (7, 4), (8, 4), (9, 4), (10, 4)])
    s.pixels_at(green[BASE], [(7, 3), (8, 3), (8, 2), (9, 1), (10, 1)])
    s.pixels_at(green[LIGHT], [(7, 2), (9, 0)])
    return s


def chili(colour, points, radii, calyx, stem_points):
    """A long, glossy chili along a curve, with a green cap at the thick end and a curving stem."""
    s = Sprite()
    # A softer ramp, so the thin tip doesn't turn to solid outline.
    tones = ramp(colour, spread=0.7)
    s.capsule(tones, points, radii)
    # A shine running down one side of the pod.
    for (x0, y0), (x1, y1) in zip(points, points[1:]):
        nx, ny = -(y1 - y0), x1 - x0
        length = math.hypot(nx, ny)
        for t in (0.3, 0.7):
            x, y = x0 + (x1 - x0) * t - nx / length * 0.8, y0 + (y1 - y0) * t - ny / length * 0.8
            if s.pixels[int(y)][int(x)] not in (None, tones[OUTLINE]):
                s.put(int(x), int(y), tones[HIGHLIGHT])
    green = ramp("#3f8a2a")
    s.pixels_at(green[BASE], calyx)
    s.pixels_at(green[SHADOW], calyx[::3])
    s.pixels_at(green[LIGHT], stem_points)
    return s


def red_chili_hand():
    return chili("#d4221a", [(11.6, 3.4), (9.6, 6.8), (7.0, 10.4), (4.4, 13.0), (2.6, 13.4)], (2.2, 0.8),
                 [(11, 2), (12, 2), (13, 3), (12, 3)], [(13, 1), (14, 0)])


def green_chili_hand():
    return chili("#3c8c26", [(2.0, 12.4), (5.6, 12.0), (9.2, 10.6), (12.0, 8.4)], (1.1, 2.1),
                 [(12, 7), (13, 7), (13, 8), (12, 6)], [(14, 6), (14, 5), (15, 4)])


def paprika_hand():
    s = Sprite()
    red = ramp("#d0301c")
    points = [(5.0, 4.6), (8.2, 9.0), (10.6, 13.0)]
    s.capsule(red, points, (3.0, 2.3))
    # Folds down its length and a glossy shine; the tip stays blunt and rounded.
    s.pixels_at(red[SHADOW], [(6, 7), (7, 8), (8, 10), (9, 11), (10, 12)])
    s.pixels_at(red[HIGHLIGHT], [(4, 6), (5, 7), (6, 9)])
    s.pixels_at(red[LIGHT], [(5, 6), (7, 10), (8, 12)])
    green = ramp("#3f8a2a")
    s.pixels_at(green[BASE], [(3, 3), (4, 3), (5, 2), (6, 3), (4, 2)])
    s.pixels_at(green[SHADOW], [(5, 3), (3, 2)])
    s.pixels_at(green[LIGHT], [(4, 1), (3, 0), (2, 0)])
    return s

def poblano_hand():
    s = Sprite()
    green = ramp("#173c18", spread=0.6)
    # Wide shoulders tapering to a point, very dark and glossy.
    s.capsule(green, [(6.0, 5.2), (8.6, 9.4), (10.6, 13.6)], (3.9, 0.7))
    shine = ramp("#b8d8b0")[BASE]
    s.pixels_at(shine, [(4, 5), (4, 6), (5, 8), (6, 10)])
    s.pixels_at(green[HIGHLIGHT], [(5, 4), (5, 7), (7, 11), (8, 12)])
    stem = ramp("#4f8a2a")
    s.pixels_at(stem[SHADOW], [(5, 2), (6, 2), (7, 2), (4, 3), (8, 3)])
    s.pixels_at(stem[BASE], [(5, 1), (4, 0), (3, 0)])
    return s


def birds_eye_hand():
    s = Sprite()
    red = ramp("#e8261c", spread=0.55)
    stem = ramp("#4f9a2a")
    # Three small, slender chilies on long green stems, pointing every which way.
    pods = [([(4.6, 7.0), (3.0, 11.0), (1.8, 13.6)], [(5, 6), (6, 4), (7, 2), (8, 1)]),
            ([(8.0, 7.4), (8.2, 11.4), (7.6, 14.4)], [(8, 6), (8, 5), (8, 4), (8, 3), (8, 2)]),
            ([(11.0, 6.6), (12.8, 10.4), (14.2, 12.8)], [(10, 5), (10, 4), (9, 3), (9, 2), (9, 1)])]
    for points, stalk in pods:
        s.pixels_at(stem[BASE], stalk)
        s.capsule(red, points, (1.9, 0.8))
        s.pixels_at(stem[SHADOW], [stalk[0]])
        x0, y0 = points[0]
        s.put(int(x0) - 1 if x0 > 1 else int(x0), int(y0) + 1, red[HIGHLIGHT])
    return s


def aji_amarillo_hand():
    s = Sprite()
    orange = ramp("#f08a1c", spread=0.8)
    stem = ramp("#5a9a2a")
    # A pair of long, blunt-tipped golden pods on thin green stems.
    s.pixels_at(stem[BASE], [(5, 4), (5, 3), (6, 2), (6, 1), (7, 0), (10, 3), (10, 2), (9, 1), (9, 0)])
    s.capsule(orange, [(10.4, 4.6), (11.8, 9.0), (12.4, 13.6)], (2.2, 1.6))
    s.capsule(orange, [(5.0, 5.4), (5.6, 10.0), (6.8, 14.0)], (2.4, 1.7))
    s.pixels_at(orange[HIGHLIGHT], [(4, 7), (4, 8), (4, 9), (10, 6), (11, 8)])
    s.pixels_at(stem[SHADOW], [(5, 4), (10, 3)])
    return s


def cabbage_head(heart, outer, vein):
    """A cabbage: a round heart cupped below by darker outer leaves, with pale veins fanning up from the base."""
    s = Sprite()
    leaves = ramp(outer)
    s.ellipse(leaves, 3.2, 11.8, 3.0, 3.8, angle=40)
    s.ellipse(leaves, 12.8, 11.8, 3.0, 3.8, angle=-40)
    s.ellipse(leaves, 8.0, 13.2, 4.6, 2.4)
    head = ramp(heart)
    s.ellipse(head, 8.0, 7.6, 5.7, 5.6)
    pale = ramp(vein)
    # The midrib up the middle, side veins curving out from it, and the edge of the top leaf.
    s.line(pale[LIGHT], 8, 12, 8, 5)
    for points in ([(8, 10), (6, 8), (5, 6), (4, 5)], [(8, 10), (10, 8), (11, 6), (12, 5)], [(7, 12), (5, 11), (3, 10)],
                   [(9, 12), (11, 11), (13, 10)]):
        s.pixels_at(pale[BASE], points)
    s.pixels_at(head[LIGHT], [(5, 3), (6, 3), (7, 2), (8, 2)])
    s.pixels_at(leaves[LIGHT], [(1, 11), (2, 10), (14, 10)])
    s.pixels_at(head[HIGHLIGHT], [(4, 5), (5, 4)])
    return s


def cabbage_hand():
    return cabbage_head("#a6d474", "#5a9a3a", "#e8f2c8")


def red_cabbage_hand():
    s = cabbage_head("#7c2c74", "#5a1f5e", "#d8b0d8")
    bloom = ramp("#8a8ac8", spread=0.5)[LIGHT]
    s.pixels_at(bloom, [(4, 7), (12, 7), (7, 4)])
    return s

def lettuce_hand():
    s = Sprite()
    leaf = ramp("#6cc23a")

    def inside(x, y):
        # A bushy head whose edge ruffles in and out.
        angle = math.atan2(y - 9.0, x - 8.0)
        reach = 6.6 + 0.8 * math.sin(angle * 7 + 0.6)
        return ((x - 8.0) / reach) ** 2 + ((y - 9.0) / (reach * 0.95)) ** 2 <= 1 and y <= 15.4

    def ruffles(x, y):
        # Wavy folds: light crests and shaded troughs that curl around the head.
        angle, radius = math.atan2(y + 0.5 - 9.0, x + 0.5 - 8.0), math.dist((x + 0.5, y + 0.5), (8.0, 9.0))
        ridge = math.sin(angle * 5 + radius * 1.3)
        return 1 if ridge > 0.55 else -1 if ridge < -0.6 else 0
    s.shape(inside, leaf, (7.6, 8.4), (6.6, 6.4), texture=ruffles)
    s.pixels_at(ramp("#e2f0b8")[BASE], [(7, 14), (8, 14), (8, 13)])
    return s


def tube_bands(s, tones, start, end, radius):
    """Paints light and shade in bands along a straight tube, so it reads as a cylinder: a shine near the lit side,
    a light band beside it, and shade along the far side."""
    (x0, y0), (x1, y1) = start, end
    length = math.dist(start, end)
    ux, uy = (x1 - x0) / length, (y1 - y0) / length
    nx, ny = uy, -ux  # Points toward the top left, where the light comes from.
    for y in range(SIZE):
        for x in range(SIZE):
            if s.pixels[y][x] in (None, tones[OUTLINE]):
                continue
            px, py = x + 0.5 - x0, y + 0.5 - y0
            along, across = px * ux + py * uy, px * nx + py * ny
            if not 0.6 <= along <= length - 0.6:
                continue
            side = across / radius
            tone = HIGHLIGHT if 0.25 < side <= 0.6 else LIGHT if 0.6 < side else SHADOW if side < -0.45 else BASE
            s.put(x, y, tones[tone])


def courgette_hand():
    s = Sprite()
    skin = ramp("#2f6c26", spread=0.85)
    start, end, radius = (2.6, 12.4), (12.6, 5.6), 2.7
    # Straight and evenly thick, with flat, blunt ends.
    cos = (end[0] - start[0]) / math.dist(start, end)
    sin = (end[1] - start[1]) / math.dist(start, end)

    def inside(x, y):
        along = (x - start[0]) * cos + (y - start[1]) * sin
        across = -(x - start[0]) * sin + (y - start[1]) * cos
        length = math.dist(start, end)
        return -1.2 <= along <= length + 0.8 and abs(across) <= radius
    s.shape(inside, skin, ((start[0] + end[0]) / 2, (start[1] + end[1]) / 2), (6, 6))
    tube_bands(s, skin, start, end, radius)
    # Fine pale flecks.
    for x, y in [(4, 12), (6, 11), (9, 9), (11, 8), (5, 13), (8, 11)]:
        if s.pixels[y][x] == skin[BASE]:
            s.put(x, y, ramp("#7aa85a")[BASE])
    # The tan, five-sided stem cap on the end, and the dry blossom scar on the other.
    tan = ramp("#c8b070")
    s.pixels_at(tan[BASE], [(14, 4), (14, 5), (15, 4)])
    s.pixels_at(tan[SHADOW], [(13, 5), (14, 6)])
    s.pixels_at(ramp("#8a7a3a")[BASE], [(1, 13)])
    return s


def cucumber_hand():
    s = Sprite()
    skin = ramp("#2c6222", spread=0.8)
    points = [(2.2, 12.0), (7.6, 10.4), (13.2, 5.8)]
    s.capsule(skin, points, (2.2, 2.4))
    # Little pale bumps all over.
    bump = ramp("#9ac070")[BASE]
    for x, y in [(3, 12), (5, 11), (7, 10), (9, 9), (11, 7), (12, 6), (4, 13), (6, 12), (8, 11), (10, 10), (6, 10), (10, 8)]:
        if s.pixels[y][x] not in (None, skin[OUTLINE]):
            s.put(x, y, bump)
    s.pixels_at(skin[HIGHLIGHT], [(4, 10), (6, 9), (9, 7)])
    s.pixels_at(ramp("#6a7a2a")[SHADOW], [(14, 4), (15, 3)])
    return s


def tomatillo_hand():
    s = Sprite()
    husk = ramp("#d2d8a8", spread=0.6)
    vein = ramp("#a4ac72")[BASE]
    # Papery husk flaps behind and beside the fruit, peeled back so the green fruit shows at the front.
    for cx, cy, angle in [(3.6, 8.0, 60), (12.4, 8.0, -60), (8.0, 4.4, 0)]:
        mask = s.ellipse(husk, cx, cy, 4.2, 2.6, angle=angle)
        for x, y in mask:
            if (x + y) % 3 == 0 and s.pixels[y][x] != husk[OUTLINE]:
                s.put(x, y, vein)
    fruit = ramp("#7cb44a")
    s.ellipse(fruit, 8.0, 10.0, 5.0, 4.8)
    s.pixels_at(fruit[HIGHLIGHT], [(5, 8), (6, 7)])
    s.pixels_at(fruit[LIGHT], [(5, 9), (7, 7)])
    s.pixels_at(ramp("#5a7a2a")[SHADOW], [(8, 1), (8, 0)])
    return s


def thai_aubergine_hand():
    s = Sprite()
    pale, stripe = ramp("#b8d4a0"), ramp("#4f8a3a")
    cx, cy, r = 8.0, 9.8, 5.4

    def skin(x, y):
        # Green stripes running from the cap to the base, around the curve of the fruit.
        span = math.sqrt(max(0.01, r * r - (y + 0.5 - cy) ** 2))
        longitude = math.asin(max(-1, min(1, (x + 0.5 - cx) / span)))
        return stripe if (longitude * 2.2) % 1.0 < 0.42 else pale
    s.ellipse(skin, cx, cy, r, r * 0.96)
    s.pixels_at(pale[HIGHLIGHT], [(5, 7), (5, 8)])
    cap = ramp("#3f7a2a")
    s.pixels_at(cap[BASE], [(5, 4), (6, 4), (7, 4), (8, 4), (9, 4), (10, 4), (11, 5), (4, 5), (6, 5), (9, 5)])
    s.pixels_at(cap[SHADOW], [(7, 5), (8, 5), (10, 5)])
    s.pixels_at(cap[LIGHT], [(8, 3), (8, 2), (9, 1), (10, 0)])
    return s

def radish_hand():
    s = Sprite()
    leaf = ramp("#62b840")
    stalk = ramp("#a0d070")
    # Tall leafy greens on pale stalks, fanning up from a small round root.
    for x1, y1 in [(4, 4), (8, 3), (12, 4)]:
        s.line(stalk[BASE], 8, 9, x1, y1)
    for cx, cy, rx, ry, angle in [(3.8, 3.6, 2.9, 2.0, 55), (12.2, 3.6, 2.9, 2.0, -55), (8.0, 2.6, 2.0, 2.6, 0)]:
        s.ellipse(leaf, cx, cy, rx, ry, angle=angle, texture=lambda x, y: -1 if (x + y) % 3 == 0 else 0)
    root = ramp("#c8204a")
    s.ellipse(root, 8.0, 11.2, 3.4, 3.2)
    s.pixels_at(root[HIGHLIGHT], [(6, 10), (6, 9)])
    tail = ramp("#ead0d6")
    s.pixels_at(tail[BASE], [(8, 14), (9, 15)])
    s.pixels_at(tail[SHADOW], [(8, 15)])
    return s


SHALLOT = [  # One shallot, pixel by pixel: plump low down, tapering to a dry tip, with root hairs below.
    "...t...",
    "...o...",
    "..olo..",
    "..olbo.",
    ".olbbo.",
    ".olhbso",
    "olhbdso",
    "olhbdso",
    "olbbdso",
    "olbbsso",
    ".obbso.",
    "..ooo..",
    "..r.r..",
]


def shallot_hand():
    s = Sprite()
    skin = ramp("#c47e62")
    dry, roots = ramp("#8a5a3a")[BASE], ramp("#c8b48a")[BASE]
    # Two shallots leaning together, the front one drawn last.
    for x0, y0, mirror in ((8, 1, True), (1, 3, False)):
        stamp(s, SHALLOT, skin, x0, y0, mirror=mirror)
        width = len(SHALLOT[0])
        for y, row in enumerate(SHALLOT):
            for x, ch in enumerate(row):
                px = x0 + (width - 1 - x if mirror else x)
                if ch == "t":
                    s.put(px, y0 + y, dry)
                elif ch == "r":
                    s.put(px, y0 + y, roots)
    return s


def asparagus_hand():
    s = Sprite()
    green = ramp("#5c9a3a")
    # Straight, upright spears side by side, two pixels wide, lit on the left, tops at slightly different heights.
    for x, top in ((1, 4), (4, 2), (7, 3), (10, 2), (13, 5)):
        for y in range(top + 1, 15):
            s.put(x, y, green[LIGHT])
            s.put(x + 1, y, green[BASE])
            # Small scales up the stem, a darker green on the shaded side.
            if (y - top) % 4 == 2:
                s.put(x + 1, y, green[SHADOW])
                s.put(x, y, green[BASE])
        # A pointed, scaly tip with only a faint purple touch.
        s.put(x, top, green[BASE])
        s.put(x + 1, top, ramp("#7a6a8a")[BASE])
        s.put(x, top - 1, green[SHADOW])
        s.put(x + 1, top + 1, green[SHADOW])
        # Pale cut ends.
        s.pixels_at(ramp("#d2e0a8")[BASE], [(x, 15), (x + 1, 15)])
    return s

def rings(s, tones, start, end, spacing, reach, tone=SHADOW, colour=None):
    """Ring scars across a root: short lines at right angles to its length, kept to pixels the root already covers."""
    (x0, y0), (x1, y1) = start, end
    length = math.dist(start, end)
    ux, uy = (x1 - x0) / length, (y1 - y0) / length
    t = spacing / 2
    while t < length:
        cx, cy = x0 + ux * t, y0 + uy * t
        for k in [i / 2 for i in range(-int(reach * 2), int(reach * 2) + 1)]:
            x, y = int(cx - uy * k), int(cy + ux * k)
            if 0 <= x < SIZE and 0 <= y < SIZE and s.pixels[y][x] not in (None, tones[OUTLINE]):
                s.put(x, y, colour or tones[tone])
        t += spacing


def curve(a, b, c, steps=40):
    """Points along a quadratic curve from a to c, bending toward b, with how far along each point is."""
    return [((1 - t) ** 2 * a[0] + 2 * (1 - t) * t * b[0] + t * t * c[0], (1 - t) ** 2 * a[1] + 2 * (1 - t) * t * b[1] + t * t * c[1], t)
            for t in [i / steps for i in range(steps + 1)]]


def rhubarb_colours():
    red, pink, green = ramp("#b8243a"), ramp("#d4566a"), ramp("#9aaa4a")

    def colour(x, y):
        # Deep red at the base, pink through the middle, green-tinged toward the top.
        return red if y >= 11 else pink if y >= 7 else green
    return red, pink, colour


def rhubarb_single():
    """One stalk on a diagonal, ending in a broad crinkled leaf."""
    s = Sprite()
    red, pink, colour = rhubarb_colours()
    s.capsule(colour, [(2.4, 15.0), (9.0, 6.4)], (1.6, 1.3))
    s.pixels_at(red[LIGHT], [(3, 13), (4, 12)])
    s.pixels_at(pink[LIGHT], [(6, 9), (7, 8)])
    leaf = ramp("#4f9a34")
    s.ellipse(leaf, 10.6, 4.4, 4.8, 3.6, angle=-30, texture=lambda x, y: -1 if (x * 2 + y) % 4 == 0 else 0)
    vein = ramp("#c8d888")[BASE]
    s.line(vein, 9, 6, 13, 2)
    s.pixels_at(vein, [(10, 3), (12, 5)])
    return s


def rhubarb_trimmed():
    """Three trimmed stalks side by side, leaves cut off, the cut tops pale green."""
    s = Sprite()
    red, pink, green = ramp("#b8243a"), ramp("#d4566a"), ramp("#a8b858")

    def colour(x, y):
        # Red low down, pink up most of the stalk, and a short green-tinged top where the leaf was cut away.
        return red if y >= 10 else pink if y >= 4 else green
    # Three parallel stalks, all leaning the same way to the right.
    stalks = [((1.8, 15.0), (5.2, 2.6)), ((5.8, 15.0), (9.2, 2.6)), ((9.8, 15.0), (13.2, 2.6))]
    for start, end in stalks:
        s.capsule(colour, [start, end], (1.4, 1.3))
    # A light streak down the left side of each stalk, and the pale cut at the top.
    cut = ramp("#d8e0a0")[BASE]
    for (x0, y0), (x1, y1) in stalks:
        for y in (13, 11, 8, 6):
            x = int(x0 + (x1 - x0) * (y0 - y) / (y0 - y1) - 0.6)
            if s.pixels[y][x] is not None:
                s.put(x, y, (red if y >= 10 else pink)[LIGHT])
        s.pixels_at(cut, [(int(x1) - 1, int(y1) - 1), (int(x1), int(y1) - 1)])
    return s


def rhubarb_hand():
    return rhubarb_trimmed()


def sweet_potato_hand():
    s = Sprite()
    skin = ramp("#b65a42")
    points = curve((1.6, 12.6), (6.0, 8.6), (14.2, 4.4))

    def inside(x, y):
        return any(math.dist((x, y), (px, py)) <= 0.6 + 2.9 * math.sin(math.pi * t) ** 0.75 for px, py, t in points)
    mask = s.shape(inside, skin, (7.4, 7.8), (6.0, 5.0))
    # Shallow creases along its length, and a few dark eyes.
    for px, py, t in points[8:34:3]:
        x, y = int(px + 1.2), int(py + 1.0)
        if (x, y) in mask and s.pixels[y][x] != skin[OUTLINE]:
            s.put(x, y, skin[SHADOW])
    s.pixels_at(ramp("#5a2a1e")[BASE], [(6, 9), (10, 6), (8, 10)])
    s.pixels_at(skin[HIGHLIGHT], [(5, 8), (6, 7), (8, 6)])
    return s


def cassava_hand():
    s = Sprite()
    bark = ramp("#7c4e30", spread=0.9)
    # Two long roots tapering away, rough-barked with ring scars; the front one is cut to show its white flesh.
    s.capsule(bark, [(4.6, 15.0), (14.4, 9.0)], (1.9, 0.6), texture=speckle(61, 0.4))
    rings(s, bark, (4.6, 15.0), (14.4, 9.0), 2.6, 1.6)
    s.capsule(bark, [(2.6, 12.0), (13.6, 2.6)], (2.6, 0.7), texture=speckle(67, 0.4))
    rings(s, bark, (2.6, 12.0), (13.6, 2.6), 2.8, 2.2)
    s.ellipse(ramp("#efe6cf", spread=0.5), 2.4, 12.2, 1.8, 1.8, outline=False)
    s.pixels_at(ramp("#d8a8a0")[BASE], [(1, 11), (1, 13), (3, 14)])
    return s


def ginger_hand():
    s = Sprite()
    skin = ramp("#d6b278")
    # A knobbly hand: a body with fingers branching up and a knob at one end, each with ring lines across it.
    parts = [((2.4, 11.0), (11.6, 10.0), 2.5), ((5.0, 10.0), (4.2, 4.4), 1.8), ((8.8, 9.6), (11.2, 4.2), 1.7),
             ((11.6, 10.4), (14.0, 12.6), 1.7)]
    for start, end, r in parts:
        s.capsule(skin, [start, end], (r, r * 0.85))
    for start, end, r in parts:
        rings(s, skin, start, end, 2.4, r * 0.8)
    s.pixels_at(skin[HIGHLIGHT], [(3, 9), (4, 9), (4, 4), (10, 4)])
    s.pixels_at(ramp("#f0dca8")[BASE], [(4, 3), (11, 3)])
    return s


def turmeric_hand():
    s = Sprite()
    skin = ramp("#c47c34")
    # Plump little fingers with fine rings; one is snapped to show the bright orange inside.
    fingers = [((1.8, 12.8), (8.8, 8.4), 2.1), ((6.0, 14.4), (13.6, 11.0), 1.9), ((6.4, 9.2), (10.6, 3.4), 1.8)]
    for start, end, r in fingers:
        s.capsule(skin, [start, end], (r, r * 0.85))
        rings(s, skin, start, end, 2.2, r * 0.75)
    s.ellipse(ramp("#f4941a"), 10.8, 3.2, 1.8, 1.8, outline=False)
    s.pixels_at(ramp("#ffc04a")[BASE], [(10, 2), (11, 3)])
    s.pixels_at(skin[HIGHLIGHT], [(3, 11), (4, 10), (7, 7)])
    return s

def galangal_hand():
    s = Sprite()
    skin = ramp("#ead8aa", spread=0.6)
    band = ramp("#a8784a")[BASE]
    # A pale rhizome: a body, a finger reaching up and left with a cut end, and a knob on the right with a cut face.
    parts = [((3.4, 11.6), (11.2, 9.0), 2.5), ((5.0, 10.6), (2.6, 4.4), 1.8), ((11.0, 9.4), (13.0, 6.0), 2.1)]
    for start, end, r in parts:
        s.capsule(skin, [start, end], (r, r * 0.9))
    # Thin brown ring bands, well spaced, so the cream skin shows between them.
    for start, end, r in parts:
        rings(s, skin, start, end, 3.0, r * 0.7, colour=band)
    cut = ramp("#f4e8c4", spread=0.4)
    s.ellipse(cut, 2.6, 4.0, 1.4, 1.2, outline=False)
    s.ellipse(cut, 13.2, 5.8, 1.6, 1.4, outline=False)
    s.pixels_at(ramp("#e07a8a")[BASE], [(2, 13), (3, 13), (2, 12)])
    s.pixels_at(skin[HIGHLIGHT], [(5, 9), (8, 8)])
    return s


def thyme_hand():
    s = Sprite()
    stem = ramp("#8a7a4a")
    leaf = ramp("#5e9a40")
    # Three thin sprigs gathered at the bottom right, with tiny paired leaves spaced along each.
    for x1, y1 in ((1, 6), (4, 1), (9, 0)):
        points = s.line(stem[SHADOW], 14, 15, x1, y1)
        for i, (x, y) in enumerate(points[2:], start=2):
            if i % 3 == 0:
                s.pixels_at(leaf[BASE], [(x, y - 1), (x + 1, y + 1)])
                s.put(x - 1, y, leaf[LIGHT])
        s.pixels_at(leaf[LIGHT], [points[-1], (points[-1][0] + 1, points[-1][1])])
    return s


def mint_hand():
    s = Sprite()
    # Short reddish stems gathered and tied at the bottom right; the rest is a bushy mass of leaves.
    stem = ramp("#8a3a3a")
    for x0, y0 in ((8, 10), (10, 9), (12, 10), (9, 12)):
        s.line(stem[BASE], 13, 15, x0, y0)
    shades = ["#3a963a", "#46a842", "#3e9e3c", "#52b04a"]
    # Oval leaves layered from the back of the bunch to the front, filling most of the icon.
    leaves = [(4.0, 3.0, 25), (8.4, 2.2, -10), (12.2, 3.6, -40), (2.6, 7.0, 50), (6.4, 6.0, 10), (10.6, 6.6, -30),
              (13.2, 8.4, -60), (4.0, 10.4, 30), (7.8, 9.6, -5), (11.0, 11.6, -40)]
    for i, (cx, cy, angle) in enumerate(leaves):
        tones = ramp(shades[i % len(shades)], spread=0.75)
        mask = s.ellipse(tones, cx, cy, 2.7, 1.8, angle=angle)
        for x, y in mask:
            if (x + y) % 2 == 0 and s.pixels[y][x] == tones[OUTLINE]:
                s.put(x, y, tones[SHADOW])  # A toothed edge.
        s.put(int(cx), int(cy), tones[SHADOW])  # The vein.
        s.put(int(cx) - 1, int(cy) - 1, tones[HIGHLIGHT])
    s.pixels_at(ramp("#c8a86a")[BASE], [(12, 14), (13, 14), (13, 13)])
    return s


def basil_hand():
    s = Sprite()
    leaf = ramp("#38a02c")
    s.line(ramp("#5a8a2a")[BASE], 8, 15, 8, 4)
    # Big, smooth, cupped leaves in pairs up the stem, glossy, each with a pale midrib.
    for cx, cy, angle in [(4.4, 11.4, 25), (11.6, 11.4, -25), (4.8, 6.6, 35), (11.2, 6.6, -35), (8.0, 3.0, 90)]:
        mask = s.ellipse(leaf, cx, cy, 3.4, 2.3, angle=angle)
        s.put(int(cx), int(cy), leaf[LIGHT])
        s.put(int(cx) - 1, int(cy) - 1, leaf[HIGHLIGHT])
    return s


def parsley_hand():
    s = Sprite()
    # Short stems gathered at the bottom right; the rest is a dense mass of flat, toothed, three-lobed leaves.
    stem = ramp("#62aa40")
    for x0, y0 in ((9, 10), (11, 10), (12, 12), (8, 12)):
        s.line(stem[BASE], 14, 15, x0, y0)
    clusters = [(3.2, 3.4), (8.0, 2.4), (12.4, 4.0), (3.0, 8.4), (7.6, 7.2), (11.8, 8.6), (5.4, 11.8)]
    for i, (cx, cy) in enumerate(clusters):
        tones = ramp(["#46a03a", "#52ac44", "#3e9434"][i % 3], spread=0.6)
        # Three toothed lobes per leaf, each with a light centre so the leaves stay distinct.
        for dx, dy in ((-1.4, 0.6), (1.4, 0.6), (0.0, -1.1)):
            mask = s.ellipse(tones, cx + dx, cy + dy, 1.7, 1.4)
            for x, y in mask:
                if (x * 3 + y) % 4 == 0 and s.pixels[y][x] == tones[OUTLINE]:
                    s.put(x, y, tones[SHADOW])
            s.put(int(cx + dx), int(cy + dy), tones[LIGHT])
        s.put(int(cx), int(cy), tones[HIGHLIGHT])
    return s

def dill_hand():
    s = Sprite()
    stem = ramp("#5a8a3a")
    frond = ramp("#3f7a38")
    # A dense bunch of fine, needle-like fronds; the stems gather at the bottom right.
    rng = random.Random(13)
    branches = [(14, 15, 1, 4), (14, 15, 4, 1), (14, 15, 8, 0), (14, 15, 1, 9), (13, 14, 5, 6), (14, 14, 11, 2), (13, 15, 3, 12)]
    for x0, y0, x1, y1 in branches:
        points = s.line(stem[SHADOW], x0, y0, x1, y1)
        for i, (x, y) in enumerate(points[2:], start=2):
            # Threads either side of each branch, thicker toward the tips.
            for dx, dy in ((0, -1), (-1, 0), (1, -1), (-1, 1), (0, -2)):
                if rng.random() < 0.5:
                    s.put(x + dx, y + dy, frond[[BASE, LIGHT, SHADOW][rng.randrange(3)]])
    return s


def bay_hand():
    s = Sprite()
    stem = ramp("#7a5a2a")
    s.line(stem[BASE], 2, 15, 12, 3)

    def pointed(tones, cx, cy, angle, length, width):
        cos, sin = math.cos(math.radians(angle)), math.sin(math.radians(angle))

        def inside(x, y):
            # A glossy lance-shaped leaf, pointed at both ends.
            u, v = (x - cx) * cos + (y - cy) * sin, -(x - cx) * sin + (y - cy) * cos
            return abs(u) <= length and abs(v) <= width * (1 - (u / length) ** 2)
        s.shape(inside, tones, (cx, cy), (length, length))
        s.put(round(cx - 0.5), round(cy - 0.5), tones[HIGHLIGHT])
    # Dark, glossy leaves set alternately up the sprig, pointing out to either side.
    for i, (cx, cy, angle) in enumerate([(3.6, 10.4, -150), (8.0, 11.4, 30), (6.4, 5.8, -120), (11.4, 7.2, 10), (12.0, 2.4, -40)]):
        pointed(ramp("#3e8a38" if i % 2 else "#4a9842", spread=0.7), cx, cy, angle, 3.6, 1.6)
    s.pixels_at(ramp("#e0c040")[LIGHT], [(9, 4), (5, 9), (10, 9)])
    return s


def barley_hand():
    s = Sprite()
    grain = ramp("#c8a868", spread=0.8)
    awn = ramp("#d8c49a")
    stalk = ramp("#a89a60")
    # A stalk rising from the bottom left into a beige-gold ear on a diagonal.
    s.line(stalk[BASE], 1, 15, 5, 11)
    ear = [(5, 10), (6, 9), (7, 8), (8, 7), (9, 6), (10, 5), (11, 4)]
    # Long whiskers running along the ear and past its tip, as on ripe barley.
    for i, (x, y) in enumerate(ear):
        if i % 2 == 0:
            s.line(awn[LIGHT], x - 1, y - 1, min(15, x + 3), max(0, y - 6))
            s.line(awn[BASE], x + 1, y + 1, min(15, x + 6), max(0, y - 3))
    # Plump kernels in two staggered rows.
    for i, (x, y) in enumerate(ear):
        s.put(x, y, grain[LIGHT])
        s.put(x - 1, y, grain[BASE])
        s.put(x, y + 1, grain[SHADOW])
    return s


def corn_cob(kernel_colour, speckle_colour=None):
    """A straight cob of kernels with husk leaves peeling back at its sides and base, and silk on top."""
    s = Sprite()
    husk = ramp("#a8cc78", spread=0.7)
    kernel = ramp(kernel_colour)

    def kernels(x, y):
        return 1 if (x + y) % 2 == 0 else -1 if y % 2 == 1 else 0
    # Husk leaves peeled back from the base and hugging the sides, behind a broad yellow cob.
    s.capsule(husk, [(3.4, 14.6), (2.6, 8.4)], (1.4, 0.6))
    s.capsule(husk, [(11.6, 14.6), (12.4, 8.4)], (1.4, 0.6))
    s.capsule(husk, [(7.5, 15.0), (7.5, 12.0)], (2.2, 1.6))
    # A straight cob, the same width from base to tip.
    s.capsule(kernel, [(7.5, 3.2), (7.5, 11.6)], (2.9, 2.9), texture=kernels)
    s.pixels_at(kernel[HIGHLIGHT], [(6, 4), (5, 7), (5, 9)])
    if speckle_colour:
        # Kernels of a second colour scattered through, as on heirloom corn.
        s.pixels_at(ramp(speckle_colour)[LIGHT], [(7, 5), (9, 7), (6, 9), (8, 11), (9, 4)])
    # Silk at the top.
    silk = ramp("#a8682a")
    s.pixels_at(silk[BASE], [(6, 0), (7, 1), (8, 0), (9, 1), (7, 0)])
    s.pixels_at(silk[LIGHT], [(8, 1), (6, 1)])
    return s


def corn_hand():
    return corn_cob("#f0c838")


def blue_corn_hand():
    return corn_cob("#3c3c8c", speckle_colour="#7a4a9a")


def rice_hand():
    s = Sprite()
    burlap = ramp("#b89a6a", spread=0.7)
    grain = ramp("#efe9d8", spread=0.5)
    # A burlap sack with a woven texture, its rolled top open on a heap of white grains, and a few spilled.
    def sack(x, y):
        return 5.0 <= y <= 15.4 and abs(x - 7.5) <= 6.0 - max(0, (y - 12.5)) * 0.6 + (0.6 if y < 7 else 0)
    s.shape(sack, burlap, (7.0, 10.0), (6.0, 6.0), texture=lambda x, y: -1 if (x + y) % 2 == 0 and y % 2 == 0 else 0)
    s.pixels_at(burlap[LIGHT], [(x, 5) for x in range(2, 14)])
    s.ellipse(grain, 7.5, 4.6, 5.0, 2.6, texture=lambda x, y: 1 if (x + 2 * y) % 3 == 0 else 0)
    s.pixels_at(grain[LIGHT], [(0, 15), (1, 14), (14, 15), (15, 14)])
    return s

def bean_pile(colour, beans, hilum=None, shine=None, spread=1.0):
    """A small heap of oval beans or berries, back ones first, each with a glint and an optional pale eye."""
    s = Sprite()
    tones = ramp(colour, spread=spread)
    for cx, cy, rx, ry, angle in beans:
        s.ellipse(tones, cx, cy, rx, ry, angle=angle)
        s.put(int(cx) - 1, int(cy) - 1, shine or tones[HIGHLIGHT])
        if hilum:
            s.pixels_at(hilum, [(int(cx) + 1, int(cy)), (int(cx) + 2, int(cy))])
    return s


# Fewer, bigger beans, so each reads as a bean rather than a speck.
PILE = [(5.0, 6.4, 3.0, 2.1, -20), (10.6, 6.8, 3.0, 2.1, 25), (3.8, 11.0, 3.0, 2.1, 30), (8.4, 10.4, 3.1, 2.2, -10),
        (12.0, 12.4, 2.8, 2.0, -35), (7.4, 13.8, 2.9, 2.0, 15)]


def azuki_bean_hand():
    return bean_pile("#8c1e24", PILE, hilum=ramp("#ece6d8")[BASE])


def black_bean_hand():
    return bean_pile("#1e1a24", PILE, hilum=ramp("#ece6d8")[BASE], shine=ramp("#6a7290")[LIGHT], spread=0.6)


def allspice_hand():
    # Dried round berries, matte and rough, each with the little crown left by its flower.
    berries = [(4.8, 5.6, 2.8), (10.6, 5.4, 2.7), (3.8, 11.0, 2.8), (8.6, 9.8, 3.0), (12.4, 11.6, 2.6), (7.6, 13.6, 2.4)]
    s = bean_pile("#6c3a24", [(cx, cy, r, r, 0) for cx, cy, r in berries])
    crown = ramp("#3a2016")[BASE]
    for cx, cy, r in berries:
        s.pixels_at(crown, [(int(cx), int(cy - r + 1)), (int(cx) + 1, int(cy - r + 1))])
    return s


def betel_leaf_hand():
    s = Sprite()
    leaf = ramp("#3e8c2a")
    angle = math.radians(35)
    cos, sin = math.cos(angle), math.sin(angle)

    def local(x, y):
        # The leaf's own frame: tilted, stem at the top left, tip toward the bottom right.
        dx, dy = x - 7.5, y - 8.0
        return dx * cos + dy * sin, -dx * sin + dy * cos

    def heart(x, y):
        u, v = local(x, y)
        # Two rounded lobes at the stem end, narrowing to a pointed tip.
        lobes = ((u + 2.6) / 3.6) ** 2 + ((v + 2.4) / 3.4) ** 2 <= 1 or ((u - 2.6) / 3.6) ** 2 + ((v + 2.4) / 3.4) ** 2 <= 1
        point = -2.4 <= v <= 7.0 and abs(u) <= 6.0 * (1 - (v + 2.4) / 9.4) ** 1.1
        return (lobes or point) and v >= -5.2
    s.shape(heart, leaf, (7.0, 7.0), (6.0, 6.0))
    vein = leaf[LIGHT]
    for t in range(-4, 7):
        x, y = 7.5 - t * sin, 8.0 + t * cos
        if s.pixels[int(y)][int(x)] not in (None, leaf[OUTLINE]):
            s.put(int(x), int(y), vein)
    s.pixels_at(leaf[HIGHLIGHT], [(6, 4), (7, 3)])
    s.pixels_at(ramp("#5a7a2a")[SHADOW], [(10, 2), (11, 1), (12, 0)])
    return s


def black_olive_hand():
    s = Sprite()
    olive = ramp("#2c1e30", spread=0.6)
    shine = ramp("#8a7aa0")[LIGHT]
    # Three glossy olives, back ones first, one with its stem.
    for cx, cy, angle in [(5.0, 5.8, -30), (11.0, 7.2, 25), (7.4, 11.6, -10)]:
        s.ellipse(olive, cx, cy, 3.6, 2.7, angle=angle)
        s.pixels_at(shine, [(int(cx) - 2, int(cy) - 1), (int(cx) - 1, int(cy) - 2), (int(cx), int(cy) - 2)])
    s.pixels_at(ramp("#6a5a2a")[SHADOW], [(8, 9), (9, 8)])
    return s

def cardamom_pod_hand():
    s = Sprite()
    pod = ramp("#9cba5c", spread=0.8)

    def spindle(start, end, width):
        (x0, y0), (x1, y1) = start, end
        length = math.dist(start, end)
        ux, uy = (x1 - x0) / length, (y1 - y0) / length

        def inside(x, y):
            # Plump in the middle and pointed at both ends.
            t = ((x - x0) * ux + (y - y0) * uy) / length
            across = abs(-(x - x0) * uy + (y - y0) * ux)
            return 0 <= t <= 1 and across <= width * math.sin(math.pi * t) ** 0.8

        def ridges(x, y):
            across = -(x + 0.5 - x0) * uy + (y + 0.5 - y0) * ux
            return -1 if abs(across) < 0.45 else 0
        s.shape(inside, pod, ((x0 + x1) / 2, (y0 + y1) / 2), (width + 1, width + 1), texture=ridges)
    # Three plump pods with a ridge down each.
    spindle((1.6, 7.2), (9.4, 1.6), 2.5)
    spindle((6.6, 10.0), (14.6, 4.8), 2.5)
    spindle((2.4, 14.6), (11.4, 10.0), 2.6)
    return s


def celery_hand():
    s = Sprite()
    stalk = ramp("#a8cc76", spread=0.75)
    # A bunch of ribbed stalks, slightly fanning, under leafy tops.
    for x0, x1 in ((4, 3), (7, 6), (10, 10), (12, 13)):
        for y in range(5, 16):
            x = round(x0 + (x1 - x0) * (15 - y) / 10)
            s.put(x, y, stalk[LIGHT])
            s.put(x + 1, y, stalk[BASE])
            s.put(x + 2, y, stalk[SHADOW] if y % 3 else stalk[BASE])
    # Small, lighter leafy tufts at the top of each stalk, with gaps between them.
    for i, (cx, cy) in enumerate(((3.4, 3.2), (7.0, 2.0), (10.8, 2.6), (13.8, 3.6))):
        tones = ramp("#62b048" if i % 2 else "#58a440", spread=0.7)
        s.ellipse(tones, cx, cy, 1.8, 1.6, texture=speckle(int(cx * 3), 0.35, up=1, down=0))
    s.pixels_at(ramp("#d8e8b0")[BASE], [(4, 15), (7, 15), (10, 15), (12, 15)])
    return s


def chickpea_hand():
    # Round, knobbly beige peas, each with the little beak where it grew.
    peas = [(4.8, 6.0, 2.7), (10.4, 6.2, 2.6), (3.8, 11.2, 2.7), (8.6, 10.2, 2.9), (12.6, 11.8, 2.5), (7.6, 14.0, 2.2)]
    s = bean_pile("#d8b47a", [(cx, cy, r, r * 0.95, 0) for cx, cy, r in peas], spread=0.8)
    beak = ramp("#a07a4a")[BASE]
    for cx, cy, r in peas:
        s.put(int(cx + r * 0.6), int(cy - r * 0.6), beak)
        s.put(int(cx) + 1, int(cy) + 1, ramp("#d8b47a")[SHADOW])
    return s


def cilantro_hand():
    s = Sprite()
    # Thin stems gathered at the bottom right; round, scalloped leaves fill the rest.
    stem = ramp("#7ab84a")
    for x0, y0 in ((9, 10), (11, 9), (12, 11), (8, 12)):
        s.line(stem[BASE], 14, 15, x0, y0)
    for i, (cx, cy) in enumerate([(3.6, 3.4), (8.2, 2.6), (12.4, 4.0), (2.8, 8.0), (7.4, 7.0), (11.6, 8.4), (4.8, 12.0), (8.8, 11.6)]):
        tones = ramp(["#5ab84a", "#4aa83e", "#66c254"][i % 3], spread=0.6)
        # Each leaf is a fan of three round, scalloped lobes.
        for dx, dy in ((-1.2, 0.4), (1.2, 0.4), (0.0, -0.9)):
            mask = s.ellipse(tones, cx + dx, cy + dy, 1.5, 1.4)
            for x, y in mask:
                if (x + y) % 2 == 0 and s.pixels[y][x] == tones[OUTLINE]:
                    s.put(x, y, tones[SHADOW])
        s.put(int(cx), int(cy), tones[HIGHLIGHT])
    return s

def cinnamon_bark_hand():
    s = Sprite()
    bark = ramp("#a05c30", spread=0.85)
    curl = ramp("#d09058")
    # Two rolled quills, parallel and apart, each showing the spiral of rolled bark at its near end.
    for start, end in (((2.2, 9.0), (13.0, 2.4)), ((3.0, 14.0), (13.8, 7.4))):
        s.capsule(bark, [start, end], (2.1, 2.1))
        s.line(bark[SHADOW], int(start[0]) + 2, int(start[1]) - 1, int(end[0]) - 1, int(end[1]) + 1)
        x, y = round(start[0]), round(start[1])
        s.pixels_at(curl[BASE], [(x - 1, y - 1), (x, y - 1), (x - 1, y)])
        s.pixels_at(bark[OUTLINE], [(x, y)])
        s.pixels_at(curl[LIGHT], [(x - 1, y + 1)])
    return s


def clove_bud_hand():
    s = Sprite()
    clove = ramp("#6a3a22", spread=0.85)
    head = ramp("#a06a40")
    # Three nail-shaped buds: a tapering stem with a round head held in four pointed sepals.
    for (hx, hy), (tx, ty) in (((4.0, 3.6), (7.6, 12.4)), ((11.4, 3.4), (9.0, 13.0)), ((12.6, 9.6), (3.6, 14.4))):
        s.capsule(clove, [(hx, hy), (tx, ty)], (1.3, 0.6))
        s.ellipse(head, hx, hy, 1.6, 1.6)
        x, y = int(hx), int(hy)
        s.pixels_at(clove[OUTLINE], [(x - 2, y + 1), (x + 2, y + 1), (x - 1, y + 2), (x + 1, y + 2)])
        s.put(x - 1, y - 1, head[HIGHLIGHT])
    return s


def coffee_cherry_hand():
    s = Sprite()
    s.line(ramp("#6b5a3a")[BASE], 0, 12, 15, 4)
    leaf = ramp("#3a8a34")
    s.ellipse(leaf, 11.6, 11.8, 3.8, 2.0, angle=25)
    s.line(leaf[LIGHT], 9, 11, 14, 13)
    # A few plump, glossy cherries along the twig: three ripe red, one still orange.
    for cx, cy, colour in ((3.8, 8.6, "#d42628"), (7.6, 7.4, "#e0302a"), (5.8, 12.0, "#c82028"), (11.0, 5.2, "#e8822e")):
        cherry = ramp(colour, spread=0.85)
        s.ellipse(cherry, cx, cy, 2.5, 2.3)
        s.pixels_at(cherry[HIGHLIGHT], [(int(cx) - 1, int(cy) - 1), (int(cx), int(cy) - 1)])
        s.put(int(cx) + 1, int(cy) + 1, cherry[SHADOW])
    return s


def elderflower_hand():
    s = Sprite()
    stem = ramp("#6a9a3a")
    # Green stems forking up into a broad, flat head of tiny cream flowers.
    s.line(stem[SHADOW], 8, 15, 8, 11)
    for x1, y1 in [(3, 8), (6, 7), (10, 7), (13, 8)]:
        s.line(stem[BASE], 8, 11, x1, y1)
    petal, centre, shade = ramp("#f2ecca")[BASE], ramp("#e8d878")[BASE], ramp("#c8c4a0")[BASE]
    rng = random.Random(21)

    def head(x, y):
        return ((x - 8.0) / 7.2) ** 2 + ((y - 5.4) / 4.4) ** 2 <= 1 and y <= 8.6
    for y in range(SIZE):
        for x in range(SIZE):
            if not head(x + 0.5, y + 0.5):
                continue
            edge = not all(head(x + 0.5 + dx, y + 0.5 + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if edge and rng.random() < 0.4:
                continue
            # Tiny flowers: cream petals, a yellow eye here and there, soft shade between them.
            roll = rng.random()
            s.put(x, y, centre if roll < 0.18 else shade if roll < 0.32 else petal)
    return s


def glutinous_rice_hand():
    s = Sprite()
    basket = ramp("#b89458", spread=0.8)
    grain = ramp("#f6f4ee", spread=0.45)

    def weave(x, y):
        return -1 if (x // 2 + y // 2) % 2 == 0 else 0
    # A woven bamboo basket, as sticky rice is served, heaped with chalky white short grains.
    s.shape(lambda x, y: 8.0 <= y <= 15.2 and abs(x - 7.5) <= 6.6 - (y - 8.0) * 0.32, basket, (7.0, 11.0), (6.6, 4.0), texture=weave)
    s.pixels_at(basket[LIGHT], [(x, 8) for x in range(1, 15)])
    s.ellipse(grain, 7.5, 7.0, 6.0, 3.6, texture=lambda x, y: -1 if (x + 2 * y) % 4 == 0 else 0)
    return s

def green_olive_hand():
    s = Sprite()
    leaf = ramp("#7a9a7a", spread=0.7)
    # A narrow, silvery olive leaf behind three glossy green olives.
    s.ellipse(leaf, 9.6, 4.6, 5.8, 1.5, angle=-25)
    s.line(leaf[LIGHT], 6, 6, 13, 3)
    olive = ramp("#8aa232", spread=0.8)
    for cx, cy, angle in [(4.6, 7.4, -30), (10.6, 9.2, 25), (6.8, 12.4, -10)]:
        s.ellipse(olive, cx, cy, 3.4, 2.6, angle=angle)
        s.pixels_at(olive[HIGHLIGHT], [(int(cx) - 2, int(cy) - 1), (int(cx) - 1, int(cy) - 2)])
    return s


def holy_basil_hand():
    s = Sprite()
    stem = ramp("#7a3a5a")
    # Purple-tinged stems gathered at the bottom right.
    for x0, y0 in ((9, 10), (11, 9), (8, 12)):
        s.line(stem[BASE], 13, 15, x0, y0)
    # Oval leaves with finely toothed green edges, layered to fill the bunch.
    for i, (cx, cy, angle) in enumerate([(4.0, 5.0, 30), (8.4, 4.2, -10), (12.2, 6.4, -40), (3.4, 9.6, 40), (7.6, 9.0, 0),
                                         (11.4, 11.0, -30), (5.4, 12.8, 20)]):
        tones = ramp(["#4a9a3a", "#5aa648", "#468e38"][i % 3], spread=0.7)
        mask = s.ellipse(tones, cx, cy, 2.7, 1.8, angle=angle)
        for x, y in mask:
            if (x + y) % 2 == 0 and s.pixels[y][x] == tones[OUTLINE]:
                s.put(x, y, tones[SHADOW])
        s.put(int(cx), int(cy), tones[SHADOW])
        s.put(int(cx) - 1, int(cy) - 1, tones[HIGHLIGHT])
    # A slender purple flower spike rising from the top.
    flower = ramp("#9a5aa0")
    s.pixels_at(flower[BASE], [(8, 0), (8, 1), (9, 1)])
    s.pixels_at(flower[LIGHT], [(7, 1), (9, 0)])
    return s


def hops_hand():
    s = Sprite()
    leaf = ramp("#4a8a34")
    s.ellipse(leaf, 4.0, 4.2, 3.2, 2.4, angle=30, texture=lambda x, y: -1 if (x + y) % 3 == 0 else 0)
    s.line(ramp("#6a8a3a")[BASE], 6, 4, 12, 1)
    cone = ramp("#a8d070", spread=0.8)

    def bracts(x, y):
        # Overlapping papery scales: a light lip on each, shade beneath.
        return 1 if y % 2 == 0 and (x + y // 2) % 2 == 0 else -1 if y % 2 == 1 else 0
    # Two papery cones of overlapping scales hanging from the bine.
    s.ellipse(cone, 10.6, 6.4, 2.8, 3.8, texture=bracts)
    s.ellipse(cone, 6.6, 11.0, 3.2, 4.2, texture=bracts)
    s.pixels_at(ramp("#6a8a3a")[BASE], [(11, 2), (7, 6)])
    return s


def laver_hand():
    s = Sprite()
    frond = ramp("#4a3048", spread=0.8)
    # Thin, ruffled fronds of seaweed, dark purple with a green sheen, their edges rippling.
    for cx, cy, angle in [(6.0, 6.0, -30), (10.0, 9.4, 20), (5.4, 11.6, 40)]:
        mask = s.ellipse(frond, cx, cy, 5.0, 2.8, angle=angle, texture=lambda x, y: 1 if (x * 2 + y) % 5 == 0 else 0)
        for x, y in mask:
            if (x + y) % 3 == 0 and s.pixels[y][x] == frond[OUTLINE]:
                s.put(x, y, None)  # A rippled, uneven edge.
    s.pixels_at(ramp("#4a6a4a")[LIGHT], [(5, 5), (9, 9), (6, 11), (7, 6)])
    return s


def lemongrass_hand():
    s = Sprite()
    blade = ramp("#7aaa48")
    base = ramp("#d8d49a", spread=0.6)
    # Three stalks: pale, swollen bulbs at the bottom and long green blades rising and leaning apart.
    for x0, x1 in ((4, 2), (7, 7), (10, 12)):
        for y in range(1, 16):
            x = round(x0 + (x1 - x0) * (15 - y) / 14)
            tones = base if y >= 10 else blade
            s.put(x, y, tones[LIGHT])
            s.put(x + 1, y, tones[BASE] if y >= 10 else tones[SHADOW])
            if y >= 11:
                s.put(x + 2, y, base[SHADOW])
    s.pixels_at(ramp("#c8a8a0")[BASE], [(4, 15), (7, 15), (10, 15)])
    return s

def long_bean_hand():
    s = Sprite()
    pod = ramp("#5aa03a", spread=0.85)
    # Three long pods lying apart on a diagonal, gently curving, with a bean bump every few pixels.
    for a, b, c in (((0.6, 9.0), (5.0, 3.0), (12.0, 0.8)), ((1.6, 14.4), (8.0, 7.4), (15.0, 2.0)), ((6.4, 15.4), (11.0, 11.4), (15.4, 7.6))):
        points = [(x, y) for x, y, t in curve(a, b, c, steps=6)]
        s.capsule(pod, points, (1.1, 0.9))
        for x, y, t in curve(a, b, c, steps=10)[1:-1:2]:
            s.put(int(x), int(y), pod[HIGHLIGHT])
    return s


def makrut_lime_leaf_hand():
    s = Sprite()
    leaf = ramp("#2f7a2e", spread=0.85)
    angle = -40
    cos, sin = math.cos(math.radians(angle)), math.sin(math.radians(angle))
    # The double leaf: a lower leaflet on a winged stalk and an upper leaflet, end to end with a pinched waist.
    for offset, length, width in ((-3.6, 3.4, 2.4), (3.2, 4.4, 2.9)):
        s.ellipse(leaf, 7.5 + offset * cos, 8.0 + offset * sin, length, width, angle=angle)
    s.line(leaf[LIGHT], 3, 13, 12, 3)
    s.pixels_at(leaf[HIGHLIGHT], [(10, 4), (5, 10)])
    s.pixels_at(ramp("#5a7a2a")[SHADOW], [(2, 14), (1, 15)])
    return s


def marjoram_hand():
    s = Sprite()
    stem = ramp("#7a8a4a")
    leaf = ramp("#94b080", spread=0.7)
    # Three sprigs gathered at the bottom right, each with small, soft, rounded grey-green leaves in pairs.
    for (x1, y1), pairs in (((2, 2), [(4, 5), (7, 8)]), ((8, 1), [(8, 3), (9, 7)]), ((14, 3), [(12, 5), (11, 9)])):
        s.line(stem[BASE], 13, 15, x1, y1)
        for x, y in pairs + [(x1, y1)]:
            s.ellipse(leaf, x - 0.8, y + 0.5, 1.6, 1.2)
            s.ellipse(leaf, x + 1.8, y + 0.5, 1.6, 1.2)
            s.put(x - 1, y, leaf[HIGHLIGHT])
    # Tiny white flower knots at the tips.
    s.pixels_at(ramp("#f0e8e0")[BASE], [(2, 1), (8, 0), (14, 2)])
    return s


def oil_palm_fruit_hand():
    s = Sprite()
    # A packed cluster of oval fruits, orange-red below and ripening to black at the tips, between spiky stalks.
    spike = ramp("#8a6a3a")[BASE]
    s.pixels_at(spike, [(1, 4), (14, 3), (2, 13), (14, 12), (8, 0)])
    fruits = [(5.0, 4.4), (9.4, 3.8), (12.4, 6.6), (3.4, 8.0), (7.4, 7.8), (11.0, 10.0), (4.8, 11.8), (8.6, 12.0)]
    red, black = ramp("#d8481e"), ramp("#2a1612", spread=0.6)
    for cx, cy in fruits:
        def colour(x, y, cy=cy):
            return black if y + 0.5 < cy - 0.6 else red
        s.ellipse(colour, cx, cy, 2.3, 2.0)
        s.put(int(cx) - 1, int(cy), red[HIGHLIGHT])
    return s


def oyster_hand():
    s = Sprite()
    rim = ramp("#7a7062", spread=0.8)
    pearl = ramp("#e8e4dc", spread=0.4)
    meat = ramp("#a8a08c", spread=0.6)
    # An open half shell: a rough, layered rim around a pearly cup, the oyster lying in it.
    s.ellipse(rim, 7.6, 8.2, 7.0, 5.6, angle=-20, texture=lambda x, y: -1 if (x + 2 * y) % 4 == 0 else 1 if (x * 3 + y) % 7 == 0 else 0)
    s.ellipse(pearl, 7.6, 8.0, 5.2, 3.8, angle=-20, outline=False)
    s.ellipse(meat, 7.8, 8.2, 3.6, 2.6, angle=-20)
    s.pixels_at(meat[LIGHT], [(6, 7), (7, 7)])
    s.pixels_at(pearl[HIGHLIGHT], [(4, 6), (11, 10)])
    return s

def oyster_mushroom_hand():
    s = Sprite()
    # Fan-shaped caps growing in overlapping shelves, each with fine gill lines toward its base.
    for cx, cy, r, colour in ((9.6, 5.0, 4.6, "#a8a094"), (5.4, 9.0, 4.8, "#b8b0a2"), (10.4, 12.0, 4.0, "#c4bcae")):
        cap = ramp(colour, spread=0.7)

        def fan(x, y, cx=cx, cy=cy, r=r):
            return math.dist((x, y), (cx, cy)) <= r and y <= cy + 0.8
        mask = s.shape(fan, cap, (cx - 1, cy - 1), (r, r))
        for x, y in mask:
            if s.pixels[y][x] != cap[OUTLINE] and (x - int(cx)) % 2 == 0 and y > cy - r * 0.5:
                s.put(x, y, cap[SHADOW])
    s.pixels_at(ramp("#d8d0c0")[BASE], [(7, 14), (8, 15)])
    return s


def pandan_leaf_hand():
    s = Sprite()
    # Broad, glossy strap-like blades fanning up from the base, each with a pale rib and a pointed tip.
    for i, (base, tip) in enumerate((((4.6, 15.0), (1.2, 1.2)), ((7.6, 15.0), (8.2, 0.4)), ((10.4, 15.0), (14.4, 1.6)))):
        leaf = ramp("#46a038" if i % 2 else "#3c9232", spread=0.7)
        s.capsule(leaf, [base, ((base[0] + tip[0]) / 2, (base[1] + tip[1]) / 2), tip], (2.0, 0.7))
        s.line(leaf[HIGHLIGHT], round(base[0]), round(base[1]) - 1, round((base[0] + tip[0] * 2) / 3), round((base[1] + tip[1] * 2) / 3))
    return s


def peanut_hand():
    s = Sprite()
    shell = ramp("#d2b07a", spread=0.8)

    def net(x, y):
        return -1 if (x + y) % 3 == 0 or (x - y) % 3 == 0 else 0
    # Pods pinched at the waist: two lobes close together, the waist a shade darker at its edges.
    for (cx, cy), angle in (((5.4, 4.6), -30), ((11.0, 7.0), 20), ((6.8, 11.6), -10)):
        cos, sin = math.cos(math.radians(angle)), math.sin(math.radians(angle))

        def pod(x, y, cx=cx, cy=cy, cos=cos, sin=sin):
            u, v = (x - cx) * cos + (y - cy) * sin, -(x - cx) * sin + (y - cy) * cos
            # Two overlapping rounded lobes along the pod's length.
            return ((u + 1.7) / 2.2) ** 2 + (v / 2.0) ** 2 <= 1 or ((u - 1.7) / 2.3) ** 2 + (v / 2.1) ** 2 <= 1
        s.shape(pod, shell, (cx, cy), (4.0, 2.4), texture=net)
    return s


def peppercorn_hand():
    s = Sprite()
    stem = ramp("#6a8a3a")
    berry = ramp("#5a9a32", spread=0.85)
    # A hanging spike of small green peppercorns, packed along a stem that droops from the top left.
    s.line(stem[SHADOW], 2, 0, 5, 3)
    for i, (cx, cy) in enumerate([(5.6, 4.2), (7.0, 5.6), (6.0, 7.0), (8.2, 7.4), (7.2, 8.8), (9.2, 9.4), (8.4, 10.8), (10.4, 11.4),
                                  (9.6, 12.8), (11.2, 13.6)]):
        s.ellipse(berry, cx, cy, 1.4, 1.4, light=(-0.7, -0.7))
        s.put(int(cx) - 1, int(cy) - 1, berry[HIGHLIGHT])
    return s

def purple_corn_hand():
    return corn_cob("#4c1c4e", speckle_colour="#8a3a7a")


def rapeseed_hand():
    s = Sprite()
    stem = ramp("#7a9a4a")
    green, gold = ramp("#9aaa48", spread=0.8), ramp("#d09040", spread=0.8)
    # A stalk carrying slim seed pods angled up and out on both sides, ripening from green below to golden-orange above.
    s.line(stem[BASE], 7, 15, 8, 0)
    for i, (y, side, length) in enumerate([(13, -1, 4), (11, 1, 5), (9, -1, 5), (7, 1, 5), (5, -1, 4), (3, 1, 4), (1, -1, 3)]):
        x0 = 8 if y < 8 else 7
        tones = gold if y < 8 else green
        for k in range(1, length + 1):
            x, yy = x0 + side * k, y - k // 2
            s.put(x, yy, tones[LIGHT if k % 2 else BASE])
            s.put(x, yy + 1, tones[SHADOW])
    return s


def rosehip_hand():
    s = Sprite()
    s.line(ramp("#6b5a3a")[BASE], 2, 2, 9, 6)
    # Glossy oval hips, each crowned at its tip by dark, dried sepals.
    for cx, cy, angle in ((4.6, 7.6, 20), (10.4, 9.0, -20), (6.8, 12.4, 10)):
        hip = ramp("#e0441c", spread=0.85)
        s.ellipse(hip, cx, cy, 2.2, 3.0, angle=angle)
        s.pixels_at(hip[HIGHLIGHT], [(int(cx) - 1, int(cy) - 1), (int(cx) - 1, int(cy))])
        tip = (int(cx - math.sin(math.radians(angle)) * -3.2), int(cy + 3.2))
        s.pixels_at(ramp("#3a2016")[BASE], [tip, (tip[0] - 1, min(15, tip[1] + 1)), (tip[0] + 1, min(15, tip[1] + 1))])
    return s


def roselle_hand():
    s = Sprite()
    red = ramp("#b8142e", spread=0.85)
    # Fleshy red calyces: a plump bulb with pointed lobes closing to a star at the tip.
    for cx, cy in ((5.4, 6.8), (10.6, 10.4)):
        s.ellipse(red, cx, cy + 0.8, 3.2, 3.4)
        x, y = int(cx), int(cy)
        s.pixels_at(red[BASE], [(x, y - 3), (x - 2, y - 2), (x + 2, y - 2)])
        s.pixels_at(red[OUTLINE], [(x, y - 4), (x - 3, y - 3), (x + 3, y - 3)])
        s.pixels_at(red[SHADOW], [(x - 1, y), (x + 1, y), (x, y + 1)])
        s.pixels_at(red[HIGHLIGHT], [(x - 2, y + 1), (x - 1, y + 2)])
    s.pixels_at(ramp("#5a7a2a")[SHADOW], [(5, 14), (10, 15), (11, 14)])
    return s


def rye_hand():
    s = Sprite()
    grain = ramp("#c8a462", spread=0.85)
    awn = ramp("#d8c08a")
    stalk = ramp("#b8a060")
    # Two plump golden ears on curving stalks, their kernels set in a herringbone, with short whiskers.
    for x0, top, lean in ((5, 1, -1), (10, 3, 1)):
        for y in range(top + 7, 16):
            s.put(x0 + (lean if y > top + 10 else 0), y, stalk[BASE])
        for y in range(top, top + 7):
            # Each row: a kernel each side of the centre, lit on the left.
            s.put(x0 - 1, y, grain[LIGHT])
            s.put(x0, y, grain[BASE] if y % 2 else grain[SHADOW])
            s.put(x0 + 1, y, grain[SHADOW] if y % 2 else grain[BASE])
            if y % 2 == 0:
                s.pixels_at(awn[LIGHT], [(x0 - 2, y - 1), (x0 + 2, y - 1)])
        s.pixels_at(awn[LIGHT], [(x0, top - 1)])
        s.pixels_at(grain[OUTLINE], [(x0, top + 7)])
    return s

def sea_lettuce_hand():
    s = Sprite()
    # A bushy clump of broad, ruffled, translucent fronds fanning up and out from a darker heart.
    fronds = [(-170, 6.6, 2.8), (-140, 7.2, 3.0), (-112, 7.6, 3.0), (-90, 7.6, 3.0), (-68, 7.6, 3.0), (-40, 7.2, 3.0), (-10, 6.6, 2.8)]
    for i, (angle, length, width) in enumerate(fronds):
        frond = ramp(["#86c43a", "#9ad048", "#7ab834", "#a8d856"][i % 4], spread=0.65)
        cx = 8.0 + math.cos(math.radians(angle)) * length * 0.5
        cy = 13.4 + math.sin(math.radians(angle)) * length * 0.5
        mask = s.ellipse(frond, cx, cy, length * 0.6, width, angle=angle, texture=lambda x, y: 1 if (x * 2 + y) % 5 == 0 else 0)
        for x, y in mask:
            if (x * 3 + y) % 4 == 0 and s.pixels[y][x] == frond[OUTLINE] and y < 12:
                s.put(x, y, None)  # A rippled, uneven edge.
    s.ellipse(ramp("#4f8e2e", spread=0.7), 8.0, 13.8, 2.6, 1.4)
    return s


def tea_shoot(colour):
    """Two pointed leaves and a bud, as tea is plucked."""
    s = Sprite()
    leaf = ramp(colour, spread=0.8)
    s.line(ramp("#7a9a4a")[BASE], 8, 15, 8, 5)
    for cx, cy, angle in ((4.4, 10.8, 35), (11.6, 8.6, -40)):
        s.ellipse(leaf, cx, cy, 4.4, 2.3, angle=angle)
        s.line(leaf[LIGHT], round(cx - 2.6 * math.cos(math.radians(angle))), round(cy - 2.6 * math.sin(math.radians(angle))),
               round(cx + 2.6 * math.cos(math.radians(angle))), round(cy + 2.6 * math.sin(math.radians(angle))))
    bud = ramp("#a8d070", spread=0.7)
    s.ellipse(bud, 8.0, 3.6, 1.7, 2.8)
    s.pixels_at(bud[HIGHLIGHT], [(7, 3), (7, 2)])
    return s


def shaded_tea_leaf_hand():
    # Shade-grown tea: a deeper, darker green than tea grown in the open.
    return tea_shoot("#2a6e30")


def soybean_hand():
    s = Sprite()
    stalk = ramp("#6a8a3a")
    s.line(stalk[BASE], 8, 0, 8, 4)
    pod = ramp("#86b050", spread=0.75)
    fuzz = ramp("#d8e8b0")[BASE]
    # Fuzzy pods hanging from one stalk, each with two or three beans bulging inside.
    for (x0, y0), (x1, y1) in (((7.4, 4.6), (3.0, 12.6)), ((8.6, 4.8), (9.4, 14.4)), ((9.6, 4.4), (13.4, 11.4))):
        mask = s.capsule(pod, [(x0, y0), (x1, y1)], (1.6, 1.6))
        for t in (0.35, 0.65, 0.9):
            s.put(int(x0 + (x1 - x0) * t), int(y0 + (y1 - y0) * t), pod[HIGHLIGHT])
        for x, y in mask:
            if s.pixels[y][x] == pod[OUTLINE] and (x * 2 + y) % 5 == 0:
                s.put(x, y, fuzz)  # A few fine hairs along the edge.
    return s


def spring_onion_hand():
    s = Sprite()
    white, pale, green = ramp("#f0f0e4", spread=0.5), ramp("#c8e0a0", spread=0.6), ramp("#4a9a34", spread=0.8)
    # A bunch lying on a diagonal: white bulbs at the bottom right, long green hollow leaves to the top left.
    for k, (x0, y0) in enumerate(((13.0, 13.4), (11.4, 14.6), (14.4, 11.4))):
        for i in range(12):
            x, y = x0 - i * 1.0, y0 - i * 1.0 - (k - 1) * 0.0
            tones = white if i < 2 else pale if i < 4 else green
            s.put(int(x), int(y), tones[LIGHT])
            s.put(int(x) + 1, int(y), tones[BASE])
            s.put(int(x), int(y) + 1, tones[SHADOW])
        s.ellipse(white, x0 + 0.6, y0 + 0.4, 1.4, 1.3)
    s.pixels_at(ramp("#c8b48a")[BASE], [(15, 15), (13, 15), (15, 13)])
    return s


def star_anise_pod_hand():
    s = Sprite()
    pod = ramp("#9c5628", spread=0.75)
    seed = ramp("#d0923e")
    # Eight boat-shaped pods radiating from the centre in a star, some split open to show a glossy seed.
    for k in range(8):
        angle = k * 45 + 22.5
        cx = 8.0 + math.cos(math.radians(angle)) * 3.6
        cy = 8.0 + math.sin(math.radians(angle)) * 3.6
        s.ellipse(pod, cx, cy, 3.4, 1.4, angle=angle)
        if k % 2 == 0:
            s.put(int(cx), int(cy), seed[LIGHT])
    s.pixels_at(pod[OUTLINE], [(7, 7), (8, 8)])
    s.pixels_at(pod[SHADOW], [(8, 7), (7, 8)])
    return s

def tamarind_pod_hand():
    s = Sprite()
    shell = ramp("#a8643a", spread=0.8)
    # Two plump, gently curved pods that swell over each seed and narrow between them, each with a dried stalk.
    for a, b, c in (((1.4, 5.6), (8.0, 1.6), (14.6, 5.0)), ((1.2, 12.2), (8.0, 8.0), (14.8, 12.8))):
        points = curve(a, b, c, steps=30)

        def inside(x, y, points=points):
            return any(math.dist((x, y), (px, py)) <= 1.8 + 0.45 * math.cos(t * math.pi * 8) for px, py, t in points)
        s.shape(inside, shell, ((a[0] + c[0]) / 2, b[1]), (7.0, 3.0), texture=speckle(int(a[1] * 7), 0.15, up=0))
        s.put(int(a[0]), int(a[1]), ramp("#6b4226")[SHADOW])
    return s


def tea_leaf_hand():
    # Tea grown in the open: a glossy, lighter green than shaded tea.
    return tea_shoot("#4aa83c")


def thai_basil_hand():
    s = Sprite()
    stem = ramp("#7a2a5a")
    # Purple stems gathered at the bottom right.
    for x0, y0 in ((8, 9), (11, 8), (9, 12)):
        s.line(stem[BASE], 13, 15, x0, y0)
    # Smooth, pointed leaves in pairs, mostly green with a few young ones flushed purple.
    for i, (cx, cy, angle) in enumerate([(3.6, 4.8, 30), (8.0, 4.0, -15), (12.4, 6.0, -45), (3.0, 9.6, 45), (7.6, 8.8, 0),
                                         (11.6, 10.8, -35), (5.2, 12.6, 25)]):
        tones = ramp("#7a4a6a", spread=0.6) if i == 2 else ramp(["#5a9a42", "#6aa84e", "#4e8e3a"][i % 3], spread=0.7)
        s.ellipse(tones, cx, cy, 3.2, 1.8, angle=angle)
        s.put(int(cx), int(cy), tones[SHADOW])
        s.put(int(cx) - 1, int(cy) - 1, tones[HIGHLIGHT])
    # A purple flower bud rising from the middle.
    flower = ramp("#6a2a6a")
    s.pixels_at(flower[BASE], [(8, 0), (8, 1), (7, 1), (9, 2)])
    s.pixels_at(flower[LIGHT], [(9, 1)])
    return s


def vanilla_pod_hand():
    s = Sprite()
    pod = ramp("#4a2c22", spread=0.7)
    # Long, thin, wrinkled pods, slightly curved, one with its tip curled over.
    for a, b, c in (((1.4, 13.4), (6.0, 4.0), (13.6, 1.4)), ((3.6, 14.8), (9.0, 7.0), (14.8, 3.4))):
        points = [(x, y) for x, y, t in curve(a, b, c, steps=8)]
        s.capsule(pod, points, (1.6, 1.0), texture=lambda x, y: -1 if (x + y) % 3 == 0 else 0)
        for x, y, t in curve(a, b, c, steps=12)[1:-2:2]:
            s.put(int(x) - 1, int(y), pod[HIGHLIGHT])
    s.pixels_at(pod[SHADOW], [(14, 0), (15, 1)])
    return s


def wakame_hand():
    s = Sprite()
    frond = ramp("#4a5e2c", spread=0.8)
    # A fresh frond: a firm midrib with broad, wavy, lobed blades spreading to either side.
    for cx, cy, angle in [(4.2, 4.2, -150), (10.8, 4.6, -30), (3.6, 8.8, 170), (12.2, 9.0, 10), (4.6, 13.0, 150), (11.4, 13.2, 30)]:
        mask = s.ellipse(frond, cx, cy, 4.0, 2.3, angle=angle, texture=lambda x, y: 1 if (x * 2 + y) % 5 == 0 else 0)
        for x, y in mask:
            if (x * 3 + y) % 7 == 0 and s.pixels[y][x] == frond[OUTLINE]:
                s.put(x, y, None)  # A wavy edge.
    rib = ramp("#7a8a48")
    s.line(rib[BASE], 8, 15, 7, 1)
    s.line(rib[LIGHT], 7, 14, 7, 2)
    return s


HAND = {"plum": plum_hand, "apricot": apricot_hand, "orange": orange_hand, "lime": lime_hand, "mango": mango_hand}
HAND.update({"tamarind_pod": tamarind_pod_hand, "tea_leaf": tea_leaf_hand, "thai_basil": thai_basil_hand,
             "vanilla_pod": vanilla_pod_hand, "wakame": wakame_hand})
HAND.update({"sea_lettuce": sea_lettuce_hand, "shaded_tea_leaf": shaded_tea_leaf_hand, "soybean": soybean_hand,
             "spring_onion": spring_onion_hand, "star_anise_pod": star_anise_pod_hand})
HAND.update({"purple_corn": purple_corn_hand, "rapeseed": rapeseed_hand, "rosehip": rosehip_hand, "roselle": roselle_hand,
             "rye": rye_hand})
HAND.update({"oyster_mushroom": oyster_mushroom_hand, "pandan_leaf": pandan_leaf_hand,
             "peanut": peanut_hand, "peppercorn": peppercorn_hand})
HAND.update({"long_bean": long_bean_hand, "makrut_lime_leaf": makrut_lime_leaf_hand, "marjoram": marjoram_hand,
             "oil_palm_fruit": oil_palm_fruit_hand, "oyster": oyster_hand})
HAND.update({"green_olive": green_olive_hand, "holy_basil": holy_basil_hand, "hops": hops_hand, "laver": laver_hand,
             "lemongrass": lemongrass_hand})
HAND.update({"cinnamon_bark": cinnamon_bark_hand, "clove_bud": clove_bud_hand, "coffee_cherry": coffee_cherry_hand,
             "elderflower": elderflower_hand, "glutinous_rice": glutinous_rice_hand})
HAND.update({"blue_corn": blue_corn_hand, "cardamom_pod": cardamom_pod_hand, "celery": celery_hand, "chickpea": chickpea_hand,
             "cilantro": cilantro_hand})
HAND.update({"allspice": allspice_hand, "azuki_bean": azuki_bean_hand, "betel_leaf": betel_leaf_hand,
             "black_bean": black_bean_hand, "black_olive": black_olive_hand})
HAND.update({"dill": dill_hand, "bay_laurel_leaf": bay_hand, "barley": barley_hand, "corn": corn_hand, "rice": rice_hand})
HAND.update({"galangal": galangal_hand, "thyme": thyme_hand, "mint": mint_hand, "basil": basil_hand, "parsley": parsley_hand})
HAND.update({"rhubarb": rhubarb_hand, "sweet_potato": sweet_potato_hand, "cassava": cassava_hand, "ginger": ginger_hand,
             "turmeric": turmeric_hand})
HAND.update({"radish": radish_hand, "shallot": shallot_hand,
             "asparagus": asparagus_hand})
HAND.update({"lettuce": lettuce_hand, "courgette": courgette_hand, "cucumber": cucumber_hand, "tomatillo": tomatillo_hand,
             "thai_aubergine": thai_aubergine_hand})
HAND.update({"poblano_chili": poblano_hand, "birds_eye_chili": birds_eye_hand, "aji_amarillo": aji_amarillo_hand,
             "cabbage": cabbage_hand, "red_cabbage": red_cabbage_hand})
HAND.update({"elderberry": elderberry_hand, "bell_pepper": bell_pepper_hand, "paprika_pepper": paprika_hand,
             "green_chili": green_chili_hand, "red_chili": red_chili_hand})
HAND.update({"white_grape": white_grape_hand, "red_grape": red_grape_hand, "strawberry": strawberry_hand, "raspberry": raspberry_hand,
             "redcurrant": redcurrant_hand})
HAND.update({"cherry": cherry_hand, "almond": almond_hand, "green_papaya": green_papaya_hand, "ume_plum": ume_plum_hand, "grape": grape_hand})
SPRITES.update(HAND)


def write():
    os.makedirs(f"{ROOT}/textures/item", exist_ok=True)
    for name, draw in SPRITES.items():
        draw().image().save(f"{ROOT}/textures/item/{name}.png")
        model = {"parent": "minecraft:item/generated", "textures": {"layer0": f"portable_pocket_cabin:item/{name}"}}
        with open(f"{ROOT}/models/item/{name}.json", "w") as f:
            json.dump(model, f, indent=2)
            f.write("\n")
        # The texture carries its own colours, so the item no longer needs a tint.
        with open(f"{ROOT}/items/{name}.json", "w") as f:
            json.dump({"model": {"type": "minecraft:model", "model": f"portable_pocket_cabin:item/{name}"}}, f, indent=2)
            f.write("\n")


def grid(path, names, columns=10):
    """A review grid: each named sprite large on an inventory-slot grey, with its name under it."""
    from PIL import ImageDraw
    scale, pad, label = 6, 10, 12
    cell = SIZE * scale
    rows = (len(names) + columns - 1) // columns
    out = Image.new("RGBA", (pad + columns * (cell + pad), pad + rows * (cell + pad + label)), (198, 198, 198, 255))
    draw = ImageDraw.Draw(out)
    for i, name in enumerate(names):
        x = pad + (i % columns) * (cell + pad)
        y = pad + (i // columns) * (cell + pad + label)
        draw.rectangle([x - 2, y - 2, x + cell + 1, y + cell + 1], fill=(139, 139, 139, 255))
        out.alpha_composite(SPRITES[name]().image().resize((cell, cell), Image.NEAREST), (x, y))
        draw.text((x, y + cell + 2), name[:16], fill=(40, 40, 40, 255))
    out.save(path)


def sheet(path, before=None):
    """A review sheet: each sprite large on an inventory-slot grey, with its name, and the old look above when given."""
    from PIL import ImageDraw
    scale, pad = 8, 12
    rows = [("now", {n: d().image() for n, d in SPRITES.items()})]
    if before:
        rows.insert(0, ("before", before))
    width = len(SPRITES) * (SIZE * scale + pad) + pad
    height = len(rows) * (SIZE * scale + pad + 14) + pad
    out = Image.new("RGBA", (width, height), (198, 198, 198, 255))
    draw = ImageDraw.Draw(out)
    for r, (label, images) in enumerate(rows):
        y = pad + r * (SIZE * scale + pad + 14)
        for i, name in enumerate(SPRITES):
            x = pad + i * (SIZE * scale + pad)
            draw.rectangle([x - 2, y - 2, x + SIZE * scale + 1, y + SIZE * scale + 1], fill=(139, 139, 139, 255))
            img = images.get(name)
            if img:
                big = img.resize((SIZE * scale, SIZE * scale), Image.NEAREST)
                out.alpha_composite(big, (x, y))
            draw.text((x, y + SIZE * scale + 3), f"{name} ({label})", fill=(40, 40, 40, 255))
    out.save(path)


if __name__ == "__main__":
    write()
    if "--sheet" in sys.argv:
        sheet(sys.argv[sys.argv.index("--sheet") + 1])
    if "--grid" in sys.argv:
        grid(sys.argv[sys.argv.index("--grid") + 1], sorted(SPRITES))
