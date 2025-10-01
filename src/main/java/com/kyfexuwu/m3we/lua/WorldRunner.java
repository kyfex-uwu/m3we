package com.kyfexuwu.m3we.lua;

import com.kyfexuwu.m3we.Utils;

public class WorldRunner {
    public final String name;
    private final CustomScript script;
    public WorldRunner(CustomScript script, String name){
        this.script=script;
        this.name=name;
    }

    public void tick(boolean isServer){
        Utils.tryAndExecute(this.script, "tick", new Object[]{isServer});
    }
}
