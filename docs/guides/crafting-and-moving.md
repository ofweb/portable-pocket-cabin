# Craft and move a cabin

Obtain a Block of Amethyst to reveal the three dimensional-core recipes. Craft all three cores to reveal the Dimensional Foundation. Craft the Foundation to reveal the Cabin Kit. These components cannot be placed as blocks. The recipes require Blocks of Amethyst; shards do not substitute.

## Craft the components

The Dimensional Logic Core uses this pattern:

```text
C R C
R B R
C R C

C = Copper Ingot
R = Redstone Dust
B = Block of Amethyst
```

The Dimensional Anchor uses this pattern:

```text
I O I
O B O
I O I

I = Iron Ingot
O = Obsidian
B = Block of Amethyst
```

The Dimensional Folding Core uses this pattern:

```text
C B C
B E B
C B C

C = Copper Ingot
B = Block of Amethyst
E = Ender Pearl
```

Combine the three cores into a Dimensional Foundation:

```text
S A S
C L C
S F S

S = Lodestone
A = Dimensional Anchor
C = Cut Copper Block
L = Dimensional Logic Core
F = Dimensional Folding Core
```

Craft the Cabin Kit with the Foundation and your chosen materials:

```text
R R R
W D W
F X F

R = roof-family Planks
W = wall-family Structural Wood
D = supported Door
F = floor-family Planks
X = Dimensional Foundation
```

The roof, wall, and floor can use different supported wood families. Both ingredients within the wall role must match, as must repeated floor and roof ingredients. The door can use a different supported variant. Structural Wood is a log for ordinary trees, a stem for crimson or warped wood, or a Bamboo Block for bamboo. The Kit keeps the selected floor, wall, roof, and door palette. An unbound Kit can be stored or given to another player before first deployment.

## Deploy and pack

Use the Kit on the top face of a solid terrain block. The first use previews the exact footprint for 30 seconds. The front stair appears above the selected block, and the door faces the direction you faced at the first use. Use the same item on the same surface again during the preview to deploy. A different valid surface replaces the preview. Leaving the dimension, losing the item, or waiting too long cancels it.

The first successful deployment binds the Kit to a permanent cabin. The cabin keeps its interior and palette when packed and moved. A blocked site, unsafe doorway, unsupported dimension, or invalid item fails without consuming the item. Placement does not remove terrain or nearby player blocks.

Use the exterior lodestone normally to enter. To pack, sneak-use it twice within 10 seconds. The first use arms packing; the second requests it. A five-second countdown follows. Packing stops if it cannot give every occupant a safe destination or deliver the packed item. The cabin remains deployed after a failed pack.

An eligible player can first deploy an unbound Kit. After binding, only the cabin owner can deploy or pack it. Normal play does not require lifecycle commands. Operators retain diagnostic and recovery commands.
