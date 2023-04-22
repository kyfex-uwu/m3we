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
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        //new ServerWorld().getPersistentStateManager().set("m3we")
        env.set("Datastore", CustomScript.dataStore);
        env.get("package").get("loaded").set("Datastore", CustomScript.dataStore);
        return CustomScript.dataStore;
    }

    static class DatastoreTable extends LuaTable{//TODO
        public DatastoreTable(){
            super();
        }

        HashMap<String, NbtElement> entries = new HashMap<>();

        public LuaValue fromNBTVal(NbtElement nbt){
            if(nbt instanceof NbtString) return LuaValue.valueOf(nbt.asString());
            else if(nbt instanceof NbtDouble) return LuaValue.valueOf(((NbtDouble) nbt).doubleValue());
            else if(nbt instanceof NbtByte) return LuaValue.valueOf(((NbtByte) nbt).byteValue()!=0);
            else return NONE;
        }
        public Optional<NbtElement> fromLuaVal(LuaValue val){
            if(val instanceof LuaString) return Optional.of(NbtString.of(val.checkjstring()));
            else if(val instanceof LuaBoolean) return Optional.of(NbtByte.of(val.checkboolean()));
            else if(val instanceof LuaNumber) return Optional.of(NbtDouble.of(val.checkdouble()));
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
                var toSet = this.fromLuaVal(key);
                if(toSet.isPresent()) this.entries.put(key.checkjstring(),toSet.get());
                else argerror("string, boolean, or number");
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
            return fromNBTVal(this.entries.get(key.checkjstring()));
        }

        @Override
        public LuaValue remove(int pos) {
            if(this.entries.containsKey(pos+""))
                return fromNBTVal(this.entries.remove(pos + ""));
            return NONE;
        }
    }

    static class DatastoreState extends PersistentState{
        public LuaTable dataStore;
        @Override
        public NbtCompound writeNbt(NbtCompound nbt) {

            return nbt;
        }

        public static DatastoreState createFromNbt(NbtCompound tag) {
            DatastoreState state = new DatastoreState();
            state.dataStore = toTable(tag);
            return state;
        }

        public static NbtCompound toNBT(DatastoreTable table){
            var toReturn = new NbtCompound();

            for(LuaValue key : table.keys()){
                //string
                //boolean (byte)
                //number
            }

            return toReturn;
        }
        public static LuaTable toTable(NbtCompound tag){
            var toReturn = new LuaTable();

            return toReturn;
        }
    }
}
