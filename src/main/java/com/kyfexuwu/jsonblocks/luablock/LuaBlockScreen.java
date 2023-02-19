package com.kyfexuwu.jsonblocks.luablock;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class LuaBlockScreen extends HandledScreen<ScreenHandler>{
    private static final Identifier TEXTURE = new Identifier("m3we", "textures/gui/lua_code.png");

    public LuaBlockScreen(ScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    // backgroundWidth = 256;
    // backgroundHeight = 196;
    // titleX = -32;
    // titleY = -9;

    protected TextFieldWidget code;

    @Override//done
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - 256) / 2;
        int y = (height - 196) / 2;
        drawTexture(matrices, x, y, 0, 0, 256, 196);
    }

    @Override//done
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);
        drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    @Override//done
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        this.textRenderer.draw(matrices, this.title, -32, -9, 4210752);
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override//done
    public boolean shouldPause() {
        return true;
    }

    @Override//todo
    public void close() {
        this.client.player.closeHandledScreen();
        super.close();
    }
}
