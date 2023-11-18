package com.kyfexuwu.m3we.lua.api;

import com.kyfexuwu.m3we.Utils;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.JavaExclusiveTable;
import net.minecraft.util.ActionResult;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.Locale;

public class EnumsAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modName, LuaValue env) {
        JavaExclusiveTable thisApi = new JavaExclusiveTable();

        thisApi.javaSet("ActionResult", populate(ActionResult.values()));
        thisApi.javaSet("Direction", populate((StringIdentifiable[])Direction.values()));
        thisApi.javaSet("RedstonePower", populate(new Boolean[]{true, false}, new String[]{"STRONG", "WEAK"}));

        return CustomScript.finalizeAPI("Enums",thisApi,env);
    }

    static JavaExclusiveTable populate(Enum<?>[] entries){
        String[] names = new String[entries.length];
        for(int i=0;i< entries.length;i++){
            names[i]=entries[i].toString().toUpperCase(Locale.ROOT);
        }
        return populate(entries, names);
    }
    static JavaExclusiveTable populate(StringIdentifiable[] entries){
        String[] names = new String[entries.length];
        for(int i=0;i< entries.length;i++){
            names[i]=entries[i].asString().toUpperCase(Locale.ROOT);
        }
        return populate(entries, names);
    }
    static <T> JavaExclusiveTable populate(T[] entries, String[] names){// T is in there so "enum"s have to be the same type
        var toReturn = new JavaExclusiveTable();
        for(int i=0;i<entries.length;i++){
            toReturn.javaSet(names[i], Utils.toLuaValue(entries[i]));
        }
        return toReturn;
    }
}
