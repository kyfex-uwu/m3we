package com.kyfexuwu.jsonblocks;

import com.google.gson.JsonElement;

import java.util.function.Function;

public class Utils {
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
}
