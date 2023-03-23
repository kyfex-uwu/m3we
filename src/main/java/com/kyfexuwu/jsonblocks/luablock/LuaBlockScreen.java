package com.kyfexuwu.jsonblocks.luablock;

import com.kyfexuwu.jsonblocks.JsonBlocks;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class LuaBlockScreen extends Screen {
    private static final Identifier TEXTURE = new Identifier("m3we", "textures/gui/lua_code.png");
    private static final Text title = Text.translatable("m3we.lua_script");

    final BlockPos pos;

    public LuaBlockScreen(BlockPos pos) {
        super(title);
        this.pos=pos;
    }

    public int backgroundWidth = 256;
    public int backgroundHeight = 196;

    protected TextFieldWidget code;

    public void renderBackground(MatrixStack matrices) {
        super.renderBackground(matrices);

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (this.width - 256) / 2;
        int y = (this.height - 196) / 2;
        drawTexture(matrices, x, y, 0, 0, 256, 196);

        this.textRenderer.draw(matrices, this.getTitle(), x + 8, y + 6, 0x404040);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    protected void init() {
        super.init();

        this.client.keyboard.setRepeatEvents(true);
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2; //-32, -9\

        this.code = new MultilineTextWidget(this.textRenderer, i + 8, j + 18, 240, 170, Text.literal("test"));
        this.code.setFocusUnlocked(false);
        this.code.setEditableColor(-1);
        this.code.setUneditableColor(-1);
        this.code.setMaxLength(32767);
        this.addSelectableChild(this.code);
        this.setInitialFocus(this.code);
        this.code.setEditable(true);

        this.addDrawable(this.code);
    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.client.keyboard.setRepeatEvents(false);
            ClientPlayNetworking.send(JsonBlocks.updateLuaBlockPacket,
                    PacketByteBufs.create().writeBlockPos(this.pos).writeString(this.code.getText()));
            this.client.player.closeHandledScreen();
        }
        if (this.code.keyPressed(keyCode, scanCode, modifiers) || this.code.isActive()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
