package com.simibubi.create.foundation.gui;

import javax.annotation.Nullable;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.simibubi.create.AllBlockPartials;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.ColorHelper;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.WrappedWorld;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Atlases;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.Fluid;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.AxisDirection;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraftforge.client.model.data.EmptyModelData;

public class GuiGameElement {

	public static GuiRenderBuilder of(ItemStack stack) {
		return new GuiItemRenderBuilder(stack);
	}

	public static GuiRenderBuilder of(IItemProvider itemProvider) {
		return new GuiItemRenderBuilder(itemProvider);
	}

	public static GuiRenderBuilder of(BlockState state) {
		return new GuiBlockStateRenderBuilder(state);
	}

	public static GuiRenderBuilder of(AllBlockPartials partial) {
		return new GuiBlockPartialRenderBuilder(partial);
	}

	public static GuiRenderBuilder of(Fluid fluid) {
		return new GuiBlockStateRenderBuilder(fluid.getDefaultState()
			.getBlockState()
			.with(FlowingFluidBlock.LEVEL, 0));
	}

	public static abstract class GuiRenderBuilder {
		double xBeforeScale, yBeforeScale;
		double x, y, z;
		double xRot, yRot, zRot;
		double scale = 1;
		int color = 0xFFFFFF;
		Vec3d rotationOffset = Vec3d.ZERO;

		public GuiRenderBuilder atLocal(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
			return this;
		}

		public GuiRenderBuilder at(double x, double y) {
			this.xBeforeScale = x;
			this.yBeforeScale = y;
			return this;
		}

		public GuiRenderBuilder rotate(double xRot, double yRot, double zRot) {
			this.xRot = xRot;
			this.yRot = yRot;
			this.zRot = zRot;
			return this;
		}

		public GuiRenderBuilder rotateBlock(double xRot, double yRot, double zRot) {
			return this.rotate(xRot, yRot, zRot)
				.withRotationOffset(VecHelper.getCenterOf(BlockPos.ZERO));
		}

		public GuiRenderBuilder scale(double scale) {
			this.scale = scale;
			return this;
		}

		public GuiRenderBuilder color(int color) {
			this.color = color;
			return this;
		}

		public GuiRenderBuilder withRotationOffset(Vec3d offset) {
			this.rotationOffset = offset;
			return this;
		}

		public abstract void render();

		protected void prepare() {
			RenderSystem.pushMatrix();
			RenderSystem.enableBlend();
			RenderSystem.enableRescaleNormal();
			RenderSystem.enableAlphaTest();
			RenderHelper.enableGuiDepthLighting();
			RenderSystem.alphaFunc(516, 0.1F);
			RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		}

		protected void transform() {
			int verticalFlip = (this instanceof GuiItemRenderBuilder) ? 1 : -1;

			RenderSystem.translated(xBeforeScale, yBeforeScale, 0);
			RenderSystem.scaled(scale, scale, scale);
			RenderSystem.translated(x, y, z);
			RenderSystem.scaled(1, verticalFlip, 1);
			RenderSystem.translated(rotationOffset.x, rotationOffset.y, rotationOffset.z);
			RenderSystem.rotatef((float) zRot, 0, 0, 1);
			RenderSystem.rotatef((float) xRot, 1, 0, 0);
			RenderSystem.rotatef((float) yRot, 0, 1, 0);
			RenderSystem.translated(-rotationOffset.x, -rotationOffset.y, -rotationOffset.z);
		}

		protected void cleanUp() {
			RenderSystem.popMatrix();
			RenderSystem.disableAlphaTest();
			RenderSystem.disableRescaleNormal();
		}
	}

	private static class GuiBlockModelRenderBuilder extends GuiRenderBuilder {

		protected IBakedModel blockmodel;
		protected BlockState blockState;

		public GuiBlockModelRenderBuilder(IBakedModel blockmodel, @Nullable BlockState blockState) {
			this.blockState = blockState == null ? Blocks.AIR.getDefaultState() : blockState;
			this.blockmodel = blockmodel;
		}

		@Override
		public void render() {
			prepare();

			Minecraft mc = Minecraft.getInstance();
			BlockRendererDispatcher blockRenderer = mc.getBlockRendererDispatcher();
			IRenderTypeBuffer.Impl buffer = mc.getBufferBuilders()
				.getEntityVertexConsumers();
			RenderType renderType = blockState.getBlock() == Blocks.AIR ? Atlases.getEntityTranslucent()
				: RenderTypeLookup.getEntityBlockLayer(blockState);
			IVertexBuilder vb = buffer.getBuffer(renderType);
			MatrixStack ms = new MatrixStack();

			transform();

			mc.getTextureManager()
				.bindTexture(PlayerContainer.BLOCK_ATLAS_TEXTURE);
			renderModel(blockRenderer, buffer, renderType, vb, ms);

			cleanUp();
		}

