package com.kyfexuwu.jsonblocks;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.loot.LootTables;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;

import static com.kyfexuwu.jsonblocks.Utils.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;


public class JBBlockIniter {

    private static final PropertyTranslator[] blockPropertyMap = {
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

    private static JsonObject blockStates;
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

        for(PropertyTranslator propToTranslate : JBBlockIniter.blockPropertyMap){
            try {
                settings.getClass().getField(propToTranslate.javaProp)
                        .set(settings, propToTranslate.transformFunc.apply(blockJsonData.get(propToTranslate.jsonProp)));
            }catch(Exception ignored){}//whatever goes on in there, i dont wanna know uwu
        }

        Block thisBlock;
        if(blockJsonData.has("blockStates")) {
            blockStates = blockJsonData.get("blockStates").getAsJsonObject();
            class customBlock extends Block {

                public static ArrayList<Property> propsList = new ArrayList<Property>();
                public static Property[] props;

                static {
                    for(String propName : blockStates.keySet()){
                        var thisState=blockStates.get(propName).getAsJsonObject();
                        switch(thisState.get("type").getAsString()){
                            case "int":
                                propsList.add(IntProperty.of(
                                        propName,
                                        thisState.get("min").getAsInt(),
                                        thisState.get("max").getAsInt()
                                ));
                                break;
                            case "bool":
                                propsList.add(BooleanProperty.of(propName));
                                break;
                            case "enum":
                                //propsList.add(IntProperty.of("test", 0, 3));
                                break;
                            case "direction":
                                propsList.add(DirectionProperty.of(propName));
                                break;
                        }
                    }
                    props = propsList.toArray(new Property[0]);
                }

                public customBlock(Settings settings) {
                    super(settings);

                    var defaultState = getStateManager().getDefaultState();
                    for(Property prop : props){
                        try {
                            var jsonDefault = blockStates.get(prop.getName()).getAsJsonObject().get("default");
                            switch (prop.getType().getName()) {
                                case "java.lang.Integer" -> defaultState = defaultState.with(prop, jsonDefault.getAsInt());
                                case "java.lang.Boolean" -> defaultState = defaultState.with(prop, jsonDefault.getAsBoolean());
                                case "net.minecraft.util.math.Direction" -> defaultState = defaultState.with(prop,
                                        Direction.byName(jsonDefault.getAsString()));
                            }
                        }catch(Exception e){
                            System.out.println("Property "+prop.getName()+" has an invalid default value");
                        }
                    }
                    setDefaultState(defaultState);
                }

                @Override
                protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
                    for(Property prop : props){
                        builder.add(prop);
                    }
                }
            }
            thisBlock = new customBlock(settings);
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
