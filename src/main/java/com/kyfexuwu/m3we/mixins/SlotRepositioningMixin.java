package com.kyfexuwu.m3we.mixins;

import com.kyfexuwu.m3we.lua.dyngui.RepositionableSlot;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Slot.class)
public abstract class SlotRepositioningMixin implements RepositionableSlot {
    @Shadow @Final @Mutable
    public int x;
    @Shadow @Final @Mutable
    public int y;

    @Override
    public void setPos(int x, int y) {
        this.x=x;
        this.y=y;
    }
}