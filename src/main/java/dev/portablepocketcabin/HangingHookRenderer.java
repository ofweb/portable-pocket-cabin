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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;

/** Draws the carcass hanging upright below a hanging hook, turned to face the way the hook faces. */
final class HangingHookRenderer implements BlockEntityRenderer<HangingHookBlockEntity, HangingHookRenderer.State> {
	static final class State extends BlockEntityRenderState {
		final ItemStackRenderState item = new ItemStackRenderState();
		float rotation;
	}

	private final ItemModelResolver itemModelResolver;

	HangingHookRenderer(BlockEntityRendererProvider.Context context) {
		this.itemModelResolver = context.itemModelResolver();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(HangingHookBlockEntity hook, State state, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breaking) {
		BlockEntityRenderer.super.extractRenderState(hook, state, partialTick, camera, breaking);
		itemModelResolver.updateForTopItem(state.item, hook.hung(), ItemDisplayContext.FIXED, hook.getLevel(), null, (int) hook.getBlockPos().asLong());
		state.rotation = -hook.getBlockState().getValue(HorizontalDirectionalBlock.FACING).toYRot();
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.item.isEmpty()) return;
		pose.pushPose();
		pose.translate(0.5F, 0.15F, 0.5F);
		pose.mulPose(Axis.YP.rotationDegrees(state.rotation));
		pose.scale(0.8F, 0.8F, 0.8F);
		state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		pose.popPose();
	}
}
