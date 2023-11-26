package com.kyfexuwu.m3we.lua;

import com.kyfexuwu.m3we.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.TwoArgFunction;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;

public class LuaSurfaceObj extends LuaTable {
    public final Object object;
    public final Class<?> objClass;
    private final Token[] fields;
    private final Token[] methods;
    public static final TwoArgFunction eqFunc=new TwoArgFunction() {
        @Override
        public LuaValue call(LuaValue one, LuaValue two) {
            if(!(one instanceof LuaSurfaceObj && two instanceof LuaSurfaceObj))
                return FALSE;
            return LuaValue.valueOf(((LuaSurfaceObj)one).object.equals(((LuaSurfaceObj)two).object));
        }
    };

    public static class Token{
        public final String obf;
        public final String deobf;
        public Token(String obf, String deobf){
            this.obf=obf;
            this.deobf=deobf;
        }
    }

    public LuaSurfaceObj(Object object){
        LuaTable thisMT = new LuaTable();
        thisMT.set(EQ, eqFunc);
        this.setmetatable(thisMT);

        this.object=object;
        this.objClass = object.getClass();
        Field[] fields = this.objClass.getFields();
        Method[] methods = this.objClass.getMethods();

        var tempFieldNames = new LinkedList<Token>();
        for(Field field : fields){
            var toAdd = field.getName();
            var deobfuscated = Utils.deobfuscate(toAdd);

            if(deobfuscated==null || tempFieldNames.stream().anyMatch((pair)->pair.deobf.equals(deobfuscated)))
                continue;

            tempFieldNames.add(new Token(toAdd,Utils.deobfuscate(toAdd)));
        }

        var tempMethodNames = new LinkedList<Token>();
        for(Method method : methods){
            var toAdd = method.getName();
            var deobfuscated = Utils.deobfuscate(toAdd);

            if(deobfuscated==null || tempMethodNames.stream().anyMatch((pair)->pair.deobf.equals(deobfuscated)))
                continue;

            if(tempFieldNames.stream().anyMatch((token)->token.deobf.equals(deobfuscated)))
                tempMethodNames.add(new Token(toAdd,"func_"+deobfuscated));
            else
                tempMethodNames.add(new Token(toAdd,deobfuscated));
        }

        tempFieldNames.sort((f1, f2) -> f1.deobf.compareToIgnoreCase(f2.deobf));
        tempMethodNames.sort((m1, m2) -> m1.deobf.compareToIgnoreCase(m2.deobf));
        this.fields = tempFieldNames.toArray(new Token[]{});
        this.methods = tempMethodNames.toArray(new Token[]{});
    }

    public String typename(){
        return "surfaceObj";
    }

    @Override
    public LuaValue get(LuaValue key){
        var toReturn = Arrays.stream(this.fields).filter((value)->value.deobf.equals(key.checkjstring()))
                .findFirst();
        try {
            if (toReturn.isPresent()) {
                return Utils.toLuaValue(this.objClass.getField(toReturn.get().obf).get(this.object));
            } else {
                toReturn = Arrays.stream(this.methods).filter((method) -> method.deobf.equals(key.checkjstring()))
                        .findFirst();
                if (toReturn.isPresent()) {
                    var matchingMethods = Arrays.stream(this.methods)
                            .filter((method)->method.deobf.equals(key.checkjstring())).toList();
                    var realName = key.checkjstring().startsWith("func_")?
                            key.checkjstring().substring(5) : key.checkjstring();

                    return new UndecidedLuaFunction(this.object, Arrays.stream(this.objClass.getMethods())
                            .filter((method)->matchingMethods.stream().anyMatch((validMethod)->
                                    Utils.deobfuscate(method.getName()).equals(realName)))
                            .toList().toArray(new Method[]{}));
                }
            }
        }catch(Exception ignored){ }

        //not a field or a method? L bozo
        return NIL;
    }
    @Override
    public LuaValue rawget(int key){ return get(LuaValue.valueOf(key)); }
    @Override
    public LuaValue rawget(LuaValue key){ return get(key); }

    @Override //now works with pairs :sugnlasses:
    public Varargs next( LuaValue keyAsValue ) {
        if(keyAsValue.isnil()) {
            if(this.fields.length>0)
                return LuaValue.valueOf(this.fields[0].deobf);
            else if(this.methods.length>0)
                return LuaValue.valueOf(this.methods[0].deobf);
        }
        String key = keyAsValue.toString();
        for(int i=0;i<this.fields.length-1;i++){
            if(key.equals(this.fields[i].deobf)){
                return LuaValue.valueOf(this.fields[i+1].deobf);
            }
        }
        if(this.fields.length>0&&key.equals(this.fields[this.fields.length-1].deobf)){
            return LuaValue.valueOf(this.methods[0].deobf);
        }
        for(int i=0;i<this.methods.length-1;i++){
            if(key.equals(this.methods[i].deobf)){
                return LuaValue.valueOf(this.methods[i+1].deobf);
            }
        }

        // nothing found, return nil.
        return NIL;
    }

    @Override
    public void set(LuaValue key, LuaValue value){
        MinecraftClient.getInstance().inGameHud.getChatHud()
                .addMessage(Text.of("You can't set properties on Java objects directly," +
                        "see if there's a helper function instead! (this should change soon)"));
    }
    @Override
    public void rawset(LuaValue key, LuaValue value){
        MinecraftClient.getInstance().inGameHud.getChatHud()
                .addMessage(Text.of("bruh"));
    }

    public String toString(){
        return "java: "+Utils.deobfuscate(Object.class.getSimpleName());
    }
}
