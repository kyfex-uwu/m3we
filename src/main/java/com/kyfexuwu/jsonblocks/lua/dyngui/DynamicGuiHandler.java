package com.kyfexuwu.jsonblocks.lua.dyngui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;

public class DynamicGuiHandler extends ScreenHandler {
    public static ScreenHandlerType<DynamicGuiHandler> dynamicGuiHandler =
            new ScreenHandlerType<>(DynamicGuiHandler::new);

    public DynamicGuiHandler(int syncId, PlayerInventory playerInventory) {
        super(null, syncId);
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int index) {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ScreenHandlerType<?> getType(){
        return dynamicGuiHandler;
    }
}
