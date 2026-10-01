# Context

## Cabin

- Meaning: A cabin is a lasting pocket home with one owner, one interior, and one active exterior at most.
- STE class: Technical name
- Forms: cabins

## Cabin owner

- Meaning: One player is the cabin owner for the life of that cabin.
- STE class: Technical name
- Forms: cabin owners, owner, owners

## Household role

- Meaning: A household role gives a player's permissions in one cabin. Each player is its owner, resident, or guest.
- STE class: Technical name
- Forms: household roles, role, roles
- Distinguish from: A role in one cabin gives no authority in a different cabin.

## Resident

- Meaning: A resident is a player assigned by the cabin owner to one cabin for household use.
- STE class: Technical name
- Forms: residents

## Guest

- Meaning: A guest is a player who is not the cabin owner or an assigned resident. Guests can enter without changing the cabin.
- STE class: Technical name
- Forms: guests

## General space

- Meaning: General space is the square cabin interior that players can use. Its saved size controls the protected structure, clear height, and next expansion step.
- STE class: Technical name
- Avoid: `Room cell`, `exterior footprint`.

## World attunement

- Meaning: World attunement is a permanent material selection for a world save. It selects variable cabin upgrade requirements for all players in that world.
- STE class: Technical name
- Avoid: `Per-player recipes`, `dimension recipes`.

## Resonance

- Meaning: Resonance is active cabin magic carried and shaped by amethyst. It preserves patterns and performs enchanting, automation, and other magical work.
- STE class: Technical name
- Avoid: `Power`, `mana`.

## Dimensional Logic Core

- Meaning: A Dimensional Logic Core is the cabin component that controls one pocket space through copper, redstone, and resonance.
- STE class: Technical name
- Avoid: `Computer`, `processor`.

## Dimensional Anchor

- Meaning: A Dimensional Anchor is the cabin component that gives a pocket space stable identity and location. Its contents persist without a deployed exterior.
- STE class: Technical name
- Avoid: `Fuel`, `power source`.

## Dimensional Folding Core

- Meaning: A Dimensional Folding Core makes a controlled connection between an exterior entrance and its pocket space with a permanent identity.
- STE class: Technical name
- Avoid: `Portal core`, `teleporter`.

## Dimensional Foundation

- Meaning: A Dimensional Foundation combines a Dimensional Logic Core, Dimensional Anchor, and Dimensional Folding Core into cabin machinery.
- STE class: Technical name
- Avoid: `Machine block`.

## Cabin Kit

- Meaning: A Cabin Kit is an unbound portable item that contains a Dimensional Foundation. Its first completed deployment creates and binds a permanent cabin identity.
- STE class: Technical name
- Avoid: `Portal pocket cabin`, `cabin item`.

## Cabin palette

- Meaning: A Cabin palette gives floor, wall, roof or ceiling, and door materials selected during Cabin Kit crafting. Exterior and interior keep them through packing, restart, and redeployment.
- STE class: Technical name
- Avoid: `Cosmetic variant`, `exterior skin`.

## Stable resident

- Meaning: A stable resident is a tamed animal that players can ride and check into a cabin stable. It keeps its identity.
- STE class: Technical name
- Avoid: `Stored mob`, `livestock`.

## Aquatic berth

- Meaning: An aquatic berth is an upgraded stable stall with water for an aquatic stable resident with a profile, such as a nautilus.
- STE class: Technical name
- Avoid: `Aquarium`.

## House cat

- Meaning: A house cat is a tamed cat that has one cabin as its home.
- STE class: Technical name
- Avoid: `Cat storage`, `cat production`.

## Central storage

- Meaning: Central storage is one inventory that belongs to the cabin for household items. It is independent of placed inventories and upgrade funds.
- STE class: Technical name

## Product template

- Meaning: A product template records a safe item identity and permitted variant data in a cabin. It stays after source removal and gives no production capability.
- STE class: Technical name
- Forms: product templates

## Cabin book

- Meaning: A cabin book is an item that reveals one or more cabin upgrades when installed. It differs from empty books and enchanted books.
- STE class: Technical name
- Forms: cabin books

## Automation book

- Meaning: An automation book is a cabin book that reveals one or more automation upgrades. It does not give an automation capability.
- STE class: Technical name
- Forms: automation books

