package com.kyfexuwu.m3we.lua.api;

import com.kyfexuwu.m3we.Utils;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.DynamicEnumProperty;
import com.kyfexuwu.m3we.lua.JavaExclusiveTable;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class StdLibApi extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        JavaExclusiveTable thisApi = new JavaExclusiveTable();

//        thisApi.javaSet("validateState",new runCommand(env));

        //stair
        //slab
        //

        return CustomScript.finalizeAPI("StdLib",thisApi,env);
    }

    abstract static class PropData{
        public final String name;
        public PropData(String name){ this.name=name; }
    }
    static class IntPropData extends PropData{
        public final int min;
        public final int max;
        public IntPropData(String name, int min, int max){
            super(name);
            this.min=min;
            this.max=max;
        }
    }
    static class BoolPropData extends PropData{
        public BoolPropData(String name) {
            super(name);
        }
    }
    static class DynEnumPropData extends PropData{
        public final Collection<String> values;
        public DynEnumPropData(String name, Collection<String> values) {
            super(name);
            this.values=values;
        }
    }

    private static String[] validateState(BlockState state, List<PropData> required){
        var props = state.getProperties();
        var missing = new ArrayList<PropData>();
        for(var prop : props){
            var found = required.stream().filter(data -> {
                if(!data.name.equals(prop.getName())) return false;
                if(prop instanceof BooleanProperty) return true;

                return false;
            }).findFirst();
            if(found.isEmpty()){
                if(prop instanceof BooleanProperty) missing.add(new BoolPropData(prop.getName()));
                if(prop instanceof IntProperty intProp) missing.add(new IntPropData(prop.getName(), intProp.field_37655, intProp.field_37656));//min, max
                if(prop instanceof DynamicEnumProperty dynEnProp) missing.add(new DynEnumPropData(prop.getName(), dynEnProp.getValues()));
            }
        }
        return needed.toArray(new String[0]);
    }
    public static class validateState extends TwoArgFunction{
        private final LuaValue globals;
        public validateState(LuaValue globals){
            super();
            this.globals=globals;
        }

        @Override
        public LuaValue call(LuaValue blockState, LuaValue propDatas) {
            var data = new ArrayList<PropData>();

            validateState(Utils.toObjectTyped(blockState))
        }
    }
}
