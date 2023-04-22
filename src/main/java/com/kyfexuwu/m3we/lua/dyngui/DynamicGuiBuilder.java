package com.kyfexuwu.m3we.lua.dyngui;

import com.kyfexuwu.m3we.Utils;
import com.kyfexuwu.m3we.lua.ScriptError;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.luaj.vm2.*;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DynamicGuiBuilder {
    public final Consumer<DynamicGui> drawPrep;
    public final Consumer<DynamicGuiHandler> guiBehavior;
    public final BiConsumer<DynamicGuiHandler, PlayerEntity> onClose;
    public int slotCount;
    public boolean hasPlayerInventory;

    public DynamicGuiBuilder(LuaValue value){
        this.drawPrep = (gui) -> {
            ScriptError.execute(()->{
                value.get("onClient").call(Utils.toLuaValue(gui));
            });
        };
        this.guiBehavior = (gui) -> {
            ScriptError.execute(()->{
                value.get("onServer").call(Utils.toLuaValue(gui));
            });
        };
        this.onClose = (gui,player) -> {
            ScriptError.execute(()->{
                value.get("onClose").call(Utils.toLuaValue(gui),Utils.toLuaValue(player));
            });
        };
        ScriptError.execute(()->{
            this.slotCount = value.get("register").get("slots").checkint();
        });
        ScriptError.execute(()->{
            this.hasPlayerInventory = value.get("register").get("hasPlayerInventory").checkboolean();
        });
    }

    public DynamicGuiHandler build(int syncId, PlayerInventory inventory, PlayerEntity player,
                                   World world, BlockPos pos, String guiName){
        return new DynamicGuiHandler(syncId, inventory, ScreenHandlerContext.create(world, pos), guiName);
    }
}
