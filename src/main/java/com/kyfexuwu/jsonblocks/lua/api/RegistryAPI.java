package com.kyfexuwu.jsonblocks.lua.api;

import com.kyfexuwu.jsonblocks.Utils;
import com.kyfexuwu.jsonblocks.lua.dyngui.DynamicGuiBuilder;
import net.minecraft.util.Pair;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.HashMap;

public class RegistryAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable thisApi = new LuaTable();
        thisApi.set("registerGui",new RegisterGUI());
        thisApi.set("getGui",new GetGUI());

        env.set("Registry", thisApi);
        env.get("package").get("loaded").set("Registry", thisApi);
        return thisApi;
    }

    static final HashMap<String, Pair<DynamicGuiBuilder, LuaFunction>> guis = new HashMap<>();
    static final class RegisterGUI extends ThreeArgFunction {
        @Override
        public LuaValue call(LuaValue guiName, LuaValue guiVisuals, LuaValue guiBehavior) {
            //if(guis.containsKey(guiName.checkjstring())) return FALSE;
            //to allow re-registering during development

            try{
                guis.put(guiName.checkjstring(),
                        new Pair<>((DynamicGuiBuilder) Utils.toObject(guiVisuals),guiBehavior.checkfunction()));
                return TRUE;
            }catch(Exception e){
                e.printStackTrace();
                return FALSE;
            }
        }
    }
    static final class GetGUI extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue guiName) {
            var toReturn = getGui(guiName.tojstring());
            if(toReturn==null) return NIL;
            return Utils.toLuaValue(toReturn);
        }
    }
    public static Pair<DynamicGuiBuilder, LuaFunction> getGui(String name){
        if(!guis.containsKey(name)) return null;
        return guis.get(name);
    }
}
