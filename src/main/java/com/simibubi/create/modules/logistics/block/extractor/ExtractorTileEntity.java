package com.simibubi.create.modules.logistics.block.extractor;

import java.util.List;

import com.simibubi.create.AllBlocksNew;
import com.simibubi.create.AllTileEntities;
import com.simibubi.create.config.AllConfigs;
import com.simibubi.create.foundation.behaviour.base.SmartTileEntity;
import com.simibubi.create.foundation.behaviour.base.TileEntityBehaviour;
import com.simibubi.create.foundation.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.behaviour.inventory.AutoExtractingBehaviour;
import com.simibubi.create.foundation.behaviour.inventory.ExtractingBehaviour;
import com.simibubi.create.foundation.behaviour.inventory.SingleTargetAutoExtractingBehaviour;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.modules.contraptions.base.KineticTileEntity;
import com.simibubi.create.modules.contraptions.relays.belt.BeltTileEntity;
import com.simibubi.create.modules.logistics.block.AttachedLogisticalBlock;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;

public class ExtractorTileEntity extends SmartTileEntity {

	protected ExtractingBehaviour extracting;
	protected FilteringBehaviour filtering;
	protected boolean extractingToBelt;

	public ExtractorTileEntity() {
		this(AllTileEntities.EXTRACTOR.type);
	}

	protected ExtractorTileEntity(TileEntityType<?> tileEntityTypeIn) {
		super(tileEntityTypeIn);
	}

	@Override
	public void addBehaviours(List<TileEntityBehaviour> behaviours) {
		int delay = AllConfigs.SERVER.logistics.extractorDelay.get();
		extracting =
			new SingleTargetAutoExtractingBehaviour(this, () -> AttachedLogisticalBlock.getBlockFacing(getBlockState()),
					this::onExtract, delay).pauseWhen(this::isPowered).waitUntil(this::canExtract);
		behaviours.add(extracting);

		filtering = new FilteringBehaviour(this, new ExtractorSlots.Filter()).withCallback(this::filterChanged);
		filtering.showCount();
		behaviours.add(filtering);
	}

	protected void onExtract(ItemStack stack) {
		if (AllBlocksNew.BELT.has(world.getBlockState(pos.down()))) {
			TileEntity te = world.getTileEntity(pos.down());
			if (te instanceof BeltTileEntity) {
				if (((BeltTileEntity) te).tryInsertingFromSide(Direction.UP, stack, false))
					return;
			}
		}

		Vec3d entityPos = VecHelper.getCenterOf(getPos()).add(0, -0.5f, 0);
		Entity entityIn = null;
		Direction facing = AttachedLogisticalBlock.getBlockFacing(getBlockState());
		if (facing == Direction.DOWN)
			entityPos = entityPos.add(0, .5, 0);

		entityIn = new ItemEntity(world, entityPos.x, entityPos.y, entityPos.z, stack);
		entityIn.setMotion(Vec3d.ZERO);
		((ItemEntity) entityIn).setPickupDelay(5);
		world.playSound(null, getPos(), SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, .125f, .1f);
		world.addEntity(entityIn);
	}

	protected boolean isAttachedToBelt() {
		Direction blockFacing = AttachedLogisticalBlock.getBlockFacing(getBlockState());
		return AllBlocksNew.BELT.has(world.getBlockState(pos.offset(blockFacing)));
	}

	protected boolean isTargetingBelt() {
		if (!AllBlocksNew.BELT.has(world.getBlockState(pos.down())))
			return false;
		TileEntity te = world.getTileEntity(pos.down());
		if (te == null || !(te instanceof BeltTileEntity))
			return false;
		return ((KineticTileEntity) te).getSpeed() != 0;
	}

	protected boolean isPowered() {
		return getBlockState().get(ExtractorBlock.POWERED);
	}

	private void filterChanged(ItemStack stack) {

	}

	protected boolean canExtract() {
		if (AllBlocksNew.BELT.has(world.getBlockState(pos.down()))) {
			TileEntity te = world.getTileEntity(pos.down());
			if (te instanceof BeltTileEntity) {
				BeltTileEntity belt = (BeltTileEntity) te;
				if (belt.getSpeed() == 0)
					return false;
				BeltTileEntity controller = belt.getControllerTE();
				if (controller != null) {
					if (!controller.getInventory().canInsertFrom(belt.index, Direction.UP))
						return false;
				}
			}
			return true;
		}

		List<Entity> entitiesWithinAABBExcludingEntity =
			world.getEntitiesWithinAABB(ItemEntity.class, new AxisAlignedBB(getPos()));
		return entitiesWithinAABBExcludingEntity.isEmpty();
	}

	@Override
	public void tick() {
		((AutoExtractingBehaviour) extracting).setTicking(!isAttachedToBelt());
		super.tick();
		boolean onBelt = isTargetingBelt();
		if (extractingToBelt != onBelt) {
			extractingToBelt = onBelt;
			((AutoExtractingBehaviour) extracting)
					.setDelay(onBelt ? 0 : AllConfigs.SERVER.logistics.extractorDelay.get());
		}
	}

}
