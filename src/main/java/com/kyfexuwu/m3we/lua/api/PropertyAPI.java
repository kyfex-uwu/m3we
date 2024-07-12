package com.kyfexuwu.m3we.lua.api;

import com.kyfexuwu.m3we.Utils;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.DynamicEnumProperty;
import com.kyfexuwu.m3we.lua.JavaExclusiveTable;
import com.kyfexuwu.m3we.lua.LuaSurfaceObj;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

public class PropertyAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        JavaExclusiveTable thisApi = new JavaExclusiveTable();
        thisApi.javaSet("get",new getPropValue());
        thisApi.javaSet("set",new setProperty());

        return CustomScript.finalizeAPI("Properties",thisApi,env);
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
    static final class setProperty extends VarArgFunction {
        public Varargs invoke(Varargs args) {
            return call(args.arg(1),args.arg(2),args.arg(3),args.arg(4));
        }
        public LuaValue call(LuaValue luaWorld, LuaValue luaPos, LuaValue propName, LuaValue luaSetTo) {
            String propNameStr=propName.checkjstring();

            var setTo=(Object)Utils.toObject(luaSetTo);
            //todo: if the property is an enum, get the enum

            var world=(World)Utils.toObject(luaWorld);
            var pos=(BlockPos) Utils.toObject(luaPos);
            var state = world.getBlockState(pos);
            for(Property<?> prop : state.getProperties()) {
                boolean strToEnum=false;
                if (prop.getName().equals(propNameStr)&&(
                        prop.getValues().contains(setTo)||(strToEnum=(prop.getType().isEnum()&&setTo instanceof String)))) {
                    try {
                        world.setBlockState(pos,processProp(prop, state, setTo, strToEnum));
                        return TRUE;
                    }catch(Exception ignored){}
                }
            }
            return FALSE;
        }
    }
    public static <T extends Comparable<T>> BlockState processProp(Property<T> prop, BlockState state,
                                                                   Object setTo, boolean strToEnum){
        if(strToEnum)
            return state.with(prop, (T) Enum.valueOf((Class<Enum>)(Object)prop.getType(), (String) setTo));
        else if(prop instanceof DynamicEnumProperty dynProp)
            return state.with(prop, (T) dynProp.parse((String) setTo).get());
        else return state.with(prop, (T) setTo);
    }
}
