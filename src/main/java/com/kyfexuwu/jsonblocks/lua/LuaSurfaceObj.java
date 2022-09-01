package com.kyfexuwu.jsonblocks.lua;

import com.kyfexuwu.jsonblocks.Utils;
import org.luaj.vm2.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;

public class LuaSurfaceObj extends LuaTable {
    public Object object;
    private final String[] valueNames;
    public LuaSurfaceObj(Object object){
        this.object=object;
        Field[] fields = object.getClass().getFields();
        Method[] methods = object.getClass().getMethods();

        this.valueNames = new String[fields.length+ methods.length];
        int currIndex=0;
        for(Field field : fields){
            this.valueNames[currIndex]=field.getName();
            currIndex++;
        }
        for(Method method : methods){
            var tempName = method.getName();
            for(int i = 0; i< fields.length; i++){
                if(tempName.equals(this.valueNames[i])){
                    tempName="func_"+tempName;//please no more than this
                    break;
                }
            }
            this.valueNames[currIndex]=tempName;
            currIndex++;
        }
    }

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

    @Override //now works with pairs :sugnlasses:
    public Varargs next( LuaValue keyAsValue ) {
        if(keyAsValue.isnil()) {
            return LuaValue.valueOf(this.valueNames[0]);
        }
        String key = keyAsValue.toString();
        for(int i=0;i<this.valueNames.length-1;i++){
            if(key.equals(this.valueNames[i])&&!key.equals("wait")){
                return LuaValue.valueOf(this.valueNames[i+1]);
            }
        }

        // nothing found, return nil.
        return NIL;
    }

    @Override
    public void set(LuaValue key, LuaValue value){}
    @Override
    public void rawset(LuaValue key, LuaValue value){}
}
