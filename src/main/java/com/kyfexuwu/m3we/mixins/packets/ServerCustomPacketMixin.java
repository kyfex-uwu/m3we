package com.kyfexuwu.m3we.mixins.packets;

import com.kyfexuwu.m3we.ProcessSignalsPacket;
import com.kyfexuwu.m3we.lua.api.SignalsAPI;
import com.kyfexuwu.m3we.luablock.LuaBlock;
import com.kyfexuwu.m3we.luablock.LuaBlockEntity;
import com.kyfexuwu.m3we.m3we;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerCustomPacketMixin {
    @Inject(method = "onCustomPayload", at = @At("HEAD"), cancellable = true)
    private void updateLuaBlock__m3we(CustomPayloadC2SPacket packet, CallbackInfo ci) {
        var channel=packet.getChannel();
        var buffer = packet.getData();
        var shouldCancel=false;
        var thisObj = (ServerPlayNetworkHandler) (Object) this;
        NetworkThreadUtils.forceMainThread(packet, thisObj, thisObj.player.getWorld());

        if(channel.equals(m3we.updateLuaBlockPacket)){
            shouldCancel=true;
            updateLuaBlock__m3we(buffer);
        }else if(channel.equals(m3we.askForLuaCodePacket)) {
            shouldCancel = true;
            askForCode__m3we(buffer);
        }
        else if(channel.equals(SignalsAPI.signalsApiChannel)) {
            shouldCancel = true;
            onSignalsC2SPacket__m3we(buffer);
        }
        if(shouldCancel) ci.cancel();
    }

    @Unique
    private void updateLuaBlock__m3we(PacketByteBuf buffer){
        var thisObj = (ServerPlayNetworkHandler) (Object) this;
        if (!thisObj.player.isCreativeLevelTwoOp()) return;

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
    private void askForCode__m3we(PacketByteBuf buffer){
        var thisObj = (ServerPlayNetworkHandler) (Object) this;
        if (!thisObj.player.isCreativeLevelTwoOp()) return;

        var pos = buffer.readBlockPos();

        var luaBlockEntity = thisObj.player.world.getBlockEntity(pos);
        if (!(luaBlockEntity instanceof LuaBlockEntity)) return;

        ServerPlayNetworking.send(thisObj.player, m3we.giveLuaCodePacket, PacketByteBufs
                .create()
                .writeBlockPos(pos)
                .writeString(((LuaBlockEntity) luaBlockEntity).getLua()));
    }
    @Unique
    private void onSignalsC2SPacket__m3we(PacketByteBuf buffer){
        ProcessSignalsPacket.process(buffer, "server");
    }
}
