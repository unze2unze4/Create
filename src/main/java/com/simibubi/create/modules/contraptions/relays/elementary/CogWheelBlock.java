package com.simibubi.create.modules.contraptions.relays.elementary;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.utility.AllShapes;
import com.simibubi.create.modules.contraptions.base.IRotate;
import com.simibubi.create.modules.contraptions.relays.advanced.SpeedControllerBlock;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

public class CogWheelBlock extends ShaftBlock {

	boolean isLarge;

	public static CogWheelBlock small(Properties properties) {
		return new CogWheelBlock(false, properties);
	}

	public static CogWheelBlock large(Properties properties) {
		return new CogWheelBlock(true, properties);
	}

	private CogWheelBlock(boolean large, Properties properties) {
		super(properties);
		isLarge = large;
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		return (isLarge ? AllShapes.LARGE_GEAR : AllShapes.SMALL_GEAR).get(state.get(AXIS));
	}

	@Override
	public boolean isValidPosition(BlockState state, IWorldReader worldIn, BlockPos pos) {
		for (Direction facing : Direction.values()) {
			if (facing.getAxis() == state.get(AXIS))
				continue;

			BlockState blockState = worldIn.getBlockState(pos.offset(facing));
			if (isLargeCog(blockState) || isLarge && isSmallCog(blockState))
				return false;
		}
		return true;
	}

	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		BlockPos placedOnPos = context.getPos()
			.offset(context.getFace()
				.getOpposite());
		World world = context.getWorld();
		BlockState placedAgainst = world.getBlockState(placedOnPos);
		Block block = placedAgainst.getBlock();

		BlockState stateBelow = world.getBlockState(context.getPos()
			.down());
		if (AllBlocks.ROTATION_SPEED_CONTROLLER.has(stateBelow) && isLarge) {
			return this.getDefaultState()
				.with(AXIS, stateBelow.get(SpeedControllerBlock.HORIZONTAL_AXIS) == Axis.X ? Axis.Z : Axis.X);
		}

		if (!(block instanceof IRotate)
			|| !(((IRotate) block).hasIntegratedCogwheel(world, placedOnPos, placedAgainst))) {
			Axis preferredAxis = getPreferredAxis(context);
			if (preferredAxis != null)
				return this.getDefaultState()
					.with(AXIS, preferredAxis);
			return this.getDefaultState()
				.with(AXIS, context.getFace()
					.getAxis());
		}

		return getDefaultState().with(AXIS, ((IRotate) block).getRotationAxis(placedAgainst));
	}

	@Override
	public float getParticleTargetRadius() {
		return isLarge ? 1.125f : .65f;
	}

	@Override
	public float getParticleInitialRadius() {
		return isLarge ? 1f : .75f;
	}

	public static boolean isSmallCog(BlockState state) {
		return AllBlocks.COGWHEEL.has(state);
	}

	public static boolean isLargeCog(BlockState state) {
		return AllBlocks.LARGE_COGWHEEL.has(state);
	}

	public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
		items.add(new ItemStack(this));
	}

	// IRotate

	@Override
	public boolean hasIntegratedCogwheel(IWorldReader world, BlockPos pos, BlockState state) {
		return !isLarge;
	}
}
