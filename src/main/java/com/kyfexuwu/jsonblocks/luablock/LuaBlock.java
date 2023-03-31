package com.kyfexuwu.jsonblocks.luablock;

import com.kyfexuwu.jsonblocks.JsonBlocks;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.lang.reflect.Field;

public class LuaBlock extends BlockWithEntity implements OperatorBlock {

    public LuaBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new LuaBlockEntity(pos, state);
    }

    @Override
    public void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock, BlockPos sourcePos, boolean notify) {
        if (world.isClient) {
            return;
        }
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof LuaBlockEntity && world.isReceivingRedstonePower(pos)) {
            //todo
            world.createAndScheduleBlockTick(pos, this, 1);
        }
    }

    public static Field widenedClient;
    static{
        try {
            widenedClient = ClientPlayerEntity.class.getDeclaredField("client");
            widenedClient.setAccessible(true);
        }catch(Exception ignored){}
    }
    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.getBlockEntity(pos) instanceof LuaBlockEntity && player.isCreativeLevelTwoOp()) {
            if(player instanceof ClientPlayerEntity){
                try{
                    ((MinecraftClient)widenedClient.get(player)).setScreen(
                            new LuaBlockScreen(pos,((LuaBlockEntity) world.getBlockEntity(pos)).getLua()));
                }catch(Exception ignored){}
            }
            return ActionResult.success(world.isClient);
        } else {
            return ActionResult.PASS;
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, JsonBlocks.luaBlockEntity, LuaBlockEntity::tick);
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        //todo: make this the return value
        return 0;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }
}