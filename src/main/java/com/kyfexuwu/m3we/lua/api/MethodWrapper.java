package com.kyfexuwu.m3we.lua.api;

import com.kyfexuwu.m3we.Utils;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.*;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class MethodWrapper {
    @FunctionalInterface
    interface TriFunction<A1,A2,A3,R>{
        R apply(A1 a1, A2 a2, A3 a3);
    }
    public static final MethodWrapper inst = new MethodWrapper();
    private MethodWrapper(){}

    public LibFunction create(Supplier<Object> func){
        return new ZeroArgFunction(){
            @Override
            public LuaValue call() {
                return Utils.toLuaValue(func.get());
            }
        };
    }
    public <A1> LibFunction create(Function<A1,Object> func){
        return new OneArgFunction(){
            @Override
            public LuaValue call(LuaValue luaValue) {
                return Utils.toLuaValue(func.apply(
                        (A1) Utils.toObject(luaValue)));
            }
        };
    }
    public <A1,A2> LibFunction create(BiFunction<A1,A2,Object> func){
        return new TwoArgFunction(){
            @Override
            public LuaValue call(LuaValue luaValue, LuaValue luaValue2) {
                return Utils.toLuaValue(func.apply(
                        (A1) Utils.toObject(luaValue),
                        (A2) Utils.toObject(luaValue2)));
            }
        };
    }
    public <A1,A2,A3> LibFunction create(TriFunction<A1,A2,A3,Object> func){
        return new ThreeArgFunction(){
            @Override
            public LuaValue call(LuaValue luaValue, LuaValue luaValue2, LuaValue luaValue3) {
                return Utils.toLuaValue(func.apply(
                        (A1) Utils.toObject(luaValue),
                        (A2) Utils.toObject(luaValue2),
                        (A3) Utils.toObject(luaValue3)));
            }
        };
    }
    public LibFunction varCreate(Function<Varargs,Object> func){
        return new VarArgFunction(){
            @Override
            public LuaValue invoke(Varargs args) {
                return Utils.toLuaValue(func.apply(args));
            }
        };
    }
}
