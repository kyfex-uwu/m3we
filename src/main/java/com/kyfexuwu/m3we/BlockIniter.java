package com.kyfexuwu.m3we;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.kyfexuwu.m3we.lua.CustomScript;
import net.minecraft.block.*;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.luaj.vm2.LuaValue;

import static com.kyfexuwu.m3we.Utils.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.BiFunction;
import java.util.function.Function;


public class BlockIniter {

    private static final Material starterMaterial = new Material.Builder(MapColor.CLEAR).build();

    private static class BlockPropertyTranslator<T> extends PropertyTranslator<T, AbstractBlock.Settings> {
        public BlockPropertyTranslator(String jsonProp,
                                       BiFunction<AbstractBlock.Settings, T, AbstractBlock.Settings> toJavaFunc,
                                       Function<ScriptAndValue, T> transformFunc) {
            super(jsonProp, toJavaFunc, transformFunc);
        }
    }
    private static final BlockPropertyTranslator<?>[] blockPropertyMap = {
            new BlockPropertyTranslator<>("hardness", AbstractBlock.Settings::hardness, FloatTransformFunc),
            new BlockPropertyTranslator<>("resistance",AbstractBlock.Settings::resistance,FloatTransformFunc),
            new BlockPropertyTranslator<>("slipperiness",AbstractBlock.Settings::slipperiness,FloatTransformFunc),
            new BlockPropertyTranslator<>("jumpMultiplier",AbstractBlock.Settings::jumpVelocityMultiplier,FloatTransformFunc),
            new BlockPropertyTranslator<>("speedMultiplier",AbstractBlock.Settings::velocityMultiplier,FloatTransformFunc),
            new BlockPropertyTranslator<>("sounds",AbstractBlock.Settings::sounds,(ScriptAndValue SAV) -> {
                try {
                    //todo
                    return (BlockSoundGroup) BlockSoundGroup.class.getField(SAV.value.getAsString()).get(null);
                    //todo: custom sounds?
                }catch(NoSuchFieldException | IllegalAccessException e){
                    return BlockSoundGroup.STONE;
                }
            }),
            new BlockPropertyTranslator<>("isOpaque",(settings, isOpaque) ->
                    isOpaque ? settings : settings.nonOpaque(), BoolTransformFunc),
            new BlockPropertyTranslator<>("luminance",AbstractBlock.Settings::luminance,(ScriptAndValue SAV) -> {
                if(SAV.value.getAsString().startsWith("script:")){
                    return state -> tryAndExecute(
                            0,
                            SAV.script,
                            SAV.value.getAsString().substring(7),
                            new Object[]{state},
                            LuaValue::checkint
                    );
                }else{
                    return value -> SAV.value.getAsInt();
                }
            }),
            new BlockPropertyTranslator<>("mapColor",AbstractBlock.Settings::mapColor,(ScriptAndValue SAV) -> {
                try {
                    return (MapColor) MapColor.class.getField(SAV.value.getAsString()).get(null);
                    //todo: custom colors?
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    return MapColor.CLEAR;
                }
            }),
            /*
            new PropertyTranslator("drops",AbstractBlock.Settings::,(ScriptAndValue SAV) -> {
                try{
                    return LootTables.class.getField(SAV.value.getAsString()).get(null);
                    //todo: custom drops
                }catch(NoSuchFieldException | IllegalAccessException e){
                    return LootTables.EMPTY;
                }
            }),
            */
            new BlockPropertyTranslator<>("isToolRequired", (settings, toolRequired) ->
                    toolRequired ? settings.requiresTool() : settings, BoolTransformFunc),
            new BlockPropertyTranslator<>("ticksRandomly", (settings, randomlyTicks) ->
                    randomlyTicks ? settings.ticksRandomly() : settings, BoolTransformFunc),
            new BlockPropertyTranslator<>("isAir", (settings, isAir) ->
                    isAir ? settings.air() : settings, BoolTransformFunc),
            new BlockPropertyTranslator<>("isCollidable", (settings, collidable) ->
                    collidable ? settings : settings.noCollision(), BoolTransformFunc),
            new BlockPropertyTranslator<>("blockCollisionCanResize", (settings, dynBounds) ->
                    dynBounds ? settings : settings.dynamicBounds(), BoolTransformFunc),
            new BlockPropertyTranslator<>("isSolidWhen", AbstractBlock.Settings::solidBlock,
                    (ScriptAndValue SAV) -> PredTransformFunc(SAV,true)),
            new BlockPropertyTranslator<>("allowsSpawningWhen",AbstractBlock.Settings::allowsSpawning,
                    (ScriptAndValue SAV) ->{
                        if(SAV.value.getAsString().startsWith("script:")){
                            return (state, world, pos, type) -> tryAndExecute(
                                    true,
                                    SAV.script,
                                    SAV.value.getAsString().substring(7),
                                    new Object[]{state,world,pos,type},
                                    LuaValue::checkboolean
                            );
                        }else{
                            return (state, world, pos, type) -> SAV.value.getAsBoolean();
                        }
            }),
            new BlockPropertyTranslator<>("visionBlockedWhen",AbstractBlock.Settings::blockVision,
                    (ScriptAndValue SAV) -> PredTransformFunc(SAV,false)),
            new BlockPropertyTranslator<>("suffocatesWhen",AbstractBlock.Settings::suffocates,
                    (ScriptAndValue SAV) -> PredTransformFunc(SAV,false)),
            new BlockPropertyTranslator<>("emissiveLightingWhen",AbstractBlock.Settings::emissiveLighting,
                    (ScriptAndValue SAV) -> PredTransformFunc(SAV,false)),
            new BlockPropertyTranslator<>("postProcessWhen",AbstractBlock.Settings::postProcess,
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
        AbstractBlock.Settings settings = AbstractBlock.Settings.of(starterMaterial);

        //setting the properties of the block settings
        String scriptName = null;
        try{
            scriptName = blockJsonData.get("script").getAsString();
        }catch(Exception ignored){}
        CustomScript tempScript = new CustomScript(scriptName);
        for (BlockPropertyTranslator<?> propToTranslate : BlockIniter.blockPropertyMap) {
            if (!blockJsonData.has(propToTranslate.jsonProp))
                continue;

            settings = propToTranslate.apply(settings, blockJsonData, tempScript);
        }
        tempScript.remove();

        Block thisBlock;
        JsonObject blockStates = new JsonObject();
        String script = null;
        if(blockJsonData.has("blockStates"))
            blockStates = blockJsonData.get("blockStates").getAsJsonObject();
        if(blockJsonData.has("script"))
            script = blockJsonData.get("script").getAsString();
        thisBlock = CustomBlockMaker.from(
                settings,
                blockStates,
                blockJsonData.get("blockShape"),
                blockJsonData.get("outlineShape"),
                script
        );

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
        m3we.jsonBlocks.put(namespace+":"+blockName,thisBlock);

        return new SuccessAndIdentifier(SuccessRate.YOU_DID_IT,thisId);
    }
}
