package com.kyfexuwu.m3we.luablock;

import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.ScriptError;
import com.kyfexuwu.m3we.m3we;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LuaBlockEntity extends BlockEntity {

    public LuaBlockEntity(BlockPos pos, BlockState state, boolean active) {
        super(m3we.luaBlockEntityType, pos, state);
        this.script = new LuaBlockScript(this);
        this.active=active;
    }

    private String lua = "";
    private boolean active;
    public LuaBlockScript script;
    public String getLua(){ return this.lua; }
    public boolean getActive() { return this.active; }
    public void setState(String lua, boolean active){
        this.active=active;
        this.setScript(lua);
    }
    public void setScript(String lua){ this.setScript(lua, false);}
    public void setScript(String lua, boolean firstLoad){
        this.lua=lua;
        if(!firstLoad){
            this.script.updateScript(lua);
            this.markDirty();
        }
    }

    boolean loaded=false;
    boolean errored=false;
    int currRevision =-1;
    public static void tick(World world, BlockPos pos, BlockState state, LuaBlockEntity blockEntity){
        if(world.isClient) return;
        if(!blockEntity.active) return;

        if(!blockEntity.loaded){
            blockEntity.script.updateScript(blockEntity.lua);
            blockEntity.loaded=true;
        }

        if(blockEntity.currRevision==-1) blockEntity.currRevision=blockEntity.script.revision;

        ScriptError.execute(() -> {
            if(blockEntity.currRevision==blockEntity.script.revision&&blockEntity.errored){
                return;
            }

            var serverTick = blockEntity.script.runEnv.get("tick");
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

            CustomScript.print(blockEntity.script.contextObj
                    .get(CustomScript.contextIdentifier).get("env").optjstring("none"),
                    "tick error: "+e.getMessage());
        });
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        nbt.putString("lua", this.lua);
        nbt.putBoolean("active", this.active);
        super.writeNbt(nbt);
    }
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.lua = nbt.getString("lua");
        this.active = nbt.getBoolean("active");
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
