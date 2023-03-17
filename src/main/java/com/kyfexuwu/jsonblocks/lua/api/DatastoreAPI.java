package com.kyfexuwu.jsonblocks.lua.api;

import com.kyfexuwu.jsonblocks.lua.CustomScript;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

public class DatastoreAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {

        env.set("Datastore", CustomScript.dataStore);
        env.get("package").get("loaded").set("Datastore", CustomScript.dataStore);
        return CustomScript.dataStore;
    }


}
