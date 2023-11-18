package com.kyfexuwu.m3we.lua;

import com.kyfexuwu.m3we.Utils;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

public class JavaExclusiveTable extends LuaTable {
    private static final LuaError jxDisabled = new LuaError("This method is disabled for JXTables");
    @Override
    public LuaValue setmetatable(LuaValue var1) { throw jxDisabled; }
    @Override
    public void rawset(LuaValue key, LuaValue value) { throw jxDisabled; }
    @Override
    public void rawset(int key, LuaValue value) { throw jxDisabled; }
    @Override
    public LuaValue remove(int var1) { throw jxDisabled; }
    @Override
    public void insert(int var1, LuaValue var2) { throw jxDisabled; }
    @Override
    public void hashset(LuaValue var1, LuaValue var2) { throw jxDisabled; }
    @Override
    public void sort(LuaValue var1) { throw jxDisabled; }

    //--

    //copied from LuaTable
    private boolean arrayset(int var1, LuaValue var2) {
        if (var1 > 0 && var1 <= this.array.length) {
            this.array[var1 - 1] = var2.isnil() ? null : var2;
            return true;
        }
        return false;
    }
    private void jRawset(LuaValue var1, LuaValue var2) {
        if (!var1.isinttype() || !this.arrayset(var1.toint(), var2)) {
            super.hashset(var1, var2);
        }

    }

    public void javaSet(LuaValue key, LuaValue value){
        this.jRawset(key, value);
    }
    public void javaSet(int key, LuaValue value){
        this.jRawset(Utils.toLuaValue(key), value);
    }
    public void javaSet(String key, LuaValue value){
        this.javaSet(Utils.toLuaValue(key), value);
    }
    public void apiMethodSet(String name, LuaValue method, String desc) {
        this.javaSet(name+"__desc", Utils.toLuaValue(desc));
        this.javaSet(name, method);
    }
}
