package com.ps5mod;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class Ps5Block extends HorizontalFacingBlock {
    public static final MapCodec<Ps5Block> CODEC = createCodec(Ps5Block::new);
    public static final BooleanProperty POWERED = Properties.POWERED;

    private static final VoxelShape SHAPE_NS = Block.createCuboidShape(4, 0, 2, 12, 16, 14);
    private static final VoxelShape SHAPE_EW = Block.createCuboidShape(2, 0, 4, 14, 16, 12);

    public Ps5Block(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(POWERED, false));
    }

    @Override
    protected MapCodec<? extends HorizontalFacingBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction d = state.get(FACING);
        return (d == Direction.NORTH || d == Direction.SOUTH) ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        // Com o cabo na mão, deixa o item tratar da ligação.
        if (player.getMainHandStack().getItem() instanceof HdmiCableItem) {
            return ActionResult.PASS;
        }
        if (!world.isClient) {
            boolean on = !state.get(POWERED);
            world.setBlockState(pos, state.with(POWERED, on), Block.NOTIFY_ALL);
            world.playSound(null, pos,
                    on ? SoundEvents.BLOCK_BEACON_ACTIVATE : SoundEvents.BLOCK_BEACON_DEACTIVATE,
                    SoundCategory.BLOCKS, 0.6f, on ? 1.6f : 1.2f);
            player.sendMessage(Text.translatable(on ? "message.ps5mod.ps5_on" : "message.ps5mod.ps5_off"), true);
        }
        return ActionResult.SUCCESS;
    }
}
