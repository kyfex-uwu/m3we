package com.kyfexuwu.jsonblocks.mixins;

import com.kyfexuwu.jsonblocks.JsonBlocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.resource.ClientBuiltinResourcePackProvider;
import net.minecraft.resource.*;
import net.minecraft.resource.metadata.PackResourceMetadata;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.Arrays;

@Mixin(MinecraftClient.class)
public class m3weResourceMixin {
    private static ResourcePack m3weResourcePack;

    //i would be surprised if there werent bugs in here
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

    @Inject(method = "<init>", at = @At("TAIL"))
    private void lookAtDirectories(RunArgs args, CallbackInfo info) {
        System.out.println("args.directories.runDir: "+args.directories.runDir.getAbsolutePath());
        System.out.println("args.directories.getResourceIndex(): "+args.directories.getResourceIndex());
    }
}
