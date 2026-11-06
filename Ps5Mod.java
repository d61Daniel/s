package com.ps5mod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class Ps5Mod implements ModInitializer {
    public static final String MOD_ID = "ps5mod";

    public static Block PS5_BLOCK;
    public static Block TV_BLOCK;
    public static Item HDMI_CABLE;
    public static BlockEntityType<TvBlockEntity> TV_BLOCK_ENTITY;

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        PS5_BLOCK = Registry.register(Registries.BLOCK, id("ps5"),
                new Ps5Block(AbstractBlock.Settings.create()
                        .strength(1.5f)
                        .sounds(BlockSoundGroup.METAL)
                        .nonOpaque()
                        .luminance(s -> s.get(Ps5Block.POWERED) ? 4 : 0)));

        TV_BLOCK = Registry.register(Registries.BLOCK, id("tv"),
                new TvBlock(AbstractBlock.Settings.create()
                        .strength(1.5f)
                        .sounds(BlockSoundGroup.METAL)
                        .nonOpaque()));

        Registry.register(Registries.ITEM, id("ps5"), new BlockItem(PS5_BLOCK, new Item.Settings()));
        Registry.register(Registries.ITEM, id("tv"), new BlockItem(TV_BLOCK, new Item.Settings()));
        HDMI_CABLE = Registry.register(Registries.ITEM, id("hdmi_cable"),
                new HdmiCableItem(new Item.Settings().maxCount(1)));

        TV_BLOCK_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("tv"),
                FabricBlockEntityTypeBuilder.create(TvBlockEntity::new, TV_BLOCK).build());

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(PS5_BLOCK);
            entries.add(TV_BLOCK);
            entries.add(HDMI_CABLE);
        });
    }
}
