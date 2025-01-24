package com.kyfexuwu.m3we.mixins.packs;

import com.kyfexuwu.m3we.m3we;
import com.kyfexuwu.m3we.m3weData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackProvider;
import net.minecraft.resource.ResourcePackSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Arrays;

@Mixin(MinecraftClient.class)
public abstract class m3weResourcePackMixin {
    @ModifyArg(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/resource/ResourcePackManager;<init>("+
                            "Lnet/minecraft/resource/ResourcePackProfile$Factory;"+
                            "[Lnet/minecraft/resource/ResourcePackProvider;)V"
            ),
            index = 1)
    private ResourcePackProvider[] addm3weResourcePack__m3we(ResourcePackProvider[] packs) {
        ResourcePackProvider[] toReturn = Arrays.copyOf(packs,packs.length+1);
        toReturn[packs.length]= (profileAdder, factory) -> {
            ResourcePackProfile resourcePackProfile = ResourcePackProfile.of(
                    m3we.MOD_ID,
                    true,
                    () -> m3weData.resourcePack,
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
