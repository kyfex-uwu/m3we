package com.kyfexuwu.m3we.lua.api;

import com.kyfexuwu.m3we.Utils;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.ArrayList;
import java.util.Locale;

public class CreateApi extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        APITable thisApi = new APITable();

        thisApi.set("itemStack",new itemStack());
        thisApi.set("blockPos",new blockPos());
        thisApi.set("blockState",new blockState());

        thisApi.locked = true;
        env.set("Create", thisApi);
        env.get("package").get("loaded").set("Create", thisApi);
        return thisApi;
    }

    public static class itemStack extends OneArgFunction{

        @Override
        public LuaValue call(LuaValue arg) {
            if(arg.isstring()&&arg.checkjstring().toLowerCase(Locale.ROOT).equals("empty")){
                return Utils.toLuaValue(new ItemStack(Blocks.AIR));
            }

            var item = Registry.ITEM.get(new Identifier(arg.get("item").checkjstring()));
            int count=1;
            if(!arg.get("count").isnil()) count = arg.get("count").checkint();
            return Utils.toLuaValue(new ItemStack(item, count));
        }
    }
    public static class blockPos extends ThreeArgFunction {
        @Override
        public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
            return Utils.toLuaValue(new BlockPos(x.checkint(), y.checkint(), z.checkint()));
        }
    }
    public static class blockState extends OneArgFunction{

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
                LuaValue k = LuaValue.NIL;
                while (true) {
                    Varargs n = propTable.next(k);
                    if ((k = n.arg1()).isnil())
                        break;
                    proposedProps.add(k.checkjstring());
                }
                for (Property prop : props.toArray(Property[]::new)) {
                    if (proposedProps.contains(prop.getName())) {
                        var setTo = (Object) Utils.toObject(propTable.get(prop.getName()));
                        if (setTo instanceof Double)
                            stateToReturn = stateToReturn.with(prop, ((Double) setTo).intValue());
                        if (setTo instanceof Boolean)
                            stateToReturn = stateToReturn.with(prop, (Boolean) setTo);
                        if (setTo instanceof String)
                            stateToReturn = stateToReturn.with(prop, (String) setTo);
                    }
                }
            }

            return Utils.toLuaValue(stateToReturn);
        }
    }
}