package com.kyfexuwu.m3we.mixins;

import com.kyfexuwu.m3we.luablock.LuaBlockEntity;
import com.kyfexuwu.m3we.luablock.LuaBlockScreen;
import com.kyfexuwu.m3we.m3we;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientLuaBlockPacketMixin {
    @Shadow @Final private MinecraftClient client;

    @Shadow private ClientWorld world;

    @Inject(method = "onCustomPayload", at = @At("HEAD"))
    private void updateLuaBlock(CustomPayloadS2CPacket packet, CallbackInfo ci) {
        if(!packet.getChannel().equals(m3we.giveLuaCodePacket)) return;
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
}
