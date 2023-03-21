package com.kyfexuwu.jsonblocks.lua.api;

import com.kyfexuwu.jsonblocks.Utils;
import com.kyfexuwu.jsonblocks.lua.ScriptError;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.Locale;

public class CreateApi extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        APITable thisApi = new APITable();

        thisApi.set("itemStack",new itemStack());

        thisApi.locked = true;
        env.set("Create", thisApi);
        env.get("package").get("loaded").set("Create", thisApi);
        return thisApi;
    }

    public static class itemStack extends OneArgFunction{

        @Override
        public LuaValue call(LuaValue arg) {
            if(arg.isstring()&& arg.checkjstring().toLowerCase(Locale.ROOT).equals("empty")){
                return Utils.toLuaValue(new ItemStack(Blocks.AIR));
            }

            final ItemStack[] toReturn = {new ItemStack(Blocks.AIR)};
            if(arg.istable()) {
                ScriptError.execute(() -> {
                    ItemConvertible item = Registry.ITEM
                            .get(new Identifier(arg.get("item").checkjstring()));

                    //todo: nbt

                    int count=1;
                    if(!arg.get("count").isnil()) count = arg.get("count").checkint();

                    toReturn[0] = new ItemStack(item, count);
                },false);
            }

            return Utils.toLuaValue(toReturn[0]);
        }
    }
}