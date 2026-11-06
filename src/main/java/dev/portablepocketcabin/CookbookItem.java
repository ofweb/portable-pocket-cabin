package dev.portablepocketcabin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Opens the cookbook screen. Browsing is client-side; the server is asked only which recipes the player can make. */
final class CookbookItem extends Item {
	/** Set by the client entrypoint, so this common class never loads client classes. */
	static Runnable opener = () -> { };

	CookbookItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) opener.run();
		return InteractionResult.SUCCESS;
	}
}
