package com.kyfexuwu.jsonblocks;

import com.kyfexuwu.jsonblocks.lua.dyngui.DynamicGui;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;

import static com.kyfexuwu.jsonblocks.lua.dyngui.DynamicGuiHandler.dynamicGuiHandler;

@Environment(EnvType.CLIENT)
public class JsonBlocksClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        JsonBlocks.jsonBlocks.forEach((name,block)->{
            if(!block.getDefaultState().isOpaque())
                BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getCutout());
        });

        HandledScreens.register(dynamicGuiHandler, DynamicGui::new);
    }
}
