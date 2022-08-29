package com.kyfexuwu.jsonblocks.lua;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

public class BlockApi extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable blockApi = new LuaTable();
        blockApi.set("getLocation",new testAPI.getLocation());

        env.set("Block", blockApi);
        return blockApi;
    }

    static final class getLocation extends OneArgFunction {
        @Override
        public LuaValue call(LuaValue arg) {
            return NIL;
        }
    }

    public BlockApi(){}
}
