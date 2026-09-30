# PDR-0012: Books reveal upgrades before purchase

Status: Accepted

## Context

Several cabin features use books for discovery. Different book installation rules would make progression hard to understand. A discovered book also needs a clear boundary from the upgrade it reveals.

## Decision

The owner installs [cabin books](../../context.md#cabin-book) through one cabin control. A successful installation consumes one book and permanently reveals its declared upgrade targets. One book can reveal more than one upgrade. Installing a book does not fund, purchase, enable, or apply an upgrade. The owner uses the normal upgrade process for each [revealed upgrade](../../context.md#revealed-upgrade). An invalid or already installed book changes nothing and is not consumed. Each cabin must get and install its own books. Network membership does not reveal upgrades or give automation capabilities.

## Rationale

One installation action gives each book the same meaning. The revealed targets let players plan their upgrades before they pay. A book does not bypass upgrade materials or owner control.

## Scope

[B-0038](../../features/B-0038/brief.md) owns book installation and acquisition. [B-0007](../../features/B-0007/brief.md), [B-0008](../../features/B-0008/brief.md), and [B-0028](../../features/B-0028/brief.md) own the effects of their purchased upgrades. [PDR-0007](0007-fund-and-install-cabin-upgrades.md) owns upgrade funding and purchase. This decision does not apply to blank books or ordinary enchanted books.

## Consequences

The cabin saves which books it installed and which targets they reveal. Book ownership alone does not change cabin abilities. Revealed targets stay available through packing, restart, and redeployment.
