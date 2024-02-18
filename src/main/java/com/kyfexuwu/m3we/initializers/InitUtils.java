package com.kyfexuwu.m3we.initializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.m3we;
import net.minecraft.block.AbstractBlock;
import net.minecraft.util.Identifier;
import org.luaj.vm2.LuaValue;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.kyfexuwu.m3we.Utils.tryAndExecute;

public class InitUtils {
    static <T extends JsonElement> T getOr(JsonObject obj, String key, T dfault){
        var toReturn = obj.get(key);
        return toReturn==null?dfault:(T) toReturn;
    }

    public static Pattern validPropertyName = Pattern.compile("[a-z0-9_]+");
    public static Pattern validNamespaceName = Pattern.compile("[a-z0-9_.-]+");
    public static Pattern validPathName = Pattern.compile("[a-z0-9/._-]+");

    public enum SuccessRate {
        CANT_READ,
        BAD_JSON,
        IDK,
        YOU_DID_IT,
        COME_BACK_LATER
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

    public static class PropertyTranslator<T, Settings, SettingsHolder> {
        final String jsonProp;
        final BiFunction<Settings, T, Settings> toJavaFunc;
        final Function<ScriptAndValue, T> transformFunc;
        final Function<SettingsHolder, T> getDefaultFunc;
        public PropertyTranslator(String jsonProp,
                                  BiFunction<Settings, T, Settings> toJavaFunc,
                                  Function<ScriptAndValue,T> transformFunc,
                                  Function<SettingsHolder, T> getDefaultFunc){
            this.jsonProp=jsonProp;
            this.toJavaFunc=toJavaFunc;
            this.transformFunc=transformFunc;
            this.getDefaultFunc=getDefaultFunc;
        }
        public Settings apply(Settings settings, JsonObject value, CustomScript script){
            try {
                return this.apply(settings, this.transformFunc.apply(new ScriptAndValue(script, value.get(this.jsonProp))));
            }catch(Exception e) {
                e.printStackTrace();
                m3we.LOGGER.error("Property " + this.jsonProp + " failed to load, check your json!");
                return settings;
            }
        }
        public Settings applyDefault(Settings settings, SettingsHolder toSetFrom){
            return this.apply(settings, this.getDefaultFunc.apply(toSetFrom));
        }
        public Settings apply(Settings settings, T value){
            return this.toJavaFunc.apply(settings, value);
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
}
