package com.kyfexuwu.m3we.lua.api;

import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.JavaExclusiveTable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

public class RedstoneAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        JavaExclusiveTable thisApi = new JavaExclusiveTable();
        thisApi.javaSet("getPower",MethodWrapper.create((World world, BlockPos pos, Boolean power)->
                power ? world.getReceivedStrongRedstonePower(pos) : world.getReceivedRedstonePower(pos)));
        thisApi.javaSet("getEmittedPower",MethodWrapper.create(World::getEmittedRedstonePower));

        return CustomScript.finalizeAPI("Redstone",thisApi,env);
    }
}
