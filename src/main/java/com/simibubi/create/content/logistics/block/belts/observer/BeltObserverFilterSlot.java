package com.simibubi.create.content.logistics.block.belts.observer;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.simibubi.create.foundation.tileEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.utility.AngleHelper;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.util.math.Vec3d;

public class BeltObserverFilterSlot extends ValueBoxTransform {

	Vec3d position = VecHelper.voxelSpace(8f, 14.5f, 16f);
	
	@Override
	protected Vec3d getLocation(BlockState state) {
		return rotateHorizontally(state, position);
	}

	@Override
	protected void rotate(BlockState state, MatrixStack ms) {
		ms.multiply(VecHelper.rotateY(AngleHelper.horizontalAngle(state.get(HorizontalBlock.HORIZONTAL_FACING)) + 180));
		ms.multiply(VecHelper.rotateX(90));
	}

}
