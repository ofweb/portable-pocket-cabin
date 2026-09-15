# Delivery 3.1 implementation plan: Tracked upgrade interface and fund

**Intent:** [Milestone 3: Upgrade interface](../roadmap/03-upgrade-interface.md)

**Scope:** Delivery 3.1 only. Configurable window purchasing, tiers, refunds and schema 5 state remain in Delivery 3.2. Existing automatic windows must remain unchanged.

**Status:** Implemented historical plan. Its one-tracked-fund interaction is superseded by the
aligned [Delivery 3.1a correction](03-1a-per-upgrade-fund-interface.md), which is not yet implemented.

This file records the design that produced the currently playable interface. It is not the action
plan for further work and must not be used to restore tracking after Delivery 3.1a is accepted.

**Post-implementation correction:** Command-created cabins may carry the old smooth-stone debug
platform into their permanent cell. Interior generation and controller opening remove only its
recognizable exposed rim so it cannot block the first expansion. Fund ejection uses the usable-room
side of the front-wall controller. Regression tests cover both coordinates and expansion validation.

## Design mapping

- `CabinRecord` and `CabinRegistry` remain the authoritative durable owner of tracking, contributed stacks and an in-progress installation.
- `CabinUpgradeDefinitions` remains the reloadable source of general-space costs and world-attuned ingredient resolution.
- A catalog exposes only the next general-space offer and compares a tracked requirement snapshot with current definitions.
- A server-side upgrade service owns all state transitions and returns explicit outcomes; menus never mutate registry state directly.
- The existing interior Lodestone opens a custom menu. A dedicated deposit control transfers only the offered stack into the requirement-capped fund; progress is rendered from synchronized authoritative state.
- A client entrypoint registers the custom screen and remains isolated from dedicated-server initialization.
- General-space installation uses a persisted operation record and an idempotent reconciliation path around the existing expansion world service.

## Task 1: Persist upgrade tracking and migrate schema 3 to 4

**Requirement:** Tracking, resolved requirements and actual contributed stacks survive restart, packing and redeployment; schema 3 saves migrate explicitly without changing automatic windows.

### RED

- Add codec/GameTest coverage that a schema 3 registry loads as schema 4 with no tracked upgrade, empty fund and unchanged cabin progression.
- Add a round-trip test for a tracked general-space target, its resolved requirement snapshot, contributed `ItemStack` data and an optional installation operation.
- Add a lifecycle-preservation GameTest that records a partially funded upgrade, exercises representative access-record changes plus packing and redeployment, and asserts the exact target, requirement snapshot and contributed stacks remain unchanged after every transition.
- Extend the dedicated-server seed/reload check with a partially funded tracked upgrade and assert it is restored exactly after restart.
- Retain regression coverage proving schema 2 remains rejected without mutating the save.
- Expected failure: schema 3 is currently rejected and `CabinRecord` has no upgrade state.
- If a test passes unexpectedly: verify it is exercising the on-disk schema gate rather than only optional codec defaults.

### GREEN

- Introduce codec-backed types for a stable general-space target, resolved item/count requirements, fund stacks and the installation operation.
- Add optional upgrade state to `CabinRecord` with empty defaults for schema 3 data.
- Advance `CabinRegistry` to schema 4, accept and mark schema 3 data for rewrite, and continue rejecting older or future schemas.
- Preserve upgrade state in every existing `CabinRecord` copy path and cabin lifecycle mutation.

### REFACTOR

- Consolidate record-copy helpers so later lifecycle changes cannot silently discard upgrade state.
- Keep domain records immutable and validate duplicate requirements, invalid counts and fund/requirement mismatches at construction.

## Task 2: Resolve and track the next general-space offer

**Requirement:** The owner can track exactly one eligible next expansion; its displayed requirements are resolved and captured, duplicate ingredients are consolidated, and definition drift makes it stale.

### RED

- Test that the catalog exposes only size `current + 1`, reports maximum reached, and resolves the attuned plank item.
- Test consolidation when multiple definition ingredients resolve to the same item.
- Test owner-only tracking, refusal to replace an existing tracked target, and refusal of unavailable or forged targets.
- Test that a changed or removed loaded definition marks the snapshot stale and blocks further contribution or installation without changing its fund.
- Expected failure: the current controller has no offer or tracking state and resolves requirements only for immediate inventory payment.
- If a test passes unexpectedly: confirm it asserts persisted tracking rather than the provisional status message.

### GREEN

- Extract offer resolution into a catalog that returns a stable target, effect description, resolved consolidated requirements and blocking state.
- Add owner-authorized `track` behavior to the upgrade service and persist the captured snapshot.
- Compare the snapshot with freshly resolved current definitions whenever contribution or installation is requested.

