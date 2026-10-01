# PDR-0009: Use fixed household roles

Status: Accepted

## Context

A trust list and entry policy cannot describe household use. Storage, rooms, and automation require one stable permission model.

## Decision

Each cabin has one fixed owner. Assigned player UUIDs identify residents. All other players are guests. Roles are local to one cabin. Each action checks the role for its target cabin before changing state.

All roles can enter a deployed cabin. Only the owner can pack or deploy it and control membership, upgrades, and cabin-wide settings. Residents can use household facilities and upgrade funds. They cannot install upgrades or modify cabin-wide settings. Guests can inspect the cabin and available upgrades. They cannot modify the interior or use private household resources. A feature can define a safe action for guests, such as mailbox delivery.

The owner can add or remove residents while the cabin is deployed or packed. This decision excludes ownership transfer, private entry, per-player overrides, and custom capability settings.

## Rationale

Fixed roles keep permissions consistent across features. Guests can enter, and action checks protect the home. Local assignments keep owner control when cabins connect.

## Scope

This decision applies to household roles and cabin permissions. Each feature states its actions for each role. The [household role brief](../../features/B-0002/brief.md) states the first feature.

Features must validate the role before each protected change. Open interactions cannot keep authority after a role change. Exceptions for guests must be stated and cannot show private quantities or settings.
