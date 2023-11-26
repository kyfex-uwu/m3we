package com.kyfexuwu.m3we;

import com.kyfexuwu.m3we.lua.dyngui.DynamicGui;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;

import static com.kyfexuwu.m3we.lua.dyngui.DynamicGuiHandler.dynamicGuiHandler;

@Environment(EnvType.CLIENT)
public class m3weClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        m3we.m3weBlocks.forEach((name, block)->{
            if(!block.getDefaultState().isOpaque())
                BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getCutout());
        });

        HandledScreens.register(dynamicGuiHandler, DynamicGui::new);
    }
}
