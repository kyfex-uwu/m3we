package com.kyfexuwu.m3we;

import com.kyfexuwu.m3we.initializers.BlockIniter;
import com.kyfexuwu.m3we.initializers.InitUtils;
import com.kyfexuwu.m3we.initializers.ItemIniter;
import com.kyfexuwu.m3we.lua.CustomScript;
import com.kyfexuwu.m3we.lua.Translations;
import com.kyfexuwu.m3we.lua.m3weBlockEntity;
import com.kyfexuwu.m3we.luablock.LuaBlock;
import com.kyfexuwu.m3we.luablock.LuaBlockEntity;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Function;

public class m3we implements ModInitializer {
    public static String MOD_ID = "m3we";
    public static Logger LOGGER = LogUtils.getLogger();

    public static Block luaBlock = new LuaBlock(FabricBlockSettings.copyOf(Blocks.COMMAND_BLOCK));
    public static final BlockEntityType<LuaBlockEntity> luaBlockEntityType = Registry.register(
            Registry.BLOCK_ENTITY_TYPE,
            new Identifier(MOD_ID, "lua_block"),
            FabricBlockEntityTypeBuilder.create((pos,state)->
                    new LuaBlockEntity(pos,state,false),luaBlock).build()
    );
    public static BlockEntityType<m3weBlockEntity> m3weBlockEntityType;
    static{
        Registry.register(Registry.BLOCK, new Identifier(MOD_ID, "lua_block"),luaBlock);
        Registry.register(Registry.ITEM, new Identifier(MOD_ID, "lua_block"),
                new BlockItem(luaBlock, new FabricItemSettings()));
        //.group(ItemGroup.OPERATOR);//1.20
    }

    public static HashMap<String, Block> m3weBlocks = new HashMap<>();
    public static HashMap<String, Item> m3weItems = new HashMap<>();

    public static File m3weFolder = new File(FabricLoader.getInstance().getConfigDir()//just inside the .minecraft folder
            .toString()+"\\m3we");
    public static File blocksFolder = new File(m3weFolder.getAbsolutePath()+"\\blocks");
    public static File itemsFolder = new File(m3weFolder.getAbsolutePath()+"\\items");
    public static File scriptsFolder = new File(m3weFolder.getAbsolutePath()+"\\scripts");

    public static final ItemGroup m3weItemGroup = FabricItemGroupBuilder.build(
            new Identifier(MOD_ID,"item_group"),
            ()->new ItemStack(Blocks.BEACON)
    );

    public static final Identifier updateLuaBlockPacket = new Identifier("m3we","update_lua_block");
    public static final Identifier askForLuaCodePacket = new Identifier("m3we","get_lua_code");
    public static final Identifier giveLuaCodePacket = new Identifier("m3we","give_lua_code");
    @Override
    public void onInitialize() {
        Translations.init();

        m3weFolder.mkdir();
        blocksFolder.mkdir();
        itemsFolder.mkdir();
        scriptsFolder.mkdir();

        initObjects(blocksFolder, BlockIniter::blockFromFile);
        initObjects(itemsFolder, ItemIniter::itemFromFile);

        m3weBlockEntityType = Registry.register(
                Registry.BLOCK_ENTITY_TYPE,
                new Identifier(MOD_ID, "m3we_block"),
                FabricBlockEntityTypeBuilder.create(m3weBlockEntity::new,
                        m3weBlocks.values().toArray(new Block[0])).build()
        );

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
                            m3we.LOGGER.error("unable to can (this is a bad message)");
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    m3we.LOGGER.error("oof ouch interrupted");
                }
            });
            watcherThread.start();
        } catch (IOException e) {
            m3we.LOGGER.error("something happened to the watcher");
            e.printStackTrace();
        }

        //--

        m3weResources.mkdir();
        initReloadables();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("m3we:reload").executes(context -> {
                    initReloadables();
                    context.getSource().sendFeedback(Text.literal("Reloaded"), false);
                    return 1;
                })));
    }

    public static void initReloadables(){
        m3weData.packResources.clear();
        m3weData.packData.clear();

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

    private static ArrayList<File> getFiles(File folder){
        var toReturn = new ArrayList<File>();
        for(var file : folder.listFiles()){
            if(file.isDirectory())
                toReturn.addAll(getFiles(file));
            if(FilenameUtils.isExtension(file.getName(),"json"))
                toReturn.add(file);
        }
        return toReturn;
    }
    private static void initObjects(File folder, Function<File, InitUtils.SuccessAndIdentifier> func){
        var prefixLength = folder.getAbsolutePath().length();

        var toProcess = getFiles(folder);
        int consecutiveComeBackLaters=0;
        while(toProcess.size()>0){
            var modFile = toProcess.get(0);

            InitUtils.SuccessAndIdentifier modObject = func.apply(modFile);
            switch (modObject.successRate) {
                case CANT_READ -> m3we.LOGGER.error("Can't read file "+modFile.getAbsolutePath().substring(prefixLength));
                case BAD_JSON -> m3we.LOGGER.error("Bad JSON in file "+modFile.getAbsolutePath().substring(prefixLength));
                case IDK -> m3we.LOGGER.error("Message me on discord @kyfexuwu and tell me to fix my mod");
                case YOU_DID_IT -> m3weData.packNamespaces.add(modObject.identifier.getNamespace());
                case COME_BACK_LATER -> toProcess.add(modFile);
            }
            toProcess.remove(0);

            if(modObject.successRate==InitUtils.SuccessRate.COME_BACK_LATER) consecutiveComeBackLaters++;
            else consecutiveComeBackLaters=0;
            if(consecutiveComeBackLaters==toProcess.size()){
                m3we.LOGGER.error("Circular loop detected in initializing objects that use \"copyFrom\", " +
                        "those objects not be initialized:\n"+toProcess.stream()
                        .map(file->file.getAbsolutePath().substring(prefixLength)));
                break;
            }
        }
    }

    public static File m3weResources = new File(m3weFolder.getAbsolutePath()+"\\resources");
}
