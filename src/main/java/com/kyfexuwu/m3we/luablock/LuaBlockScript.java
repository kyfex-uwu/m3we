package com.kyfexuwu.m3we.luablock;

import com.kyfexuwu.m3we.Utils;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.LuaSurfaceObj;
import com.kyfexuwu.m3we.lua.ScriptError;
import com.kyfexuwu.m3we.m3we;
import net.minecraft.server.world.ServerWorld;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.compiler.LuaC;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class LuaBlockScript extends CustomScript {
    public final LuaBlockEntity self;
    public LuaBlockScript(LuaBlockEntity self) {
        super("LuaBlockEntity",true);

        this.self=self;
        this.updateScript("");
    }

    public int revision=0;
    public void updateScript(String script){
        this.revision=(this.revision+1)%Integer.MAX_VALUE;

        this.runEnv = CustomScript.safeGlobal();
        this.runEnv.set(contextIdentifier, this.contextObj);
        LoadState.install(this.runEnv);
        LuaC.install(this.runEnv);

        this.runEnv.set("self", new LuaSurfaceObj(this.self));
        ScriptError.execute(() -> this.runEnv.load(script).call());

        this.contextObj.javaSet("env", Utils.toLuaValue(this.self.getWorld() instanceof ServerWorld?"server":"client"));

        for(var listener : this.updateListeners) listener.accept(this);
    }
}
