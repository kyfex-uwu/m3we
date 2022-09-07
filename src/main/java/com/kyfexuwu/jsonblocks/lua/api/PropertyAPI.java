package com.kyfexuwu.jsonblocks.lua.api;

import com.kyfexuwu.jsonblocks.Utils;
import com.kyfexuwu.jsonblocks.lua.CustomBlock;
import com.kyfexuwu.jsonblocks.lua.LuaSurfaceObj;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.Collection;

public class PropertyAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable thisApi = new LuaTable();
        thisApi.set("get",new getProperty());

        env.set("Properties", thisApi);
        env.get("package").get("loaded").set("Properties", thisApi);
        return thisApi;
    }

    static final class getProperty extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue state, LuaValue propName) {
            String propNameStr=propName.checkjstring();
            if(!(state instanceof LuaSurfaceObj))
                return NIL;

            var props=((BlockState)((LuaSurfaceObj)state).object).getProperties();
            for(Property prop : props) {
                if (prop.getName().equals(propNameStr))
                    return Utils.toLuaValue(prop);
            }
            return NIL;
        }
    }
}
