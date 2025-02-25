package com.kyfexuwu.m3we.lua.api;

import com.kyfexuwu.m3we.Utils;
import com.kyfexuwu.m3we.lua.LuaFunc;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.LibFunction;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class MethodWrapper {
    @FunctionalInterface
    public interface TriFunction<A1,A2,A3,R>{
        R apply(A1 a1, A2 a2, A3 a3);
    }

    //these fail when an arg is a number type :( todo
    public static LibFunction create(Supplier<Object> func){
        return LuaFunc.func(args->Utils.toLuaValue(func.get()));
    }
    public static <A1> LibFunction create(Function<A1,Object> func){
        return LuaFunc.func(args->Utils.toLuaValue(func.apply(
                Utils.toObjectTyped(args.get(0)))));
    }
    public static <A1,A2> LibFunction create(BiFunction<A1,A2,Object> func){
        return LuaFunc.func(args->Utils.toLuaValue(func.apply(
                Utils.toObjectTyped(args.get(0)),
                Utils.toObjectTyped(args.get(1)))));
    }
    public static <A1,A2,A3> LibFunction create(TriFunction<A1,A2,A3,Object> func){
        return LuaFunc.func(args->Utils.toLuaValue(func.apply(
                Utils.toObjectTyped(args.get(0)),
                Utils.toObjectTyped(args.get(1)),
                Utils.toObjectTyped(args.get(2)))));
    }
    public static LibFunction varCreate(Function<List<LuaValue>,Object> func){
        return LuaFunc.func(args->Utils.toLuaValue(func.apply(args)));
    }
}
