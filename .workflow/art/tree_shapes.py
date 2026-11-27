"""Fruit-tree shapes, set on each tree's configured feature from Minecraft's own trunk and foliage placers.

Run from the repository root: python3 .workflow/art/tree_shapes.py
Each kind gets a shape of its own from Minecraft's layouts, so an orchard of mixed trees looks mixed: apple spreads
wide on a forked trunk and apricot lower and wider still; sweet cherry grows tall and upright; plum is a round crown on
a branching trunk; almond, ume and annatto are gnarled and open on bending trunks; the citrus trees and yerba mate are
low, dense mounds; olive forks low under a silvery crown; bay laurel, nutmeg, clove and star anise are dense cones like
spruces; mango and cinnamon are big domes; allspice holds a small crown high on a tall trunk; coffee is a shrub-tree;
tamarind spreads flat like an acacia; papaya is a slim trunk under a small crown; the coconut palm leans and the oil
palm stands straight. Each tree keeps its own log and leaves; only the trunk and foliage placers change.
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
# More of Minecraft's layouts, so trees of different kinds look different.
UPRIGHT = (
    {"type": "minecraft:fancy_trunk_placer", "base_height": 6, "height_rand_a": 3, "height_rand_b": 0},
    {"type": "minecraft:fancy_foliage_placer", "radius": 2, "offset": 4, "height": 4},
)
LOW_SPREADING = (
    {"type": "minecraft:cherry_trunk_placer", "base_height": 4, "height_rand_a": 1, "height_rand_b": 0,
     "branch_count": {"type": "minecraft:weighted_list", "distribution": [{"data": 2, "weight": 1}, {"data": 3, "weight": 2}]},
     "branch_horizontal_length": uniform(3, 4), "branch_start_offset_from_top": {"min_inclusive": -2, "max_inclusive": -1},
     "branch_end_offset_from_top": uniform(-1, 0)},
    {"type": "minecraft:cherry_foliage_placer", "radius": 3, "offset": 0, "height": 4, "corner_hole_chance": 0.3,
     "hanging_leaves_chance": 0.12, "hanging_leaves_extension_chance": 0.3, "wide_bottom_layer_hole_chance": 0.3},
)
GNARLED = (
    {"type": "minecraft:bending_trunk_placer", "base_height": 3, "height_rand_a": 2, "height_rand_b": 0, "min_height_for_leaves": 2,
     "bend_length": uniform(1, 3)},
    {"type": "minecraft:random_spread_foliage_placer", "radius": 3, "offset": 0, "foliage_height": 3, "leaf_placement_attempts": 70},
)
MOUND = (
    {"type": "minecraft:straight_trunk_placer", "base_height": 4, "height_rand_a": 1, "height_rand_b": 0},
    {"type": "minecraft:blob_foliage_placer", "radius": 3, "offset": 0, "height": 3},
)
FORKED = (
    {"type": "minecraft:forking_trunk_placer", "base_height": 3, "height_rand_a": 1, "height_rand_b": 1},
    {"type": "minecraft:blob_foliage_placer", "radius": 2, "offset": 0, "height": 3},
)
CONE = (
    {"type": "minecraft:straight_trunk_placer", "base_height": 5, "height_rand_a": 2, "height_rand_b": 1},
    {"type": "minecraft:spruce_foliage_placer", "radius": uniform(2, 3), "offset": uniform(0, 1), "trunk_height": uniform(1, 2)},
)
DOME = (
    {"type": "minecraft:straight_trunk_placer", "base_height": 6, "height_rand_a": 2, "height_rand_b": 0},
    {"type": "minecraft:blob_foliage_placer", "radius": 3, "offset": 0, "height": 4},
)
TALL_TOP = (
    {"type": "minecraft:straight_trunk_placer", "base_height": 6, "height_rand_a": 2, "height_rand_b": 0},
    {"type": "minecraft:blob_foliage_placer", "radius": 2, "offset": 0, "height": 3},
)
LEANING_PALM = (
    {"type": "minecraft:bending_trunk_placer", "base_height": 5, "height_rand_a": 2, "height_rand_b": 0, "min_height_for_leaves": 4,
     "bend_length": uniform(2, 3)},
    {"type": "minecraft:acacia_foliage_placer", "radius": 2, "offset": 0},
)
SHAPES = {"apple": SPREADING, "apricot": LOW_SPREADING, "sweet_cherry": UPRIGHT, "plum": ROUND,
          **{tree: GNARLED for tree in ("almond", "ume", "annatto")},
          **{tree: MOUND for tree in ("lemon", "lime", "orange", "makrut_lime", "yerba_mate")},
          "olive": FORKED, **{tree: CONE for tree in ("bay_laurel", "nutmeg", "clove", "star_anise")},
          "mango": DOME, "cinnamon": DOME, "allspice": TALL_TOP,
          "coffee": SHRUB, "tamarind": FLAT, "papaya": TUFT, "coconut": LEANING_PALM, "oil_palm": PALM}


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
