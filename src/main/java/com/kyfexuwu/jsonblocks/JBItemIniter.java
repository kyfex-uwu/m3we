package com.kyfexuwu.jsonblocks;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
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
            new PropertyTranslator("recipeRemainder","recipeRemainder",(JsonElement element) ->{
                String id = element.getAsString();
                if(!id.contains(":")) id = "minecraft:"+id;
                return Registry.ITEM.get(new Identifier(id));
            }),
            new PropertyTranslator("itemGroup","group",(JsonElement element) -> {
                try {
                    return ItemGroup.class.getField(element.getAsString());
                } catch (NoSuchFieldException e) {
                    return ItemGroup.MISC;
                }
            }),
            new PropertyTranslator("rarity","rarity",(JsonElement element) -> {
                try {
                    return Rarity.valueOf(element.getAsString());
                }catch(IllegalArgumentException e){
                    return Rarity.COMMON;
                }
            }),
            new PropertyTranslator("foodComponent","foodComponent",(JsonElement element) -> {
                var thisObj = element.getAsJsonObject();

                var toReturn = new FoodComponent.Builder()
                        .hunger(thisObj.get("hunger").getAsInt())
                        .saturationModifier(thisObj.get("saturation").getAsFloat());
                if(thisObj.get("isMeat").getAsBoolean())
                    toReturn = toReturn.meat();
                if(thisObj.get("isAlwaysEdible").getAsBoolean())
                    toReturn = toReturn.alwaysEdible();
                if(thisObj.get("canEatFast").getAsBoolean())
                    toReturn = toReturn.snack();
                //todo: add status effects

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

        for(PropertyTranslator propToTranslate : JBItemIniter.itemPropertyMap){
            try {
                settings.getClass().getField(propToTranslate.javaProp)
                        .set(settings, propToTranslate.transformFunc.apply(itemJsonData.get(propToTranslate.jsonProp)));
            }catch(Exception ignored){}//whatever goes on in there, i dont wanna know uwu
        }

        Item thisItem;
        if(itemJsonData.has("blockToPlace")) {
            var blockToPlaceName = itemJsonData.get("blockToPlace").getAsString();
            if (!blockToPlaceName.contains(":")) blockToPlaceName = "json-blocks:" + blockToPlaceName;
            thisItem = new BlockItem(Registry.BLOCK.get(new Identifier(blockToPlaceName)),settings);
        }else{
            thisItem= new Item(settings);
        }

        Registry.register(
                Registry.ITEM,
                new Identifier("json-blocks", itemName),
                thisItem
        );
        JsonBlocks.jsonItems.put("json-blocks:"+itemName,thisItem);

        return SuccessRate.YOU_DID_IT;
    }
}