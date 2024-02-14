package com.kyfexuwu.m3we.lua.api;

import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.JavaExclusiveTable;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.TwoArgFunction;

import static com.kyfexuwu.m3we.lua.CustomScript.currentServer;

public class SignalsAPI extends TwoArgFunction {
    public static JavaExclusiveTable clientBus = new JavaExclusiveTable();
    public static JavaExclusiveTable serverBus = new JavaExclusiveTable();

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        JavaExclusiveTable thisApi = new JavaExclusiveTable();

        thisApi.javaSet("registerEvent",new registerEvent(env));
        //thisApi.javaSet("__eventBus", new JavaExclusiveTable());

        thisApi.javaSet("send",new send(env));

        return CustomScript.finalizeAPI("Signals",thisApi,env);
    }

    public static final Identifier signalsApiChannel = new Identifier("m3we","signals_api_message");
    static final class registerEvent extends APIFunctions.ThreeArgAPIFunc {
        public registerEvent(LuaValue globals) {
            super(globals);
        }

        @Override
        public LuaValue call(LuaValue eventName, LuaValue onClient, LuaValue onServer) {
            if(!(eventName instanceof LuaString)) throw new LuaError("Event name must be a string");
            if(!(onClient instanceof LuaFunction) || !(onServer instanceof LuaFunction))
                throw new LuaError("Event callback must be a function");

            var env = this.globals.get(CustomScript.contextIdentifier).get("env").optjstring("none");
            switch (env) {
                case "server" -> serverBus.javaSet(eventName.checkjstring(), onServer);
                case "client" -> clientBus.javaSet(eventName.checkjstring(), onClient);
                default -> {
                    return FALSE;
                }
            }

            return TRUE;
        }
    }
    static final class send extends TwoArgFunction{
        private final LuaValue globals;
        public send(LuaValue globals){
            super();
            this.globals=globals;
        }

        @Override
        public LuaValue call(LuaValue luaChannel, LuaValue luaVal) {
            var channel = luaChannel.checkjstring();
            var value = DatastoreAPI.DatastoreTable.toNBT(luaVal);
            var toSend = new NbtCompound();

            if(value.isPresent()) {
                toSend.put(channel, value.get());

                var type = this.globals.get(CustomScript.contextIdentifier).get("env");

                if(type!=null) {
                    if(type.checkjstring().equals("client"))
                        ClientPlayNetworking.send(signalsApiChannel, PacketByteBufs
                                .create()
                                .writeNbt(toSend));
                    else if(type.checkjstring().equals("server")) {
                        for(var player : currentServer.getPlayerManager().getPlayerList())
                            ServerPlayNetworking.send(player, signalsApiChannel, PacketByteBufs
                                    .create()
                                    .writeNbt(toSend));
                    }
                }
            }

            return NIL;
        }
    }
}
