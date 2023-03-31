package com.kyfexuwu.jsonblocks.mixins;

import com.kyfexuwu.jsonblocks.JsonBlocks;
import com.kyfexuwu.jsonblocks.luablock.LuaBlockEntity;
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
        if(!packet.getChannel().equals(JsonBlocks.updateLuaBlockPacket)) return;
        var buffer = packet.getData();

        var thisObj = (ServerPlayNetworkHandler) (Object) this;

        NetworkThreadUtils.forceMainThread(packet, thisObj, thisObj.player.getWorld());
        var luaBlock = thisObj.player.world.getBlockEntity(buffer.readBlockPos());
        if(!(luaBlock instanceof LuaBlockEntity)) return;

        ((LuaBlockEntity) luaBlock).setLua(buffer.readString());
    }
}
