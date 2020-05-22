package com.simibubi.create.modules.contraptions.components.contraptions;

import static com.simibubi.create.modules.contraptions.components.contraptions.piston.MechanicalPistonBlock.isExtensionPole;
import static com.simibubi.create.modules.contraptions.components.contraptions.piston.MechanicalPistonBlock.isPistonHead;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.config.AllConfigs;
import com.simibubi.create.foundation.utility.Iterate;
import com.simibubi.create.foundation.utility.NBTHelper;
import com.simibubi.create.foundation.utility.VecHelper;
import com.simibubi.create.foundation.utility.WrappedWorld;
import com.simibubi.create.modules.contraptions.base.KineticTileEntity;
import com.simibubi.create.modules.contraptions.components.contraptions.chassis.AbstractChassisBlock;
import com.simibubi.create.modules.contraptions.components.contraptions.chassis.ChassisTileEntity;
import com.simibubi.create.modules.contraptions.components.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.modules.contraptions.components.contraptions.glue.SuperGlueHandler;
import com.simibubi.create.modules.contraptions.components.contraptions.piston.MechanicalPistonBlock;
import com.simibubi.create.modules.contraptions.components.contraptions.piston.MechanicalPistonBlock.PistonState;
import com.simibubi.create.modules.contraptions.components.contraptions.piston.PistonExtensionPoleBlock;
import com.simibubi.create.modules.contraptions.components.contraptions.pulley.PulleyBlock;
import com.simibubi.create.modules.contraptions.components.contraptions.pulley.PulleyBlock.MagnetBlock;
import com.simibubi.create.modules.contraptions.components.contraptions.pulley.PulleyBlock.RopeBlock;
import com.simibubi.create.modules.contraptions.components.contraptions.pulley.PulleyTileEntity;
import com.simibubi.create.modules.contraptions.components.saw.SawBlock;
import com.simibubi.create.modules.contraptions.redstone.RedstoneContactBlock;
import com.simibubi.create.modules.contraptions.relays.belt.BeltBlock;
import com.simibubi.create.modules.logistics.block.inventories.AdjustableCrateBlock;

import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.block.SlimeBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.state.properties.ChestType;
import net.minecraft.state.properties.DoubleBlockHalf;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.Direction.AxisDirection;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.template.Template.BlockInfo;
import net.minecraftforge.common.util.Constants.BlockFlags;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

public abstract class Contraption {

	public Map<BlockPos, BlockInfo> blocks;
	public Map<BlockPos, MountedStorage> storage;
	public List<MutablePair<BlockInfo, MovementContext>> actors;
	public CombinedInvWrapper inventory;
	public List<TileEntity> customRenderTEs;
	public Set<Pair<BlockPos, Direction>> superglue;

	public AxisAlignedBB bounds;
	public boolean stalled;

	protected Set<BlockPos> cachedColliders;
	protected Direction cachedColliderDirection;
	protected BlockPos anchor;

	List<BlockPos> renderOrder;
	protected List<SuperGlueEntity> glueToRemove;

	public Contraption() {
		blocks = new HashMap<>();
		storage = new HashMap<>();
		actors = new ArrayList<>();
		superglue = new HashSet<>();
		renderOrder = new ArrayList<>();
		customRenderTEs = new ArrayList<>();
		glueToRemove = new ArrayList<>();
	}

	public Set<BlockPos> getColliders(World world, Direction movementDirection) {
		if (blocks == null)
			return null;
		if (cachedColliders == null || cachedColliderDirection != movementDirection) {
			cachedColliders = new HashSet<>();
			cachedColliderDirection = movementDirection;

			for (BlockInfo info : blocks.values()) {
				BlockPos offsetPos = info.pos.offset(movementDirection);
				if (info.state.getCollisionShape(world, offsetPos)
					.isEmpty())
					continue;
				if (blocks.containsKey(offsetPos) && !blocks.get(offsetPos).state.getCollisionShape(world, offsetPos)
					.isEmpty())
					continue;
				cachedColliders.add(info.pos);
			}

		}
		return cachedColliders;
	}

