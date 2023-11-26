package com.kyfexuwu.m3we.lua.api;

import com.kyfexuwu.m3we.Utils;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.JavaExclusiveTable;
import com.kyfexuwu.m3we.lua.ScriptError;
import com.kyfexuwu.m3we.lua.dyngui.DynamicGui;
import com.kyfexuwu.m3we.lua.dyngui.DynamicGuiHandler;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
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
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.*;

public class GuiAPI extends TwoArgFunction {
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        JavaExclusiveTable thisApi = new JavaExclusiveTable();
        thisApi.javaSet("openGui",new openGui(env));

        thisApi.javaSet("setBounds", new setBounds(env));
        thisApi.javaSet("rect",new drawRect(env));
        thisApi.javaSet("slot",new drawSlot(env));
        thisApi.javaSet("drawImg",new drawImg(env));
        thisApi.javaSet("text",new drawText(env));
        thisApi.javaSet("playerInventory",new drawInv(env));

        thisApi.javaSet("getSlot",new getSlot(env));
        thisApi.javaSet("doInWorld",new doInWorld(env));

        return CustomScript.finalizeAPI("Gui",thisApi,env);
    }

    private static final LuaError outOfContextError =
            new LuaError("You must be inside a GUI function to call this function");

    public static class DrawingProps{
        public int x=0;
        public int y=0;
        public int w=0;
        public int h=0;

        public int slotAmt=0;
    }
    static final class openGui extends TwoArgFunction {
        private final LuaValue globals;
        public openGui(LuaValue globals){
            this.globals=globals;
        }

        @Override
        public LuaValue call(LuaValue luaPlayer, LuaValue guiName) {
            PlayerEntity player;
            World world;
            BlockPos pos;
            String thisGui;
            try {
                var ctxTable=(JavaExclusiveTable) this.globals.get(CustomScript.contextIdentifier);
                if(!ctxTable.get("env").checkjstring().equals("server")) return FALSE;
                world = (World) Utils.toObject(ctxTable.get("world"));
                pos = (BlockPos) Utils.toObject(ctxTable.get("blockPos"));

                player = (PlayerEntity) Utils.toObject(luaPlayer);
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
        private final LuaValue globals;
        public setBounds(LuaValue globals){
            super();
            this.globals=globals;
        }

        @Override
        public LuaValue call(LuaValue w, LuaValue h){
            return ScriptError.execute(()->{
                var ctxTable=(JavaExclusiveTable) this.globals.get(CustomScript.contextIdentifier);
                var gui = (DynamicGui) Utils.toObject(ctxTable.get("guiData"));
                if(gui == null) throw outOfContextError;

                DynamicGui.props.x = (gui.width-w.checkint())/2;
                DynamicGui.props.y = (gui.height-h.checkint())/2;
                DynamicGui.props.w = w.checkint();
                DynamicGui.props.h = h.checkint();
            });
        }
    }
    static final class drawRect extends VarArgFunction {
        private final LuaValue globals;
        public drawRect(LuaValue globals){
            super();
            this.globals=globals;
        }

        @Override
        public Varargs invoke(Varargs args) {
            return ScriptError.execute(()->{
                var ctxTable=(JavaExclusiveTable) this.globals.get(CustomScript.contextIdentifier);
                var gui = (DynamicGui) Utils.toObject(ctxTable.get("guiData"));
                if(gui == null) throw outOfContextError;

                gui.componentsToDraw.add(new DynamicGui.RectGuiComponent(
                        args.arg(1).checkint(),
                        args.arg(2).checkint(),
                        args.arg(3).checkint(),
                        args.arg(4).checkint()
                ));
            });
        }
    }
    static final class drawSlot extends VarArgFunction {
        private final LuaValue globals;
        public drawSlot(LuaValue globals){
            super();
            this.globals=globals;
        }

        @Override
        public Varargs invoke(Varargs args) {
            LuaValue x = args.arg(1);
            LuaValue y = args.arg(2);
            LuaValue index = args.arg(3);
            LuaValue drawSlot = args.arg(4);
            var ctxTable=(JavaExclusiveTable) this.globals.get(CustomScript.contextIdentifier);
            var gui = (DynamicGui) Utils.toObject(ctxTable.get("guiData"));
            if(gui == null) throw outOfContextError;

            var success =  ScriptError.execute(()->{
                gui.componentsToDraw.add(new DynamicGui.SlotGuiComponent(
                        x.checkint(),y.checkint(),
                        index.checkint(), gui, false, drawSlot.isboolean()? (boolean) Utils.toObject(drawSlot) :true));
            });
            if(success.v){
                var toReturn=DynamicGui.props.slotAmt+(gui.handler.builder.hasPlayerInventory?36:0);
                DynamicGui.props.slotAmt++;
                return LuaValue.valueOf(toReturn);
            }
            return LuaValue.valueOf(-1);
        }
    }
    static final class drawImg extends VarArgFunction {
        private final LuaValue globals;
        public drawImg(LuaValue globals){
            super();
            this.globals=globals;
        }
        @Override
        public Varargs invoke(Varargs args) {
            try {
                var ctxTable=(JavaExclusiveTable) this.globals.get(CustomScript.contextIdentifier);
                var gui = (DynamicGui) Utils.toObject(ctxTable.get("guiData"));
                if(gui == null) throw outOfContextError;

                int narg = args.narg();
                if(!args.arg(1).isstring()) return NIL;
                for(int i=1;i<narg;i++)
                    if(!args.arg(i+1).isint()) return NIL;

                if(narg<5){
                    return NIL;
                }else if(narg<7){
                    draw(gui, args.arg(1).checkjstring(),
                            args.arg(2).checkint(),
                            args.arg(3).checkint(),
                            args.arg(4).checkint(),
                            args.arg(5).checkint());
                }else if (narg<9) {
                    draw(gui, args.arg(1).checkjstring(),
                            args.arg(2).checkint(),
                            args.arg(3).checkint(),
                            args.arg(4).checkint(),
                            args.arg(5).checkint(),
                            args.arg(6).checkint(),
                            args.arg(7).checkint());
                }else if (narg<11) {
                    draw(gui, args.arg(1).checkjstring(),
                            args.arg(2).checkint(),
                            args.arg(3).checkint(),
                            args.arg(4).checkint(),
                            args.arg(5).checkint(),
                            args.arg(6).checkint(),
                            args.arg(7).checkint(),
                            args.arg(8).checkint(),
                            args.arg(9).checkint());
                } else {
                    draw(gui, args.arg(1).checkjstring(),
                            args.arg(2).checkint(),
                            args.arg(3).checkint(),
                            args.arg(4).checkint(),
                            args.arg(5).checkint(),
                            args.arg(6).checkint(),
                            args.arg(7).checkint(),
                            args.arg(8).checkint(),
                            args.arg(9).checkint(),
                            args.arg(10).checkint(),
                            args.arg(11).checkint());
                }
            }catch(Exception ignored){}
            return NIL;
        }

        private static void draw(DynamicGui gui, String name, int tW, int tH, int x, int y, int w, int h, int sx, int sy, int sw, int sh){
            gui.componentsToDraw.add(new DynamicGui.ImgGuiComponent(name, tW, tH, x,y,w,h,sx,sy,sw,sh));
        }
        private static void draw(DynamicGui gui, String name, int tW, int tH, int x, int y, int sx, int sy, int sw, int sh){
            draw(gui, name, tW, tH, x,y,sw,sh,sx,sy,sw,sh);
        }
        private static void draw(DynamicGui gui, String name, int tW, int tH, int x, int y, int w, int h){
            draw(gui, name, tW, tH, x,y,w,h,0,0,tW, tH);
        }
        private static void draw(DynamicGui gui, String name, int tW, int tH, int x, int y){
            draw(gui, name, tW, tH, x,y, tW, tH,0,0,tW, tH);
        }
    }
    static final class drawText extends ThreeArgFunction {
        private final LuaValue globals;
        public drawText(LuaValue globals){
            super();
            this.globals=globals;
        }

        @Override
        public LuaValue call(LuaValue text, LuaValue x, LuaValue y) {
            return ScriptError.execute(()->{
                var ctxTable=(JavaExclusiveTable) this.globals.get(CustomScript.contextIdentifier);
                var gui = (DynamicGui) Utils.toObject(ctxTable.get("guiData"));
                if(gui == null) throw outOfContextError;

                gui.componentsToDraw.add(new DynamicGui.TextGuiComponent(
                        text.checkjstring(), x.checkint(), y.checkint(),gui));
            });
        }
    }

    static final class drawInv extends TwoArgFunction {
        private final LuaValue globals;
        public drawInv(LuaValue globals){
            super();
            this.globals=globals;
        }

        @Override
        public LuaValue call(LuaValue xP, LuaValue yP){
            return ScriptError.execute(()->{
                var ctxTable=(JavaExclusiveTable) this.globals.get(CustomScript.contextIdentifier);
                var gui = (DynamicGui) Utils.toObject(ctxTable.get("guiData"));
                if(gui == null) throw outOfContextError;

                gui.componentsToDraw.add(new DynamicGui.RectGuiComponent(
                        xP.checkint(),
                        yP.checkint(),
                        176,100
                ));
                gui.componentsToDraw.add(new DynamicGui.TextGuiComponent(
                        "Inventory",
                        xP.checkint()+8, yP.checkint()+6, gui
                ));
                int index=0;
                for(int y=0;y<3;y++) {
                    for (int x = 0; x < 9; x++) {
                        gui.componentsToDraw.add(new DynamicGui.SlotGuiComponent(
                                7+x*18 + xP.checkint(), 17+y*18 + yP.checkint(),
                                index, gui, true, true));
                        index++;
                    }
                }
                for (int i = 0; i < 9; i++) {
                    gui.componentsToDraw.add(new DynamicGui.SlotGuiComponent(
                            7+i*18 + xP.checkint(), 75 + yP.checkint(),
                            index, gui, true, true));
                    index++;
                }
            });
        }
    }

    static final class getSlot extends TwoArgFunction{
        private final LuaValue globals;
        public getSlot(LuaValue globals){
            this.globals=globals;
        }
        @Override
        public LuaValue call(LuaValue slotNum, LuaValue invType) {

            final Slot[] toReturn = new Slot[1];
            ScriptError.execute(()->{
                var ctxTable=(JavaExclusiveTable) this.globals.get(CustomScript.contextIdentifier);
                var handler = (DynamicGuiHandler) Utils.toObject(ctxTable.get("guiHandler"));
                if(handler == null) throw outOfContextError;

                int num = slotNum.checkint();
                if(!invType.checkjstring().equals("player")&&handler.builder.hasPlayerInventory)
                    num+=36;

                toReturn[0] = handler.slots.get(num);
            });
            if(toReturn[0] == null)
                return NIL;
            return Utils.toLuaValue(toReturn[0]);
        }
    }
    static final class doInWorld extends OneArgFunction{
        private final LuaValue globals;
        public doInWorld(LuaValue globals){
            this.globals=globals;
        }
        @Override
        public LuaValue call(LuaValue arg) {
            ScriptError.execute(()->{
                var ctxTable=(JavaExclusiveTable) this.globals.get(CustomScript.contextIdentifier);
                var handler = (DynamicGuiHandler) Utils.toObject(ctxTable.get("guiHandler"));
                if(handler == null) throw outOfContextError;

                handler.context.run((world, pos) ->
                        arg.call(Utils.toLuaValue(world),Utils.toLuaValue(pos)));
            });
            return NIL;
        }
    }
}
