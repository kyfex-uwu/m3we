package com.kyfexuwu.m3we.lua;

import com.mojang.datafixers.types.Func;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class LuaFunc {
    public static class ArrayListWithNil extends ArrayList<LuaValue>{
        @Override
        public LuaValue get(int index) {
            if(index>=0&&index<this.size()) return super.get(index);
            return LuaValue.NIL;
        }
    }
    public static VarArgFunction func(Function<List<LuaValue>, LuaValue> func){
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args){
                var list = new ArrayListWithNil();
                for(int i=1;i<=args.narg();i++) list.add(args.arg(i));

                return func.apply(list);
            }
        };
    }
}
