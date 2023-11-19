package com.kyfexuwu.m3we;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.LuaSurfaceObj;
import com.kyfexuwu.m3we.lua.Translations;
import net.minecraft.block.AbstractBlock;
import net.minecraft.util.Identifier;
import org.luaj.vm2.*;

import java.lang.reflect.Array;
import java.util.function.BiFunction;
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
    public static class SuccessAndIdentifier{
        public final SuccessRate successRate;
        public final Identifier identifier;

        public SuccessAndIdentifier(SuccessRate successRate, Identifier identifier){
            this.successRate=successRate;
            this.identifier=identifier;
        }
        public SuccessAndIdentifier(SuccessRate successRate){
            this.successRate=successRate;
            this.identifier=null;
        }
    }

    public static class ScriptAndValue{
        final CustomScript script;
        final JsonElement value;

        public ScriptAndValue(CustomScript script, JsonElement value){
            this.script=script;
            this.value=value;
        }
    }

    public static class PropertyTranslator<T, Settings> {
        final String jsonProp;
        final BiFunction<Settings, T, Settings> toJavaFunc;
        final Function<ScriptAndValue, T> transformFunc;
        public PropertyTranslator(String jsonProp,
                                  BiFunction<Settings, T, Settings> toJavaFunc,
                                  Function<ScriptAndValue,T> transformFunc){
            this.jsonProp=jsonProp;
            this.toJavaFunc=toJavaFunc;
            this.transformFunc=transformFunc;
        }
        public Settings apply(Settings settings, JsonObject value, CustomScript script){
            try {
                return this.toJavaFunc.apply(settings, this.transformFunc.apply(new ScriptAndValue(script, value.get(this.jsonProp))));
            }catch(UnsupportedOperationException e) {
                System.out.println("Property " + this.jsonProp + " failed to load, check your json!");
                e.printStackTrace();
                return settings;
            }
        }
    }

    static final Function<ScriptAndValue, Boolean> BoolTransformFunc = scriptAndValue -> scriptAndValue.value.getAsBoolean();
    static final Function<ScriptAndValue, Float> FloatTransformFunc = scriptAndValue -> scriptAndValue.value.getAsFloat();
    static final Function<ScriptAndValue, Integer> IntTransformFunc = scriptAndValue -> scriptAndValue.value.getAsInt();
    static AbstractBlock.ContextPredicate PredTransformFunc(ScriptAndValue SAV, boolean dfault){ //this one works differently
        if(SAV.value.getAsString().startsWith("script:")) {
            return (state, world, pos) -> tryAndExecute(
                    dfault,
                    SAV.script,
                    SAV.value.getAsString().substring(7),
                    new Object[]{state, world, pos},
                    LuaValue::checkboolean
            );
        }else{
            return (state, world, pos) -> SAV.value.getAsBoolean();
        }
    }

    //--

    public static void forEach(LuaTable table, BiFunction<LuaValue, LuaValue, Boolean> function){
        var key = LuaValue.NIL;
        while(true){
            Varargs n = table.next(key);
            key = n.arg1();
            if (key.isnil())
                break;

            if(function.apply(key, n.arg(2))==false) break;
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

    public static String deobfuscate(String obfuscated){
        if(!Translations.OBFUSCATED || !Translations.obfuscatedPattern.matcher(obfuscated).matches()) return obfuscated;

        int endingNum=Integer.parseInt(obfuscated.substring(obfuscated.indexOf('_')+1));

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

        return token!=null ? token.deobfuscated : obfuscated;
    }
}
