"""Shared helpers that write cooking resources: models, block states, loot tables, recipes, tags, and names. Run from the repository root."""
import json, os
R='src/main/resources'; A=R+'/assets/portable_pocket_cabin'; D=R+'/data/portable_pocket_cabin'
def w(p,o):
    os.makedirs(os.path.dirname(p),exist_ok=True); open(p,'w').write(json.dumps(o,indent=2,ensure_ascii=False)+'\n')
def faces(t): return {f:{"texture":t} for f in ["north","south","east","west","up","down"]}
def openbox(x0,z0,x1,z1,h,side,inner=None,extra=()):
    inner=inner or side
    els=[{"from":[x0,0,z0],"to":[x1,1,z1],"faces":faces("#inner")},
         {"from":[x0,0,z0],"to":[x1,h,z0+1],"faces":faces("#side")},
         {"from":[x0,0,z1-1],"to":[x1,h,z1],"faces":faces("#side")},
         {"from":[x0,0,z0+1],"to":[x0+1,h,z1-1],"faces":faces("#side")},
         {"from":[x1-1,0,z0+1],"to":[x1,h,z1-1],"faces":faces("#side")}]+list(extra)
    return {"parent":"minecraft:block/block","textures":{"particle":side,"side":side,"inner":inner},"elements":els}
def block_model(name, model, facing=True):
    w(f'{A}/models/block/{name}.json', model)
    if facing:
        w(f'{A}/blockstates/{name}.json',{"variants":{f"facing={f}":({"model":f"portable_pocket_cabin:block/{name}"}|({"y":y} if y else {})) for f,y in [("north",0),("east",90),("south",180),("west",270)]}})
    else:
        w(f'{A}/blockstates/{name}.json',{"variants":{"":{"model":f"portable_pocket_cabin:block/{name}"}}})
    w(f'{A}/items/{name}.json',{"model":{"type":"minecraft:model","model":f"portable_pocket_cabin:block/{name}"}})
    w(f'{D}/loot_table/blocks/{name}.json',{"type":"minecraft:block","pools":[{"rolls":1.0,"conditions":[{"condition":"minecraft:survives_explosion"}],"entries":[{"type":"minecraft:item","name":f"portable_pocket_cabin:{name}"}]}],"random_sequence":f"portable_pocket_cabin:blocks/{name}"})
def item_model(name, tex):
    w(f'{A}/models/item/{name}.json',{"parent":"minecraft:item/generated","textures":{"layer0":tex}})
    w(f'{A}/items/{name}.json',{"model":{"type":"minecraft:model","model":f"portable_pocket_cabin:item/{name}"}})
def tag(tool, names):
    p=f'{R}/data/minecraft/tags/block/mineable/{tool}.json'; t=json.load(open(p))
    for n in names:
        if f'portable_pocket_cabin:{n}' not in t['values']: t['values'].append(f'portable_pocket_cabin:{n}')
    w(p,t)
def shaped(name, pattern, key, result=None, count=1):
    w(f'{D}/recipe/{name}.json',{"type":"minecraft:crafting_shaped","category":"misc","key":key,"pattern":pattern,"result":{"id":f"portable_pocket_cabin:{result or name}"} | ({"count":count} if count>1 else {})})
def lang(entries):
    p=A+'/lang/en_us.json'; l=json.load(open(p)); l.update(entries); w(p,l)
def recipe(path, o): w(f'{D}/recipe/cooking/{path}.json', o)
WATER={"fabric:type":"fabric:components","base":"minecraft:potion","components":{"minecraft:potion_contents":{"potion":"minecraft:water"}}}
