"""Fruit-tree shapes, set on each tree's configured feature from Minecraft's own trunk and foliage placers.

Run from the repository root: python3 .workflow/art/tree_shapes.py
Orchard trees in life have a short trunk that forks low into a broad, rounded crown, wider than tall. Two shapes
come closest in Minecraft's terms: a spreading tree on a forked trunk, like the cherry tree's, for apple, sweet cherry,
apricot, and almond; and a rounder tree on a fancy oak's branching trunk for plum, ume, the citrus trees, olive, bay
laurel, and most of the tropical spice trees. Coffee is a shrub-tree, tamarind spreads flat like an acacia, papaya is a
slim trunk with a tuft on top, and the palms carry a flat crown on a tall trunk. Each tree keeps its own log and leaves;
only the trunk and foliage placers change. Cinnamon keeps its straight trunk, whose bark is harvested.
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
# A shrub-tree like Minecraft's azalea tree, for coffee: a bending stem under spreading leaves.
SHRUB = (
    {"type": "minecraft:bending_trunk_placer", "base_height": 2, "height_rand_a": 1, "height_rand_b": 0, "min_height_for_leaves": 1,
     "bend_length": uniform(1, 2)},
    {"type": "minecraft:random_spread_foliage_placer", "radius": 2, "offset": 0, "foliage_height": 2, "leaf_placement_attempts": 50},
)
# An acacia's forked trunk under a wide, flat crown, for tamarind.
FLAT = (
    {"type": "minecraft:forking_trunk_placer", "base_height": 4, "height_rand_a": 2, "height_rand_b": 1},
    {"type": "minecraft:acacia_foliage_placer", "radius": 2, "offset": 0},
)
# A tall, slim, straight trunk with a small tuft of leaves at the top, for papaya.
TUFT = (
    {"type": "minecraft:straight_trunk_placer", "base_height": 4, "height_rand_a": 2, "height_rand_b": 0},
    {"type": "minecraft:blob_foliage_placer", "radius": 2, "offset": 0, "height": 2},
)
# A palm: a tall, straight trunk under a flat crown of fronds, for coconut and oil palm, whose trunks give sap.
PALM = (
    {"type": "minecraft:straight_trunk_placer", "base_height": 5, "height_rand_a": 2, "height_rand_b": 0},
    {"type": "minecraft:acacia_foliage_placer", "radius": 2, "offset": 0},
)
SHAPES = {**{tree: SPREADING for tree in ("apple", "sweet_cherry", "apricot", "almond")},
          **{tree: ROUND for tree in ("plum", "ume", "lemon", "orange", "lime", "makrut_lime", "olive", "bay_laurel", "mango", "nutmeg",
                                      "clove", "allspice", "annatto", "star_anise", "yerba_mate")},
          "coffee": SHRUB, "tamarind": FLAT, "papaya": TUFT, "coconut": PALM, "oil_palm": PALM}


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
