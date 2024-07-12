package com.kyfexuwu.m3we.lua.dyngui;

import com.kyfexuwu.m3we.lua.ScriptError;
import com.kyfexuwu.m3we.lua.api.GuiAPI;
import com.kyfexuwu.m3we.m3weData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;

@Environment(value= EnvType.CLIENT)
public class DynamicGui extends HandledScreen<DynamicGuiHandler> {
    //todo: widget support
    // net.minecraft.client.gui.widget
    public static final Identifier TEXTURE = new Identifier("m3we","textures/gui/gui_maker.png");


    public DynamicGuiHandler handler;
    public PlayerInventory playerInventory;
    public DynamicGui(DynamicGuiHandler pHandler, PlayerInventory inventory, Text title) {
        super(pHandler, inventory, title);
        this.handler = pHandler;
        this.playerInventory = inventory;

        this.handler.addListener(new ScreenHandlerListener(){
            @Override
            public void onSlotUpdate(ScreenHandler thisHandler, int slotId, ItemStack stack) {
                thisHandler.slots.get(slotId).setStack(stack);
            }

            @Override
            public void onPropertyUpdate(ScreenHandler thisHandler, int property, int value) {
                //System.out.println(property+","+value);
            }
        });
    }

    private enum PieceType{
        TOPLEFT(0,0,4,4),
        TOP(4,0,1,4),
        TOPRIGHT(5,0,4,5),
        LEFT(0,4,4,1),
        CENTER(4,4,1,1),
        RIGHT(5,4,4,1),
        BOTTOMLEFT(0,5,4,4),
        BOTTOM(4,5,1,4),
        BOTTOMRIGHT(5,5,4,4),
        ITOPLEFT(9,0,4,4),
        ITOPRIGHT(13,0,4,4),
        IBOTTOMLEFT(9,4,4,4),
        IBOTTOMRIGHT(13,4,4,4),
        SLOT(0,14,18,18);

        public final int x;
        public final int y;
        public final int w;
        public final int h;
        PieceType(int x, int y, int w, int h){
            this.x=x;
            this.y=y;
            this.w=w;
            this.h=h;
        }
    }
    private static void drawPiece(PieceType type, MatrixStack matrices, int x, int y, int w, int h){
        DrawableHelper.drawTexture(matrices, x, y, w, h, type.x,type.y,type.w,type.h,32,32);
    }
    private static void drawPiece(PieceType type, MatrixStack matrices, int x, int y){
        drawPiece(type,matrices, x, y, type.w,type.h);
    }
    private static void drawRect(MatrixStack matrices, int x, int y, int w, int h){
        drawPiece(PieceType.TOPLEFT,matrices,x,y);
        drawPiece(PieceType.TOP,matrices,x+4,y,w-8,4);
        drawPiece(PieceType.TOPRIGHT,matrices,x+w-4,y);

        drawPiece(PieceType.LEFT,matrices,x,y+4,4,h-8);
        drawPiece(PieceType.CENTER,matrices,x+4,y+4,w-8,h-8);
        drawPiece(PieceType.RIGHT,matrices,x+w-4,y+4,4,h-8);

        drawPiece(PieceType.BOTTOMLEFT,matrices,x,y+h-4);
        drawPiece(PieceType.BOTTOM,matrices,x+4,y+h-4,w-8,4);
        drawPiece(PieceType.BOTTOMRIGHT,matrices,x+w-4,y+h-4);
    }

    static abstract class GuiComponent{
        public int x=0;
        public int y=0;
        public DynamicGui gui;
        public abstract void draw(MatrixStack matrices, int x, int y);
    }
    public static class RectGuiComponent extends GuiComponent{
        public int w;
        public int h;
        public RectGuiComponent(int x, int y, int w, int h){
            this.x=x;
            this.y=y;
            this.w=w;
            this.h=h;
        }
        public void draw(MatrixStack matrices, int x, int y){
            RenderSystem.setShaderTexture(0, TEXTURE);
            drawRect(matrices, this.x+x,this.y+y,this.w,this.h);
        }
    }
    public static class SlotGuiComponent extends GuiComponent{
        public int index;
        public boolean isPlayerInv;
        public boolean draw;
        public SlotGuiComponent(int x, int y, int index, DynamicGui gui, boolean isPlayerInv, boolean draw){
            this.x=x;
            this.y=y;
            this.index=index;
            this.gui=gui;
            this.isPlayerInv=isPlayerInv;
            this.draw=draw;
        }
        public void draw(MatrixStack matrices, int x, int y){
            if(!this.draw) return;
            RenderSystem.setShaderTexture(0, TEXTURE);
            drawPiece(PieceType.SLOT,matrices,this.x+x,this.y+y);
        }
        public void setItemPos(boolean noPlayerInv){
            ((RepositionableSlot)this.gui.handler.slots.get(this.index+(this.isPlayerInv || noPlayerInv?0:36)))
                    .setPos(this.x + 1, this.y+1);
        }
    }
    public static class TextGuiComponent extends GuiComponent{
        public String text;
        public TextGuiComponent(String text, int x, int y, DynamicGui gui){
            this.text=text;
            this.x=x;
            this.y=y;
            this.gui=gui;
        }
        public void draw(MatrixStack matrices, int x, int y){
            gui.textRenderer.draw(matrices, this.text, this.x+x,this.y+y, 0x404040);
        }
    }
    public static class ImgGuiComponent extends GuiComponent{
        public Identifier tex;
        public int tW;
        public int tH;
        public int w;
        public int h;
        public int sx;
        public int sy;
        public int sw;
        public int sh;
        public ImgGuiComponent(String texName, int tW, int tH, int x, int y, int w, int h, int sx, int sy, int sw, int sh){
            this.tex=new Identifier(texName);
            this.tW=tW;
            this.tH=tH;
            this.x=x;
            this.y=y;
            this.w=w;
            this.h=h;
            this.sx=sx;
            this.sy=sy;
            this.sw=sw;
            this.sh=sh;
        }
        public void draw(MatrixStack matrices, int x, int y){
            RenderSystem.setShaderTexture(0, this.tex);
            DrawableHelper.drawTexture(matrices,
                    this.x+x, this.y+y, this.w, this.h,
                    this.sx, this.sy, this.sw, this.sh,
                    this.tW, this.tH);
        }
    }

    public ArrayList<GuiComponent> componentsToDraw = new ArrayList<>();
    public static GuiAPI.DrawingProps props = new GuiAPI.DrawingProps();

    @Override
    public void init(){

    }

    public int mouseX;
    public int mouseY;

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        this.componentsToDraw.clear();
        this.handler.builder.drawPrep.accept(this, mouseX, mouseY);
        this.x=props.x;
        this.y=props.y;
        this.backgroundWidth=props.w;
        this.backgroundHeight=props.h;

        this.mouseX=mouseX;
        this.mouseY=mouseY;
        for(GuiComponent component : this.componentsToDraw){
            if(component instanceof SlotGuiComponent)
                ScriptError.execute(()->
                        ((SlotGuiComponent) component)
                                .setItemPos(!this.handler.builder.hasPlayerInventory));
        }
        for(GuiComponent component : this.componentsToDraw) {
            ScriptError.execute(()->component.draw(matrices,this.x,this.y));
        }

        props.slotAmt=0;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {}
}
