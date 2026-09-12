package dev.portablepocketcabin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

final class PackedCabinItem extends Item {
	PackedCabinItem(Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(
		ItemStack stack,
		TooltipContext context,
		TooltipDisplay display,
		Consumer<Component> textConsumer,
		TooltipFlag flag
	) {
		var binding = CabinItems.binding(stack);
		if (binding.isEmpty()) {
			textConsumer.accept(Component.literal("Unbound cabin kit").withStyle(ChatFormatting.GRAY));
			textConsumer.accept(Component.literal("Use to preview, then use again to deploy")
				.withStyle(ChatFormatting.DARK_GRAY));
			return;
		}
		binding.ifPresent(value -> {
			textConsumer.accept(Component.literal("Cabin: " + value.cabinId()).withStyle(ChatFormatting.DARK_GRAY));
			textConsumer.accept(Component.literal("Generation: " + value.generation()).withStyle(ChatFormatting.GRAY));
			if (value.pending()) {
				textConsumer.accept(Component.literal("Reserved until packing completes").withStyle(ChatFormatting.YELLOW));
			}
		});
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
			CabinPlacement.previewOrDeploy(serverPlayer);
			return InteractionResult.SUCCESS_SERVER;
		}
		return InteractionResult.PASS;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (context.getLevel().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
			CabinPlacement.previewOrDeploy(serverPlayer);
			return InteractionResult.SUCCESS_SERVER;
		}
		return InteractionResult.PASS;
	}
}
