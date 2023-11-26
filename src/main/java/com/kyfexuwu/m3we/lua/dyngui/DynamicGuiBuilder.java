package com.kyfexuwu.m3we.lua.dyngui;

import com.kyfexuwu.m3we.Utils;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.JavaExclusiveTable;
import com.kyfexuwu.m3we.lua.ScriptError;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.util.TriConsumer;
import org.luaj.vm2.*;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DynamicGuiBuilder {
    public final TriConsumer<DynamicGui, Integer, Integer> drawPrep;
    public final Consumer<DynamicGuiHandler> guiBehavior;
    public final BiConsumer<DynamicGuiHandler, PlayerEntity> onClose;
    public int slotCount;
    public boolean hasPlayerInventory;

    public DynamicGuiBuilder(LuaValue globals, LuaValue value){
        this.drawPrep = (gui, mX, mY) -> {
            ScriptError.execute(()->{
                var ctxTable=(JavaExclusiveTable)globals.get(CustomScript.contextIdentifier);
                ctxTable.javaSet("guiData",Utils.toLuaValue(gui));

                value.get("onClient").call(Utils.toLuaValue(gui), Utils.toLuaValue(mX), Utils.toLuaValue(mY));

                ctxTable.javaSet("guiData", LuaValue.NIL);
            });
        };
        this.guiBehavior = (guiHandler) -> {
            ScriptError.execute(()->{
                var ctxTable=(JavaExclusiveTable)globals.get(CustomScript.contextIdentifier);
                ctxTable.javaSet("guiHandler",Utils.toLuaValue(guiHandler));

                value.get("onServer").call(Utils.toLuaValue(guiHandler));

                ctxTable.javaSet("guiHandler", LuaValue.NIL);
            });
        };
        this.onClose = (guiHandler,player) -> {
            ScriptError.execute(()->{
                var ctxTable=(JavaExclusiveTable)globals.get(CustomScript.contextIdentifier);
                ctxTable.javaSet("guiHandler",Utils.toLuaValue(guiHandler));

                value.get("onClose").call(Utils.toLuaValue(guiHandler),Utils.toLuaValue(player));

                ctxTable.javaSet("guiHandler", LuaValue.NIL);
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
