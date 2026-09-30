# Context

## Cabin

- Meaning: A cabin is a lasting pocket home with one owner, one interior, and one active exterior at most.
- STE class: Technical name
- Forms: cabins

## Cabin owner

- Meaning: One player owns a given cabin for its lifetime.
- STE class: Technical name
- Forms: cabin owners, owner, owners

## Household role

- Meaning: A household role is a player's permission category in one cabin. Each player is its owner, resident, or guest.
- STE class: Technical name
- Forms: household roles, role, roles
- Distinguish from: A role in one cabin gives no authority in another cabin.

## Resident

- Meaning: A resident is a player whom the cabin owner explicitly assigns to one cabin for shared household use.
- STE class: Technical name
- Forms: residents

## Guest

- Meaning: A guest is a player who is not the cabin owner or an assigned resident. Guests can visit without changing the cabin.
- STE class: Technical name
- Forms: guests

## General space

- Meaning: General space is the main usable cabin interior. Its saved square size sets the protected shell, clear height, and next expansion step.
- STE class: Technical name
- Avoid: Room cell, exterior footprint.

## World attunement

- Meaning: World attunement is the permanent material pattern chosen once for a world save. It sets variable cabin upgrade requirements for every player in that world.
- STE class: Technical name
- Avoid: Per-player recipes, dimension recipes.

## Resonance

- Meaning: Resonance is active cabin magic carried and shaped by amethyst. It preserves patterns and performs enchanting, automation, and other magical work.
- STE class: Technical name
- Avoid: Power, mana.

## Dimensional Logic Core

- Meaning: A Dimensional Logic Core is the cabin component that defines and controls one pocket space through copper, redstone, and resonance.
- STE class: Technical name
- Avoid: Computer, processor.

## Dimensional Anchor

- Meaning: A Dimensional Anchor is the cabin component that gives a pocket space stable identity and location. Its contents persist without a deployed exterior.
- STE class: Technical name
- Avoid: Fuel, power source.

## Dimensional Folding Core

- Meaning: A Dimensional Folding Core is the cabin component that makes a controlled connection between an exterior entrance and its anchored pocket space.
- STE class: Technical name
- Avoid: Portal core, teleporter.

## Dimensional Foundation

- Meaning: A Dimensional Foundation assembles a Dimensional Logic Core, Dimensional Anchor, and Dimensional Folding Core into cabin machinery.
- STE class: Technical name
- Avoid: Machine block.

## Cabin Kit

- Meaning: A Cabin Kit is an unbound portable item that contains a Dimensional Foundation. Its first successful deployment creates and binds a permanent cabin identity.
- STE class: Technical name
- Avoid: Portal pocket cabin, cabin item.

## Cabin palette

- Meaning: A Cabin palette is the authoritative floor, wall, roof or ceiling, and door selection set when a Cabin Kit is crafted. Exterior and interior keep it through packing, restart, and redeployment.
- STE class: Technical name
- Avoid: Cosmetic variant, exterior skin.

## Stable resident

- Meaning: A stable resident is a specific tamed rideable animal checked into a cabin stable. It retains its individual identity.
- STE class: Technical name
- Avoid: Stored mob, livestock.

## Aquatic berth

- Meaning: An aquatic berth is a flooded upgraded stable stall that safely houses an eligible aquatic stable resident, such as a nautilus.
- STE class: Technical name
- Avoid: Aquarium.

## House cat

- Meaning: A house cat is a tamed cat that has one cabin as its home.
- STE class: Technical name
- Avoid: Cat storage, cat production.

## Central storage

- Meaning: Central storage is one cabin-owned inventory for household items. It is separate from placed inventories and upgrade funds.
- STE class: Technical name

## Product template

- Meaning: A product template is cabin knowledge of a safe item identity and allowed variant data. It persists after the source leaves storage and grants no production ability.
- STE class: Technical name
- Forms: product templates

## Cabin book

- Meaning: A cabin book is a physical item that reveals one or more cabin upgrades when installed. Blank and enchanted books are separate.
- STE class: Technical name
- Forms: cabin books

