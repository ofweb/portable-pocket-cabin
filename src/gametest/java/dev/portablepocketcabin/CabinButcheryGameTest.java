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

	@GameTest
	public void theHookBleedsSkinsAndPlucks(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos hookPos = BASE.above();
		helper.setBlock(hookPos.above(), net.minecraft.world.level.block.Blocks.STONE);
		helper.setBlock(hookPos, Butchery.HANGING_HOOK);
		BlockPos pos = helper.absolutePos(hookPos);
		var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), net.minecraft.core.Direction.NORTH, pos, false);
		var hook = (HangingHookBlockEntity) level.getBlockEntity(pos);
		// Tools are held in the last hotbar slot, so what the hook gives lands in other slots.
		player.getInventory().setSelectedSlot(8);

		use(level, player, pos, hit, new ItemStack(Butchery.CARCASSES.get("cow")));
		helper.assertTrue(hook.hung().is(Butchery.CARCASSES.get("cow")), "A cow carcass hangs on the hook");
		use(level, player, pos, hit, new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));
		helper.assertTrue(player.getInventory().countItem(Butchery.BLOOD) == 1 && Butchery.state(hook.hung()).bled(), "A glass bottle collects blood");
		use(level, player, pos, hit, new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));
		helper.assertTrue(player.getInventory().countItem(Butchery.BLOOD) == 1, "A carcass gives blood only once");
		level.getBlockState(pos).useWithoutItem(level, player, hit);
		helper.assertTrue(hook.hung().is(Butchery.CARCASSES.get("cow")), "A cow isn't skinned by hand");
		use(level, player, pos, hit, new ItemStack(CookingItems.KITCHEN_KNIFE));
		helper.assertTrue(hook.hung().is(Butchery.DRESSED.get("cow")) && player.getInventory().countItem(net.minecraft.world.item.Items.LEATHER) == 2,
			"The knife skins a cow into two hides and a dressed cow");
		level.getBlockState(pos).useWithoutItem(level, player, hit);
		helper.assertTrue(hook.hung().isEmpty() && player.getInventory().countItem(Butchery.DRESSED.get("cow")) == 1, "The dressed cow is taken down");

		use(level, player, pos, hit, new ItemStack(Butchery.CARCASSES.get("chicken")));
		level.getBlockState(pos).useWithoutItem(level, player, hit);
		helper.assertTrue(hook.hung().is(Butchery.CARCASSES.get("chicken")) && player.getInventory().countItem(net.minecraft.world.item.Items.FEATHER) == 0,
			"An unscalded chicken can't be plucked");
		player.setShiftKeyDown(true);
		level.getBlockState(pos).useWithoutItem(level, player, hit);
		player.setShiftKeyDown(false);
		helper.assertTrue(hook.hung().isEmpty(), "Sneaking takes a carcass down to scald it");
		ItemStack scalded = new ItemStack(Butchery.CARCASSES.get("chicken"));
		scalded.set(Butchery.CARCASS, new Butchery.CarcassState(false, true, java.util.Optional.empty()));
		use(level, player, pos, hit, scalded);
		use(level, player, pos, hit, new ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));
		helper.assertTrue(player.getInventory().countItem(Butchery.BLOOD) == 1, "A scalded carcass gives no blood");
		level.getBlockState(pos).useWithoutItem(level, player, hit);
		helper.assertTrue(hook.hung().is(Butchery.DRESSED.get("chicken")) && player.getInventory().countItem(net.minecraft.world.item.Items.FEATHER) == 2,
			"A scalded chicken is plucked by hand");
		level.getBlockState(pos).useWithoutItem(level, player, hit);

		ItemStack sheep = new ItemStack(Butchery.CARCASSES.get("sheep"));
		sheep.set(Butchery.CARCASS, new Butchery.CarcassState(false, false, java.util.Optional.of(DyeColor.LIME)));
		use(level, player, pos, hit, sheep);
		use(level, player, pos, hit, new ItemStack(CookingItems.KITCHEN_KNIFE));
		helper.assertTrue(player.getInventory().countItem(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("lime_wool"))) == 1, "A lime sheep skins to lime wool");
		level.getBlockState(pos).useWithoutItem(level, player, hit);

		use(level, player, pos, hit, new ItemStack(Butchery.CARCASSES.get("pig")));
		use(level, player, pos, hit, new ItemStack(CookingItems.KITCHEN_KNIFE));
		helper.assertTrue(hook.hung().is(Butchery.CARCASSES.get("pig")), "An unscalded pig can't be scraped");
		helper.setBlock(hookPos.above(), net.minecraft.world.level.block.Blocks.AIR);
		helper.assertTrue(!level.getBlockState(pos).is(Butchery.HANGING_HOOK), "A hook without support falls");
		helper.succeed();
	}

	private static void use(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.player.Player player, BlockPos pos,
		net.minecraft.world.phys.BlockHitResult hit, ItemStack stack) {
		player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
		var result = level.getBlockState(pos).useItemOn(stack, level, player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
		if (result == net.minecraft.world.InteractionResult.TRY_WITH_EMPTY_HAND) level.getBlockState(pos).useWithoutItem(level, player, hit);
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
