package com.kyfexuwu.m3we;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kyfexuwu.m3we.lua.CustomBlock;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.DynamicEnumProperty;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.*;
import net.minecraft.loot.context.*;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.item.EnchantmentPredicate;
import net.minecraft.predicate.item.ItemPredicate;
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
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.*;

import static com.kyfexuwu.m3we.Utils.tryAndExecute;
import static com.kyfexuwu.m3we.Utils.validPropertyName;

public class CustomBlockMaker {
    private enum DropBehavior{
        UNSPECIFIED(""),
        SELF("self"),
        SILK_TOUCH("silk_touch");

        private final String name;
        DropBehavior(String name){
            this.name=name;
        }
        static DropBehavior get(String str){
            for(var val : DropBehavior.values()){
                if(val.name.equals(str)) return val;
            }
            return UNSPECIFIED;
        }
        static final LootTable selfTable;
        static final LootTable silkTouchTable;
        static{
            selfTable = new LootTable.Builder()
                    .type(LootContextTypes.BLOCK)
                    .pool(new LootPool.Builder()
                            .conditionally(SurvivesExplosionLootCondition.builder().build())
                            .with(ItemEntry.builder(Items.AIR)))
                    .build();
            silkTouchTable = new LootTable.Builder()
                    .type(LootContextTypes.BLOCK)
                    .pool(new LootPool.Builder()
                            .conditionally(SurvivesExplosionLootCondition.builder().build())
                            .conditionally(MatchToolLootCondition.builder(
                                    ItemPredicate.Builder.create().enchantment(
                                            new EnchantmentPredicate(Enchantments.SILK_TOUCH,
                                                    NumberRange.IntRange.ANY))))
                            .with(ItemEntry.builder(Items.AIR)))
                    .build();
        }
        List<ItemStack> getDrops(BlockState state, LootContext context){
            switch(this){
                case SELF -> {
                    var toReturn = new ItemStack(state.getBlock().asItem());
                    toReturn.setCount(selfTable.generateLoot(context).size());
                    return Collections.singletonList(toReturn);
                }
                case SILK_TOUCH -> {
                    var toReturn = new ItemStack(state.getBlock().asItem());
                    toReturn.setCount(silkTouchTable.generateLoot(context).size());
                    return Collections.singletonList(toReturn);
                }
                default -> { return List.of(); }
            }
        }
    }

    private static final Map<String, LootContextParameter<?>> lootParams = Map.ofEntries(
            Map.entry("thisEntity",LootContextParameters.THIS_ENTITY),
            Map.entry("lastDamagePlayer",LootContextParameters.LAST_DAMAGE_PLAYER),
            Map.entry("damageSource",LootContextParameters.DAMAGE_SOURCE),
            Map.entry("killerEntity",LootContextParameters.KILLER_ENTITY),
            Map.entry("directKillerEntity",LootContextParameters.DIRECT_KILLER_ENTITY),
            Map.entry("origin",LootContextParameters.ORIGIN),
            Map.entry("blockState",LootContextParameters.BLOCK_STATE),
            Map.entry("blockEntity",LootContextParameters.BLOCK_ENTITY),
            Map.entry("tool",LootContextParameters.TOOL),
            Map.entry("explosionRadius",LootContextParameters.EXPLOSION_RADIUS));

