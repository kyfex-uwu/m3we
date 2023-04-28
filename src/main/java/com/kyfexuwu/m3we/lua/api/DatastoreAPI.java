package com.kyfexuwu.m3we.lua.api;

import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.ScriptError;
import net.minecraft.nbt.*;
import net.minecraft.world.PersistentState;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.TwoArgFunction;

import java.util.HashMap;
import java.util.Optional;

public class DatastoreAPI extends TwoArgFunction {
    public static DatastoreManager currentManager;
    static{
        new DatastoreManager();
    }

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        env.set("Datastore", currentManager.table);
        env.get("package").get("loaded").set("Datastore", currentManager.table);
        return currentManager.table;
    }

    public static class DatastoreTable extends LuaTable{//TODO
        public DatastoreTable(){
            super();
        }

        HashMap<String, LuaValue> values = new HashMap<>();

        public static LuaValue fromNBTVal(NbtElement nbt, DatastoreTable tableToWrite){
            if(nbt instanceof NbtString) return LuaValue.valueOf(nbt.asString());
            else if(nbt instanceof NbtDouble) return LuaValue.valueOf(((NbtDouble) nbt).doubleValue());
            else if(nbt instanceof NbtByte) return LuaValue.valueOf(((NbtByte) nbt).byteValue()!=0);
            else if(nbt instanceof NbtInt) return NIL;
            else if(nbt instanceof NbtCompound){
                var table = tableToWrite==null ?
                        new DatastoreTable() : tableToWrite;

                var keys = ((NbtCompound)nbt).getKeys();
                keys.forEach(key->{
                    table.set(key, fromNBTVal(((NbtCompound) nbt).get(key), null));
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
            else if(val.isnil()) return Optional.of(NbtInt.of(0));
            else if(val instanceof DatastoreTable){
                var table = new NbtCompound();

                ((DatastoreTable)val).values.forEach((key, value) -> {
                    var toAssign = toNBT(value);
                    if(toAssign.isPresent())
                        table.put(key, toAssign.get());
                });

                return Optional.of(table);
            }else if(val instanceof LuaTable){
                var table = new NbtCompound();

                var key = LuaValue.NIL;
                while(true){
                    Varargs n = val.next(key);
                    if ((key = n.arg1()).isnil())
                        break;

                    var toPut=toNBT(n.arg(2));
                    if (toPut.isPresent() && (n.arg(1) instanceof LuaString || n.arg(1) instanceof LuaNumber))
                        table.put(n.arg(1).strvalue().checkjstring(), toPut.get());
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

        public static boolean validate(LuaValue val){
            if(val instanceof LuaBoolean || val instanceof LuaNumber || val instanceof LuaString || val.isnil())
                return true;
            if(val instanceof LuaTable){
                var key = LuaValue.NIL;
                while(true){
                    Varargs n = val.next(key);
                    key = n.arg1();
                    if (key.isnil())
                        break;

                    if(!(key instanceof LuaString || key instanceof LuaNumber))
                        return false;

                    if(!validate(n.arg(2))) return false;
                }
                return true;
            }
            return false;
        }
        public static LuaValue cleanup(LuaValue toClean){
            if(toClean instanceof LuaBoolean || toClean instanceof LuaNumber
                    || toClean instanceof LuaString || toClean.isnil())
                return toClean;

            var toReturn = new DatastoreTable();
            if(toClean instanceof LuaTable){
                var key = LuaValue.NIL;
                while(true){
                    Varargs n = toClean.next(key);
                    key = n.arg1();
                    if (key.isnil())
                        break;

                    toReturn.set(key, cleanup(n.arg(2)));
                }
                return toReturn;
            }

            return NIL;
        }
        @Override
        public void rawset( LuaValue key, LuaValue value ) {
            ScriptError.execute(()->{
                if((key.isstring()||key.isnumber())&&validate(value)) {
                    this.values.put(key.strvalue().checkjstring(), cleanup(value));
                    DatastoreAPI.currentManager.markDirty();
                }
                else throw new LuaError("key must be a string or number, and values must be " +
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

    public static class DatastoreManager extends PersistentState {
        public final DatastoreTable table;
        public DatastoreManager(){
            this.table = new DatastoreTable();
            DatastoreAPI.currentManager = this;
        }

        @Override
        public NbtCompound writeNbt(NbtCompound nbt) {
            System.out.println("Datastore saved");
            nbt.put("Data", this.table.toNBT().get()); // truss me
            return nbt;
        }

        public static DatastoreManager fromNBT(NbtCompound nbt){
            var toReturn = new DatastoreManager();
            DatastoreTable.fromNBTVal(nbt.get("Data"), DatastoreAPI.currentManager.table);
            return toReturn;
        }
    }
}
