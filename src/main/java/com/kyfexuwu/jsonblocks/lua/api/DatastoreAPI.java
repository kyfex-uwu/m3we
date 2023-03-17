package com.kyfexuwu.jsonblocks.lua.api;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

public class DatastoreAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable thisApi = new LuaTable();

        env.set("Datastore", thisApi);
        env.get("package").get("loaded").set("Datastore", thisApi);
        return thisApi;
    }


}
