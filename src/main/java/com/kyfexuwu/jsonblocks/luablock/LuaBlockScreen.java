package com.kyfexuwu.jsonblocks.luablock;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class LuaBlockScreen extends HandledScreen<ScreenHandler>{
    private static final Identifier TEXTURE = new Identifier("m3we", "textures/gui/lua_code.png");

    public LuaBlockScreen(ScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    public int backgroundWidth = 256;
    public int backgroundHeight = 196;

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
        this.code.render(matrices, mouseX, mouseY, delta);
        drawMouseoverTooltip(matrices, mouseX, mouseY);
    }

    @Override//done
    protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
        this.textRenderer.draw(matrices, this.title, -32, -9, 4210752);
    }

    @Override
    protected void init() {
        super.init();

        this.client.keyboard.setRepeatEvents(true);
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2; //-32, -9
        this.code = new TextFieldWidget(this.textRenderer, i + 8, j + 18, 240, 170, Text.literal("container.repair"));
        this.code.setFocusUnlocked(false);
        this.code.setEditableColor(-1);
        this.code.setUneditableColor(-1);
        this.code.setMaxLength(32767);
        this.code.setText("");
        this.addSelectableChild(this.code);
        this.setInitialFocus(this.code);
        this.code.setEditable(false);

    }

    @Override
    public boolean shouldPause() {
        return true;
    }

    @Override
    public void removed() {
        super.removed();
        this.client.keyboard.setRepeatEvents(false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.client.player.closeHandledScreen();
        }
        if (this.code.keyPressed(keyCode, scanCode, modifiers) || this.code.isActive()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void handledScreenTick() {
        super.handledScreenTick();
        this.code.tick();
    }
}
