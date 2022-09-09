package com.kyfexuwu.jsonblocks.lua;

import com.kyfexuwu.jsonblocks.Utils;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

import java.lang.reflect.Method;
import java.util.Arrays;

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
            int[] numParamsIndexes=new int[args.length];
            boolean matches=true;
            for(int i=0;i<args.length;i++){
                //null case
                if(translatedArgs[i]==null)
                    continue;
                //boolean case
                if(translatedArgs[i].getClass().equals(Boolean.class)&&paramTypes[i].equals(boolean.class))
                    continue;
                //number cases
                if(translatedArgs[i].getClass().equals(Double.class)){
                    if(paramTypes[i].equals(int.class)){
                        translatedArgs[i] = (int) translatedArgs[i];
                        continue;
                    }
                    if(paramTypes[i].equals(float.class)){
                        translatedArgs[i] = (float) translatedArgs[i];
                        continue;
                    }
                    if(paramTypes[i].equals(double.class)){
                        translatedArgs[i] = (double) translatedArgs[i];
                        continue;
                    }
                    //else
                    numParamsIndexes[numParamsAmt]=i;
                    numParamsAmt++;
                }

                //regular case
                if(!paramTypes[i].isAssignableFrom(translatedArgs[i].getClass())){
                    matches=false;
                    break;
                }
            }
            if(matches){
                int amtOfTries = (int)Math.pow(3,numParamsAmt);
                for(int i=0;i<amtOfTries;i++) {
                    var changeableArgs = Arrays.copyOf(translatedArgs,translatedArgs.length);
                    //number type brute forcing
                    for(int j=0;j<numParamsAmt;j++){
                        switch (i / (int) Math.pow(3, j) % 3) {
                            case 0 -> changeableArgs[numParamsIndexes[j]] = ((Double) changeableArgs[numParamsIndexes[j]]).doubleValue();
                            case 1 -> changeableArgs[numParamsIndexes[j]] = ((Double) changeableArgs[numParamsIndexes[j]]).floatValue();
                            case 2 -> changeableArgs[numParamsIndexes[j]] = ((Double) changeableArgs[numParamsIndexes[j]]).intValue();
                        }
                    }

                    try {
                        return Utils.toLuaValue(
                                method.getClass().getMethod("invoke", Object.class, Object[].class)
                                        .invoke(method, thisObj, changeableArgs)
                        );
                    } catch (Exception ignored) {}
                }
            }
        }
        return NIL;
    }
}