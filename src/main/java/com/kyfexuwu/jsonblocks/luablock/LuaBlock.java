package com.kyfexuwu.jsonblocks.luablock;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CommandBlockScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

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

    /*
    @Override
    public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity instanceof CommandBlockBlockEntity) {
            CommandBlockBlockEntity commandBlockBlockEntity = (CommandBlockBlockEntity)blockEntity;
            CommandBlockExecutor commandBlockExecutor = commandBlockBlockEntity.getCommandExecutor();
            boolean bl = !StringHelper.isEmpty(commandBlockExecutor.getCommand());
            CommandBlockBlockEntity.Type type = commandBlockBlockEntity.getCommandBlockType();
            boolean bl2 = commandBlockBlockEntity.isConditionMet();
            if (type == CommandBlockBlockEntity.Type.AUTO) {
                commandBlockBlockEntity.updateConditionMet();
                if (bl2) {
                    this.execute(state, world, pos, commandBlockExecutor, bl);
                } else if (commandBlockBlockEntity.isConditionalCommandBlock()) {
                    commandBlockExecutor.setSuccessCount(0);
                }
                if (commandBlockBlockEntity.isPowered() || commandBlockBlockEntity.isAuto()) {
                    world.createAndScheduleBlockTick(pos, this, 1);
                }
            } else if (type == CommandBlockBlockEntity.Type.REDSTONE) {
                if (bl2) {
                    this.execute(state, world, pos, commandBlockExecutor, bl);
                } else if (commandBlockBlockEntity.isConditionalCommandBlock()) {
                    commandBlockExecutor.setSuccessCount(0);
                }
            }
            world.updateComparators(pos, this);
        }
    }
     */

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
                    ((MinecraftClient)widenedClient.get(player)).setScreen(new LuaBlockScreen());
                }catch(Exception ignored){}
            }
            return ActionResult.success(world.isClient);
        } else {
            return ActionResult.PASS;
        }
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