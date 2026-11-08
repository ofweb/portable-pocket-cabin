"""B-0054 harvest models: writes the loot table of every cooking crop and vine from its model.

Run from the repository root: python3 .workflow/cooking/tools/harvest_models.py
Each model gives the harvest and the next planting in its own way; see .workflow/features/B-0054/design.md.
"""
import json
import os

R = "src/main/resources"
D = f"{R}/data/portable_pocket_cabin"
A = f"{R}/assets/portable_pocket_cabin"
P = "portable_pocket_cabin:"

# Grain and tuber: the harvest is also what plants it.
GRAIN = {"barley": "barley", "rye": "rye", "corn": "corn", "blue_corn": "blue_corn", "purple_corn": "purple_corn",
         "rice": "rice", "glutinous_rice": "glutinous_rice", "white_bean": "white_bean", "black_bean": "black_bean",
         "chickpea": "chickpea", "soybean": "soybean", "azuki_bean": "azuki_bean", "peanut": "peanut",
         "sesame": "sesame_seed", "cumin": "cumin_seed", "anise": "anise_seed", "caraway": "caraway_seed",
         "mustard": "mustard_seed", "rapeseed": "rapeseed", "garlic": "garlic", "onion": "onion", "red_onion": "red_onion",
         "shallot": "shallot", "sweet_potato": "sweet_potato", "cassava": "cassava", "ginger": "ginger",
         "galangal": "galangal", "turmeric": "turmeric", "lemongrass": "lemongrass", "water_chestnut": "water_chestnut",
         "wasabi": "wasabi_root"}
# Fruit with seeds: plant -> (produce, seeds).
FRUIT = {"cabbage": ("cabbage", "cabbage_seeds"), "red_cabbage": ("red_cabbage", "red_cabbage_seeds"),
         "bell_pepper": ("bell_pepper", "bell_pepper_seeds"), "paprika_pepper": ("paprika_pepper", "paprika_pepper_seeds"),
         "green_chili": ("green_chili", "green_chili_seeds"), "poblano_chili": ("poblano_chili", "poblano_chili_seeds"),
         "red_chili": ("red_chili", "red_chili_seeds"), "birds_eye_chili": ("birds_eye_chili", "birds_eye_chili_seeds"),
         "aji_amarillo": ("aji_amarillo", "aji_amarillo_seeds"), "aubergine": ("aubergine", "aubergine_seeds"),
         "thai_aubergine": ("thai_aubergine", "thai_aubergine_seeds"), "courgette": ("courgette", "courgette_seeds"),
         "tomatillo": ("tomatillo", "tomatillo_seeds"), "roselle": ("roselle", "roselle_seeds"),
         "cardamom": ("cardamom_pod", "cardamom_seeds"), "pineapple": ("pineapple", "pineapple_crown")}
# Leaf, then seed: plant -> (leaf or root at age 6, seeds at age 7).
LEAF = {"lettuce": ("lettuce", "lettuce_seeds"), "radish": ("radish", "radish_seeds"), "basil": ("basil", "basil_seeds"),
        "holy_basil": ("holy_basil", "holy_basil_seeds"), "thai_basil": ("thai_basil", "thai_basil_seeds"),
        "dill": ("dill", "dill_seeds"), "parsley": ("parsley", "parsley_seeds"), "celery": ("celery", "celery_seeds"),
        "spring_onion": ("spring_onion", "spring_onion_seeds"), "cilantro": ("cilantro", "coriander_seed")}
# Perennial crowns on the field: plant -> (harvest, crown).
CROWN = {"asparagus": ("asparagus", "asparagus_crown"), "rhubarb": ("rhubarb", "rhubarb_crown")}
# Trellis vines with seeds: plant -> (produce, seeds). Every other vine is grown from cuttings.
SEED_VINES = {"tomato_vine": ("tomato", "tomato_seeds"), "cucumber_vine": ("cucumber", "cucumber_seeds"),
              "long_bean_vine": ("long_bean", "long_bean_seeds")}
CUTTING_VINES = ["grape_vine", "red_grape_vine", "white_grape_vine", "hop_vine", "peppercorn_vine", "vanilla_vine", "betel_vine"]
# Bushes grown from cuttings: plant -> cutting. What they give no longer plants them.
CUTTING_BUSHES = {"blackcurrant_bush": "blackcurrant_cutting", "redcurrant_bush": "redcurrant_cutting",
                  "raspberry_bush": "raspberry_cutting", "strawberry_bush": "strawberry_runner",
                  "thyme_bush": "thyme_cutting", "marjoram_bush": "marjoram_cutting", "rosemary_bush": "rosemary_cutting",
                  "oregano_bush": "oregano_cutting", "savory_bush": "savory_cutting", "mint_bush": "mint_cutting",
                  "pandan_bush": "pandan_cutting", "elder_bush": "elder_cutting", "tea_bush": "tea_cutting"}
REMOVED_SEEDS = ["barley_seeds", "rye_seeds", "corn_seeds", "blue_corn_seeds", "purple_corn_seeds", "rice_seedlings",
                 "glutinous_rice_seedlings"]


