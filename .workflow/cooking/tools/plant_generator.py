exec(open('.workflow/cooking/tools/cooking_resources.py').read())
"""Generator helpers for B-0050 plants: field crops, trellis vines, bushes, fruit trees, wild placement, trades, and recipes. A delivery script execs this file, calls the helpers, and finishes with lang(NAMES)."""
import json as _j
P="portable_pocket_cabin:"; SUGAR="#portable_pocket_cabin:sugars"; OIL="#portable_pocket_cabin:cooking_oils"
NAMES={}
STYLE={"wheat":[f"minecraft:block/wheat_stage{i}" for i in range(8)],
       "carrots":[f"minecraft:block/carrots_stage{min(3,i//2)}" for i in range(8)],
       "potatoes":[f"minecraft:block/potatoes_stage{min(3,i//2)}" for i in range(8)],
       "beetroots":[f"minecraft:block/beetroots_stage{min(3,i//2)}" for i in range(8)]}
def tinted(name, tex, color):
    w(f'{A}/models/item/{name}.json',{"parent":"minecraft:item/generated","textures":{"layer0":tex}})
    w(f'{A}/items/{name}.json',{"model":{"type":"minecraft:model","model":f"{P}item/{name}","tints":[{"type":"minecraft:constant","value":color}]}})
def bottle(name, color):
    w(f'{A}/models/item/{name}.json',{"parent":"minecraft:item/generated","textures":{"layer0":"minecraft:item/potion_overlay","layer1":"minecraft:item/potion"}})
    w(f'{A}/items/{name}.json',{"model":{"type":"minecraft:model","model":f"{P}item/{name}","tints":[{"type":"minecraft:constant","value":color}]}})
def needs(plant, soil=("minecraft:farmland",), light=9, water="nearby", support="none", chance=0.25):
    w(f'{D}/cooking_plant/{plant}.json',{"soil":list(soil),"light":light,"water":water,"support":support,"growth_chance":chance,"shade":False})
def wild(name, biomes, age="7", tree=False, sand=False):
    w(f'{D}/tags/worldgen/biome/grows_wild/{name}.json',{"replace":False,"values":[{"id":b,"required":False} if b.startswith("#") else b for b in biomes]})
    if tree:
        w(f'{D}/worldgen/placed_feature/wild_{name}.json',{"feature":P+name,"placement":[{"type":"minecraft:rarity_filter","chance":24},{"type":"minecraft:in_square"},
          {"type":"minecraft:surface_water_depth_filter","max_water_depth":0},{"type":"minecraft:heightmap","heightmap":"OCEAN_FLOOR"},{"type":"minecraft:biome"},
          {"type":"minecraft:block_predicate_filter","predicate":{"type":"minecraft:would_survive","state":{"Name":f"{P}{name}_sapling","Properties":{"stage":"0"}}}}]})
        return
    w(f'{D}/worldgen/configured_feature/wild_{name}.json',{"type":"minecraft:simple_block","config":{"to_place":{"type":"minecraft:simple_state_provider","state":{"Name":P+name,"Properties":{"age":age}}}}})
    ground={"type":"minecraft:would_survive","state":{"Name":P+name,"Properties":{"age":age}}} if sand else \
           {"type":"minecraft:matching_block_tag","tag":"minecraft:supports_vegetation","offset":[0,-1,0]}
    w(f'{D}/worldgen/placed_feature/wild_{name}.json',{"feature":f"{P}wild_{name}","placement":[{"type":"minecraft:rarity_filter","chance":40},{"type":"minecraft:in_square"},
      {"type":"minecraft:heightmap","heightmap":"WORLD_SURFACE_WG"},{"type":"minecraft:biome"},{"type":"minecraft:count","count":10},
      {"type":"minecraft:random_offset","xz_spread":{"type":"minecraft:trapezoid","max":4,"min":-4,"plateau":0},"y_spread":{"type":"minecraft:trapezoid","max":2,"min":-2,"plateau":0}},
      {"type":"minecraft:block_predicate_filter","predicate":{"type":"minecraft:all_of","predicates":[{"type":"minecraft:matching_block_tag","tag":"minecraft:air"},ground]}}]})
