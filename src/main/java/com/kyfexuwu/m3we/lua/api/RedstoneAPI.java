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
        thisApi.apiMethodSet("getPower",MethodWrapper.inst.create((World world, BlockPos pos, Boolean power)->
                power ? world.getReceivedStrongRedstonePower(pos) : world.getReceivedRedstonePower(pos)),
                        "([J]World blockWorld, [J]BlockPos blockPos, Enums.RedstonePower powerType): " +
                        "Gets the redstone level of the block in blockWorld at blockPos, strong or weak depending on " +
                        "powerType. Note: powerType STRONG will only give strong power, but powerType WEAK will return " +
                        "the highest power (strong or weak).");
        thisApi.apiMethodSet("getEmittedPower",MethodWrapper.inst.create(World::getEmittedRedstonePower),
                "([J]World blockWorld, [J]BlockPos blockPos, Enums.Direction powerDirec): " +
                        "Gets the redstone level that the block in blockWorld at blockPos is emitting from direction " +
                        "powerDirec.");

        return CustomScript.finalizeAPI("Redstone",thisApi,env);
    }
}