### REFACTOR

- Remove duplicated ingredient description and resolution logic from the provisional purchase path.
- Keep effect dispatch code-defined; do not add arbitrary datapack-executable upgrade types.

## Task 3: Accept deliberate, capped contributions

**Requirement:** Owners and currently authorized trusted players can deposit only missing exact materials; no player inventory is scanned, creative mode does not bypass funding, and the fund is not general storage.

### RED

- Test exact-item acceptance, partial contributions, multiple contributors, per-requirement caps, wrong-item rejection and excess returned to the offered stack.
- Test that the owner and an authorized trusted player may contribute while an untrusted, access-revoked or unrelated player may not.
- Test that creative players must contribute the same complete requirements.
- Test that contributed stack data persists and that no hopper, block inventory or externally discoverable container owns the fund.
- Test that the service exposes no individual-withdrawal operation; only owner-confirmed untracking may eject the complete fund.
- Test stale and concurrent contribution requests, asserting that every offered item remains either with the contributor or in the durable fund and is never duplicated or lost.
- Expected failure: the current code scans and consumes only the owner's inventory and creative bypasses all material costs.
- If a test passes unexpectedly: verify the contribution path receives one deliberately offered stack rather than an inventory reference.

### GREEN

- Implement contribution as an immediate server transaction over one deliberately offered stack, returning accepted and remainder counts without retaining items in an intermediate slot between actions.
- Persist accepted stacks against their captured requirement and flush the registry after each successful mutation.
- Require current cabin access and owner-or-trusted contributor authority on every call.

### REFACTOR

- Centralize requirement accounting so the catalog, contribution caps, completeness check and interface progress use one calculation.
- Keep the fund free of `Container`, block-entity, hopper or general automation exposure.

## Task 4: Stop tracking and physically eject the fund

**Requirement:** Only the owner can stop tracking after confirmation; every contributed stack is dropped beside the interior controller and ordinary item-entity pickup/despawn/crash semantics apply.

### RED

- Test owner-only untracking, stale service-request rejection, complete stack-data preservation and clearing only after the handled ejection succeeds.
- Test that selecting another offer never implicitly ejects or replaces the fund.
- Test that open viewers can no longer contribute after untracking and observe the cleared state.
- Expected failure: no untracking or fund ejection path exists.
- If a test passes unexpectedly: confirm item entities were spawned at the cabin's interior controller rather than inserted into an inventory.

### GREEN

- Add a confirmed untrack action that invalidates contribution views, spawns one or more item entities beside the controller and clears tracking as one server-handled operation.
- Preserve actual stack components and rely on ordinary item-entity behavior after spawning.

### REFACTOR

- Share ejection positioning and stack-splitting helpers with the future Delivery 3.2 refund path without implementing refunds now.
- Keep cancellation distinct from installation failure and from selecting a different offer.

## Task 5: Install general space with restart reconciliation

**Requirement:** Funding never auto-installs; only the owner commits a fully funded valid target; failures leave fund and tracking unchanged; an interruption cannot duplicate expansion or consume materials without the effect.

### RED

- Test rejection for incomplete funding, non-owner commit, stale definitions, changed cabin size, inactive controller and obstructed expansion, with the fund untouched in each case.
- Test successful expansion consumes the complete fund, advances exactly one size, clears tracking and refreshes the existing automatic windows.
- Test replay/idempotence and a persisted interruption at each installation boundary through startup reconciliation.
- Expected failure: the current expansion changes world blocks and then player inventory without a persisted upgrade operation.
- If a test passes unexpectedly: confirm it covers a simulated restart between durable intent, world mutation and final fund consumption.

### GREEN

- Revalidate identity, lifecycle, controller access, ownership, snapshot, current definition, funding and expansion volume before recording installation intent.
- Persist and flush an installation operation before applying the deterministic expansion effect.
- Make the expansion application safe to resume, then atomically commit progression, consume the fund and clear tracking/operation.
- Extend startup reconciliation to finish an interrupted installation before ordinary players can interact.

### REFACTOR

- Separate validation, durable transition and world-effect application so failure paths cannot partially consume the fund.
- Reuse the existing `PocketDimension` expansion checks and shell construction rather than introducing a second geometry implementation.

## Task 6: Replace controller gestures with a server-authoritative menu

**Requirement:** Either use of the interior Lodestone opens the interface; all eligible entrants may inspect, only the owner controls tracking/installation, trusted players may contribute, and concurrent viewers stay synchronized.

### RED

