package com.kyfexuwu.jsonblocks.lua.api;

import com.kyfexuwu.jsonblocks.Utils;
import com.kyfexuwu.jsonblocks.lua.ScriptError;
import com.kyfexuwu.jsonblocks.lua.dyngui.DynamicGui;
import com.kyfexuwu.jsonblocks.lua.dyngui.DynamicGuiHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.*;

public class GuiAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        APITable thisApi = new APITable();
        thisApi.set("openGui",new openGui());

        var dProps = new DrawingProps();
        var hProps = new HandlerProps();

        thisApi.set("registerContext",new registerContext(dProps,hProps));

        thisApi.set("setBounds", new setBounds(dProps));
        thisApi.set("rect",new drawRect(dProps));
        thisApi.set("slot",new drawSlot(dProps));
        thisApi.set("text",new drawText(dProps));
        thisApi.set("playerInventory",new drawInv(dProps));

        thisApi.set("getSlot",new getSlot(hProps));
        thisApi.set("doInWorld",new doInWorld(hProps));

        thisApi.locked = true;
        env.set("Gui", thisApi);
        env.get("package").get("loaded").set("Gui", thisApi);
        return thisApi;
    }

    public static class DrawingProps{
        DynamicGui gui;
        public int x=0;
        public int y=0;
        public int w=0;
        public int h=0;

        public int slotAmt=0;
    }
    public static class HandlerProps{
        DynamicGuiHandler handler;
    }

    static final class registerContext extends OneArgFunction {
        final DrawingProps dProps;
        final HandlerProps hProps;
        public registerContext(DrawingProps dProps, HandlerProps hProps){
            super();
            this.dProps=dProps;
            this.hProps=hProps;
        }

        @Override
        public LuaValue call(LuaValue gui){
            return ScriptError.execute(()->{
                Object toSet = Utils.toObject(gui);

                if(toSet instanceof DynamicGui) {
                    this.dProps.gui = (DynamicGui) toSet;
                    this.dProps.gui.props = this.dProps;
                }else if(toSet instanceof DynamicGuiHandler){
                    this.hProps.handler = (DynamicGuiHandler) Utils.toObject(gui);
                }
            });
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
                        return RegistryAPI.getGui(thisGui).build(syncId,inv,player,world,pos,thisGui);
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

    static final class setBounds extends TwoArgFunction {
        final DrawingProps props;
        public setBounds(DrawingProps props){
            super();
            this.props=props;
        }

        @Override
        public LuaValue call(LuaValue w, LuaValue h){
            return ScriptError.execute(()->{
                this.props.x = (this.props.gui.width-w.checkint())/2;
                this.props.y = (this.props.gui.height-h.checkint())/2;
                this.props.w = w.checkint();
                this.props.h = h.checkint();
            });
        }
    }
    static final class drawRect extends VarArgFunction {
        final DrawingProps props;
        public drawRect(DrawingProps props){
            super();
            this.props=props;
        }

        @Override
        public Varargs invoke(Varargs args) {
            return ScriptError.execute(()->{
                this.props.gui.componentsToDraw.add(new DynamicGui.RectGuiComponent(
                        args.arg(1).checkint(),
                        args.arg(2).checkint(),
                        args.arg(3).checkint(),
                        args.arg(4).checkint()
                ));
            });
        }
    }
    static final class drawSlot extends ThreeArgFunction {
        final DrawingProps props;
        public drawSlot(DrawingProps props){
            super();
            this.props=props;
        }

        @Override
        public LuaValue call(LuaValue x, LuaValue y, LuaValue index){
            var success =  ScriptError.execute(()->{
                this.props.gui.componentsToDraw.add(new DynamicGui.SlotGuiComponent(
                        x.checkint(),y.checkint(),
                        index.checkint(), this.props.gui, false));
            });
            if(success.v){
                var toReturn=this.props.slotAmt+(this.props.gui.handler.gui.hasPlayerInventory?36:0);
                this.props.slotAmt++;
                return LuaValue.valueOf(toReturn);
            }
            return LuaValue.valueOf(-1);
        }
    }
    static final class drawText extends ThreeArgFunction {
        final DrawingProps props;
        public drawText(DrawingProps props){
            super();
            this.props=props;
        }

        @Override
        public LuaValue call(LuaValue text, LuaValue x, LuaValue y) {
            return ScriptError.execute(()->{
                this.props.gui.componentsToDraw.add(new DynamicGui.TextGuiComponent(
                        text.checkjstring(), x.checkint(), y.checkint(),this.props.gui));
            });
        }
    }

    static final class drawInv extends TwoArgFunction {
        final DrawingProps props;
        public drawInv(DrawingProps props){
            super();
            this.props=props;
        }

        @Override
        public LuaValue call(LuaValue xP, LuaValue yP){
            return ScriptError.execute(()->{
                this.props.gui.componentsToDraw.add(new DynamicGui.RectGuiComponent(
                        xP.checkint(),
                        yP.checkint(),
                        176,100
                ));
                this.props.gui.componentsToDraw.add(new DynamicGui.TextGuiComponent(
                        "Inventory",
                        xP.checkint()+8, yP.checkint()+6, this.props.gui
                ));
                int index=0;
                for(int y=0;y<3;y++) {
                    for (int x = 0; x < 9; x++) {
                        this.props.gui.componentsToDraw.add(new DynamicGui.SlotGuiComponent(
                                7+x*18 + xP.checkint(), 17+y*18 + yP.checkint(),
                                index, this.props.gui, true));
                        index++;
                    }
                }
                for (int i = 0; i < 9; i++) {
                    this.props.gui.componentsToDraw.add(new DynamicGui.SlotGuiComponent(
                            7+i*18 + xP.checkint(), 75 + yP.checkint(),
                            index, this.props.gui, true));
                    index++;
                }
            });
        }
    }

    static final class getSlot extends TwoArgFunction{
        final HandlerProps props;
        public getSlot(HandlerProps props){
            super();
            this.props=props;
        }

        @Override
        public LuaValue call(LuaValue slotNum, LuaValue invType) {

            final Slot[] toReturn = new Slot[1];
            ScriptError.execute(()->{
                if(!invType.checkjstring().equals("player")&&this.props.handler.gui.hasPlayerInventory)
                        slotNum.add(36);

                toReturn[0] = this.props.handler.slots.get(slotNum.checkint());
            });
            if(toReturn[0] == null)
                return NIL;
            return Utils.toLuaValue(toReturn[0]);
        }
    }
    static final class doInWorld extends OneArgFunction{
        final HandlerProps props;
        public doInWorld(HandlerProps props){
            super();
            this.props=props;
        }

        @Override
        public LuaValue call(LuaValue arg) {
            ScriptError.execute(()->{
                this.props.handler.context.run((world, pos) ->
                        arg.call(Utils.toLuaValue(world),Utils.toLuaValue(pos)));
            });
            return NIL;
        }
    }
}
