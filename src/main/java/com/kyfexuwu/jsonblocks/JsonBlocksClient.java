package com.kyfexuwu.jsonblocks;

import com.kyfexuwu.jsonblocks.luablock.LuaBlockScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

import static com.kyfexuwu.jsonblocks.JsonBlocks.luaBlockScreenHandler;

@Environment(EnvType.CLIENT)
public class JsonBlocksClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(luaBlockScreenHandler, LuaBlockScreen::new);
    }
}