def w(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def age(plant, value):
    return {"condition": "minecraft:block_state_property", "block": P + plant, "properties": {"age": str(value)}}


def item(name, low=None, high=None, conditions=None):
    entry = {"type": "minecraft:item", "name": name if ":" in name else P + name}
    if conditions:
        entry["conditions"] = conditions
    if low is not None:
        count = low if high is None else {"type": "minecraft:uniform", "min": float(low), "max": float(high)}
        entry["functions"] = [{"function": "minecraft:set_count", "count": float(count) if high is None else count}]
    return entry


def pool(*entries, conditions=None):
    p = {"rolls": 1.0, "entries": list(entries)}
    if conditions:
        p["conditions"] = conditions
    return p


def pick(*children):
    return {"type": "minecraft:alternatives", "children": list(children)}


def table(plant, pools, decay=True):
    t = {"type": "minecraft:block"}
    if decay:
        t["functions"] = [{"function": "minecraft:explosion_decay"}]
    t["pools"] = pools
    t["random_sequence"] = f"{P}blocks/{plant}"
    w(f"{D}/loot_table/blocks/{plant}.json", t)


def grain(plant, harvest, ripe=7):
    table(plant, [pool(pick(item(harvest, 2, 4, [age(plant, ripe)]), item(harvest)))])


def fruit(plant, produce, seeds, ripe=7, extra=()):
    table(plant, [*extra, pool(item(produce, 1, 2), conditions=[age(plant, ripe)]),
                  pool(pick(item(seeds, 1, 3, [age(plant, ripe)]), item(seeds)))])


def leaf_then_seed(plant, leaf, seeds):
    table(plant, [pool(pick(item(seeds, 2, 3, [age(plant, 7)]), item(leaf, 1, 3, [age(plant, 6)]), item(seeds)))])


def crown(plant, harvest, crown_item):
    table(plant, [pool(item(harvest, 1, 2), conditions=[age(plant, 7)]),
                  pool(pick(item(crown_item, 2, None, [age(plant, 7)]), item(crown_item)))])


def main():
    for plant, harvest in GRAIN.items():
        grain(plant, harvest)
    for plant, (produce, seeds) in FRUIT.items():
        fruit(plant, produce, seeds)
    for plant, (leaf, seeds) in LEAF.items():
        leaf_then_seed(plant, leaf, seeds)
    for plant, (harvest, crown_item) in CROWN.items():
        crown(plant, harvest, crown_item)
    trellis = pool(item("trellis"))
    for plant, (produce, seeds) in SEED_VINES.items():
        fruit(plant, produce, seeds, extra=(trellis,))
    for plant in CUTTING_VINES:
        table(plant, [trellis], decay=False)
    for plant, cutting in CUTTING_BUSHES.items():
        table(plant, [pool(item(cutting))], decay=False)
    # Vanilla wheat plants from wheat: a ripe crop gives two to four, a young one gives one back, with no seeds.
    w(f"{R}/data/minecraft/loot_table/blocks/wheat.json", {"type": "minecraft:block", "functions": [{"function": "minecraft:explosion_decay"}],
      "pools": [pool(pick(item("minecraft:wheat", 2, 4, [{"condition": "minecraft:block_state_property", "block": "minecraft:wheat",
                                                           "properties": {"age": "7"}}]), item("minecraft:wheat")))],
      "random_sequence": "minecraft:blocks/wheat"})

    # The grain seed items are gone: their grain plants them.
    lang_path = f"{A}/lang/en_us.json"
    lang = json.load(open(lang_path))
    for seed in REMOVED_SEEDS:
        for path in (f"{A}/models/item/{seed}.json", f"{A}/items/{seed}.json"):
            if os.path.exists(path):
                os.remove(path)
        lang.pop(f"item.portable_pocket_cabin.{seed}", None)
    w(lang_path, lang)
    renamed = {}
    # Wandering traders sell the cuttings of bushes that used to plant from their fruit.
    for plant, cutting in CUTTING_BUSHES.items():
        produce = {"pandan_bush": "pandan_leaf"}.get(plant, plant.removesuffix("_bush"))
        old = f"{D}/villager_trade/wandering_trader/emerald_{produce}.json"
        if not os.path.exists(old):
            continue
        trade = json.load(open(old))
        trade["gives"]["id"] = P + cutting
        os.remove(old)
        w(f"{D}/villager_trade/wandering_trader/emerald_{cutting}.json", trade)
        renamed[f"{P}wandering_trader/emerald_{produce}"] = f"{P}wandering_trader/emerald_{cutting}"
    for folder in ("wandering_trader", "farmer/3"):
        for seed in REMOVED_SEEDS:
            old = f"{D}/villager_trade/{folder}/emerald_{seed}.json"
            if not os.path.exists(old):
                continue
            grain_item = seed.replace("_seeds", "").replace("_seedlings", "")
            trade = json.load(open(old))
            trade["gives"]["id"] = P + grain_item
            os.remove(old)
            w(f"{D}/villager_trade/{folder}/emerald_{grain_item}.json", trade)
            renamed[f"{P}{folder}/emerald_{seed}"] = f"{P}{folder}/emerald_{grain_item}"
    for tag in ("wandering_trader/uncommon", "farmer/level_3"):
        path = f"{R}/data/minecraft/tags/villager_trade/{tag}.json"
        data = json.load(open(path))
        data["values"] = [renamed.get(v, v) for v in data["values"]]
        w(path, data)


if __name__ == "__main__":
    main()
