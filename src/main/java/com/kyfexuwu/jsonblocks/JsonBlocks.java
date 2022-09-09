package com.kyfexuwu.jsonblocks;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.HashMap;

public class JsonBlocks implements ModInitializer {

    public static HashMap<String, Block> jsonBlocks= new HashMap<>();
    public static HashMap<String, Item> jsonItems= new HashMap<>();

    public static File JBFolder =new File(FabricLoader.getInstance().getConfigDir()//just inside the .minecraft folder
            .toString()+"\\jsonblocks-mods");

    @Override
    public void onInitialize() {
        JBFolder.mkdir();

        File blocksFolder = new File(JBFolder.getAbsolutePath()+"\\blocks");
        blocksFolder.mkdir();
        File itemsFolder = new File(JBFolder.getAbsolutePath()+"\\items");
        itemsFolder.mkdir();
        File scriptsFolder = new File(JBFolder.getAbsolutePath()+"\\scripts");
        scriptsFolder.mkdir();

        for (File modFile : blocksFolder.listFiles()) {
            if(FilenameUtils.isExtension("json")) {
                switch (JBBlockIniter.blockFromFile(modFile)) {
                    case CANT_READ -> System.out.println("Can't read the file!");
                    case BAD_JSON -> System.out.println("lol u messed up ur json");
                    case IDK -> System.out.println("Message me on discord KYFEX#3614 and tell me to fix my mod");
                    case YOU_DID_IT -> System.out.println("Registered block!");
                }
            }
        }
        for (File modFile : itemsFolder.listFiles()) {
            if(FilenameUtils.isExtension("json")) {
                switch(JBItemIniter.itemFromFile(modFile)){
                    case CANT_READ -> System.out.println("Can't read the file!");
                    case BAD_JSON -> System.out.println("lol u messed up ur json");
                    case IDK -> System.out.println("Message me on discord KYFEX#3614 and tell me to fix my mod");
                    case YOU_DID_IT -> System.out.println("Registered item!");
                }
            }
        }
    }
}
