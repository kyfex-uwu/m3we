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
            values[i]=varargs.arg(i+1);//i dont swear, but fr*ck lua

        return figureAndCall(values);
    }

    public LuaValue figureAndCall(LuaValue... args) {
        var translatedArgs=new Object[args.length];
        for(int i=0;i<args.length;i++){
            translatedArgs[i]=Utils.toObject(args[i]);
        }
        for(Method method: methods){
            var paramTypes = method.getParameterTypes();
            if(paramTypes.length!=args.length)
                continue;

            int numParamsAmt=0;//for later, so we can brute force number types
            boolean matches=true;
            for(int i=0;i<args.length;i++){
                //null case
                if(translatedArgs[i]==null) {
                    continue;
                }
                //boolean case
                if(translatedArgs[i].getClass().equals(Boolean.class)&&paramTypes[i].equals(boolean.class))
                    continue;

                //regular case
                if(!paramTypes[i].isAssignableFrom(translatedArgs[i].getClass())){
                    matches=false;
                    break;
                }
            }
            if(matches){
                int amtOfTries = (int)Math.pow(2,numParamsAmt);
                for(int i=0;i<amtOfTries;i++) {
                    /*
                    //number cases
                    if(translatedArgs[i].getClass().equals(Double.class)){
                        if(paramTypes[i].equals(int.class))
                            translatedArgs[i] = ((Double)translatedArgs[i]).intValue();
                        if(paramTypes[i].equals(float.class))
                            translatedArgs[i] = ((Double)translatedArgs[i]).floatValue();
                        continue;
                    }

                     */

                    try {
                        return Utils.toLuaValue(
                                method.getClass().getMethod("invoke", Object.class, Object[].class)
                                        .invoke(method, thisObj, translatedArgs)
                        );
                    } catch (Exception e) {
                        System.out.println("incorrect number type, trying again... (or something went horribly wrong)");
                    }
                }
            }
        }
        return NIL;
    }
}