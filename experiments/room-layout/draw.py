from pathlib import Path
from html import escape
import json

OUT = Path(__file__).parent
S = 7
# All coordinates describe block edges; north is negative Z.
def layout(size):
    r = size / 2
    rooms = []
    west_rows = {
        'south': [('Greenhouse',(5,7),(11,31))],
        'north': [('Stable',(11,5),(11,29))],
    }
    west_ends = []
    for side, row in west_rows.items():
        cursor = -r - (2 if side == 'south' else 7)
        for name, small, large in row:
            w,d = large
            cx = cursor - 1 - w/2
            z = 2.5 if side == 'south' else -2.5-d
            rooms.append(dict(name=name,min=small,max=large,floor=(cx-w/2,z,w,d),side=side))
            cursor = cx-w/2-2
        west_ends.append(cursor+1)
    branch_x = -r-35.5
    for name, row, side in [('Pigs',0,'west'),('Chickens',0,'east'),
                            ('Sheep',1,'west'),('Cows',1,'east')]:
        x = branch_x-13.5 if side=='west' else branch_x+2.5
        z = -14.5-14*row
        rooms.append(dict(name=name,min=(3,3),max=(11,11),floor=(x,z,11,11),
                          side=side,branch_x=branch_x))
    rooms.append(dict(name='Goats',min=(3,3),max=(11,11),
                      floor=(branch_x-5.5,-42.5,11,11),side='north',end_room=True))
    east = r+2
    for name, side in [('Kitchen','south'),('Potions','north')]:
        x = east+1
        z = 2.5 if side == 'south' else -7.5
        rooms.append(dict(name=name,min=(5,5),max=(5,5),floor=(x,z,5,5),side=side))
    east_end = east+6
    for name,x,z,w in [('Crafting',-7.5,-r-7.5,5),('Smelting',2.5,-r-15.5,7),('Enchanting',-7.5,-r-15.5,5)]:
        rooms.append(dict(name=name,min=(w,w),max=(w,w),floor=(x,z,w,w),side='east' if x<0 else 'west'))
    # End the west run at the outer edge of the northward bend.
    west_end = branch_x-1.5
    corridors=[(west_end,-1.5,-r-west_end,3),(r,-1.5,east_end-r,3),(-1.5,-r-17,3,17),(branch_x-1.5,-30.5,3,29)]
    return rooms,corridors

def overlaps(a,b):
    x,z,w,d=a; X,Z,W,D=b
    return min(x+w,X+W)>max(x,X) and min(z+d,Z+D)>max(z,Z)

def shell(a):
    x,z,w,d=a
    return (x-1,z-1,w+2,d+2)

def arboretum_door(size):
    # Retain the former arboretum entrance position on the west corridor.
    return (-size/2-28, 1.5, 1, 1)

def arboretum_floor(size):
    # This separate view has no allocation in the cabin's coordinate system.
    return (-size/2, 0, size, size)

def planting_grid(size):
    # Keep the initial grid fixed while the floor grows sideways and southward.
    return [(x, z) for x in range(-14, 15, 4)
            for z in (1.5+4*i for i in range(7))
            if -size/2+1 <= x <= size/2-1 and 1 <= z <= size-1]

arboretum_checks=[]
for size, height, expected in [(7, 5, 4), (19, 10, 20), (29, 15, 42)]:
    grid=planting_grid(size)
    assert len(grid)==expected, (size, grid)
    assert not arboretum_checks or set(planting_grid(arboretum_checks[-1]['floor_size'])).issubset(grid)
    arboretum_checks.append(dict(floor_size=size, clear_height=height,
                                 planting_positions=len(grid), floor=arboretum_floor(size)))
assert overlaps(arboretum_floor(29), (-25.5, -11, 51, 51))
assert all(-25.5 <= x <= 25.5 and -11 <= z <= 40
           for x, z in [(-14.5, 0), (14.5, 29)])

