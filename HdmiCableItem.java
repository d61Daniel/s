package com.ps5mod;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HdmiCableItem extends Item {
    /** Primeira ponta do cabo (PS5) por jogador. */
    private static final Map<UUID, BlockPos> PENDING = new HashMap<>();

    public HdmiCableItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        PlayerEntity player = ctx.getPlayer();
        if (world.isClient || player == null) {
            return ActionResult.SUCCESS;
        }
        BlockPos pos = ctx.getBlockPos();
        BlockState state = world.getBlockState(pos);

        if (state.isOf(Ps5Mod.PS5_BLOCK)) {
            PENDING.put(player.getUuid(), pos.toImmutable());
            player.sendMessage(Text.translatable("message.ps5mod.cable_ps5"), true);
            return ActionResult.SUCCESS;
        }

        if (state.isOf(Ps5Mod.TV_BLOCK)) {
            BlockPos ps5 = PENDING.get(player.getUuid());
            if (ps5 == null || !world.getBlockState(ps5).isOf(Ps5Mod.PS5_BLOCK)) {
                player.sendMessage(Text.translatable("message.ps5mod.cable_need_ps5"), true);
                return ActionResult.SUCCESS;
            }
            if (!pos.isWithinDistance(ps5, 24)) {
                player.sendMessage(Text.translatable("message.ps5mod.cable_too_far"), true);
                return ActionResult.SUCCESS;
            }
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof TvBlockEntity tv) {
                tv.setLinked(ps5);
                PENDING.remove(player.getUuid());
                player.sendMessage(Text.translatable("message.ps5mod.cable_linked"), true);
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }
}
