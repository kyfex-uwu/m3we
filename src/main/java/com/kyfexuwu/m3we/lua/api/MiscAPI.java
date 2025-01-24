package com.kyfexuwu.m3we.lua.api;

import com.kyfexuwu.m3we.Utils;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.JavaExclusiveTable;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

public class MiscAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        JavaExclusiveTable thisApi = new JavaExclusiveTable();

        thisApi.javaSet("runCommand",new runCommand(env));

        return CustomScript.finalizeAPI("Misc",thisApi,env);
    }

    public static class runCommand extends TwoArgFunction{
        private final LuaValue globals;
        public runCommand(LuaValue globals){
            super();
            this.globals=globals;
        }

        @Override
        public LuaValue call(LuaValue luaCommand, LuaValue luaWorld) {
            var command = luaCommand.checkjstring();
            var context = this.globals.get(CustomScript.contextIdentifier);

            World world = (World) Utils.toObject(luaWorld);
            if(world==null) world = (World) Utils.toObject(context.get("world"));
            if(world==null) throw new LuaError("World not found");
            if(!(world instanceof ServerWorld)) throw new LuaError("Running commands is only allowed on the server");

            Vec3i pos = (Vec3i) Utils.toObject(context.get("blockPos"));
            if(pos==null) pos = world.getSpawnPos();

            //todo: replace dummy
            world.getServer().getCommandManager().executeWithPrefix(new ServerCommandSource(
                    CommandOutput.DUMMY,
                    new Vec3d(pos.getX(), pos.getY(), pos.getZ()),
                    Vec2f.ZERO,
                    (ServerWorld) world,
                    2,
                    "[Lua Script]",
                    Text.of("[Lua Script]"),
                    world.getServer(),
                    null
            ), command);

            return NIL;
        }
    }
}
