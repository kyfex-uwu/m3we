package com.kyfexuwu.jsonblocks;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.loot.LootTables;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import static com.kyfexuwu.jsonblocks.Utils.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


public class JBBlock {

    private static PropertyTranslator[] blockPropertyMap = {
            new PropertyTranslator("hardness","hardness",FloatTransformFunc),
            new PropertyTranslator("resistance","resistance",FloatTransformFunc),
            new PropertyTranslator("slipperiness","slipperiness",FloatTransformFunc),
            new PropertyTranslator("jumpMultiplier","jumpVelocityMultiplier",FloatTransformFunc),
            new PropertyTranslator("speedMultiplier","velocityMultiplier",FloatTransformFunc),
            new PropertyTranslator("sounds","soundGroup",(JsonElement element) -> {
                try {
                    return BlockSoundGroup.class.getField(element.getAsString());
                }catch(NoSuchFieldException e){
                    return BlockSoundGroup.STONE;
                }
            }),
            new PropertyTranslator("isOpaque","opaque",BoolTransformFunc),
            //new PropertyTranslator("luminance","luminance",PredTransformFunc),
            //new PropertyTranslator("mapColor","mapColorProvider",PredTransformFunc),
            new PropertyTranslator("drops","lootTableID",(JsonElement element) -> {
                try{
                    return LootTables.class.getField(element.getAsString());
                }catch(NoSuchFieldException e){
                    return LootTables.EMPTY;
                }
            }),
            //dropsLike we gotta do manually
            new PropertyTranslator("isToolRequired","toolRequired",BoolTransformFunc),
            new PropertyTranslator("ticksRandomly","randomTicks",BoolTransformFunc),
            new PropertyTranslator("isAir","isAir", BoolTransformFunc),
            new PropertyTranslator("isCollidable","collidable",BoolTransformFunc),
            new PropertyTranslator("blockCollisionCanResize","dynamicBounds",BoolTransformFunc),
            //new PropertyTranslator("isSolidBlock", "solidBlockPredicate",PredTransformFunc),
            //new PropertyTranslator("allowsSpawningWhen","allowsSpawningPredicate", PredTransformFunc),
            //new PropertyTranslator("visionBlockedWhen","blockVisionPredicate",PredTransformFunc),
            //new PropertyTranslator("suffocatesWhen","suffocationPredicate",PredTransformFunc),
            //new PropertyTranslator("emissiveLighting","emissiveLightingPredicate",PredTransformFunc),
            //new PropertyTranslator("postProcess","postProcessPredicate",PredTransformFunc),
    };

    public static SuccessRate blockFromFile(File file) {
        if(!file.canRead()) return SuccessRate.CANT_READ;

        JsonObject blockJsonData;
        try {
            blockJsonData = new JsonParser().parse(
                    Files.readString(file.toPath())
            ).getAsJsonObject();
        }catch(IOException | JsonSyntaxException e) {
            return SuccessRate.BAD_JSON;
        }

        String blockName = "Invalid Name";
        try {
            blockName = blockJsonData.get("blockName").getAsString();
        }catch(Exception ignored){}//i have no clue what this can throw lol

        FabricBlockSettings settings;
        try{
            Object material = Material.class.getField(blockJsonData.get("material").getAsString()).get(null);
            if(!(material instanceof Material)) material=Material.STONE;
            settings = FabricBlockSettings.of((Material) material);
        }catch (NoSuchFieldException e){
            settings = FabricBlockSettings.of(Material.STONE);
        }catch (IllegalAccessException e) {
            return SuccessRate.IDK;
        }

        for(PropertyTranslator propToTranslate : JBBlock.blockPropertyMap){
            try {
                settings.getClass().getField(propToTranslate.javaProp)
                        .set(settings, propToTranslate.transformFunc.apply(blockJsonData.get(propToTranslate.jsonProp)));
            }catch(Exception ignored){}//whatever goes on in there, i dont wanna know uwu
        }

        Block thisBlock = new Block(settings);
        Registry.register(
                Registry.BLOCK,
                new Identifier("json-blocks", blockName),
                thisBlock
        );
        JsonBlocks.jsonBlocks.put("json-blocks:"+blockName,thisBlock);

        return SuccessRate.YOU_DID_IT;
    }
}
