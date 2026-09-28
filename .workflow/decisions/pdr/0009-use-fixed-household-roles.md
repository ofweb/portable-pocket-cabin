# PDR-0009: Use fixed household roles

Status: Accepted

## Context

The current trust list and entry policy do not describe who may share a cabin as a household. Later storage, rooms, and automation need one stable permission model.

## Decision

Each cabin has one fixed owner. Explicit player UUID assignments make other players residents. Every other player is a guest. Roles are local to one cabin, and every action checks the current role for its target cabin.

All roles may enter a deployed cabin. Only the owner can pack or deploy the cabin and control membership, upgrades, and cabin-wide settings. Residents may use shared household facilities and upgrade funds, but cannot install upgrades or change cabin-wide settings. Guests may inspect the physical cabin and available upgrades. Guests cannot change the cabin interior or use private household resources. A later feature may define a specific guest-safe interaction, such as mailbox delivery.

The owner can add or remove residents while the cabin is deployed or packed. Ownership transfer, private entry, per-player overrides, and custom capability settings are outside this decision.

## Rationale

Fixed roles make permissions predictable across future features. Default guest access lets players visit while direct mutation checks protect the home. Cabin-local assignments preserve each owner's control when cabins connect later.

## Scope

This decision governs household roles and the permission boundary for cabin features. Each feature defines its own role-specific actions. The [household role brief](../../features/B-0002/brief.md) defines the first delivery.

## Consequences

Features must check the current role before each protected change. Open interactions cannot retain authority after a role change. Guest-safe exceptions must be explicit and cannot expose private quantities or settings.
