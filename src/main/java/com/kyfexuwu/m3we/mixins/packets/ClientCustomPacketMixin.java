package com.kyfexuwu.m3we.mixins.packets;

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
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import org.luaj.vm2.LuaTable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientCustomPacketMixin {
    @Shadow @Final private MinecraftClient client;

    @Shadow private ClientWorld world;

    @Inject(method = "onCustomPayload", at = @At("HEAD"), cancellable = true)
    private void customPayload__m3we(CustomPayloadS2CPacket packet, CallbackInfo ci) {
        var channel = packet.getChannel();
        var buffer = packet.getData();
        var shouldCancel=false;
        var thisObj = (ClientPlayNetworkHandler) (Object) this;
        NetworkThreadUtils.forceMainThread(packet, thisObj, this.client);

        if(channel.equals(m3we.giveLuaCodePacket)){
            shouldCancel=true;
            onLuaCodePacket__m3we(buffer);
        }else if(channel.equals(SignalsAPI.signalsApiChannel)){
            shouldCancel=true;
            onSignalsS2CPacket__m3we(buffer);
        }
        if(shouldCancel) ci.cancel();
    }

    @Unique
    private void onLuaCodePacket__m3we(PacketByteBuf buffer){
        var pos = buffer.readBlockPos();

        var luaBlockEntity = this.world.getBlockEntity(pos);
        if (!(luaBlockEntity instanceof LuaBlockEntity)) return;

        var code=buffer.readString();
        ((LuaBlockEntity) luaBlockEntity).setScript(code, true);
        if(this.client.currentScreen instanceof LuaBlockScreen){
            ((LuaBlockScreen) this.client.currentScreen).code=code;
            ((LuaBlockScreen) this.client.currentScreen).updateCode();
        }
    }

    @Unique
    private void onSignalsS2CPacket__m3we(PacketByteBuf buffer){
        var data = buffer.readNbt();
        try {
            var eventName = data.getKeys().stream().findFirst().get();
            var eventData = DatastoreAPI.DatastoreTable.fromNBTVal(data.get(eventName), new LuaTable());
            for (var script : CustomScript.scripts) {
                try {
                    var env = script.contextObj.get("env");
                    if(env.isnil()) continue;
                    if (env.checkjstring().equals("client")) {
                        var eventHandler = script.runEnv.get("Signals").get("__eventBus").get("client").get(eventName);
                        if (!eventHandler.isnil()){
                            if(eventData instanceof LuaTable eventTable)
                                eventHandler.invoke(Utils.cloneTable(eventTable, null));
                            else
                                eventHandler.invoke(eventData);
                        }
                    }
                } catch (Exception ignored) { }
            }
        }catch(Exception ignored){ }
    }
}
