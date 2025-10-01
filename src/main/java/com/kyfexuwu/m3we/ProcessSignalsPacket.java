package com.kyfexuwu.m3we;

import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.api.DatastoreAPI;
import net.minecraft.network.PacketByteBuf;
import org.luaj.vm2.LuaTable;

public class ProcessSignalsPacket {
    public static void process(PacketByteBuf buffer, String desiredEnv){
        var data = buffer.readNbt();
        try {
            var eventName = data.getKeys().stream().findFirst().get();
            var eventData = DatastoreAPI.DatastoreTable.fromNBTVal(data.get(eventName), new LuaTable());
            for (var script : CustomScript.scripts) {
                try {
                    var env = script.contextObj.get("env");
                    if(env.isnil()) continue;
                    if (env.checkjstring().equals(desiredEnv)) {
                        var eventHandler = script.runEnv.get("Signals").get("__eventBus").get(desiredEnv).get(eventName);
                        if (!eventHandler.isnil()){
                            if(eventData instanceof LuaTable eventTable)
                                eventHandler.invoke(Utils.cloneTable(eventTable, null));
                            else {
                                eventHandler.invoke(eventData);
                            }
                        }
                    }
                } catch (Exception ignored) { }
            }
        }catch(Exception ignored){ }
    }
}
