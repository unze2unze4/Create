package com.simibubi.create.modules.contraptions.components.flywheel.engine;

import java.util.List;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.behaviour.base.SmartTileEntity;
import com.simibubi.create.foundation.behaviour.base.TileEntityBehaviour;
import com.simibubi.create.modules.contraptions.components.flywheel.FlywheelBlock;
import com.simibubi.create.modules.contraptions.components.flywheel.FlywheelTileEntity;

import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public class EngineTileEntity extends SmartTileEntity {

	public float appliedCapacity;
	public float appliedSpeed;
	protected FlywheelTileEntity poweredWheel;

	public EngineTileEntity(TileEntityType<?> tileEntityTypeIn) {
		super(tileEntityTypeIn);
	}

	@Override
	public void addBehaviours(List<TileEntityBehaviour> behaviours) {
	}

	@Override
	public boolean hasFastRenderer() {
		return true;
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return super.getRenderBoundingBox().grow(1.5f);
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (world.isRemote)
			return;
		if (poweredWheel != null && poweredWheel.isRemoved())
			poweredWheel = null;
		if (poweredWheel == null)
			attachWheel();
	}

	public void attachWheel() {
		Direction engineFacing = getBlockState().get(EngineBlock.HORIZONTAL_FACING);
		BlockPos wheelPos = pos.offset(engineFacing, 2);
		BlockState wheelState = world.getBlockState(wheelPos);
		if (!AllBlocks.FLYWHEEL.has(wheelState))
			return;
		Direction wheelFacing = wheelState.get(FlywheelBlock.HORIZONTAL_FACING);
		if (wheelFacing.getAxis() != engineFacing.rotateY().getAxis())
			return;
		if (FlywheelBlock.isConnected(wheelState)
				&& FlywheelBlock.getConnection(wheelState) != engineFacing.getOpposite())
			return;
		TileEntity te = world.getTileEntity(wheelPos);
		if (te.isRemoved())
			return;
		if (te instanceof FlywheelTileEntity) {
			if (!FlywheelBlock.isConnected(wheelState))
				FlywheelBlock.setConnection(world, te.getPos(), te.getBlockState(), engineFacing.getOpposite());
			poweredWheel = (FlywheelTileEntity) te;
			refreshWheelSpeed();
		}
	}

	public void detachWheel() {
		if (poweredWheel.isRemoved())
			return;
		poweredWheel.setRotation(0, 0);
		FlywheelBlock.setConnection(world, poweredWheel.getPos(), poweredWheel.getBlockState(), null);
	}

	@Override
	public void remove() {
		if (poweredWheel != null)
			detachWheel();
		super.remove();
	}

	protected void refreshWheelSpeed() {
		if (poweredWheel == null)
			return;
		poweredWheel.setRotation(appliedSpeed, appliedCapacity);
	}

}
