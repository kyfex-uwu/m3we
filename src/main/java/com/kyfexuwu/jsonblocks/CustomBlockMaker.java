package com.kyfexuwu.jsonblocks;

import com.google.gson.JsonObject;
import com.kyfexuwu.jsonblocks.lua.CustomScript;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaValue;

import java.rmi.registry.Registry;
import java.util.LinkedList;

import static com.kyfexuwu.jsonblocks.Utils.validName;

public class CustomBlockMaker {
    public static JsonObject tempBlockStates;//prolly can be fixed

    public static Block from(FabricBlockSettings settings, JsonObject blockStates, CustomScript scriptContainer) {
        tempBlockStates = blockStates;

        class customBlock extends Block {

            public static final LinkedList<Property> propsList = new LinkedList<>();
            public static final Property[] props;

            static {
                for (String propName : tempBlockStates.keySet()) {
                    if (!validName.matcher(propName).matches()) continue;
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
                        case "enum":
                            //propsList.add(IntProperty.of("test", 0, 3));
                            break;
                        case "direction":
                            propsList.add(DirectionProperty.of(propName));
                            break;
                    }
                }
                props = propsList.toArray(new Property[0]);
                propsList.clear();
            }

            public customBlock(Settings settings) {
                super(settings);

                var defaultState = getStateManager().getDefaultState();
                for (Property prop : props) {
                    try {
                        var jsonDefault = blockStates.get(prop.getName()).getAsJsonObject().get("default");
                        switch (prop.getType().getName()) {
                            case "java.lang.Integer" -> defaultState = defaultState.with(prop, jsonDefault.getAsInt());
                            case "java.lang.Boolean" -> defaultState = defaultState.with(prop, jsonDefault.getAsBoolean());
                            case "net.minecraft.util.math.Direction" -> defaultState = defaultState.with(prop,
                                    Direction.byName(jsonDefault.getAsString()));
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

            @Override
            public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
                if(scriptContainer==null) return ActionResult.PASS;

                var onUseFunc = scriptContainer.runEnv.get("onUse");
                if(onUseFunc.isnil())
                    return ActionResult.PASS;

                onUseFunc.invoke(new LuaValue[]{
                        Utils.cleanValue(state),
                        Utils.cleanValue(world),
                        Utils.cleanValue(pos),
                        Utils.cleanValue(player),
                        Utils.cleanValue(hand),
                        Utils.cleanValue(hit)
                });
                return ActionResult.PASS;
            }

            @Override
            public void onBroken(WorldAccess world, BlockPos pos, BlockState state){
                if(scriptContainer==null) return;
                var onBrokenFunc = scriptContainer.runEnv.get("onBroken");
                if(onBrokenFunc.isnil())
                    return;

                onBrokenFunc.invoke(new LuaValue[]{
                        Utils.cleanValue(world),
                        Utils.cleanValue(pos),
                        Utils.cleanValue(state)
                });
            }

            @Override
            public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
                if(scriptContainer==null) return;
                var onSteppedOnFunc = scriptContainer.runEnv.get("onSteppedOn");
                if(onSteppedOnFunc.isnil())
                    return;

                onSteppedOnFunc.invoke(new LuaValue[]{
                        Utils.cleanValue(world),
                        Utils.cleanValue(pos),
                        Utils.cleanValue(state),
                        Utils.cleanValue(entity)
                });
            }

            @Override
            public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
                if(scriptContainer==null) return;
                var onPlacedFunc = scriptContainer.runEnv.get("onPlaced");
                if(onPlacedFunc.isnil())
                    return;

                onPlacedFunc.invoke(new LuaValue[]{
                        Utils.cleanValue(world),
                        Utils.cleanValue(pos),
                        Utils.cleanValue(state),
                        Utils.cleanValue(placer),
                        Utils.cleanValue(itemStack)
                });
            }
        }

        return new customBlock(settings);
    }
}
