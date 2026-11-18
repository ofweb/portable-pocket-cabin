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



def plain_ramp(base):
    """Five tones made by darkening and lightening a colour without shifting its hue. For pale seeds and shells, which
    are mostly outline, ramp's warm outline turns them orange."""
    r, g, b = hex_rgb(base)
    return [tuple(min(255, round(c * k + (255 - c) * max(0, k - 1))) for c in (r, g, b)) for k in (0.52, 0.8, 1.0, 1.1, 1.22)]


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
    # Fresh clove buds, picked before they open: pink-red heads on greenish stems.
    return clove_heads("#8a7a3a", "#d0604a")


def cloves_hand():
    # Cloves: the buds dried to a dark, woody brown.
    return clove_heads("#5a3020", "#8a5a38")


def clove_heads(stem_colour, head_colour):
    s = Sprite()
    clove = ramp(stem_colour, spread=0.85)
    head = ramp(head_colour)
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
    return vanilla_pods("#4a2c22")


def vanilla_pods(colour, shine=True):
    s = Sprite()
    pod = ramp(colour, spread=0.7)
    # Long, thin, wrinkled pods, slightly curved, one with its tip curled over.
    for a, b, c in (((1.4, 13.4), (6.0, 4.0), (13.6, 1.4)), ((3.6, 14.8), (9.0, 7.0), (14.8, 3.4))):
        points = [(x, y) for x, y, t in curve(a, b, c, steps=8)]
        s.capsule(pod, points, (1.6, 1.0), texture=(lambda x, y: -1 if (x + y) % 3 == 0 else 0) if shine else None)
        for x, y, t in curve(a, b, c, steps=12)[1:-2:2] if shine else []:
            s.put(int(x) - 1, int(y), pod[HIGHLIGHT])
    s.pixels_at(pod[SHADOW], [(14, 0), (15, 1)])
    return s


def wakame_hand():
    s = Sprite()
    stem = ramp("#6a8a3a")
    # A tall, swaying stem with long, wavy ribbon blades rising from it in turns, translucent yellow-green.
    stem_points = [(int(8 + 1.4 * math.sin(y * 0.45)), y) for y in range(15, 0, -1)]
    blades = [(13, -1, 4.6), (11, 1, 5.0), (9, -1, 5.2), (7, 1, 5.0), (5, -1, 4.4), (3, 1, 3.8)]
    for k, (y0, side, length) in enumerate(blades):
        blade = ramp(["#7cb03c", "#8cbc48", "#6ea434"][k % 3], spread=0.65)
        x0 = stem_points[15 - y0][0]
        # Each ribbon rises and curls away from the stem, with a wavy edge.
        points = [(x0 + side * t * 0.95, y0 - t * 0.85 - 0.6 * math.sin(t * 1.6)) for t in [i * length / 6 for i in range(7)]]
        s.capsule(blade, points, (1.6, 0.9))
        s.put(int(points[3][0]), int(points[3][1]), blade[HIGHLIGHT])
    for x, y in stem_points:
        s.put(x, y, stem[BASE])
    s.pixels_at(ramp("#9ac85a")[LIGHT], [(8, 0), (9, 1)])
    return s

def water_chestnut_hand():
    s = Sprite()
    corm = ramp("#5e1a24", spread=0.75)
    sprout = ramp("#c8904a")
    # A whole corm behind: round and a little flattened, with ring lines and a tan sprout tuft on top.
    s.ellipse(corm, 10.0, 6.8, 5.0, 4.0)
    s.pixels_at(corm[LIGHT], [(7, 5), (8, 4), (12, 4), (13, 6)])
    s.line(corm[SHADOW], 6, 8, 14, 8)
    s.pixels_at(sprout[BASE], [(10, 2), (10, 1), (11, 1)])
    s.pixels_at(sprout[LIGHT], [(9, 2)])
    # A cut half in front, showing its white flesh within a dark rim.
    s.ellipse(corm, 5.6, 11.6, 4.8, 3.6)
    s.ellipse(ramp("#f2ece0", spread=0.4), 5.6, 11.4, 3.6, 2.6, outline=False)
    s.pixels_at(sprout[BASE], [(5, 7), (6, 7)])
    return s


def white_bean_hand():
    # Large, creamy white kidney beans in a heap.
    return bean_pile("#ece4d0", PILE, spread=0.5)


def yerba_mate_leaf_hand():
    s = Sprite()
    s.line(ramp("#5a3a2a")[BASE], 3, 15, 12, 1)
    # Glossy oval leaves set alternately up the stem, with small clusters of red berries.
    for i, (cx, cy, angle) in enumerate([(2.6, 10.0, -150), (8.6, 11.6, 30), (4.4, 5.4, -130), (10.8, 6.6, 20), (8.4, 1.6, -60), (13.6, 2.8, 30)]):
        leaf = ramp("#3a8a34" if i % 2 else "#4a9a3e", spread=0.75)
        s.ellipse(leaf, cx, cy, 3.2, 1.8, angle=angle)
        s.put(int(cx) - 1, int(cy), leaf[HIGHLIGHT])
    berry = ramp("#d42a24")
    for x, y in [(6, 9), (7, 8), (6, 8), (10, 4), (9, 4)]:
        s.put(x, y, berry[BASE])
    s.pixels_at(berry[LIGHT], [(7, 8), (10, 4)])
    return s


def seed_heap(colour, fleck=None, ridge=False, seed=51):
    """A small heap of tiny seeds: individual seeds as short dashes, lit on top, with a few loose ones around."""
    s = Sprite()
    tones = ramp(colour, spread=0.7)
    s.shape(lambda x, y: y >= 6.0 and ((x - 7.5) / 7.0) ** 2 + ((y - 15.6) / 9.6) ** 2 <= 1, tones, (7.0, 11.0), (7.0, 5.0))
    rng = random.Random(seed)
    for y in range(7, 16):
        for x in range(1, 15):
            if s.pixels[y][x] in (None, tones[OUTLINE]):
                continue
            roll = rng.random()
            if roll < 0.22:
                s.put(x, y, tones[HIGHLIGHT])
            elif roll < 0.4:
                s.put(x, y, tones[SHADOW])
            elif fleck and roll < 0.48:
                s.put(x, y, ramp(fleck)[BASE])
            if ridge and (x + y) % 3 == 0:
                s.put(x, y, tones[SHADOW])
    s.pixels_at(tones[LIGHT], [(1, 15), (14, 15), (13, 14)])
    return s


SEED_SPOTS = [(4.0, 4.2, -20), (9.6, 3.4, 30), (13.0, 7.0, -50), (3.2, 9.4, 40), (8.0, 8.4, -10), (12.0, 12.2, 20),
              (5.0, 13.4, -35), (9.0, 13.4, 50)]


def seed_scatter(colour, length=2.2, width=1.4, ridge=False, stalk=None, spots=SEED_SPOTS, spread=0.8):
    """A scatter of individual seeds, each a small outlined oval big enough to show its shape."""
    s = Sprite()
    tones = ramp(colour, spread=spread)
    for cx, cy, angle in spots:
        s.ellipse(tones, cx, cy, length, width, angle=angle)
        s.put(int(cx) - 1, int(cy) - 1 if width > 1.2 else int(cy), tones[HIGHLIGHT])
        if ridge:
            s.put(int(cx), int(cy), tones[SHADOW])
        if stalk:
            tip = (round(cx + math.cos(math.radians(angle)) * (length + 0.6) - 0.5), round(cy + math.sin(math.radians(angle)) * (length + 0.6) - 0.5))
            s.put(*tip, ramp(stalk)[BASE])
    return s


def aji_amarillo_seeds_hand():
    # Small, flat, rounded chili seeds, pale cream-yellow, with an orange-yellow pod.
    return pepper_seeds("#e8d68e")


def anise_seed_hand():
    s = Sprite()
    seed = ramp("#94845e", spread=0.5)
    stripe, stalk = ramp("#cbbd94")[BASE], ramp("#d8cca8")[BASE]
    # Aniseed: plump grey-brown teardrops with pale stripes along their length, many trailing a thin pale stalk.
    for cx, cy, angle in SEED_SPOTS:
        cos, sin = math.cos(math.radians(angle)), math.sin(math.radians(angle))
        s.ellipse(seed, cx, cy, 2.5, 1.6, angle=angle)
        for t in (-1, 0, 1):
            s.put(round(cx + cos * t - 0.5), round(cy + sin * t - 0.5), stripe)
        for k in (3, 4):
            s.put(round(cx - cos * k - 0.5), round(cy - sin * k - 0.5), stalk)
    return s

# Many small seeds, for the tiniest kinds such as basil.
SMALL_SEED_SPOTS = [(3.0, 3.6, -20), (7.0, 2.6, 30), (11.0, 3.4, -40), (13.4, 7.0, 10), (2.4, 7.6, 50), (6.0, 6.8, -10), (9.6, 7.4, 40),
                    (4.0, 11.2, 20), (8.0, 11.0, -30), (12.0, 11.6, 60), (2.6, 14.2, -40), (6.4, 14.4, 10), (10.0, 14.6, -20), (14.0, 14.0, 30)]
# The same, kept clear of the top right corner for seeds that carry a plant picture.
SMALL_SEED_SPOTS_LOW = [spot for spot in SMALL_SEED_SPOTS if spot[0] < 8 or spot[1] > 9]


def annatto_seed_hand():
    s = Sprite()
    seed = ramp("#9c2c24", spread=0.7)
    # Small, angular, brick-red seeds: little three-sided pyramids, one lit face each.
    for cx, cy, _ in SMALL_SEED_SPOTS:
        x, y = int(cx), int(cy)
        s.pixels_at(seed[BASE], [(x, y - 1), (x - 1, y), (x, y), (x + 1, y), (x - 1, y + 1), (x, y + 1), (x + 1, y + 1)])
        s.pixels_at(seed[LIGHT], [(x, y - 1), (x - 1, y)])
        s.pixels_at(seed[SHADOW], [(x + 1, y + 1)])
    return s


def asparagus_crown_hand():
    s = Sprite()
    root = ramp("#c8a868", spread=0.7)
    # Long, thin tan roots hanging down from the crown, spread apart with gaps between them, gently wavy.
    for end_x in (1, 4, 7, 10, 13, 15):
        start = (7 if end_x < 8 else 9, 5)
        for i, (x, y) in enumerate(s.line(root[BASE], start[0], start[1], end_x, 15)):
            if i % 4 == 2:
                s.put(x, y, root[LIGHT])
    # A small, knobby crown at the top, with pale buds pointing up.
    crown = ramp("#9a7a44", spread=0.8)
    s.ellipse(crown, 8.0, 3.8, 2.6, 1.8)
    s.pixels_at(ramp("#c8b860")[LIGHT], [(7, 1), (8, 0), (9, 1)])
    return s


def aubergine_seeds_hand():
    s = seed_scatter("#d2a46a", length=1.9, width=1.7, spots=SEED_SPOTS_LOW)
    # Flat, round seeds with a lighter rim.
    for cx, cy, _ in SEED_SPOTS_LOW:
        s.put(int(cx) + 1, int(cy) + 1, ramp("#d2a46a")[SHADOW])
    return s


def basil_seeds_hand():
    # Many tiny black teardrop seeds with a dull glint.
    s = Sprite()
    seed = ramp("#26222a", spread=0.5)
    for cx, cy, angle in SMALL_SEED_SPOTS_LOW:
        s.ellipse(seed, cx, cy, 1.5, 1.0, angle=angle)
        s.put(int(cx) - 1, int(cy) - 1, ramp("#6a6870")[BASE])
    return s


def bell_pepper_seeds_hand():
    # Flat, pale seeds, with a red bell pepper.
    return pepper_seeds("#ecda8e")

SEED_SPOTS_LOW = [(3.2, 6.4, -20), (3.4, 11.0, 40), (7.6, 9.4, -10), (11.6, 11.4, 20), (5.6, 14.0, -35), (9.8, 14.4, 50), (13.2, 14.6, 10)]


def pepper_seeds(seed_colour):
    """Pale chili or pepper seeds, kept to the lower left so the plant picture fits in the corner."""
    return seed_scatter(seed_colour, length=1.9, width=1.6, spots=SEED_SPOTS_LOW, spread=0.45)


def birds_eye_chili_seeds_hand():
    return pepper_seeds("#e2c47a")


def cutting(stem_colour, start, end, leaves=(), buds=(), bud_colour=None):
    """A cutting: a length of stem, cut at an angle at the bottom, with leaves or buds along it."""
    s = Sprite()
    stem = ramp(stem_colour, spread=0.8)
    s.capsule(stem, [start, end], (1.0, 0.8))
    s.put(int(start[0]), int(start[1]), stem[LIGHT])  # The fresh, angled cut.
    for cx, cy, angle, colour in leaves:
        leaf = ramp(colour, spread=0.75)
        s.ellipse(leaf, cx, cy, 3.0, 2.2, angle=angle)
        s.put(int(cx) - 1, int(cy) - 1, leaf[HIGHLIGHT])
    for x, y in buds:
        bud = ramp(bud_colour)
        s.pixels_at(bud[BASE], [(x, y)])
        s.pixels_at(bud[LIGHT], [(x, y - 1)])
    return s


def betel_cutting_hand():
    # A short length of green vine with two heart-shaped leaves at its nodes.
    return cutting("#7a8a3a", (3.0, 14.0), (12.0, 2.0), leaves=[(10.6, 7.0, -20, "#4a9a34"), (5.6, 6.2, -150, "#58a83e")])


def blackcurrant_cutting_hand():
    # A bare woody stick with small reddish buds set alternately along it.
    return currant_cutting("#8a7058", "#b8443a")


def currant_cutting(stem_colour, bud_colour):
    """A bare woody currant stick with buds set alternately along it, standing steeply on the left, clear of the top right
    corner, where the picture of its berries tells the blackcurrant and redcurrant cuttings apart."""
    return cutting(stem_colour, (3.6, 15.0), (6.6, 0.6), buds=[(3, 12), (6, 9), (4, 6), (7, 3)], bud_colour=bud_colour)


def cabbage_seeds_hand():
    # Tiny round seeds, dark reddish-brown, with a dull sheen.
    s = Sprite()
    seed = ramp("#4a2c26", spread=0.6)
    for cx, cy, _ in SMALL_SEED_SPOTS_LOW:
        s.ellipse(seed, cx, cy, 1.3, 1.3)
        s.put(int(cx) - 1, int(cy) - 1, ramp("#9a6a5a")[BASE])
    return s


def caraway_seed_hand():
    s = Sprite()
    seed = ramp("#7a5a3a", spread=0.55)
    stripe = ramp("#c8aa7a")[BASE]
    # Slim, dark-brown seeds, each with a pale stripe running along its length.
    for cx, cy, angle in SEED_SPOTS:
        cos, sin = math.cos(math.radians(angle)), math.sin(math.radians(angle))
        s.ellipse(seed, cx, cy, 2.7, 1.1, angle=angle)
        for t in (-1, 0, 1):
            s.put(round(cx + cos * t - 0.5), round(cy + sin * t - 0.5), stripe)
    return s

def cardamom_seeds_hand():
    # Small, dark, sticky, angular seeds.
    return seed_scatter("#3c2c24", length=1.6, width=1.3, spread=0.6)


def celery_seeds_hand():
    # Tiny ridged brown seeds.
    s = Sprite()
    seed = ramp("#7a6646", spread=0.6)
    for cx, cy, angle in SMALL_SEED_SPOTS:
        s.ellipse(seed, cx, cy, 1.4, 1.0, angle=angle)
        s.put(int(cx), int(cy), ramp("#b8a07a")[BASE])
    return s


def coriander_seed_hand():
    # Round, ribbed, tan seeds.
    s = seed_scatter("#c8a46a", length=1.8, width=1.8, spread=0.6)
    for cx, cy, _ in SEED_SPOTS:
        s.put(int(cx), int(cy) + 1, ramp("#c8a46a", spread=0.6)[SHADOW])
    return s


def courgette_seeds_hand():
    # Flat, cream ovals.
    return seed_scatter("#ece2bc", length=2.2, width=1.5, spots=SEED_SPOTS_LOW, spread=0.45)


def cucumber_seeds_hand():
    # Slimmer, pointed, pale seeds.
    return seed_scatter("#eee8c8", length=2.4, width=1.1, spots=SEED_SPOTS_LOW, spread=0.45)

def striped_seeds(colour, stripe_colour, length, width, spread=0.5):
    """Slim seeds with a ridge along their length in another colour, such as cumin and lettuce."""
    s = Sprite()
    seed, stripe = ramp(colour, spread=spread), ramp(stripe_colour)[BASE]
    for cx, cy, angle in SEED_SPOTS:
        cos, sin = math.cos(math.radians(angle)), math.sin(math.radians(angle))
        s.ellipse(seed, cx, cy, length, width, angle=angle)
        for t in (-1, 0, 1):
            s.put(round(cx + cos * t - 0.5), round(cy + sin * t - 0.5), stripe)
    return s


def cumin_seed_hand():
    # Slim, olive-grey seeds with pale ridges, tapering at both ends.
    return striped_seeds("#76763e", "#bebc7e", 2.5, 1.15)


def dill_seeds_hand():
    # Flat, oval seeds: brown in the middle with a broad pale rim.
    s = Sprite()
    rim, middle = ramp("#ab9c7e", spread=0.55), ramp("#5e4c3c")
    for cx, cy, angle in SEED_SPOTS:
        cos, sin = math.cos(math.radians(angle)), math.sin(math.radians(angle))
        s.ellipse(rim, cx, cy, 2.4, 1.7, angle=angle)
        for t in (-1, 0, 1):
            s.put(round(cx + cos * t - 0.5), round(cy + sin * t - 0.5), middle[BASE])
    return s


def green_chili_seeds_hand():
    return pepper_seeds("#e6d088")


def holy_basil_seeds_hand():
    # Tiny round seeds, dark brown rather than basil's black.
    s = Sprite()
    seed = ramp("#4e3626", spread=0.55)
    for cx, cy, _ in SMALL_SEED_SPOTS_LOW:
        s.ellipse(seed, cx, cy, 1.2, 1.2)
        s.put(int(cx) - 1, int(cy) - 1, ramp("#8c6a50")[BASE])
    return s


def lettuce_seeds_hand():
    # Slim, pointed, pale grey seeds with fine ridges.
    return striped_seeds("#cbc4b0", "#8a8272", 2.6, 1.3, spread=0.8)

def elder_cutting_hand():
    # Two straight grey-brown sticks, bound together, with knobbly paired buds at the nodes and pale corky specks.
    s = Sprite()
    bark = ramp("#94785e", spread=0.6)
    for start, end in [((1.8, 12.4), (12.4, 1.8)), ((4.2, 15.0), (14.6, 4.6))]:
        s.capsule(bark, [start, end], (1.5, 1.3))
        s.put(int(start[0]), int(start[1]), bark[HIGHLIGHT])
    # A pale band across each stick at its nodes.
    s.pixels_at(ramp("#c8aa8a")[BASE], [(4, 9), (5, 10), (8, 5), (9, 6), (8, 10), (9, 11), (12, 6), (13, 7)])
    return s


def grape_cane(colour, bud_colour):
    """A length of grape cane, cut on a slant at the top, swollen at each node with a bud. It stands steeply on the left,
    clear of the top right corner, where the picture of its grape tells the three grape cuttings apart."""
    s = Sprite()
    cane = ramp(colour, spread=0.7)
    s.capsule(cane, [(3.6, 15.0), (6.4, 1.2)], (1.8, 1.5))
    s.pixels_at(cane[HIGHLIGHT], [(6, 0), (7, 0), (7, 1)])  # The fresh, slanted cut.
    bud = ramp(bud_colour)
    for x, y in [(4, 12), (5, 8), (6, 4)]:
        # A swollen node: a ring across the cane, with a bud sitting on its right.
        s.pixels_at(cane[SHADOW], [(x - 2, y), (x - 1, y), (x, y), (x + 1, y)])
        s.pixels_at(bud[BASE], [(x + 2, y - 1)])
        s.pixels_at(bud[LIGHT], [(x + 2, y)])
    return s


def grape_cutting_hand():
    return grape_cane("#b88a4c", "#8a5232")


def red_grape_cutting_hand():
    return grape_cane("#9a6446", "#7a3a3a")


def white_grape_cutting_hand():
    return grape_cane("#c8a86c", "#8a6a3a")


def hop_rhizome_hand():
    # A thick, knobbly, light-brown underground stem with a few wiry rootlets, pink buds, and a white shoot.
    s = Sprite()
    root = ramp("#a87c56", spread=0.75)
    s.capsule(root, [(1.4, 11.4), (7.0, 9.0), (14.6, 5.6)], (2.3, 1.8))
    rootlet = ramp("#6a4a34")[BASE]
    # A tuft of rootlets at the old end, curling out every way.
    s.pixels_at(rootlet, [(0, 9), (0, 8), (1, 14), (0, 15), (3, 14), (3, 15), (2, 8), (1, 7)])
    s.pixels_at(ramp("#d89a8a")[BASE], [(6, 9), (12, 6)])
    s.pixels_at(ramp("#f0ece0")[BASE], [(9, 6), (9, 5), (10, 4)])
    s.pixels_at(root[HIGHLIGHT], [(3, 10), (8, 8), (13, 5)])
    return s


def long_bean_seeds_hand():
    # Slim, kidney-shaped, red-brown beans, each with a white eye in the dip on its inner curve.
    s = Sprite()
    bean, eye = ramp("#94362c", spread=0.7), ramp("#ece6d8")[BASE]
    for cx, cy, angle in [(4.6, 4.2, -15), (11.4, 4.6, 25), (4.0, 10.4, 50), (9.6, 9.6, -20), (6.8, 14.2, 10), (12.6, 13.6, -45)]:
        cos, sin = math.cos(math.radians(angle)), math.sin(math.radians(angle))
        s.ellipse(bean, cx, cy, 3.0, 1.7, angle=angle)
        # The dip: the middle of the upper edge turns to the eye.
        s.put(round(cx + sin * 1.2 - 0.5), round(cy - cos * 1.2 - 0.5), eye)
        s.put(round(cx - cos * 1.5 + sin * 0.2 - 0.5), round(cy - sin * 1.5 - cos * 0.2 - 0.5), bean[HIGHLIGHT])
    return s

def oregano_hand():
    s = Sprite()
    stem = ramp("#7a4a3a")
    leaf = ramp("#5e9e46", spread=0.55)
    # Three reddish sprigs gathered at the bottom right, with small, darker, oval leaves in pairs and pink flower tips.
    for (x1, y1), pairs in (((2, 3), [(4, 6), (7, 9)]), ((8, 1), [(8, 4), (9, 8)]), ((14, 4), [(12, 6), (11, 10)])):
        s.line(stem[BASE], 13, 15, x1, y1)
        for x, y in pairs:
            s.ellipse(leaf, x - 0.8, y + 0.5, 2.0, 1.4, angle=-20)
            s.ellipse(leaf, x + 1.8, y + 0.5, 2.0, 1.4, angle=20)
            s.put(x - 1, y, leaf[HIGHLIGHT])
        flower = ramp("#c870a0")
        s.pixels_at(flower[BASE], [(x1, y1), (x1 - 1, y1 + 1), (x1 + 1, y1 + 1)])
        s.put(x1, y1 - 1, flower[LIGHT])
    return s


def tiny_round_seeds(colour, glint):
    """Many tiny round seeds kept clear of the top right corner, for herbs whose seeds carry a plant picture."""
    s = Sprite()
    seed = ramp(colour, spread=0.55)
    for cx, cy, _ in SMALL_SEED_SPOTS_LOW:
        s.ellipse(seed, cx, cy, 1.2, 1.1)
        s.put(int(cx) - 1, int(cy) - 1, ramp(glint)[BASE])
    return s


def marjoram_seeds_hand():
    # Tiny round light-brown seeds, the same as oregano's.
    return tiny_round_seeds("#8e5c36", "#c49a70")


def mint_seeds_hand():
    # Tiny round seeds, a very dark brown.
    return tiny_round_seeds("#3a2418", "#7a5a48")


def mustard_seed_hand():
    # Many small round seeds, yellow-tan, with the odd dark one among them.
    s = Sprite()
    seed, dark = ramp("#d8a650", spread=0.6), ramp("#4a2c2a", spread=0.5)
    for i, (cx, cy, _) in enumerate(SMALL_SEED_SPOTS):
        tones = dark if i in (4, 11) else seed
        s.ellipse(tones, cx, cy, 1.4, 1.4)
        s.put(int(cx) - 1, int(cy) - 1, tones[HIGHLIGHT])
    return s


def nutmeg_seed_hand():
    # Two whole nutmegs: brown ovals with pale wrinkles, the back one still wrapped in its lacy red mace.
    s = Sprite()
    nut, mace = ramp("#7a4a2c", spread=0.7), ramp("#d0302a")
    s.ellipse(nut, 10.0, 6.0, 4.4, 3.4, angle=-30)
    for x, y in [(7, 4), (9, 3), (11, 4), (8, 6), (10, 6), (12, 5), (9, 8), (11, 8), (13, 7), (7, 8)]:
        s.put(x, y, mace[BASE])
    s.pixels_at(mace[LIGHT], [(10, 3), (8, 5), (12, 7)])
    s.ellipse(nut, 6.0, 10.6, 4.6, 3.6, angle=-20)
    wrinkle = ramp("#b08a64")[BASE]
    s.pixels_at(wrinkle, [(3, 10), (4, 9), (6, 9), (7, 10), (5, 12), (8, 12), (9, 11), (4, 12)])
    s.pixels_at(nut[HIGHLIGHT], [(4, 8)])
    return s


def oregano_seeds_hand():
    return marjoram_seeds_hand()

def pandan_cutting_hand():
    # A young offshoot pulled from the base of a plant: a short, pale, rooted stub with a tight fan of strap leaves.
    s = Sprite()
    stub = ramp("#c4aa76", spread=0.7)
    # Four soft blades that rise and then arch over, drooping at their tips, unlike a pineapple's stiff spikes.
    for i, (mid, tip) in enumerate([((4.0, 4.0), (0.8, 7.6)), ((6.6, 1.6), (3.0, 1.0)), ((9.6, 1.4), (13.0, 1.0)), ((12.0, 4.0), (15.0, 7.8))]):
        leaf = ramp("#4aa83a" if i % 2 else "#3e9a34", spread=0.7)
        s.capsule(leaf, [(8.0, 12.6), mid, tip], (1.6, 0.6))
    s.pixels_at(ramp("#8ad070")[BASE], [(7, 6), (7, 7), (8, 8), (9, 6)])
    s.ellipse(stub, 8.0, 13.4, 2.6, 1.6)
    s.pixels_at(stub[HIGHLIGHT], [(7, 13)])
    s.pixels_at(ramp("#6a4a34")[BASE], [(6, 15), (9, 15), (10, 15)])  # Root stubs.
    return s


def paprika_pepper_seeds_hand():
    return pepper_seeds("#e8cc84")


def parsley_seeds_hand():
    # Small, grey-brown, ribbed seeds, a little curved, each with a paler rib down its middle.
    s = Sprite()
    seed, rib = ramp("#7c705c", spread=0.55), ramp("#b8ae96")[BASE]
    for cx, cy, angle in SEED_SPOTS:
        cos, sin = math.cos(math.radians(angle)), math.sin(math.radians(angle))
        s.ellipse(seed, cx, cy, 1.9, 1.3, angle=angle)
        s.put(round(cx - 0.5), round(cy - 0.5), rib)
    return s


def peppercorn_cutting_hand():
    # A length of jointed green vine with two glossy, pointed leaves, short aerial roots at a node, and a young spike
    # of green peppercorns, to tell it from the betel vine.
    s = cutting("#5e7a34", (3.0, 15.0), (11.0, 2.0), leaves=[(9.0, 5.0, -30, "#3a8a30"), (4.6, 7.0, -140, "#40902e")])
    s.pixels_at(ramp("#8a6a4a")[BASE], [(4, 12), (3, 11), (6, 13)])  # Aerial roots at the lower node.
    berry = ramp("#8cc048", spread=0.7)
    s.put(12, 7, ramp("#6a8a3a")[SHADOW])
    for x, y in [(12, 8), (13, 9), (12, 10), (13, 11), (12, 12), (13, 13)]:
        s.put(x, y, berry[BASE])
    s.pixels_at(berry[HIGHLIGHT], [(12, 8), (12, 10), (12, 12)])
    s.pixels_at(berry[OUTLINE], [(14, 9), (14, 11), (14, 13), (11, 9), (11, 11)])
    return s


def pineapple_crown_hand():
    # The leafy top twisted from a pineapple: stiff, spiky blue-green leaves fanning up from a short golden stub of fruit.
    s = Sprite()
    gold = ramp("#d49a32", spread=0.8)
    s.ellipse(gold, 8.0, 13.4, 3.6, 2.0)
    s.pixels_at(gold[SHADOW], [(6, 13), (8, 14), (10, 13)])
    for i, tip in enumerate([(1.4, 6.0), (3.6, 2.0), (7.0, 0.6), (9.4, 0.6), (12.6, 2.6), (14.6, 6.4)]):
        leaf = ramp("#5a8a78" if i % 2 else "#4c7c6a", spread=0.75)
        s.capsule(leaf, [(8.0, 11.6), tip], (1.3, 0.5))
    for i, tip in enumerate([(5.0, 4.0), (11.0, 4.0)]):
        leaf = ramp("#6a9c86", spread=0.7)
        s.capsule(leaf, [(8.0, 11.4), tip], (1.2, 0.5))
    s.pixels_at(ramp("#9ac0ac")[BASE], [(8, 6), (8, 7), (8, 8)])
    return s

def poblano_chili_seeds_hand():
    return pepper_seeds("#e6d290")


def radish_seeds_hand():
    # Small, slightly irregular seeds, a lighter reddish-brown than cabbage seeds and a little larger.
    s = Sprite()
    seed = ramp("#7e4a30", spread=0.6)
    for cx, cy, angle in SMALL_SEED_SPOTS_LOW:
        s.ellipse(seed, cx, cy, 1.5, 1.2, angle=angle)
        s.put(int(cx) - 1, int(cy) - 1, ramp("#b88a6a")[BASE])
    return s


def raspberry_cutting_hand():
    # A length of reddish-brown cane covered in fine pale prickles, with a pair of fresh toothed leaves at the top.
    s = cutting("#8a4a3a", (3.0, 15.0), (11.6, 3.0), leaves=[(12.0, 3.0, -50, "#4a9a3a"), (8.4, 2.6, -120, "#56a640")])
    # Prickles stand out from both edges of the cane, alternating sides along it.
    (x0, y0), (x1, y1) = (3.0, 15.0), (11.6, 3.0)
    length = math.dist((x0, y0), (x1, y1))
    nx, ny = (y1 - y0) / length, -(x1 - x0) / length
    for i, t in enumerate((0.15, 0.3, 0.45, 0.6, 0.72)):
        side = 1.6 if i % 2 else -1.6
        s.put(round(x0 + (x1 - x0) * t + nx * side - 0.5), round(y0 + (y1 - y0) * t + ny * side - 0.5), ramp("#d8c0a0")[BASE])
    for cx, cy in [(12.0, 3.0), (8.4, 2.6)]:
        s.put(int(cx) + 1, int(cy) + 1, ramp("#2e7a2a")[BASE])  # The toothed edge.
    return s


def red_chili_seeds_hand():
    return pepper_seeds("#e4c67c")

def redcurrant_cutting_hand():
    # Paler, greyer bark than blackcurrant, with pinkish buds.
    return currant_cutting("#9a8670", "#c8786a")


def rhubarb_crown_hand():
    # A lumpy brown root crown dug from the ground, with fat, pointed red buds pushing up through it, crinkled leaf
    # breaking green at their tips, and short broken root ends along its underside.
    s = Sprite()
    root = ramp("#86603e", spread=0.75)
    rootlet = root[SHADOW]
    s.pixels_at(rootlet, [(2, 13), (1, 14), (5, 14), (4, 15), (9, 14), (10, 15), (13, 13), (14, 14), (7, 14)])
    s.shape(lambda x, y: ((x - 8.0) / 6.6) ** 2 + ((y - 10.4) / 3.0) ** 2 <= 1 or ((x - 11.6) / 2.6) ** 2 + ((y - 9.0) / 2.4) ** 2 <= 1,
            root, (8.0, 10.4), (6.6, 3.0), texture=speckle(5, 0.3))
    s.pixels_at(root[HIGHLIGHT], [(4, 9), (12, 8)])
    bud = ramp("#c8304a", spread=0.75)
    leaf = ramp("#8ab43a")
    for cx, cy, ry in [(4.6, 6.6, 2.2), (8.4, 5.4, 3.0), (12.0, 5.8, 2.0)]:
        s.ellipse(bud, cx, cy, 1.6, ry)
        s.put(int(cx) - 1, int(cy), bud[HIGHLIGHT])
        top = int(cy - ry)
        s.pixels_at(leaf[BASE], [(int(cx), top), (int(cx) - 1, top + 1)])
        s.put(int(cx) + 1, top, leaf[LIGHT])
    return s


