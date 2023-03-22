package com.kyfexuwu.jsonblocks.luablock;

import com.kyfexuwu.jsonblocks.JsonBlocks;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.Generic3x3ContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

public class LuaBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
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
    public void writeNbt(NbtCompound nbt) {
        nbt.putString("lua", this.lua);

        super.writeNbt(nbt);
    }
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        lua = nbt.getString("lua");
    }

    //--

    public static ScreenHandlerType<LuaBlockScreenHandler> LuaBlockScreenHandlerType =
            new ScreenHandlerType<>(LuaBlockScreenHandler::new);
    public static class LuaBlockScreenHandler extends ScreenHandler{

        //this is opening
        public LuaBlockScreenHandler(int syncId, PlayerInventory inv) {
            super(LuaBlockScreenHandlerType, syncId);
        }

        @Override
        public ItemStack transferSlot(PlayerEntity player, int index) {
            return null;
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return true;
        }
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new LuaBlockScreenHandler(syncId,null);
    }
    @Override
    public Text getDisplayName() {
        return Text.translatable("m3we.lua_script");
    }
}
