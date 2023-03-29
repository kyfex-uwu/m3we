package com.kyfexuwu.jsonblocks.luablock;

import com.kyfexuwu.jsonblocks.JsonBlocks;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;

public class LuaBlockScreen extends Screen {
    private static final Identifier TEXTURE = new Identifier("m3we", "textures/gui/lua_code.png");
    private static final Text title = Text.translatable("m3we.lua_script");

    final BlockPos pos;
    String code;
    Text[] formattedCode;

    public LuaBlockScreen(BlockPos pos, String initialCode) {
        super(title);
        this.pos=pos;
        this.code=initialCode;
        this.updateCode();
    }

    void updateCode(){
        this.formattedCode = Arrays.stream(code.split("\n")).map((str) -> Text.of(str)).toArray(Text[]::new);
    }
    void setCursor(int pos){
        this.cursorPos=Math.max(Math.min(pos,this.code.length()),0);
    }
    void moveCursor(int move){
        setCursor(this.cursorPos+move);
    }
    void write(String toWrite){
        var selection = this.getSelection();
        this.code=this.code.substring(0,selection.getLeft())+toWrite+this.code.substring(selection.getRight());
        this.moveCursor(toWrite.length());

        this.updateCode();
    }
    Pair<Integer, Integer> getSelection(){
        var toReturn = new Pair<>(this.cursorPos,this.selectionPos);
        if(this.selectionPos == -1) toReturn.setRight(this.cursorPos);
        else if(this.cursorPos<this.selectionPos){
            toReturn.setRight(this.cursorPos);
            toReturn.setLeft(this.selectionPos);
        }

        return toReturn;
    }

    public int backgroundWidth = 256;
    public int backgroundHeight = 196;

    int cursorPos = 0;
    int selectionPos = -1;
    float blinkInterval = 0;

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.renderBackground(matrices);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        drawTexture(matrices, x, y, 0, 0, 256, 196);

        this.textRenderer.draw(matrices, this.getTitle(), x + 8, y + 6, 0x404040);

        this.blinkInterval+=delta;
        if(this.blinkInterval>=16) this.blinkInterval%=16;
        int charCount=0;
        boolean renderedCursor=false;
        for(int line=0;line<this.formattedCode.length;line++) {
            //this.formattedCode[line].getStyle().getColor().getRgb();
            this.textRenderer.draw(matrices, this.formattedCode[line], x + 10, y + 20 + this.textRenderer.fontHeight * line, 0xffffff);
            if(renderedCursor||this.blinkInterval<8) continue;

            var s = this.formattedCode[line].getString();
            var l = s.length();
            if (charCount + l >= this.cursorPos) {
                this.textRenderer.draw(matrices, "|",
                        x+10+this.textRenderer.getWidth(s.substring(0, this.cursorPos - charCount))-1,
                        y + 20 + this.textRenderer.fontHeight * line,
                        0xffffff
                );
                renderedCursor=true;
            }
            charCount += l+1;
        }
    }

    @Override
    protected void init() {
        super.init();

        this.client.keyboard.setRepeatEvents(true);
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (SharedConstants.isValidChar(chr)||chr=='\n') {
            this.write(Character.toString(chr));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.client.keyboard.setRepeatEvents(false);
            ClientPlayNetworking.send(JsonBlocks.updateLuaBlockPacket,
                    PacketByteBufs.create().writeBlockPos(this.pos).writeString(this.code));
            var blockEntity=this.client.player.world.getBlockEntity(this.pos);
            if(blockEntity instanceof LuaBlockEntity)
                ((LuaBlockEntity) this.client.player.world.getBlockEntity(this.pos)).setLua(this.code);
            this.client.player.closeHandledScreen();
            return true;
        }

        //this.selecting = Screen.hasShiftDown();
        if (Screen.isSelectAll(keyCode)) {
            this.moveCursor(Integer.MAX_VALUE);
            this.selectionPos=0;
            return true;
        }
        if (Screen.isCopy(keyCode)) {
            //MinecraftClient.getInstance().keyboard.setClipboard(this.getSelectedText());
            return true;
        }
        if (Screen.isPaste(keyCode)) {
            this.write(MinecraftClient.getInstance().keyboard.getClipboard());
            return true;
        }
        if (Screen.isCut(keyCode)) {
            //MinecraftClient.getInstance().keyboard.setClipboard(this.getSelectedText());
            this.write("");
            return true;
        }
        //System.out.println(keyCode);
        switch (keyCode) {
            case 257: {
                this.write("\n");
                return true;
            }
            case 263: {
                this.moveCursor(-1);
                return true;
            }
            case 262: {
                this.moveCursor(1);
                return true;
            }
            case 265: {
                //todo
            }
            case 264: {
                //todo
            }
            case 259: {
                if(this.cursorPos<=0) return true;
                this.code = this.code.substring(0,this.cursorPos-1)+this.code.substring(this.cursorPos);
                this.moveCursor(-1);
                this.updateCode();
                return true;
            }
            case 261: {
                if(this.cursorPos>=this.code.length()) return true;
                this.code = this.code.substring(0,this.cursorPos)+this.code.substring(this.cursorPos+1);
                this.updateCode();
                return true;
            }
            case 268: {
                this.moveCursor(0);
                return true;
            }
            case 269: {
                this.moveCursor(Integer.MAX_VALUE);
                return true;
            }
        }

        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var adjMouseX = mouseX-(this.width-this.backgroundWidth)/2-8;
        var adjMouseY = mouseY-(this.height-this.backgroundHeight)/2-18;

        if(adjMouseX<0||adjMouseY<0||adjMouseX>this.backgroundWidth-16||adjMouseY>this.backgroundHeight-26) return false;

        int newPos=0;
        int y = (int) (adjMouseY/this.textRenderer.fontHeight);
        int end = Math.min(this.formattedCode.length,y);
        for(int i=0;i<end;i++) {
            newPos += this.formattedCode[i].getString().length()+1;
        }
        if(y<this.formattedCode.length)
            newPos+=this.textRenderer.trimToWidth(this.formattedCode[y],(int) adjMouseX).getString().length();

        this.setCursor(newPos);

        return true;
     }
}
