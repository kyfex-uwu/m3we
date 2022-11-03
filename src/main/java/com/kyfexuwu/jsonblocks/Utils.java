package com.kyfexuwu.jsonblocks;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.kyfexuwu.jsonblocks.lua.CustomScript;
import com.kyfexuwu.jsonblocks.lua.LuaSurfaceObj;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.jetbrains.annotations.NotNull;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

import static org.luaj.vm2.LuaValue.NIL;

public class Utils {
    public static Pattern validPropertyName = Pattern.compile("[a-z0-9_]+");
    public static Pattern validNamespaceName = Pattern.compile("[a-z0-9_.-]+");
    public static Pattern validPathName = Pattern.compile("[a-z0-9/._-]+");
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
        public PropertyTranslator(String jsonProp,String javaProp, Function<ScriptAndValue,Object> transformFunc){
            this.jsonProp=jsonProp;
            this.javaProp=javaProp;
            this.transformFunc=transformFunc;
        }
    }

    static final Function<ScriptAndValue, Object> BoolTransformFunc = scriptAndValue -> scriptAndValue.value.getAsBoolean();
    static final Function<ScriptAndValue, Object> FloatTransformFunc = scriptAndValue -> scriptAndValue.value.getAsFloat();
    static final Function<ScriptAndValue, Object> IntTransformFunc = scriptAndValue -> scriptAndValue.value.getAsInt();
    static Function<ScriptAndValue, Object> PredTransformFunc(ScriptAndValue SAV, boolean dfault){
        if(SAV.value.getAsString().startsWith("script:")) {
            return scriptAndValue ->(AbstractBlock.ContextPredicate) (state, world, pos) -> tryAndExecute(
                    dfault,
                    SAV.script,
                    SAV.value.getAsString().substring(7),
                    new Object[]{state, world, pos},
                    LuaValue::checkboolean
            );
        }else{
            return scriptAndValue ->(AbstractBlock.ContextPredicate) (state, world, pos) -> SAV.value.getAsBoolean();
        }
    }

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
        try {
            if (scriptContainer.isFake) return dfault;
            var func = scriptContainer.runEnv.get(funcString);
            if (func.isnil())
                return dfault;

            var luaArgs = new LuaValue[args.length];
            for (int i = 0; i < luaArgs.length; i++) {
                luaArgs[i] = Utils.toLuaValue(args[i]);
            }

            return transformFunc.apply(func.invoke(luaArgs).arg1());
        }catch(LuaError e){
            System.out.println(e.getMessage());
            return dfault;
        }
    }
    public static void tryAndExecute(CustomScript scriptContainer, String funcString,Object[] args){
        tryAndExecute(null,scriptContainer, funcString, args, returnValue -> null);
        //calls tryAndExecute, but always returns null
    }
}
