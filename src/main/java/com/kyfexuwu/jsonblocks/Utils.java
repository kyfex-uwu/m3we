package com.kyfexuwu.jsonblocks;

import com.google.gson.JsonElement;
import com.kyfexuwu.jsonblocks.lua.CustomScript;
import com.kyfexuwu.jsonblocks.lua.LuaSurfaceObj;
import com.kyfexuwu.jsonblocks.lua.UndecidedLuaFunction;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import org.luaj.vm2.Lua;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.lang.reflect.Array;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import static org.luaj.vm2.LuaValue.NIL;

public class Utils {
    public static Pattern validName = Pattern.compile("[a-z0-9_]+");
    public enum SuccessRate {
        CANT_READ,
        BAD_JSON,
        IDK,
        YOU_DID_IT
    }

    public static class ScriptAndValue{
        final CustomScript script;
        final JsonElement value;

        public ScriptAndValue(CustomScript script, JsonElement value){
            this.script=script;
            this.value=value;
        }
    }

    public static class PropertyTranslator {
        final String jsonProp;
        final String javaProp;
        final Function<ScriptAndValue, Object> transformFunc;
        final CustomScript scriptContainer;
        public PropertyTranslator(String jsonProp,String javaProp, Function<ScriptAndValue,Object> transformFunc){
            this.jsonProp=jsonProp;
            this.javaProp=javaProp;
            this.transformFunc=transformFunc;
            this.scriptContainer=null;
        }
        public PropertyTranslator(String jsonProp,String javaProp, CustomScript scriptContainer, Function<ScriptAndValue,Object> transformFunc){
            this.jsonProp=jsonProp;
            this.javaProp=javaProp;
            this.transformFunc=transformFunc;
            this.scriptContainer=scriptContainer;
        }
    }

    static final Function<ScriptAndValue, Object> BoolTransformFunc = scriptAndValue -> scriptAndValue.value.getAsBoolean();
    static final Function<ScriptAndValue, Object> FloatTransformFunc = scriptAndValue -> scriptAndValue.value.getAsFloat();
    static final Function<ScriptAndValue, Object> IntTransformFunc = scriptAndValue -> scriptAndValue.value.getAsInt();

    //--

    public static LuaValue toLuaValue(Object value){

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

        //not a bool, int, double, float, string, or array
        return new LuaSurfaceObj(value);
    }
    public static Object toObject(LuaValue value){
        switch(value.typename()){
            case "boolean":
                return value.toboolean();
            case "number":
                return value.todouble();
            case "string":
                return value.toString();
            case "table":
                var toReturn = new Object[value.length()];
                for(int i=0;i<toReturn.length;i++){
                    toReturn[i]=toObject(value.get(i));
                    //please dont crash please dont crash please dont crash please dont crash please dont crash please d
                }
                return toReturn;
            //case "function": //todo :blensive:
            //case "userdata": //no
            //case "thread": //no: pt. 2
            /*case "undecidedFunc":
                return ((UndecidedLuaFunction)value).methods[0];*/
            case "surfaceObj":
                return ((LuaSurfaceObj)value).object;

            case "nil":
            default:
                return null;
        }
    }

    public static <T> T tryAndExecute(T dfault, CustomScript scriptContainer, String funcString, Object[] args, Function<LuaValue,T> transformFunc){
        if(scriptContainer==null) return dfault;
        var func = scriptContainer.runEnv.get(funcString);
        if(func.isnil())
            return dfault;

        var luaArgs=new LuaValue[args.length];
        for(int i=0;i<luaArgs.length;i++){
            luaArgs[i]=Utils.toLuaValue(args[i]);
        }

        return transformFunc.apply(func.invoke(luaArgs).arg1());
    }
    public static void tryAndExecute(CustomScript scriptContainer, String funcString,Object[] args){
        tryAndExecute(null,scriptContainer, funcString, args, returnValue -> null);
        //calls tryAndExecute, but always returns null
    }
}
