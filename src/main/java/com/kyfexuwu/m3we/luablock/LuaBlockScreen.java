package com.kyfexuwu.m3we.luablock;

import com.kyfexuwu.m3we.m3we;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
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
    private static final Identifier TEXTURE = new Identifier("m3we","textures/gui/lua_code.png");
    private static final Text title = Text.translatable("m3we.lua_script");

    final BlockPos pos;
    public String code;
    String[] formattedCode;
    boolean active;

    public LuaBlockScreen(BlockPos pos, boolean active) {
        super(title);
        this.pos=pos;
        this.code="loading...";
        this.active=active;
        this.updateCode();

        ClientPlayNetworking.send(m3we.askForLuaCodePacket, PacketByteBufs
                .create()
                .writeBlockPos(pos));
    }

    public void updateCode(){
        this.formattedCode = Arrays.stream(code.split("\n")).toArray(String[]::new);
    }
    Pair<Integer, Integer> getXY(int charPos){
        if(charPos<0) return new Pair<>(0,0);

        int x=0;
        int y=0;

        int charCount=0;
        for(;y<this.formattedCode.length;y++){

            var thisLength=this.formattedCode[y].length()+1;
            if(charPos>=charCount&&charPos<charCount+thisLength){
                x=charPos-charCount;
                break;
            }
            charCount+=thisLength;
        }
        return new Pair<>(x,y);
    }
    void setCursor(int pos, boolean isSelecting){
        this.cursorPos=Math.max(Math.min(pos,this.code.length()),0);
        var y = this.getXY(this.cursorPos-1).getRight();

        if(this.scroll<y-LINES_AMT+1) this.scroll=y-LINES_AMT+1;
        else if(this.scroll>y) this.scroll=y;

        if(!isSelecting) this.selectionPos = this.cursorPos;
    }
    void moveCursor(int move, boolean isSelecting){
        setCursor(this.cursorPos+move, isSelecting);
    }
    void write(String toWrite){
        var selection = this.getSelection();
        this.code=this.code.substring(0,selection.getLeft())+toWrite+this.code.substring(selection.getRight());
        this.moveCursor(toWrite.length(), false);

        this.updateCode();
    }
    Pair<Integer, Integer> getSelection(){
        var toReturn = new Pair<>(this.cursorPos,this.selectionPos);
        if(this.cursorPos>this.selectionPos){
            toReturn.setRight(this.cursorPos);
            toReturn.setLeft(this.selectionPos);
        }
        return toReturn;
    }
    void delete(int starting, int ending){
        if(starting<0) starting=0;
        if(ending>=this.code.length()) ending=this.code.length();
        this.code=this.code.substring(0,starting)+this.code.substring(ending);
        this.updateCode();

        this.cursorPos=starting;
        this.selectionPos=starting;
    }

    public int backgroundWidth = 256;
    public int backgroundHeight = 200;

    int cursorPos = 0;
    int selectionPos = 0;
    float blinkInterval = 0;
    int scroll = 0;
    final static int LINES_AMT =19;

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.renderBackground(matrices);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        drawTexture(matrices, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight);
        var inButton=(mouseX-x>=240&&mouseX-x<=252&&mouseY-y>=2&&mouseY-y<=14);
        drawTexture(matrices, x+241, y+3, this.active?0:12, inButton?212:200, 12, 12);

        this.textRenderer.draw(matrices, this.getTitle(), x + 8, y + 6, 0x404040);

        var selection = this.getSelection();
        var sStart = selection.getLeft();
        var sEnd = selection.getRight();
        var charCount=0;
        for(int before=0;before<this.scroll;before++)
            charCount+=this.formattedCode[before].length()+1;
        for(int line=this.scroll;line<this.formattedCode.length;line++) {
            if(line>=this.scroll+LINES_AMT) break;
            var thisY=this.textRenderer.fontHeight * (line-this.scroll);

            var lineLength = this.formattedCode[line].length()+1;

            var localSStart=0;
            var localSEnd=-1;
            if(sStart>=charCount&&sStart<charCount+lineLength)
                localSStart=this.textRenderer.getWidth(this.formattedCode[line].substring(0,sStart-charCount));
            if(sEnd>=charCount&&sEnd<charCount+lineLength)
                localSEnd=this.textRenderer.getWidth(this.formattedCode[line].substring(0,sEnd-charCount));
            if(localSEnd==-1) localSEnd=this.textRenderer.getWidth(this.formattedCode[line]);

            if(charCount+lineLength>sStart&&charCount<=sEnd)
                DrawableHelper.fill(matrices,x+10+localSStart,y+20+thisY,x+10+localSEnd,y+20+thisY+this.textRenderer.fontHeight,0xff008800);

            this.textRenderer.draw(matrices, this.textRenderer.trimToWidth(this.formattedCode[line],236),
                    x + 10, y + 20 + thisY, 0xffffff);
            charCount+=lineLength;
        }

        this.blinkInterval+=delta;
        if(this.blinkInterval>=16) this.blinkInterval%=16;
        if(this.blinkInterval<8) {
            var cursorXY = this.getXY(this.cursorPos);
            if (cursorXY.getRight() < this.formattedCode.length &&
                    cursorXY.getRight()>=this.scroll&&cursorXY.getRight()<this.scroll+LINES_AMT) {
                this.textRenderer.draw(matrices, "|",
                        x + 10 - 1 + this.textRenderer.getWidth(this.formattedCode[cursorXY.getRight()].substring(0, cursorXY.getLeft())),
                        y + 20 + this.textRenderer.fontHeight * (cursorXY.getRight() - this.scroll),
                        0x00ff00);
            }
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
        if (SharedConstants.isValidChar(chr)) {
            this.write(Character.toString(chr));
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.client.keyboard.setRepeatEvents(false);
            var toSend=PacketByteBufs.create()
                    .writeBlockPos(this.pos)
                    .writeString(this.code);
            toSend.writeBoolean(this.active);
            ClientPlayNetworking.send(m3we.updateLuaBlockPacket,toSend);

            var blockEntity=this.client.player.world.getBlockEntity(this.pos);
            if(blockEntity instanceof LuaBlockEntity) {
                ((LuaBlockEntity) blockEntity).setState(this.code, this.active);
            }
            var luaBlock = this.client.player.world.getBlockState(this.pos);
            if(luaBlock.getBlock() instanceof LuaBlock) {
                this.client.player.world.setBlockState(pos, this.client.player.world.getBlockState(pos)
                        .with(LuaBlock.ACTIVE, this.active));
            }

            this.client.player.closeHandledScreen();
            return true;
        }

        if (Screen.isSelectAll(keyCode)) {
            this.setCursor(Integer.MAX_VALUE, false);
            this.selectionPos=0;
            return true;
        }
        if (Screen.isCopy(keyCode)) {
            var selection = this.getSelection();
            MinecraftClient.getInstance().keyboard.setClipboard(this.code.substring(selection.getLeft(), selection.getRight()));
            return true;
        }
        if (Screen.isPaste(keyCode)) {
            this.write(MinecraftClient.getInstance().keyboard.getClipboard());
            return true;
        }
        if (Screen.isCut(keyCode)) {
            var selection = this.getSelection();
            MinecraftClient.getInstance().keyboard.setClipboard(this.code.substring(selection.getLeft(), selection.getRight()));
            this.write("");
            this.setCursor(selection.getLeft(),false);
            return true;
        }
        switch (keyCode) {
            case 257 -> {
                this.write("\n");
                return true;
            }
            case 263 -> {
                this.moveCursor(-1, Screen.hasShiftDown());
                return true;
            }
            case 262 -> {
                this.moveCursor(1, Screen.hasShiftDown());
                return true;
            }
            case 265 -> {
                var pos = this.getXY(this.cursorPos);
                if (pos.getRight() <= 0) {
                    this.setCursor(0, Screen.hasShiftDown());
                    return true;
                }
                var newPos = this.textRenderer.trimToWidth(
                        this.formattedCode[pos.getRight() - 1],
                        this.textRenderer.getWidth(this.formattedCode[pos.getRight()].substring(0, pos.getLeft()))
                ).length();
                for (int i = 0; i < pos.getRight() - 1; i++)
                    newPos += this.formattedCode[i].length() + 1;
                this.setCursor(newPos, Screen.hasShiftDown());
                return true;
            }
            case 264 -> {
                var pos = this.getXY(this.cursorPos);
                if (pos.getRight() >= this.formattedCode.length - 1) {
                    this.setCursor(Integer.MAX_VALUE, Screen.hasShiftDown());
                    return true;
                }
                var newPos = this.textRenderer.trimToWidth(
                        this.formattedCode[pos.getRight() + 1],
                        this.textRenderer.getWidth(this.formattedCode[pos.getRight()].substring(0, pos.getLeft()))
                ).length();
                for (int i = 0; i < pos.getRight() + 1; i++)
                    newPos += this.formattedCode[i].length() + 1;
                this.setCursor(newPos, Screen.hasShiftDown());
                return true;
            }
            case 259 -> {
                if (this.cursorPos != this.selectionPos) {
                    var selection = this.getSelection();
                    this.delete(selection.getLeft(), selection.getRight());
                } else {
                    this.delete(this.cursorPos - 1, this.cursorPos);
                }
                return true;
            }
            case 261 -> {
                if (this.cursorPos != this.selectionPos) {
                    var selection = this.getSelection();
                    this.delete(selection.getLeft(), selection.getRight());
                } else {
                    this.delete(this.cursorPos, this.cursorPos + 1);
                }
                return true;
            }
            case 268 -> {
                this.moveCursor(0, Screen.hasShiftDown());
                return true;
            }
            case 269 -> {
                this.moveCursor(Integer.MAX_VALUE, Screen.hasShiftDown());
                return true;
            }
        }

        return true;
    }

    private int mouseEventPos(double adjMouseX, double adjMouseY){
        int newPos=0;
        int y = (int) (adjMouseY/this.textRenderer.fontHeight)+this.scroll;
        int end = Math.min(this.formattedCode.length,y);
        for(int i=0;i<end;i++) {
            newPos += this.formattedCode[i].length()+1;
        }
        if(y<this.formattedCode.length)
            newPos+=this.textRenderer.trimToWidth(this.formattedCode[y],(int) adjMouseX).length();

        return Math.min(newPos,this.code.length());
    }
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var adjMouseX = mouseX-(this.width-this.backgroundWidth)/2-8;
        var adjMouseY = mouseY-(this.height-this.backgroundHeight)/2-18;

        if(adjMouseX+8>=240&&adjMouseX+8<=252&&adjMouseY+18>=2&&adjMouseY+18<=14){
            this.active=!this.active;

            return true;
        }
        if(adjMouseX<0||adjMouseY<0||adjMouseX>this.backgroundWidth-16||adjMouseY>this.backgroundHeight-26) return false;

        this.setCursor(this.mouseEventPos(adjMouseX,adjMouseY), Screen.hasShiftDown());
        return true;
    }
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        var adjMouseX = mouseX-(this.width-this.backgroundWidth)/2-8;
        var adjMouseY = mouseY-(this.height-this.backgroundHeight)/2-18;

        if(adjMouseX<0||adjMouseY<0||adjMouseX>this.backgroundWidth-16||adjMouseY>this.backgroundHeight-26) return false;

        this.selectionPos = this.mouseEventPos(adjMouseX,adjMouseY);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount){
        this.scroll-=amount;
        if (this.scroll+LINES_AMT>this.formattedCode.length) this.scroll = this.formattedCode.length-LINES_AMT;
        if(this.scroll<0) this.scroll=0;
        return true;
    }
}
