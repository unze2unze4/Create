package com.simibubi.create.content.contraptions.components.crusher;

import com.simibubi.create.AllTileEntities;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;

import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class CrushingWheelTileEntity extends KineticTileEntity {

	public static DamageSource damageSource = new DamageSource("create.crush").setDamageBypassesArmor()
			.setDifficultyScaled();

	public CrushingWheelTileEntity() {
		super(AllTileEntities.CRUSHING_WHEEL.type);
		setLazyTickRate(20);
	}

	@Override
	public void onSpeedChanged(float prevSpeed) {
		super.onSpeedChanged(prevSpeed);
		fixControllers();
	}

	public void fixControllers() {
		for (Direction d : Direction.values())
			((CrushingWheelBlock) getBlockState().getBlock()).updateControllers(getBlockState(), getWorld(), getPos(),
					d);
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return new AxisAlignedBB(pos).grow(1);
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		fixControllers();
	}

	@SubscribeEvent
	public static void crushingIsFortunate(LootingLevelEvent event) {
		if (event.getDamageSource() != damageSource)
			return;
		event.setLootingLevel(2);
	}

	@SubscribeEvent
	public static void crushingTeleportsEntities(LivingDeathEvent event) {
		if (event.getSource() != damageSource)
			return;
		event.getEntity().setPos(event.getEntity().getX(), Math.floor(event.getEntity().getY()) - .5f, event.getEntity().getZ());
	}

}
