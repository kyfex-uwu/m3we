package com.kyfexuwu.jsonblocks.lua;

import com.kyfexuwu.jsonblocks.JsonBlocks;
import com.kyfexuwu.jsonblocks.lua.api.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedList;

public class CustomScript {

    public Globals runEnv;
    public final String name;

    private static Globals createNewGlobal(){
        var toReturn = new Globals();
        toReturn.load(new JseBaseLib());
        toReturn.load(new PackageLib());//needed, trust me
        toReturn.load(new TableLib());
        toReturn.load(new StringLib());
        toReturn.load(new JseMathLib());

        toReturn.set("print", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                ChatHud chatHud;
                try{
                chatHud=MinecraftClient.getInstance().inGameHud.getChatHud();//test this to see if we can cache it more
                    for (int i = 1, length = args.narg(); i <= length; i++) {
                        chatHud.addMessage(Text.of(valueToString(args.arg(i), 0)));
                    }
                }catch(NullPointerException e){
                    for (int i = 1, length = args.narg(); i <= length; i++) {
                        System.out.println("printing: "+valueToString(args.arg(i), 0));
                    }
                }
                return NIL;
            }
        });

        toReturn.load(new PropertyAPI());

        return toReturn;
    }

    public CustomScript(String fileName){
        this.name=fileName;

        setScript(fileName);

        scripts.add(this);
    }
    private void setScript(String fileName){
        this.runEnv = createNewGlobal();

        LoadState.install(this.runEnv);
        LuaC.install(this.runEnv);
        try {
            this.runEnv.load(
                    Files.readString(new File(JsonBlocks.JBFolder + "\\scripts\\" + fileName + ".lua").toPath())
            ).call();
        }catch(IOException | LuaError e){
            System.out.println("script "+fileName+" not loaded... it was a "+e.getClass().getName()+" exception");
        }
    }
    public void remove(){
        for(int i=0;i<scripts.size();i++){
            if(scripts.get(i).name.equals(this.name)) {
                scripts.remove(i);
                i--;
            }
        }
    }

    private static LinkedList<CustomScript> scripts = new LinkedList<>();
    public static void reloadScript(String name){
        for(CustomScript script : scripts){
            if(!(script.name+".lua").equals(name))
                continue;

            script.setScript(script.name);
            break;
        }
    }

    private static final int maxLevels=5;
    public static String valueToString(LuaValue value, int indents){
        StringBuilder toReturn= new StringBuilder();
        toReturn.append("  ".repeat(indents));

        switch (value.typename()) {
            case "nil", "boolean", "number", "function", "userdata", "thread" -> toReturn.append(value);
            case "string" -> toReturn.append("\"").append(value).append("\"");
            case "table" -> {
                if(indents<maxLevels) {
                    toReturn.append("{\n");
                    var keys = ((LuaTable)value).keys();
                    for(LuaValue key : keys){
                        toReturn.append(key).append("=").append(valueToString(value.get(key), indents + 1)).append(",\n");
                    }
                    toReturn.append("  ".repeat(indents)).append("}");
                }else{
                    toReturn.append("{...}");
                }
            }
            case "surfaceObj" -> toReturn.append("java object: ").append(((LuaSurfaceObj) value).object.getClass().getSimpleName());
            case "undecidedFunc" -> toReturn.append("java function: ").append(((UndecidedLuaFunction) value).methods[0].getName());
        }
        return toReturn.toString();
    }
}
