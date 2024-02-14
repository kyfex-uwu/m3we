package com.kyfexuwu.m3we.lua.api;

import com.kyfexuwu.m3we.Utils;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.JavaExclusiveTable;
import com.kyfexuwu.m3we.lua.dyngui.DynamicGuiBuilder;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.HashMap;

public class RegistryAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        JavaExclusiveTable thisApi = new JavaExclusiveTable();
        thisApi.javaSet("registerGui",new registerGUI(env));

        return CustomScript.finalizeAPI("Registry",thisApi,env);
    }

    static final HashMap<String, DynamicGuiBuilder> guis = new HashMap<>();
    static final class registerGUI extends TwoArgFunction {
        private final LuaValue globals;
        public registerGUI(LuaValue globals){
            this.globals=globals;
        }
        @Override
        public LuaValue call(LuaValue guiName, LuaValue guiBehavior) {
            try{
                guis.put(guiName.checkjstring(),new DynamicGuiBuilder(this.globals, guiBehavior));
                return TRUE;
            }catch(Exception e){
                e.printStackTrace();
                return FALSE;
            }
        }
    }
    public static DynamicGuiBuilder getGui(String name){
        if(!guis.containsKey(name)) return null;
        return guis.get(name);
    }
}
