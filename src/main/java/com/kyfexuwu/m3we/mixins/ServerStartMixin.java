package com.kyfexuwu.m3we.mixins;

import com.kyfexuwu.m3we.lua.CustomScript;
import com.mojang.datafixers.DataFixer;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.SaveLoader;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.util.ApiServices;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.Proxy;

@Mixin(MinecraftServer.class)
public abstract class ServerStartMixin {
    @Inject(method="<init>", at=@At("RETURN"))
    public void saveCurrentServer__m3we(Thread serverThread, LevelStorage.Session session,
                                        ResourcePackManager dataPackManager, SaveLoader saveLoader,
                                        Proxy proxy, DataFixer dataFixer, ApiServices apiServices,
                                        WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory,
                                        CallbackInfo ci){
        CustomScript.currentServer=(MinecraftServer)(Object)this;
    }
}
