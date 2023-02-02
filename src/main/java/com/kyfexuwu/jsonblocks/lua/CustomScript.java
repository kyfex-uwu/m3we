package com.kyfexuwu.jsonblocks.lua;

import com.kyfexuwu.jsonblocks.JsonBlocks;
import com.kyfexuwu.jsonblocks.lua.api.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.*;
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
    public final boolean isFake;

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
                for (int i = 1, length = args.narg(); i <= length; i++) {
                    MinecraftClient.getInstance().inGameHud.getChatHud()
                            .addMessage(Text.of(valueToString(args.arg(i), 0)));
                }
                return NIL;
            }
        });
        toReturn.set("explore", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue value, LuaValue key) {
                if(key==LuaValue.NIL) key = LuaString.valueOf("Click to explore");

                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal(key.toString())
                    .setStyle(Style.EMPTY.withClickEvent(new CustomClickEvent(()->{
                        if(value.typename().equals("surfaceObj")||
                                value.typename().equals("table")){
                            LuaValue nextKey=LuaValue.NIL;
                            do {
                                nextKey = (LuaValue) value.next(nextKey);
                                toReturn.get("explore").call(value.get(nextKey),nextKey);
                            } while (nextKey != LuaValue.NIL);
                        }else{
                            toReturn.get("print").call(value);
                        }

                    }))));
                return NIL;
            }
        });

        toReturn.load(new PropertyAPI());
        toReturn.load(new EnumsAPI());

        return toReturn;
    }

    public CustomScript(String fileName){
        if(fileName==null) {
            this.name = "fake";
            this.isFake = true;
            return;
        }

        this.name=fileName;
        this.isFake=false;

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
        if(this.isFake) return;

        for(int i=0;i<scripts.size();i++){
            if(scripts.get(i).name.equals(this.name)) {
                scripts.remove(i);
                i--;
            }
        }
    }

    public void setSelf(Object self){
        if(this.isFake) return;
        this.runEnv.set("self",new LuaSurfaceObj(self));
    }

    private static final LinkedList<CustomScript> scripts = new LinkedList<>();
    public static void reloadScript(String name){
        for(CustomScript script : scripts){
            if(!(script.name+".lua").equals(name))
                continue;

            script.setScript(script.name);
            break;
        }
    }

    private static final int maxLevels=5;
    private static String valueToString(LuaValue value, int indents){
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
            case "undecidedFunc" -> {
                var refMethod = ((UndecidedLuaFunction) value).methods[0];
                toReturn.append("java function: ")
                        .append(refMethod.getName());

                var paramClasses = refMethod.getParameterTypes();
                if(paramClasses.length>0){
                    toReturn.append(" [takes parameters of types ");
                    for(Class clazz : paramClasses){
                        toReturn.append(clazz.getSimpleName())
                                .append(", ");
                    }
                    toReturn.append("]");
                }else{
                    toReturn.append(" [takes no parameters]");
                }

                var returnClass=refMethod.getReturnType();
                if(!returnClass.equals(Void.class)) {
                    toReturn.append(" [returns with type ")
                            .append(returnClass.getSimpleName())
                            .append("]");
                }else{
                    toReturn.append(" [does not return]");
                }
            }
        }
        return toReturn.toString();
    }
}
