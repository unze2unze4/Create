package com.simibubi.create.content.contraptions.components.structureMovement.piston;

import java.util.List;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.CancelPlayerFallPacket;
import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionCollider;
import com.simibubi.create.content.contraptions.components.structureMovement.ContraptionEntity;
import com.simibubi.create.content.contraptions.components.structureMovement.IControlContraption;
import com.simibubi.create.foundation.networking.AllPackets;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.ServerSpeedProvider;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public abstract class LinearActuatorTileEntity extends KineticTileEntity implements IControlContraption {

	public float offset;
	public boolean running;
	public boolean assembleNextTick;
	public ContraptionEntity movedContraption;
	protected boolean forceMove;
	protected ScrollOptionBehaviour<MovementMode> movementMode;
	protected boolean waitingForSpeedChange;

	// Custom position sync
	protected float clientOffsetDiff;

	public LinearActuatorTileEntity(TileEntityType<?> typeIn) {
		super(typeIn);
		setLazyTickRate(3);
		forceMove = true;
	}

	@Override
	public void addBehaviours(List<TileEntityBehaviour> behaviours) {
		super.addBehaviours(behaviours);
		movementMode = new ScrollOptionBehaviour<>(MovementMode.class, Lang.translate("contraptions.movement_mode"),
				this, getMovementModeSlot());
		movementMode.requiresWrench();
		movementMode.withCallback(t -> waitingForSpeedChange = false);
		behaviours.add(movementMode);
	}

	@Override
	public void tick() {
		super.tick();

		if (movedContraption != null) {
			movedContraption.collisionTick();
			if (!movedContraption.isAlive())
				movedContraption = null;
		}

		if (world.isRemote)
			clientOffsetDiff *= .75f;

		if (waitingForSpeedChange && movedContraption != null) {
			if (world.isRemote) {
				float syncSpeed = clientOffsetDiff / 2f;
				offset += syncSpeed;
				movedContraption.setContraptionMotion(toMotionVector(syncSpeed));
				return;
			}
			movedContraption.setContraptionMotion(Vec3d.ZERO);
			return;
		}

		if (!world.isRemote && assembleNextTick) {
			assembleNextTick = false;
			if (running) {
				if (getSpeed() == 0)
					tryDisassemble();
				else
					sendData();
				return;
			}
			assemble();
			return;
		}

		if (!running)
			return;

		boolean contraptionPresent = movedContraption != null;
		float movementSpeed = getMovementSpeed();
		float newOffset = offset + movementSpeed;
		if ((int) newOffset != (int) offset)
			visitNewPosition();

		if (!contraptionPresent || !movedContraption.isStalled())
			offset = newOffset;

		if (contraptionPresent)
			applyContraptionMotion();

		int extensionRange = getExtensionRange();
		if (offset <= 0 || offset >= extensionRange) {
			offset = offset <= 0 ? 0 : extensionRange;
			if (!world.isRemote) {
				applyContraptionMotion();
				applyContraptionPosition();
				tryDisassemble();
				if (waitingForSpeedChange) {
					forceMove = true;
					sendData();
				}
			}
			return;
		}
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (movedContraption != null && !world.isRemote)
			sendData();
	}

	protected int getGridOffset(float offset) {
		return MathHelper.clamp((int) (offset + .5f), 0, getExtensionRange());
	}

	public float getInterpolatedOffset(float partialTicks) {
		float interpolatedOffset =
			MathHelper.clamp(offset + (partialTicks - .5f) * getMovementSpeed(), 0, getExtensionRange());
		return interpolatedOffset;
	}

	@Override
	public void onSpeedChanged(float prevSpeed) {
		super.onSpeedChanged(prevSpeed);
		assembleNextTick = true;
		waitingForSpeedChange = false;
	}

	@Override
	public void remove() {
		this.removed = true;
		if (!world.isRemote)
			disassemble();
		super.remove();
	}

	@Override
	public CompoundNBT write(CompoundNBT tag) {
		tag.putBoolean("Running", running);
		tag.putBoolean("Waiting", waitingForSpeedChange);
		tag.putFloat("Offset", offset);
		return super.write(tag);
	}

	@Override
	public CompoundNBT writeToClient(CompoundNBT compound) {
		if (forceMove) {
			compound.putBoolean("ForceMovement", forceMove);
			forceMove = false;
		}
		return super.writeToClient(compound);
	}

	@Override
	public void read(CompoundNBT tag) {
		running = tag.getBoolean("Running");
		waitingForSpeedChange = tag.getBoolean("Waiting");
		offset = tag.getFloat("Offset");
		super.read(tag);
	}

	@Override
	public void readClientUpdate(CompoundNBT tag) {
		boolean forceMovement = tag.contains("ForceMovement");
		float offsetBefore = offset;
		super.readClientUpdate(tag);

		if (forceMovement) {
			if (movedContraption != null) {
				applyContraptionPosition();
			}
		} else {
			if (running) {
				clientOffsetDiff = offset - offsetBefore;
				offset = offsetBefore;
			}
		}

		if (!running)
			movedContraption = null;

	}

	public abstract void disassemble();

	protected abstract void assemble();

	protected abstract int getExtensionRange();

	protected abstract int getInitialOffset();

	protected abstract ValueBoxTransform getMovementModeSlot();

	protected abstract Vec3d toMotionVector(float speed);

	protected abstract Vec3d toPosition(float offset);

	protected void visitNewPosition() {
		if (!world.isRemote)
			return;
		if (!ContraptionCollider.wasClientPlayerGrounded)
			return;
		// Send falldamage-cancel for the colliding player
		ContraptionCollider.wasClientPlayerGrounded = false;
		AllPackets.channel.sendToServer(new CancelPlayerFallPacket());
	}

	protected void tryDisassemble() {
		if (removed) {
			disassemble();
			return;
		}
		if (movementMode.get() == MovementMode.MOVE_NEVER_PLACE) {
			waitingForSpeedChange = true;
			return;
		}
		int initial = getInitialOffset();
		if ((int) (offset + .5f) != initial && movementMode.get() == MovementMode.MOVE_PLACE_RETURNED) {
			waitingForSpeedChange = true;
			return;
		}
		disassemble();
	}

	@Override
	public void collided() {
		if (world.isRemote) {
			waitingForSpeedChange = true;
			return;
		}
		offset = getGridOffset(offset - getMovementSpeed());
		applyContraptionPosition();
		tryDisassemble();
	}

	protected void applyContraptionMotion() {
		if (movedContraption == null)
			return;
		if (movedContraption.isStalled()) {
			movedContraption.setContraptionMotion(Vec3d.ZERO);
			return;
		}
		movedContraption.setContraptionMotion(getMotionVector());
	}

	protected void applyContraptionPosition() {
		if (movedContraption == null)
			return;
		Vec3d vec = toPosition(offset);
		movedContraption.setPosition(vec.x, vec.y, vec.z);
		if (getSpeed() == 0 || waitingForSpeedChange)
			movedContraption.setContraptionMotion(Vec3d.ZERO);
	}

	public float getMovementSpeed() {
		float movementSpeed = getSpeed() / 512f + clientOffsetDiff / 2f;
		if (world.isRemote)
			movementSpeed *= ServerSpeedProvider.get();
		return movementSpeed;
	}

	public Vec3d getMotionVector() {
		return toMotionVector(getMovementSpeed());
	}

	@Override
	public void onStall() {
		if (!world.isRemote) {
			forceMove = true;
			sendData();
		}
	}

	@Override
	public boolean isValid() {
		return !isRemoved();
	}

	@Override
	public void attach(ContraptionEntity contraption) {
		this.movedContraption = contraption;
		if (!world.isRemote) {
			this.running = true;
			sendData();
		}
	}

	@Override
	public boolean isAttachedTo(ContraptionEntity contraption) {
		return movedContraption == contraption;
	}

}