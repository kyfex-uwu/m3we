package com.kyfexuwu.jsonblocks.luablock;

import com.kyfexuwu.jsonblocks.lua.CustomScript;
import com.kyfexuwu.jsonblocks.lua.ScriptError;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.compiler.LuaC;

public class LuaBlockScript extends CustomScript {
    public LuaBlockScript(LuaBlockEntity self) {
        super("LuaBlockEntity",true);

        this.runEnv = safeGlobal();

        LoadState.install(this.runEnv);
        LuaC.install(this.runEnv);
        this.runEnv.load("").call();
    }

    public int revision=0;
    public void updateScript(String script){
        ScriptError.execute(()-> this.runEnv.load(script).call());
        this.revision=(this.revision+1)%100;
    }
}
