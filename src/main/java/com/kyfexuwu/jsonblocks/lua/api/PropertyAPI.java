package com.kyfexuwu.jsonblocks.lua.api;

import com.kyfexuwu.jsonblocks.Utils;
import com.kyfexuwu.jsonblocks.lua.LuaSurfaceObj;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

public class PropertyAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        APITable thisApi = new APITable();
        thisApi.set("get",new getPropValue());
        thisApi.set("set",new setProperty());

        thisApi.locked=true;
        env.set("Properties", thisApi);
        env.get("package").get("loaded").set("Properties", thisApi);
        return thisApi;
    }

    static final class getPropValue extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue luaState, LuaValue propName) {
            String propNameStr=propName.checkjstring();

            var state = (BlockState)((LuaSurfaceObj)luaState).object;
            var props=state.getProperties();
            for(Property<?> prop : props) {
                if (prop.getName().equals(propNameStr))
                    return Utils.toLuaValue(state.get(prop));
            }
            return NIL;
        }
    }
    static final class setProperty extends ThreeArgFunction {
        @Override
        public LuaValue call(LuaValue worldPosState, LuaValue propName, LuaValue luaSetTo) {
            String propNameStr=propName.checkjstring();

            var setTo=(Object)Utils.toObject(luaSetTo);
            //todo: if the property is an enum, get the enum

            var world=(ServerWorld)((LuaSurfaceObj)worldPosState.get(1)).object;
            var pos=(BlockPos)((LuaSurfaceObj)worldPosState.get(2)).object;
            var state=(BlockState)((LuaSurfaceObj)worldPosState.get(3)).object;
            var props=state.getProperties();
            for(Property prop : props) {
                if (prop.getName().equals(propNameStr)&&prop.getValues().contains(setTo)){
                    if(setTo instanceof Double)
                        world.setBlockState(pos,state.with(prop,((Double) setTo).intValue()));
                    if(setTo instanceof Boolean)
                        world.setBlockState(pos,state.with(prop,(Boolean) setTo));
                    if(setTo instanceof String)
                        world.setBlockState(pos,state.with(prop,(String) setTo));
                    return NIL;
                }
            }
            return NIL;
        }
    }
}
