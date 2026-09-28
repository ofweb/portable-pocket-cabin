# Delivery 4.1 implementation plan: Household roles

**Intent:** [Household role brief](../../.workflow/features/B-0002/brief.md)

**Status:** Aligned; implementation has not started.

## Outcome

Every player has one cabin-local role with fixed permissions. Owners can promote guests to residents or demote residents to guests without maintaining a separate entry policy.

## Agreed behavior

- Each cabin has exactly one fixed owner. Ownership transfer is out of scope.
- Explicit resident assignments are persisted by player UUID. Every non-owner who is not a resident is a guest.
- All roles may enter a deployed cabin. A cabin has no configurable entry policy.
- Owners retain every cabin capability. Residents inherit the former trusted-player authority to contribute to and withdraw from upgrade funds, but only the owner may install or reverse upgrades.
- Guests may enter, leave and inspect the upgrade interface. They cannot mutate upgrade funds or the cabin interior.
- Guest interior protection rejects block placement and breaking, inventory access, world-affecting item use, entity interaction or attack, item pickup or dropping and equivalent direct player-caused mutations. Player-only actions that do not affect the cabin remain available.
- Future deliveries may define explicit guest-safe interactions. The public mailbox is the first planned exception.
- `/cabin resident add <player>` promotes a guest, and `/cabin resident remove <player>` demotes a resident. The owner cannot be assigned as a resident.
- `/cabin household list` shows the fixed owner and explicit residents and explains that every other player is a guest.
- Role commands work while the cabin is deployed or packed. They resolve online or previously known player profiles. An unknown player or already-satisfied request reports the reason and performs no write.
- Only the owner may manage residents. The earlier `/cabin trust` and `/cabin access` commands are removed without aliases.
- Role changes persist immediately. Open upgrade interfaces refresh before another action; a demoted resident may continue inspecting but cannot mutate a fund.

## Compatibility and failure behavior

- Advance the cabin registry directly to schema 8. Persist only the resident UUID list; derive owner and guest roles.
- Schemas 3–7 fail closed with the existing backup and `just fresh-world` guidance. No trusted-player or entry-policy migration is provided because the mod has not been released.
- Role mutation validates cabin identity, ownership, target resolution and the resulting resident set before commit. Failure leaves persistent state and open interactions unchanged.
- Every protected operation rechecks the actor's current role immediately before mutation. A stale client cannot retain resident authority after demotion.

## Technical approach

- Add `CabinRole` with `OWNER`, `RESIDENT` and `GUEST` values and one server-authoritative permission policy for role resolution and fixed capabilities.
- Replace `CabinEntryPermission` and `trustedPlayers` in `CabinRecord` with an immutable, deduplicated resident UUID list that rejects the owner.
- Update registry copy and mutation paths to preserve residents and provide idempotent owner-authorized promotion and demotion.
- Update entry, upgrade services, menu synchronization, commands, status output, startup checks and reconciliation to use resolved roles.
- Resolve command targets from online or cached server profiles while persisting UUIDs. Display the last resolvable name and fall back to UUID text.
- Identify the cabin containing a player or targeted position and enforce guest protection at server-authoritative block, item, inventory and entity mutation boundaries. Keep the protected exit and read-only upgrade controller paths available.
- Refresh synchronized upgrade permissions after a role revision. Fund services remain the final authorization boundary.
- Remove obsolete entry-policy and trust code rather than retaining compatibility aliases or hidden settings.

No ADR is required: this delivery applies [PDR-0009](../../.workflow/decisions/pdr/0009-use-fixed-household-roles.md) without a new architectural boundary beyond the registry and server services.

## Documentation

- Update the README command list, public-entry behavior, role capabilities and schema-8 fresh-world requirement during implementation.
- Update the household role brief, PDR-0009, and this plan if implementation exposes a new externally observable decision.
- Mark Delivery 4.1 complete only after automated and manual acceptance pass.

## Testing scope

- role resolution and invariants for the fixed owner, explicit residents and default guests
- registry mutation tests for owner authorization, promotion, demotion, duplicate requests and owner-target rejection
- schema validation tests proving schemas 3–7 fail before registry or world mutation
- command tests for deployed and packed cabins, online and cached targets, unresolved names and removed command aliases
- upgrade tests for resident fund access, guest inspection, owner-only installation and reversal, and live demotion of an open menu
- guest-protection tests for block, inventory, item, entity and dropped-item mutation paths plus allowed exit and upgrade inspection
- persistence tests across restart, packing and redeployment
- the complete GameTest and dedicated-server startup/reload gates
- manual multiplayer acceptance for role commands, live permission refresh and representative guest restrictions

## Implementation sequence

1. Add failing role, schema-cutoff and registry-mutation tests.
2. Implement schema 8, role resolution and resident registry mutations.
3. Replace entry-policy and trusted-player command and service checks.
4. Add server-authoritative guest interior protection and focused regressions.
5. Refresh open upgrade-menu permissions after role changes.
6. Update playable documentation and run the complete automated and manual acceptance gates.

## Acceptance criteria

- Every player resolves to exactly one role, and only explicit resident UUIDs require new persistence.
- Every player may enter a deployed cabin; guests cannot mutate its interior or cabin-owned resources.
- Residents can use upgrade funds, while installation and reversal remain owner-only.
- Only the owner can promote or demote residents, including while the cabin is packed.
- A demoted resident loses mutation authority before another open-menu action can commit.
- Unsupported schemas fail before saved registry or pocket-world state changes.
- Resident assignments survive restart, packing and redeployment.
- Automated and manual acceptance pass with no retained entry-policy or trust interface.

## Out of scope

- ownership transfer
- entry policies, private cabins or configurable capability matrices
- explicit guest assignments or a persisted guest list
- household-management screens
- mailbox, central-storage or storage-funding implementation
- compatibility aliases for the removed trust and access commands
- migration of unreleased schemas 3–7
