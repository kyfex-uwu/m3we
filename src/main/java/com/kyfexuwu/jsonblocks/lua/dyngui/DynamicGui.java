package com.kyfexuwu.jsonblocks.lua.dyngui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class DynamicGui extends HandledScreen<ScreenHandler> {
    private static final Identifier TEXTURE = new Identifier("m3we", "textures/gui/gui_maker.png");

    public DynamicGui(ScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        //todo, get rendering from handler
    }

    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - 256) / 2;
        int y = (height - 196) / 2;

        drawPiece(PieceType.TOPLEFT,matrices,x,y);
        for(int i=1;i<=10;i++)
            drawPiece(PieceType.TOP,matrices,x+i*4,y);
        drawPiece(PieceType.TOPRIGHT,matrices,x+11*4,y);
        for(int i=1;i<=5;i++) {
            drawPiece(PieceType.LEFT,matrices,x,y+i*4);
            for (int j = 1; j <= 10; j++)
                drawPiece(PieceType.CENTER,matrices,x+j*4,y+i*4);
            drawPiece(PieceType.RIGHT,matrices,x+11*4,y+i*4);
        }
        drawPiece(PieceType.BOTTOMLEFT,matrices,x,y+6*4);
        for(int i=1;i<=10;i++)
            drawPiece(PieceType.BOTTOM,matrices,x+i*4,y+6*4);
        drawPiece(PieceType.BOTTOMRIGHT,matrices,x+11*4,y+6*4);
    }

    private enum PieceType{
        TOPLEFT(0,0),
        TOP(4,0),
        TOPRIGHT(8,0),
        LEFT(0,4),
        CENTER(4,4),
        RIGHT(8,4),
        BOTTOMLEFT(0,8),
        BOTTOM(4,8),
        BOTTOMRIGHT(8,8),
        ITOPLEFT(12,0),
        ITOPRIGHT(16,0),
        IBOTTOMLEFT(12,4),
        IBOTTOMRIGHT(16,4),
        INVSPOT(0,14,18,18);

        public final int x;
        public final int y;
        public final int w;
        public final int h;
        PieceType(int x, int y){
            this.x=x;
            this.y=y;
            this.w=4;
            this.h=4;
        }
        PieceType(int x, int y, int w, int h){
            this.x=x;
            this.y=y;
            this.w=w;
            this.h=h;
        }
    }
    private void drawPiece(PieceType type, MatrixStack matrices, int x, int y){
        DrawableHelper.drawTexture(matrices, x, y, 4,4,type.x,type.y,type.w,type.h,32,32);
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

    //--

    //public ScreenHandler getHandler
}
