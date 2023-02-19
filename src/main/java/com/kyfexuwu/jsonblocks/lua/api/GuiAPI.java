package com.kyfexuwu.jsonblocks.lua.api;

import com.kyfexuwu.jsonblocks.Utils;
import com.kyfexuwu.jsonblocks.lua.dyngui.DynamicGuiBuilder;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;

public class GuiAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable thisApi = new LuaTable();
        thisApi.set("new",new newGui());
        thisApi.set("open",new openGui());

        env.set("Gui", thisApi);
        env.get("package").get("loaded").set("Gui", thisApi);
        return thisApi;
    }

    static final class newGui extends TwoArgFunction {
        @Override
        public LuaValue call(LuaValue width, LuaValue height) {
            return Utils.toLuaValue(new DynamicGuiBuilder(width.checkint(), height.checkint()));
        }
    }
    static final class openGui extends ThreeArgFunction {
        @Override
        public LuaValue call(LuaValue luaPlayer, LuaValue guiName, LuaValue stateWorldPos) {
            if(!stateWorldPos.istable()||stateWorldPos.length()<3) return FALSE;

            PlayerEntity player;
            BlockState state;
            World world;
            BlockPos pos;
            String thisGui;
            try {
                player = (PlayerEntity) Utils.toObject(luaPlayer);
                state = (BlockState) Utils.toObject(stateWorldPos.get(1));
                world = (World) Utils.toObject(stateWorldPos.get(2));
                pos = (BlockPos) Utils.toObject(stateWorldPos.get(3));
                thisGui = guiName.checkjstring();
            }catch(Exception e){
                return FALSE;
            }

            try {
                player.openHandledScreen(new ExtendedScreenHandlerFactory() {
                    @Nullable
                    @Override
                    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
                        return RegistryAPI.getGui(thisGui).getLeft().build(syncId,inv,player,thisGui);
                    }

                    @Override
                    public Text getDisplayName() {
                        return Text.of("Custom Interface");
                    }

                    @Override
                    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
                        buf.writeString(thisGui);
                    }
                });
            }catch(Exception e){
                e.printStackTrace();
            }
            return TRUE;
        }
    }
}
