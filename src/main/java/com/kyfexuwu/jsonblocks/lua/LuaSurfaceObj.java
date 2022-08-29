package com.kyfexuwu.jsonblocks.lua;

import com.kyfexuwu.jsonblocks.Utils;
import org.luaj.vm2.*;

public class LuaSurfaceObj extends LuaTable {
    public Object object;
    public LuaSurfaceObj(Object object){
        this.object=object;
    }

    @Override
    public LuaValue get(LuaValue key){
        try {
            return Utils.cleanValue(object.getClass().getField(key.toString()).get(object));
        }catch(NoSuchFieldException e){
            return new UndecidedLuaFunction(object,object.getClass().getMethods());
        }catch(Exception ignored){}

        //not a field or a method? L bozo
        return NIL;
    }
    @Override
    public LuaValue rawget(int key){ return get(LuaValue.valueOf(key)); }
    @Override
    public LuaValue rawget(LuaValue key){ return get(key); }

    @Override
    public void set(LuaValue key, LuaValue value){}
    @Override
    public void rawset(LuaValue key, LuaValue value){}
}
