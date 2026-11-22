"""Code-drawn plant block models: 3D models built from elements, and the textures they use.

Run from the repository root: python3 .workflow/art/block_models.py
Each plant's growth stages are drawn as a model per stage, written to models/block/<plant>_stage<age>.json over the
borrowed vanilla models, with textures in textures/block/. The blockstates already point each age at its stage model.
Drawing helpers (Sprite, ramp, and the tones) come from item_sprites.py, so blocks and items share one style.
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from item_sprites import BASE, HIGHLIGHT, LIGHT, OUTLINE, SHADOW, Sprite, plain_ramp, ramp  # noqa: E402

A = "src/main/resources/assets/portable_pocket_cabin"
NS = "portable_pocket_cabin:block/"
ALL_FACES = ("north", "south", "east", "west", "up", "down")


def write_texture(name, sprite):
    path = f"{A}/textures/block/{name}.png"
    os.makedirs(os.path.dirname(path), exist_ok=True)
    sprite.image().save(path)


def write_model(name, textures, elements, particle):
    model = {"parent": "minecraft:block/block", "ambientocclusion": False,
             "textures": {"particle": NS + particle, **{k: (v if ":" in v else NS + v) for k, v in textures.items()}},
             "elements": elements}
    with open(f"{A}/models/block/{name}.json", "w") as f:
        json.dump(model, f, indent=2)
        f.write("\n")


def box(start, end, texture, faces=ALL_FACES, uv=None, rotation=None, shade=True):
    """A solid box from start to end, the given faces showing a texture (one per face when a dict)."""
    element = {"from": list(start), "to": list(end), "faces": {}}
    for face in faces:
        tex = texture[face] if isinstance(texture, dict) else texture
        element["faces"][face] = {"texture": "#" + tex, **({"uv": uv} if uv else {})}
    if rotation:
        element["rotation"] = rotation
    if not shade:
        element["shade"] = False
    return element


def plane_x(x0, x1, y0, y1, z, texture, uv=(0, 0, 16, 16)):
    """A flat, upright plane across x at depth z, showing the texture on both sides."""
    return {"from": [x0, y0, z], "to": [x1, y1, z], "shade": False,
            "faces": {"north": {"uv": list(uv), "texture": "#" + texture}, "south": {"uv": list(uv), "texture": "#" + texture}}}


def cross(texture, height=16, inset=0.8):
    """Two planes crossed diagonally, like a sapling or a young crop."""
    rot = {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True}
    return [
        {"from": [inset, 0, 8], "to": [16 - inset, height, 8], "rotation": rot, "shade": False,
         "faces": {"north": {"uv": [0, 16 - height, 16, 16], "texture": "#" + texture},
                   "south": {"uv": [0, 16 - height, 16, 16], "texture": "#" + texture}}},
        {"from": [8, 0, inset], "to": [8, height, 16 - inset], "rotation": rot, "shade": False,
         "faces": {"east": {"uv": [0, 16 - height, 16, 16], "texture": "#" + texture},
                   "west": {"uv": [0, 16 - height, 16, 16], "texture": "#" + texture}}},
    ]


def leaf(direction, length, width, lift, angle, texture, centre=(8, 8)):
    """A flat leaf lying out from the plant's centre in a direction, tilted up by an angle (a multiple of 22.5)."""
    cx, cz = centre
    half = width / 2
    if direction == "north":
        start, end, axis, sign = [cx - half, lift, cz - length], [cx + half, lift, cz], "x", 1
    elif direction == "south":
        start, end, axis, sign = [cx - half, lift, cz], [cx + half, lift, cz + length], "x", -1
    elif direction == "west":
        start, end, axis, sign = [cx - length, lift, cz - half], [cx, lift, cz + half], "z", -1
    else:
        start, end, axis, sign = [cx, lift, cz - half], [cx + length, lift, cz + half], "z", 1
    return {"from": start, "to": end, "shade": False,
            "rotation": {"origin": [cx, lift, cz], "axis": axis, "angle": sign * angle},
            "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#" + texture},
                      "down": {"uv": [0, 0, 16, 16], "texture": "#" + texture}}}


# Cabbage: a sprout, a rosette of broad leaves, a young head, and a full, firm head cupped in its outer leaves.

