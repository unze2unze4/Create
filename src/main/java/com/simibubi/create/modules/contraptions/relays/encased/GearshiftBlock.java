package com.simibubi.create.modules.contraptions.relays.encased;

import java.util.Random;

import com.simibubi.create.foundation.block.ITE;
import com.simibubi.create.modules.contraptions.RotationPropagator;
import com.simibubi.create.modules.contraptions.base.KineticTileEntity;
import com.simibubi.create.modules.contraptions.relays.gearbox.GearshiftTileEntity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.TickPriority;
import net.minecraft.world.World;

public class GearshiftBlock extends EncasedShaftBlock implements ITE<GearshiftTileEntity> {

	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

	public GearshiftBlock() {
		super();
		setDefaultState(getDefaultState().with(POWERED, false));
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new GearshiftTileEntity();
	}

	@Override
	protected void fillStateContainer(Builder<Block, BlockState> builder) {
		builder.add(POWERED);
		super.fillStateContainer(builder);
	}

	@Override
	public BlockState getStateForPlacement(BlockItemUseContext context) {
		return super.getStateForPlacement(context).with(POWERED,
				Boolean.valueOf(context.getWorld().isBlockPowered(context.getPos())));
	}

	@Override
	public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
			boolean isMoving) {
		if (worldIn.isRemote)
			return;

		boolean previouslyPowered = state.get(POWERED);
		if (previouslyPowered != worldIn.isBlockPowered(pos)) {
			detachKinetics(worldIn, pos, true);
			worldIn.setBlockState(pos, state.cycle(POWERED), 2);
		}
	}

	public boolean hasShaftTowards(IWorldReader world, BlockPos pos, BlockState state, Direction face) {
		return super.hasShaftTowards(world, pos, state, face);
	}

	@Override
	public Class<GearshiftTileEntity> getTileEntityClass() {
		return GearshiftTileEntity.class;
	}

	public void detachKinetics(World worldIn, BlockPos pos, boolean reAttachNextTick) {
		TileEntity te = worldIn.getTileEntity(pos);
		if (te == null || !(te instanceof KineticTileEntity))
			return;
		RotationPropagator.handleRemoved(worldIn, pos, (KineticTileEntity) te);

		// Re-attach next tick
		if (reAttachNextTick)
			worldIn.getPendingBlockTicks().scheduleTick(pos, this, 0, TickPriority.EXTREMELY_HIGH);
	}

	@Override
	public void tick(BlockState state, World worldIn, BlockPos pos, Random random) {
		TileEntity te = worldIn.getTileEntity(pos);
		if (te == null || !(te instanceof KineticTileEntity))
			return;
		KineticTileEntity kte = (KineticTileEntity) te;
		RotationPropagator.handleAdded(worldIn, pos, kte);
	}

}