	public boolean searchMovedStructure(World world, BlockPos pos, @Nullable Direction forcedDirection) {
		List<BlockPos> frontier = new ArrayList<>();
		Set<BlockPos> visited = new HashSet<>();
		anchor = pos;

		if (bounds == null)
			bounds = new AxisAlignedBB(BlockPos.ZERO);

		if (!BlockMovementTraits.isBrittle(world.getBlockState(pos)))
			frontier.add(pos);
		if (!addToInitialFrontier(world, pos, forcedDirection, frontier))
			return false;
		for (int limit = 100000; limit > 0; limit--) {
			if (frontier.isEmpty())
				return true;
			if (!moveBlock(world, frontier.remove(0), forcedDirection, frontier, visited))
				return false;
		}
		return false;
	}

	public void gatherStoredItems() {
		List<IItemHandlerModifiable> list = storage.values()
			.stream()
			.map(MountedStorage::getItemHandler)
			.collect(Collectors.toList());
		inventory = new CombinedInvWrapper(Arrays.copyOf(list.toArray(), list.size(), IItemHandlerModifiable[].class));
	}

	protected boolean addToInitialFrontier(World world, BlockPos pos, Direction forcedDirection,
		List<BlockPos> frontier) {
		return true;
	}

	protected boolean moveBlock(World world, BlockPos pos, Direction forcedDirection, List<BlockPos> frontier,
		Set<BlockPos> visited) {
		visited.add(pos);
		frontier.remove(pos);

		if (!world.isBlockPresent(pos))
			return false;
		if (isAnchoringBlockAt(pos))
			return true;
		if (!BlockMovementTraits.movementNecessary(world, pos))
			return true;
		if (!BlockMovementTraits.movementAllowed(world, pos))
			return false;
		BlockState state = world.getBlockState(pos);
		if (isChassis(state) && !moveChassis(world, pos, forcedDirection, frontier, visited))
			return false;

		if (AllBlocks.ADJUSTABLE_CRATE.has(state))
			AdjustableCrateBlock.splitCrate(world, pos);
		if (AllBlocks.BELT.has(state)) {
			BlockPos nextPos = BeltBlock.nextSegmentPosition(state, pos, true);
			BlockPos prevPos = BeltBlock.nextSegmentPosition(state, pos, false);
			if (nextPos != null && !visited.contains(nextPos))
				frontier.add(nextPos);
			if (prevPos != null && !visited.contains(prevPos))
				frontier.add(prevPos);
		}

		// Pulleys drag their rope and their attached structure
		if (state.getBlock() instanceof PulleyBlock) {
			int limit = AllConfigs.SERVER.kinetics.maxRopeLength.get();
			BlockPos ropePos = pos;
			while (limit-- >= 0) {
				ropePos = ropePos.down();
				if (!world.isBlockPresent(ropePos))
					break;
				BlockState ropeState = world.getBlockState(ropePos);
				Block block = ropeState.getBlock();
				if (!(block instanceof RopeBlock) && !(block instanceof MagnetBlock)) {
					if (!visited.contains(ropePos))
						frontier.add(ropePos);
					break;
				}
				add(ropePos, capture(world, ropePos));
			}
		}

		// Pistons drag their attaches poles and extension
		if (state.getBlock() instanceof MechanicalPistonBlock) {
			int limit = AllConfigs.SERVER.kinetics.maxPistonPoles.get();
			Direction direction = state.get(MechanicalPistonBlock.FACING);
			if (state.get(MechanicalPistonBlock.STATE) == PistonState.EXTENDED) {
				BlockPos searchPos = pos;
				while (limit-- >= 0) {
					searchPos = searchPos.offset(direction);
					BlockState blockState = world.getBlockState(searchPos);
					if (isExtensionPole(blockState)) {
						if (blockState.get(PistonExtensionPoleBlock.FACING)
							.getAxis() != direction.getAxis())
							break;
						if (!visited.contains(searchPos))
							frontier.add(searchPos);
						continue;
					}
					if (isPistonHead(blockState))
						if (!visited.contains(searchPos))
							frontier.add(searchPos);
					break;
				}
				if (limit <= -1)
					return false;
			}

			BlockPos searchPos = pos;
			while (limit-- >= 0) {
				searchPos = searchPos.offset(direction.getOpposite());
				BlockState blockState = world.getBlockState(searchPos);
				if (isExtensionPole(blockState)) {
					if (blockState.get(PistonExtensionPoleBlock.FACING)
						.getAxis() != direction.getAxis())
						break;
					if (!visited.contains(searchPos))
						frontier.add(searchPos);
					continue;
				}
				break;
			}

			if (limit <= -1)
				return false;
		}

		// Doors try to stay whole
		if (state.getBlock() instanceof DoorBlock) {
			BlockPos otherPartPos = pos.up(state.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? 1 : -1);
			if (!visited.contains(otherPartPos))
				frontier.add(otherPartPos);
		}

		Map<Direction, SuperGlueEntity> superglue = SuperGlueHandler.gatherGlue(world, pos);

		// Slime blocks drag adjacent blocks if possible
		boolean isSlimeBlock = state.getBlock() instanceof SlimeBlock;
		for (Direction offset : Direction.values()) {
			BlockPos offsetPos = pos.offset(offset);
			BlockState blockState = world.getBlockState(offsetPos);
			if (isAnchoringBlockAt(offsetPos))
				continue;
			if (!BlockMovementTraits.movementAllowed(world, offsetPos)) {
				if (offset == forcedDirection && isSlimeBlock)
					return false;
				continue;
			}

			boolean wasVisited = visited.contains(offsetPos);
			boolean faceHasGlue = superglue.containsKey(offset);
			boolean blockAttachedTowardsFace =
				BlockMovementTraits.isBlockAttachedTowards(blockState, offset.getOpposite());
			boolean brittle = BlockMovementTraits.isBrittle(blockState);

			if (!wasVisited && ((isSlimeBlock && !brittle) || blockAttachedTowardsFace || faceHasGlue))
				frontier.add(offsetPos);

			if (faceHasGlue)
				addGlue(superglue.get(offset));
		}

		add(pos, capture(world, pos));
		if (blocks.size() > AllConfigs.SERVER.kinetics.maxBlocksMoved.get())
			return false;

		return true;
	}

