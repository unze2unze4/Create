package com.simibubi.create.modules.contraptions.components.contraptions.glue;

import com.mojang.blaze3d.platform.GlStateManager;
import com.simibubi.create.AllItems;
import com.simibubi.create.Create;
import com.simibubi.create.config.AllConfigs;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.model.PositionTextureVertex;
import net.minecraft.client.renderer.model.TexturedQuad;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SuperGlueRenderer extends EntityRenderer<SuperGlueEntity> {

	private ResourceLocation regular = new ResourceLocation(Create.ID, "textures/entity/super_glue/slime.png");
	private ResourceLocation ghostly = new ResourceLocation(Create.ID, "textures/entity/super_glue/ghostly.png");

	private TexturedQuad quad1;
	private TexturedQuad quad2;

	public SuperGlueRenderer(EntityRendererManager renderManager) {
		super(renderManager);
		initQuads();
	}

	@Override
	protected ResourceLocation getEntityTexture(SuperGlueEntity entity) {
		return isVisible(entity) ? regular : ghostly;
	}

	@Override
	public void doRender(SuperGlueEntity entity, double x, double y, double z, float entityYaw, float partialTicks) {
		Direction facing = entity.getFacingDirection();
		PlayerEntity player = Minecraft.getInstance().player;

		boolean visible = isVisible(entity);
		boolean holdingGlue = AllItems.SUPER_GLUE.typeOf(player.getHeldItemMainhand())
				|| AllItems.SUPER_GLUE.typeOf(player.getHeldItemOffhand());
		holdingGlue = holdingGlue && AllConfigs.CLIENT.showHiddenSuperGlue.get();

		if (!visible && !holdingGlue)
			return;

		GlStateManager.pushMatrix();
		GlStateManager.translated(x, y, z);
		GlStateManager.rotated(AngleHelper.horizontalAngle(facing), 0, 1, 0);
		GlStateManager.rotated(AngleHelper.verticalAngle(facing), 1, 0, 0);

		BufferBuilder buffer = Tessellator.getInstance().getBuffer();
		bindEntityTexture(entity);

		if (!visible) {
			GlStateManager.color4f(1, 1, 1, 0.375f);
			GlStateManager.enableBlend();
			GlStateManager.disableDepthTest();
		}

		quad1.draw(buffer, 1);
		quad2.draw(buffer, 1);

		GlStateManager.disableBlend();
		GlStateManager.enableDepthTest();
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.popMatrix();
	}

	private boolean isVisible(SuperGlueEntity entity) {
		if (!entity.isAlive())
			return false;
		BlockPos pos = entity.hangingPosition;
		BlockPos pos2 = pos.offset(entity.getFacingDirection().getOpposite());
		return !SuperGlueEntity.isValidFace(entity.world, pos2, entity.getFacingDirection()) || !SuperGlueEntity.isValidFace(entity.world, pos, entity.getFacingDirection().getOpposite());
	}

	private void initQuads() {
		Vec3d diff = new Vec3d(Direction.SOUTH.getDirectionVec());
		Vec3d extension = diff.normalize().scale(1 / 32f - 1 / 128f);
		Vec3d plane = VecHelper.planeByNormal(diff);
		Axis axis = Direction.getFacingFromVector(diff.x, diff.y, diff.z).getAxis();

		Vec3d start = Vec3d.ZERO.subtract(extension);
		Vec3d end = Vec3d.ZERO.add(extension);

		plane = plane.scale(1 / 2f);
		Vec3d a1 = plane.add(start);
		Vec3d b1 = plane.add(end);
		plane = VecHelper.rotate(plane, -90, axis);
		Vec3d a2 = plane.add(start);
		Vec3d b2 = plane.add(end);
		plane = VecHelper.rotate(plane, -90, axis);
		Vec3d a3 = plane.add(start);
		Vec3d b3 = plane.add(end);
		plane = VecHelper.rotate(plane, -90, axis);
		Vec3d a4 = plane.add(start);
		Vec3d b4 = plane.add(end);

		PositionTextureVertex v11 = new PositionTextureVertex(a1, 1, 0);
		PositionTextureVertex v12 = new PositionTextureVertex(a2, 1, 1);
		PositionTextureVertex v13 = new PositionTextureVertex(a3, 0, 1);
		PositionTextureVertex v14 = new PositionTextureVertex(a4, 0, 0);

		PositionTextureVertex v21 = new PositionTextureVertex(b1, 1, 0);
		PositionTextureVertex v22 = new PositionTextureVertex(b2, 1, 1);
		PositionTextureVertex v23 = new PositionTextureVertex(b3, 0, 1);
		PositionTextureVertex v24 = new PositionTextureVertex(b4, 0, 0);

		quad1 = new TexturedQuad(new PositionTextureVertex[] { v14, v11, v12, v13 }, 0, 0, 16, 16, 16, 16);
		quad2 = new TexturedQuad(new PositionTextureVertex[] { v21, v24, v23, v22 }, 0, 0, 16, 16, 16, 16);
	}

}
