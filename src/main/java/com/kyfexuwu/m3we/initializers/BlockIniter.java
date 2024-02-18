package com.kyfexuwu.m3we.initializers;

import com.google.gson.*;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.m3we;
import net.minecraft.block.*;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.EmptyBlockView;
import org.luaj.vm2.LuaValue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.kyfexuwu.m3we.Utils.*;
import static com.kyfexuwu.m3we.initializers.InitUtils.*;


public class BlockIniter {

    private static final Material starterMaterial = new Material.Builder(MapColor.CLEAR).build();

    private static class BlockPropertyTranslator<T> extends InitUtils.PropertyTranslator<T, AbstractBlock.Settings, Block> {
        public BlockPropertyTranslator(String jsonProp,
                                       BiFunction<AbstractBlock.Settings, T, AbstractBlock.Settings> toJavaFunc,
                                       Function<InitUtils.ScriptAndValue, T> transformFunc,
                                       Function<Block, T> getDefaultFunc) {
            super(jsonProp, toJavaFunc, transformFunc, getDefaultFunc);
        }
    }
    private static final BlockPropertyTranslator<?>[] blockPropertyMap = {
            new BlockPropertyTranslator<>("hardness", AbstractBlock.Settings::hardness, FloatTransformFunc,
                    Block::getHardness),
            new BlockPropertyTranslator<>("resistance",AbstractBlock.Settings::resistance,FloatTransformFunc,
                    Block::getBlastResistance),
            new BlockPropertyTranslator<>("slipperiness",AbstractBlock.Settings::slipperiness,FloatTransformFunc,
                    Block::getSlipperiness),
            new BlockPropertyTranslator<>("jumpMultiplier",AbstractBlock.Settings::jumpVelocityMultiplier,FloatTransformFunc,
                    Block::getJumpVelocityMultiplier),
            new BlockPropertyTranslator<>("speedMultiplier",AbstractBlock.Settings::velocityMultiplier,FloatTransformFunc,
                    Block::getVelocityMultiplier),
            new BlockPropertyTranslator<>("sounds",AbstractBlock.Settings::sounds,(ScriptAndValue SAV) -> {
                try {
                    //todo! this does not work with translations
                    return (BlockSoundGroup) BlockSoundGroup.class.getField(SAV.value.getAsString()).get(null);
                }catch(NoSuchFieldException | IllegalAccessException e){
                    return BlockSoundGroup.STONE;
                }
            },block->block.getDefaultState().getSoundGroup()),
            new BlockPropertyTranslator<>("isOpaque",(settings, isOpaque) ->
                    isOpaque ? settings : settings.nonOpaque(), BoolTransformFunc,
                    block->block.getDefaultState().isOpaque()),
            new BlockPropertyTranslator<>("luminance", AbstractBlock.Settings::luminance, (ScriptAndValue SAV) -> {
                if (SAV.value.getAsString().startsWith("script:")) {
                    return state -> tryAndExecute(
                            0,
                            SAV.script,
                            SAV.value.getAsString().substring(7),
                            new Object[]{state},
                            LuaValue::checkint
                    );
                } else {
                    return value -> SAV.value.getAsInt();
                }
            }, block -> state -> state.getLuminance()),
            new BlockPropertyTranslator<>("mapColor",AbstractBlock.Settings::mapColor,(ScriptAndValue SAV) -> {
                try {
                    //todo! this does not work with translations
                    return (MapColor) MapColor.class.getField(SAV.value.getAsString()).get(null);
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    return MapColor.CLEAR;
                }
            }, AbstractBlock::getDefaultMapColor),
            new BlockPropertyTranslator<>("isToolRequired", (settings, toolRequired) ->
                    toolRequired ? settings.requiresTool() : settings, BoolTransformFunc,
                    block->block.getDefaultState().isToolRequired()),
            new BlockPropertyTranslator<>("ticksRandomly", (settings, randomlyTicks) ->
                    randomlyTicks ? settings.ticksRandomly() : settings, BoolTransformFunc,
                    block -> block.getDefaultState().hasRandomTicks()),
            new BlockPropertyTranslator<>("isAir", (settings, isAir) ->
                    isAir ? settings.air() : settings, BoolTransformFunc,
                    block -> block.getDefaultState().isAir()),
            new BlockPropertyTranslator<>("isCollidable", (settings, collidable) ->
                    collidable ? settings : settings.noCollision(), BoolTransformFunc,
                    block -> !block.getDefaultState().getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN).isEmpty()),
            new BlockPropertyTranslator<>("blockCollisionCanResize", (settings, dynBounds) ->
                    dynBounds ? settings : settings.dynamicBounds(), BoolTransformFunc,
                    Block::hasDynamicBounds),
            new BlockPropertyTranslator<>("isSolidWhen", AbstractBlock.Settings::solidBlock,
                    (ScriptAndValue SAV) -> PredTransformFunc(SAV,true),
                    block -> (state, world, pos) -> state.getMaterial().blocksLight() && state.isFullCube(world, pos)),
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
            }, block -> (state, world, pos, type) -> state.isSideSolidFullSquare(world, pos, Direction.UP) &&
                    state.getLuminance() < 14),
            new BlockPropertyTranslator<>("visionBlockedWhen",AbstractBlock.Settings::blockVision,
                    (ScriptAndValue SAV) -> PredTransformFunc(SAV,false),
                    block -> (state, world, pos) -> block.getDefaultState().getMaterial().blocksMovement() &&
                            state.isFullCube(world, pos)),
            new BlockPropertyTranslator<>("suffocatesWhen",AbstractBlock.Settings::suffocates,
                    (ScriptAndValue SAV) -> PredTransformFunc(SAV,false),
                    block -> (state, world, pos) -> block.getDefaultState().getMaterial().blocksMovement() &&
                            state.isFullCube(world, pos)),
            new BlockPropertyTranslator<>("emissiveLightingWhen",AbstractBlock.Settings::emissiveLighting,
                    (ScriptAndValue SAV) -> PredTransformFunc(SAV,false),
                    block -> (state, world, pos) -> false),
            new BlockPropertyTranslator<>("postProcessWhen",AbstractBlock.Settings::postProcess,
                    (ScriptAndValue SAV) -> PredTransformFunc(SAV,false),
                    block -> (state, world, pos) -> false),
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
        String blockName;
        try {
            blockName = blockJsonData.get("blockName").getAsString();
        }catch(Exception ignored){
            return new SuccessAndIdentifier(SuccessRate.BAD_JSON);
        }
        if(!validPathName.matcher(blockName).matches())
            return new SuccessAndIdentifier(SuccessRate.BAD_JSON);

        //setting material
        AbstractBlock.Settings settings = AbstractBlock.Settings.of(starterMaterial);

        //setting the properties of the block settings
        var scriptName = getOr(blockJsonData, "script", new JsonPrimitive("")).getAsString();
        CustomScript tempScript = new CustomScript(scriptName);

        var copyFromName = getOr(blockJsonData, "copyFrom", new JsonPrimitive("")).getAsString();
        var copyFromBlockOptional = Registry.BLOCK.getOrEmpty(new Identifier(copyFromName));
        var copyFrom = copyFromBlockOptional.orElseGet(()-> m3we.m3weBlocks.get(copyFromName));
        if(!copyFromName.isEmpty()&&copyFrom==null)
            return new SuccessAndIdentifier(SuccessRate.COME_BACK_LATER);

        for (BlockPropertyTranslator<?> propToTranslate : BlockIniter.blockPropertyMap) {
            if (!blockJsonData.has(propToTranslate.jsonProp)) {
                if(copyFrom!=null) settings = propToTranslate.applyDefault(settings, copyFrom);
                continue;
            }

            settings = propToTranslate.apply(settings, blockJsonData, tempScript);
        }
        tempScript.remove();

        var thisBlock = CustomBlockMaker.from(
                settings,
                copyFrom,
                getOr(blockJsonData, "blockStates", new JsonObject()),
                blockJsonData,
                scriptName,
                (blockJsonData.has("blockEntityScript")?
                        blockJsonData.get("blockEntityScript").getAsString():
                        null)
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
        m3we.m3weBlocks.put(namespace+":"+blockName,thisBlock);

        return new SuccessAndIdentifier(SuccessRate.YOU_DID_IT,thisId);
    }
}
