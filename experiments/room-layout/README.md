# Cabin room floor-layout reference

[Scale drawing (SVG)](layout.svg) · [PNG preview](layout.png) · [Printable PDF](layout.pdf)

[Arboretum view (SVG)](arboretum.svg) · [PNG preview](arboretum.png) · [Printable PDF](arboretum.pdf)

The drawing overlays minimum usable floors on maximum footprints at main-room
sizes 5×5 and 21×21. Both panels use the same scale: seven pixels per block.
Dark borders represent one-block walls. Corridors have three blocks of clear
width. White wall breaks represent one-block openings. Amber marks the arboretum door.

The drawing records agreed room placement and the open livestock-corridor bend.
Growth anchors, stable internal geometry, and one-block walls remain assumptions.

- West south, nearest first: greenhouse, then arboretum entrance door.
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
- The greenhouse puts its long axis southward. Its entrance stays centered
  on the north wall as it grows away from the corridor.
- The arboretum door keeps its corridor position. Its cultivated room and
  woodland appear in a separate view with no coordinates relative to the cabin.
- The stable has a north–south internal corridor on its east side, entered
  from the south. Each stall
  has seven usable blocks east–west and five north–south. One-block partitions
  separate five stalls. The first stall and corridor use 11×5; all five use 11×29.
- Maximum footprints determine spacing even when rooms start smaller.
  Adjacent room shells have one unused block between them.

The proposed known cabin footprint is 64×78 at main-room size 5 and 80×78 at
size 21, including walls and corridor ends. Arboretum space is shown separately:

| Measure | Main room 5×5 | Main room 21×21 |
|---|---:|---:|
| Maximum cabin footprint | 64×78 | 80×78 |
| Main west corridor length | 37 | 37 |
| Livestock branch length | 29 | 29 |
| Total west corridor length | 66 | 66 |
| Farthest livestock entrance from main-room west wall | 66.5 | 66.5 |

Entrance distances include the turn from corridor center to the opening.
The east corridor remains eight blocks long. The [previous straight west
layout](straight-west.svg) and its [checks](straight-west-checks.json) predate
the arboretum entrance-door revision and use different room reservations.
All nine main-room sizes pass
the [floor checks](checks.json): usable floors do not overlap, room shells do
not overlap each other or usable main-room and corridor floors, and bounds
stay inside the current 512-block cell.
Smaller cabin-room floors remain inside those maximum reservations. The
arboretum door lies in the west corridor's south wall at every main-room size,
without overlapping another room's shell.

The separate arboretum view shows usable floors of 7×7, 19×19, and 29×29,
with clear heights of 5, 10, and 15. Its fixed north entrance anchors growth
equally east–west and southward. The fixed four-block planting grid extends
from 4 to 20 to 42 positions. Floor checks verify that each tier retains the
previous tier's planting positions and leaves space before enclosed walls.

At the final tier, a 51×51 transition surrounds the 29×29 core. Its 39×39 and
45×45 markers illustrate gradual biome clues, wildlife, and snow cover.
Drawn outlines mark dimensions, not abrupt changes in tree species or spacing.
The freestanding north entrance wall is 29 blocks wide and 15 tall. Players
can walk around either end into the woodland behind it. Dashed side and south
walls apply only to enclosed tiers. The final tier has no visible ceiling.
Woodland extends beyond the cropped drawing in every direction.

This is a floor-plan check, not a complete fit proof. The aquatic berth has
no specified dimensions and is excluded. Its relation to the five stable stalls
needs agreement before it can be drawn. Most room heights remain unspecified.
The arboretum view does not establish world allocation, per-cabin isolation,
height clearance, or safe doorway operation. Design must verify these against
the [arboretum brief](../../.workflow/features/B-0023/brief.md).
Connected-cabin hallways also
need an allowance. Floor checks do not establish safe relocation or growth.

To regenerate the drawing and checks:

```sh
python experiments/room-layout/draw.py
rsvg-convert -o experiments/room-layout/layout.png experiments/room-layout/layout.svg
rsvg-convert -f pdf -o experiments/room-layout/layout.pdf experiments/room-layout/layout.svg
rsvg-convert -o experiments/room-layout/arboretum.png experiments/room-layout/arboretum.svg
rsvg-convert -f pdf -o experiments/room-layout/arboretum.pdf experiments/room-layout/arboretum.svg
```

Room sizes come from the [B-0005 brief](../../.workflow/features/B-0005/brief.md)
and its linked room features. These drawings do not change production geometry.
