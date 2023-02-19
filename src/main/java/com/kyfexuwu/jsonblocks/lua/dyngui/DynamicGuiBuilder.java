package com.kyfexuwu.jsonblocks.lua.dyngui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Pair;

import java.util.ArrayList;

public class DynamicGuiBuilder {
    static class GuiRect{
        public int x;
        public int y;
        public int w;
        public int h;
        public GuiRect(int x, int y, int w, int h){
            this.x=x;
            this.y=y;
            this.w=w;
            this.h=h;
        }
    }
    public ArrayList<GuiRect> rects = new ArrayList<>();
    public ArrayList<Pair<Integer, Integer>> slots = new ArrayList<>();

    public int width=-1;
    public int height=-1;
    public DynamicGuiBuilder(int width, int height){
        setBounds(width, height);
    }
    public DynamicGuiBuilder(){}

    public DynamicGuiBuilder setBounds(int width, int height){
        this.width=Math.max(width,0);
        this.height=Math.max(height,0);
        return this;
    }

    public DynamicGuiHandler build(int syncId, PlayerInventory inventory, PlayerEntity player, String guiName){
        if(this.width==-1){
            int w=0;
            for(GuiRect rect : rects)
                if(rect.x+rect.w>w) w=rect.x+rect.w;
            this.width=w;
        }
        if(this.height==-1){
            int h=0;
            for(GuiRect rect : rects)
                if(rect.y+rect.h>h) h=rect.y+rect.h;
            this.width=h;
        }
        return new DynamicGuiHandler(syncId, inventory, guiName);
    }

    public DynamicGuiBuilder addRect(int x, int y, int w, int h){
        rects.add(new GuiRect(x,y,w,h));
        return this;
    }
    public DynamicGuiBuilder addSlot(int x, int y){
        slots.add(new Pair<>(x,y));
        return this;
    }
}
