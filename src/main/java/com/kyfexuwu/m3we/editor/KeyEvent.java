package com.kyfexuwu.m3we.editor;

public class KeyEvent {
    public enum Type{
        CHAR,
        BACKSPACE
    }
    public final Character chr;
    public final Type type;
    public KeyEvent(Type type){
        this.chr=null;
        this.type=type;
    }
    public KeyEvent(char chr){
        this.chr=chr;
        this.type=Type.CHAR;
    }
}
