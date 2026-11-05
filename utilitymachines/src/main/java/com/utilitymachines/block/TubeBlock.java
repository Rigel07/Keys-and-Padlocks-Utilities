package com.utilitymachines.block;

import com.utilitymachines.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class TubeBlock extends BaseEntityBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private static final Map<Direction, BooleanProperty> PROPS = new EnumMap<>(Direction.class);

    static {
        PROPS.put(Direction.NORTH, NORTH);
        PROPS.put(Direction.EAST, EAST);
        PROPS.put(Direction.SOUTH, SOUTH);
        PROPS.put(Direction.WEST, WEST);
        PROPS.put(Direction.UP, UP);
        PROPS.put(Direction.DOWN, DOWN);
    }

    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    // Orden de Direction.get3DDataValue(): DOWN, UP, NORTH, SOUTH, WEST, EAST
    private static final VoxelShape[] ARMS = {
            Block.box(5, 0, 5, 11, 5, 11),
            Block.box(5, 11, 5, 11, 16, 11),
            Block.box(5, 5, 0, 11, 11, 5),
            Block.box(5, 5, 11, 11, 11, 16),
            Block.box(0, 5, 5, 5, 11, 11),
            Block.box(11, 5, 5, 16, 11, 11)
    };

    public TubeBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty prop : PROPS.values()) {
            state = state.setValue(prop, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    private boolean canConnect(LevelAccessor level, BlockPos pos, Direction dir) {
        BlockPos neighbor = pos.relative(dir);
        if (level.getBlockState(neighbor).getBlock() instanceof TubeBlock) return true;
        BlockEntity be = level.getBlockEntity(neighbor);
        return be != null && be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).isPresent();
    }

    private BlockState computeState(LevelAccessor level, BlockPos pos) {
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.values()) {
            state = state.setValue(PROPS.get(dir), canConnect(level, pos, dir));
        }
        return state;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return computeState(context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PROPS.get(direction), canConnect(level, pos, direction));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction dir : Direction.values()) {
            if (state.getValue(PROPS.get(dir))) {
                shape = Shapes.or(shape, ARMS[dir.get3DDataValue()]);
            }
        }
        return shape;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TubeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModRegistry.TUBE_BE.get(), TubeBlockEntity::serverTick);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TubeBlockEntity tube && !tube.getBuffer().isEmpty()) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), tube.getBuffer());
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }
}
