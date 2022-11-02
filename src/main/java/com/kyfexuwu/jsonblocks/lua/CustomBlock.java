package com.kyfexuwu.jsonblocks.lua;

import net.minecraft.block.Block;
import net.minecraft.state.property.Property;
import net.minecraft.util.shape.VoxelShape;

public abstract class CustomBlock extends Block {
    public static CustomScript scriptContainer;
    public static Property[] props;
    public static VoxelShape blockShape;
    public static boolean shapeIsScript;
    public static String shapeScriptString;

    public CustomBlock(Settings settings) {
        super(settings);
    }
}
