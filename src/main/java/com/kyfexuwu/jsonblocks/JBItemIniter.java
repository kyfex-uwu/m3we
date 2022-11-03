package com.kyfexuwu.jsonblocks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.kyfexuwu.jsonblocks.lua.CustomScript;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
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

import static com.kyfexuwu.jsonblocks.Utils.*;

public class JBItemIniter {

    private static final PropertyTranslator[] itemPropertyMap = {
            new PropertyTranslator("maxCount","maxCount",IntTransformFunc),
            new PropertyTranslator("maxDamage","maxDamage",IntTransformFunc),
            new PropertyTranslator("recipeRemainder","recipeRemainder",(ScriptAndValue SAV) ->{
                String id = SAV.value.getAsString();
                if(!id.contains(":")) id = "minecraft:"+id;
                return Registry.ITEM.get(new Identifier(id));
            }),
            new PropertyTranslator("itemGroup","group",(ScriptAndValue SAV) -> {
                try {
                    return ItemGroup.class.getField(SAV.value.getAsString());
                } catch (NoSuchFieldException e) {
                    return ItemGroup.MISC;
                }
            }),
            new PropertyTranslator("rarity","rarity",(ScriptAndValue SAV) -> {
                try {
                    return Rarity.valueOf(SAV.value.getAsString());
                }catch(IllegalArgumentException e){
                    return Rarity.COMMON;
                }
            }),
            new PropertyTranslator("foodComponent","foodComponent",(ScriptAndValue SAV) -> {
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
                            int duration = thisEffect.has("duration")?thisEffect.get("duration").getAsInt():0;
                            int amplifier = thisEffect.has("amplifier")?thisEffect.get("amplifier").getAsInt():0;
                            boolean visible = thisEffect.has("visible")?thisEffect.get("visible").getAsBoolean():true;
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
            }),
            new PropertyTranslator("isFireproof","fireproof",BoolTransformFunc),
    };

    public static SuccessRate itemFromFile(File file) {
        if(!file.canRead()) return SuccessRate.CANT_READ;

        JsonObject itemJsonData;
        try {
            itemJsonData = new JsonParser().parse(
                    Files.readString(file.toPath())
            ).getAsJsonObject();
        }catch(IOException | JsonSyntaxException e) {
            return SuccessRate.BAD_JSON;
        }

        String itemName = "Invalid Name";
        try {
            itemName = itemJsonData.get("itemName").getAsString();
        }catch(UnsupportedOperationException ignored){}

        FabricItemSettings settings = new FabricItemSettings();
        settings.group(JsonBlocks.m3weItems);

        for(PropertyTranslator propToTranslate : JBItemIniter.itemPropertyMap){
            try {
                settings.getClass().getField(propToTranslate.javaProp)
                        .set(settings, propToTranslate.transformFunc.apply(
                                new ScriptAndValue(null,itemJsonData.get(propToTranslate.jsonProp))
                        ));
            }catch(Exception ignored){}//whatever goes on in there, i dont wanna know uwu
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
        Registry.register(
                Registry.ITEM,
                new Identifier(namespace, itemName),
                thisItem
        );
        JsonBlocks.jsonItems.put(namespace+":"+itemName,thisItem);

        return SuccessRate.YOU_DID_IT;
    }
}