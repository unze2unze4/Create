package com.simibubi.create.content.contraptions.relays.belt;

import com.simibubi.create.content.contraptions.relays.belt.BeltBlock.Slope;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.IWorld;

public class BeltHelper {

	public static BeltTileEntity getSegmentTE(IWorld world, BlockPos pos) {
		if (!world.isAreaLoaded(pos, 0))
			return null;
		TileEntity tileEntity = world.getTileEntity(pos);
		if (!(tileEntity instanceof BeltTileEntity))
			return null;
		return (BeltTileEntity) tileEntity;
	}

	public static BeltTileEntity getControllerTE(IWorld world, BlockPos pos) {
		BeltTileEntity segment = getSegmentTE(world, pos);
		if (segment == null)
			return null;
		BlockPos controllerPos = segment.controller;
		if (controllerPos == null)
			return null;
		return getSegmentTE(world, controllerPos);
	}

	public static BeltTileEntity getBeltAtSegment(BeltTileEntity controller, int segment) {
		BlockPos pos = getPositionForOffset(controller, segment);
		TileEntity te = controller.getWorld().getTileEntity(pos);
		if (te == null || !(te instanceof BeltTileEntity))
			return null;
		return (BeltTileEntity) te;
	}

	public static BlockPos getPositionForOffset(BeltTileEntity controller, int offset) {
		BlockPos pos = controller.getPos();
		Vec3i vec = controller.getBeltFacing().getDirectionVec();
		Slope slope = controller.getBlockState().get(BeltBlock.SLOPE);
		int verticality = slope == Slope.DOWNWARD ? -1 : slope == Slope.UPWARD ? 1 : 0;

		return pos.add(offset * vec.getX(), MathHelper.clamp(offset, 0, controller.beltLength - 1) * verticality,
				offset * vec.getZ());
	}
	
	public static Vec3d getVectorForOffset(BeltTileEntity controller, float offset) {
		Slope slope = controller.getBlockState().get(BeltBlock.SLOPE);
		int verticality = slope == Slope.DOWNWARD ? -1 : slope == Slope.UPWARD ? 1 : 0;
		float verticalMovement = verticality;
		if (offset < .5)
			verticalMovement = 0;
		verticalMovement = verticalMovement * (Math.min(offset, controller.beltLength - .5f) - .5f);
		
		Vec3d vec = VecHelper.getCenterOf(controller.getPos());
		Vec3d horizontalMovement = new Vec3d(controller.getBeltFacing().getDirectionVec()).scale(offset - .5f);
		
		if (slope == Slope.VERTICAL)
			horizontalMovement = Vec3d.ZERO;
		
		vec = vec.add(horizontalMovement).add(0, verticalMovement, 0);
		return vec;
	}

}
