# Backlog

Each item moves through these statuses:

- **Needs Direction:** the intended outcome is not settled. [Direction](direction.md) records the open question.
- **Ready for Shape:** direction is settled. Shape writes or completes the feature brief.
- **Ready for Design:** the brief is Ready. Design records technical decisions, in a feature design or ADRs.
- **Ready for Implementation:** the design is Ready. Implementation builds and tests it.

Implemented items leave the backlog. Their briefs and designs stay in [features](features/) as the record.

## B-0003: Receiving mailbox

- Status: Ready for Shape
- Value: Let other players deliver items safely through one receiving mailbox that belongs to the cabin.
- Direction: [Direction](direction.md).
- Relationships: Uses B-0002. Requires installed storage in B-0004 and connected-lounge access in B-0009.
- Feature Brief: [Receiving mailbox brief](features/B-0003/brief.md).

## B-0006: House cat

- Status: Ready for Shape
- Value: Let a tamed cat live safely in the general cabin interior as its home.
- Direction: [Direction](direction.md).
- Feature Brief: [House cat brief](features/B-0006/brief.md).
- Relationships: Uses household roles in B-0002.

## B-0007: Product knowledge and automation books

- Status: Ready for Shape
- Value: Let a cabin learn safe product templates and purchase automation capabilities after their books reveal the upgrades.
- Direction: [Direction](direction.md).
- Feature Brief: [Product knowledge brief](features/B-0007/brief.md).
- Relationships: Uses B-0004 and B-0038. Used by B-0024 through B-0027 and B-0036 through B-0037.

## B-0009: Connected cabin hallways

- Status: Ready for Shape
- Value: Let owners confirm cabin connections through a lasting hallway with doors that players cannot break.
- Direction: [Direction](direction.md).
- Feature Brief: [Feature brief](features/B-0009/brief.md).
- Relationships: Uses B-0002. Used by B-0030.

## B-0011: Optional mod compatibility

- Status: Ready for Shape
- Value: Test optional mod profiles with their mods installed, removed, or changed.
- Direction: [Direction](direction.md).
- Feature Brief: [Feature brief](features/B-0011/brief.md).
- Relationships: Applies to optional mod features. Used by B-0035.

## B-0012: Palette renovation

