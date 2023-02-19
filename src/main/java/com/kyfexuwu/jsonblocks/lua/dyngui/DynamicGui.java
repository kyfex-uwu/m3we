package com.kyfexuwu.jsonblocks.lua.dyngui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DynamicGui extends HandledScreen<ScreenHandler> {
    private static final Identifier TEXTURE = new Identifier("m3we", "textures/gui/gui_maker.png");

    public DynamicGuiHandler handler;
    public DynamicGui(ScreenHandler pHandler, PlayerInventory inventory, Text title) {
        super(pHandler, inventory, title);
        this.handler = (DynamicGuiHandler) pHandler;

        this.handler.addListener(new ScreenHandlerListener(){
            @Override
            public void onSlotUpdate(ScreenHandler handler2, int slotId, ItemStack stack) {
                System.out.println(stack.getName());
            }

            @Override
            public void onPropertyUpdate(ScreenHandler handler2, int property, int value) {
                System.out.println(property+","+value);
            }
        });
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (this.width-this.handler.gui.getLeft().width)/2;
        int y = (this.height-this.handler.gui.getLeft().height)/2;

        for(DynamicGuiBuilder.GuiRect rect : this.handler.gui.getLeft().rects){
            drawRect(matrices, x+rect.x,y+rect.y,rect.w,rect.h);
        }
        for(Pair<Integer, Integer> slot : this.handler.gui.getLeft().slots){
            drawPiece(PieceType.SLOT, matrices, x+slot.getLeft(), y+slot.getRight());
        }
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
    private void drawPiece(PieceType type, MatrixStack matrices, int x, int y, int w, int h){
        DrawableHelper.drawTexture(matrices, x, y, w, h, type.x,type.y,type.w,type.h,32,32);
    }
    private void drawPiece(PieceType type, MatrixStack matrices, int x, int y){
        drawPiece(type,matrices, x, y, type.w,type.h);
    }
    private void drawRect(MatrixStack matrices, int x, int y, int w, int h){
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

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    @Override
    public Optional<Element> hoveredElement(double mouseX, double mouseY) {
        return super.hoveredElement(mouseX, mouseY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void setInitialFocus(@Nullable Element element) {
        super.setInitialFocus(element);
    }

    @Override
    public void focusOn(@Nullable Element element) {
        super.focusOn(element);
    }

    @Override
    public boolean changeFocus(boolean lookForwards) {
        return super.changeFocus(lookForwards);
    }
}
