package com.kyfexuwu.jsonblocks.lua.dyngui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.registry.Registry;

public class DynamicGuiHandler extends ScreenHandler {
    public static final ScreenHandlerType<DynamicGuiHandler> dynamicGuiHandler =
            Registry.register(Registry.SCREEN_HANDLER, "m3we_gui",
                    new ScreenHandlerType<>(DynamicGuiHandler::new));//todo
    public DynamicGuiHandler(int syncId, PlayerInventory playerInventory) {
        super(dynamicGuiHandler, syncId);
    }

    public DynamicGuiBuilder.GuiRect[] rects=new DynamicGuiBuilder.GuiRect[0];
    public DynamicGuiHandler(int syncId, PlayerInventory inventory, DynamicGuiBuilder.GuiRect[] rects) {
        this(syncId,inventory);
        this.rects=rects;
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int index) {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
