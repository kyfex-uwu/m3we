package com.kyfexuwu.jsonblocks.lua.dyngui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;

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
    private ArrayList<GuiRect> rects = new ArrayList<>();

    public DynamicGuiHandler build(int syncId, PlayerInventory inventory, PlayerEntity player){
        return new DynamicGuiHandler(syncId, inventory, rects.toArray(new GuiRect[]{}));
    }

    public DynamicGuiBuilder addRect(int x, int y, int w, int h){
        for(GuiRect rect : this.rects){
            if(rect.x==x&&rect.w==w) {
                rect.y=Math.min(rect.y,y);
                rect.h=rect.y-Math.max(rect.y+rect.h,y+h);
                return this;
            }
            if(rect.y==y&&rect.h==h) {
                rect.x=Math.min(rect.x,x);
                rect.w=rect.x-Math.max(rect.x+rect.w,x+w);
                return this;
            }
        }

        rects.add(new GuiRect(x,y,w,h));
        return this;
    }
}
