package com.kyfexuwu.m3we.lua.api;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

public class MiscAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        APITable thisApi = new APITable();

        //thisApi.set("runCommand",new runCommand());

        thisApi.locked = true;
        env.set("Misc", thisApi);
        env.get("package").get("loaded").set("Misc", thisApi);
        return thisApi;
    }

    public static class runCommand extends OneArgFunction{

        @Override
        public LuaValue call(LuaValue arg) {
            var command = arg.checkjstring();
            return NIL;
        }
    }
}
