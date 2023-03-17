package com.kyfexuwu.jsonblocks.lua.dyngui;

import com.kyfexuwu.jsonblocks.Utils;
import com.kyfexuwu.jsonblocks.lua.ScriptError;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Pair;
import org.apache.logging.log4j.util.TriConsumer;
import org.luaj.vm2.*;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DynamicGuiBuilder {
    public final Consumer<DynamicGui> drawPrep;
    public final Runnable guiBehavior;
    public int slotCount;
    public boolean hasPlayerInventory;

    public DynamicGuiBuilder(LuaValue value){
        this.drawPrep = (gui) -> {
            ScriptError.execute(()->{
                value.get("onClient").call(Utils.toLuaValue(gui));
            });
        };
        this.guiBehavior = () -> {
            ScriptError.execute(()->{
                value.get("onServer").call();
            });
        };
        ScriptError.execute(()->{
            this.slotCount = value.get("register").get("slots").checkint();
        });
        ScriptError.execute(()->{
            this.hasPlayerInventory = value.get("register").get("hasPlayerInventory").checkboolean();
        });
    }

    public DynamicGuiHandler build(int syncId, PlayerInventory inventory, PlayerEntity player, String guiName){
        return new DynamicGuiHandler(syncId, inventory, guiName);
    }
}
