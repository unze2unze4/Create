package com.simibubi.create.modules.contraptions.relays.belt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.block.IHaveColorHandler;
import com.simibubi.create.foundation.block.ITE;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.modules.contraptions.base.HorizontalKineticBlock;
import com.simibubi.create.modules.contraptions.relays.belt.transport.BeltMovementHandler.TransportedEntityInfo;
import com.simibubi.create.modules.logistics.block.belts.tunnel.BeltTunnelBlock;
import com.simibubi.create.modules.schematics.ISpecialBlockItemRequirement;
import com.simibubi.create.modules.schematics.ItemRequirement;
import com.simibubi.create.modules.schematics.ItemRequirement.ItemUseType;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.client.particle.DiggingParticle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.IProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.Direction.AxisDirection;
import net.minecraft.util.Hand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.storage.loot.LootParameters;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.Tags;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class BeltBlock extends HorizontalKineticBlock
	implements ITE<BeltTileEntity>, IHaveColorHandler, ISpecialBlockItemRequirement {

	public static final IProperty<Slope> SLOPE = EnumProperty.create("slope", Slope.class);
	public static final IProperty<Part> PART = EnumProperty.create("part", Part.class);
	public static final BooleanProperty CASING = BooleanProperty.create("casing");

	public BeltBlock(Properties properties) {
		super(properties);
		setDefaultState(getDefaultState().with(SLOPE, Slope.HORIZONTAL)
			.with(PART, Part.START)
			.with(CASING, false));
	}

	@Override
	public void fillItemGroup(ItemGroup p_149666_1_, NonNullList<ItemStack> p_149666_2_) {
		p_149666_2_.add(AllItems.BELT_CONNECTOR.asStack());
	}

	@Override
	public boolean hasShaftTowards(IWorldReader world, BlockPos pos, BlockState state, Direction face) {
		if (face.getAxis() != getRotationAxis(state))
			return false;
		try {
			return getTileEntity(world, pos).hasPulley();
		} catch (TileEntityException e) {
		}
		return false;
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.get(HORIZONTAL_FACING)
			.rotateY()
			.getAxis();
	}

	@Override
	public ItemStack getPickBlock(BlockState state, RayTraceResult target, IBlockReader world, BlockPos pos,
		PlayerEntity player) {
		return AllItems.BELT_CONNECTOR.asStack();
	}

	@Override
	public Material getMaterial(BlockState state) {
		return state.get(CASING) ? Material.WOOD : Material.WOOL;
	}

	@SuppressWarnings("deprecation")
	@Override
	public List<ItemStack> getDrops(BlockState state, net.minecraft.world.storage.loot.LootContext.Builder builder) {
		List<ItemStack> drops = super.getDrops(state, builder);
		if (state.get(CASING))
			drops.addAll(AllBlocks.BRASS_CASING.getDefaultState()
				.getDrops(builder));
		TileEntity tileEntity = builder.get(LootParameters.BLOCK_ENTITY);
		if (tileEntity instanceof BeltTileEntity && ((BeltTileEntity) tileEntity).hasPulley())
			drops.addAll(AllBlocks.SHAFT.getDefaultState()
				.getDrops(builder));
		return drops;
	}

	@Override
	public void spawnAdditionalDrops(BlockState state, World worldIn, BlockPos pos, ItemStack stack) {
		BeltTileEntity controllerTE = BeltHelper.getControllerTE(worldIn, pos);
		if (controllerTE != null)
			controllerTE.getInventory()
				.ejectAll();
	}

	@Override
	public boolean isFlammable(BlockState state, IBlockReader world, BlockPos pos, Direction face) {
		return false;
	}

	@Override
	public void onLanded(IBlockReader worldIn, Entity entityIn) {
		super.onLanded(worldIn, entityIn);
		BlockPos entityPosition = entityIn.getPosition();
		BlockPos beltPos = null;

		if (AllBlocks.BELT.has(worldIn.getBlockState(entityPosition)))
			beltPos = entityPosition;
		else if (AllBlocks.BELT.has(worldIn.getBlockState(entityPosition.down())))
			beltPos = entityPosition.down();
		if (beltPos == null)
			return;
		if (!(worldIn instanceof World))
			return;

		onEntityCollision(worldIn.getBlockState(beltPos), (World) worldIn, beltPos, entityIn);
	}

	@Override
	public void onEntityCollision(BlockState state, World worldIn, BlockPos pos, Entity entityIn) {
		if (state.get(SLOPE) == Slope.VERTICAL)
			return;
		if (entityIn instanceof PlayerEntity) {
			PlayerEntity player = (PlayerEntity) entityIn;
			if (player.isSneaking())
				return;
			if (player.abilities.isFlying)
				return;
		}

		BeltTileEntity belt = BeltHelper.getSegmentTE(worldIn, pos);
		if (belt == null || belt.getSpeed() == 0)
			return;
		if (entityIn instanceof ItemEntity && entityIn.isAlive()) {
			if (worldIn.isRemote)
				return;
			if (entityIn.getMotion().y > 0)
				return;
			withTileEntityDo(worldIn, pos, te -> {
				ItemEntity itemEntity = (ItemEntity) entityIn;
				IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
					.orElse(null);
				if (handler == null)
					return;
				ItemStack remainder = handler.insertItem(0, itemEntity.getItem()
					.copy(), false);
				if (remainder.isEmpty())
					itemEntity.remove();
			});
			return;
		}

		BeltTileEntity controller = BeltHelper.getControllerTE(worldIn, pos);
		if (controller == null || controller.passengers == null)
			return;
		if (controller.passengers.containsKey(entityIn)) {
			TransportedEntityInfo info = controller.passengers.get(entityIn);
			if (info.getTicksSinceLastCollision() != 0 || pos.equals(entityIn.getPosition()))
				info.refresh(pos, state);
		} else {
			controller.passengers.put(entityIn, new TransportedEntityInfo(pos, state));
			entityIn.onGround = true;
		}
	}

	@Override
	public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
		updateNeighbouringTunnel(worldIn, pos, state);
		withTileEntityDo(worldIn, pos, te -> {
			te.attachmentTracker.findAttachments(te);
		});
	}

	@Override
	public ActionResultType onUse(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn,
		BlockRayTraceResult hit) {
		if (player.isSneaking() || !player.isAllowEdit())
			return ActionResultType.PASS;
		ItemStack heldItem = player.getHeldItem(handIn);
		boolean isShaft = heldItem.getItem() == AllBlocks.SHAFT.get()
			.asItem();
		boolean isCasing = heldItem.getItem() == AllBlocks.BRASS_CASING.get()
			.asItem();
		boolean isDye = Tags.Items.DYES.contains(heldItem.getItem());
		boolean isHand = heldItem.isEmpty() && handIn == Hand.MAIN_HAND;

		if (isDye) {
			if (worldIn.isRemote)
				return ActionResultType.SUCCESS;
			withTileEntityDo(worldIn, pos, te -> {
				DyeColor dyeColor = DyeColor.getColor(heldItem);
				if (dyeColor == null)
					return;
				te.applyColor(dyeColor);
			});
			if (!player.isCreative())
				heldItem.shrink(1);
			return ActionResultType.SUCCESS;
		}

		BeltTileEntity belt = BeltHelper.getSegmentTE(worldIn, pos);
		if (belt == null)
			return ActionResultType.PASS;

		if (isHand) {
			BeltTileEntity controllerBelt = belt.getControllerTE();
			if (controllerBelt == null)
				return ActionResultType.PASS;
			if (worldIn.isRemote)
				return ActionResultType.SUCCESS;
			controllerBelt.getInventory()
				.forEachWithin(belt.index + .5f, .55f, (transportedItemStack) -> {
					player.inventory.placeItemBackInInventory(worldIn, transportedItemStack.stack);
					return Collections.emptyList();
				});
		}

		if (isShaft) {
			if (state.get(PART) != Part.MIDDLE)
				return ActionResultType.PASS;
			if (worldIn.isRemote)
				return ActionResultType.SUCCESS;
			if (!player.isCreative())
				heldItem.shrink(1);
			worldIn.setBlockState(pos, state.with(PART, Part.PULLEY), 2);
			belt.attachKinetics();
			return ActionResultType.SUCCESS;
		}

		if (isCasing) {
			if (state.get(CASING))
				return ActionResultType.PASS;
			if (state.get(SLOPE) == Slope.VERTICAL)
				return ActionResultType.PASS;
			if (!player.isCreative())
				heldItem.shrink(1);
			worldIn.setBlockState(pos, state.with(CASING, true), 2);
			return ActionResultType.SUCCESS;
		}

		return ActionResultType.PASS;
	}

	@Override
	public ActionResultType onWrenched(BlockState state, ItemUseContext context) {
		World world = context.getWorld();
		PlayerEntity player = context.getPlayer();

		if (state.get(CASING)) {
			if (world.isRemote)
				return ActionResultType.SUCCESS;
			world.setBlockState(context.getPos(), state.with(CASING, false), 3);
			if (!player.isCreative())
				player.inventory.placeItemBackInInventory(world, AllBlocks.BRASS_CASING.asStack());
			return ActionResultType.SUCCESS;
		}

		if (state.get(PART) == Part.PULLEY) {
			if (world.isRemote)
				return ActionResultType.SUCCESS;
			world.setBlockState(context.getPos(), state.with(PART, Part.MIDDLE), 2);
			BeltTileEntity belt = BeltHelper.getSegmentTE(world, context.getPos());
			if (belt != null) {
				belt.detachKinetics();
				belt.attachKinetics();
			}
			if (!player.isCreative())
				player.inventory.placeItemBackInInventory(world, AllBlocks.SHAFT.asStack());
			return ActionResultType.SUCCESS;
		}

		return ActionResultType.FAIL;
	}

	@Override
	protected void fillStateContainer(Builder<Block, BlockState> builder) {
		builder.add(SLOPE, PART, CASING);
		super.fillStateContainer(builder);
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return true;
	}

	@Override
	public PathNodeType getAiPathNodeType(BlockState state, IBlockReader world, BlockPos pos, MobEntity entity) {
		return PathNodeType.RAIL;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean addDestroyEffects(BlockState state, World world, BlockPos pos, ParticleManager manager) {
		// From Particle Manager, but reduced density for belts with lots of boxes
		VoxelShape voxelshape = state.getShape(world, pos);
		MutableInt amtBoxes = new MutableInt(0);
		voxelshape.forEachBox((x1, y1, z1, x2, y2, z2) -> amtBoxes.increment());
		double chance = 1d / amtBoxes.getValue();

		voxelshape.forEachBox((x1, y1, z1, x2, y2, z2) -> {
			double d1 = Math.min(1.0D, x2 - x1);
			double d2 = Math.min(1.0D, y2 - y1);
			double d3 = Math.min(1.0D, z2 - z1);
			int i = Math.max(2, MathHelper.ceil(d1 / 0.25D));
			int j = Math.max(2, MathHelper.ceil(d2 / 0.25D));
			int k = Math.max(2, MathHelper.ceil(d3 / 0.25D));

			for (int l = 0; l < i; ++l) {
				for (int i1 = 0; i1 < j; ++i1) {
					for (int j1 = 0; j1 < k; ++j1) {
						if (world.rand.nextDouble() > chance)
							continue;

						double d4 = ((double) l + 0.5D) / (double) i;
						double d5 = ((double) i1 + 0.5D) / (double) j;
						double d6 = ((double) j1 + 0.5D) / (double) k;
						double d7 = d4 * d1 + x1;
						double d8 = d5 * d2 + y1;
						double d9 = d6 * d3 + z1;
						manager
							.addEffect((new DiggingParticle(world, (double) pos.getX() + d7, (double) pos.getY() + d8,
								(double) pos.getZ() + d9, d4 - 0.5D, d5 - 0.5D, d6 - 0.5D, state)).setBlockPos(pos));
					}
				}
			}

		});
		return true;
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		return BeltShapes.getShape(state);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, IBlockReader worldIn, BlockPos pos,
		ISelectionContext context) {
		if (state.getBlock() != this)
			return VoxelShapes.empty();

		VoxelShape shape = getShape(state, worldIn, pos, context);
		try {
			if (context.getEntity() == null)
				return shape;

			BeltTileEntity belt = getTileEntity(worldIn, pos);
			BeltTileEntity controller = belt.getControllerTE();

			if (controller == null)
				return shape;
			if (controller.passengers == null || !controller.passengers.containsKey(context.getEntity())) {
				return BeltShapes.getCollisionShape(state);
			}

		} catch (TileEntityException e) {
		}
		return shape;
	}

	@Override
	public TileEntity createTileEntity(BlockState state, IBlockReader world) {
		return new BeltTileEntity();
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return state.get(CASING) && state.get(SLOPE) != Slope.VERTICAL ? BlockRenderType.MODEL
			: BlockRenderType.ENTITYBLOCK_ANIMATED;
	}

//	@Override // TODO 1.15 register layer
//	public boolean canRenderInLayer(BlockState state, BlockRenderLayer layer) {
//		return state.get(CASING) && state.get(SLOPE) != Slope.VERTICAL && layer == getRenderLayer();
//	}

	public static void initBelt(World world, BlockPos pos) {
		if (world.isRemote || world.getWorldType() == WorldType.DEBUG_ALL_BLOCK_STATES)
			return;

		BlockState state = world.getBlockState(pos);
		if (!AllBlocks.BELT.has(state))
			return;
		// Find controller
		int limit = 1000;
		BlockPos currentPos = pos;
		while (limit-- > 0) {
			BlockState currentState = world.getBlockState(currentPos);
			if (!AllBlocks.BELT.has(currentState)) {
				world.destroyBlock(pos, true);
				return;
			}
			BlockPos nextSegmentPosition = nextSegmentPosition(currentState, currentPos, false);
			if (nextSegmentPosition == null)
				break;
			if (!world.isAreaLoaded(nextSegmentPosition, 0))
				return;
			currentPos = nextSegmentPosition;
		}

		// Init belts
		int index = 0;
		List<BlockPos> beltChain = getBeltChain(world, currentPos);
		if (beltChain.size() < 2) {
			world.destroyBlock(currentPos, true);
			return;
		}

		for (BlockPos beltPos : beltChain) {
			TileEntity tileEntity = world.getTileEntity(beltPos);
			BlockState currentState = world.getBlockState(beltPos);

			if (tileEntity instanceof BeltTileEntity && AllBlocks.BELT.has(currentState)) {
				BeltTileEntity te = (BeltTileEntity) tileEntity;
				te.setController(currentPos);
				te.beltLength = beltChain.size();
				te.index = index;
				te.attachKinetics();
				te.markDirty();
				te.sendData();

				boolean isVertical = currentState.get(BeltBlock.SLOPE) == Slope.VERTICAL;

				if (currentState.get(CASING) && isVertical) {
					Block.spawnAsEntity(world, beltPos, AllBlocks.BRASS_CASING.asStack());
					world.setBlockState(beltPos, currentState.with(CASING, false), 2);
				}

				if (te.isController() && isVertical)
					te.getInventory()
						.ejectAll();
			} else {
				world.destroyBlock(currentPos, true);
				return;
			}
			index++;
		}

	}

	@Override
	public void onReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean isMoving) {
		if (world.isRemote)
			return;
		if (state.getBlock() == newState.getBlock())
			return;

		updateNeighbouringTunnel(world, pos, state);

		if (isMoving)
			return;
		TileEntity belt = world.getTileEntity(pos);
		if (belt instanceof BeltTileEntity)
			belt.remove();

		// Destroy chain
		for (boolean forward : Iterate.trueAndFalse) {
			BlockPos currentPos = nextSegmentPosition(state, pos, forward);
			if (currentPos == null)
				continue;
			BlockState currentState = world.getBlockState(currentPos);
			if (!AllBlocks.BELT.has(currentState))
				continue;
			if (currentState.get(CASING))
				Block.spawnAsEntity(world, currentPos, AllBlocks.BRASS_CASING.asStack());

			boolean hasPulley = false;
			TileEntity tileEntity = world.getTileEntity(currentPos);
			if (tileEntity instanceof BeltTileEntity) {
				BeltTileEntity te = (BeltTileEntity) tileEntity;
				if (te.isController())
					te.getInventory()
						.ejectAll();

				te.remove();
				hasPulley = te.hasPulley();
			}

			BlockState shaftState = AllBlocks.SHAFT.getDefaultState()
				.with(BlockStateProperties.AXIS, getRotationAxis(currentState));
			world.setBlockState(currentPos, hasPulley ? shaftState : Blocks.AIR.getDefaultState(), 3);
			world.playEvent(2001, currentPos, Block.getStateId(currentState));
		}
	}

	private void updateNeighbouringTunnel(World world, BlockPos pos, BlockState beltState) {
		boolean isEnd = beltState.get(PART) != Part.END;
		if (isEnd && beltState.get(PART) != Part.START)
			return;
		int offset = isEnd ? -1 : 1;
		BlockPos tunnelPos = pos.offset(beltState.get(HORIZONTAL_FACING), offset)
			.up();
		if (AllBlocks.BELT_TUNNEL.has(world.getBlockState(tunnelPos)))
			BeltTunnelBlock.updateTunnel(world, tunnelPos);
	}

	public enum Slope implements IStringSerializable {
		HORIZONTAL, UPWARD, DOWNWARD, VERTICAL;

		@Override
		public String getName() {
			return Lang.asId(name());
		}
	}

	public enum Part implements IStringSerializable {
		START, MIDDLE, END, PULLEY;

		@Override
		public String getName() {
			return Lang.asId(name());
		}
	}

	public static List<BlockPos> getBeltChain(World world, BlockPos controllerPos) {
		List<BlockPos> positions = new LinkedList<>();

		BlockState blockState = world.getBlockState(controllerPos);
		if (!AllBlocks.BELT.has(blockState))
			return positions;

		int limit = 1000;
		BlockPos current = controllerPos;
		while (limit-- > 0 && current != null) {
			BlockState state = world.getBlockState(current);
			if (!AllBlocks.BELT.has(state))
				break;
			positions.add(current);
			current = nextSegmentPosition(state, current, true);
		}

		return positions;
	}

	public static BlockPos nextSegmentPosition(BlockState state, BlockPos pos, boolean forward) {
		Direction direction = state.get(HORIZONTAL_FACING);
		Slope slope = state.get(SLOPE);
		Part part = state.get(PART);

		int offset = forward ? 1 : -1;

		if (part == Part.END && forward || part == Part.START && !forward)
			return null;
		if (slope == Slope.VERTICAL)
			return pos.up(direction.getAxisDirection() == AxisDirection.POSITIVE ? offset : -offset);
		pos = pos.offset(direction, offset);
		if (slope != Slope.HORIZONTAL)
			return pos.up(slope == Slope.UPWARD ? offset : -offset);
		return pos;
	}

	@Override
	protected boolean hasStaticPart() {
		return false;
	}

	public static boolean canAccessFromSide(Direction facing, BlockState belt) {
		if (facing == null)
			return true;
		if (!belt.get(BeltBlock.CASING))
			return false;
		Part part = belt.get(BeltBlock.PART);
		if (part != Part.MIDDLE && facing.getAxis() == belt.get(HORIZONTAL_FACING)
			.rotateY()
			.getAxis())
			return false;

		Slope slope = belt.get(BeltBlock.SLOPE);
		if (slope != Slope.HORIZONTAL) {
			if (slope == Slope.DOWNWARD && part == Part.END)
				return true;
			if (slope == Slope.UPWARD && part == Part.START)
				return true;
			Direction beltSide = belt.get(HORIZONTAL_FACING);
			if (slope == Slope.DOWNWARD)
				beltSide = beltSide.getOpposite();
			if (beltSide == facing)
				return false;
		}

		return true;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public IBlockColor getColorHandler() {
		return new BeltColor();
	}

	@Override
	public Class<BeltTileEntity> getTileEntityClass() {
		return BeltTileEntity.class;
	}

	@Override
	public ItemRequirement getRequiredItems(BlockState state) {
		List<ItemStack> required = new ArrayList<>();
		if (state.get(PART) != Part.MIDDLE)
			required.add(AllBlocks.SHAFT.asStack());
		if (state.get(CASING))
			required.add(AllBlocks.BRASS_CASING.asStack());
		if (state.get(PART) == Part.START)
			required.add(AllItems.BELT_CONNECTOR.asStack());
		if (required.isEmpty())
			return ItemRequirement.NONE;
		return new ItemRequirement(ItemUseType.CONSUME, required);
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rot) {
		BlockState rotate = super.rotate(state, rot);

		if (state.get(SLOPE) != Slope.VERTICAL)
			return rotate;
		if (state.get(HORIZONTAL_FACING)
			.getAxisDirection() != rotate.get(HORIZONTAL_FACING)
				.getAxisDirection()) {
			if (state.get(PART) == Part.START)
				return rotate.with(PART, Part.END);
			if (state.get(PART) == Part.END)
				return rotate.with(PART, Part.START);
		}

		return rotate;
	}

}
