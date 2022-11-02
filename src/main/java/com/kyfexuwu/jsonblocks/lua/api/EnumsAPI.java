package com.kyfexuwu.jsonblocks.lua.api;

import com.kyfexuwu.jsonblocks.Utils;
import net.minecraft.util.Hand;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

public class EnumsAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable thisApi = new LuaTable();

        applyEnum(Hand.values(),thisApi,"Hand");

        env.set("Enums", thisApi);
        env.get("package").get("loaded").set("Properties", thisApi);
        return thisApi;
    }

    private <T extends Enum<T>> void applyEnum(T[] enumValues, LuaTable APITable, String name){
        LuaTable enumTable = new LuaTable();
        for(T value : enumValues){
            enumTable.set(value.name(),Utils.toLuaValue(value));
        }
        APITable.set(name,enumTable);
    }
}