results=[]
for size in range(5,22,2):
    rooms,corridors=layout(size)
    solids=[('Main',(-size/2,-size/2,size,size))]+[(v['name'],v['floor']) for v in rooms]+[(f'Corridor {i}',v) for i,v in enumerate(corridors)]
    failures=[f'{n} / {N}' for i,(n,a) in enumerate(solids) for N,b in solids[i+1:] if overlaps(a,b)]
    room_shells=[(v['name'],shell(v['floor'])) for v in rooms]
    failures += [f'Shell {n} / {N}' for i,(n,a) in enumerate(room_shells) for N,b in room_shells[i+1:] if overlaps(a,b)]
    for room_name, room_box in room_shells:
        for space_name, space_box in [solids[0]] + [v for v in solids if v[0].startswith('Corridor')]:
            if overlaps(room_box, space_box):
                failures.append(f'Shell {room_name} / floor {space_name}')
    assert not failures, failures
    boxes=[shell(a) for _,a in solids]
    bounds=[min(a[0] for a in boxes), min(a[1] for a in boxes),max(a[0]+a[2] for a in boxes),max(a[1]+a[3] for a in boxes)]
    assert all(-256 < v < 256 for v in bounds)
    door=arboretum_door(size)
    west=corridors[0]
    assert west[0] <= door[0] and door[0]+door[2] <= west[0]+west[2]
    assert door[1] == west[1]+west[3]
    assert not any(overlaps(door, box) for _, box in room_shells)
    results.append(dict(main_size=size,bounds=bounds,west_corridor_length=corridors[0][2],east_corridor_length=corridors[1][2],livestock_branch_length=corridors[3][3],west_network_length=corridors[0][2]+corridors[3][3],farthest_livestock_door_distance_from_main_wall=35.5+31,known_floor_overlaps=failures,within_current_512_block_cell=True,arboretum_cabin_door=door))

svg=['<svg xmlns="http://www.w3.org/2000/svg" width="1400" height="1650" viewBox="0 0 1400 1650">',
'<defs><pattern id="grid" width="7" height="7" patternUnits="userSpaceOnUse"><path d="M7 0H0V7" fill="none" stroke="#d8dee8" stroke-width=".4"/></pattern></defs>',
'<rect width="1400" height="1650" fill="#fafbfd"/>']
def text(x,y,t,size=14,color='#26344b',anchor='start'):
    svg.append(f'<text x="{x}" y="{y}" font-family="sans-serif" font-size="{size}" fill="{color}" text-anchor="{anchor}">{escape(t)}</text>')
