package com.kyfexuwu.jsonblocks.lua.dyngui;

import com.kyfexuwu.jsonblocks.lua.api.RegistryAPI;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.registry.Registry;
import org.luaj.vm2.LuaFunction;

import java.lang.reflect.Field;
import java.util.List;

import static com.kyfexuwu.jsonblocks.JsonBlocks.MOD_ID;

public class DynamicGuiHandler extends ScreenHandler {
    public static final ExtendedScreenHandlerType<DynamicGuiHandler>  dynamicGuiHandler =
            Registry.register(Registry.SCREEN_HANDLER, new Identifier(MOD_ID, "m3we_gui"),
                    new ExtendedScreenHandlerType<>(DynamicGuiHandler::new));

    public final Pair<DynamicGuiBuilder, LuaFunction> gui;
    public DynamicInventory inv= new DynamicInventory();

    //client
    public DynamicGuiHandler(int syncId, PlayerInventory playerInventory, PacketByteBuf packet) {
        this(syncId, playerInventory, packet.readString());
    }
    //server
    public DynamicGuiHandler(int syncId, PlayerInventory playerInventory, String guiName) {
        super(dynamicGuiHandler, syncId);
        this.gui = RegistryAPI.getGui(guiName);

        var slots=this.gui.getLeft().slots;
        for(int i=0;i<slots.size();i++)
            this.addSlot(new Slot(inv, i,slots.get(i).getLeft(),slots.get(i).getRight()));//todo:position?
    }

    @Override
    public ItemStack transferSlot(PlayerEntity player, int index) {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;//todo
    }

    //--

    public static final Field listenersField;
    static{
        Field bruh=null;
        try{
            bruh=ScreenHandler.class.getDeclaredField("listeners");
            bruh.setAccessible(true);
        }catch(Exception e){
            System.out.println("cry 2");
        }
        listenersField=bruh;
    }
    private final List<ScreenHandlerListener> widenedListeners;
    {
        List<ScreenHandlerListener> bruh=null;
        try {
            bruh = (List<ScreenHandlerListener>) listenersField.get(this);
        }catch(Exception e){
            System.out.println("cry");
        }
        widenedListeners=bruh;
    }
    public void dataToClient(int id, int value){
        for (ScreenHandlerListener screenHandlerListener : this.widenedListeners) {
            screenHandlerListener.onPropertyUpdate(this, id, value);
        }
    }
    public void itemToClient(int id, ItemStack stack){
        for (ScreenHandlerListener screenHandlerListener : this.widenedListeners) {
            screenHandlerListener.onSlotUpdate(this, id, stack);
        }
    }
}
