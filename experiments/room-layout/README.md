# Cabin room floor-layout reference

[Scale drawing (SVG)](layout.svg) · [PNG preview](layout.png) · [Printable PDF](layout.pdf)

The drawing overlays minimum usable floors on maximum footprints at main-room
sizes 5×5 and 21×21. Both panels use the same scale: seven pixels per block.
Dark borders represent one-block walls. Corridors have three blocks of clear
width. White wall breaks represent one-block openings.

The drawing records agreed room placement and the open livestock-corridor bend.
Growth anchors, stable internal geometry, and one-block walls remain assumptions.

- West south, nearest first: greenhouse, then arboretum.
- West north: stable, with a livestock branch farther west.
- The livestock branch runs north, with pigs and chickens nearest the junction,
  sheep and cows next, and goats straight ahead at its north end. Its three-block clear
  width matches the main corridors. It moves with the west wing during expansion.
- The west corridor ends in an open bend into the livestock corridor. Both
  legs share a continuous three-block-wide floor, with no wall across the turn.
- Livestock entrances stay centered on the wall facing the branch. Floors
  grow away from that wall, inside each maximum reservation.
- North: crafting then enchanting on the west side. Smelting sits farther
  north on the east side, leaving room for potions below it.
- East: kitchen south and potions north, directly opposite each other.
- Greenhouse and arboretum put their long axes southward. Their entrances stay
  centered on the north wall as they grow away from the corridor.
- The stable has a north–south internal corridor on its east side, entered
  from the south. Each stall
  has seven usable blocks east–west and five north–south. One-block partitions
  separate five stalls. The first stall and corridor use 11×5; all five use 11×29.
- Maximum footprints determine spacing even when rooms start smaller.
  Adjacent room shells have one unused block between them.

The proposed known footprint is 64×88 at main-room size 5 and 80×88 at size 21,
including walls and corridor ends. Compare the [previous straight west layout](straight-west.svg)
and its [checks](straight-west-checks.json):

| Measure | Straight west layout | Livestock branch |
|---|---:|---:|
| Maximum footprint | 98×77 | 80×88 |
| Main west corridor length | 67 | 37 |
| Livestock branch length | 0 | 29 |
| Total west corridor length | 67 | 66 |
| Farthest livestock entrance from main-room west wall | 62.5 | 66.5 |

Entrance distances include the turn from corridor center to the opening.
The branch reduces the bounding footprint area by about 7% and total west
corridor length by one block. The longest livestock walk increases by four
blocks. The east corridor remains eight blocks long.
All nine main-room sizes pass
the [floor checks](checks.json): usable floors do not overlap, room shells do
not overlap each other or usable main-room and corridor floors, and bounds
stay inside the current 512-block cell.
Smaller floors remain inside those maximum reservations.

This is a floor-plan check, not a complete fit proof. The aquatic berth has
no specified dimensions and is excluded. Its relation to the five stable stalls
needs agreement before it can be drawn. Most room heights remain unspecified;
the arboretum needs five to twenty clear blocks. Connected-cabin hallways also
need an allowance. Floor checks do not establish safe relocation or growth.

To regenerate the drawing and checks:

```sh
python experiments/room-layout/draw.py
rsvg-convert -o experiments/room-layout/layout.png experiments/room-layout/layout.svg
rsvg-convert -f pdf -o experiments/room-layout/layout.pdf experiments/room-layout/layout.svg
```

Room sizes come from the [B-0005 brief](../../.workflow/features/B-0005/brief.md)
and its linked room features. These drawings do not change production geometry.
