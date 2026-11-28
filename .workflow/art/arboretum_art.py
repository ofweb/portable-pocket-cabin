"""Textures, models and blockstates for the arboretum's blocks (B-0023): the door into the grove, the grove's exit door,
and the planting board.

Run from the repository root: python3 .workflow/art/arboretum_art.py
The doors use the vanilla door shapes with their own textures: a green-stained door with a leaded window full of leaves.
The planting board is a small notice board on two posts under a little roof, with a planting chart pinned to it.
"""
import json
import os

from PIL import Image

A = "src/main/resources/assets/portable_pocket_cabin"
NS = "portable_pocket_cabin:block/"

PALETTE = {
    "D": "#3b2a1c", "M": "#5b4a2a", "L": "#6f5d34", "H": "#87723f",
    "G": "#2f5f2a", "g": "#4c8c38", "l": "#78b852",
    "S": "#a8d4dc", "s": "#7fb2c0", "K": "#d6b247", "k": "#8c6a22",
    "P": "#e8dcb8", "p": "#cdbd92", "R": "#9a4f2c", "r": "#6e3720",
    "W": "#8a6a44", "w": "#6c5133", "V": "#a3845a",
}

DOOR_TOP = [
    "DDDDDDDDDDDDDDDD",
    "DLLLLLLLLLLLLLLD",
    "DLDDDDDDDDDDDDLD",
    "DLDsSSlgSSSSsDLD",
    "DLDSSglgGSSlSDLD",
    "DLDSgGlSSgSglDLD",
    "DLDSSgSSglgSGDLD",
    "DLDDDDDDDDDDDDLD",
    "DLDsSSSgGlSSsDLD",
    "DLDSSSSSgSSlgDLD",
    "DLDsSgSSSSglGDLD",
    "DLDDDDDDDDDDDDLD",
    "DLLLLLLLLLLLLLLD",
    "DMHMMHMMHMMHMMMD",
    "DMHMMHMMHMMHMMMD",
    "DMHMMHMMHMMHMMMD",
]

DOOR_BOTTOM = [
    "DMHMMHMMHMMHMMMD",
    "DMHMMHMMHMMHMMMD",
    "DLLLLLLLLLLLLLLD",
    "DMHMMHMMHMMHMMMD",
    "DMHMMgMMHMMHMKKD",
    "DMHMglgMHMMHMkKD",
    "DMHMMgMlgMMHMMMD",
    "DMHMMGgMHMMHMMMD",
    "DMHMMHGMHMMHMMMD",
    "DMHMMHGMHMMHMMMD",
    "DMHMMwGwHMMHMMMD",
    "DMHMMwwwHMMHMMMD",
    "DMHMMHMMHMMHMMMD",
    "DLLLLLLLLLLLLLLD",
    "DMHMMHMMHMMHMMMD",
    "DDDDDDDDDDDDDDDD",
]

# The chart pinned to the board: six little saplings in pots, in two rows, on pale paper in a wooden frame.
BOARD_FRONT = [
    "wwwwwwwwwwwwwwww",
    "wPPPPPPPPPPPPPPw",
    "wPpPPPPPPPPPPpPw",
    "wPPlPPPgPPPPlPPw",
    "wPlgPPPglPPgGPPw",
    "wPPGPPPPGPPPGPPw",
    "wPRRPPPRRPPPRRPw",
    "wPrrPPPrrPPPrrPw",
    "wPPPPPPPPPPPPPPw",
    "wPPgPPPPlPPPlPPw",
    "wPglPPPgGPPlgPPw",
    "wPPGPPPPGPPPGPPw",
    "wPRRPPPRRPPPRRPw",
    "wPrrPPPrrPPPrrPw",
    "wPpPPPPPPPPPPpPw",
    "wwwwwwwwwwwwwwww",
]

WOOD = [
    "VWWWVWWWWVWWWVWW",
    "WWwWWWWVWWWwWWWW",
    "WWWWWwWWWWWWWWVW",
    "wWWVWWWWWwWWWWWW",
    "WWWWWWVWWWWWVWWw",
    "WVWWwWWWWWVWWWWW",
    "WWWWWWWWwWWWWwWW",
    "WWwWWVWWWWWWWWWV",
    "VWWWWWWWVWWwWWWW",
    "WWWWwWWWWWWWWVWW",
    "WWVWWWWwWWVWWWWW",
    "wWWWWWWWWWWWwWWW",
    "WWWWVWWWwWWWWWWV",
    "WwWWWWWWWWVWWWWW",
    "WWWWWwWVWWWWWwWW",
    "WVWWWWWWWWWWWWWW",
]

