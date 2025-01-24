package com.kyfexuwu.m3we.initializers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.kyfexuwu.m3we.Json;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.m3we;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.registry.Registry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.kyfexuwu.m3we.initializers.InitUtils.*;

public class ItemIniter {

    private static class ItemPropertyTranslator<T> extends InitUtils.PropertyTranslator<T, Item.Settings, Item> {
        public ItemPropertyTranslator(String jsonProp,
                                      BiFunction<Item.Settings, T, Item.Settings> toJavaFunc,
                                      Function<InitUtils.ScriptAndValue, T> transformFunc,
                                      Function<Item, T> getDefaultFunc) {
            super(jsonProp, toJavaFunc, transformFunc, getDefaultFunc);
        }
    }

    private static final ItemPropertyTranslator<?>[] itemPropertyMap = {
            new ItemPropertyTranslator<>("maxCount",Item.Settings::maxCount,IntTransformFunc,
                    Item::getMaxCount),
            new ItemPropertyTranslator<>("maxDamage",Item.Settings::maxDamage,IntTransformFunc,
                    Item::getMaxDamage),
            new ItemPropertyTranslator<>("recipeRemainder",Item.Settings::recipeRemainder,
                    (ScriptAndValue SAV) ->{
                        String id = SAV.value.getAsString();
                        if(!id.contains(":")) id = "minecraft:"+id;
                        return Registry.ITEM.get(new Identifier(id));
            }, Item::getRecipeRemainder),
            new ItemPropertyTranslator<>("itemGroup",Item.Settings::group,
                    (ScriptAndValue SAV) -> {
                        try {
                            return (ItemGroup) ItemGroup.class.getField(SAV.value.getAsString()).get(null);
                        } catch (Exception e) {
                            return ItemGroup.MISC;
                        }
            }, Item::getGroup),
            new ItemPropertyTranslator<>("rarity",Item.Settings::rarity,
                    (ScriptAndValue SAV) -> {
                        try {
                            return Rarity.valueOf(SAV.value.getAsString());
                        }catch(IllegalArgumentException e){
                            return Rarity.COMMON;
                        }
            }, item -> item.getRarity(item.getDefaultStack())),
            new ItemPropertyTranslator<>("foodComponent",Item.Settings::food,
                    (ScriptAndValue SAV) -> {
                        var thisObj = SAV.value.getAsJsonObject();

                        var toReturn = new FoodComponent.Builder()
                                .hunger(thisObj.get("hunger").getAsInt())
                                .saturationModifier(thisObj.get("saturation").getAsFloat());
                        if(thisObj.get("isMeat").getAsBoolean())
                            toReturn = toReturn.meat();
                        if(thisObj.get("isAlwaysEdible").getAsBoolean())
                            toReturn = toReturn.alwaysEdible();
                        if(thisObj.get("canEatFast").getAsBoolean())
                            toReturn = toReturn.snack();
                        if(thisObj.get("statusEffects").isJsonArray()){
                            var effects = thisObj.get("statusEffects").getAsJsonArray();
                            int length = effects.size();
                            for(int i=0;i<length;i++){
                                var thisEffect = effects.get(i).getAsJsonObject();
                                if(!thisEffect.has("effect"))
                                    continue;

                                try {
                                    var type = (StatusEffect) StatusEffects.class.getField(thisEffect.get("effect")
                                            .getAsString()).get(null);
                                    int duration = thisEffect.has("duration")?
                                            thisEffect.get("duration").getAsInt():0;
                                    int amplifier = thisEffect.has("amplifier")?
                                            thisEffect.get("amplifier").getAsInt():0;
                                    boolean visible = thisEffect.has("visible") ?
                                            thisEffect.get("visible").getAsBoolean():true;
                                    var effectToAdd = new StatusEffectInstance(
                                            type,
                                            duration,
                                            amplifier,
                                            false,
                                            visible
                                    );

                                    float chance = thisEffect.has("chance")?thisEffect.get("chance").getAsFloat():1;
                                    toReturn = toReturn.statusEffect(effectToAdd,chance);
                                }catch(NoSuchFieldException | IllegalAccessException ignored){}
                            }
                        }

                        return toReturn.build();
            }, Item::getFoodComponent),
            new ItemPropertyTranslator<>("isFireproof", (settings, fireproof) ->
                    fireproof ? settings.fireproof() : settings, BoolTransformFunc,
                    Item::isFireproof),
    };

    public static SuccessAndIdentifier itemFromFile(File file) {
        if(!file.canRead()) return new SuccessAndIdentifier(SuccessRate.CANT_READ);

        JsonObject itemJsonData;
        try {
            itemJsonData = Json.GSON.fromJson(
                    Files.readString(file.toPath()),
                    JsonObject.class
            );
        }catch(IOException | JsonSyntaxException e) {
            return new SuccessAndIdentifier(SuccessRate.BAD_JSON);
        }

        String itemName = "Invalid Name";
        try {
            itemName = itemJsonData.get("itemName").getAsString();
        }catch(UnsupportedOperationException ignored){}

        Item.Settings settings = new Item.Settings();
        settings.group(m3we.m3weItemGroup);

        for (ItemPropertyTranslator<?> propToTranslate : ItemIniter.itemPropertyMap) {
            if (!itemJsonData.has(propToTranslate.jsonProp))
                continue;

            settings = propToTranslate.apply(settings, itemJsonData, CustomScript.NULL);
        }

        Item thisItem;
        if(itemJsonData.has("blockToPlace")) {
            var blockToPlaceName = itemJsonData.get("blockToPlace").getAsString();
            if (!blockToPlaceName.contains(":")) blockToPlaceName = "minecraft:" + blockToPlaceName;
            thisItem = new BlockItem(Registry.BLOCK.get(new Identifier(blockToPlaceName)),settings);
        }else{
            thisItem= new Item(settings);
        }

        var namespace="json-blocks";
        if(itemJsonData.has("namespace")&&
                validNamespaceName.matcher(itemJsonData.get("namespace").getAsString()).matches()){
            namespace=itemJsonData.get("namespace").getAsString();
        }
        Identifier thisId = new Identifier(namespace, itemName);
        Registry.register(
                Registry.ITEM,
                thisId,
                thisItem
        );
        m3we.m3weItems.put(namespace+":"+itemName,thisItem);

        return new SuccessAndIdentifier(SuccessRate.YOU_DID_IT,thisId);
    }
}