	protected boolean isAnchoringBlockAt(BlockPos pos) {
		return pos.equals(anchor);
	}

	protected static boolean isChassis(BlockState state) {
		return state.getBlock() instanceof AbstractChassisBlock;
	}

	private boolean moveChassis(World world, BlockPos pos, Direction movementDirection, List<BlockPos> frontier,
		Set<BlockPos> visited) {
		TileEntity te = world.getTileEntity(pos);
		if (!(te instanceof ChassisTileEntity))
			return false;
		ChassisTileEntity chassis = (ChassisTileEntity) te;
		chassis.addAttachedChasses(frontier, visited);
		List<BlockPos> includedBlockPositions = chassis.getIncludedBlockPositions(movementDirection, false);
		if (includedBlockPositions == null)
			return false;
		for (BlockPos blockPos : includedBlockPositions)
			if (!visited.contains(blockPos))
				frontier.add(blockPos);
		return true;
	}

	protected Pair<BlockInfo, TileEntity> capture(World world, BlockPos pos) {
		BlockState blockstate = world.getBlockState(pos);
		if (AllBlocks.SAW.has(blockstate))
			blockstate = blockstate.with(SawBlock.RUNNING, true);
		if (blockstate.getBlock() instanceof ChestBlock)
			blockstate = blockstate.with(ChestBlock.TYPE, ChestType.SINGLE);
		if (AllBlocks.ADJUSTABLE_CRATE.has(blockstate))
			blockstate = blockstate.with(AdjustableCrateBlock.DOUBLE, false);
		if (AllBlocks.REDSTONE_CONTACT.has(blockstate))
			blockstate = blockstate.with(RedstoneContactBlock.POWERED, true);
		if (blockstate.getBlock() instanceof AbstractButtonBlock) {
			blockstate = blockstate.with(AbstractButtonBlock.POWERED, false);
			world.getPendingBlockTicks()
				.scheduleTick(pos, blockstate.getBlock(), -1);
		}
		if (blockstate.getBlock() instanceof PressurePlateBlock) {
			blockstate = blockstate.with(PressurePlateBlock.POWERED, false);
			world.getPendingBlockTicks()
				.scheduleTick(pos, blockstate.getBlock(), -1);
		}
		CompoundNBT compoundnbt = getTileEntityNBT(world, pos);
		TileEntity tileentity = world.getTileEntity(pos);
		return Pair.of(new BlockInfo(pos, blockstate, compoundnbt), tileentity);
	}

