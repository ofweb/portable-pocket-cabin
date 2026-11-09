"""Lists the intermediates and dishes a B-0050 delivery completes.

Run from the repository root with the delivery's raw ingredient names, comma separated:
    python3 .workflow/cooking/tools/completion_check.py "barley,rye,corn"
It treats vanilla ingredients from the sources and every item the mod already has as available, adds the given
ingredients, and prints what newly becomes makeable at existing stations, apart from what waits for B-0049.

With --growable, it lists instead the raw ingredients that the sources say are farmed, foraged, fished, or trapped but
that no plant, shellfish bed, trap, or fish in the mod gives, and exits non-zero if there are any (B-0054).
"""
import re, json, os, sys
ROOT='.'
W=ROOT+'/.workflow/cooking/'
def rows(path, start=None, stop=None):
    out=[]; section=None; active=start is None
    for line in open(path):
        if line.startswith('## '):
            section=line[3:].strip()
            if start and section==start: active=True
            if stop and section==stop: active=False
        if not active or not line.startswith('| ') or line.startswith('|---') or line.startswith('| Product') or line.startswith('| Dish'): continue
        cells=[c.strip() for c in line.strip().strip('|').split('|')]
        if len(cells)>=3: out.append((section,cells[0],cells[1],cells[2]))
    return out
inter=rows(W+'intermediates.md', None, 'Raw ingredients across all chains')
dishes=[r for r in rows(W+'recipe-catalogue.md', 'French', 'Intermediate products')]
def norm(s): return re.sub(r'\s+',' ',s.replace('*','').strip().lower())
def parse_ingredients(text):
    text=text.split('. ')[0]
    groups=[]
    for part in re.split(r',\s*', text):
        # "X, or Y" continues the list before it as an alternative, and "a calf stomach from B-0049" names the calf stomach.
        alternative=part.strip().startswith('or ')
        part=re.sub(r'^or\s+','',part.strip())
        alts=[re.sub(r'\s+from\s+b-\d+$','',re.sub(r'^an?\s+','',norm(a))) for a in re.split(r'\s+or\s+', part) if norm(a)]
        if not alts: continue
        if alternative and groups: groups[-1].extend(alts)
        else: groups.append(alts)
    return groups
def stations(proc):
    names=set()
    for step in re.split(r'→', proc):
        m=re.match(r'\s*([^:(]+?)\s*(\([^)]*\))?\s*:', step)
        if m: names.add(m.group(1).strip().lower())
    return names
HAVE_STATIONS={'cutting board','skillet','cooking pot','oven','prep counter','strainer','grill','wok','æbleskive pan','press','mill','mixing bowl',
 'mortar and pestle','butter churn','cheese press','proofing basket','dairy pan','fermentation crock','ageing shelf','drying rack','brine barrel','smoker',
 'stove','campfire','hanging hook','butcher block','meat grinder','sausage stuffer'}
# available raw names: vanilla ingredients from sources "Already in Minecraft"
vanilla=set()
sec=None
for line in open(W+'sources.md'):
    if line.startswith('## '): sec=line[3:].strip()
    if sec=='Already in Minecraft' and line.startswith('| ') and not line.startswith('| Raw') and not line.startswith('|---'):
        vanilla.add(norm(line.split('|')[1]))
vanilla|={'water','water bottle','milk','sugar','wheat','sweet berries','egg','honey','apple','salt','ice','snow'}
mod=set()
for f in os.listdir(ROOT+'/src/main/resources/assets/portable_pocket_cabin/items'):
    mod.add(f[:-5].replace('_',' '))
lang=json.load(open(ROOT+'/src/main/resources/assets/portable_pocket_cabin/lang/en_us.json'))
for k,v in lang.items():
    if k.startswith('item.portable_pocket_cabin.') or k.startswith('block.portable_pocket_cabin.'): mod.add(norm(v))
# Catalogue names the mod already covers under another name: vanilla bread, the corn tortilla item, any of the jams,
# vanilla dried kelp as kombu, the skimmed milk bucket, and sunflower oil as the frying oil.
mod |= {'bread', 'corn tortillas', 'jam', 'kombu', 'skimmed milk', 'vegetable oil'}
# Dishes the mod has under another name: the quesadilla, the pretzel, and apple juice.
mod |= {'quesadillas', 'brezeln', 'æblemost'}
# The catalogue's rennet is either of the mod's rennets, vegetable or calf rennet.
mod |= {'pork loin with rind', 'rennet'}
# The catalogue lists a carcass's parts together in one row. The mod gives each part as its own item.
mod |= {'beef chuck, beef round, beef sirloin, beef heart, calf stomach, beef bones, suet', 'chicken thighs, chicken drumsticks, chicken bones',
        'pork shoulder, pork belly, pork loin with rind, pig trotters, pork bones, back fat, pork casing'}
