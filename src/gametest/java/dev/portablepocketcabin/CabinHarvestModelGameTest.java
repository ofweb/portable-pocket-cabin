package dev.portablepocketcabin;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** B-0054: each plant gives its food and its next planting in its own way. */
public final class CabinHarvestModelGameTest {
	private static final BlockPos BASE = new BlockPos(2, 2, 2);

	@GameTest
	public void grainAndWheatPlantFromTheirHarvest(GameTestHelper helper) {
		var level = helper.getLevel();
		helper.assertTrue(CookingPlants.BARLEY instanceof BlockItem item && item.getBlock() == CookingPlants.BARLEY_CROP, "Barley plants barley");
		helper.assertTrue(CookingPlants.RICE instanceof BlockItem item && item.getBlock() == CookingPlants.RICE_CROP, "Rice plants rice");
		helper.assertTrue(!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(PortablePocketCabin.id("barley_seeds")),
			"Barley seeds are gone");
		helper.setBlock(BASE.below(), Blocks.FARMLAND);
		List<ItemStack> ripe = drops(helper, CookingPlants.BARLEY_CROP.defaultBlockState().setValue(CropBlock.AGE, 7));
		int barley = CabinPlantGameTest.count(ripe, CookingPlants.BARLEY);
		helper.assertTrue(barley >= 2 && barley <= 4 && ripe.size() == 1, "Ripe barley gives two to four barley and nothing else, found " + ripe);
		helper.assertTrue(CabinPlantGameTest.count(drops(helper, CookingPlants.BARLEY_CROP.defaultBlockState()), CookingPlants.BARLEY) == 1,
			"Young barley gives one back");

		List<ItemStack> wheat = drops(helper, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7));
		int count = CabinPlantGameTest.count(wheat, Items.WHEAT);
		helper.assertTrue(count >= 2 && count <= 4 && !CabinPlantGameTest.has(wheat, Items.WHEAT_SEEDS),
			"Ripe wheat gives two to four wheat and no seeds, found " + wheat);
		helper.setBlock(BASE, Blocks.AIR);
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 2));
		BlockPos farmland = helper.absolutePos(BASE.below());
		var result = UseBlockCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(farmland), Direction.UP, farmland, false));
		helper.assertTrue(result == InteractionResult.SUCCESS && level.getBlockState(farmland.above()).is(Blocks.WHEAT),
			"Wheat plants wheat on farmland");
		helper.assertTrue(player.getMainHandItem().getCount() == 1, "Planting uses one wheat");
		helper.succeed();
	}

	@GameTest
	public void fruitGivesSeedsAndLeafCropsGoToSeed(GameTestHelper helper) {
		helper.setBlock(BASE.below(), Blocks.FARMLAND);
		Block pepper = CookingPlants.PLANTS.get("bell_pepper");
		List<ItemStack> ripe = drops(helper, pepper.defaultBlockState().setValue(CropBlock.AGE, 7));
		int seeds = CabinPlantGameTest.count(ripe, CookingPlants.item("bell_pepper_seeds"));
		helper.assertTrue(CabinPlantGameTest.has(ripe, CookingPlants.item("bell_pepper")) && seeds >= 1 && seeds <= 3,
			"A ripe pepper gives peppers and one to three seeds, found " + ripe);

		Block lettuce = CookingPlants.LETTUCE_CROP;
		List<ItemStack> leaves = drops(helper, lettuce.defaultBlockState().setValue(CropBlock.AGE, 6));
		helper.assertTrue(CabinPlantGameTest.has(leaves, CookingPlants.LETTUCE)
			&& !CabinPlantGameTest.has(leaves, CookingPlants.item("lettuce_seeds")), "Lettuce at its leaf stage gives lettuce, found " + leaves);
		List<ItemStack> flowered = drops(helper, lettuce.defaultBlockState().setValue(CropBlock.AGE, 7));
		int lettuceSeeds = CabinPlantGameTest.count(flowered, CookingPlants.item("lettuce_seeds"));
		helper.assertTrue(lettuceSeeds >= 2 && lettuceSeeds <= 3 && !CabinPlantGameTest.has(flowered, CookingPlants.LETTUCE),
			"Lettuce left a stage longer flowers into two or three seeds, found " + flowered);
		helper.assertTrue(CabinPlantGameTest.count(drops(helper, lettuce.defaultBlockState()), CookingPlants.item("lettuce_seeds")) == 1,
			"Young lettuce gives its seed back");
		helper.succeed();
	}

	@GameTest
	public void shearsTakeCuttingsFromBushesAndVines(GameTestHelper helper) {
		var level = helper.getLevel();
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.assertTrue(!(CookingPlants.item("thyme") instanceof BlockItem) && !(CookingPlants.item("blackcurrant") instanceof BlockItem),
			"Thyme and blackcurrants no longer plant their bushes");
		helper.assertTrue(CookingPlants.item("blackcurrant_cutting") instanceof BlockItem item && item.getBlock() == CookingPlants.BLACKCURRANT_BUSH,
			"A blackcurrant cutting plants a blackcurrant bush");
		helper.setBlock(BASE.below(), Blocks.GRASS_BLOCK);
		helper.setBlock(BASE, CookingPlants.BLACKCURRANT_BUSH.defaultBlockState().setValue(HerbBushBlock.AGE, HerbBushBlock.MAX_AGE));
		BlockPos bush = helper.absolutePos(BASE);
		var shears = new ItemStack(Items.SHEARS);
		use(level, player, bush, shears);
		helper.assertTrue(dropped(helper, bush, CookingPlants.item("blackcurrant_cutting")) == 1, "Shears take one cutting from a grown bush");
		helper.assertTrue(level.getBlockState(bush).getValue(HerbBushBlock.AGE) == HerbBushBlock.MAX_AGE - Cuttings.SET_BACK,
			"Taking a cutting sets the bush back two stages");
		helper.assertTrue(shears.getDamageValue() == 1, "Taking a cutting wears the shears");
		use(level, player, bush, new ItemStack(Items.SHEARS));
		helper.assertTrue(dropped(helper, bush, CookingPlants.item("blackcurrant_cutting")) == 0, "A young bush gives no cutting");
		helper.assertTrue(CabinPlantGameTest.has(drops(helper, level.getBlockState(bush)), CookingPlants.item("blackcurrant_cutting")),
			"Breaking a bush gives its cutting, to move it");

		BlockPos vine = helper.absolutePos(BASE.east(3));
		helper.setBlock(BASE.east(3).below(), Blocks.FARMLAND);
		helper.setBlock(BASE.east(3), CookingPlants.PLANTS.get("grape_vine").defaultBlockState().setValue(CropBlock.AGE, 7));
		List<ItemStack> broken = drops(helper, BASE.east(3));
		helper.assertTrue(broken.size() == 1 && CabinPlantGameTest.has(broken, CookingPlants.TRELLIS.asItem()), "Breaking a grape vine gives only the trellis");
		use(level, player, vine, new ItemStack(Items.SHEARS));
		helper.assertTrue(dropped(helper, vine, CookingPlants.item("grape_cutting")) == 1
			&& level.getBlockState(vine).getValue(CropBlock.AGE) == 5, "Shears take a grape cutting and set the vine back two stages");
		helper.setBlock(BASE.east(3), CookingPlants.PLANTS.get("tomato_vine").defaultBlockState().setValue(CropBlock.AGE, 7));
		use(level, player, vine, new ItemStack(Items.SHEARS));
		helper.assertTrue(level.getBlockState(vine).getValue(CropBlock.AGE) == 7 && dropped(helper, vine, CookingPlants.item("tomato_seeds")) == 0,
			"Tomatoes grow from seeds, so shears take no cutting");
		helper.succeed();
	}

	@GameTest
	public void herbsFromSeedGiveSeedsWhenBroken(GameTestHelper helper) {
		var level = helper.getLevel();
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		Block mintBush = CookingPlants.PLANTS.get("mint_bush");
		helper.assertTrue(CookingPlants.item("mint_seeds") instanceof BlockItem item && item.getBlock() == mintBush, "Mint seeds plant a mint bush");
		helper.assertTrue(CookingPlants.harvestModel("mint_bush").equals("seed_bush"), "The Garden page says mint grows from seeds");
		helper.setBlock(BASE.below(), Blocks.GRASS_BLOCK);
		helper.setBlock(BASE, mintBush.defaultBlockState().setValue(HerbBushBlock.AGE, HerbBushBlock.MAX_AGE));
		BlockPos bush = helper.absolutePos(BASE);
		use(level, player, bush, new ItemStack(Items.SHEARS));
		helper.assertTrue(level.getBlockState(bush).getValue(HerbBushBlock.AGE) == HerbBushBlock.MAX_AGE, "Shears take no cutting from mint");
		int seeds = CabinPlantGameTest.count(drops(helper, BASE), CookingPlants.item("mint_seeds"));
		helper.assertTrue(seeds >= 2 && seeds <= 3, "Breaking a grown mint bush gives two or three seeds");
		helper.assertTrue(CabinPlantGameTest.count(drops(helper, mintBush.defaultBlockState()), CookingPlants.item("mint_seeds")) == 1,
			"A young mint bush gives its one seed back");
		helper.succeed();
	}

	@GameTest
	public void perennialCrownsAreCutAgainAndDivide(GameTestHelper helper) {
		var level = helper.getLevel();
		Player player = helper.makeMockPlayer(GameType.SURVIVAL);
		helper.setBlock(BASE.below(), Blocks.FARMLAND);
		helper.setBlock(BASE, CookingPlants.ASPARAGUS_CROP.defaultBlockState().setValue(CropBlock.AGE, 7));
		BlockPos pos = helper.absolutePos(BASE);
		List<ItemStack> ripe = drops(helper, BASE);
		helper.assertTrue(CabinPlantGameTest.count(ripe, CookingPlants.item("asparagus_crown")) == 2 && CabinPlantGameTest.has(ripe, CookingPlants.ASPARAGUS),
			"Digging up grown asparagus divides it into two crowns, found " + ripe);
		level.getBlockState(pos).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
		helper.assertTrue(dropped(helper, pos, CookingPlants.ASPARAGUS) >= 1 && level.getBlockState(pos).getValue(CropBlock.AGE) == PerennialCropBlock.CUT_BACK,
			"Cutting asparagus gives spears and leaves the crown to grow again");
		helper.assertTrue(CabinPlantGameTest.count(drops(helper, BASE), CookingPlants.item("asparagus_crown")) == 1, "A cut crown gives back one");

		helper.setBlock(BASE.east(3).below(), Blocks.GRASS_BLOCK);
		helper.setBlock(BASE.east(3), CookingBlocks.CARDOON.defaultBlockState().setValue(CardoonBlock.AGE, CardoonBlock.MAX_AGE));
		BlockPos cardoon = helper.absolutePos(BASE.east(3));
		level.getBlockState(cardoon).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(cardoon), Direction.UP, cardoon, false));
		helper.assertTrue(dropped(helper, cardoon, CookingItems.CARDOON_FLOWER) >= 1 && level.getBlockState(cardoon).getValue(CardoonBlock.AGE) == CardoonBlock.MAX_AGE - 1,
			"Cutting cardoon flowers leaves the plant to flower again");
		helper.succeed();
	}

	private static List<ItemStack> drops(GameTestHelper helper, BlockState state) {
		helper.setBlock(BASE, state);
		return drops(helper, BASE);
	}

	private static List<ItemStack> drops(GameTestHelper helper, BlockPos relative) {
		var level = helper.getLevel();
		BlockPos pos = helper.absolutePos(relative);
		return Block.getDrops(level.getBlockState(pos), level, pos, null);
	}

	private static void use(net.minecraft.server.level.ServerLevel level, Player player, BlockPos pos, ItemStack stack) {
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		level.getBlockState(pos).useItemOn(stack, level, player, InteractionHand.MAIN_HAND,
			new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
	}

	/** Counts and removes the dropped items of a kind around a position. */
	private static int dropped(GameTestHelper helper, BlockPos pos, Item item) {
		int count = 0;
		for (ItemEntity entity : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2))) {
			if (entity.getItem().is(item)) {
				count += entity.getItem().getCount();
				entity.discard();
			}
		}
		return count;
	}
}
