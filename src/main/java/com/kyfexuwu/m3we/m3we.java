package com.kyfexuwu.m3we;

import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.Translations;
import com.kyfexuwu.m3we.luablock.*;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;

public class m3we implements ModInitializer {
    public static String MOD_ID = "m3we";
    public static Logger LOGGER = LogUtils.getLogger();

    public static Block luaBlock = new LuaBlock(FabricBlockSettings.copyOf(Blocks.COMMAND_BLOCK));
    public static final BlockEntityType<LuaBlockEntity> luaBlockEntity = Registry.register(
            Registry.BLOCK_ENTITY_TYPE,
            new Identifier(MOD_ID, "lua_block"),
            FabricBlockEntityTypeBuilder.create((pos,state)->
                    new LuaBlockEntity(pos,state,false),luaBlock).build()
    );
    static{
        Registry.register(Registry.BLOCK, new Identifier(MOD_ID, "lua_block"),luaBlock);
        Registry.register(Registry.ITEM, new Identifier(MOD_ID, "lua_block"),
                new BlockItem(luaBlock, new FabricItemSettings()));
        //.group(ItemGroup.OPERATOR);//1.20
    }

    public static HashMap<String, Block> jsonBlocks= new HashMap<>();
    public static HashMap<String, Item> jsonItems= new HashMap<>();

    public static File JBFolder = new File(FabricLoader.getInstance().getConfigDir()//just inside the .minecraft folder
            .toString()+"\\m3we");
    public static File blocksFolder = new File(JBFolder.getAbsolutePath()+"\\blocks");
    public static File itemsFolder = new File(JBFolder.getAbsolutePath()+"\\items");
    public static File scriptsFolder = new File(JBFolder.getAbsolutePath()+"\\scripts");

    public static final ItemGroup m3weItems = FabricItemGroupBuilder.build(
            new Identifier(MOD_ID,"item_group"),
            ()->new ItemStack(Blocks.BEACON)
    );

    public static final Identifier updateLuaBlockPacket = new Identifier("m3we","update_lua_block");
    public static final Identifier askForLuaCodePacket = new Identifier("m3we","get_lua_code");
    public static final Identifier giveLuaCodePacket = new Identifier("m3we","give_lua_code");

    @Override
    public void onInitialize() {
        Translations.init();

        JBFolder.mkdir();
        blocksFolder.mkdir();
        itemsFolder.mkdir();
        scriptsFolder.mkdir();

        initObjects(blocksFolder, BlockIniter::blockFromFile,"");
        initObjects(itemsFolder, ItemIniter::itemFromFile,"");

        //--

        try {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            scriptsFolder.toPath().register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
            Thread watcherThread = new Thread(null, () -> {
                try {
                    while (true) {
                        WatchKey key = watcher.take();//blocking
                        for (WatchEvent<?> event : key.pollEvents()) {
                            if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                                CustomScript.reloadScript(((Path)event.context()).toString());
                            }
                        }
                        if (!key.reset()) {
                            //ono this is invalid
                            System.out.println("unable to can (this is a bad message)");
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    System.out.println("oof ouch interrupted");
                }
            });
            watcherThread.start();
        } catch (IOException e) {
            System.out.println("something happened to the watcher");
            e.printStackTrace();
        }

        //--

        m3weResources.mkdir();
        for (File packDir : m3weResources.listFiles()) {
            if(!packDir.isDirectory()) continue;
            var dirs = Arrays.asList(packDir.list());

            if(dirs.contains("assets")) {
                for(File resourceDir : new File(packDir.getAbsolutePath()+"\\assets").listFiles()){
                    m3weData.crawlResources(resourceDir,"", resourceDir.getName(),
                            packDir.getName()+"/assets/"+resourceDir.getName()+"/", true);
                }
            }
            if(dirs.contains("data")) {
                for(File resourceDir : new File(packDir.getAbsolutePath()+"\\data").listFiles()){
                    m3weData.crawlResources(resourceDir, "", resourceDir.getName(),
                            packDir.getName() + "/data/" + resourceDir.getName() + "/", false);
                }
            }
        }
    }

    private static void initObjects(File folder,Function<File, Utils.SuccessAndIdentifier> func, String prefix){
        prefix+=folder.getName()+"/";

        for (File modFile : folder.listFiles()) {
            if(modFile.isDirectory())
                initObjects(modFile,func,prefix);

            if(FilenameUtils.isExtension(modFile.getName(),"json")) {
                Utils.SuccessAndIdentifier modObject = func.apply(modFile);
                switch (modObject.successRate) {
                    case CANT_READ -> System.out.println("Can't read file "+prefix+modFile.getName());
                    case BAD_JSON -> System.out.println("Bad JSON in file "+prefix+modFile.getName());
                    case IDK -> System.out.println("Message me on discord KYFEX#3614 and tell me to fix my mod");
                    case YOU_DID_IT -> m3weData.packNamespaces.add(modObject.identifier.getNamespace());
                }
            }
        }
    }

    public static File m3weResources = new File(JBFolder.getAbsolutePath()+"\\resources");
}
