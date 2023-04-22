package com.kyfexuwu.m3we.luablock;

import com.kyfexuwu.m3we.m3we;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.ScriptError;
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
        super(m3we.luaBlockEntity, pos, state);
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

    boolean loaded=false;
    boolean errored=false;
    int currRevision =-1;
    public static void tick(World world, BlockPos pos, BlockState state, LuaBlockEntity blockEntity){
        if(!blockEntity.loaded){
            blockEntity.script.setSelf(blockEntity);
            blockEntity.script.updateScript(blockEntity.lua);
            blockEntity.loaded=true;
        }

        blockEntity.script.setSelf(blockEntity);
        if(blockEntity.currRevision==-1) blockEntity.currRevision=blockEntity.script.revision;

        //todo: when error, stop executing
        if(world.isClient) {
            ScriptError.execute(() -> {
                if(blockEntity.currRevision==blockEntity.script.revision&&blockEntity.errored){
                    return;
                }

                var clientTick = blockEntity.script.runEnv.get("clientTick");
                if(!clientTick.isnil())
                    clientTick.call();
                blockEntity.errored=false;
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
                if(blockEntity.currRevision==blockEntity.script.revision&&blockEntity.errored){
                    return;
                }

                var serverTick = blockEntity.script.runEnv.get("serverTick");
                if(!serverTick.isnil())
                    serverTick.call();
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
