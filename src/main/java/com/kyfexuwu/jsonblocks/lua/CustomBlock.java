package com.kyfexuwu.jsonblocks.lua;

import net.minecraft.block.Block;

public abstract class CustomBlock extends Block {
    public static CustomScript scriptContainer;
    public CustomBlock(Settings settings) {
        super(settings);
    }
}
