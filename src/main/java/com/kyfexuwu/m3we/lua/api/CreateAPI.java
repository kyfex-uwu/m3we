package com.kyfexuwu.m3we.lua.api;

import com.kyfexuwu.m3we.Utils;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.JavaExclusiveTable;
import com.kyfexuwu.m3we.lua.LuaSurfaceObj;
import com.kyfexuwu.m3we.lua.UndecidedLuaFunction;
import net.minecraft.block.BlockState;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

import java.util.ArrayList;
import java.util.Locale;

public class CreateAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        JavaExclusiveTable thisApi = new JavaExclusiveTable();

        thisApi.apiMethodSet("itemStack",new itemStack(), "({item=stackItem, count=stackCount} data): " +
                "Creates a new itemstack of the type stackItem, with count stackCount. Alternatively, " +
                "if data is \"empty\" this will create an itemstack of air (empty).");
        thisApi.apiMethodSet("blockPos",MethodWrapper.inst.create((Integer x, Integer y, Integer z)->new BlockPos(x,y,z)),
                "(int x, int y, int z): " +
                "Returns a block position of (x,y,z).");
        thisApi.apiMethodSet("blockState",new blockState(), "({block=blockName, properties={...}} data): " +
                "Creates a new blockstate of block blockName, with the properties specified. To create a powered " +
                "redstone lamp with this method, you'd use blockState({block=\"minecraft:redstone_lamp\", " +
                "properties={lit=false}}).");
        thisApi.apiMethodSet("inventory",new inventory(), "(size): " +
                "Creates a new inventory with the specified size. This is useful for when trying to create a block " +
                "with an inventory, like a chest or a crafting table.");

        var entityTable = new JavaExclusiveTable();
        thisApi.javaSet("entity", entityTable);
        entityTable.apiMethodSet("item", MethodWrapper.inst.varCreate(args->{
            //world xyz stack v(xyz)
            var world = Utils.toObject(args.arg(1), World.class);
            double x = Utils.toObject(args.arg(2), Double.class);
            double y = Utils.toObject(args.arg(3), Double.class);
            double z = Utils.toObject(args.arg(4), Double.class);
            var stack = Utils.toObject(args.arg(5), ItemStack.class);

            var itemEntity = new ItemEntity(world, x, y, z, stack);;
            try{
                double vx = Utils.toObject(args.arg(6), Double.class);
                double vy = Utils.toObject(args.arg(7), Double.class);
                double vz = Utils.toObject(args.arg(8), Double.class);
                itemEntity = new ItemEntity(world, x, y, z, stack, vx, vy, vz);
            }catch(Exception ignored){}

            world.spawnEntity(itemEntity);
            return null;
        }), "(world, x, y, z, itemStack, xVelocity, yVelocity, zVelocity): " +
                "Spawns a new itemstack entity into the world with the specified position and velocity. " +
                "If the velocity is not specified, it will be given a default velocity.");

        thisApi.javaSet("fromClass",new fromClass());

        return CustomScript.finalizeAPI("Create",thisApi,env);
    }

    static class itemStack extends OneArgFunction{
        @Override
        public LuaValue call(LuaValue arg) {
            if(arg.isstring()&&arg.checkjstring().toLowerCase(Locale.ROOT).equals("empty")){
                return Utils.toLuaValue(Items.AIR.getDefaultStack());
            }

            var item = Registry.ITEM.get(new Identifier(arg.get("item").checkjstring()));
            int count=1;
            if(!arg.get("count").isnil()) count = arg.get("count").checkint();
            return Utils.toLuaValue(new ItemStack(item, count));
        }
    }
    static class blockState extends OneArgFunction{

        @Override
        public LuaValue call(LuaValue arg) {
            var val=arg.get("block");
            String str;
            if(val.isnil()){
                str="minecraft:air";
            }else{
                str=val.checkjstring();
            }
            Identifier identifier;
            if(str.contains(":"))
                identifier = new Identifier(str);
            else
                identifier = new Identifier("minecraft",str);

            var stateToReturn = Registry.BLOCK.get(identifier).getDefaultState();

            var props = stateToReturn.getProperties();
            var proposedProps  = new ArrayList<String>();

            var propTable = arg.get("properties");
            if(!propTable.isnil()) {
                Utils.forEach((LuaTable) propTable, (key, value)->{
                    proposedProps.add(key.checkjstring());
                    return null;
                });
                for (Property<?> prop : props) {
                    if (proposedProps.contains(prop.getName())) {
                        var setTo = Utils.toObject(propTable.get(prop.getName()));
                        try{
                            stateToReturn= processProp(prop, setTo, stateToReturn,
                                    setTo instanceof String && prop.getType().isEnum());
                        }catch(Exception ignored){}
                    }
                }
            }

            return Utils.toLuaValue(stateToReturn);
        }
        private static <T extends Comparable<T>> BlockState processProp(Property<T> prop, Object setTo, BlockState state,
                                                                  boolean strToEnum){
            if(strToEnum) return state.with(prop, (T) Enum.valueOf((Class<Enum>)(Object)prop.getType(), (String) setTo));
            else return state.with(prop, (T)setTo);
        }
    }
    static class inventory extends OneArgFunction{

        @Override
        public LuaValue call(LuaValue arg) {
            if(arg.checkint()<0) throw new LuaError("Size must not be less than 0!");
            return Utils.toLuaValue(new SimpleInventory(arg.checkint()));
        }
    }

    static class fromClass extends VarArgFunction{
        @Override
        public LuaValue invoke(Varargs args) {
            var clazz = (Class<?>) ((LuaSurfaceObj)args.arg(1)).object;
            var constructor = new UndecidedLuaFunction(null,clazz.getConstructors());
            return constructor.invoke(args.subargs(2)).arg(1);
        }
    }
}