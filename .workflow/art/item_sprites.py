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
    s.ellipse(tones, 7.5, cy, r, r * 0.95, texture=speckle(hash(colour) % 97, 0.18, up=0) if pores else None)
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
    s.ellipse(tones, 7.5, 9.0, 6.6, 6.0, texture=speckle(hash(colour) % 89, 0.25) if frilly else None)
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
        s.capsule(tones, [(3, 11), (8, 8), (12, 6)], (2.6, 1.6), texture=speckle(hash(colour) % 83, 0.2, up=0))
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
    key = {"o": OUTLINE, "s": SHADOW, "b": BASE, "l": LIGHT, "h": HIGHLIGHT, "r": SHADOW}
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


HAND = {"plum": plum_hand, "apricot": apricot_hand, "orange": orange_hand, "lime": lime_hand, "mango": mango_hand}
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
