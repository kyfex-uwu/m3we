package com.kyfexuwu.m3we.mixins;

import com.kyfexuwu.m3we.Utils;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.api.DatastoreAPI;
import com.kyfexuwu.m3we.lua.api.SignalsAPI;
import com.kyfexuwu.m3we.luablock.LuaBlock;
import com.kyfexuwu.m3we.m3we;
import com.kyfexuwu.m3we.luablock.LuaBlockEntity;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerCustomPacketMixin {
    @Inject(method = "onCustomPayload", at = @At("HEAD"))
    private void updateLuaBlock__m3we(CustomPayloadC2SPacket packet, CallbackInfo ci) {
        var channel=packet.getChannel();
        if(channel.equals(m3we.updateLuaBlockPacket)) updateLuaBlock__m3we(packet);
        else if(channel.equals(m3we.askForLuaCodePacket)) askForCode__m3we(packet);
        else if(channel.equals(SignalsAPI.signalsApiChannel)) onSignalsC2SPacket__m3we(packet);
    }

    @Unique
    private void updateLuaBlock__m3we(CustomPayloadC2SPacket packet){
        var buffer = packet.getData();

        var thisObj = (ServerPlayNetworkHandler) (Object) this;

        if (!thisObj.player.isCreativeLevelTwoOp()) return;

        NetworkThreadUtils.forceMainThread(packet, thisObj, thisObj.player.getWorld());
        var pos = buffer.readBlockPos();
        var code = buffer.readString();
        var active = buffer.readBoolean();

        var luaBlockEntity = thisObj.player.world.getBlockEntity(pos);
        if (!(luaBlockEntity instanceof LuaBlockEntity)) return;
        ((LuaBlockEntity) luaBlockEntity).setState(code, active);

        var luaBlock = thisObj.player.world.getBlockState(pos);
        if (!(luaBlock.getBlock() instanceof LuaBlock)) return;
        thisObj.player.world.setBlockState(pos, thisObj.player.world.getBlockState(pos)
                .with(LuaBlock.ACTIVE, active));
    }
    @Unique
    private void askForCode__m3we(CustomPayloadC2SPacket packet){
        var buffer = packet.getData();

        var thisObj = (ServerPlayNetworkHandler) (Object) this;

        if (!thisObj.player.isCreativeLevelTwoOp()) return;

        NetworkThreadUtils.forceMainThread(packet, thisObj, thisObj.player.getWorld());
        var pos = buffer.readBlockPos();

        var luaBlockEntity = thisObj.player.world.getBlockEntity(pos);
        if (!(luaBlockEntity instanceof LuaBlockEntity)) return;

        ServerPlayNetworking.send(thisObj.player, m3we.giveLuaCodePacket, PacketByteBufs
                .create()
                .writeBlockPos(pos)
                .writeString(((LuaBlockEntity) luaBlockEntity).getLua()));
    }
    @Unique
    private void onSignalsC2SPacket__m3we(CustomPayloadC2SPacket packet){
        var data = packet.getData().readNbt();
        try {
            var eventName = data.getKeys().stream().findFirst().get();
            var eventData = (LuaTable) DatastoreAPI.DatastoreTable.fromNBTVal(data.get(eventName), new LuaTable());
            for (var script : CustomScript.scripts) {
                try {
                    if (script.contextObj.get("env").checkjstring().equals("server")) {
                        LuaFunction eventHandler = (LuaFunction) SignalsAPI.serverBus.get(eventName);
                        if (!eventHandler.isnil()) eventHandler.invoke(Utils.cloneTable(eventData, null));
                    }
                } catch (Exception ignored) {}
            }
        }catch(Exception ignored){}
    }
}
