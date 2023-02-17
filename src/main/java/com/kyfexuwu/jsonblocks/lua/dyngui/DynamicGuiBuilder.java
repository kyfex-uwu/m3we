package com.kyfexuwu.jsonblocks.lua.dyngui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;

public class DynamicGuiBuilder {
    public DynamicGuiHandler build(int syncId, PlayerInventory inventory, PlayerEntity player){
        return new DynamicGuiHandler(syncId, inventory);
    }
}
