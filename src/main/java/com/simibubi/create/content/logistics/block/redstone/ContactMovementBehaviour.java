package com.simibubi.create.content.logistics.block.redstone;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementBehaviour;
import com.simibubi.create.content.contraptions.components.structureMovement.MovementContext;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TickPriority;
import net.minecraft.world.World;

public class ContactMovementBehaviour extends MovementBehaviour {

	@Override
	public Vec3d getActiveAreaOffset(MovementContext context) {
		return new Vec3d(context.state.get(RedstoneContactBlock.FACING).getDirectionVec()).scale(.65f);
	}

	@Override
	public void visitNewPosition(MovementContext context, BlockPos pos) {
		BlockState block = context.state;
		World world = context.world;

		if (world.isRemote)
			return;
		if (context.firstMovement)
			return;

		deactivateLastVisitedContact(context);
		BlockState visitedState = world.getBlockState(pos);
		if (!AllBlocks.REDSTONE_CONTACT.has(visitedState))
			return;

		Vec3d contact = new Vec3d(block.get(RedstoneContactBlock.FACING).getDirectionVec());
		contact = VecHelper.rotate(contact, context.rotation.x, context.rotation.y, context.rotation.z);
		Direction direction = Direction.getFacingFromVector(contact.x, contact.y, contact.z);

		if (!RedstoneContactBlock.hasValidContact(world, pos.offset(direction.getOpposite()), direction))
			return;
		world.setBlockState(pos, visitedState.with(RedstoneContactBlock.POWERED, true));
		context.data.put("lastContact", NBTUtil.writeBlockPos(pos));
		return;
	}

	@Override
	public void stopMoving(MovementContext context) {
		deactivateLastVisitedContact(context);
	}

	public void deactivateLastVisitedContact(MovementContext context) {
		if (context.data.contains("lastContact")) {
			BlockPos last = NBTUtil.readBlockPos(context.data.getCompound("lastContact"));
			context.world.getPendingBlockTicks().scheduleTick(last, AllBlocks.REDSTONE_CONTACT.get(), 1, TickPriority.NORMAL);
			context.data.remove("lastContact");
		}
	}

}