## Book vendor

- Meaning: A book vendor is a villager profession with cabin book trades and a bookstall job site.
- STE class: Technical name
- Forms: book vendors, vendor, vendors

## Bookstall

- Meaning: A bookstall is the job site block for the book vendor profession.
- STE class: Technical name

## Automation capability

- Meaning: An automation capability is a cabin action from a purchased automation upgrade that stays through restart. The owner can select it to operate or stop.
- STE class: Technical name
- Forms: automation capabilities

## Known enchantment

- Meaning: A known enchantment is a cabin's permanent record of one enchantment type and its highest learned level.
- STE class: Technical name
- Forms: known enchantments

## Application limit

- Meaning: An application limit is the highest enchantment level a room can apply. A cabin can know higher levels.
- STE class: Technical name
- Forms: application limits

## Hard reserve

- Meaning: A hard reserve is a minimum item quantity selected by the owner in central storage. Automation cannot use items below it, but players can withdraw manually.
- STE class: Technical name
- Forms: hard reserves
- Distinguish from: A hard reserve holds no stacks. A job reservation commits resources to one job.

## Job reservation

- Meaning: A job reservation commits an input quantity and output capacity to one production job. Other jobs and players cannot use committed inputs.
- STE class: Technical name
- Forms: job reservations

## Stock target

- Meaning: A stock target is an item quantity selected by the owner for one product in central storage. It can start production but permits item consumption.
- STE class: Technical name
- Forms: stock targets

## Process profile

- Meaning: A process profile states a production path with inputs, outputs, returned items, by-products, and work time.
- STE class: Technical name
- Forms: process profiles
- Distinguish from: A recipe identifies how to make an item. An integration profile states optional mod compatibility.

## Production plan

- Meaning: A production plan describes one request before resource commitment. It lists recipes or process profiles, quantities, intermediate products, outputs, total work, and necessary capacity.
- STE class: Technical name
- Forms: production plans

## Production job

- Meaning: A production job is one committed production plan with saved reservations, work progress, and blocking state. It keeps that state through restart and packing.
- STE class: Technical name
- Forms: production jobs

## Upgrade fund

- Meaning: An upgrade fund holds materials that belong to the cabin for one stable upgrade target, up to its specified requirements. Multiple funds cannot hold materials without a target.
- STE class: Technical name
- Forms: upgrade funds, fund, funds
- Avoid: `Tracked upgrade`, `general storage`, `upgrade chest`.

## Upgrade contribution

- Meaning: An upgrade contribution is a necessary material that a player transfers into one upgrade fund.
- STE class: Technical name
- Avoid: `Inventory payment`, `automatic collection`.

## Upgrade installation

- Meaning: Upgrade installation is the owner confirmation that commits a fully funded upgrade. It uses the fund and applies the purchased effect.
- STE class: Technical name
- Avoid: `Automatic purchase`, `crafting the upgrade`.

## Purchased upgrade

- Meaning: A purchased upgrade is an installed upgrade effect that belongs to the cabin. A funded upgrade without installation differs from it.
- STE class: Technical name
- Avoid: `Funded upgrade`.

## Revealed upgrade

- Meaning: A revealed upgrade is a target made available when the owner installs a cabin book. It requires funding and installation.
- STE class: Technical name
- Forms: revealed upgrades

## Upgrade refund receipt

- Meaning: An upgrade refund receipt records the specified materials used to purchase one upgrade step that the owner can reverse. The cabin keeps it for that refund.
- STE class: Technical name
- Avoid: `Current recipe`, `refund value`.

## Cabin window

- Meaning: A cabin window is a purchased window with tiers on a cabin wall that permits installation. It shows exterior conditions for the deployed cabin.
- STE class: Technical name
- Avoid: `Real portal`, `cosmetic glass`.

## Window tier

- Meaning: A window tier is a purchased size step for one cabin window.
- STE class: Technical name
- Avoid: `Cabin window level`, `wall tier`.

## Generated cabin structure

- Meaning: A generated cabin structure is a set of world blocks created from saved cabin data.
- STE class: Technical name
- Forms: generated cabin structures
- Distinguish from: Packing removes the exterior entrance and keeps the interior and its contents.
