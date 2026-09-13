# Milestone 8: Cooperative knowledge, mail and resource logistics

**Depends on:** Milestones 5 through 7.

**Outcome:** Connected cabins cooperate without becoming one inventory or leaking private household configuration.

Major scope:

- permanent propagation of installed automation discoveries and learned enchantments
- cabin-local ordinary item, meal and potion knowledge
- owner-published requests and surplus offers
- reserve-aware, attributable and atomic inter-cabin storage transfers
- mailbox fulfilment and surplus-delivery automation
- cabin-specific network status and visibility
- leaving a network without revoking already copied shared discoveries

**Red:** Add failing privacy, reserve, partial-transfer, disconnect, capacity, discovery-propagation and attribution tests.

**Green:** Move one explicitly requested stack between two cabins transactionally before enabling automated fulfilment.

**Refactor:** Keep all remote transfers request-based; no automation may read or consume donor storage directly.

**Exit gate:** Every shared item changes ownership atomically and visibly, every permission is evaluated at the destination cabin, and no combined network inventory exists.


## Shared-discovery specification

_Source: shared-discovery section of the former network specification._

### Shared discoveries

When cabins connect through a hallway, installed automation discoveries and learned enchantments propagate permanently according to the storage and automation specification.

Ordinary learned items, meals and potions do not propagate. Leaving the hallway never removes knowledge already copied.


## Knowledge-sharing boundary

_Source: shared-versus-local knowledge rules from the former storage specification._

### Scope of shared knowledge

Knowledge is divided deliberately:

- **Shared discoveries:** installed automation books and learned enchantments copy permanently to cabins connected through the same hallway network.
- **Local product knowledge:** ordinary item templates, prepared meals and potion variants remain specific to the cabin whose storage received them.

Connecting temporarily is allowed to spread shared discoveries throughout a small friend group. A cabin retains copied discoveries after leaving the network. This is intended cooperative progression.

Every cabin owner independently enables or disables each usable automation. Knowing an ability never silently activates it.


## Resource-logistics specification

_Source: cabin-owned storage-sharing and status sections of the former storage specification._

### Cabin-owned storage sharing

Every cabin retains ownership of its storage. There is no combined network inventory.

An owner may expose surplus through rules such as:

- share this item above a fixed quantity
- retain enough inputs for a configured number of complete owner loadout restocks
- transfer a configured amount on request

Remote automation never consumes directly from donor storage. Instead:

1. A recipient creates a transfer request.
2. Each donor independently evaluates its current sharing and reserve rules.
3. An eligible amount is atomically removed from the donor.
4. The same amount is atomically deposited into the recipient cabin or its configured mailbox destination.
5. Failure rolls the transaction back without partial movement.

Every completed transfer is attributable and visible to both cabins. A recipient job consumes the items only after they have become local storage.

### Status and inspection

Status is always grouped by cabin and job. The system never merges every connected cabin's shortages into one mandatory list.

- Owners see full storage, automation, loadout, reserve, sharing, mailbox, room and network diagnostics.
- Residents see actionable status for facilities they may use, without private owner loadout or sharing configuration.
- Guests may see plain-language cabin warnings and shortages but cannot browse the storage catalogue, exact quantities, private loadouts or mailbox contents.
- Remote inspection exposes no more detail than the inspecting player could see for that destination cabin locally.
- Owners may publish exact requests or surplus quantities to the hallway network.

Failures should name the cabin, job and concrete reason, for example:

```text
Sune's Cabin / Dinner restock: missing mutton
Adam's Cabin / Diamond pickaxe: enchantment Efficiency V is unknown
Alex's Cabin / Greenhouse collection: central storage has no free slot
Sam's Cabin / Shared wood request: retained for five owner loadout restocks
```