def field(plant, seed, product, style, color, ptex, stex, pname, sname=None, plant_name=None, **kw):
    for age in range(8):
        w(f'{A}/models/block/{plant}_stage{age}.json',{"parent":"minecraft:block/crop","textures":{"crop":STYLE[style][age]}})
    w(f'{A}/blockstates/{plant}.json',{"variants":{f"age={a}":{"model":f"{P}block/{plant}_stage{a}"} for a in range(8)}})
    mature={"condition":"minecraft:block_state_property","block":P+plant,"properties":{"age":"7"}}
    if product:
        tinted(product,ptex,color); tinted(seed,stex,color)
        NAMES[f"item.portable_pocket_cabin.{product}"]=pname; NAMES[f"item.portable_pocket_cabin.{seed}"]=sname
        pools=[{"rolls":1.0,"conditions":[mature],"entries":[{"type":"minecraft:item","name":P+product,"functions":[{"function":"minecraft:set_count","count":{"type":"minecraft:uniform","min":1.0,"max":2.0}}]}]},
               {"rolls":1.0,"entries":[{"type":"minecraft:item","name":P+seed}]},
               {"rolls":1.0,"conditions":[mature],"entries":[{"type":"minecraft:item","name":P+seed,"functions":[{"function":"minecraft:apply_bonus","enchantment":"minecraft:fortune","formula":"minecraft:binomial_with_bonus_count","parameters":{"extra":2,"probability":0.5714286}}]}]}]
    else:
        tinted(seed,ptex,color); NAMES[f"item.portable_pocket_cabin.{seed}"]=pname
        pools=[{"rolls":1.0,"entries":[{"type":"minecraft:item","name":P+seed}]},
               {"rolls":1.0,"conditions":[mature],"entries":[{"type":"minecraft:item","name":P+seed,"functions":[{"function":"minecraft:apply_bonus","enchantment":"minecraft:fortune","formula":"minecraft:binomial_with_bonus_count","parameters":{"extra":3,"probability":0.5714286}}]}]}]
    NAMES[f"block.portable_pocket_cabin.{plant}"]=plant_name or pname
    w(f'{D}/loot_table/blocks/{plant}.json',{"type":"minecraft:block","functions":[{"function":"minecraft:explosion_decay"}],"pools":pools,"random_sequence":f"{P}blocks/{plant}"})
    needs(plant, **kw)
def trellis(plant, seed, produce, color, stex, ptex, pname, sname, prname):
    TR=[{"from":f,"to":t,"faces":faces("#post")} for f,t in [([1,0,7],[3,16,9]),([13,0,7],[15,16,9]),([1,5,7.5],[15,6,8.5]),([1,10,7.5],[15,11,8.5]),([1,15,7.5],[15,16,8.5])]]
    def cross():
        return [{"from":[0.8,0,8],"to":[15.2,16,8],"rotation":{"origin":[8,8,8],"axis":"y","angle":45,"rescale":True},"shade":False,"faces":{"north":{"uv":[0,0,16,16],"texture":"#plant"},"south":{"uv":[0,0,16,16],"texture":"#plant"}}},
                {"from":[8,0,0.8],"to":[8,16,15.2],"rotation":{"origin":[8,8,8],"axis":"y","angle":45,"rescale":True},"shade":False,"faces":{"east":{"uv":[0,0,16,16],"texture":"#plant"},"west":{"uv":[0,0,16,16],"texture":"#plant"}}}]
    for age in range(8):
        tex="minecraft:block/cave_vines_plant_lit" if age==7 else "minecraft:block/cave_vines_plant"
        w(f'{A}/models/block/{plant}_stage{age}.json',{"parent":"minecraft:block/block","ambientocclusion":False,"textures":{"particle":tex,"post":"minecraft:block/stripped_oak_log","plant":tex},"elements":TR+(cross() if age>0 else [])})
    w(f'{A}/blockstates/{plant}.json',{"variants":{f"age={a}":{"model":f"{P}block/{plant}_stage{a}"} for a in range(8)}})
    tinted(seed,stex,color); tinted(produce,ptex,color)
    mature={"condition":"minecraft:block_state_property","block":P+plant,"properties":{"age":"7"}}
    w(f'{D}/loot_table/blocks/{plant}.json',{"type":"minecraft:block","pools":[{"rolls":1.0,"entries":[{"type":"minecraft:item","name":P+"trellis"}]},{"rolls":1.0,"entries":[{"type":"minecraft:item","name":P+seed}]},
      {"rolls":1.0,"conditions":[mature],"entries":[{"type":"minecraft:item","name":P+produce,"functions":[{"function":"minecraft:set_count","count":{"type":"minecraft:uniform","min":1.0,"max":2.0}}]}]}],"random_sequence":f"{P}blocks/{plant}"})
    needs(plant, support="trellis", chance=0.2)
    NAMES.update({f"block.portable_pocket_cabin.{plant}":pname,f"item.portable_pocket_cabin.{seed}":sname,f"item.portable_pocket_cabin.{produce}":prname})
