package com.kyfexuwu.m3we.lua.api;

import net.minecraft.text.ClickEvent;

public class CustomClickEvent extends ClickEvent {
    private final Runnable onClick;
    public CustomClickEvent(Runnable onClick) {
        super(null, null);
        this.onClick = onClick;
    }

    @Override
    public String getValue() {
        this.onClick.run();
        return "CUSTOM_CLICK_EVENT";
    }

    public String toString(){
        return "CustomClickEvent";
    }
}
