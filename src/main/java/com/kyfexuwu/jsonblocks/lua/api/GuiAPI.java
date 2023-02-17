package com.kyfexuwu.jsonblocks.lua.api;

import com.kyfexuwu.jsonblocks.Utils;
import com.kyfexuwu.jsonblocks.lua.dyngui.DynamicGui;
import com.kyfexuwu.jsonblocks.lua.dyngui.DynamicGuiBuilder;
import com.kyfexuwu.jsonblocks.lua.dyngui.DynamicGuiHandler;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

public class GuiAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable thisApi = new LuaTable();
        thisApi.set("new",new newGui());
        thisApi.set("open",new openGui());

        env.set("Gui", thisApi);
        env.get("package").get("loaded").set("Gui", thisApi);
        return thisApi;
    }

    static final class newGui extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue arg) {
            return Utils.toLuaValue(new DynamicGuiBuilder());
            //what is a "syncId"
            //do player inv?
        }
    }
    static final class openGui extends ThreeArgFunction {
        @Override
        public LuaValue call(LuaValue luaPlayer, LuaValue arg, LuaValue stateWorldPos) {
            if(!stateWorldPos.istable()||stateWorldPos.length()<3) return FALSE;

            PlayerEntity player;
            BlockState state;
            World world;
            BlockPos pos;
            DynamicGuiBuilder thisGui;
            try {
                player = (PlayerEntity) Utils.toObject(luaPlayer);
                state = (BlockState) Utils.toObject(stateWorldPos.get(1));
                world = (World) Utils.toObject(stateWorldPos.get(2));
                pos = (BlockPos) Utils.toObject(stateWorldPos.get(3));
                thisGui = (DynamicGuiBuilder) Utils.toObject(arg);
            }catch(Exception e){
                return FALSE;
            }

            try {
                player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                        (syncId,inventory,playerParam)->thisGui.build(syncId,inventory,playerParam),
                        Text.of("Custom Interface")));
            }catch(Exception e){
                e.printStackTrace();
            }
            return TRUE;
        }
    }
}
