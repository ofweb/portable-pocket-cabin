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


def trellis_planes(texture):
    """The vine flat against both faces of the trellis, the way a climber grows on one."""
    face = {"uv": [0, 0, 16, 16], "texture": texture}
    return [{"from": [0, 0, z], "to": [16, 16, z], "shade": False, "faces": {"north": face, "south": face}} for z in (6.9, 9.1)]


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


def build(plant, palette, textures, stages, particle, extra=None):
    """Draws a plant's textures and writes one model per growth stage. `extra` names textures it borrows."""
    for name, grid in textures.items():
        draw(f"{plant}_{name}", grid, palette)
    names = {"particle": f"{plant}_{particle}", **{name: f"{plant}_{name}" for name in textures}, **(extra or {})}
    for age, elements in enumerate(stages):
        used = {"particle": names["particle"]}
        for element in elements:
            for face in element["faces"].values():
                key = face["texture"][1:]
                used[key] = names[key]
        write_model(f"{plant}_stage{age}", built_model(used, elements))


# Cabbage: seed leaves, a rosette of broad blue-green leaves, then a smooth, pale, round head cupped by paler wrapper
# leaves, in big outer leaves curving up round it.

CABBAGE = {"a": "#1e3a30", "b": "#2c5440", "c": "#3e6e52", "d": "#58896a", "e": "#86ae8a", "f": "#b6d48e",
           "g": "#d6ecb2"}


def cabbage():
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
    build("cabbage", CABBAGE, textures, stages, "large_leaf")


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
    build("tomato_vine", TOMATO, textures, stages, "vine", extra={"post": "minecraft:block/stripped_oak_log"})


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


def barley():
    textures = {"shoot": bottom_padded(BARLEY_SHOOT), "leafy": bottom_padded(BARLEY_LEAFY),
                "eared": bottom_padded(BARLEY_EARED),
                "turning": bottom_padded(recolour(BARLEY_EARED, {"f": "k", "e": "f"})),
                "ripe": bottom_padded(recolour(BARLEY_EARED, {"e": "k", "f": "j", "d": "i", "c": "i", "b": "h"}))}

    def clump(texture, size, count, droop=0):
        parts = []
        for i in range(count):
            lean = BARLEY_LEANS[i] + (droop if BARLEY_LEANS[i] > 0 else -droop)
            parts += tuft(texture, size, BARLEY_SPOTS[i], BARLEY_TURNS[i], lean)
        return parts
    stages = [clump("shoot", (5, 6), 3), clump("shoot", (5, 6), 6), clump("leafy", (7, 13), 5), clump("leafy", (7, 13), 6),
              clump("leafy", (7, 13), 8), clump("eared", (7, 16), 8), clump("turning", (7, 16), 8),
              clump("ripe", (7, 16), 8, droop=10)]
    build("barley", BARLEY, textures, stages, "leafy")


# Onion: tall, hollow, blue-green leaves fanning from a pale neck over a round bulb sitting on the soil, the leaves
# yellowing and flopping over when it is ready to lift. The red onion is the same plant in red.

ONION = {"b": "#2e5a3a", "c": "#457a4c", "d": "#5e9660", "e": "#86b47c", "f": "#c8dcb0", "w": "#eef0dc",
         "h": "#8a7a3a", "i": "#b0a050", "j": "#c8b868",
         "m": "#6e3e1a", "n": "#a0622a", "o": "#c88a44", "p": "#e0b070", "q": "#f0d4a0"}
RED_ONION = {**ONION, "m": "#4a1430", "n": "#7a2048", "o": "#9e3460", "p": "#c0587e", "q": "#dc90aa"}