		protected void renderModel(BlockRendererDispatcher blockRenderer, IRenderTypeBuffer.Impl buffer,
			RenderType renderType, IVertexBuilder vb, MatrixStack ms) {
			Vec3d rgb = ColorHelper.getRGB(color);
			blockRenderer.getBlockModelRenderer()
				.renderModel(ms.peek(), vb, blockState, blockmodel, (float) rgb.x, (float) rgb.y, (float) rgb.z,
					0xF000F0, OverlayTexture.DEFAULT_UV, EmptyModelData.INSTANCE);
			buffer.draw();
		}
	}

	public static class GuiBlockStateRenderBuilder extends GuiBlockModelRenderBuilder {

		public GuiBlockStateRenderBuilder(BlockState blockstate) {
			super(Minecraft.getInstance()
				.getBlockRendererDispatcher()
				.getModelForState(blockstate), blockstate);
		}

		@Override
		protected void renderModel(BlockRendererDispatcher blockRenderer, IRenderTypeBuffer.Impl buffer,
			RenderType renderType, IVertexBuilder vb, MatrixStack ms) {
			if (blockState.getBlock() instanceof FireBlock) {
				RenderHelper.disableGuiDepthLighting();
				blockRenderer.renderBlock(blockState, ms, buffer, 0xF000F0, OverlayTexture.DEFAULT_UV,
					EmptyModelData.INSTANCE);
				RenderHelper.enable();
				buffer.draw();
				return;
			}

			super.renderModel(blockRenderer, buffer, renderType, vb, ms);

			if (blockState.getFluidState()
				.isEmpty())
				return;

			for (RenderType type : RenderType.getBlockLayers()) {
				if (!RenderTypeLookup.canRenderInLayer(blockState.getFluidState(), type))
					continue;

				RenderSystem.pushMatrix();
				RenderHelper.disableStandardItemLighting();

				ClientWorld world = Minecraft.getInstance().world;
				if (renderWorld == null || renderWorld.getWorld() != world)
					renderWorld = new FluidRenderWorld(world);
				
				for (Direction d : Iterate.directions) {
					vb = buffer.getBuffer(type);
					if (d.getAxisDirection() == AxisDirection.POSITIVE)
						continue;
					
					RenderSystem.pushMatrix();
					RenderSystem.translated(.5, .5, .5);
					RenderSystem.rotatef(AngleHelper.horizontalAngle(d), 0, 1, 0);
					RenderSystem.rotatef(AngleHelper.verticalAngle(d) - 90, 0, 0, 1);
					RenderSystem.translated(-.5, -.5, -.5);
					blockRenderer.renderFluid(new BlockPos(0, 1, 0), renderWorld, vb, blockState.getFluidState());
					buffer.draw(type);
					RenderSystem.popMatrix();
				}
				
				RenderHelper.enable();
				RenderSystem.popMatrix();
				break;
			}
		}
	}

	public static class GuiBlockPartialRenderBuilder extends GuiBlockModelRenderBuilder {

		public GuiBlockPartialRenderBuilder(AllBlockPartials partial) {
			super(partial.get(), null);
		}

	}

	public static class GuiItemRenderBuilder extends GuiRenderBuilder {

		private ItemStack stack;

		public GuiItemRenderBuilder(ItemStack stack) {
			this.stack = stack;
		}

		public GuiItemRenderBuilder(IItemProvider provider) {
			this(new ItemStack(provider));
		}

		@Override
		public void render() {
			prepare();
			transform();
			Minecraft.getInstance()
				.getItemRenderer()
				.renderItemIntoGUI(stack, 0, 0);
			cleanUp();
		}

	}

	private static FluidRenderWorld renderWorld;

	private static class FluidRenderWorld extends WrappedWorld {

		public FluidRenderWorld(World world) {
			super(world);
		}

		@Override
		public int getLightLevel(LightType p_226658_1_, BlockPos p_226658_2_) {
			return 15;
		}

		@Override
		public BlockState getBlockState(BlockPos pos) {
			return Blocks.AIR.getDefaultState();
		}

	}
}
