package com.kyfexuwu.m3we.initializers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kyfexuwu.m3we.Utils;
import com.kyfexuwu.m3we.lua.*;
import com.kyfexuwu.m3we.m3we;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.condition.MatchToolLootCondition;
import net.minecraft.loot.condition.SurvivesExplosionLootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameter;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.item.EnchantmentPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.EmptyBlockView;
import net.minecraft.world.World;
import net.minecraft.world.event.listener.GameEventListener;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.util.*;
import java.util.function.Consumer;

import static com.kyfexuwu.m3we.Utils.tryAndExecute;

public class CustomBlockMaker {
    public enum DropBehavior{
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

    public static final Map<String, LootContextParameter<?>> lootParams = Map.ofEntries(
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

    public static final HashMap<Identifier, String> blockEntityScripts = new HashMap<>();

    public static Block from(AbstractBlock.Settings settings, Block copyFrom, JsonObject blockStates,
                             JsonObject blockJson, String scriptName, String blockEntityScriptName) {
        final var blockShapeJson = blockJson.get("blockShape");
        final var outlineShapeJson = blockJson.get("outlineShape");
        var dropsSelfTemp = blockJson.get("dropsSelf");
        final var dropSelfBehavior = (dropsSelfTemp!=null&&dropsSelfTemp.isJsonPrimitive())?
                DropBehavior.get(dropsSelfTemp.getAsString()):DropBehavior.UNSPECIFIED;

        if(blockEntityScriptName!=null) {
            String namespace = "m3we";
            if (blockJson.has("namespace"))
                namespace = blockJson.get("namespace").getAsString();
            blockEntityScripts.put(new Identifier(namespace, blockJson.get("blockName").getAsString()),
                    blockEntityScriptName);
        }

        class thisCustomBlock extends Block implements CustomBlock, BlockEntityProvider {
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
                Consumer<CustomScript> clientListener = script->{
                    this.clientScriptContainer.contextObj.javaSet("env", Utils.toLuaValue("client"));
                    if(!this.clientScriptContainer.isFake)
                        this.clientScriptContainer.runEnv.set("self",new LuaSurfaceObj(this));
                };
                clientListener.accept(this.clientScriptContainer);
                this.clientScriptContainer.updateListeners.add(clientListener);

                this.serverScriptContainer=new CustomScript(scriptName);
                Consumer<CustomScript> serverListener = script->{
                    this.serverScriptContainer.contextObj.javaSet("env",Utils.toLuaValue("server"));
                    if(!this.serverScriptContainer.isFake)
                        this.serverScriptContainer.runEnv.set("self",new LuaSurfaceObj(this));
                };
                serverListener.accept(this.serverScriptContainer);
                this.serverScriptContainer.updateListeners.add(clientListener);

                //shape
                if(blockShapeJson!=null&&blockShapeJson.isJsonArray()) {
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
                    if(blockShapeJson!=null){
                        this.blockShape = VoxelShapes.empty();
                        this.shapeIsScript = blockShapeJson.getAsString().startsWith("script:");
                        this.shapeScriptString = this.shapeIsScript ? blockShapeJson.getAsString().substring(7) : null;
                    }else if(copyFrom!=null){
                        this.blockShape = copyFrom.getDefaultState()
                                .getCollisionShape(EmptyBlockView.INSTANCE,BlockPos.ORIGIN);
                        this.shapeIsScript = false;
                        this.shapeScriptString = null;
                    }else{
                        this.blockShape = VoxelShapes.empty();
                        this.shapeIsScript = false;
                        this.shapeScriptString = null;
                    }
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
                        m3we.LOGGER.error("Property " + prop.getName() + " has an invalid default value");
                    }
                }
                this.setDefaultState(defaultState);
            }

            @Override
            protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
                if(blockStates!=null) {
                    var propsList = new ArrayList<Property<?>>();
                    for (String propName : blockStates.keySet()) {
                        if (!InitUtils.validPropertyName.matcher(propName).matches()) continue;
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

                if(this.serverScriptContainer.isFake || !this.serverScriptContainer.runEnv.get("getDrops").isfunction()){
                    return super.getDroppedStacks(state, builder);
                }

                var paramsTable = new LuaTable();
                for (var entry : lootParams.entrySet()) {
                    var param = lootContext.get(entry.getValue());
                    if (param != null)
                        paramsTable.set(entry.getKey(), Utils.toLuaValue(param));
                }

                this.serverScriptContainer.setStateWorldPos(state, null, null);
                List<ItemStack> toReturn = Utils.tryAndExecute(new ArrayList<>(), this.serverScriptContainer, "getDrops",
                        new Object[]{paramsTable}, returnVal -> {
                            if (!(returnVal instanceof LuaTable)) throw new LuaError("wanted a table qwq");

                            return new ArrayList<>();
                        });
                this.serverScriptContainer.clearStateWorldPos();
                return toReturn;
            }

            @Override
            public BlockState getPlacementState(ItemPlacementContext ctx){
                var container=ctx.getWorld() instanceof ServerWorld?
                        this.serverScriptContainer : this.clientScriptContainer;
                container.setStateWorldPos(null,ctx.getWorld(),ctx.getBlockPos());

                var toReturn = Utils.tryAndExecute(this.getDefaultState(),container,"getStateOnPlace",
                        new Object[]{ctx},returnValue->{
                            try{
                                var length = returnValue.narg();
                                var luaKeys=((LuaTable)returnValue).keys();
                                var stateToReturn = this.getDefaultState();

                                for(int i=0;i<length;i++){//iterate over lua table
                                    for(Property<?> prop : props){
                                        if(prop.getName().equals(luaKeys[i].toString())){
                                            var val = Utils.toObject(returnValue.get(luaKeys[i]));
                                            processProp(prop, stateToReturn, val,
                                                    val instanceof String && prop.getType().isEnum());
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

                    this.serverScriptContainer.setStateWorldPos(state, null, pos);

                    var finalToReturn = tryAndExecute(
                        VoxelShapes.empty(),
                        this.serverScriptContainer,
                        this.outlineScriptString,
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
                    this.serverScriptContainer.clearStateWorldPos();
                    return finalToReturn;
                }else{
                    return this.outlineShape!=null ? this.outlineShape : this.getCollisionShape(state,world,pos,context);
                }
            }
            @Override
            public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
                if(this.shapeIsScript){
                    if(this.shapeScriptString==null) return VoxelShapes.fullCube();
                    this.serverScriptContainer.setStateWorldPos(state, null, pos);

                    var finalToReturn = tryAndExecute(
                            VoxelShapes.empty(),
                            this.serverScriptContainer,
                            this.shapeScriptString,
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
                    this.serverScriptContainer.clearStateWorldPos();
                    return finalToReturn;
                }else{
                    return this.blockShape;
                }
            }

            @Override
            public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
                this.serverScriptContainer.setStateWorldPos(state, world, pos);

                Utils.tryAndExecute(serverScriptContainer,"randomTick",new Object[]{state,world,pos,random});
                this.serverScriptContainer.clearStateWorldPos();
            }

            @Override
            public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
                this.clientScriptContainer.setStateWorldPos(state, world, pos);

                Utils.tryAndExecute(this.clientScriptContainer,"randomDisplayTick",new Object[]{state,world,pos,random});
                this.clientScriptContainer.clearStateWorldPos();
            }

            @Override
            public void neighborUpdate(BlockState state, World world, BlockPos pos,
                                       Block sourceBlock, BlockPos sourcePos, boolean notify) {
                var container=world instanceof ServerWorld?
                        this.serverScriptContainer : this.clientScriptContainer;

                container.setStateWorldPos(state, world, pos);

                Utils.tryAndExecute(container,"neighborUpdate",
                        new Object[]{state,world,pos,sourceBlock,sourcePos,notify});
                container.clearStateWorldPos();
            }

            @Override
            public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
                this.serverScriptContainer.setStateWorldPos(state, world, pos);

                Utils.tryAndExecute(this.serverScriptContainer,"scheduledTick",new Object[]{state,world,pos,random});
                this.serverScriptContainer.clearStateWorldPos();
            }

            @Override
            public int getStrongRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction){
                this.serverScriptContainer.setStateWorldPos(state, null, pos);

                var toReturn = Utils.tryAndExecute(0,this.serverScriptContainer,"getStrongRedstonePower",
                        new Object[]{state,world,pos,direction}, LuaValue::checkint);
                this.serverScriptContainer.clearStateWorldPos();
                return toReturn;
            }
            @Override
            public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction){
                this.serverScriptContainer.setStateWorldPos(state, null, pos);

                var toReturn = Utils.tryAndExecute(0,this.serverScriptContainer,"getWeakRedstonePower",
                        new Object[]{state,world,pos,direction}, LuaValue::checkint);
                this.serverScriptContainer.clearStateWorldPos();
                return toReturn;
            }

            @Override
            public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
                var container=world instanceof ServerWorld?
                        this.serverScriptContainer : this.clientScriptContainer;

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
                this.serverScriptContainer.setStateWorldPos(state, world, pos);

                Utils.tryAndExecute(this.serverScriptContainer,"onStateReplaced",new Object[]{state,world,pos,newState,moved});
                BlockEntity blockEntity;
                if(blockEntityScriptName!=null &&
                        (blockEntity = world.getBlockEntity(pos)) instanceof m3weBlockEntity &&
                        ((m3weBlockEntity) blockEntity).type.equals(Registry.BLOCK.getId(state.getBlock())))
                    world.removeBlockEntity(pos);
                this.serverScriptContainer.clearStateWorldPos();
            }

            @Override
            public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
                var container=world instanceof ServerWorld?
                        this.serverScriptContainer : this.clientScriptContainer;

                container.setStateWorldPos(state, world, pos);

                Utils.tryAndExecute(container,"onSteppedOn",new Object[]{state,world,pos,entity});
                container.clearStateWorldPos();
            }

            @Override
            public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
                var container=world instanceof ServerWorld?
                        this.serverScriptContainer : this.clientScriptContainer;

                container.setStateWorldPos(state, world, pos);

                Utils.tryAndExecute(container,"onPlaced",new Object[]{state,world,pos,placer,itemStack});
                container.clearStateWorldPos();
            }

            //todo: so many more methods lol

            @Nullable
            @Override
            public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
                if(blockEntityScriptName==null) return null;
                return new m3weBlockEntity(pos, state);
            }

            @Nullable
            @Override
            public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
                if(blockEntityScriptName==null || world.isClient) return null;
                return type==m3we.m3weBlockEntityType ? m3weBlockEntity::tick : null;
            }

            @Nullable
            @Override
            public <T extends BlockEntity> GameEventListener getGameEventListener(ServerWorld world, T blockEntity) {
                if(blockEntityScriptName==null || !(blockEntity instanceof m3weBlockEntity)) return null;
                return (m3weBlockEntity)blockEntity;
            }
        }

        return new thisCustomBlock(settings);
    }

    public static <T extends Comparable<T>> BlockState processProp(Property<T> prop, BlockState state,
                                                              Object setTo, boolean strToEnum){
        if(strToEnum) return state.with(prop, (T) Enum.valueOf((Class<Enum>)(Object)prop.getType(), (String) setTo));
        else return state.with(prop, (T) setTo);
    }
}
