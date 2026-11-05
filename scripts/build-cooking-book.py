"""Build the cross-referenced cooking book from the cooking design documents.

The recipe catalogue, intermediate products, sources, stations, and
techniques documents are the source of truth. This script regenerates
.workflow/cooking/book/ with one page per dish, product, raw ingredient,
station, and technique, linked to each other.
"""

from collections import defaultdict
from pathlib import Path
import re
import shutil
import sys
import unicodedata


ROOT = Path(__file__).resolve().parents[1]
COOKING = ROOT / ".workflow" / "cooking"
BOOK = COOKING / "book"
FOLDERS = {
    "dish": "dishes",
    "product": "products",
    "ingredient": "ingredients",
    "station": "stations",
    "technique": "techniques",
}
STEP = re.compile(r"^(?P<station>[A-ZÆ][^:()]*?)(?: \((?P<heat>[^)]*)\))?: (?P<text>.+)$")
ITALIC = re.compile(r"\*([^*]+)\*")
errors = []


def slug(name):
    name = name.replace("æ", "ae").replace("Æ", "Ae").replace("ø", "o").replace("ß", "ss")
    name = unicodedata.normalize("NFKD", name).encode("ascii", "ignore").decode()
    return re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")


def tables(path):
    """Yield (h2, h3, intro, header, rows) for every table in a document."""
    lines = path.read_text().splitlines()
    h2 = h3 = ""
    intro = []
    i = 0
    while i < len(lines):
        line = lines[i]
        if line.startswith("## "):
            h2, h3, intro = line[3:].strip(), "", []
        elif line.startswith("### "):
            h3 = line[4:].strip()
        elif line.startswith("| ") and i + 1 < len(lines) and lines[i + 1].startswith("|---"):
            header = [cell.strip() for cell in line.strip("|").split("|")]
            rows = []
            i += 2
            while i < len(lines) and lines[i].startswith("| "):
                rows.append([cell.strip() for cell in lines[i].strip("|").split("|")])
                i += 1
            yield h2, h3, " ".join(intro), header, rows
            continue
        elif line.strip():
            intro.append(line.strip())
        i += 1


def items(cell):
    """Split an ingredient cell into (display part, [(name, is_product)])."""
    parts = []
    for part in cell.split(","):
        part = part.strip()
        names = []
        for alternative in re.split(r"\s+or\s+", part):
            alternative = re.sub(r"^or\s+", "", alternative.strip())
            match = re.fullmatch(r"\*(.+)\*", alternative)
            if match:
                names.append((match.group(1), True))
            elif alternative:
                names.append((alternative, False))
        parts.append((part, names))
    return parts


# Source documents.

dishes = {}
for h2, h3, _, header, rows in tables(COOKING / "recipe-catalogue.md"):
    if header[:3] != ["Dish", "Ingredients", "Processes"]:
        continue
    for name, ingredients, processes in rows:
        dishes[name] = {"cuisine": h2, "category": h3, "recipes": [(ingredients, processes)]}

products = {}
for h2, h3, intro, header, rows in tables(COOKING / "intermediates.md"):
    if header[:3] != ["Product", "Ingredients", "Processes"]:
        continue
    feature = re.search(r"Feature: (.+?)\.(?:\s|$)", intro)
    for names, ingredients, processes in rows:
        for name in [n.strip() for n in names.split(",")]:
            entry = products.setdefault(name, {"area": h2, "feature": feature.group(1) if feature else "", "recipes": []})
            entry["recipes"].append((ingredients, processes))

sources = {}
for h2, _, _, header, rows in tables(COOKING / "sources.md"):
    if header[:4] != ["Raw ingredient", "Where", "How", "Feature"]:
        continue
    for name, where, how, feature in rows:
        sources[name] = {"kind": h2, "where": where, "how": how, "feature": feature}

stations = {}
for h2, h3, _, header, rows in tables(COOKING / "stations.md"):
    if not header or header[0] != "Station":
        continue
    for row in rows:
        info = dict(zip(header, row))
        stations[info["Station"]] = {
            "group": h3 or h2,
            "kind": info.get("Kind", "Time" if "Time" in h3 else ""),
            "heat": info.get("Heat") or info.get("Heat used", ""),
            "supports": info.get("Techniques") or info.get("Supports", ""),
            "feature": info.get("Feature", ""),
        }

techniques = {}
for h2, _, _, header, rows in tables(COOKING / "techniques.md"):
    if header[:3] != ["Technique", "Matches", "Explanation"]:
        continue
    for name, matches, explanation in rows:
        techniques[name] = {
            "group": h2,
            "matches": [m.strip() for m in matches.split(",")],
            "explanation": explanation,
        }