def bush(plant, item, color, tex, pname, iname, stage_tex=None):
    for age in range(4):
        w(f'{A}/models/block/{plant}_stage{age}.json',{"parent":"minecraft:block/cross","textures":{"cross":(stage_tex or (lambda a: f"minecraft:block/sweet_berry_bush_stage{a}"))(age)}})
    w(f'{A}/blockstates/{plant}.json',{"variants":{f"age={a}":{"model":f"{P}block/{plant}_stage{a}"} for a in range(4)}})
    tinted(item,tex,color)
    NAMES[f"block.portable_pocket_cabin.{plant}"]=pname; NAMES[f"item.portable_pocket_cabin.{item}"]=iname
    w(f'{D}/loot_table/blocks/{plant}.json',{"type":"minecraft:block","pools":[{"rolls":1.0,"entries":[{"type":"minecraft:item","name":P+item}]}],"random_sequence":f"{P}blocks/{plant}"})
    needs(plant, soil=("#minecraft:supports_vegetation",), water="none", chance=0.2)
def tree(tree, fruit, log, fruit_color, fruit_tex, tree_name, fruit_name, chance=0.1):
    leaves=f"{tree}_leaves"; sapling=f"{tree}_sapling"
    for age,t in enumerate(["minecraft:block/azalea_leaves","minecraft:block/cherry_leaves","minecraft:block/flowering_azalea_leaves"]):
        w(f'{A}/models/block/{leaves}_stage{age}.json',{"parent":"minecraft:block/leaves","textures":{"all":t}})
    w(f'{A}/blockstates/{leaves}.json',{"variants":{f"age={a}":{"model":f"{P}block/{leaves}_stage{a}"} for a in range(3)}})
    w(f'{A}/items/{leaves}.json',{"model":{"type":"minecraft:model","model":f"{P}block/{leaves}_stage0"}})
    w(f'{A}/models/block/{sapling}.json',{"parent":"minecraft:block/cross","textures":{"cross":"minecraft:block/cherry_sapling"}})
    w(f'{A}/blockstates/{sapling}.json',{"variants":{"stage=0":{"model":f"{P}block/{sapling}"},"stage=1":{"model":f"{P}block/{sapling}"}}})
    w(f'{A}/models/item/{sapling}.json',{"parent":"minecraft:item/generated","textures":{"layer0":"minecraft:block/cherry_sapling"}})
    w(f'{A}/items/{sapling}.json',{"model":{"type":"minecraft:model","model":f"{P}item/{sapling}"}})
    tinted(fruit, fruit_tex, fruit_color)
    shears={"condition":"minecraft:any_of","terms":[{"condition":"minecraft:match_tool","predicate":{"items":"minecraft:shears"}},
            {"condition":"minecraft:match_tool","predicate":{"predicates":{"minecraft:enchantments":[{"enchantments":"minecraft:silk_touch","levels":{"min":1}}]}}}]}
    w(f'{D}/loot_table/blocks/{leaves}.json',{"type":"minecraft:block","pools":[
      {"rolls":1.0,"entries":[{"type":"minecraft:alternatives","children":[{"type":"minecraft:item","conditions":[shears],"name":P+leaves},
        {"type":"minecraft:item","conditions":[{"condition":"minecraft:survives_explosion"},{"condition":"minecraft:table_bonus","enchantment":"minecraft:fortune","chances":[0.05,0.0625,0.083333336,0.1]}],"name":P+sapling}]}]},
      {"rolls":1.0,"conditions":[{"condition":"minecraft:inverted","term":shears},{"condition":"minecraft:block_state_property","block":P+leaves,"properties":{"age":"2"}}],"entries":[{"type":"minecraft:item","name":P+fruit}]}],
      "random_sequence":f"{P}blocks/{leaves}"})
    w(f'{D}/loot_table/blocks/{sapling}.json',{"type":"minecraft:block","pools":[{"rolls":1.0,"conditions":[{"condition":"minecraft:survives_explosion"}],"entries":[{"type":"minecraft:item","name":P+sapling}]}],"random_sequence":f"{P}blocks/{sapling}"})
    w(f'{D}/worldgen/configured_feature/{tree}.json',{"type":"minecraft:tree","config":{
      "below_trunk_provider":{"type":"minecraft:rule_based_state_provider","rules":[{"if_true":{"type":"minecraft:not","predicate":{"type":"minecraft:matching_block_tag","tag":"minecraft:cannot_replace_below_tree_trunk"}},"then":{"type":"minecraft:simple_state_provider","state":{"Name":"minecraft:dirt"}}}]},
      "decorators":[],"foliage_placer":{"type":"minecraft:blob_foliage_placer","height":3,"offset":0,"radius":2},
      "foliage_provider":{"type":"minecraft:simple_state_provider","state":{"Name":P+leaves,"Properties":{"age":"0","distance":"7","persistent":"false","waterlogged":"false"}}},
      "ignore_vines":True,"minimum_size":{"type":"minecraft:two_layers_feature_size"},
      "trunk_placer":{"type":"minecraft:straight_trunk_placer","base_height":4,"height_rand_a":1,"height_rand_b":0},
      "trunk_provider":{"type":"minecraft:simple_state_provider","state":{"Name":log,"Properties":{"axis":"y"}}}}})
    needs(leaves, soil=("#minecraft:supports_vegetation",), water="none", chance=chance)
    NAMES.update({f"block.portable_pocket_cabin.{leaves}":tree_name+" Leaves",f"block.portable_pocket_cabin.{sapling}":tree_name+" Sapling",
                  f"item.portable_pocket_cabin.{fruit}":fruit_name,f"garden.portable_pocket_cabin.{leaves}.name":tree_name+" Tree"})
    for t,v in [("minecraft:tags/block/leaves",leaves),("minecraft:tags/item/leaves",leaves),("minecraft:tags/block/saplings",sapling),("minecraft:tags/item/saplings",sapling),("minecraft:tags/block/mineable/hoe",leaves)]:
        ns,path=t.split(':'); p=f'{R}/data/{ns}/{path}.json'
        try: d=_j.load(open(p))
        except FileNotFoundError: d={"replace":False,"values":[]}
        if P+v not in d["values"]: d["values"].append(P+v)
        w(p,d)
