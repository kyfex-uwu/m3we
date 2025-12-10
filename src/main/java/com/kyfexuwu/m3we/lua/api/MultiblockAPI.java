package com.kyfexuwu.m3we.lua.api;

import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.JavaExclusiveTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

public class MultiblockAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        JavaExclusiveTable thisApi = new JavaExclusiveTable();

//        thisApi.javaSet("runCommand",new MiscAPI.runCommand(env));

        return CustomScript.finalizeAPI("Multiblock",thisApi,env);
    }
}
