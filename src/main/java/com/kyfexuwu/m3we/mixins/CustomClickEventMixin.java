package com.kyfexuwu.m3we.mixins;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public class CustomClickEventMixin {
    @Inject(method="handleTextClick", at=@At(value="HEAD"), cancellable = true)
    private void customClickEventHandler(Style style, CallbackInfoReturnable<Boolean> cir) {
        if(style!=null&style.getClickEvent()!=null){//this is a kinda bruh way to to it but uwu
            if(style.getClickEvent().getValue().equals("CUSTOM_CLICK_EVENT")){
                cir.cancel();
            }
        }
    }
}
