package com.kyfexuwu.m3we.editor;

import com.kyfexuwu.m3we.editor.component.blueprint.Blueprint;
import com.kyfexuwu.m3we.editor.component.blueprint.TextBlueprint;
import com.kyfexuwu.m3we.editor.component.connection.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;

import java.util.ArrayList;

public class LuaEditorScreen extends Screen {
    private final ArrayList<Block> blocks  = new ArrayList<>();
    protected LuaEditorScreen(Text title) {
        super(title);

        this.blocks.add(new BlockOptions(BlockOptions.Type.SEQ).color(new Color(66, 152, 245))
                .appendRow(BlockOptions.fromArr(new Blueprint[]{
                        Blueprint.SOLID, Blueprint.input("first"), new TextBlueprint("testing"),
                        Blueprint.input("second"), Blueprint.input("third"),
                })).create());
        this.blocks.add(new BlockOptions(BlockOptions.Type.NONE).color(new Color(50,0,50))
                .appendRow(BlockOptions.fromArr(new Blueprint[]{
                        Blueprint.input("only"), new TextBlueprint(":3")
                })).create());
        this.blocks.add(new BlockOptions(BlockOptions.Type.SEQ).color(new Color(255,0,0))
                .appendRow(BlockOptions.fromArr(new Blueprint[]{
                        new TextBlueprint("print"), Blueprint.input("toPrint")
                })).create());
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
            this.clickData.block.offset(
                    (mouseX-this.clickData.xOffs)/this.scale,(mouseY-this.clickData.yOffs)/this.scale);
        }
        for(var block : this.blocks ) block.draw(matrices, this.textRenderer, (int) (mouseX/scale), (int) (mouseY/scale));

        matrices.pop();
    }

    private class ClickData{
        public float xOffs;
        public float yOffs;
        public Block block;
    }
    private final ClickData clickData = new ClickData();

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        var oldMouseX=mouseX;
        var oldMouseY=mouseY;
        mouseX/=this.scale;
        mouseY/=this.scale;

        for(var block : this.blocks){
            if(block.mouse((float) mouseX, (float) mouseY)){
                this.clickData.block = block;
                var pos = block.getPos();
                this.clickData.xOffs = (float) (mouseX-pos.x)*this.scale;
                this.clickData.yOffs = (float) (mouseY-pos.y)*this.scale;
                block.disconnectFromParents();
                return true;
            }
        }

        return super.mouseClicked(oldMouseX,oldMouseY,button);
    }

    private static final float radius=20;
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if(this.clickData.block==null) return super.mouseReleased(mouseX,mouseY,button);

        var possibleConnections = new ArrayList<Pair<Float, Pair<Connection, Connection>>>();
        //todo: sort and snap to one with shortest dist

        for(var connection : this.clickData.block.connections.values()){
            if(connection instanceof SeqInConnection){
                var thisPos = connection.connPos();

                for(var block : this.blocks){
                    if(block==this.clickData.block) continue;

                    for(var c2 : block.connections.values()){
                        if(!(c2 instanceof SeqOutConnection)) continue;
                        var c2Pos = c2.connPos();

                        var dist=(float)Math.sqrt((thisPos.x-c2Pos.x)*(thisPos.x-c2Pos.x)+
                                (thisPos.y-c2Pos.y)*(thisPos.y-c2Pos.y));
                        if(dist<radius)
                            possibleConnections.add(new Pair<>(dist, new Pair<>(connection,c2)));
                    }
                }
            }
            if(connection instanceof InputOutConnection){
                var thisPos = connection.connPos();

                for(var block : this.blocks){
                    if(block==this.clickData.block) continue;

                    for(var c2 : block.connections.values()){
                        if(!(c2 instanceof InputInConnection ||c2 instanceof InlineInputInConnection)) continue;
                        var c2Pos = c2.connPos();

                        var dist=(float)Math.sqrt((thisPos.x-c2Pos.x)*(thisPos.x-c2Pos.x)+
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
}
