package com.kyfexuwu.m3we.lua.api;

import com.kyfexuwu.m3we.Utils;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.JavaExclusiveTable;
import com.kyfexuwu.m3we.lua.LuaSurfaceObj;
import com.kyfexuwu.m3we.lua.UndecidedLuaFunction;
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

        thisApi.javaSet("itemStack",new itemStack());
        thisApi.javaSet("blockPos",MethodWrapper.create((Integer x, Integer y, Integer z)->new BlockPos(x,y,z)));
        thisApi.javaSet("blockState",new blockState());
        thisApi.javaSet("inventory",new inventory());

        var entityTable = new JavaExclusiveTable();
        thisApi.javaSet("entity", entityTable);
        entityTable.javaSet("item", MethodWrapper.varCreate(args->{
            //world xyz stack v(xyz)
            World world = Utils.toObjectTyped(args.get(0));
            double x = args.get(1).todouble();
            double y = args.get(2).todouble();
            double z = args.get(3).todouble();
            ItemStack stack = Utils.toObjectTyped(args.get(4));

            var itemEntity = new ItemEntity(world, x, y, z, stack);
            try{
                Double vx = Utils.toObjectTyped(args.get(5));
                Double vy = Utils.toObjectTyped(args.get(6));
                Double vz = Utils.toObjectTyped(args.get(7));
                itemEntity = new ItemEntity(world, x, y, z, stack, vx, vy, vz);
            }catch(Exception ignored){}

            world.spawnEntity(itemEntity);
            return null;
        }));

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
                            stateToReturn=PropertyAPI.processProp(prop, stateToReturn, setTo,
                                    setTo instanceof String && prop.getType().isEnum());
                        }catch(Exception ignored){}
                    }
                }
            }

            return Utils.toLuaValue(stateToReturn);
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