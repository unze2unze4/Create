package com.simibubi.create.modules.contraptions.components.contraptions.pulley;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTileEntities;
import com.simibubi.create.config.AllConfigs;
import com.simibubi.create.foundation.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.behaviour.ValueBoxTransform;
import com.simibubi.create.modules.contraptions.components.contraptions.BlockMovementTraits;
import com.simibubi.create.modules.contraptions.components.contraptions.ContraptionCollider;
import com.simibubi.create.modules.contraptions.components.contraptions.ContraptionEntity;
import com.simibubi.create.modules.contraptions.components.contraptions.piston.LinearActuatorTileEntity;

import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class PulleyTileEntity extends LinearActuatorTileEntity {

	protected int initialOffset;

	public PulleyTileEntity() {
		super(AllTileEntities.ROPE_PULLEY.type);
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return super.getRenderBoundingBox().expand(0, -offset, 0);
	}

	@Override
	public double getMaxRenderDistanceSquared() {
		return super.getMaxRenderDistanceSquared() + offset * offset;
	}

	@Override
	protected void assemble() {
		if (!(world.getBlockState(pos)
			.getBlock() instanceof PulleyBlock))
			return;
		if (speed == 0)
			return;
		if (offset >= getExtensionRange() && getSpeed() > 0)
			return;
		if (offset <= 0 && getSpeed() < 0)
			return;

		// Collect Construct
		if (!world.isRemote) {
			BlockPos anchor = pos.down((int) (offset + 1));
			initialOffset = (int) (offset);
			PulleyContraption contraption = PulleyContraption.assemblePulleyAt(world, anchor, (int) offset);

			if (contraption != null) {
				Direction movementDirection = getSpeed() > 0 ? Direction.DOWN : Direction.UP;
				if (ContraptionCollider.isCollidingWithWorld(world, contraption, anchor.offset(movementDirection),
					movementDirection))
					contraption = null;
			}

			if (contraption == null && getSpeed() > 0)
				return;

			for (int i = ((int) offset); i > 0; i--) {
				BlockPos offset = pos.down(i);
				world.setBlockState(offset, Blocks.AIR.getDefaultState(), 66);
			}

			if (contraption != null && !contraption.blocks.isEmpty()) {
				contraption.removeBlocksFromWorld(world, BlockPos.ZERO);
				movedContraption = ContraptionEntity.createStationary(world, contraption)
					.controlledBy(this);
				movedContraption.setPosition(anchor.getX(), anchor.getY(), anchor.getZ());
				world.addEntity(movedContraption);
				forceMove = true;
			}
		}

		clientOffsetDiff = 0;
		running = true;
		sendData();
	}

	@Override
	public void disassemble() {
		if (!running && movedContraption == null)
			return;
		offset = getGridOffset(offset);
		if (movedContraption != null)
			applyContraptionPosition();

		if (!world.isRemote) {
			if (offset > 0) {
				BlockPos magnetPos = pos.down((int) offset);
				world.destroyBlock(magnetPos, world.getBlockState(magnetPos)
					.getCollisionShape(world, magnetPos)
					.isEmpty());
				world.setBlockState(magnetPos, AllBlocks.PULLEY_MAGNET.getDefaultState(), 66);
			}

			for (int i = 1; i <= ((int) offset) - 1; i++) {
				BlockPos ropePos = pos.down(i);
				world.destroyBlock(ropePos, world.getBlockState(ropePos)
					.getCollisionShape(world, ropePos)
					.isEmpty());
			}
			for (int i = 1; i <= ((int) offset) - 1; i++)
				world.setBlockState(pos.down(i), AllBlocks.ROPE.getDefaultState(), 66);

			if (movedContraption != null)
				movedContraption.disassemble();
		}

		if (movedContraption != null)
			movedContraption.remove();
		movedContraption = null;
		initialOffset = 0;
		running = false;
		sendData();
	}

	@Override
	protected Vec3d toPosition(float offset) {
		if (movedContraption.getContraption() instanceof PulleyContraption) {
			PulleyContraption contraption = (PulleyContraption) movedContraption.getContraption();
			return new Vec3d(contraption.getAnchor()).add(0, contraption.initialOffset - offset, 0);

		}
		return Vec3d.ZERO;
	}

	@Override
	protected void visitNewPosition() {
		super.visitNewPosition();
		if (world.isRemote)
			return;
		if (movedContraption != null)
			return;
		if (getSpeed() <= 0)
			return;

		BlockPos posBelow = pos.down((int) (offset + getMovementSpeed()) + 1);
		if (!BlockMovementTraits.movementNecessary(world, posBelow))
			return;
		if (BlockMovementTraits.isBrittle(world.getBlockState(posBelow)))
			return;

		disassemble();
		assembleNextTick = true;
	}

	@Override
	public void read(CompoundNBT tag) {
		initialOffset = tag.getInt("InitialOffset");
		super.read(tag);
	}

	@Override
	public CompoundNBT write(CompoundNBT tag) {
		tag.putInt("InitialOffset", initialOffset);
		return super.write(tag);
	}

	@Override
	protected int getExtensionRange() {
		return Math.max(0, Math.min(AllConfigs.SERVER.kinetics.maxRopeLength.get(), pos.getY() - 1));
	}

	@Override
	protected int getInitialOffset() {
		return initialOffset;
	}

	@Override
	protected Vec3d toMotionVector(float speed) {
		return new Vec3d(0, -speed, 0);
	}

	@Override
	protected ValueBoxTransform getMovementModeSlot() {
		return new CenteredSideValueBoxTransform((state, d) -> d == Direction.UP);
	}

}
