package com.kyfexuwu.jsonblocks.lua;

import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class testAPI extends TwoArgFunction {
    public testAPI(){}

    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable apiToReturn = new LuaTable();
        apiToReturn.set("getLocation",new getLocation());

        env.set("testAPI", apiToReturn);
        env.get("package").get("loaded").set("testAPI", apiToReturn);
        return apiToReturn;
    }

    static final class getLocation extends ZeroArgFunction {
        @Override
        public LuaValue call() {
            return LuaString.valueOf("not implemented yet :(");
        }
    }
}
