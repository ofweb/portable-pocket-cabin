package dev.portablepocketcabin;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

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
		CabinItems.binding(stack).ifPresent(binding -> {
			textConsumer.accept(Component.literal("Cabin: " + binding.cabinId()).withStyle(ChatFormatting.DARK_GRAY));
			textConsumer.accept(Component.literal("Generation: " + binding.generation()).withStyle(ChatFormatting.GRAY));
			if (binding.pending()) {
				textConsumer.accept(Component.literal("Reserved until packing completes").withStyle(ChatFormatting.YELLOW));
			}
		});
	}
}
