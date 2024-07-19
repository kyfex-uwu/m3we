package com.kyfexuwu.m3we.editor;

import com.kyfexuwu.m3we.editor.component.Component;
import com.kyfexuwu.m3we.editor.component.blueprint.Blueprint;
import com.kyfexuwu.m3we.editor.component.connection.Connection;
import com.kyfexuwu.m3we.editor.component.connection.InputOutConnection;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;

import java.util.HashMap;
import java.util.function.Function;

public class Block {
    private final Color color;
    public final Component[][] components;
    public final HashMap<String, Connection> connections = new HashMap<>();
    private final Function<Block, String> exportFunc;
    private Vec2d offs = new Vec2d(0,0);

    public Block(Color color, Blueprint[][] components, Function<Block, String> export){
        this.color=color;
        this.exportFunc=export;
        this.components=new Component[components.length][];
        for(int y=0;y<components.length;y++) {
            this.components[y]=new Component[components[y].length];
            for (int x = 0; x < components[y].length; x++) {
                this.components[y][x] = components[y][x].create(this, x, y, components);
                if(this.components[y][x] instanceof Connection connection)
                    this.connections.put(connection.name, connection);
            }
        }
    }

    public void connect(String connName, Block other, String otherName){
        this.connections.get(connName).connect(other.connections.get(otherName));
    }
    public void disconnectFromParents(){
        if(this.connections.containsKey("prev")) this.connections.get("prev").disconnect();
        for(var connection : this.connections.values())
            if(connection instanceof InputOutConnection) connection.disconnect();
    }

    public Rect2d boundingBox() {
        if (this.components.length==0 || this.components[0].length==0)
            return new Rect2d(0,0,0,0);

        double width=0;
        double height=0;
        for(var row : this.components){
            width=Math.max(width, Component.rowWidth(row));
            height+=Component.rowHeight(row);
        }

        var pos = this.getPos();
        return new Rect2d(pos.x,pos.y,width, height);
    }
    public Vec2d getPos(){
        var conn = this.connections.get("prev");
        if(conn==null||!conn.isConnected()) {
            conn=null;
            for (var c2 : this.connections.values()) {
                if (c2 instanceof InputOutConnection ic2 && ic2.isConnected()) {
                    conn = ic2;
                    break;
                }
            }
        }

        if(conn!=null){
            var bBox = conn.getConnection().parent.boundingBox();
            var offs = Connection.getOffset(conn.getConnection());

            return new Vec2d(
                    bBox.x()+conn.getConnection().x()-conn.x()+this.offs.x+offs.x,
                    bBox.y()+conn.getConnection().y()-conn.y()+this.offs.y+offs.y
            );
        }

        return new Vec2d(this.offs.x,this.offs.y);
    }
    public boolean mouse(double x, double y){
        var offs = this.getPos();
        x-=offs.x;
        y-=offs.y;

        for(var row : this.components){
            for(var c : row){
                var cX=c.x();
                var cY=c.y();

                if(x>=cX&&y>=cY&&x<cX+c.width()&&y<cY+c.height())
                    return c.mouseable(x-cX,y-cY);
            }
        }

        return false;
    }
    public boolean click(double x, double y){
        var offs = this.getPos();
        x-=offs.x;
        y-=offs.y;

        for(var row : this.components){
            for(var c : row){
                var cX=c.x();
                var cY=c.y();

                if(x>=cX&&y>=cY&&x<cX+c.width()&&y<cY+c.height()&&
                        c.click(cX-x,cY-y)) return true;
            }
        }

        return false;
    }

    public void offset(Vec2d offs){
        if(this.connections.containsKey("prev")&&this.connections.get("prev").isConnected()) return;
        for(var connection : this.connections.values())
            if(connection instanceof InputOutConnection&&connection.isConnected()) return;

        this.offs = offs;
    }
    public void offset(double x, double y){
        this.offset(new Vec2d(x,y));
    }
    public Vec2d offset(){ return this.offs; }
    public void removeOffset(){ this.offs = new Vec2d(0,0); }

    public void draw(MatrixStack matrices, TextRenderer textRenderer, int mouseX, int mouseY){
        matrices.push();
        var pos = this.getPos();
        matrices.translate(pos.x, pos.y, 0);

        var color = this.mouse(mouseX,mouseY)?this.color.highlighted():this.color;

        for(var y=0;y<this.components.length;y++){
            for(var x=0;x<this.components[y].length;x++){
                matrices.push();
                matrices.translate(this.components[y][x].x(), this.components[y][x].y(), 0);
                this.components[y][x].draw(matrices, textRenderer, color);
                matrices.pop();
            }
        }
        matrices.pop();
    }

    public String export(){
        var toReturn = this.exportFunc.apply(this);
        if(this.connections.containsKey("next")) return toReturn+"\n"+this.connections.get("next").parent.export();
        return toReturn;
    }
    public static String getExport(Block block, String connection){
        return block.connections.get(connection).parent.export();
    }
}