	public static CompoundNBT getTileEntityNBT(World world, BlockPos pos) {
		TileEntity tileentity = world.getTileEntity(pos);
		CompoundNBT compoundnbt = null;
		if (tileentity != null) {
			compoundnbt = tileentity.write(new CompoundNBT());
			compoundnbt.remove("x");
			compoundnbt.remove("y");
			compoundnbt.remove("z");
		}
		return compoundnbt;
	}

	public void addGlue(SuperGlueEntity entity) {
		BlockPos pos = entity.getHangingPosition();
		Direction direction = entity.getFacingDirection();
		BlockPos localPos = pos.subtract(anchor);
		this.superglue.add(Pair.of(localPos, direction));
		glueToRemove.add(entity);
	}

	public void add(BlockPos pos, Pair<BlockInfo, TileEntity> pair) {
		BlockInfo captured = pair.getKey();
		BlockPos localPos = pos.subtract(anchor);
		BlockInfo blockInfo = new BlockInfo(localPos, captured.state, captured.nbt);

		if (blocks.put(localPos, blockInfo) != null)
			return;
		bounds = bounds.union(new AxisAlignedBB(localPos));

		TileEntity te = pair.getValue();
		if (te != null && MountedStorage.canUseAsStorage(te))
			storage.put(localPos, new MountedStorage(te));
		if (captured.state.getBlock() instanceof IPortableBlock)
			getActors().add(MutablePair.of(blockInfo, null));
	}

	public static Contraption fromNBT(World world, CompoundNBT nbt) {
		String type = nbt.getString("Type");
		Contraption contraption = AllContraptionTypes.fromType(type);
		contraption.readNBT(world, nbt);
		return contraption;
	}

