"""Draws the mod's own 16x16 item textures in code, so no one has to paint them.

Run from the repository root:
    python3 .workflow/art/item_sprites.py            # writes the textures and points the item models at them
    python3 .workflow/art/item_sprites.py --sheet out.png   # also writes a review sheet

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
        hh = h + (target - h) * abs(dh) * 2 if dl else h
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
            self.put(x, y, tones[tone])
        return mask

    def ellipse(self, tones, cx, cy, rx, ry, angle=0.0, **kw):
        cos, sin = math.cos(math.radians(angle)), math.sin(math.radians(angle))

        def inside(x, y):
            dx, dy = x - cx, y - cy
            u, v = dx * cos + dy * sin, -dx * sin + dy * cos
            return (u / rx) ** 2 + (v / ry) ** 2 <= 1

        return self.shape(inside, tones, (cx, cy), (rx, ry), **kw)

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


SPRITES = {"pineapple": pineapple, "coconut": coconut, "rosemary": rosemary, "blackcurrant": blackcurrant, "tomato": tomato,
           "aubergine": aubergine, "lemon": lemon, "garlic": garlic, "mussels": mussels, "compost": compost}


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
