package com.kyfexuwu.m3we;

import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.LuaFunctionalInterface;
import com.kyfexuwu.m3we.lua.LuaSurfaceObj;
import com.kyfexuwu.m3we.lua.Translations;
import org.luaj.vm2.*;

import java.lang.reflect.Array;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.luaj.vm2.LuaValue.NIL;

public class Utils {

    public static class Ref<T>{
        public T value;
        public Ref(T value){ this.value=value; }
    }

    public static void forEach(LuaValue table, BiFunction<LuaValue, LuaValue, Boolean> function){
        var key = NIL;
        while (true) {
            Varargs n = table.next(key);
            key = n.arg1();
            if (key.isnil())
                break;

            if (!function.apply(key, n.arg(2))) break;
        }
    }

    public static LuaValue toLuaValue(Object value){
        if(value instanceof LuaValue)
            return (LuaValue)value;

        if(value==null)
            return NIL;
        if(value instanceof Boolean)
            return LuaValue.valueOf((boolean) value);
        if(value instanceof Integer)
            return LuaValue.valueOf((int)value);
        if(value instanceof Double || value instanceof Float)
            return LuaValue.valueOf((double) value);
        if(value instanceof String)
            return LuaValue.valueOf((String) value);

        if(value.getClass().isArray()){
            var length = Array.getLength(value);
            var cleanedToReturn = new LuaTable();
            for(int i=0;i<length;i++){
                cleanedToReturn.set(i, Utils.toLuaValue(Array.get(value,i)));
            }
            return cleanedToReturn;
        }

        if(LuaFunctionalInterface.isFunctionalInterface(value.getClass()))
            return LuaFunctionalInterface.createFunction(value);

        //not a bool, int, double, float, string, or array
        return new LuaSurfaceObj(value);
    }
    public static Object toObject(LuaValue value){
        switch (value.typename()) {
            case "boolean" -> {
                return value.toboolean();
            }
            case "number" -> {
                return value.todouble();
            }
            case "string" -> {
                return value.toString();
            }
            case "table" -> {
                var toReturn = new Object[value.length()];
                for (int i = 0; i < toReturn.length; i++) {
                    toReturn[i] = toObject(value.get(i));
                    //please dont crash please dont crash please dont crash please dont crash please dont crash please d
                }
                return toReturn;
            }
            //case "function": //no: after some hard thinking
            //case "userdata": //no
            //case "thread": //no: pt. 2
            /*case "undecidedFunc":
                return ((UndecidedLuaFunction)value).methods[0];*/
            case LuaSurfaceObj.TYPENAME -> {
                return ((LuaSurfaceObj) value).object;
            }
            default -> {
                return null;
            }
        }
    }
    public static <T> T toObjectTyped(LuaValue value){
        if(value.typename().equals("number")){
            try{ return (T) (Object) value.toint(); } catch(Exception e){}
            try{ return (T) (Object) value.tofloat(); } catch(Exception e){}
            try{ return (T) (Object) value.todouble(); } catch(Exception e){}
            try{ return (T) (Object) value.tolong(); } catch(Exception e){}
        }

        var toReturn = toObject(value);
        try{
            return (T)toReturn;
        }catch(Exception e){ return null; }
    }
    public static LuaTable cloneTable(LuaTable table, LuaTable cloneInto){
        LuaTable finalCloneInto = cloneInto==null?new LuaTable():cloneInto;

        Utils.forEach(table, (key, value)->{
            if(value instanceof LuaTable)
                value = cloneTable(table, null);
            finalCloneInto.set(key.strvalue().checkjstring(), value);
            return true;
        });
        return finalCloneInto;
    }

    public static <T> T tryAndExecute(T dfault, CustomScript scriptContainer, String funcString, Object[] args, Function<LuaValue,T> transformFunc){
        try {
            if (scriptContainer.isFake) return dfault;
            var func = scriptContainer.runEnv.get(funcString);
            if (!func.isfunction())
                return dfault;

            return tryAndExecute(dfault, (LuaFunction) func, args, transformFunc);
        }catch(LuaError e){
            m3we.LOGGER.error("env failure: \n"+e.getMessage());
            return dfault;
        }
    }
    public static <T> T tryAndExecute(T dfault, LuaFunction func, Object[] args, Function<LuaValue,T> transformFunc){
        if(args==null) args=new Object[]{};
        try {
            var luaArgs = new LuaValue[args.length];
            for (int i = 0; i < luaArgs.length; i++) {
                luaArgs[i] = Utils.toLuaValue(args[i]);
            }
            return transformFunc.apply(func.invoke(luaArgs).arg1());
        }catch(LuaError e){
            m3we.LOGGER.error("function failure: \n"+e.getMessage());
            return dfault;
        }
    }
    public static void tryAndExecute(CustomScript scriptContainer, String funcString, Object[] args){
        tryAndExecute(null, scriptContainer, funcString, args, returnValue -> null);
    }
    public static void tryAndExecute(LuaFunction func, Object[] args){
        tryAndExecute(null, func, args, returnValue -> null);
    }

    public static String deobfuscate(String obfuscated){
        if(!Translations.OBFUSCATED || !Translations.obfuscatedPattern.matcher(obfuscated).matches()) return obfuscated;

        var isArray = obfuscated.endsWith("[]");
        int endingNum=Integer.parseInt(obfuscated.substring(obfuscated.indexOf('_')+1,
                obfuscated.length()-(isArray?2:0)));

        var token =  switch (obfuscated.charAt(1)) {
            case 'l' -> //class
                    Translations.classesTranslations[endingNum];
            case 'i' -> //field
                    Translations.fieldsTranslations[endingNum];
            case 'e' -> //method
                    Translations.methodsTranslations[endingNum];
            case 'o' -> //comp
                    Translations.compFieldsTranslations[endingNum];
            default -> null;
        };

        return token!=null ? token.deobfuscated+(isArray?"[]":"") : obfuscated;
    }
}
