package com.kyfexuwu.m3we.lua;

import com.kyfexuwu.m3we.Utils;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.tree.RootCommandNode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class ExploreScreen extends Screen {
    public static final Identifier TEXTURE = new Identifier("m3we","textures/gui/explorer.png");

    private LuaValue value;
    protected ExploreScreen(LuaValue toExplore) {
        super(Text.empty());
        this.value=toExplore;
    }
    void stopReference(){
        this.value=null;
    }

    private record NineSlice(int x, int y, int w, int h, int cw, int ch, int ew, int eh){

        NineSlice(int x, int y,
                  int w, int h,
                  int cw, int ch){
            this(x,y,w,h,cw,ch,(w-cw)/2,(h-ch)/2);
        }

        private static int getDim(int index, int d1, int d2){
            if(index==1) return d1;
            if(index==2) return d1+d2;
            return 0;
        }
        public void draw(MatrixStack matrices,
                                 int x, int y, int w, int h,
                                 int sx, int sy){
            DrawableHelper.drawTexture(matrices, x, y, w, h,
                    this.x+getDim(sx,this.ew,this.cw),this.y+getDim(sy,this.eh,this.ch),
                    sx==1?this.cw:this.ew, sy==1?this.ch:this.eh,
                    32,32);
        }
        public void drawScaledH(MatrixStack matrices,
                         int x, int y, int h,
                         int sx, int sy){
            this.draw(matrices, x, y, sx==1?this.cw:this.ew, h, sx, sy);
        }
        public void drawScaledW(MatrixStack matrices,
                         int x, int y, int w,
                         int sx, int sy){
            this.draw(matrices, x, y, w, sy==1?this.ch:this.eh, sx, sy);
        }
        public void draw(MatrixStack matrices,
                         int x, int y,
                         int sx, int sy){
            int realW=sx==1?this.cw:this.ew;
            int realH=sy==1?this.ch:this.eh;

            DrawableHelper.drawTexture(matrices, x, y, realW, realH,
                    this.x+getDim(sx,this.ew,this.cw),this.y+getDim(sy,this.eh,this.ch),
                    realW, realH,
                    32,32);
        }
        public void drawRect(MatrixStack matrices,
                             int x, int y, int w, int h){


            this.draw(matrices, x, y, 0, 0);
            this.drawScaledW(matrices, x+this.ew, y, w-this.ew*2, 1, 0);
            this.draw(matrices, x+w-this.ew, y, 2, 0);

            int innerHeight = h-this.eh*2;
            this.drawScaledH(matrices, x, y+this.eh, innerHeight, 0,1);
            this.drawScaledH(matrices, x+w-this.ew, y+this.eh, innerHeight, 2, 1);

            this.draw(matrices, x, y+h-this.eh, 0, 2);
            this.drawScaledW(matrices, x+this.ew, y+h-this.eh, w-this.ew*2, 1, 2);
            this.draw(matrices, x+w-this.ew, y+h-this.eh, 2, 2);
        }
    }
    private final static NineSlice main = new NineSlice(0,0,18,18,8,8);
    private final static NineSlice field = new NineSlice(18,0,5,5,1,1);
    private final static NineSlice method = new NineSlice(23,0,5,5,1,1);
    private final static NineSlice textInput = new NineSlice(18,5,7,7,1,1);

    private class Node{
        public enum Type{
            FIELD,
            METHOD
        }
        public final LuaValue value;
        public Vec2f pos = new Vec2f(0,0);
        public final Type type;
        public final Node parent;
        public final HashMap<LuaValue, Node> children = new HashMap<>();

        public Node(LuaValue value, Node parent, Type type){
            this.value=value;
            this.parent=parent;
            this.type=type;
        }
        private boolean createNodeInternal(LuaValue key, LuaValue value){
            this.children.put(key, new Node(value, this,
                    value instanceof LuaFunction ? Type.METHOD : Type.FIELD));
            return false;
        }
        public void populate(){
            Utils.forEach(this.value, this::createNodeInternal);

            var size = this.children.size();
            var start = size/-2f;
            int i=0;
            for(var child : this.children.values()){
                child.pos = new Vec2f(3,(start+i)*1.2f);//3 and 1.2 can be changed
                i++;
            }
        }
        public Vec2f getAbsolutePos(){
            return this.pos.add(this.parent.pos);
        }
        public void draw(MatrixStack matrices){
            (this.type==Type.FIELD?field:method).drawRect(matrices, (int) (this.pos.x-5), (int) (this.pos.y-5), 30, 10);
        }
    }
    public class RootNode extends Node{
        public RootNode() {
            super(LuaValue.NIL, null, Type.FIELD);
        }
        public Vec2f getAbsolutePos(){
            return this.pos;
        }
    }
    private final Node root = new RootNode();

    private static void renderBG(MatrixStack matrices, int x, int y, int w, int h, Consumer<Rect2i> toDraw){
        main.drawRect(matrices, x,y,w,h);

        DrawableHelper.enableScissor(x,y,x+w,y+h);
        int adjW=w*2-w/2*2;
        int adjH=h*2-h/2*2;
        for(int cx=0;cx<=adjW/2;cx+=main.cw){
            for(int cy=0;cy+main.ch<=adjH/2;cy+=main.ch){
                main.draw(matrices,adjW/2+cx,adjH/2+cy,1,1);
                main.draw(matrices,adjW/2-cx,adjH/2+cy,1,1);
                main.draw(matrices,adjW/2+cx,adjH/2-cy,1,1);
                main.draw(matrices,adjW/2-cx-main.cw,adjH/2-cy-main.ch,1,1);
            }
        }
        toDraw.accept(new Rect2i(x+main.ew,y+main.eh,w-main.ew*2,h-main.eh*2));
        DrawableHelper.disableScissor();
    }
    private void renderInside(Rect2i dims){

    }
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);

        RenderSystem.setShaderTexture(0, TEXTURE);
        renderBG(matrices, 5, 5, this.width-10,this.height-10, this::renderInside);
    }
}
