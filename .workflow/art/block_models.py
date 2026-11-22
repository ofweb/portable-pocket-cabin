"""Plant block textures and models, drawn as pixel grids and shown on flat planes like vanilla crops.

Run from the repository root: python3 .workflow/art/block_models.py
Each growth stage is a 16x16 grid of palette letters ('.' is clear), seen from the side: a few flat greens, light on the
upper left, dark below, no outlines, and gaps between the leaves so the plant reads as leaves rather than a blob. The
stage textures go to textures/block/, and models/block/<plant>_stage<age>.json put them on two crossed planes, sunk a
pixel so the plant stands in farmland rather than floating on it; trellis vines lie flat over the trellis.
Plants that need more than that are built in 3D: leaves as flat planes tilted out from the stem at any angle, and a
solid heart such as a cabbage head, all mapped one texel per pixel so they match. The blockstates already point each
age at its model.
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


def cross_planes(texture, bottom):
    rot = {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True}
    face = {"uv": [0, 0, 16, 16], "texture": texture}
    return [
        {"from": [0.8, bottom, 8], "to": [15.2, bottom + 16, 8], "rotation": rot, "shade": False, "faces": {"north": face, "south": face}},
        {"from": [8, bottom, 0.8], "to": [8, bottom + 16, 15.2], "rotation": rot, "shade": False, "faces": {"east": face, "west": face}},
    ]


def crop_model(texture):
    """A field crop: crossed planes sunk a pixel into the farmland."""
    return {"parent": "minecraft:block/block", "ambientocclusion": False,
            "textures": {"particle": NS + texture, "crop": NS + texture}, "elements": cross_planes("#crop", -1)}


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


def leaf(texture, size, heading, tilt, out=0.0, rise=0.0, ground=GROUND):
    """A leaf of size (width, length) pixels growing out from the plant's centre, its stalk `out` pixels from the
    centre and `rise` above the ground, tilted up by `tilt` degrees and turned to face `heading` degrees round the plant.
    The texture is drawn at one texel per pixel, so its pixels match the rest of the plant."""
    import math
    w, length = size
    # Laid flat pointing north from the stalk, then tilted up and turned round the plant's centre.
    # Tilting swings the stalk up round the centre, so start it further out and lower to land it where it should be.
    reach = out / math.cos(math.radians(tilt))
    root_z = 8 - reach
    y = ground + rise - reach * math.sin(math.radians(tilt))
    face = {"uv": [0, 0, w, length], "texture": "#" + texture}
    return {"from": [8 - w / 2, y, root_z - length], "to": [8 + w / 2, y, root_z],
            "rotation": {"origin": [8, y, 8], "x": tilt, "y": heading, "z": 0},
            "faces": {"up": face, "down": face}}


def head(size, side="side", top="top", ground=GROUND):
    """A firm, rounded heart of (width, height) pixels: a narrower foot, the full body, and narrowing tiers on top,
    each face mapped one texel per pixel from the side and top textures."""
    w, h = size
    # Narrowing by two pixels a tier above the body, as many tiers as the width allows, a pixel high each.
    tops = [w - 2 * k for k in range(1, 4) if w - 2 * k >= 4]
    body = h - 1 - len(tops)
    tiers = [(w - 2, ground, ground + 1), (w, ground + 1, ground + 1 + body)]
    for k, tw in enumerate(tops):
        tiers.append((tw, ground + 1 + body + k, ground + 2 + body + k))
    elements = []
    for tw, y0, y1 in tiers:
        lo, hi = 8 - tw / 2, 8 + tw / 2
        # The side texture's bottom row sits on the ground, so each tier shows the rows at its own height.
        side_face = {"uv": [lo, 16 - (y1 - ground), hi, 16 - (y0 - ground)], "texture": "#" + side}
        elements.append({"from": [lo, y0, lo], "to": [hi, y1, hi],
                         "faces": {"north": side_face, "south": side_face, "east": side_face, "west": side_face,
                                   "up": {"uv": [lo, lo, hi, hi], "texture": "#" + top}}})
    return elements


def stalk(texture, length, heading, tilt, out=0.0, ground=GROUND, column=0):
    """A thin stalk or tube a pixel thick, rising `length` pixels from `out` pixels north of the centre, leaning out by
    `tilt` degrees and turned to `heading`. Its sides show one column of the texture, bottom row at the ground."""
    side = {"uv": [column, 16 - length, column + 1, 16], "texture": "#" + texture}
    tip = {"uv": [column, 16 - length, column + 1, 17 - length], "texture": "#" + texture}
    return {"from": [7.5, ground, 7.5 - out], "to": [8.5, ground + length, 8.5 - out],
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


def trellis_model(texture):
    """A trellis vine: the wooden frame with the plant growing flat over both its faces."""
    frame = [{**part, "faces": {side: {"texture": "#post"} for side in ("north", "south", "east", "west", "up", "down")}} for part in TRELLIS_FRAME]
    return {"parent": "minecraft:block/block", "ambientocclusion": False,
            "textures": {"particle": NS + texture, "plant": NS + texture, "post": "minecraft:block/stripped_oak_log"},
            "elements": frame + trellis_planes("#plant")}


# Cabbage: two seed leaves, a rosette of broad leaves, leaves cupping a small heart, then a firm, domed head in a ring
# of big outer leaves spread round its foot.

CABBAGE = {"a": "#1e3f2a", "b": "#2a5634", "c": "#3c7042", "d": "#548c4c", "e": "#7cb05e", "f": "#a8d27e", "g": "#cfeaa6"}
CABBAGE_LEAVES = {"seed_leaf": (4, 4), "small_leaf": (6, 7), "large_leaf": (9, 9), "wrapper_leaf": (8, 8)}
# The wrapper leaves hugging the head are paler than the outer leaves, halfway to the head's colour.
PALER = {"wrapper_leaf": 2}

CABBAGE_HEAD_SIDE = [
    "gggggfggggggfggg",
    "ggggfffgggggffgg",
    "gggfffffggggffff",
    "ggfffgfffggfffgf",
    "gffffggffffffggf",
    "fffgggggffffgggf",
    "fffggggggfffgggf",
    "ffggggggffffggff",
    "ffgggggfffffgfff",
    "fffgggfffgffffff",
    "defgffefggfefeed",
    "defffefggffeffed",
    "deeffeffffefeeed",
    "ddeeeeeeeeeeeedd",
    "cddeeddeeddeeddc",
    "ccddcddddddcddcc",
]


def spiral_top():
    """The head from above: pale leaves wrapped in a spiral, each leaf's edge a darker curve sweeping out from the heart."""
    import math
    grid = [["f"] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            # Lighter toward the upper left, where the light falls; the rim rolls over into shade.
            r = math.hypot(x - 7.5, y - 7.5)
            grid[y][x] = "g" if (x + y < 15 and r < 6.5) else "f" if r < 7.2 else "e"
    for arm in range(3):
        for step in range(30):
            t = step / 30 * 1.5
            angle = arm * math.pi * 2 / 3 + t * 1.5
            r = 1.0 + t * 3.6
            x, y = round(7.5 + r * math.cos(angle)), round(7.5 + r * math.sin(angle))
            if 0 <= x < 16 and 0 <= y < 16:
                grid[y][x] = "e"
                # A pale lip on the leaf edge's sunny side.
                lx, ly = x - 1, y - 1
                if 0 <= lx < 16 and 0 <= ly < 16 and grid[ly][lx] in "fg":
                    grid[ly][lx] = "g"
    return ["".join(row) for row in grid]


def ring(texture, count, tilt, out=0.0, rise=0.0, turn=0.0):
    """Leaves of one kind evenly round the plant."""
    return [leaf(texture, CABBAGE_LEAVES[texture], turn + i * 360 / count, tilt, out, rise) for i in range(count)]


CABBAGE_BY_AGE = [
    # Two seed leaves, then two more as the first true leaves come.
    ring("seed_leaf", 2, 35, rise=1),
    ring("seed_leaf", 2, 30, rise=1) + ring("small_leaf", 2, 50, turn=90),
    # A rosette of broad leaves, rising as it grows.
    ring("small_leaf", 5, 30),
    ring("small_leaf", 3, 30, turn=60) + ring("large_leaf", 3, 25),
    # Leaves cupping a small heart, with the outer leaves spreading.
    head((4, 4)) + ring("small_leaf", 4, 55, out=1.5) + ring("large_leaf", 4, 25, out=1, turn=45),
    head((6, 6)) + ring("small_leaf", 4, 60, out=2.5) + ring("large_leaf", 5, 22, out=1.5, turn=36),
    # A firm head, its wrapper leaves rising from its foot and hugging it, in a ring of big outer leaves.
    head((8, 7)) + ring("wrapper_leaf", 4, 58, out=3.0, turn=45) + ring("large_leaf", 5, 20, out=2.5),
    head((10, 9)) + ring("wrapper_leaf", 5, 60, out=4.0, turn=20) + ring("large_leaf", 6, 18, out=3.5),
]


# Tomato: a seedling, a young plant, then a vine up the trellis, flowering with green fruit, then hung with red fruit.

TOMATO = {"a": "#1c4424", "b": "#2c5e30", "c": "#3f7c3a", "d": "#5c9a44", "e": "#8cc05a",
          "R": "#6e1a2a", "S": "#a82634", "T": "#d8442e", "U": "#ff9a80",
          "G": "#4c7a26", "H": "#7cae3c", "I": "#b8dc78", "Y": "#e8c234", "Z": "#b07a1c", "k": "#2c5e30"}

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
TOMATO_FRUIT = ["..k..", ".TUT.", ".STS.", "..R.."]
GREEN_FRUIT = ["..k..", ".HIH.", ".GHG.", "..G.."]
FLOWER = ["Y.Y", ".Z.", "Y.Y"]
FRUIT_SPOTS = [(7, 3), (1, 7), (9, 10), (2, 11), (11, 4)]
FLOWER_SPOTS = [(4, 4), (12, 8), (0, 13), (13, 0)]


# More plants for evaluating the style across kinds: a grain, a bulb, a root, a sprawling fruiting plant, and a bush.

import math as _m

STEP_LIGHTER = {"a": "b", "b": "c", "c": "d", "d": "e", "e": "f", "f": "g", "g": "g"}


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


def stalk_strip(tones):
    """A texture whose first columns are stalks or tubes, read bottom-up by stalk(): one column per entry."""
    rows = []
    for y in range(16):
        rows.append("".join(column[y] for column in tones).ljust(16, "."))
    return rows


def build(plant, palette, textures, stages, particle):
    """Draws a plant's textures and writes one model per growth stage."""
    for name, grid in textures.items():
        draw(f"{plant}_{name}", grid, palette)
    names = {"particle": f"{plant}_{particle}", **{name: f"{plant}_{name}" for name in textures}}
    for age, elements in enumerate(stages):
        used = {"particle": names["particle"]}
        for element in elements:
            for face in element["faces"].values():
                key = face["texture"][1:]
                used[key] = names[key]
        write_model(f"{plant}_stage{age}", built_model(used, elements))


# Barley: a tuft of grass-like shoots, leafy stems, then bearded green ears that ripen gold and nod.

BARLEY = {"a": "#26401e", "b": "#355824", "c": "#4a7030", "d": "#6a9440", "e": "#8cb458", "f": "#b0cc78",
          "h": "#8a6a24", "i": "#b8923a", "j": "#d8b85a", "k": "#ecd88a"}
BARLEY_SHOOT = ["..e..", ".ed.e", "e.d.d", "d.cdc", ".dcc.", "..c.."]
BARLEY_LEAFY = ["....e.", "...ed.", "...d..", "e..d..", "de.c..", ".dcc.e", "..c.ed", "..cdd.", "e.c...", "dcc...",
                "..cd..", "..c..."]
# A slim ear of kernels in two rows with a few long beard hairs, on a stem with two leaves.
BARLEY_EARED = ["e.e.e", ".eee.", ".dfd.", ".fdf.", ".dfd.", ".fdf.", "..d..", "..c..", "..c.e", "..cd.", "e.c..",
                ".dc..", "..c..", "..cd.", "..c.e", "..c.."]
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
    stages = [clump("shoot", (5, 6), 3), clump("shoot", (5, 6), 6), clump("shoot", (5, 6), 8), clump("leafy", (6, 12), 5),
              clump("leafy", (6, 12), 8), clump("eared", (5, 16), 8), clump("turning", (5, 16), 8),
              clump("ripe", (5, 16), 8, droop=5)]
    build("barley", BARLEY, textures, stages, "leafy")


# Onion: hollow blue-green leaves fanning from the ground, a golden bulb swelling at the surface, then the leaves
# yellowing and flopping over when it is ready to lift.

ONION = {"a": "#1e3f2a", "b": "#2e5a3a", "c": "#457a4c", "d": "#5e9660", "e": "#86b47c", "f": "#b8d4a0",
         "h": "#8a7a3a", "i": "#b0a050", "j": "#c8b868",
         "m": "#6e3e1a", "n": "#a0622a", "o": "#c88a44", "p": "#e0b070", "q": "#f0d4a0"}


def onion_textures():
    green = "dddcdddcddddcfff"   # top to bottom: the pale neck where the leaf leaves the bulb is at the bottom
    dry = "jijhjijijhjiiihh"
    yellowing = "eeiedeiededdcfff"
    side = []
    for y in range(16):
        row = ""
        for x in range(16):
            ch = "o"
            if x % 4 == 0:
                ch = "n"
            if x in (3, 5) and y < 14:
                ch = "p"
            if x == 4 and y < 14:
                ch = "q"
            if y == 15:
                ch = "m" if x % 2 else "n"
            row += ch
        side.append(row)
    top = []
    for y in range(16):
        row = ""
        for x in range(16):
            r = _m.hypot(x - 7.5, y - 7.5)
            theta = _m.atan2(y - 7.5, x - 7.5)
            ch = "c" if r < 1.2 else "d" if r < 2.0 else ("n" if int((theta + _m.pi) / (2 * _m.pi) * 10) % 2 == 0 and r > 3 else "o")
            if r < 5 and x + y < 13 and ch == "o":
                ch = "p"
            row += ch
        top.append(row)
    return {"leaf": stalk_strip([green, dry, yellowing]), "bulb_side": side, "bulb_top": top}


def onion():
    def fan(count, length, tilt, column=0, ground=GROUND, turn=0):
        return [stalk("leaf", length, turn + i * 360 / count, tilt, ground=ground, column=column) for i in range(count)]

    def bulb(w, h):
        return head((w, h), side="bulb_side", top="bulb_top")
    stages = [
        fan(2, 4, 10), fan(3, 6, 12), fan(4, 8, 14), fan(5, 10, 15),
        bulb(4, 3) + fan(6, 12, 15, ground=GROUND + 2),
        bulb(6, 4) + fan(6, 13, 16, ground=GROUND + 3),
        bulb(7, 5) + fan(4, 13, 16, ground=GROUND + 4) + fan(2, 11, 45, column=2, ground=GROUND + 4, turn=45),
        bulb(8, 5) + fan(5, 11, 76, column=1, ground=GROUND + 4, turn=20),
    ]
    build("onion", ONION, onion_textures(), stages, "bulb_side")


# Radish: two seed leaves, then a rosette of rough, lobed leaves, the root's red shoulder pushing up out of the soil.

RADISH = {"a": "#1e4424", "b": "#2c6030", "c": "#3f803a", "d": "#5ca046", "e": "#86c060", "f": "#b4dc8c",
          "r": "#7a1430", "s": "#b8243e", "t": "#e04a5a", "u": "#f08a90"}


def radish_textures():
    side = ["".join("r" if y == 15 else "u" if x in (3, 4) and y < 13 else "t" if x % 5 == 1 else "s" for x in range(16))
            for y in range(16)]
    top = []
    for y in range(16):
        row = ""
        for x in range(16):
            r = _m.hypot(x - 7.5, y - 7.5)
            row += "c" if r < 1.3 else "d" if r < 2.1 else "u" if (r < 4.5 and x + y < 13) else "t" if r < 5.5 else "s"
        top.append(row)
    # A rough leaf with a big rounded end lobe and pairs of small lobes down its stalk, the midrib pale.
    leaf = ["..dd..", ".deed.", "ddeedd", "dcdecd", "ccdecc", ".ccdc.", "dc.d.c", "bcbcbc", ".b.c.b", "cb.c.b", "...c.."]
    small = [".dd.", "deed", "cdec", ".cd.", "bcdb", ".bc.", "..c."]
    return {"seed_leaf": padded(leaf_grid(4, 4)), "small_leaf": padded(small),
            "leaf": padded(leaf), "root_side": side, "root_top": top}


def radish():
    def root(w, h):
        return head((w, h), side="root_side", top="root_top")
    stages = [
        ring_of("seed_leaf", (4, 4), 2, 30),
        ring_of("seed_leaf", (4, 4), 2, 25) + ring_of("small_leaf", (4, 7), 2, 50, turn=90),
        ring_of("small_leaf", (4, 7), 4, 45, turn=20),
        ring_of("small_leaf", (4, 7), 3, 45) + ring_of("leaf", (6, 11), 3, 50, turn=60),
        root(3, 2) + ring_of("leaf", (6, 11), 5, 50, out=1),
        root(4, 3) + ring_of("leaf", (6, 11), 6, 52, out=1.5, turn=15),
        root(5, 3) + ring_of("leaf", (6, 11), 6, 50, out=2, turn=30),
        root(6, 4) + ring_of("leaf", (6, 11), 7, 48, out=2.5, turn=10),
    ]
    build("radish", RADISH, radish_textures(), stages, "leaf")


# Courgette: seed leaves, then big, five-lobed, silver-veined leaves held up on stalks, yellow flowers, and glossy
# dark fruit lying on the ground beneath them.

COURGETTE = {"a": "#1a3e22", "b": "#28582c", "c": "#3a743a", "d": "#52904a", "e": "#86b46a", "f": "#b8d4a4",
             "g": "#d8ead0", "Y": "#f0c020", "Z": "#d08818", "X": "#fff0a0",
             "F": "#1a3c1a", "G": "#2a5a28", "H": "#467e3a", "I": "#8ab86a"}


def courgette_textures():
    fruit = ["".join("I" if (x * 7 + y * 13) % 11 == 0 else "H" if (x * 3 + y * 5) % 7 == 0 else "G" if (x + y) % 3 else "F"
                     for x in range(16)) for y in range(16)]
    flower = padded([".Y.Y.", "YXZXY", ".ZZZ.", "YXZXY", ".Y.Y."])
    bud = padded(["ZY", "YZ"])
    # A broad leaf with five shallow lobes, its veins pale and silver-mottled near the heart.
    big = ["...ded...", ".dddcddd.", "dcccecccd", "bcecfcecb", "bccefeccb", ".bcceccb.", "dbccdccbd", ".bbc.cbb.",
           "....c...."]
    small = ["..ded..", "ddcecdd", "bcefecb", ".ccecc.", "dbcdcbd", ".b.c.b.", "...c..."]
    return {"seed_leaf": padded(leaf_grid(4, 4)), "small_leaf": padded(small),
            "big_leaf": padded(big), "stalk": stalk_strip(["cdccdcccdccdccdc"]),
            "fruit": fruit, "flower": flower, "bud": bud}


def courgette():
    def flower(x, z, y):
        return [solid((x - 1, GROUND, z - 1), (x + 1, GROUND + y, z + 1), "bud"),
                {"from": [x - 2.5, GROUND + y, z - 2.5], "to": [x + 2.5, GROUND + y, z + 2.5], "shade": False,
                 "faces": {"up": {"uv": [0, 0, 5, 5], "texture": "#flower"}, "down": {"uv": [0, 0, 5, 5], "texture": "#flower"}}}]

    def fruit(x, z, width, length, heading):
        return solid((x - width / 2, GROUND, z - length / 2), (x + width / 2, GROUND + width, z + length / 2), "fruit",
                     rotation={"origin": [x, GROUND, z], "x": 0, "y": heading, "z": 0})
    big = lambda n, turn=0: on_stalks("big_leaf", (9, 9), n, 25, 4, 28, turn)
    stages = [
        ring_of("seed_leaf", (4, 4), 2, 25),
        ring_of("seed_leaf", (4, 4), 2, 20) + on_stalks("small_leaf", (7, 7), 1, 30, 2, 15, turn=90),
        on_stalks("small_leaf", (7, 7), 3, 30, 2, 20),
        on_stalks("small_leaf", (7, 7), 2, 35, 3, 15, turn=60) + on_stalks("big_leaf", (9, 9), 3, 25, 3, 28),
        big(5),
        big(6) + flower(5, 9, 3) + flower(11, 6, 2),
        big(6) + flower(5, 9, 3) + flower(11, 6, 2) + [fruit(9, 11, 2, 5, 70)],
        big(6) + flower(4, 6, 3) + [fruit(9, 11, 3, 9, 70), fruit(6, 5, 2, 7, 150)],
    ]
    build("courgette", COURGETTE, courgette_textures(), stages, "big_leaf")


# Strawberry: a low bush of three-part, toothed leaves on stalks, white flowers, then red berries hanging at its edge.

STRAWBERRY = {"a": "#1c3e20", "b": "#2a5a2c", "c": "#3c7838", "d": "#56964a", "e": "#80b864", "f": "#aad488",
              "R": "#a01828", "S": "#d82a36", "T": "#f05a5a", "Y": "#f0d060", "W": "#f8f8f0", "V": "#d8d8c8", "X": "#f0c830"}
GRASS = 0  # bushes stand on grass, a full block, not on farmland


def strawberry():
    # Three toothed leaflets, the middle one forward, each with a pale midrib.
    textures = {"leaf": padded(["...ded...", "..dcecd..", ".ddcecdd.", "dedcecded", "ecdcbcdce", "dcbc.cbcd", ".dc...cd.",
                                "....b...."]),
                "small_leaf": padded([".ded.", "edcde", "dcbcd", "..b..", "..b.."]),
                "stalk": stalk_strip(["cccdcccdcccdcccd"]),
                "berry": padded(["TS", "SY", "SS", "RS"]), "calyx": padded(["cd", "dc"]),
                "flower": padded([".W.W.", "WWVWW", ".VXV.", "WWVWW", ".W.W."])}

    def berry(heading, out):
        x, z = 8 + out * _m.sin(_m.radians(heading)), 8 - out * _m.cos(_m.radians(heading))
        return solid((x - 1, GRASS, z - 1), (x + 1, GRASS + 3, z + 1), "berry", top="calyx")

    def flower(heading, out, y):
        x, z = 8 + out * _m.sin(_m.radians(heading)), 8 - out * _m.cos(_m.radians(heading))
        face = {"uv": [0, 0, 5, 5], "texture": "#flower"}
        return {"from": [x - 2.5, GRASS + y, z - 2.5], "to": [x + 2.5, GRASS + y, z + 2.5], "shade": False,
                "faces": {"up": face, "down": face}}
    def leaves(inner, outer):
        # An inner tier held high and upright, and an outer tier lower and spreading.
        return (on_stalks("leaf", (9, 8), inner, 40, 5, 12, turn=30, ground=GRASS)
                + on_stalks("leaf", (9, 8), outer, 20, 3, 35, ground=GRASS))
    stages = [
        on_stalks("small_leaf", (5, 5), 3, 30, 2, 20, ground=GRASS),
        leaves(2, 3),
        leaves(3, 5) + [flower(20, 7, 4), flower(150, 6.5, 3), flower(260, 7, 4)],
        leaves(3, 5) + [berry(40, 6.5), berry(120, 7), berry(210, 6.5), berry(300, 7), flower(170, 6, 4)],
    ]
    build("strawberry_bush", STRAWBERRY, textures, stages, "leaf")


def main():
    step = {"a": "b", "b": "c", "c": "d", "d": "e", "e": "f", "f": "g", "g": "g", ".": "."}
    for name, size in CABBAGE_LEAVES.items():
        grid = leaf_grid(*size)
        for _ in range(PALER.get(name, 0)):
            grid = ["".join(step[ch] for ch in row) for row in grid]
        draw(f"cabbage_{name}", [row.ljust(16, ".") for row in grid] + ["." * 16] * (16 - size[1]), CABBAGE)
    draw("cabbage_head_side", CABBAGE_HEAD_SIDE, CABBAGE)
    draw("cabbage_head_top", spiral_top(), CABBAGE)
    textures = {"particle": "cabbage_large_leaf", **{name: f"cabbage_{name}" for name in CABBAGE_LEAVES},
                "side": "cabbage_head_side", "top": "cabbage_head_top"}
    for age, elements in enumerate(CABBAGE_BY_AGE):
        write_model(f"cabbage_stage{age}", built_model(textures, elements))

    for stage, grid in TOMATO_STAGES.items():
        draw(f"tomato_vine_{stage}", grid, TOMATO)
    vine = TOMATO_STAGES["vine"]
    draw("tomato_vine_flowering", vine, TOMATO,
         [(FLOWER, x, y) for x, y in FLOWER_SPOTS] + [(GREEN_FRUIT, x, y) for x, y in FRUIT_SPOTS[:3]])
    draw("tomato_vine_ripe", vine, TOMATO, [(TOMATO_FRUIT, x, y) for x, y in FRUIT_SPOTS])
    by_age = ["seedling", "seedling", "young", "young", "vine", "vine", "flowering", "ripe"]
    for age, stage in enumerate(by_age):
        write_model(f"tomato_vine_stage{age}", trellis_model(f"tomato_vine_{stage}"))

    barley()
    onion()
    radish()
    courgette()
    strawberry()


if __name__ == "__main__":
    main()
