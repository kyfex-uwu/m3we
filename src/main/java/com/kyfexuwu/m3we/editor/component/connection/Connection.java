package com.kyfexuwu.m3we.editor.component.connection;

import com.kyfexuwu.m3we.editor.Block;
import com.kyfexuwu.m3we.editor.Vec2d;
import com.kyfexuwu.m3we.editor.component.Component;

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
    protected final Vec2d getConnPos(double x, double y) {
        var toReturn = this.globalPos();
        return new Vec2d(toReturn.x+x,toReturn.y+y);
    }
    public abstract Vec2d connPos();

    public static Vec2d getOffset(Connection connection){//todo: remove this
        if(connection instanceof SeqOutConnection){
            return new Vec2d(0, connection.height());
        }else if(connection instanceof InlineInputInConnection){
            return new Vec2d(5, connection.connected.y());
        }else if(connection instanceof InputInConnection){
            return new Vec2d(5,0);
        }
        return null;
    }
}