def roselle_seeds_hand():
    # Kidney-shaped, dark brown seeds with a faint paler speckle.
    s = Sprite()
    seed = ramp("#5a3a28", spread=0.6)
    for cx, cy, angle in SEED_SPOTS_LOW:
        s.ellipse(seed, cx, cy, 1.9, 1.5, angle=angle)
        s.put(int(cx), int(cy), ramp("#8a6a50")[BASE])
        s.put(round(cx + math.sin(math.radians(angle)) * 1.2 - 0.5), round(cy - math.cos(math.radians(angle)) * 1.2 - 0.5), seed[SHADOW])  # The notch.
    return s


def rosemary_cutting_hand():
    # A rosemary sprig with its lower needles stripped: bare woody stem below, cut on a slant, and needles above.
    s = Sprite()
    needle, wood = ramp("#3f7048"), ramp("#7a6040", spread=0.7)
    stem = s.line(wood[SHADOW], 4, 15, 11, 1)
    s.pixels_at(wood[HIGHLIGHT], [(4, 15)])
    for i, (x, y) in enumerate(stem[:-1]):
        if y > 8:
            continue
        right = [(x + 1 + k, y - k // 2) for k in range(2)]
        left = [(x - 1 - k // 2, y - 1 - k) for k in range(2)]
        s.pixels_at(needle[LIGHT if i % 2 else BASE], right)
        s.pixels_at(needle[SHADOW if i % 2 else BASE], left)
    s.pixels_at(needle[HIGHLIGHT], [(11, 0), (10, 2), (9, 5)])
    s.pixels_at(wood[LIGHT], [(5, 12), (6, 10)])  # Scars where needles were stripped.
    return s

def savory_hand():
    s = Sprite()
    stem = ramp("#6a7a44")
    leaf = ramp("#5a9248", spread=0.6)
    # Three upright sprigs gathered at the bottom, with narrow, pointed leaves in pairs and tiny pale lilac flowers.
    for (x1, y1), nodes in (((3, 2), [(5, 9), (4, 5)]), ((8, 0), [(8, 8), (8, 4)]), ((13, 2), [(11, 9), (12, 5)])):
        s.line(stem[BASE], 8, 15, x1, y1)
        for x, y in nodes:
            s.ellipse(leaf, x - 1.6, y + 0.5, 2.0, 1.1, angle=-35)
            s.ellipse(leaf, x + 2.6, y + 0.5, 2.0, 1.1, angle=35)
        s.pixels_at(ramp("#d8c4e8")[BASE], [(x1, y1), (x1 - 1, y1 + 1)])
    return s


def savory_seeds_hand():
    # Small oval seeds, dark grey-black with a dull sheen.
    s = Sprite()
    seed = ramp("#3a3a3c", spread=0.5)
    for cx, cy, angle in SMALL_SEED_SPOTS_LOW:
        s.ellipse(seed, cx, cy, 1.5, 1.1, angle=angle)
        s.put(int(cx) - 1, int(cy) - 1, ramp("#8a8a86")[BASE])
    return s


def sesame_seed_hand():
    # Many small, flat, pale tan teardrops.
    s = Sprite()
    # Set by hand: the ramp turns such a pale tan peach at its outline, and these seeds are mostly outline.
    seed = [(150, 128, 90), (196, 174, 130), (222, 206, 166), (236, 224, 192), (246, 240, 222)]
    for cx, cy, angle in SMALL_SEED_SPOTS:
        s.ellipse(seed, cx, cy, 1.6, 1.1, angle=angle)
        s.put(round(cx + math.cos(math.radians(angle)) * 1.2 - 0.5), round(cy + math.sin(math.radians(angle)) * 1.2 - 0.5), seed[SHADOW])
    return s


def spring_onion_seeds_hand():
    # Flat, pale grey-green teardrop seeds, each with a darker middle.
    s = Sprite()
    seed = ramp("#a8ad86", spread=0.5)
    for cx, cy, angle in SEED_SPOTS:
        cos, sin = math.cos(math.radians(angle)), math.sin(math.radians(angle))
        s.ellipse(seed, cx, cy, 2.2, 1.4, angle=angle)
        s.put(round(cx - 0.5), round(cy - 0.5), seed[SHADOW])
        s.put(round(cx - cos * 1.2 - 0.5), round(cy - sin * 1.2 - 0.5), seed[HIGHLIGHT])
    return s


def strawberry_runner_hand():
    # A pinkish-red runner stem with a small new plant on it: three toothed leaflets and a brown root bud at its base.
    s = Sprite()
    runner = ramp("#c06a5a", spread=0.6)
    s.capsule(runner, [(0.4, 14.6), (4.0, 12.6), (7.4, 12.6)], (0.7, 0.7))
    s.capsule(runner, [(7.4, 12.6), (11.4, 12.4), (15.4, 9.0)], (0.7, 0.6))
    stalk = ramp("#8ab05a")
    s.line(stalk[BASE], 8, 12, 7, 7)
    leaf = ramp("#5aa83a", spread=0.7)
    for cx, cy, angle in [(4.0, 5.6, -30), (7.6, 2.8, -90), (11.2, 5.6, -150)]:
        mask = s.ellipse(leaf, cx, cy, 2.3, 1.7, angle=angle)
        for x, y in mask:
            if (x + y) % 2 == 0 and s.pixels[y][x] == leaf[OUTLINE]:
                s.put(x, y, leaf[SHADOW])  # A toothed edge.
        s.put(int(cx), int(cy), leaf[HIGHLIGHT])
    root = ramp("#7a4a2a")
    s.ellipse(root, 7.8, 13.6, 1.4, 1.2)
    s.pixels_at(root[SHADOW], [(7, 15), (9, 15)])
    return s


def sunflower_seeds_hand():
    # Pointed oval seeds, black with grey-white stripes running along their length.
    s = Sprite()
    seed = ramp("#4a4844", spread=0.6)
    stripe = ramp("#d4d0c6")[BASE]
    for cx, cy, angle in PILE_SPOTS:
        cos, sin = math.cos(math.radians(angle)), math.sin(math.radians(angle))
        s.ellipse(seed, cx, cy, 2.8, 1.6, angle=angle)
        for t in (-1, 0, 1):
            s.put(round(cx + cos * t - 0.5), round(cy + sin * t - 0.5), stripe)
    return s


PILE_SPOTS = [(4.4, 4.2, -30), (11.0, 4.0, 30), (3.8, 10.4, 60), (8.6, 8.4, -10), (12.4, 11.6, -60), (7.0, 13.4, 20)]

def tea_cutting_hand():
    # A single-leaf tea cutting: a short length of green-brown stem cut on a slant, one glossy, toothed leaf, and a
    # small bud where the leaf meets the stem.
    s = Sprite()
    stem = ramp("#8e8a4c", spread=0.5)
    s.capsule(stem, [(5.0, 15.0), (7.4, 6.0)], (1.5, 1.2))
    s.put(5, 15, stem[HIGHLIGHT])  # The fresh, slanted cut.
    leaf = ramp("#2e8a34", spread=0.75)
    mask = s.ellipse(leaf, 10.4, 5.4, 5.0, 2.6, angle=-35)
    for x, y in mask:
        if (x + 2 * y) % 3 == 0 and s.pixels[y][x] == leaf[OUTLINE]:
            s.put(x, y, leaf[SHADOW])  # The toothed edge.
    s.line(leaf[LIGHT], 7, 8, 13, 3)
    s.pixels_at(leaf[HIGHLIGHT], [(9, 4), (11, 3)])
    bud = ramp("#a8d070", spread=0.7)
    s.ellipse(bud, 6.4, 5.0, 1.0, 1.6)
    return s


def thai_aubergine_seeds_hand():
    return aubergine_seeds_hand()


def thai_basil_seeds_hand():
    return basil_seeds_hand()


def thyme_cutting_hand():
    # A single woody thyme sprig, its lower leaves stripped and the stem cut on a slant, tiny paired leaves above.
    s = Sprite()
    wood, leaf = ramp("#8a7a4a"), ramp("#5e9a40")
    points = s.line(wood[SHADOW], 5, 15, 11, 1)
    s.put(5, 15, wood[HIGHLIGHT])
    for i, (x, y) in enumerate(points):
        if y <= 8 and i % 2 == 0:
            s.pixels_at(leaf[BASE], [(x - 1, y), (x + 1, y)])
            s.put(x - 2, y - 1 if i % 4 else y, leaf[LIGHT])
            s.put(x + 2, y - 1 if i % 4 == 0 else y, leaf[SHADOW])
    s.pixels_at(leaf[LIGHT], [(11, 0), (12, 0), (10, 1)])
    s.pixels_at(wood[LIGHT], [(6, 12), (7, 10)])  # Scars where leaves were stripped.
    return s


def toasted_cumin_seed_hand():
    # Cumin seeds toasted to a warm brown, their ridges darkened.
    return striped_seeds("#6e5030", "#a8865a", 2.5, 1.15)

def toasted_pumpkin_seeds_hand():
    # Flat, cream pumpkin seeds in their shells, each with a raised rim, toasted golden in patches.
    s = Sprite()
    shell = plain_ramp("#e2d4ae")
    toast = ramp("#c89a4a")[BASE]
    for i, (cx, cy, angle) in enumerate(PILE_SPOTS):
        cos, sin = math.cos(math.radians(angle)), math.sin(math.radians(angle))
        s.ellipse(shell, cx, cy, 2.9, 2.0, angle=angle)
        s.put(round(cx - 0.5), round(cy - 0.5), shell[SHADOW])
        s.put(round(cx + cos * (1.0 if i % 2 else -1.0) - 0.5), round(cy + sin * (1.0 if i % 2 else -1.0) - 0.5), toast)
        s.put(round(cx + cos * 2.3 - 0.5), round(cy + sin * 2.3 - 0.5), shell[SHADOW])  # The pointed end.
    return s


def toasted_sesame_seeds_hand():
    # Sesame toasted to a golden brown.
    s = Sprite()
    seed = [(112, 72, 34), (160, 110, 56), (196, 146, 80), (218, 176, 112), (236, 208, 150)]
    for cx, cy, angle in SMALL_SEED_SPOTS:
        s.ellipse(seed, cx, cy, 1.6, 1.1, angle=angle)
        s.put(round(cx + math.cos(math.radians(angle)) * 1.2 - 0.5), round(cy + math.sin(math.radians(angle)) * 1.2 - 0.5), seed[SHADOW])
    return s


def tomatillo_seeds_hand():
    # Tiny, flat, round tan seeds.
    s = Sprite()
    seed = plain_ramp("#c8ac7a")
    for cx, cy, _ in SMALL_SEED_SPOTS_LOW:
        s.ellipse(seed, cx, cy, 1.3, 1.2)
        s.put(int(cx) - 1, int(cy) - 1, seed[HIGHLIGHT])
    return s


def tomato_seeds_hand():
    # Flat, fuzzy, pale buff seeds, rounded with a little point.
    s = Sprite()
    seed = plain_ramp("#e0cc9c")
    for cx, cy, angle in SEED_SPOTS_LOW:
        s.ellipse(seed, cx, cy, 1.8, 1.5, angle=angle)
        s.put(int(cx) - 1, int(cy) - 1, seed[HIGHLIGHT])
        s.put(round(cx + math.cos(math.radians(angle)) * 1.6 - 0.5), round(cy + math.sin(math.radians(angle)) * 1.6 - 0.5), seed[SHADOW])
    return s


def vanilla_cutting_hand():
    # A length of thick, bright green, fleshy vanilla vine with a flat oblong leaf and a pale aerial root at a node.
    s = Sprite()
    vine = ramp("#4aa83c", spread=0.6)
    s.capsule(vine, [(3.0, 15.0), (12.6, 1.0)], (1.3, 1.1))
    s.put(3, 15, vine[HIGHLIGHT])  # The fresh cut.
    leaf = ramp("#3e9a38", spread=0.6)
    s.ellipse(leaf, 11.4, 9.2, 3.6, 1.8, angle=-60)
    s.line(leaf[LIGHT], 10, 11, 12, 7)
    s.pixels_at(vine[SHADOW], [(6, 10), (7, 10), (9, 6), (10, 6)])  # Nodes.
    root = ramp("#c8c4b0")
    s.pixels_at(root[BASE], [(5, 11), (4, 12), (3, 12), (2, 13)])
    return s

def wasabi_root_hand():
    # A knobbly green rhizome, broad at the top where the leaf stalks were cut and tapering to a rough brown tip, its
    # surface bumpy with old leaf scars.
    s = Sprite()
    stalk = ramp("#6aa84a", spread=0.6)
    for end in [(5.0, 1.0), (8.4, 0.4), (11.6, 1.6)]:
        s.capsule(stalk, [(8.0, 5.0), end], (0.9, 0.7))
        s.put(round(end[0] - 0.5), round(end[1] - 0.5), stalk[HIGHLIGHT])  # The cut.
    root = ramp("#6a8a48", spread=0.6)
    s.capsule(root, [(8.0, 6.0), (7.4, 11.0), (7.0, 15.0)], (3.6, 1.4))
    # Bumps and scars in rings around the root.
    for x, y in [(5, 7), (8, 7), (10, 8), (6, 9), (9, 10), (5, 11), (8, 12), (6, 13)]:
        s.put(x, y, root[SHADOW])
    for x, y in [(6, 7), (9, 8), (7, 10), (6, 12)]:
        s.put(x, y, root[HIGHLIGHT])
    s.pixels_at(ramp("#7a6a4a")[BASE], [(7, 15), (6, 14)])
    for x, y in [(4, 8), (11, 7), (4, 11), (10, 11)]:
        s.put(x, y, root[BASE])  # Knobs breaking the outline.
    return s


def spice_bowl(powder, fleck=None, seed=7, bowl_colour="#d6d8dc"):
    """A ground spice: a small pale ceramic bowl heaped with fine powder, flecked and lit on top. The bowl is pale so
    that brown and red spices stand out against it."""
    s = Sprite()
    wood = plain_ramp(bowl_colour)
    s.shape(lambda x, y: y >= 9.0 and ((x - 7.5) / 7.0) ** 2 + ((y - 9.0) / 6.0) ** 2 <= 1, wood, (7.5, 11.0), (7.0, 5.0))
    s.pixels_at(wood[SHADOW], [(x, 14) for x in range(5, 11)])
    tones = ramp(powder, spread=0.6)
    s.shape(lambda x, y: y <= 9.5 and ((x - 7.5) / 6.2) ** 2 + ((y - 9.5) / 5.4) ** 2 <= 1, tones, (7.5, 7.0), (6.2, 3.0))
    rng = random.Random(seed)
    for y in range(4, 10):
        for x in range(2, 14):
            if s.pixels[y][x] in (None, tones[OUTLINE]):
                continue
            roll = rng.random()
            if roll < 0.18:
                s.put(x, y, tones[LIGHT if y < 7 else SHADOW])
            elif fleck and roll < 0.26:
                s.put(x, y, ramp(fleck)[BASE])
    s.pixels_at(tones[HIGHLIGHT], [(6, 5), (7, 5), (5, 6)])
    return s


def cumin_hand():
    # Ground cumin: a warm brown powder.
    return spice_bowl("#9a6a3a", fleck="#6a4a2a")


def cardamom_hand():
    # Ground cardamom: a grey-green powder flecked with the dark seeds.
    return spice_bowl("#9a9a76", fleck="#3c2c24", seed=11)


def wasabi_hand():
    # Freshly grated wasabi: a soft, pale green mound with a peaked tip, on a small dark grater.
    s = Sprite()
    grater = ramp("#4a4440", spread=0.6)
    s.shape(lambda x, y: 10.0 <= y <= 14.0 and 1.0 <= x <= 14.0, grater, (7.5, 12.0), (6.5, 2.0))
    for x in range(3, 13, 2):
        s.pixels_at(grater[LIGHT], [(x, 12)])
    paste = ramp("#8ac05a", spread=0.6)
    s.shape(lambda x, y: y <= 11.0 and ((x - 7.5) / 4.6) ** 2 + ((y - 11.0) / 6.4) ** 2 <= 1 and abs(x - 7.5) <= 4.6 - (11.0 - y) * 0.5,
            paste, (7.5, 8.0), (4.6, 4.0))
    s.pixels_at(paste[HIGHLIGHT], [(6, 6), (7, 5), (6, 8)])
    s.pixels_at(paste[SHADOW], [(9, 9), (10, 10), (8, 10)])
    return s

BOWL = "#d6d8dc"
# A glazed clay bowl, for white and cream foods that would vanish in the pale one.
CLAY = "#b8744a"


def bowl(contents, mound=0.0, texture=None, flecks=(), seed=3, bowl_colour=BOWL, plain=False):
    """A pale ceramic bowl seen from the side and a little above, filled with {contents}: a flat surface for batters and
    soups, or heaped {mound} pixels above the rim for mashes. Flecks are (colour, chance) pairs scattered over the top."""
    s = Sprite()
    ceramic = plain_ramp(bowl_colour)
    s.shape(lambda x, y: y >= 8.0 and ((x - 7.5) / 7.2) ** 2 + ((y - 8.0) / 7.0) ** 2 <= 1, ceramic, (7.5, 11.0), (7.2, 5.0))
    s.pixels_at(ceramic[SHADOW], [(x, 14) for x in range(5, 11)])
    s.pixels_at(ceramic[HIGHLIGHT], [(2, 10), (3, 11)])
    food = plain_ramp(contents) if plain else ramp(contents, spread=0.6)
    top = 8.0 - mound
    s.shape(lambda x, y: (((x - 7.5) / 6.4) ** 2 + ((y - 8.0) / 1.8) ** 2 <= 1) or
            (mound and y <= 8.0 and ((x - 7.5) / 6.0) ** 2 + ((y - 8.0) / (mound + 0.6)) ** 2 <= 1),
            food, (7.5, 8.0 - mound / 2), (6.4, 1.8 + mound / 2), texture=texture)
    rng = random.Random(seed)
    for y in range(int(top) - 1, 10):
        for x in range(1, 15):
            if s.pixels[y][x] in (None, food[OUTLINE]) or s.pixels[y][x] in ceramic:
                continue
            for colour, chance in flecks:
                if rng.random() < chance:
                    s.put(x, y, ramp(colour)[BASE])
                    break
    return s


def bumpy(x, y):
    """A lumpy surface for mashes."""
    return 1 if (x * 3 + y * 5) % 7 == 0 else -1 if (x * 5 + y * 3) % 7 == 0 else 0


def achiote_potato_mash_hand():
    # Mashed potato coloured a warm orange by achiote oil, heaped in a bowl, with a glossy streak of the oil.
    s = bowl("#e8a040", mound=3.0, texture=bumpy)
    s.pixels_at(ramp("#d0601e")[BASE], [(9, 6), (10, 6), (11, 7)])
    return s


def aebleskiver_hand():
    # Three round, golden-brown æbleskiver on a plate, dusted with sugar, with a dab of red jam.
    s = Sprite()
    plate = plain_ramp("#e2e4e8")
    s.ellipse(plate, 7.5, 12.6, 7.4, 2.6)
    ball = ramp("#c8843a", spread=0.75)
    for cx, cy in [(4.6, 9.4), (10.6, 9.4), (7.6, 6.2)]:
        s.ellipse(ball, cx, cy, 3.4, 3.2, light=(-0.6, -0.8))
        s.pixels_at(ramp("#f4f0ea")[BASE], [(int(cx) - 1, int(cy) - 2), (int(cx), int(cy) - 3), (int(cx) + 1, int(cy) - 2)])
    jam = ramp("#c02a3a", spread=0.6)
    s.ellipse(jam, 13.2, 12.6, 1.6, 1.0)
    s.put(13, 12, jam[HIGHLIGHT])
    return s


def aebleskiver_batter_hand():
    # A bowl of smooth, pale yellow batter, flecked with cardamom, with a glossy ripple.
    s = bowl("#eedc9c", flecks=[("#6a6a4a", 0.06)], plain=True)
    s.pixels_at(plain_ramp("#eedc9c")[HIGHLIGHT], [(4, 7), (5, 7), (6, 8)])
    return s


def cheese_wedge(inside, rind, holes=(), ooze=False, plain=False):
    """A wedge of cheese, tip to the left: the cut face toward us, a lit top edge along its slope, and the rind down the
    curved back on the right. Holes are pixels on the cut face; ooze lets a soft cheese run out at the foot."""
    s = Sprite()
    face = plain_ramp(inside) if plain else ramp(inside, spread=0.6)
    skin = plain_ramp(rind) if plain else ramp(rind, spread=0.7)

    def slope(x):
        return 13.4 - (x - 0.6) * 0.78  # The top edge of the cut face.
    # The wedge and its rind are one shape, so they share an outline; the rind is then painted over the back.
    s.shape(lambda x, y: 0.6 <= x <= 14.6 and slope(x) <= y <= 13.6, face, (6.0, 10.0), (7.0, 5.0))
    for x in range(12, 15):
        for y in range(16):
            if s.pixels[y][x] not in (None, face[OUTLINE]):
                s.put(x, y, skin[BASE] if x < 14 else skin[SHADOW])
    s.pixels_at(skin[HIGHLIGHT], [(12, 6), (12, 7)])
    # The lit top edge of the cut face.
    for x in range(2, 12):
        y = math.ceil(slope(x))
        if s.pixels[y][x] not in (None, face[OUTLINE]):
            s.put(x, y, face[HIGHLIGHT])
    s.pixels_at(face[SHADOW], list(holes))
    if ooze:
        s.pixels_at(face[LIGHT], [(5, 14), (6, 14), (7, 14), (8, 14)])
    return s


def aged_hard_cheese_hand():
    # A wedge of aged hard cheese: deep golden inside with a few small holes, and a hard, waxy brown rind.
    return cheese_wedge("#e6b44a", "#8a5a28", holes=[(6, 11), (9, 9), (10, 12), (4, 12), (8, 12)])


def aged_soft_cheese_hand():
    # A wedge of ripened soft cheese: a white, downy rind and a creamy, pale yellow inside running out at the foot.
    return cheese_wedge("#f0dc8e", "#f2f0e8", ooze=True, plain=True)


def aged_yerba_mate_hand():
    # Aged yerba mate: a heap of chopped dried leaf, a dusty olive-khaki, with pale bits of stem.
    return seed_heap("#8a9058", fleck="#d8d0a8", seed=29)


def ajitsuke_tamago_hand():
    # A marinated egg cut in half: the white stained amber-brown from the soy marinade, the yolk soft and deep orange.
    s = Sprite()
    white = ramp("#c8904a", spread=0.6)
    yolk = ramp("#f0a020", spread=0.6)
    for cx, cy, angle in [(5.0, 6.4, -20), (10.6, 10.0, 20)]:
        s.ellipse(white, cx, cy, 4.4, 3.4, angle=angle)
        s.ellipse(ramp("#f2e8d0", spread=0.4), cx, cy, 3.2, 2.4, angle=angle, outline=False)  # The cut white, paler inside.
        s.ellipse(yolk, cx + 0.3, cy + 0.2, 1.9, 1.6, outline=False)
        s.put(int(cx), int(cy), yolk[HIGHLIGHT])
    return s


def al_pastor_adobo_hand():
    # A bowl of thick, smooth, brick-red chili and achiote paste.
    s = bowl("#b8381e", flecks=[("#e06a2a", 0.08)])
    s.pixels_at(ramp("#b8381e")[HIGHLIGHT], [(4, 7), (5, 7)])
    return s


def al_pastor_meat_hand():
    # A pile of thin pork slices shaved from the spit: red-orange from the adobo, charred dark at the edges, with a
    # couple of bits of golden pineapple.
    s = Sprite()
    meat = ramp("#d8662e", spread=0.6)
    char = ramp("#5a2a1a")[BASE]
    for cx, cy, angle in [(5.0, 6.0, -25), (10.8, 6.6, 20), (4.4, 11.2, 15), (9.6, 11.0, -15), (7.4, 8.6, 5)]:
        mask = s.ellipse(meat, cx, cy, 3.8, 1.9, angle=angle)
        cos, sin = math.cos(math.radians(angle)), math.sin(math.radians(angle))
        s.put(round(cx + cos * 3.0 - 0.5), round(cy + sin * 3.0 - 0.5), char)
        s.put(round(cx - cos * 2.6 - 0.5), round(cy - sin * 2.6 - 0.5), char)
        s.put(round(cx - 0.5), round(cy - 1.0), meat[HIGHLIGHT])
    pineapple = plain_ramp("#f2d460")
    for cx, cy in [(12.6, 12.4), (2.6, 14.0)]:
        s.ellipse(pineapple, cx, cy, 1.4, 1.2)
    return s


def alfajor_biscuits_hand():
    # Three plain, pale, crumbly round biscuits, the halves of alfajores before filling, stacked a little apart.
    s = Sprite()
    dough = plain_ramp("#e6c88c")
    for cx, cy in [(4.8, 4.6), (11.0, 7.0), (6.0, 11.8)]:
        s.ellipse(dough, cx, cy, 3.8, 2.8)
        s.pixels_at(dough[SHADOW], [(int(cx) - 1, int(cy) + 1), (int(cx) + 1, int(cy)), (int(cx) + 2, int(cy) + 1)])  # Crumb.
        s.put(int(cx) - 2, int(cy) - 1, dough[HIGHLIGHT])
    return s

def dough_ball(colour, flour=True):
    """A soft ball of dough, a little flattened where it sits, dusted with flour."""
    s = Sprite()
    dough = plain_ramp(colour)
    s.shape(lambda x, y: ((x - 7.5) / 6.4) ** 2 + ((y - 9.4) / 5.0) ** 2 <= 1 and y <= 14.0, dough, (7.5, 9.4), (6.4, 5.0))
    s.pixels_at(dough[HIGHLIGHT], [(5, 6), (6, 5), (7, 5)])
    s.pixels_at(dough[SHADOW], [(10, 12), (11, 11), (9, 13)])
    if flour:
        s.pixels_at(plain_ramp("#f6f4ee")[BASE], [(4, 8), (9, 7), (11, 9), (6, 11)])
    return s


def alfajor_dough_hand():
    # A soft, pale, buttery dough of cornstarch and flour.
    return dough_ball("#f0e2b8")


def alfajor_rounds_hand():
    # Raw rounds cut from the dough: thick, pale, smooth discs dusted with flour, before baking.
    s = Sprite()
    dough = plain_ramp("#f2e6c4")
    for cx, cy in [(4.8, 4.6), (11.0, 7.0), (6.0, 11.8)]:
        s.ellipse(dough, cx, cy, 3.8, 2.8)
        s.put(int(cx) - 2, int(cy) - 1, dough[HIGHLIGHT])
        s.put(int(cx) + 1, int(cy), plain_ramp("#fbfaf6")[BASE])
    return s


def alfajores_de_maicena_hand():
    # One big alfajor, seen a little from above: a pale biscuit on top, a thick band of dulce de leche showing round the
    # side, its edge rolled in white coconut, and the bottom biscuit beneath.
    s = Sprite()
    biscuit = plain_ramp("#e8cc94")
    caramel = ramp("#b8682a", spread=0.6)
    s.ellipse(biscuit, 7.5, 11.4, 7.0, 2.8)
    s.ellipse(caramel, 7.5, 9.6, 7.0, 2.8)
    s.ellipse(biscuit, 7.5, 7.0, 7.0, 3.6)
    s.ellipse(plain_ramp("#f0dcae"), 7.5, 6.6, 5.2, 2.4, outline=False)  # The flat top, lighter.
    coconut = plain_ramp("#faf8f2")[BASE]
    s.pixels_at(coconut, [(1, 10), (3, 11), (5, 12), (8, 12), (10, 12), (12, 11), (14, 10), (2, 9), (13, 9)])
    s.pixels_at(biscuit[HIGHLIGHT], [(4, 5), (5, 4)])
    s.pixels_at(biscuit[SHADOW], [(7, 7), (10, 6)])  # Fine crumb on the top.
    return s


def sapling(leaf_colour, trunk_colour="#7a5a3a", leaf=(2.4, 1.3), palm=False):
    """A young fruit or spice tree: a thin stem with a few leaves along it and a tuft at the top, kept to the left so the
    picture of what it grows fits in the top right corner. Palms get long arching fronds instead."""
    s = Sprite()
    trunk = ramp(trunk_colour, spread=0.6)
    s.capsule(trunk, [(5.0, 15.0), (5.4, 5.0)], (1.0, 0.7))
    tones = ramp(leaf_colour, spread=0.7)
    if palm:
        for tip in [(0.4, 8.4), (1.0, 3.0), (4.6, 0.4), (7.4, 1.6), (10.6, 10.0)]:
            mid = ((5.4 + tip[0]) / 2, (5.0 + tip[1]) / 2 - 1.4)
            s.capsule(tones, [(5.4, 5.0), mid, tip], (1.2, 0.5))
        s.pixels_at(tones[HIGHLIGHT], [(4, 3), (6, 3)])
        return s
    length, width = leaf
    for cx, cy, angle in [(2.6, 11.0, 30), (8.0, 9.4, -30), (2.4, 6.8, 35), (8.4, 5.4, -35), (5.2, 2.6, -90)]:
        s.ellipse(tones, cx, cy, length, width, angle=angle)
        s.put(round(cx - 0.5), round(cy - 0.5), tones[LIGHT])
    return s


def allspice_sapling_hand():
    # Allspice: glossy, leathery, oval dark-green leaves.
    return sapling("#3a7a34", leaf=(2.6, 1.5))


def almond_sapling_hand():
    # Almond: slender, pointed, bright green leaves on a reddish-grey stem.
    return sapling("#5a9a44", trunk_colour="#7a5a52", leaf=(2.8, 1.0))

def fish(body, back, start, end, depth=2.2, s=None):
    """A small whole fish seen from the side, from its tail at {start} to its head at {end}: a slim body with a darker
    back and pale belly, an eye at the head, and a forked tail."""
    s = s or Sprite()
    belly, dark = plain_ramp(body), ramp(back, spread=0.6)
    length = math.dist(start, end)
    dx, dy = (end[0] - start[0]) / length, (end[1] - start[1]) / length
    mid = (start[0] + dx * length * 0.55, start[1] + dy * length * 0.55)
    s.capsule(belly, [(start[0] + dx * 1.4, start[1] + dy * 1.4), mid, end], (1.1, depth))
    # The back: the half of the body on the upper side of its line, darker.
    for y in range(16):
        for x in range(16):
            side = (x + 0.5 - start[0]) * dy - (y + 0.5 - start[1]) * dx
            if s.pixels[y][x] in belly[1:] and side > 0.2:
                s.put(x, y, dark[BASE])
    for k in (-1, 1):  # The forked tail.
        s.put(round(start[0] - dy * k * 0.9 - 0.5), round(start[1] + dx * k * 0.9 - 0.5), dark[SHADOW])
    s.put(round(end[0] - dx * 1.4 - 0.5), round(end[1] - dy * 1.4 - 0.5), (20, 20, 24))  # The eye.
    return s


def anchovy_hand():
    # Two small anchovies: slim silver fish with blue-green backs.
    s = fish("#cdd4dc", "#2e5a6a", (1.4, 8.6), (13.4, 2.0))
    return fish("#cdd4dc", "#2e5a6a", (2.4, 14.8), (14.6, 8.2), s=s)


def anko_hand():
    # Sweet azuki bean paste: smooth, glossy, a deep reddish-brown, heaped in a bowl.
    s = bowl("#5a2a2a", mound=2.4)
    s.pixels_at(ramp("#5a2a2a")[HIGHLIGHT], [(5, 6), (6, 6)])
    return s


def annatto_sapling_hand():
    # Annatto: broad, heart-shaped leaves on a reddish stem.
    return sapling("#4a8a3a", trunk_colour="#8a4a3a", leaf=(2.6, 1.8))


def anticucho_marinade_hand():
    # A bowl of thin, rusty red marinade of chilies and vinegar, flecked with herbs.
    s = bowl("#a83a1e", flecks=[("#6a7a3a", 0.06), ("#e0a060", 0.05)])
    s.pixels_at(ramp("#a83a1e")[HIGHLIGHT], [(4, 7), (5, 7), (6, 8)])
    return s


def skewer(chunk_colour, chunks=4, char="#3a1e14", s=None, start=(1.6, 14.4), end=(14.6, 1.4)):
    """A wooden skewer from lower left to upper right with chunks of grilled food along it, charred at the edges."""
    s = s or Sprite()
    s.line(ramp("#c8a070")[BASE], round(start[0]), round(start[1]), round(end[0]), round(end[1]))
    tones = ramp(chunk_colour, spread=0.7)
    for i in range(chunks):
        t = 0.22 + i * (0.62 / max(1, chunks - 1))
        cx, cy = start[0] + (end[0] - start[0]) * t, start[1] + (end[1] - start[1]) * t
        s.ellipse(tones, cx, cy, 2.4, 2.4)
        s.pixels_at(ramp(char)[BASE], [(int(cx) + 1, int(cy) + 1), (int(cx) - 1, int(cy) + 1)])
        s.put(int(cx) - 1, int(cy) - 1, tones[HIGHLIGHT])
    return s


def anticucho_skewer_hand():
    # Grilled chunks of beef heart on a skewer, dark red-brown and charred, glazed with the chili marinade.
    return skewer("#a84a2c")


def anticuchos_hand():
    # Anticuchos served: a skewer lying across a plate, with a boiled potato and a piece of corn on the cob.
    s = plate()
    potato = ramp("#d8b46a", spread=0.6)
    s.ellipse(potato, 4.4, 11.8, 2.6, 1.9)
    s.put(3, 11, potato[HIGHLIGHT])
    corn = ramp("#f0c840", spread=0.6)
    s.ellipse(corn, 11.0, 12.0, 3.0, 1.6, texture=lambda x, y: -1 if (x + y) % 2 else 0)
    return skewer("#a84a2c", chunks=3, s=s, start=(0.6, 8.6), end=(15.0, 5.0))


def plate(colour="#e4e6ea"):
    """A plain white plate seen from a little above, filling the lower part of the icon."""
    s = Sprite()
    tones = plain_ramp(colour)
    s.ellipse(tones, 7.5, 11.8, 7.4, 3.2)
    s.ellipse(plain_ramp("#f2f3f5"), 7.5, 11.6, 5.4, 2.0, outline=False)
    return s


def aonori_hand():
    # Aonori: fine, bright green flakes of dried sea lettuce, in a small bowl.
    return spice_bowl("#4a9a3a", fleck="#2a6a2a", seed=5)


def apfelstrudel_hand():
    # A length of apple strudel: a long roll of thin, golden, flaky pastry dusted with sugar, cut at the front to show the
    # apple and raisin filling wound inside.
    s = Sprite()
    pastry = ramp("#d49a4a", spread=0.7)
    s.capsule(pastry, [(13.6, 3.6), (5.0, 10.4)], (3.4, 3.4))
    sugar = plain_ramp("#fafaf6")[BASE]
    s.pixels_at(sugar, [(11, 2), (9, 4), (12, 4), (7, 6), (10, 6), (5, 8)])
    for x, y in [(12, 6), (9, 8)]:
        s.put(x, y, pastry[SHADOW])  # The folds of the pastry.
    # The cut end, toward the lower left: a spiral of filling inside a rim of pastry.
    s.ellipse(pastry, 4.6, 11.0, 3.4, 3.4)
    s.ellipse(ramp("#ecd088", spread=0.5), 4.6, 11.0, 2.4, 2.4, outline=False)
    s.pixels_at(pastry[LIGHT], [(4, 10), (5, 11), (3, 12)])
    s.pixels_at(ramp("#5a2a2a")[BASE], [(5, 10), (3, 11)])
    return s


def apple_compote_hand():
    # A bowl of soft, pale golden apple pieces in their syrup.
    s = bowl("#e8c060", mound=1.4, texture=bumpy)
    s.pixels_at(plain_ramp("#f4e0a0")[BASE], [(5, 6), (8, 6), (10, 7)])
    return s


def apple_pomace_hand():
    # Apple pomace: the pressed-out pulp, a damp, fibrous, tan-brown heap flecked with red peel and seeds.
    return seed_heap("#b08a50", fleck="#a83a2a", seed=17)


def apple_badge():
    """A small red apple with a leaf, for the apple sapling's picture: the apple itself is a vanilla item."""
    s = Sprite()
    apple = ramp("#c8282a", spread=0.7)
    s.ellipse(apple, 7.5, 9.0, 6.0, 5.6)
    s.line(ramp("#5a3a24")[BASE], 7, 4, 8, 1)
    s.ellipse(ramp("#4a9a34"), 10.4, 2.6, 2.2, 1.2, angle=-20)
    s.pixels_at(apple[HIGHLIGHT], [(4, 6), (4, 7), (5, 6)])
    return s


def annatto_badge():
    """A small annatto pod for the annatto sapling's picture: heart-shaped, red, and covered in soft spines."""
    s = Sprite()
    pod = ramp("#c8302a")
    # Heart-shaped: broad at the top, drawn to a point below.
    s.shape(lambda x, y: ((x - 7.5) / 5.6) ** 2 + ((y - 7.0) / 4.4) ** 2 <= 1 or (y >= 7 and abs(x - 7.5) <= (15.0 - y) * 0.7),
            pod, (7.5, 8.6), (5.6, 6.0))
    s.pixels_at(pod[HIGHLIGHT], [(4, 5), (5, 4)])
    spine = ramp("#f08a6a")[BASE]
    s.pixels_at(spine, [(1, 7), (14, 7), (3, 3), (12, 3), (2, 10), (13, 10), (5, 13), (10, 13), (6, 6), (9, 8), (7, 11)])
    s.line(ramp("#5a3a24")[BASE], 7, 3, 8, 0)
    return s


def apple_sapling_hand():
    return sapling("#4e9440", leaf=(2.4, 1.6))


def apricot_sapling_hand():
    # Apricot: rounded, toothed leaves on a reddish stem.
    return sapling("#58983e", trunk_colour="#7a4a3a", leaf=(2.2, 1.8))


def arepa_dough_hand():
    # Arepa dough: a smooth, pale yellow-white ball of precooked maize flour.
    return dough_ball("#f0e2a8", flour=False)


def arepa(s=None, cx=7.5, cy=8.6):
    """A round, thick maize cake griddled golden, with darker scorched patches."""
    s = s or Sprite()
    tones = plain_ramp("#eed490")
    s.ellipse(tones, cx, cy + 1.6, 6.6, 3.0)
    s.ellipse(tones, cx, cy, 6.6, 3.0)
    s.pixels_at(ramp("#b0782a")[BASE], [(int(cx) - 3, int(cy)), (int(cx) + 2, int(cy) - 1), (int(cx), int(cy) + 1), (int(cx) + 4, int(cy) + 1)])
    s.put(int(cx) - 2, int(cy) - 2, tones[HIGHLIGHT])
    return s


def arepas_hand():
    s = arepa(cx=7.5, cy=10.2)
    return arepa(s, cx=7.5, cy=5.4)


def arepas_con_queso_hand():
    # An arepa split open and stuffed with soft white cheese, a pat of butter melting on top.
    s = Sprite()
    tones = plain_ramp("#eed490")
    s.ellipse(tones, 7.5, 11.6, 6.6, 2.6)
    cheese = plain_ramp("#f6f4ea")
    s.ellipse(cheese, 7.5, 9.0, 6.0, 1.8)
    s.ellipse(tones, 7.0, 6.2, 6.4, 2.8, angle=-8)
    s.pixels_at(ramp("#b0782a")[BASE], [(4, 6), (9, 5), (6, 12), (10, 12)])
    s.ellipse(ramp("#f4d860", spread=0.5), 8.6, 4.4, 1.6, 0.9)
    return s

def arroz_con_leche_hand():
    # A bowl of creamy rice pudding dusted with cinnamon, with a few raisins.
    s = bowl("#f2ead2", mound=1.2, texture=bumpy, plain=True, flecks=[("#a8642a", 0.12)], bowl_colour=CLAY)
    s.pixels_at(ramp("#4a2228")[BASE], [(5, 7), (9, 6), (11, 8)])
    return s


def azuki_mash_hand():
    # Mashed azuki beans: a coarse, dull red-brown mash with bits of skin, heaped in a bowl.
    s = bowl("#7a3a32", mound=2.4, texture=bumpy, flecks=[("#4a1e1e", 0.1)])
    return s


def slab(lean, fat=None, marbling=0.0, rind=None, cx=7.5, cy=8.5, rx=6.6, ry=4.6, angle=-12, fat_side=1.4, seed=13, s=None):
    """A raw cut of meat seen from above: a rounded slab of lean with a band of fat along one edge, optional streaks of
    marbling, and an optional rind outside the fat."""
    s = s or Sprite()
    meat = ramp(lean, spread=0.65)
    s.ellipse(meat, cx, cy, rx, ry, angle=angle)
    cos, sin = math.cos(math.radians(angle)), math.sin(math.radians(angle))
    rng = random.Random(seed)
    for y in range(16):
        for x in range(16):
            if s.pixels[y][x] in (None, meat[OUTLINE]):
                continue
            u, v = (x + 0.5 - cx) * cos + (y + 0.5 - cy) * sin, -(x + 0.5 - cx) * sin + (y + 0.5 - cy) * cos
            if fat and v > ry - fat_side - 1.2:
                s.put(x, y, (plain_ramp(rind) if rind and v > ry - 1.6 else plain_ramp(fat))[BASE if v < ry - 1.4 else SHADOW])
            elif marbling and rng.random() < marbling:
                s.put(x, y, plain_ramp("#e6a49c")[BASE])  # Marbling: soft pink streaks, not white glints.
    return s


def back_fat_hand():
    # A thick slab of pork back fat: firm, creamy white, with the pale skin still on one edge.
    s = Sprite()
    fat = plain_ramp("#f2ece2")
    s.shape(lambda x, y: 1 <= x <= 14 and 4 <= y <= 12 + (x % 3 == 0), fat, (7.5, 8.0), (7.0, 4.0))
    s.pixels_at(plain_ramp("#e6c8a8")[BASE], [(x, 12) for x in range(2, 14)])  # The rind.
    s.pixels_at(fat[HIGHLIGHT], [(3, 5), (4, 5), (5, 6)])
    s.pixels_at(plain_ramp("#e8b8b0")[BASE], [(9, 7), (10, 8)])  # A thread of pink meat.
    return s


def bacon_hand():
    return bacon_rashers("#c85a52", "#f2e4d8")


def crisp_bacon_hand():
    # Crisp bacon: the rashers fried until the meat is deep reddish brown and the fat golden.
    return bacon_rashers("#8a3a24", "#e0b060")


def bacon_rashers(lean, fat_colour):
    # Three rashers of smoked streaky bacon, gently rippled: layers of deep pink meat and white fat, browned at the ends.
    s = Sprite()
    meat, fat = ramp(lean, spread=0.6), plain_ramp(fat_colour)
    for i, y0 in enumerate((2.0, 7.0, 12.0)):
        for x in range(1, 15):
            top = round(y0 + math.sin(x * 0.55 + i * 1.3) * 0.6)
            column = [meat[LIGHT], fat[BASE], meat[BASE], meat[SHADOW]]
            for k, colour in enumerate(column):
                s.put(x, top + k, colour)
            if x in (1, 14):
                s.pixels_at(ramp("#7a3a2a")[BASE], [(x, top + k) for k in range(4)])
    return s


def bienenstich_slice(cream):
    """A slice of bee sting cake seen from the side: a golden yeast-cake base and a top of sticky, caramelised flaked
    almonds, with a thick layer of vanilla cream between them once it is filled."""
    s = Sprite()
    cake, filling, top = plain_ramp("#e2b868"), plain_ramp("#f6eccc"), ramp("#c88a32", spread=0.6)
    bottom = 3 if cream else 6
    s.shape(lambda x, y: 1 <= x <= 14 and bottom <= y <= 14, cake, (7.5, 8.5), (7.0, 6.0))
    for y in range(bottom + 1, 14):
        for x in range(2, 14):
            s.put(x, y, top[BASE] if y <= bottom + 3 else filling[BASE] if cream and y <= 10 else cake[BASE])
    s.pixels_at(plain_ramp("#f0d8a0")[BASE], [(3, bottom + 1), (6, bottom + 1), (9, bottom + 2), (12, bottom + 1), (4, bottom + 2), (11, bottom + 3)])
    if cream:
        s.pixels_at(filling[SHADOW], [(x, 10) for x in range(2, 14)])
    s.pixels_at(cake[SHADOW], [(4, 13), (9, 12), (12, 13)])
    return s


def baked_bienenstich_hand():
    # The baked cake before filling: yeast cake under its caramelised almond top.
    return bienenstich_slice(cream=False)


def bienenstich_hand():
    # Filled with vanilla cream.
    return bienenstich_slice(cream=True)


def baked_creme_brulee_hand():
    # Baked custard in its white ramekin, before the sugar is caramelised: smooth and pale yellow.
    return ramekin(caramel=False)


def creme_brulee_hand():
    # Crème brûlée: the custard under a crackled, glassy caramel top.
    return ramekin(caramel=True)


def ramekin(caramel):
    s = Sprite()
    dish = plain_ramp("#eceef2")
    s.shape(lambda x, y: 1 <= x <= 14 and 7 <= y <= 14, dish, (7.5, 10.5), (7.0, 4.0))
    s.pixels_at(dish[SHADOW], [(x, 13) for x in range(2, 14)])
    for y in (9, 11):
        s.pixels_at(dish[LIGHT], [(x, y) for x in range(2, 14, 2)])  # The fluted side.
    top = ramp("#c07a2a", spread=0.6) if caramel else plain_ramp("#f2dc90")
    s.ellipse(top, 7.5, 6.6, 6.4, 2.4)
    s.pixels_at(top[HIGHLIGHT], [(4, 6), (5, 5), (9, 6)])
    if caramel:
        s.pixels_at(top[SHADOW], [(7, 6), (8, 7), (11, 6)])  # Cracks.
    return s


def baked_kammerjunkere_hand():
    # Kammerjunkere: small, crisp, twice-baked Danish biscuits, golden and split, with a rough sugary crumb.
    s = Sprite()
    biscuit = plain_ramp("#dca860")
    for cx, cy, angle in [(4.6, 4.6, -15), (11.0, 5.8, 20), (5.4, 11.0, 10), (11.4, 11.6, -20)]:
        s.ellipse(biscuit, cx, cy, 3.4, 2.4, angle=angle)
        s.ellipse(plain_ramp("#f0d8a0"), cx, cy - 0.3, 2.2, 1.3, angle=angle, outline=False)  # The split, crumbly face.
        s.put(int(cx), int(cy), biscuit[SHADOW])
    return s


def bay_laurel_sapling_hand():
    # Bay laurel: narrow, pointed, glossy dark leaves.
    return sapling("#3a7a3a", trunk_colour="#6a5a3a", leaf=(2.8, 1.1))


def bay_leaf_hand():
    # Two dried bay leaves: long, pointed, a dull olive-grey green, with a paler middle vein.
    s = Sprite()
    for (cx, cy, angle), colour in (((6.4, 7.0, -50), "#6e7e48"), ((9.8, 9.0, -35), "#7e8c56")):
        tones = ramp(colour, spread=0.6)
        s.ellipse(tones, cx, cy, 6.0, 2.2, angle=angle)  # noqa
        cos, sin = math.cos(math.radians(angle)), math.sin(math.radians(angle))
        for t in range(-4, 5):
            s.put(round(cx + cos * t - 0.5), round(cy + sin * t - 0.5), tones[LIGHT])
    return s


def bean_sprouts_hand():
    # A few bean sprouts: crisp, creamy-white curved stems, each with a small yellow bean head and a wispy root tail.
    s = Sprite()
    stem, head = plain_ramp("#f2eed4"), ramp("#d8cc50", spread=0.6)
    for points in [[(2.0, 14.0), (3.4, 8.0), (2.6, 3.0)], [(6.0, 15.0), (6.6, 9.0), (8.4, 3.6)], [(10.4, 15.0), (11.0, 10.0), (13.4, 5.6)],
                   [(14.6, 14.6), (9.0, 12.6), (5.0, 12.0)]]:
        s.capsule(stem, points, (0.8, 1.1))
        x, y = points[-1]
        s.ellipse(head, x, y, 1.5, 1.3)
        s.put(round(points[0][0] - 0.5), round(points[0][1] - 0.5), plain_ramp("#c8c0a0")[BASE])  # The root tail.
    return s


def bechamel_hand():
    # Béchamel: a bowl of smooth, glossy white sauce with a grating of nutmeg.
    s = bowl("#f4f0e4", plain=True, flecks=[("#a87a4a", 0.05)], bowl_colour=CLAY)
    s.pixels_at(plain_ramp("#ffffff")[BASE], [(4, 7), (5, 7)])
    return s


def bones(colour, layout, s=None):
    """Bones with knobbed ends. Each is (start, end, radius): a shaft between two rounded knuckles."""
    s = s or Sprite()
    tones = plain_ramp(colour)
    for start, end, r in layout:
        s.capsule(tones, [start, end], (r * 0.55, r * 0.55))
        for (x, y), dx, dy in [(start, -1, 1), (end, 1, -1)]:
            length = math.dist(start, end)
            nx, ny = -(end[1] - start[1]) / length, (end[0] - start[0]) / length
            for k in (-1, 1):
                s.ellipse(tones, x + nx * k * r * 0.5, y + ny * k * r * 0.5, r * 0.62, r * 0.62)
        s.put(round((start[0] + end[0]) / 2 - 0.5), round((start[1] + end[1]) / 2 - 1.0), tones[HIGHLIGHT])
    return s


def beef_bones_hand():
    # A big beef bone, creamy white, knobbed at both ends, with a scrap of red meat still on it.
    s = Sprite()
    bone = plain_ramp("#ece2cc")
    s.capsule(bone, [(3.6, 12.4), (12.4, 3.6)], (1.5, 1.5))
    for x, y in [(2.0, 11.4), (4.6, 14.0), (11.4, 2.0), (14.0, 4.6)]:
        s.ellipse(bone, x, y, 2.0, 2.0)
    s.pixels_at(bone[HIGHLIGHT], [(7, 7), (8, 6), (11, 2)])
    s.pixels_at(ramp("#a83a32")[BASE], [(8, 9), (9, 8), (9, 9)])
    return s


def beef_brisket_hand():
    # Beef brisket: a long, flat, dark red slab with a thick cap of white fat along one side, coarse grain across it.
    s = slab("#a83430", fat="#f0e6d6", rx=7.0, ry=4.4, angle=-10, fat_side=1.8)
    for x in range(3, 13, 3):
        s.line(ramp("#a83430")[SHADOW], x, 6, x + 1, 9)
    return s


def beef_chuck_hand():
    # Beef chuck: a thick, deep red roasting joint, well marbled with fat.
    return slab("#a83430", fat="#f0e6d6", marbling=0.12, rx=6.0, ry=5.4, angle=0, fat_side=1.0, seed=21)


def heart(colour, cx=7.5, cy=8.4, size=1.0, fat=None, s=None):
    """A whole heart: a rounded, pointed shape, darker toward the tip, with vessels at the top and an optional cap of fat."""
    s = s or Sprite()
    tones = ramp(colour, spread=0.6)
    s.shape(lambda x, y: ((x - cx) / (5.2 * size)) ** 2 + ((y - cy + 1.4 * size) / (3.8 * size)) ** 2 <= 1 or
            (y >= cy - 1.4 * size and abs(x - cx) <= (cy + 5.6 * size - y) * 0.8), tones, (cx, cy), (5.2 * size, 5.6 * size))
    s.pixels_at(tones[HIGHLIGHT], [(round(cx - 3 * size), round(cy - 3 * size)), (round(cx - 2 * size), round(cy - 3.6 * size))])
    vessel = ramp("#c8706a", spread=0.6)
    s.pixels_at(vessel[BASE], [(round(cx - 1), round(cy - 5.4 * size)), (round(cx - 1), round(cy - 6.2 * size)),
                               (round(cx + 1), round(cy - 5.4 * size)), (round(cx + 2), round(cy - 6.0 * size))])
    if fat:
        s.pixels_at(plain_ramp(fat)[BASE], [(round(cx - 3 * size), round(cy - 1.6 * size)), (round(cx + 2 * size), round(cy - 2.2 * size)),
                                            (round(cx + 3 * size), round(cy - 1.4 * size))])
    return s


def beef_heart_hand():
    # A whole beef heart: big, dark red, with a cap of white fat and the vessels at the top.
    return heart("#8a2a2a", fat="#f0e6d6")

def kidney(colour, cx, cy, size, s):
    """A kidney made of many small rounded lobes clustered together."""
    tones = ramp(colour, spread=0.6)
    for dx, dy in [(-2, -1), (0, -2), (2, -1), (-2, 1), (0, 0), (2, 1), (-1, 2), (1, 2)]:
        s.ellipse(tones, cx + dx * size, cy + dy * size, 1.6 * size, 1.4 * size)
    for dx, dy in [(-2, -1), (0, -2), (2, -1), (0, 0)]:
        s.put(round(cx + dx * size - 0.8), round(cy + dy * size - 0.8), tones[HIGHLIGHT])
    return s


def beef_kidneys_hand():
    # Two beef kidneys: dark red-brown, made of many small rounded lobes.
    s = kidney("#8a3a30", 5.0, 5.4, 1.0, Sprite())
    return kidney("#8a3a30", 10.6, 10.6, 1.0, s)


def liver(colour, cx=7.5, cy=8.0, rx=6.8, ry=4.6, angle=-15, s=None):
    """A smooth, glossy lobe of liver: rounded, with one lobe overlapping another and a bright sheen."""
    s = s or Sprite()
    tones = ramp(colour, spread=0.6)
    s.ellipse(tones, cx - rx * 0.25, cy + ry * 0.15, rx * 0.8, ry * 0.85, angle=angle)
    s.ellipse(tones, cx + rx * 0.2, cy - ry * 0.1, rx * 0.75, ry * 0.8, angle=angle)
    s.pixels_at(tones[HIGHLIGHT], [(round(cx - 1), round(cy - ry * 0.5)), (round(cx), round(cy - ry * 0.5)), (round(cx - 3), round(cy))])
    s.put(round(cx - 1), round(cy + 1), tones[SHADOW])
    return s


def beef_liver_hand():
    return liver("#6a2a26")


def beef_rib_roast_hand():
    # A standing rib roast: a thick, red, marbled joint with a cap of fat, and the ends of the white rib bones standing
    # up out of its top.
    s = Sprite()
    bone = plain_ramp("#ece2cc")
    for x in (4.0, 7.6, 11.2):
        s.capsule(bone, [(x, 9.0), (x + 0.6, 1.6)], (1.1, 1.1))
    return slab("#b03a34", fat="#f0e6d6", marbling=0.08, cx=7.5, cy=10.4, rx=6.6, ry=4.4, angle=0, fat_side=1.4, seed=31, s=s)


def beef_round_hand():
    # Beef round: a lean, deep red roasting joint with very little fat.
    return slab("#a83430", fat="#f0e6d6", rx=6.2, ry=5.2, angle=10, fat_side=0.6, seed=41)


def beef_shank_hand():
    # A cross-cut slice of beef shank: dark red meat round a white bone with pink marrow in its middle.
    s = slab("#a03030", fat="#f0e6d6", rx=6.6, ry=5.6, angle=0, fat_side=0.6, marbling=0.06, seed=9)
    bone = plain_ramp("#ece2cc")
    s.ellipse(bone, 7.5, 7.6, 2.4, 2.2)
    s.ellipse(ramp("#d07a6a", spread=0.5), 7.5, 7.6, 1.2, 1.1, outline=False)
    return s


def beef_sirloin_hand():
    # A sirloin steak: deep red, finely marbled, with a strip of white fat down one edge.
    return slab("#b03a34", fat="#f0e6d6", marbling=0.06, rx=6.8, ry=4.6, angle=-15, fat_side=1.6, seed=55)


def beef_strips_hand():
    # Thin strips of sliced beef, lean and red, lying at angles.
    return strips("#c24a40")


def cured_beef_strips_hand():
    # Cured beef strips: darkened to a deep, dense red by the salt, with a few white grains still on them.
    s = strips("#8a2a2a")
    s.pixels_at(plain_ramp("#f2f0ea")[BASE], [(5, 2), (8, 6), (11, 6), (4, 12), (12, 12)])
    return s


def strips(colour):
    s = Sprite()
    meat = ramp(colour, spread=0.55)
    for start, end in [((1.6, 3.4), (10.0, 1.6)), ((2.4, 8.4), (14.0, 4.8)), ((1.6, 13.6), (11.0, 10.4)), ((8.0, 14.6), (14.6, 11.4))]:
        s.capsule(meat, [start, end], (1.5, 1.3))
        s.put(round((start[0] + end[0]) / 2 - 0.5), round((start[1] + end[1]) / 2 - 1.0), meat[HIGHLIGHT])
    return s


def beef_tongue_hand():
    # A whole beef tongue: long, thick, a dull greyish pink, rough and bumpy on top, narrowing to a rounded tip.
    s = Sprite()
    tongue = ramp("#b88078", spread=0.6)
    s.capsule(tongue, [(2.4, 12.6), (8.0, 8.0), (13.4, 3.0)], (3.2, 1.8),
              texture=lambda x, y: -1 if (x * 2 + y) % 4 == 0 else 1 if (x + y * 3) % 5 == 0 else 0)
    s.pixels_at(ramp("#8a4a44")[BASE], [(1, 14), (2, 14), (1, 13)])  # The cut root.
    return s


def berliner_hand():
    # A Berliner: a round, golden-brown doughnut with a pale band round its middle, dusted with sugar, red jam peeping out.
    return doughnut(filled=True)


def fried_doughnut_hand():
    # A fried doughnut: the plain golden ball with its pale band, before it is filled and sugared.
    return doughnut(filled=False)


def doughnut(filled):
    s = Sprite()
    dough = ramp("#d0903a", spread=0.7)
    s.ellipse(dough, 7.5, 8.4, 6.6, 5.6)
    s.pixels_at(plain_ramp("#f0d8a0")[BASE], [(x, 10) for x in range(2, 14)])  # The pale band where it floated.
    if not filled:
        s.pixels_at(dough[HIGHLIGHT], [(4, 5), (5, 4)])
        return s
    sugar = plain_ramp("#fafaf6")[BASE]
    s.pixels_at(sugar, [(5, 4), (7, 4), (9, 5), (6, 6), (10, 4), (8, 6), (4, 6)])
    jam = ramp("#c02a3a", spread=0.6)
    s.ellipse(jam, 13.0, 9.6, 1.6, 1.4)
    return s


def black_bean_mash_hand():
    # Refried black beans: a dark, glossy, purple-black mash heaped in a bowl, with bits of onion.
    return bowl("#2e2632", mound=2.4, texture=bumpy, flecks=[("#e8e0d0", 0.05)])


def black_pepper_hand():
    # Ground black pepper: a coarse grey-black powder.
    return spice_bowl("#4a4644", fleck="#8a8680", seed=19)


def black_tea_hand():
    # Black tea: a heap of tightly twisted, dry, near-black leaves with a few golden tips.
    return seed_heap("#3a2a22", fleck="#b88a40", seed=23)


def blanched_almonds_hand():
    # Blanched almonds: skinned, smooth and creamy white.
    s = Sprite()
    shell = plain_ramp("#f0e4c8")
    stamp(s, ALMOND, shell, 1, 1)
    stamp(s, ALMOND, shell, 9, 2, mirror=True)
    stamp(s, ALMOND, shell, 5, 6)
    return s


def blanched_vanilla_pods_hand():
    # Fresh vanilla pods scalded to stop them growing: no longer green, a soft olive brown, not yet cured dark.
    return vanilla_pods("#7a6a32", shine=False)

def tart_shell(pastry="#e0a85a", filling=None, s=None):
    """A round tart seen a little from above: a golden pastry shell with a fluted rim, empty or holding a filling."""
    s = s or Sprite()
    crust = ramp(pastry, spread=0.6)
    s.ellipse(crust, 7.5, 9.6, 7.2, 4.8)
    inside = plain_ramp(filling) if filling else ramp(pastry, spread=0.4)
    s.ellipse(inside, 7.5, 9.0, 5.4, 3.0, outline=False, light=(0.6, 0.8) if not filling else (-0.6, -0.8))
    for x in range(1, 15, 2):  # The fluted rim.
        top = round(9.6 - 4.8 * math.sqrt(max(0.0, 1 - ((x + 0.5 - 7.5) / 7.2) ** 2)))
        s.put(x, top + 1, crust[HIGHLIGHT])
    return s


def blind_baked_shell_hand():
    # A blind-baked tart shell: empty, crisp, and golden, its base pricked with a fork.
    s = tart_shell()
    s.pixels_at(ramp("#e0a85a", spread=0.4)[SHADOW], [(5, 9), (8, 8), (10, 10), (7, 11)])
    return s


def sausage(colour, start, end, radius=1.8, s=None, cut=None):
    """A plump sausage link from {start} to {end}, tied at both ends, with a glossy skin and an optional cut face."""
    s = s or Sprite()
    tones = ramp(colour, spread=0.6)
    s.capsule(tones, [start, end], (radius, radius))
    length = math.dist(start, end)
    dx, dy = (end[0] - start[0]) / length, (end[1] - start[1]) / length
    for t in range(2, int(length) - 1, 3):
        s.put(round(start[0] + dx * t - dy * radius * 0.5 - 0.5), round(start[1] + dy * t + dx * radius * 0.5 - 1.0), tones[HIGHLIGHT])
    for x, y in (start, end):
        s.put(round(x - dx * (radius + 0.4) - 0.5) if (x, y) == start else round(x + dx * (radius + 0.4) - 0.5),
              round(y - dy * (radius + 0.4) - 0.5) if (x, y) == start else round(y + dy * (radius + 0.4) - 0.5), plain_ramp("#d8c8a8")[SHADOW])
    return s


def blood_sausage_hand():
    # Blood sausage: a plump, near-black link, and a slice cut from it showing white flecks of fat and barley.
    s = sausage("#3a1a1e", (2.4, 11.0), (11.6, 2.4), radius=2.2)
    slice_ = ramp("#3a1a1e", spread=0.5)
    s.ellipse(slice_, 11.4, 11.6, 3.0, 3.0)
    s.ellipse(ramp("#5a2028", spread=0.4), 11.4, 11.6, 2.0, 2.0, outline=False)
    s.pixels_at(plain_ramp("#eee6d4")[BASE], [(10, 11), (12, 12), (11, 10), (12, 10)])
    return s


def blood_sausage_filling_hand():
    # The raw filling: dark red blood mixed with white cubes of back fat and pale barley, in a bowl.
    return bowl("#5a1a20", mound=1.4, flecks=[("#f2ece2", 0.14), ("#d8c890", 0.08)])


def blue_corn_masa_hand():
    # Blue corn masa: a soft ball of dough, a dusky blue-grey.
    return dough_ball("#8a92b0", flour=False)


def blue_hominy_hand():
    # Blue hominy: a bowl of big, puffed, pale grey-blue kernels.
    s = bowl("#9aa0bc", mound=1.6, texture=bumpy)
    s.pixels_at(plain_ramp("#c4c8dc")[BASE], [(4, 6), (7, 6), (10, 6), (6, 7), (9, 7)])
    return s


def boeuf_bourguignon_hand():
    # Boeuf bourguignon: a deep, wine-dark stew of beef chunks with carrots, mushrooms and little onions, in a bowl.
    return bowl("#5a2a24", mound=0.8, flecks=[("#7a3a2a", 0.18), ("#e07a2a", 0.08), ("#c8b48a", 0.06), ("#f0e8dc", 0.04)])


def boiled_beetroot_hand():
    # Cooked beetroot: one whole, smooth, deep purple-red root and a half showing its darker rings.
    s = Sprite()
    beet = ramp("#7a1a3a", spread=0.6)
    s.ellipse(beet, 5.6, 7.0, 4.6, 4.4)
    s.line(ramp("#5a3a2a")[BASE], 5, 11, 4, 14)  # The root tail.
    s.pixels_at(beet[HIGHLIGHT], [(3, 5), (4, 4)])
    s.ellipse(beet, 11.0, 11.2, 3.8, 3.6)
    s.ellipse(ramp("#a02a50", spread=0.5), 11.0, 11.2, 2.6, 2.4, outline=False)
    s.ellipse(ramp("#6a1430", spread=0.5), 11.0, 11.2, 1.2, 1.1, outline=False)
    return s


def bua_loy_hand():
    # Bua loy: little pumpkin rice-flour balls floating in a bowl of warm, sweetened coconut milk.
    s = bowl("#f6f2e6", plain=True, bowl_colour=CLAY)
    for (x, y), colour in [((4, 7), "#f0a040"), ((7, 7), "#f6c860"), ((10, 7), "#f0a040"), ((6, 8), "#f6c860"), ((9, 8), "#f0a040")]:
        s.pixels_at(plain_ramp(colour)[BASE], [(x, y), (x + 1, y)])
    return s


def balls(colour, spots, radius=2.4, s=None, plain=True):
    """Round balls, such as dumplings or rice balls, each with a highlight."""
    s = s or Sprite()
    tones = plain_ramp(colour) if plain else ramp(colour, spread=0.6)
    for cx, cy in spots:
        s.ellipse(tones, cx, cy, radius, radius)
        s.put(round(cx - radius * 0.5 - 0.5), round(cy - radius * 0.5 - 0.5), tones[HIGHLIGHT])
    return s


def boiled_dango_hand():
    # Boiled dango: soft, smooth, glossy white rice-flour dumplings, before they are skewered.
    return balls("#f2f0ea", [(4.6, 10.6), (10.8, 10.6), (7.6, 5.4)], radius=3.4)


def boiled_oil_palm_fruit_hand():
    # Boiled oil palm fruit: softened, glossy, a deep orange-red, oily at the skin.
    s = balls("#d84a1e", [(4.4, 10.8), (11.0, 10.8), (7.6, 5.0)], radius=3.3, plain=False)
    return s


def boiled_picarones_vegetables_hand():
    # Boiled sweet potato and pumpkin for picarones: soft orange chunks in a bowl.
    s = bowl("#e88a2a", mound=1.8, texture=bumpy, flecks=[("#f0b040", 0.12)])
    return s


def boiled_potatoes_hand():
    # Boiled potatoes: peeled, pale yellow, smooth and glossy.
    return balls("#ecd890", [(4.4, 10.8), (11.0, 10.6), (7.6, 5.0)], radius=3.4)


def prawn(colour):
    """A cooked prawn curled into a C, head at the top: a thick segmented body tapering to a fan tail, with pale bands
    between the shell segments and a few legs inside the curl."""
    s = Sprite()
    tones = ramp(colour, spread=0.6)
    arc = [(8.0 + 5.0 * math.cos(math.radians(a)), 8.0 + 5.0 * math.sin(math.radians(a))) for a in range(-150, 100, 25)]
    s.capsule(tones, arc, (2.6, 1.0))
    band = plain_ramp("#f8e0d0")[BASE]
    for x, y in arc[2:-2:2]:
        s.put(round(x - 0.5), round(y - 0.5), band)
    tx, ty = arc[-1]
    s.pixels_at(tones[SHADOW], [(round(tx) - 2, round(ty)), (round(tx) - 1, round(ty) + 1), (round(tx), round(ty))])  # The tail fan.
    hx, hy = arc[0]
    s.put(round(hx - 0.5), round(hy - 0.5), (30, 20, 20))  # The eye.
    s.line(tones[OUTLINE], round(hx), round(hy) - 1, round(hx) + 5, round(hy) - 3)  # A feeler.
    for x, y in [(7, 7), (8, 9), (7, 11)]:
        s.put(x, y, tones[LIGHT])  # Legs.
    return s


def boiled_prawns_hand():
    # A boiled prawn: curled, pink-orange, with pale bands between the shell segments.
    return prawn("#f0784a")


def noodles(colour, cx=7.5, cy=9.0, rx=6.8, ry=4.6, s=None):
    """A loose nest of wavy noodles: rows of strands with dark gaps between them, inside a rounded heap."""
    s = s or Sprite()
    tones = plain_ramp(colour)
    s.ellipse(tones, cx, cy, rx, ry)
    for y in range(16):
        for x in range(16):
            if s.pixels[y][x] in (None, tones[OUTLINE]):
                continue
            wave = (y + round(math.sin(x * 0.9) * 1.0)) % 3
            s.put(x, y, tones[SHADOW] if wave == 0 else tones[HIGHLIGHT] if wave == 1 and x % 4 == 1 else tones[BASE])
    return s


def boiled_ramen_noodles_hand():
    # Boiled ramen noodles: a nest of wavy, springy, pale yellow strands.
    return noodles("#f0d888")


def boiled_sweet_potato_hand():
    # Boiled sweet potato: thick rounds with purple-red skin and soft, bright orange flesh.
    s = Sprite()
    skin, flesh = ramp("#8a3a4a", spread=0.6), ramp("#f08a30", spread=0.5)
    for cx, cy in [(5.0, 5.4), (10.6, 7.6), (6.0, 11.6)]:
        s.ellipse(skin, cx, cy, 3.6, 3.0)
        s.ellipse(flesh, cx, cy - 0.3, 2.6, 2.0, outline=False)
        s.put(int(cx) - 1, int(cy) - 1, flesh[HIGHLIGHT])
    return s

def boiled_tub_tim_hand():
    # Tub tim krob: little ruby-red jewels of water chestnut in white coconut milk, in a bowl.
    s = bowl("#f6f2e6", plain=True, bowl_colour=CLAY)
    ruby = ramp("#d0283a", spread=0.6)
    for x, y in [(4, 7), (7, 7), (10, 7), (5, 8), (9, 8), (12, 8)]:
        s.put(x, y, ruby[BASE])
    s.pixels_at(ruby[HIGHLIGHT], [(7, 7), (10, 7)])
    return s


def curry_with_meatballs(s, cx=7.5, cy=8.0):
    """Yellow curry sauce with three small pork meatballs sitting in it."""
    meat = ramp("#c8a088", spread=0.6)
    for x, y in [(cx - 3, cy), (cx + 1, cy - 1), (cx + 3.4, cy + 0.6)]:
        s.ellipse(meat, x, y, 1.4, 1.2)
        s.put(round(x - 1), round(y - 1), meat[HIGHLIGHT])
    return s


def boller_i_karry_stew_hand():
    # Boller i karry: small pork meatballs in a thick, bright yellow curry sauce, in a bowl.
    return curry_with_meatballs(bowl("#e8c030", flecks=[("#f0d870", 0.08)]))


def boller_i_karry_hand():
    # Served: the curry with its meatballs beside a heap of white rice, on a plate.
    s = plate()
    rice = plain_ramp("#f6f4ec")
    s.ellipse(rice, 4.6, 10.0, 3.6, 2.4, texture=lambda x, y: -1 if (x * 3 + y) % 5 == 0 else 0)
    curry = ramp("#e8c030", spread=0.6)
    s.ellipse(curry, 10.4, 11.0, 4.0, 2.2)
    meat = ramp("#c8a088", spread=0.6)
    for x, y in [(9.0, 10.0), (11.8, 10.4), (10.4, 12.0)]:
        s.ellipse(meat, x, y, 1.3, 1.1)
    s.pixels_at(ramp("#4a9a34")[BASE], [(4, 8), (5, 9)])  # A sprig of parsley.
    return s


def bonito_flakes_hand():
    # Katsuobushi shavings: a heap of tissue-thin, curled, pinkish-tan flakes.
    s = Sprite()
    flake = plain_ramp("#d8a88a")
    for cx, cy, angle in [(4.4, 5.0, -20), (10.4, 4.6, 25), (3.6, 10.4, 40), (8.4, 8.6, -10), (12.4, 10.6, -40), (7.2, 13.0, 15)]:
        s.ellipse(flake, cx, cy, 3.4, 2.0, angle=angle)
        cos, sin = math.cos(math.radians(angle)), math.sin(math.radians(angle))
        for t in (-1, 0, 1):
            s.put(round(cx + cos * t - 0.5), round(cy + sin * t + 0.3), flake[SHADOW])  # The curl.
        s.put(round(cx - cos * 1.8 - 0.5), round(cy - sin * 1.8 - 1.0), flake[HIGHLIGHT])
    return s


def bouquet_garni_hand():
    # A bouquet garni: sprigs of thyme and parsley wrapped in a bay leaf, tied in a bundle with white string.
    s = Sprite()
    bay = ramp("#4a7a3a", spread=0.6)
    s.ellipse(bay, 7.0, 9.0, 6.4, 2.6, angle=-50)
    thyme = ramp("#6a9a48")
    for x0, y0, x1, y1 in [(6, 9, 11, 1), (7, 9, 13, 3), (6, 10, 9, 0)]:
        for x, y in s.line(thyme[SHADOW], x0, y0, x1, y1)[2::2]:
            s.put(x + 1, y, thyme[BASE])
    s.pixels_at(ramp("#4aa83a")[LIGHT], [(12, 1), (13, 2), (10, 0), (11, 0)])  # Parsley leaves at the top.
    string = plain_ramp("#f4f2ea")[BASE]
    s.pixels_at(string, [(5, 9), (6, 8), (7, 9), (6, 10), (8, 8), (4, 10)])
    s.pixels_at(string, [(3, 12), (2, 13)])  # The loose end.
    return s


def bratwurst_filling_hand():
    # Bratwurst filling: finely minced, pale pink pork flecked with pepper and marjoram, in a bowl.
    return bowl("#e0a8a0", mound=1.8, texture=bumpy, flecks=[("#3a3a3a", 0.06), ("#6a8a4a", 0.06)])


def bratwurst_mit_sauerkraut_hand():
    # A browned bratwurst on a heap of pale sauerkraut, with a dab of mustard and a bread roll, on a plate.
    s = plate()
    kraut = plain_ramp("#e8e0a8")
    s.ellipse(kraut, 6.0, 10.8, 4.6, 2.2, texture=lambda x, y: -1 if (x + y * 2) % 3 == 0 else 0)
    roll = ramp("#c88a3a", spread=0.6)
    s.ellipse(roll, 12.0, 9.6, 2.8, 2.2)
    s.put(12, 9, plain_ramp("#f0d8a0")[BASE])
    sausage("#a8642e", (2.0, 9.6), (10.0, 6.4), radius=1.6, s=s)
    s.ellipse(ramp("#e0b030", spread=0.5), 9.0, 12.4, 1.4, 0.9)
    return s


def bread_dough_hand():
    # Bread dough: a smooth, risen, pale ball dusted with flour.
    return dough_ball("#ecdcb8")


def bread_roll_hand():
    # A bread roll: round, golden-brown, with a pale score across the top and a glossy crust.
    s = Sprite()
    crust = ramp("#c8822e", spread=0.7)
    s.ellipse(crust, 7.5, 9.0, 6.4, 5.0)
    s.line(plain_ramp("#f0d8a0")[BASE], 4, 9, 11, 6)
    s.pixels_at(crust[HIGHLIGHT], [(4, 6), (5, 5), (6, 5)])
    return s


def breadcrumbs_hand():
    # Breadcrumbs: dry, golden crumbs, in a small bowl.
    return spice_bowl("#d8aa5a", fleck="#a8742a", seed=27)


def brigadeiro_mixture_hand():
    # Brigadeiro mixture: thick, glossy chocolate fudge, cooked down in a bowl.
    s = bowl("#4a2a1e", mound=1.4)
    s.pixels_at(ramp("#4a2a1e")[HIGHLIGHT], [(5, 6), (6, 6), (9, 7)])
    return s


def brigadeiros_hand():
    # Brigadeiros: round chocolate truffles rolled in chocolate sprinkles, each sitting in a little paper case.
    s = Sprite()
    case = plain_ramp("#d8c0a8")
    for cx, cy in [(4.4, 11.4), (11.0, 11.4), (7.6, 5.6)]:
        s.shape(lambda x, y, cx=cx, cy=cy: cy - 0.4 <= y <= cy + 2.6 and abs(x - cx) <= 3.2 - (y - cy) * 0.3, case, (cx, cy + 1), (3.2, 1.6))
        tones = ramp("#4a2a1e", spread=0.6)
        s.ellipse(tones, cx, cy - 0.8, 2.8, 2.6)
        s.pixels_at(ramp("#2a1610")[BASE], [(round(cx) - 1, round(cy) - 2), (round(cx) + 1, round(cy) - 1), (round(cx), round(cy))])
        s.put(round(cx) - 2, round(cy) - 3, tones[HIGHLIGHT])
    return s


def brined_cheese_hand():
    # Brined cheese: a block of white cheese, moist and crumbly at the edges, glistening with brine.
    s = Sprite()
    cheese = plain_ramp("#f2f0e8")
    s.shape(lambda x, y: 2 <= x <= 13 and 4 <= y <= 12, cheese, (7.5, 8.0), (6.0, 4.0))
    for x in range(3, 13):
        s.put(x, 5, cheese[HIGHLIGHT])  # The top face.
    s.pixels_at(cheese[SHADOW], [(5, 8), (9, 10), (11, 7), (7, 11), (12, 12), (2, 6)])  # Crumbly holes.
    s.pixels_at(plain_ramp("#d0e4ec")[BASE], [(4, 6), (10, 6), (13, 9)])  # Brine glints.
    return s


def brined_pork_hand():
    # Brined pork: a pale pink slab with a band of white fat, glistening from the brine.
    s = slab("#d88a84", fat="#f2ece2", rx=6.6, ry=4.8, angle=-10, fat_side=1.6, seed=61)
    s.pixels_at(plain_ramp("#e8f0f4")[BASE], [(5, 6), (9, 5), (7, 9)])
    return s


def brown_butter_hand():
    # Brown butter: melted butter cooked to a nutty golden brown, with toasted brown flecks, in a small dish.
    s = bowl("#c8862a", flecks=[("#6a3a1a", 0.14)])
    s.pixels_at(ramp("#c8862a")[HIGHLIGHT], [(4, 7), (5, 7)])
    return s

def boiled_bua_loy_hand():
    # Boiled bua loy: little glossy balls of pumpkin rice-flour dough, cooked and soft.
    return balls("#f0b050", [(4.0, 11.4), (8.0, 11.6), (12.0, 11.0), (6.0, 7.4), (10.2, 7.2), (8.0, 3.6)], radius=2.2, plain=False)


def bua_loy_balls_hand():
    # Raw bua loy balls: little matte balls of orange pumpkin dough, rolled and dusted with flour.
    s = balls("#eeb060", [(4.0, 11.4), (8.0, 11.6), (12.0, 11.0), (6.0, 7.4), (10.2, 7.2), (8.0, 3.6)], radius=2.2)
    s.pixels_at(plain_ramp("#faf6ee")[BASE], [(4, 12), (8, 12), (11, 11), (10, 7)])
    return s


def bua_loy_dough_hand():
    # Bua loy dough: a soft ball of glutinous rice flour kneaded with pumpkin, a warm orange.
    return dough_ball("#f0b462")


def brown_sugar_hand():
    # Brown sugar: soft, moist, clumpy, golden-brown sugar.
    return spice_bowl("#a8682a", fleck="#d8a050", seed=33)


def browned_beef_hand():
    # Browned beef: cubes of beef seared dark brown and crusty on the outside.
    s = Sprite()
    meat = ramp("#7a3a24", spread=0.6)
    for cx, cy in [(4.6, 5.0), (10.6, 5.4), (4.0, 11.2), (9.6, 10.6), (13.0, 12.4)]:
        s.shape(lambda x, y, cx=cx, cy=cy: abs(x - cx) <= 2.4 and abs(y - cy) <= 2.2, meat, (cx, cy), (2.4, 2.2))
        s.pixels_at(ramp("#3a1e14")[BASE], [(round(cx), round(cy)), (round(cx) + 1, round(cy) + 1)])
        s.put(round(cx) - 1, round(cy) - 1, meat[HIGHLIGHT])
    return s


def browned_mince_hand():
    # Browned mince: crumbled minced beef fried brown, in a heap.
    return seed_heap("#7a4a2e", fleck="#4a2a1a", seed=37)


def bruised_lemongrass_hand():
    # Bruised lemongrass: the stalks crushed with the back of a knife, split and pale where they were struck.
    s = SPRITES["lemongrass"]()
    crushed = plain_ramp("#f4f0c8")[BASE]
    for x in range(16):
        for y in range(16):
            if s.pixels[y][x] is not None and (x * 3 + y * 5) % 11 == 0:
                s.put(x, y, crushed)
    return s


def bruised_tea_leaves_hand():
    # Bruised tea leaves: rolled and twisted green leaves, darkening to copper where they were bruised.
    return seed_heap("#4a7a34", fleck="#9a5a2a", seed=41)


def brunsviger_hand():
    # Brunsviger: a slab of soft yeast cake under a sticky glaze of brown sugar and butter, pooled in dark dimples.
    s = Sprite()
    cake = plain_ramp("#e8c078")
    s.shape(lambda x, y: 1 <= x <= 14 and 5 <= y <= 13, cake, (7.5, 9.0), (7.0, 4.0))
    glaze = ramp("#8a4a1e", spread=0.6)
    for y in range(6, 10):
        for x in range(2, 14):
            s.put(x, y, glaze[BASE] if (x + y) % 4 else glaze[SHADOW])
    s.pixels_at(glaze[HIGHLIGHT], [(4, 6), (8, 7), (11, 6)])
    return s


def buttered_crumbs_hand():
    # Buttered crumbs: breadcrumbs toasted in butter and sugar, a richer, deeper gold.
    return spice_bowl("#c88a3a", fleck="#f0c870", seed=43)


def cake_batter_hand():
    # Cake batter: a bowl of thick, smooth, butter-yellow batter.
    s = bowl("#f2d070", plain=True)
    s.pixels_at(plain_ramp("#fae6a0")[BASE], [(4, 7), (5, 7), (6, 8)])
    return s


def caldillo_base_hand():
    # Caldillo base: a bowl of blended tomato, onion and garlic, a bright red purée.
    return bowl("#d03a26", flecks=[("#f0d8b0", 0.06)])


def calf_stomach_hand():
    # A calf's stomach, for rennet: a pale, pinkish-cream pouch with folds across it.
    s = Sprite()
    tones = plain_ramp("#e8c8b8")
    s.ellipse(tones, 7.5, 8.6, 6.4, 5.0, angle=-20)
    for x0, y0, x1, y1 in [(4, 7, 8, 5), (4, 10, 10, 7), (6, 12, 11, 10)]:
        s.line(tones[SHADOW], x0, y0, x1, y1)
    s.pixels_at(tones[HIGHLIGHT], [(5, 5), (6, 4)])
    s.pixels_at(plain_ramp("#d8a898")[BASE], [(13, 4), (14, 3)])  # The neck of the pouch.
    return s


def cane_sugar_paste_hand():
    # Cane sugar paste: cane juice boiled down to a thick, glossy, dark amber paste.
    s = bowl("#8a4a1a", mound=1.0)
    s.pixels_at(ramp("#8a4a1a")[HIGHLIGHT], [(5, 6), (6, 6)])
    return s


def canele_batter_hand():
    # Canelé batter: a thin, pale, creamy batter speckled with vanilla seeds.
    return bowl("#f4e6c0", plain=True, flecks=[("#2a2018", 0.08)])

def charred(name, chance=0.22, seed=5):
    """Another item's sprite blistered and blackened in patches, as from a hot grill or a dry pan."""
    def drawn():
        s = SPRITES[name]()
        rng = random.Random(seed)
        burnt = [(34, 26, 22), (62, 44, 34)]
        for y in range(16):
            for x in range(16):
                colour = s.pixels[y][x]
                if colour is None or sum(colour) < 120:
                    continue
                if rng.random() < chance:
                    s.put(x, y, burnt[rng.random() < 0.5])
        return s
    return drawn


def caneles_hand():
    # Canelés: small, tall, fluted cakes with a deep mahogany, caramelised crust and a pale top.
    s = Sprite()
    crust = ramp("#7a3a1a", spread=0.6)
    for cx, cy in [(4.6, 9.4), (11.0, 9.4)]:
        s.shape(lambda x, y, cx=cx: abs(x - cx) <= 3.2 - (y - 4) * 0.08 and 4 <= y <= 14, crust, (cx, 9.0), (3.2, 5.0))
        for x in range(round(cx) - 2, round(cx) + 3, 2):
            s.pixels_at(crust[SHADOW], [(x, y) for y in range(6, 14)])  # The flutes.
        s.pixels_at(ramp("#c8823a")[BASE], [(x, 4) for x in range(round(cx) - 2, round(cx) + 3)])  # The top.
    return s


def caramel_hand():
    # Caramel: melted sugar cooked to a clear, glossy amber, in a bowl.
    s = bowl("#e09028", plain=True)
    s.pixels_at(plain_ramp("#f8d070")[BASE], [(3, 7), (4, 7), (5, 7), (9, 8), (10, 8)])
    s.pixels_at(plain_ramp("#e09028")[BASE], [(13, 9), (13, 10), (13, 11)])  # A drip down the side.
    return s


def caramelised_onion_hand():
    # Caramelised onion: soft, sticky strands of onion cooked down to a deep golden brown, in a bowl.
    return bowl("#9a5a24", mound=1.6, texture=lambda x, y: -1 if (x + y * 2) % 3 == 0 else 1 if (x * 2 + y) % 5 == 0 else 0)


def cardoon_hand():
    # Cardoon: a bunch of long, ribbed, silvery grey-green stalks, trimmed of their leaves.
    s = Sprite()
    stalk = ramp("#9aaa8a", spread=0.6)
    for x0, x1 in [(3.4, 6.0), (6.6, 8.6), (9.8, 11.0), (12.4, 13.2)]:
        s.capsule(stalk, [(x0, 15.0), (x1, 1.0)], (1.5, 1.1))
        s.line(stalk[LIGHT], round(x0), 13, round(x1), 3)
    return s


def cardoon_flower_hand():
    # A cardoon flower: a big, spiky, thistle-like head, a scaly green-grey bud topped with a tuft of violet.
    s = Sprite()
    bud = ramp("#7a9a6a", spread=0.6)
    s.ellipse(bud, 7.5, 10.4, 4.8, 4.0, texture=lambda x, y: -1 if (x + y) % 2 == 0 else 0)
    violet = ramp("#8a4ab8", spread=0.6)
    for x in range(3, 13):
        top = 6 - (2 if 5 <= x <= 10 else 0) - (x % 2)
        s.pixels_at(violet[BASE if x % 3 else LIGHT], [(x, y) for y in range(top, 7)])
    s.line(ramp("#6a8a4a")[BASE], 7, 14, 7, 15)
    return s


def carne_seca_hand():
    # Carne seca: thin, dark, leathery strips of salted, sun-dried beef.
    s = Sprite()
    meat = ramp("#6a2a20", spread=0.6)
    for start, end in [((1.6, 4.0), (12.0, 1.8)), ((2.4, 9.0), (14.0, 5.4)), ((1.6, 14.0), (13.0, 10.4))]:
        s.capsule(meat, [start, end], (1.6, 1.2), texture=lambda x, y: -1 if (x * 2 + y) % 5 == 0 else 0)
        s.put(round((start[0] + end[0]) / 2), round((start[1] + end[1]) / 2 - 1), plain_ramp("#e8e0d8")[BASE])  # Salt.
    return s


def carnitas_pork_hand():
    # Carnitas: chunks of pork slow-cooked in lard until tender and golden brown.
    return carnitas("#c8823e")


def crisp_carnitas_hand():
    # Crisp carnitas: the pork chunks fried hard until deep brown and crackling at the edges.
    s = carnitas("#9a5a26")
    s.pixels_at(ramp("#4a2814")[BASE], [(3, 4), (11, 5), (5, 10), (10, 11), (14, 13)])
    return s


def carnitas(colour):
    s = Sprite()
    meat = ramp(colour, spread=0.6)
    for cx, cy in [(4.6, 5.0), (10.6, 5.6), (4.4, 11.0), (9.8, 10.6), (13.0, 12.6)]:
        s.ellipse(meat, cx, cy, 2.8, 2.4, texture=lambda x, y: -1 if (x + y) % 3 == 0 else 0)
        s.put(round(cx) - 1, round(cy) - 1, meat[HIGHLIGHT])
    return s


def carnitas_tacos_hand():
    # Carnitas tacos: two soft corn tortillas folded round crisp pork, with white onion, green cilantro and red salsa.
    s = Sprite()
    tortilla = plain_ramp("#e8d090")
    for cx, cy in [(5.0, 6.2), (10.4, 10.6)]:
        s.ellipse(tortilla, cx, cy, 4.8, 3.8)
        filling = [(cx - 2, cy - 1), (cx - 1, cy - 2), (cx, cy - 1), (cx + 1, cy - 2), (cx + 2, cy - 1)]
        s.pixels_at(ramp("#b8742e")[BASE], [(round(x), round(y)) for x, y in filling])
        s.pixels_at(plain_ramp("#f6f4ee")[BASE], [(round(cx) - 1, round(cy) - 3)])
        s.pixels_at(ramp("#3a9a34")[BASE], [(round(cx) + 1, round(cy) - 3), (round(cx) - 2, round(cy) - 2)])
        s.pixels_at(ramp("#c8302a")[BASE], [(round(cx), round(cy) - 2)])
        s.put(round(cx) - 2, round(cy) + 1, tortilla[SHADOW])
    return s


def cassoulet_hand():
    # Cassoulet: a deep earthenware dish of white beans, sausage and duck under a golden breadcrumb crust.
    s = Sprite()
    dish = ramp("#a85a32", spread=0.6)
    s.shape(lambda x, y: y >= 7 and ((x - 7.5) / 7.2) ** 2 + ((y - 7.0) / 7.4) ** 2 <= 1, dish, (7.5, 10.5), (7.2, 5.0))
    crust = ramp("#d0963e", spread=0.6)
    s.ellipse(crust, 7.5, 7.0, 6.6, 2.6, texture=lambda x, y: -1 if (x * 3 + y) % 4 == 0 else 0)
    s.pixels_at(ramp("#8a3a2a")[BASE], [(5, 7), (10, 6)])  # Sausage breaking through.
    s.pixels_at(plain_ramp("#f2ead8")[BASE], [(8, 8), (3, 7)])  # Beans.
    return s


def cassoulet_beans_hand():
    # Cassoulet beans: plump white beans simmered in garlicky stock, in a bowl.
    s = bowl("#c8a46a", flecks=[("#f2ead8", 0.35)])
    return s


def ceviche_hand():
    # Ceviche: cubes of white fish cured in lime, with red onion, on a plate with a round of sweet potato, corn, and lime.
    s = plate()
    fish_ = plain_ramp("#f2f0ea")
    for x, y in [(4, 10), (6, 9), (5, 11), (7, 11), (8, 10)]:
        s.pixels_at(fish_[BASE], [(x, y)])
    s.pixels_at(ramp("#a83a6a")[BASE], [(5, 9), (7, 10), (6, 11)])  # Red onion.
    sweet = ramp("#f08a30", spread=0.5)
    s.ellipse(sweet, 11.2, 11.4, 2.2, 1.4)
    s.pixels_at(ramp("#f0c840")[BASE], [(9, 12), (10, 13)])  # Corn.
    lime = ramp("#7ac040", spread=0.6)
    s.ellipse(lime, 12.0, 8.4, 1.8, 1.6)
    s.put(12, 8, plain_ramp("#e8f4c8")[BASE])
    s.pixels_at(ramp("#3a9a34")[BASE], [(6, 8), (8, 9)])  # Cilantro.
    return s


def charred_dango_skewer_hand():
    # A dango skewer charred on the grill: three white rice dumplings on a stick, scorched in brown patches.
    s = Sprite()
    s.line(ramp("#c8a070")[BASE], 3, 15, 13, 1)
    ball = plain_ramp("#f2ece0")
    for cx, cy in [(5.2, 11.6), (7.8, 7.8), (10.4, 4.2)]:
        s.ellipse(ball, cx, cy, 2.6, 2.4)
        s.pixels_at(ramp("#9a5a2a")[BASE], [(round(cx), round(cy)), (round(cx) - 1, round(cy) + 1)])
        s.put(round(cx) - 1, round(cy) - 1, ball[HIGHLIGHT])
    return s

def chashu_pork_hand():
    # Chashu: rounds of rolled, braised pork belly, a spiral of pale fat and meat inside a dark, glossy soy-glazed edge.
    s = Sprite()
    glaze = ramp("#7a3a1e", spread=0.6)
    for cx, cy in [(5.2, 5.6), (10.4, 10.4)]:
        s.ellipse(glaze, cx, cy, 4.4, 4.0)
        s.ellipse(plain_ramp("#e8c8a8"), cx, cy, 3.2, 2.9, outline=False)
        s.ellipse(ramp("#b8705a", spread=0.5), cx, cy, 2.0, 1.8, outline=False)
        s.ellipse(plain_ramp("#f0dcc4"), cx, cy, 0.9, 0.8, outline=False)
        s.put(round(cx) - 3, round(cy) - 2, glaze[HIGHLIGHT])
    return s


def cheesecake_filling_hand():
    # Cheesecake filling: a smooth, thick, pale cream mixture with a zest of lemon, in a clay bowl.
    return bowl("#f6eed4", plain=True, bowl_colour=CLAY, flecks=[("#f0d040", 0.05)])


def cherry_compote_hand():
    # Cherry compote: whole dark red cherries softened in their syrup, in a bowl.
    s = bowl("#7a1426", mound=1.0)
    cherry = ramp("#a81e30", spread=0.6)
    for x, y in [(4, 7), (7, 6), (10, 7), (6, 8), (9, 8)]:
        s.put(x, y, cherry[LIGHT])
    return s


def cherry_sauce_hand():
    # Cherry sauce: the compote pressed smooth, a glossy deep red sauce with no lumps.
    s = bowl("#8a1a2a")
    s.pixels_at(ramp("#d04a5a")[BASE], [(4, 7), (5, 7), (6, 7)])
    return s


def chop(lean, fat, s=None, cx=8.4, cy=7.0, bone_end=(2.0, 14.4)):
    """A chop: a round eye of lean meat edged with fat, and the bone running out of it as a handle."""
    s = s or Sprite()
    bone = plain_ramp("#ece2cc")
    s.capsule(bone, [(cx - 2.0, cy + 2.0), bone_end], (1.2, 1.0))
    meat = ramp(lean, spread=0.6)
    s.ellipse(plain_ramp(fat), cx, cy, 5.6, 5.0)
    s.ellipse(meat, cx + 0.3, cy - 0.3, 4.2, 3.6, outline=False)
    s.put(round(cx) - 1, round(cy) - 2, meat[HIGHLIGHT])
    return s


def chevon_chops_hand():
    # Goat chops: dark red, lean meat with only a thin edge of fat, on a short bone.
    s = chop("#9a3a30", "#efe2d0", cx=5.6, cy=5.4, bone_end=(1.0, 12.4))
    return chop("#9a3a30", "#efe2d0", s=s, cx=10.8, cy=9.6, bone_end=(5.4, 15.0))


def chicken_bones_hand():
    # Chicken bones: two small leg bones and a wishbone, creamy white, from a stripped bird.
    s = Sprite()
    bone = plain_ramp("#ece2c8")
    for start, end in [((2.6, 13.0), (8.4, 5.0)), ((7.0, 14.4), (13.6, 8.4))]:
        s.capsule(bone, [start, end], (1.4, 1.4))
        for x, y in (start, end):
            s.ellipse(bone, x, y, 1.8, 1.7)
        s.put(round((start[0] + end[0]) / 2), round((start[1] + end[1]) / 2 - 1), bone[HIGHLIGHT])
    # The wishbone: a V at the top right.
    s.capsule(bone, [(9.0, 1.4), (11.6, 5.6), (14.4, 1.4)], (0.9, 0.9))
    return s


def chicken_breast_hand():
    # A chicken breast fillet: smooth, plump, pale pink, a rounded teardrop.
    s = Sprite()
    meat = plain_ramp("#f0b8b0")
    s.shape(lambda x, y: ((x - 6.6) / 5.0) ** 2 + ((y - 9.4) / 4.6) ** 2 <= 1 or (x >= 6 and abs(y - 9.0 + (x - 6) * 0.5) <= 3.4 - (x - 6) * 0.4),
            meat, (8.0, 8.4), (6.0, 5.0))
    s.pixels_at(meat[HIGHLIGHT], [(4, 7), (5, 6), (6, 6)])
    s.line(meat[SHADOW], 5, 11, 11, 7)  # The tenderloin seam.
    return s


def drumstick(skin, cx, cy, angle, s):
    """A drumstick: a plump rounded thigh end tapering to a knuckle of bone."""
    tones = plain_ramp(skin)
    cos, sin = math.cos(math.radians(angle)), math.sin(math.radians(angle))
    s.capsule(tones, [(cx, cy), (cx + cos * 5.0, cy + sin * 5.0)], (3.4, 1.3))
    bone = plain_ramp("#eee4cc")
    s.ellipse(bone, cx + cos * 6.0, cy + sin * 6.0, 1.2, 1.2)
    s.put(round(cx - 1), round(cy - 1), tones[HIGHLIGHT])
    return s


def chicken_drumsticks_hand():
    # Raw chicken drumsticks: plump, pale pink skin over the meat, tapering to the knuckle.
    s = drumstick("#eab49a", 4.6, 4.6, 35, Sprite())
    return drumstick("#eab49a", 8.4, 9.6, 15, s)


def chicken_feet_hand():
    # Chicken feet: pale yellow, scaly, with three long toes spread forward and one behind.
    s = Sprite()
    foot = plain_ramp("#e8c870")
    for cx, cy in [(5.0, 9.6), (11.0, 7.0)]:
        s.capsule(foot, [(cx, cy + 5.0), (cx, cy)], (1.2, 1.2))
        for dx, dy in [(-3.4, -3.4), (0.0, -4.6), (3.4, -3.4)]:
            s.capsule(foot, [(cx, cy), (cx + dx, cy + dy)], (0.9, 0.6))
        s.put(round(cx) - 1, round(cy) + 2, foot[SHADOW])
    return s


def chicken_giblets_hand():
    # Chicken giblets: a small dark heart, a glossy liver and a gizzard.
    s = heart("#8a2a30", cx=4.6, cy=5.4, size=0.55)
    liver("#6a2028", cx=10.6, cy=5.2, rx=4.0, ry=2.8, s=s)
    gizzard = ramp("#9a4a40", spread=0.6)
    s.ellipse(gizzard, 7.6, 11.6, 3.6, 2.8)
    s.pixels_at(plain_ramp("#e8d8c0")[BASE], [(6, 11), (7, 11), (8, 12)])  # Its silver skin.
    return s


def chicken_skin_hand():
    # Chicken skin: a loose, pale yellow sheet, pimpled with feather follicles.
    s = Sprite()
    skin = plain_ramp("#f0d8a0")
    s.shape(lambda x, y: ((x - 7.5) / 6.6) ** 2 + ((y - 8.0) / 5.4) ** 2 + 0.15 * math.sin(x * 1.3 + y) <= 1, skin, (7.5, 8.0), (6.6, 5.4))
    for x, y in [(4, 6), (7, 5), (10, 6), (5, 9), (8, 8), (11, 9), (6, 11), (9, 11)]:
        s.put(x, y, skin[SHADOW])
    return s


def chicken_thighs_hand():
    # Raw skin-on chicken thighs: broad, lumpy, irregular pieces wrapped in loose, pale pinkish-white skin, pimpled with
    # follicles, with pink meat showing where the skin falls short at the ends.
    s = Sprite()
    skin, meat = plain_ramp("#eccaca"), plain_ramp("#e89a8a")
    for cx, cy, angle, seed in [(5.2, 5.4, -15, 3), (10.4, 10.8, 20, 8)]:
        cos, sin = math.cos(math.radians(angle)), math.sin(math.radians(angle))
        rng = random.Random(seed)

        def lump(x, y, cx=cx, cy=cy, cos=cos, sin=sin):
            u, v = (x - cx) * cos + (y - cy) * sin, -(x - cx) * sin + (y - cy) * cos
            # A squarish, lumpy shape: wider than tall, its outline wobbling.
            wobble = 0.35 * math.sin(u * 1.7) + 0.25 * math.cos(v * 2.1)
            return (abs(u) / 5.0) ** 3 + (abs(v) / 3.8) ** 3 <= 1 + wobble * 0.3
        s.shape(lump, skin, (cx, cy), (5.0, 3.8))
        # Pink meat at one end, where the skin stops short.
        for t in (-1, 0, 1):
            x, y = cx - cos * 4.0 - sin * t, cy - sin * 4.0 + cos * t
            s.put(round(x - 0.5), round(y - 0.5), meat[BASE])
        s.put(round(cx - cos * 4.0 - 0.5), round(cy - sin * 4.0 + 1.5), meat[SHADOW])
        # Follicles and folds in the skin.
        for _ in range(6):
            x, y = round(cx + rng.uniform(-3, 3)), round(cy + rng.uniform(-2, 2))
            if s.pixels[y][x] in skin[1:]:
                s.put(x, y, skin[SHADOW])
        s.put(round(cx - 1), round(cy - 2), skin[HIGHLIGHT])
        s.put(round(cx), round(cy - 2), skin[HIGHLIGHT])
    return s


def chicken_wings_hand():
    # Raw chicken wings: the jointed wing in its pale, bumpy skin, with a pointed tip.
    s = Sprite()
    skin = plain_ramp("#f2d0b0")
    for (ax, ay), (bx, by), (tx, ty) in [((2.0, 6.0), (6.6, 4.0), (10.0, 1.4)), ((5.0, 13.0), (10.0, 10.6), (14.4, 7.0))]:
        s.capsule(skin, [(ax, ay), (bx, by)], (2.6, 2.2))
        s.capsule(skin, [(bx, by), (tx, ty)], (2.0, 0.8))
        s.put(round(bx - 0.5), round(by - 0.5), skin[SHADOW])  # The joint.
        s.put(round(ax), round(ay - 1), skin[HIGHLIGHT])
    return s


def chickpea_flour_hand():
    # Chickpea flour: a fine, pale yellow flour.
    return spice_bowl("#e8d48a", fleck="#c8b060", seed=47)

PIECES = [(3.4, 4.0), (8.0, 3.2), (12.4, 4.6), (2.6, 8.6), (6.6, 7.8), (11.0, 8.8), (4.2, 12.6), (8.6, 12.2), (12.8, 12.8)]


def chopped(flesh, skin=None, size=1.6, seed=3, plain=False, spots=PIECES):
    """Chopped or diced food: small solid cubes scattered in a heap, each lit on its top and left faces and shaded on its
    bottom and right, some showing the skin along the bottom edge. Size 1.2 reads as diced, 1.6 and up as chopped."""
    s = Sprite()
    tones = plain_ramp(flesh) if plain else ramp(flesh, spread=0.6)
    rind = (plain_ramp(skin) if plain else ramp(skin, spread=0.6)) if skin else None
    rng = random.Random(seed)
    side = max(3, round(size * 2.2))
    for cx, cy in spots:
        x0 = round(cx - side / 2 + rng.uniform(-0.5, 0.5))
        y0 = round(cy - side / 2 + rng.uniform(-0.5, 0.5))
        w, h = side + rng.choice((0, 0, 1)), side + rng.choice((0, 0, -1 if side > 3 else 0))
        for y in range(y0, y0 + h):
            for x in range(x0, x0 + w):
                if x == x0 + w - 1 or y == y0 + h - 1:
                    colour = tones[OUTLINE] if x == x0 + w - 1 and y == y0 + h - 1 else tones[SHADOW]
                elif x == x0 or y == y0:
                    colour = tones[LIGHT]
                else:
                    colour = tones[BASE]
                s.put(x, y, colour)
        s.put(x0, y0, tones[HIGHLIGHT])
        if rind and rng.random() < 0.6:
            s.pixels_at(rind[BASE], [(x, y0 + h - 1) for x in range(x0, x0 + w - 1)])
    return s


def chiles_rellenos_hand():
    # Chiles rellenos: a stuffed poblano fried in a golden, puffy egg batter, served in a pool of tomato caldillo.
    s = plate()
    sauce = ramp("#c83a26", spread=0.5)
    s.ellipse(sauce, 7.5, 11.2, 6.0, 2.6)
    batter = plain_ramp("#e8c060")
    s.ellipse(batter, 7.4, 8.8, 5.6, 2.8, angle=-10)
    s.pixels_at(ramp("#3a7a2a")[BASE], [(11, 7), (12, 6), (13, 6)])  # The chili's stem.
    s.pixels_at(batter[HIGHLIGHT], [(4, 8), (6, 7), (8, 7)])
    s.pixels_at(batter[SHADOW], [(5, 10), (9, 10)])
    return s


def chili_garlic_hand():
    # Chili garlic: a coarse paste of pounded red chilies and white garlic, in a bowl.
    return bowl("#c82a1e", mound=1.0, flecks=[("#f2e8d0", 0.14), ("#f05a3a", 0.1)])


def chili_pork_hand():
    # Chili pork: chunks of pork shoulder simmered in a dark red chili sauce, in a bowl.
    return bowl("#8a2418", mound=0.8, flecks=[("#c8885a", 0.16), ("#b03a26", 0.1)])


def chili_powder_hand():
    # Chili powder: a deep brick-red powder.
    return spice_bowl("#b8321e", fleck="#e0602e", seed=51)


def chocolate_hand():
    # A bar of chocolate: dark, glossy, set in squares, with one corner broken off.
    s = Sprite()
    choc = ramp("#5a301e", spread=0.6)
    s.shape(lambda x, y: 2 <= x <= 13 and 1 <= y <= 14 and not (x >= 11 and y <= 3), choc, (7.5, 7.5), (5.5, 6.5))
    for y in range(2, 14):
        for x in range(3, 13):
            if s.pixels[y][x] in choc[1:]:
                if (y - 1) % 4 == 0 or (x - 2) % 4 == 0:
                    s.put(x, y, choc[SHADOW])  # The grooves between squares.
                elif (y - 2) % 4 == 0 and (x - 3) % 4 == 0:
                    s.put(x, y, choc[HIGHLIGHT])
    return s


def chocolate_sponge_hand():
    # A chocolate sponge: a round, risen, dark brown cake, cut to show its airy crumb.
    s = Sprite()
    cake = ramp("#6a3a24", spread=0.6)
    s.ellipse(cake, 7.5, 10.4, 7.0, 3.4)
    s.shape(lambda x, y: 1 <= x <= 14 and 6 <= y <= 10, cake, (7.5, 8.0), (7.0, 2.0))
    s.ellipse(ramp("#7a4630", spread=0.5), 7.5, 6.2, 7.0, 2.8)
    for x, y in [(3, 9), (6, 10), (9, 9), (12, 10), (5, 12), (10, 12)]:
        s.put(x, y, cake[SHADOW])  # Air holes in the crumb.
    s.pixels_at(cake[HIGHLIGHT], [(4, 5), (5, 5)])
    return s


def chopped_almonds_hand():
    # Chopped almonds: rough, creamy white pieces of blanched almond.
    return chopped("#e8d6b0", size=1.3, plain=True, seed=5)


def chopped_apple_hand():
    # Chopped apple: crisp, pale cream chunks, some with a strip of red skin.
    return chopped("#f2e6b4", skin="#c82a2a", seed=7, plain=True)


def chopped_beetroot_hand():
    # Chopped beetroot: deep purple-red cubes.
    return chopped("#8a1a40", seed=9)


def chopped_carrot_hand():
    # Chopped carrot: bright orange chunks.
    return chopped("#f07a20", seed=11)


def chopped_gherkins_hand():
    # Chopped gherkins: pieces of pickled cucumber, olive green with bumpy dark skin on one edge.
    return chopped("#8e9e4c", skin="#3a5a22", seed=13)


def chopped_laver_hand():
    # Chopped laver: thin, ragged, dark green-black strips of seaweed.
    s = Sprite()
    laver = ramp("#2a3a2a", spread=0.5)
    for start, end in [((2.0, 3.0), (7.0, 5.0)), ((9.0, 2.0), (14.0, 4.0)), ((1.4, 8.0), (6.0, 10.0)), ((8.0, 7.6), (13.6, 9.4)),
                       ((3.0, 13.0), (8.0, 14.4)), ((10.0, 12.4), (14.6, 14.0))]:
        s.capsule(laver, [start, end], (1.1, 0.9))
        s.put(round(start[0] + 1), round(start[1] - 1), ramp("#4a6a4a")[BASE])
    return s


def chopped_onion_hand():
    # Chopped onion: small, glassy white pieces with a pale green-yellow tinge.
    return chopped("#e8e6d0", size=1.3, plain=True, seed=15)


def chopped_rhubarb_hand():
    # Chopped rhubarb: pieces of stalk, pink-red outside shading to green.
    return chopped("#c8e0a0", skin="#d03a4a", seed=17, plain=True)


def chorizo_hand():
    # Chorizo: a cured, wrinkled, brick-red sausage, and a slice cut from it showing red meat flecked with white fat.
    s = sausage("#a02a1e", (2.0, 11.6), (11.4, 2.2), radius=2.2)
    s.ellipse(ramp("#a02a1e", spread=0.5), 11.4, 11.6, 3.0, 3.0)
    s.ellipse(ramp("#c8402a", spread=0.4), 11.4, 11.6, 2.0, 2.0, outline=False)
    s.pixels_at(plain_ramp("#f2e8d8")[BASE], [(10, 11), (12, 12), (11, 10), (12, 10)])
    return s

def chorizo_filling_hand():
    # Chorizo filling: minced pork stained deep orange-red with paprika, flecked with garlic, in a bowl.
    return bowl("#c84a2a", mound=1.8, texture=bumpy, flecks=[("#f0e8d0", 0.06), ("#8a2a1a", 0.08)])


def choux_buns_hand():
    # Choux buns: light, hollow, puffed pastry balls, golden brown with crisp, cracked tops.
    s = Sprite()
    pastry = ramp("#d0923e", spread=0.6)
    for cx, cy, r in [(4.6, 10.6, 3.6), (11.0, 10.4, 3.4), (7.8, 5.0, 3.6)]:
        s.ellipse(pastry, cx, cy, r, r * 0.85)
        s.pixels_at(ramp("#8a5420")[BASE], [(round(cx) - 1, round(cy) - 1), (round(cx), round(cy) - 2), (round(cx) + 1, round(cy) - 1)])  # Cracks.
        s.put(round(cx) - 2, round(cy) - 2, pastry[HIGHLIGHT])
    return s


def choux_panade_hand():
    # Choux panade: flour cooked into butter and water until it forms a smooth, pale ball that leaves the pan clean.
    s = dough_ball("#f2dc9c", flour=False)
    s.pixels_at(plain_ramp("#fbefc4")[BASE], [(5, 7), (6, 7)])
    return s


def choux_pastry_hand():
    # Choux pastry: the panade beaten with eggs into a smooth, glossy, pale yellow paste, in a bowl.
    s = bowl("#f2d070", mound=1.4, plain=True)
    s.pixels_at(plain_ramp("#fbe8a8")[BASE], [(5, 5), (6, 5), (7, 6)])
    return s


def churro_dough_hand():
    # Churro dough: a stiff, smooth, pale dough of flour cooked with water.
    return dough_ball("#f0e2bc", flour=False)


def churros_hand():
    # Churros: ridged golden sticks of fried dough rolled in cinnamon sugar, with a little dish of chocolate sauce.
    return churro_sticks(served=True)


def fried_churros_hand():
    # Fried churros: the ridged golden sticks straight from the oil, before sugar and sauce.
    return churro_sticks(served=False)


def churro_sticks(served):
    s = Sprite()
    if served:
        dish = plain_ramp("#e8eaee")
        s.ellipse(dish, 11.6, 12.4, 3.8, 2.4)
        s.ellipse(ramp("#4a2618", spread=0.5), 11.6, 12.0, 2.8, 1.4, outline=False)
    dough = ramp("#d8963a", spread=0.6)
    for start, end in [((1.6, 12.0), (9.6, 1.6)), ((4.6, 14.4), (13.0, 4.0))]:
        s.capsule(dough, [start, end], (1.5, 1.5))
        length = math.dist(start, end)
        for t in range(1, int(length), 2):
            x = start[0] + (end[0] - start[0]) * t / length
            y = start[1] + (end[1] - start[1]) * t / length
            s.put(round(x - 0.5), round(y - 0.5), dough[SHADOW])  # The ridges.
        if served:
            s.put(round(start[0] + 2), round(start[1] - 3), plain_ramp("#f6eedc")[BASE])  # Sugar.
    return s


def cinnamon_hand():
    # Cinnamon: dried quills rolled tight, thin and dark reddish brown, three together.
    s = Sprite()
    bark = ramp("#8a4424", spread=0.8)
    for start, end in (((1.6, 6.6), (12.6, 1.4)), ((3.0, 14.4), (14.4, 8.6))):
        s.capsule(bark, [start, end], (1.8, 1.8))
        x, y = round(start[0]), round(start[1])
        s.pixels_at(ramp("#c8804a")[BASE], [(x - 1, y - 1), (x, y - 1)])
        s.put(x - 1, y, bark[OUTLINE])
    return s


def cinnamon_sapling_hand():
    # Cinnamon: glossy, leathery leaves, flushed red when young, with three pale veins.
    s = sapling("#3e8a3a", trunk_colour="#8a5a3a", leaf=(2.6, 1.5))
    s.pixels_at(ramp("#b84a3a")[BASE], [(5, 2), (5, 3)])  # A young red leaf at the top.
    return s


def clove_sapling_hand():
    # Clove: slim, glossy, aromatic leaves, pinkish when young.
    return sapling("#3a8040", trunk_colour="#7a6048", leaf=(2.6, 1.2))


def coarse_salt_hand():
    # Coarse salt: a heap of big, clear, white crystals.
    s = Sprite()
    salt = plain_ramp("#eef0f2")
    rng = random.Random(61)
    for cx, cy in [(4, 13), (7, 13), (10, 13), (13, 13), (5, 10), (8, 10), (11, 10), (6, 7), (9, 7), (7, 4)]:
        cx, cy = cx + rng.choice((0, 1)), cy
        s.pixels_at(salt[SHADOW], [(cx, cy + 1), (cx + 1, cy + 1)])
        s.pixels_at(salt[BASE], [(cx, cy), (cx + 1, cy)])
        s.pixels_at(salt[HIGHLIGHT], [(cx, cy - 1)])
        s.pixels_at(salt[OUTLINE], [(cx + 2, cy + 1), (cx - 1, cy + 1)])
    return s


def coated_water_chestnut_hand():
    # Coated water chestnut: little cubes dyed ruby red with beetroot and dusted in white tapioca starch.
    s = chopped("#c82a3a", size=1.2, seed=19)
    s.pixels_at(plain_ramp("#f4f2ee")[BASE], [(3, 3), (8, 2), (12, 4), (6, 7), (11, 8), (3, 12), (9, 11)])
    return s


def cocktail_sausage_filling_hand():
    # Cocktail sausage filling: plain, finely minced pale pink pork, in a bowl.
    return bowl("#eab4aa", mound=1.8, texture=bumpy)


def cocktail_sausages_hand():
    # Cocktail sausages: small, plump, smoked links, reddish brown, linked together in a row.
    s = Sprite()
    for (sx, sy), (ex, ey) in [((2.4, 5.0), (6.4, 3.4)), ((9.0, 3.4), (13.0, 5.0)), ((2.4, 12.0), (6.4, 10.4)), ((9.0, 10.4), (13.0, 12.0))]:
        sausage("#b04a2e", (sx, sy), (ex, ey), radius=2.0, s=s)
    s.pixels_at(plain_ramp("#d8c8a8")[SHADOW], [(7, 4), (8, 4), (7, 11), (8, 11)])  # The twists linking them.
    return s


def cocoa_mass_hand():
    # Cocoa mass: ground roasted cocoa nibs, a thick, glossy, very dark brown liquor, in a bowl.
    s = bowl("#3a2018", mound=0.6)
    s.pixels_at(ramp("#6a4030")[BASE], [(4, 7), (5, 7), (9, 7)])
    return s

def cocoa_powder_hand():
    # Cocoa powder: a fine, dry, warm brown powder.
    return spice_bowl("#7a4a32", fleck="#a8705a", seed=53)


def cocoa_press_cake_hand():
    # Cocoa press cake: the hard, dense disc of cocoa solids left in the press once the butter is squeezed out.
    s = Sprite()
    cake = ramp("#5a3424", spread=0.6)
    s.ellipse(cake, 7.5, 10.4, 7.0, 3.4)
    s.shape(lambda x, y: 1 <= x <= 14 and 7 <= y <= 10, cake, (7.5, 8.5), (7.0, 2.0))
    s.ellipse(ramp("#6e4430", spread=0.5), 7.5, 7.0, 7.0, 2.8)
    s.pixels_at(cake[SHADOW], [(5, 7), (9, 6), (11, 8), (4, 9)])  # The marks of the press cloth.
    s.pixels_at(plain_ramp("#d8c098")[BASE], [(7, 6), (10, 7)])  # A bloom of cocoa butter.
    return s


def coconut_sapling_hand():
    # A coconut palm seedling: fronds rising from the base.
    return sapling("#4a9a3a", trunk_colour="#8a6a3a", palm=True)


def coconut_sticky_rice_hand():
    # Coconut sticky rice: a mound of glossy white glutinous rice soaked in sweet coconut sauce, on a banana leaf.
    s = Sprite()
    leaf = ramp("#3a8a34", spread=0.6)
    s.ellipse(leaf, 7.5, 11.6, 7.4, 3.4)
    s.line(leaf[LIGHT], 1, 12, 14, 11)
    rice = plain_ramp("#f4f0e2")
    s.ellipse(rice, 7.0, 8.4, 5.0, 3.6, texture=lambda x, y: -1 if (x * 3 + y) % 5 == 0 else 0)
    s.pixels_at(plain_ramp("#f6eccc")[LIGHT], [(5, 6), (6, 6), (7, 6), (8, 7)])  # The coconut sauce.
    s.pixels_at(ramp("#d8a858")[BASE], [(6, 5), (9, 6)])  # Toasted mung beans on top.
    return s


def cod_roe_hand():
    # Cod roe: a pair of plump sacs, pale pinkish orange, finely grained with tiny eggs under a thin membrane.
    s = Sprite()
    roe = plain_ramp("#eca88a")
    for cx, cy, angle in [(5.4, 7.2, -30), (10.4, 9.4, -30)]:
        s.ellipse(roe, cx, cy, 4.8, 2.8, angle=angle, texture=lambda x, y: -1 if (x + y * 2) % 4 == 0 else 0)
        s.put(round(cx) - 2, round(cy) - 2, roe[HIGHLIGHT])
    s.pixels_at(ramp("#a84a4a")[BASE], [(1, 11), (2, 10)])  # The membrane joining them.
    return s


def coffee_sapling_hand():
    # Coffee: glossy, dark green, wavy-edged leaves in pairs.
    return sapling("#2e6e30", trunk_colour="#6a5a3a", leaf=(2.6, 1.5))


def concha_topping_hand():
    # Concha topping: a soft paste of flour, sugar and lard, half of it coloured with cocoa, in two balls.
    s = Sprite()
    for (cx, cy), colour in [((5.0, 8.4), "#f2e2bc"), ((10.6, 9.0), "#7a4a32")]:
        tones = plain_ramp(colour)
        s.ellipse(tones, cx, cy, 3.6, 3.2)
        s.put(round(cx) - 2, round(cy) - 2, tones[HIGHLIGHT])
    return s


def concha(topping, cx, cy, r, s):
    """A concha: a round sweet bun with a sugar crust scored in a shell pattern."""
    bun = ramp("#d8a058", spread=0.6)
    s.ellipse(bun, cx, cy + 1.0, r, r * 0.7)
    crust = plain_ramp(topping)
    s.ellipse(crust, cx, cy, r - 0.6, r * 0.6, outline=False)
    for k in (-2, 0, 2):  # The shell grooves fanning from the front.
        s.line(crust[SHADOW], round(cx + k * 0.4), round(cy + r * 0.4), round(cx + k * 1.4), round(cy - r * 0.4))
    return s


def conchas_hand():
    # Conchas: round Mexican sweet buns, one with a white crust and one with a cocoa crust, scored like shells.
    s = concha("#f4e8cc", 5.0, 5.4, 4.4, Sprite())
    return concha("#7a4a32", 10.6, 10.6, 4.4, s)


def cookbook_hand():
    # The cookbook: a thick book bound in deep red cloth, a gold spoon and fork crossed on its cover.
    s = Sprite()
    cover = ramp("#8a2a2a", spread=0.6)
    s.shape(lambda x, y: 2 <= x <= 13 and 1 <= y <= 14, cover, (7.5, 7.5), (5.5, 6.5))
    pages = plain_ramp("#f2ead4")
    s.pixels_at(pages[BASE], [(x, 14) for x in range(3, 13)])
    s.pixels_at(pages[SHADOW], [(13, y) for y in range(2, 14)])
    s.pixels_at(cover[SHADOW], [(3, y) for y in range(2, 14)])  # The spine.
    gold = ramp("#e8b830", spread=0.6)
    s.line(gold[BASE], 5, 11, 11, 4)  # The spoon handle.
    s.ellipse(gold, 11.0, 4.0, 1.3, 1.6, angle=40)
    s.line(gold[LIGHT], 5, 4, 11, 11)  # The fork handle.
    s.pixels_at(gold[LIGHT], [(4, 3), (5, 2), (6, 3), (4, 4)])  # The fork tines.
    return s


def cooked_black_beans_hand():
    # Cooked black beans: whole, glossy, purple-black beans in their dark broth, in a bowl.
    s = bowl("#2a2230", mound=0.8)
    shine = ramp("#6a5a7a")[BASE]
    for x, y in [(4, 7), (7, 6), (10, 7), (6, 8), (9, 8)]:
        s.put(x, y, shine)
    return s


def cooked_chevon_hand():
    # Seared goat meat: a lean piece browned dark and crusty on the outside.
    s = slab("#7a3a24", rx=6.4, ry=4.6, angle=-12, seed=67)
    s.pixels_at(ramp("#3a1e14")[BASE], [(4, 7), (7, 6), (10, 8), (6, 10), (11, 6)])
    s.pixels_at(ramp("#b86a40")[BASE], [(5, 6), (9, 6)])
    return s


def cooked_corn_hand():
    # Cooked corn: a boiled cob, its kernels plump, bright and glistening.
    s = SPRITES["corn"]()
    for x in range(16):
        for y in range(16):
            if s.pixels[y][x] is not None and (x * 2 + y * 3) % 7 == 0 and sum(s.pixels[y][x]) > 400:
                s.put(x, y, (255, 246, 200))
    return s


def cooked_soybeans_hand():
    # Cooked soybeans: plump, pale yellow beans, soft from simmering, in a bowl.
    s = bowl("#e0cc80", mound=1.0, texture=bumpy)
    return s


def cookie_dough_hand():
    # Cookie dough: a soft, buttery, tan dough studded with bits of cocoa bean.
    s = dough_ball("#e2c088", flour=False)
    s.pixels_at(ramp("#4a2a1a")[BASE], [(4, 9), (7, 7), (10, 9), (6, 11), (9, 12), (11, 7)])
    return s


def cooled_brigadeiro_mixture_hand():
    # Cooled brigadeiro mixture: the fudge set firm and matte, with a scoop taken out of it, in a bowl.
    s = bowl("#4a2a1e", mound=1.6)
    s.pixels_at(ramp("#2e1a12")[BASE], [(8, 6), (9, 6), (9, 7)])
    return s

def coq_au_vin_hand():
    # Coq au vin: chicken braised in red wine, the pieces glazed a deep burgundy, with mushrooms and little onions.
    s = bowl("#5a1e2a", mound=1.0, flecks=[("#c8885a", 0.16), ("#d8c4a0", 0.06), ("#f0e8dc", 0.04)])
    return s


def corn_husk_hand():
    # Corn husks: long, papery, pale green-gold leaves stripped from the cob, laid one over another.
    s = Sprite()
    for i, (start, end) in enumerate([((2.0, 14.6), (10.0, 1.0)), ((5.0, 15.0), (13.6, 2.4)), ((7.4, 15.0), (14.6, 6.0))]):
        tones = plain_ramp(["#d8d494", "#c8cc84", "#e2dca4"][i])
        s.capsule(tones, [start, ((start[0] + end[0]) / 2 + 0.6, (start[1] + end[1]) / 2), end], (2.0, 0.6))
        s.line(tones[SHADOW], round(start[0] + 1), round(start[1] - 1), round(end[0] - 1), round(end[1] + 1))
    return s


def corn_kernels_hand():
    # Corn kernels: a heap of plump, golden-yellow kernels cut from the cob.
    s = Sprite()
    kernel = ramp("#f0c030", spread=0.6)
    rows = [(5, 4, 3), (4, 7, 5), (3, 10, 7), (2, 13, 8)]
    for y0, x0, count in [(r[1], r[0], r[2]) for r in [(6, 4, 3), (5, 7, 4), (3, 10, 6), (2, 13, 7)]]:
        for k in range(count):
            x = x0 + k * 2
            s.pixels_at(kernel[BASE], [(x, y0)])
            s.pixels_at(kernel[HIGHLIGHT], [(x, y0 - 1)])
            s.pixels_at(kernel[SHADOW], [(x + 1, y0), (x, y0 + 1)])
    return s


def corn_paste_hand():
    # Corn paste: fresh corn ground to a thick, coarse, pale yellow paste, in a bowl.
    return bowl("#ecd47a", mound=1.4, texture=bumpy, plain=True)


def corn_tortilla_hand():
    # Corn tortillas: a small stack of thin, round, pale yellow flatbreads, freckled brown from the griddle.
    s = Sprite()
    tones = plain_ramp("#ecd8a0")
    for dy in (3.0, 1.5, 0.0):
        s.ellipse(tones, 7.5, 7.6 + dy, 6.8, 3.6)
    s.pixels_at(ramp("#a8742a")[BASE], [(4, 7), (7, 6), (10, 8), (6, 9), (11, 6), (8, 8)])
    return s


def cornstarch_hand():
    # Cornstarch: a fine, squeaky, pure white powder, in a dark clay bowl so it shows.
    return spice_bowl("#f4f4f2", fleck="#e2e2de", seed=59, bowl_colour=CLAY)


def cotija_cheese_hand():
    # Cotija: a hard, dry, salty white cheese, a wedge of it crumbling at the edge.
    s = cheese_wedge("#f2eedc", "#e0d8c0", holes=[(5, 12), (8, 11)], plain=True)
    s.pixels_at(plain_ramp("#f2eedc")[BASE], [(2, 15), (4, 15), (13, 15)])  # Crumbs.
    s.pixels_at(plain_ramp("#f2eedc")[SHADOW], [(3, 15), (14, 15)])
    return s


def coxinha_dough_hand():
    # Coxinha dough: flour cooked in chicken stock into a smooth, pale golden dough.
    return dough_ball("#eed8a0", flour=False)


def coxinhas_hand():
    # Coxinhas: teardrop-shaped croquettes, crumbed and fried a deep golden brown.
    s = Sprite()
    crumb = ramp("#c8823a", spread=0.6)
    for cx, cy in [(5.0, 9.4), (11.0, 10.4)]:
        s.shape(lambda x, y, cx=cx, cy=cy: ((x - cx) / 3.4) ** 2 + ((y - cy - 1.0) / 3.2) ** 2 <= 1 or
                (y <= cy and abs(x - cx) <= (y - (cy - 5.4)) * 0.6), crumb, (cx, cy), (3.4, 4.4),
                texture=lambda x, y: -1 if (x * 3 + y) % 4 == 0 else 0)
        s.put(round(cx) - 1, round(cy) - 1, crumb[HIGHLIGHT])
    return s


def cracked_rye_hand():
    # Cracked rye: a heap of broken grey-brown grains.
    return seed_heap("#8a7a5a", fleck="#c8b890", seed=71)


def cracklings_hand():
    # Cracklings: crisp, puffed, golden-brown bits of pork skin and fat left from rendering.
    return chopped("#c8862e", size=1.4, seed=73)


def cream_cheese_hand():
    # Cream cheese: a thick, smooth, bright white mound with a swirl on top, in a clay bowl.
    s = bowl("#f6f4ee", mound=2.4, plain=True, bowl_colour=CLAY)
    s.pixels_at(plain_ramp("#f6f4ee")[SHADOW], [(6, 5), (7, 6), (9, 5)])
    return s


def creamed_chicken_sauce_hand():
    # Creamed chicken sauce: a pale, creamy sauce with pieces of chicken, in a clay bowl.
    return bowl("#f0e4c4", plain=True, bowl_colour=CLAY, flecks=[("#e8c8a0", 0.14), ("#6a9a4a", 0.04)])


def crema_hand():
    # Crema: thin, tangy soured cream, glossy white with a little lime, in a clay bowl.
    s = bowl("#f6f4ec", plain=True, bowl_colour=CLAY, flecks=[("#9ac850", 0.03)])
    s.pixels_at(plain_ramp("#ffffff")[BASE], [(4, 7), (5, 7), (6, 7)])
    return s

def creme_brulee_custard_hand():
    # Crème brûlée custard: cream, egg yolks and sugar, a smooth pale yellow speckled with vanilla, in a bowl.
    return bowl("#f2dc90", plain=True, flecks=[("#2a2018", 0.06)])


def creme_fraiche_hand():
    # Crème fraîche: thick, rich, slightly soured cream, heaped in a clay bowl.
    s = bowl("#f6f2e2", mound=1.8, plain=True, bowl_colour=CLAY)
    s.pixels_at(plain_ramp("#f6f2e2")[SHADOW], [(7, 6), (8, 5)])
    return s


def crisp_pork_belly_hand():
    # Crisp pork belly: slices fried golden, with a layer of puffed, crackling skin along the top.
    s = Sprite()
    for cx, cy in [(5.4, 5.4), (10.2, 10.6)]:
        meat, fat, crackle = ramp("#b8783e", spread=0.6), plain_ramp("#f0d8a8"), ramp("#d89a3a", spread=0.6)
        s.shape(lambda x, y, cx=cx, cy=cy: abs(x - cx) <= 4.4 and abs(y - cy) <= 3.2, meat, (cx, cy), (4.4, 3.2))
        for x in range(round(cx) - 3, round(cx) + 4):
            s.put(x, round(cy) - 2, crackle[LIGHT if x % 2 else BASE])
            s.put(x, round(cy), fat[BASE])
    return s


def croque_monsieur_hand():
    # A croque monsieur: a toasted ham sandwich under a bubbling, browned top of cheese sauce.
    s = Sprite()
    bread = plain_ramp("#e8c888")
    s.shape(lambda x, y: 1 <= x <= 14 and 5 <= y <= 13, bread, (7.5, 9.0), (7.0, 4.0))
    s.pixels_at(ramp("#d07a6a")[BASE], [(x, 10) for x in range(2, 14)])  # The ham.
    s.pixels_at(bread[SHADOW], [(x, 12) for x in range(2, 14)])
    top = ramp("#e8b450", spread=0.6)
    s.ellipse(top, 7.5, 6.0, 6.8, 2.2)
    s.pixels_at(ramp("#a8641e")[BASE], [(4, 5), (7, 6), (10, 5), (12, 6), (6, 7)])  # Browned bubbles.
    return s


def crushed_blackcurrants_hand():
    # Crushed blackcurrants: a dark purple-black mash of berries and juice, in a bowl.
    return bowl("#3a1a3a", mound=1.0, flecks=[("#6a2a5a", 0.16)])


def crushed_ice_hand():
    # Crushed ice: a heap of clear, glinting blue-white chips.
    s = Sprite()
    ice = plain_ramp("#d8ecf4")
    rng = random.Random(83)
    for cx, cy in [(4, 13), (7, 13), (10, 13), (13, 13), (5, 10), (8, 10), (11, 10), (6, 7), (9, 7), (7, 4)]:
        cx += rng.choice((0, 1))
        s.pixels_at(ice[SHADOW], [(cx, cy + 1), (cx + 1, cy + 1)])
        s.pixels_at(ice[BASE], [(cx, cy), (cx + 1, cy)])
        s.pixels_at((255, 255, 255), [(cx, cy - 1)])
        s.pixels_at(ice[OUTLINE], [(cx + 2, cy + 1), (cx - 1, cy + 1)])
    return s


def crushed_red_grapes_hand():
    # Crushed red grapes: a deep red-purple must full of broken skins, in a bowl.
    return bowl("#5a1a3a", mound=1.0, flecks=[("#8a2a5a", 0.14), ("#3a8a3a", 0.02)])


def crushed_white_grapes_hand():
    # Crushed white grapes: a pale green-gold must full of broken skins, in a bowl.
    return bowl("#c8d080", mound=1.0, flecks=[("#a8b860", 0.14)])


def crustless_bread_hand():
    # Crustless bread: two soft white slices with the crusts cut off, square and even.
    s = Sprite()
    crumb = plain_ramp("#f4eedc")
    for x0, y0 in [(1, 2), (6, 7)]:
        s.shape(lambda x, y, x0=x0, y0=y0: x0 <= x <= x0 + 8 and y0 <= y <= y0 + 7, crumb, (x0 + 4, y0 + 3.5), (4.0, 3.5))
        s.pixels_at(crumb[SHADOW], [(x0 + 3, y0 + 3), (x0 + 6, y0 + 5), (x0 + 2, y0 + 5)])  # The open crumb.
    return s


def cubed_tofu_hand():
    # Cubed tofu: neat, soft, creamy white cubes.
    return chopped("#f2f0e4", size=1.5, plain=True, seed=89)


def cucumber_relish_hand():
    # Cucumber relish: diced cucumber and shallot with slivers of red chili in a sweet vinegar, in a bowl.
    return bowl("#c8dca0", plain=True, flecks=[("#4a8a34", 0.16), ("#d8a8b0", 0.06), ("#d02a2a", 0.05)])


def curds_hand():
    # Curds: soft white lumps of set milk sitting in pale yellow whey, in a clay bowl.
    s = bowl("#e8e4b8", plain=True, bowl_colour=CLAY)
    curd = plain_ramp("#f8f6ee")
    for x, y in [(4, 7), (7, 6), (10, 7), (6, 8), (9, 8)]:
        s.pixels_at(curd[BASE], [(x, y), (x + 1, y)])
        s.put(x, y - 1, curd[HIGHLIGHT])
    return s

def cured_pork_belly_hand():
    # Cured pork belly: a slab of layered pink meat and white fat, firm from its dry cure, crusted with salt and sugar.
    s = Sprite()
    meat, fat = ramp("#c86a6a", spread=0.6), plain_ramp("#f2e8dc")
    s.shape(lambda x, y: 1 <= x <= 14 and 4 <= y <= 12, meat, (7.5, 8.0), (7.0, 4.0))
    for y in (6, 9):
        s.pixels_at(fat[BASE], [(x, y) for x in range(2, 14)])
    s.pixels_at(plain_ramp("#e8d8c4")[BASE], [(x, 5) for x in range(2, 14)])  # The rind.
    s.pixels_at(plain_ramp("#fafaf6")[BASE], [(3, 7), (7, 8), (11, 7), (5, 10), (10, 11)])  # Salt.
    return s


def cured_pork_shoulder_hand():
    # Cured pork shoulder: a deep rose-pink joint, darkened by the cure, with a rim of white fat and a dusting of salt.
    s = slab("#b84a4e", fat="#f0e6d6", rx=6.4, ry=5.0, angle=-10, fat_side=1.2, seed=97)
    s.pixels_at(plain_ramp("#fafaf6")[BASE], [(4, 6), (8, 5), (10, 8), (6, 9)])
    return s


def cured_salmon_hand():
    # Cured salmon: thin, glossy, deep orange-pink slices with pale fat lines, edged with a crust of chopped dill.
    s = Sprite()
    fish_ = plain_ramp("#f0946a")
    for start, end in [((2.0, 4.6), (12.6, 2.4)), ((2.6, 9.4), (13.6, 7.2)), ((3.0, 14.0), (14.0, 11.8))]:
        s.capsule(fish_, [start, end], (1.8, 1.6))
        length = math.dist(start, end)
        for t in range(2, int(length) - 1, 3):
            x = start[0] + (end[0] - start[0]) * t / length
            y = start[1] + (end[1] - start[1]) * t / length
            s.put(round(x - 0.5), round(y - 0.5), plain_ramp("#f8c8b0")[BASE])  # The fat lines.
        s.pixels_at(ramp("#3a8a34")[BASE], [(round(start[0]), round(start[1]) + 1), (round(start[0]) + 2, round(start[1]) + 1)])  # Dill.
    return s


def curried_fish_paste_hand():
    # Curried fish paste: white fish pounded with red curry paste into a thick orange-red paste, in a bowl.
    return bowl("#d85a2a", mound=1.6, texture=bumpy, flecks=[("#f0a070", 0.08)])


def curry_ketchup_hand():
    # Curry ketchup: a smooth, glossy, bright red-orange sauce, in a bowl.
    s = bowl("#d83a1e")
    s.pixels_at(ramp("#f08a4a")[BASE], [(4, 7), (5, 7), (6, 7)])
    return s


def curry_ketchup_base_hand():
    # Curry ketchup base: tomatoes, vinegar and curry powder cooked down to a thick, chunky, darker red, in a bowl.
    return bowl("#a8301c", mound=1.0, texture=bumpy, flecks=[("#d0a030", 0.06)])


def curry_powder_hand():
    # Curry powder: a warm golden-yellow powder.
    return spice_bowl("#d8a02a", fleck="#a8601e", seed=101)


def curry_spice_mix_hand():
    # Curry spice mix: the whole spices blended before toasting, a coarse ochre mix flecked with red, brown and green.
    s = spice_bowl("#c89a3a", seed=103)
    for (x, y), colour in [((4, 6), "#b8321e"), ((7, 5), "#5a3a24"), ((10, 6), "#7a8a4a"), ((6, 7), "#3a3a3a"), ((9, 7), "#b8321e")]:
        s.put(x, y, ramp(colour)[BASE])
    return s


def currywurst_hand():
    # Currywurst: a sliced bratwurst under curry ketchup dusted with curry powder, with fried potatoes, on a plate.
    s = plate()
    fries = plain_ramp("#f0cc70")
    for x0, y0, x1, y1 in [(10, 13, 14, 9), (11, 14, 15, 10), (12, 14, 15, 12)]:
        s.line(fries[BASE], x0, y0, x1, y1)
    sauce = ramp("#d83a1e", spread=0.5)
    s.ellipse(sauce, 6.4, 10.4, 5.4, 2.6)
    meat = ramp("#c8844a", spread=0.6)
    for cx in (3.4, 6.0, 8.6):
        s.ellipse(meat, cx, 9.6, 1.3, 1.1)
    s.pixels_at(ramp("#e8b030")[BASE], [(4, 9), (7, 8), (9, 10), (5, 11)])  # Curry powder.
    return s


def daifuku_mochi_hand():
    # Daifuku: soft, round, white rice-cake balls dusted with starch, one cut to show the dark bean paste and strawberry.
    s = Sprite()
    mochi = plain_ramp("#f8f6f0")
    s.ellipse(mochi, 4.8, 6.0, 4.0, 3.6)
    s.put(3, 4, mochi[HIGHLIGHT])
    s.ellipse(mochi, 10.6, 10.6, 4.4, 4.0)
    s.ellipse(ramp("#5a2a2a", spread=0.5), 10.6, 10.8, 3.0, 2.6, outline=False)
    s.ellipse(ramp("#d02a3a", spread=0.5), 10.6, 10.2, 1.6, 1.4, outline=False)
    s.put(10, 9, plain_ramp("#f8d8d8")[BASE])
    return s


def dango_dough_hand():
    # Dango dough: a smooth, bright white ball of rice flour dough.
    return dough_ball("#f4f2ec", flour=False)


def desiccated_coconut_hand():
    # Desiccated coconut: fine, dry, snow-white shreds, in a clay bowl so they show.
    return spice_bowl("#f8f6f0", fleck="#e4e0d4", seed=107, bowl_colour=CLAY)


def diced_aubergine_hand():
    # Diced aubergine: small creamy cubes, some with a strip of glossy purple skin.
    return chopped("#ece0b8", skin="#4a2a5a", size=1.2, seed=109, plain=True)


def diced_beef_hand():
    # Diced beef: neat red cubes of chuck.
    return chopped("#b03a34", size=1.2, seed=113)

def diced_bell_pepper_hand():
    # Diced bell pepper: small, glossy red squares.
    return chopped("#d8281e", size=1.2, seed=127)


def diced_courgette_hand():
    # Diced courgette: pale green-white cubes edged with dark green skin.
    return chopped("#dce8b8", skin="#2e6a2a", size=1.2, seed=131, plain=True)


def diced_nopales_hand():
    # Diced nopales: bright green squares of cactus paddle, a few with pale spine marks.
    s = chopped("#5aa040", size=1.2, seed=137)
    s.pixels_at(plain_ramp("#e8ecc8")[BASE], [(3, 3), (12, 4), (7, 8), (4, 12)])
    return s


def diced_potato_hand():
    # Diced potato: neat, pale yellow cubes.
    return chopped("#ecdc98", size=1.2, seed=139, plain=True)


def diced_water_chestnut_hand():
    # Diced water chestnut: crisp white cubes, a few with a sliver of dark brown skin.
    return chopped("#f2f0e2", skin="#5a3a28", size=1.2, seed=149, plain=True)


def diced_white_fish_hand():
    # Diced white fish: soft, translucent white chunks, showing the grain of the flakes.
    s = chopped("#eeeae4", size=1.4, seed=151, plain=True)
    s.pixels_at(plain_ramp("#eeeae4")[SHADOW], [(4, 4), (9, 3), (12, 5), (3, 9), (7, 8), (11, 9), (5, 13), (9, 12)])
    return s


def dimensional_anchor_hand():
    # The dimensional anchor: a heavy block of obsidian bound with iron at the corners, an amethyst crystal set glowing
    # in its heart.
    s = Sprite()
    stone = ramp("#2a1e3a", spread=0.6)
    s.shape(lambda x, y: 1 <= x <= 14 and 1 <= y <= 14, stone, (7.5, 7.5), (6.5, 6.5),
            texture=lambda x, y: 1 if (x * 7 + y * 3) % 11 == 0 else 0)
    iron = plain_ramp("#d8dce2")
    for cx, cy in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        s.pixels_at(iron[BASE], [(cx, cy), (cx + (1 if cx < 7 else -1), cy), (cx, cy + (1 if cy < 7 else -1))])
        s.put(cx, cy, iron[HIGHLIGHT])
    gem = ramp("#a86ae0", spread=0.6)
    s.shape(lambda x, y: abs(x - 7.5) + abs(y - 7.5) <= 3.6, gem, (7.5, 7.5), (3.6, 3.6))
    s.pixels_at(plain_ramp("#f0d8ff")[BASE], [(6, 6), (7, 5)])
    s.pixels_at(gem[LIGHT], [(7, 1), (1, 7), (14, 8), (8, 14)])  # Light leaking from the seams.
    return s


def dimensional_folding_core_hand():
    # The dimensional folding core: a copper ring holding a swirling teal pearl of folded space, amethyst shards at
    # its four points.
    s = Sprite()
    copper = ramp("#c8703a", spread=0.6)
    s.ellipse(copper, 7.5, 7.5, 7.0, 7.0)
    s.ellipse(ramp("#1e3a3a", spread=0.4), 7.5, 7.5, 5.0, 5.0, outline=False)
    pearl = ramp("#2aa898", spread=0.6)
    s.ellipse(pearl, 7.5, 7.5, 3.6, 3.6)
    for x, y in [(6, 6), (7, 7), (8, 8), (9, 7), (6, 9)]:
        s.put(x, y, ramp("#7ae8d0")[BASE])  # The swirl.
    s.put(6, 5, (240, 255, 250))
    gem = ramp("#a86ae0", spread=0.6)
    for x, y in [(7, 0), (0, 7), (14, 7), (7, 14)]:
        s.pixels_at(gem[BASE], [(x, y), (x + 1, y) if y in (0, 14) else (x, y + 1)])
    return s


def dimensional_logic_core_hand():
    # The dimensional logic core: a copper plate etched with glowing redstone traces, an amethyst gem at the centre.
    s = Sprite()
    copper = ramp("#c8703a", spread=0.6)
    s.shape(lambda x, y: 1 <= x <= 14 and 1 <= y <= 14 and not ((x in (1, 14)) and (y in (1, 14))), copper, (7.5, 7.5), (6.5, 6.5))
    trace = ramp("#e02a1e", spread=0.5)
    for points in [[(3, 3), (3, 6), (6, 6)], [(12, 3), (9, 3), (9, 6)], [(3, 12), (6, 12), (6, 9)], [(12, 12), (12, 9), (9, 9)]]:
        for (x0, y0), (x1, y1) in zip(points, points[1:]):
            s.line(trace[BASE], x0, y0, x1, y1)
        s.put(*points[0], ramp("#ff7a5a")[BASE])
    gem = ramp("#a86ae0", spread=0.6)
    s.ellipse(gem, 7.5, 7.5, 2.2, 2.2)
    s.put(7, 6, plain_ramp("#f0d8ff")[BASE])
    return s


def dipped_bread_roll_hand():
    # A bread roll dipped in red chili sauce: soaked through and glossy deep red, with a little of the pale crumb showing.
    s = Sprite()
    roll = ramp("#b0301e", spread=0.6)
    s.ellipse(roll, 7.5, 9.0, 6.4, 5.0)
    s.pixels_at(ramp("#e86a3a")[BASE], [(4, 6), (5, 5), (6, 5)])
    s.pixels_at(plain_ramp("#e8c890")[BASE], [(10, 11), (11, 10)])
    s.pixels_at(roll[BASE], [(9, 14), (9, 15)])  # A drip.
    return s


def pretzel(dough, salt=True, s=None):
    """A pretzel: a rope of dough knotted into two loops over a crossed twist, sprinkled with coarse salt."""
    s = s or Sprite()
    tones = ramp(dough, spread=0.6)
    for cx in (4.8, 10.2):
        ring = [(cx + 3.6 * math.cos(math.radians(a)), 7.0 + 4.0 * math.sin(math.radians(a))) for a in range(0, 361, 30)]
        s.capsule(tones, ring, (1.2, 1.2))
    s.capsule(tones, [(1.6, 13.6), (7.5, 9.0), (13.4, 13.6)], (1.2, 1.2))  # The twisted arms.
    s.pixels_at(tones[HIGHLIGHT], [(3, 4), (9, 4), (12, 4)])
    if salt:
        s.pixels_at(plain_ramp("#fafaf6")[BASE], [(5, 3), (11, 3), (2, 7), (13, 7), (7, 10), (4, 12), (11, 12)])
    return s


def dipped_pretzels_hand():
    # Dipped pretzels: shaped raw dough dipped in lye and salted, pale and slick, not yet baked brown.
    return pretzel("#e2cc9a")


def dipped_tortillas_hand():
    # Dipped tortillas: corn tortillas warmed in green salsa, soft and coated green.
    s = corn_tortilla_hand()
    sauce = ramp("#5a9a3a", spread=0.6)
    s.ellipse(sauce, 7.5, 7.6, 5.6, 2.6, outline=False)
    s.pixels_at(sauce[LIGHT], [(5, 7), (6, 6), (9, 7)])
    return s


def dorayaki_pancake(s, cx, cy):
    tones = ramp("#b8702a", spread=0.6)
    s.ellipse(tones, cx, cy, 5.6, 2.4)
    s.ellipse(ramp("#d89a4a", spread=0.5), cx, cy - 0.3, 4.0, 1.4, outline=False)
    return s


def dorayaki_hand():
    # Dorayaki: two round, golden-brown honey pancakes sandwiching a thick layer of dark red bean paste.
    s = Sprite()
    pancake = plain_ramp("#e8c078")
    s.ellipse(pancake, 7.5, 11.0, 6.6, 2.6)
    s.ellipse(ramp("#5a2a2a", spread=0.6), 7.5, 9.0, 6.0, 1.8)
    s.ellipse(ramp("#b8702a", spread=0.6), 7.5, 6.6, 6.6, 3.0)
    s.ellipse(ramp("#d08a3a", spread=0.5), 7.5, 6.2, 4.8, 1.8, outline=False)
    s.put(5, 5, ramp("#e8b060")[BASE])
    return s


def dorayaki_batter_hand():
    # Dorayaki batter: egg, sugar, honey and flour whisked into a smooth, golden batter, in a bowl.
    s = bowl("#ecc060", plain=True)
    s.pixels_at(plain_ramp("#f8dc90")[BASE], [(4, 7), (5, 7)])
    return s


def dorayaki_pancakes_hand():
    # Dorayaki pancakes: a stack of small, round, evenly golden-brown pancakes.
    s = Sprite()
    for cy in (11.6, 8.6, 5.6):
        dorayaki_pancake(s, 7.5, cy)
    return s

def hook(s):
    """A butcher's hook at the top of the icon."""
    steel = plain_ramp("#a8acb4")
    s.pixels_at(steel[BASE], [(7, 0), (8, 0), (8, 1)])
    s.pixels_at(steel[SHADOW], [(7, 1)])
    return s


def hung_beast(coat, width=1.0, texture=None, dressed=False, fat=None, head=None, ears=False):
    """A four-legged animal hung head down from a hook by its hind legs. Undressed it wears its coat and has its head and
    front legs; dressed it is skinned and cleaned, the head and lower legs gone, showing meat with fat along the back."""
    s = hook(Sprite())
    tones = plain_ramp(coat) if not dressed else ramp(coat, spread=0.6)
    leg = tones
    s.capsule(leg, [(7.0, 2.4), (5.0 - width, 5.0)], (0.8, 0.9))
    s.capsule(leg, [(8.4, 2.4), (10.0 + width, 5.0)], (0.8, 0.9))
    rx = 2.6 + 1.8 * width
    s.shape(lambda x, y: ((x - 7.5) / rx) ** 2 + ((y - 8.6) / 4.6) ** 2 <= 1, tones, (7.5, 8.6), (rx, 4.6), texture=texture)
    if dressed:
        if fat:
            fat_tones = plain_ramp(fat)
            for y in range(5, 13):
                for x in range(16):
                    if s.pixels[y][x] in tones[1:] and (s.pixels[y][x - 1] in (None, tones[OUTLINE])):
                        s.pixels_at(fat_tones[BASE], [(x, y)])
            for y in (7, 9, 11):
                s.line(fat_tones[SHADOW], 8, y, round(7.5 + rx) - 2, y + 1)  # The ribs.
        s.capsule(tones, [(6.0, 12.0), (5.6, 14.0)], (0.9, 0.7))  # The cut stumps of the front legs.
        s.capsule(tones, [(9.0, 12.0), (9.4, 14.0)], (0.9, 0.7))
    else:
        s.capsule(leg, [(5.4, 11.6), (4.6, 15.0)], (0.9, 0.8))
        s.capsule(leg, [(9.6, 11.6), (10.4, 15.0)], (0.9, 0.8))
        face = plain_ramp(head or coat)
        s.ellipse(face, 7.5, 13.4, 1.8, 2.0)
        s.put(7, 14, (30, 24, 24))
        if ears:  # Long ears hanging down past the head.
            s.pixels_at(face[SHADOW], [(6, 15), (9, 15)])
    s.put(5, 7, tones[HIGHLIGHT])
    return s


def hung_bird(feathers, dressed=False, head=None, bill=None, wing=None):
    """A bird hung head down from a hook by its feet: in its feathers with its head on, or plucked and cleaned."""
    s = hook(Sprite())
    foot = plain_ramp("#e8b850")
    s.pixels_at(foot[BASE], [(7, 2), (8, 2), (6, 3), (9, 3)])
    tones = plain_ramp(feathers)
    s.ellipse(tones, 7.5, 8.0, 4.8, 4.2, texture=None if dressed else (lambda x, y: -1 if (x + y * 2) % 5 == 0 else 0))
    if wing and not dressed:
        wings = plain_ramp(wing)
        s.pixels_at(wings[BASE], [(3, 7), (3, 8), (4, 9), (12, 7), (12, 8), (11, 9)])
    if dressed:
        s.pixels_at(tones[SHADOW], [(5, 10), (9, 10), (7, 11)])  # Pimpled, plucked skin.
        s.put(7, 12, tones[OUTLINE])  # The cleaned cavity.
    else:
        s.capsule(plain_ramp(head or feathers), [(7.5, 11.6), (7.5, 13.4)], (1.0, 1.4))
        if bill:
            s.pixels_at(plain_ramp(bill)[BASE], [(7, 15), (8, 15)])
        s.put(6, 13, (24, 20, 20))
    s.put(5, 6, tones[HIGHLIGHT])
    return s


def cow_carcass_hand():
    # A cow carcass: the whole animal, still in its brown and white hide, hung from a hook.
    s = hung_beast("#6a4a32", width=1.4, head="#5a3e2a")
    s.pixels_at(plain_ramp("#f2efe8")[BASE], [(4, 6), (5, 6), (4, 7), (9, 9), (10, 9), (10, 10), (11, 10), (6, 11), (7, 11), (6, 5)])
    return s


def dressed_cow_hand():
    # A dressed cow: skinned and cleaned, a big side of deep red beef with white fat along the back and pale ribs.
    return hung_beast("#a83430", width=1.4, dressed=True, fat="#f0e6d6")


def pig_carcass_hand():
    # A pig carcass: the whole pig, pink and bristly, hung from a hook.
    return hung_beast("#e8a8a0", width=1.2, texture=lambda x, y: -1 if (x * 3 + y * 5) % 7 == 0 else 0, head="#e09890")


def dressed_pig_hand():
    # A dressed pig: scalded smooth and scraped clean, pale pink skin on, opened and cleaned.
    return hung_beast("#f0c8b8", width=1.2, dressed=True, fat="#faf2ea")


def sheep_carcass_hand():
    # A sheep carcass: the whole sheep, in its thick, creamy wool, hung from a hook.
    return hung_beast("#ece6d6", width=1.3, texture=lambda x, y: -1 if (x + y) % 3 == 0 else 1 if (x * 2 + y) % 5 == 0 else 0, head="#3a3030")


def dressed_sheep_hand():
    # A dressed sheep: skinned, a pinkish-red lamb carcass with plenty of white fat.
    return hung_beast("#b84a44", width=1.1, dressed=True, fat="#f2eadc")


def goat_carcass_hand():
    # A goat carcass: the whole goat, in its rough grey-brown coat, hung from a hook.
    return hung_beast("#8a7a68", width=0.9, texture=lambda x, y: -1 if (x * 2 + y) % 4 == 0 else 0, head="#6a5a4a")


def dressed_goat_hand():
    # A dressed goat: skinned, a lean, dark red carcass with only a thin edge of fat.
    return hung_beast("#9a3a30", width=0.9, dressed=True, fat="#ece0cc")


def rabbit_carcass_hand():
    # A rabbit carcass: the whole rabbit, in its soft grey-brown fur, hung from a hook.
    return hung_beast("#9a8470", width=0.4, texture=lambda x, y: -1 if (x + y * 3) % 5 == 0 else 0, head="#8a7460", ears=True)


def dressed_rabbit_hand():
    # A dressed rabbit: skinned, a slim, pale pink-red carcass.
    return hung_beast("#d08880", width=0.4, dressed=True)


def chicken_carcass_hand():
    # A chicken carcass: the whole bird, in its white feathers, hung by its feet before it is scalded and plucked.
    return hung_bird("#f2f0ea", head="#e8e4dc", bill="#d02a2a")


def dressed_chicken_hand():
    # A dressed chicken: scalded, plucked and cleaned, plump with pale golden skin.
    return hung_bird("#f0d0a8", dressed=True)


def duck_carcass_hand():
    # A duck carcass: the whole duck, in brown feathers with a green head, hung by its feet.
    return hung_bird("#8a6a4a", head="#2a6a3a", bill="#e0a030", wing="#5a4a8a")


def dressed_duck_hand():
    # A dressed duck: plucked and cleaned, with creamy, fatty skin.
    s = hung_bird("#e2b884", dressed=True)
    s.pixels_at(plain_ramp("#f6ead0")[BASE], [(4, 8), (10, 7), (6, 9), (9, 10)])  # Its thick layer of fat.
    return s

def dried(name, saturation=0.6, lightness=0.85, shrivel=0.12, seed=1):
    """Another item's sprite dried: its colours duller and darker, with shrivelled creases scattered over it."""
    def drawn():
        s = SPRITES[name]()
        rng = random.Random(seed)
        for y in range(16):
            for x in range(16):
                colour = s.pixels[y][x]
                if colour is None:
                    continue
                h, l, sat = colorsys.rgb_to_hls(*(c / 255 for c in colour))
                l *= lightness
                if rng.random() < shrivel and 0.15 < l:
                    l *= 0.75  # A crease.
                s.put(x, y, tuple(round(c * 255) for c in colorsys.hls_to_rgb(h, l, sat * saturation)))
        return s
    return drawn


def drained_cream_hand():
    # Drained cream: crème fraîche hung in cloth until thick and firm, a dense white mound, in a clay bowl.
    s = bowl("#f6f2e4", mound=3.0, plain=True, bowl_colour=CLAY)
    s.pixels_at(plain_ramp("#f6f2e4")[SHADOW], [(5, 6), (8, 5), (10, 6)])  # The marks of the cloth.
    return s


def dried_apple_hand():
    # Dried apple rings: leathery, pale tan rings with a hole in the middle and a thin edge of red-brown skin.
    s = Sprite()
    for cx, cy in [(5.0, 5.6), (10.6, 10.4)]:
        ring = plain_ramp("#d8b878")
        s.ellipse(ramp("#8a3a2a", spread=0.5), cx, cy, 4.6, 4.0)
        s.ellipse(ring, cx, cy, 3.8, 3.2, outline=False)
        s.ellipse(ring, cx, cy, 1.4, 1.2)
        s.pixels_at([None], [(round(cx) - 1, round(cy) - 1), (round(cx), round(cy) - 1)])
        s.put(round(cx) - 2, round(cy) - 2, ring[HIGHLIGHT])
    for cx, cy in [(5.0, 5.6), (10.6, 10.4)]:
        for x, y in [(round(cx) - 1, round(cy) - 1), (round(cx), round(cy) - 1), (round(cx) - 1, round(cy)), (round(cx), round(cy))]:
            s.pixels[y][x] = None  # The hole where the core was.
    return s


def dried_berries_hand():
    # Dried berries: a mix of shrivelled red, purple-black and dark pink berries.
    s = Sprite()
    rng = random.Random(157)
    for (cx, cy), colour in zip([(4.0, 4.4), (8.6, 3.6), (12.4, 5.0), (3.4, 9.0), (7.4, 8.4), (11.4, 9.6), (5.0, 13.0), (9.4, 12.6), (13.0, 13.2)],
                                ["#8a1e2a", "#3a1e3a", "#a83a5a"] * 3):
        tones = ramp(colour, spread=0.6)
        s.ellipse(tones, cx, cy, 1.9, 1.7)
        s.put(round(cx) - 1, round(cy) - 1, tones[HIGHLIGHT])
        s.put(round(cx) + rng.choice((0, 1)), round(cy), tones[SHADOW])  # Wrinkles.
    return s


def dried_bread_hand():
    # Dried bread: two hard, pale, stale slices, the crust darker and the crumb dry and open.
    s = Sprite()
    crust, crumb = plain_ramp("#b8803a"), plain_ramp("#e8d4a4")
    for x0, y0 in [(1, 2), (6, 7)]:
        s.shape(lambda x, y, x0=x0, y0=y0: x0 <= x <= x0 + 8 and y0 <= y <= y0 + 7 and not (y == y0 and x in (x0, x0 + 8)), crust,
                (x0 + 4, y0 + 3.5), (4.0, 3.5))
        for y in range(y0 + 1, y0 + 7):
            for x in range(x0 + 1, x0 + 8):
                s.put(x, y, crumb[BASE] if (x + y) % 4 else crumb[SHADOW])
    return s

def dried_cocoa_beans_hand():
    # Dried cocoa beans: plump, almond-shaped beans with a papery, dusty purplish-brown shell.
    return bean_pile("#6a3a30", PILE, shine=ramp("#a87a6a")[BASE], spread=0.7)


def dried_fruit_hand():
    # Dried stone fruit: halves of apricot and plum dried to soft, wrinkled, glossy deep orange and dark red.
    s = Sprite()
    rng = random.Random(163)
    for (cx, cy), colour in [((4.6, 4.8), "#e07a1e"), ((11.0, 5.4), "#7a2a3a"), ((4.2, 11.0), "#7a2a3a"), ((10.6, 11.4), "#e07a1e")]:
        tones = ramp(colour, spread=0.6)
        s.ellipse(tones, cx, cy, 3.4, 2.8)
        for _ in range(3):
            s.put(round(cx + rng.uniform(-2, 2)), round(cy + rng.uniform(-1.4, 1.4)), tones[SHADOW])  # Wrinkles.
        s.put(round(cx) - 2, round(cy) - 1, tones[HIGHLIGHT])
    return s

def dried_peppercorns_hand():
    # Dried peppercorns: the green berries dried to hard, wrinkled black balls, loose in a little heap.
    s = Sprite()
    corn = ramp("#2e2a28", spread=0.5)
    rng = random.Random(167)
    for cx, cy in [(4.0, 12.4), (7.4, 12.8), (10.8, 12.4), (13.0, 10.4), (5.6, 9.4), (9.0, 9.6), (3.0, 8.4), (7.4, 6.6), (11.0, 7.2), (9.2, 4.0)]:
        s.ellipse(corn, cx, cy, 1.6, 1.5)
        s.put(round(cx) - 1, round(cy) - 1, ramp("#7a746e")[BASE])
        s.put(round(cx) + rng.choice((-1, 0)), round(cy), corn[OUTLINE])  # Wrinkles.
    return s

def dried_sweet_berries_hand():
    # Dried sweet berries: small, shrivelled, deep red berries, a few still on their stalks.
    s = Sprite()
    rng = random.Random(173)
    for cx, cy in [(4.0, 4.4), (8.6, 3.6), (12.4, 5.0), (3.4, 9.0), (7.4, 8.4), (11.4, 9.6), (5.0, 13.0), (9.4, 12.6), (13.0, 13.2)]:
        tones = ramp("#8a1e24", spread=0.6)
        s.ellipse(tones, cx, cy, 1.9, 1.7)
        s.put(round(cx) - 1, round(cy) - 1, tones[HIGHLIGHT])
        s.put(round(cx) + rng.choice((0, 1)), round(cy), tones[SHADOW])
    s.pixels_at(ramp("#5a4a2a")[BASE], [(8, 1), (12, 2)])
    return s


def duck_bones_hand():
    # Duck bones: a long breastbone keel and two leg bones, creamy and a little greyer than chicken's.
    s = Sprite()
    bone = plain_ramp("#e2d8c0")
    s.capsule(bone, [(2.0, 4.0), (8.0, 2.0), (13.6, 4.4)], (1.4, 1.0))  # The keel.
    for start, end in [((3.0, 14.0), (8.0, 7.0)), ((8.4, 14.6), (13.6, 8.0))]:
        s.capsule(bone, [start, end], (1.3, 1.3))
        for x, y in (start, end):
            s.ellipse(bone, x, y, 1.7, 1.6)
    return s


def duck_breasts_hand():
    # Duck breasts: two dark red fillets under a thick cap of creamy fat, the skin scored in a diamond pattern.
    s = Sprite()
    for cx, cy in [(5.2, 5.4), (10.6, 10.6)]:
        meat, fat = ramp("#8a2a2a", spread=0.6), plain_ramp("#f0dcb4")
        s.ellipse(meat, cx, cy, 4.6, 3.2, angle=-20)
        s.ellipse(fat, cx - 0.4, cy - 0.6, 3.8, 2.2, angle=-20, outline=False)
        for k in (-2, 0, 2):
            s.put(round(cx + k), round(cy - 1 + (k % 4 == 0)), fat[SHADOW])  # The scoring.
    return s


def duck_confit_hand():
    # Duck confit: legs slow-cooked in their own fat, the skin crisp and deep golden brown.
    s = drumstick("#b06e2a", 4.6, 4.6, 35, Sprite())
    s = drumstick("#b06e2a", 8.4, 9.6, 15, s)
    s.pixels_at(ramp("#e8b860")[BASE], [(3, 3), (7, 8)])
    return s


def duck_fat_hand():
    # Duck fat: rendered and set, a smooth, pale creamy-yellow fat, in a clay bowl.
    s = bowl("#f2e2a8", mound=0.6, plain=True, bowl_colour=CLAY)
    s.pixels_at(plain_ramp("#faf2d0")[BASE], [(4, 7), (5, 7), (6, 7)])
    return s

def duck_heart_and_gizzard_hand():
    # A duck's heart and gizzard: a small dark red heart and a plump, dark gizzard with its pale silver skin.
    s = heart("#8a2a30", cx=5.0, cy=6.0, size=0.6)
    gizzard = ramp("#8a3a34", spread=0.6)
    s.ellipse(gizzard, 10.4, 10.6, 4.0, 3.2)
    s.pixels_at(plain_ramp("#e8d8c0")[BASE], [(9, 10), (10, 10), (11, 11), (10, 11)])
    return s


def duck_legs_hand():
    # Raw duck legs: dark red meat under thick, pale, fatty skin, tapering to the knuckle.
    s = drumstick("#e8c0a0", 4.6, 4.6, 35, Sprite())
    s = drumstick("#e8c0a0", 8.4, 9.6, 15, s)
    s.pixels_at(ramp("#9a3a34")[BASE], [(6, 7), (10, 11)])  # Meat showing at the cut edge.
    return s


def duck_liver_hand():
    # Duck liver: a smooth, plump, glossy lobe, paler and pinker than other livers.
    return liver("#b0605a")


def duck_spawn_egg_hand():
    # The duck spawn egg: brown, speckled green and yellow, like a mallard.
    s = Sprite()
    shell = ramp("#8a6a4a", spread=0.6)
    s.shape(lambda x, y: ((x - 7.5) / 5.2) ** 2 + ((y - 8.6) / 6.6) ** 2 + 0.08 * (8.6 - y) / 6.6 <= 1, shell, (7.5, 8.6), (5.2, 6.6))
    for (x, y), colour in [((5, 5), "#2a7a3a"), ((9, 4), "#2a7a3a"), ((10, 8), "#e0b030"), ((5, 10), "#2a7a3a"), ((8, 12), "#e0b030"),
                           ((7, 7), "#e0b030"), ((11, 11), "#2a7a3a")]:
        s.pixels_at(ramp(colour)[BASE], [(x, y), (x + 1, y)])
    s.pixels_at(shell[HIGHLIGHT], [(5, 3), (4, 4)])
    return s


def duck_wings_hand():
    # Raw duck wings: jointed wings in pale, fatty skin with a pointed tip.
    s = Sprite()
    skin = plain_ramp("#ecc8a8")
    for (ax, ay), (bx, by), (tx, ty) in [((2.0, 6.0), (6.6, 4.0), (10.0, 1.4)), ((5.0, 13.0), (10.0, 10.6), (14.4, 7.0))]:
        s.capsule(skin, [(ax, ay), (bx, by)], (2.8, 2.4))
        s.capsule(skin, [(bx, by), (tx, ty)], (2.2, 0.8))
        s.put(round(bx - 0.5), round(by - 0.5), skin[SHADOW])
    return s


def elote_hand():
    # Elote: a charred corn cob on a stick, smeared with white crema, sprinkled with cotija, and dusted with red chili.
    s = SPRITES["charred_corn"]()
    s.pixels_at(plain_ramp("#f6f2e6")[BASE], [(7, 3), (8, 5), (7, 7), (8, 9), (7, 11)])
    s.pixels_at(ramp("#c8321e")[BASE], [(8, 4), (7, 6), (8, 8), (7, 10)])
    s.pixels_at(ramp("#9ac040")[BASE], [(12, 14), (13, 13)])  # A wedge of lime.
    return s


def empanada_dough_hand():
    # Empanada dough: a rested ball of rich, golden pastry dough.
    return dough_ball("#ecd09a")


def empanadas_de_carne_hand():
    # Empanadas de carne: golden, half-moon pastries with crimped, rope-twisted edges.
    s = Sprite()
    pastry = ramp("#d8963e", spread=0.6)
    for cx, cy in [(5.4, 5.0), (10.4, 11.4)]:
        s.shape(lambda x, y, cx=cx, cy=cy: y <= cy + 2.4 and ((x - cx) / 5.0) ** 2 + ((y - cy - 2.4) / 5.0) ** 2 <= 1, pastry,
                (cx, cy), (5.0, 3.6))
        for x in range(round(cx) - 4, round(cx) + 5, 2):
            s.put(x, round(cy) + 2, pastry[SHADOW])  # The crimping.
        s.put(round(cx) - 1, round(cy) - 2, pastry[HIGHLIGHT])
    return s


def enchiladas_verdes_hand():
    # Enchiladas verdes: rolled tortillas baked under green salsa, drizzled with crema and crumbled cheese, in a dish.
    s = Sprite()
    dish = ramp("#b8743e", spread=0.6)
    s.shape(lambda x, y: 1 <= x <= 14 and 5 <= y <= 13, dish, (7.5, 9.0), (7.0, 4.0))
    sauce = ramp("#5a9a3a", spread=0.6)
    for y in (6, 8, 10):
        s.capsule(sauce, [(2.6, y + 0.5), (12.6, y + 0.5)], (1.0, 1.0))
    s.pixels_at(plain_ramp("#f6f2e6")[BASE], [(3, 7), (5, 8), (7, 7), (9, 9), (11, 8), (4, 10), (8, 11)])
    return s


def feijoada_hand():
    # Feijoada: a rich, dark black-bean stew with chunks of pork and smoked sausage, in a clay bowl.
    return bowl("#2a2028", mound=1.0, bowl_colour=CLAY, flecks=[("#a8523a", 0.12), ("#d8a888", 0.06), ("#4a6a3a", 0.02)])


def feijoada_beans_hand():
    # Feijoada beans: black beans soaking with dried beef, onion and garlic, in a bowl.
    return bowl("#2a2230", mound=0.8, flecks=[("#6a2a20", 0.08), ("#f0e8d0", 0.06)])


def fermented_cocoa_beans_hand():
    # Fermented cocoa beans: beans turned purple-brown in the sweatbox, still sticky with patches of pale pulp.
    s = bean_pile("#7a3a3a", PILE, spread=0.7)
    s.pixels_at(plain_ramp("#f0e4c8")[BASE], [(5, 7), (11, 8), (4, 11), (9, 11), (12, 13)])
    return s


def fermented_coffee_cherries_hand():
    # Fermented coffee cherries: soft, darkened, mushy red-brown cherries with a sticky sheen.
    return dried("coffee_cherry", 0.75, 0.7, 0.08, 107)()


def fermented_red_grapes_hand():
    # Fermented red grapes: dark wine-red must, frothing with bubbles on top, in a crock.
    s = bowl("#4a1428", bowl_colour="#8a6a4a")
    s.pixels_at(plain_ramp("#d8a8b8")[BASE], [(4, 7), (6, 7), (9, 7), (11, 8), (7, 8)])  # Froth.
    return s


def fermented_shrimp_hand():
    # Fermented shrimp: a thick, salty, greyish-pink shrimp paste, in a crock.
    return bowl("#a8706a", mound=1.4, bowl_colour="#8a6a4a", flecks=[("#6a4a48", 0.1)])

def fermented_tapioca_starch_hand():
    # Fermented tapioca starch: wet, sour, bright white starch settled under a little cloudy water, in a clay bowl.
    s = bowl("#f2f2ec", plain=True, bowl_colour=CLAY)
    s.pixels_at(plain_ramp("#dce4e4")[BASE], [(4, 7), (8, 7), (11, 8)])  # Bubbles.
    return s


def fermenting_anchovies_hand():
    # Fermenting anchovies: little silver fish packed in salt and brown brine, in a crock.
    s = bowl("#8a5a2a", bowl_colour="#8a6a4a")
    s.pixels_at(plain_ramp("#c8d0d8")[BASE], [(3, 7), (4, 7), (5, 7), (8, 8), (9, 8), (10, 8)])
    s.pixels_at(plain_ramp("#f4f2ec")[BASE], [(7, 7), (12, 7)])  # Salt.
    return s


def fischbrotchen_hand():
    # Fischbrötchen: a split bread roll filled with a silver-skinned pickled herring fillet, onion rings and gherkin.
    s = Sprite()
    roll = ramp("#c88a3a", spread=0.6)
    s.ellipse(roll, 7.5, 11.4, 6.6, 2.6)
    s.ellipse(plain_ramp("#c8d0d8"), 7.5, 8.8, 6.4, 1.6)  # The herring.
    s.pixels_at(plain_ramp("#f0eadc")[BASE], [(4, 8), (6, 7), (9, 8), (11, 7)])  # Onion rings.
    s.pixels_at(ramp("#6a8a3a")[BASE], [(8, 7), (12, 8)])
    s.ellipse(roll, 7.5, 6.0, 6.4, 2.6)
    s.pixels_at(roll[HIGHLIGHT], [(4, 5), (5, 4)])
    return s


def fish_bones_hand():
    # Fish bones: a whole skeleton, a spine with fine ribs, the head at one end and the tail fan at the other.
    s = Sprite()
    bone = plain_ramp("#e2dccc")
    for t in range(1, 8):  # Fine ribs either side of the spine.
        x, y = 3 + t * 9 / 8, 12 - t * 9 / 8
        s.line(bone[SHADOW], round(x), round(y), round(x - 2), round(y - 2))
        s.line(bone[SHADOW], round(x), round(y), round(x + 2), round(y + 2))
    s.capsule(bone, [(3.0, 12.0), (12.0, 3.0)], (0.8, 0.8))
    s.ellipse(bone, 12.6, 2.6, 2.2, 1.8)
    s.put(13, 2, (30, 30, 34))
    s.pixels_at(bone[BASE], [(1, 13), (2, 14), (1, 11), (3, 14)])  # The tail.
    return s


def fish_guts_hand():
    # Fish guts: a slippery heap of dark red and grey innards.
    s = Sprite()
    for (cx, cy, rx, ry), colour in [((6.0, 9.0, 4.0, 3.0), "#7a2a30"), ((10.0, 10.4, 3.4, 2.6), "#8a7a80"), ((7.6, 12.0, 3.0, 2.0), "#a03a3a")]:
        tones = ramp(colour, spread=0.6)
        s.ellipse(tones, cx, cy, rx, ry)
        s.put(round(cx) - 1, round(cy) - 1, tones[HIGHLIGHT])
    return s


def fish_head_hand():
    # A fish head: cut behind the gills, silvery grey with a darker back, a round eye and an open mouth.
    s = Sprite()
    head = plain_ramp("#b8c0c8")
    s.shape(lambda x, y: ((x - 9.0) / 6.0) ** 2 + ((y - 8.0) / 5.4) ** 2 <= 1 and x >= 5.4 + abs(y - 8.0) * 0.6, head, (9.0, 8.0), (6.0, 5.4))
    s.pixels_at(ramp("#4a5a6a")[BASE], [(9, 4), (10, 4), (11, 4), (8, 5), (12, 5)])
    s.ellipse(plain_ramp("#f0e8c8"), 8.6, 7.4, 1.4, 1.4)
    s.put(8, 7, (20, 20, 24))
    s.pixels_at(ramp("#a84a4a")[BASE], [(14, 8), (13, 8), (14, 9)])  # The cut, red at the gills.
    s.pixels_at(head[OUTLINE], [(4, 9), (5, 9)])  # The mouth.
    return s


def fish_scales_hand():
    # Fish scales: a pile of small, thin, translucent silvery discs catching the light.
    s = Sprite()
    rng = random.Random(179)
    for _ in range(14):
        x, y = rng.randint(2, 13), rng.randint(4, 13)
        s.pixels_at(plain_ramp("#d8e0e8")[BASE], [(x, y), (x + 1, y)])
        s.put(x, y - 1, (248, 252, 255))
        s.put(x + 1, y + 1, plain_ramp("#a8b4c0")[BASE])
    return s


def fiskefrikadeller_hand():
    # Fiskefrikadeller: golden fried fish cakes with a dollop of yellow remoulade, on a plate.
    s = plate()
    cake = ramp("#c8903e", spread=0.6)
    for cx, cy in [(4.8, 9.6), (9.6, 8.8)]:
        s.ellipse(cake, cx, cy, 3.4, 2.4)
        s.put(round(cx) - 1, round(cy) - 1, cake[HIGHLIGHT])
    s.ellipse(plain_ramp("#ece08a"), 12.0, 12.0, 1.8, 1.0)
    s.put(4, 12, ramp("#9ac040")[BASE])
    return s


def fiskefrikadeller_fars_hand():
    # Fiskefrikadeller fars: minced white fish with onion, egg and dill, a pale, flecked mixture in a bowl.
    return bowl("#ece6d6", mound=1.6, texture=bumpy, plain=True, bowl_colour=CLAY, flecks=[("#4a8a3a", 0.08)])


def flaeskesteg_hand():
    # Flæskesteg: Danish roast pork with a thick layer of golden, blistered crackling scored into strips across the top.
    s = Sprite()
    meat, fat = ramp("#c8806a", spread=0.6), plain_ramp("#f0dcc0")
    s.shape(lambda x, y: 1 <= x <= 14 and 6 <= y <= 13, meat, (7.5, 9.5), (7.0, 3.5))
    s.pixels_at(fat[BASE], [(x, 9) for x in range(2, 14)])
    crackling = ramp("#d89a3a", spread=0.6)
    s.shape(lambda x, y: 1 <= x <= 14 and 3 <= y <= 8, crackling, (7.5, 5.5), (7.0, 2.5),
            texture=lambda x, y: 1 if (x * 3 + y) % 4 == 0 else 0)
    for x in range(3, 14, 3):
        s.line(crackling[OUTLINE], x, 3, x, 7)  # The scoring.
    s.pixels_at(ramp("#5a7a3a")[BASE], [(7, 2), (8, 2)])  # A bay leaf tucked in.
    return s


def flaeskesteg_med_rodkal_hand():
    # Flæskesteg med rødkål: slices of roast pork with crackling, purple red cabbage, boiled potatoes and brown gravy.
    s = plate()
    s.ellipse(ramp("#6a2a4a", spread=0.5), 11.0, 11.0, 3.4, 1.8)  # Red cabbage.
    potato = plain_ramp("#ecd890")
    s.ellipse(potato, 3.6, 11.4, 2.0, 1.4)
    s.ellipse(ramp("#7a4a2a", spread=0.5), 7.0, 12.2, 2.6, 1.2)  # Gravy.
    meat = ramp("#c8806a", spread=0.6)
    s.shape(lambda x, y: 2 <= x <= 10 and 6 <= y <= 9, meat, (6.0, 7.5), (4.0, 1.5))
    s.pixels_at(ramp("#d89a3a")[LIGHT], [(x, 6) for x in range(3, 10)])  # The crackling.
    return s


def flammkuchen_hand():
    # Flammkuchen: a thin, crisp, oblong tart spread with white cream, scattered with bacon and onion, charred at the edges.
    s = Sprite()
    crust = ramp("#c8904a", spread=0.6)
    s.shape(lambda x, y: 1 <= x <= 14 and 3 <= y <= 12 and not ((x in (1, 14)) and (y in (3, 12))), crust, (7.5, 7.5), (7.0, 4.5))
    cream = plain_ramp("#f2ecd8")
    for y in range(5, 11):
        for x in range(3, 13):
            s.put(x, y, cream[BASE])
    s.pixels_at(ramp("#b84a3a")[BASE], [(4, 5), (8, 6), (11, 5), (5, 9), (10, 10), (7, 8)])  # Bacon.
    s.pixels_at(plain_ramp("#e8dcb8")[SHADOW], [(6, 5), (9, 8), (3, 8), (12, 8)])  # Onion.
    s.pixels_at(ramp("#3a2a1e")[BASE], [(1, 6), (14, 9), (5, 12), (10, 3)])  # Char.
    return s


def flan_custard_hand():
    # Flan custard: a smooth, rich, pale golden custard mixture, in a bowl.
    return bowl("#f0d888", plain=True, flecks=[("#2a2018", 0.03)])


def flan_napolitano_hand():
    # Flan napolitano: a tall, smooth, golden custard turned out onto a plate, glossy caramel pooling over and around it.
    s = plate()
    custard = plain_ramp("#f0cc6a")
    s.shape(lambda x, y: 3 <= x <= 12 and 4 <= y <= 11, custard, (7.5, 7.5), (4.5, 3.5))
    caramel = ramp("#a8561e", spread=0.6)
    s.ellipse(caramel, 7.5, 4.4, 4.6, 1.6)
    s.pixels_at(caramel[BASE], [(3, 6), (3, 7), (12, 6), (12, 8), (8, 6)])  # Drips.
    s.pixels_at(caramel[SHADOW], [(2, 12), (13, 12), (5, 13), (10, 13)])  # The pool.
    s.pixels_at(custard[HIGHLIGHT], [(4, 7), (4, 8)])
    return s


def flatbread_dough_hand():
    # Flatbread dough: a soft, smooth, oiled ball of plain dough.
    s = dough_ball("#ecd8b0", flour=False)
    s.pixels_at(plain_ramp("#f8ecc8")[BASE], [(5, 6), (6, 6)])
    return s

def french_onion_soup_hand():
    # French onion soup: a deep crock bowl of dark onion broth under a slice of bread and bubbling, browned cheese.
    s = Sprite()
    crock = ramp("#7a5a8a", spread=0.6)
    s.shape(lambda x, y: y >= 7 and ((x - 7.5) / 6.6) ** 2 + ((y - 7.0) / 7.6) ** 2 <= 1, crock, (7.5, 10.5), (6.6, 4.6))
    cheese = ramp("#e8b04a", spread=0.6)
    s.ellipse(cheese, 7.5, 6.6, 7.0, 2.8)
    s.pixels_at(ramp("#9a5a1e")[BASE], [(4, 6), (7, 5), (10, 6), (6, 7), (12, 7)])  # Browned spots.
    s.pixels_at(cheese[LIGHT], [(2, 8), (2, 9), (13, 8)])  # Cheese melting over the rim.
    return s


def fresh_cheese_hand():
    # Fresh cheese: a small, soft, pressed round of bright white cheese, marked by its mould.
    s = Sprite()
    cheese = plain_ramp("#f6f4ec")
    s.ellipse(cheese, 7.5, 10.0, 6.6, 3.0)
    s.shape(lambda x, y: 1 <= x <= 14 and 6 <= y <= 10, cheese, (7.5, 8.0), (6.6, 2.0))
    s.ellipse(plain_ramp("#fbfaf6"), 7.5, 6.4, 6.6, 2.6)
    for x in range(2, 14, 2):
        s.put(x, 9, cheese[SHADOW])  # The basket marks.
    return s


def fresh_chorizo_hand():
    # Fresh chorizo: soft, raw links of bright orange-red sausage, twisted between.
    s = Sprite()
    for (sx, sy), (ex, ey) in [((2.4, 5.0), (6.4, 3.4)), ((9.0, 3.4), (13.0, 5.0)), ((2.4, 12.0), (6.4, 10.4)), ((9.0, 10.4), (13.0, 12.0))]:
        sausage("#d8502e", (sx, sy), (ex, ey), radius=2.0, s=s)
    s.pixels_at(plain_ramp("#d8c8a8")[SHADOW], [(7, 4), (8, 4), (7, 11), (8, 11)])
    return s


def fresh_empanada_dough_hand():
    # Fresh empanada dough: just kneaded, soft and pale, before it rests.
    return dough_ball("#f0dcae")


def fresh_glass_noodles_hand():
    # Fresh glass noodles: a nest of thin, clear, glassy grey-white strands.
    return noodles("#dcdee2")


def fresh_panko_hand():
    # Fresh panko: big, light, jagged white flakes torn from crustless bread, in a clay bowl.
    return spice_bowl("#f6f2e6", fleck="#e8dcc4", seed=181, bowl_colour=CLAY)


def fresh_rice_noodles_hand():
    # Fresh rice noodles: a soft pile of wide, flat, opaque white ribbons.
    s = Sprite()
    noodle = plain_ramp("#f4f2ea")
    for y0 in (4, 7, 10, 13):
        s.capsule(noodle, [(1.6, y0 + 0.5), (5.0, y0 - 0.5), (9.0, y0 + 0.8), (14.0, y0 - 0.2)], (1.2, 1.2))
    return s


def fried_bratwurst_hand():
    # Fried bratwurst: two plump sausages fried a deep golden brown, glossy and lightly split.
    s = sausage("#94521e", (2.4, 6.6), (12.0, 3.0), radius=2.7)
    s = sausage("#94521e", (4.0, 13.4), (13.6, 9.8), radius=2.7, s=s)
    s.pixels_at(plain_ramp("#e8c8a0")[BASE], [(6, 4), (9, 11)])  # Split skins.
    return s


def fried_bread_roll_hand():
    # A fried bread roll: the chili-soaked roll fried until its red coat is dark and crisp.
    s = SPRITES["dipped_bread_roll"]()
    for x in range(16):
        for y in range(16):
            if s.pixels[y][x] is not None and (x * 3 + y) % 5 == 0:
                s.put(x, y, (90, 30, 20))
    return s


def fried_chile_relleno_hand():
    # A fried chile relleno: a stuffed poblano in a puffy, golden egg batter, its green stem poking out.
    s = Sprite()
    batter = plain_ramp("#e8bc58")
    s.ellipse(batter, 7.0, 9.0, 6.2, 3.8, angle=-25)
    s.pixels_at(batter[HIGHLIGHT], [(4, 9), (5, 8), (7, 7)])
    s.pixels_at(batter[SHADOW], [(6, 11), (9, 10)])
    s.pixels_at(ramp("#3a7a2a")[BASE], [(12, 4), (13, 3), (14, 2)])
    return s


def fried_egg_hand():
    # A fried egg: a spread of white with crisp golden edges and a round, glossy yolk.
    s = Sprite()
    white = plain_ramp("#f6f4ee")
    s.shape(lambda x, y: ((x - 7.5) / 6.6) ** 2 + ((y - 8.0) / 5.6) ** 2 + 0.12 * math.sin(x * 1.3 + y) <= 1, white, (7.5, 8.0), (6.6, 5.6))
    s.pixels_at(ramp("#d8a040")[BASE], [(2, 7), (13, 9), (6, 13), (10, 3)])  # Crisp edges.
    yolk = ramp("#f0a010", spread=0.5)
    s.ellipse(yolk, 8.0, 7.6, 2.6, 2.4)
    s.put(7, 6, yolk[HIGHLIGHT])
    return s


def fried_fiskefrikadeller_hand():
    # Fried fiskefrikadeller: oval fish cakes fried golden brown, a little craggy on top.
    s = Sprite()
    cake = ramp("#c8903e", spread=0.6)
    for cx, cy in [(5.0, 5.4), (10.6, 10.6)]:
        s.ellipse(cake, cx, cy, 4.2, 3.0)
        s.pixels_at(cake[SHADOW], [(round(cx) - 1, round(cy)), (round(cx) + 1, round(cy) + 1)])
        s.put(round(cx) - 2, round(cy) - 1, cake[HIGHLIGHT])
    return s


def fried_korokke_hand():
    # Fried korokke: oval potato croquettes in a coat of crisp, jagged golden panko.
    s = Sprite()
    crumb = ramp("#d8963a", spread=0.6)
    for cx, cy in [(5.0, 5.4), (10.6, 10.6)]:
        s.ellipse(crumb, cx, cy, 4.4, 3.2, texture=lambda x, y: 1 if (x * 3 + y) % 4 == 0 else -1 if (x + y * 3) % 5 == 0 else 0)
    return s

def patties(colour, spots=((5.0, 5.4), (10.6, 10.6)), rx=4.2, ry=3.0, flecks=(), s=None):
    """Flat fried patties, browned on top with a darker crust round the edge, optionally flecked."""
    s = s or Sprite()
    tones = ramp(colour, spread=0.6)
    for cx, cy in spots:
        s.ellipse(tones, cx, cy, rx, ry)
        s.pixels_at(tones[SHADOW], [(round(cx) - 1, round(cy)), (round(cx) + 1, round(cy) + 1)])
        s.put(round(cx) - 2, round(cy) - 1, tones[HIGHLIGHT])
        for k, (colour_, dx, dy) in enumerate(flecks):
            s.put(round(cx + dx), round(cy + dy), ramp(colour_)[BASE])
    return s


def fried_llapingachos_hand():
    # Fried llapingachos: thick potato-and-cheese patties fried to a golden crust, flecked orange with achiote.
    return patties("#e0a84a", flecks=[("#d8601e", -1, -1), ("#d8601e", 2, 0)])


def fried_nam_prik_mix_hand():
    # Fried nam prik: chili, shallot and shrimp paste fried in oil to a dark, glossy red-brown, in a bowl.
    s = bowl("#7a2a1a", mound=1.0, flecks=[("#c8401e", 0.12), ("#e8a040", 0.04)])
    return s


def fried_okonomiyaki_hand():
    # Fried okonomiyaki: a thick, round cabbage pancake fried golden brown on both sides, green flecks of cabbage showing.
    s = Sprite()
    cake = ramp("#c88a3a", spread=0.6)
    s.ellipse(cake, 7.5, 9.6, 7.0, 3.4)
    s.ellipse(ramp("#d8a050", spread=0.5), 7.5, 8.8, 6.2, 2.6, outline=False)
    s.pixels_at(ramp("#9ac060")[BASE], [(4, 8), (7, 9), (10, 8), (6, 10), (11, 10)])
    return s


def fried_pambazo_filling_hand():
    # Fried pambazo filling: potato and chorizo fried together, a red-orange, crumbly mix, in a bowl.
    return bowl("#c8502a", mound=1.6, texture=bumpy, flecks=[("#e8c070", 0.14)])


def fried_picarones_hand():
    # Fried picarones: light, ragged rings of sweet-potato and pumpkin dough fried a deep golden orange.
    s = Sprite()
    dough = ramp("#d8862e", spread=0.6)
    for cx, cy in [(5.0, 5.4), (10.6, 10.4)]:
        ring = [(cx + 3.4 * math.cos(math.radians(a)), cy + 3.0 * math.sin(math.radians(a))) for a in range(0, 361, 30)]
        s.capsule(dough, ring, (1.4, 1.4))
        s.put(round(cx) - 2, round(cy) - 3, dough[HIGHLIGHT])
    return s


def fried_potatoes_hand():
    # Fried potatoes: cubes of potato deep-fried crisp and golden.
    return chopped("#e0a840", size=1.4, seed=191)


def fried_spring_rolls_hand():
    # Fried spring rolls: crisp, blistered, golden rolls of pastry, sealed at the ends.
    s = Sprite()
    wrap = ramp("#d8a040", spread=0.6)
    for start, end in [((2.0, 6.4), (12.0, 2.4)), ((4.0, 13.4), (14.0, 9.4))]:
        s.capsule(wrap, [start, end], (2.4, 2.4), texture=lambda x, y: 1 if (x * 3 + y) % 5 == 0 else 0)
        s.put(round(start[0]), round(start[1]), wrap[SHADOW])
    return s


def fried_tod_mun_hand():
    # Fried tod mun: springy, flat Thai fish cakes, reddish-brown from curry paste, studded with green beans.
    return patties("#b0582e", flecks=[("#4a9a3a", -1, -1), ("#4a9a3a", 1, 1), ("#e8a050", 2, -1)])


def frikadeller_hand():
    # Frikadeller: flattened Danish meatballs fried a deep brown on the outside.
    return patties("#8a5228", rx=4.0, ry=3.2)


def frikadeller_fars_hand():
    # Frikadeller fars: minced pork with onion, egg and flour, a pale pink, flecked mixture in a bowl.
    return bowl("#e8b4a8", mound=1.8, texture=bumpy, flecks=[("#f0e8d0", 0.08)])


def fruchtetee_blend_hand():
    # Früchtetee: a loose blend of dried berries, deep red hibiscus, rosehip and pale apple pieces.
    s = Sprite()
    rng = random.Random(193)
    for _ in range(26):
        x, y = rng.randint(2, 13), rng.randint(5, 14)
        if abs(x - 7.5) > (y - 3) * 0.8:
            continue
        colour = rng.choice(["#7a1e2a", "#a8283a", "#c86a3a", "#d8c088", "#3a1e3a"])
        s.pixels_at(ramp(colour)[BASE], [(x, y), (x + 1, y)])
        s.put(x, y + 1, ramp(colour)[SHADOW])
    return s


def gai_satay_hand():
    # Gai satay: skewers of grilled turmeric chicken with a dish of peanut sauce and a little cucumber relish.
    s = plate()
    s.ellipse(ramp("#b8783a", spread=0.5), 11.6, 11.8, 2.6, 1.4)  # Peanut sauce.
    s.pixels_at(ramp("#9ac060")[BASE], [(3, 12), (4, 12), (5, 13)])  # Cucumber relish.
    return skewer("#e0a040", chunks=4, s=s, start=(0.6, 10.0), end=(15.0, 1.4))


def gammeldags_aeblekage_hand():
    # Gammeldags æblekage: a glass layered with apple compote, buttery toasted crumbs and whipped cream, a dot of
    # redcurrant jelly on top.
    s = Sprite()
    glass = plain_ramp("#dce8ec")
    s.shape(lambda x, y: 3 <= x <= 12 and 3 <= y <= 14 - (1 if x in (3, 12) else 0), glass, (7.5, 8.5), (4.5, 5.5))
    for y, colour in [(13, "#e8c060"), (12, "#e8c060"), (11, "#b8783a"), (10, "#b8783a"), (9, "#e8c060"), (8, "#b8783a"),
                      (7, "#faf8f2"), (6, "#faf8f2"), (5, "#faf8f2")]:
        s.pixels_at(plain_ramp(colour)[BASE], [(x, y) for x in range(4, 12)])
    s.pixels_at(ramp("#c8202a")[BASE], [(7, 4), (8, 4)])
    s.pixels_at(glass[HIGHLIGHT], [(4, 6), (4, 7), (4, 8)])
    return s


def garniture_hand():
    # Garniture: crisp bacon lardons, sautéed mushrooms and onion, mixed in a bowl.
    return bowl("#a8643a", mound=1.4, flecks=[("#7a3a24", 0.14), ("#c8a878", 0.12), ("#f0e8d0", 0.06)])


def gelatin_hand():
    # Gelatin: a clear, pale amber, wobbly set block with a glassy shine.
    s = Sprite()
    jelly = plain_ramp("#e8c880")
    s.shape(lambda x, y: 2 <= x <= 13 and 5 <= y <= 13, jelly, (7.5, 9.0), (5.5, 4.0))
    s.pixels_at(plain_ramp("#f6e6b8")[BASE], [(x, 5) for x in range(3, 13)])  # The top face.
    s.pixels_at((255, 250, 235), [(4, 7), (4, 8), (5, 7)])
    s.pixels_at(jelly[SHADOW], [(11, 11), (12, 12), (10, 12)])
    return s

def gherkins_hand():
    # Gherkins: small, knobbly, dark olive-green pickled cucumbers with a sprig of dill.
    s = Sprite()
    pickle = ramp("#5a7a2e", spread=0.6)
    for start, end in [((2.4, 6.0), (11.0, 2.6)), ((4.0, 13.0), (13.6, 9.4))]:
        s.capsule(pickle, [start, end], (2.2, 2.0), texture=lambda x, y: 1 if (x * 2 + y * 3) % 5 == 0 else 0)
    s.pixels_at(ramp("#8ab84a")[BASE], [(13, 4), (14, 3), (13, 2)])
    return s


def gingerbread_hand():
    # Gingerbread: dark, spiced honey biscuits, a heart and a round, piped with white icing.
    s = Sprite()
    biscuit = ramp("#8a4a24", spread=0.6)
    s.shape(lambda x, y: ((x - 3.6) / 2.8) ** 2 + ((y - 4.0) / 2.6) ** 2 <= 1 or ((x - 8.4) / 2.8) ** 2 + ((y - 4.0) / 2.6) ** 2 <= 1 or
            (y >= 4 and abs(x - 6.0) <= (11.0 - y) * 0.8), biscuit, (6.0, 6.0), (5.4, 5.0))
    s.ellipse(biscuit, 11.4, 11.4, 3.6, 3.4)
    icing = plain_ramp("#faf8f2")[BASE]
    s.pixels_at(icing, [(3, 4), (4, 3), (8, 3), (9, 4), (6, 8), (4, 6), (8, 6)])
    s.pixels_at(icing, [(10, 10), (12, 10), (11, 12), (9, 12), (13, 12)])
    return s


def gingerbread_dough_hand():
    # Gingerbread dough: a dark, sticky, spiced brown dough.
    return dough_ball("#8a5a34", flour=False)


def glass_noodle_dough_hand():
    # Glass noodle dough: sweet potato starch and water, a heavy, translucent grey paste.
    return dough_ball("#c8c4c0", flour=False)


def glass_noodles_hand():
    # Glass noodles: dried bundles of fine, wiry, translucent grey-white strands, tied in the middle.
    s = Sprite()
    strand = plain_ramp("#e2e4e6")
    for k in range(-3, 4):
        # Each strand a fine line, bunched at the tie and fanning out at the ends.
        y_end = 8 + k * 2
        s.line(strand[BASE if k % 2 else LIGHT], 1, y_end, 7, 8 + (k > 0) - (k < 0))
        s.line(strand[BASE if k % 2 else LIGHT], 8, 8 + (k > 0) - (k < 0), 14, y_end)
    s.pixels_at(ramp("#c8a070")[BASE], [(7, 6), (7, 7), (7, 8), (7, 9), (8, 6), (8, 9)])  # The tie.
    return s


def glutinous_rice_flour_hand():
    # Glutinous rice flour: a soft, bright white flour, in a clay bowl.
    return spice_bowl("#f8f6f0", fleck="#e8e4da", seed=197, bowl_colour=CLAY)


def bones_of(colour, size=1.0, scrap=None):
    """A pair of bones, knobbed at the ends, sized for the animal, with an optional scrap of meat."""
    def drawn():
        s = Sprite()
        bone = plain_ramp(colour)
        for start, end in [((3.0, 12.6), (12.6, 3.0)), ((3.6, 5.0), (9.0, 13.0))]:
            s.capsule(bone, [start, end], (1.1 * size, 1.1 * size))
            for x, y in (start, end):
                s.ellipse(bone, x, y, 1.6 * size, 1.5 * size)
        s.pixels_at(bone[HIGHLIGHT], [(7, 7), (8, 6)])
        if scrap:
            s.pixels_at(ramp(scrap)[BASE], [(8, 8), (9, 7)])
        return s
    return drawn


def casing(colour):
    """A natural sausage casing: a thin, translucent tube wound into a loose coil."""
    def drawn():
        s = Sprite()
        tube = plain_ramp(colour)
        # A spiral, drawn as a line of pixels, inner turns paler.
        points = []
        for step in range(0, 900, 12):
            r = 1.4 + step / 900 * 5.0
            a = math.radians(step)
            points.append((round(7.5 + r * math.cos(a)), round(8.0 + r * 0.8 * math.sin(a))))
        for i, (x, y) in enumerate(points):
            s.put(x, y, tube[SHADOW] if i % 3 == 0 else tube[BASE])
        s.pixels_at((252, 250, 246), [points[len(points) // 2], points[-6]])
        return s
    return drawn


def fat_chunk(colour, rind=None):
    """A chunk of raw fat: a creamy, waxy, irregular block, with an optional strip of skin."""
    def drawn():
        s = Sprite()
        fat = plain_ramp(colour)
        s.shape(lambda x, y: ((x - 7.5) / 6.0) ** 2 + ((y - 8.6) / 4.6) ** 2 + 0.15 * math.sin(x * 1.1 + y * 0.7) <= 1, fat, (7.5, 8.6),
                (6.0, 4.6))
        s.pixels_at(fat[HIGHLIGHT], [(4, 6), (5, 5), (6, 5)])
        s.pixels_at(fat[SHADOW], [(9, 10), (10, 11), (6, 11)])
        if rind:
            s.pixels_at(plain_ramp(rind)[BASE], [(x, 12) for x in range(4, 12)])
        return s
    return drawn


def leg_joint(lean, fat):
    """A whole leg joint: a plump rounded roast of lean meat with a cap of fat, the white shank bone sticking out."""
    def drawn():
        s = Sprite()
        bone = plain_ramp("#ece2cc")
        s.capsule(bone, [(10.0, 6.0), (14.4, 1.6)], (1.1, 1.0))
        s.ellipse(bone, 14.2, 1.8, 1.4, 1.4)
        slab(lean, fat=fat, cx=6.6, cy=9.4, rx=5.6, ry=4.8, angle=-35, fat_side=1.2, seed=211, s=s)
        return s
    return drawn


def stew_meat(lean):
    """Stew meat: neat cubes of lean meat for braising."""
    return lambda: chopped(lean, size=1.3, seed=223)


def goat_casing_hand():
    return casing("#ece0d8")()


def gougere_dough_hand():
    # Gougère dough: choux paste folded with grated hard cheese, a glossy yellow paste flecked with cheese, in a bowl.
    s = bowl("#f0cc6a", mound=1.4, plain=True, flecks=[("#e8a83a", 0.12)])
    return s


# Seeds that look like other seeds carry a small picture of their plant in the top right corner, so they can be told
# apart: the chilies and peppers, the cabbages and radish, the basils, the aubergines, tomato and tomatillo, and the
# courgette and cucumber, marjoram and oregano, mint, and savory. The three grape cuttings and the two currant cuttings carry
# one too, and so does every sapling, since young trees look alike. The picture is the plant's own sprite, shrunk.
SEED_PLANTS = {
    "aji_amarillo_seeds": "aji_amarillo", "bell_pepper_seeds": "bell_pepper", "birds_eye_chili_seeds": "birds_eye_chili",
    "green_chili_seeds": "green_chili", "paprika_pepper_seeds": "paprika_pepper", "poblano_chili_seeds": "poblano_chili",
    "red_chili_seeds": "red_chili", "cabbage_seeds": "cabbage", "red_cabbage_seeds": "red_cabbage", "radish_seeds": "radish",
    "basil_seeds": "basil", "holy_basil_seeds": "holy_basil", "thai_basil_seeds": "thai_basil", "aubergine_seeds": "aubergine",
    "thai_aubergine_seeds": "thai_aubergine", "tomato_seeds": "tomato", "tomatillo_seeds": "tomatillo",
    "courgette_seeds": "courgette", "cucumber_seeds": "cucumber", "marjoram_seeds": "marjoram", "oregano_seeds": "oregano",
    "mint_seeds": "mint", "grape_cutting": "grape", "red_grape_cutting": "red_grape", "white_grape_cutting": "white_grape",
    "blackcurrant_cutting": "blackcurrant", "redcurrant_cutting": "redcurrant", "savory_seeds": "savory",
    "allspice_sapling": "allspice", "almond_sapling": "almond", "annatto_sapling": "annatto", "apple_sapling": "apple",
    "apricot_sapling": "apricot", "bay_laurel_sapling": "bay_laurel_leaf", "cinnamon_sapling": "cinnamon_bark", "clove_sapling": "clove_bud",
    "coconut_sapling": "coconut", "coffee_sapling": "coffee_cherry",
}
# Pictures for plants whose fruit is a vanilla item, which this script does not draw.
BADGE_ONLY = {"apple": apple_badge, "annatto": annatto_badge}
BADGE = 8


def plant_badge(source):
    small = (BADGE_ONLY.get(source) or SPRITES[source])().image().resize((BADGE, BADGE), Image.BOX)
    pixels = small.load()
    for y in range(BADGE):
        for x in range(BADGE):
            r, g, b, a = pixels[x, y]
            pixels[x, y] = (r, g, b, 255 if a > 100 else 0)
    return small


def with_plant(draw, source):
    def drawn():
        s = draw()
        # Clear the corner, then put the plant picture there.
        for y in range(BADGE):
            for x in range(SIZE - BADGE, SIZE):
                s.pixels[y][x] = None
        badge = plant_badge(source).load()
        for y in range(BADGE):
            for x in range(BADGE):
                r, g, b, a = badge[x, y]
                if a:
                    s.put(SIZE - BADGE + x, y, (r, g, b))
        return s
    return drawn


HAND = {"plum": plum_hand, "apricot": apricot_hand, "orange": orange_hand, "lime": lime_hand, "mango": mango_hand}
HAND.update({"gherkins": gherkins_hand, "gingerbread": gingerbread_hand, "gingerbread_dough": gingerbread_dough_hand,
             "glass_noodle_dough": glass_noodle_dough_hand, "glass_noodles": glass_noodles_hand, "glutinous_rice_flour": glutinous_rice_flour_hand,
             # Goat: lean, dark red meat with little fat.
             "goat_bones": bones_of("#e6dcc4", 1.1, scrap="#9a3a30"), "goat_casing": goat_casing_hand, "goat_fat": fat_chunk("#efe4cc"),
             "goat_heart": lambda: heart("#7a2a2a", fat="#efe4cc"), "goat_leg": leg_joint("#9a3a30", "#efe2d0"),
             "goat_liver": lambda: liver("#6a2a24"), "goat_shoulder": lambda: slab("#9a3a30", fat="#efe2d0", rx=6.2, ry=5.2, angle=-8,
                                                                                    fat_side=0.8, marbling=0.05, seed=227),
             "goat_stew_meat": stew_meat("#9a3a30"), "gougere_dough": gougere_dough_hand})
HAND.update({"fried_llapingachos": fried_llapingachos_hand, "fried_nam_prik_mix": fried_nam_prik_mix_hand, "fried_okonomiyaki": fried_okonomiyaki_hand,
             "fried_pambazo_filling": fried_pambazo_filling_hand, "fried_picarones": fried_picarones_hand, "fried_potatoes": fried_potatoes_hand,
             "fried_spring_rolls": fried_spring_rolls_hand, "fried_tod_mun": fried_tod_mun_hand, "frikadeller": frikadeller_hand,
             "frikadeller_fars": frikadeller_fars_hand, "fruchtetee_blend": fruchtetee_blend_hand, "gai_satay": gai_satay_hand,
             "gammeldags_aeblekage": gammeldags_aeblekage_hand, "garniture": garniture_hand, "gelatin": gelatin_hand})
HAND.update({"french_onion_soup": french_onion_soup_hand, "fresh_cheese": fresh_cheese_hand, "fresh_chorizo": fresh_chorizo_hand,
             "fresh_empanada_dough": fresh_empanada_dough_hand, "fresh_glass_noodles": fresh_glass_noodles_hand, "fresh_panko": fresh_panko_hand,
             "fresh_rice_noodles": fresh_rice_noodles_hand, "fried_bratwurst": fried_bratwurst_hand, "fried_bread_roll": fried_bread_roll_hand,
             "fried_chile_relleno": fried_chile_relleno_hand, "fried_churros": fried_churros_hand, "fried_doughnut": fried_doughnut_hand,
             "fried_egg": fried_egg_hand, "fried_fiskefrikadeller": fried_fiskefrikadeller_hand, "fried_korokke": fried_korokke_hand})
HAND.update({"fermented_tapioca_starch": fermented_tapioca_starch_hand, "fermenting_anchovies": fermenting_anchovies_hand,
             "fischbrotchen": fischbrotchen_hand, "fish_bones": fish_bones_hand, "fish_guts": fish_guts_hand, "fish_head": fish_head_hand,
             "fish_scales": fish_scales_hand, "fiskefrikadeller": fiskefrikadeller_hand, "fiskefrikadeller_fars": fiskefrikadeller_fars_hand,
             "flaeskesteg": flaeskesteg_hand, "flaeskesteg_med_rodkal": flaeskesteg_med_rodkal_hand, "flammkuchen": flammkuchen_hand,
             "flan_custard": flan_custard_hand, "flan_napolitano": flan_napolitano_hand, "flatbread_dough": flatbread_dough_hand})
HAND.update({"duck_heart_and_gizzard": duck_heart_and_gizzard_hand, "duck_legs": duck_legs_hand, "duck_liver": duck_liver_hand,
             "duck_spawn_egg": duck_spawn_egg_hand, "duck_wings": duck_wings_hand, "elote": elote_hand, "empanada_dough": empanada_dough_hand,
             "empanadas_de_carne": empanadas_de_carne_hand, "enchiladas_verdes": enchiladas_verdes_hand, "feijoada": feijoada_hand,
             "feijoada_beans": feijoada_beans_hand, "fermented_cocoa_beans": fermented_cocoa_beans_hand,
             "fermented_coffee_cherries": fermented_coffee_cherries_hand, "fermented_red_grapes": fermented_red_grapes_hand,
             "fermented_shrimp": fermented_shrimp_hand})
HAND.update({"dried_rosehip": dried("rosehip", 0.6, 0.7, 0.16, 61), "dried_rosemary": dried("rosemary", 0.5, 0.85, 0.1, 67),
             "dried_savory": dried("savory", 0.45, 0.85, 0.12, 71), "dried_sea_lettuce": dried("sea_lettuce", 0.45, 0.6, 0.14, 73),
             "dried_shrimp": dried("boiled_prawns", 0.8, 0.72, 0.2, 79), "dried_soybeans": lambda: bean_pile("#d8bc80", PILE, spread=0.6),
             "dried_sweet_berries": dried_sweet_berries_hand, "dried_thyme": dried("thyme", 0.45, 0.85, 0.12, 89),
             "dried_wakame": dried("wakame", 0.35, 0.45, 0.14, 97), "dried_white_beans": dried("white_bean", 0.6, 0.92, 0.08, 101),
             "dried_yerba_mate_leaves": dried("yerba_mate_leaf", 0.5, 0.8, 0.12, 103), "duck_bones": duck_bones_hand,
             "duck_breasts": duck_breasts_hand, "duck_confit": duck_confit_hand, "duck_fat": duck_fat_hand})
HAND.update({"dried_chickpeas": dried("chickpea", 0.6, 0.9, 0.1, 11), "dried_chilies": dried("red_chili", 0.85, 0.62, 0.16, 13),
             "dried_cocoa_beans": dried_cocoa_beans_hand, "dried_coffee_cherries": dried("coffee_cherry", 0.5, 0.55, 0.2, 17),
             "dried_cooked_corn": dried("cooked_corn", 0.6, 0.85, 0.1, 19), "dried_corn": dried("corn_kernels", 0.7, 0.85, 0.08, 23),
             "dried_corn_husks": dried("corn_husk", 0.35, 1.0, 0.08, 29), "dried_fruit": dried_fruit_hand,
             "dried_hibiscus": dried("roselle", 0.7, 0.55, 0.16, 31), "dried_mint": dried("mint", 0.45, 0.85, 0.12, 37),
             "dried_nutmeg": dried("nutmeg_seed", 0.5, 0.8, 0.1, 41), "dried_oregano": dried("oregano", 0.45, 0.85, 0.12, 43),
             "dried_paprika_pepper": dried("paprika_pepper", 0.85, 0.62, 0.16, 47), "dried_peppercorns": dried_peppercorns_hand,
             "dried_raspberry": dried("raspberry", 0.6, 0.7, 0.16, 59)})
HAND.update({"drained_cream": drained_cream_hand, "dried_apple": dried_apple_hand, "dried_azuki_beans": dried("azuki_bean", 0.7, 0.85, 0.1, 3),
             "dried_berries": dried_berries_hand, "dried_black_beans": dried("black_bean", 0.5, 0.95, 0.06, 5),
             "dried_blackcurrant": dried("blackcurrant", 0.6, 0.8, 0.18, 7), "dried_bread": dried_bread_hand,
             "dried_cardamom_pods": dried("cardamom_pod", 0.55, 0.85, 0.1, 9)})
HAND.update({"dressed_chicken": dressed_chicken_hand, "dressed_cow": dressed_cow_hand, "dressed_duck": dressed_duck_hand,
             "dressed_goat": dressed_goat_hand, "dressed_pig": dressed_pig_hand, "dressed_rabbit": dressed_rabbit_hand,
             "dressed_sheep": dressed_sheep_hand, "duck_carcass": duck_carcass_hand, "goat_carcass": goat_carcass_hand,
             "pig_carcass": pig_carcass_hand, "rabbit_carcass": rabbit_carcass_hand, "sheep_carcass": sheep_carcass_hand,
             "cow_carcass": cow_carcass_hand, "chicken_carcass": chicken_carcass_hand})
HAND.update({"diced_bell_pepper": diced_bell_pepper_hand, "diced_courgette": diced_courgette_hand, "diced_nopales": diced_nopales_hand,
             "diced_potato": diced_potato_hand, "diced_water_chestnut": diced_water_chestnut_hand, "diced_white_fish": diced_white_fish_hand,
             "dimensional_anchor": dimensional_anchor_hand, "dimensional_folding_core": dimensional_folding_core_hand,
             "dimensional_logic_core": dimensional_logic_core_hand, "dipped_bread_roll": dipped_bread_roll_hand,
             "dipped_pretzels": dipped_pretzels_hand, "dipped_tortillas": dipped_tortillas_hand, "dorayaki": dorayaki_hand,
             "dorayaki_batter": dorayaki_batter_hand, "dorayaki_pancakes": dorayaki_pancakes_hand})
HAND.update({"cured_beef_strips": cured_beef_strips_hand, "cured_pork_belly": cured_pork_belly_hand, "cured_pork_shoulder": cured_pork_shoulder_hand,
             "cured_salmon": cured_salmon_hand, "curried_fish_paste": curried_fish_paste_hand, "curry_ketchup": curry_ketchup_hand,
             "curry_ketchup_base": curry_ketchup_base_hand, "curry_powder": curry_powder_hand, "curry_spice_mix": curry_spice_mix_hand,
             "currywurst": currywurst_hand, "daifuku_mochi": daifuku_mochi_hand, "dango_dough": dango_dough_hand,
             "desiccated_coconut": desiccated_coconut_hand, "diced_aubergine": diced_aubergine_hand, "diced_beef": diced_beef_hand})
HAND.update({"creme_brulee": creme_brulee_hand, "creme_brulee_custard": creme_brulee_custard_hand, "creme_fraiche": creme_fraiche_hand,
             "crisp_bacon": crisp_bacon_hand, "crisp_carnitas": crisp_carnitas_hand, "crisp_pork_belly": crisp_pork_belly_hand,
             "croque_monsieur": croque_monsieur_hand, "crushed_blackcurrants": crushed_blackcurrants_hand, "crushed_ice": crushed_ice_hand,
             "crushed_red_grapes": crushed_red_grapes_hand, "crushed_white_grapes": crushed_white_grapes_hand,
             "crustless_bread": crustless_bread_hand, "cubed_tofu": cubed_tofu_hand, "cucumber_relish": cucumber_relish_hand,
             "curds": curds_hand})
HAND.update({"coq_au_vin": coq_au_vin_hand, "corn_husk": corn_husk_hand, "corn_kernels": corn_kernels_hand, "corn_paste": corn_paste_hand,
             "corn_tortilla": corn_tortilla_hand, "cornstarch": cornstarch_hand, "cotija_cheese": cotija_cheese_hand,
             "cow_carcass": cow_carcass_hand, "coxinha_dough": coxinha_dough_hand, "coxinhas": coxinhas_hand, "cracked_rye": cracked_rye_hand,
             "cracklings": cracklings_hand, "cream_cheese": cream_cheese_hand, "creamed_chicken_sauce": creamed_chicken_sauce_hand,
             "crema": crema_hand})
HAND.update({"cocoa_powder": cocoa_powder_hand, "cocoa_press_cake": cocoa_press_cake_hand, "coconut_sapling": coconut_sapling_hand,
             "coconut_sticky_rice": coconut_sticky_rice_hand, "cod_roe": cod_roe_hand, "coffee_sapling": coffee_sapling_hand,
             "concha_topping": concha_topping_hand, "conchas": conchas_hand, "cookbook": cookbook_hand,
             "cooked_black_beans": cooked_black_beans_hand, "cooked_chevon": cooked_chevon_hand, "cooked_corn": cooked_corn_hand,
             "cooked_soybeans": cooked_soybeans_hand, "cookie_dough": cookie_dough_hand, "cooled_brigadeiro_mixture": cooled_brigadeiro_mixture_hand})
HAND.update({"chorizo_filling": chorizo_filling_hand, "choux_buns": choux_buns_hand, "choux_panade": choux_panade_hand,
             "choux_pastry": choux_pastry_hand, "churro_dough": churro_dough_hand, "churros": churros_hand, "cinnamon": cinnamon_hand,
             "cinnamon_sapling": cinnamon_sapling_hand, "clove_sapling": clove_sapling_hand, "cloves": cloves_hand, "coarse_salt": coarse_salt_hand,
             "coated_water_chestnut": coated_water_chestnut_hand, "cocktail_sausage_filling": cocktail_sausage_filling_hand,
             "cocktail_sausages": cocktail_sausages_hand, "cocoa_mass": cocoa_mass_hand})
HAND.update({"chiles_rellenos": chiles_rellenos_hand, "chili_garlic": chili_garlic_hand, "chili_pork": chili_pork_hand,
             "chili_powder": chili_powder_hand, "chocolate": chocolate_hand, "chocolate_sponge": chocolate_sponge_hand,
             "chopped_almonds": chopped_almonds_hand, "chopped_apple": chopped_apple_hand, "chopped_beetroot": chopped_beetroot_hand,
             "chopped_carrot": chopped_carrot_hand, "chopped_gherkins": chopped_gherkins_hand, "chopped_laver": chopped_laver_hand,
             "chopped_onion": chopped_onion_hand, "chopped_rhubarb": chopped_rhubarb_hand, "chorizo": chorizo_hand})
HAND.update({"chashu_pork": chashu_pork_hand, "cheesecake_filling": cheesecake_filling_hand, "cherry_compote": cherry_compote_hand,
             "cherry_sauce": cherry_sauce_hand, "chevon_chops": chevon_chops_hand, "chicken_bones": chicken_bones_hand,
             "chicken_breast": chicken_breast_hand, "chicken_carcass": chicken_carcass_hand, "chicken_drumsticks": chicken_drumsticks_hand,
             "chicken_feet": chicken_feet_hand, "chicken_giblets": chicken_giblets_hand, "chicken_skin": chicken_skin_hand,
             "chicken_thighs": chicken_thighs_hand, "chicken_wings": chicken_wings_hand, "chickpea_flour": chickpea_flour_hand})
HAND.update({"caneles": caneles_hand, "caramel": caramel_hand, "caramelised_onion": caramelised_onion_hand, "cardoon": cardoon_hand,
             "cardoon_flower": cardoon_flower_hand, "carne_seca": carne_seca_hand, "carnitas_pork": carnitas_pork_hand,
             "carnitas_tacos": carnitas_tacos_hand, "cassoulet": cassoulet_hand, "cassoulet_beans": cassoulet_beans_hand,
             "ceviche": ceviche_hand, "charred_corn": charred("corn", 0.18, 3), "charred_dango_skewer": charred_dango_skewer_hand,
             "charred_poblano": charred("poblano_chili", 0.2, 7), "charred_tomato": charred("tomato", 0.2, 9)})
HAND.update({"brown_sugar": brown_sugar_hand, "browned_beef": browned_beef_hand, "browned_mince": browned_mince_hand,
             "bruised_lemongrass": bruised_lemongrass_hand, "bruised_tea_leaves": bruised_tea_leaves_hand, "brunsviger": brunsviger_hand,
             "bua_loy": bua_loy_hand, "bua_loy_balls": bua_loy_balls_hand, "bua_loy_dough": bua_loy_dough_hand,
             "buttered_crumbs": buttered_crumbs_hand, "cake_batter": cake_batter_hand, "caldillo_base": caldillo_base_hand,
             "calf_stomach": calf_stomach_hand, "cane_sugar_paste": cane_sugar_paste_hand, "canele_batter": canele_batter_hand})
HAND.update({"boiled_tub_tim": boiled_tub_tim_hand, "boller_i_karry": boller_i_karry_hand, "boller_i_karry_stew": boller_i_karry_stew_hand,
             "bonito_flakes": bonito_flakes_hand, "bouquet_garni": bouquet_garni_hand, "bratwurst_filling": bratwurst_filling_hand,
             "bratwurst_mit_sauerkraut": bratwurst_mit_sauerkraut_hand, "bread_dough": bread_dough_hand, "bread_roll": bread_roll_hand,
             "breadcrumbs": breadcrumbs_hand, "brigadeiro_mixture": brigadeiro_mixture_hand, "brigadeiros": brigadeiros_hand,
             "brined_cheese": brined_cheese_hand, "brined_pork": brined_pork_hand, "brown_butter": brown_butter_hand})
HAND.update({"blind_baked_shell": blind_baked_shell_hand, "blood_sausage": blood_sausage_hand, "blood_sausage_filling": blood_sausage_filling_hand,
             "blue_corn_masa": blue_corn_masa_hand, "blue_hominy": blue_hominy_hand, "boeuf_bourguignon": boeuf_bourguignon_hand,
             "boiled_beetroot": boiled_beetroot_hand, "boiled_bua_loy": boiled_bua_loy_hand, "boiled_dango": boiled_dango_hand,
             "boiled_oil_palm_fruit": boiled_oil_palm_fruit_hand, "boiled_picarones_vegetables": boiled_picarones_vegetables_hand,
             "boiled_potatoes": boiled_potatoes_hand, "boiled_prawns": boiled_prawns_hand, "boiled_ramen_noodles": boiled_ramen_noodles_hand,
             "boiled_sweet_potato": boiled_sweet_potato_hand})
HAND.update({"beef_kidneys": beef_kidneys_hand, "beef_liver": beef_liver_hand, "beef_rib_roast": beef_rib_roast_hand, "beef_round": beef_round_hand,
             "beef_shank": beef_shank_hand, "beef_sirloin": beef_sirloin_hand, "beef_strips": beef_strips_hand, "beef_tongue": beef_tongue_hand,
             "berliner": berliner_hand, "bienenstich": bienenstich_hand, "black_bean_mash": black_bean_mash_hand, "black_pepper": black_pepper_hand,
             "black_tea": black_tea_hand, "blanched_almonds": blanched_almonds_hand, "blanched_vanilla_pods": blanched_vanilla_pods_hand})
HAND.update({"arroz_con_leche": arroz_con_leche_hand, "azuki_mash": azuki_mash_hand, "back_fat": back_fat_hand, "bacon": bacon_hand,
             "baked_bienenstich": baked_bienenstich_hand, "baked_creme_brulee": baked_creme_brulee_hand,
             "baked_kammerjunkere": baked_kammerjunkere_hand, "bay_laurel_sapling": bay_laurel_sapling_hand, "bay_leaf": bay_leaf_hand,
             "bean_sprouts": bean_sprouts_hand, "bechamel": bechamel_hand, "beef_bones": beef_bones_hand, "beef_brisket": beef_brisket_hand,
             "beef_chuck": beef_chuck_hand, "beef_heart": beef_heart_hand})
HAND.update({"anchovy": anchovy_hand, "anko": anko_hand, "annatto_sapling": annatto_sapling_hand, "anticucho_marinade": anticucho_marinade_hand,
             "anticucho_skewer": anticucho_skewer_hand, "anticuchos": anticuchos_hand, "aonori": aonori_hand, "apfelstrudel": apfelstrudel_hand,
             "apple_compote": apple_compote_hand, "apple_pomace": apple_pomace_hand, "apple_sapling": apple_sapling_hand,
             "apricot_sapling": apricot_sapling_hand, "arepa_dough": arepa_dough_hand, "arepas": arepas_hand,
             "arepas_con_queso": arepas_con_queso_hand})
HAND.update({"alfajor_dough": alfajor_dough_hand, "alfajor_rounds": alfajor_rounds_hand, "alfajores_de_maicena": alfajores_de_maicena_hand,
             "allspice_sapling": allspice_sapling_hand, "almond_sapling": almond_sapling_hand})
HAND.update({"aged_yerba_mate": aged_yerba_mate_hand, "ajitsuke_tamago": ajitsuke_tamago_hand, "al_pastor_adobo": al_pastor_adobo_hand,
             "al_pastor_meat": al_pastor_meat_hand, "alfajor_biscuits": alfajor_biscuits_hand})
HAND.update({"achiote_potato_mash": achiote_potato_mash_hand, "aebleskiver": aebleskiver_hand, "aebleskiver_batter": aebleskiver_batter_hand,
             "aged_hard_cheese": aged_hard_cheese_hand, "aged_soft_cheese": aged_soft_cheese_hand})
HAND.update({"wasabi_root": wasabi_root_hand, "anise": anise_seed_hand, "cumin": cumin_hand, "cardamom": cardamom_hand,
             "wasabi": wasabi_hand})
HAND.update({"toasted_pumpkin_seeds": toasted_pumpkin_seeds_hand, "toasted_sesame_seeds": toasted_sesame_seeds_hand,
             "tomatillo_seeds": tomatillo_seeds_hand, "tomato_seeds": tomato_seeds_hand, "vanilla_cutting": vanilla_cutting_hand})
HAND.update({"tea_cutting": tea_cutting_hand, "thai_aubergine_seeds": thai_aubergine_seeds_hand, "thai_basil_seeds": thai_basil_seeds_hand,
             "thyme_cutting": thyme_cutting_hand, "toasted_cumin_seed": toasted_cumin_seed_hand})
HAND.update({"savory": savory_hand, "savory_seeds": savory_seeds_hand, "sesame_seed": sesame_seed_hand,
             "spring_onion_seeds": spring_onion_seeds_hand, "strawberry_runner": strawberry_runner_hand, "sunflower_seeds": sunflower_seeds_hand})
HAND.update({"red_grape_cutting": red_grape_cutting_hand, "white_grape_cutting": white_grape_cutting_hand, "redcurrant_cutting": redcurrant_cutting_hand,
             "rhubarb_crown": rhubarb_crown_hand, "roselle_seeds": roselle_seeds_hand, "rosemary_cutting": rosemary_cutting_hand})
HAND.update({"poblano_chili_seeds": poblano_chili_seeds_hand, "radish_seeds": radish_seeds_hand, "raspberry_cutting": raspberry_cutting_hand,
             "red_cabbage_seeds": cabbage_seeds_hand, "red_chili_seeds": red_chili_seeds_hand})
HAND.update({"pandan_cutting": pandan_cutting_hand, "paprika_pepper_seeds": paprika_pepper_seeds_hand, "parsley_seeds": parsley_seeds_hand,
             "peppercorn_cutting": peppercorn_cutting_hand, "pineapple_crown": pineapple_crown_hand})
HAND.update({"marjoram_seeds": marjoram_seeds_hand, "mint_seeds": mint_seeds_hand, "mustard_seed": mustard_seed_hand,
             "nutmeg_seed": nutmeg_seed_hand, "oregano_seeds": oregano_seeds_hand, "oregano": oregano_hand})
HAND.update({"dried_cumin_seed": cumin_seed_hand, "elder_cutting": elder_cutting_hand, "grape_cutting": grape_cutting_hand,
             "hop_rhizome": hop_rhizome_hand, "long_bean_seeds": long_bean_seeds_hand})
HAND.update({"cumin_seed": cumin_seed_hand, "dill_seeds": dill_seeds_hand, "green_chili_seeds": green_chili_seeds_hand,
             "holy_basil_seeds": holy_basil_seeds_hand, "lettuce_seeds": lettuce_seeds_hand})
HAND.update({"cardamom_seeds": cardamom_seeds_hand, "celery_seeds": celery_seeds_hand, "coriander_seed": coriander_seed_hand,
             "courgette_seeds": courgette_seeds_hand, "cucumber_seeds": cucumber_seeds_hand})
HAND.update({"betel_cutting": betel_cutting_hand, "birds_eye_chili_seeds": birds_eye_chili_seeds_hand,
             "blackcurrant_cutting": blackcurrant_cutting_hand, "cabbage_seeds": cabbage_seeds_hand, "caraway_seed": caraway_seed_hand})
HAND.update({"annatto_seed": annatto_seed_hand, "asparagus_crown": asparagus_crown_hand, "aubergine_seeds": aubergine_seeds_hand,
             "basil_seeds": basil_seeds_hand, "bell_pepper_seeds": bell_pepper_seeds_hand})
HAND.update({"water_chestnut": water_chestnut_hand, "white_bean": white_bean_hand, "yerba_mate_leaf": yerba_mate_leaf_hand,
             "aji_amarillo_seeds": aji_amarillo_seeds_hand, "anise_seed": anise_seed_hand})
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
SPRITES.update({name: with_plant(SPRITES[name], source) for name, source in SEED_PLANTS.items() if name in SPRITES})


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
