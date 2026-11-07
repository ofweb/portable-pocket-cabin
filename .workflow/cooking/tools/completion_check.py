"""Lists the intermediates and dishes a B-0050 delivery completes.

Run from the repository root with the delivery's raw ingredient names, comma separated:
    python3 .workflow/cooking/tools/completion_check.py "barley,rye,corn"
It treats vanilla ingredients from the sources and every item the mod already has as available, adds the given
ingredients, and prints what newly becomes makeable at existing stations, apart from what waits for B-0049.
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
        alts=[norm(a) for a in re.split(r'\s+or\s+', part) if norm(a)]
        if alts: groups.append(alts)
    return groups
def stations(proc):
    names=set()
    for step in re.split(r'→', proc):
        m=re.match(r'\s*([^:(]+?)\s*(\([^)]*\))?\s*:', step)
        if m: names.add(m.group(1).strip().lower())
    return names
HAVE_STATIONS={'cutting board','skillet','cooking pot','oven','prep counter','strainer','grill','wok','æbleskive pan','press','mill','mixing bowl',
 'mortar and pestle','butter churn','cheese press','proofing basket','dairy pan','fermentation crock','ageing shelf','drying rack','brine barrel','smoker',
 'stove','campfire'}
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
PLANTS_D1=sys.argv[1].split(',') if len(sys.argv)>1 else []
base=vanilla|mod
before_avail, before_made, before_dishes=solve(base - set(PLANTS_D1))
after_avail, after_made, after_dishes=solve(base | set(PLANTS_D1))
new_inter=sorted(after_made - before_made - mod)
new_dishes=[d[1] for d in after_dishes if d not in before_dishes]
print("already makeable but not in mod:", sorted(before_made - mod))
print("NEW intermediates:", len(new_inter)); print(new_inter)
print("NEW dishes:", len(new_dishes)); print(new_dishes)
