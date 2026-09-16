# Portable Pocket Cabin Roadmap

This is the documentation entry point and implementation order for Portable Pocket Cabin. Each milestone's authoritative file contains its outcome, scope, specification, delivery plan, acceptance criteria, and deferred decisions.

The project has not been publicly released. The [README](README.md) defines current save compatibility and replacement requirements. A delivery is complete only after it works in a fresh world and survives a server restart. Later work may begin while manual acceptance or a retrospective audit remains pending, but the status table must say so.

## Ordering principles

1. Complete survival acquisition and command-free deployment and packing first.
2. Establish persistent data models before building features that depend on them.
3. Make rooms useful manually before adding automation.
4. Make one cabin safe and understandable before connecting multiple cabins.
5. Add cross-cabin resource movement only after local storage and network permissions are proven independently.
6. Keep balancing values data-driven and settle them through survival playtests at the milestone that consumes them.

## Milestones

| Milestone                                      | Outcome                                               | Status                                |
| ---------------------------------------------- | ----------------------------------------------------- | ------------------------------------- |
| [0](docs/roadmap/00-safe-mvp.md)               | Safe portable-cabin MVP                               | Complete                              |
| [1](docs/roadmap/01-acquisition-relocation.md) | Survival crafting and command-free relocation         | Complete                              |
| [2](docs/roadmap/02-progression-space.md)      | World-attuned expansion                               | Implemented; retrospective acceptance audit pending |
| [3](docs/roadmap/03-upgrade-interface.md)      | Discoverable cabin upgrade interface                  | 3.1a–3.2b implemented; manual client acceptance pending |
| [4](docs/roadmap/04-household-storage.md)      | Household roles, mailbox and central storage          | Draft                                 |
| [5](docs/roadmap/05-functional-rooms.md)       | Functional rooms                                      | Draft                                 |
| [6](docs/roadmap/06-cabin-companions.md)       | Cabin companions                                      | Draft                                 |
| [7](docs/roadmap/07-production-automation.md)  | Targeted production and room automation               | Draft                                 |
| [8](docs/roadmap/08-enchanting-loadouts.md)    | Enchanting, equipment requisitions and owner loadouts | Draft                                 |
| [9](docs/roadmap/09-connected-cabins.md)       | Connected cabins and safe packed-cabin access         | Draft                                 |
| [10](docs/roadmap/10-cooperative-logistics.md) | Cooperative knowledge, mail and resource logistics    | Draft                                 |
| [11](docs/roadmap/11-compatibility-polish.md)  | Compatibility, balance and presentation pass          | Draft                                 |

## Documentation contract

- The milestone files are authoritative. Requirements and implementation work for a feature belong in the milestone that delivers it.
- A milestone is a coherent player outcome and may contain many deliveries.
- A delivery covers one player-visible feature or one indivisible enabling capability needed by that feature. Independently useful or debatable behavior belongs in a separate delivery.
- Later milestones may explicitly supersede earlier behavior. The later file must name the earlier rule it replaces.
- Cross-milestone dependencies should be links, not duplicate specifications.
- Data-driven decisions remain open only until the milestone that consumes them; that milestone's alignment pass must either settle them or explicitly defer the dependent work.
- The [README](README.md) describes the currently playable build and development workflow, not future design.

Draft milestone files preserve source intent from earlier root and feature specifications until their first alignment pass removes contradictions, closes gaps, and records decisions.

## Domain language

Portable Pocket Cabin is a travelling-home domain in which one persistent pocket home can move its exterior entrance between campsites without carrying the surrounding settlement with it.

**World attunement:** The permanent material pattern chosen once for a world save that resolves variable cabin-upgrade requirements for every player in that world. Avoid “per-player recipes” and “dimension recipes.”

**Resonance:** The active cabin magic carried and shaped by amethyst; it preserves patterns and performs enchanting, automation, and other magical work. Avoid “power” and “mana.”

**Dimensional Logic Core:** The cabin component that defines and controls the rules of one pocket space through copper, redstone, and amethyst resonance. Avoid “computer” and “processor.”

**Dimensional Anchor:** The cabin component that gives a pocket space a stable identity and location so its contents persist when no exterior is deployed. Avoid “fuel” and “power source.”

**Dimensional Folding Core:** The cabin component that forms a controlled connection between an exterior entrance and its anchored pocket space. Avoid “portal core” and “teleporter.”

**Dimensional Foundation:** The assembled dimensional machinery that combines a Logic Core, Dimensional Anchor, and Folding Core into the base of a cabin. Avoid “machine block.”

**Cabin Kit:** An unbound portable cabin containing a Dimensional Foundation whose first successful deployment creates and binds its permanent cabin identity. Avoid “portal pocket cabin” and “cabin item.”

**Cabin palette:** The authoritative floor, wall, roof/ceiling, and door selection captured when a Cabin Kit is crafted, used by its exterior and interior, and preserved across packing, restart, and redeployment. Avoid “cosmetic variant” and “exterior skin.”

**Stable resident:** A specific tamed rideable animal checked into a cabin stable while retaining its individual identity. Avoid “stored mob” and “livestock.”

**Aquatic berth:** A flooded upgraded stable stall that safely houses an eligible aquatic stable resident such as a nautilus. Avoid “aquarium.”

**House cat:** A tamed cat given one cabin as its home, where it lives as a companion rather than as a stable resident or livestock population. Avoid “cat storage” and “cat production.”
