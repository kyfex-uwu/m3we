package com.kyfexuwu.jsonblocks;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.kyfexuwu.jsonblocks.lua.CustomScript;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.entity.EntityType;
import net.minecraft.loot.LootTables;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.luaj.vm2.LuaValue;

import static com.kyfexuwu.jsonblocks.Utils.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Function;
import java.util.function.ToIntFunction;


public class JBBlockIniter {

    //need to just overhaul this entire thingy
    private static final PropertyTranslator[] blockPropertyMap = {
            new PropertyTranslator("hardness","hardness",FloatTransformFunc),
            new PropertyTranslator("resistance","resistance",FloatTransformFunc),
            new PropertyTranslator("slipperiness","slipperiness",FloatTransformFunc),
            new PropertyTranslator("jumpMultiplier","jumpVelocityMultiplier",FloatTransformFunc),
            new PropertyTranslator("speedMultiplier","velocityMultiplier",FloatTransformFunc),
            new PropertyTranslator("sounds","soundGroup",(ScriptAndValue SAV) -> {
                try {
                    return BlockSoundGroup.class.getField(SAV.value.getAsString()).get(null);
                    //todo: custom sounds?
                }catch(NoSuchFieldException | IllegalAccessException e){
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
                    return (ToIntFunction<BlockState>) value -> SAV.value.getAsInt();
                }
            }),
            new PropertyTranslator("mapColor","mapColorProvider",(ScriptAndValue SAV) ->
                (Function<BlockState, MapColor>) blockState -> {
                    try {
                        return (MapColor) MapColor.class.getField(SAV.value.getAsString()).get(null);
                        //todo: custom colors?
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        return MapColor.CLEAR;
                    }
            }),
            new PropertyTranslator("drops","lootTableId",(ScriptAndValue SAV) -> {
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
            new PropertyTranslator("isSolidWhen", "solidBlockPredicate",
                    //kyfex remember these are the variable name not the setter name, dont freak out
                    (ScriptAndValue SAV) -> PredTransformFunc(SAV,true)),
            new PropertyTranslator("allowsSpawningWhen","allowsSpawningPredicate",(ScriptAndValue SAV) ->{
                if(SAV.value.getAsString().startsWith("script:")){
                    return (AbstractBlock.TypedContextPredicate<EntityType<?>>) (state, world, pos, type) -> tryAndExecute(
                            true,
                            SAV.script,
                            SAV.value.getAsString().substring(7),
                            new Object[]{state,world,pos,type},
                            LuaValue::checkboolean
                    );
                }else{
                    return (AbstractBlock.TypedContextPredicate<EntityType<?>>)
                            (state, world, pos, type) -> SAV.value.getAsBoolean();
                }
            }),
            new PropertyTranslator("visionBlockedWhen","blockVisionPredicate",
                    (ScriptAndValue SAV) -> PredTransformFunc(SAV,false)),
            new PropertyTranslator("suffocatesWhen","suffocationPredicate",
                    (ScriptAndValue SAV) -> PredTransformFunc(SAV,false)),
            new PropertyTranslator("emissiveLightingWhen","emissiveLightingPredicate",
                    (ScriptAndValue SAV) -> PredTransformFunc(SAV,false)),
            new PropertyTranslator("postProcessWhen","postProcessPredicate",
                    (ScriptAndValue SAV) -> PredTransformFunc(SAV,false)),
    };

    public static SuccessAndIdentifier blockFromFile(File file) {
        if(!file.canRead()) return new SuccessAndIdentifier(SuccessRate.CANT_READ);

        //file to json
        JsonObject blockJsonData;
        try {
            blockJsonData = new JsonParser().parse(
                    Files.readString(file.toPath())
            ).getAsJsonObject();
        }catch(IOException | JsonSyntaxException e) {
            return new SuccessAndIdentifier(SuccessRate.BAD_JSON);
        }

        //setting block name
        String blockName = "invalid";
        try {
            blockName = blockJsonData.get("blockName").getAsString();
        }catch(Exception ignored){}//i have no clue what this can throw lol
        if(!validPathName.matcher(blockName).matches())
            return new SuccessAndIdentifier(SuccessRate.BAD_JSON);

        //setting material
        AbstractBlock.Settings settings;
        try{
            Object material = Material.class.getField(blockJsonData.get("material").getAsString()).get(null);
            if(!(material instanceof Material)) material=Material.STONE;
            settings = FabricBlockSettings.of((Material) material);
        }catch (NoSuchFieldException | IllegalAccessException e){
            settings = FabricBlockSettings.of(Material.STONE);
        }

        if(blockJsonData.has("script")) {
            //setting the properties of the blocksettings
            CustomScript script = new CustomScript(blockJsonData.get("script").getAsString());
            for (PropertyTranslator propToTranslate : JBBlockIniter.blockPropertyMap) {
                if (!blockJsonData.has(propToTranslate.jsonProp))
                    continue;


                try {
                    //setting the specified java field to
                    //the json value put through the transform func
                    settings.getClass().getField(propToTranslate.javaProp)
                            .set(settings, propToTranslate.transformFunc.apply(new ScriptAndValue(
                                    script,
                                    blockJsonData.get(propToTranslate.jsonProp)
                            )));
                } catch (Exception e) {
                    System.out.println(propToTranslate.jsonProp + " failt");
                }
            }
            script.remove();//IMPORTANT
        }

        Block thisBlock;
        JsonObject blockStates = null;
        String script = null;
        if(blockJsonData.has("blockStates"))
            blockStates = blockJsonData.get("blockStates").getAsJsonObject();
        if(blockJsonData.has("script"))
            script = blockJsonData.get("script").getAsString();
        if(blockStates!=null||script!=null){
            thisBlock = CustomBlockMaker.from(
                    settings,
                    blockStates,
                    blockJsonData.get("blockShape"),
                    script
            );
        }else{
            thisBlock = new Block(settings);
        }

        var namespace="m3we";
        if(blockJsonData.has("namespace")&&
                validNamespaceName.matcher(blockJsonData.get("namespace").getAsString()).matches()){
            namespace=blockJsonData.get("namespace").getAsString();
        }
        Identifier thisId = new Identifier(namespace, blockName);
        Registry.register(
                Registry.BLOCK,
                thisId,
                thisBlock
        );
        JsonBlocks.jsonBlocks.put(namespace+":"+blockName,thisBlock);

        return new SuccessAndIdentifier(SuccessRate.YOU_DID_IT,thisId);
    }
}
