package com.kyfexuwu.jsonblocks.luablock;

import com.kyfexuwu.jsonblocks.JsonBlocks;
import com.kyfexuwu.jsonblocks.lua.api.RegistryAPI;
import com.kyfexuwu.jsonblocks.lua.dyngui.DynamicGuiHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.Lua;

import static com.kyfexuwu.jsonblocks.JsonBlocks.MOD_ID;

public class LuaBlockEntity extends BlockEntity{
    public LuaBlockEntity(BlockPos pos, BlockState state) {
        super(JsonBlocks.luaBlockEntity, pos, state);
    }

    private String lua = "";
    public String getLua(){
        return this.lua;
    }
    public void setLua(String lua){
        this.lua=lua;
        markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);

        nbt.putString("lua", this.lua);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        this.lua = nbt.getString("lua");
    }

    @Override
    public boolean copyItemDataRequiresOperator() {
        return true;
    }
}
