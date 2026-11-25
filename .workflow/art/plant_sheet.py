import sys
from PIL import Image, ImageDraw
d='build/run/clientGameTest/screenshots/'
crops={'tall':(180,420,1240,740),'wide':(0,420,1280,640),'normal':(180,400,1240,620),'sea':(300,330,1100,600),'leaves':(80,300,1200,660)}
kind={'cucumber_vine':'tall','hop_vine':'tall','long_bean_vine':'tall','peppercorn_vine':'tall','vanilla_vine':'tall','betel_vine':'tall','blue_corn':'tall','purple_corn':'tall','grape_vine':'tall','red_grape_vine':'tall','corn':'tall','tomato_vine':'tall','white_grape_vine':'tall','dill':'tall','courgette':'wide','rhubarb':'wide',
      'plum_leaves':'leaves','wakame_plant':'sea','laver_plant':'sea','sea_lettuce_plant':'sea','mussel_bed':'sea','oyster_bed':'sea'}
labels={'corn':'corn (two blocks tall)','tomato_vine':'tomato (two blocks tall on a stacked trellis)','white_grape_vine':'white grape (two blocks tall on a stacked trellis)',
        'dill':'dill (two blocks tall)','courgette':'courgette (sprawls wide, spaced out here)','rhubarb':'rhubarb (sprawls wide, spaced out here)',
        'plum_leaves':'plum leaves on a log, spaced out: leaves, blossom, fruit','raspberry_bush':'raspberry (bush, ages 0-3)',
        'asparagus':'asparagus: fern while young, then spears','wakame_plant':'wakame (under water, ages 0-3)','strawberry_bush':'strawberry (bush, ages 0-3)'}
out_path, ps = sys.argv[1], sys.argv[2].split(',')
rows=[]
for p in ps:
    im=Image.open(d+f'plant-{p}.png').crop(crops[kind.get(p,'normal')])
    rows.append((p,im.resize((im.width*2,im.height*2),Image.NEAREST)))
W=max(im.width for _,im in rows); H=sum(im.height+44 for _,im in rows)
out=Image.new('RGB',(W,H),(30,30,30)); dr=ImageDraw.Draw(out); y=0
for p,im in rows:
    dr.text((12,y+8),labels.get(p,p.replace('_',' ')),fill=(255,255,255),font_size=26); out.paste(im,(0,y+44)); y+=im.height+44
out.save(out_path)
