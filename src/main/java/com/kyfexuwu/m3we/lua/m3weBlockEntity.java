package com.kyfexuwu.m3we.lua;

import com.kyfexuwu.m3we.CustomBlockMaker;
import com.kyfexuwu.m3we.Utils;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.event.BlockPositionSource;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.listener.GameEventListener;

import static com.kyfexuwu.m3we.m3we.m3weBlockEntityType;

//todo: add serializing/deserializing
public class m3weBlockEntity extends BlockEntity implements GameEventListener {
    private final CustomScript scriptContainer;
    private final PositionSource positionSource;
    private boolean worldAndEnvSet=false;
    public final Identifier type;
    public m3weBlockEntity(BlockPos pos, BlockState state) {
        super(m3weBlockEntityType, pos, state);
        this.scriptContainer=new CustomScript(CustomBlockMaker.blockEntityScripts
                .get(Registry.BLOCK.getId(state.getBlock())));
        this.positionSource = new BlockPositionSource(pos);

        this.scriptContainer.setThis(this);
        this.scriptContainer.setStateWorldPos(state, null, pos);
        this.type = Registry.BLOCK.getId(state.getBlock());
    }
    private void setEnv(World world){
        if(this.worldAndEnvSet) return;
        String env = "none";
        if (world instanceof ServerWorld) env = "server";
        if (world instanceof ClientWorld) env = "client";
        this.scriptContainer.contextObj.javaSet("env", Utils.toLuaValue(env));
        this.worldAndEnvSet=true;
    }

    public static void tick(World world, BlockPos pos, BlockState state, BlockEntity blockEntityUncasted){
        m3weBlockEntity blockEntity = (m3weBlockEntity) blockEntityUncasted;
        blockEntity.scriptContainer.setStateWorldPos(state, world, pos);
        blockEntity.setEnv(world);

        Utils.tryAndExecute(blockEntity.scriptContainer,"tick",new Object[]{});
    }

    @Override
    public boolean shouldListenImmediately() {
        return Utils.tryAndExecute(false, this.scriptContainer, "shouldListenImmediately", new Object[]{},
                r-> r.isboolean() && r.checkboolean());
    }

    @Override
    public PositionSource getPositionSource() {
        return this.positionSource;//change maybe
    }

    @Override
    public int getRange() {
        return Utils.tryAndExecute(0, this.scriptContainer, "getListeningRange", new Object[]{},
                r-> r.isint() ? Math.max(r.checkint(), 0) : 0);
    }

    @Override
    public boolean listen(ServerWorld world, GameEvent.Message event) {
        this.scriptContainer.contextObj.javaSet("world", Utils.toLuaValue(world));
        this.setEnv(world);
        return Utils.tryAndExecute(false, this.scriptContainer, "listen", new Object[]{event},
                r-> r.isboolean() && r.checkboolean());
    }

    //--

    @Override
    protected void writeNbt(NbtCompound nbt) {
        //nbt.putString("lua", this.lua);
        super.writeNbt(nbt);
    }
    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        //this.lua = nbt.getString("lua");
    }
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}
