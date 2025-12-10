package com.kyfexuwu.m3we.lua;

import com.kyfexuwu.m3we.Utils;
import com.kyfexuwu.m3we.lua.api.*;
import com.kyfexuwu.m3we.m3we;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

import static org.luaj.vm2.LuaValue.NIL;
import static org.luaj.vm2.LuaValue.NONE;

public class CustomScript {

    public static CustomScript NULL = new CustomScript(null, true);

    public Globals runEnv;
    public final String name;
    public final boolean isFake;

    static final Disabled disabled = new Disabled();


    public static final String contextIdentifier = "__context";
    public final JavaExclusiveTable contextObj = new JavaExclusiveTable();
    public static final ArrayList<String> apiNames = new ArrayList<>();
    private static class CustomGlobals extends Globals{
        @Override
        public void hashset(LuaValue luaKey, LuaValue val) {
            try{
                var key=luaKey.checkjstring();
                if(key.equals(contextIdentifier))
                    throw new LuaError("Cannot overwrite "+contextIdentifier);
                for(var name : apiNames){
                    if(key.equals(name)) throw new LuaError("Cannot overwrite API "+name);
                }
            }catch(Exception ignored){}
            super.hashset(luaKey, val);
        }
    }
    private static final Logger printLogger = Logger.getLogger("m3we-print");
    private static Globals unsafeGlobal(){
        var toReturn = new CustomGlobals();
        toReturn.load(new JseBaseLib());
        toReturn.load(new PackageLib());//needed, trust me
        toReturn.load(new TableLib());
        toReturn.load(new StringLib());
        toReturn.load(new JseMathLib());

        toReturn.set("print", LuaFunc.func(args->{
            print(toReturn.get(contextIdentifier).get("env").optjstring("none"), args);
            return NONE;
        }));
        toReturn.set("consoleprint", LuaFunc.func(args->{
            var toPrint = new StringBuilder();
            for(int i=0;i<args.size()-1;i++) toPrint.append(args.get(i)).append(", ");
            printLogger.info(toPrint.append(args.get(args.size()-1)).toString());
            return NONE;
        }));
        toReturn.set("explore", LuaFunc.func(values->explore(values.get(0))));
        toReturn.set("loadclass", LuaFunc.func(args->{
            try {
                return loadclass(args.get(0).checkjstring());
            }catch(LuaError e){
                return NIL;
            }
        }));

        toReturn.load(new BlockEntityAPI());
        toReturn.load(new CreateAPI());
        toReturn.load(new DatastoreAPI());
        toReturn.load(new EnumsAPI());
        toReturn.load(new GuiAPI());
        toReturn.load(new MiscAPI());
        toReturn.load(new PropertyAPI());
        toReturn.load(new RedstoneAPI());
        toReturn.load(new RegistryAPI());
        toReturn.load(new SignalsAPI());
        toReturn.load(new MultiblockAPI());

        return toReturn;
    }
    protected static Globals safeGlobal(){
        var toReturn = unsafeGlobal();

        //var load = toReturn.get("load");
        toReturn.set("loadLib", LuaFunc.func(args -> {
            var arg = args.get(0);
            if (!arg.isstring() || arg.checkjstring().contains("..")) return NIL;
            try {
//                return load.call(Files.readString(Paths.get(m3we.scriptsFolder.getAbsolutePath(),
//                        arg.checkjstring() + ".lua"))).call();
            } catch (Exception ignored) {}
            return NIL;
        }));
        toReturn.set("require",disabled);
        toReturn.set("load",disabled);
        toReturn.set("dofile",disabled);
        toReturn.set("loadfile",disabled);

        return toReturn;
    }

