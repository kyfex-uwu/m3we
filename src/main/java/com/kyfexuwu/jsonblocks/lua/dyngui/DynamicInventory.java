package com.kyfexuwu.jsonblocks.lua.dyngui;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.InventoryChangedListener;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;

public class DynamicInventory implements Inventory {
    private final ArrayList<ItemStack> items = new ArrayList<>();
    private ArrayList<InventoryChangedListener> listeners = new ArrayList<>();

    //todo: size property

    @Override
    public int size() {
        return items.size();
    }
    public void setSize(int size){
        while(this.items.size()<size)
            this.items.add(ItemStack.EMPTY);
    }

    @Override
    public boolean isEmpty() {
        if(this.items.size()==0) return true;
        for(ItemStack item : this.items)
            if(!item.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getStack(int slot) {
        if(this.items.size()>slot&&slot>=0) return this.items.get(slot);
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        if(this.items.size()>slot&&slot>=0){
            var stack=this.items.get(slot);
            var toReturn=stack.copy();
            stack.decrement(amount);
            toReturn.setCount(toReturn.getCount()-stack.getCount());
            this.markDirty();
            return toReturn;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStack(int slot) {
        if(this.items.size()>slot&&slot>=0){
            var stack=this.items.get(slot);
            var toReturn=stack.copy();
            stack.setCount(0);
            this.markDirty();
            return toReturn;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        this.markDirty();
    }

    @Override
    public int getMaxCountPerStack() {
        return 64;
    }

    @Override
    public void markDirty() {
        if (this.listeners != null) {
            for (InventoryChangedListener inventoryChangedListener : this.listeners) {
                inventoryChangedListener.onInventoryChanged(this);
            }
        }
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void clear() {
        this.items.clear();
        this.markDirty();
    }
}