text(30,32,'B-0005 · Room layout and arboretum entrance',24)
text(30,58,'True scale: 7 pixels = 1 block. Minimum floors overlay maximum footprints. North is up.',15)
text(30,81,'Room order, growth anchors, stable layout and one-block walls are proposals. Aquatic berth has no agreed dimensions.',14,'#a33c22')
for panel,size in enumerate([5,21]):
    base=200+panel*700
    ox=1050; oz=base+270
    rooms,corridors=layout(size)
    def rect(a,fill,stroke='#34445b',dash='',width=1):
        x,z,w,d=a
        svg.append(f'<rect x="{ox+x*S}" y="{oz+z*S}" width="{w*S}" height="{d*S}" fill="{fill}" stroke="{stroke}" stroke-width="{width}" stroke-dasharray="{dash}"/>')
    text(30,base+15,f'Main room {size}×{size} · all known room reservations',19)
    rect((-110,-50,151,98),'url(#grid)','#d8dee8')
    for a in corridors:
        rect(shell(a),'#495568')
    for a in corridors:
        rect(a,'#e7edf5','none')
    rect(shell((-size/2,-size/2,size,size)),'#495568')
    rect((-size/2,-size/2,size,size),'#ead9af')
    for room in rooms:
        x,z,w,d=room['floor']; sw,sd=room['min']; name=room['name']
        rect(shell(room['floor']),'#495568')
        rect(room['floor'],'#d2e5d9')
        side = room['side']
        small_z = z if side == 'south' else z+d-sd
        small=(x+(w-sw)/2,small_z,sw,sd) if side in ('south','north') else room['floor']
        rect(small,'#76bda1','#175f4c','3 2',1.5)
        if side in ('south','north'):
            door_x = x+9.5 if name=='Stable' else x+w/2
            door_z = z+d if room.get('end_room') else (1.5 if side=='south' else -2.5)
            rect((door_x-.5,door_z,1,1),'#fff','#fff')
            label_z = z+d+3 if side=='south' else z-3
            if name=='Potions':
                text(ox+(x+w+1.5)*S,oz+(z+d/2)*S,name+' 5×5',12)
            else:
                text(ox+(x+w/2)*S,oz+label_z*S,name,12,anchor='middle')
                text(ox+(x+w/2)*S,oz+(label_z+2)*S,f'{w}×{d}',12,anchor='middle')
            if room['min']!=room['max']: text(ox+(x+w/2)*S,oz+(small_z+sd/2)*S+4,f'{sw}×{sd}',11,anchor='middle')
        else:
            if 'branch_x' in room:
                bx=room['branch_x']
                west=side=='west'
                small=(x+w-sw if west else x,z+(d-sd)/2,sw,sd)
                # Cover the earlier full-floor overlay with the correct entrance anchor.
                rect(room['floor'],'#d2e5d9')
                rect(small,'#76bda1','#175f4c','3 2',1.5)
                rect((bx-2.5 if west else bx+1.5,z+d/2-.5,1,1),'#fff','#fff')
                if west:
                    tx=ox+(x-1.5)*S
                    text(tx,oz+(z+d/2)*S,name,12,anchor='end')
                    text(tx,oz+(z+d/2)*S+15,'3×3 → 11×11',11,anchor='end')
                else:
                    text(ox+(x+w/2)*S,oz+(z+2)*S,name,11,anchor='middle')
                    text(ox+(x+w/2)*S,oz+(z+d-1)*S,'3×3 → 11×11',9,anchor='middle')
            else:
                rect((-2.5 if side=='east' else 1.5,z+d/2-.5,1,1),'#fff','#fff')
                if x<0:
                    text(ox+(x+w/2)*S,oz+(z+d/2)*S-2,name,9,anchor='middle')
                    text(ox+(x+w/2)*S,oz+(z+d/2)*S+10,f'{w}×{d}',9,anchor='middle')
                else:
                    text(ox+(x+w+1.5)*S,oz+(z+d/2)*S,name+f' {w}×{d}',12)
        if name=='Stable':
            # West stalls: 7 east/west × 5 north/south; corridor on east.
            for k in range(5):
                rect((x,z+d-5-k*6,7,5),'none','#6a8e78','2 2')
            rect((x+8,z,3,d),'#e7edf5','#64758b')
    for opening in [(-size/2-1,-.5,1,1),(size/2,-.5,1,1),(-.5,-size/2-1,1,1)]: rect(opening,'#fff','#fff')
    door=arboretum_door(size)
    rect(door,'#dd9c47','#7a491c',width=1.5)
    text(ox+(door[0]+.5)*S,oz+5.5*S,'Arboretum door',12,anchor='middle')
    text(ox+(door[0]+.5)*S,oz+8*S,'Separate view: arboretum.svg',11,anchor='middle')
    text(ox,oz+size*S/2+15,'Main',12,anchor='middle')
    svg.append(f'<path d="M {ox+200} {oz-125} v -25 m -5 7 l5 -7 5 7" fill="none" stroke="#26344b" stroke-width="2"/>')
    text(ox+200,oz-157,'N',14,anchor='middle')
    sx=50;sy=base+610
    svg.append(f'<path d="M{sx} {sy}h{10*S} M{sx} {sy-5}v10 M{sx+10*S} {sy-5}v10" stroke="#26344b" stroke-width="2"/>')
    text(sx,sy+22,'10 blocks',13)
    bound=results[0 if size==5 else -1]['bounds']
    text(190,sy+6,f'Known outer footprint: {bound[2]-bound[0]:g} × {bound[3]-bound[1]:g} blocks',14)
text(30,1560,'Dark: one-block shell · light green: maximum usable floor · green dashed: minimum floor · amber: arboretum door',14)
text(30,1585,'All nine main-room sizes (5–21) checked: known floors and room shells do not overlap; footprint stays within a 512-block cell.',14)
text(30,1610,'Arboretum shown separately. Allocation, aquatic berth, heights, connected hallways and growth still need Design verification.',14,'#a33c22')
svg.append('</svg>')
(OUT/'layout.svg').write_text('\n'.join(svg))
(OUT/'checks.json').write_text(json.dumps(dict(status='proposal',excluded=['arboretum world allocation','aquatic berth','vertical geometry','connected cabin hallways'],checks=results,arboretum=dict(view='separate; no relative world position',tiers=arboretum_checks,core_floor=arboretum_floor(29),transition=(-25.5,-11,51,51),markers=[39,45],final_wall_width=29,final_wall_height=15,visible_final_ceiling=False,woodland_extent='effectively endless; drawing cropped')),indent=2)+'\n')

