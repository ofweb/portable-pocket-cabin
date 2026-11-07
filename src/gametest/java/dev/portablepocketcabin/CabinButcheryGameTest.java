package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.List;

/** B-0049 butchery: carcasses, the hanging hook, and the butcher block. */
public final class CabinButcheryGameTest {
	private static final BlockPos BASE = new BlockPos(2, 2, 2);

	@GameTest
	public void adultAnimalsDropOnlyTheirCarcass(GameTestHelper helper) {
		Object[][] animals = {{EntityTypes.COW, "cow"}, {EntityTypes.MOOSHROOM, "cow"}, {EntityTypes.PIG, "pig"}, {EntityTypes.CHICKEN, "chicken"},
			{EntityTypes.RABBIT, "rabbit"}, {EntityTypes.GOAT, "goat"}, {EntityTypes.SHEEP, "sheep"}};
		for (Object[] animal : animals) {
			@SuppressWarnings("unchecked")
			List<ItemStack> drops = kill(helper, (EntityType<? extends LivingEntity>) animal[0], false, false);
			helper.assertTrue(drops.size() == 1 && drops.get(0).is(Butchery.CARCASSES.get((String) animal[1])),
				animal[0] + " drops only its carcass, found " + drops);
		}
		List<ItemStack> burnt = kill(helper, EntityTypes.PIG, false, true);
		helper.assertTrue(burnt.size() == 1 && burnt.get(0).is(Butchery.CARCASSES.get("pig")) && Butchery.state(burnt.get(0)).fresh(),
			"A pig killed while burning still drops a raw carcass");
		helper.assertTrue(kill(helper, EntityTypes.COW, true, false).isEmpty(), "Calves drop nothing");
		helper.succeed();
	}

	@GameTest
	public void sheepCarcassesKeepTheirFleeceColour(GameTestHelper helper) {
		var sheep = helper.spawn(EntityTypes.SHEEP, BASE);
		sheep.setColor(DyeColor.LIME);
		List<ItemStack> drops = die(helper, sheep);
		helper.assertTrue(drops.size() == 1 && Butchery.state(drops.get(0)).fleece().orElse(null) == DyeColor.LIME, "A lime sheep's carcass has lime fleece");
		var sheared = helper.spawn(EntityTypes.SHEEP, BASE);
		sheared.setSheared(true);
		List<ItemStack> bare = die(helper, sheared);
		helper.assertTrue(bare.size() == 1 && Butchery.state(bare.get(0)).fleece().isEmpty(), "A sheared sheep's carcass has no fleece");
		helper.succeed();
	}

	@GameTest
	public void thePotScaldsPigsAndChickens(GameTestHelper helper) {
		var level = helper.getLevel();
		for (String animal : new String[] {"pig", "chicken"}) {
			var input = new CookingInput(List.of(new ItemStack(Butchery.CARCASSES.get(animal))), ItemStack.EMPTY, 1, 0);
			var recipe = CookingStation.POT.find(level, input).orElseThrow();
			ItemStack result = recipe.value().result().create();
			helper.assertTrue(result.is(Butchery.CARCASSES.get(animal)) && Butchery.state(result).scalded() && !Butchery.state(result).fresh(),
				"Scalding gives a scalded " + animal + " carcass, which can no longer be bled");
		}
		helper.assertTrue(CookingStation.POT.find(level, new CookingInput(List.of(new ItemStack(Butchery.CARCASSES.get("cow"))), ItemStack.EMPTY, 1, 0)).isEmpty(),
			"Cows are skinned, not scalded");
		helper.succeed();
	}

	private static List<ItemStack> kill(GameTestHelper helper, EntityType<? extends LivingEntity> type, boolean baby, boolean burning) {
		LivingEntity entity = helper.spawn(type, BASE);
		if (baby && entity instanceof net.minecraft.world.entity.AgeableMob ageable) ageable.setAge(-24000);
		if (burning) entity.igniteForSeconds(5);
		return die(helper, entity);
	}

	private static List<ItemStack> die(GameTestHelper helper, LivingEntity entity) {
		var level = helper.getLevel();
		var area = new AABB(helper.absolutePos(BASE)).inflate(4);
		level.getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
		entity.kill(level);
		List<ItemStack> drops = level.getEntitiesOfClass(ItemEntity.class, area).stream().map(ItemEntity::getItem).toList();
		level.getEntitiesOfClass(ItemEntity.class, area).forEach(ItemEntity::discard);
		return drops;
	}
}
