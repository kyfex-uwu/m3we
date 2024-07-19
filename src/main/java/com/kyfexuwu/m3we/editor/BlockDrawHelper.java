package com.kyfexuwu.m3we.editor;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Matrix4f;

import java.util.function.Consumer;

public class BlockDrawHelper {
    public static class Context{
        private final BufferBuilder builder;
        private final Matrix4f matrix;
        private final Color color;
        private Context(BufferBuilder builder, Matrix4f matrix, Color color){
            this.builder=builder;
            this.matrix=matrix;
            this.color=color;
        }
        public void vertex(double x, double y){
            this.color.setColor(this.builder.vertex(this.matrix, (float)x, (float)y, 0.0f)).next();
        }
    }
    public static void vertexes(MatrixStack matrices, Color color, Consumer<Context>... funcs){
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        for(var func : funcs){
            bufferBuilder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            func.accept(new Context(bufferBuilder, matrices.peek().getPositionMatrix(), color));
            BufferRenderer.drawWithShader(bufferBuilder.end());
        }
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }
}