- Status: Needs Direction
- Value: Explore whether owners can select a different saved cabin palette.
- Direction: [Open question](direction.md#cabin-palette-changes).

## B-0013: Emergency fire packing

- Status: Needs Direction
- Value: Explore automatic packing during a fire near the cabin while players stay safe.
- Direction: [Open question](direction.md#automatic-packing-during-a-fire).

## B-0014: More cabin styles

- Status: Needs Direction
- Value: Explore more exterior and room designs that keep cabin identity clear.
- Direction: [Open question](direction.md#more-cabin-styles).

## B-0015: Cabin names and maps

- Status: Needs Direction
- Value: Explore cabin names and site displays on maps.
- Direction: [Open question](direction.md#cabin-names-and-maps).

## B-0016: Cabin sleep and exterior night

- Status: Needs Direction
- Value: Explore whether cabin sleep can control the time outside its deployed exterior.
- Direction: [Open question](direction.md#cabin-sleep-and-exterior-night).

## B-0018: Wider material support

- Status: Needs Direction
- Value: Explore more modded materials with safe profile rules.
- Direction: [Open question](direction.md#more-modded-materials).

## B-0020: Stable

- Status: Ready for Shape
- Value: Let residents house and release tamed animals with profiles that players can ride, while each animal keeps its identity.
- Direction: [Direction](direction.md).
- Relationships: Uses B-0005.
- Feature Brief: [Stable brief](features/B-0020/brief.md).

## B-0021: Aquatic berth

- Status: Ready for Shape
- Value: Add a stable berth with water for a tamed aquatic stable resident, such as a nautilus.
- Direction: [Direction](direction.md).
- Relationships: Extends the stable in B-0020.
- Feature Brief: [Aquatic berth brief](features/B-0021/brief.md).

## B-0022: Livestock room

- Status: Ready for Shape
- Value: Let residents keep livestock with profiles and population limits, and collect products manually.
- Direction: [Direction](direction.md).
- Relationships: Uses B-0005. B-0026 defines automatic actions. Supplies animals, milk, and eggs for cooking in B-0046 and B-0049.
- Feature Brief: [Livestock rooms brief](features/B-0022/brief.md).

## B-0023: Forestry room

- Status: Ready for Shape
- Value: Let residents grow trees in an expanding arboretum whose final tier leads into gentle woodlands.
- Direction: [Arboretum woodlands](direction.md#arboretum-woodlands).
- Relationships: Uses B-0005 with an arboretum entrance-door exception and B-0011 for optional wildlife and fruit compatibility. Owns funded planting and replanting; B-0026 defines automatic felling and output collection.
- Feature Brief: [Arboretum brief](features/B-0023/brief.md).

## B-0024: Bounded crafting jobs

- Status: Ready for Shape
- Value: Plan and operate local crafting jobs with recipes, hard reserves, work time, and safe outputs.
- Direction: [Direction](direction.md).
- Feature Brief: [Crafting jobs brief](features/B-0024/brief.md).
- Relationships: Uses B-0004, B-0007, and B-0039. Used by B-0008, B-0025, B-0028, B-0036, B-0037, and B-0042.

## B-0025: Cooking and brewing jobs

- Status: Ready for Shape
- Value: Let local jobs perform commissioned cooking recipes and brewing processes with limits, using the same steps as manual cooking.
- Direction: [Cooking and food production](direction.md#cooking-and-food-production).
- Feature Brief: [Cooking and brewing brief](features/B-0025/brief.md).
- Relationships: Uses B-0024. Cooking uses B-0041 and B-0045. Brewing uses B-0043.

## B-0026: Room automation

- Status: Ready for Shape
- Value: Add automatic actions that owners can enable independently in greenhouse, livestock, and forestry rooms. Manual use stays the same.
- Direction: [Direction](direction.md).
- Feature Brief: [Room automation brief](features/B-0026/brief.md).
- Relationships: Uses B-0007, B-0024, and room features B-0019, B-0022, or B-0023.

## B-0027: Optional production profiles

- Status: Ready for Shape
- Value: Add process profiles for kiln work, charcoal, and later coal production. Food profiles for optional mods are retired by PDR-0015.
- Direction: [Direction](direction.md).
- Feature Brief: [Optional production brief](features/B-0027/brief.md).
- Relationships: Uses B-0025. Optional mods must have integration profiles.

## B-0028: Equipment requisitions

- Status: Ready for Shape
- Value: Let an owner select enchantments for automation and request equipment through a material and work plan.
- Direction: [Direction](direction.md).
- Feature Brief: [Equipment requisitions brief](features/B-0028/brief.md).
- Relationships: Uses B-0008, B-0024, and B-0038.

## B-0029: Owner loadouts

- Status: Ready for Shape
- Value: Let an owner use loadout rules to move existing items between player inventory and central storage through separate book-unlocked upgrades.
- Direction: [Loadouts](direction.md#loadouts).
- Feature Brief: [Owner loadouts brief](features/B-0029/brief.md).
- Relationships: Uses B-0004 and B-0038. Independent of B-0028 and production rooms.

## B-0030: Safe packed cabin access

- Status: Needs Direction
- Value: Let players use a packed cabin through a connected hallway while they keep a safe exit.
- Direction: [Progress after vacancy](direction.md#progress-after-an-empty-cabin-becomes-occupied).
- Feature Brief: [Feature brief](features/B-0030/brief.md).
- Relationships: Uses B-0009 and the [portable-home decision](decisions/pdr/0001-preserve-the-portable-home.md).

## B-0031: Resource requests and surplus

- Status: Ready for Shape
- Value: Let connected cabins request resources and send surplus with owner confirmation. Transfers identify each cabin and keep storage independent.
- Direction: [Direction](direction.md).
- Feature Brief: [Feature brief](features/B-0031/brief.md).
- Relationships: Uses B-0009 and B-0024. Used by B-0032.

## B-0032: Mailbox automation

- Status: Ready for Shape
- Value: Let an owner enable mailbox delivery with limits for requests and surplus.
- Direction: [Direction](direction.md).
- Feature Brief: [Feature brief](features/B-0032/brief.md).
- Relationships: Uses B-0003 and B-0031.

## B-0033: Survival balance

- Status: Ready for Shape
- Value: Test costs, capacities, rates, limits, and loot for completed capabilities in Survival mode.
- Direction: [Direction](direction.md).
- Feature Brief: [Feature brief](features/B-0033/brief.md).
- Relationships: Applies to completed features. Used by B-0035.

## B-0034: Cabin presentation

- Status: Ready for Shape
- Value: Make cabin controls, status, failures, Cabin windows, models, textures, and sounds clear and consistent.
- Direction: [Direction](direction.md).
- Feature Brief: [Feature brief](features/B-0034/brief.md).
- Relationships: Applies to completed features that players can use. Used by B-0035.

## B-0035: Release verification and documentation

- Status: Ready for Shape
- Value: Verify progress in Survival mode, multiplayer recovery, and optional mod features. Documents state tested versions, migration limits, and extension formats.
- Direction: [Direction](direction.md).
- Feature Brief: [Feature brief](features/B-0035/brief.md).
- Relationships: Uses completed features and B-0011, B-0033, and B-0034.

## B-0036: Local stock targets

- Status: Ready for Shape
- Value: Let owners select local stock targets that start production with limits through known recipes and installed capabilities.
- Direction: [Direction](direction.md).
- Feature Brief: [Stock targets brief](features/B-0036/brief.md).
- Relationships: Uses B-0024.

## B-0037: Upgrade funding plans

- Status: Ready for Shape
- Value: Let an owner confirm a production plan for one upgrade's missing materials. The plan does not install the upgrade automatically.
- Direction: [Direction](direction.md).
- Feature Brief: [Upgrade funding plan brief](features/B-0037/brief.md).
- Relationships: Uses B-0024 and upgrade funds.

## B-0040: Manual smelting room

- Status: Ready for Shape
- Value: Give the household a smelting room that works without storage or automation and uses installed storage automatically.
- Direction: [Manual production rooms](direction.md#manual-production-rooms).
- Relationships: Uses B-0005 and B-0038. Integrates B-0004. Used by B-0042.
- Feature Brief: [Manual smelting room brief](features/B-0040/brief.md).

## B-0041: Manual kitchen room

- Status: Ready for Implementation
- Value: Give the household a kitchen room where it places its own cooking stations, which draw ingredients and fuel from installed central storage.
- Direction: [Manual production rooms](direction.md#manual-production-rooms).
- Relationships: Uses B-0005, B-0038, and B-0045; extends B-0004; used by B-0025.
- Feature Brief: [Manual kitchen brief](features/B-0041/brief.md).
- Design: [Manual kitchen design](features/B-0041/design.md).
- Open proposals: Later stations may need a larger room. Players can place B-0053 prep counters in it.

## B-0042: Smelting jobs

- Status: Ready for Shape
- Value: Let the household request automatic material processing through smelting capabilities unlocked separately from the manual room.
- Direction: [Manual production rooms](direction.md#manual-production-rooms).
- Relationships: Uses B-0004, B-0007, B-0024, and B-0040.

## B-0043: Manual potion room

- Status: Ready for Shape
- Value: Give the household a brewing room that works without storage or automation and uses installed storage automatically.
- Direction: [Manual production rooms](direction.md#manual-production-rooms).
- Relationships: Uses B-0005 and B-0038. Integrates B-0004. Used by brewing in B-0025.
- Feature Brief: [Manual potion room brief](features/B-0043/brief.md).

## B-0044: Book vendors and bookstalls

- Status: Ready for Shape
- Value: Let players buy cabin books from village book vendors, including vendors who claim bookstalls that players place.
- Direction: [Direction](direction.md).
- Feature Brief: [Book vendors brief](features/B-0044/brief.md).
- Relationships: Supplies books for the B-0038 installation flow.

## B-0046: Milling, baking, and dairy

- Status: Ready for Shape
- Value: Let players mill grain into flour, make doughs, starters, and baked goods, and turn milk into butter, cream, yogurt, and cheese. Butter comes from settling milk, skimming cream, churning, and salting.
- Direction: [Cooking and food production](direction.md#cooking-and-food-production).
- Relationships: Uses B-0045. Retires the vanilla bread, cookie, cake, and pumpkin pie recipes once flour exists. Uses milk and eggs from B-0022.
- Open proposals: A sourdough starter begins from flour and water in a fermentation crock and is reusable once fed.

## B-0047: Stocks, sauces, and flavour

- Status: Ready for Shape
- Value: Let players make stocks, dashi, and sauces, and learn the five basic tastes through seasoning and balance.
- Direction: [Cooking and food production](direction.md#cooking-and-food-production).
- Relationships: Uses B-0045.
- Open proposals: B-0047 owns salt, from seawater boiled down in a pot at high heat and rock salt in caves. Optional seasoning slots let salt, herbs, acid, or umami raise a balanced dish's quality, while the basic recipe still makes a good dish.

## B-0048: Fermentation, drying, and preservation

- Status: Ready for Shape
- Value: Let players ferment, dry, smoke, cure, pickle, and age ingredients over time.
- Direction: [Cooking and food production](direction.md#cooking-and-food-production).
- Relationships: Uses B-0045. Retires the vanilla dried kelp recipe once drying exists. Packed-time limits follow the open [catch-up question](direction.md#progress-after-an-empty-cabin-becomes-occupied).
- Open proposals: The vanilla smoker becomes the smoking station, burning low-heat fuel. Koji spores come from a villager trade or a rare find, and each batch returns some. Fish guts ferment into fish sauce.

## B-0049: Butchery and carcasses

- Status: Ready for Shape
- Value: Let animals drop carcasses that players bleed, skin or pluck, and butcher into cuts, offal, bones, and fat, using the whole animal. Fish are scaled, gutted, and filleted.
- Direction: [Cooking and food production](direction.md#cooking-and-food-production).
- Relationships: Uses B-0045. Uses animals from B-0022. The [butchery reference](cooking/butchery.md) lists every part of each carcass.
- Open proposals: Bonito is added as warm-ocean fishing loot. Open: carcasses from baby animals, goats and other animals, and how B-0026 automation handles slaughter.

## B-0050: Cooking crops and foraging

- Status: Ready for Shape
- Value: Add grains, vegetables, herbs, spices, chilies, fruit, and mushrooms with growing requirements, cultivation methods, and wild sources that reward exploration.
- Direction: [Cooking and food production](direction.md#cooking-and-food-production).
- Relationships: Grown in B-0019. Used by B-0045 recipes.

## B-0051: Food benefits

- Status: Ready for Shape
- Value: Give ingredients small benefit contributions that processing develops and complete dishes combine, within bounded limits.
- Direction: [Cooking and food production](direction.md#cooking-and-food-production).
- Relationships: Uses B-0045.
- Open proposals: Seasoning quality from B-0047 raises a dish's benefits.

## B-0052: Fruit pressing and drinks

- Status: Ready for Shape
- Value: Let players press juice, infuse, and make syrups and concentrates for non-alcoholic drinks and cooking.
- Direction: [Cooking and food production](direction.md#cooking-and-food-production).
- Relationships: Uses B-0045.

## B-0053: Prep counter and specialty cooking stations

- Status: Ready for Shape
- Value: Add the prep counter as the kitchen work surface, with the strainer on it, plus the grill, wok, and æbleskive pan, and oil for deep-frying in the cooking pot.
- Direction: [Cooking and food production](direction.md#cooking-and-food-production).
- Relationships: Uses B-0045. Holds counter-top stations from B-0045, B-0046, and B-0047. Players can place it in the B-0041 kitchen. Used by dishes across B-0046 through B-0052. The [station list](cooking/stations.md) gives each station's techniques and step counts.