ROOF = [row.replace("W", "R").replace("w", "r").replace("V", "R") for row in WOOD]


def rgb(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def draw(name, grid):
    assert len(grid) == 16 and all(len(row) == 16 for row in grid), name
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), (*rgb(PALETTE[ch]), 255))
    path = f"{A}/textures/block/{name}.png"
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def door(block, texture):
    """The vanilla door's eight shapes with this door's textures, and a blockstate for every facing, half and hinge."""
    parts = ["bottom_left", "bottom_left_open", "bottom_right", "bottom_right_open",
             "top_left", "top_left_open", "top_right", "top_right_open"]
    for part in parts:
        write(f"{A}/models/block/{block}_{part}.json", {
            "parent": f"minecraft:block/door_{part}",
            "textures": {"bottom": NS + texture + "_bottom", "top": NS + texture + "_top"},
        })
    variants = {}
    base = {"east": 0, "south": 90, "west": 180, "north": 270}
    for facing, rotation in base.items():
        for half in ("lower", "upper"):
            for hinge in ("left", "right"):
                for opened in (False, True):
                    part = ("bottom" if half == "lower" else "top") + "_" + hinge + ("_open" if opened else "")
                    y = rotation
                    if opened:
                        y += 90 if hinge == "left" else 270
                    variant = {"model": NS + block + "_" + part}
                    if y % 360:
                        variant["y"] = y % 360
                    variants[f"facing={facing},half={half},hinge={hinge},open={str(opened).lower()}"] = variant
    # The doors never open or take power, but the property exists, so every combination needs a model.
    write(f"{A}/blockstates/{block}.json", {"variants": {
        f"{key},powered={powered}": value for key, value in variants.items() for powered in ("false", "true")}})


def box(start, end, faces, rotation=None):
    element = {"from": list(start), "to": list(end), "faces": faces}
    if rotation:
        element["rotation"] = rotation
    return element


def all_faces(texture, start, end):
    x0, y0, z0 = start
    x1, y1, z1 = end
    return {
        "north": {"uv": [16 - x1, 16 - y1, 16 - x0, 16 - y0], "texture": texture},
        "south": {"uv": [x0, 16 - y1, x1, 16 - y0], "texture": texture},
        "east": {"uv": [16 - z1, 16 - y1, 16 - z0, 16 - y0], "texture": texture},
        "west": {"uv": [z0, 16 - y1, z1, 16 - y0], "texture": texture},
        "up": {"uv": [x0, z0, x1, z1], "texture": texture},
        "down": {"uv": [x0, 16 - z1, x1, 16 - z0], "texture": texture},
    }


def planting_board():
    """Two posts, the board between them with the chart on its south face, and a little gabled roof."""
    elements = []
    for x in (2, 12):
        elements.append(box((x, 0, 7), (x + 2, 15, 9), all_faces("#wood", (x, 0, 7), (x + 2, 15, 9))))
    faces = all_faces("#wood", (1, 4, 7.5), (15, 14, 9))
    faces["south"] = {"uv": [0, 0, 16, 16], "texture": "#front"}
    elements.append(box((1, 4, 8.5), (15, 14, 9.5), faces))
    for side, angle in ((-1, -30), (1, 30)):
        z0, z1 = (4, 8) if side < 0 else (8, 12)
        elements.append(box((0, 14.5, z0), (16, 15.5, z1), all_faces("#roof", (0, 14.5, z0), (16, 15.5, z1)),
                            {"origin": [8, 15, 8], "axis": "x", "angle": 22.5 * (-1 if side < 0 else 1)}))
    write(f"{A}/models/block/planting_board.json", {
        "textures": {"particle": NS + "planting_board_wood", "wood": NS + "planting_board_wood",
                     "front": NS + "planting_board_front", "roof": NS + "planting_board_roof"},
        "elements": elements,
    })
    write(f"{A}/blockstates/planting_board.json", {"variants": {
        f"facing={facing}": ({"model": NS + "planting_board", "y": y} if y else {"model": NS + "planting_board"})
        for facing, y in (("south", 0), ("west", 90), ("north", 180), ("east", 270))}})


def main():
    draw("arboretum_door_top", DOOR_TOP)
    draw("arboretum_door_bottom", DOOR_BOTTOM)
    draw("planting_board_front", BOARD_FRONT)
    draw("planting_board_wood", WOOD)
    draw("planting_board_roof", ROOF)
    door("arboretum_door", "arboretum_door")
    door("grove_door", "arboretum_door")
    planting_board()


if __name__ == "__main__":
    main()