def cabbage_textures():
    sprout = Sprite()
    stem, seedleaf = ramp("#6a9a48"), ramp("#7ab05a", spread=0.6)
    sprout.line(stem[BASE], 8, 15, 8, 11)
    sprout.ellipse(seedleaf, 5.4, 10.6, 2.6, 1.6, angle=-20)
    sprout.ellipse(seedleaf, 10.6, 10.6, 2.6, 1.6, angle=20)
    write_texture("cabbage_sprout", sprout)

    # One broad, round, blue-green outer leaf seen from above, its pale midrib running up from the stem end.
    blade = Sprite()
    tones = ramp("#6a9a6a", spread=0.6)
    blade.shape(lambda x, y: ((x - 7.5) / 7.2) ** 2 + ((y - 7.0) / 7.4) ** 2 + 0.06 * math.sin(x * 1.7) <= 1, tones, (7.5, 7.0), (7.2, 7.4))
    vein = plain_ramp("#c8dcb8")
    blade.line(vein[BASE], 8, 15, 8, 2)
    for y in (5, 8, 11):
        blade.line(vein[SHADOW], 8, y, 4, y - 3)
        blade.line(vein[SHADOW], 8, y, 12, y - 3)
    write_texture("cabbage_leaf", blade)

    # The head: tight, pale green leaves wrapped over one another.
    side = Sprite()
    head = ramp("#a8cc88", spread=0.5)
    side.shape(lambda x, y: True, head, (7.5, 7.5), (8.0, 8.0))
    for k, y0 in enumerate((3, 8, 13)):
        for x in range(16):
            y = y0 + round(math.sin((x + k * 5) / 16 * math.pi * 2) * 1.6)
            side.put(x, y, head[SHADOW])
            side.put(x, y - 1, head[LIGHT])
    write_texture("cabbage_head_side", side)

    top = Sprite()
    top.shape(lambda x, y: True, head, (7.5, 7.5), (8.0, 8.0))
    for r in (2.4, 4.6, 6.8):
        for a in range(0, 360, 12):
            x, y = round(7.5 + r * math.cos(math.radians(a + r * 30))), round(7.5 + r * math.sin(math.radians(a + r * 30)))
            top.put(x, y, head[SHADOW])
    top.pixels_at(head[HIGHLIGHT], [(6, 6), (7, 6), (6, 7)])
    write_texture("cabbage_head_top", top)


def OUTER_CUP(r):
    """Leaves standing up round a head of half-width r, each rooted at the head's foot on its side."""
    return [("north", (8, 8 - r)), ("south", (8, 8 + r)), ("west", (8 - r, 8)), ("east", (8 + r, 8))]


def cabbage_models():
    head_faces = {"north": "head_side", "south": "head_side", "east": "head_side", "west": "head_side", "up": "head_top", "down": "head_top"}
    textures = {"sprout": "cabbage_sprout", "leaf": "cabbage_leaf", "head_side": "cabbage_head_side", "head_top": "cabbage_head_top"}
    stages = {
        # A seedling: two round seed leaves on a short stem.
        "sprout": cross("sprout", height=8),
        # A low rosette of four broad leaves.
        "rosette": [leaf(d, 6.5, 6.0, 0.6, 22.5, "leaf") for d in ("north", "south", "east", "west")],
        # A small head forming, cupped by leaves standing up round it.
        "young": [box((6, 0, 6), (10, 4, 10), head_faces, uv=[4, 4, 12, 12]),
                  box((6.75, 4, 6.75), (9.25, 5, 9.25), head_faces, uv=[5, 5, 11, 11])]
                 + [leaf(d, 6.5, 6.0, 0.5, 22.5, "leaf") for d in ("north", "south", "east", "west")]
                 + [leaf(d, 4.5, 5.0, 0.5, 45, "leaf", centre=c) for d, c in OUTER_CUP(2.0)],
        # A full, firm head with its big outer leaves spread round its foot.
        "ripe": [box((4.5, 0, 4.5), (11.5, 6, 11.5), head_faces, uv=[1, 1, 15, 15]),
                 box((5.25, 5.5, 5.25), (10.75, 7.5, 10.75), head_faces, uv=[2, 2, 14, 14])]
                + [leaf(d, 8.0, 9.0, 0.4, 22.5, "leaf") for d in ("north", "south", "east", "west")]
                + [leaf(d, 5.0, 7.0, 0.6, 45, "leaf", centre=c) for d, c in OUTER_CUP(3.6)],
    }
    by_age = ["sprout", "sprout", "rosette", "rosette", "young", "young", "ripe", "ripe"]
    for age, stage in enumerate(by_age):
        write_model(f"cabbage_stage{age}", textures, stages[stage], particle="cabbage_leaf")


# Tomato: a vine climbing the trellis, leafier each stage, flowering yellow, then hung with green and then red fruit.

