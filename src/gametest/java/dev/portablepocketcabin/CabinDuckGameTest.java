package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.chicken.ChickenVariants;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/** B-0049 delivery 3: the duck, its carcass, and the duck chains. */
public final class CabinDuckGameTest {
	private static final BlockPos BASE = new BlockPos(2, 2, 2);
	static final String[] RECIPES = {
		"pot/unstrained_duck_fat",
		"strainer/rendered_duck_fat",
		"brine/salted_duck_legs",
		"pot/duck_confit",
		"oven/roast_duck",
		"pot/cassoulet_beans",
		"oven/cassoulet",
		"pot/scalded_duck_carcass"
	};

	@GameTest
	public void everyDeliveryRecipeIsFoundFromItsInputs(GameTestHelper helper) {
		CabinGardenRecipeGameTest.assertFoundFromInputs(helper, RECIPES);
		helper.succeed();
	}

	@GameTest
	public void ducksBreedLayAndDropTheirCarcass(GameTestHelper helper) {
		var level = helper.getLevel();
		Duck duck = Butchery.DUCK.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
		duck.snapTo(helper.absoluteVec(net.minecraft.world.phys.Vec3.atBottomCenterOf(BASE)));
		duck.finalizeSpawn(level, level.getCurrentDifficultyAt(duck.blockPosition()), EntitySpawnReason.SPAWN_ITEM_USE, null);
		level.addFreshEntity(duck);
		helper.assertTrue(duck.getVariant().is(ChickenVariants.COLD), "A duck wears the cold chicken's coat");
		helper.assertTrue(duck.isFood(new ItemStack(net.minecraft.world.item.Items.WHEAT_SEEDS)), "Ducks breed with seeds");
		helper.assertTrue(duck.getBreedOffspring(level, duck) instanceof Duck, "Two ducks raise a duckling");
		var area = new AABB(duck.blockPosition()).inflate(3);
		duck.kill(level);
		var drops = level.getEntitiesOfClass(ItemEntity.class, area).stream().map(ItemEntity::getItem).toList();
		helper.assertTrue(drops.size() == 1 && drops.get(0).is(Butchery.CARCASSES.get("duck")), "A duck drops only its carcass, found " + drops);
		helper.assertTrue(new ItemStack(CookingPlants.item("rendered_duck_fat")).is(CookingItems.COOKING_OILS), "Rendered duck fat fries like oil");
		helper.assertTrue(CarcassData.all().get(PortablePocketCabin.id("duck")).scald(), "Ducks are scalded before plucking");
		helper.succeed();
	}
}
