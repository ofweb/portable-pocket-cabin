# PDR-0010: Manual enchanting uses all known enchantments

Status: Accepted

## Context

The cabin keeps learned enchantments and can use them for manual and automated work. A limited slot selection controls automated work. The same limit would restrict manual use if both actions used one selection.

## Decision

Owners and residents can apply any known enchantment manually to an eligible item. Manual enchanting completes when the player confirms it and consumes amethyst. Manual use does not require an automation slot. Only the owner selects known enchantments for automation slots. Automated enchanting jobs can use only the enchantments in those slots and take time.

## Rationale

Manual use lets a player choose an enchantment for a specific item and get the result at once. The slot limit and work time apply to automated requests. A resident can use known enchantments without changing the owner's automation selection.

## Scope

This decision governs the difference between manual and automated enchanting. [B-0008](../../features/B-0008/brief.md) defines learning and manual use. [B-0028](../../features/B-0028/brief.md) defines automated equipment requisitions. This decision does not set the number of slots or enchanting costs.

[Known enchantment](../../context.md#known-enchantment) defines the shared term.

## Consequences

Removing an enchantment from an automation slot can pause a dependent job. The same change does not prevent manual use.
