package com.kyfexuwu.m3we.mixins;

import com.kyfexuwu.m3we.editor.LuaEditorScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.apache.commons.lang3.NotImplementedException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class PauseMenuMixin extends Screen {
    protected PauseMenuMixin(Text title) {
        super(title);
        throw new NotImplementedException("do not");
    }

    @Inject(method = "initWidgets", at=@At("RETURN"))
    public void initWidgets__m3we(CallbackInfo ci){
        this.addDrawableChild(new ButtonWidget(
                this.width / 2 - 102, this.height / 4 + 144 + -16, 204, 20,
                Text.literal("Open Lua Editor"), button -> {
            this.client.setScreen(LuaEditorScreen.create());
        }));
    }
}