	public void readNBT(World world, CompoundNBT nbt) {
		blocks.clear();
		renderOrder.clear();
		customRenderTEs.clear();

		nbt.getList("Blocks", 10)
			.forEach(c -> {
				CompoundNBT comp = (CompoundNBT) c;
				BlockInfo info = new BlockInfo(NBTUtil.readBlockPos(comp.getCompound("Pos")),
					NBTUtil.readBlockState(comp.getCompound("Block")),
					comp.contains("Data") ? comp.getCompound("Data") : null);
				blocks.put(info.pos, info);

				if (world.isRemote) {
					Block block = info.state.getBlock();
					if (RenderTypeLookup.canRenderInLayer(info.state, RenderType.getTranslucent()))
						renderOrder.add(info.pos);
					else
						renderOrder.add(0, info.pos);
					CompoundNBT tag = info.nbt;
					if (tag == null || block instanceof IPortableBlock)
						return;

					tag.putInt("x", info.pos.getX());
					tag.putInt("y", info.pos.getY());
					tag.putInt("z", info.pos.getZ());

					TileEntity te = TileEntity.create(tag);
					te.setLocation(new WrappedWorld(world) {

						@Override
						public BlockState getBlockState(BlockPos pos) {
							if (!pos.equals(te.getPos()))
								return Blocks.AIR.getDefaultState();
							return info.state;
						}

					}, te.getPos());
					if (te instanceof KineticTileEntity)
						((KineticTileEntity) te).setSpeed(0);
					te.getBlockState();
					customRenderTEs.add(te);
				}
			});

		actors.clear();
		nbt.getList("Actors", 10)
			.forEach(c -> {
				CompoundNBT comp = (CompoundNBT) c;
				BlockInfo info = blocks.get(NBTUtil.readBlockPos(comp.getCompound("Pos")));
				MovementContext context = MovementContext.readNBT(world, info, comp);
				context.contraption = this;
				getActors().add(MutablePair.of(info, context));
			});

		superglue.clear();
		nbt.getList("Superglue", 10)
			.forEach(c -> {
				CompoundNBT comp = (CompoundNBT) c;
				superglue.add(Pair.of(NBTUtil.readBlockPos(comp.getCompound("Pos")),
					Direction.byIndex(comp.getByte("Direction"))));
			});

		storage.clear();
		nbt.getList("Storage", 10)
			.forEach(c -> {
				CompoundNBT comp = (CompoundNBT) c;
				storage.put(NBTUtil.readBlockPos(comp.getCompound("Pos")),
					new MountedStorage(comp.getCompound("Data")));
			});
		List<IItemHandlerModifiable> list = storage.values()
			.stream()
			.map(MountedStorage::getItemHandler)
			.collect(Collectors.toList());
		inventory = new CombinedInvWrapper(Arrays.copyOf(list.toArray(), list.size(), IItemHandlerModifiable[].class));

		if (nbt.contains("BoundsFront"))
			bounds = NBTHelper.readAABB(nbt.getList("BoundsFront", 5));

		stalled = nbt.getBoolean("Stalled");
		anchor = NBTUtil.readBlockPos(nbt.getCompound("Anchor"));
	}

	public CompoundNBT writeNBT() {
		CompoundNBT nbt = new CompoundNBT();
		nbt.putString("Type", getType().id);
		ListNBT blocksNBT = new ListNBT();
		for (BlockInfo block : this.blocks.values()) {
			CompoundNBT c = new CompoundNBT();
			c.put("Block", NBTUtil.writeBlockState(block.state));
			c.put("Pos", NBTUtil.writeBlockPos(block.pos));
			if (block.nbt != null)
				c.put("Data", block.nbt);
			blocksNBT.add(c);
		}

		ListNBT actorsNBT = new ListNBT();
		for (MutablePair<BlockInfo, MovementContext> actor : getActors()) {
			CompoundNBT compound = new CompoundNBT();
			compound.put("Pos", NBTUtil.writeBlockPos(actor.left.pos));
			getMovement(actor.left.state).writeExtraData(actor.right);
			actor.right.writeToNBT(compound);
			actorsNBT.add(compound);
		}

		ListNBT superglueNBT = new ListNBT();
		for (Pair<BlockPos, Direction> glueEntry : superglue) {
			CompoundNBT c = new CompoundNBT();
			c.put("Pos", NBTUtil.writeBlockPos(glueEntry.getKey()));
			c.putByte("Direction", (byte) glueEntry.getValue()
				.getIndex());
			superglueNBT.add(c);
		}

		ListNBT storageNBT = new ListNBT();
		for (BlockPos pos : storage.keySet()) {
			CompoundNBT c = new CompoundNBT();
			MountedStorage mountedStorage = storage.get(pos);
			if (!mountedStorage.isWorking())
				continue;
			c.put("Pos", NBTUtil.writeBlockPos(pos));
			c.put("Data", mountedStorage.serialize());
			storageNBT.add(c);
		}

		nbt.put("Blocks", blocksNBT);
		nbt.put("Actors", actorsNBT);
		nbt.put("Superglue", superglueNBT);
		nbt.put("Storage", storageNBT);
		nbt.put("Anchor", NBTUtil.writeBlockPos(anchor));
		nbt.putBoolean("Stalled", stalled);

		if (bounds != null) {
			ListNBT bb = NBTHelper.writeAABB(bounds);
			nbt.put("BoundsFront", bb);
		}

		return nbt;
	}

