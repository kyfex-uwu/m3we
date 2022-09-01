package com.kyfexuwu.jsonblocks.lua;

import com.kyfexuwu.jsonblocks.JsonBlocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
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
