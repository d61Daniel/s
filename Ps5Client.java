package com.ps5mod.client;

import com.ps5mod.Ps5Block;
import com.ps5mod.Ps5Mod;
import com.ps5mod.TvBlock;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class Ps5Client implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        TvBlock.OPEN_SCREEN = (tv, ps5) ->
                MinecraftClient.getInstance().setScreen(new Ps5Screen(tv, ps5));

        // Quando a PS5 é desligada, da próxima vez que ligar arranca de novo (logo + menu).
        ClientTickEvents.END_WORLD_TICK.register(world ->
                Ps5Screen.BOOTED.removeIf(l -> {
                    BlockState s = world.getBlockState(BlockPos.fromLong(l));
                    return !(s.isOf(Ps5Mod.PS5_BLOCK) && s.get(Ps5Block.POWERED));
                }));
    }
}
