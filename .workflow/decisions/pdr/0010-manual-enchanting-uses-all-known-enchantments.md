# PDR-0010: Manual enchanting uses all known enchantments

Status: Accepted

## Context

A cabin keeps learned enchantments for manual and automatic work. A slot limit restricts automatic work. Using the same selection for manual work would restrict player choices too.

## Decision

Owners and residents can manually apply any known enchantment to an item that can accept it. Manual enchanting completes on player confirmation and uses amethyst. It does not require an automation slot. Only the owner selects known enchantments for automation slots. Automatic enchanting jobs can use only those selections and take time.

## Rationale

Manual use gives a selected item its enchantment at once. The slot limit and work time apply to automatic requests. Residents can use known enchantments without changing owner selections.

## Scope

This decision applies to manual and automatic enchanting. [B-0008](../../features/B-0008/brief.md) states learning and manual use. [B-0028](../../features/B-0028/brief.md) states automatic equipment requisitions. This decision does not select slot counts or enchanting costs. [Known enchantment](../../context.md#known-enchantment) gives the agreed term.

Removing a known enchantment from an automation slot can pause a job that requires it. Manual use stays available.