def solve(avail):
    avail=set(avail); made=set(); changed=True
    def ok(groups, proc):
        return all(any(a in avail for a in g) for g in groups) and stations(proc)<=HAVE_STATIONS
    while changed:
        changed=False
        for sec,prod,ing,proc in inter:
            p=norm(prod)
            if p in avail: continue
            if ok(parse_ingredients(ing), proc): avail.add(p); made.add(p); changed=True
    ds=[d for d in dishes if ok(parse_ingredients(d[2]), d[3])]
    return avail, made, ds
def growable():
    """B-0054: the raw ingredients the sources grow, farm, forage, fish, or trap that no plant, bed, trap, or fish gives."""
    java=open(ROOT+'/src/main/java/dev/portablepocketcabin/CookingPlants.java').read()
    consts=dict(re.findall(r'static final Item (\w+) = (?:ingredient|registerItem)\("(\w+)"', java))
    consts.update({'APPLE':'apple'})
    names=set()
    def item(token):
        token=token.strip()
        m=re.search(r'(?:Items\.)?([A-Z_]+)$', token)
        return consts.get(m.group(1), m.group(1).lower()) if m else None
    for kind,args in re.findall(r'\b(fieldCrop|paddyCrop|perennialCrop|trellisCrop|cuttingBush|selfBush|bush|fruitTree|seaPlant|shellfish)\(([^;]*?)\)(?:;|,\s*List)', java):
        strings=re.findall(r'"(\w+)"', args); suppliers=re.findall(r'\(\) -> ([\w.]+)', args)
        if kind in ('fieldCrop','paddyCrop','perennialCrop') and len(strings)>=2: names.update(strings[:2])
        elif kind in ('cuttingBush','selfBush','seaPlant','shellfish') and len(strings)>=2: names.add(strings[1])
        for sup in suppliers: names.add(item(sup))
    for target in re.findall(r'GIVES\.put\("\w+", List\.of\(([^)]*)\)\)', java):
        for token in target.split(','):
            if token.strip() and '(' not in token: names.add(item(token))
    # Crops registered from arrays of {plant, seed} pairs, herbs from a list, and fruit listed by fruit trees.
    for pair in re.findall(r'\{"(\w+)", "(\w+)"\}', java): names.update(pair)
    for listed in re.findall(r'new String\[\] \{([^}]*)\}', java): names.update(re.findall(r'"(\w+)"', listed))
    for listed in re.findall(r'List\.of\(([A-Z_, ]+)\)', java): names.update(item(t) for t in listed.split(','))
    if 'green_olive' in names: names.add('olive')
    names |= {'rosehip', 'prawns', 'cardoon', 'cardoon_flower', 'shaded_tea_leaf', 'tea_leaf', 'palm_sap', 'cinnamon_bark'}
    for f in ['gameplay/fishing/fish']:
        names |= set(re.findall(r'portable_pocket_cabin:(\w+)', open(ROOT+'/src/main/resources/data/minecraft/loot_table/'+f+'.json').read()))
    shown={norm(lang.get('item.portable_pocket_cabin.'+n, lang.get('block.portable_pocket_cabin.'+n, n.replace('_',' ')))) for n in names if n}
    shown |= {norm(n.replace('_',' ')) for n in names if n}
    missing=[]
    for line in open(W+'sources.md'):
        if not line.startswith('| ') or line.startswith('| Raw') or line.startswith('|---'): continue
        cells=[c.strip() for c in line.strip().strip('|').split('|')]
        if len(cells)<3 or cells[2].lower().startswith('vanilla'): continue
        if not re.search(r'farm|tree|forage|fish|trap|gather', cells[2], re.I): continue
        if norm(cells[0]) not in shown: missing.append(cells[0])
    print("not grown, foraged, fished, or trapped from anything in the mod:", missing)
    sys.exit(1 if missing else 0)
if len(sys.argv)>1 and sys.argv[1]=='--growable': growable()
PLANTS_D1=sys.argv[1].split(',') if len(sys.argv)>1 else []
base=vanilla|mod
before_avail, before_made, before_dishes=solve(base - set(PLANTS_D1))
after_avail, after_made, after_dishes=solve(base | set(PLANTS_D1))
new_inter=sorted(after_made - before_made - mod)
new_dishes=[d[1] for d in after_dishes if d not in before_dishes]
print("already makeable but not in mod:", sorted(before_made - mod))
print("NEW intermediates:", len(new_inter)); print(new_inter)
print("NEW dishes:", len(new_dishes)); print(new_dishes)
