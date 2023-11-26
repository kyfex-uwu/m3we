package com.kyfexuwu.m3we.mixins;

import com.kyfexuwu.m3we.CustomBlockMaker;
import com.kyfexuwu.m3we.lua.CustomBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractBlock.AbstractBlockState.class)
public abstract class HasBlockEntityMixin {
    @Shadow public abstract Block getBlock();

    @Inject(method="hasBlockEntity", at=@At("HEAD"), cancellable = true)
    public void hasBlockEntity__m3we(CallbackInfoReturnable<Boolean> cir){
        if(this.getBlock() instanceof CustomBlock){
            cir.setReturnValue(CustomBlockMaker.blockEntityScripts.containsKey(Registry.BLOCK.getId(this.getBlock())));
        }
    }
}