## Automation book

- Meaning: An automation book is a cabin book that reveals one or more automation upgrades. It does not grant an automation capability.
- STE class: Technical name
- Forms: automation books

## Book vendor

- Meaning: A book vendor is a villager profession that sells cabin books and uses a bookstall job site.
- STE class: Technical name
- Forms: book vendors, vendor, vendors

## Bookstall

- Meaning: A bookstall is the job site block for the book vendor profession.
- STE class: Technical name

## Automation capability

- Meaning: An automation capability is a persistent cabin ability from a purchased automation upgrade. The owner can enable or disable it.
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

- Meaning: A hard reserve is an owner-set minimum for one item in central storage. Automation cannot consume below it, but players can withdraw manually.
- STE class: Technical name
- Forms: hard reserves
- Distinguish from: A hard reserve holds no specific stacks. A job reservation commits specific resources to one job.

## Job reservation

- Meaning: A job reservation is the specific input quantity and output capacity committed to one production job. Committed inputs are unavailable to other jobs and players.
- STE class: Technical name
- Forms: job reservations

## Stock target

- Meaning: A stock target is an owner-set desired quantity of one product in central storage. It can trigger production but does not protect items from consumption.
- STE class: Technical name
- Forms: stock targets

## Process profile

- Meaning: A process profile is a declared production path with inputs, outputs, returns, by-products, and work duration.
- STE class: Technical name
- Forms: process profiles
- Distinguish from: A recipe identifies an item transformation. An integration profile declares support for optional content.

## Production plan

- Meaning: A production plan resolves one request before resources commit. It lists chosen recipes or processes, quantities, intermediates, outputs, total work, and required capacity.
- STE class: Technical name
- Forms: production plans

## Production job

- Meaning: A production job is committed, persisted execution of one production plan. It owns reservations, work progress, and blocking state through restart and packing.
- STE class: Technical name
- Forms: production jobs

## Upgrade fund

- Meaning: An upgrade fund holds cabin-owned materials for one stable upgrade target, capped by exact requirements. A cabin can hold several funds but no unassigned material wallet.
- STE class: Technical name
- Forms: upgrade funds, fund, funds
- Avoid: Tracked upgrade, general storage, upgrade chest.

## Upgrade contribution

- Meaning: An upgrade contribution is required material deliberately transferred into one upgrade fund.
- STE class: Technical name
- Avoid: Inventory payment, automatic collection.

## Upgrade installation

- Meaning: Upgrade installation is the owner's deliberate commitment of a fully funded upgrade. It consumes the fund and applies the purchased effect.
- STE class: Technical name
- Avoid: Automatic purchase, crafting the upgrade.

## Purchased upgrade

- Meaning: A purchased upgrade is an installed upgrade effect owned by the cabin. It is distinct from a funded upgrade that is not installed.
- STE class: Technical name
- Avoid: Funded upgrade.

## Revealed upgrade

- Meaning: A revealed upgrade is a target made available when the owner installs a cabin book. It still needs funding and installation.
- STE class: Technical name
- Forms: revealed upgrades

## Upgrade refund receipt

- Meaning: An upgrade refund receipt records the exact materials paid for one reversible upgrade step. The cabin retains it for a later owner-authorized refund.
- STE class: Technical name
- Avoid: Current recipe, refund value.

## Cabin window

- Meaning: A cabin window is a purchased, tiered window on an eligible cabin wall that represents conditions outside the deployed cabin.
- STE class: Technical name
- Avoid: Real portal, cosmetic glass.

## Window tier

- Meaning: A window tier is the purchased size stage of a single cabin window.
- STE class: Technical name
- Avoid: Cabin window level, wall tier.

## Generated cabin structure

- Meaning: A generated cabin structure is a set of world blocks created from saved cabin data.
- STE class: Technical name
- Forms: generated cabin structures
- Distinguish from: Packing removes the exterior entrance and keeps the interior and its contents.
