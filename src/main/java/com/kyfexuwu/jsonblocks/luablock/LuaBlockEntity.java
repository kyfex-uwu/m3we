package com.kyfexuwu.jsonblocks.luablock;

import com.kyfexuwu.jsonblocks.JsonBlocks;
import com.kyfexuwu.jsonblocks.Utils;
import com.kyfexuwu.jsonblocks.lua.CustomScript;
import com.kyfexuwu.jsonblocks.lua.ScriptError;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LuaBlockEntity extends BlockEntity {
    public LuaBlockEntity(BlockPos pos, BlockState state) {
        super(JsonBlocks.luaBlockEntity, pos, state);
        this.script = new LuaBlockScript(this);
    }

    private String lua = "";
    public LuaBlockScript script;
    public String getLua(){
        return this.lua;
    }
    public void setLua(String lua){
        this.lua=lua;
        this.script.updateScript(lua);
        this.markDirty();
    }

    boolean errored=false;
    int currRevision =-1;
    public static void tick(World world, BlockPos pos, BlockState state, LuaBlockEntity blockEntity){
        blockEntity.script.setSelf(blockEntity);
        if(blockEntity.currRevision==-1) blockEntity.currRevision=blockEntity.script.revision;

        //todo: when error, stop executing
        if(world.isClient) {
            ScriptError.execute(() -> {
                blockEntity.script.runEnv.get("clientTick").call(Utils.toLuaValue(blockEntity),Utils.toLuaValue(world));
            }, (e) -> {
                if(blockEntity.currRevision!=blockEntity.script.revision&&blockEntity.errored){
                    blockEntity.errored=false;
                    blockEntity.currRevision=blockEntity.script.revision;
                    return;
                }
                if (blockEntity.errored) return;
                blockEntity.errored=true;

                CustomScript.print("clientTick: "+e.getMessage());
            });
        }else{
            ScriptError.execute(() -> {
                blockEntity.script.runEnv.get("serverTick").call(Utils.toLuaValue(blockEntity),Utils.toLuaValue(world));
                blockEntity.errored = false;
            }, (e) -> {
                if(blockEntity.currRevision!=blockEntity.script.revision&&blockEntity.errored){
                    blockEntity.errored=false;
                    blockEntity.currRevision=blockEntity.script.revision;
                    return;
                }
                if (blockEntity.errored) return;
                blockEntity.errored=true;

                CustomScript.print("serverTick: "+e.getMessage());
            });
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.putString("lua", this.lua);
        super.writeNbt(nbt);
    }
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.lua = nbt.getString("lua");
    }
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    @Override
    public boolean copyItemDataRequiresOperator() {
        return true;
    }
}
