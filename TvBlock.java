package com.ps5mod;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.function.BiConsumer;

public class TvBlock extends BlockWithEntity {
    public static final MapCodec<TvBlock> CODEC = createCodec(TvBlock::new);
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;

    /** Preenchido pelo cliente: (posição da TV, posição da PS5) -> abre o ecrã. */
    public static BiConsumer<BlockPos, BlockPos> OPEN_SCREEN = (tv, ps5) -> {};

    private static final VoxelShape SHAPE_NS = Block.createCuboidShape(1, 0, 5, 15, 15, 11);
    private static final VoxelShape SHAPE_EW = Block.createCuboidShape(5, 0, 1, 11, 15, 15);

    public TvBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction d = state.get(FACING);
        return (d == Direction.NORTH || d == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TvBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (player.getMainHandStack().getItem() instanceof HdmiCableItem) {
            return ActionResult.PASS;
        }
        if (world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            BlockPos ps5 = (be instanceof TvBlockEntity tv) ? tv.getLinked() : null;
            if (ps5 == null) {
                player.sendMessage(Text.translatable("message.ps5mod.tv_no_cable"), true);
            } else {
                BlockState s = world.getBlockState(ps5);
                if (!s.isOf(Ps5Mod.PS5_BLOCK)) {
                    player.sendMessage(Text.translatable("message.ps5mod.tv_ps5_missing"), true);
                } else if (!s.get(Ps5Block.POWERED)) {
                    player.sendMessage(Text.translatable("message.ps5mod.tv_ps5_off"), true);
                } else {
                    OPEN_SCREEN.accept(pos, ps5);
                }
            }
        }
        return ActionResult.SUCCESS;
    }
}
