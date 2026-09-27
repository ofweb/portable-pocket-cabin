#!/usr/bin/env python3
"""Build source-traced 1.20.1 material catalogues from a jar and pinned mod sources."""

import argparse
import collections
import hashlib
import json
import re
import subprocess
import zipfile
from pathlib import Path


def dump(path, value):
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False, sort_keys=True) + "\n")


def label(identifier):
    return identifier.split(":")[-1].replace("_", " ").title().replace("O'", "O'")


def entries(text, prefix):
    pattern = re.compile(r"register\(context,\s*" + (re.escape(prefix) + r"\." if prefix else "") + r"([A-Z0-9_]+),")
    result = {}
    for match in pattern.finditer(text):
        start = match.start()
        depth = 0
        quote = False
        escape = False
        for end in range(start, len(text)):
            char = text[end]
            if char == '"' and not escape:
                quote = not quote
            if not quote:
                depth += (char == "(") - (char == ")")
                if depth == 0 and char == ")":
                    result[match.group(1)] = text[start:end + 1]
                    break
            escape = char == "\\" and not escape
    return result


def read(path):
    return Path(path).read_text()


def get_json(zipfile_, path):
    try:
        return json.loads(zipfile_.read(path))
    except KeyError:
        return None


def walk(obj):
    if isinstance(obj, dict):
        yield obj
        for value in obj.values():
            yield from walk(value)
    elif isinstance(obj, list):
        for value in obj:
            yield from walk(value)


