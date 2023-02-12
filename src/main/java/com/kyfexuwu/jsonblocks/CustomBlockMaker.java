package com.kyfexuwu.jsonblocks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kyfexuwu.jsonblocks.lua.CustomBlock;
import com.kyfexuwu.jsonblocks.lua.CustomScript;
import com.kyfexuwu.jsonblocks.lua.LuaSurfaceObj;
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
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.LinkedList;

import static com.kyfexuwu.jsonblocks.Utils.tryAndExecute;
import static com.kyfexuwu.jsonblocks.Utils.validPropertyName;

public class CustomBlockMaker {
    public static Block from(AbstractBlock.Settings settings, JsonObject blockStates, JsonElement blockShapeJson, String scriptName) {

        class thisCustomBlock extends Block implements CustomBlock {
            public Property[] props;
            public final VoxelShape blockShape;

            public final CustomScript scriptContainer;
            public final boolean shapeIsScript;
            public final String shapeScriptString;

            public thisCustomBlock(Settings settings) {
                super(settings);

                this.scriptContainer=new CustomScript(scriptName);

                //shape
                if(blockShapeJson.isJsonArray()) {
                    var blockShapeToGive = VoxelShapes.empty();
                    for (JsonElement element : (JsonArray) blockShapeJson) {
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
                    this.blockShape = blockShapeToGive;
                    this.shapeIsScript=false;
                    this.shapeScriptString=null;
                }else{
                    this.blockShape = VoxelShapes.empty();
                    this.shapeIsScript=blockShapeJson.getAsString().startsWith("script:");
                    this.shapeScriptString=this.shapeIsScript?blockShapeJson.getAsString().substring(7):null;
                }

                //--

                var defaultState = getStateManager().getDefaultState();
                for (Property prop : this.props) {
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
                if(blockStates!=null) {
                    var propsList = new LinkedList<Property>();
                    for (String propName : blockStates.keySet()) {
                        if (!validPropertyName.matcher(propName).matches()) continue;
                        var thisState = blockStates.get(propName).getAsJsonObject();
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
                    this.props = propsList.toArray(new Property[0]);
                }else{
                    this.props= new Property[0];
                }

                for (Property prop : this.props) {
                    builder.add(prop);
                }
            }

            //--
            // add overrides here!

            @Override
            public BlockState getPlacementState(ItemPlacementContext ctx){
                scriptContainer.setSelf(this);

                return Utils.tryAndExecute(this.getDefaultState(),scriptContainer,"getStateOnPlace",
                        new Object[]{ctx},returnValue->{
                    try{
                        var length = returnValue.narg();
                        var luaKeys=((LuaTable)returnValue).keys();
                        var stateToReturn = this.getDefaultState();
                        for(int i=0;i<length;i++){//iterate over lua table
                            for(Property prop : props){
                                if(prop.getName().equals(luaKeys[i].toString())){
                                    switch (prop.getType().getName()) {
                                        case "java.lang.Integer" ->
                                                stateToReturn = stateToReturn.with(prop,
                                                    returnValue.get(luaKeys[i]).checkint());
                                        case "java.lang.Boolean" ->
                                                stateToReturn = stateToReturn.with(prop,
                                                    returnValue.get(luaKeys[i]).checkboolean());
                                        case "net.minecraft.util.math.Direction" -> {
                                            if (returnValue.typename().equals("surfaceObj")){
                                                stateToReturn = stateToReturn.with(prop,(Direction)((LuaSurfaceObj)returnValue).object);
                                            }else {
                                                stateToReturn = stateToReturn.with(prop,
                                                        Direction.byName(returnValue.get(luaKeys[i]).toString()));
                                            }
                                        }
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
                if(this.shapeIsScript){
                    scriptContainer.setSelf(this);

                    if(shapeScriptString==null) return VoxelShapes.fullCube();
                    return tryAndExecute(
                        VoxelShapes.empty(),
                        scriptContainer,
                        shapeScriptString,
                        new Object[]{state, world, pos, context},
                        value -> {
                            var toReturn = VoxelShapes.empty();
                            if(!value.istable()) return toReturn;

                            if(!value.get(1).istable()) value = new LuaTable(value);
                            //if there is only one table, use that
                            //{0,0,0,1,1,1} => {{0,0,0,1,1,1}}

                            var size = value.length();
                            for (int i = 0; i < size; i++) {
                                var currValue = value.get(i + 1);
                                if (!currValue.istable() || currValue.length() != 6)
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
                    return this.blockShape;
                }
            }
            @Override
            public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
                return getOutlineShape(state,world,pos,context);
                //todo (this is so easy) (but i do not wanna do it)
            }

            @Override
            public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
                scriptContainer.setSelf(this);
                Utils.tryAndExecute(scriptContainer,"randomTick",new Object[]{state,world,pos,random});
            }

            @Override//this is actually deprecated, might wanna look into that
            public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
                scriptContainer.setSelf(this);

                Utils.tryAndExecute(scriptContainer,"neighborUpdate",new Object[]{state,world,pos,sourceBlock,sourcePos,notify});
            }

            @Override
            public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
                scriptContainer.setSelf(this);

                Utils.tryAndExecute(scriptContainer,"scheduledTick",new Object[]{state,world,pos,random});
            }

            @Override
            public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction){
                scriptContainer.setSelf(this);
                return Utils.tryAndExecute(0,scriptContainer,"getStrongRedstonePower",
                        new Object[]{state,world,pos,direction}, LuaValue::checkint);
            }
            @Override
            public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction){
                scriptContainer.setSelf(this);

                return Utils.tryAndExecute(0,scriptContainer,"getWeakRedstonePower",
                        new Object[]{state,world,pos,direction}, LuaValue::checkint);
            }

            @Override
            public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
                scriptContainer.setSelf(this);

                return Utils.tryAndExecute(ActionResult.PASS,scriptContainer,"onUse",
                    new Object[]{state,world,pos,player,hand,hit}, returnValue->{
                        try {
                            return ActionResult.valueOf(returnValue.tojstring());
                        }catch(IllegalArgumentException e) {
                            return ActionResult.PASS;
                        }
                });
            }

            @Override
            public void onBroken(WorldAccess world, BlockPos pos, BlockState state){
                scriptContainer.setSelf(this);

                Utils.tryAndExecute(scriptContainer,"onBroken",new Object[]{world,pos,state});
            }

            @Override
            public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
                scriptContainer.setSelf(this);

                Utils.tryAndExecute(scriptContainer,"onSteppedOn",new Object[]{world,pos,state,entity});
            }

            @Override
            public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
                scriptContainer.setSelf(this);

                Utils.tryAndExecute(scriptContainer,"onPlaced",new Object[]{world,pos,state,placer,itemStack});
            }
        }

        return new thisCustomBlock(settings);
    }
}
