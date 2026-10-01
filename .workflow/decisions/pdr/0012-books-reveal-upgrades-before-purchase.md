# PDR-0012: Books reveal upgrades before purchase

Status: Accepted

## Context

Cabin features must use the same book installation rules. A book reveals upgrades independently of their purchase.

## Decision

The owner installs [cabin books](../../context.md#cabin-book) through one cabin control. Completed installation uses one book and permanently reveals its specified upgrade targets. One book can reveal multiple upgrades. Installation does not fund, purchase, enable, or apply an upgrade. The owner follows the same upgrade process for each [revealed upgrade](../../context.md#revealed-upgrade). An invalid book or one already installed stays with the player and has no effect. Each cabin must get and install books locally. Network membership does not reveal upgrades or give automation capabilities.

## Rationale

One installation action gives all cabin books the same behavior. Players can plan upgrades from revealed targets before payment. Books cannot bypass upgrade materials or owner control.

## Scope

[B-0038](../../features/B-0038/brief.md) states book installation and acquisition. [B-0007](../../features/B-0007/brief.md), [B-0008](../../features/B-0008/brief.md), and [B-0028](../../features/B-0028/brief.md) state purchased upgrade effects. [PDR-0007](0007-fund-and-install-cabin-upgrades.md) states funding and purchase rules. This decision excludes empty books and Minecraft enchanted books.

The cabin saves installed books and revealed targets. Holding a book alone does not affect cabin actions. Revealed targets stay through packing, restart, and redeployment.
