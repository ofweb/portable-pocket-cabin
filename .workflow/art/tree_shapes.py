"""Fruit-tree shapes, set on each tree's configured feature from Minecraft's own trunk and foliage placers.

Run from the repository root: python3 .workflow/art/tree_shapes.py
Orchard trees in life have a short trunk that forks low into a broad, rounded crown, wider than tall. Two shapes
come closest in Minecraft's terms: a spreading tree on a forked trunk, like the cherry tree's, for apple, sweet cherry,
apricot, and almond; and a rounder tree on a fancy oak's branching trunk for plum, ume, the citrus trees, olive, and
bay laurel. Each tree keeps its own log and leaves; only the trunk and foliage placers change. The tropical trees and
palms keep their straight trunks for now.
"""
import json

D = "src/main/resources/data/portable_pocket_cabin/worldgen/configured_feature/"


def uniform(low, high):
    return {"type": "minecraft:uniform", "min_inclusive": low, "max_inclusive": high}


SPREADING = (
    {"type": "minecraft:cherry_trunk_placer", "base_height": 5, "height_rand_a": 1, "height_rand_b": 0,
     "branch_count": {"type": "minecraft:weighted_list", "distribution": [{"data": 2, "weight": 1}, {"data": 3, "weight": 1}]},
     "branch_horizontal_length": uniform(2, 3), "branch_start_offset_from_top": {"min_inclusive": -2, "max_inclusive": -1},
     "branch_end_offset_from_top": uniform(-1, 0)},
    {"type": "minecraft:cherry_foliage_placer", "radius": 3, "offset": 0, "height": 4, "corner_hole_chance": 0.25,
     "hanging_leaves_chance": 0.17, "hanging_leaves_extension_chance": 0.33, "wide_bottom_layer_hole_chance": 0.25},
)
ROUND = (
    {"type": "minecraft:fancy_trunk_placer", "base_height": 4, "height_rand_a": 2, "height_rand_b": 0},
    {"type": "minecraft:fancy_foliage_placer", "radius": 2, "offset": 4, "height": 4},
)
SHAPES = {**{tree: SPREADING for tree in ("apple", "sweet_cherry", "apricot", "almond")},
          **{tree: ROUND for tree in ("plum", "ume", "lemon", "orange", "lime", "makrut_lime", "olive", "bay_laurel")}}


def main():
    for tree, (trunk, foliage) in SHAPES.items():
        path = f"{D}{tree}.json"
        feature = json.load(open(path))
        config = feature["config"]
        config["trunk_placer"], config["foliage_placer"] = trunk, foliage
        config["minimum_size"] = {"type": "minecraft:two_layers_feature_size", "limit": 1, "lower_size": 0, "upper_size": 1}
        with open(path, "w") as f:
            json.dump(feature, f, indent=2)
            f.write("\n")


if __name__ == "__main__":
    main()
