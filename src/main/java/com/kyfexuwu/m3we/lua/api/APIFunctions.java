package com.kyfexuwu.m3we.lua.api;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.*;

public class APIFunctions {
    public abstract static class ZeroArgAPIFunc extends ZeroArgFunction{
        protected final LuaValue globals;
        public ZeroArgAPIFunc(LuaValue globals){
            this.globals=globals;
        }
    }
    public abstract static class OneArgAPIFunc extends OneArgFunction {
        protected final LuaValue globals;
        public OneArgAPIFunc(LuaValue globals){
            this.globals=globals;
        }
    }
    public abstract static class TwoArgAPIFunc extends TwoArgFunction{
        protected final LuaValue globals;
        public TwoArgAPIFunc(LuaValue globals){
            this.globals=globals;
        }
    }
    public abstract static class ThreeArgAPIFunc extends ThreeArgFunction{
        protected final LuaValue globals;
        public ThreeArgAPIFunc(LuaValue globals){
            this.globals=globals;
        }
    }
    public abstract static class VarArgAPIFunc extends VarArgFunction{
        protected final LuaValue globals;
        public VarArgAPIFunc(LuaValue globals){
            this.globals=globals;
        }
    }
}
