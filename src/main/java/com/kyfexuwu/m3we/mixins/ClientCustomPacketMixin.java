package com.kyfexuwu.m3we.mixins;

import com.kyfexuwu.m3we.Utils;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.api.DatastoreAPI;
import com.kyfexuwu.m3we.lua.api.SignalsAPI;
import com.kyfexuwu.m3we.luablock.LuaBlockEntity;
import com.kyfexuwu.m3we.luablock.LuaBlockScreen;
import com.kyfexuwu.m3we.m3we;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientCustomPacketMixin {
    @Shadow @Final private MinecraftClient client;

    @Shadow private ClientWorld world;

    @Inject(method = "onCustomPayload", at = @At("HEAD"))
    private void customPayload__m3we(CustomPayloadS2CPacket packet, CallbackInfo ci) {
        var channel = packet.getChannel();
        if(channel.equals(m3we.giveLuaCodePacket)) onLuaCodePacket__m3we(packet);
        else if(channel.equals(SignalsAPI.signalsApiChannel)) onSignalsS2CPacket__m3we(packet);
    }

    @Unique
    private void onLuaCodePacket__m3we(CustomPayloadS2CPacket packet){
        var buffer = packet.getData();

        var thisObj = (ClientPlayNetworkHandler) (Object) this;

        NetworkThreadUtils.forceMainThread(packet, thisObj, this.client);
        var pos = buffer.readBlockPos();

        var luaBlockEntity = this.world.getBlockEntity(pos);
        if (!(luaBlockEntity instanceof LuaBlockEntity)) return;

        var code=buffer.readString();
        ((LuaBlockEntity) luaBlockEntity).setScript(code);
        if(this.client.currentScreen instanceof LuaBlockScreen){
            ((LuaBlockScreen) this.client.currentScreen).code=code;
            ((LuaBlockScreen) this.client.currentScreen).updateCode();
        }
    }

    @Unique
    private void onSignalsS2CPacket__m3we(CustomPayloadS2CPacket packet){
        var data = packet.getData();
        var eventName = data.readString();
        var eventData = (LuaTable) DatastoreAPI.DatastoreTable.fromNBTVal(data.readNbt(), new LuaTable());
        for(var script : CustomScript.scripts){
            try {
                if (script.contextObj.get("env").checkjstring().equals("client")) {
                    LuaFunction eventHandler =
                            (LuaFunction) script.runEnv.get("Signals").get("__eventBus").get(eventName);
                    if(!eventHandler.isnil()) eventHandler.invoke(Utils.cloneTable(eventData,null));
                }
            }catch(Exception ignored){}
        }
    }
}
