package dev.portablepocketcabin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Draws the items lying on a skillet or cutting board, each flat at its own spot, like a campfire. */
final class CookingItemsRenderer<T extends CookingStationBlockEntity>
	implements BlockEntityRenderer<T, CookingItemsRenderer.State> {
	static final class State extends BlockEntityRenderState {
		final List<ItemStackRenderState> items = new ArrayList<>();
	}

	/** Spots in block units: x, z, and the size of the item. */
	private final float[][] spots;
	private final float height;
	private final ItemModelResolver itemModelResolver;

	private CookingItemsRenderer(BlockEntityRendererProvider.Context context, float height, float[][] spots) {
		this.itemModelResolver = context.itemModelResolver();
		this.height = height;
		this.spots = spots;
	}

	static BlockEntityRendererProvider<SkilletBlockEntity, State> skillet() {
		return context -> new CookingItemsRenderer<>(context, 2.1F / 16,
			new float[][] {{0.33F, 0.33F, 0.32F}, {0.67F, 0.33F, 0.32F}, {0.33F, 0.67F, 0.32F}, {0.67F, 0.67F, 0.32F}});
	}

	static BlockEntityRendererProvider<GrillBlockEntity, State> grill() {
		return context -> new CookingItemsRenderer<>(context, 16.1F / 16,
			new float[][] {{0.3F, 0.3F, 0.34F}, {0.7F, 0.3F, 0.34F}, {0.3F, 0.7F, 0.34F}, {0.7F, 0.7F, 0.34F}});
	}

	/** Seven wells: one in the middle and six around it, as on a real æbleskive pan. */
	static BlockEntityRendererProvider<AebleskivePanBlockEntity, State> aebleskivePan() {
		float[][] wells = new float[7][];
		wells[0] = new float[] {0.5F, 0.5F, 0.22F};
		for (int i = 0; i < 6; i++) {
			double angle = Math.PI / 3 * i;
			wells[i + 1] = new float[] {0.5F + 0.3F * (float) Math.cos(angle), 0.5F + 0.3F * (float) Math.sin(angle), 0.22F};
		}
		return context -> new CookingItemsRenderer<>(context, 2.5F / 16, wells);
	}

	static BlockEntityRendererProvider<CuttingBoardBlockEntity, State> cuttingBoard() {
		return context -> new CookingItemsRenderer<>(context, 1.1F / 16, new float[][] {{0.5F, 0.5F, 0.5F}});
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(T station, State state, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breaking) {
		BlockEntityRenderer.super.extractRenderState(station, state, partialTick, camera, breaking);
		state.items.clear();
		int seed = (int) station.getBlockPos().asLong();
		for (int slot = 0; slot < spots.length; slot++) {
			ItemStackRenderState item = new ItemStackRenderState();
			itemModelResolver.updateForTopItem(item, station.getItem(slot), ItemDisplayContext.FIXED, station.getLevel(), null, seed + slot);
			state.items.add(item);
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		for (int slot = 0; slot < state.items.size(); slot++) {
			ItemStackRenderState item = state.items.get(slot);
			if (item.isEmpty()) continue;
			float[] spot = spots[slot];
			pose.pushPose();
			pose.translate(spot[0], height, spot[1]);
			pose.mulPose(Axis.XP.rotationDegrees(90));
			pose.scale(spot[2], spot[2], spot[2]);
			item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
			pose.popPose();
		}
	}
}
