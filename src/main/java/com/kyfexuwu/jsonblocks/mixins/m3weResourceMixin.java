package com.kyfexuwu.jsonblocks.mixins;

import com.kyfexuwu.jsonblocks.JsonBlocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.io.File;
import java.util.Arrays;

@Mixin(MinecraftClient.class)
public class m3weResourceMixin {
    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/resource/ResourcePackManager;<init>("+
                                "Lnet/minecraft/resource/ResourcePackProfile$Factory;"+
                                "[Lnet/minecraft/resource/ResourcePackProvider;)V"
            ),
            index = 1)
    private ResourcePackProvider[] addm3weResourcePack(ResourcePackProvider[] packs) {
        File resourcesFolder = new File(JsonBlocks.JBFolder.getAbsolutePath()+"\\resources");
        resourcesFolder.mkdir();

        ResourcePackProvider[] toReturn = Arrays.copyOf(packs,packs.length+1);
        toReturn[packs.length]= (profileAdder, factory) -> {
            ResourcePackProfile resourcePackProfile = ResourcePackProfile.of(
                    JsonBlocks.MOD_ID,
                    true,
                    () -> JsonBlocks.m3weResourcePack,
                    factory,
                    ResourcePackProfile.InsertionPosition.BOTTOM,
                    ResourcePackSource.PACK_SOURCE_BUILTIN
            );
            if (resourcePackProfile != null) {
                profileAdder.accept(resourcePackProfile);
            }
        };

        return toReturn;
    }
}
