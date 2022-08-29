package com.kyfexuwu.jsonblocks.lua;

import com.kyfexuwu.jsonblocks.JsonBlocks;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class CustomScript {

    public Globals runEnv = createNewGlobal();

    public static Globals createNewGlobal(){
        var toReturn = new Globals();
        toReturn.load(new JseBaseLib());
        toReturn.load(new PackageLib());//needed, trust me
        toReturn.load(new TableLib());
        toReturn.load(new StringLib());
        toReturn.load(new JseMathLib());

        toReturn.load(new BlockApi());

        return toReturn;
    }

    public CustomScript(String fileName){
        LoadState.install(runEnv);
        LuaC.install(runEnv);
        try {
            runEnv.load(
                    Files.readString(new File(JsonBlocks.JBFolder + "\\scripts\\" + fileName + ".lua").toPath())
            ).call();
        }catch(IOException | LuaError ignored){}
    }
}
