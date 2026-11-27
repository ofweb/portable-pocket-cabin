"""Plant block models built in 3D in code, with pixel-art textures, matched against photos of the real plants.

Run from the repository root: python3 .workflow/art/block_models.py
Each plant is built from a few kinds of part: leaves (flat pixel-art planes tilted out from the stem at any angle, alone,
in rings, or held up on stalks), thin stalks and tubes, tufts of crossed planes for grain, round bodies stacked from
one-pixel slices for heads, bulbs, roots and berries, small solids for fruit, and the trellis with its vine. Textures
are drawn as grids of palette letters ('.' is clear) in a few flat colours, light toward the top and upper left, dark
below, with no outlines, and every part is mapped one texel per pixel so the parts match. Each growth stage is a list
of parts, written to models/block/<plant>_stage<age>.json, where the blockstates already point each age.
"""
import json
import os

from PIL import Image

A = "src/main/resources/assets/portable_pocket_cabin"
NS = "portable_pocket_cabin:block/"


def rgb(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def draw(name, grid, palette, overlays=()):
    """Writes a stage texture from rows of palette letters; overlays are (grid, x, y) pasted on top."""
    assert len(grid) == 16 and all(len(row) == 16 for row in grid), (name, [len(r) for r in grid])
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for layer, ox, oy in [(grid, 0, 0), *overlays]:
        for y, row in enumerate(layer):
            for x, ch in enumerate(row):
                if ch != "." and 0 <= x + ox < 16 and 0 <= y + oy < 16:
                    img.putpixel((x + ox, y + oy), (*rgb(palette[ch]), 255))
    path = f"{A}/textures/block/{name}.png"
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def write_model(name, model):
    with open(f"{A}/models/block/{name}.json", "w") as f:
        json.dump(model, f, indent=2)
        f.write("\n")


# Plants built in 3D: leaves as flat planes tilted out from the stem, and a solid heart such as a cabbage head.
# The farmland's top is a pixel below the block, so the ground these stand on is y = -1.

GROUND = -1


def leaf_grid(width, height):
    """A broad leaf seen flat, tip at the top and stalk at the bottom: lighter toward the sunny tip, darker toward the
    stalk and along the edge, with a pale midrib and veins running up and out from it, and a gently scalloped edge."""
    import math
    centre = (width - 1) / 2
    grid = []
    for y in range(height):
        t = y / max(1, height - 1)
        half = width / 2 * math.sqrt(max(0.0, 1 - ((t - 0.4) / 0.6) ** 2))
        row = []
        for x in range(width):
            dx = abs(x - centre)
            # Scallops: the edge dips in every third pixel down the leaf.
            edge = half - (0.6 if (y + (x > centre)) % 3 == 0 else 0)
            if dx > edge and not (dx < 0.6 and y >= height - 2):
                row.append(".")
                continue
            tone = "d" if t < 0.3 else "c" if t < 0.7 else "b"
            if dx > edge - 1:
                tone = {"d": "c", "c": "b", "b": "a"}[tone]
            row.append(tone)
        grid.append(row)
    lighter = {"a": "b", "b": "c", "c": "d", "d": "e", "e": "f", "f": "f"}
    mid = round(centre)
    for y in range(1, height):
        if grid[y][mid] != ".":
            grid[y][mid] = lighter[grid[y][mid]]
        # Veins every third row, sweeping up and out to each side.
        if y % 3 == 1:
            for side in (-1, 1):
                for k in (1, 2, 3):
                    x, vy = mid + side * k, y - k
                    if 0 <= x < width and 0 <= vy and grid[vy][x] not in ".":
                        grid[vy][x] = lighter[grid[vy][x]]
    return ["".join(row) for row in grid]


def leaf(texture, size, heading, tilt, out=0.0, rise=0.0, ground=GROUND, v=0):
    """A leaf of size (width, length) pixels growing out from the plant's centre, its stalk `out` pixels from the
    centre and `rise` above the ground, tilted up by `tilt` degrees and turned to face `heading` degrees round the plant.
    The texture is drawn at one texel per pixel, so its pixels match the rest of the plant, starting `v` rows down,
    so a long leaf can be built from pieces of one texture."""
    import math
    w, length = size
    # Laid flat pointing north from the stalk, then tilted up and turned round the plant's centre.
    # Tilting swings the stalk up round the centre, so start it further out and lower to land it where it should be.
    reach = out / math.cos(math.radians(tilt))
    root_z = 8 - reach
    y = ground + rise - reach * math.sin(math.radians(tilt))
    face = {"uv": [0, v, w, v + length], "texture": "#" + texture}
    return {"from": [8 - w / 2, y, root_z - length], "to": [8 + w / 2, y, root_z],
            "rotation": {"origin": [8, y, 8], "x": tilt, "y": heading, "z": 0},
            "faces": {"up": face, "down": face}}


def stalk(texture, length, heading, tilt, out=0.0, ground=GROUND, column=0, width=1):
    """A stalk or tube `width` pixels thick, rising `length` pixels from `out` pixels north of the centre, leaning out
    by `tilt` degrees and turned to `heading`. Its sides show `width` columns of the texture, bottom row at the ground."""
    side = {"uv": [column, 16 - length, column + width, 16], "texture": "#" + texture}
    tip = {"uv": [column, 16 - length, column + width, 16 - length + width], "texture": "#" + texture}
    return {"from": [8 - width / 2, ground, 8 - width / 2 - out], "to": [8 + width / 2, ground + length, 8 + width / 2 - out],
            "rotation": {"origin": [8, ground, 8], "x": -tilt, "y": heading, "z": 0},
            "faces": {"north": side, "south": side, "east": side, "west": side, "up": tip}}


def tuft(texture, size, at, heading=0.0, lean=0.0, ground=GROUND):
    """A single stem drawn on two small crossed planes of (width, height) pixels standing at (x, z), turned to
    `heading` and leaning by `lean` degrees, for grasses and grain. The texture's top left (width x height) is used."""
    w, h = size
    x, z = at
    face = {"uv": [0, 16 - h, w, 16], "texture": "#" + texture}
    rotation = {"origin": [x, ground, z], "x": lean, "y": heading, "z": 0}
    return [
        {"from": [x - w / 2, ground, z], "to": [x + w / 2, ground + h, z], "rotation": rotation, "shade": False,
         "faces": {"north": face, "south": face}},
        {"from": [x, ground, z - w / 2], "to": [x, ground + h, z + w / 2], "rotation": rotation, "shade": False,
         "faces": {"east": face, "west": face}},
    ]


def solid(start, end, side, top=None, rotation=None):
    """A small solid part, such as a fruit or berry, its faces mapped one texel per pixel from the textures' corners."""
    (x0, y0, z0), (x1, y1, z1) = start, end
    dx, dy, dz = x1 - x0, y1 - y0, z1 - z0
    top = top or side
    faces = {"north": {"uv": [0, 0, dx, dy], "texture": "#" + side}, "south": {"uv": [0, 0, dx, dy], "texture": "#" + side},
             "east": {"uv": [0, 0, dz, dy], "texture": "#" + side}, "west": {"uv": [0, 0, dz, dy], "texture": "#" + side},
             "up": {"uv": [0, 0, dx, dz], "texture": "#" + top}, "down": {"uv": [0, 0, dx, dz], "texture": "#" + side}}
    element = {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces}
    if rotation:
        element["rotation"] = rotation
    return element


def padded(grid):
    """A grid smaller than 16x16, placed in the texture's top left corner."""
    return [row.ljust(16, ".") for row in grid] + ["." * 16] * (16 - len(grid))


def bottom_padded(grid):
    """A grid smaller than 16x16, placed in the texture's bottom left corner, so its bottom row is the ground."""
    return ["." * 16] * (16 - len(grid)) + [row.ljust(16, ".") for row in grid]


def recolour(grid, mapping):
    return ["".join(mapping.get(ch, ch) for ch in row) for row in grid]


def built_model(textures, elements):
    return {"parent": "minecraft:block/block", "ambientocclusion": False,
            "textures": {k: (v if ":" in v else NS + v) for k, v in textures.items()}, "elements": elements}


TRELLIS_FRAME = [
    {"from": [1, 0, 7], "to": [3, 16, 9]}, {"from": [13, 0, 7], "to": [15, 16, 9]},
    {"from": [1, 5, 7.5], "to": [15, 6, 8.5]}, {"from": [1, 10, 7.5], "to": [15, 11, 8.5]}, {"from": [1, 15, 7.5], "to": [15, 16, 8.5]},
]


def trellis_planes(texture, bottom=0):
    """The vine flat against both faces of the trellis, the way a climber grows on one; `bottom` 16 for the trellis above."""
    face = {"uv": [0, 0, 16, 16], "texture": texture}
    return [{"from": [0, bottom, z], "to": [16, bottom + 16, z], "shade": False, "faces": {"north": face, "south": face}}
            for z in (6.9, 9.1)]


# Shared pieces for building plants.

import math as _m

STEP_LIGHTER = {"a": "b", "b": "c", "c": "d", "d": "e", "e": "f", "f": "g", "g": "g"}
GRASS = 0  # bushes stand on grass, a full block, not on farmland


def ring_of(texture, size, count, tilt, out=0.0, rise=0.0, turn=0.0, ground=GROUND):
    """Leaves of one kind evenly round the plant."""
    return [leaf(texture, size, turn + i * 360 / count, tilt, out, rise, ground) for i in range(count)]


def on_stalks(texture, size, count, tilt, rise, lean, turn=0.0, ground=GROUND, stalk_texture="stalk"):
    """Leaves held up on stalks: each stalk leans out from the crown and its leaf opens from the stalk's top."""
    parts = []
    length = rise / _m.cos(_m.radians(lean))
    out = rise * _m.tan(_m.radians(lean))
    for i in range(count):
        heading = turn + i * 360 / count
        parts.append(stalk(stalk_texture, round(length), heading, lean, ground=ground))
        parts.append(leaf(texture, size, heading, tilt, out, rise, ground))
    return parts


def ball(diameter, height, side, top, ground=GROUND, sink=0, centre=(8, 8)):
    """A round body built from one-pixel slices, each as wide as a ball of that diameter is at its height, squashed or
    stretched to `height`, with `sink` pixels of it buried so it can sit in the soil. Each slice is a square and the
    same square turned 45 degrees, so from above it is eight-pointed and reads as round rather than as a square
    ziggurat. Each slice shows the side texture's row at its own height, counted up from the ball's bottom; the rim
    it shows past the slice above is coloured like the slice, so the steps read as one rounded surface, and only the
    topmost slice shows the top texture."""
    cx, cz = centre
    half = height / 2
    parts = []
    for i in range(height):
        y0 = ground - sink + i
        if y0 + 1 <= ground:
            continue
        t = (i + 0.5 - half) / half
        # Squares a little smaller than the circle, so the eight points sit on it and the notches just inside.
        w = max(1, round(diameter * 0.88 * _m.sqrt(max(0.0, 1 - t * t))))
        lo, hi = 8 - w / 2, 8 + w / 2
        side_face = {"uv": [lo, 15 - i, hi, 16 - i], "texture": "#" + side}
        faces = {d: side_face for d in ("north", "south", "east", "west")}
        faces["up"] = {"uv": [lo, lo, hi, hi], "texture": "#" + top} if i == height - 1 else side_face
        if t < 0:
            faces["down"] = side_face
        box = {"from": [cx - w / 2, y0, cz - w / 2], "to": [cx + w / 2, y0 + 1, cz + w / 2], "faces": faces}
        parts.append(box)
        if w > 1:
            parts.append({**box, "rotation": {"origin": [cx, y0, cz], "axis": "y", "angle": 45}})
    return parts


def ball_top(diameter, height, ground=GROUND, sink=0):
    """Where the top of a ball() is."""
    return ground - sink + height


def rod(x, z, y0, y1, texture, column=0):
    """A thin upright stalk a pixel thick from y0 to y1 at (x, z), showing one column of the texture, stacked from
    pieces at most 16 pixels high when it is taller than the texture. Returns one element, or a list when stacked."""
    pieces = []
    y = y0
    while y < y1:
        h = min(16, y1 - y)
        side = {"uv": [column, 16 - h, column + 1, 16], "texture": "#" + texture}
        pieces.append({"from": [x - 0.5, y, z - 0.5], "to": [x + 0.5, y + h, z + 0.5],
                       "faces": {d: side for d in ("north", "south", "east", "west")}})
        y += h
    return pieces[0] if len(pieces) == 1 else pieces


def truss(x, z, top, fruits, stem="stalk"):
    """A truss of small round fruit hanging in a zigzag down a stalk from `top`, each fruit's texture named in turn."""
    parts = [rod(x, z, top - 2.2 * len(fruits) + 1, top, stem)]
    for i, fruit in enumerate(fruits):
        fx, fy = x + (0.9 if i % 2 else -0.9), top - 2.2 * (i + 1)
        parts.append(solid((fx - 1, fy, z - 1), (fx + 1, fy + 2, z + 1), fruit))
    return parts


def column(x, z, y0, y1, texture, width=2):
    """A solid upright stem `width` pixels thick from y0 to y1 at (x, z), taller than a texture if need be: it is
    stacked from pieces at most 16 pixels high, each showing the bottom of the texture's first `width` columns."""
    parts = []
    y = y0
    while y < y1:
        h = min(16, y1 - y)
        side = {"uv": [0, 16 - h, width, 16], "texture": "#" + texture}
        faces = {d: side for d in ("north", "south", "east", "west")}
        if y + h >= y1:
            faces["up"] = {"uv": [0, 0, width, width], "texture": "#" + texture}
        parts.append({"from": [x - width / 2, y, z - width / 2], "to": [x + width / 2, y + h, z + width / 2], "faces": faces})
        y += h
    return parts


def arching_leaf(texture, width, heading, y, base, tip, rise_tilt=50, droop=-15, out=1.0):
    """A long leaf that rises from the stem and arches over: a base piece tilted up and a tip piece drooping from its
    end, cut from one texture whose top `tip` rows are the tip and whose next `base` rows are the base."""
    first = leaf(texture, (width, base), heading, rise_tilt, out, y - GROUND, v=tip)
    end_out = out + base * _m.cos(_m.radians(rise_tilt))
    end_rise = y - GROUND + base * _m.sin(_m.radians(rise_tilt))
    second = leaf(texture, (width, tip), heading, droop, end_out, end_rise)
    return [first, second]


def bunch(x, z, top, rows, berry, stem="stalk"):
    """A bunch of grapes hanging from `top`: rows of small round berries, as many in each row as `rows` says, staggered
    in and out so the bunch has depth, narrowing to its tip."""
    parts = [rod(x, z, top - 1, top, stem)]
    size = 1.5
    for r, n in enumerate(rows):
        y = top - 1 - size * (r + 1)
        for j in range(n):
            bx = x + (j - (n - 1) / 2) * 1.4
            bz = z + (0.35 if (r + j) % 2 else -0.35)
            parts.append(solid((bx - size / 2, y, bz - size / 2), (bx + size / 2, y + size, bz + size / 2), berry))
    return parts


def stalk_strip(tones):
    """A texture whose first columns are stalks or tubes, read bottom-up by stalk(): one column per entry."""
    return ["".join(column[y] for column in tones).ljust(16, ".") for y in range(16)]


def shaded_side(palette_letters, height, light_columns=(2, 3, 4, 5)):
    """A side texture for a ball() of `height`: mostly one colour, so its slices read as one rounded body, darker at
    its foot, with a band of light down one side, read from the bottom row up. palette_letters runs from darkest to
    lightest: its foot, just above its foot, its body, and the light."""
    dark, mid, light, bright = palette_letters
    rows = []
    for y in range(16):
        f = (15 - y + 0.5) / height
        row = ""
        for x in range(16):
            tone = dark if f <= 0.12 else mid if f <= 0.3 else light
            if x in light_columns and 0.3 < f < 0.9:
                tone = bright
            row += tone
        rows.append(row)
    return rows


def build(plant, palette, textures, stages, particle, extra=None, tall=None):
    """Draws a plant's textures and writes one model per growth stage. `extra` names textures it borrows. A vine that
    climbs a second trellis gives `tall`, its two-block models by stage, written as <plant>_stage<age>_tall, with a
    blockstate choosing them when the vine is tall."""
    for name, grid in textures.items():
        draw(f"{plant}_{name}", grid, palette)
    names = {"particle": f"{plant}_{particle}", **{name: f"{plant}_{name}" for name in textures}, **(extra or {})}

    def model(name, elements):
        used = {"particle": names["particle"]}
        for element in elements:
            for face in element["faces"].values():
                key = face["texture"][1:]
                used[key] = names[key]
        write_model(name, built_model(used, elements))
    for age, elements in enumerate(stages):
        model(f"{plant}_stage{age}", elements)
    if tall is not None:
        for age, elements in tall.items():
            model(f"{plant}_stage{age}_tall", elements)
        variants = {f"age={age},tall={str(t).lower()}": {"model": NS + f"{plant}_stage{age}" + ("_tall" if t and age in tall else "")}
                    for age in range(len(stages)) for t in (False, True)}
        with open(f"{A}/blockstates/{plant}.json", "w") as f:
            json.dump({"variants": variants}, f, indent=2)
            f.write("\n")


# Cabbage: seed leaves, a rosette of broad blue-green leaves, then a smooth, pale, round head cupped by paler wrapper
# leaves, in big outer leaves curving up round it.

CABBAGE = {"a": "#1e3a30", "b": "#2c5440", "c": "#3e6e52", "d": "#58896a", "e": "#86ae8a", "f": "#b6d48e",
           "g": "#d6ecb2"}


def cabbage(plant="cabbage", palette=CABBAGE):
    side = shaded_side("deff", 9)
    # Two wrapper-leaf edges curving round the head, a shade darker than the leaf they lie on.
    for x in range(6, 16):
        y = 15 - round(9 * (0.74 - (x - 6) * 0.025))
        side[y] = side[y][:x] + "f" + side[y][x + 1:]
    for x in range(0, 9):
        y = 15 - round(9 * (0.42 + x * 0.025))
        side[y] = side[y][:x] + "e" + side[y][x + 1:]
    top = []
    for y in range(16):
        row = ""
        for x in range(16):
            r = _m.hypot(x - 7.5, y - 7.5)
            row += "f" if r > 5.5 else "g"
        top.append(row)
    for cx, cy, r, a0, a1 in ((5.5, 9, 3.5, -100, 60), (10, 6.5, 3, 80, 250)):
        for a in range(a0, a1, 8):
            x, y = round(cx + r * _m.cos(_m.radians(a))), round(cy + r * _m.sin(_m.radians(a)))
            top[y] = top[y][:x] + "f" + top[y][x + 1:]

    def paler(grid, steps):
        for _ in range(steps):
            grid = recolour(grid, STEP_LIGHTER)
        return grid
    textures = {"seed_leaf": padded(leaf_grid(4, 4)), "small_leaf": padded(leaf_grid(6, 7)),
                "large_leaf": padded(leaf_grid(10, 10)), "wrapper_leaf": padded(paler(leaf_grid(8, 8), 2)),
                "head_side": side, "head_top": top}
    S, L, W = (6, 7), (10, 10), (8, 8)

    def head(d, h):
        return ball(d, h, "head_side", "head_top", sink=1)
    stages = [
        ring_of("seed_leaf", (4, 4), 2, 35, rise=1),
        ring_of("seed_leaf", (4, 4), 2, 30, rise=1) + ring_of("small_leaf", S, 2, 50, turn=90),
        ring_of("small_leaf", S, 5, 35),
        ring_of("small_leaf", S, 3, 40, turn=60) + ring_of("large_leaf", L, 3, 30),
        head(4, 4) + ring_of("small_leaf", S, 4, 55, out=1.5) + ring_of("large_leaf", L, 4, 32, out=1, turn=45),
        head(6, 6) + ring_of("wrapper_leaf", W, 4, 60, out=2.5) + ring_of("large_leaf", L, 5, 35, out=1.5, turn=36),
        head(8, 8) + ring_of("wrapper_leaf", W, 4, 62, out=3.5, turn=45) + ring_of("large_leaf", L, 5, 38, out=2.5),
        head(10, 9) + ring_of("wrapper_leaf", W, 5, 65, out=4.5, turn=20) + ring_of("large_leaf", L, 6, 40, out=3.5),
    ]
    build(plant, palette, textures, stages, "large_leaf")


# Red cabbage: the same plant, its leaves and head a waxy purple shading to blue-green on the outer leaves.
RED_CABBAGE = {"a": "#2a2438", "b": "#3c3050", "c": "#56426c", "d": "#6e4e86", "e": "#8a5e9a", "f": "#a874b0", "g": "#c898c8"}


# Lettuce: a loose rosette of pale, frilly leaves, no head, then bolting to a tall stalk of small yellow flowers.
LETTUCE = {"a": "#3a6a28", "b": "#4e8a32", "c": "#68a640", "d": "#88c050", "e": "#a8d670", "f": "#c8e898",
           "g": "#e0f4bc", "Y": "#f0d84a", "Z": "#c8a830"}


def frilly(grid):
    """A leaf with a ruffled edge: every other pixel along its rim pushed out a pixel, a shade lighter."""
    rows = [list(r) for r in grid]
    for y, row in enumerate(rows):
        filled = [x for x, ch in enumerate(row) if ch != "."]
        if filled and y % 2 == 0:
            for x in (filled[0] - 1, filled[-1] + 1):
                if 0 <= x < len(row):
                    row[x] = "e"
    return ["".join(r) for r in rows]


def lettuce():
    textures = {"seed_leaf": padded(leaf_grid(4, 4)), "small_leaf": padded(frilly(recolour(leaf_grid(5, 6), STEP_LIGHTER))),
                "leaf": padded(frilly(recolour(leaf_grid(7, 8), STEP_LIGHTER))),
                "heart": padded(frilly(recolour(recolour(leaf_grid(5, 6), STEP_LIGHTER), STEP_LIGHTER))),
                "stalk": stalk_strip(["cdccdcccdccdccdc"]), "flower": padded([".Y.", "YZY", ".Y."])}
    M, S, H = (7, 8), (5, 6), (5, 6)

    def rosette(outer, inner, out=0.0, tilt=40):
        return ring_of("leaf", M, outer, tilt, out=out) + ring_of("heart", H, inner, 70, turn=30)

    def bolted():
        # A tall stalk with a few small leaves up it and a spray of small yellow flowers at the top.
        parts = ring_of("leaf", M, 4, 25, out=1) + [stalk("stalk", 15, 0, 0)]
        for k, y in enumerate((5, 9, 12)):
            parts.append(leaf("small_leaf", S, k * 120, 35, 0, y - GROUND))
        for heading, out, y in [(0, 0, 15), (60, 2, 14), (180, 2, 13), (280, 2, 14.5), (120, 1.5, 12)]:
            x, z = 8 + out * _m.sin(_m.radians(heading)), 8 - out * _m.cos(_m.radians(heading))
            face = {"uv": [0, 0, 3, 3], "texture": "#flower"}
            parts.append({"from": [x - 1.5, GROUND + y, z - 1.5], "to": [x + 1.5, GROUND + y, z + 1.5], "shade": False,
                          "faces": {"up": face, "down": face}})
        return parts
    stages = [
        ring_of("seed_leaf", (4, 4), 2, 35, rise=1),
        ring_of("seed_leaf", (4, 4), 2, 30, rise=1) + ring_of("small_leaf", S, 2, 50, turn=90),
        ring_of("small_leaf", S, 5, 45),
        rosette(4, 2), rosette(5, 3, out=0.5), rosette(6, 3, out=1, tilt=35), rosette(7, 4, out=1.5, tilt=32),
        bolted(),
    ]
    build("lettuce", LETTUCE, textures, stages, "leaf")


# Tomato: a seedling, a young plant, then a vine up the trellis that flowers, sets trusses of green cherry tomatoes,
# and ripens them through orange to red, green, orange and red together on each truss.

TOMATO = {"a": "#24502a", "b": "#346a34", "c": "#4c8a40", "d": "#6aa84c", "e": "#98c868",
          "S": "#a82634", "T": "#d8442e", "U": "#ff9a80", "O": "#e86a24", "P": "#f8a060", "Q": "#c0501c",
          "G": "#4c7a26", "H": "#7cae3c", "I": "#b8dc78", "Y": "#e8c234", "Z": "#b07a1c"}
TOMATO_STAGES = {
    "seedling": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "......ed........",
        "....edcb.de.....",
        "......b.dcb.....",
        ".......bb.......",
        ".......b........",
    ],
    "young": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "........ed......",
        "......edcb......",
        "...ed...b..ed...",
        "..dcbb..b.dccb..",
        "....bbbbb.bb....",
        ".ed....b........",
        "dccb...bbbed....",
        ".cbbb.bb.dcb....",
        ".....bb.........",
        ".......b........",
    ],
    "vine": [
        ".......edd......",
        "..ed..edcbc.ed..",
        ".edcd..cb..edcd.",
        "..cbbb.b.bbcbc..",
        "......bb.b......",
        ".ed...b...edd...",
        "edcd..bb.edcbc..",
        ".cbcbb.bbb.cb...",
        "......b.........",
        "...edd.b..edd...",
        "..edcbcbbedcdc..",
        "...cb..b..cbc...",
        ".......bb.......",
        "..edd...b.edd...",
        ".edcbcb.bedcbc..",
        "..cb..bb..cb....",
    ],
}
TOMATO_FLOWERS = [(4, 4), (12, 8), (0, 13), (13, 0), (7, 10)]


