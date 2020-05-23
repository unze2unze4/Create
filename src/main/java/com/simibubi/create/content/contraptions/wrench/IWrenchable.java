package com.simibubi.create.content.contraptions.wrench;

import com.simibubi.create.content.contraptions.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.contraptions.base.DirectionalKineticBlock;
import com.simibubi.create.content.contraptions.base.GeneratingKineticTileEntity;
import com.simibubi.create.content.contraptions.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.contraptions.base.HorizontalKineticBlock;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.utility.DirectionHelper;
import com.simibubi.create.foundation.utility.VoxelShaper;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemUseContext;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.world.World;

public interface IWrenchable {

	default ActionResultType onWrenched(BlockState state, ItemUseContext context) {
		World world = context.getWorld();
		BlockState rotated = getRotatedBlockState(state, context.getFace());
		if (!rotated.isValidPosition(world, context.getPos()))
			return ActionResultType.PASS;

		KineticTileEntity.switchToBlockState(world, context.getPos(), updateAfterWrenched(rotated, context));

		TileEntity te = context.getWorld().getTileEntity(context.getPos());
		if (te != null)
			te.updateContainingBlockInfo();
		if (te instanceof GeneratingKineticTileEntity) {
			((GeneratingKineticTileEntity) te).updateGeneratedRotation();
		}

		return ActionResultType.SUCCESS;
	}

	default BlockState updateAfterWrenched(BlockState newState, ItemUseContext context) {
		return newState;
	}

	static BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace){
		BlockState newState = originalState;

		if (targetedFace.getAxis() == Direction.Axis.Y) {
			if (originalState.has(HorizontalAxisKineticBlock.HORIZONTAL_AXIS))
				return originalState.with(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, DirectionHelper.rotateAround(VoxelShaper.axisAsFace(originalState.get(HorizontalAxisKineticBlock.HORIZONTAL_AXIS)), targetedFace.getAxis()).getAxis());
			if (originalState.has(HorizontalKineticBlock.HORIZONTAL_FACING))
				return originalState.with(HorizontalKineticBlock.HORIZONTAL_FACING, DirectionHelper.rotateAround(originalState.get(HorizontalKineticBlock.HORIZONTAL_FACING), targetedFace.getAxis()));
		}

		if (originalState.has(RotatedPillarKineticBlock.AXIS))
			return originalState.with(RotatedPillarKineticBlock.AXIS, DirectionHelper.rotateAround(VoxelShaper.axisAsFace(originalState.get(RotatedPillarKineticBlock.AXIS)), targetedFace.getAxis()).getAxis());

		if (!originalState.has(DirectionalKineticBlock.FACING)) return originalState;

		Direction stateFacing = originalState.get(DirectionalKineticBlock.FACING);

		if (stateFacing.getAxis().equals(targetedFace.getAxis())) {
			if (originalState.has(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE)) return originalState.cycle(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE);
			else return originalState;
		} else {
			do {
				newState = newState.with(DirectionalKineticBlock.FACING, DirectionHelper.rotateAround(newState.get(DirectionalKineticBlock.FACING), targetedFace.getAxis()));
				if (targetedFace.getAxis() == Direction.Axis.Y && newState.has(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE)) newState = newState.cycle(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE);
			} while (newState.get(DirectionalKineticBlock.FACING).getAxis().equals(targetedFace.getAxis()));
		}
		return newState;
	}

}
