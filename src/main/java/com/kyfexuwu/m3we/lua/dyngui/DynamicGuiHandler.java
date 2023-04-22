package com.kyfexuwu.m3we.lua.dyngui;

import com.kyfexuwu.m3we.lua.api.RegistryAPI;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import static com.kyfexuwu.m3we.m3we.MOD_ID;

public class DynamicGuiHandler extends ScreenHandler {
    public static final ExtendedScreenHandlerType<DynamicGuiHandler>  dynamicGuiHandler =
            Registry.register(Registry.SCREEN_HANDLER, new Identifier(MOD_ID, "m3we_gui"),
                    new ExtendedScreenHandlerType<>(DynamicGuiHandler::new));

    public final DynamicGuiBuilder gui;
    public DynamicInventory inv= new DynamicInventory();
    public ScreenHandlerContext context;

    //client
    public DynamicGuiHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf packet) {
        this(syncId, playerInventory, ScreenHandlerContext.EMPTY, packet.readString());
    }
    //server
    public DynamicGuiHandler(int syncId, PlayerInventory playerInventory, ScreenHandlerContext context, String guiName) {
        super(dynamicGuiHandler, syncId);
        this.gui = RegistryAPI.getGui(guiName);
        this.context = context;

        this.inv.setSize(this.gui.slotCount);
        this.inv.onOpen(playerInventory.player);

        if(this.gui.hasPlayerInventory){
            for(int i=0;i<27;i++)
                this.addSlot(new Slot(playerInventory,i+9,0,0));
            for(int i=0;i<9;i++)
                this.addSlot(new Slot(playerInventory,i,0,0));
        }
        for(int i=0;i<this.gui.slotCount;i++)
            this.addSlot(new Slot(this.inv,i,i*5,i%5*5));

        var thisRef=this;
        this.addListener(new ScreenHandlerListener() {
            @Override
            public void onSlotUpdate(ScreenHandler handler, int slotId, ItemStack stack) {
                thisRef.gui.guiBehavior.accept(thisRef);
            }

            @Override
            public void onPropertyUpdate(ScreenHandler handler, int property, int value) {

            }
        });
    }

    //--

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        return false;
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasStack()) {
            ItemStack itemStack2 = slot.getStack();
            itemStack = itemStack2.copy();
            if (index < 36 ?
                    !this.insertItem(itemStack2, 36, 36+this.gui.slotCount, false) :
                    !this.insertItem(itemStack2, 0, 36, true)) {
                return ItemStack.EMPTY;
            }
            if (itemStack2.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTakeItem(player, itemStack2);
        }
        return itemStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;//todo
    }

    @Override
    public void close(PlayerEntity player) {
        super.close(player);
        this.gui.onClose.accept(this,player);
    }
}
