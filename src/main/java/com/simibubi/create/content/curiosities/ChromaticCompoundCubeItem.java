package com.simibubi.create.content.curiosities;

import java.util.List;
import java.util.Random;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.advancement.AllTriggers;
import com.simibubi.create.foundation.config.AllConfigs;
import com.simibubi.create.foundation.config.CCuriosities;
import com.simibubi.create.foundation.item.IItemWithColorHandler;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import com.simibubi.create.foundation.utility.ColorHelper;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tileentity.BeaconTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceContext.BlockMode;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ChromaticCompoundCubeItem extends Item implements IItemWithColorHandler {

	@OnlyIn(value = Dist.CLIENT)
	public static class Color implements IItemColor {
		@Override
		public int getColor(ItemStack stack, int layer) {
			Minecraft mc = Minecraft.getInstance();
			float pt = mc.getRenderPartialTicks();
			float progress =
				(float) ((mc.player.getYaw(pt)) / 180 * Math.PI) + (AnimationTickHolder.getRenderTick() / 10f);
			if (layer == 0)
				return ColorHelper.mixColors(0x6e5773, 0x6B3074, ((float) MathHelper.sin(progress) + 1) / 2);
			if (layer == 1)
				return ColorHelper.mixColors(0xd45d79, 0x6e5773,
						((float) MathHelper.sin((float) (progress + Math.PI)) + 1) / 2);
			if (layer == 2)
				return ColorHelper.mixColors(0xea9085, 0xd45d79,
						((float) MathHelper.sin((float) (progress * 1.5f + Math.PI)) + 1) / 2);
			return 0;
		}
	}

	public ChromaticCompoundCubeItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean shouldSyncTag() {
		return true;
	}

	@Override
	public double getDurabilityForDisplay(ItemStack stack) {
		int light = stack.getOrCreateTag().getInt("CollectingLight");
		return 1 - light / (float) AllConfigs.SERVER.curiosities.lightSourceCountForRefinedRadiance.get();
	}

	@Override
	public boolean showDurabilityBar(ItemStack stack) {
		int light = stack.getOrCreateTag().getInt("CollectingLight");
		return light > 0;
	}

	@Override
	public int getRGBDurabilityForDisplay(ItemStack stack) {
		return ColorHelper.mixColors(0x413c69, 0xFFFFFF, (float) (1 - getDurabilityForDisplay(stack)));
	}

	@Override
	public int getItemStackLimit(ItemStack stack) {
		return showDurabilityBar(stack) ? 1 : 16;
	}

	@Override
	public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
		double y = entity.getY();
		double yMotion = entity.getMotion().y;
		World world = entity.world;
		CompoundNBT data = entity.getPersistentData();
		CompoundNBT itemData = entity.getItem().getOrCreateTag();

		Vec3d positionVec = entity.getPositionVec();
		CCuriosities config = AllConfigs.SERVER.curiosities;
		if (world.isRemote) {
			int light = itemData.getInt("CollectingLight");
			if (random.nextInt(config.lightSourceCountForRefinedRadiance.get() + 20) < light) {
				Vec3d start = VecHelper.offsetRandomly(positionVec, random, 3);
				Vec3d motion = positionVec.subtract(start).normalize().scale(.2f);
				world.addParticle(ParticleTypes.END_ROD, start.x, start.y, start.z, motion.x, motion.y, motion.z);
			}
			return false;
		}

		// Convert to Shadow steel if in void
		if (y < 0 && y - yMotion < -10 && config.enableShadowSteelRecipe.get()) {
			ItemStack newStack = AllItems.SHADOW_STEEL.asStack();
			newStack.setCount(stack.getCount());
			data.putBoolean("FromVoid", true);
			entity.setItem(newStack);
		}

		if (!config.enableRefinedRadianceRecipe.get())
			return false;

		// Convert to Refined Radiance if eaten enough light sources
		if (itemData.getInt("CollectingLight") >= config.lightSourceCountForRefinedRadiance.get()) {
			ItemStack newStack = AllItems.REFINED_RADIANCE.asStack();
			ItemEntity newEntity = new ItemEntity(world, entity.getX(), entity.getY(), entity.getZ(), newStack);
			newEntity.setMotion(entity.getMotion());
			newEntity.getPersistentData().putBoolean("FromLight", true);
			itemData.remove("CollectingLight");
			world.addEntity(newEntity);

			stack.split(1);
			entity.setItem(stack);
			if (stack.isEmpty())
				entity.remove();
			return false;
		}

		// Is inside beacon beam?
		boolean isOverBeacon = false;
		BlockPos.Mutable testPos = new BlockPos.Mutable(entity.getPosition());
		while (testPos.getY() > 0) {
			testPos.move(Direction.DOWN);
			BlockState state = world.getBlockState(testPos);
			if (state.getOpacity(world, testPos) >= 15 && state.getBlock() != Blocks.BEDROCK)
				break;
			if (state.getBlock() == Blocks.BEACON) {
				TileEntity te = world.getTileEntity(testPos);
				if (!(te instanceof BeaconTileEntity))
					break;
				BeaconTileEntity bte = (BeaconTileEntity) te;
				if (bte.getLevels() != 0)
					isOverBeacon = true;
				break;
			}
		}

		if (isOverBeacon) {
			ItemStack newStack = AllItems.REFINED_RADIANCE.asStack();
			newStack.setCount(stack.getCount());
			data.putBoolean("FromLight", true);
			entity.setItem(newStack);

			List<ServerPlayerEntity> players = world.getEntitiesWithinAABB(ServerPlayerEntity.class, new AxisAlignedBB(entity.getPosition()).grow(8));
			players.forEach(AllTriggers.ABSORBED_LIGHT::trigger);

			return false;
		}

		// Find a light source and eat it.
		Random r = world.rand;
		int range = 3;
		float rate = 1 / 2f;
		if (r.nextFloat() > rate)
			return false;

		BlockPos randomOffset = new BlockPos(VecHelper.offsetRandomly(positionVec, r, range));
		BlockState state = world.getBlockState(randomOffset);
		if (state.getLightValue(world, randomOffset) == 0)
			return false;
		if (state.getBlockHardness(world, randomOffset) == -1)
			return false;
		if (state.getBlock() == Blocks.BEACON)
			return false;

		RayTraceContext context = new RayTraceContext(positionVec, VecHelper.getCenterOf(randomOffset),
				BlockMode.COLLIDER, FluidMode.NONE, entity);
		if (!randomOffset.equals(world.rayTraceBlocks(context).getPos()))
			return false;

		world.destroyBlock(randomOffset, false);

		ItemStack newStack = stack.split(1);
		newStack.getOrCreateTag().putInt("CollectingLight", itemData.getInt("CollectingLight") + 1);
		ItemEntity newEntity = new ItemEntity(world, entity.getX(), entity.getY(), entity.getZ(), newStack);
		newEntity.setMotion(entity.getMotion());
		newEntity.setDefaultPickupDelay();
		world.addEntity(newEntity);
		entity.lifespan = 6000;
		if (stack.isEmpty())
			entity.remove();

		return false;
	}

	@Override
	@OnlyIn(value = Dist.CLIENT)
	public IItemColor getColorHandler() {
		return new Color();
	}

}
