# Players butcher whole animals and gather seafood

Status: Ready
Feature ID: B-0049

## Goal

Meat comes from an animal that has to be bled, skinned or plucked, and butchered, and most of it isn't steak. Animals drop whole carcasses that players dress on a hanging hook and break down on a butcher block, in real butchery order. Bones, fat, blood, and offal all have uses, so players learn to use the whole animal. Fish are scaled, gutted, and filleted, and seafood comes from the places it really lives.

It is phase 5 of the [cooking program](../../cooking/program.md), and completes stocks, sausages, cured meats, rendered fats, and calf rennet. The [butchery reference](../../cooking/butchery.md) lists every part of every animal.

## Stories and acceptance

### S1: Get a carcass

Story: A player slaughters an adult animal and gets its carcass.

Acceptance:

- **Carcasses:** adult cows, pigs, sheep, chickens, rabbits, goats, and ducks drop a carcass of their kind instead of meat. Their other drops change to come from butchery.
- **Unchanged:** baby animals drop nothing, as in vanilla. Horses and other animals stay unbutcherable.
- **Goats:** goats give chevon.
- **Ducks:** a new animal of rivers and swamps, which players can breed.
- **Blood:** using a glass bottle on a fresh carcass collects blood. It is optional and must come first.

### S2: Dress the carcass

Story: A player hangs a carcass on a hanging hook and skins or plucks it.

Acceptance:

- **Hanging hook:** a click station on a wall or ceiling.
- **Skin and pluck:**
  - Cows, sheep, rabbits, and goats are skinned with the knife.
  - Chickens and ducks are plucked by hand.
  - Pigs are scraped and keep their skin.
- **Scalding:** chickens, ducks, and pigs must be scalded in the cooking pot at low heat first. The hook says so when they aren't.
- **Yields:** dressing gives hide, fleece, feathers, or bristles, and a dressed carcass.

### S3: Butcher

Story: A player breaks a dressed carcass down on the butcher block.

Acceptance:

- **Butcher block:** a click station.
- **Cuts in order:** each cleaver use takes the next part in a real order: primal cuts, then offal, then bones and fat. Sneak-use breaks the carcass down completely.
- **Must be dressed:** an undressed carcass is refused, with a message saying it must be skinned or plucked first.
- **Where parts go:** to the player's inventory, with overflow dropped beside the block. In the [B-0041](../B-0041/brief.md) kitchen, overflow goes to storage.
- **Whole birds:** a dressed chicken or duck can also roast whole in the oven.

### S4: Prepare fish and gather seafood

Story: A player catches fish and gathers shellfish, then prepares them.

Acceptance:

- **New fish:** anchovy and bonito join fishing in warm oceans, and herring in cold oceans.
- **Shellfish:** mussels grow on stony shores and cold-ocean rocks, and oysters in lukewarm oceans and mangrove swamps. Both are blocks gathered by hand.
- **Prawn trap:** a new block placed in warm water that collects prawns over time.
- **Fish on the block:** the knife scales and guts a fish, then fillets it. Pufferfish give only bones, for stock.

### S5: Use every part

Story: A player turns by-products into food and materials.

Acceptance:

- **Every part has a use,** as the butchery reference lists:
  - **Bones:** stock in the pot, and bone meal in the mill.
  - **Fat:** rendered into lard, tallow, and schmaltz.
  - **Skin, trotters, and feet:** simmered for gelatin.
  - **Hide and fleece:** leather and wool.
  - **Fish scales and guts:** compost, and fish sauce in the crock.
- **Meat grinder:** a container station that minces meat and fish.
- **Sausage stuffer:** a container station that fills casings for sausages and blood sausage.

### S6: Complete the chains

Story: With carcasses available, the dishes that need them become makeable.

Acceptance:

- Every recipe and product held back for butchery ships here. This includes stocks, cured meats, sausages, rendered fats, gelatin, fish sauce, and calf rennet, which joins cardoon rennet as a second source.

## Feature-wide constraints and acceptance

- New stations follow B-0045 rules: recipe types, saving, breaking, hoppers where they make sense, and household permissions inside a cabin.
- **Lost drops:** vanilla meat, leather, feather, rabbit hide, and wool drops from killing are replaced, and come from butchery instead. Shearing live sheep still gives wool.
- Every new item appears in the cookbook, and the butchery steps are explained.
- Carcass parts and their order are data, so datapacks can change them.

## Scope

- **Animals:** carcasses for seven animals, including goats, and the new duck.
- **Stations:** the hanging hook, butcher block, meat grinder, and sausage stuffer.
- **Fish and seafood:** fish preparation, three new fish, mussel and oyster blocks, and the prawn trap.
- **Parts and recipes:** all parts in the butchery reference, and every recipe they complete.

## Non-goals

- **B-0026:** slaughter by livestock automation. That design decides how automation handles carcasses.
- **B-0051:** food benefits beyond hunger values.

## Related records

- [Cooking program](../../cooking/program.md).
- [Butchery reference](../../cooking/butchery.md).
- [Livestock room](../B-0022/brief.md).
- [Raw ingredient sources](../../cooking/sources.md).

## Open questions and assumptions

- **Fire:** an animal killed by fire drops a carcass as usual, because burning doesn't cook a whole animal through.
- **Tuning:** B-0033 balances part counts against the effort of raising and butchering.
