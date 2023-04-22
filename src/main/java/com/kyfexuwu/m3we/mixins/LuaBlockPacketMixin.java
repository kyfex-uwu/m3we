package com.kyfexuwu.m3we.mixins;

import com.kyfexuwu.m3we.m3we;
import com.kyfexuwu.m3we.luablock.LuaBlockEntity;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class LuaBlockPacketMixin {
    @Inject(method = "onCustomPayload(Lnet/minecraft/network/packet/c2s/play/CustomPayloadC2SPacket;)V", at = @At("HEAD"))
    private void injected(CustomPayloadC2SPacket packet, CallbackInfo ci) {
        if(!packet.getChannel().equals(m3we.updateLuaBlockPacket)) return;
        var buffer = packet.getData();

        var thisObj = (ServerPlayNetworkHandler) (Object) this;

        if(!thisObj.player.isCreativeLevelTwoOp()) return;

        NetworkThreadUtils.forceMainThread(packet, thisObj, thisObj.player.getWorld());
        var luaBlock = thisObj.player.world.getBlockEntity(buffer.readBlockPos());
        if(!(luaBlock instanceof LuaBlockEntity)) return;

        ((LuaBlockEntity) luaBlock).setLua(buffer.readString());
    }
}
