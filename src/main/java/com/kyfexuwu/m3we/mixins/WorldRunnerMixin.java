package com.kyfexuwu.m3we.mixins;

import com.kyfexuwu.m3we.lua.WorldRunner;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class WorldRunnerMixin extends World {
    protected WorldRunnerMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, RegistryEntry<DimensionType> dimension, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates) throws Exception {
        super(properties, registryRef, dimension, profiler, isClient, debugWorld, seed, maxChainedNeighborUpdates);
        throw new Exception("NUH UH");
    }

    @Unique
    private final List<WorldRunner> worldRunners = new ArrayList<>();

    @Inject(at=@At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiler/Profiler;swap(Ljava/lang/String;)V",
            args = "ldc=raid"), method="tick")
    private void tick__m3we(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        if(this.worldRunners != null) {
            for (var runner : this.worldRunners) {
                this.getProfiler().swap("m3we-worldRunner:" + runner.name);
                runner.tick(true);
            }
        }
    }
}
