package com.kyfexuwu.m3we.editor.component.connection;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.component.Component;
import net.minecraft.util.math.Vec2f;

public abstract class Connection extends Component {
    protected Connection connected;
    public final String name;
    public Connection(Block parent, String name) {
        super(parent);
        this.name=name;
    }

    public void connect(Connection other) {
        if(this.connected!=null) this.connected.connected=null;

        this.connected=other;
        other.connected=this;
        this.parent.removeOffset();
    }
    public void disconnect() {
        if (this.connected == null) return;

        this.connected.connected = null;
        this.connected = null;
    }
    public boolean isConnected(){ return this.connected!=null; }
    public Connection getConnection(){ return this.connected; }
    protected final Vec2f getConnPos(float x, float y) {
        var toReturn = this.globalPos();
        return new Vec2f(toReturn.x+x,toReturn.y+y);
    }
    public abstract Vec2f connPos();

    public static Vec2f getOffset(Connection connection){//todo: remove this
        if(connection instanceof SeqOutConnection){
            return new Vec2f(0, connection.height());
        }else if(connection instanceof InlineInputInConnection){
            return new Vec2f(5, connection.connected.y());
        }else if(connection instanceof InputInConnection){
            return new Vec2f(5,0);
        }
        return null;
    }
}