    public static Block from(AbstractBlock.Settings settings, JsonObject blockStates,
                             JsonObject blockJson, String scriptName) {
        final var blockShapeJson = blockJson.get("blockShape");
        final var outlineShapeJson = blockJson.get("outlineShape");
        var dropsSelfTemp = blockJson.get("dropsSelf");
        final var dropSelfBehavior = (dropsSelfTemp!=null&&dropsSelfTemp.isJsonPrimitive())?
                DropBehavior.get(dropsSelfTemp.getAsString()):DropBehavior.UNSPECIFIED;

        class thisCustomBlock extends Block implements CustomBlock {
            public Property<?>[] props;
            public final VoxelShape blockShape;
            public final VoxelShape outlineShape;

            public final CustomScript clientScriptContainer;
            public final CustomScript serverScriptContainer;
            public final boolean shapeIsScript;
            public final String shapeScriptString;
            public final String outlineScriptString;

            public thisCustomBlock(Settings settings) {
                super(settings);

                this.clientScriptContainer=new CustomScript(scriptName);
                this.serverScriptContainer=new CustomScript(scriptName);
                clientScriptContainer.contextObj.javaSet("env",Utils.toLuaValue("client"));
                serverScriptContainer.contextObj.javaSet("env",Utils.toLuaValue("server"));

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
                if(outlineShapeJson == null){
                    this.outlineShape=this.blockShape;
                    this.outlineScriptString=this.shapeScriptString;
                }else if(outlineShapeJson.isJsonArray()){
                    var outlineShapeToGive = VoxelShapes.empty();
                    for (JsonElement element : (JsonArray) outlineShapeJson) {
                        if (!element.isJsonArray())
                            continue;
                        JsonArray elementArray = element.getAsJsonArray();
                        if (elementArray.size() != 6)
                            continue;

                        outlineShapeToGive = VoxelShapes.union(outlineShapeToGive, VoxelShapes.cuboid(
                                elementArray.get(0).getAsDouble(),
                                elementArray.get(1).getAsDouble(),
                                elementArray.get(2).getAsDouble(),
                                elementArray.get(3).getAsDouble(),
                                elementArray.get(4).getAsDouble(),
                                elementArray.get(5).getAsDouble()
                        ));
                    }
                    this.outlineShape = outlineShapeToGive;
                    this.outlineScriptString=null;
                }else{
                    this.outlineShape = VoxelShapes.empty();
                    this.outlineScriptString=this.shapeScriptString;
                }

                //--

                var defaultState = this.getStateManager().getDefaultState();
                for (Property<?> prop : this.props) {
                    System.out.println(prop.getName());
                    try {
                        var jsonDefault = blockStates.get(prop.getName()).getAsJsonObject().get("default");
                        if(prop instanceof IntProperty)
                            defaultState = defaultState.with((IntProperty)prop, jsonDefault.getAsInt());
                        else if(prop instanceof BooleanProperty)
                            defaultState = defaultState.with((BooleanProperty)prop, jsonDefault.getAsBoolean());
                        else if(prop instanceof DynamicEnumProperty) {
                            var def = jsonDefault==null?
                                    blockStates.get(prop.getName()).getAsJsonObject().get("values").getAsJsonArray()
                                            .get(0).getAsString():
                                    jsonDefault.getAsString();
                            defaultState = defaultState.with((DynamicEnumProperty) prop, def);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Property " + prop.getName() + " has an invalid default value");
                    }
                }
                this.setDefaultState(defaultState);
            }

            @Override
            protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
                if(blockStates!=null) {
                    var propsList = new ArrayList<Property<?>>();
                    for (String propName : blockStates.keySet()) {
                        if (!validPropertyName.matcher(propName).matches()) continue;
                        var thisState = blockStates.get(propName).getAsJsonObject();
                        switch (thisState.get("type").getAsString()) {
                            case "int" -> propsList.add(IntProperty.of(
                                    propName,
                                    thisState.get("min").getAsInt(),
                                    thisState.get("max").getAsInt()
                            ));
                            case "boolean" -> propsList.add(BooleanProperty.of(propName));
                            case "enum" -> {
                                try {
                                    var jsonArr=thisState.get("values").getAsJsonArray();
                                    var arr=new ArrayList<String>();
                                    for(JsonElement element : jsonArr){
                                        arr.add(element.getAsString());
                                    }

                                    propsList.add(DynamicEnumProperty.of(propName,arr.toArray(new String[]{})));
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                    this.props = propsList.toArray(new Property[0]);
                }else{
                    this.props = new Property[0];
                }

                for (Property<?> prop : this.props) {
                    builder.add(prop);
                }
            }

            //--
            // add overrides here!

            @Override
            public List<ItemStack> getDroppedStacks(BlockState state, LootContext.Builder builder){
                LootContext lootContext = builder.parameter(LootContextParameters.BLOCK_STATE, state).build(LootContextTypes.BLOCK);
                if(dropSelfBehavior!=DropBehavior.UNSPECIFIED){
                    return dropSelfBehavior.getDrops(state,lootContext);
                }

                if(serverScriptContainer.isFake || !serverScriptContainer.runEnv.get("getDrops").isfunction()){
                    return super.getDroppedStacks(state, builder);
                }

                var paramsTable = new LuaTable();
                for (var entry : lootParams.entrySet()) {
                    var param = lootContext.get(entry.getValue());
                    if (param != null)
                        paramsTable.set(entry.getKey(), Utils.toLuaValue(param));
                }

                serverScriptContainer.setThis(this);
                serverScriptContainer.setStateWorldPos(state, null, null);
                List<ItemStack> toReturn = Utils.tryAndExecute(new ArrayList<>(), serverScriptContainer, "getDrops",
                        new Object[]{paramsTable}, returnVal -> {
                            if (!(returnVal instanceof LuaTable)) throw new LuaError("wanted a table qwq");

                            return new ArrayList<>();
                        });
                serverScriptContainer.clearStateWorldPos();
                return toReturn;
            }

            @Override
            public BlockState getPlacementState(ItemPlacementContext ctx){
                var container=ctx.getWorld() instanceof ServerWorld?
                        serverScriptContainer : clientScriptContainer;
                container.setThis(this);
                container.setStateWorldPos(null,ctx.getWorld(),ctx.getBlockPos());

                var toReturn = Utils.tryAndExecute(this.getDefaultState(),container,"getStateOnPlace",
                        new Object[]{ctx},returnValue->{
                            try{
                                var length = returnValue.narg();
                                var luaKeys=((LuaTable)returnValue).keys();
                                var stateToReturn = this.getDefaultState();
                                for(int i=0;i<length;i++){//iterate over lua table
                                    for(Property prop : props){//todo: redo this whole thing, it ugly
                                        if(prop.getName().equals(luaKeys[i].toString())){
                                            switch (prop.getType().getName()) {
                                                case "java.lang.Integer" ->
                                                        stateToReturn = stateToReturn.with(prop,
                                                                returnValue.get(luaKeys[i]).checkint());
                                                case "java.lang.Boolean" ->
                                                        stateToReturn = stateToReturn.with(prop,
                                                                returnValue.get(luaKeys[i]).checkboolean());
                                                case "java.lang.String" ->
                                                        stateToReturn = stateToReturn.with(prop,
                                                                returnValue.get(luaKeys[i]).checkjstring());
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
                container.clearStateWorldPos();
                return toReturn;
            }

            @Override//yes dont worry i already checked this is the one you need to override
            public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
                if(this.shapeIsScript){
                    if(this.outlineScriptString==null)
                        return this.outlineShape!=null ? this.outlineShape : this.getCollisionShape(state, world, pos, context);

                    serverScriptContainer.setThis(this);
                    serverScriptContainer.setStateWorldPos(state, null, pos);

                    var finalToReturn = tryAndExecute(
                        VoxelShapes.empty(),
                        serverScriptContainer,
                        outlineScriptString,
                        new Object[]{state, world, pos, context},
                        value -> {
                            var toReturn = VoxelShapes.empty();
                            if(!value.istable()) return toReturn;

                            if(!value.get(1).istable()) value = new LuaTable(value);

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
                    serverScriptContainer.clearStateWorldPos();
                    return finalToReturn;
                }else{
                    return this.outlineShape!=null ? this.outlineShape : this.getCollisionShape(state,world,pos,context);
                }
            }
            @Override
            public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
                if(this.shapeIsScript){
                    if(shapeScriptString==null) return VoxelShapes.fullCube();
                    serverScriptContainer.setThis(this);
                    serverScriptContainer.setStateWorldPos(state, null, pos);

                    var finalToReturn = tryAndExecute(
                            VoxelShapes.empty(),
                            serverScriptContainer,
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
                    serverScriptContainer.clearStateWorldPos();
                    return finalToReturn;
                }else{
                    return this.blockShape;
                }
            }

            @Override
            public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
                serverScriptContainer.setThis(this);
                serverScriptContainer.setStateWorldPos(state, world, pos);

                Utils.tryAndExecute(serverScriptContainer,"randomTick",new Object[]{state,world,pos,random});
                serverScriptContainer.clearStateWorldPos();
            }

            @Override
            public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
                clientScriptContainer.setThis(this);
                clientScriptContainer.setStateWorldPos(state, world, pos);

                Utils.tryAndExecute(clientScriptContainer,"randomDisplayTick",new Object[]{state,world,pos,random});
                clientScriptContainer.clearStateWorldPos();
            }

            @Override
            public void neighborUpdate(BlockState state, World world, BlockPos pos,
                                       Block sourceBlock, BlockPos sourcePos, boolean notify) {
                var container=world instanceof ServerWorld?
                        serverScriptContainer : clientScriptContainer;

                container.setThis(this);
                container.setStateWorldPos(state, world, pos);

                Utils.tryAndExecute(container,"neighborUpdate",
                        new Object[]{state,world,pos,sourceBlock,sourcePos,notify});
                container.clearStateWorldPos();
            }

            @Override
            public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
                serverScriptContainer.setThis(this);
                serverScriptContainer.setStateWorldPos(state, world, pos);

                Utils.tryAndExecute(serverScriptContainer,"scheduledTick",new Object[]{state,world,pos,random});
                serverScriptContainer.clearStateWorldPos();
            }

            @Override
            public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction){
                serverScriptContainer.setThis(this);
                serverScriptContainer.setStateWorldPos(state, null, pos);

                var toReturn = Utils.tryAndExecute(0,serverScriptContainer,"getStrongRedstonePower",
                        new Object[]{state,world,pos,direction}, LuaValue::checkint);
                serverScriptContainer.clearStateWorldPos();
                return toReturn;
            }
            @Override
            public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction){
                serverScriptContainer.setThis(this);
                serverScriptContainer.setStateWorldPos(state, null, pos);

                var toReturn = Utils.tryAndExecute(0,serverScriptContainer,"getWeakRedstonePower",
                        new Object[]{state,world,pos,direction}, LuaValue::checkint);
                serverScriptContainer.clearStateWorldPos();
                return toReturn;
            }

            @Override
            public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
                var container=world instanceof ServerWorld?
                        serverScriptContainer : clientScriptContainer;

                var toReturn = Utils.tryAndExecute(ActionResult.PASS,container,"onUse",
                        new Object[]{state,world,pos,player,hand,hit}, returnValue->{
                            try {
                                var r = Utils.toObject(returnValue);
                                if(r instanceof ActionResult) return (ActionResult) r;
                                if(r==null) return ActionResult.PASS;
                                return ActionResult.valueOf((String) r);
                            }catch(IllegalArgumentException e) {
                                return ActionResult.PASS;
                            }
                        });
                container.clearStateWorldPos();
                return toReturn;
            }

            @Override
            public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved){
                serverScriptContainer.setThis(this);
                serverScriptContainer.setStateWorldPos(state, world, pos);

                Utils.tryAndExecute(serverScriptContainer,"onStateReplaced",new Object[]{state,world,pos,newState,moved});
                serverScriptContainer.clearStateWorldPos();
            }

            @Override
            public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
                var container=world instanceof ServerWorld?
                        serverScriptContainer : clientScriptContainer;

                container.setThis(this);
                container.setStateWorldPos(state, world, pos);

                Utils.tryAndExecute(container,"onSteppedOn",new Object[]{state,world,pos,entity});
                container.clearStateWorldPos();
            }

            @Override
            public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
                var container=world instanceof ServerWorld?
                        serverScriptContainer : clientScriptContainer;

                container.setThis(this);
                container.setStateWorldPos(state, world, pos);

                Utils.tryAndExecute(container,"onPlaced",new Object[]{state,world,pos,placer,itemStack});
                container.clearStateWorldPos();
            }
        }

        return new thisCustomBlock(settings);
    }
}
