package com.kyfexuwu.m3we.lua;

import com.kyfexuwu.m3we.Utils;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class ExploreScreen extends Screen {
    public static final Identifier TEXTURE = new Identifier("m3we","textures/gui/explorer.png");
    public static final Identifier TEXTURE_BG = new Identifier("m3we","textures/gui/explorer_bg.png");

    private LuaValue value;
    private final Node root;
    private Vec2f offs = new Vec2f(0,0);
    private Text tooltip = null;
    protected ExploreScreen(LuaValue toExplore) {
        super(Text.empty());
        this.value=toExplore;
        this.root = new Node(LuaString.valueOf("(root)"), this.value, null, Node.Type.FIELD);
        this.root.pos = null;
    }
    void stopReference(){
        this.value=null;
        this.root.value=null;
        this.root.clearChildren();
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

            int innerHeight = h-this.eh*2;
            int innerWidth = w-this.ew*2;
            this.draw(matrices, x, y, 0, 0);
            this.drawScaledW(matrices, x+this.ew, y, innerWidth, 1, 0);
            this.draw(matrices, x+w-this.ew, y, 2, 0);

            this.drawScaledH(matrices, x, y+this.eh, innerHeight, 0,1);
            this.draw(matrices, x+this.ew, y+this.eh, innerWidth, innerHeight, 1,1);
            this.drawScaledH(matrices, x+w-this.ew, y+this.eh, innerHeight, 2, 1);

            this.draw(matrices, x, y+h-this.eh, 0, 2);
            this.drawScaledW(matrices, x+this.ew, y+h-this.eh, innerWidth, 1, 2);
            this.draw(matrices, x+w-this.ew, y+h-this.eh, 2, 2);
        }
    }
    private final static NineSlice main = new NineSlice(0,0,18,18,8,8);
    private final static NineSlice field = new NineSlice(18,0,5,5,1,1);
    private final static NineSlice method = new NineSlice(23,0,5,5,1,1);
    private final static NineSlice textInput = new NineSlice(18,5,7,7,1,1);

    private class Node extends ButtonWidget {
        public enum Type{
            FIELD,
            METHOD
        }
        public LuaValue key;
        public LuaValue value;
        public Vec2f pos = new Vec2f(0,0);
        public final Type type;
        public final Node parent;
        public final HashMap<LuaValue, Node> children = new HashMap<>();
        private DisplayData data;

        public Node(LuaValue key, LuaValue value, Node parent, Type type){
            super(0, 0, 0, 0, Text.empty(), button -> ((Node) button).click(),
                    parent!=null?(button, matrices, mouseX, mouseY) -> {
                ExploreScreen.this.tooltip = Text.of(parent.value.get(key).tojstring());
            }:(a,b,c,d)->{});
            this.key=key;
            this.value=value;
            this.parent=parent;
            this.type=type;

            var className = this.value.typename();
            var additionalData = new ArrayList<Text>();
            if(this.value instanceof LuaSurfaceObj) className=Utils.deobfuscate(((LuaSurfaceObj)value).objClass.getSimpleName());
            if(this.value instanceof UndecidedLuaFunction){
                className="JavaMethod";

                for(var descriptor : ((UndecidedLuaFunction) value).methodDescribers()){
                    var toAdd = "("+descriptor.params().stream()
                            .map(pair->pair.getB()+" "+pair.getA())
                                    .collect(Collectors.joining(", "))+") -> "+
                            descriptor.returnClass();

                    additionalData.add(Text.of(toAdd).getWithStyle(Style.EMPTY.withItalic(true)).get(0));//bro
                }
            }

            this.data = new DisplayData(className, this.key.tojstring(), additionalData.toArray(new Text[]{}));
        }

        private void click(){
            if(!this.children.isEmpty()){
                this.clearChildren();
            }else{
                this.populate();
            }
        }

        private boolean createNodeInternal(LuaValue key, LuaValue value){
            var node = new Node(key, value, this,
                    value instanceof LuaFunction ? Type.METHOD : Type.FIELD);
            this.children.put(key, node);
            ExploreScreen.this.addDrawableChild(node);
            return true;
        }
        public void populate(){
            try{
                Utils.forEach(this.value, this::createNodeInternal);
            }catch(Exception ignored){
                for(var child : this.children.values()){
                    ExploreScreen.this.remove(child);
                }
                this.children.clear();
                return;
            }

            var xDist = this.insideSize().x+30;

            var currY=0;
            for(var child : this.children.values()){
                child.pos = new Vec2f(xDist,currY);
                currY+= (int) (child.insideSize().y+10);
            }
            currY/=-2;
            for(var child : this.children.values()){
                child.pos = child.pos.add(new Vec2f(0,currY));
            }
        }

        public Vec2f getAbsolutePos(){
            return this.pos.add(this.parent!=null?this.parent.getAbsolutePos():Vec2f.ZERO);
        }

        @Override
        public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            this.renderButton(matrices, mouseX, mouseY, delta, this.getAbsolutePos());
        }

        public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta, Vec2f offs){
            this.x= (int) offs.x;
            this.y= (int) offs.y;
            var size = this.insideSize();
            this.width= (int) (size.x+6);
            this.height= (int) (size.y+6);

            this.hovered = mouseX>=offs.x&&mouseX<offs.x+this.width&&
                    mouseY>=offs.y&&mouseY<offs.y+this.height;

            RenderSystem.setShaderTexture(0, TEXTURE);

            (this.type==Type.FIELD?field:method).drawRect(matrices, (int) offs.x, (int) offs.y, this.width, this.height);
            this.renderInsides(matrices);

            if(this.isHovered()){
                //RRAAAA
                fillGradient(matrices, (int)offs.x, (int)offs.y, (int)(offs.x + this.width), (int)(offs.y + this.height),
                        0xffffffaa, 0xffffffaa, 99999999);
                fillGradient(matrices, (int)offs.x, (int)offs.y, (int)(offs.x + this.width), (int)(offs.y + this.height),
                        0xffffffaa, 0xffffffaa, -99999999);
                this.renderTooltip(matrices, mouseX, mouseY);
            }
        }
        public void clearChildren(){
            for(var child : this.children.values()){
                child.value=null;
                child.clearChildren();
                ExploreScreen.this.remove(child);
            }
            this.children.clear();
        }

        private record DisplayData(String className, String name, Text[] additionalData){
            public DisplayData(String className, String name){
                this(className,name,new Text[]{});
            }
        }
        private void renderInsides(MatrixStack matrices){
            var renderer = ExploreScreen.this.textRenderer;
            renderer.draw(matrices, this.data.name,
                    this.x+3,this.y+3,0xffffff);
            renderer.draw(matrices, this.data.className,
                    this.x+3,this.y+3+renderer.fontHeight+2,0x000000);
            int y=2;
            for(var extra : this.data.additionalData){
                renderer.draw(matrices, extra, this.x+3, this.y+3+(renderer.fontHeight+2)*y, 0x222222);
                y++;
            }
        }
        /**
         * @return the min bounding box of the insides of this node
         * (NOT INCUDING PADDING)
         */
        private Vec2f insideSize(){
            var r = ExploreScreen.this.textRenderer;

            int maxOfExtra=Integer.MIN_VALUE;
            for(var text : this.data.additionalData) maxOfExtra = Math.max(maxOfExtra, r.getWidth(text));

            return new Vec2f(
                    Math.max(Math.max(
                            r.getWidth(this.data.name),
                            r.getWidth(this.data.className)),
                            maxOfExtra
                    ),
                    (r.fontHeight+2)*(2+this.data.additionalData.length)-2
            );
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        this.offs = this.offs.add(new Vec2f((float) -deltaX, (float) -deltaY));

        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void renderBGRect(MatrixStack matrices, int x, int y, int w, int h){
        RenderSystem.setShaderTexture(0, TEXTURE_BG);
        var tessellator = Tessellator.getInstance();
        var bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

        var scale = 0.7f;
        var startX=(int)(-w/2f+this.offs.x*scale);
        var startY=(int)(-h/2f+this.offs.y*scale);
        bufferBuilder.vertex(x+main.ew, y+h-main.eh, 0)
                .texture(startX/8f, (startY+h)/8f).next();
        bufferBuilder.vertex(x+w-main.ew, y+h-main.eh, 0)
                .texture((startX+w)/8f, (startY+h)/8f).next();
        bufferBuilder.vertex(x+w-main.ew, y+main.eh, 0)
                .texture((startX+w)/8f, startY/8f).next();
        bufferBuilder.vertex(x+main.ew, y+main.eh, 0)
                .texture(startX/8f, startY/8f).next();
        tessellator.draw();
    }
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        if(this.children().isEmpty()) this.addDrawableChild(this.root);//bad
        this.root.pos = new Vec2f(this.width/2f - this.offs.x,this.height/2f - this.offs.y);

        this.renderBackground(matrices);

        int x=5;
        int y=5;
        int w=this.width-x*2;
        int h=this.height-y*2;

        RenderSystem.setShaderTexture(0, TEXTURE);
        main.drawRect(matrices, x,y,w,h);

        DrawableHelper.enableScissor(x+main.ew,y+main.eh,x+w-main.ew,y+h-main.eh);
        this.renderBGRect(matrices, x, y, w, h);
        super.render(matrices, mouseX, mouseY, delta);
        DrawableHelper.disableScissor();

        if(this.tooltip!=null) ExploreScreen.this.renderTooltip(matrices, this.tooltip,mouseX, mouseY);
        this.tooltip=null;
    }
}
