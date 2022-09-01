package com.kyfexuwu.jsonblocks;

import com.google.gson.JsonElement;
import com.kyfexuwu.jsonblocks.lua.LuaSurfaceObj;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.lang.reflect.Array;
import java.util.function.Function;
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
    public static class PropertyTranslator{
        final String jsonProp;
        final String javaProp;
        final Function<JsonElement,Object> transformFunc;
        PropertyTranslator(String jsonProp,String javaProp, Function<JsonElement,Object> transformFunc){
            this.jsonProp=jsonProp;
            this.javaProp=javaProp;
            this.transformFunc=transformFunc;
        }
    }

    static final Function<JsonElement, Object> BoolTransformFunc = JsonElement::getAsBoolean;
    static final Function<JsonElement, Object> FloatTransformFunc = JsonElement::getAsFloat;
    static final Function<JsonElement, Object> IntTransformFunc = JsonElement::getAsInt;
    static final Function<JsonElement, Object> PredTransformFunc = (JsonElement element) -> {
        return false;
    };

    //--

    public static LuaValue cleanValue(Object value){
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
                cleanedToReturn.set(i, Utils.cleanValue(Array.get(value,i)));
            }
            return cleanedToReturn;
        }

        //not a bool, int, double, float, string, or array
        return new LuaSurfaceObj(value);
    }
}
