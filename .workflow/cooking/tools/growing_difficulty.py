"""B-0054 growing difficulty: sets each cooking plant's speed tier, climate, and special needs in its growing needs data.

Run from the repository root: python3 .workflow/cooking/tools/growing_difficulty.py
A plant's climate comes from the base temperatures of the biomes it grows wild in, unless CLIMATE names it.
"""
import json
import os
import statistics

D = "src/main/resources/data/portable_pocket_cabin"

TIERS = {"fast": 0.35, "average": 0.25, "slow": 0.12, "very_slow": 0.06, "trunk": 0.05}
FAST = {"lettuce", "radish", "spring_onion", "cilantro", "dill", "parsley", "basil", "holy_basil", "thai_basil"}
SLOW = {"ginger", "galangal", "turmeric", "cassava", "sweet_potato", "water_chestnut", "lemongrass"}
VERY_SLOW = {"asparagus", "rhubarb", "pineapple", "vanilla_vine", "wasabi"}
# Sea plants and shellfish beds keep their own rates, and grow in any water and climate.
WATER = {"wakame_plant", "laver_plant", "sea_lettuce_plant", "mussel_bed", "oyster_bed"}
SHADE = {"vanilla_vine", "coffee_leaves", "cardamom", "wasabi"}
# Climates that the wild biomes would get wrong: highland Andean crops, wasabi of cold mountain streams, and beach palms.
CLIMATE = {"wasabi": "cold", "coconut_leaves": "warm", "aji_amarillo": "temperate", "purple_corn": "temperate", "oyster_mushrooms": "any"}
# Vanilla base temperatures of the biomes and biome tags that plants grow wild in.
TEMPERATURE = {"minecraft:plains": 0.8, "minecraft:sunflower_plains": 0.8, "minecraft:forest": 0.7, "minecraft:flower_forest": 0.7,
               "minecraft:dark_forest": 0.7, "minecraft:pale_garden": 0.7, "#minecraft:is_forest": 0.7, "minecraft:meadow": 0.5,
               "minecraft:cherry_grove": 0.5, "minecraft:river": 0.5, "minecraft:swamp": 0.8, "minecraft:mangrove_swamp": 0.8,
               "minecraft:beach": 0.8, "minecraft:stony_shore": 0.2, "minecraft:cold_ocean": 0.5, "minecraft:deep_cold_ocean": 0.5,
               "minecraft:lukewarm_ocean": 0.5, "minecraft:deep_lukewarm_ocean": 0.5, "#minecraft:is_taiga": 0.25,
               "minecraft:windswept_hills": 0.2, "minecraft:windswept_gravelly_hills": 0.2, "minecraft:windswept_forest": 0.2,
               "#minecraft:is_jungle": 0.95, "minecraft:sparse_jungle": 0.95, "minecraft:bamboo_jungle": 0.95,
               "#minecraft:is_savanna": 2.0, "minecraft:windswept_savanna": 2.0, "minecraft:desert": 2.0, "#minecraft:is_badlands": 2.0}


def climate_of(temperature):
    if temperature < 0.3:
        return "cold"
    if temperature < 0.9:
        return "temperate"
    if temperature < 1.5:
        return "warm"
    return "hot"


def wild_name(plant):
    if plant.endswith("_leaves"):
        return plant[:-len("_leaves")]
    if plant == "palm_log":
        return "oil_palm"
    return plant[:-len("_log")] if plant.endswith("_log") else plant


def main():
    rows = []
    for file in sorted(os.listdir(f"{D}/cooking_plant")):
        plant = file[:-len(".json")]
        path = f"{D}/cooking_plant/{file}"
        needs = json.load(open(path))
        if plant in WATER:
            needs["climate"] = "any"
        else:
            if plant.endswith("_log"):
                tier = "trunk"
            elif plant in VERY_SLOW or plant.endswith("_leaves"):
                tier = "very_slow"
            elif plant in FAST:
                tier = "fast"
            elif plant in SLOW:
                tier = "slow"
            else:
                tier = "average"
            needs["growth_chance"] = TIERS[tier]
            if plant in CLIMATE:
                needs["climate"] = CLIMATE[plant]
            else:
                biomes = json.load(open(f"{D}/tags/worldgen/biome/grows_wild/{wild_name(plant)}.json"))["values"]
                biomes = [b["id"] if isinstance(b, dict) else b for b in biomes]
                needs["climate"] = climate_of(statistics.median(TEMPERATURE[b] for b in biomes))
            if plant in SHADE:
                needs["shade"] = True
        with open(path, "w") as f:
            json.dump(needs, f, indent=2)
            f.write("\n")
        rows.append((plant, needs["climate"], needs["growth_chance"], needs.get("shade", False)))
    for row in rows:
        print(*row)


if __name__ == "__main__":
    main()
