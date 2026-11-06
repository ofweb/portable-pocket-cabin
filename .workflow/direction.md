# Direction

## Goal

Portable Pocket Cabin gives players a home that travels with them in Survival mode. Players can move its entrance and keep their home. Construction outside the cabin stays at each site.

## Intended end state

Players can get a [Cabin Kit](context.md#cabin-kit) through Survival mode and create one lasting pocket home. Players can deploy, pack, and move it between safe sites. The interior, its contents, and purchased upgrades stay with the cabin through travel, restart, and recovery. Players choose wood types for the cabin's appearance through its [Cabin palette](context.md#cabin-palette). The palette gives the home the same materials through each move.

The cabin becomes a useful household as the player explores. Its owner can give access to other players, receive deliveries, and keep resources for household use. Larger [general space](context.md#general-space) and different rooms give space for living, crops, animals, and trees. A house cat can have a home there. Players can learn from exploration to use production, enchanting, and owner loadouts. Players learn real cooking and see how much work ordinary food takes. Players can connect cabins for safe visits and work together. Each cabin keeps its identity and resources. Each cabin must get and install cabin books locally to reveal upgrades. Learned enchantments stay local to each cabin. Connections do not give automation capabilities or learned enchantments.

Production at higher tiers uses advanced upgrade materials to place automation after the stage where gathering its resource should be routine. Previously collected and traded materials satisfy the gate. The owner does not need a separate personal achievement. [The production decision](decisions/pdr/0013-gate-production-through-player-progress.md) states the rule without selecting exact costs or tiers.

Players can use the complete cabin in Survival mode without operator commands. Cabin interfaces show available actions, costs, access, and failures. Optional mod features follow the same behavior and have stated compatibility limits.

### Main room and purpose-specific rooms

The cabin has one main room for general use and dedicated rooms for household activities. Continuous corridors connect its interior, and rooms retain their contents as the cabin grows or travels. The arboretum uses an entrance door as an exception to continuous room passages. The [room purchase and traversal brief](features/B-0005/brief.md) owns shared layout, access, and relocation behavior. The [expansion decision](decisions/pdr/0006-expand-general-space-with-world-materials.md) owns main-room growth.

Growing and animal spaces have their own briefs: [greenhouse](features/B-0019/brief.md), [arboretum / forestry](features/B-0023/brief.md), [stable](features/B-0020/brief.md), [aquatic berth](features/B-0021/brief.md), and [livestock rooms](features/B-0022/brief.md). These briefs own room dimensions, placement, capacity, upgrades, and unresolved room behavior.

### Arboretum woodlands

The arboretum starts as a small room and grows away from its fixed entrance through purchased size upgrades. At its final tier, the cultivated grove keeps its size while a gradual transition leads into effectively endless woodlands belonging to the cabin, with no reachable outer wall or edge. No ceiling is visible. A freestanding entrance wall remains; players can walk around either end into the transition and woodland behind it. Players select planting types for the cultivated core. Transition trees blend those types into surrounding biome species. Tree spacing changes before sparse biome clues and wildlife appear. Different woodland kinds have clearings, ponds, gentle terrain, and non-dangerous wildlife. Bees may sting normally; goats and monsters are excluded. Alex's Mobs is an intended optional source of suitable woodland animals under the project's compatibility profiles. Players can wander far enough to become lost and have a reliable way home. The [arboretum brief](features/B-0023/brief.md) owns sizes and transition markers.

Woodlands use a selected range of familiar forest and orchard biomes, including suitable Biomes O' Plenty biomes. Beyond the first ring, temperate forests connect with matching BOP woodlands; taiga and coniferous forests lead gradually into snowy forests. These groups can appear in any direction farther out. Nearby orchards lie in the second ring, separated from the core and one another by woodland. Other orchards appear occasionally among temperate forests. Two new orchard biomes grow apple and pear trees, or orange and peach trees. They have regularly spaced trees in rows with grassy gaps. Each orchard replants its fixed fruit pair independently of cultivated-core selections.

Fruits Delight supplies orchard trees and occasional natural fruit trees and bushes among normal woodland vegetation. Woodland fruit follows this distribution:

| Woodland biome | Fruit |
|---|---|
| Birch | Pear |
| Oak and flower forest | Apple, orange, fig, kiwi |
| Taiga and BOP Coniferous Forest | Hawberry, blueberry |
| Snowy taiga and BOP Snowy Coniferous Forest | Persimmon, blueberry |
| BOP Woodland and Maple Woods | Apple, pear |
| BOP Seasonal Forest | Apple, orange, fig, kiwi |

Edible raw fruits keep ordinary hunger and saturation without status effects. Existing fruit recipes and prepared-food effects remain part of the intended experience. Fruit features require a compatible Fruits Delight version or port before delivery. Identifying and verifying that support remains technical work. The [source references](references.md#fruits-delight) retain the evidence.

Each cabin keeps the same woodland through packing, movement, and restart. Its current state is saved, but woodland trees are replanted after felling, player-built woodland structures gradually disappear after visits, and dug holes and trenches gradually return to natural terrain. Containers have a grace period, then can disappear with their contents. This gives players time to observe reclamation and retrieve stored items. Player builds in the cultivated room remain permanent. The arboretum brief owns reclamation rules.

Players entering a packed cabin through a permitted cabin connection can use its arboretum normally. Trees, fruit, and animals operate while the cabin is occupied. Live simulation pauses when the packed cabin becomes empty again. Woodland reclamation still runs once whenever the arboretum becomes empty. Paused animals are saved and resume when players return, without simulated breeding, aging, deaths, or item drops during the absence.

On return, catch-up includes planting, replanting, and growth toward maturity under suitable growing conditions, using elapsed server operating time. A cleared planting spot can therefore hold a grown tree when players return. Fruit also grows and ripens during catch-up, stopping at each plant's normal fully ripe state without accumulating extra fruit. Replanted trees must grow before producing fruit. Catch-up does not harvest or count elapsed time twice. Saplings contribute to installation and size-upgrade costs to encourage exploration. These costs cover future planting in the room and woodlands without further sapling payments. Arboretum trees provide no sapling drops to players: the room retains them for replanting. Automatic felling remains a separate capability.

The familiar arboretum entrance door loads the space on entry and returns players to the cabin on exit. Entering the cabin stays fast and does not wait for woodland loading. Beyond the door, players discover the familiar cultivated room first, then walk continuously through the transition into woodlands without another transfer, visible loading, camera jump, or warning. Clearing trees does not break that continuity. The [arboretum brief](features/B-0023/brief.md) owns progression, discovery, return, and door behavior.

### Greenhouse catch-up

When players return to an empty packed cabin, existing greenhouse plants can progress toward their normal mature state under suitable growing conditions. Only time while the server operates counts. This catch-up does not harvest or replant. Those actions require separate automation capabilities. The [greenhouse brief](features/B-0019/brief.md) owns the room behavior.

### Cooking time stations catch-up

Cooking time stations count server time that passed while their chunk was unloaded or their cabin was packed, the way greenhouse plants do. These are the fermentation crock, ageing shelf, drying rack, brine barrel, proofing basket, and dairy pan.
- **What progresses:** each batch can progress to its result.
- **What doesn't:** nothing is collected, refilled, or started.
- **What counts:** only time while the server operates.
- **Heated stations:** stations that need heat keep the B-0045 rule and pause with their fire.

### Mailbox access

Players can deliver mail to a cabin, and its owner can collect it through household interfaces. The [mailbox brief](features/B-0003/brief.md) owns installation prerequisites, access points, permissions, and delivery behavior.

### Manual production rooms

Dedicated production rooms work manually before central storage or automation. Storage integrates with installed rooms, and automation remains a separate progression path. Each room's brief owns its facilities, book requirements, upgrades, dimensions, placement, and storage interactions: [crafting](features/B-0039/brief.md), [smelting](features/B-0040/brief.md), [kitchen](features/B-0041/brief.md), [potions](features/B-0043/brief.md), and [enchanting](features/B-0008/brief.md).

### Cooking and food production

The cabin provides its own cooking system. Players learn real techniques at Minecraft-style cooking stations, follow food from farm to table, and prepare intermediate products such as flour, stock, and cheese. Fuel sets the heat. Cooking is forgiving: food does not burn or spoil. Complex dishes give stronger food benefits. Food changes apply throughout the world: animals drop carcasses, and crops have growing requirements everywhere. Players can cook anywhere with the right equipment, and the cabin kitchen makes cooking more practical. Kitchen automation requires its automation book, and commissioning a recipe consumes some of its finished product. The [cooking concept](cooking/concept.md) describes the system, the [end-state description](cooking/vision.md) shows the finished result, and [PDR-0015](decisions/pdr/0015-build-cooking-into-the-cabin.md) records the choice to build it instead of integrating Farmer's Delight. Cooking features own their recipes, equipment, benefits, and costs.

### Loadouts

Loadouts have their own cabin book and upgrade path, independent of production rooms and their automation. They move existing items between central storage and player inventory. Loadouts do not craft or enchant missing items. Equipment production remains a separate capability.

### Upgrade materials and exploration

Upgrade costs are predefined for each upgrade type and level. Costs do not generate extra requirements from the world, deployment biome, inventory, or earlier purchases. Chosen ingredients can still encourage exploration of different biomes and dimensions and give overlooked items a use.

Exploration requirements use modest quantities. Progression encourages finding materials rather than stockpiling increasing amounts of unusual items. Previously collected and traded items satisfy requirements. Players do not need to revisit a source or prove that they visited it personally.

Costs can include manufactured items. A short ingredient list can represent substantial crafting and gathering through a few complex products. Cost balance must account for their underlying ingredients and work.

General-space expansion uses the wood types selected at first construction, amethyst, and obsidian. Its wood requirements follow the saved Cabin palette. The [expansion decision](decisions/pdr/0006-expand-general-space-with-world-materials.md) states this relationship.

Players see costs only for the next available upgrade in each upgrade type. Later costs stay hidden so progression does not become a shopping list for future tiers.

Travel and changes to inventory do not alter costs.

Each upgrade specifies preferred materials. Each non-vanilla material has a shared fallback chain that can pass through other supported mods and ends with vanilla. The first available material in that chain supplies the requirement. Each fallback specifies a fixed replacement quantity to account for differences in material effort. The [material decision](decisions/pdr/0014-use-preferred-upgrade-materials-with-shared-fallbacks.md) states the rule. Ingredient fallbacks do not change the saved Cabin palette.

Biomes O' Plenty and Alex's Mobs are intended optional sources for suitable upgrade materials. Support follows the project's profile and compatibility limits. Compatible versions and the Alex's Mobs variant remain unresolved. The [mod references](references.md) retain their sources and compatibility questions.

## Boundaries and tensions

The cabin carries the home. Paths, farms, mines, docks, and terrain stay at each site. Interior blocks follow Minecraft behavior while the cabin is deployed. They also operate when players are in a connected packed cabin. An empty packed cabin pauses simulation. When players return, the cabin calculates progress for the time it was empty. Only time while the server operates counts. Progress for that time includes rooms, jobs, and placed blocks such as furnaces and crops. Rooms can make progress up to a limit while the cabin has no active exterior.

Recovery must keep the home after a failed move or lost exterior. A cabin has one active exterior at most and one owner. Each player can have one cabin at most. Connections must keep each cabin's resources, access rules, and identity. Automation has limits. Interior blocks do not simulate while a packed cabin is empty. Optional mod features must have profiles. Compatibility does not include mods without profiles.

## Open questions

### Cabin palette changes

Can an owner select a different Cabin palette by supplying the new wood? The cabin must keep its identity and purchased upgrades. Until a renovation feature is agreed, wood types stay locked to the choices at first construction.

### Automatic packing during a fire

Can a fire near the cabin start automatic packing? A decision is necessary about the effect on players in the cabin.

### More cabin styles

Can the cabin offer different exterior and room designs? A decision is necessary about their effect on cabin identity.

### Cabin names and maps

Can players give cabins names and show their sites on maps? A decision is necessary about who can see these sites.

### Cabin sleep and exterior night

Can sleep in a cabin control the time at its deployed exterior? A decision is necessary about the effect on other players.

### More modded materials

Which additional materials should have profiles? Biomes O' Plenty and Alex's Mobs are intended sources. Compatible versions must be established, including whether Alex's Mobs Continued can satisfy the Alex's Mobs goal.

### Fixed upgrade costs and optional materials

What happens to a displayed or partly funded cost if a mod is later added or removed? Shared material fallbacks must keep upgrades attainable while preserving contributions. The existing [funding decision](decisions/pdr/0007-fund-and-install-cabin-upgrades.md) prevents cost changes from applying to non-empty funds during operation.

### Progress after an empty cabin becomes occupied

Direction must confirm catch-up limits for other rooms, jobs, and placed blocks such as furnaces and crops outside the greenhouse and arboretum. The [greenhouse growth limit](#greenhouse-catch-up) and [arboretum planting, replanting, and growth limit](#arboretum-woodlands) are settled. Automation catch-up limits remain open.