	public static boolean isFrozen() {
		return AllConfigs.SERVER.control.freezeContraptions.get();
	}

	public void removeBlocksFromWorld(IWorld world, BlockPos offset) {
		removeBlocksFromWorld(world, offset, (pos, state) -> false);
	}

	public void removeBlocksFromWorld(IWorld world, BlockPos offset, BiPredicate<BlockPos, BlockState> customRemoval) {
		storage.values()
			.forEach(MountedStorage::empty);
		glueToRemove.forEach(SuperGlueEntity::remove);

		for (boolean brittles : Iterate.trueAndFalse) {
			for (Iterator<BlockInfo> iterator = blocks.values()
				.iterator(); iterator.hasNext();) {
				BlockInfo block = iterator.next();
				if (brittles != BlockMovementTraits.isBrittle(block.state))
					continue;

				BlockPos add = block.pos.add(anchor)
					.add(offset);
				if (customRemoval.test(add, block.state))
					continue;
				Block blockIn = world.getBlockState(add)
					.getBlock();
				if (block.state.getBlock() != blockIn)
					iterator.remove();
				world.getWorld()
					.removeTileEntity(add);
				int flags = 67;
				if (blockIn instanceof DoorBlock)
					flags = flags | 32 | 16;
				world.setBlockState(add, Blocks.AIR.getDefaultState(), flags);
			}
		}
	}

	public void addBlocksToWorld(World world, BlockPos offset, Vec3d rotation) {
		addBlocksToWorld(world, offset, rotation, (pos, state) -> false);
	}

	public void addBlocksToWorld(World world, BlockPos offset, Vec3d rotation,
		BiPredicate<BlockPos, BlockState> customPlacement) {
		stop(world);

		StructureTransform transform = new StructureTransform(offset, rotation);

		for (boolean nonBrittles : Iterate.trueAndFalse) {
			for (BlockInfo block : blocks.values()) {
				if (nonBrittles == BlockMovementTraits.isBrittle(block.state))
					continue;

				BlockPos targetPos = transform.apply(block.pos);
				BlockState state = transform.apply(block.state);

				if (customPlacement.test(targetPos, state))
					continue;

				if (nonBrittles)
					for (Direction face : Direction.values())
						state = state.updatePostPlacement(face, world.getBlockState(targetPos.offset(face)), world,
							targetPos, targetPos.offset(face));

				if (AllBlocks.SAW.has(state))
					state = state.with(SawBlock.RUNNING, false);

				BlockState blockState = world.getBlockState(targetPos);
				if (blockState.getBlockHardness(world, targetPos) == -1 || (state.getCollisionShape(world, targetPos)
					.isEmpty()
					&& !blockState.getCollisionShape(world, targetPos)
						.isEmpty())) {
					if (targetPos.getY() == 0)
						targetPos = targetPos.up();
					world.playEvent(2001, targetPos, Block.getStateId(state));
					Block.spawnDrops(state, world, targetPos, null);
					continue;
				}

				world.destroyBlock(targetPos, true);
				world.setBlockState(targetPos, state, 3 | BlockFlags.IS_MOVING);

				boolean verticalRotation = transform.rotationAxis == null || transform.rotationAxis.isHorizontal();
				verticalRotation = verticalRotation && transform.rotation != Rotation.NONE;
				if (verticalRotation) {
					if (state.getBlock() instanceof RopeBlock || state.getBlock() instanceof MagnetBlock)
						world.destroyBlock(targetPos, true);
				}

				TileEntity tileEntity = world.getTileEntity(targetPos);
				CompoundNBT tag = block.nbt;
				if (tileEntity != null && tag != null) {
					tag.putInt("x", targetPos.getX());
					tag.putInt("y", targetPos.getY());
					tag.putInt("z", targetPos.getZ());

					if (verticalRotation && tileEntity instanceof PulleyTileEntity) {
						tag.remove("Offset");
						tag.remove("InitialOffset");
					}

					tileEntity.read(tag);

					if (storage.containsKey(block.pos)) {
						MountedStorage mountedStorage = storage.get(block.pos);
						if (mountedStorage.isWorking())
							mountedStorage.fill(tileEntity);
					}
				}
			}
		}

		for (Pair<BlockPos, Direction> pair : superglue) {
			BlockPos targetPos = transform.apply(pair.getKey());
			Direction targetFacing = transform.transformFacing(pair.getValue());

			SuperGlueEntity entity = new SuperGlueEntity(world, targetPos, targetFacing);
			if (entity.onValidSurface()) {
				if (!world.isRemote)
					world.addEntity(entity);
			}

		}

	}

