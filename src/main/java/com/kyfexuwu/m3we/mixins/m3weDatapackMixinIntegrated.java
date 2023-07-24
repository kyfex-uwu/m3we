package com.kyfexuwu.m3we.mixins;

import com.kyfexuwu.m3we.m3we;
import com.kyfexuwu.m3we.m3weData;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackProvider;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.server.integrated.IntegratedServerLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.io.File;
import java.util.Arrays;

@Mixin(IntegratedServerLoader.class)
public class m3weDatapackMixinIntegrated {
    @ModifyArg(
            method = "createDataPackManager",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/resource/ResourcePackManager;<init>(" +
                            "Lnet/minecraft/resource/ResourceType;" +
                            "[Lnet/minecraft/resource/ResourcePackProvider;)V"
            ),
            index = 1)
    private static ResourcePackProvider[] addm3weDataPackIntegrated__m3we(ResourcePackProvider[] packs) {
        File resourcesFolder = new File(m3we.JBFolder.getAbsolutePath()+"\\resources");
        resourcesFolder.mkdir();

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