def trades(items, farmer=()):
    p=f'{R}/data/minecraft/tags/villager_trade/wandering_trader/uncommon.json'; d=_j.load(open(p))
    for item in items:
        count=1 if item.endswith(("sapling","cutting","crown","root","rhizome")) else 3
        w(f'{D}/villager_trade/wandering_trader/emerald_{item}.json',{"gives":{"id":P+item,"count":count},"wants":{"id":"minecraft:emerald","count":2 if count==1 else 1},"max_uses":8.0,"reputation_discount":0.05})
        d["values"].append(f"{P}wandering_trader/emerald_{item}")
    w(p,d)
    fp=f'{R}/data/minecraft/tags/villager_trade/farmer/level_3.json'; d=_j.load(open(fp))
    for item in farmer:
        w(f'{D}/villager_trade/farmer/3/emerald_{item}.json',{"gives":{"id":P+item,"count":4},"wants":{"id":"minecraft:emerald","count":1},"max_uses":12.0,"reputation_discount":0.05,"xp":10.0})
        d["values"].append(f"{P}farmer/3/emerald_{item}")
    w(fp,d)
def i(n,c=1):
    n=n if (isinstance(n,dict) or ":" in n or n.startswith("#")) else P+n
    return {"item":n,"count":c} if c>1 else {"item":n}
RECIPES=[]
def r(path, station, technique, ings, result, count=1, time=None, heat=None, container=None, water=0, oil=0, remainders=None, tastes=None):
    o={"type":P+station,"technique":technique,"ingredients":ings}
    if container: o["container"]=container
    if water: o["water"]=water
    if oil: o["oil"]=oil
    if heat: o["heat"]=heat
    if time is not None: o["time"]=time
    if remainders: o["remainders"]=[{"id":x if ":" in x else P+x,"count":c} if c>1 else {"id":x if ":" in x else P+x} for x,c in remainders]
    if tastes: o["tastes"]=tastes
    o["result"]={"id":result if ":" in result else P+result} | ({"count":count} if count>1 else {})
    recipe(path,o); RECIPES.append(path)
def addtag(t, items):
    p=f'{D}/tags/item/tastes/{t}.json'; d=_j.load(open(p))
    for x in items:
        if P+x not in d["values"]: d["values"].append(P+x)
    w(p,d)
def items(spec):
    for n,(tex,color,title) in spec.items():
        if tex=="bottle": bottle(n,color)
        else: tinted(n,tex,color)
        NAMES[f"item.portable_pocket_cabin.{n}"]=title
def tech(spec):
    """Names new techniques. A technique that already exists keeps its text: a delivery that means something else gives its own technique id."""
    existing=_j.load(open(A+'/lang/en_us.json'))
    for k,(n,d) in spec.items():
        key=f"technique.portable_pocket_cabin.{k}"
        if key in existing and (existing[key]!=n or existing[key+".description"]!=d):
            raise SystemExit(f"technique {k} already exists; reuse it as it is or give the new use its own id")
        NAMES[key]=n; NAMES[key+".description"]=d
GB={"item":"minecraft:glass_bottle"}; B=("minecraft:glass_bottle",1); BOWL={"item":"minecraft:bowl"}
