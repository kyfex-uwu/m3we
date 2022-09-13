package com.kyfexuwu.jsonblocks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kyfexuwu.jsonblocks.lua.CustomBlock;
import com.kyfexuwu.jsonblocks.lua.CustomScript;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;

import static com.kyfexuwu.jsonblocks.Utils.tryAndExecute;
import static com.kyfexuwu.jsonblocks.Utils.validPropertyName;

public class CustomBlockMaker {
    public static JsonObject tempBlockStates;//prolly can be fixed
    public static String tempScriptName;
    public static JsonElement tempBlockShape;

    public static Block from(AbstractBlock.Settings settings, JsonObject blockStates, JsonElement blockShape, String scriptName) {
        tempBlockStates = blockStates;//PLEASE ONLY USE ME IN THE STATIC BLOCK I DO NOT KNOW WHAT WILL HAPPEN IF U DONT
        tempScriptName = scriptName;
        tempBlockShape = blockShape;

        class thisCustomBlock extends CustomBlock {

            public static final LinkedList<Property> propsList = new LinkedList<>();
            public static final Property[] props;
            public static final VoxelShape blockShape;
            public static final boolean shapeIsScript;
            public static final String shapeScriptString;

            static {
                scriptContainer=new CustomScript(tempScriptName);

                //props
                for (String propName : tempBlockStates.keySet()) {
                    if (!validPropertyName.matcher(propName).matches()) continue;
                    var thisState = tempBlockStates.get(propName).getAsJsonObject();
                    switch (thisState.get("type").getAsString()) {
                        case "int":
                            propsList.add(IntProperty.of(
                                    propName,
                                    thisState.get("min").getAsInt(),
                                    thisState.get("max").getAsInt()
                            ));
                            break;
                        case "boolean":
                            propsList.add(BooleanProperty.of(propName));
                            break;
                        case "enum"://TODO
                            //propsList.add(EnumProperty.of("test", 0, 3));
                            break;
                        case "direction":
                            propsList.add(DirectionProperty.of(propName));
                            break;
                    }
                }
                props = propsList.toArray(new Property[0]);
                propsList.clear();

                //shape
                if(tempBlockShape.isJsonArray()) {
                    var blockShapeToGive = VoxelShapes.empty();
                    for (JsonElement element : (JsonArray) tempBlockShape) {
                        if (!element.isJsonArray())
                            continue;
                        JsonArray elementArray = element.getAsJsonArray();
                        if (elementArray.size() != 6)
                            continue;
                        blockShapeToGive = VoxelShapes.union(blockShapeToGive, VoxelShapes.cuboid(
                                elementArray.get(0).getAsDouble(),
                                elementArray.get(1).getAsDouble(),
                                elementArray.get(2).getAsDouble(),
                                elementArray.get(3).getAsDouble(),
                                elementArray.get(4).getAsDouble(),
                                elementArray.get(5).getAsDouble()
                        ));
                    }
                    blockShape = blockShapeToGive;
                    shapeIsScript=false;
                    shapeScriptString=null;
                }else{
                    blockShape = VoxelShapes.empty();
                    shapeIsScript=tempBlockShape.getAsString().startsWith("script:");
                    shapeScriptString=shapeIsScript?null:tempBlockShape.getAsString().substring(7);
                }
            }

            public thisCustomBlock(Settings settings) {
                super(settings);

                var defaultState = getStateManager().getDefaultState();
                for (Property prop : props) {
                    try {
                        var jsonDefault = blockStates.get(prop.getName()).getAsJsonObject().get("default");
                        switch (prop.getType().getName()) {
                            case "java.lang.Integer" ->
                                    defaultState = defaultState.with(prop, jsonDefault.getAsInt());
                            case "java.lang.Boolean" ->
                                    defaultState = defaultState.with(prop, jsonDefault.getAsBoolean());
                            case "net.minecraft.util.math.Direction" ->
                                    defaultState = defaultState.with(prop,
                                        Direction.byName(jsonDefault.getAsString()));
                            //todo: add enum
                        }
                    } catch (Exception e) {
                        System.out.println("Property " + prop.getName() + " has an invalid default value");
                    }
                }
                setDefaultState(defaultState);
            }

            @Override
            protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
                for (Property prop : props) {
                    builder.add(prop);
                }
            }

            //--
            // add overrides here!

            @Override
            public BlockState getPlacementState(ItemPlacementContext ctx){
                return Utils.tryAndExecute(this.getDefaultState(),scriptContainer,"getStateOnPlace",
                        new Object[]{ctx},returnValue->{
                    try{
                        var length = returnValue.narg();
                        var stateToReturn = this.getDefaultState();
                        for(int i=0;i<length;i++){//iterate over lua table
                            for(Property prop : props){
                                if(prop.getName().equals(returnValue.get(i).get(1).toString())){
                                    switch (prop.getType().getName()) {
                                        case "java.lang.Integer" ->
                                                stateToReturn = stateToReturn.with(prop,
                                                    returnValue.get(i).get(2).checkint());
                                        case "java.lang.Boolean" ->
                                                stateToReturn = stateToReturn.with(prop,
                                                    returnValue.get(i).get(2).checkboolean());
                                        case "net.minecraft.util.math.Direction" ->
                                                stateToReturn = stateToReturn.with(prop,
                                                    Direction.byName(returnValue.get(i).get(2).toString()));
                                        //todo: add enum
                                    }
                                }
                            }
                        }
                        return stateToReturn;
                    }catch(Exception e) {
                        return this.getDefaultState();
                    }
                });
            }

            @Override//yes dont worry i already checked this is the one you need to override
            public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
                if(shapeIsScript){
                    return tryAndExecute(
                        VoxelShapes.empty(),
                        scriptContainer,
                        shapeScriptString,
                        new Object[]{state, world, pos, context},
                        value -> {
                            var toReturn = VoxelShapes.empty();

                            var size = value.length();
                            for(int i=0;i<size;i++) {
                                var currValue = value.get(i+1);
                                if (!currValue.istable() || currValue.length() == 6)
                                    continue;

                                for (int count = 1; count <= 6; count++) {//lua Poopy
                                    toReturn = VoxelShapes.union(toReturn,
                                            VoxelShapes.cuboid(
                                                    currValue.get(1).checkdouble(),
                                                    currValue.get(2).checkdouble(),
                                                    currValue.get(3).checkdouble(),
                                                    currValue.get(4).checkdouble(),
                                                    currValue.get(5).checkdouble(),
                                                    currValue.get(6).checkdouble()
                                            ));
                                }
                            }
                            return toReturn;
                        }
                    );
                }else{
                    return blockShape;
                }
            }
            @Override
            public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
                return getOutlineShape(state,world,pos,context);
            }

            @Override
            public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
                Utils.tryAndExecute(scriptContainer,"randomTick",new Object[]{state,world,pos,random});
            }

            @Override
            public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
                Utils.tryAndExecute(scriptContainer,"neighborUpdate",new Object[]{state,world,pos,sourceBlock,sourcePos,notify});
            }

            @Override
            public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
                Utils.tryAndExecute(scriptContainer,"scheduledTick",new Object[]{state,world,pos,random});
            }

            @Override
            public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
                return Utils.tryAndExecute(ActionResult.PASS,scriptContainer,"onUse",
                        new Object[]{state,world,pos,player,hand,hit},returnValue->{
                    try {
                        return ActionResult.valueOf(returnValue.toString());
                    }catch(IllegalArgumentException e) {
                        return ActionResult.PASS;
                    }
                });
            }

            @Override
            public void onBroken(WorldAccess world, BlockPos pos, BlockState state){
                Utils.tryAndExecute(scriptContainer,"onBroken",new Object[]{world,pos,state});
            }

            @Override
            public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
                Utils.tryAndExecute(scriptContainer,"onSteppedOn",new Object[]{world,pos,state,entity});
            }

            @Override
            public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
                Utils.tryAndExecute(scriptContainer,"onPlaced",new Object[]{world,pos,state,placer,itemStack});
            }
        }

        return new thisCustomBlock(settings);
    }
}
