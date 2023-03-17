package com.kyfexuwu.jsonblocks.lua.api;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

public class APITable extends LuaTable {
    public boolean locked=false;

    @Override
    public void rawset(LuaValue key, LuaValue value) {
        if(locked) return;
        super.rawset(key, value);
    }
}
