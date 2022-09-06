package com.kyfexuwu.jsonblocks;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.kyfexuwu.jsonblocks.lua.CustomBlock;
import com.kyfexuwu.jsonblocks.lua.CustomScript;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.block.Material;
import net.minecraft.loot.LootTables;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.luaj.vm2.LuaValue;

import static com.kyfexuwu.jsonblocks.Utils.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.ToIntFunction;


public class JBBlockIniter {

    private static final PropertyTranslator[] blockPropertyMap = {
            new PropertyTranslator("hardness","hardness",FloatTransformFunc),
            new PropertyTranslator("resistance","resistance",FloatTransformFunc),
            new PropertyTranslator("slipperiness","slipperiness",FloatTransformFunc),
            new PropertyTranslator("jumpMultiplier","jumpVelocityMultiplier",FloatTransformFunc),
            new PropertyTranslator("speedMultiplier","velocityMultiplier",FloatTransformFunc),
            new PropertyTranslator("sounds","soundGroup",(ScriptAndValue SAV) -> {
                try {
                    return BlockSoundGroup.class.getField(SAV.value.getAsString());
                    //todo: custom sounds?
                }catch(NoSuchFieldException e){
                    return BlockSoundGroup.STONE;
                }
            }),
            new PropertyTranslator("isOpaque","opaque",BoolTransformFunc),
            new PropertyTranslator("luminance","luminance",(ScriptAndValue SAV) -> {
                if(SAV.value.getAsString().startsWith("script:")){
                    return (ToIntFunction<BlockState>) state -> tryAndExecute(
                            0,
                            SAV.script,
                            SAV.value.getAsString().substring(7),
                            new Object[]{state},
                            LuaValue::checkint
                    );
                }else{
                    return (ToIntFunction<BlockState>) state -> SAV.value.getAsInt();
                }
            }),
            new PropertyTranslator("mapColor","mapColorProvider",(ScriptAndValue SAV) ->{
                try {
                    return MapColor.class.getField(SAV.value.getAsString()).get(null);
                    //todo: custom colors?
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    return MapColor.CLEAR;
                }
            }),
            new PropertyTranslator("drops","lootTableID",(ScriptAndValue SAV) -> {
                try{
                    return LootTables.class.getField(SAV.value.getAsString());
                    //todo: custom drops
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
            //new PropertyTranslator("isSolidWhen", "solidBlock",PredTransformFunc),
            //new PropertyTranslator("allowsSpawningWhen","allowsSpawning", PredTransformFunc),
            //new PropertyTranslator("visionBlockedWhen","blockVision",PredTransformFunc),
            //new PropertyTranslator("suffocatesWhen","suffocates",PredTransformFunc),
            //new PropertyTranslator("emissiveLightingWhen","emissiveLighting",PredTransformFunc),
            //new PropertyTranslator("postProcessWhen","postProcess",PredTransformFunc),

            //todo: add block shape (stairs)
    };

    public static SuccessRate blockFromFile(File file) {
        if(!file.canRead()) return SuccessRate.CANT_READ;

        //file to json
        JsonObject blockJsonData;
        try {
            blockJsonData = new JsonParser().parse(
                    Files.readString(file.toPath())
            ).getAsJsonObject();
        }catch(IOException | JsonSyntaxException e) {
            return SuccessRate.BAD_JSON;
        }

        //setting block name
        String blockName = "Invalid Name";
        try {
            blockName = blockJsonData.get("blockName").getAsString();
        }catch(Exception ignored){}//i have no clue what this can throw lol

        //setting material
        FabricBlockSettings settings;
        try{
            Object material = Material.class.getField(blockJsonData.get("material").getAsString()).get(null);
            if(!(material instanceof Material)) material=Material.STONE;
            settings = FabricBlockSettings.of((Material) material);
        }catch (NoSuchFieldException | IllegalAccessException e){
            settings = FabricBlockSettings.of(Material.STONE);
        }

        //initing block. if i coded good this would be useful here, but
        //i think i have a memory leak atm
        Block thisBlock=null;
        CustomScript script = null;
        boolean isCustom = blockJsonData.has("blockStates")||blockJsonData.has("script");
        if(isCustom) {
            script = new CustomScript(blockJsonData.get("script").getAsString());
        }
        //setting the properties of the blocksettings
        for(PropertyTranslator propToTranslate : JBBlockIniter.blockPropertyMap){
            try {
                //setting the specified java field to
                //the json value put through the transform func
                CustomBlock finalThisBlock = (CustomBlock)thisBlock;
                settings.getClass().getField(propToTranslate.javaProp)
                        .set(settings, propToTranslate.transformFunc.apply(new ScriptAndValue(
                                script,
                                blockJsonData.get(propToTranslate.jsonProp)
                        )));
            }catch(Exception ignored){}//whatever goes on in there, i dont wanna know uwu
        }

        if(isCustom){
            thisBlock = CustomBlockMaker.from(
                    settings,
                    blockJsonData.get("blockStates").getAsJsonObject(),
                    script
            );
        }else{
            thisBlock = new Block(settings);
        }

        Registry.register(
                Registry.BLOCK,
                new Identifier("json-blocks", blockName),
                thisBlock
        );
        JsonBlocks.jsonBlocks.put("json-blocks:"+blockName,thisBlock);

        return SuccessRate.YOU_DID_IT;
    }
}
