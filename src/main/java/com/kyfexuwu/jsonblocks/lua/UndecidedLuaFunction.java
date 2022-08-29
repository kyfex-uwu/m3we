package com.kyfexuwu.jsonblocks.lua;

import com.kyfexuwu.jsonblocks.Utils;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

import java.lang.reflect.Method;

public class UndecidedLuaFunction extends VarArgFunction {
    public Object thisObj;
    public Method[] methods;
    public UndecidedLuaFunction(Object thisObj,Method[] methods){
        this.thisObj=thisObj;
        this.methods=methods;
    }

    public LuaValue call(){ return figureAndCall(); }
    public LuaValue call(LuaValue arg){ return figureAndCall(arg); }
    public LuaValue call(LuaValue arg1, LuaValue arg2){ return figureAndCall(arg1,arg2); }
    public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3){ return figureAndCall(arg1,arg2,arg3); }
    public Varargs invoke(Varargs varargs) {
        //idk if this is inefficient lol
        LuaValue[] values = new LuaValue[varargs.narg()];
        for(int i=0;i<values.length;i++)
            values[i]=varargs.arg(i);
        return figureAndCall().invoke(values);
    }

    public LuaValue figureAndCall(LuaValue... args) {
        for(Method method: methods){
            var paramTypes = method.getParameterTypes();
            var objAndParams=new Object[args.length+1];
            boolean matches=true;
            for(int i=0;i<args.length;i++){
                if(!paramTypes[i].isAssignableFrom(Utils.cleanValue(args[0]).getClass())){
                    matches=false;
                    break;
                }
                objAndParams[i+1]=args[i];
            }
            if(matches){
                try {
                    //try to understand this, i dare you
                    objAndParams[0]=thisObj;
                    var toReturn = method.getClass()
                            .getMethod("invoke",Object.class,Object[].class)
                            .invoke(method,objAndParams);

                    return Utils.cleanValue(toReturn);
                }catch(Exception ignored){}
            }
        }
        return NIL;
    }
}