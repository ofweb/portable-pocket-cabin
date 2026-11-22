"""Plant block textures and models, drawn as pixel grids and shown on flat planes like vanilla crops.

Run from the repository root: python3 .workflow/art/block_models.py
Each growth stage is a 16x16 grid of palette letters ('.' is clear), seen from the side: a few flat greens, light on the
upper left, dark below, no outlines, and gaps between the leaves so the plant reads as leaves rather than a blob. The
stage textures go to textures/block/, and models/block/<plant>_stage<age>.json put them on two crossed planes, sunk a
pixel so the plant stands in farmland rather than floating on it; trellis vines lie flat over the trellis.
A plant with a firm heart, like a cabbage head, gets that as a solid block standing in its leaves. The blockstates already point each age at its model.
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


def crop_model(texture, head=None):
    """A field crop: crossed planes sunk a pixel into the farmland, and optionally a solid head standing in them,
    given as (width, height) in pixels with its side and top textures drawn 1:1 so its pixels match the leaves."""
    textures = {"particle": NS + texture, "crop": NS + texture}
    elements = cross_planes("#crop", -1)
    if head:
        (w, h), (side, top) = head
        lo, hi = 8 - w / 2, 8 + w / 2
        textures.update(side=NS + side, top=NS + top)
        side_face = {"uv": [lo, 16 - h, hi, 16], "texture": "#side"}
        top_face = {"uv": [lo, lo, hi, hi], "texture": "#top"}
        elements.append({"from": [lo, -1, lo], "to": [hi, h - 2, hi],
                         "faces": {"north": side_face, "south": side_face, "east": side_face, "west": side_face, "up": top_face}})
        # A narrower cap a pixel high, so the head domes rather than ending flat.
        cap_side = {"uv": [lo + 1, 16 - h, hi - 1, 17 - h], "texture": "#side"}
        cap_top = {"uv": [lo + 1, lo + 1, hi - 1, hi - 1], "texture": "#top"}
        elements.append({"from": [lo + 1, h - 2, lo + 1], "to": [hi - 1, h - 1, hi - 1],
                         "faces": {"north": cap_side, "south": cap_side, "east": cap_side, "west": cap_side, "up": cap_top}})
    return {"parent": "minecraft:block/block", "ambientocclusion": False, "textures": textures, "elements": elements}


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


# Cabbage: two seed leaves, a low rosette, a cupped plant with a pale heart, then a round, pale head in its outer leaves.

CABBAGE = {"a": "#1e3f2a", "b": "#2a5634", "c": "#3c7042", "d": "#548c4c", "e": "#7cb05e", "f": "#a8d27e", "g": "#cfeaa6",
           "r": "#4a3418", "s": "#6e5226"}

CABBAGE_STAGES = {
    "sprout": [
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
        "................",
        "....ee....ee....",
        "...edcd..dced...",
        "....cbbddbbc....",
        ".......sr.......",
    ],
    "rosette": [
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
        "...ee.dce.de....",
        "..edcbdcdbdce...",
        "..dccbcecbccdb..",
        "...bbcbccbcbb...",
        "......rssr......",
    ],
    # The outer leaves round a head; the head itself is a solid block standing in them.
    "leaves_small": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "..ee........ee..",
        ".edcd......dced.",
        "..dccb....bccd..",
        "..bccdd..ddccb..",
        "...bbccddccbb...",
        "....abbccbba....",
        "......rssr......",
    ],
    "leaves_large": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        ".ee..........ee.",
        "edce........ecde",
        "dcdde......eddcd",
        "dccde......edccd",
        ".bccde....edccb.",
        ".bbccdd..ddccbb.",
        "..bbccdeedccbb..",
        "..abbccddccbba..",
        "...aabbccbbaa...",
        "......rssr......",
    ],
    # A firm, pale head of tightly wrapped leaves, the leaf edges running round it.
    "head_side": [
        "gggggfggggggfggg",
        "ggggfefgggggefgg",
        "gggfeeffggggfeef",
        "ggfefgfefggfefgf",
        "gfeffggfeffefggf",
        "fefgggggfeefgggf",
        "effggggggfefgggf",
        "efggggggfeefggfe",
        "efgggggfefffgfee",
        "effgggfefgfeffef",
        "defgffefggfefeed",
        "defffefggffeffed",
        "deeffeffffefeeed",
        "ddeeeeeeeeeeeedd",
        "cddeeddeeddeeddc",
        "ccddcddddddcddcc",
    ],
    "head_top": None,  # drawn by spiral_top() below
}


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


CABBAGE_STAGES["head_top"] = spiral_top()

# Each age's leaves, and the head standing in them as (width, height) in pixels.
CABBAGE_BY_AGE = [("sprout", None), ("sprout", None), ("rosette", None), ("rosette", None),
                  ("leaves_small", (4, 4)), ("leaves_small", (6, 5)), ("leaves_large", (7, 6)), ("leaves_large", (8, 8))]



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
    for stage, grid in CABBAGE_STAGES.items():
        draw(f"cabbage_{stage}", grid, CABBAGE)
    for age, (stage, head) in enumerate(CABBAGE_BY_AGE):
        write_model(f"cabbage_stage{age}", crop_model(f"cabbage_{stage}", head and (head, ("cabbage_head_side", "cabbage_head_top"))))

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
