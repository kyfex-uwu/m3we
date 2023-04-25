package com.kyfexuwu.m3we.lua.api;

import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.ScriptError;
import net.minecraft.nbt.*;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.HashMap;
import java.util.Optional;

public class DatastoreAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        //new ServerWorld().getPersistentStateManager().set("m3we")
        env.set("Datastore", CustomScript.dataStore);
        env.get("package").get("loaded").set("Datastore", CustomScript.dataStore);
        return CustomScript.dataStore;
    }

    public static class DatastoreTable extends LuaTable{//TODO
        public DatastoreTable(){
            super();
        }

        HashMap<String, LuaValue> values = new HashMap<>();

        public static LuaValue fromNBTVal(NbtElement nbt){
            if(nbt instanceof NbtString) return LuaValue.valueOf(nbt.asString());
            else if(nbt instanceof NbtDouble) return LuaValue.valueOf(((NbtDouble) nbt).doubleValue());
            else if(nbt instanceof NbtByte) return LuaValue.valueOf(((NbtByte) nbt).byteValue()!=0);
            else if(nbt instanceof NbtCompound){
                var table = new LuaTable();

                var keys = ((NbtCompound)nbt).getKeys();
                keys.forEach(key->{
                    table.set(key, fromNBTVal(((NbtCompound) nbt).get(key)));
                });

                return table;
            }
            else return NONE;
        }
        public static Optional<NbtElement> fromLuaVal(LuaValue val){
            if(val instanceof LuaString) return Optional.of(NbtString.of(val.checkjstring()));
            else if(val instanceof LuaBoolean) return Optional.of(NbtByte.of(val.checkboolean()));
            else if(val instanceof LuaNumber) return Optional.of(NbtDouble.of(val.checkdouble()));
            else if(val instanceof LuaTable){
                var table = new NbtCompound();

                var key = LuaValue.NIL;
                while(true){
                    Varargs n = val.next(key);
                    if ((key = n.arg1()).isnil())
                        break;

                    var toPut=fromLuaVal(n.arg(2));
                    try {
                        if (toPut.isPresent())
                            table.put(n.arg(1).strvalue().checkjstring(), toPut.get());
                    }catch(Exception ignored){ /*if key is not str or num*/ }
                }

                return Optional.of(table);
            }
            return Optional.empty();
        }

        @Override
        public void set( int key, LuaValue value ) {
            this.rawset(key,value);
        }

        @Override
        public void set( LuaValue key, LuaValue value ) {
            this.rawset(key, value);
        }

        @Override
        public void rawset( int key, LuaValue value ) {
            this.rawset(LuaValue.valueOf(key), value);
        }

        @Override
        public void rawset( LuaValue key, LuaValue value ) {
            ScriptError.execute(()->{
                //todo: validate table/input
                if(key.isstring()||key.isnumber())
                    this.values.put(key.strvalue().checkjstring(), value);
                else throw new LuaError("key must be a string or number, and values must be" +
                        "string, boolean, number, or table");
            },(e)->{
                CustomScript.print("Couldn't set datastore value! "+e.getMessage());
            });
        }

        @Override
        public LuaValue get( int key ) {
            return this.rawget(key);
        }

        @Override
        public LuaValue get( LuaValue key ) {
            return this.rawget(key);
        }

        @Override
        public LuaValue rawget( int key ) {
            return this.rawget(LuaValue.valueOf(key));
        }

        @Override
        public LuaValue rawget( LuaValue key ) {
            var toReturn = this.values.get(key.checkjstring());
            return toReturn == null ? NIL : toReturn;
        }

        @Override
        public LuaValue remove(int pos) {
            throw new LuaError("please do not do this rn");
        }
    }
}