def onion_build(plant, palette):
    side = shaded_side("nnop", 8)
    side = ["".join("n" if x % 4 == 1 and ch in "op" else ch for x, ch in enumerate(row)) for row in side]
    top = ["".join("f" if _m.hypot(x - 7.5, y - 7.5) < 1.5 else "q" if _m.hypot(x - 7.5, y - 7.5) < 2.6 else "p"
                   for x in range(16)) for y in range(16)]
    textures = {"leaf": stalk_strip(["dddcdddcdddcddde", "jijhjijijhjiiihh", "eeiedeiededdcdde"]),
                "neck": padded(["fw", "fw", "fw", "fw", "fw"]), "bulb_side": side, "bulb_top": top}

    def fan(count, length, tilt, ground, column=0, turn=0):
        return [stalk("leaf", length, turn + i * 360 / count, tilt, ground=ground, column=column) for i in range(count)]

    def plant_with_bulb(d, h, neck, leaves):
        top_y = ball_top(d, h, sink=1)
        return (ball(d, h, "bulb_side", "bulb_top", sink=1)
                + [solid((7, top_y, 7), (9, top_y + neck, 9), "neck")] + leaves(top_y + neck))
    stages = [
        fan(2, 5, 8, GROUND), fan(3, 7, 10, GROUND), fan(4, 9, 12, GROUND), fan(5, 11, 12, GROUND),
        plant_with_bulb(4, 4, 2, lambda y: fan(5, 12, 12, y)),
        plant_with_bulb(6, 6, 3, lambda y: fan(6, 13, 13, y)),
        plant_with_bulb(7, 7, 3, lambda y: fan(5, 13, 13, y) + fan(2, 11, 40, y, column=2, turn=45)),
        plant_with_bulb(9, 8, 2, lambda y: fan(5, 10, 76, y, column=1, turn=20)),
    ]
    build(plant, palette, textures, stages, "bulb_side")


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
    small = ["...d...", "d..c..d", ".ccecc.", "ccceccf", ".bcecb.", "..b.b..", "...c..."]
    flower = ["...Y...", ".Y.Y.Y.", "..YXY..", "YYXZXYY", "..YXY..", ".Y.Y.Y.", "...Y..."]
    textures = {"seed_leaf": padded(leaf_grid(4, 4)), "small_leaf": padded(small), "big_leaf": padded(big),
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
    stages = [
        ring_of("seed_leaf", (4, 4), 2, 25),
        ring_of("seed_leaf", (4, 4), 2, 20) + on_stalks("small_leaf", (7, 7), 1, 30, 2, 15, turn=90),
        on_stalks("small_leaf", (7, 7), 3, 30, 3, 20),
        on_stalks("small_leaf", (7, 7), 2, 30, 4, 15, turn=60) + big_leaves(3, 5),
        big_leaves(5, 7),
        big_leaves(6) + flower_at(30, 4, 3) + flower_at(200, 4.5, 2),
        big_leaves(6) + flower_at(30, 4, 3) + flower_at(200, 4.5, 2) + [fruit_at(9, 11, 2, 5, 70)],
        big_leaves(6) + flower_at(250, 4, 3) + [fruit_at(9, 11, 3, 9, 70), fruit_at(6, 5, 2, 7, 150)],
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
        "H": "#78a84a", "I": "#a4c870", "T": "#c0a860", "U": "#dcc890", "k": "#e8c888", "m": "#6a3e1c"}


def corn():
    strap = [".d."] + ["ded"] * 4 + ["cec"] * 5 + ["cdc"] * 4
    husk = ["".join("I" if x % 3 == 0 else "H" for x in range(16)) for _ in range(16)]
    dry = recolour(husk, {"I": "U", "H": "T"})
    textures = {"leaf": padded(strap), "old_leaf": padded(recolour(strap, {"d": "z", "e": "z", "c": "y"})),
                "stalk": stalk_strip(["cdccdcccdccdccdc", "dcdcccdcdcccdcdc"]),
                "tassel": bottom_padded(["i.j.i", ".iji.", "j.i.j", ".hih.", "..h..", "..h.."]),
                "brown_tassel": bottom_padded(recolour(["i.j.i", ".iji.", "j.i.j", ".hih.", "..h..", "..h.."], {"i": "h", "j": "i"})),
                "husk": husk, "silk": padded(["kk.", "kkk", ".kk"]), "dry_husk": dry, "dry_silk": padded(["mm.", "mmm", ".mm"])}

    def plant(height, leaves, length, tassel=None, cobs=None, old=0):
        parts = column(8, 8, GROUND, GROUND + height, "stalk")
        for k in range(leaves):
            y = GROUND + 2 + k * (height - 3) / max(1, leaves)
            name = "old_leaf" if k < old else "leaf"
            parts += arching_leaf(name, 3, k * 180 + k * 23, y, length // 2, length - length // 2)
        if tassel:
            for i, turn in enumerate((0, 60, 120)):
                parts += tuft(tassel, (5, 6), (8, 8), turn, 8 if i % 2 else -8, ground=GROUND + height)
        for heading, y, side, top in cobs or ():
            parts.append(solid((6.5, y, 4), (9.5, y + 7, 7), side, top=top,
                               rotation={"origin": [8, y, 8], "x": -18, "y": heading, "z": 0}))
        return parts
    stages = [
        plant(2, 3, 6), plant(4, 4, 8), plant(8, 5, 10), plant(12, 6, 12), plant(18, 7, 13),
        plant(24, 8, 13, tassel="tassel"),
        plant(24, 8, 13, tassel="tassel", cobs=[(40, GROUND + 9, "husk", "silk"), (220, GROUND + 12, "husk", "silk")]),
        plant(24, 8, 13, tassel="brown_tassel", old=2,
              cobs=[(40, GROUND + 9, "dry_husk", "dry_silk"), (220, GROUND + 12, "dry_husk", "dry_silk")]),
    ]
    build("corn", CORN, textures, stages, "leaf")


# White grape: a cutting, a young vine, then a woody vine trained along the trellis rails with big lobed leaves,
# setting small green bunches that swell and ripen golden green.

GRAPE = {"a": "#1e4a24", "b": "#2c6630", "c": "#40823c", "d": "#5c9e48", "e": "#86c060",
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


def grape():
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
                "ripe": padded(["PQ", "QR"]), "green": padded(["GK", "KL"])}

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
    build("white_grape_vine", GRAPE, textures, stages, "vine", extra={"post": "minecraft:block/stripped_oak_log"})


# Rice: a clump of grassy shoots rising out of standing water, then green heads that droop as they fill and ripen
# gold. The block holds the water, so the plant grows up from its bottom and only shows above the surface.

RICE = {"a": "#24502a", "b": "#346c30", "c": "#4a8a3a", "d": "#68a648", "e": "#92c464", "f": "#c0dc8c",
        "h": "#9a7a30", "i": "#c8a448", "j": "#e4c868"}
RICE_SPOTS = [(8, 8), (6.5, 6.5), (9.5, 6.5), (6.5, 9.5), (9.5, 9.5), (8, 5.5), (5.5, 8), (10.5, 8), (8, 10.5)]
RICE_TURNS = [0, 40, 80, 120, 160, 20, 60, 100, 140]
RICE_LEANS = [0, -10, 9, -8, 10, -6, 7, -9, 8]
RICE_BLADES = ["..e..", ".e.e.", "e.d.d", ".d.d.", "d.cd.", ".dc.d", "..cd.", ".cd..", "..c..", ".cc..", "..c..", "..cc.",
               "..c..", ".cc..", "..c..", "..c.."]
RICE_HEADING = ["..ff.", ".f..f", "....f", "..e.f", ".e.d.", "e.d.d", ".dcd.", "..cd.", ".cd..", "..c..", ".cc..", "..c..",
                "..cc.", "..c..", "..c..", "..c.."]


def rice():
    textures = {"blades": bottom_padded(RICE_BLADES), "heading": bottom_padded(RICE_HEADING),
                "ripe": bottom_padded(recolour(RICE_HEADING, {"f": "j", "e": "i", "d": "d"})),
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
    build("rice", RICE, textures, stages, "blades")


# Rhubarb: red buds breaking from the crown, then huge, crinkled leaves held up on thick red stalks.

RHUBARB = {"a": "#1e4224", "b": "#2c5e30", "c": "#3e7a3a", "d": "#56964a", "e": "#7eb466", "f": "#a8d08c",
           "r": "#6e1424", "s": "#a82638", "t": "#d0485a", "u": "#ec8496"}


def rhubarb():
    def crinkled(w, h):
        grid = [list(row) for row in leaf_grid(w, h)]
        for y in range(h):
            for x in range(w):
                if grid[y][x] in "cd" and (x * 5 + y * 3) % 7 == 0:
                    grid[y][x] = "b"
        return ["".join(r) for r in grid]
    textures = {"small_leaf": padded(crinkled(6, 6)), "leaf": padded(crinkled(8, 7)), "big_leaf": padded(crinkled(10, 9)),
                "stalk": stalk_strip(["tsstssts" * 2, "ssrssrss" * 2]), "bud": padded(["ut", "ts"])}

    def leaves(texture, size, count, rise, lean, turn=0, tilt=20):
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
    stages = [
        buds(3) + leaves("small_leaf", (6, 6), 1, 2, 15, tilt=40),
        buds(2) + leaves("small_leaf", (6, 6), 3, 3, 20, tilt=35),
        leaves("leaf", (8, 7), 4, 5, 22, tilt=30),
        leaves("leaf", (8, 7), 5, 6, 22, turn=30, tilt=25),
        leaves("big_leaf", (10, 9), 5, 7, 22),
        leaves("big_leaf", (10, 9), 6, 8, 22, turn=20),
        leaves("big_leaf", (10, 9), 6, 9, 22, turn=10, tilt=18) + buds(2),
        leaves("big_leaf", (10, 9), 7, 9, 22, turn=25, tilt=18),
    ]
    build("rhubarb", RHUBARB, textures, stages, "big_leaf")


# Dill: feathery blue-green fronds, then a tall stem carrying flat umbrellas of tiny yellow flowers that turn to brown
# seed heads.

DILL = {"a": "#2a4e3a", "b": "#3a6a4c", "c": "#4a7e58", "d": "#5e946a", "e": "#78aa80", "f": "#a8c890",
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
    stages = [
        fronds("small_frond", (5, 6), 2), fronds("small_frond", (5, 6), 4), fronds("frond", (7, 10), 4),
        fronds("frond", (7, 10), 6),
        fronds("frond", (7, 10), 6) + umbel(8, 8, 14, "bud_"),
        fronds("frond", (7, 10), 6) + umbel(8, 8, 17) + umbel(10, 7, 13, "bud_"),
        fronds("frond", (7, 10), 6) + umbel(8, 8, 19) + umbel(10, 7, 15) + umbel(6, 9.5, 12, "bud_"),
        fronds("old_frond", (7, 10), 5) + umbel(8, 8, 19, "seed_") + umbel(10, 7, 15, "seed_") + umbel(6, 9.5, 12, "seed_"),
    ]
    build("dill", DILL, textures, stages, "frond")


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


if __name__ == "__main__":
    main()
