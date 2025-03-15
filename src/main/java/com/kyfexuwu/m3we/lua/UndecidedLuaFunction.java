package com.kyfexuwu.m3we.lua;

import com.kyfexuwu.m3we.Utils;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import oshi.util.tuples.Pair;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class UndecidedLuaFunction extends VarArgFunction {
    public Object thisObj;
    public Executable[] methods;
    public UndecidedLuaFunction(Object thisObj, Executable[] methods){
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
        for(Executable method : this.methods){
            var paramTypes = method.getParameterTypes();
            if(paramTypes.length!=args.length)
                continue;

            int numParamsAmt=0;//for later, so we can brute force number types
            int[] numParamsIndexes=new int[args.length];
            boolean matches=true;
            for(int i=0;i<args.length;i++){
                //functional interface case
                if(LuaFunctionalInterface.isFunctionalInterface(paramTypes[i])) {
                    if(!(args[i] instanceof LuaFunction)){
                        matches=false;
                        break;
                    }
                    translatedArgs[i] = LuaFunctionalInterface.createFunctionalInterface((LuaFunction) args[i], paramTypes[i]);
                    continue;
                }
                //null case
                if(translatedArgs[i]==null)
                    continue;
                //boolean case
                if(translatedArgs[i] instanceof Boolean&&paramTypes[i].equals(boolean.class))
                    continue;
                //enum case
                if(translatedArgs[i] instanceof String&&Enum.class.isAssignableFrom(paramTypes[i])){
                    var enums = paramTypes[i].getEnumConstants();
                    for(var val : enums){
                        if(((Enum<?>)val).name().toLowerCase(Locale.ROOT).equals(
                                ((String) translatedArgs[i]).toLowerCase(Locale.ROOT))){
                            translatedArgs[i]=val;
                            break;
                        }
                    }
                    continue;
                }
                //char case
                if(translatedArgs[i] instanceof String&&paramTypes[i].equals(char.class)&&
                        ((String)translatedArgs[i]).length()==1) {
                    translatedArgs[i] = ((String)translatedArgs[i]).charAt(0);
                    continue;
                }
                //number cases
                if(translatedArgs[i] instanceof Double){
                    if(paramTypes[i].equals(int.class)){
                        translatedArgs[i] = ((Double)translatedArgs[i]).intValue();
                        continue;
                    }
                    if(paramTypes[i].equals(float.class)){
                        translatedArgs[i] = ((Double)translatedArgs[i]).floatValue();
                        continue;
                    }
                    if(paramTypes[i].equals(double.class)){
                        translatedArgs[i] = (Double) translatedArgs[i];
                        continue;
                    }
                    if(paramTypes[i].equals(long.class)){
                        translatedArgs[i] = ((Double)translatedArgs[i]).longValue();
                        continue;
                    }
                    if(paramTypes[i].equals(short.class)){
                        translatedArgs[i] = ((Double)translatedArgs[i]).shortValue();
                        continue;
                    }
                    if(paramTypes[i].equals(byte.class)){
                        translatedArgs[i] = ((Double)translatedArgs[i]).byteValue();
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
                int amtOfTries = (int)Math.pow(6,numParamsAmt);
                for(int i=0;i<amtOfTries;i++) {
                    var changeableArgs = Arrays.copyOf(translatedArgs,translatedArgs.length);
                    //number type brute forcing
                    for(int j=0;j<numParamsAmt;j++){
                        switch (i / (int) Math.pow(6, j) % 6) {
                            case 0 -> changeableArgs[numParamsIndexes[j]] = ((Double) changeableArgs[numParamsIndexes[j]]).doubleValue();
                            case 1 -> changeableArgs[numParamsIndexes[j]] = ((Double) changeableArgs[numParamsIndexes[j]]).floatValue();
                            case 2 -> changeableArgs[numParamsIndexes[j]] = ((Double) changeableArgs[numParamsIndexes[j]]).intValue();
                            case 3 -> changeableArgs[numParamsIndexes[j]] = ((Double) changeableArgs[numParamsIndexes[j]]).longValue();
                            case 4 -> changeableArgs[numParamsIndexes[j]] = ((Double) changeableArgs[numParamsIndexes[j]]).byteValue();
                            case 5 -> changeableArgs[numParamsIndexes[j]] = ((Double) changeableArgs[numParamsIndexes[j]]).shortValue();
                        }
                    }

                    try {
                        if(method instanceof Method)
                            return Utils.toLuaValue(((Method)method).invoke(this.thisObj, changeableArgs));
                        else
                            return Utils.toLuaValue(((Constructor<?>)method).newInstance(changeableArgs));
                    } catch (Exception ignored) { }
                }
            }
        }

        throw new LuaError("Function \""+this.funcName()+"\" called with incorrect arguments");
    }

    public String funcName(){
        return Utils.deobfuscate(this.methods[0].getName());
    }

    public enum MethodType{ METHOD, CONSTRUCTOR }
    public record MethodDescriptor(List<Pair<String,String>> params, String returnClass, MethodType type){}
    public String toString(){
        return "java function "+this.methods[0].getName();
    }
    public static final String CONSTRUCTOR_ID = "constructor";

    /**
     * MethodDescriptor.params is a list of pairs, where those pairs are name then class.<br>
     * Classes are turned into strings through .getSimpleName()
     * @return
     */
    public List<MethodDescriptor> methodDescribers(){
        var toReturn = new ArrayList<MethodDescriptor>();
        var refMethods = this.methods;

        for(Executable m : refMethods) {
            var token = m instanceof Method?
                    Translations.getToken((Method)m):
                    new Translations.MethodToken("<init>","<init>","type",
                            new String[]{"p1","p2","p3"});

            var params = new ArrayList<Pair<String,String>>();
            for (int i=0;i<token.paramNames.length;i++) {
                params.add(new Pair<>(token.paramNames[i], token.paramClasses[i]));
            }

            var returnClass = Utils.deobfuscate(this.thisObj.getClass().getSimpleName());
            if(m instanceof Method method) returnClass = Utils.deobfuscate(method.getReturnType().getSimpleName());
            toReturn.add(new MethodDescriptor(params, returnClass,
                    m instanceof Method ? MethodType.METHOD : MethodType.CONSTRUCTOR));
        }

        return toReturn;
    }
}