    public static Varargs createVarArgs(Object... args){
        if(args.length==1&&args[0] instanceof LuaFunc.ArrayListWithNil arg1){
            var size=arg1.size();
            var newArgs = new Object[size];
            for(int i=0;i<size;i++)
                newArgs[i]=arg1.get(i);
            return createVarArgs(newArgs);
        }

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
                return createVarArgs((Object[]) Arrays.copyOfRange(luaArgs,start-1,luaArgs.length));
            }
        };
    }

    static class Disabled extends VarArgFunction{
        @Override
        public Varargs invoke(Varargs args) {
            print("client", LuaValue.valueOf("This value is disabled"));
            return NIL;
        }
    }
    public static MinecraftServer currentServer;
    //can we autodetect environment?
    public static Varargs print(String env, Varargs args){
        StringBuilder toPrint= new StringBuilder();
        if(args.narg()==1 && args.arg(1).isstring()){
            toPrint.append(args.arg(1).checkjstring());
        }else {
            for (int i = 1, length = args.narg(); i <= length; i++) {
                toPrint.append(i > 1 ? ", " : "").append(valueToString(args.arg(i), 0));
            }
        }
        try {//CHANGE
            var message = Text.of(toPrint.toString().replace("\r",""));
            if (env.equals("server")) {
                for (var player : currentServer.getPlayerManager().getPlayerList()) {
                    if (player.hasPermissionLevel(1)) player.sendMessage(message);
                }
            } else {
                ChatMessage.message(message);
            }
        }catch(Exception e){
            m3we.LOGGER.debug("m3we print: "+toPrint);
        }
        return NIL;
    }
    public static void print(String env, Object... args){
        print(env, createVarArgs(args));
    }
    private static boolean isPrimitive(Object o){//todo: make number cast to all of these types
        final var clazz = o.getClass();
        return clazz == Integer.class ||
                clazz == Float.class ||
                clazz == Double.class ||
                clazz == Long.class ||
                clazz == Character.class ||
                clazz == Byte.class ||
                clazz == Short.class ||
                clazz == Boolean.class ||
                clazz == String.class;
    }
    public static LuaValue explore(LuaValue value){
        var obj = Utils.toObject(value);

        if(obj == null){
            ChatMessage.message(Text.literal("explored: nil").setStyle(Style.EMPTY.withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Primitive value")))));
            return NIL;
        }
        if(isPrimitive(obj)){
            ChatMessage.message(Text.literal("explored: "+obj).setStyle(Style.EMPTY.withHoverEvent(
                    new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Primitive value")))));
            return NIL;
        }

        ChatMessage.message(Text.literal("exploring "+Utils.deobfuscate(obj.getClass().getSimpleName()))
                .setStyle(Style.EMPTY.withClickEvent(new CustomClickEvent(()->
                        MinecraftClient.getInstance().setScreen(new ExploreScreen(value)))).withHoverEvent(
                                new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Text.literal("Click to explore")))));
        return NIL;
    }
    public static LuaValue loadclass(String string){
        Class<?> toReturn;
        try {
            var classToken=Arrays.stream(Translations.classesTranslations)
                    .filter(token->token!=null&&token.longDeobfuscated.equals(string)).findFirst();
            toReturn=Class.forName(classToken.isPresent()?
                    (Translations.OBFUSCATED?classToken.get().longObfuscated:classToken.get().longDeobfuscated):
                    string);
        }catch(Exception e) {
            e.printStackTrace();
            return NIL;
        }
        return Utils.toLuaValue(toReturn);
    }

    protected CustomScript(String name, boolean isFake){
        this.name=name;
        this.isFake=isFake;
    }
    public CustomScript(String fileName){
        if(fileName==null||fileName.isEmpty()) {
            this.name = "fake";
            this.isFake = true;
            return;
        }

        this.name=fileName;
        this.isFake=false;

        this.setScript(fileName);

        scripts.add(this);
    }
    public final List<Consumer<CustomScript>> updateListeners = new ArrayList<>();
    private void setScript(String fileName){
        this.runEnv = safeGlobal();
        this.runEnv.set(contextIdentifier, this.contextObj);

        LoadState.install(this.runEnv);
        LuaC.install(this.runEnv);
        try {
            var parts = fileName.split(":");
            if(parts.length==1) throw new InvalidPathException("","Script paths mush be in the form pack_name:file_name");

            this.runEnv.load(
                Files.readString(Paths.get(m3we.m3weFolder.getAbsolutePath(), parts[0],
                        "scripts", parts[1]+".lua"))
            ).call();
            for(var listener : this.updateListeners) listener.accept(this);
        }catch(IOException | LuaError | InvalidPathException e){
            m3we.LOGGER.error("script {} not loaded... it was a {} exception. Message: {}",
                    fileName, e.getClass().getName(), e.getMessage());
            //e.printStackTrace();
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
    public void setStateWorldPos(BlockState state, World world, BlockPos pos){
        if(this.isFake) return;

        this.contextObj.javaSet("blockState",Utils.toLuaValue(state));//should change these keys to constants
        this.contextObj.javaSet("world",Utils.toLuaValue(world));
        this.contextObj.javaSet("blockPos",Utils.toLuaValue(pos));
    }
    public void clearStateWorldPos() {
        //this.setStateWorldPos(null, null, null); //hm.
    }

    public static final ArrayList<CustomScript> scripts = new ArrayList<>();
    public static void reloadScript(String name){
        for(CustomScript script : scripts){
            if(!(script.name+".lua").equals(name))
                continue;

            script.setScript(script.name);
        }
    }
    public static void reloadAll(){
        for(CustomScript script : scripts){
            script.setScript(script.name);
        }
    }

    private static final int maxLevels=5;
    private static String valueToString(LuaValue value, int indents, Utils.Ref<Integer> maxVals){
        if(maxVals.value<=0) return "";

        StringBuilder toReturn= new StringBuilder();
        toReturn.append("  ".repeat(indents));

        maxVals.value--;
        switch (value.typename()) {
            case "nil", "boolean", "number", "function", "userdata", "thread" -> toReturn.append(value);
            case "string" -> toReturn.append("\"").append(value).append("\"");
            case "table" -> {
                if(indents<maxLevels) {
                    toReturn.append("{\n");
                    var keys = ((LuaTable)value).keys();
                    for(LuaValue key : keys){
                        toReturn.append(key).append("=").append(valueToString(value.get(key), indents + 1, maxVals)).append(",\n");
                    }
                    toReturn.append("  ".repeat(indents)).append("}");
                }else{
                    toReturn.append("{...}");
                }
            }
            case LuaSurfaceObj.TYPENAME -> toReturn.append("java object: ").append(Utils.deobfuscate(
                    ((LuaSurfaceObj) value).object.getClass().getSimpleName()));
            case "undecidedFunc" -> {
                var func = ((UndecidedLuaFunction) value);
                var description = func.methodDescribers();
                toReturn.append("java function: ")
                        .append(func.funcName());

                for(var descriptor : description) {
                    if (!descriptor.params().isEmpty()) {
                        toReturn.append("\n • [takes parameters: ");
                        for (int i=0;i<descriptor.params().size();i++) {
                            if(i>0) toReturn.append(", ");
                            toReturn.append(descriptor.params().get(i).getB())
                                    .append(" (")
                                    .append(descriptor.params().get(i).getA())
                                    .append(")");
                        }
                    } else {
                        toReturn.append(" [takes no parameters,");
                    }
                    toReturn.append(" and ");

                    if(descriptor.type()== UndecidedLuaFunction.MethodType.METHOD) {
                        var returnClass = descriptor.returnClass();
                        if (!returnClass.equals(Void.class.getSimpleName())) {
                            toReturn.append("returns with type ")
                                    .append(returnClass)
                                    .append("]");
                        } else {
                            toReturn.append("does not return a value]");
                        }
                    }else{
                        toReturn.append("and creates a new ")
                                .append(descriptor.returnClass())
                                .append("]");
                    }
                }
            }
        }
        return toReturn.toString();
    }
    private static String valueToString(LuaValue value, int indents){

        return valueToString(value, indents, new Utils.Ref<>(100));
    }

    public static LuaValue finalizeAPI(String name, LuaValue api, LuaValue env){
        apiNames.add(name);

        env.set(name, api);
        env.get("package").get("loaded").set(name, api);
        return api;
    }
}
