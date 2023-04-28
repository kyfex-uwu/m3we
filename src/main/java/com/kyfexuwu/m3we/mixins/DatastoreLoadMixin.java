package com.kyfexuwu.m3we.mixins;

import com.kyfexuwu.m3we.lua.api.DatastoreAPI;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.Executor;

@Mixin(ServerWorld.class)
public class DatastoreLoadMixin {
    @Inject(method="<init>", at=@At(value = "RETURN"))
    private void saveMixin(MinecraftServer server, Executor workerExecutor, LevelStorage.Session session,
                           ServerWorldProperties properties, RegistryKey worldKey, DimensionOptions dimensionOptions,
                           WorldGenerationProgressListener worldGenerationProgressListener, boolean debugWorld,
                           long seed, List spawners, boolean shouldTickTime, CallbackInfo ci) {
        var thisObj = (ServerWorld) (Object) this;

        thisObj.getPersistentStateManager().getOrCreate(
                DatastoreAPI.DatastoreManager::fromNBT,
                DatastoreAPI.DatastoreManager::new,
                "m3we_datastore");
    }
}
