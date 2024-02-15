package com.kyfexuwu.m3we.lua.api;

import com.kyfexuwu.m3we.Utils;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.ScriptError;
import net.minecraft.nbt.*;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class DatastoreAPI extends TwoArgFunction {
    public static DatastoreTable table = new DatastoreTable();

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        return CustomScript.finalizeAPI("Datastore",table,env);
    }

    public static class DatastoreTable extends LuaTable{//TODO
        public DatastoreTable(){
            super();
        }

        HashMap<String, LuaValue> values = new HashMap<>();

        public static LuaValue fromNBTVal(NbtElement nbt, LuaTable tableToWrite){
            if(nbt instanceof NbtString) return LuaValue.valueOf(nbt.asString());
            else if(nbt instanceof NbtDouble) return LuaValue.valueOf(((NbtDouble) nbt).doubleValue());
            else if(nbt instanceof NbtFloat) return LuaValue.valueOf(((NbtFloat) nbt).doubleValue());
            else if(nbt instanceof NbtInt) return LuaValue.valueOf(((NbtInt) nbt).doubleValue());
            else if(nbt instanceof NbtByte) return LuaValue.valueOf(((NbtByte) nbt).doubleValue());
            else if(nbt instanceof NbtCompound){
                var table = tableToWrite==null ?
                        new DatastoreTable() : tableToWrite;

                var keys = ((NbtCompound)nbt).getKeys();
                keys.forEach(key->{
                    LuaTable tableToGive=null;
                    if(tableToWrite!=null)
                        try {
                            tableToGive = tableToWrite.getClass().getConstructor().newInstance();
                        } catch (Exception ignored) {}
                    table.set(key, fromNBTVal(((NbtCompound) nbt).get(key), tableToGive));
                });

                return table;
            }
            else return NIL;
        }
        public Optional<NbtElement> toNBT(){ return toNBT(this); }
        public static Optional<NbtElement> toNBT(LuaValue val){
            if(val instanceof LuaString) return Optional.of(NbtString.of(val.checkjstring()));
            else if(val instanceof LuaBoolean) return Optional.of(NbtByte.of(val.checkboolean()));
            else if(val instanceof LuaNumber) return Optional.of(NbtDouble.of(val.checkdouble()));
            else if(val instanceof DatastoreTable){
                var table = new NbtCompound();

                ((DatastoreTable)val).values.forEach((key, value) -> {
                    var toAssign = toNBT(value);
                    toAssign.ifPresent(nbtElement ->
                            table.put(key, nbtElement));
                });

                return Optional.of(table);
            }else if(val instanceof LuaTable){
                var table = new NbtCompound();

                Utils.forEach((LuaTable) val, (key, value)->{
                    var toPut=toNBT(value);
                    if (toPut.isPresent() && (key instanceof LuaString || key instanceof LuaNumber))
                        table.put(key.strvalue().checkjstring(), toPut.get());

                    return null;
                });

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

        public static boolean validate(LuaValue val){
            if(val instanceof LuaBoolean || val instanceof LuaNumber || val instanceof LuaString || val.isnil())
                return true;
            if(val instanceof LuaTable){
                AtomicBoolean toReturn = new AtomicBoolean(true);
                Utils.forEach((LuaTable) val, (key, value)->{
                    if(!(key instanceof LuaString || key instanceof LuaNumber)){
                        toReturn.set(false);
                        return false;
                    }

                    if(!validate(value)) {
                        toReturn.set(false);
                        return false;
                    }

                    return null;
                });
                return toReturn.get();
            }
            return false;
        }
        public static LuaValue cleanup(LuaValue toClean){
            if(toClean instanceof LuaBoolean || toClean instanceof LuaNumber
                    || toClean instanceof LuaString || toClean instanceof DatastoreTable
                    || toClean.isnil())
                return toClean;

            var toReturn = new DatastoreTable();
            if(toClean instanceof LuaTable){
                Utils.forEach((LuaTable) toClean, (key, value)->{
                    toReturn.set(key, cleanup(value));
                    return null;
                });
                return toReturn;
            }

            return NIL;
        }
        @Override
        public void rawset( LuaValue key, LuaValue value ) {
            ScriptError.execute(()->{
                if((key.isstring()||key.isnumber())&&validate(value)) {
                    this.values.put(key.strvalue().checkjstring(), cleanup(value));
                }
                else throw new LuaError("key must be a string or number, and values must be " +
                        "string, boolean, number, or table");
            },(e)->{
                CustomScript.print("client","Couldn't set datastore value! "+e.getMessage());
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
