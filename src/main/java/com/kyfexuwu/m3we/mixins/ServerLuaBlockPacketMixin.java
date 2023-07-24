package com.kyfexuwu.m3we.mixins;

import com.kyfexuwu.m3we.luablock.LuaBlock;
import com.kyfexuwu.m3we.m3we;
import com.kyfexuwu.m3we.luablock.LuaBlockEntity;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerLuaBlockPacketMixin {
    @Inject(method = "onCustomPayload", at = @At("HEAD"))
    private void updateLuaBlock__m3we(CustomPayloadC2SPacket packet, CallbackInfo ci) {
        if(!packet.getChannel().equals(m3we.updateLuaBlockPacket)) return;
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
    @Inject(method = "onCustomPayload", at = @At("HEAD"))
    private void askForCode__m3we(CustomPayloadC2SPacket packet, CallbackInfo ci) {
        if(!packet.getChannel().equals(m3we.askForLuaCodePacket)) return;
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
}
