package com.kyfexuwu.jsonblocks.lua;

import com.kyfexuwu.jsonblocks.Utils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.TwoArgFunction;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.LinkedList;

public class LuaSurfaceObj extends LuaTable {
    public Object object;
    private final String[] valueNames;
    public static final TwoArgFunction eqFunc=new TwoArgFunction() {
        @Override
        public LuaValue call(LuaValue one, LuaValue two) {
            if(!(one instanceof LuaSurfaceObj && two instanceof LuaSurfaceObj))
                return FALSE;
            return LuaValue.valueOf(((LuaSurfaceObj)one).object.equals(((LuaSurfaceObj)two).object));
        }
    };

    public LuaSurfaceObj(Object object){
        LuaTable thisMT = new LuaTable();
        thisMT.set(EQ, eqFunc);
        this.setmetatable(thisMT);

        this.object=object;
        Field[] fields = object.getClass().getFields();//todo: dont copy methods from Object class
        Method[] methods = object.getClass().getMethods();

        var tempFieldNames = new LinkedList<String>();
        for(Field field : fields){
            var toAdd = field.getName();
            if(tempFieldNames.contains(toAdd))
                continue;
            tempFieldNames.add(toAdd);
        }
        var tempMethodNames = new LinkedList<String>();
        for(Method method : methods){
            var toAdd = method.getName();
            if(tempMethodNames.contains(toAdd))
                continue;

            if(tempFieldNames.contains(toAdd))
                toAdd="func_"+toAdd;
            tempMethodNames.add(toAdd);
        }

        tempFieldNames.addAll(tempMethodNames);
        tempFieldNames.sort(String::compareTo);
        valueNames = tempFieldNames.toArray(new String[0]);
    }

    public String typename(){
        return "surfaceObj";
    }

    @Override
    public LuaValue get(LuaValue key){
        try {
            return Utils.toLuaValue(object.getClass().getField(key.toString()).get(object));
        }catch(NoSuchFieldException e){
            LinkedList<Method> methods = new LinkedList<>();
            for(Method method : object.getClass().getMethods()){
                if(method.getName().equals(key.toString()))
                    methods.add(method);
            }
            if(methods.size()==0)
                return NIL;
            return new UndecidedLuaFunction(object,methods.toArray(new Method[0]));
        }catch(Exception ignored){}

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
            return LuaValue.valueOf(this.valueNames[0]);
        }
        String key = keyAsValue.toString();
        for(int i=0;i<this.valueNames.length-1;i++){
            if(key.equals(this.valueNames[i])&&!key.equals("wait")){//todo
                return LuaValue.valueOf(this.valueNames[i+1]);
            }
        }

        // nothing found, return nil.
        return NIL;
    }

    @Override
    public void set(LuaValue key, LuaValue value){
        MinecraftClient.getInstance().inGameHud.getChatHud()
                .addMessage(Text.of("You can't set properties on Java objects directly," +
                        "see if there's a helper function instead!"));
    }
    @Override
    public void rawset(LuaValue key, LuaValue value){
        MinecraftClient.getInstance().inGameHud.getChatHud()
                .addMessage(Text.of("bruh"));
    }

    public String toString(){
        return "java: "+Object.class.getSimpleName();
    }
}
