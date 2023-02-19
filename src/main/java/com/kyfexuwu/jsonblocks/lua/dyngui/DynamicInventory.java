package com.kyfexuwu.jsonblocks.lua.dyngui;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Predicate;

//large todo
public class DynamicInventory implements Inventory {
    private final ArrayList<ItemStack> items = new ArrayList<>();

    @Override
    public int size() {
        return items.size();
    }
    public void setSize(int size){
        while(this.items.size()<size)
            this.items.add(new ItemStack(Blocks.AIR));
    }

    @Override
    public boolean isEmpty() {
        return items.size()==0;
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
            stack.decrement(slot);
        }
        return null;
    }

    @Override
    public ItemStack removeStack(int slot) {
        return null;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {

    }

    @Override
    public int getMaxCountPerStack() {
        return Inventory.super.getMaxCountPerStack();
    }

    @Override
    public void markDirty() {

    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return false;
    }

    @Override
    public void onOpen(PlayerEntity player) {
        Inventory.super.onOpen(player);
    }

    @Override
    public void onClose(PlayerEntity player) {
        Inventory.super.onClose(player);
    }

    @Override
    public boolean isValid(int slot, ItemStack stack) {
        return Inventory.super.isValid(slot, stack);
    }

    @Override
    public int count(Item item) {
        return Inventory.super.count(item);
    }

    @Override
    public boolean containsAny(Set<Item> items) {
        return Inventory.super.containsAny(items);
    }

    @Override
    public boolean containsAny(Predicate<ItemStack> predicate) {
        return Inventory.super.containsAny(predicate);
    }

    @Override
    public void clear() {

    }
}
