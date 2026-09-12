package dev.portablepocketcabin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

record CabinHomeBinding(UUID playerId, UUID cabinId, BlockPos bedPosition) {
	static final Codec<CabinHomeBinding> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		UUIDUtil.STRING_CODEC.fieldOf("player_id").forGetter(CabinHomeBinding::playerId),
		UUIDUtil.STRING_CODEC.fieldOf("cabin_id").forGetter(CabinHomeBinding::cabinId),
		BlockPos.CODEC.fieldOf("bed_position").forGetter(CabinHomeBinding::bedPosition)
	).apply(instance, CabinHomeBinding::new));

	CabinHomeBinding {
		bedPosition = bedPosition.immutable();
	}
}