def trellis_frame():
    return [{**part, "faces": {side: {"texture": "#post"} for side in ("north", "south", "east", "west", "up", "down")}}
            for part in TRELLIS_FRAME]


def tomato():
    vine = TOMATO_STAGES["vine"]
    flowering = [list(row) for row in vine]
    for x, y in TOMATO_FLOWERS:
        for dx, dy, ch in ((0, 0, "Y"), (2, 0, "Y"), (1, 1, "Z"), (0, 2, "Y"), (2, 2, "Y")):
            if 0 <= x + dx < 16 and 0 <= y + dy < 16:
                flowering[y + dy][x + dx] = ch
    textures = {"seedling": TOMATO_STAGES["seedling"], "young": TOMATO_STAGES["young"], "vine": vine,
                "flowering": ["".join(r) for r in flowering], "stalk": stalk_strip(["bcbbcbbbcbbcbbcb"]),
                "red": padded(["UT", "TS"]), "orange": padded(["PO", "OQ"]), "green": padded(["IH", "HG"])}

    def on_trellis(stage, trusses=()):
        parts = trellis_frame() + trellis_planes("#" + stage)
        for t in trusses:
            parts += truss(*t)
        return parts
    front, back = 10.4, 5.6
    r, o, g = "red", "orange", "green"
    stages = [
        on_trellis("seedling"), on_trellis("seedling"), on_trellis("young"), on_trellis("young"),
        on_trellis("vine"), on_trellis("vine"),
        on_trellis("flowering", [(5, front, 14.5, [g, g, g]), (11.5, front, 9.5, [g, g]), (9, back, 13, [g, g, g])]),
        on_trellis("vine", [(4.5, front, 14.5, [r, r, o, g]), (9, front, 15, [r, r, r, o, g, g]),
                            (12.5, front, 9.5, [r, o, g]), (6, back, 14, [r, r, o, o, g]), (11, back, 10, [r, r, g])]),
    ]

    def climbed(stage, trusses=()):
        # Over both trellises: the climbed one above shows itself, so only the vine is drawn there.
        return on_trellis(stage, trusses) + trellis_planes("#" + stage, 16)
    tall = {
        4: climbed("vine"), 5: climbed("vine"),
        6: climbed("flowering", [(5, front, 24, [g, g, g]), (11.5, front, 18, [g, g]), (8, front, 12, [g, g]), (9, back, 22, [g, g, g])]),
        7: climbed("vine", [(4.5, front, 23, [r, r, o, g, g]), (9, front, 28, [r, r, r, o, g]), (12.5, front, 17, [r, o, g, g]),
                            (6, front, 12, [r, r, o]), (6, back, 25, [r, r, o, o, g]), (11, back, 19, [r, r, g, g]), (9, back, 11, [r, o])]),
    }
    build("tomato_vine", TOMATO, textures, stages, "vine", extra={"post": NS + "trellis_wood"}, tall=tall)


# Barley: grassy shoots, then slender stems with long arching leaves, then slim ears with long, parallel beards that
# turn pale and ripen gold, nodding.

BARLEY = {"a": "#26401e", "b": "#355824", "c": "#4a7030", "d": "#6a9440", "e": "#8cb458", "f": "#b0cc78",
          "h": "#8a6a24", "i": "#b8923a", "j": "#d8b85a", "k": "#ecd88a"}
BARLEY_SHOOT = ["..e..", ".ed.e", "e.d.d", "d.cdc", ".dcc.", "..c.."]
BARLEY_LEAFY = ["...e...", "...d...", "...d.e.", "...cd.e", ".e.c...", "e.dc...", "...c...", "...cdd.", "...c..e",
                ".ddc...", "e..c...", "...c...", "...c..."]
BARLEY_EARED = ["..e.e..", "..e.e..", "..e.e..", "..eee..", "..fdf..", "..dfd..", "..fdf..", "..dfd..", "...d...",
                "...c...", "...cdd.", "...c..e", ".ddc...", "e..c...", "...c...", "...c..."]
BARLEY_SPOTS = [(4.5, 4.5), (11, 4), (8, 8), (4, 11.5), (12, 11), (8, 13), (10.5, 7.5), (6, 8.5)]
BARLEY_TURNS = [10, 55, 100, 145, 30, 80, 125, 170]
BARLEY_LEANS = [6, -8, 4, -5, 9, -7, 5, -6]


def barley(plant="barley", palette=BARLEY, eared=BARLEY_EARED):
    textures = {"shoot": bottom_padded(BARLEY_SHOOT), "leafy": bottom_padded(BARLEY_LEAFY),
                "eared": bottom_padded(eared),
                "turning": bottom_padded(recolour(eared, {"f": "k", "e": "f"})),
                "ripe": bottom_padded(recolour(eared, {"e": "k", "f": "j", "d": "i", "c": "i", "b": "h"}))}

    def clump(texture, size, count, droop=0):
        parts = []
        for i in range(count):
            lean = BARLEY_LEANS[i] + (droop if BARLEY_LEANS[i] > 0 else -droop)
            parts += tuft(texture, size, BARLEY_SPOTS[i], BARLEY_TURNS[i], lean)
        return parts
    stages = [clump("shoot", (5, 6), 3), clump("shoot", (5, 6), 6), clump("leafy", (7, 13), 5), clump("leafy", (7, 13), 6),
              clump("leafy", (7, 13), 8), clump("eared", (7, 16), 8), clump("turning", (7, 16), 8),
              clump("ripe", (7, 16), 8, droop=10)]
    build(plant, palette, textures, stages, "leafy")


# Rye: taller and bluer than barley, its ear long and slender with short beards, ripening a greyish tan.
RYE = {"a": "#24402e", "b": "#33583e", "c": "#487052", "d": "#668e6a", "e": "#86aa84", "f": "#a8c4a0",
       "h": "#7a6a48", "i": "#a8946a", "j": "#c8b48a", "k": "#e0d4b0"}
RYE_EARED = ["..e..", ".e.e.", "..f..", ".fdf.", "..d..", ".fdf.", "..d..", ".fdf.", "..d..", ".fdf.", "..d..", "..c..",
             "..cd.", ".dc..", "..c..", "..c.."]


# Onion: tall, hollow, blue-green leaves fanning from a pale neck over a round bulb sitting on the soil, the leaves
# yellowing and flopping over when it is ready to lift. The red onion is the same plant in red.

ONION = {"b": "#2e5a3a", "c": "#457a4c", "d": "#5e9660", "e": "#86b47c", "f": "#c8dcb0", "w": "#eef0dc",
         "h": "#8a7a3a", "i": "#b0a050", "j": "#c8b868",
         "m": "#6e3e1a", "n": "#a0622a", "o": "#c88a44", "p": "#e0b070", "q": "#f0d4a0"}
RED_ONION = {**ONION, "m": "#4a1430", "n": "#7a2048", "o": "#9e3460", "p": "#c0587e", "q": "#dc90aa"}
# Garlic: a papery white bulb streaked with purple, sitting mostly underground under flat grey-green leaves.
GARLIC = {**ONION, "n": "#b8a8b8", "o": "#e4dcd4", "p": "#f2ece4", "q": "#fffaf2"}
# Shallot: a cluster of small, coppery bulbs.
SHALLOT = {**ONION, "m": "#5a2e1a", "n": "#8a4a2a", "o": "#b06a40", "p": "#cc8a5e", "q": "#e8b08a"}
SHALLOT_CLUSTER = ((-1.6, -1.2, 0.62), (1.7, -0.6, 0.6), (0.1, 1.8, 0.58))


def onion_build(plant, palette, bulbs=((0, 0, 1.0),), sink=1):
    """An onion-like plant: bulbs is (dx, dz, scale) for each bulb, so a shallot grows a cluster; sink is how deep the
    bulbs sit, deeper for garlic, which is mostly underground."""
    side = shaded_side("nnop", 8)
    side = ["".join("n" if x % 4 == 1 and ch in "op" else ch for x, ch in enumerate(row)) for row in side]
    top = ["".join("f" if _m.hypot(x - 7.5, y - 7.5) < 1.5 else "q" if _m.hypot(x - 7.5, y - 7.5) < 2.6 else "p"
                   for x in range(16)) for y in range(16)]
    textures = {"leaf": stalk_strip(["dddcdddcdddcddde", "jijhjijijhjiiihh", "eeiedeiededdcdde"]),
                "neck": padded(["fw", "fw", "fw", "fw", "fw"]), "bulb_side": side, "bulb_top": top}

    def fan(count, length, tilt, ground, column=0, turn=0):
        return [stalk("leaf", length, turn + i * 360 / count, tilt, ground=ground, column=column) for i in range(count)]

    def plant_with_bulb(d, h, neck, leaves):
        parts = []
        for dx, dz, scale in bulbs:
            bd, bh = max(2, round(d * scale)), max(2, round(h * scale))
            parts += ball(bd, bh, "bulb_side", "bulb_top", sink=sink, centre=(8 + dx, 8 + dz))
        top_y = ball_top(d, h, sink=sink) if len(bulbs) == 1 else GROUND + 1
        return parts + [solid((7, top_y, 7), (9, top_y + neck, 9), "neck")] + leaves(top_y + neck)
    stages = [
        fan(2, 5, 8, GROUND), fan(3, 7, 10, GROUND), fan(4, 9, 12, GROUND), fan(5, 11, 12, GROUND),
        plant_with_bulb(4, 4, 2, lambda y: fan(5, 12, 12, y)),
        plant_with_bulb(6, 6, 3, lambda y: fan(6, 13, 13, y)),
        plant_with_bulb(7, 7, 3, lambda y: fan(5, 13, 13, y) + fan(2, 11, 40, y, column=2, turn=45)),
        plant_with_bulb(9, 8, 2, lambda y: fan(5, 10, 76, y, column=1, turn=20)),
    ]
    build(plant, palette, textures, stages, "bulb_side")


# Spring onion: thin, upright tube leaves from a slim white base, no bulb, then a tall stalk topped with a round white
# flower head when it goes to seed.

def spring_onion():
    textures = {"leaf": stalk_strip(["dddcdddcdddcdddw", "cdddcdddcdddcwww"]), "base": padded(["ww", "fw", "ww", "fw"]),
                "flower_side": shaded_side("efww", 4, light_columns=(5, 6)),
                "flower_top": ["".join("w" if (x + y) % 3 else "f" for x in range(16)) for y in range(16)]}

    def fan(count, length, lean, turn=0):
        return ([solid((7.25, GROUND, 7.25), (8.75, GROUND + 3, 8.75), "base")]
                + [stalk("leaf", length, turn + i * 360 / count, lean, ground=GROUND, column=i % 2) for i in range(count)])
    stages = [fan(2, 4, 6), fan(3, 6, 8), fan(4, 8, 8), fan(4, 10, 9), fan(5, 11, 9), fan(5, 13, 10), fan(6, 14, 10),
              fan(4, 11, 14) + [stalk("leaf", 15, 0, 0, column=0)]
              + ball(4, 4, "flower_side", "flower_top", ground=GROUND + 14)]
    build("spring_onion", ONION, textures, stages, "leaf")


# Radish: seed leaves, then oval leaves held up on pale stalks over a round, bright red root sitting on the soil.

RADISH = {"a": "#2c6a2c", "b": "#3e8a3a", "c": "#58a648", "d": "#78c060", "e": "#a0d880", "f": "#c8ecaa",
          "r": "#8a1430", "s": "#c41e3a", "t": "#e8384e", "u": "#f89aa0", "w": "#f4e4e0"}


def radish():
    side = shaded_side("rsst", 6)
    side = [row if y < 15 else "w" * 16 for y, row in enumerate(side)]
    side = [row.replace("u", "t") if 0 < (15 - y) < 2 else row for y, row in enumerate(side)]
    top = ["".join("c" if _m.hypot(x - 7.5, y - 7.5) < 1.3 else "t" if _m.hypot(x - 7.5, y - 7.5) < 3 else "s"
                   for x in range(16)) for y in range(16)]
    textures = {"seed_leaf": padded(leaf_grid(4, 4)), "leaf": padded(leaf_grid(5, 7)), "small_leaf": padded(leaf_grid(4, 5)),
                "stalk": stalk_strip(["eefeefeeefeefeef"]), "root_side": side, "root_top": top}

    def with_root(d, h, sink, count, rise, lean, turn=0):
        return (ball(d, h, "root_side", "root_top", sink=sink)
                + on_stalks("leaf", (5, 7), count, 55, rise, lean, turn, ground=ball_top(d, h, sink=sink) - 1))
    stages = [
        ring_of("seed_leaf", (4, 4), 2, 30),
        ring_of("seed_leaf", (4, 4), 2, 25) + on_stalks("small_leaf", (4, 5), 2, 50, 3, 15, turn=90),
        on_stalks("small_leaf", (4, 5), 4, 55, 4, 15),
        on_stalks("leaf", (5, 7), 5, 55, 5, 18),
        with_root(3, 3, 0, 5, 6, 15),
        with_root(4, 4, 0, 6, 7, 15, 20),
        with_root(5, 5, 0, 6, 7, 18, 10),
        with_root(6, 6, 1, 7, 8, 20, 30),
    ]
    build("radish", RADISH, textures, stages, "leaf")


# Courgette: seed leaves, then big, deeply lobed, silver-mottled leaves held high on long stalks, big yellow star
# flowers, and glossy dark fruit lying at the base.

COURGETTE = {"a": "#1a3e22", "b": "#28582c", "c": "#3a743a", "d": "#52904a", "e": "#62925a", "f": "#86a87e",
             "Y": "#f0c020", "Z": "#d08818", "X": "#fff0a0",
             "F": "#1a3c1a", "G": "#2a5a28", "H": "#467e3a", "I": "#8ab86a"}


def courgette():
    fruit = ["".join("I" if (x * 7 + y * 13) % 11 == 0 else "H" if (x * 3 + y * 5) % 7 == 0 else "G" if (x + y) % 3 else "F"
                     for x in range(16)) for y in range(16)]
    # Five pointed lobes, the veins pale, silver patches between them near the heart.
    big = ["....d....", "d...c...d", ".cc.c.cc.", ".cccecfc.", "ccfcecccc", ".cceeecc.", "bccfeccfb", ".bc.e.cb.",
           "....c...."]
    huge = [".....d.....", "d....c....d", ".cc..c..cc.", ".cccfcfccc.", "..ccfefcc..", "dcccceccccd", ".cfceeecfc.",
            "bccfcecfccb", "..bcceccb..", ".bb..c..bb.", ".....c....."]
    small = ["...d...", "d..c..d", ".ccecc.", "ccceccf", ".bcecb.", "..b.b..", "...c..."]
    flower = ["...Y...", ".Y.Y.Y.", "..YXY..", "YYXZXYY", "..YXY..", ".Y.Y.Y.", "...Y..."]
    textures = {"seed_leaf": padded(leaf_grid(4, 4)), "small_leaf": padded(small), "big_leaf": padded(big), "huge_leaf": padded(huge),
                "stalk": stalk_strip(["deddeddddeddedde"]), "fruit": fruit, "flower": padded(flower),
                "bud": padded(["ZY", "YZ"])}

    def flower_at(heading, out, height):
        x, z = 8 + out * _m.sin(_m.radians(heading)), 8 - out * _m.cos(_m.radians(heading))
        face = {"uv": [0, 0, 7, 7], "texture": "#flower"}
        return [solid((x - 1, GROUND, z - 1), (x + 1, GROUND + height, z + 1), "bud"),
                {"from": [x - 3.5, GROUND + height, z - 3.5], "to": [x + 3.5, GROUND + height, z + 3.5], "shade": False,
                 "faces": {"up": face, "down": face}}]

    def fruit_at(x, z, width, length, heading):
        return solid((x - width / 2, GROUND, z - length / 2), (x + width / 2, GROUND + width, z + length / 2), "fruit",
                     rotation={"origin": [x, GROUND, z], "x": 0, "y": heading, "z": 0})

    def big_leaves(n, rise=8, turn=0):
        return on_stalks("big_leaf", (9, 9), n, 25, rise, 20, turn)

    def huge_leaves(n, rise, lean, turn=0):
        # Spread wide on long stalks leaning out from the crown, as a grown courgette's are: from its fifth stage it
        # sprawls about a block past each side of its own, while staying under a block high.
        return on_stalks("huge_leaf", (11, 11), n, 12, rise, lean, turn)
    stages = [
        ring_of("seed_leaf", (4, 4), 2, 25),
        ring_of("seed_leaf", (4, 4), 2, 20) + on_stalks("small_leaf", (7, 7), 1, 30, 2, 15, turn=90),
        on_stalks("small_leaf", (7, 7), 3, 30, 3, 20),
        on_stalks("small_leaf", (7, 7), 2, 30, 4, 15, turn=60) + big_leaves(3, 5),
        huge_leaves(5, 6, 35),
        huge_leaves(6, 7, 45) + on_stalks("big_leaf", (9, 9), 3, 30, 8, 15, 60) + flower_at(30, 4, 3) + flower_at(200, 4.5, 2),
        huge_leaves(7, 7, 48, 20) + on_stalks("big_leaf", (9, 9), 3, 30, 8, 15, 80) + flower_at(30, 4, 3) + flower_at(200, 4.5, 2)
        + [fruit_at(9, 11, 2, 5, 70)],
        huge_leaves(7, 7, 48, 20) + on_stalks("big_leaf", (9, 9), 3, 30, 8, 15, 80) + flower_at(250, 4, 3)
        + [fruit_at(9, 11, 3, 9, 70), fruit_at(6, 5, 2, 7, 150), fruit_at(12, 6, 2, 6, 20)],
    ]
    build("courgette", COURGETTE, textures, stages, "big_leaf")


# Strawberry: a mound of three-part, toothed leaves on long stalks, white flowers, then cone-shaped berries hanging
# near the ground, ripe red ones among pale unripe ones.

STRAWBERRY = {"a": "#1c3e20", "b": "#2a5a2c", "c": "#3c7838", "d": "#56964a", "e": "#80b864", "f": "#aad488",
              "R": "#a01828", "S": "#d82a36", "T": "#f05a5a", "Y": "#f0d060", "W": "#f8f8f0", "V": "#d8e4c0",
              "X": "#f0c830", "N": "#b8cc98"}


def strawberry():
    textures = {"leaf": padded(["...ded...", "..dcecd..", ".ddcecdd.", "dedcecded", "ecdcbcdce", "dcbc.cbcd", ".dc...cd.",
                                "....b...."]),
                "small_leaf": padded([".ded.", "edcde", "dcbcd", "..b..", "..b.."]),
                "stalk": stalk_strip(["cdccdcccdccdccdc"]),
                "berry": padded(["TTS", "SYS", "STS", "RSY"]), "unripe": padded(["WVV", "VNV", "VVN", "NVN"]),
                "calyx": padded([".c.", "cdc", ".c."]),
                "flower": padded([".W.W.", "WWVWW", ".VXV.", "WWVWW", ".W.W."])}

    def place(heading, out):
        return 8 + out * _m.sin(_m.radians(heading)), 8 - out * _m.cos(_m.radians(heading))

    def berry(heading, out, kind="berry"):
        x, z = place(heading, out)
        return [rod(x, z, GRASS + 4, GRASS + 7, "stalk"),
                solid((x - 1.5, GRASS + 1, z - 1.5), (x + 1.5, GRASS + 4, z + 1.5), kind, top="calyx"),
                solid((x - 1, GRASS, z - 1), (x + 1, GRASS + 1, z + 1), kind)]

    def flower(heading, out, y):
        x, z = place(heading, out)
        face = {"uv": [0, 0, 5, 5], "texture": "#flower"}
        return [{"from": [x - 2.5, GRASS + y, z - 2.5], "to": [x + 2.5, GRASS + y, z + 2.5], "shade": False,
                 "faces": {"up": face, "down": face}}]

    def leaves(inner, outer):
        # A mound: an inner tier held high and nearly upright, and an outer tier lower and spreading.
        return (on_stalks("leaf", (9, 8), inner, 35, 7, 10, turn=30, ground=GRASS)
                + on_stalks("leaf", (9, 8), outer, 20, 5, 30, ground=GRASS))
    stages = [
        on_stalks("small_leaf", (5, 5), 3, 30, 3, 20, ground=GRASS),
        leaves(2, 4),
        leaves(3, 5) + flower(20, 7, 5) + flower(150, 6.5, 4) + flower(260, 7, 5),
        leaves(3, 4) + berry(20, 9.5) + berry(95, 10, "unripe") + berry(160, 9.5) + berry(235, 10) + berry(305, 9.5, "unripe")
        + flower(150, 6, 5),
    ]
    build("strawberry_bush", STRAWBERRY, textures, stages, "leaf")


# Corn: a thick stalk with long strap leaves arching from each joint, a tassel at the top, and husked cobs whose silk
# turns brown as they ripen. It grows taller than a block, as maize does.

CORN = {"a": "#1e4a1e", "b": "#2c6428", "c": "#3e7e34", "d": "#58983e", "e": "#7cb456", "f": "#a8d07c",
        "h": "#a8883e", "i": "#ccac5a", "j": "#e8d088", "y": "#b8a040", "z": "#d8c868",
        "H": "#78a84a", "I": "#a4c870", "T": "#c0a860", "U": "#dcc890", "k": "#e8c888", "m": "#6a3e1c",
        "K": "#f0c030", "L": "#d89a18"}
BLUE_CORN = {**CORN, "K": "#4a5a9a", "L": "#2e3a6e"}
PURPLE_CORN = {**CORN, "K": "#5a1e4a", "L": "#3a0e30"}


