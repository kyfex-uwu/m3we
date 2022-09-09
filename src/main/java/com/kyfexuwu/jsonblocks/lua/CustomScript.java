package com.kyfexuwu.jsonblocks.lua;

import com.kyfexuwu.jsonblocks.JsonBlocks;
import com.kyfexuwu.jsonblocks.lua.api.*;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class CustomScript {

    public Globals runEnv;

    private static Globals createNewGlobal(){
        var toReturn = new Globals();
        toReturn.load(new JseBaseLib());
        toReturn.load(new PackageLib());//needed, trust me
        toReturn.load(new TableLib());
        toReturn.load(new StringLib());
        toReturn.load(new JseMathLib());

        toReturn.load(new PropertyAPI());

        return toReturn;
    }

    public CustomScript(String fileName){
        this.runEnv = createNewGlobal();

        LoadState.install(runEnv);
        LuaC.install(runEnv);
        try {
            runEnv.load(
                    Files.readString(new File(JsonBlocks.JBFolder + "\\scripts\\" + fileName + ".lua").toPath())
            ).call();
        }catch(IOException | LuaError e){
            System.out.println("script "+fileName+" not loaded... it was a "+e.getClass().getName()+" exception");
        }
    }
}
