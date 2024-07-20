package com.kyfexuwu.m3we.editor;

import com.kyfexuwu.m3we.editor.component.connection.*;
import com.kyfexuwu.m3we.editor.customblocks.Blocks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;

import java.util.ArrayList;

public class LuaEditorScreen extends Screen {
    private final ArrayList<Block> blocks  = new ArrayList<>();
    private Block focused;
    protected LuaEditorScreen(Text title) {
        super(title);

        this.blocks.add(Blocks.PRINT.create());
        this.blocks.add(Blocks.PRINT.create());
        this.blocks.add(Blocks.PRINT.create());
        this.blocks.add(Blocks.PRINT.create());
        this.blocks.add(Blocks.PLUS.create());
        this.blocks.add(Blocks.STRING.create());
        this.blocks.add(Blocks.FUNCTION.create());
        this.blocks.add(Blocks.IF.create());
    }
    public static LuaEditorScreen create(){
        return new LuaEditorScreen(Text.literal("Editor"));
    }

    @Override
    public void tick() {
        super.tick();
    }

    private float scale=0.7f;
    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);

        matrices.push();
        matrices.scale(this.scale, this.scale,1);

        if(this.clickData.block!=null){
            if(!this.clickData.disconnected) {
                var pos = this.clickData.block.getPos();
                var x = pos.x - (mouseX - this.clickData.xOffs) / this.scale;
                var y = pos.y - (mouseX - this.clickData.yOffs) / this.scale;
                var dist = Math.sqrt(x * x + y * y);
                if (dist > 5) {
                    this.clickData.block.disconnectFromParents();
                    this.clickData.disconnected = true;
                }
            }
            if(this.clickData.disconnected){
                this.clickData.block.offset(
                        (mouseX - this.clickData.xOffs) / this.scale, (mouseY - this.clickData.yOffs) / this.scale);
            }
        }
        for(var block : this.blocks ) block.draw(matrices, this.textRenderer, (int) (mouseX/scale), (int) (mouseY/scale));

        matrices.pop();
    }

    private static class ClickData{
        public double xOffs;
        public double yOffs;
        public Vec2d origPos;
        public Block block;
        public double mouseX;
        public double mouseY;
        public boolean disconnected;
        public void copy(ClickData data){
            this.xOffs=data.xOffs;
            this.yOffs=data.yOffs;
            this.origPos=data.origPos;
            this.block=data.block;
            this.mouseX=data.mouseX;
            this.mouseY=data.mouseY;
            this.disconnected=data.disconnected;
        }
        public ClickData(double xOffs, double yOffs, Vec2d origPos, Block block, double mouseX, double mouseY){
            this.xOffs=xOffs;
            this.yOffs=yOffs;
            this.origPos=origPos;
            this.block=block;
            this.mouseX=mouseX;
            this.mouseY=mouseY;
            this.disconnected=false;
        }
        public ClickData(){}
    }
    private final ClickData clickData = new ClickData();

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var oldMouseX=mouseX;
        var oldMouseY=mouseY;
        mouseX/=this.scale;
        mouseY/=this.scale;

        for(var block : this.blocks){
            if(block.mouse(mouseX, mouseY)){
                var pos = block.getPos();
                this.clickData.copy(new ClickData(
                        ((mouseX-pos.x)*this.scale), ((mouseY-pos.y)*this.scale),
                        block.offset(), block, oldMouseX, oldMouseY));
                return true;
            }
        }

        return super.mouseClicked(oldMouseX,oldMouseY,button);
    }

    private static final float radius=20;
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.focused=this.clickData.block;

        if(this.clickData.block==null) return super.mouseReleased(mouseX,mouseY,button);
        if(!this.clickData.disconnected) return true;

        if(this.clickData.mouseX==mouseX&&this.clickData.mouseY==mouseY&&
                this.clickData.block.click(mouseX/this.scale, mouseY/this.scale)){
            this.clickData.block = null;
            return true;
        }
// yeet in my fert -Annon Fullam, July 18 2024

        var possibleConnections = new ArrayList<Pair<Double, Pair<Connection, Connection>>>();

        for(var connection : this.clickData.block.connections.values()){
            if(connection instanceof SeqInConnection){
                var thisPos = connection.globalConnPos();

                for(var block : this.blocks){
                    if(block==this.clickData.block) continue;

                    for(var c2 : block.connections.values()){
                        if(!(c2 instanceof SeqOutConnection)) continue;
                        var c2Pos = c2.globalConnPos();

                        var dist=Math.sqrt((thisPos.x-c2Pos.x)*(thisPos.x-c2Pos.x)+
                                (thisPos.y-c2Pos.y)*(thisPos.y-c2Pos.y));
                        if(dist<radius)
                            possibleConnections.add(new Pair<>(dist, new Pair<>(connection,c2)));
                    }
                }
            }
            if(connection instanceof InputOutConnection){
                var thisPos = connection.globalConnPos();

                for(var block : this.blocks){
                    if(block==this.clickData.block) continue;

                    for(var c2 : block.connections.values()){
                        if(!(c2 instanceof InputInConnection ||c2 instanceof InlineInputInConnection)) continue;
                        var c2Pos = c2.globalConnPos();

                        var dist=Math.sqrt((thisPos.x-c2Pos.x)*(thisPos.x-c2Pos.x)+
                                (thisPos.y-c2Pos.y)*(thisPos.y-c2Pos.y));
                        if(dist<radius)
                            possibleConnections.add(new Pair<>(dist, new Pair<>(connection,c2)));
                    }
                }
            }
        }

        this.clickData.block = null;
        if(possibleConnections.size()==0) {
            return super.mouseReleased(mouseX, mouseY, button);
        }else{
            possibleConnections.sort((p1,p2)-> {
                var val = p1.getLeft()-p2.getLeft();
                if(val==0) return 0;
                if(val>0) return 1;
                return -1;
            });

            var pair=possibleConnections.get(0).getRight();
            pair.getLeft().connect(pair.getRight());

            return true;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(this.focused!=null){
            switch(keyCode){
                case 259 -> this.focused.keyTyped(new KeyEvent(KeyEvent.Type.BACKSPACE));
            }
        }
        return super.keyPressed(keyCode,scanCode,modifiers);
    }
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if(this.focused!=null){
            this.focused.keyTyped(new KeyEvent(chr));
        }
        return super.charTyped(chr,modifiers);
    }
}
