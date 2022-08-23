package com.kyfexuwu.jsonblocks;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

import java.io.File;
import java.util.HashMap;

public class JsonBlocks implements ModInitializer {

    public static HashMap<String, Block> jsonBlocks= new HashMap<String, Block>();
    public static HashMap<String, Item> jsonItems= new HashMap<String, Item>();

    @Override
    public void onInitialize() {
        File JBFolder=new File(FabricLoader.getInstance().getConfigDir()//just inside the .minecraft folder
                .toString()+"\\jsonblocks-mods");
        JBFolder.mkdir();

        File blocksFolder = new File(JBFolder.getAbsolutePath()+"\\blocks");
        blocksFolder.mkdir();
        try {
            for (File modFile : blocksFolder.listFiles()) {
                switch(JBBlockIniter.blockFromFile(modFile)){
                    case CANT_READ -> System.out.println("Can't read the file!");
                    case BAD_JSON -> System.out.println("lol u messed up ur json");
                    case IDK -> System.out.println("Message me on discord KYFEX#3614 and tell me to fix my mod");
                    case YOU_DID_IT -> System.out.println("Registered block!");
                }
            }
        }catch(NullPointerException ignored){}

        File itemsFolder = new File(JBFolder.getAbsolutePath()+"\\items");
        itemsFolder.mkdir();
        try {
            for (File modFile : itemsFolder.listFiles()) {
                switch(JBItemIniter.itemFromFile(modFile)){
                    case CANT_READ -> System.out.println("Can't read the file!");
                    case BAD_JSON -> System.out.println("lol u messed up ur json");
                    case IDK -> System.out.println("Message me on discord KYFEX#3614 and tell me to fix my mod");
                    case YOU_DID_IT -> System.out.println("Registered item!");
                }
            }
        }catch(NullPointerException ignored){}
    }
}
