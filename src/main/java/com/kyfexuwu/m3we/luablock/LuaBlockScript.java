package com.kyfexuwu.m3we.luablock;

import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.ScriptError;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.compiler.LuaC;

public class LuaBlockScript extends CustomScript {
    public final LuaBlockEntity self;
    public LuaBlockScript(LuaBlockEntity self) {
        super("LuaBlockEntity",true);

        this.self=self;
        this.updateScript("");
    }

    public int revision=0;
    public void updateScript(String script){
        this.runEnv = CustomScript.safeGlobal();
        LoadState.install(this.runEnv);
        LuaC.install(this.runEnv);
        this.revision=(this.revision+1)%100;

        var world = this.self.getWorld();
        if(world!=null&&!world.isClient) {
            this.setContext(this.self);
            ScriptError.execute(() -> this.runEnv.load(script).call());
        }
    }
}
