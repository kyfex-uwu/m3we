package com.kyfexuwu.m3we.lua.api;

import com.kyfexuwu.m3we.Utils;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.JavaExclusiveTable;
import com.kyfexuwu.m3we.lua.dyngui.DynamicInventory;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

public class BlockEntityAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        JavaExclusiveTable thisApi = new JavaExclusiveTable();

        thisApi.javaSet("getEntityFromPos", new getEntityFromPos(env));

        thisApi.javaSet("NBTToInventory",MethodWrapper.create((NbtCompound nbt) -> {
            var items = nbt.getList("Items", NbtElement.COMPOUND_TYPE);
            DefaultedList<ItemStack> inv = DefaultedList.ofSize(
                    items.getCompound(items.size()-1).getByte("Slot")+1, ItemStack.EMPTY);
            Inventories.readNbt(nbt, inv);
            return new DynamicInventory(inv);
        }));
        thisApi.javaSet("writeInventory",MethodWrapper.create((Inventory inv, NbtCompound nbt) -> {
            DefaultedList<ItemStack> stacks = DefaultedList.of();
            for(int i=0;i<inv.size();i++)
                stacks.add(inv.getStack(i));
            Inventories.writeNbt(nbt, stacks);
            return null;
        }));

        return CustomScript.finalizeAPI("BlockEntity",thisApi,env);
    }
    static final class getEntityFromPos extends APIFunctions.TwoArgAPIFunc {
        public getEntityFromPos(LuaValue globals) {
            super(globals);
        }

        @Override
        public LuaValue call(LuaValue worldArg, LuaValue positionArg) {
            World world = Utils.toObjectTyped(worldArg);
            BlockPos position = Utils.toObjectTyped(positionArg);
            BlockEntity toReturn = null;
            try{
                toReturn=world.getBlockEntity(position);
            }catch(Exception ignored){}
            return Utils.toLuaValue(toReturn);
        }
    }
}
