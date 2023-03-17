package com.kyfexuwu.jsonblocks.lua;

import org.luaj.vm2.LuaBoolean;

import java.util.function.Consumer;

import static org.luaj.vm2.LuaValue.FALSE;
import static org.luaj.vm2.LuaValue.TRUE;

public class ScriptError {
     public static LuaBoolean execute(Runnable toRun){
        return execute(toRun,false);
    }
    public static LuaBoolean execute(Runnable toRun, boolean log){
         return execute(toRun,(e)->{
             if(log) e.printStackTrace();
         });
    }
    public static LuaBoolean execute(Runnable toRun, Consumer<Exception> onError){
        try{
            toRun.run();
            return TRUE;
        }catch(Exception e){
            onError.accept(e);
            return FALSE;
        }
    }
}