def block_items(block, loot):
    result = set()
    if loot:
        for node in walk(loot):
            if node.get("type") in ("minecraft:item", "item") and "name" in node:
                result.add(node["name"])
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--server", required=True, type=Path, help="Inner 1.20.1 server jar")
    parser.add_argument("--bop", required=True, type=Path)
    parser.add_argument("--alex", required=True, type=Path)
    args = parser.parse_args()
    out = Path(__file__).parent
    z = zipfile.ZipFile(args.server)
    zpaths = set(z.namelist())
    bop_root = args.bop / "src/main"
    alex_root = args.alex / "src/main"
    bop_java = bop_root / "java/biomesoplenty"
    alex_java = alex_root / "java/com/github/alexthe666/alexsmobs"
    bop_data = bop_root / "resources/data"
    alex_data = alex_root / "resources/data/alexsmobs"

    def commit(path):
        result = subprocess.run(["git", "-C", str(path), "rev-parse", "HEAD"], capture_output=True, text=True, check=False)
        return result.stdout.strip() if result.returncode == 0 else None

    meta = {
        "minecraft_version": "1.20.1",
        "server_jar_sha256": hashlib.sha256(args.server.read_bytes()).hexdigest(),
        "bop_branch": "BOP-1.20-18.x.x",
        "bop_commit": commit(args.bop),
        "alex_branch": "1.20",
        "alex_commit": commit(args.alex),
        "source_roots": {
            "minecraft": "data/minecraft/ in the inner official server jar",
            "biomesoplenty": "src/main/ in Glitchfiend/BiomesOPlenty",
            "alexsmobs": "src/main/ in AlexModGuy/AlexsMobs",
        },
    }
    biomes = {}
    evidence = collections.defaultdict(list)
    unresolved = collections.defaultdict(set)

    def add(item, biome, kind, source_block=None, source_mob=None, files=(), note=""):
        if biome not in biomes or not item or ":" not in item:
            return
        evidence[item].append({
            "biome_id": biome, "acquisition_type": kind,
            "source_block": source_block, "source_mob": source_mob,
            "source_files": sorted(set(str(x) for x in files)), "note": note,
        })

    for path in sorted(x for x in zpaths if re.fullmatch(r"data/minecraft/worldgen/biome/[^/]+\.json", x)):
        ident = "minecraft:" + Path(path).stem
        data = get_json(z, path)
        dim = "nether" if ident.split(":")[1] in {"nether_wastes", "crimson_forest", "warped_forest", "soul_sand_valley", "basalt_deltas"} else "end" if ident.split(":")[1] in {"the_end", "end_highlands", "end_midlands", "end_barrens", "small_end_islands"} else "overworld"
        mobs = [x["type"] for group in data.get("spawners", {}).values() for x in group]
        biomes[ident] = {"id": ident, "display_name": label(ident), "source": "vanilla", "dimension": dim,
                         "biome_tags": [], "materials": [], "mobs": sorted(set(mobs)), "source_files": [path]}
        for group in data.get("features", []):
            for feature in group:
                placed_path = "data/minecraft/worldgen/placed_feature/" + feature.split(":")[-1] + ".json"
                placed = get_json(z, placed_path)
                if not placed:
                    continue
                configured_id = placed.get("feature", "").split(":")[-1]
                pending = [configured_id]
                seen = set()
                while pending:
                    key = pending.pop()
                    if key in seen:
                        continue
                    seen.add(key)
                    configured_path = "data/minecraft/worldgen/configured_feature/" + key + ".json"
                    configured = get_json(z, configured_path)
                    if not configured:
                        continue
                    built_in = {"minecraft:seagrass": "minecraft:seagrass", "minecraft:kelp": "minecraft:kelp",
                                "minecraft:sea_pickle": "minecraft:sea_pickle", "minecraft:bamboo": "minecraft:bamboo"}
                    block = built_in.get(configured.get("type"))
                    if block:
                        loot_path = "data/minecraft/loot_tables/blocks/" + block.split(":")[-1] + ".json"
                        for item in block_items(block, get_json(z, loot_path)):
                            add(item, ident, "aquatic" if block != "minecraft:bamboo" else "plant", block,
                                files=(path, placed_path, configured_path, loot_path), note="Built-in feature type names generated block")
                    for node in walk(configured):
                        if "Name" in node and isinstance(node["Name"], str) and ":" in node["Name"]:
                            block = node["Name"]
                            loot_path = "data/minecraft/loot_tables/blocks/" + block.split(":")[-1] + ".json"
                            loot = get_json(z, loot_path)
                            for item in block_items(block, loot):
                                add(item, ident, "tree" if block.endswith(("_log", "_stem", "_wood", "_hyphae")) else "aquatic" if any(x in block for x in ("coral", "kelp", "seagrass", "sea_pickle")) else "plant" if any(x in block for x in ("flower", "grass", "bush", "fungus", "mushroom", "fern", "vine", "leaves", "sapling")) else "environmental", block, files=(path, placed_path, configured_path, loot_path) if loot else (path, placed_path, configured_path), note="Generated block or its block-loot item")
                        for key2 in ("feature", "default", "features"):
                            value = node.get(key2)
                            if isinstance(value, str) and value.startswith("minecraft:"):
                                candidate = value.split(":")[-1]
                                if "data/minecraft/worldgen/configured_feature/" + candidate + ".json" in zpaths:
                                    pending.append(candidate)

    # Resolve all available biome tags. Missing Forge base tags stay unresolved.
    tag_defs = {}
    for path in zpaths:
        if path.startswith("data/minecraft/tags/worldgen/biome/") and path.endswith(".json"):
            tag_defs["minecraft:" + path.split("biome/", 1)[1][:-5]] = get_json(z, path).get("values", [])
    for mod in ("minecraft", "forge", "biomesoplenty"):
        root = bop_data / mod / "tags/worldgen/biome"
        if root.exists():
            for path in root.rglob("*.json"):
                ident = mod + ":" + str(path.relative_to(root))[:-5]
                data = json.loads(path.read_text())
                if data.get("replace"):
                    tag_defs[ident] = []
                tag_defs.setdefault(ident, []).extend(data.get("values", []))

    def tag_members(tag, seen=None):
        seen = seen or set()
        if tag in seen:
            return set()
        seen.add(tag)
        values = set()
        for value in tag_defs.get(tag, []):
            if isinstance(value, dict):
                value = value.get("id", "")
            if value.startswith("#"):
                values.update(tag_members(value[1:], seen.copy()))
            elif value in biomes:
                values.add(value)
        return values

    for tag in tag_defs:
        for biome in tag_members(tag):
            biomes[biome]["biome_tags"].append(tag)

    # BOP biome registrations and Java feature calls.
    mod_biomes = read(bop_java / "init/ModBiomes.java")
    biome_sources = {}
    for match in re.finditer(r"register\(context, BOPBiomes\.([A-Z0-9_]+), BOP(Overworld|Nether|End)Biomes\.([a-zA-Z0-9_]+)\([^;]+?\)\);", mod_biomes):
        name, dim, method = match.group(1), match.group(2), match.group(3)
        ident = "biomesoplenty:" + name.lower()
        biome_sources[ident] = (dim.lower(), method, match.group(0))
        biomes[ident] = {"id": ident, "display_name": label(ident), "source": "biomesoplenty", "dimension": dim.lower(),
                         "biome_tags": [], "materials": [], "mobs": [], "source_files": ["src/main/java/biomesoplenty/init/ModBiomes.java"]}
    for tag in tag_defs:
        for biome in tag_members(tag):
            if tag not in biomes[biome]["biome_tags"]:
                biomes[biome]["biome_tags"].append(tag)

    place_dir = bop_java / "common/worldgen/placement"
    feature_dir = bop_java / "common/worldgen/feature"
    placement_entries = {}
    configured_entries = {}
    holder_refs = {}
    for path in place_dir.glob("*.java"):
        source = read(path)
        cls = path.stem
        placement_entries.update({(cls, key): (expr, path) for key, expr in entries(source, cls).items()})
        holder_refs[cls] = dict(re.findall(r"(?:final )?Holder<ConfiguredFeature<\?, \?>>\s+([A-Z0-9_]+)\s*=\s*configuredFeatureGetter\.getOrThrow\((\w+\.\w+)\)", source))
    for path in feature_dir.glob("*.java"):
        cls = path.stem
        source = read(path)
        configured_entries.update({(cls, key): (expr, path) for key, expr in entries(source, cls).items()})
        # A few configured features use an unqualified local ResourceKey.
        configured_entries.update({(cls, key): (expr, path) for key, expr in entries(source, "").items()})
    tree_refs = {}
    for path in feature_dir.glob("*.java"):
        tree_refs.update(dict(re.findall(r"(?:final )?Holder<PlacedFeature>\s+([A-Z0-9_]+)\s*=\s*placedFeatureGetter\.getOrThrow\((\w+\.\w+)\)", read(path))))
    java_biome_classes = {dim: read(bop_java / ("common/biome/BOP" + dim.title() + "Biomes.java")) for dim in ("overworld", "nether", "end") if (bop_java / ("common/biome/BOP" + dim.title() + "Biomes.java")).exists()}

    def method_body(source, name):
        match = re.search(r"public static Biome " + re.escape(name) + r"\([^)]*\)\s*\{", source)
        if not match:
            return ""
        depth = 1
        for index in range(match.end(), len(source)):
            depth += (source[index] == "{") - (source[index] == "}")
            if depth == 0:
                return source[match.end():index]
        return ""

    def trace_bop(biome, cls, key, seen=None, source_chain=()):
        seen = seen or set()
        token = (cls, key)
        if token in seen:
            return
        seen.add(token)
        place = placement_entries.get(token)
        if place:
            expr, path = place
            source_chain += ("src/main/java/biomesoplenty/common/worldgen/placement/" + path.name,)
            first = re.match(r"register\(context,\s*\w+\.\w+,\s*([A-Z0-9_]+)", expr)
            if first:
                ref = holder_refs.get(cls, {}).get(first.group(1))
                if ref:
                    subcls, subkey = ref.split(".")
                    trace_bop(biome, subcls, subkey, seen, source_chain)
            return
        conf = configured_entries.get(token)
        if conf:
            expr, path = conf
            source_chain += ("src/main/java/biomesoplenty/common/worldgen/feature/" + path.name,)
            blocks = set(re.findall(r"BOPBlocks\.([A-Z0-9_]+)", expr))
            if key == "PATCH_CLOVER":
                blocks.add("CLOVER")
            if "createFir()" in expr:
                blocks.update(("FIR_LOG", "FIR_LEAVES"))
            for block in blocks:
                ident = "biomesoplenty:" + block.lower()
                loot_path = bop_data / "biomesoplenty/loot_tables/blocks" / (block.lower() + ".json")
                loot = json.loads(loot_path.read_text()) if loot_path.exists() else None
                for item in block_items(ident, loot):
                    add(item, biome, "tree" if block.endswith(("_LOG", "_WOOD")) else "plant", ident,
                        files=source_chain + (("src/main/resources/data/biomesoplenty/loot_tables/blocks/" + loot_path.name,) if loot else ()), note="BOP configured feature")
            for block in set(re.findall(r"Blocks\.([A-Z0-9_]+)", expr)):
                if "BOPBlocks." in expr and block in re.findall(r"BOPBlocks\.([A-Z0-9_]+)", expr):
                    continue
                if block in {"AIR", "DIRT", "GRASS_BLOCK", "STONE", "COBBLESTONE", "WATER", "LAVA", "MUD"}:
                    continue
                add("minecraft:" + block.lower(), biome, "tree" if block.endswith("_LOG") else "plant", "minecraft:" + block.lower(), files=source_chain, note="BOP configured feature")
            for ref in set(re.findall(r"(?:BOPTreeFeatures|BOPVegetationFeatures|BOPNetherFeatures)\.([A-Z0-9_]+)", expr)):
                for subcls in ("BOPTreeFeatures", "BOPVegetationFeatures", "BOPNetherFeatures"):
                    if (subcls, ref) in configured_entries and (subcls, ref) != token:
                        trace_bop(biome, subcls, ref, seen, source_chain)
            for ref in set(re.findall(r"\b([A-Z][A-Z0-9_]+_CHECKED)\b", expr)):
                target = tree_refs.get(ref)
                if target:
                    trace_bop(biome, *target.split("."), seen, source_chain)
            for ref in set(re.findall(r"TreeFeatures\.([A-Z0-9_]+)", expr)):
                wood = next((w for w in ("OAK", "BIRCH", "SPRUCE", "JUNGLE", "ACACIA", "DARK_OAK", "CHERRY", "MANGROVE") if w in ref), None)
                if wood:
                    add("minecraft:" + wood.lower() + "_log", biome, "tree", "minecraft:" + wood.lower() + "_log", files=source_chain, note="Vanilla tree feature in BOP selector")

    for biome, (dim, method, registration) in biome_sources.items():
        source = java_biome_classes.get(dim, "")
        body = method_body(source, method)
        if not body:
            unresolved["bop_biome_methods"].add(biome)
            continue
        biome_path = "src/main/java/biomesoplenty/common/biome/BOP" + dim.title() + "Biomes.java"
        biomes[biome]["source_files"].append(biome_path)
        for cls, key in re.findall(r"(?:addFeature|biomeBuilder\.addFeature)\([^;]*?\b(BOP\w+Placements|\w+Placements)\.([A-Z0-9_]+)\)", body):
            trace_bop(biome, cls, key, source_chain=("src/main/java/biomesoplenty/init/ModBiomes.java", biome_path))
        for mob in re.findall(r"EntityType\.([A-Z0-9_]+)", body):
            biomes[biome]["mobs"].append("minecraft:" + mob.lower())

    for biome, data in biomes.items():
        for mob in set(data["mobs"]):
            if not mob.startswith("minecraft:"):
                continue
            loot_path = "data/minecraft/loot_tables/entities/" + mob.split(":", 1)[1] + ".json"
            loot = get_json(z, loot_path)
            if not loot:
                continue
            for item in {node["name"] for node in walk(loot) if node.get("type") == "minecraft:item" and "name" in node}:
                add(item, biome, "mob_drop", source_mob=mob, files=tuple(data["source_files"]) + (loot_path,),
                    note="Entity loot table; conditions still apply")

    # Alex's Mobs spawn predicates retain exact BOP registry entries.
    default_text = read(alex_java / "config/DefaultBiomes.java")
    config_text = read(alex_java / "config/BiomeConfig.java")
    spawn_defs = {}
    definition = re.compile(r"public static final SpawnBiomeData\s+(\w+)\s*=\s*new SpawnBiomeData\(\)(.*?);", re.S)
    for match in definition.finditer(default_text):
        groups = collections.defaultdict(list)
        for typ, excluded, value, group in re.findall(r"\.addBiomeEntry\(BiomeEntryType\.(\w+),\s*(true|false),\s*\"([^\"]+)\",\s*(\d+)\)", match.group(2)):
            groups[int(group)].append((typ, excluded == "true", value))
        spawn_defs[match.group(1)] = list(groups.values())
    config_map = {name: definition for name, definition in re.findall(r"Pair\.of\(\"alexsmobs:([^\"]+?)(?:_spawns)?\",\s*DefaultBiomes\.(\w+)\)", config_text)}
    alex_loot_root = alex_data / "loot_tables/entities"
    alex_mobs = set(config_map) | {p.stem for p in alex_loot_root.glob("*.json")}
    for mob in sorted(alex_mobs):
        definition = config_map.get(mob)
        groups = spawn_defs.get(definition, [])
        eligible = set()
        for group in groups:
            matches = set(biomes)
            has_unknown = False
            unsupported = False
            for typ, excluded, value in group:
                if typ == "REGISTRY_NAME":
                    subset = {value} if value in biomes else set()
                elif value in tag_defs:
                    subset = tag_members(value)
                    if value.startswith("forge:"):
                        has_unknown = True
                        subset = {x for x in subset if x.startswith("biomesoplenty:")}
                        if excluded:
                            unsupported = True
                elif value == "minecraft:is_overworld":
                    subset = {b for b in biomes if biomes[b]["dimension"] == "overworld"}
                elif value == "minecraft:is_nether":
                    subset = {b for b in biomes if biomes[b]["dimension"] == "nether"}
                elif value == "minecraft:is_end":
                    subset = {b for b in biomes if biomes[b]["dimension"] == "end"}
                else:
                    unresolved["spawn_tags"].add(value)
                    has_unknown = True
                    unsupported = True
                    continue
                if excluded:
                    matches -= subset
                else:
                    matches &= subset
            if has_unknown:
                unresolved["partial_spawn_mappings"].add(mob)
            if not unsupported:
                eligible.update(matches)
        loot_path = alex_loot_root / (mob + ".json")
        loot = json.loads(loot_path.read_text()) if loot_path.exists() else None
        drops = {node["name"] for node in walk(loot) if node.get("type") in ("minecraft:item", "item") and "name" in node} if loot else set()
        for biome in eligible:
            biomes[biome]["mobs"].append("alexsmobs:" + mob)
            for item in drops:
                add(item, biome, "mob_drop", source_mob="alexsmobs:" + mob,
                    files=("src/main/java/com/github/alexthe666/alexsmobs/config/BiomeConfig.java", "src/main/java/com/github/alexthe666/alexsmobs/config/DefaultBiomes.java", "src/main/resources/data/alexsmobs/loot_tables/entities/" + mob + ".json"), note="Entity loot table; conditions still apply")

    # Explicit interaction and shedding sources. Each token is checked in entity code.
    interactions = {
        "grizzly_bear": {"bear_fur": "tamed adult sheds fur"},
        "moose": {"moose_antler": "adult sheds antler"},
        "cachalot_whale": {"ambergris": "special whale behavior"},
        "platypus": {"platypus_egg": "lays egg block"},
        "emu": {"emu_egg": "lays egg"},
        "roadrunner": {"roadrunner_feather": "adult sheds feather"},
        "crocodile": {"crocodile_scute": "adult growth sheds scute"},
        "cockroach": {"cockroach_ootheca": "lays ootheca"},
        "komodo_dragon": {"komodo_spit": "adult produces spit"},
        "hammerhead_shark": {"shark_tooth": "tooth drops during attack"},
        "giant_squid": {"lost_tentacle": "special combat behavior"},
    }
    for mob, items in interactions.items():
        paths = list((alex_java / "entity").glob("*" + "".join(s.title() for s in mob.split("_")) + "*.java"))
        for path in paths:
            source = path.read_text()
            for item, method in items.items():
                if item.upper() not in source:
                    continue
                for biome, data in biomes.items():
                    if "alexsmobs:" + mob in data["mobs"]:
                        add("alexsmobs:" + item, biome, "mob_interaction", source_mob="alexsmobs:" + mob,
                            files=("src/main/java/com/github/alexthe666/alexsmobs/entity/" + path.name, "src/main/java/com/github/alexthe666/alexsmobs/config/DefaultBiomes.java"), note=method)

    # Collapse duplicate evidence while keeping every biome and source path.
    raw = []
    selected = []
    woods = []
    exclude_exact = {"minecraft:diamond", "minecraft:emerald", "minecraft:amethyst_shard", "minecraft:ender_pearl", "minecraft:charcoal", "minecraft:bone", "minecraft:string", "minecraft:gunpowder", "minecraft:rotten_flesh"}
    exclude_words = ("_ore", "raw_", "_ingot", "_nugget", "_stone", "granite", "diorite", "andesite", "cobblestone", "_sand", "_dirt", "_gravel", "_planks", "_stairs", "_slab", "_wall", "_bricks", "_carpet", "_block", "bucket", "spawn_egg")
    plant_words = ("flower", "grass", "fern", "bush", "vine", "mushroom", "fungus", "wart", "roots", "petals", "lily", "cattail", "lavender", "reed", "barley", "clover", "hydrangea", "iris", "toadstool", "moss", "dripleaf", "spore", "coral", "kelp", "seagrass", "sea_pickle", "bamboo", "cactus", "pumpkin", "melon", "cocoa", "podzol", "mycelium", "mud", "scute", "feather", "shell", "hide", "fur", "antler", "slime", "mucus", "tooth", "fin", "scale", "horn", "egg", "ambergris", "rabbit_foot", "ink_sac", "nautilus")
    for item in sorted(evidence):
        facts = evidence[item]
        biome_ids = sorted({x["biome_id"] for x in facts})
        if not biome_ids:
            continue
        kinds = sorted({x["acquisition_type"] for x in facts})
        paths = sorted({p for x in facts for p in x["source_files"]})
        sources = sorted({x["source_block"] for x in facts if x["source_block"]})
        mobs = sorted({x["source_mob"] for x in facts if x["source_mob"]})
        notes = sorted({x["note"] for x in facts if x["note"]})
        occurrences = [json.loads(x) for x in sorted({json.dumps(x, sort_keys=True) for x in facts})]
        namespace, name = item.split(":", 1)
        is_wood = name.endswith(("_log", "_stem", "_wood", "_hyphae", "_sapling", "_propagule"))
        if name in {"big_dripleaf_stem", "mushroom_stem"}:
            is_wood = False
        reason = None
        if item in exclude_exact or any(x in name for x in exclude_words):
            reason = "requested exclusion"
        elif is_wood:
            reason = "separate wood catalogue"
        elif namespace == "alexsmobs" and name in {"capsid", "novelty_hat", "skelewag_sword", "straddlite"}:
            reason = "not a raw exploration material"
        elif not (any(x in name for x in plant_words) or namespace == "alexsmobs"):
            reason = "generic or unclassified block"
        elif len(biome_ids) > 20:
            reason = "available in many biomes"
        family_tags = sorted(set.intersection(*(set(biomes[x]["biome_tags"]) for x in biome_ids))) if biome_ids else []
        family_tags = [x for x in family_tags if x.startswith(("minecraft:is_", "forge:is_")) and x not in {"minecraft:is_overworld", "minecraft:is_nether", "minecraft:is_end"}]
        family_scope = "one biome" if len(biome_ids) == 1 else "one source-tagged family" if family_tags else "many or unclassified families"
        record = {"item_id": item, "display_name": label(item), "source_mod": "vanilla" if namespace == "minecraft" else namespace,
                  "acquisition_type": "tree" if is_wood else "aquatic" if "aquatic" in kinds else "plant" if "plant" in kinds else "mob_interaction" if "mob_interaction" in kinds else "mob_drop" if "mob_drop" in kinds else "environmental",
                  "biome_ids": biome_ids, "eligible_biome_count": len(biome_ids), "biome_family_scope": family_scope,
                  "shared_family_tags": family_tags,
                  "easy_outside_biomes": True if is_wood or name in {"bamboo", "cactus", "kelp", "pumpkin", "melon", "red_mushroom", "brown_mushroom"} else None,
                  "renewable_after_discovery": True if "mob_drop" in kinds or "mob_interaction" in kinds else bool(is_wood or any(x in name for x in ("flower", "grass", "mushroom", "fungus", "vine", "bamboo", "kelp", "cactus", "pumpkin", "melon", "egg", "fur", "antler"))),
                  "source_blocks": sources, "source_mobs": mobs, "acquisition_notes": notes,
                  "source_files": paths, "occurrences": occurrences, "excluded_reason": reason}
        record["rarity"] = "common" if len(biome_ids) > 8 and record["acquisition_type"] not in ("mob_drop", "mob_interaction") else "rare" if len(biome_ids) <= 2 or record["acquisition_type"] == "mob_interaction" else "uncommon"
        raw.append(record)
        if is_wood:
            woods.append({k: v for k, v in record.items() if k != "excluded_reason"})
        elif not reason:
            selected.append({k: v for k, v in record.items() if k != "excluded_reason"})
            for biome in biome_ids:
                biomes[biome]["materials"].append(item)

    for biome in biomes.values():
        for key in ("biome_tags", "materials", "mobs", "source_files"):
            biome[key] = sorted(set(biome[key]))
    dump(out / "biomes.json", {"metadata": meta, "biomes": [biomes[x] for x in sorted(biomes)]})
    dump(out / "materials.json", {"metadata": meta, "materials": selected})
    wood_types = {}
    bop_block_file = bop_java / "api/block/BOPBlocks.java"
    bop_blocks = read(bop_block_file)
    for record in woods:
        item = record["item_id"]
        namespace, name = item.split(":", 1)
        base = re.sub(r"_(log|stem|wood|hyphae|sapling|propagule)$", "", name)
        group = wood_types.setdefault(namespace + ":" + base, {"wood_id": namespace + ":" + base,
            "display_name": label(namespace + ":" + base), "biome_ids": set(), "logs": set(), "saplings": set(), "planks_item_id": None,
            "source_files": set()})
        group["biome_ids"].update(record["biome_ids"])
        group["source_files"].update(record["source_files"])
        group["saplings" if name.endswith(("sapling", "propagule")) else "logs"].add(item)
        planks = namespace + ":" + base + "_planks"
        if namespace == "minecraft" and "data/minecraft/loot_tables/blocks/" + base + "_planks.json" in zpaths:
            group["planks_item_id"] = planks
        elif namespace == "biomesoplenty" and re.search(r"\b" + base.upper() + r"_PLANKS\b", bop_blocks):
            group["planks_item_id"] = planks
            group["source_files"].add("src/main/java/biomesoplenty/api/block/BOPBlocks.java")
    grouped = []
    for group in wood_types.values():
        for key in ("biome_ids", "logs", "saplings", "source_files"):
            group[key] = sorted(group[key])
        grouped.append(group)
    dump(out / "woods.json", {"metadata": meta, "wood_types": sorted(grouped, key=lambda x: x["wood_id"]), "woods": woods})
    dump(out / "raw_extraction.json", {"metadata": meta, "materials": raw, "unresolved": {k: sorted(v) for k, v in unresolved.items()}})
    print(json.dumps({"biomes": len(biomes), "materials": len(selected), "woods": len(woods), "raw": len(raw), "unresolved": {k: len(v) for k, v in unresolved.items()}}, indent=2))


if __name__ == "__main__":
    main()