svg=['<svg xmlns="http://www.w3.org/2000/svg" width="1000" height="760" viewBox="0 0 1000 760">',
     '<defs><pattern id="grid" width="7" height="7" patternUnits="userSpaceOnUse"><path d="M7 0H0V7" fill="none" stroke="#d8dee8" stroke-width=".4"/></pattern></defs>',
     '<rect width="1000" height="760" fill="#fafbfd"/>']
ox=350;oz=240
def rect(a,fill,stroke='#34445b',dash='',width=1):
    x,z,w,d=a
    svg.append(f'<rect x="{ox+x*S}" y="{oz+z*S}" width="{w*S}" height="{d*S}" fill="{fill}" stroke="{stroke}" stroke-width="{width}" stroke-dasharray="{dash}"/>')
text(30,32,'B-0023 · Arboretum tiers and woodland transition',24)
text(30,58,'Separate view; no position relative to cabin. True scale: 7 pixels = 1 block. North is up.',15)
rect((-40.5,-20,81,75),'#e1eddd','none')
rect((-40.5,-20,81,75),'url(#grid)','none')
rect((-25.5,-11,51,51),'#d2e5d9','#527d60','6 4',2)
rect((-22.5,-8,45,45),'none','#80a497','3 3')
rect((-19.5,-5,39,39),'none','#80a497','3 3')
rect(arboretum_floor(29),'#d9dfad','#64713d',width=2)
rect(arboretum_floor(19),'none','#4774a6','4 3',2)
rect(arboretum_floor(7),'none','#a35935','3 2',2)
for x,z in planting_grid(29):
    svg.append(f'<circle cx="{ox+x*S}" cy="{oz+z*S}" r="2.5" fill="#426941"/>')
rect((-14.5,-1,29,1),'#495568')
rect((-15.5,0,1,29),'none','#495568','3 3')
rect((14.5,0,1,29),'none','#495568','3 3')
rect((-15.5,29,31,1),'none','#495568','3 3')
# Final-tier wall ends align with the usable core; enclosed-tier side walls go.
rect((-.5,-1,1,1),'#dd9c47','#7a491c',width=1.5)
text(ox,oz-19,'Fixed north entrance',13,anchor='middle')
text(ox,oz+31.5*S,'29×29 cultivated core',14,anchor='middle')
text(ox,oz+42.5*S,'51×51 gradual transition',14,anchor='middle')
for x,z,label in [(0,-16,'Woodland continues north'),(0,50,'Woodland continues south')]:
    text(ox+x*S,oz+z*S,label,13,anchor='middle')
text(80,400,'Continues west',12)
text(500,400,'Continues east',12)
text(700,125,'Usable floor / clear height',17)
text(700,155,'7×7 / 5 · 4 planting positions',14,'#a35935')
text(700,181,'19×19 / 10 · 20 positions',14,'#4774a6')
text(700,207,'29×29 / 15 · 42 positions',14,'#64713d')
text(700,247,'Final wall: 29 wide, 15 tall',14)
text(700,273,'Final tier: no visible ceiling',14)
text(700,313,'39×39: sparse biome clues',14)
text(700,339,'45×45: critters and snow cover',14)
text(700,365,'51×51: normal biome undergrowth',14)
text(700,405,'Dashed lines mark dimensions.',14)
text(700,431,'Species and spacing blend gradually.',14)
text(700,471,'Dashed outer walls exist only',14)
text(700,497,'while the arboretum is enclosed.',14)
text(30,668,'Final tier: walk around either end of the entrance wall into transition and woodland behind it.',14)
text(30,695,'The woodland drawing is cropped, with no reachable outer edge. Planting points show the fixed core grid.',14)
text(30,722,'This checks floor dimensions and anchors; allocation, height and doorway behavior need Design verification.',14,'#a33c22')
svg.append('</svg>')
(OUT/'arboretum.svg').write_text('\n'.join(svg))
print(json.dumps(results[-1]))
