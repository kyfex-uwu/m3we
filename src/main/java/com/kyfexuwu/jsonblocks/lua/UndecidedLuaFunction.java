package com.kyfexuwu.jsonblocks.lua;

import com.kyfexuwu.jsonblocks.Utils;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

import javax.lang.model.type.PrimitiveType;
import java.lang.reflect.Method;

public class UndecidedLuaFunction extends VarArgFunction {
    public Object thisObj;
    public Method[] methods;
    public UndecidedLuaFunction(Object thisObj,Method[] methods){
        this.thisObj=thisObj;
        this.methods=methods;
    }

    public String typename(){
        return "undecidedFunc";
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

        return figureAndCall(values);
    }

    public LuaValue figureAndCall(LuaValue... args) {
        var paramsObj = new Object[args.length];
        for(int i=0;i<args.length;i++){
            paramsObj[i]=Utils.toObject(args[i]);
        }
        for(Method method: methods){
            var paramTypes = method.getParameterTypes();
            if(paramTypes.length!=args.length)
                continue;

            boolean matches=true;
            for(int i=0;i<args.length;i++){
                System.out.println(paramTypes[i].getName()+" vs "+paramsObj[i].getClass().getName()+": "+
                        paramTypes[i].isAssignableFrom(paramsObj[i].getClass()));
                System.out.println("last resort "+boolean.class.getName());

                if(paramsObj[i].getClass().equals(double.class)&&(
                        Number.class.isAssignableFrom(paramTypes[i])||
                        paramTypes[i].equals(int.class)||
                        paramTypes[i].equals(float.class)||
                        paramTypes[i].equals(double.class)
                ))
                    continue;
                //todo: fix
                if(!paramTypes[i].isAssignableFrom(paramsObj[i].getClass())){
                    matches=false;
                    break;
                }
            }
            if(matches){
                try {
                    var toReturn = method.getClass()
                            .getMethod("invoke",Object.class,Object[].class)
                            .invoke(method, thisObj, paramsObj);

                    return Utils.toLuaValue(toReturn);
                }catch(Exception ignored){}
            }
        }
        return NIL;
    }
}