TRELLIS = [
    box((1, 0, 7), (3, 16, 9), "post"), box((13, 0, 7), (15, 16, 9), "post"),
    box((1, 5, 7.5), (15, 6, 8.5), "post"), box((1, 10, 7.5), (15, 11, 8.5), "post"), box((1, 15, 7.5), (15, 16, 8.5), "post"),
]


def tomato_textures():
    stem, leaves = ramp("#4f8a30"), ["#5ea83e", "#6cb646", "#559c38"]

    def vine(height, flowers=False):
        s = Sprite()
        # Two stems winding up the trellis from the root.
        for x0, phase in ((6, 0.0), (10, 2.0)):
            points = []
            for y in range(15, 15 - height, -1):
                points.append((round(x0 + math.sin(y * 0.6 + phase) * 1.4), y))
            for x, y in points:
                s.put(x, y, stem[BASE])
            # Toothed compound leaves along the stems, alternating sides.
            for i, (x, y) in enumerate(points[1::3]):
                tones = ramp(leaves[i % 3], spread=0.5)
                side = 1 if (i + (x0 > 8)) % 2 else -1
                s.ellipse(tones, x + side * 2.0, y - 0.5, 1.9, 1.2, angle=side * 30, outline=False)
                s.ellipse(tones, x + side * 3.4, y - 1.8, 1.0, 0.8, outline=False)
                s.line(stem[SHADOW], x, y, x + side * 3, y - 1)
        if flowers:
            for x, y in [(4, 4), (11, 6), (7, 9), (12, 11), (5, 12)]:
                s.pixels_at(ramp("#f0d030")[BASE], [(x, y), (x + 1, y)])
        return s
    write_texture("tomato_vine_young", vine(5))
    write_texture("tomato_vine_half", vine(10))
    write_texture("tomato_vine_full", vine(16))
    write_texture("tomato_vine_flowering", vine(16, flowers=True))

    for name, colour, calyx in (("tomato_fruit", "#d8281e", "#4a8a2a"), ("tomato_fruit_unripe", "#8ab84a", "#3a7a2a")):
        fruit = Sprite()
        tones = ramp(colour, spread=0.6)
        fruit.shape(lambda x, y: True, tones, (7.5, 7.5), (8.0, 8.0))
        fruit.pixels_at(tones[HIGHLIGHT], [(4, 4), (5, 4), (4, 5)])
        write_texture(name, fruit)
        top = Sprite()
        top.shape(lambda x, y: True, tones, (7.5, 7.5), (8.0, 8.0))
        green = ramp(calyx)
        top.pixels_at(green[BASE], [(7, 7), (8, 7), (7, 8), (8, 8), (6, 6), (9, 6), (6, 9), (9, 9), (7, 5), (8, 10)])
        write_texture(name + "_top", top)


def tomato_models():
    textures = {"post": "minecraft:block/stripped_oak_log", "young": "tomato_vine_young", "half": "tomato_vine_half",
                "full": "tomato_vine_full", "flowering": "tomato_vine_flowering", "fruit": "tomato_fruit", "fruit_top": "tomato_fruit_top",
                "unripe": "tomato_fruit_unripe", "unripe_top": "tomato_fruit_unripe_top"}

    def on_trellis(vine):
        # The vine on both faces of the trellis, just in front of and behind its rails.
        return TRELLIS + [plane_x(0, 16, 0, 16, 7.2, vine), plane_x(0, 16, 0, 16, 8.8, vine)]

    def fruit(colour):
        faces = {"north": colour, "south": colour, "east": colour, "west": colour, "up": colour + "_top", "down": colour}
        # Trusses of two or three fruit hanging on each face of the trellis, the front ones first.
        spots = [(5, 12, 9.0), (6.6, 10.6, 9.0), (10.5, 7, 9.0), (11.8, 8.6, 9.0), (8, 3.5, 9.0),
                 (4.5, 7, 5.4), (11, 12, 5.4), (9.4, 13.2, 5.4)]
        return [box((x - 1.4, y - 1.4, z), (x + 1.4, y + 1.4, z + 1.6), faces, uv=[4, 4, 12, 12]) for x, y, z in spots]
    stages = {
        "young": on_trellis("young"), "half": on_trellis("half"), "full": on_trellis("full"),
        "flowering": on_trellis("flowering") + fruit("unripe")[::2],
        "ripe": on_trellis("full") + fruit("fruit"),
    }
    by_age = ["young", "young", "half", "half", "full", "full", "flowering", "ripe"]
    for age, stage in enumerate(by_age):
        write_model(f"tomato_vine_stage{age}", textures, stages[stage], particle="tomato_vine_full")


def main():
    cabbage_textures()
    cabbage_models()
    tomato_textures()
    tomato_models()


if __name__ == "__main__":
    main()
