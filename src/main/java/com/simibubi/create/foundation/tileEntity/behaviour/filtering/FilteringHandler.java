package com.simibubi.create.foundation.tileEntity.behaviour.filtering;

import com.simibubi.create.AllKeys;
import com.simibubi.create.content.logistics.item.filter.FilterItem;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.utility.RaycastHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber
public class FilteringHandler {

	@SubscribeEvent
	public static void onBlockActivated(PlayerInteractEvent.RightClickBlock event) {
		World world = event.getWorld();
		BlockPos pos = event.getPos();
		PlayerEntity player = event.getPlayer();
		Hand hand = event.getHand();

		if (player.isSneaking())
			return;

		FilteringBehaviour behaviour = TileEntityBehaviour.get(world, pos, FilteringBehaviour.TYPE);
		if (behaviour == null)
			return;
		if (!behaviour.isActive())
			return;

		BlockRayTraceResult ray = RaycastHelper.rayTraceRange(world, player, 10);
		if (ray == null)
			return;

		if (behaviour.testHit(ray.getHitVec())) {
			if (event.getSide() != LogicalSide.CLIENT) {
				ItemStack heldItem = player.getHeldItem(hand).copy();
				if (!player.isCreative()) {
					if (behaviour.getFilter().getItem() instanceof FilterItem)
						player.inventory.placeItemBackInInventory(world, behaviour.getFilter());
					if (heldItem.getItem() instanceof FilterItem)
						player.getHeldItem(hand).shrink(1);
				}
				if (heldItem.getItem() instanceof FilterItem)
					heldItem.setCount(1);
				behaviour.setFilter(heldItem);
			}
			event.setCanceled(true);
			event.setCancellationResult(ActionResultType.SUCCESS);
			world.playSound(null, pos, SoundEvents.ENTITY_ITEM_FRAME_ADD_ITEM, SoundCategory.BLOCKS, .25f, .1f);
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static boolean onScroll(double delta) {
		RayTraceResult objectMouseOver = Minecraft.getInstance().objectMouseOver;
		if (!(objectMouseOver instanceof BlockRayTraceResult))
			return false;

		BlockRayTraceResult result = (BlockRayTraceResult) objectMouseOver;
		Minecraft mc = Minecraft.getInstance();
		ClientWorld world = mc.world;
		BlockPos blockPos = result.getPos();

		FilteringBehaviour filtering = TileEntityBehaviour.get(world, blockPos, FilteringBehaviour.TYPE);
		if (filtering == null)
			return false;
		if (mc.player.isSneaking())
			return false;
		if (!mc.player.isAllowEdit())
			return false;
		if (!filtering.isCountVisible())
			return false;
		if (!filtering.testHit(objectMouseOver.getHitVec()))
			return false;
		ItemStack filterItem = filtering.getFilter();
		if (filterItem.isEmpty())
			return false;

		filtering.ticksUntilScrollPacket = 10;
		int maxAmount = (filterItem.getItem() instanceof FilterItem) ? 64 : filterItem.getMaxStackSize();
		filtering.scrollableValue =
			(int) MathHelper.clamp(filtering.scrollableValue + delta * (AllKeys.ctrlDown() ? 16 : 1), 0, maxAmount);

		return true;
	}

}
