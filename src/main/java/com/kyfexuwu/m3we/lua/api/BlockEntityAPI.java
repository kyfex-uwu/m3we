package com.kyfexuwu.m3we.lua.api;

import com.kyfexuwu.m3we.Utils;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.JavaExclusiveTable;
import com.kyfexuwu.m3we.lua.LuaSurfaceObj;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.TwoArgFunction;

import static com.kyfexuwu.m3we.lua.CustomScript.contextIdentifier;

public class BlockEntityAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        JavaExclusiveTable thisApi = new JavaExclusiveTable();

        thisApi.javaSet("getEntityFromBlock", new getEntityFromPos(env));

        return CustomScript.finalizeAPI("BlockEntity",thisApi,env);
    }
    static final class getEntityFromPos extends APIFunctions.VarArgAPIFunc {
        public getEntityFromPos(LuaValue globals) {
            super(globals);
        }

        @Override
        public Varargs invoke(Varargs args) {
            var arg1=args.arg(1);
            var arg2=args.arg(2);
            if(arg1 instanceof LuaSurfaceObj && ((LuaSurfaceObj) arg1).objClass == BlockPos.class){
                var ctx=this.globals.get(contextIdentifier);
                return Utils.toLuaValue(((World) Utils.toObject(ctx.get("world")))
                        .getBlockEntity((BlockPos) Utils.toObject(arg1)));
            }
            return NONE;
        }
    }
}
