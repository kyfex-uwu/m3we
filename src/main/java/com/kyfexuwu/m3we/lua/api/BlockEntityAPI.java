package com.kyfexuwu.m3we.lua.api;

import com.kyfexuwu.m3we.Utils;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.JavaExclusiveTable;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

public class BlockEntityAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        JavaExclusiveTable thisApi = new JavaExclusiveTable();

        thisApi.javaSet("getEntityFromBlock", new getEntityFromPos(env));

        return CustomScript.finalizeAPI("BlockEntity",thisApi,env);
    }
    static final class getEntityFromPos extends APIFunctions.TwoArgAPIFunc {
        public getEntityFromPos(LuaValue globals) {
            super(globals);
        }

        @Override
        public LuaValue call(LuaValue positionArg, LuaValue worldArg) {
            BlockPos position = Utils.toObject(positionArg, BlockPos.class);
            World world = Utils.toObject(worldArg, World.class);
            BlockEntity toReturn = null;
            try{
                toReturn=world.getBlockEntity(position);
            }catch(Exception ignored){}
            return Utils.toLuaValue(toReturn);
        }
    }
}
