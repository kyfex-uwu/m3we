package com.kyfexuwu.m3we.lua.api;

import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.JavaExclusiveTable;
import com.kyfexuwu.m3we.lua.LuaSurfaceObj;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.HashMap;

public class RegistryAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        JavaExclusiveTable thisApi = new RegistryRegistryAccessor(Registry.REGISTRIES);

        return CustomScript.finalizeAPI("Registry",thisApi,env);
    }

    static private class RegistryAccessor extends JavaExclusiveTable{
        public final Registry<?> registry;
        public RegistryAccessor(Registry<?> registry){
            super();
            this.registry=registry;
        }
        @Override
        public LuaValue get(LuaValue key) {
            var toReturn = this.registry.get(new Identifier(key.checkjstring()));
            return toReturn==null?NIL:new LuaSurfaceObj(toReturn);
        }

        @Override
        public Varargs next(LuaValue luaKey) {
            //so technically this is so funny and does not return a stable order
            var keys = this.registry.getIds().stream().sorted().toList();

            boolean ret = luaKey.isnil();
            for (var key : keys) {
                if (ret) return LuaString.valueOf(key.toString());
                if (luaKey.checkjstring().equals(key.toString())) ret = true;
            }
            return NIL;
        }
    }
    static private class RegistryRegistryAccessor extends RegistryAccessor{
        private final HashMap<String, RegistryAccessor> registries = new HashMap<>();
        public RegistryRegistryAccessor(Registry<?> registry) {
            super(registry);
        }
        @Override
        public LuaValue get(LuaValue luaKey) {
            var key = luaKey.checkjstring();
            if(!this.registries.containsKey(key)){
                var toPut = this.registry.get(new Identifier(key));
                this.registries.put(key, toPut==null?null:new RegistryAccessor((Registry<?>) toPut));
            }
            var toReturn = this.registries.get(key);
            return toReturn==null?NIL:toReturn;
        }
    }
}
