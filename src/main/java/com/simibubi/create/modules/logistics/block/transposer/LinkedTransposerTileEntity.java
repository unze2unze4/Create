package com.simibubi.create.modules.logistics.block.transposer;

import static net.minecraft.state.properties.BlockStateProperties.POWERED;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.AllTileEntities;
import com.simibubi.create.foundation.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.behaviour.base.TileEntityBehaviour;
import com.simibubi.create.foundation.behaviour.linked.LinkBehaviour;
import com.simibubi.create.modules.logistics.block.extractor.ExtractorSlots;

public class LinkedTransposerTileEntity extends TransposerTileEntity {

	public boolean receivedSignal;
	public LinkBehaviour receiver;

	public LinkedTransposerTileEntity() {
		super(AllTileEntities.LINKED_TRANSPOSER.type);
	}

	@Override
	public void addBehaviours(List<TileEntityBehaviour> behaviours) {
		Pair<ValueBoxTransform, ValueBoxTransform> slots = ValueBoxTransform.Dual.makeSlots(ExtractorSlots.Link::new);
		receiver = LinkBehaviour.receiver(this, slots, this::setSignal);
		behaviours.add(receiver);
		super.addBehaviours(behaviours);
	}

	public void setSignal(boolean powered) {
		receivedSignal = powered;
	}

	@Override
	public void tick() {
		super.tick();
		if (world.isRemote)
			return;
		if (receivedSignal != getBlockState().get(POWERED)) {
			world.setBlockState(pos, getBlockState().cycle(POWERED));
			return;
		}
	}

}
