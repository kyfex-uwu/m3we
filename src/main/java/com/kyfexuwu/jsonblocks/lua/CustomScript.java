package com.kyfexuwu.jsonblocks.lua;

import com.kyfexuwu.jsonblocks.JsonBlocks;
import com.kyfexuwu.jsonblocks.Utils;
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
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.Arrays;
import java.util.LinkedList;

import static org.luaj.vm2.LuaValue.NIL;

public class CustomScript {

    public Globals runEnv;
    public final String name;
    public final boolean isFake;

    static final Disabled disabled = new Disabled();

    public static LuaTable dataStore = new LuaTable();//todo, make this per world

    private static Globals unsafeGlobal(){
        var toReturn = new Globals();
        toReturn.load(new JseBaseLib());
        toReturn.load(new PackageLib());//needed, trust me
        toReturn.load(new TableLib());
        toReturn.load(new StringLib());
        toReturn.load(new JseMathLib());

        toReturn.set("print", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return print(args);
            }
        });
        toReturn.set("explore", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue value) {
                return explore(value);
            }
        });

        toReturn.load(new PropertyAPI());
        toReturn.load(new GuiAPI());
        toReturn.load(new RegistryAPI());
        toReturn.load(new DatastoreAPI());
        toReturn.load(new CreateApi());

        return toReturn;
    }
    protected static Globals safeGlobal(){
        var toReturn = unsafeGlobal();

        var load = toReturn.get("load");
        toReturn.set("loadLib", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                if(!arg.isstring() || arg.checkjstring().contains("..")) return NIL;
                try{
                    return load.call(Files.readString(new File(JsonBlocks.scriptsFolder.getAbsolutePath() +
                            "\\" + arg.checkjstring() + ".lua").toPath())).call();
                }catch(Exception ignored){}
                return NIL;
            }
        });
        toReturn.set("require",disabled);
        toReturn.set("load",disabled);
        toReturn.set("dofile",disabled);
        toReturn.set("loadfile",disabled);

        return toReturn;
    }

    static Varargs createVarArgs(Object... args){
        var luaArgs = Arrays.stream(args).map(Utils::toLuaValue).toArray(LuaValue[]::new);
        return new Varargs() {
            @Override
            public LuaValue arg(int i) {
                return luaArgs[i-1];
            }

            @Override
            public int narg() {
                return luaArgs.length;
            }

            @Override
            public LuaValue arg1() {
                return arg(1);
            }

            @Override
            public Varargs subargs(int start) {
                return createVarArgs(Arrays.copyOfRange(luaArgs,start-1,luaArgs.length));
            }
        };
    }

    static class Disabled extends VarArgFunction{
        @Override
        public Varargs invoke(Varargs args) {
            print(LuaValue.valueOf("This value is disabled"));
            return NIL;
        }
    }
    public static Varargs print(Varargs args){
        StringBuilder toPrint= new StringBuilder();
        for (int i = 1, length = args.narg(); i <= length; i++) {
            if(args.narg()==1&&args.arg(1).isstring())
                toPrint = new StringBuilder(args.arg(1).checkjstring());
            else
                toPrint.append(valueToString(args.arg(i), 0));
        }
        try {
            MinecraftClient.getInstance().inGameHud.getChatHud()
                    .addMessage(Text.of(toPrint.toString()));
        }catch(Exception e){
            System.out.println(toPrint);
        }
        return NIL;
    }
    public static void print(Object... args){
        print(createVarArgs(args));
    }
    public static LuaValue explore(LuaValue value){
        var chatHud = MinecraftClient.getInstance().inGameHud.getChatHud();

        MutableText message = Text.literal("\n");

        if(value.typename().equals("surfaceObj") || value.typename().equals("table")) {
            LuaValue nextKey = (LuaValue) value.next(NIL);
            do {
                LuaValue finalNextKey = nextKey;
                message.append(Text.literal(nextKey.toString()+", ")
                        .setStyle(Style.EMPTY.withClickEvent(new CustomClickEvent(()->{
                            explore(value.get(finalNextKey));
                        })).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal(valueToString(value.get(finalNextKey),0))))));

                nextKey = (LuaValue) value.next(nextKey);
            } while (nextKey != NIL);
        }else{
            message.append(Text.literal(value.toString()));
        }

        chatHud.addMessage(message);
        return NIL;
    }

    protected CustomScript(String name, boolean isFake){
        this.name=name;
        this.isFake=isFake;
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
        this.runEnv = safeGlobal();

        LoadState.install(this.runEnv);
        LuaC.install(this.runEnv);
        try {
            this.runEnv.load(
                    Files.readString(new File(JsonBlocks.JBFolder + "\\scripts\\" + fileName + ".lua").toPath())
            ).call();
        }catch(IOException | LuaError e){
            System.out.println("script "+fileName+" not loaded... it was a "+e.getClass().getName()+" exception");
            e.printStackTrace();
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
                var refMethods = ((UndecidedLuaFunction) value).methods;
                toReturn.append("java function: ")
                        .append(refMethods[0].getName());

                for(Method m : refMethods) {
                    var paramClasses = m.getParameterTypes();
                    if (paramClasses.length > 0) {
                        toReturn.append(" [takes parameters of types: ");
                        for (Class<?> clazz : paramClasses) {
                            toReturn.append(clazz.getSimpleName())
                                    .append(", ");
                        }
                    } else {
                        toReturn.append(" [takes no parameters, ");
                    }
                    toReturn.append("and ");

                    var returnClass = m.getReturnType();
                    if (!returnClass.equals(Void.class)) {
                        toReturn.append("returns with type ")
                                .append(returnClass.getSimpleName())
                                .append("]");
                    } else {
                        toReturn.append("does not return a value]");
                    }
                }
            }
        }
        return toReturn.toString();
    }
}
