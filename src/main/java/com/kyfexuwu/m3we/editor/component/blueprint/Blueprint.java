package com.kyfexuwu.m3we.editor.component.blueprint;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.BlockDrawHelper;
import com.kyfexuwu.m3we.editor.Color;
import com.kyfexuwu.m3we.editor.component.*;
import com.kyfexuwu.m3we.editor.component.connection.*;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec2f;
import org.apache.commons.lang3.NotImplementedException;

import static net.minecraft.client.gui.DrawableHelper.drawTexture;

public class Blueprint {
    public enum Type{
        SOLID,
        SEQ,
        INPUT,
        CUSTOM
    }
    public static final Blueprint SOLID = new Blueprint(Type.SOLID);
    public interface SequenceMarker{}
    private static class SequenceMarked extends Blueprint implements SequenceMarker{
        protected SequenceMarked(String name) {
            super(Type.SEQ, name);
        }
    }
    public interface InputMarker{}
    private static class InputMarked extends Blueprint implements InputMarker{
        protected InputMarked(String name) {
            super(Type.INPUT, name);
        }
    }
    public static Blueprint seq(String name){ return new SequenceMarked(name); }
    public static Blueprint input(String name){ return new InputMarked(name); }
    private final Type type;
    private final String name;
    protected Blueprint(Type type, String name){
        this.type=type;
        this.name=name;
    }
    protected Blueprint(Type type){ this(type, null); }
    public Component create(Block block, int x, int y, Blueprint[][] allBlueprints){
        switch(this.type){
            case SOLID ->{
                if(y==0){
                    if(x==0) return new CornerComponent(block, Corner.TL);
                    if(x==allBlueprints[y].length-1) return new CornerComponent(block, Corner.TR);
                    return new HWallComponent(block, HSide.TOP);
                }
                if(y==allBlueprints.length-1){
                    if(x==0) return new CornerComponent(block, Corner.BL);
                    if(x==allBlueprints[y].length-1) return new CornerComponent(block, Corner.BR);
                    return new HWallComponent(block, HSide.BOTTOM);
                }
                if(x==0) return new VWallComponent(block, VSide.LEFT);
                if(x==allBlueprints[y].length-1) return new VWallComponent(block, VSide.RIGHT);
                return new InsideComponent(block);
            }
            case SEQ -> {
                if(this.name==null) throw new UnsupportedOperationException("Sequence component must have a name");
                if(y!=0&&y!=allBlueprints.length-1)
                    throw new UnsupportedOperationException("Cannot have a sequence notch anywhere but the edge, "+y);
                return y==0?new SeqInConnection(block, this.name):new SeqOutConnection(block, this.name);
            }
            case INPUT-> {
                if(this.name==null) throw new UnsupportedOperationException("Input component must have a name");
                if(x==0) return new InputOutConnection(block, this.name);
                if(x==allBlueprints[y].length-1) return new InputInConnection(block, this.name);
                return new InlineInputInConnection(block, this.name);
            }
            case CUSTOM -> throw new NotImplementedException("Cannot create default blueprint with SPECIAL");
        }

        //bruh
        return null;
    }

    //--
    private enum Corner{
        TL(new Vec2f(0,5), new Vec2f(5,5), new Vec2f(5,0),
                new Vec2f(3,0), new Vec2f(1,1), new Vec2f(0,3)),
        TR(new Vec2f(0,0), new Vec2f(0,5), new Vec2f(5,5),
                new Vec2f(5,3), new Vec2f(4,1), new Vec2f(2,0)),
        BL(new Vec2f(5,5), new Vec2f(5,0), new Vec2f(0,0),
                new Vec2f(0,2), new Vec2f(1,4), new Vec2f(3,5)),
        BR(new Vec2f(5,0), new Vec2f(0,0), new Vec2f(0,5),
                new Vec2f(2,5), new Vec2f(4,4), new Vec2f(5,2));

        public final Vec2f[] points;
        Corner(Vec2f... points){
            this.points=points;
        }
    }
    private enum VSide{ LEFT, RIGHT }
    private enum HSide{ TOP, BOTTOM }
    private static class CornerComponent extends NonResizableComponent {
        private final Corner type;
        public CornerComponent(Block parent, Corner type) {
            super(parent, 5, 5);
            this.type=type;
        }

        @Override
        public void draw(MatrixStack matrices, TextRenderer text, Color color) {
            BlockDrawHelper.vertexes(matrices, color, c->{
                for(var point : this.type.points) c.vertex(point.x,point.y);
            });
        }
    }
    private static class VWallComponent extends VFillingComponent {
        private final VSide side;
        public VWallComponent(Block parent, VSide side){
            super(parent, 5);
            this.side=side;
        }

        @Override
        public void draw(MatrixStack matrices, TextRenderer text, Color color) {
            BlockDrawHelper.vertexes(matrices, color, c->{
                c.vertex(0,0);
                c.vertex(0,this.height());
                c.vertex(this.width(),this.height());
                c.vertex(this.width(),0);
            });
        }
    }
    private static class HWallComponent extends HFillingComponent {
        private final HSide side;
        public HWallComponent(Block parent, HSide side){
            super(parent, 5);
            this.side=side;
        }

        @Override
        public void draw(MatrixStack matrices, TextRenderer text, Color color) {
            BlockDrawHelper.vertexes(matrices, color, c->{
                var height=this.height();
                var width=this.width();
                c.vertex(0,0);
                c.vertex(0,height);
                c.vertex(width,height);
                c.vertex(width,0);
            });
        }
    }
    private static class InsideComponent extends FillingComponent {
        public InsideComponent(Block parent) {
            super(parent);
        }

        @Override
        public void draw(MatrixStack matrices, TextRenderer text, Color color) {
            BlockDrawHelper.vertexes(matrices, color, c->{
                c.vertex(0,0);
                c.vertex(0,this.height());
                c.vertex(this.width(),this.height());
                c.vertex(this.width(),0);
            });
        }
    }
}