def corn(plant="corn", palette=CORN):
    strap = [".d."] + ["ded"] * 5 + ["cec"] * 6 + ["cdc"] * 4
    husk = ["".join("I" if x % 3 == 0 else "H" for x in range(16)) for _ in range(16)]
    tassel = ["i.j.i", ".iji.", "j.i.j", ".hih.", "..h..", "..h.."]
    textures = {"leaf": padded(strap), "old_leaf": padded(recolour(strap, {"d": "z", "e": "z", "c": "y"})),
                "stalk": stalk_strip(["cdccdcccdccdccdc", "dcdcccdcdcccdcdc"]),
                "tassel": bottom_padded(tassel), "brown_tassel": bottom_padded(recolour(tassel, {"i": "h", "j": "i"})),
                "husk": husk, "silk": padded(["kk.", "kkk", ".kk"]), "brown_silk": padded(["mm.", "mmm", ".mm"]),
                "dry_husk": recolour(husk, {"I": "U", "H": "T"}),
                "kernels": ["".join("K" if (x + y) % 2 else "L" for x in range(16)) for y in range(16)]}

    def cob(heading, y, silk):
        # A husked cob leaning out from the stalk, tapering to its tuft of silk. Ripe, its husk dries and opens at the
        # tip to show the kernels, the colour of the corn.
        rotation = {"origin": [8, y, 8], "x": -20, "y": heading, "z": 0}
        ripe = silk == "brown_silk"
        husk = "dry_husk" if ripe else "husk"
        return [solid((6.5, y, 4), (9.5, y + 5, 7), husk, rotation=rotation),
                solid((7, y + 5, 4.5), (9, y + 7, 6.5), "kernels" if ripe else husk, top=silk, rotation=rotation),
                solid((7.5, y + 7, 5), (8.5, y + 8.5, 6), silk, rotation=rotation)]

    def stalk_plant(height, leaves, length, tassel=None, cobs=(), silk="silk", old=0):
        parts = column(8, 8, GROUND, GROUND + height, "stalk")
        for k in range(leaves):
            y = GROUND + 2 + k * (height - 4) / max(1, leaves)
            name = "old_leaf" if k < old else "leaf"
            parts += arching_leaf(name, 3, k * 180 + k * 23, y, length // 2, length - length // 2)
        if tassel:
            for i, turn in enumerate((0, 60, 120)):
                parts += tuft(tassel, (5, 6), (8, 8), turn, 8 if i % 2 else -8, ground=GROUND + height)
        for heading, y in cobs:
            parts += cob(heading, GROUND + y, silk)
        return parts
    # Up to its fourth stage corn stays within its block; from then on it is two blocks tall, its tassel near the top.
    stages = [
        stalk_plant(2, 3, 6), stalk_plant(4, 4, 8), stalk_plant(8, 5, 10), stalk_plant(12, 6, 12), stalk_plant(20, 8, 14),
        stalk_plant(25, 10, 16, tassel="tassel"),
        stalk_plant(25, 10, 16, tassel="tassel", cobs=[(40, 11), (220, 14)]),
        stalk_plant(25, 10, 16, tassel="brown_tassel", cobs=[(40, 11), (220, 14)], silk="brown_silk", old=2),
    ]
    build(plant, palette, textures, stages, "leaf")


# White grape: a cutting, a young vine, then a woody vine trained along the trellis rails with big lobed leaves,
# setting small green bunches that swell and ripen golden green.

GRAPE = {"a": "#1e4a24", "b": "#2c6630", "c": "#40823c", "d": "#5c9e48", "e": "#86c060", "f": "#acd884",
         "h": "#4a2e16", "i": "#6e4826", "j": "#94683a",
         "P": "#e8f0a0", "Q": "#c8d870", "R": "#9aae48", "G": "#a8c860", "K": "#7a9a3a", "L": "#5e7e2c"}
GRAPE_VINE = [
    "..ddc....dcd....",
    ".dcecd..dceccd..",
    ".cdecc.dcdecdc..",
    "..cbc...ccbcc...",
    "....i....ib.....",
    "iiiiijiiiijiiiii",
    "..dd..ij....dd..",
    ".dcecd.i...dcecd",
    ".cdecc.ji..cdecc",
    "..cbc...i...cbc.",
    "iiiiiiiijiiiiiii",
    ".dd....ij...dd..",
    "dced...i...dcecd",
    "cdec...ji..cdec.",
    ".cb.....i...cb..",
    ".......hi.......",
]


# Grape leaves are broad, five-lobed and toothed; the canopy is stamped from them in two shades, with gaps.
GRAPE_LEAF = ["d.d.d", "dcdcd", "cdedc", ".cec.", "..b.."]
GRAPE_CANOPY = [(0, 0, 1), (5, 1, 0), (10, 0, 1), (2, 4, 0), (7, 5, 1), (11, 4, 0), (0, 8, 1), (5, 9, 0), (10, 9, 1),
                (3, 12, 1), (8, 12, 0), (12, 13, 1), (-2, 13, 0)]
GRAPE_CANOPY_FOOT = [(0, 0, 0), (5, 0, 1), (10, 1, 0), (2, 2, 1), (12, 2, 1), (7, 3, 0)]


def stamped(stamps, cane_row=None):
    """A grid of grape leaves stamped at (x, y), each lighter or not, and a woody cane along one row if asked."""
    grid = [["."] * 16 for _ in range(16)]
    if cane_row is not None:
        grid[cane_row] = list("iiiijiiiiijiiiii")
    for x0, y0, light in stamps:
        for dy, row in enumerate(GRAPE_LEAF):
            for dx, ch in enumerate(row):
                x, y = x0 + dx, y0 + dy
                if ch != "." and 0 <= x < 16 and 0 <= y < 16:
                    grid[y][x] = STEP_LIGHTER[ch] if light else ch
    return ["".join(r) for r in grid]


def grape(plant="white_grape_vine", palette=GRAPE):
    young = ["." * 16] * 6 + [
        "......dd........",
        ".....dced.......",
        ".....cdec..dd...",
        "......cb..dcec..",
        ".......i..cdec..",
        "..dd...ij..cb...",
        ".dcec...i.......",
        ".cdec...ji......",
        "..cb.....i......",
        ".........hi.....",
    ]
    cutting = ["." * 16] * 11 + ["......dd........", ".....dcec.......", ".....cdec.......", "......cbi.......", ".......hi......."]
    textures = {"cutting": cutting, "young": young, "vine": GRAPE_VINE, "stalk": stalk_strip(["iijiiiijiiijiiij"]),
                "ripe": padded(["PQ", "QR"]), "green": padded(["GK", "KL"]),
                "trunk": stalk_strip(["hihihhihhihihhih", "iijiiijiiijiiiji"]),
                "canopy": stamped(GRAPE_CANOPY), "canopy_foot": stamped(GRAPE_CANOPY_FOOT, cane_row=5)}

    def on_trellis(stage, bunches=()):
        parts = trellis_frame() + trellis_planes("#" + stage)
        for b in bunches:
            parts += bunch(*b)
        return parts
    front, back = 10.4, 5.6
    small, big = [2, 2, 1], [3, 3, 3, 2, 2, 1]
    stages = [
        on_trellis("cutting"), on_trellis("cutting"), on_trellis("young"), on_trellis("young"), on_trellis("vine"),
        on_trellis("vine"),
        on_trellis("vine", [(5, front, 9.5, small, "green"), (11, front, 14.5, small, "green"), (8, back, 9.5, small, "green")]),
        on_trellis("vine", [(4.5, front, 9.5, big, "ripe"), (11.5, front, 14.5, big, "ripe"), (8, front, 4.5, [2, 2, 1], "ripe"),
                            (6, back, 14.5, big, "ripe"), (11, back, 9.5, big, "ripe")]),
    ]

    def climbed(bunches=()):
        # A woody trunk up the lower trellis to a canopy of leaves over the climbed one, bunches hanging beneath it.
        parts = trellis_frame() + column(8, 8, 0, 15, "trunk") + trellis_planes("#canopy_foot") + trellis_planes("#canopy", 16)
        for b in bunches:
            parts += bunch(*b)
        return parts
    tall = {
        4: climbed(), 5: climbed(),
        6: climbed([(5, front, 17, [2, 2, 2, 1], "green"), (11, front, 16, [2, 2, 1], "green"), (8, back, 17, [2, 2, 2, 1], "green")]),
        7: climbed([(4.5, front, 17, big, "ripe"), (11.5, front, 16, [3, 3, 2, 2, 1], "ripe"), (8, front, 19, [3, 3, 3, 2, 1], "ripe"),
                    (6, back, 17, big, "ripe"), (11, back, 18, [3, 3, 2, 2, 1], "ripe")]),
    }
    build(plant, palette, textures, stages, "vine", extra={"post": NS + "trellis_wood"}, tall=tall)


# Black and red table grapes: the same vine, ripening dark blue-purple or red.
BLACK_GRAPE = {**GRAPE, "P": "#8a6ab0", "Q": "#5a3a86", "R": "#341e56"}
RED_GRAPE = {**GRAPE, "P": "#e07a8e", "Q": "#b03a5a", "R": "#7a1e3c"}


# Rice: a clump of grassy shoots rising out of standing water, then green heads that droop as they fill and ripen
# gold. The block holds the water, so the plant grows up from its bottom and only shows above the surface.

RICE = {"a": "#24502a", "b": "#346c30", "c": "#4a8a3a", "d": "#68a648", "e": "#92c464", "f": "#c0dc8c",
        "h": "#9a7a30", "i": "#c8a448", "j": "#e4c868", "g": "#a8b848", "k": "#7e9a38"}
RICE_SPOTS = [(8, 8), (6.5, 6.5), (9.5, 6.5), (6.5, 9.5), (9.5, 9.5), (8, 5.5), (5.5, 8), (10.5, 8), (8, 10.5)]
RICE_TURNS = [0, 40, 80, 120, 160, 20, 60, 100, 140]
RICE_LEANS = [0, -10, 9, -8, 10, -6, 7, -9, 8]
RICE_BLADES = ["..e..", ".e.e.", "e.d.d", ".d.d.", "d.cd.", ".dc.d", "..cd.", ".cd..", "..c..", ".cc..", "..c..", "..cc.",
               "..c..", ".cc..", "..c..", "..c.."]
RICE_HEADING = ["..ff.", ".f..f", "....f", "..e.f", ".e.d.", "e.d.d", ".dcd.", "..cd.", ".cd..", "..c..", ".cc..", "..c..",
                "..cc.", "..c..", "..c..", "..c.."]


def rice(plant="rice", palette=RICE):
    textures = {"blades": bottom_padded(RICE_BLADES), "heading": bottom_padded(RICE_HEADING),
                "ripe": bottom_padded(recolour(RICE_HEADING, {"f": "j", "e": "i", "d": "g", "c": "k"})),
                "stems": bottom_padded([".c.c."] * 16)}

    def clump(texture, count, top):
        base = top - 16
        parts = []
        for i in range(count):
            spot, turn, lean = RICE_SPOTS[i], RICE_TURNS[i], RICE_LEANS[i]
            parts += tuft(texture, (5, 16), spot, turn, lean, ground=base)
            if base > 0:
                parts += tuft("stems", (5, base), spot, turn, lean / 2, ground=0)
        return parts
    stages = [clump("blades", 3, 16), clump("blades", 5, 18), clump("blades", 7, 20), clump("blades", 9, 22),
              clump("blades", 9, 24), clump("heading", 9, 26), clump("heading", 9, 27), clump("ripe", 9, 27)]
    build(plant, palette, textures, stages, "blades")


# Glutinous rice: the same plant, its ripe grains a chalky cream rather than gold.
GLUTINOUS_RICE = {**RICE, "i": "#d4c89a", "j": "#efe6c4"}


# Rhubarb: red buds breaking from the crown, then huge, crinkled leaves held up on thick red stalks.

RHUBARB = {"a": "#1e4224", "b": "#2c5e30", "c": "#3e7a3a", "d": "#56964a", "e": "#7eb466", "f": "#a8d08c",
           "r": "#6e1424", "s": "#a82638", "t": "#d0485a", "u": "#ec8496"}


# Rhubarb leaves: broad and roundish with a wavy, crinkled edge, a notch either side of the stalk, and pale veins
# fanning out from where it joins.
RHUBARB_BIG_LEAF = ["...bb.bbc.bb..", "..bccbccdcbcb.", ".bccdcceccdcb.", "bccdccdecdccdb", "bcecdcdedccecb",
                    "bccecddedcdccb", ".bcceddeddcccb", ".bccceddecccb.", "..bbcccdccbb..", "...bb.dd.bb...", "......dd......"]
RHUBARB_LEAF = ["..bb.bcb..", ".bccbccdb.", "bcdcecdccb", "bccdedcecb", "bcecdedccb", ".bccddccb.", "..bb.dbb..", ".....d...."]
RHUBARB_SMALL_LEAF = [".bb.bb.", "bccbccb", "bcdedcb", "bccdccb", ".bbdbb.", "...d..."]


def rhubarb():
    textures = {"small_leaf": padded(RHUBARB_SMALL_LEAF), "leaf": padded(RHUBARB_LEAF), "big_leaf": padded(RHUBARB_BIG_LEAF),
                "stalk": stalk_strip(["tsstssts" * 2, "ssrssrss" * 2]), "bud": padded(["ut", "ts"])}

    def leaves(texture, size, count, rise, lean, turn=0, tilt=15):
        # Leaves held on thick red stalks leaning well out from the crown, so the grown plant spreads wide and low.
        parts = []
        length = rise / _m.cos(_m.radians(lean))
        out = rise * _m.tan(_m.radians(lean))
        for i in range(count):
            heading = turn + i * 360 / count
            parts.append(stalk("stalk", round(length), heading, lean, width=2))
            parts.append(leaf(texture, size, heading, tilt, out, rise))
        return parts

    def buds(n):
        return [solid((8 + 2 * _m.sin(a) - 1, GROUND, 8 + 2 * _m.cos(a) - 1), (8 + 2 * _m.sin(a) + 1, GROUND + 2, 8 + 2 * _m.cos(a) + 1), "bud")
                for a in [k * 2 * _m.pi / n for k in range(n)]]
    big, medium = (14, 11), (10, 8)
    stages = [
        buds(3) + leaves("small_leaf", (7, 6), 1, 2, 20, tilt=30),
        buds(2) + leaves("small_leaf", (7, 6), 3, 3, 25, tilt=25),
        leaves("leaf", medium, 4, 4, 30, tilt=20),
        leaves("leaf", medium, 5, 5, 32, turn=30, tilt=20),
        # From its fifth stage its huge leaves reach about a block past each side, under a block high.
        leaves("big_leaf", big, 5, 6, 38) + leaves("leaf", medium, 2, 6, 12, turn=36, tilt=35),
        leaves("big_leaf", big, 6, 7, 40, turn=20) + leaves("leaf", medium, 2, 7, 12, turn=50, tilt=35),
        leaves("big_leaf", big, 6, 7, 40, turn=10) + leaves("leaf", medium, 3, 7, 12, turn=70, tilt=35) + buds(2),
        leaves("big_leaf", big, 7, 8, 42, turn=25) + leaves("leaf", medium, 3, 8, 12, turn=85, tilt=35),
    ]
    build("rhubarb", RHUBARB, textures, stages, "big_leaf")


# Dill: feathery blue-green fronds, then a tall stem carrying flat umbrellas of tiny yellow flowers that turn to brown
# seed heads.

DILL = {"a": "#2e5232", "b": "#3e6e3e", "c": "#548a48", "d": "#6ea458", "e": "#94c070", "f": "#bcd890",
        "Y": "#e8d040", "Z": "#c0a020", "h": "#7a5a2e", "i": "#a8844a"}
DILL_FROND = ["...e...", "e..d..e", ".e.d.e.", "..ede..", "e..d..e", ".e.d.e.", "..ede..", "e..d..e", ".e.d.e.", "...d..."]
DILL_SMALL = ["..e..", "e.d.e", ".ede.", "e.d.e", ".ede.", "..d.."]
DILL_UMBEL = ["..Y.Y..", ".YY.YY.", "YY.c.YY", "..ccc..", "YY.c.YY", ".YY.YY.", "..Y.Y.."]
DILL_RAYS = ["Y..Y..Y", ".c.c.c.", "..ccc.."]
DILL_SPOTS = [(8, 6), (10, 8.5), (6, 9), (8.5, 10.5), (5.5, 6.5), (10.5, 5.5)]


def dill():
    seed = {"Y": "h", "Z": "i"}
    bud = {"Y": "d"}
    textures = {"frond": bottom_padded(DILL_FROND), "small_frond": bottom_padded(DILL_SMALL),
                "stalk": stalk_strip(["cdcccdcccdcccdcc"]),
                "umbel": padded(DILL_UMBEL), "rays": bottom_padded(DILL_RAYS),
                "bud_umbel": padded(recolour(DILL_UMBEL, bud)), "bud_rays": bottom_padded(recolour(DILL_RAYS, bud)),
                "seed_umbel": padded(recolour(DILL_UMBEL, seed)), "seed_rays": bottom_padded(recolour(DILL_RAYS, seed)),
                "old_frond": bottom_padded(recolour(DILL_FROND, {"e": "f", "d": "e"}))}

    def fronds(texture, size, count):
        parts = []
        for i in range(count):
            parts += tuft(texture, size, DILL_SPOTS[i], i * 50, (-1) ** i * 12)
        return parts

    def umbel(x, z, height, kind=""):
        top = GROUND + height
        face = {"uv": [0, 0, 7, 7], "texture": "#" + kind + "umbel"}
        stem = rod(x, z, GROUND, top, "stalk")
        return ((stem if isinstance(stem, list) else [stem]) + tuft(kind + "rays", (7, 3), (x, z), 20, 0, ground=top)
                + [{"from": [x - 3.5, top + 3, z - 3.5], "to": [x + 3.5, top + 3, z + 3.5], "shade": False,
                    "faces": {"up": face, "down": face}}])
    def fronds_up(texture, levels):
        # Feathery fronds along the stems as well as at their feet, so the tall plant stays airy all the way up.
        parts = []
        for k, y in enumerate(levels):
            spot = DILL_SPOTS[(k * 2 + 1) % len(DILL_SPOTS)]
            parts += tuft(texture, (7, 10), spot, 30 + k * 70, (-1) ** k * 15, ground=GROUND + y)
        return parts
    stages = [
        fronds("small_frond", (5, 6), 2), fronds("small_frond", (5, 6), 4), fronds("frond", (7, 10), 4),
        fronds("frond", (7, 10), 6),
        # From its fifth stage dill is two blocks tall: stems with fronds up them, crowned with flower umbrellas.
        fronds("frond", (7, 10), 6) + fronds_up("frond", [7]) + umbel(8, 8, 20, "bud_"),
        fronds("frond", (7, 10), 6) + fronds_up("frond", [7, 13]) + umbel(8, 8, 25) + umbel(10, 7, 19, "bud_"),
        fronds("frond", (7, 10), 6) + fronds_up("frond", [7, 13, 17]) + umbel(8, 8, 28) + umbel(10, 7, 23) + umbel(6, 9.5, 19)
        + umbel(9.5, 10, 26, "bud_"),
        fronds("old_frond", (7, 10), 5) + fronds_up("old_frond", [7, 13]) + umbel(8, 8, 28, "seed_") + umbel(10, 7, 23, "seed_")
        + umbel(6, 9.5, 19, "seed_") + umbel(9.5, 10, 26, "seed_"),
    ]
    build("dill", DILL, textures, stages, "frond")


# Fruit-tree leaves, made like Minecraft's: a plain leaf cube of small, dense clumps of leaves in a few greens, with
# holes, nothing sticking out of it. Blossom is drawn into the leaves, as on flowering azalea leaves, and the fruit too,
# as on a sweet berry bush, with a few fruit also hanging below the leaves where you see them from under the canopy.

PLUM = {"a": "#1a361c", "b": "#284c26", "c": "#3a6632", "d": "#528240", "e": "#6c9c4c",
        "P": "#341640", "Q": "#552866", "R": "#784290", "S": "#a684bc", "k": "#5a4020",
        "W": "#fff4f8", "V": "#f2c8da", "Y": "#f0d060"}


def noise(x, y, seed=0):
    """A fixed pseudo-random number in [0, 1) for a pixel, so textures come out the same every run."""
    n = (x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFF) / 65536


def vanilla_leaves(seed=0, holes=0.17):
    """A leaf-cube face in the manner of Minecraft's leaves: two-by-two clumps of leaf in a few greens, each lit on its
    upper left and shaded on its lower right, with a scattering of holes."""
    grid = []
    for y in range(16):
        row = ""
        for x in range(16):
            if noise(x, y, seed) < holes:
                row += "."
                continue
            clump = noise(x // 2 + (y // 2) % 2, y // 2, seed + 1)
            tone = "b" if clump < 0.3 else "c" if clump < 0.7 else "d"
            if x % 2 == 0 and y % 2 == 0 and tone != "b":
                tone = {"c": "d", "d": "e"}[tone]
            elif x % 2 == 1 and y % 2 == 1:
                tone = {"b": "a", "c": "b", "d": "c"}[tone]
            row += tone
        grid.append(row)
    return grid


def stamp(grid, shape, spots):
    """Draws a small shape onto a grid at each spot, its '.' pixels left as they are."""
    grid = [list(r) for r in grid]
    for x0, y0 in spots:
        for dy, row in enumerate(shape):
            for dx, ch in enumerate(row):
                if ch != "." and 0 <= x0 + dx < 16 and 0 <= y0 + dy < 16:
                    grid[y0 + dy][x0 + dx] = ch
    return ["".join(r) for r in grid]


PLUM_FLOWER = [".W.", "WYW", ".V."]
PLUM_FRUIT = [".k.", "QRQ", "QSQ", ".P."]
BLOSSOM_SPOTS = [(1, 1), (9, 2), (5, 6), (12, 8), (2, 11), (8, 12), (13, 13)]
FRUIT_SPOTS_ON_LEAVES = [(2, 1), (10, 3), (6, 8), (12, 10), (1, 11)]


def leafy(seed, cover=0.67):
    """A leaf-cube face in the manner of Minecraft's own leaves: small leaves of two to five pixels scattered and
    overlapping, each lit on its upper left and shaded on its lower right, until about two thirds of the face is
    covered and the rest is holes you see the leaves behind through. Four tones, 'a' darkest to 'd' lightest."""
    import random
    rng = random.Random(seed)
    shapes = [[(0, 0), (1, 0), (0, 1), (1, 1)], [(0, 0), (1, 0), (1, 1), (2, 1)], [(1, 0), (0, 1), (1, 1), (1, 2)],
              [(0, 0), (1, 0), (2, 0), (1, 1)], [(0, 0), (0, 1), (1, 1)], [(1, 0), (0, 1), (1, 1), (2, 1), (1, 2)]]
    grid = [["."] * 16 for _ in range(16)]
    filled = 0
    while filled < 256 * cover:
        shape = rng.choice(shapes)
        x0, y0 = rng.randrange(16), rng.randrange(16)
        base = rng.choice("bbcc")
        for dx, dy in shape:
            x, y = (x0 + dx) % 16, (y0 + dy) % 16
            tone = base
            if (dx, dy) == shape[0]:
                tone = {"b": "c", "c": "d"}[base]
            elif (dx, dy) == shape[-1]:
                tone = {"b": "a", "c": "b"}[base]
            if grid[y][x] == ".":
                filled += 1
            grid[y][x] = tone
    return ["".join(r) for r in grid]


def tinted(tint, tones=(0.46, 0.56, 0.70, 0.84)):
    """A tree's four leaf greens, made as Minecraft makes its leaves: grey tones times a tint colour."""
    r, g, b = rgb(tint)
    return {k: "#%02x%02x%02x" % (round(r * t), round(g * t), round(b * t)) for k, t in zip("abcd", tones)}


def fruit_tree(tree, tint, flower, fruits, hanging=None, seed=1):
    """A fruit tree's leaves through its stages: plain leaves, leaves in blossom, then leaves bearing each kind of fruit
    in turn (the olive ripens green, then black). Flowers and fruit are drawn into the leaves at spots on them; a tree
    with large fruit also hangs a few round ones just below its leaves. The leaves are drawn once, so every stage of a
    tree shows the same leaves.

    flower: (shape, palette); fruits: [(shape, palette), ...]; hanging: (diameter, palette with P, Q, R, S, k) or None."""
    leaves = leafy(seed)
    palette = tinted(tint)
    shape, colours = flower
    palette.update(colours)
    textures = {"leaves": leaves, "blossom": stamp(leaves, shape, BLOSSOM_SPOTS)}
    for i, (shape, colours) in enumerate(fruits):
        palette.update(colours)
        textures[f"fruiting{i}"] = stamp(leaves, shape, FRUIT_SPOTS_ON_LEAVES)
    if hanging:
        size, colours = hanging[0], hanging[-1]
        tall = hanging[1] if len(hanging) == 3 else size
        palette.update(colours)
        textures["fruit_side"] = shaded_side("PQRS", tall, light_columns=(6, 7))
        textures["fruit_top"] = ["".join("k" if 7 <= x <= 8 and 7 <= y <= 8 else "Q" for x in range(16)) for y in range(16)]
        textures["stalk"] = stalk_strip(["k" * 16])

    def cube(face):
        return [{"from": [0, 0, 0], "to": [16, 16, 16],
                 "faces": {d: {"uv": [0, 0, 16, 16], "texture": "#" + face, "cullface": d}
                           for d in ("north", "south", "east", "west", "up", "down")}}]

    def hang(x, z, drop):
        size = hanging[0]
        tall = hanging[1] if len(hanging) == 3 else size
        return ball(size, tall, "fruit_side", "fruit_top", ground=-drop - tall, centre=(x, z)) + [rod(x, z, -drop, 0, "stalk")]
    stages = [cube("leaves"), cube("blossom")] + [cube(f"fruiting{i}") for i in range(len(fruits))]
    build(f"{tree}_leaves", palette, textures, stages, "leaves")
    with open(f"{A}/blockstates/{tree}_leaves.json", "w") as f:
        json.dump({"variants": {f"age={age}": {"model": NS + f"{tree}_leaves_stage{age}"} for age in range(len(stages))}}, f, indent=2)
        f.write("\n")
    if hanging:
        # Most fruiting leaf blocks hang no fruit below them and a few hang one or two, picked per block by Minecraft's
        # weighted random models, so a canopy is dotted with fruit rather than lined with it.
        last = len(stages) - 1
        few = {"one": [(5, 6, 1)], "two": [(4, 5, 1), (11, 10, 2)]}
        for name, spots in few.items():
            elements = cube(f"fruiting{last - 2}") + [p for x, z, d in spots for p in hang(x, z, d)]
            used = {"particle": f"{tree}_leaves_leaves", f"fruiting{last - 2}": f"{tree}_leaves_fruiting{last - 2}",
                    "fruit_side": f"{tree}_leaves_fruit_side", "fruit_top": f"{tree}_leaves_fruit_top", "stalk": f"{tree}_leaves_stalk"}
            write_model(f"{tree}_leaves_stage{last}_{name}", built_model(used, elements))
        variants = {f"age={age}": {"model": NS + f"{tree}_leaves_stage{age}"} for age in range(last)}
        variants[f"age={last}"] = [{"model": NS + f"{tree}_leaves_stage{last}", "weight": 6},
                                   {"model": NS + f"{tree}_leaves_stage{last}_one", "weight": 3},
                                   {"model": NS + f"{tree}_leaves_stage{last}_two", "weight": 1}]
        with open(f"{A}/blockstates/{tree}_leaves.json", "w") as f:
            json.dump({"variants": variants}, f, indent=2)
            f.write("\n")


# Flowers: five white petals round a yellow eye, pink-flushed for apple; small cream tufts for olive.
WHITE_FLOWER = ([".W.", "WYW", ".V."], {"W": "#fff6f4", "V": "#e8d8d8", "Y": "#f0d060"})
APPLE_FLOWER = ([".W.", "WYW", ".V."], {"W": "#fff0f2", "V": "#f2a8c0", "Y": "#f0d060"})
OLIVE_FLOWER = (["WV", "VW"], {"W": "#f4f0d0", "V": "#d8d0a0"})
# Fruit drawn into the leaves: a stalk, then the fruit, lit on its upper left.
ROUND_FRUIT = [".k.", "QRQ", "QSQ", ".P."]
CHERRIES = [".k.", "k.k", "RkR", "RRR", "R.R"]
OLIVES = ["k..", "RR.", "RRR", ".RR"]
GREEN_OLIVES = ["k..", "GG.", "GGG", ".GG"]
BLACK_OLIVES = ["k..", "UR.", "RUR", ".RR"]


ELDER_FLOWER = ([".W.W.", "WVWVW", ".WVW."], {"W": "#f6f0d8", "V": "#ddd4ac"})
ELDER_BERRIES = [".k.k.", "PQPQP", ".PQP."]


def sapling(name, palette, grid):
    """A sapling: a small plant on two crossed planes, as Minecraft's are."""
    draw(name, bottom_padded(grid), palette)
    write_model(name, {"parent": "minecraft:block/cross", "textures": {"cross": NS + name}})


LEMON = ["..k.", ".QRQ", "QRSR", ".QRQ", "..P."]
BERRY_CLUSTER = [".k.", "RQR", "QRQ", ".R."]
SPIKY_POD = ["R.R", "RTR", "TRT", ".R."]
STAR = ["P.P", ".Q.", "PQP"]
POD = ["kP", "PQ", "QP", "PQ", "Q."]
# The flush of new leaves on trees picked for their leaves, a brighter green than the old ones.
FLUSH = ["fe", "ef"]


def tree_sapling(tree, tint, trunk="#5a4028", palm=False):
    """A tree's sapling in its own green: a stem with small leaves, or a few fronds for a palm."""
    palette = {**tinted(tint), "h": trunk, "i": trunk}
    grid = (["d...d..", ".d.c.d.", "..cbc..", ".c.i.c.", "c..i..c", "...i...", "...h...", "..hh..."] if palm else
            ["..d.c..", ".dcd.d.", "c.bdc.d", ".c.ib..", "...i.c.", "..hi...", "...h...", "...h...", "..hh..."])
    sapling(f"{tree}_sapling", palette, grid)


def tropical_and_more():
    """The rest of the fruit trees: stone fruit, citrus, the trees picked for leaves or buds, spices, and palms."""
    k = {"k": "#4a3420"}
    green_k = {"k": "#3a5a20"}
    cream = ([".W.", "WYW", ".W."], {"W": "#f8f0d8", "Y": "#e8c84a"})
    tiny_white = (["W"], {"W": "#f4f4ea"})
    fruit_tree("almond", "#6ea040", APPLE_FLOWER, [(["k.", "GG", "GH"], {**green_k, "G": "#86a85a", "H": "#b0c488"})], seed=21)
    fruit_tree("apricot", "#66a23e", APPLE_FLOWER, [(ROUND_FRUIT, {**k, "P": "#b05a18", "Q": "#e08a30", "R": "#f4b050", "S": "#fcd890"})],
               hanging=(3, {**k, "P": "#b05a18", "Q": "#e08a30", "R": "#f4b050", "S": "#fcd890"}), seed=22)
    fruit_tree("ume", "#5e9a44", APPLE_FLOWER, [(ROUND_FRUIT, {**k, "P": "#6a8a20", "Q": "#9ab83a", "R": "#c8d860", "S": "#e8f0a0"})], seed=23)
    yellow = {**green_k, "P": "#b89a10", "Q": "#e0c020", "R": "#f4dc40", "S": "#fff490"}
    fruit_tree("lemon", "#3e8a3a", WHITE_FLOWER, [(LEMON, yellow)], hanging=(3, 4, yellow), seed=24)
    lime = {**green_k, "P": "#2e6a1a", "Q": "#4e9a2a", "R": "#7ac040", "S": "#b8e080"}
    fruit_tree("lime", "#3a8436", WHITE_FLOWER, [(ROUND_FRUIT, lime)], hanging=(3, lime), seed=25)
    flush = {"e": "#8cc85a", "f": "#b8e48a"}
    fruit_tree("makrut_lime", "#2e7032", tiny_white, [(FLUSH, flush)], seed=26)
    fruit_tree("bay_laurel", "#3e7a3a", (["Y"], {"Y": "#e8dc7a"}), [(FLUSH, flush)], seed=27)
    fruit_tree("yerba_mate", "#4a8a3a", tiny_white, [(FLUSH, flush)], seed=28)
    # Cinnamon is harvested for its bark, so its leaves never flower or fruit: one leaf block, flecked with the red of its
    # new leaves.
    palette = {**tinted("#3e7a34"), "e": "#c0503a", "f": "#e0806a"}
    draw("cinnamon_leaves", stamp(leafy(29), FLUSH, FRUIT_SPOTS_ON_LEAVES), palette)
    write_model("cinnamon_leaves", {"parent": "minecraft:block/block", "textures": {"particle": NS + "cinnamon_leaves"},
                                    "elements": [{"from": [0, 0, 0], "to": [16, 16, 16],
                                                  "faces": {d: {"uv": [0, 0, 16, 16], "texture": "#particle", "cullface": d}
                                                            for d in ("north", "south", "east", "west", "up", "down")}}]})
    with open(f"{A}/blockstates/cinnamon_leaves.json", "w") as f:
        json.dump({"variants": {"": {"model": NS + "cinnamon_leaves"}}}, f, indent=2)
        f.write("\n")
    fruit_tree("clove", "#3e7a34", tiny_white, [(BERRY_CLUSTER, {"k": "#3a5a20", "R": "#c0403a", "Q": "#e07a5a"})], seed=30)
    fruit_tree("coffee", "#2e6a2a", cream, [(BERRY_CLUSTER, {"k": "#3a5a20", "R": "#b01c22", "Q": "#e0443a"})], seed=31)
    fruit_tree("allspice", "#3e7234", tiny_white, [(BERRY_CLUSTER, {"k": "#3a5a20", "R": "#3a2418", "Q": "#5a3a28"})], seed=32)
    fruit_tree("annatto", "#4a8a3e", (["P.P", ".Y.", "P.P"], {"P": "#f0a0c0", "Y": "#f0d050"}),
               [(SPIKY_POD, {"R": "#a81c20", "T": "#e0403a"})], seed=33)
    nutmeg = {**green_k, "P": "#b88a30", "Q": "#e0b450", "R": "#f0d070", "S": "#d02a2a"}
    fruit_tree("nutmeg", "#2e6a34", cream, [(ROUND_FRUIT, nutmeg)], hanging=(3, nutmeg), seed=34)
    fruit_tree("star_anise", "#3a6a3a", (["R"], {"R": "#d84a5a"}), [(STAR, {"P": "#6a3a1a", "Q": "#9a5a2a"})], seed=35)
    tamarind = {"k": "#4a3420", "P": "#5a3a1a", "Q": "#8a6038", "R": "#a87a4a", "S": "#c8a070"}
    fruit_tree("tamarind", "#5a9640", (["Y", "R"], {"Y": "#f0d050", "R": "#e07040"}), [(POD, tamarind)],
               hanging=(2, 5, tamarind), seed=36)
    mango = {**green_k, "P": "#6a8a2a", "Q": "#c8a830", "R": "#f0a030", "S": "#e85a3a"}
    fruit_tree("mango", "#3e7a34", (["W", "V"], {"W": "#f4e8c8", "V": "#e8b0a0"}), [(LEMON, mango)], hanging=(3, 4, mango), seed=37)
    papaya = {**green_k, "P": "#3a6a1a", "Q": "#5a8a2a", "R": "#8ab040", "S": "#c8d870"}
    fruit_tree("papaya", "#5a9a40", tiny_white, [(LEMON, papaya)], hanging=(3, 5, papaya), seed=38)
    coconut = {"k": "#5a4a20", "P": "#4a5a20", "Q": "#6a7a30", "R": "#8a9a40", "S": "#b0b870"}
    fruit_tree("coconut", "#5a9a3a", cream, [(ROUND_FRUIT, coconut)], hanging=(4, coconut), seed=39)
    oil_palm = {"k": "#3a2a18", "P": "#6a1a10", "Q": "#b03a18", "R": "#e06a20", "S": "#f8a040"}
    fruit_tree("oil_palm", "#4a8a34", cream, [(BERRY_CLUSTER, oil_palm)], hanging=(3, oil_palm), seed=40)
    for tree, tint, trunk in [("apple", "#6ea83c", "#5a4028"), ("sweet_cherry", "#5ea444", "#4a2a28"), ("plum", "#5a9a44", "#5a4028"),
                              ("orange", "#3e8a3a", "#5a4028"), ("olive", "#86a466", "#6a5a48"), ("almond", "#6ea040", "#6a5038"),
                              ("apricot", "#66a23e", "#6a5038"), ("ume", "#5e9a44", "#4a2a28"), ("lemon", "#3e8a3a", "#5a4028"),
                              ("lime", "#3a8436", "#5a4a30"), ("makrut_lime", "#2e7032", "#5a4a30"), ("bay_laurel", "#3e7a3a", "#5a4028"),
                              ("yerba_mate", "#4a8a3a", "#5a4a30"), ("cinnamon", "#3e7a34", "#7a4a2a"), ("clove", "#3e7a34", "#5a4a30"),
                              ("coffee", "#2e6a2a", "#5a4a30"), ("allspice", "#3e7234", "#6a5a48"), ("annatto", "#4a8a3e", "#5a4a30"),
                              ("nutmeg", "#2e6a34", "#5a4a30"), ("star_anise", "#3a6a3a", "#5a4a30"), ("tamarind", "#5a9640", "#6a5038"),
                              ("mango", "#3e7a34", "#5a4a30")]:
        tree_sapling(tree, tint, trunk)
    for tree, tint in [("papaya", "#5a9a40"), ("coconut", "#5a9a3a"), ("oil_palm", "#4a8a34")]:
        tree_sapling(tree, tint, "#8a7a50", palm=True)
    for tree in ("apple", "sweet_cherry", "plum", "orange", "olive", "almond", "apricot", "ume", "lemon", "lime", "makrut_lime",
                 "bay_laurel", "yerba_mate", "cinnamon", "clove", "coffee", "allspice", "annatto", "nutmeg", "star_anise", "tamarind",
                 "mango", "papaya", "coconut", "oil_palm"):
        with open(f"{A}/blockstates/{tree}_sapling.json", "w") as f:
            json.dump({"variants": {f"stage={stage}": {"model": NS + f"{tree}_sapling"} for stage in (0, 1)}}, f, indent=2)
            f.write("\n")


def fruit_trees():
    fruit_tree("apple", "#6ea83c", APPLE_FLOWER,
               [(ROUND_FRUIT, {"k": "#5a4020", "P": "#7a1418", "Q": "#b8222a", "R": "#e04a3a", "S": "#f49a80"})],
               hanging=(4, {"k": "#5a4020", "P": "#7a1418", "Q": "#b8222a", "R": "#e04a3a", "S": "#f49a80"}), seed=11)
    fruit_tree("sweet_cherry", "#5ea444", WHITE_FLOWER,
               [(CHERRIES, {"k": "#4a6a20", "R": "#9a0e26"})], seed=12)
    fruit_tree("plum", "#5a9a44", WHITE_FLOWER,
               [(ROUND_FRUIT, {"k": "#5a4020", "P": "#4a2266", "Q": "#7a3c9e", "R": "#a060c8", "S": "#d0a8ec"})],
               hanging=(4, {"k": "#5a4020", "P": "#4a2266", "Q": "#7a3c9e", "R": "#a060c8", "S": "#d0a8ec"}), seed=13)
    fruit_tree("orange", "#3e8a3a", WHITE_FLOWER,
               [(ROUND_FRUIT, {"k": "#3a5a20", "P": "#b04a08", "Q": "#e06e10", "R": "#f8962a", "S": "#ffc070"})],
               hanging=(4, {"k": "#3a5a20", "P": "#b04a08", "Q": "#e06e10", "R": "#f8962a", "S": "#ffc070"}), seed=14)
    fruit_tree("elder", "#5e9244", ELDER_FLOWER,
               [(ELDER_BERRIES, {"k": "#4a3420", "P": "#24102c", "Q": "#4a2a5a"})], seed=16)
    sapling("elder_sapling", {"a": "#2a4e24", "b": "#3c6a30", "c": "#58884a", "d": "#78a860", "h": "#5a4028", "i": "#7a5a38"},
            ["..d.c..", ".dcd.d.", "c.bdc.d", ".c.ib..", "...i.c.", "..hi...", "...h...", "...h...", "..hh..."])
    tropical_and_more()
    fruit_tree("olive", "#86a466", OLIVE_FLOWER,
               [(GREEN_OLIVES, {"k": "#5a6a40", "G": "#9ab440"}), (BLACK_OLIVES, {"k": "#5a6a40", "R": "#3a2440", "U": "#8a6aa0"})], seed=15)


# Bushes, made like Minecraft's azalea: a leafy box on woody stems, its sides' leaves hanging ragged over the edge. Woody
# shrubs (currants, elder, tea) grow to fill their block; herbs (thyme, marjoram, oregano, savory, mint, rosemary) stay
# low mounds. Each has its own leaves: clumps for the shrubs, fine small leaves for most herbs, needles for rosemary.

WOODY = [(5, 11, 7, 3), (3, 13, 10, 6), (1, 15, 13, 9), (0, 16, 14, 10)]
HERB = [(6, 10, 4, 2), (4, 12, 6, 4), (3, 13, 8, 5), (2, 14, 9, 6)]
TALL_HERB = [(6, 10, 5, 3), (4, 12, 8, 5), (3, 13, 10, 7), (2, 14, 12, 8)]


def ragged(grid, seed, start=11):
    """A bush side: its bottom rows more and more broken, so the leaves hang over the edge."""
    rows = [list(r) for r in grid]
    for y in range(start, 16):
        for x in range(16):
            if noise(x, y, seed + 7) < (y - start + 1) / (17 - start):
                rows[y][x] = "."
    return ["".join(r) for r in rows]


def ragged_side(seed):
    return ragged(vanilla_leaves(seed=seed, holes=0.12), seed)


def needles(seed):
    """Rosemary's foliage: short, narrow needles pointing up and out, grey-green with paler undersides."""
    import random
    rng = random.Random(seed)
    grid = [["."] * 16 for _ in range(16)]
    for _ in range(70):
        x, y = rng.randrange(16), rng.randrange(16)
        lean = rng.choice((-1, 0, 1))
        for k, tone in enumerate("dcb"):
            px, py = (x + (lean if k == 2 else 0)) % 16, (y + k) % 16
            grid[py][px] = tone if rng.random() > 0.15 else "a"
    return ["".join(r) for r in grid]


def bush_box(lo, hi, top, depth, side, top_texture="top"):
    """A leafy top and four sides hanging from it, each side seen from inside the bush too."""
    face_top = {"uv": [lo, lo, hi, hi], "texture": "#" + top_texture}
    uv = [0, 16 - depth, hi - lo, 16]
    flipped = [hi - lo, 16 - depth, 0, 16]
    y0 = top - depth
    s = "#" + side
    return [
        {"from": [lo, top, lo], "to": [hi, top, hi], "faces": {"up": face_top, "down": face_top}},
        {"from": [lo, y0, lo], "to": [hi, top, lo + 0.01], "faces": {"north": {"uv": uv, "texture": s}, "south": {"uv": flipped, "texture": s}}},
        {"from": [lo, y0, hi - 0.01], "to": [hi, top, hi], "faces": {"south": {"uv": uv, "texture": s}, "north": {"uv": flipped, "texture": s}}},
        {"from": [lo, y0, lo], "to": [lo + 0.01, top, hi], "faces": {"west": {"uv": uv, "texture": s}, "east": {"uv": flipped, "texture": s}}},
        {"from": [hi - 0.01, y0, lo], "to": [hi, top, hi], "faces": {"east": {"uv": uv, "texture": s}, "west": {"uv": flipped, "texture": s}}},
    ]


def bush_stems(height, spots=((8, 8), (6, 9), (10, 7))):
    parts = []
    for i, (x, z) in enumerate(spots):
        parts += tuft("twigs", (5, min(9, height)), (x, z), i * 60, (-1) ** i * 10, ground=GRASS)
    return parts


def flat_flowers(spots, texture, y, size=5):
    """Flat heads of small flowers lying on top of a bush, like elderflower."""
    face = {"uv": [0, 0, size, size], "texture": "#" + texture}
    return [{"from": [x - size / 2, y, z - size / 2], "to": [x + size / 2, y, z + size / 2], "shade": False,
             "faces": {"up": face, "down": face}} for x, z in spots]


def shrub(plant, palette, foliage, sizes, sides, tops=None, extras=None, twigs=None, stems=9):
    """A bush through its four stages: sizes gives each stage's box as (lo, hi, top, depth), sides and tops each stage's
    side and top texture names, extras each stage's further parts. foliage maps texture names to grids."""
    twigs = twigs or ["j.i.j", ".i.i.", "..h..", ".h.h.", "..h..", "..h..", ".h...", "..h..", "..h.."]
    textures = {"twigs": bottom_padded(twigs), **foliage}
    tops = tops or ["top"] * 4
    extras = extras or [[], [], [], []]
    stages = []
    for k, (lo, hi, top, depth) in enumerate(sizes):
        stages.append(bush_stems(min(stems, top - 1)) + bush_box(lo, hi, top, depth, sides[k], tops[k]) + extras[k])
    build(plant, palette, textures, stages, tops[-1])
    return stages


def currant_strings(berry):
    parts = []
    for x, z in [(2, 5), (5, 14.5), (11, 1.5), (14.5, 10), (8, 14.5), (1.5, 11)]:
        parts.append(rod(x, z, 6, 9, "stalk"))
        for k in range(4):
            bx, by = x + (0.4 if k % 2 else -0.4), 8 - 1.3 * (k + 1)
            parts.append(solid((bx - 0.6, by, z - 0.6), (bx + 0.6, by + 1.2, z + 0.6), berry))
    return parts


def currant(plant, palette, seed):
    side = ragged_side(seed)
    foliage = {"top": vanilla_leaves(seed=seed + 1), "side": side,
               "flowering_side": stamp(side, ["G"], [(3, 6), (11, 4), (7, 9), (13, 10), (1, 9)]),
               "fruiting_side": stamp(side, ["T", "S"], [(2, 7), (10, 5), (6, 10), (13, 9), (4, 2), (12, 12)]),
               "berry": padded(["TS", "SR"]), "stalk": stalk_strip(["i" * 16])}
    shrub(plant, palette, foliage, WOODY, ["side", "side", "flowering_side", "fruiting_side"],
          extras=[[], [], [], currant_strings("berry")])


CURRANT = {"a": "#1c3a1e", "b": "#2a5428", "c": "#3c6e36", "d": "#548a44", "e": "#70a656",
           "h": "#4a3420", "i": "#6a4e30", "j": "#8a6a44", "G": "#c8d080", "R": "#7a1020", "S": "#c42234", "T": "#ee5a64"}
BLACKCURRANT = {**CURRANT, "R": "#120a1c", "S": "#2a1a3a", "T": "#5a4670"}


def redcurrant():
    currant("redcurrant_bush", CURRANT, 5)


# Tea: a clipped hedge of glossy dark leaves, its top flushing with pale new shoots when ready to pick; grown in shade,
# the flush is a deeper blue-green.
TEA = {"a": "#16301c", "b": "#22482a", "c": "#2f5e36", "d": "#447a46", "e": "#5e9658",
       "h": "#4a3420", "i": "#6a4e30", "j": "#8a6a44", "F": "#9ccc5a", "G": "#c4e486", "M": "#2a6a4a", "N": "#3e8a5e"}


def tea():
    top = leafy(31, cover=0.9)
    side = ragged(leafy(32, cover=0.85), 32, start=13)
    flush = stamp(top, ["G", "F"], [(x, y) for y in range(0, 16, 3) for x in range((y // 3) % 2 * 2, 16, 4)])
    shaded = stamp(top, ["N", "M"], [(x, y) for y in range(0, 16, 3) for x in range((y // 3) % 2 * 2, 16, 4)])
    foliage = {"top": top, "side": side, "flush": flush, "shaded_flush": shaded}
    sizes = [(5, 11, 6, 3), (3, 13, 9, 6), (1, 15, 11, 8), (1, 15, 12, 9)]
    stages = shrub("tea_bush", TEA, foliage, sizes, ["side"] * 4, tops=["top", "top", "top", "flush"])
    # The shaded bush at picking time: its flush the deeper green, chosen by the blockstate when it grows under a roof.
    lo, hi, top_y, depth = sizes[3]
    elements = bush_stems(9) + bush_box(lo, hi, top_y, depth, "side", "shaded_flush")
    write_model("tea_bush_stage3_shaded", built_model({"particle": "tea_bush_flush", "twigs": "tea_bush_twigs",
                                                        "side": "tea_bush_side", "shaded_flush": "tea_bush_shaded_flush"}, elements))
    variants = {f"age={age},shaded={str(sh).lower()}": {"model": NS + f"tea_bush_stage{age}" + ("_shaded" if sh and age == 3 else "")}
                for age in range(4) for sh in (False, True)}
    with open(f"{A}/blockstates/tea_bush.json", "w") as f:
        json.dump({"variants": variants}, f, indent=2)
        f.write("\n")


# Herbs: low mounds of small leaves on green-brown stems, flowering a little at picking time.
HERB_TWIGS = ["..e..", ".e.e.", "..h..", ".h.h.", "..h..", "..h..", ".h..."]


def herb(plant, palette, leaves, flower, seed, sizes=HERB, flowers=9):
    side = ragged(leaves(seed), seed, start=12)
    top = leaves(seed + 1)
    import random
    rng = random.Random(seed)
    spots = [(rng.randrange(14), rng.randrange(10)) for _ in range(flowers)]
    foliage = {"top": top, "side": side, "flowering_top": stamp(top, flower, spots),
               "flowering_side": stamp(side, flower, spots[: flowers // 2])}
    shrub(plant, palette, foliage, sizes, ["side", "side", "side", "flowering_side"],
          tops=["top", "top", "top", "flowering_top"], twigs=HERB_TWIGS)


def fine(seed):
    return leafy(seed, cover=0.9)


THYME = {"a": "#34482e", "b": "#4c6442", "c": "#668058", "d": "#849c70", "e": "#a2b48c", "h": "#5a4430",
         "P": "#c88ac8", "Q": "#9a5aa8"}
MARJORAM = {"a": "#38502e", "b": "#506c40", "c": "#6c8a58", "d": "#8caa74", "e": "#b0c89a", "h": "#5a4a30",
            "W": "#f4eef0", "V": "#e0c8d8"}
OREGANO = {"a": "#22401e", "b": "#34582c", "c": "#4a743c", "d": "#669450", "e": "#88b46c", "h": "#5a3a2a",
           "P": "#d88ab8", "Q": "#a85a8a"}
SAVORY = {"a": "#1e3420", "b": "#2c4a2c", "c": "#3e623a", "d": "#567e4c", "e": "#749c66", "h": "#4a3a28",
          "W": "#f0ecf4", "V": "#c8b8dc"}
MINT = {"a": "#1e4a24", "b": "#2c6630", "c": "#3e8a3e", "d": "#58a850", "e": "#80c86c", "h": "#4a5a2a",
        "P": "#c8a8e0", "Q": "#9a7ac0"}
ROSEMARY = {"a": "#2a3a34", "b": "#3c5048", "c": "#52685e", "d": "#6e8478", "e": "#9aaca0", "h": "#5a4a3a",
            "B": "#a8c0e8", "C": "#7890c8"}


def herbs():
    herb("thyme_bush", THYME, fine, [".P", "PQ"], 41)
    herb("marjoram_bush", MARJORAM, fine, ["WV"], 42)
    herb("oregano_bush", OREGANO, fine, ["P.", "QP"], 43)
    herb("savory_bush", SAVORY, fine, ["W", "V"], 44)
    herb("mint_bush", MINT, lambda seed: leafy(seed, cover=0.9), ["P", "Q", "P"], 45, sizes=TALL_HERB, flowers=7)
    herb("rosemary_bush", ROSEMARY, needles, ["B", "C"], 46, sizes=TALL_HERB)


# Pandan: a fountain of long, glossy, strap-shaped leaves arching out from a short stem.
PANDAN = {"a": "#1a4a22", "b": "#246030", "c": "#307a3a", "d": "#44964a", "e": "#64b05e", "h": "#6a5a30"}


def pandan():
    strap = [".d."] + ["ded"] * 5 + ["cdc"] * 6 + ["bcb"] * 4
    textures = {"leaf": padded(strap), "stem": stalk_strip(["h" * 16, "c" * 16])}

    def fountain(count, length, rise=1):
        parts = [stalk("stem", 2 + rise, 0, 0, ground=GRASS, width=2)]
        for i in range(count):
            parts += arching_leaf("leaf", 3, i * 360 / count + i * 11, GRASS + rise, length // 2, length - length // 2,
                                  rise_tilt=55 - (i % 3) * 8, droop=-25, out=0.5)
        return parts
    stages = [fountain(4, 8), fountain(6, 10), fountain(8, 13, 2), fountain(10, 15, 3)]
    build("pandan_bush", PANDAN, textures, stages, "leaf")


def bushes():
    redcurrant()
    currant("blackcurrant_bush", BLACKCURRANT, 7)
    tea()
    herbs()
    pandan()


# Raspberry: a bush of arching, reddish canes with toothed leaves along them, white flowers, then red berries hanging
# from the canes.

RASPBERRY = {"a": "#1e4020", "b": "#2c5a2c", "c": "#3e763a", "d": "#58944c", "e": "#82b46a", "f": "#aad08c",
             "h": "#5a2e2a", "i": "#7a4640", "R": "#8a1428", "S": "#c8283e", "T": "#ee5a6a", "W": "#f8f4ee", "Y": "#e8d070"}


def raspberry():
    textures = {"leaf": padded(leaf_grid(5, 6)), "small_leaf": padded(leaf_grid(4, 4)),
                "cane": stalk_strip(["ihiiiihiiiihiiih"]), "berry": padded(["TS", "ST", "SR", "RS"]), "berry_top": padded(["ST", "TS"]),
                "flower": padded([".W.", "WYW", ".W."])}

    def along(heading, lean, t):
        out, rise = t * _m.sin(_m.radians(lean)), t * _m.cos(_m.radians(lean))
        return 8 + out * _m.sin(_m.radians(heading)), GRASS + rise, 8 - out * _m.cos(_m.radians(heading)), out, rise

    def cane(heading, lean, length, berries=0, flowers=0, leaf_texture="leaf", size=(5, 6)):
        parts = [stalk("cane", length, heading, lean, ground=GRASS)]
        for k, t in enumerate((0.35, 0.6, 0.85)):
            _, _, _, out, rise = along(heading, lean, t * length)
            parts.append(leaf(leaf_texture, size, heading + (40 if k % 2 else -40), 25, out, rise, GRASS))
        for k in range(berries):
            x, y, z, _, _ = along(heading, lean, (0.55 + 0.3 * k) * length)
            parts += [rod(x, z, y - 1, y, "cane"), solid((x - 1, y - 4, z - 1), (x + 1, y - 1, z + 1), "berry", top="berry_top")]
        for k in range(flowers):
            x, y, z, _, _ = along(heading, lean, (0.6 + 0.25 * k) * length)
            face = {"uv": [0, 0, 3, 3], "texture": "#flower"}
            parts.append({"from": [x - 1.5, y - 1, z - 1.5], "to": [x + 1.5, y - 1, z + 1.5], "shade": False, "faces": {"up": face, "down": face}})
        return parts

    def bush(n, length, berries=0, flowers=0, leaf_texture="leaf", size=(5, 6)):
        parts = []
        for i in range(n):
            parts += cane(i * 360 / n + 15, 14 + (i % 3) * 7, length - (i % 2) * 2, berries, flowers, leaf_texture, size)
        return parts
    stages = [bush(2, 6, leaf_texture="small_leaf", size=(4, 4)), bush(4, 10), bush(5, 14, flowers=2), bush(6, 15, berries=2)]
    build("raspberry_bush", RASPBERRY, textures, stages, "leaf")


# Asparagus: a young crown sends up feathery fern while it builds strength, then thick, scaly spears with purple tips
# push up from it, cut when tall and grown again.

ASPARAGUS = {"a": "#2a4e2a", "b": "#3a6a36", "c": "#4e8644", "d": "#68a056", "e": "#8cbc6e", "f": "#b4d690",
             "G": "#4e7a32", "H": "#6e9a44", "I": "#98bc64", "U": "#6a3e6e", "V": "#94609a"}
ASPARAGUS_FERN = ["...e...", ".e.d.e.", "e.ede.e", ".e.d.e.", "..ede..", "e..d..e", ".e.d.e.", "..ede..", "e..d..e",
                  "...d...", "...c...", "...c..."]
ASPARAGUS_SPOTS = [(8, 8), (6, 9.5), (10, 6.5), (6.5, 6), (10, 10), (5, 7.5)]


def asparagus():
    spear = ["".join("I" if (x + y * 2) % 5 == 0 else "H" if x % 2 else "G" for x in range(16)) for y in range(16)]
    textures = {"fern": bottom_padded(ASPARAGUS_FERN), "spear": spear, "tip": padded(["HU", "IH"] * 2)}

    def fern(count, height):
        parts = []
        for i in range(count):
            parts += tuft("fern", (7, height), ASPARAGUS_SPOTS[i], i * 47, (-1) ** i * 10)
        return parts

    def spears(heights):
        parts = []
        for (x, z), h in zip(ASPARAGUS_SPOTS, heights):
            lean = {"origin": [x, GROUND, z], "x": 4, "y": x * 40, "z": 0}
            # A green, scaly spear, its tip a pixel narrower and flecked purple.
            parts.append(solid((x - 1, GROUND, z - 1), (x + 1, GROUND + h, z + 1), "spear", rotation=lean))
            parts.append(solid((x - 0.5, GROUND + h, z - 0.5), (x + 0.5, GROUND + h + 1.5, z + 0.5), "tip", rotation=lean))
        return parts
    stages = [fern(1, 6), fern(2, 9), fern(4, 11), fern(5, 12),
              spears([3, 2, 2]), spears([6, 4, 5, 3]), spears([10, 8, 9, 6, 4]), spears([14, 12, 13, 11, 9, 7])]
    build("asparagus", ASPARAGUS, textures, stages, "fern")


# Wakame: olive-brown seaweed, ribbon fronds with a pale midrib and wavy edges swaying up from a holdfast on the sea
# floor, a ruffle of frills at their foot once grown.

WAKAME = {"a": "#1e1e0c", "b": "#2e3014", "c": "#44441c", "d": "#5c5c26", "e": "#787834", "f": "#94944a"}
WAKAME_SPOTS = [(8, 8), (6, 9), (10, 7), (7.5, 5.5), (9.5, 10.5), (5.5, 6.5)]


def wakame():
    blade = [".ddd." if y < 2 else ("bcecb" if y % 3 else "cdedc") for y in range(16)]
    blade[0] = "..d.."
    textures = {"blade": bottom_padded(blade), "frill": bottom_padded(["d.e.d", "cdcdc", ".bcb."]), "holdfast": padded(["ab", "ba"])}

    def fronds(count, height, frills=False):
        parts = [solid((6.5, 0, 6.5), (9.5, 1, 9.5), "holdfast")]
        for i in range(count):
            parts += tuft("blade", (5, height - (i % 2) * 2), WAKAME_SPOTS[i], i * 53, (-1) ** i * (8 + i * 2), ground=0)
        if frills:
            for i in range(3):
                parts += tuft("frill", (5, 3), WAKAME_SPOTS[i], i * 60 + 20, 25, ground=1)
        return parts
    stages = [fronds(2, 6), fronds(3, 10), fronds(5, 14), fronds(6, 16, frills=True)]
    write_blockstate = {f"age={age}": {"model": NS + f"wakame_plant_stage{age}"} for age in range(4)}
    build("wakame_plant", WAKAME, textures, stages, "blade")
    with open(f"{A}/blockstates/wakame_plant.json", "w") as f:
        json.dump({"variants": write_blockstate}, f, indent=2)
        f.write("\n")


# White bean: a bush bean. Round seed leaves, then three-part leaves on stalks, small white flowers, slim green pods
# hanging under the leaves, and at harvest the pods dry pale tan with the leaves yellowing.

BEAN = {"a": "#1e4020", "b": "#2c5a2c", "c": "#3e7838", "d": "#58964a", "e": "#80b864", "f": "#a8d088",
        "y": "#a8a040", "z": "#c8c060", "W": "#f8f8f0", "Y": "#e8e0a0",
        "G": "#4e8a30", "H": "#72aa44", "I": "#9cca6a", "h": "#a8905a", "i": "#c8b07a", "j": "#e0cc98"}
BEAN_LEAF = ["...ddd...", "..dcecd..", "..cdedc..", "dd.ccc.dd", "dcd.c.dcd", "cecd.dcec", ".cc.b.cc.", "....b...."]
BEAN_POD_SPOTS = [(20, 3.5, 6), (100, 4, 5), (170, 3.5, 6), (240, 4, 5), (310, 3.5, 6), (60, 2, 5)]


def white_bean(plant="white_bean", palette=BEAN, leaf_grid_rows=BEAN_LEAF, leaf_size=(9, 8), pod_width=1.5, pod_length=5,
               scale=1.0, pods_show=True):
    """A bush bean; the other beans use it with their own leaves, pods and colours. scale shrinks a low plant, such as the
    peanut, whose pods grow underground and so do not show."""
    pod = ["".join("I" if y % 3 == 1 and x % 2 == 0 else "H" if x % 2 else "G" for x in range(16)) for y in range(16)]
    textures = {"seed_leaf": padded(leaf_grid(4, 4)), "leaf": padded(leaf_grid_rows),
                "old_leaf": padded(recolour(leaf_grid_rows, {"d": "z", "e": "z", "c": "y"})),
                "stalk": stalk_strip(["cdccdcccdccdccdc"]), "pod": pod, "dry_pod": recolour(pod, {"G": "h", "H": "i", "I": "j"}),
                "flower": padded(["W.W", ".Y.", "W.W"])}

    def leaves(n, rise, lean, texture="leaf", turn=0):
        return on_stalks(texture, leaf_size, n, 30, rise * scale, lean, turn)

    def pods(kind, count, length=pod_length):
        if not pods_show:
            return []
        parts = []
        for heading, out, top in BEAN_POD_SPOTS[:count]:
            x, z = 8 + out * _m.sin(_m.radians(heading)), 8 - out * _m.cos(_m.radians(heading))
            rotation = {"origin": [x, GROUND + top, z], "x": 12, "y": heading, "z": 0}
            parts.append(solid((x - pod_width / 2, GROUND + top - length, z - 0.5), (x + pod_width / 2, GROUND + top, z + 0.5), kind,
                               rotation=rotation))
        return parts

    def flowers(count):
        parts = []
        for heading, out, top in BEAN_POD_SPOTS[:count]:
            x, z = 8 + out * _m.sin(_m.radians(heading)), 8 - out * _m.cos(_m.radians(heading))
            face = {"uv": [0, 0, 3, 3], "texture": "#flower"}
            y = GROUND + top * scale
            parts.append({"from": [x - 1.5, y, z - 1.5], "to": [x + 1.5, y, z + 1.5], "shade": False,
                          "faces": {"up": face, "down": face}})
        return parts
    stages = [
        ring_of("seed_leaf", (4, 4), 2, 25, rise=1),
        ring_of("seed_leaf", (4, 4), 2, 20, rise=1) + leaves(2, 3, 10, turn=90),
        leaves(4, 4, 18),
        leaves(5, 6, 20) + leaves(2, 8, 8, turn=40),
        leaves(6, 7, 22) + leaves(3, 9, 8, turn=30) + flowers(4),
        leaves(6, 7, 22) + leaves(3, 9, 8, turn=30) + flowers(2) + pods("pod", 4, 4),
        leaves(6, 7, 22) + leaves(3, 9, 8, turn=30) + pods("pod", 6),
        leaves(4, 7, 26, "old_leaf") + leaves(2, 8, 10, turn=40) + pods("dry_pod", 6),
    ]
    build(plant, palette, textures, stages, "leaf")


# The other beans: azuki with yellow flowers and pods ripening near black; black bean with purple flowers; chickpea with
# small, feathery leaves and short, puffy pods; soybean with purple flowers and short, fuzzy pods; and peanut, a low plant
# with yellow flowers whose pods grow underground.
CHICKPEA_LEAF = ["..d.d..", ".d.c.d.", "..dcd..", ".d.c.d.", "..dcd..", ".d.c.d.", "...c...", "...b..."]
BEANS = {
    "azuki_bean": {**BEAN, "W": "#f0d84a", "Y": "#c8a020", "h": "#2a2018", "i": "#4a3a28", "j": "#6a5a40"},
    "black_bean": {**BEAN, "W": "#c8a0e0", "Y": "#f0f0f0", "h": "#5a4430", "i": "#8a6a48", "j": "#a88a60"},
    "chickpea": {**BEAN, "W": "#f0d0e0", "Y": "#c87aa0", "h": "#a8905a", "i": "#c8b07a", "j": "#e0cc98"},
    "soybean": {**BEAN, "W": "#b890d8", "Y": "#f0f0f0", "h": "#8a6a3a", "i": "#a88a5a", "j": "#c8aa7a"},
    "peanut": {**BEAN, "W": "#f0c830", "Y": "#d89a18"},
}


def other_beans():
    white_bean("azuki_bean", BEANS["azuki_bean"], pod_width=1.0, pod_length=5)
    white_bean("black_bean", BEANS["black_bean"])
    white_bean("chickpea", BEANS["chickpea"], CHICKPEA_LEAF, (7, 8), pod_width=2, pod_length=2.5)
    white_bean("soybean", BEANS["soybean"], pod_width=1.5, pod_length=3)
    white_bean("peanut", BEANS["peanut"], scale=0.6, pods_show=False)


# Peppers, chilies, aubergines and tomatillo: a stem with leaves at each level, small flowers, then fruit, green at first
# and ripe at harvest. Each names its leaf size, its fruit's shape and its colours.

NIGHTSHADE = {"a": "#1e4424", "b": "#2c5e30", "c": "#3f7c3a", "d": "#5c9a44", "e": "#8cc05a", "f": "#b4dc84",
              "s": "#4a7a2e", "W": "#f8f8f0", "Y": "#f0d050",
              "G": "#6a9a2a", "H": "#9ac448", "I": "#c8e478"}


def fruit_skin(dark, mid, light):
    """A glossy fruit skin: its middle colour, a band of shine down one side, and darker at its edges."""
    return ["".join(dark if x in (0, 15) else light if x in (4, 5) else mid for x in range(16)) for _ in range(16)]


def fruiting_plant(plant, palette, leaf_size, fruit, flower=("W", "Y"), height=10, levels=4):
    """fruit(x, z, top, ripe) gives one fruit's parts hanging from `top` at (x, z)."""
    textures = {"seed_leaf": padded(leaf_grid(4, 4)), "leaf": padded(leaf_grid(*leaf_size)),
                "stem": stalk_strip(["s" * 16]), "flower": padded([f".{flower[0]}.", f"{flower[0]}{flower[1]}{flower[0]}", f".{flower[0]}."])}
    for name in ("green", "ripe"):
        textures[name] = fruit_skin(*{"green": "GHI", "ripe": "PQR"}[name])

    def plant_at(h, n, flowers=0, fruits=0, ripe=False):
        parts = [stalk("stem", max(2, h), 0, 0)]
        for k in range(n):
            y = GROUND + 1.5 + k * (h - 1.5) / max(1, n)
            for j in range(2):
                parts.append(leaf("leaf", leaf_size, k * 90 + j * 180 + 20, 35 - k * 3, 0.5, y - GROUND))
        spots = [(40, 4, 0.55), (160, 4.2, 0.7), (280, 3.8, 0.45), (100, 4.4, 0.85)]
        for i, (heading, out, at) in enumerate(spots[:flowers]):
            x, z = 8 + out * _m.sin(_m.radians(heading)), 8 - out * _m.cos(_m.radians(heading))
            face = {"uv": [0, 0, 3, 3], "texture": "#flower"}
            y = GROUND + h * at
            parts.append({"from": [x - 1.5, y, z - 1.5], "to": [x + 1.5, y, z + 1.5], "shade": False, "faces": {"up": face, "down": face}})
        for i, (heading, out, at) in enumerate(spots[:fruits]):
            x, z = 8 + out * _m.sin(_m.radians(heading)), 8 - out * _m.cos(_m.radians(heading))
            parts += fruit(x, z, GROUND + h * at, ripe)
        return parts
    H = height
    stages = [ring_of("seed_leaf", (4, 4), 2, 30, rise=1),
              ring_of("seed_leaf", (4, 4), 2, 25, rise=1) + [leaf("leaf", leaf_size, 90, 40, 0.5, 2), leaf("leaf", leaf_size, 270, 40, 0.5, 2)],
              plant_at(H * 0.4, 2), plant_at(H * 0.6, 3), plant_at(H * 0.8, levels), plant_at(H, levels, flowers=3),
              plant_at(H, levels, flowers=1, fruits=3), plant_at(H, levels, fruits=4, ripe=True)]
    build(plant, palette, textures, stages, "leaf")


def hanging_fruit(width, length, tilt=10, upright=False):
    """A fruit `width` across and `length` long, hanging from its stalk, or pointing up for a chili that grows upright;
    unripe fruit is three-fifths the size."""
    def fruit(x, z, top, ripe):
        w, l = (width, length) if ripe else (width * 0.85, length * 0.75)
        texture = "ripe" if ripe else "green"
        if upright:
            return [rod(x, z, top, top + 1, "stem"), solid((x - w / 2, top + 1, z - w / 2), (x + w / 2, top + 1 + l, z + w / 2), texture,
                                                            rotation={"origin": [x, top, z], "x": tilt, "y": 0, "z": 0})]
        return [rod(x, z, top - 1, top, "stem"), solid((x - w / 2, top - 1 - l, z - w / 2), (x + w / 2, top - 1, z + w / 2), texture,
                                                        rotation={"origin": [x, top, z], "x": tilt, "y": 0, "z": 0})]
    return fruit


def round_fruit(diameter):
    def fruit(x, z, top, ripe):
        d = diameter if ripe else max(2, diameter - 1)
        return [rod(x, z, top - 1, top, "stem")] + ball(d, d, "ripe" if ripe else "green", "ripe" if ripe else "green",
                                                         ground=top - 1 - d, centre=(x, z))
    return fruit


def nightshades():
    def shade(**colours):
        return {**NIGHTSHADE, **colours}
    red = dict(P="#8a1418", Q="#c8221e", R="#f0503a")
    fruiting_plant("red_chili", shade(**red), (4, 6), hanging_fruit(1.0, 5))
    fruiting_plant("green_chili", shade(P="#0e3a0c", Q="#1e5a14", R="#5a9a3a"), (4, 6), hanging_fruit(1.0, 5))
    fruiting_plant("paprika_pepper", shade(P="#7a1010", Q="#b81e18", R="#e04a30"), (5, 6), hanging_fruit(1.6, 6))
    fruiting_plant("aji_amarillo", shade(P="#b86a10", Q="#f0a020", R="#f8d050"), (4, 6), hanging_fruit(1.3, 6))
    fruiting_plant("birds_eye_chili", shade(**red), (3, 5), hanging_fruit(0.8, 2.5, tilt=-10, upright=True), height=8)
    fruiting_plant("bell_pepper", shade(P="#8a1010", Q="#d02a1e", R="#f86848"), (5, 7), hanging_fruit(2.8, 3, tilt=0))
    fruiting_plant("poblano_chili", shade(P="#10320e", Q="#1e4a18", R="#3e7a2e"), (5, 7), hanging_fruit(2.2, 4.5))
    fruiting_plant("aubergine", shade(P="#24102e", Q="#40204e", R="#7a4a8a", W="#c8a0e0", Y="#f0d050"), (6, 7),
                   hanging_fruit(2.6, 5, tilt=6), flower=("W", "Y"))
    fruiting_plant("thai_aubergine", shade(P="#3a6a2a", Q="#c8dca8", R="#f0f4e0", W="#d8b0e8"), (5, 6), round_fruit(3))
    fruiting_plant("tomatillo", shade(P="#6a8a3a", Q="#a8c070", R="#d8e4a8", W="#f0d84a", Y="#5a3a20"), (5, 6), round_fruit(3),
                   flower=("W", "Y"))


# Climbing vines on a trellis, built one way: stems winding up the trellis with the vine's own leaves along them, its
# flowers among the leaves, and its fruit hanging in 3D in front of and behind the trellis. A vine that climbs a second
# trellis stacked on its own covers both, its fruit hanging the full height.

def vine_texture(seed, leaf_shape, height=16, stems=(5, 11), flowers=None):
    """A vine face: stems winding up from the root to `height` pixels, leaves stamped alternately either side of them,
    some a shade lighter, and flowers (shape, count) stamped among the leaves if given."""
    import random
    rng = random.Random(seed)
    grid = [["."] * 16 for _ in range(16)]
    for k, x0 in enumerate(stems):
        phase = rng.random() * 6
        points = [(x0 + round(1.6 * _m.sin(y * 0.45 + phase)), y) for y in range(15, 15 - height, -1)]
        for x, y in points:
            if 0 <= x < 16:
                grid[y][x] = "s"
        for i, (x, y) in enumerate(points[1::3]):
            side = 1 if (i + k) % 2 else -1
            w = len(leaf_shape[0])
            x0l = x + 1 if side > 0 else x - w
            light = rng.random() < 0.4
            for dy, row in enumerate(leaf_shape):
                for dx, ch in enumerate(row if side > 0 else row[::-1]):
                    px, py = x0l + dx, y - len(leaf_shape) + 1 + dy
                    if ch != "." and 0 <= px < 16 and 0 <= py < 16:
                        grid[py][px] = STEP_LIGHTER[ch] if light else ch
    if flowers:
        shape, count = flowers
        leafy_spots = [(x, y) for y in range(16) for x in range(16) if grid[y][x] in "bcde"]
        for x, y in rng.sample(leafy_spots, min(count, len(leafy_spots))):
            for dy, row in enumerate(shape):
                for dx, ch in enumerate(row):
                    if ch != "." and 0 <= x + dx < 16 and 0 <= y + dy < 16:
                        grid[y + dy][x + dx] = ch
    return ["".join(r) for r in grid]


FRONT, BACK = 10.4, 5.6
SHORT_SPOTS = [(4.5, FRONT, 14), (11, FRONT, 9.5), (7.5, BACK, 12.5), (12, BACK, 6.5), (6, FRONT, 6)]
TALL_SPOTS = [(4.5, FRONT, 29), (11, FRONT, 24), (7.5, BACK, 27.5), (12, BACK, 21), (5, FRONT, 19), (9.5, BACK, 15),
              (11.5, FRONT, 12), (5.5, BACK, 9)]


def trellis_vine(plant, palette, leaf_shape, flower, hang, seed, flowering_fruit=2, ripe_fruit=None):
    """A trellis vine through its eight stages: a seedling, a young vine half up the trellis, the full vine, then in
    flower with young fruit, then hung with ripe fruit. From its fifth stage it can climb a stacked trellis, and its
    two-block models cover both. hang(x, z, top, ripe) gives the parts of one hanging fruit or cluster."""
    textures = {"seedling": vine_texture(seed, leaf_shape, 6), "young": vine_texture(seed, leaf_shape, 12),
                "vine": vine_texture(seed, leaf_shape), "flowering": vine_texture(seed, leaf_shape, flowers=(flower, 6)),
                "vine_top": vine_texture(seed + 1, leaf_shape, stems=(4, 8, 12)),
                "flowering_top": vine_texture(seed + 1, leaf_shape, stems=(4, 8, 12), flowers=(flower, 7))}

    def on_trellis(lower, upper=None, fruit=(), ripe=False):
        parts = trellis_frame() + trellis_planes("#" + lower)
        if upper:
            parts += trellis_planes("#" + upper, 16)
        for x, z, top in fruit:
            parts += hang(x, z, top, ripe)
        return parts
    ripe_fruit = ripe_fruit or len(SHORT_SPOTS)
    stages = [on_trellis("seedling"), on_trellis("seedling"), on_trellis("young"), on_trellis("young"), on_trellis("vine"),
              on_trellis("vine"), on_trellis("flowering", fruit=SHORT_SPOTS[:flowering_fruit]),
              on_trellis("vine", fruit=SHORT_SPOTS[:ripe_fruit], ripe=True)]
    tall = {4: on_trellis("vine", "vine_top"), 5: on_trellis("vine", "vine_top"),
            6: on_trellis("flowering", "flowering_top", fruit=TALL_SPOTS[:flowering_fruit + 1]),
            7: on_trellis("vine", "vine_top", fruit=TALL_SPOTS[:ripe_fruit + 3], ripe=True)}
    # Textures the vine's fruit uses beyond these, drawn by the vine itself as <plant>_<name>.
    extra = {"post": NS + "trellis_wood"}
    for elements in stages + list(tall.values()):
        for element in elements:
            for face in element["faces"].values():
                key = face["texture"][1:]
                if key not in textures and key != "post":
                    extra[key] = f"{plant}_{key}"
    build(plant, palette, textures, stages, "vine", extra=extra, tall=tall)


def hanging_solid(x, z, top, width, length, texture, top_texture=None, tilt=0):
    """A fruit hanging from a short stalk at `top`: a box `width` across and `length` long below it."""
    rotation = {"origin": [x, top, z], "x": tilt, "y": 0, "z": 0} if tilt else None
    return [rod(x, z, top - 1, top, "stem"),
            solid((x - width / 2, top - 1 - length, z - width / 2), (x + width / 2, top - 1, z + width / 2), texture,
                  top=top_texture, rotation=rotation)]


VINE_GREENS = {"a": "#1e4424", "b": "#2c5e30", "c": "#3f7c3a", "d": "#5c9a44", "e": "#8cc05a", "f": "#b4dc84"}


# Cucumber: rough, lobed leaves and yellow flowers, then long green cucumbers hanging among them.
CUCUMBER = {**VINE_GREENS, "s": "#4a7a2e", "Y": "#f0d030", "Z": "#c89a18", "G": "#2c5a20", "H": "#3e7a2a", "I": "#7aa848",
            "k": "#3e6a22"}


def cucumber():
    skin = ["".join("I" if (x * 3 + y * 7) % 9 == 0 else "H" if (x + y) % 3 else "G" for x in range(16)) for y in range(16)]
    palette = CUCUMBER

    def hang(x, z, top, ripe):
        return hanging_solid(x, z, top, 1.6, 6 if ripe else 3, "cucumber", tilt=8)
    trellis_vine("cucumber_vine", palette, ["d.d.", "dcdc", "cedc", ".cb."], [".Y.", "YZY", ".Y."], hang, seed=21)
    draw("cucumber_vine_cucumber", skin, palette)
    draw("cucumber_vine_stem", stalk_strip(["k" * 16]), palette)


# Hops: bines of three-lobed leaves, then clusters of pale, papery cones.
HOPS = {**VINE_GREENS, "s": "#5a7a2e", "P": "#7aa040", "Q": "#a8c860", "R": "#d0e490", "S": "#e8f4b8", "k": "#4a6a28"}


def hops():
    def hang(x, z, top, ripe):
        parts = [rod(x, z, top - 1, top, "stem")]
        size = 3 if ripe else 2
        for dx, dy in ((0, 0), (-1.2, -1.6), (1.2, -2.2)):
            parts += ball(size, size + 1, "cone_side", "cone_top", ground=top - 1 - size - 1 + dy, centre=(x + dx, z))
        return parts
    trellis_vine("hop_vine", HOPS, ["c.c", "dcd", ".b."], ["Q", "R"], hang, seed=22, flowering_fruit=3)
    draw("hop_vine_cone_side", ["".join("R" if (x + y // 2) % 2 else "Q" for x in range(16)) for y in range(16)], HOPS)
    draw("hop_vine_cone_top", ["".join("S" if (x + y) % 3 else "R" for x in range(16)) for y in range(16)], HOPS)
    draw("hop_vine_stem", stalk_strip(["k" * 16]), HOPS)


# Long bean: three-part leaves and lilac flowers, then very long, slim pods hanging in pairs.
LONG_BEAN = {**VINE_GREENS, "s": "#4e7a30", "L": "#b89ad8", "M": "#8a6ab8", "G": "#4e8a2a", "H": "#6aa83a", "k": "#3e6a22"}


def long_bean():
    def hang(x, z, top, ripe):
        length = 8 if ripe else 4
        return hanging_solid(x - 0.5, z, top, 0.5, length, "pod") + hanging_solid(x + 0.5, z, top - 1, 0.5, length - 2, "pod")
    trellis_vine("long_bean_vine", LONG_BEAN, [".d.", "dcd", "cdc", ".b."], ["L.", "ML"], hang, seed=23, flowering_fruit=3)
    draw("long_bean_vine_pod", ["".join("H" if x % 2 else "G" for x in range(16)) for _ in range(16)], LONG_BEAN)
    draw("long_bean_vine_stem", stalk_strip(["k" * 16]), LONG_BEAN)


# Peppercorn: glossy, heart-shaped leaves, then long spikes of small berries, green, ripening red.
PEPPERCORN = {**VINE_GREENS, "s": "#5a5a2a", "G": "#4e8a2a", "H": "#7ab040", "R": "#a81c1c", "T": "#e04030", "k": "#4a5a24",
              "W": "#e8ecc8"}


def peppercorn():
    def hang(x, z, top, ripe):
        parts = [rod(x, z, top - 6, top, "stem")]
        berry = "ripe_berry" if ripe else "green_berry"
        for k in range(5):
            bx = x + (0.5 if k % 2 else -0.5)
            by = top - 1.2 * (k + 1)
            parts.append(solid((bx - 0.5, by, z - 0.5), (bx + 0.5, by + 1, z + 0.5), berry))
        return parts
    trellis_vine("peppercorn_vine", PEPPERCORN, [".dd.", "dcec", "cdcb", ".cb."], ["W", "W"], hang, seed=24, flowering_fruit=3)
    draw("peppercorn_vine_green_berry", padded(["HG", "GH"]), PEPPERCORN)
    draw("peppercorn_vine_ripe_berry", padded(["TR", "RT"]), PEPPERCORN)
    draw("peppercorn_vine_stem", stalk_strip(["k" * 16]), PEPPERCORN)


# Vanilla: a thick green orchid vine with fleshy, oblong leaves and waxy pale yellow-green flowers, then bunches of
# long green pods.
VANILLA = {**VINE_GREENS, "s": "#5a9a3a", "Y": "#eef0b0", "Z": "#d0d870", "G": "#4a7a2a", "H": "#6a9a3a", "k": "#4a7a2a"}


def vanilla():
    def hang(x, z, top, ripe):
        length = 7 if ripe else 3
        parts = []
        for dx in (-0.7, 0, 0.7):
            parts += hanging_solid(x + dx, z, top - abs(dx), 0.6, length - abs(dx) * 2, "pod")
        return parts
    trellis_vine("vanilla_vine", VANILLA, ["dd", "ce", "dc", "cd", "b."], ["Y.", "ZY", "Y."], hang, seed=25, flowering_fruit=2,
                 ripe_fruit=4)
    draw("vanilla_vine_pod", ["".join("H" if x % 2 else "G" for x in range(16)) for _ in range(16)], VANILLA)
    draw("vanilla_vine_stem", stalk_strip(["k" * 16]), VANILLA)


# Betel: picked for its leaves, so it grows lusher and glossier rather than fruiting, with a few pale catkins.
BETEL = {**VINE_GREENS, "s": "#4a6a2a", "W": "#e8e4c0", "V": "#c8c098", "k": "#4a6a2a"}


def betel():
    def hang(x, z, top, ripe):
        return hanging_solid(x, z, top, 0.8, 4, "catkin")
    trellis_vine("betel_vine", BETEL, [".dd.", "dceed", "cdedc", ".cdc.", "..b.."], ["e", "f"], hang, seed=26,
                 flowering_fruit=2, ripe_fruit=3)
    draw("betel_vine_catkin", ["".join("W" if (x + y) % 2 else "V" for x in range(16)) for y in range(16)], BETEL)
    draw("betel_vine_stem", stalk_strip(["k" * 16]), BETEL)


# Umbellifers like dill but within a block: anise, caraway and cumin grown for seed, flowering in flat umbrellas that
# turn to brown seed heads; cilantro and parsley grown for their leaves, which bolt to flower and seed at the last stage.
UMBEL_GREENS = {"a": "#2a4e2a", "b": "#3a6a36", "c": "#4e8644", "d": "#68a252", "e": "#8cc06a", "f": "#b4da8e",
                "h": "#7a5a2e", "i": "#a8844a"}
FLAT_LEAF = ["..dd...", ".dedd..", "dcedd.d", ".dcd.dd", "..c.dcd", "...cc..", "...c...", "...c..."]
CURLY_LEAF = [".e.e.e.", "edededе".replace("е", "e"), ".dcdcd.", "edcdcde", ".dcdcd.", "..cdc..", "...c...", "...c..."]
THREAD_LEAF = ["e...e..", ".d.d...", "..d..e.", "..d.d..", ".e.d...", "...d.e.", "...dd..", "...c..."]


def umbellifer(plant, palette, frond, flower, leafy=False, frond_size=(7, 8), heights=(9, 11, 12)):
    palette = {**UMBEL_GREENS, **palette, "Y": flower}
    small = frond[-6:]
    seed = {"Y": "h", "Z": "i"}
    textures = {"frond": bottom_padded(frond), "small_frond": bottom_padded(small), "stalk": stalk_strip(["c" * 16]),
                "umbel": padded(DILL_UMBEL), "rays": bottom_padded(DILL_RAYS),
                "bud_umbel": padded(recolour(DILL_UMBEL, {"Y": "d"})), "bud_rays": bottom_padded(recolour(DILL_RAYS, {"Y": "d"})),
                "seed_umbel": padded(recolour(DILL_UMBEL, seed)), "seed_rays": bottom_padded(recolour(DILL_RAYS, seed)),
                "old_frond": bottom_padded(recolour(frond, {"e": "f", "d": "e"}))}

    def fronds(texture, size, count):
        parts = []
        for i in range(count):
            parts += tuft(texture, size, DILL_SPOTS[i], i * 50, (-1) ** i * 12)
        return parts

    def umbel(x, z, height, kind=""):
        top = GROUND + height
        face = {"uv": [0, 0, 7, 7], "texture": "#" + kind + "umbel"}
        return ([rod(x, z, GROUND, top, "stalk")] + tuft(kind + "rays", (7, 3), (x, z), 20, 0, ground=top)
                + [{"from": [x - 3.5, top + 3, z - 3.5], "to": [x + 3.5, top + 3, z + 3.5], "shade": False,
                    "faces": {"up": face, "down": face}}])
    F, S = frond_size, (frond_size[0] - 2, 6)
    full = fronds("frond", F, 6)
    a, b, c = heights
    if leafy:
        late = [full, fronds("frond", F, 6) + fronds("frond", (F[0], F[1] + 2), 2),
                fronds("frond", (F[0], F[1] + 2), 6),
                fronds("old_frond", F, 5) + umbel(8, 8, c) + umbel(10, 7, b, "seed_") + umbel(6, 9.5, a)]
    else:
        late = [full + umbel(8, 8, a, "bud_"), full + umbel(8, 8, c) + umbel(10, 7, b, "bud_"),
                full + umbel(8, 8, c) + umbel(10, 7, b) + umbel(6, 9.5, a),
                fronds("old_frond", F, 5) + umbel(8, 8, c, "seed_") + umbel(10, 7, b, "seed_") + umbel(6, 9.5, a, "seed_")]
    stages = [fronds("small_frond", S, 2), fronds("small_frond", S, 4), fronds("frond", F, 4), fronds("frond", F, 5)] + late
    build(plant, palette, textures, stages, "frond")


# Celery: a bunch of thick, pale, ribbed stalks topped with leaflets, bolting to white umbels at the last stage.
def celery():
    palette = {**UMBEL_GREENS, "Y": "#f4f0e0", "s": "#a8cc78", "t": "#c8e0a0"}
    textures = {"stalk": stalk_strip(["st" * 8, "ts" * 8]), "leaflet": padded(leaf_grid(5, 5)), "thin": stalk_strip(["c" * 16]),
                "umbel": padded(DILL_UMBEL), "rays": bottom_padded(DILL_RAYS),
                "seed_umbel": padded(recolour(DILL_UMBEL, {"Y": "h"})), "seed_rays": bottom_padded(recolour(DILL_RAYS, {"Y": "h"}))}

    def bunch(count, height, lean=8):
        parts = []
        for i in range(count):
            heading = i * 360 / count + i * 7
            parts.append(stalk("stalk", height, heading, lean, width=2))
            out = height * _m.sin(_m.radians(lean))
            parts.append(leaf("leaflet", (5, 5), heading, 50, out, height - 1))
        return parts

    def umbel(x, z, height, kind=""):
        top = GROUND + height
        face = {"uv": [0, 0, 7, 7], "texture": "#" + kind + "umbel"}
        return ([rod(x, z, GROUND, top, "thin")] + tuft(kind + "rays", (7, 3), (x, z), 20, 0, ground=top)
                + [{"from": [x - 3.5, top + 3, z - 3.5], "to": [x + 3.5, top + 3, z + 3.5], "shade": False,
                    "faces": {"up": face, "down": face}}])
    stages = [bunch(2, 3), bunch(3, 4), bunch(4, 6), bunch(5, 7), bunch(6, 8), bunch(7, 9), bunch(8, 10),
              bunch(5, 8, 14) + umbel(8, 8, 12) + umbel(10, 7, 10, "seed_")]
    build("celery", palette, textures, stages, "stalk")


# Basils: a bushy plant of paired, oval leaves on a square stem, going to spikes of small flowers at the last stage.
def basil_plant(plant, palette, flower):
    palette = {**palette, "W": flower[0], "V": flower[1]}
    textures = {"seed_leaf": padded(leaf_grid(4, 4)), "leaf": padded(leaf_grid(5, 6)), "stem": stalk_strip(["s" * 16]),
                "spike": bottom_padded([".W.", "WVW", ".W.", "VWV", ".W.", "WVW", ".s.", ".s."])}

    def bush(h, levels, spikes=0):
        parts = []
        for stem in range(3):
            heading = stem * 120
            parts.append(stalk("stem", max(2, h), heading, 12))
            for k in range(levels):
                y = 1.5 + k * (h - 1.5) / max(1, levels)
                out = y * _m.sin(_m.radians(12))
                for side in (-1, 1):
                    parts.append(leaf("leaf", (5, 6), heading + side * 70 + k * 40, 30, out, y))
            if spikes:
                out = h * _m.sin(_m.radians(12))
                x, z = 8 + out * _m.sin(_m.radians(heading)), 8 - out * _m.cos(_m.radians(heading))
                parts += tuft("spike", (3, 8), (x, z), heading, 0, ground=GROUND + h)
        return parts
    stages = [ring_of("seed_leaf", (4, 4), 2, 30, rise=1), ring_of("seed_leaf", (4, 4), 2, 25, rise=1) + bush(2, 1),
              bush(3, 1), bush(4, 2), bush(5, 2), bush(6, 3), bush(7, 3), bush(7, 3, spikes=True)]
    build(plant, palette, textures, stages, "leaf")


BASIL = {"a": "#1e4a1e", "b": "#2e6a2a", "c": "#448a3a", "d": "#62a850", "e": "#8ac870", "f": "#b4e098", "s": "#4a7a2e"}
HOLY_BASIL = {"a": "#2a2a2e", "b": "#3e4a34", "c": "#566a40", "d": "#728a52", "e": "#96aa70", "f": "#b8c498", "s": "#6a3a5a"}
THAI_BASIL = {"a": "#1a3a20", "b": "#284e2a", "c": "#386a36", "d": "#4e8648", "e": "#70a66a", "f": "#9ac490", "s": "#6a2a5a"}


# Mustard and rapeseed: a rosette of broad leaves, then tall stalks of yellow flowers, then slender pods. Sesame: an
# upright stalk of paired leaves with white bell flowers along it, then ribbed capsules that dry brown.
def brassica_seed(plant, palette):
    palette = {**palette, "Y": "#f0d020", "Z": "#c8a010", "P": "#8aaa48", "Q": "#b8b060"}
    flowers = [".Y.Y.", "YZYZY", ".YZY.", "..c..", "..c..", "..c..", "..c..", "..c.."]
    textures = {"seed_leaf": padded(leaf_grid(4, 4)), "leaf": padded(leaf_grid(6, 8)), "stem": stalk_strip(["c" * 16]),
                "buds": bottom_padded(recolour(flowers, {"Y": "d", "Z": "c"})), "flowers": bottom_padded(flowers),
                "pods": bottom_padded(["P.Q.P", "P.Q.P", ".PQP.", "P.c.Q", ".Pc.P", "..c..", "..c..", "..c.."]),
                "dry_pods": bottom_padded(recolour(["P.Q.P", "P.Q.P", ".PQP.", "P.c.Q", ".Pc.P", "..c..", "..c..", "..c.."],
                                                   {"P": "Q", "c": "Q"}))}

    def rosette(n, tilt=40):
        return ring_of("leaf", (6, 8), n, tilt)

    def stalks(head, height, count=4):
        parts = rosette(5, 25)
        for i in range(count):
            x, z = DILL_SPOTS[i]
            parts.append(rod(x, z, GROUND, GROUND + height, "stem"))
            parts += tuft(head, (5, 8), (x, z), i * 45, (-1) ** i * 8, ground=GROUND + height - 3)
        return parts
    stages = [ring_of("seed_leaf", (4, 4), 2, 30, rise=1), rosette(3, 45), rosette(4), rosette(6, 35),
              stalks("buds", 6, 3), stalks("flowers", 8), stalks("flowers", 9), stalks("dry_pods", 9)]
    build(plant, palette, textures, stages, "leaf")


def sesame():
    palette = {**BASIL, "W": "#f8eef0", "V": "#e8b8c8", "G": "#6a9a3a", "H": "#8ab850", "P": "#7a5a30", "Q": "#a8844a"}
    textures = {"seed_leaf": padded(leaf_grid(4, 4)), "leaf": padded(leaf_grid(4, 7)), "stem": stalk_strip(["s" * 16]),
                "bell": padded(["WW", "WV"]), "capsule": padded(["HG", "GH"]), "dry_capsule": padded(["QP", "PQ"])}

    def plant_at(h, levels, along=None):
        parts = [stalk("stem", h, 0, 0)]
        for k in range(levels):
            y = 1.5 + k * (h - 2) / max(1, levels)
            for side in (0, 180):
                parts.append(leaf("leaf", (4, 7), side + k * 90, 40, 0.5, y))
            if along:
                for side in (90, 270):
                    x, z = 8 + 1.2 * _m.sin(_m.radians(side + k * 90)), 8 - 1.2 * _m.cos(_m.radians(side + k * 90))
                    parts.append(solid((x - 0.75, GROUND + y, z - 0.75), (x + 0.75, GROUND + y + 2, z + 0.75), along))
        return parts
    stages = [ring_of("seed_leaf", (4, 4), 2, 30, rise=1), plant_at(3, 1), plant_at(5, 2), plant_at(7, 3), plant_at(9, 4),
              plant_at(11, 4, "bell"), plant_at(12, 5, "capsule"), plant_at(12, 5, "dry_capsule")]
    build("sesame", palette, textures, stages, "leaf")


def herbs_and_oilseeds():
    umbellifer("cilantro", {}, FLAT_LEAF, "#f4f0f0", leafy=True)
    umbellifer("parsley", {"c": "#3e7a36", "d": "#56963e"}, CURLY_LEAF, "#e8e0a0", leafy=True)
    umbellifer("anise", {}, THREAD_LEAF, "#f4f0e8")
    umbellifer("caraway", {}, THREAD_LEAF, "#f0d8e0")
    umbellifer("cumin", {"d": "#7aa45a", "e": "#9cbc7a"}, THREAD_LEAF, "#f0d8e8", frond_size=(7, 6), heights=(7, 8, 9))
    celery()
    basil_plant("basil", BASIL, ("#f4f0f4", "#e0d0e8"))
    basil_plant("holy_basil", HOLY_BASIL, ("#d8b0e0", "#a870c0"))
    basil_plant("thai_basil", THAI_BASIL, ("#c890d8", "#8a4aa8"))
    brassica_seed("mustard", {"a": "#22401e", "b": "#33582a", "c": "#48743a", "d": "#64904c", "e": "#88b06a", "f": "#aed090"})
    brassica_seed("rapeseed", {"a": "#22383a", "b": "#344e4e", "c": "#4a6a66", "d": "#64887e", "e": "#86a898", "f": "#aac8b8"})
    sesame()


# Roots, rhizomes and the tropical crops.
ROOT_GREENS = {"a": "#1e4424", "b": "#2c5e30", "c": "#3f7c3a", "d": "#5c9a44", "e": "#8cc05a", "f": "#b4dc84"}


def lance(width, length):
    """A long, narrow, pointed leaf with a pale midrib, such as ginger's or cardamom's."""
    rows = []
    for y in range(length):
        t = y / max(1, length - 1)
        half = max(0, round(width / 2 * _m.sin(_m.pi * min(1, t * 1.1 + 0.05))))
        row = "." * (width // 2 - half) + ("d" * (half - 1) + "e" + "d" * (half - 1) if half > 0 else "") 
        row = row.ljust(width, ".")[:width]
        rows.append(row if y < length - 1 else ("." * (width // 2) + "c").ljust(width, "."))
    return rows


def leafy_stems(plant, palette, stems, height, leaf_size, leaves_per_stem, base=None, base_parts=None, flat=False):
    """Reed-like stems, each with long leaves up it alternately either side: ginger, galangal and cardamom. Turmeric's
    broad leaves rise straight from the ground (flat). base_parts(stage) adds what shows at the foot at harvest."""
    palette = {**ROOT_GREENS, **palette}
    textures = {"leaf": padded(lance(*leaf_size)), "stem": stalk_strip(["c" * 16]), **(base or {})}
    spots = [(8, 8), (6, 9.5), (10, 6.5), (6.5, 6), (10, 10), (8, 5)]

    def clump(count, h, n):
        parts = []
        for i in range(count):
            x, z = spots[i]
            heading = i * 67
            if flat:
                parts.append(leaf("leaf", leaf_size, heading, 62 - i * 3, 0, 0.5))
                continue
            parts.append(rod(x, z, GROUND, GROUND + h, "stem") if h <= 16 else rod(x, z, GROUND, GROUND + 16, "stem"))
            for k in range(n):
                y = 2 + k * (h - 2) / max(1, n)
                side = 1 if k % 2 else -1
                parts.append({**leaf("leaf", leaf_size, heading + 90 * side, 45, 0, y),
                              "rotation": {"origin": [8, GROUND + y, 8], "x": 45, "y": heading + 90 * side, "z": 0}})
        return [p for p in parts if not isinstance(p, list)]
    sizes = [(1, 3, 1), (2, 4, 1), (3, height * 0.4, 2), (3, height * 0.55, 2), (4, height * 0.7, leaves_per_stem - 1),
             (5, height * 0.85, leaves_per_stem), (6, height, leaves_per_stem), (6, height, leaves_per_stem)]
    stages = []
    for k, (count, h, n) in enumerate(sizes):
        parts = clump(count, h, n)
        if base_parts:
            parts += base_parts(k)
        stages.append(parts)
    build(plant, palette, textures, stages, "leaf")


def knobs(texture, count=3):
    """Knobbly rhizomes or tubers showing at the soil at harvest."""
    def parts(stage):
        if stage < 7:
            return []
        out = []
        for i, (x, z) in enumerate([(5.5, 9), (10.5, 8), (8, 11), (9, 5.5)][:count]):
            out.append(solid((x - 1.5, GROUND, z - 1), (x + 1.5, GROUND + 1.5, z + 1), texture))
        return out
    return parts


def rhizome_texture(dark, mid, light):
    return ["".join(light if (x * 5 + y * 3) % 7 == 0 else dark if x % 5 == 0 else mid for x in range(16)) for y in range(16)]


def zingibers():
    ginger = {"K": "#8a6a3a", "L": "#c8a468", "M": "#e8cc98"}
    leafy_stems("ginger", ginger, 6, 12, (3, 7), 4, base={"rhizome": rhizome_texture("K", "L", "M")}, base_parts=knobs("rhizome"))
    galangal = {"K": "#8a5a3a", "L": "#d8a888", "M": "#f0d4c0"}
    leafy_stems("galangal", galangal, 6, 14, (3, 8), 5, base={"rhizome": rhizome_texture("K", "L", "M")}, base_parts=knobs("rhizome"))
    turmeric = {"K": "#9a5a10", "L": "#e08a18", "M": "#f8b840"}
    leafy_stems("turmeric", turmeric, 6, 12, (6, 12), 1, base={"rhizome": rhizome_texture("K", "L", "M")}, base_parts=knobs("rhizome"),
                flat=True)
    cardamom = {"G": "#5a8a3a", "H": "#8ab05a"}

    def pods(stage):
        if stage < 6:
            return []
        return [solid((x - 0.75, GROUND, z - 0.75), (x + 0.75, GROUND + 1.5, z + 0.75), "pod")
                for x, z in [(5, 10), (6, 11), (11, 9), (10.5, 10.5), (9, 4.5), (10, 5)]]
    leafy_stems("cardamom", cardamom, 6, 15, (3, 9), 5, base={"pod": padded(["GH", "HG"])}, base_parts=pods)


def grass_clump(plant, palette, blade, base_texture=None, heights=(6, 8, 10, 12, 13, 14, 15, 15), counts=(2, 3, 4, 5, 6, 7, 8, 8),
                ripe=None, ground=GROUND):
    """A clump of upright, arching blades: lemongrass, and water chestnut's tubular rushes rising out of its water."""
    palette = {**ROOT_GREENS, **palette}
    textures = {"blade": bottom_padded(blade), **({"ripe_blade": bottom_padded(recolour(blade, ripe))} if ripe else {}),
                **(base_texture or {})}
    spots = [(8, 8), (6.5, 6.5), (9.5, 6.5), (6.5, 9.5), (9.5, 9.5), (8, 5.5), (5.5, 8), (10.5, 8)]
    stages = []
    for k, (h, n) in enumerate(zip(heights, counts)):
        name = "ripe_blade" if ripe and k == 7 else "blade"
        parts = []
        for i in range(n):
            lean = (-1) ** i * (6 + i)
            # Taller than a texture: the full blade stands on a stem cut from the blade's foot.
            below = max(0, h - 16)
            parts += tuft(name, (5, min(16, h)), spots[i], i * 47, lean, ground=ground + below)
            if below:
                parts += tuft(name, (5, below), spots[i], i * 47, lean / 2, ground=ground)
        if base_texture:
            parts.append(solid((6.5, ground, 6.5), (9.5, ground + 2, 9.5), list(base_texture)[0]))
        stages.append(parts)
    build(plant, palette, textures, stages, "blade")


def clumps():
    grass_clump("lemongrass", {"d": "#7aa85a", "e": "#a0c880", "w": "#e8ecc8", "x": "#c8d8a0"},
                ["..e..", ".ed..", ".d.e.", "e.d..", ".dd..", "..d.e", ".dd..", "..d..", ".ed..", "..d..", ".dd..", "..d..",
                 "..d..", ".dd..", "..d..", "..d.."], base_texture={"bulb": padded(["wx", "xw"])})
    grass_clump("water_chestnut", {"c": "#3e7a3a", "d": "#5a9a48", "h": "#8a7a3a", "i": "#b0a058"},
                ["..d..", "..d..", ".dd..", "..d..", "..dd.", "..d..", ".dd..", "..d..", "..d..", "..dd.", "..d..", ".dd..",
                 "..d..", "..d..", "..dd.", "..d.."], ripe={"d": "i", "c": "h"}, heights=(16, 17, 18, 20, 22, 24, 26, 26),
                ground=0)


def pineapple():
    palette = {**ROOT_GREENS, "a": "#2a4a3a", "b": "#3a5e48", "c": "#4e7a5a", "d": "#6a966e", "e": "#90b490",
               "R": "#c83a6a", "S": "#8a2a5a", "G": "#4a7a3a", "H": "#7aa04a", "P": "#a87a20", "Q": "#e0a830", "U": "#f4d060"}
    spike = [".d.", ".d.", "dcd", "dcd", "cec", "cec", "cdc", "cdc", "bcb", "bcb", "bcb", "bcb"]
    textures = {"leaf": padded(spike), "flower": ["".join("R" if (x + y) % 3 else "S" for x in range(16)) for y in range(16)],
                "green": ["".join("H" if (x + y) % 3 == 0 else "G" for x in range(16)) for y in range(16)],
                "ripe": ["".join("U" if (x + y) % 4 == 0 else "P" if (x - y) % 4 == 0 else "Q" for x in range(16)) for y in range(16)],
                "crown": bottom_padded(["d.d.d", ".ded.", "..d.."])}

    def rosette(count, length, tilt):
        return [leaf("leaf", (3, length), i * 360 / count + i * 13, tilt + (i % 3) * 8, 0.5, 0.5) for i in range(count)]

    def fruit(kind, size):
        parts = [stalk("leaf", 3, 0, 0, width=2)] if False else []
        s = size / 2
        parts.append(solid((8 - s, GROUND + 3, 8 - s), (8 + s, GROUND + 3 + size * 1.3, 8 + s), kind))
        parts += tuft("crown", (5, 3), (8, 8), 20, 0, ground=GROUND + 3 + size * 1.3)
        return parts
    stages = [rosette(3, 4, 40), rosette(5, 5, 40), rosette(7, 7, 38), rosette(9, 8, 35), rosette(11, 9, 32),
              rosette(12, 10, 30) + fruit("flower", 2.5), rosette(12, 10, 30) + fruit("green", 4),
              rosette(12, 10, 30) + fruit("ripe", 4)]
    build("pineapple", palette, textures, stages, "leaf")


def stalked(plant, palette, leaf_rows, leaf_size, rises, leans, counts, tilt=35, extras=None, stem_letter="c"):
    """Leaves held up on stalks from the crown, more and higher each stage: cassava, sweet potato, wasabi."""
    palette = {**ROOT_GREENS, **palette}
    textures = {"seed_leaf": padded(leaf_grid(4, 4)), "leaf": padded(leaf_rows), "stalk": stalk_strip([stem_letter * 16]),
                **(extras[0] if extras else {})}
    stages = [ring_of("seed_leaf", (4, 4), 2, 30, rise=1)]
    for k in range(1, 8):
        parts = on_stalks("leaf", leaf_size, counts[k], tilt, rises[k], leans[k], turn=k * 23)
        if extras:
            parts += extras[1](k)
        stages.append(parts)
    build(plant, palette, textures, stages, "leaf")


def stalked_crops():
    palmate = ["d...d...d", ".d..d..d.", "..dcecd..", "d.cdedc.d", ".dcceccd.", "..cbcbc..", "....c....", "....c...."]
    stalked("cassava", {"s": "#8a2a3a"}, palmate, (9, 8), [0, 3, 5, 7, 9, 11, 13, 14], [0, 10, 15, 18, 20, 22, 24, 24],
            [0, 2, 3, 4, 5, 6, 7, 8], stem_letter="s")
    heart = [".dd.dd.", "dceceed", "dcedecd", ".dcedc.", "..dcd..", "...c...", "...c..."]
    tubers = ({"tuber": rhizome_texture("K", "L", "M")},
              lambda k: [solid((x - 1.5, GROUND, z - 1), (x + 1.5, GROUND + 1.5, z + 1), "tuber") for x, z in [(5, 10), (11, 9)]]
              if k == 7 else [])
    stalked("sweet_potato", {"K": "#6a1e2a", "L": "#a8384a", "M": "#d06a70"}, heart, (7, 7), [0, 1, 2, 2, 3, 3, 3, 3],
            [0, 30, 40, 45, 50, 55, 55, 55], [0, 2, 3, 4, 6, 7, 8, 8], tilt=15, extras=tubers)
    round_leaf = [".ddd.", "dcecd", "decec", "dcecd", ".dcd.", "..c..", "..c.."]
    stem = ({"rhizome": rhizome_texture("K", "L", "M")},
            lambda k: [solid((6.5, GROUND, 6.5), (9.5, GROUND + 3, 9.5), "rhizome")] if k >= 6 else [])
    stalked("wasabi", {"K": "#3a6a3a", "L": "#6a9a5a", "M": "#9ac088"}, round_leaf, (5, 7), [0, 2, 3, 4, 5, 6, 7, 7],
            [0, 15, 20, 22, 25, 28, 30, 30], [0, 2, 3, 4, 5, 6, 7, 8], tilt=30, extras=stem)


def roselle():
    palette = {**NIGHTSHADE, "s": "#8a1e2a", "W": "#f4e8a0", "Y": "#8a1e2a", "G": "#6a9a3a", "H": "#8ab850", "I": "#b8d880",
               "P": "#7a0e1e", "Q": "#b81e30", "R": "#e0485a"}
    fruiting_plant("roselle", palette, (5, 6), round_fruit(3), height=12)


def cardoon():
    palette = {"a": "#2a3a34", "b": "#3e5248", "c": "#566e60", "d": "#748e7c", "e": "#98ae9e", "f": "#bccabe",
               "P": "#7a3a9a", "Q": "#a868c8", "G": "#5a6a4a", "H": "#7a8a5a"}
    jagged = ["d.d.d.", ".dcdc.", "d.ced.", ".dcec.", "d.ced.", ".dcdc.", "d.c.d.", "..c...", "..c..."]
    textures = {"leaf": padded(jagged), "stem": stalk_strip(["c" * 16]),
                "head": ["".join("H" if (x + y) % 2 else "G" for x in range(16)) for y in range(16)],
                "tuft": bottom_padded(["P.Q.P", ".QPQ.", "PQPQP"])}
    big = (6, 9)
    stages = [ring_of("leaf", big, 4, 40), ring_of("leaf", big, 6, 35), ring_of("leaf", big, 8, 30) + ring_of("leaf", big, 3, 60, turn=30),
              ring_of("leaf", big, 8, 25) + [rod(8, 8, GROUND, GROUND + 13, "stem"),
                                             solid((6.5, GROUND + 13, 6.5), (9.5, GROUND + 15, 9.5), "head")]
              + tuft("tuft", (5, 3), (8, 8), 20, 0, ground=GROUND + 15)]
    build("cardoon", palette, textures, stages, "leaf")


def oyster_mushrooms():
    palette = {"a": "#8a8478", "b": "#aaa498", "c": "#c8c2b4", "d": "#e0dace", "e": "#f0ece2"}
    cap = ["".join("e" if (x + y) % 5 == 0 else "d" if y < 8 else "c" for x in range(16)) for y in range(16)]
    gills = ["".join("b" if x % 2 else "a" for x in range(16)) for _ in range(16)]
    textures = {"cap": cap, "gills": gills}

    def shelf(x, z, y, w, d):
        faces = {"up": {"uv": [0, 0, w, d], "texture": "#cap"}, "down": {"uv": [0, 0, w, d], "texture": "#gills"},
                 "north": {"uv": [0, 0, w, 1], "texture": "#cap"}, "south": {"uv": [0, 0, w, 1], "texture": "#cap"},
                 "east": {"uv": [0, 0, d, 1], "texture": "#cap"}, "west": {"uv": [0, 0, d, 1], "texture": "#cap"}}
        return {"from": [x - w / 2, GRASS + y, z - d / 2], "to": [x + w / 2, GRASS + y + 1, z + d / 2], "faces": faces}
    layout = [(6, 6, 0, 4, 3), (10, 9, 1, 5, 4), (7, 10, 2, 4, 3), (9.5, 5.5, 3, 4, 3), (6, 8, 4, 5, 4), (10, 7, 5, 4, 3)]
    stages = [[shelf(*l) for l in layout[:n]] for n in (1, 3, 4, 6)]
    build("oyster_mushrooms", palette, textures, stages, "cap")


def roots_and_tropicals():
    zingibers()
    clumps()
    pineapple()
    stalked_crops()
    roselle()
    cardoon()
    oyster_mushrooms()


# Sea plants and shellfish beds, under water: laver in dark red-purple ribbons, sea lettuce in bright green frilly sheets,
# mussels in blue-black clusters, oysters in rough grey shells; each fuller at each of its four stages.
def sea_life():
    def blades(plant, palette, blade, counts, heights, frills=False):
        textures = {"blade": bottom_padded(blade)}
        stages = []
        for n, h in zip(counts, heights):
            parts = []
            for i in range(n):
                parts += tuft("blade", (5, h), WAKAME_SPOTS[i], i * 61, (-1) ** i * (14 + i * 3), ground=0)
            stages.append(parts)
        build(plant, palette, textures, stages, "blade")
        with open(f"{A}/blockstates/{plant}.json", "w") as f:
            json.dump({"variants": {f"age={a}": {"model": NS + f"{plant}_stage{a}"} for a in range(4)}}, f, indent=2)
            f.write("\n")
    laver = {"a": "#2a0e1e", "b": "#45182e", "c": "#62263e", "d": "#80364c", "e": "#a04e62"}
    blades("laver_plant", laver, ["..e..", ".edc.", "edcbd", ".dcbc", "cdbc.", ".cbd.", "dcb..", ".bcd.", "..cb.", ".bc..",
                                   "..b..", "..b.."], (2, 3, 5, 6), (5, 7, 9, 11))
    sea_lettuce = {"a": "#1e5a24", "b": "#2c7a2e", "c": "#40983a", "d": "#5cb84a", "e": "#8ad868"}
    blades("sea_lettuce_plant", sea_lettuce, ["e.e.e", "dedcd", "cdedc", "ecdce", "dcecd", ".dcd.", "..c..", "..b.."],
           (2, 3, 4, 6), (4, 6, 7, 8))

    def shells(plant, palette, shell, spots, sizes):
        textures = {"shell": shell, "inside": ["".join("i" if (x + y) % 3 else "j" for x in range(16)) for y in range(16)]}
        stages = []
        for count in (2, 4, 6, 8):
            parts = []
            for i, (x, z, turn) in enumerate(spots[:count]):
                w, l, h = sizes[i % len(sizes)]
                rotation = {"origin": [x, 0, z], "x": 0, "y": turn, "z": 0}
                parts.append(solid((x - w / 2, 0, z - l / 2), (x + w / 2, h, z + l / 2), "shell", top="shell", rotation=rotation))
            stages.append(parts)
        build(plant, palette, textures, stages, "shell")
        with open(f"{A}/blockstates/{plant}.json", "w") as f:
            json.dump({"variants": {f"age={a}": {"model": NS + f"{plant}_stage{a}"} for a in range(4)}}, f, indent=2)
            f.write("\n")
    spots = [(6, 7, 20), (10, 9, 110), (8, 11, 60), (11, 5, 160), (5, 11, 80), (9, 6, 30), (12, 12, 140), (4, 5, 100)]
    mussel = {"a": "#10141e", "b": "#1e2636", "c": "#2e3a52", "d": "#4a5a7a", "i": "#e0a860", "j": "#c88a48"}
    shells("mussel_bed", mussel, ["".join("d" if x in (3, 4) and y % 4 else "c" if (x + y) % 5 else "b" for x in range(16))
                                   for y in range(16)], spots, [(2, 4.5, 1.5), (1.8, 4, 1.3)])
    oyster = {"a": "#4a4a44", "b": "#6e6a60", "c": "#928c80", "d": "#b8b2a4", "i": "#e8e4dc", "j": "#d0ccc0"}
    shells("oyster_bed", oyster, ["".join("d" if (x * 3 + y) % 7 == 0 else "b" if y % 3 == 0 else "c" for x in range(16))
                                  for y in range(16)], spots, [(4, 5, 1.5), (3.5, 4, 1.2)])


# The prawn trap: a wicker frame round netting, with a funnel mouth.
def prawn_trap():
    palette = {"h": "#5a4024", "i": "#8a6a40", "j": "#b0905a", "n": "#c8c0a0", "m": "#7a7460"}
    net = ["".join("n" if (x + y) % 4 == 0 or (x - y) % 4 == 0 else "." for x in range(16)) for y in range(16)]
    frame = [("i" * 16) if y in (0, 15) else "".join("i" if x in (0, 15) else "." for x in range(16)) for y in range(16)]
    side = ["".join(frame[y][x] if frame[y][x] != "." else net[y][x] for x in range(16)) for y in range(16)]
    mouth = ["".join("h" if 5 <= x <= 10 and 5 <= y <= 10 and (x in (5, 10) or y in (5, 10)) else side[y][x] for x in range(16))
             for y in range(16)]
    draw("prawn_trap_side", side, palette)
    draw("prawn_trap_mouth", mouth, palette)
    faces = {d: {"texture": "#side"} for d in ("north", "south", "up", "down")}
    faces["east"] = faces["west"] = {"texture": "#mouth"}
    model = {"parent": "minecraft:block/block", "textures": {"particle": NS + "prawn_trap_side", "side": NS + "prawn_trap_side",
                                                             "mouth": NS + "prawn_trap_mouth"},
             "elements": [{"from": [2, 0, 3], "to": [14, 8, 13], "faces": faces},
                          {"from": [2.01, 0.01, 3.01], "to": [13.99, 7.99, 12.99],
                           "faces": {d: {"texture": "#side"} for d in ("north", "south", "east", "west", "up", "down")}}]}
    write_model("prawn_trap", model)


# Rich soil and rich farmland: dark, crumbly loam flecked with compost; rock salt ore: stone veined with pink-white salt;
# palm and cinnamon trunks with their own bark; and the trellis in pale cane.
def speckled(base, flecks, seed, chance=0.18):
    return ["".join(flecks[int(noise(x, y, seed) * len(flecks) * 7) % len(flecks)] if noise(x, y, seed + 3) < chance else base[(x + y * 3) % len(base)]
                    for x in range(16)) for y in range(16)]


def small_blocks():
    soil = {"a": "#2a1c12", "b": "#3a2818", "c": "#4a3420", "d": "#5e4428", "g": "#4a5a2a", "h": "#7a6040"}
    draw("rich_soil", speckled("bcbcab", "ghd", 51), soil)
    farm = {**soil, "m": "#1e140c", "n": "#2a1c12"}
    top = ["".join("m" if y % 4 == 0 else ch for ch in row) for y, row in enumerate(speckled("bcb", "ghd", 52))]
    draw("rich_farmland", top, farm)
    draw("rich_farmland_moist", recolour(top, {"b": "a", "c": "b", "d": "c", "m": "a"}), farm)
    write_model("rich_soil", {"parent": "minecraft:block/cube_all", "textures": {"all": NS + "rich_soil"}})
    for name, top_name in (("rich_farmland", "rich_farmland"), ("rich_farmland_moist", "rich_farmland_moist")):
        write_model(name, {"parent": "minecraft:block/template_farmland", "textures": {"dirt": NS + "rich_soil", "top": NS + top_name}})

    stone = {"a": "#6a6a6a", "b": "#7e7e7e", "c": "#929292", "p": "#e8c8c8", "q": "#f8ecec", "r": "#c89a9a"}
    ore = [list(r) for r in speckled("bcbab", "c", 53, 0.1)]
    for x0, y0 in [(2, 3), (9, 2), (5, 9), (12, 10), (1, 12)]:
        for dx, dy, ch in [(0, 0, "q"), (1, 0, "p"), (0, 1, "p"), (1, 1, "r"), (2, 1, "p"), (1, 2, "r")]:
            if x0 + dx < 16 and y0 + dy < 16:
                ore[y0 + dy][x0 + dx] = ch
    draw("rock_salt_ore", ["".join(r) for r in ore], stone)
    write_model("rock_salt_ore", {"parent": "minecraft:block/cube_all", "textures": {"all": NS + "rock_salt_ore"}})

    palm = {"a": "#3a2e20", "b": "#5a4a34", "c": "#7a6648", "d": "#9a8462", "s": "#e0a030", "t": "#f8d070", "w": "#c8b088"}
    palm_side = ["".join("a" if y % 4 == 3 else "c" if (x + (y // 4) * 2) % 4 == 0 else "b" for x in range(16)) for y in range(16)]
    tapped = [list(r) for r in palm_side]
    for y in range(6, 11):
        for x in range(6, 10):
            tapped[y][x] = "w"
    for x, y in [(7, 10), (8, 11), (7, 12), (8, 13)]:
        tapped[y][x] = "s" if y % 2 else "t"
    palm_end = ["".join("a" if round(_m.hypot(x - 7.5, y - 7.5)) % 3 == 0 else "d" for x in range(16)) for y in range(16)]
    draw("palm_log", palm_side, palm)
    draw("palm_log_tapped", ["".join(r) for r in tapped], palm)
    draw("palm_log_top", palm_end, palm)
    cinnamon = {"a": "#3a1e12", "b": "#5a2e1a", "c": "#7a4228", "d": "#9a5a38", "e": "#c07a4a", "f": "#d89a6a"}
    bark = ["".join("a" if x % 5 == 0 else "c" if (x + y) % 3 else "b" for x in range(16)) for y in range(16)]
    stripped = ["".join("e" if x % 4 == 0 else "f" if (x + y * 2) % 7 == 0 else "d" for x in range(16)) for y in range(16)]
    ends = ["".join("b" if round(_m.hypot(x - 7.5, y - 7.5)) % 3 == 0 else "e" for x in range(16)) for y in range(16)]
    draw("cinnamon_log", bark, cinnamon)
    draw("cinnamon_log_stripped", stripped, cinnamon)
    draw("cinnamon_log_top", ends, cinnamon)
    logs = {"palm_log": ("palm_log_tapped", "palm_log"), "cinnamon_log": ("cinnamon_log", "cinnamon_log_stripped")}
    for log, (ready_side, spent_side) in logs.items():
        variants = {}
        for ready, side in ((True, ready_side), (False, spent_side)):
            name = f"{log}_{'ready' if ready else 'spent'}"
            write_model(name, {"parent": "minecraft:block/cube_column", "textures": {"end": NS + f"{log}_top", "side": NS + side}})
            write_model(name + "_horizontal", {"parent": "minecraft:block/cube_column_horizontal",
                                               "textures": {"end": NS + f"{log}_top", "side": NS + side}})
            r = str(ready).lower()
            variants[f"axis=y,ready={r}"] = {"model": NS + name}
            variants[f"axis=x,ready={r}"] = {"model": NS + name + "_horizontal", "x": 90, "y": 90}
            variants[f"axis=z,ready={r}"] = {"model": NS + name + "_horizontal", "x": 90}
        with open(f"{A}/blockstates/{log}.json", "w") as f:
            json.dump({"variants": variants}, f, indent=2)
            f.write("\n")
        # The trunk's item and its old models now point at the bark.
        write_model(log, {"parent": "minecraft:block/cube_column", "textures": {"end": NS + f"{log}_top", "side": NS + ready_side}})

    cane = {"h": "#8a6a3a", "i": "#b8955a", "j": "#d8b87a", "k": "#ecd49a"}
    draw("trellis_wood", ["".join("k" if x % 4 == 1 else "h" if y % 6 == 5 else "j" if x % 4 == 2 else "i" for x in range(16))
                          for y in range(16)], cane)
    frame = [{**part, "faces": {side: {"texture": "#post"} for side in ("north", "south", "east", "west", "up", "down")}}
             for part in TRELLIS_FRAME]
    write_model("trellis", {"parent": "minecraft:block/block", "textures": {"particle": NS + "trellis_wood", "post": NS + "trellis_wood"},
                            "elements": frame})


def sea_and_small():
    sea_life()
    prawn_trap()
    small_blocks()


# Cooking stations: the same shapes as before, painted in their own materials. Each texture is a 16x16 grid drawn from a
# function of (x, y) to a colour, lit from the upper left like the plants.

def paint(name, fn):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            c = fn(x, y)
            if c:
                img.putpixel((x, y), (*rgb(c), 255))
    path = f"{A}/textures/block/{name}.png"
    img.save(path)


def shade(c, k):
    r, g, b = rgb(c)
    return "#%02x%02x%02x" % tuple(max(0, min(255, round(v * k))) for v in (r, g, b))


def planks(light, mid, dark, seed):
    """Boards running across, each a shade of its own, with grain and a dark seam between them."""
    def fn(x, y):
        board = y // 4
        if y % 4 == 3:
            return dark
        base = (light, mid, mid, light)[(board + seed) % 4]
        if noise(x, y, seed) < 0.12 or (x + board * 5 + seed) % 16 == 0:
            return shade(base, 0.85)
        return base
    return fn


def log_side(light, mid, dark, seed):
    """Wood seen along the grain: long streaks running up."""
    def fn(x, y):
        streak = noise(x, 0, seed)
        c = light if streak < 0.3 else mid if streak < 0.8 else dark
        return shade(c, 0.9) if noise(x, y, seed + 1) < 0.08 else c
    return fn


def end_grain(light, mid, dark):
    """A cut log end: growth rings round the middle."""
    def fn(x, y):
        r = _m.hypot(x - 7.5, y - 7.5)
        if r > 7.3:
            return dark
        return mid if round(r) % 3 == 0 else light
    return fn


def metal(base, seed, speckle=0.1, shine=True):
    """Metal: one tone, lighter along the upper left edge, darker along the lower right, flecked a little."""
    def fn(x, y):
        c = base
        if shine and (x == 0 or y == 0):
            c = shade(base, 1.25)
        elif x == 15 or y == 15:
            c = shade(base, 0.7)
        elif noise(x, y, seed) < speckle:
            c = shade(base, 1.12 if noise(x, y, seed + 2) < 0.5 else 0.88)
        return c
    return fn


def glaze(base, seed):
    """Fired clay or glaze: smooth, with a soft sheen across the middle."""
    def fn(x, y):
        c = base
        if 4 <= x <= 6 and 2 <= y <= 13:
            c = shade(base, 1.12)
        if noise(x, y, seed) < 0.06:
            c = shade(base, 0.9)
        return c
    return fn


def bricks(brick, mortar, seed):
    def fn(x, y):
        row = y // 4
        if y % 4 == 3 or (x + (4 if row % 2 else 0)) % 8 == 7:
            return mortar
        return shade(brick, 1.1 if noise(x // 8 + row * 3, row, seed) < 0.4 else 0.95)
    return fn


def wicker(light, dark):
    def fn(x, y):
        return light if ((x // 2) + (y // 2)) % 2 else dark
    return fn


def chain(x, y):
    if 6 <= x <= 9:
        link = y % 6
        if (link in (0, 5) and 7 <= x <= 8) or (link in (1, 2, 3, 4) and x in (6, 9)):
            return "#5a5e66" if x == 9 else "#9aa0aa"
    return None


def stove_front(lit):
    def fn(x, y):
        if 3 <= x <= 12 and 6 <= y <= 13:                 # the oven door
            if 5 <= x <= 10 and 8 <= y <= 11:              # its window
                return ("#f8a030" if (x + y) % 2 else "#e86a18") if lit else "#1a1a1e"
            return "#3a3c42" if y == 6 or x in (3, 12) else "#2e3036"
        if y == 7 and x in (1, 14):
            return "#8a8e96"                               # door hinges
        if 1 <= y <= 3 and x in (3, 6, 9, 12):
            return "#c8ccd2" if y == 2 else "#6a6e76"      # dials
        return metal("#24262c", 31)(x, y)
    return fn


def stove_top(x, y):
    for cx, cy in ((4, 4), (11, 4), (4, 11), (11, 11)):
        r = _m.hypot(x - cx + 0.5, y - cy + 0.5)
        if 1.5 < r < 3.2:
            return "#4a4c54" if r > 2.4 else "#6a6c74"
    return metal("#2a2c32", 32, shine=False)(x, y)


def oven_front(lit):
    def fn(x, y):
        r = _m.hypot((x - 7.5) / 5.5, (y - 12) / 7.5)
        if r < 1 and y >= 5:
            if lit:
                return "#f8c040" if r < 0.5 else "#e86a18" if r < 0.8 else "#8a2a10"
            return "#120c0a" if r < 0.85 else "#2a1a14"
        if 0.95 <= r < 1.2 and y >= 4:
            return "#8a3a28"                              # arch of bricks round the mouth
        return bricks("#a8502e", "#c8b8a0", 33)(x, y)
    return fn


def grill_front(lit):
    def fn(x, y):
        if 3 <= y <= 12 and x % 3 == 1 and 2 <= x <= 13:
            return ("#f08a20" if y > 8 else "#c84a10") if lit else "#0e0e10"   # vents
        return metal("#2c2a2a", 34)(x, y)
    return fn


def grate(x, y):
    if x % 3 == 0:
        return "#5a5a5e"
    return "#1a1818" if y % 4 else "#3a3a3e"


def millstone(x, y):
    r = _m.hypot(x - 7.5, y - 7.5)
    if r < 1.6:
        return "#3a3a3a"
    if r < 7.5:
        a = _m.atan2(y - 7.5, x - 7.5)
        return "#8a8680" if int((a + _m.pi) * 4 / _m.pi) % 2 else "#a8a49c"
    return "#6a6660"


def stone(seed):
    def fn(x, y):
        n = noise(x, y, seed)
        return "#8a8a88" if n < 0.3 else "#9a9a96" if n < 0.8 else "#727270"
    return fn


def barrel_side(x, y):
    if y in (2, 3, 12, 13):
        return "#3a3c42" if y in (3, 12) else "#6a6e76"           # iron hoops
    stave = x // 4
    c = ("#8a6238", "#7a5430", "#946a40", "#806034")[stave]
    return shade(c, 0.75) if x % 4 == 3 else c


def barrel_top(x, y):
    r = _m.hypot(x - 7.5, y - 7.5)
    if r > 7.2:
        return "#3a3c42"
    return "#6a4a28" if x % 4 == 3 else "#8a6238"


def counter_top(x, y):
    if x in (0, 15) or y in (0, 15):
        return "#6a4a30"
    return "#c8c4bc" if noise(x, y, 35) < 0.7 else "#b8b4ac"


def cupboard(x, y):
    if y < 2:
        return "#6a4a30"
    if x in (0, 7, 8, 15) or y in (2, 15):
        return "#5a3e28"
    if y == 8 and x in (5, 10):
        return "#c8b060"                                          # knobs
    return planks("#94704a", "#86643e", "#5a3e28", 2)(x, y)


def screw_front(wood_light, wood_mid, wood_dark):
    """A press: a frame of posts round slats, a screw down the middle."""
    def fn(x, y):
        if x in (7, 8):
            return "#6a6e76" if (y + x) % 2 else "#9aa0aa"
        if x in (0, 1, 14, 15) or y in (0, 1):
            return wood_dark
        return wood_light if (x // 3) % 2 else wood_mid
    return fn


def station_textures():
    oak, spruce, dark_oak = ("#b8905a", "#a8804c", "#6a4a2a"), ("#8a6440", "#7a5634", "#4a3420"), ("#5a3e24", "#4a321c", "#2a1c10")
    paint("station_oak_planks", planks(*oak, 1))
    paint("station_spruce_planks", planks(*spruce, 2))
    paint("station_oak_wood", log_side("#c8a070", "#b48a5a", "#8a6640", 3))
    paint("station_spruce_wood", log_side("#a07850", "#8a6440", "#6a4a2c", 4))
    paint("station_dark_wood", log_side("#5a3e24", "#4a321c", "#2a1c10", 5))
    paint("station_end_grain", end_grain("#d0aa78", "#a8804c", "#7a5634"))
    paint("station_cast_iron", metal("#2a2a2e", 6))
    paint("station_iron", metal("#8a8e96", 7))
    paint("station_copper", metal("#c06a38", 8))
    paint("station_copper_inside", metal("#8a4a2a", 9, shine=False))
    paint("station_terracotta", glaze("#9a5a3a", 10))
    paint("station_glaze", glaze("#e8dcc0", 11))
    paint("station_brown_glaze", glaze("#6a3a22", 12))
    paint("station_enamel", glaze("#f4f4ee", 13))
    paint("station_stone", stone(14))
    paint("station_wicker", wicker("#d8b878", "#a88848"))
    paint("station_slats", lambda x, y: "#a07850" if x % 4 < 3 else None)
    paint("station_mesh", lambda x, y: "#8a8e96" if x % 3 == 0 or y % 3 == 0 else None)
    paint("station_chain", chain)
    paint("station_bricks", bricks("#a8502e", "#c8b8a0", 15))
    paint("station_stove_front", stove_front(False))
    paint("station_stove_front_on", stove_front(True))
    paint("station_stove_top", stove_top)
    paint("station_stove_side", metal("#24262c", 16))
    paint("station_oven_front", oven_front(False))
    paint("station_oven_front_on", oven_front(True))
    paint("station_grill_front", grill_front(False))
    paint("station_grill_front_on", grill_front(True))
    paint("station_grate", grate)
    paint("station_millstone", millstone)
    paint("station_barrel_side", barrel_side)
    paint("station_barrel_top", barrel_top)
    paint("station_counter_top", counter_top)
    paint("station_cupboard", cupboard)
    paint("station_press_front", screw_front("#a07850", "#8a6440", "#5a3e28"))
    paint("station_cheese_press_front", screw_front("#c8a070", "#b48a5a", "#7a5634"))
    paint("station_screw_top", lambda x, y: "#9aa0aa" if _m.hypot(x - 7.5, y - 7.5) < 2.5 else
          planks("#a07850", "#8a6440", "#5a3e28", 3)(x, y))
    paint("station_hopper", lambda x, y: "#1a1a1e" if _m.hypot(x - 7.5, y - 7.5) < 4.5 else metal("#8a8e96", 17)(x, y))


# What each station's textures become, by the role its model gives them.
STATION_TEXTURES = {
    "aebleskive_pan": {"particle": "cast_iron", "iron": "cast_iron", "handle": "dark_wood"},
    "ageing_shelf": {"particle": "spruce_planks", "leg": "spruce_wood", "board": "spruce_planks"},
    "brine_barrel": {"top": "barrel_top", "bottom": "barrel_top", "side": "barrel_side"},
    "butcher_block": {"side": "oak_wood", "bottom": "end_grain", "top": "end_grain"},
    "butter_churn": {"particle": "oak_wood", "side": "oak_wood", "inner": "oak_planks"},
    "cheese_press": {"top": "screw_top", "front": "cheese_press_front", "side": "oak_wood"},
    "cooking_pot": {"particle": "copper", "side": "copper", "inside": "copper_inside"},
    "cutting_board": {"particle": "oak_planks", "board": "oak_planks"},
    "dairy_pan": {"particle": "copper", "side": "copper", "inner": "enamel"},
    "drying_rack": {"particle": "spruce_wood", "leg": "spruce_wood", "board": "slats"},
    "fermentation_crock": {"particle": "brown_glaze", "side": "brown_glaze", "inner": "glaze"},
    "grill": {"bottom": "cast_iron", "front": "grill_front", "side": "cast_iron", "top": "grate"},
    "grill_on": {"bottom": "cast_iron", "front": "grill_front_on", "side": "cast_iron", "top": "grate"},
    "hanging_hook": {"particle": "chain", "chain": "chain"},
    "hanging_hook_wall": {"particle": "chain", "chain": "chain"},
    "mill": {"top": "stone", "front": "millstone", "side": "stone"},
    "mixing_bowl": {"particle": "terracotta", "side": "terracotta", "inner": "glaze"},
    "mortar": {"particle": "stone", "side": "stone", "inner": "stone"},
    "oven": {"bottom": "bricks", "front": "oven_front", "side": "bricks", "top": "bricks"},
    "oven_on": {"bottom": "bricks", "front": "oven_front_on", "side": "bricks", "top": "bricks"},
    "prep_counter": {"top": "counter_top", "bottom": "spruce_planks", "side": "cupboard"},
    "press": {"top": "screw_top", "front": "press_front", "side": "spruce_wood"},
    "proofing_basket": {"particle": "wicker", "side": "wicker", "inner": "wicker"},
    "skillet": {"particle": "cast_iron", "pan": "cast_iron", "handle": "oak_wood"},
    "stove": {"bottom": "cast_iron", "front": "stove_front", "side": "stove_side", "top": "stove_top"},
    "stove_on": {"bottom": "cast_iron", "front": "stove_front_on", "side": "stove_side", "top": "stove_top"},
    "strainer": {"particle": "iron", "mesh": "mesh", "rim": "iron"},
    "wok": {"particle": "cast_iron", "pan": "cast_iron", "handle": "dark_wood"},
}


def iron_box(start, end, side="side", top=None):
    """A box of the station's metal, its faces showing the texture's matching part."""
    (x0, y0, z0), (x1, y1, z1) = start, end
    top = top or side
    faces = {"north": {"uv": [x0, 16 - y1, x1, 16 - y0], "texture": "#" + side}, "south": {"uv": [x0, 16 - y1, x1, 16 - y0], "texture": "#" + side},
             "east": {"uv": [z0, 16 - y1, z1, 16 - y0], "texture": "#" + side}, "west": {"uv": [z0, 16 - y1, z1, 16 - y0], "texture": "#" + side},
             "up": {"uv": [x0, z0, x1, z1], "texture": "#" + top}, "down": {"uv": [x0, z0, x1, z1], "texture": "#" + side}}
    return {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces}


def grinder_models():
    """The meat grinder clamped to its board, a barrel, a hopper on top, a crank on its side; and the sausage stuffer, an
    upright cylinder on a base, a plunger and crank on top, a spout at its foot. Both within the outlines they had."""
    grinder = [iron_box((4, 0, 4), (12, 2, 12)), iron_box((7, 2, 7), (9, 4, 9)), iron_box((5, 4, 3), (11, 9, 13)),
               iron_box((4, 9, 5), (12, 12, 11), top="top"), iron_box((11, 6, 7.5), (13, 7, 8.5)), iron_box((12, 6, 7.5), (13, 10, 8.5)),
               {**iron_box((12.5, 9, 7), (13, 11, 9), side="handle"), "from": [12, 9.5, 7], "to": [13, 11.5, 9]}]
    write_model("meat_grinder", {"parent": "minecraft:block/block", "textures": {"particle": NS + "station_iron", "side": NS + "station_iron",
                                                                                  "top": NS + "station_hopper", "handle": NS + "station_dark_wood"},
                                 "elements": grinder})
    stuffer = [iron_box((4, 0, 3), (12, 1, 13)), iron_box((5, 1, 5), (11, 11, 11), top="top"), iron_box((7, 11, 7), (9, 13, 9)),
               iron_box((4, 13, 7.5), (12, 14, 8.5)), iron_box((4, 12, 7.5), (5, 14, 8.5), side="handle"),
               iron_box((7.5, 2, 11), (8.5, 3, 14))]
    write_model("sausage_stuffer", {"parent": "minecraft:block/block", "textures": {"particle": NS + "station_iron", "side": NS + "station_iron",
                                                                                     "top": NS + "station_hopper", "handle": NS + "station_dark_wood"},
                                    "elements": stuffer})


def stations():
    station_textures()
    for model, textures in STATION_TEXTURES.items():
        path = f"{A}/models/block/{model}.json"
        data = json.load(open(path))
        data["textures"] = {role: NS + "station_" + texture for role, texture in textures.items()}
        if "particle" not in data["textures"]:
            data["textures"]["particle"] = NS + "station_" + textures.get("side", next(iter(textures.values())))
        with open(path, "w") as f:
            json.dump(data, f, indent=2)
            f.write("\n")
    grinder_models()


# Stations built in 3D. Fronts face north; round things are rings of thin panels turned round their middle, with
# octagonal floors and discs, so they read as round. Surfaces that items are drawn on stay where the renderers expect
# them: the grill's grate and the butcher block's top at the block's top, the stove's top for a pot, the counter's for
# the stations stood on it, and the pans' floors at their old heights.

def part(start, end, tex, rotation=None):
    """A box; tex is one texture key, or a dict of side/top/bottom/front/back keys. UVs follow its place in the block."""
    (x0, y0, z0), (x1, y1, z1) = start, end
    t = tex if isinstance(tex, dict) else {"side": tex}
    side = t.get("side")
    keys = {"north": t.get("front", side), "south": t.get("back", side), "east": side, "west": side,
            "up": t.get("top", side), "down": t.get("bottom", side)}
    def uv(a0, a1, b0, b1):
        clip = lambda v: max(0.0, min(16.0, v))
        return [clip(a0), clip(b0), clip(a1), clip(b1)]
    uvs = {"north": uv(16 - x1, 16 - x0, 16 - y1, 16 - y0), "south": uv(x0, x1, 16 - y1, 16 - y0),
           "east": uv(16 - z1, 16 - z0, 16 - y1, 16 - y0), "west": uv(z0, z1, 16 - y1, 16 - y0),
           "up": uv(x0, x1, z0, z1), "down": uv(x0, x1, 16 - z1, 16 - z0)}
    faces = {}
    for face, key in keys.items():
        if key:
            u = uvs[face]
            # A face that sticks out past the block has no texture of its own there: take a sliver at the edge.
            for lo, hi in ((0, 2), (1, 3)):
                if u[lo] == u[hi]:
                    if u[hi] >= 16:
                        u[lo] = 15.5
                    else:
                        u[hi] = u[lo] + 0.5
            faces[face] = {"uv": [round(v, 3) for v in u], "texture": "#" + key}
    element = {"from": [x0, y0, z0], "to": [x1, y1, z1], "faces": faces}
    if rotation:
        element["rotation"] = rotation
    return element


def ring(cx, cz, r, y0, y1, side, inner=None, n=12, wall=1.0, rim=None):
    """A round wall: n panels a wall thick, their outsides showing `side`, their insides `inner`, their tops `rim`."""
    w = 2 * r * _m.tan(_m.pi / n) + 0.25
    parts = []
    for i in range(n):
        p = part((cx - w / 2, y0, cz - r), (cx + w / 2, y1, cz - r + wall),
                 {"side": side, "back": inner or side, "top": rim or side, "bottom": side})
        for face in ("east", "west"):
            p["faces"].pop(face, None)
        p["faces"]["north"]["uv"] = [0, round(16 - (y1 - y0), 3), round(min(16, w), 3), 16]
        p["faces"]["south"]["uv"] = [0, round(16 - (y1 - y0), 3), round(min(16, w), 3), 16]
        p["rotation"] = {"origin": [cx, y0, cz], "x": 0, "y": round(i * 360 / n, 3), "z": 0}
        parts.append(p)
    return parts


def disc(cx, cz, r, y0, y1, top, side=None, n=12):
    """A round, flat piece: a fan of n narrow panels from the middle out, so its edge is round and stays within r."""
    w = 2 * r * _m.tan(_m.pi / n)
    parts = []
    for i in range(n):
        p = part((cx - w / 2, y0, cz - r), (cx + w / 2, y1, cz), {"side": side or top, "top": top, "bottom": side or top})
        for face in ("east", "west", "south"):
            p["faces"].pop(face, None)
        p["rotation"] = {"origin": [cx, y0, cz], "x": 0, "y": round(i * 360 / n, 3), "z": 0}
        parts.append(p)
    return parts


def vessel(cx, cz, r, y0, y1, side, inner, floor=True, n=12, wall=1.0, rim=None, floor_at=None):
    parts = ring(cx, cz, r, y0, y1, side, inner, n, wall, rim)
    if floor:
        f = y0 if floor_at is None else floor_at
        parts += disc(cx, cz, r - wall * 0.6, f, f + 1, inner, side)
    return parts


def legs(x0, z0, x1, z1, y1, tex, t=2):
    return [part((x, 0, z), (x + t, y1, z + t), tex) for x in (x0, x1 - t) for z in (z0, z1 - t)]


def station_model(name, textures, elements, particle):
    write_model(name, {"parent": "minecraft:block/block",
                       "textures": {"particle": NS + "station_" + particle, **{k: NS + "station_" + v for k, v in textures.items()}},
                       "elements": elements})


def staves(x, y):
    stave = x // 4
    c = ("#8a6238", "#7a5430", "#946a40", "#806034")[stave]
    return shade(c, 0.75) if x % 4 == 3 else c


def stations_3d():
    paint("station_staves", staves)
    paint("station_dial", lambda x, y: "#c8ccd2" if (x + y) % 3 else "#6a6e76")

    # Stove: a cast-iron range on short legs, its oven door's handle and its dials standing out from the front.
    for name, front in (("stove", "stove_front"), ("stove_on", "stove_front_on")):
        station_model(name, {"body": "stove_side", "front": front, "top": "stove_top", "iron": "cast_iron", "dial": "dial"},
                      legs(0.5, 0.5, 15.5, 15.5, 2, "iron")
                      + [part((0, 2, 1), (16, 16, 16), {"side": "body", "front": "front", "top": "top", "bottom": "iron"}),
                         part((4, 10, 0), (12, 10.8, 0.6), "dial")]
                      + [part((x, 13.2, 0.2), (x + 1.4, 14.6, 1), "dial") for x in (2.8, 5.8, 8.8, 11.8)],
                      "stove_side")

    # Oven: a brick oven on a plinth, a stepped dome, a chimney at the back, a stone sill before its mouth.
    for name, front in (("oven", "oven_front"), ("oven_on", "oven_front_on")):
        station_model(name, {"brick": "bricks", "front": front, "stone": "stone"},
                      [part((0, 0, 0), (16, 3, 16), "stone"),
                       part((1, 3, 1), (15, 12, 16), {"side": "brick", "front": "front"}),
                       part((2, 12, 2), (14, 14, 15.5), "brick"), part((4, 14, 4), (12, 15, 14), "brick"),
                       part((6, 12, 11), (10, 16, 15), "brick"), part((4, 3, 0), (12, 4, 1), "stone")],
                      "bricks")

    # Grill: a firebox on four legs with a shelf between them, the grate on top where the skewers lie.
    for name, front in (("grill", "grill_front"), ("grill_on", "grill_front_on")):
        station_model(name, {"iron": "cast_iron", "front": front, "grate": "grate"},
                      legs(1, 1, 15, 15, 8, "iron")
                      + [part((1.5, 3, 1.5), (14.5, 4, 14.5), "iron"),
                         part((1, 8, 1), (15, 15, 15), {"side": "iron", "front": "front"}),
                         part((0.5, 15, 0.5), (15.5, 16, 15.5), {"side": "iron", "top": "grate"}),
                         part((-0.5, 12, 7), (1, 13, 9), "iron"), part((15, 12, 7), (16.5, 13, 9), "iron")],
                      "cast_iron")

    # Mill: a quern, a round upper stone turning on a squared lower one, a peg to turn it, a spout for the flour.
    station_model("mill", {"stone": "stone", "face": "millstone", "wood": "oak_wood"},
                  [part((1, 0, 1), (15, 5, 15), "stone"), part((0, 4, 0), (16, 5, 16), "stone")]
                  + disc(8, 8, 6.5, 5, 9, "face", "stone")
                  + [part((10.5, 9, 7.5), (11.5, 13, 8.5), "wood"), part((7, 2, -0.5), (9, 3, 1), "stone")],
                  "stone")

    # Fruit press: a slatted round basket on a tray with a spout, between two posts and a beam, a screw and plate above.
    station_model("press", {"wood": "spruce_wood", "plank": "spruce_planks", "slats": "slats", "iron": "iron"},
                  [part((0, 0, 0), (16, 1.5, 16), "plank"), part((7, 0.5, -1), (9, 1.5, 0), "plank")]
                  + ring(8, 8, 5.5, 1.5, 11, "slats", "slats", n=12, wall=0.6)
                  + [part((0.5, 1.5, 7), (2.5, 16, 9), "wood"), part((13.5, 1.5, 7), (15.5, 16, 9), "wood"),
                     part((0.5, 13, 7), (15.5, 15, 9), "wood"), part((7.5, 9, 7.5), (8.5, 16, 8.5), "iron")]
                  + disc(8, 8, 4.5, 9, 10, "plank", "plank") + [part((5, 15.2, 7.5), (11, 16, 8.5), "iron")],
                  "spruce_planks")

    # Cheese press: a round mould on a board, a follower plate pressed down by a screw from a beam on two posts.
    station_model("cheese_press", {"wood": "oak_wood", "plank": "oak_planks", "iron": "iron"},
                  [part((1, 0, 1), (15, 1.5, 15), "plank")]
                  + vessel(8, 8, 4, 1.5, 7, "plank", "plank", floor=False, n=10)
                  + disc(8, 8, 3.4, 7, 8, "plank", "plank")
                  + [part((2, 1.5, 7), (4, 14, 9), "wood"), part((12, 1.5, 7), (14, 14, 9), "wood"),
                     part((2, 11, 7), (14, 13, 9), "wood"), part((7.5, 8, 7.5), (8.5, 15, 8.5), "iron"),
                     part((5.5, 14, 7.5), (10.5, 15, 8.5), "iron")],
                  "oak_planks")

    # Brine barrel: bellied staves in iron hoops under a round lid.
    tiers = [(0, 2, 6.2), (2, 5, 6.8), (5, 11, 7.2), (11, 14, 6.8), (14, 16, 6.2)]
    barrel = []
    for y0, y1, r in tiers:
        barrel += ring(8, 8, r, y0, y1, "staves", "staves", n=14, wall=1.2)
    for y0, r in ((2.5, 7.0), (12.5, 7.0)):
        barrel += ring(8, 8, r + 0.25, y0, y0 + 1, "iron", "iron", n=14, wall=0.5)
    barrel += disc(8, 8, 6.0, 0, 1, "lid", "staves") + disc(8, 8, 5.6, 14.5, 15.3, "lid", "staves")
    station_model("brine_barrel", {"staves": "staves", "iron": "cast_iron", "lid": "barrel_top"}, barrel, "staves")

    # Butcher block: a thick block of end grain on four stout legs braced near the floor.
    station_model("butcher_block", {"wood": "oak_wood", "grain": "end_grain"},
                  [part((0, 9, 0), (16, 16, 16), {"side": "wood", "top": "grain", "bottom": "grain"})]
                  + legs(1, 1, 15, 15, 9, "wood", t=3)
                  + [part((2.5, 2, 2.5), (13.5, 3, 4), "wood"), part((2.5, 2, 12), (13.5, 3, 13.5), "wood")],
                  "end_grain")

    # Prep counter: a stone top overhanging a cupboard of doors, a dark plinth at its foot.
    def doors(z, front):
        return [part((1.6, 1.2, z), (7.8, 13, z + 0.6), {"side": "door", front: "door"}),
                part((8.2, 1.2, z), (14.4, 13, z + 0.6), {"side": "door", front: "door"}),
                part((6.8, 7, z - 0.5 if front == "front" else z + 0.6), (7.4, 8, z if front == "front" else z + 1.1), "knob"),
                part((8.6, 7, z - 0.5 if front == "front" else z + 0.6), (9.2, 8, z if front == "front" else z + 1.1), "knob")]
    paint("station_door", planks("#94704a", "#86643e", "#5a3e28", 6))
    paint("station_knob", lambda x, y: "#c8b060")
    station_model("prep_counter", {"top": "counter_top", "wood": "spruce_planks", "door": "door", "knob": "knob", "dark": "dark_wood"},
                  [part((0, 14, 0), (16, 16, 16), {"side": "wood", "top": "top"}), part((1.5, 1, 1.5), (14.5, 14, 14.5), "wood"),
                   part((2, 0, 2), (14, 1, 14), "dark")] + doors(0.9, "front") + doors(14.5, "back"),
                  "counter_top")

    # Round vessels and pans, their floors where the renderers set things on them.
    station_model("cooking_pot", {"side": "copper", "inside": "copper_inside"},
                  vessel(8, 8, 6, 0, 10, "side", "inside", floor_at=1) + disc(8, 8, 5.4, 0, 1, "side")
                  + [part((0.5, 7, 7), (2.5, 8, 9), "side"), part((13.5, 7, 7), (15.5, 8, 9), "side")], "copper")
    station_model("skillet", {"pan": "cast_iron", "handle": "oak_wood"},
                  vessel(8, 8, 6, 0, 2, "pan", "pan", floor_at=0) + [part((13.5, 1, 7), (16, 2, 9), "handle")], "cast_iron")
    station_model("wok", {"pan": "cast_iron", "handle": "dark_wood"},
                  disc(8, 8, 4, 0, 1, "pan") + vessel(8, 8, 5.6, 1, 2, "pan", "pan", floor_at=1)
                  + ring(8, 8, 7, 2, 5, "pan", "pan", wall=1) + [part((14.5, 3, 7), (16, 4, 9), "handle")], "cast_iron")
    station_model("aebleskive_pan", {"iron": "cast_iron", "handle": "dark_wood"},
                  disc(8, 8, 7, 0, 2, "iron") + ring(8, 8, 7, 2, 3, "iron", "iron") + [part((14.5, 1, 7), (16, 2, 9), "handle")],
                  "cast_iron")
    station_model("strainer", {"mesh": "mesh", "rim": "iron"},
                  disc(8, 8, 4, 0, 1, "mesh") + ring(8, 8, 5, 1, 5, "mesh", "mesh", wall=0.5)
                  + ring(8, 8, 6, 5, 6, "rim", "rim", wall=1.5), "iron")
    station_model("mixing_bowl", {"side": "terracotta", "inner": "glaze"},
                  disc(8, 8, 3.6, 0, 1, "side") + vessel(8, 8, 4.6, 1, 3, "side", "inner", floor_at=1)
                  + ring(8, 8, 5.4, 3, 6, "side", "inner"), "terracotta")
    station_model("mortar", {"side": "stone", "inner": "stone"},
                  disc(8, 8, 4, 0, 2, "side") + ring(8, 8, 4.4, 2, 6, "side", "inner", wall=1.5), "stone")
    station_model("dairy_pan", {"side": "copper", "inner": "enamel"},
                  vessel(8, 8, 7, 0, 4, "side", "inner", n=14, floor_at=0), "copper")
    station_model("proofing_basket", {"side": "wicker", "inner": "wicker"},
                  vessel(8, 8, 6, 0, 5, "side", "inner", floor_at=0), "wicker")
    crock = []
    for y0, y1, r in ((0, 2, 5.4), (2, 9, 6.2), (9, 12, 5.6), (12, 14, 4.6)):
        crock += ring(8, 8, r, y0, y1, "side", "inner", wall=1.2)
    station_model("fermentation_crock", {"side": "brown_glaze", "inner": "glaze"},
                  crock + disc(8, 8, 5, 0, 1, "inner", "side") + [part((1.2, 9, 7), (2.5, 10.5, 9), "side"),
                                                                  part((13.5, 9, 7), (14.8, 10.5, 9), "side")], "brown_glaze")
    station_model("butter_churn", {"side": "oak_wood", "inner": "oak_planks", "iron": "cast_iron"},
                  ring(8, 8, 5, 0, 7, "side", "inner") + ring(8, 8, 4.5, 7, 14, "side", "inner")
                  + ring(8, 8, 5.3, 2, 3, "iron", "iron", wall=0.5) + ring(8, 8, 4.8, 11, 12, "iron", "iron", wall=0.5)
                  + disc(8, 8, 4.3, 0, 1, "inner") + disc(8, 8, 4, 13, 14, "inner", "side")
                  + [part((7.25, 1, 7.25), (8.75, 16, 8.75), "inner"), part((6, 15, 7.5), (10, 16, 8.5), "inner")], "oak_wood")


def main():
    cabbage()
    tomato()
    barley()
    onion_build("onion", ONION)
    onion_build("red_onion", RED_ONION)
    radish()
    courgette()
    strawberry()
    corn()
    grape()
    rice()
    rhubarb()
    dill()
    fruit_trees()
    raspberry()
    asparagus()
    wakame()
    white_bean()
    other_beans()
    nightshades()
    herbs_and_oilseeds()
    roots_and_tropicals()
    sea_and_small()
    stations()
    stations_3d()
    bushes()
    cabbage("red_cabbage", RED_CABBAGE)
    lettuce()
    corn("blue_corn", BLUE_CORN)
    corn("purple_corn", PURPLE_CORN)
    grape("grape_vine", BLACK_GRAPE)
    grape("red_grape_vine", RED_GRAPE)
    rice("glutinous_rice", GLUTINOUS_RICE)
    barley("rye", RYE, RYE_EARED)
    onion_build("garlic", GARLIC, sink=5)
    onion_build("shallot", SHALLOT, SHALLOT_CLUSTER)
    spring_onion()
    cucumber()
    hops()
    long_bean()
    peppercorn()
    vanilla()
    betel()


if __name__ == "__main__":
    main()