	public void initActors(World world) {
		for (MutablePair<BlockInfo, MovementContext> pair : actors) {
			MovementContext context = new MovementContext(world, pair.left);
			context.contraption = this;
			getMovement(pair.left.state).startMoving(context);
			pair.setRight(context);
		}
	}

	public AxisAlignedBB getBoundingBox() {
		return bounds;
	}

	public List<MutablePair<BlockInfo, MovementContext>> getActors() {
		return actors;
	}

	public BlockPos getAnchor() {
		return anchor;
	}

	public void stop(World world) {
		foreachActor(world, (behaviour, ctx) -> {
			behaviour.stopMoving(ctx);
			ctx.position = null;
			ctx.motion = Vec3d.ZERO;
			ctx.relativeMotion = Vec3d.ZERO;
			ctx.rotation = Vec3d.ZERO;
		});
	}

	public void foreachActor(World world, BiConsumer<MovementBehaviour, MovementContext> callBack) {
		for (MutablePair<BlockInfo, MovementContext> pair : actors)
			callBack.accept(getMovement(pair.getLeft().state), pair.getRight());
	}

	protected static MovementBehaviour getMovement(BlockState state) {
		Block block = state.getBlock();
		if (!(block instanceof IPortableBlock))
			return null;
		return ((IPortableBlock) block).getMovementBehaviour();
	}

	public void expandBoundsAroundAxis(Axis axis) {
		AxisAlignedBB bb = bounds;
		double maxXDiff = Math.max(bb.maxX - 1, -bb.minX);
		double maxYDiff = Math.max(bb.maxY - 1, -bb.minY);
		double maxZDiff = Math.max(bb.maxZ - 1, -bb.minZ);
		double maxDiff = 0;

		if (axis == Axis.X)
			maxDiff = Math.max(maxZDiff, maxYDiff);
		if (axis == Axis.Y)
			maxDiff = Math.max(maxZDiff, maxXDiff);
		if (axis == Axis.Z)
			maxDiff = Math.max(maxXDiff, maxYDiff);

		Vec3d vec = new Vec3d(Direction.getFacingFromAxis(AxisDirection.POSITIVE, axis)
			.getDirectionVec());
		Vec3d planeByNormal = VecHelper.planeByNormal(vec);
		Vec3d min = vec.mul(bb.minX, bb.minY, bb.minZ)
			.add(planeByNormal.scale(-maxDiff));
		Vec3d max = vec.mul(bb.maxX, bb.maxY, bb.maxZ)
			.add(planeByNormal.scale(maxDiff + 1));
		bounds = new AxisAlignedBB(min, max);
	}

	protected abstract AllContraptionTypes getType();

}