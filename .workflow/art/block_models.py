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


def leaf(texture, size, heading, tilt, out=0.0, rise=0.0):
    """A leaf of size (width, length) pixels growing out from the plant's centre, its stalk `out` pixels from the
    centre and `rise` above the ground, tilted up by `tilt` degrees and turned to face `heading` degrees round the plant.
    The texture is drawn at one texel per pixel, so its pixels match the rest of the plant."""
    import math
    w, length = size
    # Laid flat pointing north from the stalk, then tilted up and turned round the plant's centre.
    # Tilting swings the stalk up round the centre, so start it further out and lower to land it where it should be.
    reach = out / math.cos(math.radians(tilt))
    root_z = 8 - reach
    y = GROUND + rise - reach * math.sin(math.radians(tilt))
    face = {"uv": [0, 0, w, length], "texture": "#" + texture}
    return {"from": [8 - w / 2, y, root_z - length], "to": [8 + w / 2, y, root_z],
            "rotation": {"origin": [8, y, 8], "x": tilt, "y": heading, "z": 0},
            "faces": {"up": face, "down": face}}


def head(size, side="side", top="top"):
    """A firm, rounded heart of (width, height) pixels: a narrower foot, the full body, and narrowing tiers on top,
    each face mapped one texel per pixel from the side and top textures."""
    w, h = size
    # Narrowing by two pixels a tier above the body, as many tiers as the width allows, a pixel high each.
    tops = [w - 2 * k for k in range(1, 4) if w - 2 * k >= 4]
    body = h - 1 - len(tops)
    tiers = [(w - 2, GROUND, GROUND + 1), (w, GROUND + 1, GROUND + 1 + body)]
    for k, tw in enumerate(tops):
        tiers.append((tw, GROUND + 1 + body + k, GROUND + 2 + body + k))
    elements = []
    for tw, y0, y1 in tiers:
        lo, hi = 8 - tw / 2, 8 + tw / 2
        # The side texture's bottom row sits on the ground, so each tier shows the rows at its own height.
        side_face = {"uv": [lo, 16 - (y1 - GROUND), hi, 16 - (y0 - GROUND)], "texture": "#" + side}
        elements.append({"from": [lo, y0, lo], "to": [hi, y1, hi],
                         "faces": {"north": side_face, "south": side_face, "east": side_face, "west": side_face,
                                   "up": {"uv": [lo, lo, hi, hi], "texture": "#" + top}}})
    return elements


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


if __name__ == "__main__":
    main()