- Add integration coverage for opening from normal and sneak use, server validation of every action, forged/stale action rejection, contribution transfer remainders, shared-view updates and access loss while open.
- Test that funded rows cannot be clicked or shift-clicked to withdraw contributions and that forged withdrawal actions are rejected.
- Test menu close, disconnect, access loss and concurrent deposits during transfer, asserting that every offered item remains either with the player or in the durable fund and is never duplicated, lost or stranded.
- Test that the old chat-only status and sneak-to-purchase path can no longer consume inventory or expand the cabin.
- Expected failure: `CabinUpgrades.useController` currently branches on sneaking and has no menu.
- If a test passes unexpectedly: confirm the test exercises the registered controller callback and menu action path.

### GREEN

- Register a common menu type and open it from the existing controller callback for both normal and sneak use.
- Synchronize offer, tracking, requirements, contribution totals, permissions and blocking reasons from current server state.
- Provide a stateless deposit control plus player inventory transfer behavior; atomically route accepted items through the service and leave wrong/excess items with the player.
- Render funded requirement rows as display-only progress, and define no menu or protocol operation for withdrawing individual contributions.
- Revalidate cabin/menu identity and permissions for every button or transfer and refresh all viewers observing the same cabin revision.

### REFACTOR

- Keep protocol state compact and explicit; do not expose the registry fund as a general menu container or capability.
- Isolate menu orchestration from catalog and service rules so the same rules remain directly testable.

## Task 7: Add the client screen and manual acceptance path

**Requirement:** The interface communicates current/max size, attuned wood, next offer, effect, requirements, funding progress, permissions, confirmations and actionable failures without placing client classes on a dedicated server.

### RED

- Add a dedicated-server classloading/startup assertion that the common entrypoint does not reference client-only classes.
- Define a manual acceptance checklist covering owner and trusted-player views, mouse and shift-click deposits, caps/remainders, stale and obstruction messages, confirmation/cancellation and live shared progress.
- Expected failure: there is no client entrypoint or screen registration.
- If the server assertion passes unexpectedly: that only proves the current no-screen build; repeat it after client registration exists.

### GREEN

- Add a client entrypoint and register a custom Cabin Upgrades screen for the common menu type.
- Implement the agreed information hierarchy, scrolling where required, owner-only action controls and explicit confirmation for fund ejection.
- Present server-provided blocking and failure messages inline; keep exact visual styling modest and within the deferred art-direction scope.

### REFACTOR

- Keep presentation strings and layout constants client-side while all decisions remain server-side.
- Verify the client entrypoint is declared separately in `fabric.mod.json` and the common initializer remains dedicated-server safe.

## Task 8: Acceptance, documentation and regression gate

**Requirement:** Delivery 3.1 is accepted only with its agreed automated, restart, regression, manual and documentation coverage, while existing windows remain unchanged.

### RED

- Before updating usage text, follow the current README and record where its controller interaction contradicts the implemented interface.
- Run the focused new tests and demonstrate they fail against the pre-implementation behavior.

### GREEN

- Update the README with controller, tracking, contribution, ejection and installation instructions and mark Delivery 3.1 implemented in the roadmap.
- Run the complete GameTest suite and the two-run dedicated-server startup/reload check.
- Manually complete the client acceptance checklist with an owner and trusted player and confirm automatic window profiles still update across lifecycle and exterior conditions.

### REFACTOR

- Remove obsolete chat-purchase wording and dead direct-inventory purchase helpers.
- Review the final diff against Delivery 3.1 only and leave all configurable-window code for Delivery 3.2.

## Manual client acceptance checklist

- [ ] An owner can open **Cabin Upgrades** with both normal-use and sneak-use of the interior Lodestone; neither gesture purchases directly or sends the former status-only message.
- [ ] A currently authorized trusted player sees the same offer and live funding totals but has no track, stop, or install controls.
- [ ] A player without current cabin entry access cannot open or continue using the interface.
- [ ] Clicking the green deposit control with a held required stack and shift-clicking a required inventory stack both fund only the missing count; wrong and excess items stay with the player.
- [ ] Two viewers contributing in succession see the same capped totals, and closing or disconnecting with items in hand does not leave an item in the menu.
- [ ] Fully funding an upgrade does not install it automatically; the owner can install it only while its target remains valid.
- [ ] An obstruction or stale datapack definition produces an inline blocking message and leaves tracking and the fund unchanged.
- [ ] Stopping tracking requires the explicit ejection confirmation, drops exact contributed stacks beside the interior controller, and refreshes another open view.
- [ ] Packing and redeploying preserve a partially funded upgrade, and the existing automatic window profiles still follow exterior conditions afterward.
- [ ] A dedicated server starts without loading `PortablePocketCabinClient` or `CabinUpgradeScreen`.