PATTERNS = sorted(
    ((pattern, name) for name, t in techniques.items() for pattern in t["matches"]),
    key=lambda p: -len(p[0]),
)


# Pages and links.

pages = {}


def register(kind, name):
    path = f"{FOLDERS[kind]}/{slug(name)}.md"
    if path in pages.values():
        errors.append(f"Two {kind} pages share the file {path}")
    pages[(kind, name)] = path


for kind, entries in (("dish", dishes), ("product", products), ("ingredient", sources),
                      ("station", stations), ("technique", techniques)):
    for name in entries:
        register(kind, name)


def link(kind, name, label=None, here="sub"):
    path = pages[(kind, name)]
    prefix = "../" if here == "sub" else ""
    return f"[{label or name}]({prefix}{path})"


def link_item(name, is_product, here="sub"):
    if is_product:
        if name in products:
            return link("product", name, here=here)
        errors.append(f"No definition for product *{name}*")
        return name
    if name in sources:
        return link("ingredient", name, here=here)
    errors.append(f"No source for raw ingredient {name}")
    return name


def render_ingredients(cell):
    rendered = []
    for part, names in items(cell):
        text = part
        for name, is_product in names:
            target = f"*{name}*" if is_product else name
            text = text.replace(target, link_item(name, is_product), 1)
        rendered.append(text)
    return ", ".join(rendered)


def render_text(text, used_techniques):
    """Link products in italics and the techniques in a step's first sentence."""
    first, sep, rest = text.partition(". ")
    segments = re.split(r"(\*[^*]+\*)", first)
    linked = set()
    out = []
    for segment in segments:
        match = re.fullmatch(r"\*([^*]+)\*", segment)
        if match:
            out.append(link_item(match.group(1), True))
            continue
        spans = []
        for pattern, name in PATTERNS:
            if name in linked:
                continue
            regex = re.compile(r"(?<![\w-])" + re.escape(pattern) + r"(?!\w)", re.IGNORECASE)
            for found in regex.finditer(segment):
                if all(found.end() <= start or found.start() >= end for start, end, _ in spans):
                    spans.append((found.start(), found.end(), name))
                    linked.add(name)
                    used_techniques.add(name)
                    break
        for start, end, name in sorted(spans, reverse=True):
            segment = segment[:start] + link("technique", name, segment[start:end]) + segment[end:]
        out.append(segment)
    rest_text = ITALIC.sub(lambda m: link_item(m.group(1), True), rest)
    return "".join(out) + sep + rest_text


station_uses = defaultdict(list)
technique_uses = defaultdict(list)
used_in = defaultdict(set)


def render_steps(owner_kind, owner, processes):
    lines = []
    for number, step in enumerate([s.strip() for s in processes.split("→")], 1):
        match = STEP.match(step)
        used = set()
        if match and match.group("station") in stations:
            station = match.group("station")
            heat = f" ({match.group('heat')})" if match.group("heat") else ""
            text = render_text(match.group("text"), used)
            lines.append(f"{number}. {link('station', station)}{heat}: {text}")
            station_uses[station].append((owner_kind, owner, match.group("heat") or "", text))
        elif match and match.group("station") == "Proposal":
            lines.append(f"{number}. Proposal: {render_text(match.group('text'), used)}")
        else:
            if match:
                errors.append(f"Unknown station {match.group('station')} in {owner}")
            lines.append(f"{number}. {render_text(step, used)}")
        for name in used:
            technique_uses[name].append((owner_kind, owner))
    return lines


def recipe_section(owner_kind, owner, recipes):
    out = []
    for index, (ingredients, processes) in enumerate(recipes, 1):
        if len(recipes) > 1:
            out += [f"### Way {index}", ""]
        for _, names in items(ingredients):
            for name, is_product in names:
                used_in[("product" if is_product else "ingredient", name)].add((owner_kind, owner))
        out += ["Ingredients: " + render_ingredients(ingredients), ""]
        out += render_steps(owner_kind, owner, processes) + [""]
    return out


def used_in_section(kind, name):
    users = sorted(used_in.get((kind, name), ()), key=lambda u: (u[0] != "dish", u[1].lower()))
    if not users:
        return ["## Used in", "", "No recipe uses it yet.", ""]
    return ["## Used in", ""] + [f"- {link(k, n)}{' (dish)' if k == 'dish' else ''}" for k, n in users] + [""]


# Build pages.

files = {}

for name, dish in dishes.items():
    body = [f"# {name}", "", f"{dish['cuisine']} · {dish['category']}. Defined in the [recipe catalogue](../../recipe-catalogue.md).", ""]
    body += ["## Recipe", ""] + recipe_section("dish", name, dish["recipes"])
    files[pages[("dish", name)]] = body

