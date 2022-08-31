package com.kyfexuwu.jsonblocks.lua;

import com.kyfexuwu.jsonblocks.Utils;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue;
import org.luaj.vm2.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;

public class LuaSurfaceObj extends LuaTable {
    public Object object;
    public LuaSurfaceObj(Object object){
        this.object=object;
    }

    //todo: maybe include pairs() support?

    @Override
    public LuaValue get(LuaValue key){
        try {
            return Utils.cleanValue(object.getClass().getField(key.toString()).get(object));
        }catch(NoSuchFieldException e){
            LinkedList<Method> methods = new LinkedList<>();
            for(Method method : object.getClass().getMethods()){
                if(method.getName().equals(key.toString()))
                    methods.add(method);
            }
            if(methods.size()==0)
                return NIL;
            return new UndecidedLuaFunction(object,methods.toArray(new Method[0]));
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