for name, product in products.items():
    feature = f" Feature: {product['feature']}." if product["feature"] else ""
    body = [f"# {name}", "", f"{product['area']}.{feature} Defined in the [intermediate products](../../intermediates.md).", ""]
    body += ["## How it is made", ""] + recipe_section("product", name, product["recipes"])
    files[pages[("product", name)]] = body

for name, product in products.items():
    files[pages[("product", name)]] += used_in_section("product", name)

for name, source in sources.items():
    body = [f"# {name}", "", f"{source['kind']}. Defined in the [sources](../../sources.md).", ""]
    body += ["## Where it comes from", "", f"- **Where:** {source['where']}", f"- **How:** {source['how']}",
             f"- **Feature:** {source['feature']}", ""]
    files[pages[("ingredient", name)]] = body + used_in_section("ingredient", name)

for name, station in stations.items():
    body = [f"# {name}", "", f"{station['group']}. Defined in the [station list](../../stations.md).", ""]
    for label, key in (("Kind", "kind"), ("Heat", "heat"), ("Supports", "supports"), ("Feature", "feature")):
        if station[key]:
            body.append(f"- **{label}:** {station[key]}")
    uses = sorted(station_uses[name], key=lambda u: (u[0] != "dish", u[1].lower()))
    body += ["", f"## Steps at this station ({len(uses)})", ""]
    body += [f"- {link(k, n)}{f' ({h})' if h else ''}: {t}" for k, n, h, t in uses] or ["No step uses it yet."]
    files[pages[("station", name)]] = body + [""]

for name, technique in techniques.items():
    uses = sorted(set(technique_uses[name]), key=lambda u: (u[0] != "dish", u[1].lower()))
    body = [f"# {name}", "", technique["explanation"], "",
            f"{technique['group']}. Defined in the [techniques](../../techniques.md).", "",
            f"## Used in ({len(uses)})", ""]
    body += [f"- {link(k, n)}{' (dish)' if k == 'dish' else ''}" for k, n in uses] or ["No step uses it yet."]
    files[pages[("technique", name)]] = body + [""]


def grouped(entries, key, kind):
    groups = defaultdict(list)
    for name, entry in entries.items():
        groups[key(entry)].append(name)
    out = []
    for group, names in groups.items():
        out += [f"### {group}", "", ", ".join(link(kind, n, here="root") for n in sorted(names, key=str.lower)), ""]
    return out


readme = [
    "# Cooking book",
    "",
    "Every dish, intermediate product, raw ingredient, station, and technique in cabin cooking has its own page here. "
    "Each ingredient, station, and technique links to its page, and each page lists where it is used.",
    "",
    "This book is generated by `scripts/build-cooking-book.py` from the [recipe catalogue](../recipe-catalogue.md), "
    "[intermediate products](../intermediates.md), [sources](../sources.md), [stations](../stations.md), and "
    "[techniques](../techniques.md). Edit those documents, then rerun the script. Do not edit the pages by hand.",
    "",
    f"It has {len(dishes)} dishes, {len(products)} intermediate products, {len(sources)} raw ingredients, "
    f"{len(stations)} stations, and {len(techniques)} techniques.",
    "",
    "## Dishes",
    "",
]
cuisines = defaultdict(lambda: defaultdict(list))
for name, dish in dishes.items():
    cuisines[dish["cuisine"]][dish["category"]].append(name)
for cuisine, categories in cuisines.items():
    readme += [f"### {cuisine}", ""]
    for category, names in categories.items():
        readme += [f"- **{category}:** " + ", ".join(link("dish", n, here="root") for n in names)]
    readme += [""]
readme += ["## Intermediate products", ""] + grouped(products, lambda e: e["area"], "product")
readme += ["## Raw ingredients", ""] + grouped(sources, lambda e: e["kind"], "ingredient")
readme += ["## Stations", ""] + grouped(stations, lambda e: e["group"], "station")
readme += ["## Techniques", ""] + grouped(techniques, lambda e: e["group"], "technique")
files["README.md"] = readme

unused = sorted(n for n in techniques if not technique_uses[n])
if unused:
    errors.append("Techniques no step matches: " + ", ".join(unused))

if errors:
    print("\n".join(sorted(set(errors))), file=sys.stderr)
    sys.exit(1)

# Write a complete new book beside the old one, then swap, so a failed run keeps the last good book.
staging = BOOK.with_name(BOOK.name + ".new")
if staging.exists():
    shutil.rmtree(staging)
for folder in FOLDERS.values():
    (staging / folder).mkdir(parents=True)
for path, body in files.items():
    (staging / path).write_text("\n".join(body).rstrip("\n") + "\n")
if BOOK.exists():
    shutil.rmtree(BOOK)
staging.rename(BOOK)
print(f"Wrote {len(files)} pages to {BOOK.relative_to(ROOT)}")
